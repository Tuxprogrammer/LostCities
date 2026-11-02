package com.tuxprogrammer.lostcities.worldgen.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.TestAssets;
import com.tuxprogrammer.lostcities.asset.block.MetalFlags;
import com.tuxprogrammer.lostcities.asset.match.BiomeDescriptor;
import com.tuxprogrammer.lostcities.asset.model.Part;
import com.tuxprogrammer.lostcities.asset.palette.CompiledPalette;
import com.tuxprogrammer.lostcities.asset.transform.Transform;
import com.tuxprogrammer.lostcities.layout.LayoutLog;
import com.tuxprogrammer.lostcities.worldgen.plan.PopulationJob;

/** The job dry run: chests, spawners, torches and tile entities as population jobs. */
public class JobTargetTest {

    /** A chest that holds city loot on cellar floors only, a spawner, a torch and a furnace with a template. */
    private static final AssetSet SET = TestAssets.loadFiles(
        "lostcities/palettes/p.json",
        "{\"palette\": [{\"char\": \"C\", \"block\": \"minecraft:chest[facing=north]\", \"loot\": \"loot\"},"
            + " {\"char\": \"1\", \"block\": \"minecraft:spawner\", \"mob\": \"mobs\"},"
            + " {\"char\": \"T\", \"block\": \"minecraft:torch\", \"torch\": true},"
            + " {\"char\": \";\", \"block\": \"minecraft:furnace\", \"tag\": {\"Items\": [{\"Slot\": 0,"
            + " \"id\": \"minecraft:coal\", \"Count\": 10}], \"facing\": 2}}]}",
        "lostcities/conditions/loot.json",
        "{\"values\": [{\"factor\": 1, \"value\": \"lostcities:chests/lostcitychest\", \"range\": \"-100,-1\"}]}",
        "lostcities/conditions/mobs.json",
        "{\"values\": [{\"factor\": 1, \"value\": \"minecraft:cave_spider\"}]}",
        "lostcities/parts/room.json",
        "{\"xsize\": 4, \"zsize\": 1, \"slices\": [[\"C1T;\"]]}",
        "lostcities/parts/after.json",
        "{\"xsize\": 3, \"zsize\": 1, \"slices\": [[\"1TC\"]]}",
        "lostcities/parts/lone.json",
        "{\"xsize\": 1, \"zsize\": 1, \"slices\": [[\"C\"]]}");

    private static final CompiledPalette PALETTE = CompiledPalette
        .compile(MetalFlags.ALL_ON, SET.palette(AssetId.parse("p")));

    private static List<PopulationJob> jobs(int oy, int x, int z, boolean noLoot, float chestWithout) {
        JobTarget target = new JobTarget(
            new JobTarget.Facts(9, x, z, 71, 71, 2, 3, BiomeDescriptor.NONE, noLoot, true, chestWithout));
        PartWriter writer = new PartWriter(
            target,
            new PaletteSequence(9, x, z),
            new PartWriter.Rules(true, true, false, false, noLoot, 63, com.tuxprogrammer.lostcities.asset.BlockRef.AIR),
            LayoutLog.SILENT);
        Part part = SET.part(AssetId.parse("room"));
        writer.place(new PartWriter.Placement(part, PALETTE, Transform.ROTATE_NONE, 0, oy, 0, HardAir.AIR, null));
        return target.jobs();
    }

    /** A spawner on upstream's {@code hardmobs} condition. */
    private static final AssetSet HARD = TestAssets.loadFiles(
        "lostcities/palettes/h.json",
        "{\"palette\": [{\"char\": \"4\", \"block\": \"minecraft:spawner\", \"mob\": \"hardmobs\"}]}",
        "lostcities/conditions/hardmobs.json",
        "{\"values\": [{\"factor\": 1, \"value\": \"minecraft:blaze\"},"
            + " {\"factor\": 1, \"value\": \"minecraft:witch\"}]}",
        "lostcities/parts/cell.json",
        "{\"xsize\": 1, \"zsize\": 1, \"slices\": [[\"4\"]]}");

    private static String hardMob(int x, boolean enderZoo) {
        JobTarget target = new JobTarget(
            new JobTarget.Facts(9, x, 0, 71, 71, 0, 3, BiomeDescriptor.NONE, false, true, 0f)
                .withOptions(false, enderZoo));
        PartWriter writer = new PartWriter(
            target,
            new PaletteSequence(9, x, 0),
            new PartWriter.Rules(true, true, false, false, false, 63, com.tuxprogrammer.lostcities.asset.BlockRef.AIR),
            LayoutLog.SILENT);
        writer.place(
            new PartWriter.Placement(
                HARD.part(AssetId.parse("cell")),
                CompiledPalette.compile(MetalFlags.ALL_ON, HARD.palette(AssetId.parse("h"))),
                Transform.ROTATE_NONE,
                0,
                75,
                0,
                HardAir.AIR,
                null));
        return target.jobs()
            .get(0)
            .argument();
    }

    @Test
    public void hardMobsAreBlazesAndWitchesUnlessTheEnderZooSetIsOn() {
        java.util.Set<String> plain = new java.util.TreeSet<>();
        java.util.Set<String> enderZoo = new java.util.TreeSet<>();
        for (int x = 0; x < 64; x++) {
            plain.add(hardMob(x, false));
            enderZoo.add(hardMob(x, true));
        }
        assertEquals(new java.util.TreeSet<>(java.util.Arrays.asList("Blaze", "Witch")), plain);
        assertEquals(new java.util.TreeSet<>(LootRules.ENDERZOO_HARDMOBS), enderZoo);
    }

