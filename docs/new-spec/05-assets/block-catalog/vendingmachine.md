# VendingMachine

Family D. Mod ID `vendingmachine`, jar `vendingmachine-0.4.103.jar`. Source read: `mods/vendingmachine/com/cubefury/vendingmachine/VendingMachine.java` (L68-L79, L110), `blocks/VendingMachineBlocks.java`, `blocks/VendingMachineCasing.java`, `blocks/MTEVendingMachine.java` (L77-L78, L318-L325), `items/VMItems.java`; GT's `gregtech/api/items/GTGenericBlock.java` (L15-L16). Sheets: `tools/block-contact-sheet/sheets/vendingmachine/vending.json` (previews in `docs/previews/blocks/vendingmachine/`, local only).

## Summary

- **A real vending machine**, the most "20th-century city" prop in the pack, but it is a **GregTech multiblock**: a controller MetaTileEntity (`gregtech:gt.blockmachines` with MTE ID 2741) inside a 2-wide, 3-high, 1-deep frame of casings (`addShape("main", {{"cc","c~","cc"}})`, `MTEVendingMachine.java` L77-L78). It registers only when `gregtech_nh` is loaded (`VendingMachine.java` L74-L77), which it always is in the pack.
- **1 registry name of its own**: the casing, registered by GT's `GTGenericBlock` with `getUnlocalizedName()`, so the name is `vendingmachine:tile.casing` (`GTGenericBlock.java` L15-L16; verify in M02). The controller and the ME uplink hatch are GT MetaTileEntities, not blocks.
- The casing is a brushed-steel panel; the controller front shows a lit or dark vending face. Placed as a formed structure it reads exactly as a street-corner or lobby vending machine.
- **Placing it in worldgen means placing a GT MTE**: `gregtech:gt.blockmachines` with a `BaseMetaTileEntity` TE whose NBT carries `mID`=2741 and facing. That is family C territory (how GT machines are generated), and an unpowered, unconnected machine shows the "off" face, which suits a ruin.
- **Fallback when the mod is absent**: two `minecraft:iron_block` high with an item frame, or air.

## Blocks

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `gregtech:gt.blockmachines` + TE `mID` 2741 | Vending Machine (controller) | furniture-deco | yes | modern, interior | front face; the middle-right block of the 2x3 frame |
| `vendingmachine:tile.casing:0` | Vending Machine Casing | furniture-deco | yes | modern | the other five blocks of the frame |
| `gregtech:gt.blockmachines` + TE `mID` from `ME_UPLINK_MTE_ID` | ME Vending Uplink Hatch | off-theme | no | | AE2 link |

## Best variety pools

- **furniture-deco (lobbies, subway platforms, gas stations, school corridors)**: one vending machine against a wall.

## Worldgen notes

- Depends on how this mod generates GT machines at all; coordinate with the family C catalog. If GT MTE placement is supported, this is a single 6-block prop with one TE.
- The MTE's structure check runs every 20 ticks (`STRUCTURE_CHECK_TICKS`); an incomplete frame just shows an unformed machine.
