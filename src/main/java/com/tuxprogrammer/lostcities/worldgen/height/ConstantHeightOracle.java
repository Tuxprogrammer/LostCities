package com.tuxprogrammer.lostcities.worldgen.height;

/**
 * A height oracle for worlds with one terrain height everywhere: superflat worlds (the preset's top layer) and, after
 * beta, PersonalSpace (its configured layer height) (docs/design/0005-rwg-overlay.md §4).
 *
 * <p>
 * Immutable. Side: both (plain Java).
 */
public final class ConstantHeightOracle implements TerrainHeightOracle {

    private final int height;

    /**
     * Creates the oracle.
     *
     * @param height the Y of the first block above the terrain
     */
    public ConstantHeightOracle(int height) {
        this.height = height;
    }

    @Override
    public int height(int blockX, int blockZ) {
        return height;
    }
}
