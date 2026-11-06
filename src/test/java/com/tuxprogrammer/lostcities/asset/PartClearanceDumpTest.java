package com.tuxprogrammer.lostcities.asset;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tuxprogrammer.lostcities.asset.block.MetalFlags;
import com.tuxprogrammer.lostcities.asset.model.Building;
import com.tuxprogrammer.lostcities.asset.model.Palette;
import com.tuxprogrammer.lostcities.asset.model.PaletteEntry;
import com.tuxprogrammer.lostcities.asset.model.Part;
import com.tuxprogrammer.lostcities.asset.model.Style;
import com.tuxprogrammer.lostcities.asset.palette.CompiledPalette;

/**
 * Research dumper, not a test of behavior: measures every bundled part's floor-to-ceiling clearance and writes
 * {@code build/p1-measure/clearance.csv} and {@code summary-data.md}. It runs only when the marker file
 * {@code build/p1-measure.run} exists, so the normal build skips it.
 *
 * <p>
 * Definitions. A <em>walkable column</em> is an (x, z) whose floor row (slice 0) is solid and whose slice 1 is clear.
 * <em>Clear</em> blocks are air, hard air, a space (leave alone) and the explicit stand-in list {@link #CLEAR}. A
 * column's <em>clear height</em> counts consecutive clear blocks from slice 1 up to the part's top. The
 * <em>headroom filler</em> is the first non-clear block above that run, when the run ends below the part's top.
 * Palette characters resolve through the standard style's first choice per group, then the building palette, then the
 * part palette; a character with several blocks is clear only when every option is clear.
 */
public class PartClearanceDumpTest {

    private static final Path MARKER = Paths.get("build", "p1-measure.run");
    private static final Path OUT = Paths.get("build", "p1-measure");

    /** Blocks you can stand in (registry path, without the namespace). */
    private static final Set<String> CLEAR = new HashSet<>(
        Arrays.asList(
            "air",
            "hard_air",
            "torch",
            "redstone_torch",
            "unlit_redstone_torch",
            "standing_sign",
            "wall_sign",
            "ladder",
            "flower_pot",
            "rail",
            "golden_rail",
            "detector_rail",
            "activator_rail",
            "stone_pressure_plate",
            "wooden_pressure_plate",
            "light_weighted_pressure_plate",
            "heavy_weighted_pressure_plate",
            "stone_button",
            "wooden_button",
            "lever",
            "tripwire",
            "tripwire_hook",
            "redstone_wire",
            "vine",
            "tallgrass",
            "deadbush",
            "yellow_flower",
            "red_flower",
            "brown_mushroom",
            "red_mushroom",
            "snow_layer",
            "skull",
            "sapling"));

    private static final String[] WALLISH = { "stone", "brick", "plank", "glass", "pane", "wool", "clay", "quartz",
        "sand", "cobble", "log", "iron_bars", "concrete", "dirt", "obsidian", "iron_block", "gold_block" };

    private static String path(String name) {
        return name.substring(name.indexOf(':') + 1);
    }

    private static boolean clearName(String name) {
        String p = path(name);
        return CLEAR.contains(p) || p.endsWith("carpet");
    }

    /** A wall or window block that can be repeated in a row. */
    private static boolean wallish(String name) {
        String p = path(name);
        if (p.contains("slab") || p.contains("stairs") || p.contains("lamp") || p.contains("glowstone")) {
            return false;
        }
        for (String w : WALLISH) {
            if (p.contains(w)) {
                return true;
            }
        }
        return false;
    }

    /** What one palette character is, for measuring. */
    private static final class Cell {

        final boolean clear;
        final boolean air;
        /** The name of the first non-clear option, or the first option when all are clear. */
        final String name;
        final boolean fixture;
        final boolean stairs;
        final boolean ladder;

        Cell(boolean clear, boolean air, String name, boolean fixture, boolean stairs, boolean ladder) {
            this.clear = clear;
            this.air = air;
            this.name = name;
            this.fixture = fixture;
            this.stairs = stairs;
            this.ladder = ladder;
        }
    }

