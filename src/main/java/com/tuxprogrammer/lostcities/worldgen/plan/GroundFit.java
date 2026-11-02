package com.tuxprogrammer.lostcities.worldgen.plan;

import java.util.Arrays;

/**
 * How the ground under and around a scattered structure is set into RWG's terrain (design record 0009, alpha review
 * AR-10 to AR-12): the structure's pads, the columns its parts stand on, in block coordinates; the Y of the ground the
 * structure is set into; and whether the ground there is leveled both ways or only cut down.
 *
 * <p>
 * Every column within {@link #WALKWAY} blocks of a pad takes the ground Y exactly; beyond it, the change fades out
 * with a smooth step, relative to the column's own surface, so where the fade ends the ground is RWG's untouched and
 * nothing joins it with a cliff. The fade is {@link #RADIUS} blocks long, longer where the column's surface lies far
 * from the ground Y ({@link #radiusFor}, up to {@link #MAX_RADIUS}): the fade steepens the land by the difference times
 * its weight's slope, so a ravine's rim or a hill beside the structure is met by a longer, gentler slope. A column's
 * change depends only on its own surface and its distance to the pads, so each chunk applies its share alone and
 * neighbors always meet.
 *
 * <p>
 * Immutable. Side: both (plain Java).
 */
public final class GroundFit {

    /** How the ground is moved toward the structure's ground Y. */
    public enum Mode {
        /** Higher ground is cut down and lower ground filled up: a structure that sits on the ground. */
        LEVEL,
        /**
         * Higher ground is cut down; lower ground stays, because the structure stands on its own supports
         * ({@code repeatslice}: the gas station and restaurant beside an elevated highway).
         */
        CUT
    }

    /** Blocks around a pad that are leveled exactly: a walkway around the structure. */
    public static final int WALKWAY = 1;
    /** Blocks beyond the walkway over which the change fades to nothing, at the least. */
    public static final int RADIUS = 12;
    /** The longest fade, in blocks beyond the walkway. */
    public static final int MAX_RADIUS = 28;
    /**
     * Fade blocks per block of difference between a column's surface and the ground Y: with the smooth step's
     * steepest slope of 1.5 over the fade, the fade then steepens the land by at most about one block a column.
     */
    static final double RADIUS_PER_BLOCK = 1.5;
    /** Chunks beyond a pad's chunk that the fade can reach: {@code ceil((WALKWAY + MAX_RADIUS + 1) / 16)}. */
    public static final int REACH_CHUNKS = 2;

    private final int[] rects;
    private final int groundY;
    private final Mode mode;

    /**
     * Describes a fit.
     *
     * @param rects   the pads, four ints each: min X, min Z, max X, max Z, inclusive block coordinates
     * @param groundY the Y of the top ground block under the structure
     * @param mode    level or cut only
     */
    public GroundFit(int[] rects, int groundY, Mode mode) {
        if (rects.length % 4 != 0 || rects.length == 0) {
            throw new IllegalArgumentException("a ground fit has whole pads, got " + rects.length + " values");
        }
        this.rects = rects.clone();
        this.groundY = groundY;
        this.mode = mode;
    }

    /** Returns the Y of the top ground block under the structure. */
    public int groundY() {
        return groundY;
    }

    /** Returns the mode. */
    public Mode mode() {
        return mode;
    }

    /** Returns the number of pads. */
    public int pads() {
        return rects.length / 4;
    }

    /**
     * Returns the distance in blocks from a column's center to the nearest pad: 0 on a pad.
     *
     * @param blockX block X
     * @param blockZ block Z
     * @return the Euclidean distance between block centers
     */
    public double distance(int blockX, int blockZ) {
        double best = Double.MAX_VALUE;
        for (int i = 0; i < rects.length; i += 4) {
            int dx = Math.max(0, Math.max(rects[i] - blockX, blockX - rects[i + 2]));
            int dz = Math.max(0, Math.max(rects[i + 1] - blockZ, blockZ - rects[i + 3]));
            best = Math.min(best, Math.sqrt((double) dx * dx + (double) dz * dz));
        }
        return best;
    }

