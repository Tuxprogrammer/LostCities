package com.tuxprogrammer.lostcities.compat.villagenames;

import net.minecraft.world.World;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.compat.HeightOracles;
import com.tuxprogrammer.lostcities.layout.LayoutProfile;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.worldgen.height.TerrainHeightOracle;

import cpw.mods.fml.common.Loader;

/**
 * The VillageNames structure oracles of a world, for the layout (0006 §2; decision log D27, D32): its villages
 * ({@link VillageOracle}) and its ocean monuments ({@link MonumentOracle}) as one {@link StructureOracle}.
 *
 * <p>
 * Guarantees, for the World lane that wires it in:
 * <ul>
 * <li><b>Pure.</b> Answers depend only on the world seed, VillageNames' config (and RWG's "Generate Villages"), the
 * world's biome provider and the height oracle. No chunk is loaded or generated, no saved data is read, and
 * {@code world.rand} is never touched: the grid seeds are computed on {@code Random}s of the oracle's own.</li>
 * <li><b>Order-independent and complete</b>, as {@link StructureOracle} requires; landmarks come back sorted by
 * identity, {@code village@<chunkX>,<chunkZ>} and {@code monument@<chunkX>,<chunkZ>} (start chunks).</li>
 * <li><b>Villages:</b> certainty {@code EXACT}. The footprint is every piece's box, as VillageNames' own start builds
 * it; gates are the path ends that face the outside ({@link VillageGates}); the ground estimate is the height
 * oracle's median over the buildings.</li>
 * <li><b>Monuments:</b> certainty {@code EXACT}; the 58 × 58 building; the ground estimate is the sea floor's median.
 * Overworld only.</li>
 * <li><b>Bounded:</b> each oracle caches {@value VillageOracle#CACHE_SIZE} start chunks; the caches belong to the
 * returned object, so dropping it with the world frees them.</li>
 * <li><b>Empty</b> ({@link StructureOracle#NONE}) when VillageNames is not loaded, or when neither its village
 * generator nor its monuments generate in this world.</li>
 * </ul>
 * Classes that touch VillageNames load only after {@code Loader.isModLoaded("VillageNames")}, so this class loads
 * without it. Server thread only.
 *
 * <p>
 * Spec: docs/new-spec/00-pack-integration/coexistence.md § Villages; docs/design/0006-build-around-other-worldgen.md
 * §2. Side: server.
 */
public final class VillageNamesOracles {

    /** VillageNames' mod ID. */
    public static final String MOD_ID = "VillageNames";

    private VillageNamesOracles() {}

    /** Returns whether VillageNames is loaded. */
    public static boolean isLoaded() {
        return Loader.isModLoaded(MOD_ID);
    }

    /**
     * Pins FML's village-creation handler order, when VillageNames is loaded ({@link VillageHandlerOrder}). Called
     * once from {@code postInit}, before any world exists.
     */
    public static void init() {
        if (!isLoaded()) {
            return;
        }
        int handlers = VillageHandlerOrder.pin();
        if (handlers >= 0) {
            LostCities.LOG.info("[LCNH-VN] village-creation handlers ordered by class name ({})", handlers);
        }
    }

    /**
     * Returns the oracle of a world, building its own height oracle ({@link HeightOracles#create}) for the ground
     * estimates and the default {@code landmarkMargin} as the gates' clearance. On classic RWG that height oracle is a
     * second generator; the World lane, which already holds one, should call
     * {@link #forWorld(World, TerrainHeightOracle, int)}.
     *
     * @param world the world, whose generator already exists
     * @return the oracle; {@link StructureOracle#NONE} when VillageNames generates nothing here
     */
    public static StructureOracle forWorld(World world) {
        if (!isLoaded()) {
            return StructureOracle.NONE;
        }
        return forWorld(world, HeightOracles.create(world), LayoutProfile.defaults().landmarkMargin);
    }

    /**
     * Returns the oracle of a world.
     *
     * @param world          the world, whose generator already exists
     * @param heights        the world's height oracle, or {@code null} to estimate every ground at Y
     *                       {@value GroundEstimates#FALLBACK_Y}
     * @param landmarkMargin the profile's {@code landmarkMargin}, in blocks: the clearance a lane keeps from village
     *                       pieces, which decides which path ends are gates
     * @return the oracle; {@link StructureOracle#NONE} when VillageNames generates nothing here
     */
    public static StructureOracle forWorld(World world, TerrainHeightOracle heights, int landmarkMargin) {
        if (!isLoaded()) {
            return StructureOracle.NONE;
        }
        return Present.create(world, heights, landmarkMargin);
    }

    /**
     * Returns the VillageNames values the oracles of a world read, as one line for the world's pinned profile
     * snapshot (0006 §2), or {@code "absent"}.
     *
     * @param world the world
     * @return the summary
     */
    public static String settingsSummary(World world) {
        return isLoaded() ? Present.settings(world)
            .summary() : "absent";
    }

    /** Everything that touches VillageNames; loaded only when it is present. */
    static final class Present {

        private Present() {}

        static VillageNamesSettings settings(World world) {
            return VillageNamesConfig.read(world);
        }

        static StructureOracle create(World world, TerrainHeightOracle heights, int landmarkMargin) {
            VillageNamesSettings settings = VillageNamesConfig.read(world);
            if (!settings.villagesOn() && !settings.monumentsOn()) {
                LostCities.LOG.info("[LCNH-VN] dimension {}: no oracle ({})", world.provider.dimensionId, settings);
                return StructureOracle.NONE;
            }
            BiomeNames biomes = new ChunkManagerBiomeNames(world.getWorldChunkManager());
            VillageNamesPlans plans = new VillageNamesPlans(world);
            VillageOracle villages = new VillageOracle(
                world.getSeed(),
                settings,
                biomes,
                plans,
                heights,
                landmarkMargin,
                VillageOracle.CACHE_SIZE);
            MonumentOracle monuments = new MonumentOracle(
                world.getSeed(),
                settings.monumentsOn(),
                biomes,
                heights,
                MonumentOracle.CACHE_SIZE);
            LostCities.LOG.info(
                "[LCNH-VN] dimension {}: oracle on ({}); generator random {}, handler order pinned {}",
                world.provider.dimensionId,
                settings,
                plans.randomClass(),
                VillageHandlerOrder.isPinned());
            return new VillageNamesStructureOracle(villages, monuments);
        }
    }
}
