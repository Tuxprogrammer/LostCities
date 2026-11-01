# 04 · City layout

## Overview

How upstream Lost Cities decides, chunk by chunk, what a city looks like in plan: which chunks are city, at which city level, in which city style; where streets, primary/secondary/tertiary roads, parks, fountains, stairs and slopes go; where buildings and multibuildings stand and how many floors and cellars they have; where bridges, corridors and doors are; and how predefined cities, buildings, streets and spheres override all of that. It documents Lost Cities **1.20-7.5.7** (branch `1.20`, commit `f18a094d`). Block writing for terrain, streets and buildings is `03-terrain`; the asset formats are `05-assets`; highways and railways are `06-highways-railways`; scattered structures and structure avoidance are `07-scattered`.

## Contents

- [city-detection.md](city-detection.md): city centers, radii, Perlin rarity, the city factor and raw city membership
- [city-styles.md](city-styles.md): which city style governs each chunk; street-majority vote
- [city-level.md](city-level.md): city level from terrain height; averaged heightmaps; space worlds; multibuildings
- [chunk-planner.md](chunk-planner.md): the three planning stages, every cache, locks, provisional values
- [chunk-characteristics.md](chunk-characteristics.md): stage 2: city, multibuilding position, road clip, building possibility, candidate building
- [building-info.md](building-info.md): stage 3: every per-chunk decision, floors and cellars, floor parts, random consumption order, height queries, terrain-correction targets
- [multibuildings.md](multibuildings.md): random multibuilding placement per multichunk
- [street-planner.md](street-planner.md): the hierarchical road field, effective road clip, mode persistence
- [streets-parks-stairs.md](streets-parks-stairs.md): street types, parks, open lots, fountains, slopes, stairs, road continuity, fronts
- [bridges.md](bridges.md): legacy and hierarchical bridges, bridge generation and supports
- [corridors.md](corridors.md): underground rail corridors
- [doors-and-connections.md](doors-and-connections.md): doorway flags, passages and doors
- [predefined-content.md](predefined-content.md): predefined cities, buildings, streets and spheres
- [transforms.md](transforms.md): directions, orientations, rotations and mirrors
- [city-settings.md](city-settings.md): city-style and world-style layout settings and how they resolve

## Key facts

- **Everything is per chunk** (16 × 16 blocks). A city level is 6 blocks (`FLOORHEIGHT`); a chunk's ground floor is at `GROUNDLEVEL + cityLevel * 6` (71 + 6n by default).
- **Three planning stages** per chunk, cached: highway info, characteristics, `BuildingInfo` ([chunk-planner.md](chunk-planner.md)). Later stages read earlier stages of neighbors.
- **Two street modes.** `LEGACY` (every city chunk without a building is street or park) and `HIERARCHICAL_GRID_V1` (a hashed road field clipped to the city). New worlds use the profile's mode, persisted per dimension; worlds created before the saved data existed stay `LEGACY` ([street-planner.md](street-planner.md)).
- **Two city-detection modes.** Center mode (`cityChance >= 0`, default 0.01) and Perlin mode (`cityChance < 0`).
- **Randomness.** Most per-chunk decisions come from one `QualityRandom` per chunk seeded with `seed + chunkZ * 341873128712 + chunkX * 132897987541`, consumed in a strict order that depends on asset list sizes ([building-info.md](building-info.md) § Random consumption order). City centers, radii, center styles and multibuilding layouts use `java.util.Random` seeded from coordinates **without** the world seed (CITY-01, CITY-02). The street and bridge planners use pure 64-bit hashes of seed, dimension id and coordinates.
- **Java 17 API in the random path.** The legacy street type uses `Random.nextInt(int, int)`, which consumes one `nextInt()`; a Java 8 port must replicate that exactly to keep the random stream in step.
- **Cached state is not immutable.** Several decisions are rewritten after construction (CITY-13, CITY-18, CITY-19). A port that wants the determinism CONTRIBUTING requires must make the plan a pure function.

### 1.20 dependencies

