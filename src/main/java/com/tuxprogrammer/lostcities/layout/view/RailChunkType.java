package com.tuxprogrammer.lostcities.layout.view;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/api/RailChunkType.java

/**
 * The type of railway part at a chunk, with whether it is a station and whether it reaches the surface: upstream's
 * types, which the railway layout (M06) produces and the city layout reads through {@link InfrastructureView}.
 *
 * <p>
 * Spec: docs/original-spec/06-highways-railways/ (railway chunk types). Side: both.
 */
public enum RailChunkType {

    /** No railway. */
    NONE(false, false),
    /** A station at the surface. */
    STATION_SURFACE(true, true),
    /** A station underground: no building may stand on it. */
    STATION_UNDERGROUND(true, false),
    /** A station extension at the surface. */
    STATION_EXTENSION_SURFACE(true, true),
    /** A station extension underground. */
    STATION_EXTENSION_UNDERGROUND(true, false),
    /** The ramp two levels down from the surface. */
    GOING_DOWN_TWO_FROM_SURFACE(false, true),
    /** The ramp one level down from the surface: no building front on it. */
    GOING_DOWN_ONE_FROM_SURFACE(false, true),
    /** A ramp further down. */
    GOING_DOWN_FURTHER(false, false),
    /** A straight east-west line. */
    HORIZONTAL(false, false),
    /** A three-way split. */
    THREE_SPLIT(false, false),
    /** A straight north-south line. */
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

    /** Returns whether this is a station or station extension. */
    public boolean isStation() {
        return station;
    }

    /** Returns whether this part reaches the surface. */
    public boolean isSurface() {
        return surface;
    }
}
