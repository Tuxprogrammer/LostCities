package com.tuxprogrammer.lostcities.asset.model;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/cityassets/WorldStyle.java
// Avoids: ASSET-23, ASSET-25, ASSET-32, PROF-12, SCAT-06

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.google.gson.JsonElement;
import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetType;
import com.tuxprogrammer.lostcities.asset.block.BlockChoice;
import com.tuxprogrammer.lostcities.asset.json.JsonFields;
import com.tuxprogrammer.lostcities.asset.match.BiomeDescriptor;
import com.tuxprogrammer.lostcities.asset.match.BiomeMatcher;
import com.tuxprogrammer.lostcities.asset.match.WeightedPick;

/**
 * A world style: the top of the asset tree for a dimension. It names the city styles and where they appear, the style
 * outside cities, multibuilding and scattered settings, highway, railway and monorail parts, and support characters
 * (docs/original-spec/05-assets/world-styles.md). The profile names it ({@code worldStyle}, default {@code standard});
 * behavior is {@code 02-profiles-dimensions}'.
 *
 * <p>
 * Changes from upstream:
 * <ul>
 * <li>Biome matchers take the 1.7.10 vocabulary (P05-6, D31).</li>
 * <li>A world style must hold a city-style selector without a biome filter, and every selected city style must end up
 * with a style, so a city always resolves a style (P-PROF-8, D31; fixes PROF-12).</li>
 * <li>A scattered list's total weight plus {@code weightnone} must be above 0 (fixes SCAT-06); part lists the file
 * writes must not be empty and must name existing parts (fixes ASSET-32), while a slot left to its default name warns
 * when that part is missing; character fields are one character (fixes ASSET-23).</li>
 * <li>City-sphere settings are parsed and ignored with a warning, because sphere landscapes are not ported before beta
 * (D12; ASSET-25's enums are validated when spheres return).</li>
 * <li>Vine states in the {@code {"Name", "Properties"}} object form are read into the same block reference as a
 * palette string (D19).</li>
 * </ul>
 * Immutable. Side: both.
 */
public final class WorldStyle {

    /** Whether multibuildings avoid railway chunks. */
    public enum RailwayAvoidance {
        IGNORE,
        BLOCK_RAILWAY
    }

    /** A weighted city style with the biomes it may appear in. */
    public static final class CityStyleChoice {

        private final float factor;
        private final CityStyle cityStyle;
        private final BiomeMatcher biomes;

        CityStyleChoice(float factor, CityStyle cityStyle, BiomeMatcher biomes) {
            this.factor = factor;
            this.cityStyle = cityStyle;
            this.biomes = biomes;
        }

        /** Returns the weight. */
        public float factor() {
            return factor;
        }

        /** Returns the city style. */
        public CityStyle cityStyle() {
            return cityStyle;
        }

        /** Returns where it may be chosen. */
        public BiomeMatcher biomes() {
            return biomes;
        }
    }

    /** A city-chance multiplier for chunks whose main biome matches. */
    public static final class BiomeMultiplier {

        private final float multiplier;
        private final BiomeMatcher biomes;

        BiomeMultiplier(float multiplier, BiomeMatcher biomes) {
            this.multiplier = multiplier;
            this.biomes = biomes;
        }

        /** Returns the multiplier. */
        public float multiplier() {
            return multiplier;
        }

        /** Returns the biomes it applies to. */
        public BiomeMatcher biomes() {
            return biomes;
        }
    }

    /** A candidate of the scattered list. */
    public static final class ScatteredRef {

        private final Scattered scattered;
        private final int weight;
        private final boolean nearHighway;
        private final boolean allowVoid;
        private final BiomeMatcher biomes;
        private final Integer maxHeightDiff;

        ScatteredRef(Scattered scattered, int weight, boolean nearHighway, boolean allowVoid, BiomeMatcher biomes,
            Integer maxHeightDiff) {
            this.scattered = scattered;
            this.weight = weight;
            this.nearHighway = nearHighway;
            this.allowVoid = allowVoid;
            this.biomes = biomes;
            this.maxHeightDiff = maxHeightDiff;
        }

        /** Returns the scattered asset. */
        public Scattered scattered() {
            return scattered;
        }

        /** Returns the weight. */
        public int weight() {
            return weight;
        }

        /** Returns whether it must be placed next to a highway. */
        public boolean nearHighway() {
            return nearHighway;
        }

        /** Returns whether it may be placed over void. */
        public boolean allowVoid() {
            return allowVoid;
        }

