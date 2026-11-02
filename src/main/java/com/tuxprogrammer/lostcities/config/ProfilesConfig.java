package com.tuxprogrammer.lostcities.config;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.tuxprogrammer.lostcities.LostCities;

/**
 * The {@code profiles} category of {@code config/lostcitiesnh.cfg}: where cities generate and with which profile
 * (docs/design/0005-rwg-overlay.md §7). The profile named here is resolved and pinned into a world's saved data when
 * the world is created, so these keys apply to new worlds only; {@code enableOnExistingWorlds} opts worlds created
 * before the mod in at their next load.
 *
 * <p>
 * Labels and tooltips are the lang keys {@code lostcitiesnh.profiles.<field>} and their {@code .tooltip}, lowercased
 * as GTNHLib builds them. Every key is listed in docs/config.md.
 *
 * <p>
 * Side: both (the integrated server reads the same file); the values matter only on the server.
 */
@Config(modid = LostCities.MODID, category = ProfilesConfig.CATEGORY)
@Config.LangKeyPattern
@Config.RequiresWorldRestart
@Config.Comment({ "Where cities generate and with which profile. New worlds only: a world pins its profile when it",
    "is created, and changing these later does not change worlds that already exist." })
public final class ProfilesConfig {

    /** The category name, also the middle part of every lang key here. */
    @Config.Ignore
    public static final String CATEGORY = "profiles";

    @Config.Comment({ "Which dimensions get cities, and with which profile: one '<dimension id>=<profile>' per line.",
        "The profile is pinned into a world when it is created. New worlds only." })
    @Config.DefaultStringList({ "0=default" })
    public static String[] dimensionProfiles = { "0=default" };

    @Config.Comment({ "Give cities to worlds created before this mod was added, at their next load. Off by default:",
        "already generated chunks will not match the new ones at the border. Use '/lostcities profile pin' to opt in",
        "one world instead." })
    @Config.DefaultBoolean(false)
    public static boolean enableOnExistingWorlds = false;

    private ProfilesConfig() {}
}
