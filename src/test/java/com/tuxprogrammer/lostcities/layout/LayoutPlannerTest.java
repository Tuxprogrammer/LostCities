package com.tuxprogrammer.lostcities.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.landmark.LandmarkKind;
import com.tuxprogrammer.lostcities.layout.landmark.Zone;
import com.tuxprogrammer.lostcities.layout.predefined.PredefinedBuilding;
import com.tuxprogrammer.lostcities.layout.predefined.PredefinedCity;
import com.tuxprogrammer.lostcities.layout.predefined.PredefinedIndex;
import com.tuxprogrammer.lostcities.layout.random.DimensionKey;
import com.tuxprogrammer.lostcities.layout.street.HierarchicalStreetPlanner;
import com.tuxprogrammer.lostcities.layout.street.Orientation;
import com.tuxprogrammer.lostcities.layout.street.PlannedRoadType;
import com.tuxprogrammer.lostcities.layout.street.PlannedStreetInfo;
import com.tuxprogrammer.lostcities.layout.street.RoadDirection;
import com.tuxprogrammer.lostcities.layout.view.InfrastructureView;
import com.tuxprogrammer.lostcities.layout.view.RailChunkType;
import com.tuxprogrammer.lostcities.layout.view.RailInfo;
import com.tuxprogrammer.lostcities.layout.view.Selection;

/**
 * {@link LayoutPlanner}: the per-chunk plan, its neighbor queries and every upstream bug the port fixes (each a
 * {@code doesNotReproduce} test).
 */
public class LayoutPlannerTest {

    private static final int R = 12;

    /** A planner over one big flat predefined city (city out to about 24 chunks) and the given assets. */
    private static LayoutPlanner bigCity(long seed, LayoutProfile profile, LayoutFixtures.FakeAssets assets) {
        return new LayoutPlanner(
            LayoutInputs.builder(seed, profile, assets, LayoutFixtures.flatTerrain())
                .predefined(LayoutFixtures.bigCity(0, 0, 480, "citystyle_common"))
                .build());
    }

    private static LayoutProfile noRandomCities() {
        return LayoutFixtures.predefinedOnly();
    }

    /** Replaces the common style's building list with one building. */
    private static LayoutFixtures.FakeAssets onlyBuilding(LayoutFixtures.FakeBuilding building) {
        LayoutFixtures.FakeAssets assets = LayoutFixtures.standardAssets();
        assets.add(building);
        LayoutFixtures.FakeCityStyle style = assets.cityStyles.get("citystyle_common");
        style.lists.get(Selection.BUILDINGS)
            .clear();
        style.with(Selection.BUILDINGS, building.name, 1f);
        style.lists.get(Selection.MULTIBUILDINGS)
            .clear();
        return assets;
    }

