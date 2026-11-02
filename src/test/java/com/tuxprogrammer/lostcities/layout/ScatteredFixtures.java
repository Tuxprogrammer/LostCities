package com.tuxprogrammer.lostcities.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.BiPredicate;

import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.layout.view.AssetView;
import com.tuxprogrammer.lostcities.layout.view.BuildingView;
import com.tuxprogrammer.lostcities.layout.view.CityStyleView;
import com.tuxprogrammer.lostcities.layout.view.InfrastructureView;
import com.tuxprogrammer.lostcities.layout.view.MultiBuildingView;
import com.tuxprogrammer.lostcities.layout.view.MultiSettings;
import com.tuxprogrammer.lostcities.layout.view.RailInfo;
import com.tuxprogrammer.lostcities.layout.view.ScatteredView;
import com.tuxprogrammer.lostcities.layout.view.StyleView;
import com.tuxprogrammer.lostcities.layout.view.TerrainView;
import com.tuxprogrammer.lostcities.layout.view.WorldStyleView;

/** Fakes for the scattered planner's tests: settings, entries, assets, terrain with column heights, highways. */
public final class ScatteredFixtures {

    private ScatteredFixtures() {}

    /** A fake scattered asset. */
    public static final class FakeAsset implements ScatteredView.Asset {

        final String name;
        List<String> buildings = Collections.emptyList();
        String multi;
        boolean rotatable;
        boolean clearRailing;
        String support;
        ScatteredView.TerrainHeight height = ScatteredView.TerrainHeight.HIGHEST;
        ScatteredView.TerrainFix fix = ScatteredView.TerrainFix.NONE;
        int offset;
        int maxHeight = 8;

        public FakeAsset(String name) {
            this.name = name;
        }

        public FakeAsset building(String... names) {
            List<String> list = new ArrayList<>();
            Collections.addAll(list, names);
            buildings = list;
            return this;
        }

        public FakeAsset multi(String value) {
            multi = value;
            return this;
        }

        public FakeAsset support(String value) {
            support = value;
            return this;
        }

        public FakeAsset anyRotation() {
            rotatable = true;
            return this;
        }

        public FakeAsset opensRailing() {
            clearRailing = true;
            return this;
        }

        public FakeAsset height(ScatteredView.TerrainHeight value) {
            height = value;
            return this;
        }

        public FakeAsset fix(ScatteredView.TerrainFix value) {
            fix = value;
            return this;
        }

        public FakeAsset offset(int value) {
            offset = value;
            return this;
        }

        public FakeAsset maxHeight(int value) {
            maxHeight = value;
            return this;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public List<String> buildings() {
            return buildings;
        }

        @Override
        public String multiBuilding() {
            return multi;
        }

        @Override
        public boolean rotatable() {
            return rotatable;
        }

        @Override
        public boolean clearHighwayRailing() {
            return clearRailing;
        }

        @Override
        public String supportPart() {
            return support;
        }

        @Override
        public ScatteredView.TerrainHeight terrainHeight() {
            return height;
        }

        @Override
        public ScatteredView.TerrainFix terrainFix() {
            return fix;
        }

        @Override
        public int heightOffset() {
            return offset;
        }

        @Override
        public int maxHeight() {
            return maxHeight;
        }
    }

    /** A fake list entry. */
    public static final class FakeEntry implements ScatteredView.Entry {

        final FakeAsset asset;
        final int weight;
        boolean nearHighway;
        Integer maxDiff;
        BiPredicate<Integer, Integer> biome = (x, z) -> true;

        public FakeEntry(FakeAsset asset, int weight) {
            this.asset = asset;
            this.weight = weight;
        }

        public FakeEntry besideHighway() {
            nearHighway = true;
            return this;
        }

        public FakeEntry maxDiff(int value) {
            maxDiff = value;
            return this;
        }

        public FakeEntry biome(BiPredicate<Integer, Integer> value) {
            biome = value;
            return this;
        }

        @Override
        public String name() {
            return asset.name;
        }

        @Override
        public int weight() {
            return weight;
        }

        @Override
        public boolean nearHighway() {
            return nearHighway;
        }

        @Override
        public boolean allowVoid() {
            return false;
        }

        @Override
        public Integer maxHeightDiff() {
            return maxDiff;
        }

        @Override
        public boolean acceptsBiome(int chunkX, int chunkZ) {
            return biome.test(chunkX, chunkZ);
        }

        @Override
        public ScatteredView.Asset asset() {
            return asset;
        }
    }

