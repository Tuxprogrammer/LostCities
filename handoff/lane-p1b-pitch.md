# Handoff: lane/p1b-pitch (city-realism phase 1b, floor pitch 8)

Stopped because both remote verification rounds were used up (round 1 failed to compile on a shadowed local; round 2 ran 1147 tests with one failure, `ProfileOptionsTest.everyTerrainValueIsAProfileOptionWithTheSameDefault`, which the last commit fixes but no build has checked yet).

## Done

- `FloorStretch` (asset/model) and the memoized `Part.stretched(extra, row)`; wired into `CityChunkWriter.writeBuilding` for floors and cellars (not the roof); `height += plan.floorPitch()`.
- `floorPitch` in both `ChunkPlan`s, `LayoutProfile`, `TerrainSettings` (default 6), and the profile option (default 8, range 6 to 10). Every floor-height use that means a building floor follows the pitch; city levels stay `LEVEL_HEIGHT` 6 (renamed from `FLOOR_HEIGHT`).
- The legacy-default API: `ProfileOptions.legacyDefault(key)` (declare, private; read, public), `ProfileOption.legacyDefault()`, `ProfileOptions.withLegacyDefaults(Map)`, which `ProfileSnapshot`'s constructor applies.
- `LayoutPlanner.capFloors`.
- Tests: `FloorStretchTest` (22 parts with median clear ≥4, vertical runs continuous, building4 runs join floors, memoized), `FloorPitchCapTest`, `ProfileSnapshotTest.aSnapshotWithoutFloorPitchResolvesToTheLegacyPitch`. Existing goldens are unchanged because `TerrainSettings.defaults()` is pitch 6.
- Docs: design record 0014, D-p1b-1 to D-p1b-7, the `docs/config.md` row.

## Finished (2026-11-06, second session)

1. Remote builds on slot p1b: `spotlessApply build -PupdateGoldens` and `spotlessCheck build` both green, 1152 tests, 0 failures.
2. `CityFixtures.planAtPitch(int)` and the `city_building_p8` golden; every existing golden hash unchanged.
3. Real-pack scenarios on `--instance 1` (RWG2_CONTINENTS, seed 4242, run 20261106-122422-i1): city.real, city.walk, city.sewing, network.highway, network.railway all PASS.
4. Docs: `floorPitch` rows in profile-options.md and pinned-profile.md, floor-height and corridor rows in new-spec 04-city-layout.
5. Corridor openings into taller cellars become 1-block steps (`CorridorSteps`, D-p1b-8, `CorridorStepsTest`).

Ready for the integrator.
