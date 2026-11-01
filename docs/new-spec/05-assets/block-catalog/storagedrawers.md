# Storage Drawers

Family D. Mod IDs `StorageDrawers` plus pack submods `StorageDrawersBop`, `StorageDrawersForestry`, `StorageDrawersNatura`, `StorageDrawersMisc` (and `StorageDrawersErebus`, which does not load), jar `StorageDrawers-2.2.31-GTNH.jar`. Source read: `mods/StorageDrawers/com/jaquadro/minecraft/storagedrawers/core/ModBlocks.java` (L87-L117), `block/BlockTrim.java`, `block/tile/TileEntityDrawers.java`, `core/api/PackFactory.java`, `api/pack/StandardDataResolver.java`, `packs/*/StorageDrawersPack.java`, `packs/*/core/{ModBlocks,DataResolver}.java`; pack config `config/StorageDrawers.cfg`. Sheets: `tools/block-contact-sheet/sheets/storagedrawers/` (`trim-vanilla-bop-natura`, `trim-forestry-misc`, `drawers`; previews in `docs/previews/blocks/storagedrawers/`, local only).

## Summary

- **Which packs load**: `autoEnablePacks=true` (`config/StorageDrawers.cfg` L113) makes each wood pack load exactly when its wood mod is present, ignoring the `enable*` lines below it. BoP, Forestry and Natura are in the pack, so those three load; Erebus is not, so it does not. The Misc pack follows `enableMisc=true` and loads **regardless of whether its source mods exist**: it registers ExtrabiomesXL, Highlands and Immersive Engineering wood blocks even though none of those mods is in the pack (`packs/misc/core/ModBlocks.java` registers unconditionally; only recipes need the mods).
- **51 registry names, about 507 block+meta entries**. Main mod: `fullDrawers1/2/4`, `halfDrawers2/4` (metas 0-5), `controller`, `controllerSlave`, `trim` (0-5), `framingTable`, `fullCustom1/2/4`, `halfCustom2/4`, `trimCustom` (15 names; `compDrawers` is off). Each pack: the five drawer sizes and a trim, per wood bank (BoP 6 names x 14 woods, Natura 6 x 13, Forestry 12 names over banks A and B for 29 woods, Misc 12 names over banks 0 and 1 for 21 woods).
- **The trims are the find: 83 plain wood trim blocks with no TE** (`BlockTrim extends Block`). Each is the drawer's side texture: planks inside a darker beveled frame, which reads as wainscoting, a paneled wall, a door surround or a column. 6 vanilla woods plus 77 modded woods is the largest set of "framed wood panel" blocks in the pack.
- **Drawers are TEs** (facing in NBT `Dir`, not meta; contents, upgrades, lock and owner in NBT). Empty, they look like wooden chests of drawers: good warehouse, shop-back-room and workshop dressing, poor in homes. Item rendering on the drawer face costs client time if they hold items.
- **Light, gravity**: none. Drawers are inventories (loot rules apply if filled).
- **Fallback when the mod is absent**: trim to the matching `minecraft:planks` meta (or `minecraft:log` for pillars); drawers to `minecraft:chest` or the planks.

## Blocks

