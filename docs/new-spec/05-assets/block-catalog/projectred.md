# Block catalog: ProjectRed

ProjectRed ships as one jar, `ProjRed-4.12.50-GTNH.jar`, with one mod id per module; the blocks here come from `ProjRed|Exploration` and `ProjRed|Illumination`. Source read at tag `4.12.50-GTNH` (`gtnh-src/source/ProjectRed`, Scala: `exploration/blocks.scala`, `exploration/proxies.scala`, `illumination/blocks.scala`, `illumination/lightobj.scala`, `illumination/proxies.scala`). Pack config checked: `config/ProjectRed.cfg`: the Exploration module is on, but **every Exploration world generator is off except the Deviating Lily** (ores, marble caves, volcanoes and moss spreading all `false`), so PR marble and basalt exist only as crafted blocks. Sheets: [`stones`](../../../../tools/block-contact-sheet/sheets/projectred/stones.json), [`lamps`](../../../../tools/block-contact-sheet/sheets/projectred/lamps.json) under `tools/block-contact-sheet/sheets/projectred/`. This catalog serves D25 in the [decision log](../../../design/decision-log.md): variety for palettes, with the [D19 mapping](../../00-pack-integration/block-mapping.md) as baseline.

Blocks register under the module's mod id with their dotted names: `ProjRed|Exploration:projectred.exploration.stone`, `ProjRed|Illumination:projectred.illumination.lamp`. The small lights are ForgeMultipart parts, which live inside `ForgeMultipart:block` tile entities, not in blocks of their own.

## Summary

- **46 entries**: 42 block+meta entries in 5 registry blocks, plus 4 multipart light kinds (each in 16 colors, normal or inverted).
- Fit upstream's look: 13 `yes`, 24 `partly`, 9 `no`.
- Not GT ore hosts or boulder hosts ([ore-veins.md](../../00-pack-integration/ore-veins.md)). PR marble and basalt share names with GT's stones but are different blocks: neither GT ores nor GT boulders replace them, so they are host-safe stand-ins for the GT stones.

| Primary role | Entries |
|---|---|
| wall-main | 4 (marble, marble brick, basalt, basalt brick) |
| rubble-ruin | 1 (basalt cobblestone) |
| fence-rail | 12 (walls) |
| industrial-dressing | 3 (copper, tin, silver blocks) |
| light | 20 (16 lamp colors, 4 multipart kinds) |
| furniture-deco | 1 (item barrel) |
| foliage | 1 (deviating lily) |
| off-theme | 4 (ruby, sapphire, peridot, electrotine blocks) |

Flags:

- **Lamps are tile entities, and the lit state lives in the tile entity.** `projectred.illumination.lamp` metadata is the color; `TileLamp` stores `inv` (inverted) and `pow` (powered) in NBT, and the block's light comes from the tile (`BlockLamp.getLightValue`). A lamp placed by metadata alone is an unlit, non-inverted lamp. A permanently lit lamp needs the tile's NBT `inv=true` written at placement. Unlit lamps work as dead fixtures in ruins.
- **Multipart lights** (lantern, fixture, fallout light, cage lamp) are ForgeMultipart parts: `ForgeMultipart:block` plus a tile entity holding the part list. Placing them in worldgen means writing multipart NBT; costly and fragile.
- **Item barrel** is a tile entity; the Deviating Lily is a tile entity (`TileLily`).
- **Light:** lamps 15 when lit; multipart lights 15 when lit, with a colored halo.
- **Hardness:** marble 1, basalt and basalt cobble and brick 2.5, gem and metal blocks 5; resistance 10 to 20. Nothing painful.
- **Gravity:** none.

## Decorative stones and walls

