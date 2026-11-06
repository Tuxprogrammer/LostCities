package com.tuxprogrammer.lostcities.worldgen.city;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.layout.Direction;
import com.tuxprogrammer.lostcities.layout.Doorway;
import com.tuxprogrammer.lostcities.layout.LayoutLog;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
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
    private static final int NETWORK_SEARCH = 60;
    private static final long[] SEEDS = { CityFixtures.SEED, 1L, 2L, 3L, 777L, 31337L };
    private static final int LIMIT = 8;

    @Test
    public void clearingTheDoorStepsOpensEveryApproachColumnWhateverStandsThere() {
        int[] at = CityFixtures.first(NETWORK_SEARCH, (x, z) -> CityChunkWriter.doorApproaches(PLANNER, x, z) != null);
        assertTrue(at != null, "a street chunk with a building door facing it");
        boolean[] approach = CityChunkWriter.doorApproaches(PLANNER, at[0], at[1]);
        int ground = PLANNER.plan(at[0], at[1])
            .cityGroundLevel();
        Map<Long, BlockRef> blocks = new HashMap<>();
        BlockRef stone = BlockRef.of("minecraft:stonebrick", 0);
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                for (int y = ground + 1; y <= ground + 2; y++) {
                    blocks.put(key(x, y, z), stone);
                }
            }
        }
        CityChunkWriter.clearDoorSteps(PLANNER, at[0], at[1], ground, new MapTarget(blocks));
        int cleared = 0;
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                for (int y = ground + 1; y <= ground + 2; y++) {
                    boolean air = blocks.get(key(x, y, z))
                        .isAir();
                    if (air) {
                        cleared++;
                        assertTrue(approach[(z << 4) | x], "only an approach column opens: " + x + "," + z);
                    }
                }
            }
        }
        assertTrue(cleared >= 2 * 2 * 2, "a door's two columns, two deep, two high: " + cleared);
    }

    private static long key(int x, int y, int z) {
        return ((long) y << 8) | (z << 4) | x;
    }

    /** A target that keeps what is set in a map. */
    private static final class MapTarget implements CityTarget {

        private final Map<Long, BlockRef> blocks;

        MapTarget(Map<Long, BlockRef> blocks) {
            this.blocks = blocks;
        }

        @Override
        public boolean dryRun() {
            return false;
        }

        @Override
        public BlockRef get(int x, int y, int z) {
            return blocks.getOrDefault(key(x, y, z), BlockRef.AIR);
        }

        @Override
        public void set(int x, int y, int z, BlockRef block) {
            blocks.put(key(x, y, z), block);
        }

        @Override
        public void clearToMax(int fromY) {}

        @Override
        public void torch(int x, int y, int z) {}

        @Override
        public void spawner(int x, int y, int z, com.tuxprogrammer.lostcities.asset.model.PaletteEntry info,
            PartWriter.Placement placement) {}

        @Override
        public void tileEntity(int x, int y, int z, com.tuxprogrammer.lostcities.asset.block.TileTemplate template,
            com.tuxprogrammer.lostcities.asset.transform.Transform transform) {}
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
