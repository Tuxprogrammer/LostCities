# Effective city settings

How a city style overrides or constrains profile options for the chunks it covers.

| # | Field | Contents |
|---|---|---|
| 1 | **Identity** | `worldgen/lost/cityassets/EffectiveCitySettings` (immutable record of resolved values and constraints). `worldgen/lost/regassets/data/CityProfileOverrides` (the city style's `profile_overrides` object; one field). City style asset fields are owned by 05-assets (`buildingsettings`, `parksettings` and similar in `CityStyleRE`). |
| 2 | **Inputs** | The chunk's profile (`BuildingInfo.getProfile`, which may be the outside profile) and the chunk's resolved city style (after city style `inherit` resolution, 05-assets). |
| 3 | **Numbers** | `profile_overrides.openLotParkChance`: float 0..1 (the codec rejects values outside this range). All other values come from the profile or city style. |
| 4 | **Behavior** | See below. |
| 5 | **Determinism** | None; pure function of profile and style. |
| 6 | **Data formats** | City style JSON: optional `profile_overrides: { openLotParkChance?: float 0..1 }`. The other overridable values are read from the city style's existing settings objects (05-assets). |
| 7 | **Output** | One `EffectiveCitySettings` per `BuildingInfo` (and one transient per building-chance check). |
| 8 | **Relationships** | Built by `BuildingInfo` (04-city-layout) at `BuildingInfo.java:527` (building chance check) and `:863` (per chunk). Read by `BuildingInfo` (building, park, fountain, front, corridor, floor and cellar decisions) and `LostCityTerrainFeature` (foliage, park border, park elevation). |
| 9 | **1.20 dependencies** | Java record (no records on the Java 8 API). |
| 10 | **Bugs** | none |

## Resolution

For each value: the city style's value if it is set (non-null after inheritance), else the profile's.

| Effective value | City style source | Profile fallback |
|---|---|---|
| `buildingChance` | building settings `buildingchance` | `buildingChance` |
| `parkChance` | park settings | `parkChance` |
| `openLotParkChance` | `profile_overrides.openLotParkChance` | `openLotParkChance` |
| `fountainChance` | street settings | `fountainChance` |
| `frontChance` | street settings | `buildingFrontChance` |
| `corridorChance` | corridor settings | `corridorChance` |
| `avoidFoliage` | park settings | `avoidFoliage` |
| `parkBorder` | park settings | `parkBorder` |
| `parkElevation` | park settings | `parkElevation` |
| `parkStreetThreshold` | park settings | `parkStreetThreshold` |

Floor and cellar counts are **constraints**, not replacements. The profile computes a count, and the city style bounds it:

| Method | Rule |
|---|---|
| `constrainMinimumFloors(n)` | `max(n, style minfloors)` when set, else `n`. |
| `constrainMaximumFloors(n)` | `min(n, style maxfloors)` when set, else `n`. |
| `constrainMaximumCellars(n)` | first `min(n, style maxcellars)` when set, then `max(result, style mincellars)` when set, so a style minimum wins over a style maximum. |

Order of application in `BuildingInfo` (04-city-layout), for each of minimum floors, maximum floors and maximum cellars:

1. Start from the profile: minimum floors `buildingMinFloors + 1`, maximum floors `buildingMaxFloors`, maximum cellars `buildingMaxCellars + cityLevel`.
2. If the building asset sets the value and its `overrideFloors` flag, return the building's value; the city style constraint is skipped.
3. Otherwise narrow by the building asset's own minimum or maximum where set.
4. Apply the city style constraint above.

The record stores the four constraints as nullable integers. The world style has no `profile_overrides` at the pin; upstream's `profile_plan.md` describes that as a plan only.

## Deviations in the new mod

See [new-spec 02-profiles-dimensions § Effective city settings](../../new-spec/02-profiles-dimensions/index.md#effective-city-settings).
