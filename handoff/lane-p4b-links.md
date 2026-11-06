# Handoff: lane p4b-links (city link network)

Branch `lane/p4b-links`, based on `gtnh` at `71c54a50` (4a merged). Do not merge `gtnh` into it. Slot `p4b`, real-pack `--instance 4`.

Session 2 (2026-11-06) also began near 160k context, so it landed only step 1 and handed off again; the next session should start fresh.

Session 3 (2026-11-06) landed steps 2 and 3 (highways along the links) and verified them with one remote build; rails (step 4) onward are left.

Session 4 (2026-11-06, "close the gaps") again began at about 161k of the 200k context budget, so it landed only gap 3 below (spill guard, D-p4b-9) and handed off. **Start the next session fresh**; the gaps below come before rails.

Why handed off (session 1): the session began at about 156k of a 200k context budget, so this lane landed the pure core and stopped.

## Design

Read [docs/design/0015-city-link-network.md](../docs/design/0015-city-link-network.md). §1 (graph), §2 (octilinear routing) and §4 (walk test) are landed and accepted; §3 (pieces), §5 (junctions, city entry), §6 (obstacles) and §7 (station hook) are proposals for you to implement. Change them if implementing them shows a better way, and log the change.

## Done

- `layout/link/CityLinkGraph`: nodes from a `Nodes` oracle, 3 nearest within 160 chunks, symmetric `links(cell)`, per-chunk `hits(x, z)` from per-cell LRU indexes. `Link.bothEndsCities()` is the station hook for 4c.
- `layout/link/LinkRaster`: straight lead, diagonal run, straight trail; kinds `END`, `STRAIGHT`, `DIAGONAL`, `TRANSITION`, with in and out step directions per chunk.
- Tests: `CityLinkGraphTest` (3-nearest by brute force, 160 cap, symmetry, order independence, network walk on 1024 × 1024 chunks on two seeds) and `LinkRasterTest`. They pass locally (javac and a reflective runner); see the report for the remote build.
- `networkLayout` is declared (`legacy` | `links`, default `legacy`, **DEFERRED**). `rarecities` is not changed yet, and neither is `railwaysCanEnd` (D-p4b-1).
- Docs: 0015, its design index row, the `docs/config.md` row, the `06-highways-railways` spec row, and decision-log rows D-p4b-1 to D-p4b-3.

## Left, in order

1. ~~Wire the oracle.~~ **Done (session 2, D-p4b-4):** `NetworkSettings.links()`/`withLinks`, read from `networkLayout` in `CityProfiles`; `Networks.links()` holds the graph (nodes via `centerYieldsCity`; `characteristics` confirmed not to read `infrastructure`). Test `layout/NetworksLinkGraphTest` (real planner, flat terrain and sea). The planner-level network-walk test for "Done means" still has to be written once generation reads the graph: walk `planner.networks().highway(x, z)` chunks back to `links().hits(x, z)`.
2. ~~Link highway view.~~ **Done (session 3, D-p4b-5 to D-p4b-7):** `layout/link/LinkHighways` answers `Networks.highway` in links mode; `HighwayInfo.Piece.link`/`plaza`, shapes `DIAGONAL`/`TRANSITION`, `levelAt`, spill reporting. `centerYieldsCity` no longer computes full characteristics (recursion found by the walk test).
3. ~~Writer.~~ **Done (session 3, D-p4b-8):** `layout/link/LinkGeometry` + `NetworkWriter.linkColumns` + `PartWriter.placeColumn`. Tests: `LinkHighwaysTest` (shapes, profiles, geometry, spill) and `LinkHighwayWalkTest` (planner walk, flat two seeds and terraces). Not built: staircase fallback at bridges/tunnels, bends/T where links meet at one level (written as overlapping pieces), 0015 §6 landmark drops.
3a. **Close the highway gaps** (session-4 brief; done means per item: code + test):
   - ~~Spill guard.~~ **Done (session 4, D-p4b-9):** `NetworkWriter.footprintBottom`/`guardBand` read `HighwayInfo.spill`/`spillLow`/`spillHigh`; test `NetworkWriterTest.spillFromALinkDiagonalIsInTheFootprintOrGuardBand`.
   - **Junctions:** where links meet at one level other than straight × straight (`LinkHighways.pieces` merge, D-p4b-7), write one bend or T/X piece, no doubled or clipped decks; shape tests in `LinkHighwaysTest`.
   - **Bridge/tunnel fallback** (owner decision 8: 45° pieces, staircase only as fallback): where a diagonal or transition is a bridge or tunnel, replace it with a corner staircase of straight + bend parts so the deck stays continuous; test in `LinkHighwayWalkTest` with a `FakeTerrain` that has a river and a hill.
   - **Landmark drops** (0015 §6): drop a link crossing a core or margin (0013 §1) when the node keeps another choice, as the legacy network avoids landmarks; test.
4. **Rails.** In `RailwayLayout`, links mode lays rail along the link paths instead of the grid. Diagonal chunks get the zig-zag. Stations hook on `bothEndsCities()`.
5. **Switch on.** Make `networkLayout` LIVE, set `rarecities` to `links` and `railwaysCanEnd` to `false`, and update the config and spec rows and 0015's status. Leave the "legacy default" pin for pinned worlds to the orchestrator.
6. **Verify (one round):** a remote `spotlessApply build`, then the `network.highway` and `network.railway` headless scenarios on `--instance 4` with a fresh `rarecities` world (`tools/remote/README.md` "Real-pack arguments"). The layout-level walk test exists (`LinkHighwayWalkTest`); extend it to rails.

## Notes

- The remote build ran `spotlessApply` on the server only, so run `spotlessApply` locally or remotely before your next commit if formatting differs.
- The headless network scenarios were not run: generation does not read the graph yet, so they would only show the legacy network.
