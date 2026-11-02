package com.tuxprogrammer.lostcities.network;

import net.minecraft.entity.player.EntityPlayerMP;

import com.tuxprogrammer.lostcities.network.validation.Sender;

/**
 * The real {@link Sender}: a connected player as the server sees it.
 *
 * <p>
 * Side: server.
 */
public final class PlayerSender implements Sender {

    private final EntityPlayerMP player;

    /**
     * Wraps a player.
     *
     * @param player the player who sent the packet
     */
    public PlayerSender(EntityPlayerMP player) {
        this.player = player;
    }

    /** Returns the wrapped player. */
    public EntityPlayerMP player() {
        return player;
    }

    @Override
    public String name() {
        return player.getCommandSenderName();
    }

    @Override
    public boolean isAlive() {
        return player.isEntityAlive() && player.playerNetServerHandler != null;
    }

    @Override
    public int dimensionId() {
        return player.dimension;
    }

    @Override
    public double x() {
        return player.posX;
    }

    @Override
    public double y() {
        return player.posY;
    }

    @Override
    public double z() {
        return player.posZ;
    }

    /**
     * Asks vanilla, as command permission does: an operator's level from {@code ops.json}, or in single player the
     * owner with cheats on.
     */
    @Override
    public boolean hasPermissionLevel(int level) {
        return level <= 0 || player.canCommandSenderUseCommand(level, LostCitiesNetwork.CHANNEL_NAME);
    }
}
