# City spheres and landscape types

How the six landscape types change terrain handling, and how city spheres (glass domes over cities) are chosen, cached and drawn. In 1.20 Lost Cities does **not** generate floating, cavern or space terrain itself: those shapes come from another generator, normally McJty's Lost Worlds mod, and Lost Cities only adapts its writing to them. The port targets the `DEFAULT` landscape on RWG ([0003](../../design/0003-world-presence.md)); the others are documented so the choice to drop or keep them is informed.

## 1 · Identity

| Piece | Role |
|---|---|
| `config/LandscapeType` (02-profiles-dimensions) | `DEFAULT` "default", `FLOATING` "floating", `SPACE` "space", `CAVERN` "cavern", `SPHERES` "spheres", `CAVERNSPHERES` "cavernspheres". |
| `LostCityProfile.isDefault/isFloating/isSpace/isCavern/isVoidSpheres/isSpheres` | `isCavern`: `CAVERN` or `CAVERNSPHERES`. `isVoidSpheres`: `SPHERES` only. `isSpheres`: `SPHERES` or `CAVERNSPHERES`. |
| `worldgen/lost/CitySphere` | Sphere selection per 16 × 16-chunk grid cell, the static cache, geometry tests, monorail candidates. Implements API `ILostSphere`. |
| `worldgen/gen/Spheres` | Draws the shell and clears around it, then runs monorails (space) and the chunk fixer. |
| `worldgen/LostCitySphereFeature` | The `lostcities:spheres` feature at `top_layer_modification`. |
| `LostCityTerrainFeature.generate` stage 6 | Sphere centerpiece part. |
| `api/ILostWorldsChunkGenerator` | Interface copied from Lost Worlds: `getOuterSeaLevel()` (nullable) for the sea level outside spheres. |

Profile options: `CITYSPHERE_CHANCE`, `CITYSPHERE_32GRID`, `CITYSPHERE_FACTOR`, `CITY_MINRADIUS`, `CITY_MAXRADIUS`, `CITYSPHERE_MONORAIL_CHANCE`, `CITYSPHERE_CLEARABOVE`, `CITYSPHERE_CLEARABOVE_UNTIL_AIR`, `CITYSPHERE_CLEARBELOW`, `CITYSPHERE_CLEARBELOW_UNTIL_AIR`, `CITYSPHERE_ONLY_PREDEFINED`, `CITYSPHERE_OUTSIDE_PROFILE`, `CITY_AVOID_VOID`, `GROUNDLEVEL`. World style `citysphere` settings (05-assets): `centertype` (`DEFAULT`, `STREET`, `BUILDING`, `NORMAL`), `centerpart`, `centerpartoffset`, `centerpartorigin` (`FIXED`, `CENTER`, `FIRSTFLOOR`, `GROUND`, `TOP`). City style: `sphereGlassBlock`, `sphereBlock`, `sphereSideBlock`. Assets: predefined spheres and predefined cities (05-assets).

## 2 · Inputs

World seed, chunk coordinates, dimension, profile, predefined spheres and cities, the city style at the sphere center, the generator's sea level and (when the generator implements it) its outer sea level.

## 3 · Numbers

| Item | Default | Notes |
|---|---|---|
| Grid cell | 16 × 16 chunks; center chunk at local (8, 8), i.e. `(chunk & ~15) + 8` | |
| `CITYSPHERE_32GRID` | false | when true, only chunks with `(x & 31) == 8` and `(z & 31) == 8` may be centers; cells are still computed on the 16 grid |
| `CITYSPHERE_CHANCE` | 0.7 | per grid center (`space` profile 0.9) |
| Radius | `CITY_MINRADIUS + nextInt(CITY_MAXRADIUS − CITY_MINRADIUS) × CITYSPHERE_FACTOR` | defaults 50, 128, 1.2 → 50 to 142.4 blocks (the factor scales only the random part, `TERR-01`); `space` profile (max 90) → 50 to 96.8 |
| Radius, predefined city at the center | city radius × `CITYSPHERE_FACTOR` | |
| Center position | X, Z = center chunk × 16 + `nextInt(16)` − 8; Y = `GROUNDLEVEL` | |
| Neighbor suppression | the 8 grid centers at ±16 chunks; a sphere is disabled when an overlapping enabled neighbor has a strictly larger radius | 16-grid only |
| `CITYSPHERE_MONORAIL_CHANCE` | 0.8 per direction | |
| Shell thickness | squared distance between `(r − 2)²` and `r²` | blocks |
| Space clearing ring | horizontal distance r … r + 2, full height `centerY − r − 1` … `centerY + r` | `SPACE` (and `FLOATING`, unreachable here) |
| `CITYSPHERE_CLEARABOVE` / `CLEARBELOW` | 0 / 0 (`space` profile 8 / 8 with both `UNTIL_AIR` true; `spheres` and `cavernspheres` profiles clear above 30) | blocks |
| Outer sea level when the generator does not provide one | −1000 | Y (so clearing writes air) |
| "Fully inside" test | all four chunk corners within r − 2 | horizontal |
| Monorail scan | up to 63 chunks each way along the center row or column | |
| Space level by relative distance to center | < 0.3: 2 + `nextInt(2)`; < 0.4: 1 + `nextInt(2)`; < 0.6: `nextInt(2)`; else 0 | city level (04-city-layout) |

