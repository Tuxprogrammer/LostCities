package com.tuxprogrammer.lostcities.worldgen.terrain;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/LostCityTerrainFeature.java
// Avoids: TERR-29, TERR-30, TERR-37, TERR-27

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.worldgen.buffer.ChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.damage.SpeedBlocks;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;
import com.tuxprogrammer.lostcities.worldgen.plan.StyleBlocks;

/**
 * Upstream's ruins and street rubble ({@code generateRuins}, {@code generateRubble}; docs/original-spec/03-terrain/
 * damage-ruins-rubble.md, "Ruins" and "Rubble layer"), run by the terrain phase after a city chunk's content.
 *
 * <ul>
 * <li><b>Ruins</b>: a ruined building loses its top above {@code cityGround + 1 + ruinHeight × floors × 6}, bent by
 * the ruin noise; along the cut each column may get one rubble block, iron bars where a block could break into them,
 * leaves that fall onto the floor below, and air above.</li>
 * <li><b>Rubble layer</b>: streets and ruined buildings get patches of rubble (mossy bricks, mossy cobblestone, moss)
 * and leaves on the ground where the rubble and leaves noises are high.</li>
 * </ul>
 *
 * <p>
 * Changes from upstream:
 * <ul>
 * <li>Avoids TERR-29: a ruin column's step count is capped so it never writes above Y 255;</li>
 * <li>avoids TERR-30: falling leaves stop above Y 0 instead of walking out of the world;</li>
 * <li>avoids TERR-37: the rubble height is interpolated with the weights the right way round, so this chunk's ground
 * holds at its center and the neighbors' toward its edges;</li>
 * <li>TERR-27: the noises and the draws are seeded from the world seed and the chunk.</li>
 * </ul>
 *
 * <p>
 * One instance per world; not thread-safe. Allocates two 256-value buffers per chunk at most. Side: both (plain Java).
 */
public final class Ruins {

    /** Noise octaves (upstream's {@code NoiseGeneratorPerlin(rand, 4)}). */
    static final int OCTAVES = 4;
    /** Ruin noise scale per block (upstream's {@code d0 × 2}, 1/16). */
    static final double RUIN_SCALE = 0.0625;
    /** Rubble noise scale per block (1/16). */
    static final double RUBBLE_SCALE = 1.0 / 16.0;
    /** Leaves noise scale per sample, sampled at chunk × 64 (1/64). */
    static final double LEAVES_SCALE = 1.0 / 64.0;
    /** Leaves noise octave growth (upstream's {@code factor} 4). */
    static final double LEAVES_FACTOR = 4.0;
    /** Upstream's "hardcoded random" for a ruin's rubble and iron bars. */
    static final float RUIN_DRAW = 0.2f;
    /** Blocks above the building's top a ruin column still clears (upstream's {@code maxHeight + 10}). */
    static final int RUIN_REACH = 10;
    /** A noise value over this threshold puts rubble or leaves on a column. */
    static final double PATCH_THRESHOLD = 0.5;
    /** A scale below this disables the rubble or leaves (upstream's {@code < 0.01f}). */
    static final float SCALE_OFF = 0.01f;
    /** Distance between two chunk centers, in blocks: the rubble height's interpolation span. */
    static final float CENTER_SPAN = 16f;

