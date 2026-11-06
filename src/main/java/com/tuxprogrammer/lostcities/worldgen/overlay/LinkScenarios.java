package com.tuxprogrammer.lostcities.worldgen.overlay;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockRailBase;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.WorldServer;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.layout.Networks;
import com.tuxprogrammer.lostcities.layout.link.CityLinkGraph;
import com.tuxprogrammer.lostcities.layout.link.LinkHighways;
import com.tuxprogrammer.lostcities.layout.link.LinkRailGeometry;
import com.tuxprogrammer.lostcities.layout.link.LinkRails;
import com.tuxprogrammer.lostcities.layout.link.LinkRaster;
import com.tuxprogrammer.lostcities.layout.rail.RailChunk;
import com.tuxprogrammer.lostcities.scenario.ScenarioContext;

/**
 * The links branches of {@code network.highway} and {@code network.railway} ({@code networkLayout=links}, design
 * record 0015): the legacy scenarios read the hub planner and the railway grid, which a links world does not use.
 * <ul>
 * <li>{@link HighwayLeg}: the shortest written link near the spawn, walked block by block from its lower plaza to its
 * upper one along a lane beside the center line, each step's deck expected between the levels the link's profile gives
 * where the path enters and leaves the chunk;</li>
 * <li>{@link RailLeg}: the shortest live line near the spawn, followed from one station's center chunk to the other's
 * along {@link LinkRails#route}: every chunk opens on the sides the route crosses ({@code RailChunk.openings}), the
 * track meets across every chunk boundary, both ends are stations, and every T on the way has its switch lever.</li>
 * </ul>
 * Side: server (scenarios run on the server thread).
 */
final class LinkScenarios {

    /** Cells around the spawn's cell whose links are considered. */
    static final int CELLS = 1;
    /**
     * Blocks the walked lane lies beside the center line on a straight piece; a diagonal uses this less one per axis.
     */
    static final int LANE_OFFSET = 4;

    private LinkScenarios() {}

    /** The links near the spawn, each once. */
    private static List<CityLinkGraph.Link> nearSpawn(ScenarioContext context, CityLinkGraph graph) {
        ChunkCoordinates spawn = context.world()
            .getSpawnPoint();
        int cellX = Math.floorDiv(spawn.posX >> 4, graph.cell());
        int cellZ = Math.floorDiv(spawn.posZ >> 4, graph.cell());
        List<CityLinkGraph.Link> out = new ArrayList<>();
        for (int dz = -CELLS; dz <= CELLS; dz++) {
            for (int dx = -CELLS; dx <= CELLS; dx++) {
                for (CityLinkGraph.Link link : graph.links(cellX + dx, cellZ + dz)) {
                    if (!out.contains(link)) {
                        out.add(link);
                    }
                }
            }
        }
        return out;
    }

    /** A highway link to walk. */
    static final class HighwayLeg {

        private final CityLinkGraph.Link link;
        /** Chunk centers walked, in order, as {chunkX, chunkZ, inLevel, outLevel}. */
        private final List<int[]> chunks = new ArrayList<>();

        private HighwayLeg(CityLinkGraph.Link link, LinkHighways highways) {
            this.link = link;
            LinkHighways.Profile profile = highways.profile(link);
            List<LinkRaster.Step> path = link.path();
            for (int i = profile.start(); i <= profile.end(); i++) {
                LinkRaster.Step s = path.get(i);
                if (i > profile.start() && profile.staircase()) {
                    LinkRaster.Step p = path.get(i - 1);
                    if (s.chunkX() != p.chunkX() && s.chunkZ() != p.chunkZ()) {
                        // The staircase's flat bend in the side chunk, along X first.
                        int level = profile.outLevel(i - 1);
                        chunks.add(new int[] { s.chunkX(), p.chunkZ(), level, level });
                    }
                }
                chunks.add(new int[] { s.chunkX(), s.chunkZ(), profile.inLevel(i), profile.outLevel(i) });
            }
        }

        /**
         * Finds the written link with the fewest chunks near the spawn.
         *
         * @param context  the scenario
         * @param networks the networks, in links mode
         * @return the leg, or {@code null} when no link near the spawn is written
         */
        static HighwayLeg find(ScenarioContext context, Networks networks) {
            LinkHighways highways = networks.linkHighways();
            CityLinkGraph.Link best = null;
            int bestLength = Integer.MAX_VALUE;
            for (CityLinkGraph.Link link : nearSpawn(context, networks.links())) {
                LinkHighways.Profile profile = highways.profile(link);
                int length = profile.end() - profile.start();
                if (profile.written() && length >= 2 && length < bestLength) {
                    best = link;
                    bestLength = length;
                }
            }
            return best == null ? null : new HighwayLeg(best, highways);
        }

        /** The chunks to generate. */
        Set<Long> chunks() {
            Set<Long> out = new LinkedHashSet<>();
            for (int[] c : chunks) {
                out.add(NetworkScenarios.key(c[0], c[1]));
            }
            return out;
        }

