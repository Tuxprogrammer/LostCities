# Structure avoidance

Keeps cities off vanilla villages and a configurable list of other structures, optionally with a one-chunk ring around them, and optionally leaves the terrain unflattened where a structure was avoided. It reads only structure references already present in the active world-generation region; it never loads or generates chunks. Source: `worldgen/lost/StructureAvoidance.java`; the avoidance keys in `setup/Config.java` (out of scope, read for behavior); its uses in `BuildingInfo` and `LostCityTerrainFeature.doNormalChunk`, on `f18a094d`.

## 1. Identity

| Member | Role |
|---|---|
| `StructureAvoidance.check(coord, multiPos, level)` | Classify a chunk (or a multibuilding footprint) |
| `Result` | `NONE`, `DIRECT` (avoided structure in this chunk), `ADJACENT` (elsewhere in the window or footprint), `UNKNOWN` (references missing); `avoidsCity()` for `DIRECT`/`ADJACENT`; `isKnown()` for all but `UNKNOWN` |
| `MULTIBUILDING_DECISIONS` | Unbounded cache of footprint decisions |
| `resolveFootprint`, `hasAvoidedStructure` | Helpers |
| `cleanCache` | Clears the footprint cache (called from `BuildingInfo.cleanCache`) |

Server config (`Config.SERVER_BUILDER`, a `ForgeConfigSpec`):

| Key | Default | Meaning |
|---|---|---|
| `avoidStructures` | `minecraft:mansion`, `minecraft:jungle_pyramid`, `minecraft:desert_pyramid`, `minecraft:igloo`, `minecraft:swamp_huts`, `minecraft:pillager_outpost` | Structure ids to avoid |
| `avoidStructuresAdjacent` | false | Also avoid chunks next to those structures |
| `avoidVillages` | true | Avoid structures tagged `#minecraft:village` |
| `avoidVillagesAdjacent` | false | Also avoid chunks next to villages |
| `avoidFlattening` | true | Do not flatten terrain where a structure was avoided |

## 2. Inputs

Chunk coordinates, the multibuilding position, the `WorldGenLevel` (only a `WorldGenRegion` is inspected), chunks at status `STRUCTURE_REFERENCES` or later inside that region, the structure registry and its village tag, the config.

## 3. Numbers

| Item | Value |
|---|---|
| Margin | 1 chunk when any adjacent check is active, else 0 |
| Window (multibuilding) | footprint expanded by the margin |
| Window (single chunk) | see § 4 step 3 and SCAT-01 |

## 4. Behavior

All (server).

### `check`

1. `terrainTransition = avoidFlattening && (avoidVillages || avoidStructures non-empty)`. `checkAdjacent = terrainTransition || avoidVillagesAdjacent || avoidStructuresAdjacent`.
2. Nothing to avoid (`!checkAdjacent && !avoidVillages && avoidStructures empty`): `NONE`. Not a `WorldGenRegion` (for example a fully loaded level, or the GUI): `UNKNOWN`.
3. Footprint bounds: `minX = chunkX - multiPos.x`, `maxX = minX + multiPos.w - 1` (same for Z). For a single chunk `multiPos` is `SINGLE = (-1, -1, 1, 1)`, so the "footprint" becomes the chunk at `(chunkX + 1, chunkZ + 1)` (SCAT-01).
4. **Multibuilding**: look up or compute the footprint decision (key: dimension, bounds, margin). `ALLOW` gives `NONE`. `AVOID` gives `DIRECT` if this chunk's own references (when present) contain an avoided structure (counted as inside the footprint), else `ADJACENT`.
5. **Single chunk**: for every chunk in the window `[min - margin, max + margin]`:
   1. A chunk not yet at `STRUCTURE_REFERENCES` in the region marks references missing and is skipped.
   2. Inside-footprint flag: the chunk lies within `[min, max]`.
   3. If it holds an avoided structure: return `DIRECT` when it is the asked chunk, else remember "found elsewhere".
