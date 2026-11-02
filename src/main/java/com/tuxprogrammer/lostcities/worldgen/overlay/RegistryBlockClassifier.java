package com.tuxprogrammer.lostcities.worldgen.overlay;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockHugeMushroom;
import net.minecraft.block.BlockLeavesBase;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockVine;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.worldgen.terrain.BlockClassifier;

/**
 * The {@link BlockClassifier} of a running game, computed from the live registry by the rules of
 * docs/new-spec/08-loot-mobs-blocks/index.md ("Block tags", P08-1, D31):
 * <ul>
 * <li>liquid: material water or lava; air: material air;</li>
 * <li>foliage: instances of {@code BlockLog}, {@code BlockLeavesBase}, {@code BlockBush}, {@code BlockVine} and
 * {@code BlockHugeMushroom}, ladders, and the blocks of the ore names {@code logWood}, {@code treeLeaves} and
 * {@code treeSapling};</li>
 * <li>not breakable: negative hardness, plus the end portal and its frame;</li>
 * <li>easily broken: material glass;</li>
 * <li>light: a light value above 0.</li>
 * </ul>
 * Answers are cached per {@link BlockRef} instance; {@link BlockRefs} hands out one instance per block and metadata.
 *
 * <p>
 * Owned by one wrapped world. Server thread only. Side: server.
 */
public final class RegistryBlockClassifier implements BlockClassifier {

    private final BlockRefs refs;
    private final Map<BlockRef, Integer> cache = new IdentityHashMap<>();
    private final Set<Block> oreFoliage = new HashSet<>();

    /**
     * Creates the classifier.
     *
     * @param refs the world's block translations
     */
    public RegistryBlockClassifier(BlockRefs refs) {
        this.refs = refs;
        for (String ore : new String[] { "logWood", "treeLeaves", "treeSapling" }) {
            for (ItemStack stack : OreDictionary.getOres(ore)) {
                Block block = stack == null ? null : Block.getBlockFromItem(stack.getItem());
                if (block != null && block != Blocks.air) {
                    oreFoliage.add(block);
                }
            }
        }
    }

    @Override
    public int flags(BlockRef ref) {
        Integer cached = cache.get(ref);
        if (cached != null) {
            return cached;
        }
        int flags = classify(refs.block(ref), ref);
        cache.put(ref, flags);
        return flags;
    }

    private int classify(Block block, BlockRef ref) {
        if (block == Blocks.air || ref.isAir()) {
            return AIR;
        }
        Material material = block.getMaterial();
        int flags = 0;
        if (material == Material.air) {
            flags |= AIR;
        }
        if (material == Material.water || material == Material.lava) {
            flags |= LIQUID;
        }
        if (block instanceof BlockLog || block instanceof BlockLeavesBase
            || block instanceof BlockBush
            || block instanceof BlockVine
            || block instanceof BlockHugeMushroom
            || block == Blocks.ladder
            || oreFoliage.contains(block)) {
            flags |= FOLIAGE;
        }
        if (block == Blocks.end_portal || block == Blocks.end_portal_frame || hardness(block) < 0) {
            flags |= NOT_BREAKABLE;
        }
        if (block == Blocks.bedrock) {
            flags |= BEDROCK;
        }
        if (material == Material.glass) {
            flags |= EASY_BREAKABLE;
        }
        if (block.getLightValue() > 0) {
            flags |= LIGHT;
        }
        return flags;
    }

    private static float hardness(Block block) {
        try {
            // Vanilla returns the field and ignores the world; a mod that reads the world gets null and may throw.
            return block.getBlockHardness(null, 0, 0, 0);
        } catch (RuntimeException e) {
            LostCities.LOG.debug("Hardness of {} needs a world; treating it as breakable", block, e);
            return 0f;
        }
    }
}
