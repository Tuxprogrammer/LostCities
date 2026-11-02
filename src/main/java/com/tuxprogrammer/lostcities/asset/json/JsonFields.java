package com.tuxprogrammer.lostcities.asset.json;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.tuxprogrammer.lostcities.asset.Problems;

/**
 * Typed reads of one JSON object's fields, reporting every missing or mistyped field with its full path. This is the
 * hand-written replacement for upstream's codecs (docs/new-spec/05-assets/index.md, "DFU codecs, defaults and
 * errors"): a decoding error names the file, the field and the value, and never throws (fixes ASSET-23 and ASSET-25
 * through its callers).
 *
 * <p>
 * Unknown keys are ignored, as upstream's codecs ignore them. A getter that finds a problem reports an error to the
 * scope and returns the default, so a parser can read every field and report every problem in one pass; the caller
 * then rejects the asset if the scope saw an error. Side: both.
 */
public final class JsonFields {

    private static final Gson GSON = new Gson();

    private final JsonObject object;
    private final String path;
    private final Problems.Scope scope;

    /**
     * Wraps an object.
     *
     * @param object the object
     * @param path   its JSON path, empty for the document root
     * @param scope  where problems go
     */
    public JsonFields(JsonObject object, String path, Problems.Scope scope) {
        this.object = object;
        this.path = path;
        this.scope = scope;
    }

    /**
     * Parses one strict JSON document: no comments, no trailing content, an object or array at the top.
     *
     * @param text the document
     * @return the element
     * @throws JsonParseException when the text is not valid strict JSON
     */
    public static JsonElement parseStrict(String text) {
        return parseStrict(new StringReader(text));
    }

    /**
     * Parses one strict JSON document from a reader.
     *
     * @param reader the document; not closed
     * @return the element
     * @throws JsonParseException when the text is not valid strict JSON
     */
    public static JsonElement parseStrict(Reader reader) {
        try {
            JsonReader json = new JsonReader(reader);
            json.setLenient(false);
            JsonElement element = GSON.getAdapter(JsonElement.class)
                .read(json);
            if (json.peek() != JsonToken.END_DOCUMENT) {
                throw new JsonParseException("content after the end of the document");
            }
            return element;
        } catch (IOException | IllegalStateException | NumberFormatException e) {
            throw new JsonParseException(e.getMessage(), e);
        }
    }

    /** Returns the wrapped object. */
    public JsonObject object() {
        return object;
    }

    /** Returns this object's JSON path. */
    public String path() {
        return path;
    }

    /** Returns the scope problems go to. */
    public Problems.Scope scope() {
        return scope;
    }

    /** Returns whether the field is present and not JSON null. */
    public boolean has(String key) {
        return object.has(key) && !object.get(key)
            .isJsonNull();
    }

    /** Returns the path of a field of this object. */
    public String field(String key) {
        return path.isEmpty() ? key : path + "." + key;
    }

    /** Reports an error on a field of this object. */
    public void error(String key, String value, String message) {
        scope.error(field(key), value, message);
    }

    /** Reports a warning on a field of this object. */
    public void warning(String key, String value, String message) {
        scope.warning(field(key), value, message);
    }

    /**
     * Reads a string field.
     *
     * @param key      the field
     * @param required whether a missing field is an error
     * @return the value, or null when missing or mistyped
     */
    public String string(String key, boolean required) {
        JsonPrimitive primitive = primitive(key, required, "a string");
        if (primitive == null) {
            return null;
        }
        if (!primitive.isString()) {
            error(key, primitive.toString(), "must be a string");
            return null;
        }
        return primitive.getAsString();
    }

    /**
     * Reads a field that must hold exactly one character (fixes ASSET-23: upstream used the first character of any
     * string and crashed on an empty one).
     *
     * @param key      the field
     * @param required whether a missing field is an error
     * @return the character, or null when missing or invalid
     */
    public Character character(String key, boolean required) {
        String text = string(key, required);
        if (text == null) {
            return null;
        }
        if (text.length() != 1) {
            error(key, text, "must be exactly one character");
            return null;
        }
        return text.charAt(0);
    }

    /**
     * Reads an integer field.
     *
     * @param key      the field
     * @param required whether a missing field is an error
     * @return the value, or null when missing or mistyped
     */
    public Integer integer(String key, boolean required) {
        JsonPrimitive primitive = primitive(key, required, "an integer");
        if (primitive == null) {
            return null;
        }
        if (!primitive.isNumber()) {
            error(key, primitive.toString(), "must be an integer");
            return null;
        }
        double value = primitive.getAsDouble();
        if (value != Math.rint(value) || value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
            error(key, primitive.toString(), "must be a whole number in the int range");
            return null;
        }
        return (int) value;
    }

    /**
     * Reads an integer field with a default.
     *
     * @param key          the field
     * @param defaultValue the value when the field is missing
     * @return the value
     */
    public int integer(String key, int defaultValue) {
        Integer value = integer(key, false);
        return value == null ? defaultValue : value;
    }

