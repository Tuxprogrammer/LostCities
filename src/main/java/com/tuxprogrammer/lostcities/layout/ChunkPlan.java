package com.tuxprogrammer.lostcities.layout;

import java.util.Collections;
import java.util.List;

import com.tuxprogrammer.lostcities.layout.landmark.LaneCell;
import com.tuxprogrammer.lostcities.layout.landmark.ZoneInfo;
import com.tuxprogrammer.lostcities.layout.street.PlannedRoadType;

/**
 * Everything the layout decides about one chunk on its own (upstream's {@code BuildingInfo}, minus the block writing
 * and the queries that read neighbors, which {@link LayoutPlanner} answers): whether it is city, its building with
 * floors, cellars and the part of every level, its street type, park, fountain and front parts, its bridge and stair
 * parts, palette choices, loot and ruin decisions, corridor candidates and its landmark zone and sewing role.
 *
 * <p>
 * Immutable and final once built (decision log D9): nothing rewrites it during generation, so neighbors and queries
 * always see the same plan (CITY-13, CITY-18, CITY-19, CITY-20 cannot occur). Floors and cellars are already fitted to
 * the 1.7.10 world: the roof stays below Y 248 and the lowest cellar above Y 2.
 *
 * <p>
 * Levels: index {@code i} of the per-level arrays is local floor {@code i - cellars()}; index {@code cellars()} is the
 * ground floor and the last index, {@code cellars() + floors()}, the roof part.
 *
 * <p>
 * Spec: docs/original-spec/04-city-layout/building-info.md, docs/new-spec/04-city-layout/index.md. Side: both.
 */
public final class ChunkPlan {

    /** Blocks per city level and per building floor (upstream's {@code FLOORHEIGHT}). */
    public static final int FLOOR_HEIGHT = 6;

    final ChunkCharacteristics characteristics;
    final boolean isCity;
    final boolean hasBuilding;
    final PlannedRoadType plannedRoad;
    final boolean hierarchicalOpen;
    final boolean predefinedStreet;
    final int groundLevel;
    final int waterLevel;
    final EffectiveCitySettings settings;
    final int highwayXLevel;
    final int highwayZLevel;
    final StreetType streetType;
    final String fountainPart;
    final String parkPart;
    final int floors;
    final int drawnFloors;
    final int cellars;
    final DoorKind door;
    final String bridgePart;
    final String largeBridgePart;
    final String stairPart;
    final float stairPriority;
    final List<String> palettes;
    final boolean noLoot;
    final float ruinHeight;
    final String[] floorParts;
    final String[] floorParts2;
    final boolean[] connectionAtX;
    final boolean[] connectionAtZ;
    final boolean xRailCorridor;
    final boolean zRailCorridor;
    final String railDungeonPart;
    final String frontPart;
    final float intensity;
    final LotKind lot;
    final int lotDepth;
    final int lotColumnFloors;

    ChunkPlan(Builder b) {
        characteristics = b.characteristics;
        isCity = b.isCity;
        hasBuilding = b.hasBuilding;
        plannedRoad = b.plannedRoad;
        hierarchicalOpen = b.hierarchicalOpen;
        predefinedStreet = b.predefinedStreet;
        groundLevel = b.groundLevel;
        waterLevel = b.waterLevel;
        settings = b.settings;
        highwayXLevel = b.highwayXLevel;
        highwayZLevel = b.highwayZLevel;
        streetType = b.streetType;
        fountainPart = b.fountainPart;
        parkPart = b.parkPart;
        floors = b.floors;
        drawnFloors = b.drawnFloors;
        cellars = b.cellars;
        door = b.door;
        bridgePart = b.bridgePart;
        largeBridgePart = b.largeBridgePart;
        stairPart = b.stairPart;
        stairPriority = b.stairPriority;
        palettes = Collections.unmodifiableList(b.palettes);
        noLoot = b.noLoot;
        ruinHeight = b.ruinHeight;
        floorParts = b.floorParts;
        floorParts2 = b.floorParts2;
        connectionAtX = b.connectionAtX;
        connectionAtZ = b.connectionAtZ;
        xRailCorridor = b.xRailCorridor;
        zRailCorridor = b.zRailCorridor;
        railDungeonPart = b.railDungeonPart;
        frontPart = b.frontPart;
        intensity = b.intensity;
        lot = b.lot;
        lotDepth = b.lotDepth;
        lotColumnFloors = b.lotColumnFloors;
    }

