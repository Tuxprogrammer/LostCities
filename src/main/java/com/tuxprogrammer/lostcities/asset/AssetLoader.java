package com.tuxprogrammer.lostcities.asset;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/cityassets/AssetRegistries.java
// Avoids: ASSET-01, ASSET-15, ASSET-28, SCAT-22

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.tuxprogrammer.lostcities.asset.block.BlockChoice;
import com.tuxprogrammer.lostcities.asset.block.BlockData;
import com.tuxprogrammer.lostcities.asset.block.BlockResolver;
import com.tuxprogrammer.lostcities.asset.block.VarietyPools;
import com.tuxprogrammer.lostcities.asset.json.JsonFields;
import com.tuxprogrammer.lostcities.asset.model.Building;
import com.tuxprogrammer.lostcities.asset.model.CityStyle;
import com.tuxprogrammer.lostcities.asset.model.Condition;
import com.tuxprogrammer.lostcities.asset.model.ConditionContext;
import com.tuxprogrammer.lostcities.asset.model.MultiBuilding;
import com.tuxprogrammer.lostcities.asset.model.Palette;
import com.tuxprogrammer.lostcities.asset.model.PaletteEntry;
import com.tuxprogrammer.lostcities.asset.model.ParseContext;
import com.tuxprogrammer.lostcities.asset.model.Part;
import com.tuxprogrammer.lostcities.asset.model.PredefinedCity;
import com.tuxprogrammer.lostcities.asset.model.PredefinedSphere;
import com.tuxprogrammer.lostcities.asset.model.RandomList;
import com.tuxprogrammer.lostcities.asset.model.Scattered;
import com.tuxprogrammer.lostcities.asset.model.Selector;
import com.tuxprogrammer.lostcities.asset.model.Stuff;
import com.tuxprogrammer.lostcities.asset.model.Style;
import com.tuxprogrammer.lostcities.asset.model.Variant;
import com.tuxprogrammer.lostcities.asset.model.WorldStyle;
import com.tuxprogrammer.lostcities.asset.variety.PaletteVariety;

/**
 * Loads every asset once, in plain Java: reads the sources in order (the jar, then the config folder, whose files
 * replace jar files with the same type and id), parses each type after the types it can name, resolves every name and
 * block, resolves city-style inheritance root first, builds the stuff index, and returns an immutable
 * {@link AssetSet} (docs/new-spec/05-assets/asset-format.md, "Loading and override"; D14).
 *
 * <p>
 * Replaces upstream's lazily compiled data-pack registries. Changes from upstream, each a bug fix:
 * <ul>
 * <li>Every name is resolved at load; a required reference that does not resolve rejects the referring asset with its
 * file and field, and an optional one warns once and acts as absent, so no lookup throws during generation (fixes
 * ASSET-01).</li>
 * <li>The stuff-by-tag index is built at load, before any chunk generates, each tag's list sorted by id (fixes
 * ASSET-15 and SCAT-22).</li>
 * <li>City-style inheritance is resolved for every city style at load, root first; a missing parent or a cycle rejects
 * the style (fixes ASSET-28).</li>
 * </ul>
 * A malformed asset is rejected with its errors and loading continues, so a real pack skips the asset with one clear
 * log line while the dev self-check fails on it (CONTRIBUTING § Code standards). Side: both; runs at {@code postInit}.
 */
public final class AssetLoader {

    /**
     * The blocks upstream's code names, by key, as 1.20 names resolved through the block-name map
     * (docs/new-spec/00-pack-integration/block-mapping.md, "Blocks upstream's code names").
     */
    public static final Map<String, String> CODE_BLOCKS = codeBlocks();

    /** The key of the palette variety in the file hashes a world pins. */
    public static final String VARIETY_KEY = "blockmap/variety";

    private AssetLoader() {}

