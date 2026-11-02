package com.tuxprogrammer.lostcities.layout;

import java.util.Random;

import com.tuxprogrammer.lostcities.layout.noise.SimplexNoise;

/**
 * A city chunk's urban intensity, from 0 at the very edge of a city to 1 in its dense core: what building heights and
 * edge lots follow (alpha review AR-5 and AR-6, docs/design/0011-city-edges-and-character.md §1).
 *
 * <p>
 * {@code I = clamp(e^0.8 × (0.25 + 0.75 c) + 0.18 n × cityTaper, 0, 1)}, where
 * <ul>
 * <li>{@code e} is the Euclidean distance, in chunks, to the nearest chunk within {@value #EDGE_WINDOW} that is not
 * city (RWG land, a landmark's margin, ground the rise cap leaves out, the sea), over {@value #EDGE_REACH}, capped at
 * 1;</li>
 * <li>{@code c} is the square root of the city factor above {@code cityThreshold}, scaled to 0 to 1;</li>
 * <li>{@code n} is two octaves of simplex noise over chunk coordinates, wavelengths of about 7 and 3 chunks, so
 * districts rise and fall against the radial trend and heights never form rings.</li>
 * </ul>
 *
 * <p>
 * A pure function of the world seed and the layout, cached per chunk in a bounded cache the planner owns. Side: both
 * (plain Java).
 */
final class Urbanity {

    /** How far, in chunks, the edge distance looks for a chunk that is not city. */
    static final int EDGE_WINDOW = 3;
    /** Edge distance, in chunks, at which the edge term reaches 1. */
    static final float EDGE_REACH = 4f;
    /** Exponent of the edge term: a little above linear near the edge. */
    static final double EDGE_EXPONENT = 0.8;
    /** The core term's share at the city factor's threshold. */
    static final float CORE_FLOOR = 0.25f;
    /** Amplitude of the noise, before {@code cityTaper}. */
    static final float NOISE_AMPLITUDE = 0.18f;
    /** Frequency, per chunk, of the noise's first octave (a wavelength of about 7 chunks). */
    static final double NOISE_FREQUENCY = 1.0 / 7.0;
    /** Frequency, per chunk, of the second octave (about 3 chunks). */
    static final double NOISE_FREQUENCY_2 = 1.0 / 3.0;
    /** Weight of the second octave. */
    static final double NOISE_WEIGHT_2 = 0.5;
    /** Salt of the noise's seed. */
    static final long NOISE_SALT = 0x55524241_4e495459L;

    /** The planner's view of whether a chunk is city, after landmark zones. */
    interface CityTest {

        /**
         * @param chunkX chunk X
         * @param chunkZ chunk Z
         * @return whether the chunk is city
         */
        boolean isCity(int chunkX, int chunkZ);
    }

    private final LayoutProfile profile;
    private final CityMap cities;
    private final CityTest city;
    private final SimplexNoise noise;
    private final ChunkCache<Float> intensities;

    Urbanity(long seed, LayoutProfile profile, CityMap cities, CityTest city, int cacheSize) {
        this.profile = profile;
        this.cities = cities;
        this.city = city;
        this.noise = new SimplexNoise(new Random(seed ^ NOISE_SALT));
        this.intensities = new ChunkCache<>(cacheSize);
    }

    /**
     * Returns a chunk's intensity, 0 to 1. A chunk that is not city has 0.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the intensity
     */
    float intensity(int chunkX, int chunkZ) {
        Float cached = intensities.get(chunkX, chunkZ);
        if (cached != null) {
            return cached;
        }
        return intensities.put(chunkX, chunkZ, compute(chunkX, chunkZ));
    }

