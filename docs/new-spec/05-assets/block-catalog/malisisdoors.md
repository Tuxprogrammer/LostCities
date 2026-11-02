# Malisis' Doors

Family D. Mod ID `malisisdoors`, jar `malisisdoors-1.19.13-GTNH.jar`. Source read: `mods/malisisdoors/net/malisis/doors/Registers.java` (L60-L488), `door/DoorDescriptor.java` (L264-L271), `door/descriptor/*.java`, `door/block/{Door,FenceGate,BigDoor,RustyHatch}.java`, `trapdoor/descriptor/*.java`, `block/*.java`; pack config `config/malisisdoors.cfg`. Sheets: `tools/block-contact-sheet/sheets/malisisdoors/doors.json` (previews in `docs/previews/blocks/malisisdoors/`, local only).

## Summary

- **About 56 registry names**, all enabled (`config/malisisdoors.cfg`: mixed blocks, vanishing blocks and camo gate on). Names come from `getUnlocalizedName().substring(5)`, so they are clean (`malisisdoors:jail_door`).
- **It replaces the vanilla wooden door, iron door, trapdoor and fence gate** (`modifyVanillaDoors=true`, `Registers.java` L62-L66, `ReplacementTool.replaceVanillaBlock`). `minecraft:wooden_door`, `minecraft:iron_door`, `minecraft:trapdoor` and `minecraft:fence_gate` keep their names and metadata layout but become Malisis blocks with a tile entity (the lower door half creates `DoorTileEntity`; `Door.java` L372-L374). Upstream's doors therefore already generate as Malisis doors; the writer must let `setBlock` create the TE and not assume they are TE-free.
- **It supplies what D19 found missing**: wood-variant doors (`door_acacia`, `door_birch`, `door_dark_oak`, `door_jungle`, `door_spruce`), wood-variant trapdoors, and an **iron trapdoor** (`malisisdoors:iron_trapdoor`), none of which Et Futurum provides under the pack's config. The block mapping can use these instead of collapsing every door to oak.
- **Urban doors**: glass sliding doors (wood and iron frames; storefronts, offices), a jail door (bars), a laboratory door, a factory door, a garage door (multi-block roll-up), a saloon door, a carriage door and a medieval door (3x3 "big doors"), a rusty hatch with a rusty ladder (manholes, basements), and **16 colored curtains** (two-high cloth "doors" that hang in window openings).
- **All door-like blocks are TEs** whose state (open, animation) is in the TE; facing, half and hinge follow the vanilla door metadata layout (`Door.java` L68-L77). A default TE renders correctly, so metadata alone gives the right look.
- **Light**: none (forcefield excepted). **Gravity**: none. **Interaction**: doors open with animation and sound; the player sensor opens doors automatically; the door factory and block mixer are machines; vanishing blocks turn invisible on redstone.
- **Fallback when the mod is absent**: every door to `minecraft:wooden_door` or `minecraft:iron_door`, trapdoors to `minecraft:trapdoor`, curtains to air or `minecraft:carpet` on the sill, fence gates to `minecraft:fence_gate`, garage and big doors to air.

## Blocks

### Doors (two blocks high; vanilla door metas)

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `malisisdoors:door_acacia`, `door_birch`, `door_dark_oak`, `door_jungle`, `door_spruce` | 1.8-style wood doors | door-trap | yes | urban-brick, rustic, interior | the wood doors missing from the D19 mapping |
| `malisisdoors:wood_sliding_door`, `iron_sliding_door` | Wooden / Iron Glass Sliding Door | door-trap, glass | yes | modern | glass panels in a frame; storefronts and office entrances |
| `malisisdoors:jail_door` | Jail Door | door-trap | yes | urban-stone, industrial | police stations, cells, cages |
| `malisisdoors:laboratory_door` | Laboratory Door | door-trap | yes | modern, industrial | white metal door with a window strip; hospitals, labs |
| `malisisdoors:factory_door` | Factory Door | door-trap | yes | industrial | heavy dark metal door |
| `malisisdoors:shoji_door` | Shoji Door | door-trap | partly | interior | paper screen; Japanese restaurant interiors only |
| `malisisdoors:saloon` | Saloon Door | door-trap | partly | rustic | bars |
| `malisisdoors:curtain_{black,red,green,brown,blue,purple,cyan,silver,gray,pink,lime,yellow,light_blue,magenta,orange,white}` (16) | Curtains | furniture-deco, pane | yes | interior | two-high cloth panels in window openings; the cheapest way to make windows look lived in |
| `malisisdoors:customDoor` | Custom Door | door-trap | no | | material from NBT; skip (name unverified) |
| `minecraft:wooden_door`, `minecraft:iron_door` (replaced) | Oak Door, Iron Door | door-trap | yes | any | upstream's doors; now TE-backed |

