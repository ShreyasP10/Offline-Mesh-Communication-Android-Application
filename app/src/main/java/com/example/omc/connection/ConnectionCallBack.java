package com.example.omc.connection;

public interface ConnectionCallback {

    void onConnectionInitiated(
            String endpointId,
            String endpointName
    );

    void onConnected(
            String endpointId
    );

    void onConnectionFailed(
            String endpointId
    );

    void onDisconnected(
            String endpointId
    );

    void onPayloadReceived(
            String endpointId,
            byte[] payload
    );
}