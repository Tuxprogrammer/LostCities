# Conditions and condition contexts

A condition asset is a weighted list of string values, each guarded by optional tests on where it is being used. Palettes name conditions in `mob` (the value is an entity id for a spawner) and `loot` (the value is a loot-table id for a container). The same tests guard building part references ([buildings-and-multibuildings.md](buildings-and-multibuildings.md#part-references)).

| # | Field | Contents |
|---|---|---|
| 1 | **Identity** | `regassets/ConditionRE`, `regassets/data/ConditionPart` (one guarded value), `regassets/data/ConditionTest` (the shared test fields and the string-or-list codec), `cityassets/Condition`, `cityassets/ConditionContext` (the facts tested, and the parser that turns test fields into a predicate). Callers: `LostCityTerrainFeature.getRandomSpawnerMob`, `createLoot`; `Building.getRandomPart/getRandomPart2`; `Scattered` (`07`). |
| 2 | **Inputs** | The condition asset; a `ConditionContext` built by the caller; a random source. |
| 3 | **Numbers** | `factor` is a float weight (any scale). Floor numbering: 0 = ground floor, negative = cellars, `floor >= floors above ground` = top. Floors are 6 blocks. |
| 4 | **Behavior** | See [Evaluation](#evaluation). |
| 5 | **Determinism** | Values are filtered in file order, then one is drawn with `Tools.getRandomFromList(random, values, factor)`: `r = random.nextFloat() · total`, subtract weights in order, return the first entry where `r ≤ 0`, else the last entry. For mobs and loot the random is the chunk's generation random (`GenerationContext.current().random()`, seeded from world seed, dimension and chunk). |
| 6 | **Data formats** | [Condition JSON](#condition-json), [Condition tests](#condition-tests). |
| 7 | **Output** | One string, or null when no value's tests pass. |
| 8 | **Relationships** | Palettes ([palettes-and-variants.md](palettes-and-variants.md)), part placement ([parts-and-placement.md](parts-and-placement.md#loot)), loot tables in `08-loot-mobs-blocks`, part selection in `04-city-layout`. |
| 9 | **1.20 dependencies** | Loot-table ids (`minecraft:chests/...`), namespaced entity ids, biome ids from `Holder<Biome>`. See the [index table](index.md#120-dependencies). |
| 10 | **Bugs** | [ASSET-04](index.md#suspected-bugs), [ASSET-05](index.md#suspected-bugs), [ASSET-29](index.md#suspected-bugs), [ASSET-33](index.md#suspected-bugs). |

## Condition JSON

| Field | Type | Default | Meaning | Validation |
|---|---|---|---|---|
| `values` | list of entries | required | The guarded values | none; an empty list always yields null |

Each entry:

| Field | Type | Default | Meaning |
|---|---|---|---|
| `factor` | float | required | Weight relative to the other entries that pass |
| `value` | string | required | The result: an entity id for `mob`, a loot-table id for `loot` |
| any [condition test](#condition-tests) | | none | Guards; all present must hold |

Example: `{"factor": 20, "value": "lostcities:chests/raildungeonchest", "inpart": "rail_dungeon1"}`.

## Condition tests

Shared by condition entries and part references. Each present field adds an AND clause; no fields means "always".

| Field | Type | Holds when |
|---|---|---|
| `top` | boolean | `floor >= floorsAboveGround` equals the value |
| `ground` | boolean | `floor == 0` equals the value |
| `cellar` | boolean | `floor < 0` equals the value |
| `isbuilding` | boolean | the context's building is not `<none>` (part selection forces true) equals the value |
| `issphere` | boolean | the position is inside a city sphere equals the value |
| `floor` | int | `floor ==` value |
| `range` | string `"l1,l2"` | `l1 <= floor <= l2` (split on `,`; a non-integer or missing half throws "Bad range specification" when the asset is compiled) |
| `chunkx` | int | the chunk X coordinate equals the value |
| `chunkz` | int | the chunk Z coordinate equals the value |
| `inpart` | string or list of strings | the context's part name is in the set |
| `belowpart` | string or list | meant: the part below is in the set; actually tests the context's part name ([ASSET-04](index.md#suspected-bugs)) |
| `inbuilding` | string or list | the context's building name is in the set |
| `inbiome` | string or list | the biome id string (`namespace:path`) is in the set |

Names are compared as plain strings. Part and building names in a context are short names (`rail_dungeon1`, not `lostcities:rail_dungeon1`) for `lostcities` assets and full ids for other namespaces, so a test written with the other form never matches ([ASSET-33](index.md#suspected-bugs)). Biome tests use full ids and do not accept biome tags.

## Condition context

| Fact | Part selection (`BuildingInfo`) | Loot and spawners (`LostCityTerrainFeature`) |
|---|---|---|
| level | `cityLevel + i − cellars` | `(y − profile GROUNDLEVEL) / 6`, truncated toward zero |
| floor | `i − cellars` | `(y − cityGroundLevel) / 6`, truncated toward zero, so positions in the first cellar mostly report floor 0 ([ASSET-05](index.md#suspected-bugs)) |
| floors below ground | the building's cellars | the building's cellars |
| floors above ground | the building's floors | the building's floors |
| part | `<none>` for main parts; the chosen main part for `parts2` | the part being placed |
| below part | the previous floor's main part, or the current one for `parts2` | `<none>` |
| building | the building's short name | the building's short name, or `<none>` without a building |
| chunk | the chunk | the chunk |
| is building | always true | building is not `<none>` |
| is sphere | chunk center (Y 0) inside a city sphere | the block position inside a city sphere |
| biome | biome at the chunk center, Y 0 | biome at the block position |

`level` is stored but no test reads it.

## Evaluation

`Condition.getRandomValue(random, context)` (server):

1. Keep every entry whose test holds, in file order.
2. None: return null. The spawner caller then throws "Condition '<name>' did not return a valid mob!"; the loot caller passes null to `new ResourceLocation` and crashes ([ASSET-29](index.md#suspected-bugs)).
3. Otherwise draw by weight as described under Determinism and return the entry's `value`.

## On 1.12

`upstream/1.12` conditions use the same `values`/`factor`/`value` structure in array files (`"type": "condition"`), with tests parsed from the raw JSON object: `inpart`, `inbuilding` and `inbiome` accept a single string only, and there is no `belowpart`.
