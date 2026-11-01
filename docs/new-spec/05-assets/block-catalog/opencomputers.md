# OpenComputers and its add-ons (Computronics, OpenSecurity, OpenPrinter, OpenGlasses)

Family D. Mod IDs `OpenComputers`, `computronics`, `opensecurity`, `openprinter`, `openglasses`; jars `OpenComputers-1.12.68-GTNH.jar`, `Computronics-1.9.13-GTNH.jar`, `opensecurity-1.2.2-GTNH.jar`, `openprinter-0.3.1-GTNH.jar`, `OpenGlasses-1.6.9-GTNH.jar`. Source read: `mods/OpenComputers/li/cil/oc/common/init/Blocks.scala`, `.../init/Items.scala` (lines 51-63), `.../common/block/ChameliumBlock.scala`, `.../common/block/Print.scala`, `li/cil/oc/Constants.scala`, `li/cil/oc/util/Color.scala`; `mods/Computronics/pl/asie/computronics/Computronics.java` (lines 185-330), `.../block/BlockColorfulLamp.java`, `.../tile/TileColorfulLamp.java`, `.../integration/railcraft/IntegrationRailcraft.java`; `mods/opensecurity/pcl/opensecurity/ContentRegistry.java` (lines 160-240), `.../blocks/BlockSecurityDoor.java`; `mods/openprinter/pcl/openprinter/ContentRegistry.java` (lines 78-87); `mods/OpenGlasses/com/bymarcin/openglasses/OpenGlasses.java` line 112; `config/computronics.cfg`. Sheets: `tools/block-contact-sheet/sheets/opencomputers/` (previews in `docs/previews/blocks/opencomputers/`, local only).

## Summary

