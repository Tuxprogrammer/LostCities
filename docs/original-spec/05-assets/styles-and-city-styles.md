# Styles and city styles

A **style** is a recipe for a chunk's palette: groups of palettes, one picked from each group. A **city style** is the per-city configuration: which style to use, which buildings, multibuildings, parks, fountains, bridges, stairs and fronts are available with what weights, which palette characters to use for streets, borders, corridors, rails and spheres, and optional overrides of profile chances. City styles inherit from a parent. Which city style a city gets is decided by the world style and profile ([world-styles.md](world-styles.md), `02-profiles-dimensions`, `04-city-layout`).

| # | Field | Contents |
|---|---|---|
| 1 | **Identity** | `regassets/StyleRE`, `regassets/data/PaletteSelector`, `cityassets/Style`; `regassets/CityStyleRE` with `regassets/data/` `Selectors`, `ObjectSelector`, `BuildingSettings`, `StreetSettings`, `StreetParts`, `ParkSettings`, `CorridorSettings`, `RailSettings`, `SphereSettings`, `GeneralSettings`, `CityProfileOverrides`; `cityassets/CityStyle` (implements `api/ILostCityCityStyle`); `cityassets/EffectiveCitySettings` (resolution against the profile, behavior owned by `04-city-layout`). Profile options with city-style counterparts: `BUILDING_CHANCE`, `PARK_CHANCE`, `OPEN_LOT_PARK_CHANCE`, `FOUNTAIN_CHANCE`, `BUILDING_FRONTCHANCE`, `CORRIDOR_CHANCE`, `AVOID_FOLIAGE`, `PARK_BORDER`, `PARK_ELEVATION`, `PARK_STREET_THRESHOLD`. |
| 2 | **Inputs** | Style and city-style assets; a `Random` and the chunk coordinate for selector draws. |
| 3 | **Numbers** | Selector `factor` weights (float); distance limits in blocks; `openLotParkChance` limited to 0..1 by its codec; other chances unchecked. |
| 4 | **Behavior** | See [Style](#style), [City style inheritance](#city-style-inheritance) and [Selectors](#selectors). |
| 5 | **Determinism** | `Style.getRandomPalette` draws one `nextFloat()` per group from the building random. Selector draws use the caller's random (building random, or a bridge random for large bridges; `04-city-layout`). Inheritance is resolved once per compiled city style. |
| 6 | **Data formats** | [Style JSON](#style-json), [City style JSON](#city-style-json). |
| 7 | **Output** | A merged palette for the chunk; names of buildings, multibuildings and parts; palette characters and chances for streets, parks, corridors, rails, spheres; the set of stuff tags. |
| 8 | **Relationships** | Palettes ([palettes-and-variants.md](palettes-and-variants.md)); buildings and parts; `EffectiveCitySettings.resolve(profile, cityStyle)` (`04-city-layout`); street, park, corridor and bridge generation (`04`, `06`); stuff (`07`); city spheres (`03`). |
| 9 | **1.20 dependencies** | Codecs only (`Codec.floatRange`, `Codec.either` via `Tools.listOrStringList`). |
| 10 | **Bugs** | [ASSET-06](index.md#suspected-bugs), [ASSET-07](index.md#suspected-bugs), [ASSET-08](index.md#suspected-bugs), [ASSET-09](index.md#suspected-bugs), [ASSET-10](index.md#suspected-bugs), [ASSET-19](index.md#suspected-bugs), [ASSET-22](index.md#suspected-bugs), [ASSET-27](index.md#suspected-bugs), [ASSET-28](index.md#suspected-bugs). |

## Style JSON

| Field | Type | Default | Meaning | Validation |
|---|---|---|---|---|
| `randompalettes` | list of list of `{factor, palette}` | required | Groups of palette choices, applied in order | an empty group crashes at use ([ASSET-08](index.md#suspected-bugs)) |
| `factor` | float | required | Weight within the group | none |
| `palette` | string | required | Palette name | `getOrThrow` at use |

## Style

`Style.getRandomPalette(provider, random)` (server):

1. Start an empty palette named `__random__`.
2. For each group in order: total the factors; `r = random.nextFloat() · total`; walk the group subtracting factors and take the first palette where `r ≤ 0`; merge it into the result (its characters and damaged entries override earlier groups').
3. If no palette was taken (empty group, or float rounding leaves `r > 0`), `merge(null)` throws a `NullPointerException` ([ASSET-08](index.md#suspected-bugs)).

Chunks inside a city use the city style's `style`; chunks outside use the world style's `outsidestyle`.

## City style JSON

Top level:

| Field | Type | Default | Meaning | Inherited |
|---|---|---|---|---|
| `inherit` | string | none | Parent city style | n/a |
| `style` | string | none (from parent) | Style name for the chunk palette. With no value anywhere in the chain, the lookup gets null and throws | yes, if unset |
| `explosionchance` | float | none | Chance that an explosion counts for this city (`DamageArea`); unset means every explosion counts | yes, if unset |
| `stuff_tags` | list of string | none | Tags of stuff assets to generate here; `all` is always added | merged (union) |
| `bridgesupport` | string (char) | none | Palette character for bridge supports; falls back to the world style's | yes, if unset |
| `bridgesupportpart` | string | none | Part used for bridge supports; falls back to the world style's | yes, if unset |
| `profile_overrides` | object | none | `openLotParkChance`: float in 0..1 | yes, if unset |
| `buildingsettings` | object | none | [Building settings](#building-settings) | field by field |
| `streetblocks` | object | none | [Street settings](#street-settings) | field by field |
| `parkblocks` | object | none | [Park settings](#park-settings) | field by field, except `grass` |
| `corridorblocks` | object | none | `corridorchance` (float), `roof` (char), `glass` (char) | field by field |
| `railblocks` | object | none | `railmain` (char): rail bed block | yes |
| `sphereblocks` | object | none | `inner` (char), `border` (char), `glass` (char) for city spheres | field by field |
| `generalblocks` | object | none | `ironbars`, `glowstone`, `leaves`, `rubbledirt` (chars) | **no** ([ASSET-07](index.md#suspected-bugs)) |
| `selectors` | object | none | [Selectors](#selectors) | lists appended ([ASSET-06](index.md#suspected-bugs)) |

All character fields take a string and use its first char; an empty string crashes ([ASSET-23](index.md#suspected-bugs)).

### Building settings

| Field | Type | Meaning (applied as constraints in `EffectiveCitySettings`) |
|---|---|---|
| `minfloors` | int | Floors are raised to at least this |
| `maxfloors` | int | Floors are capped at this |
| `mincellars` | int | Cellars are raised to at least this (applied after the maximum) |
| `maxcellars` | int | Cellars are capped at this |
| `buildingchance` | float | Replaces the profile's `BUILDING_CHANCE` |

### Street settings

| Field | Type | Default | Meaning |
|---|---|---|---|
| `fountainchance` | float | profile | Replaces `FOUNTAIN_CHANCE` |
| `frontchance` | float | profile | Replaces `BUILDING_FRONTCHANCE` |
| `width` | int | none | Street width. Parsed and inherited but never read by generation; only `ILostCityCityStyle.getStreetWidth()` exposes it, and it unboxes null ([ASSET-27](index.md#suspected-bugs)) |
| `street` | char | none | Street surface |
| `streetbase` | char | none | Street foundation |
| `streetvariant` | char | none | Parsed and inherited, never read (*unused*) |
| `border` | char | none | Street border |
| `wall` | char | none | Wall along streets at a height change |
| `parts` | [street parts](#street-parts) | `street_*` defaults | Normal streets |
| `largeparts` | street parts | `street_*` defaults | Large (highway-grid) streets |
| `tertiaryparts` | street parts | falls back to `parts` | Minor streets; when still default after inheritance, `getTertiaryStreetParts()` returns `parts` |

### Street parts

Each field takes one part name or a list (a random one is picked per chunk). An omitted field takes the default name; an omitted object is the shared `DEFAULT` instance, which is what inheritance tests for.

| Field | Default part | Used for |
|---|---|---|
| `full` | `street_full` | full street square |
| `straight` | `street_straight` | two opposite connections |
| `end` | `street_end` | dead end |
| `bend` | `street_bend` | corner |
| `t` | `street_t` | T junction |
| `none` | `street_none` | no connections |
| `all` | `street_all` | crossing |
| `connector` | `street_large_connector` | minor-street connector |
| `stair` | `street_stair` | street slope |

### Park settings

| Field | Type | Meaning |
|---|---|---|
| `parkchance` | float | Replaces `PARK_CHANCE` |
| `avoidfoliage` | boolean | Replaces `AVOID_FOLIAGE` (saplings and flowers become air) |
| `parkborder` | boolean | Replaces `PARK_BORDER` |
| `parkelevation` | boolean | Replaces `PARK_ELEVATION` |
| `parkstreetthreshold` | int | Replaces `PARK_STREET_THRESHOLD` |
| `elevation` | char | Block under elevated parks |
| `grass` | char | Park grass block (default `minecraft:grass_block`); not inherited ([ASSET-07](index.md#suspected-bugs)) |

## Selectors

`selectors` has nine optional lists of `ObjectSelector`:

| Key | Picks | Used by |
|---|---|---|
| `buildings` | building names | single-chunk buildings |
| `multibuildings` | multibuilding names | multi-chunk areas (`MultiChunk`) |
| `parks` | part names | park chunks |
| `fountains` | part names | fountain chunks |
| `stairs` | part names | street stairs (null allowed) |
| `fronts` | part names | building fronts |
| `bridges` | part names | normal bridges (`getOrThrow`: must not be empty) |
| `largebridges` | part names | large bridges in the hierarchical street mode |
| `raildungeons` | part names | dungeons next to railways |

`ObjectSelector` fields:

| Field | Type | Default | Meaning |
|---|---|---|---|
| `factor` | float | required | Weight |
| `value` | string | required | Asset name |
| `minSpawnDistance` | int | 0 | Below this distance (blocks) from the world origin the weight is 0, or faded with `feather` |
| `maxSpawnDistance` | int | `Integer.MAX_VALUE` | Above this distance the weight is 0, or faded |
| `feather` | int | 0 | Width (blocks) of a linear fade outside the limits |

Draw (server): each selector's weight is `factor`, unless a limit is set: with `d²` = squared distance of the chunk's corner block from (0, 0) computed in 32-bit ints ([ASSET-09](index.md#suspected-bugs)), below `min` the weight is 0 without feather, else 0 below `min − feather` and `factor · (√d² − (min − feather)) / feather` in between; above `max` likewise mirrored. Then `Tools.getRandomFromList` (`r = nextFloat · total`, first entry where the running `r ≤ 0`, null when the list is empty). With all weights 0 it returns the first entry ([ASSET-10](index.md#suspected-bugs)). The encoder writes the distance fields as their defaults ([ASSET-19](index.md#suspected-bugs)).

`maySelectBuilding(ids)` (spawn prefilter, `01-core`) is true when any `buildings` value, resolved to a full id, is in `ids`.

## City style inheritance

`CityStyle.init(level)` (server), run the first time the asset is fetched:

1. If already initialized, or resolution has started (`resolveInherit`), return. Set `resolveInherit`.
2. If `inherit` is set, fetch the parent with `getOrThrow`, which initializes the parent first (so chains resolve root first).
3. Copy from the parent: `style`, `explosionchance`, street width, `parts`/`largeparts`/`tertiaryparts` (when still the default instance), the four building constraints, `buildingchance`, `parkchance`, `openLotParkChance`, `fountainchance`, `frontchance`, `corridorchance`, `parkelevation`, `avoidfoliage`, `parkborder`, `parkstreetthreshold`, `street`, `streetbase`, `streetvariant`, `elevation`, corridor `roof` and `glass`, `railmain`, `border`, `wall`, sphere `inner`/`border`/`glass`, `bridgesupport`, `bridgesupportpart`: each only if unset in the child.
4. **Append** every parent selector list to the child's, and add the parent's stuff tags. A child cannot remove or reweigh a parent's entries; an explicitly empty child list (`"multibuildings": []`) still ends up with the parent's entries, and an entry listed in both counts twice ([ASSET-06](index.md#suspected-bugs)).
5. `generalblocks` and `parkblocks.grass` are not copied ([ASSET-07](index.md#suspected-bugs)).
6. Set `initialized`. If step 2 throws (missing parent), `resolveInherit` stays set and later calls return the half-built style without retrying ([ASSET-28](index.md#suspected-bugs)). A cycle does not loop: the second visit returns at step 1 with whatever was copied so far.

## On 1.12

`upstream/1.12` city styles use the same groups (`style`, `inherit`, `explosionchance`, `buildingsettings`, `streetblocks` with `width`/`street`/`streetbase`/`streetvariant`/`border`/`wall`, `parkblocks` with `elevation`, `corridorblocks`, `railblocks`, `sphereblocks`) inside array files with `"type": "citystyle"` and a `"name"`. Its selectors are top-level lists (`buildings`, `multibuildings`, `parks`, `fountains`, `stairs`, `fronts`, `bridges`, `raildungeons`) of objects keyed by the singular name (`{"factor": 1, "building": "building1"}`). 1.20-only: `stuff_tags`, `profile_overrides`, `generalblocks`, `largebridges`, the park flags, `corridorchance`, `fountainchance`/`frontchance`, street `parts`/`largeparts`/`tertiaryparts`, bridge-support fields and the selector distance fields.
