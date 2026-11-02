package com.tuxprogrammer.lostcities.layout.highway;

/**
 * Where a highway may not go: a landmark's core and its margin, the edge ring the city keeps around it (0006 §1,
 * D31 P06-1 and P06-2, alpha review AR-10). Hubs never lie in one, routes prefer a shape that crosses none, detour
 * around one, or end before it. Pure: the answer comes from the structure oracle, never from what was generated.
 *
 * <p>
 * Spec: docs/new-spec/06-highways-railways/index.md ("Highways in landmark cores", "R: route geometry"). Side: both.
 */
public interface HighwayObstacles {

    /** No obstacles anywhere. */
    HighwayObstacles NONE = new HighwayObstacles() {

        @Override
        public boolean blocksHub(int chunkX, int chunkZ) {
            return false;
        }

        @Override
        public Area area(int minX, int minZ, int maxX, int maxZ) {
            return (x, z) -> false;
        }
    };

    /** The obstacles of one rectangle of chunks, read once. */
    @FunctionalInterface
    interface Area {

        /**
         * Returns whether a chunk inside the rectangle is an obstacle; a chunk outside it may answer either way.
         *
         * @param chunkX chunk X
         * @param chunkZ chunk Z
         * @return whether a highway may not enter it
         */
        boolean blocked(int chunkX, int chunkZ);
    }

    /**
     * Returns whether a hub sample may not be a hub: the chunk is an obstacle.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return whether the sample is skipped
     */
    boolean blocksHub(int chunkX, int chunkZ);

    /**
     * Reads the obstacles of a rectangle of chunks, both corners included.
     *
     * @param minX lowest chunk X
     * @param minZ lowest chunk Z
     * @param maxX highest chunk X
     * @param maxZ highest chunk Z
     * @return the area's obstacles
     */
    Area area(int minX, int minZ, int maxX, int maxZ);
}
