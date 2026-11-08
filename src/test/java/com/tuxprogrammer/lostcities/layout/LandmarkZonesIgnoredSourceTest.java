package com.tuxprogrammer.lostcities.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.landmark.Landmark;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkKind;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkTreatment;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;
import com.tuxprogrammer.lostcities.worldgen.overlay.StructureOracles;

/** Skipping an IGNOREd oracle changes no zone and no link mask (D-perf2-oracles-1). */
class LandmarkZonesIgnoredSourceTest {

    @Test
    void zonesAndMaskAreIdenticalWithAndWithoutTheIgnoredSource() {
        Landmark village = LayoutFixtures.landmark("village@5,5", LandmarkKind.VILLAGE, 84, 84, 91, 91);
        Landmark witch = LayoutFixtures.landmark("witch@9,9", LandmarkKind.WITCHERY, 100, 100, 120, 120);
        List<StructureOracles.Factory> factories = new ArrayList<>();
        factories.add((w, h, m) -> new LayoutFixtures.FakeOracle(village));
        factories.add((w, h, m) -> new LayoutFixtures.FakeOracle(witch));
        List<Set<LandmarkKind>> kinds = new ArrayList<>();
        kinds.add(EnumSet.of(LandmarkKind.VILLAGE));
        kinds.add(EnumSet.of(LandmarkKind.WITCHERY));
        LayoutProfile profile = LayoutProfile.builder()
            .landmarkTreatment(LandmarkKind.WITCHERY, LandmarkTreatment.IGNORE)
            .build();

        StructureOracle full = StructureOracles.compose(StructureOracles.build(null, null, 2, factories));
        StructureOracle skipped = StructureOracles.compose(
            StructureOracles.build(null, null, 2, StructureOracles.activeFactories(factories, kinds, profile)));
        LandmarkZones a = new LandmarkZones(full, profile, 4096);
        LandmarkZones b = new LandmarkZones(skipped, profile, 4096);
        for (int x = -2; x < 16; x++) {
            for (int z = -2; z < 16; z++) {
                assertEquals(
                    a.zone(x, z)
                        .zone(),
                    b.zone(x, z)
                        .zone(),
                    x + "," + z);
                assertEquals(a.blocksLinks(x, z), b.blocksLinks(x, z), "mask " + x + "," + z);
            }
        }
    }
}
