package com.tuxprogrammer.lostcities.compat.villagenames;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.tuxprogrammer.lostcities.layout.landmark.BlockRect;
import com.tuxprogrammer.lostcities.layout.landmark.Certainty;
import com.tuxprogrammer.lostcities.layout.landmark.Gate;
import com.tuxprogrammer.lostcities.layout.landmark.Landmark;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkKind;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.worldgen.height.HeightProbe;
import com.tuxprogrammer.lostcities.worldgen.height.TerrainHeightOracle;
import com.tuxprogrammer.lostcities.worldgen.timing.PerfCounters;

/**
 * The structure oracle for VillageNames villages (0006 §2; coexistence.md § How a VillageNames village is placed):
 * every village whose footprint touches a chunk rectangle, without loading or generating a chunk and without touching
 * {@code world.rand}.
 *
 * <p>
 * For each grid cell near the rectangle, the cell's start chunk ({@link VillageGrid}) is a village when it lies
 * outside the village-free radius, the biome provider's biome name at block {@code (16 cx + 8, 16 cz + 8)} is in
 * VillageNames' spawn-biome list, and its plan ({@link VillagePlans}) is sizeable. The landmark is
 * {@code village@<chunkX>,<chunkZ>} of kind {@link LandmarkKind#VILLAGE}, certainty {@link Certainty#EXACT}: its
 * footprint is every piece's box, its gates are {@link VillageGates}' and its ground is {@link GroundEstimates}'
 * median over the buildings, computed when first read (only the finder and the scenarios read it).
 *
 * <p>
 * Pure, as {@link StructureOracle} requires: each start's answer is cached in a bounded cache, but the answer never
 * depends on what was asked before. Every method synchronizes, because the plan builder reuses one generator.
 *
 * <p>
 * Source: VillageNames 4.5.21-GTNH,
 * https://github.com/GTNewHorizons/VillageNames/blob/4.5.21-GTNH/src/main/java/astrotibs/villagenames/village/MapGenVillageVN.java#L105-L155
 * (the start test), and #L383 of
 * https://github.com/GTNewHorizons/VillageNames/blob/4.5.21-GTNH/src/main/java/astrotibs/villagenames/village/StructureVillageVN.java
 * (no piece starts more than 112 blocks from the town center's corner, which bounds {@link #REACH_CHUNKS}).
 *
 * <p>
 * Side: server (world generation), and plain Java in tests.
 */
public final class VillageOracle implements StructureOracle {

    /**
     * How far, in chunks, a village's footprint can reach from its start chunk: a piece starts at most 112 blocks from
     * the town center's minimum corner (two blocks into the start chunk), and a path piece reaches at most 35 blocks
     * further: 149 blocks, so 10 chunks; one more for safety. The {@code oracle.villagenames} scenario reports the
     * farthest reach it sees.
     */
    public static final int REACH_CHUNKS = 11;
    /** Starts kept in the cache: a layout query covers at most about nine grid cells. */
    public static final int CACHE_SIZE = 4096;
    /** Village plans kept for the retry of a prediction a probe abandoned while its plan was fetched. */
    public static final int PLAN_CACHE_SIZE = 256;

    /** Why a start chunk is or is not a village; for the scenario's report. */
    public enum Status {
        /** Not its grid cell's start chunk. */
        NOT_CANDIDATE,
        /** Inside VillageNames' village-free radius. */
        VILLAGE_FREE,
        /** The biome at the start is not in the spawn-biome list. */
        BIOME_REJECTED,
        /** VillageNames would plan it but not build it (two or fewer buildings). */
        NOT_SIZEABLE,
        /** A village. */
        VILLAGE
    }

    /** A start chunk's full answer: status, biome name, plan and landmark. */
    public static final class Prediction {

        private final int chunkX;
        private final int chunkZ;
        private final Status status;
        private final String biomeName;
        private final VillagePlan plan;
        private final Landmark landmark;

        Prediction(int chunkX, int chunkZ, Status status, String biomeName, VillagePlan plan, Landmark landmark) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.status = status;
            this.biomeName = biomeName;
            this.plan = plan;
            this.landmark = landmark;
        }

