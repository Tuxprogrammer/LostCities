package com.tuxprogrammer.lostcities.layout.view;

/**
 * The layout's read-only view of a multibuilding asset: a {@code dimX × dimZ} grid of single-chunk buildings that stand
 * together. The asset model (M02) implements it, rejecting a multibuilding larger than the world style's multichunk
 * area at load (CITY-25).
 *
 * <p>
 * Spec: docs/original-spec/04-city-layout/multibuildings.md. Side: both.
 */
public interface MultiBuildingView {

    /** Returns the multibuilding's name. */
    String name();

    /** Returns its width along X, in chunks. */
    int dimX();

    /** Returns its depth along Z, in chunks. */
    int dimZ();

    /**
     * Returns the building at a position in the grid.
     *
     * @param x 0 to {@code dimX - 1}
     * @param z 0 to {@code dimZ - 1}
     * @return the building's name
     */
    String buildingAt(int x, int z);
}
