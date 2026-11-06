package com.tuxprogrammer.lostcities.worldgen.overlay;

import net.minecraft.world.World;

/**
 * The entry points of {@code MixinGtWorldgenStone}: a footprint frame around each GregTech stone-boulder generator, so
 * a boulder's replaced stone, cobblestone and stained clay stay a building's own where the footprint is. Kept apart
 * from the mixin because a mixin class cannot be called from tests.
 *
 * <p>
 * Server thread only. Side: server.
 */
public final class StoneGuard {

    private StoneGuard() {}

    /**
     * Opens the frame before a boulder generator runs, for a wrapped world.
     *
     * @param world the world the generator runs in
     */
    public static void open(World world) {
        if (world == null || world.isRemote) {
            return;
        }
        OverlayWorld overlay = Overlays.INSTANCE.get(world);
        if (overlay != null) {
            FootprintGuard.pushStone(world, overlay.plan());
        }
    }

    /**
     * Closes the frame {@link #open} pushed; nothing when there is none.
     *
     * @param world the world the generator ran in
     */
    public static void close(World world) {
        if (FootprintGuard.topIsStone(world)) {
            FootprintGuard.pop();
        }
    }
}
