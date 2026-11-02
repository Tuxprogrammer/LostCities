package com.tuxprogrammer.lostcities.statics.fixtures;

import net.minecraft.world.World;

/** Fixture for {@code LayoutPurityTest}: layout-like code that takes a Minecraft world. Never run. */
public class ImpureLayout {

    /** Reads the seed from a world instead of taking it as a number. */
    public long seedOf(World world) {
        return world.getSeed();
    }
}
