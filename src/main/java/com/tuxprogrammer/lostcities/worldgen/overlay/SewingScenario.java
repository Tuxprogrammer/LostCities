package com.tuxprogrammer.lostcities.worldgen.overlay;

import java.util.Locale;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.compat.HeightOracles;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.layout.landmark.LaneRole;
import com.tuxprogrammer.lostcities.layout.landmark.Zone;
import com.tuxprogrammer.lostcities.scenario.Scenario;
import com.tuxprogrammer.lostcities.scenario.ScenarioContext;
import com.tuxprogrammer.lostcities.worldgen.city.CityFinder;
import com.tuxprogrammer.lostcities.worldgen.plan.LanePath;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainPhase;

/**
 * {@code city.sewing} (manual only): checks the sewing of a real VillageNames village into a real city on the real
 * pack (0006 §3, M05). It finds the village lane nearest chunk (−4096, 4096), where the oracle scenario found villages
 * on both world types (D90), generates the 21 × 21 chunks around it, and checks the 17 × 17 inside: no landmark core
 * written but for a lane's surface path, no building in an edge ring, and the lane paved. On RWG2 it does the same
 * around the nearest ocean monument's reserved lot: its cores are left as RWG made them. Prints {@code [LCNH-SEWING]}
 * lines with what it measured, and the viewpoints {@code /lostcities find} would give.
 *
 * <p>
 * Side: server.
 */
final class SewingScenario implements Scenario {

    /** Chunk the search starts from: where the oracle scenario found villages on seed 4242 (D90). */
    static final int START_CHUNK_X = -4096;
    static final int START_CHUNK_Z = 4096;
    /** How far the search for a sewn village and a monument reaches, in chunks. */
    static final int SEARCH = 96;
    /** Half-side of each generated region, in chunks. */
    static final int RADIUS = 10;

    private RegionGenerator village;
    private RegionGenerator monument;
    private CityFinder.Result lane;
    private CityFinder.Result reserved;
    private boolean done;

    @Override
    public String name() {
        return "city.sewing";
    }

    @Override
    public boolean manualOnly() {
        return true;
    }

    @Override
    public int ticks() {
        return 20 * 60 * 10;
    }

    @Override
    public boolean finished(ScenarioContext context) {
        return done;
    }

    @Override
    public void setup(ScenarioContext context) {
        OverlayWorld overlay = Overlays.INSTANCE.get(context.world());
        context.check(overlay != null && overlay.cities() != null, "expected a world pinned with real cities");
        CityFinder finder = new CityFinder(
            overlay.cities()
                .planner(),
            overlay.plan());
        int x = START_CHUNK_X << 4;
        int z = START_CHUNK_Z << 4;
        lane = finder.find(CityFinder.Kind.VILLAGE, x, z, SEARCH);
        context.check(lane != null, "expected a village sewn into a city within " + SEARCH + " chunks");
        LostCities.LOG.info("[LCNH-SEWING] village: {}", lane.format());
        int inner = RADIUS - 2;
        overlay.recorder()
            .record(lane.chunkX() - inner, lane.chunkX() + inner, lane.chunkZ() - inner, lane.chunkZ() + inner);
        village = new RegionGenerator(context.world(), lane.chunkX(), lane.chunkZ(), RADIUS);
        reserved = finder.find(
            CityFinder.Kind.RESERVED,
            x,
            z,
            SEARCH,
            found -> found.label()
                .contains("monument@"));
        if (reserved != null) {
            LostCities.LOG.info("[LCNH-SEWING] monument: {}", reserved.format());
            monument = new RegionGenerator(context.world(), reserved.chunkX(), reserved.chunkZ(), RADIUS);
        } else {
            LostCities.LOG.info("[LCNH-SEWING] monument: none within {} chunks", SEARCH);
        }
        for (CityFinder.Kind kind : new CityFinder.Kind[] { CityFinder.Kind.SOFTEDGE, CityFinder.Kind.RESERVED }) {
            CityFinder.Result found = finder.find(kind, lane.x(), lane.z(), RADIUS);
            LostCities.LOG.info("[LCNH-SEWING] {}: {}", kind.key(), found == null ? "none" : found.format());
        }
    }

    @Override
    public void tick(ScenarioContext context) {
        done = village.tick() && (monument == null || monument.tick());
    }

