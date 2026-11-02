package com.tuxprogrammer.lostcities.worldgen.overlay;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Gives each wrapped world's {@link PlanPrefetcher} its share of every server tick, at the end of the tick, after
 * the chunks that tick generated (docs/design/0005-rwg-overlay.md §8, B4). {@code -Dlostcitiesnh.prefetch=false}
 * turns it off, for comparing budget runs; what is generated is the same either way.
 *
 * <p>
 * Registered on FML's bus, where tick events are posted. Server thread only. Side: server.
 */
public final class PrefetchTicker {

    /** JVM property that turns the prefetcher off when {@code false}. */
    public static final String PROPERTY = "lostcitiesnh.prefetch";

    private final Overlays overlays;

    private PrefetchTicker(Overlays overlays) {
        this.overlays = overlays;
    }

    /**
     * Subscribes the ticker unless the property turns it off.
     *
     * @param overlays the installer, whose worlds are ticked
     */
    static void register(Overlays overlays) {
        if (!Boolean.parseBoolean(System.getProperty(PROPERTY, "true"))) {
            return;
        }
        FMLCommonHandler.instance()
            .bus()
            .register(new PrefetchTicker(overlays));
    }

    /**
     * Runs the prefetchers once the tick's own work is done.
     *
     * @param event FML's server tick event
     */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        for (OverlayWorld overlay : overlays.all()) {
            overlay.prefetcher()
                .tick();
        }
    }
}
