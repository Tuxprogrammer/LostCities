package com.tuxprogrammer.lostcities.worldgen.terrain;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/LostCityTerrainFeature.java
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/BuildingInfo.java
// Avoids: TERR-10, TERR-24, TERR-27

import java.util.Random;

import com.tuxprogrammer.lostcities.layout.random.StableHash;

import com.tuxprogrammer.lostcities.worldgen.buffer.ChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkKind;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;
import com.tuxprogrammer.lostcities.worldgen.plan.CityPlan;

/**
 * Upstream 1.20's terrain blend ({@code correctTerrainShape} with {@code BuildingInfo}'s corner meshes): a non-city
 * chunk near a city is fitted between a lower and an upper height mesh defined at chunk corners, so its terrain meets
 * the city ground without cliffs. Each chunk owns the corner at its block (0, 0).
 *
 * <ul>
 * <li><b>L0</b>: a corner where city and non-city chunks meet is fixed at the lowest city ground among its four
 * chunks (an ocean chunk counts as {@code groundLevel − oceanCorrectionBorder}); other corners are free.</li>
 * <li><b>L1</b>: a fixed corner takes L0 plus the {@code terrainFix} offsets; a free corner takes the tightest of its
 * eight neighbors' L0 ± 20 (edge) or 25 (diagonal) plus 0..4 of jitter.</li>
 * <li><b>L2</b>: the same over L1, so the blend reaches two chunks out.</li>
 * <li>The chunk's columns are interpolated bilinearly between its four L2 corners, bent by the transition noise,
 * then lowered with {@link ColumnOps#moveDown} or raised with {@link ColumnOps#moveUp}.</li>
 * </ul>
 *
 * <p>
 * Spec: docs/original-spec/03-terrain/terrain-blend.md; docs/new-spec/03-terrain/index.md ("Terrain blend") and
 * docs/design/0005-rwg-overlay.md §5. Changes from upstream:
 * <ul>
 * <li>Landmark cores (rule E1, 0006 §4): a core chunk is never reshaped, and a corner shared by a core chunk and a
 * non-city chunk, with no city chunk, is fixed at the core's oracle height with a window of −1 to +1;</li>
 * <li>TERR-24 (CITY-22): "the corner is constrained" is an explicit flag, not {@code h < 256};</li>
 * <li>TERR-27: the corner jitter seeds mix in the world seed;</li>
 * <li>TERR-10: the blend never writes a height back; heights are pure oracle values in the plan.</li>
 * </ul>
 *
 * <p>
 * One instance per world; not thread-safe. Allocates nothing per column. Side: both (plain Java).
 */
public final class TerrainBlend {

    /** Slack of an edge neighbor in the L1 and L2 reductions, before jitter, in blocks. */
    static final int EDGE_SLACK = 20;
    /** Slack of a diagonal neighbor, before jitter, in blocks. */
    static final int DIAGONAL_SLACK = 25;
    /** Jitter added to a neighbor's slack: {@code nextInt(5)}. */
    static final int SLACK_JITTER = 5;
    /** Distance of the substitute for an unconstrained corner from the chunk's height, in blocks. */
    static final int FREE_CORNER_SPAN = 90;
    /** Window around a landmark core's natural height at a fixed corner (rule E1), in blocks each way. */
    static final int CORE_CORNER_WINDOW = 1;
    /** Corridor width below which the transition noise has no effect, in blocks. */
    static final float NOISE_FREE_WIDTH = 4.0f;
    /** Corridor width over which the transition noise ramps to full strength, in blocks. */
    static final float NOISE_RAMP = 20.0f;

    /**
     * Amplitude of the bank wobble, as a share of the transition noise (about ±4): up to ±1.4 blocks away from city
     * corners, so a blended bank's 1-block steps follow ragged contours (M12, 0008 §5).
     */
    static final float BANK_WOBBLE = 0.35f;
    /** Salt of the per-column dither of the blend's cut. */
    static final long DITHER_SALT = 0x44495448_45524544L;

    /** How far around a chunk the plans are read: L0 of the corners two corners out needs chunks three out. */
    static final int REACH = 3;

    private static final long OFFSET_X = 899809363L;
    private static final long OFFSET_Z = 256203221L;
    private static final long L1_X = 132897987541L;
    private static final long L1_Z = 341873128712L;

    private final TerrainContext context;
    private final Random jitter = new Random();

    // Corner grids around the chunk being blended, reused between chunks.
    private final ChunkPlan[] plans = new ChunkPlan[(2 * REACH + 1) * (2 * REACH + 1)];
    private final int[] l0 = new int[36];
    private final byte[] l0Kind = new byte[36];
    private final int[] l1Min = new int[16];
    private final int[] l1Max = new int[16];
    private final boolean[] l1Fixed = new boolean[16];
    private final int[] l2Min = new int[4];
    private final int[] l2Max = new int[4];
    private final boolean[] l2Fixed = new boolean[4];
    private final boolean[] l2City = new boolean[4];

