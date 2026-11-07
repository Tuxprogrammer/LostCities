# 0014 · Floor pitch: 8-block building floors by stretching parts

- Status: Accepted (city-realism phase 1b; decision log D289 to D295)
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
9. **Per-building heights** (D-floor-heights-1). A building JSON may set `floorHeight` (every floor above the ground floor, and every cellar) and `groundFloorHeight` (the ground floor only; defaults to `floorHeight`), each 4 to 16; unset fields take the profile pitch (`layout.FloorHeights`). The ground floor's slab stays at the city ground. Every floor position goes through the building's heights: `ChunkPlan.floorY(f)`, the top (`maxHeight`), the bottom, cellar levels, ruins, `capFloors` and zone height caps, the stuff range, city-finder results, roof-door steps, corridor drops, job targets and the furnisher's standing levels. Multibuilding chunks copy the top-left chunk's heights; adjacent roofs join only where slab Ys match. A floor lower than 6 drops rows instead of repeating one (`FloorStretch.dropRows`, `Part.squeezed`): the row from 1 to `height − 2` with the most spaces goes first, avoiding rows that hold a step of a stair run, and a part never goes below a slab, one clear row and a ceiling. Overlays drop the same rows. Construction lots and synthetic test buildings keep the pitch. Upstream buildings set neither field and are unchanged. The mod's houses (`nh_cottage`, `nh_house_garden`, `nh_walkup`, `nh_corner_shop`) use 5 (3 clear rows); `nh_workshop` uses a 7-block high bay under 5-block floors.
10. **Stair runs across floors** (D-stair-xfloor-1). A run that leaves the top row of one floor part and goes on at the bottom of the part above (upstream's escalators, such as `shopping00`'s quartz run from floor 0 to the floor-2 mezzanine) is one run. `CityChunkWriter` lays a building's cellar, floor and roof parts out as one stack (`Part.stacked`, `FloorStretch.stack`, memoized per stack): runs are found over the whole stack, a step on a part's top row counting as open above as before, and a run is re-laid whole, ending at its top step and starting as many blocks further back as every part it crosses added rows. Laid out one part at a time, the upper half started `extra` blocks back while the lower half kept its top step, leaving a gap at the floor slab. Runs inside one part come out as before; a run with a dropped row (floors under 6) keeps the plain copy. A stack with a missing part or parts of different sizes falls back to one part at a time. Overlays (`parts2`) keep their per-part stretch. Every cross-floor run fits up to pitch 8; at pitch 9 and 10 the `shopping00` and `shopping_open00` escalators would need 17 or more rows over 15 columns, so they keep the plain copy (a broken run) until a ladder or landing fallback exists (`StairContinuityTest` pins that list).
11. **The shopping mall** (D-mall-1). Its ground storey is two parts tall (`shopping*_0` and `_1`: a 12-block atrium with the quartz escalator from the ground to the mezzanine), so a building JSON may set `groundFloors` (1 to 4, default 1): that many floors from the ground up take `groundFloorHeight`. The eight `shopping` and `shopping_open` buildings set `groundFloorHeight` 6 and `groundFloors` 2: the atrium, escalator, supports and windows stay as upstream has them, and the mezzanine and every floor above take the pitch. A part may pin its repeat row with the meta key `stretchrow` (an integer, 1 to `height − 2`; ours, not upstream's); the mall's `_2` and `_r1` parts pin row 3, their window band, so all four quarters of a floor stretch alike and the windows grow from 1 to 3 blocks tall.

## Consequences

- New worlds get 5 clear rows in the measured parts; old worlds are unchanged.
- Towers lose roughly a quarter of their maximum floors.
- Corridor openings into cellars (`groundLevel − 5 .. −3`) sit 2 blocks above an 8-pitch first cellar's floor; the opening becomes a staircase of 1-block steps down to it (D296).
- Later options that must be off in old worlds (density rings, pack houses) declare a legacy default the same way.
- Per-building heights keep towers at the pitch while houses get 3-clear rooms; a multiblock too tall for a lowered room fails its fit check and is skipped. `StairContinuityTest` checks every 2+-step stair run of every floor part at stretches +1 to +4 and at each building's own drops.

## Alternatives considered

- A write-time row map in `PartWriter`: no memory, but a per-column cost on every chunk and every writer path (overlays, jobs, doors) would need it. Rejected.
- Changing `FLOOR_HEIGHT` globally: moves highways, rails and city grounds and breaks every saved world. Rejected.

## Still open

- Pitch-8 golden chunks were not generated in this lane (see the lane report).
- The corridor drop above.
- Cross-floor stair runs that cannot fit at pitch 9 and 10 (the two mall escalators) have no fallback yet; a ladder needs a south-facing ladder character in the building palettes (D-stair-xfloor-1).
