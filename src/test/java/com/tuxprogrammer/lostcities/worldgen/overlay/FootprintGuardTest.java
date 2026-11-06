package com.tuxprogrammer.lostcities.worldgen.overlay;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Arrays;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import sun.misc.Unsafe;

import com.tuxprogrammer.lostcities.worldgen.plan.ChunkKind;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;
import com.tuxprogrammer.lostcities.worldgen.plan.CityPlan;

/** The footprint write guard's frames (0005 §3a). The world is left null: only identity matters. */
public class FootprintGuardTest {

    /** Chunk (0, 0) is a street whose footprint starts at Y 68; every other chunk is land. */
    private static final CityPlan PLAN = (cx, cz) -> {
        if (cx == 0 && cz == 0) {
            int[] footprint = new int[256];
            Arrays.fill(footprint, 68);
            return ChunkPlan.builder(0, 0, ChunkKind.CITY)
                .footprint(footprint)
                .build();
        }
        if (cx == 3 && cz == 0) {
            // A park beside a building: no footprint (the yard is RWG's), the door's approach is columns (7, 7) and
            // (8, 7) of the chunk, at ground Y 71.
            boolean[] approach = new boolean[256];
            approach[(7 << 4) | 7] = true;
            approach[(7 << 4) | 8] = true;
            return ChunkPlan.builder(3, 0, ChunkKind.CITY)
                .groundY(71)
                .edgeSource(() -> new ChunkPlan.Edges(new int[4], approach))
                .build();
        }
        if (cx == 2 && cz == 0) {
            // A highway tunnel's chunk: no footprint, a guarded band from Y 69 to 76 (M06).
            return ChunkPlan.builder(2, 0, ChunkKind.NONE)
                .guardBand(69, 76)
                .build();
        }
        return ChunkPlan.none(cx, cz, 0, 71, 70, false);
    };

    /** M06: BOP's lakes, which no populate event announces, stay out of a tunnel; the hill above it stays RWG's. */
    @Test
    public void dropsWritesInTheGuardedBandOfEveryColumnAndNothingAboveIt() {
        FootprintGuard.push(null, PLAN, true);
        assertTrue(FootprintGuard.drops(null, 2, 0, 0, 70, 0, null), "a lake's liquid on the deck");
        assertTrue(FootprintGuard.drops(null, 2, 0, 15, 76, 9, null), "the ceiling, in any column");
        assertTrue(FootprintGuard.drops(null, 2, 0, 7, 69, 7, null), "the row under the floor");
        assertFalse(FootprintGuard.drops(null, 2, 0, 7, 68, 7, null), "below the band passes");
        assertFalse(FootprintGuard.drops(null, 2, 0, 7, 77, 7, null), "a tree on the hill passes");
        FootprintGuard.pop();
        assertFalse(FootprintGuard.drops(null, 2, 0, 0, 70, 0, null), "no population, no guard");
    }

    /**
     * 0.2.0-beta crash: the client thread's chunk setter (a multi-block change packet) read the guard's stack while
     * the integrated server popped it, and indexed -1. A client world is never guarded, whatever the stack holds.
     */
    @Test
    public void neverGuardsAClientWorld() throws Exception {
        World client = clientWorld();
        FootprintGuard.push(client, PLAN, true);
        assertFalse(FootprintGuard.drops(client, 0, 0, 3, 80, 3, null), "a client world's write passes");
        FootprintGuard.noteUnguardedWrite(client, 0, 0, 3, 80, 3, null);
        FootprintGuard.pop();
        assertFalse(FootprintGuard.drops(client, 0, 0, 3, 80, 3, null), "and with no frame at all");
    }

