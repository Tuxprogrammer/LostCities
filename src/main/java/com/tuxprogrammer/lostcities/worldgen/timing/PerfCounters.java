package com.tuxprogrammer.lostcities.worldgen.timing;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Diagnostic counters for the chunk-generation profile: a name maps to a call count, the nanoseconds spent and the
 * longest single call. They are read by the {@code perf.sites} scenario and by nothing else.
 *
 * <p>
 * The counters are off unless the JVM is started with {@code -Dlostcitiesnh.perfCounters=true}, or with
 * {@code -Dlostcitiesnh.testHooks=true} (the {@code perf.sites} scenario needs them and is test-hooks only). The
 * switch is a {@code static final} read once at class load, so with it off every hook is one branch on a constant and
 * {@link #start()} does not even read the clock.
 *
 * <p>
 * Counting is safe from any thread: oracles are reached from the server thread, the prefetcher and a planning worker.
 * Counts and totals are {@link LongAdder}s and the maximum a compare-and-set loop; the table is a
 * {@link ConcurrentHashMap}. A report taken while other threads count is a consistent-enough snapshot for diagnostics,
 * not an atomic one.
 */
public final class PerfCounters {

    /** System property that switches the counters on. */
    public static final String PROPERTY = "lostcitiesnh.perfCounters";

    private static final String TEST_HOOKS_PROPERTY = "lostcitiesnh.testHooks";

    /** True when the counters are on for this JVM; fixed at class load. */
    public static final boolean ENABLED = Boolean.getBoolean(PROPERTY) || Boolean.getBoolean(TEST_HOOKS_PROPERTY);

    private static final Table GLOBAL = new Table();

    private PerfCounters() {}

    /**
     * Starts timing a call.
     *
     * @return the start stamp to pass to {@link #add(String, long)}, or 0 when the counters are off
     */
    public static long start() {
        return ENABLED ? System.nanoTime() : 0L;
    }

    /**
     * Records one call that took {@code System.nanoTime() - started}; does nothing when the counters are off.
     *
     * @param name    counter name, for example {@code "rl.compute"}
     * @param started the stamp returned by {@link #start()}
     */
    public static void add(String name, long started) {
        if (ENABLED) {
            GLOBAL.add(name, System.nanoTime() - started);
        }
    }

    /**
     * Records a call with no timing; does nothing when the counters are off.
     *
     * @param name counter name
     */
    public static void hit(String name) {
        if (ENABLED) {
            GLOBAL.hit(name);
        }
    }

    /** Clears every counter; does nothing when the counters are off. */
    public static void reset() {
        if (ENABLED) {
            GLOBAL.reset();
        }
    }

    /**
     * Formats the counters, busiest first.
     *
     * @return one line per counter sorted by total time (name, count, total ms, mean us, max ms); empty when off
     */
    public static List<String> report() {
        return ENABLED ? GLOBAL.report() : new ArrayList<String>();
    }

    /** A thread-safe table of named counters; the static methods above use one when the switch is on. */
    public static final class Table {

        private final ConcurrentMap<String, Stat> stats = new ConcurrentHashMap<>();

        /**
         * Records one call of {@code nanos} nanoseconds.
         *
         * @param name  counter name
         * @param nanos duration of the call
         */
        public void add(String name, long nanos) {
            Stat stat = stat(name);
            stat.count.increment();
            stat.nanos.add(nanos);
            long seen = stat.max.get();
            while (nanos > seen && !stat.max.compareAndSet(seen, nanos)) {
                seen = stat.max.get();
            }
        }

        /**
         * Records an untimed call.
         *
         * @param name counter name
         */
        public void hit(String name) {
            stat(name).count.increment();
        }

        /**
         * @param name counter name
         * @return the number of calls recorded, 0 for an unknown name
         */
        public long count(String name) {
            Stat stat = stats.get(name);
            return stat == null ? 0L : stat.count.sum();
        }

        /**
         * @param name counter name
         * @return the nanoseconds recorded, 0 for an unknown name
         */
        public long totalNanos(String name) {
            Stat stat = stats.get(name);
            return stat == null ? 0L : stat.nanos.sum();
        }

        /**
         * @param name counter name
         * @return the longest single call in nanoseconds, 0 for an unknown name
         */
        public long maxNanos(String name) {
            Stat stat = stats.get(name);
            return stat == null ? 0L : stat.max.get();
        }

        /** Clears every counter. */
        public void reset() {
            stats.clear();
        }

        /**
         * @return one line per counter sorted by total time: name, count, total ms, mean us, max ms
         */
        public List<String> report() {
            List<String> names = new ArrayList<>(stats.keySet());
            names.sort((a, b) -> Long.compare(totalNanos(b), totalNanos(a)));
            List<String> lines = new ArrayList<>();
            for (String name : names) {
                long n = count(name);
                long total = totalNanos(name);
                lines.add(
                    String.format(
                        Locale.ROOT,
                        "%-28s n=%7d total=%9.2fms mean=%8.1fus max=%7.2fms",
                        name,
                        n,
                        total / 1e6,
                        n == 0 ? 0.0 : total / 1e3 / n,
                        maxNanos(name) / 1e6));
            }
            return lines;
        }

        private Stat stat(String name) {
            Stat stat = stats.get(name);
            return stat != null ? stat : stats.computeIfAbsent(name, k -> new Stat());
        }
    }

    private static final class Stat {

        final LongAdder count = new LongAdder();
        final LongAdder nanos = new LongAdder();
        final AtomicLong max = new AtomicLong();
    }
}
