# 0015 · City link network: highways and railways that never dead-end

- Status: Accepted for §1, §2 and §4 (graph, routing, walk test), §3's highway pieces and §5's city entry and shared chunks (lane p4b, session 3); rails (§3), the staircase fallback, §6 and §7 Proposed (see `handoff/lane-p4b-links.md`)
- Gate: city-realism Phase 4b
- Amends: [0013](0013-highway-and-railway-networks.md) for worlds with `networkLayout=links`; 0013 stays the rule for `legacy`
- Date: 2026-11-06

## Context

In 0.1.1 a `rarecities` highway or railway may stop in the open: the M06 planner picks hubs from city potential, plans against the unfiltered city mask, and `rarecities` sets `railwaysCanEnd` true. The city-realism plan (Phase 4, owner decisions 7 and 8) asks that in grid-placed worlds every highway and railway chunk lie on a link between two cities, and that links run point to point with true 45° diagonal pieces. Phase 4a placed `rarecities` centers on a jittered grid ([CityGrid](../../src/main/java/com/tuxprogrammer/lostcities/layout/CityGrid.java), 64-chunk cells, ±16 jitter), which makes a node query O(cells).

## Decision

### 1. Graph

- **Nodes.** The grid centers that yield a city: `LayoutPlanner.centerYieldsCity` (the filtered mask; fixes the 0.1.1 known issue). Predefined cities are not nodes yet.
- **Wiring.** `Networks` builds the graph when `NetworkSettings.links()` is on (the profile reads `networkLayout=links`) and cities are grid-placed; otherwise `Networks.links()` is `null`. Its `Nodes.cityCenter` is `CityGrid.center` filtered by `centerYieldsCity`, whose `characteristics(...).isCity` never reads the infrastructure view, so the graph does not depend on the networks it drives (D-p4b-4).
- **Edges.** Each node chooses its 3 nearest nodes within 160 chunks (Euclidean, ties to the lower chunk X, then Z). A link exists when either end chose the other (symmetric union). Ends are stored lower end first.
- **Queries** (`layout/link/CityLinkGraph`). A link through a chunk has both ends within 160 chunks of it, so they lie within `reach = ceil(160 / cell) + 1` cells (4 for `rarecities`). Per-cell LRU caches hold each cell's node, each node's chosen links and each cell's chunk index (chunk to link pieces). A cell index is built from the chosen links of the (2·reach+1)² cells around it; no per-chunk hash scan. The node oracle reads terrain oracles only, so the graph never loads a chunk and answers the same in any order.

### 2. Routing

`layout/link/LinkRaster` turns a link into an octilinear chain of 8-neighbor chunks, always from the lower end: a straight lead along the major axis (half the axial remainder, rounded down), a 45° diagonal run (the minor delta), and a straight trail. Each chunk is `END`, `STRAIGHT`, `DIAGONAL` (enters and leaves at opposite corners) or `TRANSITION` (mid-side to corner); a link has at most two transitions. A diagonal step joins two chunks at a shared corner, where the diagonal piece's road crosses.

### 3. Pieces and writing (highways landed; rails proposed)

