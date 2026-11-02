package com.tuxprogrammer.lostcities.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.ScatteredFixtures.ColumnTerrain;
import com.tuxprogrammer.lostcities.layout.ScatteredFixtures.FakeAsset;
import com.tuxprogrammer.lostcities.layout.ScatteredFixtures.FakeEntry;
import com.tuxprogrammer.lostcities.layout.ScatteredFixtures.FakeScattered;
import com.tuxprogrammer.lostcities.layout.landmark.Landmark;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkKind;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.layout.random.QualityRandom;
import com.tuxprogrammer.lostcities.layout.view.BuildingView;
import com.tuxprogrammer.lostcities.layout.view.InfrastructureView;
import com.tuxprogrammer.lostcities.layout.view.PartContext;
import com.tuxprogrammer.lostcities.layout.view.ScatteredView;

/** {@link ScatteredPlanner}: upstream's areas, draws and placement, the zone and blend rules, and the SCAT fixes. */
public class ScatteredPlannerTest {

    private static final long SEED = 4242L;
    /** Flat ground at Y 70 (first air; the top block is 69). */
    private static final ColumnTerrain FLAT = new ColumnTerrain((x, z) -> 70);

    private static ScatteredPlanner planner(FakeScattered scattered) {
        return planner(scattered, FLAT, StructureOracle.NONE, InfrastructureView.NONE, ScatteredFixtures.noCities());
    }

    private static ScatteredPlanner planner(FakeScattered scattered, ColumnTerrain terrain, StructureOracle oracle,
        InfrastructureView infrastructure, LayoutProfile profile) {
        return new ScatteredPlanner(
            ScatteredFixtures.planner(SEED, profile, scattered, terrain, oracle, infrastructure),
            1f,
            4096);
    }

    private static FakeScattered cabins(int areaSize, float chance, int weightNone) {
        return new FakeScattered(areaSize, chance, weightNone)
            .add(new FakeEntry(new FakeAsset("lostcities:cabin").building("building1"), 10));
    }

    /** Plans of every area in a square of areas around the origin. */
    private static List<ScatteredPlan> plans(ScatteredPlanner planner, int radius) {
        List<ScatteredPlan> out = new ArrayList<>();
        for (int az = -radius; az <= radius; az++) {
            for (int ax = -radius; ax <= radius; ax++) {
                ScatteredPlan plan = planner.planOfArea(planner.area(0) + ax, planner.area(0) + az);
                if (plan.valid()) {
                    out.add(plan);
                }
            }
        }
        return out;
    }

    @Test
    public void areasAndSeedFollowUpstream() {
        ScatteredPlanner planner = planner(cabins(8, 1f, 0));
        assertEquals((0 + 2_000_000) / 8, planner.area(0));
        assertEquals((-1 + 2_000_000) / 8, planner.area(-1));
        assertEquals(SEED + 3 * 5564338337L + 7 * 25564337621L, ScatteredPlanner.planSeed(SEED, 3, 7));
    }

    @Test
    public void chanceDecidesFromTheFirstDraw() {
        ScatteredPlanner planner = planner(cabins(8, 0.7f, 0));
        int valid = 0;
        int expected = 0;
        for (int az = 0; az < 20; az++) {
            for (int ax = 0; ax < 20; ax++) {
                int x = planner.area(0) + ax * 3;
                int z = planner.area(0) + az * 3;
                if (new QualityRandom(ScatteredPlanner.planSeed(SEED, x, z)).nextFloat() < 0.7f) {
                    expected++;
                }
                valid += planner.rawPlan(x, z)
                    .valid() ? 1 : 0;
            }
        }
        assertEquals(expected, valid);
    }

    @Test
    public void multiplierZeroPlacesNothing() {
        ScatteredPlanner planner = new ScatteredPlanner(
            ScatteredFixtures.planner(
                SEED,
                ScatteredFixtures.noCities(),
                cabins(8, 1f, 0),
                FLAT,
                StructureOracle.NONE,
                InfrastructureView.NONE),
            0f,
            4096);
        assertTrue(plans(planner, 5).isEmpty());
    }

