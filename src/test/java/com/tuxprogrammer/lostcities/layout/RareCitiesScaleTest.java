package com.tuxprogrammer.lostcities.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.config.profile.BuiltinProfiles;
import com.tuxprogrammer.lostcities.config.profile.ProfileValues;
import com.tuxprogrammer.lostcities.core.ModuleState;
import com.tuxprogrammer.lostcities.layout.highway.PlanningGate;
import com.tuxprogrammer.lostcities.layout.link.CityLinkGraph;
import com.tuxprogrammer.lostcities.worldgen.city.CityFixtures;
import com.tuxprogrammer.lostcities.worldgen.city.CityProfiles;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainSettings;

/**
 * The {@code rarecities} scale-up (D-city-scale-1): against the profile as it was (64-chunk cells, jitter 16, radii
 * 100 to 256 blocks), its cities cover about four times the area and stand about four times as far apart, and every
 * city still has at least two links. Measured over a fixed seed and window on flat ground, so the numbers are layout
 * alone. Also writes a planning-cost comparison to {@code build/city-scale.txt}; timing is never asserted.
 */
public class RareCitiesScaleTest {

    private static final long SEED = 7919L;
    /** Cells measured in each direction from cell 0 (the window is {@code [-CELLS, CELLS)} cells). */
    private static final int NEW_CELLS = 3;
    /** The same square of the world in the old 64-chunk cells. */
    private static final int OLD_CELLS = 4 * NEW_CELLS;

    /** The {@code rarecities} values before the scale-up. */
    static ProfileValues oldValues() {
        Map<String, String> map = new HashMap<>(BuiltinProfiles.values("rarecities"));
        map.put("cityGridCell", "64");
        map.put("cityGridJitter", "16");
        map.put("cityMinRadius", "100");
        map.put("cityMaxRadius", "256");
        return ProfileValues.of(map);
    }

    static ProfileValues newValues() {
        return ProfileValues.of(BuiltinProfiles.values("rarecities"));
    }

    static LayoutPlanner planner(ProfileValues values, boolean flat) {
        NetworkSettings networks = CityProfiles.networkSettings(values, ModuleState.allActive(), PlanningGate.NONE);
        return new LayoutPlanner(
            LayoutInputs
                .builder(
                    SEED,
                    CityProfiles.layoutProfile(TerrainSettings.fromValues(values.asMap()), values),
                    LayoutFixtures.standardAssets(),
                    flat ? LayoutFixtures.flatTerrain() : CityFixtures.terrain())
                .networks(networks)
                .build());
    }

    /** The grid centers of the window {@code [-cells, cells)} cells that yield a city. */
    private static List<CityGrid.Center> centers(LayoutPlanner p, int cells) {
        int cell = p.profile().cityGridCell;
        List<CityGrid.Center> out = new ArrayList<>();
        for (CityGrid.Center c : p.gridCenters(-cells * cell, -cells * cell, cells * cell - 1, cells * cell - 1)) {
            if (p.centerYieldsCity(c.chunkX(), c.chunkZ())) {
                out.add(c);
            }
        }
        return out;
    }

