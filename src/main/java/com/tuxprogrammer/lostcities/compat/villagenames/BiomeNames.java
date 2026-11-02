package com.tuxprogrammer.lostcities.compat.villagenames;

/**
 * The biome name of a block column as the world's biome provider reports it ({@code BiomeGenBase.biomeName} of
 * {@code WorldChunkManager.getBiomeGenAt}), the only world input VillageNames' site tests read. At runtime it is
 * {@code ChunkManagerBiomeNames}; tests use fakes.
 *
 * <p>
 * Side: server (world generation), and plain Java in tests.
 */
@FunctionalInterface
public interface BiomeNames {

    /**
     * Returns the biome name of a column.
     *
     * @param blockX block X
     * @param blockZ block Z
     * @return the biome name, or {@code null} when the provider has none
     */
    String nameAt(int blockX, int blockZ);
}
