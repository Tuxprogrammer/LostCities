# Chunk driver and part writing

How every Lost Cities block reaches the chunk: the `ChunkDriver` staging buffer with its cursor, neighbor-shape correction and final commit, and `generatePart`, which turns a part's character columns and palette into staged blocks and side jobs (torches, loot, spawners, block entities, deferred blocks).

## 1 · Identity

| Piece | Role |
|---|---|
| `worldgen/ChunkDriver` | Cursor-based writer over a per-chunk `SectionCache`; commit with `actuallyGenerate`. One per thread, held in `GenerationContext`. |
| `ChunkDriver.SectionCache`, `ChunkDriver.S` | Staged block states for every section of the chunk, plus a per-column top tracker. |
| `LostCityTerrainFeature.generatePart` | Writes one part (05-assets `IBuildingPart`) at an offset with a rotation. |
| `LostCityTerrainFeature.HardAirSetting` | `AIR`, `WATERLEVEL`, `VOID`: what the part's "hard air" character becomes. |
| `LostCityTerrainFeature.computePalette`, `transformBlockState`, `getTypeForBlock`, `handleBlockEntity`, `handleSpawner`, `handleLoot`, `handleTodo`, `updateNeeded`, `setBlocksFromPalette` | Palette merge, rotation, and the special-block paths. |

Profile options: `EDITMODE`, `AVOID_WATER`, `AVOID_FOLIAGE`, `GENERATE_LIGHTING`, `GENERATE_SPAWNERS`, `GENERATE_LOOT`. Config: `forceSaplingGrowth`. Block tags: `lostcities:rotatable`, `lostcities:lights`, `lostcities:needspoi`, `minecraft:saplings`, `minecraft:small_flowers` (08-loot-mobs-blocks lists their contents). Part metadata: `nowater`, `forcedair`.

## 2 · Inputs

