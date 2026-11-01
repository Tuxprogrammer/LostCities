# Binnie's Mods (ExtraTrees, Botany, ExtraBees, Genetics, BinnieCore)

Family D. Mod IDs `ExtraTrees`, `Botany`, `ExtraBees`, `Genetics`, `BinnieCore`, one jar `binnie-mods-2.6.51.jar`. Source read: `mods/binnie-mods/binnie/extratrees/block/` (`ModuleBlocks.java`, `BlockETPlanks.java`, `BlockETLog.java`, `BlockETSlab.java`, `BlockETStairs.java`, `BlockETDoor.java`, `DoorType.java`, `PlankType.java`, `ILogType.java`, `WoodManager.java`, `decor/BlockFence.java`, `decor/BlockMultiFence.java`, `decor/BlockGate.java`, `decor/FenceType.java`), `extratrees/carpentry/` (`ModuleCarpentry.java`, `BlockCarpentryPanel.java`, `BlockStainedDesign.java`, `GlassType.java`, `CarpentryInterface.java`), `extratrees/machines/ModuleMachine.java`, `botany/gardening/ModuleGardening.java`, `botany/ceramic/` (`BlockCeramic.java`, `BlockCeramicBrick.java`, `BlockStained.java`, `BlockCeramicPatterned.java`), `botany/genetics/EnumFlowerColor.java`, `botany/gardening/BlockSoil.java`, `core/block/TileEntityMetadata.java`, `core/block/IBlockMetadata.java`, `core/BinnieCore.java`, `core/machines/MachineGroup.java`, `extrabees/`, `genetics/machine/ModuleMachine.java`, lang files under `mods/binnie-mods/assets/*/lang/en_US.lang`. Pack config: `config/ExtraTrees.cfg`, `config/ExtraBees.cfg` (no block switches). Sheets: `tools/block-contact-sheet/sheets/binnie/` (previews in `docs/previews/blocks/binnie/`, local only).

## Summary

- **31 block registry names**: ExtraTrees 13 (`planks`, `log`, `fence`, `multifence`, `gate`, `slab`, `doubleSlab`, `door`, `stairs`, `carpentry`, `panel`, `stainedglass`, `machine`), Botany 12 (`plant`, `soil`, `loam`, `flowerbed`, `soilNoWeed`, `loamNoWeed`, `flowerbedNoWeed`, `ceramic`, `stained`, `ceramicPattern`, `ceramicBrick`, `flower`), ExtraBees 3 (`hive`, `ectoplasm`, `alveary`), Genetics 3 (`machine`, `labMachine`, `advMachine`), BinnieCore 1 (`storage`). Leaves and saplings are Forestry's genetic blocks; ExtraTrees' hedge block is declared but never created, and its shrub-leaf block is created but never registered (`ModuleBlocks.preInit`), so neither exists in game.
- **Placeable combinations are effectively unbounded**: the variant lives in an integer, not in block metadata. Counted plainly: 35 planks, 40 logs, 70 woods for each of fence, gate, slab, stairs and four door styles (about 560), 36 ceramic colors, 36 pigmented glass colors, 10 ceramic brick and tile patterns × 36 × 36 colors, about 100 carpentry designs × two materials for tiles, panels and stained glass. **Useful: about 200** hand-picked ones (below).
- **Everything decorative is a tile entity.** Binnie's `IBlockMetadata` extends `ITileEntityProvider`; every ExtraTrees wood block, carpentry block, Botany ceramic and pigmented glass keeps its variant in `TileEntityMetadata` (`binnie.tile.metadata`), NBT `meta` (int). World metadata is only orientation (log axis, stair facing, slab half, door halves). The tile does not tick (`canUpdate` returns false) and syncs once by a description packet. `world.setBlock` creates the tile with `meta` 0; the writer must set `meta`.
- **Two surprises worth the cost.** (1) **Botany ceramic bricks and tiles** (`Botany:ceramicBrick`) are a real urban wall and floor family: running bond bricks, large bricks, striped bricks, vertical bricks, square tiles, checkerboard and mixed tiles, each in two of 36 colors over a light mortar layer. Brown, sienna, maroon, tan, wheat, gray, white and black bricks look like city brick; white tile reads as bathroom or subway tile; black-and-white checker reads as a diner floor (tinted review render, see "Rendering" below). (2) **ExtraTrees doors come in every vanilla wood**, plus 35 ExtraTrees and 29 Forestry woods, in four styles (paneled with a glazed top, solid, and two fully glazed grids). The pack disables Et Futurum's wood doors (block-mapping.md, D19), so this is the only source of spruce, birch, jungle, acacia and dark oak doors.
- Rendering: ceramics, pigmented glass, stained-glass designs, doors and log bark are grayscale textures tinted at render time. The contact sheet tool has no tint, so the committed sheets show untinted layers; colors were judged from a one-off tinted review composite (ceramic patterns in 14 colors, doors in three woods, two glass tints), not committed.
- Optional dependency: without Binnie, ceramic bricks fall back to `minecraft:brick_block` (red-brown colors), `minecraft:stonebrick` (gray), `minecraft:quartz_block` (white) or `minecraft:stained_hardened_clay` (other colors); ceramic tile floors to `minecraft:stained_hardened_clay` or `minecraft:quartz_block`; doors to `minecraft:wooden_door`; ExtraTrees planks, stairs and fences to the nearest vanilla wood; pigmented and designed glass to `minecraft:stained_glass`.

