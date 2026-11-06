package com.tuxprogrammer.lostcities.worldgen.city;

import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.block.BlockChoice;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.LotKind;
import com.tuxprogrammer.lostcities.layout.MultiPos;
import com.tuxprogrammer.lostcities.layout.random.StableHash;
import com.tuxprogrammer.lostcities.worldgen.terrain.WorldLimits;

/**
 * Writes the city districts' chunks (city-realism phase 5c, D-p5c-1 to D-p5c-3): the plaza with its monument and the
 * industrial yard. Both write only their own chunk's columns, from hashes of the seed and the block's position, so
 * they read the same whenever they are written.
 *
 * <ul>
 * <li><b>Plaza:</b> paved with a pattern of stone brick and light gray clay; at the center of its footprint (one
 * chunk, or the 2 x 2) a square basin of still water inside a stone-brick wall, and in the middle of the basin a
 * pillar of GT machine casings, tiers ULV to EV, with a quartz cap. No tile entities.</li>
 * <li><b>Industrial yard:</b> concrete, cobblestone and gravel ground inside a fence with a gap in the middle of each
 * side; a casing-walled shed with window slits, a door and a row of casing "machines" along its back wall; a tank of
 * bronze-plated bricks, a brick smokestack, iron-bar pipes and cauldron drums. The layout is rotated by a hash. GT
 * casings are plain blocks: placing real GT machine tile entities from generation code is not reliable, so the yard
 * has none (D-p5c-3).</li>
 * </ul>
 * Side: both (plain Java).
 */
final class DistrictWriter {

    /** Salt of the yard's and plaza's hashes. */
    private static final long SALT = 0x444953545752544cL;
    /** Pillar height of the plaza monument. */
    private static final int PILLAR = 9;
    /** First and last column of a side's gap in the yard fence. */
    private static final int GAP_FROM = 6;
    private static final int GAP_TO = 9;

    private final CityTarget target;
    private final ChunkPlan plan;
    private final long seed;
    private final int originX;
    private final int originZ;
    private final BlockRef clay;
    private final BlockRef brick;
    private final BlockRef cobble;
    private final BlockRef gravel;
    private final BlockRef water;
    private final BlockRef quartz;
    private final BlockRef fence;
    private final BlockRef bars;
    private final BlockRef pane;
    private final BlockRef cauldron;
    private final BlockRef stack;
    private final BlockRef tank;
    private final BlockRef[] casings = new BlockRef[5];

    DistrictWriter(CityTarget target, ChunkPlan plan, AssetSet assets, long worldSeed) {
        this.target = target;
        this.plan = plan;
        this.seed = worldSeed ^ SALT;
        this.originX = plan.chunkX() << 4;
        this.originZ = plan.chunkZ() << 4;
        this.clay = code(assets, "lot.concrete", BlockRef.of("minecraft:stained_hardened_clay", 8));
        this.brick = code(assets, "district.stone_brick", BlockRef.of("minecraft:stonebrick", 0));
        this.cobble = code(assets, "district.cobblestone", BlockRef.of("minecraft:cobblestone", 0));
        this.gravel = code(assets, "lot.gravel", BlockRef.of("minecraft:gravel", 0));
        this.water = code(assets, "district.water", BlockRef.of("minecraft:water", 0));
        this.quartz = code(assets, "district.quartz", BlockRef.of("minecraft:quartz_block", 0));
        this.fence = code(assets, "lot.fence", BlockRef.of("minecraft:fence", 0));
        this.bars = code(assets, "iron_bars", BlockRef.of("minecraft:iron_bars", 0));
        this.pane = code(assets, "district.pane", BlockRef.of("minecraft:glass_pane", 0));
        this.cauldron = code(assets, "district.cauldron", BlockRef.of("minecraft:cauldron", 0));
        this.stack = code(assets, "district.smokestack", BlockRef.of("minecraft:brick_block", 0));
        this.tank = code(assets, "district.tank", BlockRef.of("gregtech:gt.blockcasings", 10));
        for (int tier = 0; tier < casings.length; tier++) {
            casings[tier] = code(assets, "district.casing" + tier, BlockRef.of("gregtech:gt.blockcasings", tier));
        }
    }

    private static BlockRef code(AssetSet assets, String key, BlockRef fallback) {
        BlockChoice choice = assets.codeBlock(key);
        BlockRef block = choice == null ? null : choice.block();
        return block == null ? fallback : block;
    }

    /**
     * Writes the chunk.
     *
     * @param height the block Y of the city's ground surface
     */
    void write(int height) {
        target.clearToMax(height + 1);
        if (plan.lot() == LotKind.PLAZA) {
            plaza(height);
        } else {
            yard(height);
        }
    }

    // ------------------------------------------------------------------ plaza

