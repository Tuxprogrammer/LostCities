package com.tuxprogrammer.lostcities.worldgen.city;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.layout.Direction;
import com.tuxprogrammer.lostcities.layout.Doorway;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.worldgen.buffer.ArrayChunkBuffer;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;

/**
 * The blocks in front of a building's doors stay open whatever else writes there: a station's wall in a street, a
 * parapet on the roof of a lower neighbor (the walkability lane: {@code city.walk} found doors opening onto a block).
 */
public class DoorStepsTest {

    private static final LayoutCityPlan PLAN = CityFixtures.plan();
    private static final LayoutPlanner PLANNER = PLAN.planner();
    private static final int SEARCH = 90;
    private static final int NETWORK_SEARCH = 220;
    private static final int LIMIT = 8;

    @Test
    public void streetDoorStepsStayOpenInChunksWithAnNetwork() {
        int checked = 0;
        for (int z = -NETWORK_SEARCH; z <= NETWORK_SEARCH && checked < LIMIT; z++) {
            for (int x = -NETWORK_SEARCH; x <= NETWORK_SEARCH && checked < LIMIT; x++) {
                if (!NetworkWriter.hasWork(PLANNER, x, z) || CityChunkWriter.doorApproaches(PLANNER, x, z) == null) {
                    continue;
                }
                ArrayChunkBuffer buffer = CityFixtures.generate(PLAN, x, z);
                boolean[] approach = CityChunkWriter.doorApproaches(PLANNER, x, z);
                int ground = PLANNER.plan(x, z)
                    .cityGroundLevel();
                for (Direction side : Direction.values()) {
                    for (int d = 0; d < CityChunkWriter.DOOR_STEP; d++) {
                        for (int along = 7; along <= 8; along++) {
                            int[] c = CityChunkWriter.vegetationColumn(side, d, along);
                            if (!approach[(c[1] << 4) | c[0]]) {
                                continue;
                            }
                            for (int y = ground + 1; y <= ground + 2; y++) {
                                assertTrue(
                                    open(buffer.get(c[0], y, c[1])),
                                    "door step open at " + x + "," + z + " local " + c[0] + "," + y + "," + c[1] + ": "
                                        + buffer.get(c[0], y, c[1]));
                            }
                        }
                    }
                }
                checked++;
            }
        }
        assertTrue(checked > 0, "a street chunk with a network and a building door facing it");
    }

    @Test
    public void roofDoorStepsStayOpenWhereANeighborsDoorOpensOntoTheRoof() {
        int checked = 0;
        for (int z = -SEARCH; z <= SEARCH && checked < LIMIT; z++) {
            for (int x = -SEARCH; x <= SEARCH && checked < LIMIT; x++) {
                ChunkPlan plan = PLANNER.plan(x, z);
                if (!plan.hasBuilding() || plan.ruinHeight() >= 0) {
                    continue;
                }
                ArrayChunkBuffer buffer = null;
                int roofY = plan.buildingBottomHeight() + plan.floorPitch() * (plan.cellars() + plan.floors());
                for (Direction side : Direction.values()) {
                    ChunkPlan other = PLANNER.plan(x + side.stepX(), z + side.stepZ());
                    int floor = plan.floors() - other.cityLevel() + plan.cityLevel();
                    if (!other.hasBuilding() || floor < 0
                        || floor >= other.floors()
                        || PLANNER.doorway(other.chunkX(), other.chunkZ(), floor, side.opposite()) != Doorway.DOOR) {
                        continue;
                    }
                    if (buffer == null) {
                        buffer = CityFixtures.generate(PLAN, x, z);
                    }
                    for (int d = 0; d < CityChunkWriter.DOOR_STEP; d++) {
                        for (int along = 7; along <= 8; along++) {
                            int[] c = CityChunkWriter.vegetationColumn(side, d, along);
                            for (int y = roofY + 1; y <= roofY + 2; y++) {
                                assertTrue(
                                    open(buffer.get(c[0], y, c[1])),
                                    "roof door step open at " + x + "," + z + " local " + c[0] + "," + y + "," + c[1]
                                        + ": " + buffer.get(c[0], y, c[1]));
                            }
                        }
                    }
                    checked++;
                }
            }
        }
        assertTrue(checked > 0, "a roof with a neighbor's door opening onto it");
    }

    /** Air, or a vine, which grows over a doorway's neighbors but does not stop a walker. */
    private static boolean open(BlockRef block) {
        return block.isAir() || block.name()
            .contains("vine");
    }
}
