package com.tuxprogrammer.lostcities.compat.ae2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.worldgen.height.TerrainHeightOracle;

/**
 * The crater basin: the lowest paraboloid AE2 can dig, and a footprint outside which the paraboloid never reaches
 * down to a city standing up to {@link CraterBasin#CITY_HEADROOM} blocks above natural ground.
 */
class CraterBasinTest {

    private static final int X = 1000;
    private static final int Z = -500;

    /** The paraboloid at a column, for a basin's center. */
    private static double cap(CraterBasin basin, int x, int z) {
        double dx = x - X;
        double dz = z - Z;
        return basin.capCenterY() + CraterBasin.CURVATURE * (dx * dx + dz * dz);
    }

    /** Asserts that every column outside the footprint, out to 100 blocks, keeps a city's headroom under the cap. */
    private static void assertHeadroomOutside(CraterBasin basin, TerrainHeightOracle heights) {
        for (int x = X - 100; x <= X + 100; x++) {
            for (int z = Z - 100; z <= Z + 100; z++) {
                if (Math.abs(x - X) <= basin.halfWidth() && Math.abs(z - Z) <= basin.halfWidth()) {
                    continue;
                }
                assertTrue(
                    cap(basin, x, z) >= heights.height(x, z) + CraterBasin.CITY_HEADROOM,
                    "column " + x + "," + z + " of " + basin);
            }
        }
    }

    @Test
    void flatGround() {
        TerrainHeightOracle flat = (x, z) -> 70;
        MeteoriteShape shape = MeteoriteShape.of(-7543508780968384435L); // size 5.91
        CraterBasin basin = CraterBasin.of(flat, X, Z, shape);
        // 70 - 10 - 5.91 + 1 = 55.09, rounded down, less a block of slack.
        assertEquals(54, basin.capCenterY());
        assertEquals(70, basin.groundEstimate());
        assertTrue(basin.halfWidth() >= CraterBasin.DECAY_HALF_WIDTH, basin.toString());
        assertTrue(basin.halfWidth() <= CraterBasin.MAX_HALF_WIDTH, basin.toString());
        assertHeadroomOutside(basin, flat);
    }

    @Test
    void theCraterIsNeverWiderThanTheFootprint() {
        TerrainHeightOracle flat = (x, z) -> 64;
        for (long seed = 1; seed < 200; seed++) {
            MeteoriteShape shape = MeteoriteShape.of(seed);
            CraterBasin basin = CraterBasin.of(flat, X, Z, shape);
            assertTrue(basin.halfWidth() >= shape.craterSize(), basin + " " + shape);
            assertHeadroomOutside(basin, flat);
        }
    }

    @Test
    void aHillNearbyGrowsTheFootprint() {
        // A hill 20 blocks high whose crest stands 45 blocks east of the impact.
        TerrainHeightOracle hill = (x, z) -> 70 + (int) Math.max(0, 20 - Math.abs(x - (X + 45)));
        CraterBasin flat = CraterBasin.of((x, z) -> 70, X, Z, MeteoriteShape.of(7L));
        CraterBasin basin = CraterBasin.of(hill, X, Z, MeteoriteShape.of(7L));
        assertTrue(basin.halfWidth() > flat.halfWidth(), basin + " against " + flat);
        assertHeadroomOutside(basin, hill);
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
        assertTrue(steps > 500, "a zero budget reads one sample per step: " + steps);
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
    void groundEstimateIsTheMedianOverTheFootprint() {
        TerrainHeightOracle half = (x, z) -> x < X ? 60 : 80;
        CraterBasin basin = CraterBasin.of(half, X, Z, MeteoriteShape.of(3L));
        assertTrue(basin.groundEstimate() == 60 || basin.groundEstimate() == 80, basin.toString());
    }
}
