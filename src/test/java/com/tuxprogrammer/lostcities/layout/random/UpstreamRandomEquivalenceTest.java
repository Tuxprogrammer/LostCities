package com.tuxprogrammer.lostcities.layout.random;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.ParityData;

/**
 * Settles the first unverified equivalence of docs/original-spec/01-core/noise-and-random.md § Equivalence with 1.7.10:
 * Minecraft 1.20.1's {@code LegacyRandomSource(seed)} draws the same {@code nextInt()}, {@code nextInt(bound)},
 * {@code nextLong()}, {@code nextFloat()}, {@code nextDouble()} and {@code nextBoolean()} as
 * {@code java.util.Random(seed)}.
 * The 1.20 values were recorded from the 1.20.1 server jar (tools/upstream-parity).
 */
public class UpstreamRandomEquivalenceTest {

    @Test
    public void legacyRandomSourceDrawsWhatJavaUtilRandomDraws() throws Exception {
        int compared = RandomDraws.check(ParityData.records("legacy_random.txt"), Random::new);
        assertTrue(compared > 300, "only " + compared + " values compared");
    }
}
