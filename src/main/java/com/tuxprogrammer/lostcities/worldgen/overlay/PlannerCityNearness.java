package com.tuxprogrammer.lostcities.worldgen.overlay;

import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.layout.landmark.CityNearness;

/**
 * The world's {@link CityNearness}, answered from its layout planner's raw city map ({@link LayoutPlanner#isCityRaw}),
 * which depends on the seed, the profile, the biome provider and the height oracle, never on the structure oracles,
 * so asking it from an oracle cannot loop. The structure oracles are built before the planner, so the planner is set
 * once it exists; until then every answer is the conservative "a city may be here".
 *
 * <p>
 * Server thread only. Side: server.
 */
final class PlannerCityNearness implements CityNearness {

    private volatile LayoutPlanner planner;

    /**
     * Sets the planner whose city map answers.
     *
     * @param planner the world's planner
     */
    void set(LayoutPlanner planner) {
        this.planner = planner;
    }

    @Override
    public boolean anyRawCity(int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
        LayoutPlanner cities = planner;
        if (cities == null) {
            return true;
        }
        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                if (cities.isCityRaw(x, z)) {
                    return true;
                }
            }
        }
        return false;
    }
}
