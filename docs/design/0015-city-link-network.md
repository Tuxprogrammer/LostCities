# 0015 · City link network: highways and railways that never dead-end

- Status: Accepted for §1–§6 (graph, routing, highway pieces, walk test, junctions, city entry, staircase fallback, landmark drops; lane p4b, sessions 3–5); rails (§3, D321/14: coded staircase lines from city-center station to station, no line through a shallow core) and §7 (stations only on links whose both ends are cities) Accepted in session 6; ramp/flat overlap fixed (D323); live in `rarecities` (D324)
- Gate: city-realism Phase 4b
- Amends: [0013](0013-highway-and-railway-networks.md) for worlds with `networkLayout=links`; 0013 stays the rule for `legacy`
- Date: 2026-11-06

## Context

In 0.1.1 a `rarecities` highway or railway may stop in the open: the M06 planner picks hubs from city potential, plans against the unfiltered city mask, and `rarecities` sets `railwaysCanEnd` true. The city-realism plan (Phase 4, owner decisions 7 and 8) asks that in grid-placed worlds every highway and railway chunk lie on a link between two cities, and that links run point to point with true 45° diagonal pieces. Phase 4a placed `rarecities` centers on a jittered grid ([CityGrid](../../src/main/java/com/tuxprogrammer/lostcities/layout/CityGrid.java), 64-chunk cells, ±16 jitter), which makes a node query O(cells).

## Decision

### 1. Graph

- **Nodes.** The grid centers that yield a city: `LayoutPlanner.centerYieldsCity` (the filtered mask; fixes the 0.1.1 known issue). Predefined cities are not nodes yet.
- **Wiring.** `Networks` builds the graph when `NetworkSettings.links()` is on (the profile reads `networkLayout=links`) and cities are grid-placed; otherwise `Networks.links()` is `null`. Its `Nodes.cityCenter` is `CityGrid.center` filtered by `centerYieldsCity`, whose `characteristics(...).isCity` never reads the infrastructure view, so the graph does not depend on the networks it drives (D312).
- **Edges.** Each node chooses its 3 nearest nodes within 160 chunks per 64 chunks of cell (`CityLinkGraph.maxDistanceFor`, never under 160; 640 on `rarecities`' 256-chunk cells, D-city-scale-1) (Euclidean, ties to the lower chunk X, then Z). A link exists when either end chose the other (symmetric union). Ends are stored lower end first.
- **Queries** (`layout/link/CityLinkGraph`). A link through a chunk has both ends within the cap (`maxDistance`) of it, so they lie within `reach = ceil(maxDistance / cell) + 1` cells (4 for `rarecities`, at 64- and 256-chunk cells alike). The cell-index cache holds fewer cells as cells grow, about the same chunk area at every cell size. Per-cell LRU caches hold each cell's node, each node's chosen links and each cell's chunk index (chunk to link pieces). A cell index is built from the chosen links of the (2·reach+1)² cells around it; no per-chunk hash scan. The node oracle reads terrain oracles only, so the graph never loads a chunk and answers the same in any order.

### 2. Routing

`layout/link/LinkRaster` turns a link into an octilinear chain of 8-neighbor chunks, always from the lower end: a straight lead along the major axis (half the axial remainder, rounded down), a 45° diagonal run (the minor delta), and a straight trail. Each chunk is `END`, `STRAIGHT`, `DIAGONAL` (enters and leaves at opposite corners) or `TRANSITION` (mid-side to corner); a link has at most two transitions. A diagonal step joins two chunks at a shared corner, where the diagonal piece's road crosses.

### 3. Pieces and writing (highways landed; rails proposed)

- **Highway view** (`layout/link/LinkHighways`, D314). In links mode `Networks.highway` answers from it instead of the legacy planner. Each written path chunk is a `HighwayInfo.Piece.link` built from the sides or corners it opens on: `STRAIGHT` or `RAMP` (two opposite sides, the legacy axis convention), `DIAGONAL` (two opposite corners; quarter turn 0 for NW–SE, 1 for NE–SW) or `TRANSITION` (a side and a far corner; quarter turn = the side, west 0 to south 3). `Piece.levelAt(dx, dz)` gives the deck level at each opening; `xLevel`/`zLevel` report diagonals, transitions, plazas and spills on both axes, so buildings keep clear.
- **Levels.** A link leaves each plaza at its city's level and changes one level per ramp chunk: diagonal chunks first, nearest the middle of the written path, then straight chunks nearest the middle. Transitions and plazas stay flat. When a path has too few rampable chunks, the far plaza takes the level the ramps reach, so no seam ever steps. Each link is planned alone this way first (its base); crossings with other links then fix more levels (§5, D-link-crossing-1), and the ramps are re-placed in each stretch between the fixed levels by the same rule, nearest that stretch's middle.
- **Diagonal piece**, code-generated (`layout/link/LinkGeometry`, `NetworkWriter.linkColumns`, D316; no part asset, as parts rotate in quarter turns only): the strip within 8 blocks of the corner-to-corner line, cut square at both corners. Each covered column copies the cross-section of the slot's first straight part (`bridge` or `tunnel` by the generating chunk's tunnel test): the part row by the distance across the road, the part column by the distance along it, mod 16. A diagonal ramp climbs 12 half blocks evenly along its length (bottom slab on odd half blocks, as on straight ramps). Near each corner the strip spills into the two side chunks; those chunks report the spill (`HighwayInfo.spill()`) and write those columns themselves from the neighbor's piece, so no chunk writes outside itself.
- **Transition piece**: a straight strip from the middle of its side to the center, a diagonal strip from the center to its corner, and a disc of radius 8 at the center for the outside of the turn. A plaza that a diagonal leaves by its corner adds a disc at that corner, outside its own chunk, to fill the wedges.
- **Overlaps.** Where columns of several pieces of different levels meet, the highest deck wins (pieces at one level merge first, §5). Straight pieces, ramps and plazas keep their parts; the coded columns are written after them.
- **Spill guard** (built, D317). A chunk a neighbor's diagonal deck spills into (`HighwayInfo.spill`, `spillLow`, `spillHigh`) puts the spilled levels in its footprint (`NetworkWriter.footprintBottom`), or in its guard band where the deck is a tunnel (`NetworkWriter.guardBand`), so RWG's trees and other mods' decoration cannot grow through the deck. The whole chunk is guarded down to the spill's city ground, as for a chunk's own pieces. Test: `NetworkWriterTest.spillFromALinkDiagonalIsInTheFootprintOrGuardBand`.
- **Rail zig-zag** (proposed, next lane): in a diagonal chunk the track alternates one block along X and one along Z (vanilla rails auto-curve into a staircase that carts follow), one track per direction, offset by the track spacing.
- **Staircase fallback** (built, D319; owner decision 8). A link whose 45° deck would cover a bridge or tunnel chunk takes a corner staircase instead. The test (`LinkHighways.Ends.bridgeOrTunnel`, answered in `Networks` from the terrain oracle) runs over every chunk a corner opening touches, the diagonal or transition chunk and both side chunks it spills into, at the levels the 45° profile gives there: a tunnel where the ground stands more than 3 blocks above the higher deck (`ChunkPlan.isTunnel` outside a city), a bridge over a water biome or over ground more than one level below the lower deck. On the staircase every 45° step becomes a step along X, then one along Z, through the side chunk between, which holds a flat bend part (`Piece.sides`); diagonals become bends, transitions straights or bends, and a plaza's corner opening its X side. Every piece is then a part, so bridge and tunnel parts join as on the legacy network. Ramps move to straight chunks only; when too few remain, the far plaza takes the reached level as before. The decision is per link (a link has one 45° run). Tests: `LinkHighwaysTest.aLinkOverABridgeOrTunnelTakesTheStaircase`, `LinkHighwayWalkTest.linksTakeTheStaircaseOverARiverAndThroughAHill` (the walk follows the bend chunks and asserts no 45° step covers a bridge or tunnel).

