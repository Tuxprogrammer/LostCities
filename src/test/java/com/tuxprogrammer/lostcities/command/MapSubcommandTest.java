package com.tuxprogrammer.lostcities.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.IChatComponent;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.core.LangKeys;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.worldgen.city.CityFixtures;

/** {@link MapSubcommand}: upstream's 41 × 41 map from the layout alone. */
public class MapSubcommandTest {

    private static final LayoutPlanner PLANNER = CityFixtures.plan()
        .planner();

    @Test
    public void theMapIs41By41AndCountsWhatItDraws() {
        MapSubcommand.Map map = MapSubcommand.draw(PLANNER, 0, 0, MapSubcommand.RADIUS);
        assertEquals(41, map.rows.size());
        int buildings = 0;
        int city = 0;
        for (int i = 0; i < map.rows.size(); i++) {
            String row = map.rows.get(i);
            assertEquals(41, row.length());
            for (int x = 0; x < row.length(); x++) {
                char c = row.charAt(x);
                assertTrue("B+#. ".indexOf(c) >= 0, "unknown symbol " + c);
                buildings += c == 'B' ? 1 : 0;
                city += c == '+' ? 1 : 0;
                // North at the top, west on the left: row i is chunk Z i - 20, column x is chunk X x - 20.
                boolean planned = PLANNER.plan(x - 20, i - 20)
                    .hasBuilding()
                    && PLANNER.plan(x - 20, i - 20)
                        .isCity();
                assertEquals(planned, c == 'B', "chunk " + (x - 20) + "," + (i - 20));
            }
        }
        assertEquals(buildings, map.buildings);
        assertEquals(city, map.city);
        assertTrue(map.buildings > 0, "the fixture world has a city at the origin");
    }

    @Test
    public void doesNotReproduceCORE26_mapIsNotOpenToEveryPlayer() {
        List<IChatComponent> chat = new ArrayList<>();
        String error = TestSenders.errorKey(
            () -> new LostCitiesCommand().processCommand(TestSenders.console(1, chat), new String[] { "map" }));
        assertEquals(LangKeys.COMMAND_NO_PERMISSION, error);
    }
}
