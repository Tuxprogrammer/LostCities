package com.tuxprogrammer.lostcities.layout;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/City.java
// Avoids: PROF-11, PROF-10, CITY-05, CITY-06, CITY-07, CITY-39, CITY-40

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.tuxprogrammer.lostcities.layout.predefined.PredefinedCity;
import com.tuxprogrammer.lostcities.layout.random.WeightedPicker;
import com.tuxprogrammer.lostcities.layout.view.TerrainView;
import com.tuxprogrammer.lostcities.layout.view.WorldStyleView;

/**
 * Where the cities are and which city style governs each chunk: upstream's {@code City} (centers, radii, the city
 * factor, raw city membership, city styles), as one object per world instead of static methods and caches.
 *
 * <p>
 * Fixes, each with a regression test:
 * <ul>
 * <li>Avoids CITY-01 (and PROF-11): the world seed is part of the center, radius and center-style seeds, as on upstream
 * {@code 1.12}, so cities differ between worlds (decision log D10).</li>
 * <li>Avoids CITY-03 (and PROF-10): each contribution to a chunk's style carries the style of its own city center, so
 * a city has one style where cities do not overlap (D10).</li>
 * <li>Avoids CITY-05: the Perlin rarity map is this world's, built from this profile.</li>
 * <li>Avoids CITY-06: the style cache belongs to this planner, whose profile is fixed.</li>
 * <li>Avoids CITY-07: every chunk a predefined building or multibuilding occupies is forced to city, not only the
 * chunks next to a multibuilding's top-left.</li>
 * <li>Avoids CITY-39: the spawn-distance multipliers measure the distance in {@code long}, so they keep working beyond
 * 46,341 blocks from the origin.</li>
 * <li>Avoids CITY-40: a predefined city whose radius reaches past the center search square (half-width
 * {@code (cityMaxRadius + 15) / 16} chunks) still contributes out to its full radius.</li>
 * </ul>
 * Sphere landscapes (inside and outside profiles, CITY-04) are not ported (D12); the main profile governs every chunk.
 *
 * <p>
 * Spec: docs/original-spec/04-city-layout/city-detection.md, city-styles.md. Side: both.
 */
final class CityMap {

    /** Center-chance seed multipliers (with the world seed added, D10). */
    static final long CENTER_Z = 797003437L;
    static final long CENTER_X = 295075153L;
    /** Radius seed multipliers. */
    static final long RADIUS_Z = 100001653L;
    static final long RADIUS_X = 295075153L;
    /** Center-style seed multipliers. */
    static final long CENTER_STYLE_Z = 899809363L;
    static final long CENTER_STYLE_X = 256203221L;
    /** Per-chunk style seed multipliers. */
    static final long STYLE_Z = 593441843L;
    static final long STYLE_X = 217645177L;
    /** Factors at or below this skip the height window and the biome multiplier. */
    private static final float FACTOR_EPSILON = 0.0001f;
    /** Chunk width, in blocks. */
    private static final int CHUNK = 16;
    /** {@link #riseBase} when no rise cap applies: Perlin mode, no city reaches the chunk, or no cap is set. */
    static final int NO_BASE = Integer.MIN_VALUE;
    /**
     * City levels above the rise cap a chunk's terrain may reach and still be city, cut down to the cap: the foot of a
     * slope is leveled a little, steeper ground is left to RWG (AR-4).
     */
    static final int RISE_TOLERANCE = 1;
    /** Distance, in chunks, from a city center of the four extra samples its base level takes the median with. */
    static final int BASE_SPREAD = 3;
    /** Entries of the base-level cache: one per city center, so far fewer than chunks. */
    private static final int BASE_CACHE = 256;

    private final long seed;
    private final LayoutProfile profile;
    private final WorldStyleView worldStyle;
    private final TerrainView terrain;
    private final ActivePredefined predefined;
    private final CityRarityMap rarity;
    private final int searchRadius;
    private final ChunkCache<Float> factors;
    private final ChunkCache<String> styles;
    private final ChunkCache<Integer> bases;

