package com.tuxprogrammer.lostcities.editor;

import java.util.UUID;

import com.tuxprogrammer.lostcities.worldgen.overlay.OverlayWorld;
import com.tuxprogrammer.lostcities.worldgen.overlay.Overlays;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;

/**
 * Ends a player's part-editing sessions when the player logs out (avoids CORE-35: upstream kept them in a static map
 * for the life of the game). Sessions also end with their world, which owns them ({@link EditSessions}).
 *
 * <p>
 * Server thread only. Side: server.
 */
public final class EditorEvents {

    /** Registers the handler on FML's bus, where 1.7.10 posts the player login and logout events. */
    public static void register() {
        FMLCommonHandler.instance()
            .bus()
            .register(new EditorEvents());
    }

    /**
     * Drops the player's sessions in every world.
     *
     * @param event FML's logout event
     */
    @SubscribeEvent
    public void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID player = event.player.getUniqueID();
        for (OverlayWorld overlay : Overlays.INSTANCE.all()) {
            overlay.editSessions()
                .end(player);
        }
    }
}
