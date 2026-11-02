package com.tuxprogrammer.lostcities.worldgen.terrain;

import java.util.Random;

/**
 * Upstream's {@code NoiseGeneratorPerlin} with its {@code getRegion}: octaves of 2D simplex noise summed over a 16 × 16
 * region, octave {@code k} sampled at {@code scale × factor^k / 2^k} with amplitude {@code 0.55 × 2^k}. Upstream's
 * ruin,
 * rubble and leaves noises are four octaves each (docs/original-spec/03-terrain/damage-ruins-rubble.md, Numbers).
 *
 * <p>
 * Every octave is a {@link SimplexNoise2D} with its own seed and a random offset, both from the world seed and a salt
 * per noise (TERR-27), so the values differ from upstream's, which no 1.7.10 world can be compared with anyway; the
 * shape and the scales are upstream's. Immutable; allocates nothing per sample. Side: both (plain Java).
 */
public final class OctaveNoise {

    /** Amplitude of the first octave, doubling with each octave (upstream's {@code 0.55 / d1}). */
    static final double BASE_AMPLITUDE = 0.55;
    /** Range of the random coordinate offset of each octave. */
    private static final double OFFSET_RANGE = 256.0;

    private final SimplexNoise2D[] octaves;
    private final double[] offsetX;
    private final double[] offsetZ;

    /**
     * Creates the noise.
     *
     * @param worldSeed the world seed
     * @param salt      tells this noise from the others of the world
     * @param levels    the number of octaves
     */
    public OctaveNoise(long worldSeed, long salt, int levels) {
        octaves = new SimplexNoise2D[levels];
        offsetX = new double[levels];
        offsetZ = new double[levels];
        Random random = new Random(SeedMix.mix(worldSeed, salt));
        for (int i = 0; i < levels; i++) {
            octaves[i] = new SimplexNoise2D(random.nextLong());
            offsetX[i] = random.nextDouble() * OFFSET_RANGE;
            offsetZ[i] = random.nextDouble() * OFFSET_RANGE;
        }
    }

    /**
     * Fills a 16 × 16 region, as upstream's {@code getRegion}.
     *
     * @param out    256 values, index {@code x + z × 16}; overwritten
     * @param x      the region's first X, in noise units before scaling
     * @param z      the region's first Z
     * @param scale  the first octave's scale per unit
     * @param factor how much each octave's scale grows before halving (upstream's {@code factor})
     */
    public void region(double[] out, double x, double z, double scale, double factor) {
        java.util.Arrays.fill(out, 0.0);
        double grow = 1.0;
        double halve = 1.0;
        for (int k = 0; k < octaves.length; k++) {
            double s = scale * grow * halve;
            double amplitude = BASE_AMPLITUDE / halve;
            for (int dz = 0; dz < 16; dz++) {
                double nz = (z + dz) * s + offsetZ[k];
                for (int dx = 0; dx < 16; dx++) {
                    double nx = (x + dx) * s + offsetX[k];
                    out[dx + dz * 16] += octaves[k].value(nx, nz) * amplitude;
                }
            }
            grow *= factor;
            halve *= 0.5;
        }
    }
}
