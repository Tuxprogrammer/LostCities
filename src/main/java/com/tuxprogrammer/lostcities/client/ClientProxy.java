package com.tuxprogrammer.lostcities.client;

import com.tuxprogrammer.lostcities.CommonProxy;
import com.tuxprogrammer.lostcities.core.DevEnvironment;

import cpw.mods.fml.common.event.FMLInitializationEvent;

/**
 * The client's proxy. FML instantiates it by name from {@code LostCities.proxy}'s {@code @SidedProxy}, so no common
 * class refers to it and a dedicated server never loads it.
 *
 * <p>
 * The client needs almost nothing of its own: the config screen comes from GTNHLib, which opens it from the Mods list
 * for any registered config when the mod has no {@code guiFactory} (docs/new-spec/09-client-ui/index.md, D31). The
 * only client code is the dev-only {@link MainMenuProbe}.
 *
 * <p>
 * Side: client.
 */
public class ClientProxy extends CommonProxy {

    /**
     * Does the common setup, then registers the main-menu probe when asked for.
     *
     * @param event FML's initialization event
     */
    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        if (DevEnvironment.testHooksEnabled() && Boolean.getBoolean(MainMenuProbe.PROPERTY)) {
            MainMenuProbe.register();
        }
    }
}
