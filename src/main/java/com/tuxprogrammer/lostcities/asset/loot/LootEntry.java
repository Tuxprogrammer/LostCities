package com.tuxprogrammer.lostcities.asset.loot;

import java.util.Collections;
import java.util.List;

/**
 * One weighted stack of a loot category's defaults, as {@code data/lostcitiesnh/loot/categories.json} writes it: what
 * the item is (a registry name, ore-dictionary names, a GregTech {@code ItemList} constant or a loot bag group), its
 * damage, the stack size range, the weight and the tier it counts as. The world glue resolves it once at
 * {@code postInit}; an entry whose mod is absent is skipped.
 *
 * <p>
 * Immutable. Side: both (plain Java).
 */
public final class LootEntry {

    /** How an entry names its item. */
    public enum Source {
        /** A registry name, {@code modid:name}, with {@link #meta()}. */
        ITEM,
        /** Ore-dictionary names, tried in order; GregTech's stack is preferred where several mods register one. */
        ORE,
        /** A GregTech {@code gregtech.api.enums.ItemList} constant, by name. */
        GT,
        /** An EnhancedLootBags group: {@link LootBagGroups#ITEM} with the group as damage. */
        LOOTBAG
    }

    private final Source source;
    private final List<String> names;
    private final int meta;
    private final int min;
    private final int max;
    private final int weight;
    private final LootTier tier;

    /**
     * Creates an entry.
     *
     * @param source how the item is named
     * @param names  the registry name, the ore names in order, the {@code ItemList} constant, or the group ID as text
     * @param meta   the item damage for {@link Source#ITEM}; the group for {@link Source#LOOTBAG}; otherwise 0
     * @param min    the smallest stack, at least 1
     * @param max    the largest stack, at least {@code min}
     * @param weight the weight among the category's entries, at least 1
     * @param tier   the tier the item counts as
     */
    public LootEntry(Source source, List<String> names, int meta, int min, int max, int weight, LootTier tier) {
        this.source = source;
        this.names = Collections.unmodifiableList(names);
        this.meta = meta;
        this.min = min;
        this.max = max;
        this.weight = weight;
        this.tier = tier;
    }

    /** Returns how the item is named. */
    public Source source() {
        return source;
    }

    /** Returns the names: one for every source but {@link Source#ORE}, which tries each in order. */
    public List<String> names() {
        return names;
    }

    /** Returns the item damage (the group for a loot bag). */
    public int meta() {
        return meta;
    }

    /** Returns the smallest stack. */
    public int min() {
        return min;
    }

    /** Returns the largest stack. */
    public int max() {
        return max;
    }

    /** Returns the weight. */
    public int weight() {
        return weight;
    }

    /** Returns the tier the item counts as. */
    public LootTier tier() {
        return tier;
    }

    /** Returns whether the entry always resolves: a vanilla {@code minecraft:} item. */
    public boolean isVanilla() {
        return source == Source.ITEM && names.get(0)
            .startsWith("minecraft:");
    }

    @Override
    public String toString() {
        return source.name()
            .toLowerCase(java.util.Locale.ROOT) + " " + String.join("|", names) + (meta != 0 ? "@" + meta : "");
    }
}
