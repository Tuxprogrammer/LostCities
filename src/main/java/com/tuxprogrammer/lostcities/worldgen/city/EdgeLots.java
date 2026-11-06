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
 * Writes an edge lot: an empty or construction lot at a city's edge, growth that never happened (alpha review AR-6,
 * docs/design/0011-city-edges-and-character.md §3).
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
 * <b>Construction lots at real sizes</b> (city-realism phase 3, D326 and D327) span a footprint of up to 2 x 2
 * chunks. Each chunk writes only its own columns, in the footprint's coordinates: the rim, walls and fence follow the
 * footprint's outline (with a gap in the middle of each chunk's outer side), the pit runs across the inner chunk
 * borders, and one ramp climbs from one side, steeper where the pit is deeper than the ramp is long. The frame is a
 * concrete skeleton or a wooden scaffold, picked per footprint, with a deck over one bay at every floor below its top.
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
    /** Salt of a construction lot's frame choices. */
    static final long FRAME_SALT = 0x4652414d45534b4cL;

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
    private final BlockRef scaffoldPost;
    private final BlockRef scaffoldDeck;
    /** Whether the lot was drawn at a real size (phase 3); false for the single-chunk lots of 0011 section 3. */
    private final boolean real;
    /** The chunk's offset in its footprint, in blocks, and the footprint's size in blocks. */
    private final int offsetX;
    private final int offsetZ;
    private final int sizeX;
    private final int sizeZ;

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
        this.scaffoldPost = code(assets, "lot.scaffold_post", BlockRef.of("minecraft:fence", 0));
        this.scaffoldDeck = code(assets, "lot.scaffold_deck", BlockRef.of("minecraft:planks", 0));
        MultiPos pos = plan.lotFootprint();
        this.real = pos.isMulti();
        this.offsetX = real ? pos.x() * 16 : 0;
        this.offsetZ = real ? pos.z() * 16 : 0;
        this.sizeX = real ? pos.width() * 16 : 16;
        this.sizeZ = real ? pos.height() * 16 : 16;
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
        int rampSide = (int) Math.floorMod(StableHash.mix64(seed ^ originChunkX() * 31L ^ originChunkZ() * 17L), 4L);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int fx = offsetX + x;
                int fz = offsetZ + z;
                if (fx < RIM || fz < RIM || fx >= sizeX - RIM || fz >= sizeZ - RIM) {
                    continue;
                }
                int raised = ramp(fx, fz, rampSide, depth, sizeX, sizeZ);
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
            if (real) {
                frame(height, depth);
            } else {
                columns(height, depth);
            }
        }
        fence(height);
    }

    /** Chunk X of the footprint's top-left chunk (the chunk itself for a single-chunk edge lot). */
    private int originChunkX() {
        return plan.chunkX() - offsetX / 16;
    }

    /** Chunk Z of the footprint's top-left chunk. */
    private int originChunkZ() {
        return plan.chunkZ() - offsetZ / 16;
    }

    /** Whether a footprint column lies on the pit's outline, one column outside its inside. */
    private boolean onOutline(int fx, int fz) {
        boolean xEdge = fx == RIM - 1 || fx == sizeX - RIM;
        boolean zEdge = fz == RIM - 1 || fz == sizeZ - RIM;
        boolean xSpan = fx >= RIM - 1 && fx <= sizeX - RIM;
        boolean zSpan = fz >= RIM - 1 && fz <= sizeZ - RIM;
        return xEdge && zSpan || zEdge && xSpan;
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
        return ramp(x, z, side, depth, 16, 16);
    }

    /**
     * The ramp in a footprint of {@code sizeX x sizeZ} blocks (phase 3), in footprint coordinates. It lies in the
     * middle of the first chunk along its side and runs inward one column per block of depth, as far as the pit
     * allows; a pit deeper than that gets a steeper ramp, so the ramp's foot still meets the pit's floor. For a single
     * chunk at most 4 deep it is the ramp of {@link #ramp(int, int, int, int)}.
     */
    static int ramp(int x, int z, int side, int depth, int sizeX, int sizeZ) {
        int along;
        int in;
        int inside;
        switch (side) {
            case 0:
                along = z;
                in = x - RIM;
                inside = sizeX - 2 * RIM;
                break;
            case 1:
                along = z;
                in = sizeX - 1 - RIM - x;
                inside = sizeX - 2 * RIM;
                break;
            case 2:
                along = x;
                in = z - RIM;
                inside = sizeZ - 2 * RIM;
                break;
            default:
                along = x;
                in = sizeZ - 1 - RIM - z;
                inside = sizeZ - 2 * RIM;
        }
        int run = Math.min(depth, inside - 1);
        if (run <= 0 || along < GAP_FROM || along > GAP_TO || in < 0 || in >= run) {
            return 0;
        }
        int step = (depth + run - 1) / run;
        return Math.max(0, depth - step * (in + 1));
    }

    /** Dresses the pit's walls in the lot's ground, so the dig shows clay and dirt rather than the fill under it. */
    private void walls(int height, int depth) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (!onOutline(offsetX + x, offsetZ + z)) {
                    continue;
                }
                BlockRef wall = ground(x, z);
                for (int y = Math.max(WorldLimits.MIN_Y, height - depth); y < height; y++) {
                    target.set(x, y, z, wall);
                }
            }
        }
    }

    /** A foundation's 2 × 2 concrete columns, rebar on top, and at two floors or more a deck over one bay. */
    private void columns(int height, int depth) {
        int[] grid = depth >= 4 ? DEEP_COLUMNS : SHALLOW_COLUMNS;
        int floors = plan.lotColumnFloors();
        int top = Math.min(WorldLimits.MAX_Y - REBAR, height + floors * plan.floorPitch() - 1);
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
            int deck = height + plan.floorPitch();
            int from = grid[0];
            int to = grid[1] + 1;
            for (int x = from; x <= to; x++) {
                for (int z = from; z <= to; z++) {
                    target.set(x, deck, z, concrete);
                }
            }
        }
    }

    /** A construction fence along the pit's edge, open in the middle of each chunk's side. */
    private void fence(int height) {
        int y = height + 1;
        if (y > WorldLimits.MAX_Y) {
            return;
        }
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int fx = offsetX + x;
                int fz = offsetZ + z;
                if (!onOutline(fx, fz)) {
                    continue;
                }
                boolean xEdge = fx == RIM - 1 || fx == sizeX - RIM;
                boolean zEdge = fz == RIM - 1 || fz == sizeZ - RIM;
                // A corner lies on two sides; it stays open only where it falls in the gap of both.
                boolean open = (!xEdge || z >= GAP_FROM && z <= GAP_TO) && (!zEdge || x >= GAP_FROM && x <= GAP_TO);
                if (!open) {
                    target.set(x, y, z, fence);
                }
            }
        }
    }

    /**
     * A construction lot's frame (phase 3, D327): the foundation's 2 x 2 column grid rising its floors at the
     * pinned pitch, as a concrete skeleton topped with rebar or a wooden scaffold of fence posts, picked per
     * footprint; at every floor below the top a deck covers one bay of the chunk's grid, a different bay per floor.
     */
    private void frame(int height, int depth) {
        int[] grid = depth >= 4 ? DEEP_COLUMNS : SHALLOW_COLUMNS;
        int floors = plan.lotColumnFloors();
        int pitch = plan.floorPitch();
        int top = Math.min(WorldLimits.MAX_Y - REBAR, height + floors * pitch - 1);
        long footprint = StableHash
            .mix64(seed ^ FRAME_SALT ^ originChunkX() * 341873128712L ^ originChunkZ() * 132897987541L);
        boolean scaffold = (footprint & 1L) == 1L;
        BlockRef post = scaffold ? scaffoldPost : concrete;
        BlockRef deck = scaffold ? scaffoldDeck : concrete;
        for (int gx : grid) {
            for (int gz : grid) {
                for (int dx = 0; dx < 2; dx++) {
                    for (int dz = 0; dz < 2; dz++) {
                        int x = gx + dx;
                        int z = gz + dz;
                        for (int y = Math.max(WorldLimits.MIN_Y, height - depth + 1); y <= top; y++) {
                            target.set(x, y, z, post);
                        }
                        if (!scaffold) {
                            for (int y = top + 1; y <= top + REBAR; y++) {
                                target.set(x, y, z, (dx + dz) % 2 == 0 ? rebar : BlockRef.AIR);
                            }
                        }
                    }
                }
            }
        }
        int bays = grid.length - 1;
        for (int k = 1; k < floors; k++) {
            int y = height + k * pitch;
            if (y > top) {
                break;
            }
            long bay = StableHash
                .mix64(footprint ^ plan.chunkX() * 31L ^ plan.chunkZ() * 17L ^ k * 0x9E3779B97F4A7C15L);
            int bx = (int) Math.floorMod(bay, (long) bays);
            int bz = (int) Math.floorMod(bay >>> 16, (long) bays);
            for (int x = grid[bx]; x <= grid[bx + 1] + 1; x++) {
                for (int z = grid[bz]; z <= grid[bz + 1] + 1; z++) {
                    target.set(x, y, z, deck);
                }
            }
        }
    }
}
