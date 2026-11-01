# Scattered structures, stuff and predefined cities (formats)

Four asset types whose behavior belongs to other areas: scattered structures and stuff to `07-scattered`, predefined cities and spheres to `04-city-layout` and `03-terrain`. This sheet documents their JSON formats, defaults and validation, and the two block and name matchers they use.

| # | Field | Contents |
|---|---|---|
| 1 | **Identity** | `regassets/ScatteredRE` → `cityassets/ScatteredBuilding` (with enums `TerrainHeight`, `TerrainFix`); `regassets/StuffSettingsRE` → `cityassets/StuffObject` (wraps the record), `regassets/data/BlockMatcher`, `regassets/data/ResourceLocationMatcher`, `regassets/data/BiomeMatcher`; `regassets/PredefinedCityRE` → `cityassets/PredefinedCity` with `regassets/data/PredefinedBuilding`, `PredefinedStreet`; `regassets/PredefinedSphereRE` → `cityassets/PredefinedSphere`. |
| 2 | **Inputs** | The JSON; block registry and block tags (block matchers); dimension ids (predefined assets). |
| 3 | **Numbers** | See the tables. Heights in stuff are absolute block-Y levels. |
| 4 | **Behavior** | Compilation copies the fields. Block matchers are compiled into predicates when decoded. Stuff objects are indexed by tag ([loading-and-lookup.md](loading-and-lookup.md#behavior-detail)). |
| 5 | **Determinism** | No randomness here. |
| 6 | **Data formats** | Below. |
| 7 | **Output** | Compiled assets for `07` and `04`. |
| 8 | **Relationships** | World styles reference scattered assets by name ([world-styles.md](world-styles.md#scattered-settings)); city styles select stuff by `stuff_tags`; predefined cities and spheres are iterated by `City` and `CitySphere`. |
| 9 | **1.20 dependencies** | `StringRepresentable` enums; block tags (`#forge:stone`); biome tags; dimension `ResourceKey`s. |
| 10 | **Bugs** | [ASSET-13](index.md#suspected-bugs) (unknown matcher blocks match air), [ASSET-21](index.md#suspected-bugs) (stuff counts and heights unchecked), [ASSET-33](index.md#suspected-bugs) (name forms). |

## Scattered JSON

| Field | Type | Default | Meaning |
|---|---|---|---|
| `buildings` | list of string | none | Candidate buildings for a single-chunk structure |
| `multibuilding` | string | none | A multibuilding for a multi-chunk structure (used instead of `buildings`) |
| `rotatable` | boolean | false | The structure may be placed in any of the four rotations |
| `clearhighwayrailing` | boolean | false | Remove highway railing where the structure meets a highway |
| `supportpart` | string | none | Part repeated under the structure as a support |
| `terrainheight` | `lowest`, `average`, `highest`, `ocean` | required | Which terrain height the structure sits on |
| `terrainfix` | `none`, `clear`, `repeatslice` | required | `none`: leave terrain; `clear`: clear from the base upwards; `repeatslice`: repeat the bottom slice downwards until it hits solid ground |
| `heightoffset` | int | 0 | Offset in blocks from the chosen height |

Unknown enum strings fail decoding. Neither `buildings` nor `multibuilding` is required by the codec.

## Stuff JSON

Stuff is a column of palette characters scattered into city chunks (chains and cobwebs in the bundled set).

| Field | Type | Default | Meaning | Validation |
|---|---|---|---|---|
| `tags` | list of string | empty | Tags under which this stuff is indexed; city styles select by tag. Without tags it is never used | none |
| `column` | string | required | Palette characters placed bottom to top | each must be defined in the chunk palette |
| `minheight` | int | none | Lowest block Y; unset: ground level, or the bottom of the cellars when `inbuilding` | none |
| `maxheight` | int | none | Highest block Y (exclusive); unset: `minheight + 20`, or the top floor + 10 when `inbuilding` | must exceed `minheight`, else `nextInt` throws ([ASSET-21](index.md#suspected-bugs)) |
| `mincount` | int | required | Minimum columns per chunk | |
| `maxcount` | int | required | Exclusive maximum: the count is `mincount + nextInt(maxcount − mincount)` | must exceed `mincount`, else crash ([ASSET-21](index.md#suspected-bugs)) |
| `attempts` | int | required | Tries per column | |
| `inbuilding` | boolean | none | true: only in building chunks; false: only in chunks without a building. **Unset: never generated** | none |
| `seesky` | boolean | none | If set, the base position's sky visibility must equal it | |
| `biomes` | biome matcher | any | Allowed main biomes of the chunk | |
| `blocks` | block matcher | any | Block below the column must match | |
| `upperblocks` | block matcher | any | Block just above the column must match | |
| `buildings` | name matcher | any | The chunk's building id must match | |

### Block matcher

| Field | Type | Meaning |
|---|---|---|
| `if_all` | list of string | The block must match every entry |
| `if_any` | list of string | The block must match at least one entry |
| `excluding` | list of string | The block must match none |

An entry is a block id (`minecraft:andesite`) or a block tag (`#forge:stone`). Ids are looked up once when the asset is decoded; an unknown id becomes `minecraft:air` ([ASSET-13](index.md#suspected-bugs)). Absent clauses are skipped; no clauses means "any block".

### Name matcher (`ResourceLocationMatcher`)

| Field | Type | Meaning |
|---|---|---|
| `if_any` | list of string | The id must be one of these |
| `excluding` | list of string | The id must not be one of these |

Names without a namespace become `lostcities:`. A null id never matches.

## Predefined city JSON

No bundled files. Folder `predefinedcities/`.

| Field | Type | Default | Meaning |
|---|---|---|---|
| `dimension` | string | required | Dimension id (`minecraft:overworld`) |
| `chunkx`, `chunkz` | int | required | Center chunk of the city |
| `radius` | int | required | City radius in blocks |
| `citystyle` | string | required | City style for the whole city |
| `buildings` | list | none | Fixed buildings: `building` (string, required), `chunkx`, `chunkz` (int, required, relative to the center), `multi` (boolean, default false; true: `building` names a multibuilding whose top-left chunk is this one), `preventruins` (boolean, false) |
| `streets` | list | none | Fixed street chunks: `chunkx`, `chunkz` (relative) |

## Predefined sphere JSON

No bundled files. Folder `predefinedspheres/`.

| Field | Type | Default | Meaning |
|---|---|---|---|
| `dimension` | string | required | Dimension id |
| `chunkx`, `chunkz` | int | required | Chunk that owns the sphere |
| `centerx`, `centerz` | int | required | Sphere center in block coordinates |
| `radius` | int | required | Sphere radius in blocks |

## On 1.12

`upstream/1.12` has predefined cities (`"type": "city"`) and spheres (`"type": "sphere"`) in the same array files. Scattered structures and stuff do not exist there.
