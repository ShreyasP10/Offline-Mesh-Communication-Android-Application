package com.example.omc.mesh;

import android.content.Context;
import android.util.Log;

import com.example.omc.connection.ConnectionManager;
import com.example.omc.discovery.DiscoveryManager;
import com.example.omc.discovery.Peer;
import com.example.omc.routing.RoutingManager;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MeshManager {

    private static final String TAG = MeshConfig.LOG_MESH;

    private final Context context;

    private final MeshNode localNode;

    private final DiscoveryManager discoveryManager;
    private final ConnectionManager connectionManager;
    private final RoutingManager routingManager;

    private final List<Peer> discoveredPeers = new CopyOnWriteArrayList<>();

    private boolean meshRunning = false;

    public MeshManager(Context context) {

        this.context = context.getApplicationContext();

        String deviceName = android.os.Build.MANUFACTURER
                + " "
                + android.os.Build.MODEL;

        this.localNode = new MeshNode(deviceName);

        this.connectionManager = new ConnectionManager(
                this.context,
                localNode
        );

        this.discoveryManager = new DiscoveryManager(
                this.context,
                localNode,
                new DiscoveryManager.DiscoveryListener() {

                    @Override
                    public void onPeerDiscovered(Peer peer) {

                        if (!containsPeer(peer.getEndpointId())) {
                            discoveredPeers.add(peer);
                        }

                        Log.d(
                                TAG,
                                "Peer discovered: " + peer.getName()
                        );

                        connectionManager.requestConnection(peer);
                    }

                    @Override
                    public void onPeerLost(String endpointId) {

                        removePeer(endpointId);

                        Log.d(
                                TAG,
                                "Peer lost: " + endpointId
                        );
                    }
                }
        );

        this.routingManager = new RoutingManager(localNode);
    }

    public void startMesh() {

        if (meshRunning) {
            Log.d(TAG, "Mesh already running");
            return;
        }

        meshRunning = true;

        Log.d(
                TAG,
                "Starting mesh. Node ID: " + localNode.getNodeId()
        );

        connectionManager.start();

        discoveryManager.startAdvertising(
                connectionManager.getConnectionLifecycleCallback()
        );
        discoveryManager.startDiscovery();

        Log.d(TAG, "Mesh started");
    }

    public void stopMesh() {

        if (!meshRunning) {
            return;
        }

        meshRunning = false;

        discoveryManager.stop();

        connectionManager.stop();

        routingManager.clearRoutes();

        discoveredPeers.clear();

        Log.d(TAG, "Mesh stopped");
    }

    public MeshNode getLocalNode() {
        return localNode;
    }

    public boolean isMeshRunning() {
        return meshRunning;
    }

    public List<Peer> getDiscoveredPeers() {
        return discoveredPeers;
    }

    private boolean containsPeer(String endpointId) {

        for (Peer peer : discoveredPeers) {

            if (peer.getEndpointId().equals(endpointId)) {
                return true;
            }
        }

        return false;
    }

    private void removePeer(String endpointId) {

        discoveredPeers.removeIf(
                peer -> peer.getEndpointId().equals(endpointId)
        );

        routingManager.removeRoute(endpointId);
    }
}