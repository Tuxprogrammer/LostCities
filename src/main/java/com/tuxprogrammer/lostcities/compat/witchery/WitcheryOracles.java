package com.tuxprogrammer.lostcities.compat.witchery;

import net.minecraft.world.World;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.worldgen.height.TerrainHeightOracle;

import cpw.mods.fml.common.Loader;

/**
 * The Witchery structure oracle of a world, for the layout (0006 §2, §6): every candidate lot of its covens, shacks,
 * wicker men and hobgoblin huts as a {@code UNION} landmark ({@link WitcheryOracle}).
 *
 * <p>
 * Guarantees, for the glue that wires it in ({@code worldgen.overlay.StructureOracles}):
 * <ul>
 * <li><b>Pure.</b> Answers depend only on the world seed, Witchery's config, the world's biome provider and the height
 * oracle. No chunk is loaded or generated, and {@code world.rand} is never touched: the grid seed is computed on a
 * {@code Random} of the oracle's own.</li>
 * <li><b>Order-independent and complete</b>, as {@link StructureOracle} requires; landmarks come back sorted by
 * identity, {@code witchery@<chunkX>,<chunkZ>} (candidate chunks).</li>
 * <li><b>Bounded:</b> {@value WitcheryOracle#CACHE_SIZE} candidate chunks cached in the returned object.</li>
 * <li><b>Empty</b> ({@link StructureOracle#NONE}) when Witchery is not loaded, outside the overworld, or with every
 * structure type switched off.</li>
 * </ul>
 * Classes that touch Witchery load only after {@code Loader.isModLoaded("witchery")}, so this class loads without it.
 * Server thread only.
 *
 * <p>
 * Spec: docs/new-spec/00-pack-integration/coexistence.md § Witchery; docs/design/0006-build-around-other-worldgen.md
 * §2. Side: server.
 */
public final class WitcheryOracles {

    /** Witchery's mod ID. */
    public static final String MOD_ID = "witchery";

    private WitcheryOracles() {}

    /** Returns whether Witchery is loaded. */
    public static boolean isLoaded() {
        return Loader.isModLoaded(MOD_ID);
    }

    /**
     * Returns the oracle of a world.
     *
     * @param world          the world, whose generator already exists
     * @param heights        the world's height oracle, or {@code null} to estimate every ground at Y 64
     * @param landmarkMargin the profile's {@code landmarkMargin}, in blocks; unused (the layout grows the footprint)
     * @return the oracle; {@link StructureOracle#NONE} when Witchery generates nothing here
     */
    public static StructureOracle forWorld(World world, TerrainHeightOracle heights, int landmarkMargin) {
        if (!isLoaded()) {
            return StructureOracle.NONE;
        }
        return Present.create(world, heights);
    }

    /**
     * Returns the Witchery values the oracle of a world reads, as one line for the world's pinned profile snapshot
     * (0006 §2), or {@code "absent"}.
     *
     * @param world the world
     * @return the summary
     */
    public static String settingsSummary(World world) {
        return isLoaded() ? WitcheryConfig.read(world)
            .summary() : "absent";
    }

    /** Everything that touches Witchery; loaded only when it is present. */
    static final class Present {

        private Present() {}

        static StructureOracle create(World world, TerrainHeightOracle heights) {
            WitcherySettings settings = WitcheryConfig.read(world);
            if (!settings.on()) {
                LostCities.LOG.info("[LCNH-WITCHERY] dimension {}: no oracle ({})", world.provider.dimensionId, settings);
                return StructureOracle.NONE;
            }
            LostCities.LOG.info("[LCNH-WITCHERY] dimension {}: oracle on ({})", world.provider.dimensionId, settings);
            return new WitcheryOracle(
                world.getSeed(),
                settings,
                WitcheryConfig.biomes(world),
                heights,
                WitcheryOracle.CACHE_SIZE);
        }
    }
}
