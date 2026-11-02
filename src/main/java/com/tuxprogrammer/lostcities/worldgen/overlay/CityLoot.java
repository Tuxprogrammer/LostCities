package com.tuxprogrammer.lostcities.worldgen.overlay;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraftforge.common.ChestGenHooks;

import com.tuxprogrammer.lostcities.worldgen.city.LootRules;

/**
 * The alpha's chest loot (decision log, M05J rows): two {@code ChestGenHooks} categories of this mod, filled with a
 * minimal set mapped from upstream's two tables to vanilla items far below EV (docs/design/0004, the tier ceiling): upstream's
 * gold and iron ingots stay, its diamonds, emeralds and diamond tools give way to iron ones and to supplies (coal,
 * torches, bread), and the rail dungeon's table adds rails and a minecart. M08 replaces both with the tier bands of
 * docs/new-spec/00-pack-integration/loot-and-mobs.md (Q5); server owners can retune both with TooMuchLoot XML.
 *
 * <p>
 * Registered in {@code init} whatever the config says: a category is registration, which config never changes
 * (CONTRIBUTING § Architecture). Side: server (registered on both, read where chests are filled).
 */
public final class CityLoot {

    /** Fewest stacks in a chest (upstream's tables roll 2 to 4). */
    static final int MIN_STACKS = 2;
    /** Most stacks in a chest. */
    static final int MAX_STACKS = 4;

    private CityLoot() {}

    /** Registers both categories with their items. Call once, in {@code init}. */
    public static void register() {
        ChestGenHooks city = ChestGenHooks.getInfo(LootRules.CITY);
        city.setMin(MIN_STACKS);
        city.setMax(MAX_STACKS);
        add(city, Items.gold_ingot, 0, 1, 3, 5);
        add(city, Items.iron_ingot, 0, 1, 4, 5);
        add(city, Items.coal, 0, 2, 8, 5);
        add(city, Items.bread, 0, 1, 4, 5);
        add(city, Item.getItemFromBlock(net.minecraft.init.Blocks.torch), 0, 4, 16, 5);
        add(city, Items.paper, 0, 2, 6, 3);
        add(city, Items.string, 0, 1, 4, 3);
        add(city, Items.iron_sword, 0, 1, 1, 2);
        ChestGenHooks rail = ChestGenHooks.getInfo(LootRules.RAIL_DUNGEON);
        rail.setMin(MIN_STACKS);
        rail.setMax(MAX_STACKS);
        add(rail, Items.gold_ingot, 0, 1, 3, 5);
        add(rail, Items.iron_ingot, 0, 2, 6, 5);
        add(rail, Item.getItemFromBlock(net.minecraft.init.Blocks.rail), 0, 4, 16, 5);
        add(rail, Items.coal, 0, 2, 8, 5);
        add(rail, Items.minecart, 0, 1, 1, 2);
        add(rail, Items.iron_axe, 0, 1, 1, 2);
    }

    private static void add(ChestGenHooks category, Item item, int damage, int min, int max, int weight) {
        category.addItem(new WeightedRandomChestContent(new ItemStack(item, 1, damage), min, max, weight));
    }
}