    private static final byte FREE = 0;
    private static final byte CITY_CORNER = 1;
    private static final byte CORE_CORNER = 2;

    /**
     * Creates the blend of one world.
     *
     * @param context the world's terrain context
     */
    public TerrainBlend(TerrainContext context) {
        this.context = context;
    }

    /**
     * Fits a non-city chunk between the meshes, if any corner of it is constrained.
     *
     * @param buffer   the chunk
     * @param chunkX   chunk X
     * @param chunkZ   chunk Z
     * @param cityPlan the plan source
     * @return {@code true} when some column was moved
     */
    public boolean apply(ChunkBuffer buffer, int chunkX, int chunkZ, CityPlan cityPlan) {
        ChunkPlan self = cityPlan.chunk(chunkX, chunkZ);
        if (self.kind() != ChunkKind.NONE) {
            // City chunks are leveled outright; landmark cores are never reshaped (E1).
            return false;
        }
        if (!computeCorners(chunkX, chunkZ, cityPlan)) {
            return false;
        }
        return reshape(buffer, chunkX, chunkZ, self);
    }

    /**
     * Returns whether any of the chunk's four L2 corners is constrained, filling the corner grids. Exposed for tests.
     */
    boolean computeCorners(int chunkX, int chunkZ, CityPlan cityPlan) {
        boolean anyLandmarkOrCity = false;
        int side = 2 * REACH + 1;
        for (int dz = -REACH; dz <= REACH; dz++) {
            for (int dx = -REACH; dx <= REACH; dx++) {
                ChunkPlan plan = cityPlan.chunk(chunkX + dx, chunkZ + dz);
                plans[(dz + REACH) * side + dx + REACH] = plan;
                anyLandmarkOrCity |= plan.kind() != ChunkKind.NONE;
            }
        }
        if (!anyLandmarkOrCity) {
            return false;
        }
        // L0 for corners chunkX-2 .. chunkX+3 (6 × 6).
        for (int cz = 0; cz < 6; cz++) {
            for (int cx = 0; cx < 6; cx++) {
                computeL0(cx - 2, cz - 2, cz * 6 + cx);
            }
        }
        // L1 for corners chunkX-1 .. chunkX+2 (4 × 4).
        TerrainSettings settings = context.settings();
        for (int cz = 0; cz < 4; cz++) {
            for (int cx = 0; cx < 4; cx++) {
                int index = cz * 4 + cx;
                int cornerX = chunkX + cx - 1;
                int cornerZ = chunkZ + cz - 1;
                int l0Index = (cz + 1) * 6 + cx + 1;
                byte kind = l0Kind[l0Index];
                int h = l0[l0Index];
                if (kind == CITY_CORNER) {
                    l1Min[index] = h + randomizedOffset(
                        cornerX,
                        cornerZ,
                        settings.terrainFixLowerMinOffset(),
                        settings.terrainFixLowerMaxOffset());
                    l1Max[index] = h + randomizedOffset(
                        cornerX,
                        cornerZ,
                        settings.terrainFixUpperMinOffset(),
                        settings.terrainFixUpperMaxOffset());
                    l1Fixed[index] = true;
                    continue;
                }
                if (kind == CORE_CORNER) {
                    l1Min[index] = h - CORE_CORNER_WINDOW;
                    l1Max[index] = h + CORE_CORNER_WINDOW;
                    l1Fixed[index] = true;
                    continue;
                }
                int min = Integer.MAX_VALUE;
                int max = Integer.MAX_VALUE;
                boolean constrained = false;
                for (int nz = -1; nz <= 1; nz++) {
                    for (int nx = -1; nx <= 1; nx++) {
                        if (nx == 0 && nz == 0) {
                            continue;
                        }
                        int n = (cz + 1 + nz) * 6 + cx + 1 + nx;
                        if (l0Kind[n] == FREE) {
                            continue;
                        }
                        int slack = (nx != 0 && nz != 0 ? DIAGONAL_SLACK : EDGE_SLACK)
                            + heightOffset(cornerX + nx, cornerZ + nz, L1_X, L1_Z);
                        min = Math.min(min, l0[n] - slack);
                        max = Math.min(max, l0[n] + slack);
                        constrained = true;
                    }
                }
                l1Min[index] = min;
                l1Max[index] = max;
                l1Fixed[index] = constrained;
            }
        }
        // L2 for the chunk's own four corners.
        boolean any = false;
        for (int cz = 0; cz < 2; cz++) {
            for (int cx = 0; cx < 2; cx++) {
                int index = cz * 2 + cx;
                int l1Index = (cz + 1) * 4 + cx + 1;
                l2City[index] = l1Fixed[l1Index] && l0Kind[(cz + 2) * 6 + cx + 2] == CITY_CORNER;
                if (l1Fixed[l1Index]) {
                    l2Min[index] = l1Min[l1Index];
                    l2Max[index] = l1Max[l1Index];
                    l2Fixed[index] = true;
                    any = true;
                    continue;
                }
                int cornerX = chunkX + cx;
                int cornerZ = chunkZ + cz;
                int min = Integer.MAX_VALUE;
                int max = Integer.MAX_VALUE;
                boolean constrained = false;
                for (int nz = -1; nz <= 1; nz++) {
                    for (int nx = -1; nx <= 1; nx++) {
                        if (nx == 0 && nz == 0) {
                            continue;
                        }
                        int n = (cz + 1 + nz) * 4 + cx + 1 + nx;
                        if (!l1Fixed[n]) {
                            continue;
                        }
                        int slack = (nx != 0 && nz != 0 ? DIAGONAL_SLACK : EDGE_SLACK)
                            + heightOffset(cornerX + nx, cornerZ + nz, L1_Z, L1_X);
                        min = Math.min(min, l1Min[n] - slack);
                        max = Math.min(max, l1Max[n] + slack);
                        constrained = true;
                    }
                }
                l2Min[index] = min;
                l2Max[index] = max;
                l2Fixed[index] = constrained;
                any |= constrained;
            }
        }
        return any;
    }

