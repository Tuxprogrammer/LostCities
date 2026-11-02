package com.tuxprogrammer.lostcities.client;

import java.util.Properties;

/**
 * The view settings of the clean screenshot capture ({@link CleanCapture}), read from JVM properties and clamped to
 * the ranges the game's own option sliders allow. Plain Java, so the parsing is unit-tested without a client.
 *
 * <p>
 * Side: client.
 */
public final class CaptureSettings {

    /** JVM property that turns the clean capture on (with the test hooks): {@code -Dlostcitiesnh.capture=true}. */
    public static final String PROPERTY = "lostcitiesnh.capture";
    /** Field of view in degrees, as the options slider sets it: 30 to 110, default {@value #DEFAULT_FOV}. */
    public static final String FOV_PROPERTY = "lostcitiesnh.captureFov";
    /** Render distance in chunks: 2 to 16, default {@value #DEFAULT_RENDER_DISTANCE}. */
    public static final String RENDER_DISTANCE_PROPERTY = "lostcitiesnh.captureRenderDistance";
    /**
     * Radius in chunks around the player that must have arrived before a viewpoint counts as settled; default one less
     * than the render distance. It must not exceed the server's {@code view-distance}, or the wait never ends.
     */
    public static final String CHUNK_RADIUS_PROPERTY = "lostcitiesnh.captureChunkRadius";
    /** Whether clouds are drawn: default {@code false}, since they hide a city from above. */
    public static final String CLOUDS_PROPERTY = "lostcitiesnh.captureClouds";
    /**
     * Server the client joins from the main menu, {@code host:port}; unset, the client does not join by itself. The
     * shot runner uses this instead of the launcher's {@code --server}, which races FML's handshake (see
     * {@link CleanCapture}).
     */
    public static final String SERVER_PROPERTY = "lostcitiesnh.captureServer";

    static final int DEFAULT_FOV = 70;
    static final int DEFAULT_RENDER_DISTANCE = 10;

    private final int fov;
    private final int renderDistance;
    private final int chunkRadius;
    private final boolean clouds;
    private final String serverHost;
    private final int serverPort;

    private CaptureSettings(int fov, int renderDistance, int chunkRadius, boolean clouds, String serverHost,
        int serverPort) {
        this.fov = fov;
        this.renderDistance = renderDistance;
        this.chunkRadius = chunkRadius;
        this.clouds = clouds;
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    /**
     * Reads the settings from a property set; a missing or malformed value takes its default, and every number is
     * clamped to its range.
     *
     * @param properties the JVM's properties, or a test's
     * @return the settings
     */
    public static CaptureSettings from(Properties properties) {
        int fov = clamp(intValue(properties, FOV_PROPERTY, DEFAULT_FOV), 30, 110);
        int renderDistance = clamp(intValue(properties, RENDER_DISTANCE_PROPERTY, DEFAULT_RENDER_DISTANCE), 2, 16);
        int chunkRadius = clamp(intValue(properties, CHUNK_RADIUS_PROPERTY, renderDistance - 1), 0, renderDistance);
        boolean clouds = Boolean.parseBoolean(properties.getProperty(CLOUDS_PROPERTY, "false"));
        String host = null;
        int port = 0;
        String server = properties.getProperty(SERVER_PROPERTY, "").trim();
        int colon = server.lastIndexOf(':');
        if (colon > 0 && colon < server.length() - 1) {
            int parsed = intValue(server.substring(colon + 1), -1);
            if (parsed >= 1 && parsed <= 65535) {
                host = server.substring(0, colon);
                port = parsed;
            }
        }
        return new CaptureSettings(fov, renderDistance, chunkRadius, clouds, host, port);
    }

    /** Returns whether the client joins a server by itself from the main menu. */
    public boolean joinsServer() {
        return serverHost != null;
    }

    /** Returns the host to join, or {@code null}. */
    public String serverHost() {
        return serverHost;
    }

    /** Returns the port to join, or 0. */
    public int serverPort() {
        return serverPort;
    }

    /** Returns the field of view in degrees. */
    public int fov() {
        return fov;
    }

    /** Returns the render distance in chunks. */
    public int renderDistance() {
        return renderDistance;
    }

    /** Returns the radius, in chunks, that must be loaded before a viewpoint settles. */
    public int chunkRadius() {
        return chunkRadius;
    }

    /** Returns whether clouds are drawn. */
    public boolean clouds() {
        return clouds;
    }

    @Override
    public String toString() {
        return "fov=" + fov
            + " renderDistance="
            + renderDistance
            + " chunkRadius="
            + chunkRadius
            + " clouds="
            + clouds
            + " server="
            + (serverHost == null ? "none" : serverHost + ":" + serverPort);
    }

    private static int intValue(Properties properties, String key, int fallback) {
        return intValue(properties.getProperty(key), fallback);
    }

    private static int intValue(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
