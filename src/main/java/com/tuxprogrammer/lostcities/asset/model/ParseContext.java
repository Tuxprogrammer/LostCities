package com.tuxprogrammer.lostcities.asset.model;

import java.util.ArrayList;
import java.util.List;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetType;
import com.tuxprogrammer.lostcities.asset.Problems;
import com.tuxprogrammer.lostcities.asset.block.BlockChoice;
import com.tuxprogrammer.lostcities.asset.block.BlockResolver;
import com.tuxprogrammer.lostcities.asset.json.JsonFields;

/**
 * What a parser needs while it reads one asset file: the asset's id, where problems go, the block resolver, and the
 * assets of the types already loaded, so every name an asset gives is resolved at load
 * (docs/new-spec/05-assets/index.md, "Lookup": a required reference that does not resolve rejects the referring asset;
 * an optional one logs one warning and behaves as absent; fixes ASSET-01).
 *
 * <p>
 * Not thread-safe; loading runs on one thread. Side: both.
 */
public final class ParseContext {

    /** Finds assets loaded so far. */
    public interface Lookup {

        /**
         * Returns a loaded asset.
         *
         * @param type the type
         * @param id   the id
         * @return the asset, or null when no valid asset has that id
         */
        Object find(AssetType type, AssetId id);

        /** Returns whether an asset with that id existed but was rejected, for a clearer message. */
        boolean rejected(AssetType type, AssetId id);
    }

    private final AssetId id;
    private final Problems.Scope scope;
    private final BlockResolver blocks;
    private final Lookup lookup;

    /**
     * Creates a context for one asset.
     *
     * @param id     the asset's id
     * @param scope  where problems go
     * @param blocks the block resolver
     * @param lookup the assets loaded so far
     */
    public ParseContext(AssetId id, Problems.Scope scope, BlockResolver blocks, Lookup lookup) {
        this.id = id;
        this.scope = scope;
        this.blocks = blocks;
        this.lookup = lookup;
    }

    /** Returns the id of the asset being read. */
    public AssetId id() {
        return id;
    }

    /** Returns where problems go. */
    public Problems.Scope scope() {
        return scope;
    }

    /** Returns the block resolver. */
    public BlockResolver blocks() {
        return blocks;
    }

    /**
     * Resolves a block string of this asset.
     *
     * @param text      the string
     * @param character the palette character, or null
     * @param field     the JSON path
     * @return the choice, or null after reporting an error
     */
    public BlockChoice block(String text, Character character, String field) {
        return blocks.resolve(text, id, character, scope, field);
    }

    /**
     * Normalizes an asset name to a full id.
     *
     * @param name  the name
     * @param field the JSON path, for messages
     * @return the id, or null after reporting an error
     */
    public AssetId name(String name, String field) {
        try {
            return AssetId.parse(name);
        } catch (IllegalArgumentException e) {
            scope.error(field, name, e.getMessage());
            return null;
        }
    }

    /**
     * Resolves a required reference: a missing or rejected target is an error.
     *
     * @param type  the target's type
     * @param name  the name as written
     * @param kind  the expected class
     * @param field the JSON path
     * @param <T>   the target class
     * @return the target, or null after reporting an error
     */
    public <T> T require(AssetType type, String name, Class<T> kind, String field) {
        return reference(type, name, kind, field, true);
    }

    /**
     * Resolves an optional reference: a missing target is one warning and the reference acts as absent.
     *
     * @param type  the target's type
     * @param name  the name as written
     * @param kind  the expected class
     * @param field the JSON path
     * @param <T>   the target class
     * @return the target, or null when it is missing (after the warning)
     */
    public <T> T optional(AssetType type, String name, Class<T> kind, String field) {
        return reference(type, name, kind, field, false);
    }

    /**
     * Resolves a list of required references named by a field holding one name or a list.
     *
     * @param fields   the object holding the field
     * @param key      the field
     * @param type     the targets' type
     * @param kind     the expected class
     * @param required whether a missing field is an error
     * @param <T>      the target class
     * @return the targets in order; null when the field is missing or any name failed
     */
    public <T> List<T> requireAll(JsonFields fields, String key, AssetType type, Class<T> kind, boolean required) {
        List<String> names = fields.stringOrList(key, required);
        if (names == null) {
            return null;
        }
        List<T> out = new ArrayList<>();
        boolean ok = true;
        for (int i = 0; i < names.size(); i++) {
            T target = require(type, names.get(i), kind, fields.field(key) + "[" + i + "]");
            if (target == null) {
                ok = false;
            } else {
                out.add(target);
            }
        }
        return ok ? out : null;
    }

    private <T> T reference(AssetType type, String name, Class<T> kind, String field, boolean required) {
        AssetId target = name(name, field);
        if (target == null) {
            return null;
        }
        Object found = lookup.find(type, target);
        if (found == null) {
            String why = lookup.rejected(type, target) ? "was rejected (see its own errors)" : "does not exist";
            String message = "names " + type.folder() + " asset " + target + ", which " + why;
            if (required) {
                scope.error(field, name, message);
            } else {
                scope.warning(field, name, message + "; the reference acts as absent");
            }
            return null;
        }
        if (!kind.isInstance(found)) {
            scope.error(field, name, "names " + target + ", which is not a " + kind.getSimpleName());
            return null;
        }
        return kind.cast(found);
    }
}
