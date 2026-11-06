package com.tuxprogrammer.lostcities.layout.rail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.ToIntBiFunction;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.view.RailChunkType;

/** The railway grid (railway-layout.md) and its rules for landmark cores (D30, D31; new-spec 06). */
public class RailwayLayoutTest {

    static final int GROUND = 71;

    /** Inputs over predicates: cities, levels, cores, margins and the lowest ground of cores. */
    static class Inputs implements RailwayInputs {

        BiPredicate<Integer, Integer> city = (x, z) -> false;
        ToIntBiFunction<Integer, Integer> level = (x, z) -> 0;
        Set<Long> cores = new HashSet<>();
        Set<Long> margins = new HashSet<>();
        int coreGround = 70;

        @Override
        public boolean isCityRaw(int chunkX, int chunkZ) {
            return city.test(chunkX, chunkZ);
        }

        @Override
        public int cityLevel(int chunkX, int chunkZ) {
            return level.applyAsInt(chunkX, chunkZ);
        }

        @Override
        public int highwayXLevel(int chunkX, int chunkZ) {
            return -1;
        }

        @Override
        public int highwayZLevel(int chunkX, int chunkZ) {
            return -1;
        }

        @Override
        public boolean isCore(int chunkX, int chunkZ) {
            return cores.contains(key(chunkX, chunkZ));
        }

        @Override
        public boolean isMargin(int chunkX, int chunkZ) {
            return margins.contains(key(chunkX, chunkZ));
        }

        @Override
        public int lowestGround(int chunkX, int chunkZ) {
            return coreGround;
        }
    }

    static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    static RailwayLayout layout(RailwaySettings settings, Inputs inputs) {
        return new RailwayLayout(4242L, settings, inputs, GROUND, 1);
    }

    /** The chunk at grid coordinates (mx, mz) of the first period: {@code mx = floorMod(x + 1, 20)}. */
    static RailChunk at(RailwayLayout layout, int mx, int mz) {
        return layout.rail(mx - 1, mz - 1);
    }

    @Test
    public void withoutCitiesTheGridIsLinesSplitsAndBends() {
        RailwayLayout layout = layout(RailwaySettings.DEFAULTS, new Inputs());
        assertEquals(RailChunkType.HORIZONTAL, at(layout, 0, 10).type());
        assertEquals(3, at(layout, 0, 10).tracks(), "the three-track spot");
        assertEquals(2, at(layout, 10, 0).tracks());
        assertEquals(1, at(layout, 10, 10).tracks());
        assertEquals(RailChunkType.THREE_SPLIT, at(layout, 5, 10).type());
        assertEquals(RailChunk.Direction.EAST, at(layout, 5, 10).direction());
        assertEquals(RailChunk.Direction.WEST, at(layout, 15, 10).direction());
        assertEquals(RailChunkType.DOUBLE_BEND, at(layout, 5, 0).type());
        assertEquals(RailChunkType.DOUBLE_BEND, at(layout, 15, 0).type());
        assertEquals(RailChunkType.VERTICAL, at(layout, 5, 3).type());
        assertEquals(RailChunkType.VERTICAL, at(layout, 15, 17).type());
        assertEquals(RailChunkType.HORIZONTAL, at(layout, 7, 0).type(), "an approach west of the two-track spot");
        assertEquals(RailChunkType.NONE, at(layout, 0, 0).type());
        assertEquals(RailChunkType.NONE, at(layout, 2, 0).type(), "row 0 is empty west of the bend");
        assertEquals(RailChunkType.NONE, at(layout, 3, 3).type());
        assertEquals(-3, at(layout, 7, 0).level(), "Y 53 at ground 71");
    }

    @Test
    public void aStationSpotInACityIsAStationWithApproachesThatReachTheRailwayLevel() {
        Inputs inputs = new Inputs();
        inputs.city = (x, z) -> true;
        inputs.level = (x, z) -> 1;
        RailwayLayout layout = layout(RailwaySettings.DEFAULTS, inputs);
        RailChunk station = at(layout, 0, 10);
        assertTrue(
            station.type()
                .isStation());
        // East of the three-track station, the approach reaches the railway level before the split at mx 5.
        assertEquals(-3, at(layout, 4, 10).level());
        for (int mx = 1; mx < 5; mx++) {
            assertTrue(at(layout, mx, 10).isPresent(), "approach at " + mx);
        }
    }

