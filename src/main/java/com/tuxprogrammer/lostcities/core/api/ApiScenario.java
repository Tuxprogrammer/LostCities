package com.tuxprogrammer.lostcities.core.api;

import java.util.Set;
import java.util.TreeSet;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.api.ILostChunkInfo;
import com.tuxprogrammer.lostcities.api.ILostCityInformation;
import com.tuxprogrammer.lostcities.api.LostChunkType;
import com.tuxprogrammer.lostcities.api.LostCitiesAPI;
import com.tuxprogrammer.lostcities.api.event.LostCityEvent;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.ChunkSpiral;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.scenario.Scenario;
import com.tuxprogrammer.lostcities.scenario.ScenarioContext;
import com.tuxprogrammer.lostcities.scenario.ScenarioRegistry;
import com.tuxprogrammer.lostcities.worldgen.overlay.OverlayWorld;
import com.tuxprogrammer.lostcities.worldgen.overlay.Overlays;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * {@code api.live}: the public API in a live world (docs/api.md; docs/testing/README.md). It asks
 * {@code LostCitiesAPI.get().getLostInfo} for the overworld, compares a building chunk's answers with the layout,
 * then listens to the four generation events while it generates fresh chunks 128 chunks south of its arena (a fresh
 * world is needed, so they are not generated yet): every event fires, a listener reads the chunk's blocks, and
 * cancelling {@code PreGenCityChunk} leaves that chunk without its building.
 * Each fact goes to the log as {@code [LCNH-API]}.
 *
 * <p>
 * Side: server.
 */
public final class ApiScenario implements Scenario {

    /** Prefix of the log lines. */
    static final String TAG = "[LCNH-API] ";
    /** How far the scenario looks for chunks to generate, in chunks. */
    static final int SEARCH_RADIUS = 48;
    /** How far from the arena row the scenario looks, in chunks along Z. */
    static final int OFFSET_Z = 128;
    /** Blocks above a building chunk's ground, in its middle column band, that tell a building from none. */
    static final int BAND = 18;

    /**
     * Registers the scenario.
     *
     * @param registry the registry
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new ApiScenario());
    }

    /** Counts the events of a few chunks, and cancels the building of one. Its handlers are public for FML's bus. */
    public static final class Probe {

        final Set<String> fired = new TreeSet<>();
        final long cancelled;
        int blocksRead;

        Probe(long cancelled) {
            this.cancelled = cancelled;
        }

        private void note(String kind, LostCityEvent event) {
            fired.add(kind + "@" + event.getChunkX() + "," + event.getChunkZ());
        }

        /**
         * Notes the event and cancels it for the chosen chunk.
         *
         * @param event the event
         */
        @SubscribeEvent
        public void preCity(LostCityEvent.PreGenCityChunk event) {
            note("preCity", event);
            if (key(event.getChunkX(), event.getChunkZ()) == cancelled) {
                event.setCanceled(true);
            }
        }

        /**
         * Notes the event and reads a block through it.
         *
         * @param event the event
         */
        @SubscribeEvent
        public void postCity(LostCityEvent.PostGenCityChunk event) {
            note("postCity", event);
            Block block = event.getBlocks()
                .getBlock(8, 1, 8);
            if (block != null && block != Blocks.air) {
                blocksRead++;
            }
        }

        /**
         * Notes the event.
         *
         * @param event the event
         */
        @SubscribeEvent
        public void postOutside(LostCityEvent.PostGenOutsideChunk event) {
            note("postOutside", event);
        }

        /**
         * Notes the event.
         *
         * @param event the event
         */
        @SubscribeEvent
        public void preExplosion(LostCityEvent.PreExplosion event) {
            note("preExplosion", event);
        }
    }

    static long key(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
    }

    @Override
    public String name() {
        return "api.live";
    }

    @Override
    public int ticks() {
        return 0;
    }

    @Override
    public void setup(ScenarioContext context) {}

