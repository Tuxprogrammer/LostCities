package com.tuxprogrammer.lostcities.layout.random;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Random;
import java.util.function.LongFunction;

import com.tuxprogrammer.lostcities.layout.ParityData;

/**
 * Replays the recorder's op sequence ({@code L}, {@code I}, {@code I7}, {@code I1000}, {@code I256}, {@code F},
 * {@code D}, {@code Z}) on a {@link Random} and compares every value with a recording.
 */
final class RandomDraws {

    private RandomDraws() {}

    /**
     * Checks a recording against a generator built per seed.
     *
     * @param records the parity records, {@code seed op value}
     * @param factory builds the generator for a seed
     * @return how many values were compared
     */
    static int check(List<String[]> records, LongFunction<Random> factory) {
        long currentSeed = 0;
        Random random = null;
        int compared = 0;
        for (String[] r : records) {
            long seed = Long.parseLong(r[0]);
            if (random == null || seed != currentSeed) {
                random = factory.apply(seed);
                currentSeed = seed;
            }
            String where = "seed " + seed + " op " + r[1] + " #" + compared;
            switch (r[1]) {
                case "L":
                    assertEquals(Long.parseLong(r[2]), random.nextLong(), where);
                    break;
                case "I":
                    assertEquals(Integer.parseInt(r[2]), random.nextInt(), where);
                    break;
                case "I7":
                    assertEquals(Integer.parseInt(r[2]), random.nextInt(7), where);
                    break;
                case "I1000":
                    assertEquals(Integer.parseInt(r[2]), random.nextInt(1000), where);
                    break;
                case "I256":
                    assertEquals(Integer.parseInt(r[2]), random.nextInt(256), where);
                    break;
                case "F":
                    assertEquals(ParityData.floatBits(r[2]), random.nextFloat(), where);
                    break;
                case "D":
                    assertEquals(ParityData.doubleBits(r[2]), random.nextDouble(), where);
                    break;
                case "Z":
                    assertEquals(Boolean.parseBoolean(r[2]), random.nextBoolean(), where);
                    break;
                default:
                    throw new IllegalStateException("unknown op " + r[1]);
            }
            compared++;
        }
        return compared;
    }
}
