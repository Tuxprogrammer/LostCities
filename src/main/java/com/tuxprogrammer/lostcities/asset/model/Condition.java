package com.tuxprogrammer.lostcities.asset.model;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/cityassets/Condition.java
// Avoids: ASSET-29, ASSET-10

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.json.JsonFields;
import com.tuxprogrammer.lostcities.asset.match.WeightedPick;

/**
 * A condition asset: weighted string values, each guarded by a {@link ConditionGuard}. Palettes name conditions in
 * {@code loot} (the value is a loot category) and {@code mob} (the value is an entity name)
 * (docs/original-spec/05-assets/conditions.md). The values are carried as written; {@code 08-loot-mobs-blocks} turns
 * upstream's 1.20 loot-table ids and entity ids into this mod's loot categories and 1.7.10 entity names (D20).
 *
 * <p>
 * A condition that matches nothing returns null, which callers handle as "empty chest" or "no spawner" with one log
 * line, never a crash (fixes ASSET-29 and LOOT-03); a zero total picks nothing (fixes ASSET-10). Immutable. Side:
 * both.
 */
public final class Condition {

    /** One guarded value. */
    public static final class Value {

        private final float factor;
        private final String value;
        private final ConditionGuard test;

        Value(float factor, String value, ConditionGuard test) {
            this.factor = factor;
            this.value = value;
            this.test = test;
        }

        /** Returns the weight relative to the other values that pass. */
        public float factor() {
            return factor;
        }

        /** Returns the value as written. */
        public String value() {
            return value;
        }

        /** Returns the guard. */
        public ConditionGuard test() {
            return test;
        }
    }

    private final AssetId id;
    private final List<Value> values;

    private Condition(AssetId id, List<Value> values) {
        this.id = id;
        this.values = Collections.unmodifiableList(values);
    }

    /**
     * Reads a condition file.
     *
     * @param root    the document's fields
     * @param context the asset being read
     * @return the condition; the caller rejects it if an error was reported
     */
    public static Condition parse(JsonFields root, ParseContext context) {
        List<Value> values = new ArrayList<>();
        for (JsonFields entry : root.objects("values", true)) {
            Float factor = entry.number("factor", true);
            String value = entry.string("value", true);
            ConditionGuard test = ConditionGuard.parse(entry, context);
            if (factor != null && value != null) {
                if (factor < 0) {
                    entry.error("factor", Float.toString(factor), "must not be negative");
                }
                values.add(new Value(factor, value, test));
            }
        }
        return new Condition(context.id(), values);
    }

    /** Returns the id. */
    public AssetId id() {
        return id;
    }

    /** Returns the values, in file order. */
    public List<Value> values() {
        return values;
    }

    /**
     * Picks a value: keeps the values whose test holds, in file order, and draws one by weight.
     *
     * @param random  the caller's random
     * @param context the facts
     * @return the value, or null when none holds or every weight is zero
     */
    public String randomValue(Random random, ConditionContext context) {
        return randomValue(random, context, value -> true);
    }

    /**
     * Picks a value among those a caller allows: keeps the values whose test holds and that {@code allowed} accepts,
     * in file order, and draws one by weight. The loot glue uses it to leave out values with no 1.7.10 counterpart
     * before the draw, so they never take a chest's share (docs/new-spec/08-loot-mobs-blocks/index.md).
     *
     * @param random  the caller's random
     * @param context the facts
     * @param allowed which values may be drawn
     * @return the value, or null when none holds or every weight is zero
     */
    public String randomValue(Random random, ConditionContext context, Predicate<String> allowed) {
        List<Value> passing = new ArrayList<>();
        for (Value value : values) {
            if (allowed.test(value.value) && value.test.test(context)) {
                passing.add(value);
            }
        }
        if (passing.isEmpty()) {
            return null;
        }
        Value picked = WeightedPick.pick(random, passing, Value::factor);
        return picked == null ? null : picked.value;
    }
}
