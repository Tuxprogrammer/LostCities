package com.tuxprogrammer.lostcities.worldgen.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.CityHighwayLayout;
import com.tuxprogrammer.lostcities.layout.LayoutFixtures;
import com.tuxprogrammer.lostcities.layout.LayoutInputs;
import com.tuxprogrammer.lostcities.layout.LayoutLog;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.layout.LayoutProfile;
import com.tuxprogrammer.lostcities.layout.LotKind;
import com.tuxprogrammer.lostcities.layout.NetworkSettings;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.worldgen.city.furnish.FurnishPass;
import com.tuxprogrammer.lostcities.worldgen.city.furnish.RoomKind;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainSettings;

/**
 * The city-realism kinds of {@code /lostcities find}: {@code gtroom}, {@code furnished}, {@code packhouse},
 * {@code beltway} and {@code district}, over a test world with the realism options on.
 */
public class CityFinderRealismTest {

    private static final long SEED = CityFixtures.SEED;

    /** The test world with the options a new world gets that these kinds read. */
    private static final LayoutCityPlan PLAN = CityFixtures.plan(SEED, realism(), StructureOracle.NONE, LayoutLog.SILENT);

    private static TerrainSettings realism() {
        Map<String, String> values = new HashMap<>();
        values.put("furnish", "true");
        values.put("roomRules", "true");
        values.put("packHouseChance", "0.5");
        values.put("edgeLotChance", "0.5");
        values.put("densityRings", "1.0");
        values.put("cityDistricts", "true");
        return TerrainSettings.fromValues(values);
    }

    private static CityFinder finder() {
        return new CityFinder(PLAN.planner(), PLAN).withFurnish(SEED, true);
    }

    @Test
    public void furnishedAndGtRoomsNeedTheFurnishData() {
        CityFinder bare = new CityFinder(PLAN.planner(), PLAN);
        assertNull(bare.find(CityFinder.Kind.FURNISHED, 0, 0, 40), "no furnish data, no find");
        assertNull(bare.find(CityFinder.Kind.GTROOM, 0, 0, 40), "no furnish data, no find");
    }

    @Test
    public void aFurnishedFindIsAResidentialOrOfficeFloorThePassAgrees() {
        CityFinder.Result found = finder().find(CityFinder.Kind.FURNISHED, 0, 0, 80);
        assertNotNull(found, "a furnished floor near the origin");
        ChunkPlan layout = PLAN.planner()
            .plan(found.chunkX(), found.chunkZ());
        assertTrue(layout.hasBuilding() && layout.ruinHeight() < 0, found.format());
        int floor = Math.floorDiv(found.y() - layout.cityGroundLevel(), layout.floorPitch());
        RoomKind kind = RoomKind.forFloor(
            CityPalettes.id(layout.building())
                .path(),
            floor,
            FurnishPass.floorSalt(SEED, found.chunkX(), found.chunkZ(), floor),
            true);
        assertTrue(kind == RoomKind.RESIDENTIAL || kind == RoomKind.OFFICE, found.format() + " is " + kind);
        assertTrue(found.label().startsWith(kind.id() + " floor " + floor), found.format());
        assertEquals(found.y() + CityFinder.FEET, found.viewY(), 0.001, "the feet are one block above the floor");
    }

    @Test
    public void aGtRoomIsIndustrialWhereOneIsNearAndOtherwiseACellar() {
        CityFinder.Result found = finder().nearest(CityFinder.Kind.GTROOM, 0, 0, 120);
        assertNotNull(found, "an industrial floor or a cellar near the origin");
        ChunkPlan layout = PLAN.planner()
            .plan(found.chunkX(), found.chunkZ());
        int floor = Math.floorDiv(found.y() - layout.cityGroundLevel(), layout.floorPitch());
        RoomKind kind = RoomKind.forFloor(
            CityPalettes.id(layout.building())
                .path(),
            floor,
            FurnishPass.floorSalt(SEED, found.chunkX(), found.chunkZ(), floor),
            true);
        if (found.label().startsWith(CityFinder.INDUSTRIAL_LABEL)) {
            assertEquals(RoomKind.INDUSTRIAL, kind, found.format());
        } else {
            assertEquals(RoomKind.BASEMENT, kind, found.format());
            assertTrue(floor < 0, found.format());
        }
    }

