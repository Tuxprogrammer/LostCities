package com.tuxprogrammer.lostcities.worldgen.height;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Height sampling per group, against docs/original-spec/03-terrain/heightmap-sampling.md and P03-3. */
public class ChunkHeightsTest {

    /** An oracle that records where it is asked and answers X + Z. */
    private static final class Recording implements TerrainHeightOracle {

        final List<int[]> asked = new ArrayList<>();

        @Override
        public int height(int blockX, int blockZ) {
            asked.add(new int[] { blockX, blockZ });
            return 64 + Math.floorMod(blockX + blockZ, 50);
        }
    }

    @Test
    public void oneSampleAtBlockEightEightOfTheGroupCenter() {
        Recording oracle = new Recording();
        ChunkHeights heights = new ChunkHeights(oracle, 3, ChunkHeights.Listener.NONE);
        int height = heights.height(4, 5);
        assertEquals(1, oracle.asked.size());
        // Group (1, 1) covers chunks 3..5; its center chunk is 4, block 4 × 16 + 8 = 72.
        assertEquals(72, oracle.asked.get(0)[0]);
        assertEquals(72, oracle.asked.get(0)[1]);
        assertEquals(height, heights.height(3, 3), "every chunk of the group shares the sample");
        assertEquals(height, heights.height(5, 4));
        assertEquals(1, oracle.asked.size(), "answered from the cache");
    }

    @Test
    public void groupsNextToTheAxesAreFullSize() {
        ChunkHeights heights = new ChunkHeights(new Recording(), 3, ChunkHeights.Listener.NONE);
        // floorDiv: chunks -3, -2, -1 form one group whose center is -2; upstream's truncation made -1 and 0 meet.
        assertEquals(-2, heights.sampleCenter(-1));
        assertEquals(-2, heights.sampleCenter(-3));
        assertEquals(1, heights.sampleCenter(0));
        assertEquals(1, heights.sampleCenter(2));
    }

    @Test
    public void oracleTimingsReachTheListener() {
        List<Long> timings = new ArrayList<>();
        ChunkHeights heights = new ChunkHeights(new Recording(), 1, timings::add);
        heights.height(0, 0);
        heights.height(1, 0);
        assertEquals(2, timings.size());
    }

    @Test
    public void doesNotReproduceTERR10_heightsNeverChangeOnceSampled() {
        ChunkHeights heights = new ChunkHeights(new Recording(), 3, ChunkHeights.Listener.NONE);
        int first = heights.height(10, -7);
        for (int i = 0; i < ChunkHeights.CAPACITY * 2; i++) {
            heights.height(i * 3, -i * 3);
        }
        assertEquals(first, heights.height(10, -7), "eviction and other chunks never change a height");
    }

    @Test
    public void doesNotReproduceTERR34_oneSamplingPathWhateverTheCacheHolds() {
        Recording oracle = new Recording();
        ChunkHeights cold = new ChunkHeights(oracle, 3, ChunkHeights.Listener.NONE);
        ChunkHeights warm = new ChunkHeights(oracle, 3, ChunkHeights.Listener.NONE);
        for (int i = -20; i < 20; i++) {
            warm.height(i, i);
        }
        for (int i = -20; i < 20; i++) {
            assertEquals(cold.height(i, -i), warm.height(i, -i), "chunk " + i);
        }
        assertTrue(oracle.asked.size() > 0);
    }
}
