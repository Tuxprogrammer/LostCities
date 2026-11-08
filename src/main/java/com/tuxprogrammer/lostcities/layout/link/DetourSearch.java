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
 * The search is resumable (D-perf2-worker-1): {@link #advance} runs it a bounded number of units at a time, and may be
 * called again after the blocked test threw, so the plan worker can route a link in steps that each hold the plan lock
 * briefly. {@link #find} runs the same steps unbounded, so a stepped search ends with the same path.
 *
 * <p>
 * Pure: no world access, no shared state. Not thread-safe: one search is advanced by one thread at a time (its owner,
 * {@code CityLinkGraph.RouteTask}, serializes it). Side: server.
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
    private static final int SCAN = 0;
    private static final int SEARCH = 1;
    private static final int DONE = 2;
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
    /** Where the search is: the opening scan, a window's A* search, or finished. */
    private int phase = SCAN;
    private boolean endsTested;
    private int scanIndex;
    private int first = -1;
    private int last = -1;
    private Window window;
    private List<LinkRaster.Step> result;
    private long work;

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
     * Runs the search to its end in one call.
     *
     * @return the path, lower end first, or {@code null} when none is found within the bounds
     */
    List<LinkRaster.Step> find() {
        while (!advance(Integer.MAX_VALUE)) {
            // advance returns false only when its work bound is reached
        }
        return result;
    }

    /**
     * Advances the search by at most {@code maxWork} units of work, one per raster chunk the opening scan tests and one
     * per node the A* search expands (D-perf2-worker-1). The plan worker calls it in bounded steps so a route never
     * holds the plan lock for long; {@link #find} is the same loop without a bound, so a search advanced in any number
     * of steps of any sizes ends with exactly the path {@link #find} returns.
     *
     * <p>
     * Safe to call again after the blocked test threw (a probe's miss, or any other error): a unit tests every chunk it
     * needs before it changes any state, and each test's answer is memoized only once it returns, so a throw leaves the
     * search as it was before that unit, and the next call repeats the unit.
     *
     * @param maxWork the most units to spend in this call, at least 1
     * @return {@code true} when the search has finished ({@link #result()})
     */
    boolean advance(int maxWork) {
        int spent = 0;
        while (phase != DONE) {
            if (spent >= maxWork) {
                return false;
            }
            if (phase == SCAN) {
                scanStep();
                spent++;
                work++;
            } else if (searchStep()) {
                spent++;
                work++;
            }
        }
        return true;
    }

    /** Returns whether the search has finished. */
    boolean done() {
        return phase == DONE;
    }

    /** Returns the path found, or {@code null} when there is none or the search has not finished. */
    List<LinkRaster.Step> result() {
        return result;
    }

    /** Returns how many units of work {@link #advance} has spent in all. */
    long work() {
        return work;
    }

    /** Returns how many nodes the search expanded, never more than {@link #MAX_EXPANSIONS}. */
    int expansions() {
        return expansions;
    }

    /**
     * One unit of the opening scan: the two ends first (either blocked, or a raster of one chunk, means no route), then
     * one raster chunk, then the window around the blocked stretch, or the straight raster when nothing is blocked.
     */
    private void scanStep() {
        int n = straight.size();
        if (!endsTested) {
            if (n < 2 || isBlocked(ax, az) || isBlocked(bx, bz)) {
                finish(null);
                return;
            }
            endsTested = true;
            return;
        }
        if (scanIndex < n) {
            LinkRaster.Step step = straight.get(scanIndex);
            if (isBlocked(step.chunkX(), step.chunkZ())) {
                if (first < 0) {
                    first = scanIndex;
                }
                last = scanIndex;
            }
            scanIndex++;
            return;
        }
        if (first < 0) {
            finish(straight);
            return;
        }
        window = new Window(Math.max(0, first - MARGIN), Math.min(n - 1, last + MARGIN));
        phase = SEARCH;
    }

    /**
     * One pass of the current window's A* loop: drops a stale queue entry, ends the window at its goal or when the
     * budget is spent, or expands one node.
     *
     * @return {@code true} when a node was expanded (one unit of work)
     */
    private boolean searchStep() {
        Window w = window;
        if (w.open.isEmpty()) {
            windowFailed(w);
            return false;
        }
        Node node = w.open.peek();
        Integer known = w.best.get(state(node.x, node.z, node.heading));
        if (known != null && known < node.g) {
            w.open.poll();
            return false;
        }
        if (node.x == w.gx && node.z == w.gz) {
            w.open.poll();
            windowFound(w, moves(node));
            return false;
        }
        if (budget <= 0) {
            budget--;
            windowFailed(w);
            return false;
        }
        int[] headings = node.heading == NONE ? ALL
            : new int[] { (node.heading + 7) & 7, node.heading, (node.heading + 1) & 7 };
        // Every blocked test this expansion needs, before anything changes: a throw here leaves the state as it was.
        for (int heading : headings) {
            int nx = node.x + DX[heading];
            int nz = node.z + DZ[heading];
            if (nx != w.gx || nz != w.gz) {
                open(nx, nz, w.kept);
            }
        }
        w.open.poll();
        budget--;
        expansions++;
        for (int heading : headings) {
            int nx = node.x + DX[heading];
            int nz = node.z + DZ[heading];
            if (nx == w.gx && nz == w.gz) {
                if (w.endHeading != NONE && !gentle(heading, w.endHeading)) {
                    continue;
                }
            } else if (!open(nx, nz, w.kept)) {
                continue;
            }
            int g = node.g + ((heading & 1) == 0 ? SIDE : DIAGONAL)
                + (node.heading != NONE && heading != node.heading ? TURN : 0);
            long state = state(nx, nz, heading);
            Integer seen = w.best.get(state);
            if (seen != null && seen <= g) {
                continue;
            }
            w.best.put(state, g);
            w.open.add(new Node(nx, nz, heading, g, octile(nx, nz, w.gx, w.gz), node));
        }
        return true;
    }

    /** Joins a window's moves to the kept raster; a join that revisits a chunk fails the window. */
    private void windowFound(Window w, List<int[]> middle) {
        int n = straight.size();
        List<int[]> moves = new ArrayList<>(w.s + middle.size() + n - w.e);
        for (int i = 0; i < w.s; i++) {
            moves.add(
                new int[] { straight.get(i)
                    .outDx(),
                    straight.get(i)
                        .outDz() });
        }
        moves.addAll(middle);
        for (int i = w.e; i < n - 1; i++) {
            moves.add(
                new int[] { straight.get(i)
                    .outDx(),
                    straight.get(i)
                        .outDz() });
        }
        List<LinkRaster.Step> path = LinkRaster.fromMoves(ax, az, moves);
        Set<Long> seen = new HashSet<>();
        for (LinkRaster.Step step : path) {
            if (!seen.add(key(step))) {
                windowFailed(w);
                return;
            }
        }
        finish(path);
    }

    /** A window found no path: the whole link is searched once more, unless that was the whole link. */
    private void windowFailed(Window w) {
        int n = straight.size();
        if (w.s > 0 || w.e < n - 1) {
            window = new Window(0, n - 1);
        } else {
            finish(null);
        }
    }

    private void finish(List<LinkRaster.Step> path) {
        result = path;
        window = null;
        phase = DONE;
    }

    /**
     * The A* state of one window: the stretch from step s to step e, searched from the chunk at s entered with the
     * raster's heading there to the chunk at e left with the raster's heading there ({@link #NONE} at a link end),
     * avoiding blocked and kept chunks and the ground out of bounds.
     */
    private final class Window {

        final int s;
        final int e;
        final Set<Long> kept = new HashSet<>();
        final int gx;
        final int gz;
        final int endHeading;
        final PriorityQueue<Node> open = new PriorityQueue<>(256, DetourSearch::compare);
        final Map<Long, Integer> best = new HashMap<>();

        Window(int s, int e) {
            int n = straight.size();
            this.s = s;
            this.e = e;
            for (int i = 0; i < s; i++) {
                kept.add(key(straight.get(i)));
            }
            for (int i = e + 1; i < n; i++) {
                kept.add(key(straight.get(i)));
            }
            LinkRaster.Step from = straight.get(s);
            LinkRaster.Step to = straight.get(e);
            int startHeading = s == 0 ? NONE : heading(from.inDx(), from.inDz());
            this.endHeading = e == n - 1 ? NONE : heading(to.outDx(), to.outDz());
            this.gx = to.chunkX();
            this.gz = to.chunkZ();
            int sx = from.chunkX();
            int sz = from.chunkZ();
            open.add(new Node(sx, sz, startHeading, 0, octile(sx, sz, gx, gz), null));
            best.put(state(sx, sz, startHeading), 0);
        }
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
