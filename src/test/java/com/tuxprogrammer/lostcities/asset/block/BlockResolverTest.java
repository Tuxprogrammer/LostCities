package com.tuxprogrammer.lostcities.asset.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetProblem;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.BlockRegistryView;
import com.tuxprogrammer.lostcities.asset.Problems;
import com.tuxprogrammer.lostcities.asset.TestAssets;

/** Block strings to blocks: the mapping, fallbacks, the never-place list and the metal flags. */
public class BlockResolverTest {

    private static final BlockData DATA = TestAssets.blockData();

    private static BlockResolver resolver(BlockRegistryView registry) {
        return new BlockResolver(DATA.mapping(), DATA.metals(), DATA.neverPlace(), registry);
    }

    private static BlockChoice resolve(BlockResolver resolver, String text, String asset, Character c,
        Problems problems) {
        return resolver.resolve(text, asset == null ? null : AssetId.parse(asset), c, problems.in("test"), "block");
    }

    @Test
    public void mappedNamesTakeTheirPrimaryInThePack() {
        Problems problems = new Problems();
        BlockResolver resolver = resolver(TestAssets.pack(DATA));
        assertEquals(
            BlockRef.parse("etfuturum:smooth_stone:0"),
            resolve(resolver, "minecraft:stone", null, null, problems).block());
        assertEquals(
            BlockRef.parse("Botania:stone2Stairs:6"),
            resolve(
                resolver,
                "minecraft:polished_diorite_stairs[facing=south,half=top,shape=straight]",
                null,
                null,
                problems).block());
        assertEquals(
            BlockRef.parse("etfuturum:double_red_sandstone_slab:0"),
            resolve(resolver, "minecraft:red_sandstone_slab[type=double]", null, null, problems).block());
        assertTrue(
            resolver.fallbacks()
                .isEmpty());
    }

    @Test
    public void absentModsTakeTheVanillaFallbackAndAreRecorded() {
        Problems problems = new Problems();
        BlockResolver resolver = resolver(TestAssets.vanilla());
        BlockChoice stone = resolve(resolver, "minecraft:stone", null, null, problems);
        assertEquals(BlockRef.parse("minecraft:double_stone_slab:8"), stone.block());
        assertTrue(stone.isFallback());
        BlockChoice moss = resolve(resolver, "minecraft:moss_block", null, null, problems);
        assertEquals(
            BlockRef.parse("minecraft:mossy_cobblestone:0"),
            moss.block(),
            "D31: moss falls back to mossy cobblestone");
        List<BlockResolver.Fallback> fallbacks = resolver.fallbacks();
        assertEquals(2, fallbacks.size());
        assertTrue(
            fallbacks.toString()
                .contains("chisel:moss is not registered, using minecraft:mossy_cobblestone"));
    }

    @Test
    public void theMossBlockIsChiselMossWithChisel() {
        Set<String> names = new HashSet<>(TestAssets.vanillaNames());
        names.add("chisel:moss");
        BlockChoice moss = resolve(
            resolver(BlockRegistryView.of(names)),
            "minecraft:moss_block",
            null,
            null,
            new Problems());
        assertEquals(BlockRef.parse("chisel:moss:0"), moss.block());
    }

    @Test
    public void directReferencesNeedTheirModAndTheirMetadata() {
        Problems problems = new Problems();
        BlockResolver resolver = resolver(TestAssets.vanilla());
        assertEquals(
            BlockRef.parse("minecraft:mossy_cobblestone:0"),
            resolve(resolver, "minecraft:mossy_cobblestone:0", null, null, problems).block());
        assertNull(resolve(resolver, "Botania:stone:6", null, null, problems), "an absent mod has no fallback");
        // Without metadata a vanilla name is read as a 1.20 name and mapped, never guessed.
        assertEquals(
            BlockRef.parse("minecraft:mossy_cobblestone:0"),
            resolve(resolver, "minecraft:mossy_cobblestone", null, null, problems).block());
    }

    @Test
    public void doesNotReproduceCORE20_unknownNamesAreReportedNeverAir() {
        Problems problems = new Problems();
        BlockChoice choice = resolve(resolver(TestAssets.vanilla()), "minecraft:nosuch_block", null, null, problems);
        assertNull(choice);
        List<AssetProblem> errors = problems.of(AssetProblem.Severity.ERROR);
        assertEquals(1, errors.size());
        assertTrue(
            errors.get(0)
                .toString()
                .contains("not in the block-name map"));
    }

