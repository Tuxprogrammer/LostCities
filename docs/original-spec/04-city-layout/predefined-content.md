# Predefined cities, buildings, streets and spheres

Data-pack assets can pin a city, individual buildings, streets and city spheres to exact chunks. Upstream ships **none** (no `predefinedcities` or `predefinedspheres` files in `src/main/resources` at `f18a094d`), so this is a modpack and data-pack feature. Source: `worldgen/lost/cityassets/PredefinedCity.java`, `PredefinedSphere.java`, `worldgen/lost/regassets/data/PredefinedBuilding.java`, `PredefinedStreet.java`, the predefined members of `worldgen/lost/City.java`, and their uses in `BuildingInfo`; `CitySphere.getSphereAtCenter` and the sphere lookup (`worldgen/lost/CitySphere.java:378-470`, out of scope, read for behavior), on `f18a094d`.

## 1. Identity

| Class / member | Role |
|---|---|
| `PredefinedCity` | Runtime form of a `predefinedcities` asset |
| `PredefinedBuilding` (record) | `building`, `relChunkX`, `relChunkZ`, `multi`, `preventRuins` |
| `PredefinedStreet` (record) | `relChunkX`, `relChunkZ` |
| `PredefinedSphere` | Runtime form of a `predefinedspheres` asset |
| `City.getPredefinedCity` | City at a center chunk |
| `City.getPredefinedBuildingAtTopLeft` | Building whose top-left chunk is here |
| `City.getPredefinedBuilding` / `isChunkOccupied` | Building covering a chunk (multibuilding footprints expanded), street at a chunk |
| `City.getPredefinedStreet` (two overloads) | Street at a chunk; one reads `predefinedStreetMap`, the other `OCCUPIED_CHUNKS_STREET` (same content) |
| `AssetRegistries.PREDEFINED_CITIES`, `PREDEFINED_SPHERES`, `loadPredefinedStuff` | Loading (`05-assets`) |

Profile options that touch spheres: `CITYSPHERE_ONLY_PREDEFINED`, `SPAWN_SPHERE` (`02`, `03`).

## 2. Inputs

The asset registries (data-pack reload), the dimension of each asset, chunk coordinates.

## 3. Numbers

None beyond the asset values. All coordinates are chunk coordinates except a sphere's `centerx`/`centerz` (blocks).

## 4. Behavior

All (server).

### Loading and maps

1. On first use, `loadPredefinedStuff` loads both registries once (until `AssetRegistries.reset`).
2. Maps are built lazily under the `City` class lock, published as immutable maps, and cleared by `City.cleanCache`:
   - cities by `(dimension, chunkx, chunkz)`;
   - buildings by `(dimension, city chunkx + building chunkx, city chunkz + building chunkz)`, the building's top-left;
   - occupied chunks: a single building covers its top-left; a `multi` building covers every chunk of its multibuilding's `dimX × dimZ` from the top-left, each with its offset;
   - streets by `(dimension, city chunk + street chunk)`.

   Entries landing on the same chunk overwrite each other in registry iteration order (CITY-08).

### Effects

| Where | Effect |
|---|---|
| `City.isCityCenter` | A predefined city's center chunk is always a city center |
| `City.getCityRadius` | Its `radius` (blocks) |
| `City.getCityStyleForCityCenter` | Its `citystyle` |
| `City.getCityFactor` | 1.0 at a predefined building's top-left, at a predefined street, and at the three chunks east, south-east and south of a predefined multibuilding's top-left (CITY-07) |
| `initMultiBuildingSection` | Occupied chunks take the predefined multibuilding's position, or `SINGLE` for a single building or street; `MultiChunk` never places random multibuildings on occupied chunks |
| `checkBuildingPossibility` | Building top-left: building always possible. Street: never |
| candidate building | The predefined building's name replaces the random pick at its top-left |
| `getEffectivePlannedRoadType` | Occupied chunks get no planned road |
| `BuildingInfo.predefinedStreet` | Streets extend roads in hierarchical mode even without a planned road; never an open lot |
| ruins | `preventruins: true` forces `ruinHeight = -1` at the top-left (the draw still happens) |

In Perlin mode (`CITY_CHANCE < 0`) a predefined city's center and radius have no effect on the city factor; only its buildings and streets force city chunks.

### Predefined spheres

When the city-sphere lookup runs for a chunk not yet cached, it iterates predefined spheres in registry order and takes the first one in the same dimension whose radius (blocks, around `centerx`, `centerz`) intersects the chunk. That sphere is always enabled, with its radius and center at the profile's `GROUNDLEVEL`; the sphere's chunk key is `(chunkx, chunkz)`. With `CITYSPHERE_ONLY_PREDEFINED`, chunks matched by no predefined sphere get no sphere. Details of sphere geometry are in `03-terrain`. `SPAWN_SPHERE` picks a predefined sphere for player spawn (`01-core`).

## 5. Determinism

No randomness. Overlapping predefined entries resolve by hash-map and registry iteration order (CITY-08); the first matching predefined sphere wins in registry order.

## 6. Data formats

`predefinedcities/*.json` (codec `PredefinedCityRE`):

| Field | Type | Default | Meaning |
|---|---|---|---|
| `dimension` | string | required | Dimension id, for example `minecraft:overworld` |
| `chunkx`, `chunkz` | int | required | City center chunk |
| `radius` | int | required | City radius in blocks |
| `citystyle` | string | required | City style name |
| `buildings` | list | none | `PredefinedBuilding` entries |
| `streets` | list | none | `PredefinedStreet` entries |

`PredefinedBuilding`: `building` (string, required; a building name, or a multibuilding name when `multi`), `chunkx`, `chunkz` (int, required, relative to the city center), `multi` (bool, default false), `preventruins` (bool, default false).

`PredefinedStreet`: `chunkx`, `chunkz` (int, required, relative to the city center).

`predefinedspheres/*.json` (codec `PredefinedSphereRE`): `dimension` (string), `chunkx`, `chunkz` (int, sphere center chunk), `centerx`, `centerz` (int, block coordinates of the center), `radius` (int, blocks); all required.

No NBT.

## 7. Output

Forced city membership, buildings, streets and spheres at fixed chunks.

## 8. Relationships

- Used by: [city-detection.md](city-detection.md), [city-styles.md](city-styles.md), [chunk-characteristics.md](chunk-characteristics.md), [multibuildings.md](multibuildings.md), [building-info.md](building-info.md), `CitySphere` (`03`), spawn selection (`01`).
- Formats owned by `05-assets`.

## 9. 1.20 dependencies

| Mechanism | Where | Why it matters for 1.7.10 |
|---|---|---|
| Data-pack registries and codecs | `PredefinedCityRE`, `PredefinedSphereRE`, records | No data packs; assets come from the jar or a config folder |
| `ResourceKey<Level>` dimension from a string id | `PredefinedCity.dimension`, `PredefinedSphere.dimension` | Integer dimension IDs in 1.7.10 |

## 10. Bugs

CITY-07, CITY-08, CITY-40.

## Deviations in the new mod

See [new-spec 04-city-layout § Corridors, doors, predefined content, transforms, settings](../../new-spec/04-city-layout/index.md#corridors-doors-predefined-content-transforms-settings).
