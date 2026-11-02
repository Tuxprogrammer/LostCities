# Block catalog: Et Futurum Requiem

Et Futurum Requiem 2.6.65-GTNH (`etfuturum-2.6.65-GTNH.jar`), modern blocks backported to 1.7.10. Read from the source at the pack's tag (`gtnh-src/source/Et-Futurum-Requiem/`, `ModBlocks.java` and `blocks/`) and the pack's shipped `config/etfuturum/*.cfg`, with each block's enable condition evaluated against that config by script. Textures come from the jar (most live under `assets/minecraft/textures/blocks/` inside it). Part of the decorative block catalog (decision log D25). Contact sheets: `tools/block-contact-sheet/sheets/etfuturum/*.json`, rendered to `docs/previews/blocks/etfuturum/`.

## Summary

- **Registered in the pack: 340 of the 532 blocks** in `ModBlocks` (plus up to 12 deepslate-ore compat blocks whose condition depends on other mods). Registry names are `etfuturum:` plus the lowercased enum name ([block-mapping.md](../../00-pack-integration/block-mapping.md)).
- **Catalogued: 478 rows**, one per block+meta that a city could use. Stairs, signs, doors and trapdoors take one row for all their orientation metas; slabs list their material metas (top half is +8).
- **Fit with upstream's look: 169 yes, 249 partly, 60 no.**
- **Excluded, with the reason:**
- **Ores** (`copper_ore` is off; `deepslate_*_ore`, `modded_deepslate_ore`, `nether_gold_ore` is off): ores, never building blocks.
- **Technical states with no item**: `powered_copper_bulb`, `lit_smoker`, `lit_blast_furnace`, `frosted_ice`, `lava_cauldron`, `potion_cauldron`, `bubble_column`, `daylight_detector_inverted`.
- **Unbreakable blocks**: `barrier` and `light` (the invisible light block extends the barrier and has hardness -1). Generation never places unbreakable blocks ([0004](../../../design/0004-gtnh-layer-and-tier-ceiling.md)), so `light` cannot serve as a hidden lamp.
- **Gameplay flags.** Light: lantern 15, soul lantern 10, soul torch 10, end rod 14, lit copper bulbs 15/12/8/4, glow lichen 7, cave vines with berries 14, magma 3, amethyst buds 1-5. Gravity: concrete powder, old gravel. Tile entities: barrels (all tiers), banners, signs, shulker boxes, smoker, blast furnace, brewing stand, enchanting table, beacon. Transparency: tinted glass (translucent but blocks light), copper grate (not opaque), honey and slime (translucent). Random ticks that change the block: unwaxed copper weathers (metas 0-7, chiseled/grate/bulb 0-3, cut-copper stairs and slabs without `waxed`); budding amethyst grows; cherry leaves decay. Movement: blue ice (slippery), honey (sticky), slime (bouncy), magma (burns), sweet berry bush (hurts), wither rose (Wither). No connected textures.
- **GT hosts.** `etfuturum:deepslate` (any meta) and `etfuturum:tuff` meta 0 are ore hosts and are never building blocks. Every other block here is a different block or meta, so none is an ore or boulder host: `cobbled_deepslate`, `polished_deepslate`, `deepslate_bricks`, `tuff` 1-4, and every slab, stair and wall. Et Futurum's concrete and glazed terracotta are not boulder hosts either, unlike vanilla stained clay.

Blocks per role (a block can hold several roles):

| Role | Blocks+meta |
|---|---|
| `wall-main` | 48 |
| `wall-accent` | 128 |
| `trim-pillar` | 71 |
| `floor` | 51 |
| `roof` | 3 |
| `glass` | 1 |
| `street` | 9 |
| `sidewalk` | 6 |
| `stairs-slab` | 57 |
| `rubble-ruin` | 29 |
| `light` | 14 |
| `fence-rail` | 35 |
| `door-trap` | 17 |
| `furniture-deco` | 128 |
| `foliage` | 14 |
| `special` | 9 |
| `off-theme` | 53 |

### What the pack's config turns off, and who fills the gap

| Off in the pack | Flag | Fill |
|---|---|---|
| Granite, andesite, diorite and their stairs, slabs, walls | `enableStones=false` | Botania's 1.8 stones (`Botania:stone`, stairs, `stone0Wall`), Chisel's `andesite`/`diorite`/`granite` groups; calcite and polished tuff here for pale and dark-gray polished stone |
| Prismarine and the sea lantern | `enablePrismarine=false` | Botania's prismarine family and `Botania:seaLamp`; Ztones' flat lamps for flush ceiling light; lit copper bulbs here |
| Iron trapdoor | `enableIronTrapdoor=false` | **Copper trapdoors are on** (8 blocks: four weathering stages, waxed and unwaxed): the metal hatch D19 thought the pack lacked |
| Wood-variant doors (spruce, birch, jungle, acacia, dark oak, cherry, BoP, Witchery) | `enableVanillaDoors=false`, `enableNewDoors` per wood | **Copper doors are on** (8 blocks), hand-opened metal doors with windows; otherwise `minecraft:wooden_door` |
| Wood fences and gates in other woods | `enableVanillaFences=false`, `enableVanillaGates=false` | Et Futurum walls (stone brick, brick, sandstone, deepslate, blackstone, tuff); vanilla oak fence |
| Smooth sandstone and its stairs and slab | `enableSmoothSandstone=false` | vanilla cut sandstone (`minecraft:sandstone:2`), `etfuturum:stone_slab:3`; smooth *red* sandstone is on |
| Basalt, smooth basalt | `enableBasalt=false` | Botania's basalt (`Botania:stone:1`, a dark stone, not GT basalt) |
| Mud, packed mud, mud bricks | `enableMud=false` | Ztones Roen and Kryp for earthy desert masonry |
| Moss, azalea | `enableMossAzalea=false` | vanilla mossy cobblestone and vines, Botania's Solid Vine, glow lichen here |
| Crimson, warped, mangrove, bamboo woods | experiments off, `enableBambooBlocks=false` | cherry is the only new wood; stripped logs and bark of every vanilla, BoP and Witchery wood are on |
| Dripstone, lightning rod, composter, beehives, sculk | off | none needed |

### Corrections to the D19 mapping

