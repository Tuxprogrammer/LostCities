# Multibuilding placement

Multibuildings are buildings that span several chunks (a grid of single-chunk buildings that belong together). Random multibuildings are placed per *multichunk*, a square area of chunks, before any chunk asks for its building. Source: `worldgen/lost/MultiChunk.java`, `BuildingInfo.initMultiBuildingSection` and the multibuilding branches of `computeChunkCharacteristics` and the `BuildingInfo` constructor, on `f18a094d`. Predefined multibuildings are in [predefined-content.md](predefined-content.md).

## 1. Identity

| Member | Role |
|---|---|
| `MultiChunk` | One `areasize × areasize` block of chunks and the multibuildings placed in it |
| `MultiChunk.MB` | `(name, offsetX, offsetZ)` stored per occupied chunk |
| `MultiChunk.getOrCreate` | Cached per multichunk (`MULTICHUNKS`, `TimedCache`) |
| `calculateBuildings`, `canPlaceBuilding`, `placeBuilding` | The placement algorithm |
| `dump` | Debug print to `System.out`; **unused** (dead code) |
| `MultiPos` (API record) | `(x, z, w, h)`; `SINGLE = (-1, -1, 1, 1)` |

World style asset: `multisettings` (`MultiSettings`: `areasize`, `minimum`, `maximum`, `correctstylefactor`, `attempts`). City style: `multibuildings` list with weights. Profile: `MULTI_BUILDING_STREET_CONFLICT`, `MULTI_USE_CORNER`. World settings: `railwayavoidance`, `railpartheight6`.

## 2. Inputs

