# Shared utilities (`varia`)

The small helpers every other area leans on: chunk coordinates, the time-to-live cache behind most layout caches, the deferred-work queue, block-state string conversion, weighted random choice, sea level, geometry for explosion damage, counters and timing statistics. Random and noise generators have their own sheet, [noise-and-random.md](noise-and-random.md); `CustomTeleporter` is in [bed-teleporter.md](bed-teleporter.md).

| # | Field | Contents |
|---|---|---|
| 1 | **Identity** | `varia/ChunkCoord`, `ComponentFactory`, `Counter`, `GeometryTools` (with `AxisAlignedBB2D`), `Statistics`, `TimedCache`, `TodoQueue`, `Tools`, `WorldTools`. Config key `cacheCleanupSeconds` ([config.md](config.md)). |
| 2 | **Inputs** | Per helper, below. |
| 3 | **Numbers** | See each section. |
| 4 | **Behavior** | See each section. |
| 5 | **Determinism** | `TimedCache` evicts by wall-clock time; generation stays deterministic only because every cached value is a pure function of its key, the seed, the profile and the assets. `Counter.getMostOccuring` breaks ties by `HashMap` iteration order. `Tools.getRandomFromList` consumes exactly one `nextFloat` per call. |
| 6 | **Data formats** | `Tools.listOrStringList`: a JSON field that is a string or a list of strings ([below](#tools)). Block-state strings `namespace:block[prop=value,…]`. |
| 7 | **Output** | Values to callers. |
| 8 | **Relationships** | `ChunkCoord` is the key type of nearly every cache (`03`, `04`, `06`, `07`). `TimedCache` backs `BiomeInfo`, `MultiChunk`, `City` style cache, `ChunkPlanner` plans and city levels, and `LostCityTerrainFeature` heightmaps. `TodoQueue` backs `GlobalTodo` (`03`). `Tools.stringToState` is used by palettes and variants (`05`); `getRandomFromList` by city, world and city-style selection and conditions (`04`, `05`); `getSeaLevel` by `BuildingInfo`, `Spheres`, `DamageArea`; tag helpers by `LostCityTerrainFeature` and `DamageArea`; `GeometryTools` by `DamageArea` (`03`); `Counter` by `BuildingInfo` and `MultiChunk` (`04`); `Statistics` by `LostCityTerrainFeature` and `/lostcities stats`. |
| 9 | **1.20 dependencies** | `ResourceKey<Level>` in `ChunkCoord`; `BlockStateParser` and `BlockStateData.upgradeBlock` (the 1.13 flattening data fixer); block tags (`TagKey`, `Holder`); `ServerChunkCache.getGenerator().getSeaLevel()`; Mojang `Codec`/`Either`; `StructureManager.getAllStructuresAt`; `ServerLifecycleHooks.getCurrentServer`; `Component`. |
| 10 | **Bugs** | `CORE-20`, `CORE-21`, `CORE-22`, `CORE-23`, `CORE-24` |

## ChunkCoord

A record of dimension key, chunk X and chunk Z, with value equality. Neighbors: `offset(dx, dz)`, `east` (+X), `west` (−X), `north` (−Z), `south` (+Z), the four diagonals, `neighbour(direction)` for the four horizontal directions (any other direction returns the same coordinate), `lower(orientation)` and `higher(orientation)` (−1 or +1 along X or Z), `getCoord(orientation)`. `toString` is `ChunkCoord{dimension=…, chunkX=…, chunkZ=…}`. It is exposed through the API (`ILostSphere.getCenter`).

## TimedCache

A concurrent key-value cache whose entries expire a set time after their last access.

| Item | Value |
|---|---|
| Time-to-live | `cacheCleanupSeconds × 1000` ms (default 300 s); a value of 0 or less means "expire at once" (not reachable through config, whose minimum is 1) |
| Cleanup interval | `max(1000 ms, TTL / 2)` |
| First cleanup | at construction time, so the first access cleans |
| Clock | `System.currentTimeMillis()` |

Operations (server, any thread):
1. `get(key)`: absent → `null`; expired → remove and return `null`; else refresh the access time and return the value.
2. `put(key, value)`: store with the current time.
3. `computeIfAbsent(key, supplier)`: return a live entry if present; otherwise compute it inside the map's atomic `compute`, so the supplier runs while that map bin is locked. A `null` result is not stored. A supplier that touches the same cache can fail with `IllegalStateException: Recursive update` or deadlock with another thread (`CORE-22`).
4. `withPinnedValue(key, supplier, action)`: like `computeIfAbsent`, but an expired entry that is in use is kept rather than recomputed; the entry's use count is raised for the duration of `action`, and cleanup never removes an entry in use.
5. Every operation calls `maybeCleanup`: when the cleanup time has passed and this thread wins the compare-and-set, remove every entry that is expired and not in use.
6. `clear()` drops everything.

## TodoQueue

An unbounded FIFO of `(BlockPos, data)` backed by a `ConcurrentLinkedQueue`. `add`, `get` (poll the data, or `null`), `isEmpty`, `getSize`, `forEach(consumer)` (non-destructive), `forEach(n, consumer)` (poll and run up to `n` items, return how many ran). The per-tick `n` is `todoQueueSize` (default 20) in `GlobalTodo` (`03-terrain`).

## Tools

| Member | Behavior |
|---|---|
| `stateToString(state)` | `namespace:block`, plus `[name=value,…]` in the state's property order when it has properties |
| `stringToState(text)` | With `[`: parse with the vanilla block-state parser against the overworld's block lookup (needs a running server) and rethrow parse errors as runtime exceptions. Without: run the 1.13 flattening fixer on the name (so pre-1.13 names such as `minecraft:stonebrick` upgrade), look the block up and return its default state. The "not found" branch is unreachable because the registry returns air for unknown names (`CORE-20`); a malformed name throws |
| `getRandomFromList(RandomSource, list, weight)` | Empty list → `null`. Sum the weights as `float`; `r = nextFloat() × total`; walk the list subtracting weights and return the first element where `r ≤ 0`; if rounding leaves `r > 0`, return the last element |
| `getRandomFromList(Random, list, weight)` | The same, but returns `null` when rounding leaves `r > 0` (`CORE-21`). With all weights 0, both return the first element |
| `getBlocksForTag(tag)` | The block registry's members of the tag (empty if none) |
| `hasTag(block, tag)` | Whether the block's registry holder is in the tag |
| `getSeaLevel(level)` | From the chunk generator when the level is a `WorldGenLevel` with a server chunk cache, else the level's own sea level |
| `listOrStringList(field, default, getter)` | Codec field accepting either a string or a list of strings; absent → a one-element list with `default`; encodes a one-element list back as a plain string |
| `DONE` | A static set, unused |

## WorldTools

| Member | Behavior |
|---|---|
| `getOverworld()` | The current server's overworld (via `ServerLifecycleHooks`; fails without a server) |
| `getOverworld(level)` | The overworld of the level's server |
| `chunkLoaded(level, pos)` | `level.hasChunkAt(pos)`; false for null arguments. *Unused* |
| `loadWorld(key)`, `getWorld(key)`, `getWorld(level, key)` | Look up a server level. *Unused* |
| `checkStructures(level, coord)` | All structures at a position, via the structure manager. *Unused*. Its position is computed as `chunkX << (4 + 8)` because of operator precedence, not chunk center (`CORE-23`) |

## GeometryTools

Distances for explosion damage (`03-terrain`):
1. `squaredDistanceBoxPoint(box, point)`: squared distance from a point to the nearest point of an axis-aligned box (0 inside), summed per axis.
2. `maxSquaredDistanceBoxPoint(box, point)`: squared distance from the point to the farthest corner; on each axis the far side is chosen by comparing the point with the box's midpoint.
3. A 2D overload of the first on `AxisAlignedBB2D` (`minX`, `minY`, `maxX`, `maxY`, and an unused mutable `height`).

## Counter

`add(key)` increments a count in a `HashMap`; `get(key)` returns it (0 if absent); `getMap()` exposes the map; `getMostOccuring()` returns the key with the highest count, the first one met in `HashMap` order on a tie, or `null` when empty. `BuildingInfo` uses it with city-style name strings to give a street the majority style of its neighbors (`04`).

## Statistics

Keeps the last 100 chunk-generation times (milliseconds, measured with `System.currentTimeMillis` around a chunk's city generation in `LostCityTerrainFeature`) in a ring buffer, plus all-time minimum and maximum. All methods are synchronized.
1. `addTime(t)`: store at `count mod 100`, increment the count, update minimum and maximum.
2. `getAverageTime()`: sum all 100 slots (empty slots are 0) and divide by 100 with integer division before converting to `float`, so it under-reports until 100 samples exist and always truncates (`CORE-24`).
3. `getMinTime()` / `getMaxTime()`: all-time values (`Long.MAX_VALUE` / `Long.MIN_VALUE` before any sample), although the class comment says only the last 100 count.

## ComponentFactory

Thin wrappers: `translatable(key)`, `literal(text)`, `keybind(name)`. Only `literal` is used; every player-visible string in the mod is literal English (`09-client-ui`, lang).

## On 1.12

`upstream/1.12`'s `varia` holds `BlockIndex`, `ChunkCoord` (integer dimension), `Counter`, `CustomTeleporter`, `GeometryTools`, `QualityRandom` and `Tools`; it has no `TimedCache`, `TodoQueue`, `Statistics` or `WorldTools`.

## Deviations in the new mod

Not decided here; see `docs/new-spec/` (area 01-core).
