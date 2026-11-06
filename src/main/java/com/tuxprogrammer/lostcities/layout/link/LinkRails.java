package com.tuxprogrammer.lostcities.layout.link;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The railways of the city link network ({@code networkLayout=links}, design record 0015 §3): one underground line
 * along every link of a {@link CityLinkGraph}, as the sides each chunk's track opens on.
 *
 * <p>
 * A line runs from one city's center chunk to the other's at the railway level. It leaves its lower end (lower X,
 * then Z) eastward and enters its upper end from the west, or from the east when both centers share an X, so every
 * center is entered along X, where its station stands. Between, it follows the link's octilinear path
 * ({@link LinkRaster}) with each 45° step split into a step along X, then one along Z: a chunk-scale staircase, so
 * every chunk opens only on sides and its track is a straight, a bend, a T or a crossing (0015 §3, D321). Where
 * lines share a chunk their openings join; a line never stops between its two cities.
 *
 * <p>
 * Landmark cores keep the railways out (0013 §5): a line any of whose chunks is {@link Blocked} carries no track at
 * all,
 * so no line ends at a core (D322).
 *
 * <p>
 * Pure: reads only the graph and the {@link Blocked} oracle, so it answers the same in any order. Per-cell indexes and
 * per-link results are cached (bounded). Side: both.
 */
public final class LinkRails {

    /** A side a chunk's track opens on. */
    public static final int NORTH = 1;
    /** A side a chunk's track opens on. */
    public static final int EAST = 2;
    /** A side a chunk's track opens on. */
    public static final int SOUTH = 4;
    /** A side a chunk's track opens on. */
    public static final int WEST = 8;
    /** All four sides. */
    public static final int SIDES = NORTH | EAST | SOUTH | WEST;
    /** Set in {@link #openings} on a city center that a line ends at: where the city's station stands. */
    public static final int END = 16;

    /** Answers whether a line may not pass a chunk at the railway level. */
    public interface Blocked {

        /**
         * Returns whether a line may not pass a chunk: a landmark core too shallow for the railway level.
         *
         * @param chunkX chunk X
         * @param chunkZ chunk Z
         * @return whether the chunk blocks a line
         */
        boolean blocked(int chunkX, int chunkZ);
    }

    private static final int CELLS = 64;
    private static final int LINKS = 4096;

    private final CityLinkGraph graph;
    private final Blocked blocked;
    private final Map<Long, Map<Long, Integer>> cells = lru(CELLS);
    private final Map<CityLinkGraph.Link, Boolean> live = lru(LINKS);

    /**
     * Builds the railways of a link graph.
     *
     * @param graph   the graph
     * @param blocked the chunks no line may pass
     */
    public LinkRails(CityLinkGraph graph, Blocked blocked) {
        this.graph = graph;
        this.blocked = blocked;
    }

    /** Returns the graph. */
    public CityLinkGraph graph() {
        return graph;
    }

    /**
     * Returns the chunks of a link's line in order, from its lower end's center to its upper end's center, each a
     * side neighbor of the one before (class Javadoc).
     *
     * @param link the link
     * @return the chunks as {x, z} pairs, both ends included
     */
    public static List<int[]> path(CityLinkGraph.Link link) {
        int ax = link.ax();
        int az = link.az();
        int bx = link.bx();
        int bz = link.bz();
        // The lower end has the lower X (or the same X): it leaves east; the upper end is entered from the side
        // facing the lower end along X, or from the east when both share an X.
        int px = ax + 1;
        int qx = bx > ax ? bx - 1 : bx + 1;
        List<int[]> out = new ArrayList<>();
        out.add(new int[] { ax, az });
        if (Math.abs(bx - ax) + Math.abs(bz - az) <= 2) {
            // Neighbors (never two grid cities): a plain step along X, then along Z.
            stepTo(out, bx, bz);
            return Collections.unmodifiableList(out);
        }
        List<LinkRaster.Step> raster = LinkRaster.rasterize(px, az, qx, bz);
        List<int[]> middle = new ArrayList<>(raster.size() * 2);
        for (int i = 0; i < raster.size(); i++) {
            LinkRaster.Step s = raster.get(i);
            if (i > 0) {
                LinkRaster.Step p = raster.get(i - 1);
                if (s.chunkX() != p.chunkX() && s.chunkZ() != p.chunkZ()) {
                    // A 45° step: along X first, in the raster's own order.
                    middle.add(new int[] { s.chunkX(), p.chunkZ() });
                }
            }
            middle.add(new int[] { s.chunkX(), s.chunkZ() });
        }
        LinkRaster.Step first = raster.get(0);
        if (first.chunkX() != px || first.chunkZ() != az) {
            Collections.reverse(middle);
        }
        out.addAll(middle);
        out.add(new int[] { bx, bz });
        return Collections.unmodifiableList(out);
    }

