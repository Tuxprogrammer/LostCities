package com.tuxprogrammer.lostcities.config.profile;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/config/Configuration.java
// Avoids: PROF-02, PROF-22

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.tuxprogrammer.lostcities.config.profile.ProfileOption.Category;

/**
 * Reads and writes profiles in upstream's JSON format (docs/original-spec/02-profiles-dimensions/profile-system.md,
 * "Data format"): one object with the root flag {@code public} and the five categories {@code lostcity},
 * {@code explosions}, {@code cities}, {@code cityspheres} and {@code client}, each an object of option keys. A file
 * written for upstream 1.20 loads; {@code __readonly__} markers are ignored; dropped keys are read and ignored; unknown
 * keys and keys in another category are ignored with a warning.
 *
 * <p>
 * Avoids PROF-02: malformed JSON, a value of the wrong JSON type or a failed check rejects that one profile with a
 * message naming the key, instead of throwing out of mod loading. Avoids PROF-22: the profile's name is the file name
 * minus {@code .json}, so {@code my.city.json} is {@code my.city}.
 *
 * <p>
 * Pure. Side: both (plain Java).
 */
public final class ProfileJson {

    /** A profile file's extension. */
    public static final String EXTENSION = ".json";

    /** The key upstream writes into each category of a built-in's file, ignored on read. */
    public static final String READONLY_KEY = "__readonly__";

    /** The root key of upstream's public flag. */
    public static final String PUBLIC_KEY = "public";

    /** The outcome of reading one profile. */
    public static final class Load {

        private final Profile profile;
        private final List<String> warnings;
        private final List<String> errors;
        private final List<String> ignored;

        Load(Profile profile, List<String> warnings, List<String> errors, List<String> ignored) {
            this.profile = profile;
            this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
            this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
            this.ignored = Collections.unmodifiableList(new ArrayList<>(ignored));
        }

        /** Returns the profile, or {@code null} when it was rejected. */
        public Profile profile() {
            return profile;
        }

        /** Returns what was fixed or ignored. */
        public List<String> warnings() {
            return warnings;
        }

        /** Returns why the profile was rejected; empty when it loaded. */
        public List<String> errors() {
            return errors;
        }

        /** Returns the dropped keys the file set, read and ignored. */
        public List<String> ignored() {
            return ignored;
        }
    }

    private ProfileJson() {}

    /**
     * Returns a profile's name from its file name: the name minus the {@code .json} extension (PROF-22).
     *
     * @param fileName the file name
     * @return the profile name, or {@code null} when the file is not a profile file
     */
    public static String nameOf(String fileName) {
        if (!fileName.endsWith(EXTENSION) || fileName.length() == EXTENSION.length()) {
            return null;
        }
        return fileName.substring(0, fileName.length() - EXTENSION.length());
    }