## 4 · Behavior

### Landscape matrix (server)

| Behavior | `DEFAULT` | `SPHERES` | `FLOATING` | `CAVERN`, `CAVERNSPHERES` | `SPACE` |
|---|---|---|---|---|---|
| Expected generator | normal noise | Lost Worlds "spheres"-like | Lost Worlds "islands" | Lost Worlds "caves" / "cavespheres" | Lost Worlds "spheres" |
| Bedrock floor, high water, leveling | yes | yes | no | no | no |
| Terrain blend in normal chunks | yes | yes | no | no | no |
| Void check before a city chunk | no | no | yes, when `CITY_AVOID_VOID` | no | no |
| Base under streets | to bedrock | to bedrock | 3-block slab (border at −3) | 2-block slab (border at −2) | down through air to Y 2 |
| Building underside | border skirt and filler | same as default | island fill with noise | 10-block border skirt and filler | border and base down through air (10 max) |
| Clear above the top floor | yes | yes | no | no | no |
| Corner supports on borders | no | no | yes | yes | no |
| Sphere shells and centerpiece | no | yes | no | `CAVERNSPHERES` only | yes |
| Monorails | no | no | no | no | yes |
| City level source | height thresholds | sphere distance | height thresholds | same as floating | sphere distance |
| `outsideChunk` (outside every sphere) uses the outside profile's ground and sea level | no | yes | no | `CAVERNSPHERES` | yes |

The cavern heightmap rule ([heightmap sheet](heightmap-sampling.md)) applies to both cavern types.

### Choosing the sphere for a chunk: `getCitySphere(coord)` (server)

Under a per-dimension lock:
1. Load predefined assets (once).
2. If the static cache has this chunk, return it.
3. For each predefined sphere of this dimension whose circle touches one of the chunk's four corners, build that sphere (always enabled; center from the asset, Y = `GROUNDLEVEL`) and cache it.
4. Otherwise, if `CITYSPHERE_ONLY_PREDEFINED`, cache `EMPTY` (disabled, radius 0).
5. Otherwise build the sphere of the chunk's 16-grid cell center:
   1. `Random(worldSeed + centerX × 961744153 + centerZ × 837971201)`.
   2. Enabled if `nextFloat() < CITYSPHERE_CHANCE` and the center is on the grid (16 or 32).
   3. Radius and center position (Numbers), drawn in that order.
   4. On the 16 grid, for each of the 8 neighboring centers, recompute its enabled flag, radius and center with its own seed; if it is enabled, overlaps (center distance ≤ sum of radii) and is strictly larger, disable this one.
   5. If enabled, draw the four monorail candidates (north, south, west, east).
6. **Cache:** store the sphere under this chunk and under every chunk whose box touches the sphere's circle, overwriting what was there; a radius below 1 stores it only at the center chunk. A disabled sphere is cached the same way (`TERR-02`).

`isCitySphereCenter`: enabled and this is the center chunk. `intersectsWithCitySphere`: enabled and any chunk corner within r. `onCitySphereBorder`: 1–3 corners within r. `fullyInsideCitySpere`: all 4 corners within r − 2. `isInSphere(pos)`: horizontal distance ≤ r. Monorails: a chunk on a center row (`(z & 15) == 8`) has a horizontal monorail when the nearest enabled sphere to the east (within 63 chunks) wants a west link and the nearest to the west wants an east link; vertical likewise; a chunk fully inside a sphere next to such a chunk gets a station (06-highways-railways builds them).

`initSphere` (once per sphere object): `Random(worldSeed + centerX × 837971201 + centerZ × 961744153)` picks glass, base and side states from the center chunk's city-style palette.

### Drawing: `Spheres.generateSpheres` (server, `top_layer_modification`)

Only for `SPACE` and the two sphere types. The driver is attached to this chunk.
1. Get and initialize the chunk's sphere. If enabled, `fillSphere` with center (relative to the chunk), Y = `GROUNDLEVEL`, integer radius r.
2. `SPACE`: `Monorails.generateMonorails` (06).
3. Commit the driver, detach, run `ChunkFixer.fix` again ([post-processing](post-processing.md)).

