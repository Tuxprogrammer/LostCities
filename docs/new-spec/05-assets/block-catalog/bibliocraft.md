# BiblioCraft and BiblioWoods

Family D. Mod IDs `BiblioCraft` (jar `BiblioCraft[v1.11.7][MC1.7.10].jar`), `BiblioWoodsBoP` (`BiblioWoods[BiomesOPlenty][v1.9].jar`), `BiblioWoodsForestry` (`BiblioWoods[Forestry][v1.7].jar`), `BiblioWoodsNatura` (`BiblioWoods[Natura][v1.5].jar`). Source read: `mods/BiblioCraft/jds/bibliocraft/blocks/BlockLoader.java`, `blocks/Block*.java`, `tileentities/TileEntity*.java`, `assets/bibliocraft/lang/en_US.lang`; `mods/BiblioWoods_{BiomesOPlenty,Forestry,Natura}/` (registration and lang); pack config `config/BiblioCraft.cfg`. Sheets: `tools/block-contact-sheet/sheets/bibliocraft/model-atlases.json` (previews in `docs/previews/blocks/bibliocraft/`, local only).

## Summary

- **BiblioCraft: 34 registry names, about 246 block+meta entries**; every block is enabled in the pack (`config/BiblioCraft.cfg` L9-L50, all `true`). **BiblioWoods: 18 names x 14 woods (BoP), 36 names covering 24 woods (Forestry, split over a first and a `…2` block), 18 names x 13 woods (Natura)**: about 920 more entries. Useful: roughly 25 furniture types, multiplied by wood.
- **This is the best interior-dressing mod in the pack**: bookcases, desks, tables, seats, shelves, lamps, lanterns, clocks, typewriters, display cases, signs, paintings, plates and cookie jars. It is what makes an office, library, apartment or shop read as lived in. The furniture is 1700s-to-1950s wooden in style; it fits upstream's older brick buildings well and modern glass towers less well.
- **Every placeable block is a tile entity with a TESR model.** The look is a model, not a cube face; the contact sheet shows only the UV atlases (to judge material and color). TESR rendering is heavy: dozens per chunk will cost client frame time.
- **Meta = wood (or color); facing lives in the TE**, so worldgen must write NBT for every piece it wants facing a particular way. Defaults (angle 0) all face the same direction.
- **Light**: lamps and lanterns (`BiblioLamp`, `BiblioIronLamp`, `BiblioLantern`, `BiblioIronLantern`) emit light 15 with no power and no fuel (`BlockLamp.java` L60-L62; `EnableLightEmission=true` in the config). They are the only working light fixtures in this slice that need nothing.
- **Tables, shelves, labels and weapon cases** borrow light from items placed on them (`getLightValue` reads the inventory), irrelevant when generated empty.
- **No gravity.** Interaction: seats are rideable (players sit), clocks tick and chime, bells ring, typewriters and printing presses open GUIs.
- **Fallback when the mod is absent**: bookcase to `minecraft:bookshelf`; desk and table to `minecraft:crafting_table` or an oak slab on a fence post; seat to an oak stair; lamp and lantern to `etfuturum:lantern` or `minecraft:torch`; everything else to air. BiblioWoods pieces fall back to the matching BiblioCraft vanilla-wood piece first.

## Blocks

Wood metas for BiblioCraft: 0 oak, 1 spruce, 2 birch, 3 jungle, 4 acacia, 5 dark oak, 6 framed (takes a custom texture from a `customTexture` string in NBT; plain frame without it). Color metas (lamps, lanterns, typewriters, sword pedestals): dye order 0 white to 15 black, verify order in M02 against the lang keys (`WhiteTypewriter` … `BrownTypewriter` are listed white, light gray, gray, black, red, orange, yellow, lime, green, cyan, light blue, blue, purple, magenta, pink, brown in the lang file, which may be display order only).

