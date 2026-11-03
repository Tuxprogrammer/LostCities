package com.tuxprogrammer.lostcities.worldgen.overlay;

import java.util.Locale;
import java.util.function.BiPredicate;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.worldgen.height.HeightProbe;
import com.tuxprogrammer.lostcities.worldgen.plan.CachedCityPlan;
import com.tuxprogrammer.lostcities.worldgen.timing.GenerationTimers;

/**
 * Plans chunks a little ahead of generation, a few height samples per server tick, so the burst of samples a layout
 * cell needs when it first meets new ground (a village's lanes, a multibuilding area, a village's ground estimate)
 * is not paid inside one chunk's terrain phase (docs/design/0005-rwg-overlay.md §8, budget B4).
 *
 * <ul>
 * <li>Targets: every chunk {@link #LEAD} chunks (Chebyshev distance) from a chunk the overlay just generated, newest
 * generated chunk first. A region that grows one neighbor at a time, as players and the spiral of the budget scenario
 * grow it, brings each chunk within {@link #LEAD} of a generated chunk on that ring first, so every plan the terrain
 * phase will read (three chunks around it) was a target several rings earlier.</li>
 * <li>Probe: the target's plan is asked for with the world's {@link HeightProbe} active. A height sample or a column
 * that is not cached ends the attempt; the prefetcher fetches that one value with the probe off and asks again, until
 * the plan completes or the tick's {@link #TICK_BUDGET_NANOS} is spent. The plan is then cached, with every sample it
 * read.</li>
 * <li>Nothing depends on it: a plan is pure, so a prefetched plan is the plan the terrain phase would compute, and a
 * world that never ticks the prefetcher generates the same blocks. It only moves work out of the chunk phases.</li>
 * </ul>
 *
 * <p>
 * Its time is overlay work: the timers' {@code prefetch} series, counted in B3 and, per tick, in B4. One per wrapped
 * world. Server thread only. Side: server.
 */
public final class PlanPrefetcher {

    /**
     * How far ahead of a generated chunk the targets lie, in chunks: the terrain phase reads plans three chunks out,
     * so three rings of generation pass between a plan's prefetch and its first use. Measured on classic RWG, seed
     * 4242: 6 and 8 both keep every burst out of the chunk phases; 6 plans fewer chunks the front never reaches.
     */
    public static final int LEAD = 6;

    /** Most work one server tick gives the prefetcher, in nanoseconds; one sample may run past it. */
    public static final long TICK_BUDGET_NANOS = 3_000_000L;

    /**
     * Work after which a tick leaves a heavy fetch (a village's start, 3 to 9 ms once the JIT has compiled
     * VillageNames) to the start of the next tick, so the two never add up, in nanoseconds.
     */
    static final long HEAVY_START_NANOS = 500_000L;

    /** Generated chunks remembered as centers of targets; older ones are dropped when generation outruns the ticks. */
    static final int RECENT = 256;

    /**
     * Fetches one target may need before it is planned without the probe instead: a guard against two values that
     * keep evicting each other. A village's ground estimate reads about a hundred chunks; the first target in a fresh
     * highway planning region needs a step per hub cell and per height those cells read, up to a few thousand
     * (M06, {@code NetworkGate}).
     */
    static final int MAX_FETCHES_PER_TARGET = 8192;
    /** A fetch longer than this is named in the budget report by the class that asked for it, nanoseconds. */
    static final long SLOW_FETCH_NANOS = 2_000_000L;

    /** The longest slow fetch per source class, since the last reset; a handful of keys at most. */
    private final java.util.Map<String, Long> slowFetches = new java.util.TreeMap<>();
    /** Every fetch's time summed per source class, since the last reset: what each source costs budget B3. */
    private final java.util.Map<String, Long> fetchTotals = new java.util.TreeMap<>();

    private static final int PERIMETER = 8 * LEAD;

    private final CachedCityPlan plan;
    private final HeightProbe probe;
    private final GenerationTimers timers;
    private final BiPredicate<Integer, Integer> loaded;
    private final long[] recent = new long[RECENT];
    private int recentCount;
    private int recentHead;
    private boolean hasCenter;
    private int centerX;
    private int centerZ;
    private int perimeterIndex;
    private int fetches;
    private boolean warned;
    private long planned;
    private long fetchedTotal;
    private long failedAttemptNanos;
    private int tickAttempts;
    private int tickFetches;
    private long tickAttemptNanos;
    private long tickFetchNanos;
    private long tickLongestAttempt;
    private long worstTickNanos;
    private String worstTick = "none";

