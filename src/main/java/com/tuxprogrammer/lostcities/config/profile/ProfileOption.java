package com.tuxprogrammer.lostcities.config.profile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainSettings;

/**
 * One profile option: its key, the JSON category it sits in, its type, its default, its range or allowed values, and
 * what this port does with it (docs/new-spec/02-profiles-dimensions/profile-options.md). The catalog of every option
 * is {@link ProfileOptions}.
 *
 * <p>
 * A value is kept as canonical text, the form a pinned snapshot stores: {@code true}/{@code false}, a decimal integer,
 * {@link Float#toString(float)} or {@link Double#toString(double)}, the string itself, or a compact JSON array of
 * strings. {@link #canonical} turns a parsed value into that text after clamping it to the range.
 *
 * <p>
 * Immutable. Side: both (plain Java).
 */
public final class ProfileOption {

    /** The JSON categories of a profile file, in the order a profile is written (upstream's five). */
    public enum Category {

        /** General options. */
        LOSTCITY,
        /** Explosion and damage options. */
        EXPLOSIONS,
        /** City placement options. */
        CITIES,
        /** City sphere options. */
        CITYSPHERES,
        /** Client display options (all dropped). */
        CLIENT;

        /** Returns the JSON key of the category. */
        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }

        /**
         * Returns the category of a JSON key.
         *
         * @param key the key
         * @return the category, or {@code null}
         */
        public static Category of(String key) {
            for (Category category : values()) {
                if (category.key()
                    .equals(key)) {
                    return category;
                }
            }
            return null;
        }
    }

    /** The value types of options. */
    public enum Type {
        /** A JSON boolean. */
        BOOLEAN,
        /** A JSON number read as an int (upstream truncates toward zero). */
        INT,
        /** A JSON number read as a float. */
        FLOAT,
        /** A JSON number read as a double. */
        DOUBLE,
        /** A JSON string. */
        STRING,
        /** A JSON array of strings. */
        STRING_LIST
    }

    /** What this port does with an option. */
    public enum Status {
        /** Read by generation or by a command now. */
        LIVE,
        /** Kept, validated and pinned; its reader comes with the milestone {@link ProfileOption#owner()} names. */
        PENDING,
        /** Kept in the format, validated and pinned, with no effect until the landscapes return after beta (D12). */
        DEFERRED,
        /** Read and ignored, never pinned or written back (dead upstream or for features not ported). */
        DROPPED
    }

    private final String key;
    private final Category category;
    private final Type type;
    private final String defaultValue;
    private final Double min;
    private final Double max;
    private final List<String> allowed;
    private final Status status;
    private final String owner;
    private final boolean upstream;
    private final String alias;

    ProfileOption(String key, Category category, Type type, String defaultValue, Double min, Double max,
        List<String> allowed, Status status, String owner, boolean upstream, String alias) {
        this.key = Objects.requireNonNull(key);
        this.category = Objects.requireNonNull(category);
        this.type = Objects.requireNonNull(type);
        this.defaultValue = Objects.requireNonNull(defaultValue);
        this.min = min;
        this.max = max;
        this.allowed = allowed == null ? Collections.<String>emptyList()
            : Collections.unmodifiableList(new ArrayList<>(allowed));
        this.status = Objects.requireNonNull(status);
        this.owner = owner == null ? "" : owner;
        this.upstream = upstream;
        this.alias = alias;
    }

    /** Returns the JSON key, also the key a snapshot stores the value under. */
    public String key() {
        return key;
    }

    /** Returns the JSON category. */
    public Category category() {
        return category;
    }

    /** Returns the value type. */
    public Type type() {
        return type;
    }

    /**
     * Returns the legacy default: the value a pinned snapshot without this key resolves to, while new pins take
     * {@link #defaultValue()} (design record 0014).
     *
     * @return the legacy value as canonical text, or null when the option has none
     */
    public String legacyDefault() {
        return ProfileOptions.legacyDefault(key);
    }

    /** Returns the default as canonical text. */
    public String defaultValue() {
        return defaultValue;
    }

    /** Returns the smallest allowed number, or {@code null} for no bound. */
    public Double min() {
        return min;
    }

    /** Returns the largest allowed number, or {@code null} for no bound. */
    public Double max() {
        return max;
    }

    /** Returns the allowed values of an enum-like string, upper or lower case as written; empty for any. */
    public List<String> allowed() {
        return allowed;
    }

    /** Returns what the port does with the option. */
    public Status status() {
        return status;
    }

    /** Returns the milestone or area that reads the option, for {@link Status#PENDING} the one still to come. */
    public String owner() {
        return owner;
    }

    /** Returns whether upstream 1.20 has the option; {@code false} for this port's new options. */
    public boolean upstream() {
        return upstream;
    }

    /** Returns another key a profile file may use for this option (an older name), or {@code null}. */
    public String alias() {
        return alias;
    }

    /** Returns whether a profile keeps the option: every status but {@link Status#DROPPED}. */
    public boolean kept() {
        return status != Status.DROPPED;
    }

    /**
     * Returns the canonical text of a number for this option's type, clamped to its range.
     *
     * @param value    the number
     * @param warnings where a clamp or a truncation is reported
     * @return the text
     */
    public String canonical(double value, List<String> warnings) {
        double clamped = value;
        if (min != null && clamped < min) {
            clamped = min;
        }
        if (max != null && clamped > max) {
            clamped = max;
        }
        if (clamped != value) {
            warnings.add(key + "=" + format(value) + " is outside " + range() + "; using " + format(clamped));
        }
        switch (type) {
            case INT:
                int truncated = (int) clamped;
                if (truncated != clamped) {
                    warnings.add(key + "=" + format(clamped) + " is not a whole number; using " + truncated);
                }
                return Integer.toString(truncated);
            case FLOAT:
                // Plain decimal, as TerrainSettings writes its defaults: 0.0004, not 4.0E-4.
                return TerrainSettings.text((float) clamped);
            case DOUBLE:
                // Plain decimal when Java would use an exponent: 0.00025, not 2.5E-4.
                String text = Double.toString(clamped);
                return text.indexOf('E') < 0 ? text
                    : new java.math.BigDecimal(text).stripTrailingZeros()
                        .toPlainString();
            default:
                throw new IllegalStateException(key + " is not a number option");
        }
    }

    /** Returns the range as text, for messages and docs: {@code 0 to 1}, {@code at least 1}, or empty. */
    public String range() {
        if (min != null && max != null) {
            return format(min) + " to " + format(max);
        }
        if (min != null) {
            return "at least " + format(min);
        }
        if (max != null) {
            return "at most " + format(max);
        }
        return "";
    }

    private static String format(double value) {
        if (value == Math.rint(value) && Math.abs(value) < 1e15) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }

    @Override
    public String toString() {
        return category.key() + "." + key;
    }
}
