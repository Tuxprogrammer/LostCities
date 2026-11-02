package com.tuxprogrammer.lostcities.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.transform.Transform;
import com.tuxprogrammer.lostcities.editor.EditRecords;
import com.tuxprogrammer.lostcities.editor.PartRecord;

/** {@link LocatePartSubcommand}: recorded parts near the player, one name form, seven hits overall. */
public class LocatePartSubcommandTest {

    private static final AssetId STREET = AssetId.parse("lostcities:street_basic_1");

    @Test
    public void doesNotReproduceCORE30_aBundledPartIsFoundByItsShortOrFullName() {
        EditRecords records = new EditRecords(() -> {});
        records.add(0, 2, 3, new PartRecord(STREET, 65, Transform.ROTATE_NONE, 0, 0));
        List<int[]> byShort = LocatePartSubcommand.find(records, 0, AssetId.parse("street_basic_1"), 0, 0);
        List<int[]> byFull = LocatePartSubcommand.find(records, 0, AssetId.parse("lostcities:street_basic_1"), 0, 0);
        assertEquals(1, byShort.size());
        assertEquals(1, byFull.size());
        assertEquals(2 * 16 + 8, byShort.get(0)[0]);
        assertEquals(65, byShort.get(0)[1]);
        assertEquals(3 * 16 + 8, byShort.get(0)[2]);
        assertTrue(
            LocatePartSubcommand.find(records, -1, STREET, 0, 0)
                .isEmpty(),
            "another dimension's records are not searched");
    }

    @Test
    public void doesNotReproduceCORE31_theLimitStopsTheWholeSearch() {
        EditRecords records = new EditRecords(() -> {});
        // Ten records in one chunk, and more in the next: upstream's limit only left the inner loop.
        for (int i = 0; i < 10; i++) {
            records.add(0, 0, 0, new PartRecord(STREET, 60 + i, Transform.ROTATE_NONE, 0, 0));
        }
        for (int x = 1; x < 5; x++) {
            records.add(0, x, 0, new PartRecord(STREET, 64, Transform.ROTATE_NONE, 0, 0));
        }
        List<int[]> hits = LocatePartSubcommand.find(records, 0, STREET, 0, 0);
        assertEquals(LocatePartSubcommand.MAX_HITS, hits.size());
        for (int[] hit : hits) {
            assertEquals(8, hit[0], "every hit is in the nearest chunk");
        }
    }
}
