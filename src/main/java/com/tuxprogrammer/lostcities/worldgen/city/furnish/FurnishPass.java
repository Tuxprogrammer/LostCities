package com.tuxprogrammer.lostcities.worldgen.city.furnish;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.worldgen.terrain.SeedMix;

/**
 * Furnishes one building chunk's floors with a few items against the walls (design record 0016, D-p6b-1 to D-p6b-6).
 *
 * <p>
 * For each floor the pass scans the rooms ({@link RoomScanner}), takes the floor's {@link RoomKind} palette, and
 * fills a share of each room's wall-side slots in a seeded order. It keeps
 * <ul>
 * <li>its own randomness, seeded from the world seed, the chunk and the floor ({@link #floorSalt}), never the
 * writer's, so turning it on or off changes no other block;</li>
 * <li>at most {@link #TILES_PER_ROOM} tile entities per room and {@link #TILES_PER_CHUNK} per chunk, GT machines
 * included, whatever the palettes say: a tile item over a cap becomes the palette's plain fallback or nothing;</li>
 * <li>every room in one piece: an item whose cell would cut the room's free cells apart is dropped;</li>
 * <li>multiblock controllers two blocks or more from the chunk's edges.</li>
 * </ul>
 * GT machines are written as their block at the base-type metadata; the population phase gives them their
 * MetaTileEntity ({@link #machineTemplate}). Writes stay inside the chunk.
 *
 * <p>
 * Side: both (plain Java).
 */
public final class FurnishPass {

    /** The most tile entities the pass places in one chunk (research-gt-machines.md, recommended set). */
    public static final int TILES_PER_CHUNK = 6;
    /** The most tile entities the pass places in one room. */
    public static final int TILES_PER_ROOM = 2;

    /** Mixes the chunk into a floor's salt. */
    private static final long SALT_X = 8734625211L;
    private static final long SALT_Z = 5127349871L;
    /** Separates the pass's draws from the room-kind draw on the same salt. */
    private static final long PASS_STREAM = 0x5DEECE66DL;

    private FurnishPass() {}

    /** Where the pass writes. */
    @FunctionalInterface
    public interface BlockSink {

        /** Writes a block at chunk-local X and Z and block Y. */
        void set(int x, int y, int z, BlockRef block);
    }

    /** One placed item. */
    public static final class Placed {

        private final int x;
        private final int y;
        private final int z;
        private final int room;
        private final FurnishItem item;

        Placed(int x, int y, int z, int room, FurnishItem item) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.room = room;
            this.item = item;
        }

        /** Returns local X. */
        public int x() {
            return x;
        }

        /** Returns block Y. */
        public int y() {
            return y;
        }

        /** Returns local Z. */
        public int z() {
            return z;
        }

        /** Returns the room's index on its floor. */
        public int room() {
            return room;
        }

        /** Returns the item. */
        public FurnishItem item() {
            return item;
        }

