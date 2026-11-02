# 08 · Loot, mobs and blocks

What Lost Cities: New Horizons puts into chests and spawners, how its block lists steer generation in 1.7.10, and the rules for which blocks a city may hold. Upstream's behavior is in [original-spec/08-loot-mobs-blocks](../../original-spec/08-loot-mobs-blocks/index.md). The pack research behind the loot and mob choices is [loot-and-mobs.md](../00-pack-integration/loot-and-mobs.md); this index applies it to every upstream sheet and does not repeat its tables.

Status: *Accepted* where a row cites a decision record or a decision-log row. The two items this area once left open (P08-1, P08-2) were settled by D31 and are recorded in their rows; nothing is left in [Proposed, not decided](#proposed-not-decided). Bug IDs are from [BUGS.md](../../original-spec/BUGS.md); a row that names one fixes it, with a regression test ([0001](../../design/0001-port-policy-and-licensing.md) §2). Facts about other mods are *verified* with a pinned permalink or marked *unverified* ([new-spec README](../README.md#how-to-read)).

## Summary

- **Pack loot at EV or below.** City chests draw from this mod's own `ChestGenHooks` categories, one per tier band from Steam to EV plus rail, filled with pack items at generation, weighted toward the lower tiers; nothing above EV, checked by the dev self-check ([0004](../../design/0004-gtnh-layer-and-tier-ceiling.md) §3, D20).
- **No balance before beta.** Quantities and weights are not tuned in alpha; what may need it is on the [beta balance list](../beta-balance-list.md) ([0004](../../design/0004-gtnh-layer-and-tier-ceiling.md) §4).
- **Vanilla spawners with vanilla names**, `Blaze` included, at upstream's density. The pack's Special Mobs and Infernal Mobs vary what they spawn (D20).
- **No silverfish, no ores, no fortresses.** Infested blocks become plain stone bricks (D19); GT ore veins pass through cities untouched and no building block is an ore host (D18); unbreakable, explosive and painfully hard blocks are never placed (D28 rule 7, as amended by D29), except vanilla spawners, which GT makes hardness 500 and D20 places at upstream's density.
- **MalisisDoors** supplies the doors, as the pack already does for vanilla doors (D28 rule 5).
- **Block tags become lists computed at load.** 1.7.10 has no block tags; upstream's six tags become rules evaluated once against the live block registry, so pack blocks are covered without data packs ([Block tags](#block-tags); P08-1, D31).

## Deviations

### Conditions and selection

| Original sheet or system | This mod | What changes | Decision | Bugs avoided |
|---|---|---|---|---|
| [conditions.md](../../original-spec/08-loot-mobs-blocks/conditions.md) (sheet) | Changed | Same evaluation; values are this mod's loot categories and 1.7.10 entity names | D20 | LOOT-01, LOOT-02, LOOT-03, LOOT-04 |
| [Context values](../../original-spec/08-loot-mobs-blocks/conditions.md#context-values-server): `floor`, `level` | Changed | Floor division: a chest or spawner in cellar *k* reports floor −*k*, so upstream's deep-cellar entry (floors −100 to −3) is reached from the third cellar, as its range says, not the fourth | [0001](../../design/0001-port-policy-and-licensing.md) §2 | LOOT-01 (also ASSET-05) |
| Context value `belowPart` | Changed | The part on the floor below the position, instead of `<none>`; `belowpart` tests it instead of the current part | [0001](../../design/0001-port-policy-and-licensing.md) §2 | LOOT-02 (also ASSET-04) |
| Context values `part`, `building`, biome | Changed | Part and building as full ids ([05 § Names](../05-assets/asset-format.md#names), P05-2); the biome in the 1.7.10 vocabulary ([05 § Biome matchers](../05-assets/asset-format.md#biome-matchers), P05-6) | D31 | ASSET-33 |
| [Selection](../../original-spec/08-loot-mobs-blocks/conditions.md#selection): no entry matches | Changed | A chest stays empty and a spawner position becomes air, with one log line naming the condition; generation never aborts | [CONTRIBUTING § Code standards](../../../CONTRIBUTING.md#code-standards) | LOOT-03 (also ASSET-29) |
| [Weighted pick](../../original-spec/08-loot-mobs-blocks/conditions.md#weighted-pick) | Changed | A remainder left by float rounding takes the last entry; a non-empty list never returns nothing | [0001](../../design/0001-port-policy-and-licensing.md) §2 | LOOT-04 (also CORE-21, CITY-09) |
| [`chestloot`](../../original-spec/08-loot-mobs-blocks/conditions.md#chestloot-used-by-palette-character-c) | Changed | Keeps its floor and part tests and chooses among this mod's categories: ordinary floors draw mostly `city_supplies` and `city_lv`, higher floors and deeper cellars add `city_mv` and `city_hv`, `city_ev` has the smallest factor; the rail-dungeon parts draw `rail_dungeon`; the factor-30 `simple_dungeon` entry draws `city_supplies`. Factors are beta balance | D20 ([Loot design](../00-pack-integration/loot-and-mobs.md#loot-design-q5)) | |
| `chestloot`'s 32 other vanilla tables (factor 1 each) | Changed | Off by default. A profile option opts in to the 1.7.10 categories that exist ([vanilla-table entries](../00-pack-integration/loot-and-mobs.md#upstreams-vanilla-table-entries)); tables with no counterpart are dropped. The `jungle_temple_dispenser` entry is dropped from the converted `chestloot` even under the opt-in: it fills a chest with arrows only, and no bundled asset places a dispenser; `chestloot` is the only file that names it ([chestloot.json#L44-L46](https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/resources/data/lostcities/lostcities/conditions/chestloot.json#L44-L46)). A bug fix only; a future dispenser character would name its own condition. *Accepted* (P08-2, D31) | D20, D31 | LOOT-06 |
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

Upstream's six tags are data-pack files. 1.7.10 has no block tags, so each becomes a block list the asset system computes once at `postInit` from the live block registry, by the rule in its row, and exposes to `03-terrain` as a set of block references. *Accepted* (P08-1, D31).

| Original sheet or system | This mod | What changes | Decision | Bugs avoided |
|---|---|---|---|---|
| [block-tags.md](../../original-spec/08-loot-mobs-blocks/block-tags.md) (sheet) | Changed | Six tags become four computed lists, the rotation tables and one dropped step; no data generation, so nothing can drift | D31 (P08-1) | LOOT-07, LOOT-08, LOOT-09 |
| [`rotatable`](../../original-spec/08-loot-mobs-blocks/block-tags.md#lostcitiesrotatable) | Changed | Replaced by the rotation table of every directional block in the block-name map, so chests, furnaces, ladders, levers, trapdoors, wall torches, barrels and pack blocks turn with the part, not only stairs and doors | D19; [05](../05-assets/index.md#parts-and-placement) | LOOT-09 (also ASSET-18) |
| [`easybreakable`](../../original-spec/08-loot-mobs-blocks/block-tags.md#lostcitieseasybreakable) | Changed | Every block whose material is `Material.glass`: one computed list instead of a shipped file and a generator that disagree; explosion damage on its members still ×2.5 (`03-terrain`). In 1.7.10 vanilla that is exactly the shipped file's vanilla members: glass, stained glass, both panes, glowstone and the beacon (*decompiled*: vanilla 1.7.10 has no public source; read from `Block.registerBlocks` and the `BlockBeacon` constructor of the pack's Minecraft 1.7.10 server with Forge 10.13.4.1614). Botania's sea lantern, the D19 stand-in for `sea_lantern`, uses it too ([BlockSeaLamp.java#L29](https://github.com/GTNewHorizons/Botania/blob/1.13.42-GTNH/src/main/java/vazkii/botania/common/block/decor/BlockSeaLamp.java#L29)), and the rule matches the generator's `#forge:glass` intent, so pack glass is covered | D31 (P08-1) | LOOT-07 |
| [`lights`](../../original-spec/08-loot-mobs-blocks/block-tags.md#lostcitieslights) | Changed | Every block whose `getLightValue()` is above 0, pack blocks and lit variants included (1.7.10 keeps lit furnaces and redstone lamps as separate lit blocks), instead of a vanilla-only list from data generation. No light update is queued from it: ArchaicFix's Phosphor engine, on in the pack, lights every source when a chunk is first lit, so the list serves the scenario check that a fresh city's lamps are lit ([03-terrain](../03-terrain/index.md), P03-8) | D31 (P08-1, P03-8); [0005](../../design/0005-rwg-overlay.md#3-population-phase) §3 | LOOT-08 |
| [`notbreakable`](../../original-spec/08-loot-mobs-blocks/block-tags.md#lostcitiesnotbreakable) | Changed | Blocks explosion damage never changes: every block with negative hardness, plus `end_portal` and `end_portal_frame`. That covers bedrock, the end portal frame and pack blocks that cannot be broken; nothing a city places is in it (D28 rule 7). `end_gateway` does not exist in 1.7.10 | D31 (P08-1) | |
| [`foliage`](../../original-spec/08-loot-mobs-blocks/block-tags.md#lostcitiesfoliage) | Changed | Instances of `BlockLog`, `BlockLeavesBase`, `BlockBush`, `BlockVine` and `BlockHugeMushroom`, and the blocks of the ore names `logWood`, `treeLeaves` and `treeSapling`: upstream's six vanilla plant tags plus `#minecraft:climbable` (vines). RWG, Biomes O' Plenty and Forestry trees are then see-through to terrain correction as vanilla trees are; bamboo and coral do not exist. Which pack trees use classes outside this set is *unverified*; M04's terrain tests check RWG2's | D31 (P08-1) | |
| [`needspoi`](../../original-spec/08-loot-mobs-blocks/block-tags.md#lostcitiesneedspoi) and the dirt placeholder | Dropped | 1.7.10 has no points of interest; brewing stands, cauldrons and barrels are written directly | none needed (no 1.7.10 counterpart, [API-MAP](../../original-spec/API-MAP.md)) | |
| [Data generation](../../original-spec/08-loot-mobs-blocks/block-tags.md#data-generation) (`LCBlockTags`) | Dropped | No data generation in 1.7.10; the lists are computed at load | none needed | LOOT-07 |
| Data-pack additions to the tags | Dropped | No data packs; because the lists are computed from the registry, pack blocks need no additions | D31 (P08-1) | |

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
- [03-terrain](../03-terrain/index.md), [04-city-layout](../04-city-layout/index.md), [06-highways-railways](../06-highways-railways/index.md): damage and lighting, the building plan behind `noLoot`, and the rail dungeons.

## Proposed, not decided

None. P08-1 and P08-2 were settled by D31 and are recorded in their rows above.
