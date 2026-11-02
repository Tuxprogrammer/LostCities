package com.tuxprogrammer.lostcities.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.landmark.BlockRect;
import com.tuxprogrammer.lostcities.layout.landmark.HeightCap;
import com.tuxprogrammer.lostcities.layout.landmark.Landmark;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkKind;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkTreatment;
import com.tuxprogrammer.lostcities.layout.landmark.Zone;

/** {@link LandmarkZones}: core, edge ring, transition rings, avoid ring and crater caps (landmark-zones.md). */
public class LandmarkZonesTest {

    /** A village whose footprint, grown by 2 blocks, stays inside chunk (5, 5). */
    private static Landmark village() {
        return LayoutFixtures.landmark("village@5,5", LandmarkKind.VILLAGE, 84, 84, 91, 91);
    }

    private static LandmarkZones zones(LayoutProfile profile, Landmark... landmarks) {
        return new LandmarkZones(new LayoutFixtures.FakeOracle(landmarks), profile, 4096);
    }

    @Test
    public void villageZonesByChunkDistance() {
        LandmarkZones z = zones(LayoutProfile.defaults(), village());
        assertEquals(
            Zone.CORE,
            z.zone(5, 5)
                .zone());
        assertEquals(
            Zone.EDGE_RING,
            z.zone(4, 4)
                .zone());
        assertEquals(
            Zone.EDGE_RING,
            z.zone(6, 5)
                .zone());
        assertEquals(
            Zone.TRANSITION_1,
            z.zone(3, 5)
                .zone());
        assertEquals(
            Zone.TRANSITION_2,
            z.zone(8, 8)
                .zone());
        assertEquals(
            Zone.NONE,
            z.zone(1, 5)
                .zone());
        assertEquals(
            "village@5,5",
            z.zone(4, 4)
                .landmarkId());
    }

    @Test
    public void marginGrowsTheCoreAcrossAChunkBorder() {
        // Footprint 78..85 grown by 2 blocks is 76..87: chunks 4 and 5.
        LandmarkZones z = zones(
            LayoutProfile.defaults(),
            LayoutFixtures.landmark("hut", LandmarkKind.LOOTGAMES, 78, 84, 85, 90));
        assertEquals(
            Zone.CORE,
            z.zone(4, 5)
                .zone());
        assertEquals(
            Zone.CORE,
            z.zone(5, 5)
                .zone());
        assertEquals(
            Zone.EDGE_RING,
            z.zone(3, 5)
                .zone());
        assertEquals(
            Zone.NONE,
            z.zone(2, 5)
                .zone(),
            "only villages have transition rings");
    }

    @Test
    public void avoidTreatmentGivesUpstreamsNonCityRing() {
        LayoutProfile avoid = LayoutProfile.builder()
            .villageTreatment(LandmarkTreatment.AVOID)
            .build();
        LandmarkZones z = zones(avoid, village());
        assertEquals(
            Zone.CORE,
            z.zone(5, 5)
                .zone());
        assertEquals(
            Zone.AVOID_RING,
            z.zone(4, 5)
                .zone());
        assertEquals(
            Zone.NONE,
            z.zone(3, 5)
                .zone());
    }

    @Test
    public void ignoreTreatmentHasNoEffect() {
        LayoutProfile ignore = LayoutProfile.builder()
            .landmarkTreatment(LandmarkKind.LOOTGAMES, LandmarkTreatment.IGNORE)
            .build();
        LandmarkZones z = zones(ignore, LayoutFixtures.landmark("room", LandmarkKind.LOOTGAMES, 84, 84, 91, 91));
        assertEquals(
            Zone.NONE,
            z.zone(5, 5)
                .zone());
    }

    @Test
    public void terrainLandmarksHaveAMarginToo() {
        LandmarkZones z = zones(
            LayoutProfile.defaults(),
            LayoutFixtures.landmark("volcano", LandmarkKind.TERRAIN, 84, 84, 91, 91));
        assertEquals(
            Zone.CORE,
            z.zone(5, 5)
                .zone());
        assertEquals(
            Zone.EDGE_RING,
            z.zone(4, 5)
                .zone());
        assertTrue(Zone.EDGE_RING.forcesNonCity(), "the margin is never city (AR-10)");
        assertFalse(Zone.EDGE_RING.barsLanes(), "a lane runs down the margin's slope");
    }

    @Test
    public void meteoriteCapReachesTheCityAround() {
        Landmark meteor = Landmark.builder("meteor", LandmarkKind.METEORITE)
            .footprint(new BlockRect(150, 150, 170, 170))
            .heightCap(HeightCap.ae2(160, 160, 60))
            .build();
        LandmarkZones z = zones(LayoutProfile.defaults(), meteor);
        assertEquals(
            Zone.CORE,
            z.zone(10, 10)
                .zone());
        // Chunk (13, 10): nearest column (208, 160), 48 blocks out: 60 + 0.02 * 48² = 106.08.
        assertEquals(
            106,
            z.zone(13, 10)
                .heightCap());
        assertTrue(
            z.zone(13, 10)
                .inCraterBasin());
        // Beyond 200 blocks no cap applies.
        assertEquals(
            Integer.MAX_VALUE,
            z.zone(30, 10)
                .heightCap());
    }

    @Test
    public void strongestZoneWinsAndOrderDoesNotMatter() {
        Landmark a = LayoutFixtures.landmark("a", LandmarkKind.VILLAGE, 84, 84, 91, 91);
        Landmark b = LayoutFixtures.landmark("b", LandmarkKind.LOOTGAMES, 116, 84, 120, 90);
        LandmarkZones ab = zones(LayoutProfile.defaults(), a, b);
        LandmarkZones ba = zones(LayoutProfile.defaults(), b, a);
        for (int x = 0; x < 12; x++) {
            assertEquals(
                ab.zone(x, 5)
                    .toString(),
                ba.zone(x, 5)
                    .toString());
        }
        // Chunk (6, 5): edge ring of a, edge ring of b (core 7): edge ring.
        assertEquals(
            Zone.EDGE_RING,
            ab.zone(6, 5)
                .zone());
        assertEquals(
            Zone.CORE,
            ab.zone(7, 5)
                .zone());
    }

    @Test
    public void theOracleIsAskedOncePerCell() {
        LayoutFixtures.FakeOracle oracle = new LayoutFixtures.FakeOracle(village());
        LandmarkZones z = new LandmarkZones(oracle, LayoutProfile.defaults(), 4096);
        for (int x = 0; x < 8; x++) {
            for (int zz = 0; zz < 8; zz++) {
                z.zone(x, zz);
            }
        }
        assertEquals(1, oracle.calls);
    }
}
