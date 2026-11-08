package com.tuxprogrammer.lostcities.layout.link;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.tuxprogrammer.lostcities.layout.perf.PerfCounters;

/**
 * The city link graph of a grid-placed world ({@code networkLayout=links}, docs/design/0015-city-link-network.md §1).
 * Nodes are the grid's city centers that actually yield a city (the filtered mask). Each node links to its
 * {@link #NEAREST} nearest nodes within {@link #maxDistance()} chunks; a link exists when either end chose the other,
 * so links are symmetric. Every link is rasterized by {@link LinkRaster} into chunks; every highway and railway chunk
 * of the network lies on one, so following it either way reaches a city.
 *
 * <p>
 * Queries are per grid cell. A link through a chunk has both ends within {@link #maxDistance()} of it, so its ends lie
 * within {@link #reach()} cells of the chunk's cell; a cell's chunk index is built once from those cells' links and
 * kept in an LRU cache, as are each node's neighbor list and each cell's node. Everything is a function of the seed
 * and the {@link Nodes} oracle, so the answer does not depend on generation order. The oracle reads terrain oracles
 * only, never loads a chunk, so the graph cannot cascade.
 *
 * <p>
 * Work is shared and bounded (D-linkgraph-shrink-2, -3): a link's route is computed once per unordered pair of ends and
 * cached, a node's symmetric {@link #links} are cached, and every scan over neighboring cells skips the cells that
 * cannot hold a node or a path in range. None of this changes an answer: each skipped cell provably contributes
 * nothing, and each cached value is the one the computation returns.
 *
 * <p>
 * Side: server. One instance per dimension's layout, like the other planners. Thread-safe: the caches are synchronized
 * LRUs locked only around each read and write, never while a value is computed, and their values are immutable pure
 * functions of the key, so concurrent callers that compute the same value at once both return the first one stored.
 */
public final class CityLinkGraph {

    /** Links per node before the symmetric union (owner decision 8). */
    public static final int NEAREST = 3;
    /** Longest link, in chunks, on a grid of {@link #REFERENCE_CELL}-chunk cells (owner decision 8). */
    public static final int MAX_DISTANCE = 160;
    /**
     * The cell size {@link #MAX_DISTANCE} was set for. A grid with larger cells stretches the cap with them
     * (D-city-scale-1),
     * so the same neighbors stay in reach; a smaller one keeps the cap.
     */
    public static final int REFERENCE_CELL = 64;

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

    private static final int[] NONE = new int[0];
    /** The route cache's mark for a pair no path clears; never handed out. */
    private static final Link NO_ROUTE = new Link(0, 0, 0, 0);

