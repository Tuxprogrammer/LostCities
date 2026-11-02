package com.tuxprogrammer.lostcities.core.assets;

import java.util.Set;
import java.util.TreeSet;

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;

import com.tuxprogrammer.lostcities.asset.match.BiomeDescriptor;
import com.tuxprogrammer.lostcities.compat.rwg.RealisticBiomes;

/**
 * Builds the plain-data {@link BiomeDescriptor} that biome matchers test, from a world and a column: the provider
 * biome's {@code BiomeDictionary} types and name, and the RWG realistic biome where RWG shapes the terrain
 * (docs/new-spec/05-assets/asset-format.md, "Biome matchers"). Layout and worldgen call it with the chunk's main-biome
 * column, the 2D biome at block (8, 8) (docs/new-spec/02-profiles-dimensions/index.md, "Main biome of a chunk").
 *
 * <p>
 * Side: server (world generation).
 */
public final class BiomeDescriptors {

    private BiomeDescriptors() {}

    /**
     * Describes the biome of a column.
     *
     * @param world  the world
     * @param blockX the block X
     * @param blockZ the block Z
     * @return the descriptor
     */
    public static BiomeDescriptor at(World world, int blockX, int blockZ) {
        BiomeGenBase biome = world.getWorldChunkManager()
            .getBiomeGenAt(blockX, blockZ);
        return BiomeDescriptor
            .of(types(biome), biome == null ? "" : biome.biomeName, RealisticBiomes.at(world, blockX, blockZ));
    }

    /**
     * Returns the dictionary types of a biome.
     *
     * @param biome the biome, or null
     * @return the type names, upper case
     */
    public static Set<String> types(BiomeGenBase biome) {
        Set<String> out = new TreeSet<>();
        if (biome == null) {
            return out;
        }
        for (BiomeDictionary.Type type : BiomeDictionary.getTypesForBiome(biome)) {
            out.add(type.name());
        }
        return out;
    }
}
