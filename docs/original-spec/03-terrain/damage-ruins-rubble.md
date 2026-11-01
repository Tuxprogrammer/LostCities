# Explosions, damage, debris, ruins and rubble

The "abandoned" look: explosion craters that break buildings and terrain, debris thrown into neighboring chunks, ruined building tops, and the rubble and leaf layer on streets. Decision record 0004 keeps these features; they must still leave structures a player can move into.

## 1 · Identity

| Piece | Role |
|---|---|
| `worldgen/lost/DamageArea` | Per chunk: the explosions that reach it, and damage math. Built by `BuildingInfo` (`getDamageArea`). |
| `worldgen/lost/Explosion` | A center (block position) and a radius (blocks). Implements API `ILostExplosion`. |
| `LostCityTerrainFeature.breakBlocksForDamageNew` | Applies damage layer by layer. |
| `LostCityTerrainFeature.generateDebris`, `generateDebrisFromChunk` | Scatters rubble from damaged neighboring buildings. |
| `LostCityTerrainFeature.generateRuins` | Cuts the top off ruined buildings with noise. |
| `LostCityTerrainFeature.generateRubble`, `getInterpolatedHeight`, `bipolate` | Rubble and leaves on streets and ruined building chunks. |

Profile options: `EXPLOSION_CHANCE`, `EXPLOSION_MINRADIUS`, `EXPLOSION_MAXRADIUS`, `EXPLOSION_MINHEIGHT`, `EXPLOSION_MAXHEIGHT`, `MINI_EXPLOSION_*` (same five), `EXPLOSIONS_IN_CITIES_ONLY`, `DEBRIS_TO_NEARBYCHUNK_FACTOR`, `RUIN_CHANCE`, `RUIN_MINLEVEL_PERCENT`, `RUIN_MAXLEVEL_PERCENT`, `RUBBLELAYER`, `RUBBLE_DIRT_SCALE`, `RUBBLE_LEAVE_SCALE`. City style: `explosionChance`, `ironbarsBlock`, `rubbleDirtBlock`, `leavesBlock`. Building: `rubbleBlock`, `fillerBlock`. Block tags: `lostcities:not_breakable`, `lostcities:easy_breakable`. Palette: `damaged` (iron-bars) entries via `canBeDamagedToIronBars` (05-assets).

## 2 · Inputs

World seed, chunk coordinates, profile, the chunk characteristics (city level, city style) of every chunk within explosion range, the chunk's `BuildingInfo` and its 8 neighbors' (building, floors, damage factor, palettes), the staged and real blocks, noise generators.

## 3 · Numbers

| Item | Default | Unit / notes |
|---|---|---|
| `EXPLOSION_CHANCE` | 0.002 | per chunk |
| `EXPLOSION_MINRADIUS` / `MAXRADIUS` | 15 / 35 | blocks; radius = min + `nextInt(max − min)` (max never reached) |
| `EXPLOSION_MINHEIGHT` / `MAXHEIGHT` | 75 / 90 | center Y = `cityLevel × 6 + min + nextInt(max − min)` |
| `MINI_EXPLOSION_CHANCE` | 0.03 | per chunk |
| `MINI_EXPLOSION_MINRADIUS` / `MAXRADIUS` | 5 / 12 | blocks |
| `MINI_EXPLOSION_MINHEIGHT` / `MAXHEIGHT` | 60 / 100 | Y, same formula |
| `EXPLOSIONS_IN_CITIES_ONLY` | true | |
| Search range | `ceil(max(EXPLOSION_MAXRADIUS, MINI_EXPLOSION_MAXRADIUS) / 16)` = 3 chunks each way | |
| Damage at distance d from a center of radius r | `3 × (r − d) / r` for d < r, summed over explosions | ≥ 1 within 2r/3 |
| Damage floor | 0.001 | below this a block is not considered |
| `BLOCK_DAMAGE_CHANCE` | 0.7 | |
| Easy-breakable multiplier | × 2.5 | |
| Iron-bars substitution | 70 % when damage < 0.7 and the palette has a damaged form | |
| Upward carry | collected damage ÷ 1.4 per layer | |
| Layer escalation | damaged + air in a layer > 250 → factor 200; > 220 → × 1.4; > 180 → × 1.2 | out of 256 columns |
| Y range processed | 0–255 (16 sections) | hard-coded (`TERR-19`) |
| `DEBRIS_TO_NEARBYCHUNK_FACTOR` | 200 | divisor |
| Debris trigger | neighbor's damage factor > 0.5 | damage factor = Σ `3 × (r − d) / r`, d = horizontal distance from each explosion center to the neighbor chunk's (0, 0) corner |
| Debris count | `(1 + floors) × 1000 × max(1, factor × 0.7) / 200` | blocks |
| Debris iron bars | 1 in 5 | |
| `RUIN_CHANCE` | 0.05 | per building (04-city-layout rolls it) |
| `RUIN_MINLEVEL_PERCENT` / `MAXLEVEL_PERCENT` | 0.8 / 1.0 | `ruinHeight` fraction of the building's floors |
| Ruin noise | 4 octaves, scale 0.0625 per block, amplitude 1 | |
| Ruin rubble / iron bars | 0.2 each per step (first rubble only once per column) | "hardcoded random" |
| `RUBBLELAYER` | true | |
| `RUBBLE_DIRT_SCALE` / `RUBBLE_LEAVE_SCALE` | 3.0 / 6.0 | divisors; < 0.01 disables |
| Rubble noise | 4 octaves, scale 1/16 per block, amplitude 1 | |
| Leaves noise | 4 octaves, sampled at (chunk × 64) with scale 1/64 (so 1/16 per block), amplitude 4 | |
| Rubble or leaves threshold | > 0.5 | |