        /** Returns the allowed biomes. */
        public BiomeMatcher biomes() {
            return biomes;
        }

        /** Returns the maximum terrain height difference under it, in blocks, or null. */
        public Integer maxHeightDiff() {
            return maxHeightDiff;
        }
    }

    /** Multibuilding placement settings, with upstream's defaults. */
    public static final class MultiSettings {

        /** Upstream's defaults when the object is absent. */
        static final MultiSettings DEFAULTS = new MultiSettings(10, 1, 5, 0.8f, 50);

        private final int areaSize;
        private final int minimum;
        private final int maximum;
        private final float correctStyleFactor;
        private final int attempts;

        MultiSettings(int areaSize, int minimum, int maximum, float correctStyleFactor, int attempts) {
            this.areaSize = areaSize;
            this.minimum = minimum;
            this.maximum = maximum;
            this.correctStyleFactor = correctStyleFactor;
            this.attempts = attempts;
        }

        /** Returns the size in chunks of the planning areas. */
        public int areaSize() {
            return areaSize;
        }

        /** Returns the minimum multibuildings per area. */
        public int minimum() {
            return minimum;
        }

        /** Returns the maximum multibuildings per area. */
        public int maximum() {
            return maximum;
        }

        /** Returns the weight factor for multibuildings that fit the area's city style. */
        public float correctStyleFactor() {
            return correctStyleFactor;
        }

        /** Returns the placement attempts per area. */
        public int attempts() {
            return attempts;
        }
    }

    /** Default part names of the part selector, by group and key. */
    static final Map<String, String> DEFAULT_PARTS = defaultParts();

    private final AssetId id;
    private final Style outsideStyle;
    private final List<CityStyleChoice> cityStyles;
    private final List<BiomeMultiplier> multipliers;
    private final MultiSettings multiSettings;
    private final RailwayAvoidance railwayAvoidance;
    private final int railPartHeight6;
    private final Map<String, BlockChoice> vines;
    private final Integer scatteredAreaSize;
    private final float scatteredChance;
    private final int scatteredWeightNone;
    private final List<ScatteredRef> scattered;
    private final Map<String, List<Part>> parts;
    private final Character bridgeSupport;
    private final Character highwaySupport;
    private final Part bridgeSupportPart;
    private final Part highwaySupportPart;

    private WorldStyle(Builder b) {
        this.id = b.id;
        this.outsideStyle = b.outsideStyle;
        this.cityStyles = Collections.unmodifiableList(b.cityStyles);
        this.multipliers = Collections.unmodifiableList(b.multipliers);
        this.multiSettings = b.multiSettings;
        this.railwayAvoidance = b.railwayAvoidance;
        this.railPartHeight6 = b.railPartHeight6;
        this.vines = Collections.unmodifiableMap(b.vines);
        this.scatteredAreaSize = b.scatteredAreaSize;
        this.scatteredChance = b.scatteredChance;
        this.scatteredWeightNone = b.scatteredWeightNone;
        this.scattered = Collections.unmodifiableList(b.scattered);
        this.parts = Collections.unmodifiableMap(b.parts);
        this.bridgeSupport = b.bridgeSupport;
        this.highwaySupport = b.highwaySupport;
        this.bridgeSupportPart = b.bridgeSupportPart;
        this.highwaySupportPart = b.highwaySupportPart;
    }

