package com.tuxprogrammer.lostcities.worldgen.city;

import java.util.Collections;
import java.util.LinkedHashSet;
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
        CityStyle cityStyle = assets.cityStyle(CityPalettes.id(plan.cityStyle()));
        Building building = plan.building() == null ? null : assets.building(CityPalettes.id(plan.building()));
        return palettes.styleBlocks(chunkPalette(plan), cityStyle, building);
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
