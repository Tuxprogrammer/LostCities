package com.tuxprogrammer.lostcities.worldgen.city;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.AssetType;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.BlockRegistryView;
import com.tuxprogrammer.lostcities.asset.block.MetalFlags;
import com.tuxprogrammer.lostcities.asset.block.TileTemplate;
import com.tuxprogrammer.lostcities.asset.model.Building;
import com.tuxprogrammer.lostcities.asset.model.Palette;
import com.tuxprogrammer.lostcities.asset.model.PaletteEntry;
import com.tuxprogrammer.lostcities.asset.model.Part;
import com.tuxprogrammer.lostcities.asset.model.Style;
import com.tuxprogrammer.lostcities.asset.palette.CompiledPalette;
import com.tuxprogrammer.lostcities.asset.transform.Transform;

/**
 * Writes every bundled part under every city style's palettes, with the palette variety drawn for a range of seeds, and
 * reports what would not generate cleanly: a character the palette leaves undefined, or a block the registry does not
 * have (M11's exit gate: "every building and part generates without missing or unmapped blocks"). A part that belongs
 * to buildings is written once per building, under that building's palette, as a floor part (with the interior
 * characters) and as a plain part; any other part is written with the style's palettes alone.
 *
 * <p>
 * The unit test runs it over the test registry; the {@code assets.catalog} scenario runs it in the real pack over the
 * live registry, so a missing pack block shows up there. Pure Java. Side: both.
 */
public final class AssetCatalogCheck {

    /** What one run found. */
    public static final class Report {

        private final int writes;
        private final int blocksWritten;
        private final Set<String> distinctBlocks;
        private final Map<String, Set<String>> undefined;
        private final Map<String, Set<String>> unregistered;

        Report(int writes, int blocksWritten, Set<String> distinctBlocks, Map<String, Set<String>> undefined,
            Map<String, Set<String>> unregistered) {
            this.writes = writes;
            this.blocksWritten = blocksWritten;
            this.distinctBlocks = Collections.unmodifiableSet(distinctBlocks);
            this.undefined = Collections.unmodifiableMap(undefined);
            this.unregistered = Collections.unmodifiableMap(unregistered);
        }

        /** Returns how many part writes ran. */
        public int writes() {
            return writes;
        }

        /** Returns how many blocks the writes placed in all. */
        public int blocksWritten() {
            return blocksWritten;
        }

        /** Returns every distinct block written, {@code modid:name:meta}, sorted. */
        public Set<String> distinctBlocks() {
            return distinctBlocks;
        }

        /** Returns, per part and style, the characters the palette left undefined; empty when none. */
        public Map<String, Set<String>> undefined() {
            return undefined;
        }

        /** Returns, per part and style, the blocks written that the registry does not have; empty when none. */
        public Map<String, Set<String>> unregistered() {
            return unregistered;
        }

        /** Returns one summary line for the log. */
        public String summary() {
            return writes + " part writes, "
                + blocksWritten
                + " blocks, "
                + distinctBlocks.size()
                + " distinct blocks; "
                + undefined.size()
                + " part and style pairs with undefined characters, "
                + unregistered.size()
                + " with unregistered blocks";
        }
    }

    private AssetCatalogCheck() {}