    /**
     * Reads a world style file.
     *
     * @param root    the document's fields
     * @param context the asset being read
     * @return the world style, or null after reporting an error
     */
    public static WorldStyle parse(JsonFields root, ParseContext context) {
        Builder b = new Builder();
        b.id = context.id();
        boolean ok = true;
        String outside = root.string("outsidestyle", true);
        b.outsideStyle = outside == null ? null
            : context.require(AssetType.STYLES, outside, Style.class, root.field("outsidestyle"));
        ok &= b.outsideStyle != null;
        boolean unfiltered = false;
        for (JsonFields entry : root.objects("citystyles", true)) {
            Float factor = entry.number("factor", true);
            String name = entry.string("citystyle", true);
            CityStyle style = name == null ? null
                : context.require(AssetType.CITYSTYLES, name, CityStyle.class, entry.field("citystyle"));
            JsonFields biomeFields = entry.object("biomes", false);
            BiomeMatcher biomes = biomeFields == null ? BiomeMatcher.ANY : BiomeMatcher.parse(biomeFields);
            if (factor == null || style == null) {
                ok = false;
                continue;
            }
            if (style.style() == null) {
                entry.error("citystyle", name, "this city style has no style anywhere in its inheritance chain");
                ok = false;
                continue;
            }
            unfiltered |= biomes.isAny() && factor > 0;
            b.cityStyles.add(new CityStyleChoice(factor, style, biomes));
        }
        if (ok && !unfiltered) {
            root.error(
                "citystyles",
                null,
                "needs a city style without a biome filter, so every biome resolves a city style (P-PROF-8)");
            ok = false;
        }
        for (JsonFields entry : root.objects("citybiomemultipliers", false)) {
            Float multiplier = entry.number("multiplier", true);
            JsonFields biomeFields = entry.object("biomes", false);
            if (multiplier == null) {
                ok = false;
                continue;
            }
            b.multipliers.add(
                new BiomeMultiplier(
                    multiplier,
                    biomeFields == null ? BiomeMatcher.ANY : BiomeMatcher.parse(biomeFields)));
        }
        JsonFields multi = root.object("multisettings", false);
        if (multi != null) {
            Integer area = multi.integer("areasize", true);
            Integer min = multi.integer("minimum", true);
            Integer max = multi.integer("maximum", true);
            if (area == null || min == null || max == null) {
                ok = false;
            } else {
                b.multiSettings = new MultiSettings(
                    area,
                    min,
                    max,
                    multi.number("correctstylefactor", 0.8f),
                    multi.integer("attempts", 50));
            }
        }
        ok &= settings(root, context, b);
        if (root.has("cityspheres")) {
            root.warning("cityspheres", null, "city spheres are not ported before beta (D12); ignored");
        }
        ok &= scattered(root, context, b);
        ok &= partSelector(root, context, b);
        b.bridgeSupport = root.character("bridgesupport", false);
        b.highwaySupport = root.character("highwaysupport", false);
        ok &= (b.bridgeSupport != null || !root.has("bridgesupport"))
            && (b.highwaySupport != null || !root.has("highwaysupport"));
        if (root.has("bridgesupportpart")) {
            b.bridgeSupportPart = context.optional(
                AssetType.PARTS,
                root.string("bridgesupportpart", true),
                Part.class,
                root.field("bridgesupportpart"));
        }
        if (root.has("highwaysupportpart")) {
            b.highwaySupportPart = context.optional(
                AssetType.PARTS,
                root.string("highwaysupportpart", true),
                Part.class,
                root.field("highwaysupportpart"));
        }
        return ok ? new WorldStyle(b) : null;
    }

    private static boolean settings(JsonFields root, ParseContext context, Builder b) {
        boolean ok = true;
        JsonFields settings = root.object("settings", false);
        if (settings != null) {
            b.railwayAvoidance = Scattered.enumValue(settings, "railwayavoidance", RailwayAvoidance.class);
            ok = b.railwayAvoidance != null;
            b.railPartHeight6 = settings.integer("railpartheight6", 1);
        }
        for (String side : new String[] { "west", "east", "south", "north" }) {
            String key = "vine" + side;
            String text = "minecraft:vine[" + side + "=true]";
            String field = settings == null ? key : settings.field(key);
            if (settings != null && settings.has(key)) {
                JsonFields state = settings.object(key, true);
                text = state == null ? null : stateString(state);
                if (text == null) {
                    ok = false;
                    continue;
                }
            }
            BlockChoice vine = context.block(text, null, field);
            if (vine == null) {
                ok = false;
            } else {
                b.vines.put(side, vine);
            }
        }
        return ok;
    }

    /** Turns {@code {"Name": id, "Properties": {...}}} into {@code id[prop=value,...]}. */
    private static String stateString(JsonFields state) {
        String name = state.string("Name", true);
        if (name == null) {
            return null;
        }
        JsonFields properties = state.object("Properties", false);
        if (properties == null) {
            return name;
        }
        List<String> pairs = new ArrayList<>();
        for (Map.Entry<String, JsonElement> property : properties.object()
            .entrySet()) {
            pairs.add(
                property.getKey() + "="
                    + property.getValue()
                        .getAsString());
        }
        return name + "[" + String.join(",", pairs) + "]";
    }

