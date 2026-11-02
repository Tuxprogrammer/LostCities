package com.tuxprogrammer.lostcities.compat.ae2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.worldgen.height.TerrainHeightOracle;

/**
 * The crater basin: the lowest paraboloid AE2 can dig, read from the meteor's own chunk, and the footprint AE2 itself
 * rebuilds (the decay square). Keeping a city's streets under the paraboloid farther out is the layout's
 * ({@code MeteoriteOracleTest.theCityStandsUnderTheCrater}).
 */
class CraterBasinTest {

    private static final int X = 1000;
    private static final int Z = -500;

    @Test
    void flatGround() {
        TerrainHeightOracle flat = (x, z) -> 70;
        MeteoriteShape shape = MeteoriteShape.of(-7543508780968384435L); // size 5.91
        CraterBasin basin = CraterBasin.of(flat, X, Z, shape);
        // 70 - 10 - 5.91 + 1 = 55.09, rounded down, less a block of slack.
        assertEquals(54, basin.capCenterY());
        assertEquals(70, basin.groundEstimate());
        assertEquals(CraterBasin.DECAY_HALF_WIDTH, basin.halfWidth(), basin.toString());
    }

    @Test
    void theCraterIsNeverWiderThanTheFootprint() {
        TerrainHeightOracle flat = (x, z) -> 64;
        for (long seed = 1; seed < 200; seed++) {
            MeteoriteShape shape = MeteoriteShape.of(seed);
            CraterBasin basin = CraterBasin.of(flat, X, Z, shape);
            assertTrue(basin.halfWidth() >= shape.craterSize(), basin + " " + shape);
            assertTrue(basin.halfWidth() >= CraterBasin.DECAY_HALF_WIDTH, basin.toString());
        }
    }

    @Test
    void theBasinReadsNoGroundBeyondTheMeteorChunkAndItsPadding() {
        int[] farthest = { 0 };
        TerrainHeightOracle counting = (x, z) -> {
            farthest[0] = Math.max(farthest[0], Math.max(Math.abs(x - X), Math.abs(z - Z)));
            return 70;
        };
        CraterBasin.of(counting, X, Z, MeteoriteShape.of(5L));
        assertTrue(farthest[0] <= 16 + CraterBasin.STRIDE_PAD, "farthest sample " + farthest[0]);
    }

    @Test
    void theCapCenterReadsOnlyTheMeteorChunksSkyBox() {
        int chunkMinX = X >> 4 << 4;
        int chunkMinZ = Z >> 4 << 4;
        // A pit inside the meteor's chunk, within the sky box: AE2 counts it, so the cap goes down.
        TerrainHeightOracle pitInside = (x, z) -> x == chunkMinX + 3 && z == chunkMinZ + 3 ? 50 : 70;
        // A pit far outside the box and its padding: AE2 never sees it.
        TerrainHeightOracle pitOutside = (x, z) -> x <= X - 40 ? 50 : 70;
        // 50 - 10 - 5 + 1 - 1 and 70 - 10 - 5 + 1 - 1.
        assertEquals(35, CraterBasin.capCenterY(pitInside, X, Z, 5.0D));
        assertEquals(55, CraterBasin.capCenterY(pitOutside, X, Z, 5.0D));
    }

    @Test
    void aBasinReadOneSampleAtATimeIsTheOnePassBasin() {
        TerrainHeightOracle rough = (x, z) -> 62 + Math.floorMod(x * 13 + z * 7, 17);
        MeteoriteShape shape = MeteoriteShape.of(99L);
        CraterBasin whole = CraterBasin.of(rough, X, Z, shape);
        CraterBasin.Builder builder = new CraterBasin.Builder(rough, X, Z, shape);
        int steps = 1;
        while (!builder.step(0L)) {
            steps++;
        }
        CraterBasin stepped = builder.build();
        assertTrue(steps > 100, "a zero budget reads one sample per step: " + steps);
        assertEquals(CraterBasin.capCenterY(rough, X, Z, shape.size()), stepped.capCenterY());
        assertEquals(whole.capCenterY(), stepped.capCenterY());
        assertEquals(whole.halfWidth(), stepped.halfWidth());
        assertEquals(whole.groundEstimate(), stepped.groundEstimate());
    }

    @Test
    void aStrideEightOracleCannotHideALowerColumn() {
        // RWG2's oracle answers each column with the sample at its 8-block stride corner; a slope of one block per
        // block then hides up to 7 blocks between samples. The padded box sees the lower sample beyond.
        TerrainHeightOracle slope = (x, z) -> 70 + (x - Math.floorMod(x, 8)) - X;
        int chunkMinX = X >> 4 << 4;
        int lowestInBox = 70 + Math.max(X - 15, chunkMinX) - X;
        int capCenter = CraterBasin.capCenterY(slope, X, Z, 2.0D);
        assertTrue(capCenter <= lowestInBox - 10 - 2 + 1 - CraterBasin.SLACK, "cap " + capCenter);
    }

    @Test
    void groundEstimateIsTheMedianOverTheSkyBox() {
        TerrainHeightOracle half = (x, z) -> x < X ? 60 : 80;
        CraterBasin basin = CraterBasin.of(half, X, Z, MeteoriteShape.of(3L));
        assertTrue(basin.groundEstimate() == 60 || basin.groundEstimate() == 80, basin.toString());
    }
}
