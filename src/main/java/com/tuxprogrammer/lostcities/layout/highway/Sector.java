package com.tuxprogrammer.lostcities.layout.highway;

/**
 * The compass sector of one hub seen from another, by the larger of the chunk deltas; a hub first picks at most one
 * neighbor per sector.
 *
 * <p>
 * Spec: docs/original-spec/06-highways-railways/intercity-planner.md (steps C5, S). Side: both.
 */
public enum Sector {

    /** Toward −Z. */
    NORTH,
    /** Toward +X. */
    EAST,
    /** Toward +Z. */
    SOUTH,
    /** Toward −X. */
    WEST;

    /**
     * Returns the sector of a target from a source: east or west when {@code |dx| >= |dz|}, else south or north.
     *
     * @param dx target X − source X, in chunks
     * @param dz target Z − source Z, in chunks
     * @return the sector
     */
    public static Sector of(long dx, long dz) {
        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx >= 0 ? EAST : WEST;
        }
        return dz >= 0 ? SOUTH : NORTH;
    }
}
