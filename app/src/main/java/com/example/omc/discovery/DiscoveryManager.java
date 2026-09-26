package com.example.omc.discovery;

import android.content.Context;
import android.util.Log;

import com.example.omc.connection.ConnectionManager;
import com.example.omc.mesh.MeshConfig;
import com.example.omc.mesh.MeshNode;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.Strategy;

public class DiscoveryManager {

    private static final String TAG = MeshConfig.LOG_MESH;

    private final Context context;
    private final MeshNode localNode;
    private final DiscoveryListener listener;

    private final ConnectionsClient connectionsClient;

    private EndpointDiscoveryCallback endpointDiscoveryCallback;

    private boolean advertising = false;
    private boolean discovering = false;
    private boolean lowPowerMode = false;

    public interface DiscoveryListener {

        void onPeerDiscovered(Peer peer);

        void onPeerLost(String endpointId);
    }

    public DiscoveryManager(
            Context context,
            MeshNode localNode,
            DiscoveryListener listener
    ) {

        this.context = context.getApplicationContext();
        this.localNode = localNode;
        this.listener = listener;

        this.connectionsClient =
                Nearby.getConnectionsClient(this.context);
    }


    public void startAdvertising(
            ConnectionLifecycleCallback connectionLifecycleCallback
    ) {

        if (advertising) {
            Log.d(TAG, "Advertising already running");
            return;
        }

        AdvertisingOptions options =
                new AdvertisingOptions.Builder()
                        .setStrategy(Strategy.P2P_CLUSTER)
                        .setLowPower(lowPowerMode)
                        .setDisruptiveUpgrade(false)
                        .build();

        connectionsClient
                .startAdvertising(
                        localNode.getNodeName(),
                        MeshConfig.SERVICE_ID,
                        connectionLifecycleCallback,
                        options
                )
                .addOnSuccessListener(unused -> {

                    advertising = true;

                    Log.d(
                            TAG,
                            "Advertising started"
                    );
                })
                .addOnFailureListener(e -> {

                    advertising = false;

                    Log.e(
                            TAG,
                            "Advertising failed",
                            e
                    );
                });
    }


    public void startDiscovery() {

        if (discovering) {
            Log.d(TAG, "Discovery already running");
            return;
        }

        endpointDiscoveryCallback =
                new EndpointDiscoveryCallback(
                        new EndpointDiscoveryCallback.Listener() {

                            @Override
                            public void onEndpointFound(
                                    String endpointId,
                                    String endpointName
                            ) {

                                Log.d(
                                        TAG,
                                        "Endpoint found: "
                                                + endpointName
                                );

                                Peer peer =
                                        new Peer(
                                                endpointId,
                                                endpointName
                                        );

                                listener.onPeerDiscovered(
                                        peer
                                );
                            }

                            @Override
                            public void onEndpointLost(
                                    String endpointId
                            ) {

                                Log.d(
                                        TAG,
                                        "Endpoint lost: "
                                                + endpointId
                                );

                                listener.onPeerLost(
                                        endpointId
                                );
                            }
                        }
                );

        DiscoveryOptions options =
                new DiscoveryOptions.Builder()
                        .setStrategy(Strategy.P2P_CLUSTER)
                        .setLowPower(lowPowerMode)
                        .build();

        connectionsClient
                .startDiscovery(
                        MeshConfig.SERVICE_ID,
                        endpointDiscoveryCallback,
                        options
                )
                .addOnSuccessListener(unused -> {

                    discovering = true;

                    Log.d(
                            TAG,
                            "Discovery started"
                    );
                })
                .addOnFailureListener(e -> {

                    discovering = false;

                    Log.e(
                            TAG,
                            "Discovery failed",
                            e
                    );
                });
    }


    public void stopAdvertising() {
        if (advertising) {
            connectionsClient.stopAdvertising();
            advertising = false;
            Log.d(TAG, "Advertising stopped");
        }
    }

    public void stopDiscovery() {
        if (discovering) {
            connectionsClient.stopDiscovery();
            discovering = false;
            Log.d(TAG, "Discovery stopped");
        }
    }

    public void stop() {
        stopAdvertising();
        stopDiscovery();
    }

    public void setLowPowerMode(boolean lowPower, ConnectionLifecycleCallback cb) {
        if (this.lowPowerMode == lowPower) return;
        this.lowPowerMode = lowPower;
        Log.d(TAG, "Adaptive scanning duty-cycle updated: lowPower=" + lowPower);

        if (advertising && cb != null) {
            connectionsClient.stopAdvertising();
            advertising = false;
            startAdvertising(cb);
        }

        if (discovering) {
            connectionsClient.stopDiscovery();
            discovering = false;
            startDiscovery();
        }
    }

    public boolean isAdvertising() {
        return advertising;
    }

    public boolean isDiscovering() {
        return discovering;
    }
}