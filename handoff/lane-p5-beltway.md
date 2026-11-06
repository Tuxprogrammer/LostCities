# Handoff: lane/p5-beltway (phase 5a+5b, design record 0017)

## State

- Commits on `lane/p5-beltway`:
  - `e56160b0`: the implementation and `CityHighwayLayoutTest`.
  - `a8667075`: the 0017 docs, the config and new-spec rows, decisions D-p5-1 to D-p5-8, and two fixes.
- The two fixes in `a8667075`:
  - A cut-through's end pieces now run straight out to the link's plaza, because `Piece.sides` needs two sides.
  - The test's landmark sentinel was -1, which is a real chunk inside the city. It is now `Integer.MIN_VALUE`.
- Build round 1 failed with 7 failures, all fixed above. Round 2 runs: `tools/remote/lc-remote.sh build p5 HEAD -- spotlessApply build`.
- p4b's files (`NetworkScenarios`, rail pieces) are untouched.

## Round 2 result (failed, rounds used up)

1234 of 1235 tests pass (2 skipped). One failure is left: `CityHighwayLayoutTest.everyInterchangeAndCutThroughEndMeetsItsLink`, `expected: <1> but was: <0>`. A joined link's end level, `profile.inLevel`/`outLevel` at the plaza, stays at 0 while the city highway is at level 1. The likely cause is that the link has no rampable chunk before its plaza, so the ramp never climbs. A fix in `LinkHighways.compute` could force a climb onto the city highway or ramp on the city highway's own end chunks. The spotless diff has not been fetched, and the real-pack run has not been done.

## Remaining

1. If round 2 is green, bring back the spotless diff and commit it:
   `ssh <LC_BUILD_HOST> "cd <LC_REMOTE_ROOT>/wt/p5 && git diff"`, strip everything before the first `diff --git`, then `git apply`.
   If round 2 fails, report the evidence and stop. That was the last round.
2. Real-pack run on `--instance 5` with a fresh world on `rarecities`:
   - Set `profiles.dimensionProfiles` to `0=rarecities` with `--set-config`. Its file and list syntax are in `docs/config.md` §profiles. It is a string list, so check that `--set-config` handles lists.
   - Use the command in `docs/testing/README.md` "City runs on the real pack": `city.real` and `city.walk`, with ArchaicFix cascade logging on.
   - Then `grep -i cascad` the run log for our package.
3. Report: commits, what is live, evidence paths, and these open gaps:
   - an elevated plaza is a crossing piece open on every side, so an end can show an open edge;
   - the test uses the `LayoutProfile` builder, not `CityFixtures.plan`, because `CityFixtures` has no links mode (D-p5-8).
