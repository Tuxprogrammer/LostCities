package com.tuxprogrammer.lostcities.statics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Runs the ported-code gate (CONTRIBUTING § Ported code) over {@code src/main/java}, and proves each rule with a
 * fixture that breaks it. Fixtures are text under {@code src/test/resources/portedgate/<case>/}, never compiled.
 */
public class PortedCodeGateTest {

    /** The fewest Java files a real main tree holds; guards against scanning the wrong folder. */
    private static final int MIN_MAIN_FILES = 20;

    private static String notices;
    private static String license;
    private static Set<String> bugIds;

    @BeforeAll
    public static void readProjectFiles() throws Exception {
        notices = ProjectFiles.read(ProjectFiles.resolve("THIRD-PARTY-NOTICES.md"));
        license = ProjectFiles.read(ProjectFiles.resolve("LICENSE"));
        bugIds = PortedCodeGate.bugIds(ProjectFiles.read(ProjectFiles.resolve("docs/original-spec/BUGS.md")));
    }

    @Test
    public void everyPortedFilePassesTheGate() throws Exception {
        Path main = ProjectFiles.resolve("src/main/java");
        Path test = ProjectFiles.resolve("src/test/java");
        List<String> violations = new ArrayList<>(PortedCodeGate.checkRoot(main, MIN_MAIN_FILES));
        violations.addAll(PortedCodeGate.unmarkedMentions(main));
        violations.addAll(PortedCodeGate.checkNotices(notices, license));
        for (Path file : PortedCodeGate.portedFiles(main)) {
            for (String violation : PortedCodeGate.check(file, main, test, notices, bugIds)) {
                violations.add(ProjectFiles.relative(file) + ": " + violation);
            }
        }
        assertTrue(violations.isEmpty(), "ported-code gate:\n" + String.join("\n", violations));
    }

    @Test
    public void bugRegisterIsRead() {
        assertTrue(bugIds.size() > 100, "expected the bug register's rows, read " + bugIds.size());
        assertTrue(bugIds.contains("CORE-04"), "CORE-04 is a row of BUGS.md");
    }

    @Test
    public void goodFixturePasses() throws Exception {
        assertEquals(Collections.emptyList(), checkFixture("good", "GoodPort"));
    }

    @Test
    public void permalinkToAnUncitedCommitFails() throws Exception {
        assertRule("badlink", "BadLink", PortedCodeGate.PERMALINK);
    }

    @Test
    public void headerBelowTheImportsFails() throws Exception {
        assertRule("misplaced", "Misplaced", PortedCodeGate.PLACEMENT);
    }

    @Test
    public void missingJavadocFails() throws Exception {
        assertRule("nojavadoc", "NoJavadoc", PortedCodeGate.JAVADOC);
    }

    @Test
    public void missingTestClassFails() throws Exception {
        assertRule("notest", "NoTest", PortedCodeGate.TEST_CLASS);
    }

    @Test
    public void unknownBugIdFails() throws Exception {
        assertRule("unknownbug", "UnknownBug", PortedCodeGate.BUG_ID);
    }

    @Test
    public void avoidedBugWithoutRegressionTestFails() throws Exception {
        assertRule("untested", "Untested", PortedCodeGate.AVOIDS_UNTESTED);
    }

    @Test
    public void emptyAvoidsLineFails() throws Exception {
        assertRule("emptyavoids", "EmptyAvoids", PortedCodeGate.AVOIDS_EMPTY);
    }

    @Test
    public void upstreamCodeWithoutHeaderFails() throws Exception {
        List<String> violations = PortedCodeGate.unmarkedMentions(fixtureRoot("unmarked").resolve("main"));
        assertEquals(1, violations.size(), "violations: " + violations);
        assertTrue(
            violations.get(0)
                .startsWith(PortedCodeGate.UNMARKED),
            violations.get(0));
    }

    @Test
    public void emptyTreeFails() throws Exception {
        List<String> violations = PortedCodeGate.checkRoot(fixtureRoot("good").resolve("main"), MIN_MAIN_FILES);
        assertEquals(1, violations.size(), "violations: " + violations);
        assertTrue(
            violations.get(0)
                .startsWith(PortedCodeGate.ROOT),
            violations.get(0));
    }

    @Test
    public void incompleteNoticeFails() {
        List<String> violations = PortedCodeGate.checkNotices(notices.replace("Copyright (c) 2017 McJty", ""), license);
        assertEquals(1, violations.size(), "violations: " + violations);
        assertTrue(
            violations.get(0)
                .startsWith(PortedCodeGate.NOTICE),
            violations.get(0));
    }

    private static void assertRule(String fixture, String className, String rule) throws Exception {
        List<String> violations = checkFixture(fixture, className);
        assertEquals(1, violations.size(), "expected exactly one " + rule + " violation, got " + violations);
        assertTrue(
            violations.get(0)
                .startsWith(rule + ":"),
            violations.get(0));
    }

    private static List<String> checkFixture(String fixture, String className) throws Exception {
        Path root = fixtureRoot(fixture);
        Path main = root.resolve("main");
        Path file = main.resolve("com/example/" + className + ".java");
        assertTrue(PortedCodeGate.isPorted(ProjectFiles.read(file)), file + " carries the header");
        return PortedCodeGate.check(file, main, root.resolve("test"), notices, bugIds);
    }

    private static Path fixtureRoot(String fixture) throws URISyntaxException {
        return Paths.get(
            PortedCodeGateTest.class.getResource("/portedgate/" + fixture)
                .toURI());
    }
}
