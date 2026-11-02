package com.tuxprogrammer.lostcities.core.selfcheck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.config.WorldgenConfig;
import com.tuxprogrammer.lostcities.scenario.BuiltinScenarios;
import com.tuxprogrammer.lostcities.scenario.ScenarioRegistry;

/** The dev self-check's checks, on the real mod and on broken inputs. */
public class SelfCheckTest {

    @Test
    public void realChecksPassOnceScenariosAreRegistered() {
        if (ScenarioRegistry.INSTANCE.all()
            .isEmpty()) {
            BuiltinScenarios.registerAll(ScenarioRegistry.INSTANCE);
        }
        assertEquals(Collections.emptyList(), SelfCheck.runAll(DevSelfCheck.checks()));
    }

    @Test
    public void missingLangKeyIsReported() {
        LangCheck check = new LangCheck(
            Arrays.asList("lostcitiesnh.a.b", "lostcitiesnh.a.c"),
            new HashSet<>(Collections.singletonList("lostcitiesnh.a.b")));
        assertEquals(Collections.singletonList("missing from en_US.lang: lostcitiesnh.a.c"), check.problems());
    }

    @Test
    public void moduleWithoutConfigOptionIsReported() {
        // A config class with none of the module options: every module is reported missing.
        List<String> problems = new ModuleConfigCheck(Empty.class).problems();
        assertEquals(4, problems.size(), "problems: " + problems);
        assertTrue(
            problems.get(0)
                .startsWith("module "),
            problems.get(0));
    }

    @Test
    public void realModuleConfigIsConsistent() {
        assertEquals(Collections.emptyList(), new ModuleConfigCheck(WorldgenConfig.class).problems());
    }

    @Test
    public void aThrowingCheckIsReportedNotPropagated() {
        SelfCheck broken = new SelfCheck() {

            @Override
            public String name() {
                return "broken";
            }

            @Override
            public List<String> problems() {
                throw new IllegalStateException("boom");
            }
        };
        List<String> problems = SelfCheck.runAll(Collections.singletonList(broken));
        assertEquals(1, problems.size());
        assertTrue(
            problems.get(0)
                .startsWith("broken: the check itself failed"),
            problems.get(0));
    }

    @Test
    public void failureCarriesEveryProblem() {
        SelfCheckFailedException e = new SelfCheckFailedException(Arrays.asList("a: one", "b: two"));
        assertEquals(Arrays.asList("a: one", "b: two"), e.problems());
        assertTrue(
            e.getMessage()
                .contains("a: one\nb: two"),
            e.getMessage());
    }

    /** A config class with no options. */
    static final class Empty {
    }
}
