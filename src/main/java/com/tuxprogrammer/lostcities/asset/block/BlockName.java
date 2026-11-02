package com.tuxprogrammer.lostcities.asset.block;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * A 1.20 block name as converted upstream assets write it: {@code namespace:id}, optionally followed by
 * {@code [prop=value,...]}. It goes through the block-name map, which turns the id into a 1.7.10 block and the
 * properties into metadata (docs/new-spec/05-assets/asset-format.md, "Block references"; D14, D19).
 *
 * <p>
 * Parsing is strict: anything that is neither this form nor a direct {@code modid:name:meta} reference, such as 1.12's
 * {@code minecraft:red_sandstone@2}, is rejected with a message, never read as air (fixes ASSET-03 and CORE-20).
 * Side: both.
 */
public final class BlockName {

    private final String id;
    private final Map<String, String> properties;

    private BlockName(String id, Map<String, String> properties) {
        this.id = id;
        this.properties = properties;
    }

    /**
     * Parses a 1.20 block name.
     *
     * @param text the name, {@code ns:id} or {@code ns:id[prop=value,...]}
     * @return the parsed name
     * @throws IllegalArgumentException with a readable reason when the text is malformed
     */
    public static BlockName parse(String text) {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("empty block name");
        }
        String id = text;
        Map<String, String> properties = new TreeMap<>();
        int open = text.indexOf('[');
        if (open >= 0) {
            if (!text.endsWith("]")) {
                throw new IllegalArgumentException("property list does not end with ']'");
            }
            id = text.substring(0, open);
            String list = text.substring(open + 1, text.length() - 1);
            if (!list.isEmpty()) {
                for (String pair : list.split(",", -1)) {
                    int equals = pair.indexOf('=');
                    if (equals <= 0 || equals == pair.length() - 1) {
                        throw new IllegalArgumentException("property '" + pair + "' is not name=value");
                    }
                    String key = pair.substring(0, equals)
                        .trim();
                    String value = pair.substring(equals + 1)
                        .trim();
                    if (properties.put(key, value) != null) {
                        throw new IllegalArgumentException("property '" + key + "' is given twice");
                    }
                }
            }
        }
        validateId(id);
        return new BlockName(id, Collections.unmodifiableMap(properties));
    }

    /** Returns the id without properties, {@code ns:id}. */
    public String id() {
        return id;
    }

    /** Returns the properties, sorted by name; empty when none are given. */
    public Map<String, String> properties() {
        return properties;
    }

    /** Returns the canonical text form, properties sorted. */
    @Override
    public String toString() {
        if (properties.isEmpty()) {
            return id;
        }
        StringBuilder out = new StringBuilder(id).append('[');
        boolean first = true;
        for (Map.Entry<String, String> property : properties.entrySet()) {
            if (!first) {
                out.append(',');
            }
            first = false;
            out.append(property.getKey())
                .append('=')
                .append(property.getValue());
        }
        return out.append(']')
            .toString();
    }

    private static void validateId(String id) {
        int colon = id.indexOf(':');
        if (colon <= 0 || colon == id.length() - 1 || id.indexOf(':', colon + 1) >= 0) {
            throw new IllegalArgumentException("not a namespace:id block name");
        }
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                || c == '_'
                || c == '.'
                || c == '-'
                || c == '/'
                || c == ':';
            if (!ok) {
                throw new IllegalArgumentException(
                    "character '" + c + "' is not allowed in a 1.20 block id (1.12's '@meta' form is not read)");
            }
        }
    }
}
