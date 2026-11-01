# GT ore veins and cities

GT5-Unofficial 5.09.54.265 generates every overworld ore in GTNH 2.9.0: large layered veins on a 3×3-chunk grid, "placer" small ores around each vein, scattered small ores, and boulders of granite, marble and basalt. It runs as one `IWorldGenerator` that a GT mixin forces to the very end of the generator list, so it always runs after the chunk provider's own population and after every other mod's world generator, sometimes deferred to a later chunk's population. Ore blocks only ever replace a fixed list of natural stones (`minecraft:stone`, netherrack, end stone, GT granite, marble and basalt, Et Futurum deepslate and tuff, and the Moon, Mars, asteroid and GalaxySpace planet stones, which GT accepts in any dimension); they never replace air, dirt, bricks, planks or any other building block. The boulders are broader: they replace stone, cobblestone, stained hardened clay, netherrack, end stone and every metadata of GT's granite and stone blocks. GT has no API to exclude regions from ore generation and no event that says a chunk's ore generation is complete. So cities coexist with veins best by **not being made of ore-host blocks**: veins then run through the natural stone under and around a city and never touch a building. The few boulder-host blocks that buildings keep (stained hardened clay for terracotta, and the dressed GT stones the variety pools admit on the same terms, D28) are the only residual conflict.

Status: *Accepted* (spike S3; [decision log](../../design/decision-log.md) D18). Settles open question Q3.

## Answer to Q3

**Veins pass through; buildings are ore-inert.** The mechanics are verified from source and pack config. The alpha screenshots show how often boulders mark terracotta buildings, and that decides whether option B is built.

