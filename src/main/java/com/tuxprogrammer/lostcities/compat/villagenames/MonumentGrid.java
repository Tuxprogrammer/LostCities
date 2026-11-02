package com.tuxprogrammer.lostcities.compat.villagenames;

import java.util.Locale;
import java.util.Random;

import com.tuxprogrammer.lostcities.layout.landmark.BlockRect;

/**
 * VillageNames' ocean-monument grid and site test, reimplemented from its source (decision log D32): the one chunk of
 * each 32-chunk cell that may hold a monument, the biome-name test around it, and the monument's footprint.
 *
 * <p>
 * Grid: spacing 32 and separation 5, fixed (the monument world generator always builds a fresh
 * {@code StructureOceanMonument()}, whose constructor sets them); the cell is {@code floor(chunk / 32)}; the cell's
 * {@code Random} is {@code World.setRandomSeed(cellX, cellZ, 10387313)}'s; the start chunk is {@code cell × 32 +
 * (nextInt(27) + nextInt(27)) / 2} on each axis, X's two draws first.
 *
 * <p>
 * Site: every block of the 9 × 9 square centered on block {@code (16 cx + 8, 16 cz + 8)} has a biome whose lower-case
 * name contains {@code "deepocean"} or {@code "deep ocean"}, and every block of the 15 × 15 square one whose name
 * contains {@code "ocean"} or {@code "river"}. So RWG2's Kelp Forest and Coral Reef, whose names hold neither word,
 * never host a monument (D32); on RWG2 Continents only its cold deep ocean, which reports vanilla Deep Ocean, can.
 *
 * <p>
 * Footprint: the monument building's box, 58 × 58 blocks from {@code (16 cx + 8 − 29, 16 cz + 8 − 29)}, the same for
 * every facing.
 *
 * <p>
 * Source: VillageNames 4.5.21-GTNH,
 * https://github.com/GTNewHorizons/VillageNames/blob/4.5.21-GTNH/src/main/java/astrotibs/villagenames/prismarine/monument/StructureOceanMonument.java#L41-L45
 * (spacing and separation),
 * https://github.com/GTNewHorizons/VillageNames/blob/4.5.21-GTNH/src/main/java/astrotibs/villagenames/prismarine/monument/StructureOceanMonument.java#L72-L131
 * (grid and site), #L142-L187 ({@code areBiomesViableVN}), #L227-L241 (the start piece's corner);
 * https://github.com/GTNewHorizons/VillageNames/blob/4.5.21-GTNH/src/main/java/astrotibs/villagenames/prismarine/monument/StructureOceanMonumentPieces.java#L735-L748
 * (the 58-block box);
 * https://github.com/GTNewHorizons/VillageNames/blob/4.5.21-GTNH/src/main/java/astrotibs/villagenames/prismarine/monument/MonumentGeneratorIWG.java#L17-L54
 * (overworld only, with map features on, built when the start chunk is populated).
 *
 * <p>
 * Side: both (plain Java).
 */
public final class MonumentGrid {

    /** Cell side, in chunks. */
    public static final int SPACING = 32;
    /** Separation, in chunks: the offset is drawn from {@code SPACING - SEPARATION} values. */
    public static final int SEPARATION = 5;
    /** The salt VillageNames passes to {@code World.setRandomSeed} for monuments. */
    public static final int MONUMENT_SALT = 10387313;
    /** The monument building's side, in blocks. */
    public static final int SIZE = 58;
    /** Half the monument's side, rounded down: the box starts this far west and north of the center block. */
    public static final int HALF = 29;
    /** Half-side of the deep-ocean square, in blocks ({@code 16 >> 2}): a 9 × 9 square. */
    public static final int DEEP_RADIUS = 16 >> 2;
    /** Half-side of the ocean-or-river square, in blocks ({@code 29 >> 2}): a 15 × 15 square. */
    public static final int WATER_RADIUS = 29 >> 2;

