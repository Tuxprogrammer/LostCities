# Block catalog: Tinkers' Construct

Mod id `TConstruct`, jar `TConstruct-1.14.123-GTNH.jar`, source read at tag `1.14.123-GTNH` (`gtnh-src/source/TinkersConstruct`: `world/TinkerWorld.java`, `smeltery/TinkerSmeltery.java`, `tools/TinkerTools.java`, `armor/TinkerArmor.java`, and the block classes under `smeltery/blocks`, `tools/blocks`, `world/blocks`, `blocks/`). Pack config checked: `config/TinkersModules.cfg` (Smeltery, Armory, Mechworks, Weaponry and Gears modules all on) and `config/TinkersConstruct.cfg` (connected textures on, slime islands generate in the overworld). Sheets: [`brownstone`](../../../../tools/block-contact-sheet/sheets/tinkers/brownstone.json), [`seared`](../../../../tools/block-contact-sheet/sheets/tinkers/seared.json), [`bricks`](../../../../tools/block-contact-sheet/sheets/tinkers/bricks.json), [`glass`](../../../../tools/block-contact-sheet/sheets/tinkers/glass.json), [`misc`](../../../../tools/block-contact-sheet/sheets/tinkers/misc.json) under `tools/block-contact-sheet/sheets/tinkers/`. This catalog serves D25 in the [decision log](../../../design/decision-log.md): variety for palettes, with the [D19 mapping](../../00-pack-integration/block-mapping.md) as baseline.

Registry names are `TConstruct:<name>` with the name passed to `GameRegistry.registerBlock` (`SpeedBlock`, `Smeltery`, `decoration.multibrick`, `GlassBlock.StainedClear` and so on).

## Summary

- **212 block+meta entries in 43 registry blocks.**
- Fit upstream's look: 58 `yes`, 97 `partly`, 57 `no`.
- Not GT ore hosts or boulder hosts ([ore-veins.md](../../00-pack-integration/ore-veins.md)). TiC's own ores (`SearedBrick` ore block, `GravelOre`, ore berries) are excluded from this catalog.
- **The wooden rail (`rail.wood`) is not registered in this pack**: `TinkerWorld` skips it when `dreamcraft` (NewHorizonsCoreMod) is loaded.

| Primary role | Entries |
|---|---|
| wall-main | 9 (brownstone 2, seared/scorched bricks 2, polished stone brick, sandstone brick, endstone brick, bone brick, fancy stone brick) |
| wall-accent | 19 (including the iron brick, a D29 metal accent) |
| trim-pillar | 4 (chiseled and fancy brownstone, chiseled seared and scorched) |
| floor | 4 (seared and scorched paver, road) |
| street | 3 (two brownstone roads, stone road) |
| rubble-ruin | 6 (seared and scorched cobble and cracked bricks, grout, consecrated soil) |
| glass | 17 (clear glass, 16 stained) |
| pane | 17 |
| stairs-slab | 31 (7 brownstone slabs, 8 seared slabs, 16 wool slabs) |
| light | 1 (stone torch) |
| door-trap | 4 (barricades) |
| furniture-deco | 21 (crafting and tool stations, slabs, drying rack) |
| industrial-dressing | 32 (6 common-metal blocks, tanks, casting blocks, tool forges) |
| foliage | 5 (slime grass, leaves, sapling, tall grass, graveyard soil) |
| special | 2 (stone ladder, punji sticks) |
| off-theme | 37 (precious, slime and Tinkers-metal bricks and blocks, slime blocks and soils, glue, hambone, pads and channels) |

Flags:

