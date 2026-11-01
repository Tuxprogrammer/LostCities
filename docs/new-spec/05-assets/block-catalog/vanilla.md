# Block catalog: vanilla Minecraft 1.7.10

Every building-relevant block of Minecraft 1.7.10 as GTNH 2.9.0 runs it. Registry names are checked against the block registry in the decompiled 1.7.10 server (`gtnh-src/minecraft/net/minecraft/block/Block.java`); textures come from the 1.7.10 client jar. Metadata follows the pre-flattening data values that [block-mapping.md](../../00-pack-integration/block-mapping.md) uses. Part of the decorative block catalog (decision log D25). Contact sheets: `tools/block-contact-sheet/sheets/vanilla/*.json`, rendered to `docs/previews/blocks/vanilla/`.

## Summary

- **Catalogued: 274 rows** covering the building-relevant ones among the 171 blocks 1.7.10 registers; every block that can appear in a building, street, park or ruin, one row per meta that changes the look (stairs, doors, signs, trapdoors and ladders take one row for their orientation metas). All of vanilla is enabled; no pack config removes a vanilla block.
- **Upstream uses about 80 of these blocks directly** ([block-inventory.md](../../../original-spec/05-assets/block-inventory.md)); the notes mark the ones upstream's palettes name.
- **Fit with upstream's look: 159 yes, 79 partly, 36 no.**
- **Excluded, with the reason:**
- **Technical blocks**: `air`, `water`/`flowing_water`, `lava`/`flowing_lava`, `fire`, `portal`, `end_portal`, `piston_head`, `piston_extension`, `redstone_wire`, `tripwire`, `unlit_redstone_torch`, `lit_furnace`, `powered_repeater`/`unpowered_repeater`, `powered_comparator`/`unpowered_comparator` (redstone parts with no building use), `double_wooden_slab` (listed under its slab), `reeds`/`cake` item forms, `pumpkin_stem`, `melon_stem`, `cocoa`, `nether_wart` (crop), `tripwire_hook`, `snow_layer` (kept as a note: thin snow cover).
- **Never placed**: `bedrock` (unbreakable), `monster_egg` (silverfish, D19), all ores (`coal_ore`, `iron_ore`, `gold_ore`, `diamond_ore`, `emerald_ore`, `lapis_ore`, `redstone_ore`, `lit_redstone_ore`, `quartz_ore`).
- **GT hosts.** Ore hosts: `stone` (meta 0), `netherrack`, `end_stone`. Boulder hosts: those three plus `cobblestone` and **all 16 `stained_hardened_clay` colors**. Every other vanilla block is safe, including `mossy_cobblestone`, `cobblestone_wall`, `stone_stairs`, `stone_slab:3`, `hardened_clay` and every double slab ([ore-veins.md](../../00-pack-integration/ore-veins.md)).
- **Gameplay flags.** Light: glowstone, lit pumpkin, beacon 15; torch 14; lit redstone lamp 15 (needs power); redstone torch 7; ender chest 7; brewing stand 1. Gravity: sand, red sand, gravel, anvil, dragon egg. Tile entities: chests, furnace, brewing stand, enchanting table, jukebox, note block, signs, flower pot, mob head, spawner, hopper, dispenser, dropper, daylight sensor, beacon. Transparency: glass, stained glass, panes, iron bars, leaves, ice. Random ticks: leaves decay without the no-decay bit, vines spread, grass spreads, ice and snow melt, crops grow. No connected textures in 1.7.10 vanilla. Tinted textures (grass, leaves, vines, tall grass) render gray on the sheets.

Blocks per role (a block can hold several roles):

| Role | Blocks+meta |
|---|---|
| `wall-main` | 38 |
| `wall-accent` | 43 |
| `trim-pillar` | 11 |
| `floor` | 41 |
| `roof` | 9 |
| `glass` | 17 |
| `pane` | 18 |
| `street` | 5 |
| `sidewalk` | 2 |
| `stairs-slab` | 26 |
| `rubble-ruin` | 17 |
| `light` | 6 |
| `fence-rail` | 7 |
| `door-trap` | 4 |
| `furniture-deco` | 53 |
| `foliage` | 55 |
| `special` | 14 |
| `off-theme` | 29 |

### Where Et Futurum's off switches leave vanilla as the only option

- **Doors**: only oak and iron doors exist apart from Et Futurum's copper doors (`enableVanillaDoors=false`).
- **Fences and gates**: only the oak fence, oak fence gate and nether brick fence (`enableVanillaFences=false`, `enableVanillaGates=false`).
- **Trapdoors**: the wooden trapdoor, plus Et Futurum's copper and cherry trapdoors (`enableIronTrapdoor=false`, `enableVanillaTrapdoors=false`).
- **Beds**: red is vanilla's `minecraft:bed`; Et Futurum adds the other 15 colors.
- **Polished stones**: 1.7.10 has no granite, diorite or andesite; Botania and Chisel fill them ([etfuturum.md](etfuturum.md)).

## Blocks

