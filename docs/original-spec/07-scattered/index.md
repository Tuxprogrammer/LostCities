# 07 · Scattered structures, structure avoidance and stuff

## Overview

Three systems that work at the edges of cities: **scattered structures** (single buildings and multibuildings placed in the countryside, some beside highways), **structure avoidance** (keeping cities off villages and other vanilla structures), and **stuff** (small decorative block columns such as hanging chains and cobwebs inside city buildings). It documents Lost Cities **1.20-7.5.7** (branch `1.20`, commit `f18a094d`).

## Contents

- [scattered-structures.md](scattered-structures.md): area plans, weighted selection, normal and near-highway placement, terrain fitting, generation, railing openings, the bundled scattered assets
- [structure-avoidance.md](structure-avoidance.md): the avoidance check, footprint decisions, config keys and effects on cities and terrain
- [stuff.md](stuff.md): stuff selection by city-style tag, placement, the bundled stuff assets

## Key facts

- Scattered structures use **areas** of `areasize` chunks (8 in both bundled world styles). Each area holds at most one structure, planned once (`QualityRandom` seeded from the world seed and area index) and cached for the life of the server.
- The plan's random draws are **replayed** for each chunk, so every chunk of a multibuilding sees the same random stream.
- Scattered structures only appear in non-city chunks without a bridge or highway, after bridges and highways are written.
- Structure avoidance reads structure references from the active `WorldGenRegion` only; its answers depend on what has been generated nearby (SCAT-01, SCAT-02).
- Stuff runs for every city chunk, using one shared generation random reseeded from chunk coordinates only.

### 1.20 dependencies

| Mechanism | Where | Why it matters for 1.7.10 |
|---|---|---|
| Per-chunk structure references and `ChunkStatus.STRUCTURE_REFERENCES` | `StructureAvoidance` | 1.7.10 keeps structure starts inside each `MapGenStructure`; no per-chunk reference API |
| `WorldGenRegion.getChunk(..., false)` non-loading lookups | `StructureAvoidance` | No region view in 1.7.10; lookups must not cascade |
| Structure registry, `#minecraft:village` tag, structure ids `mansion`, `igloo`, `pillager_outpost` | avoidance defaults | Villages are `MapGenVillage`; temples and witch huts are `MapGenScatteredFeature`; mansions, igloos and outposts do not exist |
| Biome tags (`#is_ocean`, `#is_deep_ocean`, `#is_river`, `#is_beach`) in world-style and stuff filters | selection, terrain stats, stuff | `BiomeDictionary` types in 1.7.10 |
| Block tags (`#forge:stone`) in stuff matchers | stuff | No block tags in 1.7.10 |
| `ChunkGenerator.getBaseHeight` with `OCEAN_FLOOR_WG`, `RandomState`, `NoiseBasedChunkGenerator` | `calculateAccurateHeight` (scattered terrain stats) | RWG height sampling instead (`03-terrain`) |
| Generator sea level via a `ServerChunkCache` cast | `ocean` terrain height | Sea level 63 or RWG's |
| World minimum height -64 | void threshold (`minBuild + 3`), fallback (`minBuild + 2`) | 0 in 1.7.10 |
| `minecraft:chain` block | `chains` stuff (palette `{`) | Added in 1.16; needs a pack substitute |
| `WorldGenLevel.canSeeSky` during generation | stuff `seesky` | Different timing in 1.7.10 population |
| `ForgeConfigSpec` server config | avoidance keys | Forge `Configuration` or GTNHLib config |
| Data-pack registries and codecs (`ScatteredRE`, `StuffSettingsRE`, world styles) | all assets | Loaded from the jar; JSON parsed by hand |
| Java records, `Direction.Plane`, `RandomSource` | `Scattered`, `Stuff` | Java 8 classes and `java.util.Random` in a port |

## Related areas

