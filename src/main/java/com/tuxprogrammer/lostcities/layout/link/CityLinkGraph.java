package com.tuxprogrammer.lostcities.layout.link;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.tuxprogrammer.lostcities.layout.perf.PerfCounters;

/**
 * The city link graph of a grid-placed world ({@code networkLayout=links}, docs/design/0015-city-link-network.md §1).
 * Nodes are the grid's city centers that actually yield a city (the filtered mask). Which nodes link is decided from
 * their positions alone (D-perf2-links-1): two nodes at most {@link #maxDistance()} chunks apart link when they are
 * relative neighbors (no third node is nearer to both than they are to each other) or when either is one of the
 * other's {@link #NEAREST} nearest nodes. That rule reads only the two nodes and the nodes around them, so it is
 * symmetric and both ends decide it identically. Every link is then routed ({@link #route}) around landmarks; one that
 * no path clears is dropped when both its ends keep {@link #NEAREST} routable links without it, else kept on its
 * straight raster (D-perf2-links-2), so every node with {@link #NEAREST} nodes in range keeps at least that many links.
 * Every highway and railway chunk of the network lies on a link, so following it either way reaches a city.
 *
 * <p>
 * Queries are lazy (D-perf2-links-3). A routed path stays inside its ends' bounding box widened by
 * {@link #ROUTE_MARGIN} chunks, so a cell's candidate links are found from the node positions alone, without routing;
 * a chunk query then routes only the candidates whose widened box holds the chunk and reads the chunk off their paths.
 * Drawing one chunk therefore routes only links that can touch it, and walking a long link's chunks never routes the
 * links of every cell it crosses. Everything is a function of the seed and the {@link Nodes} oracle, so the answer does
 * not depend on generation order. The oracle reads terrain oracles only, never loads a chunk, so the graph cannot
 * cascade.
 *
 * <p>
 * Work is shared and bounded (D-linkgraph-shrink-2, -3): a link's route is computed once per unordered pair of ends and
 * cached, as are each cell's node, in-range nodes, partners, kept links and candidate links, and each chunk's hits;
 * every scan over neighboring cells skips the cells that cannot hold a node or a path in range. None of this changes
 * an answer: each skipped cell provably contributes nothing, and each cached value is the one the computation returns.
 *
 * <p>
 * Side: server. One instance per dimension's layout, like the other planners. Thread-safe: the caches are synchronized
 * LRUs locked only around each read and write, never while a value is computed, and their values are immutable pure
 * functions of the key, so concurrent callers that compute the same value at once both return the first one stored.
 */
public final class CityLinkGraph {

    /**
     * The fewest links a node keeps when that many nodes lie in range: each node links its this many nearest, and a
     * link no path clears is dropped only while both ends keep this many routable links (owner rule, D-city-scale-1,
     * D-perf2-links-2).
     */
    public static final int NEAREST = 2;
    /** Longest link, in chunks, on a grid of {@link #REFERENCE_CELL}-chunk cells (owner decision 8). */
    public static final int MAX_DISTANCE = 160;
    /**
     * The cell size {@link #MAX_DISTANCE} was set for. A grid with larger cells stretches the cap with them
     * (D-city-scale-1),
     * so the same neighbors stay in reach; a smaller one keeps the cap.
     */
    public static final int REFERENCE_CELL = 64;
    /**
     * How far, in chunks, a routed path may leave the bounding box of its two ends, on either axis: the largest detour
     * waypoint offset across the link (64), one for the waypoint's rounding and one of slack. The lazy link index
     * relies on it (class Javadoc); {@code CityLinkGraphGeometryTest} checks every route keeps it, so a change to the
     * detours that widens them must widen this too.
     */
    public static final int ROUTE_MARGIN = 66;

    /** Where the nodes are: the grid's centers and whether each yields a city. */
    public interface Nodes {

        /** Returns the grid's cell size, in chunks. */
        int cell();

        /**
         * Returns the city center of a cell if it yields a city, as {chunkX, chunkZ}, or {@code null}.
         *
         * @param cellX cell X
         * @param cellZ cell Z
         * @return the center, or {@code null} when the cell's center yields no city
         */
        int[] cityCenter(int cellX, int cellZ);

