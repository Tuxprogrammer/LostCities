# Railway layout: grid, stations and levels

Which chunks carry railway, of which type, at which level, with how many tracks and which way they face. Railways sit on a fixed repeating grid under the world; stations appear where the grid meets a city.

| # | Field | Contents |
|---|---|---|
| 1 | **Identity** | `worldgen/lost/Railway` (`getRailChunkType`, `RailChunkInfo`, `RailDirection`, `getRailwayLevel`, `removeRailChunkType`), `api/RailChunkType`. Profile options `railwaySpacingNorthSouth`, `railwaySpacingEastWest`, `railwayLevelOffset`, `railwaysCanEnd`, `railwaysEnabled`, `railwayStationsEnabled`, `railwaySurfaceStationsEnabled`. Asset: the world style's `parts.railways` (`stationopen`, `stationopenroof` are chosen here). |
| 2 | **Inputs** | World seed, chunk coordinate, the chunk's profile (`BuildingInfo.getProfile`), raw city membership of station spots, the station chunk's city level, highway levels at and next to stations, city spheres, the world style's railway parts. |
| 3 | **Numbers** | See [Numbers](#numbers). |
| 4 | **Behavior** | See [Behavior](#behavior). |
| 5 | **Determinism** | One `QualityRandom` per chunk seeded from the world seed and chunk coordinates. A static cache that other code can overwrite. See [Determinism](#determinism). |
| 6 | **Data formats** | Profile options; `RailChunkType` is public API. |
| 7 | **Output** | `RailChunkInfo`: type, direction, level, number of tracks (1 to 3), and for surface stations the part list to use. |
| 8 | **Relationships** | Read by `gen/Railways` ([railway-parts.md](railway-parts.md)), `BuildingInfo`, `MultiChunk`, `LostCityTerrainFeature`, the debug command and the GUI preview. Reads highways ([highway-occupancy.md](highway-occupancy.md)). The intercity planner keeps highways off this grid ([intercity-planner.md](intercity-planner.md)). |
| 9 | **1.20 dependencies** | None in the layout itself beyond the Y range of the levels (see table). |
| 10 | **Bugs** | `HWY-01`, `HWY-07`, `HWY-08`, `HWY-15`. |

## Numbers

| Item | Value | Notes |
|---|---|---|
| Base railway level | -3 | `Railway.RAILWAY_LEVEL_OFFSET` |
| `railwayLevelOffset` | 0, range -8..2 | Railway level = -3 + offset, so -11..-1 |
| Railway Y | `groundLevel + level × 6` | Default 71 − 18 = 53; with offset -8: 71 − 66 = 5; with +2: 65 |
| `railwaySpacingNorthSouth` (`sz`) | 10, even, 4..128 | Chunks between east-west lines |
| `railwaySpacingEastWest` (`sx`) | 10, even, 4..128 | Chunks between north-south lines; also bounds station descent |
| Repeat period | `2·sx` by `2·sz` chunks | Default 20 × 20 chunks (320 × 320 blocks) |
| Half spacing `hx` | `sx / 2` | Default 5 |
| Grid coordinates | `mx = floorMod(chunkX + 1, 2·sx)`, `mz = floorMod(chunkZ + 1, 2·sz)` | The +1 keeps the historical origin |
| Surface stations allowed | city level 0..2 only | Hard-coded `cityLevel > 2` → underground |
| Maximum station height above the railway | `sx − 2` levels | Else underground. Default 8 levels (48 blocks) |
| Maximum height for a surface extension | `sx − 4` levels | Default 6 |
| Surface-station chance | `r < 0.5` | |
| Open roof vs open | second float `< 0.5` → `stationopen` list, else `stationopenroof` | |
| Extension chance | `r < 0.4` | Surface or underground |
| `railwaysCanEnd` | false | `true` in `floating`, `space`, `biosphere`, `biosphere_caves`, `rarecities` |
| `railwaysEnabled` | true | `false` in `cavern`, `floating`, `space`, `biosphere`, `biosphere_caves`, `void_outside` |
| `railwayStationsEnabled` | true | `false` in `floating`, `space`, `biosphere`, `biosphere_caves`, `void_outside` |
| `railwaySurfaceStationsEnabled` | true | |
| Random seed per chunk | `seed + chunkZ × 2600003897 + chunkX × 43600002517` | `QualityRandom` |

Track counts: the station spot at (`mx` 0, `mz` `sz`) carries 3 tracks, (`sx`, 0) carries 2, (`sx`, `sz`) carries 1. Chunks between stations inherit the count from the chunk they chain to; vertical lines and double bends carry 1; three-way splits carry 3.

### The grid

In grid coordinates, one repeat cell looks like this (`sx` = `sz` = 10, so `hx` = 5; S = station spot, `<`/`>` = chunks that copy their east or west neighbor, `|` = vertical line, `D` = double bend, `T` = three-way split, `.` = nothing):

```
mz \ mx   0  1..4  5   6..9  10  11..14  15  16..19
   0      .   .    D    <     S2    >     D     .
 1..9     .   .    |    .     .     .     |     .
  10      S3  >    T    <     S1    >     T     <
11..19    .   .    |    .     .     .     |     .
```

Rows `mz` = 0 and `mz` = `sz` are the east-west lines; columns `mx` = `hx` and `mx` = `sx + hx` are the north-south lines. Station columns are `mx` = 0 and `mx` = `sx`. `<` chunks chain to their **east** neighbor and default to direction `WEST`; `>` chunks chain to their **west** neighbor and default to `EAST`. Exact ranges: in row `sz`, `1..hx−1` and `sx+1..sx+hx−1` are `>`, and `hx+1..sx−1` and `sx+hx+1..2sx−1` are `<`. In row 0, only `hx+1..sx−1` (`<`) and `sx+1..sx+hx−1` (`>`) carry track; `1..hx−1` and `sx+hx+1..2sx−1` are empty.

The three tracks leaving S3 eastward split at T: one continues to S1, two turn north and south along the vertical line to the double bends D, which bring them into S2 on row 0. East of S2 the mirror happens. This matches the diagram in the class comment.

## Behavior

### `getRailChunkType(coord, provider, profile)` (server)

1. (server) Return the cached `RailChunkInfo` for the coordinate if present.
2. (server) Compute it (steps 5 onward).
3. (server) Post-filter: in a space or spheres dimension profile, a chunk on a city sphere's border gets nothing. Else a station type gets nothing when `railwayStationsEnabled` is false; any other type gets nothing when `railwaysEnabled` is false. These flags come from the `profile` argument, not the chunk's own profile.
4. (server) Store with put-if-absent and return the stored value.

### Computing one chunk (server)

5. (server) Create `QualityRandom` with the per-chunk seed. Use the chunk's own profile (`BuildingInfo.getProfile`) for spacing and level. Draw `r = nextFloat()`.
6. (server) **Station spots** (S3, S2, S1 above). If the spot is a raw city chunk, it is a station (step 10); before calling, a second float picks the open or open-roof part list. Otherwise:
   - without `railwaysCanEnd`: `HORIZONTAL`, direction `BI`, at the railway level, with the spot's track count;
   - with `railwaysCanEnd`: look for cities at the neighboring station spots. S3 east: (+sx, 0), (+sx, −sz), (+sx, +sz); S3 west: (+sx, 0) [sic], (−sx, −sz), (−sx, +sz) (`HWY-01`: the first west check repeats the east offset; 1.12 checked (−sx, 0)). S2 east: (+sx, ±sz); west: (−sx, ±sz). S1 east: (+sx, 0); west: (−sx, 0). No city on either side → nothing; city only on the west → `RAILS_END_HERE` facing `WEST`; only on the east → `RAILS_END_HERE` facing `EAST`; both → `HORIZONTAL` `BI`.
7. (server) `mx` = 0 and `mz` = 0 → nothing.
8. (server) **Rows `mz` ∈ {0, `sz`}**:
   - `<` chunks: read the east neighbor's info (recursively through `getRailChunkType`, passing this chunk's `profile`). Direction is the neighbor's, or `WEST` if the neighbor is `BI` or `RAILS_END_HERE`. Then apply the adjacency rule (step 11) with "next chunk" = the west neighbor.
   - `>` chunks: the same with the west neighbor, default `EAST`, next chunk = the east neighbor.
   - (`hx`, 0): `DOUBLE_BEND`, `EAST`, 1 track. With `railwaysCanEnd`, nothing unless a city is at (−hx, ±sz) or (+hx, 0).
   - (`sx+hx`, 0): `DOUBLE_BEND`, `WEST`, 1 track. With `railwaysCanEnd`, nothing unless a city is at (+hx, ±sz) or (−hx, 0).
   - (`hx`, `sz`) and (`sx+hx`, `sz`): `THREE_SPLIT` (`EAST` and `WEST` respectively), 3 tracks. With `railwaysCanEnd`, if there is no city at (±hx, 0), nothing when the chunks `sz` north and `sz` south (the double bends) are both nothing.
   - Anything else on these rows → nothing.
9. (server) **Other rows**: `mx` = `hx` → `VERTICAL`, `EAST`; `mx` = `sx+hx` → `VERTICAL`, `WEST`; both 1 track at the railway level. With `railwaysCanEnd`, nothing if the line's chunk on the row above or the row below (same column) is nothing. All other chunks → nothing.

### Station type (server, step 10)

10. Given `r`, the track count and the open/open-roof list:
    1. City level `cl` of the chunk; railway level `rl`.
    2. Underground (`STATION_UNDERGROUND`, `BI`, `rl`) if `cl > 2`, or `cl − rl > sx − 2`, or surface stations are disabled.
    3. Underground if an X highway here has level ≤ `cl`, or a Z highway here has level ≤ `cl`.
    4. Underground if a Z highway in the west or east neighbor has level ≤ `cl`.
    5. Otherwise `STATION_SURFACE` at `cl` with the part list when `r < 0.5`, else underground.

### Adjacency rule (server, step 11)

`next` is the chunk on the far side, away from the station. From the adjacent (station-side) chunk's info:

| Adjacent type | Result |
|---|---|
| nothing | nothing |
| `STATION_SURFACE` | If an X or Z highway in `next` sits exactly at the station's level, force `r = 1`. If `r < 0.4` and the station is at most `sx − 4` levels above the railway: `STATION_EXTENSION_SURFACE` at the station level. Else start descending. |
| `STATION_UNDERGROUND` | `r < 0.4` → `STATION_EXTENSION_UNDERGROUND`, else `HORIZONTAL`, same level and tracks |
| `STATION_EXTENSION_SURFACE` | Start descending |
| `STATION_EXTENSION_UNDERGROUND` | `HORIZONTAL`, same level |
| `GOING_DOWN_*` | At the railway level → `HORIZONTAL`; else `GOING_DOWN_FURTHER` two levels lower |
| `RAILS_END_HERE` | `HORIZONTAL` if this chunk's direction equals the end's direction, else nothing |
| `HORIZONTAL` | The adjacent info itself (same type, direction, level, tracks) |
| `THREE_SPLIT`, `VERTICAL`, `DOUBLE_BEND` | Throws "This is really impossible!" (unreachable with even spacings of 4 or more) |

Start descending: if the difference between the adjacent level and `rl` is odd, `GOING_DOWN_ONE_FROM_SURFACE` one level lower; if even, `GOING_DOWN_TWO_FROM_SURFACE` two levels lower. Each later chunk drops two levels until it reaches `rl`. A station has `hx − 1` approach chunks on each side, so the `sx − 2` guard is exactly enough to reach the railway level before the line junction.

All derived infos keep the direction chosen in step 8 and the adjacent's track count.

### Removal (server)

`removeRailChunkType(coord)` overwrites the cached info with nothing. `BuildingInfo` calls it when the world style's `railwayavoidance` is `block_railway`, the chunk has a building, and the building's lowest floor (city level − cellars) reaches below the rail level + `railpartheight6` (`04-city-layout`). The bundled world style uses `ignore`.

## Determinism

- **Random:** `QualityRandom(seed + chunkZ × 2600003897 + chunkX × 43600002517)`. First `nextFloat()` is `r`; at a city station spot a second `nextFloat()` picks open vs open-roof. The seed has no dimension component, so every dimension shares the pattern. `QualityRandom` (`01-core`) overrides `next(bits)` with a three-part generator (a 64-bit LCG with multiplier 2862933555777941757 and increment 7046029254386353087, a xorshift, and a multiply-with-carry), so `nextFloat` is not `java.util.Random`'s sequence.
- **Cache:** a static `ConcurrentHashMap<ChunkCoord, RailChunkInfo>`, unbounded, cleared by `Railway.cleanCache()` at server start and stop and by the GUI preview (`HWY-15`). The key is the coordinate only, while the `profile` argument decides the enable flags and is forwarded to recursive neighbor lookups; in multi-profile dimensions the first caller's profile wins (`HWY-08`).
- **Order dependence:** with `block_railway` avoidance, whether a chain of rail chunks is cut depends on whether a neighbor's rail info was computed before or after the building's `BuildingInfo` ran `removeRailChunkType` (`HWY-07`). With `ignore` (bundled), results do not depend on order.
- **Recursion:** `<`/`>` chunks recurse toward the nearest station spot, at most `hx − 1` deep; `railwaysCanEnd` checks recurse one row up or down. No cycles.

## Data formats

Profile options in Numbers. `RailChunkType` (public API, `api`):

| Constant | `isStation` | `isSurface` |
|---|---|---|
| `NONE` | no | no |
| `STATION_SURFACE` | yes | yes |
| `STATION_UNDERGROUND` | yes | no |
| `STATION_EXTENSION_SURFACE` | yes | yes |
| `STATION_EXTENSION_UNDERGROUND` | yes | no |
| `GOING_DOWN_TWO_FROM_SURFACE` | no | yes |
| `GOING_DOWN_ONE_FROM_SURFACE` | no | yes |
| `GOING_DOWN_FURTHER` | no | no |
| `HORIZONTAL` | no | no |
| `THREE_SPLIT` | no | no |
| `VERTICAL` | no | no |
| `DOUBLE_BEND` | no | no |
| `RAILS_END_HERE` | no | no |

`RailDirection`: `BI`, `WEST`, `EAST`. `ILostChunkInfo.getRailType()` exposes the type to other mods (`01-core`).

## Output

`RailChunkInfo` per chunk, consumed by [railway-parts.md](railway-parts.md) and by city layout (buildings over railways, cellar limits, multibuilding avoidance, street decoration, front parts).

## Relationships

- Read by `gen/Railways` and `LostCityTerrainFeature.generate` ([railway-parts.md](railway-parts.md)).
- `BuildingInfo.checkBuildingPossibility`: no building over an underground station; elsewhere a building only if `cityLevel ≥ rail level + railpartheight6`. Cellars are capped above railways unless avoidance is `block_railway` (`04-city-layout`).
- `MultiChunk`: multibuildings avoid station and surface rail chunks and highway chunks; over other rail chunks (unless `block_railway`) they are allowed only when the rail leaves room for the building's maximum cellars (`04-city-layout`).
- `LostCityTerrainFeature`: no street decorations on surface rail chunks; no street or park where a surface station sits at city level (`04-city-layout`).
- Calls `Highway.getXHighwayLevel`/`getZHighwayLevel` ([highway-occupancy.md](highway-occupancy.md)), `BuildingInfo.isCityRaw`/`getCityLevel` (`04-city-layout`), `CitySphere.onCitySphereBorder` (`03-terrain`).
- The intercity planner treats rows with `floorMod(chunkZ + 1, sz) == 0` and columns with `floorMod(chunkX + 1, sx) ∈ {0, hx}` as railway corridors; these are exactly this grid's lines and station columns.
- Subway corridors between building cellars (`Corridors`, `BuildingInfo.hasXCorridor`) are a separate city-layout system (`04-city-layout`); they place vanilla rails but do not use this grid.

## 1.20 dependencies

| Mechanism | Where | Why it matters for 1.7.10 |
|---|---|---|
| Y range of rail levels | `railwayLevelOffset` -8..2 with `GROUNDLEVEL` 2..256 | Default Y 53; offset -8 gives Y 5 with ground 71, inside 1.7.10's bedrock band (0..4) once a part extends down, and below 0 with a lower ground level. 1.20 has room down to -64. |

## On 1.12

`dimensions/world/lost/Railway.java` on `upstream/1.12`: spacing fixed at 10 (period 20), level fixed at -3, no surface-station toggle and no `sx − 2` or `sx − 4` guards, station part names hard-coded (`station_open`, `station_openroof`), two extra `nextFloat()` calls before `r`, and descent parity based on the absolute level (even → down one) rather than the difference. The west check at S3 is correct there (−10, 0). Its three-split `railwaysCanEnd` check looks north twice (`chunkZ − 10`); 1.20 fixed that.

## Bugs

- `HWY-01`: with `railwaysCanEnd`, the S3 spot's "city to the west" test checks the east station spot, so lines end or continue on the wrong side.
- `HWY-07`: `removeRailChunkType` mutates the shared cache during planning, making cut rails depend on generation order (`block_railway` only).
- `HWY-08`: the cache ignores which profile asked; enable flags and recursive lookups use the caller's profile.
- `HWY-15`: static, unbounded cache.

## Deviations in the new mod

None recorded yet; the new spec decides.
