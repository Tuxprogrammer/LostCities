package com.tuxprogrammer.lostcities.layout.view;

import java.util.Random;

/**
 * The layout's read-only view of a building asset: its floor and cellar limits, how much it prefers to stand alone,
 * and its two part pickers. The asset model (M02) implements it; tests use fakes.
 *
 * <p>
 * Spec: docs/original-spec/04-city-layout/building-info.md § Floor and cellar limits. Side: both.
 */
public interface BuildingView {

    /** Value of a floor or cellar limit the building does not set. */
    int UNSET = -1;

    /** Returns the building's name. */
    String name();

    /** Returns {@code minfloors}, or {@link #UNSET}. */
    int minFloors();

    /** Returns {@code maxfloors}, or {@link #UNSET}. */
    int maxFloors();

    /** Returns {@code mincellars}, or {@link #UNSET}. */
    int minCellars();

    /** Returns {@code maxcellars}, or {@link #UNSET}. */
    int maxCellars();

    /** Returns {@code overrideFloors}: the building's limits replace the profile's instead of narrowing them. */
    boolean overrideFloors();

    /** Returns {@code preferslonely}, 0 to 1: the chance a neighbor of this candidate building has no building. */
    float prefersLonely();

    /**
     * Picks the part for one level among the parts whose condition matches.
     *
     * <p>
     * Contract: one {@code random.nextInt(n)} over the {@code n} matching parts when {@code n > 0}, no draw when none
     * matches, and then {@code null}. The asset loader rejects a building whose main parts can match nothing (CITY-17),
     * so {@code null} means a chunk where no building stands.
     *
     * @param random  the chunk's random
     * @param context the level being picked for
     * @return the part name, or {@code null}
     */
    String pickPart(Random random, PartContext context);

    /**
     * Picks the overlay part ({@code parts2}) for one level, with the same contract as {@link #pickPart}; {@code null}
     * is a normal result.
     *
     * @param random  the chunk's random
     * @param context the level, with {@link PartContext#part()} set to the main part
     * @return the overlay part name, or {@code null}
     */
    String pickPart2(Random random, PartContext context);
}
