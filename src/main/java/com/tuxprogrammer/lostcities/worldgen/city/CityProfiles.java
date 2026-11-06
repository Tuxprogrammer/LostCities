package com.tuxprogrammer.lostcities.worldgen.city;

import java.util.Map;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.config.profile.ProfileValidator;
import com.tuxprogrammer.lostcities.config.profile.ProfileValues;
import com.tuxprogrammer.lostcities.core.Module;
import com.tuxprogrammer.lostcities.core.ModuleState;
import com.tuxprogrammer.lostcities.layout.LayoutProfile;
import com.tuxprogrammer.lostcities.layout.MultiBuildingStreetConflict;
import com.tuxprogrammer.lostcities.layout.NetworkSettings;
import com.tuxprogrammer.lostcities.layout.highway.HighwaySettings;
import com.tuxprogrammer.lostcities.layout.highway.PlanningGate;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkKind;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkTreatment;
import com.tuxprogrammer.lostcities.layout.rail.RailwaySettings;
import com.tuxprogrammer.lostcities.layout.street.StreetPlannerSettings;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainSettings;

/**
 * Builds the layout's view of a world's pinned profile ({@link LayoutProfile}): the values the terrain also reads come
 * from the world's {@link TerrainSettings} (ground and sea level, the city level thresholds, the height window, the
 * height sample size, the rise cap, the ruin chances, the ocean correction border and the landmark margin), so both
 * halves of a world
 * read the same clamped numbers; every other layout option comes from the pinned profile's values
 * (docs/new-spec/02-profiles-dimensions/profile-options.md).
 *
 * <p>
 * Pure. Side: both (plain Java).
 */
public final class CityProfiles {

    /** The world style of upstream's {@code default} profile, used when a profile names none. */
    public static final AssetId WORLD_STYLE = AssetId.parse("lostcities:standard");

    private CityProfiles() {}

    /**
     * Returns the layout profile of pinned settings with every other option at its default.
     *
     * @param settings the world's pinned settings
     * @return the profile
     */
    public static LayoutProfile layoutProfile(TerrainSettings settings) {
        return layoutProfile(settings, ProfileValues.defaults());
    }

