package com.tuxprogrammer.lostcities.layout.landmark;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Another generator's structure the layout knows about, as 0006 §1 defines it: its kind, a footprint of one or more
 * block rectangles, how certain the footprint is, an estimate of its ground height from the height oracle, a village's
 * gates and a meteorite's height cap. Immutable; per-mod structure oracles in {@code compat} build it.
 *
 * <p>
 * Side: both.
 */
public final class Landmark {

    private final String id;
    private final LandmarkKind kind;
    private final List<BlockRect> footprint;
    private final Certainty certainty;
    private final int groundEstimate;
    private final List<Gate> gates;
    private final HeightCap heightCap;

    private Landmark(Builder builder) {
        this.id = builder.id;
        this.kind = builder.kind;
        this.footprint = Collections.unmodifiableList(new ArrayList<>(builder.footprint));
        this.certainty = builder.certainty;
        this.groundEstimate = builder.groundEstimate;
        this.gates = Collections.unmodifiableList(new ArrayList<>(builder.gates));
        this.heightCap = builder.heightCap;
    }

    /**
     * Starts a landmark.
     *
     * @param id   a stable identity, unique among landmarks, used in log lines and to order landmarks (for example
     *             {@code village@1024,-512})
     * @param kind the generator it comes from
     * @return a builder
     */
    public static Builder builder(String id, LandmarkKind kind) {
        return new Builder(id, kind);
    }

    /** Returns the landmark's stable identity. */
    public String id() {
        return id;
    }

    /** Returns the generator it comes from. */
    public LandmarkKind kind() {
        return kind;
    }

    /** Returns the footprint, one or more block rectangles. */
    public List<BlockRect> footprint() {
        return footprint;
    }

    /** Returns how certain the footprint is. */
    public Certainty certainty() {
        return certainty;
    }

    /** Returns the oracle's estimate of the landmark's ground height, block Y. */
    public int groundEstimate() {
        return groundEstimate;
    }

    /** Returns the village's gates; empty for every other kind. */
    public List<Gate> gates() {
        return gates;
    }

    /** Returns the meteorite's height cap, or {@code null}. */
    public HeightCap heightCap() {
        return heightCap;
    }

    /**
     * Returns the Chebyshev distance in chunks from a chunk to the landmark's core: 0 for a core chunk, a chunk the
     * footprint grown by {@code margin} blocks touches.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @param margin {@code landmarkMargin}, in blocks
     * @return the distance in chunks
     */
    public int coreDistance(int chunkX, int chunkZ, int margin) {
        int best = Integer.MAX_VALUE;
        for (BlockRect rect : footprint) {
            best = Math.min(
                best,
                rect.grow(margin)
                    .chunkDistance(chunkX, chunkZ));
        }
        return best;
    }

    /**
     * Returns the chunk rectangle {@code {minX, minZ, maxX, maxZ}} that bounds the landmark's core.
     *
     * @param margin {@code landmarkMargin}, in blocks
     * @return the bounding chunk rectangle
     */
    public int[] coreChunkBounds(int margin) {
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockRect rect : footprint) {
            BlockRect grown = rect.grow(margin);
            minX = Math.min(minX, grown.minChunkX());
            minZ = Math.min(minZ, grown.minChunkZ());
            maxX = Math.max(maxX, grown.maxChunkX());
            maxZ = Math.max(maxZ, grown.maxChunkZ());
        }
        return new int[] { minX, minZ, maxX, maxZ };
    }

    @Override
    public String toString() {
        return kind + " " + id;
    }

    /** Builds a {@link Landmark}. */
    public static final class Builder {

        private final String id;
        private final LandmarkKind kind;
        private final List<BlockRect> footprint = new ArrayList<>();
        private Certainty certainty = Certainty.EXACT;
        private int groundEstimate;
        private final List<Gate> gates = new ArrayList<>();
        private HeightCap heightCap;

        private Builder(String id, LandmarkKind kind) {
            this.id = id;
            this.kind = kind;
        }

        /**
         * Adds a footprint rectangle.
         *
         * @param rect the rectangle
         * @return this builder
         */
        public Builder footprint(BlockRect rect) {
            footprint.add(rect);
            return this;
        }

        /**
         * Sets the certainty (default {@link Certainty#EXACT}).
         *
         * @param value the certainty
         * @return this builder
         */
        public Builder certainty(Certainty value) {
            certainty = value;
            return this;
        }

        /**
         * Sets the ground estimate.
         *
         * @param y block Y
         * @return this builder
         */
        public Builder groundEstimate(int y) {
            groundEstimate = y;
            return this;
        }

        /**
         * Adds a gate (villages).
         *
         * @param gate the gate
         * @return this builder
         */
        public Builder gate(Gate gate) {
            gates.add(gate);
            return this;
        }

        /**
         * Sets the height cap (meteorites).
         *
         * @param cap the cap
         * @return this builder
         */
        public Builder heightCap(HeightCap cap) {
            heightCap = cap;
            return this;
        }

        /**
         * Builds the landmark.
         *
         * @return the landmark
         * @throws IllegalStateException when the footprint is empty
         */
        public Landmark build() {
            if (footprint.isEmpty()) {
                throw new IllegalStateException("landmark " + id + " has no footprint");
            }
            return new Landmark(this);
        }
    }
}
