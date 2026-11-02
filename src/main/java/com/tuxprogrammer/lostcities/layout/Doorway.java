package com.tuxprogrammer.lostcities.layout;

/**
 * What a building floor gets in one side wall: nothing, an open passage into the neighboring building, or a double
 * door to a street or to the roof of a lower neighbor. The decision is {@link LayoutPlanner#doorway}; the blocks
 * (filler frame, door metadata) are the building writer's.
 *
 * <p>
 * Spec: docs/original-spec/04-city-layout/doors-and-connections.md. Side: both.
 */
public enum Doorway {

    /** A plain wall. */
    NONE,
    /** A 2-wide, 2-high opening into the neighboring building's floor at the same level. */
    PASSAGE,
    /** A double door, 2 wide, to a street or a lower neighbor's roof. */
    DOOR
}
