package com.tuxprogrammer.lostcities.worldgen.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.ScatteredPlan;
import com.tuxprogrammer.lostcities.layout.ScatteredPlanner;
import com.tuxprogrammer.lostcities.worldgen.buffer.ArrayChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkContent;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;
import com.tuxprogrammer.lostcities.worldgen.plan.GroundFit;
import com.tuxprogrammer.lostcities.worldgen.plan.PopulationJob;
import com.tuxprogrammer.lostcities.worldgen.terrain.ColumnOps;
import com.tuxprogrammer.lostcities.worldgen.terrain.NameBlockClassifier;

/**
 * Scattered structures through the real plan source and the terrain phase: the bundled world style's structures over
 * the test terrain, written whole into their footprints, defended, and set into the ground (07-scattered, 0009).
 */
public class ScatteredStructuresTest {

    private static final ScatteredWorld WORLD = new ScatteredWorld(CityFixtures.SEED);
    private static final NameBlockClassifier CLASSES = new NameBlockClassifier();

    private static ScatteredPlan nearestLand() {
        ScatteredPlan cabin = WORLD.nearest("cabin");
        ScatteredPlan tower = WORLD.nearest("radiotower");
        assertTrue(cabin != null || tower != null, "no cabin or radio tower within 256 chunks");
        return cabin != null ? cabin : tower;
    }

    @Test
    public void theBundledWorldStylePlacesLandStructuresAndNoOilRigOnLand() {
        assertTrue(WORLD.scattered.enabled());
        assertEquals(8, WORLD.scattered.areaSize());
        assertNotNull(nearestLand());
        assertNull(WORLD.nearest("oilrig"), "plains everywhere: no ocean, no oil rig");
        assertNull(WORLD.nearest("highway_gas_station"), "no highways yet: no gas station");
    }

    @Test
    public void aFootprintChunkCarriesTheStructureItsGuardAndItsVeto() {
        ScatteredPlan plan = nearestLand();
        ChunkPlan chunk = WORLD.plan.chunk(plan.minChunkX(), plan.minChunkZ());
        assertFalse(chunk.isCity());
        assertTrue(chunk.isScattered());
        assertTrue(
            chunk.label()
                .startsWith("scattered "));
        assertTrue(chunk.content() != ChunkContent.NONE);
        assertTrue(chunk.hasFootprint());
        assertTrue(chunk.footprintBottom(8, 8) <= plan.lowestLevel());
        assertEquals(
            1,
            chunk.groundFits()
                .size());
        ChunkPlan beside = WORLD.plan.chunk(plan.minChunkX() + 1, plan.minChunkZ());
        assertFalse(beside.isScattered());
        assertEquals(chunk.groundFits(), beside.groundFits(), "the fade reaches the neighbor");
        ChunkPlan far = WORLD.plan.chunk(plan.minChunkX() + 3, plan.minChunkZ());
        assertTrue(
            far.groundFits()
                .isEmpty());
    }

