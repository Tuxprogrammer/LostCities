package com.tuxprogrammer.lostcities.worldgen.terrain;
// Ported from Lost Cities (MIT): https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/LostCityTerrainFeature.java

import com.tuxprogrammer.lostcities.worldgen.buffer.ChunkBuffer;

/**
 * Upstream's {@code fixTorches} rule, for the population phase: a torch with a block below stands; otherwise it
 * attaches to the first non-air neighbor in the order west (x > 0), east (x < 15), north (z > 0), south (z < 15),
 * facing away from it; otherwise it stays as written. Looking only inside the chunk, as upstream's does
 * (docs/original-spec/03-terrain/post-processing.md, docs/new-spec/03-terrain/index.md "fixTorches").
 *
 * <p>
 * 1.7.10 torch metadata: 1 on a west block (facing east), 2 on an east block, 3 on a north block, 4 on a south
 * block, 5 standing (D19). Side: both (plain Java).
 */
public final class TorchRule {

    /** Metadata of a standing torch. */
    public static final int STANDING = 5;
    /** Metadata of a torch on the block to its west. */
    public static final int ON_WEST = 1;
    /** Metadata of a torch on the block to its east. */
    public static final int ON_EAST = 2;
    /** Metadata of a torch on the block to its north. */
    public static final int ON_NORTH = 3;
    /** Metadata of a torch on the block to its south. */
    public static final int ON_SOUTH = 4;
    /** No attachment found: keep the torch as written. */
    public static final int KEEP = -1;

    private TorchRule() {}

    /**
     * Returns the metadata a torch at a position takes.
     *
     * @param chunk the chunk's blocks
     * @param x     local X
     * @param y     block Y
     * @param z     local Z
     * @return the metadata, or {@link #KEEP}
     */
    public static int metaFor(ChunkBuffer chunk, int x, int y, int z) {
        if (y > WorldLimits.MIN_Y && !chunk.get(x, y - 1, z)
            .isAir()) {
            return STANDING;
        }
        if (x > 0 && !chunk.get(x - 1, y, z)
            .isAir()) {
            return ON_WEST;
        }
        if (x < 15 && !chunk.get(x + 1, y, z)
            .isAir()) {
            return ON_EAST;
        }
        if (z > 0 && !chunk.get(x, y, z - 1)
            .isAir()) {
            return ON_NORTH;
        }
        if (z < 15 && !chunk.get(x, y, z + 1)
            .isAir()) {
            return ON_SOUTH;
        }
        return KEEP;
    }
}
