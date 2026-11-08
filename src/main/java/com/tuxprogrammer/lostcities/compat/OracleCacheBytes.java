package com.tuxprogrammer.lostcities.compat;

import com.tuxprogrammer.lostcities.compat.rwg2.Rwg2HeightOracle;
import com.tuxprogrammer.lostcities.compat.rwg2.Rwg2LandmarkOracle;
import com.tuxprogrammer.lostcities.compat.villagenames.VillageOracle;

/**
 * The most memory the structure oracles' caches can hold, estimated, for budget B5 (16 MB per world,
 * docs/design/0005-rwg-overlay.md §8). Every figure is a ceiling from the cache bounds, not a measurement.
 *
 * <p>
 * Everything here is a compile-time constant of the oracle classes, which javac inlines, so reading this class never
 * loads an oracle (and with it RWG2 or VillageNames) when its mod is absent; the sum counts every oracle whether or
 * not its mod is present, which only over-counts. Side: server.
 */
public final class OracleCacheBytes {

    /** One cached 4-long disc region: array (16 + 64), key, map entry and table slot. */
    static final long DISC_REGION_BYTES = 80 + 16 + 56 + 8;
    /** One cached 6-int junction region: array (16 + 24, padded to 40), key, map entry and table slot. */
    static final long JUNCTION_REGION_BYTES = 40 + 16 + 56 + 8;
    /** One cached landmark: the landmark with its footprint and parts, key, map entry and table slot. */
    static final long LANDMARK_BYTES = 300 + 16 + 56 + 8;
    /** One chain-grid flag: a shared Boolean, key, map entry and table slot. */
    static final long CHAIN_FLAG_BYTES = 16 + 56 + 8;
    /** One cached village prediction: the prediction, its landmark, key, map entry and table slot. */
    static final long PREDICTION_BYTES = 40 + 300 + 16 + 56 + 8;
    /** One cached village plan: a few dozen pieces with their boxes. */
    static final long VILLAGE_PLAN_BYTES = 8 * 1024;

    /** RWG2's landmark oracle: two region caches, three landmark caches and the chain grid. */
    public static final long RWG2_LANDMARKS = (long) Rwg2LandmarkOracle.REGION_CACHE_SIZE
        * (DISC_REGION_BYTES + JUNCTION_REGION_BYTES)
        + 3L * Rwg2LandmarkOracle.LANDMARK_CACHE_SIZE * LANDMARK_BYTES
        + 8L * Rwg2LandmarkOracle.LANDMARK_CACHE_SIZE * CHAIN_FLAG_BYTES;
    /** RWG2's height oracle. */
    public static final long RWG2_HEIGHTS = Rwg2HeightOracle.CACHE_BYTES;
    /** The village oracle's predictions and plans. */
    public static final long VILLAGES = (long) VillageOracle.CACHE_SIZE * PREDICTION_BYTES
        + (long) VillageOracle.PLAN_CACHE_SIZE * VILLAGE_PLAN_BYTES;
    /** All oracle caches. */
    public static final long TOTAL = RWG2_LANDMARKS + RWG2_HEIGHTS + VILLAGES;

    private OracleCacheBytes() {}
}
