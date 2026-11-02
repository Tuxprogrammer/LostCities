package com.tuxprogrammer.lostcities.asset.model;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/cityassets/CityStyle.java
// Avoids: ASSET-06, ASSET-07, ASSET-22, ASSET-23, CITY-37, ASSET-28, ASSET-32

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetType;
import com.tuxprogrammer.lostcities.asset.Problems;
import com.tuxprogrammer.lostcities.asset.json.JsonFields;

/**
 * A city style: which style a city's chunks use, which buildings, multibuildings and parts it draws from with what
 * weights, the palette characters of its streets, parks, corridors, rails and spheres, and its overrides of profile
 * chances (docs/original-spec/05-assets/styles-and-city-styles.md, "City style JSON").
 *
 * <p>
 * Inheritance is resolved at load for every city style, root first (fixes ASSET-28: a missing parent or a cycle
 * rejects the style, never a half-built one). Changes from upstream:
 * <ul>
 * <li>A child's selector list replaces its parent's when the child names that list (D15, fixes ASSET-06), so
 * {@code citystyle_border}'s {@code "multibuildings": []} means none.</li>
 * <li>{@code generalblocks} and {@code parkblocks.grass} inherit like every other character (fixes ASSET-07).</li>
 * <li>Character fields must be exactly one character (fixes ASSET-23).</li>
 * <li>{@code streetblocks.width} and {@code streetvariant} are parsed and ignored with a warning that they have no
 * effect (a note for upstream's bundled files, which carry them), and no method exposes the width, so nothing can unbox
 * it (fixes ASSET-22 and CITY-37, which upstream also
 * files as ASSET-27).</li>
 * <li>A street part list must not be empty (fixes ASSET-32).</li>
 * </ul>
 * Immutable once resolved. Side: both.
 */
public final class CityStyle {

    /** Scalar settings, each inherited from the parent when the child leaves it unset. */
    public enum Setting {

        EXPLOSION_CHANCE("", "explosionchance", Kind.NUMBER),
        BRIDGE_SUPPORT("", "bridgesupport", Kind.CHARACTER),
        OPEN_LOT_PARK_CHANCE("profile_overrides", "openLotParkChance", Kind.NUMBER),
        MIN_FLOORS("buildingsettings", "minfloors", Kind.INTEGER),
        MAX_FLOORS("buildingsettings", "maxfloors", Kind.INTEGER),
        MIN_CELLARS("buildingsettings", "mincellars", Kind.INTEGER),
        MAX_CELLARS("buildingsettings", "maxcellars", Kind.INTEGER),
        BUILDING_CHANCE("buildingsettings", "buildingchance", Kind.NUMBER),
        FOUNTAIN_CHANCE("streetblocks", "fountainchance", Kind.NUMBER),
        FRONT_CHANCE("streetblocks", "frontchance", Kind.NUMBER),
        STREET("streetblocks", "street", Kind.CHARACTER),
        STREET_BASE("streetblocks", "streetbase", Kind.CHARACTER),
        STREET_BORDER("streetblocks", "border", Kind.CHARACTER),
        STREET_WALL("streetblocks", "wall", Kind.CHARACTER),
        PARK_CHANCE("parkblocks", "parkchance", Kind.NUMBER),
        AVOID_FOLIAGE("parkblocks", "avoidfoliage", Kind.FLAG),
        PARK_BORDER("parkblocks", "parkborder", Kind.FLAG),
        PARK_ELEVATION("parkblocks", "parkelevation", Kind.FLAG),
        PARK_STREET_THRESHOLD("parkblocks", "parkstreetthreshold", Kind.INTEGER),
        PARK_ELEVATION_BLOCK("parkblocks", "elevation", Kind.CHARACTER),
        PARK_GRASS("parkblocks", "grass", Kind.CHARACTER),
        CORRIDOR_CHANCE("corridorblocks", "corridorchance", Kind.NUMBER),
        CORRIDOR_ROOF("corridorblocks", "roof", Kind.CHARACTER),
        CORRIDOR_GLASS("corridorblocks", "glass", Kind.CHARACTER),
        RAIL_MAIN("railblocks", "railmain", Kind.CHARACTER),
        SPHERE_INNER("sphereblocks", "inner", Kind.CHARACTER),
        SPHERE_BORDER("sphereblocks", "border", Kind.CHARACTER),
        SPHERE_GLASS("sphereblocks", "glass", Kind.CHARACTER),
        IRON_BARS("generalblocks", "ironbars", Kind.CHARACTER),
        GLOWSTONE("generalblocks", "glowstone", Kind.CHARACTER),
        LEAVES("generalblocks", "leaves", Kind.CHARACTER),
        RUBBLE_DIRT("generalblocks", "rubbledirt", Kind.CHARACTER);

