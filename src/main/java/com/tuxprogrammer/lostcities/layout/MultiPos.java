package com.tuxprogrammer.lostcities.layout;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/api/MultiPos.java

/**
 * A chunk's position inside a multibuilding: its offset from the top-left (north-west) chunk and the multibuilding's
 * size, or {@link #SINGLE} for a chunk that is not part of one.
 *
 * <p>
 * Spec: docs/original-spec/04-city-layout/multibuildings.md. Side: both.
 */
public final class MultiPos {

    /** Not part of a multibuilding. */
    public static final MultiPos SINGLE = new MultiPos(-1, -1, 1, 1);

    private final int x;
    private final int z;
    private final int width;
    private final int height;

    /**
     * Creates a position.
     *
     * @param x      the chunk's X offset from the top-left
     * @param z      the chunk's Z offset from the top-left
     * @param width  the multibuilding's size along X, in chunks
     * @param height its size along Z, in chunks
     */
    public MultiPos(int x, int z, int width, int height) {
        this.x = x;
        this.z = z;
        this.width = width;
        this.height = height;
    }

    /** Returns the X offset from the top-left chunk. */
    public int x() {
        return x;
    }

    /** Returns the Z offset from the top-left chunk. */
    public int z() {
        return z;
    }

    /** Returns the size along X, in chunks. */
    public int width() {
        return width;
    }

    /** Returns the size along Z, in chunks. */
    public int height() {
        return height;
    }

    /** Returns whether the chunk is not part of a multibuilding. */
    public boolean isSingle() {
        return x == -1 && z == -1;
    }

    /** Returns whether the chunk is part of a multibuilding. */
    public boolean isMulti() {
        return !isSingle();
    }

    /** Returns whether the chunk is a multibuilding's top-left chunk. */
    public boolean isTopLeft() {
        return x == 0 && z == 0;
    }

    /** Returns whether the chunk is in the multibuilding's last column. */
    public boolean isRightSide() {
        return isMulti() && x == width - 1;
    }

    /** Returns whether the chunk is in the multibuilding's last row. */
    public boolean isBottomSide() {
        return isMulti() && z == height - 1;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MultiPos)) {
            return false;
        }
        MultiPos p = (MultiPos) other;
        return x == p.x && z == p.z && width == p.width && height == p.height;
    }

    @Override
    public int hashCode() {
        return ((x * 31 + z) * 31 + width) * 31 + height;
    }

    @Override
    public String toString() {
        return isSingle() ? "single" : x + "," + z + " of " + width + "x" + height;
    }
}
