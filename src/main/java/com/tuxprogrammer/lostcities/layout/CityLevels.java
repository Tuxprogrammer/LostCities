package com.tuxprogrammer.lostcities.layout;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/BuildingInfo.java
// Avoids: CITY-34

import com.tuxprogrammer.lostcities.layout.view.TerrainView;

/**
 * A chunk's city level, 0 to 8, from the height oracle: the first {@code cityLevel<n>Height} threshold the height is
 * strictly below, optionally from the height averaged with up to four sampled city neighbors
 * ({@code useAvgHeightmap}, {@code heightSampleSize > 2}). Upstream's {@code BuildingInfo.computeCityLevel} for the
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
        int height = terrain.height(chunkX, chunkZ);
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
        for (int level = 0; level < profile.cityLevelThresholds(); level++) {
            if (height < profile.cityLevelHeight(level)) {
                return level;
            }
        }
        return profile.cityLevelThresholds();
    }
}
