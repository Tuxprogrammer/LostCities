package com.tuxprogrammer.lostcities.worldgen.buffer;

import java.util.Arrays;
import java.util.Objects;

import com.tuxprogrammer.lostcities.asset.BlockRef;

/**
 * A {@link ChunkBuffer} backed by a plain array, for unit tests, golden-chunk tests and any generation step that
 * builds a column before copying it into a chunk. It starts as all air.
 *
 * <p>
 * Storage index is {@code (y << 8) | (z << 4) | x}, so a horizontal layer is one contiguous run of 256 entries.
 *
 * <p>
 * Side: both (plain Java).
 */
public final class ArrayChunkBuffer implements ChunkBuffer {

    /** Number of blocks in a chunk column: 16 × 16 × 256. */
    public static final int VOLUME = WIDTH * WIDTH * HEIGHT;

    private final BlockRef[] blocks = new BlockRef[VOLUME];

    /** Creates a buffer full of air. */
    public ArrayChunkBuffer() {
        Arrays.fill(blocks, BlockRef.AIR);
    }

    @Override
    public BlockRef get(int x, int y, int z) {
        return blocks[index(x, y, z)];
    }

    @Override
    public void set(int x, int y, int z, BlockRef block) {
        blocks[index(x, y, z)] = Objects.requireNonNull(block, "block");
    }

    /**
     * Returns the storage index of a position.
     *
     * @param x local X, 0 to 15
     * @param y block-Y level, 0 to 255
     * @param z local Z, 0 to 15
     * @return {@code (y << 8) | (z << 4) | x}
     * @throws IndexOutOfBoundsException when the position is outside the chunk
     */
    static int index(int x, int y, int z) {
        if (!ChunkBuffer.contains(x, y, z)) {
            throw new IndexOutOfBoundsException("(" + x + ", " + y + ", " + z + ") is outside the chunk column");
        }
        return (y << 8) | (z << 4) | x;
    }
}
