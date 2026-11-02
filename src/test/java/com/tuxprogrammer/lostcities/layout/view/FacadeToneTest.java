package com.tuxprogrammer.lostcities.layout.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/** The facade tone and its contrast rule (docs/design/0010-facade-color-feathering.md §2 and §3). */
public class FacadeToneTest {

    @Test
    public void saturationIsChromaOverLightness() {
        FacadeTone tone = FacadeTone.ofLab(50f, 30f, 40f);
        assertEquals(1.0f, tone.saturation(), 1e-5f);
        FacadeTone dark = FacadeTone.ofLab(10f, 5f, 0f);
        assertEquals(0.2f, dark.saturation(), 1e-5f, "under L* 25 the floor applies");
    }

    @Test
    public void contrastWeighsLightnessAndSaturation() {
        FacadeTone a = new FacadeTone(80f, 0.3f);
        FacadeTone b = new FacadeTone(50f, 0.3f);
        assertEquals(30f, FacadeTone.contrast(a, b), 1e-4f);
        FacadeTone c = new FacadeTone(80f, 0.8f);
        assertEquals(30f, FacadeTone.contrast(a, c), 1e-4f);
        assertEquals(FacadeTone.contrast(b, c), FacadeTone.contrast(c, b), 0f);
    }

    @Test
    public void paleBesideDarkSaturatedIsHarsherThanPaleBesideGray() {
        // Measured tones of sandstone, red terracotta and stone brick (colors.tsv).
        FacadeTone sandstone = FacadeTone.ofLab(83.7f, -1.5f, 26.3f);
        FacadeTone red = FacadeTone.ofLab(36.7f, 29.9f, 29.9f);
        FacadeTone stone = FacadeTone.ofLab(52.0f, 0f, 0f);
        assertTrue(FacadeTone.contrast(sandstone, red) > 45f);
        assertTrue(FacadeTone.contrast(sandstone, stone) < 45f);
    }

    @Test
    public void theWeightedPickMatchesUpstreams() {
        List<StyleView.Choice> group = Arrays
            .asList(new StyleView.Choice("a", 1f), new StyleView.Choice("b", 0f), new StyleView.Choice("c", 3f));
        assertEquals(0, StyleView.weightedIndex(group, 0.0f));
        assertEquals(0, StyleView.weightedIndex(group, 0.25f));
        assertEquals(2, StyleView.weightedIndex(group, 0.26f));
        assertEquals(2, StyleView.weightedIndex(group, 0.999f));
    }
}
