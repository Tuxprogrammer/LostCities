package com.tuxprogrammer.lostcities.worldgen.city.furnish;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.worldgen.terrain.SeedMix;

/**
 * Furnishes one building chunk's floors with a few items against the walls (design record 0016, D330 to D335).
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
        return run(view, sink, standing, kinds, salts, palettes, scale, known, MultiblockShapes.none());
    }

    /**
     * Furnishes the floors of one chunk, with partial multiblocks in the room kinds the shapes allow (decision 14).
     * A multiblock's controller counts against the tile caps, and a chunk holds at most one multiblock.
     *
     * @param view        the chunk's blocks, read after the building and its overlays are written
     * @param sink        where items go
     * @param standing    the standing level of each floor, one above its slab
     * @param kinds       each floor's room kind
     * @param salts       each floor's salt ({@link #floorSalt})
     * @param palettes    the palettes
     * @param scale       density multiplier: 1, or 0.5 for a ruined building
     * @param known       whether a block registry name is registered
     * @param multiblocks the partial multiblock shapes, or {@link MultiblockShapes#none()}
     * @return the items placed, in placement order
     */
    public static List<Placed> run(BlockView view, BlockSink sink, int[] standing, RoomKind[] kinds, long[] salts,
        FurnishPalettes palettes, float scale, Predicate<String> known, MultiblockShapes multiblocks) {
        List<Placed> out = new ArrayList<>();
        int chunkTiles = 0;
        boolean[] multiblockDone = new boolean[1];
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
                    out,
                    view,
                    multiblocks.allows(kinds[f]) ? multiblocks : null,
                    multiblockDone);
            }
        }
        return out;
    }

    /** Controller candidates tried per room before giving up: bounds the search to a few hundred box tests. */
    private static final int MULTIBLOCK_TRIES = 48;
    /** Separates the multiblock draws from every other stream. */
    private static final long MULTIBLOCK_STREAM = 0x41C64E6DL;

    /**
     * Tries to stand one partial multiblock in a room: a seeded order of controller cells, the shape at each fixed by
     * position ({@link MultiblockShapes#pick}), the first facing whose box fits. The box must not touch a kept-clear
     * cell and must leave the room in one piece. Uses its own random, so the room's item draws do not move.
     *
     * @param front receives the cell in front of the controller, which stays clear
     * @return whether one was placed (its controller is one tile entity)
     */
    private static boolean placeMultiblock(BlockView view, RoomScanner.Floor floor, int room, boolean[] free,
        List<Integer> cells, MultiblockShapes multiblocks, long salt, float scale, Predicate<String> known,
        BlockSink sink, List<Placed> out, int[] front) {
        Random random = new Random(SeedMix.mix(salt ^ MULTIBLOCK_STREAM, room));
        if (random.nextFloat() >= multiblocks.chance() * scale) {
            return false;
        }
        List<Integer> order = new ArrayList<>(cells);
        Collections.shuffle(order, random);
        boolean[] inRoom = free.clone();
        int y = floor.y();
        int tries = 0;
        for (int cell : order) {
            int x = cell & 15;
            int z = cell >> 4;
            if (x < 2 || x > 13 || z < 2 || z > 13) {
                continue;
            }
            if (tries++ >= MULTIBLOCK_TRIES) {
                break;
            }
            MultiblockShapes.Shape shape = multiblocks.pick(salt, x, y, z);
            if (shape == null || !shape.known(known)) {
                continue;
            }
            for (int facing = 0; facing < 4; facing++) {
                if (!shape.fits(view, free, x, z, y, facing) || touchesOtherAir(view, shape, inRoom, x, z, y, facing)) {
                    continue;
                }
                boolean[] before = free.clone();
                shape.claim(free, x, z, facing);
                if (!RoomScanner.connected(free)) {
                    System.arraycopy(before, 0, free, 0, 256);
                    continue;
                }
                shape.place(sink, known, salt, multiblocks.decay(), x, z, y, facing);
                front[0] = ((z + (facing < 2 ? (facing == 0 ? -1 : 1) : 0)) << 4)
                    | (x + (facing >= 2 ? (facing == 2 ? -1 : 1) : 0));
                int controllerY = y + shape.controllerLayer();
                out.add(
                    new Placed(
                        x,
                        controllerY,
                        z,
                        room,
                        FurnishItem.machine(shape.controllerId(), MultiblockShapes.CONTROLLER_META, true, 1)));
                return true;
            }
        }
        return false;
    }

    /** Whether a box has an open neighbor that is not a cell of its room: a door's clear ring, say. */
    private static boolean touchesOtherAir(BlockView view, MultiblockShapes.Shape shape, boolean[] inRoom, int cx,
        int cz, int y, int facing) {
        boolean[] box = new boolean[256];
        boolean[] probe = new boolean[256];
        java.util.Arrays.fill(probe, true);
        shape.claim(probe, cx, cz, facing);
        for (int i = 0; i < 256; i++) {
            box[i] = !probe[i];
        }
        for (int i = 0; i < 256; i++) {
            if (!box[i]) {
                continue;
            }
            int x = i & 15;
            int z = i >> 4;
            int[][] around = { { x - 1, z }, { x + 1, z }, { x, z - 1 }, { x, z + 1 } };
            for (int[] n : around) {
                if (n[0] < 0 || n[1] < 0 || n[0] > 15 || n[1] > 15) {
                    continue;
                }
                int ni = (n[1] << 4) | n[0];
                if (!box[ni] && !inRoom[ni]
                    && view.get(n[0], y, n[1])
                        .isAir()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int furnishRoom(RoomScanner.Floor floor, int room, FurnishPalette palette, Random random, long salt,
        float scale, Predicate<String> known, int chunkTilesLeft, BlockSink sink, List<Placed> out, BlockView view,
        MultiblockShapes multiblocks, boolean[] multiblockDone) {
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
        int tiles = 0;
        int[] front = { -1 };
        if (multiblocks != null && !multiblockDone[0]
            && Math.min(Math.min(palette.maxTiles(), TILES_PER_ROOM), chunkTilesLeft) >= 1
            && placeMultiblock(view, floor, room, free, cells, multiblocks, salt, scale, known, sink, out, front)) {
            multiblockDone[0] = true;
            tiles = 1;
            slots.removeIf(c -> !free[c] || c == front[0]);
        }
        Collections.shuffle(slots, random);
        float wanted = palette.density() * scale * slots.size();
        int count = (int) wanted;
        if (random.nextFloat() < wanted - count) {
            count++;
        }
        count = Math.min(count, palette.maxItems());
        int tilesLeft = Math.min(Math.min(palette.maxTiles(), TILES_PER_ROOM), chunkTilesLeft);
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
        return completions(view, y, argument, palettes, MultiblockShapes.bundled());
    }

    /**
     * As {@link #completions(BlockView, int, String, FurnishPalettes)}, with the multiblock shapes given. A GT machine
     * block at the floor's level or one above it is first tried as a partial multiblock's controller: the shape its
     * position picks, and a facing whose casings all match, name the controller and its facing.
     *
     * @param view        the populated chunk
     * @param y           the floor's standing level
     * @param argument    the job's argument
     * @param palettes    the palettes
     * @param multiblocks the multiblock shapes
     * @return the completions, each with its own Y
     */
    public static List<Completion> completions(BlockView view, int y, String argument, FurnishPalettes palettes,
        MultiblockShapes multiblocks) {
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
                if (multiblocks.allows(kind)) {
                    Completion controller = controllerAt(view, y, x, z, salt, multiblocks);
                    if (controller != null) {
                        out.add(controller);
                        continue;
                    }
                }
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
                                machineTemplate(machine.gtId(), machineFacing(view, x, y, z)),
                                null,
                                null));
                    }
                } else if (palette.isTileBlock(block.name())) {
                    String loot = FurnishPalette.isContainer(block.name()) ? palette.lootAt(salt, x, y, z) : null;
                    String mob = FurnishPalette.SPAWNER.equals(block.name()) ? palette.mobAt(salt, x, y, z) : null;
                    out.add(new Completion(x, z, 0, "", loot, mob));
                }
            }
        }
        return out;
    }

    /** Finds a partial multiblock's controller standing over column (x, z) of a floor, or {@code null}. */
    private static Completion controllerAt(BlockView view, int y, int x, int z, long salt,
        MultiblockShapes multiblocks) {
        MultiblockShapes.Shape shape = multiblocks.pick(salt, x, y, z);
        if (shape == null) {
            return null;
        }
        int controllerY = y + shape.controllerLayer();
        BlockRef block = view.get(x, controllerY, z);
        if (!FurnishItem.GT_MACHINES.equals(block.name()) || block.meta() != MultiblockShapes.CONTROLLER_META) {
            return null;
        }
        int facing = shape.facingAt(view, x, z, controllerY);
        if (facing < 0) {
            return null;
        }
        return new Completion(x, z, shape.controllerId(), machineTemplate(shape.controllerId(), facing), null, null)
            .at(controllerY);
    }

    /** A tile entity the population phase completes. */
    public static final class Completion {

        private final int x;
        private final int z;
        private final int gtId;
        private final String template;
        private final String loot;
        private final String mob;
        private int y = -1;

        Completion(int x, int z, int gtId, String template, String loot, String mob) {
            this.loot = loot;
            this.mob = mob;
            this.x = x;
            this.z = z;
            this.gtId = gtId;
            this.template = template;
        }

        /** Returns local X. */
        public int x() {
            return x;
        }

        Completion at(int level) {
            this.y = level;
            return this;
        }

        /** Returns the block Y of the tile entity, or -1 for the job's own floor level. */
        public int y() {
            return y;
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
        /** Returns the loot category to fill a chest with, or {@code null} for anything else. */
        public String loot() {
            return loot;
        }

        /** Returns the mob a spawner takes, or {@code null} for anything else. */
        public String mob() {
            return mob;
        }

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
