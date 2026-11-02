package com.tuxprogrammer.lostcities.compat.rwg;

import net.minecraft.world.World;
import net.minecraft.world.biome.WorldChunkManager;

import com.tuxprogrammer.lostcities.worldgen.height.TerrainHeightOracle;

import rwg.biomes.realistic.RealisticBiomeBase;
import rwg.world.ChunkGeneratorRealistic;
import rwg.world.ChunkManagerRealistic;

/**
 * The height oracle on classic RWG worlds: a private second {@code ChunkGeneratorRealistic(world, seed)} asked for
 * heights with {@code getNewNoise} (docs/design/0005-rwg-overlay.md §4). Classic RWG has no world-less constructor.
 *
 * <ul>
 * <li>The constructor fires {@code InitMapGenEvent} four times (caves, village, stronghold, mineshaft); the private
 * instance's map generators are never used, so the listeners that replace them (Biomes O' Plenty, VillageNames) only
 * build objects that stay unused. It also calls {@code CanyonColor.init(seed)}, a static with the world's own seed, so
 * the value it sets is the one the world's generator set. Source:
 * https://github.com/GTNewHorizons/Realistic-World-Gen/blob/alpha-1.5.3/src/main/java/rwg/world/ChunkGeneratorRealistic.java#L107-L125</li>
 * <li>{@code getNewNoise} has no stride: it computes all 256 columns. Stone fills {@code y ≤ (int) noise}, so the
 * height is {@code (int) noise + 1}. Source: #L208-L232.</li>
 * <li>Never the live generator: {@code getNewNoise} has the same {@code mapGenBiomes} side effect as RWG2's
 * (#L365-L381).</li>
 * </ul>
 *
 * <p>
 * Load this class only when {@code Loader.isModLoaded("RWG")}. Server thread only. Side: server.
 */
public final class RwgHeightOracle implements TerrainHeightOracle {

    private final ChunkGeneratorRealistic sampler;
    private final ChunkManagerRealistic manager;
    private final RealisticBiomeBase[] biomes = new RealisticBiomeBase[256];

    private RwgHeightOracle(ChunkGeneratorRealistic sampler, ChunkManagerRealistic manager) {
        this.sampler = sampler;
        this.manager = manager;
    }

    /**
     * Builds the oracle of a world.
     *
     * @param world the world, whose chunk manager must be classic RWG's
     * @return the oracle, or {@code null} when the world's chunk manager is not classic RWG's
     */
    public static RwgHeightOracle create(World world) {
        WorldChunkManager chunkManager = world.getWorldChunkManager();
        if (!(chunkManager instanceof ChunkManagerRealistic)) {
            return null;
        }
        return new RwgHeightOracle(
            new ChunkGeneratorRealistic(world, world.getSeed()),
            (ChunkManagerRealistic) chunkManager);
    }

    @Override
    public int height(int blockX, int blockZ) {
        float[] noise = sampler.getNewNoise(manager, (blockX >> 4) << 4, (blockZ >> 4) << 4, biomes);
        // RWG's arrays are x-major: generateTerrain writes column (x, z) from noise[x × 16 + z].
        return (int) noise[(blockX & 15) * 16 + (blockZ & 15)] + 1;
    }
}
