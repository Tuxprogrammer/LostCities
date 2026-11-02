package com.tuxprogrammer.lostcities.statics.fixtures;

import net.minecraft.client.Minecraft;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Fixture for {@code ClientIsolationTest}: a client reference inside a member FML strips on the server. Never run. */
public class SideOnlyClientMember {

    /** Client-only, so its reference to {@code Minecraft} is allowed, and so is the lambda inside it. */
    @SideOnly(Side.CLIENT)
    public void render() {
        Runnable later = () -> Minecraft.getMinecraft();
        later.run();
    }

    /** Common code that does not touch the client. */
    public int common() {
        return 1;
    }
}
