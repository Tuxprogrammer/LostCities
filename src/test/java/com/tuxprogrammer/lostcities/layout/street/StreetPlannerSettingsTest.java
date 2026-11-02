package com.tuxprogrammer.lostcities.layout.street;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** {@link StreetPlannerSettings}: upstream's defaults and checks. */
public class StreetPlannerSettingsTest {

    @Test
    public void defaultsAreUpstreams() {
        StreetPlannerSettings d = StreetPlannerSettings.DEFAULTS;
        assertEquals(8, d.primarySpacingX());
        assertEquals(8, d.primarySpacingZ());
        assertEquals(.45f, d.primaryOptionalChance());
        assertEquals(4, d.primaryForceEvery());
        assertEquals(0, d.secondaryMinCountX());
        assertEquals(2, d.secondaryMaxCountZ());
        assertEquals(4, d.minimumRoadSeparation());
        assertEquals(3, d.minimumEdgeDistance());
        assertEquals(.40f, d.tertiaryChance());
        assertEquals(2, d.tertiaryMinLength());
        assertEquals(5, d.tertiaryMaxLength());
    }

    @Test
    public void rejectsWhatUpstreamRejects() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new StreetPlannerSettings(7, 8, .45f, 4, 0, 2, 0, 2, 4, 3, .4f, 2, 5));
        assertThrows(
            IllegalArgumentException.class,
            () -> new StreetPlannerSettings(8, 8, .45f, 17, 0, 2, 0, 2, 4, 3, .4f, 2, 5));
        assertThrows(
            IllegalArgumentException.class,
            () -> new StreetPlannerSettings(8, 8, .45f, 4, 3, 2, 0, 2, 4, 3, .4f, 2, 5));
        assertThrows(
            IllegalArgumentException.class,
            () -> new StreetPlannerSettings(8, 8, .45f, 4, 0, 2, 0, 2, 1, 3, .4f, 2, 5));
        assertThrows(
            IllegalArgumentException.class,
            () -> new StreetPlannerSettings(8, 8, .45f, 4, 0, 2, 0, 2, 4, 3, .4f, 6, 5));
    }
}
