package com.tuxprogrammer.lostcities.layout.street;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/street/HierarchicalBridgePlanner.java

import com.tuxprogrammer.lostcities.layout.random.DimensionKey;
import com.tuxprogrammer.lostcities.layout.random.StableHash;

/**
 * Bridges short water gaps along the raw primary-road field. For a non-city chunk on a primary road, it walks along the
 * road both ways across gap chunks to the first raw-city chunk with a raw primary road, and accepts the span when it is
 * short enough, both endpoints are effective primary roads at city level 0, and the span hash passes
 * {@code plannedPrimaryBridgeChance}. At a primary intersection only the world's preferred orientation can be a gap,
 * so a chunk never carries two bridges.
 *
 * <p>
 * Upstream reads {@code BuildingInfo}s directly; here the planner asks a {@link Context}, which the layout planner
 * answers from its plans, so the bridge decision stays a pure function of seed, coordinates, profile and terrain. Every
 * hash is upstream's (checked against {@code src/test/resources/parity/bridge_hashes.txt}).
 *
 * <p>
 * Deviation (new-spec 04-city-layout, bridges): a landmark core is never a gap chunk, so a span never crosses one
 * (0006 §1); the context reports it.
 *
 * <p>
 * Spec: docs/original-spec/04-city-layout/bridges.md. Side: both.
 */
public final class HierarchicalBridgePlanner {

    private static final long BRIDGE_SPAN_SALT = 0x31d6a7f04c82be59L;
    private static final long BRIDGE_INTERSECTION_SALT = 0x67a14ce3b9052df8L;
    private static final long MIN_X_OFFSET = 0x9e3779b97f4a7c15L;
    private static final long MIN_Z_OFFSET = 0xc2b2ae3d27d4eb4fL;
    private static final long MAX_X_OFFSET = 0x165667b19e3779f9L;
    private static final long MAX_Z_OFFSET = 0x85ebca77c2b2ae63L;
    private static final long X_ORIENTATION_SALT = 0x243f6a8885a308d3L;
    private static final long Z_ORIENTATION_SALT = 0x13198a2e03707344L;

    /** What the bridge planner needs to know about chunks; the layout planner implements it from its plans. */
    public interface Context {

        /**
         * Returns whether a chunk is raw city (city factor above the threshold; no landmark or plan state).
         *
         * @param chunkX chunk X
         * @param chunkZ chunk Z
         * @return whether the chunk is raw city
         */
        boolean isCityRaw(int chunkX, int chunkZ);

        /**
         * Returns whether a chunk's terrain asks for a bridge: a water biome, or an oracle height below the water
         * level, which catches inland lakes in plains or forest biomes.
         *
         * @param chunkX chunk X
         * @param chunkZ chunk Z
         * @return whether the chunk is water for bridging
         */
        boolean isWater(int chunkX, int chunkZ);

        /**
         * Returns whether a chunk lies in a landmark core, which no span may cross.
         *
         * @param chunkX chunk X
         * @param chunkZ chunk Z
         * @return whether the chunk is a core chunk
         */
        boolean isLandmarkCore(int chunkX, int chunkZ);

        /**
         * Returns whether a chunk's final plan is an effective primary road at city level 0, as a span's endpoints must
         * be ("higher levels need ramp assets", upstream).
         *
         * @param chunkX chunk X
         * @param chunkZ chunk Z
         * @return whether the chunk can anchor a span
         */
        boolean isLevelZeroPrimaryRoad(int chunkX, int chunkZ);
    }

    private final long seed;
    private final long dimensionHash;
    private final HierarchicalStreetPlanner streets;
    private final float bridgeChance;
    private final int maximumLength;
    private final Orientation preferredIntersection;

    /**
     * Creates the planner for one world and dimension.
     *
     * @param seed          the world seed
     * @param dimension     the dimension, hashed by its upstream ID
     * @param streets       the street field
     * @param bridgeChance  {@code plannedPrimaryBridgeChance}, 0 to 1
     * @param maximumLength {@code plannedPrimaryBridgeMaxLength}, in gap chunks
     */
    public HierarchicalBridgePlanner(long seed, DimensionKey dimension, HierarchicalStreetPlanner streets,
        float bridgeChance, int maximumLength) {
        this.seed = seed;
        this.dimensionHash = dimension.stableHash();
        this.streets = streets;
        this.bridgeChance = bridgeChance;
        this.maximumLength = maximumLength;
        this.preferredIntersection = preferredIntersectionOrientation(seed, dimension);
    }