    /**
     * L0 of one corner ({@code getLowestCityHeightAtChunkCorner}), for the corner owned by chunk
     * {@code (chunkX + dx, chunkZ + dz)}, with rule E1 for landmark cores.
     */
    private void computeL0(int dx, int dz, int index) {
        ChunkPlan p11 = plan(dx, dz);
        ChunkPlan p01 = plan(dx - 1, dz);
        ChunkPlan p10 = plan(dx, dz - 1);
        ChunkPlan p00 = plan(dx - 1, dz - 1);
        int cities = count(p00, p01, p10, p11, ChunkKind.CITY);
        if (cities == 4) {
            l0Kind[index] = FREE;
            return;
        }
        if (cities > 0) {
            int h = Math.min(
                Math.min(cityHeightForChunk(p00), cityHeightForChunk(p01)),
                Math.min(cityHeightForChunk(p10), cityHeightForChunk(p11)));
            l0[index] = h;
            l0Kind[index] = CITY_CORNER;
            return;
        }
        int cores = count(p00, p01, p10, p11, ChunkKind.LANDMARK_CORE);
        if (cores > 0 && cores < 4) {
            // E1: a corner shared by a core and a non-city chunk is fixed at the core's natural height.
            int h = Integer.MAX_VALUE;
            h = Math.min(h, coreHeight(p00));
            h = Math.min(h, coreHeight(p01));
            h = Math.min(h, coreHeight(p10));
            h = Math.min(h, coreHeight(p11));
            l0[index] = h;
            l0Kind[index] = CORE_CORNER;
            return;
        }
        // No city chunk: only ocean chunks could constrain, and upstream ignores a corner with no city at all.
        l0Kind[index] = FREE;
    }

    private int cityHeightForChunk(ChunkPlan plan) {
        if (plan.isCity()) {
            return plan.groundY();
        }
        if (plan.ocean() && !plan.isCore()) {
            return context.settings()
                .groundLevel()
                - context.settings()
                    .oceanCorrectionBorder();
        }
        return Integer.MAX_VALUE;
    }

    private static int coreHeight(ChunkPlan plan) {
        return plan.isCore() ? plan.groundY() : Integer.MAX_VALUE;
    }

    private static int count(ChunkPlan a, ChunkPlan b, ChunkPlan c, ChunkPlan d, ChunkKind kind) {
        int n = 0;
        n += a.kind() == kind ? 1 : 0;
        n += b.kind() == kind ? 1 : 0;
        n += c.kind() == kind ? 1 : 0;
        n += d.kind() == kind ? 1 : 0;
        return n;
    }

    private ChunkPlan plan(int dx, int dz) {
        int side = 2 * REACH + 1;
        return plans[(dz + REACH) * side + dx + REACH];
    }

