package com.tuxprogrammer.lostcities.compat.roguelike;

import java.util.Arrays;
import java.util.List;

import net.minecraft.block.material.Material;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraftforge.common.BiomeDictionary;

/**
 * The site rules' biome facts from a world's biome provider ({@code WorldChunkManager.getBiomeGenAt}), never from a
 * chunk's stored biome array, so the answer never depends on which chunks are loaded. Roguelike's own test reads
 * {@code World.getBiomeGenForCoords}, which is that provider for a chunk not generated yet and the chunk's stored
 * (dithered, on RWG) biome otherwise; near a biome border the two can differ, as for VillageNames' monuments (D91).
 *
 * <ul>
 * <li>Rejected biomes: any of the Forge dictionary types {@code RIVER}, {@code BEACH}, {@code MUSHROOM},
 * {@code OCEAN}. Source:
 * https://github.com/GTNewHorizons/Roguelike-Dungeons/blob/1.6.6-GTNH/src/main/java/greymerk/roguelike/dungeon/Dungeon.java#L165-L174</li>
 * <li>Grass top: the biome's {@code topBlock} has material {@code grass}, which {@code validGroundBlock} skips. Source:
 * https://github.com/GTNewHorizons/Roguelike-Dungeons/blob/1.6.6-GTNH/src/main/java/greymerk/roguelike/worldgen/WorldEditor.java#L134-L150.
 * RWG's realistic surfaces mostly lay the biome's top block; where they lay stone or gravel instead the estimate is
 * one block low, which the steering mixin absorbs.</li>
 * </ul>
 *
 * <p>
 * Server thread only: vanilla's biome cache and RWG's are not thread-safe. Side: server.
 */
final class WorldRoguelikeTerrain implements RoguelikeTerrain {

    private static final List<BiomeDictionary.Type> REJECTED = Arrays.asList(
        BiomeDictionary.Type.RIVER,
        BiomeDictionary.Type.BEACH,
        BiomeDictionary.Type.MUSHROOM,
        BiomeDictionary.Type.OCEAN);

    private final WorldChunkManager manager;

    /**
     * Wraps a biome provider.
     *
     * @param manager the world's chunk manager
     */
    WorldRoguelikeTerrain(WorldChunkManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean biomeRejects(int blockX, int blockZ) {
        BiomeGenBase biome = manager.getBiomeGenAt(blockX, blockZ);
        if (biome == null) {
            return true;
        }
        for (BiomeDictionary.Type type : BiomeDictionary.getTypesForBiome(biome)) {
            if (REJECTED.contains(type)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean grassTop(int blockX, int blockZ) {
        BiomeGenBase biome = manager.getBiomeGenAt(blockX, blockZ);
        return biome != null && biome.topBlock != null && biome.topBlock.getMaterial() == Material.grass;
    }
}
