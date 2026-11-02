package com.tuxprogrammer.lostcities.scenario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Scenario names, selectors and the built-in set. */
public class ScenarioRegistryTest {

    @Test
    public void selectorsPickByAllExactNameAndWholeWordPrefix() {
        ScenarioRegistry registry = new ScenarioRegistry();
        registry.register(named("harness.trivial", false));
        registry.register(named("harness.must_fail", true));
        registry.register(named("harnessing.other", false));
        registry.register(named("city.spawn_distance", false));

        assertEquals(Arrays.asList("city.spawn_distance", "harness.trivial", "harnessing.other"), names(registry.select("all")));
        assertEquals(Arrays.asList("harness.trivial"), names(registry.select("harness")));
        assertEquals(Arrays.asList("harness.must_fail"), names(registry.select("harness.must_fail")));
        assertTrue(registry.select("harn").isEmpty(), "a prefix must be whole words");
        assertTrue(registry.select("").isEmpty());
        assertTrue(registry.select(null).isEmpty());
    }

    @Test
    public void malformedAndDuplicateNamesAreRefused() {
        ScenarioRegistry registry = new ScenarioRegistry();
        assertThrows(IllegalArgumentException.class, () -> registry.register(named("trivial", false)));
        assertThrows(IllegalArgumentException.class, () -> registry.register(named("Harness.Trivial", false)));
        assertThrows(IllegalArgumentException.class, () -> registry.register(named("harness trivial", false)));
        registry.register(named("harness.trivial", false));
        assertThrows(IllegalArgumentException.class, () -> registry.register(named("harness.trivial", false)));
    }

    @Test
    public void builtInsRegisterAndAllSkipsTheDeliberateFailure() {
        ScenarioRegistry registry = new ScenarioRegistry();
        BuiltinScenarios.registerAll(registry);
        assertEquals(Arrays.asList("harness.fresh_chunk", "harness.must_fail", "harness.trivial"), names(registry.all()));
        assertEquals(Arrays.asList("harness.fresh_chunk", "harness.trivial"), names(registry.select(ScenarioRegistry.ALL)));
    }

    private static List<String> names(List<Scenario> scenarios) {
        List<String> out = new ArrayList<>();
        for (Scenario scenario : scenarios) {
            out.add(scenario.name());
        }
        return out;
    }

    private static Scenario named(String name, boolean manualOnly) {
        return new Scenario() {

            @Override
            public String name() {
                return name;
            }

            @Override
            public boolean manualOnly() {
                return manualOnly;
            }

            @Override
            public void setup(ScenarioContext context) {}

            @Override
            public void verify(ScenarioContext context) {}
        };
    }
}
