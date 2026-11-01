# Carpenter's Blocks

Family D. Mod ID `CarpentersBlocks`, jar `CarpentersBlocks-3.7.3-GTNH.jar`. Source read: `mods/CarpentersBlocks/com/carpentersblocks/util/registry/BlockRegistry.java`, `tileentity/TEBase.java`, `util/Attribute.java`; pack config `config/CarpentersBlocks.cfg`. Sheets: `tools/block-contact-sheet/sheets/carpentersblocks/frames-and-overlays.json` (previews in `docs/previews/blocks/carpentersblocks/`, local only).

## Summary

- **18 registry names**, all enabled (`config/CarpentersBlocks.cfg` L4-L20): `blockCarpentersBlock`, `…Barrier`, `…Bed`, `…Button`, `…CollapsibleBlock`, `…DaylightSensor`, `…Door`, `…FlowerPot`, `…GarageDoor`, `…Gate`, `…Hatch`, `…Ladder`, `…Lever`, `…PressurePlate`, `…Safe`, `…Slope`, `…Stairs`, `…Torch`. Metadata carries nothing useful: shape, orientation, cover and design all live in the tile entity.
- **Every block is a TE (`TEBase`) that takes its whole look from a "cover" block stored in NBT.** Without a cover it renders as a bare oak-like frame (`general/full_frame.png`), which looks unfinished. With a cover it can be any full block in the pack: a sloped brick roof, a stone-brick stair corner, a quartz collapsible ramp.
- **Value for this mod**: slopes (`blockCarpentersSlope`) are the only true roof slopes in the pack outside ArchitectureCraft, and the door, hatch and garage-door glass inserts (French glass, tall glass, screen) give storefront and garage looks no vanilla door has. Everything else duplicates vanilla or Malisis.
- **NBT is name-recoverable.** Covers are stored as item stacks (`cbAttrList`, one `cbAttribute` byte per side) with a `cbUniqueId` string (`modid:name`) beside the numeric ID; when the numeric ID does not resolve, `Attribute.loadAttributeFromNBT` falls back to the name (`Attribute.java` L45-L60). Worldgen can therefore write covers by name. Shape and orientation are an int in `cbMetadata`; chisel designs in `cbChiselDesign_0..6`; owner in `cbOwner`.
- **Light**: covers that emit light make the block emit light (`Enable Illumination=true`); the torch is a light source. **No gravity.** Doors, hatches, gates, garage doors and the safe are interactive; the safe has an inventory and an owner.
- **Fallback when the mod is absent**: slope to the cover block's stairs, door to `minecraft:wooden_door`/`minecraft:iron_door`, hatch to `minecraft:trapdoor`, everything else to its cover block or air.

## Blocks

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `CarpentersBlocks:blockCarpentersSlope` | Carpenter's Slope | roof, stairs-slab | yes (with a cover) | urban-brick, urban-stone, rustic | wedges, corners, oblique slopes; the `cbMetadata` int encodes the slope type and facing (decode table in `util/slope/`) |
| `CarpentersBlocks:blockCarpentersStairs` | Carpenter's Stairs | stairs-slab | partly | any | duplicates vanilla and Et Futurum stairs; useful only for materials that have no stairs |
| `CarpentersBlocks:blockCarpentersBlock` | Carpenter's Block | stairs-slab, trim-pillar | partly | any | slabs and side covers of any block |
| `CarpentersBlocks:blockCarpentersCollapsibleBlock` | Collapsible Block | roof, rubble-ruin | partly | ruin | corner-height ramps; good for rubble mounds and drifts if covered with gravel or cobble |
| `CarpentersBlocks:blockCarpentersDoor` | Carpenter's Door | door-trap | yes | modern, interior | glass inserts (French, tall glass, screen) from the type in `cbMetadata` |
| `CarpentersBlocks:blockCarpentersGarageDoor` | Garage Door | door-trap | yes | modern, industrial | multi-block roll-up door; glass-row option |
| `CarpentersBlocks:blockCarpentersHatch` | Hatch | door-trap | partly | any | |
| `CarpentersBlocks:blockCarpentersGate` | Gate | fence-rail | partly | rustic | |
| `CarpentersBlocks:blockCarpentersBarrier` | Barrier (fence/wall) | fence-rail | partly | any | fence or wall in any material |
| `CarpentersBlocks:blockCarpentersLadder` | Ladder | special | partly | any | |
| `CarpentersBlocks:blockCarpentersTorch` | Torch | light | partly | rustic | lantern or torch head; weather effects on (`Enable Torch Weather Effects=true`) |
| `CarpentersBlocks:blockCarpentersFlowerPot` | Flower Pot | furniture-deco, foliage | yes | interior, park-garden | 16 colored pot designs; plant in NBT |
| `CarpentersBlocks:blockCarpentersSafe` | Safe | furniture-deco, special | yes | interior | banks, offices; owner and inventory in NBT; good loot container |
| `CarpentersBlocks:blockCarpentersBed` | Bed | furniture-deco | partly | interior | 6 quilt designs |
| `CarpentersBlocks:blockCarpentersButton`, `…Lever`, `…PressurePlate`, `…DaylightSensor` | Redstone parts | off-theme | no | | duplicates of vanilla |

## Best variety pools

- **roof**: slopes covered with the building's wall or roof block (brick, stone brick, terracotta, dark planks): gabled and hipped roofs on small buildings.
- **door-trap**: Carpenter's door with a French-glass or tall-glass insert for shop fronts; garage door for garages and loading bays.
- **furniture-deco**: safe in banks and offices; colored flower pots on window sills.
- **rubble-ruin**: collapsible blocks covered with gravel or cobblestone as debris slopes.

## Worldgen notes

- **Never place one without a cover**: the uncovered frame looks like a placeholder. Write `cbAttrList` with the cover's item stack plus `cbUniqueId` so it survives ID differences.
- `cbMetadata` encodes the shape; its bit layout differs per block type (slope, collapsible, door) and is not documented outside the block classes. M02 must decode it from `util/slope/`, `util/collapsible/` and the door handler before any asset uses these blocks.
- Cost: one non-ticking TE per block (the safe and daylight sensor excepted), custom ISBRH rendering (cheap compared with TESRs). Fine for roofs of a few dozen blocks; avoid whole facades.
- `Enable Ownership=true`: generated safes and doors without `cbOwner` are public, which is what a ruin wants.
