package com.tuxprogrammer.lostcities.compat.witchery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.tuxprogrammer.lostcities.compat.villagenames.GroundEstimates;
import com.tuxprogrammer.lostcities.layout.landmark.BlockRect;
import com.tuxprogrammer.lostcities.layout.landmark.Certainty;
import com.tuxprogrammer.lostcities.layout.landmark.Landmark;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkKind;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.worldgen.height.TerrainHeightOracle;

/**
 * The structure oracle for Witchery's standalone structures (0006 §2; coexistence.md § Witchery): a landmark lot over
 * every grid cell's candidate chunk whose biome Witchery accepts.
 *
 * <p>
 * For each cell near the rectangle, the candidate chunk ({@link WitcheryGrid#candidateChunk}) holds a structure when
 * the biome at block {@code (x0 + 5, z0 + 5)} passes Witchery's test ({@link WitcheryBiomes}). The structure's type
 * comes from a shuffle with {@code world.rand} drawn before the grid reseeds it, so it is not predictable; the
 * landmark {@code witchery@<chunkX>,<chunkZ>} is of kind {@link LandmarkKind#WITCHERY}, certainty
 * {@link Certainty#UNION}, its footprint the union of every enabled type's box in every facing
 * ({@link WitcherySettings#unionOffsets()}: 23 × 23 blocks, offsets −9 to +13), and its ground the height oracle's
 * median over that lot.
 *
 * <p>
 * RWG2's mixin on {@code WitcheryComponent.calcGroundHeight} can still cancel a structure, on uneven or hollow ground
 * (which reads blocks) or within 8 blocks of an RWG2 landmark (pure, but tested against the real box of the type
 * drawn, which is unknown). The oracle keeps the union lot either way: a cancelled structure leaves natural ground, as
 * any empty lot does (0006 §5), and reserving the lot is what keeps a structure that does stand out of the city.
 *
 * <p>
 * Pure: answers depend only on the world seed, Witchery's config, the biome provider and the height oracle. Cached per
 * candidate chunk in a bounded cache this object owns. Synchronized; server thread in practice. Side: server (world
 * generation), and plain Java in tests.
 */
public final class WitcheryOracle implements StructureOracle {

    /** Candidate chunks kept in the cache. */
    public static final int CACHE_SIZE = 512;
    /** Witchery's biome test column, blocks from the candidate chunk's origin on each axis (its {@code midX}). */
    public static final int BIOME_OFFSET = 5;

    private static final Landmark NONE = Landmark.builder("none", LandmarkKind.WITCHERY)
        .footprint(new BlockRect(0, 0, 0, 0))
        .build();

    private final WitcherySettings settings;
    private final WitcheryGrid grid;
    private final WitcheryBiomes biomes;
    private final TerrainHeightOracle heights;
    private final int[] offsets;
    private final int reachChunks;
    private final BoundedCache<Landmark> cache;

    /**
     * Creates the oracle of a world.
     *
     * @param worldSeed the world seed
     * @param settings  Witchery's settings
     * @param biomes    Witchery's biome test over the world's biome provider
     * @param heights   the height oracle, or {@code null} for {@link GroundEstimates#FALLBACK_Y}
     * @param cacheSize the most candidate chunks cached
     */
    public WitcheryOracle(long worldSeed, WitcherySettings settings, WitcheryBiomes biomes, TerrainHeightOracle heights,
        int cacheSize) {
        this.settings = settings;
        this.grid = new WitcheryGrid(worldSeed, settings.cellSize(), WitcheryGrid.SEPARATION);
        this.biomes = biomes;
        this.heights = heights;
        this.offsets = settings.on() ? settings.unionOffsets() : new int[] { 0, 0, 15, 15 };
        int reach = 0;
        for (int offset : offsets) {
            reach = Math.max(reach, Math.abs(offset));
        }
        this.reachChunks = reach / 16 + 1;
        this.cache = new BoundedCache<>(cacheSize);
    }

