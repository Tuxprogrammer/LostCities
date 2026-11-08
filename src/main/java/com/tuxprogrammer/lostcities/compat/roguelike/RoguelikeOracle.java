package com.tuxprogrammer.lostcities.compat.roguelike;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.tuxprogrammer.lostcities.layout.landmark.BlockRect;
import com.tuxprogrammer.lostcities.layout.landmark.Certainty;
import com.tuxprogrammer.lostcities.layout.landmark.Landmark;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkKind;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.worldgen.height.HeightProbe;
import com.tuxprogrammer.lostcities.worldgen.height.TerrainHeightOracle;
import com.tuxprogrammer.lostcities.worldgen.timing.PerfCounters;

/**
 * The structure oracle for Roguelike Dungeons' towers (0006 §2): for each grid cell, the site its dungeon is predicted
 * to take, as a lot the city builds around.
 *
 * <p>
 * For each cell near the rectangle, the cell's dungeon chunk ({@link RoguelikeGrid#dungeonChunk}) yields 50 candidate
 * points ({@link RoguelikeGrid#candidates}); the first that the estimated site test ({@link SiteRules}) passes is the
 * prediction. The landmark is {@code roguelike@<blockX>,<blockZ>} of kind {@link LandmarkKind#ROGUELIKE}, certainty
 * {@link Certainty#CANDIDATE}: its footprint is the 21 × 21 blocks around the point (the widest tower, 17 blocks,
 * plus margin) and its ground the oracle height at the point. The steering mixin
 * ({@code mixins.late.MixinRoguelikeDungeon}) then keeps the real dungeon out of the city wherever the estimate is
 * wrong, so the outcome follows the layout (0006 §5).
 *
 * <p>
 * Pure, as {@link StructureOracle} requires: the answer depends on the world seed, Roguelike's settings, the biome
 * provider and the height oracle; no chunk is loaded and {@code world.rand} is never touched. Each cell's answer is
 * cached in a bounded cache owned by this object ({@value #CACHE_SIZE} cells). While the plan prefetcher probes
 * (0005 §8), a cell not cached yet is worked through in steps of {@value #CANDIDATES_PER_STEP} candidates, one step
 * per probe miss ({@link HeightProbe#miss}), its progress kept in a small bounded map, so no prefetch tick runs all
 * 50 candidates at once (budget B4). The steps test the candidates in the same order as one pass does, so the
 * prediction is the same either way.
 *
 * <p>
 * Server thread; synchronized, as the other oracles. Side: server (world generation), and plain Java in tests.
 */
public final class RoguelikeOracle implements StructureOracle {

    /** Cells kept in the cache. */
    public static final int CACHE_SIZE = 256;
    /** Half the side of a lot, blocks: the 21 × 21 footprint around the site. */
    public static final int LOT_HALF = 10;
    /**
     * How far, in blocks, a dungeon's top level can reach from its site, horizontally: 122 (D361). The tower stands
     * on the 21 × 21 lot, but its levels spread under the ground around it. Roguelike Dungeons 1.6.6-GTNH builds the
     * first level at Y 50 with its origin at the site ({@code DungeonGenerator.TOPLEVEL}, L16-L27); a room grows new
     * tunnels only while it lies less than {@code levelRange} (80 in the pack and by default) from that origin
     * ({@code LevelGeneratorClassic} L232-L236, {@code DungeonLevel.inRange}); a tunnel runs on while it is within
     * {@code levelScatter} (10) of a room, then at most {@code 2 × levelScatter} more blocks before its room
     * ({@code LevelGeneratorClassic} L176-L193); and the widest rooms reach 12 blocks from their center
     * ({@code DungeonsPrison.getSize}, {@code DungeonTreetho.getSize}). So 80 + 10 + 20 + 12. The top level's rooms
     * rise
     * up to 10 blocks above Y 50, through the depths a railway tunnel takes; the deeper levels lie 10 blocks apart
     * below it. Defaults: {@code RogueConfig} L78-L83; the pack's {@code config/roguelike_dungeons/roguelike.cfg} keeps
     * them. Source tag 1.6.6-GTNH, https://github.com/GTNewHorizons/Roguelike-Dungeons/tree/1.6.6-GTNH.
     */
    public static final int LEVEL_REACH = 122;
    /**
     * How far, in chunks, a lot can reach from its dungeon chunk: a candidate lies up to 99 blocks from the origin, 4
     * blocks into the chunk, and the lot 10 blocks further.
     */
    public static final int REACH_CHUNKS = 7;
    /** Candidates one prefetch step tests: up to about 2 ms of height samples on RWG2 Continents. */
    public static final int CANDIDATES_PER_STEP = 3;
    /** Cells whose candidates are part-way tested, kept at most. */
    static final int PROGRESS_SIZE = 32;

    private final int dimension;
    private final RoguelikeGrid grid;
    private final SiteRules rules;
    private final TerrainHeightOracle heights;
    private final HeightProbe probe;
    private final BoundedCache<Prediction> cache;
    private final BoundedCache<Progress> progress = new BoundedCache<>(PROGRESS_SIZE);

