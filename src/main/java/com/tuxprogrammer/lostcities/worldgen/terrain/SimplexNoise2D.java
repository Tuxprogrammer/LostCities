package com.tuxprogrammer.lostcities.worldgen.terrain;

import java.util.Random;

/**
 * Two-dimensional simplex noise with a seeded permutation table, values in about −1 to 1. It plays the part of
 * 1.20's {@code SimplexNoise}, which upstream uses for the terrain transition noise; this is the textbook algorithm
 * (Ken Perlin's simplex noise in Stefan Gustavson's formulation), written for this mod, not a copy of either game's
 * class. Values for a seed differ from upstream's, which no 1.7.10 world can be compared with anyway
 * (docs/new-spec/03-terrain/index.md, "Terrain blend", TERR-27 row).
 *
 * <p>
 * Immutable after construction; allocates nothing per sample. Side: both (plain Java).
 */
public final class SimplexNoise2D {

    private static final int[][] GRADIENTS = { { 1, 1 }, { -1, 1 }, { 1, -1 }, { -1, -1 }, { 1, 0 }, { -1, 0 },
        { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 }, { 0, 1 }, { 0, -1 } };

    private static final double SKEW = 0.5 * (Math.sqrt(3.0) - 1.0);
    private static final double UNSKEW = (3.0 - Math.sqrt(3.0)) / 6.0;
    /** Scales the summed corner contributions to about −1 … 1. */
    private static final double SCALE = 70.0;

    private final int[] permutation = new int[512];

    /**
     * Creates the noise for a seed.
     *
     * @param seed the seed of the permutation table
     */
    public SimplexNoise2D(long seed) {
        Random random = new Random(seed);
        int[] base = new int[256];
        for (int i = 0; i < 256; i++) {
            base[i] = i;
        }
        for (int i = 255; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int swap = base[i];
            base[i] = base[j];
            base[j] = swap;
        }
        for (int i = 0; i < 512; i++) {
            permutation[i] = base[i & 255];
        }
    }

    /**
     * Returns the noise at a point.
     *
     * @param x first coordinate
     * @param y second coordinate
     * @return a value in about −1 … 1
     */
    public double value(double x, double y) {
        double skew = (x + y) * SKEW;
        int i = floor(x + skew);
        int j = floor(y + skew);
        double unskew = (i + j) * UNSKEW;
        double x0 = x - (i - unskew);
        double y0 = y - (j - unskew);
        int i1 = x0 > y0 ? 1 : 0;
        int j1 = x0 > y0 ? 0 : 1;
        double x1 = x0 - i1 + UNSKEW;
        double y1 = y0 - j1 + UNSKEW;
        double x2 = x0 - 1.0 + 2.0 * UNSKEW;
        double y2 = y0 - 1.0 + 2.0 * UNSKEW;
        int ii = i & 255;
        int jj = j & 255;
        double n0 = corner(permutation[ii + permutation[jj]] % 12, x0, y0);
        double n1 = corner(permutation[ii + i1 + permutation[jj + j1]] % 12, x1, y1);
        double n2 = corner(permutation[ii + 1 + permutation[jj + 1]] % 12, x2, y2);
        return SCALE * (n0 + n1 + n2);
    }

    private static double corner(int gradient, double x, double y) {
        double t = 0.5 - x * x - y * y;
        if (t < 0) {
            return 0.0;
        }
        t *= t;
        return t * t * (GRADIENTS[gradient][0] * x + GRADIENTS[gradient][1] * y);
    }

    private static int floor(double value) {
        int truncated = (int) value;
        return value < truncated ? truncated - 1 : truncated;
    }
}
