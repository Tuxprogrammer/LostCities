# 0015 · City link network: highways and railways that never dead-end

- Status: Accepted for §1, §2 and §4 (landed: graph, routing, walk test); §3 and §5 to §7 Proposed (lane p4b continuation, see `handoff/lane-p4b-links.md`)
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

### 3. Pieces and writing (proposed)

- **Diagonal piece**, code-generated in `NetworkWriter` (no part asset: a part rotates in quarter turns only): the deck is the set of columns within the highway's half-width of the chunk's corner-to-corner line, with the existing palette's deck, railing and support blocks. Its corner triangles spill into the two side neighbors; the writer clears and decks those columns too (the neighbors read the diagonal's hit).
- **Transition piece**: half straight (mid-side to center), half diagonal (center to corner).
- **Rail zig-zag**: in a diagonal chunk the track alternates one block along X and one along Z (vanilla rails auto-curve into a staircase that carts follow), one track per direction, offset by the track spacing.
- **Fallback.** Where a diagonal chunk would be a bridge or tunnel (0013 §2's tests), the diagonal run becomes a corner staircase of straight and bend parts, so the existing bridge and tunnel parts are reused.
- **Levels.** A link runs at the lower of its two cities' levels with 0013 §2 ramps on straight chunks only; diagonals and transitions stay flat.

### 4. Guarantee and test

Every network chunk comes from a link path whose ends are nodes, and consecutive path chunks are 8-neighbors, so walking from any network chunk along its link reaches a city both ways. `CityLinkGraphTest.everyNetworkChunkLiesOnACityToCityLink` checks this over 1024 × 1024 chunks on two seeds, including that every chunk of every path reports the link from its own cell's index.

### 5. Junctions and city entry (proposed)

- Where links cross outside a city at one level, the chunk is a crossing (the `_bi` plaza part) at that level; at different levels the lower passes under.
- A link's first and last chunks inside the city's radius are not written; the link joins the city at the first city-edge street chunk on its path with the existing 0013 §2 plaza and ramp, so traffic enters the street grid.

### 6. Obstacles (proposed)

Cores and margins (0013 §1) are not detoured around: a link crossing one is dropped from the graph when the node's other choices remain (the node keeps its next nearest unobstructed node), so no link stops at a landmark. Railways keep the 0013 §5 dips.

### 7. Stations hook (lane 4c)

`CityLinkGraph.Link.bothEndsCities()` is the hook: stations go only on links for which it holds (all links of this graph). A station chunk is a link's first or last straight chunk outside the city radius.

## Consequences

- `networkLayout` (`legacy` | `links`) is declared, deferred, default `legacy`; `rarecities` switches to `links` and `railwaysCanEnd` false when the writer reads it.
- Default profiles and `legacy` worlds generate exactly as before: no generation code reads the graph yet.
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
