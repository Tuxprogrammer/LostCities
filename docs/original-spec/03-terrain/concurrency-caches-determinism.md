# Concurrency, caches and determinism

How upstream keeps parallel chunk generation safe, which caches the terrain code keeps and who owns them, and every random source in this area with its seed. 1.7.10 generates chunks on the server thread, so most of the locking has no counterpart; the caches and seeds matter for the port's determinism rules ([CONTRIBUTING § World generation rules](../../../CONTRIBUTING.md#world-generation-rules)).

## 1 · Identity

| Piece | Role |
|---|---|
| `LostCityFeature` | Per-dimension `IDimensionInfo` map; lifecycle read-write lock; 4096 striped generation locks; `cleanUp`. |
| `GenerationContext` | Thread-local per-chunk scope: seeded random, palette index sequence, street character, noise buffers, `ChunkDriver`. |
| `LostCityTerrainFeature` | Heightmap `TimedCache` and in-flight map; lazily built block-state sets; block-entity type cache; `Statistics`. |
| `CitySphere` | Static sphere cache and per-dimension locks. |
| `GlobalTodo` | Static per-dimension job queues. |
| `ErrorLogger` | Static last-report timestamp. |
| `varia/TimedCache` (01-core) | Concurrent map with access-time expiry. |

Config: `cacheCleanupSeconds` (default 300, 1–86 400). The volatile static `LostCityFeature.globalDimensionInfoDirtyCounter` is bumped by `ForgeEventHandlers.onCreateSpawnPoint`, the world-creation GUI (`GuiLCConfig`, twice) and `ClientEventHandlers` (01-core, 09-client-ui document when).

## 2 · Inputs

Thread identity (thread-locals), the world seed, the dimension key, chunk coordinates.

## 3 · Numbers

| Item | Value |
|---|---|
| Generation lock stripes | 4096 (power of two; index = mixed hash & 4095) |
| Stripe hash | `h = dimensionId.hashCode(); h = 31h + chunkX; h = 31h + chunkZ; h ^= h >>> 16` |
| Locks per generation call | the distinct stripes of the 3 × 3 neighborhood, taken in ascending index order, released in reverse |
| Heightmap TTL | `cacheCleanupSeconds` × 1000 ms since last access; cleanup sweep at most every max(1000 ms, TTL / 2) |
| Scratch noise buffers | 4 × 256 doubles (rubble, leaves, ruin, bottom layer) per thread |
| Palette sequence | `seed = 214013 × seed + 2531011` (32-bit), index = `(seed >> 16) & 127` |
| Fallback palette seed (outside generation) | 123456789 per thread |

## 4 · Behavior

### Lifecycle (server)

1. Every entry (`place`, `getDimensionInfo`) takes the lifecycle **read** lock. If the global dirty counter differs from the feature's own, it drops the read lock, takes the **write** lock, and (if still different) runs `cleanUpInternal`: `LostCitiesImp.cleanUp`, `ForgeEventHandlers.cleanUp` (clears `BuildingInfo`, `MultiChunk`, `Highway`, `Railway`, `BiomeInfo`, `City`, `CitySphere`, `Scattered` caches and the profile cache), `AssetRegistries.reset`, shuts down every `IDimensionInfo` (stops the highway planning service) and empties the map; then records the counter and retries.
2. `cleanUp()` (also run on server start and stop through `ForgeEventHandlers`) takes the write lock, so it waits for all running generation.
3. `getOrCreateDimensionInfo`: profile name from config for the dimension; missing profile → none; otherwise `computeIfAbsent` a `DefaultDimensionInfo(world, profile, outsideProfile)` and point it at the current world.

### Per-chunk generation (server, worker threads)

1. Lock the 3 × 3 neighborhood's stripes (sorted, so two threads never deadlock). Chunks whose neighborhoods do not share a stripe generate in parallel; a stripe collision just serializes more.
2. `GenerationContext.open` saves any outer context; the outermost scope reuses the thread's buffers and driver, a nested one gets fresh ones. Seeds the random and palette sequence. `close` restores the outer context.
3. Shared structures touched during generation:
   - `BuildingInfo` and planning caches (04-city-layout) use a per-dimension memoization lock;
   - the heightmap cache uses a `ConcurrentHashMap` of in-flight futures so one sampler group is computed once while others wait;
   - `CitySphere.getCitySphere` takes a per-dimension monitor;
   - lazily built immutable block-state sets (rails, saplings and small flowers, lights, POI) and default leaf and rubble tables use double-checked locking on `volatile` fields;
   - the block-entity type cache is a `ConcurrentHashMap`;
   - `BuildingInfo.postTodo` is a plain `HashMap`; it is only safe because neighbors' 3 × 3 locks overlap (`TERR-09` writes into a neighbor's map).

### Cache inventory