### 4. Guarantee and test

Every network chunk comes from a link path whose ends are nodes, and consecutive path chunks are 8-neighbors, so walking from any network chunk along its link reaches a city both ways. `CityLinkGraphTest.everyNetworkChunkLiesOnACityToCityLink` checks this over 1024 × 1024 chunks on two seeds, including that every chunk of every path reports the link from its own cell's index. `LinkHighwayWalkTest` walks every link through the real planner (`networks().highway(x, z)`) on flat land (two seeds) and on terraces one level apart: each written chunk has a piece, the deck levels match at every seam, and the highway ends only on a plaza whose next chunk inward lies inside the city.

### 5. Junctions and city entry

- **City entry** (built, D314). A link's chunks inside an end's city radius (the grid radius, from the center chunk to the chunk) are not written; the first chunk outside is a plaza (the `_bi` crossing part) at that city's level, open on every side, so it meets the city's edge streets. When the chunk inward of that plaza is not city (the rim failed the threshold), the link runs on as a spur to the first city chunk on its path, which becomes the plaza (D391); a landmark's core or margin, or a beltway ring the link does not join, before any city chunk keeps the plaza at the radius. A link whose cities' radii cover its whole path writes nothing.
- **Shared chunks** (built, D315, D318). Identical pieces merge. All flat pieces at one level become one piece with all their openings: a plaza when one of them is a plaza; else a straight, bend, T or crossing part when they open only on sides (legacy quarter turns); else a `LINK_JUNCTION`, written in code as a strip from the center to each opening plus a disc at the center, where a column covered by several strips copies the cross-section of the one it lies deepest in, so railings stand only on the junction's outer edge. Ramps and pieces at other levels stay, written lowest first, so the lower passes under: only crossings that could not be matched (below) and city highways (0017) the link does not join. Tests: `LinkHighwaysTest.flatPiecesAtOneLevelBecomeOneJunction`, `aLinkJunctionIsOneDeckWithRailingsOnlyOnItsOuterEdge`.
- **Ramps never share a chunk** (D323): a link's ramp candidates skip every chunk another link's path hits.
- **Crossings meet at one level** (built, D-link-crossing-1). A chunk written by several links is a crossing. Its level is the links' shared base level when they agree; else the level between their base levels that every link reaches from both of its plazas on its own, at the fewest extra ramps in total, the lower on a tie. Walking from its lower plaza, each link keeps the crossings at its own base level, and another crossing when its ramp room (rampable chunks where no other link writes a step; a chunk another link's path crosses unwritten, inside a city, counts, so links converging on one city from the same side can still meet) reaches that level from the last kept one and, from there, the next base-level crossing or the far plaza. A crossing every link there keeps is matched: each link re-places its ramps so it passes every matched crossing at that level, one level per ramp chunk, never on a shared chunk, and the flat-piece merge makes it a crossing, T or junction. A crossing with no level every link reaches, or one a link drops, keeps its grade separation (traced as `linkCrossing … matched=false`). Plaza levels never move, so city entry, the ≥2 links per city and D348 stand; a link with no crossing to move keeps its base profile. It reads only base profiles, its own and its neighbors' (two hops for the keep test), so it is order-independent and never loads a chunk. A re-planned link that now needs the staircase takes it when the straight chunks have the room, else keeps its 45° deck. Tests: `LinkHighwaysTest.linksCrossingAtDifferentCityLevelsMeetInOneJunction`, `aCrossingTooNearBothPlazasKeepsItsGradeSeparation`.

