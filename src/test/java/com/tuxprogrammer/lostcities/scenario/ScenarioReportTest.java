package com.tuxprogrammer.lostcities.scenario;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The report lines automation greps for. */
public class ScenarioReportTest {

    @Test
    public void resultLinesAreOneLineWithADefaultMessage() {
        assertEquals("[LCNH-SCENARIO] PASS harness.trivial 12 ok", ScenarioReport.resultLine(true, "harness.trivial", 12, ""));
        assertEquals(
            "[LCNH-SCENARIO] FAIL harness.must_fail 0 expected 3, got 2",
            ScenarioReport.resultLine(false, "harness.must_fail", 0, "expected 3,\n  got 2"));
        assertEquals("[LCNH-SCENARIO] FAIL a.b 1 failed", ScenarioReport.resultLine(false, "a.b", 1, null));
    }

    @Test
    public void summaryAndErrorLines() {
        assertEquals("[LCNH-SCENARIO] Scenarios: 2 passed, 1 failed", ScenarioReport.summaryLine(2, 1));
        assertEquals("[LCNH-SCENARIO] ERROR no scenario matches 'x'", ScenarioReport.errorLine("no scenario matches 'x'"));
        assertEquals("[LCNH-SCENARIO] ERROR error", ScenarioReport.errorLine(" "));
    }
}
