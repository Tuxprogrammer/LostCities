# City chunk terrain

What happens to the vanilla terrain inside a city chunk: the bedrock floor, high water, leveling to the city ground, room for buildings, the base under streets, and the street-edge borders. The building and street content itself is in [building and street writing](building-and-street-writing.md); landscape-specific variants are summarized in [city spheres and landscapes](city-spheres-and-landscapes.md).

## 1 · Identity

`worldgen/LostCityTerrainFeature`: `doCityChunk` (terrain part), `moveDown`, `moveUp`, `makeRoomForBuilding`, `clearToMax`, `clearRange`, `fillToBedrockStreetBlock`, `fillToGroundStreetBlock`, `fillMainStreetBlock`, `fillToGround`, `generateBorders`, `doBorder`, `isHigherThenNearbyStreetChunk`, `generateBorder`, `generateBorderSupport`, `setBlocksFromPalette`, `isEmpty`, `isFoliageOrEmpty`.

Profile options: `LANDSCAPE_TYPE`, `GROUNDLEVEL`, `SEALEVEL`, `BEDROCK_LAYER`, `BASE_BLOCK` (`baseBlock`), `LIQUID_BLOCK` (`liquidBlock`). City-style palette characters: `borderBlock`, `wallBlock`. Building: `fillerBlock`.

## 2 · Inputs

- `BuildingInfo`: `groundLevel` (`GROUNDLEVEL`, or the outside profile's for sphere-world outside chunks), `waterLevel` (`SEALEVEL`, or the generator's sea level when `SEALEVEL` is −1), `cityLevel`, `getCityGroundLevel()` = `groundLevel + 6 × cityLevel`, floors, cellars, `getBuildingBottomHeight()`, neighbors (`getXmin` …), `isCity` and `cityLevel` of neighbors.
- The chunk's heightmap and the neighbors' heightmaps.
- The chunk's actual blocks.

## 3 · Numbers

| Item | Value | Unit / notes |
|---|---|---|
| `GROUNDLEVEL` | 71 (default profile) | Y of city level 0 |
| `SEALEVEL` | −1 = use the generator's sea level (63 in vanilla) | Y |
| `BEDROCK_LAYER` | 1 | layers of bedrock from the minimum build height |
| `BASE_BLOCK` | `minecraft:stone` | |
| `LIQUID_BLOCK` | `minecraft:water` | |
| Leveling buffer | 6 | layers of surface material kept when terrain is lowered |
| Building bottom | `cityGround − 6 × cellars`, raised a floor at a time (dropping cellars) until above `minBuild + 2`; floors dropped until `cityGround + 6 × floors < maxBuild − 8` | computed by `BuildingInfo.getBuildingBottomHeight` |
| Clear above top floor | from the top floor's base to `min(maxBuild − 8, sampled height + 10)` | `DEFAULT`, `SPHERES` |
| Border fallback depth | 3 blocks under the city ground (`DEFAULT`, `SPHERES`, `FLOATING`), 2 (`CAVERN`, `CAVERNSPHERES`) | |
| Cavern building skirt | 10 blocks of border under the building's lowest level, edge columns only | |
| Floating building underside | fill base from the terrain up to the lowest level, but only reaching down to `lowestLevel − 6 − noise / 4` | noise: `bottomLayerNoise`, 4 octaves, scale 0.5 per block |
| Space underside | border on edge columns and base elsewhere, downward through air, at most to `max(1, lowestLevel − 10)` | |
| Space border | from `min(cityGround − 8, adjacent city ground or adjacent height − 2)` up, only when that Y > 5 | |
| Floating and cavern corner supports | wall block downward through air, then base downward through liquid, while Y > 1 | only when the chunk's sampled height > 1 |

## 4 · Behavior

### Terrain preparation in `doCityChunk` (server)

Runs before the building or street is written.

