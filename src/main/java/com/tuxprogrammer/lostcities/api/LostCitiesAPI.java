package com.tuxprogrammer.lostcities.api;

/**
 * The entry point of Lost Cities: New Horizons' public API (docs/api.md). Another mod guards every use with
 * {@code Loader.isModLoaded(LostCitiesAPI.MODID)} and then calls {@link #get()}:
 *
 * <pre>
 * if (Loader.isModLoaded(LostCitiesAPI.MODID)) {
 *     ILostCityInformation info = LostCitiesAPI.get()
 *         .getLostInfo(world);
 *     if (info != null &amp;&amp; info.getChunkInfo(chunkX, chunkZ)
 *         .isCity()) {
 *         // ...
 *     }
 * }
 * </pre>
 *
 * <p>
 * Deviation: upstream hands the API over by IMC with a function, which 1.7.10's IMC cannot carry; a static accessor
 * needs no load-order handshake and is safe from any phase (P-CORE-1, D31; docs/new-spec/01-core/index.md, API).
 *
 * <p>
 * Side: both; the queries answer on the server only.
 */
public final class LostCitiesAPI {

    /** The mod ID to check with {@code Loader.isModLoaded}. */
    public static final String MODID = "lostcitiesnh";

    /** The API's version: raised when a member is added, and its major part when one changes or goes. */
    public static final String VERSION = "1.0";

    /** Part metadata key: a building part with it set gets no doorway to the street (05-assets). */
    public static final String META_DONTCONNECT = "dontconnect";
    /** Part metadata key: the support character of a bridge or highway part. */
    public static final String META_SUPPORT = "support";
    /** Part metadata key: the lower Z bound of a part's footprint. */
    public static final String META_Z_1 = "z1";
    /** Part metadata key: the upper Z bound of a part's footprint. */
    public static final String META_Z_2 = "z2";
    /** Part metadata key: hard air in the part never becomes water. */
    public static final String META_NOWATER = "nowater";
    /** Part metadata key: hard air in the part always becomes air. */
    public static final String META_FORCEDAIR = "forcedair";

    /** The implementation's class, loaded by name so this package names nothing outside it. */
    private static final String IMPLEMENTATION = "com.tuxprogrammer.lostcities.core.api.LostCitiesImpl";

    private LostCitiesAPI() {}

    /**
     * Returns the API. Never {@code null} once the mod is loaded; safe to call from any phase.
     *
     * @return the API
     * @throws IllegalStateException when the mod's classes are missing (the mod is not installed)
     */
    public static ILostCities get() {
        return Holder.INSTANCE;
    }

    /** Loads the implementation once, on the first call. */
    private static final class Holder {

        static final ILostCities INSTANCE = load();

        private static ILostCities load() {
            try {
                return (ILostCities) Class.forName(IMPLEMENTATION)
                    .getField("INSTANCE")
                    .get(null);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Lost Cities: New Horizons is not installed", e);
            }
        }
    }
}