### Stone, masonry and solid blocks

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `minecraft:stone:0` | Stone | `off-theme` | no | - | GT ore and boulder host; only the city foundation fill (profile `baseBlock`), never a building block |
| `minecraft:cobblestone:0` | Cobblestone | `rubble-ruin`, `wall-main` | partly | standard, new: old town | GT boulder host (not an ore host); upstream's `stoneandbrick` rubble; prefer `mossy_cobblestone` or walls in buildings |
| `minecraft:mossy_cobblestone:0` | Moss Stone | `rubble-ruin`, `wall-accent` | yes | standard, new: old town | a different block from cobblestone, so not a boulder host; ruins and overgrowth |
| `minecraft:stonebrick:0` | Stone Bricks | `wall-main`, `wall-accent`, `floor` | yes | standard, border | upstream's `stone_bricks` and the main `#` of `bricks_standard`; stairs `stone_brick_stairs`, slab `stone_slab:5` |
| `minecraft:stonebrick:1` | Mossy Stone Bricks | `wall-main`, `rubble-ruin` | yes | standard, border | upstream uses it |
| `minecraft:stonebrick:2` | Cracked Stone Bricks | `rubble-ruin`, `wall-main` | yes | standard, border | upstream uses it |
| `minecraft:stonebrick:3` | Chiseled Stone Bricks | `wall-accent`, `trim-pillar` | yes | standard, border |  |
| `minecraft:brick_block:0` | Bricks | `wall-main`, `wall-accent` | yes | standard, border | upstream's `bricks` (`B`); stairs `brick_stairs`, slab `stone_slab:4` |
| `minecraft:sandstone:0` | Sandstone | `wall-main`, `rubble-ruin` | yes | desert | upstream's `sandstone`; stairs `sandstone_stairs`, slab `stone_slab:1` |
| `minecraft:sandstone:1` | Chiseled Sandstone | `wall-accent`, `trim-pillar` | yes | desert |  |
| `minecraft:sandstone:2` | Smooth Sandstone | `wall-main`, `floor` | yes | desert | upstream's `cut_sandstone`, the main `#` of `bricks_desert` |
| `minecraft:nether_brick:0` | Nether Brick | `wall-main`, `wall-accent` | partly | standard, new: old town | dark purple-brown brick; stairs `nether_brick_stairs`, slab `stone_slab:6`, fence `nether_brick_fence` |
| `minecraft:quartz_block:0` | Block of Quartz | `wall-main`, `trim-pillar`, `floor` | yes | standard, border | upstream's `quartz_block` (`Q`); stairs `quartz_stairs`, slab `stone_slab:7` |
| `minecraft:quartz_block:1` | Chiseled Quartz Block | `wall-accent`, `trim-pillar` | yes | standard, border |  |
| `minecraft:quartz_block:2` | Pillar Quartz Block | `trim-pillar` | yes | standard, border | meta 2 vertical, 3 north-south, 4 east-west |
| `minecraft:obsidian:0` | Obsidian | `wall-accent` | partly | standard | upstream uses it; hardness 50 |
| `minecraft:end_stone:0` | End Stone | `off-theme` | no | - | GT ore and boulder host |
| `minecraft:netherrack:0` | Netherrack | `off-theme` | no | - | GT ore and boulder host; burns forever when lit |
| `minecraft:double_stone_slab:8` | Smooth Stone (double slab) | `wall-main`, `floor`, `street`, `sidewalk` | yes | standard, border | upstream's `smooth_stone_slab[type=double]` (`S`, `u`, `$`); the D19 fallback for smooth stone |
| `minecraft:double_stone_slab:9` | Smooth Sandstone (double slab) | `wall-main`, `floor`, `street` | yes | desert | sandstone-top on all faces |
| `minecraft:double_stone_slab:0` | Smooth Stone Double Slab | `wall-main`, `floor`, `street` | yes | standard, border | the smooth-stone double slab with the banded side; a full block that is not raw stone |
| `minecraft:double_stone_slab:1` | Sandstone Double Slab | `wall-main`, `floor` | yes | desert |  |
| `minecraft:double_stone_slab:2` | Wooden Double Slab (old) | `wall-main`, `floor` | partly | standard, border | a stone-material block that looks like oak planks; avoid |
| `minecraft:double_stone_slab:3` | Cobblestone Double Slab | `wall-main`, `floor` | yes | standard, border |  |
| `minecraft:double_stone_slab:4` | Brick Double Slab | `wall-main`, `floor` | yes | standard, border |  |
| `minecraft:double_stone_slab:5` | Stone Brick Double Slab | `wall-main`, `floor` | yes | standard, border |  |
| `minecraft:double_stone_slab:6` | Nether Brick Double Slab | `wall-main`, `floor` | yes | standard, border |  |
| `minecraft:double_stone_slab:7` | Quartz Double Slab | `wall-main`, `floor` | yes | standard, border |  |
| `minecraft:hardened_clay:0` | Hardened Clay | `wall-main`, `wall-accent` | yes | desert, standard | plain terracotta; a different block from stained clay, so not a boulder host; D19's fallback for exposed copper |
| `minecraft:clay:0` | Clay | `off-theme` | no | - | soft natural block |
| `minecraft:glowstone:0` | Glowstone | `light` | yes | standard, border | upstream's `glowstone` (`h`); light 15 |
| `minecraft:redstone_lamp:0` | Redstone Lamp (off) | `light`, `wall-accent` | yes | standard, new: industrial | unlit; `lit_redstone_lamp` is light 15 but turns off a tick later without power, so it needs a power source beside it |
| `minecraft:lit_redstone_lamp:0` | Redstone Lamp (lit) | `light` | partly | standard, new: industrial | light 15; reverts to the unlit block on the next block update unless powered (place a redstone block or lit torch next to it) |
| `minecraft:iron_block:0` | Block of Iron | `special`, `wall-accent` | partly | standard | upstream's `!` treasure; plain iron also reads as sheet steel cladding |
| `minecraft:gold_block:0` | Block of Gold | `special` | no | - | upstream's `!` treasure |
| `minecraft:diamond_block:0` | Block of Diamond | `special` | no | - | upstream's `!` treasure |
| `minecraft:emerald_block:0` | Block of Emerald | `special` | no | - | upstream's `!` treasure |
| `minecraft:lapis_block:0` | Lapis Lazuli Block | `special` | no | - | valuable |
| `minecraft:redstone_block:0` | Block of Redstone | `special` | no | - | valuable; powers neighbors |
| `minecraft:coal_block:0` | Block of Coal | `special` | no | - | burns forever when lit |
| `minecraft:snow:0` | Snow | `off-theme` | no | - | melts near light 12 |
| `minecraft:ice:0` | Ice | `off-theme` | no | - | melts; slippery; translucent |
| `minecraft:packed_ice:0` | Packed Ice | `off-theme` | no | - | slippery; does not melt |
| `minecraft:hay_block:0` | Hay Bale | `furniture-deco` | partly | park, new: old town | farm and market detail; axis in meta |
| `minecraft:bookshelf:0` | Bookshelf | `furniture-deco`, `wall-accent` | yes | standard, border | upstream's `bookshelf` (`L`) |
| `minecraft:sponge:0` | Sponge | `off-theme` | no | - |  |
| `minecraft:pumpkin:0` | Pumpkin | `foliage` | partly | park | meta = facing |
| `minecraft:lit_pumpkin:0` | Jack o'Lantern | `light` | partly | park | light 15; meta = facing |
| `minecraft:melon_block:0` | Melon | `foliage` | no | park |  |

