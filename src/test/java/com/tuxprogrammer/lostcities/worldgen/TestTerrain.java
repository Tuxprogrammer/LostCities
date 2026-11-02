package com.tuxprogrammer.lostcities.worldgen;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.worldgen.buffer.ArrayChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.buffer.ChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.height.TerrainHeightOracle;
import com.tuxprogrammer.lostcities.worldgen.terrain.BlockAvailability;
import com.tuxprogrammer.lostcities.worldgen.terrain.NameBlockClassifier;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainContext;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainSettings;

/**
 * An RWG-like stand-in terrain for overlay tests: rolling ground from a pure function of world X and Z, bedrock at Y
 * 0 to 4, stone, three layers of dirt and grass (sand at the shore), water up to Y 62 as RWG fills it, a small cave
 * in some columns, and a tree (log and leaves) on some grass columns. Its oracle answers the same surface plus one,
 * as RWG's does.
 */
public final class TestTerrain {

    public static final BlockRef BEDROCK = BlockRef.of("minecraft:bedrock", 0);
    public static final BlockRef STONE = BlockRef.of("minecraft:stone", 0);
    public static final BlockRef DIRT = BlockRef.of("minecraft:dirt", 0);
    public static final BlockRef GRASS = BlockRef.of("minecraft:grass", 0);
    public static final BlockRef SAND = BlockRef.of("minecraft:sand", 0);
    public static final BlockRef WATER = BlockRef.of("minecraft:water", 0);
    public static final BlockRef LOG = BlockRef.of("minecraft:log", 0);
    public static final BlockRef LEAVES = BlockRef.of("minecraft:leaves", 0);

    /** The fixture's mean ground height. */
    public static final int BASE = 68;

    private TestTerrain() {}

    /** Returns the stone surface of a world column: the Y of its top solid block. */
    public static int surface(int worldX, int worldZ) {
        return BASE + (int) Math.round(9 * Math.sin(worldX / 37.0) + 7 * Math.cos(worldZ / 29.0));
    }

    /** Returns the fixture's height oracle: the surface plus one. */
    public static TerrainHeightOracle oracle() {
        return (x, z) -> surface(x, z) + 1;
    }

    /** Returns a terrain context with default settings and vanilla blocks. */
    public static TerrainContext context(long seed) {
        return new TerrainContext(
            seed,
            0,
            TerrainSettings.defaults(),
            new NameBlockClassifier(),
            BlockAvailability.VANILLA);
    }

    /**
     * Generates one chunk of the fixture.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return a new buffer
     */
    public static ArrayChunkBuffer chunk(int chunkX, int chunkZ) {
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                column(buffer, x, z, (chunkX << 4) + x, (chunkZ << 4) + z);
            }
        }
        return buffer;
    }

    private static void column(ChunkBuffer buffer, int x, int z, int worldX, int worldZ) {
        int top = surface(worldX, worldZ);
        for (int y = 0; y <= 4; y++) {
            buffer.set(x, y, z, BEDROCK);
        }
        for (int y = 5; y <= top; y++) {
            BlockRef block;
            if (y == top) {
                block = top < 64 ? SAND : GRASS;
            } else if (y > top - 4) {
                block = top < 64 ? SAND : DIRT;
            } else {
                block = STONE;
            }
            buffer.set(x, y, z, block);
        }
        for (int y = top + 1; y < 63; y++) {
            buffer.set(x, y, z, WATER);
        }
        // A cave pocket a few blocks under the surface in some columns.
        if (Math.floorMod(worldX * 7 + worldZ * 3, 11) == 0) {
            for (int y = top - 9; y <= top - 7; y++) {
                buffer.set(x, y, z, BlockRef.AIR);
            }
        }
        // A thin tree on some grass columns.
        if (top >= 64 && Math.floorMod(worldX * 13 + worldZ * 5, 37) == 0 && top + 6 <= 255) {
            for (int y = top + 1; y <= top + 4; y++) {
                buffer.set(x, y, z, LOG);
            }
            buffer.set(x, top + 5, z, LEAVES);
        }
    }
}
