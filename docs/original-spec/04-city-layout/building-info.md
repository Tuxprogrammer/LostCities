# BuildingInfo: per-chunk decisions

The third planning stage. `BuildingInfo` turns a chunk's characteristics into every concrete decision the generator needs: building or not, floors, cellars, the part on each floor, doorways, street type, park and fountain parts, door block, bridge, stair and front parts, palette, loot, ruins, corridor and bridge candidates, and rail dungeons. It also answers height and terrain-correction queries. Source: `worldgen/lost/BuildingInfo.java` (2256 lines) on `f18a094d`. Street, stair, bridge, corridor and door queries on the same class have their own sheets: [streets-parks-stairs.md](streets-parks-stairs.md), [bridges.md](bridges.md), [corridors.md](corridors.md), [doors-and-connections.md](doors-and-connections.md).

## 1. Identity

`BuildingInfo implements ILostChunkInfo` (the public API, `01-core`). One immutable-by-intent instance per chunk, cached by [chunk-planner.md](chunk-planner.md). Key fields:

| Field | Meaning |
|---|---|
| `coord`, `provider`, `profile`, `outsideChunk` | Chunk, dimension info, governing profile, outside a city sphere |
| `groundLevel`, `waterLevel` | Profile `GROUNDLEVEL`; profile `SEALEVEL` or the generator's sea level when -1 |
| `isCity`, `hasBuilding` | Final flags (public, mutable) |
| `rawPlannedRoadType`, `plannedRoadType`, `hierarchicalOpen`, `predefinedStreet` | Street plan ([streets-parks-stairs.md](streets-parks-stairs.md)) |
| `multiBuildingPos`, `multiBuilding`, `buildingType` | Multibuilding position and assets; candidate building (set for every chunk) |
| `cityLevel` | [city-level.md](city-level.md) |
| `floors` (private), `cellars`, `floorTypes[]`, `floorTypes2[]` | Floors above ground (excluding the top part), cellars, part per level, optional overlay part per level |
| `connectionAtX[]`, `connectionAtZ[]` | Random doorway flags per level |
| `streetType` | `NORMAL`, `FULL`, `PARK` (public, mutable: CITY-13) |
| `fountainType`, `parkType`, `bridgeType`, `largeBridgeType`, `stairType`, `frontType`, `railDungeon` | Parts (may be null) |
| `stairPriority` | Random float; highest wins among competing stairs |
| `doorBlock` | Door block for this building |
| `noLoot`, `ruinHeight` | Loot suppression; ruin height fraction or -1 |
| `highwayXLevel`, `highwayZLevel` | From `HighwayInfo` (`06`) |
| `xBridge`, `zBridge`, `xRailCorridor`, `zRailCorridor` | Candidate flags, no guarantee |
| `effectiveCitySettings` | [city-settings.md](city-settings.md) |
| `structureAvoidance` | Result of `StructureAvoidance.check` (`07-scattered`) |
| `torchTodo`, `postTodo` | Deferred block work queued during generation (`03-terrain`) |

Profile options: `BUILDING_MINFLOORS`, `BUILDING_MAXFLOORS`, `BUILDING_MINFLOORS_CHANCE`, `BUILDING_MAXFLOORS_CHANCE`, `BUILDING_MINCELLARS`, `BUILDING_MAXCELLARS`, `BUILDING_DOORWAYCHANCE`, `BUILDING_WITHOUT_LOOT_CHANCE`, `RUIN_CHANCE`, `RUIN_MINLEVEL_PERCENT`, `RUIN_MAXLEVEL_PERCENT`, `BRIDGE_CHANCE`, `RAILWAY_DUNGEON_CHANCE`, `GROUNDLEVEL`, `SEALEVEL`, `OCEAN_CORRECTION_BORDER`, `TERRAIN_FIX_*` (`03`), plus the city-style settings resolved in [city-settings.md](city-settings.md). Building asset fields: `minfloors`, `maxfloors`, `mincellars`, `maxcellars`, `overrideFloors`, `prefersLonely`, `allowDoors`, `allowFillers`, local palette, part and part2 lists with conditions (`05-assets`).

## 2. Inputs

The chunk's characteristics and those of its four neighbors (prefers-lonely), the top-left `BuildingInfo` (multibuildings), seed, profile and outside profile, world style (city-sphere settings, railway avoidance, `railpartheight6`), highway info, railway info, city factor, city-sphere geometry, chunk biome (part conditions), predefined building flags, assets.

## 3. Numbers

