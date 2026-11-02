package com.tuxprogrammer.lostcities.config.profile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The English words of the built-in profiles' texts, read from the jar's {@code en_US.lang}, for the one place a
 * server writes a text out instead of sending it as a lang key: {@code saveprofile}, whose file an owner then edits. A
 * text that is not a key (a profile file's literal text) is returned as it is.
 *
 * <p>
 * Side: both (plain Java over a resource).
 */
public final class ProfileTexts {

    /** The lang file inside the jar. */
    public static final String LANG_RESOURCE = "/assets/lostcitiesnh/lang/en_US.lang";

    private static volatile Map<String, String> english;

    private ProfileTexts() {}

    /**
     * Returns the English words of a lang key, or the text itself when it is not a key the lang file defines.
     *
     * @param text a lang key or literal text
     * @return the words
     */
    public static String english(String text) {
        String words = lang().get(text);
        return words == null ? text : words;
    }

    private static Map<String, String> lang() {
        Map<String, String> map = english;
        if (map == null) {
            map = read();
            english = map;
        }
        return map;
    }

    private static Map<String, String> read() {
        Map<String, String> out = new HashMap<>();
        try (InputStream in = ProfileTexts.class.getResourceAsStream(LANG_RESOURCE)) {
            if (in == null) {
                return Collections.emptyMap();
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                int equals = line.indexOf('=');
                if (equals > 0 && !line.startsWith("#")) {
                    out.put(line.substring(0, equals), line.substring(equals + 1));
                }
            }
        } catch (IOException e) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(out);
    }
}
