# Buildings and multibuildings

A building is a stack of parts chosen floor by floor by conditions. A multibuilding is a rectangular grid of buildings that occupy adjacent chunks. This sheet documents both formats and how a building picks its parts; where buildings are placed and how many floors they get is `04-city-layout`.

| # | Field | Contents |
|---|---|---|
| 1 | **Identity** | `regassets/BuildingRE`, `regassets/data/PartRef` (a part reference with conditions, extends `ConditionTest`), `cityassets/Building` (implements `api/ILostCityBuilding`); `regassets/MultiBuildingRE`, `cityassets/MultiBuilding` (implements `api/ILostCityMultiBuilding`). |
| 2 | **Inputs** | The JSON; for part choice, a `ConditionContext` per floor built by `BuildingInfo` and the building random. |
| 3 | **Numbers** | Floor height 6 blocks (`FLOORHEIGHT`). Floor counts `-1` mean "use the profile/city style". `preferslonely` 0..1 (0 default). Multibuilding grid `dimx` × `dimz` chunks. |
| 4 | **Behavior** | See [Part selection](#part-selection) and [Multibuildings](#multibuilding-json). |
| 5 | **Determinism** | `getRandomPart(random, context)` and `getRandomPart2` collect the matching references in file order and pick one uniformly with `random.nextInt(count)`. The random is the chunk's building random (`BuildingInfo.getBuildingRandom(chunkX, chunkZ, seed)`, a `QualityRandom` seeded `seed + chunkZ·341873128712 + chunkX·132897987541`), shared with the chunk's other draws, so part choice depends on the order of draws in `BuildingInfo` (`04-city-layout`). Duplicate references weigh double. |
| 6 | **Data formats** | [Building JSON](#building-json), [Part references](#part-references), [Multibuilding JSON](#multibuilding-json). |
| 7 | **Output** | Per chunk: the main part for each floor (`floorTypes`) and an optional overlay part (`floorTypes2`), plus filler and rubble characters and the building's local palette. |
| 8 | **Relationships** | Chosen by city styles' `buildings` and `multibuildings` selectors, scattered assets and predefined cities; floors and cellars constrained with the profile and city style in `04-city-layout` (`EffectiveCitySettings`); parts placed per [parts-and-placement.md](parts-and-placement.md). |
| 9 | **1.20 dependencies** | Codecs only (`Codec.either` for string-or-list fields). |
| 10 | **Bugs** | [ASSET-04](index.md#suspected-bugs) (`belowpart`), [ASSET-23](index.md#suspected-bugs) (empty `filler`), [ASSET-31](index.md#suspected-bugs) (multibuilding size versus grid). |

## Building JSON

| Field | Type | Default | Meaning | Validation |
|---|---|---|---|---|
| `filler` | string | required | Palette character used to fill and close areas around the building (usually its wall block) | first char used; empty string crashes |
| `rubble` | string | none | Palette character for rubble on top of ruined buildings; if the compiled palette lacks it, no rubble is placed | first char used |
| `refpalette` | string | none | Palette asset layered over the chunk palette for the whole building | resolved lazily with `getOrThrow`; ignored when `palette` is present |
| `palette` | object | none | Inline palette, compiled with the building | errors fail compilation |
| `minfloors` | int | -1 | Minimum floors above ground; -1 = no building-specific minimum | none |
| `maxfloors` | int | -1 | Maximum floors above ground; -1 = none | none |
| `mincellars` | int | -1 | Minimum cellars; -1 = none | none |
| `maxcellars` | int | -1 | Maximum cellars; -1 = none | none |
| `allowDoors` | boolean | true | Doors may be generated where the building meets a street or another building | none |
| `allowFillers` | boolean | true | Filler may be generated around cellars | none |
| `overrideFloors` | boolean | false | Use only this building's min/max floors and cellars, ignoring the profile's and city style's | none |
| `preferslonely` | float | 0.0 | How strongly the building wants empty neighbors. A single-chunk building slot next to it stays empty with this probability: for each of its four neighbors in turn, the slot rolls `random < neighbor's preferslonely` and is emptied on the first success (`04-city-layout`) | none |
| `parts` | list of [part references](#part-references) | required | Candidate main parts per floor | at least one must match every floor or `BuildingInfo` throws "Misconfiguration! No part condition matches…" |
| `parts2` | list of part references | none | Candidate overlay parts per floor, placed after the main part at the same height; no match is legal | none |

Note the camel case of `allowDoors`, `allowFillers` and `overrideFloors`; every other key is lower case.

## Part references

A part reference names a part and adds optional conditions. All conditions present must hold (AND). It shares its condition fields with condition entries ([conditions.md](conditions.md#condition-tests)).

| Field | Type | Default | Meaning |
|---|---|---|---|
| `part` | string | required | Part name ([name resolution](loading-and-lookup.md#behavior-detail)) |
| `top` | boolean | none | true: only the top floor (`floor >= floors above ground`); false: every other floor, cellars included |
| `ground` | boolean | none | true: only floor 0 |
| `cellar` | boolean | none | true: only floors below 0 |
| `isbuilding` | boolean | none | In building part selection the context always says "is a building" |
| `issphere` | boolean | none | The chunk center is inside a city sphere |
| `floor` | int | none | Exactly this floor (0 = ground, negative = cellars) |
| `range` | string `"l1,l2"` | none | Floor between `l1` and `l2` inclusive; a malformed range throws at compile time |
| `chunkx`, `chunkz` | int | none | Exactly this chunk coordinate |
| `inbuilding` | string or list | none | The building's short name is in the set |
| `inpart` | string or list | none | The context's part is in the set; for main parts the context part is `<none>`, so this never matches there |
| `belowpart` | string or list | none | Meant: the part on the floor below is in the set. Actually compares the context's *current* part ([ASSET-04](index.md#suspected-bugs)) |
| `inbiome` | string or list | none | The biome id at the chunk center (`ns:path` string) is in the set |

## Part selection

`BuildingInfo` (server, `04-city-layout`) calls this for floors `i = 0 … floors + cellars`, bottom (deepest cellar) to top:

1. Build a context: level = `cityLevel + i − cellars`, floor = `i − cellars`, floors below = `cellars`, floors above = `floors`, part `<none>`, below part = the previous floor's main part (`<none>` for the first), building = the building's short name, chunk coordinate; `isBuilding` true; sphere and biome at the chunk center (block Y 0).
2. `getRandomPart`: keep the `parts` entries whose conditions hold, pick one uniformly. None: throw.
3. Set "below part" to the chosen part (before step 4, so step 4 sees the current part as "below").
4. Build a second context with part = the chosen main part, and `getRandomPart2` over `parts2`. The result may be null (no overlay).

## Multibuilding JSON

| Field | Type | Default | Meaning | Validation |
|---|---|---|---|---|
| `dimx` | int | required | Width in chunks along X | not checked against the grid |
| `dimz` | int | required | Depth in chunks along Z | not checked against the grid |
| `buildings` | list of list of string | required | Grid of building names. The **outer** list is indexed by X, the inner by Z: `buildings[x][z]` is the building in chunk `(origin.x + x, origin.z + z)` | empty strings are allowed (skipped in the building set) |

`getBuildingSet()` returns every non-empty name, used by the spawn-building prefilter (`01-core`). Example: `{"dimx": 2, "dimz": 1, "buildings": [["highway_restaurant"], ["highway_restaurant_parking"]]}` puts the restaurant at x = 0 and the parking lot at x = 1.

## On 1.12

Same fields in `upstream/1.12` (inside array files with `"type": "building"` / `"multibuilding"` and a `"name"`); `filler` is checked explicitly there ("'filler' is required"). 1.12 reads `parts` conditions with the JSON-object variant of `ConditionContext.parseTest`, which on 1.20 still exists but is unused (dead code) and supports only single-string `inpart`, `inbuilding` and `inbiome`, and no `belowpart`.
