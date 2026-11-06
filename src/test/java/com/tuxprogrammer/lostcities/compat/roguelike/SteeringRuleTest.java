package com.tuxprogrammer.lostcities.compat.roguelike;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** The steering rule over a fake layout (0006 §5, D31, D361). */
class SteeringRuleTest {

    /** A layout of named city, edge-ring and scattered chunks and predicted sites. */
    private static final class FakeLayout implements SteeringRule.Layout {

        final Set<Long> city = new HashSet<>();
        final Set<Long> edge = new HashSet<>();
        final Set<Long> scattered = new HashSet<>();
        final Set<Long> predicted = new HashSet<>();
        final Set<Long> track = new HashSet<>();

        static long key(int a, int b) {
            return (long) a << 32 | b & 0xFFFFFFFFL;
        }

        @Override
        public boolean isCity(int chunkX, int chunkZ) {
            return city.contains(key(chunkX, chunkZ));
        }

        @Override
        public boolean isEdgeRing(int chunkX, int chunkZ) {
            return edge.contains(key(chunkX, chunkZ));
        }

        @Override
        public boolean isScattered(int chunkX, int chunkZ) {
            return scattered.contains(key(chunkX, chunkZ));
        }

        @Override
        public boolean isPredictedSite(int blockX, int blockZ) {
            return predicted.contains(key(blockX, blockZ));
        }

        @Override
        public boolean hasLinkTrack(int chunkX, int chunkZ) {
            return track.contains(key(chunkX, chunkZ));
        }
    }

    @Test
    void openCountryIsClear() {
        assertEquals(SteeringRule.Outcome.CLEAR, SteeringRule.decide(100, 100, new FakeLayout()));
        assertFalse(SteeringRule.Outcome.CLEAR.refuses());
    }

    @Test
    void aLotTouchingACityChunkIsRefused() {
        FakeLayout layout = new FakeLayout();
        // Point (40, 40): the lot spans blocks 30..50, chunks 1..3 on both axes.
        layout.city.add(FakeLayout.key(3, 1));
        assertEquals(SteeringRule.Outcome.CITY, SteeringRule.decide(40, 40, layout));
        assertTrue(SteeringRule.Outcome.CITY.refuses());
        // Chunk 4 lies past the lot.
        FakeLayout beyond = new FakeLayout();
        beyond.city.add(FakeLayout.key(4, 2));
        assertEquals(SteeringRule.Outcome.CLEAR, SteeringRule.decide(40, 40, beyond));
    }

    @Test
    void lotsSpanChunksAtNegativeCoordinates() {
        FakeLayout layout = new FakeLayout();
        // Point (−5, −5): the lot spans blocks −15..5, chunks −1..0.
        layout.city.add(FakeLayout.key(-1, 0));
        assertEquals(SteeringRule.Outcome.CITY, SteeringRule.decide(-5, -5, layout));
        FakeLayout far = new FakeLayout();
        far.city.add(FakeLayout.key(-2, 0));
        assertEquals(SteeringRule.Outcome.CLEAR, SteeringRule.decide(-5, -5, far));
    }

    @Test
    void edgeRingsAndScatteredFootprintsAreRefused() {
        FakeLayout edge = new FakeLayout();
        edge.edge.add(FakeLayout.key(2, 2));
        assertEquals(SteeringRule.Outcome.EDGE_RING, SteeringRule.decide(40, 40, edge));
        FakeLayout scattered = new FakeLayout();
        scattered.scattered.add(FakeLayout.key(2, 2));
        assertEquals(SteeringRule.Outcome.SCATTERED, SteeringRule.decide(40, 40, scattered));
    }

    @Test
    void thePredictedSiteKeepsItsReservedLot() {
        FakeLayout layout = new FakeLayout();
        layout.edge.add(FakeLayout.key(1, 1));
        layout.predicted.add(FakeLayout.key(40, 40));
        assertEquals(SteeringRule.Outcome.PREDICTED, SteeringRule.decide(40, 40, layout));
        assertFalse(SteeringRule.Outcome.PREDICTED.refuses());
        // A point one block off is not the site.
        assertEquals(SteeringRule.Outcome.EDGE_RING, SteeringRule.decide(41, 40, layout));
    }

