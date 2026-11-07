package com.tuxprogrammer.lostcities.asset.model;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/cityassets/Building.java
// Avoids: ASSET-04, ASSET-23

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetType;
import com.tuxprogrammer.lostcities.asset.json.JsonFields;

/**
 * A building: a stack of parts chosen floor by floor by conditions, with an optional overlay part per floor
 * (docs/original-spec/05-assets/buildings-and-multibuildings.md). Field names and camel-case keys are upstream's.
 *
 * <p>
 * Part selection keeps upstream's draw: the matching references in file order, one picked with
 * {@code random.nextInt(count)}, so duplicates weigh double. Changes from upstream: {@code filler} and {@code rubble}
 * must be exactly one character (fixes ASSET-23), and {@code belowpart} in a reference tests the part on the floor
 * below, which the caller puts in the context (fixes ASSET-04). Immutable. Side: both.
 */
public final class Building {

    /** "No building-specific value" for the floor and cellar limits, as upstream. */
    public static final int UNSET = -1;

    /** The lowest {@code floorHeight} a building may set: a slab, two clear rows and a ceiling (D-floor-heights-1). */
    public static final int MIN_FLOOR_HEIGHT = 4;

    /** The highest {@code floorHeight} a building may set. */
    public static final int MAX_FLOOR_HEIGHT = 16;

    /** The most floors {@code groundFloors} may give the ground height (decision log D-mall-1). */
    public static final int MAX_GROUND_FLOORS = 4;

    /** A part reference: a part and the conditions under which a floor may use it. */
    public static final class PartRef {

        private final Part part;
        private final ConditionGuard test;

        PartRef(Part part, ConditionGuard test) {
            this.part = part;
            this.test = test;
        }

        /** Returns the part. */
        public Part part() {
            return part;
        }

        /** Returns the conditions. */
        public ConditionGuard test() {
            return test;
        }
    }

    private final AssetId id;
    private final char filler;
    private final Character rubble;
    private final Palette palette;
    private final int minFloors;
    private final int maxFloors;
    private final int minCellars;
    private final int maxCellars;
    private final int floorHeight;
    private final int groundFloorHeight;
    private final int groundFloors;
    private final boolean allowDoors;
    private final boolean allowFillers;
    private final boolean overrideFloors;
    private final float prefersLonely;
    private final List<PartRef> parts;
    private final List<PartRef> parts2;

    private Building(AssetId id, char filler, Character rubble, Palette palette, int[] limits, boolean[] flags,
        float prefersLonely, List<PartRef> parts, List<PartRef> parts2) {
        this.id = id;
        this.filler = filler;
        this.rubble = rubble;
        this.palette = palette;
        this.minFloors = limits[0];
        this.maxFloors = limits[1];
        this.minCellars = limits[2];
        this.maxCellars = limits[3];
        this.floorHeight = limits[4];
        this.groundFloorHeight = limits[5];
        this.groundFloors = limits[6];
        this.allowDoors = flags[0];
        this.allowFillers = flags[1];
        this.overrideFloors = flags[2];
        this.prefersLonely = prefersLonely;
        this.parts = Collections.unmodifiableList(parts);
        this.parts2 = Collections.unmodifiableList(parts2);
    }

    /**
     * Reads a building file.
     *
     * @param root    the document's fields
     * @param context the asset being read
     * @return the building, or null after reporting an error
     */
    public static Building parse(JsonFields root, ParseContext context) {
        Character filler = root.character("filler", true);
        Character rubble = root.character("rubble", false);
        Palette palette = null;
        boolean ok = filler != null && (rubble != null || !root.has("rubble"));
        JsonFields inline = root.object("palette", false);
        if (inline != null) {
            palette = Palette.parse(inline, Part.local(context.id()), context);
        } else if (root.has("refpalette")) {
            palette = context
                .require(AssetType.PALETTES, root.string("refpalette", true), Palette.class, root.field("refpalette"));
            ok &= palette != null;
        }
        int[] limits = { root.integer("minfloors", UNSET), root.integer("maxfloors", UNSET),
            root.integer("mincellars", UNSET), root.integer("maxcellars", UNSET), root.integer("floorHeight", UNSET),
            root.integer("groundFloorHeight", UNSET), root.integer("groundFloors", 1) };
        // D-floor-heights-1: a building's own floor heights, slab to slab; unset takes the world's pitch.
        for (int i = 4; i <= 5; i++) {
            if (limits[i] != UNSET && (limits[i] < MIN_FLOOR_HEIGHT || limits[i] > MAX_FLOOR_HEIGHT)) {
                root.error(
                    i == 4 ? "floorHeight" : "groundFloorHeight",
                    Integer.toString(limits[i]),
                    "must be between " + MIN_FLOOR_HEIGHT + " and " + MAX_FLOOR_HEIGHT);
                ok = false;
            }
        }
        // D-mall-1: a ground storey several parts tall takes groundFloorHeight on each of them.
        if (limits[6] < 1 || limits[6] > MAX_GROUND_FLOORS) {
            root.error("groundFloors", Integer.toString(limits[6]), "must be between 1 and " + MAX_GROUND_FLOORS);
            ok = false;
        }
        boolean[] flags = { root.flag("allowDoors", true), root.flag("allowFillers", true),
            root.flag("overrideFloors", false) };
        float prefersLonely = root.number("preferslonely", 0f);
        if (prefersLonely < 0 || prefersLonely > 1) {
            root.error("preferslonely", Float.toString(prefersLonely), "must be between 0 and 1");
            ok = false;
        }
        List<PartRef> parts = refs(root, "parts", true, context);
        List<PartRef> parts2 = refs(root, "parts2", false, context);
        if (parts == null || parts2 == null) {
            return null;
        }
        if (parts.isEmpty()) {
            root.error("parts", null, "a building has at least one part");
            ok = false;
        }
        return ok ? new Building(context.id(), filler, rubble, palette, limits, flags, prefersLonely, parts, parts2)
            : null;
    }

