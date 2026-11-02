package com.tuxprogrammer.lostcities.worldgen.overlay;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.WorldServer;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.config.profile.ProfileValues;
import com.tuxprogrammer.lostcities.layout.ChunkPlan;
import com.tuxprogrammer.lostcities.layout.LayoutPlanner;
import com.tuxprogrammer.lostcities.layout.LayoutProfile;
import com.tuxprogrammer.lostcities.scenario.Scenario;
import com.tuxprogrammer.lostcities.scenario.ScenarioContext;
import com.tuxprogrammer.lostcities.scenario.ScenarioRegistry;
import com.tuxprogrammer.lostcities.worldgen.city.CityProfiles;
import com.tuxprogrammer.lostcities.worldgen.city.SpawnSearch;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainSettings;

/**
 * The profile system's in-world scenarios (M09; docs/testing/README.md), on whatever profile the world pinned:
 * <ul>
 * <li>{@code profile.effects}: plans {@link #REGION} × {@link #REGION} chunks around the spawn and checks that the
 * pinned profile governs them: the world's layout and terrain read the pinned values; with both explosion chances 0 no
 * chunk is damaged; with {@code ruinChance} 0 no building is ruined; no building has more floors than
 * {@code buildingMaxFloors}. It prints {@code [LCNH-PROFILE]} with the region's statistics, so runs of two profiles
 * on one seed can be compared;</li>
 * <li>{@code profile.spawn}: when the pinned profile asks for a spawn, the world's spawn is the chosen one and, for the
 * building options, it lies in a building chunk with standing room; otherwise no spawn was chosen.</li>
 * </ul>
 * Both read the plan only; neither generates a chunk except {@code profile.spawn} reading the spawn chunk, which the
 * server keeps loaded.
 *
 * <p>
 * Side: server.
 */
public final class ProfileScenarios {

    /** Side of the planned square, in chunks. */
    static final int REGION = 48;

    private ProfileScenarios() {}

    /**
     * Adds the scenarios to a registry.
     *
     * @param registry where to add them
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Effects());
        registry.register(new Spawn());
    }

    private static OverlayWorld requireCities(ScenarioContext context) {
        OverlayWorld overlay = Overlays.INSTANCE.get(context.world());
        context.check(overlay != null && overlay.cities() != null, "expected cities in the overworld, found none");
        return overlay;
    }

    /** Checks that the pinned profile governs the plan. */
    static final class Effects implements Scenario {

        @Override
        public String name() {
            return "profile.effects";
        }

        @Override
        public void setup(ScenarioContext context) {
            requireCities(context);
        }