        /**
         * Walks the deck and checks it as the legacy walk does: ground under every step, no step above half a block,
         * headroom over every step.
         *
         * @param context the scenario
         * @param overlay the overlay
         */
        void verify(ScenarioContext context, OverlayWorld overlay) {
            LayoutPlanner planner = overlay.cities()
                .planner();
            int ground = planner.profile().groundLevel;
            NetworkScenarios.Walk walk = new NetworkScenarios.Walk(context.world());
            int diagonals = 0;
            for (int k = 0; k + 1 < chunks.size(); k++) {
                int[] a = chunks.get(k);
                int[] b = chunks.get(k + 1);
                int dx = b[0] - a[0];
                int dz = b[1] - a[1];
                boolean diagonal = dx != 0 && dz != 0;
                diagonals += diagonal ? 1 : 0;
                int offset = diagonal ? LANE_OFFSET - 1 : LANE_OFFSET;
                int startX = (a[0] << 4) + 8 - dz * offset;
                int startZ = (a[1] << 4) + 8 + dx * offset;
                double midA = (a[2] + a[3]) / 2.0;
                double midB = (b[2] + b[3]) / 2.0;
                for (int t = 0; t < 16; t++) {
                    int x = startX + dx * t;
                    int z = startZ + dz * t;
                    double level = t < 8 ? midA + (a[3] - midA) * t / 8.0 : b[2] + (midB - b[2]) * (t - 8) / 8.0;
                    walk.exempt = overlay.damaged(x >> 4, z >> 4);
                    walk.step(x, z, (int) Math.round(ground + level * ChunkPlan.LEVEL_HEIGHT));
                }
            }
            LostCities.LOG.info(
                String.format(
                    Locale.ROOT,
                    "%slink %s: %d chunks (%d diagonal steps), %d steps walked (%d in chunks an explosion reaches),"
                        + " %d too high, %d without ground, %d without headroom",
                    NetworkScenarios.TAG,
                    link,
                    chunks.size(),
                    diagonals,
                    walk.steps,
                    walk.exempted,
                    walk.tooHigh,
                    walk.gaps,
                    walk.blocked));
            for (String example : walk.examples) {
                LostCities.LOG.info("{}  {}", NetworkScenarios.TAG, example);
            }
            context.check(walk.steps > 0, "expected steps on the deck");
            context.check(walk.gaps == 0, "expected ground under every step, found " + walk.gaps + " gaps");
            context.check(walk.tooHigh == 0, "expected no step above half a block, found " + walk.tooHigh);
            context.check(walk.blocked == 0, "expected headroom over every step, found " + walk.blocked + " blocked");
        }
    }

    /** A railway line to ride. */
    static final class RailLeg {

        private final CityLinkGraph.Link link;
        private final List<int[]> route;

        private RailLeg(CityLinkGraph.Link link, List<int[]> route) {
            this.link = link;
            this.route = route;
        }

        /**
         * Finds the live line with the shortest route near the spawn.
         *
         * @param context  the scenario
         * @param networks the networks, in links mode
         * @return the leg, or {@code null} when no line near the spawn has track
         */
        static RailLeg find(ScenarioContext context, Networks networks) {
            LinkRails rails = networks.railways()
                .links();
            RailLeg best = null;
            for (CityLinkGraph.Link link : nearSpawn(context, networks.links())) {
                if (!rails.live(link)) {
                    continue;
                }
                List<int[]> route = rails.route(link);
                if (best == null || route.size() < best.route.size()) {
                    best = new RailLeg(link, route);
                }
            }
            return best;
        }

        /** The chunks to generate. */
        Set<Long> chunks() {
            Set<Long> out = new LinkedHashSet<>();
            for (int[] c : route) {
                out.add(NetworkScenarios.key(c[0], c[1]));
            }
            return out;
        }

        /** A boundary's verdict: track meets across it, it does not, or an explosion reaches it (not judged). */
        enum Boundary {
            /** Track on both sides, meeting. */
            JOINED,
            /** No track, or track that does not meet. */
            BROKEN,
            /** Broken where an explosion reaches either chunk: upstream's damage, counted, not judged (D-p4v-5). */
            DAMAGED
        }

        /**
         * Judges one boundary of a line from the rails in the two facing columns.
         *
         * @param out     the rails in this chunk's boundary column, as {across, y}
         * @param in      the rails in the neighbor's facing column
         * @param damaged whether an explosion reaches either chunk
         * @return the verdict
         */
        static Boundary judge(List<int[]> out, List<int[]> in, boolean damaged) {
            boolean joined = !out.isEmpty() && NetworkScenarios.RailwayRide.matches(out, in)
                && NetworkScenarios.RailwayRide.matches(in, out);
            if (joined) {
                return Boundary.JOINED;
            }
            return damaged ? Boundary.DAMAGED : Boundary.BROKEN;
        }