    @Test
    public void normalPlacementStaysInTheAreaAndOneChunkWholeStructure() {
        ScatteredPlanner planner = planner(cabins(8, 1f, 0));
        List<ScatteredPlan> plans = plans(planner, 6);
        assertTrue(plans.size() > 50, "plans: " + plans.size());
        for (ScatteredPlan plan : plans) {
            int originX = plan.areaX() * 8 - ScatteredPlanner.AREA_OFFSET;
            int originZ = plan.areaZ() * 8 - ScatteredPlanner.AREA_OFFSET;
            assertTrue(plan.minChunkX() >= originX && plan.minChunkX() < originX + 8, plan.toString());
            assertTrue(plan.minChunkZ() >= originZ && plan.minChunkZ() < originZ + 8, plan.toString());
            assertEquals(1, plan.width());
            assertEquals(ScatteredPlan.Rotation.NONE, plan.rotation());
            assertEquals(70, plan.lowestLevel());
            assertEquals(69, plan.groundY());
            assertNotNull(plan.share(plan.minChunkX(), plan.minChunkZ()));
            assertTrue(planner.inFootprint(plan.minChunkX(), plan.minChunkZ()));
        }
    }

    @Test
    public void neighboringStructuresNeverTouch() {
        // Area size 2: neighbors' footprints would often touch.
        ScatteredPlanner planner = planner(cabins(2, 1f, 0));
        List<ScatteredPlan> plans = plans(planner, 15);
        int dropped = 0;
        for (int i = 0; i < plans.size(); i++) {
            for (int j = i + 1; j < plans.size(); j++) {
                assertFalse(ScatteredPlanner.touches(plans.get(i), plans.get(j)), plans.get(i) + " " + plans.get(j));
            }
        }
        for (int az = -15; az <= 15; az++) {
            for (int ax = -15; ax <= 15; ax++) {
                int x = planner.area(0) + ax;
                int z = planner.area(0) + az;
                if (planner.rawPlan(x, z)
                    .valid()
                    && !planner.planOfArea(x, z)
                        .valid()) {
                    dropped++;
                }
            }
        }
        assertTrue(dropped > 0, "the fixture never made two structures touch");
    }

    @Test
    public void doesNotReproduceSCAT05_eachEntryGetsExactlyItsWeight() {
        // One entry of weight 1 and "none" of weight 1: upstream's "<=" always picked the entry.
        FakeScattered scattered = new FakeScattered(8, 1f, 1)
            .add(new FakeEntry(new FakeAsset("lostcities:cabin").building("building1"), 1));
        ScatteredPlanner planner = planner(scattered);
        int valid = 0;
        for (int az = -12; az <= 12; az++) {
            for (int ax = -12; ax <= 12; ax++) {
                valid += planner.rawPlan(planner.area(0) + ax, planner.area(0) + az)
                    .valid() ? 1 : 0;
            }
        }
        int areas = 25 * 25;
        assertTrue(valid > areas * 0.4 && valid < areas * 0.6, "valid " + valid + " of " + areas);
    }

    @Test
    public void doesNotReproduceSCAT06_zeroWeightsLeaveTheAreaEmpty() {
        FakeScattered scattered = new FakeScattered(8, 1f, 0)
            .add(new FakeEntry(new FakeAsset("lostcities:cabin").building("building1"), 0));
        assertTrue(plans(planner(scattered), 4).isEmpty());
    }

    @Test
    public void doesNotReproduceSCAT07_anEntryIsKeptWhenAnyChunkOfTheAreaFits() {
        // The biome fits only odd chunk X: never the area's even corner, which is all upstream tested.
        FakeScattered scattered = new FakeScattered(2, 1f, 0).add(
            new FakeEntry(new FakeAsset("lostcities:cabin").building("building1"), 10)
                .biome((x, z) -> Math.floorMod(x, 2) == 1));
        List<ScatteredPlan> plans = plans(planner(scattered), 12);
        assertFalse(plans.isEmpty());
        for (ScatteredPlan plan : plans) {
            assertEquals(1, Math.floorMod(plan.minChunkX(), 2), "the footprint's own biome is still tested");
        }
    }

