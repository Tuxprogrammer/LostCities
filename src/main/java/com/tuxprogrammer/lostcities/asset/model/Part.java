package com.tuxprogrammer.lostcities.asset.model;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/cityassets/BuildingPart.java
// Avoids: ASSET-24, ASSET-23

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetType;
import com.tuxprogrammer.lostcities.asset.json.JsonFields;

/**
 * A part: a small 3D block template written as palette characters, one slice per Y level, bottom first, each slice
 * {@code zsize} rows of {@code xsize} characters (docs/original-spec/05-assets/parts-and-placement.md, "Part JSON").
 * A space means "leave the block alone". A part may layer its own palette over the chunk's, inline ({@code palette})
 * or by name ({@code refpalette}); the inline one wins when both are given, as upstream.
 *
 * <p>
 * Changes from upstream: every slice must have exactly {@code zsize} rows of exactly {@code xsize} characters, or the
 * part is rejected at load (fixes ASSET-24: a wrong row shifted the rest of the slice, and a short slice threw at
 * generation); metadata values of the wrong type for a known key are rejected at load instead of throwing when read
 * (fixes ASSET-23 for {@code char} values). Columns are precomputed, bottom to top, with all-space columns left null,
 * because generation walks columns. Immutable. Side: both.
 */
public final class Part {

    /**
     * The metadata keys read, with the value type each expects: upstream's {@code ILostCities.META_*} and our
     * {@link #STRETCHROW}.
     */
    public enum MetaKey {

        /** No doorway or corridor connection into this floor. */
        DONTCONNECT("dontconnect", Boolean.class),
        /** Palette character for the support pillar under a bridge or highway part. */
        SUPPORT("support", Character.class),
        /** First Z of a stair opening. */
        Z1("z1", Integer.class),
        /** Last Z of a stair opening. */
        Z2("z2", Integer.class),
        /** Hard air below the water level stays air. */
        NOWATER("nowater", Boolean.class),
        /** Hard air always becomes air. */
        FORCEDAIR("forcedair", Boolean.class),
        /**
         * The row this part repeats when its floor stretches (decision log D-mall-1), so the parts of one multibuilding
         * floor stretch alike; ours, not upstream's. Unset, or outside 1 to {@code height − 2}, picks the row as
         * {@link FloorStretch#repeatRow(Part)} describes.
         */
        STRETCHROW("stretchrow", Integer.class);

        private final String key;
        private final Class<?> type;

        MetaKey(String key, Class<?> type) {
            this.key = key;
            this.type = type;
        }

        /** Returns the key as written. */
        public String key() {
            return key;
        }

        static MetaKey byKey(String key) {
            for (MetaKey meta : values()) {
                if (meta.key.equals(key)) {
                    return meta;
                }
            }
            return null;
        }
    }

    private final AssetId id;
    private final int xsize;
    private final int zsize;
    private final List<String> slices;
    private final Palette palette;
    private final Map<String, Object> meta;
    private final char[][] columns;
    /** Stretched copies by {@code extra * 256 + row}, made on first use (design record 0014). */
    private final java.util.concurrent.ConcurrentHashMap<Integer, Part> stretched = new java.util.concurrent.ConcurrentHashMap<>();

    private Part(AssetId id, int xsize, int zsize, List<String> slices, Palette palette, Map<String, Object> meta) {
        this.id = id;
        this.xsize = xsize;
        this.zsize = zsize;
        this.slices = Collections.unmodifiableList(slices);
        this.palette = palette;
        this.meta = Collections.unmodifiableMap(meta);
        this.columns = new char[xsize * zsize][];
        for (int x = 0; x < xsize; x++) {
            for (int z = 0; z < zsize; z++) {
                char[] column = new char[slices.size()];
                boolean empty = true;
                for (int y = 0; y < slices.size(); y++) {
                    column[y] = slices.get(y)
                        .charAt(z * xsize + x);
                    empty &= column[y] == ' ';
                }
                columns[z * xsize + x] = empty ? null : column;
            }
        }
    }

