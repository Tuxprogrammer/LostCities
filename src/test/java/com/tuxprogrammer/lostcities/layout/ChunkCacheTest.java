package com.tuxprogrammer.lostcities.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * The layout's bounded caches, which replace upstream's wall-clock {@code TimedCache} (docs/new-spec/01-core/index.md,
 * "Utilities"; avoids CORE-22).
 */
public class ChunkCacheTest {

    /** Fills a chain of entries the way the planners do: look up, compute (which may fill others), then store. */
    private static int fill(ChunkCache<Integer> cache, int x) {
        Integer cached = cache.get(x, 0);
        if (cached != null) {
            return cached;
        }
        int value = x == 0 ? 0 : fill(cache, x - 1) + 1;
        return cache.put(x, 0, value);
    }

    @Test
    public void doesNotReproduceCORE22_aValueThatFillsOtherEntriesWhileItIsComputedNeitherThrowsNorDeadlocks() {
        // Upstream computed inside ConcurrentHashMap.compute, where a supplier that touches the same cache throws
        // "Recursive update" or deadlocks. Here the value is computed outside the map.
        ChunkCache<Integer> cache = new ChunkCache<>(64);
        assertEquals(20, fill(cache, 20));
        for (int x = 0; x <= 20; x++) {
            assertEquals(Integer.valueOf(x), cache.get(x, 0), "entry " + x + " was stored while another was computed");
        }
    }

    @Test
    public void evictsTheLeastRecentlyUsedEntryBySizeNotByTime() {
        ChunkCache<String> cache = new ChunkCache<>(2);
        cache.put(0, 0, "a");
        cache.put(1, 0, "b");
        assertEquals("a", cache.get(0, 0));
        cache.put(2, 0, "c");
        assertEquals(2, cache.size());
        assertNull(cache.get(1, 0), "the entry used longest ago goes first");
        assertEquals("a", cache.get(0, 0));
        assertEquals("c", cache.get(2, 0));
    }

    @Test
    public void keysOfDifferentCoordinatesDiffer() {
        assertNotEquals(ChunkCache.key(1, 0), ChunkCache.key(0, 1));
        assertNotEquals(ChunkCache.key(-1, 0), ChunkCache.key(0, -1));
        assertNotEquals(ChunkCache.key(-1, -1), ChunkCache.key(0, -1));
    }
}
