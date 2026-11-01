# Street planner (hierarchical grid)

The `HIERARCHICAL_GRID_V1` street mode lays a pure, order-independent road field over the whole dimension: primary roads on a jittered grid, secondary roads inside each block, and short tertiary dead-end access roads. The field knows nothing about cities; it is clipped to the city afterwards. The older `LEGACY` mode has no planner: every city chunk without a building is street or park. Source: `worldgen/street/*` (9 files), `BuildingInfo.getEffectivePlannedRoadType`, `config/StreetGenerationMode.java`, `config/MultiBuildingStreetConflict.java`, `worldgen/LostCityWorldGenData.java` (persistence, out of scope, read for behavior), on `f18a094d`.

## 1. Identity

| Class | Role |
|---|---|
| `HierarchicalStreetPlanner` | The raw road field: `getRoadType`, `getStreetInfo`, `getBlockLayout`, `isVerticalPrimary`, `isHorizontalPrimary` |
| `StreetPlannerSettings` | Validated settings record built from the profile |
| `PlannedRoadType` | `NONE`, `TERTIARY`, `SECONDARY`, `PRIMARY`; `strongest` picks the higher ordinal |
| `PlannedStreetInfo` | Road type, four neighbor-connection flags, block bounds, density, secondary positions, tertiary segment |
| `TertiaryRoadSegment` | `(id, originX, originZ, direction, length)`; `contains` |
| `RoadDirection` | `NORTH (0,-1)`, `SOUTH (0,1)`, `WEST (-1,0)`, `EAST (1,0)` |
| `EffectiveStreetResolver` | Final clip: road only if city, connected and not overridden |
| `PlannedBridgeInfo` | Bridge span record ([bridges.md](bridges.md)) |
| `StreetGenerationMode` | `LEGACY`, `HIERARCHICAL_GRID_V1`; persisted per dimension |

Profile options (category `lostcity`): `streetGenerationMode`, `primaryRoadSpacingX/Z`, `primaryRoadOptionalChance`, `primaryRoadForceEvery`, `secondaryRoadMin/MaxCountX/Z`, `minimumRoadSeparation`, `minimumRoadEdgeDistance`, `tertiaryRoadChance`, `tertiaryRoadMin/MaxLength`, `multiBuildingStreetConflict`. City-style street settings choose the street parts per road class (`05-assets`, [city-settings.md](city-settings.md)).

## 2. Inputs

World seed, dimension id string (for example `minecraft:overworld`), the main profile's settings. Nothing else: no terrain, biome or city.

## 3. Numbers

| Option | Default | Profile range | Settings check | Unit |
|---|---|---|---|---|
| `STREET_GENERATION_MODE` | `HIERARCHICAL_GRID_V1` | `LEGACY`, `HIERARCHICAL_GRID_V1` | | |
| `PRIMARY_ROAD_SPACING_X/Z` | 8 / 8 | 8..128 | >= 8 | chunks between primary candidates |
| `PRIMARY_ROAD_OPTIONAL_CHANCE` | 0.45 | 0..1 | 0..1 | chance an unforced candidate is active |
| `PRIMARY_ROAD_FORCE_EVERY` | 4 | 1..16 | 1..16 | every Nth candidate is always active |
| `SECONDARY_ROAD_MIN/MAX_COUNT_X` | 0 / 2 | min >= 0, max <= 128, min <= max | | roads per block |
| `SECONDARY_ROAD_MIN/MAX_COUNT_Z` | 0 / 2 | same | | |
| `MINIMUM_ROAD_SEPARATION` | 4 | 2..32 | >= 2 | chunks between secondary roads |
| `MINIMUM_ROAD_EDGE_DISTANCE` | 3 | 2..32 | >= 2 | chunks from the block edge |
| `TERTIARY_ROAD_CHANCE` | 0.40 | 0..1 | 0..1 | per cell |
| `TERTIARY_ROAD_MIN_LENGTH` | 2 | 1..16 (config) | >= 1, <= max | chunks |
| `TERTIARY_ROAD_MAX_LENGTH` | 5 | 1..32 | | chunks |
| `MULTI_BUILDING_STREET_CONFLICT` | `OVERRIDE_MINOR` | `BLOCK_ALL`, `OVERRIDE_MINOR`, `OVERRIDE_ALL` | | |

