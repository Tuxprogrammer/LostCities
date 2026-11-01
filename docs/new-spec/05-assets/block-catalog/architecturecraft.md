# ArchitectureCraft

Family D. Mod ID `ArchitectureCraft`, jar `ArchitectureCraft-1.12.19.jar`. Source read: `mods/ArchitectureCraft/gcewing/architecture/ArchitectureContent.java` (L70-L78, L142-L146), `common/shape/Shape.java`, `common/tile/TileShape.java` (L159-L190), `common/tile/TileArchitecture.java` (L120-L147). Sheets: none rendered: every shape takes its texture from a base block chosen in NBT, and the jar has no block textures besides the sawbench (the shapes are OBJ-style models), so a cube sheet would show nothing useful.

## Summary

- **3 registry names**: `ArchitectureCraft:sawbench` (crafting station), `ArchitectureCraft:shape`, `ArchitectureCraft:shapeSE` (the "glow" variants). **106 shapes** are selected by the `Shape` int in the tile entity: roof tiles, ridges, valleys and overhangs; cylinders, round pillars, posts, poles; Doric, Ionic and Corinthian capitals, pillar bases, triglyphs, metopes, architraves; window frames, corners and mullions; spheres; cornices; cladding; arches (diameters 1-4); plain and fancy balustrades, banisters and newels; slabs, stairs and slope tiles.
- **The best architectural-detail set in the pack**: cornices and balustrades for 1900s-1930s facades, window mullions for office grids, columns and capitals for banks, museums and city halls, real pitched and hipped roofs. These are what make a building look designed rather than voxel-stacked.
- **Everything is in one non-ticking TE (`gcewing.shape`)**, and its NBT is **name-based**: `Shape` (int id from `Shape.java`), `BaseName`/`BaseData` (the material block's registry name and metadata), optional `Name2`/`Data2` (secondary material, e.g. glass in window frames), `side` and `turn` (orientation bytes), `offsetX`, `Disconnected` (`TileShape.java` L171-L190, `TileArchitecture.java` L126-L146). No numeric IDs, so worldgen can write it reliably.
- **A shape with no NBT has no shape and no material**: it is useless without NBT.
- **Light**: `shapeSE` variants glow (shape names ending in "(Glow)"); others take light from the base block. **Gravity**: none, even with sand or gravel as material (unverified). **Interaction**: chisel and hammer change connections and orientation.
- **Fallback when the mod is absent**: each shape to the nearest vanilla form of its base block: roof tiles and slopes to the base block's stairs, cornices to upside-down stairs, balustrades to `minecraft:cobblestone_wall` or `minecraft:fence`, pillars to the base block or `minecraft:quartz_block:2`, window frames and mullions to `minecraft:glass_pane`.

## Blocks

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `ArchitectureCraft:shape` + `Shape` 0-9, 40-45, 115-116 | Roof tiles, corners, ridges, valleys, overhangs, gable overhangs, double roof tile | roof | yes | urban-brick, rustic | pitched roofs for houses and older blocks; material terracotta, brick, dark planks, stone slab |
| `…shape` + `Shape` 50-56 | Cornices (LH, RH, ends, ridge, valley, bottom) | trim-pillar, wall-accent | yes | urban-stone, urban-brick | roofline cornices on brick and stone buildings: the classic 1920s skyline detail |
| `…shape` + `Shape` 15-17, 20-23, 10-14 | Round pillar, post, pole, pillar base, Doric/Ionic/Corinthian capitals, cylinders | trim-pillar | yes | urban-stone | banks, courthouses, museums, train stations (quartz, smooth stone) |
| `…shape` + `Shape` 24-28 | Triglyph, metope, architrave | trim-pillar, wall-accent | partly | urban-stone | neoclassical friezes; rare |
| `…shape` + `Shape` 30-32 | Window frame, window corner, window mullion | glass, pane, trim-pillar | yes | modern, urban-stone | framed glass with a secondary glass material; office grids |
| `…shape` + `Shape` 70-88 | Balustrades, banisters, newels (plain and fancy) | fence-rail | yes | urban-stone | rooftop parapets, balconies, grand staircases |
| `…shape` + `Shape` 61-68 | Arches (diameter 1-4) | wall-accent | yes | urban-brick, urban-stone | arched windows and arcades |
| `…shape` + `Shape` 60 | Cladding | wall-accent | partly | modern | thin facade sheets |
| `…shape` + `Shape` 18-19, 33-38 | Bevelled corners, spheres | wall-accent | partly | modern | rounded building corners |
| `…shape` + `Shape` 90-102 | Slab, stairs, slope tiles A, B, C | stairs-slab, roof | partly | any | stairs and slabs duplicate vanilla; slope tiles give shallow roofs |
| `ArchitectureCraft:shapeSE` + `Shape` 103-114 | Glow slopes, roof tile, square, slab | light | partly | modern | glowing variants (lit signage strips) |
| `ArchitectureCraft:sawbench` | Sawbench | off-theme | no | | crafting station |

## Best variety pools

- **trim-pillar / wall-accent (rooflines)**: cornices in the facade's stone, quartz or brick.
- **roof**: roof tiles plus ridge and overhangs in terracotta, brick or spruce for low-rise houses.
- **trim-pillar (civic buildings)**: round pillars with Doric or Ionic capitals in quartz or smooth stone.
- **fence-rail (rooftops, balconies)**: plain balustrades.
- **glass (offices)**: window mullion and frame shapes with a stone frame and glass secondary.

## Worldgen notes

- Feasible and comparatively cheap: one non-ticking TE per block; rendering is cached model geometry (ISBRH), not a TESR.
- The asset format needs a way to say "shape S of material M, turned T", which the palette system does not have yet; it is a new palette-entry kind. Until then, ArchitectureCraft can only appear in hand-written parts.
- `side` and `turn` semantics come from `Trans3` in the mod; M02 must tabulate them before use.
