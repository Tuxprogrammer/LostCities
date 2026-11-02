package com.tuxprogrammer.lostcities.asset.block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.Problems;
import com.tuxprogrammer.lostcities.asset.json.JsonFields;

/**
 * The block-name map: every 1.20 block name converted upstream assets and upstream's code use, mapped to GTNH 2.9.0
 * blocks with a vanilla fallback last (D19; docs/new-spec/00-pack-integration/block-mapping.md). It is data, read from
 * {@code data/lostcitiesnh/blockmap/mapping.json}, so the one place 1.20 names become 1.7.10 blocks is a table, not
 * code (D14).
 *
 * <p>
 * Besides global rows the map holds rows scoped to named assets (the cabin's copper, the gas station's iron and the oil
 * rig's diamond look-alike), the treasure characters that are loot, and the generic fallback for a decorative metal
 * whose entry names none. A row id may contain {@code {color}}, expanded over the sixteen dye colors in dye order,
 * with the color's index replacing {@code {color}} in the candidates' metadata. Side: both.
 */
public final class BlockMapping {

    /** The bundled data file, as a class-path resource. */
    public static final String RESOURCE = "/data/lostcitiesnh/blockmap/mapping.json";

    private static final String COLOR = "{color}";

    private final Map<String, MappingRow> global;
    private final Map<String, List<MappingRow>> scoped;
    private final Map<AssetId, Set<Character>> treasure;
    private final List<BlockRef> metalFallback;
    private final Map<String, RotationFamily> families;

    private BlockMapping(Map<String, MappingRow> global, Map<String, List<MappingRow>> scoped,
        Map<AssetId, Set<Character>> treasure, List<BlockRef> metalFallback, Map<String, RotationFamily> families) {
        this.global = global;
        this.scoped = scoped;
        this.treasure = treasure;
        this.metalFallback = metalFallback;
        this.families = families;
    }

    /**
     * Reads the map.
     *
     * @param json  the parsed {@code mapping.json}
     * @param scope where problems go
     * @return the map, holding every valid row
     */
    public static BlockMapping parse(JsonElement json, Problems.Scope scope) {
        Map<String, MappingRow> global = new TreeMap<>();
        Map<String, List<MappingRow>> scoped = new TreeMap<>();
        Map<AssetId, Set<Character>> treasure = new HashMap<>();
        List<BlockRef> metalFallback = new ArrayList<>();
        Map<String, RotationFamily> families = new TreeMap<>();
        if (!json.isJsonObject()) {
            scope.error("", null, "must be an object");
            return new BlockMapping(global, scoped, treasure, metalFallback, families);
        }
        JsonFields root = new JsonFields(json.getAsJsonObject(), "", scope);
        List<String> colors = root.stringList("colors", true);
        if (colors == null) {
            colors = Collections.emptyList();
        }
        for (JsonFields row : root.objects("blocks", true)) {
            for (MappingRow parsed : parseRow(row, colors, false)) {
                if (global.put(parsed.id(), parsed) != null) {
                    row.error("id", parsed.id(), "is mapped twice");
                }
                record(families, parsed);
            }
        }
        for (JsonFields row : root.objects("scoped", false)) {
            for (MappingRow parsed : parseRow(row, colors, true)) {
                scoped.computeIfAbsent(parsed.id(), k -> new ArrayList<>())
                    .add(parsed);
                record(families, parsed);
            }
        }
        for (JsonFields entry : root.objects("treasure", false)) {
            String asset = entry.string("asset", true);
            Character c = entry.character("char", true);
            if (asset != null && c != null) {
                try {
                    treasure.computeIfAbsent(AssetId.parse(asset), k -> new HashSet<>())
                        .add(c);
                } catch (IllegalArgumentException e) {
                    entry.error("asset", asset, e.getMessage());
                }
            }
        }
        List<String> fallback = root.stringList("metalFallback", true);
        if (fallback != null) {
            for (String text : fallback) {
                BlockRef ref = parseNative(root, "metalFallback", text);
                if (ref != null) {
                    metalFallback.add(ref);
                }
            }
            if (metalFallback.isEmpty() || !isVanilla(metalFallback.get(metalFallback.size() - 1))) {
                root.error("metalFallback", null, "must end in a vanilla block");
            }
        }
        return new BlockMapping(global, scoped, treasure, Collections.unmodifiableList(metalFallback), families);
    }