### Trim (plain blocks, no TE)

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `StorageDrawers:trim:0-5` | Oak, Spruce, Birch, Jungle, Acacia, Dark Oak Trim | trim-pillar, wall-accent | yes | interior, rustic, urban-brick | texture `storagedrawers:drawers_<wood>_side`; oak and dark oak are the most useful |
| `StorageDrawersBop:trim:0-14` (meta 10 unused) | Sacred Oak, Cherry, Dark, Fir, Ethereal, Magic, Mangrove, Palm, Redwood, Willow, Pine, Hellbark, Jacaranda, Mahogany Trim | trim-pillar, wall-accent | partly | interior, rustic | Cherry, Fir, Redwood, Pine, Mahogany, Hellbark fit; Ethereal (teal), Magic (blue), Willow (green) are fantasy colors |
| `StorageDrawersNatura:trim:0-12` | Eucalyptus, Sakura, Ghostwood, Redwood, Bloodwood, Hopseed, Maple, Silverbell, Amaranth (purpleheart), Tigerwood, Willow, Darkwood, Fusewood Trim | trim-pillar, wall-accent | partly | interior, rustic | Ghostwood (near white) and Silverbell (pale) give painted-wood looks; Bloodwood (red), Darkwood (blue), Fusewood (teal), Amaranth (purple) are loud |
| `StorageDrawersForestry:trimA:0-15` | Larch, Teak, Acacia, Lime, Chestnut, Wenge, Baobab, Sequoia, Kapok, Ebony, Mahogany, Balsa, Willow, Walnut, Greenheart, Cherry Trim | trim-pillar, wall-accent | yes | interior, rustic | Teak, Wenge, Ebony, Walnut, Mahogany, Chestnut: the best dark furniture-wood paneling in the pack |
| `StorageDrawersForestry:trimB:0-12` | Mahoe, Poplar, Palm, Papaya, Pine, Plum, Maple, Citrus, Giganteum, Ipe, Padauk, Cocobolo, Zebrawood Trim | trim-pillar, wall-accent | partly | interior, rustic | Mahoe is blue-gray; Citrus and Papaya are yellow; Ipe, Padauk, Cocobolo, Zebrawood are good dark or red woods |
| `StorageDrawersMisc:trim_0:0-8` | ExtrabiomesXL Redwood, Fir, Acacia, Cypress, Japanese Maple, Rainbow Eucalyptus, Autumn, Bald Cypress, Sakura Trim | trim-pillar, wall-accent | partly | interior | registered although ExtrabiomesXL is absent; Rainbow Eucalyptus and Autumn are garish |
| `StorageDrawersMisc:trim_1:0-11` | Highlands Yellow, White, Red, Grey; Thaumcraft Greatwood, Silverwood; Witchery Rowan, Alder, Hawthorn; IE Treated Wood; Botania Livingwood, Dreamwood Trim | trim-pillar, wall-accent | partly | interior, industrial | Treated Wood (brown) reads as creosoted timber; Greatwood is very dark; Livingwood and Dreamwood are magic |

Pack trims use the texture path `<packmodid lowercase>:drawers_<wood>_side` (`StandardDataResolver.java` L98-L99).

### Drawers and controller (tile entities)

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `StorageDrawers:fullDrawers1:0-5`, `fullDrawers2`, `fullDrawers4`, `halfDrawers2`, `halfDrawers4` | Basic Drawers (1, 2, 4 fronts; full or half depth) | furniture-deco | partly | interior, industrial | NBT `Dir` (facing), `Cap`, slots, `Lock`, `Shr`, `Own`; empty by default |
| `StorageDrawersBop:fullDrawers1…halfDrawers4`, `StorageDrawersNatura:…`, `StorageDrawersForestry:fullDrawers1A/B…`, `StorageDrawersMisc:fullDrawers1_0/_1…` | Pack-wood drawers | furniture-deco | partly | interior | same, in each pack wood |
| `StorageDrawers:controller:0`, `controllerSlave:0` | Drawer Controller, Controller Slave | off-theme | no | industrial | stone-and-iron machine face |
| `StorageDrawers:framingTable`, `fullCustom1…trimCustom` | Framing Table, Framed Drawers and Trim | special | partly | interior | framed blocks take their materials from NBT; skip |

## Best variety pools

- **trim-pillar / wall-accent (interiors, wood paneling)**: `StorageDrawers:trim:0` and `:5`; `StorageDrawersForestry:trimA` metas 1 (teak), 5 (wenge), 9 (ebony), 10 (mahogany), 13 (walnut), 4 (chestnut); `trimB` 9 (ipe), 10 (padauk). Use as wainscot rows, door and window surrounds, ceiling beams.
- **trim-pillar (exteriors, rustic and old-town)**: oak, spruce, dark oak, BoP fir and redwood, Forestry larch and sequoia trims as timber framing between plaster or brick.
- **furniture-deco (warehouses, workshops, shop back rooms)**: oak and spruce `fullDrawers4` and `halfDrawers2`, empty, facing the aisle.

## Worldgen notes

- Trims are free (plain blocks). They are the reason to depend on this mod.
- Drawers: one non-ticking TE each (`TileEntityDrawers`), facing in NBT `Dir`; without it every drawer faces the default direction. Keep them to storage rooms.
- The Misc trims depend on `enableMisc=true` in the pack config; if the pack ever turns it off, those names vanish. Prefer main and Forestry trims in assets, Misc only as variety.
