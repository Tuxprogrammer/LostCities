package com.tuxprogrammer.lostcities.asset.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * One test per D19 rule (docs/new-spec/00-pack-integration/block-mapping.md, "Blockstate properties to metadata"):
 * each 1.20 property set becomes the 1.7.10 metadata the same block stores. Values are the pre-flattening encodings
 * that 1.8 to 1.12 kept from 1.7.10, which the 1.13 flattening turned into these properties.
 */
public class MetaRuleTest {

    private static int bits(MetaRule rule, String name) {
        MetaRule.Result result = rule.apply(
            BlockName.parse(name)
                .properties());
        assertTrue(
            result.errors()
                .isEmpty(),
            result.errors()
                .toString());
        return result.bits();
    }

    @Test
    public void stairsFacingAndHalf() {
        assertEquals(0, bits(MetaRule.STAIRS, "x:s[facing=east]"));
        assertEquals(1, bits(MetaRule.STAIRS, "x:s[facing=west]"));
        assertEquals(2, bits(MetaRule.STAIRS, "x:s[facing=south]"));
        assertEquals(3, bits(MetaRule.STAIRS, "x:s[facing=north]"));
        assertEquals(3, bits(MetaRule.STAIRS, "x:s"), "1.20's default facing is north");
        assertEquals(4, bits(MetaRule.STAIRS, "x:s[facing=east,half=top,shape=straight]"));
        assertEquals(7, bits(MetaRule.STAIRS, "x:s[facing=north,half=top]"));
    }

    @Test
    public void slabsBottomTopAndDouble() {
        assertEquals(0, bits(MetaRule.SLAB, "x:s[type=bottom]"));
        assertEquals(8, bits(MetaRule.SLAB, "x:s[type=top,waterlogged=false]"));
        assertEquals(0, bits(MetaRule.SLAB, "x:s[type=double]"), "the double slab is another block, chosen by the row");
    }

    @Test
    public void doorHalvesHingesAndOpen() {
        assertEquals(0, bits(MetaRule.DOOR, "x:d[facing=east,half=lower]"));
        assertEquals(1, bits(MetaRule.DOOR, "x:d[facing=south,half=lower]"));
        assertEquals(2, bits(MetaRule.DOOR, "x:d[facing=west,half=lower]"));
        assertEquals(3, bits(MetaRule.DOOR, "x:d[facing=north,half=lower,hinge=right]"));
        assertEquals(7, bits(MetaRule.DOOR, "x:d[facing=north,half=lower,open=true]"));
        assertEquals(8, bits(MetaRule.DOOR, "x:d[facing=north,half=upper,hinge=left]"));
        assertEquals(9, bits(MetaRule.DOOR, "x:d[facing=north,half=upper,hinge=right]"));
        assertEquals(11, bits(MetaRule.DOOR, "x:d[half=upper,hinge=right,powered=true]"));
    }

    @Test
    public void torchesStandingAndOnWalls() {
        assertEquals(5, bits(MetaRule.TORCH, "x:t[lit=true]"));
        assertEquals(1, bits(MetaRule.WALL_TORCH, "x:t[facing=east,lit=true]"));
        assertEquals(2, bits(MetaRule.WALL_TORCH, "x:t[facing=west]"));
        assertEquals(3, bits(MetaRule.WALL_TORCH, "x:t[facing=south]"));
        assertEquals(4, bits(MetaRule.WALL_TORCH, "x:t[facing=north]"));
    }

    @Test
    public void laddersFurnacesAndChests() {
        assertEquals(2, bits(MetaRule.FACING, "x:c[facing=north]"));
        assertEquals(3, bits(MetaRule.FACING, "x:c[facing=south]"));
        assertEquals(4, bits(MetaRule.FACING, "x:c[facing=west]"));
        assertEquals(5, bits(MetaRule.FACING, "x:c[facing=east]"));
        assertEquals(2, bits(MetaRule.FACING, "x:c"));
    }