### 6. Obstacles (built, D320)

A node chooses its 3 nearest nodes within 160 chunks whose rasterized path crosses no landmark core or margin (0013 §1; `CityLinkGraph.Nodes.blocked`, answered in `Networks` from the landmark zones). A link whose straight path is blocked detours (D390, reshaped by D-perf2-route-2): the straight raster is kept up to 16 chunks before its first blocked chunk and from 16 chunks after its last, and only the stretch between is replaced by a bounded A* search over chunks (`DetourSearch`) that turns at most 45° per chunk, also where it joins the kept raster, so it uses only straight, diagonal and transition pieces; every chunk it visits lies within the link cap of both ends, so the cell reach still bounds every path chunk, and within 64 chunks of the straight line. When that stretch finds no path the whole link is searched once; after 50 000 expansions the link has no route. Ties break by coordinates, so the path is the same whichever end asks. Only a candidate no detour clears is passed over for the next nearest; when every candidate is blocked, the node keeps its nearest alone. So a city has at least two links unless fewer than two other cities lie within 160 chunks (a lone island) or every path to them is blocked even with detours; such a city keeps what it has, and the trace logs `linkGraph node=... chosen=1`. Railways keep the 0013 §5 dips. Tests: `CityLinkGraphTest.linksDetourAroundLandmarksOrPassToTheNextChoice`, `CityLinkDetourTest`.

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

## Junctions and through-lines (lane 4b verify)

- **T junctions are switches** (D356). A chunk with track on three sides lays vanilla's unpowered curve at its
  center with a lever beside it on the free side; the lever re-shapes the curve, so every arm reaches every other.
  Four-way crossings remain an east-west straight: a known limit, since one vanilla rail cannot carry both lines.
- **Lines stop at every city they cross** (D357). `LinkRails.route` splits a line at any third city's center its
  staircase passes through, so the line arrives and leaves along X and the center is a station.
- **Underground landmarks block lines** (D359). A core whose landmark reaches underground inside its footprint
  (a LootGames room) blocks a line at any railway level, so the line carries no track at all (D322). The railway
  check counts boundaries an explosion reaches without judging them (D360).
- **Roguelike dungeons keep off lines** (D361). A Roguelike Dungeons tower's top level lies at Y 50 and spreads up
  to 122 blocks from its site (`RoguelikeOracle.LEVEL_REACH`, from the mod's 1.6.6-GTNH source), far past the 21 × 21
  lot, so blocking the lot did not save the line on seed 4242. The steering mixin refuses any candidate site, predicted
  or not, with link track within that reach (`SteeringRule.Outcome.RAIL`); the line keeps its track and the dungeon
  takes another candidate or none.
- **Verified by** `LayoutGoldenTest.rareCitiesLinkNetwork`, `LinkNetworkWalkTest` and the links branches of the
  `network.highway` and `network.railway` scenarios (D358).

## Still open

Diagonal piece looks after the first shots (the owner expects to adjust them); whether predefined cities become nodes.