    /**
     * Loads the asset set.
     *
     * @param sources  the sources, lowest priority first: the jar, then the config folder
     * @param data     the bundled block data
     * @param registry the live block registry
     * @param problems where every problem goes; the caller logs or checks them
     * @return the asset set, holding every valid asset
     */
    public static AssetSet load(List<AssetSource> sources, BlockData data, BlockRegistryView registry,
        Problems problems) {
        Map<String, AssetFile> files = new TreeMap<>();
        for (AssetSource source : sources) {
            for (AssetFile file : source.files(problems)) {
                AssetFile replaced = files.put(key(file.type(), file.id()), file);
                if (replaced != null) {
                    problems.in(file.origin())
                        .note("", null, "replaces " + replaced.origin());
                }
            }
        }
        BlockResolver resolver = new BlockResolver(data.mapping(), data.metals(), data.neverPlace(), registry);
        Map<AssetType, SortedMap<AssetId, Object>> assets = AssetSet.emptyMaps();
        Map<AssetType, Set<AssetId>> rejected = new EnumMap<>(AssetType.class);
        for (AssetType type : AssetType.values()) {
            rejected.put(type, new HashSet<AssetId>());
        }
        ParseContext.Lookup lookup = new ParseContext.Lookup() {

            @Override
            public Object find(AssetType type, AssetId id) {
                return assets.get(type)
                    .get(id);
            }

            @Override
            public boolean rejected(AssetType type, AssetId id) {
                return rejected.get(type)
                    .contains(id);
            }
        };
        Map<AssetId, CityStyle> rawCityStyles = new TreeMap<>();
        Map<AssetId, AssetFile> cityStyleFiles = new TreeMap<>();
        for (AssetType type : AssetType.values()) {
            if (type == AssetType.SCATTERED) {
                // City styles are complete before anything that names them.
                resolveCityStyles(rawCityStyles, cityStyleFiles, assets, rejected, problems, lookup, resolver);
            }
            for (AssetFile file : files.values()) {
                if (file.type() != type) {
                    continue;
                }
                Problems.Scope scope = problems.in(file.origin());
                int before = problems.size();
                Object parsed = parse(file, new ParseContext(file.id(), scope, resolver, lookup));
                if (parsed == null || problems.errorsSince(before)) {
                    rejected.get(type)
                        .add(file.id());
                    scope.error(
                        "",
                        file.id()
                            .toString(),
                        "asset rejected");
                } else if (type == AssetType.CITYSTYLES) {
                    rawCityStyles.put(file.id(), (CityStyle) parsed);
                    cityStyleFiles.put(file.id(), file);
                } else {
                    assets.get(type)
                        .put(file.id(), parsed);
                }
            }
        }
        checkBuildings(assets, files, problems);
        Map<String, List<Stuff>> stuffByTag = stuffIndex(assets);
        VarietyPools pools = VarietyPools
            .parse(data.pools(), registry, problems.in(BlockData.origin(VarietyPools.RESOURCE)));
        checkPools(pools, data, problems);
        PaletteVariety variety = PaletteVariety
            .parse(data.variety(), registry, resolver, lookup, problems, BlockData.origin(PaletteVariety.RESOURCE));
        Map<String, BlockChoice> code = new TreeMap<>();
        Problems.Scope codeScope = problems.in("code");
        for (Map.Entry<String, String> entry : CODE_BLOCKS.entrySet()) {
            BlockChoice choice = resolver.resolve(entry.getValue(), null, null, codeScope, entry.getKey());
            if (choice != null) {
                code.put(entry.getKey(), choice);
            }
        }
        RandomList leaves = table(code, new String[] { "leaves.jungle", "leaves.spruce", "leaves.oak" }, 20, 20, 88);
        RandomList rubble = table(
            code,
            new String[] { "rubble.mossy_stone_bricks", "rubble.mossy_cobblestone", "rubble.moss" },
            20,
            40,
            68);
        List<String> unreachable = unreachable(assets, stuffByTag);
        for (String asset : unreachable) {
            problems.in(asset)
                .note("", null, "nothing reachable from a world style or predefined city names this asset");
        }
        SortedMap<String, String> hashes = new TreeMap<>();
        for (AssetFile file : files.values()) {
            hashes.put(key(file.type(), file.id()), file.sha256());
        }
        // The palette variety changes what a world generates like an asset file does, so the hash covers it.
        hashes.put(
            VARIETY_KEY,
            AssetFile.sha256(
                data.variety()
                    .toString()));
        StringBuilder all = new StringBuilder();
        for (Map.Entry<String, String> entry : hashes.entrySet()) {
            all.append(entry.getKey())
                .append(' ')
                .append(entry.getValue())
                .append('\n');
        }
        Map<AssetType, SortedMap<AssetId, Object>> frozen = new EnumMap<>(AssetType.class);
        for (Map.Entry<AssetType, SortedMap<AssetId, Object>> entry : assets.entrySet()) {
            frozen.put(entry.getKey(), Collections.unmodifiableSortedMap(entry.getValue()));
        }
        return new AssetSet(
            Collections.unmodifiableMap(frozen),
            stuffByTag,
            Collections.unmodifiableSortedMap(hashes),
            AssetFile.sha256(all.toString()),
            Collections.unmodifiableList(new ArrayList<>(problems.all())),
            resolver.fallbacks(),
            unreachable,
            data.mapping(),
            pools,
            variety,
            Collections.unmodifiableMap(code),
            leaves,
            rubble,
            data.colors());
    }

