package com.tuxprogrammer.lostcities.worldgen.overlay;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** The sky light deferral skips only the asked chunk's pass, reports it owed, and never nests. */
public class SkylightDeferralTest {

    @AfterEach
    public void clear() {
        SkylightDeferral.end();
    }

    @Test
    public void skipsOnlyTheAskedChunkWhileActive() {
        assertFalse(SkylightDeferral.skips(null, 3, 4), "nothing is deferred outside a provideChunk");
        assertTrue(SkylightDeferral.begin(null, 3, 4));
        assertFalse(SkylightDeferral.skips(null, 3, 5), "another chunk is lit as usual");
        assertTrue(SkylightDeferral.skips(null, 3, 4));
        assertTrue(SkylightDeferral.end(), "the skipped pass is owed");
        assertFalse(SkylightDeferral.skips(null, 3, 4), "ended");
    }

    @Test
    public void owesNothingWhenTheDelegateNeverLit() {
        assertTrue(SkylightDeferral.begin(null, 0, 0));
        assertFalse(SkylightDeferral.end());
    }

    @Test
    public void aNestedCallDoesNotDefer() {
        assertTrue(SkylightDeferral.begin(null, 0, 0));
        assertFalse(SkylightDeferral.begin(null, 1, 0), "the nested call is not the owner");
        assertFalse(SkylightDeferral.skips(null, 1, 0), "the nested chunk is lit as usual");
        assertTrue(SkylightDeferral.skips(null, 0, 0));
        assertTrue(SkylightDeferral.end());
    }
}
