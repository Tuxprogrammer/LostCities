# Lane ki-network: handoff

Branch `lane/ki-network`, from `gtnh` fad3f405. Build slot `kinet`, real-pack instance 5.

## Done

- **KI-3, rail gap under a shallow landmark.** The straight line on each side of a core chunk that carries no railway (0013 §5 "No room") now stops on a buffer in the chunk before the gap.
  - Along X it uses the `railshorizontalend` part. Along Z the last 4 rows of the turned part are cleared and a stop block crosses the tracks.
  - Gated by `railwayCoreEnds`: on for new worlds, `legacyDefault` false.
  - Tests: `RailwayLayoutTest` (2 cases) and `ProfileSnapshotTest.aSnapshotWithoutRailwayCoreEndsKeepsTheGap`.
  - Decision row D-kinet-2.
- **KI-2, ramp stripes.** Each whole-step ramp column is topped with a smooth double stone slab.
  - Gated by `rampSurface`: on for new worlds, `TerrainSettings` default false.
  - Test: `NetworkWriterTest.withRampSurfaceAWholeStepColumnMatchesTheSlabs`.
  - Decision row D-kinet-3.
- **Docs.** README known limits, 0013 §2 and §5, `docs/config.md`, and the profile-options sheet.
- **Remote build.** Green at cbd8e5e3: 1277 tests, 0 failures, legacy goldens unchanged. The spotless diff is committed as 930dbe29.
- **Real pack.** Instance 5, classic RWG, seed 777, testHooks, run `20261106-151355-i5`: `city.real`, `network.highway` and `network.railway` all PASS.

## Open

- **KI-5 is not fixed** (D-kinet-1). These reproductions ran on the old 1524eaf6 jar; the server's run directories are under `realpack/runs/<id>`.
  - `20261106-150406-i5`, classic seed 777: highway and railway PASS.
  - `20261106-150610-i5`, classic seed 1: highway FAIL with 207 steps too high.
    - The run is cell(-1,0) to cell(0,0), from HUB@-65,82 at level 1 to HUB@22,76 at level 0, with 1 ramp.
    - The deck alternates 72/73 along x=-1028, z 1219..1300. Cause not found.
    - Next step: dump the pieces and the decks the writer laid for chunks (-65..-64, 76..82), and compare them with `expectedDeck`. The two adjacent chunks may disagree on the deck per axis (0013 §3).
  - `20261106-150725-i5`, RWG2 seed 1: highway FAIL with 2 gaps without ground, at 3760,-941 near Y 101 and at 3772,-929 near Y 96.
  - `20261106-150845-i5`, RWG2 seed 777: railway FAIL with 1 broken boundary at x 479|480. The west side has rails at Y 54 and Y 66; the east side only at Y 54.
    - This is probably a false positive: `NetworkScenarios.railsAt` scans Y 1..199 for every rail, so it also counts a city rail 12 blocks above the line.
    - Fix: limit the scan to the line part's band.
- **Overworld cascade lines.** The run logged 53 ArchaicFix cascade lines in the overworld, during the spawn-area prepare. They carry no stack traces, so the mod that caused them is not shown. The city.real summary reports "cascades during population 48". Attributing them needs a stack-trace run (for example ArchaicFix's trace option), or a comparison with the same seed on `gtnh`.
- **In-game look of the new parts.** The Z-axis buffer and the ramp surface have not been checked by eye. That check is for the owner.
