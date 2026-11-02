package com.tuxprogrammer.lostcities.network.validation;

/**
 * Why a packet was dropped. Plain Java, so tests assert on it without a game.
 *
 * <p>
 * Side: both.
 */
public enum Rejection {

    /** An index, enum ordinal, length or other bounded value was outside its legal range. */
    OUT_OF_RANGE,
    /** The packet names a dimension other than the one the sender is in. */
    WRONG_DIMENSION,
    /** The target is further from the sender than the reach limit. */
    OUT_OF_REACH,
    /** The sender lacks the permission the packet needs, such as operator level 2 for world tools. */
    UNAUTHORIZED,
    /** The sender is dead or has disconnected. */
    SENDER_INACTIVE,
    /** The target block, tile entity or entity is missing or not loaded. */
    TARGET_MISSING,
    /** The target exists but is not of the type the handler would cast to. */
    WRONG_TARGET_TYPE,
    /** The packet arrived on the side it is not meant for, for example a client-bound packet on the server. */
    WRONG_SIDE,
    /** The bytes could not be decoded. */
    MALFORMED
}
