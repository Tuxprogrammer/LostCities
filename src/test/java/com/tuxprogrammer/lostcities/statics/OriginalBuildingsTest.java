package com.tuxprogrammer.lostcities.statics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetProblem;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.TestAssets;
import com.tuxprogrammer.lostcities.asset.block.BlockChoice;
import com.tuxprogrammer.lostcities.asset.model.Building;
import com.tuxprogrammer.lostcities.asset.model.Palette;
import com.tuxprogrammer.lostcities.asset.model.PaletteEntry;
import com.tuxprogrammer.lostcities.asset.model.Part;

/**
 * The original low-density buildings (city-realism Phase 2d, tools/asset-convert/originals.py): each loads from the
 * bundled set in the full pack and in vanilla alone, keeps its class's floor range, has no more than one cellar, and
 * every floor part is a 16 x 16, six-slice floor whose slice 3 is a clean repeatable row (walls, glass, air and
 * ladders, D-p2d-2), so the floor-pitch stretch can copy it, with at most two tile-entity blocks per floor.
 */
public class OriginalBuildingsTest {

    /** Name to {min floors, max floors} of its class: houses 1 to 2, low-rises 2 to 3. */
    private static final Map<String, int[]> CLASSES = new LinkedHashMap<>();

    static {
        CLASSES.put("nh_house_garden", new int[] { 1, 2 });
        CLASSES.put("nh_cottage", new int[] { 1, 2 });
        CLASSES.put("nh_corner_shop", new int[] { 2, 3 });
        CLASSES.put("nh_workshop", new int[] { 2, 3 });
        CLASSES.put("nh_walkup", new int[] { 2, 3 });
    }

    /** The style's wall character: the low-rises take the city's own wall block. */
    private static final char STYLE_WALL = '#';

    /** Registry paths (no namespace, no properties or metadata) a clean repeatable row may hold. */
    private static final List<String> CLEAN = Arrays.asList(
        "air",
        "bricks",
        "oak_planks",
        "spruce_planks",
        "log",
        "white_terracotta",
        "stone_bricks",
        "smooth_stone",
        "cobblestone",
        "light_gray_concrete",
        "red_wool",
        "glass",
        "glass_pane",
        "ladder");

    /** Registry paths of the tile-entity blocks the palette uses. */
    private static final List<String> TILE_ENTITIES = Arrays.asList("chest", "furnace");

    @Test
    public void everyOriginalBuildingLoadsInThePackAndInVanilla() {
        AssetSet pack = TestAssets.bundled(TestAssets.pack(TestAssets.blockData()));
        AssetSet vanilla = TestAssets.bundled(TestAssets.vanilla());
        for (AssetSet set : Arrays.asList(pack, vanilla)) {
            List<String> errors = new ArrayList<>();
            for (String line : TestAssets.lines(set, AssetProblem.Severity.ERROR)) {
                if (line.contains("nh_")) {
                    errors.add(line);
                }
            }
            assertEquals(new ArrayList<String>(), errors);
            for (String name : CLASSES.keySet()) {
                assertNotNull(set.building(AssetId.of("lostcities", name)), name);
            }
        }
    }

    @Test
    public void floorsCellarsAndPartsKeepTheRules() {
        AssetSet set = TestAssets.bundled(TestAssets.pack(TestAssets.blockData()));
        for (Map.Entry<String, int[]> e : CLASSES.entrySet()) {
            String name = e.getKey();
            Building building = set.building(AssetId.of("lostcities", name));
            assertTrue(building.minFloors() >= e.getValue()[0], name + " min floors");
            assertTrue(building.maxFloors() <= e.getValue()[1], name + " max floors");
            assertTrue(building.minFloors() <= building.maxFloors(), name);
            assertTrue(building.maxCellars() >= 0 && building.maxCellars() <= 1, name + " cellars");
            Palette palette = building.palette();
            assertNotNull(palette, name + " names its palette");
            boolean roof = false;
            for (Building.PartRef ref : building.parts()) {
                Part part = ref.part();
                if (Boolean.TRUE.equals(
                    ref.test()
                        .top())) {
                    roof = true;
                    continue;
                }
                String where = name + "/" + part.id();
                assertEquals(16, part.xsize(), where);
                assertEquals(16, part.zsize(), where);
                assertEquals(6, part.height(), where);
                int tileEntities = 0;
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        char c = part.get(x, 3, z);
                        assertTrue(
                            c == STYLE_WALL || allIn(palette, c, CLEAN),
                            where + ": slice 3 at " + x + "," + z + " holds '" + c + "', not a clean repeatable block");
                        for (int y = 0; y < part.height(); y++) {
                            char d = part.get(x, y, z);
                            if (d != STYLE_WALL && anyIn(palette, d, TILE_ENTITIES)) {
                                tileEntities++;
                            }
                        }
                    }
                }
                assertTrue(tileEntities <= 2, where + ": " + tileEntities + " tile entities");
            }
            assertTrue(roof, name + " has a roof part");
        }
    }

    private static boolean allIn(Palette palette, char c, List<String> names) {
        List<String> blocks = blocks(palette, c);
        return !blocks.isEmpty() && names.containsAll(blocks);
    }

    private static boolean anyIn(Palette palette, char c, List<String> names) {
        for (String block : blocks(palette, c)) {
            if (names.contains(block)) {
                return true;
            }
        }
        return false;
    }

    /** The registry paths a palette character can place, from each choice's source text. */
    private static List<String> blocks(Palette palette, char c) {
        PaletteEntry entry = palette.entries()
            .get(c);
        List<String> out = new ArrayList<>();
        if (entry == null) {
            return out;
        }
        List<BlockChoice> choices = new ArrayList<>();
        if (entry.kind() == PaletteEntry.Kind.BLOCK) {
            choices.add(entry.block());
        } else if (entry.kind() == PaletteEntry.Kind.RANDOM) {
            choices.addAll(
                entry.random()
                    .distinct());
        }
        for (BlockChoice choice : choices) {
            String source = choice.source();
            int bracket = source.indexOf('[');
            if (bracket >= 0) {
                source = source.substring(0, bracket);
            }
            String[] bits = source.split(":");
            out.add(bits[1]);
        }
        return out;
    }
}
