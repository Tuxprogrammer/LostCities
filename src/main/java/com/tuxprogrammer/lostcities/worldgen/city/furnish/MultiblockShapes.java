package com.tuxprogrammer.lostcities.worldgen.city.furnish;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.worldgen.terrain.SeedMix;

/**
 * Partial GregTech multiblocks as set dressing for industrial rooms (design record 0016, decision 14, D362 and
 * following). The shapes are data ({@code data/lostcitiesnh/furnish/multiblocks.json}), copied from the structure
 * definitions of GT5-Unofficial 5.09.54.265 (the source of each is cited in the file).
 *
 * <p>
 * A shape is never complete: its top layers ({@code omitTop}) are left out and a share of the remaining cells decays
 * away, so the controller never forms a working machine. Hatches and mufflers are drawn as plain casings, so the only
 * tile entity a shape adds is its controller. Which shape stands at a position, and which way it faces, is a pure
 * function of the floor salt and the position ({@link #pick}) and of the casings around the controller
 * ({@link Shape#facingAt}), so the population phase finds the controller again without any record.
 *
 * <p>
 * Immutable once built. Side: both (plain Java).
 */
public final class MultiblockShapes {

    /** The class-path file of the shapes. */
    public static final String FILE = "/data/lostcitiesnh/furnish/multiblocks.json";

    /** The metadata of a multiblock controller's tile-entity base type (research-gt-machines.md §1). */
    public static final int CONTROLLER_META = 2;

    /** The most layers a shape may place: a room is five blocks tall at least. */
    public static final int MAX_PLACED_LAYERS = 4;

    private static final long STREAM = 0x2545F4914F6CDD1DL;
    private static final long POS_X = 341873128712L;
    private static final long POS_Z = 132897987541L;
    private static final long POS_Y = 2654435761L;

    /** Front direction per facing index: north, south, west, east. */
    private static final int[] FRONT_DX = { 0, 0, -1, 1 };
    private static final int[] FRONT_DZ = { -1, 1, 0, 0 };

    private static volatile MultiblockShapes bundled;

    private final List<Shape> shapes;
    private final float chance;
    private final float decay;
    private final Set<RoomKind> rooms;
    private final List<String> problems;

    MultiblockShapes(List<Shape> shapes, float chance, float decay, Set<RoomKind> rooms, List<String> problems) {
        this.shapes = Collections.unmodifiableList(new ArrayList<>(shapes));
        this.chance = chance;
        this.decay = decay;
        this.rooms = Collections.unmodifiableSet(new HashSet<>(rooms));
        this.problems = Collections.unmodifiableList(new ArrayList<>(problems));
    }

    /** Returns no shapes: the pass places no multiblocks. */
    public static MultiblockShapes none() {
        return new MultiblockShapes(
            Collections.<Shape>emptyList(),
            0f,
            0f,
            Collections.<RoomKind>emptySet(),
            Collections.<String>emptyList());
    }

    /** Returns the bundled shapes, read on first use. */
    public static MultiblockShapes bundled() {
        MultiblockShapes out = bundled;
        if (out == null) {
            synchronized (MultiblockShapes.class) {
                out = bundled;
                if (out == null) {
                    out = readBundled();
                    bundled = out;
                }
            }
        }
        return out;
    }

    private static MultiblockShapes readBundled() {
        List<String> problems = new ArrayList<>();
        try (InputStream in = MultiblockShapes.class.getResourceAsStream(FILE)) {
            if (in == null) {
                problems.add(FILE + " is not in the jar");
                return new MultiblockShapes(
                    Collections.<Shape>emptyList(),
                    0f,
                    0f,
                    Collections.<RoomKind>emptySet(),
                    problems);
            }
            return read(new InputStreamReader(in, StandardCharsets.UTF_8), problems);
        } catch (IOException | IllegalStateException e) {
            problems.add(FILE + ": " + e.getMessage());
            return new MultiblockShapes(
                Collections.<Shape>emptyList(),
                0f,
                0f,
                Collections.<RoomKind>emptySet(),
                problems);
        }
    }

