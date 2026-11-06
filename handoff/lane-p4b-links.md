# Handoff: lane p4b-links (city link network)

Branch `lane/p4b-links`, based on `gtnh` at `71c54a50` (4a merged). Do not merge `gtnh` into it. Slot `p4b`, real-pack `--instance 4`.

Why handed off: the session began at about 156k of a 200k context budget, so this lane landed the pure core and stopped.

## Design

Read [docs/design/0015-city-link-network.md](../docs/design/0015-city-link-network.md). §1 (graph), §2 (octilinear routing) and §4 (walk test) are landed and accepted; §3 (pieces), §5 (junctions, city entry), §6 (obstacles) and §7 (station hook) are proposals for you to implement. Change them if implementing them shows a better way, and log the change.

## Done

- `layout/link/CityLinkGraph`: nodes from a `Nodes` oracle, 3 nearest within 160 chunks, symmetric `links(cell)`, per-chunk `hits(x, z)` from per-cell LRU indexes. `Link.bothEndsCities()` is the station hook for 4c.
- `layout/link/LinkRaster`: straight lead, diagonal run, straight trail; kinds `END`, `STRAIGHT`, `DIAGONAL`, `TRANSITION`, with in and out step directions per chunk.
- Tests: `CityLinkGraphTest` (3-nearest by brute force, 160 cap, symmetry, order independence, network walk on 1024 × 1024 chunks on two seeds) and `LinkRasterTest`. They pass locally (javac and a reflective runner); see the report for the remote build.
- `networkLayout` is declared (`legacy` | `links`, default `legacy`, **DEFERRED**). `rarecities` is not changed yet, and neither is `railwaysCanEnd` (D-p4b-1).
- Docs: 0015, its design index row, the `docs/config.md` row, the `06-highways-railways` spec row, and decision-log rows D-p4b-1 to D-p4b-3.

## Left, in order

1. **Wire the oracle.** In `Networks`, when the profile reads `networkLayout=links` (add it to `LayoutProfile`/`CityProfiles` the way 4a added `cityGrid`; do not touch `ProfileSnapshot`, which belongs to the floor-pitch lane), build `CityLinkGraph` with `Nodes.cityCenter(cx, cz)` returning `grid.center(cx, cz)` when `planner.centerYieldsCity(...)`. Check that `characteristics(...).isCity` never reads `infrastructure`; it did not at `71c54a50`, which avoids a cycle.
2. **Link highway view.** Add a `LinkHighways` class that implements the `HighwayInfo` source for links mode: STRAIGHT chunks map to the existing `Deck` on their axis; END chunks inside the city radius are skipped, and the first chunk outside gets a plaza at the city level (0013 §2). Add `Shape.DIAGONAL` and `Shape.TRANSITION` to `HighwayInfo.Piece`, with the quarter turn taken from the step directions. `xLevel`/`zLevel` must report the level on diagonals too, so buildings keep clear.
3. **Writer.** In `worldgen/city/NetworkWriter` (981 lines; read by range), write the diagonal and transition pieces in code. Reuse the column-by-column ramp writer, which already writes per column. Fill the corner spill in the side neighbors (0015 §3).
4. **Rails.** In `RailwayLayout`, links mode lays rail along the link paths instead of the grid. Diagonal chunks get the zig-zag. Stations hook on `bothEndsCities()`.
5. **Switch on.** Make `networkLayout` LIVE, set `rarecities` to `links` and `railwaysCanEnd` to `false`, and update the config and spec rows and 0015's status. Leave the "legacy default" pin for pinned worlds to the orchestrator.
6. **Verify (one round):** a remote `spotlessApply build`, then the `network.highway` and `network.railway` headless scenarios on `--instance 4` with a fresh `rarecities` world (`tools/remote/README.md` "Real-pack arguments"). Add a layout-level walk test through `LayoutPlanner` with a flat terrain oracle.

## Notes

- The remote build ran `spotlessApply` on the server only, so run `spotlessApply` locally or remotely before your next commit if formatting differs.
- The headless network scenarios were not run: generation does not read the graph yet, so they would only show the legacy network.
