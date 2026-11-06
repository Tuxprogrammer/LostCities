# 0013 · Highway and railway networks: obstacles, deliberate ends, dips under cores

- Status: Accepted (M06; decision log D241 to D256)
- Gate: M06
- Amends: [0006](0006-build-around-other-worldgen.md) §1 for highways (the margin is an obstacle as well as the core); the new-spec rows of [06-highways-railways](../new-spec/06-highways-railways/index.md) for route geometry, route levels and railways under cores
- Date: 2026-11-02

## Context

M06 ports upstream's intercity highway planner and railway grid ([06-highways-railways](../original-spec/06-highways-railways/index.md)). The new spec already settles most of it (D11, D30, D31): the planner runs synchronously and keeps no saved hubs; landmark cores are obstacles that routes avoid, detour around or end before; shallow railways dip under a core and climb back. The alpha review adds three constraints ([AR-10](../reviews/2026-11-02-alpha.md)):

- a slope never eats a highway;
- no highway runs into a city margin or a landmark's edge ring;
- a highway that must stop ends deliberately, as a ramp or a plaza, never cut.

Upstream gives every route one level from end to end, the average of its two hubs' city levels (`highwayLevelFromCities` 3). A route between a city at level 0 and one at level 4 runs at level 2, so it ends 12 blocks above the first city's streets and 12 blocks under the second's, cut off in the air and inside the ground. Upstream ends every route on a plain straight piece. This record decides how the port meets the review's constraints and fills in what the spec left to M06.

## Decision

### 1. Obstacles: cores and margins

A highway obstacle is any chunk of a landmark's core or of the ring around it: the edge ring under `BUILD_AROUND` (the city's margin, D149) or the avoid ring under `AVOID`. Landmarks under `IGNORE` are none.

- **Hubs.** A hub sample in an obstacle is skipped, like a sample below the minimum potential (P06-2), so no run starts or ends in one.
- **Shape.** Among the shapes the railway rule allows, one whose chunks cross no obstacle wins; then upstream's city penalty; then upstream's shape hash (P06-1).
- **Detour.** Where the chosen shape meets an obstacle, the obstacle's extent is the bounding box of the obstacle chunks joined to the one met (eight-neighbors, within 16 chunks). The route leaves its line on the chunk before the box, runs parallel one chunk past the box on the nearer side (ties north, then west), and rejoins its line on the chunk after it. The parallel run moves one chunk further out if it would lie along a railway row or column, as P06-1 says.
- **Dead end.** No detour is laid when the line turns inside the box, the box touches a hub or the 16-chunk window, the offset exceeds 8 chunks, or the detour meets another obstacle. The run then ends on the chunk before the box, and a new run starts on the first chunk past it.

The detour's legs are not moved off railway columns. Railways run underground, so a leg along one crosses nothing; the parallel run follows P06-1.

### 2. Deliberate ends: ramps and plazas

A route is one or more **runs**. Each run has two ends, a hub or a dead end, and each end has its own level: the hub's city level, or the ground's city level at a dead end.

