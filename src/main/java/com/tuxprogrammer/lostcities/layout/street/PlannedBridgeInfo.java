package com.tuxprogrammer.lostcities.layout.street;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/street/PlannedBridgeInfo.java

/**
 * A primary-road bridge span between two city chunks on the same primary road, across {@link #gapLength()} non-city
 * gap chunks.
 *
 * <p>
 * Spec: docs/original-spec/04-city-layout/bridges.md#hierarchical-bridge-hierarchicalbridgeplannergetbridgeinfo. Side:
 * both.
 */
public final class PlannedBridgeInfo {

    private final long id;
    private final Orientation orientation;
    private final int minimumX;
    private final int minimumZ;
    private final int maximumX;
    private final int maximumZ;
    private final int gapLength;

    /**
     * Creates a span.
     *
     * @param id          the span hash
     * @param orientation the axis the span runs along
     * @param minimumX    chunk X of the endpoint with the lower coordinate
     * @param minimumZ    chunk Z of that endpoint
     * @param maximumX    chunk X of the endpoint with the higher coordinate
     * @param maximumZ    chunk Z of that endpoint
     * @param gapLength   gap chunks between the endpoints
     */
    public PlannedBridgeInfo(long id, Orientation orientation, int minimumX, int minimumZ, int maximumX, int maximumZ,
        int gapLength) {
        this.id = id;
        this.orientation = orientation;
        this.minimumX = minimumX;
        this.minimumZ = minimumZ;
        this.maximumX = maximumX;
        this.maximumZ = maximumZ;
        this.gapLength = gapLength;
    }

    /** Returns the span hash. */
    public long id() {
        return id;
    }

    /** Returns the axis the span runs along. */
    public Orientation orientation() {
        return orientation;
    }

    /** Returns chunk X of the lower endpoint. */
    public int minimumX() {
        return minimumX;
    }

    /** Returns chunk Z of the lower endpoint. */
    public int minimumZ() {
        return minimumZ;
    }

    /** Returns chunk X of the higher endpoint. */
    public int maximumX() {
        return maximumX;
    }

    /** Returns chunk Z of the higher endpoint. */
    public int maximumZ() {
        return maximumZ;
    }

    /** Returns the number of gap chunks between the endpoints. */
    public int gapLength() {
        return gapLength;
    }
}