    @Test
    public void leversOnWallsFloorsAndCeilings() {
        assertEquals(1, bits(MetaRule.LEVER, "x:l[face=wall,facing=east]"));
        assertEquals(4, bits(MetaRule.LEVER, "x:l[face=wall,facing=north,powered=false]"));
        assertEquals(11, bits(MetaRule.LEVER, "x:l[face=wall,facing=south,powered=true]"));
        assertEquals(5, bits(MetaRule.LEVER, "x:l[face=floor,facing=north]"));
        assertEquals(6, bits(MetaRule.LEVER, "x:l[face=floor,facing=east]"));
        assertEquals(7, bits(MetaRule.LEVER, "x:l[face=ceiling,facing=south]"));
        assertEquals(0, bits(MetaRule.LEVER, "x:l[face=ceiling,facing=west]"));
    }

    @Test
    public void railShapes() {
        String[] shapes = { "north_south", "east_west", "ascending_east", "ascending_west", "ascending_north",
            "ascending_south", "south_east", "south_west", "north_west", "north_east" };
        for (int i = 0; i < shapes.length; i++) {
            assertEquals(i, bits(MetaRule.RAIL, "x:r[shape=" + shapes[i] + "]"));
        }
        assertEquals(9, bits(MetaRule.POWERED_RAIL, "x:r[shape=east_west,powered=true]"));
        assertEquals(5, bits(MetaRule.POWERED_RAIL, "x:r[shape=ascending_south]"));
        assertFalse(
            MetaRule.POWERED_RAIL.apply(
                BlockName.parse("x:r[shape=north_east]")
                    .properties())
                .errors()
                .isEmpty(),
            "powered rails have no curves");
    }

    @Test
    public void trapdoorsFacingOpenAndHalf() {
        assertEquals(0, bits(MetaRule.TRAPDOOR, "x:t[facing=north]"));
        assertEquals(1, bits(MetaRule.TRAPDOOR, "x:t[facing=south]"));
        assertEquals(2, bits(MetaRule.TRAPDOOR, "x:t[facing=west]"));
        assertEquals(3, bits(MetaRule.TRAPDOOR, "x:t[facing=east,half=bottom,open=false,powered=false]"));
        assertEquals(15, bits(MetaRule.TRAPDOOR, "x:t[facing=east,half=top,open=true]"));
    }

    @Test
    public void saplingStageAndLeafPersistence() {
        assertEquals(8, bits(MetaRule.SAPLING, "x:s[stage=1]"));
        assertEquals(0, bits(MetaRule.SAPLING, "x:s"));
        assertEquals(4, bits(MetaRule.LEAVES, "x:l[persistent=true,distance=7]"));
    }

    @Test
    public void barrelsChainsLanternsAnvilsAndVines() {
        assertEquals(2, bits(MetaRule.BARREL, "x:b[facing=north]"));
        assertEquals(13, bits(MetaRule.BARREL, "x:b[facing=east,open=true]"));
        assertEquals(1, bits(MetaRule.BARREL, "x:b[facing=up]"));
        assertEquals(0, bits(MetaRule.CHAIN, "x:c"));
        assertEquals(1, bits(MetaRule.CHAIN, "x:c[axis=x]"));
        assertEquals(2, bits(MetaRule.CHAIN, "x:c[axis=z]"));
        assertEquals(1, bits(MetaRule.LANTERN, "x:l[hanging=true]"));
        assertEquals(2, bits(MetaRule.ANVIL, "x:a[facing=north]"));
        assertEquals(3, bits(MetaRule.ANVIL, "x:a[facing=east]"));
        assertEquals(1, bits(MetaRule.VINE, "x:v[south=true]"));
        assertEquals(2, bits(MetaRule.VINE, "x:v[west=true]"));
        assertEquals(4, bits(MetaRule.VINE, "x:v[north=true]"));
        assertEquals(8, bits(MetaRule.VINE, "x:v[east=true,up=false]"));
    }

    @Test
    public void connectionsAndWaterloggingAreDropped() {
        MetaRule.Result fence = MetaRule.PLAIN.apply(
            BlockName.parse("x:f[east=false,north=true,south=true,west=true,waterlogged=false]")
                .properties());
        assertEquals(0, fence.bits());
        assertTrue(
            fence.warnings()
                .isEmpty());
    }

    @Test
    public void unknownValuesAreErrorsAndUnknownPropertiesWarnings() {
        assertFalse(
            MetaRule.STAIRS.apply(
                BlockName.parse("x:s[facing=up]")
                    .properties())
                .errors()
                .isEmpty());
        assertFalse(
            MetaRule.PLAIN.apply(
                BlockName.parse("x:s[color=red]")
                    .properties())
                .warnings()
                .isEmpty());
    }
}
