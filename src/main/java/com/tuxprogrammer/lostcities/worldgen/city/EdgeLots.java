package com.tuxprogrammer.lostcities.worldgen.city;

import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.block.BlockChoice;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.LotKind;
import com.tuxprogrammer.lostcities.layout.random.StableHash;
import com.tuxprogrammer.lostcities.worldgen.terrain.WorldLimits;

/**
 * Writes an edge lot: an empty or construction lot at a city's edge, growth that never happened (alpha review AR-6,
 * docs/design/0008-city-edges-and-character.md §3).
 *
 * <ul>
 * <li><b>Ground:</b> at city ground, per column, coarse dirt, terracotta, brown and red terracotta and a little
 * gravel, in two-block patches: bare clay and dirt that grass never spreads onto.</li>
 * <li><b>Pit:</b> inside a {@value #RIM}-block rim, the lot is dug {@code lotDepth} blocks; its walls are the same
 * ground, and a ramp of coarse dirt climbs out on one side.</li>
 * <li><b>Foundation:</b> the pit's floor is a concrete slab with a grid of 2 × 2 concrete columns rising
 * {@code lotColumnFloors} floors, topped with iron-bar rebar; at two floors or more, a deck covers one bay at the
 * first floor.</li>
 * <li>Dug lots are fenced at the pit's edge with a gap in the middle of each side, where door approaches and the ramp
 * meet the rim.</li>
 * </ul>
 * Every choice is a hash of the world seed and the block's position, so the lot is the same whenever it is written.
 *
 * <p>
 * Side: both (plain Java).
 */
final class EdgeLots {

    /** Columns of ground at city level kept on every side of a pit: a door's approach is three deep. */
    static final int RIM = 3;
    /** First and last column of a side's middle gap in the fence. */
    static final int GAP_FROM = 6;
    static final int GAP_TO = 9;
    /** Spacing of a foundation's column grid in a shallow pit (4 columns) and a deep one (9 columns). */
    static final int[] SHALLOW_COLUMNS = { 4, 10 };
    static final int[] DEEP_COLUMNS = { 3, 7, 11 };
    /** Rebar height above a column's top, in blocks. */
    static final int REBAR = 2;
    /** Salt of a lot's per-block hashes. */
    static final long SALT = 0x4c4f5447524f554eL;

    private final CityTarget target;
    private final ChunkPlan plan;
    private final long seed;
    private final int originX;
    private final int originZ;
    private final BlockRef coarseDirt;
    private final BlockRef terracotta;
    private final BlockRef brown;
    private final BlockRef red;
    private final BlockRef gravel;
    private final BlockRef concrete;
    private final BlockRef fence;
    private final BlockRef rebar;

    EdgeLots(CityTarget target, ChunkPlan plan, AssetSet assets, long worldSeed) {
        this.target = target;
        this.plan = plan;
        this.seed = worldSeed ^ SALT;
        this.originX = plan.chunkX() << 4;
        this.originZ = plan.chunkZ() << 4;
        this.coarseDirt = code(assets, "lot.coarse_dirt", BlockRef.of("minecraft:dirt", 1));
        this.terracotta = code(assets, "lot.terracotta", BlockRef.of("minecraft:hardened_clay", 0));
        this.brown = code(assets, "lot.brown_terracotta", BlockRef.of("minecraft:stained_hardened_clay", 12));
        this.red = code(assets, "lot.red_terracotta", BlockRef.of("minecraft:stained_hardened_clay", 14));
        this.gravel = code(assets, "lot.gravel", BlockRef.of("minecraft:gravel", 0));
        this.concrete = code(assets, "lot.concrete", BlockRef.of("minecraft:stained_hardened_clay", 8));
        this.fence = code(assets, "lot.fence", BlockRef.of("minecraft:fence", 0));
        this.rebar = code(assets, "iron_bars", BlockRef.of("minecraft:iron_bars", 0));
    }

    private static BlockRef code(AssetSet assets, String key, BlockRef fallback) {
        BlockChoice choice = assets.codeBlock(key);
        BlockRef block = choice == null ? null : choice.block();
        return block == null ? fallback : block;
    }