- **Highway view** (`layout/link/LinkHighways`, D-p4b-6). In links mode `Networks.highway` answers from it instead of the legacy planner. Each written path chunk is a `HighwayInfo.Piece.link` built from the sides or corners it opens on: `STRAIGHT` or `RAMP` (two opposite sides, the legacy axis convention), `DIAGONAL` (two opposite corners; quarter turn 0 for NW–SE, 1 for NE–SW) or `TRANSITION` (a side and a far corner; quarter turn = the side, west 0 to south 3). `Piece.levelAt(dx, dz)` gives the deck level at each opening; `xLevel`/`zLevel` report diagonals, transitions, plazas and spills on both axes, so buildings keep clear.
- **Levels.** A link leaves each plaza at its city's level and changes one level per ramp chunk: diagonal chunks first, nearest the middle of the written path, then straight chunks nearest the middle. Transitions and plazas stay flat. When a path has too few rampable chunks, the far plaza takes the level the ramps reach, so no seam ever steps.
- **Diagonal piece**, code-generated (`layout/link/LinkGeometry`, `NetworkWriter.linkColumns`, D-p4b-8; no part asset, as parts rotate in quarter turns only): the strip within 8 blocks of the corner-to-corner line, cut square at both corners. Each covered column copies the cross-section of the slot's first straight part (`bridge` or `tunnel` by the generating chunk's tunnel test): the part row by the distance across the road, the part column by the distance along it, mod 16. A diagonal ramp climbs 12 half blocks evenly along its length (bottom slab on odd half blocks, as on straight ramps). Near each corner the strip spills into the two side chunks; those chunks report the spill (`HighwayInfo.spill()`) and write those columns themselves from the neighbor's piece, so no chunk writes outside itself.
- **Transition piece**: a straight strip from the middle of its side to the center, a diagonal strip from the center to its corner, and a disc of radius 8 at the center for the outside of the turn. A plaza that a diagonal leaves by its corner adds a disc at that corner, outside its own chunk, to fill the wedges.
- **Overlaps.** Where columns of several pieces meet, the highest deck wins. Straight pieces, ramps and plazas keep their parts; the coded columns are written after them.
- **Rail zig-zag** (proposed, next lane): in a diagonal chunk the track alternates one block along X and one along Z (vanilla rails auto-curve into a staircase that carts follow), one track per direction, offset by the track spacing.
- **Fallback** (not built): a corner staircase of straight and bend parts where a diagonal would be a bridge or tunnel. For now the coded strip copies the bridge or tunnel part's cross-section.

### 4. Guarantee and test

Every network chunk comes from a link path whose ends are nodes, and consecutive path chunks are 8-neighbors, so walking from any network chunk along its link reaches a city both ways. `CityLinkGraphTest.everyNetworkChunkLiesOnACityToCityLink` checks this over 1024 × 1024 chunks on two seeds, including that every chunk of every path reports the link from its own cell's index. `LinkHighwayWalkTest` walks every link through the real planner (`networks().highway(x, z)`) on flat land (two seeds) and on terraces one level apart: each written chunk has a piece, the deck levels match at every seam, and the highway ends only on a plaza whose next chunk inward lies inside the city.

### 5. Junctions and city entry

- **City entry** (built, D-p4b-6). A link's chunks inside an end's city radius (the grid radius, from the center chunk to the chunk) are not written; the first chunk outside is a plaza (the `_bi` crossing part) at that city's level, open on every side, so it meets the city's edge streets. A link whose cities' radii cover its whole path writes nothing.
- **Shared chunks** (built, D-p4b-7). Identical pieces merge. Two plazas, or a straight piece on each axis, at one level become one crossing. Any other pieces all stay, written lowest first, so at different levels the lower passes under.
- **Not built:** bends and T junctions where links meet at one level other than straight × straight; they are written as overlapping pieces.

### 6. Obstacles (proposed)

Not built yet: links cross cores and margins (0013 §1) like any other chunk. The proposal stands: a link crossing one is dropped from the graph when the node's other choices remain (the node keeps its next nearest unobstructed node), so no link stops at a landmark. Railways keep the 0013 §5 dips.

### 7. Stations hook (lane 4c)

`CityLinkGraph.Link.bothEndsCities()` is the hook: stations go only on links for which it holds (all links of this graph). A station chunk is a link's first or last straight chunk outside the city radius.

## Consequences

- `networkLayout` (`legacy` | `links`) is declared, deferred, default `legacy`; `rarecities` switches to `links` and `railwaysCanEnd` false when the writer reads it.
- Default profiles and `legacy` worlds generate exactly as before: only `networkLayout=links` reads the graph, and no shipped profile sets it yet.
- New spec: the `networkLayout=links` row in [06-highways-railways](../new-spec/06-highways-railways/index.md).

## Alternatives considered

| Alternative | Why not |
|---|---|
| Bresenham chunk lines | Alternating axial and diagonal steps make almost every chunk a transition: a zig-zag road |
| Corner staircase everywhere (owner's option 2) | The owner chose true diagonals; staircase is the fallback at bridges and tunnels |
| Diagonal parts as assets | Parts rotate in quarter turns; a 45° deck needs per-column geometry, which the ramp writer already does in code |
| Region cache of hashed centers (plan option A) | The jittered grid (4a) makes the node query O(cells) |

## Still open

Diagonal piece looks after the first shots (the owner expects to adjust them); whether predefined cities become nodes.
