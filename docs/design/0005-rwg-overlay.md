# 0005 · The RWG overlay: hook, terrain blend, heights, profiles and budget

- Status: Accepted; §3 and §3a amended by D31 (saplings, light, the carver, the write guard); §1, §3a, §6, §7 and §8 settled by the M04 implementation (decision log D73 to D75, D81)
- Gate: S2 → M04 (M04's entry gate needs this record accepted)
- Amends: none (settles the "Still open" items of [0003](0003-world-presence.md))
- Date: 2026-11-01

## Context

[0003](0003-world-presence.md) puts cities in the GTNH overworld as an overlay on RWG2 Continents and classic RWG terrain, never as a world type of our own. It leaves open how the overlay hooks into generation and how city terrain meets RWG terrain. This record answers open questions Q1, Q2, Q7 and Q10. The evidence, with pinned source links, is in [new-spec/00-pack-integration/rwg.md](../new-spec/00-pack-integration/rwg.md). The facts that drive the decision:

- Both RWG versions build a chunk in `provideChunk` in the same order: heights, a raw stone and water fill (sea level 63), `ChunkProviderEvent.ReplaceBiomeBlocks`, surface painting and bedrock, caves, structure starts, then (RWG2 continents) magma chambers and lava caves.
- Their `populate` posts `PopulateChunkEvent.Pre`, places structures, lakes, dungeons, ores and decoration, posts `PopulateChunkEvent.Post`, **then** adds snow and ice.
- RWG honors `Populate` vetoes (`LAKE`, `LAVA`, `DUNGEON`, `ANIMALS`) but **ignores `DecorateBiomeEvent.Decorate` results**, so its trees, boulders and small ruins cannot be vetoed by event.
- Neither fires `InitNoiseGensEvent`. Both fire `InitMapGenEvent` for caves, villages, strongholds and mineshafts.
- RWG2 2.0.6 has a **world-less generator** (`ChunkGeneratorRealistic(manager, seed, continental)`) whose `getNewNoise` returns a chunk's heights without generating it. Classic RWG has only the world-bound constructor. On either version, calling `getNewNoise` on the **live** generator corrupts the next chunk.
- Forge makes `ChunkProviderServer.currentChunkProvider` public, and `ChunkProviderServer.populate` runs the provider's `populate` and then every `IWorldGenerator` (GT's ore veins forced last).
- For a new world, Forge posts `WorldEvent.CreateSpawnPosition` after the chunk provider exists and before the spawn search generates the first chunk. `WorldEvent.Load` for the overworld comes after the spawn search.
- Hodgepodge (pack default) throttles generation to 20 ms per tick, populates a chunk only once its 3×3 neighbors have terrain, and defers nested population. ArchaicFix provides the cascading-worldgen warning, which the pack switches off.
- PersonalSpace's generator fires no terrain events, so only a provider-level hook reaches it.
- Upstream 1.20 writes cities in the first decoration step, after carvers and before all other decoration. It takes a chunk's height from the generator's noise-only base height at block (8, 8), sampled per 3×3 chunk group. It blends edges by moving terrain columns of non-city chunks within two chunks of a city between a lower and an upper height mesh.
- Upstream 1.12 combined Lost Cities with other terrain only through a separate world type and an opt-in API (`IChunkPrimerFactory`: raw terrain plus a height estimate) that only ATG implemented.

## Decision

### 1. Hook: a delegating chunk provider (Q1)

The overlay is a class in `worldgen` that implements `IChunkProvider` and wraps the dimension's original provider. It replaces `ChunkProviderServer.currentChunkProvider` for each dimension the config enables (the overworld only, by default).

| Concern | Rule |
|---|---|
| Install, new world | In `WorldEvent.CreateSpawnPosition`, subscribed with `priority = HIGHEST, receiveCanceled = true`, before any chunk exists |
| Install, existing world or other dimension | In `WorldEvent.Load`, if not already wrapped |
| Idempotence | Never wrap a wrapper; keep one wrapper per `WorldServer`; drop it on `WorldEvent.Unload` |
| Forwarding | `loadChunk` goes through the wrapper's own `provideChunk`, as vanilla's and RWG's generators define it, so no path skips the terrain phase (D74). Every other `IChunkProvider` method delegates unchanged, including `chunkExists`, `saveChunks`, `unloadQueuedChunks`, `canSave`, `getPossibleCreatures`, `func_147416_a` (stronghold search), `getLoadedChunkCount`, `recreateStructures` and `saveExtraData`. `makeString` returns the delegate's string |
| Generators without a height oracle | No install; log once at info level |