### Stained clay (terracotta)

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `minecraft:stained_hardened_clay:0` | White Stained Clay | `wall-main`, `wall-accent` | yes | desert, standard | GT boulder host (all 16 colors, accepted for alpha, D18) |
| `minecraft:stained_hardened_clay:1` | Orange Stained Clay | `wall-main`, `wall-accent` | yes | desert, standard | upstream's `orange_terracotta`; GT boulder host |
| `minecraft:stained_hardened_clay:2` | Magenta Stained Clay | `wall-accent` | partly | standard, border |  |
| `minecraft:stained_hardened_clay:3` | Light Blue Stained Clay | `wall-main`, `wall-accent` | yes | standard, border |  |
| `minecraft:stained_hardened_clay:4` | Yellow Stained Clay | `wall-main`, `wall-accent` | yes | desert, standard |  |
| `minecraft:stained_hardened_clay:5` | Lime Stained Clay | `wall-accent` | partly | standard, border |  |
| `minecraft:stained_hardened_clay:6` | Pink Stained Clay | `wall-accent` | partly | standard, border |  |
| `minecraft:stained_hardened_clay:7` | Gray Stained Clay | `wall-main`, `wall-accent` | yes | standard, border | upstream's `gray_terracotta`; GT boulder host |
| `minecraft:stained_hardened_clay:8` | Light Gray Stained Clay | `wall-main`, `wall-accent` | yes | standard, border | upstream's `light_gray_terracotta`; GT boulder host |
| `minecraft:stained_hardened_clay:9` | Cyan Stained Clay | `wall-main`, `wall-accent` | yes | standard, border | upstream's `cyan_terracotta`; GT boulder host |
| `minecraft:stained_hardened_clay:10` | Purple Stained Clay | `wall-accent` | partly | standard, border |  |
| `minecraft:stained_hardened_clay:11` | Blue Stained Clay | `wall-main`, `wall-accent` | yes | standard, border |  |
| `minecraft:stained_hardened_clay:12` | Brown Stained Clay | `wall-main`, `wall-accent` | yes | desert, standard | upstream's `brown_terracotta`; GT boulder host |
| `minecraft:stained_hardened_clay:13` | Green Stained Clay | `wall-main`, `wall-accent` | yes | standard, border |  |
| `minecraft:stained_hardened_clay:14` | Red Stained Clay | `wall-main`, `wall-accent` | yes | desert, standard | upstream's `red_terracotta`; GT boulder host |
| `minecraft:stained_hardened_clay:15` | Black Stained Clay | `wall-main`, `wall-accent` | yes | standard, border | upstream's `black_terracotta`; GT boulder host |

### Planks and logs

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `minecraft:planks:0` | Oak Planks | `floor`, `wall-main`, `wall-accent` | yes | standard, new: old town | upstream's `oak_planks`; stairs `oak_stairs`, slab `wooden_slab:0` |
| `minecraft:planks:1` | Spruce Planks | `floor`, `wall-main`, `wall-accent` | yes | standard, new: old town | stairs `spruce_stairs`, slab `wooden_slab:1` |
| `minecraft:planks:2` | Birch Planks | `floor`, `wall-main`, `wall-accent` | yes | standard, new: old town | stairs `birch_stairs`, slab `wooden_slab:2` |
| `minecraft:planks:3` | Jungle Planks | `floor`, `wall-main`, `wall-accent` | partly | standard, new: old town | stairs `jungle_stairs`, slab `wooden_slab:3` |
| `minecraft:planks:4` | Acacia Planks | `floor`, `wall-main`, `wall-accent` | partly | standard, new: old town | stairs `acacia_stairs`, slab `wooden_slab:4` |
| `minecraft:planks:5` | Dark Oak Planks | `floor`, `wall-main`, `wall-accent` | yes | standard, new: old town | stairs `dark_oak_stairs`, slab `wooden_slab:5` |
| `minecraft:log:0` | Oak Log | `trim-pillar`, `foliage` | partly | standard, new: old town, park | axis in meta (+4 X, +8 Z, +12 bark on all faces) |
| `minecraft:log:1` | Spruce Log | `trim-pillar`, `foliage` | partly | standard, new: old town, park |  |
| `minecraft:log:2` | Birch Log | `trim-pillar`, `foliage` | partly | standard, new: old town, park |  |
| `minecraft:log:3` | Jungle Log | `trim-pillar`, `foliage` | partly | standard, new: old town, park |  |
| `minecraft:log2:0` | Acacia Log | `trim-pillar`, `foliage` | partly | standard, new: old town, park |  |
| `minecraft:log2:1` | Dark Oak Log | `trim-pillar`, `foliage` | partly | standard, new: old town, park |  |

