package com.tuxprogrammer.lostcities.config.profile;

import java.util.Objects;

/**
 * One loaded profile: its name, whether it is public, where it came from, and every kept option's resolved value
 * (docs/new-spec/02-profiles-dimensions/index.md, "Profile system"). A built-in's texts ({@code description},
 * {@code extraDescription}, {@code warning}) are lang keys; a file's are literal text. Both are sent to players as a
 * translation, which shows a literal text as it is.
 *
 * <p>
 * Immutable. Side: both (plain Java).
 */
public final class Profile {

    /** The {@code source} of a profile that ships in the jar. */
    public static final String BUILT_IN = "built-in";

    private final String name;
    private final boolean isPublic;
    private final String source;
    private final ProfileValues values;

    /**
     * Creates a profile.
     *
     * @param name     the name
     * @param isPublic upstream's root {@code public} flag, kept for a future picker
     * @param source   {@link #BUILT_IN} or the file it was read from
     * @param values   every kept option's value
     */
    public Profile(String name, boolean isPublic, String source, ProfileValues values) {
        this.name = Objects.requireNonNull(name);
        this.isPublic = isPublic;
        this.source = Objects.requireNonNull(source);
        this.values = Objects.requireNonNull(values);
    }

    /** Returns the name. */
    public String name() {
        return name;
    }

    /** Returns upstream's {@code public} flag. */
    public boolean isPublic() {
        return isPublic;
    }

    /** Returns {@link #BUILT_IN} or the file the profile was read from. */
    public String source() {
        return source;
    }

    /** Returns whether the profile ships in the jar. */
    public boolean builtIn() {
        return BUILT_IN.equals(source);
    }

    /** Returns every kept option's value. */
    public ProfileValues values() {
        return values;
    }

    @Override
    public String toString() {
        return name + " (" + source + ")";
    }
}