    /**
     * Creates the oracle of a world.
     *
     * @param dimension the world's dimension ID; the oracle knows nothing where Roguelike does not generate
     * @param grid      the world's grid
     * @param heights   the height oracle
     * @param terrain   the biome-provider facts
     * @param cacheSize the most cells cached
     */
    public RoguelikeOracle(int dimension, RoguelikeGrid grid, TerrainHeightOracle heights, RoguelikeTerrain terrain,
        int cacheSize) {
        this.dimension = dimension;
        this.grid = grid;
        this.heights = heights;
        this.rules = new SiteRules(grid.settings(), heights, terrain);
        HeightProbe guard = heights.probe();
        this.probe = guard == null ? new HeightProbe() : guard;
        this.cache = new BoundedCache<>(cacheSize);
    }

    /** Returns the grid this oracle reads. */
    public RoguelikeGrid grid() {
        return grid;
    }

    /** Returns whether Roguelike generates dungeons in this oracle's world. */
    public boolean enabled() {
        return grid.settings()
            .generatesIn(dimension);
    }

    @Override
    public synchronized List<Landmark> landmarksTouching(int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
        long perfStart = PerfCounters.start();
        try {
            return landmarksTouching0(minChunkX, minChunkZ, maxChunkX, maxChunkZ);
        } finally {
            PerfCounters.add("rl.touching", perfStart);
        }
    }

    private List<Landmark> landmarksTouching0(int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
        if (!enabled()) {
            return Collections.emptyList();
        }
        BlockRect query = new BlockRect(minChunkX << 4, minChunkZ << 4, (maxChunkX << 4) + 15, (maxChunkZ << 4) + 15);
        int lowX = minChunkX - REACH_CHUNKS;
        int lowZ = minChunkZ - REACH_CHUNKS;
        int highX = maxChunkX + REACH_CHUNKS;
        int highZ = maxChunkZ + REACH_CHUNKS;
        List<Landmark> out = new ArrayList<>();
        for (int cellX = grid.cellOf(lowX); cellX <= grid.cellOf(highX); cellX++) {
            for (int cellZ = grid.cellOf(lowZ); cellZ <= grid.cellOf(highZ); cellZ++) {
                int[] chunk = grid.dungeonChunk(cellX, cellZ);
                if (chunk[0] < lowX || chunk[0] > highX || chunk[1] < lowZ || chunk[1] > highZ) {
                    continue;
                }
                Landmark landmark = predict(cellX, cellZ).landmark();
                if (landmark != null && touches(landmark, query)) {
                    out.add(landmark);
                }
            }
        }
        out.sort(Comparator.comparing(Landmark::id));
        return out;
    }

    /**
     * Returns a cell's prediction.
     *
     * @param cellX cell X
     * @param cellZ cell Z
     * @return the prediction, cached
     */
    public synchronized Prediction predict(int cellX, int cellZ) {
        long key = BoundedCache.key(cellX, cellZ);
        Prediction cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        if (!enabled()) {
            return Prediction.none(cellX, cellZ, grid.dungeonChunk(cellX, cellZ), Collections.emptyMap());
        }
        if (probe.active()) {
            // Up to 50 × 81 height samples: tested a few candidates per prefetch step, with the probe off.
            throw probe.miss(() -> step(cellX, cellZ));
        }
        return fetch(cellX, cellZ);
    }

    /** Tests the next few candidates of a cell, and caches its prediction once one passes or none is left. */
    private synchronized void step(int cellX, int cellZ) {
        long key = BoundedCache.key(cellX, cellZ);
        if (cache.get(key) != null) {
            return;
        }
        Progress state = progress.get(key);
        if (state == null) {
            state = progress.put(key, new Progress(cellX, cellZ));
        }
        Prediction done = state.advance(CANDIDATES_PER_STEP);
        if (done != null) {
            cache.put(key, done);
            progress.remove(key);
        }
    }

    /**
     * Returns the prediction of the cell a chunk lies in.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the prediction
     */
    public Prediction predictForChunk(int chunkX, int chunkZ) {
        return predict(grid.cellOf(chunkX), grid.cellOf(chunkZ));
    }

    private synchronized Prediction fetch(int cellX, int cellZ) {
        return cache.put(BoundedCache.key(cellX, cellZ), compute(cellX, cellZ));
    }

    private Prediction compute(int cellX, int cellZ) {
        long perfStart = PerfCounters.start();
        try {
            return compute0(cellX, cellZ);
        } finally {
            PerfCounters.add("rl.compute", perfStart);
        }
    }

    private Prediction compute0(int cellX, int cellZ) {
        return new Progress(cellX, cellZ).advance(Integer.MAX_VALUE);
    }

    /** One cell's candidates, tested in order, part-way or whole. */
    private final class Progress {

