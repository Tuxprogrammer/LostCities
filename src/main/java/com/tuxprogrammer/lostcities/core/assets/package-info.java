/**
 * The glue between the pure-Java asset system and the game: loading the assets at {@code postInit} against Forge's
 * block registry ({@link com.tuxprogrammer.lostcities.core.assets.LoadedAssets}), the asset part of the dev self-check,
 * the asset scenarios, and building biome descriptors from a world. Holds Minecraft types, so it is not part of
 * {@code asset}.
 *
 * <p>
 * Side: both (server-side use).
 */
package com.tuxprogrammer.lostcities.core.assets;
