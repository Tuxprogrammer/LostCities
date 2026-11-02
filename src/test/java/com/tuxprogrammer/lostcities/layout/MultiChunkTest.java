package com.tuxprogrammer.lostcities.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.layout.view.InfrastructureView;
import com.tuxprogrammer.lostcities.layout.view.MultiSettings;
import com.tuxprogrammer.lostcities.layout.view.RailChunkType;
import com.tuxprogrammer.lostcities.layout.view.RailInfo;

/** {@link MultiChunk}: multibuilding placement per multichunk, with the CITY-02/09/25/26 fixes. */
public class MultiChunkTest {

    private static List<String> placements(LayoutPlanner p, int multiX, int multiZ) {
        MultiChunk chunk = MultiChunk.calculate(p, multiX, multiZ);
        List<String> out = new ArrayList<>();
        for (int x = multiX * 10; x < multiX * 10 + 10; x++) {
            for (int z = multiZ * 10; z < multiZ * 10 + 10; z++) {
                MultiChunk.Placement placement = chunk.placementAt(x, z);
                if (placement != null) {
                    out.add(x + "," + z + ":" + placement.name + "@" + placement.offsetX + placement.offsetZ);
                }
            }
        }
        return out;
    }

    private static LayoutPlanner dense(long seed) {
        return LayoutFixtures
            .planner(seed, LayoutFixtures.denseCity(), LayoutFixtures.flatTerrain(), StructureOracle.NONE);
    }

    @Test
    public void placesWholeFootprintsInsideTheArea() {
        LayoutPlanner p = dense(3L);
        int found = 0;
        for (int mx = -2; mx < 2; mx++) {
            for (int mz = -2; mz < 2; mz++) {
                for (String s : placements(p, mx, mz)) {
                    found++;
                    assertTrue(s.contains("multi2x2"));
                }
            }
        }
        assertTrue(found > 0 && found % 4 == 0, "found " + found);
    }

    @Test
    public void doesNotReproduceCITY02_placementDependsOnTheWorldSeed() {
        List<String> a = new ArrayList<>();
        List<String> b = new ArrayList<>();
        // The same terrain and city mask (flat, dense); only the world seed differs.
        for (int mx = -2; mx < 2; mx++) {
            a.addAll(placements(dense(10L), mx, 0));
            b.addAll(placements(dense(11L), mx, 0));
        }
        assertNotEquals(a, b);
    }

    @Test
    public void doesNotReproduceCORE21_stylePickNeverReturnsNull() {
        // Every style pick goes through the null-safe picker; a full area of picks must place without a crash.
        LayoutPlanner p = dense(12L);
        for (int mx = -3; mx < 3; mx++) {
            placements(p, mx, mx);
        }
    }

    @Test
    public void doesNotReproduceCITY25_badSettingsPlaceNothingInsteadOfThrowing() {
        LayoutFixtures.FakeAssets assets = LayoutFixtures.standardAssets();
        assets.worldStyle().multiSettings = new MultiSettings(10, 5, 2, .8f, 50);
        LayoutPlanner inverted = new LayoutPlanner(
            LayoutInputs.builder(1L, LayoutFixtures.denseCity(), assets, LayoutFixtures.flatTerrain())
                .build());
        assertEquals(0, placements(inverted, 0, 0).size());

        LayoutFixtures.FakeAssets huge = LayoutFixtures.standardAssets();
        huge.add(new LayoutFixtures.FakeMulti("multi2x2", 11, 2, "building1"));
        LayoutPlanner tooLarge = new LayoutPlanner(
            LayoutInputs.builder(1L, LayoutFixtures.denseCity(), huge, LayoutFixtures.flatTerrain())
                .build());
        assertEquals(0, placements(tooLarge, 0, 0).size());
    }

    @Test
    public void doesNotReproduceCITY26_railClearanceUsesTheFootprintsOwnLevel() {
        // Rails at level 0 everywhere. The multichunk's top-left chunk is low (level 0); every other chunk is high
        // (level 4), with room for cellars above the rail. Upstream tested every position against the top-left's
        // level and placed nothing.
        InfrastructureView rails = new InfrastructureView() {

            @Override
            public int highwayXLevel(int chunkX, int chunkZ) {
                return -1;
            }

            @Override
            public int highwayZLevel(int chunkX, int chunkZ) {
                return -1;
            }

            @Override
            public RailInfo rail(int chunkX, int chunkZ) {
                return new RailInfo(RailChunkType.HORIZONTAL, 0);
            }

            @Override
            public int railwayLevel() {
                return 0;
            }
        };
        LayoutFixtures.FakeTerrain terrain = new LayoutFixtures.FakeTerrain(
            (x, z) -> Math.floorMod(x, 10) == 0 && Math.floorMod(z, 10) == 0 ? 70 : 110);
        LayoutPlanner p = new LayoutPlanner(
            LayoutInputs.builder(5L, LayoutFixtures.denseCity(), LayoutFixtures.standardAssets(), terrain)
                .infrastructure(rails)
                .build());
        int placed = 0;
        for (int mx = -2; mx < 2; mx++) {
            for (int mz = -2; mz < 2; mz++) {
                placed += placements(p, mx, mz).size();
            }
        }
        assertTrue(placed > 0);
    }
}
