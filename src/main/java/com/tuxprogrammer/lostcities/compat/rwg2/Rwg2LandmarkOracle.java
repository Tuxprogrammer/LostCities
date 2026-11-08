package com.tuxprogrammer.lostcities.compat.rwg2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import com.tuxprogrammer.lostcities.compat.villagenames.GroundEstimates;
import com.tuxprogrammer.lostcities.layout.landmark.BlockRect;
import com.tuxprogrammer.lostcities.layout.landmark.Certainty;
import com.tuxprogrammer.lostcities.layout.landmark.Landmark;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkKind;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.worldgen.height.HeightProbe;
import com.tuxprogrammer.lostcities.worldgen.height.TerrainHeightOracle;

/**
 * The structure oracle for RWG2's terrain landmarks (0006 §1, §2; coexistence.md § RWG2 landmarks): the volcano
 * islands, lava-cave openings and mountain-chain river junctions RWG2 keeps Witchery off, as
 * {@link LandmarkKind#TERRAIN}
 * landmarks, so the city ends there with a soft edge instead of building over them.
 *
 * <p>
 * The rule is RWG2's own ({@code WitcheryWorldgenCompat}): a column is protected when it lies in a volcano's vicinity
 * (the island, {@value #VOLCANO_RADIUS} blocks around the volcano), within {@value #LAVA_CAVE_OPENING} blocks of a lava
 * cave's center (its surface opening), or where the river-junction strength is at least {@value #JUNCTION_THRESHOLD}
 * with a mountain-chain biome within {@value #CHAIN_REACH} blocks; RWG2 then keeps {@value #RWG2_MARGIN} blocks around
 * it. The landmarks:
 * <ul>
 * <li>{@code volcano@<x>,<z>} and {@code lavacave@<x>,<z>} (the center, block coordinates): a disc of the radius plus
 * the margin's diagonal (RWG2's margin is a square of ±8 blocks, so it reaches 8√2, 12 blocks rounded up, from a
 * protected column) plus one block for the center's rounding, as one rectangle per 16-block band of Z. RWG2's own
 * flags can
 * be a little smaller than the disc (a neighboring candidate wins near the rim), never larger, so the disc errs toward
 * the landmark. Centers are found by sampling the predicates every {@value #DISC_SPACING} blocks, far closer than the
 * radii, so no volcano or cave near the query is missed.</li>
 * <li>{@code junction@<x>,<z>} (the strength's local peak): junction strength is sampled every
 * {@value #JUNCTION_SPACING} blocks; an 8 × 8 cell whose corners reach {@value #REFINE_THRESHOLD} is sampled again
 * every
 * {@value #REFINE_SPACING} blocks; a cell holding a sample of at least {@value #STRONG_SAMPLE} (a little below RWG2's
 * {@value #JUNCTION_THRESHOLD}, for the columns between samples) with a
 * mountain-chain biome on the 16-block grid within {@value #CHAIN_REACH} + 8 blocks becomes one footprint rectangle
 * (its strong points' box, grown by the margin and one block for the sampling), filed under the peak reached by
 * climbing the strength from its strongest point the way RWG2's {@code LandmarkDecorations} finds a junction's center.
 * Strength changes slowly (well under 0.05 a block), so a strong patch always lifts a corner above the refine
 * threshold. A junction whose strength has several local peaks along a ridge comes back as several landmarks with
 * overlapping footprints, whose cores join.</li>
 * </ul>
 * Certainty is {@link Certainty#EXACT}: the terrain is RWG2's, from the same functions. The ground estimate is the
 * height oracle's median over the footprint's bounds, computed when first read.
 *
 * <p>
 * Pure: answers depend only on the predicates ({@link Rwg2Landforms}, a function of the world seed and RWG2's config)
 * and the height oracle; the order of questions never changes an answer. Work is cached in bounded caches this object
 * owns: the sample results per 64-block region, the mountain-chain grid, and the landmarks. While the world's
 * {@link HeightProbe} is active (the plan prefetcher, 0005 §8), a region not computed yet is fetched apart instead of
 * inside the caller's phase, as height samples are. Synchronized; server thread in practice. Side: server (world
 * generation), and plain Java in tests.
 *
 * <p>
 * Source: RWG2 2.0.6,
 * https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/support/WitcheryWorldgenCompat.java#L12-L95
 * (the rule and its constants) and
 * https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/support/LandmarkDecorations.java#L157-L195
 * (the junction center's climb); radii from
 * https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/util/ContinentalNoise.java#L13-L15.
 */