- **1.20 `stone_stairs` are raw-stone stairs**, not smooth-stone stairs. `etfuturum:stone_stairs` uses the plain stone texture, so it is a faithful match after all; block-mapping.md's wording ("smooth-stone stairs") is wrong, the mapping itself is right. Neither it nor `etfuturum:stone_slab:0` (the raw-stone slab) is an ore host, since they are different blocks from `minecraft:stone`.
- **Metal doors and trapdoors exist**: copper doors and trapdoors are on (`enableCopper=true`), which the "Candidates considered and not used" table did not consider.
- **Copper is a decorative metal (D29).** Every copper block here (blocks, cut and chiseled copper, grates, bulbs, doors, trapdoors, cut copper stairs and slabs) is gated by the `decorativeMetals.copper` flag and placed as accents only, under the per-building metal cap: bands, trims, fittings, never whole walls, floors or roofs ([index](index.md#decorative-metals-d29)).

## Blocks

### Stone and masonry

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `etfuturum:smooth_stone:0` | Smooth Stone | `wall-main`, `floor`, `street`, `sidewalk` | yes | standard, border | D19 stand-in for raw stone; stone-slab-top texture on all faces; stairs `stone_stairs`, slab `stone_slab:0` |
| `etfuturum:smooth_quartz:0` | Smooth Quartz Block | `wall-main`, `floor`, `trim-pillar` | yes | standard, border | upstream's `smooth_quartz`; stairs `smooth_quartz_stairs`, slab `smooth_quartz_slab` |
| `etfuturum:quartz_bricks:0` | Quartz Bricks | `wall-main`, `wall-accent` | yes | standard, border | upstream's `quartz_bricks`; hardness 0.8; no stairs or slab |
| `etfuturum:calcite:0` | Calcite | `wall-main`, `wall-accent`, `floor` | yes | standard, border, desert | pale off-white stone, close to weathered limestone or white render; no stairs, slab or wall; not an ore host |
| `etfuturum:tuff:0` | Tuff | `off-theme` | no | - | GT ore host in the overworld (`etfuturum:tuff` meta 0): never a building block |
| `etfuturum:tuff:1` | Polished Tuff | `wall-main`, `wall-accent`, `floor` | yes | standard, border | dark green-gray modern masonry; not an ore host (the GT host test matches meta 0 only); stairs `polished_tuff_stairs`, slab `tuff_slab:1`, wall `tuff_wall:1` |
| `etfuturum:tuff:2` | Tuff Bricks | `wall-main`, `wall-accent`, `floor` | yes | standard, border | dark green-gray modern masonry; not an ore host (the GT host test matches meta 0 only); stairs `tuff_brick_stairs`, slab `tuff_slab:2`, wall `tuff_wall:2` |
| `etfuturum:tuff:3` | Chiseled Tuff | `wall-accent`, `trim-pillar` | partly | standard | carved top differs from the sides; not an ore host |
| `etfuturum:tuff:4` | Chiseled Tuff Bricks | `wall-accent`, `trim-pillar` | partly | standard | carved top differs from the sides; not an ore host |
| `etfuturum:deepslate:0` | Deepslate | `off-theme` | no | - | GT ore host in the overworld, any meta: never a building block (listed only so the catalog is complete) |
| `etfuturum:cobbled_deepslate:0` | Cobbled Deepslate | `rubble-ruin`, `wall-main`, `street` | partly | standard, new: old town | dark cobble; a separate block from the host `deepslate`; stairs, slab `deepslate_slab:0`, wall `deepslate_wall:0` |
| `etfuturum:polished_deepslate:0` | Polished Deepslate | `wall-main`, `floor`, `street` | yes | standard, border | dark slate-gray smooth; stairs, slab `deepslate_slab:1`, wall `deepslate_wall:1` |
| `etfuturum:deepslate_bricks:0` | Deepslate Bricks | `wall-main`, `wall-accent` | yes | standard, border | dark modern brick; stairs `deepslate_brick_stairs`, slab `deepslate_brick_slab:0`, wall `deepslate_brick_wall:0` |
| `etfuturum:deepslate_bricks:1` | Cracked Deepslate Bricks | `rubble-ruin`, `wall-main` | yes | standard, border | cracked: the damaged form of meta 0 |
| `etfuturum:deepslate_bricks:2` | Deepslate Tiles | `wall-main`, `floor`, `roof` | yes | standard, border | upstream's `deepslate_tiles`; stairs `deepslate_tile_stairs`, slab `deepslate_brick_slab:1`, wall `deepslate_brick_wall:1` |
| `etfuturum:deepslate_bricks:3` | Cracked Deepslate Tiles | `rubble-ruin`, `floor` | yes | standard, border | upstream's `cracked_deepslate_tiles` |
| `etfuturum:deepslate_bricks:4` | Chiseled Deepslate | `wall-accent`, `trim-pillar` | partly | standard, border |  |
| `etfuturum:blackstone:0` | Blackstone | `rubble-ruin`, `wall-accent` | partly | standard, border | near-black rough stone; stairs `blackstone_stairs`, slab `blackstone_slab:0`, wall `blackstone_wall:0` |
| `etfuturum:blackstone:1` | Polished Blackstone | `wall-main`, `floor`, `street` | yes | standard, border | upstream's `polished_blackstone`; stairs, slab `blackstone_slab:1`, wall `blackstone_wall:1` |
| `etfuturum:blackstone:2` | Polished Blackstone Bricks | `wall-main`, `wall-accent` | yes | standard, border | upstream's `polished_blackstone_bricks`; stairs, slab `blackstone_slab:2`, wall `blackstone_wall:2` |
| `etfuturum:blackstone:3` | Cracked Polished Blackstone Bricks | `rubble-ruin`, `wall-main` | yes | standard, border | upstream's cracked bricks |
| `etfuturum:blackstone:4` | Chiseled Polished Blackstone | `wall-accent`, `trim-pillar` | partly | standard, border | a carved piglin-face motif |
| `etfuturum:gilded_blackstone:0` | Gilded Blackstone | `rubble-ruin` | no | - | gold-flecked; drops gold nuggets, so it is loot as much as a block |
| `etfuturum:red_netherbrick:0` | Red Nether Bricks | `wall-main`, `wall-accent` | yes | desert, standard | upstream's `red_nether_bricks` (desert `B`); stairs, slab, wall |
| `etfuturum:red_netherbrick:1` | Cracked Nether Bricks | `rubble-ruin`, `wall-main` | partly | standard, new: old town | cracked *plain* nether brick (dark), not red |
| `etfuturum:red_netherbrick:2` | Chiseled Nether Bricks | `wall-accent`, `trim-pillar` | partly | standard, new: old town | dark nether brick with a carved face |
| `etfuturum:red_sandstone:0` | Red Sandstone | `wall-main` | yes | desert | upstream's `red_sandstone`; stairs, slab `red_sandstone_slab:0`, wall |
| `etfuturum:red_sandstone:1` | Chiseled Red Sandstone | `wall-accent`, `trim-pillar` | yes | desert |  |
| `etfuturum:red_sandstone:2` | Cut Red Sandstone | `wall-main`, `wall-accent`, `floor` | yes | desert | the 1.12 `red_sandstone@2` of the unused `bricks_desert_redsand` palette; slab `red_sandstone_slab:1` |
| `etfuturum:smooth_red_sandstone:0` | Smooth Red Sandstone | `wall-main`, `floor`, `street` | yes | desert | stairs and slab; the desert counterpart of smooth stone for `S` |
| `etfuturum:end_bricks:0` | End Stone Bricks | `wall-main`, `wall-accent` | partly | desert, standard | pale yellow brick, reads as limestone brick; stairs, slab, wall; not a host (only `end_stone` is) |
| `etfuturum:purpur_block:0` | Purpur Block | `wall-accent` | no | new: neon/arcade | violet tile; off the 20th-century palette; stairs and slab |
| `etfuturum:purpur_pillar:0` | Purpur Pillar | `trim-pillar` | no | new: neon/arcade | axis in meta (0 Y, 4 X, 8 Z) |
| `etfuturum:crying_obsidian:0` | Crying Obsidian | `off-theme` | no | - | hardness 50; no light set in this port |
| `etfuturum:bone:0` | Bone Block | `special` | partly | new: old town | pale ribbed pillar; ossuary or ruin detail; axis in meta |
| `etfuturum:magma:0` | Magma Block | `off-theme` | no | - | light 3; burns entities standing on it |
| `etfuturum:amethyst_block:0` | Block of Amethyst | `wall-accent` | no | new: neon/arcade | chimes when walked on |
| `etfuturum:budding_amethyst:0` | Budding Amethyst | `off-theme` | no | - | grows clusters on random ticks |
| `etfuturum:ancient_debris:0` | Ancient Debris | `off-theme` | no | - | netherite source: valuable, never in a city |
| `etfuturum:netherite_block:0` | Block of Netherite | `off-theme` | no | - | far above the EV ceiling as loot; stairs `netherite_stairs` too |
| `etfuturum:honeycomb_block:0` | Honeycomb Block | `wall-accent` | no | new: neon/arcade | orange hex pattern |
| `etfuturum:honey_block:0` | Honey Block | `off-theme` | no | - | translucent; slows and sticks entities |
| `etfuturum:slime:0` | Slime Block | `off-theme` | no | - | translucent; bounces entities |
| `etfuturum:sponge:0` | Sponge | `off-theme` | no | - | meta 1 wet sponge; absorbs water |
| `etfuturum:blue_ice:0` | Blue Ice | `off-theme` | no | - | slipperiness 0.989; does not melt |

### Earth, plants and minerals

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `etfuturum:coarse_dirt:0` | Coarse Dirt | `foliage`, `rubble-ruin` | yes | standard, park | dirt that never grows grass: stable park and vacant-lot ground |
| `etfuturum:grass_path:0` | Dirt Path | `sidewalk`, `foliage` | yes | park | 15/16 high; park paths |
| `etfuturum:old_gravel:0` | Old Gravel | `rubble-ruin` | partly | new: old town | falls (gravity); the pre-1.8 gravel look |
| `etfuturum:soul_soil:0` | Soul Soil | `off-theme` | no | - | nether ground |
| `etfuturum:nether_wart:0` | Nether Wart Block | `off-theme` | no | - | deep red organic block |
| `etfuturum:brown_mushroom:0` | Brown Mushroom Block (silk-touched) | `off-theme` | no | - | meta picks the faces (0 pores, 1-9 caps, 10 stem, 14 all cap, 15 all stem); overgrown-ruin detail at most |
| `etfuturum:red_mushroom:0` | Red Mushroom Block (silk-touched) | `off-theme` | no | - | meta picks the faces (0 pores, 1-9 caps, 10 stem, 14 all cap, 15 all stem); overgrown-ruin detail at most |
| `etfuturum:cornflower:0` | Cornflower | `foliage` | yes | standard, park | flower; potable |
| `etfuturum:lily_of_the_valley:0` | Lily of the Valley | `foliage` | yes | standard, park | flower |
| `etfuturum:rose:0` | Rose | `foliage` | yes | standard, park | the old 1.7 rose |
| `etfuturum:wither_rose:0` | Wither Rose | `foliage` | no | - | gives Wither to entities that touch it |
| `etfuturum:sweet_berry_bush:3` | Sweet Berry Bush | `foliage` | partly | park | meta 0-3 is growth stage; hurts and slows entities moving through it; no item form (berries plant it) |
| `etfuturum:pink_petals:0` | Pink Petals | `foliage` | yes | park | ground cover; meta = count and facing |
| `etfuturum:glow_lichen:0` | Glow Lichen | `foliage`, `light`, `rubble-ruin` | yes | standard, new: old town | light 7; grows on faces (meta = faces); overgrowth that lights a ruin |
| `etfuturum:cave_vine_plant:0` | Cave Vines | `foliage`, `light` | partly | new: old town | light 14 when it has glow berries; hangs from ceilings |
| `etfuturum:chorus_plant:0` | Chorus Plant | `off-theme` | no | - | End plant; breaks without support |
| `etfuturum:chorus_flower:0` | Chorus Flower | `off-theme` | no | - | End plant |
| `etfuturum:amethyst_cluster_1:0` | Small/Medium Amethyst Bud | `off-theme` | no | - | light 1-4 by size |
| `etfuturum:amethyst_cluster_2:0` | Large Bud/Amethyst Cluster | `off-theme` | no | - | light up to 5 |
| `etfuturum:beetroots:3` | Beetroots | `foliage` | partly | park | crop (meta = age); allotment gardens |

### Concrete and concrete powder

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `etfuturum:concrete:0` | White Concrete | `wall-main`, `wall-accent`, `floor`, `street`, `sidewalk` | yes | standard, border | upstream's `<color>_concrete` (black, cyan, gray, red, yellow used); no stairs or slabs in this port; not a boulder host |
| `etfuturum:concrete:1` | Orange Concrete | `wall-accent` | partly | standard |  |
| `etfuturum:concrete:2` | Magenta Concrete | `wall-accent` | partly | standard |  |
| `etfuturum:concrete:3` | Light Blue Concrete | `wall-main`, `wall-accent`, `floor` | yes | standard, border |  |
| `etfuturum:concrete:4` | Yellow Concrete | `wall-main`, `wall-accent`, `floor` | yes | standard, border |  |
| `etfuturum:concrete:5` | Lime Concrete | `wall-accent` | partly | standard |  |
| `etfuturum:concrete:6` | Pink Concrete | `wall-accent` | partly | standard |  |
| `etfuturum:concrete:7` | Gray Concrete | `wall-main`, `wall-accent`, `floor`, `street`, `sidewalk` | yes | standard, border |  |
| `etfuturum:concrete:8` | Light Gray Concrete | `wall-main`, `wall-accent`, `floor`, `street`, `sidewalk` | yes | standard, border |  |
| `etfuturum:concrete:9` | Cyan Concrete | `wall-main`, `wall-accent`, `floor` | yes | standard, border |  |
| `etfuturum:concrete:10` | Purple Concrete | `wall-accent` | partly | standard |  |
| `etfuturum:concrete:11` | Blue Concrete | `wall-main`, `wall-accent`, `floor` | yes | standard, border |  |
| `etfuturum:concrete:12` | Brown Concrete | `wall-main`, `wall-accent`, `floor` | yes | standard, border |  |
| `etfuturum:concrete:13` | Green Concrete | `wall-accent` | partly | standard |  |
| `etfuturum:concrete:14` | Red Concrete | `wall-main`, `wall-accent`, `floor` | yes | standard, border |  |
| `etfuturum:concrete:15` | Black Concrete | `wall-main`, `wall-accent`, `floor`, `street`, `sidewalk` | yes | standard, border |  |
| `etfuturum:concrete_powder:0` | White Concrete Powder | `rubble-ruin` | partly | standard | falls (gravity) and hardens to concrete in water: avoid in structures; a rubble heap only |
| `etfuturum:concrete_powder:1` | Orange Concrete Powder | `rubble-ruin` | partly | standard |  |
| `etfuturum:concrete_powder:2` | Magenta Concrete Powder | `rubble-ruin` | partly | standard |  |
| `etfuturum:concrete_powder:3` | Light Blue Concrete Powder | `rubble-ruin` | partly | standard |  |
| `etfuturum:concrete_powder:4` | Yellow Concrete Powder | `rubble-ruin` | partly | standard |  |
| `etfuturum:concrete_powder:5` | Lime Concrete Powder | `rubble-ruin` | partly | standard |  |
| `etfuturum:concrete_powder:6` | Pink Concrete Powder | `rubble-ruin` | partly | standard |  |
| `etfuturum:concrete_powder:7` | Gray Concrete Powder | `rubble-ruin` | partly | standard |  |
| `etfuturum:concrete_powder:8` | Light Gray Concrete Powder | `rubble-ruin` | partly | standard |  |
| `etfuturum:concrete_powder:9` | Cyan Concrete Powder | `rubble-ruin` | partly | standard |  |
| `etfuturum:concrete_powder:10` | Purple Concrete Powder | `rubble-ruin` | partly | standard |  |
| `etfuturum:concrete_powder:11` | Blue Concrete Powder | `rubble-ruin` | partly | standard |  |
| `etfuturum:concrete_powder:12` | Brown Concrete Powder | `rubble-ruin` | partly | standard |  |
| `etfuturum:concrete_powder:13` | Green Concrete Powder | `rubble-ruin` | partly | standard |  |
| `etfuturum:concrete_powder:14` | Red Concrete Powder | `rubble-ruin` | partly | standard |  |
| `etfuturum:concrete_powder:15` | Black Concrete Powder | `rubble-ruin` | partly | standard |  |

### Glazed terracotta

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `etfuturum:white_glazed_terracotta:0` | White Glazed Terracotta | `wall-accent`, `floor` | partly | standard, desert | bold patterned tile, rotates by facing (meta 0-3); lobby floors and accent bands; not a boulder host (a separate block from stained clay) |
| `etfuturum:orange_glazed_terracotta:0` | Orange Glazed Terracotta | `wall-accent`, `floor` | partly | standard, desert |  |
| `etfuturum:magenta_glazed_terracotta:0` | Magenta Glazed Terracotta | `off-theme` | no | - | loud pattern |
| `etfuturum:light_blue_glazed_terracotta:0` | Light Blue Glazed Terracotta | `wall-accent`, `floor` | partly | standard, desert |  |
| `etfuturum:yellow_glazed_terracotta:0` | Yellow Glazed Terracotta | `off-theme` | no | - | loud pattern |
| `etfuturum:lime_glazed_terracotta:0` | Lime Glazed Terracotta | `off-theme` | no | - | loud pattern |
| `etfuturum:pink_glazed_terracotta:0` | Pink Glazed Terracotta | `off-theme` | no | - | loud pattern |
| `etfuturum:gray_glazed_terracotta:0` | Gray Glazed Terracotta | `wall-accent`, `floor` | partly | standard |  |
| `etfuturum:light_gray_glazed_terracotta:0` | Light Gray Glazed Terracotta | `wall-accent`, `floor` | partly | standard |  |
| `etfuturum:cyan_glazed_terracotta:0` | Cyan Glazed Terracotta | `wall-accent`, `floor` | partly | standard |  |
| `etfuturum:purple_glazed_terracotta:0` | Purple Glazed Terracotta | `off-theme` | no | - | loud pattern |
| `etfuturum:blue_glazed_terracotta:0` | Blue Glazed Terracotta | `wall-accent`, `floor` | partly | standard |  |
| `etfuturum:brown_glazed_terracotta:0` | Brown Glazed Terracotta | `wall-accent`, `floor` | partly | standard, desert |  |
| `etfuturum:green_glazed_terracotta:0` | Green Glazed Terracotta | `off-theme` | no | - | loud pattern |
| `etfuturum:red_glazed_terracotta:0` | Red Glazed Terracotta | `off-theme` | no | - | loud pattern |
| `etfuturum:black_glazed_terracotta:0` | Black Glazed Terracotta | `wall-accent`, `floor` | partly | standard |  |

### Copper blocks

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `etfuturum:copper_block:0` | Block of Copper | `wall-accent` | partly | standard, new: old town | unwaxed metas 0-7 weather one stage at a time on random ticks; waxed metas 8-15 never change |
| `etfuturum:copper_block:1` | Exposed Copper | `wall-accent` | yes | standard, new: old town | upstream's `exposed_copper` |
| `etfuturum:copper_block:2` | Weathered Copper | `wall-accent` | yes | standard, new: old town |  |
| `etfuturum:copper_block:3` | Oxidized Copper | `wall-accent` | yes | standard, new: old town |  |
| `etfuturum:copper_block:4` | Cut Copper | `wall-accent`, `trim-pillar` | yes | standard, new: old town |  |
| `etfuturum:copper_block:5` | Exposed Cut Copper | `wall-accent`, `trim-pillar` | yes | standard, new: old town | the stairs upstream uses are `exposed_cut_copper_stairs`; slabs `cut_copper_slab:0-7` |
| `etfuturum:copper_block:6` | Weathered Cut Copper | `wall-accent`, `trim-pillar` | yes | standard, new: old town |  |
| `etfuturum:copper_block:7` | Oxidized Cut Copper | `wall-accent`, `trim-pillar` | yes | standard, new: old town |  |
| `etfuturum:copper_block:8` | Waxed Block of Copper | `wall-accent` | partly | standard, new: old town |  |
| `etfuturum:copper_block:9` | Waxed Exposed Copper | `wall-accent` | yes | standard, new: old town |  |
| `etfuturum:copper_block:10` | Waxed Weathered Copper | `wall-accent` | yes | standard, new: old town |  |
| `etfuturum:copper_block:11` | Waxed Oxidized Copper | `wall-accent` | yes | standard, new: old town |  |
| `etfuturum:copper_block:12` | Waxed Cut Copper | `wall-accent`, `trim-pillar` | yes | standard, new: old town |  |
| `etfuturum:copper_block:13` | Waxed Exposed Cut Copper | `wall-accent`, `trim-pillar` | yes | standard, new: old town |  |
| `etfuturum:copper_block:14` | Waxed Weathered Cut Copper | `wall-accent`, `trim-pillar` | yes | standard, new: old town |  |
| `etfuturum:copper_block:15` | Waxed Oxidized Cut Copper | `wall-accent`, `trim-pillar` | yes | standard, new: old town |  |
| `etfuturum:chiseled_copper:0` | Chiseled Copper | `wall-accent`, `trim-pillar` | partly | standard, new: old town | metas 4-7 waxed |
| `etfuturum:chiseled_copper:1` | Exposed Chiseled Copper | `wall-accent`, `trim-pillar` | partly | standard, new: old town |  |
| `etfuturum:chiseled_copper:2` | Weathered Chiseled Copper | `wall-accent`, `trim-pillar` | partly | standard, new: old town |  |
| `etfuturum:chiseled_copper:3` | Oxidized Chiseled Copper | `wall-accent`, `trim-pillar` | partly | standard, new: old town |  |
| `etfuturum:chiseled_copper:4` | Waxed Chiseled Copper | `wall-accent`, `trim-pillar` | partly | standard, new: old town |  |
| `etfuturum:chiseled_copper:5` | Waxed Exposed Chiseled Copper | `wall-accent`, `trim-pillar` | partly | standard, new: old town |  |
| `etfuturum:chiseled_copper:6` | Waxed Weathered Chiseled Copper | `wall-accent`, `trim-pillar` | partly | standard, new: old town |  |
| `etfuturum:chiseled_copper:7` | Waxed Oxidized Chiseled Copper | `wall-accent`, `trim-pillar` | partly | standard, new: old town |  |
| `etfuturum:copper_grate:0` | Copper Grate | `fence-rail`, `special` | partly | new: industrial, standard | see-through lattice block (render pass, not a full occluder); catwalks, vents and railings |
| `etfuturum:copper_grate:1` | Exposed Copper Grate | `fence-rail`, `special` | partly | new: industrial, standard |  |
| `etfuturum:copper_grate:2` | Weathered Copper Grate | `fence-rail`, `special` | partly | new: industrial, standard |  |
| `etfuturum:copper_grate:3` | Oxidized Copper Grate | `fence-rail`, `special` | partly | new: industrial, standard |  |
| `etfuturum:copper_grate:4` | Waxed Copper Grate | `fence-rail`, `special` | partly | new: industrial, standard |  |
| `etfuturum:copper_grate:5` | Waxed Exposed Copper Grate | `fence-rail`, `special` | partly | new: industrial, standard |  |
| `etfuturum:copper_grate:6` | Waxed Weathered Copper Grate | `fence-rail`, `special` | partly | new: industrial, standard |  |
| `etfuturum:copper_grate:7` | Waxed Oxidized Copper Grate | `fence-rail`, `special` | partly | new: industrial, standard |  |
| `etfuturum:copper_bulb:0` | Copper Bulb | `wall-accent` | partly | standard, new: industrial | unlit; metas 4-7 and 12-15 are the lit states |
| `etfuturum:copper_bulb:1` | Exposed Copper Bulb | `wall-accent` | partly | standard, new: industrial |  |
| `etfuturum:copper_bulb:2` | Weathered Copper Bulb | `wall-accent` | partly | standard, new: industrial |  |
| `etfuturum:copper_bulb:3` | Oxidized Copper Bulb | `wall-accent` | partly | standard, new: industrial |  |
| `etfuturum:copper_bulb:4` | Copper Bulb (lit) | `light` | partly | standard, new: industrial | lit states give light 15, 12, 8, 4 by oxidation; a redstone pulse toggles it, so place lit metas directly |
| `etfuturum:copper_bulb:5` | Exposed Copper Bulb (lit) | `light` | partly | standard, new: industrial |  |
| `etfuturum:copper_bulb:6` | Weathered Copper Bulb (lit) | `light` | partly | standard, new: industrial |  |
| `etfuturum:copper_bulb:7` | Oxidized Copper Bulb (lit) | `light` | partly | standard, new: industrial |  |
| `etfuturum:copper_bulb:8` | Waxed Copper Bulb | `wall-accent` | partly | standard, new: industrial |  |
| `etfuturum:copper_bulb:9` | Waxed Exposed Copper Bulb | `wall-accent` | partly | standard, new: industrial |  |
| `etfuturum:copper_bulb:10` | Waxed Weathered Copper Bulb | `wall-accent` | partly | standard, new: industrial |  |
| `etfuturum:copper_bulb:11` | Waxed Oxidized Copper Bulb | `wall-accent` | partly | standard, new: industrial |  |
| `etfuturum:copper_bulb:12` | Waxed Copper Bulb (lit) | `light` | partly | standard, new: industrial |  |
| `etfuturum:copper_bulb:13` | Waxed Exposed Copper Bulb (lit) | `light` | partly | standard, new: industrial |  |
| `etfuturum:copper_bulb:14` | Waxed Weathered Copper Bulb (lit) | `light` | partly | standard, new: industrial |  |
| `etfuturum:copper_bulb:15` | Waxed Oxidized Copper Bulb (lit) | `light` | partly | standard, new: industrial |  |

### Wood: stripped logs, bark, cherry

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `etfuturum:log_stripped:0` | Stripped Oak Log | `trim-pillar`, `wall-accent` | yes | standard, new: old town | axis in meta bits (+4 X, +8 Z), as vanilla logs |
| `etfuturum:log_stripped:1` | Stripped Spruce Log | `trim-pillar`, `wall-accent` | yes | standard, new: old town |  |
| `etfuturum:log_stripped:2` | Stripped Birch Log | `trim-pillar`, `wall-accent` | yes | standard, new: old town |  |
| `etfuturum:log_stripped:3` | Stripped Jungle Log | `trim-pillar`, `wall-accent` | yes | standard, new: old town |  |
| `etfuturum:log2_stripped:0` | Stripped Acacia Log | `trim-pillar`, `wall-accent` | yes | standard, new: old town |  |
| `etfuturum:log2_stripped:1` | Stripped Dark Oak Log | `trim-pillar`, `wall-accent` | yes | standard, new: old town |  |
| `etfuturum:bark:0` | Oak Wood | `trim-pillar`, `wall-accent` | partly | standard, new: old town, park | bark on all six faces (1.13 `<wood>_wood`) |
| `etfuturum:bark:1` | Spruce Wood | `trim-pillar`, `wall-accent` | partly | standard, new: old town, park |  |
| `etfuturum:bark:2` | Birch Wood | `trim-pillar`, `wall-accent` | partly | standard, new: old town, park |  |
| `etfuturum:bark:3` | Jungle Wood | `trim-pillar`, `wall-accent` | partly | standard, new: old town, park |  |
| `etfuturum:bark2:0` | Acacia Wood | `trim-pillar`, `wall-accent` | partly | standard, new: old town, park |  |
| `etfuturum:bark2:1` | Dark Oak Wood | `trim-pillar`, `wall-accent` | partly | standard, new: old town, park |  |
| `etfuturum:wood_stripped:0` | Stripped Oak Wood | `wall-main`, `wall-accent`, `floor` | yes | standard, new: old town | stripped grain on all faces: a plank alternative |
| `etfuturum:wood_stripped:1` | Stripped Spruce Wood | `wall-main`, `wall-accent`, `floor` | yes | standard, new: old town |  |
| `etfuturum:wood_stripped:2` | Stripped Birch Wood | `wall-main`, `wall-accent`, `floor` | yes | standard, new: old town |  |
| `etfuturum:wood_stripped:3` | Stripped Jungle Wood | `wall-main`, `wall-accent`, `floor` | yes | standard, new: old town |  |
| `etfuturum:wood2_stripped:0` | Stripped Acacia Wood | `wall-main`, `wall-accent`, `floor` | yes | standard, new: old town |  |
| `etfuturum:wood2_stripped:1` | Stripped Dark Oak Wood | `wall-main`, `wall-accent`, `floor` | yes | standard, new: old town |  |
| `etfuturum:wood_planks:3` | Cherry Planks | `floor`, `wall-accent` | partly | standard | pink planks; metas 0-2 and 4 (crimson, warped, mangrove, bamboo) exist in the block but their features are off in the pack; stairs `cherry_stairs`, slab `wood_slab:3` |
| `etfuturum:cherry_log:0` | Cherry Log | `trim-pillar`, `foliage` | partly | park | axis in meta |
| `etfuturum:leaves:1` | Cherry Leaves | `foliage` | partly | park | pink blossom leaves; meta 0 (mangrove) is off; decays without a log nearby unless placed with the no-decay bit |
| `etfuturum:sapling:1` | Cherry Sapling | `foliage` | partly | park | meta 0 (mangrove propagule) is off |
| `etfuturum:cherry_trapdoor:0` | Cherry Trapdoor | `door-trap` | partly | standard | meta = trapdoor bits as vanilla |

### Wood: Biomes O' Plenty and Witchery compat

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `etfuturum:bop_log_stripped:0` | Stripped Sacredoak Log (BoP) | `trim-pillar` | partly | new: old town | Biomes O' Plenty wood; registered because BoP is in the pack; axis in meta |
| `etfuturum:bop_log_stripped:1` | Stripped Cherry Log (BoP) | `trim-pillar` | partly | new: old town |  |
| `etfuturum:bop_log_stripped:2` | Stripped Dark Log (BoP) | `trim-pillar` | yes | standard, new: old town |  |
| `etfuturum:bop_log_stripped:3` | Stripped Fir Log (BoP) | `trim-pillar` | yes | standard, new: old town |  |
| `etfuturum:bop_log_stripped2:0` | Stripped Ethereal Log (BoP) | `off-theme` | no | - |  |
| `etfuturum:bop_log_stripped2:1` | Stripped Magic Log (BoP) | `off-theme` | no | - |  |
| `etfuturum:bop_log_stripped2:2` | Stripped Mangrove Log (BoP) | `trim-pillar` | partly | new: old town |  |
| `etfuturum:bop_log_stripped2:3` | Stripped Palm Log (BoP) | `trim-pillar` | partly | new: old town |  |
| `etfuturum:bop_log_stripped3:0` | Stripped Redwood Log (BoP) | `trim-pillar` | yes | standard, new: old town |  |
| `etfuturum:bop_log_stripped3:1` | Stripped Willow Log (BoP) | `trim-pillar` | partly | new: old town |  |
| `etfuturum:bop_log_stripped3:2` | Stripped Dead Log (BoP) | `trim-pillar` | partly | new: old town |  |
| `etfuturum:bop_log_stripped3:3` | Stripped Bigflowerstem Log (BoP) | `off-theme` | no | - |  |
| `etfuturum:bop_log_stripped4:0` | Stripped Pine Log (BoP) | `trim-pillar` | yes | standard, new: old town |  |
| `etfuturum:bop_log_stripped4:1` | Stripped Hellbark Log (BoP) | `off-theme` | no | - |  |
| `etfuturum:bop_log_stripped4:2` | Stripped Jacaranda Log (BoP) | `trim-pillar` | partly | new: old town |  |
| `etfuturum:bop_log_stripped4:3` | Stripped Mahogany Log (BoP) | `trim-pillar` | yes | standard, new: old town |  |
| `etfuturum:bop_wood:0` | Sacredoak Wood (BoP bark) | `trim-pillar`, `wall-accent` | partly | new: old town, park |  |
| `etfuturum:bop_stripped_wood:0` | Stripped Sacredoak Wood (BoP) | `wall-main`, `wall-accent`, `floor` | partly | new: old town |  |
| `etfuturum:bop_wood:1` | Cherry Wood (BoP bark) | `trim-pillar`, `wall-accent` | partly | new: old town, park |  |
| `etfuturum:bop_stripped_wood:1` | Stripped Cherry Wood (BoP) | `wall-main`, `wall-accent`, `floor` | partly | new: old town |  |
| `etfuturum:bop_wood:2` | Dark Wood (BoP bark) | `trim-pillar`, `wall-accent` | partly | new: old town, park |  |
| `etfuturum:bop_stripped_wood:2` | Stripped Dark Wood (BoP) | `wall-main`, `wall-accent`, `floor` | yes | standard, new: old town |  |
| `etfuturum:bop_wood:3` | Fir Wood (BoP bark) | `trim-pillar`, `wall-accent` | partly | new: old town, park |  |
| `etfuturum:bop_stripped_wood:3` | Stripped Fir Wood (BoP) | `wall-main`, `wall-accent`, `floor` | yes | standard, new: old town |  |
| `etfuturum:bop_wood2:0` | Ethereal Wood (BoP bark) | `off-theme` | no | - |  |
| `etfuturum:bop_stripped_wood2:0` | Stripped Ethereal Wood (BoP) | `off-theme` | no | - |  |
| `etfuturum:bop_wood2:1` | Magic Wood (BoP bark) | `off-theme` | no | - |  |
| `etfuturum:bop_stripped_wood2:1` | Stripped Magic Wood (BoP) | `off-theme` | no | - |  |
| `etfuturum:bop_wood2:2` | Mangrove Wood (BoP bark) | `trim-pillar`, `wall-accent` | partly | new: old town, park |  |
| `etfuturum:bop_stripped_wood2:2` | Stripped Mangrove Wood (BoP) | `wall-main`, `wall-accent`, `floor` | partly | new: old town |  |
| `etfuturum:bop_wood2:3` | Palm Wood (BoP bark) | `trim-pillar`, `wall-accent` | partly | new: old town, park |  |
| `etfuturum:bop_stripped_wood2:3` | Stripped Palm Wood (BoP) | `wall-main`, `wall-accent`, `floor` | partly | new: old town |  |
| `etfuturum:bop_wood3:0` | Redwood Wood (BoP bark) | `trim-pillar`, `wall-accent` | partly | new: old town, park |  |
| `etfuturum:bop_stripped_wood3:0` | Stripped Redwood Wood (BoP) | `wall-main`, `wall-accent`, `floor` | yes | standard, new: old town |  |
| `etfuturum:bop_wood3:1` | Willow Wood (BoP bark) | `trim-pillar`, `wall-accent` | partly | new: old town, park |  |
| `etfuturum:bop_stripped_wood3:1` | Stripped Willow Wood (BoP) | `wall-main`, `wall-accent`, `floor` | partly | new: old town |  |
| `etfuturum:bop_wood3:2` | Dead Wood (BoP bark) | `trim-pillar`, `wall-accent` | partly | new: old town, park |  |
| `etfuturum:bop_stripped_wood3:2` | Stripped Dead Wood (BoP) | `wall-main`, `wall-accent`, `floor` | partly | new: old town |  |
| `etfuturum:bop_wood3:3` | Bigflowerstem Wood (BoP bark) | `off-theme` | no | - |  |
| `etfuturum:bop_stripped_wood3:3` | Stripped Bigflowerstem Wood (BoP) | `off-theme` | no | - |  |
| `etfuturum:bop_wood4:0` | Pine Wood (BoP bark) | `trim-pillar`, `wall-accent` | partly | new: old town, park |  |
| `etfuturum:bop_stripped_wood4:0` | Stripped Pine Wood (BoP) | `wall-main`, `wall-accent`, `floor` | yes | standard, new: old town |  |
| `etfuturum:bop_wood4:1` | Hellbark Wood (BoP bark) | `off-theme` | no | - |  |
| `etfuturum:bop_stripped_wood4:1` | Stripped Hellbark Wood (BoP) | `off-theme` | no | - |  |
| `etfuturum:bop_wood4:2` | Jacaranda Wood (BoP bark) | `trim-pillar`, `wall-accent` | partly | new: old town, park |  |
| `etfuturum:bop_stripped_wood4:2` | Stripped Jacaranda Wood (BoP) | `wall-main`, `wall-accent`, `floor` | partly | new: old town |  |
| `etfuturum:bop_wood4:3` | Mahogany Wood (BoP bark) | `trim-pillar`, `wall-accent` | partly | new: old town, park |  |
| `etfuturum:bop_stripped_wood4:3` | Stripped Mahogany Wood (BoP) | `wall-main`, `wall-accent`, `floor` | yes | standard, new: old town |  |
| `etfuturum:witchery_log_stripped:0` | Stripped Rowan Log (Witchery) | `trim-pillar`, `wall-accent` | partly | new: old town | Witchery wood; registered because Witchery is in the pack |
| `etfuturum:witchery_log_stripped:1` | Stripped Alder Log (Witchery) | `trim-pillar`, `wall-accent` | partly | new: old town |  |
| `etfuturum:witchery_log_stripped:2` | Stripped Hawthorn Log (Witchery) | `trim-pillar`, `wall-accent` | partly | new: old town |  |
| `etfuturum:witchery_wood:0` | Rowan Wood (Witchery bark) | `trim-pillar`, `wall-accent` | partly | new: old town |  |
| `etfuturum:witchery_wood:1` | Alder Wood (Witchery bark) | `trim-pillar`, `wall-accent` | partly | new: old town |  |
| `etfuturum:witchery_wood:2` | Hawthorn Wood (Witchery bark) | `trim-pillar`, `wall-accent` | partly | new: old town |  |
| `etfuturum:witchery_stripped_wood:0` | Stripped Rowan Wood (Witchery) | `trim-pillar`, `wall-accent` | partly | new: old town |  |
| `etfuturum:witchery_stripped_wood:1` | Stripped Alder Wood (Witchery) | `trim-pillar`, `wall-accent` | partly | new: old town |  |
| `etfuturum:witchery_stripped_wood:2` | Stripped Hawthorn Wood (Witchery) | `trim-pillar`, `wall-accent` | partly | new: old town |  |

### Slabs, stairs and walls

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `etfuturum:stone_slab:0` | Stone Slab | `stairs-slab`, `floor` | yes | standard, border | raw-stone texture (the modern Stone Slab; vanilla's `minecraft:stone_slab:0` is the smooth one); a separate block, so not an ore host; +8 for the top half; double form `etfuturum:double_stone_slab` |
| `etfuturum:stone_slab:1` | Mossy Cobblestone Slab | `stairs-slab`, `rubble-ruin` | yes | standard, border |  |
| `etfuturum:stone_slab:2` | Mossy Stone Brick Slab | `stairs-slab`, `rubble-ruin` | yes | standard, border |  |
| `etfuturum:stone_slab:3` | Cut Sandstone Slab | `stairs-slab`, `floor` | yes | desert |  |
| `etfuturum:red_sandstone_slab:0` | Red Sandstone Slab | `stairs-slab` | yes | desert | +8 top; `double_red_sandstone_slab` is upstream's `$` in desert |
| `etfuturum:red_sandstone_slab:1` | Cut Red Sandstone Slab | `stairs-slab` | yes | desert |  |
| `etfuturum:smooth_red_sandstone_slab:0` | Smooth Red Sandstone Slab | `stairs-slab` | yes | desert | no double form registered (smooth sandstone is off) |
| `etfuturum:purpur_slab:0` | Purpur Slab | `stairs-slab` | no | new: neon/arcade |  |
| `etfuturum:smooth_quartz_slab:0` | Smooth Quartz Slab | `stairs-slab`, `trim-pillar` | yes | standard, border |  |
| `etfuturum:red_netherbrick_slab:0` | Red Nether Brick Slab | `stairs-slab` | yes | desert, standard |  |
| `etfuturum:end_brick_slab:0` | End Stone Brick Slab | `stairs-slab` | partly | desert |  |
| `etfuturum:deepslate_slab:0` | Cobbled Deepslate Slab | `stairs-slab`, `rubble-ruin` | yes | standard | upstream's `cobbled_deepslate_slab` |
| `etfuturum:deepslate_slab:1` | Polished Deepslate Slab | `stairs-slab` | yes | standard, border |  |
| `etfuturum:deepslate_brick_slab:0` | Deepslate Brick Slab | `stairs-slab` | yes | standard, border |  |
| `etfuturum:deepslate_brick_slab:1` | Deepslate Tile Slab | `stairs-slab`, `roof` | yes | standard, border |  |
| `etfuturum:tuff_slab:0` | Tuff Slab | `stairs-slab` | partly | standard | a slab is a separate block from `etfuturum:tuff`, so not an ore host |
| `etfuturum:tuff_slab:1` | Polished Tuff Slab | `stairs-slab` | yes | standard |  |
| `etfuturum:tuff_slab:2` | Tuff Brick Slab | `stairs-slab` | yes | standard |  |
| `etfuturum:cut_copper_slab:0` | Cut Copper Slab | `stairs-slab` | yes | standard, new: old town | metas 4-7 waxed |
| `etfuturum:cut_copper_slab:1` | Exposed Cut Copper Slab | `stairs-slab` | yes | standard, new: old town |  |
| `etfuturum:cut_copper_slab:2` | Weathered Cut Copper Slab | `stairs-slab` | yes | standard, new: old town |  |
| `etfuturum:cut_copper_slab:3` | Oxidized Cut Copper Slab | `stairs-slab` | yes | standard, new: old town |  |
| `etfuturum:cut_copper_slab:4` | Waxed Cut Copper Slab | `stairs-slab` | yes | standard, new: old town |  |
| `etfuturum:cut_copper_slab:5` | Waxed Exposed Cut Copper Slab | `stairs-slab` | yes | standard, new: old town |  |
| `etfuturum:cut_copper_slab:6` | Waxed Weathered Cut Copper Slab | `stairs-slab` | yes | standard, new: old town |  |
| `etfuturum:cut_copper_slab:7` | Waxed Oxidized Cut Copper Slab | `stairs-slab` | yes | standard, new: old town |  |
| `etfuturum:blackstone_slab:0` | Blackstone Slab | `stairs-slab` | yes | standard, border |  |
| `etfuturum:blackstone_slab:1` | Polished Blackstone Slab | `stairs-slab` | yes | standard, border | upstream's `polished_blackstone_slab` is meta 1 |
| `etfuturum:blackstone_slab:2` | Polished Blackstone Brick Slab | `stairs-slab` | yes | standard, border |  |
| `etfuturum:wood_slab:3` | Cherry Slab | `stairs-slab` | partly | standard | other metas off |
| `etfuturum:smooth_red_sandstone_stairs:0-7` | Smooth Red Sandstone Stairs | `stairs-slab` | yes | desert | meta = facing and half as vanilla stairs (east 0, west 1, south 2, north 3, +4 upside down) |
| `etfuturum:smooth_quartz_stairs:0-7` | Smooth Quartz Stairs | `stairs-slab` | yes | standard, border |  |
| `etfuturum:red_netherbrick_stairs:0-7` | Red Nether Brick Stairs | `stairs-slab` | yes | desert, standard |  |
| `etfuturum:mossy_stone_brick_stairs:0-7` | Mossy Stone Brick Stairs | `stairs-slab` | yes | standard | upstream's |
| `etfuturum:mossy_cobblestone_stairs:0-7` | Mossy Cobblestone Stairs | `stairs-slab` | partly | standard, new: old town |  |
| `etfuturum:stone_stairs:0-7` | Stone Stairs | `stairs-slab` | yes | standard, border | 1.20 `stone_stairs`: raw-stone texture, not smooth stone; *not* `minecraft:stone_stairs` (cobblestone) in 1.7.10; a separate block, so not an ore host |
| `etfuturum:end_brick_stairs:0-7` | End Stone Brick Stairs | `stairs-slab` | partly | desert |  |
| `etfuturum:red_sandstone_stairs:0-7` | Red Sandstone Stairs | `stairs-slab` | yes | desert |  |
| `etfuturum:purpur_stairs:0-7` | Purpur Stairs | `off-theme` | no | new: neon/arcade |  |
| `etfuturum:cobbled_deepslate_stairs:0-7` | Cobbled Deepslate Stairs | `stairs-slab` | yes | standard |  |
| `etfuturum:polished_deepslate_stairs:0-7` | Polished Deepslate Stairs | `stairs-slab` | yes | standard, border |  |
| `etfuturum:deepslate_brick_stairs:0-7` | Deepslate Brick Stairs | `stairs-slab` | yes | standard, border |  |
| `etfuturum:deepslate_tile_stairs:0-7` | Deepslate Tile Stairs | `stairs-slab`, `roof` | yes | standard, border |  |
| `etfuturum:tuff_stairs:0-7` | Tuff Stairs | `stairs-slab` | partly | standard |  |
| `etfuturum:polished_tuff_stairs:0-7` | Polished Tuff Stairs | `stairs-slab` | yes | standard |  |
| `etfuturum:tuff_brick_stairs:0-7` | Tuff Brick Stairs | `stairs-slab` | yes | standard |  |
| `etfuturum:cut_copper_stairs:0-7` | Cut Copper Stairs | `stairs-slab` | yes | standard, new: old town |  |
| `etfuturum:exposed_cut_copper_stairs:0-7` | Exposed Cut Copper Stairs | `stairs-slab` | yes | standard, new: old town | upstream's; unwaxed stairs weather |
| `etfuturum:weathered_cut_copper_stairs:0-7` | Weathered Cut Copper Stairs | `stairs-slab` | yes | standard, new: old town |  |
| `etfuturum:oxidized_cut_copper_stairs:0-7` | Oxidized Cut Copper Stairs | `stairs-slab` | yes | standard, new: old town |  |
| `etfuturum:waxed_cut_copper_stairs:0-7` | Waxed Cut Copper Stairs | `stairs-slab` | yes | standard, new: old town |  |
| `etfuturum:waxed_exposed_cut_copper_stairs:0-7` | Waxed Exposed Cut Copper Stairs | `stairs-slab` | yes | standard, new: old town |  |
| `etfuturum:waxed_weathered_cut_copper_stairs:0-7` | Waxed Weathered Cut Copper Stairs | `stairs-slab` | yes | standard, new: old town |  |
| `etfuturum:waxed_oxidized_cut_copper_stairs:0-7` | Waxed Oxidized Cut Copper Stairs | `stairs-slab` | yes | standard, new: old town |  |
| `etfuturum:blackstone_stairs:0-7` | Blackstone Stairs | `stairs-slab` | partly | standard |  |
| `etfuturum:polished_blackstone_stairs:0-7` | Polished Blackstone Stairs | `stairs-slab` | yes | standard, border |  |
| `etfuturum:polished_blackstone_brick_stairs:0-7` | Polished Blackstone Brick Stairs | `stairs-slab` | yes | standard, border |  |
| `etfuturum:cherry_stairs:0-7` | Cherry Stairs | `stairs-slab` | partly | standard |  |
| `etfuturum:netherite_stairs:0-7` | Netherite Stairs | `off-theme` | no | - | valuable; never in a city |
| `etfuturum:stone_wall:0` | Stone Brick Wall | `fence-rail` | yes | standard, border | walls connect to neighbors (no state in meta); a low parapet or fence |
| `etfuturum:stone_wall:1` | Mossy Stone Brick Wall | `fence-rail` | yes | standard |  |
| `etfuturum:stone_wall:2` | Sandstone Wall | `fence-rail` | yes | desert |  |
| `etfuturum:stone_wall:3` | Brick Wall | `fence-rail` | yes | standard, border |  |
| `etfuturum:nether_brick_wall:0` | Nether Brick Wall | `fence-rail` | partly | standard, new: old town |  |
| `etfuturum:red_sandstone_wall:0` | Red Sandstone Wall | `fence-rail` | yes | desert |  |
| `etfuturum:red_nether_brick_wall:0` | Red Nether Brick Wall | `fence-rail` | yes | desert |  |
| `etfuturum:end_brick_wall:0` | End Stone Brick Wall | `fence-rail` | partly | desert |  |
| `etfuturum:deepslate_wall:0` | Cobbled Deepslate Wall | `fence-rail` | yes | standard |  |
| `etfuturum:deepslate_wall:1` | Polished Deepslate Wall | `fence-rail` | yes | standard, border |  |
| `etfuturum:deepslate_brick_wall:0` | Deepslate Brick Wall | `fence-rail` | yes | standard, border |  |
| `etfuturum:deepslate_brick_wall:1` | Deepslate Tile Wall | `fence-rail` | yes | standard, border |  |
| `etfuturum:tuff_wall:0` | Tuff Wall | `fence-rail` | partly | standard |  |
| `etfuturum:tuff_wall:1` | Polished Tuff Wall | `fence-rail` | yes | standard |  |
| `etfuturum:tuff_wall:2` | Tuff Brick Wall | `fence-rail` | yes | standard |  |
| `etfuturum:blackstone_wall:0` | Blackstone Wall | `fence-rail` | partly | standard |  |
| `etfuturum:blackstone_wall:1` | Polished Blackstone Wall | `fence-rail` | yes | standard |  |
| `etfuturum:blackstone_wall:2` | Polished Blackstone Brick Wall | `fence-rail` | yes | standard |  |

### Lights, glass, furniture, doors and barrels

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `etfuturum:lantern:0` | Lantern | `light`, `furniture-deco` | yes | standard, border, new: old town | light 15; meta 0 standing, 1 hanging (upstream's `lantern[hanging=true]`) |
| `etfuturum:soul_lantern:0` | Soul Lantern | `light`, `furniture-deco` | partly | new: old town | light 10; blue flame |
| `etfuturum:chain:0` | Chain | `furniture-deco`, `fence-rail` | yes | standard, new: industrial | upstream's `chain`; meta = axis (0 Y, 1 X, 2 Z) |
| `etfuturum:end_rod:0` | End Rod | `light` | partly | new: tech district | light 14; thin white rod; meta = facing |
| `etfuturum:soul_torch:0` | Soul Torch | `light` | partly | new: old town | light 10; torch meta rules |
| `etfuturum:tinted_glass:0` | Tinted Glass | `glass` | yes | standard, border | dark translucent glass that blocks light like an opaque block; no pane form |
| `etfuturum:barrel:0` | Barrel | `furniture-deco` | yes | standard, new: old town | upstream's `barrel`; tile entity with an inventory (loot); meta = facing (+8 open) |
| `etfuturum:target:0` | Target | `furniture-deco` | partly | park | redstone output when hit by a projectile |
| `etfuturum:observer:0` | Observer | `off-theme` | no | - | redstone component |
| `etfuturum:smoker:0` | Smoker | `furniture-deco` | yes | standard | tile entity; kitchen and restaurant furniture |
| `etfuturum:blast_furnace:0` | Blast Furnace | `furniture-deco` | yes | standard, new: industrial | tile entity; workshop furniture |
| `etfuturum:smithing_table:0` | Smithing Table | `furniture-deco` | yes | standard, new: industrial | no tile entity |
| `etfuturum:stonecutter:0` | Stonecutter | `furniture-deco` | partly | new: industrial | not a full block |
| `etfuturum:fletching_table:0` | Fletching Table | `furniture-deco` | partly | standard |  |
| `etfuturum:cartography_table:0` | Cartography Table | `furniture-deco` | yes | standard | office or library furniture |
| `etfuturum:loom:0` | Loom | `furniture-deco` | partly | standard |  |
| `etfuturum:brewing_stand:0` | Brewing Stand (Et Futurum) | `furniture-deco` | partly | standard | light 2; tile entity; replaces the vanilla stand's behavior |
| `etfuturum:enchantment_table:0` | Enchanting Table (Et Futurum) | `furniture-deco` | partly | standard | tile entity |
| `etfuturum:beacon:0` | Beacon (Et Futurum) | `off-theme` | no | - | light 15; valuable; tile entity |
| `etfuturum:banner:0` | Banner | `furniture-deco` | yes | standard, border | tile entity: base color and patterns live in NBT; meta = rotation (standing) or facing (wall) |
| `etfuturum:shulker_box:0` | Shulker Box | `off-theme` | no | - | tile entity with inventory; color in NBT |
| `etfuturum:white_bed:0` | White Bed | `furniture-deco` | yes | standard | two-block bed, meta = facing and head bit as vanilla; red is vanilla's `minecraft:bed`; sets spawn when used |
| `etfuturum:orange_bed:0` | Orange Bed | `furniture-deco` | partly | standard |  |
| `etfuturum:magenta_bed:0` | Magenta Bed | `furniture-deco` | partly | standard |  |
| `etfuturum:light_blue_bed:0` | Light Blue Bed | `furniture-deco` | yes | standard |  |
| `etfuturum:yellow_bed:0` | Yellow Bed | `furniture-deco` | partly | standard |  |
| `etfuturum:lime_bed:0` | Lime Bed | `furniture-deco` | partly | standard |  |
| `etfuturum:pink_bed:0` | Pink Bed | `furniture-deco` | partly | standard |  |
| `etfuturum:gray_bed:0` | Gray Bed | `furniture-deco` | yes | standard |  |
| `etfuturum:light_gray_bed:0` | Light Gray Bed | `furniture-deco` | yes | standard |  |
| `etfuturum:cyan_bed:0` | Cyan Bed | `furniture-deco` | yes | standard |  |
| `etfuturum:purple_bed:0` | Purple Bed | `furniture-deco` | partly | standard |  |
| `etfuturum:blue_bed:0` | Blue Bed | `furniture-deco` | yes | standard |  |
| `etfuturum:brown_bed:0` | Brown Bed | `furniture-deco` | yes | standard |  |
| `etfuturum:green_bed:0` | Green Bed | `furniture-deco` | yes | standard |  |
| `etfuturum:black_bed:0` | Black Bed | `furniture-deco` | yes | standard |  |
| `etfuturum:copper_door:0-11` | Copper Door | `door-trap` | yes | standard, new: industrial | opened by hand (unlike iron doors); vanilla door meta layout; the only metal-look doors besides iron |
| `etfuturum:waxed_copper_door:0-11` | Waxed Copper Door | `door-trap` | yes | standard, new: industrial |  |
| `etfuturum:exposed_copper_door:0-11` | Exposed Copper Door | `door-trap` | yes | standard, new: industrial |  |
| `etfuturum:waxed_exposed_copper_door:0-11` | Waxed Exposed Copper Door | `door-trap` | yes | standard, new: industrial |  |
| `etfuturum:weathered_copper_door:0-11` | Weathered Copper Door | `door-trap` | yes | standard, new: industrial |  |
| `etfuturum:waxed_weathered_copper_door:0-11` | Waxed Weathered Copper Door | `door-trap` | yes | standard, new: industrial |  |
| `etfuturum:oxidized_copper_door:0-11` | Oxidized Copper Door | `door-trap` | yes | standard, new: industrial |  |
| `etfuturum:waxed_oxidized_copper_door:0-11` | Waxed Oxidized Copper Door | `door-trap` | yes | standard, new: industrial |  |
| `etfuturum:copper_trapdoor:0-15` | Copper Trapdoor | `door-trap`, `fence-rail` | yes | standard, new: industrial | a metal trapdoor while `enableIronTrapdoor=false`; hatch, shutter or railing |
| `etfuturum:waxed_copper_trapdoor:0-15` | Waxed Copper Trapdoor | `door-trap`, `fence-rail` | yes | standard, new: industrial |  |
| `etfuturum:exposed_copper_trapdoor:0-15` | Exposed Copper Trapdoor | `door-trap`, `fence-rail` | yes | standard, new: industrial |  |
| `etfuturum:waxed_exposed_copper_trapdoor:0-15` | Waxed Exposed Copper Trapdoor | `door-trap`, `fence-rail` | yes | standard, new: industrial |  |
| `etfuturum:weathered_copper_trapdoor:0-15` | Weathered Copper Trapdoor | `door-trap`, `fence-rail` | yes | standard, new: industrial |  |
| `etfuturum:waxed_weathered_copper_trapdoor:0-15` | Waxed Weathered Copper Trapdoor | `door-trap`, `fence-rail` | yes | standard, new: industrial |  |
| `etfuturum:oxidized_copper_trapdoor:0-15` | Oxidized Copper Trapdoor | `door-trap`, `fence-rail` | yes | standard, new: industrial |  |
| `etfuturum:waxed_oxidized_copper_trapdoor:0-15` | Waxed Oxidized Copper Trapdoor | `door-trap`, `fence-rail` | yes | standard, new: industrial |  |
| `etfuturum:polished_blackstone_pressure_plate:0` | Polished Blackstone Pressure Plate | `furniture-deco` | partly | standard |  |
| `etfuturum:polished_blackstone_button:0` | Polished Blackstone Button | `furniture-deco` | partly | standard |  |
| `etfuturum:modded_raw_ore_block:0-13` | Raw Metal Blocks (aluminum, tin, silver, lead, ...) | `off-theme` | no | - | raw-ore storage blocks for other mods' metals; valuable and off-theme |
| `etfuturum:iron_barrel:0` | Iron Barrel | `off-theme` | no | - | Iron Chests-tier barrel (larger inventory); `barrelIronChest=true` and Iron Chests is installed; metal trim reads as valuable |
| `etfuturum:gold_barrel:0` | Gold Barrel | `off-theme` | no | - |  |
| `etfuturum:diamond_barrel:0` | Diamond Barrel | `off-theme` | no | - |  |
| `etfuturum:copper_barrel:0` | Copper Barrel | `off-theme` | no | - |  |
| `etfuturum:silver_barrel:0` | Silver Barrel | `off-theme` | no | - |  |
| `etfuturum:steel_barrel:0` | Steel Barrel | `off-theme` | no | - |  |
| `etfuturum:obsidian_barrel:0` | Obsidian Barrel | `off-theme` | no | - |  |
| `etfuturum:darksteel_barrel:0` | Dark Steel Barrel | `off-theme` | no | - |  |
| `etfuturum:netherite_barrel:0` | Netherite Barrel | `off-theme` | no | - |  |

### Wood buttons, pressure plates and signs

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `etfuturum:button_spruce:0` | Spruce Button | `furniture-deco` | partly | standard | wood buttons and plates exist so a wood-matched switch can sit on any wall |
| `etfuturum:pressure_plate_spruce:0` | Spruce Pressure Plate | `furniture-deco` | yes | standard | upstream's `spruce_pressure_plate` |
| `etfuturum:sign_spruce:0-15` | Spruce Sign (standing) | `furniture-deco` | partly | standard | tile entity (text); no item form; meta = rotation |
| `etfuturum:wall_sign_spruce:2-5` | Spruce Wall Sign | `furniture-deco` | partly | standard | tile entity; meta = facing |
| `etfuturum:button_birch:0` | Birch Button | `furniture-deco` | partly | standard |  |
| `etfuturum:pressure_plate_birch:0` | Birch Pressure Plate | `furniture-deco` | partly | standard |  |
| `etfuturum:sign_birch:0-15` | Birch Sign (standing) | `furniture-deco` | partly | standard |  |
| `etfuturum:wall_sign_birch:2-5` | Birch Wall Sign | `furniture-deco` | partly | standard |  |
| `etfuturum:button_jungle:0` | Jungle Button | `furniture-deco` | partly | standard |  |
| `etfuturum:pressure_plate_jungle:0` | Jungle Pressure Plate | `furniture-deco` | partly | standard |  |
| `etfuturum:sign_jungle:0-15` | Jungle Sign (standing) | `furniture-deco` | partly | standard |  |
| `etfuturum:wall_sign_jungle:2-5` | Jungle Wall Sign | `furniture-deco` | partly | standard |  |
| `etfuturum:button_acacia:0` | Acacia Button | `furniture-deco` | partly | standard |  |
| `etfuturum:pressure_plate_acacia:0` | Acacia Pressure Plate | `furniture-deco` | partly | standard |  |
| `etfuturum:sign_acacia:0-15` | Acacia Sign (standing) | `furniture-deco` | partly | standard |  |
| `etfuturum:wall_sign_acacia:2-5` | Acacia Wall Sign | `furniture-deco` | partly | standard |  |
| `etfuturum:button_dark_oak:0` | Dark Oak Button | `furniture-deco` | partly | standard |  |
| `etfuturum:pressure_plate_dark_oak:0` | Dark Oak Pressure Plate | `furniture-deco` | partly | standard |  |
| `etfuturum:sign_dark_oak:0-15` | Dark Oak Sign (standing) | `furniture-deco` | partly | standard |  |
| `etfuturum:wall_sign_dark_oak:2-5` | Dark Oak Wall Sign | `furniture-deco` | partly | standard |  |
| `etfuturum:cherry_button:0` | Cherry Button | `furniture-deco` | partly | standard |  |
| `etfuturum:cherry_pressure_plate:0` | Cherry Pressure Plate | `furniture-deco` | partly | standard |  |
| `etfuturum:cherry_sign:0-15` | Cherry Sign | `furniture-deco` | partly | standard | tile entity |
| `etfuturum:cherry_wall_sign:2-5` | Cherry Wall Sign | `furniture-deco` | partly | standard | tile entity |
| `etfuturum:bop_sacredoak_button:0` | Sacredoak (BoP) Button | `furniture-deco` | partly | standard | one of 18 BoP/Witchery wood sets of button, pressure plate and signs (signs are tile entities) |
| `etfuturum:bop_sacredoak_pressure_plate:0` | Sacredoak (BoP) Pressure Plate | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_sacredoak_sign:0-15` | Sacredoak (BoP) Sign | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_sacredoak_wall_sign:2-5` | Sacredoak (BoP) Wall Sign | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_cherry_button:0` | Cherry (BoP) Button | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_cherry_pressure_plate:0` | Cherry (BoP) Pressure Plate | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_cherry_sign:0-15` | Cherry (BoP) Sign | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_cherry_wall_sign:2-5` | Cherry (BoP) Wall Sign | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_dark_button:0` | Dark (BoP) Button | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_dark_pressure_plate:0` | Dark (BoP) Pressure Plate | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_dark_sign:0-15` | Dark (BoP) Sign | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_dark_wall_sign:2-5` | Dark (BoP) Wall Sign | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_fir_button:0` | Fir (BoP) Button | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_fir_pressure_plate:0` | Fir (BoP) Pressure Plate | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_fir_sign:0-15` | Fir (BoP) Sign | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_fir_wall_sign:2-5` | Fir (BoP) Wall Sign | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_ethereal_button:0` | Ethereal (BoP) Button | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_ethereal_pressure_plate:0` | Ethereal (BoP) Pressure Plate | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_ethereal_sign:0-15` | Ethereal (BoP) Sign | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_ethereal_wall_sign:2-5` | Ethereal (BoP) Wall Sign | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_magic_button:0` | Magic (BoP) Button | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_magic_pressure_plate:0` | Magic (BoP) Pressure Plate | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_magic_sign:0-15` | Magic (BoP) Sign | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_magic_wall_sign:2-5` | Magic (BoP) Wall Sign | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_mangrove_button:0` | Mangrove (BoP) Button | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_mangrove_pressure_plate:0` | Mangrove (BoP) Pressure Plate | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_mangrove_sign:0-15` | Mangrove (BoP) Sign | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_mangrove_wall_sign:2-5` | Mangrove (BoP) Wall Sign | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_palm_button:0` | Palm (BoP) Button | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_palm_pressure_plate:0` | Palm (BoP) Pressure Plate | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_palm_sign:0-15` | Palm (BoP) Sign | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_palm_wall_sign:2-5` | Palm (BoP) Wall Sign | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_redwood_button:0` | Redwood (BoP) Button | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_redwood_pressure_plate:0` | Redwood (BoP) Pressure Plate | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_redwood_sign:0-15` | Redwood (BoP) Sign | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_redwood_wall_sign:2-5` | Redwood (BoP) Wall Sign | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_willow_button:0` | Willow (BoP) Button | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_willow_pressure_plate:0` | Willow (BoP) Pressure Plate | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_willow_sign:0-15` | Willow (BoP) Sign | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_willow_wall_sign:2-5` | Willow (BoP) Wall Sign | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_bamboo_button:0` | Bamboo (BoP) Button | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_bamboo_pressure_plate:0` | Bamboo (BoP) Pressure Plate | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_bamboo_sign:0-15` | Bamboo (BoP) Sign | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_bamboo_wall_sign:2-5` | Bamboo (BoP) Wall Sign | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_pine_button:0` | Pine (BoP) Button | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_pine_pressure_plate:0` | Pine (BoP) Pressure Plate | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_pine_sign:0-15` | Pine (BoP) Sign | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_pine_wall_sign:2-5` | Pine (BoP) Wall Sign | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_hellbark_button:0` | Hellbark (BoP) Button | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_hellbark_pressure_plate:0` | Hellbark (BoP) Pressure Plate | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_hellbark_sign:0-15` | Hellbark (BoP) Sign | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_hellbark_wall_sign:2-5` | Hellbark (BoP) Wall Sign | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_jacaranda_button:0` | Jacaranda (BoP) Button | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_jacaranda_pressure_plate:0` | Jacaranda (BoP) Pressure Plate | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_jacaranda_sign:0-15` | Jacaranda (BoP) Sign | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_jacaranda_wall_sign:2-5` | Jacaranda (BoP) Wall Sign | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_mahogany_button:0` | Mahogany (BoP) Button | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_mahogany_pressure_plate:0` | Mahogany (BoP) Pressure Plate | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_mahogany_sign:0-15` | Mahogany (BoP) Sign | `furniture-deco` | partly | standard |  |
| `etfuturum:bop_mahogany_wall_sign:2-5` | Mahogany (BoP) Wall Sign | `furniture-deco` | partly | standard |  |
| `etfuturum:witchery_rowan_button:0` | Rowan (Witchery) Button | `furniture-deco` | partly | standard |  |
| `etfuturum:witchery_rowan_pressure_plate:0` | Rowan (Witchery) Pressure Plate | `furniture-deco` | partly | standard |  |
| `etfuturum:witchery_rowan_sign:0-15` | Rowan (Witchery) Sign | `furniture-deco` | partly | standard |  |
| `etfuturum:witchery_rowan_wall_sign:2-5` | Rowan (Witchery) Wall Sign | `furniture-deco` | partly | standard |  |
| `etfuturum:witchery_alder_button:0` | Alder (Witchery) Button | `furniture-deco` | partly | standard |  |
| `etfuturum:witchery_alder_pressure_plate:0` | Alder (Witchery) Pressure Plate | `furniture-deco` | partly | standard |  |
| `etfuturum:witchery_alder_sign:0-15` | Alder (Witchery) Sign | `furniture-deco` | partly | standard |  |
| `etfuturum:witchery_alder_wall_sign:2-5` | Alder (Witchery) Wall Sign | `furniture-deco` | partly | standard |  |
| `etfuturum:witchery_hawthorn_button:0` | Hawthorn (Witchery) Button | `furniture-deco` | partly | standard |  |
| `etfuturum:witchery_hawthorn_pressure_plate:0` | Hawthorn (Witchery) Pressure Plate | `furniture-deco` | partly | standard |  |
| `etfuturum:witchery_hawthorn_sign:0-15` | Hawthorn (Witchery) Sign | `furniture-deco` | partly | standard |  |
| `etfuturum:witchery_hawthorn_wall_sign:2-5` | Hawthorn (Witchery) Wall Sign | `furniture-deco` | partly | standard |  |

## Best variety pools

Each pool adds variety beside the D19 baseline. "Stairs/slab" means the block has a matching stair or slab here.

| Upstream role (palette char) | Baseline (D19) | Et Futurum variety |
|---|---|---|
| Stone-brick wall `#` (`bricks_standard`, border) | `minecraft:stonebrick` | `deepslate_bricks:0` (stairs/slab/wall), `tuff:2` tuff bricks (stairs/slab/wall), `blackstone:2` (stairs/slab/wall), `quartz_bricks`, `polished_deepslate` |
| Accent `X`, band `$` | stone brick, smooth stone double slab | `smooth_stone`, `deepslate_bricks:4`, `tuff:3-4`, `polished_tuff`, `chiseled_copper`, `calcite` |
| Quartz `Q` and glass-side quartz `@` | `minecraft:quartz_block` | `smooth_quartz` (stairs/slab), `quartz_bricks`, `calcite` |
| Terracotta walls (`bricks_cyan`, `_gray`, `_silver`) | `stained_hardened_clay` 9, 7, 8 | `concrete` 9, 7, 8, 0, 15 (no boulder-host risk) |
| Desert walls and `$` | cut sandstone, orange/red terracotta, red sandstone double slab | `red_sandstone` 0-2, `smooth_red_sandstone` (stairs/slab), `red_netherbrick:0` (stairs/slab/wall), `end_bricks` (stairs/slab/wall), `calcite`, `concrete:1`, `concrete:14` |
| Street `S` and floors | smooth stone double slab | `smooth_stone`, `polished_deepslate`, `polished_blackstone`, `smooth_red_sandstone` (desert), `concrete` 7, 8, 15 |
| Sidewalk, plaza | (none) | `concrete:8`, `concrete:0`, `grass_path` in parks |
| Roof | (walls or slabs) | `deepslate_bricks:2` tiles with stairs/slab. Copper stays off roofs: under D29 it is an accent metal behind `decorativeMetals.copper` ([index](index.md#decorative-metals-d29)) |
| Rubble `}` and ruins | wall rubble variant | `deepslate_bricks:1`, `:3`, `blackstone:3`, `cobbled_deepslate`, `red_netherbrick:1`, `coarse_dirt`, `glow_lichen` |
| Glass `+` | glass, stained glass | `tinted_glass` (dark, light-blocking) |
| Light `h` | glowstone | `lantern` (hanging or standing), lit `copper_bulb` 4-7, `end_rod`; `soul_lantern` in old-town or ruin styles |
| Iron bars `:`, rails, `{` chain | iron bars, `chain` | `chain`, `copper_grate`, copper trapdoors as railings, Et Futurum walls as parapets |
| Doors | oak and iron doors | copper doors (weathered look in ruins), copper and cherry trapdoors |
| Furniture `F` | furnace, crafting table, brewing stand, anvil, cauldron, enchanting table | `smoker`, `blast_furnace`, `smithing_table`, `cartography_table`, `loom`, `fletching_table`, `barrel` (loot), beds, banners |
| Wood trim and floors | oak planks, logs | stripped logs and stripped wood of all six vanilla woods, BoP fir, pine, redwood, mahogany, dark |
| Park planting `p`, `K` | flowers, saplings | `cornflower`, `lily_of_the_valley`, `rose`, `pink_petals`, cherry sapling and leaves, `coarse_dirt`, `grass_path` |