    private static List<PartRef> refs(JsonFields root, String key, boolean required, ParseContext context) {
        List<PartRef> out = new ArrayList<>();
        boolean ok = true;
        for (JsonFields ref : root.objects(key, required)) {
            String name = ref.string("part", true);
            Part part = name == null ? null : context.require(AssetType.PARTS, name, Part.class, ref.field("part"));
            ConditionGuard test = ConditionGuard.parse(ref, context);
            if (part == null) {
                ok = false;
            } else {
                out.add(new PartRef(part, test));
            }
        }
        return ok ? out : null;
    }

    /**
     * Picks a floor's main part.
     *
     * @param random  the building random
     * @param context the floor's facts; its below part is the previous floor's main part
     * @return a matching part, uniformly among the matching references in file order; null when none matches
     */
    public Part randomPart(Random random, ConditionContext context) {
        return pick(parts, random, context);
    }

    /**
     * Picks a floor's overlay part.
     *
     * @param random  the building random
     * @param context the floor's facts with the chosen main part as the part; its below part is the main part of the
     *                floor below (fixes ASSET-04)
     * @return a matching part, or null when none matches, which is legal
     */
    public Part randomPart2(Random random, ConditionContext context) {
        return pick(parts2, random, context);
    }

    private static Part pick(List<PartRef> refs, Random random, ConditionContext context) {
        List<Part> matching = new ArrayList<>();
        for (PartRef ref : refs) {
            if (ref.test.test(context)) {
                matching.add(ref.part);
            }
        }
        if (matching.isEmpty()) {
            return null;
        }
        return matching.get(random.nextInt(matching.size()));
    }

    /** Returns the id. */
    public AssetId id() {
        return id;
    }

    /** Returns the filler character, used to fill and close areas around the building. */
    public char filler() {
        return filler;
    }

    /** Returns the rubble character, or null. */
    public Character rubble() {
        return rubble;
    }

    /** Returns the building's palette layer (inline or referenced), or null. */
    public Palette palette() {
        return palette;
    }

    /** Returns the minimum floors above ground, or {@link #UNSET}. */
    public int minFloors() {
        return minFloors;
    }

    /** Returns the maximum floors above ground, or {@link #UNSET}. */
    public int maxFloors() {
        return maxFloors;
    }

    /** Returns the minimum cellars, or {@link #UNSET}. */
    public int minCellars() {
        return minCellars;
    }

    /** Returns the maximum cellars, or {@link #UNSET}. */
    public int maxCellars() {
        return maxCellars;
    }

    /** Returns whether doors may be generated where the building meets a street or another building. */
    public boolean allowDoors() {
        return allowDoors;
    }

    /** Returns whether filler may be generated around cellars. */
    public boolean allowFillers() {
        return allowFillers;
    }

    /**
     * Returns {@code floorHeight}: blocks each floor above the ground floor and each cellar rises, slab to slab, or
     * {@link #UNSET} for the world's floor pitch (decision log D-floor-heights-1).
     */
    public int floorHeight() {
        return floorHeight;
    }

    /**
     * Returns {@code groundFloorHeight}: blocks the ground floor rises, or {@link #UNSET} for {@link #floorHeight()}.
     */
    public int groundFloorHeight() {
        return groundFloorHeight;
    }

    /**
     * Returns {@code groundFloors}: how many floors, from the ground floor up, rise {@link #groundFloorHeight()}; 1 when
     * unset (decision log D-mall-1).
     */
    public int groundFloors() {
        return groundFloors;
    }

    /** Returns whether only this building's floor and cellar limits apply. */
    public boolean overrideFloors() {
        return overrideFloors;
    }

    /** Returns how strongly the building wants empty neighbors, 0 to 1. */
    public float prefersLonely() {
        return prefersLonely;
    }

    /** Returns the main part references, in file order. */
    public List<PartRef> parts() {
        return parts;
    }

    /** Returns the overlay part references, in file order. */
    public List<PartRef> parts2() {
        return parts2;
    }
}
