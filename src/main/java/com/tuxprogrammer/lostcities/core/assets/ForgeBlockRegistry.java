package com.tuxprogrammer.lostcities.core.assets;

import com.tuxprogrammer.lostcities.asset.BlockRegistryView;

import cpw.mods.fml.common.registry.GameData;

/**
 * The glue between the asset system and Forge's block registry: answers whether a registry name is registered. Used at
 * {@code postInit}, after every mod has registered its blocks (CONTRIBUTING § Architecture, registration order).
 *
 * <p>
 * Forge's block registry is a defaulted registry: {@code getObject} answers an unknown name with air. That is why
 * upstream's typos became air (CORE-20, which upstream also files as ASSET-13), and why this view asks
 * {@code containsKey}, which only holds for registered names.
 *
 * <p>
 * Source: FML 1.7.10,
 * https://github.com/MinecraftForge/FML/blob/a099592d3d1418245e3af65eda195da244287188/src/main/java/cpw/mods/fml/common/registry/FMLControlledNamespacedRegistry.java
 * ({@code containsKey(String)} against the registry's name map; the default object for {@code getObject}). Side: both.
 */
public final class ForgeBlockRegistry implements BlockRegistryView {

    /** The one instance. */
    public static final ForgeBlockRegistry INSTANCE = new ForgeBlockRegistry();

    private ForgeBlockRegistry() {}

    @Override
    public boolean exists(String registryName) {
        return GameData.getBlockRegistry()
            .containsKey(registryName);
    }
}
