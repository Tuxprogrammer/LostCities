package com.tuxprogrammer.lostcities.worldgen.buffer;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import com.tuxprogrammer.lostcities.statics.ProjectFiles;

/**
 * The golden-chunk harness (CONTRIBUTING § Testing, decision log D7): compares a chunk buffer's
 * {@link ChunkBufferDigest} hash with a checked-in golden file, {@code src/test/resources/golden/<case>.sha256}.
 *
 * <p>
 * A golden file holds {@code #} comment lines saying what the case is and why its value is what it is, then the
 * 64-hex-digit hash. On a mismatch the harness writes the buffer's full text form to
 * {@code build/golden-actual/<case>.txt}, so the change can be read, and fails with both hashes.
 *
 * <p>
 * {@code ./gradlew test -PupdateGoldens} rewrites every golden it checks with the current hash instead of failing. A
 * golden changes only in a commit that records why (docs/testing/README.md).
 */
public final class GoldenChunks {

    /** System property the build sets from {@code -PupdateGoldens}. */
    public static final String UPDATE_PROPERTY = "lostcitiesnh.updateGoldens";

    private static final Pattern HASH = Pattern.compile("[0-9a-f]{64}");

    private GoldenChunks() {}

    /**
     * Fails unless the buffer matches its golden.
     *
     * @param caseName the golden's name, {@code [a-z0-9_]+}
     * @param buffer   the generated buffer
     */
    public static void assertMatches(String caseName, ChunkBuffer buffer) throws IOException {
        assertTextMatches(caseName, ChunkBufferDigest.text(buffer));
    }

    /**
     * Fails unless a text form matches its golden: for goldens of things other than a buffer, such as a chunk's
     * population jobs (0005, "Determinism tests": a second hash after our population phase).
     *
     * @param caseName the golden's name, {@code [a-z0-9_]+}
     * @param text     the text form
     */
    public static void assertTextMatches(String caseName, String text) throws IOException {
        Path golden = ProjectFiles.resolve("src/test/resources/golden/" + caseName + ".sha256");
        String actual = ChunkBufferDigest.sha256(text);
        if (Boolean.getBoolean(UPDATE_PROPERTY)) {
            writeActual(caseName, text);
            Files.write(
                golden,
                ("# Golden chunk " + caseName
                    + ": SHA-256 of the ChunkBufferDigest v1 text.\n"
                    + "# Rewritten by -PupdateGoldens; say why in the commit.\n"
                    + actual
                    + "\n").getBytes(StandardCharsets.UTF_8));
            return;
        }
        if (!Files.isRegularFile(golden)) {
            Path written = writeActual(caseName, text);
            fail(
                "no golden " + ProjectFiles.relative(golden)
                    + " (actual "
                    + actual
                    + ", text in "
                    + written
                    + "); create it with ./gradlew test -PupdateGoldens");
        }
        String expected = expectedHash(Files.readAllLines(golden, StandardCharsets.UTF_8));
        String mismatch = mismatch(expected, text);
        if (mismatch != null) {
            Path written = writeActual(caseName, text);
            fail("golden chunk " + caseName + ": " + mismatch + "; the actual text form is in " + written);
        }
    }

    /**
     * Compares a golden hash with a buffer's text form.
     *
     * @param expected the golden hash
     * @param text     the buffer's text form
     * @return {@code null} when they match, otherwise what differs
     */
    static String mismatch(String expected, String text) {
        String actual = ChunkBufferDigest.sha256(text);
        return expected.equals(actual) ? null : "expected " + expected + ", got " + actual;
    }

    /** Returns the hash line of a golden file. */
    static String expectedHash(List<String> lines) {
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            if (!HASH.matcher(trimmed)
                .matches()) {
                throw new IllegalStateException("golden line is not a SHA-256 hash: " + trimmed);
            }
            return trimmed;
        }
        throw new IllegalStateException("golden file holds no hash");
    }

    private static Path writeActual(String caseName, String text) throws IOException {
        Path dir = ProjectFiles.resolve("build/golden-actual");
        Files.createDirectories(dir);
        Path file = dir.resolve(caseName + ".txt");
        Files.write(file, text.getBytes(StandardCharsets.UTF_8));
        return file;
    }
}
