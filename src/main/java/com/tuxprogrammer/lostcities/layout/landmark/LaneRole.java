package com.tuxprogrammer.lostcities.layout.landmark;

/**
 * The part a chunk plays in sewing a landmark into a city (0006 §3, landmark-zones.md § Roads around a core), weakest
 * first; where roles of several landmarks overlap, the strongest wins.
 *
 * <p>
 * Side: both.
 */
public enum LaneRole {

    /** None. */
    NONE,
    /**
     * A non-city chunk on the surface lane between a city and a village outside it (within
     * {@code villageLinkDistance}):
     * the terrain phase writes a 3-wide surface path along its connections, nothing else.
     */
    LINK,
    /** A city chunk on a village lane: a road forced to the tertiary class, no building. */
    LANE,
    /**
     * The chunk just outside the core in front of a village gate. In a city it is a tertiary road like a lane chunk;
     * from it the surface path runs down the soft-edge strip and across the core to the gate.
     */
    GATE,
    /** A city chunk on a primary road's detour around a core: a primary road, no building. */
    DETOUR;

    /**
     * Returns the stronger of two roles.
     *
     * @param a one role
     * @param b the other
     * @return the one with the higher ordinal
     */
    public static LaneRole strongest(LaneRole a, LaneRole b) {
        return a.ordinal() >= b.ordinal() ? a : b;
    }

    /** Returns whether a city chunk with this role is forced to be a road. */
    public boolean forcesRoad() {
        return this == LANE || this == GATE || this == DETOUR;
    }
}
