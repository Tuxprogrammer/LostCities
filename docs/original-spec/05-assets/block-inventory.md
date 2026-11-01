# Block inventory

Every distinct block id that upstream Lost Cities places or names, at `f18a094d`: in the bundled assets (palettes, variants, local palettes of buildings and parts, stuff matchers) and in the Java code that writes blocks during generation. This is raw material for the 1.7.10 block mapping; it does not choose pack blocks (that is the new spec's job, per [0004](../../design/0004-gtnh-layer-and-tier-ceiling.md)).

## Summary

| Source | Distinct ids | Present in vanilla 1.7.10 (same or renamed) | No 1.7.10 vanilla equivalent |
|---|---|---|---|
| Bundled assets | 111 (110 valid + 1 malformed) | 77 | 34 (including the malformed `red_sandstone@2` and the `structure_void` marker) |
| Java worldgen code, not already in the assets | 17 | 11 | 6 |
| **Worldgen total** | **128** | **88** | **40** |
| Debug command `/lostcities testfill` (separate, [below](#debug-command-blocks)) | 88 | 53 | 35 |

How to read the 1.7.10 column:

- **yes (same name)**: the 1.7.10 registry name is the same, possibly with metadata for the state.
- **yes (renamed)**: the block exists in 1.7.10 under another name or as a metadata value of another block (the 1.13 "flattening" split many blocks). Metadata values follow the vanilla 1.12 → 1.13 flattening table, which uses the same metadata layout as 1.7.10 for these blocks.
- **no 1.7.10 vanilla equivalent**: added after 1.7.10 or only in its later form. Pack mods may supply one (Et Futurum Requiem, Chisel and others); the new spec decides.

Properties written in the assets are listed as found. States the chunk driver recomputes from neighbors (fence, pane, bar and wall connections, stair `shape`) only need a starting value ([parts-and-placement.md](parts-and-placement.md#part-placement)). Ids are written in the assets without properties where the block's default state is meant; the default state of each 1.20 block then applies (for example `brick_stairs` faces north).

"Uses" counts occurrences: one per palette entry, list entry or matcher entry that names the block (a block used in `block` and as `damaged` counts twice). "Kind": single = `block`, random = an entry in a `blocks` list, variant = an entry in a variant, damaged = a `damaged` target, matcher only = named in a stuff block matcher, never placed.

## Blocks in the bundled assets

| Block ID | Properties used (x uses) | Uses | Assets | Kind | 1.7.10 vanilla |
|---|---|---|---|---|---|
| `minecraft:acacia_sapling` | `stage=1` x2 | 2 | palette:common, part:park_trees | random | yes (renamed): `sapling` meta 4 (+8 for `stage=1`) |
| `minecraft:air` | none | 4 | palette:common x2, stuff:chains, stuff:cobweb | single,random,matcher only | yes (same name): `air` |
| `minecraft:andesite` | none | 1 | stuff:example | matcher only | **no 1.7.10 vanilla equivalent**: added in 1.8 |
| `minecraft:anvil` | `facing=north` x1 | 1 | palette:common | random | yes (same name): `anvil`, orientation in meta bits 0-1 |
| `minecraft:barrel` | `facing=east,open=false` x1; `facing=north` x1; `facing=south,open=false` x1; `facing=west,open=false` x1 | 4 | palette:oilrig x3, part:cabin | single | **no 1.7.10 vanilla equivalent**: added in 1.14 |
| `minecraft:birch_sapling` | `stage=1` x2 | 2 | palette:common, part:park_trees | random | yes (renamed): `sapling` meta 2 (+8 for `stage=1`) |
| `minecraft:black_concrete` | none | 1 | part:highway_gas_station | single | **no 1.7.10 vanilla equivalent**: concrete added in 1.12 |
| `minecraft:black_stained_glass` | none | 1 | palette:oilrig | single | yes (renamed): `stained_glass` meta 15 |
| `minecraft:black_terracotta` | none | 2 | palette:bricks_cyan, palette:bricks_gray | single | yes (renamed): `stained_hardened_clay` meta 15 |
| `minecraft:blue_stained_glass` | none | 4 | palette:common, palette:glass_full_blue x2, palette:glass_pane_blue | single,random | yes (renamed): `stained_glass` meta 11 |
| `minecraft:blue_stained_glass_pane` | none | 3 | palette:glass_full_blue, palette:glass_pane_blue x2 | single | yes (renamed): `stained_glass_pane` meta 11 |
| `minecraft:bookshelf` | none | 1 | palette:common | single | yes (same name): `bookshelf` |
| `minecraft:brewing_stand` | none | 1 | palette:common | random | yes (same name): `brewing_stand` (block; tile entity `Cauldron`) |
| `minecraft:brick_slab` | none | 1 | variant:bricks_rubble | variant | yes (renamed): `stone_slab` meta 4 |
| `minecraft:brick_stairs` | none | 1 | variant:bricks_rubble | variant | yes (same name): `brick_stairs` (default state = meta 0, ascending east; 1.20 default faces north = meta 3) |
| `minecraft:bricks` | none | 3 | palette:default, variant:bricks, variant:bricks_rubble | single,variant | yes (renamed): `brick_block` |
| `minecraft:brown_terracotta` | none | 1 | palette:bricks_silver | single | yes (renamed): `stained_hardened_clay` meta 12 |
| `minecraft:cauldron` | none | 1 | palette:common | random | yes (same name): `cauldron` |
| `minecraft:chain` | none | 1 | palette:common | single | **no 1.7.10 vanilla equivalent**: added in 1.16 |
| `minecraft:chest` | `facing=north` x1 | 1 | palette:common | single | yes (same name): `chest` meta 2 for `facing=north` |
| `minecraft:cobbled_deepslate_slab` | `type=bottom` x1 | 1 | variant:deepslate_rubble | variant | **no 1.7.10 vanilla equivalent**: added in 1.17 |
| `minecraft:cobbled_deepslate_stairs` | `facing=north,half=bottom` x1 | 1 | variant:deepslate_rubble | variant | **no 1.7.10 vanilla equivalent**: added in 1.17 |
| `minecraft:cobblestone_wall` | none | 2 | palette:default, palette:default_desert | single | yes (same name): `cobblestone_wall` meta 0 |
| `minecraft:cobweb` | none | 3 | palette:common x3 | single,random | yes (renamed): `web` |
| `minecraft:cracked_deepslate_tiles` | none | 2 | variant:deepslate, variant:deepslate_rubble | variant | **no 1.7.10 vanilla equivalent**: added in 1.17 |
| `minecraft:cracked_polished_blackstone_bricks` | none | 3 | palette:oilrig, variant:blackstone, variant:blackstone_rubble | single,variant | **no 1.7.10 vanilla equivalent**: added in 1.16 |
| `minecraft:cracked_stone_bricks` | none | 6 | palette:default_desert x2, variant:stoneandbrick, variant:stoneandbrick_rubble, variant:stonebrick, variant:stonebrick_rubble | random,variant | yes (renamed): `stonebrick` meta 2 |
| `minecraft:crafting_table` | none | 1 | palette:common | random | yes (same name): `crafting_table` |
| `minecraft:cut_sandstone` | none | 3 | palette:bricks_desert x2, palette:default_desert | single,random | yes (renamed): `sandstone` meta 2 (smooth sandstone) |
| `minecraft:cyan_concrete` | none | 1 | palette:common | random | **no 1.7.10 vanilla equivalent**: concrete added in 1.12 |
| `minecraft:cyan_terracotta` | none | 6 | palette:bricks_cyan x3, palette:common, palette:default, palette:default_desert | single,random | yes (renamed): `stained_hardened_clay` meta 9 |
| `minecraft:dandelion` | none | 2 | palette:common x2 | random | yes (renamed): `yellow_flower` |
| `minecraft:deepslate_tiles` | none | 2 | variant:deepslate, variant:deepslate_rubble | variant | **no 1.7.10 vanilla equivalent**: added in 1.17 |
| `minecraft:diamond_block` | none | 2 | palette:common, palette:oilrig | single,random | yes (same name): `diamond_block` |
| `minecraft:diorite_wall` | none | 1 | palette:oilrig | single | **no 1.7.10 vanilla equivalent**: added in 1.14 |
| `minecraft:dirt` | none | 1 | palette:common | single | yes (same name): `dirt` meta 0 |
| `minecraft:emerald_block` | none | 1 | palette:common | random | yes (same name): `emerald_block` |
| `minecraft:enchanting_table` | none | 1 | palette:common | random | yes (same name): `enchanting_table` |
| `minecraft:exposed_copper` | none | 1 | part:cabin | single | **no 1.7.10 vanilla equivalent**: added in 1.17 |
| `minecraft:exposed_cut_copper_stairs` | `facing=east` x1; `facing=north` x1; `facing=south` x1; `facing=west` x1 | 4 | part:cabin x4 | single | **no 1.7.10 vanilla equivalent**: added in 1.17 |
| `minecraft:furnace` | `facing=north` x1 | 2 | palette:common, palette:default | single,random | yes (same name): `furnace` meta 2 for `facing=north` (default state meta 2 too) |
| `minecraft:glass` | none | 4 | palette:common, palette:glass_full x2, palette:glass_pane | single,random | yes (same name): `glass` |
| `minecraft:glass_pane` | none | 3 | palette:glass_full, palette:glass_pane x2 | single | yes (same name): `glass_pane` |
| `minecraft:glowstone` | none | 1 | palette:common | single | yes (same name): `glowstone` |
| `minecraft:gold_block` | none | 1 | palette:common | random | yes (same name): `gold_block` |
| `minecraft:grass_block` | none | 1 | palette:common | single | yes (renamed): `grass` |
| `minecraft:gray_concrete` | none | 4 | palette:common, part:highway_gas_station, part:highway_restaurant, part:highway_restaurant_parking | single,random | **no 1.7.10 vanilla equivalent**: concrete added in 1.12 |
| `minecraft:gray_stained_glass` | none | 4 | palette:common, palette:glass_full_gray x2, palette:glass_pane_gray | single,random | yes (renamed): `stained_glass` meta 7 |
| `minecraft:gray_stained_glass_pane` | none | 3 | palette:glass_full_gray, palette:glass_pane_gray x2 | single | yes (renamed): `stained_glass_pane` meta 7 |
| `minecraft:gray_terracotta` | none | 4 | palette:bricks_gray x3, palette:common | single,random | yes (renamed): `stained_hardened_clay` meta 7 |
| `minecraft:infested_stone_bricks` | none | 3 | palette:bricks_border, palette:bricks_standard, palette:quartzbricks_border | single | yes (renamed): `monster_egg` meta 2 |
| `minecraft:iron_bars` | none | 64 | building:building6 x2, building:building7 x2, building:building8 x2, palette:bricks_border x4, palette:bricks_cyan x4, palette:bricks_desert x3, palette:bricks_desert_orange x3, palette:bricks_desert_red x3, palette:bricks_desert_redsand x3, palette:bricks_gray x4, palette:bricks_silver x4, palette:bricks_standard x4, palette:default x10, palette:default_desert x8, palette:glass_side_variant_quartz, palette:glass_side_variant_street, palette:quartzbricks_border x4, part:highway_restaurant, part:highway_restaurant_parking | single,damaged | yes (same name): `iron_bars` |
| `minecraft:iron_block` | none | 3 | palette:common, part:highway_gas_station, part:highway_restaurant_parking | single,random | yes (same name): `iron_block` |
| `minecraft:iron_door` | `facing=north,half=lower,hinge=left` x2; `facing=north,half=lower,hinge=right` x1; `facing=north,half=upper,hinge=left` x2; `facing=north,half=upper,hinge=right` x1; `facing=south,half=lower,hinge=left,open=false,powered=false` x1; `facing=south,half=upper,hinge=left,open=false,powered=false` x1; `half=lower` x1; `half=upper` x1 | 10 | palette:oilrig x4, part:cabin x2, part:highway_gas_station x2, part:highway_restaurant x2 | single | yes (same name): `iron_door`; lower half meta = facing (east 0, south 1, west 2, north 3) +4 open; upper half meta 8, +1 hinge right, +2 powered |
| `minecraft:iron_trapdoor` | `facing=east,half=bottom,open=false,powered=false` x1 | 1 | palette:oilrig | single | **no 1.7.10 vanilla equivalent**: added in 1.8 |
| `minecraft:ladder` | `facing=east` x1; `facing=north` x1 | 2 | palette:common, palette:oilrig | single | yes (same name): `ladder`; meta 2 north, 3 south, 4 west, 5 east |
| `minecraft:lantern` | `hanging=true` x1 | 1 | part:highway_restaurant | single | **no 1.7.10 vanilla equivalent**: added in 1.14 |
| `minecraft:lever` | `face=wall,facing=north,powered=false` x1; `face=wall,facing=south` x1 | 2 | palette:oilrig x2 | single | yes (same name): `lever`; wall levers meta 1 east, 2 west, 3 south, 4 north |
| `minecraft:light_blue_stained_glass` | none | 4 | palette:common, palette:glass_full_light_blue x2, palette:glass_pane_light_blue | single,random | yes (renamed): `stained_glass` meta 3 |
| `minecraft:light_blue_stained_glass_pane` | none | 3 | palette:glass_full_light_blue, palette:glass_pane_light_blue x2 | single | yes (renamed): `stained_glass_pane` meta 3 |
| `minecraft:light_gray_terracotta` | none | 3 | palette:bricks_silver x3 | single | yes (renamed): `stained_hardened_clay` meta 8 |
| `minecraft:mossy_stone_brick_stairs` | `facing=east,half=bottom,shape=straight` x1 | 1 | palette:oilrig | single | **no 1.7.10 vanilla equivalent**: added in 1.14 |
| `minecraft:mossy_stone_bricks` | none | 4 | palette:default_desert x2, variant:stonebrick, variant:stonebrick_rubble | random,variant | yes (renamed): `stonebrick` meta 1 |
| `minecraft:oak_fence` | `east=false,north=true,south=true,west=true` x1 | 2 | palette:common, palette:oilrig | single | yes (renamed): `fence` (connections are not stored; computed on render) |
| `minecraft:oak_planks` | none | 1 | part:top1x1_5 | single | yes (renamed): `planks` meta 0 |
| `minecraft:oak_pressure_plate` | none | 1 | palette:common | single | yes (renamed): `wooden_pressure_plate` |
| `minecraft:oak_sapling` | `stage=1` x2 | 2 | palette:common, part:park_trees | random | yes (renamed): `sapling` meta 0 (+8 for `stage=1`) |
| `minecraft:obsidian` | none | 1 | palette:oilrig | single | yes (same name): `obsidian` |
| `minecraft:orange_terracotta` | none | 2 | palette:bricks_desert_orange x2 | single | yes (renamed): `stained_hardened_clay` meta 1 |
| `minecraft:polished_blackstone` | none | 2 | variant:blackstone, variant:blackstone_rubble | variant | **no 1.7.10 vanilla equivalent**: added in 1.16 |
| `minecraft:polished_blackstone_bricks` | none | 3 | palette:oilrig, variant:blackstone, variant:blackstone_rubble | single,variant | **no 1.7.10 vanilla equivalent**: added in 1.16 |
| `minecraft:polished_blackstone_slab` | `type=bottom` x1 | 1 | variant:blackstone_rubble | variant | **no 1.7.10 vanilla equivalent**: added in 1.16 |
| `minecraft:polished_blackstone_stairs` | `facing=north,half=bottom` x1 | 1 | variant:blackstone_rubble | variant | **no 1.7.10 vanilla equivalent**: added in 1.16 |
| `minecraft:polished_diorite` | none | 1 | palette:oilrig | single | **no 1.7.10 vanilla equivalent**: added in 1.8 |
| `minecraft:polished_diorite_stairs` | `facing=east,half=bottom,shape=straight` x1; `facing=east,half=top,shape=straight` x1; `facing=north,half=bottom,shape=straight` x1; `facing=north,half=top,shape=straight` x1; `facing=south,half=bottom,shape=straight` x1; `facing=south,half=top,shape=straight` x1; `facing=west,half=bottom,shape=straight` x1; `facing=west,half=top,shape=straight` x1 | 8 | palette:oilrig x8 | single | **no 1.7.10 vanilla equivalent**: added in 1.14 |
| `minecraft:poppy` | none | 2 | palette:common x2 | random | yes (renamed): `red_flower` meta 0 |
| `minecraft:potted_cactus` | none | 1 | palette:common | single | yes (renamed): `flower_pot` with tile entity `FlowerPot` holding `cactus` |
| `minecraft:powered_rail` | `powered=true,shape=ascending_east` x1; `powered=true,shape=ascending_north` x1; `powered=true,shape=ascending_south` x1; `powered=true,shape=east_west` x1; `powered=true,shape=north_south` x1 | 5 | palette:rails x5 | single | yes (renamed): `golden_rail`; meta = shape 0-5 (+8 powered) |
| `minecraft:quartz_block` | none | 4 | palette:default, palette:default_desert, palette:glass_side_variant_quartz, part:cabin | single | yes (same name): `quartz_block` meta 0 |
| `minecraft:quartz_bricks` | none | 2 | variant:quartz, variant:quartz_rubble | variant | **no 1.7.10 vanilla equivalent**: added in 1.16 |
| `minecraft:quartz_slab` | none | 1 | variant:quartz_rubble | variant | yes (renamed): `stone_slab` meta 7 |
| `minecraft:quartz_stairs` | `facing=north,half=bottom,shape=straight` x2; `facing=south,half=bottom,shape=straight` x2 | 5 | palette:default x2, palette:default_desert x2, variant:quartz_rubble | single,variant | yes (same name): `quartz_stairs`; stairs meta east 0, west 1, south 2, north 3, +4 upside-down |
| `minecraft:rail` | `shape=ascending_east` x1; `shape=east_west` x1; `shape=north_east` x1; `shape=north_south` x2; `shape=north_west` x1; `shape=south_east` x1; `shape=south_west` x1 | 8 | palette:oilrig, palette:rails x7 | single | yes (same name): `rail`; meta 0 north_south, 1 east_west, 2-5 ascending east/west/north/south, 6 south_east, 7 south_west, 8 north_west, 9 north_east |
| `minecraft:red_concrete` | none | 3 | palette:oilrig, part:highway_gas_station, part:highway_restaurant | single | **no 1.7.10 vanilla equivalent**: concrete added in 1.12 |
| `minecraft:red_nether_bricks` | none | 1 | palette:default_desert | single | **no 1.7.10 vanilla equivalent**: added in 1.10 |
| `minecraft:red_sandstone` | none | 1 | palette:bricks_desert_redsand | random | **no 1.7.10 vanilla equivalent**: added in 1.8 |
| `minecraft:red_sandstone@2` | none | 2 | palette:bricks_desert_redsand x2 | single,random | **no 1.7.10 vanilla equivalent**: malformed id (1.12 `@meta` syntax); fails to load on 1.20 too (ASSET-03) |
| `minecraft:red_sandstone_slab` | `type=double` x5 | 5 | palette:bricks_desert, palette:bricks_desert_orange, palette:bricks_desert_red, palette:bricks_desert_redsand, palette:default_desert | single | **no 1.7.10 vanilla equivalent**: added in 1.8 |
| `minecraft:red_terracotta` | none | 4 | palette:bricks_desert_red x2, variant:bricks, variant:bricks_rubble | single,variant | yes (renamed): `stained_hardened_clay` meta 14 |
| `minecraft:redstone_torch` | `lit=true` x1 | 1 | palette:common | single | yes (same name): `redstone_torch` (lit) meta 5 standing |
| `minecraft:redstone_wall_torch` | `facing=east,lit=true` x1 | 1 | palette:oilrig | single | yes (renamed): `redstone_torch` (lit) meta 1 for `facing=east` |
| `minecraft:sandstone` | none | 1 | palette:bricks_desert | random | yes (same name): `sandstone` meta 0 |
| `minecraft:sea_lantern` | none | 3 | palette:oilrig, part:highway_gas_station, part:highway_restaurant_parking | single | **no 1.7.10 vanilla equivalent**: added in 1.8 |
| `minecraft:smooth_quartz` | none | 3 | palette:street_large, variant:quartz, variant:quartz_rubble | single,variant | yes (renamed): `double_stone_slab` meta 15 (hidden full-quartz variant; inferred from the 1.13 flattening table) |
| `minecraft:smooth_stone` | none | 1 | palette:oilrig | single | yes (renamed): `double_stone_slab` meta 8 (hidden seamless variant; inferred from the 1.13 flattening table) |
| `minecraft:smooth_stone_slab` | `type=bottom` x5; `type=double` x8; `type=top,waterlogged=false` x1 | 14 | palette:bricks_border, palette:bricks_standard, palette:default x4, palette:default_desert x3, palette:glass_side_variant_street, palette:oilrig, palette:quartzbricks_border, palette:street_large, variant:stoneandbrick_rubble | single,variant | yes (renamed): `stone_slab` meta 0 (`type=bottom`), meta 8 (`type=top`); `double_stone_slab` meta 0 (`type=double`) |
| `minecraft:spawner` | none | 2 | palette:common x2 | single | yes (renamed): `mob_spawner` (tile entity `MobSpawner`) |
| `minecraft:spruce_pressure_plate` | `powered=false` x1 | 1 | palette:oilrig | single | **no 1.7.10 vanilla equivalent**: added in 1.13 (only oak `wooden_pressure_plate` exists) |
| `minecraft:spruce_sapling` | `stage=1` x2 | 2 | palette:common, part:park_trees | random | yes (renamed): `sapling` meta 1 (+8 for `stage=1`) |
| `minecraft:stone` | none | 2 | variant:stoneandbrick, variant:stoneandbrick_rubble | variant | yes (same name): `stone` meta 0 |
| `minecraft:stone_brick_slab` | none | 1 | variant:stonebrick_rubble | variant | yes (renamed): `stone_slab` meta 5 |
| `minecraft:stone_brick_stairs` | `facing=east,half=bottom,shape=straight` x2; `facing=north,half=bottom,shape=straight` x2; `facing=north,half=top,shape=straight` x2; `facing=south,half=bottom,shape=straight` x2; `facing=west,half=bottom,shape=straight` x2 | 11 | palette:default x5, palette:default_desert x5, variant:stonebrick_rubble | single,variant | yes (same name): `stone_brick_stairs`; stairs meta as `quartz_stairs` |
| `minecraft:stone_bricks` | none | 6 | palette:default_desert x2, variant:stoneandbrick, variant:stoneandbrick_rubble, variant:stonebrick, variant:stonebrick_rubble | random,variant | yes (renamed): `stonebrick` meta 0 |
| `minecraft:stone_pressure_plate` | `powered=false` x1 | 1 | palette:oilrig | single | yes (same name): `stone_pressure_plate` |
| `minecraft:stone_stairs` | `facing=east,half=bottom` x1; `facing=east,half=bottom,shape=straight` x1; `facing=west,half=bottom,shape=straight` x2 | 4 | palette:default, palette:default_desert, palette:oilrig, variant:stoneandbrick_rubble | single,variant | **no 1.7.10 vanilla equivalent**: added in 1.14 (smooth-stone stairs). Name clash: 1.7.10 `stone_stairs` is cobblestone stairs |
| `minecraft:structure_void` | none | 3 | palette:common, palette:default, palette:default_desert | single | **no 1.7.10 vanilla equivalent**: added in 1.10; used only as the "hard air" marker and never placed |
| `minecraft:wall_torch` | `facing=north` x1 | 1 | palette:common | single | yes (renamed): `torch` meta 4 for `facing=north` (1 east, 2 west, 3 south, 4 north, 5 standing) |
| `minecraft:water` | none | 1 | palette:common | single | yes (same name): `water` (still) |
| `minecraft:white_stained_glass` | none | 4 | palette:common, palette:glass_full_white x2, palette:glass_pane_white | single | yes (renamed): `stained_glass` meta 0 |
| `minecraft:white_stained_glass_pane` | none | 3 | palette:glass_full_white, palette:glass_pane_white x2 | single | yes (renamed): `stained_glass_pane` meta 0 |
| `minecraft:yellow_concrete` | none | 2 | palette:oilrig, part:highway_restaurant_parking | single | **no 1.7.10 vanilla equivalent**: concrete added in 1.12 |

## Blocks written or tested by Java code

Blocks the generator uses directly rather than through a palette, including blocks it only tests for. Fourteen of these ids also appear in the asset table; they are repeated here for their Java role. The 17 Java-only ids are `bedrock`, `lava`, `oak_leaves`, `jungle_leaves`, `spruce_leaves`, `mossy_cobblestone`, `moss_block`, `torch`, the six wooden doors, `vine`, `command_block` and `cobblestone`. Line numbers are on `f18a094d` under `src/main/java/mcjty/lostcities/`. The six block tags in `src/generated/resources/data/lostcities/tags/blocks/` (owned by `08-loot-mobs-blocks`) name further vanilla blocks for tests only; they are not inventoried here.

| Block ID | Uses | Where | Role | 1.7.10 vanilla |
|---|---|---|---|---|
| `minecraft:air` | many | `LostCityTerrainFeature`, `ChunkDriver`, `Bridges`, `Corridors`, `Doors`, `Railways`, `Scattered`, `Spheres`, `CitySphere`, `DamageArea`, `NoiseChunkOpt` | clearing; palette char ` ` in parts means "leave as is", not air | yes (same name): `air` |
| `minecraft:structure_void` | 2 | `LostCityTerrainFeature:107`, `Supports:50` | hard-air marker compared by identity; replaced by air, water or the existing block | **no 1.7.10 vanilla equivalent**: added in 1.10; used only as the "hard air" marker and never placed |
| `minecraft:bedrock` | 3 | `ChunkDriver`, `LostCityTerrainFeature:730,949` | floor of city chunks in some landscapes; never moved | yes (same name): `bedrock` |
| `minecraft:stone` | 1 | `LostCityProfile` (`baseBlock` default) | profile base block | yes (same name): `stone` meta 0 |
| `minecraft:water` | several | `LostCityProfile` (`liquidBlock` default), `LostCityTerrainFeature:693`, `NoiseChunkOpt` | profile liquid; palette `W` resolves to the same state | yes (same name): `water` (still) |
| `minecraft:lava` | 4 | `LostCityTerrainFeature:696`, `NoiseChunkOpt` | terrain checks | yes (same name): `lava` (still) |
| `minecraft:dirt` | 2 | `LostCityTerrainFeature` (generatePart) | temporary placeholder for blocks tagged `lostcities:needspoi` | yes (same name): `dirt` meta 0 |
| `minecraft:grass_block` | 1 | `LostCityTerrainFeature:1547` | park grass when the city style has no `grass` char | yes (renamed): `grass` |
| `minecraft:glowstone` | 1 | `Corridors:44` | corridor light when the city style has no `glowstone` char | yes (same name): `glowstone` |
| `minecraft:iron_bars` | 2 | `LostCityTerrainFeature:1145`, `Highways:135` | ruin edges when the city style has no `ironbars` char; highway railing test | yes (same name): `iron_bars` |
| `minecraft:rail` | 3 | `Corridors:18-19`, `LostCityTerrainFeature:199` | corridor rails (`shape=east_west` and default); rail-transform state set | yes (same name): `rail`; meta 0 north_south, 1 east_west, 2-5 ascending east/west/north/south, 6 south_east, 7 south_west, 8 north_west, 9 north_east |
| `minecraft:powered_rail` | 1 | `LostCityTerrainFeature:200` | rail-transform state set | yes (renamed): `golden_rail`; meta = shape 0-5 (+8 powered) |
| `minecraft:oak_leaves` | 1 | `LostCityTerrainFeature:126` | rubble leaves, `persistent=true` | yes (renamed): `leaves` meta 0 (+4 no decay for `persistent=true`) |
| `minecraft:jungle_leaves` | 1 | `LostCityTerrainFeature:127` | rubble leaves, `persistent=true` | yes (renamed): `leaves` meta 3 (+4 no decay) |
| `minecraft:spruce_leaves` | 1 | `LostCityTerrainFeature:128` | rubble leaves, `persistent=true` | yes (renamed): `leaves` meta 1 (+4 no decay) |
| `minecraft:mossy_stone_bricks` | 1 | `LostCityTerrainFeature:169` | rubble-dirt default (20/128) | yes (renamed): `stonebrick` meta 1 |
| `minecraft:mossy_cobblestone` | 1 | `LostCityTerrainFeature:170` | rubble-dirt default (40/128) | yes (same name): `mossy_cobblestone` |
| `minecraft:moss_block` | 1 | `LostCityTerrainFeature:171` | rubble-dirt default (68/128) | **no 1.7.10 vanilla equivalent**: added in 1.17 |
| `minecraft:torch` | 1 | `LostCityTerrainFeature:405` | torch fix-up: standing torch | yes (same name): `torch` meta 5 (standing) |
| `minecraft:wall_torch` | 1 | `LostCityTerrainFeature:399` | torch fix-up: wall torch facing away from the wall | yes (renamed): `torch` meta 4 for `facing=north` (1 east, 2 west, 3 south, 4 north, 5 standing) |
| `minecraft:oak_door` | 2 | `BuildingInfo:1335,1338` | random building door | yes (renamed): `wooden_door` |
| `minecraft:birch_door` | 1 | `BuildingInfo:1331` | random building door | **no 1.7.10 vanilla equivalent**: added in 1.8 |
| `minecraft:acacia_door` | 1 | `BuildingInfo:1332` | random building door | **no 1.7.10 vanilla equivalent**: added in 1.8 |
| `minecraft:dark_oak_door` | 1 | `BuildingInfo:1333` | random building door | **no 1.7.10 vanilla equivalent**: added in 1.8 |
| `minecraft:spruce_door` | 1 | `BuildingInfo:1334` | random building door | **no 1.7.10 vanilla equivalent**: added in 1.8 |
| `minecraft:jungle_door` | 1 | `BuildingInfo:1336` | random building door | **no 1.7.10 vanilla equivalent**: added in 1.8 |
| `minecraft:iron_door` | 1 | `BuildingInfo:1337` | random building door | yes (same name): `iron_door`; lower half meta = facing (east 0, south 1, west 2, north 3) +4 open; upper half meta 8, +1 hinge right, +2 powered |
| `minecraft:vine` | 4 | `WorldSettings` defaults | vines with one face each (`west`, `east`, `south`, `north` = true), placed by `ChunkFixer` | yes (same name): `vine`; meta bits south 1, west 2, north 4, east 8 |
| `minecraft:spawner` | 1 | `LostCityTerrainFeature:1933` (NBT id `minecraft:mob_spawner`) | spawner block entity for palette `mob` chars | yes (renamed): `mob_spawner` (tile entity `MobSpawner`) |
| `minecraft:command_block` | 1 | `LostCityTerrainFeature:1915` | compared only: schedules a tick for palette `tag` command blocks | yes (same name): `command_block` |
| `minecraft:cobblestone` | 1 | `ForgeEventHandlers:567` | bed-teleport platform in the Lost City dimension (not ported, 0003) | yes (same name): `cobblestone` |

## Debug command blocks

`commands/CommandTestFill.java` (`/lostcities testfill`, `01-core`) fills an area with random building blocks for testing. It is not world generation, but it does write blocks, so its 88 ids are listed for completeness.

| Group | Ids | 1.7.10 vanilla |
|---|---|---|
| Planks (6) | `oak_planks`, `spruce_planks`, `birch_planks`, `jungle_planks`, `acacia_planks`, `dark_oak_planks` | yes (renamed): `planks` meta 0-5 |
| Wool (16) | `white_wool` … `black_wool`, all 16 colors | yes (renamed): `wool` meta 0-15 |
| Stained glass (16) | `white_stained_glass` … `black_stained_glass`, all 16 colors | yes (renamed): `stained_glass` meta 0-15 |
| Other present (15) | `glass`, `glowstone`, `oak_fence` (`fence`), `oak_fence_gate` (`fence_gate`), `acacia_slab` (`wooden_slab` meta 4), `brick_slab` (`stone_slab` 4), `brick_stairs`, `nether_brick_slab` (`stone_slab` 6), `nether_brick_stairs`, `quartz_slab` (`stone_slab` 7), `quartz_stairs`, `sandstone_slab` (`stone_slab` 1), `sandstone_stairs`, `stone_brick_slab` (`stone_slab` 5), `stone_brick_stairs` | yes |
| Absent (35) | `andesite_wall`, `brick_wall`, `diorite_wall`, `granite_wall`, `nether_brick_wall`, `prismarine_wall`, `red_sandstone_wall`, `sandstone_wall`, `stone_brick_wall`; `blackstone_slab`, `blackstone_stairs`, `polished_blackstone_brick_slab`, `polished_blackstone_brick_stairs`; `crimson_planks`, `crimson_slab`, `crimson_stairs`, `warped_planks`, `warped_slab`, `warped_stairs`; `dark_prismarine_slab`, `dark_prismarine_stairs`, `prismarine_brick_slab`, `prismarine_brick_stairs`; `end_stone_brick_slab`, `end_stone_brick_stairs`; `polished_andesite_slab`, `polished_andesite_stairs`, `polished_diorite_slab`, `polished_diorite_stairs`, `polished_granite_slab`, `polished_granite_stairs`; `purpur_slab`, `purpur_stairs`; `red_sandstone_slab`, `red_sandstone_stairs` | **no 1.7.10 vanilla equivalent** |

## Block-state properties that need a 1.7.10 encoding

Every property combination used above, grouped by how 1.7.10 stores it. This is the set a metadata mapping must cover.

| Property | Blocks | Values used | 1.7.10 storage |
|---|---|---|---|
| `facing` (horizontal) | stairs (stone brick, quartz, stone, polished diorite, mossy stone brick, copper, deepslate, blackstone), chest, furnace, ladder, wall torch, redstone wall torch, lever, iron door, iron trapdoor, barrel, anvil | north, south, east, west | metadata, per-block layout (see rows above) |
| `half` | stairs, iron door, iron trapdoor | bottom/top; lower/upper | stairs bit 4 (upside-down); door bit 8 (upper half) |
| `shape` | rail, powered rail; stairs (recomputed) | `north_south`, `east_west`, `ascending_*`, `south_east`, `south_west`, `north_west`, `north_east`; `straight` | rail meta 0-9, powered rail 0-5; 1.7.10 stairs have no stored shape |
| `hinge`, `open`, `powered` | iron door, trapdoor, levers, pressure plates, powered rail | left/right, false, true/false | door upper-half bits; lever bit 8; powered-rail bit 8 |
| `type` | slabs | bottom, top, double | `stone_slab` bit 8 (top); double slabs are separate blocks |
| `stage` | saplings | 1 | sapling bit 8 |
| `lit` | redstone torches | true | separate blocks (`redstone_torch` lit, `unlit_redstone_torch`) |
| `face` | lever | wall | lever meta 1-4 |
| `hanging` | lantern | true | none (no lantern) |
| `waterlogged` | smooth stone slab (`false`) | false | none (no waterlogging) |
| fence and wall sides `north`/`east`/`south`/`west` | oak fence | as written | not stored (computed on render) |