    /** Returns the key of a file: {@code <folder>/<id>}. */
    static String key(AssetType type, AssetId id) {
        return type.folder() + "/" + id;
    }

    private static Object parse(AssetFile file, ParseContext context) {
        JsonElement json;
        try {
            json = JsonFields.parseStrict(file.text());
        } catch (JsonParseException e) {
            context.scope()
                .error("", null, "not valid JSON: " + e.getMessage());
            return null;
        }
        if (!json.isJsonObject()) {
            context.scope()
                .error("", null, "an asset file holds one JSON object");
            return null;
        }
        JsonFields root = new JsonFields(json.getAsJsonObject(), "", context.scope());
        switch (file.type()) {
            case VARIANTS:
                return Variant.parse(root, context);
            case CONDITIONS:
                return Condition.parse(root, context);
            case PALETTES:
                return Palette.parse(root, file.id(), context);
            case PARTS:
                return Part.parse(root, context);
            case BUILDINGS:
                return Building.parse(root, context);
            case MULTIBUILDINGS:
                return MultiBuilding.parse(root, context);
            case STYLES:
                return Style.parse(root, context);
            case CITYSTYLES:
                return CityStyle.parse(root, context);
            case SCATTERED:
                return Scattered.parse(root, context);
            case STUFF:
                return Stuff.parse(root, context);
            case WORLDSTYLES:
                return WorldStyle.parse(root, context);
            case PREDEFINEDCITIES:
                return PredefinedCity.parse(root, context);
            default:
                return PredefinedSphere.parse(root, context);
        }
    }

    private static void resolveCityStyles(Map<AssetId, CityStyle> raw, Map<AssetId, AssetFile> files,
        Map<AssetType, SortedMap<AssetId, Object>> assets, Map<AssetType, Set<AssetId>> rejected, Problems problems,
        ParseContext.Lookup lookup, BlockResolver resolver) {
        Map<AssetId, CityStyle> resolved = new TreeMap<>();
        Set<AssetId> failed = new HashSet<>();
        Map<CityStyle.StreetSlot, List<Part>> defaults = defaultStreetParts(problems, lookup, resolver);
        for (AssetId id : raw.keySet()) {
            resolveCityStyle(id, raw, files, resolved, failed, rejected, problems, new ArrayDeque<AssetId>());
        }
        for (Map.Entry<AssetId, CityStyle> entry : resolved.entrySet()) {
            assets.get(AssetType.CITYSTYLES)
                .put(
                    entry.getKey(),
                    entry.getValue()
                        .withDefaults(defaults));
        }
    }

