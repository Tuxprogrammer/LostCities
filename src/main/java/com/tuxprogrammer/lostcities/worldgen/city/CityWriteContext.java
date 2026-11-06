package com.tuxprogrammer.lostcities.worldgen.city;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.match.BiomeSource;
import com.tuxprogrammer.lostcities.asset.model.Building;
import com.tuxprogrammer.lostcities.asset.model.CityStyle;
import com.tuxprogrammer.lostcities.asset.model.WorldStyle;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.LayoutLog;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.worldgen.plan.StyleBlocks;
import com.tuxprogrammer.lostcities.worldgen.terrain.BlockClassifier;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainSettings;

/**
 * What every city chunk of one world is written from: the layout planner, the loaded assets and the world style, the
 * palette caches, the pinned settings, the world seed, the chunk biomes and the block classes, with a log that reports
 * each problem once. One per wrapped world, owned by its {@link LayoutCityPlan}.
 *
 * <p>
 * Not thread-safe; world generation runs on the server thread. Side: both (plain Java).
 */
public final class CityWriteContext {

    /** Problems reported per world before the log goes quiet, so a broken asset cannot flood the log. */
    static final int MAX_REPORTED = 256;

    private final long worldSeed;
    private final LayoutPlanner planner;
    private final AssetSet assets;
    private final WorldStyle worldStyle;
    private final CityPalettes palettes;
    private final TerrainSettings settings;
    private final BiomeSource biomes;
    private final BlockClassifier classifier;
    private final LayoutLog log;
    private PartRecorder recorder = PartRecorder.NONE;
    private java.util.function.Predicate<String> knownBlocks = name -> true;
    /**
     * City styles and buildings by the names plans carry, resolved once each (D266); {@link #UNRESOLVED} for a name
     * the assets lack. Bounded by the loaded assets' names.
     */
    private final Map<String, Object> styles = new HashMap<>();
    private final Map<String, Object> buildings = new HashMap<>();

    /** A name that resolves to no asset. */
    private static final Object UNRESOLVED = new Object();

    /**
     * Creates the context of one world.
     *
     * @param worldSeed  the world seed
     * @param planner    the world's layout planner
     * @param assets     the loaded assets
     * @param worldStyle the profile's world style
     * @param palettes   the world's palette caches
     * @param settings   the pinned settings
     * @param biomes     the chunk biomes, for conditions and lane materials
     * @param classifier the block classes
     * @param sink       where each distinct problem is reported once
     */
    public CityWriteContext(long worldSeed, LayoutPlanner planner, AssetSet assets, WorldStyle worldStyle,
        CityPalettes palettes, TerrainSettings settings, BiomeSource biomes, BlockClassifier classifier,
        LayoutLog sink) {
        this.worldSeed = worldSeed;
        this.planner = planner;
        this.assets = assets;
        this.worldStyle = worldStyle;
        this.palettes = palettes;
        this.settings = settings;
        this.biomes = biomes;
        this.classifier = classifier;
        this.log = once(sink);
    }

    /** Wraps a sink so each distinct message is reported once, up to {@link #MAX_REPORTED}. */
    private static LayoutLog once(LayoutLog sink) {
        Set<String> seen = Collections.synchronizedSet(new LinkedHashSet<>());
        return message -> {
            if (seen.size() < MAX_REPORTED && seen.add(message)) {
                sink.warn(message);
            }
        };
    }

    /** Returns the world seed. */
    public long worldSeed() {
        return worldSeed;
    }

    /** Returns the layout planner. */
    public LayoutPlanner planner() {
        return planner;
    }

    /** Returns the loaded assets. */
    public AssetSet assets() {
        return assets;
    }

    /** Returns the profile's world style, or {@code null} when it is not loaded. */
    public WorldStyle worldStyle() {
        return worldStyle;
    }

    /** Returns the palette caches. */
    public CityPalettes palettes() {
        return palettes;
    }

    /** Returns the pinned settings. */
    public TerrainSettings settings() {
        return settings;
    }

    /** Returns the chunk biomes. */
    public BiomeSource biomes() {
        return biomes;
    }

    /** Returns the deduplicating log. */
    public LayoutLog log() {
        return log;
    }

    /** Returns where written parts are recorded: {@link PartRecorder#NONE} unless the world has {@code editMode}. */
    public PartRecorder recorder() {
        return recorder;
    }