    CityMap(long seed, LayoutProfile profile, WorldStyleView worldStyle, TerrainView terrain,
        ActivePredefined predefined, int cacheSize) {
        this.seed = seed;
        this.profile = profile;
        this.worldStyle = worldStyle;
        this.terrain = terrain;
        this.predefined = predefined;
        this.rarity = profile.cityChance < 0
            ? new CityRarityMap(seed, profile.cityPerlinScale, profile.cityPerlinOffset, profile.cityPerlinInnerScale)
            : null;
        this.searchRadius = (profile.cityMaxRadius + 15) / CHUNK;
        this.factors = new ChunkCache<>(cacheSize);
        this.styles = new ChunkCache<>(cacheSize);
        this.bases = new ChunkCache<>(Math.min(cacheSize, BASE_CACHE));
    }

    /** Returns whether the profile uses Perlin rarity instead of city centers. */
    boolean perlinMode() {
        return rarity != null;
    }

    /** Returns the half-width, in chunks, of the square searched for city centers around a chunk. */
    int searchRadius() {
        return searchRadius;
    }

    /**
     * Returns whether a chunk is a city center: a predefined city's center, or {@code nextDouble() < cityChance} from
     * the center seed. Never in Perlin mode, where {@code cityChance} is negative.
     */
    boolean isCityCenter(int chunkX, int chunkZ) {
        if (predefined.cityAt(chunkX, chunkZ) != null) {
            return true;
        }
        Random random = new Random(seed + chunkZ * CENTER_Z + chunkX * CENTER_X);
        return random.nextDouble() < profile.cityChance;
    }

    /** Returns the radius in blocks of the city centered on a chunk: in {@code [cityMinRadius, cityMaxRadius)}. */
    float cityRadius(int chunkX, int chunkZ) {
        PredefinedCity city = predefined.cityAt(chunkX, chunkZ);
        if (city != null) {
            return city.radius();
        }
        Random random = new Random(seed + chunkZ * RADIUS_Z + chunkX * RADIUS_X);
        int range = Math.max(1, profile.cityMaxRadius - profile.cityMinRadius);
        return profile.cityMinRadius + random.nextInt(range);
    }

    /**
     * Returns the style a city centered on a chunk has: the predefined city's, or a pick among the world style's city
     * styles for the center's biome.
     */
    String cityStyleForCenter(int chunkX, int chunkZ) {
        PredefinedCity city = predefined.cityAt(chunkX, chunkZ);
        if (city != null && city.cityStyle() != null) {
            return city.cityStyle();
        }
        Random random = new Random(seed + chunkZ * CENTER_STYLE_Z + chunkX * CENTER_STYLE_X);
        return worldStyle.pickCityStyle(random, chunkX, chunkZ);
    }

    /** Returns whether a chunk is raw city: its city factor is strictly above {@code cityThreshold}. */
    boolean isCityRaw(int chunkX, int chunkZ) {
        return cityFactor(chunkX, chunkZ) > profile.cityThreshold;
    }

    /** Returns a chunk's city factor, in {@code [0, 1]}. */
    float cityFactor(int chunkX, int chunkZ) {
        Float cached = factors.get(chunkX, chunkZ);
        if (cached != null) {
            return cached;
        }
        return factors.put(chunkX, chunkZ, computeCityFactor(chunkX, chunkZ));
    }

    private float computeCityFactor(int chunkX, int chunkZ) {
        // Avoids CITY-07: every occupied chunk of a predefined building or street is forced, whatever the footprint.
        if (predefined.occupant(chunkX, chunkZ) != null) {
            return 1.0f;
        }
        float factor = 0;
        if (rarity != null) {
            factor = rarity.getCityFactor(chunkX, chunkZ);
        } else {
            for (int cx = chunkX - searchRadius; cx <= chunkX + searchRadius; cx++) {
                for (int cz = chunkZ - searchRadius; cz <= chunkZ + searchRadius; cz++) {
                    if (isCityCenter(cx, cz)) {
                        factor += contribution(cx, cz, chunkX, chunkZ);
                    }
                }
            }
            for (PredefinedCity city : distantPredefined(chunkX, chunkZ)) {
                factor += contribution(city.chunkX(), city.chunkZ(), chunkX, chunkZ);
            }
        }
        if (factor > FACTOR_EPSILON) {
            int height = terrain.height(chunkX, chunkZ);
            if (height < profile.cityMinHeight || height > profile.cityMaxHeight) {
                return 0;
            }
            if (risesTooHigh(chunkX, chunkZ, height)) {
                return 0;
            }
            factor *= worldStyle.cityChanceMultiplier(chunkX, chunkZ);
        }
        if (profile.citySpawnDistance2 > 0) {
            factor *= (float) spawnMultiplier(chunkX, chunkZ);
        }
        return Math.min(Math.max(factor, 0), 1);
    }

