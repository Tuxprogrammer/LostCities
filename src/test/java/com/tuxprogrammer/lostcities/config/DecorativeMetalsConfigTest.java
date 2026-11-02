package com.tuxprogrammer.lostcities.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.block.DecorativeMetal;
import com.tuxprogrammer.lostcities.asset.block.MetalFlags;

/** One config flag per decorative metal (D29), every one on by default. */
public class DecorativeMetalsConfigTest {

    @Test
    public void everyMetalHasExactlyOneFlag() {
        Set<String> fields = new TreeSet<>();
        for (Field field : LostCitiesConfig.options(DecorativeMetalsConfig.class)) {
            fields.add(field.getName());
        }
        Set<String> metals = new TreeSet<>();
        for (DecorativeMetal metal : DecorativeMetal.values()) {
            metals.add(metal.key());
        }
        assertEquals(metals, fields);
        assertTrue(LostCitiesConfig.CLASSES.contains(DecorativeMetalsConfig.class));
    }

    @Test
    public void everyFlagIsOnByDefaultAndReadsIntoMetalFlags() {
        assertEquals(MetalFlags.ALL_ON, DecorativeMetalsConfig.flags());
        boolean before = DecorativeMetalsConfig.copper;
        try {
            DecorativeMetalsConfig.copper = false;
            MetalFlags flags = DecorativeMetalsConfig.flags();
            assertFalse(flags.isOn(DecorativeMetal.COPPER));
            assertTrue(flags.isOn(DecorativeMetal.IRON));
        } finally {
            DecorativeMetalsConfig.copper = before;
        }
    }
}
