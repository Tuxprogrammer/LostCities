// @formatter:off
package com.tuxprogrammer.lostcities.worldgen.damage;
// Ported from Lost Cities (MIT): https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/DamageArea.java
// Avoids: PROF-06, TERR-19
// @formatter:on

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;
import com.tuxprogrammer.lostcities.worldgen.plan.CityPlan;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainSettings;
import com.tuxprogrammer.lostcities.worldgen.terrain.WorldLimits;

/**
 * The explosions that reach one chunk, and the damage they do at a point. Explosions start in chunks within three
 * chunks (city chunks only, by default), each a pure function of the world seed and its chunk's coordinates, so every
 * chunk they reach agrees on them whatever order chunks generate in.
 *
 * <p>
 * Spec: docs/original-spec/03-terrain/damage-ruins-rubble.md ("Building the DamageArea"); numbers in
 * docs/new-spec/03-terrain/height-mapping.md ("Damage"). Changes from upstream:
 * <ul>
 * <li>PROF-06 (TERR-03): an equal min/max pair gives the minimum instead of throwing; the pairs are ordered when the
 * profile is read ({@link TerrainSettings});</li>
 * <li>TERR-19: the Y range is the 1.7.10 world, named by {@link WorldLimits}, which is upstream's hard-coded 0 to
 * 255;</li>
 * <li>{@link #isCompletelyDestroyed}, {@link #lowestExplosionY} and {@link #highestExplosionY}, dead in 1.20, serve
 * the floating-fragment pass again (TERR-28).</li>
 * </ul>
 *
 * <p>
 * Immutable. Side: both (plain Java).
 */
public final class DamageArea {

    /** Upstream's {@code BLOCK_DAMAGE_CHANCE}: below this damage a block with a damaged form may become it. */
    public static final float BLOCK_DAMAGE_CHANCE = 0.7f;

    /** Damage at distance d from a center of radius r is {@code DAMAGE_SCALE × (r − d) / r}. */
    static final float DAMAGE_SCALE = 3.0f;

    /** Number of 16-block sections in a 1.7.10 column. */
    static final int SECTIONS = WorldLimits.BUILD_HEIGHT / 16;

    private static final long INCLUDE_Z = 295075153L;
    private static final long INCLUDE_X = 899826547L;
    private static final long BIG_Z = 295075153L;
    private static final long BIG_X = 797003437L;
    private static final long MINI_Z = 1400305337L;
    private static final long MINI_X = 573259391L;

    private final int chunkX;
    private final int chunkZ;
    private final List<Explosion> explosions;

