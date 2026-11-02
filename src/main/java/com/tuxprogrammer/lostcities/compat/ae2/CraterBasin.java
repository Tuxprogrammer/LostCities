package com.tuxprogrammer.lostcities.compat.ae2;

import java.util.Arrays;

import com.tuxprogrammer.lostcities.worldgen.height.TerrainHeightOracle;

/**
 * The crater basin of one meteorite (0006 §1 and §3): the lowest Y AE2's crater paraboloid can have, and the square of
 * ground around the impact that AE2 may change, which becomes the landmark's footprint (its core). Outside it the city
 * stands below the paraboloid, buildings by the layout's floor cap and streets because the square covers every column
 * where the paraboloid could reach down to the ground.
 *
 * <p>
 * What AE2 does, at rv3-beta-1095-GTNH ({@code MeteoritePlacer}, {@code MeteoriteWorldGen}):
 * <ul>
 * <li>The meteor's center {@code y} is the first height, stepping down 15 blocks at a time from its start height,
 * where the block is natural ground and the 30³ box around it holds no water, log, plank, brick, clay, iron bar or
 * door. It reads only the meteor's own chunk ({@code ChunkOnly}), so every test is clipped to that chunk.</li>
 * <li>The crater is dug only when more than 10 blocks of the box columns {@code x − 15 … x + 14} (and Z), from
 * {@code y − 15} to {@code y + 10}, in that chunk, see the sky ({@code skyMode > 10}). A column whose sky-seeing part
 * starts at Y {@code T} contributes only above {@code T}, so a crater implies {@code y ≥ Tmin − 10}, with {@code Tmin}
 * the lowest such start in the clipped box.</li>
 * <li>The crater removes every block above {@code y − size + 1 + adjust + 0.02 d²} (d horizontal distance, adjust 0,
 * or 2 on sand, ice and snow) within 200 blocks.</li>
 * <li>Decay ({@code skyMode > 3}) moves blocks down into air beneath them and scatters fallout in the square
 * {@code x − 30 … x + 29} (and Z), from {@code y − 9} to {@code y + 29}: any building there would sag.</li>
 * </ul>
 * So the paraboloid's lowest point is at least {@code Tmin − 10 − size + 1}. The height oracle stands in for
 * {@code T}: its height is the first non-solid Y of the base terrain, which is where the sky starts on natural ground.
 * The minimum is taken over every column of the clipped box and, because RWG2's oracle answers each column with the
 * sample at its 8-block stride corner, over samples every {@value #PAD_STEP} blocks of the box grown by
 * {@value #STRIDE_PAD} (not on classic RWG, whose oracle is exact per column); {@value #SLACK} more block of slack
 * covers the rest of the oracle's error.
 *
 * <p>
 * The footprint is the square of half-width at least {@value #DECAY_HALF_WIDTH} (the decay square) and the crater's
 * size, grown to every {@value #CELL}-block cell within {@value #MAX_HALF_WIDTH} blocks whose highest corner, plus
 * {@value #CITY_HEADROOM} blocks for a city's ground and street furniture above natural ground, lies above the
 * paraboloid at the cell's nearest point. The cells are read ring by ring from the impact and the reading stops at a
 * ring nothing reaches or nears ({@link Builder}).
 *
 * <p>
 * Sources:
 * https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/rv3-beta-1095-GTNH/src/main/java/appeng/worldgen/MeteoritePlacer.java#L177-L196
 * (the sky count), #L274-L319 (the crater), #L431-L493 (decay), #L495-L564 (the center test);
 * https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/rv3-beta-1095-GTNH/src/main/java/appeng/worldgen/MeteoriteWorldGen.java#L146-L187
 * (the 15-block steps).
 *
 * <p>
 * Pure. Side: both (plain Java).
 */
public final class CraterBasin {

    /** Half-width of AE2's sky-count box, blocks: columns {@code x − 15} to {@code x + 14}. */
    static final int SKY_BOX_HALF = 15;
    /** How far above the center the sky count reaches, blocks. */
    static final int SKY_BOX_TOP = 10;
    /** Grows the sampled box so RWG2's 8-block stride cannot hide a lower column. */
    static final int STRIDE_PAD = 8;
    /** Spacing of the samples in the padding around the box, blocks. */
    static final int PAD_STEP = 4;
    /** Blocks of slack under the estimate, for the oracle's error. */
    static final int SLACK = 1;
    /** Half-width of AE2's decay square, blocks. */
    public static final int DECAY_HALF_WIDTH = 30;
    /** Farthest the footprint grows, blocks; beyond it the paraboloid is at least 82 blocks above its center. */
    public static final int MAX_HALF_WIDTH = 64;
    /** Spacing of the ground samples around the impact, blocks. */
    static final int CELL = 8;
    /** Blocks a city's ground and street furniture may stand above natural ground: one city level. */
    public static final int CITY_HEADROOM = 6;
    /** AE2's crater curvature, per block². */
    static final double CURVATURE = 0.02D;

