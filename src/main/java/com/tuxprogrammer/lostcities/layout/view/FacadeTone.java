package com.tuxprogrammer.lostcities.layout.view;

import java.util.Locale;

/**
 * The tone of a building's facade: its CIELAB lightness and its saturation, chroma relative to lightness, and the
 * contrast rule between two tones (docs/design/0010-facade-color-feathering.md §2 and §3).
 *
 * <p>
 * Immutable. Side: both (plain Java).
 */
public final class FacadeTone {

    /** Lightness below which saturation is measured against this floor, so near-black tones do not blow up. */
    public static final float SATURATION_FLOOR = 25f;
    /** Weight of a saturation difference against a lightness difference in {@link #contrast}. */
    public static final float SATURATION_WEIGHT = 60f;
    /** {@code facadeContrast} at or above which feathering is off. */
    public static final float CONTRAST_OFF = 200f;

    private final float lightness;
    private final float saturation;

    /**
     * Creates a tone.
     *
     * @param lightness  CIELAB L*, 0 to 100
     * @param saturation chroma over {@code max(L*, 25)}
     */
    public FacadeTone(float lightness, float saturation) {
        this.lightness = lightness;
        this.saturation = saturation;
    }

    /**
     * Returns the tone of a CIELAB color.
     *
     * @param lightness L*
     * @param a         a*
     * @param b         b*
     * @return the tone
     */
    public static FacadeTone ofLab(float lightness, float a, float b) {
        float chroma = (float) Math.sqrt(a * a + b * b);
        return new FacadeTone(lightness, chroma / Math.max(lightness, SATURATION_FLOOR));
    }

    /** Returns CIELAB lightness L*. */
    public float lightness() {
        return lightness;
    }

    /** Returns the saturation: chroma over {@code max(L*, 25)}. */
    public float saturation() {
        return saturation;
    }

    /**
     * Returns the contrast between two tones: {@code sqrt(ΔL*² + (60 ΔS)²)}. Hue is left out on purpose: the rule is
     * about a pale, low-saturation facade beside a dark, saturated one, not about variety of material.
     *
     * @param a one tone
     * @param b the other
     * @return the contrast, 0 or more
     */
    public static float contrast(FacadeTone a, FacadeTone b) {
        float dl = a.lightness - b.lightness;
        float ds = SATURATION_WEIGHT * (a.saturation - b.saturation);
        return (float) Math.sqrt(dl * dl + ds * ds);
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "L%.1f/S%.2f", lightness, saturation);
    }
}
