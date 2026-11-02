package com.tuxprogrammer.lostcities.worldgen.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.model.Stuff;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.worldgen.TestTerrain;
import com.tuxprogrammer.lostcities.worldgen.buffer.ArrayChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.plan.ContentContext;

/** {@link StuffWriter}: chains and cobwebs in city buildings, and the stuff fixes. */
public class StuffWriterTest {

    private static final LayoutCityPlan PLAN = CityFixtures.plan();
    private static final LayoutPlanner PLANNER = PLAN.planner();
    private static final BlockRef WEB = BlockRef.of("minecraft:web", 0);

    /** Counts the bundled cobwebs and chains in a generated chunk. */
    private static int stuffBlocks(ArrayChunkBuffer buffer) {
        int count = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 256; y++) {
                    String name = buffer.get(x, y, z)
                        .name();
                    if (WEB.name()
                        .equals(name) || name.endsWith(":chain")) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static int[] building() {
        return CityFixtures.first(
            20,
            (x, z) -> PLANNER.plan(x, z)
                .hasBuilding()
                && PLANNER.plan(x, z)
                    .ruinHeight() < 0);
    }

    private static int[] street() {
        return CityFixtures.first(
            20,
            (x, z) -> PLANNER.plan(x, z)
                .isCity()
                && !PLANNER.plan(x, z)
                    .hasBuilding());
    }

    @Test
    public void buildingsGetChainsAndCobwebsHangingFromCeilings() {
        int total = 0;
        int webs = 0;
        java.util.List<int[]> buildings = new java.util.ArrayList<>();
        com.tuxprogrammer.lostcities.worldgen.terrain.TerrainPhase phase = new com.tuxprogrammer.lostcities.worldgen.terrain.TerrainPhase(
            TestTerrain.context(CityFixtures.SEED),
            new com.tuxprogrammer.lostcities.worldgen.plan.CachedCityPlan(PLAN));
        CityFixtures.square(16, c -> {
            // Explosion damage runs after the stuff, as upstream's does, and may take a ceiling away: leave it out.
            if (buildings.size() < 8 && PLANNER.plan(c[0], c[1])
                .hasBuilding()
                && !phase.damageArea(c[0], c[1])
                    .hasExplosions()) {
                buildings.add(c);
            }
        });
        for (int[] found : buildings) {
            ArrayChunkBuffer buffer = CityFixtures.generate(PLAN, found[0], found[1]);
            total += stuffBlocks(buffer);
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 1; y < 255; y++) {
                        if (WEB.equals(buffer.get(x, y, z))) {
                            webs++;
                        }
                    }
                }
            }
        }
        assertTrue(total > 0, "no chain or cobweb in any building");
        assertTrue(webs > 0);
        // The stuff alone, over floors of stone every sixth block: every column it places hangs from one.
        int[] found = buildings.get(0);
        ArrayChunkBuffer floors = new ArrayChunkBuffer();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 256; y++) {
                    if (y % 6 == 5) {
                        floors.set(x, y, z, TestTerrain.STONE);
                    }
                }
            }
        }
        int placed = new StuffWriter(PLAN.context(), found[0], found[1], StuffWriter.NO_ORES)
            .write(floors, new ContentContext(found[0], found[1], null, null, null, new Random(0), null));
        assertTrue(placed > 0);
        int columns = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 255; y++) {
                    BlockRef block = floors.get(x, y, z);
                    if (!block.isAir() && !block.equals(TestTerrain.STONE)) {
                        columns++;
                        assertEquals(4, y % 6, "hangs under a floor at " + x + "," + y + "," + z);
                    }
                }
            }
        }
        assertEquals(placed, columns);
    }

    @Test
    public void doesNotReproduceASSET21_equalCountsPlaceExactlyThatManyWithNoDraw() {
        Random random = new Random(1);
        long before = new Random(1).nextLong();
        assertEquals(5, StuffWriter.count(random, 5, 5));
        assertEquals(before, random.nextLong(), "no draw was taken");
        for (int i = 0; i < 200; i++) {
            int count = StuffWriter.count(random, 5, 10);
            assertTrue(count >= 5 && count < 10, "maxcount stays exclusive: " + count);
        }
    }

    @Test
    public void doesNotReproduceSCAT20_aStreetHasNoBuildingForTheFilter() {
        // Upstream tested the building that would have stood on the street; here a street has none, so a stuff whose
        // buildings filter names any building never runs there, and the bundled stuff (in buildings only) neither.
        int[] found = street();
        ChunkPlan plan = PLANNER.plan(found[0], found[1]);
        assertFalse(plan.hasBuilding());
        ArrayChunkBuffer buffer = CityFixtures.generate(PLAN, found[0], found[1]);
        assertEquals(0, stuffBlocks(buffer));
    }

    @Test
    public void doesNotReproduceSCAT21_theRandomMixesInTheWorldSeed() {
        assertNotEquals(StuffWriter.seed(1L, 3, 4), StuffWriter.seed(2L, 3, 4));
        assertNotEquals(StuffWriter.seed(1L, 3, 4), StuffWriter.seed(1L, 4, 3));
    }

    @Test
    public void doesNotReproduceSCAT22_theSameChunkGetsTheSameStuffEveryTime() {
        int[] found = building();
        ArrayChunkBuffer first = CityFixtures.generate(PLAN, found[0], found[1]);
        ArrayChunkBuffer second = CityFixtures.generate(CityFixtures.plan(), found[0], found[1]);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 256; y++) {
                    assertEquals(first.get(x, y, z), second.get(x, y, z));
                }
            }
        }
        // The asset set lists each tag's stuff in id order.
        String previous = "";
        for (Stuff stuff : CityFixtures.ASSETS.stuffByTag("rubble")) {
            assertTrue(
                stuff.id()
                    .toString()
                    .compareTo(previous) > 0);
            previous = stuff.id()
                .toString();
        }
    }

    @Test
    public void doesNotReproduceSCAT23_skyIsTheChunksOwnColumn() {
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        assertTrue(StuffWriter.seesSky(buffer, 3, 70, 3));
        buffer.set(3, 90, 3, TestTerrain.STONE);
        assertFalse(StuffWriter.seesSky(buffer, 3, 70, 3));
        assertTrue(StuffWriter.seesSky(buffer, 3, 90, 3));
    }

    @Test
    public void softEdgeColumnsAreLeftAlone() {
        int[] found = building();
        boolean[] strip = new boolean[256];
        java.util.Arrays.fill(strip, true);
        ArrayChunkBuffer buffer = CityFixtures.generate(PLAN, found[0], found[1]);
        int before = stuffBlocks(buffer);
        new StuffWriter(PLAN.context(), found[0], found[1], StuffWriter.NO_ORES)
            .write(buffer, new ContentContext(found[0], found[1], null, null, null, new Random(0), strip));
        assertEquals(before, stuffBlocks(buffer));
    }
}
