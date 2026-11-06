package com.tuxprogrammer.lostcities.worldgen.overlay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSilverfish;
import net.minecraft.block.material.Material;
import net.minecraft.world.IBlockAccess;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.worldgen.city.furnish.FurnishItem;

/**
 * D-gtl-1: no block whose light opacity reads the world is written while {@code provideChunk} builds a chunk. GT's
 * {@code BlockMachines.getLightOpacity} reads its tile entity through the world (GT5-Unofficial 5.09.54.265), and the
 * overlay's sky light pass runs before the chunk is in the chunk map, so a machine there regenerated the chunk until
 * the stack overflowed on a fresh RWG2 world. GT is not on the test classpath; a block made here stands in for it.
 */
public class TerrainStandInsTest {

    /** A block that reads its tile entity for its opacity, as GT's machine block does. */
    private static final class WorldReadingMachine extends Block {

        WorldReadingMachine() {
            super(Material.iron);
        }

        @Override
        public int getLightOpacity(IBlockAccess world, int x, int y, int z) {
            return world.getTileEntity(x, y, z) == null ? 255 : 0;
        }
    }

    private static Block plain() {
        return new Block(Material.rock) {};
    }

    @Test
    public void aBlockOverridingTheWorldOpacityLookupReadsTheWorld() {
        assertTrue(TerrainStandIns.readsWorld(new WorldReadingMachine()));
        assertFalse(TerrainStandIns.readsWorld(plain()));
        assertFalse(TerrainStandIns.readsWorld((Block) null));
    }

    /** The stand-in itself must be safe, or the swap would only move the crash. */
    @Test
    public void theStandInsOpacityDoesNotReadTheWorld() {
        assertFalse(TerrainStandIns.readsWorld(BlockSilverfish.class));
        assertEquals("minecraft:monster_egg", TerrainStandIns.STAND_IN);
    }

    @Test
    public void aGtMachineIsWrittenAsTheStandInAndAnyOtherWorldReaderIsDropped() {
        Block standIn = plain();
        Block plain = plain();
        assertSame(standIn, TerrainStandIns.terrainBlock(new WorldReadingMachine(), FurnishItem.GT_MACHINES, standIn));
        assertNull(TerrainStandIns.terrainBlock(new WorldReadingMachine(), "somemod:lamp", standIn));
        assertSame(plain, TerrainStandIns.terrainBlock(plain, "minecraft:stone", standIn));
    }

    /** The terrain buffer's write path: a TE-backed GT machine never reaches the chunk's sections as itself. */
    @Test
    public void theTerrainBufferNeverWritesAWorldReadingGtMachine() {
        BlockRefs refs = new BlockRefs();
        Block standIn = plain();
        refs.bind(TerrainStandIns.STAND_IN_REF, standIn);
        for (int meta = 0; meta < 16; meta++) {
            BlockRef machine = BlockRef.of(FurnishItem.GT_MACHINES, meta);
            refs.bind(machine, new WorldReadingMachine());
            Block written = new WorldChunkBuffer(refs).writable(machine);
            assertSame(standIn, written, "meta " + meta);
            assertFalse(TerrainStandIns.readsWorld(written), "meta " + meta);
        }
        BlockRef lamp = BlockRef.of("somemod:lamp", 0);
        refs.bind(lamp, new WorldReadingMachine());
        assertNull(new WorldChunkBuffer(refs).writable(lamp));
    }
}
