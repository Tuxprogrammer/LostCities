package com.tuxprogrammer.lostcities.layout;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;

/**
 * Preview dumper: writes the {@code rarecities} city centers of today's {@link CityMap} next to jittered-grid centers
 * as a CSV, for {@code tools/preview/jitter_preview.py}. It does nothing in a normal build: it runs only when the
 * system property {@value #PROPERTY} is set or the marker file {@value #MARKER} exists in the project directory.
 * Terrain filters are not applied to either set.
 */
public class JitteredGridPreviewDump {

    static final String PROPERTY = "lc.jitterPreview";
    static final String MARKER = "build/jitter-preview.enable";
    static final String OUTPUT = "build/jitter-preview/centers.csv";

    static final long WORLD_SEED = 5253073417701929942L;
    static final int WINDOW_MIN = -512;
    static final int WINDOW_MAX = 512;
    static final int CELL = 64;
    static final int JITTER_INSET = 16;
    static final int JITTER_SPAN = 32;
    static final int MIN_RADIUS = 100;
    static final int MAX_RADIUS = 256;

    /** Per-cell seed multipliers: large odd numbers, unrelated to CityMap's. */
    private static final long CELL_Z = 25214903917L * 2L + 1L;
    private static final long CELL_X = 2685821657736338717L;

    /** One generated city center: chunk coordinates and radius in blocks. */
    static final class Center {

        final int chunkX;
        final int chunkZ;
        final int radius;

        Center(int chunkX, int chunkZ, int radius) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.radius = radius;
        }
    }

    /**
     * Returns the jittered-grid city centers whose 64 x 64 chunk cells lie in {@code [min, max)} on both axes. Each
     * cell holds one center at {@code cellOrigin + 16 + r}, {@code r} uniform in {@code [0, 32)} on each axis (the
     * middle half of the cell, so neighbors are at least 32 chunks apart), with a radius drawn as CityMap does:
     * {@code minRadius + nextInt(maxRadius - minRadius)}. Each cell's {@link Random} is seeded from the world seed and
     * the cell coordinates alone, so the result is deterministic and independent of the window.
     *
     * @param worldSeed the world seed
     * @param min       first chunk of the window, a multiple of 64
     * @param max       end of the window (exclusive), a multiple of 64
     * @param minRadius smallest radius in blocks
     * @param maxRadius end of the radius range (exclusive), blocks
     */
    static List<Center> jitteredGrid(long worldSeed, int min, int max, int minRadius, int maxRadius) {
        List<Center> out = new ArrayList<>();
        for (int cellX = Math.floorDiv(min, CELL); cellX < Math.floorDiv(max, CELL); cellX++) {
            for (int cellZ = Math.floorDiv(min, CELL); cellZ < Math.floorDiv(max, CELL); cellZ++) {
                Random random = new Random(worldSeed + cellZ * CELL_Z + cellX * CELL_X);
                int x = cellX * CELL + JITTER_INSET + random.nextInt(JITTER_SPAN);
                int z = cellZ * CELL + JITTER_INSET + random.nextInt(JITTER_SPAN);
                int radius = minRadius + random.nextInt(Math.max(1, maxRadius - minRadius));
                out.add(new Center(x, z, radius));
            }
        }
        return out;
    }

    @Test
    public void dumpCenters() throws IOException {
        Assumptions.assumeTrue(
            System.getProperty(PROPERTY) != null || new File(MARKER).exists(),
            "preview dump not requested");
        LayoutProfile profile = LayoutProfile.builder()
            .cityChance(0.00025)
            .cityRadius(MIN_RADIUS, MAX_RADIUS)
            .build();
        LayoutPlanner planner = LayoutFixtures
            .planner(WORLD_SEED, profile, LayoutFixtures.flatTerrain(), StructureOracle.NONE);
        File out = new File(OUTPUT);
        out.getParentFile()
            .mkdirs();
        int today = 0;
        try (PrintWriter w = new PrintWriter(out, StandardCharsets.UTF_8.name())) {
            w.println("set,chunkX,chunkZ,radiusBlocks");
            for (int x = WINDOW_MIN; x < WINDOW_MAX; x++) {
                for (int z = WINDOW_MIN; z < WINDOW_MAX; z++) {
                    if (planner.isCityCenter(x, z)) {
                        w.println("today," + x + "," + z + "," + (int) planner.cityRadius(x, z));
                        today++;
                    }
                }
            }
            for (Center c : jitteredGrid(WORLD_SEED, WINDOW_MIN, WINDOW_MAX, MIN_RADIUS, MAX_RADIUS)) {
                w.println("jitter," + c.chunkX + "," + c.chunkZ + "," + c.radius);
            }
        }
        System.out.println("[jitter-preview] today centers: " + today);
    }
}
