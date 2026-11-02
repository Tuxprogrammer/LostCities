package com.tuxprogrammer.lostcities.statics;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The automatic part of the ported-code gate (CONTRIBUTING § Ported code), as plain functions so
 * {@code PortedCodeGateTest} runs them on the real tree and on fixtures alike.
 *
 * <p>
 * A file is ported when it carries {@code // Ported from Lost Cities (MIT): <permalink>}. For each ported file the gate
 * reports a violation, tagged with its rule, when:
 * <ul>
 * <li>{@link #PLACEMENT}: the header is not the first line after the package declaration (its link may stand on the
 * next {@code //} line, where Spotless wraps it);</li>
 * <li>{@link #PERMALINK}: the link is not {@code github.com/McJtyMods/LostCities/blob/<commit>/...}, or the commit is
 * neither the baseline {@code f18a094d} nor another commit named in THIRD-PARTY-NOTICES.md (an older MIT branch such
 * as {@code 1.12} must be cited there first);</li>
 * <li>{@link #NOTICE}: THIRD-PARTY-NOTICES.md lacks the Lost Cities entry (heading, baseline commit, McJty's copyright
 * line, the MIT text), or LICENSE lacks McJty's copyright line;</li>
 * <li>{@link #JAVADOC}: the top-level type has no Javadoc;</li>
 * <li>{@link #TEST_CLASS}: no {@code <Name>Test.java} with a {@code @Test} exists at the mirrored test path;</li>
 * <li>{@link #BUG_ID}: an {@code Avoids:} ID is not a row of docs/original-spec/BUGS.md;</li>
 * <li>{@link #AVOIDS_EMPTY}: an {@code Avoids:} line names no ID;</li>
 * <li>{@link #AVOIDS_UNTESTED}: the test class has no {@code doesNotReproduce<ID>} test for an avoided bug, the ID
 * written without its dash ({@code doesNotReproduceCITY04_...}, CONTRIBUTING § Testing).</li>
 * </ul>
 * So that the gate never passes because it looked at nothing, {@link #ROOT} fails a missing or near-empty source tree,
 * and {@link #UNMARKED} fails any source file that names upstream's code ({@code mcjty}) without the header.
 */
final class PortedCodeGate {

    static final String PLACEMENT = "placement";
    static final String PERMALINK = "permalink";
    static final String NOTICE = "notice";
    static final String JAVADOC = "javadoc";
    static final String TEST_CLASS = "test-class";
    static final String BUG_ID = "bug-id";
    static final String AVOIDS_EMPTY = "avoids-empty";
    static final String AVOIDS_UNTESTED = "avoids-untested";
    static final String ROOT = "root";
    static final String UNMARKED = "unmarked";

    /** The baseline commit every port starts from (0001), short and full. */
    static final String BASELINE_SHORT = "f18a094d";
    static final String BASELINE_FULL = "f18a094d710a0957ead20dc24f0bf1365b885802";

    private static final Pattern HEADER = Pattern
        .compile("^\\s*//\\s*Ported from Lost Cities \\(MIT\\):(.*)$", Pattern.MULTILINE);
    private static final Pattern PERMALINK_URL = Pattern
        .compile("https://github\\.com/McJtyMods/LostCities/blob/([0-9a-fA-F]{7,40})/\\S+");
    private static final Pattern AVOIDS = Pattern.compile("^\\s*//\\s*Avoids:(.*)$", Pattern.MULTILINE);
    /** A {@code //} line holding a wrapped header's link. */
    private static final Pattern CONTINUATION = Pattern.compile("^\\s*//\\s*(https://\\S+)\\s*$");
    /** A {@code //} line holding only further bug IDs of a wrapped {@code Avoids:} line. */
    private static final Pattern ID_CONTINUATION = Pattern
        .compile("^\\s*//\\s*[A-Z]+-[0-9]+(\\s*,\\s*[A-Z]+-[0-9]+)*\\s*,?\\s*$");
    private static final Pattern BUG_TOKEN = Pattern.compile("[A-Z]+-[0-9]+");
    private static final Pattern BUG_ROW = Pattern.compile("^\\|\\s*([A-Z]+-[0-9]+)\\s*\\|", Pattern.MULTILINE);
    private static final Pattern UPSTREAM_CODE = Pattern.compile("mcjty", Pattern.CASE_INSENSITIVE);
    private static final Pattern PACKAGE = Pattern.compile("^package\\s+[\\w.]+\\s*;\\s*$");
    private static final Pattern COMMIT_IN_NOTICES = Pattern.compile("\\b[0-9a-f]{40}\\b");
    private static final Pattern TYPE_DECL = Pattern
        .compile("^(?:public |final |abstract |strictfp )*(?:class|interface|enum|@interface)\\s");

    private PortedCodeGate() {}

    /** Returns whether a source file carries the ported-code header. */
    static boolean isPorted(String source) {
        return HEADER.matcher(source)
            .find();
    }

    /** Returns the ported files under a source root, sorted by path. */
    static List<Path> portedFiles(Path mainRoot) throws IOException {
        List<Path> out = new ArrayList<>();
        for (Path file : javaFiles(mainRoot)) {
            if (isPorted(read(file))) {
                out.add(file);
            }
        }
        return out;
    }

    /** Returns a {@link #ROOT} violation when the tree is missing or holds fewer than {@code minFiles} sources. */
    static List<String> checkRoot(Path root, int minFiles) throws IOException {
        List<String> out = new ArrayList<>();
        if (!Files.isDirectory(root)) {
            out.add(ROOT + ": " + root + " is not a directory, so nothing was scanned");
        } else if (javaFiles(root).size() < minFiles) {
            out.add(ROOT + ": " + root + " holds fewer than " + minFiles + " Java files");
        }
        return out;
    }

    /** Returns an {@link #UNMARKED} violation for each file that names upstream's code but has no header. */
    static List<String> unmarkedMentions(Path mainRoot) throws IOException {
        List<String> out = new ArrayList<>();
        for (Path file : javaFiles(mainRoot)) {
            String source = read(file);
            if (!isPorted(source) && UPSTREAM_CODE.matcher(source)
                .find()) {
                out.add(
                    UNMARKED + ": " + file.getFileName() + " names upstream's code but has no 'Ported from' header");
            }
        }
        return out;
    }

    /** Returns every bug ID that has a row in the bug register. */
    static Set<String> bugIds(String bugsMarkdown) {
        Set<String> ids = new TreeSet<>();
        Matcher row = BUG_ROW.matcher(bugsMarkdown);
        while (row.find()) {
            ids.add(row.group(1));
        }
        return ids;
    }

    /** Returns the {@link #NOTICE} violations of the notices file and the license. */
    static List<String> checkNotices(String notices, String license) {
        List<String> out = new ArrayList<>();
        if (!(notices.contains("## Lost Cities") && notices.contains(BASELINE_FULL)
            && notices.contains("Copyright (c) 2017 McJty")
            && notices.contains("Permission is hereby granted, free of charge"))) {
            out.add(NOTICE + ": THIRD-PARTY-NOTICES.md has no complete '## Lost Cities' MIT entry");
        }
        if (!license.contains("Copyright (c) 2017 McJty")) {
            out.add(NOTICE + ": LICENSE lacks McJty's copyright line");
        }
        return out;
    }

    /**
     * Checks one ported file.
     *
     * @param file     the source file
     * @param mainRoot the source root it lives under
     * @param testRoot the test source root
     * @param notices  the text of THIRD-PARTY-NOTICES.md
     * @param bugIds   the IDs in the bug register
     * @return the violations, each starting with its rule tag
     */
    static List<String> check(Path file, Path mainRoot, Path testRoot, String notices, Set<String> bugIds)
        throws IOException {
        String source = read(file);
        List<String> out = new ArrayList<>();
        if (!headerDirectlyUnderPackage(source)) {
            out.add(PLACEMENT + ": the 'Ported from' header is not the line right after the package declaration");
        }
        out.addAll(checkPermalinks(source, notices));
        if (!hasTypeJavadoc(source)) {
            out.add(JAVADOC + ": the top-level type has no Javadoc");
        }
        Path test = testRoot.resolve(
            mainRoot.relativize(file)
                .toString()
                .replaceAll("\\.java$", "Test.java"));
        String testSource = Files.isRegularFile(test) ? read(test) : "";
        if (!testSource.contains("@Test")) {
            out.add(TEST_CLASS + ": no " + test.getFileName() + " with a @Test at the mirrored test path");
        }
        for (String avoidsText : avoidsTexts(source)) {
            Matcher token = BUG_TOKEN.matcher(avoidsText);
            boolean any = false;
            while (token.find()) {
                any = true;
                String id = token.group();
                if (!bugIds.contains(id)) {
                    out.add(BUG_ID + ": " + id + " is not in docs/original-spec/BUGS.md");
                }
                String testName = "doesNotReproduce" + id.replace("-", "");
                if (!testSource.contains(testName)) {
                    out.add(AVOIDS_UNTESTED + ": " + test.getFileName() + " has no " + testName + "_... test");
                }
            }
            if (!any) {
                out.add(AVOIDS_EMPTY + ": an Avoids: line names no bug ID");
            }
        }
        return out;
    }

    private static boolean headerDirectlyUnderPackage(String source) {
        String[] lines = source.split("\r?\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (PACKAGE.matcher(lines[i])
                .matches()) {
                return i + 1 < lines.length && HEADER.matcher(lines[i + 1])
                    .matches();
            }
        }
        return false;
    }

    private static List<String> checkPermalinks(String source, String notices) {
        List<String> out = new ArrayList<>();
        Set<String> allowed = new TreeSet<>();
        allowed.add(BASELINE_SHORT);
        allowed.add(BASELINE_FULL);
        Matcher commit = COMMIT_IN_NOTICES.matcher(notices);
        while (commit.find()) {
            allowed.add(commit.group());
        }
        for (String headerText : headerTexts(source)) {
            Matcher url = PERMALINK_URL.matcher(headerText);
            if (!url.find()) {
                out.add(PERMALINK + ": the header has no github.com/McJtyMods/LostCities/blob/<commit>/ link");
            } else if (!allowed.contains(
                url.group(1)
                    .toLowerCase())) {
                        out.add(
                            PERMALINK + ": pinned to "
                                + url.group(1)
                                + ", which is neither "
                                + BASELINE_SHORT
                                + " nor a commit named in THIRD-PARTY-NOTICES.md");
                    }
        }
        return out;
    }

    /**
     * Returns the text after each header's colon. Spotless wraps line comments at 120 columns, so the link of a header
     * may stand alone on the next {@code //} line; that line then counts as part of the header.
     */
    static List<String> headerTexts(String source) {
        List<String> out = new ArrayList<>();
        String[] lines = source.split("\\r?\\n", -1);
        for (int i = 0; i < lines.length; i++) {
            Matcher header = HEADER.matcher(lines[i]);
            if (header.matches()) {
                String text = header.group(1);
                if (text.trim()
                    .isEmpty() && i + 1 < lines.length) {
                    Matcher next = CONTINUATION.matcher(lines[i + 1]);
                    if (next.matches()) {
                        text = next.group(1);
                    }
                }
                out.add(text);
            }
        }
        return out;
    }

    /**
     * Returns the IDs text of each {@code Avoids:} line, with the {@code //} lines after it that hold only further IDs
     * (where Spotless wrapped a long list).
     */
    static List<String> avoidsTexts(String source) {
        List<String> out = new ArrayList<>();
        String[] lines = source.split("\\r?\\n", -1);
        for (int i = 0; i < lines.length; i++) {
            Matcher avoids = AVOIDS.matcher(lines[i]);
            if (avoids.matches()) {
                StringBuilder text = new StringBuilder(avoids.group(1));
                for (int j = i + 1; j < lines.length && ID_CONTINUATION.matcher(lines[j])
                    .matches(); j++) {
                    text.append(' ')
                        .append(
                            lines[j].trim()
                                .substring(2));
                }
                out.add(text.toString());
            }
        }
        return out;
    }

    /** Whether the first top-level type is preceded by a Javadoc comment, with only annotations or // lines between. */
    static boolean hasTypeJavadoc(String source) {
        String[] lines = source.split("\r?\n", -1);
        int decl = -1;
        for (int i = 0; i < lines.length; i++) {
            if (TYPE_DECL.matcher(lines[i])
                .find()) {
                decl = i;
                break;
            }
        }
        for (int i = decl - 1; i >= 0; i--) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("//") || line.startsWith("@")) {
                continue;
            }
            if (!line.endsWith("*/")) {
                return false;
            }
            for (int j = i; j >= 0; j--) {
                String start = lines[j].trim();
                if (start.startsWith("/**")) {
                    return true;
                }
                if (start.startsWith("/*")) {
                    return false;
                }
            }
            return false;
        }
        return false;
    }

    private static List<Path> javaFiles(Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            return new ArrayList<>();
        }
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(
                p -> p.toString()
                    .endsWith(".java"))
                .sorted()
                .collect(Collectors.toList());
        }
    }

    private static String read(Path file) throws IOException {
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }
}