    @Test
    void aPredictedSiteTheCityCoversIsRefused() {
        // Under the IGNORE treatment the city builds over the predicted lot.
        FakeLayout layout = new FakeLayout();
        layout.city.add(FakeLayout.key(2, 2));
        layout.predicted.add(FakeLayout.key(40, 40));
        assertEquals(SteeringRule.Outcome.CITY, SteeringRule.decide(40, 40, layout));
    }

    @Test
    void linkTrackWithinTheLevelsReachIsRefusedEvenAtAPredictedSite() {
        FakeLayout layout = new FakeLayout();
        // Point (8, 8): 122 blocks east is block 130, in chunk 8.
        layout.track.add(FakeLayout.key(8, 0));
        assertEquals(SteeringRule.Outcome.RAIL, SteeringRule.decide(8, 8, layout));
        assertTrue(SteeringRule.Outcome.RAIL.refuses());
        layout.predicted.add(FakeLayout.key(8, 8));
        layout.edge.add(FakeLayout.key(0, 0));
        assertEquals(
            SteeringRule.Outcome.RAIL,
            SteeringRule.decide(8, 8, layout),
            "the reserved lot does not excuse it");
        // Chunk 9 starts at block 144, 136 blocks away; and a diagonal chunk past the circle is out of reach too.
        FakeLayout beyond = new FakeLayout();
        beyond.track.add(FakeLayout.key(9, 0));
        beyond.track.add(FakeLayout.key(7, 7));
        assertEquals(SteeringRule.Outcome.CLEAR, SteeringRule.decide(8, 8, beyond));
    }

    /**
     * Seed 4242, rarecities on RWG2 Continents (real-pack {@code network.railway}, rounds 1 and 2): a Roguelike tower's
     * top level, spawner at (-713, 50, -30), cut the Y 54 tunnel of the line from chunk (-46, -17) to (-40, 27) at
     * x = -45, z -4..1, though D359 blocked lines under the 21 × 21 lot. Every candidate site of the pack's grid
     * from which a room could reach that spawner is now refused while the line has track there.
     */
    @Test
    void seed4242TowerIsSteeredOffTheLinkLine() {
        RoguelikeGrid grid = new RoguelikeGrid(4242L, RoguelikeSettings.pack(), new Random());
        int spawnerX = -713;
        int spawnerZ = -30;
        long reach = RoguelikeOracle.LEVEL_REACH;
        List<int[]> sites = new ArrayList<>();
        for (int cellX = grid.cellOf(-45) - 1; cellX <= grid.cellOf(-45) + 1; cellX++) {
            for (int cellZ = grid.cellOf(-2) - 1; cellZ <= grid.cellOf(-2) + 1; cellZ++) {
                int[] chunk = grid.dungeonChunk(cellX, cellZ);
                for (int[] c : grid.candidates(chunk[0], chunk[1])) {
                    long dx = c[0] - spawnerX;
                    long dz = c[1] - spawnerZ;
                    if (dx * dx + dz * dz < reach * reach) {
                        sites.add(c);
                    }
                }
            }
        }
        assertFalse(sites.isEmpty(), "the pack's grid has candidate sites within reach of the spawner");
        FakeLayout layout = new FakeLayout();
        for (int z = -4; z <= 1; z++) {
            layout.track.add(FakeLayout.key(-45, z));
        }
        for (int[] site : sites) {
            assertEquals(
                SteeringRule.Outcome.RAIL,
                SteeringRule.decide(site[0], site[1], layout),
                "site " + site[0] + "," + site[1]);
        }
        // Without the line the same sites are Roguelike's to decide.
        for (int[] site : sites) {
            assertFalse(
                SteeringRule.decide(site[0], site[1], new FakeLayout())
                    .refuses());
        }
    }
}
