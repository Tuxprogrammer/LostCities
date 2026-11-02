package com.tuxprogrammer.lostcities.compat.villagenames;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * The map generator's per-chunk seeding, for {@code java.util.Random} and for a generator whose {@code setSeed} skips
 * the scramble, as Hodgepodge's {@code StdLCG} does in the pack. Values worked out by an independent
 * re-implementation outside Java.
 */
class MapGenSeedsTest {

    /** A {@code Random} whose {@code setSeed} keeps the seed as given, like Hodgepodge's {@code StdLCG}. */
    static final class UnscrambledRandom extends Random {

        private static final long serialVersionUID = 1L;
        private static final long MASK = (1L << 48) - 1;
        private long state;

        @Override
        public synchronized void setSeed(long seed) {
            state = seed;
        }

        @Override
        protected int next(int bits) {
            state = state * 0x5DEECE66DL + 0xBL & MASK;
            return (int) (state >>> 48 - bits);
        }
    }

    @Test
    void javaRandomChunkSeeds() {
        assertEquals(0L, MapGenSeeds.chunkSeed(new Random(), 0L, 0, 0));
        assertEquals(8245630663160864586L, MapGenSeeds.chunkSeed(new Random(), 0L, 5, -9));
        assertEquals(12345L, MapGenSeeds.chunkSeed(new Random(), 12345L, 0, 0));
        assertEquals(6159140767192314628L, MapGenSeeds.chunkSeed(new Random(), 12345L, 5, -9));
    }

    @Test
    void unscrambledChunkSeedsDiffer() {
        assertEquals(-6911607880898508035L, MapGenSeeds.chunkSeed(new UnscrambledRandom(), 0L, 5, -9));
        assertEquals(6667384638988267529L, MapGenSeeds.chunkSeed(new UnscrambledRandom(), 12345L, 5, -9));
    }

    @Test
    void prepareDrawsOneIntAfterTheChunkSeed() {
        assertEquals(-723955400, MapGenSeeds.prepareForStart(new Random(), 0L, 0, 0).nextInt());
        assertEquals(-1954219380, MapGenSeeds.prepareForStart(new Random(), 0L, 5, -9).nextInt());
        assertEquals(490914327, MapGenSeeds.prepareForStart(new Random(), 12345L, 5, -9).nextInt());
        assertEquals(4232237, MapGenSeeds.prepareForStart(new UnscrambledRandom(), 0L, 0, 0).nextInt());
        assertEquals(1383851049, MapGenSeeds.prepareForStart(new UnscrambledRandom(), 0L, 5, -9).nextInt());
        assertEquals(-177857392, MapGenSeeds.prepareForStart(new UnscrambledRandom(), 12345L, 5, -9).nextInt());
    }

    @Test
    void reseedingForgetsEarlierState() {
        Random random = new Random(777L);
        random.nextGaussian();
        assertEquals(-1954219380, MapGenSeeds.prepareForStart(random, 0L, 5, -9).nextInt());
    }
}