    private final int capCenterY;
    private final int halfWidth;
    private final int groundEstimate;

    private CraterBasin(int capCenterY, int halfWidth, int groundEstimate) {
        this.capCenterY = capCenterY;
        this.halfWidth = halfWidth;
        this.groundEstimate = groundEstimate;
    }

    /**
     * Computes a meteorite's basin.
     *
     * @param heights the height oracle
     * @param x       the impact's block X
     * @param z       the impact's block Z
     * @param shape   the meteorite's shape
     * @return the basin
     */
    public static CraterBasin of(TerrainHeightOracle heights, int x, int z, MeteoriteShape shape) {
        return of(heights, x, z, shape, true);
    }

    /**
     * Computes a meteorite's basin, with or without its ground grid.
     *
     * @param heights the height oracle
     * @param x       the impact's block X
     * @param z       the impact's block Z
     * @param shape   the meteorite's shape
     * @param grid    whether to read the ground grid; without it the footprint is the decay square and the crater,
     *                and the ground estimate the median of the sky box, enough where no city stands near
     * @return the basin
     */
    public static CraterBasin of(TerrainHeightOracle heights, int x, int z, MeteoriteShape shape, boolean grid) {
        Builder builder = new Builder(heights, x, z, shape, grid);
        builder.step(Long.MAX_VALUE);
        return builder.build();
    }

    /**
     * Computes a basin in steps, so the plan prefetcher can spread its height samples over several fetches instead of
     * one (budget B4): each step reads samples for at most the time it is given, and at least one. On RWG2 Continents
     * a step reads many samples; on classic RWG, whose oracle computes a whole chunk for a chunk's first column, a few.
     *
     * <p>
     * The samples are the cap's (every column of the clipped sky box and the padding around it, as
     * {@link #capCenterY} reads them), then the {@value #CELL}-block ground grid from the inside out: the square of
     * rings up to {@value #INNER_RINGS} cells from the impact, which holds the decay square, then one ring at a time,
     * read only while a cell of the ring before reaches the paraboloid (its highest corner plus
     * {@value #CITY_HEADROOM} above the cap at its nearest point) or a corner of that ring stands within
     * {@value #CLIFF_MARGIN} blocks of reaching the next ring's cap. Beyond, ground that overtook the cap would have to
     * climb more than {@value #CLIFF_MARGIN} blocks within one 8-block ring: a cliff, which is accepted (decision log).
     * The rings not read save classic RWG most of the 81 chunks a full 128-block grid costs. {@code oracle.ae2} checks
     * the result against the craters AE2 digs.
     */
    static final class Builder {

        /** Rings of the ground grid always read: out to 32 blocks, past the decay square's 30. */
        static final int INNER_RINGS = 4;
        /**
         * Blocks of climb the next ring is read for even when no cell of this ring reaches the paraboloid: ground on
         * this ring that close to the next cells' cap could reach it within one 8-block ring.
         */
        static final int CLIFF_MARGIN = 16;
        private static final int CORNERS = 2 * MAX_HALF_WIDTH / CELL + 1;
        private static final int MIDDLE = CORNERS / 2;

        private final TerrainHeightOracle heights;
        private final int x;
        private final int z;
        private final MeteoriteShape shape;
        private final int[] capX;
        private final int[] capZ;
        private final int[] capValues;
        private final int[] grid = new int[CORNERS * CORNERS];
        private final boolean[] read = new boolean[CORNERS * CORNERS];
        private int capNext;
        private int capCenter;
        /** The ring being read: {@link #INNER_RINGS} for the inner square, then one more each. */
        private int ring = INNER_RINGS;
        /** The corners of the ring being read, as grid indices. */
        private int[] ringCorners;
        private int ringNext;
        private int half;
        private boolean done;
        private final boolean withGrid;

        Builder(TerrainHeightOracle heights, int x, int z, MeteoriteShape shape) {
            this(heights, x, z, shape, true);
        }