    /** A world object marked remote, made without running a constructor (nothing of it is used but the flag). */
    private static World clientWorld() throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Unsafe unsafe = (Unsafe) field.get(null);
        World world = (World) unsafe.allocateInstance(net.minecraft.client.multiplayer.WorldClient.class);
        Field remote = World.class.getField("isRemote");
        unsafe.putBoolean(world, unsafe.objectFieldOffset(remote), true);
        assertTrue(world.isRemote);
        return world;
    }

    @AfterEach
    public void clear() {
        FootprintGuard.reset();
    }

    @Test
    public void dropsWritesInsideTheFootprintWhileTheDelegatePopulates() {
        assertFalse(FootprintGuard.drops(null, 0, 0, 3, 80, 3, null), "no population, no guard");
        FootprintGuard.push(null, PLAN, true);
        assertTrue(FootprintGuard.drops(null, 0, 0, 3, 80, 3, null), "a tree on the street is dropped");
        assertFalse(FootprintGuard.drops(null, 0, 0, 3, 60, 3, null), "below the footprint passes");
        assertFalse(FootprintGuard.drops(null, 1, 0, 3, 80, 3, null), "outside the city passes");
        FootprintGuard.pop();
        assertFalse(FootprintGuard.drops(null, 0, 0, 3, 80, 3, null));
    }

    @Test
    public void snowAfterPostPassesAndOurOwnPopulationIsNeverGuarded() {
        FootprintGuard.push(null, PLAN, true);
        FootprintGuard.endOfDecoration(null);
        assertFalse(FootprintGuard.drops(null, 0, 0, 3, 80, 3, null), "snow and ice after Post pass");
        FootprintGuard.push(null, PLAN, true);
        FootprintGuard.push(null, PLAN, false);
        assertFalse(FootprintGuard.drops(null, 0, 0, 3, 80, 3, null), "the population phase writes freely");
        FootprintGuard.pop();
        assertTrue(FootprintGuard.drops(null, 0, 0, 3, 80, 3, null), "the nested delegate is guarded again");
    }

    /** M05J: other mods' trees are cut at the footprint; their ores and stones pass. */
    @Test
    public void aTreeFrameDropsOnlyTreeParts() {
        Block leaves = new Block(Material.leaves) {};
        Block wood = new Block(Material.wood) {};
        Block ore = new Block(Material.rock) {};
        FootprintGuard.pushTrees(null, PLAN);
        assertTrue(FootprintGuard.topIsTrees(null));
        assertTrue(FootprintGuard.drops(null, 0, 0, 3, 80, 3, leaves), "a rubber tree's crown over the street");
        assertTrue(FootprintGuard.drops(null, 0, 0, 3, 80, 3, wood), "its trunk");
        assertFalse(FootprintGuard.drops(null, 0, 0, 3, 80, 3, ore), "a GT vein passes");
        assertFalse(FootprintGuard.drops(null, 1, 0, 3, 80, 3, leaves), "a park outside the footprint keeps its tree");
        FootprintGuard.endOfDecoration(null);
        assertTrue(FootprintGuard.drops(null, 0, 0, 3, 80, 3, leaves), "RWG's Post event does not end a tree frame");
        FootprintGuard.pop();
        assertFalse(FootprintGuard.topIsTrees(null));
        assertFalse(FootprintGuard.drops(null, 0, 0, 3, 80, 3, leaves));
    }

    /** Beta audit: Natura's saguaros (material cactus, its tree world generator) grew inside a radio tower's legs. */
    @Test
    public void aTreeFrameDropsCacti() {
        Block saguaro = new Block(Material.cactus) {};
        FootprintGuard.pushTrees(null, PLAN);
        assertTrue(FootprintGuard.drops(null, 0, 0, 3, 80, 3, saguaro), "a saguaro inside a footprint");
        assertFalse(FootprintGuard.drops(null, 1, 0, 3, 80, 3, saguaro), "the desert outside keeps its saguaros");
        FootprintGuard.pop();
    }

    /** Known issue of 0.1.0-beta: a park tree's crown over a street door; the yard beside it keeps its trees. */
    @Test
    public void aDoorApproachRefusesTreePartsEvenOutsideTheFootprint() {
        Block leaves = new Block(Material.leaves) {};
        Block ore = new Block(Material.rock) {};
        FootprintGuard.pushTrees(null, PLAN);
        assertTrue(FootprintGuard.drops(null, 3, 0, 7, 72, 7, leaves), "leaves in the doorway");
        assertTrue(FootprintGuard.drops(null, 3, 0, 8, 73, 7, leaves), "leaves at head height");
        assertFalse(FootprintGuard.drops(null, 3, 0, 7, 74, 7, leaves), "a crown above the door passes");
        assertFalse(FootprintGuard.drops(null, 3, 0, 4, 72, 7, leaves), "the yard's other columns pass");
        assertFalse(FootprintGuard.drops(null, 3, 0, 7, 72, 7, ore), "a vein passes");
        FootprintGuard.pop();
    }

    /** Known issue of 0.1.0-beta: RWG's snow pass after Post put a snow layer in a doorway. */
    @Test
    public void snowAfterPostIsRefusedOnlyOnADoorApproach() {
        Block layer = new Block(Material.snow) {};
        Block ice = new Block(Material.ice) {};
        FootprintGuard.push(null, PLAN, true);
        FootprintGuard.endOfDecoration(null);
        assertTrue(FootprintGuard.drops(null, 3, 0, 7, 72, 7, layer), "a layer in the doorway");
        assertFalse(FootprintGuard.drops(null, 3, 0, 4, 72, 7, layer), "snow elsewhere stays");
        assertFalse(FootprintGuard.drops(null, 0, 0, 3, 80, 3, layer), "snow on a roof stays");
        assertFalse(FootprintGuard.drops(null, 3, 0, 7, 72, 7, ice), "ice is not snow");
        FootprintGuard.pop();
    }

    /** Known issue of 0.1.0-beta: a GregTech boulder cut into a building; outside the footprint it is untouched. */
    @Test
    public void aStoneFrameDropsEveryWriteInTheFootprintOnly() {
        Block granite = new Block(Material.rock) {};
        FootprintGuard.pushStone(null, PLAN);
        assertTrue(FootprintGuard.topIsStone(null));
        assertTrue(FootprintGuard.drops(null, 0, 0, 3, 80, 3, granite), "a boulder inside the footprint");
        assertFalse(FootprintGuard.drops(null, 0, 0, 3, 60, 3, granite), "below the footprint passes");
        assertFalse(FootprintGuard.drops(null, 1, 0, 3, 80, 3, granite), "outside the city passes");
        FootprintGuard.endOfDecoration(null);
        assertTrue(FootprintGuard.drops(null, 0, 0, 3, 80, 3, granite), "Post does not end a stone frame");
        FootprintGuard.pushStone(null, PLAN);
        FootprintGuard.pop();
        assertFalse(FootprintGuard.topIsStone(null), "a frame left by a throw is replaced, not stacked");
        assertFalse(FootprintGuard.drops(null, 0, 0, 3, 80, 3, granite));
    }
}