public final class Rwg2LandmarkOracle implements StructureOracle {

    /** A volcano's vicinity radius, blocks ({@code ContinentalNoise.VOLCANO_ISLAND_RADIUS}). */
    public static final int VOLCANO_RADIUS = 160;
    /** A lava cave's surface-opening radius, blocks ({@code LAVA_CAVE_SURFACE_RADIUS}). */
    public static final int LAVA_CAVE_OPENING = 70;
    /** The margin RWG2 keeps around a protected column, blocks ({@code OPENING_MARGIN}). */
    public static final int RWG2_MARGIN = 8;
    /** The junction strength from which a column is protected ({@code SKYLIGHT_THRESHOLD}). */
    public static final float JUNCTION_THRESHOLD = 0.70F;
    /** How far a mountain chain may be from a junction column, blocks ({@code hasMountainChainNearby}). */
    public static final int CHAIN_REACH = 32;
    /** Spacing of the volcano and lava-cave samples, blocks. */
    public static final int DISC_SPACING = 32;
    /** Side of a sample region, blocks. */
    public static final int REGION = 64;
    /** Spacing of the coarse junction samples, blocks. */
    public static final int JUNCTION_SPACING = 8;
    /** Spacing of the fine junction samples inside a promising cell, blocks. */
    public static final int REFINE_SPACING = 2;
    /**
     * Strength from which a fine sample counts toward a junction's footprint: below {@link #JUNCTION_THRESHOLD} by
     * more than the strength can change between a column and its nearest fine sample (well under 0.05 a block), so
     * every protected column lies within one block of a counted sample.
     */
    public static final float STRONG_SAMPLE = 0.60F;
    /** Corner strength from which a coarse cell is sampled finely. */
    public static final float REFINE_THRESHOLD = 0.30F;
    /** How far from its peak a junction's footprint is gathered, blocks. */
    public static final int JUNCTION_GATHER = 128;
    /** Regions kept in each region cache. */
    public static final int REGION_CACHE_SIZE = 16384;
    /** Landmarks and chain-grid points kept. */
    public static final int LANDMARK_CACHE_SIZE = 512;

    /** How far RWG2's square margin reaches from a protected column along a diagonal, blocks (8√2 rounded up). */
    static final int MARGIN_DIAGONAL = (int) Math.ceil(RWG2_MARGIN * Math.sqrt(2D));
    /** A volcano footprint's radius: the vicinity, the margin's diagonal and one block for the center's rounding. */
    static final int VOLCANO_REACH = VOLCANO_RADIUS + MARGIN_DIAGONAL + 1;
    /** A lava cave footprint's radius. */
    static final int LAVA_CAVE_REACH = LAVA_CAVE_OPENING + MARGIN_DIAGONAL + 1;
    /** How far a junction rectangle reaches beyond its strong samples: the margin and one block for the sampling. */
    static final int JUNCTION_GROWTH = RWG2_MARGIN + REFINE_SPACING / 2;

    private static final int CELLS = REGION / JUNCTION_SPACING;
    private static final int DISC_POINTS = REGION / DISC_SPACING;
    private static final int PART_INTS = 6;
    private static final int[] CLIMB_STEPS = { 4, 2, 1 };
    private static final Landmark ABSENT = Landmark.builder("none", LandmarkKind.TERRAIN)
        .footprint(new BlockRect(0, 0, 0, 0))
        .build();

