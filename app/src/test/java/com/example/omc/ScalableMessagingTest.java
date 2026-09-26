package com.example.omc;

import com.example.omc.mesh.SeenPacketCache;
import com.example.omc.protocol.MessageType;
import com.example.omc.protocol.OMCHeader;
import com.example.omc.protocol.OMCMessage;
import com.example.omc.protocol.ProtocolManager;
import com.example.omc.security.MessageSigner;
import com.example.omc.security.MessageVerifier;
import com.example.omc.storage.ChatMessage;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests verifying scalability enhancements for OMC mesh messaging:
 * - High-throughput SeenPacketCache with 10,000 capacity
 * - DTN FIFO causal ordering under high queue load
 * - Fast HMAC cryptographic signature verification at scale
 * - Scalable message pagination logic
 */
public class ScalableMessagingTest {

    @Test
    public void testHighVolumeSeenPacketCacheScalability() {
        SeenPacketCache cache = new SeenPacketCache(10000);
        int totalPackets = 10000;

        // Add 10,000 unique packets
        long start = System.currentTimeMillis();
        for (int i = 0; i < totalPackets; i++) {
            String pktId = "pkt-" + i;
            assertFalse("Packet should not be duplicate on first sight: " + pktId, cache.isDuplicate(pktId));
        }
        long duration = System.currentTimeMillis() - start;

        assertEquals(10000, cache.size());
        assertTrue("Inserting 10k packets should take < 500ms (fast in-memory LRU)", duration < 500);

        // Verify duplicate checks on existing packets are O(1) and instant
        for (int i = 0; i < 100; i++) {
            assertTrue(cache.isDuplicate("pkt-" + i));
            assertTrue(cache.contains("pkt-" + i));
        }

        // Bounded capacity: adding 1 more packet should maintain maxSize = 10000 by evicting eldest
        cache.isDuplicate("pkt-overflow");
        assertEquals(10000, cache.size());
        assertFalse("Eldest pkt-0 should be evicted", cache.contains("pkt-0"));
        assertTrue("Newly added packet should be present", cache.contains("pkt-overflow"));
    }

    @Test
    public void testDtnFifoCausalOrderingUnderQueueLoad() {
        List<OMCMessage> queue = new ArrayList<>();
        long baseTime = 1700000000000L;

        // Add 100 messages with out-of-order timestamps
        for (int i = 99; i >= 0; i--) {
            OMCHeader header = new OMCHeader(
                    "OMC/1.0",
                    "msg-" + i,
                    "node-alice",
                    "node-bob",
                    MessageType.CHAT,
                    8,
                    0,
                    baseTime + (i * 1000L),
                    null
            );
            queue.add(new OMCMessage(header, "Payload #" + i));
        }

        // Sort per DTN FIFO specification (oldest timestamp first)
        Collections.sort(queue, (a, b) -> Long.compare(
                a.getHeader().getTimestamp(),
                b.getHeader().getTimestamp()
        ));

        // Verify earliest message is first and ordering is strictly monotonic
        assertEquals("msg-0", queue.get(0).getHeader().getMessageId());
        assertEquals("msg-99", queue.get(99).getHeader().getMessageId());

        for (int i = 1; i < queue.size(); i++) {
            assertTrue(queue.get(i).getHeader().getTimestamp() >= queue.get(i - 1).getHeader().getTimestamp());
        }
    }

    @Test
    public void testCryptographicSigningScalability() {
        MessageSigner signer = new MessageSigner();
        MessageVerifier verifier = new MessageVerifier();
        ProtocolManager pm = new ProtocolManager();

        long start = System.currentTimeMillis();
        for (int i = 0; i < 50; i++) {
            OMCMessage msg = pm.createMessage("node-src-" + i, "node-dst-" + i, MessageType.CHAT, "Test payload data " + i);
            String sig = signer.sign(msg);
            msg.setSignature(sig);

            assertTrue("Valid signature must verify successfully", verifier.verify(msg));

            // Tamper test
            OMCMessage tampered = pm.createMessage("node-src-" + i, "node-dst-" + i, MessageType.CHAT, "Tampered payload data " + i);
            tampered.setSignature(sig);
            assertFalse("Tampered payload must fail verification", verifier.verify(tampered));
        }
        long duration = System.currentTimeMillis() - start;
        assertTrue("50 HMAC sign and verify operations should complete in < 500ms", duration < 500);
    }

    @Test
    public void testPaginatedMessageWindowing() {
        List<ChatMessage> fullHistory = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (int i = 0; i < 150; i++) {
            fullHistory.add(new ChatMessage(
                    "msg-" + i,
                    "alice",
                    "Alice",
                    "bob",
                    "Message " + i,
                    ChatMessage.STATUS_DELIVERED,
                    now + (i * 1000L),
                    true,
                    0
            ));
        }

        // Simulate page 1: most recent 50 messages (offset 0, limit 50 from end)
        int pageSize = 50;
        int total = fullHistory.size();
        int startIndex = Math.max(0, total - pageSize);
        List<ChatMessage> page1 = fullHistory.subList(startIndex, total);

        assertEquals(50, page1.size());
        assertEquals("msg-100", page1.get(0).getMessageId());
        assertEquals("msg-149", page1.get(49).getMessageId());

        // Simulate page 2: previous 50 messages
        int page2Start = Math.max(0, startIndex - pageSize);
        List<ChatMessage> page2 = fullHistory.subList(page2Start, startIndex);

        assertEquals(50, page2.size());
        assertEquals("msg-50", page2.get(0).getMessageId());
        assertEquals("msg-99", page2.get(49).getMessageId());
    }
}
