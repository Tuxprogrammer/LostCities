/**
 * Pack houses: city-edge lots that hold a house built by another pack mod's own village-component generator, called
 * the way a village calls it and never copied (docs/design/0011-city-edges-and-character.md, pack houses).
 *
 * <p>
 * Entry point: {@link com.tuxprogrammer.lostcities.compat.packhouses.PackHouses#build}. The mods are reached through
 * Forge's {@code VillagerRegistry} (their {@code IVillageCreationHandler}s) and, for vanilla's houses, through the
 * piece classes' own factories; each mod is a soft dependency, skipped silently when it is absent or its own config
 * leaves its house unregistered. {@link com.tuxprogrammer.lostcities.compat.packhouses.PieceFields} is the one place
 * that reaches into a piece by reflection.
 *
 * <p>
 * Side: server.
 */
package com.tuxprogrammer.lostcities.compat.packhouses;