### Slabs, stairs, walls, fences, bars, doors

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `minecraft:stone_slab:0` | Smooth Stone Slab | `stairs-slab`, `roof`, `sidewalk` | yes | standard, border | upstream's `smooth_stone_slab` (`_`, `=`); +8 top half |
| `minecraft:stone_slab:1` | Sandstone Slab | `stairs-slab` | yes | desert |  |
| `minecraft:stone_slab:2` | Wooden Slab (old, stone material) | `stairs-slab` | partly | standard | use `wooden_slab:0` instead |
| `minecraft:stone_slab:3` | Cobblestone Slab | `stairs-slab` | partly | standard, new: old town | a slab block, not a boulder host |
| `minecraft:stone_slab:4` | Brick Slab | `stairs-slab` | yes | standard, border | upstream's `brick_slab` |
| `minecraft:stone_slab:5` | Stone Brick Slab | `stairs-slab` | yes | standard, border | upstream's `stone_brick_slab` |
| `minecraft:stone_slab:6` | Nether Brick Slab | `stairs-slab` | partly | standard, new: old town |  |
| `minecraft:stone_slab:7` | Quartz Slab | `stairs-slab` | yes | standard, border | upstream's `quartz_slab` |
| `minecraft:wooden_slab:0` | Oak Slab | `stairs-slab`, `floor` | yes | standard, new: old town | +8 top; double form `double_wooden_slab:0` |
| `minecraft:wooden_slab:1` | Spruce Slab | `stairs-slab`, `floor` | yes | standard, new: old town |  |
| `minecraft:wooden_slab:2` | Birch Slab | `stairs-slab`, `floor` | yes | standard, new: old town |  |
| `minecraft:wooden_slab:3` | Jungle Slab | `stairs-slab`, `floor` | yes | standard, new: old town |  |
| `minecraft:wooden_slab:4` | Acacia Slab | `stairs-slab`, `floor` | yes | standard, new: old town |  |
| `minecraft:wooden_slab:5` | Dark Oak Slab | `stairs-slab`, `floor` | yes | standard, new: old town |  |
| `minecraft:oak_stairs:0-7` | Oak Stairs | `stairs-slab`, `roof` | yes | standard, new: old town | pitched roofs and porches; stair meta as D19 |
| `minecraft:spruce_stairs:0-7` | Spruce Stairs | `stairs-slab`, `roof` | yes | standard, new: old town |  |
| `minecraft:birch_stairs:0-7` | Birch Stairs | `stairs-slab`, `roof` | yes | standard, new: old town |  |
| `minecraft:jungle_stairs:0-7` | Jungle Stairs | `stairs-slab`, `roof` | yes | standard, new: old town |  |
| `minecraft:acacia_stairs:0-7` | Acacia Stairs | `stairs-slab`, `roof` | yes | standard, new: old town |  |
| `minecraft:dark_oak_stairs:0-7` | Dark Oak Stairs | `stairs-slab`, `roof` | yes | standard, new: old town |  |
| `minecraft:stone_stairs:0-7` | Cobblestone Stairs | `stairs-slab`, `rubble-ruin` | partly | standard, new: old town | 1.7.10's `stone_stairs` are cobblestone; a different block from cobblestone, so not a boulder host |
| `minecraft:brick_stairs:0-7` | Brick Stairs | `stairs-slab`, `roof` | yes | standard, border | upstream uses it |
| `minecraft:stone_brick_stairs:0-7` | Stone Brick Stairs | `stairs-slab` | yes | standard, border | upstream's (`A t z c d`) |
| `minecraft:nether_brick_stairs:0-7` | Nether Brick Stairs | `stairs-slab`, `roof` | partly | standard, new: old town |  |
| `minecraft:sandstone_stairs:0-7` | Sandstone Stairs | `stairs-slab` | yes | desert |  |
| `minecraft:quartz_stairs:0-7` | Quartz Stairs | `stairs-slab` | yes | standard, border | upstream's (`(`, `)`) |
| `minecraft:cobblestone_wall:0` | Cobblestone Wall | `fence-rail` | yes | standard, new: old town | upstream's (`w`); not a boulder host |
| `minecraft:cobblestone_wall:1` | Mossy Cobblestone Wall | `fence-rail`, `rubble-ruin` | yes | standard, new: old town |  |
| `minecraft:fence:0` | Oak Fence | `fence-rail` | yes | standard, park | upstream's `oak_fence` (`.`); the only wood fence (Et Futurum's other woods are off) |
| `minecraft:nether_brick_fence:0` | Nether Brick Fence | `fence-rail` | yes | standard, new: industrial | a dark iron-railing look |
| `minecraft:fence_gate:0-7` | Oak Fence Gate | `fence-rail`, `door-trap` | yes | standard, park | meta = facing, +4 open |
| `minecraft:iron_bars:0` | Iron Bars | `pane`, `fence-rail` | yes | standard, border, new: industrial | upstream's (`:`) and every palette's damaged form |
| `minecraft:trapdoor:0-15` | Wooden Trapdoor | `door-trap`, `fence-rail` | yes | standard | the D19 stand-in for the iron trapdoor |
| `minecraft:wooden_door:0-11` | Wooden Door | `door-trap` | yes | standard, border | two blocks; every wood's door in D19 |
| `minecraft:iron_door:0-11` | Iron Door | `door-trap` | yes | standard, border, new: industrial | needs redstone to open |
| `minecraft:ladder:2-5` | Ladder | `furniture-deco` | yes | standard, border | upstream's (`l`); meta = facing |

