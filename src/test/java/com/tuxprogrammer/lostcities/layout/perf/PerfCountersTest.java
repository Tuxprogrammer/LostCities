package com.tuxprogrammer.lostcities.layout.perf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

/** The perf counters: off by default and free, correct when on, and additive across threads. */
public class PerfCountersTest {

    @Test
    void offByDefaultRecordsNothing() {
        // The unit-test JVM sets neither -Dlostcitiesnh.perfCounters nor testHooks.
        assertFalse(PerfCounters.ENABLED);
        assertEquals(0L, PerfCounters.start());
        PerfCounters.add("x", PerfCounters.start());
        PerfCounters.hit("y");
        assertTrue(
            PerfCounters.report()
                .isEmpty());
    }

    @Test
    void countsTotalsAndMax() {
        PerfCounters.Table table = new PerfCounters.Table();
        table.add("a", 3_000_000L);
        table.add("a", 1_000_000L);
        table.hit("b");
        assertEquals(2L, table.count("a"));
        assertEquals(4_000_000L, table.totalNanos("a"));
        assertEquals(3_000_000L, table.maxNanos("a"));
        assertEquals(1L, table.count("b"));
        assertEquals(0L, table.totalNanos("b"));
        assertEquals(0L, table.count("missing"));
    }

    @Test
    void reportSortsByTotalTimeAndResetClears() {
        PerfCounters.Table table = new PerfCounters.Table();
        table.add("small", 1_000L);
        table.add("big", 5_000_000L);
        List<String> lines = table.report();
        assertEquals(2, lines.size());
        assertTrue(
            lines.get(0)
                .startsWith("big"));
        table.reset();
        assertTrue(
            table.report()
                .isEmpty());
    }

    @Test
    void concurrentIncrementsAddUp() throws Exception {
        PerfCounters.Table table = new PerfCounters.Table();
        int threads = 8;
        int perThread = 20_000;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch go = new CountDownLatch(1);
        for (int t = 0; t < threads; t++) {
            final long id = t + 1;
            pool.submit(() -> {
                go.await();
                for (int i = 0; i < perThread; i++) {
                    table.add("shared", 10L);
                    table.hit("hits");
                }
                table.add("shared", id * 1_000L);
                return null;
            });
        }
        go.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));
        assertEquals((long) threads * (perThread + 1), table.count("shared"));
        long expected = (long) threads * perThread * 10L + 1_000L * (threads * (threads + 1) / 2);
        assertEquals(expected, table.totalNanos("shared"));
        assertEquals(threads * 1_000L, table.maxNanos("shared"));
        assertEquals((long) threads * perThread, table.count("hits"));
    }
}
