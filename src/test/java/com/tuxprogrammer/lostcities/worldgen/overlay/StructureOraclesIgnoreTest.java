package com.tuxprogrammer.lostcities.worldgen.overlay;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.layout.LayoutFixtures;
import com.tuxprogrammer.lostcities.layout.LayoutProfile;
import com.tuxprogrammer.lostcities.layout.landmark.Landmark;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkKind;
import com.tuxprogrammer.lostcities.layout.landmark.LandmarkTreatment;
import com.tuxprogrammer.lostcities.layout.landmark.StructureOracle;

/** Oracles whose landmark kind the profile ignores are never built or asked (D-perf2-oracles-1). */
class StructureOraclesIgnoreTest {

    private static final Landmark VILLAGE = LayoutFixtures
        .landmark("village@5,5", LandmarkKind.VILLAGE, 84, 84, 91, 91);
    private static final Landmark WITCH = LayoutFixtures
        .landmark("witch@9,9", LandmarkKind.WITCHERY, 150, 150, 160, 160);

    private final AtomicInteger villageCalls = new AtomicInteger();
    private final AtomicInteger witchCalls = new AtomicInteger();

    private List<StructureOracles.Factory> factories() {
        List<StructureOracles.Factory> f = new ArrayList<>();
        f.add((w, h, m) -> (a, b, c, d) -> {
            villageCalls.incrementAndGet();
            return Collections.singletonList(VILLAGE);
        });
        f.add((w, h, m) -> (a, b, c, d) -> {
            witchCalls.incrementAndGet();
            return Collections.singletonList(WITCH);
        });
        return f;
    }

    private static List<Set<LandmarkKind>> kinds() {
        List<Set<LandmarkKind>> k = new ArrayList<>();
        k.add(EnumSet.of(LandmarkKind.VILLAGE));
        k.add(EnumSet.of(LandmarkKind.WITCHERY));
        return k;
    }

    private static LayoutProfile witcheryProfile(LandmarkTreatment t) {
        return LayoutProfile.builder()
            .landmarkTreatment(LandmarkKind.WITCHERY, t)
            .build();
    }

    private StructureOracle oracle(LayoutProfile profile) {
        List<StructureOracles.Factory> active = StructureOracles.activeFactories(factories(), kinds(), profile);
        return StructureOracles.compose(StructureOracles.build(null, null, 2, active));
    }

    @Test
    void ignoredSourceIsNeverCalled() {
        oracle(witcheryProfile(LandmarkTreatment.IGNORE)).landmarksTouching(0, 0, 20, 20);
        assertEquals(1, villageCalls.get());
        assertEquals(0, witchCalls.get());
    }

    @Test
    void optedInSourceIsCalled() {
        oracle(witcheryProfile(LandmarkTreatment.BUILD_AROUND)).landmarksTouching(0, 0, 20, 20);
        oracle(witcheryProfile(LandmarkTreatment.AVOID)).landmarksTouching(0, 0, 20, 20);
        assertEquals(2, witchCalls.get());
    }

    @Test
    void factoryKindsMatchFactories() {
        assertEquals(
            StructureOracles.factories()
                .size(),
            StructureOracles.factoryKinds()
                .size());
    }
}
