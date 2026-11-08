package com.tuxprogrammer.lostcities.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.tuxprogrammer.lostcities.layout.landmark.HeightCap;
import com.tuxprogrammer.lostcities.layout.landmark.Landmark;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkKind;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkTreatment;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.layout.landmark.Zone;
import com.tuxprogrammer.lostcities.layout.landmark.ZoneInfo;
import com.tuxprogrammer.lostcities.layout.perf.PerfCounters;

/**
 * Derives each chunk's landmark zone from the structure oracle (0006 §1, landmark-zones.md § Zones): the core (chunks
 * the footprint grown by {@code landmarkMargin} touches), the edge ring (the core's eight-neighbors: the city's margin,
 * never city, for every kind of landmark), for villages the
 * {@code villageTransitionRings} transition rings beyond, and the lowest meteorite height cap. Under {@code AVOID} the
 * ring around a core is non-city instead (upstream's avoidance); under {@code IGNORE} the landmark has no effect.
 *
 * <p>
 * The oracle is asked once per 8 × 8-chunk cell, for the cell grown by the farthest reach any rule has, and the
 * landmarks are sorted by identity, so a chunk's zone never depends on which chunk asked first or on the oracle's
 * order. Both caches are bounded and belong to the planner.
 *
 * <p>
 * Link routing asks only whether a chunk is a core or the ring around one ({@link #blocksLinks}), along paths hundreds
 * of chunks long. It reads a separate per-cell bit mask whose oracle query grows the cell by {@link #linkMaskReach()}
 * chunks instead of {@link #reach()}, so a long route neither queries the oracles over the wide square nor evicts the
 * caches the planner needs (D-linkgraph-shrink-1).
 *
 * <p>
 * Spec: docs/new-spec/04-city-layout/landmark-zones.md, docs/design/0006-build-around-other-worldgen.md §1. Side:
 * server. Thread-safe: each cache is locked around each read and write, never while the oracle runs, and every value
 * is a pure function of its key, so a value two threads compute at once is the same either way; the first one stored
 * is the one both return.
 */
final class LandmarkZones {

    /** Chunks per side of an oracle query cell. */
    private static final int CELL = 8;
    /** Distance in chunks of the edge ring from its core. */
    static final int EDGE_RING_DISTANCE = 1;
    /** Margin, in chunks, beyond the rings for lanes that reach just past them. */
    private static final int LANE_MARGIN = 2;
    /**
     * Cells the link mask cache holds: 4096 cells of 8 × 8 chunks cover 262144 chunks, a 512-chunk square, about the
     * width of one link route, in about 70 KB; a miss costs one oracle query of about 12 × 12 chunks
     * (D-linkgraph-shrink-5, grown under D-perf-1).
     */
    static final int LINK_MASK_SLOTS = 4096;
    /** Fewest cells the near-landmark cache holds: 1024 cells of 8 × 8 chunks, 256 KB (D-perf-1). */
    static final int MIN_NEAR_CELLS = 1024;
    /** Estimated bytes of one {@link #near} cache entry: map entry, key, list and a landmark or two. */
    static final int CELL_ENTRY_BYTES = 256;

    private final StructureOracle oracle;
    private final LayoutProfile profile;
    private final int reach;
    private final int linkMaskReach;
    private final int cellCapacity;
    private final ChunkCache<List<Landmark>> cells;
    private final ChunkCache<ZoneInfo> zones;
    private final LinkMasks linkMasks = new LinkMasks(LINK_MASK_SLOTS);

