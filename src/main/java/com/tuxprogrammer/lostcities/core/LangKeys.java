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
