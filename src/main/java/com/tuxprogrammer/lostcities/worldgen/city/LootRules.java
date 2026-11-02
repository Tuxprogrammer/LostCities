package com.tuxprogrammer.lostcities.worldgen.city;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Turns upstream's loot and mob values into this mod's (docs/new-spec/00-pack-integration/loot-and-mobs.md, Q5 and
 * Q6; decision log D20): a chest's {@code chestloot} value becomes one of this mod's tier-band {@code ChestGenHooks}
 * categories, or a 1.7.10 vanilla category under the profile's opt-in, and a spawner's 1.20 entity id becomes the
 * 1.7.10 entity name a vanilla spawner takes.
 *
 * <p>
 * The bundled {@code chestloot} condition stays upstream's file byte for byte; its floor and part tests choose the
 * upstream value, and this class converts the value (docs/new-spec/08-loot-mobs-blocks/index.md, {@code chestloot}):
 * <ul>
 * <li>{@code simple_dungeon} (factor 30, every floor) is ordinary loot: {@link #CITY_SUPPLIES} or {@link #CITY_LV}, 2
 * to 1; with the vanilla opt-in it is the pack's {@code dungeonChest}, as upstream's vanilla table;</li>
 * <li>{@code lostcitychest} (factor 8, floors 4 and up and the third cellar down) is the valuable table:
 * {@link #CITY_MV}, {@link #CITY_HV} or {@link #CITY_EV}, 10 to 5 to 1, so EV is the rarest band (0004 §3);</li>
 * <li>{@code raildungeonchest} is {@link #RAIL_DUNGEON};</li>
 * <li>the other vanilla tables become their 1.7.10 category only with the opt-in, and tables 1.7.10 has no category
 * for are dropped (LOOT-06): both are taken out <em>before</em> the condition's weighted draw ({@link #allowed}), so
 * they never leave a chest empty;</li>
 * <li>a value already naming one of this mod's categories (an asset override's) is kept.</li>
 * </ul>
 * The band draw is the chest's own random, after the condition's draw, so it never moves another chest's (LOOT-05).
 *
 * <p>
 * Pure. Side: both (plain Java).
 */
public final class LootRules {

    /** Stone and Steam: supplies, food, building blocks, bronze and iron (loot-and-mobs.md § Categories). */
    public static final String CITY_SUPPLIES = "lostcitiesnh:city_supplies";
    /** LV components, basic circuits, steel. */
    public static final String CITY_LV = "lostcitiesnh:city_lv";
    /** MV components, good circuits, aluminium. */
    public static final String CITY_MV = "lostcitiesnh:city_mv";
    /** HV components, advanced circuits, stainless steel. */
    public static final String CITY_HV = "lostcitiesnh:city_hv";
    /** EV components, EV circuits, titanium; the rarest band. */
    public static final String CITY_EV = "lostcitiesnh:city_ev";
    /** Rail dungeons: Railcraft rails, ties, carts and tools, transportation loot bags (Steam to LV). */
    public static final String RAIL_DUNGEON = "lostcitiesnh:rail_dungeon";

    /** Every category this mod registers, lowest band first. */
    public static final List<String> CATEGORIES = Collections
        .unmodifiableList(Arrays.asList(CITY_SUPPLIES, CITY_LV, CITY_MV, CITY_HV, CITY_EV, RAIL_DUNGEON));

    /** Upstream's ordinary table: every floor, factor 30. */
    static final String SIMPLE_DUNGEON = "minecraft:chests/simple_dungeon";
    /** Upstream's valuable table: floors 4 and up, and the third cellar down. */
    static final String LOST_CITY_CHEST = "lostcities:chests/lostcitychest";
    /** Upstream's rail dungeon table. */
    static final String RAIL_DUNGEON_CHEST = "lostcities:chests/raildungeonchest";

    /** {@code simple_dungeon}'s split: supplies twice as often as LV. */
    static final int[] ORDINARY_WEIGHTS = { 2, 1 };
    /** {@code lostcitychest}'s split: MV 10, HV 5, EV 1. */
    static final int[] VALUABLE_WEIGHTS = { 10, 5, 1 };

    private static final String[] ORDINARY = { CITY_SUPPLIES, CITY_LV };
    private static final String[] VALUABLE = { CITY_MV, CITY_HV, CITY_EV };

    /** Upstream's vanilla 1.20 tables with a 1.7.10 {@code ChestGenHooks} category (loot-and-mobs.md). */
    private static final Map<String, String> VANILLA = vanilla();

    /** 1.20 entity ids to 1.7.10 entity names where the name is not the id's path in camel case. */
    private static final Map<String, String> MOBS = mobs();

    /**
     * EnderZoo's overworld mobs: the optional {@code hardmobs} set (profile option {@code enderZooHardMobs}, off by
     * default; loot-and-mobs.md § Mob design). Names as EnderZoo 1.3.6 registers them with
     * {@code EntityRegistry.registerGlobalEntityID}. Source:
     * https://github.com/GTNewHorizons/EnderZoo/blob/1.3.6/src/main/java/crazypants/enderzoo/EnderZoo.java#L115-L127
     * and each entity's {@code NAME}, e.g.
     * https://github.com/GTNewHorizons/EnderZoo/blob/1.3.6/src/main/java/crazypants/enderzoo/entity/EntityFallenKnight.java#L39
     */
    public static final List<String> ENDERZOO_HARDMOBS = Collections.unmodifiableList(
        Arrays.asList(
            "enderzoo.FallenKnight",
            "enderzoo.ConcussionCreeper",
            "enderzoo.Enderminy",
            "enderzoo.WitherWitch",
            "enderzoo.DireWolf"));

    /** The entity a spawner takes when the named one's mod is absent (loot-and-mobs.md § Mob design). */
    public static final String FALLBACK_MOB = "Skeleton";

    private LootRules() {}

    /**
     * Returns whether a condition value may be drawn at all: values naming a vanilla table enter the draw only with
     * the opt-in, and values with no 1.7.10 counterpart never do.
     *
     * @param value        the condition value
     * @param vanillaOptIn the profile's {@code vanillaLootCategories}
     * @return whether the value takes part in the weighted draw
     */
    public static boolean allowed(String value, boolean vanillaOptIn) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        if (value.equals(SIMPLE_DUNGEON) || isOwn(value) || isUpstreamOwn(value)) {
            return true;
        }
        return vanillaOptIn && VANILLA.containsKey(value);
    }

    /**
     * Returns the category a drawn condition value fills from.
     *
     * @param value        the value the condition picked; {@code null} for none
     * @param vanillaOptIn the profile's {@code vanillaLootCategories}
     * @param random       the chest's own random, for the band split
     * @return the category, or the empty string for an empty chest
     */
    public static String category(String value, boolean vanillaOptIn, Random random) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (isOwn(value)) {
            return value;
        }
        if (value.equals(RAIL_DUNGEON_CHEST)) {
            return RAIL_DUNGEON;
        }
        if (value.equals(LOST_CITY_CHEST)) {
            return VALUABLE[pick(random, VALUABLE_WEIGHTS)];
        }
        if (value.equals(SIMPLE_DUNGEON)) {
            return vanillaOptIn ? VANILLA.get(SIMPLE_DUNGEON) : ORDINARY[pick(random, ORDINARY_WEIGHTS)];
        }
        if (vanillaOptIn) {
            String vanilla = VANILLA.get(value);
            return vanilla == null ? "" : vanilla;
        }
        return "";
    }

    /**
     * Returns the 1.7.10 entity name of an upstream mob id.
     *
     * @param id the id the mob condition picked, such as {@code minecraft:zombie}; {@code null} for none
     * @return the entity name, such as {@code Zombie}, or the empty string
     */
    public static String entityName(String id) {
        if (id == null || id.isEmpty()) {
            return "";
        }
        if (id.startsWith("enderzoo.")) {
            return id;
        }
        String path = id.substring(id.indexOf(':') + 1)
            .toLowerCase(Locale.ROOT);
        String mapped = MOBS.get(path);
        if (mapped != null) {
            return mapped;
        }
        StringBuilder out = new StringBuilder();
        for (String word : path.split("_")) {
            if (!word.isEmpty()) {
                out.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1));
            }
        }
        return out.toString();
    }

    private static boolean isOwn(String value) {
        return CATEGORIES.contains(value);
    }

    private static boolean isUpstreamOwn(String value) {
        return value.equals(LOST_CITY_CHEST) || value.equals(RAIL_DUNGEON_CHEST);
    }

    private static int pick(Random random, int[] weights) {
        int total = 0;
        for (int weight : weights) {
            total += weight;
        }
        int roll = random.nextInt(total);
        for (int i = 0; i < weights.length; i++) {
            roll -= weights[i];
            if (roll < 0) {
                return i;
            }
        }
        return weights.length - 1;
    }

    private static Map<String, String> vanilla() {
        Map<String, String> out = new HashMap<>();
        // loot-and-mobs.md § Upstream's vanilla-table entries; jungle_temple_dispenser is dropped (LOOT-06, P08-2).
        out.put(SIMPLE_DUNGEON, "dungeonChest");
        out.put("minecraft:chests/desert_pyramid", "pyramidDesertyChest");
        out.put("minecraft:chests/jungle_temple", "pyramidJungleChest");
        out.put("minecraft:chests/stronghold_corridor", "strongholdCorridor");
        out.put("minecraft:chests/stronghold_crossing", "strongholdCrossing");
        out.put("minecraft:chests/stronghold_library", "strongholdLibrary");
        out.put("minecraft:chests/spawn_bonus_chest", "bonusChest");
        out.put("minecraft:chests/village/village_armorer", "villageBlacksmith");
        out.put("minecraft:chests/village/village_toolsmith", "villageBlacksmith");
        out.put("minecraft:chests/village/village_weaponsmith", "villageBlacksmith");
        return Collections.unmodifiableMap(out);
    }

    private static Map<String, String> mobs() {
        Map<String, String> out = new HashMap<>();
        out.put("zombified_piglin", "PigZombie");
        out.put("magma_cube", "LavaSlime");
        out.put("iron_golem", "VillagerGolem");
        out.put("mooshroom", "MushroomCow");
        out.put("snow_golem", "SnowMan");
        out.put("ocelot", "Ozelot");
        return Collections.unmodifiableMap(out);
    }
}
