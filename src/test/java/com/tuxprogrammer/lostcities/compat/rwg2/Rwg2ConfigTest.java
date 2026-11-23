package com.tuxprogrammer.lostcities.compat.rwg2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

/**
 * {@link Rwg2Config#find}: the 2.0.9 home wins, the 2.0.6 {@code ConfigRWG} is the fallback, and a field in neither,
 * or an instance field, resolves to nothing. The two RWG2 layouts are stood in for by the nested classes below.
 */
class Rwg2ConfigTest {

    /** Stands in for RWG2 2.0.9's {@code MigrationConfig}. */
    public static final class NewHome {

        public static int landmassOffsetX = 7;
        public int instanceOnly = 1;
    }

    /** Stands in for RWG2 2.0.6's {@code ConfigRWG}. */
    public static final class OldHome {

        public static int landmassOffsetX = -3;
        public static int biomeOffsetX = 11;
    }

    private static final String NEW = NewHome.class.getName();
    private static final String OLD = OldHome.class.getName();

    @Test
    void preferredClassWinsWhenItHasTheField() throws IllegalAccessException {
        Field field = Rwg2Config.find(NEW, OLD, "landmassOffsetX");
        assertEquals(NewHome.class, field.getDeclaringClass());
        assertEquals(7, field.getInt(null));
    }

    @Test
    void fallsBackWhenThePreferredClassLacksTheField() throws IllegalAccessException {
        Field field = Rwg2Config.find(NEW, OLD, "biomeOffsetX");
        assertEquals(OldHome.class, field.getDeclaringClass());
        assertEquals(11, field.getInt(null));
    }

    @Test
    void fallsBackWhenThePreferredClassIsMissing() throws IllegalAccessException {
        Field field = Rwg2Config.find("rwg2.config.NoSuchConfig", OLD, "landmassOffsetX");
        assertEquals(-3, field.getInt(null));
    }

    @Test
    void missingEverywhereOrNotStaticIsNull() {
        assertNull(Rwg2Config.find(NEW, OLD, "noSuchField"));
        assertNull(Rwg2Config.find(NEW, "rwg2.config.NoSuchConfig", "instanceOnly"));
        assertNull(Rwg2Config.find("rwg2.config.NoSuchConfig", "rwg2.config.AlsoMissing", "landmassOffsetX"));
    }
}
