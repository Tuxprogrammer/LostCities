# Handoff: lane/p1b-pitch (city-realism phase 1b, floor pitch 8)

Stopped because both remote verification rounds were used up (round 1 failed to compile on a shadowed local; round 2 ran 1147 tests with one failure, `ProfileOptionsTest.everyTerrainValueIsAProfileOptionWithTheSameDefault`, which the last commit fixes but no build has checked yet).

## Done

- `FloorStretch` (asset/model) and the memoized `Part.stretched(extra, row)`; wired into `CityChunkWriter.writeBuilding` for floors and cellars (not the roof); `height += plan.floorPitch()`.
- `floorPitch` in both `ChunkPlan`s, `LayoutProfile`, `TerrainSettings` (default 6), and the profile option (default 8, range 6 to 10). Every floor-height use that means a building floor follows the pitch; city levels stay `LEVEL_HEIGHT` 6 (renamed from `FLOOR_HEIGHT`).
- The legacy-default API: `ProfileOptions.legacyDefault(key)` (declare, private; read, public), `ProfileOption.legacyDefault()`, `ProfileOptions.withLegacyDefaults(Map)`, which `ProfileSnapshot`'s constructor applies.
- `LayoutPlanner.capFloors`.
- Tests: `FloorStretchTest` (22 parts with median clear ≥4, vertical runs continuous, building4 runs join floors, memoized), `FloorPitchCapTest`, `ProfileSnapshotTest.aSnapshotWithoutFloorPitchResolvesToTheLegacyPitch`. Existing goldens are unchanged because `TerrainSettings.defaults()` is pitch 6.
- Docs: design record 0014, D-p1b-1 to D-p1b-7, the `docs/config.md` row.

## Left to do

1. Run one build: `tools/remote/lc-remote.sh build p1b HEAD -- spotlessApply build`.
2. Make pitch-8 goldens: add a `CityFixtures` variant with `floorPitch` 8 and a `city_building_p8` golden in `CityGoldenTest`, then run `-PupdateGoldens` remotely and fetch the `.sha256` files back.
3. Run the headless city and network scenarios on `--instance 1`.
4. Add a `floorPitch` row to `docs/new-spec/02-profiles-dimensions/profile-options.md`, list `floorPitch` under "What the snapshot holds" in `pinned-profile.md`, and add a deviation note for the floor height in the new-spec buildings sheet.
5. The corridor openings at `groundLevel − 5 .. −3` leave a 2-block drop into an 8-pitch first cellar (0014, Consequences).
