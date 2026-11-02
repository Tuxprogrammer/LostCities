package com.tuxprogrammer.lostcities.layout.street;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.ParityData;
import com.tuxprogrammer.lostcities.layout.random.DimensionKey;

/**
 * The ported street field reproduces upstream's {@code HierarchicalStreetPlanner} for the same seed and dimension ID
 * (P04-1): the primary grid, the raw road class of every chunk in a 96 × 96 window, and the full street info of a
 * sample of chunks, for five recorded cases (tools/upstream-parity).
 */
public class HierarchicalStreetPlannerTest {

    private static final StreetPlannerSettings VARIANT = new StreetPlannerSettings(
        12,
        9,
        .3f,
        3,
        1,
        3,
        0,
        1,
        3,
        2,
        .9f,
        1,
        7);

    private static final Pattern INFO = Pattern.compile(
        "^info (-?\\d+) (-?\\d+) (\\w+) (\\w+) (\\w+) (\\w+) (\\w+) (-?\\d+) (-?\\d+) (-?\\d+) (-?\\d+) (-?\\d+)"
            + " (-?\\d+) ([0-9a-f]+) (\\[[^\\]]*\\]) (\\[[^\\]]*\\]) (.*)$");

    @Test
    public void defaultSettingsSeed0() throws Exception {
        check("streets_default_seed0.txt", 0L, DimensionKey.OVERWORLD, StreetPlannerSettings.DEFAULTS);
    }

    @Test
    public void defaultSettingsSeed42() throws Exception {
        check("streets_default_seed42.txt", 42L, DimensionKey.OVERWORLD, StreetPlannerSettings.DEFAULTS);
    }

    @Test
    public void defaultSettingsNegativeSeed() throws Exception {
        check(
            "streets_default_seedneg.txt",
            -6543210987654321L,
            DimensionKey.OVERWORLD,
            StreetPlannerSettings.DEFAULTS);
    }

    @Test
    public void anotherDimensionHashesItsOwnId() throws Exception {
        check("streets_default_seed42_dim7.txt", 42L, DimensionKey.forDimension(7), StreetPlannerSettings.DEFAULTS);
    }

    @Test
    public void nonDefaultSettings() throws Exception {
        check("streets_variant_seed42.txt", 42L, DimensionKey.OVERWORLD, VARIANT);
    }

    @Test
    public void primaryRoadsRunTheLengthOfTheDimension() {
        HierarchicalStreetPlanner planner = new HierarchicalStreetPlanner(
            7L,
            DimensionKey.OVERWORLD,
            StreetPlannerSettings.DEFAULTS);
        int x = planner.primaryOffsetX();
        assertTrue(planner.isVerticalPrimary(x), "candidate 0 is always forced");
        for (int z = -5000; z <= 5000; z += 997) {
            assertEquals(PlannedRoadType.PRIMARY, planner.getRoadType(x, z));
        }
    }

    @Test
    public void tertiaryRoadsAreDeadEnds() {
        HierarchicalStreetPlanner planner = new HierarchicalStreetPlanner(
            3L,
            DimensionKey.OVERWORLD,
            StreetPlannerSettings.DEFAULTS);
        int checked = 0;
        for (int x = -60; x < 60; x++) {
            for (int z = -60; z < 60; z++) {
                PlannedStreetInfo info = planner.getStreetInfo(x, z);
                if (info.roadType() != PlannedRoadType.TERTIARY) {
                    continue;
                }
                TertiaryRoadSegment segment = info.tertiarySegment();
                int endX = segment.originX() + segment.direction()
                    .stepX() * (segment.length() + 1);
                int endZ = segment.originZ() + segment.direction()
                    .stepZ() * (segment.length() + 1);
                assertNotEquals(PlannedRoadType.PRIMARY, planner.getRoadType(endX, endZ));
                checked++;
            }
        }
        assertTrue(checked > 0, "no tertiary road in the window");
    }

