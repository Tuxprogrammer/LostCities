package com.tuxprogrammer.lostcities.worldgen.plan;

import com.tuxprogrammer.lostcities.worldgen.damage.Repairs;

/**
 * The source of chunk plans the overlay reads: the one seam between the layout and the overlay. M05 implements it
 * over M03's layout and M02's assets; M04 implements it with the synthetic test city
 * ({@code worldgen.synthetic.SyntheticCityPlan}).
 *
 * <p>
 * Contract:
 * <ul>
 * <li>{@link #chunk} is pure: the same coordinates always give an equal plan, for any order of calls, so neighbors
 * agree and existing worlds stay valid (CONTRIBUTING § World generation rules);</li>
 * <li>it never touches the world: no block reads, no chunk loads, no live generator. Heights come from the height
 * oracle and structures from the structure oracles (0005 §4, 0006 §2);</li>
 * <li>it may be called for any chunk, generated or not, including far outside cities, and for the same chunk many
 * times: from the terrain phase (the chunk and up to three chunks around it), the population vetoes and the write
 * guard (every block decoration writes during RWG's populate). Wrap implementations in {@link CachedCityPlan}.</li>
 * </ul>
 *
 * <p>
 * Side: server (world generation), and plain Java in tests.
 */
@FunctionalInterface
public interface CityPlan {

    /** A plan with no cities: every chunk is {@link ChunkKind#NONE} at level 0. */
    CityPlan EMPTY = (chunkX, chunkZ) -> ChunkPlan.none(chunkX, chunkZ, 0, 71, 71, false);

    /**
     * Returns the plan of one chunk.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return its plan; never {@code null}
     */
    ChunkPlan chunk(int chunkX, int chunkZ);

    /**
     * Returns the world's partial repairs of explosion damage (docs/design/0011-city-edges-and-character.md §4); none
     * unless the plan source knows the assets.
     *
     * @return the repairs
     */
    default Repairs repairs() {
        return Repairs.NONE;
    }
}
