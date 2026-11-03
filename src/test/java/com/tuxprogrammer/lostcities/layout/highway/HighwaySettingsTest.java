package com.tuxprogrammer.lostcities.layout.highway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The planner's settings: upstream's defaults and ranges (intercity-planner.md, "Settings"). */
public class HighwaySettingsTest {

    @Test
    public void theDefaultsAreUpstreams() {
        HighwaySettings s = HighwaySettings.DEFAULTS;
        assertTrue(s.enabled());
        assertTrue(s.supports());
        assertEquals(128, s.planningCellSize());
        assertEquals(16, s.hubSampleSpacing());
        assertEquals(0.2f, s.hubMinimumPotential());
        assertEquals(2, s.hubSearchRadiusCells());
        assertEquals(64, s.minimumHubDistance());
        assertEquals(320, s.maximumHubDistance());
        assertEquals(2, s.maximumConnectionsPerHub());
        assertEquals(40, s.minimumRouteLength());
        assertEquals(1.0f, s.routeCityPenalty());
        assertEquals(3, s.levelFromCities());
        assertEquals(0, s.networkLevel());
        assertEquals(10, s.railwaySpacingNorthSouth());
        assertEquals(10, s.railwaySpacingEastWest());
    }

    @Test
    public void valuesOutsideUpstreamsRangesAreRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> HighwaySettings.builder()
                .hubs(16, 1, 0.2f, 2)
                .build());
        assertThrows(
            IllegalArgumentException.class,
            () -> HighwaySettings.builder()
                .hubs(64, 65, 0.2f, 2)
                .build(),
            "sample spacing above the cell size");
        assertThrows(
            IllegalArgumentException.class,
            () -> HighwaySettings.builder()
                .connections(400, 320, 2, 40, 1f)
                .build(),
            "minimum distance above the maximum");
        assertThrows(
            IllegalArgumentException.class,
            () -> HighwaySettings.builder()
                .levels(5, 0)
                .build());
        assertThrows(
            IllegalArgumentException.class,
            () -> HighwaySettings.builder()
                .railwaySpacing(9, 10)
                .build(),
            "an odd railway spacing");
    }
}