    /**
     * Reads a number field.
     *
     * @param key      the field
     * @param required whether a missing field is an error
     * @return the value, or null when missing or mistyped
     */
    public Float number(String key, boolean required) {
        JsonPrimitive primitive = primitive(key, required, "a number");
        if (primitive == null) {
            return null;
        }
        if (!primitive.isNumber()) {
            error(key, primitive.toString(), "must be a number");
            return null;
        }
        float value = primitive.getAsFloat();
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            error(key, primitive.toString(), "must be a finite number");
            return null;
        }
        return value;
    }

    /**
     * Reads a number field with a default.
     *
     * @param key          the field
     * @param defaultValue the value when the field is missing
     * @return the value
     */
    public float number(String key, float defaultValue) {
        Float value = number(key, false);
        return value == null ? defaultValue : value;
    }

    /**
     * Reads a boolean field.
     *
     * @param key      the field
     * @param required whether a missing field is an error
     * @return the value, or null when missing or mistyped
     */
    public Boolean bool(String key, boolean required) {
        JsonPrimitive primitive = primitive(key, required, "a boolean");
        if (primitive == null) {
            return null;
        }
        if (!primitive.isBoolean()) {
            error(key, primitive.toString(), "must be true or false");
            return null;
        }
        return primitive.getAsBoolean();
    }

    /**
     * Reads a boolean field with a default.
     *
     * @param key          the field
     * @param defaultValue the value when the field is missing
     * @return the value
     */
    public boolean flag(String key, boolean defaultValue) {
        Boolean value = bool(key, false);
        return value == null ? defaultValue : value;
    }

    /**
     * Reads an object field.
     *
     * @param key      the field
     * @param required whether a missing field is an error
     * @return the object's fields, or null when missing or mistyped
     */
    public JsonFields object(String key, boolean required) {
        if (!has(key)) {
            if (required) {
                error(key, null, "is required (an object)");
            }
            return null;
        }
        JsonElement element = object.get(key);
        if (!element.isJsonObject()) {
            error(key, element.toString(), "must be an object");
            return null;
        }
        return new JsonFields(element.getAsJsonObject(), field(key), scope);
    }

    /**
     * Reads an array field.
     *
     * @param key      the field
     * @param required whether a missing field is an error
     * @return the array, or null when missing or mistyped
     */
    public JsonArray array(String key, boolean required) {
        if (!has(key)) {
            if (required) {
                error(key, null, "is required (a list)");
            }
            return null;
        }
        JsonElement element = object.get(key);
        if (!element.isJsonArray()) {
            error(key, element.toString(), "must be a list");
            return null;
        }
        return element.getAsJsonArray();
    }

    /**
     * Reads an array of objects; each element that is not an object is reported and skipped.
     *
     * @param key      the field
     * @param required whether a missing field is an error
     * @return the elements' fields, in order; empty when missing
     */
    public List<JsonFields> objects(String key, boolean required) {
        JsonArray array = array(key, required);
        if (array == null) {
            return Collections.emptyList();
        }
        List<JsonFields> out = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            JsonElement element = array.get(i);
            String elementPath = field(key) + "[" + i + "]";
            if (!element.isJsonObject()) {
                scope.error(elementPath, element.toString(), "must be an object");
                continue;
            }
            out.add(new JsonFields(element.getAsJsonObject(), elementPath, scope));
        }
        return out;
    }

    /**
     * Reads a field that is one string or a list of strings, as upstream's {@code listOrStringList} codec accepts.
     *
     * @param key      the field
     * @param required whether a missing field is an error
     * @return the strings, in order; null when missing or mistyped
     */
    public List<String> stringOrList(String key, boolean required) {
        if (!has(key)) {
            if (required) {
                error(key, null, "is required (a string or a list of strings)");
            }
            return null;
        }
        JsonElement element = object.get(key);
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive()
            .isString()) {
            return Collections.singletonList(element.getAsString());
        }
        if (!element.isJsonArray()) {
            error(key, element.toString(), "must be a string or a list of strings");
            return null;
        }
        return strings(element.getAsJsonArray(), field(key));
    }

    /**
     * Reads a list of strings field.
     *
     * @param key      the field
     * @param required whether a missing field is an error
     * @return the strings, in order; null when missing or mistyped
     */
    public List<String> stringList(String key, boolean required) {
        JsonArray array = array(key, required);
        return array == null ? null : strings(array, field(key));
    }

    /** Returns the keys of this object, in file order. */
    public List<String> keys() {
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            keys.add(entry.getKey());
        }
        return keys;
    }

    private List<String> strings(JsonArray array, String arrayPath) {
        List<String> out = new ArrayList<>();
        boolean ok = true;
        for (int i = 0; i < array.size(); i++) {
            JsonElement element = array.get(i);
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive()
                .isString()) {
                out.add(element.getAsString());
            } else {
                scope.error(arrayPath + "[" + i + "]", element.toString(), "must be a string");
                ok = false;
            }
        }
        return ok ? out : null;
    }

    private JsonPrimitive primitive(String key, boolean required, String what) {
        if (!has(key)) {
            if (required) {
                error(key, null, "is required (" + what + ")");
            }
            return null;
        }
        JsonElement element = object.get(key);
        if (!element.isJsonPrimitive()) {
            error(key, element.toString(), "must be " + what);
            return null;
        }
        return element.getAsJsonPrimitive();
    }
}