    private final Rwg2Landforms landforms;
    private final TerrainHeightOracle heights;
    private final HeightProbe probe;
    private final BoundedCache<long[]> discRegions;
    private final BoundedCache<int[]> junctionRegions;
    private final BoundedCache<Boolean> chainGrid;
    private final BoundedCache<Landmark> volcanoes;
    private final BoundedCache<Landmark> lavaCaves;
    private final BoundedCache<Landmark> junctions;

    /**
     * Creates the oracle of a world.
     *
     * @param landforms         RWG2's predicates
     * @param heights           the height oracle, or {@code null} for {@link GroundEstimates#FALLBACK_Y}
     * @param regionCacheSize   the most regions kept in each region cache
     * @param landmarkCacheSize the most landmarks and chain-grid points kept
     */
    public Rwg2LandmarkOracle(Rwg2Landforms landforms, TerrainHeightOracle heights, int regionCacheSize,
        int landmarkCacheSize) {
        this.landforms = landforms;
        this.heights = heights;
        HeightProbe guard = heights == null ? null : heights.probe();
        this.probe = guard == null ? new HeightProbe() : guard;
        this.discRegions = new BoundedCache<>(regionCacheSize);
        this.junctionRegions = new BoundedCache<>(regionCacheSize);
        this.chainGrid = new BoundedCache<>(landmarkCacheSize * 8);
        this.volcanoes = new BoundedCache<>(landmarkCacheSize);
        this.lavaCaves = new BoundedCache<>(landmarkCacheSize);
        this.junctions = new BoundedCache<>(landmarkCacheSize);
    }

    @Override
    public synchronized List<Landmark> landmarksTouching(int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
        BlockRect query = new BlockRect(minChunkX << 4, minChunkZ << 4, (maxChunkX << 4) + 15, (maxChunkZ << 4) + 15);
        TreeMap<String, Landmark> out = new TreeMap<>();
        for (long center : discCenters(query.grow(VOLCANO_REACH + DISC_SPACING), 0)) {
            add(out, discLandmark(center, true), query);
        }
        for (long center : discCenters(query.grow(LAVA_CAVE_REACH + DISC_SPACING), 1)) {
            add(out, discLandmark(center, false), query);
        }
        for (long peak : junctionPeaks(query.grow(JUNCTION_GROWTH))) {
            add(out, junctionLandmark(peak), query);
        }
        return new ArrayList<>(out.values());
    }

    private static void add(TreeMap<String, Landmark> out, Landmark landmark, BlockRect query) {
        if (landmark != null && landmark != ABSENT && touches(landmark, query)) {
            out.put(landmark.id(), landmark);
        }
    }

    // ---- volcanoes and lava caves

    /** Returns the distinct centers of one kind (0 volcano, 1 lava cave) that the samples in a rectangle report. */
    private TreeSet<Long> discCenters(BlockRect area, int which) {
        TreeSet<Long> centers = new TreeSet<>();
        for (int regionX = Math.floorDiv(area.minX(), REGION); regionX
            <= Math.floorDiv(area.maxX(), REGION); regionX++) {
            for (int regionZ = Math.floorDiv(area.minZ(), REGION); regionZ
                <= Math.floorDiv(area.maxZ(), REGION); regionZ++) {
                long[] samples = discRegion(regionX, regionZ);
                for (int i = 0; i < DISC_POINTS * DISC_POINTS; i++) {
                    long center = samples[i * 2 + which];
                    if (center != Rwg2Landforms.NONE) {
                        centers.add(center);
                    }
                }
            }
        }
        return centers;
    }

    private long[] discRegion(int regionX, int regionZ) {
        long key = BoundedCache.key(regionX, regionZ);
        long[] cached = discRegions.get(key);
        if (cached != null) {
            return cached;
        }
        if (probe.active()) {
            throw probe.miss(() -> fetchDiscRegion(regionX, regionZ));
        }
        return fetchDiscRegion(regionX, regionZ);
    }

