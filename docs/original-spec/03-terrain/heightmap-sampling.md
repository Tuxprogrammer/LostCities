# Heightmap sampling

How upstream learns "how high is the vanilla terrain here" without generating the chunk. Every layout decision in Lost Cities rests on this number, so it is the most important input an RWG overlay has to reproduce.

## 1 · Identity

| Class | Role |
|---|---|
| `worldgen/ChunkHeightmap` | One height per chunk (not per column), plus an optional min and max from four extra samples. |
| `worldgen/LostCityTerrainFeature` (`getHeightmap`, `publishHeightmapSample`, `takePublishedHeightmap`, `generateHeightmap`, `getMinHeightAt`) | The cache and the sampling grid. |
| `worldgen/HeightGenOpt` | Optional faster height query against the noise generator. |
| `worldgen/NoiseChunkOpt` | A trimmed copy of vanilla's `NoiseChunk` (and its `NoiseBasedAquifer`) used only by `HeightGenOpt`. |
| `IDimensionInfo.getHeightmap` (in `DefaultDimensionInfo`) | Public access; delegates to the terrain feature. |

Config keys: `heightSampleSize` (`Config.HEIGHT_SAMPLE_SIZE`), `optimizedHeightmap` (`Config.OPTIMIZED_HEIGHTMAP`), `cacheCleanupSeconds` (`Config.CACHE_CLEANUP_SECONDS`). Profile options: `LANDSCAPE_TYPE`, `GROUNDLEVEL`, `USE_AVG_HEIGHTMAP` (read by 04-city-layout).

## 2 · Inputs

- Chunk coordinates and dimension.
- The dimension's `NoiseBasedChunkGenerator` and its `RandomState` (noise router, aquifer random).
- The level's height accessor (minimum and maximum build height).
- Profile `LANDSCAPE_TYPE` and `GROUNDLEVEL`.

## 3 · Numbers

| Item | Value | Unit / notes |
|---|---|---|
| Sample position | block (8, 8) of the *sampler* chunk | relative to the chunk's minimum corner |
| `heightSampleSize` | default **3**, range 1–100 | chunks per side of a sampling group |
| `optimizedHeightmap` | default `false` | `true` uses `HeightGenOpt` |
| `cacheCleanupSeconds` | default 300, range 1–86 400 | time since last access before an entry expires |
| Height meaning | Y of the first block above the highest motion-blocking block of the column | water and other non-blocking blocks ignored (`OCEAN_FLOOR_WG` semantics) |
| Unset height sentinel | `Short.MIN_VALUE`; any stored value below −4000 reads back as `GROUNDLEVEL` | |
| "Accurate" extra samples | (2, 2), (2, 14), (14, 2), (14, 14) | only `Scattered` asks for these |
| Cavern rule base | `max(GROUNDLEVEL − 20, 1)` | Y |
| Cavern rule cap | samples above Y 100 are ignored; a sample of exactly 100 is stored as 127 | `TERR-22` |
| `NoiseChunkOpt` surface threshold | initial density without jaggedness > 0.390625 | vanilla value, used for aquifer surface estimates |
| Aquifer grid (copied from vanilla) | cells 16 × 12 × 16 blocks; jitter 10 × 9 × 10; similarity divisor 25 | blocks |

City levels derived from the height (computed by 04-city-layout, listed here because they define the interface):

| Height H (predicted) | `cityLevel` | City ground Y (with `GROUNDLEVEL` 71) |
|---|---|---|
| H < 75 | 0 | 71 |
| 75 ≤ H < 83 | 1 | 77 |
| 83 ≤ H < 91 | 2 | 83 |
| 91 ≤ H < 99 | 3 | 89 |
| 99 ≤ H < 107 | 4 | 95 |
| 107 ≤ H < 115 | 5 | 101 |
| 115 ≤ H < 123 | 6 | 107 |
| 123 ≤ H < 131 | 7 | 113 |
| H ≥ 131 | 8 | 119 |

Thresholds are `CITY_LEVEL0_HEIGHT` … `CITY_LEVEL7_HEIGHT` (defaults above; profiles change them, for example `space` uses 60, 66, 72, 78, 86, 94, …). Sphere worlds and floating and cavern worlds compute levels differently (04-city-layout).

## 4 · Behavior

### `getHeightmap(chunk)` (server)

1. Work out the sampling group. With `heightSampleSize` N > 1:
   - `top = (chunkX / N) × N` and `left = (chunkZ / N) × N`, using Java's truncating division;
   - `constX` is −1 for negative `chunkX`, else +1; `constZ` likewise;
   - the sampler chunk is `(top + (N / 2) × constX, left + (N / 2) × constZ)`.

   Because the division truncates toward zero, the groups next to the axes on the negative side are one chunk narrower: with N = 3, chunk X −1 and −2 form a group sampled at −1, while 0, 1, 2 form a group sampled at 1. With N = 1 the sampler is the chunk itself.
2. If this chunk's entry is in the cache and not expired, return it.
3. Otherwise claim the sampler: register a pending future for the sampler coordinate. If another thread already owns it, wait for that result and return a *copy* stored under this chunk's key (or the entry that appeared meanwhile).
4. As the owner: check the cache once more; if still missing, create a `ChunkHeightmap` (type, `GROUNDLEVEL`), sample the sampler chunk's (8, 8) column (below), then publish:
   - N = 1: store the object under this chunk.
   - N > 1: store a separate copy under every chunk of the N × N group, skipping keys on the X = 0 or Z = 0 axis when the group is on the negative side, so the positive group owns the axis.
