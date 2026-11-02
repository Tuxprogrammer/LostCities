package com.tuxprogrammer.lostcities.api;

/**
 * The railway piece in a chunk: upstream's thirteen values, with whether each is a station and whether it is at the
 * surface (docs/original-spec/01-core/api.md, Value types). Railways generate from M06 on; until then every chunk
 * answers {@link #NONE}.
 *
 * <p>
 * Side: both.
 */
public enum RailChunkType {

    /** No railway. */
    NONE(false, false),
    /** A station at the surface. */
    STATION_SURFACE(true, true),
    /** A station underground. */
    STATION_UNDERGROUND(true, false),
    /** A station's extension at the surface. */
    STATION_EXTENSION_SURFACE(true, true),
    /** A station's extension underground. */
    STATION_EXTENSION_UNDERGROUND(true, false),
    /** Track going down two levels from the surface. */
    GOING_DOWN_TWO_FROM_SURFACE(false, true),
    /** Track going down one level from the surface. */
    GOING_DOWN_ONE_FROM_SURFACE(false, true),
    /** Track going further down. */
    GOING_DOWN_FURTHER(false, false),
    /** Straight track along X. */
    HORIZONTAL(false, false),
    /** A three-way split. */
    THREE_SPLIT(false, false),
    /** Straight track along Z. */
    VERTICAL(false, false),
    /** A double bend. */
    DOUBLE_BEND(false, false),
    /** The end of a line. */
    RAILS_END_HERE(false, false);

    private final boolean station;
    private final boolean surface;

    RailChunkType(boolean station, boolean surface) {
        this.station = station;
        this.surface = surface;
    }

    /** Returns whether the piece is a station or a station's extension. */
    public boolean isStation() {
        return station;
    }

    /** Returns whether the piece is at the surface. */
    public boolean isSurface() {
        return surface;
    }
}