- **Counts.**
  - OpenComputers: **38 registry names** (`OpenComputers:<name>`; `Items.registerBlock` uses the bare name, `Items.scala` line 55). 53 block+meta entries (chamelium's 16 colors; the rest are single blocks with TE-held facing). **Useful: 9** (cases, screens, keyboard, rack, raid, disk drive, switch/relay, chamelium, power distributor as wall boxes).
  - Computronics: **16 registry names** with every module on in `config/computronics.cfg` (`enable` category lines 28-41), including 5 Railcraft-integration blocks (Railcraft is in the pack). **Useful: 3** (colorful lamp, tape drive, speaker).
  - OpenSecurity: **14 registry names** (the display panel is commented out). **Useful: 2** (alarm, mag card reader as wall dressing). The security door is a trap (below).
  - OpenPrinter: **3 registry names**, all **useful** office dressing (file cabinet, printer, shredder).
  - OpenGlasses: **1 registry name** (glasses terminal), not useful.
- **Look.** Dark gray, black-trimmed electronics with small LED details: they read as 1980s to 2000s computers, servers and network boxes. The best "lived-in office" and "server room" dressing in the pack. Chamelium is a plain matte block in 16 dye colors.
- **Tile entities everywhere.** Every OC machine is a TE that holds its facing, tier color and inventory in NBT. A default TE (no NBT) faces a default direction and is empty and unpowered, which is the right look for an abandoned city: dark screens, idle cases. Screens are multiblocks that merge with neighbors of the same tier and facing; placed side by side they form one large dark screen. Powered OC machines tick; unpowered ones do almost nothing.
- **Light.** Computronics' colorful lamp emits light 15 (any non-zero color) and its color comes from TE NBT `clc` (15-bit RGB, default `0x6318`, a warm white). **Quirk:** `BlockColorfulLamp.setLightValue` writes the shared `Block.lightValue`, so all lamps in the world share the light level of the last one updated (`BlockColorfulLamp.java` lines 83-95). With colored light off (the default without the Colored Lights mod), every lamp with a non-zero color is 15, so the quirk is harmless as long as no lamp is set to color 0. OC screens emit a little light only with Colored Lights installed.
- **Interaction effects.**
  - **OpenSecurity's security door cannot be opened by hand** (`onBlockActivated` returns false) and has hardness 400 and blast resistance 6000 (`BlockSecurityDoor.java` lines 40-41, 94-97). Placing one seals a room. Do not use it except to seal a deliberate vault, and never on an exit.
  - OpenSecurity's energy turret (`energyTurretBlock`) fires when driven by a computer; idle in worldgen, but off-theme.
  - Computronics' chat box, radar, camera and speech box do nothing without a computer.
- **Optional dependency.** A server without these mods loses them. Fallbacks: no vanilla block looks like a computer, so electronics dressing falls back to air (or `minecraft:bookshelf` in offices). Chamelium falls back to `minecraft:wool:<color>` or `etfuturum:concrete:<color>`. The colorful lamp falls back to `minecraft:glowstone` or `minecraft:redstone_lamp` (lit).

## Blocks

### OpenComputers

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `OpenComputers:case1:0`, `case2:0`, `case3:0` | Computer Case (Tier 1-3) | `furniture-deco` | yes | `interior`, `modern`, `industrial` | TE (`tileentity.Case`). Grayscale texture tinted by tier (light gray, gold, cyan). Facing in TE. A desktop tower; on a desk with a screen and keyboard it reads as an office workstation |
| `OpenComputers:caseCreative:0` | Creative Case | `off-theme` (creative-only) | no | | Never place |
| `OpenComputers:screen1:0`, `screen2:0`, `screen3:0` | Screen (Tier 1-3) | `furniture-deco`, `wall-accent` | yes | `interior`, `modern` | TE, multiblock. Black glass face with a gray bezel. Adjacent same-tier screens with the same facing merge into one big screen: a wall of them is a control room or a video wall |
| `OpenComputers:keyboard:0` | Keyboard | `furniture-deco` | yes | `interior` | TE. Thin plate model; sits on a desk in front of a screen or on a screen face |
| `OpenComputers:rack:0` | Rack | `furniture-deco` | yes | `industrial`, `modern` | TE. Server rack with four empty bays; servers inside need inventory NBT. Rows of empty racks make a server room |
| `OpenComputers:raid:0` | Raid | `furniture-deco` | yes | `modern` | TE. Disk-array front with three bays |
| `OpenComputers:diskDrive:0` | Disk Drive | `furniture-deco` | yes | `interior` | TE. Floppy-drive front |
| `OpenComputers:switch:0`, `relay:0`, `accessPoint:0` | Switch, Relay, Access Point | `furniture-deco` | yes | `industrial` | TEs. Network boxes with port lights; wall or shelf clutter in server rooms |
| `OpenComputers:powerDistributor:0`, `powerConverter:0`, `capacitor:0`, `carpetedCapacitor:0` | Power blocks | `furniture-deco` | partly | `industrial` | TEs. Electrical-cabinet look. The carpeted capacitor has a carpet top: an odd floor tile |
| `OpenComputers:chameliumBlock:0-15` | Block of Chamelium | `wall-main`, `wall-accent`, `floor` | yes | `modern`, `interior` | **No TE.** Plain white texture tinted by meta; meta order is OC's dye order, **not** wool order: 0 black, 1 red, 2 green, 3 brown, 4 blue, 5 purple, 6 cyan, 7 light gray, 8 gray, 9 pink, 10 lime, 11 yellow, 12 light blue, 13 magenta, 14 orange, 15 white (`Color.scala` lines 34-50). Flat, matte, plastic-like: good for modern facades and interiors |
| `OpenComputers:hologram1:0`, `hologram2:0` | Hologram Projector | `furniture-deco` | partly | `modern` | TE, slab-high pedestal |
| `OpenComputers:printer:0`, `assembler:0`, `disassembler:0`, `charger:0`, `adapter:0`, `geolyzer:0`, `motionSensor:0`, `redstone:0`, `transposer:0`, `netSplitter:0`, `waypoint:0`, `microcontroller:0` | Machines | `off-theme` (functional machines) | no | `industrial` | TEs. Usable only as background clutter; the microcontroller needs NBT |
| `OpenComputers:cable:0` | Cable | `special` | partly | `industrial` | TE. Thin cable; could run along a server-room floor |
| `OpenComputers:print:0` | 3D Print | `special` | no | | TE: shape and textures come entirely from NBT; useless without authored data, though a full designer could make any shape |
| `OpenComputers:endstone:0` | End Stone (fake) | `off-theme` | no | | Plain end stone texture, for OC's own use |
| `OpenComputers:robot:0`, `robotAfterimage:0` | Robot, robot proxy | `off-theme` | no | | Internal |

### Computronics

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `computronics:computronics.colorfulLamp:0` | Colorful Lamp | `light` | yes | `modern`, `interior`, `industrial` | TE, light 15 (see Summary). Frosted lamp block whose color comes from NBT `clc` (15-bit RGB); default warm white. Works as a ceiling or wall light of any color without power. The best colored light source in this slice |
| `computronics:computronics.tapeReader:0` | Tape Drive | `furniture-deco` | yes | `interior` | TE. Gray box with a tape slot: a 1980s tape deck |
| `computronics:computronics.speaker:0` | Speaker | `furniture-deco` | yes | `interior` | TE. Gray box with a speaker grille |
| `computronics:computronics.ironNoteBlock:0` | Iron Note Block | `furniture-deco` | partly | `industrial` | TE. Grille-faced metal box |
| `computronics:computronics.camera:0`, `chatBox:0`, `radar:0`, `cipher:0`, `cipher_advanced:0`, `audioCable:0`, `speechBox:0` | Peripherals | `furniture-deco` | partly | `industrial` | TEs. Gray machine boxes with small faces; generic electronics clutter |
| `computronics:computronics.ticketMachine:0`, `locomotiveRelay:0`, `digitalReceiverBox:0`, `digitalControllerBox:0`, `detector:0` | Railcraft integration | `special` | partly | `industrial` | TEs. The ticket machine could stand on a station platform; the signal boxes are trackside boxes |

### OpenSecurity

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `opensecurity:alarm:0` | Alarm | `furniture-deco` | yes | `industrial`, `modern` | TE. Siren box; silent unless triggered. Wall dressing in corridors |
| `opensecurity:magreader:0`, `rfidreader:0`, `keypadLock:0`, `biometricScanner:0` | Card readers, keypad, scanner | `furniture-deco` | partly | `modern` | TEs. Wall-mounted access hardware beside doors; inert without a computer |
| `opensecurity:SecurityDoor:0-11`, `SecurityDoorPrivate:0-11` | Security Door | `door-trap` | no | `modern` | **Cannot be opened by hand**, hardness 400, resistance 6000. Gray steel door look is good, behavior is not: never on an exit |
| `opensecurity:doorcontroller:0`, `entitydetector:0`, `rfidwriter:0`, `opensecurity.DataBlock:0`, `opensecurity.SwitchableHub:0`, `opensecurity.BlockKVM:0` | Machines | `off-theme` (functional machines) | no | `industrial` | TEs |
| `opensecurity:energyTurretBlock:0` | Energy Turret | `off-theme` (weapon) | no | | TE; fires when a computer drives it |

### OpenPrinter and OpenGlasses

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `openprinter:openprinter.filecabinet:0` | File Cabinet | `furniture-deco` | yes | `interior` | TE with an inventory (folders); facing in metadata or TE. Dark metal filing cabinet: office dressing, could hold loot |
| `openprinter:openprinter.printer:0` | Printer | `furniture-deco` | yes | `interior` | TE, model. Desktop printer |
| `openprinter:openprinter.shredder:0` | Shredder | `furniture-deco` | yes | `interior` | TE. Black box with a yellow slot |
| `openglasses:openglassesterminal:0` | Glasses Terminal | `off-theme` (machine) | no | | TE |

## Best variety pools

- **Office desk (furniture-deco):** `OpenComputers:case1`/`case2`/`case3`, `OpenComputers:screen1`, `OpenComputers:keyboard`, `computronics:computronics.tapeReader`, `computronics:computronics.speaker`, `openprinter:openprinter.printer`.
- **Office wall and corner:** `openprinter:openprinter.filecabinet`, `openprinter:openprinter.shredder`, `opensecurity:alarm`, `opensecurity:magreader`.
- **Server room:** `OpenComputers:rack`, `OpenComputers:raid`, `OpenComputers:switch`, `OpenComputers:relay`, `OpenComputers:powerDistributor`, `OpenComputers:cable`.
- **Control room or video wall:** a grid of `OpenComputers:screen2` or `screen3`.
- **Colored modern walls and floors:** `OpenComputers:chameliumBlock` 7 (light gray), 8 (gray), 15 (white), 0 (black), 12 (light blue), 6 (cyan), 3 (brown).
- **Lights of any color:** `computronics:computronics.colorfulLamp` with a palette of `clc` values.

## Worldgen notes

- All dressing above except chamelium needs its TE created at placement. Default TEs are correct for a dead office: empty and unpowered. Facing lives in TE NBT for OC (`oc:yaw`-style fields, *unverified* key names); without it every case and screen faces the default direction, so palettes that want a facing must write it.
- Screens merge with adjacent same-tier screens; a single screen in a palette is fine, rows merge on purpose.
- Never place OpenSecurity's security doors on a path players need, the creative case, robots, 3D prints without data, or the energy turret.
