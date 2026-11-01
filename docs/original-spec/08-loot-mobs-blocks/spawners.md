# Spawners

How spawner positions in parts become mob spawners and which mobs they get.

| # | Field | Contents |
|---|---|---|
| 1 | **Identity** | `LostCityTerrainFeature.handleSpawner` and the static `getRandomSpawnerMob`. Conditions `easymobs` and `hardmobs` ([conditions.md](conditions.md)). Palette characters `1` and `4` in `palettes/common.json` (block `minecraft:spawner`). Profile options `generateSpawners`, `buildingWithoutLootChance` (through `BuildingInfo.noLoot`). |
| 2 | **Inputs** | Palette entries with a non-empty `mob` condition; the chunk's `BuildingInfo`; the profile; the chunk's generation random; the part name. |
| 3 | **Numbers** | Mob probabilities: `easymobs` zombie, skeleton, spider 1/3 each; `hardmobs` blaze, witch 1/2 each. Spawner timing and range are vanilla defaults (Lost Cities writes none): 1.20.1 `BaseSpawner` uses initial delay 20 ticks, min delay 200 ticks, max delay 800 ticks, spawn count 4, max nearby entities 6, required player range 16 blocks, spawn range 4 blocks (*from vanilla knowledge, not from Lost Cities code*). |
| 4 | **Behavior** | See below. |
| 5 | **Determinism** | The mob is drawn from the chunk's generation random (`GenerationContext.random()`, seeded per chunk from the world seed, dimension id hash and chunk coordinates when generation opens), in the order parts and blocks are written. Deterministic for a fixed generator and asset set; any change to earlier draws in the chunk changes the mob. |
| 6 | **Data formats** | Block entity NBT attached to the chunk: `x`, `y`, `z` (int), `id` = `minecraft:mob_spawner`, `SpawnData` = `{ entity: { id: "<entity id>" } }` (the 1.20 `SpawnData` codec; no custom spawn rules). No `SpawnPotentials`, delays or ranges. |
| 7 | **Output** | A `minecraft:spawner` block with the chosen entity, or air. |
| 8 | **Relationships** | Called from part generation (03-terrain, 04-city-layout), including railway dungeon parts (06-highways-railways) and scattered structures (07-scattered). |
| 9 | **1.20 dependencies** | Namespaced entity ids; `SpawnData` codec encoded with `NbtOps`; block entity NBT stored on a proto-chunk (`setBlockEntityNbt`). |
| 10 | **Bugs** | `LOOT-01`, `LOOT-03` |

## Behavior detail

1. (server) While a part is written, a palette character with a non-empty `mob` field (and no `torch` or `loot` flag, which take precedence in that order) goes to `handleSpawner`.
2. (server) If `generateSpawners` is false or the chunk's `noLoot` flag is set, the block becomes air.
3. (server) Otherwise evaluate the named condition at the position ([conditions.md](conditions.md)) with the chunk's generation random. If no entry matches, throw `Condition '<name>' did not return a valid mob!`, aborting the chunk (`LOOT-03`). An unknown condition name throws `Can't find '<name>' in <registry>!`.
4. (server) Build the block entity tag (position, `minecraft:mob_spawner`, `SpawnData` with the entity id) and attach it to the chunk. The spawner block from the palette is written.

Probability a spawner position becomes a spawner, default profile: 0.8 in a single-chunk building, 1.0 in a multibuilding (`noLoot` is never set there, `LOOT-10`).

Unknown entity ids are not checked; vanilla would create a spawner with an unusable entry (*inferred*).

## Deviations in the new mod

To be decided in `docs/new-spec/08-loot-mobs-blocks` (vanilla spawners with NBT, [0004](../../design/0004-gtnh-layer-and-tier-ceiling.md)). None recorded here.
