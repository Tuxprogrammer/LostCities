/**
 * The Witchery structure oracle (ROADMAP M08; 0006 §2): where Witchery's covens, shacks, wicker men and hobgoblin huts
 * can stand, predicted without generating anything, so cities build around the union lot of every possible type and
 * facing.
 *
 * <p>
 * Entry point: {@link com.tuxprogrammer.lostcities.compat.witchery.WitcheryOracles#forWorld}. The grid, the settings,
 * the union footprint and the oracle are plain Java, tested without Witchery. Witchery is closed source and has no
 * development jar: {@code WitcheryConfig} and the scenario reach it by class and member names only, through
 * reflection, and only after {@code Loader.isModLoaded("witchery")}. Its contract was learned from the decompiled
 * pack jar (no code copied), as the {@code // Decompiled:} hedges say.
 *
 * <p>
 * Spec: docs/new-spec/00-pack-integration/coexistence.md § Witchery, docs/design/0006-build-around-other-worldgen.md.
 * Side: server.
 */
package com.tuxprogrammer.lostcities.compat.witchery;
