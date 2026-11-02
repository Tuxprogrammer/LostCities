package com.tuxprogrammer.lostcities.layout;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/MultiChunk.java
// Avoids: CITY-02, CORE-21, CITY-25, CITY-26

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

import com.tuxprogrammer.lostcities.layout.landmark.Zone;
import com.tuxprogrammer.lostcities.layout.random.WeightedPicker;
import com.tuxprogrammer.lostcities.layout.view.BuildingView;
import com.tuxprogrammer.lostcities.layout.view.CityStyleView;
import com.tuxprogrammer.lostcities.layout.view.MultiBuildingView;
import com.tuxprogrammer.lostcities.layout.view.MultiSettings;
import com.tuxprogrammer.lostcities.layout.view.RailInfo;
import com.tuxprogrammer.lostcities.layout.view.Selection;

/**
 * Random multibuilding placement in one multichunk, an {@code areasize × areasize} square of chunks: how many
 * multibuildings to try, which ones (weighted by how many chunks of the area each city style governs), and where each
 * fits. Multibuildings never cross a multichunk border.
 *
 * <p>
 * Fixes, each with a regression test:
 * <ul>
 * <li>Avoids CITY-02: the multichunk random includes the world seed and the dimension (decision log D10,
 * D46).</li>
 * <li>Avoids CORE-21 (CITY-09): the style pick never returns {@code null} ({@link WeightedPicker}).</li>
 * <li>Avoids CITY-25: settings with {@code maximum < minimum}, or a multibuilding larger than the area, place nothing
 * instead of throwing; the asset loader rejects them with the file and field first.</li>
 * <li>Avoids CITY-26: the rail-clearance test uses the city level the multibuilding would have at the candidate
 * position, not the area's top-left chunk's level.</li>
 * </ul>
 * Deviation (0006 §3): a position is also rejected when its footprint covers a landmark core, an avoid ring, an edge
 * ring or a chunk a detour or village lane runs through.
 *
 * <p>
 * Spec: docs/original-spec/04-city-layout/multibuildings.md. Side: both.
 */
final class MultiChunk {

    /** Multichunk seed multipliers (with the world seed and dimension added, D10). */
    static final long SEED_X = 797013493L;
    static final long SEED_Z = 295085213L;

    private final int areaSize;
    private final int topLeftX;
    private final int topLeftZ;
    private final Placement[][] grid;

    /** One footprint chunk's share of a placed multibuilding. */
    static final class Placement {

        final String name;
        final int offsetX;
        final int offsetZ;
        final int dimX;
        final int dimZ;

        Placement(String name, int offsetX, int offsetZ, int dimX, int dimZ) {
            this.name = name;
            this.offsetX = offsetX;
            this.offsetZ = offsetZ;
            this.dimX = dimX;
            this.dimZ = dimZ;
        }
    }

    private MultiChunk(int areaSize, int multiX, int multiZ) {
        this.areaSize = areaSize;
        this.topLeftX = multiX * areaSize;
        this.topLeftZ = multiZ * areaSize;
        this.grid = new Placement[areaSize][areaSize];
    }

    /** Returns the multichunk coordinate of a chunk along one axis. */
    static int multiCoordinate(int chunk, int areaSize) {
        return Math.floorDiv(chunk, areaSize);
    }

    /**
     * Returns the placement covering a chunk of this multichunk.
     *
     * @param chunkX a chunk X inside the multichunk
     * @param chunkZ a chunk Z inside the multichunk
     * @return the placement, or {@code null}
     */
    Placement placementAt(int chunkX, int chunkZ) {
        return grid[chunkX - topLeftX][chunkZ - topLeftZ];
    }

    /**
     * Places the multibuildings of one multichunk.
     *
     * @param planner the planner whose inputs decide
     * @param multiX  the multichunk's X
     * @param multiZ  the multichunk's Z
     * @return the placed grid
     */
    static MultiChunk calculate(LayoutPlanner planner, int multiX, int multiZ) {
        MultiSettings settings = planner.worldStyle.multiSettings();
        MultiChunk chunk = new MultiChunk(settings.areaSize(), multiX, multiZ);
        // Avoids CITY-25: bad settings place nothing (the loader has already rejected them with a message).
        if (settings.areaSize() < 1 || settings.maximum() < settings.minimum()) {
            return chunk;
        }
        Random random = new Random(planner.seed + planner.dimension.stableHash() + multiX * SEED_X + multiZ * SEED_Z);
        int count = settings.minimum() + random.nextInt(settings.maximum() - settings.minimum() + 1);
        if (count <= 0) {
            return chunk;
        }
        chunk.place(planner, settings, random, count);
        return chunk;
    }