    private static Cell cell(CompiledPalette cp, char c) {
        if (c == ' ') {
            return new Cell(true, true, "minecraft:air", false, false, false);
        }
        if (!cp.isDefined(c)) {
            return new Cell(false, false, "undefined:" + c, true, false, false);
        }
        boolean allClear = true;
        boolean allAir = true;
        boolean stairs = false;
        boolean ladder = false;
        String firstSolid = null;
        String first = null;
        for (BlockRef ref : cp.getAll(c)) {
            String n = ref.name();
            String p = path(n);
            if (first == null) {
                first = n;
            }
            allAir &= p.equals("air") || p.equals("hard_air");
            stairs |= p.contains("stairs");
            ladder |= p.equals("ladder");
            if (!clearName(n)) {
                allClear = false;
                if (firstSolid == null) {
                    firstSolid = n;
                }
            }
        }
        String name = firstSolid != null ? firstSolid : first != null ? first : "undefined:" + c;
        PaletteEntry info = cp.info(c);
        boolean hasInfo = info != null
            && (info.torch() || info.loot() != null || info.mob() != null || info.tag() != null);
        boolean fixture = hasInfo || (allClear && !allAir) || (!allClear && !wallish(name)) || stairs || ladder;
        return new Cell(allClear, allAir, name, fixture, stairs, ladder);
    }

    /** One part's measurements. */
    private static final class Row {

        String id;
        int height;
        String role;
        int floorUses;
        int cellarCapable;
        int topUses;
        int overlayUses;
        List<String> buildings = new ArrayList<>();
        int walkable;
        int min;
        int median;
        int max;
        double pctUnder4;
        String fillers = "";
        String ceilingShare = "";
        String repeatRow = "none";
        int stairCount;
        int ladderCount;
        String k90 = "-";
        String k100 = "-";
        String notes = "";
        int kNeed90 = -1;
        int kNeed100 = -1;
    }

    /**
     * Returns the clear height of every walkable column, optionally with row {@code repeatY} repeated {@code extra}
     * more times; collects the fillers of columns under 4 when {@code fillers} is given.
     */
    private static List<Integer> clearHeights(char[][][] grid, boolean[][] walk, int xs, int zs, int h, int repeatY,
        int extra, Map<Character, Cell> map, Map<String, Integer> fillers, List<String> fillerAt) {
        List<Integer> out = new ArrayList<>();
        int total = h + (repeatY > 0 ? extra : 0);
        for (int z = 0; z < zs; z++) {
            for (int x = 0; x < xs; x++) {
                if (!walk[x][z]) {
                    continue;
                }
                int run = 0;
                String filler = null;
                int fy = -1;
                for (int vy = 1; vy < total; vy++) {
                    int y = vy;
                    if (repeatY > 0 && vy > repeatY) {
                        y = vy <= repeatY + extra ? repeatY : vy - extra;
                    }
                    Cell cell = map.get(grid[x][z][y]);
                    if (cell.clear) {
                        run++;
                    } else {
                        filler = cell.name;
                        fy = y;
                        break;
                    }
                }
                out.add(run);
                if (fillers != null && filler != null && run < 4) {
                    fillers.merge(filler, 1, Integer::sum);
                    fillerAt.add(filler + "@" + fy);
                }
            }
        }
        return out;
    }

    private static double pctUnder(List<Integer> heights, int limit) {
        if (heights.isEmpty()) {
            return 0;
        }
        int n = 0;
        for (int v : heights) {
            if (v < limit) {
                n++;
            }
        }
        return 100.0 * n / heights.size();
    }

