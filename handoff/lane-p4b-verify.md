# Handoff: lane/p4b-verify (context limit reached, session 1)

Branch `lane/p4b-verify`, from `gtnh` at 32847b4f. Commits: 3258e8ea, 27ecae29, 298760fa (and this handoff).

## Done
- **Item 1, links golden.** `LayoutGoldenTest.rareCitiesLinkNetwork` and `golden/layout/rarecities_link_network.txt`. A remote `build -PupdateGoldens` left every legacy golden byte-identical; only the comment headers of the `.sha256` files were rewritten, and those were not committed.
- **Item 3a, T junctions (D-p4v-1).** The T center is vanilla's unpowered curve, with a lever on the free side. Code: `LinkRailGeometry.shape`/`lever`, `NetworkWriter.linkRail`. Tested by `LinkNetworkWalkTest.aTeeCenterIsAVanillaSwitchWithALever`. Crossings stay E-W straight; this is a known limit.
- **Item 3b, lines through another city (D-p4v-2).** `LinkRails.route` splits a line at third-city centers. Tested by `noLinePassesThroughAnotherCityCenterAndEveryCenterIsAStation`, which printed: 256 centers, all of them stations, 0 splits on these seeds, 290 T's, 55 crossings.
- **Item 2, scenario links branches.** In `worldgen/overlay/LinkScenarios.java`, wired into `NetworkScenarios`.
- **Item 5, build.** Remote build green: 1231 tests, 0 failures. The spotless diff was applied (27ecae29).
- **Item 6, docs.** 0015 has a new "Junctions and through-lines" section. Decision rows D-p4v-1..3 are added.

## Real-pack round 1
Instance 4, fresh rarecities, RWG2_CONTINENTS, seed 4242. Run 20261106-144732-i4, logs in `reference/remote/realpack/20261106-144732-i4/console.log`.
- PASS: `city.real`, `city.walk`, `network.highway`. The highway walk covered link -89,-44 -> -46,-17: 480 steps with 0 faults.
- **FAIL: `network.railway`.** On line -46,-17 -> -40,27 (51 chunks), 4 boundaries are broken, all "none | none", meaning no rail on either side. They sit at x=-45, z -4..1 (-45,-4|-3, -3|-2, -2|-1, 0|1). Openings are all fine, both stations are present, and the 1 T has its lever.
  - Suspects: city chunks near spawn overwrite the link tunnel (city.real found a city there), or the N-S boundary scan in `LinkScenarios.railsAcross` is wrong for this case. -1|0 passed, which argues against a scan bug.
  - Next: compare the city footprint against x=-45, z -4..1, and check `NetworkWriter.linkRail` against city/building basement writers.
- **Cascades.** 50 "Cascading world generation" warnings, none from our code. Our frames appear only as the scenario's own chunk loader at the stack root (`PathGenerator.tick`).

## Not done
- **Item 2, railway pass.** Diagnose the 4 broken boundaries above, then run round 2. Only one round is left.
- **Item 4, README known issue** "highway steps / broken track on seeds 1 and 777" (legacy, README line 81). Not investigated. Earlier logs in `reference/remote/realpack/*/console.log` show "213 too high" on route cell(-1,0)-cell(0,0) and "1 broken" on RWG2 seed 777 from station 29,-11. See D269.
