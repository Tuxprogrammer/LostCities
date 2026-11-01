# Scattered structures

Single buildings and multibuildings placed outside cities: cabins, radio towers, oil rigs and, next to highways, gas stations and restaurants. The world is cut into square areas; each area holds at most one structure, planned once and then written chunk by chunk. Source: `worldgen/gen/Scattered.java`, `worldgen/lost/cityassets/ScatteredBuilding.java`, `worldgen/lost/regassets/data/ScatteredSettings.java`, `ScatteredReference.java`, the bundled `data/lostcities/lostcities/scattered/*.json`; codec defaults from `regassets/ScatteredRE.java` and the call site in `LostCityTerrainFeature.doNormalChunk` (out of scope, read for behavior), on `f18a094d`.

## 1. Identity

| Class / member | Role |
|---|---|
| `Scattered.generateScattered` | Per chunk: find the area's plan and write this chunk's share |
| `Scattered.avoidScattered` | Chunks that never take a scattered structure |
| `Scattered.calculatePlan` | Chance, selection, placement and height for one area |
| `Scattered.findHighwayPlacements`, `getConnectedHighwayHeight` | Placement beside a highway |
| `Scattered.calculateTerrainStats` | Biome, void and slope checks; min/max/average height |
| `Scattered.generateScatteredBuilding` | Floors, parts, terrain fix |
| `Scattered.getHighwayRailingOpenings` | Tells the highway generator where to leave the railing open |
| `ScatteredPlan`, `PLANS` | Plan record and its unbounded cache, keyed by dimension, seed and area |
| `ScatteredBuilding` | Runtime scattered asset: buildings or multibuilding, terrain height and fix, offset, rotatable, railing, support part |
| `ScatteredSettings` | World style `scattered`: `areasize`, `chance`, `weightnone`, `list` |
| `ScatteredReference` | One list entry: `name`, `weight`, `nearhighway`, `allowvoid`, `biomes`, `maxheightdiff` |

Profile: `SCATTERED_CHANCE_MULTIPLIER`, `GROUNDLEVEL`, landscape type.

## 2. Inputs

Seed, dimension, chunk coordinates, world style scattered settings, scattered and building assets, biomes, heightmaps and generator noise heights, `BuildingInfo` of every candidate chunk (city, bridge), highway info (`06`), the chunk being generated.

## 3. Numbers

| Item | Value | Unit |
|---|---|---|
| `SCATTERED_CHANCE_MULTIPLIER` | 1.0 (0..100) | multiplies the area chance |
| Area normalization offset | 2,000,000 | chunks, added before integer division |
| Plan seed | `seed + ax * 5564338337 + az * 25564337621` | `QualityRandom` |
| Void threshold | heightmap height `<= minBuildHeight + 3` | block Y; skipped for `allowvoid`, default and cavern landscapes |
| Fallback height | `GROUNDLEVEL` (cavern) or `minBuildHeight + 2` | block Y, when the level is below -4000 (unreachable: see § 4) |
| `CLEAR` height | 50 | blocks above the lowest level |
| Accurate-height sample points | chunk offsets (2, 2), (2, 14), (14, 2), (14, 14) | blocks |

World style values:

| World style | `areasize` | `chance` | `weightnone` | List (weight) |
|---|---|---|---|---|
| `standard` | 8 | 0.7 | 30 | radiotower 15, oilrig 4, cabin 10, highway_gas_station 12, highway_restaurant 9 |
| `standard_everywhere` | 8 | 0.1 | 20 | radiotower 15, oilrig 1, cabin 10, highway_gas_station 12, highway_restaurant 9 |

Per-entry filters (both world styles): radiotower `maxheightdiff` 3, cabin 2, oilrig 100, gas station and restaurant 12 with `nearhighway: true`. Every land entry excludes `#minecraft:is_ocean`, `#is_river`, `#is_beach`; oilrig requires `#minecraft:is_deep_ocean`.

### Bundled scattered assets

| Asset | Building(s) | Terrain height | Terrain fix | Height offset | Rotatable | Clear railing | Support part |
|---|---|---|---|---|---|---|---|
| `cabin` | building `cabin` | `highest` | `none` | -3 | no | no | none |
| `radiotower` | building `radiotower` | `highest` | `none` | -3 | no | no | none |
| `oilrig` | multibuilding `oilrig` | `ocean` | `repeatslice` | 1 | no | no | none |
| `highway_gas_station` | building `highway_gas_station` | `highest` | `repeatslice` | 0 | yes | yes | `roadside_supports` |
| `highway_restaurant` | multibuilding `highway_restaurant` | `highest` | `repeatslice` | 0 | yes | yes | `roadside_supports` |

## 4. Behavior

All (server).

### When a chunk is considered