### Glass and panes

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `minecraft:glass:0` | Glass | `glass` | yes | standard, border | upstream's (`+`, `a`) |
| `minecraft:glass_pane:0` | Glass Pane | `pane` | yes | standard, border | upstream's (`` ` ``, `a`) |
| `minecraft:stained_glass:0` | White Stained Glass | `glass` | yes | standard, border | upstream's `white_stained_glass`; translucent; every color is in upstream's random city-sphere glass `Z` |
| `minecraft:stained_glass:1` | Orange Stained Glass | `glass` | partly | standard, border |  |
| `minecraft:stained_glass:2` | Magenta Stained Glass | `glass` | partly | standard, border |  |
| `minecraft:stained_glass:3` | Light Blue Stained Glass | `glass` | yes | standard, border | upstream's `light_blue_stained_glass`;  |
| `minecraft:stained_glass:4` | Yellow Stained Glass | `glass` | partly | standard, border |  |
| `minecraft:stained_glass:5` | Lime Stained Glass | `glass` | partly | standard, border |  |
| `minecraft:stained_glass:6` | Pink Stained Glass | `glass` | partly | standard, border |  |
| `minecraft:stained_glass:7` | Gray Stained Glass | `glass` | yes | standard, border | upstream's `gray_stained_glass`;  |
| `minecraft:stained_glass:8` | Light Gray Stained Glass | `glass` | yes | standard, border |  |
| `minecraft:stained_glass:9` | Cyan Stained Glass | `glass` | yes | standard, border |  |
| `minecraft:stained_glass:10` | Purple Stained Glass | `glass` | partly | standard, border |  |
| `minecraft:stained_glass:11` | Blue Stained Glass | `glass` | yes | standard, border | upstream's `blue_stained_glass`;  |
| `minecraft:stained_glass:12` | Brown Stained Glass | `glass` | yes | standard, border |  |
| `minecraft:stained_glass:13` | Green Stained Glass | `glass` | yes | standard, border |  |
| `minecraft:stained_glass:14` | Red Stained Glass | `glass` | partly | standard, border |  |
| `minecraft:stained_glass:15` | Black Stained Glass | `glass` | yes | standard, border | upstream's `black_stained_glass`;  |
| `minecraft:stained_glass_pane:0` | White Stained Glass Pane | `pane` | yes | standard, border | upstream's `white_stained_glass_pane` |
| `minecraft:stained_glass_pane:1` | Orange Stained Glass Pane | `pane` | partly | standard, border |  |
| `minecraft:stained_glass_pane:2` | Magenta Stained Glass Pane | `pane` | partly | standard, border |  |
| `minecraft:stained_glass_pane:3` | Light Blue Stained Glass Pane | `pane` | yes | standard, border | upstream's `light_blue_stained_glass_pane` |
| `minecraft:stained_glass_pane:4` | Yellow Stained Glass Pane | `pane` | partly | standard, border |  |
| `minecraft:stained_glass_pane:5` | Lime Stained Glass Pane | `pane` | partly | standard, border |  |
| `minecraft:stained_glass_pane:6` | Pink Stained Glass Pane | `pane` | partly | standard, border |  |
| `minecraft:stained_glass_pane:7` | Gray Stained Glass Pane | `pane` | yes | standard, border | upstream's `gray_stained_glass_pane` |
| `minecraft:stained_glass_pane:8` | Light Gray Stained Glass Pane | `pane` | yes | standard, border |  |
| `minecraft:stained_glass_pane:9` | Cyan Stained Glass Pane | `pane` | yes | standard, border |  |
| `minecraft:stained_glass_pane:10` | Purple Stained Glass Pane | `pane` | partly | standard, border |  |
| `minecraft:stained_glass_pane:11` | Blue Stained Glass Pane | `pane` | yes | standard, border | upstream's `blue_stained_glass_pane` |
| `minecraft:stained_glass_pane:12` | Brown Stained Glass Pane | `pane` | yes | standard, border |  |
| `minecraft:stained_glass_pane:13` | Green Stained Glass Pane | `pane` | yes | standard, border |  |
| `minecraft:stained_glass_pane:14` | Red Stained Glass Pane | `pane` | partly | standard, border |  |
| `minecraft:stained_glass_pane:15` | Black Stained Glass Pane | `pane` | yes | standard, border | upstream's `black_stained_glass_pane` |

