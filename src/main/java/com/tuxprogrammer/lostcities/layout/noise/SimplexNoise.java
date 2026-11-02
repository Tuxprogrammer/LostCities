package com.tuxprogrammer.lostcities.layout.noise;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/varia/NoiseGeneratorSimplex.java

import java.util.Random;

/**
 * Two-dimensional simplex noise over a permutation table drawn from a {@link Random}: upstream's
 * {@code NoiseGeneratorSimplex} (statement for statement 1.7.10's vanilla class of the same name), as 1.20's
 * {@code SimplexNoise} evaluates it. The constructor draws three {@code nextDouble} (the offsets) and then 256
 * {@code nextInt(256 - l)}, as both do.
 *
 * <p>
 * One detail follows 1.20 rather than 1.7.10: the skewed cell coordinate is {@link Math#floor(double)}; 1.7.10 and
 * upstream's own copy use a "fast floor" that is one too low for whole non-positive values. Upstream's city rarity
 * map ({@code PerlinNoiseGenerator14}) evaluates 1.20's class, so this is the value it sees; the recorded 1.20.1
 * values in {@code src/test/resources/parity/simplex_noise.txt} include such points.
 *
 * <p>
 * Upstream's own terrain noise ({@code varia/NoiseGeneratorSimplex}, {@code NoiseGeneratorPerlin}) keeps the fast
 * floor; reproducing it takes 1.7.10's vanilla classes or a port that keeps it, not this class.
 *
 * <p>
 * Spec: docs/original-spec/01-core/noise-and-random.md#noisegeneratorsimplex and #equivalence-with-1710. Side: both.
 */
public final class SimplexNoise {

    /**
     * The twelve edge gradients of a cube. Only X and Y enter a 2D value; Z is kept so the dot product is computed as
     * 1.20 computes it ({@code g.x * x + g.y * y + g.z * 0}), down to the sign of a zero result.
     */
    private static final int[][] GRADIENTS = { { 1, 1, 0 }, { -1, 1, 0 }, { 1, -1, 0 }, { -1, -1, 0 }, { 1, 0, 1 },
        { -1, 0, 1 }, { 1, 0, -1 }, { -1, 0, -1 }, { 0, 1, 1 }, { 0, -1, 1 }, { 0, 1, -1 }, { 0, -1, -1 } };
    /** The third coordinate of a 2D evaluation. */
    private static final double PLANE_Z = 0.0D;
    private static final double SQRT_3 = Math.sqrt(3.0D);
    /** Skew factor for two dimensions. */
    private static final double F2 = 0.5D * (SQRT_3 - 1.0D);
    /** Unskew factor for two dimensions. */
    private static final double G2 = (3.0D - SQRT_3) / 6.0D;
    private static final int TABLE_SIZE = 256;
    private static final int TABLE_MASK = 255;
    private static final double OFFSET_RANGE = 256.0D;
    private static final double CORNER_FALLOFF = 0.5D;
    /** Scales the summed corner contributions to roughly [-1, 1]. */
    private static final double OUTPUT_SCALE = 70.0D;

    private final int[] permutation = new int[TABLE_SIZE];
    private final double xOffset;
    private final double yOffset;
    private final double zOffset;

    /**
     * Draws the offsets and the permutation table.
     *
     * @param random the source, consumed by 3 {@code nextDouble} and 256 {@code nextInt}
     */
    public SimplexNoise(Random random) {
        xOffset = random.nextDouble() * OFFSET_RANGE;
        yOffset = random.nextDouble() * OFFSET_RANGE;
        zOffset = random.nextDouble() * OFFSET_RANGE;
        for (int i = 0; i < TABLE_SIZE; i++) {
            permutation[i] = i;
        }
        for (int l = 0; l < TABLE_SIZE; l++) {
            int j = random.nextInt(TABLE_SIZE - l) + l;
            int k = permutation[l];
            permutation[l] = permutation[j];
            permutation[j] = k;
        }
    }

    /** Returns the X offset the constructor drew (upstream's {@code xo}), in noise units. */
    public double xOffset() {
        return xOffset;
    }

    /** Returns the Y offset the constructor drew (upstream's {@code yo}), in noise units. */
    public double yOffset() {
        return yOffset;
    }

    /** Returns the Z offset the constructor drew (upstream's {@code zo}), in noise units. */
    public double zOffset() {
        return zOffset;
    }

    private int p(int index) {
        return permutation[index & TABLE_MASK];
    }

    /**
     * Evaluates the noise.
     *
     * @param x the first coordinate, in noise units
     * @param y the second coordinate, in noise units
     * @return the noise value, roughly in {@code [-1, 1]}
     */
    public double getValue(double x, double y) {
        double skew = (x + y) * F2;
        int i = (int) Math.floor(x + skew);
        int j = (int) Math.floor(y + skew);
        double unskew = (i + j) * G2;
        double x0 = x - (i - unskew);
        double y0 = y - (j - unskew);
        int i1;
        int j1;
        if (x0 > y0) {
            i1 = 1;
            j1 = 0;
        } else {
            i1 = 0;
            j1 = 1;
        }
        double x1 = x0 - i1 + G2;
        double y1 = y0 - j1 + G2;
        double x2 = x0 - 1.0D + 2.0D * G2;
        double y2 = y0 - 1.0D + 2.0D * G2;
        int ii = i & TABLE_MASK;
        int jj = j & TABLE_MASK;
        int g0 = p(ii + p(jj)) % GRADIENTS.length;
        int g1 = p(ii + i1 + p(jj + j1)) % GRADIENTS.length;
        int g2 = p(ii + 1 + p(jj + 1)) % GRADIENTS.length;
        return OUTPUT_SCALE * (corner(g0, x0, y0) + corner(g1, x1, y1) + corner(g2, x2, y2));
    }

    private static double corner(int gradient, double x, double y) {
        double t = CORNER_FALLOFF - x * x - y * y - PLANE_Z * PLANE_Z;
        if (t < 0.0D) {
            return 0.0D;
        }
        t *= t;
        int[] g = GRADIENTS[gradient];
        return t * t * (g[0] * x + g[1] * y + g[2] * PLANE_Z);
    }
}
