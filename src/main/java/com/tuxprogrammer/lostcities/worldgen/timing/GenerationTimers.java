package com.tuxprogrammer.lostcities.worldgen.timing;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainPhase;

/**
 * The overlay's generation timers and the budget check of docs/design/0005-rwg-overlay.md §8: fixed-bucket
 * histograms of the delegate's {@code provideChunk}, the terrain phase, the delegate's {@code populate}, the population
 * phase and oracle calls, each split by how the chunk was treated (city, blend, untouched) with a total, plus the count
 * of chunks whose phase failed (P03-12, D31).
 *
 * <p>
 * Measurement only: nothing generated depends on these numbers. One instance per wrapped world. Server thread only.
 * Side: both (plain Java).
 */
public final class GenerationTimers {

    /** What is timed. */
    public enum Series {
        /** The delegate's (RWG's) {@code provideChunk}. */
        PROVIDE,
        /** The overlay's terrain phase. */
        TERRAIN,
        /** The delegate's (RWG's) {@code populate}. */
        POPULATE,
        /** The overlay's population phase. */
        POPULATION,
        /** Height-oracle calls (inside the plan, so part of whichever phase asked). */
        ORACLE
    }

    /** Column of the totals in the split. */
    static final int ALL = TerrainPhase.ChunkClass.values().length;

    /** B4: no single chunk's overlay work above this, at p99.9 (half of Hodgepodge's 20 ms per tick). */
    public static final long B4_LIMIT_NANOS = 10_000_000L;
    /** B3: all overlay work at most this share of RWG's total generation time. */
    public static final double B3_SHARE = 0.15;
    /** B2: the population phase on city chunks at p95 at most this share of RWG's populate p50. */
    public static final double B2_SHARE = 0.5;
    /** B5: caches bounded to this many bytes per world. */
    public static final long B5_LIMIT_BYTES = 16L * 1024 * 1024;

    private final Histogram[][] histograms = new Histogram[Series.values().length][ALL + 1];
    private long failedChunks;

    /** Creates empty timers. */
    public GenerationTimers() {
        for (int s = 0; s < histograms.length; s++) {
            for (int c = 0; c <= ALL; c++) {
                histograms[s][c] = new Histogram();
            }
        }
    }

    /**
     * Records one duration.
     *
     * @param series     what was timed
     * @param chunkClass how the chunk was treated
     * @param nanos      the duration in nanoseconds
     */
    public void record(Series series, TerrainPhase.ChunkClass chunkClass, long nanos) {
        histograms[series.ordinal()][chunkClass.ordinal()].record(nanos);
        histograms[series.ordinal()][ALL].record(nanos);
    }

    /** Records one oracle call. */
    public void recordOracle(long nanos) {
        histograms[Series.ORACLE.ordinal()][ALL].record(nanos);
    }

    /** Counts a chunk whose phase failed and was logged. */
    public void countFailure() {
        failedChunks++;
    }

    /** Returns the number of chunks whose phase failed. */
    public long failedChunks() {
        return failedChunks;
    }

    /**
     * Returns one histogram.
     *
     * @param series     what was timed
     * @param chunkClass a chunk class, or {@code null} for the total
     * @return the histogram
     */
    public Histogram histogram(Series series, TerrainPhase.ChunkClass chunkClass) {
        return histograms[series.ordinal()][chunkClass == null ? ALL : chunkClass.ordinal()];
    }

    /** Forgets every sample and the failure count. */
    public void reset() {
        for (Histogram[] row : histograms) {
            for (Histogram histogram : row) {
                histogram.reset();
            }
        }
        failedChunks = 0;
    }

