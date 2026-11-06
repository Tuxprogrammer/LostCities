package com.tuxprogrammer.lostcities.worldgen.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.TestAssets;
import com.tuxprogrammer.lostcities.asset.block.MetalFlags;
import com.tuxprogrammer.lostcities.asset.match.BiomeDescriptor;
import com.tuxprogrammer.lostcities.asset.palette.CompiledPalette;
import com.tuxprogrammer.lostcities.asset.transform.Transform;
import com.tuxprogrammer.lostcities.layout.LayoutLog;
import com.tuxprogrammer.lostcities.worldgen.city.furnish.FurnishItem;
import com.tuxprogrammer.lostcities.worldgen.city.furnish.FurnishPalette;
import com.tuxprogrammer.lostcities.worldgen.city.furnish.FurnishPalettes;
import com.tuxprogrammer.lostcities.worldgen.city.furnish.RoomFit;
import com.tuxprogrammer.lostcities.worldgen.city.furnish.RoomKind;
import com.tuxprogrammer.lostcities.worldgen.plan.PopulationJob;

/** Lane 6c and 6d: building types resolve palettes, and chests and spawners keep to fitting, thinned rooms. */
public class RoomRulesTest {

    private static final AssetSet SET = TestAssets.loadFiles(
        "lostcities/palettes/p.json",
        "{\"palette\": [{\"char\": \"C\", \"block\": \"minecraft:chest[facing=north]\", \"loot\": \"loot\"},"
            + " {\"char\": \"1\", \"block\": \"minecraft:spawner\", \"mob\": \"mobs\"}]}",
        "lostcities/conditions/loot.json",
        "{\"values\": [{\"factor\": 1, \"value\": \"lostcities:chests/lostcitychest\"}]}",
        "lostcities/conditions/mobs.json",
        "{\"values\": [{\"factor\": 1, \"value\": \"minecraft:cave_spider\"}]}",
        "lostcities/parts/chest.json",
        "{\"xsize\": 1, \"zsize\": 1, \"slices\": [[\"C\"]]}",
        "lostcities/parts/spawner.json",
        "{\"xsize\": 1, \"zsize\": 1, \"slices\": [[\"1\"]]}");

    private static final CompiledPalette PALETTE = CompiledPalette
        .compile(MetalFlags.ALL_ON, SET.palette(AssetId.parse("p")));

    /** The first job of one chest or spawner part at a chunk, height and building. */
    private static PopulationJob job(String part, int chunkX, int y, String building, boolean rules) {
        JobTarget.Facts facts = new JobTarget.Facts(9, chunkX, 0, 71, 71, 2, 3, BiomeDescriptor.NONE, false, true, 0f)
            .withRoomRules(rules);
        JobTarget target = new JobTarget(facts);
        PartWriter writer = new PartWriter(
            target,
            new PaletteSequence(9, chunkX, 0),
            new PartWriter.Rules(true, true, false, false, false, 63, BlockRef.AIR),
            LayoutLog.SILENT);
        writer.place(
            new PartWriter.Placement(
                SET.part(AssetId.parse(part)),
                PALETTE,
                Transform.ROTATE_NONE,
                0,
                y,
                0,
                HardAir.AIR,
                building == null ? null : AssetId.parse(building)));
        return target.jobs()
            .get(0);
    }

    private static int kept(String part, int y, String building, boolean rules) {
        int kept = 0;
        for (int x = 0; x < 400; x++) {
            if (!job(part, x, y, building, rules).argument()
                .isEmpty()) {
                kept++;
            }
        }
        return kept;
    }

    @Test
    public void legacyWorldsKeepEveryPartSpawnerAndChest() {
        assertEquals(400, kept("spawner", 75, "nh_walkup", false));
        assertEquals(400, kept("chest", 75, "nh_walkup", false));
    }

    @Test
    public void spawnersLeaveHomesShopsStreetsAndRoofs() {
        assertEquals(0, kept("spawner", 75, "nh_walkup", true));
        assertEquals(0, kept("spawner", 75, "nh_cottage", true));
        assertEquals(0, kept("spawner", 75, null, true));
        assertEquals(0, kept("spawner", 71 + 3 * 6 + 2, "nh_workshop", true));
    }

    @Test
    public void spawnersStayInCellarsAndIndustrialFloorsAtTheRarityFactor() {
        int cellar = kept("spawner", 65, "nh_walkup", true);
        int industrial = kept("spawner", 75, "nh_workshop", true);
        for (int got : new int[] { cellar, industrial }) {
            assertEquals(RoomFit.SPAWNER_KEEP * 400, got, 40.0);
        }
    }

    @Test
    public void chestsGoToStorageOfficesKitchensAndIndustrialNotHomes() {
        assertEquals(0, kept("chest", 75, "nh_walkup", true));
        assertEquals(0, kept("chest", 75, "nh_cottage", true));
        assertEquals(RoomFit.CHEST_KEEP * 400, kept("chest", 65, "nh_cottage", true), 45.0);
        assertEquals(RoomFit.CHEST_KEEP * 400, kept("chest", 75, "library00", true), 45.0);
    }

