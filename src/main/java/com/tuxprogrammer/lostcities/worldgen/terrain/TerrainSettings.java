package com.tuxprogrammer.lostcities.worldgen.terrain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The profile values the overlay's terrain and population phases read, resolved and range-checked: ground and sea
 * level, city levels, blend offsets, explosions, debris, ruins, rubble, vines, the 1.12 lake veto and the landmark
 * edge numbers. It is built from a pinned profile snapshot's values ({@link #fromValues}) and written back into one
 * ({@link #toValues}), so a world keeps the numbers it was created with (docs/design/0005-rwg-overlay.md §7).
 *
 * <p>
 * Defaults are upstream's {@code default} profile (docs/original-spec/02-profiles-dimensions/profile-options.md),
 * except the explosion chances and radii and the ruin chance, which are much lower (the {@code DEFAULT_} constants,
 * AR-8).
 * Ranges follow docs/new-spec/03-terrain/height-mapping.md: anything that could leave Y 0 to 255 is clamped with a
 * warning, and every min/max pair is ordered at load so a draw between them never throws (TERR-03, PROF-06). Keys
 * are upstream's profile option names, plus this mod's own ({@code preventLakesRavinesInCities} from 1.12,
 * {@code softEdgeWidth}, {@code landmarkMargin} from docs/design/0006-build-around-other-worldgen.md §3,
 * {@code cityMaxRise} from the alpha review (docs/new-spec/03-terrain/height-mapping.md "Rise cap"), and
 * {@code heightSampleSize}, which upstream keeps in its mod config).
 *
 * <p>
 * Immutable. Side: both (plain Java); only the server reads it.
 */
public final class TerrainSettings {

    /** Number of city level thresholds ({@code cityLevel0Height} … {@code cityLevel7Height}). */
    public static final int CITY_LEVEL_THRESHOLDS = 8;

    /** {@code seaLevel} value meaning "use the generator's sea level", 63 on RWG. */
    public static final int SEA_LEVEL_DEFAULT = -1;

    /**
     * Default {@code cityMaxRise}: two city levels (12 blocks of city ground) above a city's base, the alpha review's
     * "happy medium" between leveling a mountain and building up it (AR-4, decision log D-RW rows).
     */
    public static final int DEFAULT_MAX_RISE = 2;
    /** {@code cityGrade}'s default: a city chunk sits within one level (8 blocks) of its city's base. */
    public static final int DEFAULT_GRADE = 1;
    /** {@code cityGradeRadius}'s default: the 3 × 3 chunks around a chunk set its level. */
    public static final int DEFAULT_GRADE_RADIUS = 1;

    /** Upstream's default city level thresholds, Y 75 to 131 in steps of 8. */
    private static final int[] DEFAULT_LEVEL_HEIGHTS = { 75, 83, 91, 99, 107, 115, 123, 131 };

    /**
     * Default chance per city chunk of a big explosion: 0.0004, a fifth of upstream's 0.002. Deviation (AR-8,
     * docs/new-spec/03-terrain/index.md, "Explosions, damage, debris, ruins, rubble"): damage is an occasional accent,
     * not the look of a city.
     */
    public static final float DEFAULT_EXPLOSION_CHANCE = 0.0004f;
    /** Default smallest big-explosion radius, blocks: 12 (upstream 15). Deviation as above. */
    public static final int DEFAULT_EXPLOSION_MIN_RADIUS = 12;
    /** Default big-explosion radius bound, blocks, exclusive: 24 (upstream 35). Deviation as above. */
    public static final int DEFAULT_EXPLOSION_MAX_RADIUS = 24;
    /** Default chance per city chunk of a mini explosion: 0.005, a sixth of upstream's 0.03. Deviation as above. */
    public static final float DEFAULT_MINI_EXPLOSION_CHANCE = 0.005f;
    /** Default smallest mini-explosion radius, blocks: 4 (upstream 5). Deviation as above. */
    public static final int DEFAULT_MINI_EXPLOSION_MIN_RADIUS = 4;
    /** Default mini-explosion radius bound, blocks, exclusive: 9 (upstream 12). Deviation as above. */
    public static final int DEFAULT_MINI_EXPLOSION_MAX_RADIUS = 9;
    /** Default chance per building of a ruined top: 0.015 (upstream 0.05). Deviation as above. */
    public static final float DEFAULT_RUIN_CHANCE = 0.015f;

    private final int groundLevel;
    private final int seaLevel;
    private final int[] cityLevelHeights;
    private final int cityMinHeight;
    private final int cityMaxHeight;
    private final int cityMaxRise;
    private final int cityGrade;
    private final int floorPitch;
    private final int cityGradeRadius;
    private final boolean cityAvoidOcean;
    private final boolean cityConnected;
    private final float cityTaper;
    private final float edgeLotChance;
    private final float packHouseChance;
    private final boolean furnish;
    private final float damageRepairChance;
    private final float facadeContrast;
    private final int heightSampleSize;
    private final int bedrockLayer;
    private final int terrainFixLowerMinOffset;
    private final int terrainFixLowerMaxOffset;
    private final int terrainFixUpperMinOffset;
    private final int terrainFixUpperMaxOffset;
    private final int oceanCorrectionBorder;
    private final float explosionChance;
    private final int explosionMinRadius;
    private final int explosionMaxRadius;
    private final int explosionMinHeight;
    private final int explosionMaxHeight;
    private final float miniExplosionChance;
    private final int miniExplosionMinRadius;
    private final int miniExplosionMaxRadius;
    private final int miniExplosionMinHeight;
    private final int miniExplosionMaxHeight;
    private final boolean explosionsInCitiesOnly;
    private final int debrisToNearbyChunkFactor;
    private final float ruinChance;
    private final float ruinMinlevelPercent;
    private final float ruinMaxlevelPercent;
    private final boolean rubbleLayer;
    private final float rubbleDirtScale;
    private final float rubbleLeaveScale;
    private final float vineChance;
    private final boolean preventLakesRavinesInCities;
    private final int softEdgeWidth;
    private final int landmarkMargin;
    private final boolean generateLighting;
    private final boolean generateSpawners;
    private final boolean generateLoot;
    private final float chestWithoutLootChance;
    private final boolean vanillaLootCategories;
    private final boolean enderZooHardMobs;
    private final boolean avoidWater;
    private final boolean bridgeSupports;
    private final float randomLeafBlockChance;
    private final int randomLeafBlockThickness;
    private final List<String> warnings;

    private TerrainSettings(Reader r) {
        groundLevel = r.intValue("groundLevel", 71, 2, WorldLimits.MAX_Y);
        seaLevel = r.intValue("seaLevel", SEA_LEVEL_DEFAULT, SEA_LEVEL_DEFAULT, WorldLimits.MAX_Y);
        cityLevelHeights = new int[CITY_LEVEL_THRESHOLDS];
        for (int i = 0; i < CITY_LEVEL_THRESHOLDS; i++) {
            cityLevelHeights[i] = r
                .intValue("cityLevel" + i + "Height", DEFAULT_LEVEL_HEIGHTS[i], 1, WorldLimits.MAX_Y);
        }
        cityMinHeight = r.intValue("cityMinHeight", 50, WorldLimits.MIN_Y, WorldLimits.MAX_Y);
        cityMaxHeight = r.intValue("cityMaxHeight", 150, WorldLimits.MIN_Y, WorldLimits.MAX_Y);
        cityMaxRise = r.intValue("cityMaxRise", DEFAULT_MAX_RISE, 0, CITY_LEVEL_THRESHOLDS);
        cityGrade = r.intValue("cityGrade", DEFAULT_GRADE, 0, CITY_LEVEL_THRESHOLDS);
        // Missing means a world pinned before the pitch was: upstream's 6 (design record 0014).
        floorPitch = r.intValue(
            "floorPitch",
            WorldLimits.LEGACY_FLOOR_PITCH,
            WorldLimits.LEGACY_FLOOR_PITCH,
            WorldLimits.MAX_FLOOR_PITCH);
        cityGradeRadius = r.intValue("cityGradeRadius", DEFAULT_GRADE_RADIUS, 0, 4);
        cityAvoidOcean = r.booleanValue("cityAvoidOcean", true);
        cityConnected = r.booleanValue("cityConnected", true);
        cityTaper = r.floatValue("cityTaper", 1.0f, 0f, 1f);
        edgeLotChance = r.floatValue("edgeLotChance", 0.45f, 0f, 1f);
        // 0 is the pre-pin world's value; new pins take the option's 0.25 (D282).
        packHouseChance = r.floatValue("packHouseChance", 0f, 0f, 1f);
        // false is the pre-pin world's value; new pins take the option's true (D282, design record 0016).
        furnish = r.booleanValue("furnish", false);
        damageRepairChance = r.floatValue("damageRepairChance", 0.5f, 0f, 1f);
        facadeContrast = r.floatValue("facadeContrast", 45f, 10f, 200f);
        heightSampleSize = r.intValue("heightSampleSize", 3, 1, 100);
        bedrockLayer = r.intValue("bedrockLayer", 1, 0, 10);
        if (bedrockLayer > 0 && r.values.containsKey("bedrockLayer")) {
            // 0005 §6: RWG already places bedrock at Y 0 to 4 and generation never places unbreakable blocks.
            r.warnings.add("bedrockLayer=" + bedrockLayer + " has no effect on RWG worlds; RWG's own bedrock is kept");
        }
        int[] lower = r.orderedPair("terrainFixLowerMinOffset", -4, "terrainFixLowerMaxOffset", -3, -40, 40);
        terrainFixLowerMinOffset = lower[0];
        terrainFixLowerMaxOffset = lower[1];
        int[] upper = r.orderedPair("terrainFixUpperMinOffset", -1, "terrainFixUpperMaxOffset", 1, -40, 40);
        terrainFixUpperMinOffset = upper[0];
        terrainFixUpperMaxOffset = upper[1];
        oceanCorrectionBorder = r.intValue("oceanCorrectionBorder", 4, -255, 255);
        explosionChance = r.floatValue("explosionChance", DEFAULT_EXPLOSION_CHANCE, 0f, 1f);
        int[] radius = r.orderedPair(
            "explosionMinRadius",
            DEFAULT_EXPLOSION_MIN_RADIUS,
            "explosionMaxRadius",
            DEFAULT_EXPLOSION_MAX_RADIUS,
            1,
            3000);
        explosionMinRadius = radius[0];
        explosionMaxRadius = radius[1];
        int[] height = r.orderedPair("explosionMinHeight", 75, "explosionMaxHeight", 90, 1, WorldLimits.MAX_Y);
        explosionMinHeight = height[0];
        explosionMaxHeight = height[1];
        miniExplosionChance = r.floatValue("miniExplosionChance", DEFAULT_MINI_EXPLOSION_CHANCE, 0f, 1f);
        int[] miniRadius = r.orderedPair(
            "miniExplosionMinRadius",
            DEFAULT_MINI_EXPLOSION_MIN_RADIUS,
            "miniExplosionMaxRadius",
            DEFAULT_MINI_EXPLOSION_MAX_RADIUS,
            1,
            3000);
        miniExplosionMinRadius = miniRadius[0];
        miniExplosionMaxRadius = miniRadius[1];
        int[] miniHeight = r
            .orderedPair("miniExplosionMinHeight", 60, "miniExplosionMaxHeight", 100, 1, WorldLimits.MAX_Y);
        miniExplosionMinHeight = miniHeight[0];
        miniExplosionMaxHeight = miniHeight[1];
        explosionsInCitiesOnly = r.booleanValue("explosionsInCitiesOnly", true);
        debrisToNearbyChunkFactor = r.intValue("debrisToNearbyChunkFactor", 200, 1, 10000);
        ruinChance = r.floatValue("ruinChance", DEFAULT_RUIN_CHANCE, 0f, 1f);
        float ruinMin = r.floatValue("ruinMinlevelPercent", 0.8f, 0f, 1f);
        float ruinMax = r.floatValue("ruinMaxlevelPercent", 1.0f, 0f, 1f);
        if (ruinMax < ruinMin) {
            r.warnings.add("ruinMaxlevelPercent < ruinMinlevelPercent; using the minimum for both");
            ruinMax = ruinMin;
        }
        ruinMinlevelPercent = ruinMin;
        ruinMaxlevelPercent = ruinMax;
        rubbleLayer = r.booleanValue("rubbleLayer", true);
        rubbleDirtScale = r.floatValue("rubbleDirtScale", 3.0f, 0f, 100f);
        rubbleLeaveScale = r.floatValue("rubbleLeaveScale", 6.0f, 0f, 100f);
        vineChance = r.floatValue("vineChance", 0.009f, 0f, 1f);
        // The profile key is preventLakesInCities (P-PROF-6); 1.12's name, which worlds pinned before M09 hold, still
        // reads.
        preventLakesRavinesInCities = r
            .booleanValue("preventLakesInCities", r.booleanValue("preventLakesRavinesInCities", true));
        softEdgeWidth = r.intValue("softEdgeWidth", 6, 2, 12);
        landmarkMargin = r.intValue("landmarkMargin", 2, 0, 16);
        generateLighting = r.booleanValue("generateLighting", false);
        generateSpawners = r.booleanValue("generateSpawners", true);
        generateLoot = r.booleanValue("generateLoot", true);
        chestWithoutLootChance = r.floatValue("chestWithoutLootChance", 0.2f, 0f, 1f);
        vanillaLootCategories = r.booleanValue("vanillaLootCategories", false);
        enderZooHardMobs = r.booleanValue("enderZooHardMobs", false);
        avoidWater = r.booleanValue("avoidWater", false);
        bridgeSupports = r.booleanValue("bridgeSupports", true);
        randomLeafBlockChance = r.floatValue("randomLeafBlockChance", 0.1f, 0f, 1f);
        randomLeafBlockThickness = r.intValue("randomLeafBlockThickness", 2, 1, 8);
        warnings = Collections.unmodifiableList(new ArrayList<>(r.warnings));
    }

    /** Returns the defaults: upstream's {@code default} profile on RWG. */
    public static TerrainSettings defaults() {
        return new TerrainSettings(new Reader(Collections.emptyMap()));
    }

    /**
     * Builds the settings from a profile snapshot's values. Missing keys take their defaults; malformed or
     * out-of-range values are replaced or clamped, and each such fix is listed in {@link #warnings()}.
     *
     * @param values option name to its text value
     * @return the settings
     */
    public static TerrainSettings fromValues(Map<String, String> values) {
        return new TerrainSettings(new Reader(values));
    }

    /**
     * Returns a float as a snapshot, a profile's canonical text and the docs write it: {@code Float.toString}'s text,
     * except that scientific notation becomes plain decimal, so the default {@code explosionChance} reads
     * {@code 0.0004} rather than {@code 4.0E-4}.
     *
     * @param value the value
     * @return its shortest plain decimal text
     */
    public static String text(float value) {
        String text = Float.toString(value);
        if (text.indexOf('E') < 0 || Float.isNaN(value) || Float.isInfinite(value)) {
            return text;
        }
        return new BigDecimal(text).stripTrailingZeros()
            .toPlainString();
    }

    /** Returns every resolved value by option name, in name order, as a snapshot stores them. */
    public Map<String, String> toValues() {
        Map<String, String> out = new TreeMap<>();
        out.put("groundLevel", Integer.toString(groundLevel));
        out.put("seaLevel", Integer.toString(seaLevel));
        for (int i = 0; i < CITY_LEVEL_THRESHOLDS; i++) {
            out.put("cityLevel" + i + "Height", Integer.toString(cityLevelHeights[i]));
        }
        out.put("cityMinHeight", Integer.toString(cityMinHeight));
        out.put("cityMaxHeight", Integer.toString(cityMaxHeight));
        out.put("cityMaxRise", Integer.toString(cityMaxRise));
        out.put("cityGrade", Integer.toString(cityGrade));
        out.put("floorPitch", Integer.toString(floorPitch));
        out.put("cityGradeRadius", Integer.toString(cityGradeRadius));
        out.put("cityAvoidOcean", Boolean.toString(cityAvoidOcean));
        out.put("cityConnected", Boolean.toString(cityConnected));
        out.put("cityTaper", text(cityTaper));
        out.put("edgeLotChance", text(edgeLotChance));
        out.put("packHouseChance", text(packHouseChance));
        out.put("furnish", Boolean.toString(furnish));
        out.put("damageRepairChance", text(damageRepairChance));
        out.put("facadeContrast", text(facadeContrast));
        out.put("heightSampleSize", Integer.toString(heightSampleSize));
        out.put("bedrockLayer", Integer.toString(bedrockLayer));
        out.put("terrainFixLowerMinOffset", Integer.toString(terrainFixLowerMinOffset));
        out.put("terrainFixLowerMaxOffset", Integer.toString(terrainFixLowerMaxOffset));
        out.put("terrainFixUpperMinOffset", Integer.toString(terrainFixUpperMinOffset));
        out.put("terrainFixUpperMaxOffset", Integer.toString(terrainFixUpperMaxOffset));
        out.put("oceanCorrectionBorder", Integer.toString(oceanCorrectionBorder));
        out.put("explosionChance", text(explosionChance));
        out.put("explosionMinRadius", Integer.toString(explosionMinRadius));
        out.put("explosionMaxRadius", Integer.toString(explosionMaxRadius));
        out.put("explosionMinHeight", Integer.toString(explosionMinHeight));
        out.put("explosionMaxHeight", Integer.toString(explosionMaxHeight));
        out.put("miniExplosionChance", text(miniExplosionChance));
        out.put("miniExplosionMinRadius", Integer.toString(miniExplosionMinRadius));
        out.put("miniExplosionMaxRadius", Integer.toString(miniExplosionMaxRadius));
        out.put("miniExplosionMinHeight", Integer.toString(miniExplosionMinHeight));
        out.put("miniExplosionMaxHeight", Integer.toString(miniExplosionMaxHeight));
        out.put("explosionsInCitiesOnly", Boolean.toString(explosionsInCitiesOnly));
        out.put("debrisToNearbyChunkFactor", Integer.toString(debrisToNearbyChunkFactor));
        out.put("ruinChance", text(ruinChance));
        out.put("ruinMinlevelPercent", text(ruinMinlevelPercent));
        out.put("ruinMaxlevelPercent", text(ruinMaxlevelPercent));
        out.put("rubbleLayer", Boolean.toString(rubbleLayer));
        out.put("rubbleDirtScale", text(rubbleDirtScale));
        out.put("rubbleLeaveScale", text(rubbleLeaveScale));
        out.put("vineChance", text(vineChance));
        out.put("preventLakesInCities", Boolean.toString(preventLakesRavinesInCities));
        out.put("softEdgeWidth", Integer.toString(softEdgeWidth));
        out.put("landmarkMargin", Integer.toString(landmarkMargin));
        out.put("generateLighting", Boolean.toString(generateLighting));
        out.put("generateSpawners", Boolean.toString(generateSpawners));
        out.put("generateLoot", Boolean.toString(generateLoot));
        out.put("chestWithoutLootChance", text(chestWithoutLootChance));
        out.put("vanillaLootCategories", Boolean.toString(vanillaLootCategories));
        out.put("enderZooHardMobs", Boolean.toString(enderZooHardMobs));
        out.put("avoidWater", Boolean.toString(avoidWater));
        out.put("bridgeSupports", Boolean.toString(bridgeSupports));
        out.put("randomLeafBlockChance", text(randomLeafBlockChance));
        out.put("randomLeafBlockThickness", Integer.toString(randomLeafBlockThickness));
        return out;
    }

    /** Returns what was fixed while reading the values, one line each; empty when nothing was. */
    public List<String> warnings() {
        return warnings;
    }

    /** Returns the Y of city level 0's ground ({@code groundLevel}, 71). */
    public int groundLevel() {
        return groundLevel;
    }

    /**
     * Returns the water level: the {@code seaLevel} override, or RWG's 63 when it is −1. Liquid fills up to this
     * level minus one, as RWG's water does (docs/new-spec/03-terrain/height-mapping.md).
     */
    public int waterLevel() {
        return seaLevel == SEA_LEVEL_DEFAULT ? WorldLimits.RWG_SEA_LEVEL : seaLevel;
    }

    /**
     * Returns the Y of a city level's ground: {@code groundLevel + 6 × level}.
     *
     * @param cityLevel the city level, 0 to 8
     * @return block Y
     */
    public int cityGround(int cityLevel) {
        return groundLevel + WorldLimits.LEVEL_HEIGHT * cityLevel;
    }

    /**
     * Returns the city level for a chunk height, by upstream's thresholds ({@code getLevelBasedOnHeight}): 0 below
     * {@code cityLevel0Height}, 1 below {@code cityLevel1Height}, and so on, 8 at or above {@code cityLevel7Height}.
     *
     * @param height the chunk's oracle height, block Y
     * @return the level, 0 to 8
     */
    public int levelForHeight(int height) {
        for (int i = 0; i < CITY_LEVEL_THRESHOLDS; i++) {
            if (height < cityLevelHeights[i]) {
                return i;
            }
        }
        return CITY_LEVEL_THRESHOLDS;
    }

    /** Returns the eight {@code cityLevel<n>Height} thresholds, block Y; a copy. */
    public int[] cityLevelHeights() {
        return cityLevelHeights.clone();
    }

    /** Returns the lowest oracle height a city chunk may have ({@code cityMinHeight}). */
    public int cityMinHeight() {
        return cityMinHeight;
    }

    /**
     * Returns how many city levels a city chunk may rise above its city's base level ({@code cityMaxRise}); at
     * {@link #CITY_LEVEL_THRESHOLDS} there is no cap.
     */
    public int cityMaxRise() {
        return cityMaxRise;
    }

    /**
     * Returns {@code floorPitch}: blocks per building floor and cellar, 6 to 10 (design record 0014). The defaults here
     * stay at upstream's 6; new worlds pin the option's default of 8.
     */
    public int floorPitch() {
        return floorPitch;
    }

    /** Returns {@code cityGrade}: the levels a city chunk may sit above or below its city's base level. */
    public int cityGrade() {
        return cityGrade;
    }

    /** Returns {@code cityGradeRadius}: the chunks around a chunk whose mean height sets its level. */
    public int cityGradeRadius() {
        return cityGradeRadius;
    }

    /** Returns {@code cityAvoidOcean}: whether ocean-biome chunks are never city. */
    public boolean cityAvoidOcean() {
        return cityAvoidOcean;
    }

    /** Returns {@code cityConnected}: whether city chunks must join their city's center. */
    public boolean cityConnected() {
        return cityConnected;
    }

    /**
     * Returns how strongly building heights follow the urban intensity ({@code cityTaper}, 0 to 1; alpha review AR-5,
     * docs/design/0011-city-edges-and-character.md §2).
     */
    public float cityTaper() {
        return cityTaper;
    }

    /** Returns whether building floors get furniture ({@code furnish}, design record 0016). */
    public boolean furnish() {
        return furnish;
    }

    /** Returns the share of empty edge lots that hold a pack mod's village house ({@code packHouseChance}). */
    public float packHouseChance() {
        return packHouseChance;
    }

    /** Returns the chance of an edge lot at a city's very edge ({@code edgeLotChance}; AR-6, 0011 §3). */
    public float edgeLotChance() {
        return edgeLotChance;
    }

    /** Returns the chance an explosion's damage is partly repaired ({@code damageRepairChance}; AR-9, 0011 §4). */
    public float damageRepairChance() {
        return damageRepairChance;
    }

    /**
     * Returns the most contrast two neighboring facades may have ({@code facadeContrast}; AR-17,
     * docs/design/0012-facade-color-feathering.md §3); 200 turns feathering off.
     */
    public float facadeContrast() {
        return facadeContrast;
    }

    /** Returns the highest oracle height a city chunk may have ({@code cityMaxHeight}). */
    public int cityMaxHeight() {
        return cityMaxHeight;
    }

    /** Returns the side of a height-sample group, in chunks ({@code heightSampleSize}). */
    public int heightSampleSize() {
        return heightSampleSize;
    }

    /** Returns {@code bedrockLayer}: read for upstream profiles, never acted on. */
    public int bedrockLayer() {
        return bedrockLayer;
    }

    /** Returns the lower offset range of a fixed blend corner, inclusive: −4. */
    public int terrainFixLowerMinOffset() {
        return terrainFixLowerMinOffset;
    }

    /** Returns the lower offset range of a fixed blend corner, inclusive: −3. */
    public int terrainFixLowerMaxOffset() {
        return terrainFixLowerMaxOffset;
    }

    /** Returns the upper offset range of a fixed blend corner, inclusive: −1. */
    public int terrainFixUpperMinOffset() {
        return terrainFixUpperMinOffset;
    }

    /** Returns the upper offset range of a fixed blend corner, inclusive: +1. */
    public int terrainFixUpperMaxOffset() {
        return terrainFixUpperMaxOffset;
    }

    /** Returns how far below the ground an ocean chunk's blend corner sits ({@code oceanCorrectionBorder}, 4). */
    public int oceanCorrectionBorder() {
        return oceanCorrectionBorder;
    }

    /** Returns the chance per chunk of a big explosion. */
    public float explosionChance() {
        return explosionChance;
    }

    /** Returns a big explosion's minimum radius, blocks. */
    public int explosionMinRadius() {
        return explosionMinRadius;
    }

    /** Returns a big explosion's maximum radius, blocks, exclusive as upstream draws it. */
    public int explosionMaxRadius() {
        return explosionMaxRadius;
    }

    /** Returns the minimum of a big explosion's center height above the city level's offset. */
    public int explosionMinHeight() {
        return explosionMinHeight;
    }

    /** Returns the maximum (exclusive) of a big explosion's center height. */
    public int explosionMaxHeight() {
        return explosionMaxHeight;
    }

    /** Returns the chance per chunk of a mini explosion. */
    public float miniExplosionChance() {
        return miniExplosionChance;
    }

    /** Returns a mini explosion's minimum radius, blocks. */
    public int miniExplosionMinRadius() {
        return miniExplosionMinRadius;
    }

    /** Returns a mini explosion's maximum radius, blocks, exclusive. */
    public int miniExplosionMaxRadius() {
        return miniExplosionMaxRadius;
    }

    /** Returns the minimum of a mini explosion's center height. */
    public int miniExplosionMinHeight() {
        return miniExplosionMinHeight;
    }

    /** Returns the maximum (exclusive) of a mini explosion's center height. */
    public int miniExplosionMaxHeight() {
        return miniExplosionMaxHeight;
    }

    /** Returns whether explosions start only in city chunks. */
    public boolean explosionsInCitiesOnly() {
        return explosionsInCitiesOnly;
    }

    /** Returns the debris divisor ({@code debrisToNearbyChunkFactor}, 200). */
    public int debrisToNearbyChunkFactor() {
        return debrisToNearbyChunkFactor;
    }

    /** Returns the chance per building of a ruined top. */
    public float ruinChance() {
        return ruinChance;
    }

    /** Returns the lower fraction of floors a ruin keeps. */
    public float ruinMinlevelPercent() {
        return ruinMinlevelPercent;
    }

    /** Returns the upper fraction of floors a ruin keeps. */
    public float ruinMaxlevelPercent() {
        return ruinMaxlevelPercent;
    }

    /** Returns whether streets and ruins get the rubble and leaf layer. */
    public boolean rubbleLayer() {
        return rubbleLayer;
    }

    /** Returns the rubble noise divisor; below 0.01 the rubble is off. */
    public float rubbleDirtScale() {
        return rubbleDirtScale;
    }

    /** Returns the leaf noise divisor; below 0.01 the leaves are off. */
    public float rubbleLeaveScale() {
        return rubbleLeaveScale;
    }

    /** Returns the chance per candidate block that a vine strip starts on a building wall. */
    public float vineChance() {
        return vineChance;
    }

    /**
     * Returns whether lakes, lava lakes, dungeons and oil are vetoed where a population area touches a city chunk
     * (1.12's option, on by default; 0005 §3).
     */
    public boolean preventLakesRavinesInCities() {
        return preventLakesRavinesInCities;
    }

    /** Returns the width of a soft-edge strip toward a landmark core, in columns (0006 §3, 6). */
    public int softEdgeWidth() {
        return softEdgeWidth;
    }

    /** Returns the margin kept clear around a landmark's footprint, in blocks (0006 §1, 2). */
    public int landmarkMargin() {
        return landmarkMargin;
    }

    /**
     * Returns {@code generateLighting}: whether a part's torch characters place torches (fixed up in the population
     * phase) or air. Upstream's default is off.
     */
    public boolean generateLighting() {
        return generateLighting;
    }

    /** Returns {@code generateSpawners}: whether spawner characters place spawners (else air). */
    public boolean generateSpawners() {
        return generateSpawners;
    }

    /** Returns {@code generateLoot}: whether chests are filled. */
    public boolean generateLoot() {
        return generateLoot;
    }

    /** Returns {@code chestWithoutLootChance}: the chance a chest stays empty, 0.2. */
    public float chestWithoutLootChance() {
        return chestWithoutLootChance;
    }

    /**
     * Returns {@code vanillaLootCategories}: whether city chests may also draw the pack's vanilla {@code ChestGenHooks}
     * categories upstream's {@code chestloot} names (off by default; loot-and-mobs.md § Upstream's vanilla-table
     * entries).
     */
    public boolean vanillaLootCategories() {
        return vanillaLootCategories;
    }

    /**
     * Returns {@code enderZooHardMobs}: whether {@code hardmobs} spawners take EnderZoo's overworld mobs instead of
     * upstream's blaze and witch (off by default; loot-and-mobs.md § Mob design).
     */
    public boolean enderZooHardMobs() {
        return enderZooHardMobs;
    }

    /** Returns {@code avoidWater}: whether a part's liquid becomes air. */
    public boolean avoidWater() {
        return avoidWater;
    }

    /** Returns {@code bridgeSupports}: whether bridges get supports down to the water. */
    public boolean bridgeSupports() {
        return bridgeSupports;
    }

    /** Returns {@code randomLeafBlockChance}: the base chance of the leaves along buildings beside streets, 0.1. */
    public float randomLeafBlockChance() {
        return randomLeafBlockChance;
    }

    /** Returns {@code randomLeafBlockThickness}: how many columns from a building those leaves reach, 2. */
    public int randomLeafBlockThickness() {
        return randomLeafBlockThickness;
    }

    /** Reads typed values out of the snapshot map, collecting what it had to fix. */
    private static final class Reader {

        private final Map<String, String> values;
        private final List<String> warnings = new ArrayList<>();

        Reader(Map<String, String> values) {
            this.values = values;
        }

        int intValue(String key, int fallback, int min, int max) {
            String text = values.get(key);
            if (text == null) {
                return fallback;
            }
            int value;
            try {
                value = Integer.parseInt(text.trim());
            } catch (NumberFormatException e) {
                warnings.add(key + "='" + text + "' is not a whole number; using " + fallback);
                return fallback;
            }
            if (value < min || value > max) {
                int clamped = Math.max(min, Math.min(max, value));
                warnings.add(key + "=" + value + " is outside " + min + ".." + max + "; clamped to " + clamped);
                return clamped;
            }
            return value;
        }

        float floatValue(String key, float fallback, float min, float max) {
            String text = values.get(key);
            if (text == null) {
                return fallback;
            }
            float value;
            try {
                value = Float.parseFloat(text.trim());
            } catch (NumberFormatException e) {
                warnings.add(key + "='" + text + "' is not a number; using " + fallback);
                return fallback;
            }
            if (Float.isNaN(value) || value < min || value > max) {
                float clamped = Float.isNaN(value) ? fallback : Math.max(min, Math.min(max, value));
                warnings.add(key + "=" + text + " is outside " + min + ".." + max + "; using " + clamped);
                return clamped;
            }
            return value;
        }

        boolean booleanValue(String key, boolean fallback) {
            String text = values.get(key);
            if (text == null) {
                return fallback;
            }
            String trimmed = text.trim();
            if ("true".equalsIgnoreCase(trimmed)) {
                return true;
            }
            if ("false".equalsIgnoreCase(trimmed)) {
                return false;
            }
            warnings.add(key + "='" + text + "' is not true or false; using " + fallback);
            return fallback;
        }

        /**
         * Reads a min/max pair. A maximum below the minimum takes the minimum, so every draw between them works and
         * an equal pair gives the minimum (TERR-03, PROF-06).
         */
        int[] orderedPair(String minKey, int minDefault, String maxKey, int maxDefault, int low, int high) {
            int min = intValue(minKey, minDefault, low, high);
            int max = intValue(maxKey, maxDefault, low, high);
            if (max < min) {
                warnings.add(maxKey + "=" + max + " is below " + minKey + "=" + min + "; using " + min + " for both");
                max = min;
            }
            return new int[] { min, max };
        }
    }
}
