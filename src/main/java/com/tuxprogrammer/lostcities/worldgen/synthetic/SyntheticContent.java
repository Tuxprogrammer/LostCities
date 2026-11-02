package com.tuxprogrammer.lostcities.worldgen.synthetic;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.worldgen.buffer.ChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkContent;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;
import com.tuxprogrammer.lostcities.worldgen.plan.ContentContext;
import com.tuxprogrammer.lostcities.worldgen.terrain.CityTerrain;
import com.tuxprogrammer.lostcities.worldgen.terrain.WorldLimits;

/**
 * The synthetic test city's content writers: a paved street with lamp posts, a grass park, and a boxy building of
 * stone-brick walls, glass windows, plank floors, glowstone lights, a door toward each street and cellars below. Only
 * vanilla blocks, so the fixture works in any pack. Writes only inside its chunk and never in soft-edge strips.
 *
 * <p>
 * Side: both (plain Java).
 */
final class SyntheticContent {

    static final BlockRef STREET = BlockRef.of("minecraft:double_stone_slab", 0);
    static final BlockRef WALL = BlockRef.of("minecraft:stonebrick", 0);
    static final BlockRef WINDOW = BlockRef.of("minecraft:glass_pane", 0);
    static final BlockRef FLOOR = BlockRef.of("minecraft:planks", 0);
    static final BlockRef LIGHT = BlockRef.of("minecraft:glowstone", 0);
    static final BlockRef POST = BlockRef.of("minecraft:cobblestone_wall", 0);
    static final BlockRef GRASS = BlockRef.of("minecraft:grass", 0);
    static final BlockRef DIRT = BlockRef.of("minecraft:dirt", 0);
    static final BlockRef ROOF = BlockRef.of("minecraft:stone_slab", 5);
    static final BlockRef TORCH = BlockRef.of("minecraft:torch", 5);
    static final BlockRef IRON_BARS = BlockRef.of("minecraft:iron_bars", 0);
    static final BlockRef CHEST = BlockRef.of("minecraft:chest", 2);

    /** Height of a lamp post below its light, in blocks. */
    static final int POST_HEIGHT = 3;
    /** A floor's torch sits this far above the floor. */
    static final int TORCH_HEIGHT = 3;

    private SyntheticContent() {}

    /** Returns the writer of a street chunk. */
    static ChunkContent street() {
        return SyntheticContent::writeStreet;
    }

    /** Returns the writer of a park chunk. */
    static ChunkContent park() {
        return SyntheticContent::writePark;
    }

    /** Returns the writer of a building chunk. */
    static ChunkContent building() {
        return SyntheticContent::writeBuilding;
    }

    private static void writeStreet(ChunkBuffer buffer, ContentContext context) {
        int ground = context.plan()
            .groundY();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (context.isSoftEdge(x, z)) {
                    continue;
                }
                buffer.set(x, ground, z, STREET);
                for (int y = ground + 1; y <= Math.min(WorldLimits.MAX_Y, ground + 4); y++) {
                    buffer.set(x, y, z, BlockRef.AIR);
                }
            }
        }
        lampPost(buffer, context, 2, 2, ground);
        lampPost(buffer, context, 13, 13, ground);
    }

    private static void lampPost(ChunkBuffer buffer, ContentContext context, int x, int z, int ground) {
        if (context.isSoftEdge(x, z) || ground + POST_HEIGHT + 1 > WorldLimits.MAX_Y) {
            return;
        }
        for (int i = 1; i <= POST_HEIGHT; i++) {
            buffer.set(x, ground + i, z, POST);
        }
        buffer.set(x, ground + POST_HEIGHT + 1, z, LIGHT);
    }

    private static void writePark(ChunkBuffer buffer, ContentContext context) {
        int ground = context.plan()
            .groundY();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (context.isSoftEdge(x, z)) {
                    continue;
                }
                buffer.set(x, ground, z, GRASS);
                buffer.set(x, ground - 1, z, DIRT);
            }
        }
    }

    private static void writeBuilding(ChunkBuffer buffer, ContentContext context) {
        ChunkPlan plan = context.plan();
        int ground = plan.groundY();
        int bottom = plan.buildingBottom();
        int top = plan.maxHeight();
        // Levels from the lowest cellar to the top floor, each FLOOR_HEIGHT tall.
        for (int level = bottom; level < top; level += WorldLimits.FLOOR_HEIGHT) {
            boolean aboveGround = level >= ground;
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    if (context.isSoftEdge(x, z)) {
                        continue;
                    }
                    buffer.set(x, level, z, isEdge(x, z) ? WALL : FLOOR);
                    for (int dy = 1; dy < WorldLimits.FLOOR_HEIGHT; dy++) {
                        int y = level + dy;
                        if (y > WorldLimits.MAX_Y) {
                            break;
                        }
                        buffer.set(x, y, z, isEdge(x, z) ? wallBlock(x, z, dy, aboveGround) : BlockRef.AIR);
                    }
                }
            }
            int lightY = level + WorldLimits.FLOOR_HEIGHT - 1;
            if (lightY <= WorldLimits.MAX_Y) {
                buffer.set(7, lightY, 7, LIGHT);
                buffer.set(8, lightY, 8, LIGHT);
            }
            if (aboveGround) {
                buffer.set(1, level + TORCH_HEIGHT, 7, TORCH);
            }
        }
        // Roof slab, then clear above it as upstream's clearToMax does before a roof.
        if (top <= WorldLimits.MAX_Y) {
            CityTerrain.clearToMax(buffer, plan, top + 1);
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    if (!context.isSoftEdge(x, z)) {
                        buffer.set(x, top, z, isEdge(x, z) ? WALL : ROOF);
                    }
                }
            }
        }
        doors(buffer, context, ground);
        // The plan's chest job fills the container written here (the population phase writes no blocks).
        if (!context.isSoftEdge(2, 2)) {
            buffer.set(2, ground + 1, 2, CHEST);
        }
    }

    /** A two-high door in the middle of each side that faces a street or park. */
    private static void doors(ChunkBuffer buffer, ContentContext context, int ground) {
        int[][] sides = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
        for (int[] side : sides) {
            ChunkPlan neighbor = context.neighbor(side[0], side[1]);
            if (!neighbor.isCity() || neighbor.hasBuilding() || neighbor.groundY() != ground) {
                continue;
            }
            for (int along = 7; along <= 8; along++) {
                int x = side[0] < 0 ? 0 : side[0] > 0 ? 15 : along;
                int z = side[1] < 0 ? 0 : side[1] > 0 ? 15 : along;
                if (context.isSoftEdge(x, z)) {
                    continue;
                }
                buffer.set(x, ground + 1, z, BlockRef.AIR);
                buffer.set(x, ground + 2, z, BlockRef.AIR);
            }
        }
    }

    private static BlockRef wallBlock(int x, int z, int dy, boolean aboveGround) {
        boolean corner = (x == 0 || x == 15) && (z == 0 || z == 15);
        boolean windowRow = dy == 2 || dy == 3;
        boolean windowColumn = ((x + z) % 3) != 0;
        return aboveGround && !corner && windowRow && windowColumn ? WINDOW : WALL;
    }

    private static boolean isEdge(int x, int z) {
        return x == 0 || x == 15 || z == 0 || z == 15;
    }
}
