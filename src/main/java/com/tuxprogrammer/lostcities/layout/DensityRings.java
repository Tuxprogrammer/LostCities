package com.tuxprogrammer.lostcities.layout;

import java.util.Locale;

/**
 * City density rings and building classes (city-realism phase 2c, decision D-p2c-1): a tall core that tapers to
 * low-rises, houses and parks at the edge. Original to this mod; upstream has no such notion.
 *
 * <p>
 * The ring distance {@code d} is 0 at a city's center and 1 where the city ends (its city factor falls to
 * {@code cityThreshold}): {@code d = (1 - factor) / (1 - cityThreshold)}. For a round city the factor is
 * {@code 1 - distance / radius}, so this is the distance over the city's radius; Perlin cities use the factor the
 * same way. Rings: core {@code d < 0.2}, inner to 0.45, outer to 0.75, edge beyond.
 *
 * <p>
 * Every effect is scaled by the {@code densityRings} strength {@code s}: 0 switches the whole feature off (the
 * pre-plan layout, byte for byte), 1 is the default, above 1 is stronger (the {@code rarecities} profile). Pure
 * functions; side: both (plain Java).
 */
public final class DensityRings {

    /** The rings, from the city's center out. */
    public enum Ring {
        CORE, INNER, OUTER, EDGE;

        /** Returns the ring a normalized distance falls in. */
        public static Ring of(float d) {
            return d < CORE_END ? CORE : d < INNER_END ? INNER : d < OUTER_END ? OUTER : EDGE;
        }
    }

    /** Building classes by height, from the audit of the shipped buildings. */
    public enum BuildingClass {
        TOWER, HIGH_RISE, MID_RISE, LOW_RISE, HOUSE
    }

    /** End of the core ring. */
    public static final float CORE_END = 0.2f;
    /** End of the inner ring. */
    public static final float INNER_END = 0.45f;
    /** End of the outer ring. */
    public static final float OUTER_END = 0.75f;

    /** Acceptance weight of a class in a ring: rows by {@link BuildingClass}, columns by {@link Ring}. */
    private static final float[][] WEIGHTS = {
        // core, inner, outer, edge
        { 1f, 0.5f, 0.05f, 0f }, // tower
        { 1f, 0.8f, 0.2f, 0.02f }, // high-rise
        { 0.15f, 1f, 1f, 0.25f }, // mid-rise
        { 0.02f, 0.3f, 1f, 1f }, // low-rise
        { 0f, 0.05f, 0.4f, 1f } }; // house

    /** Share of the five original buildings among singles, by ring, at strength 1. */
    private static final float[] ORIGINAL_SHARE = { 0f, 0.1f, 0.3f, 0.65f };

    /** The five original buildings (design 2d); they carry their own doors and are never rotated. */
    public static final String[] ORIGINALS = { "nh_house_garden", "nh_cottage", "nh_corner_shop", "nh_workshop",
        "nh_walkup" };

    /** Chance of a construction lot anywhere in a city, at strength 1. */
    public static final float LOT_BASE = 0.03f;
    /** Factor on {@link #LOT_BASE} in the inner and outer rings: cities grow outward. */
    public static final float LOT_GROWTH_FACTOR = 1.8f;

    /** Share of the cap that drawn floors are lifted to, per unit strength. */
    private static final float LOW_FRACTION = 0.7f;

    private DensityRings() {}

    /**
     * Returns the normalized ring distance.
     *
     * @param cityFactor    the chunk's city factor
     * @param cityThreshold the profile's {@code cityThreshold}
     * @return {@code d} in [0, 1]
     */
    public static float distance(float cityFactor, float cityThreshold) {
        float span = Math.max(1e-3f, 1f - cityThreshold);
        return Math.max(0f, Math.min(1f, (1f - cityFactor) / span));
    }

    /**
     * Returns the class of a building or multibuilding by id: the audit's table and the five originals. Unknown ids
     * are mid-rise.
     *
     * @param id a building or multibuilding name
     * @return its class
     */
    public static BuildingClass classOf(String id) {
        if (id == null) {
            return BuildingClass.MID_RISE;
        }
        switch (id.toLowerCase(Locale.ROOT)) {
            case "building1":
            case "building8":
            case "multi1":
            case "huge1":
            case "huge2":
                return BuildingClass.TOWER;
            case "multi2":
            case "multi3":
            case "multi4":
            case "multi5":
            case "center":
            case "library":
            case "townhall":
                return BuildingClass.HIGH_RISE;
            case "nh_corner_shop":
            case "nh_workshop":
            case "nh_walkup":
                return BuildingClass.LOW_RISE;
            case "nh_house_garden":
            case "nh_cottage":
            case "cabin":
                return BuildingClass.HOUSE;
            default:
                return BuildingClass.MID_RISE;
        }
    }

    /**
     * Returns the chance, 0 to 1, that a class is accepted in a ring at a strength: {@code weight ^ strength}.
     *
     * @param cls      a class
     * @param ring     a ring
     * @param strength {@code densityRings}
     * @return the acceptance chance
     */
    public static float acceptance(BuildingClass cls, Ring ring, float strength) {
        float w = WEIGHTS[cls.ordinal()][ring.ordinal()];
        if (w <= 0f) {
            return 0f;
        }
        return (float) Math.pow(w, strength);
    }

    /**
     * Returns the chance a single-chunk pick is replaced by one of the five originals.
     *
     * @param ring     a ring
     * @param strength {@code densityRings}
     * @return the chance
     */
    public static float originalChance(Ring ring, float strength) {
        return Math.min(1f, ORIGINAL_SHARE[ring.ordinal()] * strength);
    }

    /**
     * Returns the floor cap along the curve: the maximum in the core, tapering through the inner ring to 55 percent,
     * through the outer ring to 2 floors, and 1 floor at the edge. The curve is blended with the maximum by the
     * strength (clamped to 1), so a weak strength flattens it.
     *
     * @param d         ring distance
     * @param maxFloors {@code buildingMaxFloors}
     * @param strength  {@code densityRings}
     * @return the cap, at least 1
     */
    public static int floorCap(float d, int maxFloors, float strength) {
        double mid = Math.max(2, 0.55 * maxFloors);
        double curve;
        if (d < CORE_END) {
            curve = maxFloors;
        } else if (d < INNER_END) {
            curve = lerp(maxFloors, mid, (d - CORE_END) / (INNER_END - CORE_END));
        } else if (d < OUTER_END) {
            curve = lerp(mid, 2, (d - INNER_END) / (OUTER_END - INNER_END));
        } else {
            curve = 1;
        }
        double blend = Math.min(1f, strength);
        double cap = maxFloors + (curve - maxFloors) * blend;
        return Math.max(1, (int) Math.round(cap));
    }

    /**
     * Returns the least floors a building has under a cap: a share of it, so the core's towers reach their height
     * and the mean floors fall steadily outward.
     *
     * @param cap      the ring's cap
     * @param strength {@code densityRings}
     * @return the lower bound, at least 1
     */
    public static int floorLow(int cap, float strength) {
        float fraction = Math.min(0.9f, LOW_FRACTION * strength);
        return Math.max(1, (int) Math.round(cap * fraction));
    }

    /**
     * Returns the chance of a construction lot at a ring distance.
     *
     * @param d        ring distance
     * @param strength {@code densityRings}
     * @return the chance
     */
    public static float lotChance(float d, float strength) {
        Ring ring = Ring.of(d);
        float growth = ring == Ring.INNER || ring == Ring.OUTER ? LOT_GROWTH_FACTOR : 1f;
        return Math.min(1f, LOT_BASE * strength * growth);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
