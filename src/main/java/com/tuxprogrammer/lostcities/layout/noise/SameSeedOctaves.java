package com.tuxprogrammer.lostcities.layout.noise;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/varia/PerlinNoiseGenerator14.java

import java.util.Random;

/**
 * Upstream's {@code PerlinNoiseGenerator14}: octaves of {@link SimplexNoise} summed as {@code value(x f, y f) / f} with
 * {@code f} halving each octave. Every octave is built from a fresh {@code Random(seed)}, so all octaves share one
 * permutation table (CORE-25). Decision log D16 keeps that as upstream has it: the city distribution was tuned on it.
 * The unused {@code getSurfaceNoiseValue} is not ported.
 *
 * <p>
 * Upstream builds each octave from 1.20's {@code LegacyRandomSource(seed)}; the recorded values in
 * {@code src/test/resources/parity/legacy_random.txt} show it draws the same sequence as
 * {@code java.util.Random(seed)}.
 *
 * <p>
 * Spec: docs/original-spec/01-core/noise-and-random.md#perlinnoisegenerator14. Side: both.
 */
public final class SameSeedOctaves {

    private final SimplexNoise[] octaves;

    /**
     * Builds the octaves.
     *
     * @param seed   the seed every octave's {@code Random} starts from
     * @param levels the number of octaves
     */
    public SameSeedOctaves(long seed, int levels) {
        octaves = new SimplexNoise[levels];
        for (int i = 0; i < levels; i++) {
            octaves[i] = new SimplexNoise(new Random(seed));
        }
    }

    /**
     * Sums the octaves.
     *
     * @param x the first coordinate, in noise units
     * @param y the second coordinate, in noise units
     * @return the summed value
     */
    public double getValue(double x, double y) {
        double sum = 0.0D;
        double frequency = 1.0D;
        for (SimplexNoise octave : octaves) {
            sum += octave.getValue(x * frequency, y * frequency) / frequency;
            frequency /= 2.0D;
        }
        return sum;
    }
}
