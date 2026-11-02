package com.tuxprogrammer.lostcities.worldgen.buffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.BlockRef;

/**
 * The digest's text form and hash. The expected hashes here were computed outside Java, with {@code sha256sum} over
 * the text shown, so they check the format itself and not only its stability.
 */
public class ChunkBufferDigestTest {

    private static final BlockRef STONE = BlockRef.of("minecraft:stone", 0);
    private static final BlockRef BRICKS = BlockRef.of("minecraft:stonebrick", 2);

    @Test
    public void emptyBufferIsOnlyThePalette() {
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        assertEquals("lostcitiesnh-chunk-buffer v1\npalette 1\n0 minecraft:air:0\n", ChunkBufferDigest.text(buffer));
        assertEquals(
            "5800444d3402d95246e19faa0f7844ccaedbbeb090fdecb92f4ff557053baa4c",
            ChunkBufferDigest.sha256(buffer));
    }

    @Test
    public void oneBlockGivesTheHandComputedText() {
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        buffer.set(0, 0, 0, STONE);
        assertEquals(
            "lostcitiesnh-chunk-buffer v1\npalette 2\n0 minecraft:air:0\n1 minecraft:stone:0\nlayer 0 1*1 255*0\n",
            ChunkBufferDigest.text(buffer));
        assertEquals(
            "b670873f7768deb6046a1c68908f75dcad469ec7bc5cc350aec36df564873dbc",
            ChunkBufferDigest.sha256(buffer));
    }

    @Test
    public void paletteFollowsFirstAppearanceAndRunsFollowZThenX() {
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        // Bricks appear first in scan order (y 3), stone later (y 5), whatever the order of the writes.
        buffer.set(1, 5, 0, STONE);
        buffer.set(15, 3, 0, BRICKS);
        buffer.set(0, 3, 1, BRICKS);
        assertEquals(
            "lostcitiesnh-chunk-buffer v1\npalette 3\n0 minecraft:air:0\n1 minecraft:stonebrick:2\n"
                + "2 minecraft:stone:0\nlayer 3 15*0 2*1 239*0\nlayer 5 1*0 1*2 254*0\n",
            ChunkBufferDigest.text(buffer));
    }

    @Test
    public void anyChangedBlockChangesTheHash() {
        ArrayChunkBuffer a = new ArrayChunkBuffer();
        ArrayChunkBuffer b = new ArrayChunkBuffer();
        a.set(7, 100, 7, STONE);
        b.set(7, 100, 7, BlockRef.of("minecraft:stone", 1));
        assertNotEquals(ChunkBufferDigest.sha256(a), ChunkBufferDigest.sha256(b));
    }

    @Test
    public void writeOrderDoesNotMatter() {
        ArrayChunkBuffer a = new ArrayChunkBuffer();
        ArrayChunkBuffer b = new ArrayChunkBuffer();
        a.set(1, 1, 1, STONE);
        a.set(2, 2, 2, BRICKS);
        b.set(2, 2, 2, BRICKS);
        b.set(1, 1, 1, STONE);
        assertEquals(ChunkBufferDigest.text(a), ChunkBufferDigest.text(b));
    }
}
