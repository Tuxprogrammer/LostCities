# OpenBlocks

Family D. Mod ID `OpenBlocks`, jar `OpenBlocks-1.12.22-GTNH.jar`. Source read: `mods/OpenBlocks/openblocks/OpenBlocks.java` (the `Blocks` class, lines 251-402), `mods/OpenBlocks/openblocks/common/block/` (`BlockElevator`, `BlockScaffolding`, `BlockPath`, `BlockCanvas`, `BlockSky`, `BlockLadder`, `BlockFlag`), `mods/OpenBlocks/openblocks/Config.java`, `mods/OpenModsLibs/openmods/config/game/GameConfigProvider.java` (lines 249-270), `mods/OpenModsLibs/openmods/utils/ColorUtils.java`, `config/OpenBlocks.cfg`. Sheets: `tools/block-contact-sheet/sheets/openblocks/` (previews in `docs/previews/blocks/openblocks/`, local only).

## Summary

- **Registry names.** Blocks are declared with `@RegisterBlock` annotations, not `GameRegistry.registerBlock` calls. OpenModsLib registers each under the annotation's plain `name` (`remapFromLegacy=true`, so the old `openblocks_<name>` names only remap), giving `OpenBlocks:<name>` (`GameConfigProvider.java` lines 264-270).
- **Counts.** 38 blocks declared; the pack disables 2 (`heal=false`, `tank=false` in `config/OpenBlocks.cfg` lines 30 and 44), leaving **36 registry names**. Block+meta entries: about 51 (the elevator's 16 colors, the sky block's 2 modes, and one entry each for the rest; orientation metas collapsed). **Useful: 7** (elevator colors, canvas, canvas glass, path, flag, jaded ladder, rope ladder), plus 3 marginal ones (target, grave, drawing table).
- **Look.** The useful blocks are plain: the elevator is a 16-color tinted stone tile (like colored concrete with a faint panel pattern), the canvas is flat white, the path is a thin stone-flag tile. Everything else is a gadget or machine with a model (fan, cannon, bear trap, XP machines, sprinkler) and is off-theme.
- **Caveats.**
  - **Scaffolding despawns.** `BlockScaffolding` ticks randomly and breaks itself (`scaffolding.despawnRate=4` in the pack config, `Config.java` line 563). Worldgen scaffolding would vanish within minutes of loading, so it is not usable.
  - **Elevator teleports.** A player standing on an elevator who jumps or sneaks is teleported to the next same-colored elevator up to 20 blocks away in the column (`Config.elevatorTravelDistance`). Harmless as a floor, but stacked same-color floors in a tower become a hidden lift. Use one color per column, or mix colors, unless that is the point.
  - **Canvas and canvas glass are tile entities.** A canvas with no paint data renders plain white, so the default TE looks right; painted patterns need TE NBT.
  - **Sky block** renders the sky through itself when active; meta 1 (inverted) is active without power. A curiosity, off-theme.
  - **Bear trap** traps mobs and players that walk on it; **target**, **big button**, **fan**, **cannon**, **block breaker/placer**, **sponge** (removes liquids and ticks) all have side effects. None belong in a city.
- **Optional dependency.** A server without OpenBlocks loses all of these. Fallbacks: elevator to `minecraft:wool` or `etfuturum:concrete` of the same color, canvas to `minecraft:wool:0` or `etfuturum:concrete:0`, canvas glass to `minecraft:glass`, path to `minecraft:stone_slab:0`, ladders to `minecraft:ladder`, flag to nothing.

## Blocks

