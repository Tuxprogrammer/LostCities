package com.tuxprogrammer.lostcities.worldgen.overlay;

import java.util.Random;

import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;

import cpw.mods.fml.common.IWorldGenerator;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Extends the footprint write guard over the other mods' world generators (decision log, M05J rows; D75's finding): a
 * generator registered first pushes a tree frame, which drops logs, leaves, plants and vines written into a city's
 * footprint, and one registered last pops it. In the real pack IC2 rubber trees and HarvestCraft fruit trees, planted
 * on park grass by their {@code IWorldGenerator}s after our population phase, overhung streets and roofs; now their
 * crowns stop at the footprint, while GT's ore veins and every other block pass.
 *
 * <p>
 * Why two generators: FML runs every {@code IWorldGenerator} in {@code GameRegistry.generateWorld}, sorted by weight,
 * right after the chunk provider's {@code populate}, and fires no event around them
 * (cpw.mods.fml.common.registry.GameRegistry#generateWorld in Forge 10.13.4.1614: the list sorted once by weight,
 * lowest first). The lowest and highest weights bracket the others. A cascade inside the window pushes its own frames
 * and pops them, so the stack stays balanced; the closing generator pops only a tree frame of its own world.
 *
 * <p>
 * Server thread only. Side: server.
 */
public final class TreeGuard {

    /** Weight of the opening generator: before every other. */
    static final int FIRST = Integer.MIN_VALUE;
    /** Weight of the closing generator: after every other. */
    static final int LAST = Integer.MAX_VALUE;

    private TreeGuard() {}

    /**
     * Registers both generators.
     *
     * @param overlays the installer, which knows the wrapped worlds
     */
    public static void register(Overlays overlays) {
        GameRegistry.registerWorldGenerator(new Open(overlays), FIRST);
        GameRegistry.registerWorldGenerator(new Close(), LAST);
    }

    /** Pushes the tree frame of a wrapped world. */
    static final class Open implements IWorldGenerator {

        private final Overlays overlays;

        Open(Overlays overlays) {
            this.overlays = overlays;
        }

        @Override
        public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator,
            IChunkProvider chunkProvider) {
            OverlayWorld overlay = overlays.get(world);
            if (overlay != null) {
                FootprintGuard.pushTrees(world, overlay.plan());
            }
        }
    }

    /** Pops it. */
    static final class Close implements IWorldGenerator {

        @Override
        public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator,
            IChunkProvider chunkProvider) {
            if (FootprintGuard.topIsTrees(world)) {
                FootprintGuard.pop();
            }
        }
    }
}
