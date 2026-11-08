package com.tuxprogrammer.lostcities.worldgen.overlay;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

import net.minecraft.world.World;
import net.minecraft.world.biome.WorldChunkManager;

/**
 * Which world inputs the calling thread plans with (D-plan-worker-2). The server thread plans with the world's own
 * biome provider; a world's plan worker plans with a private copy of it, built from the same seed, because RWG2's
 * {@code ChunkManagerRealistic} memoizes into unsynchronized maps ({@code biomeDataMap}, a Trove
 * {@code TLongObjectHashMap}, and vanilla's {@code BiomeCache}; RWG2 2.0.6,
 * https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/world/ChunkManagerRealistic.java).
 * The copy answers exactly as the world's provider does: both are {@code new ChunkManagerRealistic(seed, continental)},
 * as RWG2's world type builds its own.
 *
 * <p>
 * Every planning path that asks the biome provider asks {@link #manager(World)} instead of
 * {@code world.getWorldChunkManager()}, and an object that holds the provider is made per thread with
 * {@link #route(World, Object, Function)}. On every thread but a bound worker both return the world's own objects, so
 * nothing changes with the worker off.
 *
 * <p>
 * Side: server, and plain Java in tests.
 */
public final class PlanThreads {

    /** What a worker thread is bound to: one world, its private provider and the objects made over it. */
    private static final class Binding {

        final World world;
        final WorldChunkManager manager;
        final Map<Object, Object> routed = new IdentityHashMap<>();

        Binding(World world, WorldChunkManager manager) {
            this.world = world;
            this.manager = manager;
        }
    }

    private static final ThreadLocal<Binding> BOUND = new ThreadLocal<>();

    private PlanThreads() {}

    /**
     * Binds the calling thread, a plan worker, to its world's private provider.
     *
     * @param world   the world it plans
     * @param manager the worker's own provider, built from the world's seed
     */
    public static void bind(World world, WorldChunkManager manager) {
        BOUND.set(new Binding(world, manager));
    }

    /** Unbinds the calling thread. */
    public static void unbind() {
        BOUND.remove();
    }

    /**
     * Returns whether the calling thread is the plan worker of a world.
     *
     * @param world the world
     * @return {@code true} on that world's bound worker
     */
    public static boolean onWorker(World world) {
        Binding binding = BOUND.get();
        return binding != null && binding.world == world;
    }

    /**
     * Returns the biome provider the calling thread plans a world with.
     *
     * @param world the world
     * @return the worker's private provider on that world's worker, else {@code world.getWorldChunkManager()}
     */
    public static WorldChunkManager manager(World world) {
        Binding binding = BOUND.get();
        if (binding != null && binding.world == world) {
            return binding.manager;
        }
        return world.getWorldChunkManager();
    }

    /**
     * Returns the calling thread's copy of an object that holds a world's biome provider.
     *
     * @param world      the world
     * @param server     the object the server thread uses, built over the world's provider; also the key of the copy
     * @param overWorker builds the worker's copy over the worker's provider; run once per worker, on the worker
     * @param <T>        the object's type
     * @return the worker's copy on that world's worker, else {@code server}
     */
    public static <T> T route(World world, T server, Function<WorldChunkManager, T> overWorker) {
        Binding binding = BOUND.get();
        if (binding == null || binding.world != world) {
            return server;
        }
        @SuppressWarnings("unchecked")
        T copy = (T) binding.routed.computeIfAbsent(server, key -> overWorker.apply(binding.manager));
        return copy;
    }
}