        @Override
        public void verify(ScenarioContext context) {
            OverlayWorld overlay = requireCities(context);
            ProfileValues values = overlay.snapshot()
                .profileValues();
            TerrainSettings settings = overlay.settings();
            LayoutPlanner planner = overlay.cities()
                .planner();
            LayoutProfile expected = CityProfiles.layoutProfile(settings, values);
            LayoutProfile actual = planner.profile();
            context.check(
                actual.cityChance == expected.cityChance && actual.buildingMaxFloors == expected.buildingMaxFloors
                    && actual.buildingChance == expected.buildingChance
                    && actual.cityStyleThreshold == expected.cityStyleThreshold
                    && actual.waterLevel() == settings.waterLevel(),
                "expected the layout to read the pinned profile, found cityChance " + actual.cityChance
                    + " (pinned "
                    + expected.cityChance
                    + ")");
            context.check(
                settings.waterLevel() == (values.getInt("seaLevel") < 0 ? 63 : values.getInt("seaLevel")),
                "expected the water level the profile pins, found " + settings.waterLevel());
            ChunkCoordinates spawn = context.world()
                .getSpawnPoint();
            int minX = (spawn.posX >> 4) - REGION / 2;
            int minZ = (spawn.posZ >> 4) - REGION / 2;
            int city = 0;
            int buildings = 0;
            int maxFloors = 0;
            int ruined = 0;
            int damaged = 0;
            Map<String, Integer> styles = new TreeMap<>();
            for (int cz = minZ; cz < minZ + REGION; cz++) {
                for (int cx = minX; cx < minX + REGION; cx++) {
                    ChunkPlan plan = planner.plan(cx, cz);
                    if (overlay.damaged(cx, cz)) {
                        damaged++;
                    }
                    if (!plan.isCity()) {
                        continue;
                    }
                    city++;
                    styles.merge(plan.cityStyle(), 1, Integer::sum);
                    if (plan.hasBuilding()) {
                        buildings++;
                        maxFloors = Math.max(maxFloors, plan.floors());
                        if (plan.ruinHeight() >= 0) {
                            ruined++;
                        }
                    }
                }
            }
            LostCities.LOG.info(
                String.format(
                    Locale.ROOT,
                    "[LCNH-PROFILE] profile=%s seed=%d region=%dx%d cityChunks=%d (%.1f%%) buildings=%d maxFloors=%d"
                        + " ruined=%d damagedChunks=%d waterLevel=%d styles=%s",
                    overlay.snapshot()
                        .profile(),
                    context.world()
                        .getSeed(),
                    REGION,
                    REGION,
                    city,
                    100.0 * city / (REGION * REGION),
                    buildings,
                    maxFloors,
                    ruined,
                    damaged,
                    settings.waterLevel(),
                    styles));
            if (settings.explosionChance() == 0 && settings.miniExplosionChance() == 0) {
                context
                    .check(damaged == 0, "expected no damaged chunk with both explosion chances 0, found " + damaged);
            }
            if (settings.ruinChance() == 0) {
                context.check(ruined == 0, "expected no ruined building with ruinChance 0, found " + ruined);
            }
            context.check(
                maxFloors <= values.getInt("buildingMaxFloors"),
                "expected at most buildingMaxFloors=" + values.getInt("buildingMaxFloors")
                    + " floors, found "
                    + maxFloors);
        }
    }

    /** Checks the spawn the profile's spawn options chose. */
    static final class Spawn implements Scenario {

        @Override
        public String name() {
            return "profile.spawn";
        }

        @Override
        public void setup(ScenarioContext context) {
            requireCities(context);
        }

        @Override
        public void verify(ScenarioContext context) {
            OverlayWorld overlay = requireCities(context);
            WorldServer world = context.world();
            SpawnSearch.Options options = SpawnSearch.Options.of(
                overlay.snapshot()
                    .profileValues());
            int[] chosen = ProfileSavedData.get(world)
                .spawn(0);
            ChunkCoordinates spawn = world.getSpawnPoint();
            LostCities.LOG.info(
                "[LCNH-SPAWN] profile={} search={} chosen={} worldSpawn={},{},{}",
                overlay.snapshot()
                    .profile(),
                options.needsSearch(),
                chosen == null ? "none" : chosen[0] + "," + chosen[1] + "," + chosen[2],
                spawn.posX,
                spawn.posY,
                spawn.posZ);
            if (!options.needsSearch()) {
                context.check(chosen == null, "expected no chosen spawn without spawn options, found one");
                return;
            }
            context.check(chosen != null, "expected a chosen spawn for the profile's spawn options, found none");
            context.check(
                spawn.posX == chosen[0] && spawn.posZ == chosen[2],
                "expected the world spawn at the chosen " + chosen[0]
                    + ","
                    + chosen[2]
                    + ", found "
                    + spawn.posX
                    + ","
                    + spawn.posZ);
            ChunkPlan plan = overlay.cities()
                .planner()
                .plan(chosen[0] >> 4, chosen[2] >> 4);
            if (options.placesExactly()) {
                context.check(
                    plan.isCity() && plan.hasBuilding(),
                    "expected the spawn in a building chunk, found " + plan);
                int[] room = SpawnSelection.standingRoom(world, chosen);
                context.check(
                    world.isAirBlock(room[0], room[1], room[2]) && world.isAirBlock(room[0], room[1] + 1, room[2])
                        && !world.isAirBlock(room[0], room[1] - 1, room[2]),
                    "expected standing room in the spawn building, found none near " + chosen[1]);
            }
        }
    }
}
