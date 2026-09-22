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

        OMCHeader header =
                new OMCHeader(
                        MeshConfig.PROTOCOL_VERSION,
                        UUID.randomUUID().toString(),
                        sourceId,
                        destinationId,
                        messageType,
                        MeshConfig.DEFAULT_TTL,
                        System.currentTimeMillis()
                );

        return new OMCMessage(
                header,
                payload
        );
    }

    public String serialize(
            OMCMessage message
    ) {

        if (message == null) {
            return null;
        }

        return gson.toJson(message);
    }

    public OMCMessage deserialize(
            String json
    ) {

        if (json == null || json.isEmpty()) {
            return null;
        }

        try {

            OMCMessage message =
                    gson.fromJson(
                            json,
                            OMCMessage.class
                    );

            if (!validate(message)) {
                return null;
            }

            return message;

        } catch (JsonSyntaxException e) {

            Log.e(
                    TAG,
                    "Invalid OMC message",
                    e
            );

            return null;
        }
    }

    public boolean validate(
            OMCMessage message
    ) {

        if (message == null) {
            return false;
        }

        if (message.getHeader() == null) {
            return false;
        }

        OMCHeader header =
                message.getHeader();

        if (!MeshConfig.PROTOCOL_VERSION
                .equals(header.getVersion())) {

            Log.e(
                    TAG,
                    "Unsupported protocol version"
            );

            return false;
        }

        if (header.getMessageId() == null ||
                header.getMessageId().isEmpty()) {

            return false;
        }

        if (header.getSourceId() == null ||
                header.getSourceId().isEmpty()) {

            return false;
        }

        if (header.getDestinationId() == null ||
                header.getDestinationId().isEmpty()) {

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

    public String serializeForTransport(
            OMCMessage message
    ) {

        return serialize(message);
    }

    public OMCMessage parseFromTransport(
            String json
    ) {

        return deserialize(json);
    }
}