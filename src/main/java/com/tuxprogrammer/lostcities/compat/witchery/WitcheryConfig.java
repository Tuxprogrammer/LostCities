package com.tuxprogrammer.lostcities.compat.witchery;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraftforge.common.BiomeDictionary;

import com.tuxprogrammer.lostcities.LostCities;

/**
 * Reads Witchery's loaded settings by class and member names, through reflection: Witchery is closed source and has
 * no development jar to compile against (0006, Consequences). Only {@link WitcheryOracles} calls it, after
 * {@code Loader.isModLoaded("witchery")}. Nothing is written: Witchery's config file and fields are only read.
 *
 * <ul>
 * <li>The cell side is read from Witchery's own generator instance ({@code Witchery.worldGenerator}, its
 * {@code field_82665_g} and {@code field_82666_h}), the values it actually uses; if that fails, from
 * {@code Config.instance().worldGenFrequency}.</li>
 * <li>The four type switches are {@code Config.instance().generateCovens}, {@code generateShacks},
 * {@code generateWickerMen} and {@code generateGoblinHuts} (config keys {@code GenerateCovens},
 * {@code GenerateShacks}, {@code GenerateWickerMen}, {@code GenerateHobgoblinHuts}); the pack ships all four on and
 * {@code WorldGenFrequency=12} ({@code config/witchery.cfg}).</li>
 * <li>The disallowed biomes are Witchery's own list, {@code BiomeManager.DISALLOWED_BIOMES}, filled at its
 * {@code postInit}; if it cannot be read, the oracle rebuilds it from the same six Forge dictionary types.</li>
 * <li>Structures generate in the overworld only (Twilight Forest aside, where no city goes).</li>
 * </ul>
 *
 * <p>
 * Decompiled: witchery-1.7.10-0.24.1.jar (GTNH 2.9.0), {@code com.emoniph.witchery.util.Config} (its public fields and
 * {@code instance()}), {@code com.emoniph.witchery.Witchery#worldGenerator},
 * {@code com.emoniph.witchery.worldgen.WitcheryWorldGenerator#generate} and
 * {@code com.emoniph.witchery.worldgen.BiomeManager#DISALLOWED_BIOMES} / {@code #addModBiomes}; member names checked
 * with
 * {@code javap} against the pack jar; inferred from decompiled bytecode and may be wrong. Third-party decompile cited
 * by
 * the spec:
 * https://github.com/GTNH-Museum/Witchery/blob/4b7a98b60de7901e3503247016aa90c90099d0ce/src/main/java/com/emoniph/witchery/worldgen/BiomeManager.java#L14-L84.
 *
 * <p>
 * Server thread only. Side: server.
 */
final class WitcheryConfig {

    /** Witchery's config class. */
    static final String CONFIG_CLASS = "com.emoniph.witchery.util.Config";
    /** Witchery's main mod class, which holds its world generator. */
    static final String MOD_CLASS = "com.emoniph.witchery.Witchery";
    /** Witchery's biome list holder. */
    static final String BIOME_MANAGER_CLASS = "com.emoniph.witchery.worldgen.BiomeManager";

    private WitcheryConfig() {}

    /**
     * Reads the settings of a world.
     *
     * @param world the world
     * @return the settings
     */
    static WitcherySettings read(World world) {
        boolean overworld = world.provider.dimensionId == 0;
        Object config = configInstance();
        List<WitcherySettings.StructureType> types = new ArrayList<>();
        int frequency = 12;
        if (config != null) {
            addIf(types, config, "generateCovens", WitcherySettings.StructureType.COVEN);
            addIf(types, config, "generateShacks", WitcherySettings.StructureType.SHACK);
            addIf(types, config, "generateWickerMen", WitcherySettings.StructureType.WICKER_MAN);
            addIf(types, config, "generateGoblinHuts", WitcherySettings.StructureType.HOBGOBLIN_HUT);
            Integer read = intField(config, "worldGenFrequency");
            if (read != null) {
                frequency = read;
            }
        } else {
            for (WitcherySettings.StructureType type : WitcherySettings.StructureType.values()) {
                types.add(type);
            }
        }
        int cellSize = generatorCellSize();
        if (cellSize <= WitcheryGrid.SEPARATION) {
            cellSize = WitcheryGrid.cellSizeFor(frequency);
        }
        return new WitcherySettings(types, cellSize, overworld);
    }

