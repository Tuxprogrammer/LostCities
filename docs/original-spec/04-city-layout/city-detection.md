# City detection and rarity

Which chunks belong to a city. Two modes exist: **city centers** (a random chunk becomes the center of a circular city) and **Perlin rarity** (a noise field decides). Both produce a *city factor* between 0 and 1 per chunk; a chunk is a raw city chunk when its factor exceeds `CITY_THRESHOLD`. Source: `worldgen/lost/City.java`, `worldgen/lost/CityRarityMap.java`, and `BuildingInfo.isCityRaw`, `isVoidChunk`, `getProfile` in `worldgen/lost/BuildingInfo.java`, all on `f18a094d`.

## 1. Identity

| Class / member | Role |
|---|---|
| `City.isCityCenter` | Is this chunk the center of a city? |
| `City.getCityRadius` | Radius in blocks of the city centered on this chunk |
| `City.getCityFactor` | The 0..1 city factor of a chunk |
| `City.getCityRarityMap`, `CityRarityMap` | Perlin rarity field (when `CITY_CHANCE < 0`) |
| `BuildingInfo.isCityRaw` | Raw city membership: factor above threshold, minus void, sphere border and monorail station chunks |
| `BuildingInfo.getProfile` | Which profile governs a chunk (inside or outside a city sphere) |
| `BuildingInfo.isVoidChunk` | Floating-island worlds: no city over void |

Profile options: `CITY_CHANCE` (`cityChance`), `CITY_MINRADIUS`, `CITY_MAXRADIUS`, `CITY_THRESHOLD`, `CITY_PERLIN_SCALE`, `CITY_PERLIN_INNERSCALE`, `CITY_PERLIN_OFFSET`, `CITY_MINHEIGHT`, `CITY_MAXHEIGHT`, `CITY_SPAWN_DISTANCE1/2`, `CITY_SPAWN_MULTIPLIER1/2`. Asset: the world style's biome city-chance multipliers (`WorldStyle.getCityChanceMultiplier`, format in `05-assets`). Predefined cities, buildings and streets ([predefined-content.md](predefined-content.md)).

## 2. Inputs

World seed (Perlin mode only), chunk coordinates and dimension, the active profile and the outside profile (city-sphere worlds), the chunk heightmap (`03-terrain`), the chunk's main biome, predefined city assets, city-sphere geometry (`03-terrain`).

## 3. Numbers

| Constant / option | Default | Range | Unit | Meaning |
|---|---|---|---|---|
| `CITY_CHANCE` | 0.01 | -1..1 | probability per chunk | Chance a chunk is a city center. Any negative value selects Perlin mode |
| `CITY_MINRADIUS` | 50 | 1..2000 | blocks | Smallest city radius |
| `CITY_MAXRADIUS` | 128 | 1..2000 | blocks | Largest city radius (exclusive in the draw) |
| Radius range guard | `max(1, MAX - MIN)` | | blocks | Inside profile only (see CITY-04) |
| `CITY_THRESHOLD` | 0.2 | 0..1 | factor | A chunk is city when its factor is strictly above this |
| `CITY_PERLIN_SCALE` | 3 | ±1,000,000 | chunks | Noise input is `chunk / scale` |
| `CITY_PERLIN_INNERSCALE` | 0.1 | ±1,000,000 | | Noise output multiplier |
| `CITY_PERLIN_OFFSET` | 0.1 | ±1,000,000 | | Subtracted after scaling |
| Perlin octaves | 4 | fixed | | `PerlinNoiseGenerator14(seed, 4)` |
| `CITY_MINHEIGHT` | 50 | -1024..2048 | block Y | Factor forced to 0 when the chunk heightmap height is below |
| `CITY_MAXHEIGHT` | 150 | -1024..2048 | block Y | Factor forced to 0 when the height is above |
| `CITY_SPAWN_DISTANCE1` | 0 | 0..10,000,000 | blocks | Distance from block (0, 0) where multiplier 1 applies |
| `CITY_SPAWN_DISTANCE2` | 0 | 0..10,000,000 | blocks | Distance where multiplier 2 applies; 0 disables the feature |
| `CITY_SPAWN_MULTIPLIER1/2` | 1.0 / 1.0 | 0..1 | | Factor multipliers at the two distances |
| Center search radius | `(CITY_MAXRADIUS + 15) / 16` = 8 | | chunks | Square half-width searched for centers |
| Factor epsilon | 0.0001 | fixed | | Height and biome checks only run above it |
| Center seed | `chunkZ * 797003437 + chunkX * 295075153` | | | `java.util.Random`, one `nextDouble` |
| Radius seed | `chunkZ * 100001653 + chunkX * 295075153` | | | `java.util.Random`, one `nextInt` |

