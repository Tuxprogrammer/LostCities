package com.tuxprogrammer.lostcities.layout.landmark;

/**
 * Tells a structure oracle whether any raw city chunk (city factor above the threshold, before landmark zones) lies in
 * a chunk rectangle, so it can skip work that only matters beside a city: a landmark whose core and rings are all more
 * than {@link #EFFECT_CHUNKS} chunks from every raw city chunk changes nothing the city writes (its edge ring, soft
 * edges and blend anchors all lie within that distance of a city chunk; 0006 §1 and §4). The answer is the layout's
 * own pure city map, so an oracle that asks stays pure.
 *
 * <p>
 * Side: both (plain Java).
 */
@FunctionalInterface
public interface CityNearness {

    /**
     * How far, in chunks, a landmark core's effects reach toward a city: the edge ring (1), and the terrain blend
     * upstream reshapes within two chunks of a city (2).
     */
    int EFFECT_CHUNKS = 3;

    /** The conservative answer when no city map is known: a city may be anywhere. */
    CityNearness ANYWHERE = (minChunkX, minChunkZ, maxChunkX, maxChunkZ) -> true;

    /**
     * Returns whether any raw city chunk lies in a chunk rectangle (inclusive).
     *
     * @param minChunkX smallest chunk X
     * @param minChunkZ smallest chunk Z
     * @param maxChunkX largest chunk X
     * @param maxChunkZ largest chunk Z
     * @return {@code true} when a raw city chunk lies in it, or when the answer is not known
     */
    boolean anyRawCity(int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ);
}