        Builder(TerrainHeightOracle heights, int x, int z, MeteoriteShape shape, boolean withGrid) {
            this.withGrid = withGrid;
            this.heights = heights;
            this.x = x;
            this.z = z;
            this.shape = shape;
            int chunkX = x >> 4;
            int chunkZ = z >> 4;
            int lowX = Math.max(x - SKY_BOX_HALF, chunkX << 4);
            int highX = Math.min(x + SKY_BOX_HALF - 1, (chunkX << 4) + 15);
            int lowZ = Math.max(z - SKY_BOX_HALF, chunkZ << 4);
            int highZ = Math.min(z + SKY_BOX_HALF - 1, (chunkZ << 4) + 15);
            java.util.List<int[]> points = new java.util.ArrayList<>();
            for (int i = lowX; i <= highX; i++) {
                for (int k = lowZ; k <= highZ; k++) {
                    points.add(new int[] { i, k });
                }
            }
            // An oracle exact per column (classic RWG's, which computes whole chunks) needs no padding.
            int pad = heights.computesWholeChunks() ? 0 : STRIDE_PAD;
            for (int i = lowX - pad; i <= highX + pad && pad > 0; i = next(i, highX + pad)) {
                for (int k = lowZ - pad; k <= highZ + pad; k = next(k, highZ + pad)) {
                    if (i < lowX || i > highX || k < lowZ || k > highZ) {
                        points.add(new int[] { i, k });
                    }
                }
            }
            capX = new int[points.size()];
            capZ = new int[points.size()];
            for (int s = 0; s < points.size(); s++) {
                capX[s] = points.get(s)[0];
                capZ[s] = points.get(s)[1];
            }
            capValues = new int[points.size()];
            half = Math.max(DECAY_HALF_WIDTH, (int) Math.ceil(shape.craterSize()));
            ringCorners = corners(0, INNER_RINGS);
        }

        /** The grid indices of the corners whose ring (Chebyshev distance from the middle) is in {@code [from, to]}. */
        private static int[] corners(int from, int to) {
            int[] out = new int[CORNERS * CORNERS];
            int n = 0;
            for (int i = 0; i < CORNERS; i++) {
                for (int k = 0; k < CORNERS; k++) {
                    int level = Math.max(Math.abs(i - MIDDLE), Math.abs(k - MIDDLE));
                    if (level >= from && level <= to) {
                        out[n++] = i * CORNERS + k;
                    }
                }
            }
            return Arrays.copyOf(out, n);
        }

        /**
         * Reads samples for at most {@code budgetNanos}, and at least one.
         *
         * @param budgetNanos the time this step may take, nanoseconds
         * @return {@code true} once every sample the basin needs is read
         */
        boolean step(long budgetNanos) {
            long start = System.nanoTime();
            do {
                readOne();
            } while (!done && System.nanoTime() - start < budgetNanos);
            return done;
        }

        private void readOne() {
            if (capNext < capValues.length) {
                capValues[capNext] = heights.height(capX[capNext], capZ[capNext]);
                capNext++;
                if (capNext == capValues.length) {
                    int lowest = Integer.MAX_VALUE;
                    for (int value : capValues) {
                        lowest = Math.min(lowest, value);
                    }
                    capCenter = (int) Math.floor(lowest - SKY_BOX_TOP - shape.size() + 1) - SLACK;
                    done = !withGrid;
                }
                return;
            }
            int index = ringCorners[ringNext++];
            int i = index / CORNERS;
            int k = index % CORNERS;
            grid[index] = heights.height(x - MAX_HALF_WIDTH + i * CELL, z - MAX_HALF_WIDTH + k * CELL);
            read[index] = true;
            if (ringNext < ringCorners.length) {
                return;
            }
            boolean reached = evaluateRing(ring);
            if (reached && ring < MIDDLE) {
                ring++;
                ringCorners = corners(ring, ring);
                ringNext = 0;
            } else {
                done = true;
            }
        }

        /**
         * Grows {@link #half} over the cells whose outer ring is {@code ring} (every cell of the inner square for the
         * first) and returns whether any of them reaches the paraboloid.
         */
        private boolean evaluateRing(int ring) {
            boolean reached = false;
            for (int i = 0; i + 1 < CORNERS; i++) {
                for (int k = 0; k + 1 < CORNERS; k++) {
                    int outer = Math.max(
                        Math.max(Math.abs(i - MIDDLE), Math.abs(i + 1 - MIDDLE)),
                        Math.max(Math.abs(k - MIDDLE), Math.abs(k + 1 - MIDDLE)));
                    if (ring == INNER_RINGS ? outer > INNER_RINGS : outer != ring) {
                        continue;
                    }
                    int highest = Math.max(
                        Math.max(grid[i * CORNERS + k], grid[(i + 1) * CORNERS + k]),
                        Math.max(grid[i * CORNERS + k + 1], grid[(i + 1) * CORNERS + k + 1]));
                    int fromX = -MAX_HALF_WIDTH + i * CELL;
                    int fromZ = -MAX_HALF_WIDTH + k * CELL;
                    double nearestX = nearest(fromX, fromX + CELL);
                    double nearestZ = nearest(fromZ, fromZ + CELL);
                    double cap = capCenter + CURVATURE * (nearestX * nearestX + nearestZ * nearestZ);
                    if (cap < highest + CITY_HEADROOM) {
                        int far = Math.max(
                            Math.max(Math.abs(fromX), Math.abs(fromX + CELL)),
                            Math.max(Math.abs(fromZ), Math.abs(fromZ + CELL)));
                        half = Math.max(half, far);
                        if (outer == ring) {
                            reached = true;
                        }
                    }
                }
            }
            if (reached) {
                return true;
            }
            // Ground on this ring within CLIFF_MARGIN of reaching the next ring's cells also reads the next ring.
            double nextCap = capCenter + CURVATURE * (ring * CELL) * (double) (ring * CELL);
            for (int index : ringCorners) {
                if (grid[index] + CITY_HEADROOM + CLIFF_MARGIN >= nextCap) {
                    return true;
                }
            }
            return false;
        }

