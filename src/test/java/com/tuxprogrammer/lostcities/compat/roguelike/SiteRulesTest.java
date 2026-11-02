package com.tuxprogrammer.lostcities.compat.roguelike;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.worldgen.height.TerrainHeightOracle;

/** The estimated site test, rule by rule, over fake heights and biomes (Dungeon.java#L165-L212 at 1.6.6-GTNH). */
class SiteRulesTest {

    /** Grass-topped land everywhere, no rejected biome. */
    private static final RoguelikeTerrain GRASS = new RoguelikeTerrain() {

        @Override
        public boolean biomeRejects(int blockX, int blockZ) {
            return false;
        }

        @Override
        public boolean grassTop(int blockX, int blockZ) {
            return true;
        }
    };

    private static SiteRules rules(TerrainHeightOracle heights) {
        return new SiteRules(RoguelikeSettings.pack(), heights, GRASS);
    }

    @Test
    void flatGrassPasses() {
        assertEquals(SiteRules.Verdict.PASS, rules((x, z) -> 72).test(0, 0));
    }

    @Test
    void rejectedBiomeFailsFirst() {
        SiteRules rules = new SiteRules(RoguelikeSettings.pack(), (x, z) -> 300, new RoguelikeTerrain() {

            @Override
            public boolean biomeRejects(int blockX, int blockZ) {
                return true;
            }

            @Override
            public boolean grassTop(int blockX, int blockZ) {
                return true;
            }
        });
        assertEquals(SiteRules.Verdict.BIOME, rules.test(0, 0));
    }

    @Test
    void groundAtTheUpperLimitIsNotAir() {
        // Height 101: the top block is at Y 100, the upper limit.
        assertEquals(SiteRules.Verdict.NO_AIR, rules((x, z) -> 101).test(0, 0));
        assertEquals(SiteRules.Verdict.PASS, rules((x, z) -> 100).test(0, 0));
    }

    @Test
    void groundBelowTheLowerLimitIsTooLow() {
        // With the pack's limits (60) every column above the sea has ground above 60; a higher limit shows the rule.
        RoguelikeSettings high = new RoguelikeSettings(
            true,
            16,
            100,
            70,
            Collections.singletonList(0),
            Collections.emptyList());
        assertEquals(SiteRules.Verdict.PASS, new SiteRules(high, (x, z) -> 72, GRASS).test(0, 0));
        assertEquals(SiteRules.Verdict.TOO_LOW, new SiteRules(high, (x, z) -> 71, GRASS).test(0, 0));
        // The scan reaches Y 69 before the sea's top at 62, so the sea reads as too low too.
        assertEquals(SiteRules.Verdict.TOO_LOW, new SiteRules(high, (x, z) -> 50, GRASS).test(0, 0));
    }

    @Test
    void seaFailsAsWater() {
        assertEquals(SiteRules.Verdict.WATER, rules((x, z) -> 50).test(0, 0));
    }

    @Test
    void groundFourAboveTheSiteNearbyIsCrowded() {
        // The site's valid ground is 70; a column 4 blocks off reaching valid ground 74 (height 76) fails it.
        TerrainHeightOracle bump = (x, z) -> x == 4 && z == -4 ? 76 : 72;
        assertEquals(SiteRules.Verdict.CROWDED, rules(bump).test(0, 0));
        TerrainHeightOracle lower = (x, z) -> x == 4 && z == -4 ? 75 : 72;
        assertEquals(SiteRules.Verdict.PASS, rules(lower).test(0, 0));
        TerrainHeightOracle outside = (x, z) -> x == 5 ? 90 : 72;
        assertEquals(SiteRules.Verdict.PASS, rules(outside).test(0, 0));
    }

    @Test
    void moreThanEightGapsBelowIsHollow() {
        // Valid ground 70 at the site; the lower layer is at 67. A column whose valid ground is below 67 is a gap.
        TerrainHeightOracle eightAround = (x, z) -> z == 2 && x >= -4 && x <= 3 ? 66 : 72;
        assertEquals(SiteRules.Verdict.PASS, rules(eightAround).test(0, 0));
        TerrainHeightOracle nineAround = (x, z) -> z == 2 && x >= -4 && x <= 4 ? 66 : 72;
        assertEquals(SiteRules.Verdict.HOLLOW, rules(nineAround).test(0, 0));
    }

    @Test
    void sandTopCountsAsGround() {
        SiteRules sand = new SiteRules(RoguelikeSettings.pack(), (x, z) -> x == 4 ? 75 : 72, new RoguelikeTerrain() {

            @Override
            public boolean biomeRejects(int blockX, int blockZ) {
                return false;
            }

            @Override
            public boolean grassTop(int blockX, int blockZ) {
                return blockX != 4;
            }
        });
        // Site ground 70 (grass); column x = 4 is sand-topped at height 75: valid ground 74 = 70 + 4.
        assertEquals(SiteRules.Verdict.CROWDED, sand.test(0, 0));
    }
}