    /**
     * Runs the check.
     *
     * @param assets   the loaded assets
     * @param flags    the decorative-metal flags to compile palettes for
     * @param registry the block registry to check written blocks against
     * @param seeds    how many variety seeds to draw per style (each also seeds the style's palette draw)
     * @return the report
     */
    public static Report run(AssetSet assets, MetalFlags flags, BlockRegistryView registry, int seeds) {
        Map<AssetId, List<AssetId>> buildingsOfPart = buildingsOfParts(assets);
        CityPalettes palettes = new CityPalettes(assets, flags);
        Map<String, Set<String>> undefined = new TreeMap<>();
        Map<String, Set<String>> unregistered = new TreeMap<>();
        Set<String> distinct = new TreeSet<>();
        int writes = 0;
        int blocks = 0;
        for (AssetId styleId : assets.ids(AssetType.STYLES)) {
            Style style = assets.style(styleId);
            for (int seed = 0; seed < seeds; seed++) {
                List<String> chosen = new ArrayList<>();
                for (Palette palette : style.randomPalettes(new Random(seed))) {
                    chosen.add(
                        palette.id()
                            .toString());
                }
                for (AssetId partId : assets.ids(AssetType.PARTS)) {
                    Part part = assets.part(partId);
                    List<AssetId> owners = buildingsOfPart.get(partId);
                    List<AssetId> contexts = owners == null ? Collections.<AssetId>singletonList(null) : owners;
                    for (AssetId building : contexts) {
                        boolean inBuilding = building != null;
                        List<String> names = new ArrayList<>(chosen);
                        names.addAll(
                            assets.variety()
                                .pick(styleId, chosen, inBuilding, mix(seed, partId, building)));
                        CityPalettes.ChunkPalette chunk = palettes
                            .chunk(names, building == null ? null : building.toString());
                        CompiledPalette palette = palettes.forPart(chunk, part);
                        String key = partId + " in " + styleId + (building == null ? "" : " for " + building);
                        for (boolean interior : inBuilding ? new boolean[] { false, true } : new boolean[] { false }) {
                            Recorder recorder = new Recorder();
                            Set<String> missing = new TreeSet<>();
                            PartWriter writer = new PartWriter(
                                recorder,
                                new PaletteSequence(seed, 0, 0),
                                new PartWriter.Rules(
                                    true,
                                    true,
                                    false,
                                    false,
                                    false,
                                    0,
                                    BlockRef.of("minecraft:water", 0)),
                                message -> missing.add(message));
                            writer.place(
                                new PartWriter.Placement(
                                    part,
                                    palette,
                                    Transform.ROTATE_NONE,
                                    0,
                                    0,
                                    0,
                                    HardAir.AIR,
                                    building).interior(interior));
                            writes++;
                            blocks += recorder.count;
                            for (String message : missing) {
                                undefined.computeIfAbsent(key, k -> new TreeSet<>())
                                    .add(message);
                            }
                            for (BlockRef block : recorder.blocks) {
                                distinct.add(block.toString());
                                if (!block.isAir() && !registry.exists(block.name())) {
                                    unregistered.computeIfAbsent(key, k -> new TreeSet<>())
                                        .add(block.toString());
                                }
                            }
                        }
                    }
                }
            }
        }
        return new Report(writes, blocks, distinct, undefined, unregistered);
    }

    /** Returns, for each part a building names (as a floor part or an overlay), the buildings that name it. */
    static Map<AssetId, List<AssetId>> buildingsOfParts(AssetSet assets) {
        Map<AssetId, List<AssetId>> out = new TreeMap<>();
        for (AssetId buildingId : assets.ids(AssetType.BUILDINGS)) {
            Building building = assets.building(buildingId);
            List<Building.PartRef> refs = new ArrayList<>(building.parts());
            refs.addAll(building.parts2());
            for (Building.PartRef ref : refs) {
                List<AssetId> owners = out.computeIfAbsent(
                    ref.part()
                        .id(),
                    k -> new ArrayList<>());
                if (!owners.contains(buildingId)) {
                    owners.add(buildingId);
                }
            }
        }
        return out;
    }

    private static long mix(int seed, AssetId part, AssetId building) {
        return seed * 0x9E3779B97F4A7C15L ^ part.hashCode() * 31L ^ (building == null ? 0 : building.hashCode());
    }

    /** A target that only records what is placed. */
    private static final class Recorder implements CityTarget {

        private final Set<BlockRef> blocks = new java.util.LinkedHashSet<>();
        private int count;

        @Override
        public boolean dryRun() {
            return false;
        }

        @Override
        public BlockRef get(int x, int y, int z) {
            return BlockRef.AIR;
        }

        @Override
        public void set(int x, int y, int z, BlockRef block) {
            blocks.add(block);
            count++;
        }

        @Override
        public void clearToMax(int fromY) {}

        @Override
        public void torch(int x, int y, int z) {}

        @Override
        public void chest(int x, int y, int z, PaletteEntry info, TileTemplate template, Transform transform,
            PartWriter.Placement placement) {}

        @Override
        public void spawner(int x, int y, int z, PaletteEntry info, PartWriter.Placement placement) {}

        @Override
        public void tileEntity(int x, int y, int z, TileTemplate template, Transform transform) {}
    }
}
