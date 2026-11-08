package com.tuxprogrammer.lostcities.asset.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stretches a building floor part to a taller floor pitch by repeating one of its rows (design record 0014). Upstream
 * floors are 6 blocks; a world pinned at pitch 8 places every floor and cellar part with its repeat row written 3
 * times, so the clear height of each room grows by 2 and ladders, walls and windows that cross the row grow with it.
 *
 * <p>
 * The repeat row is the row from 1 to {@code height − 2} with the most spaces, the highest on a tie: a mid-room row,
 * never the floor slab or the top ceiling slab. Diagonal stair runs (the same character climbing one row per block
 * along X or Z, at least {@value #MIN_STAIR_STEPS} steps, each with a space above) that cross the repeat row would
 * otherwise split into two runs joined by a vertical wall of stairs; they are re-laid instead as one longer run that
 * still ends at the stretched top step and starts on the same floor row, {@code extra} blocks further back, with the
 * run's support character put under the new steps. When the longer run does not fit (it would leave the part or
 * overwrite a block) the part keeps the plain stretch.
 *
 * <p>
 * A run that leaves the top of one floor part and goes on at the bottom of the part above (upstream's escalators, such
 * as {@code shopping00}) is re-laid whole across the stack ({@link #stack}, decision log D-stair-xfloor-1): the
 * stretched rows of every part it crosses push its start further back, so the lower half still meets the upper half.
 *
 * <p>
 * Plain Java, side: both. Pure functions on slice strings; {@link Part#stretched(int, int)} memoizes the result per
 * part and {@link Part#stacked(List, List)} per stack.
 */
public final class FloorStretch {

    /** The fewest steps a diagonal run needs to count as a stair run. */
    public static final int MIN_STAIR_STEPS = 4;

    private static final int[][] DIRECTIONS = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };

    private FloorStretch() {}

    /**
     * Returns the row a part repeats when it is stretched.
     *
     * @param part the floor part
     * @return the part's {@code stretchrow} when it sets one from 1 to {@code height − 2} (decision log D-mall-1);
     *         otherwise the row from 1 to {@code height − 2} with the most spaces, the highest on a tie; -1 when the
     *         part is under 3 rows tall and cannot stretch
     */
    public static int repeatRow(Part part) {
        Integer pinned = part.metaInteger(Part.MetaKey.STRETCHROW);
        if (pinned != null && pinned >= 1 && pinned <= part.height() - 2) {
            return pinned;
        }
        int best = -1;
        int bestSpaces = -1;
        for (int y = 1; y <= part.height() - 2; y++) {
            int spaces = 0;
            for (int z = 0; z < part.zsize(); z++) {
                for (int x = 0; x < part.xsize(); x++) {
                    if (part.get(x, y, z) == ' ') {
                        spaces++;
                    }
                }
            }
            if (spaces >= bestSpaces) {
                best = y;
                bestSpaces = spaces;
            }
        }
        return best;
    }

    /**
     * Returns the slices of a part stretched by {@code extra} rows.
     *
     * @param slices the part's slices, bottom first, each {@code z * xsize + x}
     * @param xsize  the width
     * @param zsize  the depth
     * @param extra  how many more times row {@code row} is written, at least 0
     * @param row    the repeat row, 0 to {@code slices.size() − 1}
     * @return new slices, {@code slices.size() + extra} of them
     */
    public static List<String> stretch(List<String> slices, int xsize, int zsize, int extra, int row) {
        int h = slices.size();
        if (extra <= 0 || row < 0 || row >= h) {
            return new ArrayList<>(slices);
        }
        return stack(Collections.singletonList(slices), xsize, zsize, Collections.singletonList(copies(h, extra, row)))
            .get(0);
    }

    /**
     * Returns how many times each row of a part is written when it is stretched.
     *
     * @param height the part's height
     * @param extra  how many more times row {@code row} is written; 0 or less writes every row once
     * @param row    the repeat row
     * @return one count per row, bottom first
     */
    public static int[] copies(int height, int extra, int row) {
        int[] out = new int[height];
        Arrays.fill(out, 1);
        if (extra > 0 && row >= 0 && row < height) {
            out[row] += extra;
        }
        return out;
    }

    /**
     * Returns how many times each row of a part is written when it is lowered.
     *
     * @param height the part's height
     * @param drop   the rows left out, as {@link #dropRows(Part, int)} gives them
     * @return one count per row, bottom first: 0 for a dropped row, 1 otherwise
     */
    public static int[] copies(int height, int[] drop) {
        int[] out = new int[height];
        Arrays.fill(out, 1);
        for (int d : drop) {
            if (d >= 0 && d < height) {
                out[d] = 0;
            }
        }
        return out;
    }

    /**
     * Returns a stack of floor parts, bottom first, each written at its own height (decision log D-stair-xfloor-1).
     * Every row of part {@code i} is written {@code copies.get(i)[y]} times: 0 drops it, more than 1 stretches it. A
     * diagonal stair run that leaves the top of one part and goes on at the bottom of the part above is one run here,
     * so it is re-laid whole, as a run inside one part is: it ends at its stretched top step and starts on its first
     * row as many blocks further back as the stack added rows across it. A run with a dropped row keeps the plain
     * copy, and so does a run whose longer form would leave the stack or overwrite a block. A one-part stack is
     * {@link #stretch(List, int, int, int, int)}.
     *
     * @param parts  each part's slices, bottom first, each {@code z * xsize + x}; every part has the same size
     * @param xsize  the width
     * @param zsize  the depth
     * @param copies per part, how many times each row is written
     * @return per part, its new slices
     */
    public static List<List<String>> stack(List<List<String>> parts, int xsize, int zsize, List<int[]> copies) {
        int n = parts.size();
        List<String> all = new ArrayList<>();
        List<Integer> partOf = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            all.addAll(parts.get(i));
            for (int y = 0; y < parts.get(i)
                .size(); y++) {
                partOf.add(i);
            }
        }
        int h = all.size();
        int[] cp = new int[h];
        boolean[] bottom = new boolean[h];
        boolean[] top = new boolean[h];
        int[] lo = new int[h];
        int[] newBase = new int[n + 1];
        int ny = 0;
        for (int y = 0, i = 0; i < n; i++) {
            newBase[i] = ny;
            int[] c = copies.get(i);
            for (int r = 0; r < parts.get(i)
                .size(); r++, y++) {
                cp[y] = r < c.length ? Math.max(0, c[r]) : 1;
                bottom[y] = r == 0;
                top[y] = r == parts.get(i)
                    .size() - 1;
                lo[y] = ny;
                ny += cp[y];
            }
        }
        newBase[n] = ny;
        boolean[] newBottom = new boolean[ny];
        for (int i = 0; i < n; i++) {
            if (newBase[i] < newBase[i + 1]) {
                newBottom[newBase[i]] = true;
            }
        }
        char[][] grid = new char[ny][];
        for (int y = 0; y < h; y++) {
            for (int k = 0; k < cp[y]; k++) {
                grid[lo[y] + k] = all.get(y)
                    .toCharArray();
            }
        }
        Layout stack = new Layout(all, xsize, zsize, cp, lo, bottom, newBottom);
        for (Chain chain : chains(all, xsize, zsize, MIN_STAIR_STEPS, top)) {
            int added = 0;
            boolean dropped = false;
            for (int y = chain.firstY; y <= chain.lastY(); y++) {
                added += cp[y] - 1;
                dropped |= cp[y] == 0;
            }
            if (!dropped && added > 0) {
                relay(grid, stack, added, chain);
            }
        }
        List<List<String>> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            List<String> slices = new ArrayList<>(newBase[i + 1] - newBase[i]);
            for (int y = newBase[i]; y < newBase[i + 1]; y++) {
                slices.add(new String(grid[y]));
            }
            out.add(slices);
        }
        return out;
    }

    /**
     * Returns the rows a part drops to stand at a lower floor height (decision log D-floor-heights-1), chosen one at a
     * time like {@link #repeatRow(Part)}: the remaining row from 1 to {@code height − 2} with the most spaces, the
     * highest on a tie. A row that holds a step of a diagonal stair run is skipped while any other row is left, so a
     * run keeps one rise per step.
     *
     * @param part  the floor part
     * @param fewer how many rows to drop, at least 1
     * @return the dropped rows, ascending; empty when the part is too short to drop that many
     */
    public static int[] dropRows(Part part, int fewer) {
        int h = part.height();
        if (fewer <= 0 || h - fewer < 3) {
            return new int[0];
        }
        List<String> slices = new ArrayList<>(h);
        int[] spaces = new int[h];
        for (int y = 0; y < h; y++) {
            StringBuilder row = new StringBuilder(part.xsize() * part.zsize());
            for (int z = 0; z < part.zsize(); z++) {
                for (int x = 0; x < part.xsize(); x++) {
                    char c = part.get(x, y, z);
                    row.append(c);
                    if (c == ' ') {
                        spaces[y]++;
                    }
                }
            }
            slices.add(row.toString());
        }
        boolean[] stairRow = new boolean[h];
        for (Chain chain : chains(slices, part.xsize(), part.zsize(), 2)) {
            for (int k = 0; k < chain.steps; k++) {
                stairRow[chain.firstY + k] = true;
            }
        }
        List<Integer> left = new ArrayList<>();
        for (int y = 0; y < h; y++) {
            left.add(y);
        }
        int[] out = new int[fewer];
        for (int i = 0; i < fewer; i++) {
            int best = -1;
            for (boolean allowStairs : new boolean[] { false, true }) {
                int bestSpaces = -1;
                for (int j = 1; j <= left.size() - 2; j++) {
                    int y = left.get(j);
                    if ((allowStairs || !stairRow[y]) && spaces[y] >= bestSpaces) {
                        best = j;
                        bestSpaces = spaces[y];
                    }
                }
                if (best >= 0) {
                    break;
                }
            }
            out[i] = left.remove(best);
        }
        Arrays.sort(out);
        return out;
    }

    /**
     * Returns the slices of a part without the given rows.
     *
     * @param slices the part's slices, bottom first
     * @param drop   the rows to leave out; rows past the top are ignored
     * @return new slices
     */
    public static List<String> squeeze(List<String> slices, int[] drop) {
        List<String> out = new ArrayList<>(slices.size());
        for (int y = 0; y < slices.size(); y++) {
            boolean dropped = false;
            for (int d : drop) {
                dropped |= d == y;
            }
            if (!dropped) {
                out.add(slices.get(y));
            }
        }
        return out;
    }

    /** Returns the original row a stretched row comes from. */
    static int source(int newY, int row, int extra) {
        if (newY <= row) {
            return newY;
        }
        return newY <= row + extra ? row : newY - extra;
    }

    /** A diagonal stair run in the original part: step {@code k} is at row {@code firstY + k}. */
    static final class Chain {

        final char c;
        final int firstY;
        final int x;
        final int z;
        final int dx;
        final int dz;
        final int steps;

        Chain(char c, int firstY, int x, int z, int dx, int dz, int steps) {
            this.c = c;
            this.firstY = firstY;
            this.x = x;
            this.z = z;
            this.dx = dx;
            this.dz = dz;
            this.steps = steps;
        }

        int lastY() {
            return firstY + steps - 1;
        }
    }

    /** Finds every diagonal stair run of a part. */
    static List<Chain> chains(List<String> slices, int xsize, int zsize) {
        return chains(slices, xsize, zsize, MIN_STAIR_STEPS);
    }

    /** Finds every diagonal run of at least {@code minSteps} steps. */
    static List<Chain> chains(List<String> slices, int xsize, int zsize, int minSteps) {
        boolean[] top = new boolean[slices.size()];
        if (top.length > 0) {
            top[top.length - 1] = true;
        }
        return chains(slices, xsize, zsize, minSteps, top);
    }

    /**
     * Finds every diagonal run of at least {@code minSteps} steps in a stack of parts. A step on a part's top row
     * counts as open above, as it does in the part on its own, and a run goes on into the part above when the next
     * step is there.
     *
     * @param top the rows that are a part's top row
     */
    static List<Chain> chains(List<String> slices, int xsize, int zsize, int minSteps, boolean[] top) {
        int h = slices.size();
        List<Chain> out = new ArrayList<>();
        for (int y = 0; y < h; y++) {
            for (int z = 0; z < zsize; z++) {
                for (int x = 0; x < xsize; x++) {
                    char c = at(slices, xsize, zsize, x, y, z);
                    if (c == ' ') {
                        continue;
                    }
                    for (int[] d : DIRECTIONS) {
                        if (at(slices, xsize, zsize, x - d[0], y - 1, z - d[1]) == c) {
                            continue; // not the run's first step
                        }
                        int n = 0;
                        while (at(slices, xsize, zsize, x + n * d[0], y + n, z + n * d[1]) == c
                            && (top[y + n] || open(slices, xsize, zsize, x + n * d[0], y + n + 1, z + n * d[1]))) {
                            n++;
                        }
                        if (n >= minSteps) {
                            out.add(new Chain(c, y, x, z, d[0], d[1], n));
                        }
                    }
                }
            }
        }
        return out;
    }

    /** A stack of parts as {@link #relay} sees it: the original rows and where each one is written. */
    private static final class Layout {

        final List<String> slices;
        final int xsize;
        final int zsize;
        /** Per original row, how many times it is written. */
        final int[] copies;
        /** Per original row, the first new row it is written to. */
        final int[] lo;
        /** Per original row, whether it is a part's floor row. */
        final boolean[] bottom;
        /** Per new row, whether it is a part's floor row. */
        final boolean[] newBottom;

        Layout(List<String> slices, int xsize, int zsize, int[] copies, int[] lo, boolean[] bottom,
            boolean[] newBottom) {
            this.slices = slices;
            this.xsize = xsize;
            this.zsize = zsize;
            this.copies = copies;
            this.lo = lo;
            this.bottom = bottom;
            this.newBottom = newBottom;
        }

        /** The new row an original row's last copy is written to. */
        int hi(int y) {
            return lo[y] + copies[y] - 1;
        }
    }

    /** Re-lays a run as one longer run; leaves the grid untouched when the longer run does not fit. */
    private static void relay(char[][] grid, Layout stack, int added, Chain chain) {
        int xsize = stack.xsize;
        int zsize = stack.zsize;
        char support = support(stack, chain);
        char[][] work = new char[grid.length][];
        for (int y = 0; y < grid.length; y++) {
            work[y] = grid[y].clone();
        }
        // Erase the run and its supports at every row they were copied to.
        for (int k = 0; k < chain.steps; k++) {
            int x = chain.x + k * chain.dx;
            int z = chain.z + k * chain.dz;
            int y = chain.firstY + k;
            erase(work, xsize, x, z, stack.lo[y], stack.hi(y), chain.c);
            if (support != ' ' && y - 1 >= 0 && !stack.bottom[y - 1]) {
                erase(work, xsize, x, z, stack.lo[y - 1], stack.hi(y - 1), support);
            }
        }
        int count = chain.steps + added;
        int topY = stack.hi(chain.lastY());
        int lastX = chain.x + (chain.steps - 1) * chain.dx;
        int lastZ = chain.z + (chain.steps - 1) * chain.dz;
        for (int j = 0; j < count; j++) {
            int x = lastX - j * chain.dx;
            int z = lastZ - j * chain.dz;
            int y = topY - j;
            if (x < 0 || x >= xsize || z < 0 || z >= zsize || y < 0 || work[y][z * xsize + x] != ' ') {
                return;
            }
            work[y][z * xsize + x] = chain.c;
        }
        if (support != ' ') {
            for (int j = 0; j < count; j++) {
                int x = lastX - j * chain.dx;
                int z = lastZ - j * chain.dz;
                int below = topY - j - 1;
                if (below >= 0 && !stack.newBottom[below] && work[below][z * xsize + x] == ' ') {
                    work[below][z * xsize + x] = support;
                }
            }
        }
        for (int y = 0; y < grid.length; y++) {
            grid[y] = work[y];
        }
    }

    /** The most common character under the run's steps above a floor row, besides space and the run's own. */
    private static char support(Layout stack, Chain chain) {
        Map<Character, Integer> counts = new HashMap<>();
        for (int k = 1; k < chain.steps; k++) {
            int y = chain.firstY + k - 1;
            if (stack.bottom[y]) {
                continue;
            }
            char c = at(stack.slices, stack.xsize, stack.zsize, chain.x + k * chain.dx, y, chain.z + k * chain.dz);
            if (c != ' ' && c != chain.c) {
                counts.merge(c, 1, Integer::sum);
            }
        }
        char best = ' ';
        int bestCount = 0;
        for (Map.Entry<Character, Integer> e : counts.entrySet()) {
            if (e.getValue() > bestCount || (e.getValue() == bestCount && e.getKey() < best)) {
                best = e.getKey();
                bestCount = e.getValue();
            }
        }
        return best;
    }

    private static void erase(char[][] work, int xsize, int x, int z, int from, int to, char c) {
        for (int y = from; y <= to; y++) {
            if (work[y][z * xsize + x] == c) {
                work[y][z * xsize + x] = ' ';
            }
        }
    }

    private static char at(List<String> slices, int xsize, int zsize, int x, int y, int z) {
        if (x < 0 || x >= xsize || z < 0 || z >= zsize || y < 0 || y >= slices.size()) {
            return '\0';
        }
        return slices.get(y)
            .charAt(z * xsize + x);
    }

    /** A space, or above the part's top. */
    private static boolean open(List<String> slices, int xsize, int zsize, int x, int y, int z) {
        return y >= slices.size() || at(slices, xsize, zsize, x, y, z) == ' ';
    }
}
