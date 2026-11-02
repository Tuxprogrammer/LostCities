# Building and street writing

How `LostCityTerrainFeature` writes the content of a city chunk once the terrain is prepared: the building floor by floor, or the street, park or fountain section, with front parts, vegetation, stairs and borders. *Which* building, street type, park or fountain a chunk gets is decided by 04-city-layout; the part and palette formats are 05-assets. This sheet covers the writing and every decision made inside the terrain feature.

## 1 · Identity

`worldgen/LostCityTerrainFeature`: `doCityChunk` (content part), `generateBuilding`, `generateStreet`, `generateNormalStreetSection`, `generateStreetSlopeSection`, `generateFullStreetSection`, `generateParkSection`, `generateMinorStreetConnectors`, `generateMinorStreetConnector`, `getStreetParts`, `hasStreetPartConnection`, `borderNeedsConnectionToAdjacentChunk`, `generateFrontPart`, `generateRandomVegetation`, `generateStreetDecorations`, `getTopLevel`, `getRandomPart`, `getRandomLeaf`, `getRandomDirt`, `getPossibleRandomDirts`.

City-style fields (05-assets): `streetBlock`, `parkElevationBlock`, `grassBlock`, `leavesBlock`, `rubbleDirtBlock`, `ironbarsBlock`, `borderBlock`, `wallBlock`, street part sets (`streetParts`, large/primary, tertiary): `none`, `end`, `straight`, `bend`, `t`, `all`, `full`, `stair`, `connector`. Effective city settings: `parkElevation`, `parkBorder`, `avoidFoliage`, `parkStreetThreshold`. Profile: `THICKNESS_OF_RANDOM_LEAFBLOCKS`, `CHANCE_OF_RANDOM_LEAFBLOCKS`, `RUIN_CHANCE`, `RUBBLELAYER`.

## 2 · Inputs

`BuildingInfo` of the chunk and neighbors: building, floors, cellars, floor parts (`getFloor`, `getFloorPart2`), `getAllowDoors`, `getAllowFillers`, `streetType`, `parkType`, `fountainType`, `frontType`, `stairType`, slope direction, planned road type, corridors, bridges, highway levels, rail info; the city style; the generation context random.

## 3 · Numbers

| Item | Value |
|---|---|
| Floor height | 6 blocks (`FLOORHEIGHT`); "we currently only support 6" |
| Floors written | `f` from `−cellars` to `floors` inclusive; `f = floors` is the roof part |
| Building top for spheres | `cityGround + 6 × floors` (`getTopLevel`) |
| Street surface | the street part is written at `cityGround`; decorations, parks and fronts at `cityGround + 1` (`+ 2` on an elevated park with `parkElevation`) |
| Default random leaves | 128 slots: 20 jungle, 20 spruce, 88 oak (all persistent) |
| Default random rubble ("dirt") | 128 slots: 20 mossy stone bricks, 40 mossy cobblestone, 68 moss blocks |
| `THICKNESS_OF_RANDOM_LEAFBLOCKS` | 2 (columns from the edge) |
| `CHANCE_OF_RANDOM_LEAFBLOCKS` | 0.1 |
| Vegetation cap per column | probability ≤ 0.8 (three sides only), at most 30 blocks |

## 4 · Behavior

### `doCityChunk` content order (server)

After terrain preparation ([city chunk terrain](city-chunk-terrain.md)):

1. `PreGenCityChunkEvent` (cancelable). Unless canceled: `generateBuilding` if the chunk has a building, else `generateStreet`.
2. `PostGenCityChunkEvent`.
3. If `RUIN_CHANCE > 0`: `generateRuins` ([damage sheet](damage-ruins-rubble.md)).
4. For street chunks with no highway on either axis, no surface rail, and no street slope: `generateStreetDecorations`.
5. If either highway level ≥ 0: `Highways.generateHighways` (06-highways-railways).
6. If `RUBBLELAYER` and (no building, or the building is ruined): `generateRubble` ([damage sheet](damage-ruins-rubble.md)).
7. `Stuff.generateStuff` (07-scattered).

### `generateBuilding` (server)

