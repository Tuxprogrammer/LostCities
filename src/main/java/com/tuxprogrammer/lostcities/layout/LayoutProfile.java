package com.tuxprogrammer.lostcities.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.tuxprogrammer.lostcities.layout.landmark.LandmarkKind;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkTreatment;
import com.tuxprogrammer.lostcities.layout.street.StreetPlannerSettings;

/**
 * The profile options the city layout reads, as one immutable value: the layout's narrow view of the pinned profile
 * (new-spec 02-profiles-dimensions). The profile system (M09) builds it from the loaded profile; tests build it from
 * {@link #defaults()}. Every default is upstream's {@code default} profile's value, with option names in Javadoc so the
 * fields map one to one onto upstream's keys ({@code profile-options.md}), plus the new coexistence options of 0006.
 *
 * <p>
 * Ranges are the profile loader's job: it clamps or rejects out-of-range values before a layout is built (P-PROF-1).
 * {@link #problems()} repeats the cross-option checks the layout relies on, so a test or the loader can ask.
 *
 * <p>
 * Spec: docs/original-spec/02-profiles-dimensions/profile-options.md, docs/new-spec/02-profiles-dimensions/. Side:
 * both.
 */
public final class LayoutProfile {

    /** {@code cityChance}: chance a chunk is a city center; negative selects Perlin rarity. */
    public final double cityChance;
    /** {@code cityGridCell}: jittered-grid cell edge in chunks; 0 places centers by {@code cityChance}. */
    public final int cityGridCell;
    /** {@code cityGridJitter}: chunks kept clear inside every grid cell edge. */
    public final int cityGridJitter;
    /** {@code cityMinRadius}, blocks. */
    public final int cityMinRadius;
    /** {@code cityMaxRadius}, blocks. */
    public final int cityMaxRadius;
    /** {@code cityThreshold}: a chunk is city when its factor is strictly above this. */
    public final float cityThreshold;
    /** {@code cityPerlinScale}, chunks. */
    public final double cityPerlinScale;
    /** {@code cityPerlinInnerScale}. */
    public final double cityPerlinInnerScale;
    /** {@code cityPerlinOffset}. */
    public final double cityPerlinOffset;
    /** {@code cityMinHeight}, block Y; lower terrain is never city. */
    public final int cityMinHeight;
    /** {@code cityMaxHeight}, block Y; higher terrain is never city. */
    public final int cityMaxHeight;
    /**
     * {@code cityMaxRise}, city levels: how far a city chunk's level may rise above its city's base level, the level of
     * the ground around the strongest city center (alpha review AR-4, docs/new-spec/03-terrain/height-mapping.md
     * "Rise cap"). Terrain one level above that is cut down to it; higher terrain is never city. At
     * {@link #cityLevelThresholds()} or more there is no cap, as upstream.
     */
    public final int cityMaxRise;
    /**
     * {@code cityGrade}, city levels: how far a city chunk's level may sit above or below its city's base level. Where
     * the ground is steeper, the city cuts into it or fills it, so neighboring streets and buildings stay close in
     * height instead of following every bump of the terrain. At {@link #cityLevelThresholds()} or more there is no
     * limit.
     */
    public final int cityGrade;
    /**
     * {@code cityGradeRadius}, chunks: a city chunk's level follows the mean terrain height of the
     * {@code (2r + 1) × (2r + 1)} chunks around it, so neighbors change level gradually. 0 uses the chunk's own height.
     */
    public final int cityGradeRadius;
    /** {@code cityAvoidOcean}: whether a chunk in an ocean biome is never city. */
    public final boolean cityAvoidOcean;
    /**
     * {@code cityConnected}: whether a city chunk must join its city's center through other city chunks (edge to
     * edge); a chunk cut off by terrain, water or a landmark leaves the city instead of standing alone.
     */
    public final boolean cityConnected;
    /**
     * {@code cityTaper}, 0 to 1: how strongly building heights follow the urban intensity, low at the city's edge and
     * high in its core (alpha review AR-5, docs/design/0011-city-edges-and-character.md §1 and §2); 0 is upstream's
     * floor count.
     */
    public final float cityTaper;
    /**
     * {@code floorPitch}: blocks per building floor and cellar, 6 (upstream, and worlds pinned before 0014) to 10; new
     * worlds pin 8 (docs/design/0014-floor-pitch.md). City levels stay 6 apart whatever the pitch.
     */
    public final int floorPitch;
    /**
     * {@code edgeLotChance}, 0 to 1: the chance that a building chunk at the very edge of a city is an empty or
     * construction lot instead (AR-6, 0011 §3); it falls to 0 at an intensity of 0.4.
     */
    public final float edgeLotChance;
    /**
     * {@code facadeContrast}: the most contrast two neighboring buildings' facades may have (AR-17,
     * docs/design/0012-facade-color-feathering.md §3); 200 or more turns feathering off.
     */
    public final float facadeContrast;
    /**
     * {@code densityRings}: the strength of the density rings and building classes (D306); 0 is off, the layout
     * of worlds pinned before the option.
     */
    public final float densityRings;
    /**
     * {@code lotFootprints}: whether ring construction lots take the real size, depth and frame of their ring's
     * building class (city-realism phase 3); false is the single-chunk lots of worlds pinned before the option.
     */
    public final boolean lotFootprints;
     * {@code cityHighwayLayout}: a link-network city's own highway, a beltway or a cut-through (design record 0017);
     * {@link CityHighwayLayout#NONE} in worlds pinned before the option.
     */
    public final CityHighwayLayout cityHighwayLayout;
    /** {@code citySpawnDistance1}, blocks from (0, 0). */
    public final int citySpawnDistance1;
    /** {@code citySpawnDistance2}, blocks from (0, 0); 0 disables the spawn multipliers. */
    public final int citySpawnDistance2;
    /** {@code citySpawnMultiplier1}. */
    public final double citySpawnMultiplier1;
    /** {@code citySpawnMultiplier2}. */
    public final double citySpawnMultiplier2;
    /** {@code cityStyleThreshold}; -1 is off. */
    public final float cityStyleThreshold;
    /** {@code cityStyleAlternative}: the city style used below the threshold. */
    public final String cityStyleAlternative;
    /** {@code cityLevel0Height} to {@code cityLevel7Height}, block Y. */
    private final int[] cityLevelHeights;
    /** {@code useAvgHeightmap}. */
    public final boolean useAvgHeightmap;
    /** The mod config's {@code heightSampleSize}, pinned with the profile; averaging only above 2. */
    public final int heightSampleSize;
    /** {@code multiUseCorner}: a multibuilding takes its top-left chunk's level instead of the footprint average. */
    public final boolean multiUseCorner;
    /** {@code groundLevel}: block Y of city level 0. */
    public final int groundLevel;
    /** {@code seaLevel}; -1 means RWG's sea level (63). */
    public final int seaLevel;
    /** {@code buildingChance}. */
    public final float buildingChance;
    /** {@code buildingMinFloors}. */
    public final int buildingMinFloors;
    /** {@code buildingMaxFloors}. */
    public final int buildingMaxFloors;
    /** {@code buildingMinFloorsChance}: floor-count bound at city factor -0.1. */
    public final int buildingMinFloorsChance;
    /** {@code buildingMaxFloorsChance}: floor-count bound at city factor 0.9. */
    public final int buildingMaxFloorsChance;
    /** {@code buildingMinCellars}. */
    public final int buildingMinCellars;
    /** {@code buildingMaxCellars}; the city level is added. */
    public final int buildingMaxCellars;
    /** {@code buildingDoorwayChance}. */
    public final float buildingDoorwayChance;
    /** {@code buildingWithoutLootChance}. */
    public final float buildingWithoutLootChance;
    /** {@code buildingFrontChance}. */
    public final float buildingFrontChance;
    /** {@code ruinChance}. */
    public final float ruinChance;
    /** {@code ruinMinlevelPercent}. */
    public final float ruinMinLevelPercent;
    /** {@code ruinMaxlevelPercent}. */
    public final float ruinMaxLevelPercent;
    /** {@code bridgeChance}: the legacy candidate draws, kept for the random stream. */
    public final float bridgeChance;
    /** {@code railwayDungeonChance}. */
    public final float railwayDungeonChance;
    /** {@code parkChance}. */
    public final float parkChance;
    /** {@code openLotParkChance}. */
    public final float openLotParkChance;
    /** {@code fountainChance}. */
    public final float fountainChance;
    /** {@code corridorChance}. */
    public final float corridorChance;
    /** {@code avoidFoliage}. */
    public final boolean avoidFoliage;
    /** {@code parkBorder}. */
    public final boolean parkBorder;
    /** {@code parkElevation}. */
    public final boolean parkElevation;
    /** {@code parkStreetThreshold}. */
    public final int parkStreetThreshold;
    /** {@code oceanCorrectionBorder}, blocks. */
    public final int oceanCorrectionBorder;
    /** {@code plannedPrimaryBridgeChance}. */
    public final float plannedPrimaryBridgeChance;
    /** {@code plannedPrimaryBridgeMaxLength}, gap chunks. */
    public final int plannedPrimaryBridgeMaxLength;
    /** {@code multiBuildingStreetConflict}. */
    public final MultiBuildingStreetConflict multiBuildingStreetConflict;
    /** The street field's settings ({@code primaryRoadSpacingX} and the rest). */
    public final StreetPlannerSettings streets;
    /** {@code villageTreatment}. */
    public final LandmarkTreatment villageTreatment;
    /** {@code landmarkTreatment}, per kind other than villages. */
    private final Map<LandmarkKind, LandmarkTreatment> landmarkTreatments;
    /** {@code landmarkMargin}, blocks. */
    public final int landmarkMargin;
    /** {@code villageTransitionRings}. */
    public final int villageTransitionRings;
    /** {@code villageLinkDistance}, chunks. */
    public final int villageLinkDistance;

    /** RWG's sea level, which {@code seaLevel = -1} means (new-spec 02-profiles-dimensions, height options). */
    public static final int RWG_SEA_LEVEL = 63;

    private LayoutProfile(Builder b) {
        cityChance = b.cityChance;
        cityGridCell = b.cityGridCell;
        cityGridJitter = b.cityGridJitter;
        cityMinRadius = b.cityMinRadius;
        cityMaxRadius = b.cityMaxRadius;
        cityThreshold = b.cityThreshold;
        cityPerlinScale = b.cityPerlinScale;
        cityPerlinInnerScale = b.cityPerlinInnerScale;
        cityPerlinOffset = b.cityPerlinOffset;
        cityMinHeight = b.cityMinHeight;
        cityMaxHeight = b.cityMaxHeight;
        cityMaxRise = b.cityMaxRise;
        cityGrade = b.cityGrade;
        cityGradeRadius = b.cityGradeRadius;
        cityAvoidOcean = b.cityAvoidOcean;
        cityConnected = b.cityConnected;
        cityTaper = b.cityTaper;
        floorPitch = b.floorPitch;
        edgeLotChance = b.edgeLotChance;
        facadeContrast = b.facadeContrast;
        densityRings = b.densityRings;
        lotFootprints = b.lotFootprints;
        cityHighwayLayout = b.cityHighwayLayout;
        citySpawnDistance1 = b.citySpawnDistance1;
        citySpawnDistance2 = b.citySpawnDistance2;
        citySpawnMultiplier1 = b.citySpawnMultiplier1;
        citySpawnMultiplier2 = b.citySpawnMultiplier2;
        cityStyleThreshold = b.cityStyleThreshold;
        cityStyleAlternative = b.cityStyleAlternative;
        cityLevelHeights = b.cityLevelHeights.clone();
        useAvgHeightmap = b.useAvgHeightmap;
        heightSampleSize = b.heightSampleSize;
        multiUseCorner = b.multiUseCorner;
        groundLevel = b.groundLevel;
        seaLevel = b.seaLevel;
        buildingChance = b.buildingChance;
        buildingMinFloors = b.buildingMinFloors;
        buildingMaxFloors = b.buildingMaxFloors;
        buildingMinFloorsChance = b.buildingMinFloorsChance;
        buildingMaxFloorsChance = b.buildingMaxFloorsChance;
        buildingMinCellars = b.buildingMinCellars;
        buildingMaxCellars = b.buildingMaxCellars;
        buildingDoorwayChance = b.buildingDoorwayChance;
        buildingWithoutLootChance = b.buildingWithoutLootChance;
        buildingFrontChance = b.buildingFrontChance;
        ruinChance = b.ruinChance;
        ruinMinLevelPercent = b.ruinMinLevelPercent;
        ruinMaxLevelPercent = b.ruinMaxLevelPercent;
        bridgeChance = b.bridgeChance;
        railwayDungeonChance = b.railwayDungeonChance;
        parkChance = b.parkChance;
        openLotParkChance = b.openLotParkChance;
        fountainChance = b.fountainChance;
        corridorChance = b.corridorChance;
        avoidFoliage = b.avoidFoliage;
        parkBorder = b.parkBorder;
        parkElevation = b.parkElevation;
        parkStreetThreshold = b.parkStreetThreshold;
        oceanCorrectionBorder = b.oceanCorrectionBorder;
        plannedPrimaryBridgeChance = b.plannedPrimaryBridgeChance;
        plannedPrimaryBridgeMaxLength = b.plannedPrimaryBridgeMaxLength;
        multiBuildingStreetConflict = b.multiBuildingStreetConflict;
        streets = b.streets;
        villageTreatment = b.villageTreatment;
        landmarkTreatments = Collections.unmodifiableMap(new EnumMap<>(b.landmarkTreatments));
        landmarkMargin = b.landmarkMargin;
        villageTransitionRings = b.villageTransitionRings;
        villageLinkDistance = b.villageLinkDistance;
    }

    /** Returns upstream's {@code default} profile, with the new options at their defaults. */
    public static LayoutProfile defaults() {
        return builder().build();
    }

    /** Returns a builder holding {@link #defaults()}' values. */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the terrain height at which city level {@code level + 1} starts ({@code cityLevel<level>Height}).
     *
     * @param level 0 to 7
     * @return block Y
     */
    public int cityLevelHeight(int level) {
        return cityLevelHeights[level];
    }

    /** Returns the number of level thresholds (8, so city levels run 0 to 8). */
    public int cityLevelThresholds() {
        return cityLevelHeights.length;
    }

    /**
     * Returns the city level for a terrain height: the first {@code cityLevel<n>Height} threshold the height is
     * strictly below, else {@link #cityLevelThresholds()} (docs/original-spec/04-city-layout/city-level.md).
     *
     * @param height block Y
     * @return 0 to {@link #cityLevelThresholds()}
     */
    public int levelForHeight(int height) {
        for (int level = 0; level < cityLevelHeights.length; level++) {
            if (height < cityLevelHeights[level]) {
                return level;
            }
        }
        return cityLevelHeights.length;
    }

    /** Returns whether {@link #cityMaxRise} caps anything: below {@link #cityLevelThresholds()}. */
    public boolean capsRise() {
        return cityMaxRise < cityLevelHeights.length;
    }

    /** Returns the water level the layout uses: {@link #seaLevel}, or {@link #RWG_SEA_LEVEL} when it is -1. */
    public int waterLevel() {
        return seaLevel == -1 ? RWG_SEA_LEVEL : seaLevel;
    }

    /**
     * Returns how the city treats a kind of landmark.
     *
     * @param kind the landmark's kind
     * @return {@link #villageTreatment} for villages, the per-kind {@code landmarkTreatment} otherwise
     */
    public LandmarkTreatment treatment(LandmarkKind kind) {
        if (kind == LandmarkKind.VILLAGE) {
            return villageTreatment;
        }
        LandmarkTreatment t = landmarkTreatments.get(kind);
        return t == null ? LandmarkTreatment.BUILD_AROUND : t;
    }

    /**
     * Returns the cross-option problems the layout would otherwise have to survive at generation time (PROF-06):
     * inverted min/max pairs and a floor-count bound that could fall below 1.
     *
     * @return one message per problem; empty when the profile is consistent
     */
    public List<String> problems() {
        List<String> out = new ArrayList<>();
        if (cityChance >= 0 && cityMaxRadius < cityMinRadius) {
            out.add("cityMaxRadius (" + cityMaxRadius + ") is below cityMinRadius (" + cityMinRadius + ")");
        }
        if (buildingMinFloorsChance < 1 || buildingMaxFloorsChance < 1) {
            out.add("buildingMinFloorsChance and buildingMaxFloorsChance must be at least 1");
        }
        if (buildingMaxFloors < buildingMinFloors) {
            out.add(
                "buildingMaxFloors (" + buildingMaxFloors + ") is below buildingMinFloors (" + buildingMinFloors + ")");
        }
        if (buildingMaxCellars < buildingMinCellars) {
            out.add(
                "buildingMaxCellars (" + buildingMaxCellars
                    + ") is below buildingMinCellars ("
                    + buildingMinCellars
                    + ")");
        }
        if (ruinMaxLevelPercent < ruinMinLevelPercent) {
            out.add("ruinMaxlevelPercent is below ruinMinlevelPercent");
        }
        for (int i = 1; i < cityLevelHeights.length; i++) {
            if (cityLevelHeights[i] < cityLevelHeights[i - 1]) {
                out.add("cityLevel" + i + "Height is below cityLevel" + (i - 1) + "Height");
            }
        }
        if (cityMaxRise < 0) {
            out.add("cityMaxRise must be 0 or more");
        }
        if (cityGrade < 0 || cityGradeRadius < 0) {
            out.add("cityGrade and cityGradeRadius must be 0 or more");
        }
        if (villageTransitionRings < 0 || villageLinkDistance < 0 || landmarkMargin < 0) {
            out.add("villageTransitionRings, villageLinkDistance and landmarkMargin must be 0 or more");
        }
        return out;
    }

    /** Builds a {@link LayoutProfile}; every setter is named after its profile option. */
    public static final class Builder {

        private double cityChance = .01;
        private int cityGridCell;
        private int cityGridJitter = 16;
        private int cityMinRadius = 50;
        private int cityMaxRadius = 128;
        private float cityThreshold = .2f;
        private double cityPerlinScale = 3;
        private double cityPerlinInnerScale = .1;
        private double cityPerlinOffset = .1;
        private int cityMinHeight = 50;
        private int cityMaxHeight = 150;
        private int cityMaxRise = 2;
        private int cityGrade = Integer.MAX_VALUE;
        private int cityGradeRadius;
        private boolean cityAvoidOcean;
        private boolean cityConnected;
        private float cityTaper = 1f;
        private int floorPitch = ChunkPlan.LEGACY_FLOOR_PITCH;
        private float edgeLotChance = .45f;
        private float facadeContrast = 45f;
        private float densityRings = 0f;
        private boolean lotFootprints = false;
        private CityHighwayLayout cityHighwayLayout = CityHighwayLayout.NONE;
        private int citySpawnDistance1 = 0;
        private int citySpawnDistance2 = 0;
        private double citySpawnMultiplier1 = 1.0;
        private double citySpawnMultiplier2 = 1.0;
        private float cityStyleThreshold = -1f;
        private String cityStyleAlternative = "";
        private int[] cityLevelHeights = { 75, 83, 91, 99, 107, 115, 123, 131 };
        private boolean useAvgHeightmap = false;
        private int heightSampleSize = 3;
        private boolean multiUseCorner = false;
        private int groundLevel = 71;
        private int seaLevel = -1;
        private float buildingChance = .3f;
        private int buildingMinFloors = 0;
        private int buildingMaxFloors = 8;
        private int buildingMinFloorsChance = 4;
        private int buildingMaxFloorsChance = 6;
        private int buildingMinCellars = 0;
        private int buildingMaxCellars = 3;
        private float buildingDoorwayChance = .6f;
        private float buildingWithoutLootChance = .2f;
        private float buildingFrontChance = .2f;
        // Deviation: upstream's 0.05 is turned down to 0.015 with the explosions (AR-8, TerrainSettings).
        private float ruinChance = .015f;
        private float ruinMinLevelPercent = .8f;
        private float ruinMaxLevelPercent = 1.0f;
        private float bridgeChance = .7f;
        private float railwayDungeonChance = .01f;
        private float parkChance = .2f;
        private float openLotParkChance = .8f;
        private float fountainChance = .05f;
        private float corridorChance = .7f;
        private boolean avoidFoliage = false;
        private boolean parkBorder = true;
        private boolean parkElevation = true;
        private int parkStreetThreshold = 3;
        private int oceanCorrectionBorder = 4;
        private float plannedPrimaryBridgeChance = 1.0f;
        private int plannedPrimaryBridgeMaxLength = 12;
        private MultiBuildingStreetConflict multiBuildingStreetConflict = MultiBuildingStreetConflict.OVERRIDE_MINOR;
        private StreetPlannerSettings streets = StreetPlannerSettings.DEFAULTS;
        private LandmarkTreatment villageTreatment = LandmarkTreatment.BUILD_AROUND;
        private final Map<LandmarkKind, LandmarkTreatment> landmarkTreatments = new EnumMap<>(LandmarkKind.class);
        private int landmarkMargin = 2;
        private int villageTransitionRings = 2;
        private int villageLinkDistance = 4;

        private Builder() {}

        /** @param v {@code cityChance} @return this builder */
        public Builder cityChance(double v) {
            cityChance = v;
            return this;
        }

        /**
         * @param cell {@code cityGridCell}, 0 for chance mode @param jitter {@code cityGridJitter} @return this builder
         */
        public Builder cityGrid(int cell, int jitter) {
            cityGridCell = cell;
            cityGridJitter = jitter;
            return this;
        }

        /** @param min {@code cityMinRadius} @param max {@code cityMaxRadius} @return this builder */
        public Builder cityRadius(int min, int max) {
            cityMinRadius = min;
            cityMaxRadius = max;
            return this;
        }

        /** @param v {@code cityThreshold} @return this builder */
        public Builder cityThreshold(float v) {
            cityThreshold = v;
            return this;
        }

        /**
         * @param scale      {@code cityPerlinScale}
         * @param innerScale {@code cityPerlinInnerScale}
         * @param offset     {@code cityPerlinOffset}
         * @return this builder
         */
        public Builder cityPerlin(double scale, double innerScale, double offset) {
            cityPerlinScale = scale;
            cityPerlinInnerScale = innerScale;
            cityPerlinOffset = offset;
            return this;
        }

        /** @param min {@code cityMinHeight} @param max {@code cityMaxHeight} @return this builder */
        public Builder cityHeightWindow(int min, int max) {
            cityMinHeight = min;
            cityMaxHeight = max;
            return this;
        }

        /** @param grade {@code cityGrade} @param radius {@code cityGradeRadius} @return this builder */
        public Builder cityGrading(int grade, int radius) {
            cityGrade = grade;
            cityGradeRadius = radius;
            return this;
        }

        /** @param avoidOcean {@code cityAvoidOcean} @param connected {@code cityConnected} @return this builder */
        public Builder cityShape(boolean avoidOcean, boolean connected) {
            cityAvoidOcean = avoidOcean;
            cityConnected = connected;
            return this;
        }

        /** @param levels {@code cityMaxRise}, city levels @return this builder */
        public Builder cityMaxRise(int levels) {
            cityMaxRise = levels;
            return this;
        }

        /** @param pitch {@code floorPitch}, blocks per building floor (0014) @return this builder */
        public Builder floorPitch(int pitch) {
            floorPitch = pitch;
            return this;
        }

        /** @param strength {@code densityRings}, 0 (off) to 2 @return this builder */
        public Builder densityRings(float strength) {
            densityRings = strength;
            return this;
        }

        /** @param real {@code lotFootprints}: construction lots at real sizes @return this builder */
        public Builder lotFootprints(boolean real) {
            lotFootprints = real;
            return this;
        }

        /** @param layout {@code cityHighwayLayout} (0017) @return this builder */
        public Builder cityHighwayLayout(CityHighwayLayout layout) {
            cityHighwayLayout = layout == null ? CityHighwayLayout.NONE : layout;
            return this;
        }

        /**
         * @param taper    {@code cityTaper}, 0 to 1
         * @param lots     {@code edgeLotChance}, 0 to 1
         * @param contrast {@code facadeContrast}
         * @return this builder
         */
        public Builder character(float taper, float lots, float contrast) {
            cityTaper = taper;
            edgeLotChance = lots;
            facadeContrast = contrast;
            return this;
        }

        /**
         * @param distance1   {@code citySpawnDistance1}
         * @param multiplier1 {@code citySpawnMultiplier1}
         * @param distance2   {@code citySpawnDistance2}
         * @param multiplier2 {@code citySpawnMultiplier2}
         * @return this builder
         */
        public Builder citySpawn(int distance1, double multiplier1, int distance2, double multiplier2) {
            citySpawnDistance1 = distance1;
            citySpawnMultiplier1 = multiplier1;
            citySpawnDistance2 = distance2;
            citySpawnMultiplier2 = multiplier2;
            return this;
        }

        /**
         * @param threshold   {@code cityStyleThreshold}
         * @param alternative {@code cityStyleAlternative}
         * @return this builder
         */
        public Builder cityStyleAlternative(float threshold, String alternative) {
            cityStyleThreshold = threshold;
            cityStyleAlternative = alternative;
            return this;
        }

        /** @param heights the eight {@code cityLevel<n>Height} values @return this builder */
        public Builder cityLevelHeights(int... heights) {
            if (heights.length != 8) {
                throw new IllegalArgumentException("eight city level heights expected, got " + heights.length);
            }
            cityLevelHeights = heights.clone();
            return this;
        }

        /**
         * @param use        {@code useAvgHeightmap}
         * @param sampleSize the mod config's {@code heightSampleSize}
         * @return this builder
         */
        public Builder averageHeightmap(boolean use, int sampleSize) {
            useAvgHeightmap = use;
            heightSampleSize = sampleSize;
            return this;
        }

        /** @param v {@code multiUseCorner} @return this builder */
        public Builder multiUseCorner(boolean v) {
            multiUseCorner = v;
            return this;
        }

        /** @param ground {@code groundLevel} @param sea {@code seaLevel} @return this builder */
        public Builder levels(int ground, int sea) {
            groundLevel = ground;
            seaLevel = sea;
            return this;
        }

        /** @param v {@code buildingChance} @return this builder */
        public Builder buildingChance(float v) {
            buildingChance = v;
            return this;
        }

        /**
         * @param minFloors       {@code buildingMinFloors}
         * @param maxFloors       {@code buildingMaxFloors}
         * @param minFloorsChance {@code buildingMinFloorsChance}
         * @param maxFloorsChance {@code buildingMaxFloorsChance}
         * @return this builder
         */
        public Builder floors(int minFloors, int maxFloors, int minFloorsChance, int maxFloorsChance) {
            buildingMinFloors = minFloors;
            buildingMaxFloors = maxFloors;
            buildingMinFloorsChance = minFloorsChance;
            buildingMaxFloorsChance = maxFloorsChance;
            return this;
        }

        /** @param min {@code buildingMinCellars} @param max {@code buildingMaxCellars} @return this builder */
        public Builder cellars(int min, int max) {
            buildingMinCellars = min;
            buildingMaxCellars = max;
            return this;
        }

        /**
         * @param doorway     {@code buildingDoorwayChance}
         * @param withoutLoot {@code buildingWithoutLootChance}
         * @param front       {@code buildingFrontChance}
         * @return this builder
         */
        public Builder buildingChances(float doorway, float withoutLoot, float front) {
            buildingDoorwayChance = doorway;
            buildingWithoutLootChance = withoutLoot;
            buildingFrontChance = front;
            return this;
        }

        /**
         * @param chance          {@code ruinChance}
         * @param minLevelPercent {@code ruinMinlevelPercent}
         * @param maxLevelPercent {@code ruinMaxlevelPercent}
         * @return this builder
         */
        public Builder ruins(float chance, float minLevelPercent, float maxLevelPercent) {
            ruinChance = chance;
            ruinMinLevelPercent = minLevelPercent;
            ruinMaxLevelPercent = maxLevelPercent;
            return this;
        }

        /**
         * @param bridge         {@code bridgeChance}
         * @param railwayDungeon {@code railwayDungeonChance}
         * @param corridor       {@code corridorChance}
         * @return this builder
         */
        public Builder infrastructureChances(float bridge, float railwayDungeon, float corridor) {
            bridgeChance = bridge;
            railwayDungeonChance = railwayDungeon;
            corridorChance = corridor;
            return this;
        }

        /**
         * @param park     {@code parkChance}
         * @param openLot  {@code openLotParkChance}
         * @param fountain {@code fountainChance}
         * @return this builder
         */
        public Builder streetChances(float park, float openLot, float fountain) {
            parkChance = park;
            openLotParkChance = openLot;
            fountainChance = fountain;
            return this;
        }

        /**
         * @param foliage         {@code avoidFoliage}
         * @param border          {@code parkBorder}
         * @param elevation       {@code parkElevation}
         * @param streetThreshold {@code parkStreetThreshold}
         * @return this builder
         */
        public Builder parks(boolean foliage, boolean border, boolean elevation, int streetThreshold) {
            avoidFoliage = foliage;
            parkBorder = border;
            parkElevation = elevation;
            parkStreetThreshold = streetThreshold;
            return this;
        }

        /** @param v {@code oceanCorrectionBorder} @return this builder */
        public Builder oceanCorrectionBorder(int v) {
            oceanCorrectionBorder = v;
            return this;
        }

        /**
         * @param chance    {@code plannedPrimaryBridgeChance}
         * @param maxLength {@code plannedPrimaryBridgeMaxLength}
         * @return this builder
         */
        public Builder plannedBridges(float chance, int maxLength) {
            plannedPrimaryBridgeChance = chance;
            plannedPrimaryBridgeMaxLength = maxLength;
            return this;
        }

        /** @param v {@code multiBuildingStreetConflict} @return this builder */
        public Builder multiBuildingStreetConflict(MultiBuildingStreetConflict v) {
            multiBuildingStreetConflict = v;
            return this;
        }

        /** @param v the street field's settings @return this builder */
        public Builder streets(StreetPlannerSettings v) {
            streets = v;
            return this;
        }

        /** @param v {@code villageTreatment} @return this builder */
        public Builder villageTreatment(LandmarkTreatment v) {
            villageTreatment = v;
            return this;
        }

        /**
         * @param kind      a landmark kind other than villages
         * @param treatment its {@code landmarkTreatment}
         * @return this builder
         */
        public Builder landmarkTreatment(LandmarkKind kind, LandmarkTreatment treatment) {
            if (kind == LandmarkKind.VILLAGE) {
                villageTreatment = treatment;
            } else {
                landmarkTreatments.put(kind, treatment);
            }
            return this;
        }

        /**
         * @param margin          {@code landmarkMargin}, blocks
         * @param transitionRings {@code villageTransitionRings}
         * @param linkDistance    {@code villageLinkDistance}, chunks
         * @return this builder
         */
        public Builder coexistence(int margin, int transitionRings, int linkDistance) {
            landmarkMargin = margin;
            villageTransitionRings = transitionRings;
            villageLinkDistance = linkDistance;
            return this;
        }

        /** @return the profile */
        public LayoutProfile build() {
            return new LayoutProfile(this);
        }
    }
}