    private final Nodes nodes;
    private final int cell;
    private final int reach;
    private final int maxDistance;
    private final Lru<Long, int[]> centers;
    private final Lru<Long, List<Link>> neighbors;
    private final Lru<Long, List<Link>> symmetric;
    private final Lru<Pair, Link> routes;
    private final Lru<Long, Map<Long, List<Hit>>> cellIndex;

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
        this.neighbors = new Lru<>(size * 2);
        this.symmetric = new Lru<>(size * 2);
        // About the routes the cached nodes asked for; most hold the very links the neighbor cache holds.
        this.routes = new Lru<>(size * 2 * NEAREST);
        // A cell's chunk index grows with the cell's area; keep about the same chunk area cached at every cell size.
        long scale = (long) cell * cell / ((long) REFERENCE_CELL * REFERENCE_CELL);
        this.cellIndex = new Lru<>((int) Math.max(16, size / 8 / Math.max(1, scale)));
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
     * Returns the links a cell's node chose: its {@link #NEAREST} nearest nodes within {@link #maxDistance()} to which
     * a
     * path, straight or detoured ({@link #route}), crosses no {@link Nodes#blocked} chunk, nearest first, ties to the
     * lower end; when every such path is blocked, the nearest alone. Empty when the cell has no node. Not symmetric;
     * see {@link #links}.
     *
     * @param cellX cell X
     * @param cellZ cell Z
     * @return the chosen links
     */
    public List<Link> chosen(int cellX, int cellZ) {
        List<Link> cached = neighbors.get(key(cellX, cellZ));
        if (cached != null) {
            return cached;
        }
        int[] self = node(cellX, cellZ);
        List<long[]> found = new ArrayList<>();
        if (self != null) {
            long max = (long) maxDistance * maxDistance;
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
                    long ddx = other[0] - self[0];
                    long ddz = other[1] - self[1];
                    long d2 = ddx * ddx + ddz * ddz;
                    if (d2 <= max) {
                        found.add(new long[] { d2, other[0], other[1] });
                    }
                }
            }
            found.sort((p, q) -> {
                int c = Long.compare(p[0], q[0]);
                if (c != 0) {
                    return c;
                }
                c = Long.compare(p[1], q[1]);
                return c != 0 ? c : Long.compare(p[2], q[2]);
            });
        }
        // Landmarks (0015 §6, D390): a link whose straight path crosses a blocked chunk detours around it through a
        // waypoint; only when no detour clears is it passed over for the next nearest. When every candidate is
        // blocked the node keeps its nearest, so no node is cut off.
        List<Link> out = new ArrayList<>();
        for (int i = 0; i < found.size() && out.size() < NEAREST; i++) {
            Link link = route(self[0], self[1], (int) found.get(i)[1], (int) found.get(i)[2]);
            if (link != null) {
                out.add(link);
            }
        }
        if (out.isEmpty() && !found.isEmpty()) {
            out.add(new Link(self[0], self[1], (int) found.get(0)[1], (int) found.get(0)[2]));
        }
        if (self != null && out.size() < 2 && com.tuxprogrammer.lostcities.layout.LayoutTrace.on()) {
            com.tuxprogrammer.lostcities.layout.LayoutTrace.log(
                "linkGraph node=%d,%d candidates=%d chosen=%d (every other path blocked, detours included)",
                self[0],
                self[1],
                found.size(),
                out.size());
        }
        return neighbors.putIfAbsent(key(cellX, cellZ), Collections.unmodifiableList(out));
    }

    /**
     * Returns the link between two centers along a path that crosses no {@link Nodes#blocked} chunk, or {@code null}:
     * the straight raster if it is clear, else the straight raster with only its blocked stretch replaced by a bounded
     * search around the landmarks ({@link DetourSearch}, D-perf2-route-2), every chunk within {@link #maxDistance()} of
     * both ends, so {@link #reach} still bounds every chunk of the path. A function of the two ends (taken lower first)
     * and the oracle only, so whichever end asks gets the same path; so it is computed once per unordered pair and
     * cached under the pair, lower end first (D-linkgraph-shrink-2).
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
        if (straight.ax == straight.bx && straight.az == straight.bz) {
            return null;
        }
        List<LinkRaster.Step> path = DetourSearch.detour(straight.path(), nodes::blocked, maxDistance);
        return path == null ? null : new Link(straight.ax, straight.az, straight.bx, straight.bz, path);
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
     * Returns every link at a cell's node, symmetric: the ones it chose and the ones that chose it. Sorted by far end.
     * Cached per cell, so the city highways, districts and scenarios that ask for the same node share one computation
     * (D-linkgraph-shrink-4).
     *
     * @param cellX cell X
     * @param cellZ cell Z
     * @return the links, unmodifiable; empty when the cell has no node
     */
    public List<Link> links(int cellX, int cellZ) {
        List<Link> cached = symmetric.get(key(cellX, cellZ));
        if (cached == null) {
            long perfLinks = PerfCounters.start();
            List<Link> computed = Collections.unmodifiableList(computeLinks(cellX, cellZ));
            PerfCounters.add("clg.links", perfLinks);
            cached = symmetric.putIfAbsent(key(cellX, cellZ), computed);
        }
        return cached;
    }

    private List<Link> computeLinks(int cellX, int cellZ) {
        int[] self = node(cellX, cellZ);
        List<Link> out = new ArrayList<>();
        if (self == null) {
            return out;
        }
        for (Link link : chosen(cellX, cellZ)) {
            addOnce(out, link);
        }
        for (int ox = -reach; ox <= reach; ox++) {
            for (int oz = -reach; oz <= reach; oz++) {
                // A node that chose this one lies within maxDistance of it, so a cell out of range chose nothing here.
                if (ox == 0 && oz == 0 || outOfRange(self[0], self[1], cellX + ox, cellZ + oz)) {
                    continue;
                }
                for (Link link : chosen(cellX + ox, cellZ + oz)) {
                    if (link.ax == self[0] && link.az == self[1] || link.bx == self[0] && link.bz == self[1]) {
                        addOnce(out, link);
                    }
                }
            }
        }
        out.sort(
            (p, q) -> p.ax != q.ax ? Integer.compare(p.ax, q.ax)
                : p.az != q.az ? Integer.compare(p.az, q.az)
                    : p.bx != q.bx ? Integer.compare(p.bx, q.bx) : Integer.compare(p.bz, q.bz));
        return out;
    }

    /**
     * Returns the link pieces in a chunk: every link whose path crosses it, in a fixed order. Empty off the network.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the hits
     */
    public List<Hit> hits(int chunkX, int chunkZ) {
        int cellX = Math.floorDiv(chunkX, cell);
        int cellZ = Math.floorDiv(chunkZ, cell);
        Map<Long, List<Hit>> index = cellIndex.get(key(cellX, cellZ));
        if (index == null) {
            long perfIndex = PerfCounters.start();
            Map<Long, List<Hit>> built = buildIndex(cellX, cellZ);
            PerfCounters.add("clg.buildIndex", perfIndex);
            index = cellIndex.putIfAbsent(key(cellX, cellZ), built);
        }
        List<Hit> hits = index.get(key(chunkX, chunkZ));
        return hits == null ? Collections.<Hit>emptyList() : hits;
    }

    private Map<Long, List<Hit>> buildIndex(int cellX, int cellZ) {
        int minX = cellX * cell;
        int minZ = cellZ * cell;
        int maxX = minX + cell - 1;
        int maxZ = minZ + cell - 1;
        List<Link> all = new ArrayList<>();
        // Every chunk of a path lies in the bounding box of its ends and waypoint, each within maxDistance of the
        // node that chose it, so per axis within maxDistance of that node: a cell whose box is farther than that from
        // this cell's box chose no link that crosses it (D-linkgraph-shrink-3). One chunk of slack on top.
        int cells = 1 + (maxDistance + 1) / cell;
        for (int ox = -reach; ox <= reach; ox++) {
            for (int oz = -reach; oz <= reach; oz++) {
                if (Math.abs(ox) > cells || Math.abs(oz) > cells) {
                    continue;
                }
                for (Link link : chosen(cellX + ox, cellZ + oz)) {
                    if (link.overlaps(minX, minZ, maxX, maxZ)) {
                        addOnce(all, link);
                    }
                }
            }
        }
        all.sort(
            (p, q) -> p.ax != q.ax ? Integer.compare(p.ax, q.ax)
                : p.az != q.az ? Integer.compare(p.az, q.az)
                    : p.bx != q.bx ? Integer.compare(p.bx, q.bx) : Integer.compare(p.bz, q.bz));
        Map<Long, List<Hit>> index = new LinkedHashMap<>();
        for (Link link : all) {
            List<LinkRaster.Step> path = link.path();
            for (int i = 0; i < path.size(); i++) {
                LinkRaster.Step step = path.get(i);
                if (step.chunkX() < minX || step.chunkX() > maxX || step.chunkZ() < minZ || step.chunkZ() > maxZ) {
                    continue;
                }
                index.computeIfAbsent(key(step.chunkX(), step.chunkZ()), k -> new ArrayList<>(2))
                    .add(new Hit(link, step, i));
            }
        }
        return index;
    }

    private static void addOnce(List<Link> list, Link link) {
        if (!list.contains(link)) {
            list.add(link);
        }
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
