# Natura

Family D. Mod ID `Natura`, jar `Natura-2.8.24.jar`. Source read: `mods/Natura/mods/natura/common/NContent.java` (every registration, lines 147-711, and `woodTextureNames` at line 2017), `mods/Natura/mods/natura/blocks/**` (metadata, collision effects, light, flammability), `mods/Natura/assets/natura/lang/en_US.lang` (names), `config/Natura.cfg` (feature toggles). Sheets: `tools/block-contact-sheet/sheets/natura/` (previews in `docs/previews/blocks/natura/`, local only).

## Summary

- **117 registry names, about 245 block+meta entries** (orientation collapsed), of which about **185 are useful**: 13 woods, each with planks, stairs, slabs, fence, fence gate, trapdoor, pressure plate, button, bookshelf and workbench, plus 7 doors, 13 log types, leaves, and three grass blocks with matching grass slabs.
- Registry names are the literal strings passed to `GameRegistry.registerBlock`, prefixed `Natura:`. Several contain spaces or dots: `Natura:Rare Tree`, `Natura:Dark Tree`, `Natura:Rare Leaves`, `Natura:Dark Leaves`, `Natura:Rare Sapling`, `Natura:N Crops`, `Natura:Natura.fence`, `Natura:Natura.bookshelf`, `Natura:Natura.workbench`, `Natura:stair.maple`, `Natura:fenceGate.maple`, `Natura:door.redwood`. The asset block-name map must keep them verbatim.
- **The pack enables every Natura block.** In `config/Natura.cfg`, every "wood products" toggle (bookshelves, buttons, doors, fence gates, fences, pressure plates, slabs, stairs, trapdoors, workbenches) and every "nether products" block toggle (nether glass, furnaces, obelisks, blaze rails and hoppers, netherrack buttons, levers, pressure plates, berry bushes) is `true`.
- **Wood index** (planks meta, also bookshelf, workbench and fence meta): 0 eucalyptus, 1 sakura, 2 ghostwood, 3 redwood, 4 bloodwood, 5 hopseed, 6 maple, 7 silverbell, 8 amaranth (`purpleheart` in texture names), 9 tigerwood, 10 willow, 11 darkwood, 12 fusewood. Planks 2, 4, 11 and 12 (the Nether woods) do not burn (`Planks.getPlankFlammability`).
- **Look:** Natura's woods are cleaner and less fantastical than BoP's. Eucalyptus is a soft pink, sakura a pale tan, ghostwood a white-gray birch-like plank, redwood and hopseed salmon-brown, bloodwood a deep red, maple golden, silverbell cream, amaranth purple-black, tigerwood a striped amber, willow olive, darkwood navy blue, fusewood teal. About seven read as ordinary stained or painted wood.
- **Doors matter most.** Et Futurum's wood doors are off in the pack (`block-mapping.md`), so upstream's random building doors all collapse to `minecraft:wooden_door`. Natura's seven doors (redwood, eucalyptus, hopseed, sakura, ghostwood, bloodwood, redwood bark) are plain vanilla-style `BlockDoor`s with no tile entity: a direct fix for door variety.
- **Tile entities:** only the Nether Furnace and the Blaze Hopper. Everything else is metadata only.
- **Light:** Thornvines 9, Glowshroom (small) 9, Blue Glowshroom block 9, Green and Purple Glowshroom blocks 7, Nether Furnace when lit.
- **Gravity:** Heat Sand falls (`BlockFalling`).
- **Hazards:** **Soul Glass (`NetherGlass:0`) has no collision box at all** (`getCollisionBoundingBoxFromPool` returns null) and gives Slowness: a window made of it is a hole players fall through. Heat Glass (`NetherGlass:1`) and Heat Sand burn on contact. **Fusewood logs (`Dark Tree:1`) explode (power 1.75-2) when harvested.** Thornvines and Saguaro hurt on contact. Large glowshroom blocks are walk-through (sink, slow) and give mobs Nausea. Clouds have a 1/16-block collision only, so they cannot be stood in as blocks; sulfur clouds explode when hit by a burning arrow.
- **Ore and boulder hosts:** none (no `isReplaceableOreGen` override).
- **Optional-dependency handling:** each Natura entry needs a vanilla fallback (D19): planks to the nearest `minecraft:planks` tone, doors to `minecraft:wooden_door`, trapdoors to `minecraft:trapdoor`, fences to `minecraft:fence`, gates to `minecraft:fence_gate`, bookshelves to `minecraft:bookshelf`, workbenches to `minecraft:crafting_table`, topiary grass to `minecraft:grass`. All swaps are plain block swaps because nothing used needs NBT.

