package com.tuxprogrammer.lostcities.worldgen.terrain;

import com.tuxprogrammer.lostcities.asset.BlockRef;

/**
 * Whether a block exists in the running game, so terrain code can take a documented fallback when a pack mod is
 * absent (CONTRIBUTING § World generation rules, "Blocks by name"): the lane's {@code etfuturum:grass_path} falls
 * back to gravel, {@code chisel:moss} to mossy cobblestone.
 *
 * <p>
 * Side: both (plain Java). The overlay answers from the block registry; tests answer from a fixed set.
 */
@FunctionalInterface
public interface BlockAvailability {

    /** Every block exists: for tests that do not care. */
    BlockAvailability ALL = block -> true;

    /** Only {@code minecraft:} blocks exist: the vanilla game. */
    BlockAvailability VANILLA = block -> "minecraft".equals(block.modId());

    /**
     * Returns whether the block's mod is loaded and the block is registered.
     *
     * @param block the block
     * @return {@code true} when it can be written
     */
    boolean exists(BlockRef block);

    /**
     * Returns the first of the given blocks that exists.
     *
     * @param preferred the block wanted
     * @param fallback  the block to use when it is absent; must exist
     * @return {@code preferred} when it exists, else {@code fallback}
     */
    default BlockRef orElse(BlockRef preferred, BlockRef fallback) {
        return exists(preferred) ? preferred : fallback;
    }
}
