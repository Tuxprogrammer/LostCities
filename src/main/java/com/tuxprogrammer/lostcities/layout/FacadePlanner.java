package com.tuxprogrammer.lostcities.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tuxprogrammer.lostcities.layout.random.QualityRandom;
import com.tuxprogrammer.lostcities.layout.random.StableHash;
import com.tuxprogrammer.lostcities.layout.view.CityStyleView;
import com.tuxprogrammer.lostcities.layout.view.FacadeTone;
import com.tuxprogrammer.lostcities.layout.view.FacadeView;
import com.tuxprogrammer.lostcities.layout.view.StyleView;

/**
 * The facade stage of the layout: the palettes of every building unit (a single building chunk or a whole
 * multibuilding), chosen so that neighboring buildings' facades stay within {@code facadeContrast} of each other
 * across style and biome changes (alpha review AR-17, docs/design/0007-facade-color-feathering.md §4).
 *
 * <ul>
 * <li>Every unit that could hold a building has a priority: units whose facade the palettes cannot change first, then
 * a hash of the seed and the unit, then its coordinates; a strict total order.</li>
 * <li>A unit draws one float per palette group from its own facade random; each group's default is the weighted pick
 * with that float.</li>
 * <li>Its constraints are the chosen tones of the neighboring units with a higher priority, which this stage asks for
 * recursively; each step goes to a strictly higher priority, so the recursion ends.</li>
 * <li>Per group, the default stays when it is within the limit of every constraint; otherwise the same float picks
 * among the candidates that are, by their weights, or, when none is, the candidate with the smallest worst
 * contrast.</li>
 * </ul>
 * A unit's palettes are a pure function of the seed, its coordinates, the characteristics around it and the assets,
 * whatever order chunks are planned in.
 *
 * <p>
 * Deviation from upstream's step 12.13 (docs/new-spec/04-city-layout/index.md). Not thread-safe; one per planner.
 * Side: both (plain Java).
 */
final class FacadePlanner {

    /** Salt of a unit's facade random. */
    static final long FACADE_SALT = 0x46414341_44455321L;
    /** Salt of a unit's priority hash. */
    static final long PRIORITY_SALT = 0x5052494f_52495459L;
    /**
     * Most contrast any palette choice can move a unit's facade by and still count as fixed: a building whose own palette
     * sets its main wall moves only by its accents, so it chooses first and its neighbors adapt to it.
     */
    static final float SAME_TONE = 12f;
    /** Deepest recursion followed; beyond it a neighbor imposes nothing (never reached in practice). */
    static final int MAX_DEPTH = 256;

    /** A unit's chosen palettes and the tone they give its facade. */
    static final class Choice {

        final List<String> palettes;
        final FacadeTone tone;

        Choice(List<String> palettes, FacadeTone tone) {
            this.palettes = palettes;
            this.tone = tone;
        }
    }

    private static final Choice NONE = new Choice(Collections.<String>emptyList(), null);

    private final LayoutPlanner planner;
    private final long seed;
    private final float limit;
    private final FacadeView facades;
    private final ChunkCache<Choice> choices;
    private final Map<String, Boolean> fixed = new HashMap<>();
    private int depth;

    FacadePlanner(LayoutPlanner planner, int cacheSize) {
        this.planner = planner;
        this.seed = planner.seed;
        this.limit = planner.profile.facadeContrast;
        this.facades = planner.assets.facades();
        this.choices = new ChunkCache<>(cacheSize);
    }

    /** Returns whether the stage changes anything: a limit under {@link FacadeTone#CONTRAST_OFF} and known colors. */
    boolean active() {
        return limit < FacadeTone.CONTRAST_OFF && facades != FacadeView.NONE;
    }

    /**
     * Returns the palettes of the building unit a chunk belongs to, or {@code null} when the chunk keeps its own draw
     * (not a unit that could hold a building, a style without groups, or the stage off).
     *
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return the palette names, lowest layer first, or {@code null}
     */
    List<String> palettes(int chunkX, int chunkZ) {
        if (!active()) {
            return null;
        }
        ChunkCharacteristics c = planner.characteristics(chunkX, chunkZ);
        Choice choice = choice(topX(c), topZ(c));
        return choice == NONE ? null : choice.palettes;
    }