    @Test
    public void theStructureIsWrittenWholeOnLeveledGround() {
        ScatteredPlan plan = nearestLand();
        int cx = plan.minChunkX();
        int cz = plan.minChunkZ();
        ArrayChunkBuffer buffer = WORLD.generate(cx, cz);
        // The share written alone over air: every block it places must be in the generated chunk.
        ArrayChunkBuffer alone = new ArrayChunkBuffer();
        new ScatteredWriter(
            WORLD.plan.context(),
            plan,
            plan.share(cx, cz),
            new BufferTarget(alone, null),
            new PaletteSequence(CityFixtures.SEED, cx, cz)).write();
        int placed = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 256; y++) {
                    if (!alone.get(x, y, z)
                        .isAir()) {
                        placed++;
                        assertEquals(alone.get(x, y, z), buffer.get(x, y, z), "at " + x + "," + y + "," + z);
                    }
                }
            }
        }
        assertTrue(placed > 40, "placed " + placed);
        GroundFit fit = WORLD.plan.chunk(cx, cz)
            .groundFits()
            .get(0);
        // Every pad column's ground under the structure is the plan's ground: nothing pokes inside, nothing floats.
        int padColumns = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (fit.distance((cx << 4) + x, (cz << 4) + z) > 0) {
                    continue;
                }
                padColumns++;
                int top = plan.groundY();
                assertTrue(
                    CLASSES.isEmpty(buffer.get(x, top + 1, z)) || !alone.get(x, top + 1, z)
                        .isAir(),
                    "ground above the pad at " + x
                        + ","
                        + z
                        + ": "
                        + buffer.get(x, top + 1, z)
                        + " over "
                        + buffer.get(x, top, z)
                        + " in "
                        + plan
                        + " fits "
                        + WORLD.plan.chunk(cx, cz)
                            .groundFits());
                assertFalse(CLASSES.isEmpty(buffer.get(x, top, z)), "a gap under the structure at " + x + "," + z);
            }
        }
        assertTrue(padColumns > 0);
    }

    @Test
    public void theGroundAroundMeetsTheTerrainWithoutACliff() {
        ScatteredPlan plan = nearestLand();
        int cx = plan.minChunkX();
        int cz = plan.minChunkZ();
        GroundFit fit = WORLD.plan.chunk(cx, cz)
            .groundFits()
            .get(0);
        int[][] heights = new int[48][48];
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                ArrayChunkBuffer buffer = WORLD.generate(cx + dx, cz + dz);
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        heights[(dx + 1) * 16 + x][(dz + 1) * 16 + z] = ColumnOps.surface(buffer, CLASSES, x, z, true);
                    }
                }
            }
        }
        int baseX = (cx - 1) << 4;
        int baseZ = (cz - 1) << 4;
        for (int x = 0; x + 1 < 48; x++) {
            for (int z = 0; z + 1 < 48; z++) {
                // Only the ground: columns of the structure itself are walls and towers.
                if (fit.distance(baseX + x, baseZ + z) <= 0 || fit.distance(baseX + x + 1, baseZ + z) <= 0
                    || fit.distance(baseX + x, baseZ + z + 1) <= 0) {
                    continue;
                }
                assertTrue(Math.abs(heights[x + 1][z] - heights[x][z]) <= 2, "step at " + x + "," + z);
                assertTrue(Math.abs(heights[x][z + 1] - heights[x][z]) <= 2, "step at " + x + "," + z);
            }
        }
    }

    @Test
    public void jobsComeFromTheSameWriterAndStayInTheChunk() {
        ScatteredPlan cabin = WORLD.nearest("cabin");
        if (cabin == null) {
            return;
        }
        List<PopulationJob> jobs = WORLD.plan.chunk(cabin.minChunkX(), cabin.minChunkZ())
            .jobs();
        for (PopulationJob job : jobs) {
            assertTrue(job.y() >= cabin.lowestLevel(), job.toString());
        }
    }

    @Test
    public void scatteredContainersGetALootCategory() {
        ScatteredPlan cabin = WORLD.nearest("cabin");
        assertNotNull(cabin);
        int barrels = 0;
        for (ScatteredPlan.Share share : cabin.shares()) {
            ChunkPlan chunk = WORLD.plan.chunk(share.chunkX(), share.chunkZ());
            ArrayChunkBuffer buffer = new ArrayChunkBuffer();
            chunk.content()
                .write(buffer, null);
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 0; y < 256; y++) {
                        if (!buffer.get(x, y, z)
                            .name()
                            .endsWith("barrel")) {
                            continue;
                        }
                        barrels++;
                        PopulationJob found = null;
                        for (PopulationJob job : chunk.jobs()) {
                            if (job.kind() == PopulationJob.Kind.CHEST && job.x() == x && job.y() == y
                                && job.z() == z) {
                                found = job;
                            }
                        }
                        assertNotNull(found, "no chest job at the barrel " + x + "," + y + "," + z);
                        assertFalse(found.argument().isEmpty(), "empty category: " + found);
                    }
                }
            }
        }
        assertTrue(barrels > 0, "the cabin has a barrel");
    }

    @Test
    public void structuresKeepClearOfCitiesByTheBlendsReach() {
        ScatteredPlanner planner = WORLD.scattered;
        int structures = 0;
        for (int z = -80; z <= 80; z++) {
            for (int x = -80; x <= 80; x++) {
                if (!planner.inFootprint(x, z)) {
                    continue;
                }
                structures++;
                for (int dz = -ScatteredPlanner.BLEND_CLEARANCE; dz <= ScatteredPlanner.BLEND_CLEARANCE; dz++) {
                    for (int dx = -ScatteredPlanner.BLEND_CLEARANCE; dx <= ScatteredPlanner.BLEND_CLEARANCE; dx++) {
                        assertFalse(
                            WORLD.plan.chunk(x + dx, z + dz)
                                .isCity(),
                            "city chunk near the structure at " + x + "," + z);
                    }
                }
            }
        }
        assertTrue(structures > 0);
    }

    @Test
    public void theFinderNamesTheStructureAndAViewOfIt() {
        ScatteredFinder.Result found = new ScatteredFinder(WORLD.scattered).find(null, 0, 0, 256);
        assertNotNull(found);
        String line = found.format();
        assertTrue(line.startsWith("kind=scattered block="), line);
        assertTrue(line.contains(" label=scattered lostcities:"), line);
        assertTrue(found.viewY() > found.y());
        assertTrue(ScatteredFinder.handles("scattered"));
        assertTrue(ScatteredFinder.handles("Scattered:oilrig"));
        assertFalse(ScatteredFinder.handles("street"));
        assertEquals("oilrig", ScatteredFinder.nameOf("scattered:oilrig"));
        assertNull(ScatteredFinder.nameOf("scattered"));
    }
}
