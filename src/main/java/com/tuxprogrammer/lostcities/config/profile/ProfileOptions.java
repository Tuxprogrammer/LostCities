package com.tuxprogrammer.lostcities.config.profile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.tuxprogrammer.lostcities.config.profile.ProfileOption.Category;
import com.tuxprogrammer.lostcities.config.profile.ProfileOption.Status;
import com.tuxprogrammer.lostcities.config.profile.ProfileOption.Type;
import com.tuxprogrammer.lostcities.layout.MultiBuildingStreetConflict;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkTreatment;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainSettings;
import com.tuxprogrammer.lostcities.worldgen.terrain.WorldLimits;

/**
 * Every profile option: upstream's 163 in their five categories (docs/original-spec/02-profiles-dimensions/
 * profile-options.md) and this port's new ones (docs/new-spec/02-profiles-dimensions/profile-options.md), each with
 * its type, default, range and status. The profile reader validates against it, {@code saveprofile} writes from it,
 * the pinned snapshot stores every kept option, and docs/config.md documents every row
 * ({@code ProfileOptionsDocsTest}).
 *
 * <p>
 * Defaults are upstream's {@code default} profile, with this port's changes: {@code bedrockLayer} 0 (RWG already
 * places bedrock, 0005 §6) and the 1.7.10 height ranges. The terrain options' defaults come from
 * {@link TerrainSettings#defaults()}, which owns them, so a default is written in one place. A key that
 * {@link TerrainSettings#toValues()} produces and this catalog does not declare is registered automatically as a live
 * {@code lostcity} option of the type its default reads as: adding a terrain option to {@code TerrainSettings} is all
 * it takes to make it a profile option (it still needs its row in docs/config.md).
 *
 * <p>
 * Ranges are enforced when a profile loads (PROF-04): a single number out of range is clamped with a warning; the
 * cross-option checks are {@link ProfileValidator}'s.
 *
 * <p>
 * Side: both (plain Java).
 */
public final class ProfileOptions {

    /** The highest Y a block can have on 1.7.10 (0005 §6). */
    public static final double MAX_Y = 255;

    /** The default {@code description} of a profile that sets none: upstream's class default. */
    public static final String DEFAULT_DESCRIPTION = "Default generation, common cities, explosions";

    private static final Map<String, ProfileOption> BY_KEY = new LinkedHashMap<>();
    private static final Map<String, ProfileOption> BY_ALIAS = new TreeMap<>();
    private static final Map<String, String> TERRAIN = TerrainSettings.defaults()
        .toValues();
    /** Legacy defaults by key: what a pinned snapshot without the key resolves to. */
    private static final Map<String, String> LEGACY = new TreeMap<>();

    static {
        declareLostcity();
        declareExplosions();
        declareCities();
        declareCitySpheres();
        declareClient();
        declareNew();
        for (Map.Entry<String, String> entry : TERRAIN.entrySet()) {
            if (!BY_KEY.containsKey(entry.getKey())) {
                add(
                    new ProfileOption(
                        entry.getKey(),
                        Category.LOSTCITY,
                        inferType(entry.getValue()),
                        entry.getValue(),
                        null,
                        null,
                        null,
                        Status.LIVE,
                        "03-terrain",
                        false,
                        null));
            }
        }
    }

    private ProfileOptions() {}

    /** Returns every option, upstream's in catalog order, then the new ones. */
    public static List<ProfileOption> all() {
        return Collections.unmodifiableList(new ArrayList<>(BY_KEY.values()));
    }

    /**
     * Returns an option by key or by its alias.
     *
     * @param key the key
     * @return the option, or {@code null}
     */
    public static ProfileOption get(String key) {
        ProfileOption option = BY_KEY.get(key);
        return option != null ? option : BY_ALIAS.get(key);
    }

    /** Returns every kept option's default, by key, sorted: the {@code default} profile's values. */
    public static Map<String, String> defaults() {
        Map<String, String> out = new TreeMap<>();
        for (ProfileOption option : BY_KEY.values()) {
            if (option.kept()) {
                out.put(option.key(), option.defaultValue());
            }
        }
        return out;
    }

    private static Type inferType(String text) {
        if ("true".equals(text) || "false".equals(text)) {
            return Type.BOOLEAN;
        }
        try {
            Integer.parseInt(text);
            return Type.INT;
        } catch (NumberFormatException e) {
            return Type.FLOAT;
        }
    }

