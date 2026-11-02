package com.tuxprogrammer.lostcities.worldgen.terrain;
// Ported from Lost Cities (MIT): https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/LostCityTerrainFeature.java
// Avoids: TERR-06, TERR-24, TERR-31

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.worldgen.buffer.ChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;

/**
 * The terrain work upstream's {@code doCityChunk} does around a city chunk's content, for the {@code DEFAULT}
 * landscape on RWG's finished chunk: high water, leveling every column to the city ground, making room for a
 * building, clearing above the roof, the stone base under streets and the street-edge borders.
 *
 * <p>
 * Spec: docs/original-spec/03-terrain/city-chunk-terrain.md; deviations in docs/new-spec/03-terrain/index.md ("City
 * chunk terrain"). Changes from upstream:
 * <ul>
 * <li>No bedrock floor: RWG already places bedrock at Y 0 to 4 and generation never places unbreakable blocks
 * (0005 §6);</li>
 * <li>toward a landmark core there is no border wall: leveling follows the {@link SoftEdges} strip instead (0006
 * §4);</li>
 * <li>TERR-31: the building's bottom comes from the immutable plan, which keeps it at or above Y 2, never from a
 * query that can fail;</li>
 * <li>TERR-06 and TERR-24: clearing and every bound use {@link ColumnOps} and {@link WorldLimits}.</li>
 * </ul>
 *
 * <p>
 * Side: both (plain Java).
 */
public final class CityTerrain {

    /** Depth of a street border below the city ground where the land is higher than the city (blocks). */
    static final int BORDER_FALLBACK_DEPTH = 3;

    /** How far above a building's oracle height {@link #clearToMax} clears (blocks). */
    static final int CLEAR_ABOVE_HEIGHT = 10;

    private CityTerrain() {}

