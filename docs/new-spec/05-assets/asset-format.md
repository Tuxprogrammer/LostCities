# The asset format in 1.7.10

What changes between upstream's 1.20 asset files and the files this mod loads: where they live, how names and block references are written, how matchers and tile-entity data work, and what is checked at load. Field names, defaults and per-type semantics are upstream's ([original-spec/05-assets](../../original-spec/05-assets/index.md)); this sheet lists only the changes. Rows of the [area index](index.md) link here where a cell is too small.

Status: *Accepted*. The items the [area index](index.md) once left open (P05-1 to P05-7) were settled by decision log D31 and are stated here as current rules.

## Loading and override

- **When.** Every asset type is read, parsed, resolved and validated once, at `postInit`, after every mod has registered its blocks ([CONTRIBUTING § Architecture](../../../CONTRIBUTING.md#architecture), registration order). Generation starts with a complete, immutable model; nothing compiles lazily and nothing needs a world or a running server (fixes ASSET-14).
- **Where.** The jar's bundled files first, then `config/lostcitiesnh/assets/`, whose files replace jar files with the same id and add new ones (D14). Inside the jar the tree is upstream's verbatim, `data/<namespace>/lostcities/<type>/<path>.json`, so a re-pin diffs file for file; the override folder mirrors it as `config/lostcitiesnh/assets/<namespace>/<type>/<path>.json` (P05-1, D31).
- **Ids.** A file's id is `<namespace>:<path>`, from its folder and file name, as upstream.
- **Override unit.** The whole file, as upstream. No field-level merge and no deletion.
- **No reload.** A changed override folder takes effect at the next start. The world's pinned snapshot records the asset-set hash ([0005](../../design/0005-rwg-overlay.md#7-profile-selection-q7) §7). When the loaded hash differs, one warning names the changed asset files and the world carries on; new chunks use the changed assets (P05-3, D31).
- **Pure Java.** The model lives in `lostcities.asset` and holds no Minecraft type: a block is a block reference (registry name, metadata, optional tile-entity template), checked against the live registry by the glue at load and turned into a `Block` only by the writer ([CONTRIBUTING § Architecture](../../../CONTRIBUTING.md#architecture), D6).
- **Indexes built at load.** The stuff-by-tag index is built here, each tag's list sorted by asset id (fixes ASSET-15 and SCAT-22). City-style inheritance is resolved here for every city style, root first (fixes ASSET-28).

## Names

One rule (P05-2, D31): every asset name is normalized to a full id at load.

| Written | Stored as | Example |
|---|---|---|
| Bare name | `lostcities:<name>` | `rail_dungeon1` → `lostcities:rail_dungeon1` |
| Full id | as written | `mypack:tower_top` |

Every comparison (condition tests `inpart`, `inbuilding`, `belowpart`; name matchers; selectors; references) uses the stored full id, so the two forms always match each other (fixes ASSET-33).

## Block references

Palette `block`, `damaged` and random-list entries, variant entries, block matchers and the world style's vine states all produce the same block reference.

| Form | Meaning | Status |
|---|---|---|
| `minecraft:stone_bricks`, `minecraft:oak_stairs[facing=east,half=top]` | A 1.20 name with optional properties, as converted upstream assets keep it. Looked up in the D19 mapping table, which gives `modid:name`, metadata from the properties, and the fallback chain | *Accepted* (D14, D19) |
| `Botania:stone:6`, `ProjRed\|Exploration:projectred.exploration.stone:0`, `gregtech:gt.blockframes:305`, `minecraft:mossy_cobblestone:0` | A direct 1.7.10 reference, `modid:name:meta`: the registry name exactly as registered, then `:` and the metadata, which is required, `:0` included. It is the form NEI, Hodgepodge, Et Futurum Requiem, GT, Forestry and the pack's configs write. Metadata above 15 is allowed where EndlessIDs stores it ([catalog prerequisites](block-catalog/index.md#prerequisites-for-the-asset-system) item 3) | *Accepted* (P05-4, D31) |
| `{"Name": "minecraft:vine", "Properties": {"west": "true"}}` | The world style's object form (`settings.vine*`), read into the same 1.20-name form | *Accepted* (D19) |

Rules:

- **Telling the two forms apart.** A string whose text after its last `:` is a non-negative decimal integer, with at least one `:` before it, is a direct 1.7.10 reference; any other string (one `:`, optionally followed by `[prop=value]`) is a 1.20 name and goes through the mapping table. A 1.20 id never ends in `:<digits>`, so the forms cannot collide, and because the metadata is required, a vanilla block named without it (`minecraft:mossy_cobblestone`) is always read as a 1.20 name and mapped, never guessed.
- **Registry names are kept verbatim**: `tile.` prefixes, dots, spaces, pipes and case-sensitive mod IDs ([catalog prerequisites](block-catalog/index.md#prerequisites-for-the-asset-system) item 2). A direct reference splits on its last `:`, so names containing spaces or pipes need no quoting.
- **Properties become metadata** by the [D19 rules](../00-pack-integration/block-mapping.md#blockstate-properties-to-metadata). Properties that 1.7.10 computes from neighbors are dropped.
- **Unknown names never become air.** A name that neither the mapping table nor the registry knows is reported at load with file and field; the entry takes its documented fallback, and the dev self-check fails on it (fixes ASSET-13 and CORE-20).
- **Fallback chains** may have more than one step (mod block, second mod block, vanilla), and each fallback taken is reported by the dev self-check ([block-mapping.md](../00-pack-integration/block-mapping.md#how-the-mapping-works); [catalog prerequisites](block-catalog/index.md#prerequisites-for-the-asset-system) item 7).
- **Decorative-metal entries** marked DM in the catalog resolve to their fallback while their `decorativeMetals.<metal>` flag is off in the world's pinned profile (D29).
- **Hard air.** `minecraft:structure_void` is the internal hard-air marker and is never placed.
- **1.12's `@meta` syntax is not read.** Upstream's broken `minecraft:red_sandstone@2` (ASSET-03) is neither form: it is reported at load with its file and field and takes its fallback, never air. The bundled palette is converted to the 1.20 name, `minecraft:cut_red_sandstone`, instead ([index](index.md#bundled-assets-and-blocks)).

## Matchers

| Matcher | 1.20 entries | Here |
|---|---|---|
| Block matcher (stuff `blocks`, `upperblocks`) | Block ids and block tags (`#forge:stone`) | Block references as above, and ore-dictionary names prefixed `ore:` in place of tags; `#forge:stone` converts to `ore:stone`, as the D19 mapping states. *Accepted* (D19; the `ore:` prefix P05-4, D31) |
| Biome matcher (world styles, scattered, stuff), condition `inbiome` | 1.20 biome ids and biome tags | Forge `BiomeDictionary` types, 1.7.10 biome names and RWG realistic biomes ([Biome matchers](#biome-matchers)). *Accepted* (P05-6, D31) |
| Name matcher (stuff `buildings`) | Ids, bare names qualified | Full ids, as every name ([Names](#names)) |

`if_all`, `if_any` and `excluding` keep upstream's meaning. An unknown block in a matcher is reported at load and matches nothing (fixes ASSET-13 for matchers).

### Biome matchers

This mod's syntax for a biome matcher entry (P05-6, D31). Each entry is tested against the chunk's main biome as the world's biome provider reports it, except `rwg:`, which asks RWG.

| Entry | Matches | Example |
|---|---|---|
| `type:<Type>` | A biome registered with that Forge `BiomeDictionary.Type` | `type:OCEAN`, `type:SANDY` |
| `name:<biome name>` | A biome whose `biomeName` equals the text, case-insensitively | `name:Hot Desert`, `name:Kelp Forest` |
| `rwg:<realistic biome>` | Through `compat.rwg`, the RWG realistic biome that generated the column, named by its class without the `RealisticBiome` prefix, in lower snake case; with neither RWG version loaded it matches nothing | `rwg:canyon`, `rwg:canyon_forest`, `rwg:oasis`, `rwg:red_oasis` |

Why the `rwg:` form exists: RWG's biome provider reports base biomes, and the realistic biome that shapes the terrain is not visible through them. RWG's canyon and canyon forest report the base biomes Hot Plains and Hot Forest, typed `HOT`, `SAVANNA`, `PLAINS`, `SPARSE`, and its oasis and red oasis report the base biome Oasis, which has no dictionary type, so a `type:SANDY` rule misses all four (*verified:* RWG2 [RealisticBiomeCanyon.java#L32](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/biomes/realistic/red/RealisticBiomeCanyon.java#L32), [RealisticBiomeCanyonForest.java#L34](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/biomes/realistic/savanna/RealisticBiomeCanyonForest.java#L34), [RealisticBiomeOasis.java#L38](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/biomes/realistic/desert/RealisticBiomeOasis.java#L38), [RealisticBiomeRedOasis.java#L37](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/biomes/realistic/red/RealisticBiomeRedOasis.java#L37), [BaseBiomes.java#L33-L40](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/biomes/base/BaseBiomes.java#L33-L40), [#L95-L97](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/biomes/base/BaseBiomes.java#L95-L97); classic RWG [RealisticBiomeCanyon.java#L32](https://github.com/GTNewHorizons/Realistic-World-Gen/blob/alpha-1.5.3/src/main/java/rwg/biomes/realistic/red/RealisticBiomeCanyon.java#L32), [RealisticBiomeCanyonForest.java#L34](https://github.com/GTNewHorizons/Realistic-World-Gen/blob/alpha-1.5.3/src/main/java/rwg/biomes/realistic/savanna/RealisticBiomeCanyonForest.java#L34), [RealisticBiomeOasis.java#L38](https://github.com/GTNewHorizons/Realistic-World-Gen/blob/alpha-1.5.3/src/main/java/rwg/biomes/realistic/desert/RealisticBiomeOasis.java#L38), which has no red oasis, so `rwg:red_oasis` matches only on RWG2). Classic RWG and RWG2 are both covered by the same names.

What the conversion of the bundled assets uses:

- `#is_ocean`, `#is_river` and `#is_beach` become `type:OCEAN`, `type:RIVER` and `type:BEACH`. Every RWG ocean is registered `OCEAN` and `BEACH` ([BaseBiomes.java#L73-L87](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/biomes/base/BaseBiomes.java#L73-L87)).
- `standard`'s ocean multiplier also lists `name:Kelp Forest` and `name:Coral Reef`: Biomes O' Plenty registers these ocean sub-biomes as `WATER` (Kelp Forest also `FOREST`), not `OCEAN` (*decompiled:* `BOPBiomes` of the pack's Biomes O' Plenty 2.1.0 jar, which has no public pinned source).
- The desert city style takes `type:SANDY` excluding `type:OCEAN` on the provider biome, plus `rwg:canyon`, `rwg:canyon_forest`, `rwg:oasis` and `rwg:red_oasis`. `type:MESA` is not used: no biome on RWG carries it.
- `#is_deep_ocean` (the oil rig) is not a matcher entry here. RWG2 reports vanilla Deep Ocean (ID 24) only for its cold deep ocean, while its snow, hot and wet deep oceans report the same RWG ocean base biomes as its shallow ones ([Support.java#L160-L163](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/support/Support.java#L160-L163)), and classic RWG generates no oceans, so the oil rig uses `type:OCEAN` with a height rule instead ([07-scattered](../07-scattered/index.md)).

## Tile-entity data

D28 rule 6 lets palettes carry tile-entity data, as upstream's do for chests, spawners and the furnace. The [catalog's prerequisites](block-catalog/index.md#prerequisites-for-the-asset-system) item 1 lists the blocks that need it.

- **Format.** The entry's `tag` object, as upstream, is a template of the block's 1.7.10 tile-entity NBT minus position and id.
- **Items by name.** Item stacks in a template name items by registry name; the writer resolves them to the world's numeric item IDs when it places the block, never in the asset ([block-mapping.md § How the mapping works](../00-pack-integration/block-mapping.md#how-the-mapping-works), [loot-and-mobs.md § Other block-entity data](../00-pack-integration/loot-and-mobs.md#other-block-entity-data)).
- **Facing.** A facing kept in the tile entity (BiblioCraft, Storage Drawers, Iron Chests, JABBA, FloodLights and others) is turned with the part's transform, as metadata facings are.
- **With loot and mobs.** Upstream uses only the first of torch, loot, mob and `tag`. Here a template may accompany `loot` or `mob`, so a modded container with a stored facing can still hold loot; the loot or mob step runs after the template is applied.
- **When.** Tile entities are created in the population phase, never in the terrain phase ([0005](../../design/0005-rwg-overlay.md#2-terrain-phase) §2, §3). Blocks whose tile entity `setBlock` creates correctly by itself (MalisisDoors doors) take no template, and the writer does not assume any block is tile-entity free.
- **Never placed.** A template cannot make an unbreakable or never-placed block acceptable: command blocks and every block on the [never-place list](block-catalog/index.md#never-placed) are rejected at load (D28 rule 7, D29).

## Checks at load

Every check reports the file, the field and the value. In the dev environment a failure fails the dev self-check; in a real pack the asset is skipped with one log line ([CONTRIBUTING § Code standards](../../../CONTRIBUTING.md#code-standards)). `AssetValidationTest` runs the same checks on the bundled set ([CONTRIBUTING § Testing](../../../CONTRIBUTING.md#testing)).

| Check | Fixes |
|---|---|
| Every name an asset gives resolves; required references reject the asset, optional ones warn once and act as absent | ASSET-01 |
| Every block reference resolves or has a fallback; none becomes air | ASSET-13, CORE-20 |
| Every single-character field (`char`, `filler`, `rubble`, city-style characters, part meta `char`) is exactly one character | ASSET-23 |
| Every slice has `zsize` rows of `xsize` characters | ASSET-24 |
| Every enum value (scattered `terrainheight`, `terrainfix`; sphere settings when spheres return) is known, case-insensitive with a fixed locale | ASSET-25, CITY-32 |
| Random lists fill 128 slots; palette groups and street, highway and railway part lists are not empty | ASSET-08, ASSET-32 |
| A multibuilding's `dimx` and `dimz` match its grid | ASSET-31 |
| City-style parents exist and form no cycle | ASSET-28 |
| Stuff: `maxcount >= mincount` (equal means exactly `mincount`), and `maxheight > minheight` when both are given | ASSET-21, SCAT-17, SCAT-18 |
| A scattered list's total weight plus `weightnone` is above 0 | SCAT-06 |
| `range` tests parse | (upstream already throws at load) |
| No entry names a never-placed block or a GT ore host | D18, D28 rule 7, D29 |
| Dead fields (`streetblocks.width`, `streetvariant`) warn that they have no effect | ASSET-22, ASSET-27 |
| Reachability: the dev self-check lists assets nothing names (upstream's `stuff/example` and two palettes) | ASSET-22 |

## Writing assets

The editor and the part-export commands are ported (D13) and write JSON. Any writer writes every field's real value, and a round-trip test (parse, write, parse) covers each asset type (fixes ASSET-19 and CITY-31).
