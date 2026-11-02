# Chunk characteristics

The second planning stage: for one chunk, is it city, is it part of a multibuilding, what is its city level and planned road type, could it hold a building, what is its city style and which building type is its candidate. Source: `BuildingInfo.computeChunkCharacteristics`, `isCityRaw`, `checkBuildingPossibility`, `getEffectivePlannedRoadType`, `initMultiBuildingSection`, `getTopLeftCityInfo`, `getChunkCharacteristicsGui`, `hasBuildingGui` (`worldgen/lost/BuildingInfo.java:342-657`), on `f18a094d`.

## 1. Identity

| Member | Role |
|---|---|
| `computeChunkCharacteristics` | Builds a `LostChunkCharacteristics` and wraps it in `Staged` |
| `checkBuildingPossibility` | Building chance and conflicts with roads, highways and railways |
| `getEffectivePlannedRoadType` | Clips the raw street field to the city ([street-planner.md](street-planner.md)) |
| `initMultiBuildingSection` | Multibuilding membership from predefined content or `MultiChunk` |
| `LostChunkCharacteristics` (API class) | `isCity`, `couldHaveBuilding`, `multiPos`, `cityLevel`, `cityStyle`, `multiBuilding`, `buildingType`, `rawPlannedRoadType`, `plannedRoadType` (both default `NONE`) |
| `LostCityEvent.CharacteristicsEvent` | Forge event posted with the mutable characteristics |
| `getChunkCharacteristicsGui`, `hasBuildingGui` | Simplified preview version |

Profile options: `BUILDING_CHANCE` (via city-style override), `MULTI_USE_CORNER`, the city-detection options, `STREET_GENERATION_MODE` (persisted). World settings: `railpartheight6` (default 1). Assets: city style building lists, predefined buildings and streets, multibuildings.

## 2. Inputs

Seed, coordinates, profile, world style, structure references in the active region (`07-scattered`), highways and railways (`06`), the street planner, city levels, predefined content.

## 3. Numbers

| Item | Value |
|---|---|
| Building random | `QualityRandom(seed + chunkZ * 341873128712 + chunkX * 132897987541)` |
| `BUILDING_CHANCE` | 0.3 (0..1), overridden by the city style's `buildingchance` |
| Highway clearance | building allowed only if `cityLevel > maxHighwayLevel + 1` |
| Railway clearance | `cityLevel >= railLevel + railpartheight6` (default 1) |
| Sphere edge | single buildings suppressed when relative distance to the sphere center `> 0.7` |

## 4. Behavior

All (server).

### `computeChunkCharacteristics`

1. `profile = getProfile(chunk)`. `isCity = isCityRaw(chunk, profile)` ([city-detection.md](city-detection.md)).
2. Multibuilding position:
   - not city: `multiPos = SINGLE`, no multibuilding;
   - city: `initMultiBuildingSection` (below).
3. Structure avoidance: `StructureAvoidance.check(chunk, multiPos, region)` (`07-scattered`). `avoidCity` is true when the result is `DIRECT` or `ADJACENT`. The extra clause "multibuilding, in a `WorldGenRegion`, result unknown" can never be true because the footprint path never returns `UNKNOWN` (CITY-24). If `avoidCity`, set `isCity = false` now.
4. City level ([city-level.md](city-level.md)): single chunks use their own; multibuilding chunks use the top-left level (`MULTI_USE_CORNER`) or the footprint average.
5. Create `rand` = building random (QualityRandom, seed above).
6. Hierarchical street mode only: `rawPlannedRoadType` = the raw street field at this chunk; `plannedRoadType = getEffectivePlannedRoadType(...)`. Legacy mode leaves both `NONE`.
7. `couldHaveBuilding = isCity && checkBuildingPossibility(...)`. The building random draws its first `nextFloat` only when `isCity` is true (short-circuit).
8. City-sphere worlds: a single chunk whose relative distance to the city center is above 0.7 gets `couldHaveBuilding = false` ("minimize cities at the edge of the orb").
9. City style: city chunks that cannot hold a building use the 3 × 3 street-majority vote; all others use `City.getCityStyle` ([city-styles.md](city-styles.md)).
10. Candidate building type:
    - multibuilding, not top-left: the multibuilding's building at `(multiPos.x, multiPos.z)`. (A fallback that copies the top-left's type and throws `"Topleft building type is not set!"` is unreachable in practice.)
    - multibuilding top-left: the multibuilding's building at `(0, 0)`;
    - single: `cityStyle.getRandomBuilding(rand, chunk)` (one `nextFloat` if the style lists any building), then replaced by the predefined building's name if a predefined building has its top-left here. A `null` name throws `"Invalid building for multibuilding!"`.

    This runs for **every** chunk, city or not, so every chunk has a candidate `buildingType` (CITY-11, CITY-17).
