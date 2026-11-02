/**
 * The LootGames structure oracle (ROADMAP M08; docs/design/0006-build-around-other-worldgen.md §1 and §2): where
 * LootGames digs its puzzle dungeons, predicted without generating anything, so the city leaves each room and the
 * mouth of its tunnel natural ground.
 *
 * <p>
 * Entry point: {@link com.tuxprogrammer.lostcities.compat.lootgames.LootGamesOracles#forWorld}. The site test
 * ({@code RhombGrid}), the settings and the oracle are plain Java, tested without LootGames. {@code LootGamesConfig}
 * and the scenario touch LootGames and load only when it is present. LootGames is a {@code compileOnly} dependency at
 * the version GTNH 2.9.0 ships, 2.2.14.
 *
 * <p>
 * Spec: docs/new-spec/00-pack-integration/coexistence.md § LootGames. Side: server.
 */
package com.tuxprogrammer.lostcities.compat.lootgames;
