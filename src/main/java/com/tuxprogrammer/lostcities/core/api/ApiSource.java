package com.tuxprogrammer.lostcities.core.api;

import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.worldgen.damage.DamageArea;

/**
 * What the API's answers for one world are read from: its layout planner, its loaded assets, its damage areas and its
 * pinned profile. The overlay of a live world provides it ({@link OverlaySource}); tests build one over a fixture
 * world, so the API is tested without a game.
 *
 * <p>
 * Side: server (plain Java).
 */
public interface ApiSource {

    /** Returns the dimension ID. */
    int dimension();

    /** Returns the pinned profile's name. */
    String profile();

    /** Returns the pinned ground level, block Y. */
    int groundLevel();

    /** Returns the pinned ruin chance, for ruin levels. */
    float ruinChance();

    /** Returns the layout planner generation uses. */
    LayoutPlanner planner();

    /** Returns the loaded assets. */
    AssetSet assets();

    /**
     * Returns the explosions that reach a chunk.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the damage area
     */
    DamageArea damage(int chunkX, int chunkZ);

    /**
     * Returns the scattered structure standing in a chunk: the world style entry's name of the structure whose
     * footprint holds it, from the scattered planner (07-scattered). The default, for a source without one, is none.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the structure's name, or {@code null}
     */
    default String scattered(int chunkX, int chunkZ) {
        return null;
    }
}
