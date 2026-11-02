package com.tuxprogrammer.lostcities.asset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.tuxprogrammer.lostcities.asset.AssetProblem.Severity;
import com.tuxprogrammer.lostcities.asset.block.BlockChoice;
import com.tuxprogrammer.lostcities.asset.block.BlockMapping;
import com.tuxprogrammer.lostcities.asset.block.BlockResolver;
import com.tuxprogrammer.lostcities.asset.block.VarietyPools;
import com.tuxprogrammer.lostcities.asset.model.Building;
import com.tuxprogrammer.lostcities.asset.model.CityStyle;
import com.tuxprogrammer.lostcities.asset.model.Condition;
import com.tuxprogrammer.lostcities.asset.model.MultiBuilding;
import com.tuxprogrammer.lostcities.asset.model.Palette;
import com.tuxprogrammer.lostcities.asset.model.Part;
import com.tuxprogrammer.lostcities.asset.model.PredefinedCity;
import com.tuxprogrammer.lostcities.asset.model.PredefinedSphere;
import com.tuxprogrammer.lostcities.asset.model.RandomList;
import com.tuxprogrammer.lostcities.asset.model.Scattered;
import com.tuxprogrammer.lostcities.asset.model.Stuff;
import com.tuxprogrammer.lostcities.asset.model.Style;
import com.tuxprogrammer.lostcities.asset.model.Variant;
import com.tuxprogrammer.lostcities.asset.model.WorldStyle;
import com.tuxprogrammer.lostcities.asset.variety.PaletteVariety;

/**
 * The loaded asset set: every valid asset of the thirteen types by full id, the indexes built at load, the block data,
 * and what loading found (docs/new-spec/05-assets/asset-format.md, "Loading and override"). It is complete and
 * immutable before any chunk generates; lookups never throw and never compile anything (fixes ASSET-01, ASSET-14 and
 * ASSET-15).
 *
 * <p>
 * This is the asset API that {@code layout} and {@code worldgen} consume: typed lookups by {@link AssetId}, the stuff
 * index, code-named blocks, the variety pools, the block-name map's rotation families, the asset-set hash a world pins
 * and the report of problems and fallbacks. Side: both (built on the server side at {@code postInit}).
 */
public final class AssetSet {

    private final Map<AssetType, SortedMap<AssetId, Object>> assets;
    private final Map<String, List<Stuff>> stuffByTag;
    private final SortedMap<String, String> fileHashes;
    private final String hash;
    private final List<AssetProblem> problems;
    private final List<BlockResolver.Fallback> fallbacks;
    private final List<String> unreachable;
    private final BlockMapping mapping;
    private final VarietyPools pools;
    private final PaletteVariety variety;
    private final Map<String, BlockChoice> codeBlocks;
    private final RandomList defaultLeaves;
    private final RandomList defaultRubble;

    AssetSet(Map<AssetType, SortedMap<AssetId, Object>> assets, Map<String, List<Stuff>> stuffByTag,
        SortedMap<String, String> fileHashes, String hash, List<AssetProblem> problems,
        List<BlockResolver.Fallback> fallbacks, List<String> unreachable, BlockMapping mapping, VarietyPools pools,
        PaletteVariety variety, Map<String, BlockChoice> codeBlocks, RandomList defaultLeaves,
        RandomList defaultRubble) {
        this.assets = assets;
        this.stuffByTag = stuffByTag;
        this.fileHashes = fileHashes;
        this.hash = hash;
        this.problems = problems;
        this.fallbacks = fallbacks;
        this.unreachable = unreachable;
        this.mapping = mapping;
        this.pools = pools;
        this.variety = variety;
        this.codeBlocks = codeBlocks;
        this.defaultLeaves = defaultLeaves;
        this.defaultRubble = defaultRubble;
    }

    /**
     * Returns an asset by type and id.
     *
     * @param type the type
     * @param id   the full id
     * @return the asset, or null when no valid asset has that id
     */
    public Object get(AssetType type, AssetId id) {
        return assets.get(type)
            .get(id);
    }