    /** Fake settings. */
    public static final class FakeScattered implements ScatteredView {

        final int areaSize;
        final float chance;
        final int weightNone;
        final List<Entry> entries = new ArrayList<>();

        public FakeScattered(int areaSize, float chance, int weightNone) {
            this.areaSize = areaSize;
            this.chance = chance;
            this.weightNone = weightNone;
        }

        public FakeScattered add(FakeEntry entry) {
            entries.add(entry);
            return this;
        }

        @Override
        public int areaSize() {
            return areaSize;
        }

        @Override
        public float chance() {
            return chance;
        }

        @Override
        public int weightNone() {
            return weightNone;
        }

        @Override
        public List<Entry> entries() {
            return entries;
        }
    }

    /** Assets with scattered settings over another asset view. */
    public static AssetView withScattered(AssetView base, ScatteredView scattered) {
        WorldStyleView style = base.worldStyle();
        WorldStyleView withScattered = new WorldStyleView() {

            @Override
            public String pickCityStyle(Random random, int chunkX, int chunkZ) {
                return style.pickCityStyle(random, chunkX, chunkZ);
            }

            @Override
            public float cityChanceMultiplier(int chunkX, int chunkZ) {
                return style.cityChanceMultiplier(chunkX, chunkZ);
            }

            @Override
            public MultiSettings multiSettings() {
                return style.multiSettings();
            }

            @Override
            public String outsideStyleName() {
                return style.outsideStyleName();
            }

            @Override
            public int railPartHeight6() {
                return style.railPartHeight6();
            }

            @Override
            public boolean buildingsBlockRailways() {
                return style.buildingsBlockRailways();
            }

            @Override
            public ScatteredView scattered() {
                return scattered;
            }
        };
        return new AssetView() {

            @Override
            public WorldStyleView worldStyle() {
                return withScattered;
            }

            @Override
            public CityStyleView cityStyle(String name) {
                return base.cityStyle(name);
            }

            @Override
            public BuildingView building(String name) {
                return base.building(name);
            }

            @Override
            public MultiBuildingView multiBuilding(String name) {
                return base.multiBuilding(name);
            }

            @Override
            public StyleView style(String name) {
                return base.style(name);
            }

            @Override
            public boolean partDontConnect(String partName) {
                return base.partDontConnect(partName);
            }
        };
    }

    /** Column heights as a function of block coordinates. */
    public interface ColumnHeight {

        int at(int blockX, int blockZ);
    }

    /** A terrain whose chunk height is its column at block (8, 8), counting its calls; no water. */
    public static final class ColumnTerrain implements TerrainView {

        final ColumnHeight columns;
        int calls;

        public ColumnTerrain(ColumnHeight columns) {
            this.columns = columns;
        }

        @Override
        public int height(int chunkX, int chunkZ) {
            calls++;
            return columns.at((chunkX << 4) + 8, (chunkZ << 4) + 8);
        }

        @Override
        public int columnHeight(int blockX, int blockZ) {
            calls++;
            return columns.at(blockX, blockZ);
        }

        @Override
        public boolean isWaterBiome(int chunkX, int chunkZ) {
            return false;
        }

        @Override
        public boolean isOcean(int chunkX, int chunkZ) {
            return false;
        }
    }

    /** Highways along X at every chunk row {@code z == row} and nowhere else, at one city level. */
    public static InfrastructureView highwayAlongX(int row, int level) {
        return new InfrastructureView() {

            @Override
            public int highwayXLevel(int chunkX, int chunkZ) {
                return chunkZ == row ? level : -1;
            }

            @Override
            public int highwayZLevel(int chunkX, int chunkZ) {
                return -1;
            }

            @Override
            public RailInfo rail(int chunkX, int chunkZ) {
                return RailInfo.NONE;
            }

            @Override
            public int railwayLevel() {
                return 0;
            }
        };
    }

    /** A profile with no cities. */
    public static LayoutProfile noCities() {
        return LayoutProfile.builder()
            .cityChance(0)
            .build();
    }

    /** A planner over the standard assets with scattered settings, no cities unless the profile has them. */
    public static LayoutPlanner planner(long seed, LayoutProfile profile, ScatteredView scattered, TerrainView terrain,
        StructureOracle oracle, InfrastructureView infrastructure) {
        return new LayoutPlanner(
            LayoutInputs.builder(seed, profile, withScattered(LayoutFixtures.standardAssets(), scattered), terrain)
                .structures(oracle)
                .infrastructure(infrastructure)
                .build());
    }
}
