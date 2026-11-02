package com.tuxprogrammer.lostcities.compat.witchery;

/**
 * Witchery's biome test at one block column, as the world's biome provider answers it: Witchery skips a candidate
 * whose biome at block {@code (x0 + 5, z0 + 5)} is one of its disallowed biomes (every biome of the dictionary types
 * swamp, river, ocean, mountain, jungle and beach). At runtime {@code WitcheryConfig} builds it from Witchery's own
 * list and the world's {@code WorldChunkManager}; tests use fakes.
 *
 * <p>
 * Side: server (world generation), and plain Java in tests.
 */
@FunctionalInterface
public interface WitcheryBiomes {

    /**
     * Returns whether Witchery may build at a column.
     *
     * @param blockX block X
     * @param blockZ block Z
     * @return {@code false} when the column's biome is disallowed
     */
    boolean allows(int blockX, int blockZ);
}
