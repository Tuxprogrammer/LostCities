package com.tuxprogrammer.lostcities.compat;

import java.util.Locale;

import net.minecraft.world.World;
import net.minecraft.world.gen.FlatGeneratorInfo;
import net.minecraft.world.gen.FlatLayerInfo;

import com.tuxprogrammer.lostcities.compat.rwg.RwgHeightOracle;
import com.tuxprogrammer.lostcities.compat.rwg2.Rwg2HeightOracle;
import com.tuxprogrammer.lostcities.worldgen.height.ConstantHeightOracle;
import com.tuxprogrammer.lostcities.worldgen.height.TerrainHeightOracle;

import cpw.mods.fml.common.Loader;

/**
 * Picks the height oracle for a world's type (docs/design/0005-rwg-overlay.md §4 and §7, "World type check"): RWG2's
 * world-less sampler for {@code RWG2_CONTINENTS} and {@code RWG2}, classic RWG's second generator for {@code RWG},
 * the preset's top for {@code flat}. Every other world type has no oracle, so the overlay is not installed there.
 *
 * <p>
 * The RWG classes are touched only after {@code Loader.isModLoaded}, so this class loads without either mod.
 * Server thread only. Side: server.
 */
public final class HeightOracles {

    /** RWG2's mod ID. */
    public static final String RWG2_MOD = "rwg2";
    /** Classic RWG's mod ID. */
    public static final String RWG_MOD = "RWG";

    private HeightOracles() {}

    /**
     * Returns a world's oracle.
     *
     * @param world the world, whose generator already exists
     * @return the oracle, or {@code null} when its world type has none
     */
    public static TerrainHeightOracle create(World world) {
        String type = worldType(world).toUpperCase(Locale.ROOT);
        switch (type) {
            case "RWG2_CONTINENTS", "RWG2" -> {
                return Loader.isModLoaded(RWG2_MOD) ? Rwg2HeightOracle.create(world, "RWG2_CONTINENTS".equals(type))
                    : null;
            }
            case "RWG" -> {
                return Loader.isModLoaded(RWG_MOD) ? RwgHeightOracle.create(world) : null;
            }
            case "FLAT" -> {
                return new ConstantHeightOracle(flatTop(world));
            }
            default -> {
                return null;
            }
        }
    }

    /**
     * Returns whether a world's type has an oracle, without building one (classic RWG's oracle is a second generator,
     * so it is built once, at installation).
     *
     * @param world the world
     * @return {@code true} for RWG2 Continents, RWG2 and classic RWG with their mod loaded, and superflat
     */
    public static boolean supports(World world) {
        String type = worldType(world).toUpperCase(Locale.ROOT);
        return switch (type) {
            case "RWG2_CONTINENTS", "RWG2" -> Loader.isModLoaded(RWG2_MOD);
            case "RWG" -> Loader.isModLoaded(RWG_MOD);
            case "FLAT" -> true;
            default -> false;
        };
    }

    /**
     * Returns the name of a world's type, for example {@code RWG2_CONTINENTS}.
     *
     * @param world the world
     * @return the name as registered
     */
    public static String worldType(World world) {
        return world.getWorldInfo()
            .getTerrainType()
            .getWorldTypeName();
    }

    private static int flatTop(World world) {
        FlatGeneratorInfo info = FlatGeneratorInfo.createFlatGeneratorFromString(
            world.getWorldInfo()
                .getGeneratorOptions());
        int top = 0;
        for (Object layer : info.getFlatLayers()) {
            top += ((FlatLayerInfo) layer).getLayerCount();
        }
        return top;
    }
}