    private static CityStyle resolveCityStyle(AssetId id, Map<AssetId, CityStyle> raw, Map<AssetId, AssetFile> files,
        Map<AssetId, CityStyle> resolved, Set<AssetId> failed, Map<AssetType, Set<AssetId>> rejected, Problems problems,
        Deque<AssetId> chain) {
        if (resolved.containsKey(id)) {
            return resolved.get(id);
        }
        if (failed.contains(id)) {
            return null;
        }
        CityStyle style = raw.get(id);
        Problems.Scope scope = problems.in(
            files.get(id)
                .origin());
        CityStyle parent = null;
        if (style.parent() != null) {
            if (chain.contains(style.parent()) || style.parent()
                .equals(id)) {
                scope.error(
                    "inherit",
                    style.parent()
                        .toString(),
                    "inheritance forms a cycle: " + chain);
                return fail(id, failed, rejected, scope);
            }
            if (!raw.containsKey(style.parent())) {
                String why = rejected.get(AssetType.CITYSTYLES)
                    .contains(style.parent()) ? "was rejected" : "does not exist";
                scope.error(
                    "inherit",
                    style.parent()
                        .toString(),
                    "the parent city style " + why);
                return fail(id, failed, rejected, scope);
            }
            chain.push(id);
            parent = resolveCityStyle(style.parent(), raw, files, resolved, failed, rejected, problems, chain);
            chain.pop();
            if (parent == null) {
                scope.error(
                    "inherit",
                    style.parent()
                        .toString(),
                    "the parent city style could not be resolved");
                return fail(id, failed, rejected, scope);
            }
        }
        CityStyle result = style.inherit(parent, scope);
        resolved.put(id, result);
        return result;
    }

    private static CityStyle fail(AssetId id, Set<AssetId> failed, Map<AssetType, Set<AssetId>> rejected,
        Problems.Scope scope) {
        failed.add(id);
        rejected.get(AssetType.CITYSTYLES)
            .add(id);
        scope.error("", id.toString(), "asset rejected");
        return null;
    }

    private static Map<CityStyle.StreetSlot, List<Part>> defaultStreetParts(Problems problems,
        ParseContext.Lookup lookup, BlockResolver resolver) {
        Map<CityStyle.StreetSlot, List<Part>> defaults = new EnumMap<>(CityStyle.StreetSlot.class);
        ParseContext context = new ParseContext(
            AssetId.of(AssetId.DEFAULT_NAMESPACE, "street_defaults"),
            problems.in("code"),
            resolver,
            lookup);
        for (CityStyle.StreetSlot slot : CityStyle.StreetSlot.values()) {
            Part part = context.optional(AssetType.PARTS, slot.defaultPart(), Part.class, "streetparts." + slot);
            defaults.put(slot, part == null ? Collections.<Part>emptyList() : Collections.singletonList(part));
        }
        return Collections.unmodifiableMap(defaults);
    }

    /**
     * Warns about buildings whose parts cannot match some floor: the ground floor, a middle floor, the top floor, and
     * a cellar unless the building allows none (docs/new-spec/05-assets/index.md, "Building JSON").
     */
    private static void checkBuildings(Map<AssetType, SortedMap<AssetId, Object>> assets, Map<String, AssetFile> files,
        Problems problems) {
        for (Object value : assets.get(AssetType.BUILDINGS)
            .values()) {
            Building building = (Building) value;
            int above = building.maxFloors() > 0 ? Math.min(building.maxFloors(), 3) : 3;
            int below = building.maxCellars() == 0 ? 0 : 1;
            List<Integer> floors = new ArrayList<>();
            if (below > 0) {
                floors.add(-1);
            }
            for (int floor = 0; floor <= above; floor++) {
                floors.add(floor);
            }
            for (int floor : floors) {
                ConditionContext context = ConditionContext.builder()
                    .floor(floor)
                    .floors(below, above)
                    .building(building.id())
                    .build();
                boolean matches = false;
                for (Building.PartRef ref : building.parts()) {
                    if (ref.test()
                        .test(context)) {
                        matches = true;
                        break;
                    }
                }
                if (!matches) {
                    problems.in(
                        files.get(key(AssetType.BUILDINGS, building.id()))
                            .origin())
                        .warning(
                            "parts",
                            null,
                            "no part matches floor " + floor
                                + " of a building with "
                                + below
                                + " cellar(s) and "
                                + above
                                + " floor(s) above ground");
                }
            }
        }
    }

