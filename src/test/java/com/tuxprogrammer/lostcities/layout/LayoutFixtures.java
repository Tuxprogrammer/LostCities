package com.tuxprogrammer.lostcities.layout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.tuxprogrammer.lostcities.layout.landmark.BlockRect;
import com.tuxprogrammer.lostcities.layout.landmark.Landmark;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkKind;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.layout.predefined.PredefinedBuilding;
import com.tuxprogrammer.lostcities.layout.predefined.PredefinedCity;
import com.tuxprogrammer.lostcities.layout.predefined.PredefinedIndex;
import com.tuxprogrammer.lostcities.layout.random.WeightedPicker;
import com.tuxprogrammer.lostcities.layout.view.AssetView;
import com.tuxprogrammer.lostcities.layout.view.BuildingView;
import com.tuxprogrammer.lostcities.layout.view.CityStyleView;
import com.tuxprogrammer.lostcities.layout.view.MultiBuildingView;
import com.tuxprogrammer.lostcities.layout.view.MultiSettings;
import com.tuxprogrammer.lostcities.layout.view.PartContext;
import com.tuxprogrammer.lostcities.layout.view.Selection;
import com.tuxprogrammer.lostcities.layout.view.StyleView;
import com.tuxprogrammer.lostcities.layout.view.TerrainView;
import com.tuxprogrammer.lostcities.layout.view.WorldStyleView;

/**
 * Fakes of the layout's views for tests: a small asset set that keeps upstream's random-consumption contracts, a
 * terrain from a height function, and a structure oracle over a fixed list of landmarks. Nothing here reads files or
 * Minecraft.
 */
public final class LayoutFixtures {

    private LayoutFixtures() {}

    /** A weighted entry of a fake city style list. */
    public static final class Weighted {

        final String name;
        final float weight;

        public Weighted(String name, float weight) {
            this.name = name;
            this.weight = weight;
        }
    }

    /** A fake city style; every setting unset unless given. */
    public static final class FakeCityStyle implements CityStyleView {

        final String name;
        final String styleName;
        final Map<Selection, List<Weighted>> lists = new EnumMap<>(Selection.class);
        Float buildingChance;

        public FakeCityStyle(String name, String styleName) {
            this.name = name;
            this.styleName = styleName;
            for (Selection s : Selection.values()) {
                lists.put(s, new ArrayList<>());
            }
        }

        public FakeCityStyle with(Selection list, String entry, float weight) {
            lists.get(list)
                .add(new Weighted(entry, weight));
            return this;
        }

        public FakeCityStyle buildingChance(float chance) {
            buildingChance = chance;
            return this;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String styleName() {
            return styleName;
        }

        @Override
        public Float buildingChance() {
            return buildingChance;
        }

        @Override
        public Integer minFloors() {
            return null;
        }

        @Override
        public Integer maxFloors() {
            return null;
        }

        @Override
        public Integer minCellars() {
            return null;
        }

        @Override
        public Integer maxCellars() {
            return null;
        }

        @Override
        public Float parkChance() {
            return null;
        }

        @Override
        public Float openLotParkChance() {
            return null;
        }

        @Override
        public Float fountainChance() {
            return null;
        }

        @Override
        public Float frontChance() {
            return null;
        }

        @Override
        public Float corridorChance() {
            return null;
        }

        @Override
        public Boolean avoidFoliage() {
            return null;
        }

        @Override
        public Boolean parkBorder() {
            return null;
        }

        @Override
        public Boolean parkElevation() {
            return null;
        }

        @Override
        public Integer parkStreetThreshold() {
            return null;
        }

        @Override
        public String pick(Selection list, Random random, int chunkX, int chunkZ) {
            Weighted w = WeightedPicker.pick(random, lists.get(list), e -> e.weight);
            return w == null ? null : w.name;
        }
    }

    /**
     * A fake building: cellars pick {@code <name>_cellar}, the roof {@code <name>_roof}, other floors one of
     * {@code <name>_floor_a}/{@code _b}; no overlay parts.
     */
    public static final class FakeBuilding implements BuildingView {

