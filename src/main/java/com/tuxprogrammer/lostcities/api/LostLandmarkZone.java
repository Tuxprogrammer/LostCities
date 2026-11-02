package com.tuxprogrammer.lostcities.api;

/**
 * Where a chunk lies relative to another mod's landmark, a village, a monument or another structure the city builds
 * around (docs/api.md; docs/design/0006-build-around-other-worldgen.md §1).
 *
 * <p>
 * Side: both.
 */
public enum LostLandmarkZone {

    /** Away from any landmark. */
    NONE,
    /** The ring around a core: the city's margin, never city. */
    EDGE_RING,
    /** The ring around a landmark the profile avoids. */
    AVOID_RING,
    /** The landmark's own footprint: no city block is written here. */
    CORE
}