`fillSphere` per column (x, z) with horizontal squared distance h²:
- **Inside the circle (h² ≤ r²):**
  1. From `max(minBuild, centerY − r − 1)` to `centerY`: shell blocks (between `(r − 2)²` and `r²`) become the side block; remember one below the lowest shell block as the bottom.
  2. From `centerY + 1` upward (below `min(maxBuild, centerY + r + 1)`): shell blocks become glass; the inside is not touched. At the first Y outside the sphere:
     - clear `CITYSPHERE_CLEARABOVE` blocks upward (to at most the max build height **inclusive**, `TERR-16`) with air, or liquid at or below the outer sea level;
     - if `CLEARABOVE_UNTIL_AIR`, keep clearing upward until air (no upper bound);
     - if `CITYSPHERE_CLEARBELOW` > 0 and a bottom was found, clear that many blocks downward from the bottom (not below the min build height);
     - if `CLEARBELOW_UNTIL_AIR`, keep clearing downward until the block is air (or liquid at or below the generator's sea level) or the min build height (`TERR-17`);
     - stop this column.
- **In the ring (r² < h² ≤ (r + 2)²), `SPACE` only:** set the whole column from the bottom to the top of the sphere's height range to air (liquid at or below the outer sea level).

### Centerpiece (server, pipeline stage 6)

In `SPACE`, `SPHERES` and `CAVERNSPHERES`, if this chunk is an enabled sphere's center and the world style names a `centerpart`: Y = 0 (`FIXED`), the sphere center Y (`CENTER`), the city ground (`FIRSTFLOOR`), the chunk's `groundLevel` (`GROUND`), or the building top (`TOP`), plus `centerpartoffset`; write the part at that Y with `HardAirSetting.WATERLEVEL`. `centertype` (`STREET`, `BUILDING`, `NORMAL`) forces the center chunk's city and building flags in 04-city-layout.

### Floating void check (server, pipeline stage 4)

See [pipeline](generation-pipeline.md). Floating profiles also treat a chunk with height ≤ 0 as a void chunk (04-city-layout's `isVoidChunk`).

## 5 · Determinism

Sphere selection is seeded per grid center and independent of order, except the cache: chunks covered by two spheres keep whichever was computed last (`TERR-02`). The sphere feature opens its own generation context with the same per-chunk seed as the city feature.

## 6 · Data formats

World style `citysphere` object and predefined sphere and city assets (05-assets). No NBT.

## 7 · Output

Glass domes (side blocks below the center, glass above) centered at `GROUNDLEVEL`, cleared bands above and below, cleared rings around space spheres, and centerpieces at sphere centers.

## 8 · Relationships

04-city-layout (`BuildingInfo` uses `intersectsWithCitySphere`, `getRelativeDistanceToCityCenter`, `onCitySphereBorder`, `hasMonorailStation`, `isCitySphereCenter`); 06-highways-railways (monorails, railways at sphere borders); 08-loot-mobs-blocks (`isInSphere` in loot and spawner conditions); 01-core (`/lostcities debug`, spawn selection with `SPAWN_SPHERE`, GUI preview); 02-profiles-dimensions (outside profile).

## 9 · 1.20 dependencies

- The landscape types other than `DEFAULT` depend on an external 1.20 generator (Lost Worlds) and `ILostWorldsChunkGenerator`; nothing in GTNH provides one.
- Sphere drawing in a late decoration step (`top_layer_modification`) through a second feature.
- Min and max build height (−64, 320) in the shell and clearing bounds.

## 10 · Bugs

`TERR-01`, `TERR-02`, `TERR-03`, `TERR-16`, `TERR-17`, `TERR-26`, `TERR-35`.

## On 1.12

1.12 generated the landscapes itself: `terraingen/IslandTerrainGenerator` (floating), `CavernTerrainGenerator`, `SpaceTerrainGenerator`, chosen by `doCoreChunk` per `LANDSCAPE_TYPE`, each with its own `replaceBlocksForBiome`. There was no `SPHERES` or `CAVERNSPHERES` type; spheres existed only with `SPACE`. City spheres used the same 16-chunk grid idea (`dimensions/world/lost/CitySphere`).

## Deviations in the new mod

See [new-spec 03-terrain § Landscapes and spheres](../../new-spec/03-terrain/index.md#landscapes-and-spheres-city-spheres-and-landscapesmd): only `DEFAULT` is ported ([0003](../../design/0003-world-presence.md) targets the overworld on RWG); the other landscapes and city spheres are deferred (decision log D12).
