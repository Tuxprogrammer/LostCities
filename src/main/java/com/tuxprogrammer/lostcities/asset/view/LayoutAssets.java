package com.tuxprogrammer.lostcities.asset.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.match.BiomeSource;
import com.tuxprogrammer.lostcities.asset.model.Building;
import com.tuxprogrammer.lostcities.asset.model.CityStyle;
import com.tuxprogrammer.lostcities.asset.model.ConditionContext;
import com.tuxprogrammer.lostcities.asset.model.MultiBuilding;
import com.tuxprogrammer.lostcities.asset.model.Palette;
import com.tuxprogrammer.lostcities.asset.model.Part;
import com.tuxprogrammer.lostcities.asset.model.Selector;
import com.tuxprogrammer.lostcities.asset.model.Style;
import com.tuxprogrammer.lostcities.asset.model.WorldStyle;
import com.tuxprogrammer.lostcities.layout.view.AssetView;
import com.tuxprogrammer.lostcities.layout.view.BuildingView;
import com.tuxprogrammer.lostcities.layout.view.CityStyleView;
import com.tuxprogrammer.lostcities.layout.view.MultiBuildingView;
import com.tuxprogrammer.lostcities.layout.view.MultiSettings;
import com.tuxprogrammer.lostcities.layout.view.PartContext;
import com.tuxprogrammer.lostcities.layout.view.Selection;
import com.tuxprogrammer.lostcities.layout.view.StyleView;
import com.tuxprogrammer.lostcities.layout.view.WorldStyleView;

/**
 * The asset set as {@code layout} reads it: adapters from the asset model to the narrow views the layout owns
 * (decision log D51, {@code layout.view}). Both packages are pure Java, so the adapter needs no glue; the world's
 * biomes
 * come in through a {@link BiomeSource}.
 *
 * <p>
 * Names: every name the views return is a full id ({@code lostcities:building1}), and every name they accept may be
 * bare
 * or full (P05-2, D31). {@link PartContext#NONE} becomes "no part". Draw contracts: each view method draws exactly what
 * its interface documents, by delegating to the model's picks, which keep upstream's draws. One gap: when a city-style
 * list has entries but every weight is zero at a chunk (spawn-distance windows), the pick still draws its one
 * {@code nextFloat()}, so the draw count holds, and returns {@code null}, because the new spec picks nothing there
 * (ASSET-10); {@link CityStyleView#pick} documents {@code null} only for an empty list.
 *
 * <p>
 * Immutable. Side: both (server-side use).
 */
public final class LayoutAssets implements AssetView {

    private final AssetSet assets;
    private final WorldStyle worldStyle;
    private final BiomeSource biomes;
    private final boolean variety;

    /**
     * Creates the view, with the palette variety on.
     *
     * @param assets     the loaded asset set
     * @param worldStyle the profile's world style
     * @param biomes     the world's chunk biomes
     */
    public LayoutAssets(AssetSet assets, WorldStyle worldStyle, BiomeSource biomes) {
        this(assets, worldStyle, biomes, true);
    }

    /**
     * Creates the view.
     *
     * @param assets     the loaded asset set
     * @param worldStyle the profile's world style
     * @param biomes     the world's chunk biomes
     * @param variety    whether styles draw the palette variety (the world's pinned {@code paletteVariety} module)
     */
    public LayoutAssets(AssetSet assets, WorldStyle worldStyle, BiomeSource biomes, boolean variety) {
        this.assets = assets;
        this.worldStyle = worldStyle;
        this.biomes = biomes;
        this.variety = variety;
    }

    @Override
    public WorldStyleView worldStyle() {
        return new WorldStyleAdapter(worldStyle, biomes);
    }

    @Override
    public CityStyleView cityStyle(String name) {
        CityStyle style = assets.cityStyle(id(name));
        return style == null ? null : new CityStyleAdapter(style);
    }

    @Override
    public BuildingView building(String name) {
        Building building = assets.building(id(name));
        return building == null ? null : new BuildingAdapter(building);
    }

    @Override
    public MultiBuildingView multiBuilding(String name) {
        MultiBuilding multi = assets.multiBuilding(id(name));
        return multi == null ? null : new MultiBuildingAdapter(multi);
    }