    /**
     * Reads a profile.
     *
     * @param name   the profile's name
     * @param source where it came from, for {@link Profile#source()}
     * @param text   the JSON text
     * @return the outcome
     */
    public static Load read(String name, String source, String text) {
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        JsonElement root;
        try {
            root = new JsonParser().parse(text);
        } catch (JsonParseException e) {
            errors.add("not valid JSON: " + e.getMessage());
            return new Load(null, warnings, errors, Collections.<String>emptyList());
        }
        if (root == null || !root.isJsonObject()) {
            errors.add("not a JSON object");
            return new Load(null, warnings, errors, Collections.<String>emptyList());
        }
        boolean isPublic = true;
        Map<String, String> given = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject()
            .entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            if (PUBLIC_KEY.equals(key)) {
                if (value.isJsonPrimitive() && value.getAsJsonPrimitive()
                    .isBoolean()) {
                    isPublic = value.getAsBoolean();
                } else {
                    errors.add("'public' must be true or false");
                }
                continue;
            }
            Category category = Category.of(key);
            if (category == null || !value.isJsonObject()) {
                warnings.add("unknown top-level key '" + key + "' ignored");
                continue;
            }
            readCategory(category, value.getAsJsonObject(), given, warnings, errors);
        }
        if (!errors.isEmpty()) {
            return new Load(null, warnings, errors, Collections.<String>emptyList());
        }
        ProfileValidator.Result result = ProfileValidator.validate(given);
        warnings.addAll(result.warnings());
        errors.addAll(result.errors());
        Profile profile = result.ok() ? new Profile(name, isPublic, source, ProfileValues.of(result.values())) : null;
        return new Load(profile, warnings, errors, result.ignored());
    }

    private static void readCategory(Category category, JsonObject object, Map<String, String> given,
        List<String> warnings, List<String> errors) {
        for (Map.Entry<String, JsonElement> member : object.entrySet()) {
            String key = member.getKey();
            if (READONLY_KEY.equals(key)) {
                continue;
            }
            ProfileOption option = ProfileOptions.get(key);
            if (option == null) {
                warnings.add("unknown option " + category.key() + "." + key + " ignored");
                continue;
            }
            if (option.category() != category) {
                warnings.add(
                    "option " + key
                        + " belongs in '"
                        + option.category()
                            .key()
                        + "', not '"
                        + category.key()
                        + "'; ignored");
                continue;
            }
            String text = text(option, member.getValue());
            if (text == null) {
                errors.add(key + " must be " + typeName(option) + ", not " + member.getValue());
                continue;
            }
            given.put(key, text);
        }
    }

    /** Returns a JSON value's text for an option, or {@code null} when its JSON type is wrong (PROF-02). */
    private static String text(ProfileOption option, JsonElement value) {
        switch (option.type()) {
            case BOOLEAN:
                return isPrimitive(value) && value.getAsJsonPrimitive()
                    .isBoolean() ? value.getAsString() : null;
            case INT:
            case FLOAT:
            case DOUBLE:
                return isPrimitive(value) && value.getAsJsonPrimitive()
                    .isNumber() ? value.getAsString() : null;
            case STRING_LIST:
                if (!value.isJsonArray()) {
                    return null;
                }
                List<String> items = new ArrayList<>();
                for (JsonElement item : value.getAsJsonArray()) {
                    if (!isPrimitive(item)) {
                        return null;
                    }
                    items.add(item.getAsString());
                }
                return ProfileValues.listText(items);
            default:
                return isPrimitive(value) && value.getAsJsonPrimitive()
                    .isString() ? value.getAsString() : null;
        }
    }

    private static boolean isPrimitive(JsonElement value) {
        return value != null && value.isJsonPrimitive();
    }

    private static String typeName(ProfileOption option) {
        switch (option.type()) {
            case BOOLEAN:
                return "true or false";
            case STRING:
                return "a string";
            case STRING_LIST:
                return "a list of strings";
            default:
                return "a number";
        }
    }

    /**
     * Writes a profile in upstream's format: {@code public}, then the five categories with every kept option in
     * catalog order. Dropped options are not written back.
     *
     * @param profile the profile
     * @param texts   turns a built-in's lang-key texts into words; the identity for a file's literal texts
     * @return pretty-printed JSON
     */
    public static String write(Profile profile, Function<String, String> texts) {
        JsonObject root = new JsonObject();
        root.addProperty(PUBLIC_KEY, profile.isPublic());
        Map<Category, JsonObject> categories = new LinkedHashMap<>();
        for (Category category : Category.values()) {
            categories.put(category, new JsonObject());
        }
        for (ProfileOption option : ProfileOptions.all()) {
            if (!option.kept()) {
                continue;
            }
            categories.get(option.category())
                .add(option.key(), json(option, profile.values(), texts));
        }
        for (Map.Entry<Category, JsonObject> entry : categories.entrySet()) {
            if (!entry.getValue()
                .entrySet()
                .isEmpty()) {
                root.add(
                    entry.getKey()
                        .key(),
                    entry.getValue());
            }
        }
        return new GsonBuilder().setPrettyPrinting()
            .disableHtmlEscaping()
            .create()
            .toJson(root);
    }

    private static JsonElement json(ProfileOption option, ProfileValues values, Function<String, String> texts) {
        String key = option.key();
        switch (option.type()) {
            case BOOLEAN:
                return new JsonPrimitive(values.getBoolean(key));
            case INT:
                return new JsonPrimitive(values.getInt(key));
            case FLOAT:
                return new JsonPrimitive(values.getFloat(key));
            case DOUBLE:
                return new JsonPrimitive(values.getDouble(key));
            case STRING_LIST:
                JsonArray array = new JsonArray();
                for (String item : values.getStringList(key)) {
                    array.add(new JsonPrimitive(item));
                }
                return array;
            default:
                String value = values.getString(key);
                return new JsonPrimitive(isText(key) ? texts.apply(value) : value);
        }
    }

    /** Returns whether an option is one of a profile's display texts, which a built-in holds as lang keys. */
    public static boolean isText(String key) {
        return "description".equals(key) || "extraDescription".equals(key) || "warning".equals(key);
    }
}
