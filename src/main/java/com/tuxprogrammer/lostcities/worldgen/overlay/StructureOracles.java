package com.tuxprogrammer.lostcities.worldgen.overlay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.world.World;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.compat.lootgames.LootGamesOracles;
import com.tuxprogrammer.lostcities.compat.ae2.Ae2Oracles;
import com.tuxprogrammer.lostcities.compat.roguelike.RoguelikeOracles;
import com.tuxprogrammer.lostcities.compat.rwg2.Rwg2Landmarks;
import com.tuxprogrammer.lostcities.compat.villagenames.VillageNamesOracles;
import com.tuxprogrammer.lostcities.compat.witchery.WitcheryOracles;
import com.tuxprogrammer.lostcities.layout.landmark.Landmark;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.worldgen.height.HeightProbe;
import com.tuxprogrammer.lostcities.worldgen.height.TerrainHeightOracle;

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
         * @param world          the world being wrapped
         * @param heights        the world's height oracle, for ground estimates
         * @param landmarkMargin the pinned profile's {@code landmarkMargin}, blocks
         * @return the oracle; {@link StructureOracle#NONE} when the mod is absent or off
         */
        StructureOracle forWorld(World world, TerrainHeightOracle heights, int landmarkMargin);
    }

    private StructureOracles() {}

    /**
     * Returns the oracle factories, in a fixed order.
     *
     * @return the factories
     */
    public static List<Factory> factories() {
        List<Factory> factories = new ArrayList<>();
        // One line per mod (0006 §2). VillageNames villages and ocean monuments (M05, D27, D32, D84 to D92); the
        // overload with the height oracle, since the one without builds a second classic RWG generator. Roguelike
        // Dungeons, Witchery and RWG2's terrain landmarks (M08). Roguelike and Witchery stay wired although the city
        // builds over them by default: the steering and the Witchery veto read their predicted sites. LootGames and
        // AE2 meteorites are not landmarks (D-landmark-policy-1): their oracles serve only their scenarios.
        factories.add(VillageNamesOracles::forWorld);
        factories.add(RoguelikeOracles::forWorld);
        factories.add(WitcheryOracles::forWorld);
        factories.add(Rwg2Landmarks::forWorld);
        return factories;
    }

    /**
     * Returns the name of each factory of {@link #factories()}, in the same order, for log lines.
     *
     * @return the names
     */
    public static List<String> factoryNames() {
        return java.util.Arrays.asList("villagenames", "roguelike", "witchery", "rwg2");
    }

    /**
     * Returns the composite oracle of a world over every factory.
     *
     * @param world          the world
     * @param heights        the world's height oracle
     * @param landmarkMargin the pinned profile's {@code landmarkMargin}, blocks
     * @return the oracle; {@link StructureOracle#NONE} when no factory knows anything
     */
    public static StructureOracle forWorld(World world, TerrainHeightOracle heights, int landmarkMargin) {
        return compose(build(world, heights, landmarkMargin, factories()));
    }

    /**
     * Returns the settings of every wired structure generator that decide where its structures go, one line, for the
     * pinned snapshot (D119): VillageNames', Roguelike Dungeons', Witchery's, LootGames', AE2's and RWG2's landmarks'.
     *
     * @param world the world
     * @return the summary
     */
    public static String settingsSummary(World world) {
        return "villagenames=" + VillageNamesOracles.settingsSummary(world)
            + " roguelike="
            + RoguelikeOracles.settingsSummary(world)
            + " witchery="
            + WitcheryOracles.settingsSummary(world)
            + " lootgames="
            + LootGamesOracles.settingsSummary(world)
            + " ae2="
            + Ae2Oracles.settingsSummary(world)
            + " rwg2="
            + Rwg2Landmarks.settingsSummary(world);
    }

    /**
     * Builds each factory's oracle for a world.
     *
     * @param world          the world
     * @param heights        the world's height oracle
     * @param landmarkMargin the pinned profile's {@code landmarkMargin}, blocks
     * @param factories      the factories
     * @return the oracles that are not {@link StructureOracle#NONE}
     */
    static List<StructureOracle> build(World world, TerrainHeightOracle heights, int landmarkMargin,
        List<Factory> factories) {
        List<StructureOracle> oracles = new ArrayList<>();
        for (Factory factory : factories) {
            StructureOracle oracle;
            try {
                oracle = factory.forWorld(world, heights, landmarkMargin);
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
                    if (HeightProbe.isProbe(e)) {
                        // The plan prefetcher's probe: a ground sample is not cached yet; nothing is wrong.
                        throw e;
                    }
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
