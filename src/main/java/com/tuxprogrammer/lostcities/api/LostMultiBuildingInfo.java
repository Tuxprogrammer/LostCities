package com.tuxprogrammer.lostcities.api;

/**
 * A chunk's place in a multibuilding: the multibuilding, the chunk's offset from its north-west corner chunk, and its
 * size in chunks (docs/original-spec/01-core/api.md, {@code ILostChunkInfo.MultiBuildingInfo}).
 *
 * <p>
 * Immutable. Side: both.
 */
public final class LostMultiBuildingInfo {

    private final String buildingType;
    private final int offsetX;
    private final int offsetZ;
    private final int width;
    private final int height;

    /**
     * Creates the value.
     *
     * @param buildingType the multibuilding's name
     * @param offsetX      the chunk's X offset from the corner chunk
     * @param offsetZ      the chunk's Z offset from the corner chunk
     * @param width        the size along X, in chunks
     * @param height       the size along Z, in chunks
     */
    public LostMultiBuildingInfo(String buildingType, int offsetX, int offsetZ, int width, int height) {
        this.buildingType = buildingType;
        this.offsetX = offsetX;
        this.offsetZ = offsetZ;
        this.width = width;
        this.height = height;
    }

    /** Returns the multibuilding's name. */
    public String getBuildingType() {
        return buildingType;
    }

    /** Returns the chunk's X offset from the north-west corner chunk. */
    public int getOffsetX() {
        return offsetX;
    }

    /** Returns the chunk's Z offset from the north-west corner chunk. */
    public int getOffsetZ() {
        return offsetZ;
    }

    /** Returns the size along X, in chunks. */
    public int getWidth() {
        return width;
    }

    /** Returns the size along Z, in chunks. */
    public int getHeight() {
        return height;
    }

    @Override
    public String toString() {
        return buildingType + " (" + offsetX + ", " + offsetZ + " of " + width + " x " + height + ")";
    }
}
