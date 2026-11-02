# RWG2 Continents and classic RWG: how a chunk is made, and where cities hook in

Research sheet for spike S2 (open questions Q1, Q2, Q7, Q10). It records what the GTNH 2.9.0 overworld generators do, read from source at the tags the pack ships, and the candidate hook points for the city overlay. The decision built on it is [0005](../../design/0005-rwg-overlay.md) (*Accepted*).

Every fact below either cites a pinned permalink or is marked *unverified*. Vanilla 1.7.10 code has no public pinned source; facts that rest on it are marked *vanilla*.

## Versions and sources

| Mod | 2.9.0 version | Mod ID | Source at that version | License |
|---|---|---|---|---|
| Realistic World Gen 2 | 2.0.6 | `rwg2` | [GTNewHorizons/Realistic-World-Gen-2 @ 2.0.6](https://github.com/GTNewHorizons/Realistic-World-Gen-2/tree/2.0.6) (commit `497fc1f4`) | GPL-3.0 |
| Realistic World Gen (classic) | alpha-1.5.3 | `RWG` | [GTNewHorizons/Realistic-World-Gen @ alpha-1.5.3](https://github.com/GTNewHorizons/Realistic-World-Gen/tree/alpha-1.5.3) (commit `90d0d6cf`) | GPL-3.0 |
| Hodgepodge | 2.7.221 | `hodgepodge` | [GTNewHorizons/Hodgepodge @ 2.7.221](https://github.com/GTNewHorizons/Hodgepodge/tree/2.7.221) | LGPL-3.0 |
| GT5-Unofficial | 5.09.54.265 | `gregtech` | [GTNewHorizons/GT5-Unofficial @ 5.09.54.265](https://github.com/GTNewHorizons/GT5-Unofficial/tree/5.09.54.265) | LGPL-3.0 |
| NewHorizonsCoreMod | 2.9.99 | `dreamcraft` | [GTNewHorizons/NewHorizonsCoreMod @ 2.9.99](https://github.com/GTNewHorizons/NewHorizonsCoreMod/tree/2.9.99) | GPL-3.0 |
| PersonalSpace | 1.0.41 | `personalspace` | [GTNewHorizons/PersonalSpace @ 1.0.41](https://github.com/GTNewHorizons/PersonalSpace/tree/1.0.41) | LGPL-3.0 |
| ArchaicFix | 0.8.2 | `archaicfix` | Pack takes the CurseForge jar; source assumed to be [embeddedt/ArchaicFix @ 0.8.2](https://github.com/embeddedt/ArchaicFix/tree/0.8.2) | LGPL-3.0 |
| EndlessIDs | 1.7.4 | `endlessids` | Pack takes the CurseForge jar; not read for this sheet | GPL / LGPL |
| Forge | 10.13.4.1614 | | [MinecraftForge `1.7.10` branch head `9274e4fe`](https://github.com/MinecraftForge/MinecraftForge/tree/9274e4fe435cb415099a8216c1b42235f185443e). Forge publishes no 1.7.10 tags on GitHub; the branch head is a later log4j-config fix, so the files cited here are assumed identical to 1614 (*unverified*) | LGPL-2.1 |

Both RWG mods are GPL. This mod calls their public classes and reads their constants (integration). It copies none of their code.

## Pack configuration that matters

From the pack's 2.9.0 `config/` folder. The pack ships no `rwg2.cfg`, so RWG2 runs on the defaults in its own config class; those rows give the mod's defaults ([ConfigRWG.java#L23-L45](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/config/ConfigRWG.java#L23-L45), read from `config/rwg2.cfg`: [RWG2MixinPlugin.java#L34](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/RWG2MixinPlugin.java#L34)).

| File | Setting | Value | Effect on the overlay |
|---|---|---|---|
| `defaultworldgenerator.cfg` | `World Generator` | `RWG2_CONTINENTS` | Default world type on the **client's** create-world screen. DefaultWorldGenerator is a client-side mod (manifest: `side: CLIENT`), so a dedicated server takes `level-type` from `server.properties`. *Verified:* the GTNH 2.9.0 server pack ships `level-type=rwg`, and vanilla `WorldType.parseWorldType` matches names case-insensitively, so a dedicated server's new world is **classic RWG** (`RWG`, [RWG.java#L32](https://github.com/GTNewHorizons/Realistic-World-Gen/blob/alpha-1.5.3/src/main/java/rwg/RWG.java#L32)), not RWG2 Continents; a fresh boot writes `generatorName=RWG` to `level.dat`. `level-type=RWG2_CONTINENTS` gives RWG2 Continents ([RWG.java#L39-L40](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/RWG.java#L39-L40)). So single-player worlds default to RWG2 Continents and server worlds to classic RWG, and tests cover both |
| `defaultworldgenerator.cfg` | `World Generator Whitelist` | `flat`, `RWG`, `RWG2_CONTINENTS` | Plain `RWG2` (non-continental) is registered but hidden from the screen |
| `rwg2.cfg` (not shipped: mod default) | `Generate Caves / Mineshafts / Villages / Underground Lakes / Underground Lava Lakes` | all `true` ([#L99-L103](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/config/ConfigRWG.java#L99-L103)) | Caves are carved, and villages, lakes and dungeons are placed, in city chunks unless the overlay intervenes |
| `rwg2.cfg` (not shipped: mod default) | `Minimum` and `Maximum Continent Width` | `2700` ([#L112-L125](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/config/ConfigRWG.java#L112-L125)) | Continents about 2700 blocks wide; average ocean width 100 |
| `rwg2.cfg` (not shipped: mod default) | `Biome` and `Landmass Placement Offset X/Z` | `0` ([#L205-L234](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/config/ConfigRWG.java#L205-L234)) | Terrain is a function of seed **and** these offsets |
| `RWG.cfg` | `Generate Villages`, `Generate Caves` | `true` | Same as RWG2 |
| `RWG.cfg` | `Use fixed biome lists for new worlds` | `true` | Stored per world in RWG's saved data (see Q7 precedent) |
| `hodgepodge.cfg` | `throttleChunkGeneration` | `true` | Chunk generation is spread across ticks under a time budget (see [Hodgepodge](#hodgepodge-throttled-generation-and-deferred-population)) |
| `hodgepodge.cfg` | `chunkGenBudgetMs`, `maxChunkGenPerPlayerPerTick` | `20`, `4` | 20 ms of generation per tick, at most 4 chunks per player per tick |
| `hodgepodge.cfg` | `amortizeChunkGenOverruns` | `DedicatedServerOnly` | An overrun becomes debt that later ticks pay back |
| `archaicfix.cfg` | `logCascadingWorldgen` | `false` (mod default `true`) | The pack silences the cascading-worldgen warning. Our worldgen scenario must turn it on |
| `archaicfix.cfg` | `fixVanillaCascadingWorldgen` | `true` | Changes some vanilla population offsets ("turn off for 100% seed parity") |
| `etfuturum/world.cfg` | `deepslateGenerationMode = 0`, layer dimension list `0` as whitelist, `deepslateMaxY = 22` | | Stone below about Y 22 in the overworld becomes deepslate during population |

## RWG2 (2.0.6): one overworld chunk

### World type and providers

- `RWG2` and `RWG2_CONTINENTS` are two instances of one `WorldType` subclass with a `continents` flag. [RWG.java#L39-L40](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/RWG.java#L39-L40)
- The world type returns `ChunkManagerRealistic` as the biome provider and `ChunkGeneratorRealistic` as the `IChunkProvider`. [WorldTypeRealistic.java#L17-L23](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/world/WorldTypeRealistic.java#L17-L23)
- RWG2 requires EndlessIDs (`dependencies = "required-after:endlessids"`). [RWG.java#L28-L33](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/RWG.java#L28-L33)
- Biomes are written as unsigned 16-bit IDs through EndlessIDs' `ChunkBiomeHook`, not the vanilla byte array. [ChunkBiomeArrayCompat.java#L13-L16](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/world/ChunkBiomeArrayCompat.java#L13-L16)

### `provideChunk`, in order

[ChunkGeneratorRealistic.java#L207-L265](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/world/ChunkGeneratorRealistic.java#L207-L265). The method is `synchronized`.

1. Seeds the chunk `Random` from the chunk coordinates.
2. **Heights.** `getNewNoise` computes one float height per column. It blends weighted biome terrain functions (a parabolic 17×17 sample of biomes every 8 blocks), then applies volcano and lava-cave landmarks, then carves rivers. [#L306-L561](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/world/ChunkGeneratorRealistic.java#L306-L561)
3. **Raw fill.** Stone up to the height; water above it for `y < 63`; air elsewhere. **Sea level is 63: the top water block is Y 62.** [#L282-L304](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/world/ChunkGeneratorRealistic.java#L282-L304)
4. Mountain-chain river tunnels and junction chambers are carved. [#L215](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/world/ChunkGeneratorRealistic.java#L215)
5. Per-biome map features (`generateMapGen`) for biomes weighted at the chunk center. [#L217-L234](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/world/ChunkGeneratorRealistic.java#L217-L234)
6. **`ChunkProviderEvent.ReplaceBiomeBlocks`** is posted on `EVENT_BUS` with the raw `Block[65536]`/`byte[65536]` arrays. A `DENY` result skips everything after it in this method, **including the bedrock floor**. [#L675-L686](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/world/ChunkGeneratorRealistic.java#L675-L686)
7. Surface painting per biome (`rReplace`), river biomes, then bedrock at Y 0 to 4. [#L688-L797](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/world/ChunkGeneratorRealistic.java#L688-L797)
8. Caves (`MapGenCaves`, if enabled). [#L237-L239](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/world/ChunkGeneratorRealistic.java#L237-L239)
9. Mineshaft, stronghold and village **starts** are computed (no blocks yet). RWG2 builds its village generator with `distance 24`, `size 0`, but in the pack Biomes O' Plenty replaces it on `InitMapGenEvent` and VillageNames then replaces that one at low priority, so villages follow VillageNames' 50-chunk grid ([coexistence.md § How a VillageNames village is placed](coexistence.md#how-a-villagenames-village-is-placed)). [#L157-L167](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/world/ChunkGeneratorRealistic.java#L157-L167), [#L241-L247](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/world/ChunkGeneratorRealistic.java#L241-L247)
10. Continents mode only: volcano magma chambers and lava-cave landmarks. [#L248-L258](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/world/ChunkGeneratorRealistic.java#L248-L258)
11. `new Chunk(world, blocks, meta, x, z)`, the biome array, `generateSkylightMap()`. [#L260-L264](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/world/ChunkGeneratorRealistic.java#L260-L264)

There are **no ravines** (no `MapGenRavine`). RWG2 does not call `TerrainGen.getModdedNoiseGenerators`, so **`InitNoiseGensEvent` is never fired**. It does fire **`InitMapGenEvent`** for `CAVE`, `VILLAGE`, `STRONGHOLD` and `MINESHAFT` while the generator is constructed. [#L142-L167](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/world/ChunkGeneratorRealistic.java#L142-L167)

### A world-less height sampler exists

- `ChunkManagerRealistic(long seed, boolean continental)` builds a biome provider with no `World`. [ChunkManagerRealistic.java#L128-L135](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/world/ChunkManagerRealistic.java#L128-L135)
- `ChunkGeneratorRealistic(ChunkManagerRealistic, long seed, boolean continental)` builds a generator with `mapFeatures = false` (no caves, structures or decorations). [#L136-L138](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/world/ChunkGeneratorRealistic.java#L136-L138)
- `getNewNoise(manager, blockX, blockZ, biomes, stride)` returns the 16×16 heights of a chunk. It evaluates every `stride`-th column, and its Javadoc says the sampled columns match dense generation exactly. [#L310-L319](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/world/ChunkGeneratorRealistic.java#L310-L319)
- RWG2's own preview tool uses exactly this pair to draw height maps. [RWGPreviewTool.java#L107-L114](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/preview/java/rwg2/world/RWGPreviewTool.java#L107-L114)
- **Never call `getNewNoise` on the live generator.** It records center-chunk biome weights in `mapGenBiomes` ([#L480-L500](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/world/ChunkGeneratorRealistic.java#L480-L500)), and the next `provideChunk` consumes and clears them ([#L217-L232](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/world/ChunkGeneratorRealistic.java#L217-L232)). A stray call changes the next chunk's map features.
- The noise implementation is a per-world choice read from RWG2's saved data when a generator is built. [NoiseSelector.java](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/util/NoiseSelector.java) So a private sampler must be built after RWG2 has built the world's own generator, for the same world and seed.
- The sampled height includes river carving but **not** steps 4, 5 and 8 to 10 above (river tunnels, biome map features, caves, magma chambers, lava caves). That is the same contract as upstream 1.20's base height, which is noise only.

### `populate`, in order

[ChunkGeneratorRealistic.java#L811-L1125](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/world/ChunkGeneratorRealistic.java#L811-L1125). Decoration uses the vanilla +8 offset: 655 `nextInt(16) + 8` sites in the biome classes and none without the offset (counted with grep at the tag).

| # | Step | Forge event | Does RWG2 honor a `DENY`? |
|---|---|---|---|
| 1 | `BlockFalling.fallInstantly = true`; seed from world seed and chunk | | |
| 2 | | `PopulateChunkEvent.Pre` (`EVENT_BUS`) [#L822](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/world/ChunkGeneratorRealistic.java#L822) | not cancelable |
| 3 | Mineshaft, stronghold, village blocks | | n/a |
| 4 | Underground water lakes (1 in 10, Y < 50) | `Populate LAKE` [#L835](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/world/ChunkGeneratorRealistic.java#L835) | **yes** |
| 5 | Lava lakes | `Populate LAVA` | **yes** |
| 6 | Dungeons (8 tries, Y < 128) | `Populate DUNGEON` [#L860](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/world/ChunkGeneratorRealistic.java#L860) | **yes** |
| 7 | Clay, dirt, gravel, vanilla ores, emeralds | `OreGenEvent.Pre/Post` and `GenerateMinable` (`ORE_GEN_BUS`) [#L868-L964](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/world/ChunkGeneratorRealistic.java#L868-L964) | yes, per ore type |
| 8 | Mushrooms | | n/a |
| 9 | Biome decoration: trees, boulders, small ruins, grass, flowers, cacti and so on (`rDecorate`) | `DecorateBiomeEvent.Pre`, `Decorate` × 14, `Post` [#L984-L1038](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/world/ChunkGeneratorRealistic.java#L984-L1038) | **no: `TerrainGen.decorate` results are ignored**, so trees cannot be vetoed by event |
| 10 | Et Futurum cave vines; continents: lava-cave surface | | n/a |
| 11 | 50 water springs, 20 lava springs | | n/a |
| 12 | Landmark decorations | [#L1058-L1060](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/world/ChunkGeneratorRealistic.java#L1058-L1060) | n/a |
| 13 | Animal spawning | `Populate ANIMALS` | yes |
| 14 | | `PopulateChunkEvent.Post` [#L1073](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/world/ChunkGeneratorRealistic.java#L1073) | not cancelable |
| 15 | Ice and snow, **after** `Post` | `Populate ICE` [#L1075](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/world/ChunkGeneratorRealistic.java#L1075) | **no**, result ignored |
| 16 | Deferred ocean decorations; `fallInstantly = false` | | n/a |

Consequences:

- `PopulateChunkEvent.Post` is **not** the end of RWG2's population: snow, ice and ocean decorations follow it.
- Boulders (`DecoBlob`) land on grass, dirt, stone, gravel or sand. [DecoBlob.java#L33-L38](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/deco/DecoBlob.java#L33-L38) Some street and park blocks qualify.
- RWG2 also places small ruins as decoration (`deco/ruins/DecoRuinsAdventure`, `DecoRuinsAncient`).
- `event.rand` in RWG2's events is RWG2's own population `Random`. A listener that draws from it changes every decoration after it.

### Terrain heights (Q2)

Derived from the terrain functions at the tag; **not measured**. The prototype measures a histogram (see [Measuring](#measuring-generation-cost-q10)).

| Terrain | Base and shape | Typical surface Y |
|---|---|---|
| Ocean (shallow, deep) | 52 or 34, ± about 5.5 [RealisticBiomeOcean.java#L55-L58](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/biomes/realistic/ocean/RealisticBiomeOcean.java#L55-L58). Ocean is deep where the continent field is below −300, more than 300 units out from the coast ([ChunkManagerRealistic.java#L34](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/world/ChunkManagerRealistic.java#L34), [#L682-L686](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/world/ChunkManagerRealistic.java#L682-L686)). The cold deep ocean reports vanilla Deep Ocean (ID 24); the snow, hot and wet deep oceans report RWG2's Cold, Hot and Wet Ocean ([Support.java#L160-L163](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/support/Support.java#L160-L163)) | Shallow floor 46.5 to 57.5, deep floor 28.5 to 39.5 |
| Flat lakes (taiga, tundra, snow plains) | 62 + noise capped at +3, lakes dip below sea level [TerrainFlatLakes.java#L10-L18](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/terrain/TerrainFlatLakes.java#L10-L16) | 55 to 70 |
| Grassland flats | 68 ± 9, plus squared hill noise, minus lakes [TerrainGrasslandFlats.java#L11-L28](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/terrain/TerrainGrasslandFlats.java#L11-L25) | mostly 60 to 90, hills to about 180 |
| Hilly (default base 68) | hills capped at 70 + (m − 70) / 2.5 [TerrainHilly.java#L31-L55](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/terrain/TerrainHilly.java#L31-L52) | 60 to about 170 |
| Mountain chain | `TerrainHilly` with base **120** [RealisticBiomeMountainChain.java#L26](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/biomes/realistic/land/RealisticBiomeMountainChain.java#L26) | 110 to above 200 |
| Mountain | 67 + up to 150 + detail [TerrainMountain.java#L11-L33](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/terrain/TerrainMountain.java#L11-L30) | 60 to about 250 |
| Highland (high rainforest) | 68 + up to 140 [RealisticBiomeHighRainforest.java#L39](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/biomes/realistic/land/RealisticBiomeHighRainforest.java#L39) | 60 to about 220 |
| River beds | carved toward Y 58 to 62 | 55 to 62 |

## Classic RWG (alpha-1.5.3): differences from RWG2

The pipeline is the same shape, with these differences:

| Topic | Classic RWG | Source |
|---|---|---|
| World type | One type, `RWG`, no continents | [RWG.java#L32](https://github.com/GTNewHorizons/Realistic-World-Gen/blob/alpha-1.5.3/src/main/java/rwg/RWG.java#L32) |
| Generator constructors | **Only** `ChunkGeneratorRealistic(World, long)`, which casts the world's chunk manager and fires `InitMapGenEvent` four times. No world-less sampler | [ChunkGeneratorRealistic.java#L107-L125](https://github.com/GTNewHorizons/Realistic-World-Gen/blob/alpha-1.5.3/src/main/java/rwg/world/ChunkGeneratorRealistic.java#L107-L125) |
| `provideChunk` | Not `synchronized`; no river tunnels, volcanoes or lava caves; biomes written to the vanilla byte array | [#L157-L205](https://github.com/GTNewHorizons/Realistic-World-Gen/blob/alpha-1.5.3/src/main/java/rwg/world/ChunkGeneratorRealistic.java#L157-L205) |
| Sea level | Also `y < 63` water | [#L208-L230](https://github.com/GTNewHorizons/Realistic-World-Gen/blob/alpha-1.5.3/src/main/java/rwg/world/ChunkGeneratorRealistic.java#L208-L230) |
| `ReplaceBiomeBlocks` | Same place, same `DENY` behavior | [#L411-L422](https://github.com/GTNewHorizons/Realistic-World-Gen/blob/alpha-1.5.3/src/main/java/rwg/world/ChunkGeneratorRealistic.java#L411-L422) |
| `getNewNoise` | Public, no stride; same `mapGenBiomes` side effect, so the live instance must not be used as a sampler | [#L232](https://github.com/GTNewHorizons/Realistic-World-Gen/blob/alpha-1.5.3/src/main/java/rwg/world/ChunkGeneratorRealistic.java#L232), [#L365-L381](https://github.com/GTNewHorizons/Realistic-World-Gen/blob/alpha-1.5.3/src/main/java/rwg/world/ChunkGeneratorRealistic.java#L365-L381) |
| `populate` | Same event order: `Pre` [#L485](https://github.com/GTNewHorizons/Realistic-World-Gen/blob/alpha-1.5.3/src/main/java/rwg/world/ChunkGeneratorRealistic.java#L485), decoration results ignored [#L643-L682](https://github.com/GTNewHorizons/Realistic-World-Gen/blob/alpha-1.5.3/src/main/java/rwg/world/ChunkGeneratorRealistic.java#L643-L682), `Post` [#L709](https://github.com/GTNewHorizons/Realistic-World-Gen/blob/alpha-1.5.3/src/main/java/rwg/world/ChunkGeneratorRealistic.java#L709), then ice and snow [#L711](https://github.com/GTNewHorizons/Realistic-World-Gen/blob/alpha-1.5.3/src/main/java/rwg/world/ChunkGeneratorRealistic.java#L711) | |
| Heights | Same terrain-function family. Land bases 60 to 74, coasts at 55. Ranges as in the RWG2 table, without continents or mountain chains. **Classic RWG has no oceans:** its biome choice ignores the ocean value it is given and draws only from its land, small-biome and climate lists; the test ocean (base about Y 45) is reached only from commented-out code, so it is dead code | terrain and biome classes at the tag; [ChunkManagerRealistic.java#L208-L280](https://github.com/GTNewHorizons/Realistic-World-Gen/blob/alpha-1.5.3/src/main/java/rwg/world/ChunkManagerRealistic.java#L208-L280) |
| Per-world state | Biome-list version and noise implementation are stored in world saved data. A new world is detected with `!world.getWorldInfo().isInitialized()` while the world is being built | [RwgWorldSavedData.java#L94-L126](https://github.com/GTNewHorizons/Realistic-World-Gen/blob/alpha-1.5.3/src/main/java/rwg/world/RwgWorldSavedData.java#L94-L126) |

## Forge 1.7.10 facts the overlay relies on

| Fact | Source |
|---|---|
| `ChunkProviderServer.currentChunkProvider` (`field_73246_d`) is made public by Forge's access transformer, as are `loadedChunkHashMap` and `worldObj` | [forge_at.cfg#L187-L191](https://github.com/MinecraftForge/MinecraftForge/blob/9274e4fe435cb415099a8216c1b42235f185443e/src/main/resources/forge_at.cfg#L187-L191) |
| `ChunkProviderServer.populate` calls `currentChunkProvider.populate(...)`, then `GameRegistry.generateWorld(x, z, world, currentChunkProvider, this)`. Mod generators receive the current provider as their `chunkGenerator` argument | [fml ChunkProviderServer.java.patch#L14-L15](https://github.com/MinecraftForge/MinecraftForge/blob/9274e4fe435cb415099a8216c1b42235f185443e/fml/patches/minecraft/net/minecraft/world/gen/ChunkProviderServer.java.patch#L14-L15) |
| A new chunk goes through `originalLoadChunk`: `currentChunkProvider.provideChunk`, then it joins the loaded map, then `onChunkLoad()` (posts `ChunkEvent.Load`), then `populateChunk` | [ChunkProviderServer.java.patch#L79-L109](https://github.com/MinecraftForge/MinecraftForge/blob/9274e4fe435cb415099a8216c1b42235f185443e/patches/minecraft/net/minecraft/world/gen/ChunkProviderServer.java.patch#L79-L109); `populateChunk`'s neighbor rule is *vanilla* |
| `IWorldGenerator`s run sorted by weight ascending. The set is a `HashSet` and the sort is stable, so **generators with equal weights run in hash order, which can differ between JVM runs**. Each generator gets a `Random` reseeded to the same per-chunk seed | [GameRegistry.java#L65-L66](https://github.com/MinecraftForge/MinecraftForge/blob/9274e4fe435cb415099a8216c1b42235f185443e/fml/src/main/java/cpw/mods/fml/common/registry/GameRegistry.java#L65-L66), [#L97-L127](https://github.com/MinecraftForge/MinecraftForge/blob/9274e4fe435cb415099a8216c1b42235f185443e/fml/src/main/java/cpw/mods/fml/common/registry/GameRegistry.java#L97-L127) |
| `TerrainGen.populate` and `decorate` post on `TERRAIN_GEN_BUS`; `generateOre` on `ORE_GEN_BUS`; `getModdedMapGen` posts `InitMapGenEvent` on `TERRAIN_GEN_BUS` | [TerrainGen.java#L26-L52](https://github.com/MinecraftForge/MinecraftForge/blob/9274e4fe435cb415099a8216c1b42235f185443e/src/main/java/net/minecraftforge/event/terraingen/TerrainGen.java#L26-L52) |
| The `World` constructor builds the chunk provider (`createChunkProvider`), then, for a world that is not yet initialized, calls `initialize`, which runs `createSpawnPosition` | [World.java.patch#L95-L106](https://github.com/MinecraftForge/MinecraftForge/blob/9274e4fe435cb415099a8216c1b42235f185443e/patches/minecraft/net/minecraft/world/World.java.patch#L95-L106) |
| `createSpawnPosition` posts the cancelable **`WorldEvent.CreateSpawnPosition`** before it searches for a spawn (the search generates chunks) | [WorldServer.java.patch#L186](https://github.com/MinecraftForge/MinecraftForge/blob/9274e4fe435cb415099a8216c1b42235f185443e/patches/minecraft/net/minecraft/world/WorldServer.java.patch#L186), [ForgeEventFactory.java#L256-L259](https://github.com/MinecraftForge/MinecraftForge/blob/9274e4fe435cb415099a8216c1b42235f185443e/src/main/java/net/minecraftforge/event/ForgeEventFactory.java#L256-L259) |
| `WorldEvent.Load` for the overworld is posted only after its constructor returns, so **after** the spawn search | [MinecraftServer.java.patch#L55-L97](https://github.com/MinecraftForge/MinecraftForge/blob/9274e4fe435cb415099a8216c1b42235f185443e/patches/minecraft/net/minecraft/server/MinecraftServer.java.patch#L55-L97). GT confirms it in a comment: "Chunks are populated before dim 0 fires WorldEvent.Load, by the overworld spawn search and by other dimensions reinstating chunkloader tickets" ([GTWorldgenerator.java#L161-L163](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/common/GTWorldgenerator.java#L161-L163)) |

## Hodgepodge: throttled generation and deferred population

With `throttleChunkGeneration` on (the pack default):

- Chunk generation for players is queued and spread across ticks under `chunkGenBudgetMs` (20 ms). [SpeedupsConfig.java#L116-L156](https://github.com/GTNewHorizons/Hodgepodge/blob/2.7.221/src/main/java/com/mitchej123/hodgepodge/config/SpeedupsConfig.java#L116-L156)
- It keeps an exponential moving average of terrain time and populate time per dimension (α = 0.15, both seeded at 150 ms) and skips work it predicts will not fit. [ChunkGenBudget.java](https://github.com/GTNewHorizons/Hodgepodge/blob/2.7.221/src/main/java/com/mitchej123/hodgepodge/mixins/hooks/ChunkGenBudget.java)
- **A chunk is populated only when all of its 3×3 neighbors have terrain**, by calling `ChunkProviderServer.populate` directly. [ChunkGenScheduler.java#L397-L458](https://github.com/GTNewHorizons/Hodgepodge/blob/2.7.221/src/main/java/com/mitchej123/hodgepodge/mixins/hooks/ChunkGenScheduler.java#L397-L458), [#L513](https://github.com/GTNewHorizons/Hodgepodge/blob/2.7.221/src/main/java/com/mitchej123/hodgepodge/mixins/hooks/ChunkGenScheduler.java#L513)
- After ticking starts, a population that would happen inside another population is deferred to the scheduler queue instead of running. [MixinChunkProviderServer_DeferPopulation.java](https://github.com/GTNewHorizons/Hodgepodge/blob/2.7.221/src/main/java/com/mitchej123/hodgepodge/mixins/early/minecraft/fastload/MixinChunkProviderServer_DeferPopulation.java)
- Its other `ChunkProviderServer` mixins (`disableChunkTerrainGeneration`, `disableWorldTypeChunkPopulation`, `disableModdedChunkPopulation`, all off in the pack) redirect the calls **through the `IChunkProvider` interface**, so they still work on a delegating wrapper. [MixinChunkProviderServer_DisablePopulation.java](https://github.com/GTNewHorizons/Hodgepodge/blob/2.7.221/src/main/java/com/mitchej123/hodgepodge/mixins/early/minecraft/MixinChunkProviderServer_DisablePopulation.java)
- With EndlessIDs, a chunk that is replaced must carry over the 16-bit biome array through `ChunkBiomeHook`. Hodgepodge needs a separate mixin variant for that. [MixinChunkProviderServer_DisableTerrain_EndlessIDs.java](https://github.com/GTNewHorizons/Hodgepodge/blob/2.7.221/src/main/java/com/mitchej123/hodgepodge/mixins/early/minecraft/MixinChunkProviderServer_DisableTerrain_EndlessIDs.java) The overlay edits the RWG chunk in place and never replaces it.
- `showChunkGenDebug` (off by default) shows the terrain and populate averages on the F3 screen, **in single player only**. [DebugConfig.java#L27](https://github.com/GTNewHorizons/Hodgepodge/blob/2.7.221/src/main/java/com/mitchej123/hodgepodge/config/DebugConfig.java#L27)

The cascading-worldgen **warning** in this pack comes from ArchaicFix, not Hodgepodge. It logs when a chunk is populated while another population is on the stack. [CascadeDetectionHelper.java](https://github.com/embeddedt/ArchaicFix/blob/0.8.2/src/main/java/org/embeddedt/archaicfix/helpers/CascadeDetectionHelper.java) The pack sets `logCascadingWorldgen=false`.

## Population order in the pack

What runs before and after the overlay, for one overworld chunk C:

| Order | Stage | Who | Writes |
|---|---|---|---|
| 1 | `provideChunk(C)` | RWG2 or RWG, as above | C's arrays only |
| 2 | **Overlay terrain phase** ([0005](../../design/0005-rwg-overlay.md) §2) | this mod | C only |
| 3 | `onChunkLoad` → `ChunkEvent.Load` | Forge | |
| 4 | `populate(C)` starts when neighbors are ready (vanilla: +x, +z, +xz; Hodgepodge scheduler: full 3×3) | | |
| 5 | `PopulateChunkEvent.Pre` listeners | NHCore Railcraft geodes [NH_GeodePopulator.java#L36-L37](https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/2.9.99/src/main/java/com/dreammaster/railcraftStones/NH_GeodePopulator.java#L36-L37) and quarries [NH_QuarryPopulator.java#L38-L39](https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/2.9.99/src/main/java/com/dreammaster/railcraftStones/NH_QuarryPopulator.java#L38-L39); both gate on `TerrainGen.populate`. RWG2 filters Railcraft's own population through `Populate` events [RailcraftWorldgenFilter.java#L13](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/data/RailcraftWorldgenFilter.java#L13) | +8 area |
| 6 | RWG structures, lakes, dungeons, ores, decoration, springs, landmarks | RWG (table above) | +8 area, trees spill past it |
| 7 | `PopulateChunkEvent.Post` listeners | NHCore BuildCraft oil deposits, gated on `Populate CUSTOM` [OilGeneratorFix.java#L139-L171](https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/2.9.99/src/main/java/com/dreammaster/modfixes/oilgen/OilGeneratorFix.java#L139-L171); other pack listeners *unverified* | +8 area |
| 8 | RWG ice, snow, ocean decorations | RWG | +8 area |
| 9 | **Overlay population phase** ([0005](../../design/0005-rwg-overlay.md) §3) | this mod | C only |
| 10 | `IWorldGenerator`s by weight | Thaumcraft, Natura, Forestry, IC2, Witchery, Roguelike Dungeons and others: weights *unverified* (S3, Q8) | mod-specific |
| 11 | Et Futurum deepslate layer (weight `Integer.MAX_VALUE`, per GT's comment) | EFR | stone below Y 22 |
| 12 | GT ore veins, forced last | GT5-Unofficial: weight `Integer.MAX_VALUE` [GTWorldgenerator.java#L147-L152](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/common/GTWorldgenerator.java#L147-L152), moved to the end by a mixin because "the ore world gen expects to be the last pass over the world" [GameRegistryMixin.java](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/mixin/java/gregtech/mixin/mixins/early/forge/GameRegistryMixin.java). Veins are queued and at most 5 queued chunks run per call, so **a chunk's ore can be placed during a later chunk's population** [#L175-L213](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/common/GTWorldgenerator.java#L175-L213) | ore chunks |

NewHorizonsCoreMod registers no `IWorldGenerator` for the overworld at 2.9.99 that a code search finds; its overworld hooks are the `Populate` listeners above.

## Candidate hook points (Q1)

| Hook | Fired by RWG2 / RWG? | Pros | Cons |
|---|---|---|---|
| **Delegating `IChunkProvider` wrapper** around `currentChunkProvider` | n/a: works on any provider (RWG, RWG2, flat, PersonalSpace) | One place for both phases; exact ordering (terrain after all of RWG's `provideChunk`; population after all of RWG's `populate` and before every `IWorldGenerator`); edits a real `Chunk` in place, with no lighting or block updates; times RWG and the overlay separately; no mixin; Hodgepodge's and ArchaicFix's hooks still apply | Must be installed before the first chunk is generated (see [timing](#when-to-install-the-wrapper)). Code that type-checks `currentChunkProvider` sees the wrapper: VillageNames only recognizes vanilla providers, so nothing changes for RWG ([StructureRegistry.java#L36-L41](https://github.com/GTNewHorizons/VillageNames/blob/4.5.21-GTNH/src/main/java/astrotibs/villagenames/structure/StructureRegistry.java#L36-L41)); Variable-Horizons (not in 2.9.0) checks `instanceof ChunkGeneratorRealistic`. Must forward every `IChunkProvider` method, including `recreateStructures` and `makeString` |
| `ChunkProviderEvent.ReplaceBiomeBlocks` | yes / yes | Event API; raw arrays, very fast | Fires **before** RWG paints the surface, carves caves and adds landmarks, so city blocks get repainted (stone to grass and dirt) and carved; `DENY` also drops bedrock; no chunk object; PersonalSpace and flat worlds do not fire it |
| `PopulateChunkEvent.Pre` / `Post` | yes / yes | Event API; world access | `Post` is not the end of RWG population (snow follows); world `setBlock` with lighting is far too slow for a whole city chunk; writes the +8 area, not the chunk |
| `PopulateChunkEvent.Populate` (`LAKE`, `LAVA`, `DUNGEON`, `CUSTOM`) | yes, results honored | Veto lakes, dungeons and NHCore oil in city chunks | Veto only, per populating chunk |
| `DecorateBiomeEvent.Decorate` | fired, **results ignored** | | Cannot stop RWG trees, boulders or ruins |
| `InitMapGenEvent` (`CAVE`, `VILLAGE`, `STRONGHOLD`, `MINESHAFT`) | yes / yes, at construction | Wrap the cave carver or village generator to keep them out of cities (Q8) | Not world-specific: fires for the private sampler too |
| `InitNoiseGensEvent` | **not fired** | | |
| `IWorldGenerator` | n/a | Standard API; generic | Runs after RWG's snow, unordered against equal weights; world writes only, too slow for the terrain phase; fine for small population work |
| `ChunkEvent.Load` | n/a | | Fires for chunks loaded from disk too; cannot tell a new chunk reliably |
| Mixin into RWG's `provideChunk` / `populate` | n/a | Exact placement | Two GPL mod classes, version-fragile, not generic (PersonalSpace) |
| Mixin into `ChunkProviderServer.originalLoadChunk` / `populate` | n/a | Generic; timing-proof | Hodgepodge, ArchaicFix and Variable-Horizons already redirect calls in exactly these methods; an `@Inject` at a redirected call site silently fails |
| Replace the overworld `WorldProvider` (`DimensionManager.registerProviderType(0, …)`) | n/a | Forge API, wraps at creation | Clashes with any mod that replaces dimension 0; useless for PersonalSpace. A GTNH org code search finds no such mod |
| New world type (upstream 1.12 adapter) | n/a | Upstream precedent | Ruled out by [0003](../../design/0003-world-presence.md) |

### When to install the wrapper

| Moment | New world | Existing world | Source |
|---|---|---|---|
| `WorldEvent.CreateSpawnPosition` | After `createChunkProvider`, **before the first chunk** | not fired | as above |
| `WorldEvent.Load` | too late: the spawn search has generated and populated chunks | before player-driven generation; chunk-loader tickets may load chunks earlier (mostly already generated) | as above |
| Mixin at `WorldServer.createChunkProvider` return | before anything | before anything | would be our own mixin |

Installing at `CreateSpawnPosition` (priority `HIGHEST`, `receiveCanceled = true`) with `Load` as an idempotent fallback covers new worlds, which are the target ([0003](../../design/0003-world-presence.md)). The mixin is the fallback if the prototype finds a chunk generated before installation.

## Prior art

### Upstream `1.12` ([`e8900023`](https://github.com/McJtyMods/LostCities/tree/e89000238a8095cba8a739fc3b9e595d854a1346))

- **`LostWorldTypeBOP`** is a separate world type that uses **Lost Cities' own terrain** with Biomes O' Plenty's biome provider. It does not overlay BOP terrain. [LostWorldTypeBOP.java](https://github.com/McJtyMods/LostCities/blob/e89000238a8095cba8a739fc3b9e595d854a1346/src/main/java/mcjty/lostcities/dimensions/world/LostWorldTypeBOP.java)
- **`LostWorldTypeAdapter`** registers `lc_<name>` world types for names in `adaptingWorldTypes`. It wraps the other world type's generator only if that generator implements Lost Cities' **`IChunkPrimerFactory`**: `fillChunk` (raw terrain, no decoration) and `getHeight` (a height estimate). [LostWorldTypeAdapter.java](https://github.com/McJtyMods/LostCities/blob/e89000238a8095cba8a739fc3b9e595d854a1346/src/main/java/mcjty/lostcities/dimensions/world/LostWorldTypeAdapter.java), [IChunkPrimerFactory.java](https://github.com/McJtyMods/LostCities/blob/e89000238a8095cba8a739fc3b9e595d854a1346/src/main/java/mcjty/lostcities/api/IChunkPrimerFactory.java). The code comments it "For ATG, experimental" ([LostCityChunkGenerator.java#L174-L185](https://github.com/McJtyMods/LostCities/blob/e89000238a8095cba8a739fc3b9e595d854a1346/src/main/java/mcjty/lostcities/dimensions/world/LostCityChunkGenerator.java#L174-L185)). A GitHub code search finds ATG as the only third-party implementer; no RTG integration was found.
- Lost Cities then ran its own caves, structures, decoration and population over the other mod's terrain. Neighbor heights came from generating and caching primers of neighbor chunks without loading them. [LostCityChunkGenerator.java#L188-L210](https://github.com/McJtyMods/LostCities/blob/e89000238a8095cba8a739fc3b9e595d854a1346/src/main/java/mcjty/lostcities/dimensions/world/LostCityChunkGenerator.java#L188-L210)
- Loot, spawners and sapling growth happened in `populate`, just before `PopulateChunkEvent.Post`. [#L524-L636](https://github.com/McJtyMods/LostCities/blob/e89000238a8095cba8a739fc3b9e595d854a1346/src/main/java/mcjty/lostcities/dimensions/world/LostCityChunkGenerator.java#L524-L636)
- Vegetation in cities was vetoed through `DecorateBiomeEvent.Decorate` per profile flag. [TerrainEventHandlers.java](https://github.com/McJtyMods/LostCities/blob/e89000238a8095cba8a739fc3b9e595d854a1346/src/main/java/mcjty/lostcities/TerrainEventHandlers.java) RWG ignores those results, so this does not carry over.

What applies to 1.7.10: the split into a terrain phase that writes the chunk, a population phase for tile entities, and a **height oracle** that answers for chunks not yet generated. The RWG2 world-less sampler plays the role of `IChunkPrimerFactory.getHeight`, with no cooperation needed from RWG. What does not apply: the separate world type, and decoration vetoes.

### Upstream `1.20` ([`f18a094d`](https://github.com/McJtyMods/LostCities/tree/f18a094d))

- Cities are written by a feature in the first decoration step, `raw_generation` ([lostcities.json#L5](https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/resources/data/lostcities/forge/biome_modifier/lostcities.json#L5)). Carvers run before it, and all later vanilla decoration runs over the city, limited only by its own placement checks. Upstream uses no mixins. The overlay's two phases reproduce this order over RWG.
- The chunk height is the generator's noise-only base height at block (8, 8) ([LostCityTerrainFeature.java#L922-L942](https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/LostCityTerrainFeature.java#L922-L942)). By default it is sampled once per 3×3 chunk group (`heightSampleSize = 3`) ([Config.java#L165-L167](https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/setup/Config.java#L165-L167)), cached per chunk ([#L843-L890](https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/LostCityTerrainFeature.java#L843-L890)).
- City level comes from thresholds on that height ([BuildingInfo.java#L1261-L1327](https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/BuildingInfo.java#L1261-L1327)). City ground level is `groundLevel + cityLevel × 6` (`FLOORHEIGHT`, [LostCityTerrainFeature.java#L63](https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/LostCityTerrainFeature.java#L63)).
- **Edge blending** (`correctTerrainShape`) runs on non-city chunks. It builds a lower and an upper height mesh from the nearest city corners, one chunk away (L1, offsets 20 and 25 blocks) and two chunks away (L2), plus the profile's `terrainFix*` offsets ([BuildingInfo.java#L2001-L2093](https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/BuildingInfo.java#L2001-L2093)). It interpolates the mesh bilinearly with transition noise, lowers columns above the upper mesh (keeping six surface layers), and raises columns below the lower mesh ([LostCityTerrainFeature.java#L593-L810](https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/LostCityTerrainFeature.java#L593-L810)). It reads and writes only the chunk being generated, so it fits the overlay's terrain phase.
- The L1 mesh uses `h < 256` as "a city corner exists" ([BuildingInfo.java#L2009](https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/BuildingInfo.java#L2009)). In 1.7.10, 256 is the build limit, so the port needs an explicit flag.
- Overworld profile selection: the `selectedProfile` server config (default `<CHECK>`, filled from the client's world-creation screen), `selectedCustomJson`, and `dimensionsWithProfiles` (`<dimension>=<profile>`). [Config.java#L75-L127](https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/setup/Config.java#L75-L127), [#L158-L174](https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/setup/Config.java#L158-L174)

### 1.7.10 mods in the GTNH orbit

- **Variable-Horizons** (GTNH org, MIT, [0.1.09](https://github.com/GTNewHorizons/Variable-Horizons/tree/0.1.09), **not in 2.9.0**) changes generation per world "variant" with `ChunkProviderServer` redirects ([MixinChunkProviderServer_DisablePopulation.java](https://github.com/GTNewHorizons/Variable-Horizons/blob/0.1.09/src/main/java/com/LazyFlesh/variablehorizons/mixin/mixins/early/MixinChunkProviderServer_DisablePopulation.java)), an accessor into RWG2's generator, and a `Chunk.func_150807_a` guard that rewrites blocks while a flag says generation is running ([MixinChunk_ReplaceGeneratedBlocks.java](https://github.com/GTNewHorizons/Variable-Horizons/blob/0.1.09/src/main/java/com/LazyFlesh/variablehorizons/mixin/mixins/early/MixinChunk_ReplaceGeneratedBlocks.java)). Variants are chosen in a single-player GUI or, on servers, by config or command, with a restart. It is the nearest precedent for both a write guard and per-world selection, and a likely future neighbor whose `instanceof` checks a wrapper would defeat.
- **RWG itself** places villages, small ruins, volcanoes and landmarks over its own terrain. Within this pack, no mod was found that overlays a large multi-chunk structure on RWG terrain.
- **PersonalSpace** has its own `PersonalChunkProvider implements IChunkProvider`, created by `PersonalWorldProvider.createChunkGenerator`, and fires no terrain events. [PersonalChunkProvider.java#L32](https://github.com/GTNewHorizons/PersonalSpace/blob/1.0.41/src/main/java/me/eigenraven/personalspace/world/PersonalChunkProvider.java#L32), [PersonalWorldProvider.java#L76](https://github.com/GTNewHorizons/PersonalSpace/blob/1.0.41/src/main/java/me/eigenraven/personalspace/world/PersonalWorldProvider.java#L76). Only a provider-level hook reaches it.

## Profile selection precedents (Q7)

| Mechanism | Who uses it | Fit |
|---|---|---|
| World-preset screen | Upstream 1.20 | No world type of our own, so no preset or customize button |
| Server config naming the profile | Upstream 1.20 `selectedProfile` (a per-world server config in modern Forge) | 1.7.10 has no per-world config folder |
| World saved data written at creation | Classic RWG (biome list version, noise implementation), detected by `!worldInfo.isInitialized()` | Pins the choice to the world; config edits later cannot change it |
| Config plus restart, or a command | Variable-Horizons on servers | Simple; no client involvement |
| Client screen hook (`GuiScreenEvent.InitGuiEvent` on `GuiCreateWorld`) | Variable-Horizons (world-selection screen) | Single player only; later, if at all |

## Measuring generation cost (Q10)

What exists in the pack:

- Hodgepodge's per-dimension terrain and populate averages. Single player only (F3), and they measure the whole `ChunkProviderServer.populate`, including every `IWorldGenerator`.
- No timing in RWG2. Its diagnostics trace trees and villages only. [WorldgenDiagnostics.java](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/support/WorldgenDiagnostics.java)
- Java Flight Recorder in the server JVM (the pack runs on Java 17 or later through lwjgl3ify) attributes samples to `rwg2.*`, `gregtech.*` and our package.

A wrapper can time `delegate.provideChunk`, the terrain phase, `delegate.populate` and the population phase separately, on the same chunks, in the same JVM. [0005](../../design/0005-rwg-overlay.md) sets out the method and the budget.

## Unverified, to check before code

- Weights of the other overworld `IWorldGenerator`s (Thaumcraft, Natura, Forestry, IC2, Witchery, Roguelike Dungeons, Botania, BOP) and any other `PopulateChunkEvent` listeners in the pack (S3, Q8).
- Whether EndlessIDs 1.7.4 needs anything beyond `ExtendedBlockStorage`'s block and metadata setters for writes into a generated chunk (read its source or decompile its mixin list).
- Whether a GTNH Maven `dev` artifact exists for RWG2 2.0.6 and RWG alpha-1.5.3 (for `compileOnly`).
- Forge 10.13.4.1614 against branch head `9274e4fe` for the files cited.
- The terrain height ranges above, until the prototype's histogram replaces them.
