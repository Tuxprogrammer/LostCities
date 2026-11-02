package com.tuxprogrammer.lostcities.worldgen.overlay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraftforge.common.ChestGenHooks;
import net.minecraftforge.oredict.OreDictionary;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.asset.loot.LootDefaults;
import com.tuxprogrammer.lostcities.asset.loot.LootEntry;
import com.tuxprogrammer.lostcities.asset.loot.LootTier;
import com.tuxprogrammer.lostcities.compat.gregtech.GtItemList;

/**
 * Registers this mod's loot categories with Forge's {@code ChestGenHooks} and fills them with their defaults from
 * {@link LootDefaults#bundled()}: the tier bands {@code city_supplies} (Stone and Steam) to {@code city_ev}, and
 * {@code rail_dungeon} (docs/new-spec/00-pack-integration/loot-and-mobs.md § Loot design; decision log D20). Each
 * entry resolves once against the live registries: a registry name, an ore-dictionary name (GregTech's stack
 * preferred), a GregTech {@code ItemList} constant, or an EnhancedLootBags group. An entry whose mod is absent is
 * skipped with one log line; every category keeps its vanilla entries, so none is ever empty.
 *
 * <p>
 * Every stack registered is recorded with the tier its entry names: the tier map the dev self-check and the
 * {@code gtnh.loot} scenario read to prove nothing above EV is in a city chest (docs/design/0004 §3). TooMuchLoot may
 * replace a category's contents at {@code FMLServerStartingEvent}; that is the server owner's choice
 * (docs/PACK-CHANGES.md) and is not checked against this map.
 *
 * <p>
 * Registered in {@code postInit}, after every mod's items and ore names exist, whatever the config says: a category
 * is registration, which config never changes (CONTRIBUTING § Architecture). The tier map is written once there and
 * only read afterwards. Side: both (registered on both, read where chests are filled).
 */
public final class CityLoot {

    private static final Map<String, LootTier> TIERS = new TreeMap<>();
    private static final List<String> SKIPPED = new ArrayList<>();
    private static LootDefaults defaults;

    private CityLoot() {}

    /** Registers every category with its resolved defaults. Call once, in {@code postInit}. */
    public static synchronized void register() {
        defaults = LootDefaults.bundled();
        for (String problem : defaults.problems()) {
            LostCities.LOG.error("Loot defaults: {}", problem);
        }
        TIERS.clear();
        SKIPPED.clear();
        int registered = 0;
        for (LootDefaults.Category category : defaults.categories()
            .values()) {
            ChestGenHooks info = ChestGenHooks.getInfo(category.name());
            info.setMin(category.minStacks());
            info.setMax(category.maxStacks());
            for (LootEntry entry : category.entries()) {
                ItemStack stack = resolve(entry);
                if (stack == null) {
                    SKIPPED.add(category.name() + " " + entry);
                    LostCities.LOG.info("Loot {}: {} is not in this pack; skipped", category.name(), entry);
                    continue;
                }
                info.addItem(new WeightedRandomChestContent(stack, entry.min(), entry.max(), entry.weight()));
                record(stack, entry.tier());
                registered++;
            }
        }
        LostCities.LOG.info(
            "Loot: {} entries in {} categories, {} skipped",
            registered,
            defaults.categories()
                .size(),
            SKIPPED.size());
    }

    /**
     * Returns the tier map's key of a stack: {@code modid:name@damage}.
     *
     * @param stack the stack
     * @return the key, or {@code null} for an empty stack or an unregistered item
     */
    public static String key(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return null;
        }
        Object name = Item.itemRegistry.getNameForObject(stack.getItem());
        return name == null ? null : name + "@" + stack.getItemDamage();
    }

    /**
     * Returns the tier a stack counts as in this mod's loot, from the entry that registered it.
     *
     * @param stack the stack
     * @return the tier, or {@code null} when no entry of this mod's categories registered it
     */
    public static synchronized LootTier tierOf(ItemStack stack) {
        String key = key(stack);
        return key == null ? null : TIERS.get(key);
    }

    /** Returns the tier map: every registered stack's key and tier, sorted. */
    public static synchronized Map<String, LootTier> tiers() {
        return Collections.unmodifiableMap(new TreeMap<>(TIERS));
    }

    /** Returns the entries skipped because their mod or item is absent, one line each. */
    public static synchronized List<String> skipped() {
        return Collections.unmodifiableList(new ArrayList<>(SKIPPED));
    }

    /** Returns the defaults read at registration, or {@code null} before it. */
    public static synchronized LootDefaults defaults() {
        return defaults;
    }

    private static void record(ItemStack stack, LootTier tier) {
        String key = key(stack);
        LootTier known = TIERS.get(key);
        // A stack two entries name counts as the higher of their tiers, so the check is never weaker than an entry.
        if (key != null && (known == null || tier.compareTo(known) > 0)) {
            TIERS.put(key, tier);
        }
    }

    private static ItemStack resolve(LootEntry entry) {
        switch (entry.source()) {
            case ITEM:
            case LOOTBAG:
                return named(
                    entry.names()
                        .get(0),
                    entry.meta());
            case ORE:
                for (String name : entry.names()) {
                    ItemStack stack = ore(name);
                    if (stack != null) {
                        return stack;
                    }
                }
                return null;
            case GT:
                return GtItemList.get(
                    entry.names()
                        .get(0));
            default:
                return null;
        }
    }

    private static ItemStack named(String name, int meta) {
        Object item = Item.itemRegistry.getObject(name);
        return item instanceof Item ? new ItemStack((Item) item, 1, meta) : null;
    }

    /**
     * Returns the stack of an ore name: GregTech's where GT registers one (its unified material), otherwise the first
     * registered, never a wildcard-damage stack.
     */
    private static ItemStack ore(String name) {
        if (!OreDictionary.doesOreNameExist(name)) {
            return null;
        }
        ItemStack first = null;
        for (ItemStack stack : OreDictionary.getOres(name)) {
            if (stack == null || stack.getItem() == null || stack.getItemDamage() == OreDictionary.WILDCARD_VALUE) {
                continue;
            }
            Object registry = Item.itemRegistry.getNameForObject(stack.getItem());
            if (registry != null && registry.toString()
                .startsWith(GtItemList.MOD_ID + ":")) {
                return copyOne(stack);
            }
            if (first == null) {
                first = stack;
            }
        }
        return first == null ? null : copyOne(first);
    }

    private static ItemStack copyOne(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.stackSize = 1;
        return copy;
    }
}
