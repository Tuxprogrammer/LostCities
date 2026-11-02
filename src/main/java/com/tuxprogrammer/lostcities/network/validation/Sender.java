package com.tuxprogrammer.lostcities.network.validation;

/**
 * What validation needs to know about the player who sent a packet, with no Minecraft type, so tests can supply a
 * fake. {@code network.PlayerSender} is the real one.
 *
 * <p>
 * Side: server.
 */
public interface Sender {

    /** Returns the sender's name, for the log. */
    String name();

    /** Returns {@code true} while the sender is alive and connected. */
    boolean isAlive();

    /** Returns the ID of the dimension the sender is in. */
    int dimensionId();

    /** Returns the sender's X position, in blocks. */
    double x();

    /** Returns the sender's Y position (feet), in blocks. */
    double y();

    /** Returns the sender's Z position, in blocks. */
    double z();

    /**
     * Returns whether the sender holds a permission level, as vanilla commands check it.
     *
     * @param level the level: 0 for any player, 2 for vanilla's game masters (operators)
     * @return {@code true} when the sender may act at that level
     */
    boolean hasPermissionLevel(int level);
}
