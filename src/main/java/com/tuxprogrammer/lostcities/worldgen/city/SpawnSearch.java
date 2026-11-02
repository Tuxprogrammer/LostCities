package com.tuxprogrammer.lostcities.worldgen.city;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/setup/ForgeEventHandlers.java
// Avoids: CORE-13, CORE-14, CORE-15, CORE-16

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.config.profile.ProfileValues;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;

/**
 * Chooses a new world's spawn from the profile's spawn options, upstream's {@code findSafeSpawnPoint}, asked of the
 * pure layout instead of the world (docs/original-spec/01-core/spawn-selection.md; docs/new-spec/01-core/index.md,
 * "Spawn selection"; docs/new-spec/00-pack-integration/spawn.md, D21).
 *
 * <ul>
 * <li>The position rule is the first set of {@code spawnBiome} (the 1.7.10 biome's name, any case) and
 * {@code spawnCity} (a predefined city: within 0.8 of its radius of its center). {@code spawnSphere} has no effect
 * until spheres return (D12).</li>
 * <li>The chunk rule is the first that applies of {@code spawnNotInBuilding} (no building chunk), a non-empty
 * {@code forceSpawnBuildings} or {@code forceSpawnParts} (a building chunk of a listed building whose lowest floor is a
 * listed part; upstream applies the lists even with {@code forceSpawnInBuilding} off, and so does this port, as the
 * option comment says), and {@code forceSpawnInBuilding} (any building chunk).</li>
 * <li>With no rule set, there is no search and the pack's spawn is kept (D21).</li>
 * <li>The search walks upstream's square spiral of chunks from chunk (0, 0), widening by {@code spawnRadiusIncrease}
 * from {@code spawnCheckRadius}, and gives up after {@code spawnCheckAttempts} chunks. Per chunk it tries column
 * (8, 8), then the others in upstream's order.</li>
 * </ul>
 * Avoids CORE-15: no chunk is generated; each chunk is answered by the layout and the height oracle. Avoids CORE-16:
 * the standing height comes from the plan (a city chunk's street or ground floor) or the oracle (natural ground at or
 * above the water level), not from a bottom-up scan that finds caves and cellars. Avoids CORE-14: a failed search
 * returns {@code null}, and the caller keeps vanilla's spawn instead of aborting world creation. Avoids CORE-13: the
 * forced lists are ordered sets, duplicates already dropped by the profile validator.
 *
 * <p>
 * Pure. Side: both (plain Java); the server asks it once, when a world is created.
 */
public final class SpawnSearch {

    /** Fraction of a predefined city's radius a spawn must fall within. */
    public static final double CITY_RADIUS_FRACTION = 0.8;

    /** The column tried first in a chunk. */
    public static final int CENTER = 8;

    /** The world the search reads, column by column: biomes and natural ground. */
    public interface Terrain {

        /**
         * Returns the biome name of a column.
         *
         * @param x block X
         * @param z block Z
         * @return the biome's name, or {@code null}
         */
        String biomeName(int x, int z);

        /**
         * Returns whether a biome of a name exists.
         *
         * @param name the name, any case
         * @return whether a registered biome has it
         */
        boolean biomeExists(String name);

        /**
         * Returns the natural ground height of a column: the Y of the first non-solid block above the terrain.
         *
         * @param x block X
         * @param z block Z
         * @return block Y
         */
        int height(int x, int z);
    }

    /** A predefined city a spawn may be asked for. */
    public static final class City {

        private final String name;
        private final int centerX;
        private final int centerZ;
        private final int radius;

        /**
         * Creates one.
         *
         * @param name   its asset ID
         * @param chunkX its center chunk X
         * @param chunkZ its center chunk Z
         * @param radius its radius, blocks
         */
        public City(String name, int chunkX, int chunkZ, int radius) {
            this.name = name;
            this.centerX = (chunkX << 4) + CENTER;
            this.centerZ = (chunkZ << 4) + CENTER;
            this.radius = radius;
        }
    }

    /** A chosen spawn: the block a player's feet stand in, and the chunk's kind. */
    public static final class Spot {

        private final int x;
        private final int y;
        private final int z;
        private final boolean inBuilding;
        private final int checked;

        Spot(int x, int y, int z, boolean inBuilding, int checked) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.inBuilding = inBuilding;
            this.checked = checked;
        }

        /** Returns block X. */
        public int x() {
            return x;
        }

        /** Returns the feet's block Y. */
        public int y() {
            return y;
        }

        /** Returns block Z. */
        public int z() {
            return z;
        }

