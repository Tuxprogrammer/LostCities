package com.tuxprogrammer.lostcities.worldgen.terrain;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import com.tuxprogrammer.lostcities.asset.BlockRef;

/**
 * A {@link BlockClassifier} from fixed lists of vanilla 1.7.10 block names. It is what unit and golden tests use, and
 * the classes match what the registry rules of docs/new-spec/08-loot-mobs-blocks give for vanilla blocks: liquids,
 * the {@code BlockLog}, {@code BlockLeavesBase}, {@code BlockBush}, {@code BlockVine} and {@code BlockHugeMushroom}
 * families, glass-material blocks, blocks of negative hardness and vanilla light sources.
 *
 * <p>
 * Answers are cached by {@link BlockRef} identity, so a buffer that hands out the same instances pays one hash
 * lookup per block. Not thread-safe; one per world.
 *
 * <p>
 * Side: both (plain Java).
 */
public final class NameBlockClassifier implements BlockClassifier {

    private static final Map<String, Integer> BY_NAME = Collections.unmodifiableMap(buildNames());

    /** Most cached instances before the cache is dropped. */
    private static final int CACHE_LIMIT = 4096;

    private final Map<BlockRef, Integer> cache = new IdentityHashMap<>();

    @Override
    public int flags(BlockRef block) {
        Integer cached = cache.get(block);
        if (cached != null) {
            return cached;
        }
        int flags = classify(block);
        if (cache.size() >= CACHE_LIMIT) {
            // Callers that make a new BlockRef per block would otherwise grow the cache without bound.
            cache.clear();
        }
        cache.put(block, flags);
        return flags;
    }

    /**
     * Returns the class bits of a block by its name alone, uncached.
     *
     * @param block the block
     * @return its class bits
     */
    public static int classify(BlockRef block) {
        if (block.isAir()) {
            return AIR;
        }
        Integer flags = BY_NAME.get(block.name());
        return flags == null ? 0 : flags;
    }

    private static Map<String, Integer> buildNames() {
        Map<String, Integer> out = new HashMap<>();
        put(out, LIQUID, "minecraft:water", "minecraft:flowing_water", "minecraft:lava", "minecraft:flowing_lava");
        put(
            out,
            FOLIAGE,
            "minecraft:log",
            "minecraft:log2",
            "minecraft:leaves",
            "minecraft:leaves2",
            "minecraft:sapling",
            "minecraft:tallgrass",
            "minecraft:deadbush",
            "minecraft:yellow_flower",
            "minecraft:red_flower",
            "minecraft:double_plant",
            "minecraft:brown_mushroom",
            "minecraft:red_mushroom",
            "minecraft:waterlily",
            "minecraft:wheat",
            "minecraft:carrots",
            "minecraft:potatoes",
            "minecraft:nether_wart",
            "minecraft:vine",
            "minecraft:ladder",
            "minecraft:brown_mushroom_block",
            "minecraft:red_mushroom_block");
        put(
            out,
            EASY_BREAKABLE,
            "minecraft:glass",
            "minecraft:stained_glass",
            "minecraft:glass_pane",
            "minecraft:stained_glass_pane",
            "minecraft:beacon");
        put(out, EASY_BREAKABLE | LIGHT, "minecraft:glowstone");
        put(out, NOT_BREAKABLE | BEDROCK, "minecraft:bedrock");
        put(out, NOT_BREAKABLE, "minecraft:end_portal", "minecraft:end_portal_frame", "minecraft:command_block");
        put(
            out,
            LIGHT,
            "minecraft:torch",
            "minecraft:lit_redstone_lamp",
            "minecraft:lit_pumpkin",
            "minecraft:lit_furnace",
            "minecraft:fire",
            "minecraft:redstone_torch",
            "minecraft:jack_o_lantern");
        // Lava is a liquid that also emits light.
        out.put("minecraft:lava", LIQUID | LIGHT);
        out.put("minecraft:flowing_lava", LIQUID | LIGHT);
        return out;
    }

    private static void put(Map<String, Integer> out, int flags, String... names) {
        Set<String> unique = new HashSet<>(Arrays.asList(names));
        for (String name : unique) {
            out.merge(name, flags, (a, b) -> a | b);
        }
    }
}
