# Chunk planner, stages and caches

How per-chunk layout decisions are computed once, cached, and shared between threads. Source: `worldgen/plan/ChunkPlanner.java`, `ChunkPlan.java`, `ChunkStage.java`, `Staged.java`; `BuildingInfo.getBuildingInfo`, `computeBuildingInfo`, `getChunkCharacteristics`, `cleanCache`; `City.cleanCache`; `varia/TimedCache.java` (out of scope, read for behavior), on `f18a094d`.

## 1. Identity

| Class | Role |
|---|---|
| `ChunkPlanner` | Static entry points: `cityLevel`, `highway`, `characteristics`, `buildingInfo`, `clear` |
| `ChunkPlan` | Per-chunk holder of three lazily computed stages, each behind its own `ReentrantLock` |
| `Staged<T>` | A computed value plus `cacheable` (false while structure avoidance is unknown) |
| `ChunkStage` | Enum `BASE`, `INFRASTRUCTURE`, `CHARACTERISTICS`, `CHUNK`. **Unused** (marked TODO); documents the intended order |
| `TimedCache` | Concurrent map with idle-time expiry |

Server config: `cacheCleanupSeconds` (`Config.CACHE_CLEANUP_SECONDS`).

## 2. Inputs

Chunk coordinates (with dimension), the `IDimensionInfo` provider (seed, profiles, world style, street planner, world or null).

## 3. Numbers

| Item | Value |
|---|---|
| `cacheCleanupSeconds` | 300 s default, range 1..86400 |
| Cleanup interval | `max(1 s, TTL / 2)` |
| Expiry | an entry idle (not accessed) for at least the TTL |

### Every cache this area touches

| Cache | Owner | Key | Kind | Cleared by |
|---|---|---|---|---|
| `PLANS` | `ChunkPlanner` | `ChunkCoord` | `TimedCache<ChunkPlan>` | TTL, `BuildingInfo.cleanCache` |
| `CITY_LEVELS` | `ChunkPlanner` | `ChunkCoord` | `TimedCache<Integer>` | TTL, `cleanCache` |
| `CITY_STYLE_CACHE` | `City` | `ChunkCoord` (profile not in key) | `TimedCache` | TTL, `City.cleanCache` |
| `CITY_RARITY_MAP` | `City` | dimension | `ConcurrentHashMap` | `City.cleanCache` |
| Predefined maps (cities, buildings, streets, occupied chunks) | `City` | `ChunkCoord` | immutable maps behind `volatile` | `City.cleanCache` |
| `MULTICHUNKS` | `MultiChunk` | multichunk coordinate | `TimedCache` | TTL, `MultiChunk.cleanCache` |
| `MULTIBUILDING_DECISIONS` | `StructureAvoidance` | footprint | unbounded `ConcurrentHashMap` | `StructureAvoidance.cleanCache` (`07-scattered`) |
| `MEMOIZATION_LOCKS` | `BuildingInfo` | dimension | `ConcurrentHashMap<Object>` | never |
| `RAIL_INFO` | `Railway` (`06`) | `ChunkCoord` | unbounded `ConcurrentHashMap` | `06` |

Where the clear methods are called from (world load and unload) is in `01-core`.

## 4. Behavior

All (server).

### Stage order

A chunk's decisions are computed in three dependent stages. Each may read earlier stages of *other* chunks:

1. **Infrastructure** (`ChunkPlan.highway`): the highway levels of the chunk (`06-highways-railways`).
2. **Characteristics** (`ChunkPlan.characteristics`): city membership, multibuilding position, city level, planned road type, building possibility, city style, building type ([chunk-characteristics.md](chunk-characteristics.md)).
3. **Chunk** (`ChunkPlan.buildingInfo`): the full `BuildingInfo` ([building-info.md](building-info.md)).

City level has its own cache (`CITY_LEVELS`) outside the plan.

### Lookup (`ChunkPlanner.characteristics` / `buildingInfo` / `highway`)

1. No world (GUI preview with `NullDimensionInfo`): compute and return; nothing is cached.
2. Otherwise get or create the chunk's `ChunkPlan` in `PLANS` with `withPinnedValue`. Pinning increments the entry's active-user count so it cannot expire while the call runs; an expired entry is replaced only when no thread is using it.
3. In the plan: return the stage's value if already set (volatile read). Otherwise take the stage lock, check again, compute.
4. If the computed `Staged` is `cacheable`, store it; return the value either way.

A non-cacheable result (structure references not yet available for a chunk the active `WorldGenRegion` cannot see) is recomputed on the next request. Until then neighbors may have used the provisional value (CITY-20).

### Clearing

`BuildingInfo.cleanCache` clears `PLANS`, `CITY_LEVELS` and the structure-avoidance footprint cache. `City.cleanCache` clears the predefined maps, rarity maps and the style cache.

### Locks inside `BuildingInfo`

Lazily computed, cross-chunk values (stairs, bridges, terrain-correction targets) synchronize on one lock per dimension (`MEMOIZATION_LOCKS`), so they serialize across the whole dimension. Palette compilation and damage areas use a per-instance lock.

## 5. Determinism

The planner is meant to be a pure memo: a chunk's plan depends only on seed, coordinates, profile and assets. Three things break that:

- **Provisional values** (CITY-20): a non-cacheable plan seen by a neighbor can differ from the final one.
- **Mutable state inside cached `BuildingInfo`s**: `getBuildingBottomHeight` rewrites floors and cellars (CITY-18); generation rewrites `streetType` (CITY-13); legacy bridges write other chunks' bridge fields; the constructor deletes rail chunks from the railway cache (CITY-19). After TTL eviction a recomputed `BuildingInfo` starts fresh, so results depend on generation order and on wall-clock time between accesses (`TimedCache` uses `System.currentTimeMillis`).
- **Threading**: 1.20 generates chunks in parallel; the locks keep each value consistent but not the order in which mutable state changes.

## 6. Data formats

None. No NBT; nothing is persisted. The street generation mode is persisted separately ([street-planner.md](street-planner.md)).

## 7. Output

Shared `LostChunkCharacteristics`, `BuildingInfo`, `HighwayInfo` and city levels for every consumer in `03`, `04`, `06` and `07`.

## 8. Relationships

- Called by: everything that asks `BuildingInfo.getBuildingInfo`, `getChunkCharacteristics` or `getCityLevel`.
- Calls: `BuildingInfo.computeChunkCharacteristics`, `computeBuildingInfo`, `computeCityLevel`, `Highway.computeHighwayInfo` (`06`).

## 9. 1.20 dependencies

| Mechanism | Where | Why it matters for 1.7.10 |
|---|---|---|
| Parallel chunk generation (worker threads) | locks in `ChunkPlan`, `MEMOIZATION_LOCKS`, `TimedCache` | 1.7.10 generates on the server thread; the locking model is not needed, but the memo must stay pure |
| `WorldGenRegion` and staged chunk statuses | `Staged.cacheable` | No such region in 1.7.10; populate runs with the +8 offset (`03-terrain`) |
| Java records, `ReentrantLock`, `ConcurrentHashMap.compute` | everywhere | Records are not allowed on Java 8 (CONTRIBUTING); the rest exists in Java 8 |

## 10. Bugs

CITY-13, CITY-18, CITY-19, CITY-20.

## Deviations in the new mod

See [new-spec 04-city-layout § Planning stages](../../new-spec/04-city-layout/index.md#planning-stages-chunk-plannermd-chunk-characteristicsmd).