        /** Returns whether the spot is on a building's ground floor. */
        public boolean inBuilding() {
            return inBuilding;
        }

        /** Returns how many chunks the search looked at. */
        public int checked() {
            return checked;
        }

        @Override
        public String toString() {
            return x + " " + y + " " + z + (inBuilding ? " (building)" : "") + " after " + checked + " chunk(s)";
        }
    }

    /** The spawn options, read once from the pinned profile. */
    public static final class Options {

        final String biome;
        final String city;
        final boolean notInBuilding;
        final boolean forceInBuilding;
        final Set<String> buildings;
        final Set<String> parts;
        final int checkRadius;
        final int radiusIncrease;
        final int attempts;

        private Options(String biome, String city, boolean notInBuilding, boolean forceInBuilding,
            Set<String> buildings, Set<String> parts, int checkRadius, int radiusIncrease, int attempts) {
            this.biome = biome;
            this.city = city;
            this.notInBuilding = notInBuilding;
            this.forceInBuilding = forceInBuilding;
            this.buildings = buildings;
            this.parts = parts;
            this.checkRadius = checkRadius;
            this.radiusIncrease = radiusIncrease;
            this.attempts = attempts;
        }

        /**
         * Reads the options of a profile.
         *
         * @param values the pinned values
         * @return the options
         */
        public static Options of(ProfileValues values) {
            return new Options(
                values.getString("spawnBiome")
                    .trim(),
                values.getString("spawnCity")
                    .trim(),
                values.getBoolean("spawnNotInBuilding"),
                values.getBoolean("forceSpawnInBuilding"),
                ids(values.getStringList("forceSpawnBuildings")),
                ids(values.getStringList("forceSpawnParts")),
                values.getInt("spawnCheckRadius"),
                values.getInt("spawnRadiusIncrease"),
                values.getInt("spawnCheckAttempts"));
        }

        /** Returns these options without a position rule: an option that named nothing. */
        Options withoutPosition() {
            return new Options(
                "",
                "",
                notInBuilding,
                forceInBuilding,
                buildings,
                parts,
                checkRadius,
                radiusIncrease,
                attempts);
        }

        /** Returns whether a position rule is set. */
        boolean positionRule() {
            return !biome.isEmpty() || !city.isEmpty();
        }

        /** Returns whether the forced-building chunk rule is set. */
        boolean forcedBuilding() {
            return !notInBuilding && (!buildings.isEmpty() || !parts.isEmpty() || forceInBuilding);
        }

        /** Returns whether any option asks for a search; without one the pack's spawn is kept. */
        public boolean needsSearch() {
            return positionRule() || notInBuilding || forcedBuilding();
        }

        /**
         * Returns whether a player is placed exactly on first join: only for the building options, so Forge's spawn
         * fuzz does not put them on a roof (D21).
         */
        public boolean placesExactly() {
            return forcedBuilding();
        }
    }

    private final LayoutPlanner planner;
    private final Terrain terrain;
    private final int waterLevel;
    private final List<City> cities;

    /**
     * Creates a search over one world.
     *
     * @param planner    the world's layout
     * @param terrain    the world's biomes and natural ground
     * @param waterLevel the profile's water level; natural ground below it is water
     * @param cities     the loaded predefined cities
     */
    public SpawnSearch(LayoutPlanner planner, Terrain terrain, int waterLevel, List<City> cities) {
        this.planner = planner;
        this.terrain = terrain;
        this.waterLevel = waterLevel;
        this.cities = Collections.unmodifiableList(new ArrayList<>(cities));
    }

    /**
     * Searches for a spawn.
     *
     * @param options  the spawn options
     * @param problems where a missing biome or city is reported
     * @return the spot, or {@code null} when no option asks for one, an option names nothing, or the search fails
     */
    public Spot find(Options options, List<String> problems) {
        if (!options.needsSearch()) {
            return null;
        }
        // As upstream, an option that names nothing logs and sets no rule; the search runs only if another one does.
        boolean positionRule = false;
        City city = null;
        if (!options.biome.isEmpty()) {
            positionRule = terrain.biomeExists(options.biome);
            if (!positionRule) {
                problems.add("Cannot find biome '" + options.biome + "' for the player to spawn in");
            }
        } else if (!options.city.isEmpty()) {
            city = city(options.city);
            positionRule = city != null;
            if (city == null) {
                problems.add("Cannot find city '" + options.city + "' for the player to spawn in");
            }
        }
        if (!positionRule && !options.notInBuilding && !options.forcedBuilding()) {
            return null;
        }
        Options effective = positionRule ? options : options.withoutPosition();
        int radius = effective.checkRadius;
        int ring = 0;
        int checked = 0;
        while (true) {
            int lastRing = Math.max(0, (radius + 15) >> 4);
            for (; ring <= lastRing; ring++) {
                for (int[] chunk : ring(ring)) {
                    checked++;
                    Spot spot = inChunk(chunk[0], chunk[1], effective, city, checked);
                    if (spot != null) {
                        return spot;
                    }
                    if (checked >= effective.attempts) {
                        problems.add("Can't find a valid spawn position after " + checked + " chunks");
                        return null;
                    }
                }
            }
            radius += effective.radiusIncrease;
        }
    }

    /** Returns a ring's chunks in upstream's order: top edge, right edge, bottom edge, left edge. */
    static List<int[]> ring(int r) {
        List<int[]> out = new ArrayList<>();
        if (r == 0) {
            out.add(new int[] { 0, 0 });
            return out;
        }
        for (int x = -r; x <= r; x++) {
            out.add(new int[] { x, -r });
        }
        for (int z = -r + 1; z <= r; z++) {
            out.add(new int[] { r, z });
        }
        for (int x = r - 1; x >= -r; x--) {
            out.add(new int[] { x, r });
        }
        for (int z = r - 1; z >= -r + 1; z--) {
            out.add(new int[] { -r, z });
        }
        return out;
    }

    private Spot inChunk(int chunkX, int chunkZ, Options options, City city, int checked) {
        ChunkPlan plan = planner.plan(chunkX, chunkZ);
        if (!chunkAllowed(plan, options)) {
            return null;
        }
        Spot spot = atColumn(plan, CENTER, CENTER, options, city, checked);
        if (spot != null) {
            return spot;
        }
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (x == CENTER && z == CENTER) {
                    continue;
                }
                spot = atColumn(plan, x, z, options, city, checked);
                if (spot != null) {
                    return spot;
                }
            }
        }
        return null;
    }

    /** The chunk rule. */
    boolean chunkAllowed(ChunkPlan plan, Options options) {
        boolean building = plan.isCity() && plan.hasBuilding();
        if (options.notInBuilding) {
            return !building;
        }
        if (!options.forcedBuilding()) {
            return true;
        }
        if (!building) {
            return false;
        }
        if (!options.buildings.isEmpty() && !options.buildings.contains(id(plan.building()))) {
            return false;
        }
        if (!options.parts.isEmpty()) {
            String lowest = plan.floorPart(-plan.cellars());
            return lowest != null && options.parts.contains(id(lowest));
        }
        return true;
    }

    private Spot atColumn(ChunkPlan plan, int localX, int localZ, Options options, City city, int checked) {
        int x = (plan.chunkX() << 4) + localX;
        int z = (plan.chunkZ() << 4) + localZ;
        if (!positionAllowed(x, z, options, city)) {
            return null;
        }
        if (plan.isCity()) {
            // A street or a building's ground floor: the floor block is at the city ground, the feet one above.
            return new Spot(x, plan.cityGroundLevel() + 1, z, plan.hasBuilding(), checked);
        }
        int ground = terrain.height(x, z);
        if (ground < waterLevel) {
            return null;
        }
        return new Spot(x, ground, z, false, checked);
    }

    private boolean positionAllowed(int x, int z, Options options, City city) {
        if (!options.biome.isEmpty()) {
            String name = terrain.biomeName(x, z);
            return name != null && name.equalsIgnoreCase(options.biome);
        }
        if (city != null) {
            long dx = x - city.centerX;
            long dz = z - city.centerZ;
            double reach = city.radius * CITY_RADIUS_FRACTION;
            return dx * dx + dz * dz < (long) (reach * reach);
        }
        return true;
    }

    private City city(String name) {
        String wanted = id(name);
        for (City city : cities) {
            if (id(city.name).equals(wanted)) {
                return city;
            }
        }
        return null;
    }

    /** Returns an asset name in one canonical form, {@code namespace:path}, lower case; the text itself if bad. */
    static String id(String name) {
        if (name == null) {
            return "";
        }
        try {
            return AssetId.parse(name.trim())
                .toString()
                .toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException e) {
            return name.trim()
                .toLowerCase(Locale.ROOT);
        }
    }

    private static Set<String> ids(List<String> names) {
        Set<String> out = new LinkedHashSet<>();
        for (String name : names) {
            out.add(id(name));
        }
        return out;
    }
}
