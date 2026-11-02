package com.tuxprogrammer.lostcities.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraft.world.World;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.config.profile.ApiProfiles;
import com.tuxprogrammer.lostcities.config.profile.ProfileRegistry;

import sun.misc.Unsafe;

/** {@link ILostCities} and {@link LostCitiesAPI}: the entry point, and the upstream API bugs it avoids. */
public class ILostCitiesTest {

    @Test
    public void theEntryPointLoadsTheImplementation() {
        ILostCities api = LostCitiesAPI.get();
        assertNotNull(api);
        assertSame(api, LostCitiesAPI.get());
        assertEquals("lostcitiesnh", LostCitiesAPI.MODID);
    }

    @Test
    public void doesNotReproduceCORE01_noClientProfileHandoff() {
        for (Method method : ILostCities.class.getMethods()) {
            assertFalse(
                method.getName()
                    .equals("setOverworldProfile"),
                "setOverworldProfile reached a client class from common code");
        }
    }

    @Test
    public void doesNotReproduceCORE02_aClientWorldOrNoWorldAnswersNull() throws Exception {
        assertNull(
            LostCitiesAPI.get()
                .getLostInfo(null));
        World client = clientWorld();
        assertNull(
            LostCitiesAPI.get()
                .getLostInfo(client),
            "a client world answers null instead of failing a cast");
    }

    /** A world object marked remote, made without running a constructor (nothing of it is used but the flag). */
    private static World clientWorld() throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Unsafe unsafe = (Unsafe) field.get(null);
        World world = (World) unsafe.allocateInstance(net.minecraft.client.multiplayer.WorldClient.class);
        Field remote = World.class.getField("isRemote");
        unsafe.putBoolean(world, unsafe.objectFieldOffset(remote), true);
        assertTrue(world.isRemote);
        return world;
    }

    @Test
    public void doesNotReproduceCORE03_registeredCallbacksBuildProfiles() {
        ApiProfiles api = new ApiProfiles();
        api.register(setup -> setup.createProfile("callback_ran", "default"));
        ProfileRegistry registry = new ProfileRegistry();
        registry.applyApi(api);
        assertNotNull(registry.get("callback_ran"));
    }

    @Test
    public void doesNotReproduceCORE06_noInMemoryDimensionRegistration() throws Exception {
        for (Method method : ILostCities.class.getMethods()) {
            assertFalse(
                method.getName()
                    .equals("registerDimension"),
                "a dimension registration was lost on every cache reset");
        }
        ApiProfiles api = new ApiProfiles();
        api.register(setup -> setup.createProfile("survives", "default"));
        api.freeze();
        ProfileRegistry registry = new ProfileRegistry();
        Path folder = Files.createTempDirectory("lcnh-api");
        try {
            for (int i = 0; i < 3; i++) {
                registry.load(folder);
                registry.applyApi(api);
                assertNotNull(registry.get("survives"));
            }
        } finally {
            Files.deleteIfExists(folder);
        }
    }
}
