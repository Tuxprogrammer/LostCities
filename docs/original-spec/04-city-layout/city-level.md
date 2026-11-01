# City level

The *city level* is an integer 0..8 per chunk. A chunk's street or building ground floor sits at `groundLevel + cityLevel * 6` (block Y), so a city steps up the terrain in 6-block floors. Source: `BuildingInfo.computeCityLevel` and its helpers (`worldgen/lost/BuildingInfo.java:1210-1327`), `ChunkPlanner.cityLevel` (`worldgen/plan/ChunkPlanner.java`), on `f18a094d`.

## 1. Identity

| Member | Role |
|---|---|
| `BuildingInfo.getCityLevel` | Cached city level (`ChunkPlanner.cityLevel`) |
| `BuildingInfo.computeCityLevel` | Uncached; dispatches on landscape type |
| `getCityLevelNormal`, `getCityLevelFloating`, `getCityLevelCavern`, `getCityLevelSpace` | Per landscape type |
| `getLevelBasedOnHeight` | Height-to-level table |
| `getAverageCityLevel`, `getTopLeftCityLevel` | Multibuilding level |
| `getCityGroundLevel`, `getCityGroundLevelOutsideLower`, `getMaxHeight` | Derived block heights |
| `LostCityTerrainFeature.FLOORHEIGHT` | 6 blocks per level (`03-terrain`) |

Profile options: `CITY_LEVEL0_HEIGHT` … `CITY_LEVEL7_HEIGHT`, `USE_AVG_HEIGHTMAP`, `MULTI_USE_CORNER`, `GROUNDLEVEL`, `LANDSCAPE_TYPE`. Server config: `heightSampleSize` (`Config.HEIGHT_SAMPLE_SIZE`).

## 2. Inputs

Chunk heightmap height (`03-terrain`), profile, landscape type, city-sphere geometry (space worlds), seed (space worlds), raw city membership of sample neighbors (averaged mode), the multibuilding footprint.

## 3. Numbers

| Option | Default | Range | Unit |
|---|---|---|---|
| `CITY_LEVEL0_HEIGHT` | 75 | 1..384 | block Y |
| `CITY_LEVEL1_HEIGHT` | 83 | 1..384 | block Y |
| `CITY_LEVEL2_HEIGHT` | 91 | 1..384 | block Y |
| `CITY_LEVEL3_HEIGHT` | 99 | 1..384 | block Y |
| `CITY_LEVEL4_HEIGHT` | 107 | 1..384 | block Y |
| `CITY_LEVEL5_HEIGHT` | 115 | 1..384 | block Y |
| `CITY_LEVEL6_HEIGHT` | 123 | 1..384 | block Y |
| `CITY_LEVEL7_HEIGHT` | 131 | 1..384 | block Y |
| `GROUNDLEVEL` | 71 | profile | block Y of city level 0 |
| `FLOORHEIGHT` | 6 | constant | blocks per level |
| `USE_AVG_HEIGHTMAP` | false | | |
| `heightSampleSize` | 3 | 1..100 | chunks; averaging only when > 2 |
| `MULTI_USE_CORNER` | false | | |
| Space-world level seed | `seed + chunkZ * 817505771 + chunkX * 217645177` | | `java.util.Random` |

## 4. Behavior

All (server; also run without a world for the GUI preview).

### Height to level (`getLevelBasedOnHeight`)

The first threshold the height is strictly below gives the level: `< L0` → 0, `< L1` → 1, … `< L7` → 7, otherwise 8. With defaults each level spans 8 blocks of terrain height, starting at Y 75.

### Dispatch (`computeCityLevel`)

1. Profile `space` or `voidspheres`: space rule.
2. Profile `floating`: level from the chunk heightmap height (main profile thresholds).
3. Profile `cavern`: same as floating (marked "@todo for now").
4. Otherwise: normal rule with the main profile.