## 4 · Behavior

### Building the `DamageArea` (server, when `BuildingInfo` is built)

1. `damageRandom` = `java.util.Random(worldSeed + chunkZ × 295075153 + chunkX × 899826547)`.
2. For every chunk C within the search range (3 chunks), in X-then-Z order, if explosions are not limited to cities or C is a city chunk:
   1. **Big explosion at C:** `Random(worldSeed + C.z × 295075153 + C.x × 797003437)`; if `nextFloat() < EXPLOSION_CHANCE`: radius, then center X = C.x × 16 + `nextInt(16)`, Y by the formula, Z = C.z × 16 + `nextInt(16)`. The Y uses C's city level from its characteristics.
   2. If the explosion's sphere reaches this chunk's box (X and Z of the chunk, Y 0–256; squared distance from box to center ≤ r²): include it when C's city style has no explosion chance, or `damageRandom.nextFloat() <` that chance.
   3. **Mini explosion at C:** the same with `Random(worldSeed + C.z × 1400305337 + C.x × 573259391)` and the mini settings.
3. `hasExplosions()` is true when the list is non-empty.

### `breakBlocksForDamageNew` (server, pipeline stage 10)

Runs only when the chunk has explosions and `PreExplosionEvent` was not canceled. `factor` starts at 1. `collected[x][z]` starts at 0.

For each Y from 0 to 255 (section by section):
1. If this 16-block section is within reach of any explosion: for every column whose block at Y is not air, or whose Y ≤ the chunk's `waterLevel`, add `getDamage(x, Y, z) × factor` to `collected` when it is ≥ 0.001.
2. If anything has been collected (in this or a lower layer), for every column:
   - if the block is not air, or Y ≤ `waterLevel`, and `collected` ≥ 0.001: `damageBlock` (below); stage the result if it changed, counting it as damaged;
   - else, if the block is air above the water level, count it as air;
   - then `collected /= 1.4` (a value that drops to 0 stops counting).
3. Escalate `factor` by the layer's damaged + air count (Numbers). Once factor is 200, everything reached above is effectively destroyed.

`damageBlock(state, y, damage)`:
1. Blocks tagged `not_breakable` are unchanged. Blocks tagged `easy_breakable` take 2.5 × damage.
2. With probability `damage` (a draw ≤ damage): if damage < 0.7 and the palette has a damaged form, 70 % become the damaged form (usually iron bars); otherwise, and in the remaining cases, the block becomes the profile's liquid when Y ≤ the generator's sea level, else air.

Air below the water level is also "damaged" into liquid, so explosion reach below sea level floods enclosed cellars and caves with source blocks (`TERR-18`).

### Debris (server, pipeline stage 10, always run)

For each of the 8 neighbors (4 sides, then corners) that has a building whose damage factor exceeds 0.5:
1. Rubble character: the neighbor building's `rubbleBlock` if its palette defines it, else its `fillerBlock`.
2. Count as in Numbers.
3. Start height `h` = neighbor's building top + 10 (if that is above `maxBuild − 1`, `h` becomes `minBuild − 1`: `TERR-04`).
4. Repeat count times: pick (x, z) with two `nextInt(16)`; accept with probability `locationFactor(x, z)`, which favors the side facing the neighbor (sides: `(15 − x) / 16`, `x / 16`, `(15 − z) / 16`, `z / 16`; corners: products ÷ 256). On acceptance, move down from `h` while the block is empty (air, water, lava) and `h > 0`, then stage at `h + 1` either iron bars (1 in 5, city style `ironbarsBlock` or vanilla iron bars) or the neighbor's rubble. `h` is **not** reset between blocks (`TERR-05`).

### Ruins (server, in `doCityChunk` step 3)

Only for building chunks whose `ruinHeight` ≥ 0 (04-city-layout rolls `RUIN_CHANCE` and the fraction).
1. `base` = `cityGround + 1 + ruinHeight × floors × 6`.
2. Sample the ruin noise; if `RUBBLELAYER`, also refresh the leaves noise.
3. Per column: start at `base + (int)noise`; the number of steps is `buildingTop + 10 − start` (meant to be capped at `maxBuild − 2`, but the cap compares a count with a Y: `TERR-29`). At each step upward, looking at the current block and the one below:
   - if the building has a rubble character, the one below is solid, not iron bars, and a 0.2 draw passes (once per column): place rubble;
   - else if the current block has a damaged (iron bars) form or the one below is iron bars, the one below is solid, and a 0.2 draw passes: place iron bars;
   - else if leaf budget remains (from the leaves noise ÷ `RUBBLE_LEAVE_SCALE`): drop down through empty blocks to the first solid one (`TERR-30`), place a leaf (city style `leavesBlock` or the default table), spend one;
   - else place air.

