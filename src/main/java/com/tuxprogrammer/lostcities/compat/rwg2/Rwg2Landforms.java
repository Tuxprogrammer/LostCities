package com.tuxprogrammer.lostcities.compat.rwg2;

/**
 * The four RWG2 chunk-manager predicates the terrain-landmark oracle reads ({@link Rwg2LandmarkOracle}): pure functions
 * of the world seed, RWG2's config and the block column, the same ones RWG2 itself uses to keep Witchery off its
 * landmarks. At runtime {@code ManagerLandforms} answers them from the world's {@code ChunkManagerRealistic}; tests use
 * fakes.
 *
 * <p>
 * Side: server (world generation), and plain Java in tests.
 */
public interface Rwg2Landforms {

    /** The value of a packed center when there is none ({@code Long.MIN_VALUE}, as RWG2 returns). */
    long NONE = Long.MIN_VALUE;

    /**
     * Returns the center of the volcano whose vicinity (its island, {@value Rwg2LandmarkOracle#VOLCANO_RADIUS}
     * blocks) holds a column, when RWG2 lets that volcano generate.
     *
     * @param blockX block X
     * @param blockZ block Z
     * @return the center packed by {@link #pack}, or {@link #NONE}
     */
    long volcanoCenter(int blockX, int blockZ);

    /**
     * Returns the center of the lava cave whose influence holds a column, when RWG2 lets that cave generate. The
     * surface opening is the disc of {@value Rwg2LandmarkOracle#LAVA_CAVE_OPENING} blocks around it.
     *
     * @param blockX block X
     * @param blockZ block Z
     * @return the center packed by {@link #pack}, or {@link #NONE}
     */
    long lavaCaveCenter(int blockX, int blockZ);

    /**
     * Returns RWG2's river-junction strength at a column, 0 to 1.
     *
     * @param blockX block X
     * @param blockZ block Z
     * @return the strength
     */
    float junctionStrength(int blockX, int blockZ);

    /**
     * Returns whether a column's RWG2 biome is a mountain chain.
     *
     * @param blockX block X
     * @param blockZ block Z
     * @return whether it is
     */
    boolean mountainChain(int blockX, int blockZ);

    /**
     * Packs a block position as RWG2 packs centers: X in the high 32 bits, Z in the low 32.
     *
     * @param x block X
     * @param z block Z
     * @return the packed position
     */
    static long pack(int x, int z) {
        return (long) x << 32 | z & 0xFFFFFFFFL;
    }

    /** Returns the X of a packed position. */
    static int unpackX(long packed) {
        return (int) (packed >> 32);
    }

    /** Returns the Z of a packed position. */
    static int unpackZ(long packed) {
        return (int) packed;
    }
}
