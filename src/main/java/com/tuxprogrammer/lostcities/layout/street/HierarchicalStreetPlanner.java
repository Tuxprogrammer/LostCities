package com.tuxprogrammer.lostcities.layout.street;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/street/HierarchicalStreetPlanner.java

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.tuxprogrammer.lostcities.layout.random.DimensionKey;
import com.tuxprogrammer.lostcities.layout.random.StableHash;

/**
 * Upstream's {@code HIERARCHICAL_GRID_V1} road field: primary roads on a jittered grid that runs the length of the
 * dimension, secondary roads inside each block, and short tertiary dead ends inside each cell. It is a pure function of
 * the world seed, the dimension ID and the settings; it knows nothing about cities, which clip it afterwards
 * ({@link EffectiveStreetResolver}). Every hash constant is upstream's, so for the same seed and dimension ID the field
 * is upstream's field (checked against values recorded from upstream in
 * {@code src/test/resources/parity/streets_*.txt}).
 *
 * <p>
 * The only mode ported; upstream's {@code LEGACY} street mode is dropped (decision log D11).
 *
 * <p>
 * Spec: docs/original-spec/04-city-layout/street-planner.md. Side: both.
 */
public final class HierarchicalStreetPlanner {

    /** "HGRIDV1" plus a version byte. */
    private static final long VERSION_SALT = 0x4847524944563101L;
    private static final long PRIMARY_X_SALT = 0x5f6c1d8a29e34b71L;
    private static final long PRIMARY_Z_SALT = 0x731ab9654ce20f8dL;
    private static final long PRIMARY_X_ACTIVATION_SALT = 0x2c64f8a10d735be9L;
    private static final long PRIMARY_Z_ACTIVATION_SALT = 0x58e307c4b92a6df1L;
    private static final long DENSITY_SALT = 0x26f40b9157acde31L;
    private static final long SECONDARY_X_COUNT_SALT = 0x18d2ca7645bf903eL;
    private static final long SECONDARY_Z_COUNT_SALT = 0x47a90edb1c6352f8L;
    private static final long SECONDARY_X_POSITION_SALT = 0x60b54df1298cae37L;
    private static final long SECONDARY_Z_POSITION_SALT = 0x39e71ac405d82bf6L;
    private static final long TERTIARY_CHANCE_SALT = 0x7b21c0e49fa3568dL;
    private static final long TERTIARY_SIDE_SALT = 0x14d8f72ca963be50L;
    private static final long TERTIARY_ORIGIN_SALT = 0x52ae901bd4763cf8L;
    private static final long TERTIARY_LENGTH_SALT = 0x6c03f1a984d27be5L;
    /** Per-argument offsets of {@link #hash}, upstream's. */
    private static final long X_OFFSET = 0x9e3779b97f4a7c15L;
    private static final long Z_OFFSET = 0xc2b2ae3d27d4eb4fL;
    private static final long EXTRA_OFFSET = 0x165667b19e3779f9L;
    /** Multiplier that folds a cell's two indices into the tertiary hash's extra argument. */
    private static final int CELL_KEY_MULTIPLIER = 257;
    /** Share of a block's density in its secondary-road count; the rest is per-axis variation. */
    private static final double DENSITY_SHARE = .75;
    private static final double VARIATION_SHARE = .25;
    /** A tertiary road needs a cell at least this many chunks wide across it. */
    private static final int TERTIARY_MIN_TRANSVERSE_SPAN = 4;
    /** Chunks kept clear between a tertiary road's origin and the cell's corners. */
    private static final int TERTIARY_ORIGIN_MARGIN = 2;

    private final long seed;
    private final long dimensionSalt;
    private final StreetPlannerSettings settings;
    private final int primaryOffsetX;
    private final int primaryOffsetZ;

    /**
     * Creates the field for one world and dimension.
     *
     * @param seed      the world seed
     * @param dimension the dimension, hashed by its upstream 1.20 ID ({@link DimensionKey})
     * @param settings  the profile's street settings
     */
    public HierarchicalStreetPlanner(long seed, DimensionKey dimension, StreetPlannerSettings settings) {
        this.seed = seed;
        this.dimensionSalt = dimension.stableHash();
        this.settings = settings;
        this.primaryOffsetX = StableHash.floorModHash(hash(PRIMARY_X_SALT, 0, 0, 0), settings.primarySpacingX());
        this.primaryOffsetZ = StableHash.floorModHash(hash(PRIMARY_Z_SALT, 0, 0, 0), settings.primarySpacingZ());
    }

    /** Returns the world's primary grid offset along X, in chunks. */
    public int primaryOffsetX() {
        return primaryOffsetX;
    }

    /** Returns the world's primary grid offset along Z, in chunks. */
    public int primaryOffsetZ() {
        return primaryOffsetZ;
    }

