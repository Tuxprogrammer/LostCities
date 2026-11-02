package com.tuxprogrammer.lostcities.worldgen.overlay;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import com.tuxprogrammer.lostcities.asset.BlockRef;

/**
 * A block's ore-dictionary names, for the {@code ore:} entries of stuff matchers (docs/new-spec/05-assets,
 * "Matchers"): the names Forge's {@link OreDictionary} gives the block's item stack with the block's metadata. Blocks
 * with no item have none. Answers are kept per block reference for the world's life; the set of blocks a city writes
 * is small and fixed by the assets.
 *
 * <p>
 * Server thread only. Side: server.
 */
final class OreNames implements Function<BlockRef, Set<String>> {

    private final BlockRefs refs;
    private final Map<BlockRef, Set<String>> known = new HashMap<>();

    OreNames(BlockRefs refs) {
        this.refs = refs;
    }

    @Override
    public Set<String> apply(BlockRef ref) {
        Set<String> cached = known.get(ref);
        if (cached != null) {
            return cached;
        }
        Set<String> names = lookUp(ref);
        known.put(ref, names);
        return names;
    }

    private Set<String> lookUp(BlockRef ref) {
        Block block = refs.block(ref);
        if (block == null || block == Blocks.air) {
            return Collections.emptySet();
        }
        Item item = Item.getItemFromBlock(block);
        if (item == null) {
            return Collections.emptySet();
        }
        Set<String> names = new TreeSet<>();
        for (int id : OreDictionary.getOreIDs(new ItemStack(item, 1, ref.meta()))) {
            names.add(OreDictionary.getOreName(id));
        }
        return names.isEmpty() ? Collections.<String>emptySet() : Collections.unmodifiableSet(names);
    }
}
