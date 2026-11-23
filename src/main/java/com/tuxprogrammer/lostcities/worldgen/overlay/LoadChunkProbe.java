package com.tuxprogrammer.lostcities.worldgen.overlay;

import java.util.Random;

import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkEvent;

import com.tuxprogrammer.lostcities.layout.perf.PerfCounters;
import com.tuxprogrammer.lostcities.layout.perf.PerfSpans;

import cpw.mods.fml.common.IWorldGenerator;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Names the parts of a chunk load that the generation timers leave out (D-perf3-1), as {@link PerfSpans}:
 *
 * <ul>
 * <li>{@code span.worldGenerators}: every {@code IWorldGenerator} FML runs for a chunk ({@code GameRegistry
 * .generateWorld}, called by {@code ChunkProviderServer.populate} right after the generator's own populate: GT's ores
 * and most mods' structures). A generator of the lowest possible weight opens the span and one of the highest closes
 * it, since FML runs them in ascending weight;</li>
 * <li>{@code span.chunkLoadEvent}: the {@code ChunkEvent.Load} handlers, between a highest- and a lowest-priority
 * listener.</li>
 * </ul>
 *
 * <p>
 * Registered only when the perf counters are on (test hooks or {@code -Dlostcitiesnh.perfCounters=true}), so a normal
 * server registers nothing. FML reseeds the random it hands each generator from the world seed and chunk, so these
 * two generators change no other generator's output; they write nothing. Server thread only: the client's chunk
 * events are ignored. Side: server.
 */
final class LoadChunkProbe {

    /** Span of the {@code IWorldGenerator}s. */
    static final String WORLD_GENERATORS = "worldGenerators";
    /** Span of the {@code ChunkEvent.Load} handlers. */
    static final String CHUNK_LOAD_EVENT = "chunkLoadEvent";

    private LoadChunkProbe() {}

    /** Registers the probe when the perf counters are on; otherwise does nothing. */
    static void register() {
        if (!PerfCounters.ENABLED) {
            return;
        }
        GameRegistry.registerWorldGenerator(new Edge(true), Integer.MIN_VALUE);
        GameRegistry.registerWorldGenerator(new Edge(false), Integer.MAX_VALUE);
        MinecraftForge.EVENT_BUS.register(new LoadEvents());
    }

    /** The first or last {@code IWorldGenerator}: opens or closes {@link #WORLD_GENERATORS}. */
    static final class Edge implements IWorldGenerator {

        private final boolean first;

        Edge(boolean first) {
            this.first = first;
        }

        @Override
        public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider generator,
            IChunkProvider provider) {
            if (world.isRemote) {
                return;
            }
            if (first) {
                PerfSpans.enter(WORLD_GENERATORS);
            } else {
                PerfSpans.exit(WORLD_GENERATORS);
            }
        }
    }

    /** The highest- and lowest-priority {@code ChunkEvent.Load} listeners. */
    public static final class LoadEvents {

        /**
         * Opens the span before every other handler.
         *
         * @param event Forge's chunk-load event
         */
        @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
        public void onLoadFirst(ChunkEvent.Load event) {
            if (!event.world.isRemote) {
                PerfSpans.enter(CHUNK_LOAD_EVENT);
            }
        }

        /**
         * Closes the span after every other handler.
         *
         * @param event Forge's chunk-load event
         */
        @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
        public void onLoadLast(ChunkEvent.Load event) {
            if (!event.world.isRemote) {
                PerfSpans.exit(CHUNK_LOAD_EVENT);
            }
        }
    }
}
