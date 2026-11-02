package com.tuxprogrammer.lostcities.compat.villagenames;

/**
 * Builds the plan of the village that starts in a chunk, given that VillageNames' grid and biome test accept it. At
 * runtime it is {@code VillageNamesPlans}, which runs VillageNames' own start constructor; tests use fakes.
 *
 * <p>
 * Contract: pure. The plan is a function of the world seed, the chunk, VillageNames' config and the biome provider,
 * whatever was asked before.
 *
 * <p>
 * Side: server (world generation), and plain Java in tests.
 */
@FunctionalInterface
public interface VillagePlans {

    /**
     * Returns the plan of the village starting in a chunk.
     *
     * @param chunkX the start chunk X
     * @param chunkZ the start chunk Z
     * @return the plan
     */
    VillagePlan plan(int chunkX, int chunkZ);
}
