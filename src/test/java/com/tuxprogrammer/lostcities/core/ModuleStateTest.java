package com.tuxprogrammer.lostcities.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.config.LostCitiesConfig;
import com.tuxprogrammer.lostcities.config.WorldgenConfig;

/** Module flags: one per feature area, all on by default, read through {@link ModuleState}. */
public class ModuleStateTest {

    @Test
    public void everyModuleIsOnByDefault() {
        assertEquals(ModuleState.allActive(), ModuleState.configured());
    }

    @Test
    public void snapshotFollowsTheConfigFields() {
        WorldgenConfig.railways = false;
        try {
            ModuleState state = ModuleState.configured();
            assertFalse(state.isActive(Module.RAILWAYS));
            assertTrue(state.isActive(Module.CITIES));
            assertEquals(EnumSet.of(Module.CITIES, Module.HIGHWAYS, Module.SCATTERED_STRUCTURES), state.active());
        } finally {
            WorldgenConfig.railways = true;
        }
    }

    @Test
    public void snapshotsAreImmutable() {
        Set<Module> source = EnumSet.of(Module.CITIES);
        ModuleState state = ModuleState.of(source);
        source.add(Module.HIGHWAYS);
        assertFalse(state.isActive(Module.HIGHWAYS), "a later change to the source set does not leak in");
    }

    @Test
    public void everyModuleHasAConfigFieldAndNoFieldIsLeftOver() {
        Set<String> keys = new HashSet<>();
        for (Module module : Module.values()) {
            keys.add(module.configKey());
        }
        Set<String> fields = new HashSet<>();
        LostCitiesConfig.options(WorldgenConfig.class)
            .forEach(field -> fields.add(field.getName()));
        assertEquals(keys, fields);
    }
}
