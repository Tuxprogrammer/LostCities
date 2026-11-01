# LittleTiles

Family D. Mod IDs `littletiles` (with `creativecore`), jar `littletiles-1.6.63.jar`. Source read: `mods/littletiles/com/creativemd/littletiles/LittleTiles.java` (L93-L99, L146-L149), `common/blocks/BlockLTColored.java`. Sheets: `tools/block-contact-sheet/sheets/littletiles/colored-blocks.json` (previews in `docs/previews/blocks/littletiles/`, local only).

## Summary

- **3 registry names**: `littletiles:LTColoredBlock` (11 metas), `littletiles:BlockLittleTiles` (the tile container), `littletiles:LTMissingBlock`.
- **`LTColoredBlock` is a plain block (no TE)** with 11 near-white textures: clean, floor, grainy big, grainy, grainy low, brick, bordered, big brick, structured, broken big brick, clay (`BlockLTColored.subBlocks`). LittleTiles normally tints them through tiles; placed directly as blocks they stay white and light gray, because the class has no color multiplier. That makes them **white plaster, white tile and whitewashed brick**, a palette the pack otherwise covers only with quartz and white concrete.
- **It breaks instantly.** The block is built on a bare `new Material(MapColor.stoneColor)` with no `setHardness` call (`LittleTiles.java` L93-L98, `BlockLTColored` constructor), so hardness is 0: a punch removes it, like a flower. That rules it out for structural walls players will fight in or build into; use it for interior finishes (ceilings, bathroom floors, a plaster layer over a real wall) where a fragile surface does not matter.
- `BlockLittleTiles` holds sub-block tiles in NBT and is not practical for generation.
- **Fallback when the mod is absent**: `minecraft:quartz_block:0` (clean, bordered, structured), `etfuturum:concrete:0` (clean, grainy), `minecraft:stonebrick:0` (brick variants), `minecraft:hardened_clay` (clay).

## Blocks

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `littletiles:LTColoredBlock:0` | Clean | ceiling, wall-main | partly | modern, interior | flat white: plaster walls, ceilings |
| `littletiles:LTColoredBlock:1` | Floor | floor | partly (hardness 0) | interior | white tile pattern |
| `littletiles:LTColoredBlock:2-4` | Grainy (big, normal, low) | wall-main | partly (hardness 0) | modern, urban-stone | white stucco or rendered concrete |
| `littletiles:LTColoredBlock:5` | Brick | wall-main | partly (hardness 0) | urban-brick | whitewashed small brick |
| `littletiles:LTColoredBlock:6` | Bordered | wall-accent, ceiling | partly (hardness 0) | modern | white panel with a border: ceiling tiles |
| `littletiles:LTColoredBlock:7` | Big Brick | wall-main | partly (hardness 0) | urban-stone | white ashlar |
| `littletiles:LTColoredBlock:8` | Structured | wall-accent | partly | modern | concentric square relief |
| `littletiles:LTColoredBlock:9` | Broken Big Brick | rubble-ruin, wall-main | partly (hardness 0) | ruin | cracked white ashlar: the damaged form of meta 7 |
| `littletiles:LTColoredBlock:10` | Clay | wall-main | partly | desert | plain white clay |
| `littletiles:BlockLittleTiles`, `LTMissingBlock` | Tile container, missing placeholder | off-theme | no | | NBT-only content |

## Best variety pools

- **wall-main (interior plaster only)**: metas 0, 2, 3, 7, with 9 as the ruined variant of 7; never as the outer shell (hardness 0).
- **ceiling (offices)**: metas 0 and 6.
- **floor (bathrooms, clinics)**: meta 1.

## Worldgen notes

- Plain blocks: free to place, no TE.
- Verify in game that placed blocks are white (no tint from a missing TE).
- Hardness 0 makes them ruin-proof in the wrong way: explosion and ruin damage will strip them first. Treat them as a finish layer.
