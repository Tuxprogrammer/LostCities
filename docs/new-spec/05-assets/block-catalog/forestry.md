# Forestry

Family D. Mod ID `Forestry`, jar `Forestry-4.11.40.jar`. Source read: `mods/Forestry/forestry/arboriculture/blocks/` (`BlockRegistryArboriculture.java`, `BlockWood.java`, `BlockPlanks.java`, `BlockLog.java`, `BlockSlab.java`, `BlockArbStairs.java`, `BlockArbFence.java`), `arboriculture/tiles/TileWood.java`, `arboriculture/items/ItemBlockWood.java`, `api/arboriculture/EnumWoodType.java`, `core/blocks/` (`BlockRegistry.java`, `BlockRegistryCore.java`, `BlockSoil.java`, `BlockResourceOre.java`, `BlockResourceStorage.java`), `apiculture/blocks/` (`BlockRegistryApiculture.java`, `BlockCandle.java`, `BlockStump.java`), `apiculture/tiles/TileCandle.java`, `mail/blocks/BlockMailType.java`, `core/utils/StringUtil.java`, `plugins/PluginArboriculture.java`, `plugins/PluginApiculture.java`, `assets/forestry/lang/en_US.lang`; pack config `config/forestry/modules.cfg`. Sheets: `tools/block-contact-sheet/sheets/forestry/` (previews in `docs/previews/blocks/forestry/`, local only).

## Summary

- **33 block registry names** (plus about 10 fluid blocks from `PluginFluids`, not building material). Every module is on in the pack (`config/forestry/modules.cfg`). Registry names drop the `for.` prefix: `BlockRegistry.registerBlock` sets the unlocalized name `for.<name>` and registers `StringUtil.cleanBlockName`, which strips `tile.` and `.for.`, so the planks are `Forestry:planks`.
- **About 410 placeable block and wood-type combinations**, of which about **150 are useful**: the 29 woods as planks, logs, stairs, slabs and fences (fireproof set preferred), the mailbox and the candle.
- **The woods are the value.** 29 wood types with real color range: dark floors (Ebony, Wenge, Ipe, Walnut, Teak), warm floors and paneling (Mahogany, Cherry, Maple, Padauk, Pine, Lime), pale (Balsa, Poplar, Papaya), and a few odd colors (Acacia mint, Greenheart green, Mahoe pale blue, Citrus lime, Cocobolo red stripes) that suit nothing urban. Seen on `planks.png` and `logs.png`.
- **Every Forestry wood block is a tile entity.** Planks, logs, slabs, stairs and fences (normal and fireproof) all extend `ITileEntityProvider` and keep the wood type in `TileWood` (`forestry.Wood`), NBT `WT` = `EnumWoodType` ordinal. Block metadata is 0 for planks and fences (`ItemBlockWood.getMetadata` returns 0), the axis bits for logs, the facing for stairs and the top bit for slabs. `world.setBlock` creates a `TileWood` set to Larch; the writer must then set `WT`. A block without its tile entity renders as Larch and drops nothing (`TileWood.getDrops` returns an empty list when the tile is missing).
- **Fireproof variants look identical** and neither burn nor spread fire (`BlockWood.isFlammable`, `getFlammability` 0). Worldgen should prefer them: a city of flammable wood interiors next to lava pockets or lightning burns down.
- Optional dependency: when Forestry is absent, each wood falls back to the nearest vanilla plank by color (`minecraft:planks` : 0 to 5; dark woods to dark oak 5, reds to jungle 3, pales to birch 2), stairs to the matching vanilla stairs, fences to `minecraft:fence`. The mailbox falls back to air or a cobblestone wall post; candles to a torch.

## Blocks