- `03-terrain`: `doNormalChunk` and `doCityChunk` call these systems; heightmaps, `generatePart`, `clearRange`, terrain correction (skipped for avoided chunks).
- `04-city-layout`: avoidance decides city membership ([chunk-characteristics](../04-city-layout/chunk-characteristics.md)); scattered structures avoid raw city and bridge chunks; `BuildingInfo` supplies building and height data.
- `05-assets`: scattered, stuff, building, multibuilding, palette and world-style formats; biome, block and resource-location matchers; `stuff_tags` inheritance.
- `06-highways-railways`: highway levels and tunnels for near-highway placement; highways open their railings toward scattered buildings.
- `01-core`: the server config holding the avoidance keys; cache clearing.

## Coverage

- Source files read in full: **15 of 15** in scope for this area: `worldgen/lost/StructureAvoidance.java`, `worldgen/gen/{Scattered, Stuff}.java`, `worldgen/lost/cityassets/{ScatteredBuilding, StuffObject}.java`, `worldgen/lost/regassets/data/{ScatteredSettings, ScatteredReference}.java`, `data/lostcities/lostcities/scattered/{cabin, highway_gas_station, highway_restaurant, oilrig, radiotower}.json`, `data/lostcities/lostcities/stuff/{chains, cobweb, example}.json`.
- In-scope files not covered: none.
- Out-of-scope files read for behavior (in part unless noted): `setup/Config.java` (avoidance keys and defaults), `worldgen/lost/regassets/{ScatteredRE, StuffSettingsRE (full)}.java`, `worldgen/lost/cityassets/{AssetRegistries, RegistryAssetRegistry, Building, ConditionContext}.java`, `worldgen/{LostCityTerrainFeature, ChunkHeightmap (full), GenerationContext}.java`, `api/MultiPos.java` (full), `resources/data/lostcities/lostcities/worldstyles/{standard, standard_everywhere}.json`, `palettes/common.json` (chain and cobweb entries), `citystyles/citystyle_common.json` (`stuff_tags`).
- Assets covered: 5 of 5 scattered assets, 3 of 3 stuff assets.
- Lang keys: none in scope (0 of 0). Hard-coded English only in exception messages (`"Missing buildings for scattered …"`, `"Scattered buildings only support rotations"`).
- Corrections to earlier statements: none (first version, 2026-11-01).

## Known limits

