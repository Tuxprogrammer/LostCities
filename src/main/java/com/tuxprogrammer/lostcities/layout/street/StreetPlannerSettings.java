package com.tuxprogrammer.lostcities.layout.street;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/street/StreetPlannerSettings.java

/**
 * The validated settings of the hierarchical street field, from the profile's {@code lostcity} category. The
 * constructor enforces upstream's checks; the profile loader rejects a profile that fails them with a message before a
 * planner is ever built (new-spec 02-profiles-dimensions), so the exception here guards programming errors only.
 *
 * <p>
 * Spec: docs/original-spec/04-city-layout/street-planner.md#3-numbers. Side: both.
 */
public final class StreetPlannerSettings {

    /** Upstream's defaults: the {@code default} profile's values. */
    public static final StreetPlannerSettings DEFAULTS = new StreetPlannerSettings(
        8,
        8,
        .45f,
        4,
        0,
        2,
        0,
        2,
        4,
        3,
        .40f,
        2,
        5);

    /** Smallest spacing between primary candidates, in chunks. */
    public static final int MIN_PRIMARY_SPACING = 8;
    /** Largest {@code primaryRoadForceEvery}. */
    public static final int MAX_FORCE_EVERY = 16;
    /** Smallest road separation and edge distance, in chunks. */
    public static final int MIN_SEPARATION = 2;

    private final int primarySpacingX;
    private final int primarySpacingZ;
    private final float primaryOptionalChance;
    private final int primaryForceEvery;
    private final int secondaryMinCountX;
    private final int secondaryMaxCountX;
    private final int secondaryMinCountZ;
    private final int secondaryMaxCountZ;
    private final int minimumRoadSeparation;
    private final int minimumEdgeDistance;
    private final float tertiaryChance;
    private final int tertiaryMinLength;
    private final int tertiaryMaxLength;

    /**
     * Creates validated settings.
     *
     * @param primarySpacingX       chunks between primary candidates along X ({@code primaryRoadSpacingX}), at least 8
     * @param primarySpacingZ       likewise along Z
     * @param primaryOptionalChance chance an unforced candidate is active, 0 to 1
     * @param primaryForceEvery     every Nth candidate is always active, 1 to 16
     * @param secondaryMinCountX    fewest secondary roads per block along X
     * @param secondaryMaxCountX    most secondary roads per block along X
     * @param secondaryMinCountZ    fewest along Z
     * @param secondaryMaxCountZ    most along Z
     * @param minimumRoadSeparation chunks between secondary roads, at least 2
     * @param minimumEdgeDistance   chunks from the block edge, at least 2
     * @param tertiaryChance        chance a cell gets a tertiary road, 0 to 1
     * @param tertiaryMinLength     shortest tertiary road in chunks, at least 1
     * @param tertiaryMaxLength     longest, at least the minimum
     * @throws IllegalArgumentException with upstream's message when a check fails
     */
    public StreetPlannerSettings(int primarySpacingX, int primarySpacingZ, float primaryOptionalChance,
        int primaryForceEvery, int secondaryMinCountX, int secondaryMaxCountX, int secondaryMinCountZ,
        int secondaryMaxCountZ, int minimumRoadSeparation, int minimumEdgeDistance, float tertiaryChance,
        int tertiaryMinLength, int tertiaryMaxLength) {
        if (primarySpacingX < MIN_PRIMARY_SPACING || primarySpacingZ < MIN_PRIMARY_SPACING) {
            throw new IllegalArgumentException("Primary road candidate spacing must be at least 8 chunks");
        }
        if (primaryOptionalChance < 0 || primaryOptionalChance > 1
            || primaryForceEvery < 1
            || primaryForceEvery > MAX_FORCE_EVERY) {
            throw new IllegalArgumentException("Invalid primary road activation settings");
        }
        if (secondaryMinCountX < 0 || secondaryMinCountZ < 0
            || secondaryMinCountX > secondaryMaxCountX
            || secondaryMinCountZ > secondaryMaxCountZ) {
            throw new IllegalArgumentException("Invalid secondary road count range");
        }
        if (minimumRoadSeparation < MIN_SEPARATION || minimumEdgeDistance < MIN_SEPARATION) {
            throw new IllegalArgumentException("Road separation and edge distance must be at least 2 chunks");
        }
        if (tertiaryChance < 0 || tertiaryChance > 1
            || tertiaryMinLength < 1
            || tertiaryMinLength > tertiaryMaxLength) {
            throw new IllegalArgumentException("Invalid tertiary road settings");
        }
        this.primarySpacingX = primarySpacingX;
        this.primarySpacingZ = primarySpacingZ;
        this.primaryOptionalChance = primaryOptionalChance;
        this.primaryForceEvery = primaryForceEvery;
        this.secondaryMinCountX = secondaryMinCountX;
        this.secondaryMaxCountX = secondaryMaxCountX;
        this.secondaryMinCountZ = secondaryMinCountZ;
        this.secondaryMaxCountZ = secondaryMaxCountZ;
        this.minimumRoadSeparation = minimumRoadSeparation;
        this.minimumEdgeDistance = minimumEdgeDistance;
        this.tertiaryChance = tertiaryChance;
        this.tertiaryMinLength = tertiaryMinLength;
        this.tertiaryMaxLength = tertiaryMaxLength;
    }

    /** Returns {@code primaryRoadSpacingX}, in chunks. */
    public int primarySpacingX() {
        return primarySpacingX;
    }

    /** Returns {@code primaryRoadSpacingZ}, in chunks. */
    public int primarySpacingZ() {
        return primarySpacingZ;
    }

    /** Returns {@code primaryRoadOptionalChance}. */
    public float primaryOptionalChance() {
        return primaryOptionalChance;
    }

    /** Returns {@code primaryRoadForceEvery}. */
    public int primaryForceEvery() {
        return primaryForceEvery;
    }

    /** Returns {@code secondaryRoadMinCountX}. */
    public int secondaryMinCountX() {
        return secondaryMinCountX;
    }

    /** Returns {@code secondaryRoadMaxCountX}. */
    public int secondaryMaxCountX() {
        return secondaryMaxCountX;
    }

    /** Returns {@code secondaryRoadMinCountZ}. */
    public int secondaryMinCountZ() {
        return secondaryMinCountZ;
    }

    /** Returns {@code secondaryRoadMaxCountZ}. */
    public int secondaryMaxCountZ() {
        return secondaryMaxCountZ;
    }

    /** Returns {@code minimumRoadSeparation}, in chunks. */
    public int minimumRoadSeparation() {
        return minimumRoadSeparation;
    }

    /** Returns {@code minimumRoadEdgeDistance}, in chunks. */
    public int minimumEdgeDistance() {
        return minimumEdgeDistance;
    }

    /** Returns {@code tertiaryRoadChance}. */
    public float tertiaryChance() {
        return tertiaryChance;
    }

    /** Returns {@code tertiaryRoadMinLength}, in chunks. */
    public int tertiaryMinLength() {
        return tertiaryMinLength;
    }

    /** Returns {@code tertiaryRoadMaxLength}, in chunks. */
    public int tertiaryMaxLength() {
        return tertiaryMaxLength;
    }
}
