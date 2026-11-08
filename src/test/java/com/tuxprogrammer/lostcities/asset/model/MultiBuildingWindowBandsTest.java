package com.tuxprogrammer.lostcities.asset.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.AssetType;
import com.tuxprogrammer.lostcities.asset.TestAssets;

/**
 * A multibuilding's quarters stretch alike (decision logs D-mall-1, D-library-1). Along each outer side of a
 * multibuilding, any two floor parts whose walls have the same bands at upstream's 6 rows (the same set of blocks in
 * each row: a solid row, a window row) must still have the same bands once stretched to a taller pitch. Otherwise one
 * chunk's windows grow at a different height from its neighbor's and the bands step at the chunk border.
 */
public class MultiBuildingWindowBandsTest {

    private static final int EXTRA = 2;

    private static AssetSet set;

    @BeforeAll
    public static void load() {
        set = TestAssets.bundled(TestAssets.pack(TestAssets.blockData()));
    }

    /** Returns, per row, the sorted blocks of one outer wall: N, S, W or E. */
    static List<String> bands(Part part, char side) {
        List<String> out = new ArrayList<>();
        for (int y = 0; y < part.height(); y++) {
            TreeSet<Character> chars = new TreeSet<>();
            int n = side == 'N' || side == 'S' ? part.xsize() : part.zsize();
            for (int i = 0; i < n; i++) {
                switch (side) {
                    case 'N':
                        chars.add(part.get(i, y, 0));
                        break;
                    case 'S':
                        chars.add(part.get(i, y, part.zsize() - 1));
                        break;
                    case 'W':
                        chars.add(part.get(0, y, i));
                        break;
                    default:
                        chars.add(part.get(part.xsize() - 1, y, i));
                }
            }
            out.add(chars.toString());
        }
        return out;
    }

    @Test
    public void outerWallBandsStayLevelAcrossQuarters() {
        List<String> failures = new ArrayList<>();
        for (AssetId id : set.ids(AssetType.MULTIBUILDINGS)) {
            MultiBuilding multi = set.multiBuilding(id);
            // side + unstretched bands -> the stretched bands and the part that first showed them
            Map<String, List<String>> seen = new HashMap<>();
            Map<String, AssetId> firstPart = new HashMap<>();
            for (int x = 0; x < multi.dimX(); x++) {
                for (int z = 0; z < multi.dimZ(); z++) {
                    Building building = multi.building(x, z);
                    if (building == null) {
                        continue;
                    }
                    StringBuilder sides = new StringBuilder();
                    if (z == 0) sides.append('N');
                    if (z == multi.dimZ() - 1) sides.append('S');
                    if (x == 0) sides.append('W');
                    if (x == multi.dimX() - 1) sides.append('E');
                    for (Building.PartRef ref : building.parts()) {
                        Part part = ref.part();
                        Integer floor = ref.test()
                            .floor();
                        if (Boolean.TRUE.equals(
                            ref.test()
                                .top())
                            || part.height() != 6
                            || (floor != null && floor >= 0
                                && floor < building.groundFloors()
                                && building.groundFloorHeight() == 6)) {
                            continue;
                        }
                        Part stretched = part.stretched(EXTRA, FloorStretch.repeatRow(part));
                        for (char side : sides.toString()
                            .toCharArray()) {
                            String key = side + " " + bands(part, side);
                            List<String> after = bands(stretched, side);
                            List<String> before = seen.get(key);
                            if (before == null) {
                                seen.put(key, after);
                                firstPart.put(key, part.id());
                            } else if (!before.equals(after)) {
                                failures.add(
                                    id + " side "
                                        + side
                                        + ": "
                                        + part.id()
                                        + " stretches to "
                                        + after
                                        + " but "
                                        + firstPart.get(key)
                                        + " to "
                                        + before);
                            }
                        }
                    }
                }
            }
        }
        assertTrue(failures.isEmpty(), String.join("\n", failures));
    }
}