    @Test
    public void doesNotReproduceSCAT09_singleChunkModesTakeLowestMeanAndHighest() {
        // Chunk height (the column at 8, 8) 70; the sample at (2, 2) of every chunk is 66, at (14, 14) 74.
        ColumnTerrain terrain = new ColumnTerrain((x, z) -> {
            int lx = Math.floorMod(x, 16);
            int lz = Math.floorMod(z, 16);
            return lx == 2 && lz == 2 ? 66 : lx == 14 && lz == 14 ? 74 : 70;
        });
        int[] levels = new int[3];
        ScatteredView.TerrainHeight[] modes = { ScatteredView.TerrainHeight.LOWEST, ScatteredView.TerrainHeight.AVERAGE,
            ScatteredView.TerrainHeight.HIGHEST };
        for (int i = 0; i < 3; i++) {
            FakeScattered scattered = new FakeScattered(8, 1f, 0).add(
                new FakeEntry(
                    new FakeAsset("lostcities:cabin").building("building1")
                        .height(modes[i]),
                    10));
            ScatteredPlan plan = plans(
                planner(
                    scattered,
                    terrain,
                    StructureOracle.NONE,
                    InfrastructureView.NONE,
                    ScatteredFixtures.noCities()),
                0).get(0);
            levels[i] = plan.lowestLevel();
        }
        assertEquals(66, levels[0]);
        assertEquals(70, levels[1]);
        assertEquals(74, levels[2]);
    }

    @Test
    public void doesNotReproduceSCAT10_theLastFloorIsTheTopAndTheLevelIsACityLevel() {
        Recording building = new Recording("building1", 3);
        ScatteredPlan plan = plans(plannerWith(building), 0).get(0);
        ScatteredPlan.Share share = plan.shares()
            .get(0);
        assertEquals(3, share.floors());
        assertEquals("building1_roof", share.part(2), "upstream never matched a top-of-building part");
        // Lowest level 70 is city level 0 at the default ground level 71 (floor division); floor f is level f − 1.
        int expected = Math.floorDiv(70 - LayoutProfile.defaults().groundLevel, 6);
        assertEquals(expected, building.levels.get(0));
        assertEquals(expected + 2, building.levels.get(2));
    }

    @Test
    public void doesNotReproduceSCAT11_theOverlayPickSeesTheChosenPart() {
        Recording building = new Recording("building1", 2);
        ScatteredPlan plan = plans(plannerWith(building), 0).get(0);
        ScatteredPlan.Share share = plan.shares()
            .get(0);
        assertEquals(share.part(0), building.overlayContexts.get(0));
        assertEquals(share.part(1), building.overlayContexts.get(1));
        assertEquals("over_" + share.part(0), share.part2(0));
    }

