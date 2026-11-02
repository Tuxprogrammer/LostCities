package com.tuxprogrammer.lostcities.worldgen.buffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.BlockRef;

/** The array-backed chunk buffer: starts as air, stores what it is given, and refuses to write outside its chunk. */
public class ArrayChunkBufferTest {

    private static final BlockRef STONE = BlockRef.of("minecraft:stone", 0);

    @Test
    public void startsAsAir() {
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        assertSame(BlockRef.AIR, buffer.get(0, 0, 0));
        assertSame(BlockRef.AIR, buffer.get(15, 255, 15));
    }

    @Test
    public void storesEachPositionSeparately() {
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        buffer.set(3, 70, 12, STONE);
        assertEquals(STONE, buffer.get(3, 70, 12));
        assertSame(BlockRef.AIR, buffer.get(12, 70, 3));
        assertSame(BlockRef.AIR, buffer.get(3, 71, 12));
    }

    @Test
    public void indexIsYThenZThenX() {
        assertEquals(0, ArrayChunkBuffer.index(0, 0, 0));
        assertEquals(1, ArrayChunkBuffer.index(1, 0, 0));
        assertEquals(16, ArrayChunkBuffer.index(0, 0, 1));
        assertEquals(256, ArrayChunkBuffer.index(0, 1, 0));
        assertEquals(ArrayChunkBuffer.VOLUME - 1, ArrayChunkBuffer.index(15, 255, 15));
    }

    @Test
    public void refusesPositionsOutsideTheChunk() {
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        assertThrows(IndexOutOfBoundsException.class, () -> buffer.set(16, 0, 0, STONE));
        assertThrows(IndexOutOfBoundsException.class, () -> buffer.set(0, 0, -1, STONE));
        assertThrows(IndexOutOfBoundsException.class, () -> buffer.set(0, 256, 0, STONE));
        assertThrows(IndexOutOfBoundsException.class, () -> buffer.get(0, -1, 0));
    }

    @Test
    public void refusesNull() {
        assertThrows(NullPointerException.class, () -> new ArrayChunkBuffer().set(0, 0, 0, null));
    }
}