    private static Map<String, List<Stuff>> stuffIndex(Map<AssetType, SortedMap<AssetId, Object>> assets) {
        Map<String, List<Stuff>> index = new TreeMap<>();
        // The map is sorted by id, so every list is in id order (fixes ASSET-15's timing and SCAT-22's hash order).
        for (Object value : assets.get(AssetType.STUFF)
            .values()) {
            Stuff stuff = (Stuff) value;
            for (String tag : stuff.tags()) {
                index.computeIfAbsent(tag, k -> new ArrayList<>())
                    .add(stuff);
            }
        }
        Map<String, List<Stuff>> frozen = new TreeMap<>();
        for (Map.Entry<String, List<Stuff>> entry : index.entrySet()) {
            frozen.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
        }
        return Collections.unmodifiableMap(frozen);
    }

    private static void checkPools(VarietyPools pools, BlockData data, Problems problems) {
        Problems.Scope scope = problems.in(BlockData.origin(VarietyPools.RESOURCE));
        for (VarietyPools.Pool pool : pools.pools()
            .values()) {
            boolean treasure = "treasure".equals(pool.name());
            for (VarietyPools.Candidate candidate : pool.candidates()) {
                for (BlockRef block : candidate.blocks()) {
                    String reason = data.neverPlace()
                        .reason(block, treasure);
                    if (reason != null) {
                        scope.warning(
                            pool.name(),
                            block.toString(),
                            "a pool names a never-placed block (" + reason + ")");
                    }
                }
            }
        }
    }

    private static RandomList table(Map<String, BlockChoice> code, String[] keys, int... weights) {
        List<Integer> slots = new ArrayList<>();
        List<BlockChoice> blocks = new ArrayList<>();
        for (int i = 0; i < keys.length; i++) {
            BlockChoice choice = code.get(keys[i]);
            if (choice == null) {
                return null;
            }
            slots.add(weights[i]);
            blocks.add(choice);
        }
        return RandomList.fill(slots, blocks);
    }

    /** Returns every asset nothing reachable names, as {@code <folder>/<id>}, sorted. */
    static List<String> unreachable(Map<AssetType, SortedMap<AssetId, Object>> assets,
        Map<String, List<Stuff>> stuffByTag) {
        Set<String> reached = new HashSet<>();
        Deque<Object> work = new ArrayDeque<>();
        work.addAll(
            assets.get(AssetType.WORLDSTYLES)
                .values());
        work.addAll(
            assets.get(AssetType.PREDEFINEDCITIES)
                .values());
        work.addAll(
            assets.get(AssetType.PREDEFINEDSPHERES)
                .values());
        while (!work.isEmpty()) {
            Object asset = work.pop();
            String key = keyOf(asset);
            if (key == null || !reached.add(key)) {
                continue;
            }
            work.addAll(children(asset, assets, stuffByTag));
        }
        List<String> out = new ArrayList<>();
        for (Map.Entry<AssetType, SortedMap<AssetId, Object>> entry : assets.entrySet()) {
            for (AssetId id : entry.getValue()
                .keySet()) {
                String key = key(entry.getKey(), id);
                if (!reached.contains(key)) {
                    out.add(key);
                }
            }
        }
        Collections.sort(out);
        return Collections.unmodifiableList(out);
    }

    private static String keyOf(Object asset) {
        if (asset instanceof WorldStyle) {
            return key(AssetType.WORLDSTYLES, ((WorldStyle) asset).id());
        } else if (asset instanceof CityStyle) {
            return key(AssetType.CITYSTYLES, ((CityStyle) asset).id());
        } else if (asset instanceof Style) {
            return key(AssetType.STYLES, ((Style) asset).id());
        } else if (asset instanceof Palette) {
            return key(AssetType.PALETTES, ((Palette) asset).id());
        } else if (asset instanceof Variant) {
            return key(AssetType.VARIANTS, ((Variant) asset).id());
        } else if (asset instanceof Condition) {
            return key(AssetType.CONDITIONS, ((Condition) asset).id());
        } else if (asset instanceof Part) {
            return key(AssetType.PARTS, ((Part) asset).id());
        } else if (asset instanceof Building) {
            return key(AssetType.BUILDINGS, ((Building) asset).id());
        } else if (asset instanceof MultiBuilding) {
            return key(AssetType.MULTIBUILDINGS, ((MultiBuilding) asset).id());
        } else if (asset instanceof Scattered) {
            return key(AssetType.SCATTERED, ((Scattered) asset).id());
        } else if (asset instanceof Stuff) {
            return key(AssetType.STUFF, ((Stuff) asset).id());
        } else if (asset instanceof PredefinedCity) {
            return key(AssetType.PREDEFINEDCITIES, ((PredefinedCity) asset).id());
        } else if (asset instanceof PredefinedSphere) {
            return key(AssetType.PREDEFINEDSPHERES, ((PredefinedSphere) asset).id());
        }
        return null;
    }

