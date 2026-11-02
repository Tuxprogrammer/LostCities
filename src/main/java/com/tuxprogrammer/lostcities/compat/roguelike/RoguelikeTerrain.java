package com.tuxprogrammer.lostcities.compat.roguelike;

/**
 * What the site rules need to know about a column besides its height, from the world's biome provider: whether
 * Roguelike's biome test rejects it, and whether its top block is one Roguelike does not count as ground (grass), so
 * its valid ground is one block lower. At runtime it is {@code WorldRoguelikeTerrain}; tests use fakes.
 *
 * <p>
 * Side: server (world generation), and plain Java in tests.
 */
public interface RoguelikeTerrain {

    /**
     * Returns whether Roguelike rejects a column's biome: a river, beach, mushroom or ocean dictionary type.
     *
     * @param blockX block X
     * @param blockZ block Z
     * @return {@code true} to reject
     */
    boolean biomeRejects(int blockX, int blockZ);

    /**
     * Returns whether a column's top block is grass (material {@code grass}), which Roguelike's ground test skips, so
     * the first valid ground is the block under it.
     *
     * @param blockX block X
     * @param blockZ block Z
     * @return {@code true} for a grass top
     */
    boolean grassTop(int blockX, int blockZ);
}
