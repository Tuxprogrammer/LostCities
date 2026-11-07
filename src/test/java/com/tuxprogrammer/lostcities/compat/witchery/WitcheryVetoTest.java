package com.tuxprogrammer.lostcities.compat.witchery;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * The Witchery veto skips a chunk's structure exactly when its union lot's chunks meet the city or a scattered
 * footprint (D-landmark-policy-3).
 */
class WitcheryVetoTest {

    /** A layout of named city and scattered chunks. */
    private static final class Fixture implements WitcheryVeto.Layout {

        final Set<Long> city = new HashSet<>();
        final Set<Long> scattered = new HashSet<>();

        static long key(int x, int z) {
            return ((long) x << 32) ^ (z & 0xffffffffL);
        }

        @Override
        public boolean isCity(int chunkX, int chunkZ) {
            return city.contains(key(chunkX, chunkZ));
        }

        @Override
        public boolean isScattered(int chunkX, int chunkZ) {
            return scattered.contains(key(chunkX, chunkZ));
        }
    }

    @Test
    void noCityNoVeto() {
        assertFalse(WitcheryVeto.vetoes(-46, -160, new Fixture()));
    }

    @Test
    void cityUnderTheChunkOrItsWestNorthNeighborsVetoes() {
        // The union lot spans blocks -9 to +13 from the chunk's corner: the chunk, x - 1, z - 1 and the diagonal.
        int[][] under = { { 0, 0 }, { -1, 0 }, { 0, -1 }, { -1, -1 } };
        for (int[] offset : under) {
            Fixture layout = new Fixture();
            layout.city.add(Fixture.key(10 + offset[0], -20 + offset[1]));
            assertTrue(WitcheryVeto.vetoes(10, -20, layout), offset[0] + "," + offset[1]);
        }
    }

    @Test
    void cityOutsideTheLotDoesNotVeto() {
        int[][] outside = { { 1, 0 }, { 0, 1 }, { 1, 1 }, { -2, 0 }, { 0, -2 }, { 1, -1 }, { -1, 1 } };
        for (int[] offset : outside) {
            Fixture layout = new Fixture();
            layout.city.add(Fixture.key(10 + offset[0], -20 + offset[1]));
            assertFalse(WitcheryVeto.vetoes(10, -20, layout), offset[0] + "," + offset[1]);
        }
    }

    @Test
    void scatteredFootprintVetoesLikeACity() {
        Fixture layout = new Fixture();
        layout.scattered.add(Fixture.key(-1, -1));
        assertTrue(WitcheryVeto.vetoes(0, 0, layout));
    }

    @Test
    void lotStaysInsideTheFourChunksForThePacksSettings() {
        int[] offsets = WitcherySettings.pack()
            .unionOffsets();
        assertTrue(offsets[0] >= -16 && offsets[1] >= -16, "the lot starts no farther west or north than one chunk");
        assertTrue(offsets[2] <= 15 && offsets[3] <= 15, "the lot ends inside the chunk itself");
    }
}
