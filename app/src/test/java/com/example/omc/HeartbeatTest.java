package com.example.omc;

import com.example.omc.discovery.Peer;
import com.example.omc.heartbeat.HeartbeatManager;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for mesh heartbeat keepalive and dead-peer timeout detection (FR-7).
 */
public class HeartbeatTest {

    @Test
    public void testIntervalAndTimeoutBounds() {
        HeartbeatManager manager = new HeartbeatManager(null);

        // Test safe bounds enforcement (T_dead >= 2 * T_hb)
        manager.setIntervalMs(3000L);
        assertEquals(3000L, manager.getIntervalMs());
        assertTrue(manager.getTimeoutMs() >= 2 * manager.getIntervalMs());

        // Test lower bound clamping (minimum 2000 ms)
        manager.setIntervalMs(500L);
        assertEquals(2000L, manager.getIntervalMs());

        // Test upper bound clamping (maximum 10000 ms)
        manager.setIntervalMs(25000L);
        assertEquals(10000L, manager.getIntervalMs());
    }

    @Test
    public void testDeadPeerDetectionWhenTimedOut() {
        AtomicBoolean deadPeerDetected = new AtomicBoolean(false);

        HeartbeatManager manager = new HeartbeatManager(new HeartbeatManager.HeartbeatListener() {
            @Override
            public void onSendHeartbeat() {}

            @Override
            public void onDeadPeerDetected(Peer peer) {
                deadPeerDetected.set(true);
            }
        });

        manager.setIntervalMs(2000L);
        manager.start();

        Peer stalePeer = new Peer("endpoint-123", "Device-Alpha");
        stalePeer.setConnected(true);
        // Stale timestamp older than timeout (10 seconds ago)
        stalePeer.setLastSeen(System.currentTimeMillis() - 20000L);

        List<Peer> peers = new ArrayList<>();
        peers.add(stalePeer);

        manager.checkDeadPeers(peers);
        assertTrue("Dead peer must be flagged when last seen exceeds timeout threshold", deadPeerDetected.get());

        manager.stop();
    }

    @Test
    public void testActivePeerIsNotMarkedDead() {
        AtomicBoolean deadPeerDetected = new AtomicBoolean(false);

        HeartbeatManager manager = new HeartbeatManager(new HeartbeatManager.HeartbeatListener() {
            @Override
            public void onSendHeartbeat() {}

            @Override
            public void onDeadPeerDetected(Peer peer) {
                deadPeerDetected.set(true);
            }
        });

        manager.setIntervalMs(4000L);
        manager.start();

        Peer activePeer = new Peer("endpoint-456", "Device-Beta");
        activePeer.setConnected(true);
        activePeer.updateLastSeen(); // fresh timestamp right now

        List<Peer> peers = new ArrayList<>();
        peers.add(activePeer);

        manager.checkDeadPeers(peers);
        assertFalse("Active peer with recent keepalive must NOT be flagged as dead", deadPeerDetected.get());

        manager.stop();
    }
}
