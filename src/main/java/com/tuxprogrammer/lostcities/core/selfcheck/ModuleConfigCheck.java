package com.tuxprogrammer.lostcities.core.selfcheck;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.tuxprogrammer.lostcities.config.LostCitiesConfig;
import com.tuxprogrammer.lostcities.core.Module;

/**
 * Checks that the module flags and the config agree: every {@link Module} has a boolean option of the same name in its
 * config class, every boolean option there is a module, and every option has a comment and an explicit default
 * (CONTRIBUTING § GTNH integration, "Config").
 *
 * <p>
 * Side: both (plain Java over the config annotations).
 */
public final class ModuleConfigCheck implements SelfCheck {

    private final Class<?> moduleConfig;

    /**
     * Creates the check.
     *
     * @param moduleConfig the config class that holds the module flags
     */
    public ModuleConfigCheck(Class<?> moduleConfig) {
        this.moduleConfig = moduleConfig;
    }

    @Override
    public String name() {
        return "modules";
    }

    @Override
    public List<String> problems() {
        List<String> problems = new ArrayList<>();
        Set<String> moduleKeys = new TreeSet<>();
        for (Module module : Module.values()) {
            moduleKeys.add(module.configKey());
        }
        Set<String> optionNames = new TreeSet<>();
        for (Field option : LostCitiesConfig.options(moduleConfig)) {
            optionNames.add(option.getName());
            if (option.getType() == boolean.class && !moduleKeys.contains(option.getName())) {
                problems.add("boolean option " + option.getName() + " is not a Module");
            }
            if (!option.isAnnotationPresent(Config.Comment.class)) {
                problems.add("option " + option.getName() + " has no @Config.Comment");
            }
            if (option.getType() == boolean.class && !option.isAnnotationPresent(Config.DefaultBoolean.class)) {
                problems.add("option " + option.getName() + " has no @Config.DefaultBoolean");
            }
        }
        for (String key : moduleKeys) {
            if (!optionNames.contains(key)) {
                problems.add("module " + key + " has no option in " + moduleConfig.getSimpleName());
            }
        }
        return problems;
    }
}
