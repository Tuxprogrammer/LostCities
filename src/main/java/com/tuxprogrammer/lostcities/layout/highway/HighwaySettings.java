package com.tuxprogrammer.lostcities.layout.highway;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/highway/HighwayPlannerSettings.java

/**
 * The intercity highway planner's settings: the profile's {@code highway*} options and the two railway spacings the
 * planner keeps its hubs and routes off, plus the two switches this port adds (whether highways generate at all, and
 * whether they get supports). Validated as upstream's {@code HighwayPlannerSettings} validates them; a profile that
 * passed {@code ProfileValidator} always passes here.
 *
 * <p>
 * Spec: docs/original-spec/06-highways-railways/intercity-planner.md ("Settings"),
 * docs/new-spec/06-highways-railways/index.md (the planner's settings row). Side: both.
 */
public final class HighwaySettings {

    /** Upstream's defaults, with highways and supports on. */
    public static final HighwaySettings DEFAULTS = builder().build();

    private final boolean enabled;
    private final boolean supports;
    private final int planningCellSize;
    private final int hubSampleSpacing;
    private final float hubMinimumPotential;
    private final int hubSearchRadiusCells;
    private final int minimumHubDistance;
    private final int maximumHubDistance;
    private final int maximumConnectionsPerHub;
    private final int minimumRouteLength;
    private final float routeCityPenalty;
    private final int levelFromCities;
    private final int networkLevel;
    private final int railwaySpacingNorthSouth;
    private final int railwaySpacingEastWest;

    private HighwaySettings(Builder b) {
        require(b.planningCellSize >= 32 && b.planningCellSize <= 512, "highwayPlanningCellSize 32 to 512");
        require(
            b.hubSampleSpacing >= 1 && b.hubSampleSpacing <= b.planningCellSize,
            "highwayHubSampleSpacing 1 to the cell size");
        require(b.hubMinimumPotential >= 0 && b.hubMinimumPotential <= 1, "highwayHubMinimumPotential 0 to 1");
        require(b.hubSearchRadiusCells >= 0 && b.hubSearchRadiusCells <= 8, "highwayHubSearchRadiusCells 0 to 8");
        require(
            b.minimumHubDistance >= 0 && b.minimumHubDistance <= b.maximumHubDistance && b.maximumHubDistance <= 4096,
            "0 <= highwayMinimumHubDistance <= highwayMaximumHubDistance <= 4096");
        require(
            b.maximumConnectionsPerHub >= 1 && b.maximumConnectionsPerHub <= 8,
            "highwayMaximumConnectionsPerHub 1 to 8");
        require(b.minimumRouteLength >= 0 && b.minimumRouteLength <= 4096, "highwayMinimumRouteLength 0 to 4096");
        require(b.routeCityPenalty >= 0 && b.routeCityPenalty <= 1000, "highwayRouteCityPenalty 0 to 1000");
        require(b.levelFromCities >= 0 && b.levelFromCities <= 4, "highwayLevelFromCities 0 to 4");
        require(b.networkLevel >= 0 && b.networkLevel <= 32, "highwayNetworkLevel 0 to 32");
        require(evenSpacing(b.railwaySpacingNorthSouth), "railwaySpacingNorthSouth even, 4 to 128");
        require(evenSpacing(b.railwaySpacingEastWest), "railwaySpacingEastWest even, 4 to 128");
        enabled = b.enabled;
        supports = b.supports;
        planningCellSize = b.planningCellSize;
        hubSampleSpacing = b.hubSampleSpacing;
        hubMinimumPotential = b.hubMinimumPotential;
        hubSearchRadiusCells = b.hubSearchRadiusCells;
        minimumHubDistance = b.minimumHubDistance;
        maximumHubDistance = b.maximumHubDistance;
        maximumConnectionsPerHub = b.maximumConnectionsPerHub;
        minimumRouteLength = b.minimumRouteLength;
        routeCityPenalty = b.routeCityPenalty;
        levelFromCities = b.levelFromCities;
        networkLevel = b.networkLevel;
        railwaySpacingNorthSouth = b.railwaySpacingNorthSouth;
        railwaySpacingEastWest = b.railwaySpacingEastWest;
    }

    private static boolean evenSpacing(int spacing) {
        return spacing >= 4 && spacing <= 128 && spacing % 2 == 0;
    }

    private static void require(boolean condition, String rule) {
        if (!condition) {
            throw new IllegalArgumentException("Invalid highway settings: " + rule);
        }
    }

    /** Returns a builder at upstream's defaults. */
    public static Builder builder() {
        return new Builder();
    }

    /** Returns whether highways generate: the {@code highways} module and the profile's {@code highwaysEnabled}. */
    public boolean enabled() {
        return enabled;
    }

    /** Returns {@code highwaySupports}: whether columns or support parts go under highway parts. */
    public boolean supports() {
        return supports;
    }

    /** Returns {@code highwayPlanningCellSize}, in chunks per side. */
    public int planningCellSize() {
        return planningCellSize;
    }

    /** Returns {@code highwayHubSampleSpacing}, in chunks. */
    public int hubSampleSpacing() {
        return hubSampleSpacing;
    }

