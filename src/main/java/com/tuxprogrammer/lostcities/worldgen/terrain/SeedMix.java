package com.tuxprogrammer.lostcities.worldgen.terrain;

/**
 * Seeds for the coordinate-seeded randoms of terrain generation, each mixing in the world seed. Upstream seeds the
 * blend jitter, the damage random and the street re-roll from chunk coordinates alone, so every world got the same
 * jitter and the same debris (TERR-27); here every seed is {@code worldSeed} combined with upstream's coordinate
 * formula, so values differ from upstream's but are a pure function of seed and coordinates
 * (docs/new-spec/03-terrain/index.md, "Concurrency, caches, determinism").
 *
 * <p>
 * Side: both (plain Java).
 */
public final class SeedMix {

    /** Odd 64-bit multiplier (the golden ratio) that spreads the world seed before it is combined. */
    private static final long SPREAD = 0x9E3779B97F4A7C15L;

    private SeedMix() {}

    /**
     * Combines the world seed with a coordinate-derived value.
     *
     * @param worldSeed  the world seed
     * @param coordinate upstream's coordinate formula, for example {@code chunkZ × 256203221 + chunkX × 899809363}
     * @return the seed to give {@code java.util.Random}
     */
    public static long mix(long worldSeed, long coordinate) {
        return (worldSeed * SPREAD) ^ coordinate;
    }

    /**
     * Returns the seed of a chunk's own random for one purpose.
     *
     * @param worldSeed the world seed
     * @param chunkX    chunk X
     * @param chunkZ    chunk Z
     * @param xFactor   upstream's X multiplier for that purpose
     * @param zFactor   upstream's Z multiplier for that purpose
     * @return the mixed seed
     */
    public static long chunk(long worldSeed, int chunkX, int chunkZ, long xFactor, long zFactor) {
        return mix(worldSeed, chunkX * xFactor + chunkZ * zFactor);
    }
}
