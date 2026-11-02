package com.tuxprogrammer.lostcities.layout;

/**
 * Which chunks a scattered structure's footprint touches (07-scattered; decision log D31): the Roguelike Dungeons
 * steering mixin keeps towers off them, and the population vetoes deny dungeons, lakes, lava and oil there.
 *
 * <p>
 * A world answers it from its scattered planner's pure footprints ({@code ScatteredPlanner.inFootprint}: a function of
 * the seed, the profile and the assets); {@link #NONE} where scattered structures are switched off.
 *
 * <p>
 * Side: both (plain Java).
 */
@FunctionalInterface
public interface ScatteredFootprints {

    /** No scattered structures. */
    ScatteredFootprints NONE = (chunkX, chunkZ) -> false;

    /**
     * Returns whether a scattered structure's footprint touches a chunk.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return {@code true} when one does
     */
    boolean touches(int chunkX, int chunkZ);
}