| Option / constant | Default | Range | Unit / meaning |
|---|---|---|---|
| `BUILDING_MINFLOORS` | 0 | 0..60 | floors |
| `BUILDING_MAXFLOORS` | 8 | 0..60 | floors (cap) |
| `BUILDING_MINFLOORS_CHANCE` | 4 | 1..60 | floor-count bound at city factor -0.1 |
| `BUILDING_MAXFLOORS_CHANCE` | 6 | 1..60 | floor-count bound at city factor 0.9 |
| `BUILDING_MINCELLARS` | 0 | 0..20 | cellars |
| `BUILDING_MAXCELLARS` | 3 | 0..20 | cellars; city level is added |
| `BUILDING_DOORWAYCHANCE` | 0.6 | 0..1 | per level and side |
| `BUILDING_WITHOUT_LOOT_CHANCE` | 0.2 | 0..1 | single buildings only |
| `RUIN_CHANCE` | 0.05 | 0..1 | |
| `RUIN_MINLEVEL_PERCENT` / `MAX` | 0.8 / 1.0 | 0..1 | fraction of building height |
| `BRIDGE_CHANCE` | 0.7 | 0..1 | per axis, non-city chunks |
| `RAILWAY_DUNGEON_CHANCE` | 0.01 | 0..1 | |
| `GROUNDLEVEL` | 71 | | block Y |
| `SEALEVEL` | -1 | | -1 means the generator's sea level |
| `FLOORHEIGHT` | 6 | constant | blocks |
| Height cap | `maxBuildHeight - 2 - 6` | | block Y; 312 in the 1.20 overworld |
| Bottom floor | `minBuildHeight + 2` | | block Y; -62 in the 1.20 overworld |
| Sphere floor reduction | -2 above 0.6, -1 above 0.5 | | relative distance to sphere center |
| Door choices | 7 | | `nextInt(7)` |
| Large-bridge salt | `0x4c41524745425247` | | ASCII "LARGEBRG" |
| Terrain-correction sentinel | 100000 / 256 | | "no city corner" / assumed build limit (CITY-22) |
| Corner offsets | 20 orthogonal, 25 diagonal, plus 0..4 | blocks | terrain correction L1/L2 |

## 4. Behavior

All steps (server).

### Construction

1. `outsideChunk` = city-sphere world and the chunk does not intersect a city sphere. `profile = getProfile(chunk)`.
2. Read characteristics ([chunk-characteristics.md](chunk-characteristics.md)). Re-run `StructureAvoidance.check` and keep the result (it may now be known where it was not).
3. Copy `cityLevel`, `buildingType`, `multiBuilding`, `multiBuildingPos`, `rawPlannedRoadType`. `predefinedStreet` = a predefined street occupies the chunk.
4. Create `rand` = building random (`QualityRandom(seed + chunkZ * 341873128712 + chunkX * 132897987541)`, the same seed as the characteristics stage).
5. `b = couldHaveBuilding`. For a single chunk with `b` true, test the west, east, north and south neighbors in that order: draw `nextFloat()` and compare with the neighbor's **candidate** building's `prefersLonely`; the first draw below it sets `b = false` and stops the tests. The first draw repeats `bc` from the characteristics stage (CITY-10), and the candidate type exists even where the neighbor has no building (CITY-11).
6. `c = isCity`. City-sphere center override, when the chunk does not avoid a structure, is single or has a known avoidance result (or is not in a `WorldGenRegion`), the world is `space`/`spheres`, and the chunk is a city-sphere center: read the world style's `citysphere.centertype`:
   - `default`: no change; `street`: `c = true`, `b = false`; `building`: `c = true`, `b = true`; `normal`: `c = false`.
