package com.tuxprogrammer.lostcities.asset.loot;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * The EnhancedLootBags groups city loot may hold, with the tier each counts as: the pack's own tier labels, which the
 * tier map follows (docs/new-spec/00-pack-integration/loot-and-mobs.md § EnhancedLootBags; decision log D20). The
 * item is {@code enhancedlootbags:lootbag} and its damage is the group's ID.
 *
 * <p>
 * Only the tech, food, rail, farming and computer families are here. Groups above EV, the magic, Blood Magic and
 * Witchery families, space and Hardcore Ender Expansion are absent, so a category that names one fails validation.
 * Two groups of those families are left out on content, read from the pack's
 * {@code config/EnhancedLootBags/LootBags.xml}: {@code slow_food} (29) can roll Avaritia's Cosmic Meatballs and
 * Ultimate Stew, end-game food far above EV, and {@code transportation_1_class} (34) holds Railcraft's electric
 * locomotive and tunnel bore, whose pack recipes were not tiered here.
 *
 * <p>
 * Side: both (plain Java).
 */
public final class LootBagGroups {

    /** The loot bag item's registry name. Source: LootGroupsHandler.java#L308-L309 at EnhancedLootBags 1.3.5. */
    public static final String ITEM = "enhancedlootbags:lootbag";

    private static final Map<Integer, LootTier> ALLOWED = allowed();

    private LootBagGroups() {}

    /**
     * Returns the tier a loot bag group counts as.
     *
     * @param group the group ID (the bag's damage)
     * @return the tier, or {@code null} when city loot may not hold this group
     */
    public static LootTier tierOf(int group) {
        return ALLOWED.get(group);
    }

    /** Returns every allowed group and its tier, by group ID. */
    public static Map<Integer, LootTier> all() {
        return ALLOWED;
    }

    private static Map<Integer, LootTier> allowed() {
        Map<Integer, LootTier> out = new TreeMap<>();
        // Tech: the pack's tier labels.
        out.put(1, LootTier.STONE); // basic_stone_age
        out.put(2, LootTier.STEAM); // steam_age
        out.put(4, LootTier.LV); // tier_1_lv
        out.put(5, LootTier.MV); // tier_2_mv
        out.put(6, LootTier.HV); // tier_3_hv
        out.put(7, LootTier.EV); // tier_4_ev
        // Food: fast food only (see the class comment for 29).
        out.put(28, LootTier.STONE); // fast_food
        // Rail: Railcraft carts, rails, ties and signals.
        out.put(32, LootTier.STEAM); // transportation_3_class
        out.put(33, LootTier.LV); // transportation_2_class
        // Farming.
        out.put(35, LootTier.STONE); // seeds
        out.put(36, LootTier.STONE); // gardens
        out.put(51, LootTier.STONE); // crops_farmhand
        // Computers: OpenComputers' first tier.
        out.put(45, LootTier.LV); // computer_floppies
        out.put(46, LootTier.LV); // tier_1_computers
        return Collections.unmodifiableMap(out);
    }
}
