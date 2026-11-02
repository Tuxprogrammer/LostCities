package com.tuxprogrammer.lostcities.worldgen.terrain;

import java.util.Objects;

/**
 * What every terrain step of one world shares: the world seed and dimension, the pinned terrain settings, the block
 * classes, which blocks exist, and the world-seeded transition noise of the blend. One per wrapped world, built when
 * the overlay is installed.
 *
 * <p>
 * Immutable. Side: both (plain Java).
 */
public final class TerrainContext {

    /** Salt of the transition noise seed, from upstream's {@code terrainTransitionNoise} seed formula. */
    private static final long TRANSITION_SALT = 0x6A09E667F3BCC909L;
    /** Multiplier of the dimension term, from the same formula. */
    private static final long DIMENSION_SPREAD = 0x9E3779B97F4A7C15L;

    private final long worldSeed;
    private final int dimension;
    private final TerrainSettings settings;
    private final BlockClassifier classifier;
    private final BlockAvailability availability;
    private final SimplexNoise2D transitionNoise;

    /**
     * Creates the context.
     *
     * @param worldSeed    the world seed
     * @param dimension    the dimension ID (0 for the overworld)
     * @param settings     the pinned settings
     * @param classifier   the block classes
     * @param availability which blocks exist
     */
    public TerrainContext(long worldSeed, int dimension, TerrainSettings settings, BlockClassifier classifier,
        BlockAvailability availability) {
        this.worldSeed = worldSeed;
        this.dimension = dimension;
        this.settings = Objects.requireNonNull(settings);
        this.classifier = Objects.requireNonNull(classifier);
        this.availability = Objects.requireNonNull(availability);
        // Upstream: worldSeed ^ dimensionHash × 0x9e3779b97f4a7c15 ^ 0x6a09e667f3bcc909, with the 1.7.10 dimension ID
        // standing in for the 1.20 dimension key's hash.
        this.transitionNoise = new SimplexNoise2D(worldSeed ^ (dimension * DIMENSION_SPREAD) ^ TRANSITION_SALT);
    }

    /** Returns the world seed. */
    public long worldSeed() {
        return worldSeed;
    }

    /** Returns the dimension ID. */
    public int dimension() {
        return dimension;
    }

    /** Returns the pinned terrain settings. */
    public TerrainSettings settings() {
        return settings;
    }

    /** Returns the block classes. */
    public BlockClassifier classifier() {
        return classifier;
    }

    /** Returns which blocks exist. */
    public BlockAvailability availability() {
        return availability;
    }

    /**
     * Returns upstream's terrain transition noise at a world column: {@code 3 × simplex(x / 28, z / 28)} plus
     * {@code simplex((x + 10000) / 11, (z − 10000) / 11)}, about −4 … 4.
     *
     * @param worldX block X
     * @param worldZ block Z
     * @return the noise
     */
    public float transitionNoise(int worldX, int worldZ) {
        double broad = transitionNoise.value(worldX / 28.0, worldZ / 28.0) * 3.0;
        double detail = transitionNoise.value((worldX + 10000) / 11.0, (worldZ - 10000) / 11.0);
        return (float) (broad + detail);
    }
}