    /**
     * Returns the span through a chunk along an orientation, or {@code null}.
     *
     * @param context     what the planner may ask about chunks
     * @param chunkX      chunk X
     * @param chunkZ      chunk Z
     * @param isCity      whether the chunk is city in its final plan (a city chunk never has a span)
     * @param orientation the road's axis: {@link Orientation#X} for an east-west primary
     * @return the span, or {@code null}
     */
    public PlannedBridgeInfo getBridgeInfo(Context context, int chunkX, int chunkZ, boolean isCity,
        Orientation orientation) {
        if (isCity || !isPrimaryForOrientation(chunkX, chunkZ, orientation)
            || !isGapChunk(context, chunkX, chunkZ, orientation)) {
            return null;
        }
        int[] minimum = findEndpoint(context, chunkX, chunkZ, orientation, -1);
        if (minimum == null) {
            return null;
        }
        int[] maximum = findEndpoint(context, chunkX, chunkZ, orientation, 1);
        if (maximum == null) {
            return null;
        }
        int gapLength = orientation.along(maximum[0], maximum[1]) - orientation.along(minimum[0], minimum[1]) - 1;
        if (gapLength < 1 || gapLength > maximumLength) {
            return null;
        }
        if (!context.isLevelZeroPrimaryRoad(minimum[0], minimum[1])
            || !context.isLevelZeroPrimaryRoad(maximum[0], maximum[1])) {
            return null;
        }
        long id = spanHash(orientation, minimum[0], minimum[1], maximum[0], maximum[1]);
        if (StableHash.unitDouble(id) >= bridgeChance) {
            return null;
        }
        return new PlannedBridgeInfo(id, orientation, minimum[0], minimum[1], maximum[0], maximum[1], gapLength);
    }

    /** Returns the orientation that may bridge a primary intersection in this world and dimension. */
    public Orientation preferredIntersectionOrientation() {
        return preferredIntersection;
    }

    private int[] findEndpoint(Context context, int startX, int startZ, Orientation orientation, int step) {
        int x = startX;
        int z = startZ;
        for (int distance = 1; distance <= maximumLength + 1; distance++) {
            if (orientation == Orientation.X) {
                x += step;
            } else {
                z += step;
            }
            if (context.isCityRaw(x, z) && streets.getRoadType(x, z) == PlannedRoadType.PRIMARY) {
                return new int[] { x, z };
            }
            if (distance > maximumLength || !isGapChunk(context, x, z, orientation)) {
                return null;
            }
        }
        return null;
    }

    private boolean isGapChunk(Context context, int chunkX, int chunkZ, Orientation orientation) {
        if (context.isCityRaw(chunkX, chunkZ) || !isPrimaryForOrientation(chunkX, chunkZ, orientation)) {
            return false;
        }
        // Deviation (0006 §1): a landmark core is never a gap chunk.
        if (context.isLandmarkCore(chunkX, chunkZ)) {
            return false;
        }
        boolean intersection = streets.isHorizontalPrimary(chunkZ) && streets.isVerticalPrimary(chunkX);
        if (intersection && orientation != preferredIntersection) {
            return false;
        }
        return context.isWater(chunkX, chunkZ);
    }

    private boolean isPrimaryForOrientation(int chunkX, int chunkZ, Orientation orientation) {
        return orientation == Orientation.X ? streets.isHorizontalPrimary(chunkZ) : streets.isVerticalPrimary(chunkX);
    }

    /**
     * Returns the world's preferred intersection orientation: {@link Orientation#X} when bit 0 of
     * {@code mix64(seed ^ dimensionHash ^ salt)} is 0.
     *
     * @param seed      the world seed
     * @param dimension the dimension
     * @return the preferred orientation
     */
    public static Orientation preferredIntersectionOrientation(long seed, DimensionKey dimension) {
        long value = seed ^ dimension.stableHash() ^ BRIDGE_INTERSECTION_SALT;
        return (StableHash.mix64(value) & 1L) == 0 ? Orientation.X : Orientation.Z;
    }

    /**
     * Upstream's span hash of two endpoints.
     *
     * @param orientation the span's axis
     * @param minX        lower endpoint chunk X
     * @param minZ        lower endpoint chunk Z
     * @param maxX        higher endpoint chunk X
     * @param maxZ        higher endpoint chunk Z
     * @return the hash; the span exists when its {@link StableHash#unitDouble(long)} is below the bridge chance
     */
    public long spanHash(Orientation orientation, int minX, int minZ, int maxX, int maxZ) {
        long value = seed ^ dimensionHash ^ BRIDGE_SPAN_SALT;
        value ^= StableHash.mix64((long) minX + MIN_X_OFFSET);
        value ^= StableHash.mix64((long) minZ + MIN_Z_OFFSET);
        value ^= StableHash.mix64((long) maxX + MAX_X_OFFSET);
        value ^= StableHash.mix64((long) maxZ + MAX_Z_OFFSET);
        value ^= orientation == Orientation.X ? X_ORIENTATION_SALT : Z_ORIENTATION_SALT;
        return StableHash.mix64(value);
    }
}
