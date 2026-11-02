package com.tuxprogrammer.lostcities.layout;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/CityRarityMap.java

import com.tuxprogrammer.lostcities.layout.noise.SameSeedOctaves;

/**
 * The Perlin city-rarity field used when {@code cityChance} is negative: four same-seed simplex octaves at
 * {@code chunk / cityPerlinScale}, scaled by {@code cityPerlinInnerScale}, minus {@code cityPerlinOffset}, floored at
 * 0.
 * Upstream caches one map per dimension with the first caller's parameters (CITY-05); here each planner builds its own
 * from its profile, so the parameters always match. The recorded upstream values in
 * {@code src/test/resources/parity/city_rarity.txt} pin it.
 *
 * <p>
 * Spec: docs/original-spec/04-city-layout/city-detection.md#city-factor-citygetcityfactor. Side: both.
 */
public final class CityRarityMap {

    /** Upstream's octave count. */
    private static final int OCTAVES = 4;

    private final SameSeedOctaves noise;
    private final double scale;
    private final double offset;
    private final double innerScale;

    /**
     * Creates the field.
     *
     * @param seed       the world seed
     * @param scale      {@code cityPerlinScale}, chunks
     * @param offset     {@code cityPerlinOffset}
     * @param innerScale {@code cityPerlinInnerScale}
     */
    public CityRarityMap(long seed, double scale, double offset, double innerScale) {
        this.noise = new SameSeedOctaves(seed, OCTAVES);
        this.scale = scale;
        this.offset = offset;
        this.innerScale = innerScale;
    }

    /**
     * Returns the rarity factor of a chunk.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the factor, 0 or more
     */
    public float getCityFactor(int chunkX, int chunkZ) {
        double factor = noise.getValue(chunkX / scale, chunkZ / scale) * innerScale - offset;
        if (factor < 0) {
            factor = 0;
        }
        return (float) factor;
    }
}
