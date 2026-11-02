/**
 * The JSON asset system: loading, validation and the in-memory model of buildings, parts, palettes and styles. Pure
 * Java, like {@code layout}: {@code LayoutPurityTest} keeps Minecraft types out (CONTRIBUTING § Architecture). M01 adds
 * only {@link com.tuxprogrammer.lostcities.asset.BlockRef}, the block reference chunk buffers store; M02 fills the
 * rest.
 *
 * <p>
 * Side: both.
 */
package com.tuxprogrammer.lostcities.asset;
