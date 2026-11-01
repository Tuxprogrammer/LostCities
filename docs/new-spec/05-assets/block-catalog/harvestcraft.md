# Pam's HarvestCraft

Family D. Mod ID `harvestcraft`, jar `harvestcraft-1.3.15-GTNH.jar`. Source read: `mods/harvestcraft/com/pam/harvestcraft/BlockRegistry.java` (all registrations, lines 416 to 900), `BlockPamSink.java`, `BlockPamOven.java`, `BlockPamMarket.java`, `BlockPamCandleDeco.java`, `BlockPamFruit.java`, `BlockPamFruitingLog.java`, `BlockPamCuttingBoard.java`, `BlockPamPot.java`, `PamOtherRecipes.java`, `HarvestCraft.java` (tile entity names), `assets/harvestcraft/lang/en_US.lang`; pack config `config/harvestcraft.cfg` (`enablemeatGrinder=false`, `candlelightLevel=0.9375`). Sheet: `tools/block-contact-sheet/sheets/harvestcraft/decor.json` (preview `docs/previews/blocks/harvestcraft/decor.png`, local only).

## Summary

- **187 block registry names enabled** (188 registered by code; the meat grinder is off in the pack config). By group: 60 crops, 36 saplings, 36 fruit and fruiting-log blocks, 16 candles, 12 wild gardens, 10 cakes, 15 kitchen and farm devices, 2 salt blocks. Most have a single meta (crops and fruit use meta for growth).
- **About 45 useful entries**, all interior or garden dressing: 16 colored candles (light), 4 sinks, the oven, the market stall, cutting board and pot, 12 wild gardens as overgrown foliage, a few cakes as table dressing.
- The look is kitchen and farmhouse: the oven is a white enamel range with a black cooktop and window (`decor.png`), sinks are basins set in log, stone, clay and brick counters, the market is a wooden stall. All read well in a 20th-century kitchen or corner shop.
- **Most devices are tile entities with an inventory** (`BlockContainer`: oven, churn, quern, presser, apiary, market, traps). Sinks, cutting board, pot, candles, gardens, crops, fruit and cakes are plain blocks.
- Optional dependency: without HarvestCraft, candles fall back to `minecraft:torch`, the oven to `minecraft:furnace`, sinks to `minecraft:cauldron`, the market to `minecraft:crafting_table` or air, gardens to `minecraft:tallgrass` or `minecraft:red_flower`.

## Blocks

### Kitchen and shop dressing

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `harvestcraft:pamcandleDeco1` to `pamcandleDeco16`:1 to 5 | White, Orange, Magenta, Light Blue, Yellow, Lime, Pink, Dark Grey, Light Grey, Cyan, Purple, Blue, Brown, Green, Red, Black Candle | light, furniture-deco | partly | interior | Torch model (`BlockPamCandleDeco extends BlockTorch`); meta is the torch attachment. Light 14 (`setLightLevel(0.9375)` from the pack config). No tile entity, so a cheap colored light. Three candles per block on the sheet |
| `harvestcraft:sink`:0 to 3 | Sink (log, stone, clay, brick counter) | furniture-deco | yes | interior | Full-cube counter with a basin; meta is the counter material (`PamOtherRecipes`: 0 log, 1 stone, 2 clay, 3 brick). Right-click with a bucket gives a water bucket, an endless water source (`BlockPamSink.onBlockActivated`); harmless. Stone (1) and brick (3) suit city kitchens |
| `harvestcraft:oven`:2 to 5 | Oven | furniture-deco | yes | interior | White range, facing meta 2 to 5. Tile entity `PamOven` with an inventory; place it empty. `harvestcraft:ovenon` is the lit state (same look, lit window); generate the unlit block |
| `harvestcraft:market`:0 | Market | furniture-deco, special | partly | interior, rustic | Wooden stall with a produce top; tile entity `PamMarket`. Its container takes emeralds (`ContainerMarket`) to buy seeds, saplings and animals: a working shop, which may or may not be wanted in loot-balance terms (beta balance list) |
| `harvestcraft:cuttingboard`:0 | Cutting Board | furniture-deco | yes | interior | 1/16-high board on a counter; no tile entity |
| `harvestcraft:pot`:0 | Pot | furniture-deco | yes | interior | Small iron pot model; no tile entity |
| `harvestcraft:churn`, `quern`, `presser` (and `churnon`, `quernon`) | Churn, Quern, Presser | furniture-deco | partly | rustic | Farmhouse machines with inventories (`PamChurn`, `PamQuern`, `PamPresser`); a rural shop or barn at most |
| `harvestcraft:pamcheeseCake` and 9 other cakes:0 | Cheesecake, Carrot Cake and others | furniture-deco | partly | interior | Edible cakes; meta counts eaten slices (place 0). A bakery or kitchen table item; players will eat them |
| `harvestcraft:spamcompressedsaltBlockalt`:0 | Compressed Salt Block | wall-accent | partly | desert | Plain cream block; the registry name has the typo `spamcompressedsaltBlockalt`. Smooth sandstone or quartz do the same job |
| `harvestcraft:salt`:0 | Salt Ore | off-theme | no | | An ore block; never place |
| `harvestcraft:apiary`, `beehive`, `fishtrap`, `animaltrap` | Apiary, Beehive, Fish Trap, Animal Trap | off-theme | no | rustic | Farm devices; the traps fill with loot over time |

