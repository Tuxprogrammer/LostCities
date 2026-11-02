package com.tuxprogrammer.lostcities.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraft.launchwrapper.Launch;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import com.tuxprogrammer.lostcities.core.Module;
import com.tuxprogrammer.lostcities.core.ModuleState;

import cpw.mods.fml.relauncher.FMLInjectionData;

/**
 * The module flags load from and save to {@code config/lostcitiesnh.cfg} through GTNHLib, with their defaults,
 * comments and the "new worlds only" marker (docs/ROADMAP.md, M01 exit gate: "Module flags load, save and are
 * documented").
 *
 * <p>
 * GTNHLib and Forge's {@code Configuration} find the config folder from the game directory. The test points both at a
 * temporary folder before GTNHLib's {@code ConfigurationManager} first loads, so nothing is written into the project.
 * This is the only test class that registers a config, because GTNHLib keeps one {@code Configuration} per file for
 * the life of the JVM.
 */
public class WorldgenConfigTest {

    @TempDir
    static Path gameDir;

    private static Path file;

    @BeforeAll
    public static void registerAgainstATemporaryGameDirectory() throws Exception {
        Launch.minecraftHome = gameDir.toFile();
        // Forge's Configuration reads the game directory from FMLInjectionData, which only FML's launcher fills in.
        Field home = FMLInjectionData.class.getDeclaredField("minecraftHome");
        home.setAccessible(true);
        home.set(null, gameDir.toFile());
        file = gameDir.resolve("config/lostcitiesnh.cfg");
        Files.createDirectories(file.getParent());
        // An existing file where the owner switched highways off; everything else is missing and gets its default.
        Files.write(file, "worldgen {\n    B:highways=false\n}\n".getBytes(StandardCharsets.UTF_8));
        LostCitiesConfig.register();
    }

    @AfterAll
    public static void restoreDefaults() {
        WorldgenConfig.cities = true;
        WorldgenConfig.highways = true;
        WorldgenConfig.railways = true;
        WorldgenConfig.scatteredStructures = true;
    }

    @Test
    public void loadsValuesFromTheFileAndDefaultsForTheRest() {
        assertFalse(WorldgenConfig.highways, "highways=false from the file");
        assertTrue(WorldgenConfig.cities, "cities defaults to true");
        assertTrue(WorldgenConfig.railways, "railways defaults to true");
        assertTrue(WorldgenConfig.scatteredStructures, "scatteredStructures defaults to true");
        assertFalse(ModuleState.configured()
            .isActive(Module.HIGHWAYS));
        assertTrue(ModuleState.configured()
            .isActive(Module.CITIES));
    }

    @Test
    public void writesMissingKeysWithCommentsAndTheNewWorldsMarker() throws Exception {
        String text = read();
        assertTrue(text.contains("B:cities=true"), text);
        assertTrue(text.contains("B:scatteredStructures=true"), text);
        assertTrue(text.contains("New worlds only."), "every worldgen comment says new worlds only:\n" + text);
        assertTrue(
            ConfigurationManager.getConfig(WorldgenConfig.class)
                .getCategory(WorldgenConfig.CATEGORY)
                .requiresWorldRestart(),
            "the worldgen category is marked as needing a world restart");
    }

    @Test
    public void savesChangedValues() throws Exception {
        WorldgenConfig.railways = false;
        try {
            ConfigurationManager.save(WorldgenConfig.class);
            assertTrue(read().contains("B:railways=false"), read());
        } finally {
            WorldgenConfig.railways = true;
            ConfigurationManager.save(WorldgenConfig.class);
        }
        assertTrue(read().contains("B:railways=true"), read());
    }

    @Test
    public void generatedLangKeysFollowGtnhLibsPattern() throws Exception {
        assertEquals(
            "lostcitiesnh.worldgen.scatteredstructures",
            ConfigurationManager.getConfig(WorldgenConfig.class)
                .getCategory(WorldgenConfig.CATEGORY)
                .get("scatteredStructures")
                .getLanguageKey());
        assertTrue(LostCitiesConfig.langKeys()
            .contains("lostcitiesnh.worldgen.scatteredstructures"));
        assertTrue(LostCitiesConfig.langKeys()
            .contains("lostcitiesnh.worldgen"));
    }

    @Test
    public void everyConfigClassSharesTheModsFile() {
        for (Class<?> configClass : LostCitiesConfig.CLASSES) {
            Config config = configClass.getAnnotation(Config.class);
            assertEquals("lostcitiesnh", config.modid(), configClass.getName());
            assertEquals("", config.filename(), configClass.getName() + " writes config/lostcitiesnh.cfg");
        }
    }

    private static String read() throws Exception {
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8).replace(File.separatorChar, '/');
    }
}
