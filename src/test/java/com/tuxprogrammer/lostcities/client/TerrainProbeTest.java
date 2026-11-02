package com.tuxprogrammer.lostcities.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

/** Reading Celeritas's pending sections from its debug strings (the format in Angelica 2.2.43). */
public class TerrainProbeTest {

    @Test
    public void sumsUploadsAndEveryRebuildList() {
        assertEquals(
            3 + 12 + 0 + 7,
            TerrainProbe.pendingFromDebug(
                Arrays.asList(
                    "Transfer Queue: staging",
                    "Chunk Queues: U=03 (P0=012 | P1=000 | P2=007) S=000/000",
                    "Other line")));
        assertEquals(
            0,
            TerrainProbe.pendingFromDebug(
                Collections.singletonList("Chunk Queues: U=00 (P0=000 | P1=000 | P2=000) S=004/120")));
    }

    @Test
    public void unknownWithoutTheLine() {
        assertEquals(-1, TerrainProbe.pendingFromDebug(Collections.singletonList("Chunk Builder: Busy=0")));
        assertEquals(-1, TerrainProbe.pendingFromDebug(Collections.emptyList()));
        assertEquals(-1, TerrainProbe.pendingFromDebug(null));
    }
}