`doNormalChunk` (non-city chunks, `03-terrain`) runs bridges and highways first, then, if the world style has `scattered` settings and `avoidScattered` is false, `generateScattered`. `avoidScattered` is true for raw city chunks, chunks with a bridge on either axis, and chunks with a highway.

### `generateScattered`

1. Area index `ax = (chunkX + 2000000) / areasize`, `az` likewise (integer division of a positive number).
2. Get or compute the area's plan (`PLANS`, never evicted).
3. If the plan is invalid or this chunk is outside its footprint, stop.
4. Recreate the generation random by replaying the plan's draws (below).
5. Building: the single building chosen by the plan, or, for a multibuilding, the building at this chunk's position mapped back through the rotation (`ROTATE_90`: original `(z, dimZ - 1 - x)`; `ROTATE_180`: `(dimX - 1 - x, dimZ - 1 - z)`; `ROTATE_270`: `(dimX - 1 - z, x)`).
6. `generateScatteredBuilding` with the plan's lowest level, terrain fix, support part and transform.

### `calculatePlan`

1. `rand = QualityRandom(seed + ax * 5564338337 + az * 25564337621)`.
2. Invalid if `rand.nextFloat() >= chance * SCATTERED_CHANCE_MULTIPLIER`.
3. Area origin `(ax * areasize - 2000000, az * areasize - 2000000)`.
4. **Selection** (`selectRandomScattered`), using the biome of the **area origin chunk** (SCAT-07):
   1. Keep list entries whose biome filter matches (no filter matches everything). None kept: invalid.
   2. `bound = sum of kept weights + weightnone`; `r = nextInt(bound)` (throws when 0: SCAT-06).
   3. Walk the kept entries in list order: the first with `r <= weight` wins; else `r -= weight`. Nothing wins: invalid ("none"). The `<=` gives the first entry one extra chance and "none" one fewer (SCAT-05).
5. Load the scattered asset and its multibuilding (if any). Footprint `dimX × dimZ` (1 × 1 for a single building). Invalid if larger than the area.
6. **Placement**:
   - `nearhighway` entries: collect every candidate over transforms (`ROTATE_NONE` only, or all four in order none, 90, 180, 270 when `rotatable`), X ascending, Z ascending, with the footprint swapped for 90 and 270. A candidate needs a connected highway height (below) and valid terrain stats. None: invalid. Pick `nextInt(count)`.
   - Others: `x = origin + nextInt(areasize - dimX + 1)`, `z = origin + nextInt(areasize - dimZ + 1)`, no rotation; invalid if the terrain stats fail.
7. Single building: if the asset lists one building, use it; if several, `nextInt(count)`; none throws `"Missing buildings for scattered …"`.
8. **Lowest level** (block Y of the first floor):
   - highway placement: highway height + `heightoffset`;
   - single building: `ocean` uses the generator's sea level; `lowest`, `average` and `highest` all use the chunk heightmap height (SCAT-09); then + `heightoffset`. A result below -4000 falls back to `GROUNDLEVEL` (cavern) or `minBuildHeight + 2`; the heightmap never reports below -4000, so this fallback is unreachable;
   - multibuilding: `lowest` = footprint minimum, `average` = footprint average, `highest` = footprint maximum, `ocean` = sea level; + `heightoffset`.
9. Store the plan with the seed and every bound needed to replay the draws.

### Connected highway height (`getConnectedHighwayHeight`)

The building's front faces the highway: `ROTATE_NONE` north, `ROTATE_90` east, `ROTATE_180` south, `ROTATE_270` west. For every chunk just outside that edge: the highway level along the edge's axis (X highway for north and south edges, Z highway for east and west) must exist, must not be a tunnel there, and `groundLevel + level * 6` must be the same for the whole edge. That common Y is the result.

### Terrain stats (`calculateTerrainStats`)

For every footprint chunk:
1. The entry's biome filter must match the chunk's main biome.
2. `avoidScattered` must be false.
3. Heightmap height `h`; compute accurate min/max from `h` and four generator noise heights at the chunk's sample points (`calculateAccurateHeight` mutates the shared heightmap object: SCAT-12).
4. Unless `allowvoid` or the landscape is default or cavern, reject when `h <= minBuildHeight + 3`.
5. Track the minimum of the min heights, maximum of the max heights, and the sum of `h`.

Reject when `max - min > maxheightdiff` (if set). Average = sum / chunk count.

### `generateScatteredBuilding`

