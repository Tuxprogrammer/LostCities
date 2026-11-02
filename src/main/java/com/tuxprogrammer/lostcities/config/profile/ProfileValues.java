package com.tuxprogrammer.lostcities.config.profile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

/**
 * A profile's resolved values, by option key, as canonical text ({@link ProfileOption}), with typed getters. A key the
 * map lacks (a snapshot pinned by an older version, or an option added since) reads as the option's default, so every
 * consumer sees a complete profile. Read options through here, never by parsing a snapshot's map at a use site.
 *
 * <p>
 * Immutable. Side: both (plain Java).
 */
public final class ProfileValues {

    private static final ProfileValues DEFAULTS = new ProfileValues(ProfileOptions.defaults());

    private final Map<String, String> values;

    private ProfileValues(Map<String, String> values) {
        this.values = Collections.unmodifiableMap(new TreeMap<>(values));
    }

    /**
     * Wraps values.
     *
     * @param values option key to canonical text; missing keys read as defaults
     * @return the values
     */
    public static ProfileValues of(Map<String, String> values) {
        return new ProfileValues(values);
    }

    /** Returns the {@code default} profile's values: every option at its default. */
    public static ProfileValues defaults() {
        return DEFAULTS;
    }

    /** Returns the values held, sorted by key; keys not held read as their defaults. */
    public Map<String, String> asMap() {
        return values;
    }

    /**
     * Returns an option's text: the held value, else the option's default.
     *
     * @param key the option's key
     * @return the text
     * @throws IllegalArgumentException for a key no option has
     */
    public String text(String key) {
        String value = values.get(key);
        if (value != null) {
            return value;
        }
        return option(key).defaultValue();
    }

    /**
     * Returns a boolean option.
     *
     * @param key the key
     * @return the value; the default when the held text is not a boolean
     */
    public boolean getBoolean(String key) {
        String text = text(key);
        if ("true".equalsIgnoreCase(text) || "false".equalsIgnoreCase(text)) {
            return Boolean.parseBoolean(text);
        }
        return Boolean.parseBoolean(option(key).defaultValue());
    }

    /**
     * Returns an int option.
     *
     * @param key the key
     * @return the value; the default when the held text is not a number
     */
    public int getInt(String key) {
        try {
            return (int) Double.parseDouble(text(key));
        } catch (NumberFormatException e) {
            return (int) Double.parseDouble(option(key).defaultValue());
        }
    }

    /**
     * Returns a float option.
     *
     * @param key the key
     * @return the value; the default when the held text is not a number
     */
    public float getFloat(String key) {
        try {
            return Float.parseFloat(text(key));
        } catch (NumberFormatException e) {
            return Float.parseFloat(option(key).defaultValue());
        }
    }

    /**
     * Returns a double option.
     *
     * @param key the key
     * @return the value; the default when the held text is not a number
     */
    public double getDouble(String key) {
        try {
            return Double.parseDouble(text(key));
        } catch (NumberFormatException e) {
            return Double.parseDouble(option(key).defaultValue());
        }
    }

    /**
     * Returns a string option.
     *
     * @param key the key
     * @return the value
     */
    public String getString(String key) {
        return text(key);
    }

    /**
     * Returns a string-list option.
     *
     * @param key the key
     * @return the values, in order; empty when the held text is not a list
     */
    public List<String> getStringList(String key) {
        return parseList(text(key));
    }

    /**
     * Returns the canonical text of a string list: a compact JSON array.
     *
     * @param list the strings
     * @return the text, {@code []} for none
     */
    public static String listText(List<String> list) {
        JsonArray array = new JsonArray();
        for (String value : list) {
            array.add(new JsonPrimitive(value));
        }
        return array.toString();
    }

    /**
     * Parses the canonical text of a string list.
     *
     * @param text the text
     * @return the strings; empty when the text is not a JSON array of primitives
     */
    public static List<String> parseList(String text) {
        List<String> out = new ArrayList<>();
        try {
            JsonElement element = new JsonParser().parse(text);
            if (!element.isJsonArray()) {
                return out;
            }
            for (JsonElement item : element.getAsJsonArray()) {
                if (item.isJsonPrimitive()) {
                    out.add(item.getAsString());
                }
            }
        } catch (JsonParseException | IllegalStateException e) {
            out.clear();
        }
        return out;
    }

    private static ProfileOption option(String key) {
        ProfileOption option = ProfileOptions.get(key);
        if (option == null) {
            throw new IllegalArgumentException("no profile option '" + key + "'");
        }
        return option;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ProfileValues && values.equals(((ProfileValues) other).values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }

    @Override
    public String toString() {
        return values.toString();
    }
}
