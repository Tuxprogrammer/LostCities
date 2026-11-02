package com.tuxprogrammer.lostcities.compat.villagenames;

import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;

/**
 * Biome names from a world's biome provider, {@code WorldChunkManager.getBiomeGenAt}: the provider VillageNames' start
 * and pieces read, and, on RWG, a pure function of the seed (RWG2's and classic RWG's {@code ChunkManagerRealistic},
 * which cache per column). It never reads a chunk's stored biome array, so it never depends on which chunks are
 * loaded.
 *
 * <p>
 * Server thread only: vanilla's biome cache and RWG's are not thread-safe. Side: server.
 */
final class ChunkManagerBiomeNames implements BiomeNames {

    private final WorldChunkManager manager;

    /**
     * Wraps a biome provider.
     *
     * @param manager the world's chunk manager
     */
    ChunkManagerBiomeNames(WorldChunkManager manager) {
        this.manager = manager;
    }

    @Override
    public String nameAt(int blockX, int blockZ) {
        BiomeGenBase biome = manager.getBiomeGenAt(blockX, blockZ);
        return biome == null ? null : biome.biomeName;
    }
}
