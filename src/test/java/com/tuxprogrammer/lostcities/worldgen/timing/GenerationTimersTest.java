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
    public void anInvocationAGcPauseFellIntoIsSetApartFromB4ButCountsInB3() {
        long[] collections = { 0 };
        GenerationTimers timers = new GenerationTimers(() -> collections[0]);
        for (int i = 0; i < 100; i++) {
            timers.record(GenerationTimers.Series.PROVIDE, TerrainPhase.ChunkClass.CITY, 8_000_000L);
            timers.record(GenerationTimers.Series.POPULATE, TerrainPhase.ChunkClass.CITY, 20_000_000L);
            timers.recordPhase(
                GenerationTimers.Series.TERRAIN,
                TerrainPhase.ChunkClass.CITY,
                i,
                0,
                1_000_000L,
                timers.mark());
        }
        GenerationTimers.PhaseMark mark = timers.mark();
        collections[0]++;
        timers.recordPhase(GenerationTimers.Series.PREFETCH, null, 3, 4, 45_000_000L, mark);
        List<GenerationTimers.Verdict> verdicts = timers.budgets(0);
        assertTrue(
            verdicts.get(3)
                .met(),
            verdicts.get(3)
                .toString());
        assertTrue(
            verdicts.get(3)
                .detail()
                .contains("1 invocations a GC pause fell into set apart"),
            verdicts.get(3)
                .toString());
        assertTrue(
            verdicts.get(2)
                .detail()
                .startsWith("overlay 145.0 ms"),
            "B3 counts it: " + verdicts.get(2));
        assertTrue(
            timers.report()
                .contains(
                    "slow prefetch all chunk=3,4 45.000 ms: 0 height samples 0.000 ms, structures 0.000 ms,"
                        + " a GC pause fell into it"),
            String.join("\n", timers.report()));
    }

    @Test
    public void prefetchTicksCountInB3AndB4AndTheSlowestAreNamed() {
        GenerationTimers timers = new GenerationTimers(() -> 0L);
        for (int i = 0; i < 100; i++) {
            timers.record(GenerationTimers.Series.PROVIDE, TerrainPhase.ChunkClass.CITY, 8_000_000L);
            timers.record(GenerationTimers.Series.POPULATE, TerrainPhase.ChunkClass.CITY, 20_000_000L);
            GenerationTimers.PhaseMark mark = timers.mark();
            timers.recordOracle(700_000L);
            timers.recordPhase(GenerationTimers.Series.TERRAIN, TerrainPhase.ChunkClass.CITY, i, 0, 1_000_000L, mark);
        }
        GenerationTimers.PhaseMark mark = timers.mark();
        for (int i = 0; i < 20; i++) {
            timers.recordOracle(700_000L);
        }
        timers.recordPhase(GenerationTimers.Series.PREFETCH, null, 7, -9, 14_000_000L, mark);
        List<GenerationTimers.Verdict> verdicts = timers.budgets(0);
        assertFalse(
            verdicts.get(3)
                .met(),
            "a 14 ms prefetch tick fails B4: " + verdicts.get(3));
        assertTrue(
            verdicts.get(2)
                .detail()
                .startsWith("overlay 114.0 ms"),
            "B3 counts the prefetcher: " + verdicts.get(2));
        List<String> report = timers.report();
        assertTrue(
            report.contains("slow prefetch all chunk=7,-9 14.000 ms: 20 height samples 14.000 ms, structures 0.000 ms"),
            String.join("\n", report));
        long slow = report.stream()
            .filter(line -> line.startsWith("slow "))
            .count();
        assertEquals(GenerationTimers.SLOWEST_KEPT, slow, "the slowest are kept, no more");
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
        assertFalse(
            verdicts.get(0)
                .met(),
            "B1");
        assertFalse(
            verdicts.get(3)
                .met(),
            "B4");
        assertFalse(
            timers.budgets(64L * 1024 * 1024)
                .get(4)
                .met(),
            "B5");
    }
}