    @Override
    public void verify(ScenarioContext context) {
        context.check(done, "expected the regions generated");
        OverlayWorld overlay = Overlays.INSTANCE.get(context.world());
        overlay.recorder()
            .stop();
        Count sewing = count(context.world(), overlay, lane.chunkX(), lane.chunkZ());
        LostCities.LOG
            .info("[LCNH-SEWING] {} around the village: {}", HeightOracles.worldType(context.world()), sewing);
        context.check(sewing.cores > 0, "expected the village's core chunks");
        context.check(sewing.coresWritten == 0, "landmark cores written beyond a lane: " + sewing.coresWritten);
        context.check(sewing.edgeRingBuildings == 0, "buildings in an edge ring: " + sewing.edgeRingBuildings);
        context.check(sewing.lanePaved > 0, "expected lane columns paved, found none");
        if (reserved != null) {
            Count kept = count(context.world(), overlay, reserved.chunkX(), reserved.chunkZ());
            LostCities.LOG.info("[LCNH-SEWING] around the monument: {}", kept);
            context.check(kept.coresWritten == 0, "monument cores written: " + kept.coresWritten);
        }
    }

    /** What one region shows of the sewing. */
    static final class Count {

        int cores;
        int coresWritten;
        int coreLanes;
        int edgeRing;
        int edgeRingBuildings;
        int gates;
        int lanes;
        int links;
        int laneColumns;
        int lanePaved;

        @Override
        public String toString() {
            return String.format(
                Locale.ROOT,
                "core chunks %d (written beyond a lane %d, with a lane %d), edge-ring city chunks %d (with a building %d),"
                    + " gate %d, lane %d, link %d chunks, lane columns paved %d/%d",
                cores,
                coresWritten,
                coreLanes,
                edgeRing,
                edgeRingBuildings,
                gates,
                lanes,
                links,
                lanePaved,
                laneColumns);
        }
    }

    private static Count count(WorldServer world, OverlayWorld overlay, int centerX, int centerZ) {
        LayoutPlanner planner = overlay.cities()
            .planner();
        Count count = new Count();
        int inner = RADIUS - 2;
        for (int cz = centerZ - inner; cz <= centerZ + inner; cz++) {
            for (int cx = centerX - inner; cx <= centerX + inner; cx++) {
                ChunkPlan layout = planner.plan(cx, cz);
                com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan plan = overlay.plan()
                    .chunk(cx, cz);
                Zone zone = layout.zone()
                    .zone();
                LaneRole role = layout.lane()
                    .role();
                count.gates += role == LaneRole.GATE ? 1 : 0;
                count.lanes += role == LaneRole.LANE ? 1 : 0;
                count.links += role == LaneRole.LINK ? 1 : 0;
                if (zone == Zone.EDGE_RING && layout.isCity()) {
                    count.edgeRing++;
                    count.edgeRingBuildings += layout.hasBuilding() ? 1 : 0;
                }
                if (zone == Zone.CORE) {
                    count.cores++;
                    count.coreLanes += plan.lane() != null ? 1 : 0;
                    TerrainPhase.ChunkClass treated = overlay.recorder()
                        .chunkClasses()
                        .get(ChunkRecorder.key(cx, cz));
                    // A core is never city and never takes content (ChunkPlan rejects it); where the recorder saw
                    // the chunk generate, it must also have come out untouched unless it carries a lane.
                    if (!plan.isCore()
                        || plan.lane() == null && treated != null && treated != TerrainPhase.ChunkClass.UNTOUCHED) {
                        count.coresWritten++;
                    }
                }
                if (plan.lane() != null) {
                    paved(world.getChunkFromChunkCoords(cx, cz), plan.lane(), count);
                }
            }
        }
        return count;
    }

    /** Counts a chunk's lane columns whose surface is a path block VillageNames uses. */
    private static void paved(Chunk chunk, LanePath lane, Count count) {
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                if (!lane.contains(x, z)) {
                    continue;
                }
                count.laneColumns++;
                int top = chunk.getHeightValue(x, z) - 1;
                for (int y = Math.max(0, top); y >= Math.max(0, top - 2); y--) {
                    Block block = chunk.getBlock(x, y, z);
                    String name = String.valueOf(Block.blockRegistry.getNameForObject(block));
                    if (block == Blocks.gravel || block == Blocks.planks
                        || block == Blocks.cobblestone
                        || block == Blocks.sandstone
                        || block == Blocks.hardened_clay
                        || name.endsWith("grass_path")) {
                        count.lanePaved++;
                        break;
                    }
                }
            }
        }
    }
}