5. Return the heightmap object itself. With N > 1 that object is *not* the one in the cache (`TERR-10`).

### Sampling one column (server)

1. Ask the generator for the base height at (chunk × 16 + 8, chunk × 16 + 8):
   - default: `ChunkGenerator.getBaseHeight(x, z, OCEAN_FLOOR_WG, …)`, which runs vanilla's noise column iteration;
   - `optimizedHeightmap = true`: `HeightGenOpt.getBaseHeight`, which builds a one-cell `NoiseChunkOpt` and walks the column from the top cell down, returning the first Y + 1 whose interpolated state blocks motion, or the minimum build height if none.
2. Feed the value to `ChunkHeightmap.update(y)`:
   - only a value greater than the stored one replaces it;
   - for `CAVERN` and `CAVERNSPHERES`, values outside `[max(GROUNDLEVEL − 20, 1), 100]` are ignored and 100 is stored as 127. The rule was written for 1.12, where every block of a column was fed in; with one sample per chunk it mostly leaves the height unset, which reads back as `GROUNDLEVEL` (`TERR-22`).

### What `NoiseChunkOpt` keeps and drops

It mirrors vanilla 1.20 `NoiseChunk` with these differences: only the barrier, fluid-level floodedness, fluid-level spread, erosion, depth, initial-density and final-density functions are wired (lava, temperature, vegetation, continents, ridges and the three ore-vein functions are left out); the material rule list has only the aquifer step (no ore veins); the beardifier is a constant 0 (as in vanilla's own height query); the blender is empty; the aquifer has a single fluid level (sea level, default fluid), so the deep lava level is missing. A commented-out debug block compares the two methods and prints differences over 4 blocks, so the two can disagree.

### `calculateAccurateHeight` (server)

Samples the four positions in Numbers, with the same method switch, and stores min and max over those four and the stored height. Used only by `Scattered` (07-scattered). It mutates the cached object.

### `getMinHeightAt(info, x, z, heightmap)` (server)

For a column on the chunk's edge, returns the smaller of this chunk's height and the neighbor's height across that edge (the diagonal neighbor at corners); for an interior column, this chunk's height. Used by borders under streets and buildings.

### Mutation by terrain correction

`correctTerrainShape` writes the highest Y it touched at column (8, 8) back into the chunk's heightmap with `setHeight` ([terrain blend](terrain-blend.md)). After that, readers of that chunk's height may see the corrected value or the raw one depending on order and cache expiry (`TERR-10`).

## 5 · Determinism

- The sampled value is a pure function of the seed, the dimension's noise settings and the coordinates, so it does not depend on generation order.
- Order and time dependence come from the cache: `correctTerrainShape` mutates entries; entries expire 300 s after their last access, so a later read can recompute a raw value; with N > 1 the first requester's object is uncached. City levels themselves are memoized separately by 04-city-layout (`ChunkPlanner.cityLevel`).
- `optimizedHeightmap` changes heights, so the same seed gives a different world when the option is flipped (`TERR-34`).

## 6 · Data formats

None (no JSON, no NBT). The cache is memory only.

## 7 · Output

A `ChunkHeightmap` per chunk: `getHeight()` (one Y), and after `calculateAccurateHeight` also `getMinHeight()` and `getMaxHeight()`.

## 8 · Relationships

- Read by 04-city-layout (`BuildingInfo.getCityLevelNormal`, `getCityLevelFloating`, `isVoidChunk`, `City` at `City.java:360`, `BiomeInfo`, `HierarchicalBridgePlanner`), 06-highways-railways (`DefaultDimensionInfo.applyHighwayCityConstraints`, which rejects chunks below `CITY_MINHEIGHT` 50 or above `CITY_MAXHEIGHT` 150), 07-scattered, and in this area by borders, `doBorder`, `clearToMax`, `generateBorderSupport` and `correctTerrainShape`.
- Debug output: `/lostcities debug` (01-core).

## 9 · 1.20 dependencies

- `ChunkGenerator.getBaseHeight` with `Heightmap.Types.OCEAN_FLOOR_WG`, the noise router, density functions, `RandomState`, `NoiseSettings` cell sizes, `NoiseBasedAquifer`: the whole of `HeightGenOpt` and `NoiseChunkOpt` exists only for 1.18+ noise generation. 1.7.10 has no "height without generating" query on any chunk provider, and RWG has its own terrain functions.
- The access transformer opens `NoiseChunk.getInterpolatedState`, `NoiseChunk$NoiseChunkDensityFunction`, `DensityFunctions$Marker` and its type and constructor for these classes.
- `getMinBuildHeight` as the "no surface" fallback (−64 in the overworld).

## 10 · Bugs

`TERR-10`, `TERR-22`, `TERR-34`.

## On 1.12

`dimensions/world/ChunkHeightmap` (1.12) stored one byte per column (256 values, 0–255). `LostCityChunkGenerator.getHeightmap` generated the chunk's full terrain primer with its own noise generator, cached the primer (so the real chunk later reused it) and scanned every column: from Y 255 down to the first non-air block for `DEFAULT`, to the first base block for `SPACE` (ignoring sphere glass), and inside the cavern band for `CAVERN`. Callers used the average or minimum of five columns ((2, 2), (13, 2), (2, 13), (13, 13), (8, 8)), ignoring values ≤ 5. That approach, a cached primer generated ahead of time and scanned per column, is the closer model for a 1.7.10 chunk provider.

## Deviations in the new mod

None recorded yet. The heightmap source for RWG is spike S2's decision.
