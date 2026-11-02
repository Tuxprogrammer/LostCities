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
}
