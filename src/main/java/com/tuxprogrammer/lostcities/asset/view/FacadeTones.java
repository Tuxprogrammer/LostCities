package com.tuxprogrammer.lostcities.asset.view;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.block.BlockChoice;
import com.tuxprogrammer.lostcities.asset.block.BlockColors;
import com.tuxprogrammer.lostcities.asset.model.Building;
import com.tuxprogrammer.lostcities.asset.model.Palette;
import com.tuxprogrammer.lostcities.asset.model.PaletteEntry;
import com.tuxprogrammer.lostcities.asset.model.Part;
import com.tuxprogrammer.lostcities.asset.model.RandomList;
import com.tuxprogrammer.lostcities.layout.view.FacadeTone;
import com.tuxprogrammer.lostcities.layout.view.FacadeView;

/**
 * Measures the tone of a building's facade under a stack of palettes, from the shipped block colors
 * (docs/design/0010-facade-color-feathering.md §2):
 * <ul>
 * <li>the building's facade characters are those on the outer ring of its parts, counted over every part;</li>
 * <li>each resolves through the stack as {@code CompiledPalette} resolves it (the topmost defining layer, references
 * followed), a random list to the mean of its 128 slots;</li>
 * <li>see-through and unknown blocks are skipped, and the rest are averaged in CIELAB, weighted by their counts.</li>
 * </ul>
 *
 * <p>
 * Results are cached per stack and building in a bounded map this view owns. Not thread-safe; world generation runs on
 * the server thread. Side: both (plain Java).
 */
public final class FacadeTones implements FacadeView {

    /** Tones kept: style picks times buildings in one region stay well under this. */
    static final int CAPACITY = 4096;
    /** Most reference hops followed before a character counts as undefined (a loop in a broken asset). */
    private static final int MAX_REFERENCE_DEPTH = 8;
    /** The marker cached for a stack with no known facade color. */
    private static final FacadeTone UNKNOWN = new FacadeTone(Float.NaN, Float.NaN);

    private final AssetSet assets;
    private final BlockColors colors;
    private final Map<String, FacadeTone> tones = new LinkedHashMap<String, FacadeTone>(256, 0.75f, true) {

        private static final long serialVersionUID = 1L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, FacadeTone> eldest) {
            return size() > CAPACITY;
        }
    };
    private final Map<AssetId, Map<Character, Integer>> facadeCharacters = new HashMap<>();

    /**
     * Creates the view of one world.
     *
     * @param assets the loaded assets
     * @param colors the block colors
     */
    public FacadeTones(AssetSet assets, BlockColors colors) {
        this.assets = assets;
        this.colors = colors;
    }

    @Override
    public FacadeTone tone(List<String> palettes, String building) {
        StringBuilder key = new StringBuilder();
        for (String name : palettes) {
            key.append(name)
                .append(',');
        }
        key.append('|')
            .append(building);
        String k = key.toString();
        FacadeTone cached = tones.get(k);
        if (cached == null) {
            cached = compute(palettes, building);
            tones.put(k, cached == null ? UNKNOWN : cached);
        }
        return cached == UNKNOWN ? null : cached;
    }

    private FacadeTone compute(List<String> paletteNames, String buildingName) {
        Building building = buildingName == null ? null : assets.building(LayoutAssets.id(buildingName));
        if (building == null) {
            return null;
        }
        Map<Character, PaletteEntry> last = new HashMap<>();
        for (String name : paletteNames) {
            Palette palette = assets.palette(LayoutAssets.id(name));
            if (palette != null) {
                last.putAll(palette.entries());
            }
        }
        if (building.palette() != null) {
            last.putAll(
                building.palette()
                    .entries());
        }
        double l = 0;
        double a = 0;
        double b = 0;
        double weight = 0;
        for (Map.Entry<Character, Integer> facade : facadeCharacters(building).entrySet()) {
            PaletteEntry entry = follow(last.get(facade.getKey()), last);
            if (entry == null) {
                continue;
            }
            double[] mean = mean(entry);
            if (mean == null) {
                continue;
            }
            double w = facade.getValue() * mean[3];
            l += mean[0] * w;
            a += mean[1] * w;
            b += mean[2] * w;
            weight += w;
        }
        if (weight <= 0) {
            return null;
        }
        return FacadeTone.ofLab((float) (l / weight), (float) (a / weight), (float) (b / weight));
    }

    /** Follows references to the entry that places blocks, or {@code null}. */
    private static PaletteEntry follow(PaletteEntry entry, Map<Character, PaletteEntry> last) {
        PaletteEntry current = entry;
        for (int depth = 0; current != null && depth < MAX_REFERENCE_DEPTH; depth++) {
            if (current.kind() != PaletteEntry.Kind.REFERENCE) {
                return current;
            }
            current = last.get(current.reference());
        }
        return null;
    }

    /**
     * The mean CIELAB color of an entry's opaque, known blocks, and the share of its slots they fill:
     * {@code {L, a, b, share}}, or {@code null} when none is.
     */
    private double[] mean(PaletteEntry entry) {
        if (entry.kind() == PaletteEntry.Kind.BLOCK) {
            BlockColors.Entry color = opaque(entry.block());
            return color == null ? null : new double[] { color.lightness(), color.a(), color.b(), 1.0 };
        }
        RandomList random = entry.random();
        if (random == null) {
            return null;
        }
        double l = 0;
        double a = 0;
        double b = 0;
        int known = 0;
        for (int slot = 0; slot < RandomList.SLOTS; slot++) {
            BlockColors.Entry color = opaque(random.slot(slot));
            if (color != null) {
                l += color.lightness();
                a += color.a();
                b += color.b();
                known++;
            }
        }
        if (known == 0) {
            return null;
        }
        return new double[] { l / known, a / known, b / known, known / (double) RandomList.SLOTS };
    }

    private BlockColors.Entry opaque(BlockChoice choice) {
        if (choice == null || choice.isHardAir()) {
            return null;
        }
        BlockRef block = choice.block();
        if (block == null || block.isAir()) {
            return null;
        }
        BlockColors.Entry color = colors.get(block);
        return color != null && color.isOpaque() ? color : null;
    }

    /**
     * Returns a building's facade characters: those on the outer ring (x or z at a part's edge) of its parts, with how
     * often they occur there over all its parts, in character order.
     *
     * @param building the building
     * @return the counts
     */
    Map<Character, Integer> facadeCharacters(Building building) {
        Map<Character, Integer> cached = facadeCharacters.get(building.id());
        if (cached != null) {
            return cached;
        }
        Map<Character, Integer> counts = new TreeMap<>();
        for (Building.PartRef ref : building.parts()) {
            Part part = ref.part();
            if (part == null) {
                continue;
            }
            int xs = part.xsize();
            int zs = part.zsize();
            for (int x = 0; x < xs; x++) {
                for (int z = 0; z < zs; z++) {
                    if (x != 0 && z != 0 && x != xs - 1 && z != zs - 1) {
                        continue;
                    }
                    for (int y = 0; y < part.height(); y++) {
                        counts.merge(part.get(x, y, z), 1, Integer::sum);
                    }
                }
            }
        }
        facadeCharacters.put(building.id(), counts);
        return counts;
    }
}