### Furniture (wood metas 0-6)

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `BiblioCraft:Bibliotheca:0-6` | Bookcase | furniture-deco | yes | interior | TE `TileEntityBookcase`; NBT `bookcaseAngle` (0-3), `Inventory` (16 book slots), `customTexture`. Generated empty (bare shelves) unless book items are written into `Inventory` (numeric item IDs: resolve at placement) |
| `BiblioCraft:BookcaseFilled:0-6` | Bookcase (randomly filled) | furniture-deco | yes | interior | fills itself only in `additionalPlacmentCommands`, which runs on player placement (`BlockBookcaseCreative.java`); worldgen gets an empty bookcase unless it calls `addRandomBooksToShelf()` itself |
| `BiblioCraft:BiblioDesk:0-6` | Desk | furniture-deco | yes | interior | NBT `deskAngle`; offices, libraries |
| `BiblioCraft:BiblioTable:0-6` | Table | furniture-deco | yes | interior | legs and top pieces (`leg1-4`, `top1-4`, `exposedside1-4`) recompute from neighbors in `updateEntity` (ticking TE); no angle |
| `BiblioCraft:BiblioSeats:0-6` | Seat (chair) | furniture-deco | yes | interior | NBT `Angle`, `hasBack`, `seatColor` (cushion, 0 default), `backWoodType`; a back needs `hasBack` set, else a stool. Ticking TE |
| `BiblioCraft:BiblioShelf:0-6` | Shelf | furniture-deco | yes | interior | NBT `genericShelfAngle`; shop shelves |
| `BiblioCraft:BiblioPotionShelf:0-6` | Potion Shelf | furniture-deco | partly | interior | small-item shelf, reads as a pharmacy or bar shelf |
| `BiblioCraft:BiblioRack:0-6` | Tool Rack | furniture-deco | partly | interior, industrial | NBT `rackAngle` |
| `BiblioCraft:BiblioCase:0-6` | Display Case | furniture-deco | yes | interior | glass-topped case; shops, museums. NBT `caseAngle` |
| `BiblioCraft:BiblioLabel:0-6` | Label | furniture-deco | partly | interior | small wall label |
| `BiblioCraft:BiblioMapFrames:0-6` | Map Frame | furniture-deco | yes | interior | wall frame; NBT `Angle`, `vertAngle` |
| `BiblioCraft:BiblioFancySign:0-6` | Fancy Sign | furniture-deco | yes | interior, urban-brick | shop signs; text lines in NBT |
| `BiblioCraft:BiblioWorkbench:0-6` | Fancy Workbench | furniture-deco | partly | interior | |
| `BiblioCraft:BiblioClock:0-6` | Clock | furniture-deco | yes | interior | wall or grandfather clock; ticking TE (sound) |
| `BiblioCraft:BiblioPaneler:0-6` | Furniture Paneler | special | no | interior | crafting station; skip |
| `BiblioCraft:Biblio{Flat,Simple,Middle,Fancy,Borderless}Painting:0-6` | Painting frames | furniture-deco | yes | interior | empty frames unless a painting is set in NBT |

### Lights and colored pieces (metas 0-15)

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `BiblioCraft:BiblioLamp:0-15` | Gold Lamp (shade color by meta, matching the 16 `lampLight<n>.png` textures; unverified) | light, furniture-deco | yes | interior | light 15, no power; NBT `angle`, `style` (floor, wall or ceiling, set from the clicked face on placement), `metal`, `isLightOff` |
| `BiblioCraft:BiblioIronLamp:0-15` | Iron Lamp | light, furniture-deco | yes | interior, industrial | as above, iron body |
| `BiblioCraft:BiblioLantern:0-15` | Gold Lantern | light | yes | interior, urban-brick | light 15; hanging or standing per `style` |
| `BiblioCraft:BiblioIronLantern:0-15` | Iron Lantern | light | yes | urban-brick | street-lamp-like when hung outside |
| `BiblioCraft:BiblioTypewriter:0-15` | Typewriter (color by meta) | furniture-deco | yes | interior | office desks; NBT `angle` |
| `BiblioCraft:BiblioSwordPedestal:0-15` | Sword Pedestal | furniture-deco | no | fantasy | |

### Single pieces

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `BiblioCraft:BiblioStuffs:0` | Cookie Jar | furniture-deco | yes | interior | kitchens, counters |
| `BiblioCraft:BiblioStuffs:2` | Dinner Plate | furniture-deco | yes | interior | dining tables |
| `BiblioCraft:BiblioStuffs:3` | Disc Rack | furniture-deco | partly | interior | |
| `BiblioCraft:BiblioBell:0` | Desk Bell | furniture-deco | yes | interior | reception desks |
| `BiblioCraft:BiblioClipboard:0` | Clipboard | furniture-deco | yes | interior | |
| `BiblioCraft:Armor Stand:0` | Armor Stand | furniture-deco | partly | interior | registry name contains a space |
| `BiblioCraft:Typesetting Machine:0`, `BiblioCraft:Printing Press:0` | Typesetting Table, Printing Press | furniture-deco | partly | interior, industrial | print shops; registry names contain spaces |
| `BiblioCraft:BiblioPaintPress:0` | Painting Press | special | no | interior | |
| `BiblioCraft:tile.BiblioFramedChest:0` | Framed Chest | furniture-deco | partly | interior | registered with `getUnlocalizedName()` (`BlockLoader.java` L98), so the name carries `tile.`; verify in M02 |

