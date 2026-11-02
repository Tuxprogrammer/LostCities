package com.tuxprogrammer.lostcities.compat.ae2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The AE2 values that decide where meteorites fall in one dimension: whether they generate there, the grid cell size
 * and the spawn-chance draws the generator makes before it draws the position. Built from AE2's own config arrays by
 * {@link #of}, which follows {@code MeteoriteWorldGen.generate}'s two loops over them exactly, so the oracle consumes
 * the same draws in the same order.
 *
 * <p>
 * Source:
 * https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/rv3-beta-1095-GTNH/src/main/java/appeng/worldgen/MeteoriteWorldGen.java#L44-L78
 * (the cell size: the dimension's own {@code minMeteoriteDistance} entry, at least 8, else the overworld's; the chance:
 * one {@code nextDouble()} per matching {@code meteoriteSpawnChance} entry, plus one for an overworld entry seen
 * before the dimension's). The pack ships {@code minMeteoriteDistance 0=707} and {@code meteoriteSpawnChance 0=0.3}
 * ({@code config/AppliedEnergistics2/AppliedEnergistics2.cfg}).
 *
 * <p>
 * Immutable. Side: both (plain Java).
 */
public final class MeteoriteSettings {

    /** Settings under which no meteorite generates. */
    public static final MeteoriteSettings OFF = new MeteoriteSettings(false, 0, 0, new double[0], "off");

    private final boolean enabled;
    private final int dimension;
    private final int cellSize;
    private final double[] chanceDraws;
    private final String summary;

    private MeteoriteSettings(boolean enabled, int dimension, int cellSize, double[] chanceDraws, String summary) {
        this.enabled = enabled;
        this.dimension = dimension;
        this.cellSize = cellSize;
        this.chanceDraws = chanceDraws;
        this.summary = summary;
    }

    /**
     * Reads the settings of one dimension from AE2's values.
     *
     * @param generates whether AE2 generates meteorites in this dimension at all (its {@code MeteoriteWorldGen}
     *                  feature and its dimension whitelist)
     * @param dimension the dimension ID
     * @param distances {@code minMeteoriteDistance}, entries {@code <dim>=<blocks>}
     * @param chances   {@code meteoriteSpawnChance}, entries {@code <dim>=<chance>}
     * @return the settings; {@link #OFF} when meteorites do not generate or AE2 would stop on the values (it throws
     *         for a missing overworld entry)
     */
    public static MeteoriteSettings of(boolean generates, int dimension, String[] distances, String[] chances) {
        if (!generates || distances == null || chances == null) {
            return OFF;
        }
        String dim = String.valueOf(dimension);
        int cellSize = Integer.MIN_VALUE;
        try {
            for (String distance : distances) {
                String[] split = distance.split("=");
                if (split[0].equals(dim)) {
                    cellSize = Math.max(8, Integer.parseInt(split[1]));
                    break;
                }
                if (split[0].equals("0") && cellSize == Integer.MIN_VALUE) {
                    cellSize = Integer.parseInt(split[1]);
                }
            }
            List<Double> draws = new ArrayList<>();
            for (String chance : chances) {
                String[] split = chance.split("=");
                if (split[0].equals(dim)) {
                    draws.add(Double.parseDouble(split[1]));
                }
                if (split[0].equals("0") && draws.isEmpty()) {
                    draws.add(Double.parseDouble(split[1]));
                }
            }
            if (cellSize == Integer.MIN_VALUE || cellSize <= 0 || draws.isEmpty()) {
                return OFF;
            }
            double[] values = new double[draws.size()];
            for (int i = 0; i < values.length; i++) {
                values[i] = draws.get(i);
            }
            String summary = "on cell=" + cellSize
                + " margin="
                + margin(cellSize)
                + " chance="
                + Arrays.toString(values);
            return new MeteoriteSettings(true, dimension, cellSize, values, summary);
        } catch (RuntimeException e) {
            // A malformed entry makes AE2 itself throw in generate(); no meteorite generates.
            return OFF;
        }
    }

    /**
     * Returns the margin AE2 keeps between a meteorite and its cell's edges: a tenth of the cell, at least 1.
     *
     * @param cellSize the cell size, blocks
     * @return the margin, blocks
     */
    static int margin(int cellSize) {
        return Math.max(1, cellSize / 10);
    }

    /** Returns whether meteorites generate. */
    public boolean enabled() {
        return enabled;
    }

    /** Returns the dimension the settings are for. */
    public int dimension() {
        return dimension;
    }

    /** Returns the grid cell size, blocks. */
    public int cellSize() {
        return cellSize;
    }

    /**
     * Returns the chances AE2 draws a {@code nextDouble()} against, in order; the last draw decides whether the meteor
     * starts high (a surface meteorite).
     */
    public List<Double> chanceDraws() {
        List<Double> out = new ArrayList<>();
        for (double value : chanceDraws) {
            out.add(value);
        }
        return Collections.unmodifiableList(out);
    }

    double[] chanceDrawsArray() {
        return chanceDraws;
    }

    /** Returns one line for the pinned profile snapshot. */
    public String summary() {
        return summary;
    }

    @Override
    public String toString() {
        return summary;
    }
}
