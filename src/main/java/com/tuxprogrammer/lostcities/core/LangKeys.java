package com.tuxprogrammer.lostcities.core;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Every lang key the code sends to players, in one place. Player-visible text is never hard-coded English: the server
 * sends these keys as {@code ChatComponentTranslation}, and the client resolves them from
 * {@code assets/lostcitiesnh/lang/en_US.lang} (CONTRIBUTING § Assets and localization).
 *
 * <p>
 * {@code LangKeysTest} checks that every key here, and every other key-shaped constant in the code, is in the lang
 * file; the dev self-check checks the same against the lang file inside the jar. The config screen's keys are not
 * listed here: GTNHLib derives them from the config fields, and {@code ConfigDocsTest} checks those.
 *
 * <p>
 * Side: both.
 */
public final class LangKeys {

    /** {@code /lostcities} usage line. */
    public static final String COMMAND_USAGE = "lostcitiesnh.command.usage";
    /** An unknown subcommand; argument: what was typed. */
    public static final String COMMAND_UNKNOWN = "lostcitiesnh.command.unknown";
    /** A subcommand the sender's permission level does not allow. */
    public static final String COMMAND_NO_PERMISSION = "lostcitiesnh.command.no_permission";

    /** {@code /lostcities test} usage line. */
    public static final String TEST_USAGE = "lostcitiesnh.command.test.usage";
    /** The scenario hooks are off (release jar without {@code -Dlostcitiesnh.testHooks=true}). */
    public static final String TEST_DISABLED = "lostcitiesnh.command.test.disabled";
    /** No scenario matches; argument: the selector. */
    public static final String TEST_NO_MATCH = "lostcitiesnh.command.test.no_match";
    /** A run is already in progress. */
    public static final String TEST_BUSY = "lostcitiesnh.command.test.busy";
    /** The scenario list; argument: the names. */
    public static final String TEST_LIST = "lostcitiesnh.command.test.list";
    /** A run started; argument: the number of scenarios. */
    public static final String TEST_STARTED = "lostcitiesnh.command.test.started";

    /** {@code /lostcities profile} usage line. */
    public static final String PROFILE_USAGE = "lostcitiesnh.command.profile.usage";
    /** The dimension has no pinned profile; argument: the dimension. */
    public static final String PROFILE_NONE = "lostcitiesnh.command.profile.none";
    /** The pinned profile; arguments: the dimension, the summary. */
    public static final String PROFILE_SHOW = "lostcitiesnh.command.profile.show";
    /** An unknown profile; arguments: the name, the known names. */
    public static final String PROFILE_UNKNOWN = "lostcitiesnh.command.profile.unknown";
    /** Pinning needs {@code confirm}; arguments: the profile, the dimension. */
    public static final String PROFILE_CONFIRM = "lostcitiesnh.command.profile.confirm";
    /** A profile was pinned; arguments: the profile, the dimension. */
    public static final String PROFILE_PINNED = "lostcitiesnh.command.profile.pinned";
    /** The world type has no height oracle; arguments: the dimension, the world type. */
    public static final String PROFILE_UNSUPPORTED = "lostcitiesnh.command.profile.unsupported";

    /** {@code /lostcities timers} usage line. */
    public static final String TIMERS_USAGE = "lostcitiesnh.command.timers.usage";
    /** The overlay is not installed; argument: the dimension. */
    public static final String TIMERS_NONE = "lostcitiesnh.command.timers.none";
    /** One report line; argument: the line. */
    public static final String TIMERS_LINE = "lostcitiesnh.command.timers.line";
    /** The timers were reset; argument: the dimension. */
    public static final String TIMERS_RESET = "lostcitiesnh.command.timers.reset";

    /** {@code /lostcities view} usage line. */
    public static final String VIEW_USAGE = "lostcitiesnh.command.view.usage";
    /** A player was moved; arguments: the player, X, Y, Z. */
    public static final String VIEW_DONE = "lostcitiesnh.command.view.done";

    /** {@code /lostcities find} usage line. */
    public static final String FIND_USAGE = "lostcitiesnh.command.find.usage";
    /** The world has no real cities to search; argument: the dimension. */
    public static final String FIND_NO_CITIES = "lostcitiesnh.command.find.no_cities";
    /** Nothing of a kind within a radius; arguments: the kind, the radius in chunks. */
    public static final String FIND_NONE = "lostcitiesnh.command.find.none";
    /** A find; arguments: kind, X, Y, Z, label, view X, view Y, view Z, yaw, pitch. */
    public static final String FIND_FOUND = "lostcitiesnh.command.find.found";

    /** One scenario's result; arguments: PASS or FAIL, name, milliseconds, message. */
    public static final String SCENARIO_RESULT = "lostcitiesnh.scenario.result";
    /** A run's summary; arguments: passed, failed. */
    public static final String SCENARIO_SUMMARY = "lostcitiesnh.scenario.summary";

    private LangKeys() {}

    /** Returns every key declared in this class (reflection gives no guaranteed order; callers sort if it matters). */
    public static List<String> all() {
        List<String> keys = new ArrayList<>();
        for (Field field : LangKeys.class.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers) && field.getType() == String.class) {
                try {
                    keys.add((String) field.get(null));
                } catch (IllegalAccessException e) {
                    // The fields are public, so reflection on them cannot be refused.
                    throw new IllegalStateException("cannot read " + field.getName(), e);
                }
            }
        }
        return Collections.unmodifiableList(keys);
    }
}
