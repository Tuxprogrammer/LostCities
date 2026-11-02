package com.tuxprogrammer.lostcities.worldgen.terrain;

import com.tuxprogrammer.lostcities.asset.BlockRef;

/**
 * The block classes the terrain code tests blocks against: upstream's {@code isEmpty} (air, water, lava), the
 * {@code foliage} and {@code climbable} tags terrain correction looks through, the {@code not_breakable} and
 * {@code easy_breakable} tags explosion damage reads, and light sources. 1.7.10 has no block tags, so each is a rule
 * over the block registry (docs/new-spec/08-loot-mobs-blocks/index.md, "Block tags", P08-1).
 *
 * <p>
 * Implementations: {@link NameBlockClassifier}, a fixed list of vanilla names for tests and as a fallback, and the
 * registry-backed classifier the overlay builds from the live registry (worldgen.overlay). The terrain phase calls
 * these for most blocks it reads, so implementations answer from a cache and allocate nothing.
 *
 * <p>
 * Side: both (plain Java).
 */
public interface BlockClassifier {

    /** Air. */
    int AIR = 1;
    /** Water or lava, still or flowing. */
    int LIQUID = 1 << 1;
    /** Logs, leaves, plants, saplings, vines and huge mushrooms: what terrain correction looks through. */
    int FOLIAGE = 1 << 2;
    /** Never changed by explosion damage (negative hardness, end portal and frame). */
    int NOT_BREAKABLE = 1 << 3;
    /** Takes 2.5 times the explosion damage (glass material). */
    int EASY_BREAKABLE = 1 << 4;
    /** Emits light. */
    int LIGHT = 1 << 5;
    /** Bedrock, where {@code moveUp} stops copying. */
    int BEDROCK = 1 << 6;

    /**
     * Returns the class bits of a block.
     *
     * @param block the block
     * @return a combination of the constants above; 0 for an ordinary solid block
     */
    int flags(BlockRef block);

    /** Returns upstream's {@code isEmpty}: air, water or lava. */
    default boolean isEmpty(BlockRef block) {
        return (flags(block) & (AIR | LIQUID)) != 0;
    }

    /** Returns upstream's {@code isFoliageOrEmpty}: empty, a plant or a climbable block. */
    default boolean isFoliageOrEmpty(BlockRef block) {
        return (flags(block) & (AIR | LIQUID | FOLIAGE)) != 0;
    }

    /** Returns whether a block is water or lava. */
    default boolean isLiquid(BlockRef block) {
        return (flags(block) & LIQUID) != 0;
    }

    /** Returns whether a block is a plant or climbable. */
    default boolean isFoliage(BlockRef block) {
        return (flags(block) & FOLIAGE) != 0;
    }
}
