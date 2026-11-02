package com.tuxprogrammer.lostcities.asset;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * What the asset loader needs to know about the live block registry: whether a registry name is registered. The glue
 * implements it over Forge's block registry at {@code postInit}, after every mod has registered its blocks
 * (CONTRIBUTING § Architecture, registration order); tests implement it over a fixed set of names. Keeping it this
 * small keeps {@code asset} free of Minecraft types (D6).
 *
 * <p>
 * Side: both.
 */
public interface BlockRegistryView {

    /**
     * Returns whether a block with this registry name is registered.
     *
     * @param registryName the name exactly as registered, {@code modid:name}
     * @return whether it exists
     */
    boolean exists(String registryName);

    /**
     * Returns a view over a fixed set of names.
     *
     * @param names the registry names that exist
     * @return the view
     */
    static BlockRegistryView of(Collection<String> names) {
        Set<String> copy = Collections.unmodifiableSet(new HashSet<>(names));
        return copy::contains;
    }
}
