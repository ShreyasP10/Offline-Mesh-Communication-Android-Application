package com.example.omc.security;

import com.example.omc.protocol.OMCHeader;
import com.example.omc.protocol.OMCMessage;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Signs OMC packets using HMAC-SHA256 (FR-8.2).
 * Ensures message integrity and authenticity across the mesh.
 */
public class MessageSigner {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    public static final String DEFAULT_MESH_KEY = "OMC-MESH-INTEGRITY-SECRET-KEY-V1";

    private final byte[] secretKeyBytes;

    public MessageSigner() {
        this(DEFAULT_MESH_KEY);
    }

    public MessageSigner(String secretKey) {
        if (secretKey == null || secretKey.trim().isEmpty()) {
            secretKey = DEFAULT_MESH_KEY;
        }
        this.secretKeyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Computes the HMAC-SHA256 signature for a message.
     * Signs: messageId + ":" + sourceId + ":" + destinationId + ":" + payload
     */
    public String sign(OMCMessage message) {
        if (message == null || message.getHeader() == null) {
            return null;
        }

        try {
            String dataToSign = buildDataToSign(message);
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKeyBytes, HMAC_ALGORITHM);
            mac.init(secretKeySpec);

            byte[] hmacBytes = mac.doFinal(dataToSign.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmacBytes);
        } catch (Exception e) {
            return null;
        }
    }

    public static String buildDataToSign(OMCMessage message) {
        OMCHeader h = message.getHeader();
        String msgId = h.getMessageId() != null ? h.getMessageId() : "";
        String src = h.getSourceId() != null ? h.getSourceId() : "";
        String dst = h.getDestinationId() != null ? h.getDestinationId() : "";
        String payload = message.getPayload() != null ? message.getPayload() : "";
        return msgId + ":" + src + ":" + dst + ":" + payload;
    }
}
