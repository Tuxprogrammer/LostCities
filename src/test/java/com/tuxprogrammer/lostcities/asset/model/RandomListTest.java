package com.tuxprogrammer.lostcities.asset.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetProblem;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.AssetType;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.TestAssets;
import com.tuxprogrammer.lostcities.asset.block.BlockChoice;

/** Random lists: 128 slots filled by weight in list order (docs/original-spec/05-assets/palettes-and-variants.md). */
public class RandomListTest {

    private static final BlockChoice A = BlockChoice.of(BlockRef.parse("minecraft:dirt:0"));
    private static final BlockChoice B = BlockChoice.of(BlockRef.parse("minecraft:sand:0"));
    private static final BlockChoice C = BlockChoice.of(BlockRef.parse("minecraft:gravel:0"));

    @Test
    public void fillsSlotsInOrderAndDropsTheRest() {
        // [32 A, 32 B, 1000 C] is 32/128 A, 32/128 B, 64/128 C.
        RandomList list = RandomList.fill(Arrays.asList(32, 32, 1000), Arrays.asList(A, B, C));
        assertEquals(128, RandomList.SLOTS);
        assertEquals(A, list.slot(0));
        assertEquals(A, list.slot(31));
        assertEquals(B, list.slot(32));
        assertEquals(C, list.slot(64));
        assertEquals(C, list.slot(127));
        assertEquals(
            Arrays.asList("32xminecraft:dirt:0", "32xminecraft:sand:0", "64xminecraft:gravel:0"),
            list.describe());
        assertEquals(
            3,
            list.distinct()
                .size());
    }

    @Test
    public void fewerThan128SlotsIsRejected() {
        assertNull(RandomList.fill(Arrays.asList(64, 63), Arrays.asList(A, B)));
        AssetSet set = TestAssets.loadFiles(
            "lostcities/variants/v.json",
            "{\"blocks\": [{\"random\": 100, \"block\": \"minecraft:dirt\"}]}");
        assertEquals(0, set.count(AssetType.VARIANTS));
        assertTrue(
            TestAssets.lines(set, AssetProblem.Severity.ERROR)
                .get(0)
                .contains("fewer than 128 slots"));
    }

    @Test
    public void theBundledStonebrickVariantKeepsUpstreamsMix() {
        AssetSet set = TestAssets.bundled(TestAssets.vanilla());
        RandomList stonebrick = set.variant(AssetId.parse("stonebrick"))
            .blocks();
        int plain = 0;
        for (int i = 0; i < RandomList.SLOTS; i++) {
            if (stonebrick.slot(i)
                .block()
                .equals(BlockRef.parse("minecraft:stonebrick:0"))) {
                plain++;
            }
        }
        // docs/new-spec/05-assets/block-catalog/index.md: stonebrick:0 111, :2 9, :1 8 of 128.
        assertEquals(111, plain);
    }
}