        /**
         * Follows the line from one station to the other. A boundary an explosion reaches is counted, not judged, as
         * the highway walk and {@code city.walk} treat damage (D-p4v-5).
         *
         * @param context the scenario
         * @param overlay the overlay
         */
        void verify(ScenarioContext context, OverlayWorld overlay) {
            Networks networks = overlay.cities()
                .planner()
                .networks();
            WorldServer world = context.world();
            int broken = 0;
            int damaged = 0;
            int unopened = 0;
            int tees = 0;
            int levers = 0;
            List<String> examples = new ArrayList<>();
            for (int i = 0; i < route.size(); i++) {
                int[] c = route.get(i);
                RailChunk rail = networks.railChunk(c[0], c[1]);
                int sides = rail.openings() & LinkRails.SIDES;
                if (i + 1 < route.size()) {
                    int[] n = route.get(i + 1);
                    int side = LinkRails.side(n[0] - c[0], n[1] - c[1]);
                    int back = LinkRails.side(c[0] - n[0], c[1] - n[1]);
                    if ((sides & side) == 0 || (networks.railChunk(n[0], n[1])
                        .openings() & back) == 0) {
                        unopened++;
                    }
                    List<int[]> out = railsAcross(world, c[0], c[1], n[0] - c[0], n[1] - c[1], false);
                    List<int[]> in = railsAcross(world, c[0], c[1], n[0] - c[0], n[1] - c[1], true);
                    Boundary verdict = judge(
                        out,
                        in,
                        overlay.damaged(c[0], c[1]) || overlay.damaged(n[0], n[1]));
                    if (verdict == Boundary.DAMAGED) {
                        damaged++;
                    } else if (verdict == Boundary.BROKEN) {
                        broken++;
                    }
                    if (verdict != Boundary.JOINED) {
                        if (examples.size() < 6) {
                            examples.add(
                                (verdict == Boundary.DAMAGED ? "(explosion) " : "") + "chunk " + c[0]
                                    + ","
                                    + c[1]
                                    + " to "
                                    + n[0]
                                    + ","
                                    + n[1]
                                    + ": "
                                    + NetworkScenarios.RailwayRide.describe(out)
                                    + " | "
                                    + NetworkScenarios.RailwayRide.describe(in));
                        }
                    }
                }
                int[] lever = LinkRailGeometry.lever(sides);
                if (lever != null && !rail.type()
                    .isStation()) {
                    tees++;
                    for (int y = 1; y < 200; y++) {
                        if (world.getBlock((c[0] << 4) + lever[0], y, (c[1] << 4) + lever[1]) instanceof BlockLever) {
                            levers++;
                            break;
                        }
                    }
                }
            }
            int[] first = route.get(0);
            int[] last = route.get(route.size() - 1);
            boolean stations = networks.railChunk(first[0], first[1])
                .type()
                .isStation()
                && networks.railChunk(last[0], last[1])
                    .type()
                    .isStation();
            LostCities.LOG.info(
                String.format(
                    Locale.ROOT,
                    "%sline %s: %d chunks, %d boundaries, %d broken, %d broken where an explosion reaches,"
                        + " %d not opened, stations at both ends %s, T junctions %d with %d levers",
                    NetworkScenarios.TAG,
                    link,
                    route.size(),
                    route.size() - 1,
                    broken,
                    damaged,
                    unopened,
                    stations,
                    tees,
                    levers));
            for (String example : examples) {
                LostCities.LOG.info("{}  {}", NetworkScenarios.TAG, example);
            }
            context.check(stations, "expected a station at both ends of the line");
            context.check(unopened == 0, "expected every chunk to open toward the next, " + unopened + " not");
            context.check(broken == 0, "expected every track to continue into the next chunk, " + broken + " broken");
            context.check(levers == tees, "expected a switch lever at every T, " + levers + " of " + tees);
        }

        /**
         * The rails in the boundary column of chunk {@code (cx, cz)} toward a side neighbor (or, with {@code beyond},
         * in the neighbor's facing column), as {across, y} pairs.
         */
        private static List<int[]> railsAcross(WorldServer world, int cx, int cz, int dx, int dz, boolean beyond) {
            List<int[]> out = new ArrayList<>();
            for (int a = 0; a < 16; a++) {
                int x;
                int z;
                if (dx != 0) {
                    x = (cx << 4) + (dx > 0 ? 15 : 0) + (beyond ? dx : 0);
                    z = (cz << 4) + a;
                } else {
                    x = (cx << 4) + a;
                    z = (cz << 4) + (dz > 0 ? 15 : 0) + (beyond ? dz : 0);
                }
                for (int y = 1; y < 200; y++) {
                    if (world.getBlock(x, y, z) instanceof BlockRailBase) {
                        out.add(new int[] { a, y });
                    }
                }
            }
            return out;
        }
    }
}