    @Override
    public StyleView style(String name) {
        Style style = assets.style(id(name));
        if (style == null) {
            return null;
        }
        return new StyleView() {

            @Override
            public List<String> pickPalettes(Random random) {
                List<String> out = new ArrayList<>();
                for (Palette palette : style.randomPalettes(random)) {
                    out.add(
                        palette.id()
                            .toString());
                }
                return out;
            }

            @Override
            public List<String> pickVariety(List<String> chosen, boolean building, long seed) {
                if (!variety) {
                    return Collections.emptyList();
                }
                return assets.variety()
                    .pick(style.id(), chosen, building, seed);
            }
        };
    }

    @Override
    public boolean partDontConnect(String partName) {
        Part part = assets.part(id(partName));
        return part != null && part.metaBoolean(Part.MetaKey.DONTCONNECT);
    }

    /** Returns the id of a name, or null for a malformed or "none" name. */
    static AssetId id(String name) {
        if (name == null || PartContext.NONE.equals(name)) {
            return null;
        }
        try {
            return AssetId.parse(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** The selector list behind a layout selection. */
    static CityStyle.SelectorKind kind(Selection selection) {
        switch (selection) {
            case BUILDINGS:
                return CityStyle.SelectorKind.BUILDINGS;
            case MULTIBUILDINGS:
                return CityStyle.SelectorKind.MULTIBUILDINGS;
            case PARKS:
                return CityStyle.SelectorKind.PARKS;
            case FOUNTAINS:
                return CityStyle.SelectorKind.FOUNTAINS;
            case BRIDGES:
                return CityStyle.SelectorKind.BRIDGES;
            case LARGE_BRIDGES:
                return CityStyle.SelectorKind.LARGE_BRIDGES;
            case STAIRS:
                return CityStyle.SelectorKind.STAIRS;
            case FRONTS:
                return CityStyle.SelectorKind.FRONTS;
            default:
                return CityStyle.SelectorKind.RAIL_DUNGEONS;
        }
    }

    /** A world style as the layout reads it. */
    private static final class WorldStyleAdapter implements WorldStyleView {

        private final WorldStyle style;
        private final BiomeSource biomes;

        WorldStyleAdapter(WorldStyle style, BiomeSource biomes) {
            this.style = style;
            this.biomes = biomes;
        }

        @Override
        public String pickCityStyle(Random random, int chunkX, int chunkZ) {
            CityStyle picked = style.randomCityStyle(biomes.biomeOfChunk(chunkX, chunkZ), random);
            return picked == null ? null
                : picked.id()
                    .toString();
        }

        @Override
        public float maxCityChanceMultiplier() {
            return style.maxCityChanceMultiplier();
        }

        @Override
        public float cityChanceMultiplier(int chunkX, int chunkZ) {
            return style.cityChanceMultiplier(biomes.biomeOfChunk(chunkX, chunkZ));
        }

        @Override
        public MultiSettings multiSettings() {
            WorldStyle.MultiSettings settings = style.multiSettings();
            return new MultiSettings(
                settings.areaSize(),
                settings.minimum(),
                settings.maximum(),
                settings.correctStyleFactor(),
                settings.attempts());
        }

        @Override
        public String outsideStyleName() {
            return style.outsideStyle()
                .id()
                .toString();
        }

        @Override
        public int railPartHeight6() {
            return style.railPartHeight6();
        }

        @Override
        public boolean buildingsBlockRailways() {
            return style.railwayAvoidance() == WorldStyle.RailwayAvoidance.BLOCK_RAILWAY;
        }
    }

    /** A city style as the layout reads it. */
    private static final class CityStyleAdapter implements CityStyleView {

        private final CityStyle style;

        CityStyleAdapter(CityStyle style) {
            this.style = style;
        }

        @Override
        public String name() {
            return style.id()
                .toString();
        }

        @Override
        public String styleName() {
            return style.style() == null ? null
                : style.style()
                    .id()
                    .toString();
        }

        @Override
        public Float buildingChance() {
            return style.number(CityStyle.Setting.BUILDING_CHANCE);
        }

        @Override
        public Integer minFloors() {
            return style.integer(CityStyle.Setting.MIN_FLOORS);
        }

        @Override
        public Integer maxFloors() {
            return style.integer(CityStyle.Setting.MAX_FLOORS);
        }

        @Override
        public Integer minCellars() {
            return style.integer(CityStyle.Setting.MIN_CELLARS);
        }

        @Override
        public Integer maxCellars() {
            return style.integer(CityStyle.Setting.MAX_CELLARS);
        }

        @Override
        public Float parkChance() {
            return style.number(CityStyle.Setting.PARK_CHANCE);
        }

        @Override
        public Float openLotParkChance() {
            return style.number(CityStyle.Setting.OPEN_LOT_PARK_CHANCE);
        }

        @Override
        public Float fountainChance() {
            return style.number(CityStyle.Setting.FOUNTAIN_CHANCE);
        }

        @Override
        public Float frontChance() {
            return style.number(CityStyle.Setting.FRONT_CHANCE);
        }

        @Override
        public Float corridorChance() {
            return style.number(CityStyle.Setting.CORRIDOR_CHANCE);
        }

        @Override
        public Boolean avoidFoliage() {
            return style.flag(CityStyle.Setting.AVOID_FOLIAGE);
        }

        @Override
        public Boolean parkBorder() {
            return style.flag(CityStyle.Setting.PARK_BORDER);
        }

        @Override
        public Boolean parkElevation() {
            return style.flag(CityStyle.Setting.PARK_ELEVATION);
        }

        @Override
        public Integer parkStreetThreshold() {
            return style.integer(CityStyle.Setting.PARK_STREET_THRESHOLD);
        }

        @Override
        public String pick(Selection list, Random random, int chunkX, int chunkZ) {
            List<Selector> selectors = style.selectors(kind(list));
            if (selectors.isEmpty()) {
                return null;
            }
            boolean anyWeight = false;
            for (Selector selector : selectors) {
                anyWeight |= selector.weight(chunkX, chunkZ) > 0;
            }
            if (!anyWeight) {
                // Keep the documented single draw; with every weight zero nothing is picked (ASSET-10).
                random.nextFloat();
                return null;
            }
            Selector picked = Selector.pick(random, selectors, chunkX, chunkZ);
            return picked.value()
                .toString();
        }
    }

    /** A building as the layout reads it. */
    private static final class BuildingAdapter implements BuildingView {

        private final Building building;

        BuildingAdapter(Building building) {
            this.building = building;
        }

        @Override
        public String name() {
            return building.id()
                .toString();
        }

        @Override
        public int minFloors() {
            return building.minFloors();
        }

        @Override
        public int maxFloors() {
            return building.maxFloors();
        }

        @Override
        public int minCellars() {
            return building.minCellars();
        }

        @Override
        public int maxCellars() {
            return building.maxCellars();
        }

        @Override
        public boolean overrideFloors() {
            return building.overrideFloors();
        }

        @Override
        public float prefersLonely() {
            return building.prefersLonely();
        }

        @Override
        public String pickPart(Random random, PartContext context) {
            return name(building.randomPart(random, context(context)));
        }

        @Override
        public String pickPart2(Random random, PartContext context) {
            return name(building.randomPart2(random, context(context)));
        }

        private static String name(Part part) {
            return part == null ? null
                : part.id()
                    .toString();
        }

        private ConditionContext context(PartContext context) {
            return ConditionContext.builder()
                .level(context.level())
                .floor(context.floor())
                .floors(context.cellars(), context.floors())
                .part(id(context.part()))
                .belowPart(id(context.belowPart()))
                .building(building.id())
                .chunk(context.chunkX(), context.chunkZ())
                .build();
        }
    }

    /** A multibuilding as the layout reads it. */
    private static final class MultiBuildingAdapter implements MultiBuildingView {

        private final MultiBuilding multi;

        MultiBuildingAdapter(MultiBuilding multi) {
            this.multi = multi;
        }

        @Override
        public String name() {
            return multi.id()
                .toString();
        }

        @Override
        public int dimX() {
            return multi.dimX();
        }

        @Override
        public int dimZ() {
            return multi.dimZ();
        }

        @Override
        public String buildingAt(int x, int z) {
            Building building = multi.building(x, z);
            return building == null ? null
                : building.id()
                    .toString();
        }
    }
}
