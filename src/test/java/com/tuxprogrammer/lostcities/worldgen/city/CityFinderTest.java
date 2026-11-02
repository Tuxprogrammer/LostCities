package com.tuxprogrammer.lostcities.worldgen.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.LayoutFixtures;
import com.tuxprogrammer.lostcities.layout.LayoutLog;
import com.tuxprogrammer.lostcities.layout.landmark.BlockRect;
import com.tuxprogrammer.lostcities.layout.landmark.Gate;
import com.tuxprogrammer.lostcities.layout.landmark.Landmark;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkKind;
import com.tuxprogrammer.lostcities.layout.landmark.LaneRole;
import com.tuxprogrammer.lostcities.layout.landmark.Zone;
import com.tuxprogrammer.lostcities.layout.street.RoadDirection;
import com.tuxprogrammer.lostcities.worldgen.plan.LanePath;

/** The finder of {@code /lostcities find}, over real cities and a fake village oracle. */
public class CityFinderTest {

    private static final LayoutCityPlan PLAN = CityFixtures.plan();

    /** A 2 × 2-chunk village inside the test city, with a gate in the middle of each side. */
    private static Landmark village(int chunkX, int chunkZ) {
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        int maxX = minX + 31;
        int maxZ = minZ + 31;
        return Landmark.builder("village@" + minX + "," + minZ, LandmarkKind.VILLAGE)
            .footprint(new BlockRect(minX, minZ, maxX, maxZ))
            .groundEstimate(70)
            .gate(new Gate(minX + 16, minZ, RoadDirection.NORTH))
            .gate(new Gate(minX + 16, maxZ, RoadDirection.SOUTH))
            .gate(new Gate(minX, minZ + 16, RoadDirection.WEST))
            .gate(new Gate(maxX, minZ + 16, RoadDirection.EAST))
            .build();
    }

    /** The middle of a block of city chunks near the origin, where the village goes. */
    private static int[] cityBlock() {
        int[] at = CityFixtures.first(12, (x, z) -> {
            for (int dx = -3; dx <= 4; dx++) {
                for (int dz = -3; dz <= 4; dz++) {
                    if (!PLAN.planner()
                        .plan(x + dx, z + dz)
                        .isCity()) {
                        return false;
                    }
                }
            }
            return true;
        });
        assertNotNull(at, "a block of 8 × 8 city chunks near the origin");
        return at;
    }

    @Test
    public void findsTheNearestCityCenterStreetBuildingAndEdge() {
        CityFinder finder = new CityFinder(PLAN.planner(), PLAN);
        for (CityFinder.Kind kind : new CityFinder.Kind[] { CityFinder.Kind.CENTER, CityFinder.Kind.STREET,
            CityFinder.Kind.INTERIOR, CityFinder.Kind.EDGE }) {
            CityFinder.Result found = finder.find(kind, 0, 0, 40);
            assertNotNull(found, kind.key());
            assertEquals(kind, found.kind());
            assertTrue(
                found.format()
                    .startsWith("kind=" + kind.key() + " block="),
                found.format());
        }
        CityFinder.Result center = finder.find(CityFinder.Kind.CENTER, 0, 0, 40);
        assertTrue(
            PLAN.planner()
                .isCityCenter(center.chunkX(), center.chunkZ()));
        CityFinder.Result building = finder.find(CityFinder.Kind.INTERIOR, 0, 0, 40);
        ChunkPlan plan = PLAN.planner()
            .plan(building.chunkX(), building.chunkZ());
        assertTrue(plan.hasBuilding() && plan.floors() >= 2, building.format());
    }

    @Test
    public void theNearestIsNearestByBlockDistance() {
        CityFinder finder = new CityFinder(PLAN.planner(), PLAN);
        CityFinder.Result found = finder.find(CityFinder.Kind.STREET, 100, -50, 40);
        long best = (long) (found.x() - 100) * (found.x() - 100) + (long) (found.z() + 50) * (found.z() + 50);
        CityFixtures.square(10, c -> {
            CityFinder.Result other = finder.at(CityFinder.Kind.STREET, (100 >> 4) + c[0], (-50 >> 4) + c[1]);
            if (other != null) {
                long d = (long) (other.x() - 100) * (other.x() - 100) + (long) (other.z() + 50) * (other.z() + 50);
                assertTrue(d >= best, other.format() + " is nearer than " + found.format());
            }
        });
    }

    @Test
    public void reservedLotsAndVillagesNeedAnOracleThatKnowsOne() {
        CityFinder plain = new CityFinder(PLAN.planner(), PLAN);
        assertNull(plain.find(CityFinder.Kind.RESERVED, 0, 0, 16), "no landmark, no reserved lot");
        assertNull(plain.find(CityFinder.Kind.VILLAGE, 0, 0, 16), "no landmark, no village");

        int[] block = cityBlock();
        LayoutCityPlan withVillage = CityFixtures
            .plan(CityFixtures.SEED, new LayoutFixtures.FakeOracle(village(block[0], block[1])), LayoutLog.SILENT);
        CityFinder finder = new CityFinder(withVillage.planner(), withVillage);
        CityFinder.Result reserved = finder.find(CityFinder.Kind.RESERVED, block[0] << 4, block[1] << 4, 16);
        assertNotNull(reserved);
        assertTrue(
            withVillage.planner()
                .zone(reserved.chunkX(), reserved.chunkZ())
                .zone() != Zone.NONE,
            reserved.format());
        CityFinder.Result sewn = finder.find(CityFinder.Kind.VILLAGE, block[0] << 4, block[1] << 4, 16);
        assertNotNull(sewn, "the village is sewn into the city");
        LaneRole role = withVillage.planner()
            .plan(sewn.chunkX(), sewn.chunkZ())
            .lane()
            .role();
        assertTrue(role == LaneRole.GATE || role == LaneRole.LANE || role == LaneRole.LINK, sewn.format());
    }

