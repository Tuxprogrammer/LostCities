package com.tuxprogrammer.lostcities.layout.street;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/street/PlannedRoadType.java

/**
 * The class of a road in the hierarchical street field, weakest first. {@link #PRIMARY} roads take the city style's
 * large street parts and may be paved from the primary-roads pool (decision log D28 rule 3, P03-5); {@link #TERTIARY}
 * roads are short dead-end access roads, and the class village lanes are forced to.
 *
 * <p>
 * Spec: docs/original-spec/04-city-layout/street-planner.md. Side: both.
 */
public enum PlannedRoadType {

    /** No road. */
    NONE,
    /** A dead-end access road inside a cell. */
    TERTIARY,
    /** A road inside a block, ending at the block's primary roads. */
    SECONDARY,
    /** A road on an active primary grid line, running the length of the dimension. */
    PRIMARY;

    /**
     * Returns the stronger of two classes.
     *
     * @param first  one class
     * @param second the other
     * @return the one with the higher ordinal; {@code first} on a tie
     */
    public static PlannedRoadType strongest(PlannedRoadType first, PlannedRoadType second) {
        return first.ordinal() >= second.ordinal() ? first : second;
    }

    /** Returns whether this is a road at all. */
    public boolean isRoad() {
        return this != NONE;
    }
}