    /**
     * Returns the layout profile of a world's pinned settings and profile values.
     *
     * @param settings the world's pinned terrain settings
     * @param values   the world's pinned profile values
     * @return the profile
     */
    public static LayoutProfile layoutProfile(TerrainSettings settings, ProfileValues values) {
        LayoutProfile.Builder builder = LayoutProfile.builder()
            .levels(settings.groundLevel(), settings.waterLevel())
            .cityLevelHeights(settings.cityLevelHeights())
            .cityHeightWindow(settings.cityMinHeight(), settings.cityMaxHeight())
            .cityMaxRise(settings.cityMaxRise())
            .cityGrading(settings.cityGrade(), settings.cityGradeRadius())
            .floorPitch(settings.floorPitch())
            .densityRings(settings.densityRings())
            .lotFootprints(settings.lotFootprints())
            .cityDistricts(settings.cityDistricts())
            .cityShape(settings.cityAvoidOcean(), settings.cityConnected())
            .character(settings.cityTaper(), settings.edgeLotChance(), settings.facadeContrast())
            .averageHeightmap(values.getBoolean("useAvgHeightmap"), settings.heightSampleSize())
            .ruins(settings.ruinChance(), settings.ruinMinlevelPercent(), settings.ruinMaxlevelPercent())
            .oceanCorrectionBorder(settings.oceanCorrectionBorder())
            .coexistence(
                settings.landmarkMargin(),
                values.getInt("villageTransitionRings"),
                values.getInt("villageLinkDistance"))
            .cityChance(values.getDouble("cityChance"))
            .cityGrid(
                "grid".equals(values.getString("cityPlacement")) ? values.getInt("cityGridCell") : 0,
                values.getInt("cityGridJitter"))
            .cityRadius(values.getInt("cityMinRadius"), values.getInt("cityMaxRadius"))
            .cityThreshold(values.getFloat("cityThreshold"))
            .cityPerlin(
                values.getDouble("cityPerlinScale"),
                values.getDouble("cityPerlinInnerScale"),
                values.getDouble("cityPerlinOffset"))
            .citySpawn(
                values.getInt("citySpawnDistance1"),
                values.getDouble("citySpawnMultiplier1"),
                values.getInt("citySpawnDistance2"),
                values.getDouble("citySpawnMultiplier2"))
            .cityStyleAlternative(values.getFloat("cityStyleThreshold"), values.getString("cityStyleAlternative"))
            .multiUseCorner(values.getBoolean("multiUseCorner"))
            .buildingChance(values.getFloat("buildingChance"))
            .floors(
                values.getInt("buildingMinFloors"),
                values.getInt("buildingMaxFloors"),
                values.getInt("buildingMinFloorsChance"),
                values.getInt("buildingMaxFloorsChance"))
            .cellars(values.getInt("buildingMinCellars"), values.getInt("buildingMaxCellars"))
            .buildingChances(
                values.getFloat("buildingDoorwayChance"),
                values.getFloat("buildingWithoutLootChance"),
                values.getFloat("buildingFrontChance"))
            .infrastructureChances(
                values.getFloat("bridgeChance"),
                values.getFloat("railwayDungeonChance"),
                values.getFloat("corridorChance"))
            .streetChances(
                values.getFloat("parkChance"),
                values.getFloat("openLotParkChance"),
                values.getFloat("fountainChance"))
            .parks(
                values.getBoolean("avoidFoliage"),
                values.getBoolean("parkBorder"),
                values.getBoolean("parkElevation"),
                values.getInt("parkStreetThreshold"))
            .plannedBridges(
                values.getFloat("plannedPrimaryBridgeChance"),
                values.getInt("plannedPrimaryBridgeMaxLength"))
            .multiBuildingStreetConflict(conflict(values.getString("multiBuildingStreetConflict")))
            .streets(streets(values))
            .villageTreatment(treatment(values.getString("villageTreatment")));
        for (Map.Entry<LandmarkKind, LandmarkTreatment> entry : treatments(values.getString("landmarkTreatment"))
            .entrySet()) {
            builder.landmarkTreatment(entry.getKey(), entry.getValue());
        }
        return builder.build();
    }

    /**
     * Returns a world's highway and railway settings (M06) from its pinned profile values and module flags: a module
     * switched off and the profile's own switch both turn their network off. Values a validated profile always has in
     * range; should a pinned value still fail the planner's checks, upstream's defaults stand in, so a world is never
     * refused for its snapshot (P-PROF-7).
     *
     * @param values  the world's pinned profile values
     * @param modules the world's pinned module flags
     * @param gate    schedules the highway planner's hub cells
     * @return the settings
     */
    public static NetworkSettings networkSettings(ProfileValues values, ModuleState modules, PlanningGate gate) {
        boolean highways = modules.isActive(Module.HIGHWAYS) && values.getBoolean("highwaysEnabled");
        boolean railways = modules.isActive(Module.RAILWAYS);
        HighwaySettings highwaySettings;
        try {
            highwaySettings = HighwaySettings.builder()
                .enabled(highways)
                .supports(values.getBoolean("highwaySupports"))
                .hubs(
                    values.getInt("highwayPlanningCellSize"),
                    values.getInt("highwayHubSampleSpacing"),
                    values.getFloat("highwayHubMinimumPotential"),
                    values.getInt("highwayHubSearchRadiusCells"))
                .connections(
                    values.getInt("highwayMinimumHubDistance"),
                    values.getInt("highwayMaximumHubDistance"),
                    values.getInt("highwayMaximumConnectionsPerHub"),
                    values.getInt("highwayMinimumRouteLength"),
                    values.getFloat("highwayRouteCityPenalty"))
                .levels(values.getInt("highwayLevelFromCities"), values.getInt("highwayNetworkLevel"))
                .railwaySpacing(values.getInt("railwaySpacingNorthSouth"), values.getInt("railwaySpacingEastWest"))
                .build();
        } catch (IllegalArgumentException e) {
            highwaySettings = HighwaySettings.builder()
                .enabled(highways)
                .build();
        }
        RailwaySettings railwaySettings;
        try {
            railwaySettings = new RailwaySettings(
                railways && values.getBoolean("railwaysEnabled"),
                railways && values.getBoolean("railwayStationsEnabled"),
                values.getBoolean("railwaySurfaceStationsEnabled"),
                values.getBoolean("railwaysCanEnd"),
                values.getInt("railwayLevelOffset"),
                values.getInt("railwaySpacingNorthSouth"),
                values.getInt("railwaySpacingEastWest"));
        } catch (IllegalArgumentException e) {
            railwaySettings = new RailwaySettings(railways, railways, true, false, 0, 10, 10);
        }
        boolean links;
        try {
            links = "links".equals(values.getString("networkLayout"));
        } catch (IllegalArgumentException e) {
            links = false;
        }
        return new NetworkSettings(highwaySettings, railwaySettings, gate, railways).withLinks(links);
    }

