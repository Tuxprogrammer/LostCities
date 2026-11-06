# Handoff: lane/p4b-verify (context limit reached, session 2)

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

## Session 2
- **Railway failure diagnosed and fixed (D-p4v-4, 1d81330f).** I read the region files from the round 1 world. A Roguelike Dungeons tower (ground about Y 100, spawner at -713,50,-30, stairwell and rooms down to Y 31) cut the Y 54 tunnel at x=-45. It was not the city and not an explosion. `LandmarkKind.reachesUnderground()` (ROGUELIKE, LOOTGAMES) is used by `LandmarkZones.undergroundCore` and `RailwayInputs.isUndergroundCore`, and the LinkRails Blocked oracle now treats such a core as Blocked, so D322 drops the whole line. Legacy `underCores` is unchanged; no golden changed. Test: `LinkRailCoresTest`.
  - Possible gap: Roguelike rooms reaching outside the landmark core and margin could still cut a line. Watch for it.
- **D-p4v-5:** the railway scenario counts boundaries an explosion reaches separately and does not fail on them, as the highway walk already does. Test: `LinkScenariosTest`.
- Build green on 1d81330f: 1236 tests, 0 failures. Spotless diff in 03d771a3.
- **Real-pack round 2 (the last round) FAILED.** Run 20261106-145911-i4 (main checkout, `reference/remote/realpack/20261106-145911-i4/console.log`), world p4vrare2, seed 4242.
  - Passed: `city.real`, `city.walk`, `network.highway`. 50 cascade warnings, none attributed to our terrain or population code.
  - `network.railway` failed. The same line -46,-17 -> -40,27 has the same 4 "none | none" boundaries at x=-45, z -4..1, with 0 damaged. So D-p4v-4 did not block this line.
  - Likely cause: `RoguelikeOracle.landmark` gives only a 21 x 21 footprint around the tower. The dungeon's underground levels spread over about 6 chunks (z -4..1), so the line's chunks are outside the core. The prediction may also be off or missing.
  - Next: log `zones.near` and `undergroundCore` along x=-45, z -6..3 for this seed. Then either widen ROGUELIKE's underground reach (the levels' radius) for the rail Blocked test, or have the steering mixin keep dungeon levels off link-line chunks.
  - The D-p4v-4 row and the 0015 bullet describe the intent; they need a correction once this is fixed.

## Not done
- **KI-3 (legacy rail gap under a shallow landmark).** Confirmed by design: `RailwayLayout.underCores` returns NONE for a junction, station or slope under a shallow core (0013 "No room"). This is a different cause from the link failure. A fix would be RAILS_END_HERE buffers beside the dropped chunk. It changes legacy goldens, so it needs a D282-style gate (ProfileOption, `legacyDefault`, TerrainSettings default = legacy) and a decision. README now says the link network leaves such a line out.
- **KI-5 (legacy highway steps and broken track on seeds 1 and 777).** Not investigated. Leads: "213 too high" on route cell(-1,0)-cell(0,0); "1 broken" on RWG2 seed 777 from station 29,-11; D269.
- **KI-2 (ramp slab stripes).** Not started.
