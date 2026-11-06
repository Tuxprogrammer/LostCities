package com.tuxprogrammer.lostcities.layout.rail;

/**
 * What the railway layout reads about the world, all pure and none of it asking for railways: raw city membership
 * and city levels (as upstream's {@code BuildingInfo.isCityRaw} and {@code getCityLevel}), the highway levels next to
 * stations, and the landmark zones and ground the rules for cores read (0006 §1, D30, D31).
 *
 * <p>
 * Side: both.
 */
public interface RailwayInputs {

    /**
     * Returns whether a chunk is raw city: its city factor is above the threshold, before landmark zones.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return whether it is raw city
     */
    boolean isCityRaw(int chunkX, int chunkZ);

    /**
     * Returns a chunk's city level from the height oracle.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the level
     */
    int cityLevel(int chunkX, int chunkZ);

    /**
     * Returns the city level of the east-west highway through a chunk, or −1.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the level
     */
    int highwayXLevel(int chunkX, int chunkZ);

    /**
     * Returns the city level of the north-south highway through a chunk, or −1.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the level
     */
    int highwayZLevel(int chunkX, int chunkZ);

    /**
     * Returns whether a chunk is a landmark's core.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return whether it is a core
     */
    boolean isCore(int chunkX, int chunkZ);

    /**
     * Returns whether a chunk is a landmark's margin (its edge ring, or the ring an avoided landmark keeps).
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return whether it is a margin
     */
    boolean isMargin(int chunkX, int chunkZ);

    /**
     * Returns whether a chunk is the core of a landmark that reaches underground (a LootGames room): no link-network
     * line passes under it at any railway level (D-p4v-4).
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return whether it is an underground core
     */
    default boolean isUndergroundCore(int chunkX, int chunkZ) {
        return false;
    }

    /**
     * Returns the lowest oracle ground around a core chunk, block Y: what a railway must stay 8 blocks under.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return block Y
     */
    int lowestGround(int chunkX, int chunkZ);
}
