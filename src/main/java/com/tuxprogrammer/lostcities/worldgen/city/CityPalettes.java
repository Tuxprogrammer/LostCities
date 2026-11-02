package com.tuxprogrammer.lostcities.worldgen.city;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/LostCityTerrainFeature.java
// Avoids: ASSET-26

import java.util.ArrayList;
import java.util.List;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.block.BlockChoice;
import com.tuxprogrammer.lostcities.asset.block.MetalFlags;
import com.tuxprogrammer.lostcities.asset.model.Building;
import com.tuxprogrammer.lostcities.asset.model.CityStyle;
import com.tuxprogrammer.lostcities.asset.model.Palette;
import com.tuxprogrammer.lostcities.asset.model.Part;
import com.tuxprogrammer.lostcities.asset.model.RandomList;
import com.tuxprogrammer.lostcities.asset.palette.CompiledPalette;
import com.tuxprogrammer.lostcities.worldgen.plan.StyleBlocks;

/**
 * The compiled palettes of one world's cities, behind bounded caches: a chunk's palette (its style's palette choices,
 * then its building's palette, upstream's {@code BuildingInfo.getCompiledPalette}), a part's palette over it
 * ({@code computePalette}), and the terrain blocks the overlay reads from them ({@link StyleBlocks}).
 *
 * <p>
 * Avoids ASSET-26: upstream compiled a new palette on every placement of a part with its own palette; here each
 * (chunk palette, part) pair is compiled once and kept in a bounded least-recently-used cache, as are chunk palettes
 * and style blocks. Palettes are compiled for the world's pinned decorative-metal flags (D61), so a lookup is one array
 * read.
 *
 * <p>
 * Not thread-safe; world generation runs on the server thread. Side: both (plain Java).
 */
public final class CityPalettes {

    /** Chunk palettes kept: distinct palette choices times buildings stay well under this in one region. */
    public static final int CHUNK_CAPACITY = 128;
    /** Part palettes kept: parts with their own palette over the chunk palettes in use. */
    public static final int PART_CAPACITY = 128;
    /** Style blocks kept, one per chunk palette and city style. */
    public static final int STYLE_CAPACITY = 128;

    /** A compiled chunk palette with the key it is cached under. */
    public static final class ChunkPalette {

        private final String key;
        private final CompiledPalette palette;

        ChunkPalette(String key, CompiledPalette palette) {
            this.key = key;
            this.palette = palette;
        }

        /** Returns the cache key: the palette names and the building, in order. */
        public String key() {
            return key;
        }

        /** Returns the compiled palette. */
        public CompiledPalette palette() {
            return palette;
        }
    }

    private final AssetSet assets;
    private final MetalFlags flags;
    private final LruCache<String, ChunkPalette> chunks = new LruCache<>(CHUNK_CAPACITY);
    private final LruCache<String, CompiledPalette> parts = new LruCache<>(PART_CAPACITY);
    private final LruCache<String, StyleBlocks> styles = new LruCache<>(STYLE_CAPACITY);

    /**
     * Creates the caches of one world.
     *
     * @param assets the loaded assets
     * @param flags  the world's pinned decorative-metal flags
     */
    public CityPalettes(AssetSet assets, MetalFlags flags) {
        this.assets = assets;
        this.flags = flags;
    }

    /** Returns the decorative-metal flags palettes are compiled for. */
    public MetalFlags flags() {
        return flags;
    }

    /**
     * Returns a chunk's palette: its palette choices in order, then its building's palette.
     *
     * @param paletteNames the palette names the layout chose, one per style group
     * @param building     the building's name, or {@code null}
     * @return the palette
     */
    public ChunkPalette chunk(List<String> paletteNames, String building) {
        StringBuilder key = new StringBuilder();
        for (String name : paletteNames) {
            key.append(name)
                .append(',');
        }
        key.append('|')
            .append(building == null ? "" : building);
        return chunks.get(key.toString(), k -> compile(paletteNames, building, k));
    }

    private ChunkPalette compile(List<String> paletteNames, String building, String key) {
        List<Palette> layers = new ArrayList<>();
        for (String name : paletteNames) {
            Palette palette = assets.palette(id(name));
            if (palette != null) {
                layers.add(palette);
            }
        }
        Building model = building == null ? null : assets.building(id(building));
        if (model != null && model.palette() != null) {
            layers.add(model.palette());
        }
        return new ChunkPalette(key, CompiledPalette.compile(flags, layers));
    }

    /**
     * Returns the palette a part is written with: the chunk palette with the part's own palette on top, compiled once
     * per pair (fixes ASSET-26).
     *
     * @param chunk the chunk palette
     * @param part  the part
     * @return the chunk palette itself when the part has no palette
     */
    public CompiledPalette forPart(ChunkPalette chunk, Part part) {
        if (part.palette() == null) {
            return chunk.palette();
        }
        return parts.get(
            chunk.key() + "|"
                + part.id(),
            k -> chunk.palette()
                .withLayer(part.palette()));
    }