    private float compute(int chunkX, int chunkZ) {
        if (!city.isCity(chunkX, chunkZ)) {
            return 0f;
        }
        float edge = Math.min(1f, edgeDistance(chunkX, chunkZ) / EDGE_REACH);
        float core = core(cities.cityFactor(chunkX, chunkZ), profile.cityThreshold);
        float base = (float) Math.pow(edge, EDGE_EXPONENT) * (CORE_FLOOR + (1 - CORE_FLOOR) * core);
        float value = base + NOISE_AMPLITUDE * profile.cityTaper * noise(chunkX, chunkZ);
        return Math.max(0f, Math.min(1f, value));
    }

    /**
     * The core term: the square root of the city factor above the threshold, scaled to 0 to 1.
     *
     * @param factor    the city factor
     * @param threshold {@code cityThreshold}
     * @return 0 to 1
     */
    static float core(float factor, float threshold) {
        if (threshold >= 1f) {
            return 1f;
        }
        float scaled = (factor - threshold) / (1f - threshold);
        return (float) Math.sqrt(Math.max(0f, Math.min(1f, scaled)));
    }

    /** The noise at a chunk, about −1 to 1. */
    float noise(int chunkX, int chunkZ) {
        double first = noise.getValue(chunkX * NOISE_FREQUENCY, chunkZ * NOISE_FREQUENCY);
        double second = noise.getValue(chunkX * NOISE_FREQUENCY_2 + 101.5, chunkZ * NOISE_FREQUENCY_2 - 57.25);
        return (float) ((first + NOISE_WEIGHT_2 * second) / (1 + NOISE_WEIGHT_2));
    }

    /**
     * Returns the Euclidean distance, in chunks, from a chunk to the nearest chunk within {@value #EDGE_WINDOW} that
     * is not city, or {@code EDGE_WINDOW + 1} when every chunk in the window is city.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the distance
     */
    float edgeDistance(int chunkX, int chunkZ) {
        float best = EDGE_WINDOW + 1;
        for (int dx = -EDGE_WINDOW; dx <= EDGE_WINDOW; dx++) {
            for (int dz = -EDGE_WINDOW; dz <= EDGE_WINDOW; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                float distance = (float) Math.sqrt(dx * dx + dz * dz);
                if (distance < best && !city.isCity(chunkX + dx, chunkZ + dz)) {
                    best = distance;
                }
            }
        }
        return best;
    }

    /**
     * Returns the taper's floor cap at an intensity: {@code max(minimum, ⌈maxFloors × (0.18 + 0.82 I)⌉)}, blended
     * with {@code maxFloors} by {@code 1 − cityTaper} (0011 §2).
     *
     * @param intensity the intensity
     * @param maxFloors the profile's {@code buildingMaxFloors}
     * @param minimum   the least floors a building keeps ({@code buildingMinFloors + 1})
     * @param taper     {@code cityTaper}
     * @return the cap
     */
    static int floorCap(float intensity, int maxFloors, int minimum, float taper) {
        double share = EDGE_CAP_FLOOR + (1 - EDGE_CAP_FLOOR) * intensity;
        double tapered = maxFloors * share;
        double blended = maxFloors + (tapered - maxFloors) * taper;
        return Math.max(minimum, (int) Math.ceil(blended - 1e-6));
    }

    /** The share of {@code buildingMaxFloors} a building at intensity 0 may still have. */
    static final double EDGE_CAP_FLOOR = 0.18;
    /** Intensity above which a building may rise into the high-rise core. */
    static final float CORE_INTENSITY = 0.65f;
    /** Chance of a high-rise at intensity 1, before {@code cityTaper}. */
    static final float CORE_CHANCE = 0.7f;

    /**
     * Returns the chance that a building at an intensity rises into the high-rise core (0011 §2).
     *
     * @param intensity the intensity
     * @param taper     {@code cityTaper}
     * @return 0 to {@value #CORE_CHANCE}
     */
    static float coreChance(float intensity, float taper) {
        if (intensity <= CORE_INTENSITY) {
            return 0f;
        }
        return CORE_CHANCE * taper * (intensity - CORE_INTENSITY) / (1f - CORE_INTENSITY);
    }
}
