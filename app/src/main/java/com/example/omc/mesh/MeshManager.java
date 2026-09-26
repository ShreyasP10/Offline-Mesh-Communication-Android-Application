package com.example.omc.mesh;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.omc.ack.AckManager;
import com.example.omc.connection.ConnectionCallback;
import com.example.omc.connection.ConnectionManager;
import com.example.omc.discovery.DiscoveryManager;
import com.example.omc.discovery.Peer;
import com.example.omc.dtn.DtnStore;
import com.example.omc.heartbeat.HeartbeatManager;
import com.example.omc.protocol.MessageType;
import com.example.omc.protocol.OMCHeader;
import com.example.omc.protocol.OMCMessage;
import com.example.omc.protocol.ProtocolManager;
import com.example.omc.routing.RoutingManager;
import com.example.omc.storage.ChatMessage;
import com.example.omc.storage.DatabaseHelper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Main facade orchestrating discovery, connections, routing, store-and-forward (DTN),
 * ACKs, heartbeats, and persistence.
 */
public class MeshManager {

    private static final String TAG = MeshConfig.LOG_MESH;

    private static MeshManager instance;

    public interface MeshListener {
        void onMeshStateChanged(boolean running);
        void onPeersUpdated(List<Peer> peers);
        void onMessageReceived(ChatMessage message);
        void onMessageStatusChanged(String messageId, String status);
    }

    private final Context context;
    private final MeshNode localNode;
    private final DatabaseHelper dbHelper;

    private final DiscoveryManager discoveryManager;
    private final ConnectionManager connectionManager;
    private final RoutingManager routingManager;
    private final ProtocolManager protocolManager;
    private final SeenPacketCache seenPacketCache;
    private final DtnStore dtnStore;
    private final AckManager ackManager;
    private final HeartbeatManager heartbeatManager;
    private final com.example.omc.security.MessageSigner messageSigner;
    private final com.example.omc.security.MessageVerifier messageVerifier;

    private final List<Peer> discoveredPeers = new CopyOnWriteArrayList<>();
    private final Map<String, String> endpointToNodeId = new ConcurrentHashMap<>();
    private final Map<String, String> nodeIdToEndpoint = new ConcurrentHashMap<>();

    private final java.util.concurrent.atomic.AtomicInteger dupDropsCounter = new java.util.concurrent.atomic.AtomicInteger(0);
    private final java.util.concurrent.atomic.AtomicInteger ttlDropsCounter = new java.util.concurrent.atomic.AtomicInteger(0);
    private final java.util.concurrent.atomic.AtomicInteger malformedDropsCounter = new java.util.concurrent.atomic.AtomicInteger(0);

    private final List<MeshListener> listeners = new CopyOnWriteArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final java.util.concurrent.ExecutorService messageExecutor = java.util.concurrent.Executors.newFixedThreadPool(4);

    private boolean meshRunning = false;

    public static synchronized MeshManager getInstance(Context context) {
        if (instance == null) {
            instance = new MeshManager(context.getApplicationContext());
        }
        return instance;
    }

