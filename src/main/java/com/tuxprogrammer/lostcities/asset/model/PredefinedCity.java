package com.tuxprogrammer.lostcities.asset.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetType;
import com.tuxprogrammer.lostcities.asset.json.JsonFields;

/**
 * A predefined city: a fixed city at a chunk, with a city style and optional fixed buildings and street chunks
 * (docs/original-spec/05-assets/scattered-stuff-predefined.md, "Predefined city JSON"; behavior in
 * {@code 04-city-layout}). Upstream bundles none.
 *
 * <p>
 * Deviation: {@code dimension} accepts {@code minecraft:overworld} or the numeric id 0 only, while the overworld is the
 * only target ([0003]; docs/new-spec/05-assets/index.md, "Predefined city JSON"). Immutable. Side: both.
 */
public final class PredefinedCity {

    /** A fixed building of the city, relative to its center chunk. */
    public static final class FixedBuilding {

        private final AssetId building;
        private final int chunkX;
        private final int chunkZ;
        private final boolean multi;
        private final boolean preventRuins;

        FixedBuilding(AssetId building, int chunkX, int chunkZ, boolean multi, boolean preventRuins) {
            this.building = building;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.multi = multi;
            this.preventRuins = preventRuins;
        }

        /** Returns the building, or the multibuilding when {@link #multi()}. */
        public AssetId building() {
            return building;
        }

        /** Returns the chunk X relative to the center. */
        public int chunkX() {
            return chunkX;
        }

        /** Returns the chunk Z relative to the center. */
        public int chunkZ() {
            return chunkZ;
        }

        /** Returns whether {@link #building()} names a multibuilding whose top-left chunk is this one. */
        public boolean multi() {
            return multi;
        }

        /** Returns whether ruins are prevented on this building. */
        public boolean preventRuins() {
            return preventRuins;
        }
    }

    private final AssetId id;
    private final int chunkX;
    private final int chunkZ;
    private final int radius;
    private final CityStyle cityStyle;
    private final List<FixedBuilding> buildings;
    private final List<int[]> streets;

    private PredefinedCity(AssetId id, int chunkX, int chunkZ, int radius, CityStyle cityStyle,
        List<FixedBuilding> buildings, List<int[]> streets) {
        this.id = id;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.radius = radius;
        this.cityStyle = cityStyle;
        this.buildings = Collections.unmodifiableList(buildings);
        this.streets = Collections.unmodifiableList(streets);
    }

    /**
     * Returns whether a predefined asset's {@code dimension} names the overworld.
     *
     * @param fields the asset's fields; an error is reported otherwise
     * @return whether it is the overworld
     */
    static boolean overworld(JsonFields fields) {
        String dimension = fields.string("dimension", true);
        if (dimension == null) {
            return false;
        }
        if ("minecraft:overworld".equals(dimension) || "0".equals(dimension)) {
            return true;
        }
        fields.error("dimension", dimension, "only the overworld (minecraft:overworld or 0) is a target");
        return false;
    }

    /**
     * Reads a predefined city file.
     *
     * @param root    the document's fields
     * @param context the asset being read
     * @return the city, or null after reporting an error
     */
    public static PredefinedCity parse(JsonFields root, ParseContext context) {
        boolean ok = overworld(root);
        Integer chunkX = root.integer("chunkx", true);
        Integer chunkZ = root.integer("chunkz", true);
        Integer radius = root.integer("radius", true);
        String style = root.string("citystyle", true);
        CityStyle cityStyle = style == null ? null
            : context.require(AssetType.CITYSTYLES, style, CityStyle.class, root.field("citystyle"));
        List<FixedBuilding> buildings = new ArrayList<>();
        for (JsonFields entry : root.objects("buildings", false)) {
            String name = entry.string("building", true);
            Integer x = entry.integer("chunkx", true);
            Integer z = entry.integer("chunkz", true);
            boolean multi = entry.flag("multi", false);
            if (name == null || x == null || z == null) {
                ok = false;
                continue;
            }
            Object target = multi
                ? context.require(AssetType.MULTIBUILDINGS, name, MultiBuilding.class, entry.field("building"))
                : context.require(AssetType.BUILDINGS, name, Building.class, entry.field("building"));
            if (target == null) {
                ok = false;
                continue;
            }
            buildings.add(new FixedBuilding(AssetId.parse(name), x, z, multi, entry.flag("preventruins", false)));
        }
        List<int[]> streets = new ArrayList<>();
        for (JsonFields entry : root.objects("streets", false)) {
            Integer x = entry.integer("chunkx", true);
            Integer z = entry.integer("chunkz", true);
            if (x == null || z == null) {
                ok = false;
                continue;
            }
            streets.add(new int[] { x, z });
        }
        if (!ok || chunkX == null || chunkZ == null || radius == null || cityStyle == null) {
            return null;
        }
        return new PredefinedCity(context.id(), chunkX, chunkZ, radius, cityStyle, buildings, streets);
    }

    /** Returns the id. */
    public AssetId id() {
        return id;
    }

    /** Returns the center chunk X. */
    public int chunkX() {
        return chunkX;
    }

    /** Returns the center chunk Z. */
    public int chunkZ() {
        return chunkZ;
    }

    /** Returns the city radius in blocks. */
    public int radius() {
        return radius;
    }

    /** Returns the city style for the whole city. */
    public CityStyle cityStyle() {
        return cityStyle;
    }

    /** Returns the fixed buildings. */
    public List<FixedBuilding> buildings() {
        return buildings;
    }

    /** Returns the fixed street chunks, each {@code {chunkx, chunkz}} relative to the center. */
    public List<int[]> streets() {
        return streets;
    }
}
