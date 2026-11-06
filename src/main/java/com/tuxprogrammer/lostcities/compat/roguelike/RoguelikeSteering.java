package com.tuxprogrammer.lostcities.compat.roguelike;

import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.layout.ScatteredFootprints;
import com.tuxprogrammer.lostcities.layout.landmark.Landmark;
import com.tuxprogrammer.lostcities.layout.landmark.Zone;
import com.tuxprogrammer.lostcities.layout.link.LinkRails;
import com.tuxprogrammer.lostcities.worldgen.city.LayoutCityPlan;
import com.tuxprogrammer.lostcities.worldgen.height.HeightProbe;
import com.tuxprogrammer.lostcities.worldgen.overlay.OverlayWorld;
import com.tuxprogrammer.lostcities.worldgen.overlay.Overlays;

/**
 * The world glue of the Roguelike steering mixin ({@code mixins.late.MixinRoguelikeDungeon}): answers
 * {@link SteeringRule} from a wrapped world's plan. A world the overlay does not wrap is never steered.
 *
 * <p>
 * Every answer comes from the layout, which is pure, so the steering never loads a chunk and never depends on the
 * order chunks generate in: city chunks from the cached plan, edge rings from the layout's landmark zones, scattered
 * footprints from {@link OverlayWorld#scatteredFootprints()}, predicted sites from the world's structure oracle, link
 * track from the railway layout's link lines (D361). A
 * layout failure is logged and leaves the point to Roguelike's own test, so a broken plan never stops a dungeon.
 *
 * <p>
 * Server thread only (Roguelike generates during population). Side: server.
 */
public final class RoguelikeSteering {

    private RoguelikeSteering() {}

    /**
     * Decides a candidate point of a dungeon in a dimension.
     *
     * @param dimension the dimension ID ({@code IWorldEditor.getDimension()})
     * @param blockX    the point's block X
     * @param blockZ    the point's block Z
     * @return the outcome; {@link SteeringRule.Outcome#CLEAR} for a world the overlay does not wrap
     */
    public static SteeringRule.Outcome decide(int dimension, int blockX, int blockZ) {
        WorldServer world = DimensionManager.getWorld(dimension);
        OverlayWorld overlay = world == null ? null : Overlays.INSTANCE.get(world);
        if (overlay == null) {
            return SteeringRule.Outcome.CLEAR;
        }
        try {
            return SteeringRule.decide(blockX, blockZ, new WorldLayout(overlay));
        } catch (RuntimeException e) {
            if (HeightProbe.isProbe(e)) {
                throw e;
            }
            LostCities.LOG.error(
                "[LCNH-RL] steering failed at ({}, {}) in dimension {}; Roguelike decides alone",
                blockX,
                blockZ,
                dimension,
                e);
            return SteeringRule.Outcome.CLEAR;
        }
    }

    /** A wrapped world's layout as the rule reads it. */
    static final class WorldLayout implements SteeringRule.Layout {

        private final OverlayWorld overlay;
        private final LayoutCityPlan cities;
        private final ScatteredFootprints scattered;

        WorldLayout(OverlayWorld overlay) {
            this.overlay = overlay;
            this.cities = overlay.cities();
            this.scattered = overlay.scatteredFootprints();
        }

        @Override
        public boolean isCity(int chunkX, int chunkZ) {
            return overlay.plan()
                .chunk(chunkX, chunkZ)
                .isCity();
        }

        @Override
        public boolean isEdgeRing(int chunkX, int chunkZ) {
            return cities != null && cities.planner()
                .zone(chunkX, chunkZ)
                .zone() == Zone.EDGE_RING;
        }

        @Override
        public boolean isScattered(int chunkX, int chunkZ) {
            return scattered.touches(chunkX, chunkZ);
        }

        @Override
        public boolean isPredictedSite(int blockX, int blockZ) {
            if (cities == null) {
                return false;
            }
            String id = RoguelikeOracle.id(blockX, blockZ);
            for (Landmark landmark : cities.structures()
                .landmarksTouching(blockX >> 4, blockZ >> 4, blockX >> 4, blockZ >> 4)) {
                if (id.equals(landmark.id())) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean hasLinkTrack(int chunkX, int chunkZ) {
            if (cities == null) {
                return false;
            }
            LinkRails lines = cities.planner()
                .networks()
                .railways()
                .links();
            return lines != null && lines.openings(chunkX, chunkZ) != 0;
        }
    }
}