Wood types (`EnumWoodType`, `WT` 0 to 28): 0 Larch, 1 Teak, 2 Acacia, 3 Lime, 4 Chestnut, 5 Wenge, 6 Baobab, 7 Sequoia, 8 Kapok, 9 Ebony, 10 Mahogany, 11 Balsa, 12 Willow, 13 Walnut, 14 Greenheart, 15 Cherry, 16 Mahoe, 17 Poplar, 18 Palm, 19 Papaya, 20 Pine, 21 Plum, 22 Maple, 23 Citrus, 24 Giganteum, 25 Ipe, 26 Padauk, 27 Cocobolo, 28 Zebrawood.

### Woods

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Forestry:planksFireproof`:0 + `WT` 0 to 28 | Fireproof <Wood> Planks | floor, ceiling, wall-accent | partly | interior, rustic | Interior floors and paneling. Best urban picks: Ebony 9, Wenge 5, Ipe 25, Walnut 13, Teak 1 (dark parquet); Mahogany 10, Cherry 15, Maple 22, Padauk 26 (warm); Zebrawood 28 (striped, reads as parquet); Balsa 11 (pale gray, weathered). Skip Acacia 2, Greenheart 14, Mahoe 16, Citrus 23, Cocobolo 27 (unnatural colors) |
| `Forestry:planks`:0 + `WT` 0 to 28 | <Wood> Planks | floor, ceiling | partly | interior, rustic | Same look as fireproof; flammable, so second choice |
| `Forestry:logsFireproof`:0/4/8 + `WT` | Fireproof <Wood> Wood | trim-pillar | partly | rustic | Meta is the axis (0 up, 4 east-west, 8 north-south). Bark and heart textures seen on `logs.png`; Poplar 17 has odd green bark patches |
| `Forestry:logs`:0/4/8 + `WT` | <Wood> Wood | trim-pillar | partly | rustic | Flammable twin |
| `Forestry:stairsFireproof`:0 to 7 + `WT` | Fireproof <Wood> Stairs | stairs-slab | partly | interior, rustic | Vanilla stair facing meta; wood in the tile entity |
| `Forestry:stairs`:0 to 7 + `WT` | <Wood> Stairs | stairs-slab | partly | interior, rustic | Flammable twin |
| `Forestry:slabsFireproof`:0, 8 + `WT`; `Forestry:slabsDoubleFireproof`:0 + `WT` | Fireproof <Wood> Slab | stairs-slab, floor | partly | interior | Meta 8 is the top half. The double slab drops two slabs |
| `Forestry:slabs`, `Forestry:slabsDouble` | <Wood> Slab | stairs-slab | partly | interior | Flammable twin |
| `Forestry:fencesFireproof`:0 + `WT` | Fireproof <Wood> Fence | fence-rail | partly | rustic, park-garden | No Forestry fence gates or doors exist; use vanilla gates |
| `Forestry:fences`:0 + `WT` | <Wood> Fence | fence-rail | partly | rustic | Flammable twin |
| `Forestry:leaves`, `Forestry:saplingGE`, `Forestry:pods` | Leaves, Sapling, Fruit pods | off-theme | no | park-garden | Genetic tree tile entities with a full genome; leaf textures are grayscale and tinted per species. Use vanilla leaves for park trees |

### Other blocks (`misc.png`)

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Forestry:mail`:0 | Mailbox | furniture-deco, special | yes | urban, interior | A blue box with a slot, seen on `misc.png`: reads as a street mailbox. Full-cube machine tile entity (`TileMailbox`); placing it empty is enough. Players can open it and it joins Forestry's postal network, which is harmless. One per block front at most |
| `Forestry:mail`:1, :2 | Trade Station, Stamp Collector | off-theme | no | industrial | Same blue box textures; trade stations need an owner and registration, so do not place them |
| `Forestry:candle`:1 to 5 | Candle | light, furniture-deco | partly | interior, ruin | Torch model (`BlockCandle extends BlockTorch`, meta is the torch attachment), light 14 when lit (`getLightValue`). Color and lit state in `TileCandle` (`forestry.Candle`): NBT `colour` (int RGB, white `0xffffff`), `lit` (boolean). Unlit candles give no light; set `lit` true for lit interiors |
| `Forestry:stump`:1 to 5 | Candle stump (unlit) | furniture-deco | partly | ruin | A spent candle on a torch model: burned-out dressing for ruins. No tile entity, no light. Lighting it with flint, flint and steel or a torch turns it into a lit white candle (`BlockStump.onBlockActivated`) |
| `Forestry:soil`:0, :1, :2 | Humus, Bog Earth, Peat | foliage | partly | park-garden, overgrown | Dark soils. Bog earth slowly turns into peat (random tick) |
| `Forestry:resourceStorage`:0 to 3 | Block of Apatite, Copper, Tin, Bronze | special | no | industrial | Metal storage blocks are loot, not walls; leave out (beta balance list) |
| `Forestry:resources`:0 to 2 | Apatite, Copper, Tin Ore | off-theme | no | | Ore blocks; never place (they confuse GT ore prospecting) |
| `Forestry:core`:0, :1 | Analyzer, Escritoire | off-theme | no | | TESR machines; the escritoire is a desk model but opens a research GUI |
| `Forestry:factory2`:0 to 2, `Forestry:factory`:0 to 7, `Forestry:engine`:0 to 4 | Fabricator, Raintank, Worktable; squeezer, still and others; engines | off-theme | no | industrial | Machines. The worktable has a crafting-table look and could stand in as furniture, but it is a machine tile entity |
| `Forestry:apiculture`:0, :2; `Forestry:apicultureChest`; `Forestry:alveary`:0 to 7 | Apiary, Bee House, Apiarist's Chest, Alveary parts | off-theme | no | rustic | Bee machines and a multiblock |
| `Forestry:beehives`:1 to 8 | Forest, Meadows, Modest, Tropical, Ender, Wintry, Marshy, Swarm Hive | foliage, off-theme | no | overgrown | Striped hive blocks; natural worldgen, not city material |
| `Forestry:ffarm`:0 to 5, `Forestry:arboriculture`, `Forestry:lepidopterology`, `Forestry:mushroom` | Multifarm parts, Arborist's Chest, Lepidopterist's Chest, Mushroom | off-theme | no | | Machines, chests, a crop |

