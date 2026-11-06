package com.tuxprogrammer.lostcities.asset.model;

import java.util.ArrayList;
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
 * Plain Java, side: both. Pure functions on slice strings; {@link Part#stretched(int, int)} memoizes the result per
 * part.
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
     * @return the row from 1 to {@code height − 2} with the most spaces, the highest on a tie; -1 when the part is
     *         under 3 rows tall and cannot stretch
     */
    public static int repeatRow(Part part) {
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
        char[][] grid = new char[h + extra][];
        for (int ny = 0; ny < h + extra; ny++) {
            grid[ny] = slices.get(source(ny, row, extra))
                .toCharArray();
        }
        for (Chain chain : chains(slices, xsize, zsize)) {
            if (chain.firstY <= row && row <= chain.lastY()) {
                relay(grid, slices, xsize, zsize, extra, row, chain);
            }
        }
        List<String> out = new ArrayList<>(h + extra);
        for (char[] slice : grid) {
            out.add(new String(slice));
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
                            && open(slices, xsize, zsize, x + n * d[0], y + n + 1, z + n * d[1])) {
                            n++;
                        }
                        if (n >= MIN_STAIR_STEPS) {
                            out.add(new Chain(c, y, x, z, d[0], d[1], n));
                        }
                    }
                }
            }
        }
        return out;
    }

    /** Re-lays a run as one longer run; leaves the grid untouched when the longer run does not fit. */
    private static void relay(char[][] grid, List<String> slices, int xsize, int zsize, int extra, int row,
        Chain chain) {
        char support = support(slices, xsize, zsize, chain);
        char[][] work = new char[grid.length][];
        for (int y = 0; y < grid.length; y++) {
            work[y] = grid[y].clone();
        }
        // Erase the run and its supports at every row they were copied to.
        for (int k = 0; k < chain.steps; k++) {
            int x = chain.x + k * chain.dx;
            int z = chain.z + k * chain.dz;
            int y = chain.firstY + k;
            erase(work, xsize, x, z, rows(y, row, extra), chain.c);
            if (support != ' ' && y - 1 >= 1) {
                erase(work, xsize, x, z, rows(y - 1, row, extra), support);
            }
        }
        int count = chain.steps + extra;
        int topY = chain.lastY() + extra;
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
                if (below >= 1 && work[below][z * xsize + x] == ' ') {
                    work[below][z * xsize + x] = support;
                }
            }
        }
        for (int y = 0; y < grid.length; y++) {
            grid[y] = work[y];
        }
    }

    /** The most common character under the run's steps above the floor row, besides space and the run's own. */
    private static char support(List<String> slices, int xsize, int zsize, Chain chain) {
        Map<Character, Integer> counts = new HashMap<>();
        for (int k = 1; k < chain.steps; k++) {
            int y = chain.firstY + k - 1;
            if (y < 1) {
                continue;
            }
            char c = at(slices, xsize, zsize, chain.x + k * chain.dx, y, chain.z + k * chain.dz);
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

    /** The stretched rows an original row was written to. */
    private static int[] rows(int y, int row, int extra) {
        if (y < row) {
            return new int[] { y };
        }
        if (y > row) {
            return new int[] { y + extra };
        }
        int[] out = new int[extra + 1];
        for (int i = 0; i <= extra; i++) {
            out[i] = row + i;
        }
        return out;
    }

    private static void erase(char[][] work, int xsize, int x, int z, int[] rows, char c) {
        for (int y : rows) {
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
