package com.tuxprogrammer.lostcities.compat.villagenames;

import java.util.Random;

/**
 * Puts a map generator's {@code Random} in the state VillageNames' village generator has when it decides a chunk, as
 * vanilla 1.7.10's {@code MapGenBase.generate} and {@code MapGenStructure.func_151538_a} leave it: seeded with the
 * world seed, {@code a = nextLong()} and {@code b = nextLong()}, reseeded with {@code chunkX × a ^ chunkZ × b ^
 * worldSeed}, then one {@code nextInt()} drawn before the structure is asked for its start. The start's size draw and
 * every piece of its plan come from that state.
 *
 * <p>
 * The {@code Random} must be of the class the live generator uses. In GTNH 2.9.0 that is not {@code java.util.Random}:
 * Hodgepodge's {@code fastRandom} (on in the pack's {@code hodgepodge.cfg}) makes every {@code MapGenBase} build a
 * {@code StdLCG}, whose {@code setSeed} skips {@code Random}'s seed scramble, so the same seeds give other numbers.
 * The oracle therefore passes the {@code rand} of a {@code MapGenBase} of its own, which Hodgepodge treats the same way
 * (decision log D85).
 *
 * <p>
 * Source: vanilla 1.7.10 {@code MapGenBase.generate} and {@code MapGenStructure.func_151538_a} (no pinned public
 * source); Hodgepodge 2.7.221,
 * https://github.com/GTNewHorizons/Hodgepodge/blob/2.7.221/src/main/java/com/mitchej123/hodgepodge/mixins/early/minecraft/fastload/rand/MixinMapGenBase.java
 * and
 * https://github.com/GTNewHorizons/Hodgepodge/blob/2.7.221/src/main/java/com/mitchej123/hodgepodge/util/StdLCG.java.
 *
 * <p>
 * Side: both (plain Java).
 */
public final class MapGenSeeds {

    private MapGenSeeds() {}

    /**
     * Returns the seed {@code MapGenBase.generate} gives its {@code Random} for a chunk.
     *
     * @param random    a {@code Random} of the generator's class; reseeded, so its state is lost
     * @param worldSeed the world seed
     * @param chunkX    chunk X
     * @param chunkZ    chunk Z
     * @return the per-chunk seed
     */
    public static long chunkSeed(Random random, long worldSeed, int chunkX, int chunkZ) {
        random.setSeed(worldSeed);
        long a = random.nextLong();
        long b = random.nextLong();
        return chunkX * a ^ chunkZ * b ^ worldSeed;
    }

    /**
     * Leaves a generator's {@code Random} as {@code MapGenStructure} leaves it just before it asks whether a chunk
     * starts a structure: the chunk seed set and one {@code nextInt()} drawn.
     *
     * @param random    a {@code Random} of the generator's class
     * @param worldSeed the world seed
     * @param chunkX    chunk X
     * @param chunkZ    chunk Z
     * @return {@code random}, ready for {@code getStructureStart}
     */
    public static Random prepareForStart(Random random, long worldSeed, int chunkX, int chunkZ) {
        random.setSeed(chunkSeed(random, worldSeed, chunkX, chunkZ));
        random.nextInt();
        return random;
    }
}
