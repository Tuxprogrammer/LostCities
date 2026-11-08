package com.tuxprogrammer.lostcities.asset.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.TestAssets;
import com.tuxprogrammer.lostcities.layout.FloorHeights;

/**
 * The shopping mall at a taller pitch (decision log D-mall-1). Its ground storey is two parts tall ({@code _0} and
 * {@code _1}, one 12-block atrium with an escalator from the ground to the mezzanine); those two keep upstream's 6 rows
 * each, so the escalator, its cobblestone supports and the atrium windows stand as upstream has them. The mezzanine
 * ({@code _2}) and every floor above take the world's pitch, and every quarter of a floor repeats the same row, so the
 * window bands line up across the four chunks.
 */
public class ShoppingMallTest {

    private static final String[] QUARTERS = { "00", "01", "10", "11" };

    private static AssetSet set;

    @BeforeAll
    public static void load() {
        set = TestAssets.bundled(TestAssets.pack(TestAssets.blockData()));
    }

    private static Part part(String name) {
        Part part = set.part(AssetId.of("lostcities", name));
        assertNotNull(part, name);
        return part;
    }

    @Test
    public void atriumKeepsUpstreamHeightAndUpperFloorsTakeThePitch() {
        for (String variant : new String[] { "shopping", "shopping_open" }) {
            for (String quarter : QUARTERS) {
                Building building = set.building(AssetId.of("lostcities", variant + quarter));
                assertNotNull(building, variant + quarter);
                for (int pitch = 6; pitch <= 10; pitch++) {
                    FloorHeights h = FloorHeights
                        .of(pitch, building.floorHeight(), building.groundFloorHeight(), building.groundFloors());
                    String where = variant + quarter + " pitch " + pitch;
                    assertEquals(6, h.height(0), where);
                    assertEquals(6, h.height(1), where);
                    assertEquals(12, h.offset(2), "the mezzanine slab stays at upstream's height: " + where);
                    assertEquals(pitch, h.height(2), where);
                    assertEquals(pitch, h.height(3), where);
                }
            }
        }
    }

    @Test
    public void everyQuarterOfAnUpperFloorRepeatsTheWindowRow() {
        List<String> upper = new ArrayList<>();
        for (String quarter : QUARTERS) {
            upper.add("shopping" + quarter + "_2");
            upper.add("shopping" + quarter + "_r1");
            upper.add("shopping_open" + quarter + "_2");
        }
        for (String name : upper) {
            // Row 3 is the window band of the east quarters; repeating it makes the windows taller, not misplaced.
            assertEquals(3, FloorStretch.repeatRow(part(name)), name);
        }
        Part east = part("shopping10_2");
        assertEquals('#', east.get(15, 2, 3));
        assertEquals('a', east.get(15, 3, 3));
        assertEquals('#', east.get(15, 4, 3));
        Part stretched = east.stretched(2, FloorStretch.repeatRow(east));
        for (int y = 3; y <= 5; y++) {
            assertEquals('a', stretched.get(15, y, 3), "a window 3 blocks tall at y " + y);
        }
        assertEquals('#', stretched.get(15, 6, 3));
    }

    @Test
    public void mezzanineStairStaysOneRunIntoTheFloorAbove() {
        // shopping11_2 climbs from the mezzanine through its ceiling into shopping11_r1.
        for (int extra = 0; extra <= 2; extra++) {
            List<Part> parts = new ArrayList<>();
            parts.add(part("shopping11_2"));
            parts.add(part("shopping11_r1"));
            List<int[]> copies = new ArrayList<>();
            for (Part p : parts) {
                copies.add(StairContinuityTest.copies(p, extra));
            }
            assertNull(
                StairContinuityTest.brokenAcross(parts, copies, Part.stacked(parts, copies)),
                "pitch " + (6 + extra));
        }
    }

    @Test
    public void stretchRowOutsideThePartIsIgnored() {
        Part plain = part("shopping01_0");
        assertNull(plain.metaInteger(Part.MetaKey.STRETCHROW));
        assertEquals(4, FloorStretch.repeatRow(plain), "unpinned parts keep the emptiest row");
    }
}
