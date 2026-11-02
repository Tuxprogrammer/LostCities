package com.tuxprogrammer.lostcities.layout.rail;

/**
 * The railway grid's settings: the profile's {@code railway*} options and the {@code railways} module.
 *
 * <p>
 * Spec: docs/original-spec/06-highways-railways/railway-layout.md ("Numbers"),
 * docs/new-spec/06-highways-railways/index.md (railway rows). Side: both.
 */
public final class RailwaySettings {

    /** Upstream's base railway level: three levels below the ground ({@code Railway.RAILWAY_LEVEL_OFFSET}). */
    public static final int BASE_LEVEL = -3;

    /** Upstream's defaults, with railways on. */
    public static final RailwaySettings DEFAULTS = new RailwaySettings(true, true, true, false, 0, 10, 10);

    private final boolean enabled;
    private final boolean stations;
    private final boolean surfaceStations;
    private final boolean canEnd;
    private final int levelOffset;
    private final int spacingNorthSouth;
    private final int spacingEastWest;

    /**
     * Creates the settings.
     *
     * @param enabled           {@code railwaysEnabled} and the {@code railways} module
     * @param stations          {@code railwayStationsEnabled}
     * @param surfaceStations   {@code railwaySurfaceStationsEnabled}
     * @param canEnd            {@code railwaysCanEnd}
     * @param levelOffset       {@code railwayLevelOffset}, −8 to 2, already raised so the rails stay above Y 5
     * @param spacingNorthSouth {@code railwaySpacingNorthSouth}, even, 4 to 128
     * @param spacingEastWest   {@code railwaySpacingEastWest}, even, 4 to 128
     * @throws IllegalArgumentException for an odd or out-of-range spacing
     */
    public RailwaySettings(boolean enabled, boolean stations, boolean surfaceStations, boolean canEnd,
        int levelOffset, int spacingNorthSouth, int spacingEastWest) {
        if (!even(spacingNorthSouth) || !even(spacingEastWest)) {
            throw new IllegalArgumentException("Railway spacings must be even, 4 to 128");
        }
        this.enabled = enabled;
        this.stations = stations;
        this.surfaceStations = surfaceStations;
        this.canEnd = canEnd;
        this.levelOffset = levelOffset;
        this.spacingNorthSouth = spacingNorthSouth;
        this.spacingEastWest = spacingEastWest;
    }

    private static boolean even(int spacing) {
        return spacing >= 4 && spacing <= 128 && spacing % 2 == 0;
    }

    /** Returns whether railways other than stations generate. */
    public boolean enabled() {
        return enabled;
    }

    /** Returns whether stations generate. */
    public boolean stations() {
        return stations;
    }

    /** Returns whether a station may be at the surface. */
    public boolean surfaceStations() {
        return surfaceStations;
    }

    /** Returns {@code railwaysCanEnd}. */
    public boolean canEnd() {
        return canEnd;
    }

    /** Returns {@code railwayLevelOffset}. */
    public int levelOffset() {
        return levelOffset;
    }

    /** Returns the main railway's city level: {@code −3 + railwayLevelOffset}. */
    public int railwayLevel() {
        return BASE_LEVEL + levelOffset;
    }

    /** Returns {@code railwaySpacingNorthSouth} ({@code sz}). */
    public int spacingNorthSouth() {
        return spacingNorthSouth;
    }

    /** Returns {@code railwaySpacingEastWest} ({@code sx}). */
    public int spacingEastWest() {
        return spacingEastWest;
    }
}
