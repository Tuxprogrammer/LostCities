# JABBA (Just Another Better Barrel Attempt)

Family D. Mod ID `JABBA`, jar `JABBA-1.5.24.jar`. Source read: `mods/JABBA/mcp/mobius/betterbarrels/BetterBarrels.java` (L271), `common/blocks/TileEntityBarrel.java` (L175-L181, L540-L555). Sheets: `tools/block-contact-sheet/sheets/jabba/barrel.json` (previews in `docs/previews/blocks/jabba/`, local only).

## Summary

- **1 registry name, 1 entry**: `JABBA:barrel`. A TE (`TileEntityBarrel`) storing one item type; facing in NBT ints `orientation` and `rotation` (`ForgeDirection` ordinals), upgrades in NBT. Default (structural tier 0) is a dark wooden crate with cross-braced sides and a pale label face.
- It looks like a **shipping crate**, which is useful dressing for warehouses, docks, loading bays and shop store rooms; the label face shows the stored item when filled (TESR text and icon), blank when empty.
- No light, no gravity. The TE can tick (`canUpdate` depends on `ticking`, false unless a hopper-style upgrade is installed).
- **Fallback when the mod is absent**: `etfuturum:barrel` or `minecraft:chest`, or `minecraft:planks:5`.

## Blocks

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `JABBA:barrel:0` | Better Barrel | furniture-deco | yes | industrial | crate look; write `orientation` so the label faces the aisle, or leave default |

## Best variety pools

- **furniture-deco (warehouses, docks)**: stacks of empty barrels mixed with Storage Drawers and vanilla chests.

## Worldgen notes

- One TE per crate, non-ticking when empty; a TESR draws the label. Use in stacks of a few, not walls of hundreds.
