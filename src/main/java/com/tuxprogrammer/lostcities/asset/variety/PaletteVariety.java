package com.tuxprogrammer.lostcities.asset.variety;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetType;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.BlockRegistryView;
import com.tuxprogrammer.lostcities.asset.Problems;
import com.tuxprogrammer.lostcities.asset.block.BlockResolver;
import com.tuxprogrammer.lostcities.asset.json.JsonFields;
import com.tuxprogrammer.lostcities.asset.model.Palette;
import com.tuxprogrammer.lostcities.asset.model.ParseContext;

/**
 * The palette variety of the block catalog's pools (D25, D28; docs/new-spec/05-assets/asset-format.md, "Palette
 * variety"): small option palettes, each one candidate of a pool for one role (a wall, a band, glass, an interior
 * floor,
 * a ceiling, a light, furniture, park flora), and per style the groups that draw one option per building on top of the
 * style's own palettes. Read from {@code data/lostcitiesnh/blockmap/variety.json}, which
 * {@code tools/asset-convert/variety.py} writes from the catalog's pools.
 *
 * <p>
 * How a chunk uses it: the layout draws the style's palettes as upstream does, then, with a random of its own, one
 * option per group that applies (a group may apply only when the style drew a given palette, so a stone-brick
 * building's
 * wall options never land on a cyan terracotta one, and only to building chunks or only to street and park chunks). The
 * chosen options are appended to the chunk's palette list, so they sit above the style's palettes and below the
 * building's and part's own palettes, which keep the last word, as upstream's layering does. A multibuilding's chunks
 * share their top-left chunk's choice.
 *
 * <p>
 * An option whose blocks are not all registered is unavailable: its share goes to the group's baseline ({@code none}),
 * as a pool candidate falls back to the baseline when its mod is absent. The real-pack resolution report lists such
 * blocks, so the bundled options are checked against the pack. The draw is a pure function of the seed and the group's
 * name, so editing one group never moves another.
 *
 * <p>
 * Option palettes may define the interior and park characters below, which no upstream part uses: the part writer
 * substitutes them for a building's floor, ceiling and ceiling lights, and the park greenery draws its plants from
 * them.
 * Undefined, nothing changes. Immutable once read. Side: both (plain Java).
 */
public final class PaletteVariety {

    /** The bundled data file, as a class-path resource. */
    public static final String RESOURCE = "/data/lostcitiesnh/blockmap/variety.json";

    /** The namespace of the option palettes' ids. */
    public static final String NAMESPACE = "lostcitiesnh";
    /** The path prefix of the option palettes' ids: {@code lostcitiesnh:variety/<option>}. */
    public static final String PREFIX = "variety/";
    /** The option name that adds no palette: the group's baseline, upstream's look. */
    public static final String NONE = "none";

    /** An interior floor: a building's floor block with air above it and a ceiling over that air. */
    public static final char FLOOR = '';
    /** An interior ceiling: a building's block with air below it and a floor under that air. */
    public static final char CEILING = '';
    /** A ceiling light: a ceiling block on the light grid. */
    public static final char CEILING_LIGHT = '';
    /** Park plants of temperate land, a 128-slot list drawn per free grass block. */
    public static final char FLORA_TEMPERATE = '';
    /** Park plants of hot, dry land. */
    public static final char FLORA_ARID = '';
    /** Park plants of cold land. */
    public static final char FLORA_COLD = '';
    /** Park plants of jungles. */
    public static final char FLORA_JUNGLE = '';
    /** Park plants of swamps. */
    public static final char FLORA_SWAMP = '';

    /** Which chunks a group applies to. */
    public enum Scope {

        /** Every city chunk. */
        ALL,
        /** Chunks with a building. */
        BUILDING,
        /** Chunks without a building: streets, parks, plazas. */
        STREET;

        boolean applies(boolean building) {
            return this == ALL || (this == BUILDING) == building;
        }
    }

    /** One option palette. */
    public static final class Option {

        private final String name;
        private final String pool;
        private final AssetId id;
        private final Palette palette;
        private final List<String> missing;

        Option(String name, String pool, AssetId id, Palette palette, List<String> missing) {
            this.name = name;
            this.pool = pool;
            this.id = id;
            this.palette = palette;
            this.missing = missing;
        }

        /** Returns the option's name. */
        public String name() {
            return name;
        }

        /** Returns the catalog pool it comes from. */
        public String pool() {
            return pool;
        }

        /** Returns the palette's id, {@code lostcitiesnh:variety/<name>}. */
        public AssetId id() {
            return id;
        }

        /** Returns the palette, or null when the option is unavailable. */
        public Palette palette() {
            return palette;
        }

