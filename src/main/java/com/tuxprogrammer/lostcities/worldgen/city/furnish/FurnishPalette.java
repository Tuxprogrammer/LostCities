package com.tuxprogrammer.lostcities.worldgen.city.furnish;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.tuxprogrammer.lostcities.worldgen.terrain.SeedMix;

/**
 * One room kind's furniture: how much of the wall-side floor it covers and the weighted items it draws from (design
 * record 0016, decision 5). Read from {@code data/lostcitiesnh/furnish/<room>.json}:
 *
 * <pre>
 * { "room": "office", "density": 0.3, "maxItems": 6, "maxTiles": 1,
 *   "fallback": { "block": "minecraft:bookshelf" },
 *   "items": [ { "block": "chisel:bookshelf", "meta": 1, "weight": 4 },
 *              { "block": "minecraft:furnace", "tile": true, "facingMeta": [2, 3, 4, 5] },
 *              { "gt": 12, "baseMeta": 1, "weight": 1 } ] }
 * </pre>
 *
 * A {@code facingMeta} table gives the metadata for a front pointing north, south, west and east. GT IDs on
 * {@link #DENIED_MACHINES} are rejected whatever the file says. Immutable. Side: both (plain Java).
 */
public final class FurnishPalette {

    /**
     * GT MetaTileEntity IDs that worldgen never places (research-gt-machines.md §4): debug blocks, the pollution
     * creator, teleporter, tesseracts, repellators, world accelerators, lightning rods, miners, rock breakers, pumps,
     * pollution cleaners, the Thaumcraft researcher and ME hatches. Each pair is an inclusive range.
     */
    static final int[][] DENIED_MACHINES = { { 238, 238 }, { 349, 349 }, { 11304, 11306 }, { 951, 951 }, { 1145, 1145 },
        { 833, 834 }, { 1146, 1149 }, { 11100, 11103 }, { 1174, 1175 }, { 679, 681 }, { 441, 444 }, { 1140, 1142 },
        { 758, 761 }, { 956, 956 }, { 2710, 2718 } };

    /** Mixes a block position into a machine pick. */
    private static final long POS_X = 341873128712L;
    private static final long POS_Z = 132897987541L;
    private static final long POS_Y = 2654435761L;

    private final RoomKind room;
    private final float density;
    private final int maxItems;
    private final int maxTiles;
    private final FurnishItem fallback;
    private final List<FurnishItem> items;
    private final List<FurnishItem> machines;
    private final int totalWeight;
    private final int machineWeight;
    private final Set<String> tileBlocks;

    FurnishPalette(RoomKind room, float density, int maxItems, int maxTiles, FurnishItem fallback,
        List<FurnishItem> items) {
        this.room = room;
        this.density = density;
        this.maxItems = maxItems;
        this.maxTiles = maxTiles;
        this.fallback = fallback;
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
        List<FurnishItem> gt = new ArrayList<>();
        Set<String> tiles = new HashSet<>();
        int total = 0;
        int gtTotal = 0;
        for (FurnishItem item : items) {
            total += item.weight();
            if (item.isMachine()) {
                gt.add(item);
                gtTotal += item.weight();
            } else if (item.tile()) {
                tiles.add(item.block());
            }
        }
        this.machines = Collections.unmodifiableList(gt);
        this.totalWeight = total;
        this.machineWeight = gtTotal;
        this.tileBlocks = Collections.unmodifiableSet(tiles);
    }

    /** Returns whether a GT MetaTileEntity ID is on the deny list. */
    public static boolean denied(int gtId) {
        for (int[] range : DENIED_MACHINES) {
            if (gtId >= range[0] && gtId <= range[1]) {
                return true;
            }
        }
        return false;
    }

    /** Returns the room kind. */
    public RoomKind room() {
        return room;
    }

    /** Returns the share of eligible wall-side cells that get an item, 0 to 1. */
    public float density() {
        return density;
    }

    /** Returns the most items one room gets. */
    public int maxItems() {
        return maxItems;
    }

    /** Returns the most tile entities one room gets from this palette, never above the code's per-room cap. */
    public int maxTiles() {
        return maxTiles;
    }

    /** Returns the plain block used when a tile item is over a cap, or {@code null} to leave the cell empty. */
    public FurnishItem fallback() {
        return fallback;
    }

    /** Returns the items in file order. */
    public List<FurnishItem> items() {
        return items;
    }

    /** Returns whether a block name is one of this palette's tile-entity block items (not a GT machine). */
    public boolean isTileBlock(String name) {
        return tileBlocks.contains(name);
    }

    /**
     * Draws an item by weight, skipping items whose block the world does not know. A GT machine stands for the whole
     * GT share; which machine is fixed by its position ({@link #machineAt}).
     *
     * @param random the pass's random
     * @param known  whether a registry name is registered
     * @return the item, or {@code null} when the draw lands on an unknown block
     */
    public FurnishItem pick(Random random, Predicate<String> known) {
        if (totalWeight <= 0) {
            return null;
        }
        int r = random.nextInt(totalWeight);
        for (FurnishItem item : items) {
            r -= item.weight();
            if (r < 0) {
                return known.test(item.block()) ? item : null;
            }
        }
        return null;
    }

    /**
     * Returns the GT machine at a position: a pure function of the salt and the position, so the population phase
     * finds the same machine the terrain phase placed without any record of it.
     *
     * @param salt the floor's salt
     * @param x    local X
     * @param y    block Y
     * @param z    local Z
     * @return the machine, or {@code null} when the palette has none
     */
    public FurnishItem machineAt(long salt, int x, int y, int z) {
        if (machineWeight <= 0) {
            return null;
        }
        long seed = SeedMix.mix(salt, x * POS_X + z * POS_Z + y * POS_Y);
        int r = new Random(seed).nextInt(machineWeight);
        for (FurnishItem item : machines) {
            r -= item.weight();
            if (r < 0) {
                return item;
            }
        }
        return null;
    }

