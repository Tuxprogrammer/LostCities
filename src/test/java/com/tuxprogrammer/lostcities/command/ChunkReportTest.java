package com.tuxprogrammer.lostcities.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.IChatComponent;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.core.LangKeys;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.worldgen.city.CityFixtures;

/** {@link ChunkReport}: {@code debug}'s full dump, and the operator gate on {@code debug} and {@code map}. */
public class ChunkReportTest {

    private static final LayoutPlanner PLANNER = CityFixtures.plan()
        .planner();

    @Test
    public void theDumpNamesTheChunksBuildingAndEveryLevel() {
        int[] chunk = CityFixtures.first(
            12,
            (x, z) -> PLANNER.plan(x, z)
                .hasBuilding());
        ChunkPlan plan = PLANNER.plan(chunk[0], chunk[1]);
        List<String> lines = ChunkReport.lines(PLANNER, "default", chunk[0], chunk[1], "CITY", 0, 0.015f, 70);
        assertTrue(lines.contains("buildingType = " + plan.building()), lines.toString());
        assertTrue(lines.contains("isCity = true"));
        assertTrue(lines.contains("chunkX = " + chunk[0]));
        long floorLines = lines.stream()
            .filter(line -> line.startsWith("floor"))
            .count();
        assertTrue(floorLines >= plan.floors() + plan.cellars() + 1, "one line per level: " + lines);
        assertEquals(lines, ChunkReport.lines(PLANNER, "default", chunk[0], chunk[1], "CITY", 0, 0.015f, 70));
    }

    @Test
    public void doesNotReproduceCORE26_debugAndMapNeedOperatorLevel2() {
        assertEquals(2, new DebugSubcommand().permissionLevel());
        assertEquals(2, new MapSubcommand().permissionLevel());
        LostCitiesCommand command = new LostCitiesCommand();
        for (String subcommand : new String[] { "debug", "map" }) {
            List<IChatComponent> chat = new ArrayList<>();
            String error = TestSenders
                .errorKey(() -> command.processCommand(TestSenders.console(0, chat), new String[] { subcommand }));
            assertEquals(LangKeys.COMMAND_NO_PERMISSION, error, subcommand + " for a player without level 2");
            assertTrue(chat.isEmpty(), "nothing is sent before the check");
        }
    }
}
