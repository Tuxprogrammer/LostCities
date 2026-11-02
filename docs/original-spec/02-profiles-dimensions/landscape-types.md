# Landscape types

What the profile option `landscapeType` selects and where each value changes behavior. The detailed algorithms live in the owning areas; this sheet is the map.

| # | Field | Contents |
|---|---|---|
| 1 | **Identity** | Enum `config/LandscapeType`: `DEFAULT` (`default`), `FLOATING` (`floating`), `SPACE` (`space`), `CAVERN` (`cavern`), `SPHERES` (`spheres`), `CAVERNSPHERES` (`cavernspheres`). The JSON value is the lower-case name. Predicates on `LostCityProfile`: `isDefault`, `isFloating`, `isSpace`, `isCavern` (cavern or cavernspheres), `isVoidSpheres` (spheres only), `isSpheres` (spheres or cavernspheres). |
| 2 | **Inputs** | The profile's `landscapeType`. |
| 3 | **Numbers** | Six values. Hard-coded heights in consumers: void probe starts at Y 255 (floating); cavern heightmap ignores Y > 100 and Y below max(`groundLevel` - 20, 1), and records Y 100 as 127 (03-terrain); a building on floating terrain fills down to `lowestLevel - 6 - noise/4`. |
| 4 | **Behavior** | See the table below. |
| 5 | **Determinism** | None of its own. |
| 6 | **Data formats** | `lostcity.landscapeType` string in profile JSON. The declared allowed list omits `spheres` and `cavernspheres` (`PROF-15`), but `init` accepts all six. |
| 7 | **Output** | Branches in terrain, layout, spawn and sphere code. |
| 8 | **Relationships** | 03-terrain (terrain feature, heightmap, spheres), 04-city-layout (`BuildingInfo`, `City`), 06-highways-railways (`Highway`, `Railway`, monorails), 07-scattered (`Scattered`), 01-core (spawn selection), 09-client-ui (preview heightmap). |
| 9 | **1.20 dependencies** | The types assume the terrain already exists: in 1.20 Lost Cities is a feature that runs over another generator's terrain (vanilla noise or Lost Worlds' world types, which the profile warnings name). None of the landscape types generates islands, caverns or void itself. |
| 10 | **Bugs** | `PROF-15` |

## What each type changes

Every type at the pin works on terrain produced by another generator. The profile `warning` texts tell the player which Lost Worlds world type to pair it with ([builtin-profiles.md](builtin-profiles.md)).

| Behavior | default | floating | space | cavern | spheres | cavernspheres | Owner |
|---|---|---|---|---|---|---|---|
| City level from | terrain height | terrain height | sphere distance inside a sphere; outside profile's height rule outside | terrain height (same as floating) | sphere distance / outside profile (as space) | terrain height (as cavern) | 04-city-layout `BuildingInfo.computeCityLevel` |
| City spheres and outside profile | no | no | yes | no | yes | yes | 03-terrain `CitySphere`, `gen/Spheres` |
| Monorails generated with spheres | – | – | yes | – | no | no | 06-highways-railways |
| Area outside a sphere's inner radius but inside its outer radius cleared to liquid/air | – | yes (only matters with spheres, so effectively never) | yes | – | no | no | 03-terrain `Spheres` |
| Bedrock layer in city chunks (`bedrockLayer`) | yes | no | no | no | yes | no | 03-terrain |
| Terrain correction and surface leveling around cities | yes | no | no | no | yes | no | 03-terrain |
| Clear terrain above a building's top floor | yes | no | no | no | yes | no | 03-terrain |
| Street border fill | down to bedrock | 3-block border | down to ground level | 2-block border | down to bedrock | 2-block border | 03-terrain `generateBorders` |
| Building foundation | default fill | fit to island underside with noise, clear interior | fill to ground, clear interior | thin border shell and filler | default fill | thin border shell and filler | 03-terrain `makeRoomForBuilding` |
| Void-chunk detection (`cityAvoidVoid`, `isVoidChunk`) | no | yes | no | no | no | no | 03-terrain, 04-city-layout |
| Heightmap ignores cavern ceiling | no | no | no | yes | no | yes | 03-terrain `ChunkHeightmap` |
| Clear 15 blocks above highways | yes | yes | yes | no | yes | no | 06-highways-railways |
| Scattered structure fallback height when terrain search fails | min build height + 2 | same | same | `groundLevel` | same | `groundLevel` | 07-scattered |
| Scattered structures rejected over void unless allowed | no | yes | yes | no | yes | no | 07-scattered |
| Spawn point search always runs | only when a spawn option needs it | always | always | always | only when needed | always | 01-core `ForgeEventHandlers` |
| Fewer floors near sphere edges, fewer buildings at the rim | – | – | yes | – | yes | yes | 04-city-layout |
| Highway cache signature includes the type ordinal | yes | yes | yes | yes | yes | yes | 06-highways-railways |

Built-in profiles per type: `default` for most; `floating`; `space`; `cavern`; `biosphere` (spheres); `biosphere_caves` (cavernspheres).

## On 1.12

In `upstream/1.12` the landscape types are produced by Lost Cities' own chunk generator and world types (`dimensions/world/LostCityChunkGenerator`, `LostWorldType`), so `floating`, `space` and `cavern` generate their own terrain there. 1.12 has no `spheres` or `cavernspheres` types.

## Deviations in the new mod

See [new-spec 02-profiles-dimensions § Landscape types](../../new-spec/02-profiles-dimensions/index.md#landscape-types): only `default` is ported (decision log D12).