    private void plaza(int height) {
        MultiPos pos = plan.lotFootprint();
        int w = pos.isMulti() ? pos.width() : 1;
        int ox = plan.chunkX() - (pos.isMulti() ? pos.x() : 0);
        int oz = plan.chunkZ() - (pos.isMulti() ? pos.z() : 0);
        // The footprint's center as an edge coordinate; a block's center is at wx + 0.5.
        int ex = 16 * ox + 8 * w;
        int ez = 16 * oz + 8 * w;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int wx = originX + x;
                int wz = originZ + z;
                int dx2 = 2 * wx + 1 - 2 * ex;
                int dz2 = 2 * wz + 1 - 2 * ez;
                int m = Math.max(Math.abs(dx2), Math.abs(dz2));
                target.set(x, height, z, paving(wx, wz, m));
                monument(x, z, height, m);
            }
        }
    }

    /** Stone brick in a checker of 2-block squares, with a band of clay at every sixth ring from the center. */
    private BlockRef paving(int wx, int wz, int m) {
        if ((m / 2 + 1) % 6 == 0) {
            return clay;
        }
        return ((wx >> 1) + (wz >> 1) & 1) == 0 ? brick : clay;
    }

    private void monument(int x, int z, int height, int m) {
        if (m <= 1) {
            for (int dy = 1; dy <= PILLAR; dy++) {
                BlockRef block = dy == PILLAR ? quartz : casings[(dy - 1) % casings.length];
                put(x, height + dy, z, block);
            }
        } else if (m == 3) {
            put(x, height + 1, z, brick);
            put(x, height + 2, z, brick);
        } else if (m <= 7) {
            put(x, height + 1, z, water);
        } else if (m == 9) {
            put(x, height + 1, z, brick);
            put(x, height + 2, z, brick);
        }
    }

    // ------------------------------------------------------------------ yard

    private void yard(int height) {
        int rot = (int) Math.floorMod(StableHash.mix64(seed ^ originX * 31L ^ originZ * 17L), 4L);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                target.set(x, height, z, ground(originX + x, originZ + z));
                if (fenceAt(x, z)) {
                    put(x, height + 1, z, fence);
                }
                int sx;
                int sz;
                switch (rot) {
                    case 1:
                        sx = z;
                        sz = 15 - x;
                        break;
                    case 2:
                        sx = 15 - x;
                        sz = 15 - z;
                        break;
                    case 3:
                        sx = 15 - z;
                        sz = x;
                        break;
                    default:
                        sx = x;
                        sz = z;
                }
                for (int dy = 1; dy <= 9; dy++) {
                    BlockRef block = feature(sx, sz, dy);
                    if (block != null) {
                        put(x, height + dy, z, block);
                    }
                }
            }
        }
    }

    private BlockRef ground(int wx, int wz) {
        double u = StableHash
            .unitDouble(StableHash.mix64(seed ^ (long) wx * 341873128712L ^ (long) wz * 132897987541L));
        return u < 0.55 ? clay : u < 0.80 ? cobble : gravel;
    }

    /** The fence outline one block inside the chunk edge, open in the middle of each side. */
    private static boolean fenceAt(int x, int z) {
        boolean onX = (x == 1 || x == 14) && z >= 1 && z <= 14;
        boolean onZ = (z == 1 || z == 14) && x >= 1 && x <= 14;
        if (onX && !onZ) {
            return z < GAP_FROM || z > GAP_TO;
        }
        if (onZ && !onX) {
            return x < GAP_FROM || x > GAP_TO;
        }
        return onX;
    }

    /** The yard's contents at a source column and height above ground, or {@code null} for none. */
    private BlockRef feature(int sx, int sz, int dy) {
        // Shed: x 3 to 10, z 3 to 8, walls and roof five high; door in the south wall.
        if (sx >= 3 && sx <= 10 && sz >= 3 && sz <= 8 && dy <= 5) {
            boolean wall = sx == 3 || sx == 10 || sz == 3 || sz == 8;
            if (dy == 5) {
                return casings[0];
            }
            if (wall) {
                if (sz == 8 && (sx == 6 || sx == 7) && dy <= 2) {
                    return BlockRef.AIR;
                }
                if (dy == 3 && (sx == 5 || sx == 8 || sz == 5 && (sx == 3 || sx == 10))) {
                    return pane;
                }
                return casings[1];
            }
            // Machines along the back wall.
            if (sz == 4 && dy <= 1 + (sx & 1)) {
                return casings[2 + (sx % 3)];
            }
            return BlockRef.AIR;
        }
        // Tank: 3 x 3, six high.
        if (sx >= 11 && sx <= 13 && sz >= 3 && sz <= 5 && dy <= 6) {
            return tank;
        }
        // Pipe from the tank to the stack.
        if (sx == 12 && sz >= 6 && sz <= 11 && dy <= 2) {
            return bars;
        }
        // Smokestack.
        if (sx == 12 && sz == 12) {
            return stack;
        }
        // Drums.
        if (dy == 1 && sz == 11 && (sx == 4 || sx == 5 || sx == 7)) {
            return cauldron;
        }
        return null;
    }

    private void put(int x, int y, int z, BlockRef block) {
        if (y <= WorldLimits.MAX_Y) {
            target.set(x, y, z, block);
        }
    }
}
