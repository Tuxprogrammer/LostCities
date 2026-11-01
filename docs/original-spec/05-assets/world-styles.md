# World styles (format)

A world style is the top of the asset tree for one dimension: which city styles appear where, the style used outside cities, multibuilding and scattered-structure settings, city-sphere centers, highway, railway and monorail part names, and support characters. The profile names it (`worldStyle`, default `standard`). This sheet documents the JSON format and the compiled object; what the generators do with each setting is in `02-profiles-dimensions`, `04-city-layout`, `06-highways-railways` and `07-scattered`.

| # | Field | Contents |
|---|---|---|
| 1 | **Identity** | `regassets/WorldStyleRE`; `regassets/data/` `MultiSettings`, `WorldSettings`, `CitySphereSettings`, `ScatteredSettings`, `ScatteredReference`, `PartSelector`, `HighwayParts`, `MonorailParts`, `RailwayParts`, `CityStyleSelector`, `CityBiomeMultiplier`, `BiomeMatcher`; `cityassets/WorldStyle` (behavior owned by `02-profiles-dimensions`). |
| 2 | **Inputs** | The JSON; biome holders and biome tags for matchers. |
| 3 | **Numbers** | Defaults listed per field below. |
| 4 | **Behavior** | Compilation copies the fields; biome selectors become predicates. `getRandomCityStyle(provider, coord, random)` keeps the city styles whose matcher accepts the chunk's main biome and draws one by `factor` (`Tools.getRandomFromList`, null when none match). `getCityChanceMultiplier` returns the `multiplier` of the **first** matching `citybiomemultipliers` entry, else 1.0; `getMaximumCityChanceMultiplier` returns the largest multiplier, at least 1.0, or +∞ if any is not finite. |
| 5 | **Determinism** | No randomness in compilation; city-style choice uses the caller's random (`04-city-layout`). |
| 6 | **Data formats** | Below. |
| 7 | **Output** | A `WorldStyle` read by the dimension info and generators. |
| 8 | **Relationships** | Profiles (`02`), city detection and multibuildings (`04`), highways, railways, monorails, bridges (`06`), scattered structures (`07`), city spheres (`03`), vines (`ChunkFixer`, `03`). |
| 9 | **1.20 dependencies** | `Biome.LIST_CODEC` (biome ids and `#tags` as `HolderSet`s), `BlockState.CODEC` for vines, `StringRepresentable` enums. See the [index table](index.md#120-dependencies). |
| 10 | **Bugs** | [ASSET-19](index.md#suspected-bugs) (`centerpartorigin` encoder), [ASSET-25](index.md#suspected-bugs) (enum parsing throws), [ASSET-23](index.md#suspected-bugs) (empty chars), [ASSET-32](index.md#suspected-bugs) (empty part lists). |

## World style JSON

| Field | Type | Default | Meaning |
|---|---|---|---|
| `outsidestyle` | string | required | Style for chunks outside cities |
| `citystyles` | list of [city-style selectors](#city-style-selector) | required | City styles and where they may appear |
| `citybiomemultipliers` | list of `{multiplier, biomes}` | none | City-chance multiplier for chunks whose main biome matches; first match wins |
| `multisettings` | [multi settings](#multi-settings) | `areasize 10, minimum 1, maximum 5, correctstylefactor 0.8, attempts 50` | Multibuilding placement |
| `settings` | [world settings](#world-settings) | `ignore`, `1`, default vines | Railway avoidance and vines |
| `cityspheres` | [city-sphere settings](#city-sphere-settings) | none | Sphere centers (sphere landscapes only) |
| `scattered` | [scattered settings](#scattered-settings) | none | Scattered structures outside cities; none means no scattered structures |
| `parts` | [part selector](#part-selector) | all defaults | Highway, railway and monorail part names |
| `bridgesupport` | string (char) | none | Bridge support character when the city style has none |
| `highwaysupport` | string (char) | none | Highway support character |
| `bridgesupportpart` | string | none | Bridge support part when the city style has none |
| `highwaysupportpart` | string | none | Highway support part |

### City-style selector

| Field | Type | Default | Meaning |
|---|---|---|---|
| `factor` | float | required | Weight |
| `citystyle` | string | required | City style name |
| `biomes` | [biome matcher](#biome-matcher) | none (any biome) | Where it may be chosen |

### Biome matcher

| Field | Type | Meaning |
|---|---|---|
| `if_all` | list of biome lists | The biome must be in every listed set |
| `if_any` | list of biome lists | The biome must be in at least one set |
| `excluding` | list of biome lists | The biome must be in none |

Each element is a vanilla biome list: a biome id string (`"minecraft:desert"`), a tag (`"#minecraft:is_ocean"`) or a list of ids. Absent clauses are skipped. Unknown ids or tags fail decoding or match nothing, depending on vanilla's holder-set rules.

### Multi settings

| Field | Type | Default | Meaning |
|---|---|---|---|
| `areasize` | int | required (10 when the object is absent) | Size in chunks of the square areas in which multibuildings are planned |
| `minimum` | int | required (1) | Minimum multibuildings per area |
| `maximum` | int | required (5) | Maximum multibuildings per area |
| `correctstylefactor` | float | 0.8 | Weight factor for multibuildings that fit the area's city style |
| `attempts` | int | 50 | Placement attempts per area |

### World settings

| Field | Type | Default | Meaning |
|---|---|---|---|
| `railwayavoidance` | `ignore` or `block_railway` | required | Whether multibuildings avoid railway chunks |
| `railpartheight6` | int | 1 | Railway part height in floors of 6 blocks |
| `vinewest`, `vineeast`, `vinesouth`, `vinenorth` | block state object (`{"Name": ..., "Properties": {...}}`) | `minecraft:vine` with the one matching side true | Vine states `ChunkFixer` hangs on building walls that face a neighboring chunk (`03-terrain`) |

### City-sphere settings

| Field | Type | Default | Meaning | Validation |
|---|---|---|---|---|
| `centerpart` | string | none | Part placed at each sphere center | `getOrWarn` at use |
| `centertype` | string | `default` | `default`, `street`, `building` or `normal` (case-insensitive) | unknown value throws `IllegalArgumentException` during decoding ([ASSET-25](index.md#suspected-bugs)) |
| `centerpartorigin` | string | `top` | `fixed`, `center`, `firstfloor`, `ground`, `top`: the reference height for the center part | same; the encoder writes `centertype` here ([ASSET-19](index.md#suspected-bugs)) |
| `centerpartoffset` | int | 0 | Offset in blocks from the origin | none |

### Scattered settings

| Field | Type | Default | Meaning |
|---|---|---|---|
| `areasize` | int | required | Size in chunks of each area that may hold one scattered structure |
| `chance` | float | required | Chance that an area holds a structure |
| `weightnone` | int | required | Weight of choosing nothing |
| `list` | list of references | required | Candidates |

Each reference:

| Field | Type | Default | Meaning |
|---|---|---|---|
| `name` | string | required | Scattered asset name |
| `weight` | int | required | Weight (summed with the others into `totalweight`) |
| `nearhighway` | boolean | false | Must be placed next to a highway |
| `allowvoid` | boolean | false | May be placed over void (floating/space landscapes) |
| `biomes` | biome matcher | none (any) | Allowed biomes |
| `maxheightdiff` | int | none | Maximum terrain height difference under the structure, in blocks |

### Part selector

| Object | Field (JSON) | Default part name |
|---|---|---|
| `monorails` | `both`, `vertical`, `station` (single strings) | `monorails_both`, `monorails_vertical`, `monorails_station` |
| `highways` | `tunnel`, `open`, `bridge`, `tunnel_bi`, `open_bi`, `bridge_bi`, `tunnel_bend`, `open_bend`, `bridge_bend`, `tunnel_t`, `open_t`, `bridge_t` (string or list) | `highway_<same>` |
| `railways` | `stationunderground`, `stationopen`, `stationopenroof`, `stationundergroundstairs`, `stationstaircase`, `stationstaircasesurface`, `railshorizontal`, `railshorizontalend`, `railshorizontalwater`, `railsvertical`, `railsverticalwater`, `rails3split`, `railsbend`, `railsflat`, `railsdown1`, `railsdown2` (string or list) | `station_underground`, `station_open`, `station_openroof`, `station_underground_stairs`, `station_staircase`, `station_staircase_surface`, `rails_horizontal`, `rails_horizontal_end`, `rails_horizontal_water`, `rails_vertical`, `rails_vertical_water`, `rails_3split`, `rails_bend`, `rails_flat`, `rails_down1`, `rails_down2` |

Every default part exists in the bundled parts.

## On 1.12

`upstream/1.12` world styles (`"type": "worldstyle"`, with a `"name"`) have only `outsidestyle` and `citystyles` (`factor`, `citystyle`, `biomes` as a list of biome names). Everything else on this sheet is a 1.20 addition.

Lists in the part selector pick one entry per call with the chunk's generation random (`LostCityTerrainFeature.getRandomPart`); a one-element list draws nothing.