### Construction and dressing

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `OpenBlocks:elevator:0-15` | Elevator (16 colors) | `wall-accent`, `floor` | partly | `modern`, `interior` | No TE. Meta is the wool color order (white 0 to black 15; `ColorUtils.java` line 75). Tinted pale-stone texture with a faint frame; reads as colored plaster or concrete panels. Teleports players between same-colored elevators in a column (see Summary) |
| `OpenBlocks:elevator_rotating:0-15` | Rotating Elevator | `floor` | partly | `modern` | TE (`TileEntityElevatorRotating`). Same as the elevator with an arrow pattern and facing; no reason to prefer it over the plain one |
| `OpenBlocks:canvas:0` | Canvas | `wall-main`, `wall-accent`, `ceiling` | yes | `modern`, `interior` | TE (`TileEntityCanvas`). Plain white block; a TE with no paint renders white. A clean drywall or gallery-wall white. Material sponge, soft to break |
| `OpenBlocks:canvasglass:0` | Canvas Glass | `glass` | partly | `modern` | TE. Clear glass with a light frame pattern; paintable like the canvas |
| `OpenBlocks:path:0` | Path | `sidewalk`, `floor` | yes | `park-garden`, `urban-stone` | No TE. 0.1-block-high stone-flag tile with no collision box; needs a solid block below or it drops (`BlockPath.java` lines 14-43). Good stepping-stone dressing on park paths and sidewalks |
| `OpenBlocks:ladder:0-15` | Jaded Ladder | `door-trap`, `special` | partly | `industrial`, `rustic` | No TE. A trapdoor that is also a ladder (extends `BlockTrapDoor`); trapdoor metas. Fits roof hatches over a ladder shaft |
| `OpenBlocks:ropeladder` (meta: orientation) | Rope Ladder | `special` | partly | `rustic`, `ruin` | No TE. Flat rope ladder, meta is orientation. Placing by hand unrolls it to the ground; worldgen writes each block itself. Fire-escape or ruin dressing |
| `OpenBlocks:flag` (meta: orientation) | Flag | `furniture-deco` | partly | `urban-stone`, `park-garden` | TE (`TileEntityFlag`: `colorIndex` 0-15 and `angle`). Model: a pole with a small pennant, on the ground or a wall; meta is orientation. Without TE data it is the default color. Rooftop and plaza dressing |
| `OpenBlocks:scaffolding:0` | Scaffolding | none | no | `industrial` | Despawns (see Summary). Not usable |
| `OpenBlocks:sky:0`, `:1` | Sky Block (normal, inverted) | `off-theme` (renders the sky) | no | `fantasy` | TE. Meta 1 shows the sky without power |
| `OpenBlocks:sponge:0` | Sponge | `off-theme` (removes liquids, ticks) | no | | |

### Gadgets and machines (off-theme)

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `OpenBlocks:target:0` | Target | `furniture-deco` | partly | `park-garden` | TE, model (archery target), light 4 (`setLightLevel(0.3f)`). Could dress a shooting range; emits redstone when hit |
| `OpenBlocks:grave:0` | Grave | `furniture-deco` | partly | `ruin` | TE (stores a dead player's items). A headstone model; an empty grave is plausible cemetery dressing but the TE expects player data |
| `OpenBlocks:drawingtable:0` | Drawing Table | `furniture-deco` | partly | `interior` | TE. Wood-topped table with a white sheet; office or studio dressing |
| `OpenBlocks:trophy:0` | Trophy | `furniture-deco` | partly | `interior` | TE: needs a mob type in NBT, otherwise renders empty |
| `OpenBlocks:bigbutton:0`, `fan`, `beartrap`, `sprinkler`, `cannon`, `vacuumhopper`, `xpbottler`, `xpdrain`, `xpshower`, `autoanvil`, `autoenchantmenttable`, `blockbreaker`, `blockPlacer`, `itemDropper`, `village_highlighter`, `donationStation`, `paintmixer`, `paintcan`, `projector`, `guide`, `builder_guide`, `goldenegg`, `imaginary` | Machines and gadgets | `off-theme` (functional machines, models, side effects) | no | | All are TEs. Bear trap catches entities; fan pushes them; golden egg runs a hatching animation; imaginary blocks are invisible without glasses |

## Best variety pools

- **Interior white wall:** `OpenBlocks:canvas:0` (with `etfuturum:concrete:0` and `minecraft:quartz_block:0`).
- **Colored panels, modern lobbies and stores:** `OpenBlocks:elevator` in muted colors (8 light gray, 7 gray, 0 white, 3 light blue, 9 cyan, 12 brown), one color per column or mixed.
- **Park and sidewalk overlay:** `OpenBlocks:path:0` on grass or stone.
- **Roof hatch:** `OpenBlocks:ladder` over a vanilla ladder shaft.
- **Fire escape or ruin:** `OpenBlocks:ropeladder` hanging down a facade.
- **Rooftop and plaza dressing:** `OpenBlocks:flag` (TE color varies).

## Worldgen notes

- Canvas, canvas glass, flag, grave, trophy and drawing table need their TE created when placed. The canvas's default TE is correct as is; a flag needs `colorIndex` and `angle` for variety.
- Never place scaffolding, sky blocks, bear traps or any machine.
- Elevators in stacked floors of one color form a working lift; decide per building.