    private DamageArea(int chunkX, int chunkZ, List<Explosion> explosions) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.explosions = Collections.unmodifiableList(explosions);
    }

    /**
     * Collects the explosions that reach a chunk.
     *
     * @param worldSeed the world seed
     * @param chunkX    chunk X
     * @param chunkZ    chunk Z
     * @param settings  the pinned settings
     * @param cityPlan  the plan source, for each origin chunk's city flag, level and style's explosion chance
     * @return the chunk's damage area
     */
    public static DamageArea of(long worldSeed, int chunkX, int chunkZ, TerrainSettings settings, CityPlan cityPlan) {
        List<Explosion> found = new ArrayList<>();
        Random include = new Random(worldSeed + chunkZ * INCLUDE_Z + chunkX * INCLUDE_X);
        int reach = searchRange(settings);
        for (int cx = chunkX - reach; cx <= chunkX + reach; cx++) {
            for (int cz = chunkZ - reach; cz <= chunkZ + reach; cz++) {
                ChunkPlan origin = cityPlan.chunk(cx, cz);
                if (settings.explosionsInCitiesOnly() && !origin.isCity()) {
                    continue;
                }
                Explosion big = bigExplosionAt(worldSeed, origin, settings);
                if (big != null && reaches(chunkX, chunkZ, big) && kept(include, origin)) {
                    found.add(big);
                }
                Explosion mini = miniExplosionAt(worldSeed, origin, settings);
                if (mini != null && reaches(chunkX, chunkZ, mini) && kept(include, origin)) {
                    found.add(mini);
                }
            }
        }
        return new DamageArea(chunkX, chunkZ, found);
    }

    /**
     * Returns a damage area with the given explosions, for tests and for callers that collected them otherwise.
     *
     * @param chunkX     chunk X
     * @param chunkZ     chunk Z
     * @param explosions the explosions that reach the chunk
     * @return the area
     */
    public static DamageArea withExplosions(int chunkX, int chunkZ, List<Explosion> explosions) {
        return new DamageArea(chunkX, chunkZ, new ArrayList<>(explosions));
    }

    /** Returns how many chunks around an explosion's origin it can reach: {@code (max radius + 15) / 16}. */
    public static int searchRange(TerrainSettings settings) {
        return (Math.max(settings.explosionMaxRadius(), settings.miniExplosionMaxRadius()) + 15) / 16;
    }

    /**
     * Returns the big explosion that starts in a chunk, or {@code null}.
     *
     * @param worldSeed the world seed
     * @param origin    the origin chunk's plan
     * @param settings  the pinned settings
     * @return the explosion or {@code null}
     */
    static Explosion bigExplosionAt(long worldSeed, ChunkPlan origin, TerrainSettings settings) {
        Random random = new Random(worldSeed + origin.chunkZ() * BIG_Z + origin.chunkX() * BIG_X);
        if (random.nextFloat() >= settings.explosionChance()) {
            return null;
        }
        int radius = between(random, settings.explosionMinRadius(), settings.explosionMaxRadius());
        int x = (origin.chunkX() << 4) + random.nextInt(16);
        int y = origin.cityLevel() * WorldLimits.FLOOR_HEIGHT
            + between(random, settings.explosionMinHeight(), settings.explosionMaxHeight());
        int z = (origin.chunkZ() << 4) + random.nextInt(16);
        return new Explosion(x, y, z, radius);
    }

    /**
     * Returns the mini explosion that starts in a chunk, or {@code null}.
     *
     * @param worldSeed the world seed
     * @param origin    the origin chunk's plan
     * @param settings  the pinned settings
     * @return the explosion or {@code null}
     */
    static Explosion miniExplosionAt(long worldSeed, ChunkPlan origin, TerrainSettings settings) {
        Random random = new Random(worldSeed + origin.chunkZ() * MINI_Z + origin.chunkX() * MINI_X);
        if (random.nextFloat() >= settings.miniExplosionChance()) {
            return null;
        }
        int radius = between(random, settings.miniExplosionMinRadius(), settings.miniExplosionMaxRadius());
        int x = (origin.chunkX() << 4) + random.nextInt(16);
        int y = origin.cityLevel() * WorldLimits.FLOOR_HEIGHT
            + between(random, settings.miniExplosionMinHeight(), settings.miniExplosionMaxHeight());
        int z = (origin.chunkZ() << 4) + random.nextInt(16);
        return new Explosion(x, y, z, radius);
    }

    /**
     * Upstream's {@code min + nextInt(max − min)}, with the maximum exclusive as upstream documents it, and the
     * minimum for an equal pair instead of {@code nextInt(0)}'s exception (PROF-06, TERR-03).
     */
    static int between(Random random, int min, int max) {
        return max > min ? min + random.nextInt(max - min) : min;
    }

    private static boolean kept(Random include, ChunkPlan origin) {
        float chance = origin.explosionChance();
        return Float.isNaN(chance) || include.nextFloat() < chance;
    }

    private static boolean reaches(int chunkX, int chunkZ, Explosion explosion) {
        return squaredDistanceToBox(
            chunkX << 4,
            WorldLimits.MIN_Y,
            chunkZ << 4,
            (chunkX << 4) + 15,
            WorldLimits.BUILD_HEIGHT,
            (chunkZ << 4) + 15,
            explosion) <= explosion.squaredRadius();
    }

    /** Upstream's {@code GeometryTools.squaredDistanceBoxPoint}: 0 inside the box. */
    static double squaredDistanceToBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Explosion e) {
        double d = 0;
        d += axis(e.x(), minX, maxX);
        d += axis(e.y(), minY, maxY);
        d += axis(e.z(), minZ, maxZ);
        return d;
    }

    private static double axis(int value, int min, int max) {
        if (value < min) {
            return (double) (value - min) * (value - min);
        }
        if (value > max) {
            return (double) (value - max) * (value - max);
        }
        return 0;
    }

    /** Upstream's {@code GeometryTools.maxSquaredDistanceBoxPoint}: to the farthest corner. */
    static double maxSquaredDistanceToBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Explosion e) {
        double d = 0;
        d += farAxis(e.x(), minX, maxX);
        d += farAxis(e.y(), minY, maxY);
        d += farAxis(e.z(), minZ, maxZ);
        return d;
    }

    private static double farAxis(int value, int min, int max) {
        double far = value < (min + max) / 2.0 ? max : min;
        return (value - far) * (value - far);
    }

    /** Returns chunk X. */
    public int chunkX() {
        return chunkX;
    }

    /** Returns chunk Z. */
    public int chunkZ() {
        return chunkZ;
    }

    /** Returns whether any explosion reaches the chunk. */
    public boolean hasExplosions() {
        return !explosions.isEmpty();
    }

    /** Returns the explosions that reach the chunk. */
    public List<Explosion> explosions() {
        return explosions;
    }

    /**
     * Returns whether any explosion reaches one 16-block section of the chunk.
     *
     * @param section 0 to 15: Y {@code 16 × section} to {@code 16 × section + 15}
     * @return {@code true} when the section is reached
     */
    public boolean hasExplosions(int section) {
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        for (Explosion explosion : explosions) {
            double d = squaredDistanceToBox(
                minX,
                section << 4,
                minZ,
                minX + 15,
                (section << 4) + 15,
                minZ + 15,
                explosion);
            if (d <= explosion.squaredRadius()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether a section lies wholly within two thirds of an explosion's radius, where its damage is at least
     * 1 everywhere (upstream's {@code isCompletelyDestroyed}).
     *
     * @param section 0 to 15
     * @return {@code true} when nothing in the section survives
     */
    public boolean isCompletelyDestroyed(int section) {
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        for (Explosion explosion : explosions) {
            double far = maxSquaredDistanceToBox(
                minX,
                section << 4,
                minZ,
                minX + 15,
                (section << 4) + 15,
                minZ + 15,
                explosion);
            double fatal = explosion.radius() * 2.0 / 3.0;
            if (far <= fatal * fatal) {
                return true;
            }
        }
        return false;
    }

    /** Returns the lowest Y of a section an explosion reaches, or −1 when none does. */
    public int lowestExplosionY() {
        for (int section = 0; section < SECTIONS; section++) {
            if (hasExplosions(section)) {
                return section << 4;
            }
        }
        return -1;
    }

    /** Returns the highest Y of a section an explosion reaches, or −1 when none does. */
    public int highestExplosionY() {
        for (int section = SECTIONS - 1; section >= 0; section--) {
            if (hasExplosions(section)) {
                return (section << 4) + 15;
            }
        }
        return -1;
    }

    /**
     * Returns how badly the chunk is damaged: the summed damage at its (0, 0) corner at each explosion's own height
     * (upstream's {@code getDamageFactor}, used by debris).
     */
    public float damageFactor() {
        float damage = 0f;
        for (Explosion explosion : explosions) {
            double squared = explosion.squaredDistanceTo(chunkX * 16.0, explosion.y(), chunkZ * 16.0);
            if (squared < explosion.squaredRadius()) {
                damage += (float) (DAMAGE_SCALE * (explosion.radius() - Math.sqrt(squared)) / explosion.radius());
            }
        }
        return damage;
    }

    /**
     * Returns the damage at a block: {@code 3 × (r − d) / r} summed over the explosions it is inside of.
     *
     * @param x block X (world)
     * @param y block Y
     * @param z block Z (world)
     * @return 0 for no damage; at least 1 within two thirds of a radius
     */
    public float damageAt(int x, int y, int z) {
        float damage = 0f;
        for (int i = 0, n = explosions.size(); i < n; i++) {
            Explosion explosion = explosions.get(i);
            double squared = explosion.squaredDistanceTo(x, y, z);
            if (squared < explosion.squaredRadius()) {
                damage += (float) (DAMAGE_SCALE * (explosion.radius() - Math.sqrt(squared)) / explosion.radius());
            }
        }
        return damage;
    }
}
