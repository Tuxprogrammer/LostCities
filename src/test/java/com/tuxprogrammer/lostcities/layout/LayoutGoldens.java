package com.tuxprogrammer.lostcities.layout;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.tuxprogrammer.lostcities.statics.ProjectFiles;

/**
 * Layout goldens (decision log D-M03-3): {@code src/test/resources/golden/layout/<case>.txt} holds the case's
 * description, the legend, the three maps of {@link LayoutText#maps} in full, so a change can be read as a diff, and a
 * {@code details-sha256:} line pinning every chunk's full description ({@link LayoutText#details}). On a mismatch the
 * actual file and the details are written to {@code build/golden-actual/layout/}; {@code ./gradlew test
 * -PupdateGoldens} rewrites the goldens, as for chunk goldens (docs/testing/README.md).
 */
final class LayoutGoldens {

    /** The system property the build sets from {@code -PupdateGoldens}. */
    static final String UPDATE_PROPERTY = "lostcitiesnh.updateGoldens";

    private LayoutGoldens() {}

    /**
     * Fails unless a layout matches its golden.
     *
     * @param caseName    the golden's name
     * @param description what the case is, one or more lines (written as comments)
     * @param maps        the maps text
     * @param details     the details text
     */
    static void assertMatches(String caseName, String description, String maps, String details) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String line : description.split("\n")) {
            sb.append("# ")
                .append(line)
                .append('\n');
        }
        sb.append(LayoutText.LEGEND)
            .append('\n')
            .append(maps)
            .append("details-sha256: ")
            .append(sha256(details))
            .append('\n');
        String actual = sb.toString();
        Path golden = ProjectFiles.resolve("src/test/resources/golden/layout/" + caseName + ".txt");
        if (Boolean.getBoolean(UPDATE_PROPERTY)) {
            Files.createDirectories(golden.getParent());
            Files.write(golden, actual.getBytes(StandardCharsets.UTF_8));
            writeActual(caseName, actual, details);
            return;
        }
        if (!Files.isRegularFile(golden)) {
            Path written = writeActual(caseName, actual, details);
            fail(
                "no layout golden " + ProjectFiles.relative(golden)
                    + "; the actual text is in "
                    + written
                    + "; create it with ./gradlew test -PupdateGoldens");
        }
        String expected = new String(Files.readAllBytes(golden), StandardCharsets.UTF_8).replace("\r\n", "\n");
        if (!expected.equals(actual)) {
            Path written = writeActual(caseName, actual, details);
            fail("layout golden " + caseName + " differs; the actual text and details are in " + written.getParent());
        }
    }

    private static Path writeActual(String caseName, String actual, String details) throws IOException {
        Path dir = ProjectFiles.resolve("build/golden-actual/layout");
        Files.createDirectories(dir);
        Files.write(dir.resolve(caseName + ".details.txt"), details.getBytes(StandardCharsets.UTF_8));
        Path file = dir.resolve(caseName + ".txt");
        Files.write(file, actual.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    static String sha256(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