1. Floors: `min = building.minfloors` (values `<= 0` become 1), `max` likewise; `min >= max` gives `min`, else `min + nextInt(max - min + 1)`.
2. For `f = 0 .. floors - 1`, starting at the lowest level:
   1. Condition context with level = the lowest level's **block Y**, floor `f`, no cellars, `floors` above ground, part `<none>`, the previous floor's part as below-part; `isBuilding = true`; biome at the chunk center. `isTopOfBuilding` (`floor >= floors`) is never true, so top-only parts are never chosen (SCAT-10, marked TODO upstream).
   2. Part = `getRandomPart` (one `nextInt`), part2 = `getRandomPart2` with the same context (one `nextInt`; `null` allowed). City buildings use a second context naming the chosen part (SCAT-11).
   3. First floor only, terrain fix:
      - `none`: nothing;
      - `clear`: air in every column from the lowest level up to 49 blocks above it;
      - `repeatslice`: take the support part (or this floor's part if none is named), and for every column whose bottom slice is not a space, start one block below the lowest level (rotated position) and write that bottom-slice block downward while the existing block is exactly default air or the profile's liquid. No lower bound (SCAT-08).
   4. Generate the part with the plan's transform; the returned height is the floor part's top. Part2 is generated at that returned height, one part above the floor it belongs to (SCAT-13). City buildings place part2 at the floor's own height.

### Highway railing openings (`getHighwayRailingOpenings`, for `06`)

For a highway chunk at a given height: for each horizontal neighbor, compute that neighbor's area plan (computing it if needed). The side is open when the plan is valid, its asset has `clearhighwayrailing`, its connected highway height equals this height, the neighbor is inside its footprint, and this highway chunk lies on the edge the building faces.

## 5. Determinism

| Random | Seed | Draws |
|---|---|---|
| Plan (`QualityRandom`) | `seed + ax * 5564338337L + az * 25564337621L` | `nextFloat` (chance); `nextInt(bound)` (selection); `nextInt(candidates)` or two `nextInt` (position); `nextInt(buildings)` if more than one |
| Generation | the plan seed, replayed through the same draws | then `nextInt` floors (if a range), and per floor `nextInt` part and `nextInt` part2 (when candidates exist) |

Every chunk of a multibuilding replays the same stream, so all chunks agree on the floor count. Plans are computed lazily by whichever chunk asks first; they read `BuildingInfo` and heightmaps of chunks across the area (possibly far from the generating chunk) and generator noise, which are deterministic apart from SCAT-12. `PLANS` is never evicted (SCAT-14).

## 6. Data formats

`scattered/*.json` (`ScatteredRE`): `buildings` (list of building names, optional), `multibuilding` (string, optional), `rotatable` (bool, default false), `clearhighwayrailing` (bool, default false), `supportpart` (string, optional), `terrainheight` (`lowest`, `average`, `highest`, `ocean`; required), `terrainfix` (`none`, `clear`, `repeatslice`; required), `heightoffset` (int, default 0).

World style `scattered` (`ScatteredSettings`): `areasize` (int), `chance` (float), `weightnone` (int), `list` (required). `totalweight` is computed over the whole list but unused by selection (which sums only biome-matching entries).

List entry (`ScatteredReference`): `name` (string), `weight` (int), `nearhighway` (bool, default false), `allowvoid` (bool, default false), `biomes` (biome matcher, optional), `maxheightdiff` (int, optional, blocks). No NBT.

## 7. Output

Scattered buildings written into non-city chunks; railing openings for highways.

## 8. Relationships

- Called by: `LostCityTerrainFeature.doNormalChunk` (`03`); `Highways` for railing openings (`06`).
- Calls: `BuildingInfo.getBuildingInfo`, `isCityRaw`, `hasBridge` (`04-city-layout`), `Highway.getHighwayInfo`, `hasHighway` (`06`), `BiomeInfo`, `ChunkHeightmap`, `generatePart`, `clearRange`, `computePalette` (`03`), `AssetRegistries` (`05`).

## 9. 1.20 dependencies

| Mechanism | Where | Why it matters for 1.7.10 |
|---|---|---|
| Biome tags `#minecraft:is_ocean`, `#is_deep_ocean`, `#is_river`, `#is_beach` in world-style filters | selection, terrain stats | `BiomeDictionary` types in 1.7.10; filters need translating |
| `ChunkGenerator.getBaseHeight` with `OCEAN_FLOOR_WG`, `NoiseBasedChunkGenerator`, `RandomState` | `calculateAccurateHeight` | No equivalent; RWG height sampling (`03-terrain`) |
| Generator sea level via `ServerChunkCache` cast | `ocean` terrain height | Sea level 63 or RWG's in 1.7.10 |
| World minimum height -64 | void threshold, fallback | 0 in 1.7.10 |
| `Direction.Plane.HORIZONTAL`, records | railing openings, plan types | Java 8 classes in a port |
| Data-pack registries for scattered assets and world styles | asset loading | From the jar |

## 10. Bugs

SCAT-05, SCAT-06, SCAT-07, SCAT-08, SCAT-09, SCAT-10, SCAT-11, SCAT-12, SCAT-13, SCAT-14, SCAT-15.

## Deviations in the new mod

None recorded yet.
