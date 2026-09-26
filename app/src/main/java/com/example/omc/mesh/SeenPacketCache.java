package com.example.omc.mesh;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Thread-safe Bounded LRU Cache for packet de-duplication to prevent broadcast storms
 * and routing loops per OMC-SDD §3.2 & §6.5.
 */
public class SeenPacketCache {

    private static final int DEFAULT_MAX_SIZE = 10000;

    private final Set<String> cache;

    public SeenPacketCache() {
        this(DEFAULT_MAX_SIZE);
    }

    public SeenPacketCache(final int maxSize) {
        Map<String, Boolean> lruMap = new LinkedHashMap<String, Boolean>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                return size() > maxSize;
            }
        };
        this.cache = Collections.synchronizedSet(Collections.newSetFromMap(lruMap));
    }

    /**
     * Checks if the packetId has already been seen.
     * If not seen, it adds it to the cache and returns false.
     * If already seen, returns true.
     */
    public synchronized boolean isDuplicate(String packetId) {
        if (packetId == null || packetId.isEmpty()) {
            return false;
        }
        if (cache.contains(packetId)) {
            return true;
        }
        cache.add(packetId);
        return false;
    }

    public synchronized boolean contains(String packetId) {
        return packetId != null && cache.contains(packetId);
    }

    public synchronized void add(String packetId) {
        if (packetId != null && !packetId.isEmpty()) {
            cache.add(packetId);
        }
    }

    public synchronized void clear() {
        cache.clear();
    }

    public synchronized int size() {
        return cache.size();
    }
}