    private static List<ChunkPlan> region(LayoutPlanner p) {
        List<ChunkPlan> out = new ArrayList<>();
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                out.add(p.plan(x, z));
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ plan basics

    @Test
    public void buildingsHaveConsistentLevels() {
        LayoutPlanner p = bigCity(1L, noRandomCities(), LayoutFixtures.standardAssets());
        int buildings = 0;
        for (ChunkPlan plan : region(p)) {
            assertTrue(plan.isCity(), "the big city covers the region");
            if (plan.hasBuilding()) {
                buildings++;
                assertEquals(plan.floors() + plan.cellars() + 1, plan.levels());
                assertTrue(
                    plan.floorPart(plan.floors())
                        .endsWith("_roof"),
                    plan.toString());
                if (plan.cellars() > 0) {
                    assertTrue(
                        plan.floorPart(-1)
                            .endsWith("_cellar"));
                }
                assertTrue(plan.cityGroundLevel() + (plan.floors() + 1) * 6 <= 256);
                assertFalse(plan.isPlannedRoad());
            } else {
                assertTrue(plan.isStreetOrParkSection());
            }
            if (plan.isHierarchicalOpen()) {
                assertEquals(StreetType.PARK, plan.streetType());
            }
            if (plan.isPlannedRoad()) {
                assertEquals(StreetType.NORMAL, plan.streetType());
                assertEquals(null, plan.fountainPart());
            }
        }
        assertTrue(buildings > 100, "buildings " + buildings);
    }

    @Test
    public void streetsArePlannedFromTheRawField() {
        LayoutPlanner p = bigCity(2L, noRandomCities(), LayoutFixtures.standardAssets());
        int roads = 0;
        int primaries = 0;
        for (ChunkPlan plan : region(p)) {
            if (plan.isPlannedRoad()) {
                roads++;
                assertEquals(plan.rawRoad(), plan.plannedRoad());
                primaries += plan.isPrimaryRoad() ? 1 : 0;
            } else if (plan.rawRoad()
                .isRoad()
                && plan.multiPos()
                    .isSingle()) {
                        fail("a raw road in a single city chunk is clipped only by a building over it: " + plan);
                    }
        }
        assertTrue(roads > 50 && primaries > 10, roads + " roads, " + primaries + " primaries");
    }

    private static void fail(String message) {
        org.junit.jupiter.api.Assertions.fail(message);
    }

    @Test
    public void multibuildingChunksShareTheirTopLeftsDecisions() {
        LayoutPlanner p = bigCity(3L, noRandomCities(), LayoutFixtures.standardAssets());
        int multi = 0;
        for (ChunkPlan plan : region(p)) {
            MultiPos pos = plan.multiPos();
            if (pos.isMulti() && !pos.isTopLeft()) {
                ChunkPlan top = p.plan(plan.chunkX() - pos.x(), plan.chunkZ() - pos.z());
                assertEquals(top.floors(), plan.floors());
                assertEquals(top.cellars(), plan.cellars());
                assertEquals(top.cityLevel(), plan.cityLevel());
                assertEquals(top.door(), plan.door());
                assertEquals(top.hasBuilding(), plan.hasBuilding());
                multi++;
            }
        }
        assertTrue(multi > 0);
    }

    @Test
    public void doesNotReproduceLOOT10_multibuildingsRollTheirLootToo() {
        LayoutProfile noLoot = LayoutProfile.builder()
            .cityChance(0)
            .buildingChances(0.3f, 1.0f, 0.2f)
            .build();
        LayoutPlanner p = bigCity(3L, noLoot, LayoutFixtures.standardAssets());
        int multi = 0;
        for (ChunkPlan plan : region(p)) {
            if (plan.hasBuilding() && plan.multiPos()
                .isMulti()) {
                multi++;
                // Upstream rolled noLoot only for single-chunk buildings, so a multibuilding always had loot.
                assertTrue(plan.noLoot(), plan.toString());
            }
        }
        assertTrue(multi > 0);
    }

    @Test
    public void plansAreCachedAndFinal() {
        LayoutPlanner p = bigCity(4L, noRandomCities(), LayoutFixtures.standardAssets());
        ChunkPlan first = p.plan(3, 3);
        p.layout(3, 3);
        p.layout(4, 3);
        assertSame(first, p.plan(3, 3));
    }

    // ------------------------------------------------------------------ neighbor queries

    @Test
    public void slopesStairsAndCorridorsKeepTheirInvariants() {
        LayoutPlanner p = new LayoutPlanner(
            LayoutInputs.builder(5L, noRandomCities(), LayoutFixtures.standardAssets(), LayoutFixtures.rollingTerrain())
                .predefined(LayoutFixtures.bigCity(0, 0, 480, "citystyle_common"))
                .build());
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                ChunkPlan plan = p.plan(x, z);
                Direction slope = p.streetSlope(x, z);
                if (slope != null) {
                    ChunkPlan upper = p.plan(x + slope.stepX(), z + slope.stepZ());
                    assertEquals(plan.cityLevel() + 1, upper.cityLevel());
                    assertTrue(p.doesRoadExtendTo(plan) && p.doesRoadExtendTo(upper));
                    assertTrue(p.hasRoadConnection(plan, upper));
                }
                Direction stair = p.stairDirection(x, z);
                if (stair != null) {
                    ChunkPlan above = p.plan(x + stair.stepX(), z + stair.stepZ());
                    assertEquals(plan.cityLevel() + 1, above.cityLevel());
                    assertFalse(above.hasBuilding());
                    assertEquals(null, slope);
                }
                for (Orientation o : Orientation.values()) {
                    if (p.hasCorridor(x, z, o)) {
                        assertTrue(o == Orientation.X ? plan.xRailCorridor() : plan.zRailCorridor());
                    }
                }
            }
        }
    }

    @Test
    public void primaryRoadsBridgeAWaterGap() {
        // Three columns of water and low ground (Y 40, not city) cut the city, where no north-south primary crosses.
        HierarchicalStreetPlanner streets = new HierarchicalStreetPlanner(
            6L,
            DimensionKey.OVERWORLD,
            LayoutProfile.defaults().streets);
        int gap = 2;
        while (streets.isVerticalPrimary(gap) || streets.isVerticalPrimary(gap + 1)
            || streets.isVerticalPrimary(gap + 2)) {
            gap++;
        }
        final int first = gap;
        LayoutFixtures.FakeTerrain terrain = new LayoutFixtures.FakeTerrain(
            (x, z) -> x >= first && x <= first + 2 ? 40 : 70).water(first, -100, first + 2, 100);
        LayoutPlanner p = new LayoutPlanner(
            LayoutInputs.builder(6L, noRandomCities(), LayoutFixtures.standardAssets(), terrain)
                .predefined(LayoutFixtures.bigCity(0, 0, 480, "citystyle_common"))
                .build());
        int bridged = 0;
        for (int z = -R; z <= R; z++) {
            if (streets.isHorizontalPrimary(z) && p.plan(first - 1, z)
                .isPrimaryRoad()
                && p.plan(first + 3, z)
                    .isPrimaryRoad()) {
                assertNotNull(p.bridge(first + 1, z, Orientation.X), "row " + z);
                assertEquals("bridge_large", p.bridgePart(first + 1, z, Orientation.X));
                assertEquals(
                    "bridge_large",
                    p.layout(first, z)
                        .bridgePart(Orientation.X));
                bridged++;
            }
        }
        assertTrue(bridged > 0);
    }

    // ------------------------------------------------------------------ bug regressions

    @Test
    public void doesNotReproduceCITY10_firstLonelyDrawIsNotTheBuildingChanceDraw() {
        // prefersLonely 0.35 >= buildingChance 0.3: upstream's first prefers-lonely draw repeated bc (< 0.3), so a
        // chunk whose west neighbor could hold a building never got one.
        LayoutFixtures.FakeAssets assets = onlyBuilding(new LayoutFixtures.FakeBuilding("lonely").prefersLonely(.35f));
        LayoutPlanner p = bigCity(10L, noRandomCities(), assets);
        int candidates = 0;
        int built = 0;
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                ChunkCharacteristics c = p.characteristics(x, z);
                if (c.couldHaveBuilding() && c.multiPos()
                    .isSingle()
                    && p.characteristics(x - 1, z)
                        .couldHaveBuilding()) {
                    candidates++;
                    built += p.plan(x, z)
                        .hasBuilding() ? 1 : 0;
                }
            }
        }
        assertTrue(candidates > 20, "candidates " + candidates);
        assertTrue(built > 0, "no building next to a west neighbor that could hold one");
    }

    @Test
    public void doesNotReproduceCITY11_onlyNeighborsThatCouldHoldABuildingCount() {
        // prefersLonely 1: any counted neighbor suppresses. Upstream counted every neighbor's candidate building.
        LayoutFixtures.FakeAssets assets = onlyBuilding(new LayoutFixtures.FakeBuilding("hermit").prefersLonely(1f));
        LayoutPlanner p = bigCity(11L, noRandomCities(), assets);
        int alone = 0;
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                ChunkCharacteristics c = p.characteristics(x, z);
                if (!c.couldHaveBuilding()) {
                    continue;
                }
                boolean neighbors = false;
                for (Direction d : Direction.values()) {
                    neighbors |= p.characteristics(x + d.stepX(), z + d.stepZ())
                        .couldHaveBuilding();
                }
                if (!neighbors) {
                    alone++;
                    assertTrue(
                        p.plan(x, z)
                            .hasBuilding(),
                        x + "," + z);
                } else {
                    assertFalse(
                        p.plan(x, z)
                            .hasBuilding(),
                        x + "," + z);
                }
            }
        }
        assertTrue(alone > 0);
    }

    @Test
    public void doesNotReproduceTERR07_fullStreetsAreReachableAsPlazas() {
        LayoutPlanner probe = bigCity(12L, noRandomCities(), LayoutFixtures.standardAssets());
        int[] found = findPlazaSite(probe);
        assertNotNull(found, "no secondary or tertiary road leading into a non-primary chunk");
        int roadX = found[0];
        int roadZ = found[1];
        int coreX = found[2];
        int coreZ = found[3];
        LayoutPlanner p = new LayoutPlanner(
            LayoutInputs.builder(12L, noRandomCities(), LayoutFixtures.standardAssets(), LayoutFixtures.flatTerrain())
                .predefined(LayoutFixtures.bigCity(0, 0, 480, "citystyle_common"))
                .structures(
                    new LayoutFixtures.FakeOracle(
                        LayoutFixtures.landmark(
                            "room",
                            LandmarkKind.LOOTGAMES,
                            coreX * 16 + 4,
                            coreZ * 16 + 4,
                            coreX * 16 + 11,
                            coreZ * 16 + 11)))
                .build());
        assertEquals(
            Zone.CORE,
            p.zone(coreX, coreZ)
                .zone());
        ChunkPlan plaza = p.plan(roadX, roadZ);
        assertEquals(
            Zone.EDGE_RING,
            p.zone((roadX + coreX) / 2, (roadZ + coreZ) / 2)
                .zone(),
            "the margin lies between the plaza and the core");
        assertTrue(plaza.isCity());
        assertEquals(StreetType.FULL, plaza.streetType());
        assertTrue(plaza.isPlannedRoad());
    }

    /**
     * A road chunk with two raw connections whose chunk two out along one of them could be a core, with the margin
     * between: no primary near it.
     */
    private static int[] findPlazaSite(LayoutPlanner p) {
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                ChunkPlan plan = p.plan(x, z);
                if (plan.plannedRoad() != PlannedRoadType.SECONDARY && plan.plannedRoad() != PlannedRoadType.TERTIARY) {
                    continue;
                }
                PlannedStreetInfo raw = p.streets()
                    .getStreetInfo(x, z);
                int connections = 0;
                for (RoadDirection d : RoadDirection.values()) {
                    connections += raw.connects(d) ? 1 : 0;
                }
                if (connections < 2) {
                    continue;
                }
                for (RoadDirection d : RoadDirection.values()) {
                    // The core two chunks out, the margin between; no primary crosses the margin's bounds or the road.
                    int cx = x + 2 * d.stepX();
                    int cz = z + 2 * d.stepZ();
                    boolean primaryNear = p.streets()
                        .isVerticalPrimary(x)
                        || p.streets()
                            .isHorizontalPrimary(z);
                    for (int i = -1; i <= 1; i++) {
                        primaryNear |= p.streets()
                            .isVerticalPrimary(cx + i)
                            || p.streets()
                                .isHorizontalPrimary(cz + i);
                    }
                    if (raw.connects(d) && !primaryNear) {
                        return new int[] { x, z, cx, cz };
                    }
                }
            }
        }
        return null;
    }

    @Test
    public void streetTypesAreFinal_answersDoNotDependOnWhatWasPlannedFirst() {
        LayoutPlanner askFirst = bigCity(13L, noRandomCities(), LayoutFixtures.standardAssets());
        LayoutPlanner planFirst = bigCity(13L, noRandomCities(), LayoutFixtures.standardAssets());
        List<String> a = new ArrayList<>();
        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 6; z++) {
                for (Direction d : Direction.values()) {
                    a.add(x + "," + z + d + askFirst.hasFrontPartFrom(x, z, d) + askFirst.isElevatedParkSection(x, z));
                }
            }
        }
        region(planFirst);
        List<String> b = new ArrayList<>();
        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 6; z++) {
                for (Direction d : Direction.values()) {
                    b.add(
                        x + "," + z + d + planFirst.hasFrontPartFrom(x, z, d) + planFirst.isElevatedParkSection(x, z));
                }
            }
        }
        assertEquals(a, b);
    }

    @Test
    public void doesNotReproducePROF06_invertedFloorChancesNeverThrow() {
        LayoutProfile inverted = LayoutProfile.builder()
            .cityChance(0)
            .floors(0, 8, 6, 1)
            .build();
        LayoutPlanner p = bigCity(14L, inverted, LayoutFixtures.standardAssets());
        int buildings = 0;
        for (ChunkPlan plan : region(p)) {
            if (plan.hasBuilding()) {
                buildings++;
                assertTrue(plan.floors() >= 1);
            }
        }
        assertTrue(buildings > 0);
    }

    @Test
    public void doesNotReproduceCITY15_heightCapStopsAtZeroFloors() {
        LayoutPlanner high = bigCity(
            15L,
            LayoutProfile.builder()
                .cityChance(0)
                .levels(245, -1)
                .build(),
            LayoutFixtures.standardAssets());
        for (ChunkPlan plan : region(high)) {
            assertFalse(plan.hasBuilding(), "a city at Y 245 has no room for a floor: " + plan);
        }
        LayoutPlanner tall = bigCity(
            15L,
            LayoutProfile.builder()
                .cityChance(0)
                .levels(230, -1)
                .build(),
            LayoutFixtures.standardAssets());
        int buildings = 0;
        for (ChunkPlan plan : region(tall)) {
            if (plan.hasBuilding()) {
                buildings++;
                assertTrue(plan.floors() >= 1 && plan.floors() <= 2, plan + " floors " + plan.floors());
            }
        }
        assertTrue(buildings > 0);
    }

    @Test
    public void doesNotReproduceCITY16_cellarsAreUniformBetweenMinimumAndMaximum() {
        LayoutFixtures.FakeAssets assets = onlyBuilding(new LayoutFixtures.FakeBuilding("cellared").cellars(2, 3));
        LayoutPlanner p = bigCity(16L, noRandomCities(), assets);
        int buildings = 0;
        int atMaximum = 0;
        for (int x = -20; x <= 20; x++) {
            for (int z = -20; z <= 20; z++) {
                ChunkPlan plan = p.plan(x, z);
                if (plan.hasBuilding() && plan.multiPos()
                    .isSingle()) {
                    buildings++;
                    assertTrue(plan.cellars() == 2 || plan.cellars() == 3);
                    atMaximum += plan.cellars() == 3 ? 1 : 0;
                }
            }
        }
        assertTrue(buildings > 60, "buildings " + buildings);
        double share = atMaximum / (double) buildings;
        // Upstream's clamp gave the maximum three draws in four.
        assertTrue(share > .3 && share < .7, "share at the maximum " + share);
    }

    @Test
    public void doesNotReproduceCITY17_aBuildingWithNoMatchingPartIsLeftOut() {
        LayoutFixtures.FakeAssets assets = onlyBuilding(new LayoutFixtures.FakeBuilding("broken").noParts());
        List<String> warnings = new ArrayList<>();
        LayoutPlanner p = new LayoutPlanner(
            LayoutInputs.builder(17L, noRandomCities(), assets, LayoutFixtures.flatTerrain())
                .predefined(LayoutFixtures.bigCity(0, 0, 480, "citystyle_common"))
                .log(warnings::add)
                .build());
        for (ChunkPlan plan : region(p)) {
            assertFalse(plan.hasBuilding());
        }
        assertEquals(1, warnings.size(), warnings.toString());
        assertTrue(
            warnings.get(0)
                .contains("broken"));
    }

    @Test
    public void doesNotReproduceCITY18_floorsAndCellarsAreFittedOnceAndQueriesArePure() {
        LayoutPlanner low = bigCity(
            18L,
            LayoutProfile.builder()
                .cityChance(0)
                .levels(10, -1)
                .build(),
            LayoutFixtures.standardAssets());
        int cellared = 0;
        for (ChunkPlan plan : region(low)) {
            int floors = plan.floors();
            int cellars = plan.cellars();
            assertTrue(plan.buildingBottomHeight() > LayoutPlanner.MIN_BOTTOM_Y);
            assertTrue(cellars <= 1, "Y 10 leaves room for one cellar: " + plan);
            plan.buildingBottomHeight();
            plan.maxHeight();
            assertEquals(floors, plan.floors());
            assertEquals(cellars, plan.cellars());
            cellared += cellars;
        }
        assertTrue(cellared > 0);
    }

    @Test
    public void doesNotReproduceCITY19_planningNeverRemovesRails() {
        int[] calls = new int[1];
        InfrastructureView rails = new InfrastructureView() {

            @Override
            public int highwayXLevel(int chunkX, int chunkZ) {
                return -1;
            }

            @Override
            public int highwayZLevel(int chunkX, int chunkZ) {
                return -1;
            }

            @Override
            public RailInfo rail(int chunkX, int chunkZ) {
                calls[0]++;
                return Math.abs(chunkZ) <= 2 ? new RailInfo(RailChunkType.HORIZONTAL, -2) : RailInfo.NONE;
            }

            @Override
            public int railwayLevel() {
                return 0;
            }
        };
        LayoutFixtures.FakeAssets assets = LayoutFixtures.standardAssets();
        assets.worldStyle().blockRailways = true;
        LayoutPlanner p = new LayoutPlanner(
            LayoutInputs.builder(19L, noRandomCities(), assets, LayoutFixtures.flatTerrain())
                .predefined(LayoutFixtures.bigCity(0, 0, 480, "citystyle_common"))
                .infrastructure(rails)
                .build());
        int colliding = 0;
        for (int x = -20; x <= 20; x++) {
            for (int z = -2; z <= 2; z++) {
                ChunkPlan plan = p.plan(x, z);
                RailInfo rail = rails.rail(x, z);
                assertTrue(rail.isPresent(), "the rail is still there after planning");
                // Rail parts are one level high (railpartheight6 = 1): a cellar at or below the rail's level collides.
                if (plan.hasBuilding() && plan.lowestCityLevel() <= rail.level()) {
                    colliding++;
                }
            }
        }
        // The plan exposes what the railway layout needs to drop the rail itself (D9).
        assertTrue(colliding > 0);
        assertTrue(calls[0] > 0);
    }

    @Test
    public void doesNotReproduceCITY20_cacheEvictionNeverChangesAPlan() {
        LayoutPlanner big = bigCity(20L, noRandomCities(), LayoutFixtures.standardAssets());
        LayoutPlanner tiny = new LayoutPlanner(
            LayoutInputs.builder(20L, noRandomCities(), LayoutFixtures.standardAssets(), LayoutFixtures.flatTerrain())
                .predefined(LayoutFixtures.bigCity(0, 0, 480, "citystyle_common"))
                .cacheSize(64)
                .build());
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                assertEquals(LayoutText.describe(big, x, z), LayoutText.describe(tiny, x, z), x + "," + z);
            }
        }
    }

    @Test
    public void doesNotReproduceCITY21_majorityTiesGoToTheOwnStyleThenTheFirstName() {
        String[] tieWithOwn = { "b", "b", "b", "a", "a", "c", "c", "c", "c" };
        // own "a": a=2+1=3 (own counted twice), b=3, c=4: c wins outright.
        assertEquals("c", LayoutPlanner.voteMajority("a", tieWithOwn));
        String[] ownTies = { "b", "b", "b", "b", "a", "a", "c", "c", "c" };
        // own "a": a=3, b=4, c=3 -> b; now a tie b=4, a=4:
        assertEquals("b", LayoutPlanner.voteMajority("a", ownTies));
        String[] tie = { "b", "b", "b", "a", "a", "a", "d", "d", "d" };
        // own "a": a=4, b=3, d=3 -> a.
        assertEquals("a", LayoutPlanner.voteMajority("a", tie));
        String[] exactTie = { "z", "z", "z", "z", "own", "own", "y", "y", "y" };
        // own=3 (2 + 1), z=4: z wins. Swap one z for own: own=4, z=3.
        assertEquals("z", LayoutPlanner.voteMajority("own", exactTie));
        String[] ownWinsTie = { "z", "z", "z", "z", "own", "own", "own", "y", "y" };
        // own=4, z=4: the chunk's own style wins the tie.
        assertEquals("own", LayoutPlanner.voteMajority("own", ownWinsTie));
        String[] othersTie = { "q", "q", "q", "q", "p", "p", "p", "p", "own" };
        // own=2, p=4, q=4: own is not tied, the first name wins, whatever a hash map's order would be.
        assertEquals("p", LayoutPlanner.voteMajority("own", othersTie));
    }

    @Test
    public void doesNotReproduceTERR24_cornersAtY256AreStillConstrained() {
        // Ground level 250 and terrain at Y 78 (city level 1): city ground Y 256, which upstream took for "no corner".
        LayoutPlanner p = new LayoutPlanner(
            LayoutInputs.builder(
                22L,
                LayoutProfile.builder()
                    .cityChance(0)
                    .levels(250, -1)
                    .build(),
                LayoutFixtures.standardAssets(),
                new LayoutFixtures.FakeTerrain((x, z) -> 78))
                .predefined(LayoutFixtures.bigCity(0, 0, 160, "citystyle_common"))
                .build());
        int checked = 0;
        for (int x = 0; x < 16; x++) {
            boolean here = p.plan(x, 0)
                .isCity();
            boolean west = p.plan(x - 1, 0)
                .isCity();
            if (here != west) {
                CornerHeight corner = p.cornerHeight(x, 0);
                assertTrue(corner.isConstrained(), "corner at " + x);
                assertEquals(256, corner.height());
                checked++;
            }
        }
        assertTrue(checked > 0);
        assertFalse(
            p.cornerHeight(0, 0)
                .isConstrained(),
            "all four city: unconstrained");
    }

    @Test
    public void doesNotReproduceCITY24_multibuildingsNeverCoverALandmarkCoreOrEdgeRing() {
        LayoutPlanner p = LayoutFixtures.cityWith(
            24L,
            480,
            LayoutFixtures.landmark("tower", LandmarkKind.ROGUELIKE, 20, 20, 60, 60),
            LayoutFixtures.landmark("room", LandmarkKind.LOOTGAMES, -90, 30, -70, 50));
        int multi = 0;
        for (ChunkPlan plan : region(p)) {
            Zone zone = plan.zone()
                .zone();
            if (plan.multiPos()
                .isMulti()) {
                multi++;
                assertTrue(zone != Zone.CORE && zone != Zone.EDGE_RING, plan.toString());
            }
            if (zone == Zone.CORE) {
                assertFalse(plan.isCity());
            }
            if (zone == Zone.EDGE_RING) {
                // The margin (AR-10): the city builds inside it, so it is never city at all.
                assertFalse(plan.isCity(), plan.toString());
            }
        }
        assertTrue(multi > 0);
    }

    @Test
    public void doesNotReproduceCITY35_lastColumnMultibuildingWallsGetDoorways() {
        LayoutPlanner p = bigCity(35L, noRandomCities(), LayoutFixtures.standardAssets());
        int walls = 0;
        int open = 0;
        for (int x = -20; x <= 20; x++) {
            for (int z = -20; z <= 20; z++) {
                ChunkPlan plan = p.plan(x, z);
                if (plan.hasBuilding() && plan.multiPos()
                    .isRightSide()) {
                    for (int level = 0; level < plan.levels(); level++) {
                        walls++;
                        open += p.hasConnectionAtX(x, z, level) ? 1 : 0;
                    }
                }
            }
        }
        assertTrue(walls > 10, "walls " + walls);
        assertTrue(open > 0, "upstream never opened the last column's west wall");
    }

    @Test
    public void doesNotReproduceCITY36_westPassageChecksTheNeighborsDontconnect() {
        LayoutPlanner closed = pair("closed");
        LayoutPlanner control = pair("building2");
        int passages = 0;
        ChunkPlan a = closed.plan(1, 0);
        assertTrue(
            a.hasBuilding() && closed.plan(0, 0)
                .hasBuilding());
        for (int f = -a.cellars(); f < a.floors(); f++) {
            assertTrue(closed.doorway(1, 0, f, Direction.XMIN) != Doorway.PASSAGE, "floor " + f);
            passages += control.doorway(1, 0, f, Direction.XMIN) == Doorway.PASSAGE ? 1 : 0;
        }
        assertTrue(passages > 0, "the control pair must have a west passage, or the test proves nothing");
    }

    /** Two predefined buildings side by side: {@code west} at chunk (0, 0), building1 at (1, 0). */
    private static LayoutPlanner pair(String west) {
        LayoutFixtures.FakeAssets assets = LayoutFixtures.standardAssets();
        assets.add(new LayoutFixtures.FakeBuilding("closed").closed());
        PredefinedIndex index = PredefinedIndex.build(
            "minecraft:overworld",
            Collections.singletonList(
                new PredefinedCity(
                    "pair",
                    "minecraft:overworld",
                    0,
                    0,
                    300,
                    "citystyle_common",
                    Arrays.asList(
                        new PredefinedBuilding(west, 0, 0, false, false),
                        new PredefinedBuilding("building1", 1, 0, false, false)),
                    Collections.<int[]>emptyList())),
            name -> null);
        return new LayoutPlanner(
            LayoutInputs.builder(36L, noRandomCities(), assets, LayoutFixtures.flatTerrain())
                .predefined(index)
                .build());
    }
}
