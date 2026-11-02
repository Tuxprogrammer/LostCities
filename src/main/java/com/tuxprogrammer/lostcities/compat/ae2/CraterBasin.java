package com.tuxprogrammer.lostcities.compat.ae2;

import java.util.Arrays;

import com.tuxprogrammer.lostcities.layout.landmark.HeightCap;
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
 * The footprint is the square of half-width {@value #DECAY_HALF_WIDTH} (the decay square), or the crater's size where
 * that is larger: the ground AE2 itself rebuilds. Farther out the city stays under the paraboloid through the layout,
 * which caps a building's floors and leaves out of the city every chunk whose ground, plus {@link
 * HeightCap#CITY_HEADROOM} blocks, the cap over it would cut (decision log, D-M08-23); so the oracle reads no ground
 * beyond the meteor's own chunk. The ground estimate is the median of the sky box's columns.
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
        Builder builder = new Builder(heights, x, z, shape);
        builder.step(Long.MAX_VALUE);
        return builder.build();
    }

    /**
     * Computes a basin in steps, so the plan prefetcher can spread its height samples (every column of the clipped
     * sky box and the padding around it, as {@link #capCenterY} reads them: about 300 on RWG2 Continents, one chunk on
     * classic RWG) over several fetches (budget B4): each step reads samples for at most the time it is given, and at
     * least one. The basin is the same as one pass.
     */
    static final class Builder {

        private final TerrainHeightOracle heights;
        private final MeteoriteShape shape;
        private final int[] sampleX;
        private final int[] sampleZ;
        private final int[] values;
        private final int boxSamples;
        private int next;

        Builder(TerrainHeightOracle heights, int x, int z, MeteoriteShape shape) {
            this.heights = heights;
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
            boxSamples = points.size();
            // An oracle exact per column (classic RWG's, which computes whole chunks) needs no padding.
            int pad = heights.computesWholeChunks() ? 0 : STRIDE_PAD;
            for (int i = lowX - pad; i <= highX + pad && pad > 0; i = next(i, highX + pad)) {
                for (int k = lowZ - pad; k <= highZ + pad; k = next(k, highZ + pad)) {
                    if (i < lowX || i > highX || k < lowZ || k > highZ) {
                        points.add(new int[] { i, k });
                    }
                }
            }
            sampleX = new int[points.size()];
            sampleZ = new int[points.size()];
            for (int s = 0; s < points.size(); s++) {
                sampleX[s] = points.get(s)[0];
                sampleZ[s] = points.get(s)[1];
            }
            values = new int[points.size()];
        }

        /**
         * Reads samples for at most {@code budgetNanos}, and at least one.
         *
         * @param budgetNanos the time this step may take, nanoseconds
         * @return {@code true} once every sample is read
         */
        boolean step(long budgetNanos) {
            long start = System.nanoTime();
            do {
                values[next] = heights.height(sampleX[next], sampleZ[next]);
                next++;
            } while (next < values.length && System.nanoTime() - start < budgetNanos);
            return next >= values.length;
        }

        /** Returns the basin; {@link #step} must have returned {@code true}. */
        CraterBasin build() {
            int lowest = Integer.MAX_VALUE;
            for (int value : values) {
                lowest = Math.min(lowest, value);
            }
            int capCenter = (int) Math.floor(lowest - SKY_BOX_TOP - shape.size() + 1) - SLACK;
            int half = Math.max(DECAY_HALF_WIDTH, (int) Math.ceil(shape.craterSize()));
            int[] box = Arrays.copyOf(values, boxSamples);
            Arrays.sort(box);
            return new CraterBasin(capCenter, half, box[box.length / 2]);
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

    /** Returns the lowest Y of the crater paraboloid, block Y. */
    public int capCenterY() {
        return capCenterY;
    }

    /** Returns the footprint's half-width around the impact, blocks. */
    public int halfWidth() {
        return halfWidth;
    }

    /** Returns the median oracle height over the sky box, block Y. */
    public int groundEstimate() {
        return groundEstimate;
    }

    @Override
    public String toString() {
        return "basin(cap y" + capCenterY + " half " + halfWidth + " ground " + groundEstimate + ")";
    }
}