### BiblioWoods

Each edition registers the same furniture set in its own woods; meta = wood index in the order of the edition's lang file. Only the wood changes, so these are variety for the rows above.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `BiblioWoodsBoP:BiblioWood{Bookcase,potshelf,shelf,rack,case,label,desk,table,Seat,MapFrame,FancySign,FancyWorkbench,Clock,PaintingT0-T4}:0-13` | BoP-wood furniture (Sacred Oak, Cherry, Dark, Fir, Hellbark, Ethereal, Jacaranda, Magic, Mangrove, Palm, Pine, Redwood, Willow, Mahogany) | furniture-deco | partly | interior | Cherry, Fir, Pine, Redwood, Willow, Mahogany read as real furniture woods; Magic, Ethereal, Hellbark are fantasy colors |
| `BiblioWoodsForestry:BiblioWoodFst{Bookcase,Bookcase2,…}` (36 names) | Forestry-wood furniture (24 woods: Larch, Teak, Acacia, Lime, Chestnut, Wenge, Baobab, Sequoia, Kapok, Ebony, Mahogany, Balsa, Willow, Walnut, Greenheart, Cherry, Mahoe, Poplar, Palm, Papaya, Pine, Plum, Maple, Citrus) | furniture-deco | yes | interior | the most realistic wood set: walnut, teak, ebony, mahogany, chestnut, maple are classic furniture woods. Seat, MapFrame, FancySign, FancyWorkbench, Clock and paintings use names without `Fst` |
| `BiblioWoodsNatura:BiblioWood{Bookcase,potshelf,shelf,rack,case0,label,desk,table,Seat,MapFrame,FancySign,FancyWorkbench,Clock,PaintingT0-T4}:0-12` | Natura-wood furniture (Bloodwood, Darkwood, Eucalyptus, Fusewood, Ghostwood, Hopseed, Maple, Amaranth, Redwood, Silverbell, Sakura, Tiger, Willow) | furniture-deco | partly | interior | Maple, Redwood, Sakura, Eucalyptus fit; Bloodwood, Ghostwood, Fusewood are nether woods |

BiblioWoods blocks use their own TE subclasses (`TEBookcase`, `TEDesk`, … in `jds/bibliowood/*/blocks/`), with the same NBT layout as BiblioCraft's (unverified field by field).

## Best variety pools

- **furniture-deco, offices**: desk (walnut, oak, dark oak, mahogany), seat with back, typewriter (black, gray, white), clock, clipboard, filing-like shelves, bookcases; iron lamp for desk light.
- **furniture-deco, libraries and apartments**: bookcase (several woods), table plus seats, gold lamp, painting frames, map frames, dinner plates on tables, cookie jar in kitchens.
- **furniture-deco, shops**: display case, shelf, fancy sign over the door, desk bell on the counter.
- **light, interiors**: gold and iron lamps (0-15 shades; warm whites and yellows first), lanterns hung from ceilings.
- **light, exteriors (old town)**: iron lantern on walls of `urban-brick` buildings.

## Worldgen notes

- **NBT is required for a good look**: facing (`*Angle` keys, one per TE class, listed above), seat backs, lamp style, books. Writing it is straightforward (all keys are ints, booleans and strings), except inventories, whose item stacks store numeric IDs that must be resolved per world at placement.
- **Cost**: every piece is a TE with a TESR. Seats, tables and clocks tick (`canUpdate` true). Keep to a few pieces per room and a budget per chunk (suggest 8 to 16), and prefer non-ticking pieces (bookcase, desk, shelf, case, lamp) for bulk.
- **Defaults** (TE created by `setBlock` with no NBT) are usable for symmetric pieces (tables, lamps standing upright, lanterns) and wrong for anything with a front (bookcases, desks, seats, signs, clocks all face angle 0).
- Ruins: furniture should be removed or scattered by damage, not left floating; BiblioCraft blocks have no support checks, so a floating desk stays floating.
- A server without BiblioCraft logs missing-block warnings and drops the TE data; every palette entry needs the fallback listed above.
