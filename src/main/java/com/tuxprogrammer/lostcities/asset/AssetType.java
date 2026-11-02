package com.tuxprogrammer.lostcities.asset;

/**
 * The thirteen asset types, each read from its own folder: {@code data/<namespace>/lostcities/<folder>/<path>.json} in
 * the jar and {@code config/lostcitiesnh/assets/<namespace>/<folder>/<path>.json} for overrides
 * (docs/original-spec/05-assets/loading-and-lookup.md, "Registries and folders"; docs/new-spec/05-assets/
 * asset-format.md, "Loading and override"; P05-1, decision log D31).
 *
 * <p>
 * The order is the load order: a type is parsed after every type it can name, so references resolve in one pass.
 * Side: both.
 */
public enum AssetType {

    /** Weighted block lists that palettes share. */
    VARIANTS("variants"),
    /** Weighted values guarded by tests (loot and spawner choices), which palettes name. */
    CONDITIONS("conditions"),
    /** Character-to-block maps. */
    PALETTES("palettes"),
    /** Block templates written as palette characters. */
    PARTS("parts"),
    /** Floor-by-floor stacks of parts. */
    BUILDINGS("buildings"),
    /** Grids of buildings over adjacent chunks. */
    MULTIBUILDINGS("multibuildings"),
    /** Recipes for a chunk's palette. */
    STYLES("styles"),
    /** Per-city settings and selectors. */
    CITYSTYLES("citystyles"),
    /** Structures outside cities. */
    SCATTERED("scattered"),
    /** Columns scattered into city chunks. */
    STUFF("stuff"),
    /** The top of the asset tree for a dimension. */
    WORLDSTYLES("worldstyles"),
    /** Fixed cities. */
    PREDEFINEDCITIES("predefinedcities"),
    /** Fixed city spheres; parsed and kept, not used before beta (D12). */
    PREDEFINEDSPHERES("predefinedspheres");

    private final String folder;

    AssetType(String folder) {
        this.folder = folder;
    }

    /** Returns the folder name, as upstream's data packs use it. */
    public String folder() {
        return folder;
    }

    /**
     * Returns the type stored in a folder.
     *
     * @param folder the folder name
     * @return the type, or null when no type uses that folder
     */
    public static AssetType byFolder(String folder) {
        for (AssetType type : values()) {
            if (type.folder.equals(folder)) {
                return type;
            }
        }
        return null;
    }
}
