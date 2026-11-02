package com.tuxprogrammer.lostcities.editor;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/commands/CommandExportPart.java
// Avoids: CORE-29

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.tuxprogrammer.lostcities.asset.BlockRef;

/**
 * Turns an edited part, read back from the world, into upstream's export file: a JSON object with a comment, the
 * {@code missingpalette} of blocks the part's palette has no character for, and the {@code exportedpart} itself
 * (docs/original-spec/01-core/part-editor.md, "Export file"; docs/new-spec/01-core/index.md, Part editor).
 *
 * <p>
 * Each cell, slice by slice, row by row, column by column, takes the session's character for its block; a block the
 * palette does not know takes the first character of {@link #POOL} that is not in use, once per block. Blocks are
 * written in this mod's native form {@code modid:name:meta} (D14).
 *
 * <p>
 * Changes from upstream:
 * <ul>
 * <li>Avoids CORE-29: when the pool runs out the export fails with a result that says so, instead of writing
 * {@code null} into a row; {@code missingpalette} is sorted by block, so the same part exports the same file every
 * time.</li>
 * <li>Air that no palette character stands for exports as a space (the part leaves the world's block there), not as
 * a new palette character for air.</li>
 * </ul>
 *
 * <p>
 * Side: both (plain Java).
 */
public final class PartExport {

    /**
     * Upstream's character pool, in its order: {@code a}–{@code z}, {@code A}–{@code Z}, {@code 0}–{@code 9}, 30
     * symbols, then U+0370 to U+04FF (Greek and Cyrillic); 492 characters.
     */
    public static final List<Character> POOL = pool();

    /** The comment of an export with new blocks (upstream's text). */
    static final String COMMENT_MISSING = "'missingpalette' represents all blockstates that it couldn't find in the"
        + " palette. These have to be put in a palette. 'exportedpart' is the actual exported part";
    /** The comment of an export without new blocks (upstream's text). */
    static final String COMMENT_PLAIN = "'exportedpart' is the actual exported part";

    /** Reads the block at a part-local cell. */
    @FunctionalInterface
    public interface Grid {

        /**
         * Returns a cell's block.
         *
         * @param x the part's local X
         * @param y the slice
         * @param z the part's local Z
         * @return the block in the world there
         */
        BlockRef at(int x, int y, int z);
    }

    /** What an export produced. */
    public static final class Result {

        private final String json;
        private final int newBlocks;

        Result(String json, int newBlocks) {
            this.json = json;
            this.newBlocks = newBlocks;
        }

        /** Returns the file's text, or {@code null} when the character pool ran out. */
        public String json() {
            return json;
        }

        /** Returns whether the export failed because the pool ran out. */
        public boolean poolExhausted() {
            return json == null;
        }

        /** Returns how many blocks went into {@code missingpalette}. */
        public int newBlocks() {
            return newBlocks;
        }
    }

    private PartExport() {}

    private static List<Character> pool() {
        List<Character> out = new ArrayList<>();
        for (char c = 'a'; c <= 'z'; c++) {
            out.add(c);
        }
        for (char c = 'A'; c <= 'Z'; c++) {
            out.add(c);
        }
        for (char c = '0'; c <= '9'; c++) {
            out.add(c);
        }
        for (char c : "!@#$%^&*()_+-=[]{}|;:'<>,.?/`~".toCharArray()) {
            out.add(c);
        }
        for (char c = 'Ͱ'; c <= 'ӿ'; c++) {
            out.add(c);
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * Exports a part.
     *
     * @param xsize      the part's X size
     * @param zsize      the part's Z size
     * @param height     the part's slice count
     * @param reverse    the session's reverse palette: the character each block stands for
     * @param used       the characters the part's palette defines; new characters avoid them
     * @param refPalette the part's reference palette, written as {@code refpalette}, or {@code null}
     * @param grid       the world's blocks at the part's cells
     * @return the export
     */
    public static Result export(int xsize, int zsize, int height, Map<BlockRef, Character> reverse, Set<Character> used,
        String refPalette, Grid grid) {
        Set<Character> taken = new HashSet<>(used);
        taken.add(' ');
        taken.addAll(reverse.values());
        Map<BlockRef, Character> assigned = new LinkedHashMap<>();
        int next = 0;
        JsonArray slices = new JsonArray();
        for (int y = 0; y < height; y++) {
            JsonArray rows = new JsonArray();
            for (int z = 0; z < zsize; z++) {
                StringBuilder row = new StringBuilder(xsize);
                for (int x = 0; x < xsize; x++) {
                    BlockRef block = grid.at(x, y, z);
                    Character c = reverse.get(block);
                    if (c == null) {
                        c = assigned.get(block);
                    }
                    if (c == null && block.isAir()) {
                        c = ' ';
                    }
                    if (c == null) {
                        while (next < POOL.size() && taken.contains(POOL.get(next))) {
                            next++;
                        }
                        if (next >= POOL.size()) {
                            // Avoids CORE-29: fail instead of writing "null" into the row.
                            return new Result(null, assigned.size());
                        }
                        c = POOL.get(next);
                        taken.add(c);
                        assigned.put(block, c);
                    }
                    row.append(c.charValue());
                }
                rows.add(new JsonPrimitive(row.toString()));
            }
            slices.add(rows);
        }
        JsonObject root = new JsonObject();
        root.addProperty("__comment__", assigned.isEmpty() ? COMMENT_PLAIN : COMMENT_MISSING);
        if (!assigned.isEmpty()) {
            root.add("missingpalette", missingPalette(assigned));
        }
        JsonObject part = new JsonObject();
        part.addProperty("xsize", xsize);
        part.addProperty("zsize", zsize);
        part.add("slices", slices);
        if (refPalette != null) {
            part.addProperty("refpalette", refPalette);
        }
        root.add("exportedpart", part);
        String json = new GsonBuilder().setPrettyPrinting()
            .disableHtmlEscaping()
            .create()
            .toJson(root);
        return new Result(json, assigned.size());
    }

    /** The new blocks as a palette, sorted by block (avoids CORE-29's unstable order). */
    private static JsonObject missingPalette(Map<BlockRef, Character> assigned) {
        List<BlockRef> blocks = new ArrayList<>(assigned.keySet());
        blocks.sort(
            (a, b) -> a.toString()
                .compareTo(b.toString()));
        JsonArray entries = new JsonArray();
        for (BlockRef block : blocks) {
            JsonObject entry = new JsonObject();
            entry.addProperty(
                "char",
                assigned.get(block)
                    .toString());
            entry.addProperty("block", block.toString());
            entries.add(entry);
        }
        JsonObject palette = new JsonObject();
        palette.add("palette", entries);
        return palette;
    }
}
