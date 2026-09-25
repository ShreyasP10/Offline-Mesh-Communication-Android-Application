package com.example.omc;

import com.example.omc.mesh.SeenPacketCache;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SeenPacketCacheTest {

    @Test
    public void testDuplicateDetection() {
        SeenPacketCache cache = new SeenPacketCache(100);

        // First time seeing packet: not duplicate
        assertFalse(cache.isDuplicate("pkt-1"));
        assertTrue(cache.contains("pkt-1"));

        // Second time seeing same packet: duplicate
        assertTrue(cache.isDuplicate("pkt-1"));

        // Another packet: not duplicate
        assertFalse(cache.isDuplicate("pkt-2"));
        assertEquals(2, cache.size());
    }

    @Test
    public void testLruEviction() {
        // Cache with max capacity 3
        SeenPacketCache cache = new SeenPacketCache(3);

        cache.isDuplicate("pkt-1");
        cache.isDuplicate("pkt-2");
        cache.isDuplicate("pkt-3");
        assertEquals(3, cache.size());

        // Adding 4th should evict the eldest ("pkt-1")
        cache.isDuplicate("pkt-4");
        assertEquals(3, cache.size());
        assertFalse(cache.contains("pkt-1"));
        assertTrue(cache.contains("pkt-2"));
        assertTrue(cache.contains("pkt-3"));
        assertTrue(cache.contains("pkt-4"));
    }
}