    /** Returns chunk X. */
    public int chunkX() {
        return characteristics.chunkX;
    }

    /** Returns chunk Z. */
    public int chunkZ() {
        return characteristics.chunkZ;
    }

    /** Returns the characteristics stage this plan was built from. */
    public ChunkCharacteristics characteristics() {
        return characteristics;
    }

    /** Returns the chunk's landmark zone. */
    public ZoneInfo zone() {
        return characteristics.zone;
    }

    /** Returns the chunk's role in sewing a landmark into the city (detour, village lane, gate, link path). */
    public LaneCell lane() {
        return characteristics.lane;
    }

    /** Returns whether the chunk is city. */
    public boolean isCity() {
        return isCity;
    }

    /** Returns whether a building stands here. */
    public boolean hasBuilding() {
        return hasBuilding;
    }

    /** Returns whether the chunk is a city chunk without a building: a street, plaza, park or open lot. */
    public boolean isStreetOrParkSection() {
        return isCity && !hasBuilding;
    }

    /** Returns the multibuilding position, {@link MultiPos#SINGLE} for a single chunk. */
    public MultiPos multiPos() {
        return characteristics.multiPos;
    }

    /** Returns the multibuilding's name, or {@code null}. */
    public String multiBuilding() {
        return characteristics.multiBuilding;
    }

    /** Returns the building's name when a building stands here, otherwise {@code null}. */
    public String building() {
        return hasBuilding ? characteristics.buildingType : null;
    }

    /** Returns the city style's name. */
    public String cityStyle() {
        return characteristics.cityStyle;
    }

    /** Returns the city level, 0 to 8. */
    public int cityLevel() {
        return characteristics.cityLevel;
    }

    /** Returns the profile's ground level: block Y of city level 0. */
    public int groundLevel() {
        return groundLevel;
    }

    /** Returns the water level, block Y. */
    public int waterLevel() {
        return waterLevel;
    }

    /** Returns the block Y of this chunk's street or ground floor: {@code groundLevel + cityLevel * 6}. */
    public int cityGroundLevel() {
        return groundLevel + characteristics.cityLevel * FLOOR_HEIGHT;
    }

    /** Returns {@link #cityGroundLevel()}, minus 1 for non-city chunks (upstream's terrain reference). */
    public int cityGroundLevelOutsideLower() {
        return isCity ? cityGroundLevel() : cityGroundLevel() - 1;
    }

    /** Returns the raw road class of the street field at this chunk. */
    public PlannedRoadType rawRoad() {
        return characteristics.rawRoad;
    }

    /** Returns the effective road class: {@link PlannedRoadType#NONE} unless this is a city street. */
    public PlannedRoadType plannedRoad() {
        return plannedRoad;
    }

    /** Returns whether a planned road runs here. */
    public boolean isPlannedRoad() {
        return plannedRoad != PlannedRoadType.NONE;
    }

    /** Returns whether a primary road runs here (whose surface may come from the primary-roads pool, P03-5). */
    public boolean isPrimaryRoad() {
        return plannedRoad == PlannedRoadType.PRIMARY;
    }

    /** Returns whether this is an open lot: city, no building, no road, no predefined street. */
    public boolean isHierarchicalOpen() {
        return hierarchicalOpen;
    }

    /** Returns whether a predefined street occupies the chunk. */
    public boolean isPredefinedStreet() {
        return predefinedStreet;
    }

    /** Returns the city settings resolved for this chunk's style and profile. */
    public EffectiveCitySettings settings() {
        return settings;
    }

    /** Returns the east-west highway's city level, or -1. */
    public int highwayXLevel() {
        return highwayXLevel;
    }

    /** Returns the north-south highway's city level, or -1. */
    public int highwayZLevel() {
        return highwayZLevel;
    }

    /** Returns the higher of the two highway levels, or -1 when there is none. */
    public int maxHighwayLevel() {
        return Math.max(highwayXLevel, highwayZLevel);
    }

