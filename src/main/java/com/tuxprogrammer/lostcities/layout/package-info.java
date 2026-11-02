/**
 * City, street, building, highway and railway layout: which chunk holds what. Pure Java: classes here take seeds,
 * coordinates, profiles and assets and return plain data, and no Minecraft, Forge, FML or other mod's type appears in
 * them. {@code LayoutPurityTest} enforces it (CONTRIBUTING § Architecture, decision log D6).
 *
 * <p>
 * Entry point: {@link com.tuxprogrammer.lostcities.layout.LayoutPlanner}, one per world and dimension, built from
 * {@link com.tuxprogrammer.lostcities.layout.LayoutInputs}. It answers
 * {@link com.tuxprogrammer.lostcities.layout.LayoutPlanner#plan(int, int)} (a chunk's immutable
 * {@link com.tuxprogrammer.lostcities.layout.ChunkPlan}) and
 * {@link com.tuxprogrammer.lostcities.layout.LayoutPlanner#layout(int, int)} (the plan with every neighbor-dependent
 * answer). The layout reads everything else through narrow interfaces it owns (decision log D-M03-7):
 * {@code view} (assets, terrain, highways and railways), {@code landmark} (the structure oracle and the zones around
 * other mods' structures), {@code predefined} (predefined cities). {@code street} is the hierarchical street and
 * bridge field, {@code random} and {@code noise} upstream's generators.
 *
 * <p>
 * Side: both.
 */
package com.tuxprogrammer.lostcities.layout;
