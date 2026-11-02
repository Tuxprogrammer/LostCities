package com.tuxprogrammer.lostcities.core.api;

import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.worldgen.damage.DamageArea;
import com.tuxprogrammer.lostcities.worldgen.overlay.OverlayWorld;

/**
 * The API's view of a live world with real cities: its overlay's planner, assets, damage areas and pinned settings.
 *
 * <p>
 * Server thread only. Side: server.
 */
final class OverlaySource implements ApiSource {

    private final OverlayWorld overlay;

    /**
     * Creates the view.
     *
     * @param overlay the world's overlay, with real cities
     */
    OverlaySource(OverlayWorld overlay) {
        this.overlay = overlay;
    }

    @Override
    public int dimension() {
        return overlay.dimension();
    }

    @Override
    public String profile() {
        return overlay.snapshot()
            .profile();
    }

    @Override
    public int groundLevel() {
        return overlay.settings()
            .groundLevel();
    }

    @Override
    public float ruinChance() {
        return overlay.settings()
            .ruinChance();
    }

    @Override
    public LayoutPlanner planner() {
        return overlay.cities()
            .planner();
    }

    @Override
    public AssetSet assets() {
        return overlay.cities()
            .context()
            .assets();
    }

    @Override
    public DamageArea damage(int chunkX, int chunkZ) {
        return overlay.damageArea(chunkX, chunkZ);
    }

    /** The scattered structure whose footprint holds the chunk (07-scattered), from the pure plan. */
    @Override
    public String scattered(int chunkX, int chunkZ) {
        com.tuxprogrammer.lostcities.layout.ScatteredPlanner scattered = overlay.cities()
            .scattered();
        com.tuxprogrammer.lostcities.layout.ScatteredPlan plan = scattered == null ? null
            : scattered.planAt(chunkX, chunkZ);
        return plan == null ? null : plan.entry();
    }
}
