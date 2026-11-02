package com.tuxprogrammer.lostcities.worldgen.damage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.block.BlockChoice;
import com.tuxprogrammer.lostcities.asset.block.BlockColors;
import com.tuxprogrammer.lostcities.layout.random.StableHash;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;
import com.tuxprogrammer.lostcities.worldgen.terrain.BlockClassifier;

/**
 * Partial repairs of explosion damage: some explosions' holes were filled back in with a similar but not identical
 * material, a visible patch (alpha review AR-9, docs/design/0008-city-edges-and-character.md §4).
 *
 * <ul>
 * <li>An explosion is repaired with chance {@code damageRepairChance}, from the world seed and its center, so every
 * chunk it reaches agrees.</li>
 * <li>A repaired explosion is filled up to a repair line, {@code bottom + (0.45 + 0.4 u) × diameter}, varied by up to
 * one block per column; above it the damage stays.</li>
 * <li>Only in city chunks: inside a building from its lowest cellar up, elsewhere up to city ground.</li>
 * <li>A see-through block (glass, panes) is boarded with planks, one wood per explosion; an opaque block becomes the
 * repair material whose measured color is nearest a CIE76 difference of {@value #TARGET_DIFFERENCE} from it, between
 * {@value #MIN_DIFFERENCE} and {@value #MAX_DIFFERENCE}, one of the three nearest by a hash of the explosion and the
 * block; the same block always takes the same patch in one repair.</li>
 * </ul>
 *
 * <p>
 * Not thread-safe (a bounded per-world cache of patches); one per world. Side: both (plain Java).
 */
public final class Repairs {

    /** No repairs: every explosion's damage stays. */
    public static final Repairs NONE = new Repairs(0f, Collections.<Material>emptyList(),
        Collections.<BlockRef>emptyList(), BlockColors.EMPTY);

    /** The color difference a patch aims for: visibly different, clearly related. */
    static final float TARGET_DIFFERENCE = 12f;
    /** Least color difference of a patch: anything closer reads as the same block. */
    static final float MIN_DIFFERENCE = 6f;
    /** Most color difference of a patch: anything further reads as a different material. */
    static final float MAX_DIFFERENCE = 30f;
    /** How many of the nearest materials a patch is drawn from. */
    static final int CHOICES = 3;
    /** The lowest repair line, as a share of the explosion's diameter above its bottom. */
    static final float LINE_MIN = 0.45f;
    /** The spread of the repair line, as a share of the diameter. */
    static final float LINE_SPREAD = 0.4f;
    /** Patches cached per world; the distinct blocks of one region stay far below this. */
    static final int CACHE = 4096;

    private static final long REPAIR_SALT = 0x52455041_49525321L;

    /** A repair material and its measured color. */
    static final class Material {

        final BlockRef block;
        final BlockColors.Entry color;

        Material(BlockRef block, BlockColors.Entry color) {
            this.block = block;
            this.color = color;
        }
    }

    private final float chance;
    private final List<Material> materials;
    private final List<BlockRef> boards;
    private final BlockColors colors;
    private final Map<Long, BlockRef> patches = new HashMap<>();

    Repairs(float chance, List<Material> materials, List<BlockRef> boards, BlockColors colors) {
        this.chance = chance;
        this.materials = materials;
        this.boards = boards;
        this.colors = colors;
    }

    /**
     * Builds the repairs of one world from the loaded assets: the code-named repair materials and boards, measured by
     * the shipped block colors.
     *
     * @param assets the loaded assets
     * @param chance {@code damageRepairChance}
     * @return the repairs; {@link #NONE} when the chance is 0
     */
    public static Repairs of(AssetSet assets, float chance) {
        if (chance <= 0) {
            return NONE;
        }
        BlockColors colors = assets.colors();
        List<Material> materials = new ArrayList<>();
        List<BlockRef> boards = new ArrayList<>();
        for (Map.Entry<String, BlockChoice> entry : assets.codeBlocks()
            .entrySet()) {
            BlockRef block = entry.getValue()
                .block();
            if (block == null || block.isAir()) {
                continue;
            }
            if (entry.getKey()
                .startsWith("repair.boards.")) {
                boards.add(block);
            } else if (entry.getKey()
                .startsWith("repair.material.")) {
                BlockColors.Entry color = colors.get(block);
                if (color != null && color.isOpaque()) {
                    materials.add(new Material(block, color));
                }
            }
        }
        return new Repairs(chance, materials, boards, colors);
    }

    /** Returns whether any repair can happen. */
    public boolean active() {
        return chance > 0 && !materials.isEmpty();
    }

    /**
     * Returns whether an explosion was repaired: a pure function of the world seed and the explosion.
     *
     * @param worldSeed the world seed
     * @param explosion the explosion
     * @return whether its damage is partly filled in
     */
    public boolean isRepaired(long worldSeed, Explosion explosion) {
        return StableHash.unitDouble(hash(worldSeed, explosion)) < chance;
    }