    LandmarkZones(StructureOracle oracle, LayoutProfile profile, int cacheSize) {
        this.oracle = oracle;
        this.profile = profile;
        int capReach = HeightCap.AE2_REACH / 16 + 1;
        int ringReach = EDGE_RING_DISTANCE + profile.villageTransitionRings + LANE_MARGIN;
        int linkReach = profile.villageLinkDistance + EDGE_RING_DISTANCE + LANE_MARGIN;
        this.reach = Math.max(capReach, Math.max(ringReach, linkReach));
        // A landmark puts a chunk in its core or ring only when one of its footprint rectangles, grown by the margin in
        // blocks, lies within EDGE_RING_DISTANCE chunks of it; the plain rectangle then lies within that plus the
        // margin rounded up to whole chunks. Never wider than near()'s reach, so the mask sees near()'s candidates.
        this.linkMaskReach = Math.min(reach, EDGE_RING_DISTANCE + (profile.landmarkMargin + 15) / 16);
        this.cellCapacity = Math.max(MIN_NEAR_CELLS, cacheSize / (CELL * CELL));
        this.cells = new ChunkCache<>(cellCapacity);
        this.zones = new ChunkCache<>(cacheSize);
    }

    /** Returns how far, in chunks, a landmark's rules can reach beyond its core. */
    int reach() {
        return reach;
    }

    /** Returns how far, in chunks, the link mask's oracle query reaches beyond its cell ({@link #blocksLinks}). */
    int linkMaskReach() {
        return linkMaskReach;
    }

    /**
     * Returns the landmarks whose rules may reach a chunk, sorted by identity: every landmark whose footprint touches
     * the chunk's query cell grown by {@link #reach()}.
     */
    List<Landmark> near(int chunkX, int chunkZ) {
        int cellX = Math.floorDiv(chunkX, CELL);
        int cellZ = Math.floorDiv(chunkZ, CELL);
        List<Landmark> cached = cachedNear(cellX, cellZ);
        if (cached != null) {
            return cached;
        }
        List<Landmark> found = query(cellX, cellZ, reach);
        synchronized (cells) {
            List<Landmark> raced = cells.get(cellX, cellZ);
            return raced != null ? raced : cells.put(cellX, cellZ, found);
        }
    }

    private List<Landmark> cachedNear(int cellX, int cellZ) {
        synchronized (cells) {
            return cells.get(cellX, cellZ);
        }
    }

    /**
     * Returns the landmarks touching a query cell grown by some chunks: sorted by identity, the first of each identity
     * kept, unmodifiable.
     */
    private List<Landmark> query(int cellX, int cellZ, int grow) {
        int minX = cellX * CELL - grow;
        int minZ = cellZ * CELL - grow;
        int maxX = cellX * CELL + CELL - 1 + grow;
        int maxZ = cellZ * CELL + CELL - 1 + grow;
        long perfNear = PerfCounters.start();
        List<Landmark> found = new ArrayList<>(oracle.landmarksTouching(minX, minZ, maxX, maxZ));
        PerfCounters.add("lz.near.miss", perfNear);
        found.sort(Comparator.comparing(Landmark::id));
        List<Landmark> deduplicated = new ArrayList<>();
        for (Landmark landmark : found) {
            if (deduplicated.isEmpty() || !deduplicated.get(deduplicated.size() - 1)
                .id()
                .equals(landmark.id())) {
                deduplicated.add(landmark);
            }
        }
        return Collections.unmodifiableList(deduplicated);
    }

    /**
     * Returns whether a link may not cross a chunk: the chunk is a landmark's {@link Zone#CORE}, {@link Zone#EDGE_RING}
     * or {@link Zone#AVOID_RING} (0013 §1, 0015 §6). Always the answer {@link #zone} gives, since those are the three
     * strongest zones and a landmark not ignored gives one of them exactly when the chunk lies within
     * {@link #EDGE_RING_DISTANCE} of its grown footprint; but read from a per-cell bit mask whose oracle query grows
     * the
     * cell by {@link #linkMaskReach()} only, kept apart from the zone caches (D-linkgraph-shrink-1).
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return whether the chunk is a core or the ring around one
     */
    boolean blocksLinks(int chunkX, int chunkZ) {
        int cellX = Math.floorDiv(chunkX, CELL);
        int cellZ = Math.floorDiv(chunkZ, CELL);
        long key = ChunkCache.key(cellX, cellZ);
        long mask = 0L;
        boolean held;
        synchronized (linkMasks) {
            held = linkMasks.holds(key);
            if (held) {
                mask = linkMasks.mask(key);
            }
        }
        if (!held) {
            mask = computeLinkMask(cellX, cellZ);
            synchronized (linkMasks) {
                linkMasks.put(key, mask);
            }
        }
        int bit = (chunkX - cellX * CELL) * CELL + (chunkZ - cellZ * CELL);
        return (mask >>> bit & 1L) != 0;
    }

