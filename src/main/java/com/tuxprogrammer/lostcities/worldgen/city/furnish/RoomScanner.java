package com.tuxprogrammer.lostcities.worldgen.city.furnish;

import java.util.ArrayList;
import java.util.List;

import com.tuxprogrammer.lostcities.asset.BlockRef;

/**
 * Finds the rooms of one floor from the blocks already written (design record 0016, decision 3).
 *
 * <p>
 * A <em>room cell</em> is a column at the standing level {@code y} that is air at {@code y} and {@code y + 1} with a
 * floor block (not air, not liquid) at {@code y - 1}. Room cells are flood-filled 4-connected into rooms. A cell is
 * <em>kept clear</em> when it lies within one block (the 3 × 3 around it) of
 * <ul>
 * <li>a door, trapdoor, fence gate, stair, ladder or rail from {@code y - 2} to {@code y + 1}, so stairs going down
 * and ladders through the floor keep their landing;</li>
 * <li>a hole in the floor (air at {@code y} and {@code y - 1}), the top of a stairwell;</li>
 * <li>an open cell on the chunk's edge: a doorway, a passage or the open side of a building that spans chunks.</li>
 * </ul>
 * A <em>slot</em> is a room cell inside X and Z 1 to 14 that is not kept clear and has a wall beside it: a horizontal
 * neighbor that is neither air nor a passage block. Items go only on slots, against that wall.
 *
 * <p>
 * Pure; side: both (plain Java).
 */
public final class RoomScanner {

    /** Marks a column that is no room cell. */
    public static final int NO_ROOM = -1;

    private static final int[] DX = { 0, 0, -1, 1 };
    private static final int[] DZ = { -1, 1, 0, 0 };

    private RoomScanner() {}

    /** One floor's rooms. */
    public static final class Floor {

        private final int y;
        private final int[] room = new int[256];
        private final boolean[] clear = new boolean[256];
        private final int[] wall = new int[256];
        private int rooms;

        Floor(int y) {
            this.y = y;
        }

        /** Returns the standing level. */
        public int y() {
            return y;
        }

        /** Returns the number of rooms. */
        public int rooms() {
            return rooms;
        }

        /** Returns the room of a column ({@code (z << 4) | x}), or {@link #NO_ROOM}. */
        public int room(int cell) {
            return room[cell];
        }

        /** Returns whether a column is kept clear. */
        public boolean keptClear(int cell) {
            return clear[cell];
        }

        /**
         * Returns the side of the wall a slot stands against, as the facing index of the direction toward it, or -1
         * when the column is no slot.
         */
        public int wallSide(int cell) {
            return wall[cell];
        }

        /** Returns whether a column is a slot. */
        public boolean isSlot(int cell) {
            return wall[cell] >= 0;
        }

        /** Returns the columns of one room, in column order. */
        public List<Integer> cells(int id) {
            List<Integer> out = new ArrayList<>();
            for (int i = 0; i < 256; i++) {
                if (room[i] == id) {
                    out.add(i);
                }
            }
            return out;
        }
    }

