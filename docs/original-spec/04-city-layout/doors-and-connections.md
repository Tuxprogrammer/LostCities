# Doors and building connections

Where a building floor gets an opening to the neighboring building (an open passage), or a double door to a street or the roof of a lower neighbor. Source: `worldgen/gen/Doors.java`; `BuildingInfo.connectionAtX/Z`, `hasConnectionAtX`, `hasConnectionAtZ`, `hasConnectionAtXFromStreet`, `hasConnectionAtZFromStreet`, `hasConnectionAt`, `getRandomDoor` (`worldgen/lost/BuildingInfo.java:1072-1073, 1329-1340, 1793-1904`); the call site in `LostCityTerrainFeature` (`2203-2226`, out of scope, read for behavior), on `f18a094d`.

## 1. Identity

| Member | Role |
|---|---|
| `connectionAtX[i]`, `connectionAtZ[i]` | Random doorway flag for array level `i` on the west (X) or north (Z) side |
| `hasConnectionAtX(i)`, `hasConnectionAtZ(i)` | Connection on this chunk's west / north side, building view |
| `hasConnectionAtXFromStreet(i)`, `hasConnectionAtZFromStreet(i)` | Same, asked of the chunk east / south of a building |
| `Doors.generateDoors(feature, info, height, f)` | Writes openings and doors for local floor `f` |
| `doorBlock` | One of seven 1.20 doors |

Profile: `BUILDING_DOORWAYCHANCE`. Building asset: `allowDoors`, `filler` character. Part metadata: `dontconnect` (`ILostCities.META_DONTCONNECT`).

## 2. Inputs

This chunk's and the four neighbors' `BuildingInfo` (city, building, floors, cellars, level, floor parts, flags, slope, front), the compiled palette.

## 3. Numbers

| Item | Value |
|---|---|
| `BUILDING_DOORWAYCHANCE` | 0.6 (0..1), per level per side, drawn only if the neighbor on that side is city |
| Opening | 2 wide (x or z = 7, 8), 2 high, in a 4-wide frame (6..9) |
| Frame | filler columns at 6 and 9 from the floor's bottom Y to bottom + 3; filler below and above the opening |
| Door rows | lower half at floor bottom + 1, upper half at + 2 |
| Door blocks | `nextInt(7)`: birch, acacia, dark oak, spruce, oak, jungle, iron |

## 4. Behavior

All (server).

### Flags (constructor)

For each array level `i` (0 = lowest cellar, `cellars` = ground floor, `cellars + floors` = top part):
`connectionAtX[i] = isCity(west) && nextFloat() < BUILDING_DOORWAYCHANCE`; then the same for `connectionAtZ[i]` with the north neighbor. Every chunk has these arrays, sized by its own (candidate) floors and cellars, even a street chunk.

### Connection queries

`hasConnectionAtX(i)`, building view of its west side:
1. Not city: false.
2. Part of a multibuilding and in its last column (`x == w - 1`): false (CITY-35).
3. `i` outside the array: false.
4. This chunk's part at `i` is `dontconnect`: false.
5. The west neighbor would carry this building's front part (`west.hasFrontPartFrom(this)`): true.
6. Otherwise `connectionAtX[i]`.

`hasConnectionAtXFromStreet(i)`, asked of the chunk east of a building about its own west side: the same, without step 4 and with step 5 reading `this.hasFrontPartFrom(west)`. Z versions use the north side and the last row (`z == h - 1`).

Each edge between two chunks uses the flag of the east (or south) chunk, so both sides agree.

### `Doors.generateDoors` per floor

Called for each local floor `f` from `-cellars` to `floors - 1` (not the top part) when the building's `allowDoors` is true. `height` is the floor's bottom Y. `filler` = the building's filler character.

1. **West side (x = 0)**, when `hasConnectionAtX(f + cellars)`:
   - if the west neighbor has a building whose floor or cellar includes the same global level: open passage (frame, air, air);
   - else if "top or outside" (below): door pair facing east, hinge left at z = 7 and right at z = 8.
2. **East side (x = 15)**:
   - passage when: this floor (if valid) and the east neighbor's floor at the same global level are not `dontconnect`, the neighbor has a building whose floor or cellar includes that level, and the neighbor's own west connection at that level is set;
   - else door pair facing west (hinge right at z = 7, left at z = 8) when "top or outside" and the east chunk's `hasConnectionAtXFromStreet(f + east.cellars)`.
3. **North side (z = 0)** like the west side, doors facing north (hinge left at x = 7, right at x = 8).
4. **South side (z = 15)** like the east side, doors facing south (hinge right at x = 7, left at x = 8).

"Top or outside" (`hasConnectionToTopOrOutside`) is true when this floor is not `dontconnect`, the neighbor is not a street slope, and either the neighbor is a city chunk without a building, this is the ground floor (`f == 0`) and the neighbor is at the same city level; or the neighbor has a building and this floor's global level equals the neighbor's top (its roof).

The west/north passage test does not check the neighbor's `dontconnect`, while the east/south test does (CITY-36).

### Front parts force doors

A street chunk that will carry a neighbor building's front part makes that building's connection true at every level, and the "top or outside" rule then admits a door on the ground floor facing the front.

## 5. Determinism

Flags come from the building random (one `nextFloat` per level and side, only when the neighbor is city). Queries read neighbors' final state; `streetType` changes during generation alter `hasFrontPartFrom` (CITY-13) and `getBuildingBottomHeight` can change floors and cellars (CITY-18), so door placement depends on generation order.

## 6. Data formats

Building `allowDoors`, `filler`; part `dontconnect` metadata (`05-assets`). No NBT.

## 7. Output

Openings and doors in building walls.

## 8. Relationships

- Called by: the building generator in `LostCityTerrainFeature` (`03-terrain`).
- Calls: [streets-parks-stairs.md](streets-parks-stairs.md) (`hasFrontPartFrom`, slopes), `ChunkDriver` (`03`).

## 9. 1.20 dependencies

| Mechanism | Where | Why it matters for 1.7.10 |
|---|---|---|
| Door block states `HALF`, `HINGE`, `FACING` | `getDoor` | 1.7.10 doors encode facing, open and hinge in metadata of the lower and upper block |
| Birch, acacia, dark oak, spruce and jungle doors | `getRandomDoor` | 1.7.10 has only `wooden_door` and `iron_door` (per-wood doors arrived in 1.8; Et Futurum Requiem may provide them) |

## 10. Bugs

CITY-13, CITY-18, CITY-35, CITY-36.

## Deviations in the new mod

None recorded yet.