### Big and special doors

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `malisisdoors:garage_door` | Garage Door | door-trap | yes | modern, industrial | stacks vertically into a roll-up door; each block is a TE |
| `malisisdoors:carriage_door` | Carriage Door | door-trap | partly | urban-brick | 3x3 double door with stone frame; old warehouses, stations. Builds its frame of `carriage_door_collisionHelper` blocks in `onBlockPlacedBy`, only when a player places it |
| `malisisdoors:medieval_door` | Medieval Door | door-trap | no | fantasy | |
| `malisisdoors:rustyHatch` | Rusty Hatch | door-trap | yes | industrial, ruin | manholes, basement hatches; forms its 2x3 multiblock in `onBlockPlacedBy` (`RustyHatch.java` L87-L96), only when a player places it, so worldgen cannot place it |
| `malisisdoors:rustyLadder` | Rusty Ladder | special | yes | industrial, ruin | plain `BlockLadder` subclass, no TE; manholes, service shafts |
| `malisisdoors:forcefieldDoor` | Forcefield | off-theme | no | | sci-fi |

### Trapdoors and fence gates

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `malisisdoors:iron_trapdoor` | Iron Trapdoor | door-trap | yes | industrial, modern | the iron trapdoor D19 had to replace with a wooden one |
| `malisisdoors:sliding_trapdoor` | Sliding Trapdoor | door-trap | partly | industrial | gold-and-iron hatch |
| `malisisdoors:trapdoor_{acacia,birch,dark_oak,jungle,spruce}` | Wood Trapdoors | door-trap | yes | rustic, interior | |
| `malisisdoors:{acacia,birch,darkOak,jungle,spruce}FenceGate` | Wood Fence Gates | fence-rail | yes | park-garden, rustic | |
| `malisisdoors:camoFenceGate` | Camo Fence Gate | fence-rail | no | | takes neighbor textures |
| `minecraft:trapdoor`, `minecraft:fence_gate` (replaced) | Trapdoor, Oak Fence Gate | door-trap, fence-rail | yes | any | now TE-backed |

### Other blocks

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `malisisdoors:player_sensor` | Player Sensor | special | partly | modern | automatic doors for malls and offices; a thin panel |
| `malisisdoors:vanishing_block:0-3`, `vanishing_block_diamond` | Vanishing Frames | off-theme | no | | redstone-controlled invisible blocks |
| `malisisdoors:block_mixer`, `mixed_block` | Block Mixer, Mixed Block | off-theme | no | | mixed block stores two block states in NBT |
| `malisisdoors:door_factory` | Door Factory | off-theme | no | | machine |
| `malisisdoors:medieval_door_collisionHelper`, `carriage_door_collisionHelper` | Collision helpers | special | no | | internal parts of big doors |

## Best variety pools

- **door-trap, storefronts and offices**: iron and wood glass sliding doors, laboratory door; oak and dark oak doors for older buildings.
- **door-trap, homes**: the five wood doors weighted by building style (dark oak and spruce for brick, birch for modern).
- **door-trap, industrial**: factory door, garage door, iron trapdoor; the rusty ladder alone for manholes and service shafts.
- **door-trap, police and prisons**: jail door.
- **furniture-deco, windows**: curtains in white, silver, gray, red, brown, blue, green; occasional bright colors.

## Worldgen notes

- Doors are cheap: one non-ticking TE per lower half (animation ticks only while moving). Curtains count as doors.
- Use vanilla door metadata. Write the lower half then the upper half; the lower half creates the TE.
- The carriage door, the medieval door and the rusty hatch build their multiblock in `onBlockPlacedBy`, which runs only when a player places the block ([integration-opportunities.md](../../00-pack-integration/integration-opportunities.md#doors-malisisdoors)). `setBlock` leaves them incomplete, so worldgen skips all three, as the [index](index.md#doors) does.
- Garage doors are one block per row and open as a stack; generate the full column (how a partial column behaves is unverified).
- A server without the mod: vanilla door names still resolve (they are vanilla names); the `malisisdoors:*` names need the fallbacks above.
