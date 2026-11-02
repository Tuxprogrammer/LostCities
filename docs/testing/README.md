# Testing

How to run each test layer of [CONTRIBUTING § Testing](../../CONTRIBUTING.md#testing), and the list of in-world scenarios. Every build and Minecraft run happens on the build server through [tools/remote/](../../tools/remote/README.md) (decision log D4); the commands below are what runs there.

## Unit and static-check tests

```sh
./gradlew spotlessApply build      # format, compile, every unit and static-check test
./gradlew test                     # tests only
```

The static checks live in `src/test/java/com/tuxprogrammer/lostcities/statics/`. Each one has a fixture that breaks it, and a test that the check reports the fixture; the fixtures are classes under `statics/fixtures/`, synthetic classes built in the test (`Java8ApiTest`), or text under `src/test/resources/portedgate/` and `assetvalidation/` that is never compiled.

## Golden chunks

`GoldenChunks.assertMatches(case, buffer)` compares a chunk buffer's hash with `src/test/resources/golden/<case>.sha256`:

- The hash is the SHA-256 of `ChunkBufferDigest`'s version-1 text form: a palette in first-appearance order and one run-length line per non-air layer. The format is documented on `ChunkBufferDigest`.
- On a mismatch the test fails with both hashes and writes the actual text form to `build/golden-actual/<case>.txt`, so the change can be read.
- `./gradlew test -PupdateGoldens` rewrites every golden it checks instead of failing. A golden changes only in a commit that says why; keep the comment lines at the top of the file current.

The M01 goldens pin `GoldenFixtureTerrain`, a stand-in generator, at three seeds and chunk positions. Their hashes were computed outside Java by an independent re-implementation of the generator (including `java.util.Random`) and of the text form, so they check the format as well as its stability. M04 adds ten `terrain_*` goldens: the terrain phase over the synthetic test city on the `TestTerrain` fixture (a street, three lots, a soft-edge strip, a landmark core with and without a lane, an edge ring and two blended chunks). M05 adds `CityGoldenTest`: real city chunks of the `CityFixtures` world (the bundled assets resolved for the full pack, the layout over `TestTerrain`, seed 4242) after the terrain phase (`city_building`, `city_primary_street`, `city_park`, `city_ruin`), and the population jobs of the building and the street as text goldens (`city_building_jobs`, `city_primary_street_jobs`, through `GoldenChunks.assertTextMatches`), the second hash of [0005](../design/0005-rwg-overlay.md), "Determinism tests".

## Asset validation and palette goldens

`AssetValidationTest` loads every bundled asset with the game's own loader over two registries, vanilla 1.7.10 alone (`src/test/resources/vanilla-1.7.10-blocks.txt`) and the full block-name map, and checks the bundled index against the tree. `PaletteGoldenTest` hashes `CompiledPalette.describe()` for a few representative buildings into `src/test/resources/golden/palette_<case>.sha256`, with the same `-PupdateGoldens` and `build/golden-actual/` conventions as the golden chunks. In the real pack, run the resolution report with:

```sh
tools/remote/lc-remote.sh realpack --slot <slot> -- --fresh --world <name> --level-type RWG \
    --jvm '-Dlostcitiesnh.testHooks=true -Dlostcitiesnh.selfCheck=true' \
    --cmds 'lostcities test assets.pack_resolution; until:Scenarios: ; wait:3'
```

## Layout goldens

The city layout (`layout`, M03) has its own goldens, one text file per case in `src/test/resources/golden/layout/` (decision log D47), written by `LayoutGoldenTest` through `LayoutGoldens`:

- each file holds the case's description, a legend and three maps of the case's region, north at the top: what each chunk is (building, multibuilding, road class, park, plaza, core, lane, detour), its landmark zone, and its building's floors;
- a `details-sha256:` line pins every chunk's full decision line (`LayoutText.describe`: building, parts, street type, draws, zone, lane, neighbor answers);
- on a mismatch the test writes the actual file and the details to `build/golden-actual/layout/`; `-PupdateGoldens` rewrites the files, as for chunk goldens. Review the map diff before committing a rewritten golden, and say why in the commit.

The cases are 0006's: a village inside a city, one at its edge, one three chunks out, a severed primary road, a meteorite basin; plus a plain city and a village under `AVOID`. They run against fake structure oracles, fake assets and fake terrain (`LayoutFixtures`), so they need no Minecraft.

## Layout parity

`src/test/resources/parity/` holds values recorded from upstream Lost Cities 1.20 and from Minecraft 1.20.1 by [tools/upstream-parity/](../../tools/upstream-parity/README.md) (decision log D45). The street planner, bridge hashes, `QualityRandom`, simplex noise, the city rarity map and the dimension hash are compared with them bit for bit. Upstream's planner above the street field (`City`, `BuildingInfo`, `MultiChunk`) cannot run outside a 1.20 server, so it is checked against the spec's numbers, the `doesNotReproduce` regressions and the layout goldens. Recording again needs JDK 17 or newer and internet access on the build server; see the tool's README.

## Dev server and the dev self-check

`./gradlew runServer` boots the dev server with GTNHLib, the mod's one hard dependency. In the development environment the dev self-check runs in `postInit` and logs `Dev self-check passed`; a failure stops the server with every problem listed. `-Dlostcitiesnh.selfCheck=true` or `false` forces it on or off ([config.md](../config.md#development-and-testing-switches)).

## Scenarios

`/lostcities test <scenario|prefix|all|list>` (alias `/lostcitiesnh scenario …`), operator level 2. It runs only in the development environment, or in a release jar started with `-Dlostcitiesnh.testHooks=true`.

- `list` names every scenario; `all` runs every scenario that is not manual-only; a prefix of whole words (`harness`) runs that group; an exact name runs that scenario, manual-only ones included.
- Scenarios run one at a time on the server thread, driven by server ticks. Each gets its own 8 × 8-chunk arena far from spawn (chunk X 2048 + 16 × its position in the run, chunk Z 2048), so on a fresh world it works on untouched terrain.
- Results go to the log and to the sender's chat:

  ```
  [LCNH-SCENARIO] PASS harness.trivial 969 ok
  [LCNH-SCENARIO] FAIL harness.must_fail 0 deliberate failure: ...
  [LCNH-SCENARIO] Scenarios: 2 passed, 0 failed
  [LCNH-SCENARIO] ERROR no scenario matches 'nosuch'
  ```

  The summary line comes only after every scenario of the run has finished. Automation treats `failed` above 0, or an `ERROR` line, as a failure.

Without console input, JVM properties start a run once the server is up and can stop the server after its summary:

```sh
./gradlew runServer --mcJvmArgs=-Dlostcitiesnh.scenarios=all --mcJvmArgs=-Dlostcitiesnh.stopAfterScenarios=true
tools/remote/lc-remote.sh realpack --slot <slot> -- --fresh --world <name> --level-type RWG \
    --jvm '-Dlostcitiesnh.testHooks=true -Dlostcitiesnh.scenarios=all -Dlostcitiesnh.stopAfterScenarios=true'
```

Run scenarios on new worlds (`--fresh`, or a new `--world`), on both generators: `--level-type RWG` and `--level-type RWG2_CONTINENTS` (decision log D33).

### Scenario list

| Scenario | Area | Checks | In `all` |
|---|---|---|---|
| `harness.trivial` | Harness | The overworld is loaded and ticking: setup, 20 ticks, verify | yes |
| `harness.fresh_chunk` | Harness | The arena's corner chunk is generated and loaded on demand, outside any generation step, and holds terrain | yes |
| `harness.must_fail` | Harness | Fails on purpose, to show that FAIL lines and the summary count reach the log | no (manual-only) |
| `assets.loaded` | Assets | The asset set loaded at `postInit` holds every bundled asset (at least 324) with no error, every code-named block resolved, and a biome descriptor for the arena, with an RWG realistic biome wherever an RWG chunk manager runs the world | yes |
| `assets.pack_resolution` | Assets | In the real pack: every block the assets and the variety pools name resolves to its primary, with no fallback taken, and every palette-variety option's blocks are registered; logs the full asset report, so a run lists every unresolved name | no (manual-only) |
| `assets.catalog` | Assets (M11) | Writes every bundled part under every style's palettes, with the palette variety drawn for 48 seeds per style (a building's parts under that building's palette, as floor parts and as plain parts), and fails on a character the palette leaves undefined or a block the live registry lacks (`worldgen.city.AssetCatalogCheck`; `AssetCatalogCheckTest` runs it over the test registries). Prints `[LCNH-ASSETS] catalog:` with the counts | yes |
| `overlay.installed` | Overlay (M04) | The overworld's chunk provider is the overlay's wrapper, with a pinned snapshot; logs how many chunks were loaded before it was installed (0 on a new world) | yes |
| `overlay.city` | Overlay (M04) | Needs a world pinned with the synthetic test city (`-Dlostcitiesnh.planSource=synthetic`). Generates 25 × 25 chunks around the city at chunk (2048, 2048) and checks the 17 × 17 inside: street chunks paved at the city ground, the write guard dropped RWG's decoration and no log or leaf passed it, landmark cores without a lane left exactly as RWG made them, the lane laid, every glowstone lit after the chunk's light pass (ArchaicFix's Phosphor, P03-8). Prints `[LCNH-HASH] … terrain=<sha> population=<sha>` (every chunk right after the terrain phase; every population job's site right after our population phase): two fresh worlds of the same seed and world type must print the same pair | yes |
| `overlay.budget` | Overlay (M04) | Needs the synthetic city. Generates 65 × 65 chunks in spiral order beside a dry synthetic city, drops the first 512 as warm-up, prints `[LCNH-BUDGET]` timer lines and the verdicts B1 to B5, and fails when B1 to B4 are exceeded (0005 §8) | no (manual-only) |
| `city.real` | Cities (M05) | Needs a world pinned with the layout source (the default). Prints `[LCNH-CITY] spawn=… nearestCityChunkFromSpawn=… nearestCityChunkFromOrigin=…` (blocks to the center of the nearest city chunk, D53's measure); generates 21 × 21 chunks around the city center nearest the spawn whose region lies outside the spawn area, and checks the 17 × 17 inside: city chunks and buildings exist, 9 in 10 street chunks are paved at the city ground, no log or leaf passed the guard; prints the buildings it saw and `[LCNH-HASH] … terrain=<sha> population=<sha>`, which two fresh worlds of one seed and world type must share (D117) | yes |
| `city.walk` | Cities (M05) | Walkability over the buildings of the spawn's city and of that city: floors with a floor, outer walls closed but for doorways, stairs or ladders between floors, every ground-floor street door opening onto open space; ruined and explosion-damaged buildings exempt. Prints `[LCNH-WALK]` lines with the pass counts and a few failing places (D116) | yes |
| `city.sewing` | Cities (M05) | Needs VillageNames. Finds the village lane nearest chunk (−4096, 4096) (D90's villages), generates 21 × 21 chunks around it and checks the 17 × 17 inside: core chunks written only for a lane's path, margin chunks present and none of them city, lane columns paved in VillageNames' path blocks; on RWG2 also the nearest ocean monument's reserved lot (cores untouched). Prints `[LCNH-SEWING]` lines with the counts and the `find` results for `village`, `softedge` and `reserved` there | no (manual-only) |
| `city.budget` | Cities (M05) | D81's budget method with real content: 65 × 65 chunks from 20 chunks beside the spawn's city, the first 512 dropped; fails when B1 to B5 are exceeded | no (manual-only) |
| `oracle.villagenames` | VillageNames oracles (M05) | Needs VillageNames, as in the real pack. Scans the village grid cells within 6 cells and the monument cells within 20 cells of chunk (−4096, 4096) (`-Dlostcitiesnh.vnScenario.center=<x>,<z>` moves it) and checks the scan loaded no chunk, left `world.rand` untouched, gives the same landmarks to a fresh oracle asked tile by tile in reverse order, and keeps every footprint within the oracle's reach. Generates the 6 nearest predicted villages (every chunk their footprint touches, populated), 6 starts whose biome VillageNames rejects (3 × 3 chunks) and the 8 nearest predicted monuments (7 × 7 chunks), then compares with VillageNames' own `data/Village.dat` and `data/Monument.dat`: each predicted village recorded, valid, with exactly the predicted piece boxes, and built blocks in at least half of its buildings (VillageNames leaves pieces over water and some small decorations unbuilt; each is logged with the blocks around it); no rejected start holding a valid village; every valid village and monument recorded anywhere predicted, unless the start chunk's stored biome explains it (an explained miss, logged). Prints `[LCNH-VN] oracle-hash=` and `actual-hash=`: two fresh worlds of one seed and world type print the same pair | no (manual-only) |
| `profile.effects` | Profiles (M09) | On any pinned profile: the layout reads the pinned values; plans 48 × 48 chunks around the spawn and prints `[LCNH-PROFILE] profile=… cityChunks=… buildings=… maxFloors=… ruined=… damagedChunks=… waterLevel=… styles=…`, so two profiles on one seed can be compared; fails when explosion chances of 0 still damage a chunk, a `ruinChance` of 0 still ruins a building, or a building passes `buildingMaxFloors` | yes |
| `profile.spawn` | Profiles (M09) | Prints `[LCNH-SPAWN]`; without spawn options, no spawn was chosen; with them, the world spawn is the chosen one, and for the building options it lies in a building chunk with standing room. Pin spawn options for a run with `-Dlostcitiesnh.profileOverrides=forceSpawnInBuilding=true` ([config.md](../config.md#development-and-testing-switches)) | yes |

Each feature adds its scenarios to this table.

To run the city scenarios with another profile in the real pack, add `--jvm '-Dlostcitiesnh.testHooks=true -Dlostcitiesnh.dimensionProfiles=0=<profile>'` to a fresh world's run; `city.real`'s hashes then compare two fresh worlds of that profile.

### Finding things to look at

`/lostcities find <kind> [radius]` (test hooks only, operator level 2) prints the nearest `center` (city center), `street` (a primary street with at least two chunks of open street, no park or fountain, at its level ahead; the view looks along them from head height), `interior` (a building of two floors or more with a street door), `edge` (a city chunk beside RWG land), `rise` (a city street beside non-city land at least 12 blocks above it: where the rise cap stops the city before high terrain; the view looks from 30 blocks back into the city, 44 above its ground), `softedge` (a chunk of a landmark's margin between a city chunk and the core, with its slope and soft-edge strip), `reserved` (a landmark core or its margin) or `village` (a village's gate, lane or link chunk) from the sender, within `radius` chunks (48 by default, at most 256). The log line is machine-readable:

```
[LCNH-FIND] kind=interior block=-24,71,-8 chunk=-2,-1 view=-23.5,72.0,-7.5,0,0 label=building lostcities:building2, 2 floors, door ZMIN
[LCNH-FIND] kind=village none within 48 chunks
```

`view=X,Y,Z,YAW,PITCH` feeds `/lostcities view <player> X Y Z YAW PITCH`. `softedge`, `reserved` and `village` find something only where a structure oracle knows a landmark: VillageNames villages on both world types, and ocean monuments on RWG2 (D107, D115).

`/lostcities find <softedge|reserved|village> <radius> best` ranks every candidate of those kinds within the radius instead of taking the nearest (`worldgen.city.LandmarkViews`, D130). Candidates come from the structure oracle's landmarks, so a radius of 200 chunks takes a few seconds, not a scan of every chunk. Lower scores win, ties go to the nearer: the relief of the ground within two chunks of the target (city ground in city chunks, the oracle height elsewhere), plus the gap between the city's ground and the village's (a gate whose margin does not open onto a city adds 40), and for a soft edge a penalty for a step under 3 or over 10 blocks. The views stand beside what they show, 26 blocks above the highest ground or roof on the line of sight (16 for a soft edge), and look down at it: a village gate from 40 blocks beside its lane (the city on one half of the frame, the village on the other), a reserved lot (the core of the village whose gate ranks best, since a flat core can still be a forest of tall trees) from 44 blocks inside the city across its margin, a soft edge from 26 blocks along the margin and 18 blocks into the city from its middle. The label names the landmark and the score's parts:

```
[LCNH-FIND] kind=village block=952,71,-1368 chunk=59,-86 view=912.5,97.0,-1375.5,281,33 label=gate of village@53,-86 facing NORTH, in the city, relief 6, ground 71 to the village's 72
```

### City runs on the real pack

```sh
tools/remote/lc-remote.sh realpack --slot <slot> -- --fresh --world <name> --level-type RWG2_CONTINENTS --seed 4242 \
    --set-config 'archaicfix.cfg:logCascadingWorldgen=true' --set-config 'archaicfix.cfg:logCascadingWorldgenStacktrace=true' \
    --jvm '-Dlostcitiesnh.testHooks=true' \
    --cmds 'lostcities test city.real; until:\[LCNH-SCENARIO\] Scenarios:; lostcities test city.walk; until:\[LCNH-SCENARIO\] Scenarios:; lostcities test city.sewing; until:\[LCNH-SCENARIO\] Scenarios:; lostcities find interior; lostcities timers reset; lostcities test city.budget; until:\[LCNH-SCENARIO\] Scenarios:; wait:5'
```

Repeat with `--level-type RWG`; two such runs per world type are two fresh worlds whose `[LCNH-HASH]` lines must match. `city.sewing` runs before the budget as part of its warm-up: it builds about a hundred VillageNames village starts, so the JIT has compiled VillageNames' start constructor before the budget region meets a village, as on a server that has generated land for a while (the first few starts of a JVM's life cost 14 to 140 ms each, warm ones 3 to 9 ms; D128). Each `until:` waits for a line written after the command before it ([tools/remote/README.md](../../tools/remote/README.md#real-pack-arguments-runsh)).

The `[LCNH-BUDGET]` lines give the `provide`, `terrain`, `populate` and `population` series by chunk class, then `oracle` (the layout's height samples), `structures` (every structure-oracle call, ground estimates included, and the village starts the prefetcher builds) and `prefetch` (the plan prefetcher's work per server tick, D125); then the eight slowest invocations of a terrain or population phase or a prefetch tick, each with the height samples and structure-oracle time it took (`slow terrain city chunk=4,36 6.654 ms: 7 height samples 4.271 ms, structures 0.066 ms`); then the prefetcher's counts (plans it completed, values it fetched, the time its abandoned attempts cost, its slowest tick broken down) and the plan cache's misses; then B1 to B5. B3 counts the prefetcher's work as overlay work, and B4 takes the worst p99.9 of the terrain phase, the population phase and the prefetch tick, leaving out (and counting) the invocations a garbage-collection pause fell into (D127). A chunk's one sky light pass counts as RWG's `provide` when the overlay deferred RWG's own pass, and as the terrain phase's otherwise (D122). `-Dlostcitiesnh.prefetch=false` turns the prefetcher off for a comparison run; what is generated is the same.

### Overlay runs on the real pack

The overlay scenarios run on the real GTNH 2.9.0 server with the release jar, on both world types, with ArchaicFix's cascade warning switched on for the run (D33, D82):

```sh
tools/remote/lc-remote.sh realpack --slot <slot> -- --fresh --world <name> --level-type RWG2_CONTINENTS --seed 4242 \
    --set-config 'archaicfix.cfg:logCascadingWorldgen=true' \
    --jvm '-Dlostcitiesnh.testHooks=true -Dlostcitiesnh.planSource=synthetic' \
    --cmds 'lostcities test overlay; until:\[LCNH-SCENARIO\] Scenarios:; lostcities timers; lostcities timers reset; lostcities test overlay.budget; until:\[LCNH-BUDGET\] worldType; wait:5'
```

Repeat with `--level-type RWG`, and run each a second time with `--fresh` to compare the `[LCNH-HASH]` lines. Cascade warnings are attributed by adding `--set-config 'archaicfix.cfg:logCascadingWorldgenStacktrace=true'`: a warning caused by the overlay would show `worldgen.terrain`, `worldgen.overlay.PopulationPhase` or the guard as the frame that loads the chunk; `OverlayChunkProvider.populate` only passes RWG's own population through.

### VillageNames oracle runs on the real pack

The oracle scenario runs on the real GTNH 2.9.0 server with the release jar, on fresh worlds of both types, twice per type to compare the hashes (D33):

```sh
tools/remote/lc-remote.sh realpack --slot <slot> -- --fresh --world <name> --level-type RWG2_CONTINENTS --seed 4242 \
    --jvm '-Dlostcitiesnh.testHooks=true' \
    --cmds 'lostcities test oracle.villagenames; until:\[LCNH-SCENARIO\] Scenarios:; wait:3'
```

Repeat with `--level-type RWG`. The `[LCNH-VN]` lines give, per world, the scan's verdicts, each generated village (biome, pieces, paths, gates, ground estimate, match against the saved start, buildings with built blocks), each rejected start, each monument, and the counts of predicted against recorded structures. Classic RWG has no oceans, so it predicts and records no monument.

### Review screenshots on the real pack

Screenshots for review, such as the alpha gate's set, come from a shot list run on the real GTNH 2.9.0 server and client (D93 to D98). The list is a JSON file under `tools/remote/shots/` naming the world and each viewpoint, with a caption; `lc-remote.sh shots` takes every shot clean and writes a review page to `reference/remote/shots/<id>/index.html`:

```sh
tools/remote/lc-remote.sh shots tools/remote/shots/synthetic-city.json --slot <slot>
tools/remote/lc-remote.sh shots tools/remote/shots/synthetic-city.json --slot <slot> -- --level-type RWG --world shots-rwg
```

The alpha gate's set of real cities is two lists, one per world type, whose every shot but the RWG2 monument takes its position and view from `/lostcities find` (D124): city from above, street level, a building interior, the city's edge, a village's gate, a reserved lot and a soft edge. The village shots, and on classic RWG the reserved lot and the soft edge too, use `find … best` within 48 (RWG2) or 200 chunks (classic RWG), since classic RWG's nearest village on seed 4242 sits on a mountainside (D130); the RWG2 soft edge stays the nearest, the bank between a street and a village's pond.

```sh
tools/remote/lc-remote.sh shots tools/remote/shots/alpha-rwg2.json --slot <slot>
tools/remote/lc-remote.sh shots tools/remote/shots/alpha-rwg.json --slot <slot>
```

The client runs with the mod's clean capture (`-Dlostcitiesnh.testHooks=true -Dlostcitiesnh.capture=true`, `client.CleanCapture`): no GUI, hand, chat, tutorial toast or pack overlays, and a `[LCNH-CAPTURE] settled` line once the chunks around a viewpoint have arrived and the build queue is empty; the runner waits for it, then screenshots until the picture holds still. The format, the runner's steps and its bounds are in [tools/remote/README.md](../../tools/remote/README.md#shot-lists). `CaptureSettingsTest` and `SettleTrackerTest` cover the capture's settings and its settle rule; the runner itself is proved by the demo set in the M05S report.

## Dev client

`./gradlew runClient --mcJvmArgs=-Dlostcitiesnh.quitAtMainMenu=true` boots the dev client; once the main menu has been open for 60 client ticks, the client logs `[LCNH-CLIENT] main menu reached`, saves a screenshot to `run/client/screenshots/` and closes itself. On the build server it runs under an Xvfb display from our range (`lc_xvfb`), through `bin/lc-build.sh` so the slot lock and build timeout apply.