    /** Returns the ids of every valid asset of a type, sorted. */
    public Collection<AssetId> ids(AssetType type) {
        return Collections.unmodifiableSet(
            assets.get(type)
                .keySet());
    }

    /** Returns how many valid assets of a type were loaded. */
    public int count(AssetType type) {
        return assets.get(type)
            .size();
    }

    /** Returns how many valid assets were loaded in all. */
    public int count() {
        int total = 0;
        for (SortedMap<AssetId, Object> map : assets.values()) {
            total += map.size();
        }
        return total;
    }

    /** Returns a variant, or null. */
    public Variant variant(AssetId id) {
        return (Variant) get(AssetType.VARIANTS, id);
    }

    /** Returns a condition, or null. */
    public Condition condition(AssetId id) {
        return (Condition) get(AssetType.CONDITIONS, id);
    }

    /** Returns a palette, or null: a palette asset, or an available palette-variety option. */
    public Palette palette(AssetId id) {
        Palette palette = (Palette) get(AssetType.PALETTES, id);
        return palette != null ? palette : variety.palette(id);
    }

    /** Returns the palette variety drawn on top of the styles' palettes. */
    public PaletteVariety variety() {
        return variety;
    }

    /** Returns a part, or null. */
    public Part part(AssetId id) {
        return (Part) get(AssetType.PARTS, id);
    }

    /** Returns a building, or null. */
    public Building building(AssetId id) {
        return (Building) get(AssetType.BUILDINGS, id);
    }

    /** Returns a multibuilding, or null. */
    public MultiBuilding multiBuilding(AssetId id) {
        return (MultiBuilding) get(AssetType.MULTIBUILDINGS, id);
    }

    /** Returns a style, or null. */
    public Style style(AssetId id) {
        return (Style) get(AssetType.STYLES, id);
    }

    /** Returns a city style with its inheritance resolved, or null. */
    public CityStyle cityStyle(AssetId id) {
        return (CityStyle) get(AssetType.CITYSTYLES, id);
    }

    /** Returns a scattered structure, or null. */
    public Scattered scattered(AssetId id) {
        return (Scattered) get(AssetType.SCATTERED, id);
    }

    /** Returns a stuff asset, or null. */
    public Stuff stuff(AssetId id) {
        return (Stuff) get(AssetType.STUFF, id);
    }

    /** Returns a world style, or null. */
    public WorldStyle worldStyle(AssetId id) {
        return (WorldStyle) get(AssetType.WORLDSTYLES, id);
    }

    /** Returns every predefined city, sorted by id. */
    public List<PredefinedCity> predefinedCities() {
        return values(AssetType.PREDEFINEDCITIES, PredefinedCity.class);
    }

    /** Returns every predefined sphere, sorted by id; kept, not used before beta (D12). */
    public List<PredefinedSphere> predefinedSpheres() {
        return values(AssetType.PREDEFINEDSPHERES, PredefinedSphere.class);
    }

    /**
     * Returns the stuff assets with a tag, sorted by id, built at load (fixes ASSET-15 and SCAT-22).
     *
     * @param tag the tag
     * @return the stuff; empty for an unknown tag
     */
    public List<Stuff> stuffByTag(String tag) {
        List<Stuff> list = stuffByTag.get(tag);
        return list == null ? Collections.<Stuff>emptyList() : list;
    }

    /**
     * Returns a block upstream's code names, resolved through the block-name map at load: random doors, highway and
     * corridor details, default leaves and rubble, park grass, torches and vines
     * (docs/new-spec/00-pack-integration/block-mapping.md, "Blocks upstream's code names").
     *
     * @param key one of {@link AssetLoader#CODE_BLOCKS}' keys, such as {@code door.birch} or {@code rubble.moss}
     * @return the choice, or null for an unknown key
     */
    public BlockChoice codeBlock(String key) {
        return codeBlocks.get(key);
    }