    /**
     * Upstream's high-water rule: with a water level above the profile's ground level (a drowned city), liquid from
     * the ground level up to the water level minus one in every column. Unreachable with RWG's sea level 63 unless a
     * profile overrides {@code seaLevel}.
     *
     * @param buffer  the chunk
     * @param context the world's terrain context
     * @param plan    the chunk's plan
     */
    public static void highWater(ChunkBuffer buffer, TerrainContext context, ChunkPlan plan) {
        TerrainSettings settings = context.settings();
        if (settings.waterLevel() <= settings.groundLevel()) {
            return;
        }
        BlockRef liquid = plan.style()
            .liquid();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                ColumnOps.fill(buffer, x, z, settings.groundLevel(), settings.waterLevel(), liquid);
            }
        }
    }

    /**
     * Levels every column: lowers it to its target with {@code moveDown}, or raises it with {@code moveUp} when it
     * is lower. The target is the city ground, or the soft-edge strip's height toward a core.
     *
     * @param buffer  the chunk
     * @param context the world's terrain context
     * @param targets 256 target top Ys, index {@code z × 16 + x}
     */
    public static void level(ChunkBuffer buffer, TerrainContext context, int[] targets) {
        BlockClassifier classes = context.classifier();
        int waterLevel = context.settings()
            .waterLevel();
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int target = targets[(z << 4) | x];
                int touched = ColumnOps
                    .moveDown(buffer, classes, x, z, target + 1, WorldLimits.BUILD_HEIGHT, waterLevel);
                if (touched == ColumnOps.NOT_TOUCHED) {
                    ColumnOps.moveUp(buffer, classes, x, z, target);
                }
            }
        }
    }

    /**
     * Upstream's {@code makeRoomForBuilding} for the {@code DEFAULT} landscape: a border skirt on the chunk's edge
     * columns from the lower of this and the adjacent chunk's height up to the building's lowest level, the filler
     * at the lowest level where it is air, then the building's volume cleared (with water below the water level in
     * a drowned city).
     *
     * @param buffer  the chunk
     * @param context the world's terrain context
     * @param plan    the chunk's plan; must have a building
     * @param sides   the plans toward −X, +X, −Z and +Z
     * @param skip    columns to leave alone (soft-edge strips), index {@code z × 16 + x}; {@code null} for none
     */
    public static void makeRoomForBuilding(ChunkBuffer buffer, TerrainContext context, ChunkPlan plan,
        ChunkPlan[] sides, boolean[] skip) {
        TerrainSettings settings = context.settings();
        int lowest = plan.buildingBottom();
        int top = plan.groundY() + WorldLimits.FLOOR_HEIGHT * plan.floors();
        boolean withWater = settings.waterLevel() > settings.groundLevel();
        BlockRef border = plan.style()
            .border();
        BlockRef filler = plan.style()
            .filler();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (skip != null && skip[(z << 4) | x]) {
                    continue;
                }
                if (isSide(x, z)) {
                    int y = minHeightAt(plan, sides, x, z);
                    if (y >= lowest) {
                        // The building sits below the land here, so only a thin border.
                        y = lowest - BORDER_FALLBACK_DEPTH;
                    }
                    ColumnOps.fill(buffer, x, z, y, lowest, border);
                }
                if (buffer.get(x, lowest, z)
                    .isAir()) {
                    buffer.set(x, lowest, z, filler);
                }
                ColumnOps.clearRange(
                    buffer,
                    x,
                    z,
                    lowest,
                    top,
                    withWater,
                    settings.waterLevel(),
                    plan.style()
                        .liquid());
            }
        }
    }

    /**
     * Upstream's {@code clearToMax}: clears every column from {@code fromY} up to the lower of Y 248 and the chunk's
     * oracle height plus ten, before a roof is written.
     *
     * @param buffer the chunk
     * @param plan   the chunk's plan
     * @param fromY  the roof's base Y
     */
    public static void clearToMax(ChunkBuffer buffer, ChunkPlan plan, int fromY) {
        int max = Math.min(WorldLimits.BUILDING_TOP_CAP, plan.naturalHeight() + CLEAR_ABOVE_HEIGHT);
        if (fromY >= max) {
            return;
        }
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                ColumnOps.clearRange(buffer, x, z, fromY, max, false, 0, BlockRef.AIR);
            }
        }
    }

    /**
     * Upstream's {@code generateBorders} for the {@code DEFAULT} landscape: the base under the street
     * ({@code fillToBedrockStreetBlock}), then a border along each side whose neighbor is lower or not a city, with
     * the low wall on top when the chunk may hold a street or park. Sides facing a landmark core get no border: their
     * soft-edge strip was leveled instead.
     *
     * @param buffer     the chunk
     * @param context    the world's terrain context
     * @param plan       the chunk's plan
     * @param sides      the plans toward −X, +X, −Z and +Z
     * @param canDoParks whether the street could hold a street or park (no highway at its level)
     * @param skip       columns to leave alone (soft-edge strips); {@code null} for none
     */
    public static void streetBaseAndBorders(ChunkBuffer buffer, TerrainContext context, ChunkPlan plan,
        ChunkPlan[] sides, boolean canDoParks, boolean[] skip) {
        BlockClassifier classes = context.classifier();
        int ground = plan.groundY();
        BlockRef base = plan.style()
            .base();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (skip != null && skip[(z << 4) | x]) {
                    continue;
                }
                ColumnOps.fillEmptyBelow(buffer, classes, x, z, ground - 1, base);
            }
        }
        for (ChunkPlan.Side side : ChunkPlan.Side.values()) {
            ChunkPlan adjacent = sides[side.ordinal()];
            if (adjacent.isCore() || !doBorder(plan, adjacent)) {
                continue;
            }
            int openings = plan.borderOpenings(side);
            for (int along = 0; along < 16; along++) {
                int x = side == ChunkPlan.Side.WEST ? 0 : side == ChunkPlan.Side.EAST ? 15 : along;
                int z = side == ChunkPlan.Side.NORTH ? 0 : side == ChunkPlan.Side.SOUTH ? 15 : along;
                if (skip != null && skip[(z << 4) | x]) {
                    continue;
                }
                boolean opening = canDoParks && (openings & (1 << along)) != 0;
                borderColumn(buffer, plan, sides, x, z, canDoParks, opening);
            }
        }
    }

    /**
     * Upstream's {@code doBorder} for the {@code DEFAULT} landscape: a border where the neighbor is a lower city
     * chunk ({@code isHigherThenNearbyStreetChunk}) or a non-city chunk at this level or below.
     *
     * @param plan     this chunk's plan
     * @param adjacent the neighbor's plan
     * @return whether that side gets a border
     */
    static boolean doBorder(ChunkPlan plan, ChunkPlan adjacent) {
        if (adjacent.isCity()) {
            if (adjacent.hasBuilding()) {
                return adjacent.cityLevel() + adjacent.floors() < plan.cityLevel();
            }
            return adjacent.cityLevel() < plan.cityLevel();
        }
        return adjacent.cityLevel() <= plan.cityLevel();
    }

    private static void borderColumn(ChunkBuffer buffer, ChunkPlan plan, ChunkPlan[] sides, int x, int z,
        boolean canDoParks, boolean opening) {
        int ground = plan.groundY();
        int borderTop = ground + (opening ? 0 : 1);
        BlockRef border = plan.style()
            .border();
        int y = minHeightAt(plan, sides, x, z);
        if (y < ground + 1) {
            // The land is lower: a border from there up to the street.
            ColumnOps.fill(buffer, x, z, y - 1, borderTop, border);
        } else {
            // The land is higher: a thin border anyway.
            ColumnOps.fill(buffer, x, z, ground - BORDER_FALLBACK_DEPTH, borderTop, border);
        }
        if (canDoParks && ground + 1 <= WorldLimits.MAX_Y) {
            buffer.set(
                x,
                ground + 1,
                z,
                opening ? BlockRef.AIR
                    : plan.style()
                        .wall());
        }
    }

    /**
     * Upstream's {@code getMinHeightAt}: the chunk's oracle height, or on an edge column the lower of it and the
     * adjacent chunk's (the diagonal one at a corner).
     *
     * @param plan  this chunk's plan
     * @param sides the plans toward −X, +X, −Z and +Z
     * @param x     local X
     * @param z     local Z
     * @return block Y
     */
    static int minHeightAt(ChunkPlan plan, ChunkPlan[] sides, int x, int z) {
        int height = plan.naturalHeight();
        boolean west = x == 0;
        boolean east = x == 15;
        boolean north = z == 0;
        boolean south = z == 15;
        if (!west && !east && !north && !south) {
            return height;
        }
        // Corners use the lower of both sides, standing in for the diagonal chunk the plan array does not hold.
        int adjacent = Integer.MAX_VALUE;
        if (west) {
            adjacent = Math.min(adjacent, sides[ChunkPlan.Side.WEST.ordinal()].naturalHeight());
        }
        if (east) {
            adjacent = Math.min(adjacent, sides[ChunkPlan.Side.EAST.ordinal()].naturalHeight());
        }
        if (north) {
            adjacent = Math.min(adjacent, sides[ChunkPlan.Side.NORTH.ordinal()].naturalHeight());
        }
        if (south) {
            adjacent = Math.min(adjacent, sides[ChunkPlan.Side.SOUTH.ordinal()].naturalHeight());
        }
        return Math.min(height, adjacent);
    }

    private static boolean isSide(int x, int z) {
        return x == 0 || x == 15 || z == 0 || z == 15;
    }
}
