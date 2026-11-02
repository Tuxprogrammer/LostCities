package com.tuxprogrammer.lostcities.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * {@link CityRarityMap} reproduces upstream's Perlin city rarity for the same seed and parameters, as upstream computes
 * it over Minecraft 1.20.1's own simplex noise (values recorded by tools/upstream-parity).
 */
public class CityRarityMapTest {

    @Test
    public void matchesUpstreamOverMinecraftsNoise() throws Exception {
        List<String[]> records = ParityData.records("city_rarity.txt");
        for (String[] r : records) {
            long seed = Long.parseLong(r[0]);
            double scale = Double.parseDouble(r[1]);
            double offset = Double.parseDouble(r[2]);
            double inner = Double.parseDouble(r[3]);
            int cx = Integer.parseInt(r[4]);
            int cz = Integer.parseInt(r[5]);
            float expected = ParityData.floatBits(r[6]);
            CityRarityMap map = new CityRarityMap(seed, scale, offset, inner);
            assertEquals(expected, map.getCityFactor(cx, cz), String.join(" ", r));
        }
        assertTrue(records.size() > 2000);
    }

    @Test
    public void neverNegative() {
        CityRarityMap map = new CityRarityMap(1L, 3, 10, .1);
        assertEquals(0f, map.getCityFactor(5, 5));
    }
}
