package com.tuxprogrammer.lostcities.asset.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
 *
 * <p>
 * Runs that climb from one floor part into the next (upstream's escalators) stay one run too (decision log
 * D-stair-xfloor-1): for every pair of parts a building may stack, at every height the profile or the building gives
 * each, a run that leaves the top of the lower part still meets its upper half.
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

    /** The parts a building places as its roof: the references marked as the top. */
    private static Set<Part> topParts(Building building) {
        Set<Part> out = new LinkedHashSet<>();
        for (List<Building.PartRef> refs : Arrays.asList(building.parts(), building.parts2())) {
            for (Building.PartRef ref : refs) {
                if (Boolean.TRUE.equals(
                    ref.test()
                        .top())) {
                    out.add(ref.part());
                }
            }
        }
        return out;
    }

    /** How many times each row of a floor part is written at {@code extra} more (or, below 0, fewer) rows. */
    static int[] copies(Part part, int extra) {
        if (extra > 0) {
            return FloorStretch.copies(part.height(), extra, FloorStretch.repeatRow(part));
        }
        if (extra < 0) {
            return FloorStretch.copies(part.height(), FloorStretch.dropRows(part, -extra));
        }
        return FloorStretch.copies(part.height(), 0, -1);
    }

    /** The slices of parts stacked bottom first, and per original row of the stack, whether it is a part's top. */
    private static List<String> concat(List<Part> parts, boolean[] top) {
        List<String> out = new ArrayList<>();
        for (Part part : parts) {
            out.addAll(slices(part));
            if (top != null) {
                top[out.size() - 1] = true;
            }
        }
        return out;
    }

    /**
     * Whether every run that crosses from one part of {@code before} into the next still rises one block per step
     * when the parts are written {@code copies} times per row and placed as {@code after}.
     *
     * @return the first broken run, or null
     */
    static String brokenAcross(List<Part> before, List<int[]> copies, List<Part> after) {
        int h = 0;
        for (Part part : before) {
            h += part.height();
        }
        boolean[] top = new boolean[h];
        List<String> original = concat(before, top);
        List<String> placed = concat(after, null);
        int xsize = before.get(0)
            .xsize();
        int zsize = before.get(0)
            .zsize();
        int[] cp = new int[h];
        int[] lo = new int[h];
        for (int i = 0, y = 0, ny = 0; i < before.size(); i++) {
            for (int c : copies.get(i)) {
                cp[y] = c;
                lo[y++] = ny;
                ny += c;
            }
        }
        for (FloorStretch.Chain chain : FloorStretch.chains(original, xsize, zsize, 2, top)) {
            boolean crosses = false;
            int added = 0;
            for (int y = chain.firstY; y < chain.lastY(); y++) {
                crosses |= top[y];
            }
            if (!crosses) {
                continue;
            }
            for (int y = chain.firstY; y <= chain.lastY(); y++) {
                added += Math.max(0, cp[y] - 1);
            }
            int last = -1;
            for (int k = 0; k < chain.steps; k++) {
                int x = chain.x + k * chain.dx;
                int z = chain.z + k * chain.dz;
                int y = chain.firstY + k;
                int t = -1;
                if (cp[y] > 0) {
                    int from = Math.min(placed.size() - 1, lo[y] + cp[y] - 1 + added);
                    for (int ny = from; ny >= 0; ny--) {
                        if (placed.get(ny)
                            .charAt(z * xsize + x) == chain.c) {
                            t = ny;
                            break;
                        }
                    }
                }
                if (t < 0 || (k > 0 && t - last != 1)) {
                    return "'" + chain.c + "' run at " + chain.x + "," + chain.firstY + "," + chain.z + " step " + k;
                }
                last = t;
            }
        }
        return null;
    }

    /** The extra rows a floor of this building may take: every pitch a world may pin, and the building's own. */
    private static Set<Integer> extras(Building building) {
        Set<Integer> out = new TreeSet<>();
        for (int extra = 0; extra <= MAX_EXTRA; extra++) {
            out.add(extra);
        }
        for (int height : new int[] { building.floorHeight(), building.groundFloorHeight() }) {
            if (height != Building.UNSET) {
                out.add(height - 6);
            }
        }
        return out;
    }

    /** Lays out a stack the way the old per-part stretch did, each part on its own. */
    private static List<Part> perPart(List<Part> parts, List<int[]> copies) {
        List<Part> out = new ArrayList<>();
        for (int i = 0; i < parts.size(); i++) {
            out.add(
                Part.stacked(Collections.singletonList(parts.get(i)), Collections.singletonList(copies.get(i)))
                    .get(0));
        }
        return out;
    }

    /**
     * Checks every pair of parts a building may stack (a floor under a floor, or under the roof) at every pair of
     * heights, and returns the broken ones.
     *
     * @param joined whether to lay the pair out as one stack, as the writer does, or each part on its own
     */
    private static Set<String> crossFailures(boolean joined) {
        Set<String> failures = new TreeSet<>();
        for (AssetId id : set.ids(AssetType.BUILDINGS)) {
            Building building = set.building(id);
            Set<Integer> extras = extras(building);
            List<Part> uppers = new ArrayList<>(floorParts(building));
            uppers.addAll(topParts(building));
            for (Part lower : floorParts(building)) {
                for (Part upper : uppers) {
                    if (upper.xsize() != lower.xsize() || upper.zsize() != lower.zsize()) {
                        continue;
                    }
                    boolean roof = !floorParts(building).contains(upper);
                    for (int lowerExtra : extras) {
                        for (int upperExtra : roof ? Collections.singleton(0) : extras) {
                            List<Part> parts = Arrays.asList(lower, upper);
                            List<int[]> copies = Arrays.asList(copies(lower, lowerExtra), copies(upper, upperExtra));
                            List<Part> after = joined ? Part.stacked(parts, copies) : perPart(parts, copies);
                            String why = brokenAcross(parts, copies, after);
                            if (why != null) {
                                failures.add(
                                    id.path() + " " + lower.id()
                                        .path()
                                        + " +"
                                        + lowerExtra
                                        + " / "
                                        + upper.id()
                                            .path()
                                        + " +"
                                        + upperExtra
                                        + ": "
                                        + why);
                            }
                        }
                    }
                }
            }
        }
        return failures;
    }

    @Test
    public void everyRunAcrossFloorsStaysContinuous() {
        Set<String> failures = crossFailures(true);
        assertTrue(failures.isEmpty(), failures.size() + " split cross-floor runs:\n" + String.join("\n", failures));
    }

    /**
     * The runs the old per-part stretch split (D-stair-xfloor-1), kept as a record: laid out one part at a time, the
     * {@code shopping00} escalator breaks at the floor-1 slab.
     */
    @Test
    public void perPartStretchSplitsTheEscalator() {
        Set<String> failures = crossFailures(false);
        StringBuilder text = new StringBuilder();
        for (String failure : failures) {
            text.append(failure)
                .append('\n');
        }
        try {
            java.nio.file.Path out = java.nio.file.Paths.get("build", "reports", "stair-xfloor-before.txt");
            java.nio.file.Files.createDirectories(out.getParent());
            java.nio.file.Files.write(out, text.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (java.io.IOException e) {
            // The record is a convenience; the assertion below is the test.
        }
        assertTrue(
            failures.stream()
                .anyMatch(f -> f.startsWith("shopping00 shopping00_0 +2 / shopping00_1 +2")),
            "the per-part stretch splits shopping00 at pitch 8:\n" + text);
    }

    /**
     * The owner's report: the quartz escalator of {@code shopping00} climbs from floor 0 through floor 1 to the
     * mezzanine of floor 2 in one run at pitch 8, as at every other pitch.
     */
    @Test
    public void shopping00EscalatorClimbsThroughStretchedFloors() {
        Building shopping = set.building(AssetId.of("lostcities", "shopping00"));
        assertNotNull(shopping);
        List<Part> parts = new ArrayList<>();
        for (String name : new String[] { "shopping00_0", "shopping00_1", "shopping00_2" }) {
            parts.add(set.part(AssetId.of("lostcities", name)));
        }
        for (int extra = 0; extra <= MAX_EXTRA; extra++) {
            List<int[]> copies = new ArrayList<>();
            for (Part part : parts) {
                copies.add(copies(part, extra));
            }
            List<Part> after = Part.stacked(parts, copies);
            assertNull(brokenAcross(parts, copies, after), "pitch " + (6 + extra));
            // The top step stays where upstream has it: the last row of floor 1, at the far side of the chunk.
            Part floor1 = after.get(1);
            assertEquals(')', floor1.get(8, floor1.height() - 1, 15), "pitch " + (6 + extra));
            assertEquals(6 + extra, floor1.height());
        }
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