### Wool and carpet

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `minecraft:wool:0` | White Wool | `furniture-deco`, `wall-accent` | partly | standard | flammable; awnings, upholstery, carpets as blocks; not used by upstream |
| `minecraft:wool:1` | Orange Wool | `off-theme` | no | - | loud color |
| `minecraft:wool:2` | Magenta Wool | `off-theme` | no | - | loud color |
| `minecraft:wool:3` | Light Blue Wool | `off-theme` | no | - | loud color |
| `minecraft:wool:4` | Yellow Wool | `off-theme` | no | - | loud color |
| `minecraft:wool:5` | Lime Wool | `off-theme` | no | - | loud color |
| `minecraft:wool:6` | Pink Wool | `off-theme` | no | - | loud color |
| `minecraft:wool:7` | Gray Wool | `furniture-deco`, `wall-accent` | partly | standard |  |
| `minecraft:wool:8` | Light Gray Wool | `furniture-deco`, `wall-accent` | partly | standard |  |
| `minecraft:wool:9` | Cyan Wool | `furniture-deco`, `wall-accent` | partly | standard |  |
| `minecraft:wool:10` | Purple Wool | `off-theme` | no | - | loud color |
| `minecraft:wool:11` | Blue Wool | `furniture-deco`, `wall-accent` | partly | standard |  |
| `minecraft:wool:12` | Brown Wool | `furniture-deco`, `wall-accent` | partly | standard |  |
| `minecraft:wool:13` | Green Wool | `furniture-deco`, `wall-accent` | partly | standard |  |
| `minecraft:wool:14` | Red Wool | `furniture-deco`, `wall-accent` | partly | standard |  |
| `minecraft:wool:15` | Black Wool | `furniture-deco`, `wall-accent` | partly | standard |  |
| `minecraft:carpet:0` | White Carpet | `floor`, `furniture-deco` | yes | standard, border | 1/16 high floor covering; interiors of offices, hotels and homes; breaks without a block below |
| `minecraft:carpet:1` | Orange Carpet | `floor`, `furniture-deco` | partly | standard, border |  |
| `minecraft:carpet:2` | Magenta Carpet | `floor`, `furniture-deco` | partly | standard, border |  |
| `minecraft:carpet:3` | Light Blue Carpet | `floor`, `furniture-deco` | partly | standard, border |  |
| `minecraft:carpet:4` | Yellow Carpet | `floor`, `furniture-deco` | partly | standard, border |  |
| `minecraft:carpet:5` | Lime Carpet | `floor`, `furniture-deco` | partly | standard, border |  |
| `minecraft:carpet:6` | Pink Carpet | `floor`, `furniture-deco` | partly | standard, border |  |
| `minecraft:carpet:7` | Gray Carpet | `floor`, `furniture-deco` | yes | standard, border |  |
| `minecraft:carpet:8` | Light Gray Carpet | `floor`, `furniture-deco` | yes | standard, border |  |
| `minecraft:carpet:9` | Cyan Carpet | `floor`, `furniture-deco` | yes | standard, border |  |
| `minecraft:carpet:10` | Purple Carpet | `floor`, `furniture-deco` | partly | standard, border |  |
| `minecraft:carpet:11` | Blue Carpet | `floor`, `furniture-deco` | yes | standard, border |  |
| `minecraft:carpet:12` | Brown Carpet | `floor`, `furniture-deco` | yes | standard, border |  |
| `minecraft:carpet:13` | Green Carpet | `floor`, `furniture-deco` | yes | standard, border |  |
| `minecraft:carpet:14` | Red Carpet | `floor`, `furniture-deco` | yes | standard, border |  |
| `minecraft:carpet:15` | Black Carpet | `floor`, `furniture-deco` | yes | standard, border |  |