    /**
     * Reads the shapes. A bad shape is dropped and reported; the rest are kept.
     *
     * @param reader   the JSON
     * @param problems where whatever is wrong goes, one line each
     * @return the shapes
     * @throws IllegalStateException when the text is not a JSON object
     */
    public static MultiblockShapes read(java.io.Reader reader, List<String> problems) {
        JsonElement root;
        try {
            root = new JsonParser().parse(reader);
        } catch (JsonParseException e) {
            throw new IllegalStateException("multiblocks is not JSON: " + e.getMessage(), e);
        }
        if (!root.isJsonObject()) {
            throw new IllegalStateException("multiblocks is not a JSON object");
        }
        JsonObject json = root.getAsJsonObject();
        float chance = (float) number(json, "chance", 0.3);
        float decay = (float) number(json, "decay", 0.1);
        if (chance < 0 || chance > 1 || decay < 0 || decay > 0.5) {
            problems.add("multiblocks: 'chance' must be 0 to 1 and 'decay' 0 to 0.5");
            chance = Math.max(0f, Math.min(1f, chance));
            decay = Math.max(0f, Math.min(0.5f, decay));
        }
        Set<RoomKind> rooms = new HashSet<>();
        if (json.has("rooms") && json.get("rooms")
            .isJsonArray()) {
            for (JsonElement e : json.getAsJsonArray("rooms")) {
                RoomKind kind = RoomKind.byId(e.getAsString());
                if (kind == null) {
                    problems.add("multiblocks.rooms: unknown room kind " + e.getAsString());
                } else {
                    rooms.add(kind);
                }
            }
        }
        List<Shape> shapes = new ArrayList<>();
        JsonElement list = json.get("shapes");
        if (list == null || !list.isJsonArray()) {
            problems.add("multiblocks: no 'shapes' list");
        } else {
            JsonArray array = list.getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                try {
                    shapes.add(Shape.parse(array.get(i), "multiblocks.shapes[" + i + "]"));
                } catch (IllegalArgumentException | IllegalStateException e) {
                    problems.add(e.getMessage());
                }
            }
        }
        return new MultiblockShapes(shapes, chance, decay, rooms, problems);
    }

    private static double number(JsonObject json, String key, double fallback) {
        return json.has(key) && json.get(key)
            .isJsonPrimitive() ? json.get(key)
                .getAsDouble() : fallback;
    }

    /** Returns the shapes. */
    public List<Shape> shapes() {
        return shapes;
    }

    /** Returns the chance that an industrial room tries to place one multiblock. */
    public float chance() {
        return chance;
    }

    /** Returns the share of non-anchor cells that decay away. */
    public float decay() {
        return decay;
    }

    /** Returns whether rooms of a kind may hold a multiblock. */
    public boolean allows(RoomKind kind) {
        return !shapes.isEmpty() && rooms.contains(kind);
    }

    /** Returns one line per problem in the file; empty when it is well formed. */
    public List<String> problems() {
        return problems;
    }

    /**
     * Returns the shape at a position, a pure function of the floor salt and the position. The population phase calls
     * this with the same arguments as the terrain phase did.
     *
     * @param salt the floor's salt
     * @param x    local X of the controller
     * @param y    block Y of the floor's standing level (not the controller's own Y)
     * @param z    local Z of the controller
     * @return the shape, or {@code null} when there are none
     */
    public Shape pick(long salt, int x, int y, int z) {
        if (shapes.isEmpty()) {
            return null;
        }
        long seed = SeedMix.mix(salt ^ STREAM, x * POS_X + z * POS_Z + y * POS_Y);
        return shapes.get(new Random(seed).nextInt(shapes.size()));
    }

    /** Returns the set of every block name the shapes draw, for the registry check. */
    public Set<String> blockNames() {
        Set<String> out = new HashSet<>();
        for (Shape shape : shapes) {
            for (Cell cell : shape.cells()) {
                out.add(cell.block.name());
            }
        }
        return out;
    }

    /** One placed cell of a shape, relative to the controller. */
    public static final class Cell {

        private final int layer;
        private final int row;
        private final int col;
        private final BlockRef block;

        Cell(int layer, int row, int col, BlockRef block) {
            this.layer = layer;
            this.row = row;
            this.col = col;
            this.block = block;
        }

        /** Returns the layer from the bottom, 0 up. */
        public int layer() {
            return layer;
        }

        /** Returns the row, 0 at the front (the controller's row), counting backwards. */
        public int row() {
            return row;
        }

        /** Returns the column, counted from the structure's left edge. */
        public int col() {
            return col;
        }

        /** Returns the block. */
        public BlockRef block() {
            return block;
        }
    }

    /** One multiblock shape. Immutable. */
    public static final class Shape {

        private final String id;
        private final int controllerId;
        private final int width;
        private final int depth;
        private final int layers;
        private final int placedLayers;
        private final int controllerLayer;
        private final int controllerCol;
        private final int fullCells;
        private final List<Cell> cells;
        private final List<Cell> anchors;

        private Shape(String id, int controllerId, int width, int depth, int layers, int placedLayers,
            int controllerLayer, int controllerCol, int fullCells, List<Cell> cells, List<Cell> anchors) {
            this.id = id;
            this.controllerId = controllerId;
            this.width = width;
            this.depth = depth;
            this.layers = layers;
            this.placedLayers = placedLayers;
            this.controllerLayer = controllerLayer;
            this.controllerCol = controllerCol;
            this.fullCells = fullCells;
            this.cells = Collections.unmodifiableList(cells);
            this.anchors = Collections.unmodifiableList(anchors);
        }

        static Shape parse(JsonElement element, String where) {
            if (!element.isJsonObject()) {
                throw new IllegalStateException(where + ": not an object");
            }
            JsonObject json = element.getAsJsonObject();
            String id = json.has("id") ? json.get("id")
                .getAsString() : where;
            where = where + " (" + id + ")";
            int controllerId = (int) number(json, "controller", 0);
            if (controllerId <= 0 || FurnishPalette.denied(controllerId)) {
                throw new IllegalStateException(where + ": controller id " + controllerId + " is missing or denied");
            }
            if (!json.has("source") || json.get("source")
                .getAsString()
                .isEmpty()) {
                throw new IllegalStateException(where + ": no 'source' citation");
            }
            Map<Character, BlockRef> legend = new LinkedHashMap<>();
            JsonObject leg = json.has("legend") && json.get("legend")
                .isJsonObject() ? json.getAsJsonObject("legend") : new JsonObject();
            for (Map.Entry<String, JsonElement> e : leg.entrySet()) {
                JsonObject b = e.getValue()
                    .getAsJsonObject();
                int meta = (int) number(b, "meta", 0);
                if (e.getKey()
                    .length() != 1 || meta < 0
                    || meta > BlockRef.MAX_META) {
                    throw new IllegalStateException(where + ": bad legend entry " + e.getKey());
                }
                legend.put(
                    e.getKey()
                        .charAt(0),
                    BlockRef.of(
                        b.get("block")
                            .getAsString(),
                        meta));
            }
            JsonArray layerArray = json.getAsJsonArray("layers");
            int layers = layerArray.size();
            int omitTop = (int) number(json, "omitTop", 0);
            int placed = layers - omitTop;
            if (omitTop < 1 || placed < 1 || placed > MAX_PLACED_LAYERS) {
                throw new IllegalStateException(
                    where + ": 'omitTop' " + omitTop + " must leave 1 to " + MAX_PLACED_LAYERS + " of " + layers);
            }
            int depth = 0;
            int width = 0;
            int cl = -1;
            int cr = -1;
            int cc = -1;
            int full = 0;
            List<Cell> cells = new ArrayList<>();
            for (int top = 0; top < layers; top++) {
                int layer = layers - 1 - top;
                JsonArray rows = layerArray.get(top)
                    .getAsJsonArray();
                depth = rows.size();
                for (int r = 0; r < rows.size(); r++) {
                    String row = rows.get(r)
                        .getAsString();
                    width = row.length();
                    for (int c = 0; c < row.length(); c++) {
                        char ch = row.charAt(c);
                        if (ch == '-') {
                            continue;
                        }
                        if (ch == '~') {
                            if (cl >= 0 || r != 0) {
                                throw new IllegalStateException(where + ": one controller '~' in the front row only");
                            }
                            cl = layer;
                            cr = r;
                            cc = c;
                            continue;
                        }
                        BlockRef block = legend.get(ch);
                        if (block == null) {
                            throw new IllegalStateException(where + ": '" + ch + "' is not in the legend");
                        }
                        full++;
                        if (layer < placed) {
                            cells.add(new Cell(layer, r, c, block));
                        }
                    }
                }
            }
            if (cl < 0 || cl >= placed) {
                throw new IllegalStateException(where + ": the controller must sit in a placed layer");
            }
            if (cells.size() >= full) {
                throw new IllegalStateException(where + ": the shape is complete");
            }
            List<Cell> anchors = new ArrayList<>();
            for (Cell cell : cells) {
                if (cell.layer == cl && cell.row <= 1) {
                    anchors.add(cell);
                }
            }
            if (anchors.isEmpty()) {
                throw new IllegalStateException(where + ": no anchor cells beside the controller");
            }
            return new Shape(id, controllerId, width, depth, layers, placed, cl, cc, full, cells, anchors);
        }

        /** Returns the shape's id. */
        public String id() {
            return id;
        }

        /** Returns the controller's MetaTileEntity id. */
        public int controllerId() {
            return controllerId;
        }

        /** Returns the width across the front. */
        public int width() {
            return width;
        }

        /** Returns the depth behind the front. */
        public int depth() {
            return depth;
        }

        /** Returns the layer count of the working machine. */
        public int layers() {
            return layers;
        }

        /** Returns the number of layers the shape places. */
        public int placedLayers() {
            return placedLayers;
        }

        /** Returns the layer, from the bottom, the controller stands in. */
        public int controllerLayer() {
            return controllerLayer;
        }

        /** Returns the block count of the complete machine, controller excluded. */
        public int fullCells() {
            return fullCells;
        }

        /** Returns the cells the shape places before decay. */
        public List<Cell> cells() {
            return cells;
        }

        private static int latX(int facing) {
            return facing < 2 ? 1 : 0;
        }

        private static int latZ(int facing) {
            return facing < 2 ? 0 : 1;
        }

        private int worldX(int cx, int facing, int row, int col) {
            return cx - FRONT_DX[facing] * row + latX(facing) * (col - controllerCol);
        }

        private int worldZ(int cz, int facing, int row, int col) {
            return cz - FRONT_DZ[facing] * row + latZ(facing) * (col - controllerCol);
        }

        /**
         * Returns whether the footprint fits the chunk and the room: every column of the box inside the chunk, the
         * controller two or more blocks from the chunk's edges, every column a free room cell, the cell in front of
         * the controller free, and air through the placed layers.
         *
         * @param view   the written chunk
         * @param free   the room's free cells, indexed {@code z * 16 + x}
         * @param cx     local X of the controller
         * @param cz     local Z of the controller
         * @param baseY  the floor's standing level (layer 0)
         * @param facing the way the controller faces, 0 north, 1 south, 2 west, 3 east
         * @return whether it fits
         */
        public boolean fits(BlockView view, boolean[] free, int cx, int cz, int baseY, int facing) {
            if (cx < 2 || cx > 13 || cz < 2 || cz > 13) {
                return false;
            }
            int fx = cx + FRONT_DX[facing];
            int fz = cz + FRONT_DZ[facing];
            if (fx < 0 || fz < 0 || fx > 15 || fz > 15 || !free[(fz << 4) | fx]) {
                return false;
            }
            for (int r = 0; r < depth; r++) {
                for (int c = 0; c < width; c++) {
                    int x = worldX(cx, facing, r, c);
                    int z = worldZ(cz, facing, r, c);
                    if (x < 0 || z < 0 || x > 15 || z > 15 || !free[(z << 4) | x]) {
                        return false;
                    }
                    for (int l = 0; l < placedLayers; l++) {
                        if (!view.get(x, baseY + l, z)
                            .isAir()) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }

        /**
         * Marks the footprint's columns as taken.
         *
         * @param free   the room's free cells
         * @param cx     local X of the controller
         * @param cz     local Z of the controller
         * @param facing the facing
         */
        public void claim(boolean[] free, int cx, int cz, int facing) {
            for (int r = 0; r < depth; r++) {
                for (int c = 0; c < width; c++) {
                    free[(worldZ(cz, facing, r, c) << 4) | worldX(cx, facing, r, c)] = false;
                }
            }
        }

        /**
         * Writes the shape: its casings (less the decayed ones) and the controller's block.
         *
         * @param sink   where blocks go
         * @param known  whether a registry name is registered
         * @param salt   the floor's salt, which seeds the decay
         * @param decay  the share of non-anchor cells that decay
         * @param cx     local X of the controller
         * @param cz     local Z of the controller
         * @param baseY  the floor's standing level
         * @param facing the facing
         * @return the number of casing blocks written
         */
        public int place(FurnishPass.BlockSink sink, Predicate<String> known, long salt, float decay, int cx, int cz,
            int baseY, int facing) {
            int written = 0;
            for (Cell cell : cells) {
                if (!known.test(cell.block.name())) {
                    continue;
                }
                int x = worldX(cx, facing, cell.row, cell.col);
                int z = worldZ(cz, facing, cell.row, cell.col);
                int y = baseY + cell.layer;
                if (!anchors.contains(cell) && decays(salt, decay, x, y, z)) {
                    continue;
                }
                sink.set(x, y, z, cell.block);
                written++;
            }
            sink.set(cx, baseY + controllerLayer, cz, BlockRef.of(FurnishItem.GT_MACHINES, CONTROLLER_META));
            return written;
        }

        /** Returns whether the shape can be drawn: every block of it is registered. */
        public boolean known(Predicate<String> known) {
            if (!known.test(FurnishItem.GT_MACHINES)) {
                return false;
            }
            for (Cell cell : cells) {
                if (!known.test(cell.block.name())) {
                    return false;
                }
            }
            return true;
        }

        private static boolean decays(long salt, float decay, int x, int y, int z) {
            if (decay <= 0f) {
                return false;
            }
            long seed = SeedMix.mix(salt ^ (STREAM + 1), x * POS_X + z * POS_Z + y * POS_Y);
            return new Random(seed).nextFloat() < decay;
        }

        /**
         * Finds the way a controller at a position faces, from the casings around it: the one facing whose anchor
         * cells all match. Returns -1 when none does (the casings were damaged away, or this is not a multiblock).
         *
         * @param view the chunk
         * @param cx   local X of the controller
         * @param cz   local Z of the controller
         * @param y    the controller's block Y
         * @return the facing 0 to 3, or -1
         */
        public int facingAt(BlockView view, int cx, int cz, int y) {
            for (int facing = 0; facing < 4; facing++) {
                boolean all = true;
                for (Cell anchor : anchors) {
                    int x = worldX(cx, facing, anchor.row, anchor.col);
                    int z = worldZ(cz, facing, anchor.row, anchor.col);
                    if (x < 0 || z < 0
                        || x > 15
                        || z > 15
                        || !view.get(x, y, z)
                            .equals(anchor.block)) {
                        all = false;
                        break;
                    }
                }
                if (all) {
                    return facing;
                }
            }
            return -1;
        }
    }
}
