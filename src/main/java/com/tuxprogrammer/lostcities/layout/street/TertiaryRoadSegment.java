package com.tuxprogrammer.lostcities.layout.street;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/street/TertiaryRoadSegment.java

/**
 * A short dead-end access road: it leaves the boundary road at its origin and covers the chunks at distance
 * {@code 1 .. length} along its direction.
 *
 * <p>
 * Spec: docs/original-spec/04-city-layout/street-planner.md#tertiary-roads. Side: both.
 */
public final class TertiaryRoadSegment {

    private final long id;
    private final int originX;
    private final int originZ;
    private final RoadDirection direction;
    private final int length;

    /**
     * Creates a segment.
     *
     * @param id        the cell's identity hash
     * @param originX   the origin chunk X, on the boundary road
     * @param originZ   the origin chunk Z, on the boundary road
     * @param direction the direction it runs, away from the origin
     * @param length    its length in chunks, not counting the origin
     */
    public TertiaryRoadSegment(long id, int originX, int originZ, RoadDirection direction, int length) {
        this.id = id;
        this.originX = originX;
        this.originZ = originZ;
        this.direction = direction;
        this.length = length;
    }

    /** Returns the cell's identity hash. */
    public long id() {
        return id;
    }

    /** Returns the origin chunk X. */
    public int originX() {
        return originX;
    }

    /** Returns the origin chunk Z. */
    public int originZ() {
        return originZ;
    }

    /** Returns the direction the segment runs. */
    public RoadDirection direction() {
        return direction;
    }

    /** Returns the length in chunks. */
    public int length() {
        return length;
    }

    /**
     * Returns whether the segment covers a chunk: on its line, at distance 1 to {@link #length()} from the origin.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return whether the chunk is part of the segment
     */
    public boolean contains(int chunkX, int chunkZ) {
        int dx = chunkX - originX;
        int dz = chunkZ - originZ;
        int distance = dx * direction.stepX() + dz * direction.stepZ();
        return distance >= 1 && distance <= length && dx * direction.stepZ() == dz * direction.stepX();
    }

    @Override
    public String toString() {
        return id + "," + originX + "," + originZ + "," + direction + "," + length;
    }
}
