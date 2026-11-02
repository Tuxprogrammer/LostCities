package com.tuxprogrammer.lostcities.asset.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.tuxprogrammer.lostcities.asset.match.BiomeSource;
import com.tuxprogrammer.lostcities.asset.model.Building;
import com.tuxprogrammer.lostcities.asset.model.MultiBuilding;
import com.tuxprogrammer.lostcities.asset.model.Part;
import com.tuxprogrammer.lostcities.asset.model.Scattered;
import com.tuxprogrammer.lostcities.asset.model.WorldStyle;
import com.tuxprogrammer.lostcities.layout.view.ScatteredView;

/**
 * A world style's {@code scattered} settings as the layout reads them ({@link ScatteredView}): the entries in file
 * order, each testing its biome filter against the world's chunk biomes, and each asset's placement rules and height.
 *
 * <p>
 * Spec: docs/original-spec/07-scattered/scattered-structures.md § 6. Immutable. Side: both (server-side use).
 */
final class ScatteredAdapter implements ScatteredView {

    private final int areaSize;
    private final float chance;
    private final int weightNone;
    private final List<Entry> entries;

    private ScatteredAdapter(WorldStyle style, BiomeSource biomes) {
        this.areaSize = style.scatteredAreaSize();
        this.chance = style.scatteredChance();
        this.weightNone = style.scatteredWeightNone();
        List<Entry> list = new ArrayList<>();
        for (WorldStyle.ScatteredRef ref : style.scattered()) {
            list.add(new EntryAdapter(ref, biomes));
        }
        this.entries = Collections.unmodifiableList(list);
    }

    /**
     * Returns the view of a world style's scattered settings.
     *
     * @param style  the world style
     * @param biomes the world's chunk biomes
     * @return the view, or {@code null} when the world style has none
     */
    static ScatteredView of(WorldStyle style, BiomeSource biomes) {
        if (style.scatteredAreaSize() == null || style.scattered()
            .isEmpty()) {
            return null;
        }
        return new ScatteredAdapter(style, biomes);
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

    /**
     * Returns the most blocks a building can rise above its first floor: its greatest floor count (at least 1) times
     * its tallest main or overlay part.
     *
     * @param building the building
     * @return blocks
     */
    static int buildingHeight(Building building) {
        int tallest = 0;
        for (Building.PartRef ref : building.parts()) {
            tallest = Math.max(tallest, height(ref.part()));
        }
        for (Building.PartRef ref : building.parts2()) {
            tallest = Math.max(tallest, height(ref.part()));
        }
        return Math.max(1, building.maxFloors()) * tallest;
    }

    private static int height(Part part) {
        return part == null ? 0 : part.height();
    }

    /** One list entry. */
    private static final class EntryAdapter implements Entry {

        private final WorldStyle.ScatteredRef ref;
        private final BiomeSource biomes;
        private final Asset asset;

        EntryAdapter(WorldStyle.ScatteredRef ref, BiomeSource biomes) {
            this.ref = ref;
            this.biomes = biomes;
            this.asset = ref.scattered() == null ? null : new AssetAdapter(ref.scattered());
        }

        @Override
        public String name() {
            return ref.scattered() == null ? ""
                : ref.scattered()
                    .id()
                    .toString();
        }

        @Override
        public int weight() {
            return ref.weight();
        }

        @Override
        public boolean nearHighway() {
            return ref.nearHighway();
        }

        @Override
        public boolean allowVoid() {
            return ref.allowVoid();
        }

        @Override
        public Integer maxHeightDiff() {
            return ref.maxHeightDiff();
        }

        @Override
        public boolean acceptsBiome(int chunkX, int chunkZ) {
            return ref.biomes()
                .isAny()
                || ref.biomes()
                    .matches(biomes.biomeOfChunk(chunkX, chunkZ));
        }

        @Override
        public Asset asset() {
            return asset;
        }
    }

    /** One scattered asset. */
    private static final class AssetAdapter implements Asset {

        private final Scattered scattered;
        private final List<String> buildings;
        private final int maxHeight;

        AssetAdapter(Scattered scattered) {
            this.scattered = scattered;
            List<String> names = new ArrayList<>();
            int tallest = 0;
            for (Building building : scattered.buildings()) {
                names.add(
                    building.id()
                        .toString());
                tallest = Math.max(tallest, buildingHeight(building));
            }
            MultiBuilding multi = scattered.multiBuilding();
            if (multi != null) {
                for (Building building : multi.buildings()) {
                    tallest = Math.max(tallest, buildingHeight(building));
                }
            }
            this.buildings = Collections.unmodifiableList(names);
            this.maxHeight = tallest;
        }

        @Override
        public String name() {
            return scattered.id()
                .toString();
        }

        @Override
        public List<String> buildings() {
            return buildings;
        }

        @Override
        public String multiBuilding() {
            return scattered.multiBuilding() == null ? null
                : scattered.multiBuilding()
                    .id()
                    .toString();
        }

        @Override
        public boolean rotatable() {
            return scattered.rotatable();
        }

        @Override
        public boolean clearHighwayRailing() {
            return scattered.clearHighwayRailing();
        }

        @Override
        public String supportPart() {
            return scattered.supportPart() == null ? null
                : scattered.supportPart()
                    .id()
                    .toString();
        }

        @Override
        public TerrainHeight terrainHeight() {
            return TerrainHeight.valueOf(
                scattered.terrainHeight()
                    .name());
        }

        @Override
        public TerrainFix terrainFix() {
            return TerrainFix.valueOf(
                scattered.terrainFix()
                    .name());
        }

        @Override
        public int heightOffset() {
            return scattered.heightOffset();
        }

        @Override
        public int maxHeight() {
            return maxHeight;
        }
    }
}
