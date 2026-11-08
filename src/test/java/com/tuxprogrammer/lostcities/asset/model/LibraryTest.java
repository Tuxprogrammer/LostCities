package com.tuxprogrammer.lostcities.asset.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.TestAssets;

/**
 * The library at a taller pitch (decision log D-library-1). Every floor part of its four quarters repeats row 3, the
 * top row of the 3-block window band and of the bookshelf walls, so the windows grow taller in one level band across
 * all four chunks and the shelves grow with them. Before the pin, {@code library11_2} repeated its window row while
 * the other parts repeated the solid band above it, so its windows sat a block off from its neighbors'.
 */
public class LibraryTest {

    private static final List<String> FLOOR_PARTS = Arrays.asList(
        "library00_1",
        "library01_1",
        "library01_2",
        "library01_3",
        "library10_1",
        "library10_2",
        "library10_3",
        "library10_4",
        "library10_5",
        "library11_1",
        "library11_2");

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
    public void everyFloorPartRepeatsTheWindowRow() {
        for (String name : FLOOR_PARTS) {
            assertEquals(3, FloorStretch.repeatRow(part(name)), name);
        }
    }

    @Test
    public void windowsGrowInOneBand() {
        for (String name : FLOOR_PARTS) {
            Part p = part(name);
            Part stretched = p.stretched(2, FloorStretch.repeatRow(p));
            // The west wall (x 0) of a west quarter or the east wall (x 15) of an east one; row 0 is the slab.
            int x = name.charAt(7) == '0' ? 0 : 15;
            StringBuilder column = new StringBuilder();
            for (int y = 0; y < stretched.height(); y++) {
                boolean glass = false;
                for (int zz = 0; zz < stretched.zsize(); zz++) {
                    glass |= stretched.get(x, y, zz) == 'a';
                }
                column.append(glass ? 'a' : '#');
            }
            assertEquals("#aaaaa##", column.toString(), name);
        }
    }

    @Test
    public void stairStaysOneRunIntoTheFloorAbove() {
        for (int extra = 0; extra <= 2; extra++) {
            List<Part> parts = Arrays.asList(part("library00_1"), part("library00_1"));
            List<int[]> copies = new ArrayList<>();
            for (Part p : parts) {
                copies.add(StairContinuityTest.copies(p, extra));
            }
            assertNull(
                StairContinuityTest.brokenAcross(parts, copies, Part.stacked(parts, copies)),
                "pitch " + (6 + extra));
        }
    }
}
