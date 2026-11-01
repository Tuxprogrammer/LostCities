# Nuclear Control

Family D. Mod ID `IC2NuclearControl`, jar `IC2NuclearControl-2.7.17.jar`. Source read: `mods/IC2NuclearControl/shedar/mods/ic2/nuclearcontrol/IC2NuclearControl.java` (lines 158-159), `.../blocks/BlockNuclearControlLight.java`, `.../utils/LightDamages.java`, `.../utils/BlockDamages.java`, `.../crossmod/appeng/CrossAppeng.java`, `.../crossmod/bigreactors/CrossBigReactors.java`, `assets/nuclearcontrol/lang/en_US.lang`. Sheets: `tools/block-contact-sheet/sheets/nuclearcontrol/` (previews in `docs/previews/blocks/nuclearcontrol/`, local only).

## Summary

- **Counts.** 3 registry names in the pack: `blockNuclearControlMain` (metas 0-10), `blockNuclearControlLight` (metas 0-3) and the Applied Energistics `networkLink`. The Big Reactors block is not registered (Big Reactors is not in the pack). 16 block+meta entries. **Useful: 4** (the two lamps on and off) plus 3 as wall dressing (alarms, thermal monitor).
- **Look.** The lamps are full-cube industrial light panels: a white frosted panel and an orange one, each in an unlit and a lit state. They read as factory, hospital or parking-garage ceiling lights. The alarms are small boxy sirens; the panels are flat information screens.
- **Light and redstone.** Lamp metas 1 and 3 are lit (light 15); 0 and 2 are unlit (`BlockNuclearControlLight.getLightValue`, lines 43-46). The block behaves like a redstone lamp: `onBlockAdded` and `onNeighborBlockChange` switch a lit lamp off when it is not powered and an unlit lamp on when it is (lines 67-101). Worldgen that writes blocks straight into the chunk does not call `onBlockAdded`, so a lit lamp stays lit until a neighbor changes; the first block update next to it (a player breaking a block, a door opening) turns it off. Same trade-off as `minecraft:lit_redstone_lamp`. Pairing it with a hidden `minecraft:redstone_block` keeps it lit for good.
- **Tile entities.** Lamps: none. Every `blockNuclearControlMain` meta is a TE (monitors, alarms, panels, counters), most of them IC2 machines that tick and read IC2 reactors. The howler and industrial alarms sound only when powered by redstone.
- **Optional dependency.** A server without Nuclear Control loses these. Fallbacks: lit lamps to `minecraft:redstone_lamp` lit (`minecraft:lit_redstone_lamp`) or `minecraft:glowstone`, unlit lamps to `minecraft:redstone_lamp`.

## Blocks

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `IC2NuclearControl:blockNuclearControlLight:0` | White Lamp (off) | `ceiling`, `wall-accent` | yes | `industrial`, `modern` | No TE. Gray frosted panel; the dark state of the white lamp. Good for derelict buildings |
| `IC2NuclearControl:blockNuclearControlLight:1` | White Lamp (on) | `light` | yes | `industrial`, `modern`, `interior` | No TE, light 15. Bright white panel: ceiling lighting for offices, garages, factories. Turns off on a neighbor update unless powered |
| `IC2NuclearControl:blockNuclearControlLight:2` | Orange Lamp (off) | `wall-accent` | partly | `industrial` | No TE. Dark red-brown panel |
| `IC2NuclearControl:blockNuclearControlLight:3` | Orange Lamp (on) | `light` | yes | `industrial`, `urban-stone` | No TE, light 15. Sodium-vapor orange: street and tunnel lighting, warning lights |
| `IC2NuclearControl:blockNuclearControlMain:1` | Industrial Alarm | `furniture-deco` | yes | `industrial` | TE, model: a siren box with a red light. Silent unless powered |
| `IC2NuclearControl:blockNuclearControlMain:2` | Howler Alarm | `furniture-deco` | yes | `industrial` | TE, model: a horn speaker. Silent unless powered |
| `IC2NuclearControl:blockNuclearControlMain:0` | Thermal Monitor | `furniture-deco` | partly | `industrial` | TE, thin wall plate with a dial |
| `IC2NuclearControl:blockNuclearControlMain:3` | Remote Thermal Monitor | `off-theme` (machine) | no | `industrial` | TE |
| `IC2NuclearControl:blockNuclearControlMain:4`, `:5`, `:9`, `:10` | Information Panel, Extender, Advanced Panel, Advanced Extender | `furniture-deco` | partly | `modern`, `industrial` | TEs. Flat screens that merge into multi-block displays; blank without cards. A dark wall display |
| `IC2NuclearControl:blockNuclearControlMain:6`, `:7`, `:8` | Energy Counter, Average Counter, Range Trigger | `off-theme` (IC2 machines) | no | `industrial` | TEs |
| `IC2NuclearControl:networkLink:0` | ME Network Monitor | `off-theme` (AE2 machine) | no | | TE |

## Best variety pools

- **Ceiling lights, industrial and commercial:** `IC2NuclearControl:blockNuclearControlLight:1` (with `minecraft:glowstone`, Computronics' colorful lamp, and the pack's other lamps).
- **Street, tunnel and highway lighting:** `IC2NuclearControl:blockNuclearControlLight:3`.
- **Dead or damaged lights (ruins, ceilings with gaps):** metas 0 and 2 mixed among lit ones.
- **Factory and corridor wall dressing:** `blockNuclearControlMain:1` and `:2` (alarms).

## Worldgen notes

- Lamps are plain blocks and cheap; decide whether lit lamps should survive block updates (add a hidden redstone block) or may go dark when players start tinkering, which suits an abandoned city.
- The main block's metas are IC2 TEs; only the alarms and panels are worth placing, and they need their TE created.
