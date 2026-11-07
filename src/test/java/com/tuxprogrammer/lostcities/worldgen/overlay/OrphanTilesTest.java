package com.tuxprogrammer.lostcities.worldgen.overlay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.worldgen.buffer.ArrayChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.city.furnish.FurnishPalette;
import com.tuxprogrammer.lostcities.worldgen.city.furnish.FurnishPalettes;
import com.tuxprogrammer.lostcities.worldgen.city.furnish.FurnishPass;
import com.tuxprogrammer.lostcities.worldgen.city.furnish.RoomKind;
import com.tuxprogrammer.lostcities.worldgen.plan.PopulationJob;

/**
 * The orphan sweep of the population phase (decision log D-popsync-1): furniture a later terrain pass moved off its
 * floor is still completed, and a spawner nothing can give a mob is air, never vanilla's pig spawner.
 */
public class OrphanTilesTest {

    private static final BlockRef STONE = BlockRef.of("minecraft:stonebrick", 0);
    private static final BlockRef SPAWNER = BlockRef.of(FurnishPalette.SPAWNER, 0);
    private static final BlockRef CHEST = BlockRef.of(FurnishPalette.CHEST, 0);
    private static final int GROUND = 71;
    private static final int PITCH = 8;
    /** The upper floor's standing level, as nh_workshop's second floor in the playtest world (y 80). */
    private static final int UPPER = GROUND + PITCH + 1;

    /** Two walled storeys, slabs at {@link #GROUND} and one pitch above. */
    private static ArrayChunkBuffer building() {
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        for (int s = 0; s < 2; s++) {
            int slab = GROUND + s * PITCH;
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    buffer.set(x, slab, z, STONE);
                    boolean edge = x == 0 || z == 0 || x == 15 || z == 15;
                    for (int y = slab + 1; y < slab + PITCH; y++) {
                        buffer.set(x, y, z, edge ? STONE : BlockRef.AIR);
                    }
                }
            }
        }
        return buffer;
    }

    /** Drops a column's blocks from {@code from} up one block, as the floating-fragment pass drops a blob. */
    private static void dropColumn(ArrayChunkBuffer buffer, int x, int z, int from) {
        for (int y = from; y < UPPER + PITCH; y++) {
            buffer.set(x, y - 1, z, buffer.get(x, y, z));
        }
    }

    /** Furnishes the upper floor as industrial with the bundled palettes; returns the spawner placed, or null. */
    private static FurnishPass.Placed furnishedSpawner(ArrayChunkBuffer buffer, long salt) {
        List<FurnishPass.Placed> placed = FurnishPass.run(
            buffer::get,
            buffer::set,
            new int[] { UPPER },
            new RoomKind[] { RoomKind.INDUSTRIAL },
            new long[] { salt },
            FurnishPalettes.bundled(),
            1f,
            name -> true);
        for (FurnishPass.Placed p : placed) {
            if (FurnishPalette.SPAWNER.equals(
                p.item()
                    .block())) {
                return p;
            }
        }
        return null;
    }

    @Test
    public void aFurnishedSpawnerAFallMovedBelowItsFloorIsStillGivenItsMob() {
        ArrayChunkBuffer buffer = null;
        FurnishPass.Placed spawner = null;
        long salt = 0;
        for (; salt < 500 && spawner == null; salt++) {
            buffer = building();
            spawner = furnishedSpawner(buffer, salt);
        }
        assertNotNull(spawner, "the industrial palette places a spawner for some salt");
        salt--;
        String argument = FurnishPass.jobArgument(RoomKind.INDUSTRIAL, salt);
        int x = spawner.x();
        int z = spawner.z();
        // The playtest: the strip under the spawner fell one block after the furnish pass.
        dropColumn(buffer, x, z, GROUND + PITCH);
        assertEquals(SPAWNER, buffer.get(x, UPPER - 1, z));

        // The bug: the floor's job scans its standing level and misses the spawner, which kept vanilla's pig.
        boolean[] handled = new boolean[16 * 16 * 256];
        for (FurnishPass.Completion c : FurnishPass
            .completions(buffer::get, UPPER, argument, FurnishPalettes.bundled())) {
            assertFalse(c.x() == x && c.z() == z, "the floor's job sees no spawner at its level");
            handled[OrphanTiles.index(c.x(), c.y() >= 0 ? c.y() : UPPER, c.z())] = true;
        }

        // The fix: the sweep finds it and gives it the mob its floor drew for that column.
        List<PopulationJob> jobs = Collections
            .singletonList(PopulationJob.of(PopulationJob.Kind.FURNISH, 0, UPPER, 0, argument));
        List<OrphanTiles.Orphan> orphans = OrphanTiles
            .find(buffer::get, jobs, i -> handled[i], FurnishPalettes.bundled(), 0, 255);
        assertEquals(1, orphans.size(), orphans.toString());
        OrphanTiles.Orphan orphan = orphans.get(0);
        assertEquals(OrphanTiles.Action.COMPLETE, orphan.action());
        assertEquals(UPPER - 1, orphan.y());
        String drawn = FurnishPalettes.bundled()
            .palette(RoomKind.INDUSTRIAL)
            .mobAt(salt, x, UPPER, z);
        assertEquals(drawn, orphan.mob());
    }

    @Test
    public void aPartSpawnerThatFellTakesItsColumnJobsMob() {
        ArrayChunkBuffer buffer = building();
        buffer.set(5, UPPER - 2, 6, SPAWNER);
        PopulationJob job = PopulationJob.of(PopulationJob.Kind.SPAWNER, 5, UPPER, 6, "Skeleton");
        List<OrphanTiles.Orphan> orphans = OrphanTiles
            .find(buffer::get, Arrays.asList(job), i -> false, FurnishPalettes.bundled(), 0, 255);
        assertEquals(1, orphans.size(), orphans.toString());
        assertEquals(
            "Skeleton",
            orphans.get(0)
                .mob());
    }

    @Test
    public void aSpawnerWithNoSourceIsClearedAndAChestWithNoSourceIsNotOurs() {
        ArrayChunkBuffer buffer = building();
        buffer.set(3, UPPER, 3, SPAWNER);
        buffer.set(4, UPPER, 3, CHEST);
        List<OrphanTiles.Orphan> orphans = OrphanTiles.find(
            buffer::get,
            Collections.singletonList(PopulationJob.of(PopulationJob.Kind.TORCH, 1, UPPER, 1, "")),
            i -> false,
            FurnishPalettes.bundled(),
            0,
            255);
        assertEquals(1, orphans.size(), orphans.toString());
        assertEquals(
            OrphanTiles.Action.CLEAR,
            orphans.get(0)
                .action());
        assertEquals(
            3,
            orphans.get(0)
                .x());
    }

    @Test
    public void aPositionAJobCompletedIsNoOrphan() {
        ArrayChunkBuffer buffer = building();
        buffer.set(3, UPPER, 3, SPAWNER);
        int done = OrphanTiles.index(3, UPPER, 3);
        assertTrue(
            OrphanTiles.find(buffer::get, Collections.emptyList(), i -> i == done, FurnishPalettes.bundled(), 0, 255)
                .isEmpty());
    }
}