Hash constants (all 64-bit): `VERSION_SALT 0x4847524944563101` ("HGRIDV1" + version byte), `PRIMARY_X_SALT 0x5f6c1d8a29e34b71`, `PRIMARY_Z_SALT 0x731ab9654ce20f8d`, `PRIMARY_X_ACTIVATION_SALT 0x2c64f8a10d735be9`, `PRIMARY_Z_ACTIVATION_SALT 0x58e307c4b92a6df1`, `DENSITY_SALT 0x26f40b9157acde31`, `SECONDARY_X_COUNT_SALT 0x18d2ca7645bf903e`, `SECONDARY_Z_COUNT_SALT 0x47a90edb1c6352f8`, `SECONDARY_X_POSITION_SALT 0x60b54df1298cae37`, `SECONDARY_Z_POSITION_SALT 0x39e71ac405d82bf6`, `TERTIARY_CHANCE_SALT 0x7b21c0e49fa3568d`, `TERTIARY_SIDE_SALT 0x14d8f72ca963be50`, `TERTIARY_ORIGIN_SALT 0x52ae901bd4763cf8`, `TERTIARY_LENGTH_SALT 0x6c03f1a984d27be5`. Mixing constants: `0x9e3779b97f4a7c15`, `0xc2b2ae3d27d4eb4f`, `0x165667b19e3779f9` (per-argument offsets); `mix64` multipliers `0xbf58476d1ce4e5b9`, `0x94d049bb133111eb` with shifts 30, 27, 31; FNV-1a 64 basis `0xcbf29ce484222325`, prime `0x100000001b3`. The tertiary cell key is `cellX * 257 + cellZ`.

## 4. Behavior

All (server). Every function is pure.

### Hashing

1. `dimensionSalt = mix64(FNV-1a-64 of the dimension id's UTF-16 chars)`.
2. `hash(salt, x, z, extra) = mix64(seed ^ dimensionSalt ^ VERSION_SALT ^ salt ^ mix64(u(x) + C1) ^ mix64(u(z) + C2) ^ mix64(u(extra) + C3))`, where `u` widens an `int` to an unsigned `long` and `C1..C3` are the three offsets above.
3. `unitDouble(h) = (h >>> 11) * 2^-53` (in `[0, 1)`); `floorModHash(h, m) = floorMod(h, m)` on the signed `long`.

### Primary roads

1. `primaryOffsetX = floorModHash(hash(PRIMARY_X_SALT, 0, 0, 0), spacingX)`; Z likewise. Per world and dimension.
2. Candidate index for a chunk X: `floorDiv(chunkX - offsetX, spacingX)`. A chunk is on a candidate line when `floorMod(chunkX - offsetX, spacingX) == 0`.
3. A candidate `k` is active when `floorMod(k, forceEvery) == 0`, or else when `unitDouble(hash(ACTIVATION_SALT, k, 0, 0)) < optionalChance` (X axis; the Z axis hashes `(0, k, 0)` with its own salt).
4. `isVerticalPrimary(chunkX)`: on a candidate line and that candidate is active. `isHorizontalPrimary(chunkZ)` likewise on Z. Primary roads run the full length of the dimension.

### Blocks

`getBlockLayout(chunkX, chunkZ)`:
1. On each axis, `blockX` = the nearest active candidate at or before the chunk's candidate index (searching back at most `forceEvery - 1`); `nextBlockX` = the first active candidate after it (searching at most `forceEvery`). A failed search throws `"No forced primary candidate found"` (unreachable with valid settings).
2. `westX = offsetX + blockX * spacingX`, `eastX = offsetX + nextBlockX * spacingX`; likewise `northZ`, `southZ`. `spacingX = eastX - westX`.
3. `density = unitDouble(hash(DENSITY_SALT, blockX, blockZ, 0))`.
4. Secondary counts per axis: if min == max, that value; else `variedDensity = density * 0.75 + unitDouble(hash(COUNT_SALT, blockX, blockZ, 0)) * 0.25` and `count = min(max, min + floor(variedDensity * (max - min + 1)))`.
5. Secondary positions per axis: local offsets `edge .. spacing - edge` (none if empty). Sort them by `hash(POSITION_SALT, blockX, blockZ, local)` as unsigned, then by value. Walk that order and keep an offset if it is at least `minimumRoadSeparation` from every kept one, until `count` are kept. Sort the kept offsets ascending and add `westX` (or `northZ`).

Secondary positions depend on both block indices, so a secondary road ends at the block's primary roads.

### Tertiary roads