    @Test
    public void listsEachSpecialBlockAsAJob() {
        List<PopulationJob> jobs = jobs(65, 0, 0, false, 0f);
        assertEquals(4, jobs.size(), jobs.toString());
        assertEquals(
            PopulationJob.Kind.CHEST,
            jobs.get(0)
                .kind());
        assertEquals(
            PopulationJob.Kind.SPAWNER,
            jobs.get(1)
                .kind());
        assertEquals(
            "CaveSpider",
            jobs.get(1)
                .argument());
        assertEquals(
            PopulationJob.Kind.TORCH,
            jobs.get(2)
                .kind());
        assertEquals(
            PopulationJob.Kind.TILE_ENTITY,
            jobs.get(3)
                .kind());
        assertTrue(
            jobs.get(3)
                .template()
                .contains("minecraft:coal"),
            jobs.get(3)
                .template());
    }

    @Test
    public void doesNotReproduceASSET05_aCellarChestIsOnFloorMinusOne() {
        // Y 70 is one block below the city ground 71: the first cellar, floor −1, which the loot range -100..-1
        // includes; upstream's truncating division called it floor 0 and the chest stayed empty.
        assertTrue(
            java.util.Arrays.asList(LootRules.CITY_MV, LootRules.CITY_HV, LootRules.CITY_EV)
                .contains(
                    jobs(70, 0, 0, false, 0f).get(0)
                        .argument()),
            "lostcitychest becomes one of the MV to EV bands");
        assertEquals(
            "",
            jobs(71, 0, 0, false, 0f).get(0)
                .argument(),
            "the ground floor is outside the range");
    }

    /**
     * Returns the category a chest at local (2, 65, 0) of a chunk draws: alone ({@code lone} placed at X 2), or after a
     * spawner and a torch the same part lists first ({@code after} placed at X 0).
     */
    private static String chestAtTwo(String part, int ox, int chunkX) {
        JobTarget target = new JobTarget(
            new JobTarget.Facts(9, chunkX, 0, 71, 71, 2, 3, BiomeDescriptor.NONE, false, true, 0.5f));
        PartWriter writer = new PartWriter(
            target,
            new PaletteSequence(9, chunkX, 0),
            new PartWriter.Rules(true, true, false, false, false, 63, com.tuxprogrammer.lostcities.asset.BlockRef.AIR),
            LayoutLog.SILENT);
        writer.place(
            new PartWriter.Placement(
                SET.part(AssetId.parse(part)),
                PALETTE,
                Transform.ROTATE_NONE,
                ox,
                65,
                0,
                HardAir.AIR,
                null));
        for (PopulationJob job : target.jobs()) {
            if (job.kind() == PopulationJob.Kind.CHEST) {
                return job.argument();
            }
        }
        throw new AssertionError("no chest in " + part);
    }

    /**
     * LOOT-05: upstream's chests drew from the chunk's random after vines and other post-steps, in hash-map order, so
     * anything listed before a chest changed its draw. Here a spawner's draw and a torch listed first leave the chest's
     * category, and whether it stays empty, as they are for the chest alone.
     */
    @Test
    public void doesNotReproduceLOOT05_jobsListedBeforeAChestDoNotMoveItsDraw() {
        int empty = 0;
        for (int chunkX = 0; chunkX < 32; chunkX++) {
            String alone = chestAtTwo("lone", 2, chunkX);
            assertEquals(alone, chestAtTwo("after", 0, chunkX), "chunk " + chunkX);
            if (alone.isEmpty()) {
                empty++;
            }
        }
        assertTrue(empty > 0 && empty < 32, "both filled and empty chests are drawn: " + empty + " empty");
    }

    @Test
    public void doesNotReproduceLOOT05_eachChestDrawsFromItsOwnPosition() {
        // The same chest gets the same draw however often it is asked.
        String alone = jobs(65, 3, 4, false, 0.5f).get(0)
            .argument();
        for (int i = 0; i < 5; i++) {
            assertEquals(
                alone,
                jobs(65, 3, 4, false, 0.5f).get(0)
                    .argument());
        }
        int empty = 0;
        for (int x = 0; x < 64; x++) {
            if (jobs(65, x, 0, false, 0.5f).get(0)
                .argument()
                .isEmpty()) {
                empty++;
            }
        }
        assertTrue(empty > 16 && empty < 48, "about half the chests empty at chance 0.5: " + empty);
    }

    @Test
    public void aBuildingWithoutLootKeepsEmptyChestsAndNoSpawners() {
        List<PopulationJob> jobs = jobs(65, 0, 0, true, 0f);
        assertEquals(
            "",
            jobs.get(0)
                .argument());
        assertTrue(
            jobs.stream()
                .noneMatch(job -> job.kind() == PopulationJob.Kind.SPAWNER),
            jobs.toString());
    }

    @Test
    public void doesNotReproduceTERR09_jobsAreThisChunksAndInsideIt() {
        for (PopulationJob job : jobs(65, -7, 12, false, 0f)) {
            assertTrue(job.x() >= 0 && job.x() < 16 && job.z() >= 0 && job.z() < 16, job.toString());
        }
    }
}