    public MeshManager(Context context) {
        this.context = context.getApplicationContext();
        this.dbHelper = DatabaseHelper.getInstance(this.context);

        // Load or create persistent node identity
        String savedNodeId = dbHelper.getSetting("node_id", null);
        String savedNodeName = dbHelper.getSetting("display_name", null);

        if (savedNodeId == null) {
            savedNodeId = UUID.randomUUID().toString();
            dbHelper.saveSetting("node_id", savedNodeId);
        }

        if (savedNodeName == null || savedNodeName.trim().isEmpty()) {
            savedNodeName = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;
            dbHelper.saveSetting("display_name", savedNodeName);
        }

        this.localNode = new MeshNode(savedNodeId, savedNodeName, true);

        this.protocolManager = new ProtocolManager();
        this.routingManager = new RoutingManager(localNode);
        this.seenPacketCache = new SeenPacketCache(10000);
        this.dtnStore = new DtnStore(this.context);
        this.ackManager = new AckManager();
        this.messageSigner = new com.example.omc.security.MessageSigner();
        this.messageVerifier = new com.example.omc.security.MessageVerifier();

        this.connectionManager = new ConnectionManager(this.context, localNode);

        // Setup connection callbacks
        this.connectionManager.setCallback(new ConnectionCallback() {
            @Override
            public void onConnectionInitiated(String endpointId, String endpointName) {
                MeshLogger.log(TAG, "Incoming connection initiated from: " + endpointName + " (" + endpointId + ")");
            }

            @Override
            public void onConnected(String endpointId) {
                handleEndpointConnected(endpointId);
            }

            @Override
            public void onConnectionFailed(String endpointId) {
                MeshLogger.log(TAG, "Connection failed for endpoint: " + endpointId, "E");
                updatePeerConnected(endpointId, false);
            }

            @Override
            public void onDisconnected(String endpointId) {
                handleEndpointDisconnected(endpointId);
            }

            @Override
            public void onPayloadReceived(String endpointId, byte[] payload) {
                // Scalable async packet dispatch - never blocks the radio connection thread
                messageExecutor.execute(() -> handleInboundPayload(endpointId, payload));
            }
        });

        // Setup discovery listener
        this.discoveryManager = new DiscoveryManager(
                this.context,
                localNode,
                new DiscoveryManager.DiscoveryListener() {
                    @Override
                    public void onPeerDiscovered(Peer peer) {
                        handlePeerDiscovered(peer);
                    }

                    @Override
                    public void onPeerLost(String endpointId) {
                        handlePeerLost(endpointId);
                    }
                }
        );

        // Setup ACK callbacks
        this.ackManager.setListener(new AckManager.AckListener() {
            @Override
            public void onDeliveryConfirmed(String messageId) {
                dbHelper.updateMessageStatus(messageId, ChatMessage.STATUS_DELIVERED);
                dtnStore.markDelivered(messageId);
                notifyMessageStatusChanged(messageId, ChatMessage.STATUS_DELIVERED);
            }

            @Override
            public void onDeliveryFailed(String messageId) {
                dbHelper.updateMessageStatus(messageId, ChatMessage.STATUS_FAILED);
                notifyMessageStatusChanged(messageId, ChatMessage.STATUS_FAILED);
            }

            @Override
            public void onRetryRequested(OMCMessage message, int attempt) {
                transmitPacket(message, null);
            }
        });

        // Setup Heartbeat callbacks
        this.heartbeatManager = new HeartbeatManager(new HeartbeatManager.HeartbeatListener() {
            @Override
            public void onSendHeartbeat() {
                sendHeartbeatToNeighbors();
            }

            @Override
            public void onDeadPeerDetected(Peer peer) {
                if (peer != null) {
                    connectionManager.disconnect(peer.getEndpointId());
                    handleEndpointDisconnected(peer.getEndpointId());
                }
            }
        });
    }

    public synchronized void startMesh() {
        if (meshRunning) {
            MeshLogger.log(TAG, "Mesh is already running");
            return;
        }

        meshRunning = true;
        MeshLogger.log(TAG, "Starting OMC Mesh. Node: " + localNode.getNodeName() + " (" + localNode.getNodeId() + ")");

        connectionManager.start();

        discoveryManager.startAdvertising(
                connectionManager.getConnectionLifecycleCallback()
        );
        discoveryManager.startDiscovery();

        long hbSec = 4;
        try {
            hbSec = Long.parseLong(dbHelper.getSetting("heartbeat_interval_sec", "4"));
        } catch (NumberFormatException ignored) {}
        heartbeatManager.setIntervalMs(hbSec * 1000L);
        heartbeatManager.start();

        notifyMeshStateChanged(true);
        MeshLogger.log(TAG, "OMC Mesh successfully started and active");
    }