    @Test
    public void aGateChunkAndTheCoreCarryTheLanesPath() {
        int[] block = cityBlock();
        LayoutCityPlan withVillage = CityFixtures
            .plan(CityFixtures.SEED, new LayoutFixtures.FakeOracle(village(block[0], block[1])), LayoutLog.SILENT);
        int gates = 0;
        int coreLanes = 0;
        for (int z = block[1] - 4; z <= block[1] + 6; z++) {
            for (int x = block[0] - 4; x <= block[0] + 6; x++) {
                com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan plan = withVillage.chunk(x, z);
                LanePath lane = plan.lane();
                if (withVillage.planner()
                    .plan(x, z)
                    .lane()
                    .role() == LaneRole.GATE) {
                    gates++;
                    assertNotNull(lane, "a gate chunk has a path");
                }
                if (plan.isCore() && lane != null) {
                    coreLanes++;
                }
            }
        }
        assertTrue(gates > 0, "the layout laid a gate");
        assertTrue(coreLanes > 0, "the path runs into the core to the gate");
    }

    @Test
    public void theBestVillageViewStandsBesideAGateInTheCity() {
        int[] block = cityBlock();
        Landmark village = village(block[0], block[1]);
        LayoutCityPlan withVillage = CityFixtures
            .plan(CityFixtures.SEED, new LayoutFixtures.FakeOracle(village), LayoutLog.SILENT);
        CityFinder finder = new CityFinder(withVillage.planner(), withVillage, new LayoutFixtures.FakeOracle(village));
        CityFinder.Result gate = finder.findBest(CityFinder.Kind.VILLAGE, block[0] << 4, block[1] << 4, 24);
        assertNotNull(gate);
        assertEquals(
            LaneRole.GATE,
            withVillage.planner()
                .plan(gate.chunkX(), gate.chunkZ())
                .lane()
                .role(),
            gate.format());
        assertTrue(
            withVillage.chunk(gate.chunkX(), gate.chunkZ())
                .isCity(),
            "sewn into the city: " + gate.format());
        double side = Math.hypot(gate.viewX() - gate.x() - 0.5, gate.viewZ() - gate.z() - 0.5);
        assertEquals(
            Math.hypot(LandmarkViews.GATE_SIDE, LandmarkViews.GATE_TOWARD_CITY),
            side,
            0.01,
            "beside the lane: " + gate.format());
        assertTrue(gate.viewY() >= gate.y() + LandmarkViews.LIFT, "above the ground on the line of sight");
        assertEquals(
            Math.round(CityFinder.yawToward(gate.viewX(), gate.viewZ(), gate.x() + 0.5, gate.z() + 0.5)),
            gate.yaw(),
            0.01f,
            "looks at the gate");
        assertTrue(gate.pitch() > 0f && gate.pitch() < 90f, "looks down: " + gate.format());
    }

    @Test
    public void theBestReservedLotAndSoftEdgeFaceTheCore() {
        int[] block = cityBlock();
        Landmark village = village(block[0], block[1]);
        LayoutCityPlan withVillage = CityFixtures
            .plan(CityFixtures.SEED, new LayoutFixtures.FakeOracle(village), LayoutLog.SILENT);
        CityFinder finder = new CityFinder(withVillage.planner(), withVillage, new LayoutFixtures.FakeOracle(village));
        CityFinder.Result lot = finder.findBest(CityFinder.Kind.RESERVED, block[0] << 4, block[1] << 4, 24);
        assertNotNull(lot);
        assertEquals(
            Zone.CORE,
            withVillage.planner()
                .zone(lot.chunkX(), lot.chunkZ())
                .zone(),
            lot.format());
        CityFinder.Result strip = finder.findBest(CityFinder.Kind.SOFTEDGE, block[0] << 4, block[1] << 4, 24);
        assertNotNull(strip);
        boolean facesCore = false;
        for (com.tuxprogrammer.lostcities.layout.Direction side : com.tuxprogrammer.lostcities.layout.Direction
            .values()) {
            facesCore |= withVillage.planner()
                .facesCore(strip.chunkX(), strip.chunkZ(), side);
        }
        assertTrue(facesCore, strip.format());
        assertNull(
            new CityFinder(withVillage.planner(), withVillage)
                .findBest(CityFinder.Kind.VILLAGE, block[0] << 4, block[1] << 4, 24),
            "without the oracle, no landmark to rank");
    }

    @Test
    public void yawLooksTowardTheTarget() {
        assertEquals(0f, CityFinder.yawToward(0, 0, 0, 10), 0.01f, "south");
        assertEquals(90f, CityFinder.yawToward(0, 0, -10, 0), 0.01f, "west");
        assertEquals(180f, CityFinder.yawToward(0, 0, 0, -10), 0.01f, "north");
        assertEquals(270f, CityFinder.yawToward(0, 0, 10, 0), 0.01f, "east");
    }
}
