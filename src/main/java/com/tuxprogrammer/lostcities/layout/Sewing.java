package com.tuxprogrammer.lostcities.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.tuxprogrammer.lostcities.layout.landmark.Gate;
import com.tuxprogrammer.lostcities.layout.landmark.Landmark;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkTreatment;
import com.tuxprogrammer.lostcities.layout.landmark.LaneCell;
import com.tuxprogrammer.lostcities.layout.landmark.LaneRole;
import com.tuxprogrammer.lostcities.layout.street.HierarchicalStreetPlanner;
import com.tuxprogrammer.lostcities.layout.street.PlannedRoadType;
import com.tuxprogrammer.lostcities.layout.street.RoadDirection;

/**
 * Sews landmarks into the road network (0006 §3, landmark-zones.md § Roads around a core): primary roads that a core
 * severs detour around it, and village gates get lanes from the nearest road (in a city) or a surface path across the
 * land between (a village within {@code villageLinkDistance} of a city). Plazas, where a secondary or tertiary road
 * ends facing a core, are decided per chunk by the planner.
 *
 * <p>
 * The result for each landmark is a pure function of the landmark, raw city membership, the raw street field, the
 * landmark zones and predefined occupancy, all of which come before any chunk's characteristics; so lanes and detours
 * can block multibuildings without a cycle. Results are kept per landmark in a bounded cache.
 *
 * <p>
 * The geometry is settled in decision log D50, with the margin of the alpha review (AR-10): the core's eight-neighbors
 * are the city's margin, never city, so a detour runs through the ring of chunks just outside the margin's bounding
 * chunk rectangle, on the side with the shorter run (ties west for north-south roads, north for east-west ones). A
 * lane starts in the gate's margin chunk, whose path runs down the margin's slope, and goes on from the city chunk
 * beyond it, straight or L-shaped, to the nearest road chunk, bending at the corner nearer the city's center; a
 * secondary or tertiary road is preferred to a primary one up to {@link #PRIMARY_PENALTY} chunks farther, so a village
 * is sewn in with small roads (AR-7).
 *
 * <p>
 * Spec: docs/design/0006-build-around-other-worldgen.md §3, docs/new-spec/00-pack-integration/coexistence.md § Sewing
 * a village into a city. Side: server.
 */
final class Sewing {

    /** How far a gate's chunk may be stepped outward from the gate before it leaves the core. */
    private static final int MAX_GATE_STEPS = 8;
    /** Extra chunks a lane will run to reach a secondary or tertiary road rather than a primary one (AR-7). */
    static final int PRIMARY_PENALTY = 2;

    private final LayoutProfile profile;
    private final CityMap cities;
    private final LandmarkZones zones;
    private final HierarchicalStreetPlanner streets;
    private final ActivePredefined predefined;
    private final Map<String, Map<Long, LaneCell>> sewn;

