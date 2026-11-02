package com.tuxprogrammer.lostcities.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.api.event.LostCityEvent;
import com.tuxprogrammer.lostcities.config.profile.ApiProfiles;
import com.tuxprogrammer.lostcities.config.profile.ProfileRegistry;

/** {@link ILostCityProfile} and the API's surface: no misspelled or dead member, no internal type. */
public class ILostCityProfileTest {

    /** Every type of the API package. */
    static final List<Class<?>> API = Arrays.asList(
        LostCitiesAPI.class,
        ILostCities.class,
        ILostCityProfileSetup.class,
        ILostCityProfile.class,
        ILostCityInformation.class,
        ILostChunkInfo.class,
        ILostCityInfo.class,
        ILostExplosion.class,
        ILostCityAsset.class,
        ILostCityAssetRegistry.class,
        ILostCityBuilding.class,
        ILostCityMultiBuilding.class,
        ILostCityCityStyle.class,
        ILostChunkBlocks.class,
        LostChunkType.class,
        LostRoadType.class,
        LostLandmarkZone.class,
        LostMultiBuildingInfo.class,
        RailChunkType.class,
        LostCityEvent.class,
        LostCityEvent.PreGenCityChunk.class,
        LostCityEvent.PostGenCityChunk.class,
        LostCityEvent.PostGenOutsideChunk.class,
        LostCityEvent.PreExplosion.class);

    @Test
    public void doesNotReproduceCORE38_noInternalTypeInAnApiSignature() {
        List<String> leaks = new ArrayList<>();
        for (Class<?> type : API) {
            for (Method method : type.getDeclaredMethods()) {
                if (!java.lang.reflect.Modifier.isPublic(method.getModifiers()) || method.isSynthetic()) {
                    continue;
                }
                List<Type> types = new ArrayList<>(Arrays.asList(method.getGenericParameterTypes()));
                types.add(method.getGenericReturnType());
                for (Type used : types) {
                    String name = used.getTypeName();
                    if (name.contains("com.tuxprogrammer.lostcities.")
                        && !name.matches(".*com\\.tuxprogrammer\\.lostcities\\.api\\.[A-Za-z.$]+.*")) {
                        leaks.add(type.getSimpleName() + "." + method.getName() + ": " + name);
                    }
                }
            }
        }
        assertTrue(leaks.isEmpty(), "internal types in API signatures: " + leaks);
    }

    @Test
    public void doesNotReproduceCORE38_noMisspelledOrFourLevelSetter() throws Exception {
        List<String> names = new ArrayList<>();
        for (Method method : ILostCityProfile.class.getMethods()) {
            names.add(method.getName());
        }
        assertFalse(names.contains("setCityChancle"));
        assertTrue(names.contains("setCityChance"));
        Method levels = ILostCityProfile.class.getMethod("setCityLevelHeights", int[].class);
        assertEquals(1, levels.getParameterCount());
    }

    @Test
    public void everySetterReachesItsOption() {
        ApiProfiles api = new ApiProfiles();
        api.register(setup -> {
            ILostCityProfile profile = setup.createProfile("setters", "default");
            assertEquals("setters", profile.getName());
            profile.setWorldStyle("standard");
            profile.setRuinChance(0.5f, 0.2f, 0.6f);
            profile.setOceanCorrectionBorder(8);
            profile.setOption("editMode", "true");
        });
        ProfileRegistry registry = new ProfileRegistry();
        assertTrue(
            registry.applyApi(api)
                .isEmpty());
        com.tuxprogrammer.lostcities.config.profile.ProfileValues values = registry.get("setters")
            .values();
        assertEquals(0.5f, values.getFloat("ruinChance"), 1e-6);
        assertEquals(0.2f, values.getFloat("ruinMinlevelPercent"), 1e-6);
        assertEquals(0.6f, values.getFloat("ruinMaxlevelPercent"), 1e-6);
        assertEquals(8, values.getInt("oceanCorrectionBorder"));
        assertTrue(values.getBoolean("editMode"));
    }
}
