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

The M01 goldens pin `GoldenFixtureTerrain`, a stand-in generator, at three seeds and chunk positions. Their hashes were computed outside Java by an independent re-implementation of the generator (including `java.util.Random`) and of the text form, so they check the format as well as its stability. M04 adds ten `terrain_*` goldens: the terrain phase over the synthetic test city on the `TestTerrain` fixture (a street, three lots, a soft-edge strip, a landmark core with and without a lane, an edge ring and two blended chunks). M05 adds goldens for real city chunks: taken after the terrain phase, and separately after our population phase ([0005](../design/0005-rwg-overlay.md), "Determinism tests").

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
| `assets.pack_resolution` | Assets | In the real pack: every block the assets and the variety pools name resolves to its primary, with no fallback taken; logs the full asset report, so a run lists every unresolved name | no (manual-only) |
| `overlay.installed` | Overlay (M04) | The overworld's chunk provider is the overlay's wrapper, with a pinned snapshot; logs how many chunks were loaded before it was installed (0 on a new world) | yes |
| `overlay.city` | Overlay (M04) | Needs a world pinned with the synthetic test city (`-Dlostcitiesnh.planSource=synthetic`). Generates 25 × 25 chunks around the city at chunk (2048, 2048) and checks the 17 × 17 inside: street chunks paved at the city ground, the write guard dropped RWG's decoration and no log or leaf passed it, landmark cores without a lane left exactly as RWG made them, the lane laid, every glowstone lit after the chunk's light pass (ArchaicFix's Phosphor, P03-8). Prints `[LCNH-HASH] … terrain=<sha> population=<sha>` (every chunk right after the terrain phase; every population job's site right after our population phase): two fresh worlds of the same seed and world type must print the same pair | yes |
| `overlay.budget` | Overlay (M04) | Needs the synthetic city. Generates 65 × 65 chunks in spiral order beside a dry synthetic city, drops the first 512 as warm-up, prints `[LCNH-BUDGET]` timer lines and the verdicts B1 to B5, and fails when B1 to B4 are exceeded (0005 §8) | no (manual-only) |
| `oracle.villagenames` | VillageNames oracles (M05) | Needs VillageNames, as in the real pack. Scans the village grid cells within 6 cells and the monument cells within 20 cells of chunk (−4096, 4096) (`-Dlostcitiesnh.vnScenario.center=<x>,<z>` moves it) and checks the scan loaded no chunk, left `world.rand` untouched, gives the same landmarks to a fresh oracle asked tile by tile in reverse order, and keeps every footprint within the oracle's reach. Generates the 6 nearest predicted villages (every chunk their footprint touches, populated), 6 starts whose biome VillageNames rejects (3 × 3 chunks) and the 8 nearest predicted monuments (7 × 7 chunks), then compares with VillageNames' own `data/Village.dat` and `data/Monument.dat`: each predicted village recorded, valid, with exactly the predicted piece boxes, and built blocks in at least half of its buildings (VillageNames leaves pieces over water and some small decorations unbuilt; each is logged with the blocks around it); no rejected start holding a valid village; every valid village and monument recorded anywhere predicted, unless the start chunk's stored biome explains it (an explained miss, logged). Prints `[LCNH-VN] oracle-hash=` and `actual-hash=`: two fresh worlds of one seed and world type print the same pair | no (manual-only) |

Each feature adds its scenarios to this table.

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

Screenshots for review, such as the alpha gate's set, come from a shot list run on the real GTNH 2.9.0 server and client (D-M05S-1 to D-M05S-6). The list is a JSON file under `tools/remote/shots/` naming the world and each viewpoint, with a caption; `lc-remote.sh shots` takes every shot clean and writes a review page to `reference/remote/shots/<id>/index.html`:

```sh
tools/remote/lc-remote.sh shots tools/remote/shots/synthetic-city.json --slot <slot>
tools/remote/lc-remote.sh shots tools/remote/shots/synthetic-city.json --slot <slot> -- --level-type RWG --world shots-rwg
```

The client runs with the mod's clean capture (`-Dlostcitiesnh.testHooks=true -Dlostcitiesnh.capture=true`, `client.CleanCapture`): no GUI, hand, chat, tutorial toast or pack overlays, and a `[LCNH-CAPTURE] settled` line once the chunks around a viewpoint have arrived and the build queue is empty; the runner waits for it, then screenshots until the picture holds still. The format, the runner's steps and its bounds are in [tools/remote/README.md](../../tools/remote/README.md#shot-lists). `CaptureSettingsTest` and `SettleTrackerTest` cover the capture's settings and its settle rule; the runner itself is proved by the demo set in the M05S report.

## Dev client

`./gradlew runClient --mcJvmArgs=-Dlostcitiesnh.quitAtMainMenu=true` boots the dev client; once the main menu has been open for 60 client ticks, the client logs `[LCNH-CLIENT] main menu reached`, saves a screenshot to `run/client/screenshots/` and closes itself. On the build server it runs under an Xvfb display from our range (`lc_xvfb`), through `bin/lc-build.sh` so the slot lock and build timeout apply.
