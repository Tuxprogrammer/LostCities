# Cooking for Blockheads

Family D. Mod ID `cookingforblockheads`, jar `CookingForBlockheads-1.4.14-GTNH.jar`. Source read: `mods/CookingForBlockheads/net/blay09/mods/cookingforblockheads/CommonProxy.java` (registration, L47-L114), `CookingForBlockheads.java`, `block/*.java`, `tile/*.java`. No pack config file switches blocks off. Sheets: `tools/block-contact-sheet/sheets/cookingforblockheads/kitchen.json` (previews in `docs/previews/blocks/cookingforblockheads/`, local only).

## Summary

- **26 registry names, 26 block+meta entries** (facing aside), all useful for kitchens: `cookingtable`, `cookingoven`, `counter`, `countercorner`, `cabinet`, `cabinetcorner`, `fridge`, `sink`, `toolrack`, `toaster`, and **16 `<color>_kitchen_floor` blocks**.
- **Kitchen floors are plain blocks** (no TE, `Material.rock`, hardness 5; `block/BlockKitchenFloor.java`): a two-tone checkerboard of white and the dye color. They are among the best floor tiles in the pack for diners, kitchens, bathrooms, laundromats and shop floors.
- **The kitchen furniture is TE-based with model rendering**: facing is in block metadata (2-5, the furnace convention; `BlockOven.java` L131-L142, `BlockFridge.java` L145-L158, `BlockCounter.java` L102-L115); the TE holds a `Color` byte (counters, cabinets, sink, cooking table) and an inventory. A default TE (color 0, empty) looks complete, so facing in meta is enough for a good look.
- **Light**: none. **Gravity**: none. **Interaction**: GUIs; the oven burns fuel and ticks; the fridge and counters are inventories (loot containers if wanted); the sink gives water.
- **Fallback when the mod is absent**: kitchen floor to `minecraft:quartz_block` (white) or `minecraft:stained_hardened_clay` of the color; counter and cabinet to a wooden slab or `minecraft:crafting_table`; oven to `minecraft:furnace`; fridge to `minecraft:iron_block` (or air); sink to `minecraft:cauldron`.

## Blocks

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `cookingforblockheads:white_kitchen_floor:0` | White Kitchen Floor | floor | yes | interior, modern | white/light-gray checker; the default |
| `cookingforblockheads:black_kitchen_floor:0` | Black Kitchen Floor | floor | yes | interior, modern | classic black-and-white diner checker |
| `cookingforblockheads:{gray,light_gray}_kitchen_floor:0` | Gray, Light Gray Kitchen Floor | floor | yes | interior, modern, industrial | |
| `cookingforblockheads:{red,blue,light_blue,cyan,green,lime,yellow,orange,brown}_kitchen_floor:0` | Colored Kitchen Floors | floor | yes | interior | 1950s diner and bathroom colors; red and light blue fit best |
| `cookingforblockheads:{magenta,pink,purple}_kitchen_floor:0` | Magenta, Pink, Purple Kitchen Floor | floor | partly | interior | loud; use sparingly (salons, kids' rooms) |
| `cookingforblockheads:counter:2-5` | Kitchen Counter | furniture-deco | yes | interior | TE: `Color`, `IsFlipped`, inventory; counter tops connect visually |
| `cookingforblockheads:countercorner:2-5` | Kitchen Counter Corner | furniture-deco | yes | interior | |
| `cookingforblockheads:cabinet:2-5` | Kitchen Cabinet | furniture-deco | yes | interior | wall-hung upper cabinet |
| `cookingforblockheads:cabinetcorner:2-5` | Kitchen Cabinet Corner | furniture-deco | yes | interior | |
| `cookingforblockheads:fridge:2-5` | Fridge | furniture-deco | yes | interior | stacks two high into a tall fridge |
| `cookingforblockheads:cookingoven:2-5` | Oven | furniture-deco | yes | interior | ticking TE (fuel) |
| `cookingforblockheads:sink:2-5` | Sink | furniture-deco | yes | interior | kitchens, bathrooms |
| `cookingforblockheads:toaster:2-5` | Toaster | furniture-deco | yes | interior | sits on a counter |
| `cookingforblockheads:toolrack:2-5` | Tool Rack | furniture-deco | yes | interior | wall rack for utensils |
| `cookingforblockheads:cookingtable:2-5` | Cooking Table | furniture-deco | partly | interior | full block with a cutting board top; plain enough to be a butcher block |

## Best variety pools

- **floor (kitchens, diners, bathrooms)**: white, black, light gray, red, light blue kitchen floors; mix one color per room.
- **floor (shops, laundromats)**: gray and light gray kitchen floors.
- **furniture-deco (kitchens)**: counter runs with a sink, oven and fridge, cabinets above, a toaster on the counter.

## Worldgen notes

- Kitchen floors cost nothing (plain blocks) and are the headline item.
- Furniture: one TE per block; only the oven ticks. Rendering uses ISBRH and TESR models (`client/` renderers), moderate cost. A kitchen of 6 to 10 pieces per apartment is reasonable.
- Facing is metadata, so palettes can rotate kitchens like vanilla furnaces.
- Counters and fridges are inventories: either leave empty or route through the loot rules ([loot-and-mobs.md](../../00-pack-integration/loot-and-mobs.md)).
