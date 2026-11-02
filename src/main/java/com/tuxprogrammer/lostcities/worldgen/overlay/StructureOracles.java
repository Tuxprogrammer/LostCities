package com.tuxprogrammer.lostcities.worldgen.overlay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.world.World;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.layout.landmark.Landmark;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;

/**
 * The one place every structure oracle is wired (docs/design/0006-build-around-other-worldgen.md §2): a list of
 * per-mod oracle factories, each turning a world into a {@link StructureOracle} (empty when its mod is absent or
 * switched off), composed into the single oracle the world's {@code LayoutPlanner} gets.
 *
 * <p>
 * The composite asks every oracle and concatenates the answers; the layout sorts landmarks by identity, so the order of
 * the factories never changes a world. Every factory must keep {@link StructureOracle}'s contract (pure, complete,
 * stable identities); one that throws is logged and treated as knowing nothing for that query, so a broken
 * integration never stops generation.
 *
 * <p>
 * Server thread only. Side: server.
 */
public final class StructureOracles {

    /** Makes one mod's oracle for a world. */
    @FunctionalInterface
    public interface Factory {

        /**
         * Returns the oracle of a world.
         *
         * @param world the world being wrapped
         * @return the oracle; {@link StructureOracle#NONE} when the mod is absent or off
         */
        StructureOracle forWorld(World world);
    }

    private StructureOracles() {}

    /**
     * Returns the oracle factories, in a fixed order.
     *
     * @return the factories
     */
    public static List<Factory> factories() {
        List<Factory> factories = new ArrayList<>();
        // ---- Structure oracles, one line per mod. -----------------------------------------------------------------
        // M05 (VillageNames villages and ocean monuments, D27, D32): the main session adds
        // factories.add(com.tuxprogrammer.lostcities.compat.villagenames.VillageNamesOracles::forWorld);
        // when M05O's compat.villagenames lands. M08 adds Roguelike Dungeons, Witchery, LootGames, AE2 and RWG2 here.
        // -------------------------------------------------------------------------------------------------------------
        return factories;
    }

    /**
     * Returns the composite oracle of a world over every factory.
     *
     * @param world the world
     * @return the oracle; {@link StructureOracle#NONE} when no factory knows anything
     */
    public static StructureOracle forWorld(World world) {
        return compose(build(world, factories()));
    }

    /**
     * Builds each factory's oracle for a world.
     *
     * @param world     the world
     * @param factories the factories
     * @return the oracles that are not {@link StructureOracle#NONE}
     */
    static List<StructureOracle> build(World world, List<Factory> factories) {
        List<StructureOracle> oracles = new ArrayList<>();
        for (Factory factory : factories) {
            StructureOracle oracle;
            try {
                oracle = factory.forWorld(world);
            } catch (RuntimeException e) {
                LostCities.LOG.error("A structure oracle failed to start; its structures are not known", e);
                continue;
            }
            if (oracle != null && oracle != StructureOracle.NONE) {
                oracles.add(oracle);
            }
        }
        return oracles;
    }

    /**
     * Composes oracles into one.
     *
     * @param oracles the oracles, in a fixed order
     * @return the composite; {@link StructureOracle#NONE} for none, the oracle itself for one
     */
    public static StructureOracle compose(List<StructureOracle> oracles) {
        if (oracles.isEmpty()) {
            return StructureOracle.NONE;
        }
        List<StructureOracle> fixed = Collections.unmodifiableList(new ArrayList<>(oracles));
        return (minChunkX, minChunkZ, maxChunkX, maxChunkZ) -> {
            List<Landmark> out = new ArrayList<>();
            for (StructureOracle oracle : fixed) {
                try {
                    out.addAll(oracle.landmarksTouching(minChunkX, minChunkZ, maxChunkX, maxChunkZ));
                } catch (RuntimeException e) {
                    LostCities.LOG.error(
                        "A structure oracle failed for chunks ({}, {}) to ({}, {}); treated as empty",
                        minChunkX,
                        minChunkZ,
                        maxChunkX,
                        maxChunkZ,
                        e);
                }
            }
            return out;
        };
    }
}