    /**
     * Reads a part file.
     *
     * @param root    the document's fields
     * @param context the asset being read
     * @return the part, or null after reporting an error
     */
    public static Part parse(JsonFields root, ParseContext context) {
        Integer xsize = root.integer("xsize", true);
        Integer zsize = root.integer("zsize", true);
        JsonArray rawSlices = root.array("slices", true);
        if (xsize == null || zsize == null || rawSlices == null) {
            return null;
        }
        if (xsize <= 0 || zsize <= 0) {
            root.error("xsize", xsize + "x" + zsize, "the footprint must be at least 1 x 1");
            return null;
        }
        List<String> slices = new ArrayList<>();
        boolean ok = true;
        for (int y = 0; y < rawSlices.size(); y++) {
            String path = root.field("slices") + "[" + y + "]";
            JsonElement slice = rawSlices.get(y);
            if (!slice.isJsonArray()) {
                context.scope()
                    .error(path, null, "a slice is a list of rows");
                ok = false;
                continue;
            }
            JsonArray rows = slice.getAsJsonArray();
            if (rows.size() != zsize) {
                context.scope()
                    .error(path, Integer.toString(rows.size()), "has that many rows; zsize is " + zsize);
                ok = false;
                continue;
            }
            StringBuilder flat = new StringBuilder(xsize * zsize);
            for (int z = 0; z < rows.size(); z++) {
                JsonElement row = rows.get(z);
                if (!row.isJsonPrimitive() || !row.getAsJsonPrimitive()
                    .isString()) {
                    context.scope()
                        .error(path + "[" + z + "]", row.toString(), "a row is a string");
                    ok = false;
                    continue;
                }
                String text = row.getAsString();
                if (text.length() != xsize) {
                    context.scope()
                        .error(path + "[" + z + "]", text, "has " + text.length() + " characters; xsize is " + xsize);
                    ok = false;
                    continue;
                }
                flat.append(text);
            }
            slices.add(flat.toString());
        }
        if (slices.isEmpty() && ok) {
            root.error("slices", null, "a part has at least one slice");
            ok = false;
        }
        Palette palette = null;
        JsonFields inline = root.object("palette", false);
        if (inline != null) {
            palette = Palette.parse(inline, local(context.id()), context);
        } else {
            String ref = root.string("refpalette", false);
            if (ref != null) {
                palette = context.require(AssetType.PALETTES, ref, Palette.class, root.field("refpalette"));
                ok &= palette != null;
            }
        }
        Map<String, Object> meta = meta(root, context);
        return ok && meta != null ? new Part(context.id(), xsize, zsize, slices, palette, meta) : null;
    }

    /** Returns the id an inline palette of an asset gets, upstream's {@code __local__} name. */
    static AssetId local(AssetId owner) {
        return AssetId.of(owner.namespace(), owner.path() + "/__local__");
    }

    /**
     * Returns this part stretched to a taller floor pitch: row {@code row} written {@code extra} more times, with
     * diagonal stair runs across it re-laid ({@link FloorStretch}). Copies are memoized, so the cost is paid once per
     * part, row and extra; the original stays the part every pitch-6 world places.
     *
     * @param extra how many rows to add; 0 or less returns this part
     * @param row   the repeat row, usually {@link FloorStretch#repeatRow(Part)} of the floor's main part
     * @return the stretched part, with the same id, palette and metadata
     */
    public Part stretched(int extra, int row) {
        if (extra <= 0 || row < 0 || row >= height()) {
            return this;
        }
        return stretched.computeIfAbsent(
            extra * 256 + row,
            key -> new Part(id, xsize, zsize, FloorStretch.stretch(slices, xsize, zsize, extra, row), palette, meta));
    }

    /**
     * Returns this part at a lower floor height: the given rows left out (decision log D-floor-heights-1). Copies are
     * memoized like {@link #stretched(int, int)}.
     *
     * @param drop the rows to leave out, usually {@link FloorStretch#dropRows(Part, int)} of the floor's main part;
     *             empty returns this part
     * @return the lower part, with the same id, palette and metadata
     */
    public Part squeezed(int[] drop) {
        int mask = 0;
        for (int d : drop) {
            if (d >= 0 && d < height() && d < 30) {
                mask |= 1 << d;
            }
        }
        if (mask == 0) {
            return this;
        }
        return stretched.computeIfAbsent(
            -mask,
            key -> new Part(id, xsize, zsize, FloorStretch.squeeze(slices, drop), palette, meta));
    }

    /** Stacks already laid out, by their parts (compared by identity) and row counts; cleared when it grows large. */
    private static final java.util.concurrent.ConcurrentHashMap<List<Object>, List<Part>> STACKS = new java.util.concurrent.ConcurrentHashMap<>();

    /** The most stacks {@link #STACKS} holds before it starts over. */
    private static final int MAX_STACKS = 4096;

