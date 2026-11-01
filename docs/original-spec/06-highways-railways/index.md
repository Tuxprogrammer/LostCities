# 06 · Highways and railways

## Overview

This area documents upstream Lost Cities 1.20 at `f18a094d` (mod version 1.20-7.5.7): the highways that link cities and the railways, stations, rail dungeons and monorails that run under and between them. It covers both highway algorithms (the original noise-based `LEGACY` layout and the default `INTERCITY_NETWORK_V1` hub-and-route planner), the planner's threading, caches and saved data, how highway and railway parts are chosen and written into a chunk, the fixed railway grid with its stations and descents, and the monorails of `space` profiles. Field formats of the part-selector assets belong to `05-assets`; this area documents how they are used.

## Contents

- [highway-occupancy.md](highway-occupancy.md): which chunks carry a highway, on which axis and level; mode selection and persistence; every consumer
- [legacy-highways.md](legacy-highways.md): the `LEGACY` noise-driven straight highways
- [intercity-planner.md](intercity-planner.md): the `INTERCITY_NETWORK_V1` planner end to end: cells, hubs, candidates, selection, acceptance, ownership, route geometry, chunk query, hashing, caches
- [city-potential.md](city-potential.md): the remote city-potential estimate behind hubs and route penalties, its height and biome modifier, and the hub city level
- [planning-service-and-persistence.md](planning-service-and-persistence.md): the per-dimension thread pool, future caches, and `LostCityHighwayData` hub persistence and its signature
- [highway-parts.md](highway-parts.md): tunnel, open and bridge parts; straight, crossing, bend and T shapes; clearing, supports, railing openings for highway-side scattered buildings
- [railway-layout.md](railway-layout.md): the railway grid, station spots, station types, descents, levels, rails that end, and the rail cache
- [railway-parts.md](railway-parts.md): railway parts per type, water variants, track reduction, staircases, rail dungeons and their tunnels
- [monorails.md](monorails.md): monorail lines between city spheres and their stations

## Key facts

- **Two highway modes, chosen once per dimension.** New worlds use the profile's `highwayGenerationMode` (default `INTERCITY_NETWORK_V1`) and persist it; worlds without the persisted marker stay `LEGACY` forever. The intercity mode ignores the legacy options, including `highwayDistanceMask = 0`, which several profiles use to turn highways off (`HWY-06`).
- **Levels are city levels.** A highway or railway at level *n* sits at Y = `GROUNDLEVEL + 6n` (default ground 71). Railways sit at level -3 + `railwayLevelOffset` (default Y 53). A route keeps one level end to end; there are no ramps and no ramp parts.
- **The intercity planner is pure and bounded.** Planning cells of 128 × 128 chunks hold at most one hub; hubs pick up to 2 partners within 2 cells, preferring different compass sectors; only mutual picks become routes; the smaller cell owns the route; routes are straight or one L. A chunk query reads 25 cells. All choices use a salted SplitMix64-style hash of seed, dimension ID and coordinates; there is no `Random`. The "mandatory first connection" and "emergency search" in upstream's `HIGHWAY_MAJOR_CITY_PLAN.md` are **not** implemented at the pin.
- **Hubs avoid the railway grid** by formula: hub chunks are shifted off railway rows and station/line columns, so routes, which run along hub rows and columns, never run along a railway.
- **Hub decisions are persisted** in overworld saved data under a signature; a stored hub wins over recalculation (`HWY-11`).
- **Planning runs on a private thread pool** per dimension; the generating thread blocks until the 25-cell neighborhood is ready. Inputs (terrain height, biome) are 1.20 thread-safe samplers.
- **Railways are a fixed grid** repeating every `2 × spacing` chunks (default 20 × 20), with 3-, 2- and 1-track station spots where the grid meets a city, three-way splits, double bends and vertical lines. Stations are surface (city level 0..2, 50 % chance) or underground with a staircase.
- **Generation order in a chunk:** building or street → ruins → highway → … → railways → rail dungeons → damage. Railways overwrite highways where they overlap.
- **Monorails exist only in `space` profiles** and are placed by the spheres feature.
- **Upstream uses Java 16+** (records, switch expressions, `List.of`, `Optional.isEmpty`); the port targets the Java 8 API.

### 1.20 dependencies

Each row is also proposed for `API-MAP.md`.