        /** Returns the start chunk X. */
        public int chunkX() {
            return chunkX;
        }

        /** Returns the start chunk Z. */
        public int chunkZ() {
            return chunkZ;
        }

        /** Returns the verdict. */
        public Status status() {
            return status;
        }

        /** Returns the biome name at the start's test column, or {@code null} when not read. */
        public String biomeName() {
            return biomeName;
        }

        /** Returns the plan, or {@code null} when the start was rejected before planning. */
        public VillagePlan plan() {
            return plan;
        }

        /** Returns the landmark, or {@code null} unless the status is {@link Status#VILLAGE}. */
        public Landmark landmark() {
            return landmark;
        }
    }

    private final VillageNamesSettings settings;
    private final VillageGrid grid;
    private final BiomeNames biomes;
    private final VillagePlans plans;
    private final TerrainHeightOracle heights;
    private final int gateClearance;
    private final BoundedCache<Prediction> cache;
    /**
     * Village plans kept apart from the predictions ({@value #PLAN_CACHE_SIZE}, never more than the predictions): a
     * plan prefetcher's probe (0005 §8)
     * abandons a prediction whose plan is not built, fetches the plan alone and asks again, and VillageNames' start
     * constructor must not run again on the retry.
     */
    private final BoundedCache<VillagePlan> planCache;
    /** The probe that guards the height oracle's cache; while it is active, a plan not built yet is fetched apart. */
    private final HeightProbe probe;

    /**
     * Creates the oracle of a world.
     *
     * @param worldSeed     the world seed
     * @param settings      VillageNames' settings
     * @param biomes        the biome provider's names
     * @param plans         the plan builder
     * @param heights       the height oracle, or {@code null} for {@link GroundEstimates#FALLBACK_Y}
     * @param gateClearance the clearance a lane keeps from village pieces, in blocks ({@code landmarkMargin})
     * @param cacheSize     the most start chunks cached
     */
    public VillageOracle(long worldSeed, VillageNamesSettings settings, BiomeNames biomes, VillagePlans plans,
        TerrainHeightOracle heights, int gateClearance, int cacheSize) {
        this.settings = settings;
        this.grid = new VillageGrid(worldSeed, settings);
        this.biomes = biomes;
        this.plans = plans;
        this.heights = heights;
        this.gateClearance = gateClearance;
        this.cache = new BoundedCache<>(cacheSize);
        this.planCache = new BoundedCache<>(Math.min(PLAN_CACHE_SIZE, cacheSize));
        HeightProbe guard = heights == null ? null : heights.probe();
        this.probe = guard == null ? new HeightProbe() : guard;
    }

    /** Returns the grid this oracle reads. */
    public VillageGrid grid() {
        return grid;
    }

    @Override
    public synchronized List<Landmark> landmarksTouching(int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
        long perfStart = PerfCounters.start();
        try {
            return landmarksTouching0(minChunkX, minChunkZ, maxChunkX, maxChunkZ);
        } finally {
            PerfCounters.add("vo.touching", perfStart);
        }
    }

    private List<Landmark> landmarksTouching0(int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
        if (!settings.villagesOn()) {
            return Collections.emptyList();
        }
        BlockRect query = new BlockRect(minChunkX << 4, minChunkZ << 4, (maxChunkX << 4) + 15, (maxChunkZ << 4) + 15);
        List<Landmark> out = new ArrayList<>();
        int lowX = minChunkX - REACH_CHUNKS;
        int lowZ = minChunkZ - REACH_CHUNKS;
        int highX = maxChunkX + REACH_CHUNKS;
        int highZ = maxChunkZ + REACH_CHUNKS;
        for (int cellX = grid.cellOf(lowX); cellX <= grid.cellOf(highX); cellX++) {
            for (int cellZ = grid.cellOf(lowZ); cellZ <= grid.cellOf(highZ); cellZ++) {
                int[] start = grid.startChunk(cellX, cellZ);
                if (start[0] < lowX || start[0] > highX || start[1] < lowZ || start[1] > highZ) {
                    continue;
                }
                Landmark landmark = predict(start[0], start[1]).landmark();
                if (landmark != null && touches(landmark, query)) {
                    out.add(landmark);
                }
            }
        }
        out.sort(Comparator.comparing(Landmark::id));
        return out;
    }

    /**
     * Returns the full answer for one chunk: whether a village starts there and, if so, its plan and landmark.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the prediction
     */
    public synchronized Prediction predict(int chunkX, int chunkZ) {
        long key = BoundedCache.key(chunkX, chunkZ);
        Prediction cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        return cache.put(key, compute(chunkX, chunkZ));
    }

    /** Builds a village's plan with VillageNames' start constructor and keeps it for retries. */
    private synchronized VillagePlan fetchPlan(int chunkX, int chunkZ) {
        long perfPlan = PerfCounters.start();
        VillagePlan plan = plans.plan(chunkX, chunkZ);
        PerfCounters.add("vo.plan", perfPlan);
        return planCache.put(BoundedCache.key(chunkX, chunkZ), plan);
    }

    private Prediction compute(int chunkX, int chunkZ) {
        long perfStart = PerfCounters.start();
        try {
            return compute0(chunkX, chunkZ);
        } finally {
            PerfCounters.add("vo.compute", perfStart);
        }
    }

    private Prediction compute0(int chunkX, int chunkZ) {
        if (!settings.villagesOn()) {
            return new Prediction(chunkX, chunkZ, Status.NOT_CANDIDATE, null, null, null);
        }
        if (grid.insideVillageFreeRadius(chunkX, chunkZ)) {
            return new Prediction(chunkX, chunkZ, Status.VILLAGE_FREE, null, null, null);
        }
        if (!grid.isCandidate(chunkX, chunkZ)) {
            return new Prediction(chunkX, chunkZ, Status.NOT_CANDIDATE, null, null, null);
        }
        String biomeName = biomes.nameAt(chunkX * 16 + 8, chunkZ * 16 + 8);
        if (!settings.isSpawnBiome(biomeName)) {
            return new Prediction(chunkX, chunkZ, Status.BIOME_REJECTED, biomeName, null, null);
        }
        long key = BoundedCache.key(chunkX, chunkZ);
        VillagePlan plan;
        if (planCache.contains(key)) {
            plan = planCache.get(key);
        } else if (probe.active()) {
            // The plan prefetcher builds one village per fetch, outside the attempt (0005 §8, B4).
            throw probe.missHeavy(() -> fetchPlan(chunkX, chunkZ));
        } else {
            plan = fetchPlan(chunkX, chunkZ);
        }
        if (plan == null || !plan.isSizeable()
            || plan.pieces()
                .isEmpty()) {
            return new Prediction(chunkX, chunkZ, Status.NOT_SIZEABLE, biomeName, plan, null);
        }
        return new Prediction(chunkX, chunkZ, Status.VILLAGE, biomeName, plan, landmark(chunkX, chunkZ, plan));
    }

    private Landmark landmark(int chunkX, int chunkZ, VillagePlan plan) {
        Landmark.Builder builder = Landmark.builder(id(chunkX, chunkZ), LandmarkKind.VILLAGE)
            .certainty(Certainty.EXACT);
        List<BlockRect> buildings = new ArrayList<>();
        for (VillagePlan.Piece piece : plan.pieces()) {
            builder.footprint(piece.box());
            if (!piece.isPath()) {
                buildings.add(piece.box());
            }
        }
        for (Gate gate : VillageGates.find(plan.pieces(), gateClearance)) {
            builder.gate(gate);
        }
        // Read only by the finder and the scenarios: computed on first read (D263).
        return builder.groundEstimate(() -> GroundEstimates.median(heights, buildings))
            .build();
    }

    /**
     * Returns a village landmark's identity.
     *
     * @param chunkX the start chunk X
     * @param chunkZ the start chunk Z
     * @return {@code village@<chunkX>,<chunkZ>}
     */
    public static String id(int chunkX, int chunkZ) {
        return "village@" + chunkX + "," + chunkZ;
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
            if (VillageGates.intersects(rect, query)) {
                return true;
            }
        }
        return false;
    }
}
