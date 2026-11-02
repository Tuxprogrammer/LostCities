package com.tuxprogrammer.lostcities.worldgen.terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.worldgen.buffer.ArrayChunkBuffer;

/** Upstream's torch attachment rule. */
public class TorchRuleTest {

    private static final BlockRef WALL = BlockRef.of("minecraft:stonebrick", 0);

    @Test
    public void standsOnABlockBelow() {
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        buffer.set(5, 69, 5, WALL);
        assertEquals(TorchRule.STANDING, TorchRule.metaFor(buffer, 5, 70, 5));
    }

    @Test
    public void attachesToTheFirstSideInUpstreamsOrder() {
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        buffer.set(6, 70, 5, WALL);
        buffer.set(5, 70, 6, WALL);
        assertEquals(TorchRule.ON_EAST, TorchRule.metaFor(buffer, 5, 70, 5), "east before south");
        buffer.set(4, 70, 5, WALL);
        assertEquals(TorchRule.ON_WEST, TorchRule.metaFor(buffer, 5, 70, 5), "west first");
    }

    @Test
    public void looksOnlyInsideTheChunk() {
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        assertEquals(TorchRule.KEEP, TorchRule.metaFor(buffer, 0, 70, 0));
    }
}