    @Test
    public void aPackHouseFindFacesItsStreetFromTheDoorSide() {
        CityFinder.Result found = finder().find(CityFinder.Kind.PACKHOUSE, 0, 0, 120);
        assertNotNull(found, "a pack house near the origin");
        int side = PLAN.chunk(found.chunkX(), found.chunkZ())
            .packHouseSide();
        assertTrue(side >= 0, found.format());
        assertTrue(
            PLAN.planner()
                .plan(found.chunkX(), found.chunkZ())
                .lot() == LotKind.BARE,
            "an empty lot: " + found.format());
        double dx = found.viewX() - (found.x() + 0.5);
        double dz = found.viewZ() - (found.z() + 0.5);
        assertEquals(CityFinder.HOUSE_BACK, Math.abs(dx) + Math.abs(dz), 0.001, "one chunk out on the door side");
        assertEquals(
            Math.round(CityFinder.yawToward(found.viewX(), found.viewZ(), found.x(), found.z())),
            found.yaw(),
            0.01f,
            "looks at the house");
    }

    @Test
    public void aDistrictIsAYardOrAPlazaAndYardsComeFirst() {
        CityFinder finder = finder();
        CityFinder.Result found = finder.nearest(CityFinder.Kind.DISTRICT, 0, 0, 160);
        assertNotNull(found, "a district near the origin");
        LotKind lot = PLAN.planner()
            .plan(found.chunkX(), found.chunkZ())
            .lot();
        assertTrue(lot == LotKind.INDUSTRY || lot == LotKind.PLAZA, found.format());
        assertTrue(found.label().startsWith(lot.label()), found.format());
        if (lot == LotKind.PLAZA) {
            assertNull(
                finder.find(
                    CityFinder.Kind.DISTRICT,
                    0,
                    0,
                    160,
                    other -> other.label().startsWith(LotKind.INDUSTRY.label())),
                "a plaza only where no yard is in reach");
        }
        assertEquals(found.y() + CityFinder.DISTRICT_HEIGHT, found.viewY(), 0.001);
    }

    @Test
    public void aBeltwayFindIsAStraightPieceOfARingWithRingAhead() {
        CityFinder.Result found = null;
        LayoutPlanner planner = null;
        for (long seed = 1; seed <= 8 && found == null; seed++) {
            planner = beltwayPlanner(seed);
            found = new CityFinder(planner, null).find(CityFinder.Kind.BELTWAY, 0, 0, 190);
        }
        assertNotNull(found, "a beltway city among the first eight seeds");
        assertTrue(found.label().startsWith("beltway "), found.format());
        assertEquals(CityFinder.Kind.BELTWAY, found.kind());
        assertFalse(
            planner.plan(found.chunkX(), found.chunkZ())
                .hasBuilding(),
            "no building on the ring: " + found.format());
        assertTrue(found.viewY() > found.y(), "above the deck: " + found.format());
    }

    private static LayoutPlanner beltwayPlanner(long seed) {
        LayoutProfile profile = LayoutProfile.builder()
            .cityChance(0.00025)
            .cityRadius(100, 256)
            .cityGrid(64, 16)
            .densityRings(1.5f)
            .cityHighwayLayout(CityHighwayLayout.BELTWAY)
            .build();
        return new LayoutPlanner(
            LayoutInputs.builder(seed, profile, LayoutFixtures.standardAssets(), LayoutFixtures.flatTerrain())
                .networks(NetworkSettings.DEFAULTS.withLinks(true))
                .build());
    }

    @Test
    public void everyKindHasAKeyTheCommandTakes() {
        for (String key : new String[] { "gtroom", "furnished", "packhouse", "beltway", "district" }) {
            CityFinder.Kind kind = CityFinder.Kind.of(key);
            assertNotNull(kind, key);
            assertEquals(key, kind.key());
        }
        assertNull(CityFinder.Kind.of("gtroom; op"), "arguments are matched, never interpreted");
    }
}
