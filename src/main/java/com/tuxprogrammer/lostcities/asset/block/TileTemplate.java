package com.tuxprogrammer.lostcities.asset.block;

import com.google.gson.JsonObject;
import com.tuxprogrammer.lostcities.asset.json.JsonFields;

/**
 * A tile-entity template a palette entry carries: the block's 1.7.10 tile-entity NBT minus position and id, written as
 * JSON, with items named by registry name. The writer builds the NBT at placement, resolving item names to the
 * world's numeric IDs and turning any stored facing with the part (docs/new-spec/05-assets/asset-format.md,
 * "Tile-entity data"; D28 rule 6).
 *
 * <p>
 * Immutable: the template is kept as its canonical JSON text, and {@link #json()} returns a fresh copy each time.
 * Side: both.
 */
public final class TileTemplate {

    private final String text;

    private TileTemplate(String text) {
        this.text = text;
    }

    /**
     * Captures a template.
     *
     * @param json the template object
     * @return the template, or null for an empty object, which counts as absent as upstream
     */
    public static TileTemplate of(JsonObject json) {
        if (json == null || json.entrySet()
            .isEmpty()) {
            return null;
        }
        return new TileTemplate(json.toString());
    }

    /** Returns a fresh copy of the template. */
    public JsonObject json() {
        return JsonFields.parseStrict(text)
            .getAsJsonObject();
    }

    /** Returns the canonical JSON text. */
    @Override
    public String toString() {
        return text;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TileTemplate && ((TileTemplate) other).text.equals(text);
    }

    @Override
    public int hashCode() {
        return text.hashCode();
    }
}
