package com.tuxprogrammer.lostcities.compat.packhouses;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;

import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureVillagePieces;

import org.junit.jupiter.api.Test;

import cpw.mods.fml.common.registry.VillagerRegistry;

/**
 * A pack house is made with a village start piece and sits inside its chunk with the margin, for every kind that can be
 * made without the game running and every door side, so building it reads and writes only the populated chunk.
 */
public class PackHousesTest {

    private static final int[][] CHUNKS = { { 0, 0 }, { 3, -7 }, { -1, -1 }, { -40, 27 }, { 1875000, -1875000 } };
    private static final int Y = 70;

    /** Places a kind at every test chunk and side; returns how many were placed. */
    private static int placeEverywhere(PackHouses.Usable use) {
        int placed = 0;
        for (int[] chunk : CHUNKS) {
            for (int side = 0; side < 4; side++) {
                long seed = 31L * chunk[0] + chunk[1] * 17L + side;
                StructureComponent piece = PackHouses.place(
                    use,
                    PackHouses.villageStart(null),
                    seed,
                    new Random(seed),
                    chunk[0],
                    chunk[1],
                    Y,
                    side);
                if (piece == null) {
                    continue;
                }
                placed++;
                StructureBoundingBox box = piece.getBoundingBox();
                assertTrue(
                    PackHouses.insideWithMargin(box, chunk[0], chunk[1]),
                    use.kind.id + " side " + side + " at " + chunk[0] + "," + chunk[1] + " box " + box);
                assertEquals(Y, box.minY, use.kind.id);
            }
        }
        return placed;
    }

    @Test
    public void everyVanillaHouseThatFitsSitsInsideItsChunkWithTheMargin() {
        int kinds = 0;
        for (PackHouses.Kind kind : PackHouses.KINDS) {
            if (kind.vanilla == null) {
                continue;
            }
            Method factory = PackHouses.factory(kind.vanilla);
            assertNotNull(factory, kind.id);
            int placed = placeEverywhere(new PackHouses.Usable(kind, null, factory));
            // Every side is the same size turned, so a kind fits at every side or none.
            assertTrue(placed == 0 || placed == CHUNKS.length * 4, kind.id + " placed " + placed);
            if (placed > 0) {
                kinds++;
            }
        }
        assertTrue(kinds >= 4, "vanilla kinds that fit: " + kinds);
    }

    @Test
    public void aModHouseThatReadsTheStartPieceIsMadeAndFits() {
        StructureVillagePieces.PieceWeight weight = new StructureVillagePieces.PieceWeight(StartReader.class, 1, 1);
        VillagerRegistry.instance()
            .registerVillageCreationHandler(new StartReaderHandler());
        PackHouses.Kind kind = new PackHouses.Kind("test.startreader", "test", StartReader.class.getName(), null);
        StartReader.made = 0;
        assertEquals(CHUNKS.length * 4, placeEverywhere(new PackHouses.Usable(kind, weight, null)));
        // Measured, then made again at its box: two per placement.
        assertEquals(CHUNKS.length * 4 * 2, StartReader.made);
    }

    @Test
    public void theStartPieceCarriesTheBiomeAndIsNotADesertWithoutOne() {
        StructureVillagePieces.Start start = PackHouses.villageStart(null);
        assertNotNull(start);
        assertFalse(start.inDesert);
        assertSame(null, start.biome);
    }

    @Test
    public void aBoxAtTheEdgeOrAcrossItIsNotInside() {
        assertTrue(PackHouses.insideWithMargin(new StructureBoundingBox(2, 60, 2, 13, 70, 13), 0, 0));
        assertFalse(PackHouses.insideWithMargin(new StructureBoundingBox(1, 60, 2, 12, 70, 13), 0, 0));
        assertFalse(PackHouses.insideWithMargin(new StructureBoundingBox(2, 60, 2, 14, 70, 13), 0, 0));
        assertFalse(PackHouses.insideWithMargin(new StructureBoundingBox(-14, 60, 2, -3, 70, 13), 0, 0));
        assertTrue(PackHouses.insideWithMargin(new StructureBoundingBox(-14, 60, -14, -3, 70, -3), -1, -1));
    }

    /**
     * Shaped like Forestry's bee house (ForestryMC 4.11.40, {@code ComponentVillageBeeHouse.java:70-77, 103-112}): a
     * 10 by 11 box whose constructor reads the start piece, which threw when the start was {@code null}.
     */
    public static final class StartReader extends StructureVillagePieces.House1 {

        static int made;

        public StartReader() {}

        public StartReader(StructureVillagePieces.Start start, int type, Random random, StructureBoundingBox box,
            int mode) {
            super(start, type, random, box, mode);
            boolean desert = start.inDesert;
            assertFalse(desert);
            made++;
        }

        @Override
        public boolean addComponentParts(World world, Random random, StructureBoundingBox box) {
            return true;
        }
    }

    private static final class StartReaderHandler implements VillagerRegistry.IVillageCreationHandler {

        @Override
        public StructureVillagePieces.PieceWeight getVillagePieceWeight(Random random, int size) {
            return new StructureVillagePieces.PieceWeight(StartReader.class, 1, 1);
        }

        @Override
        public Class<?> getComponentClass() {
            return StartReader.class;
        }

        @Override
        @SuppressWarnings("rawtypes")
        public Object buildComponent(StructureVillagePieces.PieceWeight weight, StructureVillagePieces.Start start,
            List pieces, Random random, int x, int y, int z, int mode, int type) {
            StructureBoundingBox box = StructureBoundingBox
                .getComponentToAddBoundingBox(x, y, z, 0, 0, 0, 10, 9, 11, mode);
            return new StartReader(start, type, random, box, mode);
        }
    }
}
