package com.tuxprogrammer.lostcities.asset.match;

/**
 * The main biome of a chunk, as biome matchers test it: the 2D biome at block (8, 8) of the chunk
 * (docs/new-spec/02-profiles-dimensions/index.md, "Main biome of a chunk"). The glue implements it over the world with
 * {@code core.assets.BiomeDescriptors} and a bounded per-world cache; tests implement it with a fixed descriptor.
 *
 * <p>
 * Side: both (server-side use).
 */
public interface BiomeSource {

    /**
     * Returns the facts of a chunk's main biome.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the descriptor, never null
     */
    BiomeDescriptor biomeOfChunk(int chunkX, int chunkZ);
}