    /**
     * Deviation (alpha review AR-4): whether a chunk's terrain rises more than {@code cityMaxRise} +
     * {@link #RISE_TOLERANCE} city levels above its city's base level. Upstream lets a city climb any slope up to
     * {@code cityMaxHeight}, so parks and buildings sat 40 to 60 blocks above the rest of their city; here the city
     * stops before such ground and the blend shapes the slope between.
     */
    private boolean risesTooHigh(int chunkX, int chunkZ, int height) {
        if (!profile.capsRise()) {
            return false;
        }
        int base = riseBase(chunkX, chunkZ);
        return base != NO_BASE && profile.levelForHeight(height) > base + profile.cityMaxRise + RISE_TOLERANCE;
    }

    /**
     * Returns the base level of the city that reaches a chunk most strongly: the city level of the median of five
     * height samples, at its center and {@link #BASE_SPREAD} chunks from it on each axis, so one hill or hollow at
     * the center does not lift or sink the whole city. {@link #NO_BASE} when no cap applies: Perlin mode (no
     * centers), no city reaching the chunk, or {@code cityMaxRise} at the number of levels or more.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the base city level, or {@link #NO_BASE}
     */
    int riseBase(int chunkX, int chunkZ) {
        if (!profile.capsRise()) {
            return NO_BASE;
        }
        int[] center = strongestCenter(chunkX, chunkZ);
        if (center == null) {
            return NO_BASE;
        }
        Integer cached = bases.get(center[0], center[1]);
        if (cached != null) {
            return cached;
        }
        int cx = center[0];
        int cz = center[1];
        int[] samples = { terrain.height(cx, cz), terrain.height(cx - BASE_SPREAD, cz),
            terrain.height(cx + BASE_SPREAD, cz), terrain.height(cx, cz - BASE_SPREAD),
            terrain.height(cx, cz + BASE_SPREAD) };
        Arrays.sort(samples);
        return bases.put(cx, cz, profile.levelForHeight(samples[samples.length / 2]));
    }

    /**
     * The contribution {@code (r - d) / r} of the city centered on {@code (cx, cz)} to a chunk, where {@code d} is the
     * distance in blocks between the chunks' north-west corners, or 0 when the chunk is outside the radius.
     */
    float contribution(int cx, int cz, int chunkX, int chunkZ) {
        float radius = cityRadius(cx, cz);
        int dx = (cx - chunkX) * CHUNK;
        int dz = (cz - chunkZ) * CHUNK;
        float squared = dx * dx + dz * dz;
        if (squared >= radius * radius) {
            return 0;
        }
        float distance = (float) Math.sqrt(squared);
        return (radius - distance) / radius;
    }

    /**
     * The spawn-distance multiplier: {@code citySpawnMultiplier1} up to {@code citySpawnDistance1} blocks from block
     * (0, 0), {@code citySpawnMultiplier2} from {@code citySpawnDistance2}, linear between.
     */
    private double spawnMultiplier(int chunkX, int chunkZ) {
        // Avoids CITY-39: upstream squares the block coordinates in int, which overflows past 46,341 blocks.
        long bx = (long) chunkX * CHUNK;
        long bz = (long) chunkZ * CHUNK;
        float distance = (float) Math.sqrt((double) (bx * bx + bz * bz));
        if (distance <= profile.citySpawnDistance1) {
            return profile.citySpawnMultiplier1;
        }
        if (distance >= profile.citySpawnDistance2) {
            return profile.citySpawnMultiplier2;
        }
        float f = (distance - profile.citySpawnDistance1) / (profile.citySpawnDistance2 - profile.citySpawnDistance1);
        return profile.citySpawnMultiplier1 + f * (profile.citySpawnMultiplier2 - profile.citySpawnMultiplier1);
    }

    /**
     * Returns the name of the city style that governs a chunk, before the street-majority vote: a weighted pick among
     * the cities that reach the chunk, each contributing its own center's style (or the alternative style below
     * {@code cityStyleThreshold}); outside every city, a pick among the world style's styles for the chunk's biome.
     * {@code null} only when the world style matches nothing, which a loaded world style never does.
     */
    String cityStyleName(int chunkX, int chunkZ) {
        String cached = styles.get(chunkX, chunkZ);
        if (cached != null) {
            return cached;
        }
        String computed = computeCityStyleName(chunkX, chunkZ);
        return computed == null ? null : styles.put(chunkX, chunkZ, computed);
    }

