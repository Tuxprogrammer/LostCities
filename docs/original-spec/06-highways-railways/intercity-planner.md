# Intercity highway planner (`INTERCITY_NETWORK_V1`)

The default highway layout for new worlds. The world is divided into square planning cells; each cell owns at most one hub (an approximate city center). Hubs pick nearby hubs as neighbors, a connection exists only where two hubs pick each other, and each connection becomes a straight or L-shaped route of chunks. Any chunk can ask which routes cross it by looking at a bounded neighborhood of cells, so the answer never depends on what else has been generated.

| # | Field | Contents |
|---|---|---|
| 1 | **Identity** | `worldgen/highway/IntercityHighwayPlanner` (planner and its private `BoundedCache`), records `HubKey`, `HighwayHub`, `HighwayConnectionKey`, `HighwaySegment`, `HighwayRoute` (with `RouteShape`), `HighwayInfo`, enums `HighwayAxis`, `HighwaySector`, settings `HighwayPlannerSettings`, interfaces `CityPotential`, `HighwayLevelSource`, `HighwayHubPersistence`. Profile options `highwayPlanningCellSize`, `highwayHubSampleSpacing`, `highwayHubMinimumPotential`, `highwayHubSearchRadiusCells`, `highwayMinimumHubDistance`, `highwayMaximumHubDistance`, `highwayMaximumConnectionsPerHub`, `highwayMinimumRouteLength`, `highwayRouteCityPenalty`, `highwayLevelFromCities`, `highwayNetworkLevel`, `railwaySpacingNorthSouth`, `railwaySpacingEastWest`. |
| 2 | **Inputs** | World seed; dimension ID string (for example `minecraft:overworld`); the settings above; a `CityPotential` ([city-potential.md](city-potential.md)); a `HighwayLevelSource` (city level at a chunk); a `HighwayHubPersistence` (saved hub decisions, [planning-service-and-persistence.md](planning-service-and-persistence.md)). |
| 3 | **Numbers** | See [Numbers](#numbers). |
| 4 | **Behavior** | Hub selection, candidates, neighbor selection, acceptance, ownership, route geometry, chunk query. See [Behavior](#behavior). |
| 5 | **Determinism** | No `java.util.Random`. Every choice uses a salted 64-bit hash of seed, dimension and coordinates; ties are broken explicitly. Caches are bounded and do not affect results. See [Determinism](#determinism). |
| 6 | **Data formats** | Profile options; hub decisions go through `HighwayHubPersistence`. |
| 7 | **Output** | `HighwayInfo` for a chunk: lowest level per axis, classification, route hits. |
| 8 | **Relationships** | Built by `DefaultDimensionInfo` (server) and `NullDimensionInfo` (GUI preview); driven by `HighwayPlanningService`; queried through `Highway` ([highway-occupancy.md](highway-occupancy.md)); route hits drive junction parts in `gen/Highways` ([highway-parts.md](highway-parts.md)); avoids the railway grid ([railway-layout.md](railway-layout.md)). |
| 9 | **1.20 dependencies** | The dimension ID string feeds the hash; hub potential and city level read 1.20 terrain and biome samplers (through its inputs). Java 16+ records and `List.of`/`copyOf`, `Optional.isEmpty`. |
| 10 | **Bugs** | `HWY-09`, `HWY-10`. Related: `HWY-06`, `HWY-11`. |

## Numbers

### Settings (from the profile; validated in `HighwayPlannerSettings` and again in `LostCityProfile`)

| Setting | Profile key | Default | Valid range | Unit |
|---|---|---|---|---|
| Planning cell size | `highwayPlanningCellSize` | 128 | 32..512 | chunks per side |
| Hub sample spacing | `highwayHubSampleSpacing` | 16 | 1..cell size | chunks |
| Hub minimum potential | `highwayHubMinimumPotential` | 0.20 | 0..1 | potential (score 200 000) |
| Hub search radius | `highwayHubSearchRadiusCells` | 2 | 0..8 | cells (a square of 2r+1 cells per side) |
| Minimum hub distance | `highwayMinimumHubDistance` | 64 | 0..max | chunks, Euclidean |
| Maximum hub distance | `highwayMaximumHubDistance` | 320 | min..4096 | chunks, Euclidean |
| Maximum connections per hub | `highwayMaximumConnectionsPerHub` | 2 | 1..8 | selections per hub |
| Minimum route length | `highwayMinimumRouteLength` | 40 | 0..4096 | chunks, Manhattan |
| Route city penalty | `highwayRouteCityPenalty` | 1.0 | 0..1000 | weight |
| Level mode | `highwayLevelFromCities` | 3 | 0..4 | |
| Network level | `highwayNetworkLevel` | 0 | 0..32 | city levels |
| Railway spacing north-south | `railwaySpacingNorthSouth` | 10 | 4..128, even | chunks |
| Railway spacing east-west | `railwaySpacingEastWest` | 10 | 4..128, even | chunks |

Invalid values throw `IllegalArgumentException` when the profile loads or the settings are built.

### Constants

| Constant | Value | Use |
|---|---|---|
| `VERSION_SALT` | `0x494e544552563101` | Mixed into every hash ("INTERV1" plus a version byte) |
| `HUB_SAMPLE_SALT` | `0x5d39a74e10c26bf1` | Tie-break among hub samples |
| `HUB_POSITION_SALT` | `0x27f4b8916d3a50ce` | Sample-lattice offset per cell |
| `HUB_STRENGTH_SALT` | `0x618ad2e5073fb94c` | Per-cell input to the sample tie-break |
| `CONNECTION_RANK_SALT` | `0x34ce791b582da6f0` | Candidate rank tie-break |
| `CONNECTION_ACCEPT_SALT` | `0x72b514e90c63dfa8` | Second candidate tie-break |
| `ROUTE_SHAPE_SALT` | `0x19e8c347a65d2bf0` | Last-resort L-shape choice |
| `SCORE_SCALE` | 1 000 000 | Potential 0..1 becomes an integer score 0..1 000 000 |
| `ROUTE_PENALTY_SAMPLE_SPACING` | 8 chunks | Penalty sample step along a route |
| `ENDPOINT_PENALTY_DISCOUNT` | 16 chunks (Manhattan) | Samples this close to either hub are not penalized |
| Hub cache | 4096 entries | per planner |
| Candidate, selection, route caches | 2048 entries each | per planner |
| Chunk cache | 8192 entries | per planner |

### Derived numbers at the defaults

| Quantity | Value |
|---|---|
| Samples per cell | 8 × 8 = 64 (lattice step 16 in a 128-chunk cell) |
| Cells a hub looks at for partners | 5 × 5 − 1 = 24 |
| Cells whose routes a chunk query reads | 5 × 5 = 25 |
| Cells whose hubs a first query in a fresh area may compute | up to 13 × 13 = 169 (radius 3r), so up to 10 816 potential samples |
| Longest possible route | 320 chunks Euclidean between hubs, so at most about 452 chunks Manhattan |
| Hub position | never on a railway corridor row or column (see step H4) |

## Behavior

All steps are (server) during generation, or (client) in the GUI preview with a planner of its own. The planner is pure: it never reads chunks or the world directly; it only calls its `CityPotential`, `HighwayLevelSource` and `HighwayHubPersistence`.

### Hashing (used by every step)

- `mix64(v)`: the SplitMix64 finalizer: `v ^= v >>> 30; v *= 0xbf58476d1ce4e5b9; v ^= v >>> 27; v *= 0x94d049bb133111eb; v ^= v >>> 31`.
- `dimensionSalt`: 64-bit FNV-1a over the UTF-16 code units of the dimension ID (offset basis `0xcbf29ce484222325`, prime `0x100000001b3`; XOR the code unit, then multiply), then `mix64`.
- `hash(salt, x, z, extra)`: start with `seed ^ dimensionSalt ^ VERSION_SALT ^ salt`; XOR in `mix64(u(x) + 0x9e3779b97f4a7c15)`, `mix64(u(z) + 0xc2b2ae3d27d4eb4f)` and `mix64(u(extra) + 0x165667b19e3779f9)`, where `u()` widens an int to a long without sign extension; return `mix64` of the result.
- `pairExtra(cell)`: `m = mix64(u(cellX) ^ rotateLeft(u(cellZ), 32))`; the low 32 bits of `m ^ (m >>> 32)`.
- `floorModHash(h, n)`: `floorMod(h, n)` with `h` treated as a signed 64-bit value.
- Unsigned comparison (`Long.compareUnsigned`) is used wherever two hashes are compared.

### Planning cells

- A chunk's cell is `(floorDiv(chunkX, cellSize), floorDiv(chunkZ, cellSize))`; floor division keeps negative coordinates consistent.
- `HubKey` orders cells by X, then Z. `HighwayConnectionKey` stores the two cells in that order (smaller first) and rejects equal cells.

### H. Hub of a cell (`getHub`)

1. Look in the hub cache, then in persistence. If persistence knows the cell (hub or "no hub"), return that without recalculating.
2. Otherwise calculate (steps H3 to H8), store the result in persistence (including "no hub"), and cache it.
3. Origin of the cell: `(cellX × size, cellZ × size)` in chunks. Lattice offsets: `offX = floorModHash(hash(HUB_POSITION_SALT, cellX, cellZ, 0), spacing)`, `offZ` likewise with `extra = 1`. Samples are at local coordinates `offX, offX + spacing, …` (below `size`) by `offZ, offZ + spacing, …`, iterated X outer, Z inner.
4. Move each sample off the railway grid: if the sample's chunk X lies on a north-south railway or station column (`floorMod(chunkX + 1, spacingEW)` is 0 or `spacingEW / 2`), shift it one chunk east, or one chunk west if it is in the cell's last column. Likewise, if its chunk Z lies on an east-west railway row (`floorMod(chunkZ + 1, spacingNS) == 0`), shift it one chunk south, or north in the last row. Because spacings are even and at least 4, the shifted position is never itself on the grid.
5. Tie value of the sample: `hash(HUB_SAMPLE_SALT, chunkX, chunkZ, floorModHash(hash(HUB_STRENGTH_SALT, cellX, cellZ, 0), 2³¹ − 1))`.
6. Required score: if there is already a best sample whose score is at least the minimum, a new sample must reach the best score (if its tie value is unsigned-smaller than the best's) or the best score + 1 (otherwise); if not, it must reach the minimum score `round(minimumPotential × 1 000 000)`. The potential source may return -1 ("cannot reach the required score", a pure performance shortcut, [city-potential.md](city-potential.md)); such samples are skipped.
7. A sample replaces the best when there is no best yet, when its score is higher, or when the score is equal and its tie value is unsigned-smaller.
8. If no sample reached the minimum score, the cell has no hub. Otherwise the hub is the best sample's chunk and score, and its `cityLevel` is the level source's city level at that chunk (`HighwayCityLevelSource`: `ChunkPlanner.cityLevel`, `04-city-layout`).

### C. Connection candidates of a hub

1. If the cell has no hub, it has no candidates.
2. For every other cell within the search radius (a square, `dx` outer, `dz` inner, -r..r), take its hub, if any.
3. Reject the target when the squared Euclidean chunk distance between the two hubs is below `minimumHubDistance²` or above `maximumHubDistance²`, when the Manhattan distance is below `minimumRouteLength`, or when no route shape between them is railway-clear (step R3): for hubs sharing a chunk row or column, the single straight segment must be clear; otherwise at least one of the two L shapes must be.
4. Record the candidate with: its sector (step C5), the squared distance, the Manhattan length, `rank = hash(CONNECTION_RANK_SALT, first.cellX, first.cellZ, pairExtra(second))` and `acceptanceTie = hash(CONNECTION_ACCEPT_SALT, first.cellX, first.cellZ, pairExtra(second))`, where `first`/`second` are the canonical (ordered) cells of the pair. Both values are therefore the same from either end.
5. Sector of the target relative to the source, by chunk deltas `dx`, `dz`: if `|dx| ≥ |dz|`, `EAST` when `dx ≥ 0`, else `WEST`; otherwise `SOUTH` when `dz ≥ 0`, else `NORTH`.
6. Sort candidates by: squared distance ascending; target score descending; `rank` unsigned ascending; `acceptanceTie` unsigned ascending; target cell ascending.

### S. Selected neighbors of a hub

1. Walk the sorted candidates; take each candidate whose sector has not been used yet, until `maximumConnectionsPerHub` are taken.
2. If fewer were taken, walk the list again and add candidates not yet taken, in order, until the maximum is reached.

### A. Accepted connections and route ownership

1. A connection between hubs A and B exists only when A selected B **and** B selected A.
2. The cell that sorts first (smaller X, then smaller Z) owns the route. `getOwnedRoutes(cell)` returns, sorted by connection key, a route for each accepted connection the cell owns.
3. There is no "mandatory first connection" and no emergency search. A hub can end up with no route (for example when its picks are not reciprocated). `HIGHWAY_MAJOR_CITY_PLAN.md` on the branch proposes both; neither is implemented at `f18a094d`.

### R. Route geometry (`createRoute`)

1. Length is the Manhattan distance between the hubs. The level is chosen by `highwayLevelFromCities` from the two hubs' stored city levels: 0 → the first (owner) hub's level; 1 → minimum; 2 → maximum; 3 → integer average, truncated; 4 → `highwayNetworkLevel`. Any other value throws `IllegalStateException`.
2. Same chunk Z: one X segment from hub to hub, shape `STRAIGHT`, penalty 0. Same chunk X: one Z segment, `STRAIGHT`, penalty 0.
3. Otherwise two L shapes are possible:
   - `HORIZONTAL_THEN_VERTICAL`: X segment along the first hub's Z to the second hub's X, then a Z segment down the second hub's X.
   - `VERTICAL_THEN_HORIZONTAL`: Z segment along the first hub's X to the second hub's Z, then an X segment along the second hub's Z.

   A shape is railway-clear when none of its X segments lies on an east-west railway row and none of its Z segments lies on a north-south railway or station column (same tests as step H4). Since every segment runs along a hub's own row or column and hubs were moved off the grid, both shapes are normally clear; the test matters only for hubs restored from persistence under other settings or built through the public `createRoute(first, second)`.
4. Choose: if exactly one shape is clear, take it; else if the city penalties (step R5) differ, take the lower; else take horizontal-first when bit 0 of `hash(ROUTE_SHAPE_SALT, first.cellX, first.cellZ, pairExtra(second))` is 0.
5. City penalty of a shape: 0 when `routeCityPenalty` is 0. Otherwise, along each segment sample every 8th chunk from its start (distance 0, 8, 16, … up to its length; the end is sampled only if the length is a multiple of 8; the corner is the start of the second segment), skip samples within Manhattan distance 16 of either hub, sum their potential scores (0..1 000 000, with the full height and biome modifier), multiply by `routeCityPenalty` and round to a long.
6. The route stores both hubs, its segments, level, shape, length and penalty. A chunk is on the route when it lies within the inclusive bounding box of any segment.

### Q. Chunk query (`getHighwayInfo`)

1. Look in the chunk cache.
2. Collect, keyed and ordered by connection key, every route owned by any cell within the search radius of the chunk's cell whose segments contain the chunk. This is complete: a route's chunks lie between its two hub cells, and those cells are within the radius of each other.
3. No route → `HighwayInfo.NONE`.
4. For each route, note whether the chunk is on one of its X segments, Z segments, or both (a bend). The X level is the **lowest** level among routes hitting the chunk on X; the Z level likewise.
5. Classify as in [highway-occupancy.md](highway-occupancy.md) and return the levels, the classification and one `RouteHit` per route.

Which chunks get highway pieces, in this mode: every chunk on a route segment, inclusive of both hub chunks, except chunks that intersect a city sphere in space or spheres profiles (filtered by the facade, not by the planner).

Other public queries (used by the debug command): `getNearbyHubs` (hubs in the radius square, sorted by cell), `getConnectionCandidates`, `getSelectedNeighbours`, `getAcceptedConnections`, `getRoute(key)`, `getCacheStats`, `clearCaches`.

## Determinism

- **Random sources:** none. Every choice uses `hash()` with the world seed, the dimension salt, a fixed version salt and a per-purpose salt. Results do not depend on generation order, thread, or cache state.
- **Ties:** every sort has a total order ending in the cell key; sample selection ends in an unsigned hash comparison; equal hashes are astronomically unlikely, and the sample loop order (X outer, Z inner, first wins on equal tie value) settles them.
- **Caches** (all per planner, cleared by `clearCaches`): hub 4096, candidates 2048, selections 2048, routes 2048, chunk info 8192. Each holds `CompletableFuture`s in a `ConcurrentHashMap`; the first caller computes on its own thread, later callers wait on the future. A failed computation is removed and the exception rethrown. When a cache exceeds its size, completed entries are removed in the map's iteration order until it fits. Despite the class comment, this is not least-recently-used (`HWY-10`). Caches only affect speed.
- **Dependency layering:** route → selection → candidates → hub; no layer calls a higher one, so concurrent computations cannot deadlock on each other. Hub calculation calls the city-level source, which does not query highways.
- **Persisted state:** the only stored input is the hub decision per cell. A persisted hub is used even if a fresh calculation would now differ ([planning-service-and-persistence.md](planning-service-and-persistence.md), `HWY-11`).
- **Overflow:** coordinate arithmetic uses exact methods (`addExact`, `toIntExact`) and throws rather than wrapping near the integer limits.

## Data formats

Settings come from the profile (table above). No JSON assets. Hubs are persisted through `HighwayHubPersistence`; the server implementation is `LostCityHighwayData`.

## Output

`HighwayInfo` per chunk ([highway-occupancy.md](highway-occupancy.md)). `HighwayRoute.contains`, `RouteHit.route().segments()` and the level are read by `gen/Highways` to pick straight, bend, T or crossing parts.

Unused at the pin: `HighwayHub.strength()`, `RouteHit.bend()` (computed, not read by generation), `HighwayRoute.routeLength`, `cityPenalty` and `shape` (debug output only).

## Relationships

- Built by `DefaultDimensionInfo` with `ApproximateCityPotential` plus a height and biome modifier, `HighwayCityLevelSource`, and `LostCityHighwayData` persistence; by `NullDimensionInfo` (GUI preview) with the bare potential and no persistence ([city-potential.md](city-potential.md)).
- Driven by `HighwayPlanningService` ([planning-service-and-persistence.md](planning-service-and-persistence.md)).
- Coordinates with the railway grid of [railway-layout.md](railway-layout.md) by formula only; it does not call `Railway`.
- Debug output: `/lostcities debug` (`CommandDebug`, `01-core`) prints the cell, settings, hub, candidates, selections, accepted routes and cache statistics.

## 1.20 dependencies

| Mechanism | Where | Why it matters for 1.7.10 |
|---|---|---|
| Dimension ID string in the hash | `dimensionSalt` | 1.7.10 has integer dimension IDs. Whatever string the port hashes decides the layout. |
| Java 16+ records, switch expressions, `List.of`, `List.copyOf`, `Optional.isEmpty` | throughout `worldgen/highway` | The target runs on the Java 8 API. |
| Terrain height and biome inputs | via `CityPotential` and `HighwayLevelSource` | See [city-potential.md](city-potential.md). |

## Bugs

- `HWY-09`: when routes on the same axis and different levels share chunks, only the lowest level is kept there, so the higher route has an abrupt level change where they meet or part.
- `HWY-10`: "LRU" caches evict arbitrary completed entries (iteration order), including hot ones.

## Deviations in the new mod

None recorded yet; the new spec decides.
