package com.tuxprogrammer.lostcities.layout.landmark;

/**
 * A chunk's place among the landmarks around it: its {@link Zone}, the landmark that put it there, and the lowest
 * meteorite height cap over it. Derived before the characteristics stage and part of the immutable plan
 * (landmark-zones.md § Zones).
 *
 * <p>
 * Side: both.
 */
public final class ZoneInfo {

    /** A chunk no landmark reaches. */
    public static final ZoneInfo NONE = new ZoneInfo(Zone.NONE, null, Integer.MAX_VALUE);

    private final Zone zone;
    private final String landmarkId;
    private final int heightCap;

    /**
     * Creates zone info.
     *
     * @param zone       the strongest zone
     * @param landmarkId the identity of the landmark that gives that zone, or {@code null} for {@link Zone#NONE}
     * @param heightCap  the lowest meteorite height cap over the chunk, block Y, or {@link Integer#MAX_VALUE}
     */
    public ZoneInfo(Zone zone, String landmarkId, int heightCap) {
        this.zone = zone;
        this.landmarkId = landmarkId;
        this.heightCap = heightCap;
    }

    /** Returns the strongest zone. */
    public Zone zone() {
        return zone;
    }

    /** Returns the landmark that gives the zone, or {@code null}. */
    public String landmarkId() {
        return landmarkId;
    }

    /** Returns the highest Y a building roof here may reach, or {@link Integer#MAX_VALUE} when no cap applies. */
    public int heightCap() {
        return heightCap;
    }

    /** Returns whether a meteorite's crater cap reaches the chunk (a crater basin, if the chunk is city). */
    public boolean inCraterBasin() {
        return heightCap != Integer.MAX_VALUE;
    }

    @Override
    public String toString() {
        return zone + (landmarkId == null ? "" : " " + landmarkId) + (inCraterBasin() ? " cap " + heightCap : "");
    }
}
