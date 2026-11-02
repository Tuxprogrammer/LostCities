package com.tuxprogrammer.lostcities.worldgen.city;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The alpha's mapping from upstream's loot and mob values to this mod's (decision log, M05J rows): a chest's
 * {@code chestloot} value becomes one of two {@code ChestGenHooks} categories this mod registers, and a spawner's
 * 1.20 entity id becomes the 1.7.10 entity name a vanilla spawner takes (docs/new-spec/00-pack-integration/
 * loot-and-mobs.md, Q6). M08 replaces the categories with the tier bands of Q5; the names stay.
 *
 * <p>
 * Pure. Side: both (plain Java).
 */
public final class LootRules {

    /** The category of city chests: every value of {@code chestloot} except the rail dungeon's. */
    public static final String CITY = "lostcitiesnh:city";
    /** The category of rail dungeon chests: upstream's {@code raildungeonchest}. */
    public static final String RAIL_DUNGEON = "lostcitiesnh:rail_dungeon";

    /** 1.20 entity ids to 1.7.10 entity names where the name is not the id's path in camel case. */
    private static final Map<String, String> MOBS = mobs();

    private LootRules() {}

    /**
     * Returns the category a loot condition's value fills from.
     *
     * @param value the value the condition picked, an upstream loot-table id; {@code null} for none
     * @return the category, or the empty string for an empty chest
     */
    public static String category(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.contains("raildungeonchest") ? RAIL_DUNGEON : CITY;
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