`provideChunk(x, z)` calls the delegate, then runs the **terrain phase** on the returned chunk. `populate(provider, x, z)` calls the delegate, then runs the **population phase**. Forge then runs the `IWorldGenerator`s, with GT's ore veins last. The overlay therefore sees all of RWG's terrain before writing, and all of RWG's decoration before populating, in the same order upstream 1.20 has over vanilla.

The hook needs no mixin. The footprint rule's write guard (§3a) and the sky light deferral (§2 step 5) are the only ones the overlay has; the compat mixins of [0006](0006-build-around-other-worldgen.md) act on other mods' generators. A second fallback, if the prototype finds a chunk generated before installation, is one `@Inject` at the return of `WorldServer.createChunkProvider`, with a comment saying why no event works.

### 2. Terrain phase

Runs only for chunks the layout marks as city, street, blend (within two chunks of a city), scattered-structure footprint ([07-scattered](../new-spec/07-scattered/index.md#scattered-structures), M07) or, later, highway and railway chunks. Every other chunk passes through untouched. A **landmark core** ([0006](0006-build-around-other-worldgen.md) §1), the chunks another mod's structure is predicted to occupy, is none of these: it passes through untouched, apart from a village lane's surface path ([0006](0006-build-around-other-worldgen.md) §3).

1. Ask the pure-Java layout (`layout`) what the chunk holds. The layout takes heights from the **height oracle** (§4), never from loaded chunks.
2. Write blocks through the chunk-buffer interface (decision log D6), backed by the chunk's `ExtendedBlockStorage` sections: create missing sections, and set block and metadata through the vanilla setters, which EndlessIDs patches.
3. City chunks: upstream's city chunk generation, including damage and debris.
4. Blend chunks: upstream's edge blending (§5).
5. Recompute the height map and sky light (`generateSkylightMap`). The provider defers RWG's own pass on the chunk it is generating (a head-cancel mixin on `Chunk.generateSkylightMap`, for that one chunk only) and lights every chunk once after this phase, written or not (decision log D-M05J-24).

Rules:

- **Edit the chunk in place.** Never replace the `Chunk` object, because EndlessIDs keeps its 16-bit biome array on it. Never change biomes.
- Place no tile entities here; they belong to the population phase.
- Read and write only this chunk.
- Allocate nothing per block.

### 3. Population phase

Runs after RWG's `populate` returns, for the chunk being populated:

- chest blocks with loot and spawners, with their tile entities;
- torch and other attachment fix-ups, and sapling stages (saplings are never grown at generation; they stay saplings and grow by random ticks, D31);
- any other step upstream does with world access.

It writes no light updates. In the pack, ArchaicFix's Phosphor backport (`enablePhosphor=true`) lights every light source in a chunk when vanilla's light population pass runs ([LightingHooks.java#L329-L369](https://github.com/embeddedt/ArchaicFix/blob/0.8.2/src/main/java/org/embeddedt/archaicfix/lighting/world/lighting/LightingHooks.java#L329-L369)), so lamps the terrain phase wrote are lit without a relight; the terrain phase keeps `generateSkylightMap`. This is a recorded dependency, checked by M04's scenario (D31).

Rules:

- Writes only into the populated chunk itself, which is always loaded. Never into its neighbors, so it never cascades, with vanilla's neighbor rule or with Hodgepodge's 3×3 rule.
- Uses its own `Random`, seeded from the world seed and the chunk coordinates. **Never draw from RWG's or an event's `Random`**: that would change RWG's decoration.
- RWG's decoration does not survive inside the city's footprint (§3a).
- When a population area touches a city chunk, the overlay vetoes RWG's `LAKE`, `LAVA` and `DUNGEON` steps through `PopulateChunkEvent.Populate` (RWG honors these), and NHCore's oil deposits through `CUSTOM`. Otherwise lakes and dungeons cut into cellars and break [0004](0004-gtnh-layer-and-tier-ceiling.md)'s "sound enough to move into". Upstream 1.12 had this as a profile option (`preventLakesRavinesInCities`); 1.20 has none, because its own feature step makes the question moot. It returns as a profile option, on by default, recorded as a deviation. The same vetoes apply when a population area touches a scattered structure's footprint ([07-scattered](../new-spec/07-scattered/index.md), D31). Landmark cores are not city chunks and get no veto. The treatment of every other pack feature is in [other-worldgen.md](../new-spec/00-pack-integration/other-worldgen.md).
- Villages and the other surface structures the layout can predict are not kept out of cities: the city builds around them ([0006](0006-build-around-other-worldgen.md)). The cave carver is not wrapped: RWG carves caves in `provideChunk`, before the terrain phase, as vanilla carves them before upstream's feature. Mineshafts and strongholds, which RWG writes in its `populate`, are the residual risk to cellars; the write guard (§3a) drops their writes inside the footprint (D31).

### 3a. RWG sets the landscape; the city is set into it

The owner's rule: RWG builds the landscape first, with its terrain and its decoration (trees, boulders, small ruins). The city is then set into that landscape.

- **Inside the footprint**, RWG's decoration is cleared. The footprint is every building's volume, every street, bridge, corridor and highway or railway cross-section, and their foundations. Nothing RWG placed survives there.
- **Outside the footprint**, RWG's decoration stays: parks, yards, the spaces between buildings and the blended city edges. That gives the lived-in, overgrown look.
- **Landmark cores** ([0006](0006-build-around-other-worldgen.md) §1) are outside the footprint. Neither the write guard nor the footprint restore acts in them, so RWG's decoration there stays, and other mods' structures (village pieces, which RWG's own `populate` writes, included) land on RWG's untouched ground. Edge-ring parks and soft-edge strips are outside the footprint too.
- City levels still follow upstream's stepped heights (§6), and edges blend into RWG's ground (§5).

**Mechanism.** A **write guard**: one mixin on the chunk's block setter (`Chunk.func_150807_a`) that drops writes inside footprint volumes. It keeps a stack of frames on the server thread: active around each delegate `populate`, inactive around our population phase, and switched off by `PopulateChunkEvent.Post` at lowest priority so RWG's snow and ice pass. `IWorldGenerator`s run after our wrapper returns; a third kind of frame, pushed by a generator registered at the lowest weight and popped by one at the highest, drops only tree parts there (logs and leaves, and blocks of the wood, leaves, plants or vine material), so IC2 rubber trees and HarvestCraft fruit trees planted on park grass stop at street and roof footprints while GT's ore veins and every other block pass (D75, decision log D-M05J-11). It is active during every delegate `populate`, wherever the write lands, because decoration spreads over four chunks and cascaded big trees reach chunk +2; the prototype measures that spill with ArchaicFix's `logCascadingWorldgen`. The layout answers "is this block in a footprint" from pure data. It is precise, cheap, and the mixin comment says why no event works: RWG ignores `DecorateBiomeEvent` results. The footprint is every city-written column from its lowest block up to Y 255; parks, yards, edge rings and soft-edge strips are outside it (D31).

The **fallback**, only if the prototype shows the guard cannot tell RWG's writes apart, is a footprint restore: a pass after decoration that rewrites the city's blocks inside the footprint from the layout. No mixin, but it costs more. It is the same machinery as the GT boulder restore of [new-spec 00-pack-integration/ore-veins.md](../new-spec/00-pack-integration/ore-veins.md), should that become necessary.

Revisit after the alpha and beta reviews: if cities feel too synthetic, the owner may let the city follow the terrain more closely.

### 4. Height oracle

`compat.rwg2` and `compat.rwg` each provide a `TerrainHeightOracle` that returns the base terrain height of a column (the stone surface before caves, map features and landmarks) without generating or loading a chunk:

| Generator | Oracle |
|---|---|
| RWG2 / RWG2 Continents | A private `ChunkGeneratorRealistic(world's ChunkManagerRealistic, seed, continental)`, built after RWG2 has built the world's own generator (so the per-world noise choice is loaded). `continental` comes from the world type name. Calls `getNewNoise(…, stride = 8)` |
| Classic RWG | A private second `ChunkGeneratorRealistic(world, seed)`. The constructor fires `InitMapGenEvent` four times; the prototype checks this has no effect on the world's own generator |
| Flat (`flat`) | The flat preset's top layer, a constant |
| PersonalSpace (post-beta) | Its configured layer height, a constant |

- The oracle is never the live generator.
- Heights follow upstream: the chunk height is the oracle height at block (8, 8), sampled once per `heightSampleSize`-chunk group (upstream default 3). Results are cached per world in a bounded cache.
- For the chunk being generated, the terrain phase may read the real chunk's column heights where upstream reads the chunk being generated (blending). City levels come only from the oracle, so neighbors always agree.

### 5. Terrain blend (Q2)

Upstream 1.20's `correctTerrainShape` is ported unchanged in behavior, apart from landmark cores and the corner test below:

- the L1 and L2 desired-height meshes from the nearest city corners (offsets 20 and 25 blocks, plus `terrainFixLower/UpperMin/MaxOffset`);
- bilinear interpolation with transition noise;
- columns above the upper mesh are lowered, keeping six surface layers;
- columns below the lower mesh are raised.

**Landmark cores** ([0006](0006-build-around-other-worldgen.md) §4, rule E1) are excluded from the blend: a core chunk is never reshaped, and a mesh corner shared by a core chunk and a non-city chunk is fixed at the core's natural height from the height oracle (offsets −1 to +1), as a city corner is fixed at the city ground. The blend between a city and a nearby landmark is spread over the chunks between them.

It runs in the terrain phase on RWG's finished chunk. RWG has already painted its surface, so lowered and raised columns keep RWG's grass, sand or snow. Caves are already carved, and raised columns carry their voids along, as upstream's do. The corner test: the L1 mesh's `h < 256` "corner exists" test becomes an explicit flag, because 256 is the 1.7.10 build limit.

### 6. Height mapping (Q2)

RWG's sea level is 63, the same as the 1.20 overworld's. So upstream's overworld numbers carry over unchanged. Only the build range and the options that could leave it change:

| Number | Upstream 1.20 | Here | Why |
|---|---|---|---|
| Build range | Y −64 to 319 | Y 0 to 255 | 1.7.10 |
| Sea level | 63 | 63 | RWG fills water below 63 |
| `groundLevel` | 71 | 71 | Relative to sea level, unchanged |
| `cityLevel0Height` to `cityLevel7Height` | 75 to 131, step 8 | same | City ground = 71 + 6 × level: 71 to 119 |
| `cityMinHeight` / `cityMaxHeight` | 50 / 150 | same | 150 keeps cities off RWG2 mountain chains (base 120) and high mountains, as upstream keeps them off peaks |
| `FLOORHEIGHT` | 6 | 6 | |
| `buildingMaxFloors` | 8 (allowed 0 to 60) | 8 (allowed 0 to 22) | Upstream's per-chunk cap (`cityGround + 6 × floors < maxBuild − 8`, so below Y 248) drops floors one at a time, and it keeps everything below Y 255: at city level 8 (ground Y 119) a building gets at most 21 floors and its roof starts at Y 245; at level 7 and below all 22 fit. Nothing passes Y 254 ([height-mapping.md](../new-spec/03-terrain/height-mapping.md#building-envelope)) |
| `bedrockLayer` | 1 at Y −64 | read, writes nothing; a warning when above 0 | RWG already places bedrock at Y 0 to 4, and generation never places unbreakable blocks ([0004](0004-gtnh-layer-and-tier-ceiling.md), [block-mapping.md](../new-spec/00-pack-integration/block-mapping.md#blocks-upstreams-code-names)). The key stays in the profile format so upstream profiles load |
| `terrainFix*Offset`, L1/L2 offsets | −4/−3, −1/+1; 20/25 | same | |
| `heightSampleSize` | 3 | 3, a pinned profile value (D73) | Revisited from the prototype's histogram |
| `seaLevel` override | −1 = world default | −1 → 63 | |
| Any option range above 255 or below 0 | up to 384 or 2048 | clamped to 0 to 255 at load, with a warning | |

The full per-option list is in the `03-terrain` new-spec area's [height-mapping.md](../new-spec/03-terrain/height-mapping.md).

### 7. Profile selection (Q7)

There is no world-preset screen. The profile is server configuration, pinned into the world when the world is created.

| Piece | Rule |
|---|---|
| Config (GTNHLib, server side, `config/lostcitiesnh.cfg`) | Category `profiles`: `dimensionProfiles = ["0=default"]` (`<dimension id>=<profile>`); `enableOnExistingWorlds = false` (D73) |
| Profiles | Built-in profiles from the jar. Custom profiles are JSON files in upstream's format in `config/lostcitiesnh/profiles/`. Only profiles whose landscape is an overlay (upstream's `default`-type profiles) are valid on RWG; others are rejected at load with a message |
| Pinning | When the wrapper is installed on a world that is not initialized (a new world), the profile is resolved and a snapshot is written to the overworld's saved data (`data/lostcitiesnh.dat`): profile name, every resolved value, the asset-set hash, the layout version and the world type. From then on the world uses the snapshot, and config edits do not change it. Classic RWG uses the same pattern for its per-world settings |
| Existing worlds | No snapshot means no cities. The server owner opts a world in per world with the operator command `/lostcities profile pin <profile> confirm` ([commands.md](../new-spec/01-core/commands.md)), which takes the snapshot then. `enableOnExistingWorlds = true` opts in every world without a snapshot at its next load. Either way, the border between old and new chunks is the owner's concern ([0003](0003-world-presence.md)) |
| World type check | Installed only for world types with an oracle: `RWG2_CONTINENTS`, `RWG2`, `RWG`, `flat`. Others are skipped with a log line |
| Changing a pinned world | `/lostcities profile` shows the pinned profile; `/lostcities profile pin <profile> confirm` pins one. Re-pinning needs an explicit confirm argument and logs that already-generated chunks will not match |
| Client | The mod is required on both sides, with FML's default version check: command output uses lang keys the client resolves, and the pack installs every mod on both sides. No client feature depends on the profile (upstream's horizon option needs a world type of its own and is dropped), so there is no sync packet. Single player reads the same config file. A create-world screen button is a later client-UI item, not alpha |
| Tuning | Owners copy a built-in profile to the profiles folder, edit it, and name it in `dimensionProfiles` before creating the world |

### 8. Time budget and how it is measured (Q10)

**Instrumentation.** The wrapper keeps fixed-bucket histograms, with no allocation per sample, of `System.nanoTime` deltas for:

- the delegate's `provideChunk`;
- the terrain phase;
- the delegate's `populate`;
- the population phase;
- oracle calls.

Each is split by chunk class: city, blend or untouched. An operator command and a server-stop log line print p50, p95, p99, p99.9 and max per series. Timing is measurement only. It never changes what is generated (no "skip detail when slow"), because that would break determinism.

**Method**, on the build server, never a laptop:

1. Dedicated GTNH 2.9.0 server with the real pack and its start flags, with ArchaicFix `logCascadingWorldgen = true` and GC logging on.
2. Three fixed seeds, each on `RWG2_CONTINENTS` and on `RWG`.
3. A scenario command generates a 64 × 64 chunk square in spiral order from spawn, one chunk at a time, with no players. This keeps Hodgepodge's player throttle out of the numbers. The first 512 chunks are discarded as JIT warm-up.
4. **Baseline run** with the overlay disabled by config. It gives RWG's own `provideChunk` and `populate` distributions from the same timers.
5. **Overlay run** with the default profile.
6. One run of each under Java Flight Recorder, to attribute time to `rwg2.*`, `gregtech.*` and our package.
7. In single player, Hodgepodge's `showChunkGenDebug` averages as a cross-check.

**Budget**, as first set; the M04 measurement below holds every one:

| ID | Budget |
|---|---|
| B1 | Terrain phase on city chunks: p95 ≤ RWG's own `provideChunk` p50 in the same run |
| B2 | Population phase on city chunks: p95 ≤ 0.5 × RWG's `populate` p50 |
| B3 | Over the whole 64 × 64 region, all overlay work ≤ 15 % of RWG's total generation time |
| B4 | No single chunk's overlay work above 10 ms at p99.9: half of Hodgepodge's 20 ms per-tick generation budget |
| B5 | Oracle cache and layout caches bounded to 16 MB per world, freed on unload |

The headless worldgen scenario (decision log D7) fails when B1 to B4 are exceeded on the build server: `overlay.budget` ([docs/testing/](../testing/README.md)).

**Measured in M04** (D81): the real GTNH 2.9.0 server on the build server, seed 4242, the synthetic test city, `overlay.budget`: 65 × 65 chunks in spiral order starting 20 chunks beside a 13 × 13-chunk city, the first 512 chunks dropped, no player. RWG's numbers are the same run's delegate timers. Times in milliseconds.

| | RWG2 Continents | Classic RWG |
|---|---|---|
| RWG `provideChunk`, p50 (all chunks) | 2.10 | 1.84 |
| RWG `populate`, p50 (all chunks) | 2.88 | 3.93 |
| B1: terrain phase on city chunks, p95 | **1.18** (≤ 2.10) | **1.44** (≤ 1.84) |
| B2: population phase on city chunks, p95 | **0.023** (≤ 1.44) | **0.020** (≤ 1.97) |
| B3: overlay share of RWG's total over the region | **1.7 %** (330 of 19,165) | **2.6 %** (577 of 22,456) |
| B4: worst phase, p99.9 | **1.70** (≤ 10) | **3.15** (≤ 10) |
| B5: plan and height caches, bound | **4.1 MB** (≤ 16) | **4.1 MB** (≤ 16) |
| Oracle call, p50 / p99.9 (inside the terrain phase) | 0.33 / 1.66 | 0.79 / 1.39 (classic RWG computes all 256 columns) |

Cold, in the `overlay.city` scenario before JIT warm-up, the terrain phase's p99.9 over 1,269 chunks was 11.5 ms on RWG2 Continents: the first city chunks of a server's life, which B4's warm-up rule excludes. JFR, three seeds and Hodgepodge's `showChunkGenDebug` cross-check remain for the alpha gate.

## Consequences

- **M04 scope:**
  - the wrapper and its installation;
  - the chunk buffer over `ExtendedBlockStorage`;
  - the two oracles in `compat.rwg2` and `compat.rwg`;
  - the blend port with the sentinel fix;
  - the population phase;
  - the `Populate` vetoes;
  - the timers and the budget check;
  - profile pinning in saved data.
- **Dependencies:** RWG2 2.0.6 and RWG alpha-1.5.3 as `compileOnly`, used only under `compat`, guarded by `Loader.isModLoaded("rwg2")` / `("RWG")`. `usesMixins` is on for the footprint write guard (§3a), through UniMixins in the GTNH style.
- **Determinism tests:**
  - Golden-chunk hashes are taken after the terrain phase, and separately after our population phase, never after the whole of population. Forge runs `IWorldGenerator`s of equal weight in an order that can change between JVM runs, and GT places ore veins from a queue, sometimes during a later chunk's population.
  - Golden hashes hold only for a given pack and config, because RWG2's terrain depends on its config (continent widths, placement offsets) and on installed biome mods.
- **Spec deviations** (to record in `02-profiles-dimensions` and `03-terrain`):
  - there is no world-preset screen, and profiles are pinned per world;
  - non-overlay landscape types (floating, space, cavern, spheres) are unavailable on RWG;
  - the build range is 0 to 255;
  - the client horizon option is dropped;
  - RWG decoration is kept out of building, street and route footprints (§3a) and stays everywhere else;
  - lakes, lava lakes and dungeons are kept out of city chunks by a profile option (from upstream 1.12), on by default.
- **Testing:** the cascading-worldgen warning comes from ArchaicFix, which the pack ships switched off. The worldgen scenario enables it in its own config ([CONTRIBUTING § World generation rules](../../CONTRIBUTING.md#world-generation-rules)).
- **Pack changes:** none.
- **PersonalSpace stays open:** the same wrapper installs on any dimension the config lists, given a constant-height oracle.

## Alternatives considered

| Alternative | Why not |
|---|---|
| Write the city in `ChunkProviderEvent.ReplaceBiomeBlocks` | Fires before RWG paints the surface, carves caves and adds landmarks, so the city would be repainted and carved. `DENY` also removes bedrock. Not fired by PersonalSpace or flat worlds |
| Write the whole city in population (`PopulateChunkEvent.Post` or an `IWorldGenerator`) | World `setBlock` with lighting for about 25,000 blocks per city chunk is far too slow, and writing the +8 area spreads each chunk over four. `Post` also comes before RWG's snow. Kept only for the population phase's world work: tile entities, torches and the command-block tick |
| Mixins into RWG2's and RWG's `provideChunk` and `populate` | Two GPL classes that change between releases, and nothing for flat or PersonalSpace. Not needed, since the wrapper sits at the same points |
| Mixins into `ChunkProviderServer.originalLoadChunk` / `populate` | Hodgepodge, ArchaicFix and Variable-Horizons redirect calls in exactly these methods. Injections there are brittle and order-dependent |
| Replace the overworld `WorldProvider` | Clashes with any other mod doing the same, and cannot reach PersonalSpace |
| An adapter world type, as upstream 1.12 | Ruled out by [0003](0003-world-presence.md), and it needed RWG's cooperation |
| Heights from loaded neighbor chunks | Neighbors are often not generated yet. Generating them cascades; waiting for them makes layout depend on generation order |
| Calling `getNewNoise` on RWG's live generator | Corrupts the next chunk's map features |
| Letting RWG decoration run over the whole city, as vanilla decoration does over upstream 1.20 cities | Trees and boulders on streets and roofs. The owner's rule (§3a) keeps decoration outside the footprint only |
| A per-world config folder or a client world-creation screen for profiles | 1.7.10 has no per-world config. A client screen is single player only. Both are possible later, on top of the saved-data pin |

## Still open

For the throwaway prototype on the build server (S2), which overlays a flat 3 × 3 test city with simple blocks:

1. **Install timing.** On a new world, the wrapper counts chunks generated before installation; the expected count is zero. *M04: zero on both world types (`overlay.installed`).* Repeat on an existing world with a chunk-loader ticket, to see whether the `createChunkProvider` mixin fallback is needed.
2. **Oracle accuracy.** Compare the oracle height with the generated chunk's real height at (8, 8) over 4,096 chunks per generator, and report the error distribution. Check that the second classic-RWG instance has no visible side effects (`InitMapGenEvent` listeners, RWG's static noise flag and canyon colors).
3. **Height histogram** for RWG2 Continents and classic RWG over the same region. It decides whether `heightSampleSize = 3` and the upstream level thresholds give sensible city levels on RWG, or whether an RWG-tuned profile is needed.
4. **The footprint rule (§3a)**: implement the write guard over every delegate `populate`, fall back to the footprint restore only if the guard can't tell RWG's writes apart, measure the spill with `logCascadingWorldgen`, and measure the cost against the budget. Screenshots of city edges and interiors:
   - RWG trees in parks and yards, and none on streets, roofs or inside buildings; boulders and small ruins likewise; landmarks;
   - lakes and dungeons in cellars before and after the vetoes;
   - NHCore oil deposits, Railcraft geodes and quarries;
   - landmark cores left untouched, with RWG's decoration and the village's own pieces ([0006](0006-build-around-other-worldgen.md));
   - snow on streets;
   - lamps and glowstone in a fresh city at night, lit by ArchaicFix's Phosphor without a relight (§3).

   *M04, with the synthetic test city on seed 4242:* the guard works on both world types (17,651 and 40,231 writes dropped in the 17 × 17-chunk test region; no log or leaf passes it during a population), every glowstone is lit after the chunk's light pass on both, and landmark cores without a lane are left exactly as RWG made them. Other mods' trees from `IWorldGenerator`s overhung footprints (D75). *M05, real cities on RWG2 Continents, seed 4242:* with the tree frames (D-M05J-11), no log or leaf from any decoration is left in a footprint of the 17 × 17-chunk `city.real` region. The real-pack screenshots in the M04 report show the city from above, its edge and a village core with its lane; the vetoes, oil, geodes and night shots are left to the alpha set.

5. **Budget numbers** per §8 (B1 to B5), written back here. *M04: written in §8; all met.*
6. **Cascades:** zero ArchaicFix warnings attributed to this mod, and Hodgepodge's neighbor-fill count unchanged against the baseline. *M04: with `logCascadingWorldgenStacktrace` on, every warning (55 on RWG2 Continents, 63 on classic RWG, against 49 and 56 for the bare pack on the same seed during the spawn search) loads its chunk from HarvestCraft fruit trees, Forestry and MagicBees hives, ExtraBees water hives, Biomes O' Plenty reeds in RWG's decoration, NewHorizonsCoreMod's oil or RWG itself; the overlay appears only as the frame that passes RWG's `populate` through. The neighbor-fill count is not measured.*
7. **EndlessIDs:** blocks with IDs above 4095 and metadata written through `ExtendedBlockStorage` render, save and reload correctly.
8. **Wrapper compatibility:**
   - the F3 provider name;
   - stronghold search (Eye of Ender);
   - `recreateStructures` after a reload;
   - WorldEdit `//regen`;
   - RWG2's `/rwg_noise` command.

Settled elsewhere: ore-vein coexistence (Q3), which pack features to veto or remove in cities (Q8), and spawn handling (Q9), all by S3; building around other mods' structures by [0006](0006-build-around-other-worldgen.md).
