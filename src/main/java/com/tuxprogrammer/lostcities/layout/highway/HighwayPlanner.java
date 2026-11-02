package com.tuxprogrammer.lostcities.layout.highway;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/highway/IntercityHighwayPlanner.java
// Avoids: HWY-06, HWY-09, HWY-10, HWY-11, HWY-14, HWY-15

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.tuxprogrammer.lostcities.layout.random.StableHash;

/**
 * Upstream's {@code INTERCITY_NETWORK_V1} planner, pure and synchronous: planning cells own at most one hub; each hub
 * picks bounded, sector-diverse neighbors; a connection exists where two hubs pick each other; the smaller cell owns
 * the route; a chunk asks the routes owned by the cells around it. Every choice is a salted SplitMix64 hash of the
 * world seed, the dimension and the coordinates; there is no {@code Random}, and no answer depends on what has been
 * generated or asked before.
 *
 * <p>
 * Changes from upstream (new-spec 06, design record 0007):
 * <ul>
 * <li>Landmark obstacles (D31 P06-1, P06-2, alpha review AR-10): a hub is never an obstacle chunk (a landmark's core
 * or its margin); a route prefers a shape that crosses none, detours around one, or ends a chunk before it.</li>
 * <li>Deliberate ends (AR-10): each run reaches its ends at their own level by ramps ({@link Run}), and a run's end
 * chunk becomes a plaza ({@link HighwayInfo}).</li>
 * <li>Avoids HWY-09: a route that shares a chunk along its axis with another takes the lowest level among them along
 * its whole length, so neither breaks off where they meet.</li>
 * <li>Avoids HWY-10 and HWY-15: every cache is bounded, least recently used in fact, and owned by this planner.</li>
 * <li>Avoids HWY-11 and HWY-14: no hub is persisted and no thread pool exists; the planner runs on the caller's
 * thread, and a hub cell's computation can be scheduled through a {@link PlanningGate} instead.</li>
 * <li>Avoids HWY-06: highways switched off ({@link HighwaySettings#enabled()}) give no highway anywhere.</li>
 * </ul>
 *
 * <p>
 * Spec: docs/original-spec/06-highways-railways/intercity-planner.md, docs/new-spec/06-highways-railways/index.md.
 * Not thread-safe; one per world. Side: both.
 */
public final class HighwayPlanner {

    /** "INTERV1" and a version byte, mixed into every hash. */
    static final long VERSION_SALT = 0x494e544552563101L;
    /** Tie-break among hub samples. */
    static final long HUB_SAMPLE_SALT = 0x5d39a74e10c26bf1L;
    /** Sample-lattice offset per cell. */
    static final long HUB_POSITION_SALT = 0x27f4b8916d3a50ceL;
    /** Per-cell input to the sample tie-break. */
    static final long HUB_STRENGTH_SALT = 0x618ad2e5073fb94cL;
    /** Candidate rank tie-break. */
    static final long CONNECTION_RANK_SALT = 0x34ce791b582da6f0L;
    /** Second candidate tie-break. */
    static final long CONNECTION_ACCEPT_SALT = 0x72b514e90c63dfa8L;
    /** Last-resort L-shape choice. */
    static final long ROUTE_SHAPE_SALT = 0x19e8c347a65d2bf0L;
    /** Penalty sample step along a route, in chunks. */
    static final int ROUTE_PENALTY_SAMPLE_SPACING = 8;
    /** Samples this close to either hub (Manhattan, chunks) are not penalized. */
    static final int ENDPOINT_PENALTY_DISCOUNT = 16;

    /** Most sideways offset a detour may take, in chunks (P06-1). */
    public static final int MAX_DETOUR_OFFSET = 8;
    /**
     * Half-width, in chunks, of the window an obstacle's extent is read in; an obstacle reaching its edge is too large
     * to go around, and the highway ends before it.
     */
    static final int OBSTACLE_WINDOW = 16;

    /** Upstream's cache sizes, in entries. */
    static final int HUB_CACHE = 4096;
    static final int CANDIDATE_CACHE = 2048;
    static final int SELECTION_CACHE = 2048;
    static final int ROUTE_CACHE = 2048;
    static final int CHUNK_CACHE = 8192;

    private static final Hub NO_HUB = new Hub(new HubKey(0, 0), 0, 0, -1, 0);

