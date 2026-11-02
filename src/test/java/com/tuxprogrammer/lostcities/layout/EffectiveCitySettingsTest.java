package com.tuxprogrammer.lostcities.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** {@link EffectiveCitySettings}: style values win, profile values fall back, floor values are constraints. */
public class EffectiveCitySettingsTest {

    @Test
    public void styleOverridesTheProfile() {
        LayoutFixtures.FakeCityStyle style = new LayoutFixtures.FakeCityStyle("s", "st").buildingChance(.9f);
        EffectiveCitySettings settings = EffectiveCitySettings.resolve(LayoutProfile.defaults(), style);
        assertEquals(.9f, settings.buildingChance());
        assertEquals(.8f, settings.openLotParkChance(), "profile fallback");
        assertEquals(.05f, settings.fountainChance());
        assertEquals(3, settings.parkStreetThreshold());
    }

    @Test
    public void withoutAStyleOnlyProfileValuesApply() {
        EffectiveCitySettings settings = EffectiveCitySettings.resolve(LayoutProfile.defaults(), null);
        assertEquals(.3f, settings.buildingChance());
        assertEquals(7, settings.constrainMaximumFloors(7), "no constraint");
        assertEquals(2, settings.constrainMaximumCellars(2));
    }
}
