package com.tuxprogrammer.lostcities.worldgen.city;

import com.tuxprogrammer.lostcities.worldgen.terrain.SeedMix;

/**
 * Upstream's per-chunk palette sequence ({@code GenerationContext.next128}): the linear congruential generator
 * {@code seed = 214013 × seed + 2531011}, whose bits 16 to 22 pick the slot of a random palette character, one draw per
 * block placed (docs/new-spec/03-terrain/index.md, {@code GenerationContext} row). The seed mixes in the world seed and
 * the chunk (TERR-27), so the sequence is a pure function of the chunk.
 *
 * <p>
 * Not thread-safe; one per chunk being written. Side: both (plain Java).
 */
public final class PaletteSequence {

    /** Upstream's multiplier. */
    private static final int MULTIPLIER = 214013;
    /** Upstream's increment. */
    private static final int INCREMENT = 2531011;
    /** Shift to the slot bits. */
    private static final int SHIFT = 16;
    /** Mask of a slot, 0 to 127. */
    private static final int SLOT_MASK = 0x7F;
    /** Upstream's chunk X multiplier of the generation context seed. */
    private static final long CHUNK_X = 257017164707L;
    /** Upstream's chunk Z multiplier of the generation context seed. */
    private static final long CHUNK_Z = 101754694003L;

    private int seed;

    /**
     * Starts the sequence of one chunk.
     *
     * @param worldSeed the world seed
     * @param chunkX    chunk X
     * @param chunkZ    chunk Z
     */
    public PaletteSequence(long worldSeed, int chunkX, int chunkZ) {
        long mixed = SeedMix.chunk(worldSeed, chunkX, chunkZ, CHUNK_X, CHUNK_Z);
        this.seed = (int) (mixed ^ (mixed >>> 32));
    }

    /** Returns the next slot, 0 to 127. */
    public int next() {
        seed = MULTIPLIER * seed + INCREMENT;
        return (seed >> SHIFT) & SLOT_MASK;
    }
}
