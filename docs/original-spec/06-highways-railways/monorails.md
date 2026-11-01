# Monorails

Elevated single tracks that link city spheres in the `space` landscape, with a station where a line enters a sphere.

| # | Field | Contents |
|---|---|---|
| 1 | **Identity** | `worldgen/gen/Monorails` (`generateMonorails`), asset data `worldgen/lost/regassets/data/MonorailParts` (world style `parts.monorails`). Line placement lives in `CitySphere.hasHorizontalMonorail`, `hasVerticalMonorail`, `hasMonorailStation`, `fullyInsideCitySpere` (`03-terrain`, read for context). Profile options `monorailChance` (`CITYSPHERE_MONORAIL_CHANCE`), `monorailOffset` (`CITYSPHERE_MONORAIL_HEIGHT_OFFSET`). |
| 2 | **Inputs** | The chunk's `BuildingInfo` (cached monorail flags, neighbors, city style), city spheres and their four monorail candidate flags, the profile's ground level and monorail offset. |
| 3 | **Numbers** | See [Numbers](#numbers). |
| 4 | **Behavior** | See [Behavior](#behavior). |
| 5 | **Determinism** | No random draws in this class; sphere candidate flags come from each sphere's seeded `Random`. |
| 6 | **Data formats** | `MonorailParts` JSON. |
| 7 | **Output** | `monorails_vertical` track parts between spheres, `monorails_both` where two lines cross outside spheres, `monorails_station` (with fill below) just inside a sphere. |
| 8 | **Relationships** | Called only from `gen/Spheres` for `space` profiles (`03-terrain`). `BuildingInfo.isCityRaw` treats station chunks as non-city; bridges are not built on monorail chunks in space (`04-city-layout`). |
| 9 | **1.20 dependencies** | Y range of `GROUNDLEVEL + monorailOffset`; codecs. |
| 10 | **Bugs** | none found. |

## Numbers

| Item | Value | Notes |
|---|---|---|
| Monorail Y | `GROUNDLEVEL + monorailOffset` | Default 71 − 2 = 69; offset range -100..100 |
| `monorailChance` | 0.8 | 0..1, per sphere per direction; `biosphere` and `biosphere_caves` set 0 |
| Horizontal (east-west) lines | chunk rows with `chunkZ & 15 == 8` | The sphere-center rows of the 16-chunk sphere grid |
| Vertical (north-south) lines | chunk columns with `chunkX & 15 == 8` | |
| Line scan range | 63 chunks each way | Candidate centers where the scanned coordinate `& 15 == 8` |
| "Fully inside a sphere" | all four chunk corners within sphere radius − 2 blocks of the center | `CitySphere.fullyInsideCitySpere` |

Bundled parts (16 × 16, `05-assets`): `monorails_both` (10 high), `monorails_vertical` (10), `monorails_station` (4); each has meta `nowater = true`.

## Behavior

### Where lines run (server, `CitySphere`)

1. (server) Each enabled sphere draws four flags, in order north, south, west, east, each `nextFloat() < monorailChance`, from the sphere's own `Random(seed + centerX × 961744153 + centerZ × 837971201)` after its radius and center draws.
2. (server) A chunk on a line row has a horizontal monorail when, scanning east, the first enabled sphere found wants a west connection, and, scanning west, the first enabled sphere found wants an east connection. If either scan finds no sphere, or the first sphere declines, there is no line.
3. (server) Vertical lines work the same on line columns: the first sphere to the south must want a north connection and the first to the north a south connection.
4. (server) A chunk fully inside a sphere has a monorail station when one of its four neighbors has a monorail and is not fully inside a sphere. Station chunks are excluded from cities (`BuildingInfo.isCityRaw`).

### Placing parts (server, `Monorails.generateMonorails`)

1. (server) Read the chunk's horizontal and vertical monorail flags (cached on `BuildingInfo`).
2. (server) Both: if the chunk does not intersect a sphere, generate `both` unrotated at monorail Y with hard-air mode `WATERLEVEL`. Stop either way.
3. (server) Only horizontal: rotation 90°. Only vertical: no rotation. Neither: stop.
4. (server) If the chunk is fully inside a sphere, it becomes a station when a neighbor has a monorail that is not fully inside; neighbors are checked west, east, north, south, and the first match decides the transform: west → `MIRROR_90_X`, east → `ROTATE_90`, north → `ROTATE_NONE`, south → `MIRROR_Z`. Before placing, fill the chunk from monorail Y down to the ground with the city style's border block (`fillToGround`, `04-city-layout`). With no such neighbor, nothing is placed.
5. (server) Otherwise (not fully inside) use the `vertical` part with the rotation from step 3.
6. (server) Generate the part at monorail Y with `WATERLEVEL`.

Only `space` profiles call this (`gen/Spheres`, after the sphere shell). `LostCityTerrainFeature` has the call commented out.

## Determinism

- No random draws here. Candidate flags come from per-sphere `Random`s seeded with the world seed and the sphere center (`03-terrain`).
- `BuildingInfo` caches the two flags in volatile fields; recomputing gives the same value.
- Line scans read sphere data only, never chunks.

## Data formats

`MonorailParts` (world style `parts.monorails`; owned by `05-assets`):

| Field | Type | Default |
|---|---|---|
| `both` | string | `monorails_both` |
| `vertical` | string | `monorails_vertical` |
| `station` | string | `monorails_station` |

Unlike highway and railway parts, each field takes one part name, not a list. A selector equal to the default is not written back.

## Output

Track parts at monorail Y between spheres, crossing parts where lines cross outside spheres, and station parts with a filled base just inside spheres.

## Relationships

- Called by `gen/Spheres` (`03-terrain`).
- Reads `CitySphere` (`03-terrain`), `BuildingInfo` neighbors and city style (`04-city-layout`), `LostCityTerrainFeature.generatePart`, `fillToGround` (`04-city-layout`).

## 1.20 dependencies

| Mechanism | Where | Why it matters for 1.7.10 |
|---|---|---|
| Y range | `GROUNDLEVEL + monorailOffset`, offset -100..100 | Can fall outside 1.7.10's 0..255. |
| DFU codecs | `MonorailParts` | Plain JSON in 1.7.10. |

Design record 0003 targets the overworld; whether `space` profiles and monorails come over is for the new spec.

## Bugs

None found.

## Deviations in the new mod

None recorded yet; the new spec decides.
