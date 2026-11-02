// spotless:off
package com.tuxprogrammer.lostcities.asset.model;
// Ported from Lost Cities (MIT): https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/cityassets/Palette.java
// Avoids: ASSET-23, ASSET-30, ASSET-14
// spotless:on

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetType;
import com.tuxprogrammer.lostcities.asset.block.BlockChoice;
import com.tuxprogrammer.lostcities.asset.block.TileTemplate;
import com.tuxprogrammer.lostcities.asset.json.JsonFields;

/**
 * A palette: characters mapped to blocks, read from a palette file or from the inline {@code palette} of a building or
 * part (docs/original-spec/05-assets/palettes-and-variants.md, "Palette JSON" and "Parsing a palette"). Palettes are
 * layered and compiled for a chunk by {@link com.tuxprogrammer.lostcities.asset.palette.CompiledPalette}.
 *
 * <p>
 * Changes from upstream:
 * <ul>
 * <li>{@code char} must be exactly one character, or the entry is rejected (fixes ASSET-23).</li>
 * <li>A {@code damaged} target belongs to the entry that declares it, never to every character with the same block
 * (fixes ASSET-30).</li>
 * <li>Blocks and variants resolve at load from the loaded asset set and the block registry, never through a running
 * server (fixes ASSET-14).</li>
 * </ul>
 * A character given twice keeps the later entry, as upstream, with a warning. Immutable. Side: both.
 */
public final class Palette {

    private final AssetId id;
    private final Map<Character, PaletteEntry> entries;

    private Palette(AssetId id, Map<Character, PaletteEntry> entries) {
        this.id = id;
        this.entries = Collections.unmodifiableMap(entries);
    }

    /**
     * Reads a palette object, {@code {"palette": [entry, ...]}}.
     *
     * @param fields  the object
     * @param id      the palette's id: the file's for a palette asset, a local id for an inline palette
     * @param context the asset being read
     * @return the palette; the caller rejects the asset if an error was reported
     */
    public static Palette parse(JsonFields fields, AssetId id, ParseContext context) {
        Map<Character, PaletteEntry> entries = new LinkedHashMap<>();
        for (JsonFields entry : fields.objects("palette", true)) {
            PaletteEntry parsed = entry(entry, context);
            if (parsed != null && entries.put(parsed.character(), parsed) != null) {
                entry.warning("char", String.valueOf(parsed.character()), "is defined twice; the later entry is used");
            }
        }
        return new Palette(id, entries);
    }

    /** Returns the id; inline palettes have {@code <asset>/__local__}. */
    public AssetId id() {
        return id;
    }

    /** Returns the entries by character, in file order. */
    public Map<Character, PaletteEntry> entries() {
        return entries;
    }

    private static PaletteEntry entry(JsonFields entry, ParseContext context) {
        Character c = entry.character("char", true);
        if (c == null) {
            return null;
        }
        BlockChoice damaged = null;
        String damagedText = entry.string("damaged", false);
        if (damagedText != null) {
            damaged = context.block(damagedText, c, entry.field("damaged"));
            if (damaged == null) {
                return null;
            }
        }
        Condition loot = null;
        String lootName = entry.string("loot", false);
        if (lootName != null && !lootName.isEmpty()) {
            loot = context.require(AssetType.CONDITIONS, lootName, Condition.class, entry.field("loot"));
            if (loot == null) {
                return null;
            }
        }
        Condition mob = null;
        String mobName = entry.string("mob", false);
        if (mobName != null && !mobName.isEmpty()) {
            mob = context.require(AssetType.CONDITIONS, mobName, Condition.class, entry.field("mob"));
            if (mob == null) {
                return null;
            }
        }
        boolean torch = entry.flag("torch", false);
        JsonFields tagFields = entry.object("tag", false);
        TileTemplate tag = tagFields == null ? null : TileTemplate.of(tagFields.object());
        int kinds = (entry.has("block") ? 1 : 0) + (entry.has("variant") ? 1 : 0)
            + (entry.has("frompalette") ? 1 : 0)
            + (hasBlocks(entry) ? 1 : 0);
        if (kinds > 1) {
            entry.warning(
                "",
                String.valueOf(c),
                "names more than one of block, variant, frompalette, blocks; the first in that order is used");
        }
        if (entry.has("block")) {
            String text = entry.string("block", true);
            BlockChoice block = text == null ? null : context.block(text, c, entry.field("block"));
            return block == null ? null
                : new PaletteEntry(c, PaletteEntry.Kind.BLOCK, block, null, ' ', damaged, loot, mob, torch, tag);
        }
        if (entry.has("variant")) {
            String name = entry.string("variant", true);
            Variant variant = name == null ? null
                : context.require(AssetType.VARIANTS, name, Variant.class, entry.field("variant"));
            return variant == null ? null
                : new PaletteEntry(
                    c,
                    PaletteEntry.Kind.RANDOM,
                    null,
                    variant.blocks(),
                    ' ',
                    damaged,
                    loot,
                    mob,
                    torch,
                    tag);
        }
        if (entry.has("frompalette")) {
            Character target = entry.character("frompalette", true);
            return target == null ? null
                : new PaletteEntry(c, PaletteEntry.Kind.REFERENCE, null, null, target, damaged, loot, mob, torch, tag);
        }
        if (hasBlocks(entry)) {
            RandomList random = RandomList.parse(entry, "blocks", context, c);
            return random == null ? null
                : new PaletteEntry(c, PaletteEntry.Kind.RANDOM, null, random, ' ', damaged, loot, mob, torch, tag);
        }
        entry.error("", String.valueOf(c), "names none of block, variant, frompalette, blocks");
        return null;
    }

    private static boolean hasBlocks(JsonFields entry) {
        // An empty inline list counts as absent, as upstream.
        return entry.has("blocks") && entry.object()
            .get("blocks")
            .isJsonArray()
            && entry.object()
                .getAsJsonArray("blocks")
                .size() > 0;
    }
}