    /** The one-block plants {@link #isSmallPlant} names: tall grass and ferns, and both small-flower blocks. */
    private static final Set<String> SMALL_PLANTS = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList("minecraft:tallgrass", "minecraft:yellow_flower", "minecraft:red_flower")));
    private static final long RUIN_SALT = 0x52_55_49_4E_4E_4F_49_53L;
    private static final long RUBBLE_SALT = 0x52_55_42_42_4C_45_4E_53L;
    private static final long LEAVES_SALT = 0x4C_45_41_56_45_53_4E_53L;
    private static final long DRAW_X = 899809363L;
    private static final long DRAW_Z = 256203221L;

    private final TerrainContext context;
    private final OctaveNoise ruinNoise;
    private final OctaveNoise rubbleNoise;
    private final OctaveNoise leavesNoise;
    private final double[] ruinBuffer = new double[256];
    private final double[] rubbleBuffer = new double[256];
    private final double[] leavesBuffer = new double[256];

    /**
     * Creates the ruins of one world.
     *
     * @param context the world's terrain context
     */
    public Ruins(TerrainContext context) {
        this.context = context;
        this.ruinNoise = new OctaveNoise(context.worldSeed(), RUIN_SALT, OCTAVES);
        this.rubbleNoise = new OctaveNoise(context.worldSeed(), RUBBLE_SALT, OCTAVES);
        this.leavesNoise = new OctaveNoise(context.worldSeed(), LEAVES_SALT, OCTAVES);
    }

    /**
     * Returns the random of a chunk's ruin and rubble draws.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return a new random
     */
    public Random random(int chunkX, int chunkZ) {
        return new Random(SeedMix.chunk(context.worldSeed(), chunkX, chunkZ, DRAW_X, DRAW_Z));
    }

    /**
     * Cuts a ruined building (upstream's {@code generateRuins}).
     *
     * @param buffer the chunk, after its content
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @param plan   the chunk's plan
     * @param random the chunk's ruin random
     * @param skip   soft-edge columns to leave alone, or {@code null}
     * @return the number of blocks changed
     */
    public int ruin(ChunkBuffer buffer, int chunkX, int chunkZ, ChunkPlan plan, Random random, boolean[] skip) {
        if (!plan.hasBuilding() || plan.ruinHeight() < 0
            || context.settings()
                .ruinChance() <= 0) {
            return 0;
        }
        TerrainSettings settings = context.settings();
        BlockClassifier classes = context.classifier();
        StyleBlocks style = plan.style();
        ruinNoise.region(ruinBuffer, chunkX << 4, chunkZ << 4, RUIN_SCALE, 1.0);
        boolean doLeaves = settings.rubbleLayer();
        if (doLeaves) {
            leavesNoise.region(leavesBuffer, chunkX << 6, chunkZ << 6, LEAVES_SCALE, LEAVES_FACTOR);
        }
        int base = (int) (plan.groundY() + 1 + plan.ruinHeight() * plan.floors() * plan.floorPitch());
        BlockRef bars = style.ironBars();
        BlockRef rubble = style.ruinRubble();
        List<BlockRef> leaves = style.leaves();
        int changed = 0;
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                if (skip != null && skip[(z << 4) | x]) {
                    continue;
                }
                int y = Math.max(WorldLimits.MIN_Y + 1, base + (int) ruinBuffer[x + z * 16]);
                // Avoids TERR-29: the step count is capped by the world's top, not compared with a Y.
                int steps = Math.min(plan.maxHeight() + RUIN_REACH - y, WorldLimits.MAX_Y + 1 - y);
                int leafBudget = 0;
                if (doLeaves && settings.rubbleLeaveScale() >= SCALE_OFF) {
                    leafBudget = (int) (leavesBuffer[x + z * 16] / settings.rubbleLeaveScale());
                }
                boolean doRubble = rubble != null;
                // D28 rule 3: the cut turns the floor below it into a walking surface, which holds no speed block.
                BlockRef floor = buffer.get(x, y - 1, z);
                if (SpeedBlocks.isSpeedBlock(floor)) {
                    buffer.set(x, y - 1, z, SpeedBlocks.withoutSpeed(floor));
                }
                while (steps > 0 && y <= WorldLimits.MAX_Y) {
                    BlockRef current = buffer.get(x, y, z);
                    BlockRef below = buffer.get(x, y - 1, z);
                    boolean solidBelow = !classes.isEmpty(below);
                    if (doRubble && solidBelow && !below.equals(bars) && random.nextFloat() < RUIN_DRAW) {
                        doRubble = false;
                        buffer.set(x, y, z, rubble);
                    } else if ((style.damagedForm(current) != null || below.equals(bars)) && solidBelow
                        && random.nextFloat() < RUIN_DRAW) {
                            buffer.set(x, y, z, bars);
                        } else if (leafBudget > 0) {
                            // Avoids TERR-30: the fall stops above the bottom of the world.
                            while (y - 1 > WorldLimits.MIN_Y && classes.isEmpty(buffer.get(x, y - 1, z))) {
                                y--;
                                steps++;
                            }
                            buffer.set(x, y, z, leaves.get(random.nextInt(leaves.size())));
                            leafBudget--;
                        } else {
                            buffer.set(x, y, z, BlockRef.AIR);
                        }
                    changed++;
                    y++;
                    steps--;
                }
            }
        }
        return changed;
    }

    /**
     * Lays rubble and leaves on a street or a ruined building (upstream's {@code generateRubble}).
     *
     * @param buffer the chunk
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @param plan   the chunk's plan
     * @param around the plans of the chunk's 3 × 3 neighborhood, index {@code (dz + 1) × 3 + dx + 1}
     * @param random the chunk's ruin random
     * @param skip   soft-edge columns to leave alone, or {@code null}
     * @return the number of blocks placed
     */
    public int rubble(ChunkBuffer buffer, int chunkX, int chunkZ, ChunkPlan plan, ChunkPlan[] around, Random random,
        boolean[] skip) {
        TerrainSettings settings = context.settings();
        if (!settings.rubbleLayer() || plan.hasBuilding() && plan.ruinHeight() < 0) {
            return 0;
        }
        BlockClassifier classes = context.classifier();
        StyleBlocks style = plan.style();
        rubbleNoise.region(rubbleBuffer, chunkX << 4, chunkZ << 4, RUBBLE_SCALE, 1.0);
        leavesNoise.region(leavesBuffer, chunkX << 6, chunkZ << 6, LEAVES_SCALE, LEAVES_FACTOR);
        float dirtScale = settings.rubbleDirtScale();
        float leaveScale = settings.rubbleLeaveScale();
        List<BlockRef> dirt = style.rubbleDirt();
        List<BlockRef> leaves = style.leaves();
        int placed = 0;
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                if (skip != null && skip[(z << 4) | x] || plan.keepsClear(x, z)) {
                    continue;
                }
                double vr = dirtScale < SCALE_OFF ? 0 : rubbleBuffer[x + z * 16] / dirtScale;
                double vl = leaveScale < SCALE_OFF ? 0 : leavesBuffer[x + z * 16] / leaveScale;
                if (vr <= PATCH_THRESHOLD && vl <= PATCH_THRESHOLD) {
                    continue;
                }
                int y = interpolatedHeight(around, x, z);
                if (y <= WorldLimits.MIN_Y || y > WorldLimits.MAX_Y) {
                    continue;
                }
                if (!classes.isEmpty(buffer.get(x, y - 1, z))) {
                    for (int i = 0; i < vr && y <= WorldLimits.MAX_Y; i++) {
                        BlockRef cell = buffer.get(x, y, z);
                        if (classes.isEmpty(cell) || i == 0 && isSmallPlant(cell)) {
                            buffer.set(x, y, z, dirt.get(random.nextInt(dirt.size())));
                            placed++;
                        }
                        y++;
                    }
                }
                if (y > WorldLimits.MAX_Y) {
                    continue;
                }
                BlockRef leafBase = buffer.get(x, y - 1, z);
                if (leafBase.equals(style.base()) || dirt.contains(leafBase)) {
                    for (int i = 0; i < vl && y <= WorldLimits.MAX_Y; i++) {
                        if (classes.isEmpty(buffer.get(x, y, z))) {
                            buffer.set(x, y, z, leaves.get(random.nextInt(leaves.size())));
                            placed++;
                        }
                        y++;
                    }
                }
            }
        }
        return placed;
    }

    /**
     * Returns whether a block is a one-block plant a rubble pile covers rather than stacks on: tall grass, ferns and
     * small flowers, which a park's greenery puts on most of its grass (AR-18). Stacked, the pile would float on the
     * plant.
     *
     * @param block the block on the ground
     * @return {@code true} for a small plant
     */
    static boolean isSmallPlant(BlockRef block) {
        return SMALL_PLANTS.contains(block.name());
    }

    /**
     * Returns the ground under a column for the rubble layer: the "outside lower" city grounds (a non-city chunk's one
     * lower) of the four chunks whose centers surround the column, interpolated bilinearly.
     *
     * <p>
     * Avoids TERR-37: the weight of a corner falls with the distance from its chunk's center, so the column at this
     * chunk's center takes this chunk's ground.
     *
     * @param around the 3 × 3 plans
     * @param x      local X
     * @param z      local Z
     * @return block Y
     */
    static int interpolatedHeight(ChunkPlan[] around, int x, int z) {
        int west = x < 8 ? -1 : 0;
        int north = z < 8 ? -1 : 0;
        float h00 = outsideLower(around, west, north);
        float h10 = outsideLower(around, west + 1, north);
        float h01 = outsideLower(around, west, north + 1);
        float h11 = outsideLower(around, west + 1, north + 1);
        // Distance from the west (north) chunk's center, which sits 8 blocks inside that chunk.
        float tx = (x < 8 ? x + 8 : x - 8) / CENTER_SPAN;
        float tz = (z < 8 ? z + 8 : z - 8) / CENTER_SPAN;
        float h0 = h00 + (h10 - h00) * tx;
        float h1 = h01 + (h11 - h01) * tx;
        return Math.round(h0 + (h1 - h0) * tz);
    }

    private static int outsideLower(ChunkPlan[] around, int dx, int dz) {
        ChunkPlan plan = around[(dz + 1) * 3 + dx + 1];
        return plan.isCity() ? plan.groundY() : plan.groundY() - 1;
    }
}
