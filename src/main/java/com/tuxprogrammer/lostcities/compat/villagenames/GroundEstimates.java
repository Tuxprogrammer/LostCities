package com.tuxprogrammer.lostcities.compat.villagenames;

import java.util.Arrays;
import java.util.List;

import com.tuxprogrammer.lostcities.layout.landmark.BlockRect;
import com.tuxprogrammer.lostcities.worldgen.height.TerrainHeightOracle;

/**
 * Estimates a structure's ground height from the height oracle, never from the world: the median of the oracle's
 * heights at the center and four corners of each box. A VillageNames building takes the median ground of its own box
 * at population (coexistence.md, "Heights come at population"), and the city never writes in a core, so that ground
 * is RWG's; the median over every building's samples is the one number a landmark carries (0006 §1,
 * {@code groundEstimate}).
 *
 * <p>
 * Side: server (world generation), and plain Java in tests.
 */
public final class GroundEstimates {

    /** The ground Y used when no height oracle covers the world: VillageNames' placeholder piece height. */
    public static final int FALLBACK_Y = 64;

    private GroundEstimates() {}

    /**
     * Returns the median oracle height over boxes.
     *
     * @param heights the height oracle, or {@code null}
     * @param boxes   the boxes to sample, at least one
     * @return the median height, block Y; {@link #FALLBACK_Y} without an oracle
     */
    public static int median(TerrainHeightOracle heights, List<BlockRect> boxes) {
        if (heights == null || boxes.isEmpty()) {
            return FALLBACK_Y;
        }
        int[] samples = new int[boxes.size() * 5];
        int n = 0;
        for (BlockRect box : boxes) {
            int centerX = Math.floorDiv(box.minX() + box.maxX(), 2);
            int centerZ = Math.floorDiv(box.minZ() + box.maxZ(), 2);
            samples[n++] = heights.height(centerX, centerZ);
            samples[n++] = heights.height(box.minX(), box.minZ());
            samples[n++] = heights.height(box.maxX(), box.minZ());
            samples[n++] = heights.height(box.minX(), box.maxZ());
            samples[n++] = heights.height(box.maxX(), box.maxZ());
        }
        Arrays.sort(samples);
        return samples[samples.length / 2];
    }
}