    /** Returns {@code highwayHubMinimumPotential}, 0 to 1. */
    public float hubMinimumPotential() {
        return hubMinimumPotential;
    }

    /** Returns {@code highwayHubSearchRadiusCells}. */
    public int hubSearchRadiusCells() {
        return hubSearchRadiusCells;
    }

    /** Returns {@code highwayMinimumHubDistance}, in chunks (Euclidean). */
    public int minimumHubDistance() {
        return minimumHubDistance;
    }

    /** Returns {@code highwayMaximumHubDistance}, in chunks (Euclidean). */
    public int maximumHubDistance() {
        return maximumHubDistance;
    }

    /** Returns {@code highwayMaximumConnectionsPerHub}. */
    public int maximumConnectionsPerHub() {
        return maximumConnectionsPerHub;
    }

    /** Returns {@code highwayMinimumRouteLength}, in chunks (Manhattan). */
    public int minimumRouteLength() {
        return minimumRouteLength;
    }

    /** Returns {@code highwayRouteCityPenalty}. */
    public float routeCityPenalty() {
        return routeCityPenalty;
    }

    /** Returns {@code highwayLevelFromCities}, 0 to 4. */
    public int levelFromCities() {
        return levelFromCities;
    }

    /** Returns {@code highwayNetworkLevel}, clamped at profile load so the parts stay below Y 255 (P06-6). */
    public int networkLevel() {
        return networkLevel;
    }

    /** Returns {@code railwaySpacingNorthSouth}: chunks between east-west railway lines. */
    public int railwaySpacingNorthSouth() {
        return railwaySpacingNorthSouth;
    }

    /** Returns {@code railwaySpacingEastWest}: chunks between north-south railway lines. */
    public int railwaySpacingEastWest() {
        return railwaySpacingEastWest;
    }

    /** Builds {@link HighwaySettings}. */
    public static final class Builder {

        private boolean enabled = true;
        private boolean supports = true;
        private int planningCellSize = 128;
        private int hubSampleSpacing = 16;
        private float hubMinimumPotential = 0.2f;
        private int hubSearchRadiusCells = 2;
        private int minimumHubDistance = 64;
        private int maximumHubDistance = 320;
        private int maximumConnectionsPerHub = 2;
        private int minimumRouteLength = 40;
        private float routeCityPenalty = 1.0f;
        private int levelFromCities = 3;
        private int networkLevel;
        private int railwaySpacingNorthSouth = 10;
        private int railwaySpacingEastWest = 10;

        private Builder() {}

        /** @param on whether highways generate @return this builder */
        public Builder enabled(boolean on) {
            enabled = on;
            return this;
        }

        /** @param on {@code highwaySupports} @return this builder */
        public Builder supports(boolean on) {
            supports = on;
            return this;
        }

        /**
         * Sets the hub search.
         *
         * @param cellSize         {@code highwayPlanningCellSize}
         * @param sampleSpacing    {@code highwayHubSampleSpacing}
         * @param minimumPotential {@code highwayHubMinimumPotential}
         * @param searchRadius     {@code highwayHubSearchRadiusCells}
         * @return this builder
         */
        public Builder hubs(int cellSize, int sampleSpacing, float minimumPotential, int searchRadius) {
            planningCellSize = cellSize;
            hubSampleSpacing = sampleSpacing;
            hubMinimumPotential = minimumPotential;
            hubSearchRadiusCells = searchRadius;
            return this;
        }

        /**
         * Sets the connection rules.
         *
         * @param minimumDistance    {@code highwayMinimumHubDistance}
         * @param maximumDistance    {@code highwayMaximumHubDistance}
         * @param maximumConnections {@code highwayMaximumConnectionsPerHub}
         * @param minimumLength      {@code highwayMinimumRouteLength}
         * @param cityPenalty        {@code highwayRouteCityPenalty}
         * @return this builder
         */
        public Builder connections(int minimumDistance, int maximumDistance, int maximumConnections, int minimumLength,
            float cityPenalty) {
            minimumHubDistance = minimumDistance;
            maximumHubDistance = maximumDistance;
            maximumConnectionsPerHub = maximumConnections;
            minimumRouteLength = minimumLength;
            routeCityPenalty = cityPenalty;
            return this;
        }

        /**
         * Sets how a route's level is chosen.
         *
         * @param mode  {@code highwayLevelFromCities}
         * @param level {@code highwayNetworkLevel}
         * @return this builder
         */
        public Builder levels(int mode, int level) {
            levelFromCities = mode;
            networkLevel = level;
            return this;
        }

        /**
         * Sets the railway spacings the planner keeps off.
         *
         * @param northSouth {@code railwaySpacingNorthSouth}
         * @param eastWest   {@code railwaySpacingEastWest}
         * @return this builder
         */
        public Builder railwaySpacing(int northSouth, int eastWest) {
            railwaySpacingNorthSouth = northSouth;
            railwaySpacingEastWest = eastWest;
            return this;
        }

        /**
         * Returns the settings.
         *
         * @return the settings
         * @throws IllegalArgumentException when a value is outside upstream's range
         */
        public HighwaySettings build() {
            return new HighwaySettings(this);
        }
    }
}
