# Village Names

Family D. Mod ID `VillageNames`, jar `VillageNames-4.5.21-GTNH.jar`. Source read: `mods/VillageNames/astrotibs/villagenames/block/ModBlocksVN.java` (L46-L86), `block/BlockLunarin{Gold,Iron}.java`; pack config `config/VillageNames4/general.cfg` (L62-L71). Sheets: `tools/block-contact-sheet/sheets/villagenames/blocks.json` (previews in `docs/previews/blocks/villagenames/`, local only).

## Summary

- The mod can add prismarine, sea lanterns, concrete, concrete powder and glazed terracotta, but **in the pack almost all of it is off**: `Add Prismarine=false` (L68), and concrete, concrete powder and glazed terracotta register only when Et Futurum Requiem is absent (`ModBlocksVN.java` L70-L86; Et Futurum is in the pack). The textures for them ship in the jar but the blocks do not exist. **It does not fill the sea-lantern or prismarine gap from D19.**
- **3 registry names, 4 entries register**: `VillageNames:lunarinGoldBlock`, `VillageNames:lunarinIronBlock` (`Add Lunarin Blocks=true`, L62) and `VillageNames:sponge:0-1` (`Add Sponges=true`, L71). No TEs, no light, no gravity.
- Lunarin blocks are moon-village bricks: gold and iron metal bricks (`Material.iron`, hardness 3.3; gold 2.0). Shiny metal-plated brick reads as Art Deco cladding or a bank vault wall at best.
- **Fallback when the mod is absent**: lunarin gold to `minecraft:gold_block`, lunarin iron to `minecraft:iron_block` (both on the treasure balance list), sponge to `minecraft:sponge`.

## Blocks

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `VillageNames:lunarinIronBlock:0` | Lunarin Iron Brick | wall-accent | partly | modern, industrial | bright steel brick; lobbies, vaults, Art Deco trims |
| `VillageNames:lunarinGoldBlock:0` | Lunarin Gold Brick | wall-accent | partly | modern | gold brick; very loud, theaters and hotel lobbies only |
| `VillageNames:sponge:0-1` | Sponge, Wet Sponge | off-theme | no | | duplicates vanilla sponge |

## Best variety pools

- **wall-accent (lobbies, vaults)**: lunarin iron brick in small bands; lunarin gold as a rare accent.

## Worldgen notes

- Plain blocks, free to place. Neither is a GT ore host. Gold-looking blocks invite players to mine them for value: check what they drop (unverified) before using them in loot-sensitive places.