    private synchronized long[] fetchDiscRegion(int regionX, int regionZ) {
        long[] samples = new long[DISC_POINTS * DISC_POINTS * 2];
        int index = 0;
        for (int i = 0; i < DISC_POINTS; i++) {
            for (int j = 0; j < DISC_POINTS; j++) {
                int x = regionX * REGION + i * DISC_SPACING;
                int z = regionZ * REGION + j * DISC_SPACING;
                samples[index++] = landforms.volcanoCenter(x, z);
                samples[index++] = landforms.lavaCaveCenter(x, z);
            }
        }
        return discRegions.put(BoundedCache.key(regionX, regionZ), samples);
    }

    private Landmark discLandmark(long center, boolean volcano) {
        BoundedCache<Landmark> cache = volcano ? volcanoes : lavaCaves;
        Landmark cached = cache.get(center);
        if (cached != null) {
            return cached;
        }
        int x = Rwg2Landforms.unpackX(center);
        int z = Rwg2Landforms.unpackZ(center);
        List<BlockRect> rows = discRows(x, z, volcano ? VOLCANO_REACH : LAVA_CAVE_REACH);
        Landmark.Builder builder = Landmark.builder(id(volcano ? "volcano" : "lavacave", x, z), LandmarkKind.TERRAIN)
            .certainty(Certainty.EXACT);
        for (BlockRect row : rows) {
            builder.footprint(row);
        }
        List<BlockRect> groundBox = Collections.singletonList(bounds(rows));
        // Read only by the finder and the scenarios: computed on first read (D263).
        Landmark built = builder.groundEstimate(() -> GroundEstimates.median(heights, groundBox))
            .build();
        return cache.put(center, built);
    }

    /**
     * Returns a disc as rectangles, one per 16-block band of Z, each as wide as the disc gets within its band.
     *
     * @param centerX the center's block X
     * @param centerZ the center's block Z
     * @param radius  the radius, blocks
     * @return the rectangles, north to south
     */
    static List<BlockRect> discRows(int centerX, int centerZ, int radius) {
        List<BlockRect> rows = new ArrayList<>();
        int top = centerZ - radius;
        int bottom = centerZ + radius;
        for (int band = Math.floorDiv(top, 16); band <= Math.floorDiv(bottom, 16); band++) {
            int low = Math.max(band * 16, top);
            int high = Math.min(band * 16 + 15, bottom);
            int nearest = centerZ < low ? low - centerZ : centerZ > high ? centerZ - high : 0;
            int half = (int) Math.floor(Math.sqrt((double) radius * radius - (double) nearest * nearest));
            rows.add(new BlockRect(centerX - half, low, centerX + half, high));
        }
        return rows;
    }

    // ---- river junctions

    /** Returns the peaks of every junction part in the regions a rectangle touches. */
    private TreeSet<Long> junctionPeaks(BlockRect area) {
        TreeSet<Long> peaks = new TreeSet<>();
        for (int regionX = Math.floorDiv(area.minX(), REGION); regionX
            <= Math.floorDiv(area.maxX(), REGION); regionX++) {
            for (int regionZ = Math.floorDiv(area.minZ(), REGION); regionZ
                <= Math.floorDiv(area.maxZ(), REGION); regionZ++) {
                int[] parts = junctionRegion(regionX, regionZ);
                for (int i = 0; i < parts.length; i += PART_INTS) {
                    peaks.add(Rwg2Landforms.pack(parts[i], parts[i + 1]));
                }
            }
        }
        return peaks;
    }

    private int[] junctionRegion(int regionX, int regionZ) {
        long key = BoundedCache.key(regionX, regionZ);
        int[] cached = junctionRegions.get(key);
        if (cached != null) {
            return cached;
        }
        if (probe.active()) {
            throw probe.miss(() -> fetchJunctionRegion(regionX, regionZ));
        }
        return fetchJunctionRegion(regionX, regionZ);
    }

