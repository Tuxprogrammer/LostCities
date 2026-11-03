package com.tuxprogrammer.lostcities.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.tuxprogrammer.lostcities.layout.landmark.HeightCap;
import com.tuxprogrammer.lostcities.layout.landmark.Landmark;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkTreatment;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.layout.landmark.Zone;
import com.tuxprogrammer.lostcities.layout.landmark.ZoneInfo;

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
 * Spec: docs/new-spec/04-city-layout/landmark-zones.md, docs/design/0006-build-around-other-worldgen.md §1. Side:
 * server.
 */
final class LandmarkZones {

    /** Chunks per side of an oracle query cell. */
    private static final int CELL = 8;
    /** Distance in chunks of the edge ring from its core. */
    static final int EDGE_RING_DISTANCE = 1;
    /** Margin, in chunks, beyond the rings for lanes that reach just past them. */
    private static final int LANE_MARGIN = 2;

    private final StructureOracle oracle;
    private final LayoutProfile profile;
    private final int reach;
    private final ChunkCache<List<Landmark>> cells;
    private final ChunkCache<ZoneInfo> zones;

    LandmarkZones(StructureOracle oracle, LayoutProfile profile, int cacheSize) {
        this.oracle = oracle;
        this.profile = profile;
        int capReach = HeightCap.AE2_REACH / 16 + 1;
        int ringReach = EDGE_RING_DISTANCE + profile.villageTransitionRings + LANE_MARGIN;
        int linkReach = profile.villageLinkDistance + EDGE_RING_DISTANCE + LANE_MARGIN;
        this.reach = Math.max(capReach, Math.max(ringReach, linkReach));
        this.cells = new ChunkCache<>(Math.max(64, cacheSize / (CELL * CELL)));
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
        List<Landmark> cached = cells.get(cellX, cellZ);
        if (cached != null) {
            return cached;
        }
        int minX = cellX * CELL - reach;
        int minZ = cellZ * CELL - reach;
        int maxX = cellX * CELL + CELL - 1 + reach;
        int maxZ = cellZ * CELL + CELL - 1 + reach;
        List<Landmark> found = new ArrayList<>(oracle.landmarksTouching(minX, minZ, maxX, maxZ));
        found.sort(Comparator.comparing(Landmark::id));
        List<Landmark> deduplicated = new ArrayList<>();
        for (Landmark landmark : found) {
            if (deduplicated.isEmpty() || !deduplicated.get(deduplicated.size() - 1)
                .id()
                .equals(landmark.id())) {
                deduplicated.add(landmark);
            }
        }
        return cells.put(cellX, cellZ, Collections.unmodifiableList(deduplicated));
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

    /** Returns a chunk's zone. */
    ZoneInfo zone(int chunkX, int chunkZ) {
        ZoneInfo cached = zones.get(chunkX, chunkZ);
        if (cached != null) {
            return cached;
        }
        return zones.put(chunkX, chunkZ, computeZone(chunkX, chunkZ));
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
}
