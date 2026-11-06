package com.tuxprogrammer.lostcities.layout.link;

/**
 * The track and tunnel of a coded link-network rail chunk (0015 §3): one track along the chunk's middle row or column
 * (block 7) from each side it opens on to the center, inside a tunnel seven blocks wide (blocks 4 to 10 across, walls
 * at 3 and 11), as the {@code rails_horizontal} part's single-track interior. A station part's track is at row 7 too,
 * so lines run on into stations.
 *
 * <p>
 * Pure: answers per column from the chunk's {@link LinkRails} side bits. Side: both.
 */
public final class LinkRailGeometry {

    /** The row (along X) or column (along Z) of the track. */
    public static final int TRACK = 7;
    /** The tunnel's first and last interior row across. */
    static final int INSIDE_LOW = 4;
    static final int INSIDE_HIGH = 10;
    /** Vanilla rail shapes (block metadata). */
    public static final int NORTH_SOUTH = 0;
    public static final int EAST_WEST = 1;
    public static final int SOUTH_EAST = 6;
    public static final int SOUTH_WEST = 7;
    public static final int NORTH_WEST = 8;
    public static final int NORTH_EAST = 9;
    /** Column roles. */
    public static final int NONE = 0;
    public static final int INSIDE = 1;
    public static final int WALL = 2;

    private LinkRailGeometry() {}

    /**
     * Returns whether a column lies inside the tunnel: the center square, or an arm toward a side it opens on.
     *
     * @param sides the chunk's side bits
     * @param x     local X, 0 to 15
     * @param z     local Z, 0 to 15
     * @return whether it is inside
     */
    static boolean inside(int sides, int x, int z) {
        if (x < 0 || x > 15 || z < 0 || z > 15) {
            return false;
        }
        boolean acrossX = z >= INSIDE_LOW && z <= INSIDE_HIGH;
        boolean acrossZ = x >= INSIDE_LOW && x <= INSIDE_HIGH;
        if (acrossX && acrossZ) {
            return true;
        }
        return acrossX
            && (x < INSIDE_LOW && (sides & LinkRails.WEST) != 0 || x > INSIDE_HIGH && (sides & LinkRails.EAST) != 0)
            || acrossZ && (z < INSIDE_LOW && (sides & LinkRails.NORTH) != 0
                || z > INSIDE_HIGH && (sides & LinkRails.SOUTH) != 0);
    }

    /**
     * Returns a column's role: {@link #INSIDE}, {@link #WALL} (outside, next to an inside column, diagonals included)
     * or {@link #NONE}.
     *
     * @param sides the chunk's side bits
     * @param x     local X
     * @param z     local Z
     * @return the role
     */
    public static int role(int sides, int x, int z) {
        if (inside(sides, x, z)) {
            return INSIDE;
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (inside(sides, x + dx, z + dz)) {
                    return WALL;
                }
            }
        }
        return NONE;
    }

    /**
     * Returns whether a column runs along Z: it lies on a north or south arm, outside the center square.
     *
     * @param x local X
     * @param z local Z
     * @return whether the part's cross-section is turned to run along Z here
     */
    public static boolean alongZ(int x, int z) {
        return (z < INSIDE_LOW || z > INSIDE_HIGH) && x >= INSIDE_LOW - 1 && x <= INSIDE_HIGH + 1;
    }

    /**
     * Returns the rail shape at a column, or -1 where no track lies. Arms are straight. The center of a T is a switch
     * (D356): the curve vanilla's own rail picks for those three neighbors when unpowered (south-east before
     * south-west before north-east before north-west, {@code BlockRailBase.Rail}), so the {@link #lever} beside it
     * flips it to the other curve exactly as a player-built T switch does. With the curve every arm has a way out and
     * a way in (a cart meeting a curve from its outside is turned onto one of its ends), and the lever reaches every
     * pair. A crossing keeps east-west straight through (a vanilla crossing cannot carry both lines); a straight or
     * bend joins its two sides.
     *
     * @param sides the chunk's side bits
     * @param x     local X
     * @param z     local Z
     * @return the shape, or -1
     */
    public static int shape(int sides, int x, int z) {
        if (z == TRACK && x != TRACK) {
            return (x < TRACK ? sides & LinkRails.WEST : sides & LinkRails.EAST) != 0 ? EAST_WEST : -1;
        }
        if (x == TRACK && z != TRACK) {
            return (z < TRACK ? sides & LinkRails.NORTH : sides & LinkRails.SOUTH) != 0 ? NORTH_SOUTH : -1;
        }
        if (x != TRACK) {
            return -1;
        }
        boolean n = (sides & LinkRails.NORTH) != 0;
        boolean e = (sides & LinkRails.EAST) != 0;
        boolean s = (sides & LinkRails.SOUTH) != 0;
        boolean w = (sides & LinkRails.WEST) != 0;
        if (Integer.bitCount(sides & LinkRails.SIDES) == 3) {
            // Vanilla's unpowered choice for a T: the last match in NW, NE, SW, SE order wins.
            return s && e ? SOUTH_EAST : s && w ? SOUTH_WEST : NORTH_EAST;
        }
        if (e && w || !n && !s) {
            return EAST_WEST;
        }
        if (n && s || !e && !w) {
            return NORTH_SOUTH;
        }
        if (n) {
            return e ? NORTH_EAST : NORTH_WEST;
        }
        return e ? SOUTH_EAST : SOUTH_WEST;
    }

    /**
     * Returns whether a track column carries a powered rail: the middle of each arm (block 3 or 12 along it).
     *
     * @param sides the chunk's side bits
     * @param x     local X
     * @param z     local Z
     * @return whether it is powered
     */
    /**
     * Returns where a T's switch lever stands: on the floor in the column beside the center, on the side no arm
     * leaves by (inside the tunnel's center square, off the track); {@code null} for any other chunk.
     *
     * @param sides the chunk's side bits
     * @return {local X, local Z}, or {@code null}
     */
    public static int[] lever(int sides) {
        if (Integer.bitCount(sides & LinkRails.SIDES) != 3) {
            return null;
        }
        int free = ~sides & LinkRails.SIDES;
        return free == LinkRails.NORTH ? new int[] { TRACK, TRACK - 1 }
            : free == LinkRails.SOUTH ? new int[] { TRACK, TRACK + 1 }
                : free == LinkRails.WEST ? new int[] { TRACK - 1, TRACK } : new int[] { TRACK + 1, TRACK };
    }

    public static boolean powered(int sides, int x, int z) {
        int s = shape(sides, x, z);
        if (s < 0) {
            return false;
        }
        return z == TRACK && (x == 3 || x == 12) || x == TRACK && (z == 3 || z == 12);
    }
}
