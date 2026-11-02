package com.tuxprogrammer.lostcities.compat.lootgames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The rhomb grid against sites recorded from LootGames' own {@code canSpawnInChunk_v3} (as written at 2.2.14, run
 * outside the game) for seed 4242, rhomb size 20, chunks -60 to 59 on both axes.
 */
class RhombGridTest {

    @Test
    void sitesMatchLootGames() {
        RhombGrid grid = new RhombGrid(4242L, 20);
        List<String> sites = new ArrayList<>();
        for (int chunkX = -60; chunkX < 60; chunkX++) {
            for (int chunkZ = -60; chunkZ < 60; chunkZ++) {
                if (grid.spawnsIn(chunkX, chunkZ)) {
                    sites.add(chunkX + "," + chunkZ);
                }
            }
        }
        assertEquals(22, sites.size(), sites.toString());
        assertEquals(
            Arrays.asList(
                "-51,10",
                "-51,38",
                "-41,-40",
                "-41,-10",
                "-37,11",
                "-29,33",
                "-27,-37",
                "-27,-9",
                "-23,-59",
                "-16,6",
                "-16,46",
                "-13,-36"),
            sites.subList(0, 12));
    }

    @Test
    void theAnswerDoesNotDependOnWhatWasAskedBefore() {
        RhombGrid grid = new RhombGrid(77L, 20);
        boolean first = grid.spawnsIn(-37, 11);
        grid.spawnsIn(500, -500);
        assertEquals(first, grid.spawnsIn(-37, 11));
    }

    @Test
    void aboutOneSitePerFiveHundredChunks() {
        RhombGrid grid = new RhombGrid(123456789L, 20);
        int sites = 0;
        for (int chunkX = 0; chunkX < 400; chunkX++) {
            for (int chunkZ = 0; chunkZ < 400; chunkZ++) {
                sites += grid.spawnsIn(chunkX, chunkZ) ? 1 : 0;
            }
        }
        // 160,000 chunks; LootGames' rhombs of 40 x 40 rhomb cells hold one site each (coexistence.md: about one per
        // 533 chunks).
        assertTrue(sites > 200 && sites < 400, "sites: " + sites);
    }
}
