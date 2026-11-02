/**
 * Realistic World Gen 2 integration: the height oracle of RWG2 and RWG2 Continents worlds
 * ({@code Rwg2HeightOracle}, docs/design/0005-rwg-overlay.md §4) and the terrain-landmark oracle of its volcanoes,
 * lava-cave openings and mountain-chain river junctions
 * ({@link com.tuxprogrammer.lostcities.compat.rwg2.Rwg2Landmarks}, docs/design/0006-build-around-other-worldgen.md
 * §1).
 *
 * <p>
 * The landmark logic ({@code Rwg2LandmarkOracle}) is plain Java over the {@code Rwg2Landforms} predicates and is tested
 * with fakes; {@code ManagerLandforms} answers them from the world's chunk manager. Every class that names an RWG2
 * type loads only after {@code Loader.isModLoaded("rwg2")}. RWG2 is a {@code compileOnly} dependency at the version
 * GTNH 2.9.0 ships, 2.0.6.
 *
 * <p>
 * Side: server.
 */
package com.tuxprogrammer.lostcities.compat.rwg2;
