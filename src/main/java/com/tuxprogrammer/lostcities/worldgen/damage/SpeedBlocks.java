package com.tuxprogrammer.lostcities.worldgen.damage;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.tuxprogrammer.lostcities.asset.BlockRef;

/**
 * Speed blocks and the normal block each becomes under damage (decision log D28 rule 3): a speed block may stand in
 * a wall or trim, but ruins and explosion damage turn walls into walking surfaces, so damage swaps it for a normal
 * block of the same look, and rubble never holds one (docs/new-spec/03-terrain/index.md, "Damage on speed blocks";
 * docs/new-spec/05-assets/block-catalog/index.md, rule 2 and the SPD tag).
 *
 * <p>
 * The speed blocks are the catalog's SPD set: Chisel {@code concrete}, {@code road}, {@code road2} and their slabs;
 * every GT concrete meta ({@code gregtech:gt.blockconcretes}); every Tinkers' brownstone block and slab. Thaumcraft's
 * travel paving is never placed, so it needs no swap. Each maps to a vanilla look-alike, so the swap works without the
 * pack mod's other blocks: gray concrete to stone or smooth stone, dark GT concrete to brown terracotta, light GT
 * concrete and brownstone to sandstone, roads to cobblestone, slabs to the vanilla slab of the same color with the
 * top bit kept. The table lives here until M02's asset model carries a speed flag per palette entry, which can then
 * name a pool-specific partner.
 *
 * <p>
 * Side: both (plain Java).
 */
public final class SpeedBlocks {

    private static final Map<String, BlockRef[]> SWAPS = new HashMap<>();

    static {
        // Source name, target name, and the target meta for each source meta.
        register("chisel:concrete", "minecraft:stone", constant(0));
        register("chisel:road", "minecraft:cobblestone", constant(0));
        register("chisel:road2", "minecraft:cobblestone", constant(0));
        register("chisel:concrete_slab", "minecraft:stone_slab", slab(0));
        // GT concrete: metas 0..7 dark (brown), 8..15 light (beige).
        int[] gtConcrete = new int[16];
        for (int meta = 0; meta < 16; meta++) {
            gtConcrete[meta] = meta < 8 ? 12 : 4;
        }
        register("gregtech:gt.blockconcretes", "minecraft:stained_hardened_clay", gtConcrete);
        // Brownstone 0 rough, 1 rough road, 2 smooth, 3 brick, 4 road, 5 fancy, 6 chiseled.
        register(
            "TConstruct:SpeedBlock",
            "minecraft:sandstone",
            new int[] { 0, 0, 2, 2, 2, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 });
        register("TConstruct:SpeedSlab", "minecraft:stone_slab", slab(1));
    }

    private SpeedBlocks() {}

    /**
     * Returns the normal block a speed block becomes under damage.
     *
     * @param block any block
     * @return its normal partner, or {@code null} when it is not a speed block
     */
    public static BlockRef normalFor(BlockRef block) {
        BlockRef[] targets = SWAPS.get(block.name());
        return targets == null ? null : targets[block.meta()];
    }

    /**
     * Returns the block with any speed block swapped for its normal partner.
     *
     * @param block any block
     * @return {@code block}, or its normal partner
     */
    public static BlockRef withoutSpeed(BlockRef block) {
        BlockRef normal = normalFor(block);
        return normal == null ? block : normal;
    }

    /** Returns whether a block is a speed block. */
    public static boolean isSpeedBlock(BlockRef block) {
        return SWAPS.containsKey(block.name());
    }

    private static void register(String name, String target, int[] metas) {
        BlockRef[] targets = new BlockRef[16];
        for (int meta = 0; meta < 16; meta++) {
            targets[meta] = BlockRef.of(target, metas[meta]);
        }
        SWAPS.put(name, targets);
    }

    private static int[] constant(int meta) {
        int[] out = new int[16];
        Arrays.fill(out, meta);
        return out;
    }

    /** A slab: the target's slab type, with the source's top-half bit (8) kept. */
    private static int[] slab(int type) {
        int[] out = new int[16];
        for (int meta = 0; meta < 16; meta++) {
            out[meta] = type | (meta & 8);
        }
        return out;
    }
}
