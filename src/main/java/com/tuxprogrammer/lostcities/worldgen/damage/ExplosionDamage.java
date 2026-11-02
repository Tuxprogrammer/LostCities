package com.tuxprogrammer.lostcities.worldgen.damage;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/LostCityTerrainFeature.java
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/DamageArea.java
// Avoids: TERR-04, TERR-05, TERR-18, TERR-27

import java.util.Arrays;
import java.util.Random;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.worldgen.buffer.ChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;
import com.tuxprogrammer.lostcities.worldgen.terrain.BlockClassifier;
import com.tuxprogrammer.lostcities.worldgen.terrain.SeedMix;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainContext;
import com.tuxprogrammer.lostcities.worldgen.terrain.WorldLimits;

/**
 * Explosion damage and debris, pipeline stage 10 of upstream's terrain feature: {@code breakBlocksForDamageNew}
 * breaks blocks layer by layer with damage that carries upward and escalates as layers empty, and
 * {@code generateDebris} scatters rubble from badly damaged neighbors onto this chunk.
 *
 * <p>
 * Spec: docs/original-spec/03-terrain/damage-ruins-rubble.md; deviations in docs/new-spec/03-terrain/index.md
 * ("Explosions, damage, debris, ruins, rubble"). Changes from upstream:
 * <ul>
 * <li>TERR-18: only blocks that were not air are damaged, so enclosed air below the water level is never flooded; a
 * broken block below the water level (Y 62 and below on RWG) becomes the liquid, using the chunk's one water level,
 * which honors a {@code seaLevel} override;</li>
 * <li>TERR-04: a debris start above the world is capped at Y 255 instead of jumping to the bottom;</li>
 * <li>TERR-05: every debris block's landing search starts again from the start height;</li>
 * <li>TERR-27: the damage random's seed mixes in the world seed;</li>
 * <li>D28 rule 3: a speed block in the damaged volume that survives becomes its normal partner, and debris never
 * places one ({@link SpeedBlocks}).</li>
 * </ul>
 *
 * <p>
 * One instance per world; reuses its buffers between chunks. Not thread-safe. Side: both (plain Java).
 */
public final class ExplosionDamage {

    /** Damage below which a block is not considered at all. */
    static final float DAMAGE_FLOOR = 0.001f;
    /** Divisor applied to a column's collected damage per layer as it carries upward. */
    static final float CARRY_DIVISOR = 1.4f;
    /** Factor that destroys everything above once a layer is almost empty. */
    static final float TOTAL_DESTRUCTION = 200f;
    /** Multiplier of an easily broken (glass-material) block's damage. */
    static final float EASY_BREAKABLE_FACTOR = 2.5f;
    /** Chance a lightly damaged block with a damaged form becomes it. */
    static final float DAMAGED_FORM_CHANCE = 0.7f;
    /** Debris trigger: a neighbor's damage factor must exceed this. */
    static final float DEBRIS_TRIGGER = 0.5f;
    /** Debris estimate: blocks per floor, plus one. */
    static final int DEBRIS_BLOCKS_PER_FLOOR = 1000;
    /** Debris starts this far above the neighbor's top. */
    static final int DEBRIS_START_ABOVE = 10;
    /** One debris block in this many is iron bars. */
    static final int DEBRIS_IRON_BARS_ONE_IN = 5;

    private static final long RESEED_X = 257017164707L;
    private static final long RESEED_Z = 101754694003L;

    private final TerrainContext context;
    private final float[] collected = new float[256];

    /**
     * Creates the damage pass of one world.
     *
     * @param context the world's terrain context
     */
    public ExplosionDamage(TerrainContext context) {
        this.context = context;
    }

    /**
     * Returns the chunk's damage random, reseeded as upstream's stage 9 does, with the world seed mixed in (TERR-27).
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return a new random
     */
    public Random damageRandom(int chunkX, int chunkZ) {
        return new Random(SeedMix.chunk(context.worldSeed(), chunkX, chunkZ, RESEED_X, RESEED_Z));
    }

    /**
     * Breaks blocks for the explosions that reach the chunk.
     *
     * @param buffer the chunk
     * @param area   the chunk's damage area
     * @param plan   the chunk's plan, for its damaged forms and liquid
     * @param random the damage random
     * @return the number of blocks changed
     */
    public int breakBlocks(ChunkBuffer buffer, DamageArea area, ChunkPlan plan, Random random) {
        return breakBlocks(buffer, area, plan, random, Repairs.NONE);
    }

