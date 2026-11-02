package com.tuxprogrammer.lostcities.worldgen.height;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The prefetcher's probe over the height caches (0005 §8, B4): a miss while probing throws and computes nothing, the
 * fetch caches exactly the value the oracle gives, and the caches hold a whole burst while it is fetched piece by
 * piece.
 */
public class HeightProbeTest {

    /** A whole-chunk oracle that counts its chunk computations and answers a pure function of the column. */
    private static final class WholeChunks implements TerrainHeightOracle {

        int chunks;

        @Override
        public int height(int blockX, int blockZ) {
            return 60 + Math.floorMod(blockX * 7 + blockZ * 13, 40);
        }

        @Override
        public boolean computesWholeChunks() {
            return true;
        }

        @Override
        public void chunkHeights(int chunkX, int chunkZ, int[] out) {
            chunks++;
            for (int i = 0; i < 256; i++) {
                out[i] = height((chunkX << 4) + (i >> 4), (chunkZ << 4) + (i & 15));
            }
        }
    }

    @Test
    public void aMissWhileProbingThrowsAndTheFetchCachesTheOraclesValue() {
        WholeChunks oracle = new WholeChunks();
        HeightProbe probe = new HeightProbe();
        ChunkHeights heights = new ChunkHeights(
            ChunkColumnCache.wrap(oracle, probe),
            3,
            ChunkHeights.Listener.NONE,
            probe);
        probe.setActive(true);
        assertThrows(HeightProbe.NotCached.class, () -> heights.height(10, -4));
        assertEquals(0, oracle.chunks, "a probe computes nothing");
        assertTrue(probe.fetchMissing());
        assertEquals(1, oracle.chunks, "the fetch computes the one chunk the sample needs");
        assertTrue(probe.active(), "the fetch turns the probe back on");
        int probed = heights.height(10, -4);
        probe.setActive(false);
        ChunkHeights plain = new ChunkHeights(new WholeChunks(), 3, ChunkHeights.Listener.NONE);
        assertEquals(plain.height(10, -4), probed, "the probed value is the oracle's");
        assertFalse(probe.fetchMissing(), "nothing left to fetch");
    }

    @Test
    public void aSampleTheColumnCacheHoldsNeedsNoFetch() {
        WholeChunks oracle = new WholeChunks();
        HeightProbe probe = new HeightProbe();
        TerrainHeightOracle columns = ChunkColumnCache.wrap(oracle, probe);
        ChunkHeights heights = new ChunkHeights(columns, 3, ChunkHeights.Listener.NONE, probe);
        // A structure oracle's ground estimate read a column of the group's center chunk (chunk 4, 4) first.
        columns.height(4 * 16 + 1, 4 * 16 + 2);
        probe.setActive(true);
        heights.height(3, 5);
        assertEquals(1, oracle.chunks);
        assertTrue(heights.isCached(5, 3));
    }

    @Test
    public void theColumnCacheHoldsAVillagesWorkingSet() {
        WholeChunks oracle = new WholeChunks();
        HeightProbe probe = new HeightProbe();
        ChunkColumnCache columns = new ChunkColumnCache(oracle, probe);
        // A large village's ground estimate reads about a hundred chunks; fetch them one by one, then probe them all.
        for (int x = -6; x < 6; x++) {
            for (int z = 20; z < 32; z++) {
                columns.height(x * 16 + 3, z * 16 + 9);
            }
        }
        probe.setActive(true);
        for (int x = -6; x < 6; x++) {
            for (int z = 20; z < 32; z++) {
                assertTrue(columns.isCached(x * 16, z * 16), "chunk " + x + "," + z);
                assertEquals(oracle.height(x * 16 + 15, z * 16 + 4), columns.height(x * 16 + 15, z * 16 + 4));
            }
        }
        assertEquals(144, oracle.chunks, "each chunk computed once");
    }

    @Test
    public void theGroupCacheKeepsABurstWhileItArrives() {
        HeightProbe probe = new HeightProbe();
        ChunkHeights heights = new ChunkHeights((x, z) -> x ^ z, 3, ChunkHeights.Listener.NONE, probe);
        // A village's lanes asked for about fifty groups at once; the prefetcher fetches them one per attempt.
        int fetched = 0;
        probe.setActive(true);
        for (int attempt = 0; attempt < 200; attempt++) {
            try {
                for (int gx = 0; gx < 8; gx++) {
                    for (int gz = 0; gz < 8; gz++) {
                        heights.height(gx * 3 - 40, gz * 3 + 7);
                    }
                }
                break;
            } catch (HeightProbe.NotCached missing) {
                probe.fetchMissing();
                fetched++;
            }
        }
        assertEquals(64, fetched, "every group fetched once, none evicted before the burst completed");
        assertEquals(64, heights.samples());
    }

    @Test
    public void probeMissesAreRecognized() {
        HeightProbe probe = new HeightProbe();
        assertTrue(HeightProbe.isProbe(probe.miss(() -> {})));
        assertFalse(HeightProbe.isProbe(new IllegalStateException()));
    }
}
