package com.tuxprogrammer.lostcities.compat.villagenames;

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import com.tuxprogrammer.lostcities.worldgen.overlay.PlanThreads;

/**
 * Biome names from a world's biome provider, {@code WorldChunkManager.getBiomeGenAt}: the provider VillageNames' start
 * and pieces read, and, on RWG, a pure function of the seed (RWG2's and classic RWG's {@code ChunkManagerRealistic},
 * which cache per column). It never reads a chunk's stored biome array, so it never depends on which chunks are
 * loaded.
 *
 * <p>
 * Vanilla's biome cache and RWG's are not thread-safe, so a world's plan worker asks its own copy of the provider
 * ({@link PlanThreads#manager(World)}, D-plan-worker-2). Side: server.
 */
final class ChunkManagerBiomeNames implements BiomeNames {

    private final World world;

    /**
     * Wraps a world's biome provider.
     *
     * @param world the world
     */
    ChunkManagerBiomeNames(World world) {
        this.world = world;
    }

    @Override
    public String nameAt(int blockX, int blockZ) {
        BiomeGenBase biome = PlanThreads.manager(world)
            .getBiomeGenAt(blockX, blockZ);
        return biome == null ? null : biome.biomeName;
    }
}