    @Override
    public void verify(ScenarioContext context) {
        WorldServer world = context.world();
        OverlayWorld overlay = Overlays.INSTANCE.get(world);
        ILostCityInformation info = LostCitiesAPI.get()
            .getLostInfo(world);
        if (overlay == null || overlay.cities() == null) {
            context.check(info == null, "a world without real cities must answer null");
            LostCities.LOG.info("{}dimension 0 has no real cities: getLostInfo answers null", TAG);
            return;
        }
        context.check(info != null, "getLostInfo answered null for a world with cities");
        context.check(
            info.getDimension() == 0 && info.getProfile()
                .equals(
                    overlay.snapshot()
                        .profile()),
            "getLostInfo describes another world");
        LayoutPlanner planner = overlay.cities()
            .planner();
        // Well away from every scenario's arena row, so on a fresh world these chunks are not generated yet.
        int centerX = context.arenaChunkX() + ScenarioContext.ARENA_CHUNKS / 2;
        int centerZ = context.arenaChunkZ() + OFFSET_Z;
        int[] cancelled = fresh(world, planner, centerX, centerZ, null, true);
        context.check(cancelled != null, "no ungenerated building chunk near the arena");
        int[] kept = fresh(world, planner, centerX, centerZ, cancelled, true);
        int[] outside = fresh(world, planner, centerX, centerZ, null, false);
        context.check(kept != null && outside != null, "no second building or outside chunk near the arena");

        ChunkPlan plan = planner.plan(kept[0], kept[1]);
        ILostChunkInfo chunk = info.getChunkInfo(kept[0], kept[1]);
        context.check(
            chunk.isCity() && chunk.getType() == LostChunkType.BUILDING
                && plan.building()
                    .equals(chunk.getBuildingType())
                && chunk.getNumFloors() == plan.floors()
                && chunk.getCityGroundLevel() == plan.cityGroundLevel(),
            "the API's answer for chunk " + kept[0] + "," + kept[1] + " differs from the layout");

        Probe probe = new Probe(key(cancelled[0], cancelled[1]));
        MinecraftForge.EVENT_BUS.register(probe);
        try {
            for (int[] target : new int[][] { cancelled, kept, outside }) {
                // loadChunk generates; a provide request for an unloaded chunk may answer an empty chunk instead.
                world.theChunkProviderServer.loadChunk(target[0], target[1]);
            }
        } finally {
            MinecraftForge.EVENT_BUS.unregister(probe);
        }
        LostCities.LOG.info(
            "{}events {} blocksRead={} cancelled={},{} kept={},{} outside={},{}",
            TAG,
            probe.fired,
            probe.blocksRead,
            cancelled[0],
            cancelled[1],
            kept[0],
            kept[1],
            outside[0],
            outside[1]);
        for (String expected : new String[] { "preCity@" + kept[0] + "," + kept[1],
            "postCity@" + kept[0] + "," + kept[1], "preCity@" + cancelled[0] + "," + cancelled[1],
            "postOutside@" + outside[0] + "," + outside[1], "preExplosion@" + kept[0] + "," + kept[1] }) {
            context.check(probe.fired.contains(expected), "event " + expected + " did not fire: " + probe.fired);
        }
        context.check(probe.blocksRead > 0, "a listener could not read the chunk's blocks");

        int keptBlocks = band(world, kept, planner.plan(kept[0], kept[1]));
        int cancelledBlocks = band(world, cancelled, planner.plan(cancelled[0], cancelled[1]));
        LostCities.LOG.info("{}building blocks kept={} cancelled={}", TAG, keptBlocks, cancelledBlocks);
        context.check(
            keptBlocks > 4 * Math.max(1, cancelledBlocks),
            "cancelling PreGenCityChunk should leave the chunk without its building: " + cancelledBlocks
                + " blocks there, "
                + keptBlocks
                + " in a kept building");
    }

    /** Counts the solid blocks of a chunk's middle 8 × 8 columns from 2 to {@link #BAND} blocks above its ground. */
    private static int band(WorldServer world, int[] chunk, ChunkPlan plan) {
        int ground = plan.cityGroundLevel();
        int count = 0;
        for (int y = ground + 2; y < ground + BAND; y++) {
            for (int z = 4; z < 12; z++) {
                for (int x = 4; x < 12; x++) {
                    if (!world.isAirBlock((chunk[0] << 4) + x, y, (chunk[1] << 4) + z)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /**
     * Returns the nearest ungenerated chunk whose plan has a single building of at least two floors, or no city; none
     * of whose neighbors is generated either, so its terrain phase runs now.
     */
    private static int[] fresh(WorldServer world, LayoutPlanner planner, int centerX, int centerZ, int[] not,
        boolean building) {
        int[][] found = new int[1][];
        ChunkSpiral.walk(centerX, centerZ, SEARCH_RADIUS, (x, z) -> {
            if (not != null && not[0] == x && not[1] == z) {
                return true;
            }
            ChunkPlan plan = planner.plan(x, z);
            boolean wanted = building ? plan.hasBuilding() && plan.multiPos()
                .isSingle() && plan.floors() >= 2 && plan.ruinHeight() < 0
                : !plan.isCity() && plan.zone()
                    .zone() == com.tuxprogrammer.lostcities.layout.landmark.Zone.NONE;
            if (!wanted || world.theChunkProviderServer.chunkExists(x, z)) {
                return true;
            }
            found[0] = new int[] { x, z };
            return false;
        });
        return found[0];
    }
}
