package com.tuxprogrammer.lostcities.compat.ae2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

/**
 * The meteorite grid against values recorded from AE2's own formulas ({@code MeteoriteWorldGen.generate},
 * {@code Platform.seedFromGrid}, as written at rv3-beta-1095-GTNH, run outside the game for seed 4242 with the pack's
 * {@code 0=707} and {@code 0=0.3}), and the settings against AE2's config loops.
 */
class MeteoriteGridTest {

    private static final String[] PACK_DISTANCE = { "0=707" };
    private static final String[] PACK_CHANCE = { "0=0.3" };

    private static MeteoriteGrid packGrid() {
        return new MeteoriteGrid(4242L, MeteoriteSettings.of(true, 0, PACK_DISTANCE, PACK_CHANCE));
    }

    @Test
    void packSettings() {
        MeteoriteSettings settings = MeteoriteSettings.of(true, 0, PACK_DISTANCE, PACK_CHANCE);
        assertTrue(settings.enabled());
        assertEquals(707, settings.cellSize());
        assertEquals(Arrays.asList(0.3D), settings.chanceDraws());
        assertEquals(70, MeteoriteSettings.margin(707));
    }

    @Test
    void sitesMatchAe2() {
        MeteoriteGrid grid = packGrid();
        assertSite(grid.site(0, 0), 286, 473, 184, -7543508780968384435L);
        assertSite(grid.site(0, -2), 488, -851, 128, 5058118976279521559L);
        assertSite(grid.site(-4, 1), -2360, 842, 128, -8045570049540908799L);
        assertSite(grid.site(4, 6), 2902, 4571, 199, 3532439230978648689L);
    }

    private static void assertSite(MeteoriteGrid.Site site, int x, int z, int startY, long seed) {
        assertEquals(x, site.x(), site.toString());
        assertEquals(z, site.z(), site.toString());
        assertEquals(startY, site.startY(), site.toString());
        assertEquals(seed, site.seed(), site.toString());
        assertTrue(site.falls(), site.toString());
    }

    @Test
    void siteIsInsideItsCellMargin() {
        MeteoriteGrid grid = packGrid();
        for (int cellX = -5; cellX <= 5; cellX++) {
            for (int cellZ = -5; cellZ <= 5; cellZ++) {
                MeteoriteGrid.Site site = grid.site(cellX, cellZ);
                assertEquals(cellX, grid.cellOf(site.x()));
                assertEquals(cellZ, grid.cellOf(site.z()));
                assertTrue(Math.floorMod(site.x(), 707) >= 70 && Math.floorMod(site.x(), 707) < 637, site.toString());
                assertTrue(site.startY() == 128 || site.startY() >= 180 && site.startY() < 200, site.toString());
                // With a 70-block margin the meteor chunk's corner is always in the same cell.
                assertTrue(site.falls(), site.toString());
            }
        }
    }

    @Test
    void sameCellSameSiteWhateverWasAskedBefore() {
        MeteoriteGrid grid = packGrid();
        long seed = grid.site(3, -2)
            .seed();
        grid.site(-9, 9);
        assertEquals(
            seed,
            grid.site(3, -2)
                .seed());
    }

    @Test
    void chanceDrawsFollowAe2sLoop() {
        // Dimension 7 after an overworld entry: AE2 draws once for "0" (nothing drawn yet), then once for "7".
        MeteoriteSettings settings = MeteoriteSettings
            .of(true, 7, new String[] { "7=300" }, new String[] { "0=0.3", "7=0.5" });
        assertEquals(Arrays.asList(0.3D, 0.5D), settings.chanceDraws());
        // The dimension's own distance is clamped to at least 8.
        assertEquals(
            8,
            MeteoriteSettings.of(true, 7, new String[] { "7=2" }, PACK_CHANCE)
                .cellSize());
        // The overworld's distance stands in for a dimension without one, unclamped.
        assertEquals(
            707,
            MeteoriteSettings.of(true, 7, PACK_DISTANCE, PACK_CHANCE)
                .cellSize());
        // Overworld: its own entry draws, and the fallback branch then sees a draw made and draws nothing more.
        assertEquals(
            Arrays.asList(0.3D),
            MeteoriteSettings.of(true, 0, PACK_DISTANCE, PACK_CHANCE)
                .chanceDraws());
    }

    @Test
    void offWhenAe2WouldNotGenerate() {
        assertSame(MeteoriteSettings.OFF, MeteoriteSettings.of(false, 0, PACK_DISTANCE, PACK_CHANCE));
        assertSame(MeteoriteSettings.OFF, MeteoriteSettings.of(true, 0, new String[] { "7=707" }, PACK_CHANCE));
        assertSame(MeteoriteSettings.OFF, MeteoriteSettings.of(true, 0, PACK_DISTANCE, new String[] { "7=0.3" }));
        assertSame(MeteoriteSettings.OFF, MeteoriteSettings.of(true, 0, new String[] { "0=far" }, PACK_CHANCE));
        assertFalse(MeteoriteSettings.OFF.enabled());
        assertThrows(IllegalArgumentException.class, () -> new MeteoriteGrid(1L, MeteoriteSettings.OFF));
    }

    @Test
    void smallCellsCanHoldSitesThatNeverFall() {
        MeteoriteGrid grid = new MeteoriteGrid(
            99L,
            MeteoriteSettings.of(true, 0, new String[] { "0=20" }, PACK_CHANCE));
        int never = 0;
        for (int cellX = -20; cellX <= 20; cellX++) {
            for (int cellZ = -20; cellZ <= 20; cellZ++) {
                MeteoriteGrid.Site site = grid.site(cellX, cellZ);
                boolean expected = grid.cellOf(site.x() >> 4 << 4) == cellX && grid.cellOf(site.z() >> 4 << 4) == cellZ;
                assertEquals(expected, site.falls());
                never += site.falls() ? 0 : 1;
            }
        }
        assertTrue(never > 0, "a 20-block cell must have sites whose chunk corner lies in another cell");
    }
}