        /**
         * Returns whether a link must not cross a chunk: a landmark's core or margin (0013 §1, 0015 §6). The default
         * blocks nothing.
         *
         * @param chunkX chunk X
         * @param chunkZ chunk Z
         * @return whether the chunk is blocked
         */
        default boolean blocked(int chunkX, int chunkZ) {
            return false;
        }
    }

    /**
     * One link: two city centers, lower end first, and its path. Immutable, so links cached and shared between threads
     * need no lock: the path is rasterized in the constructor.
     */
    public static final class Link {

        private final int ax;
        private final int az;
        private final int bx;
        private final int bz;
        private final List<LinkRaster.Step> path;
        private final int minX;
        private final int minZ;
        private final int maxX;
        private final int maxZ;

        /** The straight link between two centers, given in either order. */
        Link(int ax, int az, int bx, int bz) {
            this(ax, az, bx, bz, null);
        }

        /**
         * A link along a given path (a detour, D390), or the straight raster when the path is {@code null}; the path
         * runs from the lower end.
         */
        Link(int ax, int az, int bx, int bz, List<LinkRaster.Step> path) {
            boolean lower = LinkRaster.lower(ax, az, bx, bz);
            this.ax = lower ? ax : bx;
            this.az = lower ? az : bz;
            this.bx = lower ? bx : ax;
            this.bz = lower ? bz : az;
            List<LinkRaster.Step> steps = path != null ? path
                : LinkRaster.rasterize(this.ax, this.az, this.bx, this.bz);
            int x0 = Integer.MAX_VALUE;
            int z0 = Integer.MAX_VALUE;
            int x1 = Integer.MIN_VALUE;
            int z1 = Integer.MIN_VALUE;
            for (LinkRaster.Step step : steps) {
                x0 = Math.min(x0, step.chunkX());
                z0 = Math.min(z0, step.chunkZ());
                x1 = Math.max(x1, step.chunkX());
                z1 = Math.max(z1, step.chunkZ());
            }
            this.path = steps;
            this.minX = x0;
            this.minZ = z0;
            this.maxX = x1;
            this.maxZ = z1;
        }

        /** Returns the lower end's chunk X. */
        public int ax() {
            return ax;
        }

        /** Returns the lower end's chunk Z. */
        public int az() {
            return az;
        }

        /** Returns the upper end's chunk X. */
        public int bx() {
            return bx;
        }

        /** Returns the upper end's chunk Z. */
        public int bz() {
            return bz;
        }

        /**
         * Returns whether both ends are cities. Always true in this graph; the station hook for lane 4c (0015 §5):
         * stations go only on links for which this holds.
         */
        public boolean bothEndsCities() {
            return true;
        }

        /**
         * Returns the link's chunks, lower end first: the straight raster, or the detour the graph chose around a
         * landmark (D390). Equal links carry the same path, since the graph derives it from the ends alone.
         */
        public List<LinkRaster.Step> path() {
            return path;
        }

        /** Returns whether the path's bounding box overlaps a chunk rectangle. */
        boolean overlaps(int x0, int z0, int x1, int z1) {
            return maxX >= x0 && minX <= x1 && maxZ >= z0 && minZ <= z1;
        }

