package com.tuxprogrammer.lostcities.layout.highway;

/**
 * The city level of a chunk, from the height oracle, as the layout computes it (upstream's
 * {@code HighwayCityLevelSource}): a hub's level, and the level a highway's dead end comes down or up to. Pure; never
 * asks for highways.
 *
 * <p>
 * Spec: docs/original-spec/06-highways-railways/city-potential.md ("City level at a hub"). Side: both.
 */
@FunctionalInterface
public interface HighwayTerrain {

    /**
     * Returns a chunk's city level.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the level, 0 or more
     */
    int cityLevel(int chunkX, int chunkZ);
}