    /** Returns the settings. */
    public StreetPlannerSettings settings() {
        return settings;
    }

    /**
     * Returns the raw road class of a chunk.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the class before any city clip
     */
    public PlannedRoadType getRoadType(int chunkX, int chunkZ) {
        return rawAt(chunkX, chunkZ).roadType;
    }

    /**
     * Returns the raw street field at a chunk, with its connections to the four neighbors.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the raw street info
     */
    public PlannedStreetInfo getStreetInfo(int chunkX, int chunkZ) {
        RawRoad raw = rawAt(chunkX, chunkZ);
        boolean road = raw.roadType != PlannedRoadType.NONE;
        boolean north = road && getRoadType(chunkX, chunkZ - 1) != PlannedRoadType.NONE;
        boolean south = road && getRoadType(chunkX, chunkZ + 1) != PlannedRoadType.NONE;
        boolean west = road && getRoadType(chunkX - 1, chunkZ) != PlannedRoadType.NONE;
        boolean east = road && getRoadType(chunkX + 1, chunkZ) != PlannedRoadType.NONE;
        return new PlannedStreetInfo(raw.roadType, north, south, west, east, raw.block, raw.tertiary);
    }

    private RawRoad rawAt(int chunkX, int chunkZ) {
        BlockLayout block = getBlockLayout(chunkX, chunkZ);
        if (isVerticalPrimary(chunkX) || isHorizontalPrimary(chunkZ)) {
            return new RawRoad(PlannedRoadType.PRIMARY, block, null);
        }
        if (block.secondaryX()
            .contains(chunkX)
            || block.secondaryZ()
                .contains(chunkZ)) {
            return new RawRoad(PlannedRoadType.SECONDARY, block, null);
        }
        TertiaryRoadSegment tertiary = getTertiarySegment(block, chunkX, chunkZ);
        if (tertiary != null && tertiary.contains(chunkX, chunkZ)) {
            return new RawRoad(PlannedRoadType.TERTIARY, block, tertiary);
        }
        return new RawRoad(PlannedRoadType.NONE, block, tertiary);
    }

    /**
     * Returns the block a chunk lies in. Candidate indices use floor division, so there is no seam at negative
     * coordinates; the block's west and north primaries are the nearest active candidates at or before the chunk's,
     * its east and south ones the first active candidates after them.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the block
     */
    public BlockLayout getBlockLayout(int chunkX, int chunkZ) {
        int candidateX = Math.toIntExact(Math.floorDiv((long) chunkX - primaryOffsetX, settings.primarySpacingX()));
        int candidateZ = Math.toIntExact(Math.floorDiv((long) chunkZ - primaryOffsetZ, settings.primarySpacingZ()));
        int blockX = findActiveAtOrBefore(candidateX, true);
        int blockZ = findActiveAtOrBefore(candidateZ, false);
        int nextBlockX = findActiveAfter(blockX, true);
        int nextBlockZ = findActiveAfter(blockZ, false);
        int westX = candidateCoordinate(primaryOffsetX, blockX, settings.primarySpacingX());
        int northZ = candidateCoordinate(primaryOffsetZ, blockZ, settings.primarySpacingZ());
        int eastX = candidateCoordinate(primaryOffsetX, nextBlockX, settings.primarySpacingX());
        int southZ = candidateCoordinate(primaryOffsetZ, nextBlockZ, settings.primarySpacingZ());
        double density = StableHash.unitDouble(hash(DENSITY_SALT, blockX, blockZ, 0));
        int countX = selectCount(
            settings.secondaryMinCountX(),
            settings.secondaryMaxCountX(),
            density,
            hash(SECONDARY_X_COUNT_SALT, blockX, blockZ, 0));
        int countZ = selectCount(
            settings.secondaryMinCountZ(),
            settings.secondaryMaxCountZ(),
            density,
            hash(SECONDARY_Z_COUNT_SALT, blockX, blockZ, 0));
        List<Integer> secondaryX = selectSecondaryPositions(
            blockX,
            blockZ,
            westX,
            eastX - westX,
            countX,
            SECONDARY_X_POSITION_SALT);
        List<Integer> secondaryZ = selectSecondaryPositions(
            blockX,
            blockZ,
            northZ,
            southZ - northZ,
            countZ,
            SECONDARY_Z_POSITION_SALT);
        return new BlockLayout(blockX, blockZ, westX, northZ, eastX, southZ, density, secondaryX, secondaryZ);
    }

