package com.tuxprogrammer.lostcities.asset.block;

/**
 * The decorative metals of D29: each metal at or below the EV ceiling that the block catalog places, with one worldgen
 * flag that covers its blocks in every mod. A flagged metal appears only as capped accents; with its flag off, each
 * entry naming it takes its fallback (docs/new-spec/05-assets/block-catalog/index.md, "Decorative metals (D29)").
 * Metals above EV are never placed and have no constant here.
 *
 * <p>
 * The blocks each metal covers are data, in {@code data/lostcitiesnh/blockmap/metals.json}; the config key of each
 * flag is {@code decorativeMetals.<key>}. Side: both.
 */
public enum DecorativeMetal {

    IRON("iron"),
    GOLD("gold"),
    CAST_IRON("castIron"),
    COPPER("copper"),
    TIN("tin"),
    BRONZE("bronze"),
    LEAD("lead"),
    SILVER("silver"),
    NICKEL("nickel"),
    INVAR("invar"),
    ELECTRUM("electrum"),
    STEEL("steel"),
    BLACK_STEEL("blackSteel"),
    DAMASCUS_STEEL("damascusSteel"),
    METEORIC_IRON("meteoricIron"),
    ELECTRICAL_STEEL("electricalSteel"),
    DARK_STEEL("darkSteel"),
    ALUMINUM("aluminum"),
    ALUMINUM_BRASS("aluminumBrass"),
    COBALT("cobalt"),
    STAINLESS_STEEL("stainlessSteel"),
    TITANIUM("titanium"),
    DESH("desh"),
    THAUMIUM("thaumium");

    private final String key;

    DecorativeMetal(String key) {
        this.key = key;
    }

    /** Returns the key, as the data files and the config field name write it ({@code castIron}). */
    public String key() {
        return key;
    }

    /**
     * Returns the metal with a key.
     *
     * @param key the key, exactly as {@link #key()} writes it
     * @return the metal, or null when there is none
     */
    public static DecorativeMetal byKey(String key) {
        for (DecorativeMetal metal : values()) {
            if (metal.key.equals(key)) {
                return metal;
            }
        }
        return null;
    }
}