        /** The value type of a setting. */
        enum Kind {
            NUMBER,
            INTEGER,
            FLAG,
            CHARACTER
        }

        private final String group;
        private final String key;
        private final Kind kind;

        Setting(String group, String key, Kind kind) {
            this.group = group;
            this.key = key;
            this.kind = kind;
        }

        /** Returns the JSON path of the setting, {@code streetblocks.street}. */
        public String path() {
            return group.isEmpty() ? key : group + "." + key;
        }
    }

    /** The nine selector lists of a city style, with the asset type each names. */
    public enum SelectorKind {

        BUILDINGS("buildings", AssetType.BUILDINGS, true),
        MULTIBUILDINGS("multibuildings", AssetType.MULTIBUILDINGS, true),
        PARKS("parks", AssetType.PARTS, true),
        FOUNTAINS("fountains", AssetType.PARTS, true),
        STAIRS("stairs", AssetType.PARTS, false),
        FRONTS("fronts", AssetType.PARTS, true),
        BRIDGES("bridges", AssetType.PARTS, true),
        LARGE_BRIDGES("largebridges", AssetType.PARTS, false),
        RAIL_DUNGEONS("raildungeons", AssetType.PARTS, true);

        private final String key;
        private final AssetType type;
        private final boolean required;

        SelectorKind(String key, AssetType type, boolean required) {
            this.key = key;
            this.type = type;
            this.required = required;
        }

        /** Returns the JSON key under {@code selectors}. */
        public String key() {
            return key;
        }

        /** Returns the type of the assets the list names. */
        public AssetType type() {
            return type;
        }
    }

    /** The three street part sets: normal streets, large streets, minor streets. */
    public enum StreetSet {

        PARTS("parts"),
        LARGE_PARTS("largeparts"),
        TERTIARY_PARTS("tertiaryparts");

        private final String key;

        StreetSet(String key) {
            this.key = key;
        }
    }

    /** The nine street part slots with upstream's default part names. */
    public enum StreetSlot {

        FULL("full", "street_full"),
        STRAIGHT("straight", "street_straight"),
        END("end", "street_end"),
        BEND("bend", "street_bend"),
        T("t", "street_t"),
        NONE("none", "street_none"),
        ALL("all", "street_all"),
        CONNECTOR("connector", "street_large_connector"),
        STAIR("stair", "street_stair");

        private final String key;
        private final String defaultPart;

        StreetSlot(String key, String defaultPart) {
            this.key = key;
            this.defaultPart = defaultPart;
        }

        /** Returns upstream's default part name for this slot. */
        public String defaultPart() {
            return defaultPart;
        }
    }

    /** The stuff tag every city style has. */
    public static final String ALL_STUFF = "all";

    private final AssetId id;
    private final AssetId parent;
    private final Style style;
    private final Part bridgeSupportPart;
    private final Map<Setting, Object> settings;
    private final Map<SelectorKind, List<Selector>> selectors;
    private final Map<StreetSet, Map<StreetSlot, List<Part>>> streets;
    private final Set<String> stuffTags;

    private CityStyle(AssetId id, AssetId parent, Style style, Part bridgeSupportPart, Map<Setting, Object> settings,
        Map<SelectorKind, List<Selector>> selectors, Map<StreetSet, Map<StreetSlot, List<Part>>> streets,
        Set<String> stuffTags) {
        this.id = id;
        this.parent = parent;
        this.style = style;
        this.bridgeSupportPart = bridgeSupportPart;
        this.settings = settings;
        this.selectors = selectors;
        this.streets = streets;
        this.stuffTags = stuffTags;
    }