- **Level profile.** A run keeps the route's level between its ends, and reaches each end at that end's own level by ramps of one city level (6 blocks) per chunk next to it. Ramps never fall on a corner or an end chunk, which stay flat, so bends, junctions and plazas keep their own parts. A run too short to make up the whole difference arrives as near its end's level as its ramp chunks allow.
- **Ramp.** A ramp is the straight piece (tunnel, open or bridge, by upstream's tests, the tunnel test at the ramp's upper level) written column by column along its axis. Each column stands up to 6 blocks higher, in half blocks: 0 at the low end and 12 half blocks at the high end, never more than one half block from its neighbor. A column a half block up gets a bottom smooth-stone slab on its deck, so the climb is a half-block staircase that needs no jump (the owner's rule for lanes, AR-7 and AR-11). With `rampSurface` (on for new worlds, D368) a whole-step column's top deck block becomes a smooth double stone slab, so the ramp reads as one smooth-stone surface instead of stripes of slab and road. The clearing above, the supports and the slabs follow the shifted deck.
- **Held chunks (KI-5).** With `highwayRampHolds` (on for new worlds, D-ki5-1) a run keeps flat, as it keeps its own corners, every chunk on it where another route near the same hub turns or ends; its ramps move to the next free chunks. Without it, two routes leaving one hub along one axis could ramp where the other turns: the lower deck wins (section 3), so the turn got a straight ramp piece instead of a bend and the other axis's walls cut the lane (RWG2 seed 1, chunk 235,-59). The `network.highway` scenario stands on a block's collision top, so a snow layer on a deck is no step, and `network.railway` scans for rails only in the line's own Y band, so a city rail above or below the line is not read as the line (D-ki5-2).
- **Plaza.** A run's end chunk is a junction piece at the end's level: a crossing (the `_bi` part) where one route ends there, or a bend or T where several meet. Upstream ends a run with a plain straight piece.
- **What the city reads.** A chunk with a ramp reports the ramp's higher level as its highway level, so a building or cellar keeps clear of the whole ramp.

So a highway leaves a city at the city's own street level, climbs or falls to the route's level outside it, and comes down or up again to the next city's streets. Before a landmark it ends on a plaza at the ground's level.

### 3. Overlapping routes (HWY-09)

Two routes overlap when they share more than one chunk along the same axis. Meeting end to end at a hub shares one chunk and does not count.

- **Shared level.** A route that overlaps another takes the lowest own level among the routes it overlaps, along its whole length. The routes considered are those owned by the cells within the search radius of its own owner cell. This is one step, bounded by the 25-cell query (P06-3).
- **One deck per axis.** Where several routes hit a chunk on one axis, the chunk shows one deck: the lowest, as upstream keeps; on a tie, a flat deck before a ramp; then the lower connection key. So a route passing through keeps its road where another climbs to its own hub beside it.

### 4. Nothing eats a highway

- **Order.** Highways and railways are written last in a chunk's terrain phase: after the city's content, street borders, ruins and rubble in a city chunk, and after the blend in any other chunk. No blend, soft-edge strip, border or retaining wall runs after them in that chunk. Highways never enter a margin, the only place a soft-edge strip runs.
- **Footprint.** The columns above a highway's deck (open roads, bridges, ramps and plazas) are in the footprint, so RWG's trees and boulders are dropped there. A tunnel's hill keeps its decoration. A chunk with a tunnel, a railway part or a rail dungeon has a guarded band instead: the Y range from one block under the lowest part's floor to the top of the highest, in every column, which the write guard treats as footprint. Biomes O' Plenty writes its poison, lava and water lakes from its own biome decorator with no populate event to veto (`LakesForcedGenerator`, Glitchfiend/BiomesOPlenty branch `BOP-1.7.10-2.1.x`), and on classic RWG one flooded a tunnel's deck; the band keeps those lakes, and the rest of RWG's decoration, out of the tunnels and lines (D256).
- **Population vetoes.** A highway chunk's population area gets the city's vetoes: no lake, lava lake, dungeon or oil deposit can carve into it.

### 5. Railways under landmark cores (D30, D31 P06-4)

A railway part whose top is at least 8 blocks under the lowest oracle ground around a core chunk (the chunk and its four neighbors) passes under it unchanged.

- **Dips.** A shallower straight line (east-west `HORIZONTAL` or north-south `VERTICAL`) dips. Over the run of identical chunks between its junctions, the depth is the deepest level any of its core chunks needs, and never below Y 5. The line descends before the first such core chunk and climbs back after the last, with upstream's descent parts: one level for an odd difference first, then two levels a chunk, as upstream's `startDescending` parity does. Slopes lie in chunks outside the core. A north-south slope is the descent part turned 90°.
- **No room.** Where the line has no room for its slopes before its junctions, or a junction, station or slope would itself be too shallow under a core, that core chunk carries no railway. This is a gap in the network, kept for the case it was the only answer (a junction or station within a few chunks of a low-lying village). With `railwayCoreEnds` (on for new worlds, D367) the straight line on each side of the gap stops on a buffer in the chunk before it (`railshorizontalend` along X; along Z the last four rows of the turned part cleared and a stop block across the tracks), so no rail ends cut. A gap on both sides of a chunk leaves it empty.
- **Stations.** A station spot in a core is no station; its line passes as plain track. A station spot in a margin is an underground station without a staircase, since the margin carries nothing the city makes. No rail dungeon stands in a core or a margin (E7).

### 6. Cost (P06-8)

The highway planner's caches are smaller than upstream's, so a world's caches stay within B5 (0005 §8): 1,024 hubs, 64 candidate lists (they are read only to make a selection, which has its own cache), 256 selection and route lists, and 512 chunks; the railway layout keeps 512 entries in each of its two caches. Each estimate is counted in the B5 figure: about 0.8 MB of the world's 15.9 MB with scattered structures on.

A hub cell samples up to 64 city potentials, each of which may need a height, so it is computed through a `PlanningGate`. While the plan prefetcher probes, a missing hub ends the attempt like a missing height. The prefetcher's step then computes the hub with the probe on and fetches the one height it stops at, and asks again. A chunk phase computes a hub only when the prefetcher has not reached it. The city potential is the layout's own city factor, read without caching it; its pruning bound is the raw factor times the world style's largest biome multiplier.

### 7. Smaller choices

- **Subways.** The `railways` module also switches off the cities' rail corridors under their streets ("subways"), as the config documents; their random draws stay, so nothing else in a city moves.
- **Writing.** Network parts are picked from their lists by a random of their own (the bundled lists hold one part each), and draw palette blocks from a sequence of their own, so a city chunk's own draws are unchanged where a highway crosses it.
- **Layout version.** `ProfileSnapshot.LAYOUT_VERSION` becomes 3. A world pinned before M06 keeps generating, and logs that its new chunks have highways and railways its old ones lack.

## Consequences

- **Code.** `layout.highway` (the planner, runs, decks, pieces), `layout.rail` (the grid and the dips) and `layout.Networks` are built by `LayoutPlanner` from `LayoutInputs.networks`. `worldgen.city.NetworkWriter` writes them; `worldgen.overlay.NetworkGate` schedules hubs; `NetworkScenarios` checks them in the world.
- **Spec.** The new-spec area's rows for highways in cores, route geometry, overlapping routes, planning cost, railways and cores, and the railing openings record this.
- **M12.** An edge lot never takes a highway or railway chunk: M12 reads the networks through the layout's infrastructure view before it draws a lot, and keeps a lot over a rail corridor or rail dungeon undug, so no highway runs through a pit (`NetworkWriterTest`).
- **M07.** Scattered footprints keep off highway chunks and the highway planner ignores them, so the order is one-way (D254). A highway-side building opens the railing of the flat piece it faces (D255).
- **Shots.** Both alpha lists add a highway, a ramp, a station and a railway view.

## Alternatives considered

| Alternative | Why not |
|---|---|
| Keep upstream's one level per route and cut highways at hubs | AR-10: a highway may not stop cut; the owner walks from city to city |
| End every route at a turning circle at the cruise level | Still ends in the air or in a hill when the hub's city is at another level |
| Ramps of two levels per chunk (12 blocks over 16) | A one-block rise per column needs a jump; the owner's lanes step half blocks |
| Ramp parts as new assets | No upstream part exists, and a part per level difference and part kind multiplies; a column shift reuses every bundled and custom highway part |
| Only the core as obstacle, margins allowed (the spec's first reading) | AR-10: no highway runs into a margin; the soft-edge strip runs there |
| The exact one-step overlap set (cells within twice the radius) | Needs hubs within five search radii of a query, 441 cells instead of 289, to fix a case that needs three routes overlapping across cells two radii apart |
| Lower a junction or station under a core with its line | Rebuilds upstream's station approaches and splits; a gap at a low-lying village close to a junction is rare |
| A worker thread for hub cells | Vanilla's `IntCache` and RWG's chunk manager are not thread-safe (D125) |
