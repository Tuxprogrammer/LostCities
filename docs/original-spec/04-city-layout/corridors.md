# Corridors

Underground rail corridors run below city streets, one floor under ground level, between buildings that have cellars. Source: `BuildingInfo.hasXCorridor`, `hasZCorridor`, `canRailGoThrough`, `canWaterCorridorGoThrough`, the corridor flags in the constructor (`worldgen/lost/BuildingInfo.java:1076-1082, 1687-1749`), and `worldgen/gen/Corridors.java`, on `f18a094d`.

## 1. Identity

| Member | Role |
|---|---|
| `BuildingInfo.xRailCorridor`, `zRailCorridor` | Random candidate flags per chunk |
| `hasXCorridor()`, `hasZCorridor()` | A corridor actually runs through this chunk |
| `canRailGoThrough()` | City chunk with no building, or a building without cellars |
| `canWaterCorridorGoThrough()` | City chunk with no building, or a building with at most one cellar. **Unused** in this area (no caller found in scope) |
| `Corridors.generateCorridors` | Writes the corridor in a street chunk |
| `Corridors.generateCorridorConnections` | Opens the wall of a building cellar next to a corridor |

City-style settings: `corridors.corridorchance`, `corridors.roof`, `corridors.glass`, and the style's `glowstone` character. Profile: `CORRIDOR_CHANCE`, base block (`getBaseBlock`, `02`).

## 2. Inputs

The chunk's and neighbors' `BuildingInfo` (city, building, cellars, candidate flags), the compiled palette, the city style.

## 3. Numbers

| Item | Value | Unit |
|---|---|---|
| `CORRIDOR_CHANCE` | 0.7 (0..1), overridden by `corridorchance` | per axis per chunk |
| Corridor band | z 7..10 (X corridor), x 7..10 (Z corridor) | blocks |
| Corridor column | `groundLevel - 6` floor (roof character), `-5` rail or air, `-4` and `-3` air, `-2` and `-1` roof (or glass and glowstone on the light line) | block Y |
| Rail | X: at z = 10, shape east-west; Z: at x = 10, default (north-south) | |
| Light | X: x = 7, z 8..9; Z: z = 7, x 8..9: glass then glowstone at the top | |
| Fill outside the band | base block from `groundLevel - 5` up to `cityGround - 1` | block Y |
| Opening into a cellar | 4 wide (7..10) at the edge, `groundLevel - 5` up to `groundLevel - 3` (3 high) | block Y |

## 4. Behavior

All (server).

### Candidates (constructor)

A building with at least one cellar gets no candidates and draws nothing. Every other chunk draws `xRailCorridor = nextFloat() < corridorChance`, then `zRailCorridor` the same.

### Does a corridor run here (`hasXCorridor`)

1. Not an X candidate: false.
2. Walk west from the west neighbor while the chunk `canRailGoThrough` and is an X candidate. The chunk where the walk stops must have a building with cellars; else false.
3. Walk east the same way; the stop chunk must have a building with cellars.

`hasZCorridor` is the same along Z. A corridor therefore links two cellared buildings through a straight run of candidate street (or cellarless building) chunks.

### Writing (`generateCorridors`, called from street generation for city chunks without a building)

For each column:
1. In the corridor band: at `groundLevel - 6` the roof character, then the rail (on the rail line) or air, then two air blocks.
   - On the light line: then glass and glowstone (the style's glowstone character, or vanilla glowstone), and the glowstone position is queued for a client light update.
   - Elsewhere: then two roof blocks.
2. Outside the band: base block from `groundLevel - 5` up to `cityGround - 1` (`ChunkDriver.setBlockRange` excludes its upper bound).

Note: the corridor sits at the profile ground level, not the chunk's city level, so it is one floor under level-0 streets and deeper under higher ones.

### Openings (`generateCorridorConnections`, building chunks)

For each side whose neighbor has a corridor along that axis, set the 4-wide, 3-high opening on that edge to air. Called for buildings with at least one cellar.

## 5. Determinism

The candidate draws come from the building random (two `nextFloat` per eligible chunk). Walks are pure reads of neighbors. No order dependence beyond CITY-18 (cellar counts can change after `getBuildingBottomHeight`).

## 6. Data formats

City-style `corridors` object (`05-assets`, [city-settings.md](city-settings.md)). No NBT.

## 7. Output

Corridor blocks and rails in street chunks; openings in cellar walls.

## 8. Relationships

- Called by: `LostCityTerrainFeature.generateStreet` and the building generator (`03-terrain`).
- Calls: `ChunkDriver` (`03`), `LostCityTerrainFeature.updateNeeded`.

## 9. 1.20 dependencies

| Mechanism | Where | Why it matters for 1.7.10 |
|---|---|---|
| `RailBlock.SHAPE` block-state property | rail placement | Rail metadata 0 (north-south) and 1 (east-west) in 1.7.10 |
| `Block.UPDATE_CLIENTS` light update | glowstone | Light updates are handled differently in 1.7.10 population |

## 10. Bugs

CITY-18 (indirect). No corridor-specific bug found.

## Deviations in the new mod

None recorded yet.
