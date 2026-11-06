package com.tuxprogrammer.lostcities.worldgen.plan;

import com.tuxprogrammer.lostcities.layout.random.StableHash;

/**
 * Which empty edge lots hold a house built by another pack mod's own village generator
 * (docs/design/0011-city-edges-and-character.md, pack houses). A pure function of the world seed and the chunk, so the
 * choice is the same in whatever order chunks generate and in every session.
 *
 * <p>
 * Side: both (plain Java).
 */
public final class PackHouseSelection {

    /** Salt of the selection hash. */
    static final long SALT = 0x5041434b484f5553L;
    /** Salt of the candidate pick, so the pick is independent of the selection roll. */
    static final long PICK_SALT = 0x5049434b4b494e44L;

    /** Salt of the {@code Random} a house's piece is built from. */
    public static final long SEED_SALT = 0x484f555345524e44L;

    private PackHouseSelection() {}

    /**
     * Returns whether an empty edge lot holds a pack house.
     *
     * @param worldSeed the world seed
     * @param chunkX    the lot's chunk X
     * @param chunkZ    the lot's chunk Z
     * @param chance    the share of lots, 0 to 1
     * @return {@code true} to build a house there
     */
    public static boolean picks(long worldSeed, int chunkX, int chunkZ, float chance) {
        if (chance <= 0f) {
            return false;
        }
        return StableHash.unitDouble(hash(worldSeed ^ SALT, chunkX, chunkZ)) < chance;
    }

    /**
     * Returns the index of the house kind a lot holds.
     *
     * @param worldSeed the world seed
     * @param chunkX    the lot's chunk X
     * @param chunkZ    the lot's chunk Z
     * @param kinds     how many kinds are available, at least 1
     * @return an index from 0 to {@code kinds - 1}
     */
    public static int kind(long worldSeed, int chunkX, int chunkZ, int kinds) {
        return (int) Math.floorMod(hash(worldSeed ^ PICK_SALT, chunkX, chunkZ), (long) kinds);
    }

    private static long hash(long seed, int chunkX, int chunkZ) {
        return StableHash.mix64(seed ^ chunkX * 341873128712L ^ chunkZ * 132897987541L);
    }
}
