package com.example.omc.connection;

import android.content.Context;
import android.util.Log;

import com.example.omc.discovery.Peer;
import com.example.omc.mesh.MeshConfig;
import com.example.omc.mesh.MeshNode;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionManager {

    private static final String TAG = MeshConfig.LOG_MESH;

    private final Context context;
    private final MeshNode localNode;

    private final ConnectionsClient connectionsClient;

    private final Map<String, ConnectionState> connectionStates =
            new ConcurrentHashMap<>();

    private ConnectionCallback callback;

    public ConnectionManager(
            Context context,
            MeshNode localNode
    ) {

        this.context = context.getApplicationContext();
        this.localNode = localNode;

        this.connectionsClient =
                Nearby.getConnectionsClient(this.context);
    }

    public void setCallback(ConnectionCallback callback) {
        this.callback = callback;
    }

    public void start() {

        Log.d(
                TAG,
                "ConnectionManager started"
        );
    }

    public void requestConnection(Peer peer) {

        if (peer == null) {
            return;
        }

        String endpointId = peer.getEndpointId();

        ConnectionState currentState =
                connectionStates.get(endpointId);

        if (currentState == ConnectionState.CONNECTING ||
                currentState == ConnectionState.CONNECTED) {

            return;
        }

        connectionStates.put(
                endpointId,
                ConnectionState.CONNECTING
        );

        Log.d(
                TAG,
                "Requesting connection to: "
                        + peer.getName()
        );

        connectionsClient.requestConnection(
                        localNode.getNodeName(),
                        endpointId,
                        connectionLifecycleCallback
                )
                .addOnSuccessListener(unused -> {

                    Log.d(
                            TAG,
                            "Connection request sent: "
                                    + endpointId
                    );
                })
                .addOnFailureListener(e -> {

                    connectionStates.put(
                            endpointId,
                            ConnectionState.FAILED
                    );

                    Log.e(
                            TAG,
                            "Connection request failed: "
                                    + endpointId,
                            e
                    );

                    if (callback != null) {

                        callback.onConnectionFailed(
                                endpointId
                        );
                    }
                });
    }

    public void acceptConnection(
            String endpointId
    ) {

        connectionsClient.acceptConnection(
                        endpointId,
                        payloadCallback
                )
                .addOnSuccessListener(unused -> {

                    Log.d(
                            TAG,
                            "Accepted connection: "
                                    + endpointId
                    );
                })
                .addOnFailureListener(e -> {

                    Log.e(
                            TAG,
                            "Failed to accept connection: "
                                    + endpointId,
                            e
                    );
                });
    }

    public void rejectConnection(
            String endpointId
    ) {

        connectionsClient.rejectConnection(endpointId);

        connectionStates.put(
                endpointId,
                ConnectionState.DISCONNECTED
        );
    }

    public void sendText(
            String endpointId,
            String text
    ) {

        if (text == null) {
            return;
        }

        byte[] bytes =
                text.getBytes(StandardCharsets.UTF_8);

        sendBytes(endpointId, bytes);
    }

    public void sendBytes(
            String endpointId,
            byte[] bytes
    ) {

        if (endpointId == null || bytes == null) {
            return;
        }

        if (bytes.length > MeshConfig.MAX_MESSAGE_SIZE) {

            Log.e(
                    TAG,
                    "Payload exceeds maximum size"
            );

            return;
        }

        Payload payload =
                Payload.fromBytes(bytes);

        connectionsClient
                .sendPayload(endpointId, payload)
                .addOnSuccessListener(unused -> {

                    Log.d(
                            TAG,
                            "Payload sent to "
                                    + endpointId
                    );
                })
                .addOnFailureListener(e -> {

                    Log.e(
                            TAG,
                            "Payload send failed",
                            e
                    );
                });
    }

    public void disconnect(
            String endpointId
    ) {

        if (endpointId == null) {
            return;
        }

        connectionsClient.disconnectFromEndpoint(
                endpointId
        );

        connectionStates.put(
                endpointId,
                ConnectionState.DISCONNECTED
        );
    }

    public void stop() {

        connectionsClient.stopAllEndpoints();

        connectionStates.clear();

        Log.d(
                TAG,
                "ConnectionManager stopped"
        );
    }

    public ConnectionState getConnectionState(
            String endpointId
    ) {

        return connectionStates.getOrDefault(
                endpointId,
                ConnectionState.DISCONNECTED
        );
    }

    private final ConnectionLifecycleCallback
            connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {

                @Override
                public void onConnectionInitiated(
                        String endpointId,
                        ConnectionInfo connectionInfo
                ) {

                    Log.d(
                            TAG,
                            "Connection initiated: "
                                    + endpointId
                    );

                    connectionStates.put(
                            endpointId,
                            ConnectionState.CONNECTING
                    );

                    if (callback != null) {

                        callback.onConnectionInitiated(
                                endpointId,
                                connectionInfo.getEndpointName()
                        );
                    }

                    acceptConnection(endpointId);
                }

                @Override
                public void onConnectionResult(
                        String endpointId,
                        ConnectionResolution result
                ) {

                    if (result.getStatus()
                            .isSuccess()) {

                        connectionStates.put(
                                endpointId,
                                ConnectionState.CONNECTED
                        );

                        Log.d(
                                TAG,
                                "Connected: "
                                        + endpointId
                        );

                        if (callback != null) {

                            callback.onConnected(
                                    endpointId
                            );
                        }

                    } else {

                        connectionStates.put(
                                endpointId,
                                ConnectionState.FAILED
                        );

                        Log.e(
                                TAG,
                                "Connection failed: "
                                        + endpointId
                        );

                        if (callback != null) {

                            callback.onConnectionFailed(
                                    endpointId
                            );
                        }
                    }
                }

                @Override
                public void onDisconnected(
                        String endpointId
                ) {

                    connectionStates.put(
                            endpointId,
                            ConnectionState.DISCONNECTED
                    );

                    Log.d(
                            TAG,
                            "Disconnected: "
                                    + endpointId
                    );

                    if (callback != null) {

                        callback.onDisconnected(
                                endpointId
                        );
                    }
                }
            };

    private final PayloadCallback payloadCallback =
            new PayloadCallback() {

                @Override
                public void onPayloadReceived(
                        String endpointId,
                        Payload payload
                ) {

                    if (payload.getType()
                            != Payload.Type.BYTES) {

                        return;
                    }

                    byte[] bytes =
                            payload.asBytes();

                    if (bytes == null) {
                        return;
                    }

                    if (bytes.length >
                            MeshConfig.MAX_MESSAGE_SIZE) {

                        Log.e(
                                TAG,
                                "Received payload too large"
                        );

                        return;
                    }

                    Log.d(
                            TAG,
                            "Payload received from "
                                    + endpointId
                    );

                    if (callback != null) {

                        callback.onPayloadReceived(
                                endpointId,
                                bytes
                        );
                    }
                }

                @Override
                public void onPayloadTransferUpdate(
                        String endpointId,
                        PayloadTransferUpdate update
                ) {

                    // Reserved for future transfer progress handling.
                }
            };
}