    /**
     * Returns the terrain blocks of a chunk: the city style's border, wall, iron bars, leaves and rubble characters and
     * the building's filler and rubble, resolved through the chunk palette; vanilla defaults where a character is
     * unset or undefined.
     *
     * @param chunk     the chunk palette
     * @param cityStyle the city style, or {@code null}
     * @param building  the building, or {@code null}
     * @return the style blocks
     */
    public StyleBlocks styleBlocks(ChunkPalette chunk, CityStyle cityStyle, Building building) {
        String key = chunk.key() + "|" + (cityStyle == null ? "" : cityStyle.id());
        return styles.get(key, k -> buildStyle(chunk.palette(), cityStyle, building));
    }

    private StyleBlocks buildStyle(CompiledPalette palette, CityStyle cityStyle, Building building) {
        StyleBlocks.Builder out = StyleBlocks.builder();
        BlockRef border = blockOf(palette, setting(cityStyle, CityStyle.Setting.STREET_BORDER));
        if (border != null) {
            out.border(border);
        }
        BlockRef wall = blockOf(palette, setting(cityStyle, CityStyle.Setting.STREET_WALL));
        if (wall != null) {
            out.wall(wall);
        }
        BlockRef bars = blockOf(palette, setting(cityStyle, CityStyle.Setting.IRON_BARS));
        if (bars == null) {
            bars = code("iron_bars");
        }
        if (bars != null) {
            out.ironBars(bars);
        }
        List<BlockRef> leaves = slots(palette, setting(cityStyle, CityStyle.Setting.LEAVES), assets.defaultLeaves());
        if (!leaves.isEmpty()) {
            out.leaves(leaves);
        }
        List<BlockRef> rubbleDirt = slots(
            palette,
            setting(cityStyle, CityStyle.Setting.RUBBLE_DIRT),
            assets.defaultRubble());
        if (!rubbleDirt.isEmpty()) {
            out.rubbleDirt(rubbleDirt);
        }
        if (building != null) {
            BlockRef filler = blockOf(palette, building.filler());
            if (filler != null) {
                out.filler(filler);
            }
            BlockRef rubble = blockOf(palette, building.rubble());
            out.ruinRubble(rubble);
            if (rubble != null) {
                out.rubble(rubble);
            } else if (filler != null) {
                out.rubble(filler);
            }
        }
        for (char c : palette.characters()) {
            BlockRef damaged = palette.damaged(c);
            if (damaged == null) {
                continue;
            }
            for (BlockRef block : palette.getAll(c)) {
                if (!block.isAir()) {
                    out.damaged(block, damaged);
                }
            }
        }
        return out.build();
    }

    /** Returns a city-style character, or {@code null}. */
    static Character setting(CityStyle style, CityStyle.Setting setting) {
        return style == null ? null : style.character(setting);
    }

    /** Returns a character's first slot as a block, or {@code null} when it is unset or undefined. */
    static BlockRef blockOf(CompiledPalette palette, Character c) {
        if (c == null || !palette.isDefined(c)) {
            return null;
        }
        BlockChoice choice = palette.choice(c, 0);
        if (choice == null || choice.isHardAir()) {
            return null;
        }
        return choice.block(palette.flags());
    }

    /** Returns a code-named block (block-mapping.md, "Blocks upstream's code names"), or {@code null}. */
    BlockRef code(String key) {
        BlockChoice choice = assets.codeBlock(key);
        return choice == null ? null : choice.block(flags);
    }

    private List<BlockRef> slots(CompiledPalette palette, Character c, RandomList fallback) {
        List<BlockRef> out = new ArrayList<>(RandomList.SLOTS);
        if (c != null && palette.isDefined(c)) {
            for (int slot = 0; slot < RandomList.SLOTS; slot++) {
                out.add(palette.get(c, slot));
            }
            return out;
        }
        if (fallback != null) {
            for (int slot = 0; slot < RandomList.SLOTS; slot++) {
                out.add(
                    fallback.slot(slot)
                        .block(flags));
            }
        }
        return out;
    }

    /** Returns the cached chunk palettes, for the B5 estimate and tests. */
    public int cachedChunkPalettes() {
        return chunks.size();
    }

    /** Returns the cached part palettes, for tests. */
    public int cachedPartPalettes() {
        return parts.size();
    }

    /** Returns how many part palettes were compiled (cache misses), for tests. */
    public long compiledPartPalettes() {
        return parts.misses();
    }

    /** Empties every cache. */
    public void clear() {
        chunks.clear();
        parts.clear();
        styles.clear();
    }

    /** Returns the id of a full or bare asset name, or {@code null} for a malformed one. */
    static AssetId id(String name) {
        if (name == null) {
            return null;
        }
        try {
            return AssetId.parse(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