### Normal rule (`getCityLevelNormal`)

1. `height` = chunk heightmap height.
2. If `USE_AVG_HEIGHTMAP` and `heightSampleSize > 2`, with `s = heightSampleSize`:
   1. Compute the sample-cell center of this chunk on each axis with truncating division: `base = (chunk / s) * s + (s / 2) * sign`, where `sign` is -1 for negative chunk coordinates and 1 otherwise (CITY-34: the cell around 0 is irregular).
   2. The four neighbor samples are one cell left, right, up and down of that center, on the center's row or column.
   3. Sum `height` with the heightmap height of each neighbor sample that is a raw city chunk ([city-detection.md](city-detection.md)), count them, and integer-divide.
   4. Level from the average.
3. Otherwise level from `height`.

### Space rule (`getCityLevelSpace`)

1. Chunk inside a city sphere: `dist` = relative distance to the sphere center (0 at center, 1 at edge, `03-terrain`). Seed a `java.util.Random` from the space-world seed.
   - `dist < 0.3`: `2 + nextInt(2)` (2 or 3)
   - `dist < 0.4`: `1 + nextInt(2)` (1 or 2)
   - `dist < 0.6`: `nextInt(2)` (0 or 1)
   - else 0
2. Outside: normal rule with the outside profile.

### Multibuildings

In `computeChunkCharacteristics`, a chunk that is part of a multibuilding gets:
- `MULTI_USE_CORNER = true`: the top-left chunk's level;
- otherwise the integer average (truncated) of `getCityLevel` over every chunk of the footprint.

Every chunk of one footprint therefore shares one level.

### Derived heights

| Member | Value |
|---|---|
| `getCityGroundLevel()` | `groundLevel + cityLevel * 6` |
| `getCityGroundLevelOutsideLower()` | same, minus 1 for non-city chunks |
| `getMaxHeight()` | building: `cityGround + floors * 6`; no building: `groundLevel + maxHighwayLevel * 6` if a highway exists, else `cityGround` |
| `localToGlobal(l)` / `globalToLocal(l)` | `l + cityLevel` / `l - cityLevel` (levels, not blocks) |

`groundLevel` is the chunk profile's `GROUNDLEVEL` (outside profile for chunks outside a city sphere).

### Cache

`ChunkPlanner.cityLevel` stores levels in a `TimedCache` (`CITY_LEVELS`, 300 s TTL by default). Without a world (GUI) it computes directly. `getCityLevelGui` always computes directly.

## 5. Determinism

The level is a pure function of the heightmap, profile and coordinates, plus the seeded space-world random. The averaged mode reads neighbor heightmaps and raw city flags, which are themselves pure. Order independent.

## 6. Data formats

Profile options only (`02-profiles-dimensions`). No JSON, no NBT.

## 7. Output

`LostChunkCharacteristics.cityLevel`, copied into `BuildingInfo.cityLevel`. Used by every height decision in this area, by terrain correction (`03-terrain`) and by highways and railways (`06-highways-railways`).

## 8. Relationships

- Called by: `computeChunkCharacteristics`, `MultiChunk` (top-left level), highway and railway code (`06`).
- Calls: `IDimensionInfo.getHeightmap`, `CitySphere.getRelativeDistanceToCityCenter`, `isCityRaw`.

## 9. 1.20 dependencies

| Mechanism | Where | Why it matters for 1.7.10 |
|---|---|---|
| Level thresholds 75..131 and ground level 71 chosen for 1.20 terrain (sea 63, range -64..320) | `CITY_LEVELn_HEIGHT`, `GROUNDLEVEL` | 1.7.10 RWG terrain heights differ; config range up to 384 exceeds the 256 build limit |
| Noise-based chunk heightmap | `getHeightmap` | Comes from RWG in 1.7.10 (`03-terrain`) |

## 10. Bugs

CITY-34.

## Deviations in the new mod

None recorded yet.