    /**
     * Returns the chosen tone of the unit whose top-left chunk is given, for tests.
     *
     * @param topX top-left chunk X
     * @param topZ top-left chunk Z
     * @return the tone, or {@code null}
     */
    FacadeTone tone(int topX, int topZ) {
        return choice(topX, topZ).tone;
    }

    private Choice choice(int topX, int topZ) {
        Choice cached = choices.get(topX, topZ);
        if (cached != null) {
            return cached;
        }
        return choices.put(topX, topZ, compute(topX, topZ));
    }

    /** The style and building of a unit, or {@code null} when it is not a unit. */
    private Unit unit(int topX, int topZ) {
        ChunkCharacteristics c = planner.characteristics(topX, topZ);
        if (!c.isCity || !c.couldHaveBuilding || c.buildingType == null
            || c.multiPos.isMulti() && !c.multiPos.isTopLeft()) {
            return null;
        }
        CityStyleView cityStyle = planner.cityStyle(c.cityStyle);
        if (cityStyle == null) {
            return null;
        }
        StyleView style = planner.assets.style(cityStyle.styleName());
        if (style == null || style.groups()
            .isEmpty()) {
            return null;
        }
        return new Unit(topX, topZ, c.multiPos.width(), c.multiPos.height(), cityStyle.styleName(), style,
            c.buildingType);
    }

    private Choice compute(int topX, int topZ) {
        Unit unit = unit(topX, topZ);
        if (unit == null) {
            return NONE;
        }
        List<List<StyleView.Choice>> groups = unit.style.groups();
        QualityRandom random = new QualityRandom(
            seed ^ FACADE_SALT ^ (long) topX * LayoutPlanner.BUILDING_X ^ (long) topZ * LayoutPlanner.BUILDING_Z);
        float[] floats = new float[groups.size()];
        List<String> picks = new ArrayList<>(groups.size());
        for (int g = 0; g < groups.size(); g++) {
            floats[g] = random.nextFloat();
            picks.add(
                groups.get(g)
                    .get(StyleView.weightedIndex(groups.get(g), floats[g]))
                    .palette());
        }
        List<FacadeTone> constraints = constraints(unit);
        if (!constraints.isEmpty()) {
            for (int g = 0; g < groups.size(); g++) {
                feather(unit, groups.get(g), g, floats[g], picks, constraints);
            }
        }
        List<String> chosen = Collections.unmodifiableList(picks);
        return new Choice(chosen, facades.tone(chosen, unit.building));
    }

    /** Re-picks one group against the constraints, keeping the default when it fits. */
    private void feather(Unit unit, List<StyleView.Choice> group, int g, float u, List<String> picks,
        List<FacadeTone> constraints) {
        if (group.size() < 2) {
            return;
        }
        float[] worst = new float[group.size()];
        boolean anyKnown = false;
        for (int i = 0; i < group.size(); i++) {
            picks.set(
                g,
                group.get(i)
                    .palette());
            FacadeTone tone = facades.tone(picks, unit.building);
            worst[i] = worstContrast(tone, constraints);
            anyKnown |= tone != null;
        }
        int defaultIndex = StyleView.weightedIndex(group, u);
        if (!anyKnown || worst[defaultIndex] <= limit) {
            picks.set(
                g,
                group.get(defaultIndex)
                    .palette());
            return;
        }
        List<StyleView.Choice> eligible = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        int best = defaultIndex;
        for (int i = 0; i < group.size(); i++) {
            if (worst[i] <= limit && group.get(i)
                .weight() > 0) {
                eligible.add(group.get(i));
                indices.add(i);
            }
            if (worst[i] < worst[best] && group.get(i)
                .weight() > 0) {
                best = i;
            }
        }
        int pick = eligible.isEmpty() ? best : indices.get(StyleView.weightedIndex(eligible, u));
        picks.set(
            g,
            group.get(pick)
                .palette());
    }

    private static float worstContrast(FacadeTone tone, List<FacadeTone> constraints) {
        if (tone == null) {
            return 0f;
        }
        float worst = 0f;
        for (FacadeTone other : constraints) {
            worst = Math.max(worst, FacadeTone.contrast(tone, other));
        }
        return worst;
    }

