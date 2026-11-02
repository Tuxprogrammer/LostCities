package com.tuxprogrammer.lostcities.api;

import net.minecraft.block.Block;

/**
 * The blocks of the chunk a generation event is about, while the terrain phase writes it (docs/api.md, Events).
 * Coordinates are chunk-local: X and Z 0 to 15, Y 0 to 255; a position outside is ignored on write and reads as air.
 * Writes go straight into the chunk being generated; never touch another chunk from an event (no cascading).
 *
 * <p>
 * Side: server, during world generation.
 */
public interface ILostChunkBlocks {

    /**
     * Returns a block.
     *
     * @param x local X
     * @param y block Y
     * @param z local Z
     * @return the block; air outside the chunk
     */
    Block getBlock(int x, int y, int z);

    /**
     * Returns a block's metadata.
     *
     * @param x local X
     * @param y block Y
     * @param z local Z
     * @return the metadata
     */
    int getMeta(int x, int y, int z);

    /**
     * Writes a block.
     *
     * @param x     local X
     * @param y     block Y
     * @param z     local Z
     * @param block the block
     * @param meta  its metadata
     */
    void setBlock(int x, int y, int z, Block block, int meta);
}
