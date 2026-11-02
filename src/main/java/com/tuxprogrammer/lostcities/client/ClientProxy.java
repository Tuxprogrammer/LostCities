package com.tuxprogrammer.lostcities.client;

import com.tuxprogrammer.lostcities.CommonProxy;

/**
 * The client's proxy. FML instantiates it by name from {@code LostCities.proxy}'s {@code @SidedProxy}, so no common
 * class refers to it and a dedicated server never loads it.
 *
 * <p>
 * The client needs nothing of its own yet: the config screen comes from GTNHLib, which opens it from the Mods list for
 * any registered config when the mod has no {@code guiFactory} (docs/new-spec/09-client-ui/index.md, D31).
 *
 * <p>
 * Side: client.
 */
public class ClientProxy extends CommonProxy {}
