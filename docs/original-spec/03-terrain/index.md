# 03 · Terrain

## Overview

How upstream Lost Cities 1.20 (branch `1.20` at `f18a094d`, mod version 1.20-7.5.7, Minecraft 1.20.1, Forge 47) turns a finished vanilla chunk into a city chunk or a blended neighbor: the per-chunk pipeline and where it hooks into generation, the predicted terrain heights every layout decision rests on, leveling and filling inside city chunks, the terrain blend at city edges, the block-writing driver and part writer, post-processing (torches, vines, deferred jobs), explosions, debris, ruins and rubble, city spheres and the landscape types, the caches, threading and seeds, and the per-world saved data.

Upstream 1.20 has no chunk generator of its own. It runs as a world-generation feature in the first decoration step (`raw_generation`), after vanilla has generated noise terrain, surface and caves, and rewrites the chunk in place. The [pipeline sheet](generation-pipeline.md) is the contract for the RWG overlay (spike S2) and ends with a one-page statement of the [terrain and city interface](generation-pipeline.md#the-terrain-and-city-interface).

## Contents

- [generation-pipeline.md](generation-pipeline.md): feature placement, stage order per chunk, calls into other areas, the terrain/city interface (the S2 contract)
- [heightmap-sampling.md](heightmap-sampling.md): predicted terrain height per chunk, the 3 × 3 sampling grid, the cache, `HeightGenOpt` and `NoiseChunkOpt`, the level thresholds
- [city-chunk-terrain.md](city-chunk-terrain.md): bedrock floor, high water, leveling to the city ground, room for buildings, base under streets, street-edge borders, water and caves
- [terrain-blend.md](terrain-blend.md): the L0/L1/L2 corner meshes, transition noise and column moves that blend normal chunks into cities
- [building-and-street-writing.md](building-and-street-writing.md): floors, cellars and second parts; street, park and fountain sections; part selection; front parts; vegetation; stairs
- [chunk-driver-and-parts.md](chunk-driver-and-parts.md): the staging buffer, neighbor-shape correction, the commit, `generatePart` and its special blocks
- [post-processing.md](post-processing.md): torches, the chunk fixer (vines, deferred jobs, border vine cleanup), global tick jobs
- [damage-ruins-rubble.md](damage-ruins-rubble.md): explosions and damage, debris, ruins, the rubble and leaf layer
- [city-spheres-and-landscapes.md](city-spheres-and-landscapes.md): the six landscape types, city sphere selection, caching and drawing, the centerpiece
- [concurrency-caches-determinism.md](concurrency-caches-determinism.md): lifecycle and striped locks, generation context, cache inventory, every random seed
- [saved-data.md](saved-data.md): `LostCityWorldGenData`, the pinned street and highway modes

## Key facts

1. **Lost Cities rewrites finished terrain.** In 1.20 caves, canyons, aquifers and surface rules are already in the chunk when it runs; lakes, ores, structure pieces and vegetation come after it, on top of the city. It suppresses none of them.
2. **Layout never looks at generated blocks.** City detection, levels and borders use a *predicted* height per chunk from the noise generator (top solid block + 1, water ignored, sampled once per 3 × 3 chunk group at block (8, 8) of the group's center chunk). Only the final writing reads the real columns.
3. **City ground is a fixed frame.** Street level = `GROUNDLEVEL` + 6 × `cityLevel`, with level 0–8 from fixed height thresholds, so streets sit at Y 71, 77, …, 119 by default. Every building floor is 6 blocks.
4. **City chunks are leveled, neighbors are blended.** In `DEFAULT` and `SPHERES` landscapes a city chunk's terrain top is shifted to exactly the city ground (top 6 layers moved down, or the column shifted up), with a 1-layer bedrock floor and stone under streets down to the first solid layer. Normal chunks up to about 3 chunks away are squeezed between two interpolated meshes pinned to the city ground at city corners.
5. **One staging buffer per chunk.** All writes go to the `ChunkDriver` cache and reach the chunk in one direct section copy at the end, without block updates, lighting or block-entity handling; block entities go in as pending NBT; loot, POI blocks and light refreshes run after the commit.
6. **Order matters more than it should.** Several upstream paths make output depend on generation order or cache timing (`TERR-02`, `TERR-07`, `TERR-09`, `TERR-10`, `TERR-11`, `TERR-12`, `TERR-20`). The port's determinism rule forbids all of them.
7. **Floating, cavern and space terrain is not Lost Cities'.** In 1.20 those landscapes expect the Lost Worlds mod's generators; Lost Cities only adapts its writing.

Y levels and height assumptions in this area (1.20 overworld: build height −64 to 320, sea level 63):

| Number | Value | Meaning | Sheet |
|---|---|---|---|
| `GROUNDLEVEL` | 71 | city level 0 street Y | city chunk terrain |
| `FLOORHEIGHT` | 6 | blocks per level and per floor | pipeline |
| `CITY_LEVEL0…7_HEIGHT` | 75, 83, 91, 99, 107, 115, 123, 131 | predicted-height thresholds for levels 1–8 | heightmap |
| `SEALEVEL` | −1 → generator sea level (63) | water level for drowned cities, damage, parts | city chunk terrain |
| `BEDROCK_LAYER` | 1 | bedrock layers from the min build height in city chunks | city chunk terrain |
| Heightmap sample | block (8, 8); group 3 × 3 chunks | | heightmap |
| Cavern heightmap band | `max(GROUNDLEVEL − 20, 1)` … 100; 100 → 127 | | heightmap |
| Building bottom limits | > min build + 2; top < max build − 8 | | city chunk terrain |
| Clear above roof | to sampled height + 10 | | city chunk terrain |
| Blend: fixed corner | city ground −4…−3 (lower), −1…+1 (upper) | | terrain blend |
| Blend: neighbor slack | 20–24 (edge), 25–29 (diagonal) per step; ±90 fallback | blocks | terrain blend |
| Blend: "fixed" test | < 256 (hard-coded) | | terrain blend |
| Ocean corner height | `GROUNDLEVEL − 4` = 67 | | terrain blend |
| Explosion center Y | 6 × level + 75…89 (big), 6 × level + 60…99 (mini) | absolute, not relative to `GROUNDLEVEL` | damage |
| Damage pass | Y 0–255 only | | damage |
| Debris start | building top + 10 | | damage |
| Ruins | from city ground + 1 + ruin fraction × floors × 6 to building top + 10 | | damage |
| Void probe | Y 255 down | floating | pipeline |
| Void-biome probe | Y 60 at chunk center | | pipeline |
| Sphere center Y | `GROUNDLEVEL` | | spheres |
| Space border cutoff | Y > 5; heights ≤ 5 are void | | city chunk terrain |
| Hard-coded floors | 0 or 1 in several loops (`TERR-24`) | | several |
| Highway city constraint | `CITY_MINHEIGHT` 50 … `CITY_MAXHEIGHT` 150 | | heightmap |

### 1.20 dependencies

Each row is also for `API-MAP.md`.

| Mechanism | Where | Why it matters for 1.7.10 |
|---|---|---|
| Feature + configured/placed feature + Forge biome modifier, decoration steps `raw_generation` and `top_layer_modification` | `data/lostcities/worldgen/**`, `data/lostcities/forge/biome_modifier/**`, `LostCityFeature`, `LostCitySphereFeature` | No feature registry or decoration steps; hooks are the chunk provider (`provideChunk`), `populate` (+8 offset), `PopulateChunkEvent`, `DecorateBiomeEvent`, `ChunkProviderEvent.ReplaceBiomeBlocks`, or a mixin into RWG. Defines *when* the city is written relative to caves, lakes, ores and vegetation. |
| `WorldGenRegion` 3 × 3 writable window, `ChunkStatus` gating (`FEATURES`, `FULL`) | `ChunkFixer`, `ChunkDriver.updateAdjacent`, `LostCityFeature` | 1.7.10 has no statuses; cross-chunk writes during population land in up to 4 chunks and may cascade. Vines, debris and neighbor shapes need a new rule. |
| Multithreaded chunk generation | `LostCityFeature` locks, `GenerationContext` thread-locals, `TimedCache` | 1.7.10 generates on the server thread; locking is unnecessary but the per-chunk scoping and seeds still apply. |
| Height prediction from the noise router (`ChunkGenerator.getBaseHeight`, `OCEAN_FLOOR_WG`, density functions, aquifers) | `ChunkHeightmap`, `HeightGenOpt`, `NoiseChunkOpt`, access transformer | No "height without generating" API in 1.7.10 or RWG. The most important S2 question: how to predict RWG surface height for any chunk, deterministically. |
| World height −64 to 320 from the level | `ChunkDriver.SectionCache`, leveling, building limits, spheres | 1.7.10 is 0–255 (16 sections). Several loops already assume 0 or 255 (`TERR-24`), which happen to match 1.7.10. |
| Generator sea level (`ChunkGenerator.getSeaLevel`) | `Tools.getSeaLevel`, `BuildingInfo.waterLevel`, damage, spheres | 1.7.10 has a fixed sea level of 63 for vanilla; RWG's own sea level must be checked. |
| Block states with shape properties and `updateShape` (stairs shape, walls, fences, panes) | `ChunkDriver.correct` | 1.7.10 stores metadata; fences, panes and walls connect at render time, stairs have no shape. Most correction is unnecessary. |
| Structure void as the "keep existing block" marker | `generatePart` (`HardAirSetting.VOID`), `ChunkDriver.correct` | No structure void in 1.7.10; a sentinel value is needed. |
| Direct section writes (`BulkSectionAccess`, `LevelChunkSection.setBlockState`) and `Heightmap.Types` | `ChunkDriver.actuallyGenerate` | 1.7.10: `Block[]`/`byte[]` primer arrays or `ExtendedBlockStorage`, one height map, precipitation height, light recalculation. |
| Pending block-entity NBT (`ChunkAccess.setBlockEntityNbt`), `SpawnData` codec | `handleBlockEntity`, `handleSpawner` | 1.7.10 creates `TileEntity` objects directly; spawner NBT uses `EntityId`/`SpawnData` with a different layout. |
| POI system | `generatePart` (`needspoi` tag, dirt placeholder) | No POI in 1.7.10; the workaround is unnecessary. |
| Loot tables (`RandomizableContainerBlockEntity.setLootTable`) | `createLoot` | 1.7.10 uses `ChestGenHooks` and weighted content (08-loot-mobs-blocks). |
| Block tags (`saplings`, `small_flowers`, `climbable`, `lostcities:foliage`, `lights`, `needspoi`, `rotatable`, `not_breakable`, `easy_breakable`) | terrain feature, `DamageArea` | No block tags; explicit lists or ore dictionary. |
| Biome tags (`is_overworld`, `forge:is_void`, `is_ocean`, `is_deep_ocean`, `is_beach`, `is_river`) | biome modifiers, `LostCityFeature`, `isWaterBiome` | `BiomeDictionary` types; RWG biomes register their own. |
| Vanilla 1.20-only blocks: `MOSS_BLOCK`, `STRUCTURE_VOID`, `WALL_TORCH` as its own block | default rubble table, parts, `fixTorches` | Need 1.7.10 or pack substitutes (decision 0004 mapping table). |
| `SavedData` / `DimensionDataStorage` | `LostCityWorldGenData` | `WorldSavedData` with `MapStorage`. |
| Scheduled ticks and `ServerChunkCache.blockChanged` | command-block post job | `scheduleBlockUpdate`, `markBlockForUpdate`. |
| `RandomSource`, `LegacyRandomSource`, `fork()` | `GenerationContext`, sapling growth | `java.util.Random`. |
| `SaplingBlock.advanceTree`, `isAreaLoaded` | `GlobalTodo` jobs | `BlockSapling.func_149878_d` (grow tree) exists; area checks via `checkChunksExist`. |
| Lost Worlds generators and `ILostWorldsChunkGenerator` | landscapes, `Spheres` | Nothing equivalent in GTNH; non-`DEFAULT` landscapes have no terrain source. |
| Forge `ForgeConfigSpec` values (`cacheCleanupSeconds`, `heightSampleSize`, `optimizedHeightmap`, `avoidFlattening`, `todoQueueSize`, `forceSaplingGrowth`) | throughout | GTNHLib config (02-profiles-dimensions owns config). |
| Java 9–17 APIs: `Random.nextInt(int, int)`, `Set.of`, `Set.copyOf`, `Map.copyOf`, records, switch expressions | terrain feature, `GlobalTodo` | Java 8 runtime: no such methods; records not allowed. |

## Related areas

- `01-core`: `LostCityEvent` API events posted by the pipeline, `ForgeEventHandlers` (tick jobs, cache cleanup, new-world signal), `TimedCache`, `TodoQueue`, noise helpers, commands that read heightmaps and statistics.
- `02-profiles-dimensions`: every profile option used here, `LandscapeType`, `DefaultDimensionInfo`, profile selection per dimension, the outside profile.
- `04-city-layout`: `BuildingInfo` (city flag, levels, buildings, street types, corner meshes, ruin roll, damage area), city spheres' use in layout, doors, bridges, stair and slope decisions.
- `05-assets`: parts, palettes, city styles, world styles (vines, sphere settings), part metadata (`nowater`, `forcedair`, `z1`, `z2`), `Transform`.
- `06-highways-railways`: highways, railways, corridors, monorails, the highway saved data; they call `clearRange` and `fillToGround` here.
- `07-scattered`: scattered structures, structure avoidance (which can switch off the blend), "stuff".
- `08-loot-mobs-blocks`: loot and spawner selection called from `generatePart`, the block tags.
- `09-client-ui`: the GUI preview path (`NullDimensionInfo` builds its own terrain feature).

## Coverage

- Source files read: **23 of 23** in scope, each in full.
  - Java (17): `worldgen/ChunkDriver`, `ChunkFixer`, `ChunkHeightmap`, `ErrorLogger`, `GenerationContext`, `GlobalTodo`, `HeightGenOpt`, `LostCityFeature`, `LostCitySphereFeature`, `LostCityTerrainFeature`, `LostCityWorldGenData`, `NoiseChunkOpt`; `worldgen/lost/CitySphere`, `DamageArea`, `Explosion`; `worldgen/gen/Spheres`; `setup/TerrainEventHandlers`.
  - Resources (6): `data/lostcities/worldgen/configured_feature/{lostcities,spheres}.json`, `worldgen/placed_feature/{lostcities,spheres}.json`, `forge/biome_modifier/{lostcities,lostcity_spheres}.json`.
- In-scope files not covered: none.
- Out-of-scope files read (in part unless noted), to resolve inputs and callers: `worldgen/DefaultDimensionInfo` (full), `worldgen/lost/BuildingInfo` (levels, corner meshes, street-type choice, todo storage, building bottom), `worldgen/plan/ChunkPlanner` (city-level cache), `config/LostCityProfile` (defaults and landscape helpers), `config/ProfileSetup` (sphere and space profiles), `config/LandscapeType` (full), `config/StreetGenerationMode`, `config/HighwayGenerationMode`, `setup/Config` (terrain keys), `setup/ForgeEventHandlers` (tick and cleanup), `varia/TimedCache` (full), `varia/TodoQueue` (full), `varia/Tools` (`getSeaLevel`), `api/LostCityEvent` (cancelable events), `api/ILostWorldsChunkGenerator` (full), `api/ILostCities` (meta keys), `worldgen/gen/Highways` (predicate clears), `worldgen/lost/regassets/data/CitySphereSettings`, `WorldSettings` (vine fields), `data/lostcities/dimension/lostcity.json`, `dimension_type/lostcity.json`, `META-INF/accesstransformer.cfg`, `plan_performance.md`, `changelog.txt` (Lost Worlds lines). Upstream `1.12` at `e8900023`: `dimensions/world/LostCityChunkGenerator`, `terraingen/LostCitiesTerrainGenerator` (generate, core chunk, normal chunk, flatten, city chunk start, post-explosion fix), `ChunkHeightmap` (full), `LostGenCaves` (full).
- Assets: none in scope beyond the six JSON files. Textures: none. Sounds: none.
- Lang keys: 0 of 0 in scope. Hard-coded English: one player-visible chat message, `There was an error generating a chunk. See log for details!` (`LostCityFeature.java:67`, sent through `ErrorLogger.report`); log and exception texts are not player-facing.
- Corrections to earlier statements: none (first version, 2026-11-01).

## Known limits

- Nothing was run. All in-world descriptions (floating plants over lowered terrain, water pockets, rubble placement) are *inferred* from the code.
- The landscape types other than `DEFAULT` need the Lost Worlds mod, which was not read; their terrain shapes are described only as far as Lost Cities' code assumes them.
- The `NoiseChunkOpt` comparison with vanilla `NoiseChunk` is from knowledge of vanilla 1.20.1, not a line diff against decompiled vanilla (none is in the repo).
- Generators in other areas called by the pipeline (bridges, highways, railways, corridors, monorails, scattered, stuff, doors) are linked, not documented here; their own cross-chunk writes are theirs to list.
- `BuildingInfo` internals were read only where this area consumes them; 04-city-layout owns that class.
- 1.12 notes come from targeted reading of the files listed under Coverage, not the whole 1.12 tree.

## Suspected bugs

| ID | Title | Location | What looks wrong | Severity |
|---|---|---|---|---|
| TERR-01 | Sphere radius factor applies to the random part only | `worldgen/lost/CitySphere.java:263` | `CITY_MINRADIUS + nextInt(MAX − MIN) × CITYSPHERE_FACTOR`: by operator precedence the factor scales only the random part, so with defaults radii run 50–142 blocks rather than the whole radius being scaled (60–152); predefined-city spheres, by contrast, scale the whole radius. | worldgen |
| TERR-02 | City sphere cache depends on query order | `worldgen/lost/CitySphere.java:483` | `updateCache` writes a sphere (enabled or not) under every chunk its circle touches, overwriting earlier entries. Where two cells' circles overlap (radius over about 120 blocks, or the 32 grid, whose off-grid cells compute a disabled sphere), a chunk keeps whichever sphere was computed last. The cache is static and unbounded. | worldgen |
| TERR-03 | `nextInt(max − min)` ranges crash when equal and never reach max | `worldgen/lost/DamageArea.java:105` (also `:107`, `:116`, `:118`; `worldgen/lost/CitySphere.java:263`) | An equal min and max (radius or height) throws `IllegalArgumentException` during `BuildingInfo` construction; otherwise the maximum is exclusive. | crash |
| TERR-04 | Debris from tall buildings starts at the bottom of the world | `worldgen/LostCityTerrainFeature.java:2113` | When the neighbor's top + 10 exceeds `maxBuild − 1`, `h` is set to `minBuild − 1` instead of being capped, so debris is placed at the minimum build height, replacing bedrock. | worldgen |
| TERR-05 | Debris landing height is not reset per block | `worldgen/LostCityTerrainFeature.java:2127` | `h` keeps descending across all debris blocks; a later block in a higher column is placed at the old `h + 1`, inside walls or terrain, overwriting solid blocks. | worldgen |
| TERR-06 | Water-mode clearing skips the water-level row | `worldgen/LostCityTerrainFeature.java:533` (also `:543`) | Liquid fills to `waterLevel − 1` and air starts at `waterLevel + 1`, so the block at `waterLevel` keeps old terrain inside drowned buildings and highways. | worldgen |
| TERR-07 | Street-type re-roll at generation time | `worldgen/LostCityTerrainFeature.java:1234` (same expression at `worldgen/lost/BuildingInfo.java:897`) | `nextInt(0, values().length − 2)` is always 0, so every non-elevated, non-open-lot street becomes `NORMAL` (non-elevated parks vanish; `FULL` is unreachable). The re-roll writes the shared, cached `BuildingInfo.streetType`, which neighbors read through memoized stair directions and front-part checks, so output depends on generation order. Seeded without the world seed. | worldgen |
| TERR-08 | Random vegetation is asymmetric | `worldgen/LostCityTerrainFeature.java:1523` (also `:1469`, `:1479`, `:1487`) | The +Z side lacks the 0.8 probability cap; −X/−Z sides weight T + 1…2, +X/+Z weight 1…T and never use column 15. | cosmetic |
| TERR-09 | Front parts are written with the neighbor's `BuildingInfo` | `worldgen/LostCityTerrainFeature.java:1449` (effects at `:1903`, `:1928`, `:400`) | Spawner and block-entity NBT positions use the neighbor's chunk origin, so the NBT lands in the neighbor at the mirrored spot; torch and loot jobs are queued on the neighbor and either run in the neighbor's pass (torches written at aliased positions, `TERR-14`) or never, if the neighbor already generated. | worldgen |
| TERR-10 | Terrain correction mutates cached heightmaps | `worldgen/LostCityTerrainFeature.java:675` (and `:878`–`882`) | `setHeight` changes the cached height other chunks read for borders, the ±90 fallback, scattered and bridge checks; with sample size > 1 the first requester gets an uncached object, and entries expire after 300 s and come back raw. Results depend on order and timing. | worldgen |
| TERR-11 | Building vines depend on neighbor status | `worldgen/ChunkFixer.java:41` (also `:56`, `:72`, `:87`) | Vines on a shared +X/+Z wall are placed only if that neighbor is already at `FEATURES`; nobody revisits the face later, so vines exist or not by generation order. | worldgen |
| TERR-12 | Cross-chunk shape updates are dropped | `worldgen/ChunkDriver.java:180` | Neighbor shape changes are written only when the neighbor chunk is `FULL`, which never happens during generation, so fences, walls, panes and stairs across chunk borders connect only from the later chunk's side. | visual |
| TERR-13 | Heightmaps can only rise after the commit | `worldgen/ChunkDriver.java:52` | Updating with a bedrock state at the top staged Y never lowers `MOTION_BLOCKING`, `OCEAN_FLOOR` or `WORLD_SURFACE` where terrain was lowered or blasted, so later decoration in the chunk uses the old surface (floating plants, *inferred*). | visual |
| TERR-14 | Driver cache aliases positions outside the chunk | `worldgen/ChunkDriver.java:420` (also `:458`) | The cache indexes by `x & 15`, `z & 15` only; any cursor outside the chunk reads or writes this chunk's column. Triggered by `TERR-09`'s torches. | worldgen |
| TERR-15 | Every read is written back at commit | `worldgen/ChunkDriver.java:78` | `getBlock` stages what it read, so the commit rewrites every inspected block (whole columns during leveling). Wasted work, and a hazard if the region changes between read and commit. | performance |
| TERR-16 | Sphere clearing writes past the build limit | `worldgen/gen/Spheres.java:118` (also `:126`) | `CITYSPHERE_CLEARABOVE` clears up to `maxBuild` inclusive, and `CLEARABOVE_UNTIL_AIR` has no upper bound; a write at `maxBuild` indexes past the section array. With no guard in the sphere feature this crashes generation. Config-only. | crash |
| TERR-17 | Clear-below-until-air never stops below sea level | `worldgen/gen/Spheres.java:146` | The stop test expects liquid at or below the generator's sea level but the loop writes liquid only at or below the outer sea level (−1000 by default); below sea level with no liquid it clears down to the minimum build height. Enabled in the `space` profile. | worldgen |
| TERR-18 | Explosions flood enclosed air below the water level | `worldgen/lost/DamageArea.java:88` (and `worldgen/LostCityTerrainFeature.java:484`) | Air at or below the chunk's `waterLevel` is "damaged" into liquid, so cellars and caves inside a blast fill with water source blocks; the gate uses the profile water level and the replacement the generator sea level. | worldgen |
| TERR-19 | Damage pass covers Y 0–255 only | `worldgen/LostCityTerrainFeature.java:455` (also `worldgen/lost/DamageArea.java:39`) | Blocks below 0 or above 255 inside an explosion are never damaged on a −64 to 320 world. | worldgen |
| TERR-20 | Deferred tree growth is dropped or cascades | `worldgen/LostCityTerrainFeature.java:1974` (queue at `worldgen/GlobalTodo.java:57`) | A job whose 1-block area is unloaded is consumed without effect (the sapling stays); a loaded one grows a full tree on a later tick, which can reach unloaded or ungenerated chunks; run order is a hash-map copy's. | worldgen |
| TERR-21 | Global job queues outlive the world | `worldgen/GlobalTodo.java:33` | The static per-dimension map is never cleared, so in single player, jobs from a world that was left run in the next world with the same dimension key. | gameplay |
| TERR-22 | Cavern heightmap rule no longer fits one sample | `worldgen/ChunkHeightmap.java:51` | Written for per-block scanning: one sampled height above 100 is ignored (height falls back to `GROUNDLEVEL`), exactly 100 becomes 127. | worldgen |
| TERR-23 | Lowering under water leaves air under the water | `worldgen/LostCityTerrainFeature.java:751` (clear at `:774`) | Water counts as empty when finding the surface, so lowering a lake bed above the target clears the bed to air and leaves the water column suspended (*inferred*). | visual |
| TERR-24 | Hard-coded Y bounds on a −64 to 320 world | `worldgen/LostCityTerrainFeature.java:728` (also `:781`, `:1466`, `:1484`, `:1502`, `:1520`, `:2127`, `:1341`, `:1436`, `:271`; `worldgen/lost/BuildingInfo.java:2009`, `:2069`) | Column copies and scans stop at 0 or 1, void checks start at 255, mesh "fixed" tests use 256, ignoring the level's real limits. | worldgen |
| TERR-25 | Generation errors are swallowed after partial writes | `worldgen/LostCityFeature.java:63` | Exceptions are caught and only logged (twice) and announced in chat; cross-chunk writes and early commits (`generateBuilding` with second parts) remain, leaving a half-built chunk. | worldgen |
| TERR-26 | Border vine scan is heavy and repeated | `worldgen/ChunkFixer.java:154` (second run from `worldgen/gen/Spheres.java:57`) | Every chunk scans 128 full columns from the surface to the min build height; sphere worlds run the whole fixer twice per chunk. | performance |
| TERR-27 | Some seeds ignore the world seed | `worldgen/LostCityTerrainFeature.java:352` (also `:552`–`568`, `:1233`) | Block damage, debris, blend offsets and the street re-roll use coordinate-only seeds, so every world shares those patterns. | cosmetic |
| TERR-28 | No cleanup of floating fragments after explosions | `worldgen/LostCityTerrainFeature.java:356` | 1.12 removed or dropped disconnected blobs (`fixAfterExplosionNew`); 1.20 has no such pass, so blasted buildings keep floating pieces, against 0004's "no floating debris". | visual |
| TERR-29 | Ruin column cap compares a count with a Y | `worldgen/LostCityTerrainFeature.java:1160` | The step count (`top + 10 − start`) is capped at `maxBuild − 2`, a Y value, so it never limits anything. The column runs to the building top + 9; a building at the highest top `getBuildingBottomHeight` allows (`maxBuild − 9`) writes at `maxBuild` and crashes (edge case). | crash |
| TERR-30 | Ruin leaves can fall forever | `worldgen/LostCityTerrainFeature.java:1181` | The drop to the first solid block has no lower bound; a column empty to the bottom (floating or void worlds) walks below the build range and throws. | crash |
| TERR-31 | Degenerate building bottom is not handled | `worldgen/LostCityTerrainFeature.java:2192` | `getBuildingBottomHeight` can return `Integer.MIN_VALUE`, which is used as a Y; the first read throws (caught by `TERR-25`, leaving the chunk unbuilt). | crash |
| TERR-32 | Vines never use the last column of a wall | `worldgen/ChunkFixer.java:45` (also `:60`, `:76`, `:91`) | Loops run 0–14, so column 15 of every wall gets no vines. | cosmetic |
| TERR-33 | Error chat throttle and logging | `worldgen/ErrorLogger.java:21` (and `worldgen/LostCityFeature.java:64`) | The throttle timestamp is a plain static written from worker threads; errors are logged and also printed to stderr. | cosmetic |
| TERR-34 | `optimizedHeightmap` changes the world | `worldgen/HeightGenOpt.java:13` (switch at `worldgen/LostCityTerrainFeature.java:930`) | The optimized path drops the lava aquifer and other router parts and can differ from vanilla's height (upstream's own debug code checks for > 4 blocks), so flipping the option changes city levels for the same seed. | worldgen |
| TERR-35 | Sphere feature has no exception guard | `worldgen/LostCitySphereFeature.java:35` | Unlike the city feature, any exception in sphere drawing (for example `TERR-16`) propagates out of world generation. | crash |
| TERR-36 | Predicate range writes skip unstaged blocks | `worldgen/ChunkDriver.java:397` | The predicate variant replaces only positions already staged or read, so highway "clear above the deck" passes leave vanilla blocks that nothing else touched (*inferred* for hillside highways). | worldgen |
| TERR-37 | Rubble height interpolation is reversed | `worldgen/LostCityTerrainFeature.java:1113` | `bipolate` weights put the diagonal neighbor's ground at the chunk center and this chunk's toward the corners; rubble then fails to appear where levels change. | cosmetic |

<!-- Merged and deduplicated into BUGS.md. Suspected, not confirmed; no fixes designed here. -->
