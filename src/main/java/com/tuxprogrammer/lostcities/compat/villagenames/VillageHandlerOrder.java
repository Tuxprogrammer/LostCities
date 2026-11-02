package com.tuxprogrammer.lostcities.compat.villagenames;

import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import com.tuxprogrammer.lostcities.LostCities;

import cpw.mods.fml.common.registry.VillagerRegistry;

/**
 * Pins the order in which Forge asks village-creation handlers for their piece weights to the handlers' component
 * class names, so the same seed builds the same villages on every server run (decision log D-M05O-3).
 *
 * <p>
 * Why: FML keeps the handlers in a {@code HashMap} keyed by {@code Class}, whose iteration order follows identity
 * hash codes and so changes from one JVM run to the next. {@code VillagerRegistry.addExtraVillageComponents} walks
 * that map, each handler draws from the village's {@code Random} (VillageNames' own ~150 building handlers among
 * them), and the piece list keeps that order, so the town center, every building and every path of a VillageNames
 * village depend on the run. A village already started keeps its plan in {@code data/Village.dat}, but a world
 * restarted, or the same seed on a new server, gets other villages, and no oracle could predict them. A map ordered by
 * class name makes the order, and with it every village, a function of the seed, the config and the installed mods.
 * It is the only change: the same handlers, asked with the same arguments.
 *
 * <p>
 * Reflection, not a mixin: the map is a private field of FML (not obfuscated), and replacing its contents once at
 * {@code postInit} needs no hook in any method.
 *
 * <p>
 * Source: Forge 10.13.4.1614 {@code cpw.mods.fml.common.registry.VillagerRegistry} (fields
 * {@code villageCreationHandlers}, {@code addExtraVillageComponents}), read from the 1.7.10 server with Forge applied
 * (no pinned public source); VillageNames 4.5.21-GTNH registers its buildings as handlers,
 * https://github.com/GTNewHorizons/VillageNames/blob/4.5.21-GTNH/src/main/java/astrotibs/villagenames/VillageNames.java#L638-L642,
 * and its piece list calls the registry,
 * https://github.com/GTNewHorizons/VillageNames/blob/4.5.21-GTNH/src/main/java/astrotibs/villagenames/village/StructureVillageVN.java#L311.
 *
 * <p>
 * Side: both (the registry is common), applied once at {@code postInit}.
 */
public final class VillageHandlerOrder {

    /** The private field of {@code VillagerRegistry} that holds the handlers. */
    static final String FIELD = "villageCreationHandlers";

    /** Orders classes by name. */
    static final Comparator<Class<?>> BY_NAME = Comparator.comparing(Class::getName);

    private static boolean pinned;

    private VillageHandlerOrder() {}

    /**
     * Returns a map with the same entries, iterated by class name, that keeps that order for entries added later.
     *
     * @param handlers the handlers by component class
     * @param <V>      the handler type
     * @return the ordered map
     */
    static <V> TreeMap<Class<?>, V> byName(Map<Class<?>, V> handlers) {
        TreeMap<Class<?>, V> ordered = new TreeMap<>(BY_NAME);
        ordered.putAll(handlers);
        return ordered;
    }

    /** Returns whether the order is pinned in this game. */
    public static synchronized boolean isPinned() {
        return pinned;
    }

    /**
     * Replaces FML's handler map with one ordered by class name, once.
     *
     * @return the number of handlers, or −1 when the field could not be replaced (logged)
     */
    public static synchronized int pin() {
        try {
            Field field = VillagerRegistry.class.getDeclaredField(FIELD);
            field.setAccessible(true);
            VillagerRegistry registry = VillagerRegistry.instance();
            @SuppressWarnings("unchecked")
            Map<Class<?>, VillagerRegistry.IVillageCreationHandler> current = (Map<Class<?>, VillagerRegistry.IVillageCreationHandler>) field
                .get(registry);
            if (!(current instanceof TreeMap && ((TreeMap<?, ?>) current).comparator() == BY_NAME)) {
                field.set(registry, byName(current));
            }
            pinned = true;
            return current.size();
        } catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
            LostCities.LOG.warn(
                "[LCNH-VN] could not pin the order of FML's village-creation handlers; VillageNames villages will"
                    + " differ between server runs and the village oracle cannot predict them: {}",
                e.toString());
            return -1;
        }
    }
}
