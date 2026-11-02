package com.tuxprogrammer.lostcities.layout.view;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** {@link RailChunkType}: upstream's station and surface flags, and {@link RailInfo}'s use of them. */
public class RailChunkTypeTest {

    @Test
    public void flagsAreUpstreams() {
        assertTrue(RailChunkType.STATION_UNDERGROUND.isStation());
        assertFalse(RailChunkType.STATION_UNDERGROUND.isSurface());
        assertTrue(RailChunkType.GOING_DOWN_ONE_FROM_SURFACE.isSurface());
        assertFalse(RailChunkType.HORIZONTAL.isSurface());
        assertFalse(RailChunkType.NONE.isStation());
    }

    @Test
    public void railInfoSurfaceOrStation() {
        assertTrue(new RailInfo(RailChunkType.STATION_EXTENSION_UNDERGROUND, 0).isSurfaceOrStation());
        assertFalse(new RailInfo(RailChunkType.VERTICAL, -2).isSurfaceOrStation());
        assertFalse(RailInfo.NONE.isPresent());
    }
}