    public synchronized void stopMesh() {
        if (!meshRunning) {
            return;
        }

        meshRunning = false;
        MeshLogger.log(TAG, "Stopping OMC Mesh...");

        discoveryManager.stop();
        heartbeatManager.stop();
        ackManager.clear();
        connectionManager.stop();

        routingManager.clearRoutes();
        discoveredPeers.clear();
        endpointToNodeId.clear();
        nodeIdToEndpoint.clear();

        notifyMeshStateChanged(false);
        notifyPeersUpdated();
        MeshLogger.log(TAG, "OMC Mesh stopped");
    }

    // --- Peer Management ---

    private void handlePeerDiscovered(Peer peer) {
        Peer existing = findPeerByEndpoint(peer.getEndpointId());
        if (existing == null) {
            discoveredPeers.add(peer);
            MeshLogger.log(TAG, "Discovered new peer: " + peer.getName() + " (" + peer.getEndpointId() + ")");
        } else {
            existing.updateLastSeen();
        }

        notifyPeersUpdated();

        // Auto-connect to discovered peer
        connectionManager.requestConnection(peer);
    }

    private void handlePeerLost(String endpointId) {
        Peer peer = findPeerByEndpoint(endpointId);
        if (peer != null) {
            discoveredPeers.remove(peer);
            MeshLogger.log(TAG, "Peer lost: " + peer.getName() + " (" + endpointId + ")");
        }
        handleEndpointDisconnected(endpointId);
        notifyPeersUpdated();
    }

    private void handleEndpointConnected(String endpointId) {
        MeshLogger.log(TAG, "Connected to endpoint: " + endpointId);
        updatePeerConnected(endpointId, true);

        // Send HELLO post-connect handshake
        sendHello(endpointId);

        // Direct routing fallback
        routingManager.addDirectRoute(endpointId);

        // Attempt DTN flush
        flushDtnQueue(endpointId);

        notifyPeersUpdated();
    }

    private void handleEndpointDisconnected(String endpointId) {
        MeshLogger.log(TAG, "Disconnected from endpoint: " + endpointId);
        updatePeerConnected(endpointId, false);

        String nodeId = endpointToNodeId.remove(endpointId);
        if (nodeId != null) {
            nodeIdToEndpoint.remove(nodeId);
            routingManager.removeRoute(nodeId);
        }
        routingManager.removeRoute(endpointId);

        notifyPeersUpdated();
    }

    private void updatePeerConnected(String endpointId, boolean connected) {
        Peer peer = findPeerByEndpoint(endpointId);
        if (peer != null) {
            peer.setConnected(connected);
            peer.updateLastSeen();
        }
    }

    private Peer findPeerByEndpoint(String endpointId) {
        for (Peer p : discoveredPeers) {
            if (p.getEndpointId().equals(endpointId)) {
                return p;
            }
        }
        return null;
    }

    public Peer findPeerByNodeId(String nodeId) {
        if (nodeId == null) return null;
        for (Peer p : discoveredPeers) {
            if (nodeId.equals(p.getNodeId()) || nodeId.equals(p.getEndpointId())) {
                return p;
            }
        }
        return null;
    }

    // --- Message Sending ---

