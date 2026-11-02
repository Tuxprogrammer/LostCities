package com.tuxprogrammer.lostcities.layout;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/cityassets/EffectiveCitySettings.java

import com.tuxprogrammer.lostcities.layout.view.CityStyleView;

/**
 * A chunk's city-style settings resolved against the profile: each chance and park setting is the city style's when
 * it sets one, else the profile's; the style's floor and cellar values are constraints on the computed limits, never
 * replacements.
 *
 * <p>
 * Spec: docs/original-spec/02-profiles-dimensions/effective-city-settings.md,
 * docs/original-spec/04-city-layout/city-settings.md. Side: both.
 */
public final class EffectiveCitySettings {

    private final float buildingChance;
    private final Integer minFloorConstraint;
    private final Integer maxFloorConstraint;
    private final Integer minCellarConstraint;
    private final Integer maxCellarConstraint;
    private final float parkChance;
    private final float openLotParkChance;
    private final float fountainChance;
    private final float frontChance;
    private final float corridorChance;
    private final boolean avoidFoliage;
    private final boolean parkBorder;
    private final boolean parkElevation;
    private final int parkStreetThreshold;

    private EffectiveCitySettings(LayoutProfile profile, CityStyleView style) {
        buildingChance = valueOr(style == null ? null : style.buildingChance(), profile.buildingChance);
        minFloorConstraint = style == null ? null : style.minFloors();
        maxFloorConstraint = style == null ? null : style.maxFloors();
        minCellarConstraint = style == null ? null : style.minCellars();
        maxCellarConstraint = style == null ? null : style.maxCellars();
        parkChance = valueOr(style == null ? null : style.parkChance(), profile.parkChance);
        openLotParkChance = valueOr(style == null ? null : style.openLotParkChance(), profile.openLotParkChance);
        fountainChance = valueOr(style == null ? null : style.fountainChance(), profile.fountainChance);
        frontChance = valueOr(style == null ? null : style.frontChance(), profile.buildingFrontChance);
        corridorChance = valueOr(style == null ? null : style.corridorChance(), profile.corridorChance);
        avoidFoliage = valueOr(style == null ? null : style.avoidFoliage(), profile.avoidFoliage);
        parkBorder = valueOr(style == null ? null : style.parkBorder(), profile.parkBorder);
        parkElevation = valueOr(style == null ? null : style.parkElevation(), profile.parkElevation);
        parkStreetThreshold = valueOr(style == null ? null : style.parkStreetThreshold(), profile.parkStreetThreshold);
    }

    /**
     * Resolves a city style's settings against a profile.
     *
     * @param profile the profile
     * @param style   the chunk's city style, or {@code null} for profile values only
     * @return the effective settings
     */
    public static EffectiveCitySettings resolve(LayoutProfile profile, CityStyleView style) {
        return new EffectiveCitySettings(profile, style);
    }

    /** Returns the building chance. */
    public float buildingChance() {
        return buildingChance;
    }

    /** Returns the park chance (upstream's legacy street mode; kept for the API). */
    public float parkChance() {
        return parkChance;
    }

    /** Returns the chance an open lot gets a park part. */
    public float openLotParkChance() {
        return openLotParkChance;
    }

    /** Returns the fountain chance. */
    public float fountainChance() {
        return fountainChance;
    }

    /** Returns the chance a building has a front part. */
    public float frontChance() {
        return frontChance;
    }

    /** Returns the corridor chance, per axis. */
    public float corridorChance() {
        return corridorChance;
    }

    /** Returns whether park parts avoid foliage. */
    public boolean avoidFoliage() {
        return avoidFoliage;
    }

    /** Returns whether a park's edge uses the street block. */
    public boolean parkBorder() {
        return parkBorder;
    }

    /** Returns whether elevated parks are raised one block. */
    public boolean parkElevation() {
        return parkElevation;
    }

    /** Returns how many of a park's 8 neighbors must be street or park for it to be elevated. */
    public int parkStreetThreshold() {
        return parkStreetThreshold;
    }

    /**
     * Applies the style's minimum-floor constraint.
     *
     * @param floors the computed minimum
     * @return {@code max(floors, minfloors)} when the style sets one
     */
    public int constrainMinimumFloors(int floors) {
        return minFloorConstraint == null ? floors : Math.max(floors, minFloorConstraint);
    }

    /**
     * Applies the style's maximum-floor constraint.
     *
     * @param floors the computed maximum
     * @return {@code min(floors, maxfloors)} when the style sets one
     */
    public int constrainMaximumFloors(int floors) {
        return maxFloorConstraint == null ? floors : Math.min(floors, maxFloorConstraint);
    }

    /**
     * Applies the style's cellar constraints.
     *
     * @param cellars the computed maximum
     * @return {@code min} with the style's maximum, then {@code max} with its minimum, where set
     */
    public int constrainMaximumCellars(int cellars) {
        int result = maxCellarConstraint == null ? cellars : Math.min(cellars, maxCellarConstraint);
        return minCellarConstraint == null ? result : Math.max(result, minCellarConstraint);
    }

    private static float valueOr(Float value, float fallback) {
        return value == null ? fallback : value;
    }

    private static int valueOr(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private static boolean valueOr(Boolean value, boolean fallback) {
        return value == null ? fallback : value;
    }
}
