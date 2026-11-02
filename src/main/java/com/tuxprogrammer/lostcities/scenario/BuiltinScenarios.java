package com.tuxprogrammer.lostcities.scenario;

import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

/**
 * The harness's own scenarios, which prove the harness itself works on a fresh world (docs/ROADMAP.md, M01 exit gate).
 * Feature scenarios arrive with their features and are listed in docs/testing/README.md.
 *
 * <p>
 * Side: server.
 */
public final class BuiltinScenarios {

    /** Ticks {@code harness.trivial} waits. */
    static final int TRIVIAL_TICKS = 20;

    private BuiltinScenarios() {}

    /**
     * Adds the built-in scenarios to a registry.
     *
     * @param registry where to add them
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Trivial());
        registry.register(new FreshChunk());
        registry.register(new MustFail());
    }

    /** The overworld is loaded and ticking: the runner reaches setup, ticks and verify. */
    static final class Trivial implements Scenario {

        private long startTime;

        @Override
        public String name() {
            return "harness.trivial";
        }

        @Override
        public int ticks() {
            return TRIVIAL_TICKS;
        }

        @Override
        public void setup(ScenarioContext context) {
            WorldServer world = context.world();
            context.check(world != null, "expected the overworld to be loaded, found none");
            startTime = world.getTotalWorldTime();
        }

        @Override
        public void verify(ScenarioContext context) {
            long elapsed = context.world()
                .getTotalWorldTime() - startTime;
            context.check(
                context.ticksRun() == TRIVIAL_TICKS,
                "expected " + TRIVIAL_TICKS + " ticks, ran " + context.ticksRun());
            context.check(elapsed > 0, "expected world time to advance, it advanced " + elapsed);
        }
    }

    /**
     * The arena's corner chunk can be generated and loaded on demand, outside any generation step, and holds terrain.
     * Worldgen scenarios (M04) build on this.
     */
    static final class FreshChunk implements Scenario {

        @Override
        public String name() {
            return "harness.fresh_chunk";
        }

        @Override
        public int ticks() {
            return 0;
        }

        @Override
        public void setup(ScenarioContext context) {
            Chunk chunk = context.world().theChunkProviderServer
                .loadChunk(context.arenaChunkX(), context.arenaChunkZ());
            context.check(chunk != null, "expected a chunk at the arena corner, got none");
            context.check(
                chunk.xPosition == context.arenaChunkX() && chunk.zPosition == context.arenaChunkZ(),
                "expected chunk (" + context.arenaChunkX()
                    + ", "
                    + context.arenaChunkZ()
                    + "), got ("
                    + chunk.xPosition
                    + ", "
                    + chunk.zPosition
                    + ")");
        }

        @Override
        public void verify(ScenarioContext context) {
            Chunk chunk = context.world()
                .getChunkFromChunkCoords(context.arenaChunkX(), context.arenaChunkZ());
            int filledSections = 0;
            for (ExtendedBlockStorage section : chunk.getBlockStorageArray()) {
                if (section != null && !section.isEmpty()) {
                    filledSections++;
                }
            }
            context.check(
                !chunk.isEmpty() && filledSections > 0,
                "expected generated terrain in the arena corner chunk, found " + filledSections + " filled sections");
        }
    }

    /**
     * Fails on purpose, so the FAIL path of the report can be seen working. Manual-only: {@code all} never runs it.
     */
    static final class MustFail implements Scenario {

        @Override
        public String name() {
            return "harness.must_fail";
        }

        @Override
        public int ticks() {
            return 0;
        }

        @Override
        public boolean manualOnly() {
            return true;
        }

        @Override
        public void setup(ScenarioContext context) {}

        @Override
        public void verify(ScenarioContext context) {
            context.check(false, "deliberate failure: proves that FAIL lines and the summary count reach the log");
        }
    }
}