1. **Bedrock floor** (`DEFAULT` and `SPHERES`): for every column, bedrock from the minimum build height for `BEDROCK_LAYER` layers.
2. **High water** (`DEFAULT` and `SPHERES`): if `waterLevel > groundLevel`, liquid from `groundLevel` to `waterLevel − 1` in every column. (This is the "drowned city" setup: profiles with a sea level above the city ground.)
3. **Leveling** (`DEFAULT` and `SPHERES`): for each of the 256 columns, with `ground` = city ground Y:
   1. `moveDown(x, z, ground + 1, maxBuild)`. Scan down from `maxBuild − 1` through air, water, lava, climbable blocks and foliage (`lostcities:foliage` tag), remembering the highest foliage block. If the scan reaches `ground + 1` without meeting terrain, nothing happens; the scan stops there without looking at the block at `ground + 1`, so a column whose surface is exactly `ground + 1` is left as it is (`TERR-39`). Otherwise copy up to 6 blocks from the terrain surface downward (stopping at `ground + 1`), set everything from the highest foliage (or the surface) down to `ground + 1` to air, and write the copied blocks from `ground` downward. The column's terrain top is now at `ground`.
   2. If `moveDown` did nothing (terrain at or below `ground`), `moveUp(x, z, ground)`. Scan down from `ground` through the same "empty" set to the first solid block S. If S ≥ `ground`, nothing happens. Otherwise copy the column upward by `ground − S`: block at S goes to `ground`, S − 1 to `ground − 1`, and so on, stopping at the first air block or bedrock in the source (or at Y 0). The column's top is now at `ground`. A cave or any air gap ends the copy, so the column under the shifted part keeps its old blocks.
4. Post the cancelable `PreGenCityChunkEvent`; unless canceled, write the building (`generateBuilding`, which first calls `makeRoomForBuilding`) or the street (`generateStreet`, which ends with `generateBorders`). Then post `PostGenCityChunkEvent`.

Other landscapes skip steps 1–3 and rely on `makeRoomForBuilding` and `generateBorders`.

### `makeRoomForBuilding(info, lowestLevel)` (server)

`lowestLevel` is the building's bottom Y. "Top" below means `cityGround + 6 × floors`.

- **`FLOATING`:** sample the bottom-layer noise for the chunk. Per column: scan down from `maxBuild − 1` through air, but not below `max(minBuild, lowestLevel − 6 − (int)(noise / 4))`. If the found Y is above `minBuild + 1` and below `lowestLevel − 1`, fill base from just above it to `lowestLevel − 1` (so the island grows up to meet the building). Then clear the column from `lowestLevel` to the top (water below `waterLevel` when `waterLevel > groundLevel`).
- **`SPACE`:** `fillToGround` under the building (border on edge columns, base inside), then clear every column from `lowestLevel` to the top, never with water.
- **`CAVERN` and `CAVERNSPHERES`:** on edge columns, border from `lowestLevel − 10` to `lowestLevel − 1`. In any column whose block at `lowestLevel` is air, put the building's filler there. Then clear to the top (with the water rule).
- **`DEFAULT` and `SPHERES`:** on edge columns, border from `getMinHeightAt(x, z)` (or `lowestLevel − 3` when that is at or above `lowestLevel`) to `lowestLevel − 1`. Filler at `lowestLevel` where it is air. Then clear to the top (with the water rule).

### `clearRange(info, x, z, y1, y2, dowater)` (server)

With `dowater`: liquid from `y1` to `waterLevel − 1`, air from `waterLevel + 1` to `y2 − 1`; the block at `waterLevel` itself is left as it was (`TERR-06`). Without: air from `y1` to `y2 − 1`. A variant applies only to blocks matching a predicate (used by highways and railways).

### `clearToMax` (server)

Called in `DEFAULT` and `SPHERES` just before the top (roof) part is written: clear from the roof's base Y to `min(maxBuild − 8, sampledHeight + 10) − 1` in every column, never with water. Because the leveling has already flattened the terrain to the city ground, this mostly matters where leveling left blocks (*inferred*).

### Base under streets: `generateBorders` part 1 (server)

By landscape type:

| Type | Fill |
|---|---|
| `DEFAULT`, `SPHERES` | `fillToBedrockStreetBlock`: from `cityGround − 1` down, replace air, water and lava with base until the first other block or `minBuild + BEDROCK_LAYER`. Only the run of empty space directly under the street is filled; a cave lower down, under solid rock, stays. |
| `FLOATING` | `fillMainStreetBlock` offset 3: base at `cityGround − 2` and `cityGround − 1`, border at `cityGround − 3`. |
| `CAVERN`, `CAVERNSPHERES` | `fillMainStreetBlock` offset 2: base at `cityGround − 1`, border at `cityGround − 2`. |
| `SPACE` | `fillToGroundStreetBlock`: base from `cityGround − 1` down through air while Y > 1. |

### Street-edge borders: `generateBorders` part 2 (server)

