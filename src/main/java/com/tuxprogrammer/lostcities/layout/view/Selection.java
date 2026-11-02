package com.tuxprogrammer.lostcities.layout.view;

/**
 * The weighted lists of a city style the layout picks from, each with one {@code nextFloat} when the list is not empty
 * and none when it is (05-assets, {@code CityStyle.getRandom*}). The layout, never the asset model, fixes the order of
 * the draws.
 *
 * <p>
 * Side: both.
 */
public enum Selection {

    /** {@code buildings}: candidate single-chunk buildings. */
    BUILDINGS,
    /** {@code multibuildings}. */
    MULTIBUILDINGS,
    /** {@code parks}: park parts. */
    PARKS,
    /** {@code fountains}: fountain parts. */
    FOUNTAINS,
    /** {@code bridges}: bridge parts. */
    BRIDGES,
    /** {@code largebridges}: bridge parts for hierarchical primary spans. */
    LARGE_BRIDGES,
    /** {@code stairs}: stair parts. */
    STAIRS,
    /** {@code fronts}: building front parts. */
    FRONTS,
    /** {@code raildungeons}: rail dungeon parts. */
    RAIL_DUNGEONS
}
