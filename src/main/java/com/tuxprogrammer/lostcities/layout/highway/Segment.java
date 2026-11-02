package com.tuxprogrammer.lostcities.layout.highway;

/**
 * A straight piece of a route: a row of chunks along {@link Axis#X} or a column along {@link Axis#Z}, from a start
 * chunk to an end chunk, both included. A run's segments follow each other from one of its ends to the other, each
 * starting on the chunk where the one before it ended (a corner).
 *
 * <p>
 * Spec: docs/original-spec/06-highways-railways/intercity-planner.md (step R). Side: both.
 */
public final class Segment {

    private final int startX;
    private final int startZ;
    private final int endX;
    private final int endZ;
    private final Axis axis;

    /**
     * Creates a segment.
     *
     * @param startX its first chunk's X
     * @param startZ its first chunk's Z
     * @param endX   its last chunk's X
     * @param endZ   its last chunk's Z
     * @param axis   its axis; an X segment keeps its Z, a Z segment its X
     * @throws IllegalArgumentException when the chunks do not lie on one row or column of that axis
     */
    public Segment(int startX, int startZ, int endX, int endZ, Axis axis) {
        if (axis == Axis.X ? startZ != endZ : startX != endX) {
            throw new IllegalArgumentException(
                "A " + axis
                    + " segment must keep its other coordinate: "
                    + startX
                    + ","
                    + startZ
                    + " to "
                    + endX
                    + ","
                    + endZ);
        }
        this.startX = startX;
        this.startZ = startZ;
        this.endX = endX;
        this.endZ = endZ;
        this.axis = axis;
    }

    /** Returns the first chunk's X. */
    public int startX() {
        return startX;
    }

    /** Returns the first chunk's Z. */
    public int startZ() {
        return startZ;
    }

    /** Returns the last chunk's X. */
    public int endX() {
        return endX;
    }

    /** Returns the last chunk's Z. */
    public int endZ() {
        return endZ;
    }

    /** Returns the axis. */
    public Axis axis() {
        return axis;
    }

    /** Returns its length in chunks: the steps from the first chunk to the last. */
    public int length() {
        return Math.abs(endX - startX) + Math.abs(endZ - startZ);
    }

    /** Returns +1 when it runs toward +X or +Z, −1 toward −X or −Z, 0 for a single chunk. */
    public int direction() {
        return axis == Axis.X ? Integer.signum(endX - startX) : Integer.signum(endZ - startZ);
    }

    /**
     * Returns whether a chunk lies on the segment (its inclusive bounding box).
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return whether it is on it
     */
    public boolean contains(int chunkX, int chunkZ) {
        return chunkX >= Math.min(startX, endX) && chunkX <= Math.max(startX, endX)
            && chunkZ >= Math.min(startZ, endZ)
            && chunkZ <= Math.max(startZ, endZ);
    }

    /**
     * Returns how many steps from the segment's first chunk a chunk on it lies.
     *
     * @param chunkX chunk X of a chunk on the segment
     * @param chunkZ chunk Z
     * @return the offset, 0 to {@link #length()}
     */
    public int offsetOf(int chunkX, int chunkZ) {
        return Math.abs(chunkX - startX) + Math.abs(chunkZ - startZ);
    }

    /**
     * Returns whether two segments share a chunk along the same axis: the same row or column, overlapping ranges.
     *
     * @param other the other segment
     * @return whether they overlap
     */
    public boolean overlaps(Segment other) {
        if (axis != other.axis) {
            return false;
        }
        if (axis == Axis.X) {
            return startZ == other.startZ && Math.max(Math.min(startX, endX), Math.min(other.startX, other.endX))
                <= Math.min(Math.max(startX, endX), Math.max(other.startX, other.endX));
        }
        return startX == other.startX && Math.max(Math.min(startZ, endZ), Math.min(other.startZ, other.endZ))
            <= Math.min(Math.max(startZ, endZ), Math.max(other.startZ, other.endZ));
    }

    @Override
    public String toString() {
        return axis + "[" + startX + "," + startZ + "->" + endX + "," + endZ + "]";
    }
}