For each side (`XMIN` x = 0, `XMAX` x = 15, `ZMIN` z = 0, `ZMAX` z = 15), draw a border along it when `doBorder` says so:

- the adjacent chunk is a city chunk that is lower: for a building, `adjacent.cityLevel + adjacent.floors < cityLevel`; for a street, `adjacent.cityLevel < cityLevel`; or
- the adjacent chunk is not a city chunk and `adjacent.cityLevel ≤ cityLevel` (non-city chunks still get a level from their height); or
- `SPACE` only: the adjacent non-city chunk's height is > 5 and `height − 4 < cityGround`.

Per border column, `borderTop` = `cityGround + 1`, or `cityGround` when the column is an opening into the neighbor (a neighbor's street slope part or stair part reaches this column, using the part's `z1`/`z2` metadata rotated to this side, or the neighbor has a bridge on this axis). The border is the city style's `borderBlock`:

| Type | Border column |
|---|---|
| `DEFAULT`, `SPHERES` | `y = getMinHeightAt(x, z)`. If `y < cityGround + 1`, border from `y − 1` to `borderTop − 1`; else from `cityGround − 3`. |
| `SPACE` | from the space border Y (Numbers) to `borderTop − 1`. |
| `FLOATING` | from `cityGround − 3`; corners also get a support column (offset 3). |
| `CAVERN`, `CAVERNSPHERES` | from `cityGround − 2`; corners get a support column (offset 2). |

When the street could hold a street or park (`canDoStreetOrPark`), the block at `cityGround + 1` on the border column becomes the city style's `wallBlock`, or air at an opening.

### Water, lava and caves

- Water, lava and air count as "empty" everywhere (`isEmpty`). Leveling and street fill treat them alike: water above the terrain is skipped, and empty space under streets becomes base.
- Water is created only by the high-water rule, by `clearRange` with water, by `HardAirSetting.WATERLEVEL` parts, and by explosion damage below the water level ([damage sheet](damage-ruins-rubble.md)). Nothing in this area writes lava.
- Caves carved by vanilla stay where they are not overwritten: under buildings below `lowestLevel` (except the filler at `lowestLevel` and border skirts), under streets below the first solid layer, and below a `moveUp` copy that stopped at an air gap.
- No fluid updates are scheduled for any block written here, so water next to removed blocks stays put (*inferred* from the direct section writes).

## 5 · Determinism

No random choices except the floating bottom-layer noise (world-seeded, see [concurrency sheet](concurrency-caches-determinism.md)). Results depend on the vanilla column and on heightmaps, which can be order-dependent (`TERR-10`).

## 6 · Data formats

City style characters `borderBlock`, `wallBlock`; building `fillerBlock`; street and stair part metadata `z1`, `z2` (05-assets). No NBT.

## 7 · Output

A column per (x, z) whose terrain ends exactly at the city ground (default and sphere worlds), with a stone base under streets down to the first solid layer, a bedrock floor, cleared space for the building, and border walls where the city meets lower ground.

## 8 · Relationships

Called from `doCityChunk` and `generateBuilding` / `generateStreet` ([pipeline](generation-pipeline.md)). `clearRange` and `fillToGround` are also called by 06-highways-railways (`Highways`, `Railways`, `Monorails`) and 07-scattered. `getBuildingBottomHeight` and the level numbers come from 04-city-layout.

## 9 · 1.20 dependencies

- Minimum build height −64 and maximum 320 (`getMinBuildHeight`, `getMaxBuildHeight`) for the bedrock floor, leveling scans and building bounds; several loops instead hard-code 0, 1 or 255 (`TERR-24`).
- Block tags `minecraft:climbable` and `lostcities:foliage` in `isFoliageOrEmpty`.
- Liquids as block states (`Blocks.WATER`, `Blocks.LAVA` identity checks).

## 10 · Bugs

`TERR-06`, `TERR-23`, `TERR-24`, `TERR-31`, `TERR-39`.

## On 1.12

In 1.12 a `DEFAULT` city chunk started from an **empty** primer, not from generated terrain (`LostCityChunkGenerator.getChunkPrimer`), so there was nothing to level: the generator wrote bedrock, high water and the city into blank space, and caves and canyons were carved afterwards (canyons optionally skipped in city chunks by `PREVENT_LAKES_RAVINES_IN_CITIES`; caves capped by `MAX_CAVE_HEIGHT`). Non-default landscapes started from their own generated primer, as in 1.20.

## Deviations in the new mod

None recorded yet.