| Mechanism | Where | Why it matters for 1.7.10 |
|---|---|---|
| World height -64..320 | `Supports.generatePart` runs support parts down to the minimum build height; `highwayNetworkLevel` up to 32 (Y 263 + part); `railwayLevelOffset` -8 (Y 5); `monorailOffset` ±100 | 1.7.10 is 0..255 with bedrock at 0..4; several reachable Y values fall outside or into bedrock. |
| Terrain height from the noise generator (`ChunkGenerator` + `RandomState`) | hub potential modifier, hub city level, `isTunnel`, surface-station clearing (via `ChunkHeightmap`, `03-terrain`) | No noise router in 1.7.10; under RWG the height must come from RWG without generating chunks. |
| `BiomeSource.getNoiseBiome` with `Climate.Sampler`, quart coordinates; `Holder<Biome>` predicates and biome tags | `DefaultDimensionInfo.getBiome`, world-style city multipliers | 1.7.10 uses `WorldChunkManager.getBiomeGenAt` and `BiomeDictionary`; sampled biome and matching differ. |
| Multithreaded world generation; planner work on a `ForkJoinPool` | `HighwayPlanningService` | 1.7.10 generates on the server thread; terrain and biome samplers (and RWG) are not known to be thread-safe off-thread. |
| `ResourceKey<Level>` string dimension IDs | intercity hash (`dimensionSalt`), saved-data keys, `GenerationContext` seed mix | 1.7.10 uses integer dimension IDs; the chosen string changes the layout. |
| `SavedData` / `DimensionDataStorage.computeIfAbsent` | `LostCityHighwayData`, `LostCityWorldGenData` | 1.7.10 has `WorldSavedData` with `MapStorage`; different API. |
| Forge `LevelEvent.CreateSpawnPosition` as the new-world signal | mode persistence | 1.7.10 has `WorldEvent.CreateSpawnPosition`; ordering against dimension-info creation must be rechecked. |
| `RandomSource`/`LegacyRandomSource` per chunk (`GenerationContext`) | part choice from lists | 1.7.10 uses `java.util.Random`; sequences differ. |
| `SimplexNoise` via `LegacyRandomSource` (`PerlinNoiseGenerator14`) | legacy highways | `NoiseGeneratorSimplex`/`Perlin` in 1.7.10 give different values. |
| Block tags `LEAVES`, `LOGS` | clearing above highways | No block tags in 1.7.10. |
| `BlockState` identity: liquid test, all states of `RAIL` and `POWERED_RAIL`, `IRON_BARS` | `Railways`, `Highways` | 1.7.10 compares block plus metadata. |
| DFU codecs (`RecordCodecBuilder`, `Tools.listOrStringList`) | `HighwayParts`, `RailwayParts`, `MonorailParts`, `RailSettings` | Plain JSON parsing in 1.7.10. |
| Java 16+ language and API | `worldgen/highway/*`, gen classes | Records, switch expressions, `List.of`/`copyOf`, `Optional.isEmpty`/`stream`, `Stream.toList` are unavailable on the Java 8 API. |

## Related areas

- `01-core`: `/lostcities debug` prints highway and rail state; `QualityRandom`, `PerlinNoiseGenerator14`, `ChunkCoord`; `ILostChunkInfo.getRailType`/`getMaxHighwayLevel` expose results; `LostCityWorldGenData` lifecycle.
- `02-profiles-dimensions`: every highway and railway option, the per-profile overrides, the inside/outside profile split.
- `03-terrain`: heightmaps, city spheres (exclusions, monorail lines), generation order, `gen/Spheres` calls monorails.
- `04-city-layout`: city membership and levels, `BuildingInfo` rules over highways and railways (building allowed, cellar caps, rail removal under `block_railway`), bridges, multibuildings, corridors (the city "subways", separate from this grid), `Supports`, `generatePart`.
- `05-assets`: field formats of `parts.highways`, `parts.railways`, `parts.monorails`, `railblocks`, `raildungeons`, `settings.railwayavoidance`/`railpartheight6`, and the part catalog.
- `07-scattered`: highway-attached scattered buildings (gas stations, restaurants) and the railing openings they request; scattered avoidance of highway chunks.
- `08-loot-mobs-blocks`: loot in rail dungeons and stations.
- `09-client-ui`: the map preview draws highways and railways with its own planner (no modifier, no persistence, a pool per frame).

