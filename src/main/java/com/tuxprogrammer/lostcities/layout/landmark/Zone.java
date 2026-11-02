package com.tuxprogrammer.lostcities.layout.landmark;

/**
 * The zone a chunk lies in relative to the landmarks around it, weakest first; where zones of several landmarks
 * overlap, the strongest wins (landmark-zones.md § Zones).
 *
 * <p>
 * Side: both.
 */
public enum Zone {

    /** No landmark near enough to matter. */
    NONE,
    /** Villages only: the second transition ring (at most 4 floors, building chance halved). */
    TRANSITION_2,
    /** Villages only: the first transition ring, next to the edge ring (at most 2 floors, building chance halved). */
    TRANSITION_1,
    /**
     * One of a core's eight-neighbors under {@code BUILD_AROUND}: the city's margin. Never city: the city builds inside
     * it, and the blend slopes this natural ground from the city's edge down or up to the core's, crossed only by a
     * village's lane (0006 §1, alpha review AR-10).
     */
    EDGE_RING,
    /** One of a core's eight-neighbors under the {@code AVOID} treatment: never city, as upstream's avoidance. */
    AVOID_RING,
    /** A chunk the landmark's footprint, grown by {@code landmarkMargin}, touches; never city. */
    CORE;

    /** Returns whether this zone is a transition ring. */
    public boolean isTransition() {
        return this == TRANSITION_1 || this == TRANSITION_2;
    }

    /** Returns whether a chunk in this zone is never city: a core, the margin around it, or an avoid ring. */
    public boolean forcesNonCity() {
        return this == CORE || this == EDGE_RING || this == AVOID_RING;
    }

    /**
     * Returns whether a village lane may not cross a chunk in this zone: a core (the lane's path in a core is drawn
     * from the gate, not routed) or an avoid ring. The margin carries lanes down its slope.
     */
    public boolean barsLanes() {
        return this == CORE || this == AVOID_RING;
    }

    /**
     * Returns the stronger of two zones.
     *
     * @param a one zone
     * @param b the other
     * @return the one with the higher ordinal
     */
    public static Zone strongest(Zone a, Zone b) {
        return a.ordinal() >= b.ordinal() ? a : b;
    }
}
