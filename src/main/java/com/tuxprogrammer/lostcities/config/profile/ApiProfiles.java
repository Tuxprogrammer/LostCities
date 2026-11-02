package com.tuxprogrammer.lostcities.config.profile;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/setup/LostCityProfileSetupImp.java
// Avoids: CORE-03, CORE-06, CORE-38

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import com.tuxprogrammer.lostcities.api.ILostCityProfile;
import com.tuxprogrammer.lostcities.api.ILostCityProfileSetup;

/**
 * Profiles other mods add through the API ({@code ILostCities.registerProfileSetup}): the callbacks, frozen when the
 * first server is about to start, and the profiles they build each time the profiles are read
 * (docs/new-spec/01-core/index.md, API, P-CORE-3; docs/api.md).
 *
 * <p>
 * Changes from upstream:
 * <ul>
 * <li>Avoids CORE-03: the callbacks run, at server start and on every {@code profile reload}; upstream filled a list
 * nothing read.</li>
 * <li>Avoids CORE-06: an API profile is rebuilt on every read of the profiles, so a reload never loses it; which
 * dimension uses it is {@code dimensionProfiles}', and a world keeps its pinned copy.</li>
 * <li>Avoids CORE-38: the setters are spelled right and cover every option; values are checked as a file's are, and a
 * profile that fails is rejected with a message instead of going live.</li>
 * </ul>
 *
 * <p>
 * Thread-safe. Side: both (plain Java).
 */
public final class ApiProfiles {

    /** The game's callbacks. */
    public static final ApiProfiles INSTANCE = new ApiProfiles();

    /** What a profile name may be, as for {@code saveprofile}. */
    static final Pattern NAME = Pattern.compile("[a-z0-9_]+");

    /** The source a profile created through the API reports. */
    public static final String SOURCE = "api";

    /** Number of {@code cityLevel<N>Height} options. */
    static final int CITY_LEVELS = 8;

    /** What one read of the callbacks produced. */
    public static final class Build {

        private final Map<String, Profile> profiles;
        private final List<String> errors;

        Build(Map<String, Profile> profiles, List<String> errors) {
            this.profiles = Collections.unmodifiableMap(profiles);
            this.errors = Collections.unmodifiableList(errors);
        }

        /** Returns the accepted profiles by name. */
        public Map<String, Profile> profiles() {
            return profiles;
        }

        /** Returns one line per rejected profile or failed callback. */
        public List<String> errors() {
            return errors;
        }
    }

    private final List<Consumer<ILostCityProfileSetup>> callbacks = new ArrayList<>();
    private boolean frozen;

    /** Creates an empty set; tests make their own, the game uses {@link #INSTANCE}. */
    public ApiProfiles() {}

    /**
     * Registers a callback.
     *
     * @param callback the callback
     * @throws IllegalStateException once frozen
     */
    public synchronized void register(Consumer<ILostCityProfileSetup> callback) {
        if (frozen) {
            throw new IllegalStateException(
                "Lost Cities profile callbacks are frozen once a server has started; register them before postInit ends");
        }
        callbacks.add(java.util.Objects.requireNonNull(callback));
    }

    /** Freezes the callbacks: called when a server is about to start, before any world is pinned. */
    public synchronized void freeze() {
        frozen = true;
    }

    /** Returns whether the callbacks are frozen. */
    public synchronized boolean frozen() {
        return frozen;
    }

    /** Returns how many callbacks are registered. */
    public synchronized int size() {
        return callbacks.size();
    }

    /**
     * Runs every callback and checks the profiles they create.
     *
     * @param base  looks up a base profile (a built-in or a file profile), {@code null} when unknown
     * @param taken whether a name already belongs to a built-in or a file profile
     * @return the profiles and the errors
     */
    public synchronized Build build(Function<String, Profile> base, Predicate<String> taken) {
        Map<String, Profile> out = new TreeMap<>();
        List<String> errors = new ArrayList<>();
        for (Consumer<ILostCityProfileSetup> callback : callbacks) {
            List<Draft> drafts = new ArrayList<>();
            try {
                callback.accept((name, baseProfile) -> {
                    Draft draft = draft(name, baseProfile, base, taken, out, drafts);
                    drafts.add(draft);
                    return draft;
                });
            } catch (RuntimeException e) {
                // A mod's broken callback rejects its own profiles; the rest still load.
                errors.add("a profile callback failed (" + e + "); its profiles are left out");
                continue;
            }
            for (Draft draft : drafts) {
                ProfileValidator.Result result = ProfileValidator.validate(draft.values);
                if (!result.ok()) {
                    errors.add("API profile '" + draft.name + "' rejected: " + String.join("; ", result.errors()));
                    continue;
                }
                out.put(draft.name, new Profile(draft.name, true, SOURCE, ProfileValues.of(result.values())));
            }
        }
        return new Build(out, errors);
    }

    private static Draft draft(String name, String baseProfile, Function<String, Profile> base, Predicate<String> taken,
        Map<String, Profile> built, List<Draft> drafts) {
        if (name == null || !NAME.matcher(name)
            .matches()) {
            throw new IllegalArgumentException("profile name '" + name + "' is not lower-case letters, digits and _");
        }
        boolean drafted = drafts.stream()
            .anyMatch(d -> d.name.equals(name));
        if (taken.test(name) || built.containsKey(name) || drafted) {
            throw new IllegalArgumentException("profile name '" + name + "' is taken");
        }
        Profile from = baseProfile == null ? null : base.apply(baseProfile);
        if (from == null) {
            throw new IllegalArgumentException("Unknown base profile '" + baseProfile + "'!");
        }
        return new Draft(name, from.values());
    }

    /** A profile a callback is configuring: the base's values, with the callback's changes. */
    static final class Draft implements ILostCityProfile {

        final String name;
        final Map<String, String> values;

        Draft(String name, ProfileValues base) {
            this.name = name;
            this.values = new TreeMap<>(base.asMap());
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void setDescription(String description) {
            setOption("description", description);
        }

        @Override
        public void setWorldStyle(String worldStyle) {
            setOption("worldStyle", worldStyle);
        }

        @Override
        public void setCityChance(double chance) {
            setOption("cityChance", Double.toString(chance));
        }

        @Override
        public void setRuinChance(float chance, float minPercent, float maxPercent) {
            setOption("ruinChance", Float.toString(chance));
            setOption("ruinMinlevelPercent", Float.toString(minPercent));
            setOption("ruinMaxlevelPercent", Float.toString(maxPercent));
        }

        @Override
        public void setGroundLevel(int groundLevel) {
            setOption("groundLevel", Integer.toString(groundLevel));
        }

        @Override
        public void setCityLevelHeights(int... levels) {
            if (levels == null || levels.length != CITY_LEVELS) {
                throw new IllegalArgumentException("setCityLevelHeights takes " + CITY_LEVELS + " levels");
            }
            for (int i = 0; i < CITY_LEVELS; i++) {
                setOption(String.format(Locale.ROOT, "cityLevel%dHeight", i), Integer.toString(levels[i]));
            }
        }

        @Override
        public void setOceanCorrectionBorder(int border) {
            setOption("oceanCorrectionBorder", Integer.toString(border));
        }

        @Override
        public void setOption(String key, String value) {
            ProfileOption option = key == null ? null : ProfileOptions.get(key);
            if (option == null) {
                throw new IllegalArgumentException("unknown profile option '" + key + "'");
            }
            values.put(option.key(), value == null ? "" : value);
        }
    }
}
