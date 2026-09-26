package com.example.omc.dtn;

import android.content.Context;

import com.example.omc.mesh.MeshConfig;
import com.example.omc.mesh.MeshLogger;
import com.example.omc.protocol.MessageType;
import com.example.omc.protocol.OMCHeader;
import com.example.omc.protocol.OMCMessage;
import com.example.omc.storage.ChatMessage;
import com.example.omc.storage.DatabaseHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Delay-Tolerant Networking (DTN) Store-and-Forward engine.
 * Stores unroutable messages and flushes them when a route or peer appears.
 */
public class DtnStore {

    private static final String TAG = MeshConfig.LOG_DTN;
    private static final long DEFAULT_EXPIRY_MS = 24 * 60 * 60 * 1000L; // 24 hours
    private static final int MAX_IN_MEMORY_PENDING = 500;

    private final DatabaseHelper dbHelper;
    private final ConcurrentHashMap<String, OMCMessage> inMemoryPending = new ConcurrentHashMap<>();

    public DtnStore(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
        restorePendingFromDatabase();
    }

    private void restorePendingFromDatabase() {
        try {
            List<ChatMessage> pending = dbHelper.getPendingMessages();
            if (pending != null) {
                int count = 0;
                for (ChatMessage cm : pending) {
                    if (count >= MAX_IN_MEMORY_PENDING) break;
                    OMCHeader header = new OMCHeader(
                            MeshConfig.PROTOCOL_VERSION,
                            cm.getMessageId(),
                            cm.getSourceId(),
                            cm.getDestinationId(),
                            MessageType.CHAT,
                            MeshConfig.DEFAULT_TTL,
                            cm.getHopCount(),
                            cm.getTimestamp(),
                            null
                    );
                    OMCMessage msg = new OMCMessage(header, cm.getText());
                    inMemoryPending.put(cm.getMessageId(), msg);
                    count++;
                }
                if (count > 0) {
                    MeshLogger.log(TAG, "Restored " + count + " pending DTN messages from persistent storage");
                }
            }
        } catch (Exception e) {
            MeshLogger.log(TAG, "Failed to restore pending messages: " + e.getMessage(), "E");
        }
    }

    public void save(OMCMessage message) {
        if (message == null || message.getHeader() == null) return;
        String id = message.getHeader().getMessageId();
        inMemoryPending.put(id, message);

        // Also record in database with PENDING status
        ChatMessage chatMsg = new ChatMessage(
                id,
                message.getHeader().getSourceId(),
                "Me",
                message.getHeader().getDestinationId(),
                message.getPayload(),
                ChatMessage.STATUS_PENDING,
                message.getHeader().getTimestamp(),
                true,
                message.getHeader().getHopCount()
        );
        dbHelper.saveMessage(chatMsg);

        MeshLogger.log(TAG, "DTN stored pending message: " + id + " for " + message.getHeader().getDestinationId());
    }

    public List<OMCMessage> getPendingForDestination(String destinationId) {
        List<OMCMessage> matches = new ArrayList<>();
        if (destinationId == null) return matches;

        for (OMCMessage msg : inMemoryPending.values()) {
            if (msg.getHeader() != null) {
                String dest = msg.getHeader().getDestinationId();
                if (destinationId.equals(dest) || ChatMessage.BROADCAST_DESTINATION.equalsIgnoreCase(dest)) {
                    matches.add(msg);
                }
            }
        }

        // Enforce FIFO order (oldest first) per FR-5.4
        Collections.sort(matches, (a, b) -> Long.compare(
                a.getHeader() != null ? a.getHeader().getTimestamp() : 0,
                b.getHeader() != null ? b.getHeader().getTimestamp() : 0
        ));

        return matches;
    }

    public List<OMCMessage> getAllPending() {
        List<OMCMessage> all = new ArrayList<>(inMemoryPending.values());
        Collections.sort(all, (a, b) -> Long.compare(
                a.getHeader() != null ? a.getHeader().getTimestamp() : 0,
                b.getHeader() != null ? b.getHeader().getTimestamp() : 0
        ));
        return all;
    }

    public void remove(String messageId) {
        if (messageId != null) {
            inMemoryPending.remove(messageId);
        }
    }

    public void markDelivered(String messageId) {
        if (messageId != null) {
            inMemoryPending.remove(messageId);
            dbHelper.updateMessageStatus(messageId, ChatMessage.STATUS_DELIVERED);
            MeshLogger.log(TAG, "DTN marked delivered: " + messageId);
        }
    }

    public int purgeExpired() {
        long expiryHours = 24;
        try {
            expiryHours = Long.parseLong(dbHelper.getSetting("message_expiry_hours", "24"));
        } catch (NumberFormatException ignored) {}
        long cutoff = System.currentTimeMillis() - (expiryHours * 60 * 60 * 1000L);

        int count = 0;
        for (OMCMessage msg : inMemoryPending.values()) {
            if (msg.getHeader() != null && msg.getHeader().getTimestamp() < cutoff) {
                inMemoryPending.remove(msg.getHeader().getMessageId());
                dbHelper.updateMessageStatus(msg.getHeader().getMessageId(), ChatMessage.STATUS_FAILED);
                count++;
            }
        }
        dbHelper.purgeExpired(cutoff);
        if (count > 0) {
            MeshLogger.log(TAG, "Purged " + count + " expired DTN messages");
        }
        return count;
    }

    public long getOldestPendingAgeMs() {
        long now = System.currentTimeMillis();
        long oldestTime = -1;
        for (OMCMessage msg : inMemoryPending.values()) {
            if (msg.getHeader() != null) {
                long t = msg.getHeader().getTimestamp();
                if (oldestTime == -1 || t < oldestTime) {
                    oldestTime = t;
                }
            }
        }
        return oldestTime == -1 ? 0 : Math.max(0, now - oldestTime);
    }

    public int size() {
        return inMemoryPending.size();
    }
}
