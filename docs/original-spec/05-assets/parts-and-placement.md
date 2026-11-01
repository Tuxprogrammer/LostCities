# Parts, part placement and transforms

A part is a small 3D block template written as palette characters. Buildings stack parts floor by floor; streets, parks, bridges, highways, railways, stations, monorails, stairs and scattered structures are single parts. This sheet covers the part format, how a part is turned into blocks (`LostCityTerrainFeature.generatePart`, the asset-facing half of chunk writing), and the transforms that rotate or mirror parts.

| # | Field | Contents |
|---|---|---|
| 1 | **Identity** | `regassets/BuildingPartRE` (raw), `regassets/data/PartMeta` (metadata entry), `cityassets/BuildingPart` (compiled; implements `IBuildingPart`), `worldgen/lost/Transform` (rotation enum), `LostCityTerrainFeature.generatePart`, `computePalette`, `transformBlockState`, `handleLoot`, `handleSpawner`, `handleBlockEntity`, `handleTodo`, `fixTorches`. Meta keys are constants in `api/ILostCities` (`META_DONTCONNECT`, `META_SUPPORT`, `META_Z_1`, `META_Z_2`, `META_NOWATER`, `META_FORCEDAIR`). Profile options read: `EDITMODE`, `AVOID_WATER`, `AVOID_FOLIAGE`, `GENERATE_LIGHTING`, `GENERATE_SPAWNERS`, `GENERATE_LOOT`, `CHEST_WITHOUT_LOOT_CHANCE`, `GROUNDLEVEL`; common config `FORCE_SAPLING_GROWTH`. |
| 2 | **Inputs** | The part's slices and metadata; the chunk's compiled palette (`BuildingInfo.getCompiledPalette()`, from the style plus the building's local palette); the part's own local or referenced palette; the transform; the origin offset `(ox, oy, oz)` in chunk-local blocks; the hard-air setting; `info.waterLevel`; `info.noLoot`; the block tags `lostcities:rotatable`, `lostcities:lights`, `lostcities:needspoi`, `minecraft:saplings`, `minecraft:small_flowers`. |
| 3 | **Numbers** | Footprint `xsize` × `zsize` blocks (no enforced maximum; 16 × 16 is the chunk). Height = number of slices, one block each. `FLOORHEIGHT` = 6 blocks. Transforms use the constant 15 (chunk width − 1). Bundled parts: 207; footprints from 2 × 13 to 16 × 16, heights 1 to 24 slices. |
| 4 | **Behavior** | See [Part placement](#part-placement). |
| 5 | **Determinism** | Placement itself has no random choice except through the palette: a random-list character draws `GenerationContext.nextPaletteIndex()`, a per-chunk LCG (`seed = 214013·seed + 2531011`, index `(seed >> 16) & 127`) whose start is mixed from the world seed, dimension id hash and chunk coordinates (`GenerationContext.open`). The draw order is the placement order (x outer, z inner, y up), so the same chunk and the same sequence of parts gives the same blocks. Spawner mobs and loot use `GenerationContext.current().random()` (same per-chunk seed). Code that runs outside world generation (editor, commands) uses a thread-local fallback context seeded with 123456789. |
| 6 | **Data formats** | [Part JSON](#part-json) and [Part metadata](#part-metadata). |
| 7 | **Output** | Blocks written through the `ChunkDriver` into the chunk being generated; block-entity NBT for spawners and `tag` entries; post-generation to-dos (loot chests, POI blocks, command blocks, sapling growth); torch to-dos fixed after the chunk; for edit mode, a record of which part went where (`EditModeData`). Returns `oy + slices`, the Y just above the part. |
| 8 | **Relationships** | Called by building generation (one main part per floor plus an optional `parts2` overlay), streets, parks, fountains, fronts, stairs, highways, railways, stations, monorails (`06`), rail dungeons and scattered structures (`07`), and city-sphere centers (`03`). Uses [palettes-and-variants.md](palettes-and-variants.md) for characters and [conditions.md](conditions.md) for loot and mobs. |
| 9 | **1.20 dependencies** | `BlockState.rotate(Rotation)`, `RailShape`, block tags, `STRUCTURE_VOID`, block-entity NBT via `ChunkAccess.setBlockEntityNbt`, `BlockEntityType` discovery, `SpawnData` codec, loot-table ids, POI system, `SaplingBlock.advanceTree`. See the [index table](index.md#120-dependencies). |
| 10 | **Bugs** | [ASSET-16](index.md#suspected-bugs), [ASSET-17](index.md#suspected-bugs), [ASSET-18](index.md#suspected-bugs) (transforms), [ASSET-24](index.md#suspected-bugs) (slice validation), [ASSET-05](index.md#suspected-bugs) and [ASSET-29](index.md#suspected-bugs) (loot and spawner conditions), [ASSET-26](index.md#suspected-bugs) (part palette recompiled per placement), [ASSET-32](index.md#suspected-bugs) (empty part lists). |

## Part JSON

| Field | Type | Default | Meaning | Validation |
|---|---|---|---|---|
| `xsize` | int | required | Width along X, in blocks | none; should equal each row's length |
| `zsize` | int | required | Depth along Z, in blocks | none; should equal the number of rows per slice |
| `slices` | list of list of string | required | One entry per Y level, bottom first. Each entry is a list of `zsize` rows (Z = 0 first); each row is a string of `xsize` palette characters (X = 0 first). | none. Rows are concatenated, so a short or long row shifts every later character; a slice shorter than `xsize·zsize` throws `StringIndexOutOfBoundsException` at generation ([ASSET-24](index.md#suspected-bugs)) |
| `refpalette` | string | none | Name of a palette asset layered over the chunk palette for this part only | resolved lazily with `getOrThrow` on first placement; ignored if `palette` is also present |
| `palette` | object | none | An inline palette (same format as a palette file: `{"palette":[...]}`), compiled when the part is compiled | errors in it fail the part's compilation |
| `meta` | list of objects | none | Typed key/value metadata ([below](#part-metadata)) | none |

Example (abridged):

```json
{ "xsize": 16, "zsize": 16, "refpalette": "rails",
  "meta": [ { "key": "nowater", "boolean": true } ],
  "slices": [ [ "################", "...15 more rows" ], [ "..." ] ] }
```

The compiled part stores the slices as one string per Y (`index = z·xsize + x`) and lazily builds *vertical slices*: one `char[]` per column `(x, z)` holding that column bottom to top, or null when the whole column is spaces. Generation walks columns, so an all-space column costs nothing.

## Part metadata

Each `meta` entry has `key` and one value field. The first present field wins, in this order: `integer`, `float`, `boolean`, `char` (first character of the string), `string`. Readers cast to the type they expect, so a wrong value type throws `ClassCastException`, except `getMetaBoolean`, which treats anything that is not a boolean as false.

| Key | Type | Read by | Meaning | Bundled use |
|---|---|---|---|---|
| `dontconnect` | boolean | `BuildingInfo`, `Doors` | No doorway or corridor connection is made into this floor | 15 shopping parts |
| `support` | char | `Bridges`, `Highways` | Palette character for the support pillar under a bridge or highway part (overrides the city or world style's `bridgesupport`/`highwaysupport`) | none |
| `z1`, `z2` | integer | `LostCityTerrainFeature` (stairs and street slopes) | Z range (inclusive) of the stair opening, used to keep the adjacent border clear | 6 stair parts |
| `nowater` | boolean | `generatePart` | Hard air below the water level stays air instead of becoming water | 3 monorail parts |
| `forcedair` | boolean | `generatePart` | Hard air always becomes air, whatever the hard-air setting | none |

## Part placement

`generatePart(info, part, transform, ox, oy, oz, hardAirSetting)`, all steps (server):

1. If the profile is in edit mode, record `(chunk, oy, part name)` in `EditModeData`.
2. Compile the palette for this part: start from the chunk's compiled palette; if the part has a local or referenced palette, layer it on top (`new CompiledPalette(chunkPalette, partPalette)`). This is rebuilt on every call; there is no cache.
3. Read the part's `nowater` and `forcedair` flags.
4. For each column `x` in `0..xsize-1`, `z` in `0..zsize-1` with a non-null vertical slice:
   1. Destination `rx = ox + transform.rotateX(x, z)`, `rz = oz + transform.rotateZ(x, z)` (chunk-local). Start at `oy`.
   2. For each character `c` from the bottom:
      1. `b = palette.get(c)` (a random-list character draws the next palette index). If `b` is null, throw `RuntimeException("Could not find entry 'c' in the palette for part 'name'!")`.
      2. `inf = palette.getInfo(c)` (loot, mob, torch or NBT information, see [palettes-and-variants.md](palettes-and-variants.md#information-characters)).
      3. If the transform is not `ROTATE_NONE`, transform the block state ([Transforms](#transforms)).
      4. If `b` is air: **leave the existing block** and move up. A space in a part therefore means "don't touch", not "make air".
      5. Else if `b` is the profile's liquid block: if the profile has `AVOID_WATER`, write air instead.
      6. Else if `b` is `minecraft:structure_void` ("hard air"):
         - with `forcedair`: write air;
         - `HardAirSetting.AIR`: write air;
         - `WATERLEVEL`: write the liquid if `!AVOID_FOLIAGE && !nowater && oy + y < info.waterLevel`, else air;
         - `VOID`: pass `structure_void` to the chunk driver, which drops it, so the block already there is kept (street sections use this).
      7. Else if `inf` is not null, in this order (the first that applies wins):
         - torch: with `GENERATE_LIGHTING`, write the block and queue a torch fix-up at this position; without it, write air.
         - loot (non-empty): write the block now, and if the building is not `noLoot`, queue a post-to-do that, if the position is not air at that time, sets the block again (`UPDATE_CLIENTS`) and fills it with loot.
         - mob (non-empty): if `GENERATE_SPAWNERS` and not `noLoot`, write the block and attach spawner NBT; otherwise write air.
         - tag: write the block and attach the NBT.
      8. Else if `b` is a state of a block tagged `lostcities:needspoi` (brewing stand, cauldron, barrel, blast furnace, smoker, composter, fletching table, lectern, stonecutter, loom, smithing table, grindstone): write `minecraft:dirt` now and queue a post-to-do that replaces the dirt with `b` (`UPDATE_NONE`) if it is still dirt.
      9. Else if `b` is tagged `lostcities:lights`: write it and mark the position for a client lighting update.
      10. Else if `b` is a sapling or small flower ([handleTodo](#saplings-and-flowers)): apply that handling, then write.
      11. Otherwise write `b`.
5. Return `oy + slices`.

Every write goes through `ChunkDriver.correct` (`03-terrain`), which recomputes neighbor-dependent properties from the blocks around the position: the four connection sides of fences, panes and iron bars (`CrossCollisionBlock`), the four sides of walls, and the `shape` of stairs. `structure_void` is dropped there. So those properties in a palette are only starting values.

After the whole chunk is generated, `fixTorches` replaces each queued torch: a standing torch if the block below is not air; otherwise a wall torch facing away from the first non-air neighbor in the order west, east, north, south (skipping neighbors outside the chunk). If none applies the palette's block stays. So the torch character's own facing in the palette does not matter when lighting is on.

### Loot

When the post-to-do runs (server), `generateLoot` checks the block entity: if it is a `RandomizableContainerBlockEntity` and the profile has `GENERATE_LOOT`, `createLoot` runs; if there is no block entity at all it logs an error. `createLoot` skips the chest with probability `CHEST_WITHOUT_LOOT_CHANCE`, else evaluates the loot condition named by the palette's `loot` ([conditions.md](conditions.md)) with this context: level `(y − GROUNDLEVEL) / 6`, floor `(y − cityGroundLevel) / 6` (both truncating toward zero, [ASSET-05](index.md#suspected-bugs)), cellars and floors of the building, part = this part's name, below part `<none>`, building = the building's name or `<none>`, sphere and biome looked up at the position. The chosen string is used as a loot-table id with `RandomizableContainerBlockEntity.setLootTable`. A condition that matches nothing gives a null id and crashes ([ASSET-29](index.md#suspected-bugs)).

### Spawners

`handleSpawner` (server) evaluates the mob condition named by `mob` with the same context as loot, using the per-chunk random. A null result throws "Condition '…' did not return a valid mob!". The NBT written is `{x, y, z, id: "minecraft:mob_spawner", SpawnData: {entity: {id: <mob id>}}}` (`SpawnData` encoded by its codec). The mob id is not checked against the entity registry.

### Block-entity NBT (`tag`)

`handleBlockEntity` finds the block-entity type that accepts the state (first match over the whole block-entity registry, cached per block); if none, it logs a warning and writes the plain block. Otherwise it copies the palette NBT, adds `x`, `y`, `z` and `id` (the type's registry id) and attaches it to the chunk. For a `minecraft:command_block` it also queues a post-to-do that marks the block changed and schedules a block tick one tick later. The only bundled `tag` entry is `;` in palette `default` (a furnace with 10 coal), which no part uses.

### Saplings and flowers

Applies to states of blocks in `#minecraft:saplings` and `#minecraft:small_flowers`. If the chunk's effective `avoidFoliage` is on, the block becomes air. Otherwise, for a sapling:
- with `FORCE_SAPLING_GROWTH`: queue a global to-do that, once the area is loaded and the sapling is still there, grows it with `advanceTree` using a forked copy of the per-chunk random;
- without it: queue a post-to-do that sets the sapling with `stage=1` (`UPDATE_ALL_IMMEDIATE`).

Flowers are written unchanged.

## Transforms

`Transform` maps a part's local `(x, z)` to chunk-local coordinates and tells block states how to turn. All formulas assume a 16 × 16 footprint: smaller parts rotate about the chunk, not about their own footprint.

| Transform | `rotateX(x, z)` | `rotateZ(x, z)` | Block-state rotation used | What it really is |
|---|---|---|---|---|
| `ROTATE_NONE` | x | z | none (states untouched) | identity |
| `ROTATE_90` | 15 − z | x | `CLOCKWISE_90` | 90° clockwise seen from above |
| `ROTATE_180` | 15 − x | 15 − z | `CLOCKWISE_180` | 180° |
| `ROTATE_270` | z | 15 − x | `COUNTERCLOCKWISE_90` | 90° counter-clockwise |
| `MIRROR_X` | 15 − x | z | `CLOCKWISE_180` | mirror across the X axis midline (a reflection) |
| `MIRROR_Z` | x | 15 − z | `CLOCKWISE_180` | mirror across the Z axis midline |
| `MIRROR_90_X` | z | x | `CLOCKWISE_90` | transpose (swap X and Z) |

`getOpposite()` swaps 90 and 270 and leaves the others. `randomRotation()` picks one of the four rotations with `Math.random()` (unseeded); nothing in the pinned code calls it.

Block states are transformed only for (server):

- blocks in `#lostcities:rotatable` (`#minecraft:stairs`, `#minecraft:doors`): `BlockState.rotate(transform's rotation)`. For the three mirror transforms that is a rotation, not a reflection ([ASSET-17](index.md#suspected-bugs));
- `minecraft:rail` and `minecraft:powered_rail` states: the `shape` property is remapped by the table in `Transform.transform(RailShape)`. Some combinations are missing and throw `IllegalStateException("Cannot happen!")` (`MIRROR_Z` on `ascending_east`/`ascending_west`), and `MIRROR_90_X` leaves `east_west` unchanged ([ASSET-16](index.md#suspected-bugs)).

Every other directional block (ladders, levers, chests, furnaces, barrels, trapdoors, wall-mounted redstone torches) keeps its palette facing in a rotated part ([ASSET-18](index.md#suspected-bugs)). Torch characters are re-oriented afterwards by `fixTorches`.

Bridge parts do not go through `generatePart`. `Bridges.generateBridge` (`06-highways-railways`) writes them itself over the full 16 × 16 at the bridge level: it transposes X and Z for the Z orientation without transforming block states, writes air instead of skipping it, and honors only the torch information (no loot, spawners, NBT, POI or sapling handling). Bridge and highway support parts likewise use their own writer, `Supports.generatePart` (`06`).

Transforms used at the pin (`generatePart` and `Supports.generatePart` call sites): building floors and overlays, parks, fountains, the full street square, rail dungeons, the monorail crossing and sphere center parts are placed with `ROTATE_NONE`; street sections, street slopes, minor-street connectors, stairs and building fronts are rotated by their direction; highways use the four rotations; railways and stations use `MIRROR_X` among others; monorails use `ROTATE_90`, `MIRROR_90_X` and `MIRROR_Z`; bridge support parts use `MIRROR_90_X` on the Z orientation; rotatable scattered structures use the four rotations (`07-scattered`).

## On 1.12

`upstream/1.12` parts use the same `xsize`/`zsize`/`slices`/`refpalette`/`palette`/`meta` fields inside a single array file with `"type": "part"`. Its rotation works on `IBlockState.withRotation`, the same idea with metadata-backed states, so it is the nearer model for 1.7.10 metadata rotation tables.