    /**
     * Writes the lot at a city ground height.
     *
     * @param height the block Y of the city's ground surface
     */
    void write(int height) {
        // Nothing stands on a lot: whatever the ground held above city level (trees, the terrain the leveling left)
        // is cleared first.
        target.clearToMax(height + 1);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                target.set(x, height, z, ground(x, z));
            }
        }
        LotKind kind = plan.lot();
        if (!kind.isDug()) {
            return;
        }
        int depth = plan.lotDepth();
        int rampSide = (int) Math.floorMod(StableHash.mix64(seed ^ plan.chunkX() * 31L ^ plan.chunkZ() * 17L), 4L);
        for (int x = RIM; x < 16 - RIM; x++) {
            for (int z = RIM; z < 16 - RIM; z++) {
                int raised = ramp(x, z, rampSide, depth);
                int floor = height - depth + raised;
                for (int y = floor + 1; y <= height; y++) {
                    target.set(x, y, z, BlockRef.AIR);
                }
                BlockRef bottom;
                if (raised > 0) {
                    bottom = coarseDirt;
                } else if (kind == LotKind.FOUNDATION) {
                    bottom = concrete;
                } else {
                    bottom = ground(x, z);
                }
                if (floor >= WorldLimits.MIN_Y) {
                    target.set(x, floor, z, bottom);
                }
            }
        }
        walls(height, depth);
        if (kind == LotKind.FOUNDATION) {
            columns(height, depth);
        }
        fence(height, rampSide);
    }

    /** The lot's ground at a column: two-block patches of clay and dirt, with single columns mixed in. */
    BlockRef ground(int x, int z) {
        int wx = originX + x;
        int wz = originZ + z;
        long patch = StableHash.mix64(seed ^ (long) (wx >> 1) * 341873128712L ^ (long) (wz >> 1) * 132897987541L);
        long single = StableHash.mix64(seed ^ (long) wx * 132897987541L ^ (long) wz * 341873128712L);
        double u = StableHash.unitDouble(StableHash.unitDouble(single) < 0.3 ? single : patch);
        if (u < 0.40) {
            return coarseDirt;
        }
        if (u < 0.65) {
            return terracotta;
        }
        if (u < 0.80) {
            return brown;
        }
        if (u < 0.88) {
            return red;
        }
        return gravel;
    }

    /**
     * How far a pit column's floor is raised by the ramp: the ramp is four columns wide in the middle of one side and
     * climbs one block per column toward the rim, so its top meets city ground.
     */
    static int ramp(int x, int z, int side, int depth) {
        int along;
        int in;
        switch (side) {
            case 0:
                along = z;
                in = x - RIM;
                break;
            case 1:
                along = z;
                in = 15 - RIM - x;
                break;
            case 2:
                along = x;
                in = z - RIM;
                break;
            default:
                along = x;
                in = 15 - RIM - z;
        }
        if (along < GAP_FROM || along > GAP_TO || in >= depth) {
            return 0;
        }
        return depth - 1 - in;
    }

    /** Dresses the pit's walls in the lot's ground, so the dig shows clay and dirt rather than the fill under it. */
    private void walls(int height, int depth) {
        for (int i = RIM - 1; i <= 16 - RIM; i++) {
            for (int y = height - depth; y < height; y++) {
                wall(RIM - 1, y, i);
                wall(16 - RIM, y, i);
                wall(i, y, RIM - 1);
                wall(i, y, 16 - RIM);
            }
        }
    }

    private void wall(int x, int y, int z) {
        if (y >= WorldLimits.MIN_Y && x >= 0 && x < 16 && z >= 0 && z < 16) {
            target.set(x, y, z, ground(x, z));
        }
    }

    /** A foundation's 2 × 2 concrete columns, rebar on top, and at two floors or more a deck over one bay. */
    private void columns(int height, int depth) {
        int[] grid = depth >= 4 ? DEEP_COLUMNS : SHALLOW_COLUMNS;
        int floors = plan.lotColumnFloors();
        int top = Math.min(WorldLimits.MAX_Y - REBAR, height + floors * ChunkPlan.FLOOR_HEIGHT - 1);
        for (int gx : grid) {
            for (int gz : grid) {
                for (int dx = 0; dx < 2; dx++) {
                    for (int dz = 0; dz < 2; dz++) {
                        int x = gx + dx;
                        int z = gz + dz;
                        for (int y = height - depth + 1; y <= top; y++) {
                            target.set(x, y, z, concrete);
                        }
                        for (int y = top + 1; y <= top + REBAR; y++) {
                            target.set(x, y, z, (dx + dz) % 2 == 0 ? rebar : BlockRef.AIR);
                        }
                    }
                }
            }
        }
        if (floors >= 2) {
            int deck = height + ChunkPlan.FLOOR_HEIGHT;
            int from = grid[0];
            int to = grid[1] + 1;
            for (int x = from; x <= to; x++) {
                for (int z = from; z <= to; z++) {
                    target.set(x, deck, z, concrete);
                }
            }
        }
    }

    /** A construction fence along the pit's edge, open in the middle of each side. */
    private void fence(int height, int rampSide) {
        int y = height + 1;
        if (y > WorldLimits.MAX_Y) {
            return;
        }
        for (int i = RIM - 1; i <= 16 - RIM; i++) {
            if (i >= GAP_FROM && i <= GAP_TO) {
                continue;
            }
            target.set(RIM - 1, y, i, fence);
            target.set(16 - RIM, y, i, fence);
            target.set(i, y, RIM - 1, fence);
            target.set(i, y, 16 - RIM, fence);
        }
    }
}
