package com.tuxprogrammer.lostcities.compat.villagenames;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Which blocks show that a VillageNames building or ocean monument was built in a column: blocks RWG's terrain and
 * decoration never place on the surface (crafted wood, glass, torches, doors, beds, farmland, wool, stone bricks, the
 * workstations VillageNames and Et Futurum add), and the monument's prismarine. Natural blocks that villages also use,
 * such as logs, cobblestone (RWG's boulders), sandstone and clay, are left out, so a hit is evidence of the structure.
 * Used only by the {@code oracle.villagenames} scenario, by registry name.
 *
 * <p>
 * Side: both (plain Java).
 */
final class StructureBlocks {

    /** Vanilla blocks only a structure places on RWG's surface. */
    private static final Set<String> VILLAGE_BLOCKS = Collections.unmodifiableSet(
        new HashSet<>(
            Arrays.asList(
                "minecraft:planks",
                "minecraft:oak_stairs",
                "minecraft:spruce_stairs",
                "minecraft:birch_stairs",
                "minecraft:jungle_stairs",
                "minecraft:acacia_stairs",
                "minecraft:dark_oak_stairs",
                "minecraft:stone_stairs",
                "minecraft:stone_brick_stairs",
                "minecraft:sandstone_stairs",
                "minecraft:brick_stairs",
                "minecraft:wooden_slab",
                "minecraft:double_wooden_slab",
                "minecraft:stone_slab",
                "minecraft:double_stone_slab",
                "minecraft:fence",
                "minecraft:fence_gate",
                "minecraft:cobblestone_wall",
                "minecraft:glass",
                "minecraft:glass_pane",
                "minecraft:stained_glass",
                "minecraft:stained_glass_pane",
                "minecraft:torch",
                "minecraft:wooden_door",
                "minecraft:iron_door",
                "minecraft:trapdoor",
                "minecraft:bed",
                "minecraft:crafting_table",
                "minecraft:furnace",
                "minecraft:lit_furnace",
                "minecraft:bookshelf",
                "minecraft:chest",
                "minecraft:hay_block",
                "minecraft:farmland",
                "minecraft:wheat",
                "minecraft:carrots",
                "minecraft:potatoes",
                "minecraft:wool",
                "minecraft:carpet",
                "minecraft:ladder",
                "minecraft:standing_sign",
                "minecraft:wall_sign",
                "minecraft:stonebrick",
                "minecraft:brick_block",
                "minecraft:flower_pot",
                "minecraft:cauldron",
                "minecraft:anvil",
                "minecraft:brewing_stand",
                "minecraft:iron_bars",
                "minecraft:lever",
                "minecraft:wooden_pressure_plate",
                "minecraft:stone_pressure_plate",
                "minecraft:wooden_button",
                "minecraft:stone_button")));

    /** Words in the resource path of the workstations and fittings VillageNames places from Et Futurum and itself. */
    private static final String[] VILLAGE_WORDS = { "lantern", "barrel", "campfire", "banner", "smoker",
        "blast_furnace", "composter", "lectern", "grindstone", "stonecutter", "loom", "cartography", "fletching",
        "smithing", "bell" };

    /**
     * Words in the resource path of any mod's building blocks that no terrain generator places: VillageNames swaps
     * mod blocks into its buildings (Witchery's snow stairs in snowy villages, for example).
     */
    private static final String[] BUILDING_WORDS = { "stairs", "slab", "fence", "door", "pane", "torch", "ladder",
        "lantern", "sign", "carpet", "wool", "planks" };

    /** Words in the resource path of a monument's blocks. */
    private static final String[] MONUMENT_WORDS = { "prismarine", "sea_lantern", "sponge" };

    private StructureBlocks() {}

    /**
     * Returns whether a block is evidence of a village building.
     *
     * @param registryName the block's {@code modid:name}, or {@code null}
     * @return whether only a structure places it
     */
    static boolean isVillageBlock(String registryName) {
        if (registryName == null) {
            return false;
        }
        if (VILLAGE_BLOCKS.contains(registryName)) {
            return true;
        }
        int colon = registryName.indexOf(':');
        String mod = colon < 0 ? "" : registryName.substring(0, colon);
        if ("VillageNames".equals(mod)) {
            return true;
        }
        String path = registryName.substring(colon + 1)
            .toLowerCase(Locale.ROOT);
        for (String word : BUILDING_WORDS) {
            if (path.contains(word)) {
                return true;
            }
        }
        if (!"etfuturum".equals(mod)) {
            return false;
        }
        for (String word : VILLAGE_WORDS) {
            if (path.contains(word)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether a block is a monument's.
     *
     * @param registryName the block's {@code modid:name}, or {@code null}
     * @return whether it is prismarine, a sea lantern or a sponge
     */
    static boolean isMonumentBlock(String registryName) {
        if (registryName == null) {
            return false;
        }
        String lower = registryName.toLowerCase(Locale.ROOT);
        for (String word : MONUMENT_WORDS) {
            if (lower.contains(word)) {
                return true;
            }
        }
        return false;
    }
}