        /** Returns the registry names of the blocks that are not registered; empty when the option is available. */
        public List<String> missing() {
            return missing;
        }

        /** Returns whether every block of the option is registered and its palette parsed. */
        public boolean available() {
            return palette != null;
        }
    }

    /** One weighted choice of a group: an option, or the baseline when {@code option} is null. */
    public static final class Choice {

        private final Option option;
        private final int weight;

        Choice(Option option, int weight) {
            this.option = option;
            this.weight = weight;
        }

        /** Returns the option, or null for the baseline. */
        public Option option() {
            return option;
        }

        /** Returns the weight. */
        public int weight() {
            return weight;
        }
    }

    /** One group: a role whose options a building draws one of. */
    public static final class Group {

        private final String name;
        private final AssetId when;
        private final Scope scope;
        private final List<Choice> choices;
        private final int total;
        private final long salt;

        Group(String name, AssetId when, Scope scope, List<Choice> choices) {
            this.name = name;
            this.when = when;
            this.scope = scope;
            this.choices = choices;
            int sum = 0;
            for (Choice choice : choices) {
                sum += choice.weight;
            }
            this.total = sum;
            this.salt = salt(name);
        }

        /** Returns the group's name, unique within its style. */
        public String name() {
            return name;
        }

        /** Returns the palette the style must have drawn for the group to apply, or null for any. */
        public AssetId when() {
            return when;
        }

        /** Returns which chunks the group applies to. */
        public Scope scope() {
            return scope;
        }

        /** Returns the choices, in file order. */
        public List<Choice> choices() {
            return choices;
        }

        /**
         * Draws this group's option for one building.
         *
         * @param seed the building's variety seed
         * @return the option, or null for the baseline (or an unavailable option, whose share is the baseline's)
         */
        public Option draw(long seed) {
            if (total <= 0) {
                return null;
            }
            long roll = Math.floorMod(mix(seed ^ salt), (long) total);
            for (Choice choice : choices) {
                roll -= choice.weight;
                if (roll < 0) {
                    return choice.option == null || !choice.option.available() ? null : choice.option;
                }
            }
            return null;
        }
    }

    private final Map<String, Option> options;
    private final Map<AssetId, Option> byId;
    private final Map<AssetId, List<Group>> styles;

    private PaletteVariety(Map<String, Option> options, Map<AssetId, List<Group>> styles) {
        this.options = Collections.unmodifiableMap(options);
        Map<AssetId, Option> ids = new TreeMap<>();
        for (Option option : options.values()) {
            ids.put(option.id, option);
        }
        this.byId = Collections.unmodifiableMap(ids);
        this.styles = Collections.unmodifiableMap(styles);
    }

    /** Returns a catalog with no options and no groups. */
    public static PaletteVariety empty() {
        return new PaletteVariety(new LinkedHashMap<>(), new TreeMap<>());
    }

    /**
     * Reads the catalog and parses each available option's palette. Runs after the assets are loaded, so option
     * palettes may name variants, and {@code when} palettes must exist.
     *
     * @param json     the parsed {@code variety.json}
     * @param registry the live registry
     * @param resolver the block resolver the assets used
     * @param lookup   the loaded assets
     * @param problems where problems go
     * @param origin   the file's label for problem lines
     * @return the catalog
     */
    public static PaletteVariety parse(JsonElement json, BlockRegistryView registry, BlockResolver resolver,
        ParseContext.Lookup lookup, Problems problems, String origin) {
        Problems.Scope scope = problems.in(origin);
        Map<String, JsonFields> raw = new LinkedHashMap<>();
        Map<String, Option> options = new LinkedHashMap<>();
        Map<AssetId, List<Group>> styles = new TreeMap<>();
        if (!json.isJsonObject()) {
            scope.error("", null, "must be an object");
            return new PaletteVariety(options, styles);
        }
        JsonFields root = new JsonFields(json.getAsJsonObject(), "", scope);
        for (JsonFields fields : root.objects("options", true)) {
            String name = fields.string("name", true);
            if (name != null && raw.put(name, fields) != null) {
                fields.error("name", name, "is used by two options");
            }
        }
        OptionReader reader = new OptionReader(raw, options, registry, resolver, lookup, problems, origin);
        for (JsonFields style : root.objects("styles", true)) {
            String name = style.string("style", true);
            if (name == null) {
                continue;
            }
            AssetId id;
            try {
                id = AssetId.parse(name);
            } catch (IllegalArgumentException e) {
                style.error("style", name, e.getMessage());
                continue;
            }
            if (lookup.find(AssetType.STYLES, id) == null) {
                // A set without the style (a test set, or an override that removed it) never draws its groups.
                style.scope()
                    .note(style.field("style"), name, "names no loaded style; its groups are not read");
                continue;
            }
            List<Group> groups = new ArrayList<>();
            Set<String> names = new LinkedHashSet<>();
            for (JsonFields group : style.objects("groups", true)) {
                Group parsed = group(group, reader, lookup);
                if (parsed == null) {
                    continue;
                }
                if (!names.add(parsed.name)) {
                    group.error("name", parsed.name, "is used by two groups of this style");
                    continue;
                }
                groups.add(parsed);
            }
            if (styles.put(id, Collections.unmodifiableList(groups)) != null) {
                style.error("style", name, "is listed twice");
            }
        }
        return new PaletteVariety(options, styles);
    }