    /**
     * Returns Witchery's biome test over a world's biome provider.
     *
     * @param world the world
     * @return the test
     */
    static WitcheryBiomes biomes(World world) {
        Set<Integer> disallowed = disallowedBiomes();
        WorldChunkManager manager = world.getWorldChunkManager();
        return (x, z) -> {
            BiomeGenBase biome = manager.getBiomeGenAt(x, z);
            return biome == null || !disallowed.contains(biome.biomeID);
        };
    }

    /** Returns the IDs of the biomes Witchery's generator skips. */
    static Set<Integer> disallowedBiomes() {
        Set<Integer> out = new HashSet<>();
        try {
            Field field = Class.forName(BIOME_MANAGER_CLASS)
                .getField("DISALLOWED_BIOMES");
            Object list = field.get(null);
            if (list instanceof List<?>) {
                for (Object id : (List<?>) list) {
                    if (id instanceof Integer) {
                        out.add((Integer) id);
                    }
                }
            }
        } catch (ReflectiveOperationException | LinkageError e) {
            LostCities.LOG.warn(
                "[LCNH-WITCHERY] cannot read Witchery's disallowed biomes ({}); rebuilt from types",
                e.toString());
        }
        if (out.isEmpty()) {
            BiomeDictionary.Type[] types = { BiomeDictionary.Type.SWAMP, BiomeDictionary.Type.RIVER,
                BiomeDictionary.Type.OCEAN, BiomeDictionary.Type.MOUNTAIN, BiomeDictionary.Type.JUNGLE,
                BiomeDictionary.Type.BEACH };
            for (BiomeDictionary.Type type : types) {
                for (BiomeGenBase biome : BiomeDictionary.getBiomesForType(type)) {
                    out.add(biome.biomeID);
                }
            }
        }
        return out;
    }

    /**
     * Returns Witchery's world generator, or {@code null}: the scenario reads the structures it recorded.
     *
     * @return the generator instance
     */
    static Object generator() {
        try {
            Field field = Class.forName(MOD_CLASS)
                .getDeclaredField("worldGenerator");
            field.setAccessible(true);
            return field.get(null);
        } catch (ReflectiveOperationException | LinkageError | SecurityException e) {
            LostCities.LOG.warn("[LCNH-WITCHERY] cannot read Witchery's world generator ({})", e.toString());
            return null;
        }
    }

    private static int generatorCellSize() {
        Object generator = generator();
        if (generator == null) {
            return -1;
        }
        Integer cell = intDeclared(generator, "field_82665_g");
        Integer separation = intDeclared(generator, "field_82666_h");
        if (cell == null || separation == null || separation != WitcheryGrid.SEPARATION) {
            return -1;
        }
        return cell;
    }

    private static Object configInstance() {
        try {
            Method instance = Class.forName(CONFIG_CLASS)
                .getMethod("instance");
            return instance.invoke(null);
        } catch (ReflectiveOperationException | LinkageError e) {
            LostCities.LOG
                .warn("[LCNH-WITCHERY] cannot read Witchery's config ({}); assuming the pack's", e.toString());
            return null;
        }
    }

    private static void addIf(List<WitcherySettings.StructureType> types, Object config, String field,
        WitcherySettings.StructureType type) {
        try {
            if (config.getClass()
                .getField(field)
                .getBoolean(config)) {
                types.add(type);
            }
        } catch (ReflectiveOperationException | IllegalArgumentException e) {
            LostCities.LOG.warn("[LCNH-WITCHERY] cannot read Witchery's {} ({}); assuming on", field, e.toString());
            types.add(type);
        }
    }

    private static Integer intField(Object target, String field) {
        try {
            return target.getClass()
                .getField(field)
                .getInt(target);
        } catch (ReflectiveOperationException | IllegalArgumentException e) {
            LostCities.LOG.warn("[LCNH-WITCHERY] cannot read Witchery's {} ({})", field, e.toString());
            return null;
        }
    }

    private static Integer intDeclared(Object target, String field) {
        try {
            Field declared = target.getClass()
                .getDeclaredField(field);
            declared.setAccessible(true);
            return declared.getInt(target);
        } catch (ReflectiveOperationException | IllegalArgumentException | SecurityException e) {
            return null;
        }
    }
}
