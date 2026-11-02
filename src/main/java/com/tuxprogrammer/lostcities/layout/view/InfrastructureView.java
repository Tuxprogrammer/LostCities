package com.tuxprogrammer.lostcities.layout.view;

/**
 * What the city layout reads about highways and railways, which M06 lays out. The answers must be the raw
 * infrastructure layout's: they may depend on city membership and landmark zones (which come before any plan), but
 * never on a chunk's building plan, which reads them. Where a building blocks a rail ({@code block_railway}), the
 * railway layout asks the building plan instead of the plan deleting the rail (D9, CITY-19).
 *
 * <p>
 * Until M06, {@link #NONE} stands in: no highways, no railways.
 *
 * <p>
 * Side: both.
 */
public interface InfrastructureView {

    /** No highways and no railways. */
    InfrastructureView NONE = new InfrastructureView() {

        @Override
        public int highwayXLevel(int chunkX, int chunkZ) {
            return -1;
        }

        @Override
        public int highwayZLevel(int chunkX, int chunkZ) {
            return -1;
        }

        @Override
        public RailInfo rail(int chunkX, int chunkZ) {
            return RailInfo.NONE;
        }

        @Override
        public int railwayLevel() {
            return 0;
        }
    };

    /**
     * Returns the city level of the east-west highway through a chunk.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the level, or -1 when there is none
     */
    int highwayXLevel(int chunkX, int chunkZ);

    /**
     * Returns the city level of the north-south highway through a chunk.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the level, or -1 when there is none
     */
    int highwayZLevel(int chunkX, int chunkZ);

    /**
     * Returns the railway at a chunk.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the rail info, {@link RailInfo#NONE} when there is none
     */
    RailInfo rail(int chunkX, int chunkZ);

    /** Returns the profile's main railway city level (upstream's {@code Railway.getRailwayLevel}). */
    int railwayLevel();
}
