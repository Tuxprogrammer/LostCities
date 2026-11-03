package com.tuxprogrammer.lostcities.worldgen.height;

/**
 * Answers the base terrain height of a column without generating or loading a chunk: the stone surface before caves,
 * map features and landmarks, as upstream 1.20's noise-only {@code getBaseHeight(OCEAN_FLOOR_WG)} answers it
 * (docs/design/0005-rwg-overlay.md §4). The layout takes every height from an oracle, never from loaded chunks, so
 * neighbors always agree and nothing cascades.
 *
 * <p>
 * Implementations: {@code compat.rwg2.Rwg2HeightOracle} (a private world-less RWG2 generator),
 * {@code compat.rwg.RwgHeightOracle} (a private second classic-RWG generator) and {@link ConstantHeightOracle} (flat
 * and PersonalSpace worlds). An oracle is never the world's live generator, whose {@code getNewNoise} would corrupt
 * the next chunk.
 *
 * <p>
 * Side: server (world generation), and plain Java in tests.
 */
@FunctionalInterface
public interface TerrainHeightOracle {

    /**
     * Returns the height of a column: the Y of the first non-solid block above the base terrain, so the column's
     * stone ends at {@code height − 1}. On RWG that is {@code (int) noise + 1}, because both versions fill stone for
     * {@code y ≤ (int) noise} (docs/new-spec/03-terrain/index.md, "Heights").
     *
     * @param blockX block X
     * @param blockZ block Z
     * @return block Y, 0 to 256
     */
    int height(int blockX, int blockZ);

    /**
     * Returns whether one call computes the heights of the whole chunk anyway (classic RWG's {@code getNewNoise}), so a
     * per-chunk cache in front of it ({@link ChunkColumnCache}) makes further columns of that chunk free.
     *
     * @return {@code true} for a whole-chunk oracle; {@code false} by default
     */
    default boolean computesWholeChunks() {
        return false;
    }

    /**
     * Returns whether a column's height is already held, so asking for it costs no oracle work. The plan prefetcher's
     * probe ({@link HeightProbe}) asks this before it counts a sample as missing.
     *
     * @param blockX block X
     * @param blockZ block Z
     * @return {@code false} by default: every call computes
     */
    default boolean isCached(int blockX, int blockZ) {
        return false;
    }

    /**
     * Returns the world's probe that guards this oracle's cache, so a structure oracle reading heights through it can
     * defer its own costly work the same way ({@link HeightProbe}).
     *
     * @return the probe, or {@code null} when nothing guards this oracle
     */
    default HeightProbe probe() {
        return null;
    }

    /**
     * Returns how many times a cache in front of an oracle has asked it, for the budget report (0005 §8): whole chunks
     * for {@link ChunkColumnCache}, columns for {@link ColumnHeightCache}.
     *
     * @return the count; 0 for an oracle that is not such a cache
     */
    default long computations() {
        return 0;
    }

    /**
     * Fills the heights of every column of a chunk, index {@code x × 16 + z}.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @param out    256 heights, overwritten
     */
    default void chunkHeights(int chunkX, int chunkZ, int[] out) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                out[x * 16 + z] = height((chunkX << 4) + x, (chunkZ << 4) + z);
            }
        }
    }
}
