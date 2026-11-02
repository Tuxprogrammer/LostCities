package com.tuxprogrammer.lostcities.layout.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.ParityData;

/**
 * Settles the second unverified equivalence of docs/original-spec/01-core/noise-and-random.md: {@link SimplexNoise}
 * over {@code java.util.Random(seed)} computes what Minecraft 1.20.1's {@code SimplexNoise(LegacyRandomSource(seed))}
 * computes, bit for bit, over a grid that includes negative, whole and zero coordinates (where 1.7.10's fast floor
 * would differ).
 */
public class SimplexNoiseTest {

    @Test
    public void matchesMinecraft120BitForBit() throws Exception {
        List<String[]> records = ParityData.records("simplex_noise.txt");
        Map<Long, SimplexNoise> noises = new HashMap<>();
        for (String[] r : records) {
            long seed = Long.parseLong(r[0]);
            SimplexNoise noise = noises.computeIfAbsent(seed, s -> new SimplexNoise(new Random(s)));
            double x = ParityData.doubleBits(r[1]);
            double y = ParityData.doubleBits(r[2]);
            double expected = ParityData.doubleBits(r[3]);
            assertEquals(
                Double.doubleToLongBits(expected),
                Double.doubleToLongBits(noise.getValue(x, y)),
                "seed " + seed + " at " + x + "," + y);
        }
        assertTrue(records.size() > 1000);
    }

    @Test
    public void constructorDrawsTheOffsetsFirst() {
        Random random = new Random(5L);
        double xo = random.nextDouble() * 256.0;
        SimplexNoise noise = new SimplexNoise(new Random(5L));
        assertEquals(xo, noise.xOffset());
    }
}
