# Iron Chests

Family D. Mod ID `IronChest`, jar `IronChest-6.1.13.jar`. Source read: `mods/IronChest/cpw/mods/ironchest/IronChest.java` (L114), `IronChestType.java` (L32-L56), `BlockIronChest.java`, `TileEntityIronChest.java` (L35, L187, L205); pack config `config/IronChest.cfg`. Sheets: `tools/block-contact-sheet/sheets/ironchest/chests.json` (previews in `docs/previews/blocks/ironchest/`, local only; the cubes are the item icons, in game a chest model).

## Summary

- **1 registry name, 9 enabled entries**: `IronChest:BlockIronChest`, meta = `IronChestType` ordinal: 0 iron, 1 gold, 2 diamond, 3 copper, 4 steel, 5 crystal, 6 obsidian, 7 dirt, 9 dark steel. Netherite (8) and silver (10) are off (`enableNetheriteChests=false`, `enableSilverChests=false`).
- **Loot containers, not decoration.** TE with a TESR chest model; facing in NBT byte `facing`; inventory in NBT. Larger inventories than vanilla, so they suit stores, warehouses and bank vaults where upstream puts `C` chests.
- Crystal chests render their contents through the glass (extra TESR cost).
- **Fallback when the mod is absent**: `minecraft:chest`.

## Blocks

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `IronChest:BlockIronChest:0` | Iron Chest | special, furniture-deco | yes | industrial | a plain steel chest: warehouses, factories |
| `IronChest:BlockIronChest:3` | Copper Chest | special | yes | industrial | |
| `IronChest:BlockIronChest:4` | Steel Chest | special | yes | industrial | |
| `IronChest:BlockIronChest:9` | Dark Steel Chest | special | partly | industrial | |
| `IronChest:BlockIronChest:1`, `:2`, `:6` | Gold, Diamond, Obsidian Chest | special | partly | | treasure look; bank vaults only, and the chest itself is valuable (balance) |
| `IronChest:BlockIronChest:5` | Crystal Chest | special | partly | modern | shows contents |
| `IronChest:BlockIronChest:7` | Dirt Chest 9000 | off-theme | no | | joke item |

## Best variety pools

- **special (loot containers in industrial buildings)**: iron, copper, steel chests replacing `minecraft:chest` at low weight.

## Worldgen notes

- One non-ticking TE (lid animation only when opened) with a TESR. Write `facing`, then route the inventory through the loot rules ([loot-and-mobs.md](../../00-pack-integration/loot-and-mobs.md)). The chest block itself is crafted from metal, so gold, diamond and obsidian chests are loot in their own right: keep them out or put them on the balance list.
