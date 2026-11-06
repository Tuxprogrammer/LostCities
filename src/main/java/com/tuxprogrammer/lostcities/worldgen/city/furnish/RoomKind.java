package com.tuxprogrammer.lostcities.worldgen.city.furnish;

import java.util.Locale;
import java.util.Random;

import com.tuxprogrammer.lostcities.layout.DensityRings;

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

    private static final long TABLE_STREAM = 0x7461626CL;

    private static RoomKind byTable(String name, int floor, Random random) {
        if (name.contains("workshop") || name.contains("oilrig") || name.contains("radiotower")) {
            return INDUSTRIAL;
        }
        if (name.startsWith("highway_restaurant")) {
            return floor == 0 && random.nextInt(3) != 0 ? KITCHEN : SHOP;
        }
        if (name.contains("gas_station")) {
            return SHOP;
        }
        if (name.startsWith("town") || name.startsWith("center") || name.startsWith("library")) {
            return OFFICE;
        }
        if (name.startsWith("shopping") || name.contains("shop")) {
            return floor == 0 ? (random.nextInt(4) == 0 ? KITCHEN : SHOP) : OFFICE;
        }
        boolean tower = DensityRings.classOf(name) == DensityRings.BuildingClass.TOWER;
        boolean highRise = DensityRings.classOf(name) == DensityRings.BuildingClass.HIGH_RISE;
        if (tower || highRise) {
            if (floor == 0) {
                return random.nextInt(3) == 0 ? KITCHEN : SHOP;
            }
            return highRise && random.nextInt(4) == 0 ? RESIDENTIAL : OFFICE;
        }
        return null;
    }

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
        return forFloor(building, floor, seed, false);
    }

    /**
     * Returns the kind of one floor, optionally by the room-palette table (lane 6c, {@code roomRules}). The table maps
     * every building the mod ships:
     *
     * <ul>
     * <li>towers ({@code building1}, {@code building8}, {@code multi1}, {@code huge*}) are offices above the ground
     * floor;</li>
     * <li>{@code town*}, {@code townhall}, {@code center*}, {@code library*} are offices throughout;</li>
     * <li>restaurants are kitchens on the ground floor two times in three, gas stations and shops are shops;</li>
     * <li>{@code multi2} to {@code multi5} are shops or kitchens on the ground floor and offices above, a quarter of
     * their upper floors residential;</li>
     * <li>everything else keeps the name rules above.</li>
     * </ul>
     *
     * @param building the building's asset path, or {@code null}
     * @param floor    the floor, negative for cellars
     * @param seed     the floor's seed
     * @param table    whether to use the table
     * @return the kind
     */
    public static RoomKind forFloor(String building, int floor, long seed, boolean table) {
        if (table && floor >= 0 && building != null) {
            RoomKind mapped = byTable(building.toLowerCase(Locale.ROOT), floor, new Random(seed ^ TABLE_STREAM));
            if (mapped != null) {
                return mapped;
            }
        }
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
        if (name.contains("house") || name.contains("cottage") || name.contains("walkup") || name.contains("cabin")) {
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
