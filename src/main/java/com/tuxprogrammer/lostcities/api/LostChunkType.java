package com.tuxprogrammer.lostcities.api;

/**
 * What a chunk is, in upstream's terms of the {@code debug} command's "final city content" (docs/api.md;
 * docs/original-spec/01-core/commands.md, {@code debug}).
 *
 * <p>
 * Side: both.
 */
public enum LostChunkType {

    /** Not part of a city: natural terrain (a bridge or highway may still cross it). */
    NONE,
    /** A single building. */
    BUILDING,
    /** One chunk of a multibuilding. */
    MULTI_BUILDING,
    /** A planned road. */
    PLANNED_ROAD,
    /** A street of a predefined city. */
    PREDEFINED_STREET,
    /** A street section, with a fountain or a front where the city style draws one. */
    STREET,
    /** A park. */
    PARK,
    /** An open lot: a park chunk with no park part. */
    OPEN_LOT,
    /** A landmark core: reserved for another mod's structure, which the city builds around. */
    LANDMARK
}