    /**
     * Reads a city style file without its parent. The loader then calls {@link #inherit(CityStyle, Problems.Scope)}
     * root first.
     *
     * @param root    the document's fields
     * @param context the asset being read
     * @return the unresolved city style, or null after reporting an error
     */
    public static CityStyle parse(JsonFields root, ParseContext context) {
        boolean ok = true;
        AssetId parent = null;
        String inherit = root.string("inherit", false);
        if (inherit != null) {
            parent = context.name(inherit, root.field("inherit"));
            ok = parent != null;
        }
        Style style = null;
        if (root.has("style")) {
            style = context.require(AssetType.STYLES, root.string("style", true), Style.class, root.field("style"));
            ok &= style != null;
        }
        Part support = null;
        if (root.has("bridgesupportpart")) {
            support = context.optional(
                AssetType.PARTS,
                root.string("bridgesupportpart", true),
                Part.class,
                root.field("bridgesupportpart"));
        }
        Map<Setting, Object> settings = new EnumMap<>(Setting.class);
        for (Setting setting : Setting.values()) {
            JsonFields group = setting.group.isEmpty() ? root : root.object(setting.group, false);
            if (group == null || !group.has(setting.key)) {
                continue;
            }
            Object value = read(group, setting);
            if (value == null) {
                ok = false;
            } else {
                settings.put(setting, value);
            }
        }
        Object openLot = settings.get(Setting.OPEN_LOT_PARK_CHANCE);
        if (openLot != null && ((Float) openLot < 0 || (Float) openLot > 1)) {
            context.scope()
                .error(Setting.OPEN_LOT_PARK_CHANCE.path(), openLot.toString(), "must be between 0 and 1");
            ok = false;
        }
        JsonFields streetBlocks = root.object("streetblocks", false);
        Map<StreetSet, Map<StreetSlot, List<Part>>> streets = new EnumMap<>(StreetSet.class);
        if (streetBlocks != null) {
            // Upstream's own files carry these fields, so for a bundled file the fact is a note, and the boot stays
            // free of warnings; an override that sets them gets the warning.
            boolean bundled = context.scope()
                .file()
                .startsWith("jar:");
            for (String dead : new String[] { "width", "streetvariant" }) {
                if (streetBlocks.has(dead) && bundled) {
                    context.scope()
                        .note(streetBlocks.field(dead), null, "is parsed and has no effect (unused upstream too)");
                } else if (streetBlocks.has(dead)) {
                    streetBlocks.warning(dead, null, "is parsed and has no effect (unused upstream too)");
                }
            }
            for (StreetSet set : StreetSet.values()) {
                JsonFields fields = streetBlocks.object(set.key, false);
                if (fields != null) {
                    Map<StreetSlot, List<Part>> slots = streetSet(fields, context);
                    if (slots == null) {
                        ok = false;
                    } else {
                        streets.put(set, slots);
                    }
                }
            }
        }
        Map<SelectorKind, List<Selector>> selectors = new EnumMap<>(SelectorKind.class);
        JsonFields selectorFields = root.object("selectors", false);
        if (selectorFields != null) {
            for (SelectorKind kind : SelectorKind.values()) {
                if (!selectorFields.has(kind.key)) {
                    continue;
                }
                List<Selector> list = selectors(selectorFields, kind, context);
                if (list == null) {
                    ok = false;
                } else {
                    selectors.put(kind, list);
                }
            }
        }
        Set<String> tags = new TreeSet<>();
        List<String> stuffTags = root.stringList("stuff_tags", false);
        if (stuffTags != null) {
            tags.addAll(stuffTags);
        }
        return ok ? new CityStyle(context.id(), parent, style, support, settings, selectors, streets, tags) : null;
    }