    private static void check(String file, long seed, DimensionKey dimension, StreetPlannerSettings settings)
        throws Exception {
        HierarchicalStreetPlanner planner = new HierarchicalStreetPlanner(seed, dimension, settings);
        List<String> lines = ParityData.lines(file);
        int rows = 0;
        int infos = 0;
        for (String line : lines) {
            String[] f = line.split(" ");
            switch (f[0]) {
                case "offsets":
                    assertEquals(Integer.parseInt(f[1]), planner.primaryOffsetX(), "offset X");
                    assertEquals(Integer.parseInt(f[2]), planner.primaryOffsetZ(), "offset Z");
                    break;
                case "vertical":
                    assertEquals(line, primaries("vertical", planner, true));
                    break;
                case "horizontal":
                    assertEquals(line, primaries("horizontal", planner, false));
                    break;
                case "row":
                    int z = Integer.parseInt(f[1]);
                    StringBuilder actual = new StringBuilder();
                    for (int x = -48; x < 48; x++) {
                        actual.append(
                            planner.getRoadType(x, z)
                                .name()
                                .charAt(0));
                    }
                    assertEquals(f[2], actual.toString(), file + " row " + z);
                    rows++;
                    break;
                case "info":
                    checkInfo(planner, line, file);
                    infos++;
                    break;
                default:
                    throw new IllegalStateException("unknown record " + f[0]);
            }
        }
        assertEquals(96, rows);
        assertTrue(infos > 50);
    }

    private static String primaries(String label, HierarchicalStreetPlanner planner, boolean vertical) {
        StringBuilder sb = new StringBuilder(label);
        for (int i = -100; i <= 100; i++) {
            if (vertical ? planner.isVerticalPrimary(i) : planner.isHorizontalPrimary(i)) {
                sb.append(' ')
                    .append(i);
            }
        }
        return sb.toString();
    }

    private static void checkInfo(HierarchicalStreetPlanner planner, String line, String file) {
        Matcher m = INFO.matcher(line);
        assertTrue(m.matches(), "unparsable record: " + line);
        int x = Integer.parseInt(m.group(1));
        int z = Integer.parseInt(m.group(2));
        PlannedStreetInfo s = planner.getStreetInfo(x, z);
        BlockLayout b = s.block();
        String where = file + " info " + x + "," + z;
        assertEquals(
            m.group(3),
            s.roadType()
                .name(),
            where);
        assertEquals(Boolean.parseBoolean(m.group(4)), s.connects(RoadDirection.NORTH), where);
        assertEquals(Boolean.parseBoolean(m.group(5)), s.connects(RoadDirection.SOUTH), where);
        assertEquals(Boolean.parseBoolean(m.group(6)), s.connects(RoadDirection.WEST), where);
        assertEquals(Boolean.parseBoolean(m.group(7)), s.connects(RoadDirection.EAST), where);
        assertEquals(Integer.parseInt(m.group(8)), b.blockX(), where);
        assertEquals(Integer.parseInt(m.group(9)), b.blockZ(), where);
        assertEquals(Integer.parseInt(m.group(10)), b.westX(), where);
        assertEquals(Integer.parseInt(m.group(11)), b.northZ(), where);
        assertEquals(Integer.parseInt(m.group(12)), b.eastX(), where);
        assertEquals(Integer.parseInt(m.group(13)), b.southZ(), where);
        assertEquals(ParityData.doubleBits(m.group(14)), b.density(), where);
        assertEquals(
            m.group(15),
            b.secondaryX()
                .toString(),
            where);
        assertEquals(
            m.group(16),
            b.secondaryZ()
                .toString(),
            where);
        if ("-".equals(m.group(17))) {
            assertNull(s.tertiarySegment(), where);
        } else {
            assertEquals(
                m.group(17),
                s.tertiarySegment()
                    .toString(),
                where);
        }
    }
}
