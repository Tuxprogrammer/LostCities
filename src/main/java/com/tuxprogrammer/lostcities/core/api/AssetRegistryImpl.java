package com.tuxprogrammer.lostcities.core.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Function;

import com.tuxprogrammer.lostcities.api.ILostCityAsset;
import com.tuxprogrammer.lostcities.api.ILostCityAssetRegistry;
import com.tuxprogrammer.lostcities.api.ILostCityBuilding;
import com.tuxprogrammer.lostcities.api.ILostCityCityStyle;
import com.tuxprogrammer.lostcities.api.ILostCityMultiBuilding;
import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.AssetType;
import com.tuxprogrammer.lostcities.asset.model.Building;
import com.tuxprogrammer.lostcities.asset.model.CityStyle;
import com.tuxprogrammer.lostcities.asset.model.MultiBuilding;

/**
 * One asset type of the loaded assets as an {@link ILostCityAssetRegistry}, each asset behind a small view that shows
 * only plain values (avoids CORE-38: no internal palette or block type in a signature). Side: both (plain Java).
 *
 * @param <T> the API type
 */
final class AssetRegistryImpl<T extends ILostCityAsset> implements ILostCityAssetRegistry<T> {

    private final AssetSet assets;
    private final AssetType type;
    private final Function<AssetId, T> view;

    private AssetRegistryImpl(AssetSet assets, AssetType type, Function<AssetId, T> view) {
        this.assets = assets;
        this.type = type;
        this.view = view;
    }

    /** Returns the buildings of an asset set. */
    static ILostCityAssetRegistry<ILostCityBuilding> buildings(AssetSet assets) {
        return new AssetRegistryImpl<>(assets, AssetType.BUILDINGS, id -> {
            Building building = assets.building(id);
            return building == null ? null : new BuildingView(building);
        });
    }

    /** Returns the multibuildings of an asset set. */
    static ILostCityAssetRegistry<ILostCityMultiBuilding> multiBuildings(AssetSet assets) {
        return new AssetRegistryImpl<>(assets, AssetType.MULTIBUILDINGS, id -> {
            MultiBuilding multi = assets.multiBuilding(id);
            return multi == null ? null : new MultiBuildingView(multi);
        });
    }

    /** Returns the city styles of an asset set. */
    static ILostCityAssetRegistry<ILostCityCityStyle> cityStyles(AssetSet assets) {
        return new AssetRegistryImpl<>(assets, AssetType.CITYSTYLES, id -> {
            CityStyle style = assets.cityStyle(id);
            return style == null ? null : new CityStyleView(style);
        });
    }

    @Override
    public T get(String name) {
        if (name == null) {
            return null;
        }
        AssetId id;
        try {
            id = AssetId.parse(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
        return view.apply(id);
    }

    @Override
    public List<T> getAll() {
        List<T> out = new ArrayList<>();
        for (AssetId id : new TreeSet<>(assets.ids(type))) {
            T asset = view.apply(id);
            if (asset != null) {
                out.add(asset);
            }
        }
        return Collections.unmodifiableList(out);
    }

    /** The name and id every view shares. */
    private abstract static class View implements ILostCityAsset {

        private final AssetId id;

        View(AssetId id) {
            this.id = id;
        }

        @Override
        public String getName() {
            return id.path();
        }

        @Override
        public String getId() {
            return id.toString();
        }

        @Override
        public String toString() {
            return id.toString();
        }
    }

    /** A building. */
    private static final class BuildingView extends View implements ILostCityBuilding {

        private final Building building;

        BuildingView(Building building) {
            super(building.id());
            this.building = building;
        }

        @Override
        public int getMinFloors() {
            return building.minFloors();
        }

        @Override
        public int getMaxFloors() {
            return building.maxFloors();
        }

        @Override
        public int getMinCellars() {
            return building.minCellars();
        }

        @Override
        public int getMaxCellars() {
            return building.maxCellars();
        }

        @Override
        public float getPrefersLonely() {
            return building.prefersLonely();
        }

        @Override
        public boolean getAllowDoors() {
            return building.allowDoors();
        }

        @Override
        public boolean getAllowFillers() {
            return building.allowFillers();
        }

        @Override
        public boolean getOverrideFloors() {
            return building.overrideFloors();
        }
    }

    /** A multibuilding. */
    private static final class MultiBuildingView extends View implements ILostCityMultiBuilding {

        private final MultiBuilding multi;

        MultiBuildingView(MultiBuilding multi) {
            super(multi.id());
            this.multi = multi;
        }

        @Override
        public String getBuilding(int x, int z) {
            if (x < 0 || z < 0 || x >= multi.dimX() || z >= multi.dimZ()) {
                return null;
            }
            Building building = multi.building(x, z);
            return building == null ? null
                : building.id()
                    .toString();
        }

        @Override
        public int getDimX() {
            return multi.dimX();
        }

        @Override
        public int getDimZ() {
            return multi.dimZ();
        }
    }

    /** A city style. */
    private static final class CityStyleView extends View implements ILostCityCityStyle {

        private final CityStyle style;

        CityStyleView(CityStyle style) {
            super(style.id());
            this.style = style;
        }

        @Override
        public String getStyle() {
            return style.style() == null ? null
                : style.style()
                    .id()
                    .toString();
        }

        @Override
        public Float getExplosionChance() {
            return style.number(CityStyle.Setting.EXPLOSION_CHANCE);
        }

        @Override
        public Integer getMinFloorCount() {
            return style.integer(CityStyle.Setting.MIN_FLOORS);
        }

        @Override
        public Integer getMaxFloorCount() {
            return style.integer(CityStyle.Setting.MAX_FLOORS);
        }

        @Override
        public Integer getMinCellarCount() {
            return style.integer(CityStyle.Setting.MIN_CELLARS);
        }

        @Override
        public Integer getMaxCellarCount() {
            return style.integer(CityStyle.Setting.MAX_CELLARS);
        }

        @Override
        public Float getBuildingChance() {
            return style.number(CityStyle.Setting.BUILDING_CHANCE);
        }

        @Override
        public Float getParkChance() {
            return style.number(CityStyle.Setting.PARK_CHANCE);
        }

        @Override
        public Float getFountainChance() {
            return style.number(CityStyle.Setting.FOUNTAIN_CHANCE);
        }

        @Override
        public Float getCorridorChance() {
            return style.number(CityStyle.Setting.CORRIDOR_CHANCE);
        }

        @Override
        public Float getFrontChance() {
            return style.number(CityStyle.Setting.FRONT_CHANCE);
        }

        @Override
        public Boolean getAvoidFoliage() {
            return style.flag(CityStyle.Setting.AVOID_FOLIAGE);
        }

        @Override
        public Boolean getParkBorder() {
            return style.flag(CityStyle.Setting.PARK_BORDER);
        }

        @Override
        public Boolean getParkElevation() {
            return style.flag(CityStyle.Setting.PARK_ELEVATION);
        }

        @Override
        public Integer getParkStreetThreshold() {
            return style.integer(CityStyle.Setting.PARK_STREET_THRESHOLD);
        }
    }
}
