package com.tuxprogrammer.lostcities.layout;

import com.tuxprogrammer.lostcities.layout.landmark.LaneCell;
import com.tuxprogrammer.lostcities.layout.landmark.ZoneInfo;
import com.tuxprogrammer.lostcities.layout.street.PlannedRoadType;

/**
 * The second planning stage of a chunk (upstream's {@code LostChunkCharacteristics}): city membership, multibuilding
 * position, city level, raw and effective road, whether a building could stand, the city style and the candidate
 * building; plus, here, the chunk's landmark zone and sewing role, which come first. Immutable; the
 * {@link LayoutPlanner} computes and caches it.
 *
 * <p>
 * Spec: docs/original-spec/04-city-layout/chunk-characteristics.md, docs/new-spec/04-city-layout/landmark-zones.md.
 * Side: both.
 */
public final class ChunkCharacteristics {

    final int chunkX;
    final int chunkZ;
    final ZoneInfo zone;
    final LaneCell lane;
    final boolean isCity;
    final MultiPos multiPos;
    final String multiBuilding;
    final int cityLevel;
    final PlannedRoadType rawRoad;
    final PlannedRoadType plannedRoad;
    final boolean couldHaveBuilding;
    final String cityStyle;
    final String buildingType;
    /** How many 64-bit values this stage drew from the chunk's building random (P04-2: the plan continues after). */
    final long draws;

    ChunkCharacteristics(int chunkX, int chunkZ, ZoneInfo zone, LaneCell lane, boolean isCity, MultiPos multiPos,
        String multiBuilding, int cityLevel, PlannedRoadType rawRoad, PlannedRoadType plannedRoad,
        boolean couldHaveBuilding, String cityStyle, String buildingType, long draws) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.zone = zone;
        this.lane = lane;
        this.isCity = isCity;
        this.multiPos = multiPos;
        this.multiBuilding = multiBuilding;
        this.cityLevel = cityLevel;
        this.rawRoad = rawRoad;
        this.plannedRoad = plannedRoad;
        this.couldHaveBuilding = couldHaveBuilding;
        this.cityStyle = cityStyle;
        this.buildingType = buildingType;
        this.draws = draws;
    }

    /** Returns chunk X. */
    public int chunkX() {
        return chunkX;
    }

    /** Returns chunk Z. */
    public int chunkZ() {
        return chunkZ;
    }

    /** Returns the chunk's landmark zone. */
    public ZoneInfo zone() {
        return zone;
    }

    /** Returns the chunk's sewing role. */
    public LaneCell lane() {
        return lane;
    }

    /** Returns whether the chunk is city: raw city, and not a landmark core or avoid ring. */
    public boolean isCity() {
        return isCity;
    }

    /** Returns the chunk's multibuilding position, {@link MultiPos#SINGLE} when it is not part of one. */
    public MultiPos multiPos() {
        return multiPos;
    }

    /** Returns the multibuilding's name, or {@code null}. */
    public String multiBuilding() {
        return multiBuilding;
    }

    /** Returns the city level, 0 to 8 (shared by every chunk of a multibuilding). */
    public int cityLevel() {
        return cityLevel;
    }

    /** Returns the raw road class of the street field. */
    public PlannedRoadType rawRoad() {
        return rawRoad;
    }

    /** Returns the effective road class: clipped to the city, with detours and lanes forced. */
    public PlannedRoadType plannedRoad() {
        return plannedRoad;
    }

    /** Returns whether a building could stand here. */
    public boolean couldHaveBuilding() {
        return couldHaveBuilding;
    }

    /** Returns the city style's name, after the street-majority vote for city chunks without a building. */
    public String cityStyle() {
        return cityStyle;
    }

    /** Returns the candidate building's name; every chunk has one when its style lists buildings, or {@code null}. */
    public String buildingType() {
        return buildingType;
    }
}
