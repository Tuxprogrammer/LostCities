# Handoff: lane p4b-links (city link network)

Branch `lane/p4b-links`, based on `gtnh` at `71c54a50` (4a merged). Do not merge `gtnh` into it. Slot `p4b`, real-pack `--instance 4`.

Session 2 (2026-11-06) also began near 160k context, so it landed only step 1 and handed off again; the next session should start fresh.

Session 3 (2026-11-06) landed steps 2 and 3 (highways along the links) and verified them with one remote build; rails (step 4) onward are left.

Session 4 (2026-11-06, "close the gaps") again began at about 161k of the 200k context budget, so it landed only gap 3 below (spill guard, D-p4b-9) and handed off. **Start the next session fresh**; the gaps below come before rails.

Session 5 (2026-11-06) closed the remaining 3a gaps (junctions D-p4b-10, staircase D-p4b-11, landmark drops D-p4b-12), verified with one green remote `spotlessApply build` (1184 tests, 0 failures). **Next: step 4 (rails), then 5 and 6.** Start fresh.

Session 6 (2026-11-06) landed step 4 (rails, D-p4b-13/14), the ramp overlap fix (D-p4b-15) and most of step 5 (networkLayout LIVE, rarecities on links + railwaysCanEnd=false, D-p4b-16, docs), plus `LinkNetworkWalkTest` (rail reciprocity and stations, center-to-center lines, highway chunks on written links, ramp overlap, coded geometry). One green remote `spotlessApply build` (1189 tests, 0 failures) at `01066338`. Began at about 160k context, so handed off. **Left: items 7 to 10 below.** Start fresh.

Why handed off (session 1): the session began at about 156k of a 200k context budget, so this lane landed the pure core and stopped.

## Design

Read [docs/design/0015-city-link-network.md](../docs/design/0015-city-link-network.md). §1 to §6 are landed and accepted, except rails in §3; §7 (station hook) is a proposal for you to implement. Change them if implementing them shows a better way, and log the change.

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
   - ~~Junctions.~~ **Done (session 5, D-p4b-10):** `LinkHighways.merge` joins flat pieces at one level (`Piece.sides`, `Piece.linkJunction`, shape `LINK_JUNCTION` in `LinkGeometry`).
   - ~~Bridge/tunnel fallback.~~ **Done (session 5, D-p4b-11):** per-link staircase in `LinkHighways.compute` via `Ends.bridgeOrTunnel` (answered in `Networks`); walk test `linksTakeTheStaircaseOverARiverAndThroughAHill`.
   - ~~Landmark drops.~~ **Done (session 5, D-p4b-12):** `CityLinkGraph.Nodes.blocked` (landmark zones in `Networks`); test `CityLinkGraphTest.linksPassOverLandmarksWhenTheNodeHasAnotherChoice`.
4. **Rails (next).** Scope: `RailwayLayout` only, plus its tests. In links mode lay rail along `CityLinkGraph` link paths (reuse `LinkRaster` chunks and the per-link levels idea from `LinkHighways`) instead of the grid; diagonal chunks get the zig-zag; rails keep the 0013 §5 landmark dips (do not apply `Nodes.blocked` to rails); stations hook on `Link.bothEndsCities()` (0015 §7). Extend `LinkHighwayWalkTest`'s approach to a rail walk. Legacy goldens must stay byte-identical. Do not switch the layout on in this step.
5. **Switch on.** Make `networkLayout` LIVE, set `rarecities` to `links` and `railwaysCanEnd` to `false`, and update the config and spec rows and 0015's status. Leave the "legacy default" pin for pinned worlds to the orchestrator.
6. **Verify (one round):** a remote `spotlessApply build`, then the `network.highway` and `network.railway` headless scenarios on `--instance 4` with a fresh `rarecities` world (`tools/remote/README.md` "Real-pack arguments"). The layout-level walk test exists (`LinkHighwayWalkTest`); extend it to rails.

7. **Links golden.** Add a `networkLayout=links` golden to `LayoutGoldenTest` (legacy goldens untouched; they stayed green in session 6).
8. **Scenarios.** `worldgen/overlay/NetworkScenarios` `network.highway`/`network.railway` are legacy-specific (HighwayWalk reads `HighwayPlanner.ownedRoutes`, RailwayRide looks for grid station spots and a THREE_SPLIT). Add links branches (walk `Networks.links()` paths; ride from one station END chunk to the other via `RailChunk.openings`), then one realpack round on `--instance 4` with a fresh `rarecities` world: `city.real`, `city.walk`, `network.highway`, `network.railway`, no cascade warnings.
9. **README known issue** "highway steps and broken track on seeds 1 and 777": not re-checked yet. Those seeds use the legacy network; re-check after the realpack run.
10. **Integrator:** add `legacyDefault("networkLayout", "legacy")` (D282 mechanism, on `gtnh` only) when merging this lane. Then delete this file.

## Notes

- Known limits (D-p4b-13): at a T the vanilla center rail joins two arms; a cart on the third arm meets it from the side. A line through another city's center makes a coded junction there, no station.

- Open gaps from session 5: (a) a ramp sharing a chunk with a flat piece at a level inside the ramp's range still overlaps it (only same-level flat pieces merge); (b) the staircase is decided from the 45° profile's levels, so a staircase link whose re-planned levels differ could in rare cases still meet a bridge on a straight chunk, which is fine (straight parts bridge as legacy); (c) none of this is seen in-game yet: generation only reads the graph with `networkLayout=links`, still DEFERRED.

- Bring the server's spotless diff back after each remote build (`ssh host "cd <root>/wt/p4b && git diff"`, `git apply`), as session 5 did.
- The headless network scenarios were not run: generation does not read the graph yet, so they would only show the legacy network.
