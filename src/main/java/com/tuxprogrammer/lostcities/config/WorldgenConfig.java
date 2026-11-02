package com.tuxprogrammer.lostcities.config;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.tuxprogrammer.lostcities.LostCities;

/**
 * The {@code worldgen} category of {@code config/lostcitiesnh.cfg}: one module flag per feature area. A flag switches
 * that area's generation off; it never changes what is registered, because the mod registers no content at all
 * (CONTRIBUTING § Architecture, docs/new-spec/01-core/index.md).
 *
 * <p>
 * Every key here changes what a world generates, so the category is marked {@code @Config.RequiresWorldRestart} and
 * every comment says "new worlds only": when profile pinning lands (M04, docs/design/0005-rwg-overlay.md §7) the flags
 * are copied into a world's pinned profile when the world is created, and later edits affect only worlds created
 * after them (D31). GTNHLib's automatic config screen shows the restart marker. Labels and tooltips are the lang keys
 * {@code lostcitiesnh.worldgen.<field>} and {@code lostcitiesnh.worldgen.<field>.tooltip}, with the field name in
 * lowercase, because GTNHLib lowercases every part of a generated key.
 *
 * <p>
 * Read the flags through {@link com.tuxprogrammer.lostcities.core.ModuleState}, never directly at a use site. Every key
 * is listed in docs/config.md; {@code ConfigDocsTest} keeps the two in step.
 *
 * <p>
 * Source: GTNHLib 0.11.54,
 * https://github.com/GTNewHorizons/GTNHLib/blob/0.11.54/src/main/java/com/gtnewhorizon/gtnhlib/config/Config.java#L58-L84
 * (lang keys from {@code %mod.%cat.%field} only with {@code @LangKeyPattern}) and #L205-L207
 * ({@code RequiresWorldRestart});
 * https://github.com/GTNewHorizons/GTNHLib/blob/0.11.54/src/main/java/com/gtnewhorizon/gtnhlib/config/ConfigurationManager.java#L462-L484
 * (every placeholder's value is lowercased).
 *
 * <p>
 * Side: both (the integrated server reads the same file); the values matter only on the server.
 */
@Config(modid = LostCities.MODID, category = WorldgenConfig.CATEGORY)
@Config.LangKeyPattern
@Config.RequiresWorldRestart
@Config.Comment({ "World generation: one switch per feature area. New worlds only: these values are pinned into a",
    "world when it is created, and changing them later does not change worlds that already exist." })
public final class WorldgenConfig {

    /** The category name, also the middle part of every lang key here. Not an option: GTNHLib reads every field. */
    @Config.Ignore
    public static final String CATEGORY = "worldgen";

    @Config.Comment("Generate cities: buildings, streets, bridges and their parks. New worlds only.")
    @Config.DefaultBoolean(true)
    public static boolean cities = true;

    @Config.Comment("Generate highways between and through cities. New worlds only.")
    @Config.DefaultBoolean(true)
    public static boolean highways = true;

    @Config.Comment("Generate railways, stations and subways. New worlds only.")
    @Config.DefaultBoolean(true)
    public static boolean railways = true;

    @Config.Comment("Generate scattered structures outside cities. New worlds only.")
    @Config.DefaultBoolean(true)
    public static boolean scatteredStructures = true;

    @Config.Comment({ "Which dimensions get cities, and with which profile: one '<dimension id>=<profile>' per line.",
        "The profile is pinned into a world when it is created. New worlds only." })
    @Config.DefaultStringList({ "0=default" })
    public static String[] dimensionProfiles = { "0=default" };

    @Config.Comment({ "Give cities to worlds created before this mod was added, at their next load. Off by default:",
        "already generated chunks will not match the new ones at the border. Use '/lostcities profile pin' to opt in",
        "one world instead." })
    @Config.DefaultBoolean(false)
    public static boolean enableOnExistingWorlds = false;

    private WorldgenConfig() {}
}
