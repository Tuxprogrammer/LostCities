package com.tuxprogrammer.lostcities.asset.model;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/cityassets/ScatteredBuilding.java
// Avoids: ASSET-25, CITY-32

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetType;
import com.tuxprogrammer.lostcities.asset.json.JsonFields;

/**
 * A scattered structure outside cities: one of a list of buildings, or a multibuilding, set on the terrain by a height
 * rule (docs/original-spec/05-assets/scattered-stuff-predefined.md, "Scattered JSON"; behavior in
 * {@code 07-scattered}).
 *
 * <p>
 * Changes from upstream: enum values are read case-insensitively with a fixed locale and an unknown one is a load
 * error naming the file (fixes ASSET-25 and CITY-32); an asset with neither {@code buildings} nor
 * {@code multibuilding} is rejected at load. Immutable. Side: both.
 */
public final class Scattered {

    /** Which terrain height the structure sits on. */
    public enum TerrainHeight {
        LOWEST,
        AVERAGE,
        HIGHEST,
        OCEAN
    }

    /** How the terrain under the structure is fixed. */
    public enum TerrainFix {
        NONE,
        CLEAR,
        REPEATSLICE
    }

    private final AssetId id;
    private final List<Building> buildings;
    private final MultiBuilding multiBuilding;
    private final boolean rotatable;
    private final boolean clearHighwayRailing;
    private final Part supportPart;
    private final TerrainHeight terrainHeight;
    private final TerrainFix terrainFix;
    private final int heightOffset;

    private Scattered(AssetId id, List<Building> buildings, MultiBuilding multiBuilding, boolean rotatable,
        boolean clearHighwayRailing, Part supportPart, TerrainHeight terrainHeight, TerrainFix terrainFix,
        int heightOffset) {
        this.id = id;
        this.buildings = buildings;
        this.multiBuilding = multiBuilding;
        this.rotatable = rotatable;
        this.clearHighwayRailing = clearHighwayRailing;
        this.supportPart = supportPart;
        this.terrainHeight = terrainHeight;
        this.terrainFix = terrainFix;
        this.heightOffset = heightOffset;
    }

    /**
     * Reads a scattered file.
     *
     * @param root    the document's fields
     * @param context the asset being read
     * @return the structure, or null after reporting an error
     */
    public static Scattered parse(JsonFields root, ParseContext context) {
        boolean ok = true;
        List<Building> buildings = Collections.emptyList();
        if (root.has("buildings")) {
            buildings = context.requireAll(root, "buildings", AssetType.BUILDINGS, Building.class, true);
            ok = buildings != null;
        }
        MultiBuilding multi = null;
        if (root.has("multibuilding")) {
            multi = context.require(
                AssetType.MULTIBUILDINGS,
                root.string("multibuilding", true),
                MultiBuilding.class,
                root.field("multibuilding"));
            ok &= multi != null;
        }
        if (ok && buildings.isEmpty() && multi == null) {
            root.error("buildings", null, "a scattered structure names buildings or a multibuilding");
            ok = false;
        }
        Part support = null;
        if (root.has("supportpart")) {
            support = context
                .require(AssetType.PARTS, root.string("supportpart", true), Part.class, root.field("supportpart"));
            ok &= support != null;
        }
        TerrainHeight height = enumValue(root, "terrainheight", TerrainHeight.class);
        TerrainFix fix = enumValue(root, "terrainfix", TerrainFix.class);
        if (!ok || height == null || fix == null) {
            return null;
        }
        return new Scattered(
            context.id(),
            buildings == null ? Collections.<Building>emptyList() : Collections.unmodifiableList(buildings),
            multi,
            root.flag("rotatable", false),
            root.flag("clearhighwayrailing", false),
            support,
            height,
            fix,
            root.integer("heightoffset", 0));
    }

    /**
     * Reads a required enum field case-insensitively with a fixed locale (fixes ASSET-25 and CITY-32).
     *
     * @param fields the object
     * @param key    the field
     * @param type   the enum
     * @param <E>    the enum type
     * @return the constant, or null after reporting an error
     */
    static <E extends Enum<E>> E enumValue(JsonFields fields, String key, Class<E> type) {
        String text = fields.string(key, true);
        if (text == null) {
            return null;
        }
        for (E constant : type.getEnumConstants()) {
            if (constant.name()
                .equals(text.toUpperCase(Locale.ROOT))) {
                return constant;
            }
        }
        StringBuilder known = new StringBuilder();
        for (E constant : type.getEnumConstants()) {
            known.append(known.length() == 0 ? "" : ", ")
                .append(
                    constant.name()
                        .toLowerCase(Locale.ROOT));
        }
        fields.error(key, text, "is not one of " + known);
        return null;
    }

    /** Returns the id. */
    public AssetId id() {
        return id;
    }

    /** Returns the candidate buildings for a single-chunk structure; empty when a multibuilding is used. */
    public List<Building> buildings() {
        return buildings;
    }

    /** Returns the multibuilding, or null. */
    public MultiBuilding multiBuilding() {
        return multiBuilding;
    }

    /** Returns whether the structure may be placed in any of the four rotations. */
    public boolean rotatable() {
        return rotatable;
    }

    /** Returns whether highway railing is removed where the structure meets a highway. */
    public boolean clearHighwayRailing() {
        return clearHighwayRailing;
    }

    /** Returns the support part repeated under the structure, or null. */
    public Part supportPart() {
        return supportPart;
    }

    /** Returns the height rule. */
    public TerrainHeight terrainHeight() {
        return terrainHeight;
    }

    /** Returns the terrain fix. */
    public TerrainFix terrainFix() {
        return terrainFix;
    }

    /** Returns the offset in blocks from the chosen height. */
    public int heightOffset() {
        return heightOffset;
    }
}