## Blocks

### Planks, stairs, slabs, fences, gates, plates, buttons (sheet `woods`)

Per wood: planks `Natura:planks:<i>`; stairs `Natura:stair.<wood>` (vanilla stair metas); slabs `Natura:plankSlab1:0-7` for woods 0-7 and `Natura:plankSlab2:0-4` for woods 8-12 (+8 top; doubles `plankSlab1Double`, `plankSlab2Double`); fence `Natura:Natura.fence:<i>` (connects like a vanilla fence); fence gate `Natura:fenceGate.<wood>` (vanilla gate metas); pressure plate `Natura:pressureplate.<wood>`; button `Natura:button.<wood>`. In stair, gate, plate, button and trapdoor names amaranth is `amaranth` and hopseed is `hopseed`; in item names it is `purpleheart` and `bush`.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Natura:planks:0` (+ `stair.eucalyptus`, `plankSlab1:0`, fence 0, `fenceGate.eucalyptus`) | Eucalyptus Planks | floor, wall-accent, stairs-slab, fence-rail | partly | interior | Soft pink; a painted or pickled floor. Fallback `minecraft:planks:2` |
| `Natura:planks:1` (+ `stair.sakura`, `plankSlab1:1`, fence 1, gate) | Sakura Planks | floor, wall-main, stairs-slab, fence-rail | yes | interior, rustic | Pale tan; a light oak or ash. Fallback `minecraft:planks:2` |
| `Natura:planks:2` (+ `stair.ghostwood`, `plankSlab1:2`, fence 2, gate) | Ghostwood Planks | floor, wall-main, stairs-slab, fence-rail | yes | modern, interior | White-gray, birch-like; painted siding or a gray-washed floor. Does not burn. Fallback `minecraft:planks:2` |
| `Natura:planks:3` (+ `stair.redwood`, `plankSlab1:3`, fence 3, gate) | Redwood Planks | floor, wall-main, stairs-slab, fence-rail | yes | rustic, interior | Salmon-brown. Fallback `minecraft:planks:4` |
| `Natura:planks:4` (+ `stair.bloodwood`, `plankSlab1:4`, fence 4, gate) | Bloodwood Planks | floor, wall-accent | partly | interior | Deep red; a lacquered accent. Does not burn. Fallback `minecraft:planks:4` |
| `Natura:planks:5` (+ `stair.hopseed`, `plankSlab1:5`, fence 5, gate) | Hopseed Planks | floor, stairs-slab, fence-rail | yes | rustic | Light salmon. Fallback `minecraft:planks:3` |
| `Natura:planks:6` (+ `stair.maple`, `plankSlab1:6`, fence 6, gate) | Maple Planks | floor, stairs-slab | yes | interior | Golden honey tone; a classic gym or hall floor. Fallback `minecraft:planks:0` |
| `Natura:planks:7` (+ `stair.silverbell`, `plankSlab1:7`, fence 7, gate) | Silverbell Planks | floor, wall-main, stairs-slab | yes | interior, modern | Cream; a pale modern floor. Fallback `minecraft:planks:2` |
| `Natura:planks:8` (+ `stair.amaranth`, `plankSlab2:0`, fence 8, gate) | Amaranth Planks | wall-accent | partly | interior | Purple-black; an ebony accent at most |
| `Natura:planks:9` (+ `stair.tiger`, `plankSlab2:1`, fence 9, gate) | Tigerwood Planks | floor, furniture-deco | partly | interior | Striped amber; a showy hotel or bar floor |
| `Natura:planks:10` (+ `stair.willow`, `plankSlab2:2`, fence 10, gate) | Willow Planks | off-theme | no | fantasy | Olive green |
| `Natura:planks:11` (+ `stair.darkwood`, `plankSlab2:3`, fence 11, gate) | Darkwood Planks | wall-accent | partly | modern | Navy blue; reads as painted wood. Does not burn |
| `Natura:planks:12` (+ `stair.fusewood`, `plankSlab2:4`, fence 12, gate) | Fusewood Planks | off-theme | no | fantasy | Teal. Does not burn; the planks are safe, only the log explodes |
| `Natura:pressureplate.<wood>:0-1` (13 blocks) | Wood Pressure Plates | furniture-deco | yes | interior | Variety for upstream's `-` oak plate. Fallback `minecraft:wooden_pressure_plate` |
| `Natura:button.<wood>:0-15` (13 blocks) | Wood Buttons | furniture-deco | partly | interior | Facing metas as vanilla |
| `Natura:NetherPressurePlate`, `Natura:NetherButton`, `Natura:NetherLever` | Netherrack plate, button, lever | furniture-deco | partly | industrial | Red netherrack texture |

### Logs (sheet `woods`)

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Natura:tree:0-3` (+4/+8 axis, +12 bark) | Eucalyptus, Sakura, Ghostwood, Hopseed Wood | trim-pillar | partly | rustic, park-garden | `BlockLog` metas. Ghostwood bark is white-gray and blotched |
| `Natura:Rare Tree:0-3` (+4/+8, +12) | Maple, Silverbell, Amaranth, Tiger Wood | trim-pillar | partly | rustic | |
| `Natura:Dark Tree:0` (+4/+8) | Darkwood | trim-pillar | partly | | Navy bark |
| `Natura:Dark Tree:1` (+4/+8) | Fusewood | off-theme | no | | **Explodes when harvested** (`DarkTreeBlock.onBlockHarvested`): never place |
| `Natura:willow:0` (+4/+8) | Willow Wood | trim-pillar | partly | park-garden | |
| `Natura:redwood:0`, `:1`, `:2` | Redwood Bark, Redwood (heart), Redwood Root | trim-pillar, wall-main | yes | rustic | No axis; bark on all sides for `:0`. A plain brown bark wall for log cabins |
| `Natura:bloodwood:0-7`, `:15` | Bloodwood | off-theme | no | | 2x2 giant-log quadrants; `:15` is all bark. Hardness 8 |

