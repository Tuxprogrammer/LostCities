package com.tuxprogrammer.lostcities.worldgen.overlay;

import net.minecraft.world.World;

import com.tuxprogrammer.lostcities.asset.match.BiomeDescriptor;
import com.tuxprogrammer.lostcities.asset.match.BiomeSource;
import com.tuxprogrammer.lostcities.core.assets.BiomeDescriptors;

/**
 * The asset matchers' {@link BiomeSource} over a wrapped world: the chunk's main biome, the 2D biome at block (8, 8)
 * with its RWG realistic biome, from the world's biome provider, which is pure on RWG2 and classic RWG (0006 §2). It
 * never reads blocks or loads chunks. A direct-mapped cache of {@link #CAPACITY} chunks owned by the world keeps the
 * layout's repeated asks cheap; eviction never changes an answer.
 *
 * <p>
 * Server thread only. Side: server.
 */
public final class WorldBiomes implements BiomeSource {

    /** Chunks cached. */
    public static final int CAPACITY = 4096;

    private static final int MASK = CAPACITY - 1;
    private static final int CENTER = 8;

    private final World world;
    private final long[] keys = new long[CAPACITY];
    private final BiomeDescriptor[] values = new BiomeDescriptor[CAPACITY];

    /**
     * Creates the source of one world.
     *
     * @param world the world
     */
    public WorldBiomes(World world) {
        this.world = world;
    }

    @Override
    public BiomeDescriptor biomeOfChunk(int chunkX, int chunkZ) {
        long key = ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
        int slot = ((chunkX * 0x9E3779B1) ^ (chunkZ * 0x85EBCA77)) >>> 4 & MASK;
        BiomeDescriptor value = values[slot];
        if (value != null && keys[slot] == key) {
            return value;
        }
        value = BiomeDescriptors.at(world, (chunkX << 4) + CENTER, (chunkZ << 4) + CENTER);
        keys[slot] = key;
        values[slot] = value;
        return value;
    }

    /** Empties the cache; the world is unloading. */
    public void clear() {
        java.util.Arrays.fill(values, null);
    }
}
