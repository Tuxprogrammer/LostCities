package com.tuxprogrammer.lostcities.api;

/**
 * The planned road through a city chunk, from the hierarchical street planner (docs/api.md;
 * docs/original-spec/04-city-layout/).
 *
 * <p>
 * Side: both.
 */
public enum LostRoadType {

    /** No planned road. */
    NONE,
    /** A tertiary road. */
    TERTIARY,
    /** A secondary road. */
    SECONDARY,
    /** A primary road. */
    PRIMARY
}
