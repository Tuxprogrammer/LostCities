# Chest loot

How loot chests are placed and given a loot table, and every pool and entry of the two Lost Cities loot tables.

| # | Field | Contents |
|---|---|---|
| 1 | **Identity** | `LostCityTerrainFeature.handleLoot` (queues a post-todo), `generateLoot` and the static `createLoot` (assign a loot table), `ChunkFixer.fix` (runs the post-todos), `BuildingInfo.noLoot`. Loot tables `data/lostcities/loot_tables/chests/lostcitychest.json` (`lostcities:chests/lostcitychest`) and `raildungeonchest.json` (`lostcities:chests/raildungeonchest`). Condition `chestloot` ([conditions.md](conditions.md)). Profile options `generateLoot`, `chestWithoutLootChance`, `buildingWithoutLootChance`. |
| 2 | **Inputs** | Palette entries with a `loot` condition (bundled: character `C`, `minecraft:chest[facing=north]`); the chunk's `BuildingInfo`; the profile; the chunk's generation random; the 1.20 vanilla loot tables the condition names. |
| 3 | **Numbers** | `chestWithoutLootChance` 0.2; `buildingWithoutLootChance` 0.2; loot table numbers below. |
| 4 | **Behavior** | See [Behavior detail](#behavior-detail). |
| 5 | **Determinism** | See [Determinism](#determinism). |
| 6 | **Data formats** | 1.20 loot table JSON (pools with `rolls`, `entries` of type `item` with `weight` and `set_count` functions). Block entity NBT: the loot table id and a loot seed, written by vanilla `setLootTable`. |
| 7 | **Output** | A chest block with a deferred loot table and seed, or an empty chest. Items are rolled by vanilla when a player first opens or breaks the chest. |
| 8 | **Relationships** | Part generation (03-terrain, 04-city-layout) places the chest and calls `handleLoot`; railway dungeons (06-highways-railways) and scattered structures (07-scattered) use the same path; `ChunkFixer` (03-terrain) runs the queued work; the API's `createLoot` is public static (01-core may call it). |
| 9 | **1.20 dependencies** | Loot table JSON and number providers; `RandomizableContainerBlockEntity.setLootTable` (deferred, seeded loot); 33 vanilla 1.20 chest loot tables; namespaced loot table ids; block entity NBT on a proto-chunk. |
| 10 | **Bugs** | `LOOT-01`, `LOOT-03`, `LOOT-04`, `LOOT-05`, `LOOT-06`, `LOOT-09`, `LOOT-10` |

## Behavior detail

1. (server) While a part is written into the chunk, a palette character whose palette info has a non-empty `loot` field is handled by `handleLoot`. The chest block itself is written like any other block (the palette's `facing=north` is not rotated with the part, `LOOT-09`).
2. (server) If the chunk's `BuildingInfo.noLoot` is set, nothing else happens: the chest stays empty. `noLoot` is rolled once per chunk as `random < buildingWithoutLootChance`, but only for chunks that are not part of a multibuilding; multibuilding chunks copy the top-left chunk's value, which is always false (`LOOT-10`). The same flag removes spawners ([spawners.md](spawners.md)).
3. (server) Otherwise a post-todo is queued for the chest position (keyed by position in a `HashMap`).
4. (server) After the chunk is written, `ChunkFixer.fix` reseeds the chunk's generation random with `worldSeed ^ chunkX × 341873128712 ^ chunkZ × 132897987541`, generates vines (which draw from it when `vineChance` ≥ 0.000001), then runs every post-todo in the map's iteration order.
5. (server) The chest post-todo: if the block at the position is no longer air (explosion damage may have removed it), set the chest block again (client update flag) and continue; if it is air, skip.
6. (server) If there is no block entity at the position, log `Error setting loot at <x>,<y>,<z>` and stop. If the block entity is not a randomizable container, stop silently.
7. (server) If `generateLoot` is false, stop: the chest stays empty.
8. (server) Draw `random.nextFloat()`; below `chestWithoutLootChance` (0.2), stop: the chest stays empty.
9. (server) Evaluate the chunk's condition (`chestloot` for the bundled palette) at the chest position ([conditions.md](conditions.md)). A `null` result is not checked (`LOOT-03`).
10. (server) Call vanilla `setLootTable(world, random, pos, table)`, which stores the loot table id and a seed drawn from the same random on the chest's block entity.
11. (server, later) When a player opens or breaks the chest, vanilla rolls the table with that seed and fills random slots.

Probability that a chest in a single-chunk building ends up with a loot table, default profile: (1 - 0.2 building) × (1 - 0.2 chest) = 0.64. In a multibuilding: 0.8.

## Loot tables

Both tables have one pool. `rolls` is a uniform integer from `min` to `max` inclusive (2, 3 or 4, each 1/3). Each roll picks one entry by weight. `set_count` sets a uniform integer count from `min` to `max` inclusive; a count of 0 yields no item.

### `lostcities:chests/lostcitychest`

Pool `lostcities:lostcitychest`, rolls 2–4, total weight 17.

| Entry | Weight | Chance per roll | Count | Expected per chest (3 rolls) |
|---|---|---|---|---|
| `minecraft:diamond_sword` | 2 | 11.76 % | 0–1 (no item half the time) | 0.18 |
| `minecraft:gold_ingot` | 5 | 29.41 % | 1–5 | 2.65 |
| `minecraft:diamond` | 5 | 29.41 % | 1–5 | 2.65 |
| `minecraft:emerald` | 5 | 29.41 % | 1–3 | 1.76 |

### `lostcities:chests/raildungeonchest`

Pool `lostcities:raildungeonchest`, rolls 2–4, total weight 22.

| Entry | Weight | Chance per roll | Count | Expected per chest (3 rolls) |
|---|---|---|---|---|
| `minecraft:diamond_axe` | 2 | 9.09 % | 0–1 | 0.14 |
| `minecraft:gold_ingot` | 5 | 22.73 % | 1–5 | 2.05 |
| `minecraft:iron_ingot` | 5 | 22.73 % | 3–10 | 4.43 |
| `minecraft:diamond` | 5 | 22.73 % | 2–9 | 3.75 |
| `minecraft:emerald` | 5 | 22.73 % | 2–5 | 2.39 |

Neither table has conditions, enchantment functions or a `type` field. Item stacks are plain (no NBT).

### Vanilla tables reached through `chestloot`

The condition hands out 32 vanilla 1.20 chest tables with factor 1 and `minecraft:chests/simple_dungeon` with factor 30 ([conditions.md](conditions.md#chestloot-used-by-palette-character-c)). Their contents are vanilla 1.20.1 data and are not reproduced here. `minecraft:chests/jungle_temple_dispenser` is a dispenser table (arrows only), so about one loot chest in 62 holds only arrows (`LOOT-06`).

## Determinism

- The loot table choice and the loot seed come from the chunk's generation random after `ChunkFixer.fix` reseeds it with the world seed and chunk coordinates.
- Before the chests, vine generation draws from the same random, so changing `vineChance` (or anything that changes which blocks get vines) changes every chest in the chunk (`LOOT-05`).
- Chests are processed in `HashMap<BlockPos, Runnable>` iteration order, together with other post-todos (POI blocks, command blocks). The order is stable for the same set of positions but is not a designed order, and any added or removed post-todo can reshuffle the draws (`LOOT-05`).
- The items themselves are rolled later from the stored seed, so they do not depend on when the chest is opened.

## On 1.12

`upstream/1.12` ships the identical `lostcitychest` table in `assets/lostcities/loot_tables/chests/lostcitychest.json`.

## Deviations in the new mod

To be decided in `docs/new-spec/08-loot-mobs-blocks` (pack loot at EV or below, [0004](../../design/0004-gtnh-layer-and-tier-ceiling.md)). None recorded here.
