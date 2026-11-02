package com.tuxprogrammer.lostcities.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.editor.EditRecords;
import com.tuxprogrammer.lostcities.editor.EditSession;
import com.tuxprogrammer.lostcities.editor.EditSessions;
import com.tuxprogrammer.lostcities.editor.PartRecord;
import com.tuxprogrammer.lostcities.worldgen.city.CityFixtures;
import com.tuxprogrammer.lostcities.worldgen.city.LayoutCityPlan;

/** {@link EditPartSubcommand}: {@code editpart} and {@code resumeedit} over records and sessions. */
public class EditPartSubcommandTest {

    @Test
    public void theTwoNames() {
        assertEquals("editpart", new EditPartSubcommand(false).name());
        assertEquals("resumeedit", new EditPartSubcommand(true).name());
    }

    @Test
    public void doesNotReproduceCORE34_onlyTheTerrainPhaseRecordsNeverTheDryRun() {
        LayoutCityPlan city = CityFixtures.plan();
        EditRecords records = new EditRecords(() -> {});
        city.context()
            .recorder(records.recorder(0));
        int[] chunk = CityFixtures.first(
            12,
            (x, z) -> city.planner()
                .plan(x, z)
                .hasBuilding());
        // The population dry run writes the same parts into a job list; it must not record them.
        city.chunk(chunk[0], chunk[1])
            .jobs();
        assertEquals(0, records.chunkCount(), "the dry run recorded parts");
        CityFixtures.generate(city, chunk[0], chunk[1]);
        assertFalse(
            records.at(0, chunk[0], chunk[1])
                .isEmpty(),
            "the terrain phase records them");
        int count = records.at(0, chunk[0], chunk[1])
            .size();
        city.chunk(chunk[0], chunk[1])
            .jobs();
        assertEquals(
            count,
            records.at(0, chunk[0], chunk[1])
                .size());
    }

    @Test
    public void doesNotReproduceCORE35_aSessionIsTheWorldsNotAStaticMap() {
        UUID player = new UUID(7, 7);
        EditSessions world = new EditSessions();
        world.start(
            player,
            new EditSession(
                0,
                0,
                new PartRecord(
                    com.tuxprogrammer.lostcities.asset.AssetId.parse("building1_1"),
                    70,
                    com.tuxprogrammer.lostcities.asset.transform.Transform.ROTATE_NONE,
                    0,
                    0),
                java.util.Collections.emptyMap()));
        world.end(player);
        assertNull(world.get(player));
        assertNull(new EditSessions().get(player), "another world never sees the session");
    }
}