    /** Returns the street type: {@link StreetType#FULL} is a plaza facing a landmark. */
    public StreetType streetType() {
        return streetType;
    }

    /** Returns the fountain part, or {@code null}. */
    public String fountainPart() {
        return fountainPart;
    }

    /** Returns the park part, or {@code null} (an open lot without a park part is plain grass). */
    public String parkPart() {
        return parkPart;
    }

    /** Returns the floors above ground, not counting the roof part. */
    public int floors() {
        return floors;
    }

    /** Returns the cellars below ground. */
    public int cellars() {
        return cellars;
    }

    /** Returns the block Y of the lowest cellar's floor: {@code cityGroundLevel - cellars * 6}. */
    public int buildingBottomHeight() {
        return cityGroundLevel() - cellars * FLOOR_HEIGHT;
    }

    /** Returns the city level of the lowest cellar ({@code cityLevel - cellars}), for railway collision tests. */
    public int lowestCityLevel() {
        return characteristics.cityLevel - cellars;
    }

    /**
     * Returns the top of the chunk: the building's roof floor ({@code cityGround + floors * 6}) when a building stands,
     * the highway's level when one passes, else the city ground (upstream's {@code getMaxHeight}).
     */
    public int maxHeight() {
        if (hasBuilding) {
            return cityGroundLevel() + floors * FLOOR_HEIGHT;
        }
        if (maxHighwayLevel() >= 0) {
            return groundLevel + maxHighwayLevel() * FLOOR_HEIGHT;
        }
        return cityGroundLevel();
    }

    /** Returns the building's door. */
    public DoorKind door() {
        return door;
    }

    /** Returns the city style's bridge part drawn for this chunk, or {@code null}. */
    public String bridgePart() {
        return bridgePart;
    }

    /** Returns the large bridge part for primary spans, or {@code null}. */
    public String largeBridgePart() {
        return largeBridgePart;
    }

    /** Returns the stair part, or {@code null}. */
    public String stairPart() {
        return stairPart;
    }

    /** Returns the stair priority: among competing stairs the highest wins. */
    public float stairPriority() {
        return stairPriority;
    }

    /** Returns the chosen palette names, one per random-palette group of the chunk's style. */
    public List<String> palettes() {
        return palettes;
    }

    /** Returns whether the building's chests stay empty. */
    public boolean noLoot() {
        return noLoot;
    }

    /** Returns the ruin height as a fraction of the building's height, or -1 when it is not a ruin. */
    public float ruinHeight() {
        return ruinHeight;
    }

    /**
     * Returns the block Y the ruin layer centers on, or -1 (upstream's {@code getRuinLevel}).
     *
     * @param ruinChance the profile's {@code ruinChance}
     * @return block Y, or -1
     */
    public int ruinLevel(float ruinChance) {
        if (ruinChance <= 0 || ruinHeight < 0) {
            return -1;
        }
        return (int) (cityGroundLevel() + 1 + ruinHeight * floors * FLOOR_HEIGHT);
    }

    /**
     * Returns whether a highway at a city level must be a tunnel here: in a city when the city stands above it, else
     * when the terrain does (upstream's {@code isTunnel}).
     *
     * @param level         the highway's city level
     * @param terrainHeight the oracle's height of this chunk
     * @return whether the highway tunnels
     */
    public boolean isTunnel(int level, int terrainHeight) {
        if (isCity) {
            return characteristics.cityLevel > level;
        }
        return terrainHeight > groundLevel + level * FLOOR_HEIGHT + 3;
    }

    /** Returns the number of levels a building's part arrays hold: {@code cellars + floors + 1}. */
    public int levels() {
        return floorParts.length;
    }

    /**
     * Returns whether a local floor is part of this chunk's building column.
     *
     * @param localFloor 0 the ground floor, negative a cellar, {@link #floors()} the roof
     * @return whether the floor exists
     */
    public boolean isValidFloor(int localFloor) {
        int index = localFloor + cellars;
        return index >= 0 && index < floorParts.length;
    }

