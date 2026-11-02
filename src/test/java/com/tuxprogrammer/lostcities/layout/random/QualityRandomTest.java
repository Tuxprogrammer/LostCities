package com.tuxprogrammer.lostcities.layout.random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.ParityData;

/** {@link QualityRandom} reproduces upstream's generator bit for bit, and its draw counter replays a stream. */
public class QualityRandomTest {

    @Test
    public void matchesValuesRecordedFromUpstream() throws Exception {
        int compared = RandomDraws.check(ParityData.records("quality_random.txt"), QualityRandom::new);
        assertTrue(compared > 500, "only " + compared + " values compared");
    }

    @Test
    public void skippingTheDrawCountReachesTheSameState() {
        QualityRandom a = new QualityRandom(1234L);
        a.nextFloat();
        a.nextDouble();
        a.nextInt(7);
        a.nextBoolean();
        assertEquals(5, a.drawCount(), "nextDouble draws two values, the others one each");
        QualityRandom b = new QualityRandom(1234L);
        b.skip(a.drawCount());
        for (int i = 0; i < 20; i++) {
            assertEquals(a.nextLong(), b.nextLong());
        }
    }

    @Test
    public void seedingDoesNotCountAsADraw() {
        assertEquals(0, new QualityRandom(99L).drawCount());
    }
}