        final String name;
        int minFloors = UNSET;
        int maxFloors = UNSET;
        int minCellars = UNSET;
        int maxCellars = UNSET;
        boolean overrideFloors;
        float prefersLonely;
        boolean noParts;
        boolean closed;

        public FakeBuilding(String name) {
            this.name = name;
        }

        public FakeBuilding floors(int min, int max) {
            minFloors = min;
            maxFloors = max;
            return this;
        }

        public FakeBuilding cellars(int min, int max) {
            minCellars = min;
            maxCellars = max;
            return this;
        }

        public FakeBuilding prefersLonely(float value) {
            prefersLonely = value;
            return this;
        }

        public FakeBuilding noParts() {
            noParts = true;
            return this;
        }

        /** Every part refuses connections ({@code dontconnect}). */
        public FakeBuilding closed() {
            closed = true;
            return this;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public int minFloors() {
            return minFloors;
        }

        @Override
        public int maxFloors() {
            return maxFloors;
        }

        @Override
        public int minCellars() {
            return minCellars;
        }

        @Override
        public int maxCellars() {
            return maxCellars;
        }

        @Override
        public boolean overrideFloors() {
            return overrideFloors;
        }

        @Override
        public float prefersLonely() {
            return prefersLonely;
        }

        @Override
        public String pickPart(Random random, PartContext context) {
            if (noParts) {
                return null;
            }
            List<String> matching;
            if (context.isCellar()) {
                matching = Collections.singletonList(name + "_cellar");
            } else if (context.isTopOfBuilding()) {
                matching = Collections.singletonList(name + "_roof");
            } else {
                matching = Arrays.asList(name + "_floor_a", name + "_floor_b");
            }
            String part = matching.get(random.nextInt(matching.size()));
            return closed ? part + "_closed" : part;
        }

        @Override
        public String pickPart2(Random random, PartContext context) {
            return null;
        }
    }

    /** A fake multibuilding of one building repeated. */
    public static final class FakeMulti implements MultiBuildingView {

        final String name;
        final int dimX;
        final int dimZ;
        final String building;

        public FakeMulti(String name, int dimX, int dimZ, String building) {
            this.name = name;
            this.dimX = dimX;
            this.dimZ = dimZ;
            this.building = building;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public int dimX() {
            return dimX;
        }

        @Override
        public int dimZ() {
            return dimZ;
        }

        @Override
        public String buildingAt(int x, int z) {
            return building;
        }
    }

    /** A fake world style: a weighted city-style list for every biome, multiplier 1. */
    public static final class FakeWorldStyle implements WorldStyleView {

        final List<Weighted> cityStyles = new ArrayList<>();
        MultiSettings multiSettings = MultiSettings.DEFAULTS;
        int railPartHeight6 = 1;
        boolean blockRailways;
        float multiplier = 1f;

        public FakeWorldStyle cityStyle(String name, float weight) {
            cityStyles.add(new Weighted(name, weight));
            return this;
        }

        @Override
        public String pickCityStyle(Random random, int chunkX, int chunkZ) {
            Weighted w = WeightedPicker.pick(random, cityStyles, e -> e.weight);
            return w == null ? null : w.name;
        }

        @Override
        public float cityChanceMultiplier(int chunkX, int chunkZ) {
            return multiplier;
        }

        @Override
        public MultiSettings multiSettings() {
            return multiSettings;
        }

        @Override
        public String outsideStyleName() {
            return "style_outside";
        }

        @Override
        public int railPartHeight6() {
            return railPartHeight6;
        }

        @Override
        public boolean buildingsBlockRailways() {
            return blockRailways;
        }
    }

    /** Fake assets, built up by the test. */
    public static final class FakeAssets implements AssetView {

        final FakeWorldStyle worldStyle = new FakeWorldStyle();
        final Map<String, FakeCityStyle> cityStyles = new HashMap<>();
        final Map<String, FakeBuilding> buildings = new HashMap<>();
        final Map<String, FakeMulti> multis = new HashMap<>();

