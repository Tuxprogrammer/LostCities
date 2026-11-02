package com.tuxprogrammer.lostcities.worldgen.buffer;

import com.tuxprogrammer.lostcities.asset.BlockRef;

/**
 * The blocks of one chunk column, as world generation reads and writes them. Every write the overlay makes goes
 * through this interface, so the same writing code runs against a real chunk in the game and against an
 * {@link ArrayChunkBuffer} in unit and golden-chunk tests (decision log D6, docs/design/0005-rwg-overlay.md §2).
 *
 * <p>
 * Coordinates are local to the chunk: {@code x} and {@code z} from 0 to {@link #WIDTH} - 1, {@code y} from 0 to
 * {@link #HEIGHT} - 1 (block-Y levels; 1.7.10's build range is Y 0 to 255). Implementations reject coordinates
 * outside it rather than write into a neighbor, because generation never writes outside its own chunk (no cascading
 * generation, CONTRIBUTING § World generation rules).
 *
 * <p>
 * The interface holds no Minecraft type. The world-backed implementation over {@code ExtendedBlockStorage} arrives
 * with the overlay in M04; it must allocate nothing per block.
 *
 * <p>
 * Side: server (world generation), and plain Java in tests.
 */
public interface ChunkBuffer {

    /** Width of a chunk along X and along Z, in blocks. */
    int WIDTH = 16;

    /** Height of a chunk column in 1.7.10, in blocks: Y 0 to 255. */
    int HEIGHT = 256;

    /**
     * Returns the block at a position.
     *
     * @param x local X, 0 to 15
     * @param y block-Y level, 0 to 255
     * @param z local Z, 0 to 15
     * @return the block there; {@link BlockRef#AIR} where nothing was written
     * @throws IndexOutOfBoundsException when the position is outside the chunk
     */
    BlockRef get(int x, int y, int z);

    /**
     * Writes a block at a position, replacing what was there.
     *
     * @param x     local X, 0 to 15
     * @param y     block-Y level, 0 to 255
     * @param z     local Z, 0 to 15
     * @param block the block to write; never {@code null} (write {@link BlockRef#AIR} to clear)
     * @throws IndexOutOfBoundsException when the position is outside the chunk
     * @throws NullPointerException      when {@code block} is {@code null}
     */
    void set(int x, int y, int z, BlockRef block);

    /**
     * Returns {@code true} when a position lies inside a chunk column.
     *
     * @param x local X
     * @param y block-Y level
     * @param z local Z
     * @return whether {@link #get} and {@link #set} accept the position
     */
    static boolean contains(int x, int y, int z) {
        return x >= 0 && x < WIDTH && z >= 0 && z < WIDTH && y >= 0 && y < HEIGHT;
    }
}
