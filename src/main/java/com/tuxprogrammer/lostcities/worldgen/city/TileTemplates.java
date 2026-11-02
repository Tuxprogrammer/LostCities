package com.tuxprogrammer.lostcities.worldgen.city;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.tuxprogrammer.lostcities.asset.block.TileTemplate;
import com.tuxprogrammer.lostcities.asset.transform.Direction;
import com.tuxprogrammer.lostcities.asset.transform.Transform;

/**
 * Turns the facings a tile-entity template stores with a part's transform, where {@code MetaRotation} cannot because
 * the block keeps its facing in the tile entity, not in metadata (docs/new-spec/05-assets/asset-format.md,
 * "Tile-entity data": BiblioCraft, Storage Drawers, Iron Chests, JABBA, FloodLights). Item names stay names: the world
 * glue resolves them to the world's numeric IDs when it applies the template.
 *
 * <p>
 * A facing key is a top-level integer holding a six-direction index in Forge's {@code ForgeDirection} order (0 down, 1
 * up, 2 north, 3 south, 4 west, 5 east), the order those mods store. The keys are listed in {@link #FACING_KEYS};
 * which mod writes which key, and that each is this order, is unverified until the M11 palettes use them in game.
 *
 * <p>
 * Pure. Side: both (plain Java).
 */
public final class TileTemplates {

    /** Top-level keys holding a {@code ForgeDirection} index: {@code facing}, {@code Dir}, {@code teDirection}... */
    public static final Set<String> FACING_KEYS = Collections
        .unmodifiableSet(new HashSet<>(Arrays.asList("facing", "Facing", "Dir", "dir", "direction", "teDirection")));

    private TileTemplates() {}

    /**
     * Returns a template as JSON text with its facings turned.
     *
     * @param template  the template, or {@code null}
     * @param transform the part's transform
     * @return the JSON text; the empty string for no template
     */
    public static String rotated(TileTemplate template, Transform transform) {
        if (template == null) {
            return "";
        }
        JsonObject json = template.json();
        if (transform != Transform.ROTATE_NONE) {
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                if (!FACING_KEYS.contains(entry.getKey())) {
                    continue;
                }
                JsonElement value = entry.getValue();
                if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive()
                    .isNumber()) {
                    continue;
                }
                int index = value.getAsInt();
                if (index < 0 || index >= Direction.values().length) {
                    continue;
                }
                Direction turned = transform.apply(Direction.values()[index]);
                entry.setValue(new JsonPrimitive(turned.ordinal()));
            }
        }
        return json.toString();
    }
}