    @Test
    public void dumpPartClearance() throws IOException {
        Assumptions.assumeTrue(Files.exists(MARKER), "research dumper: create build/p1-measure.run to run it");
        AssetSet set = TestAssets.bundled(TestAssets.pack(TestAssets.blockData()));
        Style style = set.style(AssetId.parse("standard"));
        List<Palette> base = new ArrayList<>();
        for (List<Style.Choice> group : style.groups()) {
            base.add(
                group.get(0)
                    .palette());
        }

        Map<AssetId, Row> rows = new TreeMap<>();
        for (AssetId id : set.ids(AssetType.PARTS)) {
            Row r = new Row();
            r.id = id.path();
            rows.put(id, r);
        }
        for (AssetId bid : set.ids(AssetType.BUILDINGS)) {
            Building b = set.building(bid);
            JsonObject json = buildingJson(bid);
            for (int pass = 0; pass < 2; pass++) {
                String key = pass == 0 ? "parts" : "parts2";
                List<Building.PartRef> refs = pass == 0 ? b.parts() : b.parts2();
                JsonArray raw = json.has(key) ? json.getAsJsonArray(key) : new JsonArray();
                for (int i = 0; i < refs.size(); i++) {
                    Row r = rows.get(
                        refs.get(i)
                            .part()
                            .id());
                    JsonObject ref = i < raw.size() ? raw.get(i)
                        .getAsJsonObject() : new JsonObject();
                    boolean top = ref.has("top") && ref.get("top")
                        .getAsBoolean();
                    if (pass == 1) {
                        r.overlayUses++;
                    } else if (top) {
                        r.topUses++;
                    } else {
                        r.floorUses++;
                        if (b.maxCellars() != 0) {
                            r.cellarCapable++;
                        }
                        r.buildings.add(bid.path());
                    }
                }
            }
        }

        for (Map.Entry<AssetId, Row> e : rows.entrySet()) {
            Row r = e.getValue();
            Part part = set.part(e.getKey());
            r.height = part.height();
            r.role = r.floorUses > 0 ? "floor" : r.topUses > 0 ? "top" : r.overlayUses > 0 ? "overlay" : "unused";
            try {
                measure(set, base, part, r);
            } catch (RuntimeException ex) {
                r.notes += "measure failed: " + ex + "; ";
            }
        }

        Files.createDirectories(OUT);
        StringBuilder csv = new StringBuilder(
            "part,height,role,floor_cellar_buildings,cellar_capable_buildings,top_buildings,overlay_buildings,walkable,"
                + "clear_min,clear_median,clear_max,pct_under_4,top3_fillers,share_fillers_in_top3_rows,repeat_row_y,"
                + "stair_blocks,ladder_blocks,stretch_rows_for_90pct,stretch_rows_for_100pct,notes,buildings\n");
        for (Row r : rows.values()) {
            csv.append(
                String.join(
                    ",",
                    q(r.id),
                    "" + r.height,
                    r.role,
                    "" + r.floorUses,
                    "" + r.cellarCapable,
                    "" + r.topUses,
                    "" + r.overlayUses,
                    "" + r.walkable,
                    "" + r.min,
                    "" + r.median,
                    "" + r.max,
                    String.format("%.1f", r.pctUnder4),
                    q(r.fillers),
                    q(r.ceilingShare),
                    r.repeatRow,
                    "" + r.stairCount,
                    "" + r.ladderCount,
                    r.k90,
                    r.k100,
                    q(r.notes),
                    q(String.join(" ", r.buildings))))
                .append('\n');
        }
        Files.write(OUT.resolve("clearance.csv"), csv.toString().getBytes(StandardCharsets.UTF_8));

        int floorParts = 0;
        int noWalk = 0;
        int medianUnder4 = 0;
        int anyUnder4 = 0;
        int capUnder4 = 0;
        int withRow = 0;
        int withStairs = 0;
        int withLadder = 0;
        int withEither = 0;
        Map<Integer, Integer> k90Hist = new TreeMap<>();
        Map<Integer, Integer> k100Hist = new TreeMap<>();
        Map<Integer, Integer> heights = new TreeMap<>();
        Map<Integer, Integer> medians = new TreeMap<>();
        for (Row r : rows.values()) {
            if (!r.role.equals("floor")) {
                continue;
            }
            floorParts++;
            heights.merge(r.height, 1, Integer::sum);
            if (r.walkable == 0) {
                noWalk++;
                continue;
            }
            medians.merge(r.median, 1, Integer::sum);
            medianUnder4 += r.median < 4 ? 1 : 0;
            anyUnder4 += r.min < 4 ? 1 : 0;
            capUnder4 += r.height - 1 < 4 ? 1 : 0;
            withRow += r.repeatRow.equals("none") ? 0 : 1;
            withStairs += r.stairCount > 0 ? 1 : 0;
            withLadder += r.ladderCount > 0 ? 1 : 0;
            withEither += r.stairCount > 0 || r.ladderCount > 0 ? 1 : 0;
            k90Hist.merge(r.kNeed90, 1, Integer::sum);
            k100Hist.merge(r.kNeed100, 1, Integer::sum);
        }
        String md = "floorParts=" + floorParts + "\nnoWalkable=" + noWalk + "\nheightHistogram=" + heights
            + "\nmedianClearHistogram=" + medians + "\nmedianUnder4=" + medianUnder4 + "\nanyColumnUnder4=" + anyUnder4
            + "\ncapacityUnder4(height-1<4)=" + capUnder4 + "\nwithRepeatRow=" + withRow + "\nwithStairs=" + withStairs
            + "\nwithLadders=" + withLadder + "\nwithStairsOrLadders=" + withEither
            + "\nrowsFor90pct(-1=none)=" + k90Hist + "\nrowsFor100pct(-1=none)=" + k100Hist + "\n";
        Files.write(OUT.resolve("summary-data.md"), md.getBytes(StandardCharsets.UTF_8));
    }

