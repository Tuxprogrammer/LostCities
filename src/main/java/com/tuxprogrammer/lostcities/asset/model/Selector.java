package com.tuxprogrammer.lostcities.asset.model;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/cityassets/CityStyle.java
// Avoids: ASSET-09, ASSET-10, CITY-31

import java.util.List;
import java.util.Random;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.match.WeightedPick;

/**
 * One entry of a city style's selector list: an asset name with a weight, optionally limited to a distance band from
 * the world origin with a linear fade of {@code feather} blocks outside it (docs/original-spec/05-assets/
 * styles-and-city-styles.md, "Selectors"; upstream's {@code ObjectSelector}).
 *
 * <p>
 * Changes from upstream: distances are computed in 64-bit arithmetic, so they never overflow far from the origin or
 * for a large {@code maxSpawnDistance} (fixes ASSET-09); with every weight zero nothing is picked (fixes ASSET-10).
 * The real distance fields are kept on the object, so any writer writes them (fixes CITY-31, which upstream also files
 * as ASSET-19). Immutable. Side: both.
 */
public final class Selector {

    /** Blocks per chunk, for turning a chunk coordinate into its corner block. */
    private static final int CHUNK_BLOCKS = 16;

    private final float factor;
    private final AssetId value;
    private final Object target;
    private final int minSpawnDistance;
    private final int maxSpawnDistance;
    private final int feather;

    Selector(float factor, AssetId value, Object target, int minSpawnDistance, int maxSpawnDistance, int feather) {
        this.factor = factor;
        this.value = value;
        this.target = target;
        this.minSpawnDistance = minSpawnDistance;
        this.maxSpawnDistance = maxSpawnDistance;
        this.feather = feather;
    }

    /** Returns the weight. */
    public float factor() {
        return factor;
    }

    /** Returns the selected asset's id. */
    public AssetId value() {
        return value;
    }

    /** Returns the selected asset: a {@link Building}, {@link MultiBuilding} or {@link Part}. */
    public Object target() {
        return target;
    }

    /** Returns the distance in blocks below which the weight is zero or fading; 0 for none. */
    public int minSpawnDistance() {
        return minSpawnDistance;
    }

    /** Returns the distance in blocks above which the weight is zero or fading; {@code Integer.MAX_VALUE} for none. */
    public int maxSpawnDistance() {
        return maxSpawnDistance;
    }

    /** Returns the fade width in blocks; 0 for a hard edge. */
    public int feather() {
        return feather;
    }

    /**
     * Returns the weight at a chunk: {@link #factor()}, faded outside the distance band, measured from the world
     * origin to the chunk's corner block as upstream does.
     *
     * @param chunkX the chunk X
     * @param chunkZ the chunk Z
     * @return the weight, 0 or more
     */
    public float weight(int chunkX, int chunkZ) {
        if (minSpawnDistance <= 0 && maxSpawnDistance == Integer.MAX_VALUE) {
            return factor;
        }
        long blockX = (long) chunkX * CHUNK_BLOCKS;
        long blockZ = (long) chunkZ * CHUNK_BLOCKS;
        long squared = blockX * blockX + blockZ * blockZ;
        long min = minSpawnDistance;
        long max = maxSpawnDistance;
        if (squared < min * min) {
            if (feather <= 0) {
                return 0f;
            }
            long fadeStart = min - feather;
            if (fadeStart > 0 && squared < fadeStart * fadeStart) {
                return 0f;
            }
            double fraction = (Math.sqrt(squared) - fadeStart) / (double) (min - fadeStart);
            return (float) (Math.max(0, fraction) * factor);
        }
        if (squared > max * max) {
            if (feather <= 0) {
                return 0f;
            }
            long fadeEnd = max + feather;
            if (squared > fadeEnd * fadeEnd) {
                return 0f;
            }
            double fraction = (fadeEnd - Math.sqrt(squared)) / (double) (fadeEnd - max);
            return (float) (Math.max(0, fraction) * factor);
        }
        return factor;
    }

    /**
     * Picks one selector of a list by its weight at a chunk.
     *
     * @param random the caller's random; one {@code nextFloat()} when any weight is positive
     * @param list   the selectors
     * @param chunkX the chunk X
     * @param chunkZ the chunk Z
     * @return the selector, or null when the list is empty or every weight is zero
     */
    public static Selector pick(Random random, List<Selector> list, int chunkX, int chunkZ) {
        return WeightedPick.pick(random, list, s -> s.weight(chunkX, chunkZ));
    }
}