    /**
     * Returns the row for a 1.20 id as one asset uses it: a row scoped to that asset wins over the global row.
     *
     * @param id    the 1.20 id, without properties
     * @param asset the asset naming the block, or null outside any asset (code-named blocks)
     * @return the row, or null when the id is not mapped
     */
    public MappingRow find(String id, AssetId asset) {
        if (asset != null) {
            List<MappingRow> candidates = scoped.get(id);
            if (candidates != null) {
                for (MappingRow row : candidates) {
                    if (row.scope()
                        .contains(asset)) {
                        return row;
                    }
                }
            }
        }
        return global.get(id);
    }

    /** Returns whether a palette character of an asset is treasure, which is loot (block catalog, Treasure). */
    public boolean isTreasure(AssetId asset, char c) {
        Set<Character> chars = treasure.get(asset);
        return chars != null && chars.contains(c);
    }

    /** Returns the chain a decorative-metal block takes when its flag is off and its entry names no fallback. */
    public List<BlockRef> metalFallback() {
        return metalFallback;
    }

    /** Returns every global 1.20 id, sorted. */
    public Set<String> ids() {
        return Collections.unmodifiableSet(global.keySet());
    }

    /** Returns every row, global then scoped, for reports and tests. */
    public List<MappingRow> rows() {
        List<MappingRow> out = new ArrayList<>(global.values());
        for (List<MappingRow> rows : scoped.values()) {
            out.addAll(rows);
        }
        return out;
    }

    /**
     * Returns the rotation family of a 1.7.10 block that some row maps to, from the rule its metadata follows.
     *
     * @param registryName the block's registry name
     * @return the family, {@link RotationFamily#NONE} for blocks the map does not orient
     */
    public RotationFamily familyOf(String registryName) {
        RotationFamily family = families.get(registryName);
        return family == null ? RotationFamily.NONE : family;
    }

    /** Returns the rotation family of every block the map orients, by registry name. */
    public Map<String, RotationFamily> families() {
        return Collections.unmodifiableMap(families);
    }

    private static void record(Map<String, RotationFamily> families, MappingRow row) {
        List<MappingRow.Candidate> all = new ArrayList<>(row.chain());
        all.addAll(row.doubleChain());
        all.addAll(row.metalOff());
        for (MappingRow.Candidate candidate : all) {
            if (candidate.rule()
                .family() != RotationFamily.NONE) {
                families.put(
                    candidate.base()
                        .name(),
                    candidate.rule()
                        .family());
            }
        }
    }

    private static List<MappingRow> parseRow(JsonFields row, List<String> colors, boolean isScoped) {
        String id = row.string("id", true);
        if (id == null) {
            return Collections.emptyList();
        }
        if (!id.contains(COLOR)) {
            MappingRow parsed = parseOne(row, id, null, -1, isScoped);
            return parsed == null ? Collections.<MappingRow>emptyList() : Collections.singletonList(parsed);
        }
        List<MappingRow> out = new ArrayList<>();
        for (int i = 0; i < colors.size(); i++) {
            MappingRow parsed = parseOne(row, id.replace(COLOR, colors.get(i)), COLOR, i, isScoped);
            if (parsed != null) {
                out.add(parsed);
            }
        }
        return out;
    }