### Earth and plants

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `minecraft:leaves:0` | Oak Leaves | `foliage` | yes | standard, park | biome-tinted (renders gray here); +4 no-decay bit, which generated leaves need; upstream's park trees and overgrowth |
| `minecraft:leaves:1` | Spruce Leaves | `foliage` | yes | standard, park |  |
| `minecraft:leaves:2` | Birch Leaves | `foliage` | yes | standard, park |  |
| `minecraft:leaves:3` | Jungle Leaves | `foliage` | yes | standard, park |  |
| `minecraft:leaves2:0` | Acacia Leaves | `foliage` | yes | standard, park |  |
| `minecraft:leaves2:1` | Dark Oak Leaves | `foliage` | yes | standard, park |  |
| `minecraft:sapling:0` | Oak Sapling | `foliage` | yes | standard, park | upstream's saplings in `p`; +8 for stage 1 |
| `minecraft:sapling:1` | Spruce Sapling | `foliage` | yes | standard, park |  |
| `minecraft:sapling:2` | Birch Sapling | `foliage` | yes | standard, park |  |
| `minecraft:sapling:3` | Jungle Sapling | `foliage` | yes | standard, park |  |
| `minecraft:sapling:4` | Acacia Sapling | `foliage` | yes | standard, park |  |
| `minecraft:sapling:5` | Dark Oak Sapling | `foliage` | yes | standard, park |  |
| `minecraft:grass:0` | Grass Block | `foliage` | yes | standard, park | upstream's `grass_block` (`G`); biome-tinted top |
| `minecraft:dirt:0` | Dirt | `foliage`, `rubble-ruin` | yes | standard, park | upstream's `dirt` (`D`) |
| `minecraft:dirt:1` | Coarse Dirt (1.7.10) | `foliage`, `rubble-ruin` | yes | standard, park | 1.7.10 coarse dirt: dirt texture, never grows grass |
| `minecraft:dirt:2` | Podzol | `foliage` | partly | park |  |
| `minecraft:mycelium:0` | Mycelium | `off-theme` | no | - | spreads |
| `minecraft:sand:0` | Sand | `rubble-ruin`, `foliage` | partly | desert | falls (gravity) |
| `minecraft:sand:1` | Red Sand | `rubble-ruin` | partly | desert | falls |
| `minecraft:gravel:0` | Gravel | `rubble-ruin` | partly | standard, new: old town | falls |
| `minecraft:soul_sand:0` | Soul Sand | `off-theme` | no | - | slows walking |
| `minecraft:farmland:0` | Farmland | `foliage` | partly | park | turns to dirt when trampled or dry |
| `minecraft:web:0` | Cobweb | `rubble-ruin`, `special` | yes | standard, new: old town | upstream's (`%`, `\`); slows movement |
| `minecraft:vine:0` | Vines | `foliage`, `rubble-ruin` | yes | standard, new: old town | meta = attached faces; overgrowth; spreads on random ticks |
| `minecraft:tallgrass:1` | Tall Grass | `foliage` | yes | standard, park | meta 2 fern; tinted |
| `minecraft:tallgrass:2` | Fern | `foliage` | yes | park |  |
| `minecraft:deadbush:0` | Dead Bush | `foliage`, `rubble-ruin` | yes | desert |  |
| `minecraft:yellow_flower:0` | Dandelion | `foliage` | yes | standard, park | upstream's (`K`, `p`) |
| `minecraft:red_flower:0` | Poppy | `foliage` | yes | standard, park | upstream's `poppy` |
| `minecraft:red_flower:1` | Blue Orchid | `foliage` | yes | standard, park |  |
| `minecraft:red_flower:2` | Allium | `foliage` | yes | standard, park |  |
| `minecraft:red_flower:3` | Azure Bluet | `foliage` | yes | standard, park |  |
| `minecraft:red_flower:4` | Red Tulip | `foliage` | yes | standard, park |  |
| `minecraft:red_flower:5` | Orange Tulip | `foliage` | yes | standard, park |  |
| `minecraft:red_flower:6` | White Tulip | `foliage` | yes | standard, park |  |
| `minecraft:red_flower:7` | Pink Tulip | `foliage` | yes | standard, park |  |
| `minecraft:red_flower:8` | Oxeye Daisy | `foliage` | yes | standard, park |  |
| `minecraft:double_plant:0` | Sunflower | `foliage` | yes | park | two blocks: lower meta 0-5, upper meta 8 |
| `minecraft:double_plant:1` | Lilac | `foliage` | yes | park |  |
| `minecraft:double_plant:2` | Double Tallgrass | `foliage` | yes | park |  |
| `minecraft:double_plant:3` | Large Fern | `foliage` | yes | park |  |
| `minecraft:double_plant:4` | Rose Bush | `foliage` | yes | park |  |
| `minecraft:double_plant:5` | Peony | `foliage` | yes | park |  |
| `minecraft:waterlily:0` | Lily Pad | `foliage` | yes | park | fountains and ponds; on water only |
| `minecraft:cactus:0` | Cactus | `foliage` | yes | desert | hurts; needs sand; upstream's potted cactus `*` |
| `minecraft:reeds:0` | Sugar Cane | `foliage` | partly | park | needs water beside it |
| `minecraft:brown_mushroom:0` | Brown Mushroom | `foliage`, `rubble-ruin` | partly | new: old town | dark basements |
| `minecraft:red_mushroom:0` | Red Mushroom | `foliage`, `rubble-ruin` | partly | new: old town |  |
| `minecraft:brown_mushroom_block:14` | Brown Mushroom Block | `off-theme` | no | - |  |
| `minecraft:red_mushroom_block:14` | Red Mushroom Block | `off-theme` | no | - |  |
| `minecraft:flower_pot:0` | Flower Pot | `furniture-deco`, `foliage` | yes | standard, border | upstream's potted cactus (`*`); the plant lives in a tile entity |
| `minecraft:wheat:7` | Wheat (crop) | `foliage` | partly | park | meta = age; allotments |
| `minecraft:carrots:7` | Carrots (crop) | `foliage` | partly | park |  |
| `minecraft:potatoes:7` | Potatoes (crop) | `foliage` | partly | park |  |

