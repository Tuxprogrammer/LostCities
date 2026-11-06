package com.tuxprogrammer.lostcities.layout.rail;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.link.CityLinkGraph;

/**
 * Link-network lines and landmark cores (D322, D359). A landmark whose structure reaches underground inside its
 * core (a LootGames room) blocks a line at any railway level, as a too-shallow core already did; the deep-ground test
 * applies only to surface landmarks. Roguelike Dungeons towers, whose levels reach far past their lot, are steered
 * off link lines instead ({@code SteeringRuleTest}, D361).
 */
public class LinkRailCoresTest {

    /** Two cities on one row, centers at chunks (10, 10) and (74, 10); the line between runs along X. */
    private static final class TwoCities implements CityLinkGraph.Nodes {

        @Override
        public int cell() {
            return 64;
        }

        @Override
        public int[] cityCenter(int cellX, int cellZ) {
            if (cellZ == 0 && (cellX == 0 || cellX == 1)) {
                return new int[] { 10 + 64 * cellX, 10 };
            }
            return null;
        }
    }

    private static final class Inputs extends RailwayLayoutTest.Inputs {

        final Set<Long> underground = new HashSet<>();

        @Override
        public boolean isUndergroundCore(int chunkX, int chunkZ) {
            return underground.contains(RailwayLayoutTest.key(chunkX, chunkZ));
        }
    }

    private static RailwayLayout layout(Inputs inputs) {
        return new RailwayLayout(
            4242L,
            RailwaySettings.DEFAULTS,
            inputs,
            RailwayLayoutTest.GROUND,
            1,
            new CityLinkGraph(new TwoCities(), 16));
    }

    @Test
    public void aLinePassesUnderADeepGroundedSurfaceCore() {
        Inputs inputs = new Inputs();
        inputs.coreGround = 200;
        inputs.cores.add(RailwayLayoutTest.key(40, 10));
        RailwayLayout layout = layout(inputs);
        assertTrue(
            layout.rail(40, 10)
                .isPresent(),
            "a village-like core with ground far above the railway level lets the line pass");
    }

    @Test
    public void aLineThroughAnUndergroundCoreCarriesNoTrack() {
        Inputs inputs = new Inputs();
        inputs.coreGround = 200;
        inputs.cores.add(RailwayLayoutTest.key(40, 10));
        inputs.underground.add(RailwayLayoutTest.key(40, 10));
        RailwayLayout layout = layout(inputs);
        assertFalse(
            layout.rail(40, 10)
                .isPresent(),
            "no track crosses an underground core");
        assertFalse(
            layout.rail(20, 10)
                .isPresent(),
            "the whole line is left out, so no track ends at the core (D322)");
        assertFalse(
            layout.rail(10, 10)
                .isPresent(),
            "and no station waits for it");
    }
}