    private static boolean scattered(JsonFields root, ParseContext context, Builder b) {
        JsonFields scattered = root.object("scattered", false);
        if (scattered == null) {
            return true;
        }
        boolean ok = true;
        b.scatteredAreaSize = scattered.integer("areasize", true);
        Float chance = scattered.number("chance", true);
        Integer none = scattered.integer("weightnone", true);
        if (b.scatteredAreaSize == null || chance == null || none == null) {
            return false;
        }
        b.scatteredChance = chance;
        b.scatteredWeightNone = none;
        long total = Math.max(0, none);
        for (JsonFields entry : scattered.objects("list", true)) {
            String name = entry.string("name", true);
            Integer weight = entry.integer("weight", true);
            Scattered target = name == null ? null
                : context.require(AssetType.SCATTERED, name, Scattered.class, entry.field("name"));
            JsonFields biomeFields = entry.object("biomes", false);
            BiomeMatcher biomes = biomeFields == null ? BiomeMatcher.ANY : BiomeMatcher.parse(biomeFields);
            if (target == null || weight == null) {
                ok = false;
                continue;
            }
            total += Math.max(0, weight);
            b.scattered.add(
                new ScatteredRef(
                    target,
                    weight,
                    entry.flag("nearhighway", false),
                    entry.flag("allowvoid", false),
                    biomes,
                    entry.integer("maxheightdiff", false)));
        }
        if (ok && total <= 0) {
            scattered.error("weightnone", null, "the list's total weight plus weightnone must be above 0 (SCAT-06)");
            ok = false;
        }
        return ok;
    }

    private static boolean partSelector(JsonFields root, ParseContext context, Builder b) {
        JsonFields selector = root.object("parts", false);
        boolean ok = true;
        for (Map.Entry<String, String> entry : DEFAULT_PARTS.entrySet()) {
            String[] key = entry.getKey()
                .split("\\.");
            JsonFields group = selector == null ? null : selector.object(key[0], false);
            List<String> names = group != null && group.has(key[1]) ? group.stringOrList(key[1], true)
                : Collections.singletonList(entry.getValue());
            String field = group == null ? "parts." + entry.getKey() : group.field(key[1]);
            if (names == null) {
                ok = false;
                continue;
            }
            if (names.isEmpty()) {
                context.scope()
                    .error(field, "[]", "a part list must not be empty (an empty list crashed upstream)");
                ok = false;
                continue;
            }
            boolean written = group != null && group.has(key[1]);
            List<Part> parts = new ArrayList<>();
            for (int i = 0; i < names.size(); i++) {
                // A name the file writes is required; an unwritten slot's default name warns when missing.
                Part part = written ? context.require(AssetType.PARTS, names.get(i), Part.class, field)
                    : context.optional(AssetType.PARTS, names.get(i), Part.class, field);
                if (part == null) {
                    ok &= !written;
                } else {
                    parts.add(part);
                }
            }
            b.parts.put(entry.getKey(), Collections.unmodifiableList(parts));
        }
        return ok;
    }

    private static Map<String, String> defaultParts() {
        Map<String, String> out = new LinkedHashMap<>();
        out.put("monorails.both", "monorails_both");
        out.put("monorails.vertical", "monorails_vertical");
        out.put("monorails.station", "monorails_station");
        for (String key : new String[] { "tunnel", "open", "bridge", "tunnel_bi", "open_bi", "bridge_bi", "tunnel_bend",
            "open_bend", "bridge_bend", "tunnel_t", "open_t", "bridge_t" }) {
            out.put("highways." + key, "highway_" + key);
        }
        String[][] railways = { { "stationunderground", "station_underground" }, { "stationopen", "station_open" },
            { "stationopenroof", "station_openroof" }, { "stationundergroundstairs", "station_underground_stairs" },
            { "stationstaircase", "station_staircase" }, { "stationstaircasesurface", "station_staircase_surface" },
            { "railshorizontal", "rails_horizontal" }, { "railshorizontalend", "rails_horizontal_end" },
            { "railshorizontalwater", "rails_horizontal_water" }, { "railsvertical", "rails_vertical" },
            { "railsverticalwater", "rails_vertical_water" }, { "rails3split", "rails_3split" },
            { "railsbend", "rails_bend" }, { "railsflat", "rails_flat" }, { "railsdown1", "rails_down1" },
            { "railsdown2", "rails_down2" } };
        for (String[] pair : railways) {
            out.put("railways." + pair[0], pair[1]);
        }
        return Collections.unmodifiableMap(out);
    }

    /** Returns the id. */
    public AssetId id() {
        return id;
    }

    /** Returns the style for chunks outside cities. */
    public Style outsideStyle() {
        return outsideStyle;
    }

    /** Returns the city-style choices, in file order. */
    public List<CityStyleChoice> cityStyles() {
        return cityStyles;
    }

