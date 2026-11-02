package com.tuxprogrammer.lostcities.layout.highway;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Lays a route's chosen shape around landmark obstacles (new-spec 06 "R: route geometry", D31 P06-1, alpha review
 * AR-10, design record 0009). The shape is walked chunk by chunk from the first hub to the second. Where it meets an
 * obstacle (a core or its margin), the obstacle's extent is the bounding box of the obstacle chunks joined to the one
 * met (eight-neighbors, within {@link HighwayPlanner#OBSTACLE_WINDOW} chunks):
 * <ol>
 * <li><b>Detour:</b> where the shape runs straight through the box, it leaves its line on the chunk before the box,
 * runs parallel one chunk past the box on the nearer side (ties: north, then west), moving one chunk further out if
 * that row or column carries a railway, and rejoins its line on the chunk after the box: two legs and a parallel run,
 * with bends at the four corners and the route's level throughout.</li>
 * <li><b>Dead end:</b> where no detour fits (the shape turns inside the box, the box touches a hub or the window,
 * the offset exceeds {@link HighwayPlanner#MAX_DETOUR_OFFSET}, or the detour meets another obstacle), the run ends on
 * the chunk before the box and a new run starts on the first chunk past it. Both ends become plazas reached by ramps.
 * A run of a single chunk is dropped.</li>
 * </ol>
 *
 * <p>
 * Pure: obstacles come from {@link HighwayObstacles}, levels from {@link HighwayTerrain}. Side: both.
 */
final class RouteGeometry {

    private final HighwayPlanner planner;
    private final Hub first;
    private final Hub second;
    private final List<Run> runs = new ArrayList<>();
    private int detours;

    /**
     * Lays out a route.
     *
     * @param planner the planner, for its obstacles, levels and railway rules
     * @param first   the first hub, where the shape starts
     * @param second  the second hub, where it ends
     * @param shape   the chosen shape's segments, in order
     */
    RouteGeometry(HighwayPlanner planner, Hub first, Hub second, List<Segment> shape) {
        this.planner = planner;
        this.first = first;
        this.second = second;
        lay(shape);
    }

    /** Returns the runs. */
    List<Run> runs() {
        return runs;
    }

    /** Returns how many detours were taken. */
    int detours() {
        return detours;
    }

    private void lay(List<Segment> shape) {
        List<int[]> path = new ArrayList<>();
        List<Boolean> blocked = new ArrayList<>();
        boolean any = false;
        for (Segment segment : shape) {
            HighwayObstacles.Area area = planner.obstacles()
                .area(
                    Math.min(segment.startX(), segment.endX()),
                    Math.min(segment.startZ(), segment.endZ()),
                    Math.max(segment.startX(), segment.endX()),
                    Math.max(segment.startZ(), segment.endZ()));
            int stepX = segment.axis() == Axis.X ? segment.direction() : 0;
            int stepZ = segment.axis() == Axis.Z ? segment.direction() : 0;
            for (int i = path.isEmpty() ? 0 : 1; i <= segment.length(); i++) {
                int x = segment.startX() + stepX * i;
                int z = segment.startZ() + stepZ * i;
                boolean hit = area.blocked(x, z);
                any |= hit;
                path.add(new int[] { x, z });
                blocked.add(hit);
            }
        }
        if (!any) {
            runs.add(new Run(shape, hubEnd(first), hubEnd(second)));
            return;
        }
        walk(path, blocked);
    }

    private void walk(List<int[]> path, List<Boolean> blocked) {
        int n = path.size();
        List<int[]> current = new ArrayList<>();
        current.add(path.get(0));
        Run.End start = hubEnd(first);
        int i = 1;
        while (i < n) {
            if (!blocked.get(i)) {
                current.add(path.get(i));
                i++;
                continue;
            }
            int[] box = cluster(path.get(i));
            int j = i;
            if (box != null) {
                while (j < n && inside(box, path.get(j))) {
                    j++;
                }
            }
            List<int[]> detour = box == null ? null : detour(path, current, i, j, box);
            if (detour != null) {
                current.addAll(detour);
                detours++;
                i = j + 1;
                continue;
            }
            // Dead end: this run ends on the chunk before the obstacle.
            finish(current, start, deadEnd(current.get(current.size() - 1)));
            while (j < n && blocked.get(j)) {
                j++;
            }
            if (j >= n) {
                return;
            }
            current = new ArrayList<>();
            current.add(path.get(j));
            start = deadEnd(path.get(j));
            i = j + 1;
        }
        finish(current, start, hubEnd(second));
    }

    private void finish(List<int[]> chunks, Run.End start, Run.End end) {
        if (chunks.size() < 2) {
            return;
        }
        runs.add(new Run(segmentsOf(chunks), start, end));
    }

    /**
     * Returns the chunks of a detour around a box, from the first leg's chunk after the leaving corner to the rejoining
     * corner on the line, or {@code null} when none fits.
     */
    private List<int[]> detour(List<int[]> path, List<int[]> current, int i, int j, int[] box) {
        int n = path.size();
        if (current.size() < 2 || j > n - 2 || i < 2) {
            return null;
        }
        int[] before = path.get(i - 1);
        int[] after = path.get(j);
        boolean alongX = before[1] == after[1];
        if (!alongX && before[0] != after[0]) {
            return null;
        }
        // The line must run straight through the box, from the chunk before the corner to the one after the rejoin.
        for (int k = i - 2; k <= j + 1; k++) {
            int[] c = path.get(k);
            if (alongX ? c[1] != before[1] : c[0] != before[0]) {
                return null;
            }
        }
        int[] last = current.get(current.size() - 1);
        int[] previous = current.get(current.size() - 2);
        if (alongX ? previous[1] != last[1] : previous[0] != last[0]) {
            return null;
        }
        int line = alongX ? before[1] : before[0];
        int nearSide = (alongX ? box[1] : box[0]) - 1;
        int farSide = (alongX ? box[3] : box[2]) + 1;
        boolean low = line - nearSide <= farSide - line;
        int offsetLine = low ? nearSide : farSide;
        boolean railway = alongX ? planner.isHorizontalRailwayCorridor(offsetLine)
            : planner.isVerticalRailwayOrStationCorridor(offsetLine);
        if (railway) {
            offsetLine += low ? -1 : 1;
        }
        if (Math.abs(offsetLine - line) > HighwayPlanner.MAX_DETOUR_OFFSET) {
            return null;
        }
        int step = Integer.signum(offsetLine - line);
        int along = alongX ? Integer.signum(after[0] - before[0]) : Integer.signum(after[1] - before[1]);
        List<int[]> out = new ArrayList<>();
        int beforeAlong = alongX ? before[0] : before[1];
        int afterAlong = alongX ? after[0] : after[1];
        for (int o = line + step; o != offsetLine + step; o += step) {
            out.add(point(alongX, beforeAlong, o));
        }
        for (int a = beforeAlong + along; a != afterAlong + along; a += along) {
            out.add(point(alongX, a, offsetLine));
        }
        for (int o = offsetLine - step; o != line - step; o -= step) {
            out.add(point(alongX, afterAlong, o));
        }
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (int[] c : out) {
            minX = Math.min(minX, c[0]);
            minZ = Math.min(minZ, c[1]);
            maxX = Math.max(maxX, c[0]);
            maxZ = Math.max(maxZ, c[1]);
        }
        HighwayObstacles.Area area = planner.obstacles()
            .area(minX, minZ, maxX, maxZ);
        for (int[] c : out) {
            if (area.blocked(c[0], c[1])) {
                return null;
            }
        }
        return out;
    }

    private static int[] point(boolean alongX, int along, int across) {
        return alongX ? new int[] { along, across } : new int[] { across, along };
    }

    /**
     * Returns the bounding box {minX, minZ, maxX, maxZ} of the obstacle chunks eight-connected to a chunk, or
     * {@code null} when they reach the edge of the window around it.
     */
    private int[] cluster(int[] seed) {
        int window = HighwayPlanner.OBSTACLE_WINDOW;
        int originX = seed[0] - window;
        int originZ = seed[1] - window;
        int side = 2 * window + 1;
        HighwayObstacles.Area area = planner.obstacles()
            .area(originX, originZ, originX + side - 1, originZ + side - 1);
        boolean[] seen = new boolean[side * side];
        Deque<int[]> queue = new ArrayDeque<>();
        queue.add(seed);
        seen[window * side + window] = true;
        int[] box = { seed[0], seed[1], seed[0], seed[1] };
        while (!queue.isEmpty()) {
            int[] c = queue.poll();
            int lx = c[0] - originX;
            int lz = c[1] - originZ;
            if (lx == 0 || lz == 0 || lx == side - 1 || lz == side - 1) {
                return null;
            }
            box[0] = Math.min(box[0], c[0]);
            box[1] = Math.min(box[1], c[1]);
            box[2] = Math.max(box[2], c[0]);
            box[3] = Math.max(box[3], c[1]);
            for (int dz = -1; dz <= 1; dz++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int nx = lx + dx;
                    int nz = lz + dz;
                    int index = nz * side + nx;
                    if (seen[index]) {
                        continue;
                    }
                    seen[index] = true;
                    if (area.blocked(originX + nx, originZ + nz)) {
                        queue.add(new int[] { originX + nx, originZ + nz });
                    }
                }
            }
        }
        return box;
    }

    private static boolean inside(int[] box, int[] c) {
        return c[0] >= box[0] && c[0] <= box[2] && c[1] >= box[1] && c[1] <= box[3];
    }

    /** Splits a chain of neighboring chunks into straight segments, a corner wherever the direction turns. */
    static List<Segment> segmentsOf(List<int[]> chunks) {
        List<Segment> out = new ArrayList<>();
        int startIndex = 0;
        for (int k = 2; k <= chunks.size(); k++) {
            boolean turn = k == chunks.size() || direction(chunks, k) != direction(chunks, startIndex + 1);
            if (turn) {
                int[] s = chunks.get(startIndex);
                int[] e = chunks.get(k - 1);
                out.add(new Segment(s[0], s[1], e[0], e[1], s[1] == e[1] ? Axis.X : Axis.Z));
                startIndex = k - 1;
            }
        }
        return out;
    }

    /** The step into chunk {@code k} from the one before it: 0 east, 1 west, 2 south, 3 north. */
    private static int direction(List<int[]> chunks, int k) {
        int[] a = chunks.get(k - 1);
        int[] b = chunks.get(k);
        if (b[0] != a[0]) {
            return b[0] > a[0] ? 0 : 1;
        }
        return b[1] > a[1] ? 2 : 3;
    }

    private static Run.End hubEnd(Hub hub) {
        return new Run.End(Run.EndKind.HUB, hub.chunkX(), hub.chunkZ(), hub.cityLevel());
    }

    private Run.End deadEnd(int[] c) {
        return new Run.End(
            Run.EndKind.DEAD_END,
            c[0],
            c[1],
            planner.terrain()
                .cityLevel(c[0], c[1]));
    }
}
