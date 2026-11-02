package com.tuxprogrammer.lostcities.scenario;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import com.tuxprogrammer.lostcities.core.selfcheck.SelfCheck;

/**
 * The registered scenarios, by name, and how a selector picks among them.
 *
 * <p>
 * A selector is {@code all} (every scenario except the manual-only ones), an exact name, or a prefix of whole words
 * ({@code harness} selects {@code harness.trivial} and {@code harness.fresh_chunk}, but not manual-only ones). Runs go
 * in name order, so a run's order never depends on registration order.
 *
 * <p>
 * The registry is a registry in CONTRIBUTING's sense, filled once at startup and never changed by a world. Side:
 * server.
 */
public final class ScenarioRegistry {

    /** A valid scenario name: two or more lowercase words separated by dots. */
    public static final Pattern NAME = Pattern.compile("[a-z0-9_]+(\\.[a-z0-9_]+)+");

    /** The selector for every scenario that is not manual-only. */
    public static final String ALL = "all";

    /** The one registry of the game. */
    public static final ScenarioRegistry INSTANCE = new ScenarioRegistry();

    private final Map<String, Scenario> byName = new TreeMap<>();

    /** Creates an empty registry; the game uses {@link #INSTANCE}, tests their own. */
    public ScenarioRegistry() {}

    /**
     * Adds a scenario.
     *
     * @param scenario the scenario
     * @throws IllegalArgumentException when its name is malformed or already taken
     */
    public synchronized void register(Scenario scenario) {
        String name = scenario.name();
        if (name == null || !NAME.matcher(name)
            .matches()) {
            throw new IllegalArgumentException("scenario name '" + name + "' is not lowercase.dotted.words");
        }
        if (byName.containsKey(name)) {
            throw new IllegalArgumentException("scenario name '" + name + "' is registered twice");
        }
        byName.put(name, scenario);
    }

    /** Returns every registered scenario, in name order. */
    public synchronized List<Scenario> all() {
        return Collections.unmodifiableList(new ArrayList<>(byName.values()));
    }

    /**
     * Returns the scenarios a selector picks, in name order.
     *
     * @param selector {@code all}, an exact name, or a prefix of whole dot-separated words
     * @return the matching scenarios; empty when none matches
     */
    public synchronized List<Scenario> select(String selector) {
        List<Scenario> out = new ArrayList<>();
        if (selector == null || selector.isEmpty()) {
            return out;
        }
        Scenario exact = byName.get(selector);
        if (exact != null) {
            out.add(exact);
            return out;
        }
        String prefix = selector + ".";
        for (Scenario scenario : byName.values()) {
            if (scenario.manualOnly()) {
                continue;
            }
            if (ALL.equals(selector) || scenario.name()
                .startsWith(prefix)) {
                out.add(scenario);
            }
        }
        return out;
    }

    /**
     * Returns the dev self-check's view of the game's registry: it must hold at least one scenario that {@code all}
     * runs. Names are already validated by {@link #register}.
     */
    public static SelfCheck selfCheck() {
        return new SelfCheck() {

            @Override
            public String name() {
                return "scenarios";
            }

            @Override
            public List<String> problems() {
                List<String> problems = new ArrayList<>();
                if (INSTANCE.select(ALL)
                    .isEmpty()) {
                    problems.add("no scenario is registered for 'all'");
                }
                return problems;
            }
        };
    }
}