    private final long seed;
    private final long dimensionSalt;
    private final HighwaySettings settings;
    private final CityPotential potential;
    private final HighwayTerrain terrain;
    private final HighwayObstacles obstacles;
    private final PlanningGate gate;
    private final LruCache<HubKey, Hub> hubs = new LruCache<>(HUB_CACHE);
    private final LruCache<HubKey, List<Candidate>> candidates = new LruCache<>(CANDIDATE_CACHE);
    private final LruCache<HubKey, List<HubKey>> selections = new LruCache<>(SELECTION_CACHE);
    private final LruCache<HubKey, List<Route>> routes = new LruCache<>(ROUTE_CACHE);
    private final LruCache<ConnectionKey, Integer> levels = new LruCache<>(ROUTE_CACHE);
    private final LruCache<Long, HighwayInfo> chunks = new LruCache<>(CHUNK_CACHE);

    /**
     * Builds the planner of one world.
     *
     * @param seed          the world seed
     * @param dimensionSalt the dimension's stable hash ({@code DimensionKey.stableHash()})
     * @param settings      the settings
     * @param potential     the city potential
     * @param terrain       the city levels
     * @param obstacles     the landmark obstacles
     * @param gate          decides when a missing hub cell is computed
     */
    public HighwayPlanner(long seed, long dimensionSalt, HighwaySettings settings, CityPotential potential,
        HighwayTerrain terrain, HighwayObstacles obstacles, PlanningGate gate) {
        this.seed = seed;
        this.dimensionSalt = dimensionSalt;
        this.settings = settings;
        this.potential = potential;
        this.terrain = terrain;
        this.obstacles = obstacles;
        this.gate = gate;
    }

    /** Returns the settings. */
    public HighwaySettings settings() {
        return settings;
    }

    /**
     * Returns the planning cell of a chunk.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the cell
     */
    public HubKey cell(int chunkX, int chunkZ) {
        return HubKey.of(chunkX, chunkZ, settings.planningCellSize());
    }

    // ================================================================== H: hubs

    /**
     * Returns whether a cell's hub is computed already.
     *
     * @param key the cell
     * @return whether it is cached
     */
    public boolean hubKnown(HubKey key) {
        return hubs.holds(key);
    }

    /**
     * Returns a cell's hub.
     *
     * @param key the cell
     * @return the hub, or {@code null} when the cell has none
     */
    public Hub hub(HubKey key) {
        Hub cached = hubs.get(key);
        if (cached == null) {
            gate.enter(() -> computeHub(key));
            cached = computeHub(key);
        }
        return cached == NO_HUB ? null : cached;
    }

    private Hub computeHub(HubKey key) {
        Hub cached = hubs.get(key);
        if (cached != null) {
            return cached;
        }
        Hub hub = calculateHub(key);
        return hubs.put(key, hub == null ? NO_HUB : hub);
    }

    private Hub calculateHub(HubKey cell) {
        int size = settings.planningCellSize();
        int spacing = settings.hubSampleSpacing();
        int minimum = Math.round(settings.hubMinimumPotential() * CityPotential.SCORE_SCALE);
        int startX = Math.toIntExact((long) cell.cellX() * size);
        int startZ = Math.toIntExact((long) cell.cellZ() * size);
        int offsetX = StableHash.floorModHash(hash(HUB_POSITION_SALT, cell.cellX(), cell.cellZ(), 0), spacing);
        int offsetZ = StableHash.floorModHash(hash(HUB_POSITION_SALT, cell.cellX(), cell.cellZ(), 1), spacing);
        int strength = StableHash
            .floorModHash(hash(HUB_STRENGTH_SALT, cell.cellX(), cell.cellZ(), 0), Integer.MAX_VALUE);
        int bestX = 0;
        int bestZ = 0;
        int bestScore = -1;
        long bestTie = 0L;
        for (int localX = offsetX; localX < size; localX += spacing) {
            for (int localZ = offsetZ; localZ < size; localZ += spacing) {
                int chunkX = moveXOffRailwayCorridor(Math.addExact(startX, localX), localX, size);
                int chunkZ = moveZOffRailwayCorridor(Math.addExact(startZ, localZ), localZ, size);
                long tie = hash(HUB_SAMPLE_SALT, chunkX, chunkZ, strength);
                boolean qualifyingBest = bestScore >= minimum;
                int required = qualifyingBest ? bestScore + (Long.compareUnsigned(tie, bestTie) < 0 ? 0 : 1) : minimum;
                int score = potential.scoreWithUpperBound(chunkX, chunkZ, required);
                if (score < 0) {
                    continue;
                }
                boolean better = bestScore < 0 || score > bestScore
                    || score == bestScore && Long.compareUnsigned(tie, bestTie) < 0;
                // Deviation (P06-2, AR-10): a sample in a landmark's core or margin is skipped, as one below the
                // minimum is, so a route never starts or ends in one.
                if (better && !obstacles.blocksHub(chunkX, chunkZ)) {
                    bestX = chunkX;
                    bestZ = chunkZ;
                    bestScore = score;
                    bestTie = tie;
                }
            }
        }
        if (bestScore < minimum) {
            return null;
        }
        return new Hub(cell, bestX, bestZ, bestScore, terrain.cityLevel(bestX, bestZ));
    }

