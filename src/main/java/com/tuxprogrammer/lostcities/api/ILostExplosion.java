package com.tuxprogrammer.lostcities.api;

/**
 * An explosion that damaged the city when it generated (docs/original-spec/01-core/api.md, {@code ILostExplosion}).
 *
 * <p>
 * Side: server.
 */
public interface ILostExplosion {

    /** Returns the radius, in blocks. */
    int getRadius();

    /** Returns the center's block X. */
    int getCenterX();

    /** Returns the center's block Y. */
    int getCenterY();

    /** Returns the center's block Z. */
    int getCenterZ();
}
