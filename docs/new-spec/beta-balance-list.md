# Beta balance list

Balance waits for the beta phase ([0004](../design/0004-gtnh-layer-and-tier-ceiling.md)). Until then, content stays faithful to upstream within the EV ceiling, and anything that may need tuning is listed here for the beta pass.

| Item | Where it is decided | What to judge at beta |
|---|---|---|
| Blaze spawners in overworld cities | [loot-and-mobs.md](00-pack-integration/loot-and-mobs.md), decision log D20 | An early overworld source of blaze rods and Broken Blaze Spawners |
| Spawner density and GT's spawner hardness | [loot-and-mobs.md](00-pack-integration/loot-and-mobs.md), D20 | How hard a building is to move into; each city as a stock of Broken Spawners |
| Loot quantities and tier weighting: each category's entries, weights and stack ranges (`data/lostcitiesnh/loot/categories.json`), and the band splits (supplies 2 : LV 1 on every floor; MV 10 : HV 5 : EV 1 on high floors and deep cellars) | [loot-and-mobs.md](00-pack-integration/loot-and-mobs.md), D20, [0010](../design/0010-loot-tier-map.md) | How far city loot pushes early-game progression |
| City frequency and size near spawn, and the distance the near-spawn city option guarantees | [spawn.md](00-pack-integration/spawn.md), D21, D30 | How quickly a new player reaches a city and its loot |
| The `!` treasure mix (diamond 15, gold 20, emerald 15, iron 78 of 128, upstream's `common` palette) and its pack treasure | [block catalog § Treasure](05-assets/block-catalog/index.md#treasure), [block-mapping.md](00-pack-integration/block-mapping.md#vanilla-blocks-with-a-1710-equivalent) | How many storage blocks a city gives away, and how early |
| Floating-fragment numbers after explosions (`destroyLoneBlocksFactor` 0.05, `destroyOrMoveChance` 0.4, `destroySmallSectionsSize` 50, upstream 1.12's defaults) | [03-terrain](03-terrain/index.md#explosions-damage-debris-ruins-rubble-damage-ruins-rubblemd) (P03-11, D31) | How much of a damaged building is cleared or dropped, against how ruined it looks |
| Decorative metal flags and the per-building metal cap, with its two exceptions: upstream's cabin keeps its copper and the highway gas station and restaurant parking keep their 24 iron blocks while those flags are on | [block catalog](05-assets/block-catalog/index.md#decorative-metals-d29), D29, D31 | How many ingots a city gives away, above all steel and aluminum; whether any metal should default off; whether the two exceptions stay |
