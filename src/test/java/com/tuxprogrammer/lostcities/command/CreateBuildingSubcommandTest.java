package com.tuxprogrammer.lostcities.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.ForcedBuilding;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.worldgen.buffer.ChunkBufferDigest;
import com.tuxprogrammer.lostcities.worldgen.city.CityFixtures;
import com.tuxprogrammer.lostcities.worldgen.city.LayoutCityPlan;

/** {@link CreateBuildingSubcommand} and {@code LayoutPlanner.forcedBuilding}: a forced building, the layout intact. */
public class CreateBuildingSubcommandTest {

    @Test
    public void doesNotReproduceCORE32_aForcedBuildingLeavesTheCachedLayoutAlone() {
        LayoutCityPlan city = CityFixtures.plan();
        LayoutPlanner planner = city.planner();
        int[] chunk = CityFixtures.first(
            12,
            (x, z) -> planner.plan(x, z)
                .hasBuilding());
        ChunkPlan before = planner.plan(chunk[0], chunk[1]);
        String hashBefore = ChunkBufferDigest.sha256(CityFixtures.generate(city, chunk[0], chunk[1]));

        ForcedBuilding forced = planner.forcedBuilding(chunk[0], chunk[1], "building2", 4, 2);
        assertNotNull(forced);

        assertSame(before, planner.plan(chunk[0], chunk[1]), "the cached plan is the same object");
        assertEquals(
            before.building(),
            planner.plan(chunk[0], chunk[1])
                .building());
        assertEquals(
            hashBefore,
            ChunkBufferDigest.sha256(CityFixtures.generate(city, chunk[0], chunk[1])),
            "generation of the chunk is unchanged");
    }

    @Test
    public void everyLevelHasAPartFromTheLowestCellarUp() {
        LayoutPlanner planner = CityFixtures.plan()
            .planner();
        ForcedBuilding forced = planner.forcedBuilding(0, 0, "lostcities:building2", 5, 3);
        assertNotNull(forced);
        assertEquals(9, forced.levels());
        for (int floor = -3; floor <= 5; floor++) {
            assertNotNull(forced.part(floor), "level " + floor);
            assertNotNull(CityFixtures.ASSETS.part(AssetId.parse(forced.part(floor))), forced.part(floor));
        }
        ForcedBuilding again = planner.forcedBuilding(0, 0, "building2", 5, 3);
        assertEquals(forced.toString(), again.toString(), "the same chunk forces the same parts");
    }

    @Test
    public void anUnknownBuildingOrABadCountGivesNothing() {
        LayoutPlanner planner = CityFixtures.plan()
            .planner();
        assertNull(planner.forcedBuilding(0, 0, "no_such_building", 3, 0));
        assertNull(planner.forcedBuilding(0, 0, "building2", 0, 0));
        assertNull(planner.forcedBuilding(0, 0, "building2", 3, -1));
    }

    @Test
    public void theRoofStaysBelowY255() {
        // 20 floors and 10 cellars are 31 levels of 6: 186 blocks.
        assertEquals(20, CreateBuildingSubcommand.floorsThatFit(255 - 186 + 1 - 1, 10));
        int bottom = 200;
        int fit = CreateBuildingSubcommand.floorsThatFit(bottom, 2);
        assertTrue(bottom + (fit + 2 + 1) * ChunkPlan.LEVEL_HEIGHT - 1 <= CreateBuildingSubcommand.TOP_Y);
        assertTrue(bottom + (fit + 1 + 2 + 1) * ChunkPlan.LEVEL_HEIGHT - 1 > CreateBuildingSubcommand.TOP_Y);
    }
}
