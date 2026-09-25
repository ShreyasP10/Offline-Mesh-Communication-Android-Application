package com.example.omc.security;

import com.example.omc.protocol.OMCMessage;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Verifies OMC packet signatures against tampering (FR-8.2).
 * Constant-time comparison ensures resistance to timing attacks.
 */
public class MessageVerifier {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final byte[] secretKeyBytes;

    public MessageVerifier() {
        this(MessageSigner.DEFAULT_MESH_KEY);
    }

    public MessageVerifier(String secretKey) {
        if (secretKey == null || secretKey.trim().isEmpty()) {
            secretKey = MessageSigner.DEFAULT_MESH_KEY;
        }
        this.secretKeyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Verifies the HMAC-SHA256 signature attached to the message.
     *
     * @param message The received OMC message
     * @return true if valid and authentic, false if tampered or missing signature
     */
    public boolean verify(OMCMessage message) {
        if (message == null || message.getHeader() == null) {
            return false;
        }

        String signature = message.getSignature();
        if (signature == null || signature.trim().isEmpty()) {
            return false;
        }

        try {
            String dataToSign = MessageSigner.buildDataToSign(message);
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKeyBytes, HMAC_ALGORITHM);
            mac.init(secretKeySpec);

            byte[] expectedHmac = mac.doFinal(dataToSign.getBytes(StandardCharsets.UTF_8));
            byte[] providedHmac = Base64.getDecoder().decode(signature.trim());

            return MessageDigest.isEqual(expectedHmac, providedHmac);
        } catch (Exception e) {
            return false;
        }
    }
}
