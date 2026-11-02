package com.tuxprogrammer.lostcities.layout.landmark;

import java.util.Collections;
import java.util.List;

/**
 * Tells the layout which other mods' structures (landmarks) lie near a chunk rectangle, so the city can build around
 * them (0006 §2). The glue builds one aggregate oracle from every enabled per-mod oracle in {@code compat}; the layout
 * sees only this interface.
 *
 * <p>
 * Contract, which every implementation must keep because the layout's determinism rests on it:
 * <ul>
 * <li><b>Pure.</b> The answer is a function of the world seed, the coordinates, the biome provider, the height oracle
 * and other mods' config; never of blocks, loaded or generated chunks, saved data or {@code world.rand}.</li>
 * <li><b>Complete.</b> Every landmark whose footprint touches the rectangle is returned, whatever rectangle was asked
 * before.</li>
 * <li><b>Stable identities.</b> The same structure always comes back with the same {@link Landmark#id()}.</li>
 * </ul>
 * The order of the returned list does not matter: the layout sorts by identity.
 *
 * <p>
 * Side: server (the layout runs where chunks generate).
 */
public interface StructureOracle {

    /** An oracle that knows no structures: the layout is upstream's, without coexistence. */
    StructureOracle NONE = (minChunkX, minChunkZ, maxChunkX, maxChunkZ) -> Collections.emptyList();

    /**
     * Returns every landmark whose footprint touches a chunk rectangle (inclusive).
     *
     * @param minChunkX smallest chunk X
     * @param minChunkZ smallest chunk Z
     * @param maxChunkX largest chunk X
     * @param maxChunkZ largest chunk Z
     * @return the landmarks, in any order
     */
    List<Landmark> landmarksTouching(int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ);
}