    /**
     * Returns the part of a local floor.
     *
     * @param localFloor 0 the ground floor, negative a cellar, {@link #floors()} the roof
     * @return the part name, or {@code null} outside the building or where no part was chosen
     */
    public String floorPart(int localFloor) {
        return isValidFloor(localFloor) ? floorParts[localFloor + cellars] : null;
    }

    /**
     * Returns the overlay part ({@code parts2}) of a local floor.
     *
     * @param localFloor the local floor
     * @return the overlay part, or {@code null}
     */
    public String floorPart2(int localFloor) {
        return isValidFloor(localFloor) ? floorParts2[localFloor + cellars] : null;
    }

    /**
     * Returns the random doorway flag of the west side at a level index (0 the lowest cellar).
     *
     * @param level the level index
     * @return the drawn flag; {@code false} outside the arrays
     */
    public boolean connectionFlagX(int level) {
        return level >= 0 && level < connectionAtX.length && connectionAtX[level];
    }

    /**
     * Returns the random doorway flag of the north side at a level index (0 the lowest cellar).
     *
     * @param level the level index
     * @return the drawn flag; {@code false} outside the arrays
     */
    public boolean connectionFlagZ(int level) {
        return level >= 0 && level < connectionAtZ.length && connectionAtZ[level];
    }

    /** Returns whether this chunk is a candidate for an east-west rail corridor (no guarantee one runs). */
    public boolean xRailCorridor() {
        return xRailCorridor;
    }

    /** Returns whether this chunk is a candidate for a north-south rail corridor. */
    public boolean zRailCorridor() {
        return zRailCorridor;
    }

    /** Returns whether a rail corridor may pass: city, and no building or a building without cellars. */
    public boolean canRailGoThrough() {
        return isCity && (!hasBuilding || cellars == 0);
    }

    /** Returns the rail dungeon part, or {@code null}. */
    public String railDungeonPart() {
        return railDungeonPart;
    }

    /** Returns the building's front part, or {@code null}. */
    public String frontPart() {
        return frontPart;
    }

    /**
     * Returns the chunk's urban intensity, 0 at a city's very edge to 1 in its core (docs/design/0008, §1); 0 outside
     * cities.
     */
    public float intensity() {
        return intensity;
    }

    /** Returns the chunk's edge lot, {@link LotKind#NONE} for every other chunk (0008 §3). */
    public LotKind lot() {
        return lot;
    }

    /** Returns how deep a dug lot is, in blocks below city ground: 1, 2 or 4; 0 for any other chunk. */
    public int lotDepth() {
        return lotDepth;
    }

    /** Returns how many floors a foundation's columns rise above city ground: 1 to 3; 0 for any other chunk. */
    public int lotColumnFloors() {
        return lotColumnFloors;
    }

    @Override
    public String toString() {
        return "plan(" + chunkX()
            + ","
            + chunkZ()
            + (isCity ? " city" : "")
            + (hasBuilding ? " " + building() : "")
            + ")";
    }

    /** Collects a plan's fields while the planner builds it. */
    static final class Builder {

        ChunkCharacteristics characteristics;
        boolean isCity;
        boolean hasBuilding;
        PlannedRoadType plannedRoad = PlannedRoadType.NONE;
        boolean hierarchicalOpen;
        boolean predefinedStreet;
        int groundLevel;
        int waterLevel;
        EffectiveCitySettings settings;
        int highwayXLevel = -1;
        int highwayZLevel = -1;
        StreetType streetType = StreetType.NORMAL;
        String fountainPart;
        String parkPart;
        int floors;
        int drawnFloors;
        int cellars;
        DoorKind door = DoorKind.OAK;
        String bridgePart;
        String largeBridgePart;
        String stairPart;
        float stairPriority;
        List<String> palettes = Collections.emptyList();
        boolean noLoot;
        float ruinHeight = -1;
        String[] floorParts = new String[1];
        String[] floorParts2 = new String[1];
        boolean[] connectionAtX = new boolean[1];
        boolean[] connectionAtZ = new boolean[1];
        boolean xRailCorridor;
        boolean zRailCorridor;
        String railDungeonPart;
        String frontPart;
        float intensity;
        LotKind lot = LotKind.NONE;
        int lotDepth;
        int lotColumnFloors;
    }
}
