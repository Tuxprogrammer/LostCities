package com.tuxprogrammer.lostcities.asset.loot;

import java.util.Locale;

/**
 * GregTech's voltage tiers, with the two pre-electric ages the pack's loot bags name, in progression order. Every item
 * this mod's loot categories hold carries one, and nothing above {@link #CEILING} may generate in the overworld
 * (docs/design/0004-gtnh-layer-and-tier-ceiling.md §3).
 *
 * <p>
 * Side: both (plain Java).
 */
public enum LootTier {

    /** The stone age: vanilla supplies, food, trash. */
    STONE,
    /** The steam age: bronze, wrought iron, steam-age loot bags. */
    STEAM,
    /** Low voltage: steel, LV components and circuits. */
    LV,
    /** Medium voltage: aluminium, MV components. */
    MV,
    /** High voltage: stainless steel, HV components. */
    HV,
    /** Extreme voltage: titanium, EV components; the ceiling. */
    EV,
    /** Insane voltage and above never generate in the overworld. */
    IV,
    /** Ludicrous voltage. */
    LUV,
    /** ZPM voltage. */
    ZPM,
    /** Ultimate voltage. */
    UV,
    /** Highly ultimate voltage and beyond. */
    UHV;

    /** The highest tier anything generated in the overworld may have (0004 §3). */
    public static final LootTier CEILING = EV;

    /** Returns whether this tier is at or below {@link #CEILING}. */
    public boolean withinCeiling() {
        return compareTo(CEILING) <= 0;
    }

    /**
     * Parses a tier name, case-insensitively.
     *
     * @param name the name, such as {@code lv}
     * @return the tier, or {@code null} for an unknown name
     */
    public static LootTier parse(String name) {
        if (name == null) {
            return null;
        }
        try {
            return valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
