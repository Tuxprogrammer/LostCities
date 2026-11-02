# Stuff (decorative columns)

"Stuff" assets place short vertical columns of blocks at random spots in city chunks: hanging chains and cobwebs inside buildings by default. A city style opts in through tags. Source: `worldgen/gen/Stuff.java`, `worldgen/lost/cityassets/StuffObject.java`, the bundled `data/lostcities/lostcities/stuff/*.json`; `regassets/StuffSettingsRE.java` (codec, out of scope) and the `STUFF_BY_TAG` index in `AssetRegistries.load` (out of scope), on `f18a094d`.

## 1. Identity

| Class / member | Role |
|---|---|
| `Stuff.generateStuff(feature, info)` | Per city chunk: run every stuff asset whose tag the city style lists |
| `Stuff.actuallyGenerateStuff` | Count, attempts and placement for one asset |
| `StuffObject` | Runtime wrapper around a `StuffSettingsRE` and its id |
| `AssetRegistries.STUFF_BY_TAG` | Tag → list of stuff assets, built once at asset load |

City-style field: `stuff_tags` (list; `citystyle_common` sets `["rubble"]`, inherited by styles that inherit it: `05-assets`).

## 2. Inputs

The chunk's `BuildingInfo` (has building, candidate building id, ground level, city ground, floors, cellars), the city style's stuff tags, the main biome, the chunk's blocks as generated so far, sky visibility from the world, the compiled palette.

## 3. Numbers

| Item | Value |
|---|---|
| Random seed | `chunkX * 2570174657 + chunkZ * 101754695981` (no world seed) |
| Default `minheight` | `groundLevel`; inside a building `cityGround - cellars * 6` |
| Default `maxheight` | `minheight + 20`; inside a building `cityGround + floors * 6 + 10` |
| Count | `nextInt(maxcount - mincount) + mincount`, so `mincount .. maxcount - 1` |

### Bundled stuff assets

| Asset | Tags | Column (bottom to top) | Count | Attempts | Where | Extra filters |
|---|---|---|---|---|---|---|
| `chains` | `rubble` | `{` (palette `common`: `minecraft:chain`) | 5..9 | 8 | in buildings | block above the column must not be air (hangs from a ceiling) |
| `cobweb` | `rubble` | `\` (palette `common`: `minecraft:cobweb`) | 5..19 | 8 | in buildings | block above must not be air |
| `example` | none | `:::h` | 2..6 | 10 | outside buildings, `maxheight` 120 | `seesky: true`; biomes excluding `#is_ocean`, `#is_river`, `#is_beach`; block below in `#forge:stone` or andesite; buildings `lostcities:building1..3` |

`example` has no tags, so no city style can reach it: **unused** (a format sample).

## 4. Behavior

All (server). Runs for every city chunk, last in `LostCityTerrainFeature.doCityChunk`, after the building or street, ruins, street decorations, highways and rubble (`LostCityTerrainFeature.java:1016`). Non-city chunks never get stuff.

### `generateStuff`

1. Reseed the generation context's shared `RandomSource` with the seed above.
2. For each tag in the city style's `stuff_tags`, in list order, and each stuff asset under that tag, in index order:
   1. Skip unless `inbuilding` is set and equals `hasBuilding`. An asset without `inbuilding` never runs (SCAT-19).
   2. Skip unless the `buildings` matcher is "any" or matches the chunk's candidate building id. For street chunks that is the building that would have stood there (SCAT-20).
   3. Skip unless the `biomes` matcher accepts the chunk's main biome.
   4. `actuallyGenerateStuff`.

### `actuallyGenerateStuff`

1. Resolve `minheight`/`maxheight` (explicit values win; defaults above).
2. `count = nextInt(maxcount - mincount) + mincount` (throws when equal: SCAT-17).
3. For each of `count` columns, up to `attempts` tries:
   1. `x = nextInt(16)`, `y = nextInt(maxheight - minheight) + minheight` (throws when `maxheight <= minheight`: SCAT-18), `z = nextInt(16)`.
   2. The `blocks` matcher must accept the block at `y - 1`, and the `upperblocks` matcher the block at `y + column length`.
   3. If `seesky` is set, the world's `canSeeSky` at that position must equal it (SCAT-23).
   4. Every position of the column must currently be air.
   5. Write the column's characters bottom-up through the compiled palette and stop trying for this column.

## 5. Determinism

One shared `RandomSource` (`LegacyRandomSource`) per generation context, reseeded from coordinates only (SCAT-21). Draw order depends on the tag list and on `STUFF_BY_TAG` list order, which follows the registry's `ConcurrentHashMap` iteration (SCAT-22). Every rejected attempt consumes three draws. `canSeeSky` depends on the region's current heightmaps (SCAT-23).

## 6. Data formats

`stuff/*.json` (`StuffSettingsRE`):

| Field | Type | Default | Meaning |
|---|---|---|---|
| `tags` | list of string | empty | City-style tags that enable it |
| `column` | string | required | Palette characters, bottom to top |
| `minheight`, `maxheight` | int | computed | Block Y range for the column base (max exclusive) |
| `mincount`, `maxcount` | int | required | Columns per chunk (max exclusive) |
| `attempts` | int | required | Tries per column |
| `inbuilding` | bool | none (never runs) | Building chunks only (true) or chunks without a building (false) |
| `seesky` | bool | none (no test) | Required sky visibility |
| `biomes` | biome matcher | any | Main-biome filter |
| `blocks` | block matcher | any | Block below the column |
| `upperblocks` | block matcher | any | Block above the column |
| `buildings` | resource-location matcher | any | Building id filter |

Matcher syntax is in `05-assets`. No NBT.

## 7. Output

Chains, cobwebs (and any custom columns) in city chunks.

## 8. Relationships

- Called by: `LostCityTerrainFeature` city post-processing (`03`).
- Calls: `BiomeInfo` (`03`), `CompiledPalette` and matchers (`05`), `ChunkDriver` (`03`).

## 9. 1.20 dependencies

| Mechanism | Where | Why it matters for 1.7.10 |
|---|---|---|
| `minecraft:chain` (palette `{`) | `chains` | No chain block in 1.7.10 (added in 1.16); a pack substitute is needed |
| Block tags (`#forge:stone`) and biome tags in matchers | `example`, matchers | No tags in 1.7.10 |
| `WorldGenLevel.canSeeSky` during generation | `seesky` | 1.7.10 population has `World.canBlockSeeTheSky`, with different timing |
| `RandomSource` / `LegacyRandomSource` | shared random | `java.util.Random` in 1.7.10; whether the sequences match is *unverified* ([01-core: Equivalence with 1.7.10](../01-core/noise-and-random.md#equivalence-with-1710)) |
| Data-pack registry for stuff assets | loading | From the jar |

## 10. Bugs

SCAT-17, SCAT-18, SCAT-19, SCAT-20, SCAT-21, SCAT-22, SCAT-23.

## Deviations in the new mod

Recorded in the new spec: [07-scattered § Deviations](../../new-spec/07-scattered/index.md#deviations) and [0009](../../design/0009-scattered-structures-in-rwg-terrain.md).
