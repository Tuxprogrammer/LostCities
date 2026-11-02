package com.tuxprogrammer.lostcities.layout.view;

import java.util.Random;

/**
 * The layout's read-only view of a resolved city style (after inheritance, 05-assets): the settings layout decisions
 * read and its weighted part lists. The asset model (M02) implements it; tests use fakes.
 *
 * <p>
 * Settings are boxed and {@code null} when no style in the inheritance chain sets them, in which case the profile's
 * value applies ({@link com.tuxprogrammer.lostcities.layout.EffectiveCitySettings}).
 *
 * <p>
 * Spec: docs/original-spec/04-city-layout/city-settings.md. Side: both.
 */
public interface CityStyleView {

    /** Returns the city style's name, as world styles and street-majority votes refer to it. */
    String name();

    /** Returns the name of the {@code style} asset whose palettes the style's chunks use. */
    String styleName();

    /** Returns {@code buildingsettings.buildingchance}, or {@code null}. */
    Float buildingChance();

    /** Returns {@code buildingsettings.minfloors}, or {@code null}. */
    Integer minFloors();

    /** Returns {@code buildingsettings.maxfloors}, or {@code null}. */
    Integer maxFloors();

    /** Returns {@code buildingsettings.mincellars}, or {@code null}. */
    Integer minCellars();

    /** Returns {@code buildingsettings.maxcellars}, or {@code null}. */
    Integer maxCellars();

    /** Returns {@code parks.parkchance}, or {@code null}. */
    Float parkChance();

    /** Returns the style's {@code openlotparkchance}, or {@code null}. */
    Float openLotParkChance();

    /** Returns {@code streets.fountainchance}, or {@code null}. */
    Float fountainChance();

    /** Returns {@code streets.frontchance}, or {@code null}. */
    Float frontChance();

    /** Returns {@code corridors.corridorchance}, or {@code null}. */
    Float corridorChance();

    /** Returns {@code parks.avoidfoliage}, or {@code null}. */
    Boolean avoidFoliage();

    /** Returns {@code parks.parkborder}, or {@code null}. */
    Boolean parkBorder();

    /** Returns {@code parks.parkelevation}, or {@code null}. */
    Boolean parkElevation();

    /** Returns {@code parks.parkstreetthreshold}, or {@code null}. */
    Integer parkStreetThreshold();

    /**
     * Picks from one of the style's weighted lists, honoring each entry's spawn-distance window.
     *
     * <p>
     * Contract: exactly one {@code random.nextFloat()} when the list has an entry, none when it is empty. The result
     * is {@code null} for an empty list and for a list whose every entry weighs zero at this chunk (ASSET-10: a
     * zero-weight entry is never picked), never for float rounding (CITY-09). Callers treat {@code null} as "nothing
     * picked here", the same as a missing style.
     *
     * @param list   which list
     * @param random the chunk's random
     * @param chunkX the chunk the pick is for (spawn-distance windows), chunk X
     * @param chunkZ chunk Z
     * @return the picked asset name, or {@code null} when nothing in the list can be picked here
     */
    String pick(Selection list, Random random, int chunkX, int chunkZ);
}