    /** The chosen tones of the neighboring units with a higher priority. */
    private List<FacadeTone> constraints(Unit unit) {
        List<FacadeTone> out = new ArrayList<>();
        if (depth >= MAX_DEPTH) {
            return out;
        }
        long own = priority(unit);
        List<Long> seen = new ArrayList<>();
        for (int x = unit.topX - 1; x <= unit.topX + unit.width; x++) {
            for (int z = unit.topZ - 1; z <= unit.topZ + unit.height; z++) {
                boolean inside = x >= unit.topX && x < unit.topX + unit.width
                    && z >= unit.topZ
                    && z < unit.topZ + unit.height;
                if (inside) {
                    continue;
                }
                ChunkCharacteristics c = planner.characteristics(x, z);
                int nx = topX(c);
                int nz = topZ(c);
                long key = ChunkCache.key(nx, nz);
                if (seen.contains(key)) {
                    continue;
                }
                seen.add(key);
                Unit neighbor = unit(nx, nz);
                if (neighbor == null || !higher(neighbor, own, unit)) {
                    continue;
                }
                depth++;
                try {
                    FacadeTone tone = choice(nx, nz).tone;
                    if (tone != null) {
                        out.add(tone);
                    }
                } finally {
                    depth--;
                }
            }
        }
        return out;
    }

    /** The top-left chunk X of the unit a chunk belongs to (the chunk itself when it is single). */
    private static int topX(ChunkCharacteristics c) {
        return c.multiPos.isSingle() ? c.chunkX : c.chunkX - c.multiPos.x();
    }

    /** The top-left chunk Z of the unit a chunk belongs to. */
    private static int topZ(ChunkCharacteristics c) {
        return c.multiPos.isSingle() ? c.chunkZ : c.chunkZ - c.multiPos.z();
    }

    /** Whether a neighbor ranks above a unit whose priority is {@code own}. */
    private boolean higher(Unit neighbor, long own, Unit unit) {
        long theirs = priority(neighbor);
        if (theirs != own) {
            return theirs > own;
        }
        if (neighbor.topX != unit.topX) {
            return neighbor.topX > unit.topX;
        }
        return neighbor.topZ > unit.topZ;
    }

    /**
     * A unit's priority: a fixed-facade unit's top bit set, then a hash of the seed and its top-left chunk. Higher
     * chooses first.
     */
    private long priority(Unit unit) {
        long hash = StableHash.mix64(seed ^ PRIORITY_SALT ^ ChunkCache.key(unit.topX, unit.topZ)) >>> 1;
        return isFixed(unit) ? hash | Long.MIN_VALUE >>> 1 : hash >>> 1;
    }

    /** Whether no palette choice changes a unit's facade tone: its building's own palette fixes its walls. */
    private boolean isFixed(Unit unit) {
        String key = unit.styleName + "|" + unit.building;
        Boolean cached = fixed.get(key);
        if (cached != null) {
            return cached;
        }
        boolean result = computeFixed(unit);
        fixed.put(key, result);
        return result;
    }

    private boolean computeFixed(Unit unit) {
        List<List<StyleView.Choice>> groups = unit.style.groups();
        List<String> picks = new ArrayList<>();
        for (List<StyleView.Choice> group : groups) {
            picks.add(
                group.get(0)
                    .palette());
        }
        FacadeTone base = facades.tone(picks, unit.building);
        for (int g = 0; g < groups.size(); g++) {
            List<StyleView.Choice> group = groups.get(g);
            for (int i = 1; i < group.size(); i++) {
                picks.set(
                    g,
                    group.get(i)
                        .palette());
                FacadeTone tone = facades.tone(picks, unit.building);
                if (base == null ? tone != null : tone == null || FacadeTone.contrast(base, tone) > SAME_TONE) {
                    return false;
                }
            }
            picks.set(
                g,
                group.get(0)
                    .palette());
        }
        return true;
    }

    /** A building unit: its top-left chunk, footprint, style and building. */
    private static final class Unit {

        final int topX;
        final int topZ;
        final int width;
        final int height;
        final String styleName;
        final StyleView style;
        final String building;

        Unit(int topX, int topZ, int width, int height, String styleName, StyleView style, String building) {
            this.topX = topX;
            this.topZ = topZ;
            this.width = width;
            this.height = height;
            this.styleName = styleName;
            this.style = style;
            this.building = building;
        }
    }
}
