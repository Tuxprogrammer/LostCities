# Loot and mob conditions

How a condition asset turns a chest or spawner position into a loot table id or an entity id, and what the three bundled conditions produce. The condition JSON format belongs to `05-assets`; this sheet documents evaluation and outcomes.

| # | Field | Contents |
|---|---|---|
| 1 | **Identity** | `worldgen/lost/cityassets/Condition` (compiled condition: list of (test, (factor, value))), `cityassets/ConditionContext` (the values a test can see and the test parser), `regassets/ConditionRE`, `regassets/data/ConditionPart` and `ConditionTest` (loader records, 05-assets), `BuildingInfo.ConditionTodo` (condition name, part name, building name), `varia/Tools.getRandomFromList` (weighted pick). Callers: `LostCityTerrainFeature.getRandomSpawnerMob` ([spawners.md](spawners.md)) and `createLoot` ([chest-loot.md](chest-loot.md)). Assets: `data/lostcities/lostcities/conditions/chestloot.json`, `easymobs.json`, `hardmobs.json`. Palette entries that use them: `palettes/common.json` characters `1`, `4`, `C`. |
| 2 | **Inputs** | The position being filled; the chunk's `BuildingInfo` (city ground level, cellars, floors, building name, chunk coordinate); the part name; the profile's `groundLevel`; the biome at the position; whether the position is inside a city sphere; the per-chunk generation random. |
| 3 | **Numbers** | Floor height `FLOORHEIGHT` = 6 blocks. Placeholder name `<none>`. Factors and values of the bundled conditions: below. |
| 4 | **Behavior** | See [Behavior detail](#behavior-detail). |
| 5 | **Determinism** | One `nextFloat()` per evaluation from the random the caller passes: the chunk's generation random (`GenerationContext.random()`). For spawners its state depends on everything drawn earlier while generating the chunk; for chests it is reseeded per chunk in `ChunkFixer.fix` and then shared with vine generation and the other chests ([chest-loot.md](chest-loot.md#determinism)). Entry order is JSON order. |
| 6 | **Data formats** | Condition JSON: `{ "values": [ { factor: float, value: string, top?, ground?, cellar?, isbuilding?, issphere?: bool, floor?, chunkx?, chunkz?: int, belowpart?, inpart?, inbuilding?, inbiome?: string or list of strings, range?: "<min>,<max>" } ] }`. Full field reference in 05-assets. |
| 7 | **Output** | A string (loot table id or entity id), or `null` when no entry matches. |
| 8 | **Relationships** | Palette entries with `loot` or `mob` (05-assets) mark positions; part generation (03-terrain/04-city-layout `generatePart`) reaches the callers; rail dungeon parts (06-highways-railways) and scattered structures (07-scattered) go through the same path. |
| 9 | **1.20 dependencies** | Namespaced ids for loot tables, entities and biomes; DFU codec loading of condition assets from a data-pack registry; `Holder<Biome>` lookup for `inbiome`. |
| 10 | **Bugs** | `LOOT-01`, `LOOT-02`, `LOOT-03`, `LOOT-04` |

## Behavior detail

### Context values (server)

For a block at Y `y` in a chunk with `BuildingInfo info`:

| Value | How it is computed |
|---|---|
| `level` | `(y - profile.groundLevel) / 6`, Java integer division (truncates toward zero). No test reads it. |
| `floor` | `(y - info.getCityGroundLevel()) / 6`, truncating toward zero (`LOOT-01`). City ground level is `groundLevel + 6 × cityLevel`. |
| `floorsBelowGround` | `info.cellars`. Not read by any test. |
| `floorsAboveGround` | `info.getNumFloors()`. Used by `top`. |
| `part` | Name of the part being generated (namespace `lostcities:` dropped), or `<none>`. |
| `belowPart` | Always `<none>`. |
| `building` | The chunk's building name if it has a building, else `<none>`. |
| chunk coordinate | `info.coord`. |
| `isSphere` | Whether the position lies inside a city sphere (03-terrain). |
| biome | Id of the biome the world reports at the position. |

Because `floor` truncates toward zero, positions just below the city ground count as floor 0:

| Offset below or above city ground (blocks) | `floor` |
|---|---|
| +6 … +11 | 1 |
| 0 … +5 | 0 |
| -1 … -5 | 0 |
| -6 … -11 | -1 (only -6 is in cellar 1; -7 … -11 are in cellar 2) |
| -12 … -17 | -2 |
| -18 … -23 | -3 |

A cellar occupies 6 blocks from `ground - 6k` to `ground - 6k + 5`. A chest or spawner standing one block above a cellar's floor slab therefore reports `-(k - 1)`: cellar 1 reads as the ground floor, cellar 3 as -2.

### Tests

Each entry's present fields are combined with AND; an entry with none always matches.

| Field | True when |
|---|---|
| `top` | `true`: `floor >= floorsAboveGround`; `false`: the opposite. |
| `ground` | `true`: `floor == 0`; `false`: the opposite. |
| `cellar` | `true`: `floor < 0`; `false`: the opposite. |
| `isbuilding` | `true`: building is not `<none>`; `false`: the opposite. |
| `issphere` | `true`: inside a sphere; `false`: the opposite. |
| `floor` | `floor` equals the value. |
| `chunkx`, `chunkz` | The chunk coordinate equals the value. |
| `belowpart` | The **current** part is in the set (`LOOT-02`). |
| `inpart` | The current part is in the set. |
| `inbuilding` | The building name is in the set. |
| `inbiome` | The biome id string is in the set. |
| `range` | `min <= floor <= max`. A malformed range throws `Bad range specification: <l1>,<l2>!` at load. |

### Selection

1. Collect `(factor, value)` for every matching entry, in JSON order.
2. If none match, return `null`.
3. Weighted pick ([Tools.getRandomFromList](#weighted-pick)) and return the value.

### Weighted pick

`r = random.nextFloat() × (sum of factors)`; walk the list subtracting each factor and return the first entry where `r <= 0`. If rounding leaves `r` slightly above 0 after the last entry, the result is `null` (`LOOT-04`). A zero-factor entry can only be chosen when `nextFloat()` returns exactly 0 and it is first.

## Bundled conditions

### `chestloot` (used by palette character `C`)

| # | Factor | Value (loot table) | Test |
|---|---|---|---|
| 1 | 8 | `lostcities:chests/lostcitychest` | `range` `4,100` (floor 4 or higher) |
| 2 | 8 | `lostcities:chests/lostcitychest` | `range` `-100,-3` (floor -3 or lower) |
| 3 | 20 | `lostcities:chests/raildungeonchest` | `inpart` `rail_dungeon1` |
| 4 | 20 | `lostcities:chests/raildungeonchest` | `inpart` `rail_dungeon2` |
| 5 | 1 | `minecraft:chests/buried_treasure` | always |
| 6 | 1 | `minecraft:chests/desert_pyramid` | always |
| 7 | 1 | `minecraft:chests/end_city_treasure` | always |
| 8 | 1 | `minecraft:chests/igloo_chest` | always |
| 9 | 1 | `minecraft:chests/jungle_temple` | always |
| 10 | 1 | `minecraft:chests/jungle_temple_dispenser` | always (`LOOT-06`) |
| 11 | 1 | `minecraft:chests/pillager_outpost` | always |
| 12 | 1 | `minecraft:chests/shipwreck_supply` | always |
| 13 | 1 | `minecraft:chests/shipwreck_treasure` | always |
| 14 | 30 | `minecraft:chests/simple_dungeon` | always |
| 15 | 1 | `minecraft:chests/spawn_bonus_chest` | always |
| 16 | 1 | `minecraft:chests/stronghold_corridor` | always |
| 17 | 1 | `minecraft:chests/stronghold_crossing` | always |
| 18 | 1 | `minecraft:chests/stronghold_library` | always |
| 19 | 1 | `minecraft:chests/underwater_ruin_big` | always |
| 20 | 1 | `minecraft:chests/underwater_ruin_small` | always |
| 21 | 1 | `minecraft:chests/woodland_mansion` | always |
| 22–37 | 1 each | `minecraft:chests/village/village_armorer`, `village_butcher`, `village_cartographer`, `village_desert_house`, `village_fisher`, `village_fletcher`, `village_mason`, `village_plains_house`, `village_savanna_house`, `village_shepherd`, `village_snowy_house`, `village_taiga_house`, `village_tannery`, `village_temple`, `village_toolsmith`, `village_weaponsmith` | always |

Unconditional weight: 32 entries of 1 plus `simple_dungeon` 30 = 62.

Resulting loot table probabilities (given the chest gets loot at all; see [chest-loot.md](chest-loot.md) for the chances before this step):

| Situation | Total | `lostcitychest` | `raildungeonchest` | `simple_dungeon` | each factor-1 table |
|---|---|---|---|---|---|
| Not a rail dungeon, floor -2 … 3 (includes cellars 1–3 above their floor slab) | 62 | 0 | 0 | 48.39 % | 1.61 % |
| Not a rail dungeon, floor ≥ 4 or ≤ -3 | 70 | 11.43 % | 0 | 42.86 % | 1.43 % |
| `rail_dungeon1` or `rail_dungeon2` part, floor -2 … 3 | 82 | 0 | 24.39 % | 36.59 % | 1.22 % |
| Rail dungeon part, floor ≥ 4 or ≤ -3 | 90 | 8.89 % | 22.22 % | 33.33 % | 1.11 % |

Which floor a rail dungeon chest reports depends on the railway's depth relative to the chunk's city ground level (06-highways-railways); *inferred* from code, not measured.

### `easymobs` (used by palette character `1`)

| Factor | Value | Test | Probability |
|---|---|---|---|
| 1 | `minecraft:zombie` | always | 1/3 |
| 1 | `minecraft:skeleton` | always | 1/3 |
| 1 | `minecraft:spider` | always | 1/3 |

### `hardmobs` (used by palette character `4`)

| Factor | Value | Test | Probability |
|---|---|---|---|
| 1 | `minecraft:blaze` | always | 1/2 |
| 1 | `minecraft:witch` | always | 1/2 |

### Palette entries that reference conditions

From `palettes/common.json` (05-assets owns palettes); no other bundled palette sets `loot` or `mob`:

| Character | Block | Field |
|---|---|---|
| `1` | `minecraft:spawner` | `mob: easymobs` |
| `4` | `minecraft:spawner` | `mob: hardmobs` |
| `C` | `minecraft:chest[facing=north]` | `loot: chestloot` |

## On 1.12

`upstream/1.12` keeps all conditions in one file, `assets/lostcities/citydata/conditions.json`. Its evaluation code was not compared.

## Deviations in the new mod

To be decided in `docs/new-spec/08-loot-mobs-blocks`. None recorded here.