- The region and chunk (set with `setPrimer`).
- Writes and reads from the generators.
- For `generatePart`: the chunk's `BuildingInfo` (or a neighbor's; see `TERR-09`), the part, a `Transform`, an offset (x, y, z relative to the chunk), the hard-air setting, the compiled palette (city palette merged with the part's local palette).

## 3 · Numbers

| Item | Value |
|---|---|
| Section size | 16 × 16 × 16; index `(x << 8) + ((y & 15) << 4) + z` |
| Sections staged | `(maxBuild − minBuild) / 16` (24 in the 1.20 overworld) |
| Empty column marker | `Integer.MIN_VALUE` |
| Heightmaps raised at commit | `MOTION_BLOCKING`, `MOTION_BLOCKING_NO_LEAVES`, `OCEAN_FLOOR`, `WORLD_SURFACE` |
| Cross-chunk neighbor write flag | `Block.UPDATE_CLIENTS` (2), only if the neighbor chunk is `FULL` |
| Loot, POI and light refresh flags | `UPDATE_CLIENTS` (2); POI block placement `UPDATE_NONE`; non-forced sapling `UPDATE_ALL_IMMEDIATE` |
| Command block tick delay | 1 tick |

## 4 · Behavior

### Staging (server)

1. `setPrimer(region, chunk)` creates a fresh `SectionCache` sized to the level's build height and remembers the chunk's coordinates. Every section starts empty (all `null`), and every column's top is "none".
2. The cursor is a mutable block position. `current(x, y, z)` takes chunk-relative x, z and absolute y; `currentAbsolute` takes a world position. `incX/incY/incZ/decY` move it.
3. **Write one block** (`block(state)` or `add(state)` which also moves up): run `correct(state)` (below), then stage the result unless it is `null`. Staging an identical state is skipped. A non-air state marks the section non-empty and raises the column's top; air recomputes the column's top by scanning down through staged states.
4. **Write a range** (`setBlockRange(x, y1, z, y2, state)`): stage `state` from `y1` to `y2 − 1` in one column, with no correction. A predicate variant only replaces staged states that pass the test, and skips every position nothing was staged or read at, so it never touches untouched vanilla blocks (`TERR-36`; highways use it to clear above the deck).
5. **Read** (`getBlock`, `getBlockDown`, `getBlockEast` …, `getBlock(x, y, z)`): return the staged state; if none, read the region and **stage what was read** (`TERR-15`). The cache addresses positions by `x & 15`, `z & 15` only, so a position in another chunk aliases this chunk's column (`TERR-14`); `getBlockSafe` avoids that by reading other chunks straight from the region, and only neighbor correction uses it.

### Correction on single-block writes (server)

For `block()` and `add()` only:

1. For each horizontal neighbor (west, east, north, south), read it (from the region if it is in another chunk). Ladders are left alone. Otherwise ask the neighbor's state to update its shape against the new block (`updateShape`). If the shape changes: stage it when it is in this chunk; write it to the world with `UPDATE_CLIENTS` when its chunk is already `FULL`; otherwise drop the change (`TERR-12`). An exception from `updateShape` (seen with beehives) is ignored.
2. Then fix the new block's own connections:
   - fences, panes, bars (`CrossCollisionBlock`): connect each side when the neighbor is not air and either occludes or is not a connection exception;
   - walls: each side `LOW` or `NONE` by the same test;
   - stairs: compute `straight`, `inner_left/right` or `outer_left/right` from the stairs in front and behind, as vanilla does;
   - structure void: return `null`, so nothing is staged and the existing block shows through ("alpha channel for parts").

### Commit: `actuallyGenerate(chunk)` (server)

1. For every non-empty staged section, get the chunk's section through `BulkSectionAccess` and set each non-null staged state directly in the section (no block updates, no lighting, no block-entity creation or removal, no fluid ticks).
2. For each column with a top, raise the four heightmaps to that Y by feeding them a bedrock state at it. Heightmaps can only go up this way (`TERR-13`).
3. Clear the cache (sections and tops). The driver can be reused; `generateBuilding` commits mid-way when a floor has a second part.

### `generatePart(info, part, transform, ox, oy, oz, hardAir)` (server)

1. In edit mode (`EDITMODE`), record the part's name and Y for the chunk (01-core part editor).
2. Merge palettes: the chunk's compiled palette plus the part's local palette, if any (a new `CompiledPalette` each call).
3. Read part metadata `nowater` and `forcedair`.
4. For each (x, z) of the part with a column of characters: rotate to (rx, rz) = (ox + `transform.rotateX(x, z)`, oz + `transform.rotateZ(x, z)`); set the cursor to (rx, oy, rz); for each character from the bottom up:
   1. Look the character up; a missing entry throws (`Could not find entry … in the palette for part …`).
   2. Rotate the state when the transform is not none: blocks tagged `lostcities:rotatable` rotate by the transform's rotation; rail and powered rail states remap their shape; others are unchanged.
   3. **Air** in the part leaves the world's block as it is (cursor moves up).
   4. **Liquid** (the profile's liquid block) becomes air when `AVOID_WATER`.
   5. **Hard air** (structure void): air when `forcedair`; else by setting — `AIR`: air; `WATERLEVEL`: liquid when not `AVOID_FOLIAGE`, not `nowater` and below `waterLevel`, else air; `VOID`: kept as structure void, which the driver turns into "leave the existing block".
   6. A palette entry with extra info:
      - torch: when `GENERATE_LIGHTING`, queue the position for torch fixing; else air;
      - loot: unless the chunk is marked `noLoot`, queue a deferred job that, if the block is still not air after the commit, places the block again with `UPDATE_CLIENTS` and assigns a loot table ([post-processing](post-processing.md), 08-loot-mobs-blocks);
      - mob spawner: when `GENERATE_SPAWNERS` and not `noLoot`, choose a mob through the condition (08) and store spawner block-entity NBT (`id` `minecraft:mob_spawner`, `SpawnData`) on the chunk at the position; else air;
      - other tag data: find the block-entity type valid for the state (searching the whole registry once per block, then cached), copy the tag, set `x`, `y`, `z`, `id`, and store it as pending block-entity NBT on the chunk; a command block also gets a deferred job that marks the block changed and schedules a tick in 1 tick.
   7. A plain state in `lostcities:needspoi`: stage dirt now and queue a job that swaps the real state in (`UPDATE_NONE`) if the dirt is still there after commit (POI registration needs a real placement).
   8. A plain state in `lostcities:lights`: queue a light refresh (set air then the state again, `UPDATE_CLIENTS`).
   9. A sapling or small flower: air when the city settings avoid foliage. A sapling with `forceSaplingGrowth` (default true) queues a global server-tick job that grows it into a tree ([post-processing](post-processing.md)); without it, a deferred job sets the sapling's `stage` to 1 with full updates.
   10. Stage the state with `add` (corrected, cursor up).
5. Return `oy + part height`.

## 5 · Determinism

The driver has no randomness. `generatePart` consumes the context random only for spawner mob choice and for palette entries that pick randomly (05-assets), plus `RandomSource.fork()` for forced sapling growth. Order dependence: `TERR-12` (neighbor shapes), `TERR-09` (state stored on a neighbor's `BuildingInfo`).

## 6 · Data formats

Block-entity NBT written: the tag from the palette plus `x`, `y`, `z` (int) and `id` (string); spawners: `x`, `y`, `z`, `id` = `minecraft:mob_spawner`, `SpawnData` = `{entity: {id: <mob>}}` encoded with the 1.20 `SpawnData` codec.

## 7 · Output

Staged blocks committed in one pass per chunk; pending block-entity NBT on the chunk; torch, post-todo and global todo queues filled for later stages.

## 8 · Relationships

Every generator in every area writes through the driver (`LostCityTerrainFeature.getDriver()`). `generatePart` is called by this area's building, street and sphere code and by 04, 06 and 07 generators. Loot and spawner choice: 08-loot-mobs-blocks. Part format: 05-assets.

## 9 · 1.20 dependencies

- `BlockState` properties and `updateShape`: stair shapes, wall sides, fence and pane connections are stored states in 1.20; in 1.7.10 they are computed at render time from neighbors (fences, panes, walls) or stored in metadata (stairs have no shape property).
- Structure void (`Blocks.STRUCTURE_VOID`) as the "keep existing" marker: no such block in 1.7.10.
- `BulkSectionAccess`, `LevelChunkSection.setBlockState`, `Heightmap.Types` and `getOrCreateHeightmapUnprimed`: 1.7.10 has `ExtendedBlockStorage`, one height map and precipitation height, and light recalculation.
- `ChunkAccess.setBlockEntityNbt` (deferred block-entity creation) and the `SpawnData` codec.
- POI blocks and the delayed-placement workaround: no POI system in 1.7.10.
- `ChunkStatus.FULL` checks for neighbor writes.
- Block tags (`ROTATABLE`, `LIGHTS`, `NEEDSPOI`, `SAPLINGS`, `SMALL_FLOWERS`) and `ForgeRegistries.BLOCK_ENTITY_TYPES`.
- `Rotation` / `RailShape` transforms (05-assets documents `Transform`).

## 10 · Bugs

`TERR-09`, `TERR-12`, `TERR-13`, `TERR-14`, `TERR-15`, `TERR-36`.

## On 1.12

`dimensions/world/driver/` had `IPrimerDriver` with `SafeDriver` and `OptimizedDriver` over a 1.12 `ChunkPrimer` (a char array of block state IDs). Writes went straight into the primer before the chunk existed, so there was no commit step and no heightmap fix-up (the `Chunk` built its own height map), and fences, walls and stairs needed no correction (1.12 computes those visual states from neighbors). Torches were queued with an index and a metadata map per direction.

## Deviations in the new mod

See [new-spec 03-terrain § Chunk buffer and part writing](../../new-spec/03-terrain/index.md#chunk-buffer-and-part-writing-chunk-driver-and-partsmd).