    private static List<Object> children(Object asset, Map<AssetType, SortedMap<AssetId, Object>> assets,
        Map<String, List<Stuff>> stuffByTag) {
        List<Object> out = new ArrayList<>();
        if (asset instanceof WorldStyle) {
            WorldStyle world = (WorldStyle) asset;
            out.add(world.outsideStyle());
            for (WorldStyle.CityStyleChoice choice : world.cityStyles()) {
                out.add(choice.cityStyle());
            }
            for (WorldStyle.ScatteredRef ref : world.scattered()) {
                out.add(ref.scattered());
            }
            for (List<Part> parts : world.parts()
                .values()) {
                out.addAll(parts);
            }
            addIfPresent(out, world.bridgeSupportPart(), world.highwaySupportPart());
        } else if (asset instanceof CityStyle) {
            CityStyle city = (CityStyle) asset;
            addIfPresent(out, city.style(), city.bridgeSupportPart());
            if (city.parent() != null) {
                addIfPresent(
                    out,
                    assets.get(AssetType.CITYSTYLES)
                        .get(city.parent()));
            }
            for (CityStyle.SelectorKind kind : CityStyle.SelectorKind.values()) {
                for (Selector selector : city.selectors(kind)) {
                    out.add(selector.target());
                }
            }
            for (CityStyle.StreetSet set : CityStyle.StreetSet.values()) {
                for (CityStyle.StreetSlot slot : CityStyle.StreetSlot.values()) {
                    out.addAll(city.streetParts(set, slot));
                }
            }
            for (String tag : city.stuffTags()) {
                List<Stuff> stuff = stuffByTag.get(tag);
                if (stuff != null) {
                    out.addAll(stuff);
                }
            }
        } else if (asset instanceof Style) {
            for (List<Style.Choice> group : ((Style) asset).groups()) {
                for (Style.Choice choice : group) {
                    out.add(choice.palette());
                }
            }
        } else if (asset instanceof Palette) {
            addPalette(out, (Palette) asset, assets);
        } else if (asset instanceof Part) {
            Palette palette = ((Part) asset).palette();
            if (palette != null) {
                out.add(palette);
                addPalette(out, palette, assets);
            }
        } else if (asset instanceof Building) {
            Building building = (Building) asset;
            if (building.palette() != null) {
                out.add(building.palette());
                addPalette(out, building.palette(), assets);
            }
            for (Building.PartRef ref : building.parts()) {
                out.add(ref.part());
            }
            for (Building.PartRef ref : building.parts2()) {
                out.add(ref.part());
            }
        } else if (asset instanceof MultiBuilding) {
            out.addAll(((MultiBuilding) asset).buildings());
        } else if (asset instanceof Scattered) {
            Scattered scattered = (Scattered) asset;
            out.addAll(scattered.buildings());
            addIfPresent(out, scattered.multiBuilding(), scattered.supportPart());
        } else if (asset instanceof PredefinedCity) {
            PredefinedCity city = (PredefinedCity) asset;
            out.add(city.cityStyle());
            for (PredefinedCity.FixedBuilding fixed : city.buildings()) {
                addIfPresent(
                    out,
                    assets.get(fixed.multi() ? AssetType.MULTIBUILDINGS : AssetType.BUILDINGS)
                        .get(fixed.building()));
            }
        }
        return out;
    }