    /**
     * Picks a city style for a biome: keeps the choices whose matcher accepts it and draws one by weight.
     *
     * @param biome  the city center chunk's main biome
     * @param random the caller's random
     * @return the city style; never null, because one choice has no biome filter
     */
    public CityStyle randomCityStyle(BiomeDescriptor biome, Random random) {
        List<CityStyleChoice> matching = new ArrayList<>();
        for (CityStyleChoice choice : cityStyles) {
            if (choice.biomes.matches(biome)) {
                matching.add(choice);
            }
        }
        CityStyleChoice picked = WeightedPick.pick(random, matching, CityStyleChoice::factor);
        return picked == null ? null : picked.cityStyle;
    }

    /** Returns the city-chance multiplier of the first entry whose biomes match, else 1. */
    public float cityChanceMultiplier(BiomeDescriptor biome) {
        for (BiomeMultiplier multiplier : multipliers) {
            if (multiplier.biomes.matches(biome)) {
                return multiplier.multiplier;
            }
        }
        return 1f;
    }

    /** Returns the largest multiplier, at least 1, or positive infinity if any is not finite. */
    public float maxCityChanceMultiplier() {
        float max = 1f;
        for (BiomeMultiplier multiplier : multipliers) {
            if (Float.isInfinite(multiplier.multiplier) || Float.isNaN(multiplier.multiplier)) {
                return Float.POSITIVE_INFINITY;
            }
            max = Math.max(max, multiplier.multiplier);
        }
        return max;
    }

    /** Returns the city-chance multipliers, in file order. */
    public List<BiomeMultiplier> multipliers() {
        return multipliers;
    }

    /** Returns the multibuilding settings. */
    public MultiSettings multiSettings() {
        return multiSettings;
    }

    /** Returns whether multibuildings avoid railway chunks; {@code IGNORE} by default. */
    public RailwayAvoidance railwayAvoidance() {
        return railwayAvoidance;
    }

    /** Returns the railway part height in floors of 6 blocks. */
    public int railPartHeight6() {
        return railPartHeight6;
    }

    /**
     * Returns the vine hung on a wall facing a side.
     *
     * @param side {@code west}, {@code east}, {@code south} or {@code north}
     * @return the vine block
     */
    public BlockChoice vine(String side) {
        return vines.get(side);
    }

    /** Returns the scattered area size in chunks, or null when the world style has no scattered structures. */
    public Integer scatteredAreaSize() {
        return scatteredAreaSize;
    }

    /** Returns the chance that an area holds a structure. */
    public float scatteredChance() {
        return scatteredChance;
    }

    /** Returns the weight of choosing nothing. */
    public int scatteredWeightNone() {
        return scatteredWeightNone;
    }

    /** Returns the scattered candidates, in file order. */
    public List<ScatteredRef> scattered() {
        return scattered;
    }

    /**
     * Returns the parts of a part-selector slot.
     *
     * @param group {@code highways}, {@code railways} or {@code monorails}
     * @param key   the slot, such as {@code tunnel_bi} or {@code stationopen}
     * @return the parts; empty only when the slot's default part is missing (reported at load); null for an unknown
     *         slot
     */
    public List<Part> parts(String group, String key) {
        return parts.get(group + "." + key);
    }

    /** Returns every part-selector slot, {@code group.key} to parts. */
    public Map<String, List<Part>> parts() {
        return parts;
    }

    /** Returns the bridge support character, or null. */
    public Character bridgeSupport() {
        return bridgeSupport;
    }

    /** Returns the highway support character, or null. */
    public Character highwaySupport() {
        return highwaySupport;
    }

    /** Returns the bridge support part, or null. */
    public Part bridgeSupportPart() {
        return bridgeSupportPart;
    }

    /** Returns the highway support part, or null. */
    public Part highwaySupportPart() {
        return highwaySupportPart;
    }

    /** Collects the fields while parsing. */
    private static final class Builder {

        AssetId id;
        Style outsideStyle;
        final List<CityStyleChoice> cityStyles = new ArrayList<>();
        final List<BiomeMultiplier> multipliers = new ArrayList<>();
        MultiSettings multiSettings = MultiSettings.DEFAULTS;
        RailwayAvoidance railwayAvoidance = RailwayAvoidance.IGNORE;
        int railPartHeight6 = 1;
        final Map<String, BlockChoice> vines = new LinkedHashMap<>();
        Integer scatteredAreaSize;
        float scatteredChance;
        int scatteredWeightNone;
        final List<ScatteredRef> scattered = new ArrayList<>();
        final Map<String, List<Part>> parts = new LinkedHashMap<>();
        Character bridgeSupport;
        Character highwaySupport;
        Part bridgeSupportPart;
        Part highwaySupportPart;
    }
}
