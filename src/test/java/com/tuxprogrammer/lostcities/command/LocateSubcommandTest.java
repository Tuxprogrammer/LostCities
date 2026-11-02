package com.tuxprogrammer.lostcities.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.worldgen.city.CityFixtures;

/** {@link LocateSubcommand}: a building's chunks near the sender, from the layout alone. */
public class LocateSubcommandTest {

    private static final LayoutPlanner PLANNER = CityFixtures.plan()
        .planner();

    @Test
    public void findsTheBuildingNearestFirstAndAtMostSevenTimes() {
        int[] chunk = CityFixtures.first(
            12,
            (x, z) -> PLANNER.plan(x, z)
                .hasBuilding());
        ChunkPlan plan = PLANNER.plan(chunk[0], chunk[1]);
        AssetId id = AssetId.parse(plan.building());
        List<int[]> hits = LocateSubcommand.find(PLANNER, id, chunk[0], chunk[1]);
        assertFalse(hits.isEmpty());
        assertTrue(hits.size() <= LocateSubcommand.MAX_HITS);
        assertEquals((chunk[0] << 4) + 8, hits.get(0)[0], "the sender's own chunk comes first");
        assertEquals(plan.cityGroundLevel(), hits.get(0)[1]);
        for (int[] hit : hits) {
            ChunkPlan found = PLANNER.plan(hit[0] >> 4, hit[2] >> 4);
            assertEquals(id, AssetId.parse(found.building()));
        }
    }

    @Test
    public void aShortAndAFullNameFindTheSameChunks() {
        int[] chunk = CityFixtures.first(
            12,
            (x, z) -> PLANNER.plan(x, z)
                .hasBuilding());
        AssetId id = AssetId.parse(
            PLANNER.plan(chunk[0], chunk[1])
                .building());
        List<int[]> full = LocateSubcommand.find(PLANNER, AssetId.parse(id.toString()), 0, 0);
        List<int[]> bare = LocateSubcommand.find(PLANNER, AssetId.parse(id.path()), 0, 0);
        assertEquals(full.size(), bare.size());
    }
}