    private long computeLinkMask(int cellX, int cellZ) {
        // near()'s list when it is at hand: it holds every landmark the narrower query would, plus farther ones that
        // cannot set a bit.
        List<Landmark> landmarks = cachedNear(cellX, cellZ);
        if (landmarks == null) {
            landmarks = query(cellX, cellZ, linkMaskReach);
        }
        long mask = 0L;
        for (Landmark landmark : landmarks) {
            if (profile.treatment(landmark.kind()) == LandmarkTreatment.IGNORE) {
                continue;
            }
            for (int dx = 0; dx < CELL; dx++) {
                for (int dz = 0; dz < CELL; dz++) {
                    if (landmark.coreDistance(cellX * CELL + dx, cellZ * CELL + dz, profile.landmarkMargin)
                        <= EDGE_RING_DISTANCE) {
                        mask |= 1L << (dx * CELL + dz);
                    }
                }
            }
        }
        return mask;
    }

    /**
     * Returns the most memory the near-landmark cache and the link mask cache can hold, estimated, in bytes, for budget
     * B5. The zone cache is one of the layout caches {@code LayoutCityPlan} already counts.
     *
     * @return bytes
     */
    long estimatedCacheBytes() {
        return (long) cellCapacity * CELL_ENTRY_BYTES + LinkMasks.bytes(LINK_MASK_SLOTS);
    }

    /**
     * Returns the landmarks a highway must keep out of within a rectangle of chunks: every landmark not ignored whose
     * core or margin (the edge ring, or the ring of an avoided landmark) may touch it, sorted by identity (M06,
     * design record 0013).
     *
     * @param minX lowest chunk X
     * @param minZ lowest chunk Z
     * @param maxX highest chunk X
     * @param maxZ highest chunk Z
     * @return the landmarks
     */
    List<Landmark> obstaclesIn(int minX, int minZ, int maxX, int maxZ) {
        int grow = EDGE_RING_DISTANCE + 1;
        List<Landmark> found = new ArrayList<>(
            oracle.landmarksTouching(minX - grow, minZ - grow, maxX + grow, maxZ + grow));
        found.removeIf(landmark -> profile.treatment(landmark.kind()) == LandmarkTreatment.IGNORE);
        found.sort(Comparator.comparing(Landmark::id));
        return found;
    }

    /**
     * Returns whether a chunk lies in a landmark's core or the ring around it.
     *
     * @param landmark the landmark
     * @param chunkX   chunk X
     * @param chunkZ   chunk Z
     * @return whether the chunk is that close
     */
    boolean inCoreOrRing(Landmark landmark, int chunkX, int chunkZ) {
        return landmark.coreDistance(chunkX, chunkZ, profile.landmarkMargin) <= EDGE_RING_DISTANCE;
    }

    /**
     * Returns whether a chunk lies in the core of a landmark, not ignored, that reaches underground
     * ({@link LandmarkKind#reachesUnderground()}): no railway passes under it (D359).
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return whether it is such a core
     */
    boolean undergroundCore(int chunkX, int chunkZ) {
        for (Landmark landmark : near(chunkX, chunkZ)) {
            if (landmark.kind()
                .reachesUnderground() && profile.treatment(landmark.kind()) != LandmarkTreatment.IGNORE
                && landmark.coreDistance(chunkX, chunkZ, profile.landmarkMargin) == 0) {
                return true;
            }
        }
        return false;
    }