| Mechanism | Where | Why it matters for 1.7.10 |
|---|---|---|
| World height -64..320 (`getMinBuildHeight`/`getMaxBuildHeight`) | `BuildingInfo` floor cap (`maxBuild - 8`), `getBuildingBottomHeight` (`minBuild + 2`), `Supports` lower bound | 0..255 in 1.7.10; tall buildings lose floors, the bottom bound becomes Y 2 |
| Height defaults tuned for 1.20 terrain | `GROUNDLEVEL` 71, `CITY_LEVEL0..7_HEIGHT` 75..131, `CITY_MINHEIGHT` 50, `CITY_MAXHEIGHT` 150, config ranges up to 384 | RWG heights and sea level differ; every height number needs remapping in the new spec |
| Hard-coded 256 build-limit sentinel | `BuildingInfo` terrain-correction L1/L2 | Matches 1.7.10's limit but is a latent assumption |
| Generator sea level (`ServerChunkCache.getGenerator().getSeaLevel()`) | `waterLevel` when `SEALEVEL = -1` | RWG/world sea level in 1.7.10 |
| Noise heightmap (`ChunkGenerator.getBaseHeight`) | city factor height window, city level | RWG terrain (`03-terrain`) |
| Biome holders and biome tags (`#is_ocean`, `#is_deep_ocean`, `#is_beach`, `#is_river`) | `isOcean`, water-biome test for bridges, city-style and multiplier biome filters | `BiomeGenBase` and `BiomeDictionary` types in 1.7.10 |
| `ResourceKey<Level>` and namespaced dimension ids hashed into roads and bridges | `ChunkCoord`, `HierarchicalStreetPlanner.dimensionSalt`, `HierarchicalBridgePlanner.spanHash` | Integer dimension IDs in 1.7.10; the hash input must be specified |
| Block states and properties (`DoorBlock.HALF/HINGE/FACING`, `RailBlock.SHAPE`, `Rotation`) | doors, corridors, transforms | Metadata in 1.7.10 |
| Per-wood doors (birch, acacia, dark oak, spruce, jungle) | `getRandomDoor` | Only `wooden_door` and `iron_door` in 1.7.10 |
| `Blocks.STRUCTURE_VOID`, `Blocks.GRASS_BLOCK` | supports, parks | No structure void; `grass` in 1.7.10 |
| Block tag `lostcities:rotatable` | `transformBlockState` | No block tags in 1.7.10 |
| Data-pack registries and Mojang codecs | all assets including predefined content and settings | Assets load from the jar; JSON parsed by hand |
| `SavedData` in the overworld storage | street mode persistence | `WorldSavedData` in 1.7.10 |
| Parallel chunk generation, `WorldGenRegion`, staged chunk statuses | planner locks, `Staged.cacheable` | Single-threaded generation and populate with the +8 offset in 1.7.10 |
| Java 16/17 API: records, `List.of`, `Stream.toList`, `Random.nextInt(int, int)` | throughout | Not allowed by CONTRIBUTING (Java 8 API) |
| Forge event bus (`LostCityEvent.CharacteristicsEvent`, post-gen events) | characteristics | Exists in 1.7.10 Forge |

## Related areas

- `01-core`: cache clearing on world load/unload, the API (`ILostChunkInfo`), the part editor (`setBuildingType`), `CommandDebug`.
- `02-profiles-dimensions`: every profile option used here, landscape types, the outside profile.
- `03-terrain`: heightmaps, city spheres, terrain correction (consumes the L0/L1/L2 targets), block writing for streets, parks, buildings and borders, `ChunkDriver`.
- `05-assets`: city styles, world styles, buildings, multibuildings, parts and their conditions, palettes, and the pickers whose random consumption this area relies on.
- `06-highways-railways`: highway levels, railway chunk types and levels, rail dungeons, monorails; `Railway.removeRailChunkType` (CITY-19).
- `07-scattered`: structure avoidance feeds city membership; scattered structures avoid city, bridge and highway chunks.
- `09-client-ui`: the GUI preview uses `getChunkCharacteristicsGui`.

## Coverage

