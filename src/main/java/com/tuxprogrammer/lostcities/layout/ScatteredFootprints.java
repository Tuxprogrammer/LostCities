package com.tuxprogrammer.lostcities.layout;

/**
 * Which chunks a scattered structure's footprint touches (07-scattered; decision log D31): the Roguelike Dungeons
 * steering mixin keeps towers off them, and the population vetoes deny dungeons, lakes, lava and oil there.
 *
 * <p>
 * The seam is here so the steering is complete before scattered structures exist: until M07 builds them, a world's
 * footprints are {@link #NONE}, which is the truth then, since no scattered structure generates. M07 implements it
 * from its scattered layout (pure, like every layout answer: a function of the seed, the profile and the assets).
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