    /**
     * Returns the block Y up to which a repaired explosion's damage is filled in, at one column.
     *
     * @param worldSeed the world seed
     * @param explosion the explosion
     * @param x         world X
     * @param z         world Z
     * @return the repair line, block Y (inclusive)
     */
    public static int repairLine(long worldSeed, Explosion explosion, int x, int z) {
        long hash = hash(worldSeed, explosion);
        double u = StableHash.unitDouble(StableHash.mix64(hash ^ 0x4c494e45L));
        int bottom = explosion.y() - explosion.radius();
        int line = bottom + (int) Math.round((LINE_MIN + LINE_SPREAD * u) * 2 * explosion.radius());
        long column = StableHash.mix64(hash ^ (long) x * 341873128712L ^ (long) z * 132897987541L);
        return line + StableHash.floorModHash(column, 3) - 1;
    }

    /**
     * Returns the patch a breaking block becomes, or {@code null} when it stays broken.
     *
     * @param worldSeed the world seed
     * @param plan      the chunk's plan
     * @param area      the chunk's damage area
     * @param classes   the block classes
     * @param block     the block that breaks
     * @param x         local X, 0 to 15
     * @param y         block Y
     * @param z         local Z, 0 to 15
     * @return the patch, or {@code null}
     */
    public BlockRef patch(long worldSeed, ChunkPlan plan, DamageArea area, BlockClassifier classes, BlockRef block,
        int x, int y, int z) {
        if (!active() || !plan.isCity()) {
            return null;
        }
        if (plan.hasBuilding() ? y < plan.buildingBottom() : y > plan.groundY()) {
            return null;
        }
        int wx = (area.chunkX() << 4) + x;
        int wz = (area.chunkZ() << 4) + z;
        Explosion explosion = nearest(area, wx, y, wz);
        if (explosion == null || !isRepaired(worldSeed, explosion) || y > repairLine(worldSeed, explosion, wx, wz)) {
            return null;
        }
        long key = hash(worldSeed, explosion) ^ ((long) block.hashCode() << 1);
        BlockRef cached = patches.get(key);
        if (cached != null) {
            return cached.isAir() ? null : cached;
        }
        BlockRef patch = choose(hash(worldSeed, explosion), block, classes);
        if (patches.size() >= CACHE) {
            patches.clear();
        }
        patches.put(key, patch == null ? BlockRef.AIR : patch);
        return patch;
    }

    /** The explosion whose sphere holds the block most deeply (smallest distance over radius), or {@code null}. */
    static Explosion nearest(DamageArea area, int x, int y, int z) {
        Explosion best = null;
        double bestShare = Double.MAX_VALUE;
        for (Explosion explosion : area.explosions()) {
            double share = explosion.squaredDistanceTo(x, y, z) / Math.max(1, explosion.squaredRadius());
            if (share < bestShare) {
                bestShare = share;
                best = explosion;
            }
        }
        return best;
    }

    private BlockRef choose(long hash, BlockRef block, BlockClassifier classes) {
        if ((classes.flags(block) & BlockClassifier.EASY_BREAKABLE) != 0) {
            return boards.isEmpty() ? null : boards.get(StableHash.floorModHash(hash ^ 0x424f4152L, boards.size()));
        }
        BlockColors.Entry color = colors.get(block);
        if (color == null || !color.isOpaque()) {
            return null;
        }
        List<Material> near = similar(color, block);
        if (near.isEmpty()) {
            return null;
        }
        return near.get(StableHash.floorModHash(StableHash.mix64(hash ^ block.hashCode()), near.size())).block;
    }

    /**
     * Returns up to {@value #CHOICES} repair materials whose color differs from a block's by
     * {@value #MIN_DIFFERENCE} to {@value #MAX_DIFFERENCE}, nearest a difference of {@value #TARGET_DIFFERENCE}
     * first; never the block itself.
     *
     * @param color the block's color
     * @param block the block
     * @return the candidates, best first
     */
    List<Material> similar(BlockColors.Entry color, BlockRef block) {
        List<Material> out = new ArrayList<>();
        List<Float> scores = new ArrayList<>();
        for (Material material : materials) {
            if (material.block.equals(block)) {
                continue;
            }
            float difference = difference(color, material.color);
            if (difference < MIN_DIFFERENCE || difference > MAX_DIFFERENCE) {
                continue;
            }
            float score = Math.abs(difference - TARGET_DIFFERENCE);
            int at = 0;
            while (at < scores.size() && scores.get(at) <= score) {
                at++;
            }
            out.add(at, material);
            scores.add(at, score);
        }
        return out.size() > CHOICES ? new ArrayList<>(out.subList(0, CHOICES)) : out;
    }

    /** The CIE76 difference of two colors: Euclidean distance in CIELAB. */
    static float difference(BlockColors.Entry a, BlockColors.Entry b) {
        float dl = a.lightness() - b.lightness();
        float da = a.a() - b.a();
        float db = a.b() - b.b();
        return (float) Math.sqrt(dl * dl + da * da + db * db);
    }

    private static long hash(long worldSeed, Explosion explosion) {
        return StableHash.mix64(
            worldSeed ^ REPAIR_SALT
                ^ (long) explosion.x() * 341873128712L
                ^ (long) explosion.z() * 132897987541L
                ^ (long) explosion.y() * 2654435761L
                ^ explosion.radius());
    }
}
