package com.tuxprogrammer.lostcities.layout.street;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/Orientation.java

/**
 * The axis a bridge, corridor or connection runs along: {@link #X} east-west, {@link #Z} north-south.
 *
 * <p>
 * Spec: docs/original-spec/04-city-layout/transforms.md. Side: both.
 */
public enum Orientation {

    /** Along X (east-west). */
    X,
    /** Along Z (north-south). */
    Z;

    /**
     * Returns a chunk coordinate along this axis.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return {@code chunkX} for {@link #X}, {@code chunkZ} for {@link #Z}
     */
    public int along(int chunkX, int chunkZ) {
        return this == X ? chunkX : chunkZ;
    }
}
