package com.tuxprogrammer.lostcities.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.network.base.IPacket;
import com.tuxprogrammer.lostcities.LostCities;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.network.NetworkCheckHandler;

/**
 * The channel's version check (docs/new-spec/01-core/index.md, "Network"; avoids CORE-36). Upstream's channel accepted
 * any remote version and registered two packets with empty handlers. Here FML's default check applies: with no
 * {@code acceptableRemoteVersions} and no {@code @NetworkCheckHandler}, a client must have this mod at the same
 * version, which command output needs because it is lang keys the client resolves.
 *
 * <p>
 * Source: FML's {@code NetworkModHolder} in Forge 10.13.4.1614, which ignores the remote side only for
 * {@code acceptableRemoteVersions = "*"} and otherwise checks the remote version against the mod's own.
 */
public class ChannelVersionTest {

    @Test
    public void doesNotReproduceCORE36_theChannelRequiresTheSameModVersionOnBothSides() {
        Mod mod = LostCities.class.getAnnotation(Mod.class);
        assertNotNull(mod);
        assertEquals("", mod.acceptableRemoteVersions(), "FML's default: the same version on both sides");
        for (Method method : LostCities.class.getDeclaredMethods()) {
            assertFalse(
                method.isAnnotationPresent(NetworkCheckHandler.class),
                method.getName() + " would replace FML's version check");
        }
        assertEquals(LostCities.MODID, LostCitiesNetwork.CHANNEL_NAME);
        // Every packet goes through a base class that checks side, sender and bounds; none is an empty handler.
        for (IPacket packet : LostCitiesNetwork.clientboundPackets()) {
            assertTrue(packet instanceof ClientboundPacket, packet.getClass() + " skips the client-bound checks");
        }
        for (IPacket packet : LostCitiesNetwork.serverboundPackets()) {
            assertTrue(packet instanceof ServerboundPacket, packet.getClass() + " skips the server-bound checks");
        }
    }
}
