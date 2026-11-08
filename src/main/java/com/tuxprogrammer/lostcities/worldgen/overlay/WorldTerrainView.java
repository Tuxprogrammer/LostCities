package com.tuxprogrammer.lostcities.worldgen.overlay;

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;

import com.tuxprogrammer.lostcities.layout.view.TerrainView;
import com.tuxprogrammer.lostcities.worldgen.height.ChunkHeights;
import com.tuxprogrammer.lostcities.worldgen.height.TerrainHeightOracle;

/**
 * The layout's {@link TerrainView} over a wrapped world (decision log D51): chunk heights from the height oracle's
 * group samples ({@link ChunkHeights}, 0005 §4) and biome tests from the world's biome provider, which is pure on
 * RWG2 and classic RWG (0006 §2). It never reads blocks or loads chunks, so it answers the same for a chunk whatever
 * has been generated. The biome is the one at block (8, 8) of the chunk, as upstream's main biome is.
 *
 * <p>
 * M05 hands it to M03's {@code LayoutPlanner}. Server thread only. Side: server.
 */
public final class WorldTerrainView implements TerrainView {

    /** Block offset of the column whose biome is the chunk's main biome. */
    static final int CENTER = 8;

    private final World world;
    private final ChunkHeights heights;
    private final TerrainHeightOracle columns;

    /**
     * Creates the view, with column heights from the chunk heights alone.
     *
     * @param world   the wrapped world
     * @param heights its oracle's chunk heights
     */
    public WorldTerrainView(World world, ChunkHeights heights) {
        this(world, heights, null);
    }

    /**
     * Creates the view.
     *
     * @param world   the wrapped world
     * @param heights its oracle's chunk heights
     * @param columns its per-column oracle, behind the world's column cache and probe; {@code null} for none
     */
    public WorldTerrainView(World world, ChunkHeights heights, TerrainHeightOracle columns) {
        this.world = world;
        this.heights = heights;
        this.columns = columns;
    }

    @Override
    public int columnHeight(int blockX, int blockZ) {
        return columns == null ? heights.height(blockX >> 4, blockZ >> 4) : columns.height(blockX, blockZ);
    }

    @Override
    public int height(int chunkX, int chunkZ) {
        return heights.height(chunkX, chunkZ);
    }

    @Override
    public boolean isWaterBiome(int chunkX, int chunkZ) {
        BiomeGenBase biome = biome(chunkX, chunkZ);
        return biome != null && (BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.OCEAN)
            || BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.BEACH)
            || BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.RIVER)
            || BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.WATER));
    }

    @Override
    public boolean isOcean(int chunkX, int chunkZ) {
        BiomeGenBase biome = biome(chunkX, chunkZ);
        return biome != null && BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.OCEAN);
    }

    private BiomeGenBase biome(int chunkX, int chunkZ) {
        return PlanThreads.manager(world)
            .getBiomeGenAt((chunkX << 4) + CENTER, (chunkZ << 4) + CENTER);
    }
}