### Bookshelves, workbenches, doors, trapdoors (sheets `woods`, `workbench-doors-trapdoors`)

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Natura:Natura.bookshelf:0-12` | Bookshelves in 13 woods | furniture-deco | yes | interior | Vanilla bookshelf subclass, no TE, enchanting power as vanilla. Same books, different frame wood: direct variety for upstream's `L`. Fallback `minecraft:bookshelf` |
| `Natura:Natura.workbench:0-12` | Workbenches in 13 woods | furniture-deco | yes | interior | `BlockWorkbench` subclass, no TE. Variety for upstream's `F` furniture mix. Fallback `minecraft:crafting_table` |
| `Natura:door.redwood` | Redwood Door | door-trap | yes | rustic, urban-brick | Vanilla door metas (lower 0-7, upper 8-11); a solid plank door with a small window. Fallback `minecraft:wooden_door` |
| `Natura:door.eucalyptus` | Eucalyptus Door | door-trap | partly | interior | Pink, with a heart-shaped window |
| `Natura:door.hopseed` | Hopseed Door | door-trap | yes | rustic | Salmon, slatted |
| `Natura:door.sakura` | Sakura Door | door-trap | yes | interior | Tan, four-pane window |
| `Natura:door.ghostwood` | Ghostwood Door | door-trap | yes | modern | White-gray, four-pane window; an office or apartment door |
| `Natura:door.bloodwood` | Bloodwood Door | door-trap | partly | | Deep red, solid |
| `Natura:door.redwoodbark` | Redwood Bark Door | door-trap | partly | rustic | Bark-faced |
| `Natura:trapdoor.<wood>` (13 blocks, vanilla trapdoor metas) | Trapdoors in 13 woods | door-trap, furniture-deco | yes | interior | Hole patterns differ per wood (four holes, slats, a spiral for silverbell). Useful as shutters and hatches. Fallback `minecraft:trapdoor` |

### Leaves, saplings, grass, glass and the rest (sheet `nature-misc`)

Leaves are `BlockLeaves` subclasses: meta `% 4` is the type, **+4 = never decays**, +8 = decay check. `floraleaves` and `Rare Leaves` are biome-tinted (gray in the jar).

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Natura:floraleaves:0-2` (+4) | Redwood, Eucalyptus, Hopseed Leaves | foliage | yes | park-garden | Tinted green |
| `Natura:floraleavesnocolor:0` (+4) | Sakura Leaves | foliage | yes | park-garden | Pink blossom; street trees |
| `Natura:floraleavesnocolor:1` (+4) | Ghostwood Leaves | foliage | partly | ruin | White; ghostly dead-tree look |
| `Natura:floraleavesnocolor:2` (+4) | Bloodleaves | off-theme | no | | Red; Nether |
| `Natura:floraleavesnocolor:3` (+4) | Willow Leaves | foliage | yes | park-garden | Dark green |
| `Natura:Rare Leaves:0-3` (+4) | Maple, Silverbell, Amaranth, Tiger Leaves | foliage | yes | park-garden | Tinted |
| `Natura:Dark Leaves:0-3` (+4) | Darkwood, flowering, fruiting Darkwood, Fusewood Leaves | off-theme | no | | Nether colors |
| `Natura:florasapling:0-7`, `Natura:Rare Sapling:0-4` | 13 saplings | foliage | partly | park-garden | Grow into trees on random ticks |
| `Natura:GrassBlock:0` | Topiary Grass | foliage, special | yes | park-garden | **Grass texture on all six faces, no random tick, and mobs cannot spawn on it** (`GrassBlock.canCreatureSpawn`). A hedge or clipped lawn block that never turns to dirt. Fallback `minecraft:grass` or `minecraft:leaves:4` for hedges |
| `Natura:GrassBlock:1`, `:2` | Bluegrass, Autumnal Grass | foliage | partly | park-garden | Blue-tinted and orange-tinted versions |
| `Natura:GrassSlab:0-2` (+8 top), `Natura:GrassSlabDouble:0-2` | Topiary, Bluegrass, Autumnal Grass Slab | foliage, stairs-slab | yes | park-garden | Half-height lawn: terraced park edges and low hedges. Vanilla has no grass slab: fall back to `minecraft:grass` (a full block) or leave air |
| `Natura:Bluebells:0` | Bluebells | foliage | yes | park-garden | Flower |
| `Natura:Glowshroom:0-2` | Green, Purple, Blue Glowshroom | light | partly | ruin | Light 9; small mushrooms that glow in tunnels |
| `Natura:greenGlowshroom`, `purpleGlowshroom`, `blueGlowshroom` | Glowshroom blocks | off-theme | no | | Walk-through, Nausea to mobs |
| `Natura:Thornvines:0-15` | Thornvines | off-theme | no | | Light 9, hurts |
| `Natura:Saguaro:0+` | Saguaro Cactus | foliage | partly | desert | Hurts on contact; desert squares at most |
| `Natura:BerryBush:0-15` | Raspberry, Blueberry, Blackberry, Maloberry Bush | foliage | partly | park-garden, overgrown | Meta `% 4` type, stage in the high bits, 12-15 ripe; leafy cube bushes that grow on random ticks |
| `Natura:NetherBerryBush:0-15` | Blight-, Dusk-, Sky-, Stingberry Bush | off-theme | no | | Nether |
| `Natura:N Crops:0-8` | Barley, Cotton | foliage | partly | park-garden | Crops on farmland |
| `Natura:NetherGlass:0` | Soul Glass | off-theme | no | | **No collision**, Slowness. Gray, translucent. Never as a window |
| `Natura:NetherGlass:1` | Heat Glass | off-theme | no | | Orange-red; burns on contact |
| `Natura:Cloud:0-3` | Cloud, Dark, Ash, Sulfur Cloud | off-theme | no | | 1/16-block collision; sulfur explodes with burning arrows |
| `Natura:heatsand:0` | Heat Sand | off-theme | no | | Falls; burns |
| `Natura:soil.tainted:0` | Tainted Soil | off-theme | no | | Nether soil |
| `Natura:NetherFurnace:2-5` | Nether Furnace | furniture-deco | partly | industrial | TE (`NetherrackFurnaceLogic`); a red netherrack furnace. Facing in meta, contents in NBT |
| `Natura:Obelisk:0` | Obelisk | off-theme | no | | Respawn point block |
| `Natura:NetherHopper:0-5` | Blaze Hopper | off-theme | no | | TE |
| `Natura:Blazerail`, `BrailPowered`, `BrailDetector`, `BrailActivator` | Blaze rails | off-theme | partly | industrial | Rails that do not burn; upstream railways use vanilla rails |

