package com.tuxprogrammer.lostcities.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.tuxprogrammer.lostcities.layout.landmark.BlockRect;
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
 * The oracle is asked once per {@link #SUPER} × {@link #SUPER}-chunk super-cell, for the super-cell grown by the
 * farthest reach any rule has (D-perf2-route-1); each 8 × 8-chunk cell's list is the super-cell's landmarks whose
 * footprint touches that cell grown by the same reach, which is every landmark a query of the cell alone must return.
 * The landmarks are sorted by identity, so a chunk's zone never depends on which chunk asked first or on the oracle's
 * order. Every cache is bounded and belongs to the planner.
 *
 * <p>
 * Link routing asks only whether a chunk is a core or the ring around one ({@link #blocksLinks}), along paths hundreds
 * of chunks long. It reads a separate per-cell bit mask, derived from the same super-cell list, so a long route asks
 * the oracle once per super-cell it crosses, not once per cell, and never evicts the zone caches the planner needs
 * (D-linkgraph-shrink-1, D-perf2-route-1).
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
    /**
     * Chunks per side of a super-cell, the unit of every oracle query: 8 × 8 cells. One query of a 64-chunk square
     * grown by {@link #reach()} costs the oracles about what a few cells' queries did (each oracle pads its own
     * search), and serves 64 cells (D-perf2-route-1).
     */
    static final int SUPER = 64;
    /** Super-cells the landmark list cache holds: 256 cover a 1024-chunk square, wider than a link route's corridor. */
    static final int SUPER_SLOTS = 256;
    /**
     * Estimated bytes of one super-cell entry: map entry, key, list and bounds of a few dozen landmarks, the landmarks
     * themselves shared with the oracles' caches.
     */
    static final int SUPER_ENTRY_BYTES = 8 * 1024;

    private final StructureOracle oracle;
    private final LayoutProfile profile;
    private final int reach;
    private final int cellCapacity;
    private final ChunkCache<List<Landmark>> cells;
    private final ChunkCache<ZoneInfo> zones;
    private final ChunkCache<SuperCell> supers = new ChunkCache<>(SUPER_SLOTS);
    private final LinkMasks linkMasks = new LinkMasks(LINK_MASK_SLOTS);

    LandmarkZones(StructureOracle oracle, LayoutProfile profile, int cacheSize) {
        this.oracle = oracle;
        this.profile = profile;
        int capReach = HeightCap.AE2_REACH / 16 + 1;
        int ringReach = EDGE_RING_DISTANCE + profile.villageTransitionRings + LANE_MARGIN;
        int linkReach = profile.villageLinkDistance + EDGE_RING_DISTANCE + LANE_MARGIN;
        this.reach = Math.max(capReach, Math.max(ringReach, linkReach));
        this.cellCapacity = Math.max(MIN_NEAR_CELLS, cacheSize / (CELL * CELL));
        this.cells = new ChunkCache<>(cellCapacity);
        this.zones = new ChunkCache<>(cacheSize);
    }

    /** Returns how far, in chunks, a landmark's rules can reach beyond its core. */
    int reach() {
        return reach;
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
        List<Landmark> found = nearFromSuper(cellX, cellZ);
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
     * Returns the landmarks of a cell's super-cell whose footprint touches the cell grown by {@link #reach()}: sorted
     * by
     * identity, the first of each identity kept, unmodifiable. The super-cell's query covers that grown cell, so by the
     * oracle's completeness every landmark touching it is in the super-cell's list.
     */
    private List<Landmark> nearFromSuper(int cellX, int cellZ) {
        SuperCell sup = superCell(Math.floorDiv(cellX, SUPER / CELL), Math.floorDiv(cellZ, SUPER / CELL));
        List<Landmark> out = new ArrayList<>();
        for (Landmark landmark : sup.landmarks) {
            if (withinReach(landmark, cellX, cellZ)) {
                out.add(landmark);
            }
        }
        return out.isEmpty() ? Collections.<Landmark>emptyList() : Collections.unmodifiableList(out);
    }

    /** Returns whether a landmark's footprint touches a cell grown by {@link #reach()}: {@link #near}'s test. */
    private boolean withinReach(Landmark landmark, int cellX, int cellZ) {
        int minX = (cellX * CELL - reach) * 16;
        int minZ = (cellZ * CELL - reach) * 16;
        int maxX = (cellX * CELL + CELL - 1 + reach) * 16 + 15;
        int maxZ = (cellZ * CELL + CELL - 1 + reach) * 16 + 15;
        for (BlockRect rect : landmark.footprint()) {
            if (rect.minX() <= maxX && rect.maxX() >= minX && rect.minZ() <= maxZ && rect.maxZ() >= minZ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a super-cell's landmarks, querying the oracle once on a miss for the super-cell grown by
     * {@link #reach()}: the {@code lz.near.miss} counter counts these queries.
     */
    private SuperCell superCell(int superX, int superZ) {
        synchronized (supers) {
            SuperCell cached = supers.get(superX, superZ);
            if (cached != null) {
                return cached;
            }
        }
        int minX = superX * SUPER - reach;
        int minZ = superZ * SUPER - reach;
        int maxX = superX * SUPER + SUPER - 1 + reach;
        int maxZ = superZ * SUPER + SUPER - 1 + reach;
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
        int[][] bounds = new int[deduplicated.size()][];
        for (int i = 0; i < bounds.length; i++) {
            bounds[i] = deduplicated.get(i)
                .coreChunkBounds(profile.landmarkMargin);
        }
        SuperCell computed = new SuperCell(Collections.unmodifiableList(deduplicated), bounds);
        synchronized (supers) {
            SuperCell raced = supers.get(superX, superZ);
            return raced != null ? raced : supers.put(superX, superZ, computed);
        }
    }

    /**
     * Returns whether a link may not cross a chunk: the chunk is a landmark's {@link Zone#CORE}, {@link Zone#EDGE_RING}
     * or {@link Zone#AVOID_RING} (0013 §1, 0015 §6). Always the answer {@link #zone} gives, since those are the three
     * strongest zones and a landmark not ignored gives one of them exactly when the chunk lies within
     * {@link #EDGE_RING_DISTANCE} of its grown footprint; but read from a per-cell bit mask, derived from the cell's
     * super-cell list ({@link #SUPER}), kept apart from the zone caches (D-linkgraph-shrink-1, D-perf2-route-1).
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
        // The super-cell's list holds every landmark within reach() of the cell, a superset of what the old per-cell
        // query grown by the ring and margin saw. Only those near() would give the cell count, so the mask stays the
        // zone's answer at every margin; one whose core bounds, grown by the ring, miss the cell cannot set a bit, so
        // only the overlap is tested chunk by chunk.
        SuperCell sup = superCell(Math.floorDiv(cellX, SUPER / CELL), Math.floorDiv(cellZ, SUPER / CELL));
        List<Landmark> landmarks = sup.landmarks;
        int cellMinX = cellX * CELL;
        int cellMinZ = cellZ * CELL;
        long mask = 0L;
        for (int i = 0; i < landmarks.size(); i++) {
            Landmark landmark = landmarks.get(i);
            if (profile.treatment(landmark.kind()) == LandmarkTreatment.IGNORE) {
                continue;
            }
            int[] b = sup.bounds[i];
            if (b[0] - EDGE_RING_DISTANCE > cellMinX + CELL - 1 || b[2] + EDGE_RING_DISTANCE < cellMinX
                || b[1] - EDGE_RING_DISTANCE > cellMinZ + CELL - 1
                || b[3] + EDGE_RING_DISTANCE < cellMinZ
                || !withinReach(landmark, cellX, cellZ)) {
                continue;
            }
            int x0 = Math.max(cellMinX, b[0] - EDGE_RING_DISTANCE);
            int z0 = Math.max(cellMinZ, b[1] - EDGE_RING_DISTANCE);
            int x1 = Math.min(cellMinX + CELL - 1, b[2] + EDGE_RING_DISTANCE);
            int z1 = Math.min(cellMinZ + CELL - 1, b[3] + EDGE_RING_DISTANCE);
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    if (landmark.coreDistance(x, z, profile.landmarkMargin) <= EDGE_RING_DISTANCE) {
                        mask |= 1L << ((x - cellMinX) * CELL + (z - cellMinZ));
                    }
                }
            }
        }
        return mask;
    }

    /**
     * Returns the most memory the near-landmark, super-cell and link mask caches can hold, estimated, in bytes, for
     * budget
     * B5. The zone cache is one of the layout caches {@code LayoutCityPlan} already counts.
     *
     * @return bytes
     */
    long estimatedCacheBytes() {
        return (long) cellCapacity * CELL_ENTRY_BYTES + LinkMasks.bytes(LINK_MASK_SLOTS)
            + (long) SUPER_SLOTS * SUPER_ENTRY_BYTES;
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

    /** A super-cell's landmarks, sorted by identity, and each one's core chunk bounds {minX, minZ, maxX, maxZ}. */
    private static final class SuperCell {

        final List<Landmark> landmarks;
        final int[][] bounds;

        SuperCell(List<Landmark> landmarks, int[][] bounds) {
            this.landmarks = landmarks;
            this.bounds = bounds;
        }
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
