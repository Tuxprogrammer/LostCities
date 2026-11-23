package com.tuxprogrammer.lostcities.compat.rwg2;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import com.tuxprogrammer.lostcities.LostCities;

/**
 * RWG2's config values this mod reads, from whichever RWG2 is loaded. RWG2 2.0.6 (GTNH 2.9.0) keeps them all as
 * static fields of {@code rwg2.config.ConfigRWG}. RWG2 2.0.9 (GTNH 2.9.1) moved them to GTNHLib annotated config
 * classes: the continent shape to {@code ContinentalConfig}, the placement offsets to {@code MigrationConfig}, and
 * "Generate Villages" to {@code SettingsConfig}
 * (<a href="https://github.com/GTNewHorizons/Realistic-World-Gen-2/commit/d09e3dc">RWG2 d09e3dc</a>, tag 2.0.9).
 * Referring to either class directly fails with {@code NoSuchFieldError} on the other version, so each value is read
 * by reflection: the 2.0.9 home first, then {@code ConfigRWG}. Names and types are the same in both versions.
 *
 * <p>
 * A value found in neither class gets RWG2's own default (offsets 0, "Generate Villages" on). This is logged once per
 * field. The {@link Field} handles are resolved once when the class loads; the values are read on every call, so the
 * answers follow RWG2's config as it is at that moment. Load this class only after
 * {@code Loader.isModLoaded("rwg2")}. Thread-safe: a static field read through a resolved handle.
 *
 * <p>
 * Spec: docs/new-spec/00-pack-integration/rwg.md. Side: server.
 */
public final class Rwg2Config {

    /** RWG2's pre-2.0.9 config class, the fallback home of every value. */
    static final String LEGACY = "rwg2.config.ConfigRWG";
    /** RWG2 2.0.9's home of the continent and island shape. */
    static final String CONTINENTAL = "rwg2.config.ContinentalConfig";
    /** RWG2 2.0.9's home of the landmass and biome placement offsets. */
    static final String MIGRATION = "rwg2.config.MigrationConfig";
    /** RWG2 2.0.9's home of "Generate Villages". */
    static final String SETTINGS = "rwg2.config.SettingsConfig";

    private static final Field MINIMUM_CONTINENT_WIDTH = find(CONTINENTAL, "minimumContinentWidth");
    private static final Field MAXIMUM_CONTINENT_WIDTH = find(CONTINENTAL, "maximumContinentWidth");
    private static final Field MINIMUM_ISLAND_WIDTH = find(CONTINENTAL, "minimumIslandWidth");
    private static final Field MAXIMUM_ISLAND_WIDTH = find(CONTINENTAL, "maximumIslandWidth");
    private static final Field AVERAGE_OCEAN_WIDTH = find(CONTINENTAL, "averageOceanWidth");
    private static final Field MINIMUM_OCEAN_WIDTH = find(CONTINENTAL, "minimumOceanWidth");
    private static final Field MAXIMUM_OCEAN_FRACTION = find(CONTINENTAL, "maximumOceanFraction");
    private static final Field ISLAND_PLACEMENT_CHANCE = find(CONTINENTAL, "islandPlacementChance");
    private static final Field LARGE_ISLAND_VOLCANO_CHANCE = find(CONTINENTAL, "largeIslandVolcanoChance");
    private static final Field AVERAGE_LANDMARKS = find(CONTINENTAL, "averageLandmarksPerTypeAndContinent");
    private static final Field LANDMASS_OFFSET_X = find(MIGRATION, "landmassOffsetX");
    private static final Field LANDMASS_OFFSET_Z = find(MIGRATION, "landmassOffsetZ");
    private static final Field BIOME_OFFSET_X = find(MIGRATION, "biomeOffsetX");
    private static final Field BIOME_OFFSET_Z = find(MIGRATION, "biomeOffsetZ");
    private static final Field GENERATE_VILLAGES = find(SETTINGS, "generateVillages");

    private Rwg2Config() {}

    /** RWG2's "Landmass Placement Offset X": continent and island placement is sampled at {@code x + offset}. */
    static int landmassOffsetX() {
        return intValue(LANDMASS_OFFSET_X, 0);
    }

    /** RWG2's "Landmass Placement Offset Z": continent and island placement is sampled at {@code z + offset}. */
    static int landmassOffsetZ() {
        return intValue(LANDMASS_OFFSET_Z, 0);
    }

    /** RWG2's "Generate Villages" switch. */
    public static boolean generateVillages() {
        if (GENERATE_VILLAGES == null) {
            return true;
        }
        try {
            return GENERATE_VILLAGES.getBoolean(null);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            return true;
        }
    }

    /**
     * Returns the continental settings as one line for the log, so a real-pack run shows what the landmark oracle saw.
     * A value that cannot be read prints as {@code ?}.
     */
    static String summary() {
        return "continent=" + number(MINIMUM_CONTINENT_WIDTH)
            + "-"
            + number(MAXIMUM_CONTINENT_WIDTH)
            + ",island="
            + number(MINIMUM_ISLAND_WIDTH)
            + "-"
            + number(MAXIMUM_ISLAND_WIDTH)
            + ",ocean="
            + number(AVERAGE_OCEAN_WIDTH)
            + "/"
            + number(MINIMUM_OCEAN_WIDTH)
            + "/"
            + number(MAXIMUM_OCEAN_FRACTION)
            + ",islandChance="
            + number(ISLAND_PLACEMENT_CHANCE)
            + ",volcanoChance="
            + number(LARGE_ISLAND_VOLCANO_CHANCE)
            + ",landmarks="
            + number(AVERAGE_LANDMARKS)
            + ",landmassOffset="
            + number(LANDMASS_OFFSET_X)
            + "/"
            + number(LANDMASS_OFFSET_Z)
            + ",biomeOffset="
            + number(BIOME_OFFSET_X)
            + "/"
            + number(BIOME_OFFSET_Z)
            + ",source="
            + (MINIMUM_CONTINENT_WIDTH == null ? "?"
                : MINIMUM_CONTINENT_WIDTH.getDeclaringClass()
                    .getSimpleName());
    }

    /**
     * Returns the public static field {@code name} of {@code preferred}, or of {@link #LEGACY} when {@code preferred}
     * is missing or lacks it, or {@code null} when neither has it.
     */
    static Field find(String preferred, String name) {
        return find(preferred, LEGACY, name);
    }

    /**
     * Returns the public static field {@code name} of {@code preferred}, or of {@code fallback} when {@code preferred}
     * is missing or lacks it, or {@code null} when neither has it.
     */
    static Field find(String preferred, String fallback, String name) {
        Field field = staticField(preferred, name);
        if (field == null) {
            field = staticField(fallback, name);
        }
        if (field == null) {
            LostCities.LOG.warn("[LCNH-RWG2] RWG2 config field {} not found; using RWG2's default", name);
        }
        return field;
    }

    private static Field staticField(String className, String name) {
        try {
            Field field = Class.forName(className, false, Rwg2Config.class.getClassLoader())
                .getField(name);
            return Modifier.isStatic(field.getModifiers()) ? field : null;
        } catch (ClassNotFoundException | NoSuchFieldException | SecurityException | LinkageError e) {
            return null;
        }
    }

    private static int intValue(Field field, int fallback) {
        if (field == null) {
            return fallback;
        }
        try {
            return field.getInt(null);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            return fallback;
        }
    }

    private static String number(Field field) {
        if (field == null) {
            return "?";
        }
        try {
            Object value = field.get(null);
            return String.valueOf(value);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            return "?";
        }
    }
}