    /** Reads options on first use, so only the options of loaded styles are parsed and checked. */
    private static final class OptionReader {

        private final Map<String, JsonFields> raw;
        private final Map<String, Option> read;
        private final Set<String> failed = new LinkedHashSet<>();
        private final BlockRegistryView registry;
        private final BlockResolver resolver;
        private final ParseContext.Lookup lookup;
        private final Problems problems;
        private final String origin;

        OptionReader(Map<String, JsonFields> raw, Map<String, Option> read, BlockRegistryView registry,
            BlockResolver resolver, ParseContext.Lookup lookup, Problems problems, String origin) {
            this.raw = raw;
            this.read = read;
            this.registry = registry;
            this.resolver = resolver;
            this.lookup = lookup;
            this.problems = problems;
            this.origin = origin;
        }

        /** Returns the option, reading it the first time; null when it does not exist or was rejected. */
        Option get(String name) {
            Option option = read.get(name);
            if (option != null || failed.contains(name)) {
                return option;
            }
            JsonFields fields = raw.get(name);
            option = fields == null ? null
                : PaletteVariety.option(fields, registry, resolver, lookup, problems, origin);
            if (option == null) {
                failed.add(name);
            } else {
                read.put(name, option);
            }
            return option;
        }

        boolean exists(String name) {
            return raw.containsKey(name);
        }
    }

    /** Reads one option: unavailable when a block is not registered, null when its palette is rejected. */
    private static Option option(JsonFields fields, BlockRegistryView registry, BlockResolver resolver,
        ParseContext.Lookup lookup, Problems problems, String origin) {
        String name = fields.string("name", true);
        if (name == null) {
            return null;
        }
        if (!name.matches("[a-z0-9_]+")) {
            fields.error("name", name, "an option name is lower case letters, digits and '_'");
            return null;
        }
        if (NONE.equals(name)) {
            fields.error("name", name, "is reserved for the baseline");
            return null;
        }
        String pool = fields.string("pool", false);
        AssetId id = AssetId.of(NAMESPACE, PREFIX + name);
        List<String> missing = new ArrayList<>();
        JsonArray entries = fields.array("palette", true);
        if (entries == null) {
            return null;
        }
        for (String block : blockStrings(entries)) {
            if (!BlockRef.isDirectForm(block)) {
                continue;
            }
            String registryName;
            try {
                registryName = BlockRef.parse(block)
                    .name();
            } catch (IllegalArgumentException e) {
                fields.error("palette", block, e.getMessage());
                return null;
            }
            if (!registry.exists(registryName) && !missing.contains(registryName)) {
                missing.add(registryName);
            }
        }
        if (!missing.isEmpty()) {
            return new Option(name, pool, id, null, Collections.unmodifiableList(missing));
        }
        Problems.Scope scope = problems.in(origin + " option " + name);
        int before = problems.size();
        Palette palette = Palette.parse(fields, id, new ParseContext(id, scope, resolver, lookup));
        if (problems.errorsSince(before)) {
            scope.error("", name, "option rejected");
            return null;
        }
        return new Option(name, pool, id, palette, Collections.<String>emptyList());
    }

