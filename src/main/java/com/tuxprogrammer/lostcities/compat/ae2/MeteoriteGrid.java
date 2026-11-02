package com.tuxprogrammer.lostcities.compat.ae2;

import java.util.Random;

/**
 * Where AE2 puts the meteorite of a grid cell: one candidate per {@code cellSize}-block cell, drawn from the world seed
 * and the cell alone, as {@code MeteoriteWorldGen.generate} draws it.
 *
 * <ol>
 * <li>The cell of a chunk is {@code floorDiv(chunkX << 4, cellSize)} (and Z).</li>
 * <li>The FML generator {@code Random} is reseeded by {@code Platform.seedFromGrid}: {@code setSeed(worldSeed)},
 * {@code xSeed = nextLong() >> 3}, {@code zSeed = nextLong() >> 3} (the source's {@code >> 2 + 1L}),
 * {@code setSeed((xSeed × cellX + zSeed × cellZ) ^ worldSeed)}. The reseed is complete, so only the class of that
 * {@code Random} matters: FML's {@code GameRegistry.generateWorld} creates a plain {@code java.util.Random}, which no
 * mod in the pack replaces (Hodgepodge's {@code StdLCG} only enters {@code MapGenBase}, {@code MapGenCaves} and
 * {@code ChunkProviderGenerate}; CoFH Core's patch of {@code generateWorld} only adds hooks around it).</li>
 * <li>One {@code nextDouble()} per spawn-chance draw ({@link MeteoriteSettings#chanceDraws()}); the last decides a
 * surface meteorite.</li>
 * <li>{@code x = cellX × cellSize + nextInt(cellSize − 2 × margin) + margin}, then Z the same way, with
 * {@code margin = max(1, cellSize / 10)}.</li>
 * <li>{@code depth = 180 + nextInt(20)}: the start height of a surface meteorite; any other starts at Y 128.</li>
 * <li>The meteor seed: {@code nextLong()}, drawn again while it is 0.</li>
 * </ol>
 * The meteorite only falls when the chunk that holds {@code (x, z)} is the one generating, and that chunk's own cell
 * is computed from its minimum corner; a site whose chunk corner lies in another cell (possible only for cells under
 * 160 blocks) never falls, and {@link Site#falls()} says so.
 *
 * <p>
 * Source:
 * https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/rv3-beta-1095-GTNH/src/main/java/appeng/worldgen/MeteoriteWorldGen.java#L36-L99
 * and
 * https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/rv3-beta-1095-GTNH/src/main/java/appeng/util/Platform.java#L221-L227;
 * Forge 10.13.4.1614 {@code GameRegistry.generateWorld} (no pinned public source; read from the pack's server).
 *
 * <p>
 * Not thread-safe (one {@code Random}); the oracle synchronizes. Side: both (plain Java).
 */
public final class MeteoriteGrid {

    /** Start height of a meteorite whose chance draw failed. */
    public static final int LOW_START_Y = 128;
    /** Lowest start height of a surface meteorite. */
    public static final int SURFACE_START_Y = 180;
    /** Spread of a surface meteorite's start height. */
    static final int SURFACE_START_SPREAD = 20;
    /** The source's {@code >> 2 + 1L}, which Java parses as a shift by 3. */
    private static final int SEED_SHIFT = 3;

    /** One cell's candidate meteorite. */
    public static final class Site {

        private final int cellX;
        private final int cellZ;
        private final int x;
        private final int z;
        private final int startY;
        private final long seed;
        private final boolean falls;

        Site(int cellX, int cellZ, int x, int z, int startY, long seed, boolean falls) {
            this.cellX = cellX;
            this.cellZ = cellZ;
            this.x = x;
            this.z = z;
            this.startY = startY;
            this.seed = seed;
            this.falls = falls;
        }

        /** Returns the cell X. */
        public int cellX() {
            return cellX;
        }

        /** Returns the cell Z. */
        public int cellZ() {
            return cellZ;
        }

        /** Returns the impact's block X. */
        public int x() {
            return x;
        }

        /** Returns the impact's block Z. */
        public int z() {
            return z;
        }

        /** Returns the height AE2 starts its search from, block Y: 180 to 199 for a surface meteorite, else 128. */
        public int startY() {
            return startY;
        }

        /** Returns the meteor seed, never 0. */
        public long seed() {
            return seed;
        }

        /** Returns whether the site's own chunk generates it (see the class comment). */
        public boolean falls() {
            return falls;
        }

        @Override
        public String toString() {
            return "meteorite(" + x + "," + z + " start y" + startY + " seed " + seed + (falls ? "" : " never") + ")";
        }
    }

    private final long worldSeed;
    private final MeteoriteSettings settings;
    private final Random random = new Random();

    /**
     * Creates the grid of a world.
     *
     * @param worldSeed the world seed
     * @param settings  the dimension's settings, enabled
     */
    public MeteoriteGrid(long worldSeed, MeteoriteSettings settings) {
        if (!settings.enabled()) {
            throw new IllegalArgumentException("meteorites are off: " + settings);
        }
        this.worldSeed = worldSeed;
        this.settings = settings;
    }

    /** Returns the settings. */
    public MeteoriteSettings settings() {
        return settings;
    }

    /**
     * Returns the cell of a block coordinate.
     *
     * @param block block X or Z
     * @return the cell
     */
    public int cellOf(int block) {
        return Math.floorDiv(block, settings.cellSize());
    }

    /**
     * Returns a cell's candidate.
     *
     * @param cellX cell X
     * @param cellZ cell Z
     * @return the site
     */
    public Site site(int cellX, int cellZ) {
        Random rng = random;
        rng.setSeed(worldSeed);
        long xSeed = rng.nextLong() >> SEED_SHIFT;
        long zSeed = rng.nextLong() >> SEED_SHIFT;
        rng.setSeed(xSeed * cellX + zSeed * cellZ ^ worldSeed);
        boolean surface = false;
        for (double chance : settings.chanceDrawsArray()) {
            surface = rng.nextDouble() < chance;
        }
        int size = settings.cellSize();
        int margin = MeteoriteSettings.margin(size);
        int x = cellX * size + rng.nextInt(size - 2 * margin) + margin;
        int z = cellZ * size + rng.nextInt(size - 2 * margin) + margin;
        int depth = SURFACE_START_Y + rng.nextInt(SURFACE_START_SPREAD);
        long seed = rng.nextLong();
        while (seed == 0) {
            seed = rng.nextLong();
        }
        boolean falls = cellOf(x >> 4 << 4) == cellX && cellOf(z >> 4 << 4) == cellZ;
        return new Site(cellX, cellZ, x, z, surface ? depth : LOW_START_Y, seed, falls);
    }
}
