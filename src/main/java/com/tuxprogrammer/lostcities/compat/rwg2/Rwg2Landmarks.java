package com.tuxprogrammer.lostcities.compat.rwg2;

import java.util.Locale;

import net.minecraft.world.World;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.compat.HeightOracles;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.scenario.ScenarioRegistry;
import com.tuxprogrammer.lostcities.worldgen.height.TerrainHeightOracle;

import cpw.mods.fml.common.Loader;

/**
 * RWG2's terrain landmarks of a world, for the layout (0006 §1, §2, §6): volcano islands, lava-cave openings and
 * mountain-chain river junctions as {@code TERRAIN} landmarks ({@link Rwg2LandmarkOracle}), so a city ends at them
 * with a soft edge.
 *
 * <p>
 * Guarantees, for the glue that wires it in ({@code worldgen.overlay.StructureOracles}):
 * <ul>
 * <li><b>Pure.</b> Answers depend only on the world seed and RWG2's config (through the world's own
 * {@code ChunkManagerRealistic}, whose predicates are pure) and the height oracle. No chunk is loaded or generated and
 * {@code world.rand} is never touched.</li>
 * <li><b>Order-independent and complete</b>, as {@link StructureOracle} requires; landmarks come back sorted by
 * identity: {@code volcano@<x>,<z>}, {@code lavacave@<x>,<z>}, {@code junction@<x>,<z>} (block coordinates).</li>
 * <li><b>Bounded:</b> {@value Rwg2LandmarkOracle#REGION_CACHE_SIZE} sample regions per kind and
 * {@value Rwg2LandmarkOracle#LANDMARK_CACHE_SIZE} landmarks per kind, owned by the returned object.</li>
 * <li><b>Empty</b> ({@link StructureOracle#NONE}) unless RWG2 is loaded and the world is an RWG2 or RWG2 Continents
 * world: classic RWG has none of these landmarks. Volcanoes and lava caves exist only on RWG2 Continents; junctions on
 * both.</li>
 * </ul>
 * Classes that touch RWG2 load only after {@code Loader.isModLoaded("rwg2")}. Server thread only.
 *
 * <p>
 * Spec: docs/new-spec/00-pack-integration/coexistence.md § RWG2 landmarks; docs/design/0006-build-around-other-worldgen.md
 * §1. Side: server.
 */
public final class Rwg2Landmarks {

    private Rwg2Landmarks() {}

    /**
     * Returns whether a world is one RWG2 generates: RWG2 loaded and the world type {@code RWG2} or
     * {@code RWG2_CONTINENTS}.
     *
     * @param world the world
     * @return whether the oracle applies
     */
    public static boolean applies(World world) {
        return Loader.isModLoaded(HeightOracles.RWG2_MOD) && HeightOracles.worldType(world)
            .toUpperCase(Locale.ROOT)
            .startsWith("RWG2");
    }

    /**
     * Returns the oracle of a world.
     *
     * @param world          the world, whose generator already exists
     * @param heights        the world's height oracle (its probe defers sampling work), or {@code null}
     * @param landmarkMargin the profile's {@code landmarkMargin}, in blocks; unused (the layout grows the footprint)
     * @return the oracle; {@link StructureOracle#NONE} when the world is not RWG2's
     */
    public static StructureOracle forWorld(World world, TerrainHeightOracle heights, int landmarkMargin) {
        if (!applies(world)) {
            return StructureOracle.NONE;
        }
        return Present.create(world, heights);
    }

    /**
     * Returns the RWG2 values that move its landmarks, as one line for the world's pinned profile snapshot (0006 §2),
     * or {@code "absent"}.
     *
     * @param world the world
     * @return the summary
     */
    public static String settingsSummary(World world) {
        return applies(world) ? Present.settings() : "absent";
    }

    /**
     * Registers the {@code oracle.rwg2} scenario when RWG2 is loaded; the scenario's class names RWG2 types, so it is
     * never loaded without it.
     *
     * @param registry where to add it
     */
    public static void registerScenarios(ScenarioRegistry registry) {
        if (Loader.isModLoaded(HeightOracles.RWG2_MOD)) {
            Rwg2LandmarkScenario.registerAll(registry);
        }
    }

    /** Everything that touches RWG2; loaded only when it is present. */
    static final class Present {

        private Present() {}

        static StructureOracle create(World world, TerrainHeightOracle heights) {
            if (!(world.getWorldChunkManager() instanceof rwg2.world.ChunkManagerRealistic manager)) {
                LostCities.LOG.info(
                    "[LCNH-RWG2] dimension {}: no landmark oracle (the chunk manager is not RWG2's)",
                    world.provider.dimensionId);
                return StructureOracle.NONE;
            }
            LostCities.LOG.info(
                "[LCNH-RWG2] dimension {}: landmark oracle on ({})",
                world.provider.dimensionId,
                settings());
            return new Rwg2LandmarkOracle(
                new ManagerLandforms(manager),
                heights,
                Rwg2LandmarkOracle.REGION_CACHE_SIZE,
                Rwg2LandmarkOracle.LANDMARK_CACHE_SIZE);
        }

        static String settings() {
            return String.format(
                Locale.ROOT,
                "continent=%.0f-%.0f,island=%.0f-%.0f,ocean=%.0f/%.0f/%.2f,islandChance=%.2f,volcanoChance=%.2f,"
                    + "landmarks=%.2f,landmassOffset=%d/%d,biomeOffset=%d/%d",
                rwg2.config.ConfigRWG.minimumContinentWidth,
                rwg2.config.ConfigRWG.maximumContinentWidth,
                rwg2.config.ConfigRWG.minimumIslandWidth,
                rwg2.config.ConfigRWG.maximumIslandWidth,
                rwg2.config.ConfigRWG.averageOceanWidth,
                rwg2.config.ConfigRWG.minimumOceanWidth,
                rwg2.config.ConfigRWG.maximumOceanFraction,
                rwg2.config.ConfigRWG.islandPlacementChance,
                rwg2.config.ConfigRWG.largeIslandVolcanoChance,
                rwg2.config.ConfigRWG.averageLandmarksPerTypeAndContinent,
                rwg2.config.ConfigRWG.landmassOffsetX,
                rwg2.config.ConfigRWG.landmassOffsetZ,
                rwg2.config.ConfigRWG.biomeOffsetX,
                rwg2.config.ConfigRWG.biomeOffsetZ);
        }
    }
}