    public String sendChatMessage(String destinationId, String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        String messageId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();

        boolean isBroadcast = ChatMessage.BROADCAST_DESTINATION.equalsIgnoreCase(destinationId);

        OMCMessage message = protocolManager.createMessage(
                localNode.getNodeId(),
                destinationId,
                MessageType.CHAT,
                text
        );
        message.getHeader().setMessageId(messageId);

        // Sign message if per-message signing is enabled (FR-8.2)
        boolean signMessages = Boolean.parseBoolean(dbHelper.getSetting("sign_messages", "false"));
        if (signMessages) {
            String signature = messageSigner.sign(message);
            message.setSignature(signature);
            message.getHeader().setFlags(message.getHeader().getFlags() | OMCHeader.FLAG_SIGNED);
        }

        // Add to seen cache so we don't process our own broadcast if echoed
        seenPacketCache.add(messageId);

        // Save to database
        ChatMessage chatMsg = new ChatMessage(
                messageId,
                localNode.getNodeId(),
                "Me",
                destinationId,
                text,
                ChatMessage.STATUS_SENT,
                now,
                true,
                0
        );
        dbHelper.saveMessage(chatMsg);

        // Transmit
        boolean transmitted = transmitPacket(message, null);

        if (!transmitted && !isBroadcast) {
            // Save to DTN for later
            chatMsg.setStatus(ChatMessage.STATUS_PENDING);
            dbHelper.updateMessageStatus(messageId, ChatMessage.STATUS_PENDING);
            dtnStore.save(message);
        } else if (!isBroadcast) {
            // Unicast: track ACK
            ackManager.trackMessage(message);
        }

        notifyMessageReceived(chatMsg);
        return messageId;
    }

    public void retryMessage(String messageId) {
        List<ChatMessage> all = dbHelper.getAllMessages();
        for (ChatMessage m : all) {
            if (m.getMessageId().equals(messageId)) {
                OMCMessage omcMsg = protocolManager.createMessage(
                        localNode.getNodeId(),
                        m.getDestinationId(),
                        MessageType.CHAT,
                        m.getText()
                );
                omcMsg.getHeader().setMessageId(messageId);

                boolean signMessages = Boolean.parseBoolean(dbHelper.getSetting("sign_messages", "false"));
                if (signMessages) {
                    String signature = messageSigner.sign(omcMsg);
                    omcMsg.setSignature(signature);
                    omcMsg.getHeader().setFlags(omcMsg.getHeader().getFlags() | OMCHeader.FLAG_SIGNED);
                }

                m.setStatus(ChatMessage.STATUS_SENT);
                dbHelper.updateMessageStatus(messageId, ChatMessage.STATUS_SENT);
                notifyMessageStatusChanged(messageId, ChatMessage.STATUS_SENT);
                transmitPacket(omcMsg, null);
                if (!m.isBroadcast()) {
                    ackManager.trackMessage(omcMsg);
                }
                break;
            }
        }
    }

    // --- Protocol & Relay Engine ---

    private void handleInboundPayload(String incomingEndpoint, byte[] payloadBytes) {
        String json = new String(payloadBytes, StandardCharsets.UTF_8);
        OMCMessage message = protocolManager.parseFromTransport(json);

        if (message == null || message.getHeader() == null) {
            malformedDropsCounter.incrementAndGet();
            MeshLogger.log(TAG, "Dropped malformed or invalid packet from " + incomingEndpoint, "E");
            return;
        }

        OMCHeader header = message.getHeader();
        String messageId = header.getMessageId();
        String sourceId = header.getSourceId();
        String destinationId = header.getDestinationId();

        // Verify signature if packet is signed or signing is enforced (FR-8.2)
        if (message.getSignature() != null && !message.getSignature().isEmpty()) {
            boolean valid = messageVerifier.verify(message);
            if (!valid) {
                malformedDropsCounter.incrementAndGet();
                MeshLogger.log(TAG, "SECURITY ALERT: Signature verification failed for packet " + messageId + " from " + sourceId, "E");
                return;
            }
        } else if (Boolean.parseBoolean(dbHelper.getSetting("sign_messages", "false")) && header.getMessageType() == MessageType.CHAT) {
            malformedDropsCounter.incrementAndGet();
            MeshLogger.log(TAG, "SECURITY ALERT: Dropped unsigned chat packet (signing enforced): " + messageId, "E");
            return;
        }

        // Dedup check (SeenPacketCache)
        if (seenPacketCache.isDuplicate(messageId)) {
            dupDropsCounter.incrementAndGet();
            MeshLogger.log(MeshConfig.LOG_ROUTE, "Duplicate packet dropped: " + messageId);
            return;
        }

        // Update sender liveness
        Peer peer = findPeerByEndpoint(incomingEndpoint);
        if (peer != null) {
            peer.updateLastSeen();
        }

        // Process message type
        switch (header.getMessageType()) {
            case HELLO:
                handleHello(incomingEndpoint, sourceId, message.getPayload());
                break;

            case HEARTBEAT:
                MeshLogger.log(MeshConfig.LOG_HEARTBEAT, "Heartbeat from " + sourceId);
                break;

            case ACK:
                handleAck(incomingEndpoint, message);
                break;

            case CHAT:
                handleChat(incomingEndpoint, message);
                break;

            default:
                MeshLogger.log(TAG, "Unhandled message type: " + header.getMessageType());
                break;
        }
    }

