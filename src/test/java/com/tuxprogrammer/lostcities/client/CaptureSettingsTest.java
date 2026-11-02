package com.tuxprogrammer.lostcities.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;

import org.junit.jupiter.api.Test;

/** The clean capture's view settings: defaults, clamping, and bad values falling back. */
public class CaptureSettingsTest {

    @Test
    public void defaultsWithoutProperties() {
        CaptureSettings settings = CaptureSettings.from(new Properties());
        assertEquals(70, settings.fov());
        assertEquals(10, settings.renderDistance());
        assertEquals(9, settings.chunkRadius(), "one less than the render distance");
        assertFalse(settings.clouds());
    }

    @Test
    public void valuesAreReadAndTrimmed() {
        Properties p = new Properties();
        p.setProperty(CaptureSettings.FOV_PROPERTY, " 90 ");
        p.setProperty(CaptureSettings.RENDER_DISTANCE_PROPERTY, "8");
        p.setProperty(CaptureSettings.CHUNK_RADIUS_PROPERTY, "5");
        p.setProperty(CaptureSettings.CLOUDS_PROPERTY, "true");
        CaptureSettings settings = CaptureSettings.from(p);
        assertEquals(90, settings.fov());
        assertEquals(8, settings.renderDistance());
        assertEquals(5, settings.chunkRadius());
        assertTrue(settings.clouds());
    }

    @Test
    public void numbersAreClampedToTheOptionRanges() {
        Properties p = new Properties();
        p.setProperty(CaptureSettings.FOV_PROPERTY, "200");
        p.setProperty(CaptureSettings.RENDER_DISTANCE_PROPERTY, "1");
        p.setProperty(CaptureSettings.CHUNK_RADIUS_PROPERTY, "40");
        CaptureSettings settings = CaptureSettings.from(p);
        assertEquals(110, settings.fov());
        assertEquals(2, settings.renderDistance());
        assertEquals(2, settings.chunkRadius(), "never beyond the render distance");

        p.setProperty(CaptureSettings.FOV_PROPERTY, "-5");
        p.setProperty(CaptureSettings.RENDER_DISTANCE_PROPERTY, "64");
        p.setProperty(CaptureSettings.CHUNK_RADIUS_PROPERTY, "-1");
        settings = CaptureSettings.from(p);
        assertEquals(30, settings.fov());
        assertEquals(16, settings.renderDistance());
        assertEquals(0, settings.chunkRadius());
    }

    @Test
    public void joinsOnlyAWellFormedServer() {
        Properties p = new Properties();
        assertFalse(CaptureSettings.from(p).joinsServer(), "no join without the property");

        p.setProperty(CaptureSettings.SERVER_PROPERTY, " 127.0.0.1:25800 ");
        CaptureSettings settings = CaptureSettings.from(p);
        assertTrue(settings.joinsServer());
        assertEquals("127.0.0.1", settings.serverHost());
        assertEquals(25800, settings.serverPort());

        for (String bad : new String[] { "127.0.0.1", ":25800", "127.0.0.1:", "host:port", "host:0", "host:70000" }) {
            p.setProperty(CaptureSettings.SERVER_PROPERTY, bad);
            assertFalse(CaptureSettings.from(p).joinsServer(), bad);
        }
    }

    @Test
    public void malformedNumbersTakeTheirDefaults() {
        Properties p = new Properties();
        p.setProperty(CaptureSettings.FOV_PROPERTY, "wide");
        p.setProperty(CaptureSettings.RENDER_DISTANCE_PROPERTY, "");
        CaptureSettings settings = CaptureSettings.from(p);
        assertEquals(70, settings.fov());
        assertEquals(10, settings.renderDistance());
    }
}