        private final int cellX;
        private final int cellZ;
        private final int[] chunk;
        private final int[][] candidates;
        private final Map<SiteRules.Verdict, Integer> rejected = new EnumMap<>(SiteRules.Verdict.class);
        private int next;

        Progress(int cellX, int cellZ) {
            this.cellX = cellX;
            this.cellZ = cellZ;
            this.chunk = grid.dungeonChunk(cellX, cellZ);
            this.candidates = grid.candidates(chunk[0], chunk[1]);
        }

        /**
         * Tests up to {@code count} more candidates.
         *
         * @return the prediction once a candidate passes or none is left; {@code null} while candidates remain
         */
        Prediction advance(int count) {
            for (int tested = 0; tested < count && next < candidates.length; tested++, next++) {
                int x = candidates[next][0];
                int z = candidates[next][1];
                SiteRules.Verdict verdict = rules.test(x, z);
                if (verdict == SiteRules.Verdict.PASS) {
                    return new Prediction(cellX, cellZ, chunk, next, x, z, landmark(x, z), rejected);
                }
                rejected.merge(verdict, 1, Integer::sum);
            }
            return next >= candidates.length ? Prediction.none(cellX, cellZ, chunk, rejected) : null;
        }
    }

    private Landmark landmark(int x, int z) {
        return Landmark.builder(id(x, z), LandmarkKind.ROGUELIKE)
            .footprint(lot(x, z))
            .certainty(Certainty.CANDIDATE)
            .groundEstimate(heights.height(x, z))
            .build();
    }

    /**
     * Returns the lot of a site: the 21 × 21 blocks around it.
     *
     * @param x the site's block X
     * @param z the site's block Z
     * @return the footprint
     */
    public static BlockRect lot(int x, int z) {
        return new BlockRect(x - LOT_HALF, z - LOT_HALF, x + LOT_HALF, z + LOT_HALF);
    }

    /**
     * Returns a Roguelike landmark's identity.
     *
     * @param x the site's block X
     * @param z the site's block Z
     * @return {@code roguelike@<x>,<z>}
     */
    public static String id(int x, int z) {
        return "roguelike@" + x + "," + z;
    }

    private static boolean touches(Landmark landmark, BlockRect query) {
        for (BlockRect rect : landmark.footprint()) {
            if (rect.maxX() >= query.minX() && rect.minX() <= query.maxX()
                && rect.maxZ() >= query.minZ()
                && rect.minZ() <= query.maxZ()) {
                return true;
            }
        }
        return false;
    }

    /** One cell's answer: its dungeon chunk and, when a candidate passes, which one and its landmark. */
    public static final class Prediction {

        private final int cellX;
        private final int cellZ;
        private final int chunkX;
        private final int chunkZ;
        private final int candidate;
        private final int x;
        private final int z;
        private final Landmark landmark;
        private final Map<SiteRules.Verdict, Integer> rejected;

        Prediction(int cellX, int cellZ, int[] chunk, int candidate, int x, int z, Landmark landmark,
            Map<SiteRules.Verdict, Integer> rejected) {
            this.cellX = cellX;
            this.cellZ = cellZ;
            this.chunkX = chunk[0];
            this.chunkZ = chunk[1];
            this.candidate = candidate;
            this.x = x;
            this.z = z;
            this.landmark = landmark;
            Map<SiteRules.Verdict, Integer> copy = new EnumMap<>(SiteRules.Verdict.class);
            copy.putAll(rejected);
            this.rejected = Collections.unmodifiableMap(copy);
        }

        static Prediction none(int cellX, int cellZ, int[] chunk, Map<SiteRules.Verdict, Integer> rejected) {
            return new Prediction(cellX, cellZ, chunk, -1, 0, 0, null, rejected);
        }

        /** Returns the cell X. */
        public int cellX() {
            return cellX;
        }

        /** Returns the cell Z. */
        public int cellZ() {
            return cellZ;
        }

        /** Returns the dungeon chunk's X. */
        public int chunkX() {
            return chunkX;
        }

        /** Returns the dungeon chunk's Z. */
        public int chunkZ() {
            return chunkZ;
        }

        /** Returns the index of the passing candidate, 0 to 49, or −1 when none passes. */
        public int candidate() {
            return candidate;
        }

        /** Returns whether a candidate passes. */
        public boolean hasSite() {
            return landmark != null;
        }

        /** Returns the site's block X; meaningless without a site. */
        public int x() {
            return x;
        }

        /** Returns the site's block Z; meaningless without a site. */
        public int z() {
            return z;
        }

        /** Returns the landmark, or {@code null} when no candidate passes. */
        public Landmark landmark() {
            return landmark;
        }

        /** Returns how many earlier candidates failed, by reason. */
        public Map<SiteRules.Verdict, Integer> rejected() {
            return rejected;
        }

        @Override
        public String toString() {
            return "cell " + cellX
                + ","
                + cellZ
                + " chunk "
                + chunkX
                + ","
                + chunkZ
                + (landmark == null ? " none" : " site " + x + "," + z + " #" + candidate)
                + " "
                + rejected;
        }
    }
}
