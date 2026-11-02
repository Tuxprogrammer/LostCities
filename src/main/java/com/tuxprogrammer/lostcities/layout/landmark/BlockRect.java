package com.tuxprogrammer.lostcities.layout.landmark;

/**
 * An axis-aligned rectangle of block columns, inclusive on both ends: one piece of a landmark's footprint.
 *
 * <p>
 * Spec: docs/design/0006-build-around-other-worldgen.md §1. Side: both.
 */
public final class BlockRect {

    private final int minX;
    private final int minZ;
    private final int maxX;
    private final int maxZ;

    /**
     * Creates a rectangle; the corners may be given in any order.
     *
     * @param x1 one corner's block X
     * @param z1 one corner's block Z
     * @param x2 the opposite corner's block X
     * @param z2 the opposite corner's block Z
     */
    public BlockRect(int x1, int z1, int x2, int z2) {
        this.minX = Math.min(x1, x2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxZ = Math.max(z1, z2);
    }

    /** Returns the smallest block X. */
    public int minX() {
        return minX;
    }

    /** Returns the smallest block Z. */
    public int minZ() {
        return minZ;
    }

    /** Returns the largest block X. */
    public int maxX() {
        return maxX;
    }

    /** Returns the largest block Z. */
    public int maxZ() {
        return maxZ;
    }

    /**
     * Returns this rectangle grown on every side.
     *
     * @param blocks how many blocks to grow by, zero or more
     * @return the grown rectangle
     */
    public BlockRect grow(int blocks) {
        return new BlockRect(minX - blocks, minZ - blocks, maxX + blocks, maxZ + blocks);
    }

    /** Returns the smallest chunk X this rectangle touches. */
    public int minChunkX() {
        return minX >> 4;
    }

    /** Returns the smallest chunk Z this rectangle touches. */
    public int minChunkZ() {
        return minZ >> 4;
    }

    /** Returns the largest chunk X this rectangle touches. */
    public int maxChunkX() {
        return maxX >> 4;
    }

    /** Returns the largest chunk Z this rectangle touches. */
    public int maxChunkZ() {
        return maxZ >> 4;
    }

    /**
     * Returns the Chebyshev distance, in chunks, from a chunk to the chunks this rectangle touches: 0 when it touches
     * the chunk, 1 for the eight neighbors of those chunks, and so on.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the distance in chunks
     */
    public int chunkDistance(int chunkX, int chunkZ) {
        int dx = Math.max(0, Math.max(minChunkX() - chunkX, chunkX - maxChunkX()));
        int dz = Math.max(0, Math.max(minChunkZ() - chunkZ, chunkZ - maxChunkZ()));
        return Math.max(dx, dz);
    }

    /**
     * Returns whether a block column lies inside.
     *
     * @param x block X
     * @param z block Z
     * @return whether the column is inside
     */
    public boolean contains(int x, int z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof BlockRect)) {
            return false;
        }
        BlockRect r = (BlockRect) other;
        return minX == r.minX && minZ == r.minZ && maxX == r.maxX && maxZ == r.maxZ;
    }

    @Override
    public int hashCode() {
        return ((minX * 31 + minZ) * 31 + maxX) * 31 + maxZ;
    }

    @Override
    public String toString() {
        return "[" + minX + "," + minZ + " .. " + maxX + "," + maxZ + "]";
    }
}