        public FakeAssets add(FakeCityStyle style) {
            cityStyles.put(style.name, style);
            return this;
        }

        public FakeAssets add(FakeBuilding building) {
            buildings.put(building.name, building);
            return this;
        }

        public FakeAssets add(FakeMulti multi) {
            multis.put(multi.name, multi);
            return this;
        }

        @Override
        public FakeWorldStyle worldStyle() {
            return worldStyle;
        }

        @Override
        public CityStyleView cityStyle(String name) {
            return cityStyles.get(name);
        }

        @Override
        public BuildingView building(String name) {
            return buildings.get(name);
        }

        @Override
        public MultiBuildingView multiBuilding(String name) {
            return multis.get(name);
        }

        @Override
        public StyleView style(String name) {
            // Two random-palette groups: one draw each.
            return random -> Arrays.asList(
                random.nextFloat() < .5f ? name + "_a" : name + "_b",
                random.nextFloat() < .5f ? name + "_c" : name + "_d");
        }

        @Override
        public boolean partDontConnect(String partName) {
            return partName.endsWith("_closed");
        }
    }

    /** The standard fake assets: two city styles, four buildings, a 2 × 2 multibuilding. */
    public static FakeAssets standardAssets() {
        FakeAssets assets = new FakeAssets();
        assets.add(
            new FakeCityStyle("citystyle_common", "style_common").with(Selection.BUILDINGS, "building1", 1f)
                .with(Selection.BUILDINGS, "building2", 1f)
                .with(Selection.BUILDINGS, "tower", .5f)
                .with(Selection.MULTIBUILDINGS, "multi2x2", 1f)
                .with(Selection.PARKS, "park1", 1f)
                .with(Selection.FOUNTAINS, "fountain1", 1f)
                .with(Selection.BRIDGES, "bridge1", 1f)
                .with(Selection.LARGE_BRIDGES, "bridge_large", 1f)
                .with(Selection.STAIRS, "stair1", 1f)
                .with(Selection.FRONTS, "front1", 1f)
                .with(Selection.RAIL_DUNGEONS, "dungeon1", 1f));
        assets.add(
            new FakeCityStyle("citystyle_desert", "style_desert").with(Selection.BUILDINGS, "building2", 1f)
                .with(Selection.BUILDINGS, "tower", 1f)
                .with(Selection.MULTIBUILDINGS, "multi2x2", 1f)
                .with(Selection.PARKS, "park_desert", 1f)
                .with(Selection.BRIDGES, "bridge1", 1f)
                .with(Selection.STAIRS, "stair1", 1f)
                .with(Selection.FRONTS, "front1", 1f));
        assets.add(new FakeBuilding("building1"));
        assets.add(new FakeBuilding("building2").cellars(0, 1));
        assets.add(new FakeBuilding("tower").floors(4, 12));
        assets.add(new FakeMulti("multi2x2", 2, 2, "building1"));
        assets.worldStyle.cityStyle("citystyle_common", 3f)
            .cityStyle("citystyle_desert", 1f);
        return assets;
    }

    /** Height as a function of chunk coordinates. */
    public interface Height {

        int at(int chunkX, int chunkZ);
    }

    /** A fake terrain from a height function; water below Y 63. */
    public static final class FakeTerrain implements TerrainView {

        final Height height;
        int[] waterRect;

        public FakeTerrain(Height height) {
            this.height = height;
        }

        /** Marks a chunk rectangle as a water biome (inclusive). */
        public FakeTerrain water(int minX, int minZ, int maxX, int maxZ) {
            waterRect = new int[] { minX, minZ, maxX, maxZ };
            return this;
        }

        @Override
        public int height(int chunkX, int chunkZ) {
            return height.at(chunkX, chunkZ);
        }

        @Override
        public boolean isWaterBiome(int chunkX, int chunkZ) {
            return waterRect != null && chunkX >= waterRect[0]
                && chunkZ >= waterRect[1]
                && chunkX <= waterRect[2]
                && chunkZ <= waterRect[3];
        }