    @Test
    public void doesNotReproduceASSET03_the112MetaFormIsRejected() {
        Problems problems = new Problems();
        assertNull(resolve(resolver(TestAssets.vanilla()), "minecraft:red_sandstone@2", null, null, problems));
        assertTrue(
            problems.all()
                .get(0)
                .toString()
                .contains("'@meta' form is not read"));
    }

    @Test
    public void structureVoidIsTheHardAirMarker() {
        assertSame(
            BlockChoice.HARD_AIR,
            resolve(resolver(TestAssets.vanilla()), "minecraft:structure_void", null, null, new Problems()));
    }

    @Test
    public void infestedBlocksBecomeTheirPlainBlocks() {
        assertEquals(
            BlockRef.parse("minecraft:stonebrick:0"),
            resolve(resolver(TestAssets.vanilla()), "minecraft:infested_stone_bricks", null, null, new Problems())
                .block());
    }

    @Test
    public void neverPlacedBlocksAreRejected() {
        Problems problems = new Problems();
        BlockResolver resolver = resolver(TestAssets.vanilla());
        assertNull(resolve(resolver, "minecraft:bedrock:0", null, null, problems));
        assertNull(resolve(resolver, "minecraft:stone:0", null, null, problems), "raw stone is a GT ore host");
        assertNull(resolve(resolver, "minecraft:monster_egg:2", null, null, problems));
        assertNull(resolve(resolver, "minecraft:diamond_block", null, null, problems), "valuable storage outside loot");
    }

    @Test
    public void treasureMayHoldValuablesAndIgnoresTheMetalFlags() {
        BlockChoice diamond = resolve(
            resolver(TestAssets.vanilla()),
            "minecraft:diamond_block",
            "common",
            '!',
            new Problems());
        assertEquals(BlockRef.parse("minecraft:diamond_block:0"), diamond.block());
        BlockChoice iron = resolve(
            resolver(TestAssets.vanilla()),
            "minecraft:iron_block",
            "common",
            '!',
            new Problems());
        assertNull(iron.metal());
        assertEquals(BlockRef.parse("minecraft:iron_block:0"), iron.block(MetalFlags.ALL_OFF));
    }

    @Test
    public void theOilRigsDiamondsAreANonValuableLookAlike() {
        BlockChoice pack = resolve(
            resolver(TestAssets.pack(DATA)),
            "minecraft:diamond_block",
            "oilrig",
            'M',
            new Problems());
        assertEquals(BlockRef.parse("Ztones:tile.iszmBlock:8"), pack.block());
        BlockChoice vanilla = resolve(
            resolver(TestAssets.vanilla()),
            "minecraft:diamond_block",
            "oilrig",
            'M',
            new Problems());
        assertEquals(BlockRef.parse("minecraft:stained_hardened_clay:3"), vanilla.block());
    }

    @Test
    public void theGasStationsIronKeepsUpstreamsLookWhileTheIronFlagIsOn() {
        BlockChoice iron = resolve(
            resolver(TestAssets.pack(DATA)),
            "minecraft:iron_block",
            "highway_gas_station",
            'i',
            new Problems());
        assertEquals(DecorativeMetal.IRON, iron.metal());
        assertTrue(iron.capExempt());
        assertEquals(BlockRef.parse("minecraft:iron_block:0"), iron.block(MetalFlags.ALL_ON));
        assertEquals(BlockRef.parse("chisel:factoryblock:12"), iron.block(MetalFlags.ALL_OFF));
        BlockChoice vanilla = resolve(
            resolver(TestAssets.vanilla()),
            "minecraft:iron_block",
            "highway_gas_station",
            'i',
            new Problems());
        assertEquals(BlockRef.parse("minecraft:double_stone_slab:8"), vanilla.block(MetalFlags.ALL_OFF));
    }

    @Test
    public void aDirectMetalBlockOutsideTreasureFollowsItsFlag() {
        Set<String> names = new HashSet<>(TestAssets.vanillaNames());
        names.add("chisel:steelblock");
        BlockChoice steel = resolve(
            resolver(BlockRegistryView.of(names)),
            "chisel:steelblock:3",
            "p",
            'x',
            new Problems());
        assertEquals(DecorativeMetal.STEEL, steel.metal());
        assertEquals(BlockRef.parse("chisel:steelblock:3"), steel.block(MetalFlags.ALL_ON));
        assertEquals(BlockRef.parse("minecraft:double_stone_slab:8"), steel.block(MetalFlags.ALL_OFF));
    }
}