### Rubble layer (server, in `doCityChunk` step 6)

When `RUBBLELAYER` and the chunk is a street or a ruined building:
1. Sample rubble and leaves noise for the chunk.
2. Per column, `vr = rubble ÷ RUBBLE_DIRT_SCALE`, `vl = leaves ÷ RUBBLE_LEAVE_SCALE` (0 when the scale is < 0.01). If either > 0.5:
   1. Height = bilinear interpolation of the "outside lower" city ground (a non-city chunk's ground is one lower) of the four chunks around the column's quadrant: for x < 8 and z < 8 the −X−Z, −Z, −X neighbors and the chunk itself, and so on per quadrant, with offsets dx, dz = x ± 8, z ± 8 and weight `(15 − d) / 15`. The weights are reversed, so near the chunk's center the result is the diagonal neighbor's value and near the chunk's corners it leans toward this chunk's (`TERR-37`). Where all four levels match, this is simply the city ground.
   2. If the block below is neither air nor liquid: for `i < vr`, add a rubble block (city style `rubbleDirtBlock` or the default table: mossy stone bricks, mossy cobblestone, moss) where empty, else just move up.
   3. If the block below is now the base block or one of the rubble blocks: for `i < vl`, add a leaf where empty, else move up.

## 5 · Determinism

| Random | Seed |
|---|---|
| Explosion inclusion | `worldSeed + chunkZ × 295075153 + chunkX × 899826547` |
| Big explosion at C | `worldSeed + C.z × 295075153 + C.x × 797003437` |
| Mini explosion at C | `worldSeed + C.z × 1400305337 + C.x × 573259391` |
| Block damage and debris | the context random after the stage-9 reseed: `chunkX × 257017164707 + chunkZ × 101754694003` (no world seed, `TERR-27`) |
| Ruins | the context random (world-seeded mix), in call order |
| Rubble, leaves, ruin, bottom-layer noise | `NoiseGeneratorPerlin` × 4, constructed in that order (rubble, leaves, ruin, bottom layer) from one `LegacyRandomSource(worldSeed)` per dimension |
| Leaf and rubble block picks | palette sequence |

Explosions are a pure function of seed and coordinates. Block damage reads the staged chunk, which by then is fully generated and deterministic for the chunk. Debris lands on whatever the chunk holds at that point, which does not depend on neighbors' state.

## 6 · Data formats

`ILostExplosion` (API): `getRadius()`, `getCenter()`. Exposed through `ILostChunkInfo.getExplosions()` (01-core). No NBT.

## 7 · Output

Holes and partly broken blocks (iron bars), water below sea level inside craters, rubble piles next to badly damaged buildings, ragged ruined building tops with rubble, bars and leaves, and a rubble and leaf scatter on streets.

## 8 · Relationships

`BuildingInfo` builds the `DamageArea` and rolls ruins (04-city-layout). City styles and palettes give explosion chance, rubble, bars and leaves (05-assets). `PreExplosionEvent` lets other mods cancel damage and debris (01-core API). `/lostcities debug` prints the explosion count.

## 9 · 1.20 dependencies

- `AABB` box distance via `GeometryTools`; `BlockPos.distToCenterSqr` (measures to block centers, +0.5).
- Block tags `not_breakable`, `easy_breakable`.
- Vanilla `IRON_BARS`, `MOSSY_STONE_BRICKS`, `MOSSY_COBBLESTONE`, `MOSS_BLOCK` (no moss block in 1.7.10).
- Y 0–255 hard-coded loops on a −64 to 320 world.

## 10 · Bugs

`TERR-03`, `TERR-04`, `TERR-05`, `TERR-18`, `TERR-19`, `TERR-27`, `TERR-28`, `TERR-29`, `TERR-30`, `TERR-37`.

Unused code: `DamageArea.isCompletelyDestroyed` (a section fully within 2r/3 of a center), `getLowestExplosionHeight` and `getHighestExplosionHeight` have no callers in 1.20 (*dead code*; they served 1.12's cleanup below).

## On 1.12

1.12 ran `fixAfterExplosionNew` after the damage pass: it found connected blobs of solid blocks between the lowest and highest exploded sections, and either deleted a blob (by `DESTROY_OR_MOVE_CHANCE`, or when it was smaller than `DESTROY_SMALL_SECTIONS_SIZE`, or had fewer than 5 connections) or dropped it down to rest on the next solid block. 1.20 has no such pass and no such profile options, so floating fragments stay (`TERR-28`).

## Deviations in the new mod

None recorded yet.