    /**
     * Returns a building's parts, bottom first, each written at its own height, with diagonal stair runs that cross
     * from one part into the next re-laid whole ({@link FloorStretch#stack}, decision log D-stair-xfloor-1). A part no
     * such run touches comes out as {@link #stretched(int, int)} or {@link #squeezed(int[])} would give it.
     *
     * @param parts  the parts, bottom first, all the same width and depth
     * @param copies per part, how many times each row is written ({@link FloorStretch#copies})
     * @return the parts as placed, with the same ids, palettes and metadata
     */
    public static List<Part> stacked(List<Part> parts, List<int[]> copies) {
        List<Object> key = new ArrayList<>(parts.size() * 2);
        for (int i = 0; i < parts.size(); i++) {
            key.add(new Identity(parts.get(i)));
            key.add(java.util.Arrays.toString(copies.get(i)));
        }
        List<Part> hit = STACKS.get(key);
        if (hit != null) {
            return hit;
        }
        List<List<String>> slices = new ArrayList<>(parts.size());
        for (Part part : parts) {
            slices.add(part.slices);
        }
        Part first = parts.get(0);
        List<List<String>> laid = FloorStretch.stack(slices, first.xsize, first.zsize, copies);
        List<Part> out = new ArrayList<>(parts.size());
        for (int i = 0; i < parts.size(); i++) {
            Part part = parts.get(i);
            out.add(
                laid.get(i)
                    .equals(part.slices) ? part
                        : new Part(part.id, part.xsize, part.zsize, laid.get(i), part.palette, part.meta));
        }
        out = Collections.unmodifiableList(out);
        if (STACKS.size() >= MAX_STACKS) {
            STACKS.clear();
        }
        STACKS.put(key, out);
        return out;
    }

    /** A part compared by identity, so a stack key never mixes up parts of two asset sets. */
    private static final class Identity {

        private final Part part;

        Identity(Part part) {
            this.part = part;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Identity && ((Identity) o).part == part;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(part);
        }
    }

    /** Returns the id. */
    public AssetId id() {
        return id;
    }

    /** Returns the width along X, in blocks. */
    public int xsize() {
        return xsize;
    }

    /** Returns the depth along Z, in blocks. */
    public int zsize() {
        return zsize;
    }

    /** Returns the height: the number of slices, one block each. */
    public int height() {
        return slices.size();
    }

    /**
     * Returns the character at a position.
     *
     * @param x 0 to {@code xsize − 1}
     * @param y 0 to {@code height − 1}, the slice
     * @param z 0 to {@code zsize − 1}
     * @return the palette character; a space means "leave the block alone"
     */
    public char get(int x, int y, int z) {
        return slices.get(y)
            .charAt(z * xsize + x);
    }

    /**
     * Returns a column, bottom to top.
     *
     * @param x 0 to {@code xsize − 1}
     * @param z 0 to {@code zsize − 1}
     * @return the characters, or null when the whole column is spaces; callers must not modify it
     */
    public char[] column(int x, int z) {
        return columns[z * xsize + x];
    }

    /** Returns the part's own palette layer (inline or referenced), or null. */
    public Palette palette() {
        return palette;
    }

    /** Returns every character the slices use, besides the space, sorted. */
    public java.util.SortedSet<Character> characters() {
        java.util.SortedSet<Character> out = new java.util.TreeSet<>();
        for (String slice : slices) {
            for (int i = 0; i < slice.length(); i++) {
                if (slice.charAt(i) != ' ') {
                    out.add(slice.charAt(i));
                }
            }
        }
        return out;
    }

    /** Returns a boolean metadata value; false when absent. */
    public boolean metaBoolean(MetaKey key) {
        Object value = meta.get(key.key());
        return value instanceof Boolean && (Boolean) value;
    }

    /** Returns an integer metadata value, or null when absent. */
    public Integer metaInteger(MetaKey key) {
        Object value = meta.get(key.key());
        return value instanceof Integer ? (Integer) value : null;
    }

    /** Returns a character metadata value, or null when absent. */
    public Character metaCharacter(MetaKey key) {
        Object value = meta.get(key.key());
        return value instanceof Character ? (Character) value : null;
    }

    /** Returns every metadata value by key, typed as written. */
    public Map<String, Object> meta() {
        return meta;
    }

    private static Map<String, Object> meta(JsonFields root, ParseContext context) {
        Map<String, Object> meta = new LinkedHashMap<>();
        boolean ok = true;
        for (JsonFields entry : root.objects("meta", false)) {
            String key = entry.string("key", true);
            if (key == null) {
                ok = false;
                continue;
            }
            // The first present field wins, in upstream's order.
            Object value;
            if (entry.has("integer")) {
                value = entry.integer("integer", true);
            } else if (entry.has("float")) {
                value = entry.number("float", true);
            } else if (entry.has("boolean")) {
                value = entry.bool("boolean", true);
            } else if (entry.has("char")) {
                value = entry.character("char", true);
            } else if (entry.has("string")) {
                value = entry.string("string", true);
            } else {
                entry.error("", key, "a meta entry has one of integer, float, boolean, char, string");
                ok = false;
                continue;
            }
            if (value == null) {
                ok = false;
                continue;
            }
            MetaKey known = MetaKey.byKey(key);
            if (known != null && !known.type.isInstance(value)) {
                entry.error(
                    "",
                    key,
                    "meta '" + key
                        + "' must be a "
                        + known.type.getSimpleName()
                            .toLowerCase(java.util.Locale.ROOT));
                ok = false;
                continue;
            }
            meta.put(key, value);
        }
        return ok ? meta : null;
    }
}