Multichunk coordinates, world style, profile (the dimension's main profile, not per chunk), city styles of every chunk in the area, raw city flags, highways, railways, the street planner, predefined occupancy, building assets (max cellars).

## 3. Numbers

| Field | Default (code) | `standard` world style | Meaning |
|---|---|---|---|
| `areasize` | 10 | 10 | chunks per multichunk side |
| `minimum` | 1 | 1 | fewest multibuildings attempted per multichunk |
| `maximum` | 5 | 5 | most attempted |
| `correctstylefactor` | 0.8 | 0.8 | share of footprint chunks that must have the building's city style |
| `attempts` | 50 | 50 | random positions tried per multibuilding |
| Random seed | `mcX * 797013493 + mcZ * 295085213` | | `java.util.Random`, no world seed |
| `MULTI_BUILDING_STREET_CONFLICT` | `OVERRIDE_MINOR` | | `BLOCK_ALL`, `OVERRIDE_MINOR`, `OVERRIDE_ALL` |

## 4. Behavior

All (server).

### Multichunk coordinate

`mc = (floorDiv(chunkX, areasize), floorDiv(chunkZ, areasize))`; its top-left chunk is `mc * areasize`. The chunk's slot is `chunk - topLeft`.

### `calculateBuildings`

1. `rand = new Random(mcX * 797013493L + mcZ * 295085213L)` (no world seed, no dimension: CITY-02).
2. `cnt = minimum + nextInt(maximum - minimum + 1)`. If `cnt <= 0`, stop: no multibuildings. A `maximum` below `minimum` throws (CITY-25).
3. `cityLevel` = the city level of the multichunk's top-left chunk (used for every candidate position: CITY-26).
4. Count the city style of every chunk in the area (`City.getCityStyle` with the main profile). A `null` style throws `"Cannot find city style for chunk"`.
5. Sort the distinct styles by name. For `i` in `0 .. cnt-1`: pick a style weighted by its chunk count (one `nextFloat`; may return `null`: CITY-09), then a multibuilding from that style's list (one `nextFloat`). An unknown multibuilding throws. Remember the style per multibuilding.
6. Sort the chosen multibuildings by `dimX + dimZ`, largest first (stable sort, so equal sizes keep pick order).
7. For each multibuilding:
   1. `maxCellars` = the largest `maxcellars` among its buildings (an unknown building throws).
   2. Up to `attempts` times: `x = nextInt(areasize - dimX + 1)`, `z = nextInt(areasize - dimZ + 1)` (throws if the multibuilding is larger than the area: CITY-25). If `canPlaceBuilding`, write `MB(name, xx, zz)` into every footprint slot and stop trying.

Multibuildings never cross a multichunk border.

### `canPlaceBuilding`

For every chunk of the footprint, reject the position if:
1. the slot is already taken;
2. predefined content occupies the chunk;
3. hierarchical street mode and the conflict policy blocks the chunk's **raw** road type: `BLOCK_ALL` blocks any road, `OVERRIDE_MINOR` only primary roads, `OVERRIDE_ALL` nothing. Unblocked roads are later removed by the effective-road rule ([street-planner.md](street-planner.md));
4. a surface railway or station is there, the chunk is not raw city, or a highway is there;
5. a railway is there, avoidance is not `block_railway`, and `min(cityLevel - railLevel - railpartheight6, maxCellars) < maxCellars`, i.e. the cellars would not fit above the rail.

Count the footprint chunks whose city style equals the multibuilding's style. Reject if that count is below `dimX * dimZ * correctstylefactor`.

### Use by chunks

- [chunk-characteristics.md](chunk-characteristics.md): an occupied slot gives `multiPos = (offsetX, offsetZ, dimX, dimZ)`; the candidate building is the multibuilding's building at that offset. The planned road is removed. `checkBuildingPossibility` returns true. The chunk's city level is the footprint average or the top-left level.
- Structure avoidance treats the whole footprint as one decision (`07-scattered`).
- [building-info.md](building-info.md): non-top-left chunks copy floors, cellars, street fields, door, bridge and stair parts, palette, loot and ruin decisions from the top-left `BuildingInfo`; parts per floor are still drawn per chunk. Prefers-lonely is skipped. `noLoot` is always false for multibuildings.
- A chunk in the last column (`x == w-1`) never has a connection on its west side, and a chunk in the last row (`z == h-1`) never on its north side; see [doors-and-connections.md](doors-and-connections.md).

## 5. Determinism

| Random | Seed | Draws |
|---|---|---|
| Multichunk | `mcX * 797013493L + mcZ * 295085213L` | 1 `nextInt` count; per multibuilding 2 `nextFloat`; per attempt 2 `nextInt` |

No world seed (CITY-02): with the same terrain mask, multibuilding positions repeat in every world and every dimension. The style list is sorted by name before weighted picks, so `HashMap` order does not leak in. The result depends on `getCityStyle`, whose cache ignores the profile (CITY-06). Cached in a `TimedCache`; recomputation is identical.

## 6. Data formats

`multisettings` in the world style (fields above; codec defaults `correctstylefactor` 0.8, `attempts` 50; `areasize`, `minimum`, `maximum` required). City-style `multibuildings` and multibuilding definitions in `05-assets`. No NBT.

## 7. Output

A grid of `MB` entries per multichunk, read by `initMultiBuildingSection`.

## 8. Relationships

- Called by: `BuildingInfo.initMultiBuildingSection`.
- Calls: `City.getCityStyle`, `City.isChunkOccupied`, `BuildingInfo.isCityRaw`, `getCityLevel`, `hasHighway`, `Railway.getRailChunkType` (`06`), `HierarchicalStreetPlanner.getRoadType`, `CityStyle.getRandomMultiBuilding` (`05`).

## 9. 1.20 dependencies

| Mechanism | Where | Why it matters for 1.7.10 |
|---|---|---|
| Data-pack world style and multibuilding assets | `MultiSettings` codec | Load from the jar |
| Java records (`MB`, `MultiPos`, `MultiSettings`) | throughout | Not allowed on Java 8 |

## 10. Bugs

CITY-02, CITY-06, CITY-09, CITY-25, CITY-26.

## Deviations in the new mod

See [new-spec 04-city-layout § Buildings](../../new-spec/04-city-layout/index.md#buildings-building-infomd-multibuildingsmd).
