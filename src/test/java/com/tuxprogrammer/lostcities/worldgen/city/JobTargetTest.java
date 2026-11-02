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
        "{\"xsize\": 4, \"zsize\": 1, \"slices\": [[\"C1T;\"]]}");

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

    @Test
    public void listsEachSpecialBlockAsAJob() {
        List<PopulationJob> jobs = jobs(65, 0, 0, false, 0f);
        assertEquals(4, jobs.size(), jobs.toString());
        assertEquals(PopulationJob.Kind.CHEST, jobs.get(0).kind());
        assertEquals(PopulationJob.Kind.SPAWNER, jobs.get(1).kind());
        assertEquals("CaveSpider", jobs.get(1).argument());
        assertEquals(PopulationJob.Kind.TORCH, jobs.get(2).kind());
        assertEquals(PopulationJob.Kind.TILE_ENTITY, jobs.get(3).kind());
        assertTrue(jobs.get(3).template().contains("minecraft:coal"), jobs.get(3).template());
    }

    @Test
    public void doesNotReproduceASSET05_aCellarChestIsOnFloorMinusOne() {
        // Y 70 is one block below the city ground 71: the first cellar, floor −1, which the loot range -100..-1
        // includes; upstream's truncating division called it floor 0 and the chest stayed empty.
        assertEquals(LootRules.CITY, jobs(70, 0, 0, false, 0f).get(0).argument());
        assertEquals("", jobs(71, 0, 0, false, 0f).get(0).argument(), "the ground floor is outside the range");
    }

    @Test
    public void doesNotReproduceLOOT05_eachChestDrawsFromItsOwnPosition() {
        // The same chest gets the same draw whatever else the chunk lists before it.
        String alone = jobs(65, 3, 4, false, 0.5f).get(0).argument();
        for (int i = 0; i < 5; i++) {
            assertEquals(alone, jobs(65, 3, 4, false, 0.5f).get(0).argument());
        }
        int empty = 0;
        for (int x = 0; x < 64; x++) {
            if (jobs(65, x, 0, false, 0.5f).get(0).argument().isEmpty()) {
                empty++;
            }
        }
        assertTrue(empty > 16 && empty < 48, "about half the chests empty at chance 0.5: " + empty);
    }

    @Test
    public void aBuildingWithoutLootKeepsEmptyChestsAndNoSpawners() {
        List<PopulationJob> jobs = jobs(65, 0, 0, true, 0f);
        assertEquals("", jobs.get(0).argument());
        assertTrue(jobs.stream().noneMatch(job -> job.kind() == PopulationJob.Kind.SPAWNER), jobs.toString());
    }

    @Test
    public void doesNotReproduceTERR09_jobsAreThisChunksAndInsideIt() {
        for (PopulationJob job : jobs(65, -7, 12, false, 0f)) {
            assertTrue(job.x() >= 0 && job.x() < 16 && job.z() >= 0 && job.z() < 16, job.toString());
        }
    }
}