    private static void add(ProfileOption option) {
        if (BY_KEY.put(option.key(), option) != null) {
            throw new IllegalStateException("profile option " + option.key() + " declared twice");
        }
        if (option.alias() != null) {
            BY_ALIAS.put(option.alias(), option);
        }
    }

    /** A live option whose default is {@link TerrainSettings}'. */
    private static void terrain(Category category, String key, Type type, Double min, Double max) {
        String value = TERRAIN.get(key);
        if (value == null) {
            throw new IllegalStateException("TerrainSettings has no default for " + key);
        }
        add(new ProfileOption(key, category, type, value, min, max, null, Status.LIVE, "03-terrain", true, null));
    }

    private static void option(Category category, String key, Type type, String value, Double min, Double max,
        Status status, String owner) {
        add(new ProfileOption(key, category, type, value, min, max, null, status, owner, true, null));
    }

    private static void live(Category category, String key, Type type, String value, Double min, Double max,
        String owner) {
        option(category, key, type, value, min, max, Status.LIVE, owner);
    }

    private static void chance(Category category, String key, String value, String owner) {
        live(category, key, Type.FLOAT, value, 0d, 1d, owner);
    }

    private static void flag(Category category, String key, boolean value, Status status, String owner) {
        option(category, key, Type.BOOLEAN, Boolean.toString(value), null, null, status, owner);
    }

    private static void text(Category category, String key, String value, Status status, String owner) {
        option(category, key, Type.STRING, value, null, null, status, owner);
    }

    private static void choice(Category category, String key, String value, Status status, String owner,
        String... allowed) {
        add(
            new ProfileOption(
                key,
                category,
                Type.STRING,
                value,
                null,
                null,
                Arrays.asList(allowed),
                status,
                owner,
                true,
                null));
    }

    private static void dropped(Category category, String key, Type type, String value) {
        option(category, key, type, value, null, null, Status.DROPPED, "");
    }

    private static Double d(double value) {
        return value;
    }