    /**
     * Scans one floor.
     *
     * @param view the chunk
     * @param y    the standing level, one above the floor slab
     * @return the floor
     */
    public static Floor scan(BlockView view, int y) {
        Floor floor = new Floor(y);
        boolean[] cell = new boolean[256];
        boolean[] passage = new boolean[256];
        boolean[] hole = new boolean[256];
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int i = (z << 4) | x;
                // Each of the four levels is read once; reads are pure, so this is the same floor as reading them
                // per test, at four block reads a column instead of up to seven.
                BlockRef under = view.get(x, y - 2, z);
                BlockRef below = view.get(x, y - 1, z);
                BlockRef at = view.get(x, y, z);
                BlockRef above = view.get(x, y + 1, z);
                boolean open = at.isAir() && above.isAir();
                cell[i] = open && !below.isAir() && !isLiquid(below);
                hole[i] = open && below.isAir();
                passage[i] = isPassage(under) || isPassage(below) || isPassage(at) || isPassage(above);
                floor.room[i] = NO_ROOM;
                floor.wall[i] = -1;
            }
        }
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int i = (z << 4) | x;
                boolean edge = x == 0 || z == 0 || x == 15 || z == 15;
                if (passage[i] || hole[i] || edge && cell[i]) {
                    markAround(floor.clear, x, z);
                }
            }
        }
        for (int i = 0; i < 256; i++) {
            if (cell[i] && floor.room[i] == NO_ROOM) {
                flood(floor, cell, i, floor.rooms++);
            }
        }
        for (int z = 1; z < 15; z++) {
            for (int x = 1; x < 15; x++) {
                int i = (z << 4) | x;
                if (!cell[i] || floor.clear[i]) {
                    continue;
                }
                for (int side = 0; side < 4; side++) {
                    BlockRef next = view.get(x + DX[side], y, z + DZ[side]);
                    if (!next.isAir() && !isPassage(next) && !isLiquid(next)) {
                        floor.wall[i] = side;
                        break;
                    }
                }
            }
        }
        return floor;
    }

    /**
     * Returns whether the free columns of a room are all reachable from each other, 4-connected.
     *
     * @param free the room's free columns, as flags per column
     * @return whether they form one piece (an empty set counts as one)
     */
    public static boolean connected(boolean[] free) {
        int start = -1;
        int total = 0;
        for (int i = 0; i < 256; i++) {
            if (free[i]) {
                total++;
                if (start < 0) {
                    start = i;
                }
            }
        }
        if (start < 0) {
            return true;
        }
        boolean[] seen = new boolean[256];
        int[] stack = new int[256];
        int top = 0;
        stack[top++] = start;
        seen[start] = true;
        int reached = 0;
        while (top > 0) {
            int i = stack[--top];
            reached++;
            int x = i & 15;
            int z = i >> 4;
            for (int side = 0; side < 4; side++) {
                int nx = x + DX[side];
                int nz = z + DZ[side];
                if (nx < 0 || nz < 0 || nx > 15 || nz > 15) {
                    continue;
                }
                int n = (nz << 4) | nx;
                if (free[n] && !seen[n]) {
                    seen[n] = true;
                    stack[top++] = n;
                }
            }
        }
        return reached == total;
    }

    /**
     * Returns whether a block is one a player walks through or climbs: a door, trapdoor, fence gate, stair, ladder or
     * rail, by registry name.
     */
    public static boolean isPassage(BlockRef block) {
        if (block.isAir()) {
            return false;
        }
        // Matches the name after the mod ID in place: the scan calls this for most blocks of every floor, so it
        // allocates nothing.
        String name = block.name();
        int from = name.indexOf(':') + 1;
        return name.indexOf("door", from) >= 0 || name.indexOf("stairs", from) >= 0
            || name.indexOf("ladder", from) >= 0
            || name.indexOf("rail", from) >= 0
            || name.indexOf("gate", from) >= 0;
    }

    private static boolean isLiquid(BlockRef block) {
        String name = block.name();
        return name.equals("minecraft:water") || name.equals("minecraft:flowing_water")
            || name.equals("minecraft:lava")
            || name.equals("minecraft:flowing_lava");
    }

    private static void markAround(boolean[] clear, int x, int z) {
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                int nx = x + dx;
                int nz = z + dz;
                if (nx >= 0 && nz >= 0 && nx < 16 && nz < 16) {
                    clear[(nz << 4) | nx] = true;
                }
            }
        }
    }

    private static void flood(Floor floor, boolean[] cell, int start, int id) {
        int[] stack = new int[256];
        int top = 0;
        stack[top++] = start;
        floor.room[start] = id;
        while (top > 0) {
            int i = stack[--top];
            int x = i & 15;
            int z = i >> 4;
            for (int side = 0; side < 4; side++) {
                int nx = x + DX[side];
                int nz = z + DZ[side];
                if (nx < 0 || nz < 0 || nx > 15 || nz > 15) {
                    continue;
                }
                int n = (nz << 4) | nx;
                if (cell[n] && floor.room[n] == NO_ROOM) {
                    floor.room[n] = id;
                    stack[top++] = n;
                }
            }
        }
    }
}