### Lights, furniture and functional blocks

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `minecraft:torch:0` | Torch | `light` | yes | standard, border | upstream's (`T`); light 14; meta 1-4 wall, 5 floor |
| `minecraft:redstone_torch:0` | Redstone Torch | `light` | yes | standard | upstream's (`g`); light 7 |
| `minecraft:crafting_table:0` | Crafting Table | `furniture-deco` | yes | standard | upstream's `F` |
| `minecraft:furnace:0` | Furnace | `furniture-deco` | yes | standard | upstream's `F`; tile entity |
| `minecraft:chest:0` | Chest | `furniture-deco`, `special` | yes | standard, border | upstream's `C` (loot); tile entity |
| `minecraft:trapped_chest:0` | Trapped Chest | `furniture-deco` | partly | standard | tile entity; redstone when opened |
| `minecraft:ender_chest:0` | Ender Chest | `off-theme` | no | - | light 7; player-linked storage |
| `minecraft:anvil:0` | Anvil | `furniture-deco` | yes | standard, new: industrial | upstream's `F`; falls (gravity) |
| `minecraft:cauldron:0` | Cauldron | `furniture-deco` | yes | standard | upstream's `F` |
| `minecraft:brewing_stand:0` | Brewing Stand | `furniture-deco` | yes | standard | upstream's `F`; tile entity; light 1 |
| `minecraft:enchanting_table:0` | Enchantment Table | `furniture-deco` | yes | standard | upstream's `F`; tile entity |
| `minecraft:jukebox:0` | Jukebox | `furniture-deco` | yes | standard | tile entity |
| `minecraft:noteblock:0` | Note Block | `furniture-deco` | partly | standard | tile entity |
| `minecraft:bed:0` | Bed (red) | `furniture-deco` | yes | standard | two blocks; red only in vanilla (Et Futurum adds the other 15) |
| `minecraft:standing_sign:0` | Sign (standing) | `furniture-deco` | yes | standard | tile entity (text); meta = rotation |
| `minecraft:wall_sign:0` | Sign (wall) | `furniture-deco` | yes | standard, border | tile entity; storefront signs |
| `minecraft:wooden_pressure_plate:0` | Wooden Pressure Plate | `furniture-deco` | yes | standard | upstream's (`-`) |
| `minecraft:stone_pressure_plate:0` | Stone Pressure Plate | `furniture-deco` | yes | standard | upstream uses it |
| `minecraft:light_weighted_pressure_plate:0` | Weighted Pressure Plate (Light) | `furniture-deco` | partly | standard |  |
| `minecraft:heavy_weighted_pressure_plate:0` | Weighted Pressure Plate (Heavy) | `furniture-deco` | partly | standard, new: industrial |  |
| `minecraft:stone_button:0` | Stone Button | `furniture-deco` | partly | standard |  |
| `minecraft:wooden_button:0` | Wooden Button | `furniture-deco` | partly | standard |  |
| `minecraft:lever:0` | Lever | `furniture-deco` | yes | standard, new: industrial | upstream uses it |
| `minecraft:rail:0` | Rail | `street`, `special` | yes | standard | upstream's rails letters |
| `minecraft:golden_rail:0` | Powered Rail | `street`, `special` | yes | standard | upstream uses it |
| `minecraft:detector_rail:0` | Detector Rail | `special` | partly | standard |  |
| `minecraft:activator_rail:0` | Activator Rail | `special` | partly | standard |  |
| `minecraft:hopper:0` | Hopper | `furniture-deco` | partly | new: industrial | tile entity |
| `minecraft:dispenser:0` | Dispenser | `off-theme` | no | - | tile entity; fires items |
| `minecraft:dropper:0` | Dropper | `off-theme` | no | - | tile entity |
| `minecraft:daylight_detector:0` | Daylight Sensor | `furniture-deco` | partly | new: tech district | tile entity; a rooftop solar-panel look |
| `minecraft:mob_spawner:0` | Monster Spawner | `special` | yes | standard | upstream's (`1`, `4`); tile entity; mobs in loot-and-mobs.md |
| `minecraft:beacon:0` | Beacon | `off-theme` | no | - | light 15; valuable |
| `minecraft:skull:0` | Mob Head | `furniture-deco` | partly | new: old town | tile entity holds the skull type; the sheet shows a stand-in texture |
| `minecraft:tnt:0` | TNT | `off-theme` | no | - | explodes |
| `minecraft:piston:0` | Piston | `off-theme` | no | - | redstone mechanism |
| `minecraft:sticky_piston:0` | Sticky Piston | `off-theme` | no | - | redstone mechanism |
| `minecraft:command_block:0` | Command Block | `off-theme` | no | - | never |
| `minecraft:dragon_egg:0` | Dragon Egg | `off-theme` | no | - | unique trophy; falls and teleports |
| `minecraft:end_portal_frame:0` | End Portal Frame | `off-theme` | no | - | unbreakable |
| `minecraft:cake:0` | Cake | `furniture-deco` | partly | standard | bakery and restaurant tables |

## Best variety pools

Vanilla is the D19 baseline, so its pools are mostly the variety that upstream's palettes did not use.

| Upstream role (palette char) | Baseline (D19) | Vanilla variety not used by upstream |
|---|---|---|
| Stone-brick wall `#` | `stonebrick:0`, mossy and cracked in the rubble variant | `stonebrick:3` (chiseled) as accent, `double_stone_slab:0` (banded smooth stone), `double_stone_slab:5` |
| Brick | `brick_block` | `hardened_clay` (plain terracotta, not a boulder host), `nether_brick` for dark brick |
| Quartz `Q` | `quartz_block:0` | `quartz_block:1` chiseled, `:2` pillar for columns, `double_stone_slab:7` |
| Terracotta walls | `stained_hardened_clay` 7, 8, 9 (boulder hosts) | `stained_hardened_clay` 0, 4, 11, 12, 13 for new color palettes; `hardened_clay` as the non-host plain option |
| Desert | sandstone, cut sandstone, orange/red terracotta | `sandstone:1` chiseled, `double_stone_slab:9` smooth sandstone top, `stained_hardened_clay` 0, 4, 12 |
| Glass `+`, `` ` `` | glass, panes, white/black/blue/gray/light-blue stained | `stained_glass` 8 (light gray), 9 (cyan), 12 (brown), 13 (green) and their panes |
| Floors | smooth stone double slab | `planks` 1, 2, 5; `carpet` 0, 7, 8, 12, 14, 15 for interiors; `wooden_slab` |
| Roof | (walls, slabs) | wooden and brick stairs as pitched roofs; `stone_slab:0` flat roofs |
| Light `h` | glowstone | `lit_pumpkin` (parks, seasonal), `redstone_lamp` with power, torches |
| Fence `.` and walls `w` | oak fence, cobblestone wall | `nether_brick_fence` (dark railing), `cobblestone_wall:1`, `iron_bars` |
| Rubble `}` and ruins | wall rubble variant | `mossy_cobblestone`, `stonebrick:1-2`, `gravel`, `vine`, `web`, `dirt:1` |
| Furniture `F` | furnace, crafting table, brewing stand, anvil, cauldron, enchanting table | `jukebox`, `noteblock`, `cake`, `hay_block`, `bookshelf`, `flower_pot`, red `bed`, signs |
| Park `p`, `K` | poppy, dandelion, saplings | the other eight small flowers, the six tall flowers, ferns, lily pads, `dirt:2` podzol |
