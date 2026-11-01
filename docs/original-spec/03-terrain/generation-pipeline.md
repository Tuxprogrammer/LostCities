# Per-chunk generation pipeline

The contract for how one chunk becomes a Lost Cities chunk in upstream 1.20 at `f18a094d`: where the work is hooked into Minecraft's generation, which stages run in which order, what each stage reads and writes, and which other areas' generators it calls. This is the sheet the RWG-overlay design (spike S2) builds against; the [interface section](#the-terrain-and-city-interface) at the end states the boundary between "vanilla terrain" and "city terrain" in one place.

Paths are relative to `src/main/java/mcjty/lostcities/` or `src/main/resources/` on `f18a094d`.

## 1 · Identity

| Piece | Role |
|---|---|
| `worldgen/LostCityFeature` | The registered world-generation feature `lostcities:lostcity`. Entry point for every chunk; owns the per-dimension `IDimensionInfo` cache and the generation locks ([concurrency sheet](concurrency-caches-determinism.md)). |
| `worldgen/LostCitySphereFeature` | Second feature, `lostcities:spheres`. Draws city-sphere shells late in decoration ([spheres sheet](city-spheres-and-landscapes.md)). |
| `worldgen/LostCityTerrainFeature` | One instance per dimension (built by `DefaultDimensionInfo`). `generate(region, chunk)` is the per-chunk pipeline described here. |
| `worldgen/ChunkDriver` | Staging buffer for every block write in a chunk; committed once at the end ([driver sheet](chunk-driver-and-parts.md)). |
| `worldgen/ChunkFixer` | Post-commit pass: vines, deferred block work, boundary vine cleanup ([post-processing sheet](post-processing.md)). |
| `worldgen/GenerationContext` | Thread-local scratch state for one chunk: seeded random, palette sequence, noise buffers, the driver. |
| `worldgen/ErrorLogger` | Logs chunk facts and warns players in chat when a chunk throws. |
| `setup/TerrainEventHandlers` | Entirely commented out (dead code); see [Unused](#unused-decoration-suppression). |
| `data/lostcities/worldgen/configured_feature/lostcities.json`, `spheres.json` | Configured features of type `lostcities:lostcity` and `lostcities:spheres`, empty config. |
| `data/lostcities/worldgen/placed_feature/lostcities.json`, `spheres.json` | Placed features, one placement modifier: `minecraft:count` with `count: 1`. |
| `data/lostcities/forge/biome_modifier/lostcities.json` | `forge:add_features`, biomes `#minecraft:is_overworld`, feature `lostcities:lostcities`, step `raw_generation`. |
| `data/lostcities/forge/biome_modifier/lostcity_spheres.json` | `forge:add_features`, biomes `#minecraft:is_overworld`, feature `lostcities:spheres`, step `top_layer_modification`. |

Config keys and profile options involved are listed per stage below and in the stage sheets.

## 2 · Inputs

- The `WorldGenRegion` handed to the feature (the chunk plus its 8 neighbors) and the chunk's `ChunkAccess`.
- The dimension's `IDimensionInfo`: world seed, dimension key, active profile (and the "outside" profile for sphere worlds), world style. Built lazily per dimension by `LostCityFeature` (02-profiles-dimensions documents profile selection).
- The chunk's `BuildingInfo` (04-city-layout): `isCity`, `hasBuilding`, `outsideChunk`, `cityLevel`, `groundLevel`, `waterLevel`, floors, cellars, street and park choices, highway and rail info, damage area. `BuildingInfo` is computed from **predicted** terrain heights, never from generated blocks.
- The predicted surface height of this and nearby chunks ([heightmap sheet](heightmap-sampling.md)).
- The chunk's actual blocks as vanilla generated them (read through the driver), used by leveling, terrain correction, void detection, damage and debris.
- The biome at the chunk's middle column, Y 60 (only to skip void biomes).

## 3 · Numbers

| Constant | Value | Where |
|---|---|---|
| Decoration step, city feature | `raw_generation` (the first of the 11 decoration steps) | biome modifier `lostcities.json` |
| Decoration step, sphere feature | `top_layer_modification` (the last step) | biome modifier `lostcity_spheres.json` |
| Placements per chunk | 1 (`minecraft:count` 1, no other modifier, so the origin is the chunk's minimum corner) | placed features |
| Biome filter | `#minecraft:is_overworld` | biome modifiers |
| Void-biome probe | middle block of the chunk at Y 60; skip when tagged `forge:is_void` | `LostCityFeature.java:54` |
| `FLOORHEIGHT` | 6 blocks per city level and per building floor | `LostCityTerrainFeature.java:63` |
| Void probes (floating) | columns (2,2), (2,14), (14,2), (14,14), (8,8), scanned down from Y 255 | `LostCityTerrainFeature.java:270-277, 305` |
| Error chat throttle | 10 000 ms between chat warnings | `ErrorLogger.java:25` |

## 4 · Behavior

### Placement (server)

1. Minecraft generates the whole chunk to the `FEATURES` status first: noise terrain, aquifers, surface rules, then the `CARVERS` stage (caves and canyons). Lost Cities therefore always works on finished vanilla terrain, caves included.
2. At the `FEATURES` status the chunk generator walks the 11 decoration steps. In `raw_generation` it reaches `lostcities:lostcities` once per chunk whose biomes include an overworld-tagged biome. (In 1.20, lakes, ores, underground structure pieces, surface structure pieces, vegetation and freezing all come in later steps, so they run on top of the city.)
3. In `top_layer_modification` it reaches `lostcities:spheres` once per chunk.

### `LostCityFeature.place` (server)

1. Return without doing anything unless the level is a `WorldGenRegion`.
2. Take the lifecycle read lock, refreshing all world-bound caches first if the global "dirty" counter moved ([concurrency sheet](concurrency-caches-determinism.md)).
3. Find or build the dimension's `IDimensionInfo`. If the dimension has no Lost Cities profile (`Config.getProfileForDimension` returns none) or the named profile does not exist, stop (no generation).
4. Lock the striped generation locks of the chunk's 3 × 3 neighborhood.
5. If the biome at the chunk's middle, Y 60, is tagged `forge:is_void`, stop.
6. Call `LostCityTerrainFeature.generate(region, chunk)`. Any exception is caught: logged with chunk coordinates, chunk facts written to the log by `ErrorLogger.logChunkInfo`, and a red chat message sent to every player at most once every 10 s. The chunk then keeps whatever was already committed (normally the vanilla terrain; see `TERR-25`).

### `LostCityTerrainFeature.generate` (server)

Ordered stages. "Driver" means the write goes into the staging buffer and reaches the chunk only at stage 12.

1. **Open the generation context** for (world seed, dimension, chunkX, chunkZ): seeds the per-chunk random and palette sequence, and supplies the per-thread driver and noise buffers.
2. **Attach the driver** to this region and chunk (saving the previous attachment, restored in a `finally`).
3. **Fetch inputs:** this chunk's `ChunkHeightmap` and `BuildingInfo`; record the city style's street palette character in the context.
4. **Decide city or normal.** `doCity` is true when `isCity`, or when the chunk is an `outsideChunk` (sphere worlds, outside every sphere) that has a building. If `doCity`, the profile is `FLOATING` and `CITY_AVOID_VOID` is set, probe the five void columns; if any is all air from Y 255 to the minimum build height, treat the chunk as normal.
5. **City chunk** (`doCityChunk`): terrain preparation, then the building or the street, ruins, street decorations, highways, rubble and "stuff". See [city chunk terrain](city-chunk-terrain.md) and [building and street writing](building-and-street-writing.md).
   **Normal chunk** (`doNormalChunk`): terrain blend toward nearby city chunks, then bridges, highways and scattered structures. See [terrain blend](terrain-blend.md).
6. **Sphere centerpiece** (only `SPACE`, `SPHERES` and `CAVERNSPHERES` landscapes): if this chunk is the center of an enabled city sphere and the world style's city-sphere settings name a center part, write that part at a Y chosen by its origin setting ([spheres sheet](city-spheres-and-landscapes.md)).
7. **Railways:** if the chunk's rail info is not `NONE`, call `Railways.generateRailways`; always call `Railways.generateRailwayDungeons` (06-highways-railways).
8. **Torches:** resolve queued torch positions into standing or wall torches ([post-processing](post-processing.md)).
9. **Reseed** the context random to a chunk-only seed: `chunkX × 257017164707 + chunkZ × 101754694003` (no world seed).
10. **Damage:** post the cancelable `LostCityEvent.PreExplosionEvent`. If no listener cancels it: apply explosion damage when the chunk's `DamageArea` has explosions, then drop debris from damaged neighboring buildings ([damage sheet](damage-ruins-rubble.md)).
11. *(Building chunks with second parts commit early inside stage 5; see [building writing](building-and-street-writing.md#generatebuilding-server).)*
12. **Commit:** `ChunkDriver.actuallyGenerate` copies every staged block into the chunk sections and raises the chunk's heightmaps.
13. **Detach the driver**, then run `ChunkFixer.fix`: vines on building walls, deferred block actions (loot, POI blocks, light refresh, saplings, command blocks), and removal of unsupported vines along both sides of the chunk border. These write straight into the region, not through the driver.
14. Close the context; add the wall-clock time to the dimension's `Statistics` (used only by `/lostcities stats`).

### Calls into other areas

| Stage | Call | Area |
|---|---|---|
| 3 | `BuildingInfo.getBuildingInfo`, `CityStyle` | 04-city-layout |
| 5 (city) | `Corridors.generateCorridors`, `Corridors.generateCorridorConnections` | 06-highways-railways |
| 5 (city) | `Doors.generateDoors` | 04-city-layout |
| 5 (city) | `Highways.generateHighways` (only when a highway level is set) | 06-highways-railways |
| 5 (city) | `Stuff.generateStuff` | 07-scattered |
| 5 (normal) | `Bridges.generateBridges`, `Highways.generateHighways` | 06-highways-railways (bridges: 04-city-layout) |
| 5 (normal) | `Scattered.avoidScattered`, `Scattered.generateScattered` (only when the world style has scattered settings) | 07-scattered |
| 5, 6 | `generatePart` on parts and palettes | 05-assets for the formats; [driver sheet](chunk-driver-and-parts.md) for the writing |
| 5 | `handleLoot`, `handleSpawner`, `createLoot`, `getRandomSpawnerMob` | 08-loot-mobs-blocks |
| 7 | `Railways.generateRailways`, `generateRailwayDungeons` | 06-highways-railways |
| 5, 10 | `LostCityEvent` posts (`PreGenCityChunkEvent`, `PostGenCityChunkEvent`, `PostGenOutsideChunkEvent`, `PreExplosionEvent`) | 01-core (API) |

### `LostCitySphereFeature.place` (server)

Same guards as the city feature (region check, lifecycle lock, 3 × 3 locks, void biome), then open a generation context for the chunk and call `Spheres.generateSpheres`, which only acts for `SPACE`, `SPHERES` and `CAVERNSPHERES` profiles. It has **no** exception handler ([spheres sheet](city-spheres-and-landscapes.md)).

### Unused: decoration suppression

`setup/TerrainEventHandlers` is a class whose whole body is commented out (marked `@todo 1.14`). In the 1.12 era it listened to `DecorateBiomeEvent.Decorate` and denied fossils, desert wells, water lakes, pumpkins, grass, mushrooms, cacti, reeds, lily pads, flowers or trees per profile flags (`AVOID_GENERATED_*`), and in space profiles replanted trees only inside sphere borders. None of it runs in 1.20, and the profile flags no longer exist. It is documented because 1.7.10 has the same `DecorateBiomeEvent`, so the 1.12 approach is available to the port.

## 5 · Determinism

| Source | Seed | Notes |
|---|---|---|
| Context random (stages 4–8) | `GenerationContext.mixSeed(worldSeed, dimension, chunkX, chunkZ)` | Shared by everything in the chunk in call order. |
| Palette sequence | low and high halves of the same mixed seed, XORed | 7-bit LCG, see [concurrency sheet](concurrency-caches-determinism.md). |
| Context random (stages 9–10) | `chunkX × 257017164707 + chunkZ × 101754694003` | World-independent (`TERR-27`). |
| `ChunkFixer` random | `worldSeed ^ chunkX × 341873128712 ^ chunkZ × 132897987541` | Reseeds the same context random. |

Order dependence enters through shared caches and cross-chunk effects, not through the per-chunk seeds: `TERR-07`, `TERR-09`, `TERR-10`, `TERR-11`, `TERR-12`, `TERR-02`. The 3 × 3 striped locks serialize neighbors but do not fix an order.

## 6 · Data formats

- JSON: the six worldgen and biome-modifier files in [Identity](#1--identity). They carry no fields beyond those listed.
- NBT: none written by the pipeline itself (see [saved data](saved-data.md)).

## 7 · Output

A fully written chunk at the end of the `raw_generation` step: city chunks rebuilt from bedrock to the top of the city, normal chunks near cities reshaped in their top layers, plus bridges, highways, railways, scattered structures, damage and debris. Later vanilla decoration then runs on this result. Deferred work (tree growth from saplings) can still happen on later server ticks ([post-processing](post-processing.md)).

## 8 · Relationships

- Called by: Minecraft's feature decoration, through the two biome modifiers.
- Calls: the stage sheets in this area, and the areas in the table above.
- `IDimensionInfo` is also fetched (outside generation) by commands and `LostCitiesImp` through `LostCityFeature.getDimensionInfo` (01-core).

## 9 · 1.20 dependencies

- Feature registration, configured and placed features, Forge biome modifiers and decoration steps (no 1.7.10 counterpart; 1.7.10 has the chunk provider, `populate` with its +8 offset, and the `PopulateChunkEvent` and `DecorateBiomeEvent` hooks).
- `WorldGenRegion` with a writable 3 × 3 chunk window and `ChunkStatus` (no 1.7.10 counterpart).
- Multithreaded chunk generation (1.7.10 generates on the server thread).
- Biome tags `#minecraft:is_overworld` and `forge:is_void` (1.7.10: `BiomeDictionary` types or explicit lists).

## 10 · Bugs

`TERR-25` (exceptions leave half-written chunks), `TERR-27` (world-independent seeds), `TERR-33` (error reporting), `TERR-35` (sphere feature has no exception guard), and the order-dependence bugs listed under Determinism.

## The terrain and city interface

What an overlay on another generator must provide, and what Lost Cities does with it. Everything here is upstream behavior; designing the RWG answer is S2's job.

**Lost Cities consumes, before it writes anything:**

1. A *predicted surface height per chunk*, available for any chunk coordinate without generating that chunk, as a pure function of the seed and coordinates. Upstream asks the noise generator for the top solid block + 1 (water ignored) at block (8, 8) of one sample chunk per 3 × 3 group ([heightmap sheet](heightmap-sampling.md)). Every layout decision uses it: city levels, highway-city constraints, bridges, borders, scattered structures.
2. The *sea level* of the generator (`Tools.getSeaLevel`, vanilla 63), unless the profile sets `SEALEVEL`.
3. The *main biome* of a chunk (04-city-layout's `BiomeInfo`), for ocean, beach and river tests (`isWaterBiome`, `isOcean`).
4. The *real block columns* of the chunk being generated, after caves and canyons are carved.

**The fixed city frame it imposes:**

- City ground Y = `GROUNDLEVEL` (71) + 6 × `cityLevel`, `cityLevel` 0–8 chosen from the predicted height by fixed thresholds (`CITY_LEVEL0_HEIGHT` 75, then every 8 blocks to 131). So city streets sit at Y 71, 77, …, 119 regardless of the generator's own terrain profile.
- Buildings occupy `ground − 6 × cellars` to `ground + 6 × floors`, plus a roof part.

**What it writes:**

| Chunk kind | Columns written | Vertical extent |
|---|---|---|
| City chunk, `DEFAULT` or `SPHERES` | all 256 | bedrock floor (`BEDROCK_LAYER` layers at the minimum build height); the terrain top shifted to exactly the city ground; empty space under streets filled down to the bedrock floor; building or street from the ground up; clearing above the building to the sampled height + 10 |
| City chunk, other landscapes | all 256 | from a few blocks under the city ground upward; the generator's terrain below is left alone except for supports |
| Normal chunk within about 3 chunks of a city (`DEFAULT`, `SPHERES` only) | columns whose surface lies outside the blend corridor | the top 6 layers of terrain moved down, or the whole column shifted up, to fit the corridor; nothing else |
| Any chunk | bridge, highway, railway, scattered and debris footprints | per those generators |

**What it leaves to vanilla:** everything generated before `raw_generation` that it does not overwrite (for example caves under buildings that the leveling did not reach), and everything generated after it in the same chunk (lakes, ores, structure pieces, vegetation, snow). Lost Cities does not suppress later vanilla decoration in 1.20, so ore features can still replace stone in the city's base fill and in stone parts of buildings, and vegetation can still grow on park grass (*inferred* from the step order; not observed).