    /**
     * Returns whether a chunk column lies on an active north-south primary road.
     *
     * @param chunkX chunk X
     * @return whether the column is on an active primary line
     */
    public boolean isVerticalPrimary(int chunkX) {
        long relative = (long) chunkX - primaryOffsetX;
        if (Math.floorMod(relative, (long) settings.primarySpacingX()) != 0) {
            return false;
        }
        int candidate = Math.toIntExact(Math.floorDiv(relative, (long) settings.primarySpacingX()));
        return isActivePrimaryCandidate(candidate, true);
    }

    /**
     * Returns whether a chunk row lies on an active east-west primary road.
     *
     * @param chunkZ chunk Z
     * @return whether the row is on an active primary line
     */
    public boolean isHorizontalPrimary(int chunkZ) {
        long relative = (long) chunkZ - primaryOffsetZ;
        if (Math.floorMod(relative, (long) settings.primarySpacingZ()) != 0) {
            return false;
        }
        int candidate = Math.toIntExact(Math.floorDiv(relative, (long) settings.primarySpacingZ()));
        return isActivePrimaryCandidate(candidate, false);
    }

    private boolean isActivePrimaryCandidate(int candidate, boolean xAxis) {
        if (Math.floorMod(candidate, settings.primaryForceEvery()) == 0) {
            return true;
        }
        long salt = xAxis ? PRIMARY_X_ACTIVATION_SALT : PRIMARY_Z_ACTIVATION_SALT;
        return StableHash.unitDouble(hash(salt, xAxis ? candidate : 0, xAxis ? 0 : candidate, 0))
            < settings.primaryOptionalChance();
    }

    private int findActiveAtOrBefore(int candidate, boolean xAxis) {
        for (int distance = 0; distance < settings.primaryForceEvery(); distance++) {
            int current = Math.subtractExact(candidate, distance);
            if (isActivePrimaryCandidate(current, xAxis)) {
                return current;
            }
        }
        // Unreachable: every primaryForceEvery-th candidate is active.
        throw new IllegalStateException("No forced primary candidate found");
    }

    private int findActiveAfter(int candidate, boolean xAxis) {
        for (int distance = 1; distance <= settings.primaryForceEvery(); distance++) {
            int current = Math.addExact(candidate, distance);
            if (isActivePrimaryCandidate(current, xAxis)) {
                return current;
            }
        }
        // Unreachable: every primaryForceEvery-th candidate is active.
        throw new IllegalStateException("No forced primary candidate found");
    }

    private static int candidateCoordinate(int offset, int candidate, int spacing) {
        return Math.toIntExact((long) offset + (long) candidate * spacing);
    }

    private static int selectCount(int minimum, int maximum, double density, long variationHash) {
        if (minimum == maximum) {
            return minimum;
        }
        // The density is shared by both axes; a smaller per-axis share keeps the X and Z counts from always matching.
        double variedDensity = density * DENSITY_SHARE + StableHash.unitDouble(variationHash) * VARIATION_SHARE;
        int count = minimum + (int) Math.floor(variedDensity * (maximum - minimum + 1));
        return Math.min(maximum, count);
    }

    private List<Integer> selectSecondaryPositions(int blockX, int blockZ, int start, int spacing, int desired,
        long salt) {
        if (desired == 0) {
            return Collections.emptyList();
        }
        int first = settings.minimumEdgeDistance();
        int last = spacing - settings.minimumEdgeDistance();
        if (first > last) {
            return Collections.emptyList();
        }
        List<Integer> candidates = new ArrayList<>();
        for (int local = first; local <= last; local++) {
            candidates.add(local);
        }
        candidates.sort((a, b) -> {
            int comparison = Long.compareUnsigned(hash(salt, blockX, blockZ, a), hash(salt, blockX, blockZ, b));
            return comparison != 0 ? comparison : Integer.compare(a, b);
        });
        List<Integer> selected = new ArrayList<>();
        for (int local : candidates) {
            if (isSeparated(selected, local)) {
                selected.add(local);
                if (selected.size() == desired) {
                    break;
                }
            }
        }
        Collections.sort(selected);
        List<Integer> positions = new ArrayList<>(selected.size());
        for (int local : selected) {
            positions.add(start + local);
        }
        return positions;
    }

    private boolean isSeparated(List<Integer> selected, int local) {
        for (int existing : selected) {
            if (Math.abs(existing - local) < settings.minimumRoadSeparation()) {
                return false;
            }
        }
        return true;
    }