    @Test
    public void doesNotReproduceSCAT12_plansArePureWhateverOrderTheyAreAskedIn() {
        ColumnTerrain terrain = new ColumnTerrain((x, z) -> 70 + Math.floorMod(x * 7 + z * 3, 3));
        FakeScattered scattered = cabins(8, 0.8f, 5);
        ScatteredPlanner forward = planner(
            scattered,
            terrain,
            StructureOracle.NONE,
            InfrastructureView.NONE,
            ScatteredFixtures.noCities());
        ScatteredPlanner backward = planner(
            scattered,
            terrain,
            StructureOracle.NONE,
            InfrastructureView.NONE,
            ScatteredFixtures.noCities());
        List<String> a = new ArrayList<>();
        List<String> b = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            a.add(describe(forward.planOfArea(1000 + i % 10, 1000 + i / 10)));
        }
        for (int i = 99; i >= 0; i--) {
            b.add(0, describe(backward.planOfArea(1000 + i % 10, 1000 + i / 10)));
        }
        assertEquals(a, b);
    }

    @Test
    public void doesNotReproduceSCAT14_theCacheIsBoundedAndADroppedPlanComesBackTheSame() {
        ScatteredPlanner planner = new ScatteredPlanner(
            ScatteredFixtures.planner(
                SEED,
                ScatteredFixtures.noCities(),
                cabins(8, 0.7f, 5),
                FLAT,
                StructureOracle.NONE,
                InfrastructureView.NONE),
            1f,
            64);
        String first = describe(planner.planOfArea(5, 5));
        for (int i = 0; i < 1000; i++) {
            planner.planOfArea(100 + i, 7);
        }
        assertEquals(first, describe(planner.planOfArea(5, 5)));
    }

    @Test
    public void doesNotReproduceSCAT15_theHighwaySearchReadsNoTerrainWithoutAHighway() {
        ColumnTerrain terrain = new ColumnTerrain((x, z) -> 70);
        FakeScattered scattered = new FakeScattered(8, 1f, 0).add(
            new FakeEntry(
                new FakeAsset("lostcities:highway_gas_station").building("building1")
                    .anyRotation(),
                10).besideHighway());
        ScatteredPlanner planner = planner(
            scattered,
            terrain,
            StructureOracle.NONE,
            InfrastructureView.NONE,
            ScatteredFixtures.noCities());
        assertTrue(plans(planner, 2).isEmpty());
        assertEquals(0, terrain.calls, "candidates without a highway stop before their terrain");
    }

    @Test
    public void doesNotReproduceSCAT01_footprintsKeepOutOfEveryLandmarkZone() {
        Landmark village = LayoutFixtures.landmark("village@5,5", LandmarkKind.VILLAGE, 84, 84, 91, 91);
        ScatteredPlanner planner = planner(
            cabins(2, 1f, 0),
            FLAT,
            new LayoutFixtures.FakeOracle(village),
            InfrastructureView.NONE,
            ScatteredFixtures.noCities());
        for (int z = -6; z <= 16; z++) {
            for (int x = -6; x <= 16; x++) {
                // The core (5, 5) and every chunk within the blend's reach of it stay clear.
                boolean near = Math.abs(x - 5) <= ScatteredPlanner.BLEND_CLEARANCE
                    && Math.abs(z - 5) <= ScatteredPlanner.BLEND_CLEARANCE;
                if (near) {
                    assertFalse(planner.inFootprint(x, z), x + "," + z);
                }
            }
        }
    }

    @Test
    public void doesNotReproduceSCAT02_theFootprintDecisionDoesNotDependOnOrder() {
        Landmark village = LayoutFixtures.landmark("village@5,5", LandmarkKind.VILLAGE, 84, 84, 91, 91);
        FakeScattered scattered = cabins(2, 1f, 0);
        ScatteredPlanner a = planner(
            scattered,
            FLAT,
            new LayoutFixtures.FakeOracle(village),
            InfrastructureView.NONE,
            ScatteredFixtures.noCities());
        ScatteredPlanner b = planner(
            scattered,
            FLAT,
            new LayoutFixtures.FakeOracle(village),
            InfrastructureView.NONE,
            ScatteredFixtures.noCities());
        StringBuilder forward = new StringBuilder();
        StringBuilder backward = new StringBuilder();
        for (int x = -8; x <= 18; x++) {
            forward.append(a.inFootprint(x, 5) ? 'S' : '.');
        }
        for (int x = 18; x >= -8; x--) {
            backward.insert(0, b.inFootprint(x, 5) ? 'S' : '.');
        }
        assertEquals(forward.toString(), backward.toString());
    }

    @Test
    public void doesNotReproduceSCAT03_cachesStayBounded() {
        ScatteredPlanner planner = new ScatteredPlanner(
            ScatteredFixtures.planner(
                SEED,
                ScatteredFixtures.noCities(),
                cabins(8, 1f, 0),
                FLAT,
                StructureOracle.NONE,
                InfrastructureView.NONE),
            1f,
            64);
        String first = describe(planner.planOfArea(-3, 9));
        for (int i = 0; i < 500; i++) {
            planner.planOfArea(i, -i);
        }
        assertEquals(first, describe(planner.planOfArea(-3, 9)));
    }

    @Test
    public void footprintsKeepTheBlendsReachFromCities() {
        ScatteredPlanner planner = planner(
            cabins(4, 1f, 0),
            FLAT,
            StructureOracle.NONE,
            InfrastructureView.NONE,
            LayoutProfile.defaults());
        LayoutPlanner layout = ScatteredFixtures.planner(
            SEED,
            LayoutProfile.defaults(),
            cabins(4, 1f, 0),
            FLAT,
            StructureOracle.NONE,
            InfrastructureView.NONE);
        int structures = 0;
        int cities = 0;
        for (int z = -60; z <= 60; z++) {
            for (int x = -60; x <= 60; x++) {
                if (layout.isCityRaw(x, z)) {
                    cities++;
                }
                if (!planner.inFootprint(x, z)) {
                    continue;
                }
                structures++;
                for (int dz = -ScatteredPlanner.BLEND_CLEARANCE; dz <= ScatteredPlanner.BLEND_CLEARANCE; dz++) {
                    for (int dx = -ScatteredPlanner.BLEND_CLEARANCE; dx <= ScatteredPlanner.BLEND_CLEARANCE; dx++) {
                        assertFalse(
                            layout.plan(x + dx, z + dz)
                                .isCity(),
                            "city within the clearance of " + x + "," + z);
                    }
                }
            }
        }
        assertTrue(cities > 0 && structures > 0, "cities " + cities + ", structures " + structures);
    }

    @Test
    public void oceanStructuresNeedDeepWater() {
        FakeScattered rigs = new FakeScattered(8, 1f, 0).add(
            new FakeEntry(
                new FakeAsset("lostcities:oilrig").building("building1")
                    .height(ScatteredView.TerrainHeight.OCEAN)
                    .offset(1),
                10));
        assertTrue(
            plans(
                planner(
                    rigs,
                    new ColumnTerrain((x, z) -> 50),
                    StructureOracle.NONE,
                    InfrastructureView.NONE,
                    ScatteredFixtures.noCities()),
                3).isEmpty(),
            "shallow water");
        List<ScatteredPlan> deep = plans(
            planner(
                rigs,
                new ColumnTerrain((x, z) -> 40),
                StructureOracle.NONE,
                InfrastructureView.NONE,
                ScatteredFixtures.noCities()),
            3);
        assertFalse(deep.isEmpty());
        assertEquals(
            ScatteredPlanner.SEA_LEVEL + 1,
            deep.get(0)
                .lowestLevel());
    }

    @Test
    public void aStructureThatWouldLeaveTheWorldIsRejected() {
        FakeScattered tall = new FakeScattered(8, 1f, 0).add(
            new FakeEntry(
                new FakeAsset("lostcities:radiotower").building("building1")
                    .maxHeight(30),
                10));
        assertTrue(
            plans(
                planner(
                    tall,
                    new ColumnTerrain((x, z) -> 230),
                    StructureOracle.NONE,
                    InfrastructureView.NONE,
                    ScatteredFixtures.noCities()),
                3).isEmpty());
        assertFalse(
            plans(
                planner(
                    tall,
                    new ColumnTerrain((x, z) -> 200),
                    StructureOracle.NONE,
                    InfrastructureView.NONE,
                    ScatteredFixtures.noCities()),
                3).isEmpty());
    }

    @Test
    public void steepSurroundingsAreRejectedSoTheFitNeverMakesAWall() {
        // Even chunk columns stand at one height, odd ones at another: every footprint's rings land on the other.
        FakeScattered cabins = cabins(8, 1f, 0);
        ColumnTerrain cliffs = new ColumnTerrain((x, z) -> Math.floorMod(x >> 4, 2) == 0 ? 70 : 90);
        assertTrue(
            plans(
                planner(cabins, cliffs, StructureOracle.NONE, InfrastructureView.NONE, ScatteredFixtures.noCities()),
                3).isEmpty());
        ColumnTerrain gentle = new ColumnTerrain((x, z) -> Math.floorMod(x >> 4, 2) == 0 ? 70 : 76);
        assertFalse(
            plans(
                planner(cabins, gentle, StructureOracle.NONE, InfrastructureView.NONE, ScatteredFixtures.noCities()),
                3).isEmpty());
        // A structure on supports only minds higher ground: on the high chunks, the low ones around it are fine.
        FakeScattered supported = new FakeScattered(8, 1f, 0).add(
            new FakeEntry(
                new FakeAsset("lostcities:platform").building("building1")
                    .fix(ScatteredView.TerrainFix.REPEATSLICE),
                10));
        List<ScatteredPlan> high = plans(
            planner(supported, cliffs, StructureOracle.NONE, InfrastructureView.NONE, ScatteredFixtures.noCities()),
            3);
        assertFalse(high.isEmpty());
        for (ScatteredPlan plan : high) {
            assertEquals(1, Math.floorMod(plan.minChunkX(), 2));
        }
    }

    @Test
    public void landStructuresNeverStandBelowTheSea() {
        FakeScattered cabins = cabins(8, 1f, 0);
        // Ground (the first air) at 62 puts the top block at 61, under the water up to Y 62.
        assertTrue(
            plans(
                planner(
                    cabins,
                    new ColumnTerrain((x, z) -> 62),
                    StructureOracle.NONE,
                    InfrastructureView.NONE,
                    ScatteredFixtures.noCities()),
                2).isEmpty());
        assertFalse(
            plans(
                planner(
                    cabins,
                    new ColumnTerrain((x, z) -> 63),
                    StructureOracle.NONE,
                    InfrastructureView.NONE,
                    ScatteredFixtures.noCities()),
                2).isEmpty());
    }

    @Test
    public void maxHeightDiffRejectsSteepGround() {
        FakeScattered steep = new FakeScattered(8, 1f, 0)
            .add(new FakeEntry(new FakeAsset("lostcities:cabin").building("building1"), 10).maxDiff(2));
        ColumnTerrain slope = new ColumnTerrain((x, z) -> 70 + Math.floorMod(x, 16) / 4);
        assertTrue(
            plans(planner(steep, slope, StructureOracle.NONE, InfrastructureView.NONE, ScatteredFixtures.noCities()), 3)
                .isEmpty());
    }

    @Test
    public void multibuildingChunksMapBackThroughTheRotationAsUpstream() {
        com.tuxprogrammer.lostcities.layout.view.MultiBuildingView multi = new com.tuxprogrammer.lostcities.layout.view.MultiBuildingView() {

            @Override
            public String name() {
                return "m";
            }

            @Override
            public int dimX() {
                return 2;
            }

            @Override
            public int dimZ() {
                return 3;
            }

            @Override
            public String buildingAt(int x, int z) {
                return x + "," + z;
            }
        };
        assertEquals("1,0", ScatteredPlanner.buildingAt(multi, 1, 0, ScatteredPlan.Rotation.NONE));
        // ROTATE_90: (z, dimZ − 1 − x); ROTATE_180: (dimX − 1 − x, dimZ − 1 − z); ROTATE_270: (dimX − 1 − z, x).
        assertEquals("0,1", ScatteredPlanner.buildingAt(multi, 1, 0, ScatteredPlan.Rotation.ROTATE_90));
        assertEquals("0,2", ScatteredPlanner.buildingAt(multi, 1, 0, ScatteredPlan.Rotation.ROTATE_180));
        assertEquals("1,1", ScatteredPlanner.buildingAt(multi, 1, 0, ScatteredPlan.Rotation.ROTATE_270));
    }

    @Test
    public void everyChunkOfAMultibuildingReplaysTheSameDraws() {
        Recording building = new Recording("b", 1);
        building.maxFloors = 5;
        building.minFloors = 1;
        LayoutFixtures.FakeAssets base = LayoutFixtures.standardAssets();
        base.add(new LayoutFixtures.FakeMulti("lostcities:rig", 2, 2, "b"));
        FakeScattered scattered = new FakeScattered(8, 1f, 0)
            .add(new FakeEntry(new FakeAsset("lostcities:rig").multi("lostcities:rig"), 10));
        ScatteredPlanner planner = new ScatteredPlanner(
            new LayoutPlanner(
                LayoutInputs.builder(SEED, ScatteredFixtures.noCities(), withBuilding(base, scattered, building), FLAT)
                    .build()),
            1f,
            4096);
        List<ScatteredPlan> plans = plans(planner, 3);
        assertFalse(plans.isEmpty());
        for (ScatteredPlan plan : plans) {
            assertEquals(
                4,
                plan.shares()
                    .size());
            int floors = plan.shares()
                .get(0)
                .floors();
            for (ScatteredPlan.Share share : plan.shares()) {
                assertEquals(floors, share.floors());
            }
        }
    }

    @Test
    public void highwayPlacementFacesTheHighwayAtItsHeight() {
        // A highway along X at chunk row Z = 3, city level 1: block Y 71 + 6.
        FakeScattered gas = new FakeScattered(8, 1f, 0).add(
            new FakeEntry(
                new FakeAsset("lostcities:highway_gas_station").building("building1")
                    .anyRotation()
                    .opensRailing(),
                10).besideHighway());
        ScatteredPlanner planner = planner(
            gas,
            FLAT,
            StructureOracle.NONE,
            ScatteredFixtures.highwayAlongX(3, 1),
            ScatteredFixtures.noCities());
        int found = 0;
        for (int ax = -4; ax <= 4; ax++) {
            ScatteredPlan plan = planner.planOfArea(planner.area(ax * 8), planner.area(0));
            if (!plan.valid()) {
                continue;
            }
            found++;
            int height = LayoutProfile.defaults().groundLevel + 6;
            assertEquals(Integer.valueOf(height), plan.highwayHeight());
            assertEquals(height, plan.lowestLevel());
            boolean north = plan.rotation() == ScatteredPlan.Rotation.NONE && plan.minChunkZ() == 4;
            boolean south = plan.rotation() == ScatteredPlan.Rotation.ROTATE_180 && plan.minChunkZ() == 2;
            assertTrue(north || south, plan.toString());
            int highwayZ = 3;
            Direction toward = north ? Direction.ZMAX : Direction.ZMIN;
            assertTrue(planner.opensRailing(plan.minChunkX(), highwayZ, toward, height));
            assertFalse(planner.opensRailing(plan.minChunkX(), highwayZ, toward, height + 6));
        }
        assertTrue(found > 0);
    }

    @Test
    public void anAreaHasAHighwaySpotExactlyWhenTheHighwayReachesIt() {
        FakeScattered gas = new FakeScattered(8, 1f, 0)
            .add(
                new FakeEntry(
                    new FakeAsset("lostcities:highway_gas_station").building("building1")
                        .anyRotation(),
                    10).besideHighway())
            .add(new FakeEntry(new FakeAsset("lostcities:cabin").building("building1"), 10));
        ScatteredPlanner planner = planner(
            gas,
            FLAT,
            StructureOracle.NONE,
            ScatteredFixtures.highwayAlongX(3, 1),
            ScatteredFixtures.noCities());
        int beside = planner.area(3);
        // The highway's own area and its neighbors that a footprint can face it from; none far from it.
        assertTrue(planner.hasHighwaySpot("lostcities:highway_gas_station", 0, beside));
        assertFalse(planner.hasHighwaySpot("lostcities:highway_gas_station", 0, beside + 3));
        assertFalse(planner.hasHighwaySpot("lostcities:cabin", 0, beside), "a cabin does not stand beside highways");
        assertFalse(planner.hasHighwaySpot("lostcities:unknown", 0, beside));
        // Every placed gas station is in an area that has a spot.
        for (int ax = -4; ax <= 4; ax++) {
            for (int az = beside - 2; az <= beside + 2; az++) {
                ScatteredPlan plan = planner.planOfArea(ax, az);
                if (plan.valid() && plan.entry()
                    .equals("lostcities:highway_gas_station")) {
                    assertTrue(planner.hasHighwaySpot(plan.entry(), ax, az), plan.toString());
                }
            }
        }
    }

    private static String describe(ScatteredPlan plan) {
        StringBuilder out = new StringBuilder(plan.toString());
        for (ScatteredPlan.Share share : plan.shares()) {
            out.append(' ')
                .append(share.building());
            for (int f = 0; f < share.floors(); f++) {
                out.append('/')
                    .append(share.part(f));
            }
        }
        return out.toString();
    }

    private static ScatteredPlanner plannerWith(Recording building) {
        LayoutFixtures.FakeAssets base = LayoutFixtures.standardAssets();
        FakeScattered scattered = new FakeScattered(8, 1f, 0)
            .add(new FakeEntry(new FakeAsset("lostcities:cabin").building(building.name), 10));
        return new ScatteredPlanner(
            new LayoutPlanner(
                LayoutInputs.builder(SEED, ScatteredFixtures.noCities(), withBuilding(base, scattered, building), FLAT)
                    .build()),
            1f,
            4096);
    }

    private static com.tuxprogrammer.lostcities.layout.view.AssetView withBuilding(LayoutFixtures.FakeAssets base,
        FakeScattered scattered, Recording building) {
        com.tuxprogrammer.lostcities.layout.view.AssetView view = ScatteredFixtures.withScattered(base, scattered);
        return new com.tuxprogrammer.lostcities.layout.view.AssetView() {

            @Override
            public com.tuxprogrammer.lostcities.layout.view.WorldStyleView worldStyle() {
                return view.worldStyle();
            }

            @Override
            public com.tuxprogrammer.lostcities.layout.view.CityStyleView cityStyle(String name) {
                return view.cityStyle(name);
            }

            @Override
            public BuildingView building(String name) {
                return building.name.equals(name) ? building : view.building(name);
            }

            @Override
            public com.tuxprogrammer.lostcities.layout.view.MultiBuildingView multiBuilding(String name) {
                return view.multiBuilding(name);
            }

            @Override
            public com.tuxprogrammer.lostcities.layout.view.StyleView style(String name) {
                return view.style(name);
            }

            @Override
            public boolean partDontConnect(String partName) {
                return view.partDontConnect(partName);
            }
        };
    }

    /** A building that records the contexts it is asked with; its overlay is "over_" plus the chosen part. */
    private static final class Recording implements BuildingView {

        final String name;
        int minFloors;
        int maxFloors;
        final List<Integer> levels = new ArrayList<>();
        final List<String> overlayContexts = new ArrayList<>();

        Recording(String name, int floors) {
            this.name = name;
            this.minFloors = floors;
            this.maxFloors = floors;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public int minFloors() {
            return minFloors;
        }

        @Override
        public int maxFloors() {
            return maxFloors;
        }

        @Override
        public int minCellars() {
            return UNSET;
        }

        @Override
        public int maxCellars() {
            return UNSET;
        }

        @Override
        public boolean overrideFloors() {
            return false;
        }

        @Override
        public float prefersLonely() {
            return 0;
        }

        @Override
        public String pickPart(Random random, PartContext context) {
            levels.add(context.level());
            random.nextInt(2);
            return context.isTopOfBuilding() ? name + "_roof" : name + "_floor";
        }

        @Override
        public String pickPart2(Random random, PartContext context) {
            overlayContexts.add(context.part());
            return "over_" + context.part();
        }
    }

    @Test
    public void aMissingBuildingLeavesTheAreaEmpty() {
        FakeScattered scattered = new FakeScattered(8, 1f, 0)
            .add(new FakeEntry(new FakeAsset("lostcities:cabin").building("no_such_building"), 10));
        assertTrue(plans(planner(scattered), 2).isEmpty());
        assertNull(planner(scattered).planAt(0, 0));
    }
}