## Best variety pools

- **door-trap:** `door.ghostwood`, `door.sakura`, `door.redwood`, `door.hopseed` for buildings; `door.redwoodbark` for rustic ones. The only metadata-only wood doors in the pack besides vanilla oak. Trapdoors in 13 woods for shutters and hatches.
- **floor (interior wood):** planks 1 (sakura), 2 (ghostwood), 3 (redwood), 5 (hopseed), 6 (maple), 7 (silverbell), with matching stairs and slabs; 9 (tigerwood) for a showy room.
- **wall-main (wood siding):** ghostwood and silverbell (pale painted siding), redwood and sakura (natural); `redwood:0` bark for log cabins.
- **furniture-deco:** 13 bookshelves (upstream's `L`), 13 workbenches (upstream's `F`), 13 pressure plates (upstream's `-`).
- **fence-rail:** `Natura.fence:0-12` and `fenceGate.<wood>` replace upstream's `.` oak fence with 13 tones; ghostwood, sakura, redwood and maple fences read best.
- **foliage (parks):** `GrassBlock:0` topiary grass for hedges and lawns, `GrassSlab:0` for terraces, `floraleavesnocolor:0` sakura and `Rare Leaves:0` maple trees, `Bluebells`, berry bushes.
- **light:** small glowshrooms (`Glowshroom:0-2`, light 9) in dark ruins and tunnels.

## Worldgen notes

- **No NBT needed** for anything recommended. Doors are two blocks with vanilla door metas; place both halves in one pass. Fences and gates connect from neighbors.
- **Leaves:** set bit 4 (meta + 4) or they decay.
- **Never place:** `Dark Tree:1` fusewood logs (explode when broken), `NetherGlass:0` soul glass (no collision: players fall through), `NetherGlass:1` heat glass and `heatsand` (burn), thornvines, clouds, glowshroom blocks.
- **Topiary grass and mob spawning:** a city with topiary-grass parks spawns no mobs on those blocks. That suits manicured parks in an inhabited-looking district but removes some danger from a ruined one; palettes should choose deliberately.
- **Saplings, berry bushes and crops** grow on random ticks and change shape over time.
- **Fire:** planks 0, 1, 3, 5-10 and their products burn like vanilla wood; ghostwood, bloodwood, darkwood and fusewood planks do not.
- **Ore and boulder hosts:** none.
- **Without Natura:** fall back per the summary list; the doors fall back to `minecraft:wooden_door`, exactly the D19 baseline.
