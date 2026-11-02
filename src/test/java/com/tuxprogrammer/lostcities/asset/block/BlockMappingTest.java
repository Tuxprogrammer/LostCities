package com.tuxprogrammer.lostcities.asset.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.TestAssets;

/** The block-name map as data (D19; docs/new-spec/00-pack-integration/block-mapping.md). */
public class BlockMappingTest {

    private static final BlockData DATA = TestAssets.blockData();

    @Test
    public void everyChainEndsInAVanillaBlockAndPlacesNothingForbidden() {
        List<String> problems = new ArrayList<>();
        for (MappingRow row : DATA.mapping()
            .rows()) {
            if (row.hardAir() || row.matchOnly()) {
                continue;
            }
            BlockRef last = row.chain()
                .get(
                    row.chain()
                        .size() - 1)
                .base();
            if (!TestAssets.vanillaNames()
                .contains(last.name())) {
                problems.add(row.id() + " ends in " + last + ", which is not a vanilla 1.7.10 block");
            }
            for (MappingRow.Candidate candidate : row.chain()) {
                String reason = DATA.neverPlace()
                    .reason(candidate.base(), false);
                if (reason != null && !"minecraft:bedrock".equals(row.id())
                    && !row.id()
                        .endsWith("_block")) {
                    problems.add(row.id() + " maps to never-placed " + candidate.base() + " (" + reason + ")");
                }
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    public void colorRowsExpandInDyeOrder() {
        MappingRow cyan = DATA.mapping()
            .find("minecraft:cyan_terracotta", null);
        assertEquals(
            BlockRef.parse("minecraft:stained_hardened_clay:9"),
            cyan.chain()
                .get(0)
                .base());
        MappingRow black = DATA.mapping()
            .find("minecraft:black_concrete", null);
        assertEquals(
            BlockRef.parse("etfuturum:concrete:15"),
            black.chain()
                .get(0)
                .base());
        assertEquals(
            BlockRef.parse("minecraft:stained_hardened_clay:15"),
            black.chain()
                .get(1)
                .base());
    }

    @Test
    public void scopedRowsApplyOnlyToTheirAssets() {
        MappingRow cabin = DATA.mapping()
            .find("minecraft:exposed_copper", AssetId.parse("cabin"));
        assertEquals(DecorativeMetal.COPPER, cabin.metal());
        assertTrue(cabin.capExempt());
        MappingRow elsewhere = DATA.mapping()
            .find("minecraft:exposed_copper", AssetId.parse("building1"));
        assertEquals(null, elsewhere.metal());
        assertTrue(
            DATA.mapping()
                .isTreasure(AssetId.parse("common"), '!'));
    }

    @Test
    public void everyDecorativeMetalHasBlocks() {
        for (DecorativeMetal metal : DecorativeMetal.values()) {
            assertNotNull(DecorativeMetal.byKey(metal.key()));
        }
        assertEquals(
            DecorativeMetal.COPPER,
            DATA.metals()
                .metalOf(BlockRef.parse("etfuturum:copper_block:1")));
        assertEquals(
            DecorativeMetal.CAST_IRON,
            DATA.metals()
                .metalOf(BlockRef.parse("gregtech:gt.blockmetal8:2")));
        assertEquals(
            null,
            DATA.metals()
                .metalOf(BlockRef.parse("gregtech:gt.blockmetal8:3")));
    }

    @Test
    public void everyBlockNameTheBundledAssetsUseIsMapped() {
        // AssetValidationTest loads the whole set; this names the rows D19 lists that the assets never use, so a
        // re-pin or an override can use them too.
        for (String id : new String[] { "minecraft:oak_door", "minecraft:jungle_door", "minecraft:infested_deepslate",
            "minecraft:moss_block", "minecraft:cut_red_sandstone", "minecraft:sandstone_slab",
            "minecraft:red_sandstone_stairs" }) {
            assertNotNull(
                DATA.mapping()
                    .find(id, null),
                id);
        }
    }
}