    /** Returns the grid this oracle reads. */
    public WitcheryGrid grid() {
        return grid;
    }

    /** Returns the settings this oracle reads. */
    public WitcherySettings settings() {
        return settings;
    }

    @Override
    public synchronized List<Landmark> landmarksTouching(int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
        if (!settings.on()) {
            return Collections.emptyList();
        }
        BlockRect query = new BlockRect(minChunkX << 4, minChunkZ << 4, (maxChunkX << 4) + 15, (maxChunkZ << 4) + 15);
        List<Landmark> out = new ArrayList<>();
        int lowX = minChunkX - reachChunks;
        int lowZ = minChunkZ - reachChunks;
        int highX = maxChunkX + reachChunks;
        int highZ = maxChunkZ + reachChunks;
        for (int cellX = grid.cellOf(lowX); cellX <= grid.cellOf(highX); cellX++) {
            for (int cellZ = grid.cellOf(lowZ); cellZ <= grid.cellOf(highZ); cellZ++) {
                int[] candidate = grid.candidateChunk(cellX, cellZ);
                if (candidate[0] < lowX || candidate[0] > highX || candidate[1] < lowZ || candidate[1] > highZ) {
                    continue;
                }
                Landmark landmark = structureAt(candidate[0], candidate[1]);
                if (landmark != null && touches(landmark, query)) {
                    out.add(landmark);
                }
            }
        }
        out.sort(Comparator.comparing(Landmark::id));
        return out;
    }

    /**
     * Returns the lot whose candidate chunk this is, or {@code null}: the chunk must be its cell's candidate and pass
     * the biome test.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the landmark, or {@code null}
     */
    public synchronized Landmark structureAt(int chunkX, int chunkZ) {
        if (!settings.on()) {
            return null;
        }
        long key = BoundedCache.key(chunkX, chunkZ);
        Landmark cached = cache.get(key);
        if (cached == null) {
            // Computed whole before it is cached: a height probe abandoning the ground estimate leaves nothing.
            cached = cache.put(key, compute(chunkX, chunkZ));
        }
        return cached == NONE ? null : cached;
    }

    /**
     * Returns whether the biome test accepts a candidate chunk, whatever the grid says; for the scenario's report.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return whether Witchery's biome test passes there
     */
    public boolean biomeAccepts(int chunkX, int chunkZ) {
        return biomes.allows((chunkX << 4) + BIOME_OFFSET, (chunkZ << 4) + BIOME_OFFSET);
    }

    /**
     * Returns the union lot of a candidate chunk.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the block rectangle every enabled type and facing lies in
     */
    public BlockRect lot(int chunkX, int chunkZ) {
        int originX = chunkX << 4;
        int originZ = chunkZ << 4;
        return new BlockRect(originX + offsets[0], originZ + offsets[1], originX + offsets[2], originZ + offsets[3]);
    }

    private Landmark compute(int chunkX, int chunkZ) {
        if (!grid.isCandidate(chunkX, chunkZ) || !biomeAccepts(chunkX, chunkZ)) {
            return NONE;
        }
        BlockRect lot = lot(chunkX, chunkZ);
        return Landmark.builder(id(chunkX, chunkZ), LandmarkKind.WITCHERY)
            .footprint(lot)
            .certainty(Certainty.UNION)
            .groundEstimate(GroundEstimates.median(heights, Collections.singletonList(lot)))
            .build();
    }

    /**
     * Returns a Witchery landmark's identity.
     *
     * @param chunkX the candidate chunk X
     * @param chunkZ the candidate chunk Z
     * @return {@code witchery@<chunkX>,<chunkZ>}
     */
    public static String id(int chunkX, int chunkZ) {
        return "witchery@" + chunkX + "," + chunkZ;
    }

    private static boolean touches(Landmark landmark, BlockRect query) {
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
