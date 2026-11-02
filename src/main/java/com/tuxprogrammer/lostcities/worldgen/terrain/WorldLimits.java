package com.tuxprogrammer.lostcities.worldgen.terrain;

/**
 * The 1.7.10 world limits and the fixed numbers of upstream's terrain code, as named constants. Upstream hard-codes
 * 0, 1, 255 and 256 in loops written for a Y −64 to 319 world (TERR-24); here every such bound is one of these
 * (docs/new-spec/03-terrain/height-mapping.md, "The rule" item 4).
 *
 * <p>
 * Side: both (plain Java).
 */
public final class WorldLimits {

    /** The lowest block Y of a 1.7.10 world. */
    public static final int MIN_Y = 0;

    /** The highest block Y of a 1.7.10 world. */
    public static final int MAX_Y = 255;

    /** The build height, exclusive: upstream's {@code getMaxBuildHeight()} on 1.7.10. */
    public static final int BUILD_HEIGHT = 256;

    /** Blocks per city level and per building floor (upstream {@code FLOORHEIGHT}). */
    public static final int FLOOR_HEIGHT = 6;

    /** RWG's sea level on both versions: water fills {@code y < 63}, so the top water block is Y 62. */
    public static final int RWG_SEA_LEVEL = 63;

    /** The lowest Y a building's bottom may reach: upstream's {@code minBuild + 2}. */
    public static final int BUILDING_BOTTOM_MIN = MIN_Y + 2;

    /** Exclusive cap of a building's top floor base: upstream's {@code maxBuild − 8}, Y 248 here. */
    public static final int BUILDING_TOP_CAP = BUILD_HEIGHT - 8;

    private WorldLimits() {}

    /**
     * Clamps a Y into the world.
     *
     * @param y a block Y
     * @return {@code y} limited to {@link #MIN_Y} … {@link #MAX_Y}
     */
    public static int clampY(int y) {
        return Math.max(MIN_Y, Math.min(MAX_Y, y));
    }
}