    private void handleHello(String endpointId, String nodeId, String nodeName) {
        endpointToNodeId.put(endpointId, nodeId);
        nodeIdToEndpoint.put(nodeId, endpointId);

        Peer peer = findPeerByEndpoint(endpointId);
        if (peer != null) {
            peer.setNodeId(nodeId);
            if (nodeName != null && !nodeName.isEmpty()) {
                peer.setName(nodeName);
            }
        }

        routingManager.addDirectRoute(nodeId);
        MeshLogger.log(TAG, "HELLO handshake mapped: " + nodeName + " -> " + nodeId + " (" + endpointId + ")");

        flushDtnQueue(nodeId);
        notifyPeersUpdated();
    }

    private void handleAck(String incomingEndpoint, OMCMessage message) {
        OMCHeader header = message.getHeader();
        String ackTarget = header.getAckForMessageId();
        if (ackTarget == null || ackTarget.isEmpty()) {
            String payload = message.getPayload();
            if (payload != null && payload.startsWith("ACK:")) {
                ackTarget = payload.substring(4);
            }
        }

        if (localNode.getNodeId().equals(header.getDestinationId())) {
            if (ackTarget != null) {
                ackManager.onAckReceived(ackTarget);
                dbHelper.updateMessageStatus(ackTarget, ChatMessage.STATUS_DELIVERED);
                notifyMessageStatusChanged(ackTarget, ChatMessage.STATUS_DELIVERED);
            }
        } else if (header.getTtl() > 1) {
            // Relays forward ACKs like DATA (FR-6.4)
            header.setTtl(header.getTtl() - 1);
            header.setHopCount(header.getHopCount() + 1);
            MeshLogger.log(MeshConfig.LOG_ROUTE, "Relaying ACK packet " + header.getMessageId() + " toward " + header.getDestinationId());
            transmitPacket(message, incomingEndpoint);
        } else {
            ttlDropsCounter.incrementAndGet();
        }
    }

    private void handleChat(String incomingEndpoint, OMCMessage message) {
        OMCHeader header = message.getHeader();
        String dest = header.getDestinationId();
        boolean isForMe = localNode.getNodeId().equals(dest);
        boolean isBroadcast = ChatMessage.BROADCAST_DESTINATION.equalsIgnoreCase(dest);

        if (isForMe || isBroadcast) {
            // Deliver locally
            Peer senderPeer = findPeerByNodeId(header.getSourceId());
            String senderName = senderPeer != null ? senderPeer.getName() : "Node-" + header.getSourceId().substring(0, Math.min(6, header.getSourceId().length()));

            ChatMessage chatMsg = new ChatMessage(
                    header.getMessageId(),
                    header.getSourceId(),
                    senderName,
                    dest,
                    message.getPayload(),
                    ChatMessage.STATUS_DELIVERED,
                    header.getTimestamp(),
                    false,
                    header.getHopCount()
            );
            dbHelper.saveMessage(chatMsg);
            MeshLogger.log(TAG, "Delivered message from " + senderName + ": " + message.getPayload());
            notifyMessageReceived(chatMsg);

            // Send ACK if unicast
            if (isForMe) {
                sendAck(header.getSourceId(), header.getMessageId());
            }
        }

        // Multi-hop Relay: if broadcast or unicast for someone else, forward if TTL > 1
        if (header.getTtl() > 1 && (!isForMe || isBroadcast)) {
            header.setTtl(header.getTtl() - 1);
            header.setHopCount(header.getHopCount() + 1);

            MeshLogger.log(MeshConfig.LOG_ROUTE, "Relaying packet " + header.getMessageId() + " (TTL: " + header.getTtl() + ", Hops: " + header.getHopCount() + ")");
            transmitPacket(message, incomingEndpoint);
        }
    }

