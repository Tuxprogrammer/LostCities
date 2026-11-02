package com.tuxprogrammer.lostcities.statics.fixtures;

import java.util.Random;

/** Fixture for {@code LayoutPurityTest}: layout-like code that takes only plain data. Never run. */
public class PureLayout {

    /** A seeded decision from a seed and chunk coordinates. */
    public boolean isCity(long seed, int chunkX, int chunkZ) {
        return new Random(seed ^ ((long) chunkX << 32) ^ chunkZ).nextInt(10) == 0;
    }
}