- Nothing was generated in game; placement and appearance statements are *inferred* from code.
- Structure avoidance's dependence on `WorldGenRegion` timing could not be measured; SCAT-01 and SCAT-02 are read from the code paths.
- The palette mappings for stuff columns other than `{` and `\` are not resolved here (`05-assets`).
- The `example` stuff asset has no tags and is unreachable; it is documented as unused.

## Suspected bugs

| ID | Title | Location | What looks wrong | Severity |
|---|---|---|---|---|
| SCAT-01 | Single-chunk avoidance window is shifted by (+1, +1) | `worldgen/lost/StructureAvoidance.java:84-87` | Bounds use `chunk - multiPos.x`; for single chunks `MultiPos.SINGLE` has `x = z = -1` (`api/MultiPos.java:7`), so the window is centered on the south-east diagonal neighbor. With margin 0 the chunk's own structures are never seen; with margin 1 the north and west neighbors are skipped and chunks two away to the south and east are included. | worldgen |
| SCAT-02 | Multibuilding footprint decision fails open and is cached for good | `worldgen/lost/StructureAvoidance.java:92` (resolver `:140-157`) | Chunks without references yet are skipped and the first answer is cached until `cleanCache`, so a footprint can be allowed or avoided depending on generation order. | worldgen |
| SCAT-03 | Footprint decision cache is unbounded | `worldgen/lost/StructureAvoidance.java:36` | `ConcurrentHashMap` never evicted while the server runs. | performance |
| SCAT-04 | Default avoid list names a structure that does not exist | `setup/Config.java:45` | `minecraft:swamp_huts`; the 1.20 structure id is `minecraft:swamp_hut`, so witch huts are not avoided. | gameplay |
| SCAT-05 | Scattered weighted pick is off by one | `worldgen/gen/Scattered.java:360` | `rndweight <= weight` gives the first matching entry one extra chance and "none" one fewer. | worldgen |
| SCAT-06 | Zero total weight crashes | `worldgen/gen/Scattered.java:356` | `nextInt(totalweight + weightnone)` throws when both are 0 for the kept entries. | crash |
| SCAT-07 | Selection filters by the area origin's biome | `worldgen/gen/Scattered.java:132` (also `:346`) | The candidate list is filtered by the biome of the area's corner chunk, not the chosen placement, so valid structures elsewhere in the area are excluded and the pick can then fail on the real footprint. | worldgen |
| SCAT-08 | `repeatslice` fill has no lower bound | `worldgen/gen/Scattered.java:451` | The downward loop runs while the block is exactly default air or the profile liquid, with no minimum-height check (unlike `Supports`); over void it can run off the bottom of the world. Other air variants and fluids stop it early. | crash |
| SCAT-09 | Single-chunk terrain-height modes are identical | `worldgen/gen/Scattered.java:471-474` | `lowest`, `average` and `highest` all return the heightmap height; the accurate min/max computed for the chunk are unused. | worldgen |
| SCAT-10 | Scattered floors never match top-of-building conditions | `worldgen/gen/Scattered.java:398-401` | Floors run `0 .. floors-1` with `floorsAboveGround = floors`, so `isTopOfBuilding` is never true (upstream TODO); the context's level is a block Y, not a city level. | worldgen |
| SCAT-11 | Part2 condition context lacks the chosen part | `worldgen/gen/Scattered.java:424` | City buildings pick part2 with a context naming the chosen part; scattered buildings reuse the part-less context. | worldgen |
| SCAT-12 | Terrain stats mutate a shared heightmap | `worldgen/gen/Scattered.java:294` | `calculateAccurateHeight` writes min/max into a cached `ChunkHeightmap` shared by a sample group and across threads; concurrent plans can read another chunk's values. | worldgen |
| SCAT-13 | Part2 is placed one part too high | `worldgen/gen/Scattered.java:463-465` | Part2 is generated at the height returned after the floor part (its top), not at the floor's own base as city buildings do. | worldgen |
| SCAT-14 | Plan cache is unbounded | `worldgen/gen/Scattered.java:37` | `PLANS` is never evicted, one entry per area ever touched. | performance |
| SCAT-15 | Near-highway placement search is costly | `worldgen/gen/Scattered.java:221-237` | Up to 4 rotations × every position in the area × every footprint chunk, each building `BuildingInfo`, biome and heightmap data for chunks far from the one generating. | performance |
| SCAT-16 | Avoided-structure list is cached for the JVM lifetime | `setup/Config.java:141` | `AVOID_STRUCTURES_SET` is built once and never reset, so config changes need a restart. | gameplay |
| SCAT-17 | Stuff count draw crashes and never reaches `maxcount` | `worldgen/gen/Stuff.java:74` | `nextInt(maxcount - mincount)` throws when they are equal and excludes `maxcount`. | crash |
| SCAT-18 | Stuff height draw crashes on an empty range | `worldgen/gen/Stuff.java:78` | `nextInt(maxheight - minheight)` throws when `maxheight <= minheight` (possible with an explicit `minheight` above the default `maxheight`). | crash |
| SCAT-19 | Stuff without `inbuilding` never generates | `worldgen/gen/Stuff.java:32` | An unset optional field disables the asset instead of meaning "either". | gameplay |
| SCAT-20 | Stuff building filter uses the candidate building in street chunks | `worldgen/gen/Stuff.java:34` | For `inbuilding: false` the `buildings` matcher tests the building that would have stood on the street. | worldgen |
| SCAT-21 | Stuff random ignores the world seed | `worldgen/gen/Stuff.java:23` | Seeded from chunk coordinates only. | cosmetic |
| SCAT-22 | Stuff order follows hash-map iteration | `worldgen/lost/cityassets/AssetRegistries.java:65-69` | `STUFF_BY_TAG` lists are built by iterating a `ConcurrentHashMap`, and their order decides random consumption. | worldgen |
| SCAT-23 | Stuff `seesky` reads live world state | `worldgen/gen/Stuff.java:83` | `canSeeSky` on the generation region depends on its current heightmaps, not on the chunk being written. | worldgen |

<!-- Merged and deduplicated into BUGS.md by the orchestrator. Suspected, not confirmed; no fixes designed here. -->