`ProjRed|Exploration:projectred.exploration.stone` metas 0 to 11 and `ProjRed|Exploration:projectred.exploration.stonewalls` metas 0 to 11 (walls use the same faces, cobblestone-wall model).

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `ProjRed\|Exploration:projectred.exploration.stone:0` | Marble | wall-main, floor | yes: off-white polished stone, a stand-in for upstream's polished diorite | urban | Hardness 1 |
| `ProjRed\|Exploration:projectred.exploration.stone:1` | Marble Brick | wall-main, wall-accent | yes: cream bricks with dark joints | urban | |
| `ProjRed\|Exploration:projectred.exploration.stone:2` | Basalt Cobblestone | rubble-ruin | partly: dark cobble | ruined | |
| `ProjRed\|Exploration:projectred.exploration.stone:3` | Basalt | wall-main, floor | yes: dark smooth stone | urban, industrial | |
| `ProjRed\|Exploration:projectred.exploration.stone:4` | Basalt Brick | wall-main, wall-accent | yes: dark gray bricks, a deepslate-brick alternative | urban, industrial | |
| `ProjRed\|Exploration:projectred.exploration.stone:5` | Block of Ruby | off-theme | no | none | Gem value |
| `ProjRed\|Exploration:projectred.exploration.stone:6` | Block of Sapphire | off-theme | no | none | Gem value |
| `ProjRed\|Exploration:projectred.exploration.stone:7` | Block of Peridot | off-theme | no | none | Gem value |
| `ProjRed\|Exploration:projectred.exploration.stone:8` | Block of Copper | industrial-dressing | partly: copper plate | industrial | Resource value |
| `ProjRed\|Exploration:projectred.exploration.stone:9` | Block of Tin | industrial-dressing | partly: white metal plate | industrial | |
| `ProjRed\|Exploration:projectred.exploration.stone:10` | Block of Silver | industrial-dressing | partly: pale blue plate | industrial | |
| `ProjRed\|Exploration:projectred.exploration.stone:11` | Block of Electrotine | off-theme | no: blue mottled | none | |
| `ProjRed\|Exploration:projectred.exploration.stonewalls:0` | Marble Wall | fence-rail | yes: a pale `cobblestone_wall` (`w`) alternative | urban | |
| `ProjRed\|Exploration:projectred.exploration.stonewalls:1` | Marble Brick Wall | fence-rail | yes | urban | |
| `ProjRed\|Exploration:projectred.exploration.stonewalls:2` | Basalt Cobblestone Wall | fence-rail | yes: dark cobble wall | ruined, urban | |
| `ProjRed\|Exploration:projectred.exploration.stonewalls:3` | Basalt Wall | fence-rail | yes | urban, industrial | |
| `ProjRed\|Exploration:projectred.exploration.stonewalls:4` | Basalt Brick Wall | fence-rail | yes | urban, industrial | |
| `ProjRed\|Exploration:projectred.exploration.stonewalls:5` | Ruby Wall | fence-rail, off-theme | no | none | |
| `ProjRed\|Exploration:projectred.exploration.stonewalls:6` | Sapphire Wall | fence-rail | no | none | |
| `ProjRed\|Exploration:projectred.exploration.stonewalls:7` | Peridot Wall | fence-rail | no | none | |
| `ProjRed\|Exploration:projectred.exploration.stonewalls:8` | Copper Wall | fence-rail | partly | industrial | |
| `ProjRed\|Exploration:projectred.exploration.stonewalls:9` | Tin Wall | fence-rail | partly | industrial | |
| `ProjRed\|Exploration:projectred.exploration.stonewalls:10` | Silver Wall | fence-rail | partly | industrial | |
| `ProjRed\|Exploration:projectred.exploration.stonewalls:11` | Electrotine Wall | fence-rail | no | none | |
| `ProjRed\|Exploration:projectred.exploration.barrel:0` | Item Barrel | furniture-deco | yes: banded wooden barrel, an alternative for the oil rig's barrels | urban, industrial | TE |
| `ProjRed\|Exploration:projectred.exploration.lily:0` | Deviating Lily | foliage | no: colored magic flower | none | TE; the one PR world generator the pack leaves on |

## Lamps and lights

`ProjRed|Illumination:projectred.illumination.lamp` metas 0 to 15 (dye order). Each color has two looks, off and lit; the sheet shows both. The colors come from per-color textures, not tints. Each row counts as 1 entry.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `ProjRed\|Illumination:projectred.illumination.lamp:0` | White Lamp | light | yes: framed white light panel, a glowstone (`h`) alternative | urban, industrial | TE; lit needs `inv=true` |
| `…lamp:1` | Orange Lamp | light | partly: colored | urban | TE |
| `…lamp:2` | Magenta Lamp | light | partly | urban | TE |
| `…lamp:3` | Light Blue Lamp | light | partly | urban | TE |
| `…lamp:4` | Yellow Lamp | light | yes: warm light | urban | TE |
| `…lamp:5` | Lime Lamp | light | partly | urban | TE |
| `…lamp:6` | Pink Lamp | light | partly | urban | TE |
| `…lamp:7` | Gray Lamp | light | partly: dim when lit | urban | TE |
| `…lamp:8` | Light Gray Lamp | light | yes: neutral light | urban, industrial | TE |
| `…lamp:9` | Cyan Lamp | light | partly | urban | TE |
| `…lamp:10` | Purple Lamp | light | partly | urban | TE |
| `…lamp:11` | Blue Lamp | light | partly | urban | TE |
| `…lamp:12` | Brown Lamp | light | partly | urban | TE |
| `…lamp:13` | Green Lamp | light | partly | urban | TE |
| `…lamp:14` | Red Lamp | light | partly: warning light | urban, industrial | TE |
| `…lamp:15` | Black Lamp | light | partly: barely glows | urban | TE |
| `ForgeMultipart:block` part `pr_lantern` | Lantern (16 colors, normal or inverted) | light | partly: hanging or standing lantern | urban | Multipart TE |
| `ForgeMultipart:block` part `pr_fixture` | Fixture (16 colors, normal or inverted) | light, ceiling | partly: small ceiling or wall fitting | urban, industrial | Multipart TE |
| `ForgeMultipart:block` part `pr_cagelamp` | Fallout Light (16 colors, normal or inverted) | light | partly: caged bulkhead light | industrial | Multipart TE; texture `fallout.png` |
| `ForgeMultipart:block` part `pr_cagelamp2` | Cage Lamp (16 colors, normal or inverted) | light | partly: caged lamp | industrial | Multipart TE; texture `cage_lamp.png` |

The multipart textures are UV atlases; the sheet shows them for reference only.

## Best variety pools

- **Marble and quartz palettes (`quartzbricks_border`, oil rig's polished diorite `D`):** Marble and Marble Brick, with Marble Wall for the oil rig's diorite wall `w`.
- **Dark brick buildings (deepslate and blackstone variants):** Basalt Brick and Basalt, Basalt Cobblestone for their rubble.
- **Walls `w`:** Marble, Basalt and Basalt Cobblestone walls next to `cobblestone_wall`.
- **Light `h`:** white, light gray and yellow lamps, lit through tile-entity NBT; any color unlit in ruins.
- **Industrial style:** caged lamps only if multipart placement is ever supported. The copper, tin and silver blocks are capped accents behind their D29 metal flags ([D29](index.md#decorative-metals-d29)).