    private static void declareLostcity() {
        Category c = Category.LOSTCITY;
        // Identity and blocks.
        text(c, "description", DEFAULT_DESCRIPTION, Status.LIVE, "01-core");
        text(c, "extraDescription", "", Status.LIVE, "01-core");
        text(c, "warning", "", Status.LIVE, "01-core");
        text(c, "worldStyle", "standard", Status.LIVE, "02-profiles-dimensions");
        dropped(c, "icon", Type.STRING, "");
        text(c, "liquidBlock", "minecraft:water", Status.LIVE, "03-terrain");
        text(c, "baseBlock", "minecraft:stone", Status.LIVE, "03-terrain");
        // Spawn selection.
        text(c, "spawnBiome", "", Status.LIVE, "01-core");
        text(c, "spawnCity", "", Status.LIVE, "01-core");
        text(c, "spawnSphere", "", Status.DEFERRED, "01-core");
        flag(c, "spawnNotInBuilding", false, Status.LIVE, "01-core");
        flag(c, "forceSpawnInBuilding", false, Status.LIVE, "01-core");
        option(c, "forceSpawnBuildings", Type.STRING_LIST, "[]", null, null, Status.LIVE, "01-core");
        option(c, "forceSpawnParts", Type.STRING_LIST, "[]", null, null, Status.LIVE, "01-core");
        live(c, "spawnCheckRadius", Type.INT, "200", d(1), d(100000), "01-core");
        live(c, "spawnRadiusIncrease", Type.INT, "100", d(1), d(100000), "01-core");
        live(c, "spawnCheckAttempts", Type.INT, "20000", d(1), d(1000000), "01-core");
        // Terrain look and damage layers.
        live(c, "scatteredChanceMultiplier", Type.FLOAT, "1.0", d(0), d(100), "07-scattered");
        terrain(c, "terrainFixLowerMinOffset", Type.INT, d(-40), d(40));
        terrain(c, "terrainFixLowerMaxOffset", Type.INT, d(-40), d(40));
        terrain(c, "terrainFixUpperMinOffset", Type.INT, d(-40), d(40));
        terrain(c, "terrainFixUpperMaxOffset", Type.INT, d(-40), d(40));
        terrain(c, "vineChance", Type.FLOAT, d(0), d(1));
        terrain(c, "randomLeafBlockChance", Type.FLOAT, d(0), d(1));
        terrain(c, "randomLeafBlockThickness", Type.INT, d(1), d(8));
        flag(c, "avoidFoliage", false, Status.LIVE, "04-city-layout");
        choice(c, "landscapeType", "default", Status.LIVE, "02-profiles-dimensions", "default");
        terrain(c, "rubbleLayer", Type.BOOLEAN, null, null);
        terrain(c, "rubbleDirtScale", Type.FLOAT, d(0), d(100));
        terrain(c, "rubbleLeaveScale", Type.FLOAT, d(0), d(100));
        terrain(c, "ruinChance", Type.FLOAT, d(0), d(1));
        terrain(c, "ruinMinlevelPercent", Type.FLOAT, d(0), d(1));
        terrain(c, "ruinMaxlevelPercent", Type.FLOAT, d(0), d(1));
        // Deviation: the default is 0, not 1; RWG already places bedrock at Y 0 to 4 and the overlay writes none
        // (0005 §6), and a value above 0 only draws a warning that it has no effect.
        add(
            new ProfileOption(
                "bedrockLayer",
                c,
                Type.INT,
                "0",
                d(0),
                d(10),
                null,
                Status.LIVE,
                "03-terrain",
                true,
                null));
        terrain(c, "avoidWater", Type.BOOLEAN, null, null);
        // Levels.
        terrain(c, "groundLevel", Type.INT, d(2), d(MAX_Y));
        terrain(c, "seaLevel", Type.INT, d(-1), d(MAX_Y));
        // Loot, spawners, lighting.
        terrain(c, "chestWithoutLootChance", Type.FLOAT, d(0), d(1));
        chance(c, "buildingWithoutLootChance", "0.2", "04-city-layout");
        terrain(c, "generateSpawners", Type.BOOLEAN, null, null);
        terrain(c, "generateLoot", Type.BOOLEAN, null, null);
        terrain(c, "generateLighting", Type.BOOLEAN, null, null);
        // Buildings.
        chance(c, "buildingChance", "0.3", "04-city-layout");
        // Deviation: 0 to 22 instead of 0 to 60, as buildingMaxFloors, since upstream lets the minimum win over the
        // maximum (height-mapping.md, P03-14, D31).
        live(c, "buildingMinFloors", Type.INT, "0", d(0), d(22), "04-city-layout");
        // Deviation: 0 to 22 instead of 0 to 60, so no building passes Y 255 (0005 §6).
        live(c, "buildingMaxFloors", Type.INT, "8", d(0), d(22), "04-city-layout");
        live(c, "buildingMinFloorsChance", Type.INT, "4", d(1), d(60), "04-city-layout");
        live(c, "buildingMaxFloorsChance", Type.INT, "6", d(1), d(60), "04-city-layout");
        live(c, "buildingMinCellars", Type.INT, "0", d(0), d(20), "04-city-layout");
        live(c, "buildingMaxCellars", Type.INT, "3", d(0), d(20), "04-city-layout");
        chance(c, "buildingDoorwayChance", "0.6", "04-city-layout");
        chance(c, "buildingFrontChance", "0.2", "04-city-layout");
        flag(c, "multiUseCorner", false, Status.LIVE, "04-city-layout");
        flag(c, "useAvgHeightmap", false, Status.LIVE, "04-city-layout");
        // Streets, parks, bridges, corridors.
        chance(c, "parkChance", "0.2", "04-city-layout");
        chance(c, "openLotParkChance", "0.8", "04-city-layout");
        chance(c, "corridorChance", "0.7", "04-city-layout");
        chance(c, "bridgeChance", "0.7", "04-city-layout");
        chance(c, "plannedPrimaryBridgeChance", "1.0", "04-city-layout");
        live(c, "plannedPrimaryBridgeMaxLength", Type.INT, "12", d(1), d(64), "04-city-layout");
        chance(c, "fountainChance", "0.05", "04-city-layout");
        terrain(c, "bridgeSupports", Type.BOOLEAN, null, null);
        flag(c, "parkElevation", true, Status.LIVE, "04-city-layout");
        flag(c, "parkBorder", true, Status.LIVE, "04-city-layout");
        live(c, "parkStreetThreshold", Type.INT, "3", d(0), d(8), "04-city-layout");
        dropped(c, "streetGenerationMode", Type.STRING, "HIERARCHICAL_GRID_V1");
        live(c, "primaryRoadSpacingX", Type.INT, "8", d(8), d(128), "04-city-layout");
        live(c, "primaryRoadSpacingZ", Type.INT, "8", d(8), d(128), "04-city-layout");
        chance(c, "primaryRoadOptionalChance", "0.45", "04-city-layout");
        live(c, "primaryRoadForceEvery", Type.INT, "4", d(1), d(16), "04-city-layout");
        live(c, "secondaryRoadMinCountX", Type.INT, "0", d(0), d(128), "04-city-layout");
        live(c, "secondaryRoadMaxCountX", Type.INT, "2", d(0), d(128), "04-city-layout");
        live(c, "secondaryRoadMinCountZ", Type.INT, "0", d(0), d(128), "04-city-layout");
        live(c, "secondaryRoadMaxCountZ", Type.INT, "2", d(0), d(128), "04-city-layout");
        live(c, "minimumRoadSeparation", Type.INT, "4", d(2), d(32), "04-city-layout");
        live(c, "minimumRoadEdgeDistance", Type.INT, "3", d(2), d(32), "04-city-layout");
        chance(c, "tertiaryRoadChance", "0.4", "04-city-layout");
        live(c, "tertiaryRoadMinLength", Type.INT, "2", d(1), d(16), "04-city-layout");
        live(c, "tertiaryRoadMaxLength", Type.INT, "5", d(1), d(32), "04-city-layout");
        List<String> conflicts = new ArrayList<>();
        for (MultiBuildingStreetConflict value : MultiBuildingStreetConflict.values()) {
            conflicts.add(value.name());
        }
        choice(
            c,
            "multiBuildingStreetConflict",
            MultiBuildingStreetConflict.OVERRIDE_MINOR.name(),
            Status.LIVE,
            "04-city-layout",
            conflicts.toArray(new String[0]));
        // Railways: the layout reads the dungeon chance, the railway grid the rest (M06).
        chance(c, "railwayDungeonChance", "0.01", "04-city-layout");
        flag(c, "railwaysCanEnd", false, Status.LIVE, "06-highways-railways");
        // Deviation: the city link network (0015). "legacy" is the M06 network and the default, so a world pinned
        // before this option existed reads "legacy"; "links" needs cityPlacement=grid. Deferred until the network
        // writer reads it (D309).
        choice(c, "networkLayout", "legacy", Status.LIVE, "06-highways-railways", "legacy", "links");
        flag(c, "railwaysEnabled", true, Status.LIVE, "06-highways-railways");
        flag(c, "railwayStationsEnabled", true, Status.LIVE, "06-highways-railways");
        flag(c, "railwaySurfaceStationsEnabled", true, Status.LIVE, "06-highways-railways");
        option(c, "railwayLevelOffset", Type.INT, "0", d(-8), d(2), Status.LIVE, "06-highways-railways");
        option(c, "railwaySpacingNorthSouth", Type.INT, "10", d(4), d(128), Status.LIVE, "06-highways-railways");
        option(c, "railwaySpacingEastWest", Type.INT, "10", d(4), d(128), Status.LIVE, "06-highways-railways");
        // Highways: only the intercity planner's options survive (D11).
        dropped(c, "highwayRequiresTwoCities", Type.BOOLEAN, "true");
        option(c, "highwayLevelFromCities", Type.INT, "3", d(0), d(4), Status.LIVE, "06-highways-railways");
        dropped(c, "highwayDistanceMask", Type.INT, "7");
        dropped(c, "highwayMainPerlinScale", Type.FLOAT, "50.0");
        dropped(c, "highwaySecondaryPerlinScale", Type.FLOAT, "10.0");
        dropped(c, "highwayPerlinFactor", Type.FLOAT, "2.0");
        flag(c, "highwaySupports", true, Status.LIVE, "06-highways-railways");
        dropped(c, "highwayGenerationMode", Type.STRING, "INTERCITY_NETWORK_V1");
        option(c, "highwayPlanningCellSize", Type.INT, "128", d(32), d(512), Status.LIVE, "06-highways-railways");
        option(c, "highwayHubSampleSpacing", Type.INT, "16", d(1), d(512), Status.LIVE, "06-highways-railways");
        option(c, "highwayHubMinimumPotential", Type.FLOAT, "0.2", d(0), d(1), Status.LIVE, "06-highways-railways");
        option(c, "highwayHubSearchRadiusCells", Type.INT, "2", d(0), d(8), Status.LIVE, "06-highways-railways");
        option(c, "highwayMinimumHubDistance", Type.INT, "64", d(0), d(4096), Status.LIVE, "06-highways-railways");
        option(c, "highwayMaximumHubDistance", Type.INT, "320", d(0), d(4096), Status.LIVE, "06-highways-railways");
        option(c, "highwayMaximumConnectionsPerHub", Type.INT, "2", d(1), d(8), Status.LIVE, "06-highways-railways");
        option(c, "highwayMinimumRouteLength", Type.INT, "40", d(0), d(4096), Status.LIVE, "06-highways-railways");
        option(c, "highwayRouteCityPenalty", Type.FLOAT, "1.0", d(0), d(1000), Status.LIVE, "06-highways-railways");
        option(c, "highwayNetworkLevel", Type.INT, "0", d(0), d(32), Status.LIVE, "06-highways-railways");
        // Modes and switches.
        flag(c, "editMode", false, Status.LIVE, "01-core");
        dropped(c, "generateNether", Type.BOOLEAN, "false");
    }