    @Test
    public void doesNotReproduceHWY01_theWestTestOfTheThreeTrackSpotLooksWest() {
        Inputs inputs = new Inputs();
        // The only city is the spot one spacing west of the three-track spot at (x -1, z 9).
        inputs.city = (x, z) -> x == -11 && z == 9;
        RailwayLayout layout = layout(new RailwaySettings(true, true, true, true, 0, 10, 10), inputs);
        RailChunk spot = layout.rail(-1, 9);
        assertEquals(RailChunkType.RAILS_END_HERE, spot.type(), "upstream found no city west and laid nothing");
        assertEquals(RailChunk.Direction.WEST, spot.direction());
    }

    @Test
    public void doesNotReproduceHWY08_eachWorldsLayoutAnswersFromItsOwnProfile() {
        RailwayLayout on = layout(RailwaySettings.DEFAULTS, new Inputs());
        RailwayLayout off = layout(new RailwaySettings(false, true, true, false, 0, 10, 10), new Inputs());
        // Interleaved: neither answer leaks into the other.
        assertTrue(
            on.rail(6, -1)
                .isPresent());
        assertFalse(
            off.rail(6, -1)
                .isPresent());
        assertTrue(
            on.rail(6, -1)
                .isPresent());
    }

    @Test
    public void doesNotReproduceHWY15_theCachesAreBounded() {
        RailwayLayout layout = layout(RailwaySettings.DEFAULTS, new Inputs());
        for (int x = 0; x < 60; x++) {
            for (int z = 0; z < 60; z++) {
                layout.rail(x, z);
            }
        }
        assertTrue(layout.cachedEntries() <= 2 * RailwayLayout.CACHE_SIZE, "held " + layout.cachedEntries());
    }

    @Test
    public void aLineDeepEnoughPassesUnderACoreUnchanged() {
        Inputs inputs = new Inputs();
        inputs.cores.add(key(1, 9));
        inputs.coreGround = 80;
        RailChunk rail = layout(RailwaySettings.DEFAULTS, inputs).rail(1, 9);
        assertEquals(RailChunkType.HORIZONTAL, rail.type());
        assertEquals(-3, rail.level());
    }

    @Test
    public void aShallowLineDipsUnderACoreAndClimbsBackOutsideIt() {
        Inputs inputs = new Inputs();
        inputs.cores.add(key(1, 9));
        // Ground at Y 60: the part's top must stay at Y 52 or below, level -5 (floor Y 41, top Y 47).
        inputs.coreGround = 60;
        RailwayLayout layout = layout(RailwaySettings.DEFAULTS, inputs);
        RailChunk before = layout.rail(0, 9);
        RailChunk under = layout.rail(1, 9);
        RailChunk after = layout.rail(2, 9);
        assertEquals(RailChunkType.HORIZONTAL, under.type());
        assertEquals(-5, under.level());
        assertEquals(RailChunkType.GOING_DOWN_FURTHER, before.type());
        assertEquals(RailChunk.Direction.EAST, before.direction(), "its low end faces the core");
        assertEquals(-5, before.level());
        assertEquals(-3, before.topLevel());
        assertEquals(RailChunkType.GOING_DOWN_FURTHER, after.type());
        assertEquals(RailChunk.Direction.WEST, after.direction());
        assertEquals(
            -3,
            layout.rail(-1, 9)
                .level(),
            "the line is at its level again past the slopes");
        assertEquals(
            -3,
            layout.rail(3, 9)
                .level());
    }

    @Test
    public void aLineWithNoRoomToDipLeavesOnlyTheShallowCoreChunkOut() {
        Inputs inputs = new Inputs();
        // The core sits on the line's last chunk before the split at x 4: no room for a slope after it.
        inputs.cores.add(key(3, 9));
        inputs.coreGround = 60;
        RailwayLayout layout = layout(RailwaySettings.DEFAULTS, inputs);
        assertFalse(
            layout.rail(3, 9)
                .isPresent());
        assertTrue(
            layout.rail(-1, 9)
                .isPresent());
        assertEquals(
            RailChunkType.THREE_SPLIT,
            layout.rail(4, 9)
                .type());
    }

    @Test
    public void aStationSpotInACoreIsNoStationAndOneInAMarginHasNoStaircase() {
        Inputs inputs = new Inputs();
        inputs.city = (x, z) -> true;
        inputs.cores.add(key(-1, 9));
        inputs.coreGround = 90;
        inputs.margins.add(key(9, -1));
        RailwayLayout layout = layout(RailwaySettings.DEFAULTS, inputs);
        RailChunk core = layout.rail(-1, 9);
        assertFalse(
            core.type()
                .isStation());
        assertEquals(RailChunkType.HORIZONTAL, core.type());
        RailChunk margin = layout.rail(9, -1);
        assertEquals(RailChunkType.STATION_UNDERGROUND, margin.type());
        assertFalse(margin.staircase());
    }
}
