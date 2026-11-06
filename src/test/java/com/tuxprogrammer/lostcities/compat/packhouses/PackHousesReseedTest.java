package com.tuxprogrammer.lostcities.compat.packhouses;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Random;

import org.junit.jupiter.api.Test;

/** The world's shared random is a function of the piece seed during a pack house build. */
public class PackHousesReseedTest {

    @Test
    public void sameSeedGivesTheSameDrawsWhateverTheRandomHeldBefore() {
        Random a = new Random(1);
        Random b = new Random(99);
        a.nextLong();
        PackHouses.reseed(a, 12345L);
        PackHouses.reseed(b, 12345L);
        for (int i = 0; i < 20; i++) {
            assertEquals(a.nextInt(1000), b.nextInt(1000));
        }
    }

    @Test
    public void differentPiecesDrawDifferently() {
        Random a = new Random();
        Random b = new Random();
        PackHouses.reseed(a, 1L);
        PackHouses.reseed(b, 2L);
        assertNotEquals(a.nextLong(), b.nextLong());
    }
}