    /**
     * Returns this style with its parent's settings filled in: scalars and street part sets the child leaves unset,
     * selector lists the child does not name (D15), and the union of the stuff tags plus {@value #ALL_STUFF}. Pass
     * null for a root style.
     *
     * @param resolvedParent the parent, already resolved, or null
     * @param scope          where problems go
     * @return the resolved style; a style still without {@code style} is valid as a parent, and the world style that
     *         selects it reports the gap
     */
    public CityStyle inherit(CityStyle resolvedParent, Problems.Scope scope) {
        Style mergedStyle = style;
        Part mergedSupport = bridgeSupportPart;
        Map<Setting, Object> mergedSettings = new EnumMap<>(Setting.class);
        Map<SelectorKind, List<Selector>> mergedSelectors = new EnumMap<>(SelectorKind.class);
        Map<StreetSet, Map<StreetSlot, List<Part>>> mergedStreets = new EnumMap<>(StreetSet.class);
        Set<String> tags = new TreeSet<>(stuffTags);
        if (resolvedParent != null) {
            mergedStyle = mergedStyle != null ? mergedStyle : resolvedParent.style;
            mergedSupport = mergedSupport != null ? mergedSupport : resolvedParent.bridgeSupportPart;
            mergedSettings.putAll(resolvedParent.settings);
            mergedSelectors.putAll(resolvedParent.selectors);
            mergedStreets.putAll(resolvedParent.streets);
            tags.addAll(resolvedParent.stuffTags);
        }
        mergedSettings.putAll(settings);
        mergedSelectors.putAll(selectors);
        mergedStreets.putAll(streets);
        tags.add(ALL_STUFF);
        return new CityStyle(
            id,
            parent,
            mergedStyle,
            mergedSupport,
            Collections.unmodifiableMap(mergedSettings),
            Collections.unmodifiableMap(mergedSelectors),
            Collections.unmodifiableMap(mergedStreets),
            Collections.unmodifiableSet(tags));
    }

    /** Returns the id. */
    public AssetId id() {
        return id;
    }

    /** Returns the parent's id, or null for a root style. */
    public AssetId parent() {
        return parent;
    }

    /** Returns the style of the city's chunk palettes; null only on an abstract style nothing uses directly. */
    public Style style() {
        return style;
    }

    /** Returns the bridge support part, or null to use the world style's. */
    public Part bridgeSupportPart() {
        return bridgeSupportPart;
    }

    /** Returns a number setting, or null when unset in the whole chain (the caller uses the profile's value). */
    public Float number(Setting setting) {
        return (Float) settings.get(setting);
    }

    /** Returns an integer setting, or null when unset. */
    public Integer integer(Setting setting) {
        return (Integer) settings.get(setting);
    }

    /** Returns a boolean setting, or null when unset. */
    public Boolean flag(Setting setting) {
        return (Boolean) settings.get(setting);
    }

    /** Returns a character setting, or null when unset. */
    public Character character(Setting setting) {
        return (Character) settings.get(setting);
    }

    /**
     * Returns a selector list.
     *
     * @param kind the list
     * @return the selectors after inheritance; empty when no style in the chain names the list
     */
    public List<Selector> selectors(SelectorKind kind) {
        List<Selector> list = selectors.get(kind);
        return list == null ? Collections.<Selector>emptyList() : list;
    }

    /**
     * Picks from a selector list at a chunk.
     *
     * @param kind   the list
     * @param random the caller's random
     * @param chunkX the chunk X
     * @param chunkZ the chunk Z
     * @return the chosen selector, or null when the list is empty or every weight is zero there
     */
    public Selector pick(SelectorKind kind, Random random, int chunkX, int chunkZ) {
        return Selector.pick(random, selectors(kind), chunkX, chunkZ);
    }

    /**
     * Returns the street parts of a slot.
     *
     * @param set  normal, large or minor streets
     * @param slot the connection shape
     * @return the parts; minor streets fall back to normal streets, and a set no style names uses upstream's default
     *         part names (resolved by the loader into {@link #withDefaults(Map)})
     */
    public List<Part> streetParts(StreetSet set, StreetSlot slot) {
        Map<StreetSlot, List<Part>> slots = streets.get(set);
        if (slots == null && set == StreetSet.TERTIARY_PARTS) {
            slots = streets.get(StreetSet.PARTS);
        }
        if (slots == null) {
            return Collections.emptyList();
        }
        List<Part> parts = slots.get(slot);
        return parts == null ? Collections.<Part>emptyList() : parts;
    }

