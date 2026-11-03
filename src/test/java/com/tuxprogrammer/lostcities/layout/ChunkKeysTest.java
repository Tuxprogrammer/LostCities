package com.tuxprogrammer.lostcities.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** The mixed chunk keys of the layout's hash maps (D265). */
public class ChunkKeysTest {

    @Test
    public void everyPairOfARegionHasItsOwnKey() {
        Set<Long> keys = new HashSet<>();
        for (int x = -200; x <= 200; x++) {
            for (int z = -200; z <= 200; z++) {
                assertTrue(keys.add(ChunkKeys.of(x, z)), "key of " + x + "," + z + " is taken");
            }
        }
        assertTrue(keys.add(ChunkKeys.of(Integer.MAX_VALUE, Integer.MIN_VALUE)));
        assertTrue(keys.add(ChunkKeys.of(Integer.MIN_VALUE, Integer.MAX_VALUE)));
    }

    @Test
    public void nearbyChunksSpreadOverHashBuckets() {
        // A region of 64 x 64 chunks in a table of 8,192 buckets, as HashMap spreads a Long's hash code. The plain
        // packing's x ^ z gives 128 distinct hashes, dozens of chunks per bucket; the mixed keys stay near one.
        Map<Integer, Integer> buckets = new HashMap<>();
        int worst = 0;
        for (int x = 0; x < 64; x++) {
            for (int z = 0; z < 64; z++) {
                int h = Long.hashCode(ChunkKeys.of(x, z));
                int bucket = (h ^ (h >>> 16)) & 8191;
                worst = Math.max(worst, buckets.merge(bucket, 1, Integer::sum));
            }
        }
        assertTrue(worst <= 8, "most chunks in one bucket: " + worst);
    }

    @Test
    public void theKeyIsAFunctionOfThePair() {
        assertEquals(ChunkKeys.of(12, -7), ChunkKeys.of(12, -7));
        assertTrue(ChunkKeys.of(1, 0) != ChunkKeys.of(0, 1));
    }
}