    // ================================================================== C, S, A: connections

    /**
     * Returns a hub's connection candidates, sorted as upstream sorts them.
     *
     * @param source the hub's cell
     * @return the candidates; empty when the cell has no hub
     */
    public List<Candidate> candidates(HubKey source) {
        List<Candidate> cached = candidates.get(source);
        if (cached != null) {
            return cached;
        }
        return candidates.put(source, calculateCandidates(source));
    }

    private List<Candidate> calculateCandidates(HubKey sourceKey) {
        Hub source = hub(sourceKey);
        if (source == null) {
            return Collections.emptyList();
        }
        int radius = settings.hubSearchRadiusCells();
        long minimumSquared = square(settings.minimumHubDistance());
        long maximumSquared = square(settings.maximumHubDistance());
        List<Candidate> out = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                HubKey targetKey = sourceKey.offset(dx, dz);
                if (targetKey.equals(sourceKey)) {
                    continue;
                }
                Hub target = hub(targetKey);
                if (target == null) {
                    continue;
                }
                long deltaX = (long) target.chunkX() - source.chunkX();
                long deltaZ = (long) target.chunkZ() - source.chunkZ();
                long distanceSquared = deltaX * deltaX + deltaZ * deltaZ;
                int routeLength = Math.toIntExact(Math.abs(deltaX) + Math.abs(deltaZ));
                if (distanceSquared < minimumSquared || distanceSquared > maximumSquared
                    || routeLength < settings.minimumRouteLength()
                    || !hasRailwayClearRoute(source, target)) {
                    continue;
                }
                ConnectionKey key = ConnectionKey.of(sourceKey, targetKey);
                long rank = pairHash(CONNECTION_RANK_SALT, key);
                long acceptance = pairHash(CONNECTION_ACCEPT_SALT, key);
                out.add(
                    new Candidate(
                        source,
                        target,
                        key,
                        Sector.of(deltaX, deltaZ),
                        distanceSquared,
                        routeLength,
                        rank,
                        acceptance));
            }
        }
        out.sort(CANDIDATE_ORDER);
        return Collections.unmodifiableList(out);
    }

    /** Distance, then the target's score (higher first), then the two hashes unsigned, then the target cell. */
    private static final Comparator<Candidate> CANDIDATE_ORDER = (a, b) -> {
        int c = Long.compare(a.distanceSquared(), b.distanceSquared());
        if (c != 0) {
            return c;
        }
        c = Integer.compare(
            b.target()
                .score(),
            a.target()
                .score());
        if (c != 0) {
            return c;
        }
        c = Long.compareUnsigned(a.rank(), b.rank());
        if (c != 0) {
            return c;
        }
        c = Long.compareUnsigned(a.acceptance(), b.acceptance());
        return c != 0 ? c
            : a.target()
                .key()
                .compareTo(
                    b.target()
                        .key());
    };

    /**
     * Returns the cells a hub picks: one per sector first, then the rest in candidate order, up to the maximum.
     *
     * @param source the hub's cell
     * @return the picked cells
     */
    public List<HubKey> selected(HubKey source) {
        List<HubKey> cached = selections.get(source);
        if (cached != null) {
            return cached;
        }
        return selections.put(source, calculateSelected(source));
    }

    private List<HubKey> calculateSelected(HubKey source) {
        List<Candidate> list = candidates(source);
        if (list.isEmpty()) {
            return Collections.emptyList();
        }
        int maximum = settings.maximumConnectionsPerHub();
        List<HubKey> picked = new ArrayList<>(maximum);
        Set<Sector> used = EnumSet.noneOf(Sector.class);
        for (Candidate candidate : list) {
            if (used.add(candidate.sector())) {
                picked.add(
                    candidate.target()
                        .key());
                if (picked.size() == maximum) {
                    return Collections.unmodifiableList(picked);
                }
            }
        }
        for (Candidate candidate : list) {
            HubKey target = candidate.target()
                .key();
            if (!picked.contains(target)) {
                picked.add(target);
                if (picked.size() == maximum) {
                    break;
                }
            }
        }
        return Collections.unmodifiableList(picked);
    }

    /**
     * Returns a hub's accepted connections: the picks that pick it back, sorted.
     *
     * @param key the hub's cell
     * @return the connections
     */
    public List<ConnectionKey> accepted(HubKey key) {
        if (hub(key) == null) {
            return Collections.emptyList();
        }
        List<ConnectionKey> out = new ArrayList<>();
        for (HubKey neighbor : selected(key)) {
            if (selected(neighbor).contains(key)) {
                out.add(ConnectionKey.of(key, neighbor));
            }
        }
        Collections.sort(out);
        return out;
    }

    // ================================================================== R: routes

    /**
     * Returns the routes a cell owns: one per accepted connection whose smaller cell it is, sorted.
     *
     * @param owner the cell
     * @return the routes
     */
    public List<Route> ownedRoutes(HubKey owner) {
        List<Route> cached = routes.get(owner);
        if (cached != null) {
            return cached;
        }
        return routes.put(owner, calculateOwnedRoutes(owner));
    }

    private List<Route> calculateOwnedRoutes(HubKey owner) {
        Hub ownerHub = hub(owner);
        if (ownerHub == null) {
            return Collections.emptyList();
        }
        List<Route> out = new ArrayList<>();
        for (HubKey neighbor : selected(owner)) {
            ConnectionKey key = ConnectionKey.of(owner, neighbor);
            if (!key.owner()
                .equals(owner) || !selected(neighbor).contains(owner)) {
                continue;
            }
            out.add(createRoute(key, ownerHub, hub(neighbor)));
        }
        out.sort(Comparator.comparing(Route::key));
        return Collections.unmodifiableList(out);
    }

    /**
     * Builds a route between two accepted hubs: upstream's geometry, with obstacles avoided.
     *
     * @param key the connection
     * @param a   one hub
     * @param b   the other
     * @return the route
     */
    Route createRoute(ConnectionKey key, Hub a, Hub b) {
        Hub first = a.key()
            .equals(key.first()) ? a : b;
        Hub second = first == a ? b : a;
        int length = Math.abs(second.chunkX() - first.chunkX()) + Math.abs(second.chunkZ() - first.chunkZ());
        int level = selectLevel(first, second);
        Route.Shape shape;
        List<Segment> chosen;
        long penalty = 0L;
        if (first.chunkZ() == second.chunkZ()) {
            shape = Route.Shape.STRAIGHT;
            chosen = Collections.singletonList(
                new Segment(first.chunkX(), first.chunkZ(), second.chunkX(), second.chunkZ(), Axis.X));
        } else if (first.chunkX() == second.chunkX()) {
            shape = Route.Shape.STRAIGHT;
            chosen = Collections.singletonList(
                new Segment(first.chunkX(), first.chunkZ(), second.chunkX(), second.chunkZ(), Axis.Z));
        } else {
            List<Segment> horizontal = horizontalFirst(first, second);
            List<Segment> vertical = verticalFirst(first, second);
            boolean horizontalClear = isRailwayClear(horizontal);
            boolean verticalClear = isRailwayClear(vertical);
            boolean chooseHorizontal;
            if (horizontalClear != verticalClear) {
                chooseHorizontal = horizontalClear;
            } else {
                // Deviation (P06-1): among the shapes the railway rule leaves, one that crosses no obstacle wins.
                boolean horizontalBlocked = crossesObstacle(horizontal);
                boolean verticalBlocked = crossesObstacle(vertical);
                if (horizontalBlocked != verticalBlocked) {
                    chooseHorizontal = !horizontalBlocked;
                } else {
                    long horizontalPenalty = routePenalty(horizontal, first, second);
                    long verticalPenalty = routePenalty(vertical, first, second);
                    if (horizontalPenalty != verticalPenalty) {
                        chooseHorizontal = horizontalPenalty < verticalPenalty;
                    } else {
                        chooseHorizontal = (pairHash(ROUTE_SHAPE_SALT, key) & 1L) == 0L;
                    }
                    penalty = chooseHorizontal ? horizontalPenalty : verticalPenalty;
                }
            }
            shape = chooseHorizontal ? Route.Shape.HORIZONTAL_THEN_VERTICAL : Route.Shape.VERTICAL_THEN_HORIZONTAL;
            chosen = chooseHorizontal ? horizontal : vertical;
        }
        RouteGeometry geometry = new RouteGeometry(this, first, second, chosen);
        return new Route(key, first, second, level, shape, length, penalty, geometry.runs(), geometry.detours());
    }

    private static List<Segment> horizontalFirst(Hub first, Hub second) {
        List<Segment> out = new ArrayList<>(2);
        out.add(new Segment(first.chunkX(), first.chunkZ(), second.chunkX(), first.chunkZ(), Axis.X));
        out.add(new Segment(second.chunkX(), first.chunkZ(), second.chunkX(), second.chunkZ(), Axis.Z));
        return out;
    }

    private static List<Segment> verticalFirst(Hub first, Hub second) {
        List<Segment> out = new ArrayList<>(2);
        out.add(new Segment(first.chunkX(), first.chunkZ(), first.chunkX(), second.chunkZ(), Axis.Z));
        out.add(new Segment(first.chunkX(), second.chunkZ(), second.chunkX(), second.chunkZ(), Axis.X));
        return out;
    }

    private int selectLevel(Hub first, Hub second) {
        switch (settings.levelFromCities()) {
            case 0:
                return first.cityLevel();
            case 1:
                return Math.min(first.cityLevel(), second.cityLevel());
            case 2:
                return Math.max(first.cityLevel(), second.cityLevel());
            case 3:
                return (first.cityLevel() + second.cityLevel()) / 2;
            default:
                return settings.networkLevel();
        }
    }

    private boolean hasRailwayClearRoute(Hub first, Hub second) {
        if (first.chunkZ() == second.chunkZ()) {
            return !isHorizontalRailwayCorridor(first.chunkZ());
        }
        if (first.chunkX() == second.chunkX()) {
            return !isVerticalRailwayOrStationCorridor(first.chunkX());
        }
        return isRailwayClear(horizontalFirst(first, second)) || isRailwayClear(verticalFirst(first, second));
    }

    /** Upstream's rule: no X segment along an east-west railway row, no Z segment along a line or station column. */
    boolean isRailwayClear(List<Segment> segments) {
        for (Segment segment : segments) {
            if (segment.axis() == Axis.X && isHorizontalRailwayCorridor(segment.startZ())) {
                return false;
            }
            if (segment.axis() == Axis.Z && isVerticalRailwayOrStationCorridor(segment.startX())) {
                return false;
            }
        }
        return true;
    }

    private boolean crossesObstacle(List<Segment> segments) {
        for (Segment segment : segments) {
            HighwayObstacles.Area area = obstacles.area(
                Math.min(segment.startX(), segment.endX()),
                Math.min(segment.startZ(), segment.endZ()),
                Math.max(segment.startX(), segment.endX()),
                Math.max(segment.startZ(), segment.endZ()));
            int steps = segment.length();
            int stepX = segment.axis() == Axis.X ? segment.direction() : 0;
            int stepZ = segment.axis() == Axis.Z ? segment.direction() : 0;
            for (int i = 0; i <= steps; i++) {
                if (area.blocked(segment.startX() + stepX * i, segment.startZ() + stepZ * i)) {
                    return true;
                }
            }
        }
        return false;
    }

    private int moveXOffRailwayCorridor(int chunkX, int localX, int cellSize) {
        if (!isVerticalRailwayOrStationCorridor(chunkX)) {
            return chunkX;
        }
        return localX + 1 < cellSize ? Math.addExact(chunkX, 1) : Math.subtractExact(chunkX, 1);
    }

    private int moveZOffRailwayCorridor(int chunkZ, int localZ, int cellSize) {
        if (!isHorizontalRailwayCorridor(chunkZ)) {
            return chunkZ;
        }
        return localZ + 1 < cellSize ? Math.addExact(chunkZ, 1) : Math.subtractExact(chunkZ, 1);
    }

    /**
     * Returns whether a chunk row carries an east-west railway: {@code floorMod(z + 1, spacingNorthSouth) == 0}.
     *
     * @param chunkZ the row
     * @return whether a railway runs along it
     */
    public boolean isHorizontalRailwayCorridor(int chunkZ) {
        return Math.floorMod(chunkZ + 1, settings.railwaySpacingNorthSouth()) == 0;
    }

    /**
     * Returns whether a chunk column carries a north-south railway or station spots:
     * {@code floorMod(x + 1, spacingEastWest)} is 0 or half the spacing.
     *
     * @param chunkX the column
     * @return whether it is a railway column
     */
    public boolean isVerticalRailwayOrStationCorridor(int chunkX) {
        int spacing = settings.railwaySpacingEastWest();
        int gridX = Math.floorMod(chunkX + 1, spacing);
        return gridX == spacing / 2 || gridX == 0;
    }

    private long routePenalty(List<Segment> segments, Hub first, Hub second) {
        if (settings.routeCityPenalty() == 0.0f) {
            return 0L;
        }
        long sum = 0L;
        for (Segment segment : segments) {
            int length = segment.length();
            for (int distance = 0; distance <= length; distance += ROUTE_PENALTY_SAMPLE_SPACING) {
                int chunkX = interpolate(segment.startX(), segment.endX(), distance, length);
                int chunkZ = interpolate(segment.startZ(), segment.endZ(), distance, length);
                if (manhattan(chunkX, chunkZ, first.chunkX(), first.chunkZ()) <= ENDPOINT_PENALTY_DISCOUNT
                    || manhattan(chunkX, chunkZ, second.chunkX(), second.chunkZ()) <= ENDPOINT_PENALTY_DISCOUNT) {
                    continue;
                }
                sum += CityPotential.score(potential.potential(chunkX, chunkZ));
            }
        }
        return Math.round(sum * (double) settings.routeCityPenalty());
    }

    /** Returns the obstacles, for the route geometry. */
    HighwayObstacles obstacles() {
        return obstacles;
    }

    /** Returns the city levels, for the route geometry's dead ends. */
    HighwayTerrain terrain() {
        return terrain;
    }

    // ================================================================== level sharing (HWY-09)

    /**
     * Returns a route's level after sharing: its own, or the lowest own level of the routes it shares a chunk with
     * along the same axis, among the routes owned by the cells within the search radius of its owner (avoids HWY-09;
     * one step, not repeated, so the rule stays bounded, new-spec 06).
     *
     * @param route the route
     * @return the level it is built at
     */
    public int sharedLevel(Route route) {
        Integer cached = levels.get(route.key());
        if (cached != null) {
            return cached;
        }
        int level = route.level();
        HubKey owner = route.key()
            .owner();
        int radius = settings.hubSearchRadiusCells();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (Route other : ownedRoutes(owner.offset(dx, dz))) {
                    if (other.level() < level && !other.key()
                        .equals(route.key()) && overlap(route, other)) {
                        level = other.level();
                    }
                }
            }
        }
        return levels.put(route.key(), level);
    }

    private static boolean overlap(Route a, Route b) {
        for (Segment sa : a.segments()) {
            for (Segment sb : b.segments()) {
                if (sa.overlaps(sb)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ================================================================== Q: chunk query

    /**
     * Returns the highway in a chunk.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the info; {@link HighwayInfo#NONE} when none passes or highways are off
     */
    public HighwayInfo info(int chunkX, int chunkZ) {
        if (!settings.enabled()) {
            // Avoids HWY-06: switched off means no highway anywhere.
            return HighwayInfo.NONE;
        }
        Long key = ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
        HighwayInfo cached = chunks.get(key);
        if (cached != null) {
            return cached;
        }
        return chunks.put(key, calculateInfo(chunkX, chunkZ));
    }

    private HighwayInfo calculateInfo(int chunkX, int chunkZ) {
        HubKey queryCell = cell(chunkX, chunkZ);
        int radius = settings.hubSearchRadiusCells();
        Map<ConnectionKey, Route> found = new TreeMap<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (Route route : ownedRoutes(queryCell.offset(dx, dz))) {
                    if (route.contains(chunkX, chunkZ)) {
                        found.put(route.key(), route);
                    }
                }
            }
        }
        if (found.isEmpty()) {
            return HighwayInfo.NONE;
        }
        List<HighwayInfo.Hit> hits = new ArrayList<>();
        Deck bestX = null;
        Deck bestZ = null;
        for (Route route : found.values()) {
            int level = sharedLevel(route);
            Deck xDeck = null;
            Deck zDeck = null;
            for (Run run : route.runs()) {
                int index = run.indexOf(chunkX, chunkZ);
                if (index < 0) {
                    continue;
                }
                boolean end = index == 0 || index == run.length();
                int entry = run.entryLevel(index, level);
                int exit = run.exitLevel(index, level);
                for (Segment segment : run.segments()) {
                    if (!segment.contains(chunkX, chunkZ)) {
                        continue;
                    }
                    boolean forward = segment.direction() >= 0;
                    Deck deck = new Deck(
                        segment.axis(),
                        forward ? entry : exit,
                        forward ? exit : entry,
                        end,
                        route.key());
                    if (segment.axis() == Axis.X) {
                        xDeck = xDeck == null ? deck : xDeck;
                    } else {
                        zDeck = zDeck == null ? deck : zDeck;
                    }
                }
                break;
            }
            hits.add(new HighwayInfo.Hit(route, level, xDeck, zDeck));
            bestX = lower(bestX, xDeck);
            bestZ = lower(bestZ, zDeck);
        }
        return new HighwayInfo(bestX, bestZ, hits);
    }

    /** The deck with the lower bottom; the first one (lower connection key) on a tie. */
    private static Deck lower(Deck current, Deck candidate) {
        if (candidate == null) {
            return current;
        }
        if (current == null || candidate.min() < current.min()) {
            return candidate;
        }
        return current;
    }

    /** Returns hub, chunk and route cache counts, for the debug output: hits, misses and sizes. */
    public String cacheStats() {
        return "hubs " + hubs.size()
            + " ("
            + hubs.hits()
            + " hits, "
            + hubs.misses()
            + " misses), routes "
            + routes.size()
            + ", chunks "
            + chunks.size()
            + " ("
            + chunks.hits()
            + " hits, "
            + chunks.misses()
            + " misses)";
    }

    // ================================================================== hashing

    /**
     * Upstream's hash: the world seed, the dimension salt, the version salt and a purpose salt, with SplitMix64 mixes
     * of the three inputs widened without sign extension.
     *
     * @param salt  the purpose salt
     * @param x     first input
     * @param z     second input
     * @param extra third input
     * @return the hash
     */
    long hash(long salt, int x, int z, int extra) {
        long value = seed ^ dimensionSalt ^ VERSION_SALT ^ salt;
        value ^= StableHash.mix64(Integer.toUnsignedLong(x) + 0x9e3779b97f4a7c15L);
        value ^= StableHash.mix64(Integer.toUnsignedLong(z) + 0xc2b2ae3d27d4eb4fL);
        value ^= StableHash.mix64(Integer.toUnsignedLong(extra) + 0x165667b19e3779f9L);
        return StableHash.mix64(value);
    }

    private long pairHash(long salt, ConnectionKey key) {
        return hash(
            salt,
            key.first()
                .cellX(),
            key.first()
                .cellZ(),
            pairExtra(key.second()));
    }

    /**
     * Upstream's {@code pairExtra}: the low 32 bits of a mixed cell, folded.
     *
     * @param key the cell
     * @return the folded value
     */
    static int pairExtra(HubKey key) {
        long mixed = StableHash
            .mix64(Integer.toUnsignedLong(key.cellX()) ^ Long.rotateLeft(Integer.toUnsignedLong(key.cellZ()), 32));
        return (int) (mixed ^ mixed >>> 32);
    }

    private static long square(int value) {
        return (long) value * value;
    }

    private static int interpolate(int start, int end, int distance, int length) {
        if (length == 0) {
            return start;
        }
        return start + Integer.signum(end - start) * distance;
    }

    private static int manhattan(int x1, int z1, int x2, int z2) {
        return Math.toIntExact(Math.abs((long) x2 - x1) + Math.abs((long) z2 - z1));
    }
}
