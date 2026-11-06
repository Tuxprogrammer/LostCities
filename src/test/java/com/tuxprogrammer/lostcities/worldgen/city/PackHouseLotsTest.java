package com.tuxprogrammer.lostcities.worldgen.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.layout.LotKind;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;

/** Pack houses stand only on empty edge lots, at the option's rate, with a door side, the same on every query. */
public class PackHouseLotsTest {

    private static final LayoutCityPlan PLAN = CityFixtures.plan();
    private static final int R = 60;

    @Test
    public void onlyEmptyEdgeLotsHoldAHouseAtAboutTheOptionsRate() {
        LayoutPlanner planner = PLAN.planner();
        int bare = 0;
        int houses = 0;
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                ChunkPlan plan = PLAN.chunk(x, z);
                boolean empty = planner.plan(x, z)
                    .lot() == LotKind.BARE;
                if (!empty) {
                    assertEquals(-1, plan.packHouseSide(), "a house at (" + x + ", " + z + ") that is not an empty lot");
                    continue;
                }
                bare++;
                if (plan.packHouseSide() >= 0) {
                    houses++;
                    assertTrue(plan.packHouseSide() < 4);
                    assertEquals(plan.packHouseSide(), PLAN.chunk(x, z).packHouseSide());
                }
            }
        }
        assertTrue(bare >= 20, "the test world has empty lots, found " + bare);
        double chance = 0.25;
        double sigma = Math.sqrt(bare * chance * (1 - chance));
        assertEquals(bare * chance, houses, 4 * sigma + 1, "houses among " + bare + " empty lots");
    }
}
