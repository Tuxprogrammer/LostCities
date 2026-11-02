package com.tuxprogrammer.lostcities.compat.ae2;

import java.util.Random;

/**
 * A meteorite's size, crater size and lava, from its meteor seed alone, as {@code MeteoritePlacer}'s seeded
 * constructor draws them from {@code new Random(seed)}: {@code size = nextDouble() × 6 + 2} (2 to 8 blocks),
 * {@code craterSize = 2 × size + 5}, then, after the sky count (which draws nothing), {@code lava = nextFloat() > 0.9}.
 *
 * <p>
 * Source:
 * https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/rv3-beta-1095-GTNH/src/main/java/appeng/worldgen/MeteoritePlacer.java#L160-L213
 *
 * <p>
 * Immutable. Side: both (plain Java).
 */
public final class MeteoriteShape {

    private final double size;
    private final double craterSize;
    private final boolean lava;

    private MeteoriteShape(double size, double craterSize, boolean lava) {
        this.size = size;
        this.craterSize = craterSize;
        this.lava = lava;
    }

    /**
     * Draws a meteorite's shape.
     *
     * @param seed the meteor seed
     * @return the shape
     */
    public static MeteoriteShape of(long seed) {
        Random rng = new Random(seed);
        double size = rng.nextDouble() * 6.0D + 2;
        double crater = size * 2 + 5;
        boolean lava = rng.nextFloat() > 0.9F;
        return new MeteoriteShape(size, crater, lava);
    }

    /** Returns the meteorite's radius, blocks (AE2's {@code real_sizeOfMeteorite}). */
    public double size() {
        return size;
    }

    /** Returns the crater's size, blocks (AE2's {@code realCrater}). */
    public double craterSize() {
        return craterSize;
    }

    /** Returns whether the crater fills with lava. */
    public boolean lava() {
        return lava;
    }

    @Override
    public String toString() {
        return String.format("size %.2f crater %.2f%s", size, craterSize, lava ? " lava" : "");
    }
}
