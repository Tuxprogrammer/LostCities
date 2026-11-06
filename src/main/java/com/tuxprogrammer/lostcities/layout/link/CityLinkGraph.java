package com.tuxprogrammer.lostcities.layout.link;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The city link graph of a grid-placed world ({@code networkLayout=links}, docs/design/0015-city-link-network.md §1).
 * Nodes are the grid's city centers that actually yield a city (the filtered mask). Each node links to its
 * {@link #NEAREST} nearest nodes within {@link #MAX_DISTANCE} chunks; a link exists when either end chose the other,
 * so links are symmetric. Every link is rasterized by {@link LinkRaster} into chunks; every highway and railway chunk
 * of the network lies on one, so following it either way reaches a city.
 *
 * <p>
 * Queries are per grid cell. A link through a chunk has both ends within {@link #MAX_DISTANCE} of it, so its ends lie
 * within {@link #reach()} cells of the chunk's cell; a cell's chunk index is built once from those cells' links and
 * kept in an LRU cache, as are each node's neighbor list and each cell's node. Everything is a function of the seed
 * and the {@link Nodes} oracle, so the answer does not depend on generation order. The oracle reads terrain oracles
 * only, never loads a chunk, so the graph cannot cascade.
 *
 * <p>
 * Side: server. Not thread-safe; one instance per dimension's layout, like the other planners.
 */
public final class CityLinkGraph {

    /** Links per node before the symmetric union (owner decision 8). */
    public static final int NEAREST = 3;
    /** Longest link, in chunks (owner decision 8). */
    public static final int MAX_DISTANCE = 160;

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

    /** One link: two city centers, lower end first. */
    public static final class Link {

        private final int ax;
        private final int az;
        private final int bx;
        private final int bz;
        private List<LinkRaster.Step> path;

        Link(int ax, int az, int bx, int bz) {
            boolean lower = LinkRaster.lower(ax, az, bx, bz);
            this.ax = lower ? ax : bx;
            this.az = lower ? az : bz;
            this.bx = lower ? bx : ax;
            this.bz = lower ? bz : az;
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

        /** Returns the link's chunks, lower end first. */
        public List<LinkRaster.Step> path() {
            if (path == null) {
                path = LinkRaster.rasterize(ax, az, bx, bz);
            }
            return path;
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

    private final Nodes nodes;
    private final int cell;
    private final int reach;
    private final Lru<int[]> centers;
    private final Lru<List<Link>> neighbors;
    private final Lru<Map<Long, List<Hit>>> cellIndex;

    /**
     * Builds the graph over a node oracle.
     *
     * @param nodes     the grid's city centers
     * @param cacheSize cache capacity, in cells
     */
    public CityLinkGraph(Nodes nodes, int cacheSize) {
        this.nodes = nodes;
        this.cell = nodes.cell();
        // A center lies anywhere in its cell, so two centers MAX_DISTANCE apart are at most this many cells apart.
        this.reach = (MAX_DISTANCE + cell - 1) / cell + 1;
        int size = Math.max(64, cacheSize);
        this.centers = new Lru<>(size * 4);
        this.neighbors = new Lru<>(size * 2);
        this.cellIndex = new Lru<>(Math.max(16, size / 8));
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
        int[] cached = centers.get(cellX, cellZ);
        if (cached == null) {
            int[] center = nodes.cityCenter(cellX, cellZ);
            cached = centers.put(cellX, cellZ, center == null ? NONE : center);
        }
        return cached.length == 0 ? null : cached;
    }

    /**
     * Returns the links a cell's node chose: its {@link #NEAREST} nearest nodes within {@link #MAX_DISTANCE} whose path
     * crosses no {@link Nodes#blocked} chunk, nearest first, ties to the lower end; when every such path is blocked,
     * the
     * nearest alone. Empty when the cell has no node. Not symmetric; see {@link #links}.
     *
     * @param cellX cell X
     * @param cellZ cell Z
     * @return the chosen links
     */
    public List<Link> chosen(int cellX, int cellZ) {
        List<Link> cached = neighbors.get(cellX, cellZ);
        if (cached != null) {
            return cached;
        }
        int[] self = node(cellX, cellZ);
        List<long[]> found = new ArrayList<>();
        if (self != null) {
            long max = (long) MAX_DISTANCE * MAX_DISTANCE;
            for (int ox = -reach; ox <= reach; ox++) {
                for (int oz = -reach; oz <= reach; oz++) {
                    if (ox == 0 && oz == 0) {
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
        // Landmarks (0015 §6): a link whose path crosses a blocked chunk is passed over for the next nearest, unless
        // every candidate is blocked; then the node keeps its nearest, so no node is cut off.
        List<Link> out = new ArrayList<>();
        for (int i = 0; i < found.size() && out.size() < NEAREST; i++) {
            Link link = new Link(self[0], self[1], (int) found.get(i)[1], (int) found.get(i)[2]);
            if (!crossesBlocked(link)) {
                out.add(link);
            }
        }
        if (out.isEmpty() && !found.isEmpty()) {
            out.add(new Link(self[0], self[1], (int) found.get(0)[1], (int) found.get(0)[2]));
        }
        return neighbors.put(cellX, cellZ, Collections.unmodifiableList(out));
    }

    private boolean crossesBlocked(Link link) {
        for (LinkRaster.Step step : link.path()) {
            if (nodes.blocked(step.chunkX(), step.chunkZ())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns every link at a cell's node, symmetric: the ones it chose and the ones that chose it. Sorted by far end.
     *
     * @param cellX cell X
     * @param cellZ cell Z
     * @return the links; empty when the cell has no node
     */
    public List<Link> links(int cellX, int cellZ) {
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
                if (ox == 0 && oz == 0) {
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
        Map<Long, List<Hit>> index = cellIndex.get(cellX, cellZ);
        if (index == null) {
            index = cellIndex.put(cellX, cellZ, buildIndex(cellX, cellZ));
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
        for (int ox = -reach; ox <= reach; ox++) {
            for (int oz = -reach; oz <= reach; oz++) {
                for (Link link : chosen(cellX + ox, cellZ + oz)) {
                    if (Math.max(link.ax, link.bx) >= minX && Math.min(link.ax, link.bx) <= maxX
                        && Math.max(link.az, link.bz) >= minZ
                        && Math.min(link.az, link.bz) <= maxZ) {
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

    /** A small access-ordered LRU keyed by cell or chunk coordinates. */
    private static final class Lru<V> {

        private final LinkedHashMap<Long, V> map;

        Lru(int capacity) {
            this.map = new LinkedHashMap<Long, V>(16, 0.75f, true) {

                private static final long serialVersionUID = 1L;

                @Override
                protected boolean removeEldestEntry(Map.Entry<Long, V> eldest) {
                    return size() > capacity;
                }
            };
        }

        V get(int x, int z) {
            return map.get(key(x, z));
        }

        V put(int x, int z, V value) {
            map.put(key(x, z), value);
            return value;
        }
    }
}