    private static final String[] DEEP_NAMES = { "deepocean", "deep ocean" };
    private static final String[] WATER_NAMES = { "ocean", "river" };

    private final long worldSeed;

    /**
     * Creates the grid of a world.
     *
     * @param worldSeed the world seed
     */
    public MonumentGrid(long worldSeed) {
        this.worldSeed = worldSeed;
    }

    /**
     * Returns the grid cell of a chunk coordinate, exactly as VillageNames computes it.
     *
     * @param chunk a chunk X or Z
     * @return the cell X or Z
     */
    public int cellOf(int chunk) {
        int shifted = chunk < 0 ? chunk - (SPACING - 1) : chunk;
        return shifted / SPACING;
    }

    /**
     * Returns the one chunk of a grid cell that may hold a monument.
     *
     * @param cellX the cell X
     * @param cellZ the cell Z
     * @return {@code {chunkX, chunkZ}}
     */
    public int[] startChunk(int cellX, int cellZ) {
        Random random = new Random(VillageGrid.worldRandomSeed(worldSeed, cellX, cellZ, MONUMENT_SALT));
        int range = SPACING - SEPARATION;
        int x = cellX * SPACING + (random.nextInt(range) + random.nextInt(range)) / 2;
        int z = cellZ * SPACING + (random.nextInt(range) + random.nextInt(range)) / 2;
        return new int[] { x, z };
    }

    /**
     * Returns whether a chunk's biomes accept a monument, by VillageNames' name test, reading each column's biome name
     * once at most and stopping at the first column that fails. The center column is read first, so most chunks cost
     * one lookup.
     *
     * @param chunkX the start chunk X
     * @param chunkZ the start chunk Z
     * @param biomes the biome provider's names
     * @return whether the site passes
     */
    public static boolean siteAccepts(int chunkX, int chunkZ, BiomeNames biomes) {
        int centerX = chunkX * 16 + 8;
        int centerZ = chunkZ * 16 + 8;
        if (!nameMatches(biomes.nameAt(centerX, centerZ), DEEP_NAMES)) {
            return false;
        }
        for (int dz = -DEEP_RADIUS; dz <= DEEP_RADIUS; dz++) {
            for (int dx = -DEEP_RADIUS; dx <= DEEP_RADIUS; dx++) {
                if (!nameMatches(biomes.nameAt(centerX + dx, centerZ + dz), DEEP_NAMES)) {
                    return false;
                }
            }
        }
        for (int dz = -WATER_RADIUS; dz <= WATER_RADIUS; dz++) {
            for (int dx = -WATER_RADIUS; dx <= WATER_RADIUS; dx++) {
                boolean insideDeep = Math.abs(dx) <= DEEP_RADIUS && Math.abs(dz) <= DEEP_RADIUS;
                // A deep-ocean name contains "ocean", so the inner square already passes the outer test.
                if (!insideDeep && !nameMatches(biomes.nameAt(centerX + dx, centerZ + dz), WATER_NAMES)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns whether a biome name contains any of the given words once lower-cased, as {@code areBiomesViableVN}
     * tests it.
     *
     * @param biomeName the biome name, or {@code null}
     * @param words     the accepted words, lower case
     * @return whether one is contained
     */
    static boolean nameMatches(String biomeName, String[] words) {
        if (biomeName == null) {
            return false;
        }
        String lower = biomeName.toLowerCase(Locale.ROOT);
        for (String word : words) {
            if (lower.contains(word)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the monument's footprint in a start chunk.
     *
     * @param chunkX the start chunk X
     * @param chunkZ the start chunk Z
     * @return the building's box in X and Z
     */
    public static BlockRect footprint(int chunkX, int chunkZ) {
        int minX = chunkX * 16 + 8 - HALF;
        int minZ = chunkZ * 16 + 8 - HALF;
        return new BlockRect(minX, minZ, minX + SIZE - 1, minZ + SIZE - 1);
    }
}
