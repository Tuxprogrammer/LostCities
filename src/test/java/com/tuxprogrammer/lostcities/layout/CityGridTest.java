package com.tuxprogrammer.lostcities.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.config.profile.BuiltinProfiles;
import com.tuxprogrammer.lostcities.config.profile.ProfileValues;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.worldgen.city.CityProfiles;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainSettings;

/** The jittered-grid city centers ({@link CityGrid}) and their use by {@link CityMap}: decisions D270 to 4. */
public class CityGridTest {

    private static final long SEED = JitteredGridPreviewDump.WORLD_SEED;

    private static LayoutPlanner gridPlanner(long seed) {
        LayoutProfile profile = LayoutProfile.builder()
            .cityChance(0.00025)
            .cityRadius(100, 256)
            .cityGrid(64, 16)
            .build();
        return LayoutFixtures.planner(seed, profile, LayoutFixtures.flatTerrain(), StructureOracle.NONE);
    }

    @Test
    public void centersMatchTheValuesTheApprovedPreviewDrew() {
        // Values computed independently (a Java Random replica) for the preview's seed.
        CityGrid grid = new CityGrid(SEED, 64, 16, 100, 256);
        assertCenter(grid.center(0, 0), 29, 43, 215);
        assertCenter(grid.center(-1, -1), -27, -31, 191);
        assertCenter(grid.center(3, -2), 227, -84, 196);
        assertCenter(grid.center(-8, 7), -474, 477, 123);
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    private static void assertCenter(CityGrid.Center c, int x, int z, int radius) {
        assertEquals(x, c.chunkX());
        assertEquals(z, c.chunkZ());
        assertEquals(radius, c.radius());
    }

    @Test
    public void thePreviewDumpListsTheMainClassCenters() {
        List<JitteredGridPreviewDump.Center> preview = JitteredGridPreviewDump.jitteredGrid(SEED, -512, 512, 100, 256);
        List<CityGrid.Center> main = new CityGrid(SEED, 64, 16, 100, 256).centersInWindow(-512, 512);
        assertEquals(256, preview.size());
        assertEquals(main.size(), preview.size());
        for (int i = 0; i < main.size(); i++) {
            assertEquals(
                main.get(i)
                    .chunkX(),
                preview.get(i).chunkX);
            assertEquals(
                main.get(i)
                    .chunkZ(),
                preview.get(i).chunkZ);
            assertEquals(
                main.get(i)
                    .radius(),
                preview.get(i).radius);
        }
    }

    @Test
    public void everyCellHoldsOneCenterInItsMiddleAndTheWindowDoesNotMatter() {
        CityGrid grid = new CityGrid(SEED, 64, 16, 100, 256);
        for (CityGrid.Center c : grid.centersInWindow(-512, 512)) {
            int inX = Math.floorMod(c.chunkX(), 64);
            int inZ = Math.floorMod(c.chunkZ(), 64);
            assertTrue(inX >= 16 && inX < 48 && inZ >= 16 && inZ < 48, "centered in the middle half of its cell");
            assertTrue(c.radius() >= 100 && c.radius() < 256);
        }
        List<CityGrid.Center> big = grid.centersInCellsOf(-100, -100, 100, 100);
        List<CityGrid.Center> small = grid.centersInCellsOf(0, 0, 10, 10);
        assertEquals(16, big.size(), "4 x 4 cells touch the window");
        assertEquals(1, small.size());
        assertEquals(
            grid.center(0, 0)
                .chunkX(),
            small.get(0)
                .chunkX());
    }

    @Test
    public void gridPlannerCityCentersAreExactlyTheGridCenters() {
        LayoutPlanner planner = gridPlanner(SEED);
        CityGrid grid = new CityGrid(SEED, 64, 16, 100, 256);
        Set<Long> expected = new HashSet<>();
        for (CityGrid.Center c : grid.centersInWindow(-256, 256)) {
            expected.add(key(c.chunkX(), c.chunkZ()));
            assertTrue(planner.isCityCenter(c.chunkX(), c.chunkZ()));
            assertEquals(c.radius(), planner.cityRadius(c.chunkX(), c.chunkZ()), 0f);
        }
        Set<Long> found = new HashSet<>();
        for (int x = -256; x < 256; x++) {
            for (int z = -256; z < 256; z++) {
                if (planner.isCityCenter(x, z)) {
                    found.add(key(x, z));
                }
            }
        }
        assertEquals(expected, found);
        assertEquals(64, found.size());
    }

    @Test
    public void gridWorldCityChunksComeOnlyFromGridCenters() {
        LayoutPlanner planner = gridPlanner(SEED);
        CityGrid grid = new CityGrid(SEED, 64, 16, 100, 256);
        int cityChunks = 0;
        for (int x = -200; x < 200; x += 2) {
            for (int z = -200; z < 200; z += 2) {
                if (!planner.isCityRaw(x, z)) {
                    continue;
                }
                cityChunks++;
                boolean reached = false;
                for (CityGrid.Center c : grid.centersNear(x, z, 40)) {
                    double dx = (c.chunkX() - x) * 16.0;
                    double dz = (c.chunkZ() - z) * 16.0;
                    reached |= dx * dx + dz * dz < (double) c.radius() * c.radius();
                }
                assertTrue(reached, "city chunk " + x + "," + z + " lies inside no grid center's radius");
            }
        }
        assertTrue(cityChunks > 100, "the window holds cities");
    }

    @Test
    public void aGridCenterYieldsACityOnLandAndNoneAtSea() {
        LayoutPlanner land = gridPlanner(SEED);
        CityGrid.Center c = new CityGrid(SEED, 64, 16, 100, 256).center(0, 0);
        assertTrue(land.centerYieldsCity(c.chunkX(), c.chunkZ()));
        assertFalse(land.centerYieldsCity(c.chunkX() + 1, c.chunkZ()), "not a center");
        assertNotNull(land.gridCenters(0, 0, 10, 10));
        LayoutProfile ocean = LayoutProfile.builder()
            .cityChance(0.00025)
            .cityRadius(100, 256)
            .cityGrid(64, 16)
            .cityShape(true, true)
            .build();
        LayoutPlanner sea = LayoutFixtures
            .planner(SEED, ocean, new LayoutFixtures.FakeTerrain((x, z) -> 40), StructureOracle.NONE);
        assertFalse(sea.centerYieldsCity(c.chunkX(), c.chunkZ()), "below the height window");
    }

    @Test
    public void chanceModeIsUntouched() {
        LayoutProfile profile = LayoutProfile.builder()
            .cityChance(0.00025)
            .build();
        LayoutPlanner planner = LayoutFixtures
            .planner(SEED, profile, LayoutFixtures.flatTerrain(), StructureOracle.NONE);
        assertNull(planner.gridCenters(0, 0, 10, 10));
    }

    @Test
    public void rejectsACellTheJitterFills() {
        assertThrows(IllegalArgumentException.class, () -> new CityGrid(1L, 64, 32, 50, 100));
    }

    @Test
    public void aSnapshotWithoutTheKeyReadsChanceAndOnlyRarecitiesIsGrid() {
        ProfileValues old = ProfileValues.of(Collections.<String, String>emptyMap());
        assertEquals("chance", old.getString("cityPlacement"));
        assertEquals(
            0,
            CityProfiles.layoutProfile(TerrainSettings.defaults(), old).cityGridCell,
            "a world pinned before the option keeps its centers");
        for (String name : BuiltinProfiles.names()) {
            String placement = BuiltinProfiles.get(name)
                .values()
                .getString("cityPlacement");
            assertEquals("rarecities".equals(name) ? "grid" : "chance", placement, name);
        }
        LayoutProfile rare = CityProfiles.layoutProfile(
            TerrainSettings.defaults(),
            BuiltinProfiles.get("rarecities")
                .values());
        assertEquals(256, rare.cityGridCell, "D-city-scale-1");
        assertEquals(64, rare.cityGridJitter);
        assertEquals(200, rare.cityMinRadius);
        assertEquals(512, rare.cityMaxRadius);
    }
}