    /**
     * Returns the report lines: per series and class with samples, its count and p50, p95, p99, p99.9 and max in
     * milliseconds.
     *
     * @return lines for the log or a command
     */
    public List<String> report() {
        List<String> lines = new ArrayList<>();
        for (Series series : Series.values()) {
            for (int c = 0; c <= ALL; c++) {
                Histogram h = histograms[series.ordinal()][c];
                if (h.count() == 0) {
                    continue;
                }
                String name = c == ALL ? "all" : TerrainPhase.ChunkClass.values()[c].name()
                    .toLowerCase(Locale.ROOT);
                lines.add(
                    String.format(
                        Locale.ROOT,
                        "%-10s %-9s n=%-6d p50=%.3f p95=%.3f p99=%.3f p99.9=%.3f max=%.3f total=%.1f ms",
                        series.name()
                            .toLowerCase(Locale.ROOT),
                        name,
                        h.count(),
                        ms(h.quantile(0.50)),
                        ms(h.quantile(0.95)),
                        ms(h.quantile(0.99)),
                        ms(h.quantile(0.999)),
                        ms(h.max()),
                        ms(h.sum())));
            }
        }
        lines.add("failed chunks: " + failedChunks);
        return lines;
    }

    /**
     * Checks budgets B1 to B5 (0005 §8).
     *
     * @param cacheBytes the world's cache bound, estimated, for B5
     * @return one verdict per budget
     */
    public List<Verdict> budgets(long cacheBytes) {
        List<Verdict> out = new ArrayList<>();
        Histogram provide = histogram(Series.PROVIDE, null);
        Histogram populate = histogram(Series.POPULATE, null);
        Histogram terrainCity = histogram(Series.TERRAIN, TerrainPhase.ChunkClass.CITY);
        Histogram populationCity = histogram(Series.POPULATION, TerrainPhase.ChunkClass.CITY);
        Histogram terrain = histogram(Series.TERRAIN, null);
        Histogram population = histogram(Series.POPULATION, null);
        long b1Limit = provide.quantile(0.50);
        out.add(
            new Verdict(
                "B1",
                terrainCity.count() > 0 && terrainCity.quantile(0.95) <= b1Limit,
                String.format(
                    Locale.ROOT,
                    "terrain city p95 %.3f ms <= RWG provideChunk p50 %.3f ms",
                    ms(terrainCity.quantile(0.95)),
                    ms(b1Limit))));
        long b2Limit = (long) (B2_SHARE * populate.quantile(0.50));
        out.add(
            new Verdict(
                "B2",
                populationCity.count() > 0 && populationCity.quantile(0.95) <= b2Limit,
                String.format(
                    Locale.ROOT,
                    "population city p95 %.3f ms <= 0.5 x RWG populate p50 %.3f ms",
                    ms(populationCity.quantile(0.95)),
                    ms(b2Limit))));
        long rwgTotal = provide.sum() + populate.sum();
        long overlayTotal = terrain.sum() + population.sum();
        double share = rwgTotal == 0 ? 0 : (double) overlayTotal / rwgTotal;
        out.add(
            new Verdict(
                "B3",
                rwgTotal > 0 && share <= B3_SHARE,
                String.format(
                    Locale.ROOT,
                    "overlay %.1f ms = %.1f %% of RWG %.1f ms (<= 15 %%)",
                    ms(overlayTotal),
                    100 * share,
                    ms(rwgTotal))));
        long worst = Math.max(terrain.quantile(0.999), population.quantile(0.999));
        out.add(
            new Verdict(
                "B4",
                worst <= B4_LIMIT_NANOS,
                String.format(Locale.ROOT, "worst phase p99.9 %.3f ms <= 10 ms", ms(worst))));
        out.add(
            new Verdict(
                "B5",
                cacheBytes <= B5_LIMIT_BYTES,
                String.format(Locale.ROOT, "caches at most %.1f MB <= 16 MB", cacheBytes / (1024.0 * 1024.0))));
        return out;
    }

    private static double ms(long nanos) {
        return nanos / 1_000_000.0;
    }

    /** One budget's result. */
    public static final class Verdict {

        private final String id;
        private final boolean met;
        private final String detail;

        Verdict(String id, boolean met, String detail) {
            this.id = id;
            this.met = met;
            this.detail = detail;
        }

        /** Returns the budget's ID, B1 to B5. */
        public String id() {
            return id;
        }

        /** Returns whether the budget holds. */
        public boolean met() {
            return met;
        }

        /** Returns the measured numbers. */
        public String detail() {
            return detail;
        }

        @Override
        public String toString() {
            return id + (met ? " PASS " : " FAIL ") + detail;
        }
    }
}
