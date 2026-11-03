package com.tuxprogrammer.lostcities.worldgen.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.block.TileTemplate;
import com.tuxprogrammer.lostcities.asset.model.PaletteEntry;
import com.tuxprogrammer.lostcities.asset.transform.Transform;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainPhase;

/**
 * The population's dry run passes over the characters that can list no job (D-PF-5): its jobs must be those of a pass
 * that resolves every character, chunk for chunk, draws included.
 */
public class DryRunJobsTest {

    private static final LayoutCityPlan PLAN = CityFixtures.plan();
    private static final LayoutPlanner PLANNER = PLAN.planner();
    private static final int SEARCH = 24;

    /** A job list that resolves every character: the dry run's target, reporting itself as a real write. */
    private static final class FullPass implements CityTarget {

        final JobTarget jobs;

        FullPass(JobTarget jobs) {
            this.jobs = jobs;
        }

        @Override
        public boolean dryRun() {
            return false;
        }

        @Override
        public BlockRef get(int x, int y, int z) {
            return jobs.get(x, y, z);
        }

        @Override
        public void set(int x, int y, int z, BlockRef block) {
            jobs.set(x, y, z, block);
        }

        @Override
        public void clearToMax(int fromY) {
            jobs.clearToMax(fromY);
        }

        @Override
        public void torch(int x, int y, int z) {
            jobs.torch(x, y, z);
        }

        @Override
        public void chest(int x, int y, int z, PaletteEntry info, TileTemplate template, Transform transform,
            PartWriter.Placement placement) {
            jobs.chest(x, y, z, info, template, transform, placement);
        }

        @Override
        public void spawner(int x, int y, int z, PaletteEntry info, PartWriter.Placement placement) {
            jobs.spawner(x, y, z, info, placement);
        }

        @Override
        public void tileEntity(int x, int y, int z, TileTemplate template, Transform transform) {
            jobs.tileEntity(x, y, z, template, transform);
        }
    }

    private static JobTarget jobs(CityWriteContext context, int chunkX, int chunkZ, ChunkPlan plan) {
        return new JobTarget(
            new JobTarget.Facts(
                context.worldSeed(),
                chunkX,
                chunkZ,
                64,
                plan.cityGroundLevel(),
                plan.hasBuilding() ? plan.cellars() : 0,
                plan.hasBuilding() ? plan.floors() : 0,
                context.biomes()
                    .biomeOfChunk(chunkX, chunkZ),
                plan.noLoot(),
                true,
                0.1f));
    }

    private static void write(CityWriteContext context, int chunkX, int chunkZ, CityTarget target) {
        new CityChunkWriter(
            context,
            chunkX,
            chunkZ,
            target,
            new Random(TerrainPhase.contentSeed(context.worldSeed(), chunkX, chunkZ)),
            new PaletteSequence(context.worldSeed(), chunkX, chunkZ)).writeCity();
    }

    @Test
    public void theDryRunListsTheJobsOfAPassThatResolvesEveryCharacter() {
        CityWriteContext context = PLAN.context();
        int cities = 0;
        int withJobs = 0;
        for (int x = -SEARCH; x <= SEARCH; x++) {
            for (int z = -SEARCH; z <= SEARCH; z++) {
                ChunkPlan plan = PLANNER.plan(x, z);
                if (!plan.isCity()) {
                    continue;
                }
                JobTarget dry = jobs(context, x, z, plan);
                JobTarget full = jobs(context, x, z, plan);
                write(context, x, z, dry);
                write(context, x, z, new FullPass(full));
                assertEquals(
                    full.jobs()
                        .toString(),
                    dry.jobs()
                        .toString(),
                    "jobs of chunk " + x + "," + z);
                cities++;
                withJobs += dry.jobs()
                    .isEmpty() ? 0 : 1;
            }
        }
        assertTrue(cities > 50, "city chunks compared: " + cities);
        assertTrue(withJobs > 10, "chunks with jobs compared: " + withJobs);
    }
}
