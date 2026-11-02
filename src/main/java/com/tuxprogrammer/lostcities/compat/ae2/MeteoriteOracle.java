package com.tuxprogrammer.lostcities.compat.ae2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.tuxprogrammer.lostcities.layout.landmark.BlockRect;
import com.tuxprogrammer.lostcities.layout.landmark.Certainty;
import com.tuxprogrammer.lostcities.layout.landmark.HeightCap;
import com.tuxprogrammer.lostcities.layout.landmark.Landmark;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkKind;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.worldgen.height.HeightProbe;
import com.tuxprogrammer.lostcities.worldgen.height.TerrainHeightOracle;

/**
 * The structure oracle for AE2 meteorites (0006 §1 and §2): one landmark per grid cell whose candidate falls, so the
 * city leaves the impact natural ground and steps down toward it under the crater instead of being sliced.
 *
 * <p>
 * The landmark is {@code meteorite@<x>,<z>} (the impact's block coordinates) of kind {@link LandmarkKind#METEORITE},
 * certainty {@link Certainty#EXACT}: its footprint is the square of ground AE2 may change ({@link CraterBasin}), its
 * ground estimate the median oracle height over it, and its height cap AE2's paraboloid,
 * {@link HeightCap#ae2(int, int, int)} with the lowest center the crater can have. A meteorite that finds no ground
 * (an ocean, a cave under every step) leaves its core natural, as any empty lot (0006 §5).
 *
 * <p>
 * Pure: the answer depends on the world seed, AE2's settings and the height oracle only; no chunk is read and no
 * world {@code Random} is touched. Landmarks are cached per cell in a bounded cache of {@value #CACHE_SIZE} cells
 * that belongs to this object. Synchronized; the server thread is the only caller in practice.
 *
 * <p>
 * Spec: docs/new-spec/00-pack-integration/coexistence.md § AE2 meteorites. Side: server, and plain Java in tests.
 */
public final class MeteoriteOracle implements StructureOracle {

    /** Cells kept in the cache. */
    public static final int CACHE_SIZE = 64;
    /** Height used without an oracle, block Y. */
    static final int FALLBACK_Y = 64;

    /** Basins part-way read, kept at most. */
    static final int BUILDING_SIZE = 16;
    /** Time one prefetch step may spend reading a basin's samples, nanoseconds. */
    static final long STEP_NANOS = 1_500_000L;

    private static final Landmark NONE = Landmark.builder("none", LandmarkKind.METEORITE)
        .footprint(new BlockRect(0, 0, 0, 0))
        .build();

    private final MeteoriteGrid grid;
    private final TerrainHeightOracle heights;
    private final HeightProbe probe;
    private final SiteCache<CraterBasin.Builder> building = new SiteCache<>(BUILDING_SIZE);
    private final SiteCache<Landmark> cache;

    /**
     * Creates the oracle of a world.
     *
     * @param worldSeed the world seed
     * @param settings  the dimension's settings, enabled
     * @param heights   the height oracle; {@code null} estimates every height at Y {@value #FALLBACK_Y}
     * @param cacheSize the most cells cached
     */
    public MeteoriteOracle(long worldSeed, MeteoriteSettings settings, TerrainHeightOracle heights, int cacheSize) {
        this.grid = new MeteoriteGrid(worldSeed, settings);
        this.heights = heights == null ? (x, z) -> FALLBACK_Y : heights;
        HeightProbe guard = heights == null ? null : heights.probe();
        this.probe = guard == null ? new HeightProbe() : guard;
        this.cache = new SiteCache<>(cacheSize);
    }

    /** Returns the grid this oracle reads. */
    public MeteoriteGrid grid() {
        return grid;
    }

    @Override
    public synchronized List<Landmark> landmarksTouching(int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
        BlockRect query = new BlockRect(minChunkX << 4, minChunkZ << 4, (maxChunkX << 4) + 15, (maxChunkZ << 4) + 15);
        int reach = CraterBasin.MAX_HALF_WIDTH;
        List<Landmark> out = new ArrayList<>();
        for (int cellX = grid.cellOf(query.minX() - reach); cellX <= grid.cellOf(query.maxX() + reach); cellX++) {
            for (int cellZ = grid.cellOf(query.minZ() - reach); cellZ <= grid.cellOf(query.maxZ() + reach); cellZ++) {
                Landmark landmark = meteoriteOf(cellX, cellZ, query, reach);
                if (landmark != null && touches(landmark, query)) {
                    out.add(landmark);
                }
            }
        }
        out.sort(Comparator.comparing(Landmark::id));
        return out;
    }

    /**
     * Returns the meteorite of a cell, or {@code null} when its candidate never falls.
     *
     * @param cellX cell X
     * @param cellZ cell Z
     * @return the landmark, or {@code null}
     */
    public synchronized Landmark meteoriteAt(int cellX, int cellZ) {
        return meteoriteOf(cellX, cellZ, null, 0);
    }

    /**
     * Returns a cell's landmark; with a query, a site too far from it to touch it is skipped before its basin is
     * computed, and nothing is cached for it.
     */
    private Landmark meteoriteOf(int cellX, int cellZ, BlockRect query, int reach) {
        long key = SiteCache.key(cellX, cellZ);
        Landmark cached = cache.get(key);
        if (cached != null) {
            return cached == NONE ? null : cached;
        }
        MeteoriteGrid.Site site = grid.site(cellX, cellZ);
        if (!site.falls()) {
            cache.put(key, NONE);
            return null;
        }
        if (query != null && !query.grow(reach)
            .contains(site.x(), site.z())) {
            return null;
        }
        if (probe.active()) {
            // About 550 height samples: read a part per prefetch step, with the probe off (B4).
            throw probe.miss(() -> step(key, site));
        }
        Landmark landmark = build(site, CraterBasin.of(heights, site.x(), site.z(), MeteoriteShape.of(site.seed())));
        cache.put(key, landmark);
        return landmark;
    }

    /** Reads the next part of a basin's samples, and caches the landmark once all are read. */
    private synchronized void step(long key, MeteoriteGrid.Site site) {
        if (cache.get(key) != null) {
            return;
        }
        CraterBasin.Builder builder = building.get(key);
        if (builder == null) {
            builder = building
                .put(key, new CraterBasin.Builder(heights, site.x(), site.z(), MeteoriteShape.of(site.seed())));
        }
        if (builder.step(STEP_NANOS)) {
            cache.put(key, build(site, builder.build()));
            building.remove(key);
        }
    }

    private Landmark build(MeteoriteGrid.Site site, CraterBasin basin) {
        int half = basin.halfWidth();
        return Landmark.builder(id(site.x(), site.z()), LandmarkKind.METEORITE)
            .footprint(new BlockRect(site.x() - half, site.z() - half, site.x() + half, site.z() + half))
            .certainty(Certainty.EXACT)
            .groundEstimate(basin.groundEstimate())
            .heightCap(HeightCap.ae2(site.x(), site.z(), basin.capCenterY()))
            .build();
    }

    /**
     * Returns a meteorite landmark's identity.
     *
     * @param x the impact's block X
     * @param z the impact's block Z
     * @return {@code meteorite@<x>,<z>}
     */
    public static String id(int x, int z) {
        return "meteorite@" + x + "," + z;
    }

    /** Returns whether any footprint rectangle of a landmark overlaps a block rectangle. */
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
}