| Cache | Key | Owner and lifetime | Bounded | Notes |
|---|---|---|---|---|
| Heightmaps | `ChunkCoord` | terrain feature (per dimension info); TTL 300 s | by time | mutated after creation (`TERR-10`) |
| Pending heightmap samples | sampler `ChunkCoord` | terrain feature; removed when done | yes | |
| `IDimensionInfo` | dimension key | `LostCityFeature`; until cleanup | yes | |
| City spheres | `ChunkCoord` | **static**; cleared on server start/stop and by the GUI | no | order-dependent fill (`TERR-02`) |
| Global todo queues | dimension, then `ChunkPos` | **static**; never cleared | no | `TERR-21` |
| Block-entity types | `Block` | terrain feature | by block count | |
| Block-state sets, leaf and rubble tables | — | terrain feature, built once | yes | |
| `ChunkDriver` staging | — | per thread, cleared at each commit | yes | |
| Statistics | — | terrain feature | yes | wall-clock times for `/lostcities stats` |

### Error reporting (server)

`ErrorLogger.report(message)` sends a red system chat message to every player on the server, at most once every 10 000 ms (a static, unsynchronized timestamp). `logChunkInfo` logs whether the chunk is a city, its level, multibuilding, building, floors and cellars, catching its own exceptions.

## 5 · Determinism

Every random source in this area:

| Use | Generator and seed | World seed? | Sheet |
|---|---|---|---|
| Per-chunk context (parts, ruins, spawner mobs, loot, fork for trees) | `LegacyRandomSource(mixSeed(worldSeed, dimension, x, z))`, mixSeed = XOR of the seed with `dimHash × 0x9e3779b97f4a7c15`, `x × 257017164707`, `z × 101754694003`, then the 64-bit finalizer (`>>> 33`, × `0xff51afd7ed558ccd`, `>>> 33`, × `0xc4ceb9fe1a85ec53`, `>>> 33`) | yes | pipeline |
| Palette index sequence | `(int)(mixed ^ mixed >>> 32)` then LCG | yes | this sheet |
| Damage and debris | context random reseeded to `x × 257017164707 + z × 101754694003` | **no** | damage |
| Chunk fixer (vines) | context random reseeded to `worldSeed ^ x × 341873128712 ^ z × 132897987541` | yes | post-processing |
| Rubble, leaves, ruin, floating bottom noise | `NoiseGeneratorPerlin(rand, 4)` × 4 in that order from `LegacyRandomSource(worldSeed)` | yes (not the dimension) | damage, city chunk |
| Terrain transition noise | `NoiseGeneratorSimplex(LegacyRandomSource(worldSeed ^ dimHash × 0x9e3779b97f4a7c15 ^ 0x6a09e667f3bcc909))` | yes | blend |
| Corner offsets L0, L1, L2 | thread-local `java.util.Random`, coordinate-only seeds | **no** | blend |
| Street-type re-roll | `new Random(z × 155557723 + x × 45555558379)` | **no** | building and street |
| Random vegetation | thread-local `Random(worldSeed × 377 + z × 341873128712 + x × 132897987541)` | yes | building and street |
| Explosions | `Random(worldSeed + …)` per chunk, three salts | yes | damage |
| Spheres | `Random(worldSeed + cx × 961744153 + cz × 837971201)`; blocks `Random(worldSeed + cx × 837971201 + cz × 961744153)` | yes | spheres |

Order and timing dependence in this area: `TERR-02`, `TERR-07`, `TERR-09`, `TERR-10`, `TERR-11`, `TERR-12`, `TERR-20`. Wall-clock time is used only for cache expiry, statistics and the chat throttle, but cache expiry feeds back into output through `TERR-10`.

## 6 · Data formats

None.

## 7 · Output

None directly; it shapes how the other stages run.

## 8 · Relationships

All sheets in this area; 04-city-layout (`BuildingInfo` locking, `ChunkPlanner` caches); 01-core (`ForgeEventHandlers`, `TimedCache`, `Statistics`, commands); 02-profiles-dimensions (`Config.getProfileForDimension`, `DefaultDimensionInfo`).

## 9 · 1.20 dependencies

- Concurrent chunk generation on worker threads and the `WorldGenRegion` 3 × 3 window: 1.7.10 generates and populates on the server thread, with the population +8 offset instead.
- `LegacyRandomSource`, `RandomSource.fork`; the Perlin and simplex helpers are Lost Cities' own (`varia`), ported from older Minecraft.
- `Set.copyOf` (Java 10+), `Map.copyOf`.

## 10 · Bugs

`TERR-21`, `TERR-25`, `TERR-33`, and the order-dependence bugs above.

## On 1.12

1.12 had no locking: one `LostCityChunkGenerator` per world on the server thread, with `HashMap` caches of primers and heightmaps keyed by chunk, and the generator's own `Random` reseeded per use.

## Deviations in the new mod

None recorded yet.