11. Post `CharacteristicsEvent` on the Forge bus. Listeners may change any field.
12. If `avoidCity`: force `isCity = false` and `couldHaveBuilding = false` again, so avoidance wins over listeners.
13. Return `Staged(characteristics, cacheable = avoidance result is known)`.

### `initMultiBuildingSection`

1. If the chunk is occupied by predefined content (`City.isChunkOccupied`):
   - a predefined multibuilding covers it: `multiPos = (offsetX, offsetZ, dimX, dimZ)` and that multibuilding;
   - anything else (predefined single building or street): `SINGLE`.
2. Otherwise ask `MultiChunk.getOrCreate(chunk).getMultiBuilding(chunk)` ([multibuildings.md](multibuildings.md)): none gives `SINGLE`; a hit gives its offset within the multibuilding and its dimensions.

### `checkBuildingPossibility`

Draws `bc = rand.nextFloat()` first, always. Then, in order:

1. Predefined building top-left here: **true** (no other test).
2. Predefined street here: **false**.
3. `buildingChance` = the city style's `buildingchance`, else `BUILDING_CHANCE` (via `EffectiveCitySettings`). The style is `City.getCityStyle(chunk)`, not the majority style.
4. Part of a multibuilding: **true** (conflicts were settled when it was placed).
5. A planned road (effective, not raw) here: **false**.
6. `bc >= buildingChance`: **false**.
7. A highway at this chunk (either axis): true only if `cityLevel > max(xLevel, zLevel) + 1`.
8. A railway at this chunk: an underground station gives **false**; otherwise true only if `cityLevel >= railLevel + railpartheight6`.
9. Otherwise **true**.

### `getEffectivePlannedRoadType`

1. Not city, or the raw field has no road here: resolve with `connected = false`, `overridden = false`, which yields `NONE`.
2. `overridden` = a predefined building or predefined street occupies the chunk, or the chunk is part of a multibuilding.
3. `connectedCityNeighbor` = at least one direction in which the raw road continues (`PlannedStreetInfo.connects`) leads to a chunk with the same profile that is raw city.
4. `EffectiveStreetResolver.resolve(raw, isCity, connected, overridden)`: `NONE` unless city, connected and not overridden; otherwise the raw type. This removes one-chunk road stubs where the city mask barely touches the road field.

### GUI preview (`getChunkCharacteristicsGui`, both sides)

`isCity = isCityRaw`; `cityLevel = computeCityLevel` (uncached); `couldHaveBuilding = isCity && rand.nextFloat() < profile.BUILDING_CHANCE`. It ignores city-style building chance, multibuildings, roads, highways, railways and avoidance, so the preview can disagree with the world (CITY-33). `hasBuildingGui` returns `couldHaveBuilding`.

## 5. Determinism

| Random | Seed | Draws here |
|---|---|---|
| Building random (`QualityRandom`) | `seed + chunkZ * 341873128712L + chunkX * 132897987541L` | `bc` (1 `nextFloat`, only if city), then 1 `nextFloat` for the building pick (single chunks with a non-empty building list) |

The `BuildingInfo` constructor seeds a **new** building random with the same seed, so its first `nextFloat` equals `bc` (CITY-10).

Order and state: the result is cached only when structure avoidance is known (CITY-20); the Forge event lets other mods change results; the majority vote's tie-break follows `HashMap` order (CITY-21).

## 6. Data formats

Reads city-style `buildings`, `buildingchance`, multibuilding definitions and predefined content (`05-assets`, [predefined-content.md](predefined-content.md)). No NBT.

## 7. Output

A `LostChunkCharacteristics` per chunk, consumed by the `BuildingInfo` constructor, `MultiChunk`, `HierarchicalBridgePlanner`, the API (`ILostChunkInfo`), highways (`06`) and terrain (`03`).

## 8. Relationships

- Called by: `ChunkPlanner.characteristics`; the GUI (`09-client-ui`) through the `Gui` variants.
- Calls: [city-detection.md](city-detection.md), [city-level.md](city-level.md), [city-styles.md](city-styles.md), [multibuildings.md](multibuildings.md), [street-planner.md](street-planner.md), `StructureAvoidance` (`07-scattered`), `Highway` and `Railway` (`06`), `CitySphere` (`03`).

## 9. 1.20 dependencies

| Mechanism | Where | Why it matters for 1.7.10 |
|---|---|---|
| `WorldGenRegion` and structure references | step 3 | 1.7.10 has no structure-reference API (`07-scattered`) |
| Forge event bus | `CharacteristicsEvent` | Exists in 1.7.10 (`cpw.mods.fml` / `MinecraftForge.EVENT_BUS`); the event class is ours to define |

## 10. Bugs

CITY-10, CITY-11, CITY-17, CITY-20, CITY-21, CITY-24, CITY-33.

## Deviations in the new mod

See [new-spec 04-city-layout § Planning stages](../../new-spec/04-city-layout/index.md#planning-stages-chunk-plannermd-chunk-characteristicsmd) and [landmark-zones.md](../../new-spec/04-city-layout/landmark-zones.md).
