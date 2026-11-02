package com.tuxprogrammer.lostcities.statics.fixtures;

import net.minecraft.client.Minecraft;

/** Fixture for {@code ClientIsolationTest}: common-looking code that touches a client class. Never run. */
public class UsesClientClass {

    /** Refers to {@code Minecraft}, which a dedicated server does not have. */
    public void tick() {
        Minecraft.getMinecraft();
    }
}
