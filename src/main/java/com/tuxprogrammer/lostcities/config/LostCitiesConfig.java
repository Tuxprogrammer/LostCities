package com.tuxprogrammer.lostcities.config;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import com.tuxprogrammer.lostcities.LostCities;

/**
 * Registers the mod's GTNHLib config classes, which together make up {@code config/lostcitiesnh.cfg}.
 *
 * <p>
 * Registering is all the config screen needs: GTNHLib's client mixin on FML's mod list enables the Config button for
 * every mod with a registered config and no {@code guiFactory}, and builds the screen from the annotations
 * (docs/new-spec/09-client-ui/index.md, D31). The mod therefore has no {@code guiFactory} and no GUI code.
 *
 * <p>
 * Source: GTNHLib 0.11.54,
 * https://github.com/GTNewHorizons/GTNHLib/blob/0.11.54/src/main/java/com/gtnewhorizon/gtnhlib/config/ConfigurationManager.java#L61-L99
 * (register "before or during preInit"; the file is read at registration) and
 * https://github.com/GTNewHorizons/GTNHLib/blob/0.11.54/src/main/java/com/gtnewhorizon/gtnhlib/mixins/early/fml/MixinGuiModList.java#L31-L62
 * (the automatic screen).
 *
 * <p>
 * Side: both.
 */
public final class LostCitiesConfig {

    /** Every config class, in the order they are registered. Tests and the dev self-check walk this list. */
    public static final List<Class<?>> CLASSES = Collections
        .unmodifiableList(Arrays.<Class<?>>asList(WorldgenConfig.class, DecorativeMetalsConfig.class));

    /** What FML's config screen appends to an entry's lang key to find its tooltip. */
    public static final String TOOLTIP_SUFFIX = ".tooltip";

    private LostCitiesConfig() {}

    /**
     * Loads every config class from {@code config/lostcitiesnh.cfg}, writing missing keys with their defaults. Call
     * once, in {@code preInit}.
     *
     * @throws ConfigException when a config class is malformed, which is a programming error that stops the game
     */
    public static void register() {
        for (Class<?> configClass : CLASSES) {
            ConfigurationManager.registerConfig(configClass);
        }
    }

    /**
     * Returns the config options of a config class: its public static fields that GTNHLib reads.
     *
     * @param configClass one of {@link #CLASSES}
     * @return the option fields, sorted by name
     */
    public static List<Field> options(Class<?> configClass) {
        List<Field> options = new ArrayList<>();
        for (Field field : configClass.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers)
                && !Modifier.isFinal(modifiers)
                && !field.isSynthetic()
                && !field.isAnnotationPresent(Config.Ignore.class)) {
                options.add(field);
            }
        }
        options.sort(Comparator.comparing(Field::getName));
        return options;
    }

    /**
     * Returns the lang keys GTNHLib's config screen looks up for every config class: per category its label and
     * tooltip, per option its label and tooltip. GTNHLib builds them as {@code lostcitiesnh.<category>.<field>} and
     * lowercases each part; FML's config screen appends {@code .tooltip} for the tooltip.
     *
     * @return the keys, sorted
     */
    public static Set<String> langKeys() {
        Set<String> keys = new TreeSet<>();
        for (Class<?> configClass : CLASSES) {
            Config config = configClass.getAnnotation(Config.class);
            String category = LostCities.MODID + "."
                + config.category()
                    .toLowerCase(Locale.ROOT);
            keys.add(category);
            keys.add(category + TOOLTIP_SUFFIX);
            for (Field option : options(configClass)) {
                String key = category + "."
                    + option.getName()
                        .toLowerCase(Locale.ROOT);
                keys.add(key);
                keys.add(key + TOOLTIP_SUFFIX);
            }
        }
        return keys;
    }
}
