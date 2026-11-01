# Directions, orientations and transforms

Small enums used everywhere in the layout: the four chunk sides, the two axes, and the seven part transforms (four rotations and three mirrors) with their effect on chunk-local coordinates, block states and rail shapes. Source: `worldgen/lost/Direction.java`, `Orientation.java`, `Transform.java`; `LostCityTerrainFeature.transformBlockState` (`worldgen/LostCityTerrainFeature.java:1993-2009`, out of scope, read for behavior), on `f18a094d`.

## 1. Identity

| Enum | Values | Members |
|---|---|---|
| `Direction` | `XMIN` (west), `XMAX` (east), `ZMIN` (north), `ZMAX` (south); `VALUES` in that order | `getOrientation`, `getRotation`, `getOpposite`, `get(info)` (neighbor `BuildingInfo`), `atSide(x, z)` |
| `Orientation` | `X`, `Z` | `getMinDir`, `getMaxDir`, `getOpposite` |
| `Transform` | `ROTATE_NONE`, `ROTATE_90`, `ROTATE_180`, `ROTATE_270`, `MIRROR_X`, `MIRROR_Z`, `MIRROR_90_X` | `getMcRotation`, `randomRotation`, `getOpposite`, `rotateX`, `rotateZ`, `transform(RailShape)` |

## 2. Inputs

Chunk-local coordinates 0..15, block states, rail shapes.

## 3. Numbers

### Coordinates (`rotateX`, `rotateZ`) and block rotation (`getMcRotation`)

| Transform | new x | new z | Block-state rotation | `getOpposite` |
|---|---|---|---|---|
| `ROTATE_NONE` | x | z | none | `ROTATE_NONE` |
| `ROTATE_90` | 15 - z | x | clockwise 90 | `ROTATE_270` |
| `ROTATE_180` | 15 - x | 15 - z | clockwise 180 | `ROTATE_180` |
| `ROTATE_270` | z | 15 - x | counterclockwise 90 | `ROTATE_90` |
| `MIRROR_X` | 15 - x | z | clockwise 180 | itself |
| `MIRROR_Z` | x | 15 - z | clockwise 180 | itself |
| `MIRROR_90_X` | z | x | clockwise 90 | itself |

`ROTATE_90` maps east to south, matching Minecraft's clockwise rotation. The mirrors map to rotations for block states, which is wrong for any block facing along the unmirrored axis (CITY-28).

### Rail shapes (`transform`)

`ROTATE_NONE` returns the shape unchanged. Otherwise:

| Shape | `ROTATE_90` | `ROTATE_180` | `ROTATE_270` | `MIRROR_X` | `MIRROR_Z` | `MIRROR_90_X` |
|---|---|---|---|---|---|---|
| north-south | east-west | same | east-west | same | same | east-west |
| east-west | north-south | same | north-south | same | same | same |
| ascending east | asc. south | asc. west | asc. north | asc. west | **throws** | asc. north |
| ascending west | asc. north | asc. east | asc. south | asc. east | **throws** | asc. south |
| ascending north | asc. east | asc. south | asc. west | asc. south | asc. south | asc. west |
| ascending south | asc. west | asc. north | asc. east | asc. north | asc. north | asc. east |
| south-east | south-west | north-west | north-east | south-west | north-east | north-east |
| south-west | north-west | north-east | south-east | south-east | north-west | south-east |
| north-west | north-east | south-east | south-west | north-east | south-west | south-west |
| north-east | south-east | south-west | north-west | north-west | south-east | north-west |

The rotations are consistent. The mirrors are not: `MIRROR_X` flips ascending north/south although it only mirrors X; `MIRROR_Z` throws `IllegalStateException("Cannot happen!")` on ascending east/west (CITY-27); `MIRROR_90_X` transposes coordinates, rotates block states clockwise 90 and maps rails like `ROTATE_270` except east-west (CITY-28).

### Direction helpers

| `Direction` | Orientation | `getRotation` | Opposite | `atSide` true when |
|---|---|---|---|---|
| `XMIN` | X | `ROTATE_NONE` | `XMAX` | x == 0 |
| `XMAX` | X | `ROTATE_180` | `XMIN` | x == 15 |
| `ZMIN` | Z | `ROTATE_90` | `ZMAX` | z == 0 |
| `ZMAX` | Z | `ROTATE_270` | `ZMIN` | z == 15 |

## 4. Behavior

1. (server) A part placed with a transform writes each source column `(x, z)` at `(rotateX(x, z), rotateZ(x, z))`.
2. (server) `transformBlockState`: blocks in the `lostcities:rotatable` tag are rotated with `getMcRotation`; `rail` and `powered_rail` states get their shape mapped with `transform`; other rail-like states in the rail-state set throw `"Error with rail!"`; everything else is unchanged.
3. `randomRotation` returns a uniform rotation from `Math.random()`. **Dead code**: no caller (CITY-29).

Uses: stairs and fronts (rotations by side), scattered buildings (rotations only), bridge support parts in Z bridges (`MIRROR_90_X`), railways (`MIRROR_X`), monorails (`MIRROR_90_X`, `MIRROR_Z`), highway junctions (`06`).

## 5. Determinism

Pure, except the unused `randomRotation`.

## 6. Data formats

None. The `rotatable` block tag is in `08-loot-mobs-blocks`.

## 7. Output

Transformed coordinates, block states and rail shapes.

## 8. Relationships

- Used by: `LostCityTerrainFeature.generatePart` and `transformBlockState` (`03`), [bridges.md](bridges.md), `07-scattered`, `06-highways-railways`.

## 9. 1.20 dependencies

| Mechanism | Where | Why it matters for 1.7.10 |
|---|---|---|
| `Rotation` and `BlockState.rotate` | `getMcRotation` | 1.7.10 has no generic rotation; each block family's metadata must be mapped |
| `RailShape` enum and the `SHAPE` property | `transform` | Rail metadata 0..9 in 1.7.10 (same ten shapes) |
| Block tag `lostcities:rotatable` | `transformBlockState` | No block tags; a block list instead |

## 10. Bugs

CITY-27, CITY-28, CITY-29.

## Deviations in the new mod

None recorded yet.
