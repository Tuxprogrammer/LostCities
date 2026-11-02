package com.tuxprogrammer.lostcities.worldgen.buffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.BlockRef;

/**
 * Golden chunks for fixed seeds and chunk coordinates (CONTRIBUTING § Testing), and the harness's own behavior. Until
 * the layout and the overlay exist, the generator is {@link GoldenFixtureTerrain}; M03 to M05 add goldens for real
 * city chunks through the same {@link GoldenChunks#assertMatches}.
 */
public class GoldenChunkTest {

    /** The seed the GTNH CI workflow's {@code runServer} step uses. */
    static final long CI_SEED = -6202107849386030209L;

    @Test
    public void seedZeroAtTheOrigin() throws Exception {
        GoldenChunks.assertMatches("fixture_seed0_0_0", GoldenFixtureTerrain.generate(0L, 0, 0));
    }

    @Test
    public void seed42AtNegativeX() throws Exception {
        GoldenChunks.assertMatches("fixture_seed42_m1_5", GoldenFixtureTerrain.generate(42L, -1, 5));
    }

    @Test
    public void ciSeedFarFromSpawn() throws Exception {
        GoldenChunks.assertMatches("fixture_seedci_1000_m1000", GoldenFixtureTerrain.generate(CI_SEED, 1000, -1000));
    }

    @Test
    public void generationOrderDoesNotMatter() {
        String first = ChunkBufferDigest.sha256(GoldenFixtureTerrain.generate(42L, -1, 5));
        GoldenFixtureTerrain.generate(42L, 7, 7);
        GoldenFixtureTerrain.generate(0L, -1, 5);
        assertEquals(first, ChunkBufferDigest.sha256(GoldenFixtureTerrain.generate(42L, -1, 5)));
    }

    @Test
    public void harnessReportsAChangedBlock() {
        ArrayChunkBuffer buffer = GoldenFixtureTerrain.generate(0L, 0, 0);
        String expected = ChunkBufferDigest.sha256(buffer);
        assertNull(GoldenChunks.mismatch(expected, ChunkBufferDigest.text(buffer)));
        buffer.set(8, 200, 8, BlockRef.of("minecraft:glass", 0));
        assertNotNull(GoldenChunks.mismatch(expected, ChunkBufferDigest.text(buffer)));
    }

    @Test
    public void goldenFileSkipsCommentsAndRejectsJunk() {
        String hash = "dc724052ed16e224d0088f255a4032b8083c10991d13da3da2451e0429f127f3";
        assertEquals(hash, GoldenChunks.expectedHash(Arrays.asList("# why", "", hash)));
        assertThrows(IllegalStateException.class, () -> GoldenChunks.expectedHash(Arrays.asList("# only a comment")));
        assertThrows(IllegalStateException.class, () -> GoldenChunks.expectedHash(Arrays.asList("not-a-hash")));
    }
}
