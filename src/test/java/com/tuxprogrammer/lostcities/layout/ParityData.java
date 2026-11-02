package com.tuxprogrammer.lostcities.layout;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.tuxprogrammer.lostcities.statics.ProjectFiles;

/**
 * Reads the reference values recorded from upstream Lost Cities 1.20 and Minecraft 1.20.1 by
 * {@code tools/upstream-parity/} (decision log D-M03-1), in {@code src/test/resources/parity/}. Each file is a few
 * {@code #} comment lines, then one record per line with space-separated fields.
 */
public final class ParityData {

    private ParityData() {}

    /**
     * Returns the records of a parity file, each split on spaces.
     *
     * @param name the file name, for example {@code quality_random.txt}
     * @return the records, at least one (a file without records fails)
     */
    public static List<String[]> records(String name) throws IOException {
        Path file = ProjectFiles.resolve("src/test/resources/parity/" + name);
        List<String[]> out = new ArrayList<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            out.add(line.split(" "));
        }
        if (out.isEmpty()) {
            throw new IllegalStateException("parity file " + name + " holds no records");
        }
        return out;
    }

    /**
     * Returns the record lines of a parity file, unsplit.
     *
     * @param name the file name
     * @return the lines that are not comments
     */
    public static List<String> lines(String name) throws IOException {
        Path file = ProjectFiles.resolve("src/test/resources/parity/" + name);
        List<String> out = new ArrayList<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (!line.isEmpty() && !line.startsWith("#")) {
                out.add(line);
            }
        }
        return out;
    }

    /** Parses a float from the recorder's hexadecimal bit pattern. */
    public static float floatBits(String hex) {
        return Float.intBitsToFloat((int) Long.parseLong(hex, 16));
    }

    /** Parses a double from the recorder's hexadecimal bit pattern. */
    public static double doubleBits(String hex) {
        return Double.longBitsToDouble(Long.parseUnsignedLong(hex, 16));
    }
}
