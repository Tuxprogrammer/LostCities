package com.tuxprogrammer.lostcities.compat.rwg2;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/** {@link ChunkNoiseCache}: LRU bound, identical values on revisit and eviction, safe under concurrent callers. */
class ChunkNoiseCacheTest {

    /** A pure function of the coordinates, standing in for RWG2's noise. */
    private static float[] noise(int x, int z) {
        return new float[] { x * 3F + z, x - z * 2F, x * z, 64F + (x ^ z) };
    }

    @Test
    void aRevisitIsServedFromTheCacheWithTheSameValues() {
        AtomicInteger calls = new AtomicInteger();
        ChunkNoiseCache cache = new ChunkNoiseCache(8, (x, z) -> {
            calls.incrementAndGet();
            return noise(x, z);
        });
        assertArrayEquals(noise(3, -4), cache.get(3, -4));
        assertArrayEquals(noise(3, -4), cache.get(3, -4));
        assertEquals(1, calls.get());
    }

    @Test
    void anEvictedChunkIsRecomputedToTheSameValuesAndTheCacheStaysBounded() {
        AtomicInteger calls = new AtomicInteger();
        ChunkNoiseCache cache = new ChunkNoiseCache(4, (x, z) -> {
            calls.incrementAndGet();
            return noise(x, z);
        });
        for (int x = 0; x < 20; x++) {
            assertArrayEquals(noise(x, 7), cache.get(x, 7));
            assertTrue(cache.size() <= 4, "size " + cache.size());
        }
        int before = calls.get();
        assertArrayEquals(noise(0, 7), cache.get(0, 7));
        assertEquals(before + 1, calls.get(), "chunk 0 was evicted");
        assertArrayEquals(noise(19, 7), cache.get(19, 7));
        assertEquals(before + 1, calls.get(), "chunk 19 is still cached");
    }

    @Test
    void negativeCoordinatesDoNotCollide() {
        ChunkNoiseCache cache = new ChunkNoiseCache(16, ChunkNoiseCacheTest::noise);
        assertArrayEquals(noise(-1, 1), cache.get(-1, 1));
        assertArrayEquals(noise(1, -1), cache.get(1, -1));
        assertArrayEquals(noise(-1, 1), cache.get(-1, 1));
    }

    @Test
    void concurrentCallersAlwaysSeeTheRightValuesAndTheBoundHolds() throws Exception {
        ChunkNoiseCache cache = new ChunkNoiseCache(64, ChunkNoiseCacheTest::noise);
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int t = 0; t < 4; t++) {
                final int seed = t;
                Callable<Integer> job = () -> {
                    int wrong = 0;
                    for (int i = 0; i < 20000; i++) {
                        int x = (i * 7 + seed) % 200 - 100;
                        int z = (i * 13 + seed * 5) % 150 - 75;
                        if (!Arrays.equals(noise(x, z), cache.get(x, z))) {
                            wrong++;
                        }
                    }
                    return wrong;
                };
                results.add(pool.submit(job));
            }
            for (Future<Integer> result : results) {
                assertEquals(0, result.get());
            }
        } finally {
            pool.shutdownNow();
        }
        assertTrue(cache.size() <= 64, "size " + cache.size());
    }
}
