package com.tuxprogrammer.lostcities.worldgen.timing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainPhase;

/** The fixed-bucket timers and budgets B1 to B5 (0005 §8). */
public class GenerationTimersTest {

    @Test
    public void quantilesAreWithinOneBucket() {
        Histogram histogram = new Histogram();
        for (int i = 1; i <= 1000; i++) {
            histogram.record(i * 10_000L);
        }
        assertEquals(1000, histogram.count());
        assertEquals(10_000_000L, histogram.max());
        long p50 = histogram.quantile(0.5);
        assertTrue(p50 >= 5_000_000L && p50 <= 5_000_000L * 9 / 8 + 1, "p50 " + p50);
        assertEquals(10_000_000L, histogram.quantile(1.0));
    }

    @Test
    public void bucketBoundsCoverEveryValue() {
        for (long value : new long[] { 0, 1, 1023, 1024, 1500, 123_456_789L, Long.MAX_VALUE / 2 }) {
            int bucket = Histogram.bucket(value);
            assertTrue(value <= Histogram.upperBound(bucket) || bucket == Histogram.BUCKETS - 1, "value " + value);
        }
    }

    @Test
    public void budgetsPassWhenTheOverlayIsCheap() {
        GenerationTimers timers = new GenerationTimers();
        for (int i = 0; i < 100; i++) {
            timers.record(GenerationTimers.Series.PROVIDE, TerrainPhase.ChunkClass.CITY, 8_000_000L);
            timers.record(GenerationTimers.Series.TERRAIN, TerrainPhase.ChunkClass.CITY, 1_000_000L);
            timers.record(GenerationTimers.Series.POPULATE, TerrainPhase.ChunkClass.CITY, 20_000_000L);
            timers.record(GenerationTimers.Series.POPULATION, TerrainPhase.ChunkClass.CITY, 100_000L);
            timers.record(GenerationTimers.Series.PROVIDE, TerrainPhase.ChunkClass.UNTOUCHED, 8_000_000L);
            timers.record(GenerationTimers.Series.POPULATE, TerrainPhase.ChunkClass.UNTOUCHED, 20_000_000L);
        }
        List<GenerationTimers.Verdict> verdicts = timers.budgets(4L * 1024 * 1024);
        for (GenerationTimers.Verdict verdict : verdicts) {
            assertTrue(verdict.met(), verdict.toString());
        }
    }

    @Test
    public void slowTerrainFailsB1AndB4() {
        GenerationTimers timers = new GenerationTimers();
        for (int i = 0; i < 100; i++) {
            timers.record(GenerationTimers.Series.PROVIDE, TerrainPhase.ChunkClass.CITY, 5_000_000L);
            timers.record(GenerationTimers.Series.TERRAIN, TerrainPhase.ChunkClass.CITY, 30_000_000L);
            timers.record(GenerationTimers.Series.POPULATE, TerrainPhase.ChunkClass.CITY, 20_000_000L);
            timers.record(GenerationTimers.Series.POPULATION, TerrainPhase.ChunkClass.CITY, 100_000L);
        }
        List<GenerationTimers.Verdict> verdicts = timers.budgets(0);
        assertFalse(verdicts.get(0)
            .met(), "B1");
        assertFalse(verdicts.get(3)
            .met(), "B4");
        assertFalse(timers.budgets(64L * 1024 * 1024)
            .get(4)
            .met(), "B5");
    }
}
