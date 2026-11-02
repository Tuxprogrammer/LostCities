package com.tuxprogrammer.lostcities.api;

/**
 * A multibuilding asset: a grid of buildings over several chunks (docs/original-spec/01-core/api.md,
 * {@code ILostCityMultiBuilding}).
 *
 * <p>
 * Side: both.
 */
public interface ILostCityMultiBuilding extends ILostCityAsset {

    /**
     * Returns the building at a grid position, relative to the north-west chunk.
     *
     * @param x 0 to {@link #getDimX()} − 1
     * @param z 0 to {@link #getDimZ()} − 1
     * @return the building's id, or {@code null} for an empty slot or a position outside the grid
     */
    String getBuilding(int x, int z);

    /** Returns the size along X, in chunks. */
    int getDimX();

    /** Returns the size along Z, in chunks. */
    int getDimZ();
}
