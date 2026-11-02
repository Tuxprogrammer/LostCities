# 08 · Loot, mobs and blocks

What Lost Cities: New Horizons puts into chests and spawners, how its block lists steer generation in 1.7.10, and the rules for which blocks a city may hold. Upstream's behavior is in [original-spec/08-loot-mobs-blocks](../../original-spec/08-loot-mobs-blocks/index.md). The pack research behind the loot and mob choices is [loot-and-mobs.md](../00-pack-integration/loot-and-mobs.md); this index applies it to every upstream sheet and does not repeat its tables.

Status: *Accepted* where a row cites a decision record or a decision-log row; *Proposed* where it says so, with the recommendation in [Proposed, not decided](#proposed-not-decided). Bug IDs are from [BUGS.md](../../original-spec/BUGS.md); a row that names one fixes it, with a regression test ([0001](../../design/0001-port-policy-and-licensing.md) §2). Facts about other mods are *verified* with a pinned permalink or marked *unverified* ([new-spec README](../README.md#how-to-read)).

## Summary

- **Pack loot at EV or below.** City chests draw from this mod's own `ChestGenHooks` categories, one per tier band from Steam to EV plus rail, filled with pack items at generation, weighted toward the lower tiers; nothing above EV, checked by the dev self-check ([0004](../../design/0004-gtnh-layer-and-tier-ceiling.md) §3, D20).
- **No balance before beta.** Quantities and weights are not tuned in alpha; what may need it is on the [beta balance list](../beta-balance-list.md) ([0004](../../design/0004-gtnh-layer-and-tier-ceiling.md) §4).
- **Vanilla spawners with vanilla names**, `Blaze` included, at upstream's density. The pack's Special Mobs and Infernal Mobs vary what they spawn (D20).
- **No silverfish, no ores, no fortresses.** Infested blocks become plain stone bricks (D19); GT ore veins pass through cities untouched and no building block is an ore host (D18); unbreakable, explosive and painfully hard blocks are never placed (D28 rule 7, as amended by D29), except vanilla spawners, which GT makes hardness 500 and D20 places at upstream's density.
- **MalisisDoors** supplies the doors, as the pack already does for vanilla doors (D28 rule 5).
- **Block tags become lists computed at load.** 1.7.10 has no block tags; upstream's six tags become rules evaluated once against the live block registry, so pack blocks are covered without data packs. The rules are *Proposed* ([P08-1](#p08-1-block-lists-in-1710)).

## Deviations

### Conditions and selection

| Original sheet or system | This mod | What changes | Decision | Bugs avoided |
|---|---|---|---|---|
| [conditions.md](../../original-spec/08-loot-mobs-blocks/conditions.md) (sheet) | Changed | Same evaluation; values are this mod's loot categories and 1.7.10 entity names | D20 | LOOT-01, LOOT-02, LOOT-03, LOOT-04 |
| [Context values](../../original-spec/08-loot-mobs-blocks/conditions.md#context-values-server): `floor`, `level` | Changed | Floor division: a chest or spawner in cellar *k* reports floor −*k*, so upstream's deep-cellar entry (floors −100 to −3) is reached from the third cellar, as its range says, not the fourth | [0001](../../design/0001-port-policy-and-licensing.md) §2 | LOOT-01 (also ASSET-05) |
| Context value `belowPart` | Changed | The part on the floor below the position, instead of `<none>`; `belowpart` tests it instead of the current part | [0001](../../design/0001-port-policy-and-licensing.md) §2 | LOOT-02 (also ASSET-04) |
| Context values `part`, `building`, biome | Changed | Part and building as full ids ([05 P05-2](../05-assets/index.md#p05-2-one-name-form), *Proposed*); the biome in the 1.7.10 vocabulary ([05 P05-6](../05-assets/index.md#p05-6-biome-vocabulary), *Proposed*) | none yet | ASSET-33 |
| [Selection](../../original-spec/08-loot-mobs-blocks/conditions.md#selection): no entry matches | Changed | A chest stays empty and a spawner position becomes air, with one log line naming the condition; generation never aborts | [CONTRIBUTING § Code standards](../../../CONTRIBUTING.md#code-standards) | LOOT-03 (also ASSET-29) |
| [Weighted pick](../../original-spec/08-loot-mobs-blocks/conditions.md#weighted-pick) | Changed | A remainder left by float rounding takes the last entry; a non-empty list never returns nothing | [0001](../../design/0001-port-policy-and-licensing.md) §2 | LOOT-04 (also CORE-21, CITY-09) |
| [`chestloot`](../../original-spec/08-loot-mobs-blocks/conditions.md#chestloot-used-by-palette-character-c) | Changed | Keeps its floor and part tests and chooses among this mod's categories: ordinary floors draw mostly `city_supplies` and `city_lv`, higher floors and deeper cellars add `city_mv` and `city_hv`, `city_ev` has the smallest factor; the rail-dungeon parts draw `rail_dungeon`; the factor-30 `simple_dungeon` entry draws `city_supplies`. Factors are beta balance | D20 ([Loot design](../00-pack-integration/loot-and-mobs.md#loot-design-q5)) | |
| `chestloot`'s 32 other vanilla tables (factor 1 each) | Changed | Off by default. A profile option opts in to the 1.7.10 categories that exist ([vanilla-table entries](../00-pack-integration/loot-and-mobs.md#upstreams-vanilla-table-entries)); tables with no counterpart are dropped. The dispenser table is *Proposed* ([P08-2](#p08-2-the-dispenser-table)) | D20 | LOOT-06 |
| [`easymobs`](../../original-spec/08-loot-mobs-blocks/conditions.md#easymobs-used-by-palette-character-1), `hardmobs` | Changed | `Zombie`, `Skeleton`, `Spider` at 1/3 each; `Blaze`, `Witch` at 1/2 each: the 1.7.10 names of upstream's mobs. An optional EnderZoo `hardmobs` set exists, off by default, with `Skeleton` as its fallback | D20 ([Mob design](../00-pack-integration/loot-and-mobs.md#mob-design-q6)) | |
| [Palette entries `1`, `4`, `C`](../../original-spec/08-loot-mobs-blocks/conditions.md#palette-entries-that-reference-conditions) in `common.json` | Same | Same characters and conditions; the chest's facing turns with the part ([05](../05-assets/index.md#parts-and-placement)) | D19 | LOOT-09 |

### Chest loot

| Original sheet or system | This mod | What changes | Decision | Bugs avoided |
|---|---|---|---|---|
| [chest-loot.md](../../original-spec/08-loot-mobs-blocks/chest-loot.md) (sheet) | Changed | Same placement and switches; chests are filled at generation from this mod's categories, not given a loot table to roll on first open | D20 | LOOT-01, LOOT-03, LOOT-04, LOOT-05, LOOT-06, LOOT-09, LOOT-10 |
| [Behavior detail](../../original-spec/08-loot-mobs-blocks/chest-loot.md#behavior-detail): the queued post-todo and `ChunkFixer` | Changed | The chest is written in the terrain phase; its tile entity is filled in the population phase of the chunk it stands in, which is always loaded ([0005](../../design/0005-rwg-overlay.md#3-population-phase) §3). A chest removed by damage is skipped, as upstream | [0005](../../design/0005-rwg-overlay.md#3-population-phase) §3 | |
| Fill timing (roll on first open) | Changed | Filled at generation with `WeightedRandomChestContent.generateChestContents` from the chosen category; no per-world loot state. Chests generated before `FMLServerStartingEvent` (the spawn area) use this mod's defaults even where a TooMuchLoot file overrides the category | D20 ([Fill timing](../00-pack-integration/loot-and-mobs.md#fill-timing)) | |
| `generateLoot`, `chestWithoutLootChance` (0.2) | Same | Profile options as upstream (`02-profiles-dimensions`) | D20 | |
| `buildingWithoutLootChance` (0.2) and `noLoot` | Changed | Rolled once per building, multibuildings included: every chunk of a multibuilding (and of a scattered multibuilding) shares its origin chunk's roll, so a multibuilding is loot-free with the same chance as any building | [0001](../../design/0001-port-policy-and-licensing.md) §2; D20 | LOOT-10 |
| [`lostcities:chests/lostcitychest`](../../original-spec/08-loot-mobs-blocks/chest-loot.md#lostcitieschestslostcitychest), [`raildungeonchest`](../../original-spec/08-loot-mobs-blocks/chest-loot.md#lostcitieschestsraildungeonchest) | Dropped | Diamonds, emeralds, gold and diamond tools give way to the tier bands: `lostcitychest`'s entries go to `city_*` by floor, `raildungeonchest`'s to `rail_dungeon` | D20 | |
| [Vanilla tables reached through `chestloot`](../../original-spec/08-loot-mobs-blocks/chest-loot.md#vanilla-tables-reached-through-chestloot) | Changed | Only through the opt-in option, as the matching 1.7.10 categories, which TooMuchLoot rewrites in the pack | D20 | LOOT-06 |
| Loot categories | New | `lostcitiesnh:city_supplies` (Stone and Steam), `city_lv`, `city_mv`, `city_hv`, `city_ev` (rare), `rail_dungeon` (Steam to MV), registered in `postInit` with defaults from pack items: GT components and materials by tier, EnhancedLootBags bags of the matching tier, pack food and building supplies ([Categories](../00-pack-integration/loot-and-mobs.md#categories)) | D20; [0004](../../design/0004-gtnh-layer-and-tier-ceiling.md) §2, §3 | |
| Tier ceiling | New | A tier map (item to tier, loot-bag group to tier) ships with the loot defaults; the dev self-check walks every category this mod registers and fails on any item above EV or missing from the map | [0004](../../design/0004-gtnh-layer-and-tier-ceiling.md) §3; D20 | |
| Loot bags | New | Tech, food, rail, farming and computer families only; no magic, Blood Magic or Witchery bags by default | D20 | |
| Server-owner tuning | New | A TooMuchLoot XML file with the same category name retunes any category; this mod adds no content config of its own | D20 | |
| Absent mods in loot | New | An item whose mod is absent is skipped with one log line; a band never becomes empty, because its GT and vanilla entries always resolve | D20 | |
| [Determinism](../../original-spec/08-loot-mobs-blocks/chest-loot.md#determinism) | Changed | Loot draws use their own `Random`, seeded from the world seed, the chunk and the chest's position, never shared with vines or other post-steps, and chests are filled in position order. Golden-chunk tests compare blocks and the chosen category per chest, not item stacks | D9; [loot-and-mobs.md § Fill timing](../00-pack-integration/loot-and-mobs.md#fill-timing) | LOOT-05 |
| Containers other than the vanilla chest | Same | A palette entry with `loot` on any container (Et Futurum's barrel, a modded chest with a stored facing) is filled the same way ([05 asset-format.md § Tile-entity data](../05-assets/asset-format.md#tile-entity-data)) | D28 rule 6 | |
| Public static `createLoot` | Deferred | The API is `01-core`'s (M10) | none yet | |

### Spawners

| Original sheet or system | This mod | What changes | Decision | Bugs avoided |
|---|---|---|---|---|
| [spawners.md](../../original-spec/08-loot-mobs-blocks/spawners.md) (sheet) | Changed | Same positions and switches; 1.7.10 spawner and names | D20 | LOOT-01, LOOT-03 |
| Spawner block and data | Changed | `minecraft:mob_spawner`, its entity set with `MobSpawnerBaseLogic.setEntityName`, as Roguelike Dungeons does in the pack ([Spawner conventions](../00-pack-integration/loot-and-mobs.md#spawner-conventions)); written in the population phase. No spawner block of this mod's own | D20; [0004](../../design/0004-gtnh-layer-and-tier-ceiling.md) §2 | |
| Timing and range | Same | Vanilla defaults; Lost Cities writes none, as upstream | D20 | |
| `generateSpawners`, `noLoot` | Same | Off or loot-free: the position becomes air | D20 | LOOT-10 |
| Unknown entity names | Changed | Every mob a condition can return is checked against the entity registry at load. A mob whose mod is absent takes its fallback, and the dev self-check reports it | D20 | |
| Density | Same | Upstream's; on the [beta balance list](../beta-balance-list.md) with Blaze spawners | D20 | |
| Breaking spawners | Same | The pack's GT sets spawner hardness 500 and EnderIO drops a Broken Spawner each time; this mod changes neither. On the beta balance list | D20 | |
| Mob variety | New | No code: Special Mobs and Infernal Mobs transform what vanilla spawners spawn ([Spawner conventions](../00-pack-integration/loot-and-mobs.md#spawner-conventions)) | D20 | |

### Block tags

Upstream's six tags are data-pack files. 1.7.10 has no block tags, so each becomes a block list the asset system computes once at `postInit` from the live registry. The rules are *Proposed* ([P08-1](#p08-1-block-lists-in-1710)).

| Original sheet or system | This mod | What changes | Decision | Bugs avoided |
|---|---|---|---|---|
| [block-tags.md](../../original-spec/08-loot-mobs-blocks/block-tags.md) (sheet) | Changed | Six tags become five computed lists and one dropped step; no data generation, so nothing can drift | none yet ([P08-1](#p08-1-block-lists-in-1710)) | LOOT-07, LOOT-08, LOOT-09 |
| [`rotatable`](../../original-spec/08-loot-mobs-blocks/block-tags.md#lostcitiesrotatable) | Changed | Replaced by the rotation table of every directional block in the block-name map, so chests, furnaces, ladders, levers, trapdoors, wall torches, barrels and pack blocks turn with the part, not only stairs and doors | D19; [05](../05-assets/index.md#parts-and-placement) | LOOT-09 (also ASSET-18) |
| [`easybreakable`](../../original-spec/08-loot-mobs-blocks/block-tags.md#lostcitieseasybreakable) | Changed | One computed list instead of a shipped file and a generator that disagree; explosion damage on its members still ×2.5 (`03-terrain`) | none yet ([P08-1](#p08-1-block-lists-in-1710)) | LOOT-07 |
| [`lights`](../../original-spec/08-loot-mobs-blocks/block-tags.md#lostcitieslights) | Changed | Every registered block that emits light, pack blocks and lit variants included, instead of a vanilla-only list from data generation; the light work is the population phase's | none yet ([P08-1](#p08-1-block-lists-in-1710)); [0005](../../design/0005-rwg-overlay.md#3-population-phase) §3 | LOOT-08 |
| [`notbreakable`](../../original-spec/08-loot-mobs-blocks/block-tags.md#lostcitiesnotbreakable) | Changed | Blocks explosion damage never changes; `end_gateway` does not exist in 1.7.10 | none yet ([P08-1](#p08-1-block-lists-in-1710)) | |
| [`foliage`](../../original-spec/08-loot-mobs-blocks/block-tags.md#lostcitiesfoliage) | Changed | Leaves, logs, plants and saplings by class and ore-dictionary name, so RWG, Biomes O' Plenty and Forestry trees are see-through to terrain correction as vanilla trees are; bamboo and coral do not exist | none yet ([P08-1](#p08-1-block-lists-in-1710)) | |
| [`needspoi`](../../original-spec/08-loot-mobs-blocks/block-tags.md#lostcitiesneedspoi) and the dirt placeholder | Dropped | 1.7.10 has no points of interest; brewing stands, cauldrons and barrels are written directly | none needed (no 1.7.10 counterpart, [API-MAP](../../original-spec/API-MAP.md)) | |
| [Data generation](../../original-spec/08-loot-mobs-blocks/block-tags.md#data-generation) (`LCBlockTags`) | Dropped | No data generation in 1.7.10; the lists are computed at load | none needed | LOOT-07 |
| Data-pack additions to the tags | Dropped | No data packs; because the lists are computed from the registry, pack blocks need no additions | none yet ([P08-1](#p08-1-block-lists-in-1710)) | |

### Blocks

| Original sheet or system | This mod | What changes | Decision | Bugs avoided |
|---|---|---|---|---|
| Blocks and items registered by the mod | Same | None, as upstream: every block is the pack's or vanilla's | D19, D25; [CONTRIBUTING § GTNH integration](../../../CONTRIBUTING.md#gtnh-integration) | |
| Infested blocks | Changed | Every `minecraft:monster_egg` an asset names, upstream's three `infested_stone_bricks` palettes or an override's, maps to its plain block: no silverfish | D19 ([No infested blocks](../00-pack-integration/block-mapping.md#no-infested-blocks)) | |
| GT ore veins | New | Untouched: no ore generation, no exclusion, no restore pass at alpha. Building blocks are ore-inert; terracotta and the dressed GT stones are boulder hosts kept for the look, with a restore pass limited to boulder hosts if the alpha screenshots show damage. The profile's raw-stone `baseBlock` lets veins run under cities | D18 ([ore-veins.md](../00-pack-integration/ore-veins.md)); [0003](../../design/0003-world-presence.md) §4 | |
| Doors and trapdoors | Changed | MalisisDoors blocks with a tile entity and an opening animation, the pack's own replacement for vanilla doors; its wood doors stand in for upstream's 1.8 woods and its iron trapdoor for the 1.20 one | D28 rule 5 ([Doors and trapdoors](../00-pack-integration/block-mapping.md#doors-and-trapdoors)) | |
| Never-placed blocks | New | Unbreakable, explosive and painfully hard blocks (hardness above 50; vanilla spawners are exempt, at upstream's density, with GT's hardness on the [beta balance list](../beta-balance-list.md)), valuable storage outside loot, metals above EV, GT ore hosts, infested blocks, casings and machines, and the pack's gameplay hazards: the [never-place list](../05-assets/block-catalog/index.md#never-placed), enforced at load for every palette, override and generated part | D28 rule 7, D29; [0004](../../design/0004-gtnh-layer-and-tier-ceiling.md) §5 | |
| Metal storage blocks | Changed | Metals at or below EV come back as capped accents behind one flag each; treasure (`!`) and chest loot ignore the flags; metals above EV never appear ([decorative metals](../05-assets/block-catalog/index.md#decorative-metals-d29)) | D29 | |
| Bedrock and protected blocks | Changed | Never placed; RWG's own bedrock is left alone; nothing claims land | [0004](../../design/0004-gtnh-layer-and-tier-ceiling.md) §5; D19 | |

## Related areas

- [00-pack-integration](../00-pack-integration/index.md): [loot-and-mobs.md](../00-pack-integration/loot-and-mobs.md), [block-mapping.md](../00-pack-integration/block-mapping.md) and [ore-veins.md](../00-pack-integration/ore-veins.md), the research these rows apply.
- [05-assets](../05-assets/index.md): the condition and palette formats, tile-entity templates, rotation tables and the block catalog.
- [07-scattered](../07-scattered/index.md): chests and spawners in scattered structures.
- `03-terrain`, `04-city-layout`, `06-highways-railways`: not yet written; they own damage and lighting, the building plan behind `noLoot`, and the rail dungeons.

## Proposed, not decided

### P08-1 Block lists in 1.7.10

Each upstream tag needs a 1.7.10 rule. **Recommendation:** compute each list once at `postInit` from the live block registry, as below, and expose it to `03-terrain` as a set of block references.

| Tag | Upstream use | Recommended rule | Why |
|---|---|---|---|
| `easybreakable` | Explosion damage ×2.5 | Every block whose material is `Material.glass` | In 1.7.10 vanilla that is exactly the shipped file's vanilla members: glass, stained glass, both panes, glowstone and the beacon (*decompiled*: vanilla 1.7.10 has no public source; read from `Block.registerBlocks` and the `BlockBeacon` constructor of the pack's Minecraft 1.7.10 server with Forge 10.13.4.1614, so it may be wrong). Botania's sea lantern, the D19 stand-in for `sea_lantern`, uses it too ([BlockSeaLamp.java#L29](https://github.com/GTNewHorizons/Botania/blob/1.13.42-GTNH/src/main/java/vazkii/botania/common/block/decor/BlockSeaLamp.java#L29)). It also matches the generator's `#forge:glass` intent, so pack glass is covered. The shipped file and the generator stop disagreeing (LOOT-07) |
| `lights` | Queue a light update | Every block whose `getLightValue()` is above 0 | 1.7.10 keeps lit furnaces and redstone lamps as separate lit blocks, so they are included; pack lamps are covered (LOOT-08). Lights that depend on a tile entity are left to the population phase's relight |
| `notbreakable` | Explosion damage never applies | Every block with negative hardness, plus `end_portal` and `end_portal_frame` | Covers bedrock, the end portal frame and pack blocks that cannot be broken; nothing a city places is in it (D28 rule 7) |
| `foliage` | Terrain correction looks through it | Instances of `BlockLog`, `BlockLeavesBase`, `BlockBush`, `BlockVine`, `BlockHugeMushroom`, and the blocks of the ore names `logWood`, `treeLeaves`, `treeSapling` | Upstream's six vanilla plant tags plus `#minecraft:climbable` (vines); RWG's and the pack's trees then count as trees. *Unverified* which pack trees use classes outside this set; M04's terrain tests check RWG2's |
| `rotatable` | Rotate with the part | Replaced by the block-name map's rotation tables | [05](../05-assets/index.md#parts-and-placement) |
| `needspoi` | Points of interest | Dropped | No 1.7.10 counterpart |

### P08-2 The dispenser table

Upstream's `chestloot` offers `minecraft:chests/jungle_temple_dispenser` to chests, which then hold only arrows (LOOT-06). The pack research maps it to `pyramidJungleDispenser` and notes that it only fits a dispenser ([vanilla-table entries](../00-pack-integration/loot-and-mobs.md#upstreams-vanilla-table-entries)), but does not say what the converted `chestloot` does with it under the opt-in. **Recommendation:** drop the entry from the converted `chestloot`, which fills chests only; a future dispenser character would name its own condition.
