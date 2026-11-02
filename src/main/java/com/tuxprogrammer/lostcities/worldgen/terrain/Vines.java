package com.tuxprogrammer.lostcities.worldgen.terrain;
// Ported from Lost Cities (MIT): https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/ChunkFixer.java
// Avoids: TERR-11, TERR-26, TERR-32

import java.util.Random;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.worldgen.buffer.ChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.damage.DamageArea;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;

/**
 * Vines hanging on building walls, written by the chunk that holds the vine blocks, on all four of its sides,
 * against the neighbor's wall face as the plan describes it (docs/new-spec/03-terrain/index.md, "Vines on building
 * walls", P03-9, D31). For each side whose neighbor has a building, each candidate block of the band between this
 * chunk's own top (or its ground plus 3) and the neighbor's top starts a strip with {@code vineChance}; a strip
 * continues down with 0.8 per block while the block is air and above the band's bottom.
 *
 * <p>
 * Upstream (ChunkFixer) wrote into the +X and +Z neighbors only when they had reached {@code FEATURES} and checked
 * {@code canSurvive} against blocks that may not exist yet. Here a pure rule replaces that test: a vine is placed
 * only where the plan says the neighbor's wall stands at that height and the neighbor's damage area does not reach
 * the section. Fixes:
 * <ul>
 * <li>TERR-11: the result never depends on whether a neighbor was generated;</li>
 * <li>TERR-32: every column of a wall, 0 to 15, takes vines;</li>
 * <li>TERR-26: there is no border vine cleanup, because no vine is ever placed without the wall the plan keeps.</li>
 * </ul>
 *
 * <p>
 * Side: both (plain Java).
 */
public final class Vines {

    /** Chance per block a strip continues downward. */
    static final float CONTINUE_CHANCE = 0.8f;
    /** The band starts this far above a chunk's city ground. */
    static final int BAND_ABOVE_GROUND = 3;
    /** Upstream skips vines entirely below this chance. */
    static final float MIN_CHANCE = 0.000001f;

    private static final long FIXER_X = 341873128712L;
    private static final long FIXER_Z = 132897987541L;

    /** 1.7.10 vine metadata per side the vine attaches to: west 2, east 8, north 4, south 1. */
    private static final int[] VINE_META = { 2, 8, 4, 1 };
    private static final BlockRef[] VINES = { BlockRef.of("minecraft:vine", VINE_META[0]),
        BlockRef.of("minecraft:vine", VINE_META[1]), BlockRef.of("minecraft:vine", VINE_META[2]),
        BlockRef.of("minecraft:vine", VINE_META[3]) };

    private Vines() {}

    /**
     * Returns the vine random of a chunk: upstream's {@code ChunkFixer} reseed, which already mixes the world seed.
     *
     * @param worldSeed the world seed
     * @param chunkX    chunk X
     * @param chunkZ    chunk Z
     * @return a new random
     */
    public static Random random(long worldSeed, int chunkX, int chunkZ) {
        return new Random(worldSeed ^ chunkX * FIXER_X ^ chunkZ * FIXER_Z);
    }

    /**
     * Writes the chunk's vines.
     *
     * @param buffer      the chunk, after its content
     * @param context     the world's terrain context
     * @param plan        the chunk's plan
     * @param sides       the plans toward −X, +X, −Z and +Z
     * @param sideDamage  the damage areas of those neighbors, in the same order
     * @param random      the vine random
     * @return the number of vine blocks placed
     */
    public static int grow(ChunkBuffer buffer, TerrainContext context, ChunkPlan plan, ChunkPlan[] sides,
        DamageArea[] sideDamage, Random random) {
        float chance = context.settings()
            .vineChance();
        if (chance < MIN_CHANCE || plan.isCore()) {
            return 0;
        }
        int ownBottom = plan.groundY() + BAND_ABOVE_GROUND;
        int bottom = Math.max(ownBottom, plan.hasBuilding() ? plan.maxHeight() : ownBottom);
        int placed = 0;
        for (ChunkPlan.Side side : ChunkPlan.Side.values()) {
            ChunkPlan wall = sides[side.ordinal()];
            if (!wall.hasBuilding()) {
                continue;
            }
            int top = Math.min(wall.maxHeight(), WorldLimits.BUILD_HEIGHT);
            BlockRef vine = VINES[side.ordinal()];
            for (int along = 0; along < 16; along++) {
                int x = side == ChunkPlan.Side.WEST ? 0 : side == ChunkPlan.Side.EAST ? 15 : along;
                int z = side == ChunkPlan.Side.NORTH ? 0 : side == ChunkPlan.Side.SOUTH ? 15 : along;
                for (int y = bottom; y < top; y++) {
                    if (random.nextFloat() < chance) {
                        placed += strip(buffer, wall, sideDamage[side.ordinal()], vine, x, y, z, bottom, random);
                    }
                }
            }
        }
        return placed;
    }

    private static int strip(ChunkBuffer buffer, ChunkPlan wall, DamageArea wallDamage, BlockRef vine, int x,
        int startY, int z, int bottom, Random random) {
        if (!canHang(buffer, wall, wallDamage, x, startY, z)) {
            return 0;
        }
        buffer.set(x, startY, z, vine);
        int placed = 1;
        int y = startY - 1;
        while (y >= bottom && random.nextFloat() < CONTINUE_CHANCE) {
            if (!canHang(buffer, wall, wallDamage, x, y, z)) {
                break;
            }
            buffer.set(x, y, z, vine);
            placed++;
            y--;
        }
        return placed;
    }

    /** The pure survival rule: air here, the neighbor's planned wall at this height, and no damage in its section. */
    private static boolean canHang(ChunkBuffer buffer, ChunkPlan wall, DamageArea wallDamage, int x, int y, int z) {
        if (y < WorldLimits.MIN_Y || y > WorldLimits.MAX_Y || !buffer.get(x, y, z)
            .isAir()) {
            return false;
        }
        if (y < wall.groundY() || y >= wall.maxHeight()) {
            return false;
        }
        return !wallDamage.hasExplosions(y >> 4);
    }
}
