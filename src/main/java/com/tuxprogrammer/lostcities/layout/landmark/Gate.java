package com.tuxprogrammer.lostcities.layout.landmark;

import com.tuxprogrammer.lostcities.layout.street.RoadDirection;

/**
 * A village gate: the end of a village path that faces the outside, at a block column on the footprint, with the
 * direction the path leaves in. Only villages have gates (0006 §1; coexistence.md § Sewing a village into a city, item
 * 4).
 *
 * <p>
 * Side: both.
 */
public final class Gate {

    private final int x;
    private final int z;
    private final RoadDirection facing;

    /**
     * Creates a gate.
     *
     * @param x      the path end's block X
     * @param z      the path end's block Z
     * @param facing the direction the path leaves the village in
     */
    public Gate(int x, int z, RoadDirection facing) {
        this.x = x;
        this.z = z;
        this.facing = facing;
    }

    /** Returns the path end's block X. */
    public int x() {
        return x;
    }

    /** Returns the path end's block Z. */
    public int z() {
        return z;
    }

    /** Returns the direction the path leaves the village in, which is the side of the core it serves. */
    public RoadDirection facing() {
        return facing;
    }

    /** Returns the chunk X of the path end. */
    public int chunkX() {
        return x >> 4;
    }

    /** Returns the chunk Z of the path end. */
    public int chunkZ() {
        return z >> 4;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Gate)) {
            return false;
        }
        Gate g = (Gate) other;
        return x == g.x && z == g.z && facing == g.facing;
    }

    @Override
    public int hashCode() {
        return (x * 31 + z) * 31 + facing.ordinal();
    }

    @Override
    public String toString() {
        return "gate(" + x + "," + z + " " + facing + ")";
    }
}
