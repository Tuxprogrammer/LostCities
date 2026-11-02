package com.tuxprogrammer.lostcities.layout.predefined;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/City.java
// Avoids: CITY-07, CITY-08

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tuxprogrammer.lostcities.layout.view.MultiBuildingView;

/**
 * The predefined cities, buildings and streets of one dimension, indexed by chunk: upstream's predefined maps in
 * {@code City} ({@code getPredefinedCity}, {@code getPredefinedBuildingAtTopLeft}, {@code getPredefinedBuilding},
 * {@code isChunkOccupied}, {@code getPredefinedStreet}) as one immutable index the layout owns, instead of static maps.
 *
 * <ul>
 * <li>Avoids CITY-07: every chunk of a predefined multibuilding's footprint is occupied and, through the layout, forced
 * to city, whatever its size; upstream checks only three neighbors of the top-left, so only a 2 × 2 footprint was
 * covered.</li>
 * <li>Avoids CITY-08: entries that land on a chunk another entry already holds are rejected with both assets named,
 * instead of overwriting each other in map order. Cities are indexed in source-name order, so which one is kept never
 * depends on load order.</li>
 * </ul>
 *
 * <p>
 * Spec: docs/original-spec/04-city-layout/predefined-content.md. Side: both.
 */
public final class PredefinedIndex {

    /** An index with no predefined content: upstream's bundled state. */
    public static final PredefinedIndex EMPTY = new PredefinedIndex(
        Collections.emptyMap(),
        Collections.emptyMap(),
        Collections.emptyMap(),
        Collections.emptyList());

    private final Map<Long, PredefinedCity> cities;
    private final List<PredefinedCity> cityList;
    private final Map<Long, PredefinedBuilding> buildingsAtTopLeft;
    private final Map<Long, Occupant> occupants;
    private final List<String> problems;

    private PredefinedIndex(Map<Long, PredefinedCity> cities, Map<Long, PredefinedBuilding> buildingsAtTopLeft,
        Map<Long, Occupant> occupants, List<String> problems) {
        this.cities = cities;
        List<PredefinedCity> list = new ArrayList<>(cities.values());
        list.sort(Comparator.comparing(PredefinedCity::source));
        this.cityList = Collections.unmodifiableList(list);
        this.buildingsAtTopLeft = buildingsAtTopLeft;
        this.occupants = occupants;
        this.problems = problems;
    }

    /** What occupies a chunk: a predefined building (with the chunk's offset in it) or a predefined street. */
    public static final class Occupant {

        private final String source;
        private final PredefinedBuilding building;
        private final int offsetX;
        private final int offsetZ;

        Occupant(String source, PredefinedBuilding building, int offsetX, int offsetZ) {
            this.source = source;
            this.building = building;
            this.offsetX = offsetX;
            this.offsetZ = offsetZ;
        }

        /** Returns the asset the occupant comes from, for log lines. */
        public String source() {
            return source;
        }

        /** Returns the building, or {@code null} for a street. */
        public PredefinedBuilding building() {
            return building;
        }

        /** Returns whether this is a predefined street. */
        public boolean isStreet() {
            return building == null;
        }

        /** Returns the chunk's X offset inside its building (0 for single buildings and streets). */
        public int offsetX() {
            return offsetX;
        }

        /** Returns the chunk's Z offset inside its building. */
        public int offsetZ() {
            return offsetZ;
        }
    }