1. `lowestLevel` = `getBuildingBottomHeight()` (this can also reduce floors and cellars to fit the world; it returns `Integer.MIN_VALUE` for a degenerate building, which is not checked: `TERR-31`).
2. `makeRoomForBuilding` ([city chunk terrain](city-chunk-terrain.md#makeroomforbuildinginfo-lowestlevel-server)).
3. For `f` = −cellars … floors, at Y `height` starting from `lowestLevel` and rising 6 per floor:
   1. On the roof (`f = floors`) in `DEFAULT` and `SPHERES`, `clearToMax` first.
   2. Write the floor part with `HardAirSetting.AIR` (hard air becomes air).
   3. Remember the floor's second part, if any, with its Y.
   4. Below the roof, if the building allows doors: `Doors.generateDoors(height + 1, f)` (04-city-layout).
4. If there are cellars and the building allows fillers: replace the perimeter columns from `lowestLevel` up to `min(cityGround, neighbor's cityGround)` (inclusive) with the building's filler character, side by side (z = 0 and 15 for every x; x = 0 and 15 for z 1–14). This hides cellar windows underground.
5. If cellars ≥ 1: `Corridors.generateCorridorConnections` (06-highways-railways).
6. If any second parts were remembered: **commit the driver now** (`actuallyGenerate`), then write each second part at its Y with `HardAirSetting.AIR` on top of the committed chunk. These writes are committed with the rest at the end.

### `generateStreet` (server)

1. If the chunk has an X or Z corridor: `Corridors.generateCorridors` (06).
2. `canDoStreetOrPark` is true when neither highway level equals the city level, the rail type is not `STATION_SURFACE`, and it is not `STATION_EXTENSION_SURFACE` unless the rail level is below the city level.
3. If `canDoStreetOrPark`, with `height = cityGround`:
   1. **Elevated park** (`isElevatedParkSection`: street type `PARK` and at least `parkStreetThreshold` of the 8 neighbors are street or park sections): fill the whole 16 × 16 at `height` with the `parkElevationBlock`; if the city settings enable `parkElevation`, `height` += 1.
   2. Otherwise, **unless** the chunk is a hierarchical open lot: re-roll `info.streetType` with `new Random(chunkZ × 155557723 + chunkX × 45555558379).nextInt(0, 1)`, which is always `NORMAL` (`TERR-07`). The re-roll overwrites the cached `BuildingInfo` field.
   3. Write the section by type:
      - `NORMAL` without slope: `generateNormalStreetSection`; with a slope: `generateStreetSlopeSection`.
      - `FULL`: one random `full` part, `HardAirSetting.VOID`.
      - `PARK`: `generateParkSection`.
   4. `height` += 1.
   5. Without a slope, write the park part (type `PARK`) or the fountain part (if any) at `height` with `HardAirSetting.AIR`.
   6. Without a slope: `generateRandomVegetation(height)`, then front parts from the −X, −Z, +X and +Z neighbors with rotations none, 90°, 180°, 270°.
4. `generateBorders` ([city chunk terrain](city-chunk-terrain.md#base-under-streets-generateborders-part-1-server)).

### Street part selection (server)

- Part set: large street parts for a `PRIMARY` planned road, tertiary parts for `TERTIARY`, the normal set otherwise.
- A side "connects" when `BuildingInfo.hasRoadConnection(this, neighbor)` is true or the neighbor has a bridge on that axis; on a primary road only a connection to another primary road (or a bridge) counts, so a minor street does not turn the primary road's center line.
- By the number of connecting sides (rotations relative to the part as authored, which connects toward −X):

| Connections | Part list | Rotation |
|---|---|---|
| 0 | `none` | none |
| 1 | `end` | −X none; +X 180°; −Z 90°; +Z 270° |
| 2, opposite | `straight` | −X or +X present: none or 180°; else −Z 90° (both Z sides) |
| 2, corner | `bend` | −X−Z none; −X+Z 270°; +X−Z 90°; +X+Z 180° |
| 3 | `t` | missing −X: 90°; missing +X: 270°; missing −Z: 180°; missing +Z: none |
| 4 | `all` | none |

- A list with one entry is used directly; otherwise `getRandomPart` picks `nextInt(size)` from the context random.
- After a normal section on a primary road, for each side that has a road connection to a non-primary neighbor, a random `connector` part is overlaid with that side's rotation.
- Street, slope, full and connector parts use `HardAirSetting.VOID` (hard air keeps whatever is already there). Slopes use `stair` parts rotated by the slope direction.

### `generateParkSection` (server)

Writes one layer at `height`. Inner columns get grass (the city style's `grassBlock` or vanilla grass). Edge columns get the street block when `parkBorder` is on, else grass; on an elevated park, an edge column instead stays grass when the neighbors in that direction are elevated parks too (corner columns need all three neighbors around the corner).

### `generateRandomVegetation` (server)

For each side whose neighbor has a building, over `THICKNESS_OF_RANDOM_LEAFBLOCKS` columns along that side: drop from `height` to the first non-air block below (not below Y 0), then stack leaves while `random < p` and fewer than 30 placed. `p` is `CHANCE_OF_RANDOM_LEAFBLOCKS × weight`, capped at 0.8, with weights T + 1 … 2 on the −X and −Z sides (x or z = 0 … T − 1), and 1 … T on the +X and +Z sides (x or z = 15 − T … 14, so column 15 is never used); the +Z side has no 0.8 cap (`TERR-08`). Random: `java.util.Random(worldSeed × 377 + chunkZ × 341873128712 + chunkX × 132897987541)`. Leaves come from the city style's `leavesBlock` or the default table, indexed by the palette sequence.

### Front parts (server)

`generateFrontPart(info, height, neighbor, rotation)` writes the **neighbor's** `frontType` part into this chunk at `height` when `info.hasFrontPartFrom(neighbor)` (04-city-layout: the neighbor has a building with a front, this street is `NORMAL` and lower than the building's top, and rail conditions allow). The part is written with the neighbor's `BuildingInfo`, which misplaces block entities and todo items (`TERR-09`).

### `generateStreetDecorations` (server)

If `getActualStairDirection()` is set and a stair part was chosen, write it at `cityGround + 1` with `HardAirSetting.AIR`, rotated: −X none, +X 180°, −Z 90°, +Z 270°.

## 5 · Determinism

- Part choices and park, fountain and leaf picks use the context random and palette sequence in call order ([concurrency sheet](concurrency-caches-determinism.md)).
- The street-type re-roll and random vegetation use their own coordinate seeds as above.
- Order dependence: the street-type re-roll mutates shared state that neighbors read (`TERR-07`); front-part side effects land in neighbors' state (`TERR-09`).

## 6 · Data formats

City style, street parts, building, part metadata and palettes, all from 05-assets. No NBT is written here (block-entity NBT goes through the [driver sheet](chunk-driver-and-parts.md)).

## 7 · Output

Buildings floor by floor with doors, fillers and corridor connections; streets, parks, fountains, front parts, vegetation, stairs and borders, all staged in the driver.

## 8 · Relationships

04-city-layout (all choices), 05-assets (parts), 06-highways-railways (corridors, highways), 07-scattered (stuff), 08-loot-mobs-blocks (loot and spawners in parts), [chunk driver and parts](chunk-driver-and-parts.md) (`generatePart`).

## 9 · 1.20 dependencies

- Vanilla 1.20 blocks in the default leaf and rubble tables: `OAK_LEAVES`, `JUNGLE_LEAVES`, `SPRUCE_LEAVES` (with `PERSISTENT`), `MOSSY_STONE_BRICKS`, `MOSSY_COBBLESTONE`, `MOSS_BLOCK` (no moss block in 1.7.10), `GRASS_BLOCK`.
- `java.util.Random.nextInt(int, int)` is a Java 17 method (`TERR-07`'s re-roll); Java 8 has no such overload.
- `Set.of` for the rubble set (Java 9+).

## 10 · Bugs

`TERR-07`, `TERR-08`, `TERR-09`, `TERR-31`.

## Deviations in the new mod

See [new-spec 03-terrain § Building and street writing](../../new-spec/03-terrain/index.md#building-and-street-writing-building-and-street-writingmd).