    @Test
    public void unkeptChestsAreClearedAndKeptOnesFilledAndLegacyNeverClears() {
        int clears = 0;
        int chests = 0;
        for (int x = 0; x < 400; x++) {
            PopulationJob job = job("chest", x, 65, "nh_walkup", true);
            if (job.kind() == PopulationJob.Kind.CLEAR_CHEST) {
                clears++;
                assertTrue(
                    job.argument()
                        .isEmpty());
            } else {
                chests++;
                assertEquals(PopulationJob.Kind.CHEST, job.kind());
            }
            assertEquals(PopulationJob.Kind.CHEST, job("chest", x, 65, "nh_walkup", false).kind());
        }
        assertEquals(400, clears + chests);
        assertTrue(clears > 100 && chests > 100);
        // A home floor fits no chest: every one is cleared.
        for (int x = 0; x < 50; x++) {
            assertEquals(PopulationJob.Kind.CLEAR_CHEST, job("chest", x, 75, "nh_walkup", true).kind());
        }
    }

    @Test
    public void theRarityRollIsDeterministic() {
        for (int x = 0; x < 50; x++) {
            assertEquals(
                job("chest", x, 65, "nh_walkup", true).argument(),
                job("chest", x, 65, "nh_walkup", true).argument());
        }
    }

    @Test
    public void everyShippedBuildingResolvesAPalette() {
        FurnishPalettes palettes = FurnishPalettes.bundled();
        List<String> names = new ArrayList<>();
        for (String dir : new String[] { "buildings", "multibuildings" }) {
            File[] files = new File("src/main/resources/data/lostcities/lostcities/" + dir)
                .listFiles((d, n) -> n.endsWith(".json"));
            assertNotNull(files);
            for (File file : files) {
                names.add(
                    file.getName()
                        .replace(".json", ""));
            }
        }
        assertTrue(names.size() > 40, names.toString());
        for (String name : names) {
            for (int floor = -2; floor < 8; floor++) {
                for (long seed = 0; seed < 8; seed++) {
                    RoomKind kind = RoomKind.forFloor(name, floor, seed, true);
                    FurnishPalette palette = palettes.palette(kind);
                    assertNotNull(palette, name + " floor " + floor);
                    assertFalse(
                        palette.items()
                            .isEmpty(),
                        name);
                }
            }
        }
    }

    @Test
    public void theTableMapsTypesAsDocumented() {
        assertEquals(RoomKind.OFFICE, RoomKind.forFloor("town00", 1, 1L, true));
        assertEquals(RoomKind.OFFICE, RoomKind.forFloor("building1", 3, 1L, true));
        assertEquals(RoomKind.INDUSTRIAL, RoomKind.forFloor("oilrig00", 0, 1L, true));
        assertEquals(RoomKind.RESIDENTIAL, RoomKind.forFloor("nh_house_garden", 1, 1L, true));
        assertEquals(RoomKind.BASEMENT, RoomKind.forFloor("town00", -1, 1L, true));
        // Without the table the legacy name rules apply, so town floors are a seeded pick.
        Set<RoomKind> legacy = new HashSet<>();
        for (long s = 0; s < 40; s++) {
            legacy.add(RoomKind.forFloor("town00", 2, s, false));
        }
        assertTrue(legacy.contains(RoomKind.RESIDENTIAL));
    }

    @Test
    public void paletteChestsAndSpawnersSitOnlyInFittingRoomsWithTiersAtOrBelowEv() {
        FurnishPalettes palettes = FurnishPalettes.bundled();
        assertEquals(new ArrayList<String>(), palettes.problems());
        Set<String> allowed = new HashSet<>(
            Arrays.asList(
                LootRules.CITY_SUPPLIES,
                LootRules.CITY_LV,
                LootRules.CITY_MV,
                LootRules.CITY_HV,
                LootRules.CITY_EV));
        boolean anyChest = false;
        boolean anySpawner = false;
        for (RoomKind kind : RoomKind.values()) {
            FurnishPalette palette = palettes.palette(kind);
            for (FurnishItem item : palette.items()) {
                if (FurnishPalette.CHEST.equals(item.block())) {
                    anyChest = true;
                    assertTrue(RoomFit.lootFits(kind), kind.id());
                }
                if (FurnishPalette.SPAWNER.equals(item.block())) {
                    anySpawner = true;
                    assertTrue(RoomFit.spawnerFits(kind), kind.id());
                }
            }
            assertTrue(
                allowed.containsAll(
                    palette.loot()
                        .keySet()),
                kind.id());
            if (!RoomFit.lootFits(kind)) {
                assertTrue(
                    palette.loot()
                        .isEmpty(),
                    kind.id());
            }
            if (!RoomFit.spawnerFits(kind)) {
                assertTrue(
                    palette.mobs()
                        .isEmpty(),
                    kind.id());
            }
        }
        assertTrue(anyChest && anySpawner);
        FurnishPalette basement = palettes.palette(RoomKind.BASEMENT);
        assertNotNull(basement.lootAt(5L, 3, 70, 4));
        assertEquals(basement.mobAt(5L, 3, 70, 4), basement.mobAt(5L, 3, 70, 4));
    }

    @Test
    public void aSpawnerInAResidentialPaletteIsRejected() {
        List<String> problems = new ArrayList<>();
        FurnishPalette palette = FurnishPalette.read(
            RoomKind.RESIDENTIAL,
            new StringReader(
                "{\"room\":\"residential\",\"mobs\":{\"Zombie\":1},\"loot\":{\"lostcitiesnh:city_lv\":1},\"items\":["
                    + "{\"block\":\"minecraft:mob_spawner\",\"tile\":true},"
                    + "{\"block\":\"minecraft:chest\",\"tile\":true},{\"block\":\"minecraft:planks\"}]}"),
            problems);
        assertEquals(2, problems.size(), problems.toString());
        assertEquals(
            1,
            palette.items()
                .size());
    }
}
