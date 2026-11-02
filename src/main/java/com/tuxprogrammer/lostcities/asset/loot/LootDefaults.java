package com.tuxprogrammer.lostcities.asset.loot;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * The default contents of this mod's loot categories, read from {@value #RESOURCE}: one tier band per category, Stone
 * and Steam to EV, plus the rail dungeon's, each a list of {@link LootEntry}s (docs/new-spec/00-pack-integration/
 * loot-and-mobs.md § Loot design; decision log D20). The entries' tiers are the tier map the EV ceiling is checked
 * against (docs/design/0004-gtnh-layer-and-tier-ceiling.md §3): {@link #problems()} lists every entry above
 * {@link LootTier#CEILING}, above its category's band, or whose loot bag group is not allowed, and a category that
 * could end up empty because none of its entries is vanilla.
 *
 * <p>
 * File format: {@code {"categories": {"<name>": {"band": "LV", "min": 2, "max": 4, "entries": [...]}}}}, where an
 * entry is an object with exactly one of {@code item} (registry name, with optional {@code meta}), {@code ore} (a name
 * or a list), {@code gt} (an {@code ItemList} constant) or {@code lootbag} (a group ID), plus {@code min} and
 * {@code max} (default 1), {@code weight} (default 1) and {@code tier}. Order is kept: the categories are filled in
 * file order, so their contents are the same on every server.
 *
 * <p>
 * Immutable. Side: both (plain Java).
 */
public final class LootDefaults {

    /** The bundled defaults, a class-path resource. */
    public static final String RESOURCE = "/data/lostcitiesnh/loot/categories.json";

    /** One category's defaults. */
    public static final class Category {

        private final String name;
        private final LootTier band;
        private final int minStacks;
        private final int maxStacks;
        private final List<LootEntry> entries;

        Category(String name, LootTier band, int minStacks, int maxStacks, List<LootEntry> entries) {
            this.name = name;
            this.band = band;
            this.minStacks = minStacks;
            this.maxStacks = maxStacks;
            this.entries = Collections.unmodifiableList(entries);
        }

        /** Returns the {@code ChestGenHooks} category name, such as {@code lostcitiesnh:city_lv}. */
        public String name() {
            return name;
        }

        /** Returns the band: the highest tier the category holds. */
        public LootTier band() {
            return band;
        }

        /** Returns the fewest stacks a chest gets. */
        public int minStacks() {
            return minStacks;
        }

        /** Returns the most stacks a chest gets. */
        public int maxStacks() {
            return maxStacks;
        }

        /** Returns the entries, in file order. */
        public List<LootEntry> entries() {
            return entries;
        }
    }

    private final Map<String, Category> categories;
    private final List<String> problems;

    private LootDefaults(Map<String, Category> categories, List<String> problems) {
        this.categories = Collections.unmodifiableMap(categories);
        this.problems = Collections.unmodifiableList(problems);
    }

    /**
     * Reads the bundled defaults.
     *
     * @return the defaults
     * @throws IllegalStateException when the resource is missing or not JSON
     */
    public static LootDefaults bundled() {
        try (InputStream in = LootDefaults.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException(RESOURCE + " is not in the jar");
            }
            return read(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("cannot read " + RESOURCE, e);
        }
    }

    /**
     * Reads defaults from JSON text.
     *
     * @param reader the JSON
     * @return the defaults, with {@link #problems()} for whatever is wrong in them
     * @throws IllegalStateException when the text is not a JSON object
     */
    public static LootDefaults read(Reader reader) {
        JsonElement root;
        try {
            root = new JsonParser().parse(reader);
        } catch (JsonParseException e) {
            throw new IllegalStateException("loot defaults are not JSON: " + e.getMessage(), e);
        }
        if (!root.isJsonObject() || !root.getAsJsonObject()
            .has("categories")) {
            throw new IllegalStateException("loot defaults need a 'categories' object");
        }
        List<String> problems = new ArrayList<>();
        Map<String, Category> out = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject()
            .getAsJsonObject("categories")
            .entrySet()) {
            Category category = category(
                entry.getKey(),
                entry.getValue()
                    .getAsJsonObject(),
                problems);
            if (category != null) {
                out.put(category.name, category);
            }
        }
        return new LootDefaults(out, problems);
    }

    /** Returns the categories by name, in file order. */
    public Map<String, Category> categories() {
        return categories;
    }

    /** Returns one line per problem; empty when the defaults keep every rule. */
    public List<String> problems() {
        return problems;
    }

    private static Category category(String name, JsonObject json, List<String> problems) {
        LootTier band = LootTier.parse(string(json, "band"));
        if (band == null) {
            problems.add(name + ": no valid 'band'");
            return null;
        }
        if (!band.withinCeiling()) {
            problems.add(name + ": band " + band + " is above the " + LootTier.CEILING + " ceiling");
        }
        int min = integer(json, "min", 1);
        int max = integer(json, "max", min);
        if (min < 1 || max < min) {
            problems.add(name + ": stack counts " + min + " to " + max + " are not a range from 1 up");
        }
        List<LootEntry> entries = new ArrayList<>();
        JsonArray array = json.has("entries") ? json.getAsJsonArray("entries") : new JsonArray();
        for (int i = 0; i < array.size(); i++) {
            LootEntry entry = entry(
                name + "[" + i + "]",
                array.get(i)
                    .getAsJsonObject(),
                band,
                problems);
            if (entry != null) {
                entries.add(entry);
            }
        }
        boolean anyVanilla = false;
        for (LootEntry entry : entries) {
            anyVanilla |= entry.isVanilla();
        }
        if (!anyVanilla) {
            problems.add(name + ": no vanilla entry, so the category is empty where its mods are absent");
        }
        return new Category(name, band, min, max, entries);
    }

    private static LootEntry entry(String where, JsonObject json, LootTier band, List<String> problems) {
        LootEntry.Source source = null;
        List<String> names = new ArrayList<>();
        int meta = integer(json, "meta", 0);
        int sources = 0;
        if (json.has("item")) {
            sources++;
            source = LootEntry.Source.ITEM;
            names.add(string(json, "item"));
        }
        if (json.has("ore")) {
            sources++;
            source = LootEntry.Source.ORE;
            JsonElement ore = json.get("ore");
            if (ore.isJsonArray()) {
                for (JsonElement name : ore.getAsJsonArray()) {
                    names.add(name.getAsString());
                }
            } else {
                names.add(ore.getAsString());
            }
        }
        if (json.has("gt")) {
            sources++;
            source = LootEntry.Source.GT;
            names.add(string(json, "gt"));
        }
        if (json.has("lootbag")) {
            sources++;
            source = LootEntry.Source.LOOTBAG;
            meta = integer(json, "lootbag", -1);
            names.add(LootBagGroups.ITEM);
        }
        if (sources != 1 || names.isEmpty() || names.contains(null)) {
            problems.add(where + ": needs exactly one of item, ore, gt or lootbag");
            return null;
        }
        LootTier tier = LootTier.parse(string(json, "tier"));
        if (tier == null) {
            problems.add(where + " (" + names + "): no valid 'tier'; every loot item needs one for the tier map");
            return null;
        }
        if (!tier.withinCeiling()) {
            problems.add(where + " (" + names + "): tier " + tier + " is above the " + LootTier.CEILING + " ceiling");
        }
        if (tier.compareTo(band) > 0) {
            problems.add(where + " (" + names + "): tier " + tier + " is above its category's band " + band);
        }
        if (source == LootEntry.Source.LOOTBAG) {
            LootTier groupTier = LootBagGroups.tierOf(meta);
            if (groupTier == null) {
                problems.add(where + ": loot bag group " + meta + " is not allowed in city loot");
            } else if (groupTier != tier) {
                problems.add(where + ": loot bag group " + meta + " is " + groupTier + ", not " + tier);
            }
        }
        if (source == LootEntry.Source.ITEM && names.get(0)
            .indexOf(':') <= 0) {
            problems.add(where + ": item '" + names.get(0) + "' is not a modid:name registry name");
        }
        int min = integer(json, "min", 1);
        int max = integer(json, "max", min);
        int weight = integer(json, "weight", 1);
        if (min < 1 || max < min || weight < 1) {
            problems.add(where + " (" + names + "): needs 1 <= min <= max and weight >= 1");
        }
        return new LootEntry(source, names, meta, min, max, weight, tier);
    }

    private static String string(JsonObject json, String key) {
        JsonElement value = json.get(key);
        return value == null || !value.isJsonPrimitive() ? null : value.getAsString();
    }

    private static int integer(JsonObject json, String key, int fallback) {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return value.getAsInt();
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