    private static void declareExplosions() {
        Category c = Category.EXPLOSIONS;
        terrain(c, "debrisToNearbyChunkFactor", Type.INT, d(1), d(10000));
        terrain(c, "explosionChance", Type.FLOAT, d(0), d(1));
        terrain(c, "explosionMinRadius", Type.INT, d(1), d(1000));
        terrain(c, "explosionMaxRadius", Type.INT, d(1), d(3000));
        terrain(c, "explosionMinHeight", Type.INT, d(1), d(MAX_Y));
        terrain(c, "explosionMaxHeight", Type.INT, d(1), d(MAX_Y));
        terrain(c, "miniExplosionChance", Type.FLOAT, d(0), d(1));
        terrain(c, "miniExplosionMinRadius", Type.INT, d(1), d(1000));
        terrain(c, "miniExplosionMaxRadius", Type.INT, d(1), d(3000));
        terrain(c, "miniExplosionMinHeight", Type.INT, d(1), d(MAX_Y));
        terrain(c, "miniExplosionMaxHeight", Type.INT, d(1), d(MAX_Y));
        terrain(c, "explosionsInCitiesOnly", Type.BOOLEAN, null, null);
    }

    private static void declareCities() {
        Category c = Category.CITIES;
        live(c, "cityChance", Type.DOUBLE, "0.01", d(-1), d(1), "04-city-layout");
        // Deviation: the jittered-grid placement; "chance" is upstream's per-chunk chance and the default, so a world
        // pinned before these options existed reads "chance" (D270).
        choice(c, "cityPlacement", "chance", Status.LIVE, "04-city-layout", "chance", "grid");
        live(c, "cityGridCell", Type.INT, "64", d(8), d(1024), "04-city-layout");
        live(c, "cityGridJitter", Type.INT, "16", d(0), d(511), "04-city-layout");
        live(c, "citySpawnDistance1", Type.INT, "0", d(0), d(10000000), "04-city-layout");
        live(c, "citySpawnDistance2", Type.INT, "0", d(0), d(10000000), "04-city-layout");
        live(c, "citySpawnMultiplier1", Type.DOUBLE, "1.0", d(0), d(1), "04-city-layout");
        live(c, "citySpawnMultiplier2", Type.DOUBLE, "1.0", d(0), d(1), "04-city-layout");
        live(c, "cityMinRadius", Type.INT, "50", d(1), d(2000), "04-city-layout");
        live(c, "cityMaxRadius", Type.INT, "128", d(1), d(2000), "04-city-layout");
        live(c, "cityPerlinScale", Type.DOUBLE, "3.0", d(-1000000), d(1000000), "04-city-layout");
        live(c, "cityPerlinOffset", Type.DOUBLE, "0.1", d(-1000000), d(1000000), "04-city-layout");
        live(c, "cityPerlinInnerScale", Type.DOUBLE, "0.1", d(-1000000), d(1000000), "04-city-layout");
        live(c, "cityThreshold", Type.FLOAT, "0.2", d(0), d(1), "04-city-layout");
        // Deviation: -1 (off) or 0 to 1, so the default is inside the range (PROF-16); the validator turns any other
        // negative value into -1.
        live(c, "cityStyleThreshold", Type.FLOAT, "-1.0", d(-1), d(1), "04-city-layout");
        text(c, "cityStyleAlternative", "", Status.LIVE, "04-city-layout");
        flag(c, "cityAvoidVoid", true, Status.DEFERRED, "03-terrain");
        for (int i = 0; i < TerrainSettings.CITY_LEVEL_THRESHOLDS; i++) {
            terrain(c, "cityLevel" + i + "Height", Type.INT, d(1), d(MAX_Y));
        }
        terrain(c, "cityMinHeight", Type.INT, d(0), d(MAX_Y));
        terrain(c, "cityMaxHeight", Type.INT, d(0), d(MAX_Y));
        terrain(c, "oceanCorrectionBorder", Type.INT, d(-255), d(255));
    }

