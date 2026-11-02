package com.tuxprogrammer.lostcities.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.view.FacadeTones;
import com.tuxprogrammer.lostcities.layout.view.FacadeTone;
import com.tuxprogrammer.lostcities.worldgen.city.CityFixtures;

/**
 * The facade stage (alpha review AR-17, docs/design/0007-facade-color-feathering.md §4): neighboring buildings stay
 * within {@code facadeContrast} where their palettes allow, across a desert and a standard city, and the choice does
 * not depend on the order chunks are planned in.
 */
public class FacadePlannerTest {

    private static final long SEED = 4242L;
    /** Half-width of the region checked, in chunks: wide enough for desert and standard cities to meet. */
    private static final int R = 40;
    private static final FacadeTones TONES = new FacadeTones(CityFixtures.ASSETS, CityFixtures.ASSETS.colors());

    /** A neighboring pair of building units, by their top-left chunks. */
    private static final class Pair {

        final int ax;
        final int az;
        final int bx;
        final int bz;
        final boolean styles;

        Pair(int ax, int az, int bx, int bz, boolean styles) {
            this.ax = ax;
            this.az = az;
            this.bx = bx;
            this.bz = bz;
            this.styles = styles;
        }
    }

    private static int[] top(LayoutPlanner planner, int x, int z) {
        ChunkPlan plan = planner.plan(x, z);
        MultiPos pos = plan.multiPos();
        return pos.isSingle() ? new int[] { x, z } : new int[] { x - pos.x(), z - pos.z() };
    }

    /** Every pair of distinct building units whose chunks touch, diagonals included. */
    private static List<Pair> pairs(LayoutPlanner planner) {
        List<Pair> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                if (!planner.plan(x, z)
                    .hasBuilding()) {
                    continue;
                }
                int[] a = top(planner, x, z);
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (!planner.plan(x + dx, z + dz)
                            .hasBuilding()) {
                            continue;
                        }
                        int[] b = top(planner, x + dx, z + dz);
                        if (a[0] == b[0] && a[1] == b[1]) {
                            continue;
                        }
                        String first = a[0] + "," + a[1];
                        String second = b[0] + "," + b[1];
                        String key = first.compareTo(second) < 0 ? first + "|" + second : second + "|" + first;
                        if (!seen.add(key)) {
                            continue;
                        }
                        boolean styles = !planner.plan(a[0], a[1])
                            .cityStyle()
                            .equals(
                                planner.plan(b[0], b[1])
                                    .cityStyle());
                        out.add(new Pair(a[0], a[1], b[0], b[1], styles));
                    }
                }
            }
        }
        return out;
    }

    private static FacadeTone tone(LayoutPlanner planner, int x, int z) {
        ChunkPlan plan = planner.plan(x, z);
        return TONES.tone(plan.palettes(), plan.building());
    }

    private static int clashes(LayoutPlanner planner, List<Pair> pairs, float limit) {
        return clashList(planner, pairs, limit).size();
    }

    /** The pairs over the limit, described for a failure message. */
    private static List<String> clashList(LayoutPlanner planner, List<Pair> pairs, float limit) {
        List<String> out = new ArrayList<>();
        for (Pair pair : pairs) {
            FacadeTone a = tone(planner, pair.ax, pair.az);
            FacadeTone b = tone(planner, pair.bx, pair.bz);
            if (a != null && b != null && FacadeTone.contrast(a, b) > limit + 1e-3f) {
                ChunkPlan pa = planner.plan(pair.ax, pair.az);
                ChunkPlan pb = planner.plan(pair.bx, pair.bz);
                out.add(
                    String.format(
                        "%n  %d,%d %s %s %s | %d,%d %s %s %s | %.1f",
                        pair.ax,
                        pair.az,
                        pa.building(),
                        pa.palettes(),
                        a,
                        pair.bx,
                        pair.bz,
                        pb.building(),
                        pb.palettes(),
                        b,
                        FacadeTone.contrast(a, b)));
            }
        }
        return out;
    }

    @Test
    public void featheringRemovesClashesBetweenNeighbors() {
        LayoutPlanner on = CharacterFixtures.planner(SEED, LayoutInputs.DEFAULT_CACHE_SIZE);
        LayoutPlanner off = CharacterFixtures.planner(SEED, LayoutInputs.DEFAULT_CACHE_SIZE, "facadeContrast", "200");
        List<Pair> pairs = pairs(on);
        int across = 0;
        for (Pair pair : pairs) {
            across += pair.styles ? 1 : 0;
        }
        assertTrue(pairs.size() > 100, "neighboring pairs: " + pairs.size());
        assertTrue(across > 0, "the desert and the standard city meet");
        int before = clashes(off, pairs(off), 45f);
        int after = clashes(on, pairs, 45f);
        assertTrue(before > 0, "without feathering some neighbors clash");
        assertEquals(0, after, "feathering leaves no clash of " + before + clashList(on, pairs, 45f));
    }

    @Test
    public void featheringKeepsVariety() {
        LayoutPlanner on = CharacterFixtures.planner(SEED, LayoutInputs.DEFAULT_CACHE_SIZE);
        List<String> walls = new ArrayList<>();
        for (int x = 0; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                ChunkPlan plan = on.plan(x, z);
                if (plan.hasBuilding() && plan.palettes()
                    .size() > 2
                    && !walls.contains(
                        plan.palettes()
                            .get(2))) {
                    walls.add(
                        plan.palettes()
                            .get(2));
                }
            }
        }
        assertTrue(walls.size() >= 3, "the standard city keeps most of its brick palettes: " + walls);
    }

    @Test
    public void planningOrderDoesNotMatter() {
        LayoutPlanner forward = CharacterFixtures.planner(SEED, LayoutInputs.DEFAULT_CACHE_SIZE);
        LayoutPlanner backward = CharacterFixtures.planner(SEED, 64);
        List<int[]> chunks = new ArrayList<>();
        for (int x = -12; x <= 12; x++) {
            for (int z = -12; z <= 12; z++) {
                chunks.add(new int[] { x, z });
            }
        }
        List<String> a = new ArrayList<>();
        for (int[] c : chunks) {
            a.add(
                c[0] + ","
                    + c[1]
                    + " "
                    + forward.plan(c[0], c[1])
                        .palettes());
        }
        Collections.reverse(chunks);
        List<String> b = new ArrayList<>();
        for (int[] c : chunks) {
            b.add(
                c[0] + ","
                    + c[1]
                    + " "
                    + backward.plan(c[0], c[1])
                        .palettes());
        }
        Collections.sort(a);
        Collections.sort(b);
        assertEquals(a, b);
    }
}
