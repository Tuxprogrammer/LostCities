package com.tuxprogrammer.lostcities.core;

import net.minecraft.launchwrapper.Launch;

/**
 * Tells the dev-only features, the scenario command and harness and the dev self-check, whether they may run.
 *
 * <p>
 * The development environment is the deobfuscated workspace that {@code runServer} and {@code runClient} start.
 * Release jars run obfuscated in the pack, where these features stay off, except through the explicit opt-in
 * {@link #TEST_HOOKS_PROPERTY} for real-pack testing (testing-gtnh-mods, layer 5).
 *
 * <p>
 * Side: both.
 */
public final class DevEnvironment {

    /** JVM property that turns the scenario hooks on in a release jar: {@code -Dlostcitiesnh.testHooks=true}. */
    public static final String TEST_HOOKS_PROPERTY = "lostcitiesnh.testHooks";

    /**
     * JVM property that runs the dev self-check outside the development environment, or skips it inside:
     * {@code -Dlostcitiesnh.selfCheck=true} or {@code false}.
     */
    public static final String SELF_CHECK_PROPERTY = "lostcitiesnh.selfCheck";

    /** Launch-blackboard key FML sets to {@code true} in a deobfuscated workspace. */
    private static final String DEOBFUSCATED_KEY = "fml.deobfuscatedEnvironment";

    private DevEnvironment() {}

    /**
     * Returns whether the game runs in the deobfuscated development workspace.
     *
     * @return {@code true} in {@code runServer} and {@code runClient}; {@code false} in a release jar and in unit
     *         tests, where there is no launch blackboard
     */
    public static boolean isActive() {
        return Launch.blackboard != null && Boolean.TRUE.equals(Launch.blackboard.get(DEOBFUSCATED_KEY));
    }

    /**
     * Returns whether the scenario command and harness may run.
     *
     * @return {@code true} in the development environment, or when {@code -Dlostcitiesnh.testHooks=true} is set
     */
    public static boolean testHooksEnabled() {
        return isActive() || Boolean.getBoolean(TEST_HOOKS_PROPERTY);
    }

    /**
     * Returns whether the dev self-check runs in {@code postInit}.
     *
     * @return the value of {@code -Dlostcitiesnh.selfCheck} when it is set, otherwise {@link #isActive()}
     */
    public static boolean selfCheckEnabled() {
        String value = System.getProperty(SELF_CHECK_PROPERTY);
        return value == null ? isActive() : Boolean.parseBoolean(value);
    }
}
