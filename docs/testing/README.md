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

The M01 goldens pin `GoldenFixtureTerrain`, a stand-in generator, at three seeds and chunk positions. Their hashes were computed outside Java by an independent re-implementation of the generator (including `java.util.Random`) and of the text form, so they check the format as well as its stability. M03 to M05 add goldens for real city chunks: taken after the terrain phase, and separately after our population phase ([0005](../design/0005-rwg-overlay.md), "Determinism tests").

## Asset validation and palette goldens

`AssetValidationTest` loads every bundled asset with the game's own loader over two registries, vanilla 1.7.10 alone (`src/test/resources/vanilla-1.7.10-blocks.txt`) and the full block-name map, and checks the bundled index against the tree. `PaletteGoldenTest` hashes `CompiledPalette.describe()` for a few representative buildings into `src/test/resources/golden/palette_<case>.sha256`, with the same `-PupdateGoldens` and `build/golden-actual/` conventions as the golden chunks. In the real pack, run the resolution report with:

```sh
tools/remote/lc-remote.sh realpack --slot <slot> -- --fresh --world <name> --level-type RWG \
    --jvm '-Dlostcitiesnh.testHooks=true -Dlostcitiesnh.selfCheck=true' \
    --cmds 'lostcities test assets.pack_resolution; until:Scenarios: ; wait:3'
```

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

Each feature adds its scenarios to this table.

## Dev client

`./gradlew runClient --mcJvmArgs=-Dlostcitiesnh.quitAtMainMenu=true` boots the dev client; once the main menu has been open for 60 client ticks, the client logs `[LCNH-CLIENT] main menu reached`, saves a screenshot to `run/client/screenshots/` and closes itself. On the build server it runs under an Xvfb display from our range (`lc_xvfb`), through `bin/lc-build.sh` so the slot lock and build timeout apply.