    /**
     * Creates the prefetcher of one world.
     *
     * @param plan   the world's cached plan
     * @param probe  the world's probe, shared with its height caches
     * @param timers the world's timers
     * @param loaded whether a chunk is loaded, so generated; its plan needs no prefetch
     */
    PlanPrefetcher(CachedCityPlan plan, HeightProbe probe, GenerationTimers timers,
        BiPredicate<Integer, Integer> loaded) {
        this.plan = plan;
        this.probe = probe;
        this.timers = timers;
        this.loaded = loaded;
    }

    /**
     * Notes a chunk the overlay generated: the chunks {@link #LEAD} around it become targets.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     */
    void generated(int chunkX, int chunkZ) {
        recent[recentHead] = key(chunkX, chunkZ);
        recentHead = (recentHead + 1) % RECENT;
        recentCount = Math.min(RECENT, recentCount + 1);
    }

    /** Returns how many generated chunks wait to be centers of targets. */
    int pending() {
        return recentCount + (hasCenter ? 1 : 0);
    }

    /**
     * Prefetches for one server tick, within {@link #TICK_BUDGET_NANOS}.
     *
     * @return how many values were fetched
     */
    int tick() {
        if (!hasCenter && recentCount == 0) {
            return 0;
        }
        GenerationTimers.PhaseMark mark = timers.mark();
        long start = System.nanoTime();
        tickAttempts = 0;
        tickFetches = 0;
        tickAttemptNanos = 0;
        tickFetchNanos = 0;
        tickLongestAttempt = 0;
        int fetched = 0;
        int lastX = 0;
        int lastZ = 0;
        boolean worked = false;
        boolean stop = false;
        while (!stop && System.nanoTime() - start < TICK_BUDGET_NANOS && nextTarget()) {
            int[] offset = perimeterOffset(perimeterIndex);
            int x = centerX + offset[0];
            int z = centerZ + offset[1];
            if (plan.isCached(x, z) || loaded.test(x, z)) {
                advance();
                continue;
            }
            worked = true;
            lastX = x;
            lastZ = z;
            switch (attempt(x, z, System.nanoTime() - start)) {
                case PLANNED -> advance();
                case FETCHED -> fetched++;
                case FETCHED_HEAVY -> {
                    fetched++;
                    stop = true;
                }
                default -> stop = true;
            }
        }
        if (worked) {
            long nanos = System.nanoTime() - start;
            timers.recordPhase(GenerationTimers.Series.PREFETCH, null, lastX, lastZ, nanos, mark);
            if (nanos > worstTickNanos) {
                worstTickNanos = nanos;
                worstTick = String.format(
                    Locale.ROOT,
                    "%.3f ms at chunk %d,%d: %d attempts %.3f ms (longest %.3f), %d fetches %.3f ms",
                    nanos / 1_000_000.0,
                    lastX,
                    lastZ,
                    tickAttempts,
                    tickAttemptNanos / 1_000_000.0,
                    tickLongestAttempt / 1_000_000.0,
                    tickFetches,
                    tickFetchNanos / 1_000_000.0);
            }
        }
        return fetched;
    }

    /** What one attempt did. */
    private enum Outcome {
        /** The plan completed and is cached. */
        PLANNED,
        /** One missing value was fetched. */
        FETCHED,
        /** One heavy value was fetched; the tick ends with it. */
        FETCHED_HEAVY,
        /** A heavy value is missing and the tick has already worked; it waits for the start of the next tick. */
        DEFERRED_HEAVY
    }