    private static Map<String, Integer> sortByValue(Map<String, Integer> in) {
        List<Map.Entry<String, Integer>> list = new ArrayList<>(in.entrySet());
        list.sort((a, b) -> b.getValue() - a.getValue());
        Map<String, Integer> out = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : list) {
            out.put(e.getKey(), e.getValue());
        }
        return out;
    }

    private static String q(String s) {
        return "\"" + s.replace("\"", "'") + "\"";
    }

    private static JsonObject buildingJson(AssetId id) throws IOException {
        String resource = "/data/" + id.namespace() + "/lostcities/buildings/" + id.path() + ".json";
        try (InputStream in = PartClearanceDumpTest.class.getResourceAsStream(resource)) {
            if (in == null) {
                return new JsonObject();
            }
            JsonElement el = new JsonParser().parse(new StringReader(AssetSource.read(in)));
            return el.getAsJsonObject();
        }
    }

    private static void measure(AssetSet set, List<Palette> base, Part part, Row r) {
        List<Palette> layers = new ArrayList<>(base);
        if (!r.buildings.isEmpty()) {
            Building b = set.building(AssetId.of("lostcities", r.buildings.get(0)));
            if (b != null && b.palette() != null) {
                layers.add(b.palette());
            }
        }
        if (part.palette() != null) {
            layers.add(part.palette());
        }
        CompiledPalette cp = CompiledPalette.compile(MetalFlags.ALL_ON, layers);
        int xs = part.xsize();
        int zs = part.zsize();
        int h = part.height();
        char[][][] grid = new char[xs][zs][h];
        Map<Character, Cell> map = new LinkedHashMap<>();
        Map<Character, Integer> counts = new TreeMap<>();
        for (int x = 0; x < xs; x++) {
            for (int z = 0; z < zs; z++) {
                for (int y = 0; y < h; y++) {
                    char c = part.get(x, y, z);
                    grid[x][z][y] = c;
                    counts.merge(c, 1, Integer::sum);
                    if (!map.containsKey(c)) {
                        map.put(c, cell(cp, c));
                    }
                }
            }
        }
        boolean[][] walk = new boolean[xs][zs];
        int walkable = 0;
        for (int x = 0; x < xs; x++) {
            for (int z = 0; z < zs; z++) {
                if (!map.get(grid[x][z][0]).clear && h > 1 && map.get(grid[x][z][1]).clear) {
                    walk[x][z] = true;
                    walkable++;
                }
            }
        }
        r.walkable = walkable;
        for (Map.Entry<Character, Cell> e : map.entrySet()) {
            if (e.getValue().stairs) {
                r.stairCount += counts.get(e.getKey());
            }
            if (e.getValue().ladder) {
                r.ladderCount += counts.get(e.getKey());
            }
        }
        if (part.meta()
            .containsKey("z1")) {
            r.notes += "stair-opening meta z1/z2; ";
        }
        Set<String> undefined = new TreeSet<>();
        for (Cell c : map.values()) {
            if (c.name.startsWith("undefined:")) {
                undefined.add(c.name);
            }
        }
        if (!undefined.isEmpty()) {
            r.notes += "undefined chars " + undefined + "; ";
        }
        if (walkable == 0) {
            r.notes += "no walkable columns; ";
            return;
        }
        Map<String, Integer> fillers = new TreeMap<>();
        List<String> fillerAt = new ArrayList<>();
        List<Integer> heights = clearHeights(grid, walk, xs, zs, h, 0, 0, map, fillers, fillerAt);
        List<Integer> sorted = new ArrayList<>(heights);
        Collections.sort(sorted);
        r.min = sorted.get(0);
        r.max = sorted.get(sorted.size() - 1);
        r.median = sorted.get(sorted.size() / 2);
        r.pctUnder4 = pctUnder(heights, 4);
        Map<String, List<Integer>> ys = new TreeMap<>();
        int topRows = 0;
        for (String fa : fillerAt) {
            int at = fa.lastIndexOf('@');
            int y = Integer.parseInt(fa.substring(at + 1));
            ys.computeIfAbsent(fa.substring(0, at), k -> new ArrayList<>())
                .add(y);
            if (y >= h - 3) {
                topRows++;
            }
        }
        List<Map.Entry<String, Integer>> top = new ArrayList<>(sortByValue(fillers).entrySet());
        StringBuilder f = new StringBuilder();
        for (int i = 0; i < Math.min(3, top.size()); i++) {
            String name = top.get(i)
                .getKey();
            List<Integer> yy = ys.get(name);
            Collections.sort(yy);
            if (i > 0) {
                f.append("; ");
            }
            f.append(name)
                .append(" y")
                .append(yy.get(yy.size() / 2))
                .append(" x")
                .append(
                    top.get(i)
                        .getValue());
        }
        r.fillers = f.toString();
        r.ceilingShare = fillerAt.isEmpty() ? "" : String.format("%.0f%%", 100.0 * topRows / fillerAt.size());

        // repeatable row: only air and wall or window blocks, and air over at least half the walkable columns
        int bestY = -1;
        int bestClear = -1;
        for (int y = 1; y < h; y++) {
            boolean ok = true;
            int clearCols = 0;
            for (int x = 0; x < xs && ok; x++) {
                for (int z = 0; z < zs; z++) {
                    Cell c = map.get(grid[x][z][y]);
                    if (c.air) {
                        clearCols += walk[x][z] ? 1 : 0;
                    } else if (c.fixture || c.clear) {
                        ok = false;
                        break;
                    }
                }
            }
            if (ok && clearCols * 2 >= walkable && clearCols > bestClear) {
                bestClear = clearCols;
                bestY = y;
            }
        }
        if (bestY > 0) {
            r.repeatRow = Integer.toString(bestY);
            for (int k = 0; k <= 8; k++) {
                List<Integer> hs = clearHeights(grid, walk, xs, zs, h, bestY, k, map, null, null);
                double under = pctUnder(hs, 4);
                if (r.kNeed90 < 0 && under <= 10.0) {
                    r.kNeed90 = k;
                    r.k90 = Integer.toString(k);
                }
                if (r.kNeed100 < 0 && under == 0.0) {
                    r.kNeed100 = k;
                    r.k100 = Integer.toString(k);
                }
            }
        }
    }
}
