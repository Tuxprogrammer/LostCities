package com.tuxprogrammer.lostcities.layout;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/BuildingInfo.java
// Avoids: CITY-34

import com.tuxprogrammer.lostcities.layout.view.TerrainView;

/**
 * A chunk's city level, 0 to 8, from the height oracle: the first {@code cityLevel<n>Height} threshold the height is
 * strictly below, optionally from the height averaged with up to four sampled city neighbors
 * ({@code useAvgHeightmap}, {@code heightSampleSize > 2}). A city chunk's level is then capped at its city's base level
 * plus {@code cityMaxRise} (a deviation from upstream, alpha review AR-4; {@link CityMap#riseBase}). Upstream's
 * {@code BuildingInfo.computeCityLevel} for the
 * {@code default} landscape; the space, floating and cavern rules are not ported (D12). Levels are cached per chunk in
 * a bounded cache the planner owns.
 *
 * <p>
 * Avoids CITY-34: the sample grid uses floor division, so the cells around coordinate 0 are as regular as every other
 * (upstream's truncating division makes chunks -2 and -1 a two-chunk cell).
 *
 * <p>
 * Spec: docs/original-spec/04-city-layout/city-level.md. Side: both.
 */
final class CityLevels {

    /** Averaging only happens for sample sizes above this. */
    private static final int MIN_AVERAGING_SAMPLE = 2;

    private final LayoutProfile profile;
    private final TerrainView terrain;
    private final CityMap cities;
    private final ChunkCache<Integer> levels;

    CityLevels(LayoutProfile profile, TerrainView terrain, CityMap cities, int cacheSize) {
        this.profile = profile;
        this.terrain = terrain;
        this.cities = cities;
        this.levels = new ChunkCache<>(cacheSize);
    }

    /** Returns a chunk's city level. */
    int cityLevel(int chunkX, int chunkZ) {
        Integer cached = levels.get(chunkX, chunkZ);
        if (cached != null) {
            return cached;
        }
        return levels.put(chunkX, chunkZ, compute(chunkX, chunkZ));
    }

    private int compute(int chunkX, int chunkZ) {
        int level = natural(chunkX, chunkZ);
        if (!profile.capsRise() || !cities.isCityRaw(chunkX, chunkZ)) {
            return level;
        }
        // Deviation (AR-4, height-mapping.md "Rise cap"): a city chunk rises at most cityMaxRise levels above its
        // city's base; CityMap has already left out terrain more than one level higher.
        int base = cities.riseBase(chunkX, chunkZ);
        if (base == CityMap.NO_BASE) {
            return level;
        }
        // Deviation (cityGrade): and sinks at most cityGrade levels below it, so the city is cut into the hills
        // and raised over the hollows instead of stepping with every bump.
        int high = base + Math.min(profile.cityMaxRise, profile.cityGrade);
        int low = (int) Math.max(0L, (long) base - profile.cityGrade);
        return Math.max(low, Math.min(high, level));
    }

    /** Returns a chunk's level from the terrain alone, averaged as upstream averages it. */
    private int natural(int chunkX, int chunkZ) {
        int height = terrain.height(chunkX, chunkZ);
        int radius = profile.cityGradeRadius;
        if (radius > 0) {
            // Deviation (cityGradeRadius): the mean of the window, so neighbors change level gradually.
            long sum = 0;
            int count = 0;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    sum += terrain.height(chunkX + dx, chunkZ + dz);
                    count++;
                }
            }
            return levelForHeight((int) (sum / count));
        }
        int sample = profile.heightSampleSize;
        if (!profile.useAvgHeightmap || sample <= MIN_AVERAGING_SAMPLE) {
            return levelForHeight(height);
        }
        int baseX = cellCenter(chunkX, sample, 0);
        int baseZ = cellCenter(chunkZ, sample, 0);
        int sum = height;
        int count = 1;
        int[][] samples = { { cellCenter(chunkX, sample, -1), baseZ }, { cellCenter(chunkX, sample, 1), baseZ },
            { baseX, cellCenter(chunkZ, sample, -1) }, { baseX, cellCenter(chunkZ, sample, 1) } };
        for (int[] s : samples) {
            if (cities.isCityRaw(s[0], s[1])) {
                sum += terrain.height(s[0], s[1]);
                count++;
            }
        }
        return levelForHeight(sum / count);
    }

    /**
     * Returns the center chunk of the sample cell {@code shift} cells from the one holding {@code chunk}.
     *
     * <p>
     * Avoids CITY-34: {@link Math#floorDiv(int, int)} instead of truncating division, and the center is the cell's
     * start plus half the sample size on both sides of 0.
     */
    static int cellCenter(int chunk, int sample, int shift) {
        return (Math.floorDiv(chunk, sample) + shift) * sample + sample / 2;
    }

    /** Returns the city level for a terrain height: the first threshold the height is strictly below, else 8. */
    int levelForHeight(int height) {
        return profile.levelForHeight(height);
    }
}