### Gardens, trees and crops

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `harvestcraft:berrygarden`, `desertgarden`, `grassgarden`, `gourdgarden`, `groundgarden`, `herbgarden`, `leafygarden`, `mushroomgarden`, `stalkgarden`, `textilegarden`, `tropicalgarden`, `watergarden`:0 | Berry, Desert, Grass, Gourd, Ground, Herb, Leafy, Mushroom, Stalk, Textile, Tropical, Water Garden | foliage | yes | overgrown, park-garden | Wild bushes on a cross model (`decor.png`). Plain blocks; harvesting drops seeds and produce. Good overgrowth in yards, parks and ruined lots. Gardens extend `BlockFlower` (`base/BlockGarden.java`): they need grass or dirt below and break otherwise; `desertgarden` needs sand, `watergarden` sits on water, `mushroomgarden` has its own soil rule |
| 60 `harvestcraft:pam<crop>Crop`:0 to 7 | Crops (blackberry to waterchestnut) | foliage | partly | park-garden | Growth stage in meta; need farmland. Community garden plots in cities, ruined or tended |
| 36 `harvestcraft:pam<fruit>Sapling`:0 | Fruit tree saplings | foliage | partly | park-garden | Grow into vanilla-leaf trees with hanging fruit |
| 33 `harvestcraft:pam<Fruit>`:0 to 2 | Hanging fruit (apple, cherry, lemon and others) | foliage | partly | park-garden | Hang under vanilla leaves (`BlockPamFruit.canBlockStay` checks `Blocks.leaves` above), meta 0 to 2 is ripeness |
| `harvestcraft:pamMaple`, `pamPaperbark`, `pamCinnamon` | Maple, Paperbark, Cinnamon Wood | trim-pillar | partly | rustic | Fruiting logs (rotated-pillar meta); vanilla logs do the job |

## Best variety pools

- **Lights (interior):** the 16 candles, especially white, light grey, red and yellow; free of tile entities, unlike Forestry's candle.
- **Kitchen dressing:** oven (one per kitchen), stone or brick sink, cutting board, pot on a counter.
- **Shop dressing:** market stall for grocers and street markets; cakes in bakeries.
- **Overgrowth:** the 12 wild gardens mixed with vanilla tall grass in yards, parks and rubble lots; crops in small fenced community gardens.

## Worldgen notes

- Oven, market, churn, quern and presser carry tile entities with inventories; write them empty. Their facing is block meta (`BlockPamOven.onBlockPlacedBy`: 2 to 5).
- Candles, sinks, cutting boards, pots, gardens, crops and cakes need no tile entity.
- Gardens and crops pop off without the right block below; place them after the ground layer, and crops on farmland.
- Do not place the salt ore, traps (they generate loot), the lit oven variant or the meat grinder (not registered).
- A server without HarvestCraft loses all of these; palettes need the vanilla fallbacks in the summary.
