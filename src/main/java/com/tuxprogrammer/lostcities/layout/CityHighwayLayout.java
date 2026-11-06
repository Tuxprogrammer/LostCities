package com.tuxprogrammer.lostcities.layout;

import java.util.Locale;

/**
 * The profile option {@code cityHighwayLayout} (design record 0017): what highway a city of the link network
 * ({@code networkLayout=links}) carries inside itself. Worlds pinned before the option read {@link #NONE} (D282).
 */
public enum CityHighwayLayout {

    /** No city highway: links stop at a plaza on the city's edge (0015 §5), the layout before 0017. */
    NONE,
    /** A beltway in every city with the room for one; the others get a cut-through. */
    BELTWAY,
    /** A cut-through in every city with two links. */
    CUTTHROUGH,
    /** Per city, a beltway or a cut-through, weighted by the city's room (owner decision 3). */
    RANDOM;

    /**
     * Returns the layout an option value names, or {@link #NONE} for any other text.
     *
     * @param text the option value, any case
     * @return the layout
     */
    public static CityHighwayLayout parse(String text) {
        if (text != null) {
            for (CityHighwayLayout layout : values()) {
                if (layout.key()
                    .equals(
                        text.trim()
                            .toLowerCase(Locale.ROOT))) {
                    return layout;
                }
            }
        }
        return NONE;
    }

    /** Returns the option value that names this layout. */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
