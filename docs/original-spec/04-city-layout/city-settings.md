# City-style layout settings

The city-style sub-objects that tune layout (buildings, streets, parks, corridors, spheres), the world style's multibuilding and city-sphere settings, and how they combine with profile values. This sheet covers **behavior**; field formats, inheritance between city styles and the asset catalog are in `05-assets`. Source: `worldgen/lost/regassets/data/{BuildingSettings, StreetSettings, ParkSettings, CorridorSettings, MultiSettings, SphereSettings, CitySphereSettings}.java`, `worldgen/lost/cityassets/EffectiveCitySettings.java` (out of scope, read for behavior), the sphere center part in `LostCityTerrainFeature` (`315-334`, out of scope), on `f18a094d`.

## 1. Identity

| Class | Asset location | Role |
|---|---|---|
| `BuildingSettings` | city style `buildingsettings` | Building chance override; floor and cellar constraints |
| `StreetSettings` | city style `streets` | Fountain and front chances; street, border and wall characters; street part sets per road class |
| `ParkSettings` | city style `parks` | Park chance; foliage, border, elevation, neighbor threshold; elevation and grass characters |
| `CorridorSettings` | city style `corridors` | Corridor chance; roof and glass characters |
| `SphereSettings` | city style `sphere` | Sphere shell characters (space landscape) |
| `MultiSettings` | world style `multisettings` | Multibuilding area and counts ([multibuildings.md](multibuildings.md)) |
| `CitySphereSettings` | world style `citysphere` | What the center chunk of a city sphere becomes |
| `EffectiveCitySettings` | (runtime) | City-style values resolved against profile fallbacks |
| `StreetParts` | inside `streets` | Part names for each street piece shape |

## 2. Inputs

The chunk's city style (after inheritance, `05-assets`) and governing profile.

## 3. Numbers

### Resolution (`EffectiveCitySettings.resolve`)

| Effective value | City-style field | Profile fallback (default) |
|---|---|---|
| `buildingChance` | `buildingsettings.buildingchance` | `BUILDING_CHANCE` (0.3) |
| `parkChance` | `parks.parkchance` | `PARK_CHANCE` (0.2) |
| `openLotParkChance` | `openlotparkchance` (city style) | `OPEN_LOT_PARK_CHANCE` (0.8) |
| `fountainChance` | `streets.fountainchance` | `FOUNTAIN_CHANCE` (0.05) |
| `frontChance` | `streets.frontchance` | `BUILDING_FRONTCHANCE` (0.2) |
| `corridorChance` | `corridors.corridorchance` | `CORRIDOR_CHANCE` (0.7) |
| `avoidFoliage` | `parks.avoidfoliage` | `AVOID_FOLIAGE` (false) |
| `parkBorder` | `parks.parkborder` | `PARK_BORDER` (true) |
| `parkElevation` | `parks.parkelevation` | `PARK_ELEVATION` (true) |
| `parkStreetThreshold` | `parks.parkstreetthreshold` | `PARK_STREET_THRESHOLD` (3) |
| `minFloorConstraint` | `buildingsettings.minfloors` | none (no constraint) |
| `maxFloorConstraint` | `buildingsettings.maxfloors` | none |
| `minCellarConstraint` | `buildingsettings.mincellars` | none |
| `maxCellarConstraint` | `buildingsettings.maxcellars` | none |

### Constraints

| Method | Effect |
|---|---|
| `constrainMinimumFloors(f)` | `max(f, minfloors)` when set |
| `constrainMaximumFloors(f)` | `min(f, maxfloors)` when set |
| `constrainMaximumCellars(c)` | `min(c, maxcellars)` when set, then `max(result, mincellars)` when set |

City-style floor and cellar values are constraints on the computed limits, never replacements ([building-info.md](building-info.md) § Floor and cellar limits). Building assets with `overrideFloors` bypass them.

### `MultiSettings` defaults

`areasize` 10, `minimum` 1, `maximum` 5, `correctstylefactor` 0.8, `attempts` 50 (code `DEFAULT`; the codec requires the first three).

### `StreetParts` defaults

| Key | Default part |
|---|---|
| `full` | `street_full` |
| `straight` | `street_straight` |
| `end` | `street_end` |
| `bend` | `street_bend` |
| `t` | `street_t` |
| `none` | `street_none` |
| `all` | `street_all` |
| `connector` | `street_large_connector` |
| `stair` | `street_stair` |