    private static void declareCitySpheres() {
        Category c = Category.CITYSPHERES;
        option(c, "citySphereFactor", Type.FLOAT, "1.2", d(0.1), d(10), Status.DEFERRED, "03-terrain");
        option(c, "citySphereChance", Type.FLOAT, "0.7", d(0), d(1), Status.DEFERRED, "03-terrain");
        option(c, "citySphereClearAbove", Type.INT, "0", d(0), d(1024), Status.DEFERRED, "03-terrain");
        flag(c, "citySphereClearAboveUntilAir", false, Status.DEFERRED, "03-terrain");
        option(c, "citySphereClearBelow", Type.INT, "0", d(0), d(1024), Status.DEFERRED, "03-terrain");
        flag(c, "citySphereClearBelowUntilAir", false, Status.DEFERRED, "03-terrain");
        dropped(c, "sphereSurfaceVariation", Type.FLOAT, "1.0");
        dropped(c, "outsideSurfaceVariation", Type.FLOAT, "1.0");
        option(c, "monorailChance", Type.FLOAT, "0.8", d(0), d(1), Status.DEFERRED, "06-highways-railways");
        flag(c, "onlyPredefined", false, Status.DEFERRED, "03-terrain");
        dropped(c, "outsideGroundLevel", Type.INT, "-1");
        text(c, "outsideProfile", "", Status.DEFERRED, "02-profiles-dimensions");
        option(c, "monorailOffset", Type.INT, "-2", d(-100), d(100), Status.DEFERRED, "06-highways-railways");
        flag(c, "grid32", false, Status.DEFERRED, "03-terrain");
    }