    /**
     * Reads a palette.
     *
     * @param room     the room kind the file is for
     * @param reader   the JSON
     * @param problems where whatever is wrong goes, one line each; bad items are dropped, the rest kept
     * @return the palette
     * @throws IllegalStateException when the text is not a JSON object
     */
    public static FurnishPalette read(RoomKind room, Reader reader, List<String> problems) {
        JsonElement root;
        try {
            root = new JsonParser().parse(reader);
        } catch (JsonParseException e) {
            throw new IllegalStateException("furnish palette " + room.id() + " is not JSON: " + e.getMessage(), e);
        }
        if (!root.isJsonObject()) {
            throw new IllegalStateException("furnish palette " + room.id() + " is not a JSON object");
        }
        JsonObject json = root.getAsJsonObject();
        String where = room.id();
        String named = string(json, "room");
        if (!room.id()
            .equals(named)) {
            problems.add(where + ": 'room' is " + named + ", expected " + room.id());
        }
        float density = (float) number(json, "density", 0.25);
        if (density < 0 || density > 1) {
            problems.add(where + ": 'density' " + density + " is outside 0 to 1");
            density = Math.max(0f, Math.min(1f, density));
        }
        int maxItems = Math.max(0, (int) number(json, "maxItems", 4));
        int maxTiles = (int) number(json, "maxTiles", 1);
        if (maxTiles < 0 || maxTiles > FurnishPass.TILES_PER_ROOM) {
            problems.add(where + ": 'maxTiles' " + maxTiles + " is outside 0 to " + FurnishPass.TILES_PER_ROOM);
            maxTiles = Math.max(0, Math.min(FurnishPass.TILES_PER_ROOM, maxTiles));
        }
        FurnishItem fallback = null;
        if (json.has("fallback")) {
            fallback = item(where + ".fallback", json.get("fallback"), problems);
            if (fallback != null && fallback.tile()) {
                problems.add(where + ".fallback: a fallback must be a plain block, not a tile entity");
                fallback = null;
            }
        }
        List<FurnishItem> items = new ArrayList<>();
        JsonElement list = json.get("items");
        if (list == null || !list.isJsonArray()) {
            problems.add(where + ": no 'items' list");
        } else {
            JsonArray array = list.getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                FurnishItem item = item(where + ".items[" + i + "]", array.get(i), problems);
                if (item != null) {
                    items.add(item);
                }
            }
        }
        return new FurnishPalette(room, density, maxItems, maxTiles, fallback, items);
    }

    private static FurnishItem item(String where, JsonElement element, List<String> problems) {
        if (!element.isJsonObject()) {
            problems.add(where + ": not an object");
            return null;
        }
        JsonObject json = element.getAsJsonObject();
        int weight = (int) number(json, "weight", 1);
        if (weight < 1) {
            problems.add(where + ": 'weight' must be at least 1");
            return null;
        }
        if (json.has("gt")) {
            int id = (int) number(json, "gt", 0);
            int baseMeta = (int) number(json, "baseMeta", -1);
            if (id <= 0) {
                problems.add(where + ": 'gt' must be a positive MetaTileEntity ID");
                return null;
            }
            if (denied(id)) {
                problems.add(where + ": GT machine " + id + " is on the deny list");
                return null;
            }
            // Base types 0-3 and 12-15 are BaseMetaTileEntity; 4-11 are pipes (research-gt-machines.md §1).
            if (baseMeta < 0 || baseMeta > 15 || baseMeta >= 4 && baseMeta <= 11) {
                problems.add(where + ": 'baseMeta' " + baseMeta + " is not a machine base type (0-3, 12-15)");
                return null;
            }
            boolean controller = json.has("controller") && json.get("controller")
                .getAsBoolean();
            return FurnishItem.machine(id, baseMeta, controller, weight);
        }
        String block = string(json, "block");
        if (block == null || block.indexOf(':') <= 0) {
            problems.add(where + ": needs 'block' as modid:name, or 'gt'");
            return null;
        }
        int meta = (int) number(json, "meta", 0);
        if (meta < 0 || meta > 15) {
            problems.add(where + ": 'meta' " + meta + " is outside 0 to 15");
            return null;
        }
        boolean tile = json.has("tile") && json.get("tile")
            .getAsBoolean();
        int[] facing = null;
        JsonElement table = json.get("facingMeta");
        if (table != null) {
            if (!table.isJsonArray() || table.getAsJsonArray()
                .size() != 4) {
                problems.add(where + ": 'facingMeta' needs four values (north, south, west, east)");
                return null;
            }
            facing = new int[4];
            for (int i = 0; i < 4; i++) {
                facing[i] = table.getAsJsonArray()
                    .get(i)
                    .getAsInt();
                if (facing[i] < 0 || facing[i] > 15) {
                    problems.add(where + ": 'facingMeta' value " + facing[i] + " is outside 0 to 15");
                    return null;
                }
            }
        }
        return FurnishItem.block(block, meta, tile, facing, weight);
    }

    private static String string(JsonObject json, String key) {
        JsonElement value = json.get(key);
        return value == null || !value.isJsonPrimitive() ? null : value.getAsString();
    }

    private static double number(JsonObject json, String key, double fallback) {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return value.getAsDouble();
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
