package com.tuxprogrammer.lostcities.layout;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/Direction.java

import com.tuxprogrammer.lostcities.layout.street.Orientation;

/**
 * A chunk side, in upstream's order ({@code XMIN}, {@code XMAX}, {@code ZMIN}, {@code ZMAX}), which slope and stair
 * decisions depend on. Upstream's {@code getRotation()} (a block transform) is the asset model's; the quarter turns are
 * given here as {@link #quarterTurns()}.
 *
 * <p>
 * Spec: docs/original-spec/04-city-layout/transforms.md, streets-parks-stairs.md. Side: both.
 */
public enum Direction {

    /** The west side (-X). */
    XMIN(-1, 0, 0),
    /** The east side (+X). */
    XMAX(1, 0, 2),
    /** The north side (-Z). */
    ZMIN(0, -1, 1),
    /** The south side (+Z). */
    ZMAX(0, 1, 3);

    private final int stepX;
    private final int stepZ;
    private final int quarterTurns;

    Direction(int stepX, int stepZ, int quarterTurns) {
        this.stepX = stepX;
        this.stepZ = stepZ;
        this.quarterTurns = quarterTurns;
    }

    /** Returns the X step to the neighbor on this side, in chunks. */
    public int stepX() {
        return stepX;
    }

    /** Returns the Z step to the neighbor on this side, in chunks. */
    public int stepZ() {
        return stepZ;
    }

    /**
     * Returns the rotation, in clockwise quarter turns, a part placed facing this side gets: upstream's
     * {@code XMIN → ROTATE_NONE}, {@code XMAX → ROTATE_180}, {@code ZMIN → ROTATE_90}, {@code ZMAX → ROTATE_270}.
     */
    public int quarterTurns() {
        return quarterTurns;
    }

    /** Returns the axis this side's neighbor lies along. */
    public Orientation orientation() {
        return stepX != 0 ? Orientation.X : Orientation.Z;
    }

    /** Returns the opposite side. */
    public Direction opposite() {
        switch (this) {
            case XMIN:
                return XMAX;
            case XMAX:
                return XMIN;
            case ZMIN:
                return ZMAX;
            default:
                return ZMIN;
        }
    }
}
