package com.tuxprogrammer.lostcities.asset.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.TestAssets;

/**
 * Pitch-8 floors (design record 0014): the 22 parts phase 1a measured at a median clear height of 3 get at least 4,
 * vertical runs (ladders) through the repeat row stay continuous, and building4's stair runs still join floor to
 * floor.
 */
public class FloorStretchTest {

    private static final int EXTRA = 2;
    private static AssetSet set;

    @BeforeAll
    public static void load() {
        set = TestAssets.bundled(TestAssets.pack(TestAssets.blockData()));
    }

    /** The 22 parts reference/p1-measure found at a median of 3 clear rows. */
    private static List<String> lowParts() {
        List<String> out = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            out.add("building1_" + i);
        }
        for (int b = 2; b <= 4; b++) {
            for (int i = 1; i <= 4; i++) {
                out.add("building" + b + "_" + i);
            }
        }
        out.add("highway_restaurant_parking");
        return out;
    }

    private static Part part(String path) {
        return set.part(AssetId.of("lostcities", path));
    }

    private static List<String> slices(Part part) {
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

    /** Median run of spaces from row 1 up over columns whose row 0 is a block and row 1 a space. */
    private static int medianClear(Part part) {
        List<Integer> runs = new ArrayList<>();
        for (int z = 0; z < part.zsize(); z++) {
            for (int x = 0; x < part.xsize(); x++) {
                if (part.get(x, 0, z) == ' ' || part.get(x, 1, z) != ' ') {
                    continue;
                }
                int run = 0;
                for (int y = 1; y < part.height() && part.get(x, y, z) == ' '; y++) {
                    run++;
                }
                runs.add(run);
            }
        }
        Collections.sort(runs);
        return runs.isEmpty() ? 0 : runs.get(runs.size() / 2);
    }

    @Test
    public void everyLowPartGetsFourClearRows() {
        List<String> parts = lowParts();
        assertEquals(22, parts.size());
        for (String path : parts) {
            Part part = part(path);
            Part tall = part.stretched(EXTRA, FloorStretch.repeatRow(part));
            assertEquals(part.height() + EXTRA, tall.height(), path);
            assertTrue(medianClear(tall) >= 4, path + " clear " + medianClear(tall));
        }
    }

    @Test
    public void verticalRunsThroughTheRepeatRowStayContinuous() {
        for (String path : lowParts()) {
            Part part = part(path);
            int row = FloorStretch.repeatRow(part);
            Part tall = part.stretched(EXTRA, row);
            List<FloorStretch.Chain> chains = FloorStretch.chains(slices(part), part.xsize(), part.zsize());
            for (int z = 0; z < part.zsize(); z++) {
                for (int x = 0; x < part.xsize(); x++) {
                    char c = part.get(x, row, z);
                    if (c == ' ' || onChain(chains, c) || underChain(part, chains, c)) {
                        continue;
                    }
                    int lo = row;
                    while (lo > 0 && part.get(x, lo - 1, z) == c) {
                        lo--;
                    }
                    int hi = row;
                    while (hi < part.height() - 1 && part.get(x, hi + 1, z) == c) {
                        hi++;
                    }
                    for (int y = lo; y <= hi + EXTRA; y++) {
                        assertEquals(c, tall.get(x, y, z), path + " at " + x + "," + y + "," + z);
                    }
                }
            }
        }
    }

    private static boolean onChain(List<FloorStretch.Chain> chains, char c) {
        for (FloorStretch.Chain chain : chains) {
            if (chain.c == c) {
                return true;
            }
        }
        return false;
    }

    /** Whether {@code c} is a stair run's support, which the relay moves on purpose. */
    private static boolean underChain(Part part, List<FloorStretch.Chain> chains, char c) {
        for (FloorStretch.Chain chain : chains) {
            for (int k = 1; k < chain.steps; k++) {
                int y = chain.firstY + k - 1;
                if (part.get(chain.x + k * chain.dx, y, chain.z + k * chain.dz) == c) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    public void building4StairsJoinFloorToFloor() {
        for (int i = 1; i <= 4; i++) {
            Part part = part("building4_" + i);
            Part tall = part.stretched(EXTRA, FloorStretch.repeatRow(part));
            List<FloorStretch.Chain> before = FloorStretch.chains(slices(part), part.xsize(), part.zsize());
            assertTrue(!before.isEmpty(), "building4_" + i + " has a stair run");
            List<FloorStretch.Chain> after = FloorStretch.chains(slices(tall), tall.xsize(), tall.zsize());
            int joined = 0;
            for (FloorStretch.Chain chain : after) {
                // From the floor row's first step to the top row, which meets the next floor's row 0 step.
                if (chain.firstY == 1 && chain.lastY() == tall.height() - 1) {
                    joined++;
                }
            }
            assertEquals(before.size(), joined, "building4_" + i + " runs that climb a whole stretched floor");
        }
    }

    @Test
    public void stretchedCopiesAreMemoizedAndZeroIsTheOriginal() {
        Part part = part("building1_1");
        int row = FloorStretch.repeatRow(part);
        assertSame(part, part.stretched(0, row));
        assertSame(part.stretched(EXTRA, row), part.stretched(EXTRA, row));
    }
}
