package com.tuxprogrammer.lostcities.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.TreeSet;

/**
 * Reads the keys of a Minecraft {@code .lang} file: one {@code key=value} per line, {@code #} starting a comment line,
 * as 1.7.10's {@code StringTranslate} reads it.
 *
 * <p>
 * Side: both (plain Java).
 */
public final class LangFile {

    /** Classpath location of the English lang file, the source of truth for every player-visible string. */
    public static final String EN_US = "/assets/lostcitiesnh/lang/en_US.lang";

    private LangFile() {}

    /**
     * Returns the keys a lang file defines.
     *
     * @param in the file's bytes, UTF-8; closed by this method
     * @return the keys, sorted
     * @throws IOException when the stream cannot be read
     */
    public static Set<String> keys(InputStream in) throws IOException {
        Set<String> keys = new TreeSet<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int equals = trimmed.indexOf('=');
                if (equals > 0) {
                    keys.add(
                        trimmed.substring(0, equals)
                            .trim());
                }
            }
        }
        return keys;
    }
}
