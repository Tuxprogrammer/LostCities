package com.tuxprogrammer.lostcities.compat.rwg;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * The {@code rwg:} matcher's names: a realistic biome's class name without the {@code RealisticBiome} prefix, in lower
 * snake case (docs/new-spec/05-assets/asset-format.md, "Biome matchers"). Class names from RWG2 2.0.6 and RWG
 * alpha-1.5.3 ({@code rwg2/biomes/realistic/red/RealisticBiomeCanyon.java} and its siblings).
 */
public class RealisticBiomesTest {

    @Test
    public void classNamesBecomeMatcherNames() {
        assertEquals("canyon", RealisticBiomes.nameOf("RealisticBiomeCanyon"));
        assertEquals("canyon_forest", RealisticBiomes.nameOf("RealisticBiomeCanyonForest"));
        assertEquals("oasis", RealisticBiomes.nameOf("RealisticBiomeOasis"));
        assertEquals("red_oasis", RealisticBiomes.nameOf("RealisticBiomeRedOasis"));
        assertEquals("bop_volcano", RealisticBiomes.nameOf("RealisticBiomeBOPVolcano"));
        assertEquals("mountain_chain", RealisticBiomes.nameOf("RealisticBiomeMountainChain"));
    }
}