    /** Returns a chunk's zone. */
    ZoneInfo zone(int chunkX, int chunkZ) {
        synchronized (zones) {
            ZoneInfo cached = zones.get(chunkX, chunkZ);
            if (cached != null) {
                return cached;
            }
        }
        long perfZone = PerfCounters.start();
        ZoneInfo computed = computeZone(chunkX, chunkZ);
        PerfCounters.add("lz.zone.miss", perfZone);
        synchronized (zones) {
            ZoneInfo raced = zones.get(chunkX, chunkZ);
            return raced != null ? raced : zones.put(chunkX, chunkZ, computed);
        }
    }

    private ZoneInfo computeZone(int chunkX, int chunkZ) {
        Zone zone = Zone.NONE;
        String landmarkId = null;
        int cap = Integer.MAX_VALUE;
        for (Landmark landmark : near(chunkX, chunkZ)) {
            LandmarkTreatment treatment = profile.treatment(landmark.kind());
            if (treatment == LandmarkTreatment.IGNORE) {
                continue;
            }
            Zone here = zoneFor(landmark, treatment, landmark.coreDistance(chunkX, chunkZ, profile.landmarkMargin));
            if (here.ordinal() > zone.ordinal()) {
                zone = here;
                landmarkId = landmark.id();
            }
            if (treatment == LandmarkTreatment.BUILD_AROUND && landmark.heightCap() != null) {
                cap = Math.min(
                    cap,
                    landmark.heightCap()
                        .lowestCapOver(chunkX, chunkZ));
            }
        }
        if (zone == Zone.NONE && cap == Integer.MAX_VALUE) {
            return ZoneInfo.NONE;
        }
        return new ZoneInfo(zone, landmarkId, cap);
    }

    private Zone zoneFor(Landmark landmark, LandmarkTreatment treatment, int distance) {
        if (distance == 0) {
            return Zone.CORE;
        }
        if (distance == EDGE_RING_DISTANCE) {
            return treatment == LandmarkTreatment.AVOID ? Zone.AVOID_RING : Zone.EDGE_RING;
        }
        if (treatment == LandmarkTreatment.BUILD_AROUND && landmark.kind()
            .isSewn() && distance <= EDGE_RING_DISTANCE + profile.villageTransitionRings) {
            return distance == EDGE_RING_DISTANCE + 1 ? Zone.TRANSITION_1 : Zone.TRANSITION_2;
        }
        return Zone.NONE;
    }

    /**
     * Returns the floor cap a zone puts on a building, after upstream's draw: 2 in the first transition ring, 4 beyond
     * (0006 §1), or {@link Integer#MAX_VALUE}.
     */
    static int transitionFloorCap(Zone zone) {
        if (zone == Zone.TRANSITION_1) {
            return 2;
        }
        if (zone == Zone.TRANSITION_2) {
            return 4;
        }
        return Integer.MAX_VALUE;
    }

    /**
     * A direct-mapped cache of link masks, one {@code long} per 8 × 8-chunk cell (bit {@code dx * 8 + dz} set when that
     * chunk {@linkplain #blocksLinks blocks links}), in three primitive arrays. A slot holds the last cell hashed to
     * it;
     * a collision only costs a recomputation, never a different answer. Not thread-safe by itself: the owner locks it.
     */
    private static final class LinkMasks {

        private final long[] keys;
        private final long[] masks;
        private final boolean[] filled;

        LinkMasks(int slots) {
            keys = new long[slots];
            masks = new long[slots];
            filled = new boolean[slots];
        }

        /** Returns the most memory a cache of some slots holds, in bytes: the three arrays and their headers. */
        static long bytes(int slots) {
            return slots * (2L * Long.BYTES + 1) + 3 * 16;
        }

        private int slot(long key) {
            return (int) (ChunkKeys.of((int) (key >> 32), (int) key) >>> 40) & (keys.length - 1);
        }

        /** Returns whether a cell's mask is held. */
        boolean holds(long key) {
            int slot = slot(key);
            return filled[slot] && keys[slot] == key;
        }

        /** Returns a held cell's mask. */
        long mask(long key) {
            return masks[slot(key)];
        }

        /** Stores a cell's mask. */
        void put(long key, long mask) {
            int slot = slot(key);
            keys[slot] = key;
            masks[slot] = mask;
            filled[slot] = true;
        }
    }
}
