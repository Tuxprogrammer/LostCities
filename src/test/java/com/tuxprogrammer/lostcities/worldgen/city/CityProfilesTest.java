package com.tuxprogrammer.lostcities.worldgen.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.config.profile.BuiltinProfiles;
import com.tuxprogrammer.lostcities.config.profile.ProfileValues;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkKind;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkTreatment;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainSettings;

/**
 * A non-default profile changes generation as its options say (M09's exit gate): each built-in, pinned into the same
 * test world, against {@code default}.
 */
public class CityProfilesTest {

    static final int RADIUS = 24;

    /** What a profile's layout puts into the region around the origin. */
    static final class Stats {

        int city;
        int buildings;
        int maxFloors;
        int ruined;
        int borderStyle;
    }

    static Stats stats(String profile) {
        LayoutPlanner planner = CityFixtures
            .planner(CityFixtures.SEED, ProfileValues.of(BuiltinProfiles.values(profile)));
        Stats stats = new Stats();
        for (int z = -RADIUS; z <= RADIUS; z++) {
            for (int x = -RADIUS; x <= RADIUS; x++) {
                ChunkPlan plan = planner.plan(x, z);
                if (!plan.isCity()) {
                    continue;
                }
                stats.city++;
                if (plan.cityStyle() != null && plan.cityStyle()
                    .contains("citystyle_border")) {
                    stats.borderStyle++;
                }
                if (plan.hasBuilding()) {
                    stats.buildings++;
                    stats.maxFloors = Math.max(stats.maxFloors, plan.floors());
                    if (plan.ruinHeight() >= 0) {
                        stats.ruined++;
                    }
                }
            }
        }
        return stats;
    }

    @Test
    public void cityDensityFollowsTheProfile() {
        Stats normal = stats("default");
        Stats rare = stats("rarecities");
        Stats only = stats("onlycities");
        assertTrue(normal.city > 0, "the test world has cities");
        assertTrue(rare.city < normal.city, "rarecities: " + rare.city + " vs " + normal.city);
        assertTrue(only.city > normal.city, "onlycities: " + only.city + " vs " + normal.city);
    }

    @Test
    public void tallbuildingsBuildsHigherButNotPastItsCap() {
        Stats normal = stats("default");
        Stats tall = stats("tallbuildings");
        assertTrue(normal.maxFloors <= 8, "default caps at 8 floors, found " + normal.maxFloors);
        assertTrue(tall.maxFloors > 8 && tall.maxFloors <= 19, "tallbuildings found " + tall.maxFloors);
    }

    @Test
    public void nodamageHasNoRuinsAndNoExplosions() {
        assertEquals(0, stats("nodamage").ruined);
        TerrainSettings settings = TerrainSettings.fromValues(BuiltinProfiles.values("nodamage"));
        assertEquals(0f, settings.explosionChance());
        assertEquals(0f, settings.miniExplosionChance());
        assertTrue(stats("ancient").ruined > stats("default").ruined, "ancient ruins most buildings");
    }

    @Test
    public void largecitiesUsesTheBorderStyleBelowItsThreshold() {
        assertEquals(0, stats("default").borderStyle);
        assertTrue(stats("largecities").borderStyle > 0);
    }

    @Test
    public void atlantisRaisesTheWaterLevel() {
        TerrainSettings settings = TerrainSettings.fromValues(BuiltinProfiles.values("atlantis"));
        assertEquals(89, settings.waterLevel());
        assertEquals(
            89,
            CityProfiles.layoutProfile(settings, ProfileValues.of(BuiltinProfiles.values("atlantis")))
                .waterLevel());
    }

    @Test
    public void coexistenceOptionsReachTheLayout() {
        java.util.Map<String, String> values = BuiltinProfiles.values("default");
        values.put("villageTreatment", "AVOID");
        values.put("landmarkTreatment", "IGNORE,ocean_monument=AVOID");
        values.put("villageTransitionRings", "3");
        com.tuxprogrammer.lostcities.layout.LayoutProfile profile = CityProfiles
            .layoutProfile(TerrainSettings.fromValues(values), ProfileValues.of(values));
        assertEquals(LandmarkTreatment.AVOID, profile.treatment(LandmarkKind.VILLAGE));
        assertEquals(LandmarkTreatment.AVOID, profile.treatment(LandmarkKind.OCEAN_MONUMENT));
        assertEquals(LandmarkTreatment.IGNORE, profile.treatment(LandmarkKind.ROGUELIKE));
        assertEquals(3, profile.villageTransitionRings);
    }
}