## Best variety pools

- **Interior floors (dark):** fireproof planks Ebony, Wenge, Ipe, Walnut, Teak. They add five dark woods vanilla lacks (vanilla has only dark oak).
- **Interior floors and wainscoting (warm):** Mahogany, Cherry, Maple, Padauk, Pine, Lime, Zebrawood.
- **Pale floors and weathered interiors:** Balsa, Poplar.
- **Rustic trim pillars:** fireproof logs of the woods above; their bark reads as wood posts in cottages and old shops.
- **Street dressing:** the Mailbox, sparingly (one per few blocks), on sidewalks beside doors.
- **Lights:** lit Forestry candles in dyed colors on tables; the unlit stump in ruins.

## Worldgen notes

- Every wood block needs its `TileWood` with `WT`. That is one non-ticking tile entity per block (`TileWood.canUpdate` returns false). A plank floor in every room of a tall building multiplies tile entities fast: use Forestry woods for accents and single floors, not whole structures. Vanilla planks stay the bulk material.
- Placing a wood block through `world.setBlock` makes a Larch tile; set `WT` after placement or write the tile entity NBT `{id:"forestry.Wood", WT:<n>}` directly.
- Fireproof variants are the right default for generated buildings: no fire spread from lava, lightning or player accidents.
- Candles need `TileCandle` NBT for color and lit state (`forestry.Candle`); light depends on `lit`.
- Do not place the ore blocks, metal storage blocks, hives, machines, trade stations, leaves or saplings.
- A server without Forestry loses all of these; palettes need the vanilla fallbacks listed in the summary.
