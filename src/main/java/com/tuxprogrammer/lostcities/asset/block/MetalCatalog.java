package com.tuxprogrammer.lostcities.asset.block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.Problems;
import com.tuxprogrammer.lostcities.asset.json.JsonFields;

/**
 * Which blocks each decorative metal covers, read from {@code data/lostcitiesnh/blockmap/metals.json} (D29: one flag
 * covers that metal's blocks in every mod). The resolver asks it which metal a resolved block is made of, so an entry
 * naming a metal block directly is still governed by its flag.
 *
 * <p>
 * Side: both.
 */
public final class MetalCatalog {

    /** The bundled data file, as a class-path resource. */
    public static final String RESOURCE = "/data/lostcitiesnh/blockmap/metals.json";

    private final Map<DecorativeMetal, List<BlockPattern>> blocks;

    private MetalCatalog(Map<DecorativeMetal, List<BlockPattern>> blocks) {
        this.blocks = blocks;
    }

    /** Returns a catalog that covers nothing, for tests. */
    public static MetalCatalog empty() {
        return new MetalCatalog(new EnumMap<>(DecorativeMetal.class));
    }

    /**
     * Reads the catalog.
     *
     * @param json  the parsed {@code metals.json}
     * @param scope where problems go
     * @return the catalog; metals with errors cover only their valid patterns
     */
    public static MetalCatalog parse(JsonElement json, Problems.Scope scope) {
        Map<DecorativeMetal, List<BlockPattern>> blocks = new EnumMap<>(DecorativeMetal.class);
        if (!json.isJsonObject()) {
            scope.error("", null, "must be an object");
            return new MetalCatalog(blocks);
        }
        JsonFields root = new JsonFields(json.getAsJsonObject(), "", scope);
        JsonFields metals = root.object("metals", true);
        if (metals == null) {
            return new MetalCatalog(blocks);
        }
        JsonObject object = metals.object();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            DecorativeMetal metal = DecorativeMetal.byKey(entry.getKey());
            if (metal == null) {
                metals.error(entry.getKey(), null, "is not a decorative metal");
                continue;
            }
            List<String> patterns = metals.stringList(entry.getKey(), true);
            List<BlockPattern> parsed = new ArrayList<>();
            if (patterns != null) {
                for (String pattern : patterns) {
                    try {
                        parsed.add(BlockPattern.parse(pattern));
                    } catch (IllegalArgumentException e) {
                        metals.error(entry.getKey(), pattern, e.getMessage());
                    }
                }
            }
            blocks.put(metal, Collections.unmodifiableList(parsed));
        }
        for (DecorativeMetal metal : DecorativeMetal.values()) {
            if (!blocks.containsKey(metal)) {
                metals.error(metal.key(), null, "metal has no block list");
            }
        }
        return new MetalCatalog(blocks);
    }

    /**
     * Returns the metal a block is made of.
     *
     * @param block the block
     * @return the metal, or null when the block is not a decorative metal
     */
    public DecorativeMetal metalOf(BlockRef block) {
        for (Map.Entry<DecorativeMetal, List<BlockPattern>> entry : blocks.entrySet()) {
            for (BlockPattern pattern : entry.getValue()) {
                if (pattern.matches(block)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }
}
