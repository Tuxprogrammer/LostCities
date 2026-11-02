package com.tuxprogrammer.lostcities.config.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.tuxprogrammer.lostcities.core.ModuleState;

/** Profile files in {@code config/lostcitiesnh/profiles/} (docs/new-spec/02-profiles-dimensions/index.md). */
public class ProfileRegistryTest {

    @TempDir
    Path folder;

    private void write(String file, String text) throws IOException {
        Files.write(folder.resolve(file), text.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void builtInsAreThereWithoutAnyFile() {
        ProfileRegistry registry = new ProfileRegistry();
        assertNotNull(registry.get("default"));
        assertNotNull(registry.get("atlantis"));
        assertNull(registry.get("cavern"), "not ported before PersonalSpace (D12)");
    }

    @Test
    public void doesNotReproducePROF03_oneUnreadableFileDoesNotStopTheRest() throws IOException {
        Files.createDirectory(folder.resolve("aaa.json"));
        write("bbb.json", "{ \"lostcity\": { ");
        write("ccc.json", "{ \"cities\": { \"cityChance\": 0.05 } }");
        ProfileRegistry registry = new ProfileRegistry();
        List<ProfileRegistry.Message> report = registry.load(folder);
        assertNotNull(registry.get("ccc"), report::toString);
        assertNull(registry.get("aaa"));
        assertNull(registry.get("bbb"));
        assertNotNull(registry.rejection("bbb"));
        assertEquals(
            2,
            report.stream()
                .filter(ProfileRegistry.Message::error)
                .count(),
            report::toString);
    }

    @Test
    public void aFileMayNotTakeABuiltInsName() throws IOException {
        write("default.json", "{ \"cities\": { \"cityChance\": 0.5 } }");
        ProfileRegistry registry = new ProfileRegistry();
        List<ProfileRegistry.Message> report = registry.load(folder);
        assertEquals(
            "0.01",
            registry.get("default")
                .values()
                .text("cityChance"));
        assertTrue(
            report.get(0)
                .text()
                .contains("saveprofile"));
    }

    @Test
    public void doesNotReproducePROF09_aPinnedWorldKeepsItsValuesWhenTheFileChanges() throws IOException {
        write("mine.json", "{ \"cities\": { \"cityChance\": 0.05 } }");
        ProfileRegistry.INSTANCE.load(folder);
        try {
            ProfileSnapshot pinned = ProfileSnapshot
                .resolve("mine", ProfileSnapshot.PLAN_LAYOUT, ModuleState.allActive(), "none", "RWG", "test");
            write("mine.json", "{ \"cities\": { \"cityChance\": 0.5 } }");
            ProfileRegistry.INSTANCE.load(folder);
            assertEquals(
                0.05,
                pinned.profileValues()
                    .getDouble("cityChance"));
            assertEquals(
                0.5,
                ProfileRegistry.INSTANCE.get("mine")
                    .values()
                    .getDouble("cityChance"));
        } finally {
            Files.delete(folder.resolve("mine.json"));
            ProfileRegistry.INSTANCE.load(folder);
        }
    }

    @Test
    public void doesNotReproducePROF13_aMissingWorldStyleRejectsTheProfileNotTheDimension() throws IOException {
        write("odd.json", "{ \"lostcity\": { \"worldStyle\": \"nowhere\" } }");
        write("dark.json", "{ \"lostcity\": { \"liquidBlock\": \"minecraft:nothing\" } }");
        ProfileRegistry registry = new ProfileRegistry();
        registry.load(folder);
        List<ProfileRegistry.Message> report = registry
            .checkAssets("standard"::equals, "citystyle_border"::equals, name -> !name.contains("nothing"));
        assertNull(registry.get("odd"));
        assertNull(registry.get("dark"));
        assertNotNull(registry.get("largecities"));
        assertEquals(2, report.size(), report::toString);
        assertThrows(IllegalArgumentException.class, () -> registry.require("odd"));
    }
}
