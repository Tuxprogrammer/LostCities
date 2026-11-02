package com.tuxprogrammer.lostcities.core.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.api.ILostChunkInfo;
import com.tuxprogrammer.lostcities.api.ILostCityBuilding;
import com.tuxprogrammer.lostcities.api.ILostCityCityStyle;
import com.tuxprogrammer.lostcities.api.ILostCityInformation;
import com.tuxprogrammer.lostcities.api.ILostCityMultiBuilding;
import com.tuxprogrammer.lostcities.api.LostChunkType;
import com.tuxprogrammer.lostcities.api.LostLandmarkZone;
import com.tuxprogrammer.lostcities.api.RailChunkType;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.worldgen.city.CityFixtures;
import com.tuxprogrammer.lostcities.worldgen.city.LayoutCityPlan;
import com.tuxprogrammer.lostcities.worldgen.damage.DamageArea;
import com.tuxprogrammer.lostcities.worldgen.damage.Explosion;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainSettings;

/**
 * The API over a fixture world (seed 4242, the bundled assets, {@code TestTerrain}): every answer agrees with the
 * layout generation writes from, and asking changes nothing generation reads.
 */
public class LostCityInformationImplTest {

    private static final LayoutCityPlan CITY = CityFixtures.plan();
    private static final LayoutPlanner PLANNER = CITY.planner();

    /** The fixture world's source, with one explosion over the chunk (3, 3). */
    private static final ApiSource SOURCE = new ApiSource() {

        @Override
        public int dimension() {
            return 0;
        }

        @Override
        public String profile() {
            return "default";
        }

        @Override
        public int groundLevel() {
            return TerrainSettings.defaults()
                .groundLevel();
        }

        @Override
        public float ruinChance() {
            return TerrainSettings.defaults()
                .ruinChance();
        }

        @Override
        public LayoutPlanner planner() {
            return PLANNER;
        }

        @Override
        public AssetSet assets() {
            return CityFixtures.ASSETS;
        }

        @Override
        public DamageArea damage(int chunkX, int chunkZ) {
            if (chunkX == 3 && chunkZ == 3) {
                return DamageArea
                    .withExplosions(chunkX, chunkZ, Collections.singletonList(new Explosion(56, 88, 56, 12)));
            }
            return DamageArea.withExplosions(chunkX, chunkZ, Collections.<Explosion>emptyList());
        }
    };

    private static final ILostCityInformation INFO = new LostCityInformationImpl(SOURCE);

    @Test
    public void aBuildingChunkReportsItsBuilding() {
        int[] chunk = CityFixtures.first(
            12,
            (x, z) -> PLANNER.plan(x, z)
                .hasBuilding()
                && PLANNER.plan(x, z)
                    .multiPos()
                    .isSingle());
        ChunkPlan plan = PLANNER.plan(chunk[0], chunk[1]);
        ILostChunkInfo info = INFO.getChunkInfo(chunk[0], chunk[1]);
        assertTrue(info.isCity());
        assertEquals(LostChunkType.BUILDING, info.getType());
        assertEquals(plan.building(), info.getBuildingType());
        assertEquals(plan.floors(), info.getNumFloors());
        assertEquals(plan.cellars(), info.getNumCellars());
        assertEquals(plan.cityLevel(), info.getCityLevel());
        assertEquals(plan.cityGroundLevel(), info.getCityGroundLevel());
        assertEquals(plan.cityStyle(), info.getCityStyle());
        assertNull(info.getMultiBuildingInfo());
        assertNotNull(
            INFO.getBuildings()
                .get(info.getBuildingType()),
            "the building is in the registry");
    }

    @Test
    public void streetsAndOutsideChunksReportWhatTheyAre() {
        int[] street = CityFixtures.first(
            12,
            (x, z) -> PLANNER.plan(x, z)
                .isCity()
                && !PLANNER.plan(x, z)
                    .hasBuilding());
        ILostChunkInfo info = INFO.getChunkInfo(street[0], street[1]);
        assertTrue(info.isCity());
        assertNull(info.getBuildingType());
        assertEquals(0, info.getNumFloors());
        assertTrue(
            info.getType() != LostChunkType.BUILDING && info.getType() != LostChunkType.NONE,
            info.getType()
                .name());
        int[] outside = CityFixtures.first(
            40,
            (x, z) -> !PLANNER.plan(x, z)
                .isCity());
        if (outside != null) {
            ILostChunkInfo none = INFO.getChunkInfo(outside[0], outside[1]);
            assertFalse(none.isCity());
            assertEquals(LostChunkType.NONE, none.getType());
            assertNull(none.getCityStyle());
        }
    }

    @Test
    public void highwaysRailsZonesAndScatteredAnswerNoneBeforeTheirLanes() {
        ILostChunkInfo info = INFO.getChunkInfo(0, 0);
        assertEquals(RailChunkType.NONE, info.getRailType());
        assertEquals(-1, info.getRailLevel());
        assertEquals(
            PLANNER.plan(0, 0)
                .maxHighwayLevel(),
            info.getMaxHighwayLevel());
        assertEquals(LostLandmarkZone.NONE, info.getLandmarkZone());
        assertNull(info.getScatteredStructure());
    }

    @Test
    public void explosionsAndDamageComeFromTheChunksDamageArea() {
        ILostChunkInfo info = INFO.getChunkInfo(3, 3);
        assertEquals(
            1,
            info.getExplosions()
                .size());
        assertEquals(
            12,
            info.getExplosions()
                .get(0)
                .getRadius());
        assertEquals(
            56,
            info.getExplosions()
                .get(0)
                .getCenterX());
        assertTrue(info.getDamage(5) > 1f, "the section holding the center is destroyed");
        assertEquals(0f, info.getDamage(15));
        assertEquals(0f, info.getDamage(99), "a section outside the world has none");
        assertTrue(
            INFO.getChunkInfo(0, 0)
                .getExplosions()
                .isEmpty());
    }

    @Test
    public void heightsAndRegistries() {
        assertEquals(INFO.getGroundLevel() + 12, INFO.getRealHeight(2));
        assertEquals("default", INFO.getProfile());
        ILostCityBuilding building = INFO.getBuildings()
            .get("building1");
        assertNotNull(building);
        assertEquals("lostcities:building1", building.getId());
        assertEquals("building1", building.getName());
        assertEquals(
            INFO.getBuildings()
                .get("lostcities:building1")
                .getId(),
            building.getId());
        assertNull(
            INFO.getBuildings()
                .get("no:such:thing"));
        assertFalse(
            INFO.getBuildings()
                .getAll()
                .isEmpty());
        for (ILostCityMultiBuilding multi : INFO.getMultiBuildings()
            .getAll()) {
            assertTrue(multi.getDimX() > 0 && multi.getDimZ() > 0);
            assertNull(multi.getBuilding(-1, 0));
        }
        ILostCityCityStyle style = INFO.getCityStyles()
            .getAll()
            .get(0);
        assertNotNull(style.getId());
    }

    @Test
    public void askingChangesNothingGenerationReads() {
        int[] chunk = CityFixtures.first(
            12,
            (x, z) -> PLANNER.plan(x, z)
                .hasBuilding());
        ChunkPlan before = PLANNER.plan(chunk[0], chunk[1]);
        for (int dz = -2; dz <= 2; dz++) {
            for (int dx = -2; dx <= 2; dx++) {
                INFO.getChunkInfo(chunk[0] + dx, chunk[1] + dz)
                    .getCityInfo();
            }
        }
        assertEquals(
            before.toString(),
            PLANNER.plan(chunk[0], chunk[1])
                .toString());
    }
}
