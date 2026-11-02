package com.tuxprogrammer.lostcities.api;

import java.util.List;

/**
 * What one chunk holds in a world with cities: its city, building, street, highways, rails, landmark zone and damage
 * (docs/api.md; docs/original-spec/01-core/api.md, {@code ILostChunkInfo}). Names of assets are full ids,
 * {@code namespace:path}.
 *
 * <p>
 * Side: server.
 */
public interface ILostChunkInfo {

    /** Returns the chunk X. */
    int getChunkX();

    /** Returns the chunk Z. */
    int getChunkZ();

    /** Returns whether the chunk belongs to a city. */
    boolean isCity();

    /** Returns what the chunk is: a building, a street, a park, a landmark core, or nothing of a city. */
    LostChunkType getType();

    /** Returns the city style's name, or {@code null} outside a city. */
    String getCityStyle();

    /** Returns the city's radius and style when the chunk is a city's center, else {@code null}. */
    ILostCityInfo getCityInfo();

    /** Returns the building's name, or {@code null} when the chunk has no building. */
    String getBuildingType();

    /** Returns the multibuilding the chunk is part of, or {@code null}. */
    LostMultiBuildingInfo getMultiBuildingInfo();

    /** Returns the city level, 0 for the lowest; 0 outside a city. */
    int getCityLevel();

    /** Returns the block Y of the chunk's city ground: its street, or its building's ground floor. */
    int getCityGroundLevel();

    /** Returns the floors above ground, the roof part's level included; 0 without a building. */
    int getNumFloors();

    /** Returns the cellars; 0 without a building. */
    int getNumCellars();

    /** Returns the planned road through the chunk. */
    LostRoadType getRoadType();

    /** Returns the highway level along X, or -1 for none. */
    int getHighwayXLevel();

    /** Returns the highway level along Z, or -1 for none. */
    int getHighwayZLevel();

    /** Returns the higher highway level, or -1 for none. */
    int getMaxHighwayLevel();

    /** Returns the railway piece in the chunk; {@link RailChunkType#NONE} for none. Never {@code null}. */
    RailChunkType getRailType();

    /** Returns the railway's level, or -1 for none. */
    int getRailLevel();

    /** Returns the landmark zone the chunk lies in: a core is reserved for another mod's structure. */
    LostLandmarkZone getLandmarkZone();

    /**
     * Returns the scattered structure whose footprint holds the chunk: its world-style entry's id, such as
     * {@code lostcities:cabin}, or {@code null} for none. Pure: a chunk not generated yet answers what will stand
     * there.
     */
    String getScatteredStructure();

    /** Returns the level of the ruined building's ruin, or -1 when it is not ruined. */
    int getRuinLevel();

    /**
     * Returns the explosion damage at the center of a 16-block section of the chunk.
     *
     * @param chunkY the section, 0 to 15 (block Y 16 × chunkY to 16 × chunkY + 15)
     * @return 0 for none; above 1 destroys blocks
     */
    float getDamage(int chunkY);

    /** Returns the explosions that reach the chunk. */
    List<ILostExplosion> getExplosions();
}
