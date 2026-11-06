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
| Beltway fits | All of the following hold: `half·√2 + 2 ≤ extent`, so there is city beyond the ring's corners; no landmark zone lies within the extent; and at least 60% of ring chunks are city base. |
| `random` | Picks a beltway with chance `clamp(0.15 + 0.8·(fullness − 0.5)/0.5, 0.15, 0.95)`, where fullness is the city-base share of the extent disc. The roll is a seeded hash of the center. It only applies when the beltway fits; otherwise the city gets a cut-through. |
| `beltway` | A beltway where it fits, otherwise a cut-through. |
| Level | One above the highest city level under the route, capped at the top city level. The highway is elevated, so the planner leaves no building under it and a street runs beneath. |
| Interchanges | A beltway city's links are written up to the ring and stop at Chebyshev distance `half`. The link's end chunk is a ring chunk at the ring's level, and the link's existing ramps carry it there. |
| Cut-through | It joins the two links whose edge plazas point most apart: lowest cosine, ties broken by link index. The route is an axis-aligned L between the two plazas, trying the bend nearer the center first. If that route crosses a blocked (landmark) zone, it tries the other bend, then the next pair. If no pair works, the city gets no city highway. Only the two joined links end at the cut-through level. |
| Towers | In a beltway city, chunks at Chebyshev distance ≥ `half − 1` get ring distance ≥ `INNER_END`. Towers and high-rises therefore stay inside the ring, and lower buildings and parks sit near it. |

Legacy-network profiles do not get the option. A cut-through and the ring's interchanges need the link graph's external connections, and the legacy grid network has no such connections to join.

## Consequences

- `rarecities` worlds gain beltways and cut-throughs. Old worlds and `default` are unchanged, and the legacy goldens stay byte-identical.
- Tests: `CityHighwayLayoutTest` covers determinism, the ring's size and margin, no building on the ring, interchange levels, the cut-through ends, no beltway without room, and every highway chunk lying on a link or a city highway.
- Open gap: an elevated plaza is a crossing piece open on every side, so an elevated end can show an open edge.
