package com.example.omc;

import com.example.omc.mesh.MeshConfig;
import com.example.omc.protocol.MessageType;
import com.example.omc.protocol.OMCHeader;
import com.example.omc.protocol.OMCMessage;
import com.example.omc.protocol.ProtocolManager;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ProtocolTest {

    private ProtocolManager protocolManager;

    @Before
    public void setUp() {
        protocolManager = new ProtocolManager();
    }

    @Test
    public void testCreateAndSerializeMessage() {
        OMCMessage message = protocolManager.createMessage(
                "node-A",
                "node-B",
                MessageType.CHAT,
                "Hello Mesh"
        );

        assertNotNull(message);
        assertNotNull(message.getHeader());
        assertEquals("node-A", message.getHeader().getSourceId());
        assertEquals("node-B", message.getHeader().getDestinationId());
        assertEquals(MessageType.CHAT, message.getHeader().getMessageType());
        assertEquals("Hello Mesh", message.getPayload());

        String json = protocolManager.serialize(message);
        assertNotNull(json);
        assertTrue(json.contains("Hello Mesh"));
        assertTrue(json.contains("node-A"));
    }

    @Test
    public void testDeserializeAndValidateMessage() {
        OMCMessage original = protocolManager.createMessage(
                "node-A",
                "node-B",
                MessageType.CHAT,
                "Test Payload"
        );
        String json = protocolManager.serialize(original);

        OMCMessage parsed = protocolManager.deserialize(json);
        assertNotNull(parsed);
        assertEquals(original.getHeader().getMessageId(), parsed.getHeader().getMessageId());
        assertEquals("Test Payload", parsed.getPayload());
        assertTrue(protocolManager.validate(parsed));
    }

    @Test
    public void testCreateHelloMessage() {
        OMCMessage hello = protocolManager.createHello("node-X", "Device-Alpha");
        assertNotNull(hello);
        assertEquals(MessageType.HELLO, hello.getHeader().getMessageType());
        assertEquals("Device-Alpha", hello.getPayload());
        assertEquals(1, hello.getHeader().getTtl());
    }

    @Test
    public void testCreateAckMessage() {
        String msgId = "msg-12345";
        OMCMessage ack = protocolManager.createAck("node-B", "node-A", msgId);
        assertNotNull(ack);
        assertEquals(MessageType.ACK, ack.getHeader().getMessageType());
        assertEquals(msgId, ack.getHeader().getAckForMessageId());
    }

    @Test
    public void testValidationFailureOnCorruptedData() {
        assertFalse(protocolManager.validate(null));

        OMCMessage badVersion = new OMCMessage(
                new OMCHeader("OMC/99.0", "id", "src", "dst", MessageType.CHAT, 5, System.currentTimeMillis()),
                "text"
        );
        assertFalse(protocolManager.validate(badVersion));

        assertNull(protocolManager.deserialize("invalid json string"));
    }
}
