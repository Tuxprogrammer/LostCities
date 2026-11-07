# 0017 · City highway layout: beltway or cut-through

- Status: Accepted
- Gate: none (city-realism plan, phase 5a+5b)
- Amends: [0015](0015-city-link-network.md) §5 (city entry)
- Date: 2026-11-06

## Context

Under [0015](0015-city-link-network.md), a link stops at the first chunk outside each end city's radius. Inside the city the highway simply ends at a plaza, so a city has no highway of its own. The city-realism plan asks for one of two layouts, picked at random per city and weighted by how much room the city has. A large city that generates in full gets a ring around its core. A small city, or one that tapers around a village, dungeon or landmark core, gets an elevated road that crosses it.

## Decision

The option is `cityHighwayLayout`, with the values `none`, `beltway`, `cutthrough` and `random`. It is read only in `networkLayout=links`. Its default is `random`, and it is pinned to `none` for old worlds (D282). Code: `layout/link/CityHighways`, wired in `Networks` and `LinkHighways`.

| Rule | Value |
|---|---|
| Extent | The city's extent in chunks is `radius · (1 − cityThreshold) / 16`. |
| Ring size | `half = max(5, round(0.5 · extent))`. The ring is the square at Chebyshev distance `half` from the center, 8·`half` chunks long, and the inner block is at least 9 x 9. |
| Beltway fits | All of the following hold: `half·√2 + 2 ≤ extent`, so there is city beyond the ring's corners; no ring chunk lies in a landmark's core or margin; and at least 60% of ring chunks are city base. The preferred half-size is tried first, then each smaller one down to 5, then each larger one, so a landmark inside the city moves the ring until it clears (D392, replacing the old rule that any landmark zone in the extent ruled the beltway out). |
| `random` | Picks a beltway with chance `clamp(0.15 + 0.8·(fullness − 0.5)/0.5, 0.15, 0.95)`, where fullness is the city-base share of the extent disc. The roll is a seeded hash of the center. It only applies when the beltway fits; otherwise the city gets a cut-through. |
| `beltway` | A beltway where it fits, otherwise a cut-through. |
| Level | One above the highest city level under the route, capped at the top city level. The highway is elevated, so the planner leaves no building under it and a street runs beneath. |
| Interchanges | A beltway city's links are written up to the ring and stop at Chebyshev distance `half`. The link's end chunk is a ring chunk at the ring's level, and the link's existing ramps carry it there. |
| Cut-through | It joins the two links whose edge plazas point most apart: lowest cosine, ties broken by link index. The route is an axis-aligned L between the two plazas, trying the bend nearer the center first. If that route crosses a blocked (landmark) zone, it tries the other bend, then the next pair. If no pair works, the city gets no city highway. Only the two joined links end at the cut-through level. |
| Ramp room | A link joins a city highway only if it can always climb to it: the straight chunks no other link crosses, strictly between its written ends, must number at least the worst level gap. That gap is the larger of the gap to the other city's own level and the gap to any level the other city's highway could take, from `min(1, top)` to one above the highest city level within that city's radius plus two chunks. The bound reads the city map only, so two cities never wait on each other's plans. A beltway's link without the room is not joined and stops at the city's edge as under 0015, outside the ring. A cut-through pair needs the room on both links, else the next bend or pair is tried. |
| Interchange deck | Where a joined link meets its city highway, the link adds only its own opening and the two pieces merge into one deck: a straight, bend or T, or a link junction when the link leaves by a corner. There is no plaza, so no edge of the elevated deck is open; railings stand on every side without an opening, as on 0015's junctions. A cut-through's end piece opens only inward. |
| Towers | In a beltway city, chunks at Chebyshev distance ≥ `half − 1` get ring distance ≥ `INNER_END`. Towers and high-rises therefore stay inside the ring, and lower buildings and parks sit near it. |

Legacy-network profiles do not get the option. A cut-through and the ring's interchanges need the link graph's external connections, and the legacy grid network has no such connections to join.

## Consequences

- `rarecities` worlds gain beltways and cut-throughs. Old worlds and `default` are unchanged, and the legacy goldens stay byte-identical.
- Tests: `CityHighwayLayoutTest` covers determinism, the ring's size and margin, no building on the ring, interchange levels over five seeds, unjoined beltway links ending off the ring, no elevated plaza and every side opening of a city highway deck met by a deck that opens back, the cut-through ends, no beltway without room, and every highway chunk lying on a link or a city highway.
- The ramp-room bound is conservative: a short link between close cities may stay unjoined even when the other city's actual highway would have been in reach.
