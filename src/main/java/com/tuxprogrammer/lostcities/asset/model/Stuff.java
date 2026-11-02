// spotless:off
package com.tuxprogrammer.lostcities.asset.model;
// Ported from Lost Cities (MIT): https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/regassets/StuffSettingsRE.java
// Avoids: ASSET-21
// spotless:on

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.json.JsonFields;
import com.tuxprogrammer.lostcities.asset.match.BiomeMatcher;
import com.tuxprogrammer.lostcities.asset.match.BlockMatcher;
import com.tuxprogrammer.lostcities.asset.match.NameMatcher;

/**
 * A stuff asset: a column of palette characters scattered into city chunks, such as the bundled chains and cobwebs
 * (docs/original-spec/05-assets/scattered-stuff-predefined.md, "Stuff JSON"; behavior in {@code 07-scattered}). Heights
 * are absolute block Y.
 *
 * <p>
 * Changes from upstream (docs/new-spec/05-assets/index.md, "Stuff JSON"):
 * <ul>
 * <li>{@code maxcount} must be at least {@code mincount}; equal means exactly {@code mincount}, where upstream's
 * {@code nextInt(0)} threw (fixes ASSET-21 and SCAT-17).</li>
 * <li>{@code maxheight} must exceed {@code minheight} when both are given (fixes SCAT-18).</li>
 * <li>An unset {@code inbuilding} means "either", where upstream never generated the stuff (fixes SCAT-19).</li>
 * </ul>
 * Immutable. Side: both.
 */
public final class Stuff {

    private final AssetId id;
    private final Set<String> tags;
    private final String column;
    private final Integer minHeight;
    private final Integer maxHeight;
    private final int minCount;
    private final int maxCount;
    private final int attempts;
    private final Boolean inBuilding;
    private final Boolean seeSky;
    private final BiomeMatcher biomes;
    private final BlockMatcher blocks;
    private final BlockMatcher upperBlocks;
    private final NameMatcher buildings;

    private Stuff(AssetId id, Set<String> tags, String column, Integer minHeight, Integer maxHeight, int minCount,
        int maxCount, int attempts, Boolean inBuilding, Boolean seeSky, BiomeMatcher biomes, BlockMatcher blocks,
        BlockMatcher upperBlocks, NameMatcher buildings) {
        this.id = id;
        this.tags = tags;
        this.column = column;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.minCount = minCount;
        this.maxCount = maxCount;
        this.attempts = attempts;
        this.inBuilding = inBuilding;
        this.seeSky = seeSky;
        this.biomes = biomes;
        this.blocks = blocks;
        this.upperBlocks = upperBlocks;
        this.buildings = buildings;
    }

    /**
     * Reads a stuff file.
     *
     * @param root    the document's fields
     * @param context the asset being read
     * @return the stuff, or null after reporting an error
     */
    public static Stuff parse(JsonFields root, ParseContext context) {
        String column = root.string("column", true);
        Integer minCount = root.integer("mincount", true);
        Integer maxCount = root.integer("maxcount", true);
        Integer attempts = root.integer("attempts", true);
        Integer minHeight = root.integer("minheight", false);
        Integer maxHeight = root.integer("maxheight", false);
        if (column == null || minCount == null || maxCount == null || attempts == null) {
            return null;
        }
        boolean ok = true;
        if (column.isEmpty()) {
            root.error("column", column, "a column has at least one character");
            ok = false;
        }
        if (minCount < 0 || maxCount < minCount) {
            root.error(
                "maxcount",
                minCount + ".." + maxCount,
                "needs 0 <= mincount <= maxcount (equal means exactly mincount)");
            ok = false;
        }
        if (attempts < 0) {
            root.error("attempts", Integer.toString(attempts), "must not be negative");
            ok = false;
        }
        if (minHeight != null && maxHeight != null && maxHeight <= minHeight) {
            root.error("maxheight", minHeight + ".." + maxHeight, "must be above minheight");
            ok = false;
        }
        Set<String> tags = new TreeSet<>();
        List<String> tagList = root.stringList("tags", false);
        if (tagList != null) {
            tags.addAll(tagList);
        }
        JsonFields biomeFields = root.object("biomes", false);
        JsonFields blockFields = root.object("blocks", false);
        JsonFields upperFields = root.object("upperblocks", false);
        JsonFields nameFields = root.object("buildings", false);
        BiomeMatcher biomes = biomeFields == null ? BiomeMatcher.ANY : BiomeMatcher.parse(biomeFields);
        BlockMatcher blocks = blockFields == null ? BlockMatcher.ANY
            : BlockMatcher.parse(blockFields, context.blocks());
        BlockMatcher upper = upperFields == null ? BlockMatcher.ANY : BlockMatcher.parse(upperFields, context.blocks());
        NameMatcher buildings = nameFields == null ? NameMatcher.ANY : NameMatcher.parse(nameFields);
        if (!ok) {
            return null;
        }
        return new Stuff(
            context.id(),
            Collections.unmodifiableSet(tags),
            column,
            minHeight,
            maxHeight,
            minCount,
            maxCount,
            attempts,
            root.bool("inbuilding", false),
            root.bool("seesky", false),
            biomes,
            blocks,
            upper,
            buildings);
    }

    /** Returns the id. */
    public AssetId id() {
        return id;
    }

    /** Returns the tags city styles select it by; without tags it is never used. */
    public Set<String> tags() {
        return tags;
    }

    /** Returns the palette characters placed bottom to top. */
    public String column() {
        return column;
    }

    /** Returns the lowest block Y, or null for the default. */
    public Integer minHeight() {
        return minHeight;
    }

    /** Returns the highest block Y (exclusive), or null for the default. */
    public Integer maxHeight() {
        return maxHeight;
    }

    /** Returns the minimum columns per chunk. */
    public int minCount() {
        return minCount;
    }

    /** Returns the maximum columns per chunk; equal to {@link #minCount()} means exactly that many. */
    public int maxCount() {
        return maxCount;
    }

    /** Returns the tries per column. */
    public int attempts() {
        return attempts;
    }

    /** Returns true for building chunks only, false for chunks without a building, null for either. */
    public Boolean inBuilding() {
        return inBuilding;
    }

    /** Returns the required sky visibility of the base position, or null when not tested. */
    public Boolean seeSky() {
        return seeSky;
    }

    /** Returns the allowed main biomes of the chunk. */
    public BiomeMatcher biomes() {
        return biomes;
    }

    /** Returns the test on the block below the column. */
    public BlockMatcher blocks() {
        return blocks;
    }

    /** Returns the test on the block just above the column. */
    public BlockMatcher upperBlocks() {
        return upperBlocks;
    }

    /** Returns the test on the chunk's building. */
    public NameMatcher buildings() {
        return buildings;
    }
}
