package com.example.omc;

import com.example.omc.protocol.MessageType;
import com.example.omc.protocol.OMCHeader;
import com.example.omc.protocol.OMCMessage;
import com.example.omc.protocol.ProtocolManager;
import com.example.omc.security.MessageSigner;
import com.example.omc.security.MessageVerifier;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests verifying cryptographic packet signing and tamper detection (FR-8.2).
 */
public class SecurityTest {

    private ProtocolManager protocolManager;
    private MessageSigner signer;
    private MessageVerifier verifier;

    @Before
    public void setUp() {
        protocolManager = new ProtocolManager();
        signer = new MessageSigner();
        verifier = new MessageVerifier();
    }

    @Test
    public void testValidSignatureVerification() {
        OMCMessage message = protocolManager.createMessage("Alice", "Bob", MessageType.CHAT, "Urgent: Meet at extraction point");
        String signature = signer.sign(message);
        assertNotNull(signature);
        assertFalse(signature.isEmpty());

        message.setSignature(signature);
        message.getHeader().setFlags(OMCHeader.FLAG_SIGNED);

        assertTrue("Valid signature must pass verification", verifier.verify(message));
    }

    @Test
    public void testTamperedPayloadIsRejected() {
        OMCMessage message = protocolManager.createMessage("Alice", "Bob", MessageType.CHAT, "Safe at Gate 3");
        String signature = signer.sign(message);
        message.setSignature(signature);

        // Tamper with the message payload
        message.setPayload("Tampered message from adversary");

        assertFalse("Tampered payload must fail signature verification", verifier.verify(message));
    }

    @Test
    public void testTamperedDestinationIsRejected() {
        OMCMessage message = protocolManager.createMessage("Alice", "Bob", MessageType.CHAT, "Medic needed");
        String signature = signer.sign(message);
        message.setSignature(signature);

        // Tamper with destination ID
        message.getHeader().setDestinationId("Eve-Malicious");

        assertFalse("Tampered destination must fail signature verification", verifier.verify(message));
    }

    @Test
    public void testTamperedMessageIdIsRejected() {
        OMCMessage message = protocolManager.createMessage("Alice", "Bob", MessageType.CHAT, "Payload OK");
        String signature = signer.sign(message);
        message.setSignature(signature);

        // Tamper with message ID
        message.getHeader().setMessageId("fake-uuid-0000");

        assertFalse("Tampered message ID must fail signature verification", verifier.verify(message));
    }

    @Test
    public void testUnsignedMessageFailsVerification() {
        OMCMessage message = protocolManager.createMessage("Alice", "Bob", MessageType.CHAT, "No signature attached");
        assertFalse("Unsigned message must return false on verify", verifier.verify(message));

        message.setSignature("");
        assertFalse("Empty signature must return false", verifier.verify(message));
    }

    @Test
    public void testMismatchedSecretKeyFails() {
        MessageSigner keySigner = new MessageSigner("KEY-GROUP-A");
        MessageVerifier keyVerifier = new MessageVerifier("KEY-GROUP-B");

        OMCMessage message = protocolManager.createMessage("Alice", "Bob", MessageType.CHAT, "Secret handshake");
        message.setSignature(keySigner.sign(message));

        assertFalse("Signature signed with different key must fail verification", keyVerifier.verify(message));
    }
}
