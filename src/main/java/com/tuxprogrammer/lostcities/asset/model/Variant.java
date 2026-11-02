package com.tuxprogrammer.lostcities.asset.model;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.json.JsonFields;

/**
 * A variant asset: a named random block list that palettes share through their {@code variant} field
 * (docs/original-spec/05-assets/palettes-and-variants.md, "Variant JSON"). Variants resolve from the loaded asset set
 * at load, never from a running server (fixes ASSET-14).
 *
 * <p>
 * Immutable. Side: both.
 */
public final class Variant {

    private final AssetId id;
    private final RandomList blocks;

    private Variant(AssetId id, RandomList blocks) {
        this.id = id;
        this.blocks = blocks;
    }

    /**
     * Reads a variant file.
     *
     * @param root    the document's fields
     * @param context the asset being read
     * @return the variant, or null after reporting an error
     */
    public static Variant parse(JsonFields root, ParseContext context) {
        RandomList blocks = RandomList.parse(root, "blocks", context, null);
        return blocks == null ? null : new Variant(context.id(), blocks);
    }

    /** Returns the id. */
    public AssetId id() {
        return id;
    }

    /** Returns the 128-slot list. */
    public RandomList blocks() {
        return blocks;
    }
}
