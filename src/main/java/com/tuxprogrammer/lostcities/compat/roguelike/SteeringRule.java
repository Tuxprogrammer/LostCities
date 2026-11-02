package com.tuxprogrammer.lostcities.compat.roguelike;

/**
 * The steering rule for Roguelike Dungeons (0006 §5, other-worldgen.md T4, decision log D31): a candidate point is
 * refused when its tower's lot (the 21 × 21 blocks around it, {@link RoguelikeOracle#lot}) touches a city chunk, an
 * edge-ring chunk or a scattered structure's footprint, unless the point is a predicted site whose lot is reserved
 * (no city chunk under it). The dungeon then lands in its lot, outside the city, or not at all, whatever order chunks
 * generate in, because every input is the pure layout.
 *
 * <p>
 * Under the default {@code BUILD_AROUND} treatment a predicted site's lot is a landmark core, never city and never an
 * edge ring, so the exception is what keeps the rule from refusing the site the city left room for; under
 * {@code IGNORE} the city covers the site and the tower is refused like any other.
 *
 * <p>
 * Pure. Side: both (plain Java).
 */
public final class SteeringRule {

    /** What the rule reads from the layout. */
    public interface Layout {

        /**
         * Returns whether a chunk is a city chunk.
         *
         * @param chunkX chunk X
         * @param chunkZ chunk Z
         * @return {@code true} for a city chunk
         */
        boolean isCity(int chunkX, int chunkZ);

        /**
         * Returns whether a chunk is in a landmark's edge ring.
         *
         * @param chunkX chunk X
         * @param chunkZ chunk Z
         * @return {@code true} for an edge-ring chunk
         */
        boolean isEdgeRing(int chunkX, int chunkZ);

        /**
         * Returns whether a scattered structure's footprint touches a chunk.
         *
         * @param chunkX chunk X
         * @param chunkZ chunk Z
         * @return {@code true} when one does
         */
        boolean isScattered(int chunkX, int chunkZ);

        /**
         * Returns whether a point is a predicted Roguelike site.
         *
         * @param blockX block X
         * @param blockZ block Z
         * @return {@code true} when the structure oracle predicts a tower there
         */
        boolean isPredictedSite(int blockX, int blockZ);
    }

    /** How the rule decided. */
    public enum Outcome {

        /** The lot touches nothing the city holds: Roguelike's own test decides. */
        CLEAR,
        /** The lot is a predicted site's reserved lot: Roguelike's own test decides. */
        PREDICTED,
        /** The lot touches a city chunk: refused. */
        CITY,
        /** The lot touches an edge ring: refused. */
        EDGE_RING,
        /** The lot touches a scattered structure: refused. */
        SCATTERED;

        /** Returns whether the point is refused. */
        public boolean refuses() {
            return this == CITY || this == EDGE_RING || this == SCATTERED;
        }
    }

    private SteeringRule() {}

    /**
     * Decides a candidate point.
     *
     * @param blockX the point's block X
     * @param blockZ the point's block Z
     * @param layout the world's layout
     * @return the outcome
     */
    public static Outcome decide(int blockX, int blockZ, Layout layout) {
        int minChunkX = (blockX - RoguelikeOracle.LOT_HALF) >> 4;
        int maxChunkX = (blockX + RoguelikeOracle.LOT_HALF) >> 4;
        int minChunkZ = (blockZ - RoguelikeOracle.LOT_HALF) >> 4;
        int maxChunkZ = (blockZ + RoguelikeOracle.LOT_HALF) >> 4;
        boolean city = false;
        boolean edge = false;
        boolean scattered = false;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                city |= layout.isCity(chunkX, chunkZ);
                edge |= layout.isEdgeRing(chunkX, chunkZ);
                scattered |= layout.isScattered(chunkX, chunkZ);
            }
        }
        if (!city && !edge && !scattered) {
            return Outcome.CLEAR;
        }
        if (!city && layout.isPredictedSite(blockX, blockZ)) {
            return Outcome.PREDICTED;
        }
        if (city) {
            return Outcome.CITY;
        }
        return edge ? Outcome.EDGE_RING : Outcome.SCATTERED;
    }
}
