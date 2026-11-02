package com.tuxprogrammer.lostcities.statics.fixtures;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Fixture for {@code EventHandlerVisibilityTest}: a handler FML could not call. Never registered. */
public class HiddenHandler {

    @SubscribeEvent
    void onTick(TickEvent.ServerTickEvent event) {}
}