    /** Returns every code-named block by key, sorted. */
    public Map<String, BlockChoice> codeBlocks() {
        return codeBlocks;
    }

    /** Returns upstream's default leaf table: 20 jungle, 20 spruce, 88 oak, all persistent. */
    public RandomList defaultLeaves() {
        return defaultLeaves;
    }

    /** Returns upstream's default rubble table: 20 mossy stone bricks, 40 mossy cobblestone, 68 moss (D31). */
    public RandomList defaultRubble() {
        return defaultRubble;
    }

    /** Returns the variety pools, resolved against the registry. */
    public VarietyPools pools() {
        return pools;
    }

    /** Returns the block-name map, for rotation families and reports. */
    public BlockMapping mapping() {
        return mapping;
    }

    /** Returns the SHA-256 of every effective asset file, keyed {@code <folder>/<id>}, sorted. */
    public SortedMap<String, String> fileHashes() {
        return fileHashes;
    }

    /**
     * Returns the asset-set hash: one SHA-256 over every effective file's key and hash, in sorted order. A world pins
     * it at creation; when the loaded hash differs, one warning names the changed files and the world carries on
     * (P05-3, D31).
     *
     * @return the hash, lower-case hex
     */
    public String hash() {
        return hash;
    }

    /**
     * Returns the files that differ from a pinned snapshot, for the one changed-assets warning (P05-3).
     *
     * @param pinned the snapshot's {@link #fileHashes()}
     * @return the keys added, removed or changed, sorted
     */
    public List<String> changedFiles(Map<String, String> pinned) {
        List<String> out = new ArrayList<>();
        java.util.SortedSet<String> keys = new java.util.TreeSet<>(fileHashes.keySet());
        keys.addAll(pinned.keySet());
        for (String key : keys) {
            String now = fileHashes.get(key);
            String then = pinned.get(key);
            if (now == null || !now.equals(then)) {
                out.add(key);
            }
        }
        return out;
    }

    /** Returns every problem found while loading, in the order found. */
    public List<AssetProblem> problems() {
        return problems;
    }

    /** Returns the problems of one severity. */
    public List<AssetProblem> problems(Severity severity) {
        List<AssetProblem> out = new ArrayList<>();
        for (AssetProblem problem : problems) {
            if (problem.severity() == severity) {
                out.add(problem);
            }
        }
        return out;
    }

    /** Returns every fallback block taken because a primary block's mod is absent. */
    public List<BlockResolver.Fallback> fallbacks() {
        return fallbacks;
    }

    /** Returns the assets nothing reachable from a world style or predefined city names, as {@code folder/id}. */
    public List<String> unreachable() {
        return unreachable;
    }

    /** Returns one summary line for the log. */
    public String summary() {
        Map<AssetType, Integer> counts = new EnumMap<>(AssetType.class);
        for (AssetType type : AssetType.values()) {
            counts.put(type, count(type));
        }
        return count() + " assets "
            + counts
            + ", "
            + problems(Severity.ERROR).size()
            + " errors, "
            + problems(Severity.WARNING).size()
            + " warnings, "
            + fallbacks.size()
            + " fallbacks, "
            + pools.unresolved()
                .size()
            + " pool blocks unresolved, hash "
            + hash.substring(0, 12);
    }

    private <T> List<T> values(AssetType type, Class<T> kind) {
        List<T> out = new ArrayList<>();
        for (Object value : assets.get(type)
            .values()) {
            out.add(kind.cast(value));
        }
        return Collections.unmodifiableList(out);
    }

    /** Returns an empty map per type, for the loader. */
    static Map<AssetType, SortedMap<AssetId, Object>> emptyMaps() {
        Map<AssetType, SortedMap<AssetId, Object>> maps = new EnumMap<>(AssetType.class);
        for (AssetType type : AssetType.values()) {
            maps.put(type, new TreeMap<AssetId, Object>());
        }
        return maps;
    }
}