    /**
     * Breaks blocks for the explosions that reach the chunk, and fills a repaired explosion's breaks below its repair
     * line with patches (deviation, alpha review AR-9: docs/design/0008-city-edges-and-character.md §4). A patch
     * replaces only a block that would have become air or liquid; the damage random is drawn exactly as without
     * repairs.
     *
     * @param buffer  the chunk
     * @param area    the chunk's damage area
     * @param plan    the chunk's plan, for its damaged forms and liquid
     * @param random  the damage random
     * @param repairs the world's repairs
     * @return the number of blocks changed
     */
    public int breakBlocks(ChunkBuffer buffer, DamageArea area, ChunkPlan plan, Random random, Repairs repairs) {
        if (!area.hasExplosions()) {
            return 0;
        }
        BlockClassifier classes = context.classifier();
        int waterLevel = context.settings()
            .waterLevel();
        int originX = area.chunkX() << 4;
        int originZ = area.chunkZ() << 4;
        float factor = 1.0f;
        boolean carrying = false;
        boolean repairing = repairs.active() && plan.isCity();
        Arrays.fill(collected, 0f);
        int changed = 0;
        for (int section = 0; section < DamageArea.SECTIONS; section++) {
            boolean reached = area.hasExplosions(section);
            for (int dy = 0; dy < 16; dy++) {
                int y = (section << 4) + dy;
                if (reached) {
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            // TERR-18: only real blocks collect damage; air below the water level is left alone.
                            if (buffer.get(x, y, z)
                                .isAir()) {
                                continue;
                            }
                            float damage = area.damageAt(originX + x, y, originZ + z) * factor;
                            if (damage >= DAMAGE_FLOOR) {
                                collected[(z << 4) | x] += damage;
                                carrying = true;
                            }
                        }
                    }
                }
                if (!carrying) {
                    continue;
                }
                int damaged = 0;
                int air = 0;
                carrying = false;
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        int index = (z << 4) | x;
                        BlockRef block = buffer.get(x, y, z);
                        if (!block.isAir()) {
                            float damage = collected[index];
                            if (damage >= DAMAGE_FLOOR) {
                                BlockRef result = damageBlock(block, classes, random, y, damage, plan, waterLevel);
                                if (result != block && repairing && isHole(result, plan)) {
                                    BlockRef patch = repairs
                                        .patch(context.worldSeed(), plan, area, classes, block, x, y, z);
                                    if (patch != null) {
                                        result = patch;
                                    }
                                }
                                if (result != block) {
                                    buffer.set(x, y, z, result);
                                    damaged++;
                                    changed++;
                                }
                            }
                        } else {
                            air++;
                        }
                        float carried = collected[index] / CARRY_DIVISOR;
                        if (carried <= 0) {
                            collected[index] = 0;
                        } else {
                            collected[index] = carried;
                            carrying = true;
                        }
                    }
                }
                int total = damaged + air;
                if (total > 250) {
                    factor = TOTAL_DESTRUCTION;
                } else if (total > 220) {
                    factor *= 1.4f;
                } else if (total > 180) {
                    factor *= 1.2f;
                }
            }
        }
        return changed;
    }

    /** Whether a damage result leaves a hole: air, or the chunk's liquid below the water level. */
    private static boolean isHole(BlockRef result, ChunkPlan plan) {
        return result.isAir() || result.equals(
            plan.style()
                .liquid());
    }

    /**
     * Upstream's {@code damageBlock}: a not-breakable block is unchanged; an easily broken one takes 2.5 times the
     * damage; with probability {@code damage} the block breaks, into its damaged form 70 % of the time when the damage
     * is light and it has one, else into liquid below the water level or air above it. A surviving speed block in the
     * damaged volume becomes its normal partner (D28 rule 3).
     *
     * @param block      the block, not air
     * @param classes    the block classes
     * @param random     the damage random
     * @param y          its Y
     * @param damage     the collected damage
     * @param plan       the chunk's plan
     * @param waterLevel the water level
     * @return the new block, or {@code block} itself when unchanged
     */
    static BlockRef damageBlock(BlockRef block, BlockClassifier classes, Random random, int y, float damage,
        ChunkPlan plan, int waterLevel) {
        int flags = classes.flags(block);
        if ((flags & BlockClassifier.NOT_BREAKABLE) != 0) {
            return block;
        }
        if ((flags & BlockClassifier.EASY_BREAKABLE) != 0) {
            damage *= EASY_BREAKABLE_FACTOR;
        }
        if (random.nextFloat() <= damage) {
            BlockRef broken = y < waterLevel ? plan.style()
                .liquid() : BlockRef.AIR;
            BlockRef damagedForm = plan.style()
                .damagedForm(block);
            if (damage < DamageArea.BLOCK_DAMAGE_CHANCE && damagedForm != null) {
                return random.nextFloat() < DAMAGED_FORM_CHANCE ? damagedForm : broken;
            }
            return broken;
        }
        BlockRef normal = SpeedBlocks.normalFor(block);
        return normal == null ? block : normal;
    }

    /**
     * Scatters debris from the eight neighbors, sides first, then corners, in upstream's order.
     *
     * @param buffer    the chunk
     * @param plan      the chunk's plan
     * @param neighbors the eight neighbors' plans, in the order −X, +X, −Z, +Z, −X−Z, +X+Z, −X+Z, +X−Z
     * @param areas     the eight neighbors' damage areas, in the same order; {@code null} for a neighbor without a
     *                  building, which drops no debris
     * @param random    the damage random
     * @return the number of debris blocks placed
     */
    public int debris(ChunkBuffer buffer, ChunkPlan plan, ChunkPlan[] neighbors, DamageArea[] areas, Random random) {
        int placed = 0;
        for (int i = 0; i < 8; i++) {
            placed += debrisFrom(buffer, plan, neighbors[i], areas[i], random, i);
        }
        return placed;
    }

    private int debrisFrom(ChunkBuffer buffer, ChunkPlan plan, ChunkPlan adjacent, DamageArea area, Random random,
        int direction) {
        if (!adjacent.hasBuilding()) {
            return 0;
        }
        float damageFactor = area.damageFactor();
        if (damageFactor <= DEBRIS_TRIGGER) {
            return 0;
        }
        int blocks = (1 + adjacent.floors()) * DEBRIS_BLOCKS_PER_FLOOR;
        float damage = Math.max(1.0f, damageFactor * DamageArea.BLOCK_DAMAGE_CHANCE);
        int count = (int) (blocks * damage) / context.settings()
            .debrisToNearbyChunkFactor();
        // TERR-04: cap at the top of the world instead of starting under it.
        int start = Math.min(adjacent.maxHeight() + DEBRIS_START_ABOVE, WorldLimits.MAX_Y);
        BlockClassifier classes = context.classifier();
        BlockRef rubble = SpeedBlocks.withoutSpeed(
            adjacent.style()
                .rubble());
        BlockRef ironBars = plan.style()
            .ironBars();
        int placed = 0;
        for (int i = 0; i < count; i++) {
            int x = random.nextInt(16);
            int z = random.nextInt(16);
            if (random.nextFloat() >= locationFactor(direction, x, z)) {
                continue;
            }
            // TERR-05: every block searches down from the start height again.
            int y = start;
            while (y > WorldLimits.MIN_Y && classes.isEmpty(buffer.get(x, y, z))) {
                y--;
            }
            BlockRef block = random.nextInt(DEBRIS_IRON_BARS_ONE_IN) == 0 ? ironBars : rubble;
            if (y + 1 <= WorldLimits.MAX_Y) {
                buffer.set(x, y + 1, z, block);
                placed++;
            }
        }
        return placed;
    }

    /** Upstream's per-direction location factors, which favor the side facing the damaged neighbor. */
    static float locationFactor(int direction, int x, int z) {
        return switch (direction) {
            case 0 -> (15.0f - x) / 16.0f;
            case 1 -> x / 16.0f;
            case 2 -> (15.0f - z) / 16.0f;
            case 3 -> z / 16.0f;
            case 4 -> ((15.0f - x) * (15.0f - z)) / 256.0f;
            case 5 -> (x * z) / 256.0f;
            case 6 -> ((15.0f - x) * z) / 256.0f;
            default -> (x * (15.0f - z)) / 256.0f;
        };
    }

    /** Chunk offsets of the eight debris neighbors, in {@link #debris}'s order: {dx, dz}. */
    public static final int[][] NEIGHBOR_OFFSETS = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 }, { -1, -1 }, { 1, 1 },
        { -1, 1 }, { 1, -1 } };
}