        @Override
        public String toString() {
            return item + "@" + x + "," + y + "," + z;
        }
    }

    /**
     * Returns a floor's salt: what its room kind, its draws and its machines' picks are seeded from.
     *
     * @param worldSeed the world seed
     * @param chunkX    chunk X
     * @param chunkZ    chunk Z
     * @param floor     the floor, negative for cellars
     * @return the salt
     */
    public static long floorSalt(long worldSeed, int chunkX, int chunkZ, int floor) {
        return SeedMix.mix(SeedMix.chunk(worldSeed, chunkX, chunkZ, SALT_X, SALT_Z), floor);
    }

    /**
     * Furnishes the floors of one chunk.
     *
     * @param view     the chunk's blocks, read after the building and its overlays are written
     * @param sink     where items go
     * @param standing the standing level of each floor, one above its slab
     * @param kinds    each floor's room kind
     * @param salts    each floor's salt ({@link #floorSalt})
     * @param palettes the palettes
     * @param scale    density multiplier: 1, or 0.5 for a ruined building
     * @param known    whether a block registry name is registered
     * @return the items placed, in placement order
     */
    public static List<Placed> run(BlockView view, BlockSink sink, int[] standing, RoomKind[] kinds, long[] salts,
        FurnishPalettes palettes, float scale, Predicate<String> known) {
        List<Placed> out = new ArrayList<>();
        int chunkTiles = 0;
        for (int f = 0; f < standing.length; f++) {
            FurnishPalette palette = palettes.palette(kinds[f]);
            if (palette == null || palette.items()
                .isEmpty()) {
                continue;
            }
            Random random = new Random(salts[f] ^ PASS_STREAM);
            int y = standing[f];
            RoomScanner.Floor floor = RoomScanner.scan(view, y);
            for (int room = 0; room < floor.rooms(); room++) {
                chunkTiles += furnishRoom(
                    floor,
                    room,
                    palette,
                    random,
                    salts[f],
                    scale,
                    known,
                    TILES_PER_CHUNK - chunkTiles,
                    sink,
                    out);
            }
        }
        return out;
    }

    private static int furnishRoom(RoomScanner.Floor floor, int room, FurnishPalette palette, Random random, long salt,
        float scale, Predicate<String> known, int chunkTilesLeft, BlockSink sink, List<Placed> out) {
        List<Integer> cells = floor.cells(room);
        boolean[] free = new boolean[256];
        List<Integer> slots = new ArrayList<>();
        for (int cell : cells) {
            free[cell] = true;
            if (floor.isSlot(cell)) {
                slots.add(cell);
            }
        }
        if (slots.isEmpty()) {
            return 0;
        }
        Collections.shuffle(slots, random);
        float wanted = palette.density() * scale * slots.size();
        int count = (int) wanted;
        if (random.nextFloat() < wanted - count) {
            count++;
        }
        count = Math.min(count, palette.maxItems());
        int tilesLeft = Math.min(Math.min(palette.maxTiles(), TILES_PER_ROOM), chunkTilesLeft);
        int tiles = 0;
        int placed = 0;
        int y = floor.y();
        for (int cell : slots) {
            if (placed >= count) {
                break;
            }
            int x = cell & 15;
            int z = cell >> 4;
            FurnishItem item = palette.pick(random, known);
            if (item != null && item.isMachine()) {
                item = palette.machineAt(salt, x, y, z);
                if (item != null && item.controller() && (x < 2 || x > 13 || z < 2 || z > 13)) {
                    item = palette.fallback();
                }
            }
            if (item != null && item.tile() && tiles >= tilesLeft) {
                item = palette.fallback();
            }
            if (item == null || !known.test(item.block())) {
                continue;
            }
            free[cell] = false;
            if (!RoomScanner.connected(free)) {
                free[cell] = true;
                continue;
            }
            // The front points away from the wall the item stands against.
            int facing = floor.wallSide(cell) ^ 1;
            sink.set(x, y, z, item.blockFor(facing));
            out.add(new Placed(x, y, z, room, item));
            placed++;
            if (item.tile()) {
                tiles++;
            }
        }
        return tiles;
    }

    /**
     * Returns the facing index a GT machine's front takes: away from a wall with open air in front, else toward any
     * open side, else south. The population phase reads it from the finished chunk.
     *
     * @param view the chunk
     * @param x    local X
     * @param y    block Y
     * @param z    local Z
     * @return the facing index, {@link FurnishItem#NORTH} to {@link FurnishItem#EAST}
     */
    public static int machineFacing(BlockView view, int x, int y, int z) {
        int open = -1;
        for (int side = 0; side < 4; side++) {
            int front = side ^ 1;
            BlockRef back = neighbor(view, x, y, z, side);
            BlockRef ahead = neighbor(view, x, y, z, front);
            if (ahead != null && ahead.isAir()) {
                if (back != null && !back.isAir()) {
                    return front;
                }
                if (open < 0) {
                    open = front;
                }
            }
        }
        return open < 0 ? FurnishItem.SOUTH : open;
    }

    /**
     * Returns the tile-entity template of a GT machine: its MetaTileEntity ID and its front (ForgeDirection ordinal 2
     * to 5), the two keys {@code BaseMetaTileEntity.readFromNBT} needs (research-gt-machines.md §1).
     *
     * @param gtId   the MetaTileEntity ID
     * @param facing the facing index
     * @return the template as JSON text
     */
    public static String machineTemplate(int gtId, int facing) {
        return "{\"mID\":" + gtId + ",\"mFacing\":" + (facing + 2) + "}";
    }

    /**
     * Returns the population job's argument for a floor: its room kind and salt.
     *
     * @param kind the floor's room kind
     * @param salt the floor's salt
     * @return the argument
     */
    public static String jobArgument(RoomKind kind, long salt) {
        return kind.id() + ";" + salt;
    }

    /**
     * Lists what the population phase completes on one furnished floor: each of the palette's tile blocks gets its
     * tile entity (an empty template), and each GT machine block whose base type matches the machine its position
     * picks gets that machine's template. A block the damage or ruin pass removed is simply not found.
     *
     * @param view     the populated chunk
     * @param y        the floor's standing level
     * @param argument the job's argument ({@link #jobArgument})
     * @param palettes the palettes
     * @return one {@code {x, z}} position and template per tile entity, the template empty for a plain one
     */
    public static List<Completion> completions(BlockView view, int y, String argument, FurnishPalettes palettes) {
        List<Completion> out = new ArrayList<>();
        int split = argument.indexOf(';');
        RoomKind kind = split < 0 ? null : RoomKind.byId(argument.substring(0, split));
        FurnishPalette palette = kind == null ? null : palettes.palette(kind);
        if (palette == null) {
            return out;
        }
        long salt;
        try {
            salt = Long.parseLong(argument.substring(split + 1));
        } catch (NumberFormatException e) {
            return out;
        }
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                BlockRef block = view.get(x, y, z);
                if (block.isAir()) {
                    continue;
                }
                if (FurnishItem.GT_MACHINES.equals(block.name())) {
                    FurnishItem machine = palette.machineAt(salt, x, y, z);
                    if (machine != null && machine.meta() == block.meta()) {
                        out.add(
                            new Completion(
                                x,
                                z,
                                machine.gtId(),
                                machineTemplate(machine.gtId(), machineFacing(view, x, y, z))));
                    }
                } else if (palette.isTileBlock(block.name())) {
                    out.add(new Completion(x, z, 0, ""));
                }
            }
        }
        return out;
    }

    /** A tile entity the population phase completes. */
    public static final class Completion {

        private final int x;
        private final int z;
        private final int gtId;
        private final String template;

        Completion(int x, int z, int gtId, String template) {
            this.x = x;
            this.z = z;
            this.gtId = gtId;
            this.template = template;
        }

        /** Returns local X. */
        public int x() {
            return x;
        }

        /** Returns local Z. */
        public int z() {
            return z;
        }

        /** Returns the GT MetaTileEntity ID, or 0 for a plain tile entity. */
        public int gtId() {
            return gtId;
        }

        /** Returns the template as JSON text, empty for a plain tile entity. */
        public String template() {
            return template;
        }
    }

    private static BlockRef neighbor(BlockView view, int x, int y, int z, int side) {
        int nx = x + (side == FurnishItem.WEST ? -1 : side == FurnishItem.EAST ? 1 : 0);
        int nz = z + (side == FurnishItem.NORTH ? -1 : side == FurnishItem.SOUTH ? 1 : 0);
        if (nx < 0 || nz < 0 || nx > 15 || nz > 15) {
            return null;
        }
        return view.get(nx, y, nz);
    }
}
