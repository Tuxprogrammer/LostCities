package com.tuxprogrammer.lostcities.asset.model;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/cityassets/MultiBuilding.java
// Avoids: ASSET-31

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetType;
import com.tuxprogrammer.lostcities.asset.json.JsonFields;

/**
 * A multibuilding: a grid of buildings over {@code dimx} × {@code dimz} adjacent chunks. The outer list of
 * {@code buildings} is indexed by X and the inner by Z, so {@code buildings[x][z]} is the building in chunk
 * {@code (origin.x + x, origin.z + z)}; an empty name leaves that chunk out of the set
 * (docs/original-spec/05-assets/buildings-and-multibuildings.md, "Multibuilding JSON").
 *
 * <p>
 * Change from upstream: {@code dimx} and {@code dimz} must match the grid, or the multibuilding is rejected at load
 * (fixes ASSET-31). Immutable. Side: both.
 */
public final class MultiBuilding {

    private final AssetId id;
    private final int dimX;
    private final int dimZ;
    private final Building[][] grid;

    private MultiBuilding(AssetId id, int dimX, int dimZ, Building[][] grid) {
        this.id = id;
        this.dimX = dimX;
        this.dimZ = dimZ;
        this.grid = grid;
    }

    /**
     * Reads a multibuilding file.
     *
     * @param root    the document's fields
     * @param context the asset being read
     * @return the multibuilding, or null after reporting an error
     */
    public static MultiBuilding parse(JsonFields root, ParseContext context) {
        Integer dimX = root.integer("dimx", true);
        Integer dimZ = root.integer("dimz", true);
        JsonArray outer = root.array("buildings", true);
        if (dimX == null || dimZ == null || outer == null) {
            return null;
        }
        if (dimX <= 0 || dimZ <= 0) {
            root.error("dimx", dimX + "x" + dimZ, "the grid must be at least 1 x 1");
            return null;
        }
        if (outer.size() != dimX) {
            root.error("buildings", Integer.toString(outer.size()), "has that many columns along X; dimx is " + dimX);
            return null;
        }
        Building[][] grid = new Building[dimX][dimZ];
        boolean ok = true;
        for (int x = 0; x < dimX; x++) {
            String path = root.field("buildings") + "[" + x + "]";
            JsonElement column = outer.get(x);
            if (!column.isJsonArray() || column.getAsJsonArray()
                .size() != dimZ) {
                context.scope()
                    .error(path, column.toString(), "must be a list of exactly dimz = " + dimZ + " building names");
                ok = false;
                continue;
            }
            JsonArray names = column.getAsJsonArray();
            for (int z = 0; z < dimZ; z++) {
                JsonElement name = names.get(z);
                if (!name.isJsonPrimitive()) {
                    context.scope()
                        .error(path + "[" + z + "]", name.toString(), "must be a building name");
                    ok = false;
                    continue;
                }
                String text = name.getAsString();
                if (text.isEmpty()) {
                    continue;
                }
                grid[x][z] = context.require(AssetType.BUILDINGS, text, Building.class, path + "[" + z + "]");
                ok &= grid[x][z] != null;
            }
        }
        return ok ? new MultiBuilding(context.id(), dimX, dimZ, grid) : null;
    }

    /** Returns the id. */
    public AssetId id() {
        return id;
    }

    /** Returns the width in chunks along X. */
    public int dimX() {
        return dimX;
    }

    /** Returns the depth in chunks along Z. */
    public int dimZ() {
        return dimZ;
    }

    /**
     * Returns the building at a grid position.
     *
     * @param x 0 to {@code dimx − 1}
     * @param z 0 to {@code dimz − 1}
     * @return the building, or null for an empty slot
     */
    public Building building(int x, int z) {
        return grid[x][z];
    }

    /** Returns every building the grid names, each once, in grid order (upstream's {@code getBuildingSet}). */
    public Set<Building> buildings() {
        Set<Building> out = new LinkedHashSet<>();
        for (Building[] column : grid) {
            for (Building building : column) {
                if (building != null) {
                    out.add(building);
                }
            }
        }
        return Collections.unmodifiableSet(out);
    }

    /** Returns the ids of {@link #buildings()}, for reachability. */
    public List<AssetId> buildingIds() {
        List<AssetId> out = new ArrayList<>();
        for (Building building : buildings()) {
            out.add(building.id());
        }
        return out;
    }
}