    private String computeCityStyleName(int chunkX, int chunkZ) {
        Random random = new Random(seed + chunkZ * STYLE_Z + chunkX * STYLE_X);
        List<Contribution> contributions = new ArrayList<>();
        if (rarity != null) {
            // Perlin mode has no centers, so the style stays per chunk (decision log D48).
            float factor = rarity.getCityFactor(chunkX, chunkZ);
            contributions.add(new Contribution(factor, styleOrAlternative(factor, chunkX, chunkZ)));
        } else {
            for (int cx = chunkX - searchRadius; cx <= chunkX + searchRadius; cx++) {
                for (int cz = chunkZ - searchRadius; cz <= chunkZ + searchRadius; cz++) {
                    if (!isCityCenter(cx, cz)) {
                        continue;
                    }
                    float factor = contribution(cx, cz, chunkX, chunkZ);
                    if (factor > 0) {
                        // Avoids CITY-03: the style of this contribution's own center, not the chunk's.
                        contributions.add(new Contribution(factor, styleOrAlternative(factor, cx, cz)));
                    }
                }
            }
            for (PredefinedCity city : distantPredefined(chunkX, chunkZ)) {
                float factor = contribution(city.chunkX(), city.chunkZ(), chunkX, chunkZ);
                if (factor > 0) {
                    contributions
                        .add(new Contribution(factor, styleOrAlternative(factor, city.chunkX(), city.chunkZ())));
                }
            }
        }
        if (contributions.isEmpty()) {
            return worldStyle.pickCityStyle(random, chunkX, chunkZ);
        }
        Contribution picked = WeightedPicker.pick(random, contributions, c -> c.weight);
        return picked.style;
    }

    /**
     * Avoids CITY-40: the predefined cities whose center lies outside a chunk's search square but whose radius may
     * reach it. Upstream only looks inside the square, so a predefined city larger than {@code cityMaxRadius} was cut
     * off there.
     */
    private List<PredefinedCity> distantPredefined(int chunkX, int chunkZ) {
        List<PredefinedCity> out = new ArrayList<>();
        for (PredefinedCity city : predefined.cities()) {
            if (Math.abs(city.chunkX() - chunkX) > searchRadius || Math.abs(city.chunkZ() - chunkZ) > searchRadius) {
                if (contribution(city.chunkX(), city.chunkZ(), chunkX, chunkZ) > 0) {
                    out.add(city);
                }
            }
        }
        return out;
    }

    private String styleOrAlternative(float factor, int centerX, int centerZ) {
        if (factor < profile.cityStyleThreshold) {
            return profile.cityStyleAlternative;
        }
        return cityStyleForCenter(centerX, centerZ);
    }

    /**
     * Returns the city center whose contribution to a chunk is largest, as {@code {chunkX, chunkZ}}, or {@code null}
     * when no city reaches it (and always in Perlin mode). Ties go to the center with the smaller Z, then X.
     */
    int[] strongestCenter(int chunkX, int chunkZ) {
        if (rarity != null) {
            return null;
        }
        int[] best = null;
        float bestFactor = 0;
        for (PredefinedCity city : distantPredefined(chunkX, chunkZ)) {
            float factor = contribution(city.chunkX(), city.chunkZ(), chunkX, chunkZ);
            if (factor > bestFactor) {
                bestFactor = factor;
                best = new int[] { city.chunkX(), city.chunkZ() };
            }
        }
        for (int cz = chunkZ - searchRadius; cz <= chunkZ + searchRadius; cz++) {
            for (int cx = chunkX - searchRadius; cx <= chunkX + searchRadius; cx++) {
                if (isCityCenter(cx, cz)) {
                    float factor = contribution(cx, cz, chunkX, chunkZ);
                    if (factor > bestFactor) {
                        bestFactor = factor;
                        best = new int[] { cx, cz };
                    }
                }
            }
        }
        return best;
    }

    /** One city's weight in a chunk's style pick, and the style it brings. */
    private static final class Contribution {

        final float weight;
        final String style;

        Contribution(float weight, String style) {
            this.weight = weight;
            this.style = style;
        }
    }
}
