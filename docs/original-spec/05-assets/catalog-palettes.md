# Catalog: palettes and variants

Every bundled palette character, every local palette inside a building or part, and every variant, at `f18a094d`. Formats and compile rules are in [palettes-and-variants.md](palettes-and-variants.md). Block ids drop the `minecraft:` prefix; every bundled block is vanilla. Random lists show the **effective** slots out of 128 after compilation (weights past slot 128 are cut), with the written weight in parentheses where it differs. "Damaged to" is the block the character's block(s) become under explosion or ruin damage. Inheritance between palettes does not exist: palettes are layered by the style (later groups override earlier ones), then by the building, then by the part.

## Character conventions

The bundled assets use characters consistently across palettes. A part can use any character that every style guarantees; all 207 parts are covered by all four styles (checked by script against the style groups, the building local palettes and the part local palettes).

| Char | Meaning in the bundled set | Defined by |
|---|---|---|
| ` ` (space) | air: leave the existing block | `common` |
| `~`, `b` | structure void: hard air | `common` (`~`), `default`, `default_desert` (`b`) |
| `#` | main wall block of the building | brick palettes (`bricks_*`, `quartzbricks_border`) |
| `X` | accent wall block | brick palettes |
| `$` | accent slab or band | brick palettes |
| `}` | building rubble (the buildings' `rubble` char) | `bricks_standard`, `_cyan`, `_gray`, `_silver`, `_border`, `quartzbricks_border`; **not** the desert brick palettes ([ASSET-20](index.md#suspected-bugs)) |
| `a` | windows (glass block or pane) | `glass_*` |
| `+` | glass block | `glass_*` |
| `` ` `` | glass pane | `glass_*` |
| `@` | window-side column: glass, the wall block, quartz or (unused) smooth stone | `glass_side_variant_*` |
| `Q` | quartz block | `default`, `default_desert` |
| `R` | cyan terracotta (highway surface) | `default`, `default_desert` |
| `S`, `u` | smooth stone double slab (street, floors); red sandstone double slab in deserts for `S` | `default`, `default_desert` |
| `B` | bricks (red nether bricks in deserts) | `default`, `default_desert` |
| `v`, `y`, `x` | stone bricks (support pillars, rail beds, park elevation); cut sandstone for `x` in deserts | `default`, `default_desert` |
| `_`, `=` | smooth stone bottom slab | `default`, `default_desert` |
| `w` | cobblestone wall | `default`, `default_desert` |
| `:` | iron bars | `default`, `default_desert` |
| `A`, `t`, `z`, `c`, `d` | stone brick stairs facing west, east, north, south, and north upside-down | `default`, `default_desert` |
| `(`, `)` | quartz stairs facing north, south | `default`, `default_desert` |
| `/` | stone stairs facing west | `default`, `default_desert` |
| `;` | furnace with 10 coal (block-entity NBT); used by no part | `default` only |
| `1`, `4` | spawner: easy mobs, hard mobs | `common` |
| `C` | chest with loot (`chestloot`) | `common` |
| `T` | torch (re-oriented after generation) | `common` |
| `h` | glowstone | `common` |
| `g` | lit redstone torch | `common` |
| `9` | random concrete and terracotta (city-sphere border); used by no part | `common` |
| `Y` | white stained glass | `common` |
| `Z` | random glass colors (city-sphere glass) | `common` |
| `p` | random flowers and saplings | `common` |
| `K` | random poppy or dandelion | `common` |
| `!` | random precious-metal blocks (treasure) | `common` |
| `F` | random furniture (furnace, crafting table, brewing stand, anvil, cauldron, enchanting table) or cobweb | `common` |
| `%` | cobweb 40/128, else air | `common` |
| `\` | cobweb (stuff column) | `common` |
| `{` | chain (stuff column) | `common` |
| `*` | potted cactus | `common` |
| `l` | ladder facing north | `common` |
| `-` | oak pressure plate | `common` |
| `.` | oak fence | `common` |
| `L` | bookshelf | `common` |
| `W` | water | `common` |
| `G`, `D` | grass block, dirt | `common` |
| rails letters | rail shapes (`k e j q s r n`) and powered rails (`m E H f i`) | `rails` |
| `l`, `q` | street-large slab and smooth quartz (shadow `common`'s `l` ladder in those parts) | `street_large` |
| many | oil-rig fittings | `oilrig` |

Characters used by no part: `9`, `;`, `\`, `{` (the last two are used by stuff columns, `9` by city-sphere borders).

## Shared palettes

### `bricks_border`

File `data/lostcities/lostcities/palettes/bricks_border.json`. 4 characters.

| Char | Maps to | Damaged to | Info |
|---|---|---|---|
| `X` | `infested_stone_bricks` | `iron_bars` |  |
| `$` | `smooth_stone_slab[type=double]` | `iron_bars` |  |
| `#` | variant `bricks` | `iron_bars` |  |
| `}` | variant `bricks_rubble` | `iron_bars` |  |

### `bricks_cyan`

File `data/lostcities/lostcities/palettes/bricks_cyan.json`. 4 characters.

| Char | Maps to | Damaged to | Info |
|---|---|---|---|
| `X` | `cyan_terracotta` | `iron_bars` |  |
| `$` | `black_terracotta` | `iron_bars` |  |
| `#` | `cyan_terracotta` | `iron_bars` |  |
| `}` | `cyan_terracotta` | `iron_bars` |  |

### `bricks_desert`

File `data/lostcities/lostcities/palettes/bricks_desert.json`. 3 characters.

| Char | Maps to | Damaged to | Info |
|---|---|---|---|
| `X` | `cut_sandstone` | `iron_bars` |  |
| `$` | `red_sandstone_slab[type=double]` | `iron_bars` |  |
| `#` | random: `sandstone` 12/128, `cut_sandstone` 116/128 (w 2000) | `iron_bars` |  |

### `bricks_desert_orange`

File `data/lostcities/lostcities/palettes/bricks_desert_orange.json`. 3 characters.

| Char | Maps to | Damaged to | Info |
|---|---|---|---|
| `X` | `orange_terracotta` | `iron_bars` |  |
| `$` | `red_sandstone_slab[type=double]` | `iron_bars` |  |
| `#` | `orange_terracotta` | `iron_bars` |  |

### `bricks_desert_red`

File `data/lostcities/lostcities/palettes/bricks_desert_red.json`. 3 characters.

| Char | Maps to | Damaged to | Info |
|---|---|---|---|
| `X` | `red_terracotta` | `iron_bars` |  |
| `$` | `red_sandstone_slab[type=double]` | `iron_bars` |  |
| `#` | `red_terracotta` | `iron_bars` |  |

### `bricks_desert_redsand`

File `data/lostcities/lostcities/palettes/bricks_desert_redsand.json`. 3 characters. *Unused*: no style lists it. It uses the 1.12 `@meta` syntax (`red_sandstone@2`, smooth red sandstone in 1.12, `cut_red_sandstone` since 1.13) and would fail to load if referenced ([ASSET-03](index.md#suspected-bugs)).

| Char | Maps to | Damaged to | Info |
|---|---|---|---|
| `X` | `red_sandstone@2` | `iron_bars` |  |
| `$` | `red_sandstone_slab[type=double]` | `iron_bars` |  |
| `#` | random: `red_sandstone` 12/128, `red_sandstone@2` 116/128 (w 2000) | `iron_bars` |  |

### `bricks_gray`

File `data/lostcities/lostcities/palettes/bricks_gray.json`. 4 characters.

| Char | Maps to | Damaged to | Info |
|---|---|---|---|
| `X` | `gray_terracotta` | `iron_bars` |  |
| `$` | `black_terracotta` | `iron_bars` |  |
| `#` | `gray_terracotta` | `iron_bars` |  |
| `}` | `gray_terracotta` | `iron_bars` |  |

### `bricks_silver`

File `data/lostcities/lostcities/palettes/bricks_silver.json`. 4 characters.

| Char | Maps to | Damaged to | Info |
|---|---|---|---|
| `X` | `light_gray_terracotta` | `iron_bars` |  |
| `$` | `brown_terracotta` | `iron_bars` |  |
| `#` | `light_gray_terracotta` | `iron_bars` |  |
| `}` | `light_gray_terracotta` | `iron_bars` |  |

### `bricks_standard`

File `data/lostcities/lostcities/palettes/bricks_standard.json`. 4 characters.

| Char | Maps to | Damaged to | Info |
|---|---|---|---|
| `X` | `infested_stone_bricks` | `iron_bars` |  |
| `$` | `smooth_stone_slab[type=double]` | `iron_bars` |  |
| `#` | variant `stonebrick` | `iron_bars` |  |
| `}` | variant `stonebrick_rubble` | `iron_bars` |  |

### `common`

File `data/lostcities/lostcities/palettes/common.json`. 26 characters.

| Char | Maps to | Damaged to | Info |
|---|---|---|---|
| `1` | `spawner` |  | spawner, mob condition `easymobs` |
| `4` | `spawner` |  | spawner, mob condition `hardmobs` |
| `T` | `wall_torch[facing=north]` |  | torch |
| `h` | `glowstone` |  |  |
| `g` | `redstone_torch[lit=true]` |  |  |
| `9` | random: `cyan_terracotta` 32/128, `gray_terracotta` 32/128, `cyan_concrete` 32/128, `gray_concrete` 32/128 (w 1000) |  |  |
| `Y` | `white_stained_glass` |  |  |
| `Z` | random: `glass` 32/128, `gray_stained_glass` 32/128, `blue_stained_glass` 32/128, `light_blue_stained_glass` 32/128 (w 1000) |  |  |
| `p` | random: `poppy` 30/128, `dandelion` 30/128, `oak_sapling[stage=1]` 15/128, `spruce_sapling[stage=1]` 15/128, `birch_sapling[stage=1]` 15/128, `acacia_sapling[stage=1]` 23/128 (w 1000) |  |  |
| `K` | random: `poppy` 64/128, `dandelion` 64/128 (w 1000) |  |  |
| `!` | random: `diamond_block` 15/128, `gold_block` 20/128, `emerald_block` 15/128, `iron_block` 78/128 (w 1000) |  |  |
| `F` | random: `furnace[facing=north]` 25/128, `crafting_table` 20/128, `brewing_stand` 7/128, `anvil[facing=north]` 6/128, `cauldron` 5/128, `enchanting_table` 2/128, `cobweb` 63/128 (w 1000) |  |  |
| `%` | random: `cobweb` 40/128, `air` 88/128 (w 1000) |  |  |
| `\` | `cobweb` |  |  |
| `{` | `chain` |  |  |
| `*` | `potted_cactus` |  |  |
| `l` | `ladder[facing=north]` |  |  |
| `-` | `oak_pressure_plate` |  |  |
| `.` | `oak_fence` |  |  |
| `L` | `bookshelf` |  |  |
| `W` | `water` |  |  |
| `C` | `chest[facing=north]` |  | loot condition `chestloot` |
| `G` | `grass_block` |  |  |
| `D` | `dirt` |  |  |
| ` ` (space) | `air` |  |  |
| `~` | `structure_void` |  |  |

### `default`

File `data/lostcities/lostcities/palettes/default.json`. 22 characters.

| Char | Maps to | Damaged to | Info |
|---|---|---|---|
| `R` | `cyan_terracotta` | `iron_bars` |  |
| `;` | `furnace` | `iron_bars` | block-entity NBT `{"Items":[{"Slot":0,"id":"minecraft:coal","Count":10}]}` |
| `Q` | `quartz_block` | `iron_bars` |  |
| `u` | `smooth_stone_slab[type=double]` | `iron_bars` |  |
| `S` | `smooth_stone_slab[type=double]` | `iron_bars` |  |
| `b` | `structure_void` |  |  |
| `B` | `bricks` | `iron_bars` |  |
| `v` | variant `stonebrick` | `iron_bars` |  |
| `y` | variant `stonebrick` | `iron_bars` |  |
| `x` | variant `stonebrick` | `iron_bars` |  |
| `_` | `smooth_stone_slab[type=bottom]` |  |  |
| `=` | `smooth_stone_slab[type=bottom]` |  |  |
| `w` | `cobblestone_wall` |  |  |
| `:` | `iron_bars` |  |  |
| `A` | `stone_brick_stairs[facing=west,half=bottom,shape=straight]` |  |  |
| `t` | `stone_brick_stairs[facing=east,half=bottom,shape=straight]` |  |  |
| `z` | `stone_brick_stairs[facing=north,half=bottom,shape=straight]` |  |  |
| `c` | `stone_brick_stairs[facing=south,half=bottom,shape=straight]` |  |  |
| `d` | `stone_brick_stairs[facing=north,half=top,shape=straight]` |  |  |
| `(` | `quartz_stairs[facing=north,half=bottom,shape=straight]` |  |  |
| `)` | `quartz_stairs[facing=south,half=bottom,shape=straight]` |  |  |
| `/` | `stone_stairs[facing=west,half=bottom,shape=straight]` |  |  |

### `default_desert`

File `data/lostcities/lostcities/palettes/default_desert.json`. 21 characters.

| Char | Maps to | Damaged to | Info |
|---|---|---|---|
| `Q` | `quartz_block` | `iron_bars` |  |
| `R` | `cyan_terracotta` | `iron_bars` |  |
| `u` | `smooth_stone_slab[type=double]` | `iron_bars` |  |
| `S` | `red_sandstone_slab[type=double]` | `iron_bars` |  |
| `b` | `structure_void` |  |  |
| `B` | `red_nether_bricks` | `iron_bars` |  |
| `v` | random: `cracked_stone_bricks` 10/128, `mossy_stone_bricks` 9/128, `stone_bricks` 109/128 (w 2000) | `iron_bars` |  |
| `y` | random: `cracked_stone_bricks` 10/128, `mossy_stone_bricks` 9/128, `stone_bricks` 109/128 (w 2000) | `iron_bars` |  |
| `x` | `cut_sandstone` |  |  |
| `_` | `smooth_stone_slab[type=bottom]` |  |  |
| `=` | `smooth_stone_slab[type=bottom]` |  |  |
| `w` | `cobblestone_wall` |  |  |
| `:` | `iron_bars` |  |  |
| `A` | `stone_brick_stairs[facing=west,half=bottom,shape=straight]` |  |  |
| `t` | `stone_brick_stairs[facing=east,half=bottom,shape=straight]` |  |  |
| `z` | `stone_brick_stairs[facing=north,half=bottom,shape=straight]` |  |  |
| `c` | `stone_brick_stairs[facing=south,half=bottom,shape=straight]` |  |  |
| `d` | `stone_brick_stairs[facing=north,half=top,shape=straight]` |  |  |
| `(` | `quartz_stairs[facing=north,half=bottom,shape=straight]` |  |  |
| `)` | `quartz_stairs[facing=south,half=bottom,shape=straight]` |  |  |
| `/` | `stone_stairs[facing=west,half=bottom,shape=straight]` |  |  |

### `glass_full`

File `data/lostcities/lostcities/palettes/glass_full.json`. 3 characters.

| Char | Maps to | Damaged to | Info |
|---|---|---|---|
| `+` | `glass` |  |  |
| `` ` `` | `glass_pane` |  |  |
| `a` | `glass` |  |  |

### `glass_full_blue`

File `data/lostcities/lostcities/palettes/glass_full_blue.json`. 3 characters.

| Char | Maps to | Damaged to | Info |
|---|---|---|---|
| `+` | `blue_stained_glass` |  |  |
| `` ` `` | `blue_stained_glass_pane` |  |  |
| `a` | `blue_stained_glass` |  |  |

### `glass_full_gray`

File `data/lostcities/lostcities/palettes/glass_full_gray.json`. 3 characters.

| Char | Maps to | Damaged to | Info |
|---|---|---|---|
| `+` | `gray_stained_glass` |  |  |
| `` ` `` | `gray_stained_glass_pane` |  |  |
| `a` | `gray_stained_glass` |  |  |

### `glass_full_light_blue`

File `data/lostcities/lostcities/palettes/glass_full_light_blue.json`. 3 characters.

| Char | Maps to | Damaged to | Info |
|---|---|---|---|
| `+` | `light_blue_stained_glass` |  |  |
| `` ` `` | `light_blue_stained_glass_pane` |  |  |
| `a` | `light_blue_stained_glass` |  |  |

### `glass_full_white`

File `data/lostcities/lostcities/palettes/glass_full_white.json`. 3 characters.

| Char | Maps to | Damaged to | Info |
|---|---|---|---|
| `+` | `white_stained_glass` |  |  |
| `` ` `` | `white_stained_glass_pane` |  |  |
| `a` | `white_stained_glass` |  |  |

### `glass_pane`

File `data/lostcities/lostcities/palettes/glass_pane.json`. 3 characters.

| Char | Maps to | Damaged to | Info |
|---|---|---|---|
| `+` | `glass` |  |  |
| `` ` `` | `glass_pane` |  |  |
| `a` | `glass_pane` |  |  |

### `glass_pane_blue`

File `data/lostcities/lostcities/palettes/glass_pane_blue.json`. 3 characters.

| Char | Maps to | Damaged to | Info |
|---|---|---|---|
| `+` | `blue_stained_glass` |  |  |
| `` ` `` | `blue_stained_glass_pane` |  |  |
| `a` | `blue_stained_glass_pane` |  |  |

### `glass_pane_gray`

File `data/lostcities/lostcities/palettes/glass_pane_gray.json`. 3 characters.

| Char | Maps to | Damaged to | Info |
|---|---|---|---|
| `+` | `gray_stained_glass` |  |  |
| `` ` `` | `gray_stained_glass_pane` |  |  |
| `a` | `gray_stained_glass_pane` |  |  |

### `glass_pane_light_blue`

File `data/lostcities/lostcities/palettes/glass_pane_light_blue.json`. 3 characters.

| Char | Maps to | Damaged to | Info |
|---|---|---|---|
| `+` | `light_blue_stained_glass` |  |  |
| `` ` `` | `light_blue_stained_glass_pane` |  |  |
| `a` | `light_blue_stained_glass_pane` |  |  |

### `glass_pane_white`

File `data/lostcities/lostcities/palettes/glass_pane_white.json`. 3 characters.

| Char | Maps to | Damaged to | Info |
|---|---|---|---|
| `+` | `white_stained_glass` |  |  |
| `` ` `` | `white_stained_glass_pane` |  |  |
| `a` | `white_stained_glass_pane` |  |  |

### `glass_side_variant_bricks`

File `data/lostcities/lostcities/palettes/glass_side_variant_bricks.json`. 1 characters.

| Char | Maps to | Damaged to | Info |
|---|---|---|---|
| `@` | same as char `#` |  |  |

### `glass_side_variant_glass`

File `data/lostcities/lostcities/palettes/glass_side_variant_glass.json`. 1 characters.

| Char | Maps to | Damaged to | Info |
|---|---|---|---|
| `@` | same as char `a` |  |  |

### `glass_side_variant_quartz`

File `data/lostcities/lostcities/palettes/glass_side_variant_quartz.json`. 1 characters.

| Char | Maps to | Damaged to | Info |
|---|---|---|---|
| `@` | `quartz_block` | `iron_bars` |  |

### `glass_side_variant_street`

File `data/lostcities/lostcities/palettes/glass_side_variant_street.json`. 1 characters. *Unused*: no style lists it.

| Char | Maps to | Damaged to | Info |
|---|---|---|---|
| `@` | `smooth_stone_slab[type=double]` | `iron_bars` |  |

### `oilrig`

File `data/lostcities/lostcities/palettes/oilrig.json`. 38 characters.

| Char | Maps to | Damaged to | Info |
|---|---|---|---|
| `M` | `diamond_block` |  |  |
| `q` | `obsidian` |  |  |
| `I` | `black_stained_glass` |  |  |
| `0` | `polished_blackstone_bricks` |  |  |
| `[` | `cracked_polished_blackstone_bricks` |  |  |
| `J` | `sea_lantern` |  |  |
| `P` | `spruce_pressure_plate[powered=false]` |  |  |
| `E` | `oak_fence[east=false,north=true,south=true,west=true]` |  |  |
| `2` | `smooth_stone` |  |  |
| `s` | `barrel[facing=east,open=false]` |  |  |
| `N` | `barrel[facing=west,open=false]` |  |  |
| `U` | `barrel[facing=south,open=false]` |  |  |
| `&` | `red_concrete` |  |  |
| `m` | `stone_pressure_plate[powered=false]` |  |  |
| `V` | `iron_trapdoor[facing=east,half=bottom,open=false,powered=false]` |  |  |
| `^` | `smooth_stone_slab[type=top,waterlogged=false]` |  |  |
| `j` | `ladder[facing=east]` |  |  |
| `O` | `iron_door[facing=south,half=upper,hinge=left,open=false,powered=false]` |  |  |
| `n` | `iron_door[facing=south,half=lower,hinge=left,open=false,powered=false]` |  |  |
| `_` | `iron_door[facing=north,half=lower,hinge=right]` |  |  |
| `-` | `iron_door[facing=north,half=upper,hinge=right]` |  |  |
| `L` | `lever[face=wall,facing=south]` |  |  |
| `D` | `polished_diorite` |  |  |
| `5` | `polished_diorite_stairs[facing=north,half=top,shape=straight]` |  |  |
| `6` | `polished_diorite_stairs[facing=east,half=top,shape=straight]` |  |  |
| `7` | `polished_diorite_stairs[facing=south,half=top,shape=straight]` |  |  |
| `8` | `polished_diorite_stairs[facing=west,half=top,shape=straight]` |  |  |
| `l` | `polished_diorite_stairs[facing=north,half=bottom,shape=straight]` |  |  |
| `i` | `polished_diorite_stairs[facing=east,half=bottom,shape=straight]` |  |  |
| `o` | `polished_diorite_stairs[facing=south,half=bottom,shape=straight]` |  |  |
| `p` | `polished_diorite_stairs[facing=west,half=bottom,shape=straight]` |  |  |
| `3` | `yellow_concrete` |  |  |
| `\|` | `redstone_wall_torch[facing=east,lit=true]` |  |  |
| `w` | `diorite_wall` |  |  |
| `e` | `stone_stairs[facing=east,half=bottom,shape=straight]` |  |  |
| `k` | `rail[shape=north_south]` |  |  |
| `f` | `mossy_stone_brick_stairs[facing=east,half=bottom,shape=straight]` |  |  |
| `r` | `lever[face=wall,facing=north,powered=false]` |  |  |

### `quartzbricks_border`

File `data/lostcities/lostcities/palettes/quartzbricks_border.json`. 4 characters.

| Char | Maps to | Damaged to | Info |
|---|---|---|---|
| `X` | `infested_stone_bricks` | `iron_bars` |  |
| `$` | `smooth_stone_slab[type=double]` | `iron_bars` |  |
| `#` | variant `quartz` | `iron_bars` |  |
| `}` | variant `quartz_rubble` | `iron_bars` |  |

### `rails`

File `data/lostcities/lostcities/palettes/rails.json`. 12 characters.

| Char | Maps to | Damaged to | Info |
|---|---|---|---|
| `k` | `rail[shape=north_south]` |  |  |
| `e` | `rail[shape=east_west]` |  |  |
| `j` | `rail[shape=ascending_east]` |  |  |
| `q` | `rail[shape=south_east]` |  |  |
| `s` | `rail[shape=south_west]` |  |  |
| `r` | `rail[shape=north_west]` |  |  |
| `n` | `rail[shape=north_east]` |  |  |
| `m` | `powered_rail[powered=true,shape=north_south]` |  |  |
| `E` | `powered_rail[powered=true,shape=ascending_south]` |  |  |
| `H` | `powered_rail[powered=true,shape=ascending_north]` |  |  |
| `f` | `powered_rail[powered=true,shape=east_west]` |  |  |
| `i` | `powered_rail[powered=true,shape=ascending_east]` |  |  |

### `street_large`

File `data/lostcities/lostcities/palettes/street_large.json`. 2 characters.

| Char | Maps to | Damaged to | Info |
|---|---|---|---|
| `l` | `smooth_stone_slab[type=double]` |  |  |
| `q` | `smooth_quartz` |  |  |

## Local palettes

Inline palettes inside buildings and parts. They apply only to that building (all its floors) or that part, layered over the chunk palette.

### building `building6` (local palette)

| Char | Maps to | Damaged to | Info |
|---|---|---|---|
| `#` | variant `deepslate` | `iron_bars` |  |
| `}` | variant `deepslate_rubble` | `iron_bars` |  |

### building `building7` (local palette)

| Char | Maps to | Damaged to | Info |
|---|---|---|---|
| `#` | variant `blackstone` | `iron_bars` |  |
| `}` | variant `blackstone_rubble` | `iron_bars` |  |

### building `building8` (local palette)

| Char | Maps to | Damaged to | Info |
|---|---|---|---|
| `#` | variant `stoneandbrick` | `iron_bars` |  |
| `}` | variant `stoneandbrick_rubble` | `iron_bars` |  |

### part `cabin` (local palette)

| Char | Maps to | Damaged to | Info |
|---|---|---|---|
| `D` | `iron_door[half=lower]` |  |  |
| `d` | `iron_door[half=upper]` |  |  |
| `P` | `quartz_block` |  |  |
| `B` | `barrel[facing=north]` |  |  |
| `c` | `exposed_copper` |  |  |
| `1` | `exposed_cut_copper_stairs[facing=north]` |  |  |
| `2` | `exposed_cut_copper_stairs[facing=west]` |  |  |
| `3` | `exposed_cut_copper_stairs[facing=south]` |  |  |
| `4` | `exposed_cut_copper_stairs[facing=east]` |  |  |

### part `highway_gas_station` (local palette)

| Char | Maps to | Damaged to | Info |
|---|---|---|---|
| `p` | `gray_concrete` |  |  |
| `r` | `red_concrete` |  |  |
| `i` | `iron_block` |  |  |
| `e` | `sea_lantern` |  |  |
| `k` | `black_concrete` |  |  |
| `a` | `iron_door[facing=north,half=lower,hinge=left]` |  |  |
| `j` | `iron_door[facing=north,half=upper,hinge=left]` |  |  |

### part `highway_restaurant` (local palette)

| Char | Maps to | Damaged to | Info |
|---|---|---|---|
| `p` | `gray_concrete` |  |  |
| `r` | `red_concrete` |  |  |
| `a` | `iron_door[facing=north,half=lower,hinge=left]` |  |  |
| `j` | `iron_door[facing=north,half=upper,hinge=left]` |  |  |
| `n` | `lantern[hanging=true]` |  |  |
| `b` | `iron_bars` |  |  |

### part `highway_restaurant_parking` (local palette)

| Char | Maps to | Damaged to | Info |
|---|---|---|---|
| `p` | `gray_concrete` |  |  |
| `m` | `yellow_concrete` |  |  |
| `i` | `iron_block` |  |  |
| `e` | `sea_lantern` |  |  |
| `b` | `iron_bars` |  |  |

### part `park_trees` (local palette)

| Char | Maps to | Damaged to | Info |
|---|---|---|---|
| `P` | random: `oak_sapling[stage=1]` 32/128, `spruce_sapling[stage=1]` 32/128, `birch_sapling[stage=1]` 32/128, `acacia_sapling[stage=1]` 32/128 (w 1000) |  |  |

### part `top1x1_5` (local palette)

| Char | Maps to | Damaged to | Info |
|---|---|---|---|
| `o` | `oak_planks` |  |  |

## Variants

Each variant is a shared random list ([format](palettes-and-variants.md#variant-json)). Slots are out of 128 after compilation.

| Variant | Blocks (effective slots out of 128) | Used by |
|---|---|---|
| `blackstone` | `polished_blackstone_bricks` 32, `cracked_polished_blackstone_bricks` 32, `polished_blackstone` 64 (w 1000) | building `building7` `#` |
| `blackstone_rubble` | `polished_blackstone_bricks` 20, `cracked_polished_blackstone_bricks` 20, `polished_blackstone_slab[type=bottom]` 20, `polished_blackstone_stairs[facing=north,half=bottom]` 20, `polished_blackstone` 48 (w 1000) | building `building7` `}` |
| `bricks` | `bricks` 10, `red_terracotta` 118 (w 1000) | palette `bricks_border` `#` |
| `bricks_rubble` | `bricks` 10, `brick_stairs` 10, `brick_slab` 10, `red_terracotta` 98 (w 1000) | palette `bricks_border` `}` |
| `deepslate` | `cracked_deepslate_tiles` 32, `deepslate_tiles` 96 (w 1000) | building `building6` `#` |
| `deepslate_rubble` | `cracked_deepslate_tiles` 32, `cobbled_deepslate_slab[type=bottom]` 32, `cobbled_deepslate_stairs[facing=north,half=bottom]` 32, `deepslate_tiles` 32 (w 1000) | building `building6` `}` |
| `quartz` | `quartz_bricks` 10, `smooth_quartz` 118 (w 1000) | palette `quartzbricks_border` `#` |
| `quartz_rubble` | `quartz_bricks` 10, `quartz_stairs` 8, `quartz_slab` 10, `smooth_quartz` 100 (w 1000) | palette `quartzbricks_border` `}` |
| `stoneandbrick` | `cracked_stone_bricks` 32, `stone_bricks` 32, `stone` 64 (w 1000) | building `building8` `#` |
| `stoneandbrick_rubble` | `cracked_stone_bricks` 20, `stone_bricks` 20, `smooth_stone_slab[type=bottom]` 20, `stone_stairs[facing=east,half=bottom]` 20, `stone` 48 (w 1000) | building `building8` `}` |
| `stonebrick` | `cracked_stone_bricks` 9, `mossy_stone_bricks` 8, `stone_bricks` 111 (w 1000) | palette `bricks_standard` `#`, palette `default` `v`, palette `default` `x`, palette `default` `y` |
| `stonebrick_rubble` | `cracked_stone_bricks` 9, `mossy_stone_bricks` 8, `stone_brick_stairs` 5, `stone_brick_slab` 15, `stone_bricks` 91 (w 1000) | palette `bricks_standard` `}` |
