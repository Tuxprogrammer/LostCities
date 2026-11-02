package com.tuxprogrammer.lostcities.worldgen.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.TestAssets;
import com.tuxprogrammer.lostcities.asset.match.BiomeDescriptor;
import com.tuxprogrammer.lostcities.asset.model.Part;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.Direction;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.layout.StreetType;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkKind;
import com.tuxprogrammer.lostcities.worldgen.plan.LanePath;
import com.tuxprogrammer.lostcities.worldgen.terrain.WorldLimits;

/** The real plan source over the layout and the bundled assets (D71). */
public class LayoutCityPlanTest {

    private static final LayoutCityPlan PLAN = CityFixtures.plan();
    private static final LayoutPlanner PLANNER = PLAN.planner();
    private static final int RADIUS = 20;

    @Test
    public void everyChunkGetsAPlanThatAgreesWithTheLayout() {
        int[] counts = new int[3];
        CityFixtures.square(RADIUS, c -> {
            com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan plan = PLAN.chunk(c[0], c[1]);
            ChunkPlan layout = PLANNER.plan(c[0], c[1]);
            assertEquals(layout.isCity(), plan.isCity());
            assertEquals(layout.hasBuilding(), plan.hasBuilding());
            if (plan.isCity()) {
                counts[0]++;
                assertEquals(layout.cityGroundLevel(), plan.groundY());
            }
            if (plan.hasBuilding()) {
                counts[1]++;
                assertEquals(layout.floors(), plan.floors());
                assertEquals(layout.cellars(), plan.cellars());
            }
            if ("bridge".equals(plan.label())) {
                counts[2]++;
            }
        });
        assertTrue(counts[0] > 100, "city chunks: " + counts[0]);
        assertTrue(counts[1] > 30, "buildings: " + counts[1]);
    }

    @Test
    public void doesNotReproduceTERR31_noBuildingReachesBelowYTwo() {
        CityFixtures.square(RADIUS, c -> {
            com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan plan = PLAN.chunk(c[0], c[1]);
            if (plan.hasBuilding()) {
                assertTrue(plan.buildingBottom() >= WorldLimits.BUILDING_BOTTOM_MIN, plan.toString());
            }
        });
    }

    @Test
    public void doesNotReproduceTERR25_everyPlanIsCompleteBeforeAnyWrite() {
        // The plan, its jobs and its footprint are built from pure inputs alone, so a failure would surface here,
        // before the terrain phase writes a block, never half way through a chunk.
        CityFixtures.square(8, c -> {
            com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan plan = PLAN.chunk(c[0], c[1]);
            plan.jobs();
            plan.footprintBottom(8, 8);
        });
    }

    @Test
    public void theFootprintCoversBuildingsAndStreetsButNotParks() {
        CityFixtures.square(RADIUS, c -> {
            com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan plan = PLAN.chunk(c[0], c[1]);
            ChunkPlan layout = PLANNER.plan(c[0], c[1]);
            if (plan.hasBuilding()) {
                assertEquals(plan.buildingBottom() - 1, plan.footprintBottom(3, 3));
            } else if (plan.isCity() && layout.streetType() == StreetType.PARK && plan.lane() == null) {
                assertEquals(com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan.NO_FOOTPRINT, plan.footprintBottom(3, 3));
            } else if (plan.isCity() && layout.isPlannedRoad()) {
                assertEquals(plan.groundY() - LayoutCityPlan.STREET_FOOTPRINT_DEPTH, plan.footprintBottom(3, 3));
            }
        });
    }

    @Test
    public void plansArePureWhateverTheOrderOfQuestions() {
        LayoutCityPlan other = CityFixtures.plan();
        List<String> forward = new ArrayList<>();
        List<String> backward = new ArrayList<>();
        for (int i = -6; i <= 6; i++) {
            forward.add(describe(PLAN.chunk(i, i * 2)));
        }
        for (int i = 6; i >= -6; i--) {
            backward.add(0, describe(other.chunk(i, i * 2)));
        }
        assertEquals(forward, backward);
    }

    private static String describe(com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan plan) {
        StringBuilder out = new StringBuilder(plan.toString()).append(' ')
            .append(plan.label())
            .append(' ')
            .append(plan.jobs());
        for (int i = 0; i < 256; i += 17) {
            out.append(',')
                .append(plan.footprintBottom(i & 15, i >> 4));
        }
        return out.toString();
    }

    @Test
    public void aSlopesStairOpensItsColumnsOfTheBorder() {
        Part slope = TestAssets.loadFiles(
            "lostcities/parts/s.json",
            "{\"xsize\": 1, \"zsize\": 1, \"slices\": [[\"a\"]], \"meta\": [{\"key\": \"z1\", \"integer\": 6},"
                + " {\"key\": \"z2\", \"integer\": 9}]}")
            .part(AssetId.parse("s"));
        // The neighbor to the west climbs toward us: its part's far side (x 15, z 6..9) turned to face east lands on
        // our west column at z 6..9.
        int mask = LayoutCityPlan.openingOf(slope, Direction.XMIN);
        assertEquals(0b1111 << 6, mask);
        assertEquals(0, LayoutCityPlan.openingOf(null, Direction.XMIN));
    }

    @Test
    public void aLanesMaterialFollowsTheVillagesBiome() {
        BiomeDescriptor mesa = BiomeDescriptor.of(new HashSet<>(Arrays.asList("MESA", "SANDY", "HOT")), "Mesa", null);
        BiomeDescriptor desert = BiomeDescriptor.of(new HashSet<>(Arrays.asList("SANDY", "HOT", "DRY")), "Desert", null);
        assertEquals(LanePath.Material.MESA, materialIn(mesa));
        assertEquals(LanePath.Material.SAND, materialIn(desert));
        assertEquals(LanePath.Material.PLAIN, PLAN.material(0, 0));
    }

    private static LanePath.Material materialIn(BiomeDescriptor biome) {
        CityWriteContext context = PLAN.context();
        CityWriteContext other = new CityWriteContext(
            context.worldSeed(),
            context.planner(),
            context.assets(),
            context.worldStyle(),
            context.palettes(),
            context.settings(),
            (x, z) -> biome,
            new com.tuxprogrammer.lostcities.worldgen.terrain.NameBlockClassifier(),
            com.tuxprogrammer.lostcities.layout.LayoutLog.SILENT);
        return new LayoutCityPlan(
            other,
            com.tuxprogrammer.lostcities.layout.landmark.StructureOracle.NONE,
            CityFixtures.terrain()).material(0, 0);
    }

    @Test
    public void theCacheEstimateStaysWithinBudgetB5() {
        long bytes = PLAN.estimatedCacheBytes() + com.tuxprogrammer.lostcities.worldgen.plan.CachedCityPlan.CAPACITY
            * (long) com.tuxprogrammer.lostcities.worldgen.plan.CachedCityPlan.ESTIMATED_PLAN_BYTES;
        assertTrue(bytes <= 16L * 1024 * 1024, "estimated " + bytes + " bytes");
    }

    @Test
    public void nonCityChunksCarryNoBuildingAndCoresNoContent() {
        CityFixtures.square(RADIUS, c -> {
            com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan plan = PLAN.chunk(c[0], c[1]);
            if (plan.kind() != ChunkKind.CITY) {
                assertTrue(!plan.hasBuilding());
            }
        });
    }
}
