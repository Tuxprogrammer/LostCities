package com.tuxprogrammer.lostcities.compat.villagenames;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.tuxprogrammer.lostcities.layout.landmark.BlockRect;
import com.tuxprogrammer.lostcities.layout.landmark.Certainty;
import com.tuxprogrammer.lostcities.layout.landmark.Landmark;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkKind;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.worldgen.height.TerrainHeightOracle;

/**
 * The structure oracle for VillageNames' ocean monuments (decision log D32): a landmark core where a monument stands,
 * so cities, highways and oil rigs keep clear of it and its edge ring as they do of villages.
 *
 * <p>
 * For each 32-chunk cell near the rectangle, the cell's start chunk ({@link MonumentGrid#startChunk}) holds a monument
 * when its biomes pass VillageNames' name test ({@link MonumentGrid#siteAccepts}). The landmark is
 * {@code monument@<chunkX>,<chunkZ>} of kind {@link LandmarkKind#OCEAN_MONUMENT}, certainty {@link Certainty#EXACT},
 * its footprint the 58 × 58 building ({@link MonumentGrid#footprint}) and its ground the height oracle's median over
 * it (the sea floor), computed when first read. Pure and cached like {@link VillageOracle}.
 *
 * <p>
 * Side: server (world generation), and plain Java in tests.
 */
public final class MonumentOracle implements StructureOracle {

    /** How far, in chunks, a monument's footprint reaches from its start chunk: 21 blocks west, 36 east. */
    public static final int REACH_CHUNKS = 3;
    /** Starts kept in the cache. */
    public static final int CACHE_SIZE = 256;

    private static final Landmark NONE = Landmark.builder("none", LandmarkKind.OCEAN_MONUMENT)
        .footprint(new BlockRect(0, 0, 0, 0))
        .build();

    private final boolean enabled;
    private final MonumentGrid grid;
    private final BiomeNames biomes;
    private final TerrainHeightOracle heights;
    private final BoundedCache<Landmark> cache;

    /**
     * Creates the oracle of a world.
     *
     * @param worldSeed the world seed
     * @param enabled   whether monuments generate in this world ({@link VillageNamesSettings#monumentsOn()})
     * @param biomes    the biome provider's names
     * @param heights   the height oracle, or {@code null} for {@link GroundEstimates#FALLBACK_Y}
     * @param cacheSize the most start chunks cached
     */
    public MonumentOracle(long worldSeed, boolean enabled, BiomeNames biomes, TerrainHeightOracle heights,
        int cacheSize) {
        this.enabled = enabled;
        this.grid = new MonumentGrid(worldSeed);
        this.biomes = biomes;
        this.heights = heights;
        this.cache = new BoundedCache<>(cacheSize);
    }

    /** Returns the grid this oracle reads. */
    public MonumentGrid grid() {
        return grid;
    }

    @Override
    public synchronized List<Landmark> landmarksTouching(int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
        if (!enabled) {
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
                Landmark landmark = monumentAt(start[0], start[1]);
                if (landmark != null && VillageOracle.touches(landmark, query)) {
                    out.add(landmark);
                }
            }
        }
        out.sort(Comparator.comparing(Landmark::id));
        return out;
    }

    /**
     * Returns the monument whose start chunk this is, or {@code null}: the chunk must be its cell's start chunk and
     * pass the biome test.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the landmark, or {@code null}
     */
    public synchronized Landmark monumentAt(int chunkX, int chunkZ) {
        if (!enabled) {
            return null;
        }
        long key = BoundedCache.key(chunkX, chunkZ);
        Landmark cached = cache.get(key);
        if (cached == null) {
            cached = cache.put(key, compute(chunkX, chunkZ));
        }
        return cached == NONE ? null : cached;
    }

    private Landmark compute(int chunkX, int chunkZ) {
        int[] start = grid.startChunk(grid.cellOf(chunkX), grid.cellOf(chunkZ));
        if (start[0] != chunkX || start[1] != chunkZ || !MonumentGrid.siteAccepts(chunkX, chunkZ, biomes)) {
            return NONE;
        }
        BlockRect box = MonumentGrid.footprint(chunkX, chunkZ);
        return Landmark.builder(id(chunkX, chunkZ), LandmarkKind.OCEAN_MONUMENT)
            .footprint(box)
            .certainty(Certainty.EXACT)
            // Read only by the finder and the scenarios: computed on first read (D263).
            .groundEstimate(() -> GroundEstimates.median(heights, Collections.singletonList(box)))
            .build();
    }

    /**
     * Returns a monument landmark's identity.
     *
     * @param chunkX the start chunk X
     * @param chunkZ the start chunk Z
     * @return {@code monument@<chunkX>,<chunkZ>}
     */
    public static String id(int chunkX, int chunkZ) {
        return "monument@" + chunkX + "," + chunkZ;
    }
}
