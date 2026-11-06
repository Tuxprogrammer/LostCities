package com.tuxprogrammer.lostcities.worldgen.overlay;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The network scenarios' measures (KI-5). The highway walk stands on a block's collision top, so a thin snow layer on a
 * bridge in a cold biome is no step. The legacy {@code network.railway} scan band: a boundary's rails are looked for
 * only in the line's own city
 * levels, so a city's rail a level or more above the line is not counted as a track that fails to continue.
 */
public class NetworkScenariosTest {

    @Test
    public void theBandCoversOneCityLevelFromTheLineGround() {
        assertArrayEquals(new int[] { 53, 58 }, NetworkScenarios.RailwayRide.band(65, -2, -2));
    }

    @Test
    public void aBoundaryBetweenTwoLevelsCoversBoth() {
        assertArrayEquals(new int[] { 53, 64 }, NetworkScenarios.RailwayRide.band(65, -1, -2));
    }

    /** Seed 777 on RWG2: the line's rails at Y 54 are in the band; a city rail twelve blocks above is not. */
    @Test
    public void aCityRailTwoLevelsAboveTheLineIsOutside() {
        int[] band = NetworkScenarios.RailwayRide.band(65, -2, -2);
        assertTrue(54 >= band[0] && 54 <= band[1]);
        assertFalse(66 >= band[0] && 66 <= band[1]);
    }

    /** Classic RWG seed 1: a snow layer (meta 0, a box with no height) on the deck at Y 72 stands at 72, not 73. */
    @Test
    public void aThinSnowLayerStandsAtItsFloor() {
        assertEquals(72.0, NetworkScenarios.Walk.standingTop(72, 72.0), 1e-9);
    }

    @Test
    public void aFullBlockAndATallBoxStandAtTheBlockTop() {
        assertEquals(73.0, NetworkScenarios.Walk.standingTop(72, 73.0), 1e-9);
        assertEquals(73.0, NetworkScenarios.Walk.standingTop(72, 73.5), 1e-9);
    }

    @Test
    public void aThickSnowLayerIsAPartialStep() {
        assertEquals(72.5, NetworkScenarios.Walk.standingTop(72, 72.5), 1e-9);
    }
}
