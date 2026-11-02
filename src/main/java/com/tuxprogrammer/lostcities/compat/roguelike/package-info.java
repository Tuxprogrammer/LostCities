/**
 * The Roguelike Dungeons integration (ROADMAP M08; 0006 §2 and §5; decision log D26, D31): where each grid cell's
 * tower is predicted to stand, as a lot the city builds around, and the steering that keeps the real tower out of
 * city chunks, edge rings and scattered footprints whatever order chunks generate in.
 *
 * <p>
 * Entry points: {@link com.tuxprogrammer.lostcities.compat.roguelike.RoguelikeOracles#forWorld} (the oracle) and
 * {@link com.tuxprogrammer.lostcities.compat.roguelike.RoguelikeSteering} (called by the late mixin
 * {@code mixins.late.MixinRoguelikeDungeon}). The grid and candidate math ({@code RoguelikeGrid}), the settings, the
 * estimated site test ({@code SiteRules}), the oracle and the steering rule are plain Java, tested without Roguelike.
 * {@code RoguelikeConfig} reads the mod's config and loads only when it is present. Roguelike Dungeons is a
 * {@code compileOnly} dependency at the version GTNH 2.9.0 ships, 1.6.6-GTNH.
 *
 * <p>
 * Spec: docs/new-spec/00-pack-integration/coexistence.md § Roguelike Dungeons,
 * docs/design/0006-build-around-other-worldgen.md. Side: server.
 */
package com.tuxprogrammer.lostcities.compat.roguelike;