- Source files read in full: **35 of 35** in scope for this area: `worldgen/lost/{City, BuildingInfo, MultiChunk, Direction, Orientation, Transform, CityRarityMap}.java`; `worldgen/street/*` (9); `worldgen/plan/*` (4); `worldgen/gen/{Corridors, Bridges, Doors, Supports}.java`; `worldgen/lost/cityassets/{PredefinedCity, PredefinedSphere}.java`; `worldgen/lost/regassets/data/{StreetSettings, ParkSettings, CorridorSettings, BuildingSettings, MultiSettings, PredefinedBuilding, PredefinedStreet, SphereSettings, CitySphereSettings}.java`. (`StructureAvoidance.java` is covered in `07-scattered`.)
- In-scope files not covered: none.
- Out-of-scope files read for behavior (in part unless noted): `varia/{QualityRandom (full), Counter (full), TimedCache (full), ChunkCoord (full), Tools}.java`; `api/{LostChunkCharacteristics, MultiPos}.java` (full); `config/{LostCityProfile, StreetGenerationMode (full), MultiBuildingStreetConflict (full)}.java`; `setup/Config.java`; `worldgen/{LostCityTerrainFeature, ChunkHeightmap, ChunkDriver, DefaultDimensionInfo, LostCityWorldGenData (full)}.java`; `worldgen/lost/{Railway, CitySphere}.java`; `worldgen/lost/cityassets/{EffectiveCitySettings (full), AssetRegistries, RegistryAssetRegistry, CityStyle, WorldStyle, Building, Style, ConditionContext}.java`; `worldgen/lost/regassets/{PredefinedCityRE, PredefinedSphereRE, data/StreetParts, data/WorldSettings}.java`; `resources/data/lostcities/lostcities/worldstyles/{standard, standard_everywhere}.json`; `upstream/1.12` `dimensions/world/lost/{City, BuildingInfo}.java` (seeds only).
- Lang keys: none in scope (0 of 0). Hard-coded English appears only in exception messages (`RuntimeException` texts in `BuildingInfo`, `MultiChunk`, `Bridges`, `Supports`, `Transform`, `HierarchicalStreetPlanner`, `StreetPlannerSettings`), which are logged, not shown to players.
- Textures and sounds: none in scope.
- Corrections to earlier statements: none (first version, 2026-11-01).

## Known limits

- Nothing was generated in game; every statement about appearance (patchwork city styles, door and bridge orientation) is *inferred* from code.
- `ChunkStage` is unused (TODO) and `MultiChunk.dump`, `Transform.randomRotation`, `canWaterCorridorGoThrough` and the `FULL` street type are unreachable or uncalled; they are documented as such.
- Behavior that depends on 1.20's parallel generation and `WorldGenRegion` timing (CITY-20) could not be quantified.
- The `CityStyle`, `WorldStyle`, `Building` and `Style` pickers were read only far enough to state their random consumption; their full behavior is `05-assets`.
- Upstream ships no predefined cities or spheres, so that path is untested by bundled data.

## Suspected bugs

