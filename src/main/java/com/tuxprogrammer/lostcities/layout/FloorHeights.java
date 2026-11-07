package com.tuxprogrammer.lostcities.layout;

/**
 * The floor heights of one building (decision log D-floor-heights-1): how many blocks each of its floors and cellars
 * rises, slab to slab. Every building takes the world's floor pitch (design record 0014) unless its JSON sets
 * {@code floorHeight} (every floor above the ground floor, and every cellar) or {@code groundFloorHeight} (the ground
 * floor only, defaulting to {@code floorHeight}).
 *
 * <p>
 * Local floor {@code f} counts as in {@link ChunkPlan}: 0 is the ground floor, negative floors are cellars, and floor
 * {@code floors} is the roof part. The ground floor's slab is always at the city ground, so a building meets the street
 * at the same level whatever its heights. With every height equal to the pitch, each method gives what the single
 * pitch gave: {@code offset(f) == f * pitch} and {@code floorOf(dy) == floorDiv(dy, pitch)}.
 *
 * <p>
 * Plain Java, immutable, side: both.
 */
public final class FloorHeights {

    /** Blocks the ground floor rises. */
    public final int ground;

    /** Blocks every floor above the ground floor rises. */
    public final int upper;

    /** Blocks every cellar rises. */
    public final int cellar;

    private FloorHeights(int ground, int upper, int cellar) {
        this.ground = ground;
        this.upper = upper;
        this.cellar = cellar;
    }

    /**
     * Returns the heights of a building that sets none: every floor and cellar at the world's pitch.
     *
     * @param pitch the world's floor pitch
     * @return the heights
     */
    public static FloorHeights uniform(int pitch) {
        return new FloorHeights(pitch, pitch, pitch);
    }

    /**
     * Returns a building's heights.
     *
     * @param pitch             the world's floor pitch, for whatever the building does not set
     * @param floorHeight       the building's {@code floorHeight}, or a value below 1 when unset
     * @param groundFloorHeight the building's {@code groundFloorHeight}, or a value below 1 when unset
     * @return the heights
     */
    public static FloorHeights of(int pitch, int floorHeight, int groundFloorHeight) {
        int upper = floorHeight > 0 ? floorHeight : pitch;
        int ground = groundFloorHeight > 0 ? groundFloorHeight : upper;
        return new FloorHeights(ground, upper, upper);
    }

    /** Whether every floor and cellar has the same height. */
    public boolean isUniform() {
        return ground == upper && upper == cellar;
    }

    /**
     * Returns the blocks local floor {@code f} rises.
     *
     * @param f the local floor: negative for cellars
     * @return its height
     */
    public int height(int f) {
        return f < 0 ? cellar : f == 0 ? ground : upper;
    }

    /**
     * Returns the Y of local floor {@code f}'s slab over the city ground; floor {@code floors} gives the roof part's
     * base, the building's top.
     *
     * @param f the local floor: negative for cellars
     * @return blocks above the city ground, negative below it
     */
    public int offset(int f) {
        if (f <= 0) {
            return f * cellar;
        }
        return ground + (f - 1) * upper;
    }

    /**
     * Returns the floor a block {@code dy} over the city ground stands in: the largest {@code f} with
     * {@code offset(f) <= dy}. It is also the most floors whose slabs fit in {@code dy} blocks.
     *
     * @param dy blocks above the city ground, negative below it
     * @return the local floor
     */
    public int floorOf(int dy) {
        if (dy < 0) {
            return Math.floorDiv(dy, cellar);
        }
        if (dy < ground) {
            return 0;
        }
        return 1 + (dy - ground) / upper;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FloorHeights)) {
            return false;
        }
        FloorHeights h = (FloorHeights) o;
        return ground == h.ground && upper == h.upper && cellar == h.cellar;
    }

    @Override
    public int hashCode() {
        return (ground * 31 + upper) * 31 + cellar;
    }

    @Override
    public String toString() {
        return isUniform() ? Integer.toString(upper) : ground + "/" + upper;
    }
}