- **Tile entities: every seared and scorched smeltery block is one.** `SmelteryBlock.createNewTileEntity` returns `MultiServantLogic` for metas 2 and 4 to 11 (the decorative bricks, stone, cobble, paver, road, fancy, chiseled), so a seared wall is a wall of tile entities. The seared **slab** (`SearedSlab`) is a plain block. Also tile entities: tanks, casting blocks, crafting and tool stations, crafting slabs, furnace slab, drying rack, tool forges.
- **Light:** stone torch 14. Nothing else emits light.
- **Hardness:** seared and scorched blocks 3; **seared slabs 12** (eight times stone brick); metal blocks 10; multibricks copy the source block (obsidian bricks hardness 50, diamond bricks 5, and so on); barricades 4; punji 3.
- **Speed:** every brownstone meta speeds up walking, not only the roads. `SpeedBlock.onWalkedOn` multiplies a walking player's horizontal motion by 1.15 on metas 0, 2, 3, 5 and 6 and by 1.3 on the two roads (metas 1 and 4), unless the player sneaks or is wet (`TPlayerHandler` calls it client-side for the local player). `SpeedSlab.onEntityWalking` boosts any entity on any slab meta harder (2.2, or 2.7 on the road slabs, while horizontal motion is below 0.5). D28 rule 3: brownstone is walked on only on highways and primary roads; the blocks may stand in walls and trims, and the slabs, which are stairs-slab and sidewalk pieces, stay out.
- **Value:** gold, diamond, lapis, redstone and metal bricks and the metal blocks are resource stores; a building of them is loot. Under D29 the iron bricks and the copper, bronze, tin, aluminum, aluminum brass, steel and cobalt blocks come back as accents behind their metal flags, under a per-building cap ([D29](index.md#decorative-metals-d29)); the gold bricks fall under the gold flag but look off-theme, and the gem and redstone bricks and the other metal blocks stay loot.
- **Punji sticks** damage entities that walk into them.
- **Gravity:** none.

## Brownstone

`TConstruct:SpeedBlock` (hardness 3) and `TConstruct:SpeedSlab` (+8 top). Tan sandstone-colored stone.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `TConstruct:SpeedBlock:0` | Rough Brownstone | wall-main | yes: rough tan stone, a sandstone partner | desert, ruined | Speeds walking (1.15×), so not rubble |
| `TConstruct:SpeedBlock:1` | Rough Brownstone Road | street | yes: gray cobbled paving | urban, desert | Speeds walking (1.3×): highways and primary roads only |
| `TConstruct:SpeedBlock:2` | Brownstone | wall-main | yes: smooth tan stone, like cut sandstone | desert, urban | Speeds walking (1.15×) |
| `TConstruct:SpeedBlock:3` | Brownstone Brick | wall-accent, wall-main | yes: tan bricks, a `bricks_desert` alternative | desert, urban | Speeds walking (1.15×) |
| `TConstruct:SpeedBlock:4` | Brownstone Road | street | yes: tan paved road | desert | Speeds walking (1.3×): highways and primary roads only |
| `TConstruct:SpeedBlock:5` | Fancy Brownstone | trim-pillar, wall-accent | yes: panel pattern | desert | Speeds walking (1.15×) |
| `TConstruct:SpeedBlock:6` | Chiseled Brownstone | trim-pillar, wall-accent | yes: square-in-square relief | desert | Speeds walking (1.15×) |
| `TConstruct:SpeedSlab:0-6` (+8 top) | Brownstone Slabs (7 kinds, metas as above) | stairs-slab | yes | desert | 7 entries; every slab speeds walking (2.2×, road slabs 2.7×), so no slab, curb or sidewalk use (D28 rule 3) |

## Seared and scorched blocks

`TConstruct:Smeltery` (seared, dark gray) and `TConstruct:SmelteryNether` (scorched, dark brown), metas 2 and 4 to 11; meta 3 is unused and metas 0 and 1 are the controller and drain. `TConstruct:SearedSlab` metas 0 to 7 (+8 top): bricks, stone, cobble, paver, road, fancy, chiseled, creeper.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `TConstruct:Smeltery:2` | Seared Bricks | wall-main, wall-accent | yes: dark gray bricks, like deepslate bricks | urban, industrial | TE |
| `TConstruct:Smeltery:4` | Seared Stone | wall-accent | yes: dark smooth stone | urban, industrial | TE |
| `TConstruct:Smeltery:5` | Seared Cobblestone | rubble-ruin | yes: dark cobble | ruined, industrial | TE |
| `TConstruct:Smeltery:6` | Seared Paver | floor | yes: dark paving | urban, industrial | TE |
| `TConstruct:Smeltery:7` | Cracked Seared Bricks | rubble-ruin, wall-accent | yes | ruined | TE |
| `TConstruct:Smeltery:8` | Seared Road | floor, street | yes: dark large tiles | urban, industrial | TE |
| `TConstruct:Smeltery:9` | Fancy Seared Bricks | wall-accent | yes | urban, industrial | TE |
| `TConstruct:Smeltery:10` | Chiseled Seared Bricks | trim-pillar | yes | urban, industrial | TE |
| `TConstruct:Smeltery:11` | Chiseled Seared Bricks (creeper face) | wall-accent | partly: creeper face | industrial | TE; top is paver |
| `TConstruct:SmelteryNether:2` | Scorched Bricks | wall-main, wall-accent | partly: dark brown, soot-stained brick | ruined, industrial | TE |
| `TConstruct:SmelteryNether:4` | Scorched Stone | wall-accent | partly | ruined, industrial | TE |
| `TConstruct:SmelteryNether:5` | Scorched Cobblestone | rubble-ruin | partly: burned rubble | ruined | TE |
| `TConstruct:SmelteryNether:6` | Scorched Paver | floor | partly | ruined, industrial | TE |
| `TConstruct:SmelteryNether:7` | Scorched Cracked Bricks | rubble-ruin | partly: fire-damaged brick | ruined | TE |
| `TConstruct:SmelteryNether:8` | Scorched Road | floor | partly | industrial | TE |
| `TConstruct:SmelteryNether:9` | Scorched Fancy Bricks | wall-accent | partly | industrial | TE |
| `TConstruct:SmelteryNether:10` | Scorched Chiseled Bricks | trim-pillar | partly | industrial | TE |
| `TConstruct:SmelteryNether:11` | Scorched Chiseled Bricks (creeper face) | wall-accent | no | none | TE |
| `TConstruct:SearedSlab:0-7` (+8 top) | Seared Slabs (8 kinds) | stairs-slab, rubble-ruin | yes: dark slabs without tile entities | urban, industrial, ruined | 8 entries; hardness 12 |

## Decorative bricks

`TConstruct:decoration.multibrick` (14 metas), `decoration.multibrickfancy` (16), `decoration.multibrickmetal` (8). Hardness and blast resistance copy the source block.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `TConstruct:decoration.multibrick:0` | Obsidian Brick | wall-accent | partly: purple-black brick | industrial | Hardness 50 |
| `TConstruct:decoration.multibrick:1` | Sandstone Brick | wall-main | yes: pale sandstone bricks | desert | |
| `TConstruct:decoration.multibrick:2` | Netherrack Brick | wall-accent, rubble-ruin | partly: dull red brick | ruined | |
| `TConstruct:decoration.multibrick:3` | Polished Stone Brick | wall-main | yes: light gray brick, a `stonebrick` alternative | urban | |
| `TConstruct:decoration.multibrick:4` | Iron Brick | wall-accent | partly: white metal brick | industrial, urban | Hardness 5; iron value |
| `TConstruct:decoration.multibrick:5` | Gold Brick | off-theme | no | none | Gold value |
| `TConstruct:decoration.multibrick:6` | Lapis Brick | off-theme | no | none | Lapis value |
| `TConstruct:decoration.multibrick:7` | Diamond Brick | off-theme | no | none | Diamond value |
| `TConstruct:decoration.multibrick:8` | Redstone Brick | off-theme | no | none | Redstone value |
| `TConstruct:decoration.multibrick:9` | Bone Brick | wall-main | partly: cream brick | desert | |
| `TConstruct:decoration.multibrick:10` | Slime Brick | off-theme | no | none | |
| `TConstruct:decoration.multibrick:11` | Blue Slime Brick | off-theme | no | none | |
| `TConstruct:decoration.multibrick:12` | Endstone Brick | wall-main | partly: pale yellow brick | desert | |
| `TConstruct:decoration.multibrick:13` | Obsidian Brick (from ingots) | wall-accent | partly: navy-black brick | industrial | Hardness 50 |
| `TConstruct:decoration.multibrickfancy:0` | Fancy Obsidian Brick | wall-accent | partly | industrial | Hardness 50 |
| `TConstruct:decoration.multibrickfancy:1` | Fancy Sandstone Brick | wall-accent, trim-pillar | yes | desert | |
| `TConstruct:decoration.multibrickfancy:2` | Fancy Netherrack Brick | wall-accent | partly | ruined | |
| `TConstruct:decoration.multibrickfancy:3` | Fancy Polished Stone Brick | wall-accent | yes: gray panel, a chiseled stone brick alternative | urban | |
| `TConstruct:decoration.multibrickfancy:4` | Fancy Iron Brick | wall-accent | partly | industrial | |
| `TConstruct:decoration.multibrickfancy:5` | Fancy Gold Brick | off-theme | no | none | Gold value |
| `TConstruct:decoration.multibrickfancy:6` | Fancy Lapis Brick | off-theme | no | none | |
| `TConstruct:decoration.multibrickfancy:7` | Fancy Diamond Brick | off-theme | no | none | |
| `TConstruct:decoration.multibrickfancy:8` | Fancy Redstone Brick | off-theme | no | none | |
| `TConstruct:decoration.multibrickfancy:9` | Fancy Bone Brick | wall-accent | partly | desert | |
| `TConstruct:decoration.multibrickfancy:10` | Fancy Slime Brick | off-theme | no | none | |
| `TConstruct:decoration.multibrickfancy:11` | Fancy Blue Slime Brick | off-theme | no | none | |
| `TConstruct:decoration.multibrickfancy:12` | Fancy Endstone Brick | wall-accent | partly | desert | |
| `TConstruct:decoration.multibrickfancy:13` | Fancy Obsidian Brick (from ingots) | wall-accent | partly | industrial | |
| `TConstruct:decoration.multibrickfancy:14` | Fancy Stone Brick | wall-main, wall-accent | yes: mid-gray panel brick | urban | |
| `TConstruct:decoration.multibrickfancy:15` | Stone Road | street, sidewalk | yes: gray paving slabs | urban | |
| `TConstruct:decoration.multibrickmetal:0` | Alumite Brick | off-theme | no: pink | none | |
| `TConstruct:decoration.multibrickmetal:1` | Ardite Brick | off-theme | no: orange | none | |
| `TConstruct:decoration.multibrickmetal:2` | Cobalt Brick | off-theme | no: blue | none | |
| `TConstruct:decoration.multibrickmetal:3` | Manyullyn Brick | off-theme | no: purple | none | |
| `TConstruct:decoration.multibrickmetal:4` | Fancy Alumite Brick | off-theme | no | none | |
| `TConstruct:decoration.multibrickmetal:5` | Fancy Ardite Brick | off-theme | no | none | |
| `TConstruct:decoration.multibrickmetal:6` | Fancy Cobalt Brick | off-theme | no | none | |
| `TConstruct:decoration.multibrickmetal:7` | Fancy Manyullyn Brick | off-theme | no | none | |

## Glass

Connected textures (`Connected Textures Enabled=true`): the sheet shows each block's plain face. Clear glass is `TConstruct:GlassBlock:0`, its pane `TConstruct:GlassPane:0`. Stained clear glass is `TConstruct:GlassBlock.StainedClear` and its panes `TConstruct:GlassPaneClearStained`, metas in dye order. Each color row below covers the block and the pane (2 entries).

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `TConstruct:GlassBlock:0` | Clear Glass | glass | yes: clean frameless glass | urban, industrial | |
| `TConstruct:GlassPane:0` | Clear Glass Pane | pane | yes | urban, industrial | |
| `GlassBlock.StainedClear:0`, `GlassPaneClearStained:0` | White Stained (Clear) Glass and Pane | glass, pane | yes: upstream's `Y` white glass | urban | |
| `…:1` | Orange | glass, pane | partly: colorful | urban | |
| `…:2` | Magenta | glass, pane | partly | urban | |
| `…:3` | Light Blue | glass, pane | yes: upstream glass color | urban | |
| `…:4` | Yellow | glass, pane | partly | urban | |
| `…:5` | Lime | glass, pane | partly | urban | |
| `…:6` | Pink | glass, pane | partly | urban | |
| `…:7` | Gray | glass, pane | yes: upstream glass color | urban, industrial | |
| `…:8` | Light Gray | glass, pane | yes | urban | |
| `…:9` | Cyan | glass, pane | yes: matches the cyan terracotta palette | urban | |
| `…:10` | Purple | glass, pane | partly | urban | |
| `…:11` | Blue | glass, pane | yes: upstream glass color | urban | |
| `…:12` | Brown | glass, pane | partly: smoked glass | urban, ruined | |
| `…:13` | Green | glass, pane | partly | urban | |
| `…:14` | Red | glass, pane | partly | urban | |
| `…:15` | Black | glass, pane | yes: upstream's oil-rig `I` black glass | urban, industrial | |

## Metal blocks, soils and small decorations

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `TConstruct:MetalBlock:0` | Block of Cobalt | off-theme | no: blue patterned | none | Hardness 10 |
| `TConstruct:MetalBlock:1` | Block of Ardite | off-theme | no | none | |
| `TConstruct:MetalBlock:2` | Block of Manyullyn | off-theme | no | none | |
| `TConstruct:MetalBlock:3` | Block of Copper | industrial-dressing | partly: copper plate | industrial | Resource value |
| `TConstruct:MetalBlock:4` | Block of Bronze | industrial-dressing | partly | industrial | |
| `TConstruct:MetalBlock:5` | Block of Tin | industrial-dressing | partly | industrial | |
| `TConstruct:MetalBlock:6` | Block of Aluminum | industrial-dressing, wall-accent | partly: light gray plate | industrial | |
| `TConstruct:MetalBlock:7` | Block of Aluminum Brass | industrial-dressing | partly: riveted brass | industrial | |
| `TConstruct:MetalBlock:8` | Block of Alumite | off-theme | no: pink | none | |
| `TConstruct:MetalBlock:9` | Block of Steel | industrial-dressing, wall-accent | partly: gray plate | industrial | |
| `TConstruct:MetalBlock:10` | Block of Solid Ender | off-theme | no | none | |
| `TConstruct:slime.gel:0-2` | Congealed Blue, Green, Purple Slime | off-theme | no | none | 3 entries; bouncy |
| `TConstruct:CraftedSoil:0` | Slimy Mud (green) | off-theme | no | none | |
| `TConstruct:CraftedSoil:1` | Grout | rubble-ruin | partly: gray aggregate, reads as broken concrete | ruined | |
| `TConstruct:CraftedSoil:2` | Slimy Mud (blue) | off-theme | no | none | |
| `TConstruct:CraftedSoil:3` | Graveyard Soil | foliage | partly: dark soil for overgrown lots | overgrown | |
| `TConstruct:CraftedSoil:4` | Consecrated Soil | rubble-ruin | partly: brown soil | overgrown | |
| `TConstruct:CraftedSoil:5` | Slime Soil | off-theme | no | none | |
| `TConstruct:CraftedSoil:6` | Nether Grout | off-theme | no | none | |
| `TConstruct:GlueBlock:0` | Glue Block | off-theme | no | none | Sticky |
| `TConstruct:MeatBlock:0` | Hambone | off-theme | no | none | |
| `TConstruct:decoration.stonetorch:0-5` | Stone Torch | light | yes: torch alternative with a stone stick (`T`) | urban, ruined | Light 14; metadata is attachment |
| `TConstruct:decoration.stoneladder:0-5` | Stone Ladder | special | yes: dark ladder (`l`) | urban, industrial | Metadata is facing |
| `TConstruct:trap.punji:0` | Punji Sticks | special | no | none | Damages entities |
| `TConstruct:trap.barricade.oak` | Oak Barricade | door-trap, rubble-ruin | yes: boarded-up openings | ruined | Hardness 4; metadata is board count and facing |
| `TConstruct:trap.barricade.spruce` | Spruce Barricade | door-trap, rubble-ruin | yes | ruined | |
| `TConstruct:trap.barricade.birch` | Birch Barricade | door-trap, rubble-ruin | yes | ruined | |
| `TConstruct:trap.barricade.jungle` | Jungle Barricade | door-trap, rubble-ruin | yes | ruined | |
| `TConstruct:WoolSlab1:0-7`, `WoolSlab2:0-7` (+8 top) | Wool Slabs (16 colors) | stairs-slab, floor | partly: carpet-like floors and awnings | urban | 16 entries; flammable |

## Smeltery fittings and workstations

All tile entities. Not rendered on a sheet (model blocks).

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `TConstruct:LavaTank:0` | Seared Tank | industrial-dressing | partly: dark tank with gauge | industrial | TE (holds fluid) |
| `TConstruct:LavaTank:1` | Seared Glass | industrial-dressing, glass | partly: framed dark glass | industrial | TE |
| `TConstruct:LavaTank:2` | Seared Window | industrial-dressing, glass | partly | industrial | TE |
| `TConstruct:LavaTankNether:0-2` | Scorched Tank, Glass, Window | industrial-dressing | partly | industrial | 3 entries; TE |
| `TConstruct:SearedBlock:0-2` | Casting Table, Faucet, Casting Basin | industrial-dressing | partly | industrial | 3 entries; TE |
| `TConstruct:SearedBlockNether:0-2` | Scorched Casting Table, Faucet, Basin | industrial-dressing | partly | industrial | 3 entries; TE |
| `TConstruct:CraftingStation:0` | Crafting Station | furniture-deco | partly: workshop table | urban | TE |
| `TConstruct:ToolStationBlock:0-6, 10-13` | Tool Station, Part Builders, Pattern Chest, Stencil Tables | furniture-deco | partly: workshop furniture | urban, industrial | 11 entries; TE |
| `TConstruct:CraftingSlab:0-6` | Crafting slabs | furniture-deco | partly | urban | 7 entries; TE |
| `TConstruct:FurnaceSlab:0` | Slab Furnace | furniture-deco | partly | urban | TE |
| `TConstruct:Armor.DryingRack:0` | Drying Rack | furniture-deco | partly: wall shelf | urban | TE |
| `TConstruct:ToolForgeBlock:0-13` | Tool Forges (one per metal block top) | industrial-dressing | no: metal-block topped benches | industrial | 14 entries; TE |

## Slime-island foliage and gadgets

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `TConstruct:slime.grass:0` | Slime Grass | foliage | no | none | |
| `TConstruct:slime.leaves:0` | Slime Leaves | foliage | no | none | |
| `TConstruct:slime.sapling:0` | Slimy Sapling | foliage | no | none | |
| `TConstruct:slime.grass.tall:0` | Slimy Tall Grass | foliage | no | none | |
| `TConstruct:slime.pad:0`, `slime.channel:0`, `blood.channel:0` | Bounce Pad, Slime Channel, Blood Channel | off-theme | no | none | 3 entries; move entities |

## Best variety pools

- **Main wall `#`, standard bricks:** Polished Stone Brick (`multibrick:3`) and Fancy Stone Brick (`multibrickfancy:14`) next to vanilla stone bricks.
- **Dark brick (blackstone and deepslate buildings):** Seared Slabs as plain blocks; the full seared blocks only where tile entities are acceptable.
- **Desert bricks (`bricks_desert`):** Brownstone, Brownstone Brick, Fancy and Chiseled Brownstone (speed blocks, which D28 rule 3 allows in walls and trims), Sandstone Brick and Fancy Sandstone Brick, Bone and Endstone Brick for pale variety.
- **Side streets, floors and sidewalks `S`/`u`:** Stone Road (`multibrickfancy:15`), which has no speed.
- **Highways and primary roads:** Rough Brownstone Road and Brownstone Road (`SpeedBlock:1`, `:4`), as weighted options (D28 rule 3). No road slabs: they speed walking too.
- **Glass `glass_*` and colored glass `Z`:** Clear Glass and its pane; stained clear glass in white, gray, light gray, light blue, blue, cyan and black.
- **Rubble `}`:** Cracked Seared Bricks and Seared Cobblestone (TE), seared slabs, Grout; barricades over doors and windows in ruins.
- **Light `h`/`T`:** Stone Torch.
- **Ladder `l`:** Stone Ladder.
- **Industrial style:** seared tanks and casting tables as dead-factory set dressing; Steel and Aluminum blocks and Iron Brick as capped metal accents behind their D29 flags ([D29](index.md#decorative-metals-d29)).
