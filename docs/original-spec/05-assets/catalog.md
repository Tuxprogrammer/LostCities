# Catalog: bundled assets

Every bundled asset at `f18a094d` except palettes and variants, which are in [catalog-palettes.md](catalog-palettes.md). Paths are under `src/main/resources/data/lostcities/lostcities/`. Descriptions of what a structure looks like are *inferred* from names, part contents and palettes; nothing was generated in a running game. Floor counts of `profile` mean the building sets none (`-1`) and the profile and city style decide (`04-city-layout`). Floors are 6 blocks high.

## Counts

| Type | Files | Unused at the pin |
|---|---|---|
| buildings | 37 | none |
| multibuildings | 14 | none |
| parts | 207 | none (every part is reachable from a building, a selector, a scattered asset or a Java default) |
| palettes | 30 | `bricks_desert_redsand`, `glass_side_variant_street` |
| variants | 12 | none |
| styles | 4 | none |
| citystyles | 5 | none (`citystyle_border` is reached only through the `largecities` profile's `CITY_STYLE_ALTERNATIVE`) |
| conditions | 3 | none |
| worldstyles | 2 | `standard_everywhere` (*config-only*: no standard profile names it) |
| scattered | 5 | none |
| stuff | 3 | `example` (no `tags`, never indexed) |
| predefinedcities, predefinedspheres | 0 | n/a |
| **total** | **322** | |

## Buildings

Selector weights are the `factor`s in `citystyle_common` (inherited by `citystyle_standard`, `citystyle_desert` and `citystyle_border`); `citystyle_border` lists buildings 1 to 5 again, so in border cities their weight counts twice ([ASSET-06](index.md#suspected-bugs)). "Top" means the top floor, which is `floor >= floors above ground`, so a building has floors + 1 levels of parts above ground counting the roof level.

| Building | Floors above | Cellars | Main parts (`parts`) | Overlay (`parts2`) | Filler / rubble | Local palette | Chosen by | File |
|---|---|---|---|---|---|---|---|---|
| `building1` | profile | profile | `building1_1`…`_9` on non-top floors (cellars too); roof `top1x1_1`…`_5` | none | `#` / `}` | none | common 0.3 (+ border 0.3); `multi1` | `buildings/building1.json` |
| `building2` | profile | profile | `building2_1`…`_4`; roof `top1x1_1`…`_5` | none | `#` / `}` | none | common 0.4 (+ border 0.4); `multi2` | `buildings/building2.json` |
| `building3` | profile | profile | `building3_1`…`_4`; roof `top1x1_1`…`_5` | none | `#` / `}` | none | common 0.2 (+ border 0.2); `multi3` | `buildings/building3.json` |
| `building4` | profile | profile | `building4_1`…`_4`; roof `top4_1`…`_3` | none | `#` / `}` | none | common 0.2 (+ border 0.2); `multi4`, `huge1` | `buildings/building4.json` |
| `building5` | profile | profile | `building5_1`…`_4`; roof `top1x1_2`…`_5` | none | `#` / `}` | none | common 0.3 (+ border 0.3); `multi5`, `huge2` | `buildings/building5.json` |
| `building6` | profile | profile | as `building5` | none | `#` / `}` | `#` variant `deepslate`, `}` variant `deepslate_rubble` | common 0.2 | `buildings/building6.json` |
| `building7` | profile | profile | as `building4` | none | `#` / `}` | `#` variant `blackstone`, `}` variant `blackstone_rubble` | common 0.2 | `buildings/building7.json` |
| `building8` | profile | profile | as `building1` | none | `#` / `}` | `#` variant `stoneandbrick`, `}` variant `stoneandbrick_rubble` | common 0.4 | `buildings/building8.json` |
| `center00` | profile | profile | `center00_1`…`_3`; roof `top_center00_1`, `_2` | none | `#` / `}` | none | multibuilding `center` (0, 0) | `buildings/center00.json` |
| `center01` | profile | profile | `center01_1`, `_2`; roof `top_center_1`…`_4` | none | `#` / `}` | none | `center` (0, 1) | `buildings/center01.json` |
| `center10` | profile | profile | `center10_1`…`_4`; roof `top_center_1`…`_4` | none | `#` / `}` | none | `center` (1, 0) | `buildings/center10.json` |
| `center11` | profile | profile | `center11_1`; roof `top_center_1`…`_4` | none | `#` / `}` | none | `center` (1, 1) | `buildings/center11.json` |
| `library00` | profile | profile | `library00_1`; roof `top_library00_1`, `_2` | none | `#` / `}` | none | `library` (0, 0) | `buildings/library00.json` |
| `library01` | profile | profile | `library01_1`…`_3`; roof `top_library_1`…`_4` | none | `#` / `}` | none | `library` (0, 1) | `buildings/library01.json` |
| `library10` | profile | profile | `library10_1`…`_5`; roof `top_library_1`…`_4` | none | `#` / `}` | none | `library` (1, 0) | `buildings/library10.json` |
| `library11` | profile | profile | `library11_1`, `_2`; roof `top_library_1`…`_4` | none | `#` / `}` | none | `library` (1, 1) | `buildings/library11.json` |
| `shopping00` | 4–6 | max 0 | floor 0 `shopping00_0`, 1 `_1`, 2 `_2`, 3+ non-top `shopping00_r1`; roof `top_center_1`…`_4` | none | `#` / `}` | none | `shopping` (0, 0); `preferslonely` 0.9 | `buildings/shopping00.json` |
| `shopping01` | 4–6 | max 0 | `shopping01_0`/`_1`/`_2`, 3+ `shopping01_r1`; roof `top_center_*` | `shopping01_in_1`…`_3` on floor 0 and floors 3–100 non-top | `#` / `}` | none | `shopping` (0, 1); 0.9 | `buildings/shopping01.json` |
| `shopping10` | 4–6 | max 0 | `shopping10_0`/`_1`/`_2`, 3+ `shopping10_r1`; roof `top_center_*` | `shopping10_in_1`…`_3` on floor 0 and floors 2–100 non-top | `#` / `}` | none | `shopping` (1, 0) | `buildings/shopping10.json` |
| `shopping11` | 4–6 | max 0 | `shopping11_0`/`_1`/`_2`, 3+ `shopping11_r1`; roof `shopping11_top` | `shopping11_in_1`…`_3` on floor 0 and floors 2–100 non-top | `#` / `}` | none | `shopping` (1, 1) | `buildings/shopping11.json` |
| `shopping_open00` | 4–6 | max 0 | `shopping_open00_0`/`_1`/`_2`, 3+ `shopping00_r1`; roof `top_center_*` | none | `#` / `}` | none | `shopping_open` (0, 0); 0.9 | `buildings/shopping_open00.json` |
| `shopping_open01` | 4–6 | max 0 | `shopping_open01_0`/`_1`/`_2`, 3+ `shopping01_r1`; roof `top_center_*` | `shopping01_in_*` as `shopping01` | `#` / `}` | none | `shopping_open` (0, 1); 0.9 | `buildings/shopping_open01.json` |
| `shopping_open10` | 4–6 | max 0 | `shopping_open10_0`/`_1`/`_2`, 3+ `shopping10_r1`; roof `top_center_*` | `shopping10_in_*` as `shopping10` | `#` / `}` | none | `shopping_open` (1, 0) | `buildings/shopping_open10.json` |
| `shopping_open11` | 4–6 | max 0 | `shopping_open11_0`/`_1`/`_2`, 3+ `shopping11_r1`; roof `shopping11_top` | `shopping11_in_*` as `shopping11` | `#` / `}` | none | `shopping_open` (1, 1) | `buildings/shopping_open11.json` |
| `town00` | profile | profile | cellars `town00_c`; ground `town00_0`; upper `town00_1`; roof `top_center00_1`, `_2` | `town00_in_1`…`_3` on non-top floors | `#` / `}` | none | `townhall` (0, 0) | `buildings/town00.json` |
| `town01` | profile | profile | `town01_c`/`_0`/`_1`; roof `top_center_*` | `town01_in_1`, `town_in_1`…`_6` on non-top floors | `#` / `}` | none | `townhall` (0, 1) | `buildings/town01.json` |
| `town10` | profile | profile | `town10_c`/`_0`/`_1`; roof `top_center_*` | `town_in_1`…`_6` | `#` / `}` | none | `townhall` (1, 0) | `buildings/town10.json` |
| `town11` | profile | profile | `town11_c`/`_0`/`_1`; roof `top_center_*` | `town_in_1`…`_6` | `#` / `}` | none | `townhall` (1, 1) | `buildings/town11.json` |
| `cabin` | 1 | profile | `cabin` on every level | none | `#` / `}` | none | scattered `cabin` | `buildings/cabin.json` |
| `radiotower` | 1 | profile | `radiotower` | none | `#` / `}` | none | scattered `radiotower` | `buildings/radiotower.json` |
| `highway_gas_station` | 1 | profile | `highway_gas_station` | none | `Q` / `B` | none | scattered `highway_gas_station` | `buildings/highway_gas_station.json` |
| `highway_restaurant` | 1 | profile | `highway_restaurant` | none | `B` / `B` | none | multibuilding `highway_restaurant` (0, 0) | `buildings/highway_restaurant.json` |
| `highway_restaurant_parking` | 1 | profile | `highway_restaurant_parking` | none | `S` / `B` | none | `highway_restaurant` (1, 0) | `buildings/highway_restaurant_parking.json` |
| `oilrig00` | 1 | profile | `oilrig00` | none | `#` / `}` | none | multibuilding `oilrig` (0, 0) | `buildings/oilrig00.json` |
| `oilrig01` | 1 | profile | `oilrig01` | none | `#` / `}` | none | `oilrig` (0, 1) | `buildings/oilrig01.json` |
| `oilrig10` | 1 | profile | `oilrig10` | none | `#` / `}` | none | `oilrig` (1, 0) | `buildings/oilrig10.json` |
| `oilrig11` | 1 | profile | `oilrig11` | none | `#` / `}` | none | `oilrig` (1, 1) | `buildings/oilrig11.json` |

No bundled building sets `refpalette`, `allowDoors`, `allowFillers`, `overrideFloors` or `mincellars`. Every building's parts list covers every floor it can get from its own limits, so the "Misconfiguration" error is not reachable with the bundled assets alone; it depends on the constraints from the profile and city style (`04-city-layout`).

## Multibuildings

Grid positions are `(x, z)` chunk offsets from the top-left (north-west) chunk; the JSON's outer list is X.

| Multibuilding | Size (x × z chunks) | Buildings | Weight (`citystyle_common`) | File |
|---|---|---|---|---|
| `center` | 2 × 2 | `center00`, `center01` / `center10`, `center11` | 0.07 | `multibuildings/center.json` |
| `library` | 2 × 2 | `library00`, `library01` / `library10`, `library11` | 0.07 | `multibuildings/library.json` |
| `townhall` | 2 × 2 | `town00`, `town01` / `town10`, `town11` | 0.05 | `multibuildings/townhall.json` |
| `shopping` | 2 × 2 | `shopping00`, `shopping01` / `shopping10`, `shopping11` | 0.15 | `multibuildings/shopping.json` |
| `shopping_open` | 2 × 2 | `shopping_open00`…`11` | 0.12 | `multibuildings/shopping_open.json` |
| `multi1` | 2 × 2 | 4 × `building1` | 0.2 | `multibuildings/multi1.json` |
| `multi2` | 2 × 2 | 4 × `building2` | 0.2 | `multibuildings/multi2.json` |
| `multi3` | 2 × 2 | 4 × `building3` | 0.12 | `multibuildings/multi3.json` |
| `multi4` | 2 × 2 | 4 × `building4` | 0.12 | `multibuildings/multi4.json` |
| `multi5` | 2 × 2 | 4 × `building5` | 0.05 | `multibuildings/multi5.json` |
| `huge1` | 3 × 3 | 9 × `building4` | 0.05 | `multibuildings/huge1.json` |
| `huge2` | 3 × 3 | 9 × `building5` | 0.05 | `multibuildings/huge2.json` |
| `oilrig` | 2 × 2 | `oilrig00`, `oilrig01` / `oilrig10`, `oilrig11` | scattered `oilrig` | `multibuildings/oilrig.json` |
| `highway_restaurant` | 2 × 1 | `highway_restaurant` / `highway_restaurant_parking` | scattered `highway_restaurant` | `multibuildings/highway_restaurant.json` |

`citystyle_border` declares `"multibuildings": []` but inherits all twelve city multibuildings from `citystyle_common` ([ASSET-06](index.md#suspected-bugs)).

## Part families

207 parts in 32 families. "Footprint" is x × z × height in blocks (with the number of parts if more than one). "Chars" is the union of palette characters the family uses ([character conventions](catalog-palettes.md#character-conventions)). "Palettes" lists part-level palettes; every other character comes from the chunk palette.

| Family | Parts | Footprint | Depicts (*inferred*) | Chars | Palettes | Used by |
|---|---|---|---|---|---|---|
| `building1_*` | 9 | 16×16×6 | One office-tower floor: wall ring with windows and window-side columns, interior walls, stairs, furniture, chests, spawners, bookshelves | `` #*1:@CDFLQTWXal`` | none | `building1`, `building8` |
| `building2_*` | 4 | 16×16×6 | Floor with accent banding (`$`) | ``#$*1:CFLTal`` | none | `building2` |
| `building3_*` | 4 | 16×16×6 | Floor with cobwebs (`%`) and chests | ``#%1:CFLTal`` | none | `building3` |
| `building4_*` | 4 | 16×16×6 | Floor with smooth slab floors and stone-brick stairs | ``#*1:CFQSTacd`` | none | `building4`, `building7` |
| `building5_*` | 4 | 16×16×6 | Floor with slab floors, fences, pressure plates, flowers | ``#-.=DFKLT_al`` | none | `building5`, `building6` |
| `top1x1_*` | 5 | 16×16×6 (4), 16×16×8 | Roofs: iron-bar railings, glass, quartz; `top1x1_5` a stair-stepped oak-plank roof | ``#:AQ_acotuz`` | `top1x1_5` local (`o` oak planks) | buildings 1, 2, 3, 5, 6, 8 |
| `top4_*` | 3 | 16×16×1, ×3, ×4 | Low roofs with railings and slabs | ``#:_`` | none | `building4`, `building7` |
| `center*_*` | 10 | 16×16×6 | Quarters of a 2 × 2 civic center with quartz trim and glass | ``#)+14:CFQSTa`` | none | `center00`…`11` |
| `top_center*` | 6 | 16×16×1 (2), ×3, ×4 (3) | Roofs for centers, shopping and town halls: flat, railing, glass, roof garden (`G`, `D`, `p`) | ``#):DGapw`` | none | center, shopping, town buildings |
| `library*_*` | 11 | 16×16×6 | Quarters of a 2 × 2 library with bookshelves and cobwebs | ``#%)*-.14:CFLQSTaw`` | none | `library00`…`11` |
| `top_library*` | 6 | 16×16×1 (2), ×3, ×4 (3) | Library roofs (same set of shapes as `top_center*`) | ``#):DGapw`` | none | `library*` |
| `shopping*_{0,1,2,r1,top}` | 17 | 16×16×6 (16), 16×16×1 | Quarters of a 2 × 2 mall: ground floor, two lower floors, repeating upper floor, empty roof (`shopping11_top`) | ``#):QST`aw`` | none | `shopping*`, `shopping_open*` (the `r1` parts) |
| `shopping*_in_*` | 9 | 16×16×1, ×3 (5), ×4 (3) | Mall interior overlays: pools, planters, glass floors, benches; `shopping11_in_3` is empty | ``-.DKLQSWZ_`` | none | `parts2` of shopping buildings |
| `shopping_open*_*` | 12 | 16×16×6 | Mall floors with open sides (fewer windows than `shopping*`) | ``#):QST`aw`` | none | `shopping_open*` |
| `town*_{0,1,c}` | 12 | 16×16×6 | Town-hall quarters: cellar, ground floor, upper floor | ``#):QSTa`` | none | `town*` |
| `town*_in_*` | 10 | 16×16×1, ×2 (2), ×3 (2), ×5 (5) | Town-hall interior overlays: treasure rooms (`!`, chests, spawners), pools, furniture; `town_in_1` is empty | ``!#-.14CDFKQW_au`` | none | `parts2` of `town*` |
| `cabin`, `radiotower` | 2 | 16×16×8, 16×16×24 | A copper-roofed cabin with barrels and iron doors; a tall iron-bar radio mast with a redstone-torch beacon | ``#1234:BCDPcdg`` | `cabin` local (doors, quartz, barrel, copper, copper stairs) | scattered |
| `oilrig*` | 4 | 16×16×21, 16×16×22 (3) | Quarters of an ocean oil rig on pillars: decks, ladders, barrels, iron doors, diorite stairs, sea lanterns | 46 chars | `refpalette` `oilrig` | multibuilding `oilrig` |
| `highway_gas_station`, `highway_restaurant*` | 3 | 16×16×7 (2), 16×16×4 | Roadside gas station; diner; parking lot with yellow lines | ``-.BCFLQYabehijkmnpr`` | local palettes (concrete colors, iron doors, lanterns, sea lanterns) | scattered |
| `roadside_supports` | 1 | 16×16×1 | Brick support slice repeated under roadside structures | ``B`` | none | scattered `supportpart` |
| `street_*` | 8 | 16×16×1 (7), 16×16×7 | Street tiles by connection shape, and a street slope (`street_stair`, `z1` 4, `z2` 11) | ``BS_b`` | none | `StreetParts` defaults |
| `street_large_*` | 9 | 16×16×1 (8), 16×16×7 | Wide-street tiles with quartz lane markings; connector; slope (`z1` 1, `z2` 14) | `` _blq`` | `refpalette` `street_large` | `citystyle_common.streetblocks.largeparts` |
| `building_front*` | 3 | 2×16×4, 3×16×4 (2) | Porches or awnings along a building's street side | ``#_aw`` | none | city-style `fronts` |
| `stairs*` | 4 | 2×13×6, 2×16×6, 6×11×7, 6×12×7 | Stairways from street level down to a lower street (`z1`/`z2` mark the opening) | ``/=T_wx`` | none | city-style `stairs` |
| `park_*` | 8 | 16×16×1 to ×6 | Parks: pool, flower beds, trees (local sapling list), pillared planters, fountains, small pavilions | ``#DGPSTW_puwx`` | `park_trees` local (`P` random saplings) | city-style `parks` |
| `fountain*` | 3 | 16×16×2 (2), 16×16×4 | Street fountains | ``#:TW`` | none | city-style `fountains` |
| `bridge_*` | 3 | 16×16×4 (2), 16×16×7 | Open bridge, covered bridge, wide open bridge | ``STlqvw`` | `bridge_large_open`: `refpalette` `street_large` | city-style `bridges`, `largebridges` |
| `highway_{tunnel,open,bridge}*` | 12 | 16×16×6 (4), ×9 (4), ×12 (4) | Highway in tunnel, open cut, or on a bridge; straight, bidirectional (`_bi`), bend, T | ``:RTuvw~`` | none | `HighwayParts` defaults |
| `rails_*` | 10 | 16×16×6 (7), ×4, ×12, ×18 | Underground railway segments: straight, end, under water, vertical, 3-way split, bend, flat, one and two levels down | ``+:T_efgijkmnqrsy~`` | `refpalette` `rails` | `RailwayParts` defaults |
| `station_*` | 6 | 16×16×2 to ×7 | Railway stations: underground, open, open with roof, stair access, staircases to the surface | ``()+:=QT_efghwy~`` | `refpalette` `rails` | `RailwayParts` defaults |
| `rail_dungeon*` | 2 | 16×16×6 | Rooms next to railways with chests (`raildungeonchest` weight), spawners and treasure | ``!14CFy~`` | `refpalette` `rails` | city-style `raildungeons` |
| `monorails_*` | 3 | 16×16×10 (2), 16×16×4 | Elevated monorail track, crossing and station (meta `nowater`) | ``()EHQYefgkmwy~`` | `refpalette` `rails` | `MonorailParts` defaults |

Meta used by bundled parts: `dontconnect` on 15 shopping parts; `z1`/`z2` on `stairs1` (0, 2), `stairs2` (13, 15), `stairsbig` (4, 11), `stairsnormal` (5, 10), `street_stair` (4, 11), `street_large_stair` (1, 14); `nowater` on the three monorail parts.

## Styles

| Style | Groups (one palette picked per group, equal weights unless stated) | Used by |
|---|---|---|
| `standard` | `common` · `default` · one of `bricks_standard`, `bricks_cyan`, `bricks_gray`, `bricks_silver` · one of 10 glass palettes (`glass_full`, `glass_pane`, and `_white`, `_blue`, `_light_blue`, `_gray` of each) · one of `glass_side_variant_glass` (listed twice, so 2/4), `glass_side_variant_bricks`, `glass_side_variant_quartz` | `citystyle_standard` |
| `desert` | `common` · `default_desert` · one of `bricks_desert`, `bricks_desert_red`, `bricks_desert_orange` · the 10 glass palettes · the side variants as `standard` | `citystyle_desert` |
| `standard_border` | `common` · `default` · one of `bricks_border`, `quartzbricks_border` · the 10 glass palettes · the side variants as `standard` | `citystyle_border` |
| `outside` | `common` · `default` · `bricks_standard` · `glass_full` · `glass_side_variant_glass` | world styles' `outsidestyle` |

## City styles

| City style | Inherits | Style | Own settings | Own selectors | Reached from |
|---|---|---|---|---|---|
| `citystyle_config` | none | none | `streetblocks.width` 8 (never read) | none | parent of `citystyle_common` |
| `citystyle_common` | `citystyle_config` | none | `stuff_tags` `rubble`; `streetblocks` border `y`, wall `w`, street `S`, streetbase `b`, streetvariant `B`, `largeparts` = the nine `street_large_*`; `parkblocks.elevation` `x`; `corridorblocks` roof `x`, glass `+`; `railblocks.railmain` `y`; `sphereblocks` glass `Z`, border `9`, inner `b` | buildings 1–8 (weights in [Buildings](#buildings)); 12 multibuildings ([Multibuildings](#multibuildings)); bridges `bridge_open`, `bridge_covered` (1 each); largebridges `bridge_large_open`; fronts `building_front1`–`3`; stairs `stairs1`, `stairs2`, `stairsnormal`, `stairsbig`; fountains `fountain1`–`3`; parks `park_pool`, `park_plants`, `park_trees`, `park_plants_pillars` (1.0), `park_fountain1`, `park_fountain2` (0.8), `park_building`, `park_building2` (0.3); raildungeons `rail_dungeon1`, `rail_dungeon2` | parent of the three below |
| `citystyle_standard` | `citystyle_common` | `standard` | none | none | world styles, factor 0.5, any biome |
| `citystyle_desert` | `citystyle_common` | `desert` | none | none | world styles, factor 9.0, biomes `minecraft:desert`, `minecraft:badlands` |
| `citystyle_border` | `citystyle_common` | `standard_border` | `buildingsettings` maxfloors 1, maxcellars 1, buildingchance 0.2; the same street, park, corridor, rail and sphere characters as common | buildings 1–5 again; `multibuildings: []`; the same bridges, fronts, stairs, fountains, parks and rail dungeons again (no `largebridges`) | profile `largecities` (`CITY_STYLE_ALTERNATIVE`) |

After inheritance every bundled city style has the same characters, no `generalblocks` (so iron bars, glowstone, leaves and rubble dirt fall back to the Java defaults), no `explosionchance` (every explosion applies), and `stuff_tags` {`all`, `rubble`}. In `citystyle_border` every selector entry it repeats from common counts twice.

## Conditions

| Condition | Used by | Values (factor, guard) |
|---|---|---|
| `chestloot` | `common` `C` (`loot`) | `lostcities:chests/lostcitychest` 8 on floors 4–100 and 8 on floors −100 to −3; `lostcities:chests/raildungeonchest` 20 in `rail_dungeon1` and 20 in `rail_dungeon2`; `minecraft:chests/simple_dungeon` 30; 1 each, unguarded: `buried_treasure`, `desert_pyramid`, `end_city_treasure`, `igloo_chest`, `jungle_temple`, `jungle_temple_dispenser`, `pillager_outpost`, `shipwreck_supply`, `shipwreck_treasure`, `spawn_bonus_chest`, `stronghold_corridor`, `stronghold_crossing`, `stronghold_library`, `underwater_ruin_big`, `underwater_ruin_small`, `woodland_mansion`, and 16 `village/village_*` tables (armorer, butcher, cartographer, desert_house, fisher, fletcher, mason, plains_house, savanna_house, shepherd, snowy_house, taiga_house, tannery, temple, toolsmith, weaponsmith) — 37 entries |
| `easymobs` | `common` `1` (`mob`) | `minecraft:zombie`, `minecraft:skeleton`, `minecraft:spider`, 1 each |
| `hardmobs` | `common` `4` (`mob`) | `minecraft:blaze`, `minecraft:witch`, 1 each |

So an ordinary chest on floors 0–3 draws from 33 tables with total weight 62, `simple_dungeon` being 30/62; a chest in a rail dungeon adds 20 for `raildungeonchest`. Because of truncating division, chests in the first two cellars count as floors 0 and −1 and the `-100,-3` guard starts lower than intended ([ASSET-05](index.md#suspected-bugs)). The loot tables themselves are `08-loot-mobs-blocks`.

## World styles

| World style | Outside style | City styles | Biome multipliers | Scattered | Other |
|---|---|---|---|---|---|
| `standard` (profile default) | `outside` | `citystyle_standard` 0.5 anywhere; `citystyle_desert` 9.0 in `minecraft:desert`, `minecraft:badlands` | 0.1 in `#minecraft:is_ocean`, 0.3 in `#minecraft:is_river` | areasize 8, chance 0.7, weightnone 30: `radiotower` 15 (maxheightdiff 3), `oilrig` 4 (100, only `#minecraft:is_deep_ocean`), `cabin` 10 (2), `highway_gas_station` 12 (12, near highway), `highway_restaurant` 9 (12, near highway); all but the oil rig exclude ocean, river and beach tags | bridge and highway support `v`; settings `ignore`, railpartheight6 1; multisettings 10/1/5/0.8/50 |
| `standard_everywhere` (*config-only*) | `outside` | as `standard` | none | areasize 8, chance 0.1, weightnone 20, same list with `oilrig` weight 1 | supports `v`; multisettings 10/1/5 with default factor and attempts; settings default |

In a desert or badlands biome the desert style is chosen with probability 9 / 9.5; elsewhere `citystyle_standard` is the only candidate.

## Scattered

| Scattered | Buildings | Rotatable | Clear railing | Support part | Terrain height | Terrain fix | Height offset |
|---|---|---|---|---|---|---|---|
| `cabin` | `cabin` | no | no | none | highest | none | −3 |
| `radiotower` | `radiotower` | no | no | none | highest | none | −3 |
| `highway_gas_station` | `highway_gas_station` | yes | yes | `roadside_supports` | highest | repeatslice | 0 |
| `highway_restaurant` | multibuilding `highway_restaurant` | yes | yes | `roadside_supports` | highest | repeatslice | 0 |
| `oilrig` | multibuilding `oilrig` | no | no | none | ocean | repeatslice | 1 |

## Stuff

| Stuff | Tags | Column | Count (min, max exclusive) | Attempts | In building | Height | Matchers |
|---|---|---|---|---|---|---|---|
| `chains` | `rubble` | `{` (chain) | 5, 10 | 8 | yes | default | block above must not be air |
| `cobweb` | `rubble` | `\` (cobweb) | 5, 20 | 8 | yes | default | block above must not be air |
| `example` (*unused*) | none | `:::h` (iron bars ×3 and glowstone) | 2, 7 | 10 | no | max 120; sees sky | not ocean, river or beach; on `#forge:stone` or andesite; buildings `building1`–`3` |