6. Missing references: `UNKNOWN`. Otherwise `ADJACENT` if found elsewhere, else `NONE`.

With the defaults (`avoidFlattening` and `avoidVillages` on) the margin is 1 and, because of SCAT-01, the single-chunk window is the 3 × 3 block from `(chunkX, chunkZ)` to `(chunkX + 2, chunkZ + 2)`: the asked chunk is its north-west corner, not its center.

### `resolveFootprint` (multibuildings)

Scan the expanded footprint. Chunks without references are skipped (fail open). The first chunk with an avoided structure gives `AVOID`; otherwise `ALLOW`. The decision is cached until `cleanCache`, even if it was made before all references existed (SCAT-02).

### `hasAvoidedStructure(chunk, insideFootprint, terrainTransition)`

1. No structure references: false.
2. For each referenced structure with at least one reference:
   - village: `avoidVillages` and (inside, or `terrainTransition`, or `avoidVillagesAdjacent`) and the structure is in `#minecraft:village`: true;
   - listed: (inside, or `terrainTransition`, or `avoidStructuresAdjacent`) and the id is in `avoidStructures`: true.

With `terrainTransition`, structures in the margin ring count even when both adjacent options are off; the ring stays city-free so normal terrain correction can blend into the structure.

The `avoidStructures` set is built once per JVM from the config and never refreshed (SCAT-16).

### Effects

| Where | Effect |
|---|---|
| `computeChunkCharacteristics` (`04`) | `DIRECT` or `ADJACENT`: the chunk is not city and has no building; re-applied after the characteristics event. A result of `UNKNOWN` makes the characteristics non-cacheable |
| `BuildingInfo` constructor (`04`) | Re-checks and stores the result; non-cacheable while `UNKNOWN`; a city-sphere center override only applies when the chunk does not avoid |
| `doNormalChunk` (`03`) | Terrain correction is skipped for a `DIRECT` chunk when `avoidFlattening` is on |

## 5. Determinism

The result depends on which neighbor chunks already have structure references inside the current `WorldGenRegion`, which depends on generation timing. Single chunks stay `UNKNOWN` (and uncached) until their window is complete. Multibuilding footprints commit on first sight and fail open (SCAT-02). `MULTIBUILDING_DECISIONS` is unbounded (SCAT-03).

## 6. Data formats

Server config keys above. No JSON, no NBT.

## 7. Output

A `Result` per chunk, used for city membership and terrain flattening.

## 8. Relationships

- Called by: `BuildingInfo.computeChunkCharacteristics`, the `BuildingInfo` constructor (`04-city-layout`).
- Consumed by: `LostCityTerrainFeature.doNormalChunk` (`03`).
- Config owned by `01-core`.

## 9. 1.20 dependencies

| Mechanism | Where | Why it matters for 1.7.10 |
|---|---|---|
| Structure references on `ChunkAccess` (`getAllReferences`, `hasAnyStructureReferences`) and `ChunkStatus.STRUCTURE_REFERENCES` | the whole check | 1.7.10 has no per-chunk references; structures live in each `MapGenStructure`'s map of structure starts (the exact query to use is for `gtnh-integration-research` and the new spec) |
| `WorldGenRegion.getChunk(x, z, status, false)` | non-loading lookups | No region view; must not trigger generation (cascading rule) |
| Structure registry and `StructureTags.VILLAGE` | village test | `MapGenVillage` only; no tags |
| Structure ids (mansion, igloo, pillager outpost) | defaults | Do not exist in 1.7.10; temples and witch huts come from `MapGenScatteredFeature` |
| `ForgeConfigSpec` server config | keys | Forge `Configuration` or GTNHLib config in 1.7.10 |

## 10. Bugs

SCAT-01, SCAT-02, SCAT-03, SCAT-04, SCAT-16.

## Deviations in the new mod

Recorded in the new spec: [07-scattered § Deviations](../../new-spec/07-scattered/index.md#deviations) and [0009](../../design/0009-scattered-structures-in-rwg-terrain.md).