## 4. Behavior

All steps are (server); the GUI preview runs the same code against a `NullDimensionInfo` with no world (both).

### Profile of a chunk (`BuildingInfo.getProfile`)

1. If the main profile is `space` or `spheres` and the chunk intersects a city sphere, use the main profile; if it does not, use the outside profile.
2. Otherwise always the main profile.

### City center (`City.isCityCenter`)

1. A chunk holding a predefined city's center is always a center.
2. Seed a `java.util.Random` from the center seed above (chunk coordinates only, no world seed: CITY-01).
3. `space`/`spheres` worlds: look up the city sphere for the chunk.
   - No enabled sphere: center when `nextDouble() < outsideProfile.CITY_CHANCE`.
   - The chunk is the sphere's center chunk: center when `nextDouble() < profile.CITY_CHANCE`.
   - Any other chunk inside a sphere: never a center.
4. Other worlds: center when `nextDouble() < profile.CITY_CHANCE`. With a negative `CITY_CHANCE` this is never true; Perlin mode does not use centers.

### City radius (`City.getCityRadius`)

1. Predefined city at this chunk: its `radius`.
2. Seed a `java.util.Random` from the radius seed (no world seed: CITY-01).
3. `space`/`spheres` worlds: if the chunk intersects a city sphere, `CITY_MINRADIUS + nextInt(max(1, MAX - MIN))` from the main profile; otherwise `MIN + nextInt(MAX - MIN)` from the outside profile, with no guard (CITY-04).
4. Other worlds: `CITY_MINRADIUS + nextInt(max(1, MAX - MIN))`. The result lies in `[MIN, MAX)` blocks.

### City factor (`City.getCityFactor`)

1. Predefined overrides, each returning 1.0 at once (no height, biome or spawn-distance adjustment):
   - a predefined building has its top-left at this chunk;
   - a predefined street is at this chunk;
   - a predefined **multi**building has its top-left at the chunk to the west, north-west or north. Only those three neighbors are checked, so only a 2 × 2 footprint is covered (CITY-07).
2. Perlin mode (`CITY_CHANCE < 0`): the dimension's rarity map gives `max(0, noise(chunkX / scale, chunkZ / scale) * innerScale - offset)`. The map is created once per dimension with the first caller's parameters (CITY-05).
3. Center mode: for every chunk `c` in the square of half-width `(CITY_MAXRADIUS + 15) / 16` around this chunk:
   1. skip it unless `getProfile(c)` is the same profile object as this chunk's profile;
   2. skip it unless `isCityCenter(c)`;
   3. `r = getCityRadius(c)`; `d` = distance in blocks between the north-west corners of `c` and this chunk;
   4. if `d² < r²`, add `(r - d) / r` to the factor.

   Overlapping cities add up, so the factor can pass 1 before the final clamp.
4. If the factor is above 0.0001 and a world exists: read the chunk heightmap. No heightmap gives 0. A height below `CITY_MINHEIGHT` or above `CITY_MAXHEIGHT` gives 0.
5. If the factor is still above 0.0001 and a world exists: multiply by the world style's city-chance multiplier for the chunk's main biome (first matching biome rule; 1.0 if none matches).
6. If `CITY_SPAWN_DISTANCE2 > 0`: `dist` = distance in blocks from block (0, 0) to the chunk's north-west corner (the world origin, not the actual spawn point). Multiply by `MULTIPLIER1` when `dist <= DISTANCE1`, by `MULTIPLIER2` when `dist >= DISTANCE2`, and by the linear interpolation between them otherwise.
7. Clamp to `[0, 1]`.

### Raw city membership (`BuildingInfo.isCityRaw`)