    private void sendHello(String endpointId) {
        OMCMessage hello = protocolManager.createHello(localNode.getNodeId(), localNode.getNodeName());
        String json = protocolManager.serialize(hello);
        if (json != null) {
            connectionManager.sendText(endpointId, json);
        }
    }

    private void sendHeartbeatToNeighbors() {
        if (!meshRunning) return;
        OMCMessage hb = protocolManager.createHeartbeat(localNode.getNodeId());
        String json = protocolManager.serialize(hb);
        if (json == null) return;

        for (Peer p : discoveredPeers) {
            if (p.isConnected()) {
                connectionManager.sendText(p.getEndpointId(), json);
            }
        }
        heartbeatManager.checkDeadPeers(discoveredPeers);
    }

    private void sendAck(String toNodeId, String messageId) {
        OMCMessage ack = protocolManager.createAck(localNode.getNodeId(), toNodeId, messageId);
        MeshLogger.log(MeshConfig.LOG_ACK, "Sending ACK for message " + messageId + " to " + toNodeId);
        transmitPacket(ack, null);
    }

    private boolean transmitPacket(OMCMessage message, String excludeEndpoint) {
        String json = protocolManager.serialize(message);
        if (json == null) return false;

        String destNodeId = message.getHeader().getDestinationId();
        boolean isBroadcast = ChatMessage.BROADCAST_DESTINATION.equalsIgnoreCase(destNodeId);

        boolean sentAny = false;

        if (isBroadcast) {
            // Flood to all connected peers except incoming
            for (Peer p : discoveredPeers) {
                if (p.isConnected() && (excludeEndpoint == null || !p.getEndpointId().equals(excludeEndpoint))) {
                    connectionManager.sendText(p.getEndpointId(), json);
                    sentAny = true;
                }
            }
        } else {
            // Unicast: check direct endpoint or next hop in routing table
            String directEndpoint = nodeIdToEndpoint.get(destNodeId);
            if (directEndpoint != null && (excludeEndpoint == null || !directEndpoint.equals(excludeEndpoint))) {
                connectionManager.sendText(directEndpoint, json);
                sentAny = true;
            } else {
                String nextHopNodeId = routingManager.findNextHop(destNodeId);
                String nextHopEndpoint = nextHopNodeId != null ? nodeIdToEndpoint.get(nextHopNodeId) : null;
                if (nextHopEndpoint != null && (excludeEndpoint == null || !nextHopEndpoint.equals(excludeEndpoint))) {
                    connectionManager.sendText(nextHopEndpoint, json);
                    sentAny = true;
                } else {
                    // Flood relay to all connected peers except incoming
                    for (Peer p : discoveredPeers) {
                        if (p.isConnected() && (excludeEndpoint == null || !p.getEndpointId().equals(excludeEndpoint))) {
                            connectionManager.sendText(p.getEndpointId(), json);
                            sentAny = true;
                        }
                    }
                }
            }
        }

        return sentAny;
    }

