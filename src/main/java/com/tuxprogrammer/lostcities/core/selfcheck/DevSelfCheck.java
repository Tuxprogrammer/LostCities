package com.tuxprogrammer.lostcities.core.selfcheck;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.config.LostCitiesConfig;
import com.tuxprogrammer.lostcities.config.WorldgenConfig;
import com.tuxprogrammer.lostcities.core.DevEnvironment;
import com.tuxprogrammer.lostcities.core.LangFile;
import com.tuxprogrammer.lostcities.core.LangKeys;
import com.tuxprogrammer.lostcities.core.assets.AssetSelfCheck;
import com.tuxprogrammer.lostcities.core.assets.LoadedAssets;
import com.tuxprogrammer.lostcities.scenario.ScenarioRegistry;

/**
 * The dev self-check: in the development environment, a {@code postInit} pass that fails loudly, stopping the dev
 * server or client, when something the mod ships is inconsistent (CONTRIBUTING § Testing, layer 3).
 *
 * <p>
 * Checks: every lang key the code and the config screen use is in the jar's {@code en_US.lang}; the module flags
 * and the config agree; every scenario is registered under a valid, unique name; every asset loads and every block it
 * names resolves, to its primary or a documented fallback (M02, {@link AssetSelfCheck}). M08 adds the loot tier
 * ceiling.
 *
 * <p>
 * Runs in the development environment, or wherever {@code -Dlostcitiesnh.selfCheck=true} asks for it
 * ({@link DevEnvironment#selfCheckEnabled()}). Side: both.
 */
public final class DevSelfCheck {

    private DevSelfCheck() {}

    /**
     * Runs every check when the self-check is enabled, logs the outcome, and throws when anything failed.
     *
     * @throws SelfCheckFailedException when a check reports a problem; FML turns it into a crash that stops the game
     */
    public static void runIfEnabled() {
        if (!DevEnvironment.selfCheckEnabled()) {
            return;
        }
        List<String> problems = SelfCheck.runAll(checks());
        if (problems.isEmpty()) {
            LostCities.LOG.info("Dev self-check passed");
            return;
        }
        for (String problem : problems) {
            LostCities.LOG.error("Dev self-check: {}", problem);
        }
        throw new SelfCheckFailedException(problems);
    }

    /** Returns the checks, in the order they run. */
    static List<SelfCheck> checks() {
        List<String> required = new ArrayList<>(LangKeys.all());
        required.addAll(LostCitiesConfig.langKeys());
        List<SelfCheck> checks = new ArrayList<>(
            Arrays.asList(
                new LangCheck(required, jarLangKeys()),
                new ModuleConfigCheck(WorldgenConfig.class),
                ScenarioRegistry.selfCheck()));
        if (LoadedAssets.isLoaded()) {
            checks.add(new AssetSelfCheck(LoadedAssets.current()));
        }
        return checks;
    }

    private static Set<String> jarLangKeys() {
        InputStream in = DevSelfCheck.class.getResourceAsStream(LangFile.EN_US);
        if (in == null) {
            throw new IllegalStateException(LangFile.EN_US + " is not in the jar");
        }
        try {
            return LangFile.keys(in);
        } catch (IOException e) {
            throw new IllegalStateException("cannot read " + LangFile.EN_US, e);
        }
    }
}
