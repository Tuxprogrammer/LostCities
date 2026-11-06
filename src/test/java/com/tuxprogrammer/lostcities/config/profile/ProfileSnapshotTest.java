package com.tuxprogrammer.lostcities.config.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.core.Module;
import com.tuxprogrammer.lostcities.core.ModuleState;

/** Profile pinning (0005 §7). */
public class ProfileSnapshotTest {

    @Test
    public void theDefaultProfileResolvesEveryTerrainValue() {
        ProfileSnapshot snapshot = ProfileSnapshot
            .resolve("default", ProfileSnapshot.PLAN_LAYOUT, ModuleState.allActive(), "none", "RWG", "test");
        assertEquals(
            "71",
            snapshot.values()
                .get("groundLevel"));
        assertEquals(
            71,
            snapshot.terrainSettings()
                .groundLevel());
        assertEquals(ProfileSnapshot.LAYOUT_VERSION, snapshot.layoutVersion());
        assertTrue(
            snapshot.modules()
                .get("cities"));
    }

    @Test
    public void moduleFlagsArePinnedWithTheProfile() {
        ModuleState noCities = ModuleState.of(EnumSet.of(Module.HIGHWAYS));
        ProfileSnapshot snapshot = ProfileSnapshot
            .resolve("default", ProfileSnapshot.PLAN_LAYOUT, noCities, "none", "RWG2_CONTINENTS", "test");
        assertFalse(
            snapshot.moduleState()
                .isActive(Module.CITIES));
        assertTrue(
            snapshot.moduleState()
                .isActive(Module.HIGHWAYS));
    }