    /**
     * Indexes the predefined cities of one dimension.
     *
     * @param dimensionId    the dimension ID to keep cities of, for example {@code minecraft:overworld}
     * @param all            every loaded predefined city, of any dimension
     * @param multiBuildings looks up a multibuilding by name, for footprints; {@code null} for an unknown name
     * @return the index; its {@link #problems()} name each rejected entry
     */
    public static PredefinedIndex build(String dimensionId, List<PredefinedCity> all,
        MultiBuildingLookup multiBuildings) {
        List<PredefinedCity> sorted = new ArrayList<>();
        for (PredefinedCity city : all) {
            if (city.dimension()
                .equals(dimensionId)) {
                sorted.add(city);
            }
        }
        sorted.sort(Comparator.comparing(PredefinedCity::source));
        Map<Long, PredefinedCity> cities = new HashMap<>();
        Map<Long, PredefinedBuilding> tops = new HashMap<>();
        Map<Long, Occupant> occupants = new HashMap<>();
        List<String> problems = new ArrayList<>();
        for (PredefinedCity city : sorted) {
            long center = key(city.chunkX(), city.chunkZ());
            PredefinedCity other = cities.get(center);
            if (other != null) {
                problems.add(
                    "predefined city " + city.source()
                        + " has the same center chunk as "
                        + other.source()
                        + "; it is ignored");
                continue;
            }
            cities.put(center, city);
            for (PredefinedBuilding building : city.buildings()) {
                addBuilding(city, building, multiBuildings, tops, occupants, problems);
            }
            for (int[] street : city.streets()) {
                int x = city.chunkX() + street[0];
                int z = city.chunkZ() + street[1];
                Occupant held = occupants.get(key(x, z));
                if (held != null) {
                    problems.add(
                        "predefined street of " + city
                            .source() + " at chunk " + x + "," + z + " overlaps " + held.source() + "; it is ignored");
                    continue;
                }
                occupants.put(key(x, z), new Occupant(city.source(), null, 0, 0));
            }
        }
        return new PredefinedIndex(cities, tops, occupants, Collections.unmodifiableList(problems));
    }

    private static void addBuilding(PredefinedCity city, PredefinedBuilding building,
        MultiBuildingLookup multiBuildings, Map<Long, PredefinedBuilding> tops, Map<Long, Occupant> occupants,
        List<String> problems) {
        int topX = city.chunkX() + building.relChunkX();
        int topZ = city.chunkZ() + building.relChunkZ();
        int dimX = 1;
        int dimZ = 1;
        if (building.multi()) {
            MultiBuildingView multi = multiBuildings.find(building.building());
            if (multi == null) {
                problems.add(
                    "predefined multibuilding " + building.building()
                        + " of "
                        + city.source()
                        + " is not a known multibuilding; it is ignored");
                return;
            }
            dimX = multi.dimX();
            dimZ = multi.dimZ();
        }
        for (int x = 0; x < dimX; x++) {
            for (int z = 0; z < dimZ; z++) {
                Occupant held = occupants.get(key(topX + x, topZ + z));
                if (held != null) {
                    problems.add(
                        "predefined building " + building.building()
                            + " of "
                            + city.source()
                            + " at chunk "
                            + (topX + x)
                            + ","
                            + (topZ + z)
                            + " overlaps "
                            + held.source()
                            + "; it is ignored");
                    return;
                }
            }
        }
        tops.put(key(topX, topZ), building);
        for (int x = 0; x < dimX; x++) {
            for (int z = 0; z < dimZ; z++) {
                occupants.put(key(topX + x, topZ + z), new Occupant(city.source(), building, x, z));
            }
        }
    }

    /** Looks up multibuildings by name while indexing. */
    public interface MultiBuildingLookup {

        /**
         * Finds a multibuilding.
         *
         * @param name its name
         * @return the multibuilding, or {@code null}
         */
        MultiBuildingView find(String name);
    }

    /** Returns the entries that were rejected while indexing, one message each naming the assets involved. */
    public List<String> problems() {
        return problems;
    }

    /**
     * Returns the predefined city centered on a chunk.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the city, or {@code null}
     */
    public PredefinedCity cityAt(int chunkX, int chunkZ) {
        return cities.get(key(chunkX, chunkZ));
    }

    /**
     * Returns the predefined building whose top-left chunk is this chunk.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the building, or {@code null}
     */
    public PredefinedBuilding buildingAtTopLeft(int chunkX, int chunkZ) {
        return buildingsAtTopLeft.get(key(chunkX, chunkZ));
    }

    /**
     * Returns what predefined content occupies a chunk.
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the occupant, or {@code null}
     */
    public Occupant occupant(int chunkX, int chunkZ) {
        return occupants.get(key(chunkX, chunkZ));
    }

    /** Returns every indexed predefined city, sorted by source name. */
    public List<PredefinedCity> cities() {
        return cityList;
    }

    /** Returns whether the index holds anything at all. */
    public boolean isEmpty() {
        return cities.isEmpty() && occupants.isEmpty();
    }

    private static long key(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
    }
}
