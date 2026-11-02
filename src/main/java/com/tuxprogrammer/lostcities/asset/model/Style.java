package com.tuxprogrammer.lostcities.asset.model;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/cityassets/Style.java
// Avoids: ASSET-08

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetType;
import com.tuxprogrammer.lostcities.asset.json.JsonFields;
import com.tuxprogrammer.lostcities.asset.match.WeightedPick;

/**
 * A style: the recipe for a chunk's palette, groups of weighted palettes from which one palette per group is drawn;
 * later groups override earlier ones character by character (docs/original-spec/05-assets/styles-and-city-styles.md,
 * "Style").
 *
 * <p>
 * Changes from upstream: an empty group, or one whose weights are all zero, is rejected at load, and when float
 * rounding leaves a remainder after the last entry the last entry is taken (fixes ASSET-08: upstream merged null and
 * threw). One {@code nextFloat()} is drawn per group, as upstream. Immutable. Side: both.
 */
public final class Style {

    /** One palette of a group, with its weight. */
    public static final class Choice {

        private final float factor;
        private final Palette palette;

        Choice(float factor, Palette palette) {
            this.factor = factor;
            this.palette = palette;
        }

        /** Returns the weight within the group. */
        public float factor() {
            return factor;
        }

        /** Returns the palette. */
        public Palette palette() {
            return palette;
        }
    }

    private final AssetId id;
    private final List<List<Choice>> groups;

    private Style(AssetId id, List<List<Choice>> groups) {
        this.id = id;
        this.groups = Collections.unmodifiableList(groups);
    }

    /**
     * Reads a style file.
     *
     * @param root    the document's fields
     * @param context the asset being read
     * @return the style, or null after reporting an error
     */
    public static Style parse(JsonFields root, ParseContext context) {
        JsonArray outer = root.array("randompalettes", true);
        if (outer == null) {
            return null;
        }
        List<List<Choice>> groups = new ArrayList<>();
        boolean ok = true;
        for (int g = 0; g < outer.size(); g++) {
            String path = root.field("randompalettes") + "[" + g + "]";
            JsonElement group = outer.get(g);
            if (!group.isJsonArray() || group.getAsJsonArray()
                .size() == 0) {
                context.scope()
                    .error(
                        path,
                        group.toString(),
                        "a palette group is a non-empty list (an empty group crashed upstream)");
                ok = false;
                continue;
            }
            List<Choice> choices = new ArrayList<>();
            float total = 0;
            JsonArray entries = group.getAsJsonArray();
            for (int i = 0; i < entries.size(); i++) {
                JsonElement element = entries.get(i);
                String entryPath = path + "[" + i + "]";
                if (!element.isJsonObject()) {
                    context.scope()
                        .error(entryPath, element.toString(), "must be {factor, palette}");
                    ok = false;
                    continue;
                }
                JsonFields entry = new JsonFields(element.getAsJsonObject(), entryPath, context.scope());
                Float factor = entry.number("factor", true);
                String name = entry.string("palette", true);
                Palette palette = name == null ? null
                    : context.require(AssetType.PALETTES, name, Palette.class, entry.field("palette"));
                if (factor == null || palette == null) {
                    ok = false;
                    continue;
                }
                total += Math.max(0, factor);
                choices.add(new Choice(factor, palette));
            }
            if (total <= 0 && ok) {
                context.scope()
                    .error(path, null, "a palette group needs a positive total weight");
                ok = false;
            }
            groups.add(Collections.unmodifiableList(choices));
        }
        if (groups.isEmpty() && ok) {
            root.error("randompalettes", null, "a style has at least one palette group");
            ok = false;
        }
        return ok ? new Style(context.id(), groups) : null;
    }

    /** Returns the id. */
    public AssetId id() {
        return id;
    }

    /** Returns the groups, in order. */
    public List<List<Choice>> groups() {
        return groups;
    }

    /**
     * Draws a chunk's palette layers: one palette per group, in group order.
     *
     * @param random the building random; one {@code nextFloat()} per group
     * @return the chosen palettes, lowest layer first, one per group
     */
    public List<Palette> randomPalettes(Random random) {
        List<Palette> out = new ArrayList<>();
        for (List<Choice> group : groups) {
            Choice choice = WeightedPick.pick(random, group, Choice::factor);
            out.add(choice.palette);
        }
        return out;
    }
}
