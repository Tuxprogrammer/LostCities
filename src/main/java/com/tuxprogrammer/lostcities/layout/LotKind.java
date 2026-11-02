package com.tuxprogrammer.lostcities.layout;

/**
 * What an edge lot shows: growth that never happened at a city's edge (alpha review AR-6,
 * docs/design/0009-city-edges-and-character.md §3).
 *
 * <p>
 * Side: both.
 */
public enum LotKind {

    /** Not a lot: a building, street, park or plaza as upstream plans it. */
    NONE("none"),
    /** An empty lot: bare clay and dirt ground at city level, nothing growing. */
    BARE("empty lot"),
    /** An excavation: a pit dug 1, 2 or 4 blocks for a basement inside a rim at city level. */
    PIT("excavation"),
    /** A foundation: a pit with a concrete floor and concrete columns rising for a high-rise's first stage. */
    FOUNDATION("foundation");

    private final String label;

    LotKind(String label) {
        this.label = label;
    }

    /** Returns the lot's label in logs and the finder. */
    public String label() {
        return label;
    }

    /** Returns whether the lot is dug below city ground. */
    public boolean isDug() {
        return this == PIT || this == FOUNDATION;
    }
}
