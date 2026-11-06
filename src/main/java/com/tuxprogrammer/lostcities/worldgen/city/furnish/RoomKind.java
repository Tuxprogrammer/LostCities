package com.tuxprogrammer.lostcities.worldgen.city.furnish;

import java.util.Locale;
import java.util.Random;

/**
 * What a floor's rooms are furnished as (design record 0016, decision 4). The kind is a pure function of the building,
 * the floor and a seed, so the terrain phase and the population dry run agree without reading any block.
 *
 * <p>
 * Side: both (plain Java).
 */
public enum RoomKind {

    /** Flats and houses: bookshelves, carpets, candles, pots. */
    RESIDENTIAL,
    /** Offices and libraries: bookshelves, tables, panels. */
    OFFICE,
    /** Street-level shops: counters, barrels, carpets. */
    SHOP,
    /** Kitchens and canteens: furnaces, cauldrons, counters. */
    KITCHEN,
    /** Cellars: a workshop with a few low-tier GT machines. */
    BASEMENT,
    /** Workshops, oil rigs and towers: GT machines and factory panels. */
    INDUSTRIAL;

    /** Returns the palette file's name and the population job's argument. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Returns the kind named by {@link #id()}.
     *
     * @param id the name
     * @return the kind, or {@code null} for an unknown name
     */
    public static RoomKind byId(String id) {
        for (RoomKind kind : values()) {
            if (kind.id()
                .equals(id)) {
                return kind;
            }
        }
        return null;
    }

    /**
     * Returns the kind of one floor.
     *
     * <ul>
     * <li>cellars are {@link #BASEMENT};</li>
     * <li>workshops, oil rigs and radio towers are {@link #INDUSTRIAL} above ground;</li>
     * <li>the ground floor of a shop, restaurant or gas station is {@link #SHOP} or {@link #KITCHEN};</li>
     * <li>houses, cottages, walk-ups and cabins are {@link #RESIDENTIAL};</li>
     * <li>libraries and town centers are {@link #OFFICE};</li>
     * <li>the ground floor of any other building is a shop or kitchen half the time, and upper floors are
     * residential or office by a seeded pick.</li>
     * </ul>
     *
     * @param building the building's asset path (for example {@code nh_walkup}), or {@code null}
     * @param floor    the floor, negative for cellars
     * @param seed     the floor's seed
     * @return the kind
     */
    public static RoomKind forFloor(String building, int floor, long seed) {
        if (floor < 0) {
            return BASEMENT;
        }
        String name = building == null ? "" : building.toLowerCase(Locale.ROOT);
        Random random = new Random(seed);
        if (name.contains("workshop") || name.contains("oilrig") || name.contains("radiotower")) {
            return INDUSTRIAL;
        }
        boolean commercial = name.contains("shop") || name.contains("restaurant") || name.contains("gas_station");
        if (floor == 0 && commercial) {
            return random.nextInt(3) == 0 ? KITCHEN : SHOP;
        }
        if (name.contains("house") || name.contains("cottage")
            || name.contains("walkup")
            || name.contains("cabin")) {
            return RESIDENTIAL;
        }
        if (name.contains("library") || name.contains("center")) {
            return OFFICE;
        }
        if (commercial) {
            return OFFICE;
        }
        if (floor == 0 && random.nextBoolean()) {
            return random.nextInt(3) == 0 ? KITCHEN : SHOP;
        }
        return random.nextBoolean() ? RESIDENTIAL : OFFICE;
    }
}