    /**
     * Returns the world style a profile names, as an asset ID; {@link #WORLD_STYLE} when it names none or a malformed
     * name (the registry rejects a profile whose world style is not loaded, so this is a last resort).
     *
     * @param values the pinned profile values
     * @return the world style's ID
     */
    public static AssetId worldStyle(ProfileValues values) {
        String name = values.getString("worldStyle")
            .trim();
        if (name.isEmpty()) {
            return WORLD_STYLE;
        }
        try {
            return AssetId.parse(name);
        } catch (IllegalArgumentException e) {
            return WORLD_STYLE;
        }
    }

    /**
     * Returns the street field's settings from the profile's road options; upstream's defaults when the pinned values
     * fail its checks, which a validated profile never does (a world is never refused for its snapshot, P-PROF-7).
     */
    static StreetPlannerSettings streets(ProfileValues values) {
        try {
            return roadSettings(values);
        } catch (IllegalArgumentException e) {
            return StreetPlannerSettings.DEFAULTS;
        }
    }

    private static StreetPlannerSettings roadSettings(ProfileValues values) {
        return new StreetPlannerSettings(
            values.getInt("primaryRoadSpacingX"),
            values.getInt("primaryRoadSpacingZ"),
            values.getFloat("primaryRoadOptionalChance"),
            values.getInt("primaryRoadForceEvery"),
            values.getInt("secondaryRoadMinCountX"),
            values.getInt("secondaryRoadMaxCountX"),
            values.getInt("secondaryRoadMinCountZ"),
            values.getInt("secondaryRoadMaxCountZ"),
            values.getInt("minimumRoadSeparation"),
            values.getInt("minimumRoadEdgeDistance"),
            values.getFloat("tertiaryRoadChance"),
            values.getInt("tertiaryRoadMinLength"),
            values.getInt("tertiaryRoadMaxLength"));
    }

    private static MultiBuildingStreetConflict conflict(String name) {
        for (MultiBuildingStreetConflict value : MultiBuildingStreetConflict.values()) {
            if (value.name()
                .equalsIgnoreCase(name)) {
                return value;
            }
        }
        return MultiBuildingStreetConflict.OVERRIDE_MINOR;
    }

    private static LandmarkTreatment treatment(String name) {
        for (LandmarkTreatment value : LandmarkTreatment.values()) {
            if (value.name()
                .equalsIgnoreCase(name)) {
                return value;
            }
        }
        return LandmarkTreatment.BUILD_AROUND;
    }

    private static Map<LandmarkKind, LandmarkTreatment> treatments(String text) {
        try {
            return ProfileValidator.parseLandmarkTreatments(text);
        } catch (IllegalArgumentException e) {
            return ProfileValidator.parseLandmarkTreatments("");
        }
    }
}
