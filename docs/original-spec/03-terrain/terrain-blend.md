# Terrain blend at city edges

How a normal (non-city) chunk near a city is reshaped so its terrain meets the city ground without cliffs. Only the `DEFAULT` and `SPHERES` landscapes do this. The corner heights are computed in `BuildingInfo` (04-city-layout's file) but are documented here in full because they are the blend contract.

## 1 · Identity

| Piece | Role |
|---|---|
| `LostCityTerrainFeature.doNormalChunk` | Decides whether to correct, then calls the correction and the normal-chunk generators. |
| `LostCityTerrainFeature.correctTerrainShape` | Bilinear lower and upper meshes, noise, and per-column `moveDown` / `moveUp`. |
| `LostCityTerrainFeature.getTerrainTransitionNoise` | World-coordinate simplex noise that bends the meshes. |
| `LostCityTerrainFeature.getRandomizedOffset`, `getHeightOffsetL1`, `getHeightOffsetL2` | Coordinate-seeded jitter for the corner heights. |
| `BuildingInfo.getLowestCityHeightAtChunkCorner`, `getCityHeightForChunk`, `getDesiredMaxHeightL1`, `getDesiredMaxHeightL2`, `updateMinMaxL1`, `updateMinMaxL2`, `MinMax` | Corner height levels L0, L1, L2. |

Profile options: `TERRAIN_FIX_LOWER_MIN_OFFSET`, `TERRAIN_FIX_LOWER_MAX_OFFSET`, `TERRAIN_FIX_UPPER_MIN_OFFSET`, `TERRAIN_FIX_UPPER_MAX_OFFSET`, `OCEAN_CORRECTION_BORDER`, `GROUNDLEVEL`, `LANDSCAPE_TYPE`. Config: `avoidFlattening` (`Config.AVOID_FLATTENING`).

## 2 · Inputs

- `BuildingInfo` of the chunk and its 8 neighbors, and of their neighbors (L2 reaches two chunks out): `isCity`, city ground level, ocean flag (main biome tagged ocean or deep ocean), structure-avoidance result.
- This chunk's heightmap (one sampled height).
- The chunk's real columns.
- World seed and dimension (for the transition noise).

## 3 · Numbers

| Item | Value | Notes |
|---|---|---|
| "No constraint" sentinel | 100 000 | `MinMax()` default; L0 for unmixed corners and non-city land |
| L0 for a non-city ocean chunk | `groundLevel − OCEAN_CORRECTION_BORDER` = 71 − 4 = 67 | instead of 100 000 |
| "Fixed corner" test in L1 and L2 | value < 256 | hard-coded, not the build height (`TERR-24`) |
| Fixed-corner lower offset | random in [`TERRAIN_FIX_LOWER_MIN_OFFSET`, `…MAX_OFFSET`] = [−4, −3] | added to L0 |
| Fixed-corner upper offset | random in [`TERRAIN_FIX_UPPER_MIN_OFFSET`, `…MAX_OFFSET`] = [−1, 1] | added to L0 |
| L1/L2 neighbor slack, edge neighbors | 20 + `nextInt(5)` (20–24) | blocks |
| L1/L2 neighbor slack, diagonal neighbors | 25 + `nextInt(5)` (25–29) | blocks |
| Unconstrained corner substitute | sampled height ± 90 | when another corner is constrained |
| Constraint test in `correctTerrainShape` | a corner's min or max < max build height (320) | |
| Interpolation | bilinear, corners pinned at x, z = 0 and 15, weight `(15 − i) / 15` | |
| Transition noise | `3 × simplex(x / 28, z / 28) + simplex((x + 10000) / 11, (z − 10000) / 11)` | world X, Z in blocks; range about ±4 |
| Noise strength | `min(1, max(0, max − min − 4) / 20)` | 0 where the corridor is ≤ 4 blocks wide; full at ≥ 24 |
| Kept surface layers when lowering | 6 | |
| Heightmap written back | highest Y touched at column (8, 8) | |

## 4 · Behavior

### When it runs (server)

`doNormalChunk` calls `correctTerrainShape` when the landscape is `DEFAULT` or `SPHERES`, unless the chunk has a **direct** structure-avoidance result and `avoidFlattening` is true (default true), in which case the terrain around an avoided vanilla structure is left alone. Then it posts `PostGenOutsideChunkEvent` and runs bridges, highways and scattered structures.

### Corner heights (server, in `BuildingInfo`)

Each chunk owns the corner at its block (0, 0). The four chunks touching that corner are the chunk itself, its −X neighbor, its −Z neighbor and its −X −Z neighbor.

1. **L0** (`getLowestCityHeightAtChunkCorner`): if all four are city chunks, or none is, 100 000. Otherwise the minimum over the four of `getCityHeightForChunk`: a city chunk's ground Y; a non-city ocean chunk's `groundLevel − OCEAN_CORRECTION_BORDER`; any other chunk 100 000.
2. **L1** (`getDesiredMaxHeightL1`, memoized): if this corner's L0 < 256, the corner is *fixed*: lower = L0 + offset in [−4, −3], upper = L0 + offset in [−1, 1], each from `getRandomizedOffset(chunkX, chunkZ, …)`. Otherwise start at (100 000, 100 000) and, for each of the 8 neighboring chunks' corners, with slack s = 20 or 25 (edge or diagonal) + `getHeightOffsetL1(neighbor)`: lower = min(lower, L0ₙ − s), upper = min(upper, L0ₙ + s).
3. **L2** (`getDesiredMaxHeightL2`, memoized): if L1's lower < 256, use L1. Otherwise the same 8-neighbor reduction over the neighbors' L1 values, with slack 20 or 25 + `getHeightOffsetL2(neighbor)`: lower = min(L1ₙ.lower − s), upper = min(L1ₙ.upper + s).

So a corner where city meets land is pinned to the city ground (−4 … +1); corners one chunk away allow about ±20–29 blocks; corners two chunks away about ±40–58; beyond that the terrain is free.

### `correctTerrainShape` (server)

1. Read L2 of the four corners of this chunk: own (x 0, z 0), +X neighbor's (x 16 → pinned at 15), +Z neighbor's, +X +Z neighbor's.
2. If no corner's lower or upper is below the max build height, stop.
3. Replace each unconstrained upper with `sampledHeight + 90` and each unconstrained lower with `sampledHeight − 90`.
4. For each column (x, z):
   1. Interpolate upper and lower bilinearly between the corners.
   2. Add the transition noise × strength to both.
   3. Cap upper at the max build height. `moveDown(x, z, (int)upper, maxBuild)`: if the terrain surface (skipping air, water, lava, climbable blocks and foliage) is above `upper`, lower the top 6 surface layers so the new top is at `upper − 1`, clearing foliage above.
   4. If nothing was lowered: raise lower to at least the min build height and `moveUp(x, z, (int)lower)`: if the surface is below `lower`, shift the column up so its top is at `lower`, copying until the first air gap or bedrock.
   5. At column (8, 8), remember the Y returned (the highest touched).
5. If something was touched at (8, 8), store it as this chunk's heightmap height (`TERR-10`).

`moveDown` and `moveUp` are described step by step in [city chunk terrain](city-chunk-terrain.md#terrain-preparation-in-docitychunk-server).

### Where the blend reaches (consequence)

Only normal chunks whose own four corners carry an L2 constraint are touched, which is any chunk within two chunk steps of a city edge corner (about 3 chunks, 48 blocks, including the chunk on the edge). City chunks are never blended; they are leveled outright. A city whose ground is far above or below the surrounding land therefore meets it through a ramp of at most about 20–29 blocks of height change per chunk, then walls (borders) inside the city chunk itself.

## 5 · Determinism

| Random | Seed |
|---|---|
| `getRandomizedOffset(chunkX, chunkZ, min, max)` | `java.util.Random(chunkZ × 256203221 + chunkX × 899809363)`, one `nextInt(max − min + 1)` |
| `getHeightOffsetL1(chunkX, chunkZ)` | `Random(chunkZ × 341873128712 + chunkX × 132897987541)`, `nextInt(5)` |
| `getHeightOffsetL2(chunkX, chunkZ)` | `Random(chunkZ × 132897987541 + chunkX × 341873128712)`, `nextInt(5)` |
| Transition noise | `NoiseGeneratorSimplex(LegacyRandomSource(worldSeed ^ dimensionHash × 0x9e3779b97f4a7c15 ^ 0x6a09e667f3bcc909))` |

The three jitter functions use thread-local `Random` objects, ignore the world seed (`TERR-27`), and use the same two constants as each other in swapped roles. The meshes are memoized in `BuildingInfo` and are order-independent. The ±90 fallback reads the cached heightmap, which can be order-dependent (`TERR-10`).

## 6 · Data formats

None.

## 7 · Output

Normal chunks near cities whose top terrain fits between the meshes. Nothing is added (no blocks of new material except copies of the column's own surface).

## 8 · Relationships

- Called from [the pipeline](generation-pipeline.md) stage 5 for normal chunks.
- Corner levels: 04-city-layout (`BuildingInfo`), which also uses `isOcean` and the city ground.
- Structure avoidance: 07-scattered documents `StructureAvoidance`.

## 9 · 1.20 dependencies

- Max and min build height (320, −64) in the constraint test and caps; hard-coded 256 in the L1/L2 "fixed" tests.
- Biome tags `IS_OCEAN`, `IS_DEEP_OCEAN` for the ocean rule.

## 10 · Bugs

`TERR-10`, `TERR-23`, `TERR-24`, `TERR-27`.

## On 1.12

1.12 used a different, distance-based blend (`flattenChunkToCityBorder`). For a normal chunk with any city chunk among its 8 neighbors, each column got a target height bilinearly interpolated from per-corner heights, and an offset of 2 × (distance in blocks to the nearest city chunk's box). The column was filled with base up to `target − offset − rand(2)` and cleared from `target + offset + rand(2)` to Y 230. A second pass did the same downward-only for neighbors carrying an open highway. There were no L1/L2 meshes and no noise, so the effect reached only one chunk out.

## Deviations in the new mod

See [new-spec 03-terrain § Terrain blend](../../new-spec/03-terrain/index.md#terrain-blend-terrain-blendmd).