    private static void declareClient() {
        Category c = Category.CLIENT;
        dropped(c, "horizon", Type.FLOAT, "-1.0");
        dropped(c, "fogRed", Type.FLOAT, "-1.0");
        dropped(c, "fogGreen", Type.FLOAT, "-1.0");
        dropped(c, "fogBlue", Type.FLOAT, "-1.0");
        dropped(c, "fogDensity", Type.FLOAT, "-1.0");
    }

    /** This port's options, all in {@code lostcity} (P-PROF-6, D31). */
    private static void declareNew() {
        Category c = Category.LOSTCITY;
        // Upstream 1.12's preventLakesRavinesInCities, renamed because ravines are not covered (P-PROF-6, P03-2);
        // the old name still loads.
        add(
            new ProfileOption(
                "preventLakesInCities",
                c,
                Type.BOOLEAN,
                TERRAIN.get("preventLakesInCities"),
                null,
                null,
                null,
                Status.LIVE,
                "03-terrain",
                false,
                "preventLakesRavinesInCities"));
        add(
            new ProfileOption(
                "highwaysEnabled",
                c,
                Type.BOOLEAN,
                "true",
                null,
                null,
                null,
                Status.LIVE,
                "06-highways-railways",
                false,
                null));
        List<String> treatments = new ArrayList<>();
        for (LandmarkTreatment treatment : LandmarkTreatment.values()) {
            treatments.add(treatment.name());
        }
        add(
            new ProfileOption(
                "villageTreatment",
                c,
                Type.STRING,
                LandmarkTreatment.BUILD_AROUND.name(),
                null,
                null,
                treatments,
                Status.LIVE,
                "04-city-layout",
                false,
                null));
        add(
            new ProfileOption(
                "landmarkTreatment",
                c,
                Type.STRING,
                LandmarkTreatment.BUILD_AROUND.name(),
                null,
                null,
                null,
                Status.LIVE,
                "04-city-layout",
                false,
                null));
        newTerrain("landmarkMargin", Type.INT, d(0), d(16));
        newTerrain("softEdgeWidth", Type.INT, d(2), d(12));
        newTerrain("cityMaxRise", Type.INT, d(0), d(TerrainSettings.CITY_LEVEL_THRESHOLDS));
        newTerrain("cityGrade", Type.INT, d(0), d(TerrainSettings.CITY_LEVEL_THRESHOLDS));
        newTerrain("cityGradeRadius", Type.INT, d(0), d(4));
        newTerrain("cityAvoidOcean", Type.BOOLEAN, null, null);
        newTerrain("cityConnected", Type.BOOLEAN, null, null);
        newTerrain("cityTaper", Type.FLOAT, d(0), d(1));
        newTerrain("edgeLotChance", Type.FLOAT, d(0), d(1));
        // New worlds get 0.25; worlds pinned before the key existed have no pack houses (D282).
        add(
            new ProfileOption(
                "packHouseChance",
                Category.LOSTCITY,
                Type.FLOAT,
                "0.25",
                d(0),
                d(1),
                null,
                Status.LIVE,
                "03-terrain",
                false,
                null));
        // New worlds get 1.0; worlds pinned before the key existed have no rings (D282, D306).
        add(
            new ProfileOption(
                "densityRings",
                Category.LOSTCITY,
                Type.FLOAT,
                "1.0",
                d(0),
                d(2),
                null,
                Status.LIVE,
                "04-city-layout",
                false,
                null));
        // New worlds get construction lots at real sizes; worlds pinned before the key keep single-chunk lots (D325).
        add(
            new ProfileOption(
                "lotFootprints",
                Category.LOSTCITY,
                Type.BOOLEAN,
                "true",
                null,
                null,
                null,
                Status.LIVE,
                "04-city-layout",
                false,
                null));
        // New worlds get furniture; worlds pinned before the key existed keep empty rooms (D282, 0016).
        add(
            new ProfileOption(
                "furnish",
                Category.LOSTCITY,
                Type.BOOLEAN,
                "true",
                null,
                null,
                null,
                Status.LIVE,
                "03-terrain",
                false,
                null));
        // New worlds get "random"; worlds pinned before the key existed have no city highway (D282, 0017). Only
        // networkLayout=links reads it.
        add(
            new ProfileOption(
                "cityHighwayLayout",
                Category.LOSTCITY,
                Type.STRING,
                "random",
                null,
                null,
                java.util.Arrays.asList("none", "beltway", "cutthrough", "random"),
                Status.LIVE,
                "06-highways-railways",
                false,
                null));
        // New worlds get the plaza, industrial yards and park belt; pinned worlds keep their layout (D282, D350).
        add(
            new ProfileOption(
                "cityDistricts",
                Category.LOSTCITY,
                Type.BOOLEAN,
                "true",
                null,
                null,
                null,
                Status.LIVE,
                "04-city-layout",
                false,
                null));
        // New worlds keep door steps open past a station's walls and on a lower roof; pinned worlds do not (D282,
        // D376).
        add(
            new ProfileOption(
                "doorStepsOpen",
                Category.LOSTCITY,
                Type.BOOLEAN,
                "true",
                null,
                null,
                null,
                Status.LIVE,
                "04-city-layout",
                false,
                null));
        // New worlds get room-fitting loot and spawners; pinned worlds keep the parts' placement (D282, 0016).
        add(
            new ProfileOption(
                "roomRules",
                Category.LOSTCITY,
                Type.BOOLEAN,
                "true",
                null,
                null,
                null,
                Status.LIVE,
                "08-loot-mobs-blocks",
                false,
                null));
        // New worlds stop a legacy rail line on a buffer before a core chunk it cannot pass; pinned worlds keep the gap
        // (KI-3, 0013 §5, D367).
        add(
            new ProfileOption(
                "railwayCoreEnds",
                Category.LOSTCITY,
                Type.BOOLEAN,
                "true",
                null,
                null,
                null,
                Status.LIVE,
                "06-highways-railways",
                false,
                null));
        // New worlds keep a highway run flat where another route turns or ends on it; pinned worlds keep the ramp there
        // (KI-5, 0013 §2).
        add(
            new ProfileOption(
                "highwayRampHolds",
                Category.LOSTCITY,
                Type.BOOLEAN,
                "true",
                null,
                null,
                null,
                Status.LIVE,
                "06-highways-railways",
                false,
                null));
        // New worlds give highway ramps one surface; pinned worlds keep the slab stripes (KI-2, 0013 §2, D368).
        add(
            new ProfileOption(
                "rampSurface",
                Category.LOSTCITY,
                Type.BOOLEAN,
                "true",
                null,
                null,
                null,
                Status.LIVE,
                "06-highways-railways",
                false,
                null));
        newTerrain("damageRepairChance", Type.FLOAT, d(0), d(1));
        newTerrain("facadeContrast", Type.FLOAT, d(10), d(200));
        add(
            new ProfileOption(
                "villageTransitionRings",
                c,
                Type.INT,
                "2",
                d(0),
                d(8),
                null,
                Status.LIVE,
                "04-city-layout",
                false,
                null));
        add(
            new ProfileOption(
                "villageLinkDistance",
                c,
                Type.INT,
                "4",
                d(0),
                d(16),
                null,
                Status.LIVE,
                "04-city-layout",
                false,
                null));
        // M08: the vanilla-category opt-in for chest loot and the EnderZoo hardmobs set, both off (D20).
        newTerrain("vanillaLootCategories", Type.BOOLEAN, null, null);
        newTerrain("enderZooHardMobs", Type.BOOLEAN, null, null);
        // Upstream's mod-config key, pinned with the profile so it cannot change an existing world (CORE-10, D73).
        newTerrain("heightSampleSize", Type.INT, d(1), d(100));
        // New worlds get 8; worlds pinned before the key existed keep upstream's 6 (design record 0014).
        add(
            new ProfileOption(
                "floorPitch",
                Category.LOSTCITY,
                Type.INT,
                Integer.toString(WorldLimits.FLOOR_PITCH),
                d(WorldLimits.LEGACY_FLOOR_PITCH),
                d(WorldLimits.MAX_FLOOR_PITCH),
                null,
                Status.LIVE,
                "03-terrain",
                false,
                null));
        legacyDefault("floorPitch", Integer.toString(WorldLimits.LEGACY_FLOOR_PITCH));
        // Worlds pinned before pack houses existed keep their edge lots as they were (D282).
        legacyDefault("packHouseChance", "0.0");
        legacyDefault("densityRings", "0.0");
        // Worlds pinned before the link network keep the legacy highways and railways, whatever the default becomes.
        legacyDefault("networkLayout", "legacy");
        legacyDefault("lotFootprints", "false");
        // Worlds pinned before the furnish pass keep their empty rooms (design record 0016).
        legacyDefault("furnish", "false");
        legacyDefault("cityHighwayLayout", "none");
        legacyDefault("cityDistricts", "false");
        legacyDefault("doorStepsOpen", "false");
        legacyDefault("roomRules", "false");
        legacyDefault("railwayCoreEnds", "false");
        legacyDefault("highwayRampHolds", "false");
        legacyDefault("rampSurface", "false");
    }