    /**
     * Returns this style with the default street parts filled in for normal and large streets no style in the chain
     * names. Called by the loader once the parts are loaded.
     *
     * @param defaults the default part of each slot; slots whose default part is missing are left empty
     * @return the style
     */
    public CityStyle withDefaults(Map<StreetSlot, List<Part>> defaults) {
        Map<StreetSet, Map<StreetSlot, List<Part>>> filled = new EnumMap<>(StreetSet.class);
        filled.putAll(streets);
        for (StreetSet set : new StreetSet[] { StreetSet.PARTS, StreetSet.LARGE_PARTS }) {
            if (!filled.containsKey(set)) {
                filled.put(set, defaults);
            }
        }
        return new CityStyle(
            id,
            parent,
            style,
            bridgeSupportPart,
            settings,
            selectors,
            Collections.unmodifiableMap(filled),
            stuffTags);
    }

    /** Returns the stuff tags: the chain's union plus {@value #ALL_STUFF}. */
    public Set<String> stuffTags() {
        return stuffTags;
    }

    /** Returns every asset id the style names, for reachability. */
    public List<AssetId> references() {
        List<AssetId> out = new ArrayList<>();
        if (style != null) {
            out.add(style.id());
        }
        if (bridgeSupportPart != null) {
            out.add(bridgeSupportPart.id());
        }
        for (List<Selector> list : selectors.values()) {
            for (Selector selector : list) {
                out.add(selector.value());
            }
        }
        for (Map<StreetSlot, List<Part>> slots : streets.values()) {
            for (List<Part> parts : slots.values()) {
                for (Part part : parts) {
                    out.add(part.id());
                }
            }
        }
        return out;
    }

    private static Object read(JsonFields group, Setting setting) {
        switch (setting.kind) {
            case NUMBER:
                return group.number(setting.key, true);
            case INTEGER:
                return group.integer(setting.key, true);
            case FLAG:
                return group.bool(setting.key, true);
            default:
                return group.character(setting.key, true);
        }
    }

    private static Map<StreetSlot, List<Part>> streetSet(JsonFields fields, ParseContext context) {
        Map<StreetSlot, List<Part>> slots = new EnumMap<>(StreetSlot.class);
        boolean ok = true;
        for (StreetSlot slot : StreetSlot.values()) {
            List<String> names = fields.has(slot.key) ? fields.stringOrList(slot.key, true)
                : Collections.singletonList(slot.defaultPart);
            if (names == null) {
                ok = false;
                continue;
            }
            if (names.isEmpty()) {
                fields.error(slot.key, "[]", "a street part list must not be empty (an empty list crashed upstream)");
                ok = false;
                continue;
            }
            List<Part> parts = new ArrayList<>();
            for (int i = 0; i < names.size(); i++) {
                // Street parts are optional references upstream (getOrWarn): a missing one warns and is left out.
                Part part = context.optional(AssetType.PARTS, names.get(i), Part.class, fields.field(slot.key));
                if (part != null) {
                    parts.add(part);
                }
            }
            slots.put(slot, Collections.unmodifiableList(parts));
        }
        return ok ? Collections.unmodifiableMap(slots) : null;
    }

    private static List<Selector> selectors(JsonFields fields, SelectorKind kind, ParseContext context) {
        List<Selector> out = new ArrayList<>();
        boolean ok = true;
        for (JsonFields entry : fields.objects(kind.key, true)) {
            Float factor = entry.number("factor", true);
            String value = entry.string("value", true);
            int min = entry.integer("minSpawnDistance", 0);
            int max = entry.integer("maxSpawnDistance", Integer.MAX_VALUE);
            int feather = entry.integer("feather", 0);
            if (factor == null || value == null) {
                ok = false;
                continue;
            }
            if (factor < 0 || min < 0 || max < min || feather < 0) {
                entry.error(
                    "",
                    value,
                    "factor, minSpawnDistance and feather must not be negative, nor maxSpawnDistance below the minimum");
                ok = false;
                continue;
            }
            Class<?> type = kind.type == AssetType.BUILDINGS ? Building.class
                : kind.type == AssetType.MULTIBUILDINGS ? MultiBuilding.class : Part.class;
            Object target = kind.required ? context.require(kind.type, value, type, entry.field("value"))
                : context.optional(kind.type, value, type, entry.field("value"));
            if (target == null) {
                ok &= !kind.required;
                continue;
            }
            out.add(new Selector(factor, context.name(value, entry.field("value")), target, min, max, feather));
        }
        return ok ? Collections.unmodifiableList(out) : null;
    }
}
