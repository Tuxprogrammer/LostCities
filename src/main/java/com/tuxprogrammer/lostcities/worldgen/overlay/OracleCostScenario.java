package com.tuxprogrammer.lostcities.worldgen.overlay;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.compat.HeightOracles;
import com.tuxprogrammer.lostcities.layout.landmark.CityNearness;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.scenario.Scenario;
import com.tuxprogrammer.lostcities.scenario.ScenarioContext;
import com.tuxprogrammer.lostcities.scenario.ScenarioRegistry;
import com.tuxprogrammer.lostcities.worldgen.height.TerrainHeightOracle;

/**
 * {@code oracle.cost} (manual only): what each structure oracle costs per call on land it has not seen, so a budget
 * miss (B4) can be put on the oracle that causes it. For each factory of {@link StructureOracles}, in order, it builds
 * a fresh oracle over a fresh height oracle and asks it for {@value #QUERIES} 3 × 3-chunk rectangles 4,096 blocks
 * apart, each the first query of its area, and prints {@code [LCNH-ORACLE-COST] <name> p50 p95 max total}. The
 * height work an oracle causes is counted in its cost, as it is in a chunk's phase.
 *
 * <p>
 * Server thread only. Side: server.
 */
public final class OracleCostScenario implements Scenario {

    /** Queries per oracle. */
    static final int QUERIES = 64;
    /** Blocks between query areas, so each query is cold. */
    static final int SPACING_CHUNKS = 256;

    /**
     * Adds the scenario to a registry.
     *
     * @param registry where to add it
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new OracleCostScenario());
    }

    @Override
    public String name() {
        return "oracle.cost";
    }

    @Override
    public boolean manualOnly() {
        return true;
    }

    @Override
    public int ticks() {
        return 0;
    }

    @Override
    public void setup(ScenarioContext context) {}

    @Override
    public void verify(ScenarioContext context) {
        List<StructureOracles.Factory> factories = StructureOracles.factories();
        List<String> names = StructureOracles.factoryNames();
        for (int i = 0; i < factories.size(); i++) {
            TerrainHeightOracle heights = HeightOracles.create(context.world());
            StructureOracle oracle = factories.get(i)
                .forWorld(context.world(), heights, 2, CityNearness.ANYWHERE);
            if (oracle == null || oracle == StructureOracle.NONE) {
                LostCities.LOG.info("[LCNH-ORACLE-COST] {} none", names.get(i));
                continue;
            }
            long[] nanos = new long[QUERIES];
            for (int q = 0; q < QUERIES; q++) {
                int cx = 40_000 + (q % 8) * SPACING_CHUNKS + i * 9 * SPACING_CHUNKS;
                int cz = -40_000 + (q / 8) * SPACING_CHUNKS;
                long start = System.nanoTime();
                oracle.landmarksTouching(cx - 1, cz - 1, cx + 1, cz + 1);
                nanos[q] = System.nanoTime() - start;
            }
            long[] sorted = nanos.clone();
            Arrays.sort(sorted);
            long total = 0;
            for (long n : nanos) {
                total += n;
            }
            LostCities.LOG.info(
                "[LCNH-ORACLE-COST] {}",
                String.format(
                    Locale.ROOT,
                    "%s p50 %.3f ms p95 %.3f ms max %.3f ms total %.1f ms",
                    names.get(i),
                    sorted[QUERIES / 2] / 1e6,
                    sorted[QUERIES * 95 / 100] / 1e6,
                    sorted[QUERIES - 1] / 1e6,
                    total / 1e6));
        }
    }
}