## Blocks

Wood index (fence, gate, slab, stairs, door, multifence; `PlankType.setup` and `WoodManager.getPlankTypeIndex`): vanilla 0 to 5 (oak, spruce, birch, jungle, acacia, dark oak), ExtraTrees `32 + ordinal`, Forestry `128 + ordinal` (Forestry's 29 woods, order as in forestry.md). ExtraTrees plank ordinals (`PlankType.ExtraTreePlanks`, also `ExtraTrees:planks` `meta`): 0 Fir, 1 Cedar, 2 Hemlock, 3 Cypress, 4 Fig, 5 Beech, 6 Alder, 7 Hazel, 8 Hornbeam, 9 Box, 10 Butternut, 11 Hickory, 12 Whitebeam, 13 Elm, 14 Apple, 15 Yew, 16 Pear, 17 Hawthorn, 18 Rowan, 19 Elder, 20 Maclura, 21 Syzgium, 22 Brazilwood, 23 Logwood, 24 Iroko, 25 Locust, 26 Eucalyptus, 27 Purpleheart, 28 Ash, 29 Holly, 30 Olive, 31 Sweetgum, 32 Rosewood, 33 Ginkgo, 34 PinkIvory.

Colors (`EnumFlowerColor`, ceramic and pigmented glass): 0 aquamarine, 1 black, 2 blue, 3 brown, 4 chocolate, 5 coral, 6 crimson, 7 cyan, 8 gold, 9 goldenrod, 10 gray, 11 green, 12 indigo, 13 khaki, 14 lavender, 15 lime, 16 magenta, 17 maroon, 18 navy, 19 olive, 20 orange, 21 peru, 22 pink, 23 plum, 24 purple, 25 red, 26 salmon, 27 sienna, 28 tan, 29 teal, 30 thistle, 31 turquoise, 32 violet, 33 wheat, 34 white, 35 yellow.

### Botany ceramics and glass (`botany-ceramic-misc.png`)

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Botany:ceramicBrick`:0, `meta` = color1 + 256 × color2 + 65536 × 1 | Ceramic Bricks | wall-main, wall-accent | yes | urban-brick | Running-bond bricks with light mortar. City colors: 3 brown, 27 sienna, 17 maroon, 4 chocolate, 21 peru, 28 tan, 33 wheat, 10 gray, 34 white, 1 black. Two colors give mixed-brick fronts (color2 colors alternate bricks) |
| `Botany:ceramicBrick`, type 3 | Large Ceramic Bricks | wall-main | yes | urban-brick, modern | Larger blocks: reads as stone-clad or glazed block walls in gray, white, tan |
| `Botany:ceramicBrick`, type 2 | Striped Ceramic Bricks | wall-accent | partly | urban-brick | Alternating courses in the two colors; a band or plinth accent |
| `Botany:ceramicBrick`, types 7, 8, 9 | Vertical, Vertical Striped, Large Vertical Ceramic Bricks | wall-accent, trim-pillar | partly | urban-brick | Soldier-course look for lintels and pilasters |
| `Botany:ceramicBrick`, type 0 | Ceramic Tile | floor, wall-accent | yes | interior, modern | 2×2 square tiles with thin grout. White and gray read as bathroom and kitchen tile; black and maroon as lobby floors |
| `Botany:ceramicBrick`, type 5 | Chequered Ceramic Tile | floor | yes | interior | Small checkerboard; black and white is a diner or lobby floor |
| `Botany:ceramicBrick`, types 4, 6 | Split, Mixed Ceramic Tile | floor | partly | interior | Split is a large checker in two colors; mixed has dark inset squares |
| `Botany:ceramic`:0, `meta` 0 to 35 | Ceramic Block | wall-main, wall-accent, floor | partly | modern | Flat glazed color, almost no texture. White, wheat, tan and gray read as plaster or painted concrete; bright colors are off-theme |
| `Botany:stained`:0, `meta` 0 to 35 | Pigmented Glass | glass | partly | modern | Translucent tinted glass (render pass 1). 36 colors beyond vanilla's 16; gray, navy, teal and black suit office towers |
| `Botany:ceramicPattern`:0, `meta` = design and two colors | Ceramic Block (patterned) | floor, wall-accent | partly | interior | Same design system as ExtraTrees carpentry (emblems, stripes, edges, bars, letters, diagonals). Letters and bars could make signage or borders |
| `Botany:soil`, `loam`, `flowerbed` and `*NoWeed`:0 to 11 | Soil, Loam, Flowerbed | foliage | partly | park-garden, overgrown | Meta = acidity × 3 + moisture (`BlockSoil`). Random ticks adjust moisture; the plain variants grow weeds. Use the `NoWeed` variants if any |
| `Botany:flower`, `Botany:plant` | Flower, Weeds | foliage | no | park-garden | Flowers are genetic tile entities with a genome; weeds spread. Vanilla flowers are simpler |

### ExtraTrees woods (`extratrees-planks.png`, `extratrees-logs.png`, `extratrees-decor.png`)

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `ExtraTrees:planks`:0, `meta` 0 to 34 | <Wood> Wood Planks | floor, ceiling, wall-accent | partly | interior, rustic | Textures are pre-colored. Good floors: Hickory 11 (light brown), Beech 5, Elm 13, Hornbeam 8 (mid brown), Apple 14, Iroko 24 (dark brown), Brazilwood 22, Purpleheart 27, Rosewood 32 (deep red), Whitebeam 12 (gray), Holly 29, Box 9, Ginkgo 33 (cream). Off: PinkIvory 34 (pink), Logwood 23 (red), Maclura 20 (yellow) |
| `ExtraTrees:log`:0/4/8, `meta` 0 to 39 | <Wood> Log | trim-pillar | partly | rustic | 40 log types; bark is grayscale and tinted, trunk end is colored. Banana 11 and Eucalyptus3 37 have odd green or rainbow bark |
| `ExtraTrees:door`:door halves, `meta` = 256 × style + wood index | <Style> <Wood> Wood Door | door-trap | yes | urban-brick, interior, rustic | Styles (`DoorType`; lang name in parentheses): 0 standard (paneled lower half, glazed upper), 1 Solid (no glass), 2 double ("Split"; coarse glazed grid in both halves), 3 Full (fine glazed grid in both halves). Judged from the lower-half textures on `extratrees-decor.png` and the opacity of the upper-half textures. The texture is tinted by the wood color. The only wood-variant doors in the pack; covers all vanilla woods (index 1 to 5) |
| `ExtraTrees:fence`:0, `meta` = wood index | <Wood> Fence | fence-rail | partly | rustic, park-garden | 70 woods; renders with its own fence renderer |
| `ExtraTrees:gate`:gate bits, `meta` = wood index | <Wood> Gate | fence-rail, door-trap | partly | rustic, park-garden | Wood-variant fence gates, again including all vanilla woods |
| `ExtraTrees:multifence`:0, `meta` = plank1 + 256 × fence type + 65536 × plank2 | <Wood> fence variants | fence-rail | partly | park-garden | Fence type bits (`FenceType`): size 0 to 2, solid, embossed. Solid tall fences make privacy fences around yards |
| `ExtraTrees:stairs`:0 to 7, `meta` = wood index | <Wood> Stairs | stairs-slab | partly | interior, rustic | |
| `ExtraTrees:slab`:0, 8; `ExtraTrees:doubleSlab`, `meta` = wood index | <Wood> Wood Slab | stairs-slab | partly | interior | |
| `ExtraTrees:carpentry`:0, `meta` = plank1 + (plank2 << 8) + (design << 16) + (rotation << 26) + (facing << 28) | Wooden Tile | floor, wall-accent | partly | interior | Two-wood inlay designs (about 100, `EnumDesign`): parquet borders, stripes, checkers, letters. Good for lobby floors and paneled studies |
| `ExtraTrees:panel`:0, same `meta` encoding | Wooden Panel | furniture-deco, floor | partly | interior | A 1/16 panel on one face (`BlockCarpentryPanel`): wainscoting or a thin inlaid floor over another block |
| `ExtraTrees:stainedglass`:0, same `meta` encoding with glass materials | <Color> Stained Glass | glass | partly | urban-brick, interior | Materials are vanilla stained glass 0 to 15 or Botany pigmented glass `128 + color` (`GlassType`). Designs with two glass colors make leaded or mullioned windows (barred, crossed, edged, ornate barred); a church or old-bank window |
| `ExtraTrees:machine`:0 to 4 | Lumbermill, Woodworker, Panelworker, Glassworker, Tileworker | off-theme | no | industrial | Machines |

### ExtraBees, Genetics, BinnieCore

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `ExtraBees:hive`:0 to 3 | Water, Rock, Nether, Marble Hive (`EnumHiveType`) | off-theme | no | | Natural hives; the rock and marble hives look like striped stone cubes on the sheet |
| `ExtraBees:ectoplasm` | Ectoplasm | off-theme | no | | Translucent green goo block |
| `ExtraBees:alveary`, `Genetics:machine`, `Genetics:labMachine`, `Genetics:advMachine`, `BinnieCore:storage` | Mutator and other alveary parts, genetics machines, compartments | off-theme | no | industrial | Machines and storage |

## Best variety pools

- **Brick walls (wall-main):** `Botany:ceramicBrick` bricks in brown, sienna, maroon, chocolate, tan, wheat, gray, white, black; large bricks in gray, white, tan. Ten genuinely new brick colors for the `bricks_*` palettes.
- **Brick accents (wall-accent, trim):** striped bricks (wheat on brown, gray on black), vertical and large vertical bricks for lintels and piers.
- **Tile floors:** ceramic tile white, gray, black; chequered black and white; split gray and white. Bathrooms, kitchens, lobbies and diners.
- **Doors:** ExtraTrees doors in all styles over oak, spruce, birch, dark oak and the darker ExtraTrees and Forestry woods. Glazed-grid doors (styles 2 and 3) for shop fronts, standard for houses, solid for back doors.
- **Leaded windows:** ExtraTrees stained glass, barred or crossed designs, light gray or white glass with black or gray bars.
- **Interior floors:** ExtraTrees planks Hickory, Beech, Elm, Iroko, Whitebeam, Box; carpentry wooden tiles with a border design for lobbies.
- **Park fences:** ExtraTrees fences and gates in vanilla and ExtraTrees woods; solid multifences for yards.

## Worldgen notes

- **One tile entity per block, for every decorative Binnie block.** The tile is cheap (no ticking) but each is saved in the chunk and sent to clients. A whole brick facade in ceramic bricks is hundreds of tile entities per building. Use ceramics for accent walls, ground-floor shop fronts, single-floor tile, and a minority of buildings; keep vanilla bricks as the bulk. Measure chunk save size and client load before raising the share.
- Writer contract: place the block, then set the tile's `meta` (or write `{id:"binnie.tile.metadata", meta:<int>}`). Both door halves carry a tile; the renderer reads the lower half's (`BlockETDoor.getIcon`), so set both.
- A missing or unset tile renders as variant 0 (Fir planks, aquamarine ceramic, standard oak door) and drops that variant when broken: a visible, player-facing error. The dev self-check should report any Binnie block written without its tile.
- No light, no gravity. Pigmented and designed glass are translucent (pass 1). Plain Botany soils tick and grow weeds.
- The ceramic and carpentry `meta` packs colors and designs into one int; asset files should name the parts (pattern, color1, color2, design, rotation) and the writer should pack them, rather than storing raw numbers in assets.
- A server without Binnie's Mods loses all of these; every palette entry needs the vanilla fallback listed in the summary.