    private void flushDtnQueue(String destinationOrEndpoint) {
        List<OMCMessage> pending = dtnStore.getPendingForDestination(destinationOrEndpoint);
        for (OMCMessage m : pending) {
            MeshLogger.log(MeshConfig.LOG_DTN, "Flushing DTN pending message: " + m.getHeader().getMessageId());
            boolean sent = transmitPacket(m, null);
            if (sent) {
                dtnStore.remove(m.getHeader().getMessageId());
                dbHelper.updateMessageStatus(m.getHeader().getMessageId(), ChatMessage.STATUS_SENT);
                notifyMessageStatusChanged(m.getHeader().getMessageId(), ChatMessage.STATUS_SENT);
                if (!ChatMessage.BROADCAST_DESTINATION.equalsIgnoreCase(m.getHeader().getDestinationId())) {
                    ackManager.trackMessage(m);
                }
            }
        }
    }

    // --- State and Listeners ---

    public boolean isMeshRunning() {
        return meshRunning;
    }

    public MeshNode getLocalNode() {
        return localNode;
    }

    public List<Peer> getDiscoveredPeers() {
        return Collections.unmodifiableList(discoveredPeers);
    }

    public int getConnectedPeerCount() {
        int count = 0;
        for (Peer p : discoveredPeers) {
            if (p.isConnected()) count++;
        }
        return count;
    }

    public void updateDisplayName(String newName) {
        if (newName != null && !newName.trim().isEmpty()) {
            String trimmed = newName.trim();
            localNode.setNodeName(trimmed);
            dbHelper.saveSetting("display_name", trimmed);
            MeshLogger.log(TAG, "Display name updated to: " + trimmed);

            if (meshRunning && discoveryManager != null && connectionManager != null) {
                // Restart advertising with the new node name so nearby peers discover it immediately
                discoveryManager.stopAdvertising();
                discoveryManager.startAdvertising(connectionManager.getConnectionLifecycleCallback());

                // Broadcast updated HELLO handshake to all currently connected peers
                for (Peer p : discoveredPeers) {
                    if (p.isConnected()) {
                        sendHello(p.getEndpointId());
                    }
                }
            }
        }
    }

    public void addListener(MeshListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(MeshListener listener) {
        listeners.remove(listener);
    }

    private void notifyMeshStateChanged(boolean running) {
        mainHandler.post(() -> {
            for (MeshListener l : listeners) {
                try { l.onMeshStateChanged(running); } catch (Exception ignored) {}
            }
        });
    }

    private void notifyPeersUpdated() {
        List<Peer> copy = new ArrayList<>(discoveredPeers);
        mainHandler.post(() -> {
            for (MeshListener l : listeners) {
                try { l.onPeersUpdated(copy); } catch (Exception ignored) {}
            }
        });
    }

    private void notifyMessageReceived(ChatMessage message) {
        mainHandler.post(() -> {
            for (MeshListener l : listeners) {
                try { l.onMessageReceived(message); } catch (Exception ignored) {}
            }
        });
    }

    private void notifyMessageStatusChanged(String messageId, String status) {
        mainHandler.post(() -> {
            for (MeshListener l : listeners) {
                try { l.onMessageStatusChanged(messageId, status); } catch (Exception ignored) {}
            }
        });
    }

    public int getDupDropsCount() {
        return dupDropsCounter.get();
    }

    public int getTtlDropsCount() {
        return ttlDropsCounter.get();
    }

    public int getMalformedDropsCount() {
        return malformedDropsCounter.get();
    }

    public int getSeenCacheSize() {
        return seenPacketCache.size();
    }

    public int getDtnPendingCount() {
        return dtnStore.size();
    }

    public long getDtnOldestPendingAgeSec() {
        return dtnStore.getOldestPendingAgeMs() / 1000L;
    }

    public int getRoutingTableSize() {
        return routingManager.getRoutingTable().size();
    }

    public List<com.example.omc.routing.Route> getRoutingTableRoutes() {
        return routingManager.getRoutingTable().getAllRoutes();
    }

    public void setLowPowerMode(boolean lowPower) {
        if (discoveryManager != null && connectionManager != null) {
            discoveryManager.setLowPowerMode(lowPower, connectionManager.getConnectionLifecycleCallback());
        }
    }
}