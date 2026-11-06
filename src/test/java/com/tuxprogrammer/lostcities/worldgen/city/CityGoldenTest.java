package com.tuxprogrammer.lostcities.worldgen.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.layout.StreetType;
import com.tuxprogrammer.lostcities.worldgen.buffer.GoldenChunks;
import com.tuxprogrammer.lostcities.worldgen.plan.PopulationJob;

/**
 * Golden chunks of real cities (M05): the terrain phase with the real plan over the {@link CityFixtures} world, for a
 * multi-floor building, a primary street, a park and a ruined building, each taken after the terrain phase, and the
 * population jobs of the building and the street as a second golden (0005, "Determinism tests"). The chunks are the
 * first of each kind in a fixed scan from the origin, so a layout change shows here as a moved or changed chunk.
 */
public class CityGoldenTest {

    private static final LayoutCityPlan PLAN = CityFixtures.plan();
    private static final LayoutPlanner PLANNER = PLAN.planner();
    private static final int SEARCH = 24;

    private static int[] building() {
        return CityFixtures.first(SEARCH, (x, z) -> {
            ChunkPlan plan = PLANNER.plan(x, z);
            return plan.hasBuilding() && plan.floors() >= 3 && plan.cellars() >= 1 && plan.ruinHeight() < 0;
        });
    }

    private static int[] street() {
        return CityFixtures.first(
            SEARCH,
            (x, z) -> PLANNER.plan(x, z)
                .isPrimaryRoad());
    }

    private static int[] park() {
        return CityFixtures.first(SEARCH, (x, z) -> {
            ChunkPlan plan = PLANNER.plan(x, z);
            return plan.isCity() && plan.streetType() == StreetType.PARK && plan.parkPart() != null;
        });
    }

    private static int[] ruin() {
        return CityFixtures.first(
            SEARCH * 2,
            (x, z) -> PLANNER.plan(x, z)
                .ruinHeight() >= 0);
    }

    @Test
    public void goldenCityChunksAfterTheTerrainPhase() throws IOException {
        golden("city_building", building());
        golden("city_primary_street", street());
        golden("city_park", park());
        golden("city_ruin", ruin());
    }

    /**
     * The first multi-floor building with a cellar in the same world pinned at floor pitch 8 (design record 0014): its
     * floors and cellars stretched, its roof not.
     */
    @Test
    public void goldenCityBuildingAtPitchEight() throws IOException {
        LayoutCityPlan plan = CityFixtures.planAtPitch(8);
        LayoutPlanner planner = plan.planner();
        int[] at = CityFixtures.first(SEARCH, (x, z) -> {
            ChunkPlan chunk = planner.plan(x, z);
            return chunk.hasBuilding() && chunk.floors() >= 3 && chunk.cellars() >= 1 && chunk.ruinHeight() < 0;
        });
        assertNotNull(at, "city_building_p8 near the origin");
        assertEquals(8, planner.plan(at[0], at[1]).floorPitch());
        GoldenChunks.assertMatches("city_building_p8", CityFixtures.generate(plan, at[0], at[1]));
    }

    @Test
    public void goldenPopulationJobs() throws IOException {
        jobs("city_building_jobs", building());
        jobs("city_primary_street_jobs", street());
    }

    private static void golden(String name, int[] at) throws IOException {
        assertNotNull(at, name + " near the origin");
        GoldenChunks.assertMatches(name, CityFixtures.generate(PLAN, at[0], at[1]));
    }

    private static void jobs(String name, int[] at) throws IOException {
        assertNotNull(at, name + " near the origin");
        StringBuilder text = new StringBuilder("chunk ").append(at[0])
            .append(',')
            .append(at[1])
            .append('\n');
        for (PopulationJob job : PLAN.chunk(at[0], at[1])
            .jobs()) {
            text.append(job)
                .append('\n');
        }
        GoldenChunks.assertTextMatches(name, text.toString());
    }
}
