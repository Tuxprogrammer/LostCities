# 0014 · Floor pitch: 8-block building floors by stretching parts

- Status: Accepted (city-realism phase 1b; decision log D-p1b-1 to D-p1b-7)
- Gate: beta
- Amends: the floor height of [docs/original-spec](../original-spec/README.md) buildings (upstream `FLOORHEIGHT` 6) for building floors and cellars only
- Date: 2026-11-06

## Context

Upstream floors are 6 blocks: a floor slab, 3 clear rows and a 2-thick ceiling. Phase 1a measured 22 floor parts (`building1_1..9`, `building2_1..4`, `building3_1..4`, `building4_1..4`, `highway_restaurant_parking`) at a median clear height of 3, which feels cramped. The owner settled: +2 rows per floor, ladders extend, `building4` stair runs lengthen, towers stay under Y 248, and the pitch is pinned per world so existing saves do not change.

## Decision

1. **City levels stay 6; only building floors and cellars use the pitch.** City grounds, highway, rail and ramp levels, damage, `cityLevelHeights`, the profile validator and `HeightCap.CITY_HEADROOM` keep `LEVEL_HEIGHT` 6 (renamed from `FLOOR_HEIGHT` in `WorldLimits` and the layout `ChunkPlan`). Building floors, cellars, ruins, edge-lot columns, the stuff range, the crater floor cap, `oneFloorFits`, the city-finder feather and the floor number of conditions follow the world's `floorPitch`.
2. **Parts stretch at load, lazily.** `Part.stretched(extra, row)` returns a memoized copy with row `row` written `extra` more times (`FloorStretch.stretch`). The cost is paid once per part; pitch-6 worlds place the original, so both pitches can run in one server.
3. **The repeat row** is the row from 1 to `height − 2` with the most spaces, highest on a tie (`FloorStretch.repeatRow`). Overlays (`parts2`) repeat the main part's row so they stay aligned. Ladders and every other vertical run through that row grow with it.
4. **Stair runs are re-laid.** A diagonal run of one character (at least 4 steps, each with a space above) that crosses the repeat row is erased with its support character and re-laid as one run `extra` steps longer, ending at the stretched top step and starting on the same floor row, with the support under the new steps. If it does not fit, the plain stretch stays. This keeps `building4_*` runs joining floor to floor.
5. **What stretches:** floor and cellar parts that are 6 tall; not the roof part (`f == floors`), not scattered structures (`ScatteredAdapter`, `ScatteredContent` keep 6), not the part editor or `CreateBuildingSubcommand`, which edit parts as stored.
6. **Cellars** stretch too (cellar and floor parts are the same parts). The cellar cap against rails and highways converts its room in city levels to blocks (`room * 6 / pitch`), and `lowestCityLevel` rounds the cellar depth up to whole levels (`ChunkPlan.cellarLevels`).
7. **Towers** cap their floors with `LayoutPlanner.capFloors` so the roof base stays under Y 248 (`BUILDING_TOP_CAP`); at pitch 8 a ground of 70 fits 22 floors instead of 29.
8. **The pin.** `floorPitch` is a profile option (6 to 10, default 8). A profile option may declare a *legacy default*, the value a pinned snapshot lacking the key resolves to (`ProfileOptions.legacyDefault(key)`, `ProfileOption.legacyDefault()`); every `ProfileSnapshot` passes its values through `ProfileOptions.withLegacyDefaults(values)`. `floorPitch`'s legacy default is 6, so worlds pinned before this change keep 6-block floors and generate byte-identically; new pins write 8. `TerrainSettings.defaults()` stays at 6 so the existing goldens hold.

## Consequences

- New worlds get 5 clear rows in the measured parts; old worlds are unchanged.
- Towers lose roughly a quarter of their maximum floors.
- Corridor openings into cellars (`groundLevel − 5 .. −3`) sit 2 blocks above an 8-pitch first cellar's floor, leaving a 2-block step down. Not fixed here.
- Later options that must be off in old worlds (density rings, pack houses) declare a legacy default the same way.

## Alternatives considered

- A write-time row map in `PartWriter`: no memory, but a per-column cost on every chunk and every writer path (overlays, jobs, doors) would need it. Rejected.
- Changing `FLOOR_HEIGHT` globally: moves highways, rails and city grounds and breaks every saved world. Rejected.

## Still open

- Pitch-8 golden chunks were not generated in this lane (see the lane report).
- The corridor drop above.