1. **Nothing suppresses, moves or excludes GT generation.** Every vein, small ore and boulder that GT would place in natural stone under or around a city is placed. The city's foundation fill (upstream's profile `baseBlock`, `minecraft:stone`) is natural stone, so veins continue under and into city mounds, and basements and subways that cut through a vein expose its ore.
2. **The block mapping keeps buildings, streets, highways and railways off the ore-host list** (the [host table](#which-blocks-gt-replaces) below). That alone makes them immune to veins, placer ores and small ores, which is the bulk of GT's output. The one upstream asset that would break the rule is the `stoneandbrick` variant (building8), which is about 94 % `minecraft:stone`; it maps to a non-host stone such as a smooth-stone or stone-brick block ([block-mapping.md](block-mapping.md)).
3. **Boulder hosts are accepted for alpha.** Upstream's colored terracotta (22 references in 10 palette and variant files) maps faithfully to `minecraft:stained_hardened_clay`, which GT's boulders can overwrite. The GT stones' cobble, brick, cracked, mossy, chiseled and smooth forms join the palettes' variety pools on the same terms (D28; [block catalog](../05-assets/block-catalog/index.md)). The alpha screenshot review decides whether boulder damage shows; if it does, M08 adds the restore pass (option B below) for boulder hosts only.
4. **Ore lost under a city is only what the city displaced before GT ran**: air in basements, tunnels and building volumes, and non-host blocks. That matches the owner's default ("losing some ore under a building is acceptable") without any restoration code.

Option B, a restore pass limited to boulder-host blocks, is the fix if the alpha screenshots show boulder damage. It needs a mixin into GT internals, because no GT event covers boulders or signals the end of a chunk's ore generation, so it is held back until the screenshots show it is needed. Terracotta keeps its faithful mapping either way, and GT stones stay in the pools: the owner chose the restore pass over non-host look-alikes.

## How GT generates the overworld

### The hook and when it runs

| Fact | Status |
|---|---|
| One class, `GTWorldgenerator`, implements `IWorldGenerator` and registers itself with `GameRegistry.registerWorldGenerator(this, Integer.MAX_VALUE)`. | *Verified:* [GTWorldgenerator.java#L147-L154](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/common/GTWorldgenerator.java#L147-L154) |
| A GT mixin (`GTWorldgenSortingFix`, always applied) moves every `GTWorldgenerator` to the end of Forge's sorted generator list, after Et Futurum's deepslate generator, which also uses `Integer.MAX_VALUE`. GT's ore generation is therefore the last world generator to run. | *Verified:* [GameRegistryMixin.java#L19-L46](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/mixin/java/gregtech/mixin/mixins/early/forge/GameRegistryMixin.java#L19-L46), [Mixin.java#L67-L69](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/mixin/Mixin.java#L67-L69) |
| `IWorldGenerator`s run from `ChunkProviderServer.populate`, right after the chunk provider's own `populate` (which is where `PopulateChunkEvent.Pre/Post` and biome decoration happen). So GT runs after the provider's population, after decoration, and after every other mod's world generator. | *Verified:* Forge 1.7.10 [ChunkProviderServer.java.patch#L11-L18](https://github.com/MinecraftForge/MinecraftForge/blob/9274e4fe435cb415099a8216c1b42235f185443e/fml/patches/minecraft/net/minecraft/world/gen/ChunkProviderServer.java.patch#L11-L18) |
| `generate` does not place anything directly. It queues a `WorldGenContainer` for the chunk in a static list and, unless a run is already in progress, runs queued tasks from the head of the list, at most five per call. Because the loop bound is recomputed as the list shrinks, a backlog drains a few tasks per call. **A chunk's ore generation can run during a later chunk's population**, possibly in another dimension. | *Verified:* [GTWorldgenerator.java#L156-L214](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/common/GTWorldgenerator.java#L156-L214) |
| The dimension is found by name (`"Overworld"` for dimension 0, whatever the chunk provider), so RWG2 Continents and classic RWG overworlds get the overworld ore set. | *Verified:* [DimensionDef.java#L25-L28](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/galacticgreg/api/enums/DimensionDef.java#L25-L28), [DimensionDef.java#L233-L262](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/galacticgreg/api/enums/DimensionDef.java#L233-L262) |
| One task (`WorldGenContainer.run`) does, in order: (1) every entry of `GregTechAPI.sWorldgenList`, which holds the boulder generators and the scattered small ores; (2) every vein whose seed chunk lies within 2 chunks of this chunk. | *Verified:* [GTWorldgenerator.java#L689-L746](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/common/GTWorldgenerator.java#L689-L746), [WorldgenStone.java#L54-L55](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/common/WorldgenStone.java#L54-L55), [WorldgenGTOreSmallPieces.java#L37](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/common/WorldgenGTOreSmallPieces.java#L37) |
| No other class in the GT5-Unofficial jar (which also carries bartworks, GT++, GalacticGreg and others) registers a world generator or listens to population events in the overworld. `WorldGeneratorSpace` handles asteroid dimensions. | *Verified* by search of the tag: only [GTWorldgenerator.java](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/common/GTWorldgenerator.java) and [WorldGeneratorSpace.java](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/galacticgreg/WorldGeneratorSpace.java) implement `IWorldGenerator`; the rest are dimension chunk providers |
| Vanilla coal, iron, gold, diamond, redstone, lapis and quartz generation is denied through `OreGenEvent.GenerateMinable` when `disableVanillaOres` is true. Vanilla underground dirt and gravel stay on. | *Verified:* [GTProxy.java#L172-L179](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/common/GTProxy.java#L172-L179), [GTProxy.java#L1082-L1088](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/common/GTProxy.java#L1082-L1088); pack: `config/GregTech/WorldGeneration.cfg` (`disableVanillaOres=true`, `generateUndergroundDirtGen=true`, `generateUndergroundGravelGen=true`) |
| Whether RWG2's and classic RWG's decorators post `OreGenEvent` so that the vanilla-ore denial reaches them. | *unverified*; RWG research (`rwg.md`) |

### Pack settings that change ore generation

All from `config/GregTech/GregTech.cfg` unless noted. Ore mixes themselves are defined in code ([OreMixes.java](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/api/enums/OreMixes.java)); no pack config overrides them (*verified* by search of the pack's `config/` folder).

| Setting | Pack value | Effect |
|---|---|---|
| `oreveinPercentage` | 100 | Every vein seed chunk tries to get a vein ([ModDimensionDef.java#L38](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/galacticgreg/api/ModDimensionDef.java#L38)) |
| `oreveinAttempts` | 64 | Mixes tried per seed |
| `oreveinMaxPlacementAttempts` | 8 | Failed placements tolerated per seed |
| `oreveinPlacerOres` | true | Each vein chunk also gets placer small ores |
| `oreveinPlacerOresMultiplier` | 2 | Placer ore count multiplier |
| `generateBasaltOres`, `generateBlackGraniteOres`, `generateMarbleOres`, `generateRedGraniteOres` | true | Ores may form inside those stones (`config/GregTech/WorldGeneration.cfg`) |
| Et Futurum `deepslateGenerationMode` | 0 | Stone below about y 22 becomes deepslate, overworld only (`config/etfuturum/world.cfg`: `deepslateMaxY=22`, dimension list `0` used as a whitelist). Deepslate and tuff are ore hosts in the overworld. |

### Where veins go

| Fact | Status |
|---|---|
| Vein seed chunks are every third chunk on each axis. New worlds use the `EQUAL_SPACING` pattern: chunk x and z both ≡ 1 (mod 3), with `floorMod`, so the grid continues across 0. Worlds created before 5.09.43.111 keep the older `AXISSYMMETRICAL` grid. The pattern is saved per world (`GregTech_OregenPattern`). | *Verified:* [GTWorldgenerator.java#L79-L83](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/common/GTWorldgenerator.java#L79-L83), [GTWorldgenerator.java#L216-L224](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/common/GTWorldgenerator.java#L216-L224) |
| Each seed's vein comes from an RNG seeded by the world seed, the dimension ID and the seed chunk's coordinates, then a weighted pick among the mixes allowed in the dimension. | *Verified:* [GTWorldgenerator.java#L472-L545](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/common/GTWorldgenerator.java#L472-L545) |
| **The pick reads the world.** The first chunk to process a seed dry-runs the chosen mix's bottom layer in that chunk; if no ore could be placed there (no host stone), it tries another mix, up to 8 failed placements, then marks the seed empty. The result is cached in a static map for every later chunk of that seed. Which vein a seed gets therefore depends on generation order and on the blocks already in the first chunk processed, including city blocks. | *Verified:* [GTWorldgenerator.java#L503-L657](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/common/GTWorldgenerator.java#L503-L657), [WorldgenGTOreLayer.java#L188-L192](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/common/WorldgenGTOreLayer.java#L188-L192) |
| Vein bottom Y is random within the mix's height range (overworld dimensions respect vein heights). A vein spans 16 + 2 × rand(size) blocks on each horizontal axis around its seed chunk (size 16 to 32 for overworld mixes), and 9 layers vertically, from bottom Y − 1. Density falls off with distance from the seed. | *Verified:* [WorldgenGTOreLayer.java#L293-L344](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/common/WorldgenGTOreLayer.java#L293-L344), [WorldgenGTOreLayer.java#L361-L399](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/common/WorldgenGTOreLayer.java#L361-L399) |
| A task places the part of each nearby vein that falls in world columns `chunkX·16 + 2` to `chunkX·16 + 17` (and the same in z): GT offsets by 2 blocks, not vanilla's 8, so a task writes 2 columns into the +x and +z neighbors. | *Verified:* [WorldgenGTOreLayer.java#L209-L233](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/common/WorldgenGTOreLayer.java#L209-L233) |
| Placer small ores: about (vein overlap area × density ÷ 10 × multiplier) attempts per vein chunk, at random columns in the same window and at y 10 to 169 (sporadic: 10 to 199), regardless of the vein's own height. | *Verified:* [WorldgenGTOreLayer.java#L406-L443](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/common/WorldgenGTOreLayer.java#L406-L443) |
| Scattered small ores: per chunk, at vanilla's +8 offset (`chunkX·16 + 8` to `+ 23`), within each small-ore definition's height range. | *Verified:* [WorldgenGTOreSmallPieces.java#L113-L151](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/common/WorldgenGTOreSmallPieces.java#L113-L151) |

Overworld height ranges, read from [OreMixes.java](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/api/enums/OreMixes.java) and [SmallOres.java](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/api/enums/SmallOres.java) (*verified*):

| Kind | Range | Notes |
|---|---|---|
| Veins (21 overworld mixes, total weight 1520) | y 5 to 220 | Mixes reaching above y 60 carry 720 of the 1520 weight: Cassiterite 60–220, Lignite 80–210, Copper-Tin 80–200, Dolomite 150–200, Magnetite 60–180, Coal 30–80, Oil Sands 50–80. GTNH's RWG terrain is high, so these veins sit at city height. |
| Small ores (11 overworld) | y 5 to 250 | Coal 120–250, Tin 80–220, Zinc 80–210, Copper 60–180, Nickel 80–150, Iron 40–100; the rest below y 60 |
| Placer small ores | y 10 to 199 | Anywhere in a vein chunk |

Since every range overlaps city height, **height alone never keeps ore out of a city**; only the host rule below does.

### Which blocks GT replaces

Ore placement (veins, placer ores, small ores) goes through `OreManager.setOreForWorldGen`, which places an ore only where the existing block is one of the enabled stone types valid in the dimension. Air never qualifies. The ore block keeps the stone type of the block it replaced.

| Ore host in the overworld | Block (meta) | Status |
|---|---|---|
| Stone | `minecraft:stone` (0) | *Verified* |
| Netherrack | `minecraft:netherrack` (0); not dimension-restricted, so a host in the overworld too | *Verified* |
| End stone | `minecraft:end_stone` (0); not dimension-restricted | *Verified* |
| Black granite, red granite | `gregtech:gt.blockgranites` (0, 8) only: the raw stones, not the cobble, bricks or smooth variants | *Verified* |
| Marble, basalt | `gregtech:gt.blockstones` (0, 8) only | *Verified* |
| Deepslate | `etfuturum:deepslate`, **any metadata**; overworld only | *Verified* |
| Tuff | `etfuturum:tuff` (0); overworld only | *Verified* |
| Moon rock | `GalacticraftCore:tile.moonBlock` (4); any dimension | *Verified* ([galacticraft.md](../05-assets/block-catalog/galacticraft.md)) |
| Mars stone, Mars sub-surface rock | `GalacticraftMars:tile.mars` (9, 6); any dimension | *Verified* ([galacticraft.md](../05-assets/block-catalog/galacticraft.md)) |
| Asteroid rock | `GalacticraftMars:tile.asteroidsBlock` (1); any dimension | *Verified* ([galacticraft.md](../05-assets/block-catalog/galacticraft.md)) |
| GalaxySpace planet stones (33) | `GalaxySpace:` `mercuryblocks` (1, 2), `venusblocks` (1), `ceresblocks` (1), `phobosblocks` (1, 2), `deimosblocks` (1), `ioblocks` (2), `europagrunt` (1), `ganymedeblocks` (1), `callistoblocks` (1), `enceladusblocks` (1, 3), `titanblocks` (1, 2), `mirandablocks` (1, 2), `oberonblocks` (1, 2), `proteusblocks` (1, 2), `tritonblocks` (1, 2), `plutoblocks` (4, 5), `haumeablocks`, `makemakegrunt` (1), `acentauribbsubgrunt`, `barnardaEsubgrunt`, `barnardaFsubgrunt`, `vegabsubgrunt`, `tcetieblocks` (1, 2); any dimension | *Verified* ([galaxyspace.md](../05-assets/block-catalog/galaxyspace.md)) |

GT registers the Moon, Mars, asteroid and GalaxySpace stone types without a dimension restriction, and `OreManager` places ore into any enabled stone type valid in the dimension, so these planet stones would take ore in the overworld if a city placed them ([StoneType.java#L84-L127](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/api/enums/StoneType.java#L84-L127), [#L385-L409](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/api/enums/StoneType.java#L385-L409), [#L462-L474](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/api/enums/StoneType.java#L462-L474)). The surface blocks of the GalaxySpace bodies (meta 0 or the plain `…grunt` block, except Haumea's only block), the dungeon bricks, slabs, stairs, walls and Mars cobblestone are not hosts. None of these planet blocks is a boulder host.

Sources: [OreManager.java#L87-L124](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/common/ores/OreManager.java#L87-L124), [StoneType.java#L49-L83](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/api/enums/StoneType.java#L49-L83), [StoneType.java#L162-L174](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/api/enums/StoneType.java#L162-L174), [StoneType.java#L374-L411](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/api/enums/StoneType.java#L374-L411); block-and-meta matching (exact meta unless either side is the wildcard): GTNHLib 0.11.54 [ImmutableBlockMeta.java#L46-L50](https://github.com/GTNewHorizons/GTNHLib/blob/0.11.54/src/main/java/com/gtnewhorizon/gtnhlib/util/data/ImmutableBlockMeta.java#L46-L50); registry names: [BlockGranites.java#L16](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/common/blocks/BlockGranites.java#L16), [BlockStones.java#L13](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/common/blocks/BlockStones.java#L13), [BlockStonesAbstract.java#L200-L202](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/common/blocks/BlockStonesAbstract.java#L200-L202).

GT boulders (`WorldgenStone`) use a different, broader test: a block is replaced when `isReplaceableOreGen` matches any of the targets below. Forge's default is `this == target`, so metadata is ignored, and no GT block overrides it.

| Boulder host | Covers | Status |
|---|---|---|
| `minecraft:stone` | all | *Verified* |
| `minecraft:cobblestone` | all (mossy cobblestone is a different block and is safe) | *Verified* |
| `minecraft:stained_hardened_clay` | **all 16 colors**: the 1.7.10 form of colored terracotta | *Verified* |
| `minecraft:end_stone`, `minecraft:netherrack` | all | *Verified* |
| `gregtech:gt.blockgranites`, `gregtech:gt.blockstones` | **all 16 metas**, including cobble, bricks, mossy, cracked, chiseled and smooth | *Verified* |

So every GT stone meta is a boulder host, and the raw metas 0 and 8 are ore hosts as well. In palettes, GT stones follow D28: the raw metas never, every other meta on terracotta's terms (kept, and covered by the restore pass if the alpha screenshots show boulder damage).

Sources: [WorldgenStone.java#L216-L233](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/common/WorldgenStone.java#L216-L233); Forge 1.7.10 [Block.java.patch](https://github.com/MinecraftForge/MinecraftForge/blob/9274e4fe435cb415099a8216c1b42235f185443e/patches/minecraft/net/minecraft/block/Block.java.patch) (`isReplaceableOreGen` returns `this == target`); search of the GT tag finds no override.

Boulders in the overworld (*verified*, [GTStones.java#L208-L388](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/api/enums/GTStones.java#L208-L387), [WorldgenStone.java#L68-L239](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/common/WorldgenStone.java#L68-L239)):

- 20 generators, all enabled: black granite, red granite, marble and basalt, each in five sizes. Radius before axis squashing is `size / 16` in integer blocks: 4, 6, 12, 18 and 25. One to three axes are shrunk by up to 4×, so most boulders are shafts or slabs.
- Each chunk is a boulder seed for a generator with chance 1/5, 1/10, 1/10, 1/70 or 1/150 by size. Summed over the 20 generators, that is about **1.7 boulder seeds per chunk**.
- Centers are at y 0 to 179 (12 generators) or 0 to 119 (8 generators).
- A boulder is skipped entirely when the block at its bottom Y in the processing chunk's center column is air, and individual air blocks are never filled. The skip is cached for the seed, so it depends on which chunk processes the seed first.
- Writes use vanilla's +8 window and flag 0 (no block update).

### Ore blocks are plain blocks

| Fact | Status |
|---|---|
| Ores are the meta blocks `gregtech:gt.blockores2` to `gt.blockores7`, one per group of stone types, with no tile entity. Metadata is `material ID + 1000 × stone index`, plus offsets for small and natural ores; this exceeds 15 and relies on the pack's EndlessIDs extended metadata. | *Verified:* [GTBlockOre.java#L57-L63](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/common/blocks/GTBlockOre.java#L57-L63), [GTOreAdapter.java#L62-L116](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/common/ores/GTOreAdapter.java#L62-L116), [GTOreAdapter.java#L210-L231](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/common/ores/GTOreAdapter.java#L210-L231) |
| The old tile-entity ores (`TileEntityOres`) are still registered, for worlds from older versions; new generation does not place them. | *Verified:* [BlockOresAbstractLegacy.java#L50](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/common/blocks/BlockOresAbstractLegacy.java#L50) |
| Ore writes use `setBlock(..., 3)` (block update and client sync); boulder writes use flag 0. | *Verified:* [OreManager.java#L93](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/common/ores/OreManager.java#L93), [WorldgenStone.java#L230](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/common/WorldgenStone.java#L230) |

A restore pass can therefore overwrite an ore or boulder block with an ordinary `setBlock`, with no tile entity to clean up.

### Hooks other mods can use

| Hook | What it gives | Status |
|---|---|---|
| `gregtech.api.events.VeinGenerateEvent` on the Forge event bus | Posted after each cached vein is placed into a chunk: world, task chunk, seed chunk, the layer and the placement result. It is **not** posted for boulders or scattered small ores, nor for the first chunk of a seed whose vein search failed. The number of events per task varies (one per nearby seed), so it cannot signal "this chunk's ore generation is done". | *Verified:* [VeinGenerateEvent.java#L8-L25](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/api/events/VeinGenerateEvent.java#L8-L25), [GTWorldgenerator.java#L660-L687](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/common/GTWorldgenerator.java#L660-L687) |
| Region exclusion | None. The only exclusion zones are hard-coded inside GT for The End (main island, Hardcore Ender Expansion islands, Draconic Evolution's chaos island). | *Verified:* [DimensionDef.java#L257-L280](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/galacticgreg/api/enums/DimensionDef.java#L257-L284) |
| "Chunk ore generation finished" | No event. The task queue is private. | *Verified:* [GTWorldgenerator.java#L53](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/common/GTWorldgenerator.java#L53), [GTWorldgenerator.java#L689-L746](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/common/GTWorldgenerator.java#L689-L746) |

## Options for cities

| | Option | Ore kept | Buildings intact | Coupling to GT | Determinism and cascades |
|---|---|---|---|---|---|
| **A** | **Ore-inert buildings** (chosen). Never touch GT; map every non-natural city block to a non-host block. | All ore in natural stone, under and around cities | Immune to veins, placer and small ores. Exposed only where a building keeps a boulder host (terracotta as stained hardened clay, and the GT stones D28 admits) | None | City blocks stay a pure function of seed and layout. Nothing new runs after GT. |
| **B** | **Restore after GT** (the fallback, boulder hosts only). After GT's task for a chunk finishes, re-place the planned city blocks in the window that task wrote (`chunkX·16 + 2` to `+ 23`, same in z) wherever the current block differs from the plan and the plan block is not natural stone. | As A; ore inside restored blocks is lost | Fully restored | A mixin at the tail of `GTWorldgenerator$WorldGenContainer.run`, because no event covers boulders or signals completion. Brittle across GT bumps (internal nested class). | The window lies in chunks that population guarantees exist, so no cascade, but the pass can run during a later chunk's population because GT defers tasks. The re-placed blocks come from the deterministic layout, so the result stays deterministic for city blocks. Costs about one chunk's write per chunk. |
| **C** | **Exclusion.** Mixin into `OreManager.setOreForWorldGen` and `WorldgenStone` (or `GTWorldgenerator.generate`) to skip positions inside city footprints. | Loses every vein crossing a city footprint, above and below ground | Fully intact | Two or more mixins into GT internals, called per block | Changes GT's own vein choice (the dry run reads the world). Fights GT ore generation, which [0004](../../design/0004-gtnh-layer-and-tier-ceiling.md) rules out. |
| **D** | **Veins kept below ground level only.** | Same as A in practice | Same as A | None | Already what A delivers: ore only forms in host stone, the stone under a city is natural, and the buildings above are not hosts. A height cut would need option C's mixins and would also remove high veins from the RWG mountains around a city. |

**Decision: A now, B only if the alpha screenshots show boulder damage** (D18). A needs no code that touches GT, keeps all ore that natural terrain would hold, and makes the block mapping, not a runtime pass, carry the guarantee. B, limited to boulder-host blocks, is the fallback for the boulder hosts that faithful buildings keep. C and D are rejected.

### Consequences for other areas

- **Block mapping** ([block-mapping.md](block-mapping.md)): blocks that end up in buildings, streets, highways, railways and scattered structures must not be ore hosts (first host table). Prefer blocks that are not boulder hosts either (second table). Never use raw `minecraft:stone`, netherrack, end stone, the raw `gt.blockgranites` and `gt.blockstones` metas 0 and 8, `etfuturum:deepslate` (any meta), `etfuturum:tuff` meta 0, or any of the planet stones in the host table as a building block. Upstream's `minecraft:stone` in building parts maps to a non-host stone. Terracotta maps faithfully to `minecraft:stained_hardened_clay`, a boulder host only, and the other GT stone metas may join the variety pools on the same terms (D28).
- **Terrain** (S2, `rwg.md`): city foundation fill stays natural `minecraft:stone` so veins continue into it. City blocks are written before GT runs for that chunk, which holds whether they go in during chunk generation or in our own population, since GT is always last. Nothing the city writes is written again later, except by option B.
- **Testing:** golden-chunk and determinism tests cover city blocks only. GT's vein choice depends on generation order (static cache, deferred queue, a dry run that reads the world), so real-pack checks must not compare ore placement between two worlds.
- **Expected side effects** (accepted, nothing to fix): a vein seed whose first processed chunk is a city chunk may get a different mix, because the dry run sees fewer host blocks; building interiors can suppress a boulder seed for good; basements and tunnels expose vein cross-sections.

## Decisions

Recorded in the [decision log](../../design/decision-log.md) as D18:

- Option A at alpha: veins pass through cities, buildings are ore-inert, nothing is excluded and nothing is restored. Occasional granite, marble or basalt boulders cutting into terracotta buildings are accepted until the alpha review.
- If the alpha screenshots show boulder damage, option B: a restore pass limited to boulder-host blocks, through a tail mixin on GT's chunk task, re-checked at every GT bump. Terracotta is not remapped to non-host look-alikes.

Recorded as D28 (rule 4): GT stones (granite, marble, basalt and their brick and smooth forms) join the palettes' variety pools on terracotta's terms; option B, if built, covers them too. D28 (rule 7) also keeps the planet stones GT treats as ore hosts out of every palette.

## Pack-wide observations (outside this mod)

- `WorldgenStone.validStoneSeeds` is keyed by dimension and chunk only, not by world seed, and is never cleared. In single player, boulder seeds decided in one world carry over into the next world opened in the same session. *Verified:* [WorldgenStone.java#L34](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/common/WorldgenStone.java#L32), [WorldgenStone.java#L86-L118](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/common/WorldgenStone.java#L86-L118). Not this mod's concern; worth a GT issue.
- GT's ore output is order-dependent by design (static vein cache filled by the first chunk processed, a dry run that reads the world, a deferred queue), so the same seed can give different veins on two fresh worlds that generate chunks in a different order.