    /**
     * Asks for one target's plan with the probe active, and fetches what it misses.
     *
     * @param elapsed how long this tick has worked so far, in nanoseconds
     * @return what happened
     */
    private Outcome attempt(int x, int z, long elapsed) {
        if (fetches >= MAX_FETCHES_PER_TARGET) {
            if (!warned) {
                warned = true;
                LostCities.LOG.warn(
                    "Plan prefetch of chunk ({}, {}) needed more than {} height fetches; planning it unprobed",
                    x,
                    z,
                    MAX_FETCHES_PER_TARGET);
            }
            plan.chunk(x, z);
            return Outcome.PLANNED;
        }
        long start = System.nanoTime();
        tickAttempts++;
        probe.setActive(true);
        try {
            plan.chunk(x, z);
            planned++;
            noteAttempt(System.nanoTime() - start);
            return Outcome.PLANNED;
        } catch (HeightProbe.NotCached missing) {
            probe.setActive(false);
            long attemptNanos = System.nanoTime() - start;
            noteAttempt(attemptNanos);
            failedAttemptNanos += attemptNanos;
            boolean heavy = probe.missingIsHeavy();
            if (heavy && elapsed > HEAVY_START_NANOS) {
                probe.dropMissing();
                return Outcome.DEFERRED_HEAVY;
            }
            long fetchStart = System.nanoTime();
            probe.fetchMissing();
            long fetchNanos = System.nanoTime() - fetchStart;
            if (fetchNanos > SLOW_FETCH_NANOS) {
                slowFetches.merge(probe.lastFetchSource(), fetchNanos, Math::max);
            }
            fetchTotals.merge(probe.lastFetchSource(), fetchNanos, Long::sum);
            tickFetches++;
            tickFetchNanos += fetchNanos;
            fetches++;
            fetchedTotal++;
            if (heavy) {
                // A structure generator's start: structure-oracle work, though it runs outside the oracle's call.
                timers.recordStructures(fetchNanos);
                return Outcome.FETCHED_HEAVY;
            }
            return Outcome.FETCHED;
        } finally {
            probe.setActive(false);
        }
    }

    private void noteAttempt(long nanos) {
        tickAttemptNanos += nanos;
        tickLongestAttempt = Math.max(tickLongestAttempt, nanos);
    }

    /** Forgets the counts {@link #summary()} reports, with the timers' reset at the end of a budget warm-up. */
    void resetStats() {
        slowFetches.clear();
        fetchTotals.clear();
        planned = 0;
        fetchedTotal = 0;
        failedAttemptNanos = 0;
        worstTickNanos = 0;
        worstTick = "none";
    }

    /** A map of source to nanoseconds as milliseconds, sorted by source. */
    private static String millisText(java.util.Map<String, Long> bySource) {
        StringBuilder out = new StringBuilder("{");
        for (java.util.Map.Entry<String, Long> entry : bySource.entrySet()) {
            if (out.length() > 1) {
                out.append(", ");
            }
            out.append(entry.getKey())
                .append('=')
                .append(String.format(Locale.ROOT, "%.1f", entry.getValue() / 1_000_000.0));
        }
        return out.append('}')
            .toString();
    }

    /**
     * Returns a line for the budget report: plans the prefetcher completed, values it fetched, and the time its
     * abandoned attempts cost before their miss (the probe's overhead).
     *
     * @return the line
     */
    public String summary() {
        return String.format(
            Locale.ROOT,
            "prefetcher: %d plans, %d fetches, abandoned attempts %.1f ms; slowest tick %s; fetches over "
                + SLOW_FETCH_NANOS / 1_000_000
                + " ms, longest by source "
                + millisText(slowFetches)
                + ", all fetches' time by source "
                + millisText(fetchTotals),
            planned,
            fetchedTotal,
            failedAttemptNanos / 1_000_000.0,
            worstTick);
    }

    /** Makes sure there is a current target: the next perimeter cell of the newest center. */
    private boolean nextTarget() {
        if (hasCenter) {
            return true;
        }
        if (recentCount == 0) {
            return false;
        }
        recentHead = (recentHead - 1 + RECENT) % RECENT;
        recentCount--;
        long key = recent[recentHead];
        centerX = (int) (key >> 32);
        centerZ = (int) key;
        perimeterIndex = 0;
        hasCenter = true;
        return true;
    }

    private void advance() {
        fetches = 0;
        perimeterIndex++;
        if (perimeterIndex >= PERIMETER) {
            hasCenter = false;
        }
    }

    /**
     * Returns the offset of a cell of the square ring at distance {@link #LEAD}: the north side west to east, the east
     * side north to south, the south side east to west, then the west side south to north.
     *
     * @param index 0 to {@code 8 × LEAD − 1}
     * @return {dx, dz}
     */
    static int[] perimeterOffset(int index) {
        int side = 2 * LEAD;
        int edge = index / side;
        int along = index % side;
        return switch (edge) {
            case 0 -> new int[] { -LEAD + along, -LEAD };
            case 1 -> new int[] { LEAD, -LEAD + along };
            case 2 -> new int[] { LEAD - along, LEAD };
            default -> new int[] { -LEAD, LEAD - along };
        };
    }

    private static long key(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }
}
