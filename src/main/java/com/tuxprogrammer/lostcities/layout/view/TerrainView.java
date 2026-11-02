package com.tuxprogrammer.lostcities.layout.view;

/**
 * What the layout reads about natural terrain, per chunk: the height oracle's chunk height (0005 §4) and the biome
 * tests it needs. The terrain glue (M04) implements it over RWG's pure height oracle and biome provider; tests use
 * fakes. It must be pure: the same answer for a chunk whatever has been generated.
 *
 * <p>
 * Spec: docs/new-spec/03-terrain/height-mapping.md, docs/new-spec/04-city-layout/index.md (biome rows). Side: server.
 */
public interface TerrainView {

    /**
     * Returns the chunk's natural height, as upstream's {@code ChunkHeightmap.getHeight()}: the value city detection
     * compares with {@code cityMinHeight}/{@code cityMaxHeight} and city levels are taken from.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return block Y
     */
    int height(int chunkX, int chunkZ);

    /**
     * Returns whether the chunk's main biome is water for bridging: a {@code BiomeDictionary} type {@code OCEAN},
     * {@code BEACH}, {@code RIVER} or {@code WATER} (upstream's {@code #is_ocean}, {@code #is_deep_ocean},
     * {@code #is_beach}, {@code #is_river}).
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return whether the biome is a water biome
     */
    boolean isWaterBiome(int chunkX, int chunkZ);

    /**
     * Returns whether the chunk's main biome has the {@code BiomeDictionary} type {@code OCEAN} (upstream's
     * {@code isOcean}: {@code #is_ocean} or {@code #is_deep_ocean}).
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return whether the biome is an ocean
     */
    boolean isOcean(int chunkX, int chunkZ);
}