    /**
     * Returns how much of the move toward the ground Y a column at a distance takes over the shortest fade: 1 on a pad
     * and its walkway, a smooth step down to 0 at {@link #WALKWAY} + {@link #RADIUS} + 1 blocks.
     *
     * @param distance the distance to the nearest pad, in blocks
     * @return 0 to 1
     */
    public static double weight(double distance) {
        return weight(distance, RADIUS);
    }

    /**
     * Returns the length of the fade for a column: {@link #RADIUS}, or {@link #RADIUS_PER_BLOCK} blocks per block its
     * surface lies off the ground Y, at most {@link #MAX_RADIUS}.
     *
     * @param surface the column's top solid Y
     * @return blocks beyond the walkway
     */
    public double radiusFor(int surface) {
        return Math.max(RADIUS, Math.min(MAX_RADIUS, RADIUS_PER_BLOCK * Math.abs(groundY - surface)));
    }

    /**
     * Returns how much of the move toward the ground Y a column takes, with its surface's fade.
     *
     * @param surface the column's top solid Y
     * @param blockX  block X
     * @param blockZ  block Z
     * @return 0 to 1
     */
    public double weightAt(int surface, int blockX, int blockZ) {
        return weight(distance(blockX, blockZ), radiusFor(surface));
    }

    /**
     * Returns the smooth step of a fade: 1 within the walkway, 0 at {@link #WALKWAY} + {@code radius} + 1 blocks.
     *
     * @param distance the distance to the nearest pad, in blocks
     * @param radius   the fade's length beyond the walkway, in blocks
     * @return 0 to 1
     */
    public static double weight(double distance, double radius) {
        if (distance <= WALKWAY) {
            return 1.0;
        }
        double t = (distance - WALKWAY) / (radius + 1);
        if (t >= 1.0) {
            return 0.0;
        }
        return 1.0 - t * t * (3.0 - 2.0 * t);
    }

    /**
     * Returns the surface Y a column takes, from its own surface.
     *
     * @param surface the column's top solid Y
     * @param blockX  block X
     * @param blockZ  block Z
     * @return the new top Y; {@code surface} when nothing moves
     */
    public int target(int surface, int blockX, int blockZ) {
        double w = weightAt(surface, blockX, blockZ);
        if (w <= 0.0) {
            return surface;
        }
        int target = (int) Math.round(surface + (groundY - surface) * w);
        if (mode == Mode.CUT && target > surface) {
            return surface;
        }
        return target;
    }

    /**
     * Returns whether a column lies past every fade: farther than {@link #WALKWAY} + {@link #MAX_RADIUS} + 1 blocks
     * from every pad.
     *
     * @param blockX block X
     * @param blockZ block Z
     * @return whether the fit can never move it
     */
    public boolean beyond(int blockX, int blockZ) {
        return distance(blockX, blockZ) >= WALKWAY + MAX_RADIUS + 1;
    }

    /**
     * Returns whether the fit can change a column of a chunk.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return whether some pad lies within reach of the chunk
     */
    public boolean reaches(int chunkX, int chunkZ) {
        int reach = WALKWAY + MAX_RADIUS + 1;
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        for (int i = 0; i < rects.length; i += 4) {
            if (rects[i] - reach <= minX + 15 && rects[i + 2] + reach >= minX
                && rects[i + 1] - reach <= minZ + 15
                && rects[i + 3] + reach >= minZ) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof GroundFit)) {
            return false;
        }
        GroundFit that = (GroundFit) other;
        return groundY == that.groundY && mode == that.mode && Arrays.equals(rects, that.rects);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(rects) * 31 + groundY * 7 + mode.ordinal();
    }

    @Override
    public String toString() {
        return "GroundFit[" + mode + " y=" + groundY + " pads=" + Arrays.toString(rects) + "]";
    }
}