    /** Mean city chunks per city: raw city chunks within each center's reach. Neighbors never overlap here. */
    private static double meanArea(LayoutPlanner p, List<CityGrid.Center> centers) {
        long total = 0;
        for (CityGrid.Center c : centers) {
            int r = c.radius() / 16 + 1;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz <= r * r && p.isCityRaw(c.chunkX() + dx, c.chunkZ() + dz)) {
                        total++;
                    }
                }
            }
        }
        return total / (double) centers.size();
    }

    /** Mean distance in chunks from each center to its nearest neighbor, over the inner centers of the window. */
    private static double meanNearest(List<CityGrid.Center> centers, int cell, int cells) {
        double sum = 0;
        int n = 0;
        int inner = (cells - 1) * cell;
        for (CityGrid.Center a : centers) {
            if (a.chunkX() < -inner || a.chunkX() >= inner || a.chunkZ() < -inner || a.chunkZ() >= inner) {
                continue;
            }
            long best = Long.MAX_VALUE;
            for (CityGrid.Center b : centers) {
                if (a == b) {
                    continue;
                }
                long dx = a.chunkX() - b.chunkX();
                long dz = a.chunkZ() - b.chunkZ();
                best = Math.min(best, dx * dx + dz * dz);
            }
            sum += Math.sqrt(best);
            n++;
        }
        return sum / n;
    }

    @Test
    public void citiesCoverFourTimesTheAreaFourTimesAsFarApart() throws Exception {
        LayoutPlanner before = planner(oldValues(), true);
        LayoutPlanner after = planner(newValues(), true);
        assertEquals(256, after.profile().cityGridCell);
        List<CityGrid.Center> oldCenters = centers(before, OLD_CELLS);
        List<CityGrid.Center> newCenters = centers(after, NEW_CELLS);
        double areaRatio = meanArea(after, newCenters) / meanArea(before, oldCenters);
        double spacingRatio = meanNearest(newCenters, 256, NEW_CELLS) / meanNearest(oldCenters, 64, OLD_CELLS);
        report(
            String.format(
                Locale.ROOT,
                "cities old=%d new=%d meanArea old=%.1f new=%.1f chunks ratio=%.3f meanNearest old=%.1f new=%.1f"
                    + " chunks ratio=%.3f",
                oldCenters.size(),
                newCenters.size(),
                meanArea(before, oldCenters),
                meanArea(after, newCenters),
                areaRatio,
                meanNearest(oldCenters, 64, OLD_CELLS),
                meanNearest(newCenters, 256, NEW_CELLS),
                spacingRatio));
        assertTrue(areaRatio >= 3 && areaRatio <= 5, "area ratio " + areaRatio);
        assertTrue(spacingRatio >= 3 && spacingRatio <= 5, "spacing ratio " + spacingRatio);
    }

    @Test
    public void everyCityKeepsTwoLinksAtTheNewScale() throws Exception {
        LayoutPlanner p = planner(newValues(), true);
        CityLinkGraph graph = p.networks()
            .links();
        assertEquals(CityLinkGraph.maxDistanceFor(256), graph.maxDistance());
        int nodes = 0;
        long linkChunks = 0;
        int links = 0;
        int longest = 0;
        for (int cx = -NEW_CELLS; cx < NEW_CELLS; cx++) {
            for (int cz = -NEW_CELLS; cz < NEW_CELLS; cz++) {
                int[] node = graph.node(cx, cz);
                if (node == null) {
                    continue;
                }
                nodes++;
                List<CityLinkGraph.Link> at = graph.links(cx, cz);
                assertTrue(at.size() >= 2, "city " + node[0] + "," + node[1] + " has " + at.size() + " links");
                for (CityLinkGraph.Link link : at) {
                    int length = (int) Math.round(
                        Math.sqrt(
                            (double) (link.bx() - link.ax()) * (link.bx() - link.ax())
                                + (double) (link.bz() - link.az()) * (link.bz() - link.az())));
                    assertTrue(length <= graph.maxDistance(), "link " + link + " is " + length + " chunks");
                    longest = Math.max(longest, length);
                    linkChunks += length;
                    links++;
                }
            }
        }
        assertTrue(nodes >= 30, "nodes " + nodes);
        report(
            String.format(
                Locale.ROOT,
                "links nodes=%d linkEnds=%d meanLength=%.1f longest=%d chunks (cap %d)",
                nodes,
                links,
                linkChunks / (double) links,
                longest,
                graph.maxDistance()));
    }

    /**
     * Planning cost per chunk over the same world square (the character fixture terrain), old against new, each run
     * twice on a fresh planner with the faster run kept. Reported, not asserted: a unit runner's timing is noise.
     */
    @Test
    public void reportsPlanningCostPerChunk() throws Exception {
        int half = 96;
        planWindow(planner(oldValues(), false), 32);
        double[] best = { Double.MAX_VALUE, Double.MAX_VALUE };
        for (int round = 0; round < 2; round++) {
            best[0] = Math.min(best[0], planWindow(planner(oldValues(), false), half));
            best[1] = Math.min(best[1], planWindow(planner(newValues(), false), half));
        }
        report(
            String.format(
                Locale.ROOT,
                "planning window=%d chunks old=%.1f us/chunk new=%.1f us/chunk ratio=%.2f",
                (2 * half) * (2 * half),
                best[0],
                best[1],
                best[1] / best[0]));
    }

    /** Plans every chunk of the square and its networks; returns microseconds per chunk. */
    private static double planWindow(LayoutPlanner p, int half) {
        long start = System.nanoTime();
        int sink = 0;
        for (int z = -half; z < half; z++) {
            for (int x = -half; x < half; x++) {
                sink += p.plan(x, z)
                    .isCity() ? 1 : 0;
                sink += p.networks()
                    .highway(x, z)
                    .hasHighway() ? 1 : 0;
                sink += p.networks()
                    .railChunk(x, z)
                    .isPresent() ? 1 : 0;
            }
        }
        long elapsed = System.nanoTime() - start;
        assertTrue(sink >= 0);
        return elapsed / 1000.0 / ((2 * half) * (2 * half));
    }

    private static synchronized void report(String line) throws Exception {
        System.out.println("[city-scale] " + line);
        File out = new File("build", "city-scale.txt");
        out.getParentFile()
            .mkdirs();
        try (Writer w = new OutputStreamWriter(new FileOutputStream(out, true), StandardCharsets.UTF_8)) {
            w.write(line);
            w.write('\n');
        }
    }
}
