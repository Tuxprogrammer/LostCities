package com.tuxprogrammer.lostcities.api;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/api/ILostCities.java
// Avoids: CORE-01, CORE-02, CORE-03, CORE-06

import java.util.function.Consumer;

import net.minecraft.world.World;

/**
 * The API's root, from {@link LostCitiesAPI#get()} (docs/api.md; docs/original-spec/01-core/api.md).
 *
 * <p>
 * Changes from upstream:
 * <ul>
 * <li>Avoids CORE-02: {@link #getLostInfo} takes any world and answers {@code null} for a client world instead of
 * failing a cast.</li>
 * <li>Avoids CORE-01: there is no {@code setOverworldProfile}; no common code reaches a client class.</li>
 * <li>Avoids CORE-03: profile callbacks run, every time the profiles are read, and are frozen when a server starts,
 * before any world is pinned; upstream's ran never.</li>
 * <li>Avoids CORE-06: there is no {@code registerDimension}; which dimension has cities is the server's
 * {@code dimensionProfiles} setting, pinned per world.</li>
 * </ul>
 *
 * <p>
 * Side: both; call on the server thread.
 */
public interface ILostCities {

    /**
     * Returns what Lost Cities planned for a world. The answers come from the same immutable layout generation uses,
     * so they agree with what is, or will be, generated; asking generates no chunk.
     *
     * @param world any world
     * @return the world's information, or {@code null} for a client world, a world whose dimension has no pinned
     *         profile with cities, or {@code null}
     */
    ILostCityInformation getLostInfo(World world);

    /**
     * Registers a callback that adds profiles. Callbacks run on the server thread each time the profiles are read: when
     * a server is about to start and on {@code /lostcities profile reload}. A profile they create can be named in
     * {@code dimensionProfiles} like a built-in one.
     *
     * @param callback called with a setup object; must be deterministic
     * @throws IllegalStateException after the first server has started: the set is frozen then
     */
    void registerProfileSetup(Consumer<ILostCityProfileSetup> callback);
}