    private TertiaryRoadSegment getTertiarySegment(BlockLayout block, int chunkX, int chunkZ) {
        List<Integer> xRoads = boundaries(block.westX(), block.secondaryX(), block.eastX());
        List<Integer> zRoads = boundaries(block.northZ(), block.secondaryZ(), block.southZ());
        int cellX = findCell(xRoads, chunkX);
        int cellZ = findCell(zRoads, chunkZ);
        if (cellX < 0 || cellZ < 0) {
            return null;
        }
        int x0 = xRoads.get(cellX);
        int x1 = xRoads.get(cellX + 1);
        int z0 = zRoads.get(cellZ);
        int z1 = zRoads.get(cellZ + 1);
        int cellKey = cellX * CELL_KEY_MULTIPLIER + cellZ;
        long identity = hash(TERTIARY_CHANCE_SALT, block.blockX(), block.blockZ(), cellKey);
        if (StableHash.unitDouble(identity) >= settings.tertiaryChance()) {
            return null;
        }
        RoadDirection direction = selectTertiaryDirection(block, cellKey, x0, x1, z0, z1);
        if (direction == null) {
            return null;
        }
        boolean vertical = direction.isNorthSouth();
        int interiorLength = vertical ? z1 - z0 - 1 : x1 - x0 - 1;
        int maxLength = Math.min(settings.tertiaryMaxLength(), interiorLength - 1);
        if (maxLength < settings.tertiaryMinLength()) {
            return null;
        }
        int lengthRange = maxLength - settings.tertiaryMinLength() + 1;
        int length = settings.tertiaryMinLength()
            + StableHash.floorModHash(hash(TERTIARY_LENGTH_SALT, block.blockX(), block.blockZ(), cellKey), lengthRange);
        long originHash = hash(TERTIARY_ORIGIN_SALT, block.blockX(), block.blockZ(), cellKey);
        int originX;
        int originZ;
        if (vertical) {
            int originMin = x0 + TERTIARY_ORIGIN_MARGIN;
            int originMax = x1 - TERTIARY_ORIGIN_MARGIN;
            if (originMin > originMax) {
                return null;
            }
            originX = originMin + StableHash.floorModHash(originHash, originMax - originMin + 1);
            originZ = direction == RoadDirection.SOUTH ? z0 : z1;
        } else {
            int originMin = z0 + TERTIARY_ORIGIN_MARGIN;
            int originMax = z1 - TERTIARY_ORIGIN_MARGIN;
            if (originMin > originMax) {
                return null;
            }
            originX = direction == RoadDirection.EAST ? x0 : x1;
            originZ = originMin + StableHash.floorModHash(originHash, originMax - originMin + 1);
        }
        return new TertiaryRoadSegment(identity, originX, originZ, direction, length);
    }

    private static List<Integer> boundaries(int low, List<Integer> secondary, int high) {
        List<Integer> roads = new ArrayList<>(secondary.size() + 2);
        roads.add(low);
        roads.addAll(secondary);
        roads.add(high);
        return roads;
    }

    /**
     * Keeps the hashed side as the first choice but walks the other sides in order when it cannot fit, so a narrow
     * cell is not dropped only because its first side is too short. Deterministic.
     */
    private RoadDirection selectTertiaryDirection(BlockLayout block, int cellKey, int x0, int x1, int z0, int z1) {
        RoadDirection[] directions = RoadDirection.values();
        int first = StableHash
            .floorModHash(hash(TERTIARY_SIDE_SALT, block.blockX(), block.blockZ(), cellKey), directions.length);
        for (int offset = 0; offset < directions.length; offset++) {
            RoadDirection direction = directions[(first + offset) % directions.length];
            boolean vertical = direction.isNorthSouth();
            int transverseSpan = vertical ? x1 - x0 : z1 - z0;
            int inwardSpan = vertical ? z1 - z0 : x1 - x0;
            if (transverseSpan >= TERTIARY_MIN_TRANSVERSE_SPAN
                && inwardSpan - TERTIARY_ORIGIN_MARGIN >= settings.tertiaryMinLength()) {
                return direction;
            }
        }
        return null;
    }

    private static int findCell(List<Integer> roads, int coordinate) {
        for (int i = 0; i < roads.size() - 1; i++) {
            if (coordinate > roads.get(i) && coordinate < roads.get(i + 1)) {
                return i;
            }
        }
        return -1;
    }

    private long hash(long streamSalt, int x, int z, int extra) {
        long value = seed ^ dimensionSalt ^ VERSION_SALT ^ streamSalt;
        value ^= StableHash.mix64(Integer.toUnsignedLong(x) + X_OFFSET);
        value ^= StableHash.mix64(Integer.toUnsignedLong(z) + Z_OFFSET);
        value ^= StableHash.mix64(Integer.toUnsignedLong(extra) + EXTRA_OFFSET);
        return StableHash.mix64(value);
    }

    /** One chunk's raw road, its block and its cell's tertiary segment. */
    private static final class RawRoad {

        final PlannedRoadType roadType;
        final BlockLayout block;
        final TertiaryRoadSegment tertiary;

        RawRoad(PlannedRoadType roadType, BlockLayout block, TertiaryRoadSegment tertiary) {
            this.roadType = roadType;
            this.block = block;
            this.tertiary = tertiary;
        }
    }
}