    Sewing(LayoutProfile profile, CityMap cities, LandmarkZones zones, HierarchicalStreetPlanner streets,
        ActivePredefined predefined, int cacheSize) {
        this.profile = profile;
        this.cities = cities;
        this.zones = zones;
        this.streets = streets;
        this.predefined = predefined;
        int capacity = Math.max(64, cacheSize / 64);
        this.sewn = new LinkedHashMap<String, Map<Long, LaneCell>>(64, 0.75f, true) {

            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Map<Long, LaneCell>> eldest) {
                return size() > capacity;
            }
        };
    }

    /** Returns a chunk's sewing role: the strongest any nearby landmark gives it. */
    LaneCell cell(int chunkX, int chunkZ) {
        LaneCell result = LaneCell.NONE;
        long key = ChunkCache.key(chunkX, chunkZ);
        for (Landmark landmark : zones.near(chunkX, chunkZ)) {
            LaneCell here = sewing(landmark).get(key);
            if (here != null) {
                result = merge(result, here);
            }
        }
        return result;
    }

    private Map<Long, LaneCell> sewing(Landmark landmark) {
        Map<Long, LaneCell> cached = sewn.get(landmark.id());
        if (cached != null) {
            return cached;
        }
        Map<Long, LaneCell> cells = new HashMap<>();
        if (profile.treatment(landmark.kind()) == LandmarkTreatment.BUILD_AROUND) {
            detours(landmark, cells);
            if (landmark.kind()
                .isSewn()) {
                lanes(landmark, cells);
            }
        }
        Map<Long, LaneCell> result = cells.isEmpty() ? Collections.emptyMap() : cells;
        sewn.put(landmark.id(), result);
        return result;
    }

    /**
     * Two roles on one chunk: the stronger role wins, but the connections of both are kept, and so is a gate, so a gate
     * whose chunk a detour also runs through still leads its path into the core.
     */
    private static LaneCell merge(LaneCell a, LaneCell b) {
        LaneCell strong = LaneRole.strongest(a.role(), b.role()) == a.role() ? a : b;
        LaneCell weak = strong == a ? b : a;
        return new LaneCell(
            strong.role(),
            strong.connectionBits() | weak.connectionBits(),
            strong.gate() != null ? strong.gate() : weak.gate(),
            strong.landmarkId() != null ? strong.landmarkId() : weak.landmarkId());
    }

    // ------------------------------------------------------------------ predicates

    /** Whether a lane may not cross a chunk: a core or an avoid ring of any landmark. */
    private boolean barred(int chunkX, int chunkZ) {
        return zones.zone(chunkX, chunkZ)
            .zone()
            .barsLanes();
    }

    /** Whether a chunk will be city: raw city and not a core, margin or avoid ring. */
    private boolean city(int chunkX, int chunkZ) {
        return cities.isCityRaw(chunkX, chunkZ) && !zones.zone(chunkX, chunkZ)
            .zone()
            .forcesNonCity();
    }

    // ------------------------------------------------------------------ detours

    private void detours(Landmark landmark, Map<Long, LaneCell> cells) {
        // Around the margin: the core's bounds grown by the margin's chunk, so the detour stays out of the margin.
        int[] b = landmark.coreChunkBounds(profile.landmarkMargin);
        int ring = LandmarkZones.EDGE_RING_DISTANCE;
        int x0 = b[0] - ring;
        int z0 = b[1] - ring;
        int x1 = b[2] + ring;
        int z1 = b[3] + ring;
        for (int c = x0; c <= x1; c++) {
            if (streets.isVerticalPrimary(c) && city(c, z0 - 1) && city(c, z1 + 1)) {
                boolean westFirst = c - (x0 - 1) <= (x1 + 1) - c;
                List<int[]> path = verticalDetour(c, z0, z1, westFirst ? x0 - 1 : x1 + 1);
                if (!allCity(path)) {
                    path = verticalDetour(c, z0, z1, westFirst ? x1 + 1 : x0 - 1);
                }
                if (allCity(path)) {
                    mark(cells, path, LaneRole.DETOUR, null, landmark.id(), RoadDirection.NORTH, RoadDirection.SOUTH);
                }
            }
        }
        for (int r = z0; r <= z1; r++) {
            if (streets.isHorizontalPrimary(r) && city(x0 - 1, r) && city(x1 + 1, r)) {
                boolean northFirst = r - (z0 - 1) <= (z1 + 1) - r;
                List<int[]> path = horizontalDetour(r, x0, x1, northFirst ? z0 - 1 : z1 + 1);
                if (!allCity(path)) {
                    path = horizontalDetour(r, x0, x1, northFirst ? z1 + 1 : z0 - 1);
                }
                if (allCity(path)) {
                    mark(cells, path, LaneRole.DETOUR, null, landmark.id(), RoadDirection.WEST, RoadDirection.EAST);
                }
            }
        }
    }

    /** The detour of the north-south primary at chunk X {@code c}, from its north end to its south end. */
    private static List<int[]> verticalDetour(int c, int z0, int z1, int sideX) {
        List<int[]> path = new ArrayList<>();
        int step = sideX < c ? -1 : 1;
        for (int x = c; x != sideX; x += step) {
            path.add(new int[] { x, z0 - 1 });
        }
        for (int z = z0 - 1; z <= z1 + 1; z++) {
            path.add(new int[] { sideX, z });
        }
        for (int x = sideX - step; x != c - step; x -= step) {
            path.add(new int[] { x, z1 + 1 });
        }
        return path;
    }

    /** The detour of the east-west primary at chunk Z {@code r}, from its west end to its east end. */
    private static List<int[]> horizontalDetour(int r, int x0, int x1, int sideZ) {
        List<int[]> path = new ArrayList<>();
        int step = sideZ < r ? -1 : 1;
        for (int z = r; z != sideZ; z += step) {
            path.add(new int[] { x0 - 1, z });
        }
        for (int x = x0 - 1; x <= x1 + 1; x++) {
            path.add(new int[] { x, sideZ });
        }
        for (int z = sideZ - step; z != r - step; z -= step) {
            path.add(new int[] { x1 + 1, z });
        }
        return path;
    }

    private boolean allCity(List<int[]> path) {
        for (int[] c : path) {
            if (!city(c[0], c[1])) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ lanes

    private void lanes(Landmark landmark, Map<Long, LaneCell> cells) {
        int[] bounds = landmark.coreChunkBounds(profile.landmarkMargin);
        int midX = (bounds[0] + bounds[2]) >> 1;
        int midZ = (bounds[1] + bounds[3]) >> 1;
        int[] center = cities.strongestCenter(midX, midZ);
        long refX = center == null ? midX * 16L + 8 : center[0] * 16L + 8;
        long refZ = center == null ? midZ * 16L + 8 : center[1] * 16L + 8;
        for (RoadDirection side : RoadDirection.values()) {
            Gate gate = gateFor(landmark, side, refX, refZ);
            if (gate != null) {
                lane(landmark, gate, cells, refX, refZ);
            }
        }
    }

    /** The gate on one side of the core nearest the city's center; ties go north, then west. */
    private static Gate gateFor(Landmark landmark, RoadDirection side, long refX, long refZ) {
        Gate best = null;
        long bestDistance = Long.MAX_VALUE;
        for (Gate gate : landmark.gates()) {
            if (gate.facing() != side) {
                continue;
            }
            long dx = gate.x() - refX;
            long dz = gate.z() - refZ;
            long distance = dx * dx + dz * dz;
            if (best == null || distance < bestDistance
                || distance == bestDistance && (gate.z() < best.z() || gate.z() == best.z() && gate.x() < best.x())) {
                best = gate;
                bestDistance = distance;
            }
        }
        return best;
    }

    private void lane(Landmark landmark, Gate gate, Map<Long, LaneCell> cells, long refX, long refZ) {
        int gx = gate.chunkX();
        int gz = gate.chunkZ();
        RoadDirection out = gate.facing();
        int steps = 0;
        while (landmark.coreDistance(gx, gz, profile.landmarkMargin) == 0) {
            if (++steps > MAX_GATE_STEPS) {
                return;
            }
            gx += out.stepX();
            gz += out.stepZ();
        }
        if (barred(gx, gz)) {
            return;
        }
        // The gate chunk is the margin chunk in front of the gate; its path runs down the margin's slope (AR-7). In a
        // city the lane goes on from the city chunk beyond it, so it crosses the margin straight.
        int sx = gx + out.stepX();
        int sz = gz + out.stepZ();
        boolean inCity = city(sx, sz);
        int maxCoreDistance = LandmarkZones.EDGE_RING_DISTANCE + profile.villageTransitionRings;
        if (!inCity && profile.villageLinkDistance <= 0) {
            return;
        }
        List<int[]> path;
        if (inCity) {
            List<int[]> rest = streets.getRoadType(sx, sz)
                .isRoad() ? new ArrayList<>(Collections.singletonList(new int[] { sx, sz }))
                    : route(landmark, sx, sz, maxCoreDistance + 1, true, maxCoreDistance, refX, refZ);
            if (rest == null) {
                return;
            }
            path = new ArrayList<>();
            path.add(new int[] { gx, gz });
            path.addAll(rest);
        } else {
            path = route(landmark, gx, gz, profile.villageLinkDistance + 1, false, maxCoreDistance, refX, refZ);
            if (path == null) {
                return;
            }
        }
        // The road chunk the lane reaches keeps its own road; the lane's last chunk connects to it.
        int[] target = path.size() > 1 ? path.remove(path.size() - 1) : null;
        int[] first = path.get(0);
        LaneCell gateCell = new LaneCell(LaneRole.GATE, 0, gate, landmark.id()).with(out.opposite());
        if (path.size() > 1) {
            gateCell = gateCell.with(direction(first, path.get(1)));
        } else if (target != null) {
            gateCell = gateCell.with(direction(first, target));
        }
        put(cells, first[0], first[1], gateCell);
        for (int i = 1; i < path.size(); i++) {
            int[] c = path.get(i);
            LaneRole role = city(c[0], c[1]) ? LaneRole.LANE : LaneRole.LINK;
            LaneCell cell = new LaneCell(role, 0, null, landmark.id()).with(direction(c, path.get(i - 1)));
            if (i + 1 < path.size()) {
                cell = cell.with(direction(c, path.get(i + 1)));
            } else if (target != null) {
                cell = cell.with(direction(c, target));
            }
            put(cells, c[0], c[1], cell);
        }
    }

    /**
     * Finds the lane from a gate chunk: the nearest road chunk (Manhattan distance, straight before bent, then smaller
     * Z, then smaller X) reachable by a straight or L-shaped run whose chunks may carry a lane. Returns the run
     * from the gate chunk to the road chunk, the road chunk last, or {@code null}.
     */
    private List<int[]> route(Landmark landmark, int gx, int gz, int reach, boolean inCity, int maxCoreDistance,
        long refX, long refZ) {
        List<int[]> targets = new ArrayList<>();
        for (int dz = -reach; dz <= reach; dz++) {
            for (int dx = -reach; dx <= reach; dx++) {
                int tx = gx + dx;
                int tz = gz + dz;
                if ((dx != 0 || dz != 0) && city(tx, tz)
                    && streets.getRoadType(tx, tz)
                        .isRoad()
                    && landmark.coreDistance(tx, tz, profile.landmarkMargin) > 0) {
                    targets.add(new int[] { tx, tz });
                }
            }
        }
        targets.sort((a, b) -> {
            int c = Integer.compare(cost(a, gx, gz), cost(b, gx, gz));
            if (c != 0) {
                return c;
            }
            c = Boolean.compare(!straight(a, gx, gz), !straight(b, gx, gz));
            if (c != 0) {
                return c;
            }
            c = Integer.compare(a[1], b[1]);
            return c != 0 ? c : Integer.compare(a[0], b[0]);
        });
        for (int[] target : targets) {
            int[] near = new int[] { target[0], gz };
            int[] far = new int[] { gx, target[1] };
            boolean nearFirst = squaredToReference(near, refX, refZ) <= squaredToReference(far, refX, refZ);
            List<int[]> path = run(gx, gz, target, nearFirst ? near : far);
            if (!laneAllowed(landmark, path, inCity, maxCoreDistance)) {
                path = run(gx, gz, target, nearFirst ? far : near);
                if (!laneAllowed(landmark, path, inCity, maxCoreDistance)) {
                    continue;
                }
            }
            return path;
        }
        return null;
    }

    private static int manhattan(int[] c, int gx, int gz) {
        return Math.abs(c[0] - gx) + Math.abs(c[1] - gz);
    }

    /** A target road's cost: its distance, and {@link #PRIMARY_PENALTY} more for a primary road (AR-7). */
    private int cost(int[] c, int gx, int gz) {
        return manhattan(c, gx, gz)
            + (streets.getRoadType(c[0], c[1]) == PlannedRoadType.PRIMARY ? PRIMARY_PENALTY : 0);
    }

    private static boolean straight(int[] c, int gx, int gz) {
        return c[0] == gx || c[1] == gz;
    }

    private static long squaredToReference(int[] chunk, long refX, long refZ) {
        long dx = chunk[0] * 16L + 8 - refX;
        long dz = chunk[1] * 16L + 8 - refZ;
        return dx * dx + dz * dz;
    }

    /** The chunks from the gate chunk to the corner and on to the target, the target last. */
    private static List<int[]> run(int gx, int gz, int[] target, int[] corner) {
        List<int[]> path = new ArrayList<>();
        int x = gx;
        int z = gz;
        path.add(new int[] { x, z });
        while (x != corner[0] || z != corner[1]) {
            x += Integer.signum(corner[0] - x);
            z += Integer.signum(corner[1] - z);
            path.add(new int[] { x, z });
        }
        while (x != target[0] || z != target[1]) {
            x += Integer.signum(target[0] - x);
            z += Integer.signum(target[1] - z);
            path.add(new int[] { x, z });
        }
        return path;
    }

    /**
     * Whether every chunk of a run may carry the lane: never a core or avoid ring, never predefined content; in a
     * city, only transition chunks of this landmark that are city (the margin is behind the run's start).
     */
    private boolean laneAllowed(Landmark landmark, List<int[]> path, boolean inCity, int maxCoreDistance) {
        for (int[] c : path.subList(0, path.size() - 1)) {
            if (barred(c[0], c[1]) || predefined.occupant(c[0], c[1]) != null) {
                return false;
            }
            if (inCity) {
                int distance = landmark.coreDistance(c[0], c[1], profile.landmarkMargin);
                if (!city(c[0], c[1]) || distance <= LandmarkZones.EDGE_RING_DISTANCE || distance > maxCoreDistance) {
                    return false;
                }
            }
        }
        return true;
    }

    private static RoadDirection direction(int[] from, int[] to) {
        if (to[0] < from[0]) {
            return RoadDirection.WEST;
        }
        if (to[0] > from[0]) {
            return RoadDirection.EAST;
        }
        return to[1] < from[1] ? RoadDirection.NORTH : RoadDirection.SOUTH;
    }

    private static void mark(Map<Long, LaneCell> cells, List<int[]> path, LaneRole role, Gate gate, String landmarkId,
        RoadDirection startOutward, RoadDirection endOutward) {
        for (int i = 0; i < path.size(); i++) {
            int[] c = path.get(i);
            LaneCell cell = new LaneCell(role, 0, gate, landmarkId);
            if (i == 0) {
                cell = cell.with(startOutward);
            }
            if (i == path.size() - 1) {
                cell = cell.with(endOutward);
            }
            if (i > 0) {
                cell = cell.with(direction(c, path.get(i - 1)));
            }
            if (i + 1 < path.size()) {
                cell = cell.with(direction(c, path.get(i + 1)));
            }
            put(cells, c[0], c[1], cell);
        }
    }

    private static void put(Map<Long, LaneCell> cells, int chunkX, int chunkZ, LaneCell cell) {
        long key = ChunkCache.key(chunkX, chunkZ);
        LaneCell existing = cells.get(key);
        cells.put(key, existing == null ? cell : merge(existing, cell));
    }
}
