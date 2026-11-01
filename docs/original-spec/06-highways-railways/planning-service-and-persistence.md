# Highway planning service, threading and hub persistence

How the intercity planner is run during generation (a per-dimension thread pool that precomputes the planner's dependencies), and how hub decisions are saved with the world.

| # | Field | Contents |
|---|---|---|
| 1 | **Identity** | `worldgen/highway/HighwayPlanningService`, `worldgen/highway/BoundedFutureCache`, `worldgen/highway/HighwayHubPersistence` (with `Lookup` and `NONE`), `worldgen/LostCityHighwayData` (saved data). Created by `DefaultDimensionInfo` (out of scope, read). No profile options of its own. |
| 2 | **Inputs** | The planner; the overworld's data storage; the dimension key; the cache signature (seed, settings, profile values, world-style ID, `Config.HEIGHT_SAMPLE_SIZE`). |
| 3 | **Numbers** | See [Numbers](#numbers). |
| 4 | **Behavior** | See [Behavior](#behavior). |
| 5 | **Determinism** | Results are the planner's; the service only changes when and on which thread they are computed. Persistence makes hub decisions sticky. See [Determinism](#determinism). |
| 6 | **Data formats** | `LostCityHighwayData` NBT. See [Data formats](#data-formats). |
| 7 | **Output** | `HighwayInfo` for a chunk (blocking call); a saved-data file of hub decisions. |
| 8 | **Relationships** | Called by `Highway.computeHighwayInfo` ([highway-occupancy.md](highway-occupancy.md)); wraps [intercity-planner.md](intercity-planner.md); shut down by `LostCityFeature.cleanUpInternal` and by the GUI preview. |
| 9 | **1.20 dependencies** | Multithreaded chunk generation; `SavedData`/`DimensionDataStorage.computeIfAbsent`; `ResourceKey<Level>` IDs; `CompoundTag`/`ListTag`. |
| 10 | **Bugs** | `HWY-10`, `HWY-11`, `HWY-14`. |

## Numbers

| Item | Value |
|---|---|
| Worker threads per service | `max(2, availableProcessors / 2)`, a dedicated `ForkJoinPool` per dimension info |
| Service hub-future cache | 4096 entries |
| Service candidate, selection, route and prepare caches | 2048 entries each |
| Saved-data name | `LostCityHighwayData` (overworld data storage) |
| `FORMAT_VERSION` | 2 (any other stored version is ignored and the data starts empty) |
| `HUB_ALGORITHM_VERSION` | 3 (mixed into the signature) |
| Signature hash | FNV-1a style: start `0xcbf29ce484222325`; for each input, XOR the 64-bit value, multiply by `0x100000001b3`; strings one UTF-16 unit at a time |

## Behavior

### Construction (server)

1. (server) `DefaultDimensionInfo` builds the planner only when the dimension's mode is `INTERCITY_NETWORK_V1`. It computes the cache signature, gets the overworld's `LostCityHighwayData` and a dimension-scoped persistence view for (dimension, signature), and wraps the planner in a `HighwayPlanningService`.
2. (server) In `LEGACY` mode both the planner and the service are absent; asking for either throws `IllegalStateException`.
3. (server) `shutdown()` on the dimension info shuts the pool down. `LostCityFeature.cleanUpInternal` calls it for every dimension info on server start, server stop, and when the new-world marker forces a rebuild.

### Query (server, on the generating thread)

1. (server) `getHighwayInfo(chunk)` calls `prepare(chunk)` and **blocks** (`join`) until it completes, then asks the planner directly, which is now served from the planner's caches.
2. (server) `prepare(chunk)` is cached per planning cell. It requests the owned routes of every cell within the search radius and completes when all of them are ready.
3. (server) Owned routes of a cell: first its selected neighbors; then, for each neighbor whose cell sorts after it, that neighbor's selected neighbors (the reciprocity check); then, on the pool, `planner.getOwnedRoutes`.
4. (server) Selected neighbors: first the candidates, then, on the pool, `planner.getSelectedNeighbours`.
5. (server) Candidates: first the source hub; if it exists, the hubs of every other cell in the radius; then, on the pool, `planner.getConnectionCandidates`.
6. (server) Hub: one pool task calling `planner.getHub`.
7. (server) Every stage is memoized in a `BoundedFutureCache`. A stage whose future fails is removed so a later call retries; the failure reaches the generating thread as a `CompletionException` from `join`.

`prepareHubs(chunk)` (hubs of the radius square, sorted) exists but has no caller at the pin.

### `BoundedFutureCache` (both)

1. `computeIfAbsent(key, factory)`: return the existing future if any. Otherwise insert a placeholder future, call the factory, and complete the placeholder when the factory's future completes. If the factory throws or its future fails, remove the placeholder and complete it exceptionally.
2. After each completion, if the map is larger than its bound and no other thread is trimming, remove completed entries in iteration order until the map fits; repeat while it is still too large and completed entries remain. In-flight futures are never removed, so the map can exceed its bound while many tasks are pending.
3. `clear()` drops all entries; `size()` counts them. `HighwayPlanningService.clear()` clears every service cache and the planner's caches; it has no caller at the pin (the whole service is discarded instead).

### Hub persistence (server)

1. (server) `forDimension(dimension, signature)`: if the stored data for that dimension ID is missing or has another signature, replace it with an empty set under the new signature and mark the data dirty. Return a view whose `get`/`put` go to `lookup`/`store` for that dimension and signature.
2. (server) `lookup`: if the dimension's data is missing, has another signature, or has no entry for the cell, report "unknown" (the planner then calculates). Otherwise return the stored decision: a hub, or "no hub".
3. (server) `store`: if the dimension's data is missing or has another signature, replace it with an empty set under this signature. Put the decision; mark dirty if it changed.
4. (server) All three methods and `save` are `synchronized` on the data object. `store` runs on planner worker threads.
5. (server) Signature inputs, in order: `FORMAT_VERSION`; `HUB_ALGORITHM_VERSION`; world seed; cell size; sample spacing; minimum potential (float bits); railway spacing north-south; railway spacing east-west; `cityChance` (double bits); `cityMinRadius`; `cityMaxRadius`; `citySpawnDistance1`; `citySpawnDistance2`; `citySpawnMultiplier1` and `2` (double bits); `cityPerlinScale`, `cityPerlinOffset`, `cityPerlinInnerScale` (double bits); `cityMinHeight`; `cityMaxHeight`; then for the profile and for the outside profile either `0` (no profile) or `1`, landscape-type ordinal, `useAvgHeightmap` (1/0), `cityLevel0Height` … `cityLevel7Height`; then `Config.HEIGHT_SAMPLE_SIZE`; then the world-style ID string.

## Determinism

- **Threads:** results do not depend on which thread computes them, because the planner is pure and every cache stores futures, so one computation per key wins and others wait.
- **Blocking:** the generating thread waits on the pool for every chunk whose planning cell has not been prepared. In 1.20 the generating thread is a world-generation worker, not the main server thread.
- **Pool lifetime:** one pool per dimension info. The GUI preview, by its own comment in `GuiLCConfig`, builds a preview dimension info, and therefore a new pool, per frame and shuts it down afterwards (`HWY-14`, `09-client-ui`).
- **Persistence:** a stored decision overrides recalculation for the life of the signature. Inputs that change hub results but are not in the signature keep old hubs alive: the world-style asset's biome multipliers (only its ID is signed), `Config.OPTIMIZED_HEIGHTMAP`, predefined cities, other terrain mods, and any later code change that does not bump `HUB_ALGORITHM_VERSION`. Then two worlds with the same seed and settings can disagree depending on what was saved (`HWY-11`). The Javadoc's claim that the signature "never changes a calculated result" holds only when every input is signed.
- **Signed changes:** a changed signature discards all stored hubs for that dimension, so already-generated chunks may no longer match newly planned routes at the border (a layout break the owner should expect when settings change).
- **Growth:** the stored map gains one entry per planning cell ever queried, including empty cells, with no bound (cells are 128 × 128 chunks by default, so growth is small).
- **Dirty flag:** `setDirty` is called from worker threads; the flag is not volatile. At worst a save is skipped and the hubs are recalculated later, with the same result if the inputs are unchanged.

## Data formats

`LostCityHighwayData` (overworld `data/LostCityHighwayData.dat`):

| Key | Type | Meaning |
|---|---|---|
| `version` | int | Must be 2, else the whole file is ignored |
| `dimensions` | compound | One entry per dimension ID string, written in sorted order |
| `dimensions.<id>.signature` | long | Cache signature the hubs were computed under |
| `dimensions.<id>.hubs` | list of compound | One per known planning cell, sorted by cell |
| `…hubs[].cellX`, `cellZ` | int | Planning-cell coordinates |
| `…hubs[].hasHub` | boolean (byte) | false: the cell has no hub |
| `…hubs[].chunkX`, `chunkZ` | int | Hub chunk (only when `hasHub`) |
| `…hubs[].potential` | int | Hub score 0..1 000 000 |
| `…hubs[].cityLevel` | int | City level at the hub, used for route levels |

Missing keys read as 0 or false (NBT defaults).

## Output

`HighwayInfo` per chunk; the saved-data file.

## Relationships

- `Highway.computeHighwayInfo` → `HighwayPlanningService.getHighwayInfo` ([highway-occupancy.md](highway-occupancy.md)).
- Wraps `IntercityHighwayPlanner` ([intercity-planner.md](intercity-planner.md)).
- `LostCityHighwayData` shares the overworld data storage with `LostCityWorldGenData` (`01-core`).
- `CommandDebug` prints `getParallelismStats()`.

## 1.20 dependencies

| Mechanism | Where | Why it matters for 1.7.10 |
|---|---|---|
| Multithreaded chunk generation; planner work on a private `ForkJoinPool` | `HighwayPlanningService` | 1.7.10 generates on the server thread. A pool can still compute the pure planner, but every input it reads (heightmap, biome, city level) must then be safe off-thread. |
| `SavedData` and `DimensionDataStorage.computeIfAbsent(load, create, name)` | `LostCityHighwayData.get` | 1.7.10 uses `WorldSavedData` with `MapStorage.loadData`/`setData`. |
| `ResourceKey<Level>` dimension IDs as NBT keys | `forDimension`, `save` | 1.7.10 dimensions are integers. |
| `CompoundTag`/`ListTag` | `save`, load constructor | `NBTTagCompound`/`NBTTagList` exist; method names differ. |

## Bugs

- `HWY-10`: the "bounded" caches evict arbitrary completed entries, not least-recently-used ones.
- `HWY-11`: hub persistence keys on a signature that omits inputs that change hubs, so layouts depend on history.
- `HWY-14`: one thread pool per dimension info (and per GUI preview frame).

## Deviations in the new mod

None recorded yet; the new spec decides.
