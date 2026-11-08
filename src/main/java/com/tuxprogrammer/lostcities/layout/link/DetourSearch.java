package com.tuxprogrammer.lostcities.layout.link;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Routes a link around the landmarks its straight raster crosses (0015 §6, D390, D-perf2-route-2). The straight
 * raster is kept up to {@link #MARGIN} chunks before its first blocked chunk and from {@link #MARGIN} chunks after its
 * last; only the stretch between is replaced, by a bounded A* search over chunks. When that window finds no path the
 * whole link is searched once more from end to end; when that fails too, or the search spends
 * {@link #MAX_EXPANSIONS} expansions in all, the link has no route.
 *
 * <p>
 * The search moves between 8-neighbors and turns at most 45° per chunk, also where it joins the kept raster, so the
 * path is built of a plain link's straight, diagonal and transition pieces. Every chunk it visits lies within
 * {@code maxDistance} of both ends, so the graph's cell reach still bounds the path, and within {@link #CORRIDOR}
 * chunks of the straight line, which bounds the ground (and so the landmark queries) one route reads. A move costs 10
 * along an axis, 14 diagonally and {@link #TURN} more where it turns; the heuristic is the octile distance to the
 * window's end, admissible and consistent, so the path found is a cheapest one. Ties break by the queue's total order
 * (cost, then remaining distance, then chunk X, chunk Z and heading), so the path is a pure function of the two ends
 * (lower first) and the blocked test, in any generation order.
 *
 * <p>
 * Pure: no world access, no shared state. Side: server.
 */
final class DetourSearch {

    /** Chunks of the straight raster kept free for the search on either side of the blocked stretch. */
    static final int MARGIN = 16;
    /** Farthest a detour strays from the straight line, in chunks: the old detours' widest waypoint offset. */
    static final int CORRIDOR = 64;
    /** Most search expansions one route may spend over both windows before it fails. */
    static final int MAX_EXPANSIONS = 50_000;
    /** Extra cost of a move that turns 45°: keeps the path from zigzagging between equal-length shapes. */
    static final int TURN = 3;

    private static final int SIDE = 10;
    private static final int DIAGONAL = 14;
    /** The heading of a search node that has not moved yet. */
    private static final int NONE = 8;
    /** The eight headings, counterclockwise from +X; adjacent indices differ by 45°. */
    private static final int[] DX = { 1, 1, 0, -1, -1, -1, 0, 1 };
    private static final int[] DZ = { 0, 1, 1, 1, 0, -1, -1, -1 };

    /** Whether a link may not cross a chunk. */
    interface Blocked {

        /**
         * Returns whether the chunk is blocked.
         *
         * @param chunkX chunk X
         * @param chunkZ chunk Z
         * @return whether a link must not cross it
         */
        boolean test(int chunkX, int chunkZ);
    }

    private final List<LinkRaster.Step> straight;
    private final Blocked blocked;
    private final long maxSquared;
    private final int ax;
    private final int az;
    private final int bx;
    private final int bz;
    private final long lineX;
    private final long lineZ;
    private final long corridorSquared;
    private final Map<Long, Boolean> memo = new HashMap<>();
    private int budget = MAX_EXPANSIONS;
    private int expansions;

    /**
     * Prepares a search; {@link #find} runs it.
     *
     * @param straight    the link's straight raster, lower end first
     * @param blocked     the blocked test
     * @param maxDistance the farthest any chunk of the path may lie from either end
     */
    DetourSearch(List<LinkRaster.Step> straight, Blocked blocked, int maxDistance) {
        this.straight = straight;
        this.blocked = blocked;
        this.maxSquared = (long) maxDistance * maxDistance;
        LinkRaster.Step first = straight.get(0);
        LinkRaster.Step last = straight.get(straight.size() - 1);
        this.ax = first.chunkX();
        this.az = first.chunkZ();
        this.bx = last.chunkX();
        this.bz = last.chunkZ();
        this.lineX = bx - ax;
        this.lineZ = bz - az;
        this.corridorSquared = (long) CORRIDOR * CORRIDOR * (lineX * lineX + lineZ * lineZ);
    }

    /**
     * Returns a path between a link's ends that crosses no blocked chunk, or {@code null}.
     *
     * @param straight    the link's straight raster, lower end first
     * @param blocked     the blocked test
     * @param maxDistance the farthest any chunk of the path may lie from either end
     * @return the path, lower end first, or {@code null} when none is found within the bounds
     */
    static List<LinkRaster.Step> detour(List<LinkRaster.Step> straight, Blocked blocked, int maxDistance) {
        return new DetourSearch(straight, blocked, maxDistance).find();
    }

    /**
     * Runs the search once.
     *
     * @return the path, lower end first, or {@code null} when none is found within the bounds
     */
    List<LinkRaster.Step> find() {
        return search();
    }

    /** Returns how many nodes the search expanded, never more than {@link #MAX_EXPANSIONS}. */
    int expansions() {
        return expansions;
    }

    private List<LinkRaster.Step> search() {
        int n = straight.size();
        if (n < 2 || isBlocked(ax, az) || isBlocked(bx, bz)) {
            return null;
        }
        int first = -1;
        int last = -1;
        for (int i = 0; i < n; i++) {
            LinkRaster.Step step = straight.get(i);
            if (isBlocked(step.chunkX(), step.chunkZ())) {
                if (first < 0) {
                    first = i;
                }
                last = i;
            }
        }
        if (first < 0) {
            return straight;
        }
        int s = Math.max(0, first - MARGIN);
        int e = Math.min(n - 1, last + MARGIN);
        List<LinkRaster.Step> path = window(s, e);
        if (path == null && (s > 0 || e < n - 1)) {
            path = window(0, n - 1);
        }
        return path;
    }

    /** Searches the stretch from step s to step e and joins it to the kept raster, or returns {@code null}. */
    private List<LinkRaster.Step> window(int s, int e) {
        int n = straight.size();
        Set<Long> kept = new HashSet<>();
        for (int i = 0; i < s; i++) {
            kept.add(key(straight.get(i)));
        }
        for (int i = e + 1; i < n; i++) {
            kept.add(key(straight.get(i)));
        }
        LinkRaster.Step from = straight.get(s);
        LinkRaster.Step to = straight.get(e);
        int startHeading = s == 0 ? NONE : heading(from.inDx(), from.inDz());
        int endHeading = e == n - 1 ? NONE : heading(to.outDx(), to.outDz());
        List<int[]> middle = astar(from.chunkX(), from.chunkZ(), startHeading, to.chunkX(), to.chunkZ(), endHeading, kept);
        if (middle == null) {
            return null;
        }
        List<int[]> moves = new ArrayList<>(s + middle.size() + n - e);
        for (int i = 0; i < s; i++) {
            moves.add(new int[] { straight.get(i).outDx(), straight.get(i).outDz() });
        }
        moves.addAll(middle);
        for (int i = e; i < n - 1; i++) {
            moves.add(new int[] { straight.get(i).outDx(), straight.get(i).outDz() });
        }
        List<LinkRaster.Step> path = LinkRaster.fromMoves(ax, az, moves);
        Set<Long> seen = new HashSet<>();
        for (LinkRaster.Step step : path) {
            if (!seen.add(key(step))) {
                return null;
            }
        }
        return path;
    }

    /** One search node: a chunk reached with a heading, its cost so far and the node it came from. */
    private static final class Node {

        final int x;
        final int z;
        final int heading;
        final int g;
        final int f;
        final int h;
        final Node parent;

        Node(int x, int z, int heading, int g, int h, Node parent) {
            this.x = x;
            this.z = z;
            this.heading = heading;
            this.g = g;
            this.h = h;
            this.f = g + h;
            this.parent = parent;
        }
    }

    private static int compare(Node p, Node q) {
        int c = Integer.compare(p.f, q.f);
        if (c != 0) {
            return c;
        }
        c = Integer.compare(p.h, q.h);
        if (c != 0) {
            return c;
        }
        c = Integer.compare(p.x, q.x);
        if (c != 0) {
            return c;
        }
        c = Integer.compare(p.z, q.z);
        return c != 0 ? c : Integer.compare(p.heading, q.heading);
    }

    /**
     * The cheapest moves from a chunk entered with one heading to a chunk left with another ({@link #NONE} at a link
     * end), avoiding blocked and kept chunks and the ground out of bounds; {@code null} when the budget runs out first
     * or no path exists.
     */
    private List<int[]> astar(int sx, int sz, int startHeading, int gx, int gz, int endHeading, Set<Long> kept) {
        PriorityQueue<Node> open = new PriorityQueue<>(256, DetourSearch::compare);
        Map<Long, Integer> best = new HashMap<>();
        open.add(new Node(sx, sz, startHeading, 0, octile(sx, sz, gx, gz), null));
        best.put(state(sx, sz, startHeading), 0);
        while (!open.isEmpty()) {
            Node node = open.poll();
            Integer known = best.get(state(node.x, node.z, node.heading));
            if (known != null && known < node.g) {
                continue;
            }
            if (node.x == gx && node.z == gz) {
                return moves(node);
            }
            if (budget-- <= 0) {
                return null;
            }
            expansions++;
            int[] headings = node.heading == NONE ? ALL
                : new int[] { (node.heading + 7) & 7, node.heading, (node.heading + 1) & 7 };
            for (int heading : headings) {
                int nx = node.x + DX[heading];
                int nz = node.z + DZ[heading];
                if (nx == gx && nz == gz) {
                    if (endHeading != NONE && !gentle(heading, endHeading)) {
                        continue;
                    }
                } else if (!open(nx, nz, kept)) {
                    continue;
                }
                int g = node.g + ((heading & 1) == 0 ? SIDE : DIAGONAL)
                    + (node.heading != NONE && heading != node.heading ? TURN : 0);
                long state = state(nx, nz, heading);
                Integer seen = best.get(state);
                if (seen != null && seen <= g) {
                    continue;
                }
                best.put(state, g);
                open.add(new Node(nx, nz, heading, g, octile(nx, nz, gx, gz), node));
            }
        }
        return null;
    }

    private static final int[] ALL = { 0, 1, 2, 3, 4, 5, 6, 7 };

    /** Whether the search may enter a chunk short of its goal: in bounds, not kept, not blocked. */
    private boolean open(int x, int z, Set<Long> kept) {
        long da = (long) (x - ax) * (x - ax) + (long) (z - az) * (z - az);
        long db = (long) (x - bx) * (x - bx) + (long) (z - bz) * (z - bz);
        if (da > maxSquared || db > maxSquared) {
            return false;
        }
        long cross = (x - ax) * lineZ - (z - az) * lineX;
        if (cross * cross > corridorSquared) {
            return false;
        }
        return !kept.contains(key(x, z)) && !isBlocked(x, z);
    }

    private boolean isBlocked(int x, int z) {
        Long k = key(x, z);
        Boolean cached = memo.get(k);
        if (cached == null) {
            cached = blocked.test(x, z);
            memo.put(k, cached);
        }
        return cached;
    }

    /** Whether two headings differ by at most 45°: a turn a link's pieces can make. */
    private static boolean gentle(int a, int b) {
        int d = (a - b) & 7;
        return d == 0 || d == 1 || d == 7;
    }

    private static List<int[]> moves(Node goal) {
        List<int[]> out = new ArrayList<>();
        for (Node node = goal; node.parent != null; node = node.parent) {
            out.add(new int[] { DX[node.heading], DZ[node.heading] });
        }
        Collections.reverse(out);
        return out;
    }

    private static int octile(int x, int z, int gx, int gz) {
        int dx = Math.abs(gx - x);
        int dz = Math.abs(gz - z);
        return SIDE * Math.max(dx, dz) + (DIAGONAL - SIDE) * Math.min(dx, dz);
    }

    private static int heading(int dx, int dz) {
        for (int i = 0; i < 8; i++) {
            if (DX[i] == dx && DZ[i] == dz) {
                return i;
            }
        }
        throw new IllegalArgumentException("not a move: " + dx + "," + dz);
    }

    private static long key(LinkRaster.Step step) {
        return key(step.chunkX(), step.chunkZ());
    }

    private static long key(int x, int z) {
        return (long) x << 32 | z & 0xffffffffL;
    }

    private static long state(int x, int z, int heading) {
        return key(x, z) * 9 + heading;
    }
}
