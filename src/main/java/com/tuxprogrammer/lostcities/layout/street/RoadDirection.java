package com.tuxprogrammer.lostcities.layout.street;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/street/RoadDirection.java

/**
 * The four directions of the street field, in upstream's order (the tertiary-road side walk depends on it), with their
 * steps. North is -Z and west is -X, as in Minecraft.
 *
 * <p>
 * Spec: docs/original-spec/04-city-layout/street-planner.md. Side: both.
 */
public enum RoadDirection {

    /** Toward -Z. */
    NORTH(0, -1),
    /** Toward +Z. */
    SOUTH(0, 1),
    /** Toward -X. */
    WEST(-1, 0),
    /** Toward +X. */
    EAST(1, 0);

    private final int stepX;
    private final int stepZ;

    RoadDirection(int stepX, int stepZ) {
        this.stepX = stepX;
        this.stepZ = stepZ;
    }

    /** Returns the X step, in chunks or blocks. */
    public int stepX() {
        return stepX;
    }

    /** Returns the Z step, in chunks or blocks. */
    public int stepZ() {
        return stepZ;
    }

    /** Returns whether this direction runs along Z (north or south). */
    public boolean isNorthSouth() {
        return stepX == 0;
    }

    /** Returns the opposite direction. */
    public RoadDirection opposite() {
        switch (this) {
            case NORTH:
                return SOUTH;
            case SOUTH:
                return NORTH;
            case WEST:
                return EAST;
            default:
                return WEST;
        }
    }
}
