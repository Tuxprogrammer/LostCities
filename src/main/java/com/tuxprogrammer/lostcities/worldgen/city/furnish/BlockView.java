package com.tuxprogrammer.lostcities.worldgen.city.furnish;

import com.tuxprogrammer.lostcities.asset.BlockRef;

/**
 * Read access to one chunk's blocks at chunk-local X and Z and block Y, for the furnish pass: the terrain phase's
 * buffer, or the populated chunk itself. Side: both (plain Java).
 */
@FunctionalInterface
public interface BlockView {

    /**
     * Returns a block.
     *
     * @param x local X, 0 to 15
     * @param y block Y
     * @param z local Z, 0 to 15
     * @return the block, {@link BlockRef#AIR} for air
     */
    BlockRef get(int x, int y, int z);
}
