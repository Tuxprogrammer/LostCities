package com.tuxprogrammer.lostcities.compat.witchery;

import net.minecraft.world.World;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.layout.ScatteredFootprints;
import com.tuxprogrammer.lostcities.layout.landmark.Landmark;
import com.tuxprogrammer.lostcities.worldgen.city.LayoutCityPlan;
import com.tuxprogrammer.lostcities.worldgen.height.HeightProbe;
import com.tuxprogrammer.lostcities.worldgen.overlay.OverlayWorld;
import com.tuxprogrammer.lostcities.worldgen.overlay.Overlays;

/**
 * Skips Witchery's overworld structure for a chunk whose structure would stand on a city chunk or a scattered
 * structure's footprint (decision log D-landmark-policy-3): the city builds over Witchery's lots by default
 * ({@code IGNORE}), so a covens, shack, wicker man or hobgoblin hut must not then generate half inside a building.
 * Called by the late mixin {@code mixins.late.MixinWitcheryWorldGenerator} at the head of
 * {@code WitcheryWorldGenerator.generateOverworld}.
 *
 * <p>
 * Witchery's generator is an {@code IWorldGenerator} whose overworld pass does nothing but try its four structures at
 * the grid cell's one candidate chunk, building at block {@code (16 chunkX, 16 chunkZ)} plus the type's offsets
 * (Decompiled: witchery-1.7.10-0.24.1.jar (GTNH 2.9.0), {@code com.emoniph.witchery.worldgen.WitcheryWorldGenerator},
 * {@code generateOverworld} and {@code nonInRange}; reference copy
 * https://github.com/GTNH-Museum/Witchery/blob/4b7a98b60de7901e3503247016aa90c90099d0ce/src/main/java/com/emoniph/witchery/worldgen/WitcheryWorldGenerator.java#L80-L119).
 * Skipping that pass for a chunk therefore skips exactly that chunk's structure and nothing else. Every type's box in
 * every facing lies inside the union lot of {@link WitcherySettings#unionOffsets()}, blocks −9 to +13 from the chunk's
 * corner, so inside the chunk and its west, north and north-west neighbors: {@link #vetoes} tests those four chunks.
 *
 * <p>
 * Every input is the pure layout, so the outcome never depends on which chunks exist or the order they generate in. A
 * profile that sets {@code witchery=BUILD_AROUND} keeps the lot out of the city, and the veto never fires there.
 *
 * <p>
 * Server thread only. Side: server.
 */
public final class WitcheryVeto {

    /** What the veto reads from the layout. */
    public interface Layout {

        /**
         * Returns whether a chunk is a city chunk.
         *
         * @param chunkX chunk X
         * @param chunkZ chunk Z
         * @return {@code true} for a city chunk
         */
        boolean isCity(int chunkX, int chunkZ);

        /**
         * Returns whether a scattered structure's footprint touches a chunk.
         *
         * @param chunkX chunk X
         * @param chunkZ chunk Z
         * @return {@code true} when one does
         */
        boolean isScattered(int chunkX, int chunkZ);
    }

    private static long vetoed;

    private WitcheryVeto() {}

    /**
     * Returns whether a Witchery structure tried at a chunk would touch a city chunk or a scattered footprint: whether
     * any of the chunk and its west, north and north-west neighbors is one.
     *
     * @param chunkX the chunk Witchery populates
     * @param chunkZ the chunk Witchery populates
     * @param layout the layout
     * @return {@code true} to skip the structure
     */
    public static boolean vetoes(int chunkX, int chunkZ, Layout layout) {
        for (int x = chunkX - 1; x <= chunkX; x++) {
            for (int z = chunkZ - 1; z <= chunkZ; z++) {
                if (layout.isCity(x, z) || layout.isScattered(x, z)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Decides Witchery's overworld pass for a chunk of a world.
     *
     * @param world  the world
     * @param blockX the chunk's corner block X, as Witchery passes it
     * @param blockZ the chunk's corner block Z, as Witchery passes it
     * @return {@code true} to skip the pass; {@code false} for a world the overlay does not wrap
     */
    public static boolean skip(World world, int blockX, int blockZ) {
        OverlayWorld overlay = Overlays.INSTANCE.get(world);
        if (overlay == null) {
            return false;
        }
        int chunkX = blockX >> 4;
        int chunkZ = blockZ >> 4;
        try {
            if (!vetoes(chunkX, chunkZ, new WorldLayout(overlay))) {
                return false;
            }
            vetoed++;
            if (isPredictedSite(overlay, chunkX, chunkZ)) {
                LostCities.LOG.info(
                    "[LCNH-WITCHERY] skipped {} in dimension {}: it would stand on a city or scattered chunk",
                    WitcheryOracle.id(chunkX, chunkZ),
                    overlay.dimension());
            }
            return true;
        } catch (RuntimeException e) {
            if (HeightProbe.isProbe(e)) {
                throw e;
            }
            LostCities.LOG
                .error("[LCNH-WITCHERY] veto failed at chunk ({}, {}); Witchery decides alone", chunkX, chunkZ, e);
            return false;
        }
    }

    /** Returns how many Witchery passes were skipped since the server started, for the scenarios. */
    public static long vetoed() {
        return vetoed;
    }

    private static boolean isPredictedSite(OverlayWorld overlay, int chunkX, int chunkZ) {
        LayoutCityPlan cities = overlay.cities();
        if (cities == null) {
            return false;
        }
        String id = WitcheryOracle.id(chunkX, chunkZ);
        for (Landmark landmark : cities.structures()
            .landmarksTouching(chunkX, chunkZ, chunkX, chunkZ)) {
            if (id.equals(landmark.id())) {
                return true;
            }
        }
        return false;
    }

    /** A wrapped world's layout as the veto reads it. */
    static final class WorldLayout implements Layout {

        private final OverlayWorld overlay;
        private final ScatteredFootprints scattered;

        WorldLayout(OverlayWorld overlay) {
            this.overlay = overlay;
            this.scattered = overlay.scatteredFootprints();
        }

        @Override
        public boolean isCity(int chunkX, int chunkZ) {
            return overlay.plan()
                .chunk(chunkX, chunkZ)
                .isCity();
        }

        @Override
        public boolean isScattered(int chunkX, int chunkZ) {
            return scattered.touches(chunkX, chunkZ);
        }
    }
}