7. `isCity = c`, `hasBuilding = b`.
8. `plannedRoadType` = the characteristics' effective type if `isCity && !hasBuilding`, else `NONE`. `hierarchicalOpen` = hierarchical mode, city, no building, no planned road and no predefined street.
9. `groundLevel` and sea level from the outside profile when `outsideChunk`, else from the main profile. `waterLevel` = that sea level, or the generator's sea level if it is -1.
10. `effectiveCitySettings` = resolve(profile, characteristics' city style).
11. **Multibuilding, not top-left:** copy from the top-left `BuildingInfo`: highway levels, `streetType`, `fountainType`, `parkType`, `floors`, `cellars`, `doorBlock`, `bridgeType`, `largeBridgeType`, `stairType`, `stairPriority`, palette and compiled palette, `noLoot`, `ruinHeight`. Skip to step 13.
12. **Otherwise**, in this order (the order fixes random consumption):
    1. Highway levels from `Highway.getHighwayInfo` (`06`).
    2. Street type, park and fountain (draws depend on the street mode; see [streets-parks-stairs.md](streets-parks-stairs.md) § Street type).
    3. `cityFactor = City.getCityFactor(chunk)` (no draw).
    4. **Floors**, `f = BUILDING_MINFLOORS + nextInt(bound) + 1`, with `bound = (int)(MINFLOORS_CHANCE + (cityFactor + 0.1) * (MAXFLOORS_CHANCE - MINFLOORS_CHANCE))`. With defaults: bound 4 for factor below 0.4, 5 below 0.9, 6 above; so 1..6 floors. Then `f = min(f, maxfloors)`, then `f = max(f, minfloors)` (the minimum wins). A bound of 0 or less throws (CITY-14).
    5. City-sphere chunk: relative distance above 0.6 gives `max(minfloors, f - 2)`; above 0.5 gives `max(minfloors, f - 1)`.
    6. Height cap: while `cityGround + f * 6 >= maxBuildHeight - 8`, `f--` (no lower bound: CITY-15). `floors = f`.
    7. **Cellars**: `fb = mincellars + (maxcellars <= 0 ? 0 : nextInt(maxcellars + 1))`, `mincellars = max(BUILDING_MINCELLARS, building.mincellars)`. If a highway exists, or a railway exists and railway avoidance is not `block_railway`: `maxUnder` = the larger of the present highway level and rail level; `fb = max(0, min(fb, cityLevel - maxUnder - railpartheight6))`. Finally `fb = min(fb, maxcellars)` (CITY-16). `cellars = fb`.
    8. `doorBlock` from `nextInt(7)`: 0 birch, 1 acacia, 2 dark oak, 3 spruce, 4 oak, 5 jungle, 6 iron.
    9. `bridgeType` = the city style's random bridge part (one `nextFloat`); a missing part throws.
    10. `largeBridgeType`: hierarchical mode only, from a separate `QualityRandom(seed ^ 0x4c41524745425247 ^ chunkX * 341873128712 ^ chunkZ * 132897987541)` and the style's large-bridge list; legacy mode `null`.
    11. `stairType` = random stair part (one `nextFloat` if the list is non-empty).
    12. `stairPriority = nextFloat()`.
    13. Palette: non-city chunks use the world style's outside style; city chunks the city style's `style`. `Style.getRandomPalette` draws one `nextFloat` per random-palette group (`05-assets`).
    14. `r = nextFloat()`; `noLoot = single && r < BUILDING_WITHOUT_LOOT_CHANCE`.
    15. `r = nextFloat()` (ruin height fraction); then `nextFloat() < RUIN_CHANCE` and no predefined building with `preventruins` gives `ruinHeight = MIN% + (MAX% - MIN%) * r`, else -1.
13. **Railway collision** (any chunk): with railway avoidance `block_railway` and a building: if a rail chunk is here and `cityLevel - cellars <= railLevel + railpartheight6 - 1`, delete the rail chunk from the railway cache (CITY-19).
14. **Floor parts**: arrays of length `floors + cellars + 1`. For `i = 0 .. floors + cellars` (bottom cellar to top part):
    1. Condition context: level `cityLevel + i - cellars`, floor `i - cellars`, `floorsBelowGround = cellars`, `floorsAboveGround = floors`, part `<none>`, the part chosen for the floor below (`<none>` at the bottom), building name, chunk; `isBuilding = true`; `isSphere` and biome evaluated at the chunk center.
    2. `getRandomPart` picks uniformly among parts whose condition matches (one `nextInt` if any). No match throws `"Misconfiguration! No part condition matches …"` (CITY-17).
    3. Second context: same, with part = the chosen part and below-part also set to the chosen part. `getRandomPart2` picks an overlay part the same way; `null` is allowed.
    4. `connectionAtX[i] = isCity(west) && nextFloat() < BUILDING_DOORWAYCHANCE`; `connectionAtZ[i]` likewise with the north neighbor. The draw happens only when the neighbor is city.

    The top entry (`floor == floors`) is the roof part; conditions see it as `isTopOfBuilding`. This loop runs for every chunk, building or not.
15. **Corridor candidates**: a building with cellars gets none (no draw); otherwise two draws against the city style's corridor chance ([corridors.md](corridors.md)).
16. **Bridge candidates**: city chunks get none (no draw); others two draws against `BRIDGE_CHANCE` ([bridges.md](bridges.md)).
17. **Rail dungeon**: `nextFloat() < RAILWAY_DUNGEON_CHANCE` and (no building, or the railway level is below `cityLevel - cellars`): random rail-dungeon part of the city style (one `nextFloat`); else `null`. Placed by `06-highways-railways`.
18. **Front**: `nextFloat() < frontChance` (city style, else `BUILDING_FRONTCHANCE` 0.2): random front part (one `nextFloat`); else `null`.

### Floor and cellar limits

| Limit | Start | Building `overrideFloors` set | Building value without override | City-style constraint |
|---|---|---|---|---|
| `maxfloors` | `BUILDING_MAXFLOORS` | building `maxfloors` (final) | `min` with it | `min` with style `maxfloors` |
| `minfloors` | `BUILDING_MINFLOORS + 1` | building `minfloors` (final) | `max` with it | `max` with style `minfloors` |
| `maxcellars` | `BUILDING_MAXCELLARS + cityLevel` | building `maxcellars`, else building `mincellars` (final) | `min` with max, then `max` with min | `min` with style max, then `max` with style min |

A building value of -1 means unset.

### Random consumption order (building random)

For a single or top-left chunk, after the characteristics stage:

| # | Draw | Condition |
|---|---|---|
| 1-4 | `nextFloat` per neighbor W, E, N, S | single, could have a building; stops at the first success |
| 5 | street-type draws | see [streets-parks-stairs.md](streets-parks-stairs.md) |
| 6 | `nextInt(bound)` floors | always |
| 7 | `nextInt(maxcellars + 1)` | `maxcellars > 0` |
| 8 | `nextInt(7)` door | always |
| 9 | `nextFloat` bridge part | style has bridges |
| 10 | `nextFloat` stair part | style has stairs |
| 11 | `nextFloat` stair priority | always |
| 12 | `nextFloat` per palette group | style has random palettes |
| 13-15 | `nextFloat` ×3: loot, ruin fraction, ruin chance | always |
| per level | `nextInt` part, `nextInt` part2, `nextFloat` X doorway, `nextFloat` Z doorway | candidates exist / neighbor is city |
| tail | corridors ×2, bridges ×2, dungeon (+1), front (+1) | as in steps 15-18 |

For a non-top-left multibuilding chunk the random goes straight from step 4 to the per-level draws. An empty asset list skips its draw, shifting every later draw.

### Height and API queries

| Query | Result |
|---|---|
| `getBuildingBottomHeight()` | `lowest = cityGround - cellars * 6`; while `lowest <= minBuildHeight + 2`, add 6 and decrement `cellars`; while `cityGround + floors * 6 >= maxBuildHeight - 8`, decrement `floors`. Either going negative returns `Integer.MIN_VALUE`. **Writes** `cellars` and `floors` (CITY-18) |
| `getFloorAtY(lowest, y)` | `floorTypes[(y - lowest) / 6]` inside `[lowest, lowest + (floors + cellars + 1) * 6)`, else `null` |
| `isValidFloor(l)`, `getFloor(l)`, `getFloorPart2(l)` | `l` is a local level (0 = ground floor, negative = cellar); index `l + cellars` |
| `isTunnel(level)` | City: `cityLevel > level`. Otherwise the heightmap height is above `groundLevel + level * 6 + 3` |
| `getRuinLevel()` | -1 if `RUIN_CHANCE <= 0` or no ruin; else `(int)(cityGround + 1 + ruinHeight * floors * 6)` |
| `getDamage(chunkY)` | damage area value at the chunk center, block Y `chunkY * 16 + 8` (`03-terrain`) |
| `getCityInfo()` | only for city-center chunks: radius and center style. Returns `null` everywhere in Perlin mode ("@todo REWRITE FOR THE NEW CITY FACTOR") |
| `getBuildingType()` / `getBuildingId()` | the building's name / id when `hasBuilding`, else `null` |
| `getMultiBuildingInfo()` | id, position and dimensions when part of a multibuilding |
| `getCompiledPalette()` | the chunk palette, merged with the building's local palette when there is a building; computed once |
| `isOcean()` | main biome is in `#is_ocean` or `#is_deep_ocean` |
| `getSphere()` | the enabled city sphere of the chunk, or `null` |
| `setBuildingType(...)` | editor only (`01-core`): sets building, floors, cellars and ground level and re-picks parts with the building random |

### Terrain-correction targets

Used by terrain correction (`03-terrain`) to blend terrain toward city edges. Computed at the chunk's north-west corner and cached under the dimension lock.

1. **L0** (`getLowestCityHeightAtChunkCorner`): look at this chunk and its west, north and north-west neighbors. All city or all non-city: 100000 (no effect). Mixed: the minimum of each chunk's `getCityHeightForChunk` (city: `cityGround`; non-city ocean: `groundLevel - OCEAN_CORRECTION_BORDER` (4); other non-city: 100000).
2. **L1**: if L0 `< 256`, min = L0 + random offset in `[TERRAIN_FIX_LOWER_MIN_OFFSET, TERRAIN_FIX_LOWER_MAX_OFFSET]`, max = L0 + random offset in `[TERRAIN_FIX_UPPER_MIN_OFFSET, TERRAIN_FIX_UPPER_MAX_OFFSET]`. Otherwise start from (100000, 100000) and, for each of the 8 neighbors with its L0 `h` and `offs` = 20 (orthogonal) or 25 (diagonal) plus a 0..4 per-chunk offset: lower min to `h - offs` and lower max to `h + offs` when smaller.
3. **L2**: if L1's min `< 256`, copy L1; otherwise the same 8-neighbor pass over the neighbors' L1 values with a second 0..4 offset.

The random offsets come from coordinate-seeded `java.util.Random`s in `LostCityTerrainFeature` (no world seed; `03-terrain`). The 256 test is a hard-coded build limit (CITY-22).

## 5. Determinism

- The building random is seeded from the world seed and coordinates; the large-bridge random likewise. Consumption is strictly ordered as tabled above; an empty asset list, a non-city neighbor or a zero cellar range each skips a draw.
- The characteristics stage and the constructor use the same seed, so the constructor's first float repeats `bc` (CITY-10).
- Writes to shared or cached state: the railway cache (CITY-19), `floors`/`cellars` in `getBuildingBottomHeight` (CITY-18), `streetType` during generation (CITY-13), other chunks' bridge fields (legacy bridges). These make results depend on generation order and cache eviction.
- Part conditions read the biome at the chunk center (y 0).

## 6. Data formats

Reads building and city-style fields listed in § 1 (formats in `05-assets`). No NBT or saved data.

## 7. Output

The `BuildingInfo` consumed by the generator (`03-terrain`: buildings, streets, borders, ruins, rubble, loot), highways and railways (`06`), scattered structures (`07`) and the API.

## 8. Relationships

- Called by: `ChunkPlanner.buildingInfo` and everything that asks for a neighbor's info.
- Calls: [chunk-characteristics.md](chunk-characteristics.md), [city-detection.md](city-detection.md), [city-settings.md](city-settings.md), `Highway`/`Railway` (`06`), `CitySphere`/`DamageArea`/`LostCityTerrainFeature` helpers (`03`), `AssetRegistries` and `Building`/`CityStyle`/`Style` pickers (`05`), `StructureAvoidance` (`07`).

## 9. 1.20 dependencies

| Mechanism | Where | Why it matters for 1.7.10 |
|---|---|---|
| World height -64..320 (`getMinBuildHeight`, `getMaxBuildHeight`) | floor cap, `getBuildingBottomHeight` | 0..255 in 1.7.10: cap becomes 247 and the floor 2; tall cities lose floors |
| Hard-coded 256 sentinel | terrain-correction L1/L2 | Coincides with the 1.7.10 limit, but city ground cannot reach it there either |
| Per-wood doors (birch, acacia, dark oak, spruce, jungle) | `getRandomDoor` | 1.7.10 has only `wooden_door` and `iron_door` |
| Biome tags `#is_ocean`, `#is_deep_ocean` | `isOcean` | `BiomeDictionary.Type.OCEAN` or a biome list in 1.7.10 |
| `Holder<Biome>` and registry access | part conditions' `getBiome` | Biome IDs and names in 1.7.10 |
| Generator sea level (`ServerChunkCache.getGenerator().getSeaLevel()`) | `waterLevel` when `SEALEVEL = -1` | RWG sea level (63) in 1.7.10 |
| `Random.nextInt(int, int)` (Java 17) | legacy street type | Not in Java 8; a port must consume exactly one `nextInt()` |
| Forge event bus | `CharacteristicsEvent`, post-gen events | Exists in 1.7.10 |

## 10. Bugs

CITY-10, CITY-11, CITY-13, CITY-14, CITY-15, CITY-16, CITY-17, CITY-18, CITY-19, CITY-22.

## Deviations in the new mod

See [new-spec 04-city-layout § Buildings](../../new-spec/04-city-layout/index.md#buildings-building-infomd-multibuildingsmd).