    private static MappingRow parseOne(JsonFields row, String id, String placeholder, int colorIndex,
        boolean isScoped) {
        try {
            BlockName.parse(id);
        } catch (IllegalArgumentException e) {
            row.error("id", id, e.getMessage());
            return null;
        }
        boolean hardAir = row.flag("hardAir", false);
        boolean matchOnly = row.flag("matchOnly", false);
        MetaRule rule = MetaRule.PLAIN;
        String ruleName = row.string("rule", false);
        if (ruleName != null) {
            rule = MetaRule.byName(ruleName);
            if (rule == null) {
                row.error("rule", ruleName, "is not a metadata rule");
                return null;
            }
        }
        List<MappingRow.Candidate> chain = chain(row, "to", !hardAir, rule, placeholder, colorIndex);
        List<MappingRow.Candidate> doubleChain = chain(row, "double", false, rule, placeholder, colorIndex);
        if (chain == null || doubleChain == null) {
            return null;
        }
        if (!hardAir && !matchOnly && !endsVanilla(chain)) {
            row.error("to", id, "must end in a vanilla block, so resolution never fails");
            return null;
        }
        if (!doubleChain.isEmpty() && !endsVanilla(doubleChain)) {
            row.error("double", id, "must end in a vanilla block");
            return null;
        }
        DecorativeMetal metal = null;
        String metalKey = row.string("metal", false);
        if (metalKey != null) {
            metal = DecorativeMetal.byKey(metalKey);
            if (metal == null) {
                row.error("metal", metalKey, "is not a decorative metal");
                return null;
            }
        }
        List<MappingRow.Candidate> metalOff = chain(row, "metalOff", metal != null, rule, placeholder, colorIndex);
        if (metalOff == null) {
            return null;
        }
        if (metal != null && !endsVanilla(metalOff)) {
            row.error("metalOff", id, "must end in a vanilla block");
            return null;
        }
        Set<AssetId> scope = new LinkedHashSet<>();
        if (isScoped) {
            List<String> assets = row.stringList("assets", true);
            if (assets == null || assets.isEmpty()) {
                row.error("assets", null, "a scoped row names at least one asset");
                return null;
            }
            for (String asset : assets) {
                try {
                    scope.add(AssetId.parse(asset));
                } catch (IllegalArgumentException e) {
                    row.error("assets", asset, e.getMessage());
                    return null;
                }
            }
        }
        JsonFields tagFields = row.object("tag", false);
        TileTemplate tag = tagFields == null ? null : TileTemplate.of(tagFields.object());
        return new MappingRow(
            id,
            chain,
            doubleChain,
            metal,
            metalOff,
            row.flag("capExempt", false),
            hardAir,
            matchOnly,
            tag,
            scope);
    }

    private static List<MappingRow.Candidate> chain(JsonFields row, String key, boolean required, MetaRule rowRule,
        String placeholder, int colorIndex) {
        JsonArray array = row.array(key, required);
        List<MappingRow.Candidate> out = new ArrayList<>();
        if (array == null) {
            // Missing and optional: an empty chain. Missing and required, or mistyped: already reported.
            return row.has(key) || required ? null : out;
        }
        for (int i = 0; i < array.size(); i++) {
            JsonElement element = array.get(i);
            String text;
            MetaRule rule = rowRule;
            if (element.isJsonPrimitive()) {
                text = element.getAsString();
            } else if (element.isJsonObject()) {
                JsonFields candidate = new JsonFields(
                    element.getAsJsonObject(),
                    row.field(key) + "[" + i + "]",
                    row.scope());
                text = candidate.string("block", true);
                String ruleName = candidate.string("rule", false);
                if (ruleName != null) {
                    rule = MetaRule.byName(ruleName);
                    if (rule == null) {
                        candidate.error("rule", ruleName, "is not a metadata rule");
                        return null;
                    }
                }
                if (text == null) {
                    return null;
                }
            } else {
                row.scope()
                    .error(row.field(key) + "[" + i + "]", element.toString(), "must be a block or an object");
                return null;
            }
            if (placeholder != null) {
                text = text.replace(placeholder, Integer.toString(colorIndex));
            }
            BlockRef ref = parseNative(row, key, text);
            if (ref == null) {
                return null;
            }
            out.add(new MappingRow.Candidate(ref, rule));
        }
        return out;
    }

    private static BlockRef parseNative(JsonFields row, String key, String text) {
        if (!BlockRef.isDirectForm(text)) {
            row.error(key, text, "a mapping target must be modid:name:meta, with the metadata written out");
            return null;
        }
        try {
            return BlockRef.parse(text);
        } catch (IllegalArgumentException e) {
            row.error(key, text, e.getMessage());
            return null;
        }
    }

    private static boolean endsVanilla(List<MappingRow.Candidate> chain) {
        return !chain.isEmpty() && isVanilla(
            chain.get(chain.size() - 1)
                .base());
    }

    private static boolean isVanilla(BlockRef ref) {
        return "minecraft".equals(ref.modId());
    }

    /** Returns a map with no rows, for tests that build their own. */
    public static BlockMapping empty() {
        return parse(new JsonObject(), new Problems().in("empty"));
    }
}
