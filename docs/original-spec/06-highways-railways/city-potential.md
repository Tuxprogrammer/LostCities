# City potential and hub city level

The inputs the intercity planner uses to find hubs and to price routes: a cheap, remote estimate of "how much city" a chunk would have, and the city level at a hub. Neither reads `BuildingInfo` or any generated chunk.

| # | Field | Contents |
|---|---|---|
| 1 | **Identity** | `worldgen/highway/CityPotential` (interface and score scale), `ApproximateCityPotential` (implementation, with its `Modifier`), `HighwayLevelSource` and `HighwayCityLevelSource`. The modifier and the upper-bound factor are supplied by `DefaultDimensionInfo.applyHighwayCityConstraints` and `WorldStyle.getMaximumCityChanceMultiplier` (out of scope, read for context). Profile options `cityChance`, `cityMinRadius`, `cityMaxRadius`, `citySpawnDistance1/2`, `citySpawnMultiplier1/2`, `cityPerlinScale`, `cityPerlinOffset`, `cityPerlinInnerScale`, `cityMinHeight`, `cityMaxHeight`. World-style asset field: the city biome multipliers (`05-assets`). |
| 2 | **Inputs** | World seed, chunk coordinate, the dimension profile, the world style's biome multipliers, the chunk heightmap height and the noise biome at the chunk center. |
| 3 | **Numbers** | See [Numbers](#numbers). |
| 4 | **Behavior** | See [Behavior](#behavior). |
| 5 | **Determinism** | `java.util.Random` seeded from chunk coordinates only (not the world seed) for city centers and radii, matching `City`; `CityRarityMap` seeded by the world seed in noise mode. |
| 6 | **Data formats** | Profile options and world-style biome multipliers; nothing persisted here. |
| 7 | **Output** | A potential 0..1, or an integer score 0..1 000 000, or -1 for "cannot reach the required score"; a city level for a hub chunk. |
| 8 | **Relationships** | Read by [intercity-planner.md](intercity-planner.md) (hub scoring, route penalty). Mirrors `City.getCityFactor` (`04-city-layout`). |
| 9 | **1.20 dependencies** | Heightmap from the 1.20 noise generator; biome from `BiomeSource.getNoiseBiome` with a `Climate.Sampler`; biome predicates on `Holder<Biome>`. |
| 10 | **Bugs** | none found in scope. Cross-area: city centers ignore the world seed (`04-city-layout`). |

## Numbers

| Item | Value | Notes |
|---|---|---|
| Score scale | 1 000 000 | `score(p) = round(clamp(p, 0, 1) × 1 000 000)` |
| Center search offset | `(cityMaxRadius + 15) / 16` chunks | Default `cityMaxRadius` 128 → 8, a 17 × 17 square of candidate centers |
| City-center seed | `chunkZ × 797003437 + chunkX × 295075153` | `java.util.Random`, first `nextDouble()` < `cityChance` |
| City-radius seed | `chunkZ × 100001653 + chunkX × 295075153` | `java.util.Random`, `cityMinRadius + nextInt(max(1, cityMaxRadius − cityMinRadius))` blocks |
| Noise-city mode | `cityChance < 0` | Uses `CityRarityMap(seed, cityPerlinScale, cityPerlinOffset, cityPerlinInnerScale)` |
| Defaults | `cityChance` 0.01, `cityMinRadius` 50, `cityMaxRadius` 128, `cityMinHeight` 50, `cityMaxHeight` 150 | `02-profiles-dimensions` owns these |
| "Effectively zero" | 0.0001 | Below this the modifier is skipped and no upper bound is computed |
| Biome sample point | block `(chunkX × 16 + 8, height, chunkZ × 16 + 8)` | Converted to quart coordinates (`>> 2`) |
| Fallback biome | `minecraft:plains` | When the chunk source is not a `ServerChunkCache` |

## Behavior

### Unmodified potential (both)

1. (both) If `cityChance < 0`, the factor is `CityRarityMap.getCityFactor(chunkX, chunkZ)`.
2. (both) Otherwise, for every chunk in the square of side `2 × offset + 1` around the target: if it is a city center (its seeded `Random`'s first `nextDouble()` is below `cityChance`), get its radius (seeded `Random`, as in the table), measure the block distance between the two chunk origins (both multiplied by 16), and if inside the radius add `(radius − distance) / radius`. The sum can exceed 1.
3. (both) If `citySpawnDistance2 > 0`, multiply by a spawn-distance factor: at block distance from the world origin (chunk × 16) up to `citySpawnDistance1` it is `citySpawnMultiplier1`; from `citySpawnDistance2` on, `citySpawnMultiplier2`; linear in between.

### Modifier (server, `DefaultDimensionInfo`)

4. (server) If the potential is at most 0.0001, return it unchanged.
5. (server) Read the chunk's heightmap height (`ChunkHeightmap.getHeight`, from the generator's terrain noise, `03-terrain`). If it is below `cityMinHeight` or above `cityMaxHeight`, return 0.
6. (server) Sample the noise biome at the chunk center at that height and multiply by the world style's city-chance multiplier for that biome (first matching predicate wins; 1.0 when none match).
7. (both) Clamp to 0..1.

The GUI preview (`NullDimensionInfo`) builds the potential without a modifier and without an upper bound (`09-client-ui`), so its hubs can differ from the server's.

### Upper bound for pruning (server)

`getScoreWithUpperBound(chunk, required)` lets the hub search skip the expensive modifier:

1. (server) Compute the unmodified potential.
2. (server) If it is above 0.0001 and finite, and the world style's maximum biome multiplier is finite and at least 1.0, compute the score of the next float above `potential × maximumMultiplier`. If that score is below `required`, return -1.
3. (server) Otherwise return the score of the modified potential.

The maximum multiplier is the largest multiplier in the world style, at least 1.0, or infinity if any multiplier is not finite (which disables pruning). The bound is sound because the modifier can only multiply by a listed multiplier or return 0, so pruning never changes which sample wins.

### City level at a hub (server)

`HighwayCityLevelSource.getCityLevel(chunkX, chunkZ)` returns `ChunkPlanner.cityLevel` for the chunk: the cached `BuildingInfo.computeCityLevel`, which depends on the landscape type, the heightmap and, with averaging on, raw city membership of sample chunks (`04-city-layout`). It never asks for highways, so there is no cycle.

### Differences from the real city factor

Compared with `City.getCityFactor` (`04-city-layout`), the approximation ignores predefined buildings, streets and cities; ignores the inside/outside profile split of city-sphere worlds; and samples the biome with the noise biome source at the chunk center instead of `BiomeInfo`'s main biome. The order of the height test, biome multiplier, spawn multiplier and clamp gives the same result where the inputs agree.

## Determinism

- City centers and radii use `java.util.Random` seeded only from chunk coordinates with fixed constants, exactly as `City` does. Every world therefore shares the same city-center pattern in `cityChance` mode (a `04-city-layout` matter; noted here because hub positions inherit it). The noise-city mode is seeded with the world seed.
- No caches in this class. The heightmap and city-level caches belong to `03-terrain` and `04-city-layout`.
- Called from planner worker threads ([planning-service-and-persistence.md](planning-service-and-persistence.md)); every input it reads is thread-safe in 1.20.

## Data formats

Profile options (table above); world-style city biome multipliers (`05-assets`). Nothing persisted.

## Output

Potential scores for hub selection and route penalties; hub city levels for route levels.

## Relationships

- Used by [intercity-planner.md](intercity-planner.md).
- Mirrors `City.getCityFactor`, `City.isCityCenter`, `City.getCityRadius` (`04-city-layout`).
- Reads `LostCityTerrainFeature.getHeightmap` (`03-terrain`), `WorldStyle` biome multipliers (`05-assets`).

## 1.20 dependencies

| Mechanism | Where | Why it matters for 1.7.10 |
|---|---|---|
| Terrain height from the 1.20 noise generator (`ChunkGenerator` with `RandomState`) | heightmap behind the modifier and the city level | 1.7.10 has no noise router; under RWG the height must come from RWG without generating chunks. |
| `BiomeSource.getNoiseBiome` with `Climate.Sampler`, quart coordinates | `DefaultDimensionInfo.getBiome` | 1.7.10 uses `WorldChunkManager.getBiomeGenAt`; the sample point and result differ. |
| Biome predicates on `Holder<Biome>` (biome tags) | world-style city multipliers | 1.7.10 has no biome tags; it has `BiomeDictionary` types. |
| Thread-safe samplers | the modifier runs on planner worker threads | 1.7.10 and RWG samplers are not known to be thread-safe. |

## Bugs

None found in scope.

## Deviations in the new mod

None recorded yet; the new spec decides.
