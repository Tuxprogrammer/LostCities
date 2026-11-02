/**
 * The VillageNames structure oracles (ROADMAP M05; decision log D27, D32): where its villages and ocean monuments
 * stand, predicted without generating anything, so cities build around them and sew villages in (0006).
 *
 * <p>
 * Entry point: {@link com.tuxprogrammer.lostcities.compat.villagenames.VillageNamesOracles#forWorld}. The grid math
 * ({@code VillageGrid}, {@code MonumentGrid}, {@code MapGenSeeds}), the settings, the gates, the ground estimate and
 * both oracles are plain Java, tested without VillageNames. Three classes touch VillageNames and load only when it is
 * present: {@code VillageNamesConfig} (its config), {@code VillageNamesPlans} (its village start, built on a generator
 * of our own) and the scenario's reader of its saved structures. VillageNames is a {@code compileOnly} dependency at
 * the version GTNH 2.9.0 ships, 4.5.21-GTNH.
 *
 * <p>
 * Spec: docs/new-spec/00-pack-integration/coexistence.md § Villages, docs/design/0006-build-around-other-worldgen.md.
 * Side: server.
 */
package com.tuxprogrammer.lostcities.compat.villagenames;