For a chunk inside a cell (strictly between two consecutive roads on each axis, counting the block's primaries and secondaries):
1. Cell indices `cellX`, `cellZ`; bounds `x0 < x1`, `z0 < z1`. `identity = hash(TERTIARY_CHANCE_SALT, blockX, blockZ, cellX * 257 + cellZ)`. No road if `unitDouble(identity) >= tertiaryChance`.
2. Direction: start at index `floorModHash(hash(SIDE_SALT, …), 4)` in `NORTH, SOUTH, WEST, EAST` and walk forward cyclically. Take the first direction where the transverse span (`x1 - x0` for north/south, `z1 - z0` for west/east) is at least 4 and the inward span minus 2 is at least `minLength`. None: no road.
3. `maxLength = min(maxLength, inwardSpan - 2)`. `length = minLength + floorModHash(hash(LENGTH_SALT, …), maxLength - minLength + 1)`.
4. Origin on the boundary road the segment leaves from: `SOUTH` starts on `z0`, `NORTH` on `z1`, `EAST` on `x0`, `WEST` on `x1`. The transverse coordinate is `min + floorModHash(hash(ORIGIN_SALT, …), max - min + 1)` within `[x0 + 2, x1 - 2]` (or `z`).
5. The segment covers chunks at distance `1 .. length` from the origin along the direction. It always ends at least one chunk short of the far road: a dead end.

### Raw road type of a chunk (`getRoadType`)

1. On an active vertical or horizontal primary line: `PRIMARY`.
2. Else on one of its block's secondary X or Z positions: `SECONDARY`.
3. Else inside its cell's tertiary segment: `TERTIARY`.
4. Else `NONE`.

`getStreetInfo` adds the block data and four flags: a direction is connected when this chunk and the neighbor in that direction both have a raw road of any class.

### Clip to the city ([chunk-characteristics.md](chunk-characteristics.md))

The effective road type is the raw type only when the chunk is city, at least one connected neighbor is raw city with the same profile, and no predefined building, predefined street or multibuilding occupies the chunk; otherwise `NONE`. In the `BuildingInfo`, a chunk keeps its planned road only if it is city without a building.

### Mode selection and persistence (server)

`DefaultDimensionInfo` asks `LostCityWorldGenData` (saved data `LostCityWorldGenData` in the overworld's data storage) for the dimension's mode:
1. A persisted mode for the dimension id wins.
2. Otherwise, if the world was marked new at creation (`newWorldStreetModes`), use the profile's `streetGenerationMode` and persist it.
3. Otherwise (an existing world without the data) use `LEGACY`, unpersisted.

NBT: `newWorldStreetModes` (boolean), `streetModes` (compound: dimension id → mode name). Unknown persisted names fall back to `LEGACY` with an error log.

## 5. Determinism

Pure hashing of seed, dimension id and coordinates; no `Random`, no caches, no order dependence. The planner is built once per `DefaultDimensionInfo` from the main profile; the GUI preview builds one with the overworld id.

## 6. Data formats

Profile keys above (`02-profiles-dimensions`). Saved data keys above. City-style `streets` settings (`parts`, `largeparts`, `tertiaryparts`) choose parts per class (`05-assets`).

## 7. Output

`PlannedRoadType` per chunk (raw and effective), consumed by [chunk-characteristics.md](chunk-characteristics.md), [multibuildings.md](multibuildings.md), [bridges.md](bridges.md) and street rendering (`03-terrain`).

## 8. Relationships

- Called by: `computeChunkCharacteristics`, `MultiChunk.canPlaceBuilding`, `HierarchicalBridgePlanner`, street rendering and slopes ([streets-parks-stairs.md](streets-parks-stairs.md)), `CommandDebug` (`01-core`).
- Calls: nothing outside the package.

## 9. 1.20 dependencies

| Mechanism | Where | Why it matters for 1.7.10 |
|---|---|---|
| Namespaced dimension id string hashed into every road | `dimensionSalt` | 1.7.10 dimensions are integer IDs; the hash input must be defined |
| Java 16+ API `List.of()`, `Stream.toList()`, records | `selectSecondaryPositions`, all records | Not on Java 8 |
| `SavedData` in the overworld data storage | mode persistence | `WorldSavedData` exists in 1.7.10 |

## 10. Bugs

None found in the planner itself. Clip-related: CITY-20 (provisional characteristics).

## Deviations in the new mod

None recorded yet.
