package com.example.omc.protocol;

import android.util.Log;

import com.example.omc.mesh.MeshConfig;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.UUID;

public class ProtocolManager {

    private static final String TAG = MeshConfig.LOG_MESH;

    private final Gson gson;

    public ProtocolManager() {
        gson = new Gson();
    }

    public OMCMessage createMessage(
            String sourceId,
            String destinationId,
            MessageType messageType,
            String payload
    ) {
        OMCHeader header = new OMCHeader(
                MeshConfig.PROTOCOL_VERSION,
                UUID.randomUUID().toString(),
                sourceId,
                destinationId,
                messageType,
                MeshConfig.DEFAULT_TTL,
                System.currentTimeMillis()
        );

        return new OMCMessage(header, payload);
    }

    public OMCMessage createAck(String sourceId, String destinationId, String ackForMessageId) {
        OMCHeader header = new OMCHeader(
                MeshConfig.PROTOCOL_VERSION,
                UUID.randomUUID().toString(),
                sourceId,
                destinationId,
                MessageType.ACK,
                MeshConfig.DEFAULT_TTL,
                0,
                System.currentTimeMillis(),
                ackForMessageId
        );
        return new OMCMessage(header, "ACK:" + ackForMessageId);
    }

    public OMCMessage createHello(String sourceId, String nodeName) {
        OMCHeader header = new OMCHeader(
                MeshConfig.PROTOCOL_VERSION,
                UUID.randomUUID().toString(),
                sourceId,
                "BROADCAST",
                MessageType.HELLO,
                1, // 1-hop link local only
                System.currentTimeMillis()
        );
        return new OMCMessage(header, nodeName);
    }

    public OMCMessage createHeartbeat(String sourceId) {
        OMCHeader header = new OMCHeader(
                MeshConfig.PROTOCOL_VERSION,
                UUID.randomUUID().toString(),
                sourceId,
                "BROADCAST",
                MessageType.HEARTBEAT,
                1, // 1-hop keepalive
                System.currentTimeMillis()
        );
        return new OMCMessage(header, "PING");
    }

    public String serialize(OMCMessage message) {
        if (message == null) {
            return null;
        }
        return gson.toJson(message);
    }

    public OMCMessage deserialize(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }

        try {
            OMCMessage message = gson.fromJson(json, OMCMessage.class);
            if (!validate(message)) {
                return null;
            }
            return message;
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "Invalid OMC message format: " + json, e);
            return null;
        }
    }

    public boolean validate(OMCMessage message) {
        if (message == null || message.getHeader() == null) {
            return false;
        }

        OMCHeader header = message.getHeader();

        if (!MeshConfig.PROTOCOL_VERSION.equals(header.getVersion())) {
            Log.e(TAG, "Unsupported protocol version: " + header.getVersion());
            return false;
        }

        if (header.getMessageId() == null || header.getMessageId().isEmpty()) {
            return false;
        }

        if (header.getSourceId() == null || header.getSourceId().isEmpty()) {
            return false;
        }

        if (header.getDestinationId() == null || header.getDestinationId().isEmpty()) {
            return false;
        }

        if (header.getMessageType() == null) {
            return false;
        }

        if (header.getTtl() < 0) {
            return false;
        }

        return true;
    }

    public String serializeForTransport(OMCMessage message) {
        return serialize(message);
    }

    public OMCMessage parseFromTransport(String json) {
        return deserialize(json);
    }
}