Cross-area observations for those owners (outside this area's scope, not registered here):

- `04-city-layout`: `City.isCityCenter` and `getCityRadius` seed `java.util.Random` from chunk coordinates only, so in `cityChance` mode every world has the same city centers; the highway potential copies this.
- `03-terrain`/`04-city-layout`: `LostCityTerrainFeature.clearRange` with water fills liquid below the water level and air from water level + 1, so the block at exactly the water level is left as it was (affects clearing above drowned highways).
- `01-core`: `LostCityWorldGenData.save` iterates its mode maps without the lock that `getHighwayMode`/`getStreetMode` hold while writing them.
- `01-core`: `PerlinNoiseGenerator14` seeds all four octaves identically.
- `04-city-layout`: `Bridges` assumes highway levels are 0 or 1 ("a highway at level 0 blocks bridge parts"); intercity highways can be at any level.

## Coverage

- Source files read: **28 of 28** in scope (3047 lines), all in full:
  - `worldgen/highway/`: `ApproximateCityPotential`, `BoundedFutureCache`, `CityPotential`, `HighwayAxis`, `HighwayCityLevelSource`, `HighwayConnectionKey`, `HighwayHub`, `HighwayHubPersistence`, `HighwayInfo`, `HighwayLevelSource`, `HighwayPlannerSettings`, `HighwayPlanningService`, `HighwayRoute`, `HighwaySector`, `HighwaySegment`, `HubKey`, `IntercityHighwayPlanner` (17);
  - `worldgen/lost/Highway`, `worldgen/lost/Railway`, `worldgen/LostCityHighwayData`;
  - `worldgen/gen/Highways`, `Railways`, `Monorails`;
  - `api/RailChunkType`;
  - `worldgen/lost/regassets/data/HighwayParts`, `RailwayParts`, `MonorailParts`, `RailSettings`.
- In-scope files not covered: none.
- Out-of-scope files read, in full: `config/HighwayGenerationMode`, `worldgen/IDimensionInfo`, `worldgen/DefaultDimensionInfo`, `worldgen/LostCityWorldGenData`, `worldgen/plan/ChunkPlanner`, `worldgen/lost/regassets/data/WorldSettings`, `worldgen/lost/regassets/data/PartSelector`, `varia/QualityRandom`, `varia/PerlinNoiseGenerator14`.
- Out-of-scope files read in part (the highway, railway and monorail paths): `worldgen/lost/BuildingInfo`, `worldgen/LostCityTerrainFeature`, `worldgen/LostCityFeature`, `worldgen/GenerationContext`, `worldgen/ChunkDriver`, `worldgen/plan/ChunkPlan`, `worldgen/lost/City`, `worldgen/lost/CitySphere`, `worldgen/lost/MultiChunk`, `worldgen/gen/Scattered`, `worldgen/gen/Spheres`, `worldgen/gen/Supports`, `worldgen/gen/Corridors`, `worldgen/lost/cityassets/WorldStyle`, `worldgen/lost/cityassets/CityStyle`, `worldgen/lost/regassets/WorldStyleRE`, `worldgen/lost/regassets/CityStyleRE`, `config/LostCityProfile`, `config/ProfileSetup`, `setup/Config`, `setup/ForgeEventHandlers`, `commands/CommandDebug`, `gui/NullDimensionInfo`, `gui/GuiLCConfig`, `api/ILostCities`, `varia/Tools`, `varia/ChunkCoord`.
- Upstream notes: `HIGHWAY_MAJOR_CITY_PLAN.md` (status, goals, problems, phases 4 and 5) and `profile_plan.md` (highway and railway rows) were read to explain intent only.
- `upstream/1.12` compared: `dimensions/world/lost/Highway`, `dimensions/world/lost/Railway`, `dimensions/world/terraingen/LostCitiesTerrainGenerator` (highway part generation).
- Assets: dimensions of the 36 bundled highway, rail, station, monorail and rail-dungeon parts, and the highway and rail fields of the bundled world and city styles, were tabulated; the full catalog is `05-assets`'.
- Textures, sounds: none in scope. Lang keys: 0 of 0 (this area has no player-visible strings; debug output is hard-coded English in `CommandDebug`, `01-core`).
- Corrections to earlier statements: none (first version).

## Known limits

- Nothing was generated or run; every description of what a structure looks like is *inferred* from code paths and part dimensions.
- Part contents (block layouts inside each highway, rail and station part) were not inspected beyond their sizes; `05-assets` catalogs them.
- `CitySphere`, `BuildingInfo`, `City` and `LostCityTerrainFeature` were read only where they touch this area; statements about them are limited to those paths.
- Thread-safety of the 1.20 samplers used off-thread (heightmap, biome) is assumed from 1.20's design, not verified.
- 1.12 monorails were not compared.

## Suspected bugs

| ID | Title | Location | What looks wrong | Severity |
|---|---|---|---|---|
| HWY-01 | Station "city to the west" test checks the east spot | `worldgen/lost/Railway.java:130` | With `railwaysCanEnd`, the 3-track spot's west test starts with `offset(spacingX, 0)` (east) instead of `(-spacingX, 0)`. A city only due west gives no rail; a city only due east is counted on both sides, so rails continue west into nothing. 1.12 had `chunkX - 10`. | worldgen |
| HWY-02 | Z highways test the east neighbor twice | `worldgen/gen/Highways.java:37`, `:39`, `:46` | The in-city test for a Z-axis part passes `getXmax()` as both neighbors, so the west neighbor's city flag and level are ignored when choosing open road vs bridge. Also on 1.12. | worldgen |
| HWY-03 | Legacy X and Z highway noise share one seed | `worldgen/lost/Highway.java:24-26` | Both `PerlinNoiseGenerator14` are built from `provider.getSeed()`; the X and Z fields are the same field with swapped scales (identical at the origin). 1.12 salted Z with `^ 879190747`. | worldgen |
| HWY-04 | Legacy extent walk can loop forever | `worldgen/lost/Highway.java:121`, `:128` | The walk to a highway's ends has no bound; with `highwayPerlinFactor` at or below the noise minimum (about -15; the option allows -100) every chunk qualifies and generation hangs. | crash |
| HWY-05 | Rejected legacy highways are never cached | `worldgen/lost/Highway.java:133-159` | When a run is shorter than 6 chunks or lacks its cities, nothing is cached, so every query for any of its chunks re-walks the whole run and re-tests cities. | performance |
| HWY-06 | Intercity mode ignores `highwayDistanceMask = 0` | `worldgen/lost/Highway.java:65-74` | Only the legacy path reads the mask. `space`, `biosphere`, `biosphere_caves` and `void_outside` set it to 0 to disable highways but default to the intercity mode, so highways are still planned outside spheres (the potential estimate ignores spheres, and `space` has `cityChance` 0.7, so hubs are likely wherever the height filter passes) and can run through the void, where `space` also disables supports. | worldgen |
| HWY-07 | Rail removal makes rails depend on generation order | `worldgen/lost/Railway.java:355-357`, caller `worldgen/lost/BuildingInfo.java:1012` | Under `block_railway` avoidance, `removeRailChunkType` overwrites a cached rail info while neighbors may already have derived their own info from it; whether a chain of rail chunks is cut depends on which was computed first. | worldgen |
| HWY-08 | Rail cache ignores the asking profile | `worldgen/lost/Railway.java:334-353` | The cache key is the coordinate; enable flags use the caller's `profile` while the grid uses the chunk's own profile, and recursive lookups pass the caller's profile. In multi-profile dimensions the first caller decides. | worldgen |
| HWY-09 | Overlapping routes collapse to the lowest level | `worldgen/highway/IntercityHighwayPlanner.java:455`, `:458` | Where two routes share chunks on one axis at different levels, only the lowest level is kept there; the higher route keeps its level elsewhere, so it breaks off or jumps where they meet or part. | visual |
| HWY-10 | "LRU" caches evict arbitrary entries | `worldgen/highway/IntercityHighwayPlanner.java:17`, `:621-642`; `worldgen/highway/BoundedFutureCache.java:64-85` | Trimming removes completed entries in `ConcurrentHashMap` iteration order, not least recently used; hot hubs and chunks can be evicted and recomputed. In-flight entries let the map exceed its bound. | performance |
| HWY-11 | Hub persistence signature misses inputs | `worldgen/LostCityHighwayData.java:135-162` | The signature covers the world-style ID but not its biome multipliers, nor `Config.OPTIMIZED_HEIGHTMAP`, predefined cities, other terrain sources or unversioned code changes. Stale persisted hubs then override recalculation, so the layout depends on world history, contrary to the class Javadoc. | worldgen |
| HWY-12 | Missing palette entries crash generation | `worldgen/gen/Highways.java:110`, `worldgen/gen/Railways.java:150` | A highway support character or rail main block absent from the palette throws `RuntimeException` mid-generation instead of skipping. | crash |
| HWY-13 | Rail dungeon tunnels differ north vs south | `worldgen/gen/Railways.java:156-179` | The tunnel toward a northern dungeon is 4 deep (walls 3), toward a southern one 5 deep (walls 4). May compensate for part layout; unverified. | cosmetic |
| HWY-14 | One thread pool per dimension info | `worldgen/highway/HighwayPlanningService.java:31` | Each `DefaultDimensionInfo` and each GUI preview frame creates a `ForkJoinPool` of `max(2, cpus/2)` threads. | performance |
| HWY-15 | Unbounded static caches | `worldgen/lost/Highway.java:19-21`, `worldgen/lost/Railway.java:94` | Legacy level caches and the rail cache keep an entry for every chunk touched in a server session, cleared only at server start and stop. | performance |

<!-- Merged and deduplicated into BUGS.md. Suspected, not confirmed; no fixes designed here. -->
