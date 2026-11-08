package com.tuxprogrammer.lostcities.layout;

/**
 * The floor heights of one building (decision log D-floor-heights-1): how many blocks each of its floors and cellars
 * rises, slab to slab. Every building takes the world's floor pitch (design record 0014) unless its JSON sets
 * {@code floorHeight} (every floor above the ground floor, and every cellar) or {@code groundFloorHeight} (the ground
 * floor only, defaulting to {@code floorHeight}). A building whose ground storey is several parts tall, such as the
 * shopping mall's atrium, sets {@code groundFloors}: that many floors from the ground up take {@code groundFloorHeight}
 * (decision log D-mall-1).
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

    /** Blocks each ground floor rises. */
    public final int ground;

    /** Blocks every floor above the ground floors rises. */
    public final int upper;

    /** Blocks every cellar rises. */
    public final int cellar;

    /** How many floors, from floor 0 up, rise {@link #ground}; at least 1, and 1 whenever {@code ground == upper}. */
    public final int groundFloors;

    private FloorHeights(int ground, int upper, int cellar, int groundFloors) {
        this.ground = ground;
        this.upper = upper;
        this.cellar = cellar;
        // With the two heights equal the count changes nothing, so it is kept at 1 for equals.
        this.groundFloors = ground == upper ? 1 : Math.max(1, groundFloors);
    }

    /**
     * Returns the heights of a building that sets none: every floor and cellar at the world's pitch.
     *
     * @param pitch the world's floor pitch
     * @return the heights
     */
    public static FloorHeights uniform(int pitch) {
        return new FloorHeights(pitch, pitch, pitch, 1);
    }

    /**
     * Returns a building's heights with one ground floor.
     *
     * @param pitch             the world's floor pitch, for whatever the building does not set
     * @param floorHeight       the building's {@code floorHeight}, or a value below 1 when unset
     * @param groundFloorHeight the building's {@code groundFloorHeight}, or a value below 1 when unset
     * @return the heights
     */
    public static FloorHeights of(int pitch, int floorHeight, int groundFloorHeight) {
        return of(pitch, floorHeight, groundFloorHeight, 1);
    }

    /**
     * Returns a building's heights.
     *
     * @param pitch             the world's floor pitch, for whatever the building does not set
     * @param floorHeight       the building's {@code floorHeight}, or a value below 1 when unset
     * @param groundFloorHeight the building's {@code groundFloorHeight}, or a value below 1 when unset
     * @param groundFloors      the building's {@code groundFloors}: how many floors take {@code groundFloorHeight};
     *                          a value below 1 means 1
     * @return the heights
     */
    public static FloorHeights of(int pitch, int floorHeight, int groundFloorHeight, int groundFloors) {
        int upper = floorHeight > 0 ? floorHeight : pitch;
        int ground = groundFloorHeight > 0 ? groundFloorHeight : upper;
        return new FloorHeights(ground, upper, upper, groundFloors);
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
        return f < 0 ? cellar : f < groundFloors ? ground : upper;
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
        if (f <= groundFloors) {
            return f * ground;
        }
        return groundFloors * ground + (f - groundFloors) * upper;
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
        int groundTop = groundFloors * ground;
        if (dy < groundTop) {
            return dy / ground;
        }
        return groundFloors + (dy - groundTop) / upper;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FloorHeights)) {
            return false;
        }
        FloorHeights h = (FloorHeights) o;
        return ground == h.ground && upper == h.upper && cellar == h.cellar && groundFloors == h.groundFloors;
    }

    @Override
    public int hashCode() {
        return ((ground * 31 + upper) * 31 + cellar) * 31 + groundFloors;
    }

    @Override
    public String toString() {
        if (isUniform()) {
            return Integer.toString(upper);
        }
        return (groundFloors == 1 ? "" : groundFloors + "x") + ground + "/" + upper;
    }
}
