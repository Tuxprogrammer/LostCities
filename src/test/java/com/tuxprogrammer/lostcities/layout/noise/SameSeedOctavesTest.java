package com.tuxprogrammer.lostcities.layout.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * {@link SameSeedOctaves} sums same-seed octaves as upstream's {@code PerlinNoiseGenerator14} does (CORE-25 kept by
 * D16); {@code CityRarityMapTest} checks it end to end against upstream's recorded rarity values.
 */
public class SameSeedOctavesTest {

    @Test
    public void sumsHalvingOctavesOfOneTable() {
        SimplexNoise single = new SimplexNoise(new Random(42L));
        double x = 1.7;
        double y = -3.2;
        double expected = single.getValue(x, y) + single.getValue(x * .5, y * .5) / .5
            + single.getValue(x * .25, y * .25) / .25;
        assertEquals(expected, new SameSeedOctaves(42L, 3).getValue(x, y));
    }
}