        /** Returns the basin; {@link #step} must have returned {@code true}. */
        CraterBasin build() {
            int bounded = Math.min(half, MAX_HALF_WIDTH);
            if (!withGrid) {
                int[] box = capValues.clone();
                Arrays.sort(box);
                return new CraterBasin(capCenter, bounded, box[box.length / 2]);
            }
            int[] inside = new int[grid.length];
            int n = 0;
            for (int i = 0; i < CORNERS; i++) {
                for (int k = 0; k < CORNERS; k++) {
                    int dx = -MAX_HALF_WIDTH + i * CELL;
                    int dz = -MAX_HALF_WIDTH + k * CELL;
                    if (read[i * CORNERS + k] && Math.abs(dx) <= bounded && Math.abs(dz) <= bounded) {
                        inside[n++] = grid[i * CORNERS + k];
                    }
                }
            }
            int[] samples = Arrays.copyOf(inside, n);
            Arrays.sort(samples);
            return new CraterBasin(capCenter, bounded, samples[samples.length / 2]);
        }
    }

    /**
     * Returns the lowest Y the crater paraboloid can have at the impact: {@code Tmin − 10 − size + 1} less the slack,
     * rounded down (see the class comment).
     *
     * @param heights the height oracle
     * @param x       the impact's block X
     * @param z       the impact's block Z
     * @param size    the meteorite's size, blocks
     * @return block Y
     */
    static int capCenterY(TerrainHeightOracle heights, int x, int z, double size) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        int lowX = Math.max(x - SKY_BOX_HALF, chunkX << 4);
        int highX = Math.min(x + SKY_BOX_HALF - 1, (chunkX << 4) + 15);
        int lowZ = Math.max(z - SKY_BOX_HALF, chunkZ << 4);
        int highZ = Math.min(z + SKY_BOX_HALF - 1, (chunkZ << 4) + 15);
        // Every column of the box, which lies in the meteor's chunk: one chunk of the oracle.
        int[] chunk = new int[256];
        heights.chunkHeights(chunkX, chunkZ, chunk);
        int lowest = Integer.MAX_VALUE;
        for (int i = lowX; i <= highX; i++) {
            for (int k = lowZ; k <= highZ; k++) {
                lowest = Math.min(lowest, chunk[(i & 15) * 16 + (k & 15)]);
            }
        }
        // The padding, where a stride corner standing for columns inside the box may lie.
        // An oracle exact per column (classic RWG's, which computes whole chunks) needs no padding.
        int pad = heights.computesWholeChunks() ? 0 : STRIDE_PAD;
        for (int i = lowX - pad; i <= highX + pad && pad > 0; i = next(i, highX + pad)) {
            for (int k = lowZ - pad; k <= highZ + pad; k = next(k, highZ + pad)) {
                if (i < lowX || i > highX || k < lowZ || k > highZ) {
                    lowest = Math.min(lowest, heights.height(i, k));
                }
            }
        }
        return (int) Math.floor(lowest - SKY_BOX_TOP - size + 1) - SLACK;
    }

    /** Steps by {@link #PAD_STEP}, landing on the last column exactly. */
    private static int next(int at, int last) {
        if (at == last) {
            return last + 1;
        }
        return Math.min(at + PAD_STEP, last);
    }

    /** Returns the offset nearest 0 within {@code [from, to]}. */
    private static double nearest(int from, int to) {
        if (from > 0) {
            return from;
        }
        if (to < 0) {
            return to;
        }
        return 0;
    }

    /** Returns the lowest Y of the crater paraboloid, block Y. */
    public int capCenterY() {
        return capCenterY;
    }

    /** Returns the footprint's half-width around the impact, blocks. */
    public int halfWidth() {
        return halfWidth;
    }

    /** Returns the median oracle height over the footprint, block Y. */
    public int groundEstimate() {
        return groundEstimate;
    }

    @Override
    public String toString() {
        return "basin(cap y" + capCenterY + " half " + halfWidth + " ground " + groundEstimate + ")";
    }
}
