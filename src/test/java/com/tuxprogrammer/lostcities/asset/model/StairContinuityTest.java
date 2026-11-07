package com.tuxprogrammer.lostcities.asset.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.AssetType;
import com.tuxprogrammer.lostcities.asset.TestAssets;

/**
 * Stairs stay walkable when a floor part changes height (design record 0014, decision log D-floor-heights-1): every
 * diagonal run of a floor part (two steps or more, air above each step) still rises one block per step after the part
 * is stretched to every pitch a world may pin, or lowered to the height its building sets. A run that the stretch
 * splits into two runs joined by a vertical wall of stairs fails here.
 */
public class StairContinuityTest {

    /** Pitches a world may pin: 6 (no stretch) to 10. */
    private static final int MAX_EXTRA = 4;

    private static AssetSet set;

    @BeforeAll
    public static void load() {
        set = TestAssets.bundled(TestAssets.pack(TestAssets.blockData()));
    }

    static List<String> slices(Part part) {
        List<String> out = new ArrayList<>();
        for (int y = 0; y < part.height(); y++) {
            StringBuilder s = new StringBuilder();
            for (int z = 0; z < part.zsize(); z++) {
                for (int x = 0; x < part.xsize(); x++) {
                    s.append(part.get(x, y, z));
                }
            }
            out.add(s.toString());
        }
        return out;
    }

    /** The highest Y from {@code lo} to {@code hi} where the column holds {@code c}, or -1. */
    private static int top(Part part, int x, int z, int lo, int hi, char c) {
        for (int y = Math.min(hi, part.height() - 1); y >= Math.max(0, lo); y--) {
            if (part.get(x, y, z) == c) {
                return y;
            }
        }
        return -1;
    }

    /**
     * Whether every run of {@code before} still rises one block per step in {@code after}, a step at original row
     * {@code y} looked for from {@code y + lo} to {@code y + hi}.
     */
    private static String broken(Part before, Part after, int lo, int hi) {
        for (FloorStretch.Chain chain : FloorStretch.chains(slices(before), before.xsize(), before.zsize(), 2)) {
            int last = -1;
            for (int k = 0; k < chain.steps; k++) {
                int x = chain.x + k * chain.dx;
                int z = chain.z + k * chain.dz;
                int y = chain.firstY + k;
                int t = top(after, x, z, y + lo, y + hi, chain.c);
                if (t < 0 || (k > 0 && t - last != 1)) {
                    return "'" + chain.c + "' run at " + chain.x + "," + chain.firstY + "," + chain.z + " step " + k;
                }
                last = t;
            }
        }
        return null;
    }

    /** The floor parts of every building: every reference not marked as the top. */
    private static Set<Part> floorParts(Building building) {
        Set<Part> out = new LinkedHashSet<>();
        for (List<Building.PartRef> refs : java.util.Arrays.asList(building.parts(), building.parts2())) {
            for (Building.PartRef ref : refs) {
                if (!Boolean.TRUE.equals(
                    ref.test()
                        .top())
                    && ref.part()
                        .height() == 6) {
                    out.add(ref.part());
                }
            }
        }
        return out;
    }

    @Test
    public void everyStairRunStaysContinuousAtEveryPitch() {
        Set<String> failures = new TreeSet<>();
        for (AssetId id : set.ids(AssetType.BUILDINGS)) {
            Building building = set.building(id);
            for (Part part : floorParts(building)) {
                int row = FloorStretch.repeatRow(part);
                for (int extra = 1; extra <= MAX_EXTRA; extra++) {
                    String why = broken(part, part.stretched(extra, row), 0, extra);
                    if (why != null) {
                        failures.add(part.id() + " +" + extra + ": " + why);
                    }
                }
                for (int height : new int[] { building.floorHeight(), building.groundFloorHeight() }) {
                    if (height != Building.UNSET && height < 6) {
                        int fewer = 6 - height;
                        String why = broken(part, part.squeezed(FloorStretch.dropRows(part, fewer)), -fewer, 0);
                        if (why != null) {
                            failures.add(part.id() + " -" + fewer + ": " + why);
                        }
                    }
                }
            }
        }
        assertTrue(failures.isEmpty(), failures.size() + " split stair runs:\n" + String.join("\n", failures));
    }

    @Test
    public void lowerFloorsDropTheEmptiestRowAndKeepSlabs() {
        Part office = set.part(AssetId.of("lostcities", "nh_workshop_office"));
        int[] drop = FloorStretch.dropRows(office, 1);
        assertArrayEquals(new int[] { 4 }, drop);
        Part low = office.squeezed(drop);
        assertEquals(5, low.height());
        for (int y : new int[] { 0, 1, 2, 3 }) {
            assertEquals(slices(office).get(y), slices(low).get(y));
        }
        assertEquals(slices(office).get(5), slices(low).get(4));
        assertEquals(low, office.squeezed(drop), "memoized");
        assertEquals(0, FloorStretch.dropRows(office, 4).length, "a part keeps its slab, a clear row and a ceiling");
    }
}
