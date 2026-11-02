package com.tuxprogrammer.lostcities.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;

/**
 * The palette variety's seeds (D-M11-1, D-M11-10): a pure function of the world seed and the chunk, a building's per
 * chunk, a street's or park's shared by each four-by-four block of chunks.
 */
public class VarietySeedTest {

    private static LayoutPlanner planner(long seed) {
        return LayoutFixtures
            .planner(seed, LayoutProfile.defaults(), LayoutFixtures.flatTerrain(), StructureOracle.NONE);
    }

    @Test
    public void aBuildingsSeedDependsOnTheWorldAndTheChunkOnly() {
        LayoutPlanner a = planner(4242L);
        LayoutPlanner b = planner(4242L);
        assertEquals(a.varietySeed(3, -7), b.varietySeed(3, -7));
        assertNotEquals(a.varietySeed(3, -7), a.varietySeed(4, -7));
        assertNotEquals(a.varietySeed(3, -7), planner(4243L).varietySeed(3, -7));
    }

    @Test
    public void streetsShareTheirSeedPerFourByFourBlock() {
        LayoutPlanner p = planner(4242L);
        long block = p.streetVarietySeed(4, -8);
        for (int x = 4; x < 8; x++) {
            for (int z = -8; z < -4; z++) {
                assertEquals(block, p.streetVarietySeed(x, z), x + ", " + z);
            }
        }
        assertNotEquals(block, p.streetVarietySeed(8, -8));
        assertNotEquals(block, p.streetVarietySeed(4, -9));
        assertNotEquals(p.varietySeed(1, -2), p.streetVarietySeed(4, -8), "streets and buildings never share a seed");
    }
}