    private static Group group(JsonFields fields, OptionReader options, ParseContext.Lookup lookup) {
        String name = fields.string("name", true);
        if (name == null) {
            return null;
        }
        AssetId when = null;
        String whenName = fields.string("when", false);
        if (whenName != null) {
            try {
                when = AssetId.parse(whenName);
            } catch (IllegalArgumentException e) {
                fields.error("when", whenName, e.getMessage());
                return null;
            }
            if (lookup.find(AssetType.PALETTES, when) == null) {
                fields.error("when", whenName, "names no loaded palette");
                return null;
            }
        }
        Scope scope = Scope.ALL;
        String scopeName = fields.string("for", false);
        if (scopeName != null) {
            try {
                scope = Scope.valueOf(scopeName.toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException e) {
                fields.error("for", scopeName, "is all, building or street");
                return null;
            }
        }
        List<Choice> choices = new ArrayList<>();
        boolean ok = true;
        for (JsonFields choice : fields.objects("options", true)) {
            String optionName = choice.string("option", true);
            Integer weight = choice.integer("weight", true);
            if (optionName == null || weight == null) {
                ok = false;
                continue;
            }
            if (weight <= 0) {
                choice.error("weight", String.valueOf(weight), "a weight is a positive whole number");
                ok = false;
                continue;
            }
            if (NONE.equals(optionName)) {
                choices.add(new Choice(null, weight));
                continue;
            }
            if (!options.exists(optionName)) {
                choice.error("option", optionName, "names no option of the file");
                ok = false;
                continue;
            }
            Option option = options.get(optionName);
            if (option == null) {
                // Rejected with its own errors: the group falls back to its baseline for this share.
                choices.add(new Choice(null, weight));
                continue;
            }
            choices.add(new Choice(option, weight));
        }
        if (!ok || choices.isEmpty()) {
            if (choices.isEmpty() && ok) {
                fields.error("options", null, "a group has at least one option");
            }
            return null;
        }
        return new Group(name, when, scope, Collections.unmodifiableList(choices));
    }

    /** Returns every block string an option's palette entries name: blocks, list blocks and damaged targets. */
    static List<String> blockStrings(JsonArray entries) {
        List<String> out = new ArrayList<>();
        for (JsonElement element : entries) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject entry = element.getAsJsonObject();
            addString(out, entry.get("block"));
            addString(out, entry.get("damaged"));
            JsonElement list = entry.get("blocks");
            if (list != null && list.isJsonArray()) {
                for (JsonElement row : list.getAsJsonArray()) {
                    if (row.isJsonObject()) {
                        addString(
                            out,
                            row.getAsJsonObject()
                                .get("block"));
                    }
                }
            }
        }
        return out;
    }

    private static void addString(List<String> out, JsonElement element) {
        if (element != null && element.isJsonPrimitive()
            && element.getAsJsonPrimitive()
                .isString()) {
            out.add(element.getAsString());
        }
    }

    /**
     * Draws a chunk's variety: one option per group of the style that applies, in group order.
     *
     * @param style    the style the chunk's palettes came from
     * @param chosen   the palette ids the style drew, as {@code namespace:path}
     * @param building whether the chunk holds a building
     * @param seed     the chunk's (or its multibuilding's) variety seed
     * @return the option palettes' ids, as {@code namespace:path}, lowest layer first; empty when nothing applies
     */
    public List<String> pick(AssetId style, List<String> chosen, boolean building, long seed) {
        List<Group> groups = styles.get(style);
        if (groups == null || groups.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<>();
        for (Group group : groups) {
            if (!group.scope.applies(building)) {
                continue;
            }
            if (group.when != null && !chosen.contains(group.when.toString())) {
                continue;
            }
            Option option = group.draw(seed);
            if (option != null) {
                out.add(
                    option.id()
                        .toString());
            }
        }
        return out;
    }

    /** Returns an option's palette by id, or null when there is none or it is unavailable. */
    public Palette palette(AssetId id) {
        Option option = byId.get(id);
        return option == null ? null : option.palette;
    }

    /** Returns every option, in file order. */
    public Map<String, Option> options() {
        return options;
    }

    /** Returns the groups of a style, or an empty list. */
    public List<Group> groups(AssetId style) {
        List<Group> groups = styles.get(style);
        return groups == null ? Collections.<Group>emptyList() : groups;
    }

    /** Returns the styles that have groups, sorted. */
    public Set<AssetId> styles() {
        return styles.keySet();
    }

    /** Returns every option block that is not registered, as {@code option: name}, for the report. */
    public List<String> unresolved() {
        List<String> out = new ArrayList<>();
        for (Option option : options.values()) {
            for (String name : option.missing) {
                out.add(option.name + ": " + name);
            }
        }
        return out;
    }

    /** Returns how many options are available. */
    public int availableCount() {
        int count = 0;
        for (Option option : options.values()) {
            if (option.available()) {
                count++;
            }
        }
        return count;
    }

    /** A group's salt: its name's hash spread over 64 bits (String.hashCode is fixed by the Java specification). */
    static long salt(String name) {
        return mix(name.hashCode() * 0x9E3779B97F4A7C15L);
    }

    /** SplitMix64's finalizer: a well-spread 64-bit value from any input. */
    static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