    /**
     * Sets where written parts are recorded; the overlay sets it once, when it builds an {@code editMode} world.
     *
     * @param value the recorder, never {@code null}
     */
    public void recorder(PartRecorder value) {
        recorder = java.util.Objects.requireNonNull(value);
    }

    /**
     * Returns whether a block registry name is registered, for the furnish pass's soft resolution (design record
     * 0016): every name until the overlay sets the live registry's test.
     */
    public java.util.function.Predicate<String> knownBlocks() {
        return knownBlocks;
    }

    /**
     * Sets the test of registered block names; the overlay sets it once, from the live registry.
     *
     * @param value the test, never {@code null}
     */
    public void knownBlocks(java.util.function.Predicate<String> value) {
        knownBlocks = java.util.Objects.requireNonNull(value);
    }

    /**
     * Returns a chunk's palette: its palette choices and its building's palette.
     *
     * @param plan the chunk's layout plan
     * @return the palette
     */
    public CityPalettes.ChunkPalette chunkPalette(ChunkPlan plan) {
        return palettes.chunk(plan.palettes(), plan.building());
    }

    /**
     * Returns a chunk's terrain blocks.
     *
     * @param plan the chunk's layout plan
     * @return the style blocks
     */
    public StyleBlocks styleBlocks(ChunkPlan plan) {
        return styleBlocks(plan, chunkPalette(plan));
    }

    /**
     * Returns a chunk's terrain blocks from its palette, when the caller already has it ({@link #chunkPalette}).
     *
     * @param plan  the chunk's layout plan
     * @param chunk the chunk's palette
     * @return the style blocks
     */
    public StyleBlocks styleBlocks(ChunkPlan plan, CityPalettes.ChunkPalette chunk) {
        return palettes.styleBlocks(chunk, cityStyle(plan.cityStyle()), building(plan.building()));
    }

    /**
     * Returns the city style a plan names, resolved once per name.
     *
     * @param name the style's asset name, or {@code null}
     * @return the city style, or {@code null} when the assets lack it
     */
    public CityStyle cityStyle(String name) {
        if (name == null) {
            return assets.cityStyle(null);
        }
        Object found = styles.get(name);
        if (found == null) {
            CityStyle style = assets.cityStyle(CityPalettes.id(name));
            found = style == null ? UNRESOLVED : style;
            styles.put(name, found);
        }
        return found == UNRESOLVED ? null : (CityStyle) found;
    }

    /** Returns the building a plan names, or {@code null}. */
    private Building building(String name) {
        if (name == null) {
            return null;
        }
        Object found = buildings.get(name);
        if (found == null) {
            Building building = assets.building(CityPalettes.id(name));
            found = building == null ? UNRESOLVED : building;
            buildings.put(name, found);
        }
        return found == UNRESOLVED ? null : (Building) found;
    }

    /**
     * Returns the part-writing rules of a chunk.
     *
     * @param plan the chunk's layout plan
     * @return the rules
     */
    public PartWriter.Rules rules(ChunkPlan plan) {
        return rules(plan, styleBlocks(plan));
    }

    /**
     * Returns the part-writing rules of a chunk whose style blocks the caller already has.
     *
     * @param plan  the chunk's layout plan
     * @param style the chunk's style blocks
     * @return the rules
     */
    public PartWriter.Rules rules(ChunkPlan plan, StyleBlocks style) {
        return new PartWriter.Rules(
            settings.generateLighting(),
            settings.generateSpawners(),
            settings.avoidWater(),
            plan.settings() != null && plan.settings()
                .avoidFoliage(),
            plan.noLoot(),
            settings.waterLevel(),
            style.liquid());
    }

    /** Returns upstream's {@code isEmpty}: air or liquid. */
    public boolean isEmpty(BlockRef block) {
        return classifier.isEmpty(block);
    }

    /**
     * Returns whether a block is a log, leaf, plant or vine: what the clearing above a highway leaves standing
     * (upstream keeps leaves and logs; new-spec 06, the clearing row).
     *
     * @param block the block
     * @return whether it is foliage
     */
    public boolean isFoliage(BlockRef block) {
        return classifier.isFoliage(block);
    }

    /** Returns whether a block is water or lava, still or flowing. */
    public boolean isLiquid(BlockRef block) {
        return classifier.isLiquid(block);
    }
}