    /**
     * Declares an option's legacy default: the value a pinned snapshot that lacks the key resolves to, for options
     * that must keep old worlds as they were while new pins take {@link ProfileOption#defaultValue()} (design record
     * 0014). Call it right after the option's {@code add}.
     *
     * @param key   a declared option
     * @param value the legacy value
     */
    private static void legacyDefault(String key, String value) {
        if (!BY_KEY.containsKey(key)) {
            throw new IllegalStateException("legacy default for undeclared option " + key);
        }
        LEGACY.put(key, value);
    }

    /**
     * Returns an option's legacy default.
     *
     * @param key the option key
     * @return the value a pinned snapshot without the key resolves to, or null when the option has none
     */
    public static String legacyDefault(String key) {
        return LEGACY.get(key);
    }

    /**
     * Fills in the legacy default of every option that declares one and is missing from pinned values. Every
     * {@link ProfileSnapshot} passes its values through this, so a world pinned before an option existed keeps the
     * old behavior while new pins, which write every option, keep the new default.
     *
     * @param pinned the values a snapshot was pinned with
     * @return a new sorted map: {@code pinned} plus the missing legacy defaults
     */
    public static Map<String, String> withLegacyDefaults(Map<String, String> pinned) {
        Map<String, String> out = new TreeMap<>(pinned);
        for (Map.Entry<String, String> entry : LEGACY.entrySet()) {
            if (!out.containsKey(entry.getKey())) {
                out.put(entry.getKey(), entry.getValue());
            }
        }
        return out;
    }

    private static void newTerrain(String key, Type type, Double min, Double max) {
        add(
            new ProfileOption(
                key,
                Category.LOSTCITY,
                type,
                TERRAIN.get(key),
                min,
                max,
                null,
                Status.LIVE,
                "03-terrain",
                false,
                null));
    }
}