1. A void chunk is never city. Void means: the profile is `floating` and the chunk heightmap height is `<= 0`.
2. `space`/`spheres` worlds: a chunk on a city sphere's border, or holding a monorail station, is never city.
3. Otherwise city when `getCityFactor(chunk, profile) > profile.CITY_THRESHOLD`.

`isCityRaw` is the uncached test. The final, cached city flag (after structure avoidance and the characteristics event) is in [chunk-characteristics.md](chunk-characteristics.md); the BuildingInfo can still change it for city-sphere centers ([building-info.md](building-info.md)).

## 5. Determinism

| Random | Seed | Draws | World seed? |
|---|---|---|---|
| Center | `chunkZ * 797003437L + chunkX * 295075153L` | 1 `nextDouble` | **No** (CITY-01) |
| Radius | `chunkZ * 100001653L + chunkX * 295075153L` | 1 `nextInt` | **No** (CITY-01) |
| Perlin rarity | world seed, 4 octaves | pure function | Yes |

- With the default `CITY_CHANCE = 0.01`, every world (and every dimension) has city centers and radii at the same chunk coordinates. Only the heightmap, biome multiplier and terrain differ between seeds. On 1.12 (`dimensions/world/lost/City.java`) the same seeds included `provider.seed`; the 1.20 code dropped it.
- The rarity map is cached per dimension key with the parameters of the first call (CITY-05). In a city-sphere world the inside and outside profiles share one map.
- No order dependence otherwise: each answer is a function of coordinates, profile, assets, heightmap and biome. The heightmap itself is cached and sampled (`03-terrain`); see CITY-34 for the averaged-heightmap grid.

## 6. Data formats

No JSON is read directly. Profile options are listed above (format in `02-profiles-dimensions`). Predefined city fields are in [predefined-content.md](predefined-content.md). No NBT or saved data.

## 7. Output

A float city factor in `[0, 1]` and a boolean raw city flag per chunk, handed to [chunk-characteristics.md](chunk-characteristics.md), the floor-count formula in [building-info.md](building-info.md), [multibuildings.md](multibuildings.md), [bridges.md](bridges.md), [city-level.md](city-level.md) (averaged heightmap) and `07-scattered` (scattered structures avoid raw city chunks).

## 8. Relationships

- Called by: `BuildingInfo.computeChunkCharacteristics`, the `BuildingInfo` constructor (floors), `getEffectivePlannedRoadType`, `HierarchicalBridgePlanner`, `MultiChunk.canPlaceBuilding`, `Scattered.avoidScattered`, `getCityLevelNormal`, the highway planners (`06-highways-railways`), `BuildingInfo.getCityInfo` (API).
- Calls: `CitySphere` (`03-terrain`), `IDimensionInfo.getHeightmap` (`03-terrain`), `WorldStyle.getCityChanceMultiplier` (`05-assets`), `AssetRegistries` predefined loaders (`05-assets`).

## 9. 1.20 dependencies

| Mechanism | Where | Why it matters for 1.7.10 |
|---|---|---|
| Height defaults tuned for a -64..320 world with sea level 63 | `CITY_MINHEIGHT` 50, `CITY_MAXHEIGHT` 150 | 1.7.10 is 0..255 and RWG has its own height profile; the window must be re-derived |
| `ResourceKey<Level>` as dimension identity | `ChunkCoord`, `CITY_RARITY_MAP` keys | 1.7.10 dimensions are integer IDs |
| `Holder<Biome>` main biome and biome predicates | biome multiplier | 1.7.10 `BiomeGenBase` and `BiomeDictionary` types instead of biome tags |
| Data-pack registry for predefined cities | `AssetRegistries.loadPredefinedStuff` | No data packs in 1.7.10 |
| Noise heightmap (`ChunkGenerator.getBaseHeight`) | height window | RWG terrain; see `03-terrain` |

## 10. Bugs

CITY-01, CITY-04, CITY-05, CITY-07, CITY-34 (heightmap sample grid; see [city-level.md](city-level.md)), CITY-39 (spawn-distance overflow), CITY-40 (predefined radius cut at the search square).

## Deviations in the new mod

None recorded yet; the new spec decides.