    private void place(LayoutPlanner planner, MultiSettings settings, Random random, int count) {
        // Count the city style of every chunk in the area; a sorted map iterates by name, so no hash order leaks in.
        TreeMap<String, Integer> styleCounts = new TreeMap<>();
        for (int x = 0; x < areaSize; x++) {
            for (int z = 0; z < areaSize; z++) {
                String style = planner.cities.cityStyleName(topLeftX + x, topLeftZ + z);
                if (style != null) {
                    styleCounts.merge(style, 1, Integer::sum);
                }
            }
        }
        if (styleCounts.isEmpty()) {
            return;
        }
        List<String> styles = new ArrayList<>(styleCounts.keySet());
        List<Candidate> candidates = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String styleName = WeightedPicker.pick(random, styles, s -> (float) styleCounts.get(s));
            CityStyleView style = planner.cityStyle(styleName);
            String name = style == null ? null : style.pick(Selection.MULTIBUILDINGS, random, topLeftX, topLeftZ);
            MultiBuildingView multi = name == null ? null : planner.assets.multiBuilding(name);
            if (multi == null) {
                if (name != null) {
                    planner.warnOnce(
                        "multibuilding:" + name,
                        "unknown multibuilding '" + name + "' in city style " + styleName + "; none is placed for it");
                }
                continue;
            }
            candidates.add(new Candidate(multi, styleName));
        }
        // Largest first; a stable sort keeps equal sizes in pick order, as upstream.
        candidates.sort((a, b) -> Integer.compare(b.size(), a.size()));
        for (Candidate candidate : candidates) {
            tryPlace(planner, settings, random, candidate);
        }
    }

    private void tryPlace(LayoutPlanner planner, MultiSettings settings, Random random, Candidate candidate) {
        MultiBuildingView multi = candidate.multi;
        int maxCellars = maxCellars(planner, multi);
        if (maxCellars < 0) {
            return;
        }
        int dimX = multi.dimX();
        int dimZ = multi.dimZ();
        // Avoids CITY-25: a multibuilding larger than the area is never placed.
        if (dimX < 1 || dimZ < 1 || dimX > areaSize || dimZ > areaSize) {
            return;
        }
        for (int attempt = 0; attempt < settings.attempts(); attempt++) {
            int x = random.nextInt(areaSize - dimX + 1);
            int z = random.nextInt(areaSize - dimZ + 1);
            if (canPlace(planner, settings, candidate, maxCellars, x, z)) {
                for (int xx = 0; xx < dimX; xx++) {
                    for (int zz = 0; zz < dimZ; zz++) {
                        grid[x + xx][z + zz] = new Placement(multi.name(), xx, zz, dimX, dimZ);
                    }
                }
                return;
            }
        }
    }

    /** The largest {@code maxcellars} of the multibuilding's buildings (at least 0), or -1 for an unknown building. */
    private static int maxCellars(LayoutPlanner planner, MultiBuildingView multi) {
        int max = 0;
        for (int x = 0; x < multi.dimX(); x++) {
            for (int z = 0; z < multi.dimZ(); z++) {
                String name = multi.buildingAt(x, z);
                BuildingView building = name == null ? null : planner.assets.building(name);
                if (building == null) {
                    planner.warnOnce(
                        "multibuilding-building:" + multi.name(),
                        "multibuilding " + multi.name() + " names unknown building '" + name + "'; it is never placed");
                    return -1;
                }
                max = Math.max(max, building.maxCellars());
            }
        }
        return max;
    }

    private boolean canPlace(LayoutPlanner planner, MultiSettings settings, Candidate candidate, int maxCellars, int x,
        int z) {
        MultiBuildingView multi = candidate.multi;
        int partLevel = planner.worldStyle.railPartHeight6();
        int cityLevel = footprintLevel(planner, topLeftX + x, topLeftZ + z, multi.dimX(), multi.dimZ());
        int correctStyle = 0;
        for (int xx = 0; xx < multi.dimX(); xx++) {
            for (int zz = 0; zz < multi.dimZ(); zz++) {
                if (grid[x + xx][z + zz] != null) {
                    return false;
                }
                int cx = topLeftX + x + xx;
                int cz = topLeftZ + z + zz;
                if (planner.predefined.occupant(cx, cz) != null) {
                    return false;
                }
                if (planner.profile.multiBuildingStreetConflict.roadBlocks(planner.streets.getRoadType(cx, cz))) {
                    return false;
                }
                RailInfo rail = planner.infrastructure.rail(cx, cz);
                if (rail.isSurfaceOrStation() || !planner.cities.isCityRaw(cx, cz) || planner.hasHighway(cx, cz)) {
                    return false;
                }
                if (rail.isPresent() && !planner.worldStyle.buildingsBlockRailways()) {
                    // Avoids CITY-26: the level this multibuilding would have here, not the area's corner's.
                    int max = Math.min(cityLevel - rail.level() - partLevel, maxCellars);
                    if (max < maxCellars) {
                        return false;
                    }
                }
                // Deviation (0006 §3): never over a core, an avoid ring, an edge ring, a detour or a lane.
                Zone zone = planner.zones.zone(cx, cz)
                    .zone();
                if (zone == Zone.CORE || zone == Zone.AVOID_RING
                    || zone == Zone.EDGE_RING
                    || planner.sewing.cell(cx, cz)
                        .role()
                        .forcesRoad()) {
                    return false;
                }
                if (candidate.styleName.equals(planner.cities.cityStyleName(cx, cz))) {
                    correctStyle++;
                }
            }
        }
        return correctStyle >= multi.dimX() * multi.dimZ() * settings.correctStyleFactor();
    }

    /** The city level a multibuilding gets at a position: its top-left's, or the footprint average (truncated). */
    static int footprintLevel(LayoutPlanner planner, int topX, int topZ, int dimX, int dimZ) {
        if (planner.profile.multiUseCorner) {
            return planner.levels.cityLevel(topX, topZ);
        }
        int sum = 0;
        for (int x = 0; x < dimX; x++) {
            for (int z = 0; z < dimZ; z++) {
                sum += planner.levels.cityLevel(topX + x, topZ + z);
            }
        }
        return sum / (dimX * dimZ);
    }

    /** One multibuilding to place, with the style it was picked for. */
    private static final class Candidate {

        final MultiBuildingView multi;
        final String styleName;

        Candidate(MultiBuildingView multi, String styleName) {
            this.multi = multi;
            this.styleName = styleName;
        }

        int size() {
            return multi.dimX() + multi.dimZ();
        }
    }
}
