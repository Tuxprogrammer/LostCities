package com.tuxprogrammer.lostcities.compat.villagenames;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

/** The pinned handler order: by class name, whatever the insertion order, also for handlers added later. */
class VillageHandlerOrderTest {

    private static List<Class<?>> keys(Map<Class<?>, String> map) {
        return new ArrayList<>(map.keySet());
    }

    @Test
    void ordersByClassName() {
        Map<Class<?>, String> handlers = new LinkedHashMap<>();
        handlers.put(String.class, "s");
        handlers.put(Integer.class, "i");
        handlers.put(ArrayList.class, "a");
        TreeMap<Class<?>, String> ordered = VillageHandlerOrder.byName(handlers);
        assertEquals(Arrays.<Class<?>>asList(Integer.class, String.class, ArrayList.class), keys(ordered));
        ordered.put(Byte.class, "b");
        assertEquals(Arrays.<Class<?>>asList(Byte.class, Integer.class, String.class, ArrayList.class), keys(ordered));
        assertEquals("i", ordered.get(Integer.class));
    }

    @Test
    void insertionOrderDoesNotMatter() {
        Map<Class<?>, String> forward = new HashMap<>();
        Map<Class<?>, String> backward = new LinkedHashMap<>();
        List<Class<?>> classes = Arrays.asList(Long.class, Short.class, Double.class, Float.class, Character.class);
        for (Class<?> c : classes) {
            forward.put(c, c.getSimpleName());
        }
        for (int i = classes.size() - 1; i >= 0; i--) {
            backward.put(
                classes.get(i),
                classes.get(i)
                    .getSimpleName());
        }
        assertEquals(keys(VillageHandlerOrder.byName(forward)), keys(VillageHandlerOrder.byName(backward)));
    }
}
