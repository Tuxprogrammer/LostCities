package com.tuxprogrammer.lostcities.worldgen.overlay;

import net.minecraft.util.ChunkCoordinates;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.scenario.Scenario;
import com.tuxprogrammer.lostcities.scenario.ScenarioContext;
import com.tuxprogrammer.lostcities.scenario.ScenarioRegistry;
import com.tuxprogrammer.lostcities.layout.perf.PerfCounters;

/**
 * Diagnostic scenario {@code perf.sites}: generates several fresh 25 x 25 chunk squares spaced about 1000 blocks
 * apart along +X from spawn and logs the generation timers and the {@link PerfCounters} per site as
 * {@code [LCNH-PERF]}. Each site enters new link-graph cells, the way a teleporting or fast-travelling player does.
 */
public final class PerfScenarios {

    static final int SITES = Integer.getInteger("lostcitiesnh.perfSites", 6);
    static final int SITE_RADIUS = Integer.getInteger("lostcitiesnh.perfRadius", 12);
    static final int SITE_SPACING = Integer.getInteger("lostcitiesnh.perfSpacing", 63);

    private PerfScenarios() {}

    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Sites());
    }

    static final class Sites implements Scenario {

        private int site;
        private RegionGenerator generator;
        private long siteNanos;
        private int siteTicks;
        private long planMissesAtStart;
        private int startX;
        private int startZ;
        private boolean done;

        @Override
        public String name() {
            return "perf.sites";
        }

        @Override
        public boolean manualOnly() {
            return true;
        }

        @Override
        public int ticks() {
            return 12000;
        }

        @Override
        public boolean finished(ScenarioContext context) {
            return done;
        }

        @Override
        public void setup(ScenarioContext context) {
            ChunkCoordinates spawn = context.world()
                .getSpawnPoint();
            startX = spawn.posX >> 4;
            startZ = spawn.posZ >> 4;
            LostCities.LOG.info(
                "[LCNH-PERF] {} sites of {} x {} chunks every {} chunks along +X from chunk ({}, {})",
                SITES,
                2 * SITE_RADIUS + 1,
                2 * SITE_RADIUS + 1,
                SITE_SPACING,
                startX,
                startZ);
            beginSite(context);
        }

        private void beginSite(ScenarioContext context) {
            OverlayWorld overlay = Overlays.INSTANCE.get(context.world());
            // Start far enough from spawn that the spawn region generated at boot does not pre-warm the site.
            int x = startX + (site + 1) * SITE_SPACING;
            generator = new RegionGenerator(context.world(), x, startZ, SITE_RADIUS);
            siteNanos = 0;
            siteTicks = 0;
            if (overlay != null) {
                overlay.timers()
                    .reset();
                overlay.prefetcher()
                    .resetStats();
                planMissesAtStart = overlay.planMisses();
            }
            PerfCounters.reset();
            LostCities.LOG.info("[LCNH-PERF] site {} begins at chunk ({}, {})", site, x, startZ);
        }

        @Override
        public void tick(ScenarioContext context) {
            long started = System.nanoTime();
            boolean siteDone = generator.tick();
            siteNanos += System.nanoTime() - started;
            siteTicks++;
            if (!siteDone) {
                return;
            }
            report(context);
            site++;
            if (site >= SITES) {
                done = true;
                LostCities.LOG.info("[LCNH-PERF] done");
                return;
            }
            beginSite(context);
        }

        private void report(ScenarioContext context) {
            OverlayWorld overlay = Overlays.INSTANCE.get(context.world());
            LostCities.LOG.info(
                "[LCNH-PERF] site {}: {} chunks in {} ticks, {} ms of generator time ({} ms per chunk)",
                site,
                generator.size(),
                siteTicks,
                siteNanos / 1_000_000L,
                String.format("%.2f", siteNanos / 1e6 / generator.size()));
            if (overlay != null) {
                for (String line : overlay.timers()
                    .report()) {
                    LostCities.LOG.info("[LCNH-PERF] site {} timer {}", site, line);
                }
                LostCities.LOG.info(
                    "[LCNH-PERF] site {} prefetch {}; plan misses {}",
                    site,
                    overlay.prefetcher()
                        .summary(),
                    overlay.planMisses() - planMissesAtStart);
            }
            for (String line : PerfCounters.report()) {
                LostCities.LOG.info("[LCNH-PERF] site {} counter {}", site, line);
            }
        }

        @Override
        public void verify(ScenarioContext context) {
            context.check(done, "expected all sites generated, at site " + site);
        }
    }
}