    private static void addPalette(List<Object> out, Palette palette,
        Map<AssetType, SortedMap<AssetId, Object>> assets) {
        for (PaletteEntry entry : palette.entries()
            .values()) {
            addIfPresent(out, entry.loot(), entry.mob());
        }
        // Variants are stored by value in the entries; reach them by matching their lists.
        for (Object value : assets.get(AssetType.VARIANTS)
            .values()) {
            Variant variant = (Variant) value;
            for (PaletteEntry entry : palette.entries()
                .values()) {
                if (entry.random() == variant.blocks()) {
                    out.add(variant);
                }
            }
        }
    }

    private static void addIfPresent(List<Object> out, Object... values) {
        for (Object value : values) {
            if (value != null) {
                out.add(value);
            }
        }
    }

    private static Map<String, String> codeBlocks() {
        Map<String, String> out = new LinkedHashMap<>();
        for (String wood : Arrays.asList("oak", "birch", "acacia", "dark_oak", "spruce", "jungle", "iron")) {
            out.put("door." + wood, "minecraft:" + wood + "_door");
        }
        out.put("iron_bars", "minecraft:iron_bars");
        out.put("glowstone", "minecraft:glowstone");
        out.put("rail", "minecraft:rail");
        out.put("powered_rail", "minecraft:powered_rail[powered=true]");
        out.put("dirt", "minecraft:dirt");
        out.put("grass", "minecraft:grass_block");
        out.put("water", "minecraft:water");
        out.put("torch", "minecraft:torch");
        out.put("wall_torch", "minecraft:wall_torch");
        out.put("vine", "minecraft:vine");
        out.put("leaves.oak", "minecraft:oak_leaves[persistent=true]");
        out.put("leaves.spruce", "minecraft:spruce_leaves[persistent=true]");
        out.put("leaves.jungle", "minecraft:jungle_leaves[persistent=true]");
        out.put("rubble.mossy_stone_bricks", "minecraft:mossy_stone_bricks");
        out.put("rubble.mossy_cobblestone", "minecraft:mossy_cobblestone");
        out.put("rubble.moss", "minecraft:moss_block");
        // Edge lots (docs/design/0009-city-edges-and-character.md §3): clay and dirt ground nothing grows on, concrete.
        out.put("lot.coarse_dirt", "minecraft:dirt:1");
        out.put("lot.terracotta", "minecraft:hardened_clay:0");
        out.put("lot.brown_terracotta", "minecraft:stained_hardened_clay:12");
        out.put("lot.red_terracotta", "minecraft:stained_hardened_clay:14");
        out.put("lot.gravel", "minecraft:gravel:0");
        out.put("lot.concrete", "minecraft:light_gray_concrete");
        out.put("lot.fence", "minecraft:fence:0");
        // Repairs (0009 §4): boards for windows, and plain materials a patch is chosen from by color.
        out.put("repair.boards.oak", "minecraft:planks:0");
        out.put("repair.boards.spruce", "minecraft:planks:1");
        out.put("repair.boards.birch", "minecraft:planks:2");
        String[] plain = { "minecraft:cobblestone:0", "minecraft:stonebrick:0", "minecraft:brick_block:0",
            "minecraft:hardened_clay:0", "minecraft:sandstone:0", "minecraft:sandstone:2",
            "minecraft:double_stone_slab:8", "minecraft:quartz_block:0" };
        for (String block : plain) {
            out.put("repair.material." + block.substring("minecraft:".length()), block);
        }
        for (int color = 0; color < 16; color++) {
            out.put("repair.material.stained_hardened_clay:" + color, "minecraft:stained_hardened_clay:" + color);
        }
        for (String color : Arrays.asList(
            "white",
            "orange",
            "magenta",
            "light_blue",
            "yellow",
            "lime",
            "pink",
            "gray",
            "light_gray",
            "cyan",
            "purple",
            "blue",
            "brown",
            "green",
            "red",
            "black")) {
            out.put("repair.material.concrete." + color, "minecraft:" + color + "_concrete");
        }
        return Collections.unmodifiableMap(out);
    }
}