    private static void stepTo(List<int[]> out, int bx, int bz) {
        int[] at = out.get(out.size() - 1);
        int x = at[0];
        int z = at[1];
        while (x != bx) {
            x += Integer.signum(bx - x);
            out.add(new int[] { x, z });
        }
        while (z != bz) {
            z += Integer.signum(bz - z);
            out.add(new int[] { x, z });
        }
    }

    /**
     * Returns the side bit that leads from one chunk to its side neighbor.
     *
     * @param dx the step, X
     * @param dz the step, Z
     * @return the bit, or 0 when the step is not to a side neighbor
     */
    public static int side(int dx, int dz) {
        if (dx == 1 && dz == 0) {
            return EAST;
        }
        if (dx == -1 && dz == 0) {
            return WEST;
        }
        if (dx == 0 && dz == 1) {
            return SOUTH;
        }
        if (dx == 0 && dz == -1) {
            return NORTH;
        }
        return 0;
    }

    /**
     * Returns whether a link carries a line: no chunk of its path is {@link Blocked} and both its ends are cities
     * (the station hook, 0015 §7).
     *
     * @param link the link
     * @return whether it has track
     */
    public synchronized boolean live(CityLinkGraph.Link link) {
        Boolean cached = live.get(link);
        if (cached != null) {
            return cached;
        }
        boolean value = link.bothEndsCities();
        for (int[] c : value ? path(link) : Collections.<int[]>emptyList()) {
            if (blocked.blocked(c[0], c[1])) {
                value = false;
                break;
            }
        }
        live.put(link, value);
        return value;
    }

    /**
     * Returns the sides a chunk's track opens on, with {@link #END} on a city center a line ends at; 0 off the
     * network.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the bits
     */
    public synchronized int openings(int chunkX, int chunkZ) {
        int cell = graph.cell();
        int cellX = Math.floorDiv(chunkX, cell);
        int cellZ = Math.floorDiv(chunkZ, cell);
        long cellKey = key(cellX, cellZ);
        Map<Long, Integer> index = cells.get(cellKey);
        if (index == null) {
            index = buildIndex(cellX, cellZ);
            cells.put(cellKey, index);
        }
        Integer value = index.get(key(chunkX, chunkZ));
        return value == null ? 0 : value;
    }

    private Map<Long, Integer> buildIndex(int cellX, int cellZ) {
        int cell = graph.cell();
        int reach = graph.reach();
        // A line lies in its ends' box widened by one chunk along X.
        int minX = cellX * cell - 1;
        int minZ = cellZ * cell;
        int maxX = minX + cell + 1;
        int maxZ = minZ + cell - 1;
        List<CityLinkGraph.Link> all = new ArrayList<>();
        for (int ox = -reach; ox <= reach; ox++) {
            for (int oz = -reach; oz <= reach; oz++) {
                for (CityLinkGraph.Link link : graph.chosen(cellX + ox, cellZ + oz)) {
                    if (Math.max(link.ax(), link.bx()) >= minX && Math.min(link.ax(), link.bx()) <= maxX
                        && Math.max(link.az(), link.bz()) >= minZ
                        && Math.min(link.az(), link.bz()) <= maxZ
                        && !all.contains(link)) {
                        all.add(link);
                    }
                }
            }
        }
        Map<Long, Integer> index = new LinkedHashMap<>();
        int x0 = cellX * cell;
        int z0 = cellZ * cell;
        for (CityLinkGraph.Link link : all) {
            if (!live(link)) {
                continue;
            }
            List<int[]> path = path(link);
            for (int i = 0; i < path.size(); i++) {
                int[] c = path.get(i);
                if (c[0] < x0 || c[0] >= x0 + cell || c[1] < z0 || c[1] >= z0 + cell) {
                    continue;
                }
                int bits = 0;
                if (i > 0) {
                    int[] p = path.get(i - 1);
                    bits |= side(p[0] - c[0], p[1] - c[1]);
                }
                if (i + 1 < path.size()) {
                    int[] n = path.get(i + 1);
                    bits |= side(n[0] - c[0], n[1] - c[1]);
                }
                if (i == 0 || i == path.size() - 1) {
                    bits |= END;
                }
                index.merge(key(c[0], c[1]), bits, (a, b) -> a | b);
            }
        }
        return index;
    }

    private static long key(int x, int z) {
        return (long) x << 32 | (z & 0xffffffffL);
    }

    private static <K, V> Map<K, V> lru(int capacity) {
        return new LinkedHashMap<K, V>(64, 0.75f, true) {

            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > capacity;
            }
        };
    }
}
