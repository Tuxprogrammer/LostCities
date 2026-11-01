# Highway occupancy: modes, levels and axes

How upstream decides, for one chunk, whether a highway runs through it, along which axis, and at which level. This is the facade every consumer calls; the two algorithms behind it have their own sheets ([legacy-highways.md](legacy-highways.md), [intercity-planner.md](intercity-planner.md)).

| # | Field | Contents |
|---|---|---|
| 1 | **Identity** | `worldgen/lost/Highway` (static facade), `worldgen/highway/HighwayInfo` (result record), `HighwayAxis`, `config/HighwayGenerationMode` (out of scope, read for context), `worldgen/LostCityWorldGenData` (mode persistence, out of scope), `worldgen/plan/ChunkPlanner.highway` (per-chunk cache, out of scope). Profile option `highwayGenerationMode` (`HIGHWAY_GENERATION_MODE`). |
| 2 | **Inputs** | Chunk coordinate (`ChunkCoord`: dimension key, chunk X, chunk Z), the dimension's `IDimensionInfo`, a `LostCityProfile`, the dimension's persisted highway mode. |
| 3 | **Numbers** | See [Numbers](#numbers). Level unit: one city level = `FLOORHEIGHT` = 6 blocks. "No highway" = level -1. |
| 4 | **Behavior** | See [Behavior](#behavior). |
| 5 | **Determinism** | Pure function of seed, coordinates, profile and (intercity) persisted hubs; cached per chunk. See [Determinism](#determinism). |
| 6 | **Data formats** | `LostCityWorldGenData` saved data (mode per dimension). See [Data formats](#data-formats). |
| 7 | **Output** | A `HighwayInfo`: X level, Z level, a classification, and (intercity only) the route hits. |
| 8 | **Relationships** | Consumers listed under [Relationships](#relationships). |
| 9 | **1.20 dependencies** | Dimension identity as `ResourceKey<Level>` strings; Forge `LevelEvent.CreateSpawnPosition` as the new-world signal; `SavedData` in the overworld's `DimensionDataStorage`. |
| 10 | **Bugs** | `HWY-06` (intercity mode ignores `highwayDistanceMask`), `HWY-15` (unbounded static caches). |

## Numbers

| Item | Value | Notes |
|---|---|---|
| Modes | `LEGACY`, `INTERCITY_NETWORK_V1` | Enum names are persisted; renaming one breaks saved worlds (the enum's own comment says so). |
| Profile default `highwayGenerationMode` | `INTERCITY_NETWORK_V1` | Applies only to worlds that are new (see behavior). |
| Level meaning | city level 0, 1, 2, … | Highway ground Y = `groundLevel + level × 6`, where `groundLevel` is the profile's `GROUNDLEVEL` (default 71) or the outside profile's for chunks outside a city sphere. |
| "No highway" | -1 | In both `xLevel` and `zLevel`. |
| Level range, legacy | city levels from `BuildingInfo.getCityLevel` (thresholds `cityLevel0Height`..`cityLevel7Height`, see `04-city-layout`) or `highwayNetworkLevel` | |
| Level range, intercity | same, or `highwayNetworkLevel` 0..32 in mode 4 | Level 32 puts the road at Y 71 + 192 = 263 by default. |

`HighwayInfo` fields:

| Field | Type | Meaning |
|---|---|---|
| `xLevel` | int | Level of the highway running along the X axis (east-west) through this chunk, or -1 |
| `zLevel` | int | Level of the highway running along the Z axis (north-south), or -1 |
| `classification` | enum | `NONE`, `X_HIGHWAY`, `Z_HIGHWAY`, `SAME_LEVEL_INTERSECTION`, `MULTI_LEVEL_INTERSECTION` |
| `routeHits` | list of `RouteHit(route, xAxis, zAxis, bend)` | Intercity only: every route whose segments contain the chunk; empty in legacy mode. `bend` is true when the chunk lies on both an X and a Z segment of the same route. |

`hasHighway()` is true when either level is 0 or more. The constant `HighwayInfo.NONE` is (-1, -1, `NONE`, empty).

Classification rule (both modes): both levels set and equal → `SAME_LEVEL_INTERSECTION`; both set and different → `MULTI_LEVEL_INTERSECTION`; only X → `X_HIGHWAY`; only Z → `Z_HIGHWAY`; neither → `NONE`.

## Behavior

### Selecting the mode for a dimension (server)

1. (server) When the dimension's `DefaultDimensionInfo` is built, it asks `LostCityWorldGenData` (stored in the overworld) for the dimension's highway mode, passing the profile's requested mode.
2. (server) If a mode is already persisted for that dimension ID, it is used as is. An unknown persisted name is logged as an error and treated as `LEGACY`.
3. (server) Otherwise, if the world carries the "new world" marker, the requested mode is chosen and persisted for the dimension.
4. (server) Otherwise (a world that existed before this saved data, or a dimension info built before the marker was set), `LEGACY` is returned and **not** persisted.
5. (server) The marker is set by the Forge `CreateSpawnPosition` event, which only fires for a new world. That handler also bumps a global counter so any dimension info built earlier is discarded and rebuilt, and therefore picks the requested mode.

The GUI preview (`NullDimensionInfo`, area `09-client-ui`) uses the profile's requested mode directly, with no persistence.

### Answering "what highway is in this chunk" (server)

1. (server) `Highway.getHighwayInfo(coord, provider, profile)` delegates to `ChunkPlanner.highway`. When the provider has a world, the answer is cached in that chunk's `ChunkPlan` (a timed cache, `cacheCleanupSeconds`, default 300 s), computed once under a per-plan lock. Without a world (GUI preview), it is computed every time.
2. (server) `computeHighwayInfo` branches on the dimension's mode:
   - **Intercity:** if the provider has a world, the profile is a space or spheres profile, and the chunk intersects a city sphere, the answer is `NONE`. Otherwise it asks the dimension's `HighwayPlanningService` for the chunk ([planning-service-and-persistence.md](planning-service-and-persistence.md)), which returns the planner's `HighwayInfo` ([intercity-planner.md](intercity-planner.md)).
   - **Legacy:** computes the X level and the Z level independently with the noise algorithm ([legacy-highways.md](legacy-highways.md)), then classifies them. `routeHits` is empty.
3. (server) `getXHighwayLevel`, `getZHighwayLevel` and `hasHighway` are thin wrappers over the same result.

Profile options that only the legacy algorithm reads, and that the intercity mode silently ignores: `highwayDistanceMask` (including its "0 disables highways" meaning), `highwayRequiresTwoCities`, `highwayMainPerlinScale`, `highwaySecondaryPerlinScale`, `highwayPerlinFactor`. See `HWY-06`.

### Axes

- An **X highway** runs east-west: it occupies a row of chunks with a constant chunk Z. Its parts are placed unrotated.
- A **Z highway** runs north-south: a column of chunks with a constant chunk X. Its parts are rotated 90°.
- One chunk can carry one level per axis. When several routes cross a chunk on the same axis, the intercity planner keeps only the lowest level ([intercity-planner.md](intercity-planner.md), `HWY-09`).

## Determinism

- Legacy: a pure function of the world seed, the chunk coordinate and the profile, except for the static caches described in its sheet.
- Intercity: a pure function of the world seed, the dimension ID string, the planner settings, the city-potential inputs, and the persisted hub decisions ([planning-service-and-persistence.md](planning-service-and-persistence.md)).
- The per-chunk `ChunkPlan` cache key is the chunk coordinate only; the `profile` argument is not part of it. Every caller in upstream passes the chunk's own profile or the dimension profile, so this matters only in multi-profile (city sphere) dimensions, where the first caller's profile wins.
- The mode itself depends on world history: a world created before the saved data existed stays `LEGACY` forever.

## Data formats

`LostCityWorldGenData` (overworld data storage, name `LostCityWorldGenData`; documented here only for the highway part, owner `03-terrain`, [saved-data.md](../03-terrain/saved-data.md)):

| NBT key | Type | Meaning |
|---|---|---|
| `newWorldHighwayModes` | boolean | The new-world marker for highway modes |
| `newWorldStreetModes` | boolean | The same marker for street modes; both are set together |
| `highwayModes` | compound | One string per dimension ID (`minecraft:overworld`), value `LEGACY` or `INTERCITY_NETWORK_V1` |

Profile option (JSON profile files and the profile config): `highwayGenerationMode`, string, one of the two enum names, default `INTERCITY_NETWORK_V1`. An unknown name throws when the profile loads.

## Relationships

Callers of the facade (all server, during generation unless noted):

| Caller | Use | Area |
|---|---|---|
| `gen/Highways.generateHighways` | Places highway parts | this area, [highway-parts.md](highway-parts.md) |
| `BuildingInfo` constructor | Stores `highwayXLevel`, `highwayZLevel`; caps cellars above highways | `04-city-layout` |
| `BuildingInfo.checkBuildingPossibility` | Allows a building over a highway only if `cityLevel > max highway level + 1` | `04-city-layout` |
| `BuildingInfo.getMaxHeight`, `hasFrontPartFrom` | Height of a highway chunk; no front parts next to highways | `04-city-layout` |
| `LostCityTerrainFeature` city chunk | Skips street decorations on highway chunks; decides `canDoStreetOrPark` | `03-terrain`, `04-city-layout` |
| `gen/Bridges` | No bridge parts where a highway is at level 0 | `04-city-layout` |
| `MultiChunk` | Multibuildings avoid highway chunks | `04-city-layout` |
| `gen/Scattered` | Avoids highway chunks; places highway-attached scattered buildings | `07-scattered` |
| `Railway.getStationType`, `testAdjacentRailChunk` | Pushes stations underground near highways | [railway-layout.md](railway-layout.md) |
| `CommandDebug` | `/lostcities debug` prints the mode, classification, route hits and planner state | `01-core` |
| `GuiLCConfig` preview (client) | Draws highway chunks in the map preview | `09-client-ui` |

## 1.20 dependencies

| Mechanism | Where | Why it matters for 1.7.10 |
|---|---|---|
| Dimension IDs as `ResourceKey<Level>` strings | `LostCityWorldGenData`, `ChunkCoord`, planner `dimensionId` | 1.7.10 identifies dimensions by integer ID. The string also feeds the intercity hash, so the chosen identifier changes the layout. |
| `SavedData` in the overworld `DimensionDataStorage` | `LostCityWorldGenData.get` | 1.7.10 uses `WorldSavedData` and `MapStorage`; a counterpart exists with a different API. |
| Forge `LevelEvent.CreateSpawnPosition` | `ForgeEventHandlers.onCreateSpawnPoint` | 1.7.10 Forge has `WorldEvent.CreateSpawnPosition`; the timing relative to dimension-info creation must be rechecked. |

## Bugs

- `HWY-06`: in `INTERCITY_NETWORK_V1` mode `highwayDistanceMask = 0` no longer disables highways, so profiles that turn highways off (space, biosphere, biosphere caves, void outside) still get them outside city spheres.
- `HWY-15`: the legacy level caches and the rail cache are static and unbounded for a server session.

## Deviations in the new mod

None recorded yet; the new spec decides.
