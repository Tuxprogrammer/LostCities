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
        assertEquals("71", snapshot.values()
            .get("groundLevel"));
        assertEquals(71, snapshot.terrainSettings()
            .groundLevel());
        assertEquals(ProfileSnapshot.LAYOUT_VERSION, snapshot.layoutVersion());
        assertTrue(snapshot.modules()
            .get("cities"));
    }

    @Test
    public void moduleFlagsArePinnedWithTheProfile() {
        ModuleState noCities = ModuleState.of(EnumSet.of(Module.HIGHWAYS));
        ProfileSnapshot snapshot = ProfileSnapshot
            .resolve("default", ProfileSnapshot.PLAN_LAYOUT, noCities, "none", "RWG2_CONTINENTS", "test");
        assertFalse(snapshot.moduleState()
            .isActive(Module.CITIES));
        assertTrue(snapshot.moduleState()
            .isActive(Module.HIGHWAYS));
    }

    @Test
    public void anUnknownProfileIsRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> ProfileSnapshot
                .resolve("atlantis", ProfileSnapshot.PLAN_LAYOUT, ModuleState.allActive(), "none", "RWG", "test"));
    }
}
