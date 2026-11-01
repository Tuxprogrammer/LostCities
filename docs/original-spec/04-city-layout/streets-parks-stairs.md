# Street sections, parks, fountains, stairs and fronts

What a city chunk without a building becomes: a normal street, a park, an open lot, a fountain, a slope or a stair, and which building fronts face it. Source: `BuildingInfo` street members (`worldgen/lost/BuildingInfo.java:891-924, 1342-1492, 1751-1829`) and their use in `LostCityTerrainFeature.generateStreet`, `generateParkSection`, `generateFrontPart`, `generateStreetDecorations` (`worldgen/LostCityTerrainFeature.java:1017-1035, 1197-1262, 1447-1452, 1534-1602`, out of scope, read for behavior), on `f18a094d`. The street and border block writing itself is `03-terrain`.

## 1. Identity

| Member | Role |
|---|---|
| `BuildingInfo.streetType` | `NORMAL`, `FULL`, `PARK` |
| `parkType`, `fountainType`, `stairType`, `frontType` | Parts chosen in the constructor |
| `isStreetOrParkSection()` | `isCity && !hasBuilding` |
| `isElevatedParkSection()` | A park with enough street or park neighbors |
| `isPlannedRoad()`, `isPrimaryRoad()`, `isHierarchicalOpen()`, `isPredefinedStreet()` | Street plan flags |
| `getStreetSlopeDirection()` | Hierarchical full-chunk slope |
| `getStairDirection()`, `getActualStairDirection()`, `stairPriority` | Stairs between city levels |
| `doesRoadExtendTo()`, `hasRoadConnection(a, b)` | Road continuity |
| `hasFrontPartFrom(adj)` | Whether a neighboring building's front part is placed on this street |

City-style settings (via [city-settings.md](city-settings.md)): `parkchance`, `openlotparkchance`, `fountainchance`, `frontchance`, `parkstreetthreshold`, `parkelevation`, `parkborder`, `avoidfoliage`; parts lists `parks`, `fountains`, `stairs`, `fronts`; blocks `elevation`, `grass`, street characters.

## 2. Inputs

The chunk's and neighbors' `BuildingInfo` (city, building, level, street type, planned road), highway and rail info, effective city settings, street mode.

## 3. Numbers

| Option | Default | Range | Meaning |
|---|---|---|---|
| `PARK_CHANCE` | 0.2 | 0..1 | legacy: chance a street chunk is a park |
| `OPEN_LOT_PARK_CHANCE` | 0.8 | 0..1 | hierarchical: chance an open lot gets a park part |
| `FOUNTAIN_CHANCE` | 0.05 | 0..1 | |
| `BUILDING_FRONTCHANCE` | 0.2 | 0..1 | chance a building has a front part |
| `PARK_STREET_THRESHOLD` | 3 | 0..8 | street/park neighbors (of 8) needed for an elevated park |
| `PARK_ELEVATION` | true | | raise the park by one block |
| `PARK_BORDER` | true | | park edge uses the street block |
| `AVOID_FOLIAGE` | false | | (part saplings; `03-terrain`) |
| Generation-time street seed | `chunkZ * 155557723 + chunkX * 45555558379` | | `java.util.Random`, no world seed |

## 4. Behavior

All (server).

### Street type in the constructor

Single and top-left chunks; non-top-left multibuilding chunks copy these from the top-left.

| Mode / case | `streetType` | Fountain | Park part | Draws |
|---|---|---|---|---|
| `LEGACY` | `nextDouble() < parkChance` → `PARK`; else `values()[nextInt(0, 1)]`, always `NORMAL` (CITY-12) | `nextFloat() < fountainChance` → random fountain | always a random park part | double, (int), float, (fountain float), (park float) |
| Hierarchical open lot (`hierarchicalOpen`) | `PARK` | none | `nextDouble() < openLotParkChance` → random park part, else none | double, (park float) |
| Hierarchical, anything else (planned road, building, non-city) | `NORMAL` | `nextFloat() < fountainChance` → random fountain, dropped again on a planned road | none | float, (fountain float) |

The hierarchical fountain draw is kept on roads only to keep the random consumption stable. "Random part" draws one `nextFloat` when the city style's list is non-empty.

### Where streets are generated