    /**
     * Samples one region's junction strength and returns its parts, six numbers each: the peak's X and Z, then the box
     * of the cell's strong samples.
     */
    private synchronized int[] fetchJunctionRegion(int regionX, int regionZ) {
        int originX = regionX * REGION;
        int originZ = regionZ * REGION;
        float[][] coarse = new float[CELLS + 1][CELLS + 1];
        for (int i = 0; i <= CELLS; i++) {
            for (int j = 0; j <= CELLS; j++) {
                coarse[i][j] = landforms
                    .junctionStrength(originX + i * JUNCTION_SPACING, originZ + j * JUNCTION_SPACING);
            }
        }
        List<int[]> parts = new ArrayList<>();
        for (int i = 0; i < CELLS; i++) {
            for (int j = 0; j < CELLS; j++) {
                float corner = Math
                    .max(Math.max(coarse[i][j], coarse[i + 1][j]), Math.max(coarse[i][j + 1], coarse[i + 1][j + 1]));
                if (corner >= REFINE_THRESHOLD) {
                    int[] part = refineCell(originX + i * JUNCTION_SPACING, originZ + j * JUNCTION_SPACING);
                    if (part != null) {
                        parts.add(part);
                    }
                }
            }
        }
        int[] flat = new int[parts.size() * PART_INTS];
        for (int p = 0; p < parts.size(); p++) {
            System.arraycopy(parts.get(p), 0, flat, p * PART_INTS, PART_INTS);
        }
        return junctionRegions.put(BoundedCache.key(regionX, regionZ), flat);
    }

    /** Samples one 8 × 8 cell finely; returns its part, or {@code null} when nothing in it is protected. */
    private int[] refineCell(int cellX, int cellZ) {
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        float best = -1F;
        int bestX = 0;
        int bestZ = 0;
        for (int a = 0; a < JUNCTION_SPACING; a += REFINE_SPACING) {
            for (int b = 0; b < JUNCTION_SPACING; b += REFINE_SPACING) {
                int x = cellX + a;
                int z = cellZ + b;
                float strength = landforms.junctionStrength(x, z);
                if (strength >= STRONG_SAMPLE) {
                    minX = Math.min(minX, x);
                    minZ = Math.min(minZ, z);
                    maxX = Math.max(maxX, x);
                    maxZ = Math.max(maxZ, z);
                }
                if (strength > best) {
                    best = strength;
                    bestX = x;
                    bestZ = z;
                }
            }
        }
        if (minX == Integer.MAX_VALUE || !chainNear(cellX, cellZ)) {
            return null;
        }
        long peak = climb(bestX, bestZ, best);
        return new int[] { Rwg2Landforms.unpackX(peak), Rwg2Landforms.unpackZ(peak), minX, minZ, maxX, maxZ };
    }