        /**
         * Appends this link's hits in one chunk, path order. Consecutive steps are 8-neighbors, so a step {@code d}
         * chunks (Chebyshev) from the chunk is at least {@code d} steps from any step in it, and the scan skips ahead
         * that far.
         */
        void hitsAt(int chunkX, int chunkZ, List<Hit> out) {
            if (!overlaps(chunkX, chunkZ, chunkX, chunkZ)) {
                return;
            }
            int i = 0;
            int n = path.size();
            while (i < n) {
                LinkRaster.Step step = path.get(i);
                int d = Math.max(Math.abs(step.chunkX() - chunkX), Math.abs(step.chunkZ() - chunkZ));
                if (d == 0) {
                    out.add(new Hit(this, step, i));
                    i++;
                } else {
                    i += d;
                }
            }
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Link)) {
                return false;
            }
            Link l = (Link) o;
            return ax == l.ax && az == l.az && bx == l.bx && bz == l.bz;
        }

        @Override
        public int hashCode() {
            return ((ax * 31 + az) * 31 + bx) * 31 + bz;
        }

        @Override
        public String toString() {
            return "link " + ax + "," + az + " -> " + bx + "," + bz;
        }
    }

    /** One link's piece in a chunk. */
    public static final class Hit {

        private final Link link;
        private final LinkRaster.Step step;
        private final int index;

        Hit(Link link, LinkRaster.Step step, int index) {
            this.link = link;
            this.step = step;
            this.index = index;
        }

        /** Returns the link. */
        public Link link() {
            return link;
        }

        /** Returns the link's step in this chunk. */
        public LinkRaster.Step step() {
            return step;
        }

        /** Returns the step's index on the link's path, 0 at the lower end. */
        public int index() {
            return index;
        }
    }

    /**
     * A node: its center and its cell. Carrying the cell means no rule assumes where a center lies within its cell.
     */
    static final class Node {

        final int x;
        final int z;
        final int cellX;
        final int cellZ;

        Node(int x, int z, int cellX, int cellZ) {
            this.x = x;
            this.z = z;
            this.cellX = cellX;
            this.cellZ = cellZ;
        }

        boolean at(int ox, int oz) {
            return x == ox && z == oz;
        }

        long d2(Node o) {
            long dx = o.x - x;
            long dz = o.z - z;
            return dx * dx + dz * dz;
        }
    }

    /** A geometric link before routing: its two nodes, lower end first. */
    private static final class Candidate {

        final Node a;
        final Node b;

        Candidate(Node a, Node b) {
            this.a = a;
            this.b = b;
        }

        /** Whether the ends' box widened by {@link #ROUTE_MARGIN} overlaps a chunk rectangle. */
        boolean mayCross(int x0, int z0, int x1, int z1) {
            return Math.max(a.x, b.x) + ROUTE_MARGIN >= x0 && Math.min(a.x, b.x) - ROUTE_MARGIN <= x1
                && Math.max(a.z, b.z) + ROUTE_MARGIN >= z0
                && Math.min(a.z, b.z) - ROUTE_MARGIN <= z1;
        }
    }

    private static final int[] NONE = new int[0];
    private static final Node[] NO_NODES = new Node[0];
    /** The route cache's mark for a pair no path clears; never handed out. */
    private static final Link NO_ROUTE = new Link(0, 0, 0, 0);

    private final Nodes nodes;
    private final int cell;
    private final int reach;
    private final int maxDistance;
    private final Lru<Long, int[]> centers;
    private final Lru<Long, Node[]> inRange;
    private final Lru<Long, Node[]> partners;
    private final Lru<Long, List<Link>> neighbors;
    private final Lru<Long, List<Link>> symmetric;
    private final Lru<Pair, Link> routes;
    private final Lru<Pair, Link> kept;
    private final Lru<Long, List<Candidate>> cellCandidates;
    private final Lru<Long, List<Hit>> chunkHits;
    private final AtomicInteger candidateBuilds = new AtomicInteger();

    /**
     * Builds the graph over a node oracle.
     *
     * @param nodes     the grid's city centers
     * @param cacheSize cache capacity, in cells
     */
    public CityLinkGraph(Nodes nodes, int cacheSize) {
        this.nodes = nodes;
        this.cell = nodes.cell();
        this.maxDistance = maxDistanceFor(cell);
        // A center lies anywhere in its cell, so two centers maxDistance apart are at most this many cells apart.
        this.reach = (maxDistance + cell - 1) / cell + 1;
        int size = Math.max(64, cacheSize);
        this.centers = new Lru<>(size * 4);
        this.inRange = new Lru<>(size * 2);
        this.partners = new Lru<>(size * 2);
        this.neighbors = new Lru<>(size * 2);
        this.symmetric = new Lru<>(size * 2);
        // About the links the cached nodes keep: three per node on average, each shared by two nodes.
        this.routes = new Lru<>(size * 6);
        this.kept = new Lru<>(size * 6);
        this.cellCandidates = new Lru<>(Math.max(64, size / 2));
        // Enough chunks for the shared-chunk scans along a few long links (LinkHighways), each value a short list.
        this.chunkHits = new Lru<>(size * 8);
    }

    /** Returns the grid cell size, in chunks. */
    public int cell() {
        return cell;
    }

    /**
     * Returns the longest link on a grid of a given cell size, in chunks: {@link #MAX_DISTANCE} scaled by the cell over
     * {@link #REFERENCE_CELL}, never below {@link #MAX_DISTANCE}. Neighbors on a jittered grid sit about one cell
     * apart, so a fixed cap would leave a grid of 256-chunk cells (the {@code rarecities} profile) without a single
     * link; scaling it keeps the same number of candidates per node at every cell size (D-city-scale-1).
     *
     * @param cell the cell edge in chunks
     * @return the cap in chunks
     */
    public static int maxDistanceFor(int cell) {
        return Math.max(MAX_DISTANCE, (int) ((long) MAX_DISTANCE * cell / REFERENCE_CELL));
    }

    /** Returns this graph's longest link, in chunks ({@link #maxDistanceFor}). */
    public int maxDistance() {
        return maxDistance;
    }

    /** Returns how many cells around a cell its links' ends may lie in. */
    public int reach() {
        return reach;
    }

    /**
     * Returns a cell's city center, or {@code null}, cached.
     *
     * @param cellX cell X
     * @param cellZ cell Z
     * @return {chunkX, chunkZ}, or {@code null}
     */
    public int[] node(int cellX, int cellZ) {
        int[] cached = centers.get(key(cellX, cellZ));
        if (cached == null) {
            int[] center = nodes.cityCenter(cellX, cellZ);
            cached = centers.putIfAbsent(key(cellX, cellZ), center == null ? NONE : center);
        }
        return cached.length == 0 ? null : cached;
    }

    /**
     * Returns whether a cell's chunk box lies farther than {@link #maxDistance()} from a point, in straight-line
     * distance: then no node in that cell is within link range of the point (D-linkgraph-shrink-3).
     */
    private boolean outOfRange(int x, int z, int cellX, int cellZ) {
        long dx = gap(x, cellX);
        long dz = gap(z, cellZ);
        return dx * dx + dz * dz > (long) maxDistance * maxDistance;
    }

    /** Returns how far a coordinate lies outside a cell's span along one axis, 0 inside. */
    private int gap(int v, int cellV) {
        int min = cellV * cell;
        int max = min + cell - 1;
        return v < min ? min - v : v > max ? v - max : 0;
    }

    /**
     * Returns the nodes within {@link #maxDistance()} of a cell's node, nearest first, ties to the lower center; empty
     * when the cell has no node. Positions only: no routing.
     */
    private Node[] inRange(int cellX, int cellZ) {
        Node[] cached = inRange.get(key(cellX, cellZ));
        if (cached != null) {
            return cached;
        }
        int[] self = node(cellX, cellZ);
        List<Node> found = new ArrayList<>();
        if (self != null) {
            long max = (long) maxDistance * maxDistance;
            Node me = new Node(self[0], self[1], cellX, cellZ);
            for (int ox = -reach; ox <= reach; ox++) {
                for (int oz = -reach; oz <= reach; oz++) {
                    // A cell out of range holds no candidate: its node, if any, fails the distance test below.
                    if (ox == 0 && oz == 0 || outOfRange(self[0], self[1], cellX + ox, cellZ + oz)) {
                        continue;
                    }
                    int[] other = node(cellX + ox, cellZ + oz);
                    if (other == null) {
                        continue;
                    }
                    Node o = new Node(other[0], other[1], cellX + ox, cellZ + oz);
                    if (me.d2(o) <= max) {
                        found.add(o);
                    }
                }
            }
            found.sort((p, q) -> {
                int c = Long.compare(me.d2(p), me.d2(q));
                if (c != 0) {
                    return c;
                }
                c = Integer.compare(p.x, q.x);
                return c != 0 ? c : Integer.compare(p.z, q.z);
            });
        }
        return inRange.putIfAbsent(key(cellX, cellZ), found.isEmpty() ? NO_NODES : found.toArray(NO_NODES));
    }

    /**
     * Returns the nodes a cell's node links to by position (class Javadoc, D-perf2-links-1), nearest first: each node
     * in range that is a relative neighbor, one of its {@link #NEAREST} nearest, or has it among its own
     * {@link #NEAREST} nearest. Symmetric: B is among A's partners exactly when A is among B's. No routing.
     */
    Node[] partners(int cellX, int cellZ) {
        Node[] cached = partners.get(key(cellX, cellZ));
        if (cached != null) {
            return cached;
        }
        Node[] near = inRange(cellX, cellZ);
        List<Node> out = new ArrayList<>();
        if (near.length > 0) {
            int[] self = node(cellX, cellZ);
            Node me = new Node(self[0], self[1], cellX, cellZ);
            for (int i = 0; i < near.length; i++) {
                Node other = near[i];
                if (i < NEAREST || relativeNeighbors(me, other, near) || amongNearest(me, other)) {
                    out.add(other);
                }
            }
        }
        return partners.putIfAbsent(key(cellX, cellZ), out.isEmpty() ? NO_NODES : out.toArray(NO_NODES));
    }

    /**
     * Whether no third node is strictly nearer to both {@code a} and {@code b} than they are to each other. Any such
     * node is nearer to {@code a} than {@code b} is, so it is in {@code a}'s in-range list ({@code near}, sorted by
     * distance from {@code a}); the scan stops at {@code b}'s distance. Strict, so the answer is the same from either
     * end.
     */
    private static boolean relativeNeighbors(Node a, Node b, Node[] near) {
        long ab = a.d2(b);
        for (Node c : near) {
            long ac = a.d2(c);
            if (ac >= ab) {
                break;
            }
            if (b.d2(c) < ab) {
                return false;
            }
        }
        return true;
    }

    /** Whether {@code a} is one of {@code b}'s {@link #NEAREST} nearest nodes, by {@code b}'s own ordering. */
    private boolean amongNearest(Node a, Node b) {
        Node[] near = inRange(b.cellX, b.cellZ);
        for (int i = 0; i < near.length && i < NEAREST; i++) {
            if (near[i].at(a.x, a.z)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the link a geometric pair keeps, or {@code null} when it is dropped (D-perf2-links-2): its route when one
     * clears the landmarks; else nothing when both ends keep {@link #NEAREST} routable links without it; else the
     * straight raster, so no node falls below {@link #NEAREST} links. A pure function of the unordered pair, cached
     * under it lower end first.
     */
    private Link resolve(Node p, Node q) {
        boolean lower = LinkRaster.lower(p.x, p.z, q.x, q.z);
        Node a = lower ? p : q;
        Node b = lower ? q : p;
        Pair pair = new Pair(a.x, a.z, b.x, b.z);
        Link cached = kept.get(pair);
        if (cached == null) {
            Link link = route(a.x, a.z, b.x, b.z);
            if (link == null) {
                boolean droppable = routableWithout(a, b) >= NEAREST && routableWithout(b, a) >= NEAREST;
                link = droppable ? null : new Link(a.x, a.z, b.x, b.z);
            }
            cached = kept.putIfAbsent(pair, link == null ? NO_ROUTE : link);
        }
        return cached == NO_ROUTE ? null : cached;
    }

    /** Counts a node's partners other than one whose route clears, stopping at {@link #NEAREST}. */
    private int routableWithout(Node self, Node other) {
        int count = 0;
        for (Node p : partners(self.cellX, self.cellZ)) {
            if (p.at(other.x, other.z)) {
                continue;
            }
            if (route(self.x, self.z, p.x, p.z) != null && ++count >= NEAREST) {
                break;
            }
        }
        return count;
    }

    /**
     * Returns the links a cell's node keeps (class Javadoc), nearest far end first; empty when the cell has no node.
     * Symmetric since D-perf2-links-1: the same links as {@link #links}, in distance order. Routes only this node's
     * links (and, for one no path clears, its ends' other links).
     *
     * @param cellX cell X
     * @param cellZ cell Z
     * @return the kept links
     */
    public List<Link> chosen(int cellX, int cellZ) {
        List<Link> cached = neighbors.get(key(cellX, cellZ));
        if (cached != null) {
            return cached;
        }
        int[] self = node(cellX, cellZ);
        Node[] far = partners(cellX, cellZ);
        List<Link> out = new ArrayList<>();
        if (self != null) {
            Node me = new Node(self[0], self[1], cellX, cellZ);
            for (Node other : far) {
                Link link = resolve(me, other);
                if (link != null) {
                    out.add(link);
                }
            }
        }
        if (self != null && out.size() < NEAREST && com.tuxprogrammer.lostcities.layout.LayoutTrace.on()) {
            com.tuxprogrammer.lostcities.layout.LayoutTrace.log(
                "linkGraph node=%d,%d partners=%d kept=%d (fewer nodes in range than the minimum)",
                self[0],
                self[1],
                far.length,
                out.size());
        }
        return neighbors.putIfAbsent(key(cellX, cellZ), Collections.unmodifiableList(out));
    }

    /** Waypoint offsets across the link, in chunks, nearest first (D390). */
    private static final int[] DETOUR_OFFSETS = { 12, 24, 36, 48, 64 };
    /** Waypoint positions along the link, as a share of its length (D390). */
    private static final double[] DETOUR_ALONG = { 0.5, 0.3, 0.7 };

    /**
     * Returns the link between two centers along a path that crosses no {@link Nodes#blocked} chunk, or {@code null}:
     * the straight raster if it is clear, else the first clear detour ({@link LinkRaster#rasterizeVia}) through a
     * waypoint offset across the link (each of {@link #DETOUR_OFFSETS}, at each of {@link #DETOUR_ALONG}, left before
     * right), whose waypoint lies within {@link #maxDistance()} of both ends, so {@link #reach} still bounds every
     * chunk of the path. A function of the two ends (taken lower first) and the oracle only, so whichever end asks
     * gets the same path; so it is computed once per unordered pair and cached under the pair, lower end first
     * (D-linkgraph-shrink-2).
     *
     * @param x1 one end's chunk X
     * @param z1 one end's chunk Z
     * @param x2 the other end's chunk X
     * @param z2 the other end's chunk Z
     * @return the link, or null when no path clears
     */
    Link route(int x1, int z1, int x2, int z2) {
        boolean lower = LinkRaster.lower(x1, z1, x2, z2);
        Pair pair = lower ? new Pair(x1, z1, x2, z2) : new Pair(x2, z2, x1, z1);
        Link cached = routes.get(pair);
        if (cached == null) {
            long perfRoute = PerfCounters.start();
            Link computed = computeRoute(x1, z1, x2, z2);
            PerfCounters.add("clg.route", perfRoute);
            cached = routes.putIfAbsent(pair, computed == null ? NO_ROUTE : computed);
        }
        return cached == NO_ROUTE ? null : cached;
    }

    /** Returns how many routes the cache holds, for tests. */
    int cachedRoutes() {
        return routes.size();
    }

    private Link computeRoute(int x1, int z1, int x2, int z2) {
        Link straight = new Link(x1, z1, x2, z2);
        if (!crossesBlocked(straight.path())) {
            return straight;
        }
        int ax = straight.ax;
        int az = straight.az;
        int bx = straight.bx;
        int bz = straight.bz;
        double dx = bx - ax;
        double dz = bz - az;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length == 0) {
            return null;
        }
        // The unit normal, left of the lower-to-upper direction.
        double nx = -dz / length;
        double nz = dx / length;
        long max = (long) maxDistance * maxDistance;
        for (int offset : DETOUR_OFFSETS) {
            for (double along : DETOUR_ALONG) {
                for (int side = 1; side >= -1; side -= 2) {
                    int wx = (int) Math.round(ax + along * dx + side * offset * nx);
                    int wz = (int) Math.round(az + along * dz + side * offset * nz);
                    long da = (long) (wx - ax) * (wx - ax) + (long) (wz - az) * (wz - az);
                    long db = (long) (wx - bx) * (wx - bx) + (long) (wz - bz) * (wz - bz);
                    if (da > max || db > max) {
                        continue;
                    }
                    List<LinkRaster.Step> path = LinkRaster.rasterizeVia(ax, az, wx, wz, bx, bz);
                    if (path != null && !crossesBlocked(path)) {
                        return new Link(ax, az, bx, bz, path);
                    }
                }
            }
        }
        return null;
    }

    private boolean crossesBlocked(List<LinkRaster.Step> path) {
        for (LinkRaster.Step step : path) {
            if (nodes.blocked(step.chunkX(), step.chunkZ())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns every link at a cell's node: the links it keeps, which since D-perf2-links-1 are also every link that
     * reaches it. Sorted by ends. Cached per cell, so the city highways, districts and scenarios that ask for the same
     * node share one computation (D-linkgraph-shrink-4).
     *
     * @param cellX cell X
     * @param cellZ cell Z
     * @return the links, unmodifiable; empty when the cell has no node
     */
    public List<Link> links(int cellX, int cellZ) {
        List<Link> cached = symmetric.get(key(cellX, cellZ));
        if (cached == null) {
            long perfLinks = PerfCounters.start();
            List<Link> computed = new ArrayList<>(chosen(cellX, cellZ));
            computed.sort(CityLinkGraph::byEnds);
            PerfCounters.add("clg.links", perfLinks);
            cached = symmetric.putIfAbsent(key(cellX, cellZ), Collections.unmodifiableList(computed));
        }
        return cached;
    }

    private static int byEnds(Link p, Link q) {
        return p.ax != q.ax ? Integer.compare(p.ax, q.ax)
            : p.az != q.az ? Integer.compare(p.az, q.az)
                : p.bx != q.bx ? Integer.compare(p.bx, q.bx) : Integer.compare(p.bz, q.bz);
    }

    /**
     * Returns the kept links whose ends' bounding box overlaps a chunk rectangle within a cell, sorted by ends. Routes
     * only the candidates of that cell whose box does (for {@link LinkRails}, whose lines stay in their ends' box).
     *
     * @param cellX the cell X holding the rectangle
     * @param cellZ the cell Z holding the rectangle
     * @param x0    rectangle min chunk X, at most {@link #ROUTE_MARGIN} outside the cell
     * @param z0    rectangle min chunk Z, likewise
     * @param x1    rectangle max chunk X, likewise
     * @param z1    rectangle max chunk Z, likewise
     * @return the links
     */
    public List<Link> linksNear(int cellX, int cellZ, int x0, int z0, int x1, int z1) {
        List<Link> out = new ArrayList<>();
        for (Candidate c : candidates(cellX, cellZ)) {
            if (Math.max(c.a.x, c.b.x) >= x0 && Math.min(c.a.x, c.b.x) <= x1
                && Math.max(c.a.z, c.b.z) >= z0
                && Math.min(c.a.z, c.b.z) <= z1) {
                Link link = resolve(c.a, c.b);
                if (link != null) {
                    out.add(link);
                }
            }
        }
        return out;
    }

    /**
     * Returns the link pieces in a chunk: every kept link whose path crosses it, sorted by ends, then by path index.
     * Empty off the network. Routes only the cell's candidates whose widened box holds the chunk (class Javadoc).
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the hits
     */
    public List<Hit> hits(int chunkX, int chunkZ) {
        long chunk = key(chunkX, chunkZ);
        List<Hit> cached = chunkHits.get(chunk);
        if (cached != null) {
            return cached;
        }
        List<Hit> out = new ArrayList<>(2);
        for (Candidate c : candidates(Math.floorDiv(chunkX, cell), Math.floorDiv(chunkZ, cell))) {
            if (!c.mayCross(chunkX, chunkZ, chunkX, chunkZ)) {
                continue;
            }
            Link link = resolve(c.a, c.b);
            if (link != null) {
                link.hitsAt(chunkX, chunkZ, out);
            }
        }
        return chunkHits
            .putIfAbsent(chunk, out.isEmpty() ? Collections.<Hit>emptyList() : Collections.unmodifiableList(out));
    }

    /**
     * Returns a cell's candidate links: every geometric pair (class Javadoc) whose ends' box widened by
     * {@link #ROUTE_MARGIN} overlaps the cell, so every kept link with a chunk in the cell is among them. Sorted by
     * ends. Positions only, no routing: the cascade the old per-cell index caused (every crossed cell routing every
     * nearby node's candidates, D-perf2-links-3) is gone.
     */
    private List<Candidate> candidates(int cellX, int cellZ) {
        List<Candidate> cached = cellCandidates.get(key(cellX, cellZ));
        if (cached != null) {
            return cached;
        }
        long perfIndex = PerfCounters.start();
        candidateBuilds.incrementAndGet();
        int minX = cellX * cell;
        int minZ = cellZ * cell;
        int maxX = minX + cell - 1;
        int maxZ = minZ + cell - 1;
        // A link is at most maxDistance long, so both its ends lie within maxDistance + ROUTE_MARGIN of the cell on
        // each axis when its widened box overlaps it.
        int span = (maxDistance + ROUTE_MARGIN + cell - 1) / cell;
        List<Candidate> out = new ArrayList<>();
        for (int ox = -span; ox <= span; ox++) {
            for (int oz = -span; oz <= span; oz++) {
                int[] self = node(cellX + ox, cellZ + oz);
                if (self == null) {
                    continue;
                }
                Node me = new Node(self[0], self[1], cellX + ox, cellZ + oz);
                for (Node other : partners(cellX + ox, cellZ + oz)) {
                    // Each pair once, from its lower end.
                    if (!LinkRaster.lower(me.x, me.z, other.x, other.z)) {
                        continue;
                    }
                    Candidate c = new Candidate(me, other);
                    if (c.mayCross(minX, minZ, maxX, maxZ)) {
                        out.add(c);
                    }
                }
            }
        }
        out.sort(
            (p, q) -> p.a.x != q.a.x ? Integer.compare(p.a.x, q.a.x)
                : p.a.z != q.a.z ? Integer.compare(p.a.z, q.a.z)
                    : p.b.x != q.b.x ? Integer.compare(p.b.x, q.b.x) : Integer.compare(p.b.z, q.b.z));
        List<Candidate> built = Collections.unmodifiableList(out);
        PerfCounters.add("clg.buildIndex", perfIndex);
        return cellCandidates.putIfAbsent(key(cellX, cellZ), built);
    }

    /** Returns how many cell candidate lists were built, for tests. */
    int candidateBuilds() {
        return candidateBuilds.get();
    }

    private static long key(int x, int z) {
        return (long) x << 32 | z & 0xffffffffL;
    }

    /** An unordered pair of link ends, lower end first: the route cache's key. */
    private static final class Pair {

        private final int ax;
        private final int az;
        private final int bx;
        private final int bz;

        Pair(int ax, int az, int bx, int bz) {
            this.ax = ax;
            this.az = az;
            this.bx = bx;
            this.bz = bz;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Pair)) {
                return false;
            }
            Pair p = (Pair) o;
            return ax == p.ax && az == p.az && bx == p.bx && bz == p.bz;
        }

        @Override
        public int hashCode() {
            return ((ax * 31 + az) * 31 + bx) * 31 + bz;
        }
    }

    /**
     * A small access-ordered LRU, synchronized. Values are computed outside it and stored with {@link #putIfAbsent}, so
     * two threads that computed the same value at once both get the first one stored.
     */
    private static final class Lru<K, V> {

        private final LinkedHashMap<K, V> map;

        Lru(int capacity) {
            this.map = new LinkedHashMap<K, V>(16, 0.75f, true) {

                private static final long serialVersionUID = 1L;

                @Override
                protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                    return size() > capacity;
                }
            };
        }

        synchronized V get(K key) {
            return map.get(key);
        }

        /** Stores a value unless one is held, and returns the one held afterwards. */
        synchronized V putIfAbsent(K key, V value) {
            V held = map.get(key);
            if (held != null) {
                return held;
            }
            map.put(key, value);
            return value;
        }

        synchronized int size() {
            return map.size();
        }
    }
}