        @Override
        public boolean isOcean(int chunkX, int chunkZ) {
            return isWaterBiome(chunkX, chunkZ);
        }
    }

    /** Gentle rolling terrain between Y 68 and 92, so city levels 0 to 2 appear. */
    public static FakeTerrain rollingTerrain() {
        return new FakeTerrain((x, z) -> 80 + (int) (12 * Math.sin(x * 0.11) * Math.cos(z * 0.07)));
    }

    /** Flat terrain at Y 70 (city level 0). */
    public static FakeTerrain flatTerrain() {
        return new FakeTerrain((x, z) -> 70);
    }

    /** A structure oracle over a fixed list, answering every rectangle completely. */
    public static final class FakeOracle implements StructureOracle {

        final List<Landmark> landmarks;
        int calls;

        public FakeOracle(Landmark... landmarks) {
            this.landmarks = Arrays.asList(landmarks);
        }

        @Override
        public List<Landmark> landmarksTouching(int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
            calls++;
            BlockRect query = new BlockRect(minChunkX * 16, minChunkZ * 16, maxChunkX * 16 + 15, maxChunkZ * 16 + 15);
            List<Landmark> out = new ArrayList<>();
            for (Landmark landmark : landmarks) {
                for (BlockRect rect : landmark.footprint()) {
                    if (rect.maxX() >= query.minX() && rect.minX() <= query.maxX()
                        && rect.maxZ() >= query.minZ()
                        && rect.minZ() <= query.maxZ()) {
                        out.add(landmark);
                        break;
                    }
                }
            }
            // Reverse, so a planner that depended on the oracle's order would notice.
            Collections.reverse(out);
            return out;
        }
    }

    /** A planner over the standard assets. */
    public static LayoutPlanner planner(long seed, LayoutProfile profile, TerrainView terrain, StructureOracle oracle) {
        return new LayoutPlanner(
            LayoutInputs.builder(seed, profile, standardAssets(), terrain)
                .structures(oracle)
                .build());
    }

    /**
     * One big predefined city centered on a chunk, with no random cities ({@code cityChance} 0 in the profile): city
     * out to 80% of the radius.
     */
    public static PredefinedIndex bigCity(int centerX, int centerZ, int radiusBlocks, String style) {
        return PredefinedIndex.build(
            "minecraft:overworld",
            Collections.singletonList(
                new PredefinedCity(
                    "fixture_city",
                    "minecraft:overworld",
                    centerX,
                    centerZ,
                    radiusBlocks,
                    style,
                    Collections.<PredefinedBuilding>emptyList(),
                    Collections.<int[]>emptyList())),
            name -> null);
    }

    /** A profile with no random cities, for {@link #bigCity}. */
    public static LayoutProfile predefinedOnly() {
        return LayoutProfile.builder()
            .cityChance(0)
            .build();
    }

    /** A planner over one big predefined city, the standard assets, flat terrain and the given landmarks. */
    public static LayoutPlanner cityWith(long seed, int radiusBlocks, Landmark... landmarks) {
        return new LayoutPlanner(
            LayoutInputs.builder(seed, predefinedOnly(), standardAssets(), flatTerrain())
                .predefined(bigCity(0, 0, radiusBlocks, "citystyle_common"))
                .structures(new FakeOracle(landmarks))
                .build());
    }

    /** A landmark whose footprint is one block rectangle. */
    public static Landmark landmark(String id, LandmarkKind kind, int minX, int minZ, int maxX, int maxZ) {
        return Landmark.builder(id, kind)
            .footprint(new BlockRect(minX, minZ, maxX, maxZ))
            .groundEstimate(70)
            .build();
    }

    /** A profile where every chunk near the origin is city: cities everywhere (threshold below every factor). */
    public static LayoutProfile denseCity() {
        return LayoutProfile.builder()
            .cityChance(.02)
            .cityRadius(150, 200)
            .build();
    }
}
