package com.tuxprogrammer.lostcities.core;

/**
 * The feature areas that each have a module flag (docs/new-spec/01-core/index.md, "Module flags"). A flag switches the
 * area's generation and behavior off; it never changes registration (CONTRIBUTING § Architecture).
 *
 * <p>
 * Each constant names its key in the {@code worldgen} category of the config. The key is part of the config file
 * format: once released it is never renamed (config-safe-registration).
 *
 * <p>
 * Side: both (plain Java).
 */
public enum Module {

    /** Cities: buildings, streets, bridges and their parks (M05). */
    CITIES("cities"),
    /** Highways, between and through cities (M06). */
    HIGHWAYS("highways"),
    /** Railways, stations and subways (M06). */
    RAILWAYS("railways"),
    /** Scattered structures outside cities (M07). */
    SCATTERED_STRUCTURES("scatteredStructures");

    private final String configKey;

    Module(String configKey) {
        this.configKey = configKey;
    }

    /** Returns the module's key in the {@code worldgen} config category, also the config field's name. */
    public String configKey() {
        return configKey;
    }
}
