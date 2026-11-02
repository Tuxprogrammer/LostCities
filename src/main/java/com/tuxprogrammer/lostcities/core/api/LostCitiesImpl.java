package com.tuxprogrammer.lostcities.core.api;

import java.util.function.Consumer;

import net.minecraft.world.World;

import com.tuxprogrammer.lostcities.api.ILostCities;
import com.tuxprogrammer.lostcities.api.ILostCityInformation;
import com.tuxprogrammer.lostcities.api.ILostCityProfileSetup;
import com.tuxprogrammer.lostcities.config.profile.ApiProfiles;
import com.tuxprogrammer.lostcities.worldgen.overlay.OverlayWorld;
import com.tuxprogrammer.lostcities.worldgen.overlay.Overlays;

/**
 * The API's implementation, which {@code LostCitiesAPI.get()} loads by name (docs/api.md). It holds no state of its
 * own: a world's information is a view over the world's overlay, which the world owns, and the profile callbacks
 * live in {@link ApiProfiles}.
 *
 * <p>
 * Side: both; {@link #getLostInfo} answers on the server only.
 */
public final class LostCitiesImpl implements ILostCities {

    /** The one instance, read by {@code LostCitiesAPI}. */
    public static final LostCitiesImpl INSTANCE = new LostCitiesImpl();

    private LostCitiesImpl() {}

    @Override
    public ILostCityInformation getLostInfo(World world) {
        // Avoids CORE-02: a client world, or any world without real cities, answers null instead of failing a cast.
        if (world == null || world.isRemote) {
            return null;
        }
        OverlayWorld overlay = Overlays.INSTANCE.get(world);
        if (overlay == null || overlay.cities() == null) {
            return null;
        }
        return new LostCityInformationImpl(new OverlaySource(overlay));
    }

    @Override
    public void registerProfileSetup(Consumer<ILostCityProfileSetup> callback) {
        ApiProfiles.INSTANCE.register(callback);
    }
}
