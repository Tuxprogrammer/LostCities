package com.tuxprogrammer.lostcities.layout.view;

import java.util.List;

/**
 * The layout's view of facade colors: the tone of a building's outer walls under a stack of palettes
 * (docs/design/0010-facade-color-feathering.md §2). The asset side measures it from the shipped block colors.
 *
 * <p>
 * Side: both.
 */
public interface FacadeView {

    /** A view that knows no colors: every tone is unknown, so feathering changes nothing. */
    FacadeView NONE = (palettes, building) -> null;

    /**
     * Returns the tone of a building's facade under a stack of palettes.
     *
     * @param palettes the palette names, lowest layer first (the building's own palette goes on top of them)
     * @param building the building's name
     * @return the tone, or {@code null} when no facade block of the building has a known opaque color
     */
    FacadeTone tone(List<String> palettes, String building);
}