`generateStreet` runs for city chunks without a building (`03-terrain` calls it):
1. Corridors first if this chunk has them ([corridors.md](corridors.md)).
2. `canDoStreetOrPark` = neither highway level equals `cityLevel`, the rail is not a surface station, and not a surface station extension at or above the city level.
3. If so, at `height = cityGround`:
   1. **Elevated park** (`isElevatedParkSection`): fill the whole 16 × 16 layer at `height` with the style's park `elevation` block; if `parkelevation`, `height++`.
   2. Else, unless the chunk is a hierarchical open lot: **overwrite `streetType`** with `values()[nextInt(0, 1)]` from a fresh `java.util.Random(chunkZ * 155557723 + chunkX * 45555558379)`, which is always `NORMAL` (CITY-12, CITY-13). A legacy park that is not elevated therefore becomes a normal street.
   3. Section by type: `NORMAL` → flat street, or a slope section when `getStreetSlopeDirection` is set; `FULL` → full street section (unreachable); `PARK` → park section. Then `height++`.
   4. No slope and (`PARK` or a fountain): place the park part (for `PARK`) or the fountain part at `height` with no rotation.
   5. No slope: random vegetation, then front parts from the west (rotation none), north (90), east (180) and south (270) neighbors.
4. Borders (`03-terrain`).

### Park section (`generateParkSection`)

Interior blocks are the style's `grass` character or vanilla grass. Edge blocks: for a non-elevated park, the street character when `parkborder`, else grass. For an elevated park, an edge block is grass where the neighbors across that edge are elevated parks too (corners need all three neighbors); other edge blocks follow the `parkborder` rule.

### Elevated park (`isElevatedParkSection`)

True when the chunk is a street-or-park section with `streetType == PARK` and at least `parkstreetthreshold` of its 8 neighbors (orthogonal and diagonal) are street-or-park sections.

### Slopes (`getStreetSlopeDirection`, hierarchical mode only)

A road chunk becomes a full-chunk slope toward direction `d` when all of these hold (else `null`):
1. this chunk extends roads (`doesRoadExtendTo`);
2. exactly one direction has a same-class road neighbor one city level higher (`d`); "same class" means both extend roads and both are primary or both are not;
3. the opposite neighbor is a same-class road at this level (the approach);
4. the chunk beyond the upper one, in direction `d`, is a same-class road at the upper level (the departure);
5. no side neighbor (perpendicular to `d`) of this chunk is a same-class road at this level, and none of the upper chunk is at the upper level.

### Stairs

`getStairDirection` (once per chunk, under the dimension lock): `null` if the chunk has a slope. For a city chunk without a building whose `streetType` is not `PARK`, the first of west, east, north, south whose neighbor is city, has no building and is exactly one level higher; else `null`.

`getActualStairDirection`: the stair direction, unless any of the 8 neighbors has a stair direction and a higher `stairPriority`, in which case `null`.

Stairs are placed by `generateStreetDecorations` when the chunk has no building, no highway on either axis, no surface rail and no slope: the `stairType` part at `cityGround + 1`, rotated `XMIN` none, `XMAX` 180, `ZMIN` 90, `ZMAX` 270.

### Road continuity

- `doesRoadExtendTo`: hierarchical mode: city, no building, and a planned road or a predefined street. Legacy: city, no building, and not an elevated park.
- `hasRoadConnection(a, b)`: both extend roads and either share a city level or one is a slope whose upper chunk is the other.

### Fronts (`hasFrontPartFrom(adj)`, called on the street chunk)

True when all hold:
1. the neighbor `adj` has a building and a `frontType`;
2. this chunk's street type is `NORMAL` (an elevated park counts as `PARK`);
3. this chunk's level is below the top of `adj` (`cityLevel < adj.cityLevel + adj.floors`);
4. the rail here is not an underground station or a "going down one from surface" piece, and there is no highway here;
5. `adj`'s floor at this chunk's level (if valid) is not marked `dontconnect`.

The front part is generated at the street height with the rotation of its side (see above). Because `streetType` is rewritten during generation, the answer for a legacy, non-elevated park changes from false to true once the street chunk has generated (CITY-13).

## 5. Determinism

- Constructor draws come from the building random (seeded from the world seed).
- The generation-time overwrite uses a coordinate-only `java.util.Random` and mutates cached state (CITY-13). Its result is constant (`NORMAL`), so the seed does not matter, but the mutation is order-dependent.
- Stair arbitration reads neighbors' priorities; deterministic.

## 6. Data formats

City-style fields above (`05-assets`). No NBT.

## 7. Output

Street, park, fountain, slope, stair and front placement decisions for `03-terrain`; road continuity for borders, doors and bridges.

## 8. Relationships

- Called by: `LostCityTerrainFeature` street and border code (`03`), [doors-and-connections.md](doors-and-connections.md), [bridges.md](bridges.md).
- Calls: [building-info.md](building-info.md) neighbor accessors, rail and highway info (`06`).

## 9. 1.20 dependencies

| Mechanism | Where | Why it matters for 1.7.10 |
|---|---|---|
| `Random.nextInt(int, int)` (Java 17) | both street-type draws | Not in Java 8; must consume exactly one `nextInt()` to stay in step |
| `Blocks.GRASS_BLOCK` fallback | park section | `grass` in 1.7.10 |

## 10. Bugs

CITY-12, CITY-13.

## Deviations in the new mod

None recorded yet.
