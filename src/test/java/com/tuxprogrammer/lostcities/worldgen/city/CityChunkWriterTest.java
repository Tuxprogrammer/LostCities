package com.tuxprogrammer.lostcities.worldgen.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.model.Part;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.Direction;
import com.tuxprogrammer.lostcities.layout.Doorway;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.layout.StreetType;
import com.tuxprogrammer.lostcities.worldgen.buffer.ArrayChunkBuffer;

/** A city chunk's own content: buildings, streets, parks, doors, fronts and vegetation. */
public class CityChunkWriterTest {

    private static final LayoutCityPlan PLAN = CityFixtures.plan();
    private static final LayoutPlanner PLANNER = PLAN.planner();
    private static final int SEARCH = 24;

    @Test
    public void aBuildingIsWrittenFloorByFloorFromItsParts() {
        int[] at = CityFixtures.first(SEARCH, (x, z) -> {
            ChunkPlan plan = PLANNER.plan(x, z);
            return plan.hasBuilding() && plan.floors() >= 3 && plan.ruinHeight() < 0;
        });
        assertNotNull(at, "a building of three floors near the origin");
        ChunkPlan plan = PLANNER.plan(at[0], at[1]);
        ArrayChunkBuffer buffer = CityFixtures.generate(PLAN, at[0], at[1]);
        for (int f = 0; f < plan.floors(); f++) {
            int y = plan.cityGroundLevel() + ChunkPlan.FLOOR_HEIGHT * f;
            int solid = 0;
            for (int x = 1; x < 15; x++) {
                for (int z = 1; z < 15; z++) {
                    if (!buffer.get(x, y, z)
                        .isAir()) {
                        solid++;
                    }
                }
            }
            assertTrue(solid > 100, "floor " + f + " of " + plan.building() + " has a floor: " + solid);
        }
    }

    @Test
    public void doesNotReproduceCITY36_doorwaysFollowTheLayoutOnEverySide() {
        int checked = 0;
        for (int z = -SEARCH; z <= SEARCH && checked < 6; z++) {
            for (int x = -SEARCH; x <= SEARCH && checked < 6; x++) {
                ChunkPlan plan = PLANNER.plan(x, z);
                if (!plan.hasBuilding() || plan.ruinHeight() >= 0) {
                    continue;
                }
                ArrayChunkBuffer buffer = null;
                for (Direction side : Direction.values()) {
                    Doorway doorway = PLANNER.doorway(x, z, 0, side);
                    if (doorway == Doorway.NONE) {
                        continue;
                    }
                    if (buffer == null) {
                        buffer = CityFixtures.generate(PLAN, x, z);
                    }
                    int lx = side == Direction.XMIN ? 0 : side == Direction.XMAX ? 15 : 7;
                    int lz = side == Direction.ZMIN ? 0 : side == Direction.ZMAX ? 15 : 7;
                    BlockRef lower = buffer.get(lx, plan.cityGroundLevel() + 1, lz);
                    if (doorway == Doorway.PASSAGE) {
                        assertTrue(lower.isAir(), "a passage is open: " + lower);
                    } else {
                        assertTrue(lower.name()
                            .contains("door"), "a door stands on side " + side + ": " + lower);
                    }
                    checked++;
                }
            }
        }
        assertTrue(checked > 0, "some doorway near the origin");
    }

    @Test
    public void doorHalvesCarry1710Metadata() {
        BlockRef door = BlockRef.of("minecraft:wooden_door", 0);
        assertEquals(0, CityChunkWriter.doorHalf(door, Direction.XMIN, true, false).meta(), "west wall faces east");
        assertEquals(2, CityChunkWriter.doorHalf(door, Direction.XMAX, true, false).meta(), "east wall faces west");
        assertEquals(3, CityChunkWriter.doorHalf(door, Direction.ZMIN, true, false).meta());
        assertEquals(1, CityChunkWriter.doorHalf(door, Direction.ZMAX, true, false).meta());
        // The two leaves of a double door hinge on opposite sides.
        assertEquals(8, CityChunkWriter.doorHalf(door, Direction.XMIN, true, true).meta());
        assertEquals(9, CityChunkWriter.doorHalf(door, Direction.XMIN, false, true).meta());
        assertEquals(9, CityChunkWriter.doorHalf(door, Direction.XMAX, true, true).meta());
    }

