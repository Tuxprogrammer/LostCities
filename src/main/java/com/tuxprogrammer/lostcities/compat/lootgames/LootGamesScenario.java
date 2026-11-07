package com.tuxprogrammer.lostcities.compat.lootgames;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.compat.HeightOracles;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.layout.landmark.BlockRect;
import com.tuxprogrammer.lostcities.layout.landmark.Landmark;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.scenario.RegionJob;
import com.tuxprogrammer.lostcities.scenario.Scenario;
import com.tuxprogrammer.lostcities.scenario.ScenarioContext;
import com.tuxprogrammer.lostcities.scenario.ScenarioRegistry;
import com.tuxprogrammer.lostcities.worldgen.overlay.OverlayWorld;
import com.tuxprogrammer.lostcities.worldgen.overlay.Overlays;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * The {@code oracle.lootgames} scenario (manual only; docs/testing/README.md): compares the LootGames oracle with
 * LootGames itself on a world of the real pack with the city.
 *
 * <ol>
 * <li><b>Site test.</b> Over every chunk within {@value #SCAN_CHUNKS} chunks of the spawn, the oracle's site test must
 * agree with LootGames' own {@code canSpawnInChunk_v3}, called by reflection on its registered generator; the scan
 * must load no chunk.</li>
 * <li><b>Layout.</b> Every chunk a predicted room or tunnel touches must be a core of that landmark in the world's own
 * layout, never city.</li>
 * <li><b>Generate</b> the {@value #TARGETS} sites nearest the spawn, with their room and tunnel chunks populated, and
 * look for LootGames' master block at each room's center column: at least one room must be there, and no populated
 * chunk may hold a room's master block at a center the oracle did not predict.</li>
 * </ol>
 *
 * <p>
 * Side: server.
 */
public final class LootGamesScenario implements Scenario {

    /** Chunks scanned around the spawn, each way. */
    static final int SCAN_CHUNKS = 96;
    /** Sites to generate. */
    static final int TARGETS = 4;
    /** Generation time per tick. */
    static final long TICK_BUDGET_NANOS = 40_000_000L;
    /** Lowest and highest Y LootGames can put a master block: surfaces from 21 to 128, the block 7 under. */
    static final int MASTER_MIN_Y = 10;
    static final int MASTER_MAX_Y = 128;
    /** Mismatches named in a failure. */
    static final int EXAMPLES = 8;

    private LootGamesOracle oracle;
    private final List<Landmark> targets = new ArrayList<>();
    private final List<RegionJob> jobs = new ArrayList<>();
    private int jobIndex;
    private boolean done;

    /**
     * Adds the scenario to a registry.
     *
     * @param registry where to add it
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new LootGamesScenario());
    }

    @Override
    public String name() {
        return "oracle.lootgames";
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
    public void setup(ScenarioContext context) throws ReflectiveOperationException {
        targets.clear();
        jobs.clear();
        jobIndex = 0;
        done = false;
        WorldServer world = context.world();
        context.check(LootGamesOracles.isLoaded(), "expected LootGames loaded, found it absent");
        LootGamesSettings settings = LootGamesOracles.settings(world);
        context.check(settings.enabled(), "expected LootGames dungeons in the overworld, found " + settings);
        int chunksBefore = world.theChunkProviderServer.getLoadedChunkCount();
        StructureOracle built = LootGamesOracles.forWorld(world, HeightOracles.create(world), 2);
        context.check(built instanceof LootGamesOracle, "expected a LootGames oracle, found " + built);
        oracle = (LootGamesOracle) built;

        ChunkCoordinates spawn = world.getSpawnPoint();
        int centerX = spawn.posX >> 4;
        int centerZ = spawn.posZ >> 4;
        LootGamesSite theirs = new LootGamesSite();
        List<String> mismatches = new ArrayList<>();
        List<Landmark> sites = new ArrayList<>();
        for (int chunkX = centerX - SCAN_CHUNKS; chunkX <= centerX + SCAN_CHUNKS; chunkX++) {
            for (int chunkZ = centerZ - SCAN_CHUNKS; chunkZ <= centerZ + SCAN_CHUNKS; chunkZ++) {
                boolean ours = oracle.grid()
                    .spawnsIn(chunkX, chunkZ);
                if (ours != theirs.spawnsIn(chunkX, chunkZ, world) && mismatches.size() < EXAMPLES) {
                    mismatches.add(chunkX + "," + chunkZ + (ours ? " predicted" : " missed"));
                }
                if (ours) {
                    sites.add(oracle.dungeonAt(chunkX, chunkZ));
                }
            }
        }
        context.check(
            mismatches.isEmpty(),
            "expected the oracle's site test to agree with LootGames' own, differs at " + mismatches);
        context.check(
            world.theChunkProviderServer.getLoadedChunkCount() == chunksBefore,
            "expected the scan to load no chunk, loaded chunks went from " + chunksBefore
                + " to "
                + world.theChunkProviderServer.getLoadedChunkCount());

        LayoutPlanner planner = planner(context);
        int coreChunks = 0;
        int nearCity = 0;
        for (Landmark site : sites) {
            boolean touchesCity = false;
            for (BlockRect rect : site.footprint()) {
                for (int chunkX = rect.minChunkX(); chunkX <= rect.maxChunkX(); chunkX++) {
                    for (int chunkZ = rect.minChunkZ(); chunkZ <= rect.maxChunkZ(); chunkZ++) {
                        ChunkPlan plan = planner.plan(chunkX, chunkZ);
                        // D-landmark-policy-1: a LootGames room is not a landmark; the city builds over it.
                        context.check(
                            !site.id()
                                .equals(
                                    plan.zone()
                                        .landmarkId()),
                            "expected " + site.id()
                                + "'s chunk "
                                + chunkX
                                + ","
                                + chunkZ
                                + " no LootGames zone, found "
                                + plan.zone());
                        touchesCity |= planner.isCityRaw(chunkX, chunkZ);
                        coreChunks++;
                    }
                }
            }
            nearCity += touchesCity ? 1 : 0;
        }
        sites.sort(Comparator.comparingLong(site -> distanceSquared(site, spawn)));
        for (int i = 0; i < Math.min(TARGETS, sites.size()); i++) {
            Landmark target = sites.get(i);
            targets.add(target);
            BlockRect room = target.footprint()
                .get(0);
            BlockRect tunnel = target.footprint()
                .get(1);
            jobs.add(
                new RegionJob(
                    room.minChunkX() - 1,
                    room.minChunkZ() - 1,
                    room.maxChunkX() + 1,
                    tunnel.maxChunkZ() + 1,
                    room.minChunkX(),
                    room.minChunkZ(),
                    room.maxChunkX(),
                    tunnel.maxChunkZ()));
        }
        LostCities.LOG.info(
            "[LCNH-LG] scan {} chunks {}..{} x {}..{}: {} sites agree with LootGames, {} on city land, {} core chunks"
                + " checked; targets {}",
            HeightOracles.worldType(world),
            centerX - SCAN_CHUNKS,
            centerX + SCAN_CHUNKS,
            centerZ - SCAN_CHUNKS,
            centerZ + SCAN_CHUNKS,
            sites.size(),
            nearCity,
            coreChunks,
            targets);
        context.check(!targets.isEmpty(), "expected a LootGames site near the spawn, found none");
    }

    @Override
    public void tick(ScenarioContext context) {
        long start = System.nanoTime();
        while (jobIndex < jobs.size() && System.nanoTime() - start < TICK_BUDGET_NANOS) {
            if (jobs.get(jobIndex)
                .step(context.world().theChunkProviderServer, start, TICK_BUDGET_NANOS)) {
                jobIndex++;
            }
        }
        done = jobIndex >= jobs.size();
    }

    @Override
    public void verify(ScenarioContext context) {
        context.check(done, "expected every region generated, finished " + jobIndex + "/" + jobs.size());
        WorldServer world = context.world();
        int built = 0;
        List<String> unpredicted = new ArrayList<>();
        for (int i = 0; i < targets.size(); i++) {
            Landmark target = targets.get(i);
            BlockRect room = target.footprint()
                .get(0);
            int y = masterY(world, room.minX() + LootGamesOracle.ROOM_HALF, room.minZ() + LootGamesOracle.ROOM_HALF);
            LostCities.LOG.info("[LCNH-LG] {}: {}", target.id(), y < 0 ? "no room" : "room master block at y " + y);
            built += y < 0 ? 0 : 1;
            RegionJob job = jobs.get(i);
            for (int chunkX = room.minChunkX(); chunkX <= room.maxChunkX(); chunkX++) {
                for (int chunkZ = room.minChunkZ(); chunkZ <= target.footprint()
                    .get(1)
                    .maxChunkZ(); chunkZ++) {
                    if (!job.populates(chunkX, chunkZ) || oracle.grid()
                        .spawnsIn(chunkX, chunkZ)) {
                        continue;
                    }
                    int other = masterY(
                        world,
                        (chunkX << 4) + LootGamesOracle.CENTER_OFFSET,
                        (chunkZ << 4) + LootGamesOracle.CENTER_OFFSET);
                    if (other >= 0) {
                        unpredicted.add(chunkX + "," + chunkZ + " y " + other);
                    }
                }
            }
        }
        LostCities.LOG.info("[LCNH-LG] {} of {} predicted rooms built", built, targets.size());
        context.check(unpredicted.isEmpty(), "expected no room the oracle did not predict, found " + unpredicted);
        context.check(built > 0, "expected at least one predicted room built, none of " + targets + " was");
    }

    /** Returns the Y of LootGames' master block in a column, or -1. */
    private static int masterY(World world, int x, int z) {
        for (int y = MASTER_MIN_Y; y <= MASTER_MAX_Y; y++) {
            Block block = world.getBlock(x, y, z);
            GameRegistry.UniqueIdentifier name = GameRegistry.findUniqueIdentifierFor(block);
            if (name != null && LootGamesOracles.MOD_ID.equals(name.modId) && name.name.contains("MasterBlock")) {
                return y;
            }
        }
        return -1;
    }

    private static long distanceSquared(Landmark site, ChunkCoordinates from) {
        BlockRect room = site.footprint()
            .get(0);
        long dx = room.minX() + LootGamesOracle.ROOM_HALF - from.posX;
        long dz = room.minZ() + LootGamesOracle.ROOM_HALF - from.posZ;
        return dx * dx + dz * dz;
    }

    private static LayoutPlanner planner(ScenarioContext context) {
        OverlayWorld overlay = Overlays.INSTANCE.get(context.world());
        context.check(
            overlay != null && overlay.cities() != null,
            "expected the overlay with the layout plan in the overworld (world type "
                + HeightOracles.worldType(context.world())
                + "), found none");
        return overlay.cities()
            .planner();
    }

    /**
     * LootGames' own site test, {@code LootGamesWorldGen.canSpawnInChunk_v3} on the generator it registered
     * ({@code LootGamesLegacy.WorldGen}), reached by reflection: it is private, and calling it is the comparison this
     * test exists for. It reseeds only the generator's own {@code Random}.
     *
     * <p>
     * Source:
     * https://github.com/GTNewHorizons/LootGames/blob/2.2.14/src/main/java/eu/usrv/legacylootgames/LootGamesLegacy.java#L20
     * and #L37-L38.
     */
    static final class LootGamesSite {

        private final Object generator;
        private final Method test;

        LootGamesSite() throws ReflectiveOperationException {
            Class<?> legacy = Class.forName("eu.usrv.legacylootgames.LootGamesLegacy");
            Field field = legacy.getField("WorldGen");
            generator = field.get(null);
            test = generator.getClass()
                .getDeclaredMethod("canSpawnInChunk_v3", int.class, int.class, World.class);
            test.setAccessible(true);
        }

        boolean spawnsIn(int chunkX, int chunkZ, World world) throws ReflectiveOperationException {
            try {
                return (Boolean) test.invoke(generator, chunkX, chunkZ, world);
            } catch (InvocationTargetException e) {
                throw new IllegalStateException("LootGames' site test failed at " + chunkX + "," + chunkZ, e);
            }
        }
    }
}
