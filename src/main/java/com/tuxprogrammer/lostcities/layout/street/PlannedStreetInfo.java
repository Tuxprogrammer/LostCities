package com.tuxprogrammer.lostcities.layout.street;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/street/PlannedStreetInfo.java

/**
 * The raw street field at one chunk: its road class, whether the road continues into each neighbor (both chunks have a
 * raw road of any class), its block and its tertiary segment. Raw means not yet clipped to the city
 * ({@link EffectiveStreetResolver}).
 *
 * <p>
 * Spec: docs/original-spec/04-city-layout/street-planner.md#raw-road-type-of-a-chunk-getroadtype. Side: both.
 */
public final class PlannedStreetInfo {

    private final PlannedRoadType roadType;
    private final boolean north;
    private final boolean south;
    private final boolean west;
    private final boolean east;
    private final BlockLayout block;
    private final TertiaryRoadSegment tertiarySegment;

    PlannedStreetInfo(PlannedRoadType roadType, boolean north, boolean south, boolean west, boolean east,
        BlockLayout block, TertiaryRoadSegment tertiarySegment) {
        this.roadType = roadType;
        this.north = north;
        this.south = south;
        this.west = west;
        this.east = east;
        this.block = block;
        this.tertiarySegment = tertiarySegment;
    }

    /** Returns the raw road class. */
    public PlannedRoadType roadType() {
        return roadType;
    }

    /** Returns whether there is a raw road here. */
    public boolean isRoad() {
        return roadType != PlannedRoadType.NONE;
    }

    /**
     * Returns whether the road continues into the neighbor in a direction.
     *
     * @param direction the direction
     * @return whether this chunk and that neighbor both have a raw road
     */
    public boolean connects(RoadDirection direction) {
        switch (direction) {
            case NORTH:
                return north;
            case SOUTH:
                return south;
            case WEST:
                return west;
            default:
                return east;
        }
    }

    /** Returns the block this chunk lies in. */
    public BlockLayout block() {
        return block;
    }

    /**
     * Returns the tertiary segment of this chunk's cell, or {@code null}. It is set whenever the cell has one, even if
     * this chunk is not on it, as upstream does.
     */
    public TertiaryRoadSegment tertiarySegment() {
        return tertiarySegment;
    }
}