    @Test
    public void doesNotReproduceTERR07_aParkStaysAParkAtGenerationTime() {
        int[] at = CityFixtures.first(SEARCH, (x, z) -> {
            ChunkPlan plan = PLANNER.plan(x, z);
            return plan.isCity() && !plan.hasBuilding()
                && plan.streetType() == StreetType.PARK
                && !PLANNER.isElevatedParkSection(x, z)
                && PLANNER.zone(x, z)
                    .zone() == com.tuxprogrammer.lostcities.layout.landmark.Zone.NONE;
        });
        assertNotNull(at, "a park near the origin");
        ArrayChunkBuffer buffer = CityFixtures.generate(PLAN, at[0], at[1]);
        int ground = PLANNER.plan(at[0], at[1])
            .cityGroundLevel();
        int grass = 0;
        for (int x = 1; x < 15; x++) {
            for (int z = 1; z < 15; z++) {
                if ("minecraft:grass".equals(buffer.get(x, ground, z)
                    .name())) {
                    grass++;
                }
            }
        }
        // Upstream re-rolled every street type to NORMAL here and laid a street part instead.
        assertTrue(grass > 150, "the park's grass, not a street: " + grass);
    }

    @Test
    public void doesNotReproduceTERR08_randomVegetationIsTheSameOnEverySide() {
        for (int d = 0; d < 2; d++) {
            float chance = CityChunkWriter.vegetationChance(d, 2, 0.1f);
            assertEquals(0.1f * (3 - d), chance, 1e-6f, "one weight for every side");
            assertTrue(CityChunkWriter.vegetationChance(d, 8, 0.5f) <= CityChunkWriter.MAX_VEGETATION_CHANCE);
        }
        // The outermost column is used on the +X and +Z sides too; upstream stopped at column 14.
        assertEquals(15, CityChunkWriter.vegetationColumn(Direction.XMAX, 0, 4)[0]);
        assertEquals(15, CityChunkWriter.vegetationColumn(Direction.ZMAX, 0, 4)[1]);
        assertEquals(0, CityChunkWriter.vegetationColumn(Direction.XMIN, 0, 4)[0]);
        assertEquals(0, CityChunkWriter.vegetationColumn(Direction.ZMIN, 0, 4)[1]);
    }

    @Test
    public void doesNotReproduceTERR09_aFrontIsWrittenInTheStreetWithTheStreetsJobs() {
        int[] at = CityFixtures.first(SEARCH, (x, z) -> {
            if (!PLANNER.plan(x, z)
                .isCity() || PLANNER.plan(x, z)
                    .hasBuilding()) {
                return false;
            }
            for (Direction side : Direction.values()) {
                if (PLANNER.hasFrontPartFrom(x, z, side)) {
                    return true;
                }
            }
            return false;
        });
        assertNotNull(at, "a street with a building's front near the origin");
        // The street's own jobs are all inside it: nothing is queued on the building, as upstream's front did.
        for (com.tuxprogrammer.lostcities.worldgen.plan.PopulationJob job : PLAN.chunk(at[0], at[1])
            .jobs()) {
            assertTrue(job.x() >= 0 && job.x() < 16 && job.z() >= 0 && job.z() < 16, job.toString());
        }
        ArrayChunkBuffer buffer = CityFixtures.generate(PLAN, at[0], at[1]);
        int ground = PLANNER.plan(at[0], at[1])
            .cityGroundLevel();
        int above = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = ground + 1; y <= ground + 4; y++) {
                    if (!buffer.get(x, y, z)
                        .isAir()) {
                        above++;
                    }
                }
            }
        }
        assertTrue(above > 0, "the front stands in the street chunk");
    }

    @Test
    public void doesNotReproduceASSET32_anEmptyPartListPicksNothingAndDrawsNothing() {
        Random random = new Random(5);
        long before = new Random(5).nextLong();
        assertNull(CityChunkWriter.pick(Collections.<Part>emptyList(), random));
        assertEquals(before, random.nextLong(), "no draw for an empty list");
    }

    @Test
    public void theDryRunListsTheSameJobsEveryTime() {
        int[] at = CityFixtures.first(SEARCH, (x, z) -> PLANNER.plan(x, z)
            .hasBuilding()
            && !PLAN.chunk(x, z)
                .jobs()
                .isEmpty());
        assertNotNull(at, "a building with population jobs");
        LayoutCityPlan other = CityFixtures.plan();
        assertEquals(
            PLAN.chunk(at[0], at[1])
                .jobs()
                .toString(),
            other.chunk(at[0], at[1])
                .jobs()
                .toString());
    }
}