Each key takes a string or a list. By the chunk's planned road type (`LostCityTerrainFeature.java:1708-1712`): `PRIMARY` uses `largeparts`, `TERTIARY` uses `tertiaryparts` (falling back to `parts` when left at the default), everything else, including legacy streets, uses `parts`. Piece selection and placement are in `03-terrain`.

### `CitySphereSettings`

| Field | Values | Default | Effect |
|---|---|---|---|
| `centertype` | `default`, `street`, `building`, `normal` | `default` | City and building flags of the sphere's center chunk ([building-info.md](building-info.md) step 6) |
| `centerpart` | part name | none | Part generated at the sphere center chunk |
| `centerpartorigin` | `fixed`, `center`, `firstfloor`, `ground`, `top` | `top` | Base Y for the center part: 0, sphere center Y, city ground, profile ground level, or the top of the chunk (building roof or city ground) |
| `centerpartoffset` | int | 0 | Added to that Y |

The center part is generated with no rotation, with water-level hard-air handling (`03-terrain`).

## 4. Behavior

1. (server) `BuildingInfo` resolves `EffectiveCitySettings` once per chunk from its profile and its city style, and every chance in this area reads it.
2. (server) Characters (`street`, `streetbase`, `streetvariant`, `border`, `wall`, `elevation`, `grass`, `roof`, `glass`, sphere `inner`/`border`/`glass`) are palette characters resolved through the chunk's compiled palette at generation time.
3. (both) Codecs parse the fields; a single-character string becomes a `Character`.

Unused by generation: `streets.width` (`streetWidth`) is only exposed through the API (`ILostCityCityStyle.getStreetWidth`), which unboxes it and throws when no style in the inheritance chain sets it (CITY-37).

## 5. Determinism

No randomness. Pure resolution.

## 6. Data formats

All fields are optional (`null` means "inherit or fall back"), except `MultiSettings.areasize`, `minimum`, `maximum`.

| Object | Fields (type) |
|---|---|
| `buildingsettings` | `minfloors`, `mincellars`, `maxfloors`, `maxcellars` (int); `buildingchance` (float) |
| `streets` | `fountainchance`, `frontchance` (float); `width` (int); `street`, `streetbase`, `streetvariant`, `border`, `wall` (char); `parts`, `largeparts`, `tertiaryparts` (`StreetParts`) |
| `parks` | `parkchance` (float); `avoidfoliage`, `parkborder`, `parkelevation` (bool); `parkstreetthreshold` (int); `elevation`, `grass` (char) |
| `corridors` | `corridorchance` (float); `roof`, `glass` (char) |
| `sphere` | `inner`, `border`, `glass` (char) |
| `multisettings` | `areasize`, `minimum`, `maximum` (int, required); `correctstylefactor` (float, 0.8); `attempts` (int, 50) |
| `citysphere` | `centerpart` (string); `centertype` (string); `centerpartorigin` (string); `centerpartoffset` (int) |

Enum strings in `citysphere` are upper-cased with the default locale before `valueOf` (CITY-32); an unknown value throws. When encoding, `centerpartorigin` is written from `centertype` (CITY-31). No NBT.

## 7. Output

Chances, constraints and characters used by [building-info.md](building-info.md), [streets-parks-stairs.md](streets-parks-stairs.md), [corridors.md](corridors.md), [multibuildings.md](multibuildings.md) and `03-terrain`.

## 8. Relationships

- Owned by `CityStyle` and `WorldStyle` (`05-assets`).
- Read by every sheet in this area.

## 9. 1.20 dependencies

| Mechanism | Where | Why it matters for 1.7.10 |
|---|---|---|
| Mojang `Codec` / `RecordCodecBuilder`, `Optional` fields | all settings classes | No codecs in 1.7.10; parse JSON by hand (Gson ships with 1.7.10) |
| Java 16+ records and `List.of` | `MultiSettings`, `StreetParts`, `EffectiveCitySettings` | Not on Java 8 |

## 10. Bugs

CITY-31, CITY-32, CITY-37.

## Deviations in the new mod

See [new-spec 04-city-layout § Corridors, doors, predefined content, transforms, settings](../../new-spec/04-city-layout/index.md#corridors-doors-predefined-content-transforms-settings).