    @Test
    public void anUnknownProfileIsRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> ProfileSnapshot
                .resolve("cavern", ProfileSnapshot.PLAN_LAYOUT, ModuleState.allActive(), "none", "RWG", "test"));
    }

    @Test
    public void aBuiltInProfilePinsEveryKeptOption() {
        ProfileSnapshot snapshot = ProfileSnapshot
            .resolve("atlantis", ProfileSnapshot.PLAN_LAYOUT, ModuleState.allActive(), "none", "RWG", "test");
        assertEquals(
            "89",
            snapshot.values()
                .get("seaLevel"));
        assertEquals(
            89,
            snapshot.terrainSettings()
                .waterLevel());
        for (ProfileOption option : ProfileOptions.all()) {
            assertEquals(
                option.kept(),
                snapshot.values()
                    .containsKey(option.key()),
                option.key());
        }
    }

    @Test
    public void aSnapshotPinnedBeforeM09ReadsMissingOptionsAsDefaults() {
        java.util.Map<String, String> old = new java.util.TreeMap<>();
        old.put("groundLevel", "71");
        old.put("preventLakesRavinesInCities", "false");
        ProfileSnapshot snapshot = new ProfileSnapshot(
            "default",
            ProfileSnapshot.PLAN_LAYOUT,
            old,
            java.util.Collections.<String, Boolean>emptyMap(),
            "none",
            1,
            "RWG",
            "old");
        assertEquals(
            0.3f,
            snapshot.profileValues()
                .getFloat("buildingChance"));
        assertFalse(
            snapshot.terrainSettings()
                .preventLakesRavinesInCities(),
            "1.12's key, pinned by M04 worlds, still reads");
    }

    @Test
    public void pinsTheDecorativeMetalsAndTheAssetFiles() {
        java.util.Map<String, String> files = new java.util.TreeMap<>();
        files.put("parts/lostcities:a", "aa");
        ProfileSnapshot snapshot = ProfileSnapshot.resolve(
            "default",
            ProfileSnapshot.PLAN_LAYOUT,
            com.tuxprogrammer.lostcities.core.ModuleState
                .of(java.util.EnumSet.allOf(com.tuxprogrammer.lostcities.core.Module.class)),
            "hash1",
            files,
            com.tuxprogrammer.lostcities.asset.block.MetalFlags.ALL_OFF,
            "RWG",
            "test");
        assertEquals(com.tuxprogrammer.lostcities.asset.block.MetalFlags.ALL_OFF, snapshot.metalFlags());
        assertEquals(files, snapshot.assetFiles());
        assertEquals(
            java.util.Collections.emptyList(),
            snapshot.changedAssets("hash1", pinned -> java.util.Arrays.asList("x")));
        assertEquals(
            java.util.Arrays.asList("x"),
            snapshot.changedAssets("hash2", pinned -> java.util.Arrays.asList("x")));
    }

    @Test
    public void aSnapshotPinnedBeforeMetalsAndAssetsKeepsEveryMetalAndWarnsOfNothing() {
        ProfileSnapshot old = new ProfileSnapshot(
            "default",
            ProfileSnapshot.PLAN_LAYOUT,
            java.util.Collections.<String, String>emptyMap(),
            java.util.Collections.<String, Boolean>emptyMap(),
            ProfileSnapshot.NO_ASSETS,
            1,
            "RWG",
            "test");
        assertEquals(com.tuxprogrammer.lostcities.asset.block.MetalFlags.ALL_ON, old.metalFlags());
        assertEquals(
            java.util.Collections.emptyList(),
            old.changedAssets("any", pinned -> java.util.Arrays.asList("x")));
    }

    @Test
    public void pinsTheStructureGeneratorSettings() {
        ProfileSnapshot snapshot = new ProfileSnapshot(
            "default",
            ProfileSnapshot.PLAN_LAYOUT,
            java.util.Collections.<String, String>emptyMap(),
            java.util.Collections.<String, Boolean>emptyMap(),
            "h",
            1,
            "RWG",
            "test");
        assertEquals("", snapshot.structureSettings(), "a world pinned before has none");
        ProfileSnapshot pinned = snapshot.withStructureSettings("villagenames=absent");
        assertEquals("villagenames=absent", pinned.structureSettings());
        assertFalse(pinned.equals(snapshot));
        assertEquals(snapshot.profile(), pinned.profile());
    }

    /** Design record 0014: a world pinned before {@code floorPitch} existed keeps upstream's 6; new pins get 8. */
    @Test
    public void aSnapshotWithoutFurnishGetsNoFurniture() {
        // D282 pinning for the furnish pass (design record 0016): a world saved before it existed stays bare.
        java.util.Map<String, String> old = new java.util.TreeMap<>();
        old.put("groundLevel", "71");
        assertFalse(
            new ProfileSnapshot(
                "default",
                ProfileSnapshot.PLAN_LAYOUT,
                old,
                java.util.Collections.<String, Boolean>emptyMap(),
                "none",
                1,
                "RWG",
                "old").terrainSettings()
                    .furnish());
        assertTrue(
            new ProfileSnapshot(
                "default",
                ProfileSnapshot.PLAN_LAYOUT,
                ProfileOptions.defaults(),
                java.util.Collections.<String, Boolean>emptyMap(),
                "none",
                1,
                "RWG",
                "new").terrainSettings()
                    .furnish());
    }

    @Test
    public void aSnapshotWithoutPackHouseChanceGetsNoPackHouses() {
        java.util.Map<String, String> old = new java.util.TreeMap<>();
        old.put("groundLevel", "71");
        ProfileSnapshot snapshot = new ProfileSnapshot(
            "default",
            ProfileSnapshot.PLAN_LAYOUT,
            old,
            java.util.Collections.<String, Boolean>emptyMap(),
            "none",
            1,
            "RWG",
            "old");
        assertEquals(
            0f,
            snapshot.terrainSettings()
                .packHouseChance(),
            0f);
        assertEquals(
            0.25f,
            new ProfileSnapshot(
                "default",
                ProfileSnapshot.PLAN_LAYOUT,
                ProfileOptions.defaults(),
                java.util.Collections.<String, Boolean>emptyMap(),
                "none",
                1,
                "RWG",
                "new").terrainSettings()
                    .packHouseChance(),
            0f);
    }

    @Test
    public void aSnapshotWithoutLotFootprintsKeepsSingleChunkLots() {
        java.util.Map<String, String> old = new java.util.TreeMap<>();
        old.put("groundLevel", "71");
        assertFalse(
            new ProfileSnapshot(
                "default",
                ProfileSnapshot.PLAN_LAYOUT,
                old,
                java.util.Collections.<String, Boolean>emptyMap(),
                "none",
                1,
                "RWG",
                "old").terrainSettings()
                    .lotFootprints());
        assertTrue(
            new ProfileSnapshot(
                "default",
                ProfileSnapshot.PLAN_LAYOUT,
                ProfileOptions.defaults(),
                java.util.Collections.<String, Boolean>emptyMap(),
                "none",
                1,
                "RWG",
                "new").terrainSettings()
                    .lotFootprints());
    }

    @Test
    public void aSnapshotWithoutDensityRingsGetsNoRings() {
        java.util.Map<String, String> old = new java.util.TreeMap<>();
        old.put("groundLevel", "71");
        ProfileSnapshot snapshot = new ProfileSnapshot(
            "default",
            ProfileSnapshot.PLAN_LAYOUT,
            old,
            java.util.Collections.<String, Boolean>emptyMap(),
            "none",
            1,
            "RWG",
            "old");
        assertEquals(
            0f,
            snapshot.terrainSettings()
                .densityRings(),
            0f);
        assertEquals(
            1f,
            new ProfileSnapshot(
                "default",
                ProfileSnapshot.PLAN_LAYOUT,
                ProfileOptions.defaults(),
                java.util.Collections.<String, Boolean>emptyMap(),
                "none",
                1,
                "RWG",
                "new").terrainSettings()
                    .densityRings(),
            0f);
    }

    @Test
    public void aSnapshotWithoutFloorPitchResolvesToTheLegacyPitch() {
        java.util.Map<String, String> old = new java.util.TreeMap<>();
        old.put("groundLevel", "71");
        ProfileSnapshot snapshot = new ProfileSnapshot(
            "default",
            ProfileSnapshot.PLAN_LAYOUT,
            old,
            java.util.Collections.<String, Boolean>emptyMap(),
            "none",
            1,
            "RWG",
            "old");
        assertEquals("6", ProfileOptions.legacyDefault("floorPitch"));
        assertEquals(
            "6",
            snapshot.values()
                .get("floorPitch"));
        assertEquals(
            6,
            snapshot.profileValues()
                .getInt("floorPitch"));
        assertEquals(
            6,
            snapshot.terrainSettings()
                .floorPitch());
        assertEquals(
            "8",
            ProfileOptions.defaults()
                .get("floorPitch"));
        old.put("floorPitch", "8");
        ProfileSnapshot pinned = new ProfileSnapshot(
            "default",
            ProfileSnapshot.PLAN_LAYOUT,
            old,
            java.util.Collections.<String, Boolean>emptyMap(),
            "none",
            1,
            "RWG",
            "new");
        assertEquals(
            8,
            pinned.terrainSettings()
                .floorPitch());
    }
}