    /**
     * Returns whether a mountain-chain biome stands on the 16-block grid within {@value #CHAIN_REACH} + 8 blocks of a
     * cell: a superset of RWG2's test for each of its columns, which looks 0, 16 and 32 blocks away on each axis.
     */
    private boolean chainNear(int cellX, int cellZ) {
        int reach = CHAIN_REACH + JUNCTION_SPACING;
        int lowX = Math.floorDiv(cellX - reach, 16) * 16;
        int lowZ = Math.floorDiv(cellZ - reach, 16) * 16;
        int highX = cellX + JUNCTION_SPACING - 1 + reach;
        int highZ = cellZ + JUNCTION_SPACING - 1 + reach;
        for (int x = lowX; x <= highX; x += 16) {
            for (int z = lowZ; z <= highZ; z += 16) {
                long key = Rwg2Landforms.pack(x, z);
                Boolean chain = chainGrid.get(key);
                if (chain == null) {
                    chain = chainGrid.put(key, landforms.mountainChain(x, z));
                }
                if (chain) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Climbs the junction strength to its local peak, as RWG2 finds a junction's center (steps 4, 2, 1). */
    private long climb(int startX, int startZ, float startStrength) {
        int x = startX;
        int z = startZ;
        float best = startStrength;
        for (int step : CLIMB_STEPS) {
            boolean moved;
            do {
                moved = false;
                int bestX = x;
                int bestZ = z;
                for (int dx = -step; dx <= step; dx += step) {
                    for (int dz = -step; dz <= step; dz += step) {
                        float strength = landforms.junctionStrength(x + dx, z + dz);
                        if (strength > best) {
                            best = strength;
                            bestX = x + dx;
                            bestZ = z + dz;
                            moved = true;
                        }
                    }
                }
                x = bestX;
                z = bestZ;
            } while (moved);
        }
        return Rwg2Landforms.pack(x, z);
    }

    /** Builds a junction's landmark from every part within {@value #JUNCTION_GATHER} blocks that climbs to its peak. */
    private Landmark junctionLandmark(long peak) {
        Landmark cached = junctions.get(peak);
        if (cached != null) {
            return cached;
        }
        int peakX = Rwg2Landforms.unpackX(peak);
        int peakZ = Rwg2Landforms.unpackZ(peak);
        BlockRect gather = new BlockRect(peakX, peakZ, peakX, peakZ).grow(JUNCTION_GATHER);
        List<BlockRect> rects = new ArrayList<>();
        for (int regionX = Math.floorDiv(gather.minX(), REGION); regionX
            <= Math.floorDiv(gather.maxX(), REGION); regionX++) {
            for (int regionZ = Math.floorDiv(gather.minZ(), REGION); regionZ
                <= Math.floorDiv(gather.maxZ(), REGION); regionZ++) {
                int[] parts = junctionRegion(regionX, regionZ);
                for (int i = 0; i < parts.length; i += PART_INTS) {
                    if (parts[i] == peakX && parts[i + 1] == peakZ
                        && gather.contains(parts[i + 2], parts[i + 3])
                        && gather.contains(parts[i + 4], parts[i + 5])) {
                        rects.add(
                            new BlockRect(parts[i + 2], parts[i + 3], parts[i + 4], parts[i + 5])
                                .grow(JUNCTION_GROWTH));
                    }
                }
            }
        }
        if (rects.isEmpty()) {
            return junctions.put(peak, ABSENT);
        }
        Landmark.Builder builder = Landmark.builder(id("junction", peakX, peakZ), LandmarkKind.TERRAIN)
            .certainty(Certainty.EXACT);
        for (BlockRect rect : rects) {
            builder.footprint(rect);
        }
        List<BlockRect> groundBox = Collections.singletonList(bounds(rects));
        // Read only by the finder and the scenarios: computed on first read (D263).
        Landmark built = builder.groundEstimate(() -> GroundEstimates.median(heights, groundBox))
            .build();
        return junctions.put(peak, built);
    }

    // ---- helpers

    /**
     * Returns a terrain landmark's identity.
     *
     * @param kind {@code volcano}, {@code lavacave} or {@code junction}
     * @param x    the center's or peak's block X
     * @param z    the center's or peak's block Z
     * @return {@code <kind>@<x>,<z>}
     */
    public static String id(String kind, int x, int z) {
        return kind + "@" + x + "," + z;
    }

    private static BlockRect bounds(List<BlockRect> rects) {
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockRect rect : rects) {
            minX = Math.min(minX, rect.minX());
            minZ = Math.min(minZ, rect.minZ());
            maxX = Math.max(maxX, rect.maxX());
            maxZ = Math.max(maxZ, rect.maxZ());
        }
        return new BlockRect(minX, minZ, maxX, maxZ);
    }

    /**
     * Returns whether any footprint rectangle of a landmark overlaps a block rectangle.
     *
     * @param landmark the landmark
     * @param query    the rectangle
     * @return whether they share a column
     */
    static boolean touches(Landmark landmark, BlockRect query) {
        for (BlockRect rect : landmark.footprint()) {
            if (rect.minX() <= query.maxX() && rect.maxX() >= query.minX()
                && rect.minZ() <= query.maxZ()
                && rect.maxZ() >= query.minZ()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether a column lies in any footprint rectangle of the landmarks touching its chunk: the scenario's
     * check against RWG2's own rule.
     *
     * @param x block X
     * @param z block Z
     * @return whether a terrain landmark covers the column
     */
    public synchronized boolean covers(int x, int z) {
        for (Landmark landmark : landmarksTouching(x >> 4, z >> 4, x >> 4, z >> 4)) {
            for (BlockRect rect : landmark.footprint()) {
                if (rect.contains(x, z)) {
                    return true;
                }
            }
        }
        return false;
    }
}
