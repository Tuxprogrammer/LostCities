package com.tuxprogrammer.lostcities.compat.roguelike;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** The steering rule over a fake layout (0006 §5, D31). */
class SteeringRuleTest {

    /** A layout of named city, edge-ring and scattered chunks and predicted sites. */
    private static final class FakeLayout implements SteeringRule.Layout {

        final Set<Long> city = new HashSet<>();
        final Set<Long> edge = new HashSet<>();
        final Set<Long> scattered = new HashSet<>();
        final Set<Long> predicted = new HashSet<>();

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
}
