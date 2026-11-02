package com.tuxprogrammer.lostcities.api;

/**
 * What Lost Cities planned for one world (docs/api.md; docs/original-spec/01-core/api.md,
 * {@code ILostCityInformation}). Every answer is a pure function of the world seed, the pinned profile and the loaded
 * assets, read from the layout generation uses; nothing here generates or loads a chunk.
 *
 * <p>
 * Deviation: {@code getSphere} waits for the sphere landscapes, after beta (D12).
 *
 * <p>
 * Side: server; call on the server thread.
 */
public interface ILostCityInformation {

    /** Returns the dimension ID. */
    int getDimension();

    /** Returns the name of the profile pinned into this world. */
    String getProfile();

    /**
     * Returns a chunk's information.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the information, never {@code null}
     */
    ILostChunkInfo getChunkInfo(int chunkX, int chunkZ);

    /** Returns the profile's ground level: the block Y of city level 0's street. */
    int getGroundLevel();

    /**
     * Returns the block Y of a city level's street: {@code groundLevel + 6 × level}. The air a player stands in
     * starts one block higher.
     *
     * @param level the city level, 0 or more
     * @return block Y
     */
    int getRealHeight(int level);

    /** Returns the loaded buildings. */
    ILostCityAssetRegistry<ILostCityBuilding> getBuildings();

    /** Returns the loaded multibuildings. */
    ILostCityAssetRegistry<ILostCityMultiBuilding> getMultiBuildings();

    /** Returns the loaded city styles. */
    ILostCityAssetRegistry<ILostCityCityStyle> getCityStyles();
}
