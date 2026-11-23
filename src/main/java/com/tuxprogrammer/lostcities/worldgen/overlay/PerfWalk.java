package com.tuxprogrammer.lostcities.worldgen.overlay;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.gen.ChunkProviderServer;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.layout.perf.TickStats;
import com.tuxprogrammer.lostcities.scenario.Scenario;
import com.tuxprogrammer.lostcities.scenario.ScenarioContext;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Diagnostic scenario {@code perf.walk} (D-perf3-4): walks a player's load window across fresh ground along +Z from
 * spawn at sprinting speed and logs the server's tick times as {@code [LCNH-PERF] walk}. Manual-only, test hooks.
 *
 * <p>
 * Each tick the window's center moves {@code -Dlostcitiesnh.walkSpeed} (0.28) blocks. When it enters a new chunk row,
 * the row {@code -Dlostcitiesnh.walkRadius} (the server's view distance) chunks ahead is loaded through
 * {@code ChunkProviderServer.loadChunk}, all in that tick as the player manager does, and the row left behind is
 * queued for unloading. The loads go through the wrapped generator, so the prefetcher and the plan worker see them as
 * they see a player's. A tick is timed from a highest-priority start listener to a lowest-priority end listener
 * registered after the prefetcher's, so it holds the walk's loads and the prefetch tick and leaves out the wait for
 * the next tick, when the plan worker plans. The first {@code -Dlostcitiesnh.walkWarmup} (100) ticks are left out.
 * Hodgepodge's per-player generation throttle does not see this walk.
 */
public final class PerfWalk implements Scenario {

    static final int TICKS = Integer.getInteger("lostcitiesnh.walkTicks", 4800);
    static final double SPEED = Double.parseDouble(System.getProperty("lostcitiesnh.walkSpeed", "0.28"));
    static final int WARMUP = Integer.getInteger("lostcitiesnh.walkWarmup", 100);
    /** Chunks from spawn along +Z where the walk starts, past the spawn area generated at boot. */
    static final int START_OFFSET = 24;

    private final TickStats mspt = new TickStats(TICKS);
    private final TickStats interval = new TickStats(TICKS);
    private int radius;
    private int centerX;
    private double posZ;
    private int frontRow;
    private int ticks;
    private long tickStart;
    private long lastStart;
    private long loadNanos;
    private int loads;

    @Override
    public String name() {
        return "perf.walk";
    }

    @Override
    public boolean manualOnly() {
        return true;
    }

    @Override
    public int ticks() {
        return TICKS;
    }

    @Override
    public void setup(ScenarioContext context) {
        ChunkCoordinates spawn = context.world()
            .getSpawnPoint();
        radius = Integer.getInteger(
            "lostcitiesnh.walkRadius",
            MinecraftServer.getServer()
                .getConfigurationManager()
                .getViewDistance());
        centerX = spawn.posX >> 4;
        int startRow = (spawn.posZ >> 4) + START_OFFSET;
        posZ = startRow * 16.0 + 8.0;
        ChunkProviderServer server = context.world().theChunkProviderServer;
        for (int z = startRow - radius; z <= startRow + radius; z++) {
            for (int x = centerX - radius; x <= centerX + radius; x++) {
                server.loadChunk(x, z);
            }
        }
        frontRow = startRow + radius;
        FMLCommonHandler.instance()
            .bus()
            .register(this);
        LostCities.LOG.info(
            "[LCNH-PERF] walk from chunk ({}, {}) along +Z, {} ticks at {} blocks a tick, radius {}, planWorker={}",
            centerX,
            startRow,
            TICKS,
            SPEED,
            radius,
            PlanWorker.enabled());
    }

    @Override
    public void tick(ScenarioContext context) {
        ticks++;
        posZ += SPEED;
        int row = (int) Math.floor(posZ / 16.0);
        ChunkProviderServer server = context.world().theChunkProviderServer;
        long started = System.nanoTime();
        while (frontRow < row + radius) {
            frontRow++;
            int behind = frontRow - 2 * radius - 1;
            for (int x = centerX - radius; x <= centerX + radius; x++) {
                server.loadChunk(x, frontRow);
                server.unloadChunksIfNotNearSpawn(x, behind);
                loads++;
            }
        }
        loadNanos += System.nanoTime() - started;
    }

    /**
     * Stamps the start of a server tick.
     *
     * @param event FML's server tick
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onTickStart(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        tickStart = System.nanoTime();
        if (lastStart != 0L && ticks > WARMUP) {
            interval.add(tickStart - lastStart);
        }
        lastStart = tickStart;
    }

    /**
     * Records the tick once every other end-of-tick handler, the prefetcher's included, has run.
     *
     * @param event FML's server tick
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTickEnd(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && tickStart != 0L && ticks > WARMUP) {
            mspt.add(System.nanoTime() - tickStart);
        }
    }

    @Override
    public void verify(ScenarioContext context) {
        FMLCommonHandler.instance()
            .bus()
            .unregister(this);
        LostCities.LOG.info(
            "[LCNH-PERF] walk mspt {}; tick-to-tick {}; {} chunks loaded in {} ms; planWorker={}",
            mspt.summary(),
            interval.summary(),
            loads,
            loadNanos / 1_000_000L,
            PlanWorker.enabled());
        context.check(mspt.size() > 0, "expected timed ticks, got none");
    }
}
