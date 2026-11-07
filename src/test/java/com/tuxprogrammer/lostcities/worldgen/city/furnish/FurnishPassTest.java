package com.tuxprogrammer.lostcities.worldgen.city.furnish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.worldgen.buffer.ArrayChunkBuffer;

/** The furnish pass on hand-built floors (design record 0016, D330 to D335). */
public class FurnishPassTest {

    private static final int FLOOR = 64;
    private static final int Y = FLOOR + 1;
    private static final BlockRef STONE = BlockRef.of("minecraft:stonebrick", 0);
    private static final BlockRef DOOR = BlockRef.of("minecraft:wooden_door", 0);
    private static final BlockRef STAIRS = BlockRef.of("minecraft:stone_brick_stairs", 0);
    private static final BlockRef LADDER = BlockRef.of("minecraft:ladder", 2);

    private static final int PITCH = 5;

    /** Five walled 16 x 16 storeys with a door, a stair and a ladder; {@code inner} adds a wall at X 8. */
    private static ArrayChunkBuffer room(boolean inner) {
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        for (int s = 0; s < 5; s++) {
            storey(buffer, inner, s * PITCH);
        }
        return buffer;
    }

    private static void storey(ArrayChunkBuffer buffer, boolean inner, int up) {
        int slab = FLOOR + up;
        int stand = Y + up;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                buffer.set(x, slab, z, STONE);
                boolean edge = x == 0 || z == 0 || x == 15 || z == 15;
                for (int y = stand; y < stand + 4; y++) {
                    buffer.set(x, y, z, edge || inner && x == 8 && z != 4 ? STONE : BlockRef.AIR);
                }
            }
        }
        buffer.set(0, stand, 7, DOOR);
        buffer.set(0, stand + 1, 7, DOOR);
        buffer.set(5, stand, 12, STAIRS);
        buffer.set(14, stand, 3, LADDER);
        buffer.set(14, stand + 1, 3, LADDER);
    }

    /** A palette of dense tile items, to press against the caps. */
    private static FurnishPalettes dense() {
        Map<RoomKind, FurnishPalette> map = new EnumMap<>(RoomKind.class);
        for (RoomKind kind : RoomKind.values()) {
            map.put(
                kind,
                new FurnishPalette(
                    kind,
                    1f,
                    100,
                    FurnishPass.TILES_PER_ROOM,
                    FurnishItem.block("minecraft:bookshelf", 0, false, null, 1),
                    Arrays.asList(
                        FurnishItem.block("minecraft:furnace", 0, true, new int[] { 2, 3, 4, 5 }, 3),
                        FurnishItem.machine(12, 1, false, 3),
                        FurnishItem.block("minecraft:bookshelf", 0, false, null, 1))));
        }
        return new FurnishPalettes(map, new ArrayList<>());
    }

    private static List<FurnishPass.Placed> furnish(ArrayChunkBuffer buffer, long salt, int floors) {
        int[] standing = new int[floors];
        RoomKind[] kinds = new RoomKind[floors];
        long[] salts = new long[floors];
        for (int i = 0; i < floors; i++) {
            standing[i] = Y + i * PITCH;
            kinds[i] = RoomKind.INDUSTRIAL;
            salts[i] = salt + i;
        }
        return FurnishPass.run(buffer::get, buffer::set, standing, kinds, salts, dense(), 1f, name -> true);
    }

    private static boolean tile(FurnishItem item) {
        return item.tile();
    }

    @Test
    public void theBundledPalettesParseWithoutProblems() {
        FurnishPalettes palettes = FurnishPalettes.bundled();
        assertEquals(new ArrayList<String>(), palettes.problems());
        for (RoomKind kind : RoomKind.values()) {
            FurnishPalette palette = palettes.palette(kind);
            assertNotNull(palette, kind.id());
            assertFalse(
                palette.items()
                    .isEmpty(),
                kind.id());
            for (FurnishItem item : palette.items()) {
                assertFalse(item.isMachine() && FurnishPalette.denied(item.gtId()), item.toString());
            }
        }
    }

    @Test
    public void everyRoomKindCarriesAModestShareOfGtMachinesAndBasementsDoNotDominate() {
        FurnishPalettes palettes = FurnishPalettes.bundled();
        for (RoomKind kind : RoomKind.values()) {
            int gt = 0;
            int total = 0;
            for (FurnishItem item : palettes.palette(kind)
                .items()) {
                total += item.weight();
                gt += item.isMachine() ? item.weight() : 0;
            }
            assertTrue(gt > 0, kind.id() + " has no GT machine");
            assertTrue(
                kind == RoomKind.INDUSTRIAL || gt * 100 <= total * 40,
                kind.id() + " is more than 40% GT machines");
        }
    }

    @Test
    public void aDeniedOrMalformedMachineIsRejected() {
        List<String> problems = new ArrayList<>();
        FurnishPalette palette = FurnishPalette.read(
            RoomKind.INDUSTRIAL,
            new StringReader(
                "{\"room\":\"industrial\",\"items\":[{\"gt\":951,\"baseMeta\":1},{\"gt\":2712,\"baseMeta\":1},"
                    + "{\"gt\":12,\"baseMeta\":5},{\"gt\":12,\"baseMeta\":1}]}"),
            problems);
        assertEquals(3, problems.size(), problems.toString());
        assertEquals(
            1,
            palette.items()
                .size());
        assertTrue(FurnishPalette.denied(11101));
        assertFalse(FurnishPalette.denied(12));
    }

    @Test
    public void doorsStairsAndLaddersKeepAClearRing() {
        ArrayChunkBuffer buffer = room(false);
        List<FurnishPass.Placed> placed = furnish(buffer, 7L, 1);
        assertFalse(placed.isEmpty());
        int[][] keep = { { 0, 7 }, { 5, 12 }, { 14, 3 } };
        for (FurnishPass.Placed item : placed) {
            for (int[] k : keep) {
                assertTrue(
                    Math.abs(item.x() - k[0]) > 1 || Math.abs(item.z() - k[1]) > 1,
                    item + " stands next to the passage at " + k[0] + "," + k[1]);
            }
            assertTrue(item.x() >= 1 && item.x() <= 14 && item.z() >= 1 && item.z() <= 14, item.toString());
        }
    }

    @Test
    public void aRoomStaysInOnePieceAfterFurnishing() {
        for (long salt = 0; salt < 20; salt++) {
            ArrayChunkBuffer buffer = room(true);
            furnish(buffer, salt, 1);
            RoomScanner.Floor floor = RoomScanner.scan(buffer::get, Y);
            boolean[] free = new boolean[256];
            for (int i = 0; i < 256; i++) {
                free[i] = floor.room(i) != RoomScanner.NO_ROOM;
            }
            assertTrue(RoomScanner.connected(free), "salt " + salt);
            assertEquals(1, floor.rooms(), "salt " + salt);
        }
    }

    @Test
    public void tileEntitiesStayWithinTheRoomAndChunkCaps() {
        // Two rooms on each of five floors: the chunk cap, not the rooms', must bind.
        ArrayChunkBuffer buffer = room(true);
        List<FurnishPass.Placed> placed = furnish(buffer, 3L, 5);
        Map<String, Integer> perRoom = new HashMap<>();
        int total = 0;
        for (FurnishPass.Placed item : placed) {
            if (tile(item.item())) {
                total++;
                perRoom.merge(item.y() + "/" + item.room(), 1, Integer::sum);
            }
        }
        assertTrue(total <= FurnishPass.TILES_PER_CHUNK, "tiles " + total);
        for (int count : perRoom.values()) {
            assertTrue(count <= FurnishPass.TILES_PER_ROOM, perRoom.toString());
        }
    }

    @Test
    public void theSameSeedAndChunkFurnishTheSame() {
        List<FurnishPass.Placed> a = furnish(room(true), 11L, 1);
        List<FurnishPass.Placed> b = furnish(room(true), 11L, 1);
        assertEquals(a.toString(), b.toString());
        assertFalse(
            a.toString()
                .equals(furnish(room(true), 12L, 1).toString()));
    }

    @Test
    public void thePopulationPhaseFindsTheMachinesThePassPlaced() {
        ArrayChunkBuffer buffer = room(false);
        long salt = 5L;
        List<FurnishPass.Placed> placed = furnish(buffer, salt, 1);
        int machines = 0;
        int tiles = 0;
        for (FurnishPass.Placed item : placed) {
            machines += item.item()
                .isMachine() ? 1 : 0;
            tiles += item.item()
                .tile() ? 1 : 0;
        }
        List<FurnishPass.Completion> done = FurnishPass
            .completions(buffer::get, Y, FurnishPass.jobArgument(RoomKind.INDUSTRIAL, salt), dense());
        int found = 0;
        for (FurnishPass.Completion completion : done) {
            if (completion.gtId() > 0) {
                found++;
                assertTrue(
                    completion.template()
                        .matches("\\{\"mID\":12,\"mFacing\":[2-5]\\}"),
                    completion.template());
            }
        }
        assertEquals(machines, found);
        assertTrue(done.size() <= tiles, done.size() + " > " + tiles);
    }

    @Test
    public void aFurnishedBarrelCarriesALootCategoryAsAChestDoes() {
        Map<RoomKind, FurnishPalette> map = new EnumMap<>(RoomKind.class);
        Map<String, Integer> loot = new HashMap<>();
        loot.put("lostcitiesnh:city_supplies", 1);
        for (RoomKind kind : RoomKind.values()) {
            map.put(
                kind,
                new FurnishPalette(
                    kind,
                    1f,
                    100,
                    FurnishPass.TILES_PER_ROOM,
                    FurnishItem.block("minecraft:bookshelf", 0, false, null, 1),
                    Arrays.asList(FurnishItem.block("etfuturum:barrel", 0, true, new int[] { 2, 3, 4, 5 }, 1)),
                    loot,
                    new HashMap<>()));
        }
        FurnishPalettes palettes = new FurnishPalettes(map, new ArrayList<>());
        ArrayChunkBuffer buffer = room(false);
        long salt = 7L;
        FurnishPass.run(
            buffer::get,
            buffer::set,
            new int[] { Y },
            new RoomKind[] { RoomKind.INDUSTRIAL },
            new long[] { salt },
            palettes,
            1f,
            name -> true);
        List<FurnishPass.Completion> done = FurnishPass
            .completions(buffer::get, Y, FurnishPass.jobArgument(RoomKind.INDUSTRIAL, salt), palettes);
        assertFalse(done.isEmpty());
        for (FurnishPass.Completion completion : done) {
            assertEquals("lostcitiesnh:city_supplies", completion.loot());
        }
        assertTrue(FurnishPalette.isContainer("etfuturum:barrel"));
        assertTrue(FurnishPalette.isContainer("minecraft:chest"));
        assertFalse(FurnishPalette.isContainer("minecraft:furnace"));
    }

    @Test
    public void aMachineFacesAwayFromItsWall() {
        ArrayChunkBuffer buffer = room(false);
        // Against the north wall (z 0) at x 10, z 1: the front points south.
        assertEquals(FurnishItem.SOUTH, FurnishPass.machineFacing(buffer::get, 10, Y, 1));
        assertEquals(FurnishItem.WEST, FurnishPass.machineFacing(buffer::get, 14, Y, 10));
        assertEquals("{\"mID\":14,\"mFacing\":3}", FurnishPass.machineTemplate(14, FurnishItem.SOUTH));
    }

    @Test
    public void cellarsAreBasementsAndWorkshopsIndustrial() {
        assertEquals(RoomKind.BASEMENT, RoomKind.forFloor("nh_walkup", -1, 1L));
        assertEquals(RoomKind.INDUSTRIAL, RoomKind.forFloor("nh_workshop", 2, 1L));
        assertEquals(RoomKind.RESIDENTIAL, RoomKind.forFloor("nh_walkup", 3, 1L));
        RoomKind shop = RoomKind.forFloor("nh_corner_shop", 0, 1L);
        assertTrue(shop == RoomKind.SHOP || shop == RoomKind.KITCHEN, shop.id());
        assertEquals(RoomKind.SHOP, RoomKind.byId("shop"));
    }

    /** A passage is told by the name after the mod ID only (the in-place match of D381). */
    @Test
    void passagesMatchTheNameAfterTheModId() {
        assertTrue(RoomScanner.isPassage(DOOR));
        assertTrue(RoomScanner.isPassage(STAIRS));
        assertTrue(RoomScanner.isPassage(LADDER));
        assertTrue(RoomScanner.isPassage(BlockRef.of("minecraft:golden_rail", 0)));
        assertTrue(RoomScanner.isPassage(BlockRef.of("minecraft:fence_gate", 0)));
        assertTrue(RoomScanner.isPassage(BlockRef.of("minecraft:trapdoor", 0)));
        assertFalse(RoomScanner.isPassage(STONE));
        assertFalse(RoomScanner.isPassage(BlockRef.AIR));
        assertFalse(RoomScanner.isPassage(BlockRef.of("railcraft:cube", 0)));
        assertFalse(RoomScanner.isPassage(BlockRef.of("doorgate:cobble", 0)));
    }
}
