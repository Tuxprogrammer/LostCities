# Handoff: lane/perf3 (perf round 3)

Stopped for context (about 170k of 200k). Brief: the orchestrator's perf-brief.md, which covers ROADMAP "Next version · GTNH 2.9.1" → "Chunk-generation performance follow-ups", items 1–5.

## State
- Branch `lane/perf3`, base `edc44e55`. The base build on slot perf3 is green (spotlessCheck build, 1426 tests).
- `c9736b65` is instrumentation only and is **not yet built or run**.
  - `layout/perf/PerfSpans` is an exclusive span stack with a `split()` line. Also `PerfCounters.addNanos/totalNanos/count`.
  - `layout/perf/TickStats` gives exact mean, p95, p99 and max.
  - `worldgen/overlay/LoadChunkProbe` adds two spans, but only while the perf counters are on:
    - `worldGenerators`, timed by IWorldGenerator edges at MIN and MAX weight;
    - `chunkLoadEvent`, timed by HIGHEST and LOWEST `ChunkEvent.Load` handlers.
  - More spans:
    - `provide` and `populate` in `OverlayChunkProvider`;
    - `loadChunk` and `serverPopulate` in `RegionGenerator`.
    - `PerfScenarios.report` logs them as `[LCNH-PERF] site N split ...`.
  - `PerfWalk` (`perf.walk`, manual-only) logs `[LCNH-PERF] walk mspt ...; tick-to-tick ...`.
  - `Rwg2HeightOracle.CACHE_CHUNKS` reads `-Dlostcitiesnh.noiseCacheChunks`. The default is unchanged at 4096.
  - Tests: `PerfSpansTest`, `TickStatsTest`.
- Item 4 fact: `PerfCounters` already has the thread dimension (`tagThread`, `PlanWorker.THREAD_TAG = ".worker"`).

## Base numbers so far (2.9.1, edc44e55, seed 4242, fresh)
- rare-1 (`reference/remote/realpack/20261122-182145-i3`): gen 27795 ms.
  - Timers: provide 8500, terrain 1389, populate 15598, population 34, prefetch 144.
  - Worst chunk: 769 ms at 251,3.
  - Counters: clg.route 412, clg.buildIndex 442, ho.noise 199, ho.noise.worker 445.
- default-1 (`.../20261122-182147-i4`): gen 29396 ms.
  - Timers: provide 8723, terrain 2356, populate 15415, population 465.
  - Worst chunk: 421 ms at 122,2.
- Still to come from the base chain:
  - instance 3: rare-2, rare-3 and hash-rwg2c;
  - instance 4: default-2, default-3, hash-rwg and the JFR run.
  - The first chain ran only one job per instance, because ssh ate the loop's stdin. jobs.sh now passes `< /dev/null`.
- Do not rebuild slot perf3 while those runs are going, because each run copies the slot jar.

## Helpers
They are in the session scratchpad `runs/` folder and may be gone; recreate them if so.
- `rp.sh`
- `jobs.sh <jobfile>`:
  - Lines are `inst|label|level-type|jvm|key`.
  - Keys are `sites`, `hash` and `siteswalk`.
  - `RARE` expands to `-Dlostcitiesnh.testHooks=true -Dlostcitiesnh.dimensionProfiles=0=rarecities`.
  - Each run goes through `lc-remote.sh realpack --slot perf3 -- --instance N --fresh --level-type LT --seed 4242 --jvm ... --cmds ...`.
- `perfsum.py <run-dir>...` prints totals, the worst chunk, the split, the walk line and hashes.
- `jfragg.py` reads, on stdin, the output of `jfr print --events jdk.ExecutionSample --stack-depth 300 perf3.jfr`.
  - The JFR file is in the realpack/instances/4/server/ folder; fetch it with `lc-remote.sh fetch`.

## Next
1. Collect the base totals and hashes, then aggregate the JFR. The item 2 breakdown goes into 0005 §8.
2. Build: `tools/agent/quiet-run.sh -- tools/remote/lc-remote.sh build perf3 HEAD -- spotlessApply build`.
   - Two APIs are not checked yet: `FMLCommonHandler.instance().bus().unregister` and `getConfigurationManager().getViewDistance()`.
3. After-runs:
   - Instance 3:
     - rare on ×3 with siteswalk;
     - rare with `-Dlostcitiesnh.planWorker=false` ×3 with siteswalk;
     - hash RWG2C with the worker on.
   - Instance 4:
     - rare with `-Dlostcitiesnh.noiseCacheChunks=65536` ×3;
     - default ×3;
     - hash RWG2C with the worker off;
     - hash on classic RWG.
4. Decide each item:
   - Item 1: name the split. The likely big share is `worldGenerators`: GT ores and the other IWorldGenerators run outside the "delegate populate" timer.
   - Item 3: keep 65536 only if it wins beyond noise and B5 (48 MB) still holds.
   - Item 4: compare MSPT with the worker on and off.
   - Item 5: chunk 251,3 is landmark-oracle queries inside link routing (clg.route, clg.buildIndex, lz.near.miss). VillageOracle and Roguelike are not worker-safe, so the leaning is to measure and decide.
5. Docs:
   - ROADMAP: five checked boxes with italic evidence notes.
   - 0005 §8: a "Measured in perf round 3" section.
   - Decision log: D-perf3-1 to D-perf3-5.
   - docs/testing/README.md: a `perf.walk` row next to `perf.sites`.
6. Commit, then `lc-remote.sh sync perf3 HEAD --bundle`. Never push to GitHub. Report in at most 15 lines.
