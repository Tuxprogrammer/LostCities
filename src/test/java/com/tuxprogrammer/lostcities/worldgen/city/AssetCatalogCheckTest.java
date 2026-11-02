package com.tuxprogrammer.lostcities.worldgen.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.AssetType;
import com.tuxprogrammer.lostcities.asset.BlockRegistryView;
import com.tuxprogrammer.lostcities.asset.TestAssets;
import com.tuxprogrammer.lostcities.asset.block.BlockData;
import com.tuxprogrammer.lostcities.asset.block.MetalFlags;

/**
 * M11's exit gate in plain Java: every bundled part, under every style's palettes and the palette variety, generates
 * with no undefined character and no unregistered block, in the full pack and in vanilla alone. The
 * {@code assets.catalog} scenario runs the same check in the real pack against the live registry.
 */
public class AssetCatalogCheckTest {

    private static final BlockData DATA = TestAssets.blockData();
    private static final int SEEDS = 12;

    private static void check(BlockRegistryView registry, String label) {
        AssetSet set = TestAssets.bundled(registry);
        AssetCatalogCheck.Report report = AssetCatalogCheck.run(set, MetalFlags.ALL_ON, registry, SEEDS);
        assertEquals(Collections.emptyMap(), report.unregistered(), label + ": every written block is registered");
        assertEquals(Collections.emptyMap(), report.undefined(), label + ": every character is defined");
        assertTrue(
            report.writes() >= set.count(AssetType.PARTS) * set.count(AssetType.STYLES) * SEEDS,
            label + ": every part under every style: " + report.summary());
    }

    @Test
    public void everyPartGeneratesInThePack() {
        check(TestAssets.pack(DATA), "pack");
    }

    @Test
    public void everyPartGeneratesInVanillaAlone() {
        check(TestAssets.vanilla(), "vanilla");
    }

    @Test
    public void theVarietyReachesThePackDecorativeBlocks() {
        BlockRegistryView pack = TestAssets.pack(DATA);
        AssetSet set = TestAssets.bundled(pack);
        AssetCatalogCheck.Report report = AssetCatalogCheck.run(set, MetalFlags.ALL_ON, pack, SEEDS);
        long modded = report.distinctBlocks()
            .stream()
            .filter(name -> !name.startsWith("minecraft:"))
            .count();
        assertTrue(modded > 150, "pack blocks written: " + modded + " of " + report.summary());
    }
}