| ID | Title | Location | What looks wrong | Severity |
|---|---|---|---|---|
| CITY-01 | City centers, radii and center styles ignore the world seed | `worldgen/lost/City.java:187` (also `:215`, `:243`) | The three `Random`s are seeded from chunk coordinates only, so in center mode every world and dimension has cities at the same chunks with the same radii and center styles. On 1.12 the seeds included the world seed. | worldgen |
| CITY-02 | Multibuilding layout ignores world seed and dimension | `worldgen/lost/MultiChunk.java:73` | Multichunk random uses only the multichunk coordinates. | worldgen |
| CITY-03 | Chunk city style uses the chunk, not the city center | `worldgen/lost/City.java:282` | `getCityStyleForCityCenter(coord)` is called with the current chunk instead of the center `c`, so the style is a per-chunk random pick; the method's own comment says to call it on a center. Same on 1.12. | worldgen |
| CITY-04 | Outside-profile radius draw lacks the range guard | `worldgen/lost/City.java:225` | `nextInt(outsideMax - outsideMin)` throws when the outside profile's max radius is not above its min; the inside path clamps the range to at least 1. | crash |
| CITY-05 | Rarity map cached with the first caller's parameters | `worldgen/lost/City.java:50` | `computeIfAbsent` keyed by dimension only; in city-sphere worlds the inside and outside profiles share one Perlin map. | worldgen |
| CITY-06 | City style cache ignores the profile | `worldgen/lost/City.java:249` | Cache key is the chunk only; `MultiChunk` passes the main profile, `BuildingInfo` the chunk's profile; the first caller wins. | worldgen |
| CITY-07 | Predefined multibuilding city forcing assumes 2 × 2 | `worldgen/lost/City.java:317` | Only the W, NW and N neighbors are checked for a predefined multibuilding top-left; larger footprints can contain non-city chunks. | worldgen |
| CITY-08 | Overlapping predefined entries resolve by map order | `worldgen/lost/City.java:112` (also `:145`, `:165`) | Several entries on one chunk overwrite each other in `HashMap`/registry iteration order. | worldgen |
| CITY-09 | Weighted picks can return `null` | `worldgen/lost/City.java:294`; `worldgen/lost/MultiChunk.java:106` | `Tools.getRandomFromList(Random, …)` returns `null` when float rounding leaves a remainder (`varia/Tools.java:117`); `MultiChunk` then dereferences it, `City` resolves a `null` style name. | crash |
| CITY-10 | Building-chance roll reused as the first prefers-lonely roll | `worldgen/lost/BuildingInfo.java:805` | The constructor reseeds the building random with the characteristics seed (`:399`, `:801`), so the west-neighbor test reuses `bc`, which is already below the building chance; a west neighbor with `prefersLonely >= buildingChance` always suppresses. | worldgen |
| CITY-11 | Prefers-lonely reads neighbors' candidate buildings | `worldgen/lost/BuildingInfo.java:805-811` | Every chunk has a candidate `buildingType`, so street and non-city neighbors also suppress buildings. | worldgen |
| CITY-12 | Street type `FULL` is unreachable | `worldgen/lost/BuildingInfo.java:897` | `nextInt(0, values().length - 2)` is `nextInt(0, 1)`, always `NORMAL`; same at `LostCityTerrainFeature.java:1234`. 1.12 drew from all three. | worldgen |
| CITY-13 | Street type rewritten during generation | `worldgen/LostCityTerrainFeature.java:1233-1235` | Generation sets a cached `BuildingInfo.streetType` from `PARK` to `NORMAL`; `hasFrontPartFrom` (`BuildingInfo.java:1803-1809`) and so neighbor fronts and doors depend on which chunk generated first. | worldgen |
| CITY-14 | Floor-count bound can be zero or negative | `worldgen/lost/BuildingInfo.java:928` | With `buildingMinFloorsChance > buildingMaxFloorsChance` the `nextInt` bound can drop to 0 or below and throw. | crash |
| CITY-15 | Height-cap loop has no lower bound | `worldgen/lost/BuildingInfo.java:948` | `f--` until the building fits; a city ground near the cap gives negative floors and a `NegativeArraySizeException` at `:1017`. | crash |
| CITY-16 | Cellar count piles up at the maximum | `worldgen/lost/BuildingInfo.java:955` | `mincellars + nextInt(maxcellars + 1)` is then clamped to `maxcellars` (`:975`), so with `mincellars > 0` the top value absorbs the overflow. | worldgen |
| CITY-17 | Every chunk selects floor parts and can throw | `worldgen/lost/BuildingInfo.java:1043` | The floor loop runs for street and non-city chunks over their candidate building; a part list with no matching condition crashes where no building will stand. | crash |
| CITY-18 | `getBuildingBottomHeight` mutates floors and cellars | `worldgen/lost/BuildingInfo.java:1919` (also `:1926`) | A query changes cached state; neighbors see different `floors`/`cellars` before and after, and eviction restores the old values. | worldgen |
| CITY-19 | `BuildingInfo` deletes railway chunks from a shared cache | `worldgen/lost/BuildingInfo.java:1012` | `Railway.removeRailChunkType` during construction: neighbors that read the rail first keep it; after a railway cache reset the rail returns until the `BuildingInfo` is rebuilt. | worldgen |
| CITY-20 | Provisional plans are visible to neighbors | `worldgen/plan/ChunkPlan.java:47` (also `:67`) | Non-cacheable results are returned and later recomputed; neighbors may already have built against the provisional value. | worldgen |
| CITY-21 | Street-majority tie broken by `HashMap` order | `worldgen/lost/BuildingInfo.java:431` | `Counter.getMostOccuring` iterates a `HashMap` (`varia/Counter.java:27`). | worldgen |
| CITY-22 | Terrain correction assumes a 256 build limit | `worldgen/lost/BuildingInfo.java:2009` (also `:2069`) | `@todo build limit`: a real corner height of 256 or more is treated as "no city". | worldgen |
| CITY-23 | Legacy bridge walks recurse across rows | `worldgen/lost/BuildingInfo.java:1541` | Odd-row checks call neighbors' `hasXBridge`, which walk again, all under the dimension-wide lock. | performance |
| CITY-24 | Dead avoidance clause | `worldgen/lost/BuildingInfo.java:389` | The footprint path never returns `UNKNOWN`, so the multibuilding clause is never true. | cosmetic |
| CITY-25 | `MultiChunk` crashes on bad multibuilding settings | `worldgen/lost/MultiChunk.java:138` (also `:79`) | `nextInt(areasize - dim + 1)` with a multibuilding larger than the area, or `nextInt(max - min + 1)` with `maximum < minimum`, throws. | crash |
| CITY-26 | `MultiChunk` checks positions against the wrong level and profile | `worldgen/lost/MultiChunk.java:86` (also `:92`, `:140`) | The rail-clearance test uses the area's top-left city level for every position, and the main profile even in city-sphere worlds. | worldgen |
| CITY-27 | Rail transform throws for `MIRROR_Z` | `worldgen/lost/Transform.java:79` (throw at `:204`) | Ascending east/west rails have no `MIRROR_Z` case and hit `IllegalStateException`; monorails use `MIRROR_Z`. | crash |
| CITY-28 | Mirror transforms are inconsistent | `worldgen/lost/Transform.java:10-12` | Mirrors rotate block states; `MIRROR_X` flips ascending north/south; `MIRROR_90_X` transposes coordinates, rotates blocks clockwise and maps rails like `ROTATE_270` (east-west unchanged). | visual |
| CITY-29 | `randomRotation` uses `Math.random` | `worldgen/lost/Transform.java:25` | Unseeded; dead code today. | cosmetic |
| CITY-30 | Z bridges are transposed but not block-rotated | `worldgen/gen/Bridges.java:49` | Directional blocks in a Z-oriented bridge keep their X orientation (*inferred*). | visual |
| CITY-31 | City-sphere codec writes `centertype` under `centerpartorigin` | `worldgen/lost/regassets/data/CitySphereSettings.java:14` | Encoding round-trips the wrong value. | cosmetic |
| CITY-32 | Locale-dependent enum parsing | `worldgen/lost/regassets/data/CitySphereSettings.java:26-27` | `toUpperCase()` with the default locale; under Turkish, values with `i` (`building`, `fixed`, `firstfloor`) fail `valueOf`. | crash |
| CITY-33 | GUI preview characteristics differ from the world | `worldgen/lost/BuildingInfo.java:349-365` | Preview ignores city-style building chance, multibuildings, roads, highways, railways and avoidance. | cosmetic |
| CITY-34 | Averaged-heightmap sample grid uses truncating division | `worldgen/lost/BuildingInfo.java:1268` | Cells around coordinate 0 are irregular (chunks -2..-1 form a two-chunk cell). | worldgen |
| CITY-35 | Multibuilding doorway guard covers only the last column and row | `worldgen/lost/BuildingInfo.java:1837` (also `:1874`) | Internal walls next to the last column/row never get random doorways; other internal walls can. | worldgen |
| CITY-36 | West/north passage ignores the neighbor's `dontconnect` | `worldgen/gen/Doors.java:33` | The east/south test checks both floors (`:122-133`), the west/north test only its own, leaving a half-open wall. | visual |
| CITY-37 | API street width unboxes `null` | `worldgen/lost/cityassets/CityStyle.java:185-186` | `getStreetWidth()` returns `int` from a nullable `Integer`; the field is otherwise unused. | crash |

<!-- Merged and deduplicated into BUGS.md by the orchestrator. Suspected, not confirmed; no fixes designed here. -->
