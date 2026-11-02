package com.tuxprogrammer.lostcities.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.transform.Transform;
import com.tuxprogrammer.lostcities.worldgen.buffer.ChunkBufferDigest;
import com.tuxprogrammer.lostcities.worldgen.city.CityFixtures;
import com.tuxprogrammer.lostcities.worldgen.city.LayoutCityPlan;
import com.tuxprogrammer.lostcities.worldgen.city.PartRecorder;

/** {@link EditRecords}: the part editor's records, written by the terrain phase of an {@code editMode} world. */
public class EditRecordsTest {

    private static final AssetId STREET = AssetId.parse("street_basic_1");
    private static final AssetId FLOOR = AssetId.parse("building1_1");

    @Test
    public void doesNotReproduceCORE34_recordsKeepWritingOrderAndAChunkWrittenAgainIsReplaced() {
        int[] changes = { 0 };
        EditRecords records = new EditRecords(() -> changes[0]++);
        PartRecorder recorder = records.recorder(0);
        recorder.beginChunk(3, 4);
        recorder.record(3, 4, FLOOR, 70, Transform.ROTATE_NONE, 0, 0);
        recorder.record(3, 4, STREET, 64, Transform.ROTATE_90, 2, 3);
        assertEquals(
            2,
            records.at(0, 3, 4)
                .size());
        assertEquals(
            FLOOR,
            records.at(0, 3, 4)
                .get(0)
                .part(),
            "writing order is kept");
        assertEquals(
            Transform.ROTATE_90,
            records.at(0, 3, 4)
                .get(1)
                .transform());
        // The same chunk written again (a regenerated chunk) replaces its list instead of doubling it.
        recorder.beginChunk(3, 4);
        recorder.record(3, 4, STREET, 64, Transform.ROTATE_NONE, 0, 0);
        assertEquals(
            1,
            records.at(0, 3, 4)
                .size());
        assertTrue(changes[0] > 0, "every change marks the data for saving");
    }

    @Test
    public void dimensionsAndChunksAreApart() {
        EditRecords records = new EditRecords(() -> {});
        records.add(0, 1, 1, new PartRecord(FLOOR, 70, Transform.ROTATE_NONE, 0, 0));
        records.add(-1, 1, 1, new PartRecord(STREET, 64, Transform.ROTATE_NONE, 0, 0));
        records.add(0, 1, 2, new PartRecord(STREET, 64, Transform.ROTATE_NONE, 0, 0));
        assertEquals(
            FLOOR,
            records.at(0, 1, 1)
                .get(0)
                .part());
        assertEquals(
            STREET,
            records.at(-1, 1, 1)
                .get(0)
                .part());
        assertTrue(
            records.at(7, 1, 1)
                .isEmpty());
        List<String> order = new ArrayList<>();
        for (EditRecords.Entry entry : records.entries()) {
            order.add(entry.dimension() + "/" + entry.chunkX() + "/" + entry.chunkZ());
        }
        assertEquals("[-1/1/1, 0/1/1, 0/1/2]", order.toString(), "entries come in a stable order for saving");
    }

    @Test
    public void spansIsUpstreamsHalfOpenRange() {
        PartRecord record = new PartRecord(FLOOR, 70, Transform.ROTATE_NONE, 0, 0);
        assertTrue(record.spans(70, 6));
        assertTrue(record.spans(75, 6));
        assertFalse(record.spans(76, 6));
        assertFalse(record.spans(69, 6));
    }

    @Test
    public void recordingChangesNoGeneratedBlockAndRecordsTheChunksParts() {
        LayoutCityPlan plain = CityFixtures.plan();
        LayoutCityPlan recorded = CityFixtures.plan();
        EditRecords records = new EditRecords(() -> {});
        recorded.context()
            .recorder(records.recorder(0));
        int[] building = CityFixtures.first(
            12,
            (x, z) -> plain.planner()
                .plan(x, z)
                .hasBuilding());
        int[] street = CityFixtures.first(
            12,
            (x, z) -> plain.planner()
                .plan(x, z)
                .isCity()
                && !plain.planner()
                    .plan(x, z)
                    .hasBuilding());
        for (int[] chunk : new int[][] { building, street }) {
            assertEquals(
                ChunkBufferDigest.sha256(CityFixtures.generate(plain, chunk[0], chunk[1])),
                ChunkBufferDigest.sha256(CityFixtures.generate(recorded, chunk[0], chunk[1])),
                "recording must not change the chunk " + chunk[0] + "," + chunk[1]);
        }
        com.tuxprogrammer.lostcities.layout.ChunkPlan plan = plain.planner()
            .plan(building[0], building[1]);
        List<PartRecord> parts = records.at(0, building[0], building[1]);
        assertTrue(parts.size() >= plan.floors() + plan.cellars() + 1, "one record per level at least: " + parts);
        assertEquals(
            AssetId.parse(plan.floorPart(-plan.cellars())),
            parts.get(0)
                .part(),
            "the lowest level is written first");
        assertEquals(
            plan.buildingBottomHeight(),
            parts.get(0)
                .y());
    }
}
