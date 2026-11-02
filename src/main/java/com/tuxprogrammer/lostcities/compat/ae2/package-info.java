/**
 * The AE2 meteorite structure oracle (ROADMAP M08; docs/design/0006-build-around-other-worldgen.md §1 to §3): where
 * Applied Energistics 2 drops a meteorite, how big it is, and the crater basin the city steps down toward, predicted
 * without generating anything.
 *
 * <p>
 * Entry point: {@link com.tuxprogrammer.lostcities.compat.ae2.Ae2Oracles#forWorld}. The grid draws
 * ({@code MeteoriteGrid}), the meteorite's shape ({@code MeteoriteShape}), the basin ({@code CraterBasin}), the
 * settings and the oracle are plain Java, tested without AE2. {@code Ae2Config} and the scenario touch AE2 and load
 * only when it is present. AE2 is a {@code compileOnly} dependency at the version GTNH 2.9.0 ships,
 * rv3-beta-1095-GTNH.
 *
 * <p>
 * Spec: docs/new-spec/00-pack-integration/coexistence.md § AE2 meteorites. Side: server.
 */
package com.tuxprogrammer.lostcities.compat.ae2;
