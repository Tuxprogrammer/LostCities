# The asset format in 1.7.10

What changes between upstream's 1.20 asset files and the files this mod loads: where they live, how names and block references are written, how matchers and tile-entity data work, and what is checked at load. Field names, defaults and per-type semantics are upstream's ([original-spec/05-assets](../../original-spec/05-assets/index.md)); this sheet lists only the changes. Rows of the [area index](index.md) link here where a cell is too small.

Status: *Accepted* where a decision is cited; items marked *Proposed* carry the recommendation from the index's [Proposed, not decided](index.md#proposed-not-decided) section.

## Loading and override

- **When.** Every asset type is read, parsed, resolved and validated once, at `postInit`, after every mod has registered its blocks ([CONTRIBUTING § Architecture](../../../CONTRIBUTING.md#architecture), registration order). Generation starts with a complete, immutable model; nothing compiles lazily and nothing needs a world or a running server (fixes ASSET-14).
- **Where.** The jar's bundled files first, then `config/lostcitiesnh/assets/`, whose files replace jar files with the same id and add new ones (D14). The folder tree inside both is *Proposed* ([P05-1](index.md#p05-1-folder-layout)).
- **Ids.** A file's id is `<namespace>:<path>`, from its folder and file name, as upstream.
- **Override unit.** The whole file, as upstream. No field-level merge and no deletion.
- **No reload.** A changed override folder takes effect at the next start. The world's pinned snapshot records the asset-set hash ([0005](../../design/0005-rwg-overlay.md#7-profile-selection-q7) §7); a warning on a changed hash is *Proposed* ([P05-3](index.md#p05-3-asset-set-hash-warning)).
- **Pure Java.** The model lives in `lostcities.asset` and holds no Minecraft type: a block is a block reference (registry name, metadata, optional tile-entity template), checked against the live registry by the glue at load and turned into a `Block` only by the writer ([CONTRIBUTING § Architecture](../../../CONTRIBUTING.md#architecture), D6).
- **Indexes built at load.** The stuff-by-tag index is built here, each tag's list sorted by asset id (fixes ASSET-15 and SCAT-22). City-style inheritance is resolved here for every city style, root first (fixes ASSET-28).

## Names

One rule, *Proposed* ([P05-2](index.md#p05-2-one-name-form)):

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
| `Botania:stone@6`, `ProjRed\|Exploration:projectred.exploration.stone@0`, `gregtech:gt.blockframes@305` | A direct 1.7.10 reference: the registry name exactly as registered, then `@` and the metadata (0 if omitted). Metadata above 15 is allowed where EndlessIDs stores it ([catalog prerequisites](block-catalog/index.md#prerequisites-for-the-asset-system) item 3) | *Proposed* ([P05-4](index.md#p05-4-native-block-references)) |
| `{"Name": "minecraft:vine", "Properties": {"west": "true"}}` | The world style's object form (`settings.vine*`), read into the same 1.20-name form | *Accepted* (D19) |

Rules:

- **Registry names are kept verbatim**: `tile.` prefixes, dots, spaces, pipes and case-sensitive mod IDs ([catalog prerequisites](block-catalog/index.md#prerequisites-for-the-asset-system) item 2). The `@` form splits on the last `@`, so names containing spaces or pipes need no quoting.
- **Properties become metadata** by the [D19 rules](../00-pack-integration/block-mapping.md#blockstate-properties-to-metadata). Properties that 1.7.10 computes from neighbors are dropped.
- **Unknown names never become air.** A name that neither the mapping table nor the registry knows is reported at load with file and field; the entry takes its documented fallback, and the dev self-check fails on it (fixes ASSET-13 and CORE-20).
- **Fallback chains** may have more than one step (mod block, second mod block, vanilla), and each fallback taken is reported by the dev self-check ([block-mapping.md](../00-pack-integration/block-mapping.md#how-the-mapping-works); [catalog prerequisites](block-catalog/index.md#prerequisites-for-the-asset-system) item 7).
- **Decorative-metal entries** marked DM in the catalog resolve to their fallback while their `decorativeMetals.<metal>` flag is off in the world's pinned profile (D29).
- **Hard air.** `minecraft:structure_void` is the internal hard-air marker and is never placed.
- **1.12's `@meta` on a 1.20 name** (upstream's broken `minecraft:red_sandstone@2`, ASSET-03) is a 1.7.10 reference under this syntax; `minecraft:red_sandstone` is not a 1.7.10 block, so it is reported at load. The bundled palette is converted to the 1.20 name instead ([index](index.md#bundled-assets-and-blocks)).

## Matchers

| Matcher | 1.20 entries | Here |
|---|---|---|
| Block matcher (stuff `blocks`, `upperblocks`) | Block ids and block tags (`#forge:stone`) | Block references as above, and ore-dictionary names prefixed `ore:` in place of tags; `#forge:stone` converts to `ore:stone`, as the D19 mapping states. *Accepted* for the conversion (D19); the `ore:` prefix is part of P05-4 |
| Biome matcher (world styles, scattered, stuff), condition `inbiome` | 1.20 biome ids and biome tags | Forge `BiomeDictionary` types (`type:OCEAN`) and 1.7.10 biome names (`name:Hot Desert`). *Proposed* ([P05-6](index.md#p05-6-biome-vocabulary)) |
| Name matcher (stuff `buildings`) | Ids, bare names qualified | Full ids, as every name ([Names](#names)) |

`if_all`, `if_any` and `excluding` keep upstream's meaning. An unknown block in a matcher is reported at load and matches nothing (fixes ASSET-13 for matchers).

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
