package com.tuxprogrammer.lostcities.worldgen.city.furnish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.worldgen.buffer.ArrayChunkBuffer;

/** Partial GT multiblocks as set dressing (design record 0016, decision 14). */
public class MultiblockShapesTest {

    private static final int FLOOR = 64;
    private static final int Y = FLOOR + 1;
    private static final BlockRef STONE = BlockRef.of("minecraft:stonebrick", 0);
    private static final BlockRef DOOR = BlockRef.of("minecraft:wooden_door", 0);

    /** An always-placing copy of the bundled shapes. */
    private static MultiblockShapes always() {
        MultiblockShapes bundled = MultiblockShapes.bundled();
        return new MultiblockShapes(
            bundled.shapes(),
            1f,
            bundled.decay(),
            java.util.Collections.singleton(RoomKind.INDUSTRIAL),
            new ArrayList<String>());
    }

    /** One walled 16 x 16 storey, four blocks of air high, with a door at X 0, Z 7. */
    private static ArrayChunkBuffer storey() {
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                buffer.set(x, FLOOR, z, STONE);
                boolean edge = x == 0 || z == 0 || x == 15 || z == 15;
                for (int y = Y; y < Y + 5; y++) {
                    buffer.set(x, y, z, edge ? STONE : BlockRef.AIR);
                }
            }
        }
        buffer.set(0, Y, 7, DOOR);
        buffer.set(0, Y + 1, 7, DOOR);
        return buffer;
    }

    private static FurnishPalettes palettes() {
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

    private static List<FurnishPass.Placed> run(ArrayChunkBuffer buffer, long salt, float scale) {
        return FurnishPass.run(
            buffer::get,
            buffer::set,
            new int[] { Y },
            new RoomKind[] { RoomKind.INDUSTRIAL },
            new long[] { salt },
            palettes(),
            scale,
            name -> true,
            always());
    }

    private static boolean isController(FurnishPass.Placed placed) {
        return placed.item()
            .controller();
    }

    @Test
    public void theBundledShapesParseAndAreAllIncomplete() {
        MultiblockShapes shapes = MultiblockShapes.bundled();
        assertEquals(new ArrayList<String>(), shapes.problems());
        assertTrue(
            shapes.shapes()
                .size() >= 3);
        assertTrue(shapes.allows(RoomKind.INDUSTRIAL));
        assertFalse(shapes.allows(RoomKind.OFFICE));
        for (MultiblockShapes.Shape shape : shapes.shapes()) {
            assertTrue(
                shape.cells()
                    .size() < shape.fullCells(),
                shape.id());
            assertTrue(shape.placedLayers() < shape.layers(), shape.id());
            assertFalse(FurnishPalette.denied(shape.controllerId()), shape.id());
        }
        for (String block : shapes.blockNames()) {
            assertTrue(block.startsWith("gregtech:gt.block"), block);
        }
    }

    @Test
    public void aCompleteShapeOrAMissingCitationIsRejected() {
        List<String> problems = new ArrayList<>();
        MultiblockShapes shapes = MultiblockShapes.read(
            new StringReader(
                "{\"shapes\":[{\"id\":\"full\",\"controller\":1002,\"source\":\"x\",\"omitTop\":0,"
                    + "\"legend\":{\"C\":{\"block\":\"gregtech:gt.blockcasings2\",\"meta\":1}},"
                    + "\"layers\":[[\"CCC\",\"C-C\",\"CCC\"],[\"C~C\",\"C-C\",\"CCC\"]]},"
                    + "{\"id\":\"nosource\",\"controller\":1002,\"omitTop\":1,"
                    + "\"legend\":{\"C\":{\"block\":\"gregtech:gt.blockcasings2\",\"meta\":1}},"
                    + "\"layers\":[[\"CCC\",\"C-C\",\"CCC\"],[\"C~C\",\"C-C\",\"CCC\"]]}]}"),
            problems);
        assertEquals(
            0,
            shapes.shapes()
                .size());
        assertEquals(2, problems.size(), problems.toString());
    }

    @Test
    public void everyShapeFindsItsFacingFromItsCasings() {
        for (MultiblockShapes.Shape shape : MultiblockShapes.bundled()
            .shapes()) {
            for (int facing = 0; facing < 4; facing++) {
                ArrayChunkBuffer buffer = storey();
                boolean[] free = new boolean[256];
                for (int i = 0; i < 256; i++) {
                    free[i] = (i & 15) > 0 && (i & 15) < 15 && (i >> 4) > 0 && (i >> 4) < 15;
                }
                assertTrue(shape.fits(buffer::get, free, 7, 7, Y, facing), shape.id());
                shape.place(buffer::set, name -> true, 99L, 0.12f, 7, 7, Y, facing);
                int found = shape.facingAt(buffer::get, 7, 7, Y + shape.controllerLayer());
                assertEquals(facing, found, shape.id());
            }
        }
    }

    @Test
    public void theBoxStaysInsideTheChunkAndNeverFormsTheWholeMachine() {
        int placedShapes = 0;
        for (long salt = 1; salt <= 60; salt++) {
            ArrayChunkBuffer buffer = storey();
            ArrayChunkBuffer before = storey();
            List<FurnishPass.Placed> placed = run(buffer, salt, 1f);
            long controllers = placed.stream()
                .filter(MultiblockShapesTest::isController)
                .count();
            assertTrue(controllers <= 1, "at most one multiblock per chunk");
            long tiles = placed.stream()
                .filter(
                    p -> p.item()
                        .tile())
                .count();
            assertTrue(tiles <= FurnishPass.TILES_PER_CHUNK, "chunk tile cap");
            if (controllers == 0) {
                continue;
            }
            placedShapes++;
            FurnishPass.Placed controller = placed.stream()
                .filter(MultiblockShapesTest::isController)
                .findFirst()
                .get();
            assertTrue(controller.x() >= 2 && controller.x() <= 13 && controller.z() >= 2 && controller.z() <= 13);
            long casings = 0;
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = FLOOR; y < Y + 5; y++) {
                        BlockRef now = buffer.get(x, y, z);
                        if (!now.equals(before.get(x, y, z)) && now.name()
                            .startsWith("gregtech:gt.blockcasings")) {
                            casings++;
                            assertTrue(x > 0 && x < 15 && z > 0 && z < 15, "inside the walls");
                        }
                    }
                }
            }
            MultiblockShapes.Shape shape = MultiblockShapes.bundled()
                .shapes()
                .stream()
                .filter(
                    s -> s.controllerId() == controller.item()
                        .gtId())
                .findFirst()
                .get();
            assertTrue(casings > 0);
            assertTrue(casings < shape.fullCells(), shape.id() + " must stay incomplete");
            assertTrue(
                casings <= shape.cells()
                    .size());
            assertTrue(reachable(buffer, controller));
        }
        assertTrue(placedShapes >= 10, "multiblocks are placed: " + placedShapes);
    }

    /** Whether the open cell in front of the controller can be walked to from the door. */
    private static boolean reachable(ArrayChunkBuffer buffer, FurnishPass.Placed controller) {
        boolean[] seen = new boolean[256];
        int[] stack = new int[256];
        int top = 0;
        stack[top++] = (7 << 4) | 1;
        seen[(7 << 4) | 1] = true;
        while (top > 0) {
            int i = stack[--top];
            int x = i & 15;
            int z = i >> 4;
            int[][] around = { { x - 1, z }, { x + 1, z }, { x, z - 1 }, { x, z + 1 } };
            for (int[] n : around) {
                if (n[0] < 1 || n[1] < 1 || n[0] > 14 || n[1] > 14) {
                    continue;
                }
                int ni = (n[1] << 4) | n[0];
                if (!seen[ni] && buffer.get(n[0], Y, n[1])
                    .isAir()) {
                    seen[ni] = true;
                    stack[top++] = ni;
                }
            }
        }
        int fx = controller.x();
        int fz = controller.z();
        int[][] sides = { { fx - 1, fz }, { fx + 1, fz }, { fx, fz - 1 }, { fx, fz + 1 } };
        for (int[] n : sides) {
            if (seen[(n[1] << 4) | n[0]]) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void placementIsDeterministic() {
        for (long salt = 1; salt <= 12; salt++) {
            ArrayChunkBuffer a = storey();
            ArrayChunkBuffer b = storey();
            List<FurnishPass.Placed> pa = run(a, salt, 1f);
            List<FurnishPass.Placed> pb = run(b, salt, 1f);
            assertEquals(pa.toString(), pb.toString());
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = FLOOR; y < Y + 5; y++) {
                        assertEquals(a.get(x, y, z), b.get(x, y, z));
                    }
                }
            }
        }
    }

    @Test
    public void theControllerIsFoundAgainByThePopulationPhase() {
        int found = 0;
        for (long salt = 1; salt <= 40; salt++) {
            ArrayChunkBuffer buffer = storey();
            List<FurnishPass.Placed> placed = run(buffer, salt, 1f);
            FurnishPass.Placed controller = placed.stream()
                .filter(MultiblockShapesTest::isController)
                .findFirst()
                .orElse(null);
            if (controller == null) {
                continue;
            }
            List<FurnishPass.Completion> done = FurnishPass
                .completions(buffer::get, Y, FurnishPass.jobArgument(RoomKind.INDUSTRIAL, salt), palettes(), always());
            FurnishPass.Completion match = null;
            for (FurnishPass.Completion c : done) {
                if (c.y() >= 0) {
                    match = c;
                }
            }
            assertNotNull(match, "salt " + salt);
            assertEquals(controller.x(), match.x());
            assertEquals(controller.z(), match.z());
            assertEquals(controller.y(), match.y());
            assertEquals(
                controller.item()
                    .gtId(),
                match.gtId());
            assertTrue(
                match.template()
                    .contains("\"mID\":" + match.gtId()));
            found++;
        }
        assertTrue(found >= 5);
    }

    @Test
    public void aRuinedBuildingPlacesFewerAndOtherRoomKindsNone() {
        int full = 0;
        int ruined = 0;
        for (long salt = 1; salt <= 80; salt++) {
            full += run(storey(), salt, 1f).stream()
                .filter(MultiblockShapesTest::isController)
                .count();
            ruined += run(storey(), salt, 0.5f).stream()
                .filter(MultiblockShapesTest::isController)
                .count();
        }
        assertTrue(ruined < full, ruined + " < " + full);
        ArrayChunkBuffer buffer = storey();
        List<FurnishPass.Placed> office = FurnishPass.run(
            buffer::get,
            buffer::set,
            new int[] { Y },
            new RoomKind[] { RoomKind.OFFICE },
            new long[] { 5L },
            palettes(),
            1f,
            name -> true,
            always());
        assertTrue(
            office.stream()
                .noneMatch(MultiblockShapesTest::isController));
    }

    @Test
    public void aShapeWithAnUnknownBlockIsSkipped() {
        ArrayChunkBuffer buffer = storey();
        for (long salt = 1; salt <= 30; salt++) {
            List<FurnishPass.Placed> placed = FurnishPass.run(
                buffer::get,
                buffer::set,
                new int[] { Y },
                new RoomKind[] { RoomKind.INDUSTRIAL },
                new long[] { salt },
                palettes(),
                1f,
                name -> !name.startsWith("gregtech:"),
                always());
            assertTrue(
                placed.stream()
                    .noneMatch(MultiblockShapesTest::isController));
        }
    }
}
