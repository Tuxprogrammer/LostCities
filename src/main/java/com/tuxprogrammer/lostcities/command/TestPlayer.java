package com.tuxprogrammer.lostcities.command;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.util.IChatComponent;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;

import com.mojang.authlib.GameProfile;

/**
 * A fake player for the command scenarios: an operator or not, as asked, that keeps the chat it is sent, so a scenario
 * can run each subcommand as an operator and as an ordinary player and read what came back (docs/testing/README.md,
 * "commands" scenarios). Forge's {@code FakePlayer} answers every permission check with "no" and drops chat.
 *
 * <p>
 * Test hooks only. Side: server.
 */
public final class TestPlayer extends FakePlayer {

    private final int level;
    private final List<IChatComponent> chat = new ArrayList<>();

    /**
     * Creates a player.
     *
     * @param world the world it stands in
     * @param name  its name, from which its UUID is derived
     * @param level the highest permission level it has: 0 for an ordinary player, 4 for an operator
     */
    public TestPlayer(WorldServer world, String name, int level) {
        super(world, new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)), name));
        this.level = level;
    }

    @Override
    public boolean canCommandSenderUseCommand(int required, String command) {
        return required <= level;
    }

    @Override
    public void addChatMessage(IChatComponent message) {
        chat.add(message);
    }

    /** Returns the chat received since the last {@link #clearChat()}. */
    public List<IChatComponent> chat() {
        return chat;
    }

    /** Forgets the chat received. */
    public void clearChat() {
        chat.clear();
    }

    /**
     * Moves the player without telling any client (it has none).
     *
     * @param x block X
     * @param y block Y of the feet
     * @param z block Z
     */
    public void moveTo(double x, double y, double z) {
        setPosition(x, y, z);
    }
}