    private boolean reshape(ChunkBuffer buffer, int chunkX, int chunkZ, ChunkPlan self) {
        int freeMax = WorldLimits.clampY(self.naturalHeight() + FREE_CORNER_SPAN);
        int freeMin = WorldLimits.clampY(self.naturalHeight() - FREE_CORNER_SPAN);
        float max00 = l2Fixed[0] ? l2Max[0] : freeMax;
        float max10 = l2Fixed[1] ? l2Max[1] : freeMax;
        float max01 = l2Fixed[2] ? l2Max[2] : freeMax;
        float max11 = l2Fixed[3] ? l2Max[3] : freeMax;
        float min00 = l2Fixed[0] ? l2Min[0] : freeMin;
        float min10 = l2Fixed[1] ? l2Min[1] : freeMin;
        float min01 = l2Fixed[2] ? l2Min[2] : freeMin;
        float min11 = l2Fixed[3] ? l2Min[3] : freeMin;
        BlockClassifier classes = context.classifier();
        int waterLevel = context.settings()
            .waterLevel();
        float city00 = l2City[0] ? 1f : 0f;
        float city10 = l2City[1] ? 1f : 0f;
        float city01 = l2City[2] ? 1f : 0f;
        float city11 = l2City[3] ? 1f : 0f;
        boolean moved = false;
        for (int x = 0; x < 16; x++) {
            float factor = (15.0f - x) / 15.0f;
            float city0 = city11 + (city01 - city11) * factor;
            float city1 = city10 + (city00 - city10) * factor;
            float maxh0 = max11 + (max01 - max11) * factor;
            float maxh1 = max10 + (max00 - max10) * factor;
            float minh0 = min11 + (min01 - min11) * factor;
            float minh1 = min10 + (min00 - min10) * factor;
            for (int z = 0; z < 16; z++) {
                float zFactor = (15.0f - z) / 15.0f;
                float maxHeight = maxh0 + (maxh1 - maxh0) * zFactor;
                float minHeight = minh0 + (minh1 - minh0) * zFactor;
                float freedom = Math.max(0.0f, maxHeight - minHeight - NOISE_FREE_WIDTH);
                float strength = Math.min(1.0f, freedom / NOISE_RAMP);
                float offset = context.transitionNoise((chunkX << 4) + x, (chunkZ << 4) + z) * strength;
                // Deviation (M12, 0008 §5): away from city corners the cut is dithered per column and wobbled by the
                // transition noise, so a bank's 1-block steps follow ragged contours instead of straight terraces;
                // at a city corner the weight is 0 and the street's edge keeps its height.
                float bank = 1f - (city0 + ((city1 - city0) * (15.0f - z) / 15.0f));
                int worldX = (chunkX << 4) + x;
                int worldZ = (chunkZ << 4) + z;
                float rough = bank <= 0f ? 0f
                    : bank * (dither(worldX, worldZ)
                        + BANK_WOBBLE * context.transitionNoise(worldX * 3 + 7, worldZ * 3 + 7));
                maxHeight = Math.min(maxHeight + offset + rough, WorldLimits.BUILD_HEIGHT);
                minHeight = Math.max(minHeight + offset + rough, WorldLimits.MIN_Y);
                int touched = ColumnOps
                    .moveDown(buffer, classes, x, z, (int) Math.floor(maxHeight), WorldLimits.BUILD_HEIGHT, waterLevel);
                if (touched == ColumnOps.NOT_TOUCHED) {
                    touched = ColumnOps.moveUp(buffer, classes, x, z, (int) Math.floor(minHeight));
                }
                moved |= touched != ColumnOps.NOT_TOUCHED;
            }
        }
        return moved;
    }

    /**
     * A per-column value in [0, 1) from the world seed and the column: adding it before the floor rounds the cut
     * stochastically, so a gentle ramp's steps scatter rather than run in straight lines.
     *
     * @param worldX block X
     * @param worldZ block Z
     * @return the dither
     */
    float dither(int worldX, int worldZ) {
        long hash = StableHash
            .mix64(context.worldSeed() ^ DITHER_SALT ^ (long) worldX * 341873128712L ^ (long) worldZ * 132897987541L);
        return (float) StableHash.unitDouble(hash);
    }

    /**
     * Upstream's {@code getRandomizedOffset}: one draw in {@code min..max}, seeded by the corner and world (TERR-27).
     */
    int randomizedOffset(int chunkX, int chunkZ, int min, int max) {
        jitter.setSeed(SeedMix.chunk(context.worldSeed(), chunkX, chunkZ, OFFSET_X, OFFSET_Z));
        return jitter.nextInt(max - min + 1) + min;
    }

    /** Upstream's {@code getHeightOffsetL1} and {@code L2} (factors swapped): 0..4, seeded by corner and world. */
    int heightOffset(int chunkX, int chunkZ, long xFactor, long zFactor) {
        jitter.setSeed(SeedMix.chunk(context.worldSeed(), chunkX, chunkZ, xFactor, zFactor));
        return jitter.nextInt(SLACK_JITTER);
    }

    /** Returns L2 of one of the last chunk's corners, for tests: index {@code z × 2 + x}; {@code null} when free. */
    int[] l2Corner(int index) {
        return l2Fixed[index] ? new int[] { l2Min[index], l2Max[index] } : null;
    }
}
