package com.tuxprogrammer.lostcities.worldgen.buffer;

import java.util.Random;

import com.tuxprogrammer.lostcities.asset.BlockRef;

/**
 * A stand-in generator for the golden-chunk harness until the layout (M03) and the overlay (M04) exist: simple layered
 * ground and one pillar, all decided by a {@link Random} seeded from the world seed and the chunk coordinates, as
 * CONTRIBUTING's determinism rule asks of real generation. Its output is pinned by the goldens in
 * {@code src/test/resources/golden/}, which were computed by an independent re-implementation outside Java.
 */
final class GoldenFixtureTerrain {

    static final BlockRef BEDROCK = BlockRef.of("minecraft:bedrock", 0);
    static final BlockRef STONE = BlockRef.of("minecraft:stone", 0);
    static final BlockRef DIRT = BlockRef.of("minecraft:dirt", 0);
    static final BlockRef GRASS = BlockRef.of("minecraft:grass", 0);

    /** Lowest ground level, in block-Y. */
    static final int GROUND_MIN = 60;
    /** Number of possible ground levels above {@link #GROUND_MIN}. */
    static final int GROUND_SPREAD = 10;
    /** Tallest pillar, in blocks. */
    static final int PILLAR_MAX = 20;
    /** Stone-brick metadata variants (plain, mossy, cracked, chiseled). */
    static final int BRICK_VARIANTS = 4;

    private GoldenFixtureTerrain() {}

    /**
     * Generates one chunk column.
     *
     * @param seed   the world seed
     * @param chunkX the chunk's X
     * @param chunkZ the chunk's Z
     * @return a new buffer holding the column
     */
    static ArrayChunkBuffer generate(long seed, int chunkX, int chunkZ) {
        // The same per-chunk seed shape vanilla uses for its chunk randoms.
        Random random = new Random(seed * 341873128712L + chunkX * 132897987541L + chunkZ);
        int ground = GROUND_MIN + random.nextInt(GROUND_SPREAD);
        int pillarX = random.nextInt(ChunkBuffer.WIDTH);
        int pillarZ = random.nextInt(ChunkBuffer.WIDTH);
        int pillarHeight = 1 + random.nextInt(PILLAR_MAX);
        BlockRef brick = BlockRef.of("minecraft:stonebrick", random.nextInt(BRICK_VARIANTS));
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        for (int z = 0; z < ChunkBuffer.WIDTH; z++) {
            for (int x = 0; x < ChunkBuffer.WIDTH; x++) {
                buffer.set(x, 0, z, BEDROCK);
                for (int y = 1; y <= ground; y++) {
                    buffer.set(x, y, z, y == ground ? GRASS : y > ground - 4 ? DIRT : STONE);
                }
            }
        }
        for (int y = ground + 1; y <= ground + pillarHeight; y++) {
            buffer.set(pillarX, y, pillarZ, brick);
        }
        return buffer;
    }
}
