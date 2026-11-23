package com.tuxprogrammer.lostcities.layout.perf;

/**
 * Exclusive-time spans for the chunk-load breakdown of {@code perf.sites} (D-perf3-1): a span's own time, without the
 * spans nested inside it, is added to the {@link PerfCounters} counter {@code "span." + name}. Because every nested
 * span is taken out of its parent, the exclusive times of a root span and everything under it add up to the root's
 * whole time, so a chunk load splits into named parts with nothing counted twice: a cascading load inside a
 * population counts as a load, not as population.
 *
 * <p>
 * Spans are kept per thread and are off with the counters ({@link PerfCounters#ENABLED}); then {@link #enter} and
 * {@link #exit} are one branch on a constant. They measure only: nothing reads them back during generation.
 */
public final class PerfSpans {

    /** Prefix of the counters the spans record into. */
    public static final String PREFIX = "span.";

    private static final ThreadLocal<Stack> STACKS = new ThreadLocal<Stack>() {

        @Override
        protected Stack initialValue() {
            return new Stack();
        }
    };

    private PerfSpans() {}

    /**
     * Opens a span on the current thread; does nothing when the counters are off.
     *
     * @param name the span's name, the counter suffix its exclusive time goes to
     */
    public static void enter(String name) {
        if (PerfCounters.ENABLED) {
            STACKS.get()
                .enter(name, System.nanoTime());
        }
    }

    /**
     * Closes the innermost open span of that name on the current thread and records its exclusive time. Spans opened
     * inside it and never closed (a handler that threw) are closed with it and recorded too.
     *
     * @param name the name given to {@link #enter}
     */
    public static void exit(String name) {
        if (PerfCounters.ENABLED) {
            STACKS.get()
                .exit(name, System.nanoTime(), PerfSpans::record);
        }
    }

    /**
     * Returns whether the current thread has a span open, for callers that only time the outermost of something.
     *
     * @return {@code true} when a span is open on this thread; always {@code false} when the counters are off
     */
    public static boolean open() {
        return PerfCounters.ENABLED && STACKS.get()
            .depth() > 0;
    }

    /**
     * Formats a whole split into named parts: each part's milliseconds and share of the whole, then what no part
     * covers ({@code outside}, the whole less the parts).
     *
     * @param wholeNanos the time being split, in nanoseconds
     * @param names      the parts' names
     * @param nanos      the parts' times, in the order of {@code names}
     * @return for example {@code "1000.0 ms = a 600.0 (60.0 %) + b 300.0 (30.0 %) + outside 100.0 (10.0 %)"}
     */
    public static String split(long wholeNanos, String[] names, long[] nanos) {
        StringBuilder line = new StringBuilder(String.format(java.util.Locale.ROOT, "%.1f ms =", wholeNanos / 1e6));
        long rest = wholeNanos;
        for (int i = 0; i < names.length; i++) {
            line.append(i == 0 ? " " : " + ")
                .append(part(names[i], nanos[i], wholeNanos));
            rest -= nanos[i];
        }
        line.append(names.length == 0 ? " " : " + ")
            .append(part("outside", rest, wholeNanos));
        return line.toString();
    }

    private static String part(String name, long nanos, long wholeNanos) {
        double share = wholeNanos == 0 ? 0.0 : 100.0 * nanos / wholeNanos;
        return String.format(java.util.Locale.ROOT, "%s %.1f (%.1f %%)", name, nanos / 1e6, share);
    }

    private static void record(String name, long exclusiveNanos) {
        PerfCounters.addNanos(PREFIX + name, exclusiveNanos);
    }

    /** Receives a closed span's exclusive time. */
    public interface Sink {

        /**
         * Records one closed span.
         *
         * @param name           the span's name
         * @param exclusiveNanos its time less the time of the spans closed inside it
         */
        void closed(String name, long exclusiveNanos);
    }

    /**
     * One thread's open spans, with an injected clock so tests drive it. Not thread-safe; deeper than
     * {@link #MAX_DEPTH} spans are ignored rather than grown into.
     */
    public static final class Stack {

        /** The most spans open at once; a deeper {@link #enter} is counted and ignored with its {@link #exit}. */
        public static final int MAX_DEPTH = 64;

        private final String[] names = new String[MAX_DEPTH];
        private final long[] starts = new long[MAX_DEPTH];
        private final long[] children = new long[MAX_DEPTH];
        private int depth;
        private int overflow;

        /**
         * Opens a span.
         *
         * @param name its name
         * @param now  the clock, in nanoseconds
         */
        public void enter(String name, long now) {
            if (depth >= MAX_DEPTH) {
                overflow++;
                return;
            }
            names[depth] = name;
            starts[depth] = now;
            children[depth] = 0L;
            depth++;
        }

        /**
         * Closes the innermost open span named {@code name}, and any opened inside it, reporting each to the sink. A
         * name with no open span is ignored.
         *
         * @param name the span's name
         * @param now  the clock, in nanoseconds
         * @param sink receives each closed span
         */
        public void exit(String name, long now, Sink sink) {
            if (overflow > 0) {
                overflow--;
                return;
            }
            int target = depth - 1;
            while (target >= 0 && !names[target].equals(name)) {
                target--;
            }
            if (target < 0) {
                return;
            }
            while (depth > target) {
                depth--;
                long total = now - starts[depth];
                sink.closed(names[depth], total - children[depth]);
                if (depth > 0) {
                    children[depth - 1] += total;
                }
                names[depth] = null;
            }
        }

        /**
         * Returns how many spans are open.
         *
         * @return the depth, 0 when none is open
         */
        public int depth() {
            return depth;
        }
    }
}
