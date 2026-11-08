# Handoff: lane/plan-worker (off-server-thread city planning)

Status: design settled, three pieces written, **not wired, not tested**. Context limit reached; resume from here.

## Done (this commit)

- `worldgen/height/HeightProbe.java` (outside the lane's file list): worker deferral policy. `bindWorker(thread, safeSource)`;
  in `fetchMissing()`, on the bound worker thread, a heavy fetch or a fetch whose source is not allowlisted is stored
  (`takeDeferred()`, `deferredIsHeavy()`, call the latter first) and `NotCached` is thrown instead of running it.
- `worldgen/overlay/PlanLock.java`: fair `Semaphore(1)`, held by the server thread from construction; `openWindow()` /
  `closeWindow()` (server), `acquireUnit()` / `releaseUnit()` (worker).
- `worldgen/overlay/PlanWorker.java`: single daemon thread, `LinkedBlockingDeque` of targets with a dedupe set; one
  unit (probe attempt plus at most one fetch) per lock hold; deferred fetches go to a `ConcurrentLinkedQueue<Deferred>`
  for the server; `SAFE_SOURCES` allowlist; `enabled()` reads `-Dlostcitiesnh.planWorker` (default true); `stop()`.

## Design (log as decision rows)

- **D-plan-worker-1, coarse lock with windows.** Planning caches are not thread-safe (see below), so all planning
  is serialized under `PlanLock`. The server thread opens windows only (a) around `delegate.provideChunk` in
  `OverlayChunkProvider` (skip when nested or when `FootprintGuard.populating()`), and (b) from `ServerTickEvent` END
  (handler priority LOWEST, after the prefetcher tick) to `ServerTickEvent` START (priority HIGHEST). The worker holds
  the lock for one unit, so the server waits at most one unit. Serial plus pure means deterministic.
- **D-plan-worker-2, the worker owns its world inputs.** It is enabled only on RWG2 worlds (`ChunkManagerRealistic`).
  The worker gets its own `ChunkManagerRealistic(seed, continental)`; RWG2's `biomeDataMap` is an unsynchronized
  `TLongObjectHashMap` (cite the RWG2 tag from `dependencies.gradle`, never a local path). The worker also gets its own
  `Rwg2HeightOracle`. Vanilla GenLayer/IntCache is unsafe across threads (Hodgepodge `fastIntCache` defaults off), hence
  RWG2 only.
- **D-plan-worker-3, deferred sources.** The worker runs only fetches from `ColumnHeightCache`, `ChunkHeights`,
  `ChunkColumnCache`, `NetworkGate`, `Rwg2LandmarkOracle`. Roguelike, Witchery, AE2, LootGames and VillageOracle fetches
  (and every heavy fetch, i.e. VillageNames `start()`) are deferred to the server prefetcher, which runs them and
  resubmits the target first. This is the "guard" for oracles not proven pure.
- **D-plan-worker-4, JVM switch only.** `-Dlostcitiesnh.planWorker=false` turns it off; no config option.

## Remaining steps

1. `PlanThreads` (new, `worldgen/overlay/`): a ThreadLocal context mapping World to the worker's manager.
   `manager(world)` returns the worker's manager on the worker, else `world.getWorldChunkManager()`. Route these sites:
   - `worldgen/overlay/WorldTerrainView.biome()`
   - `core/assets/BiomeDescriptors.java:35`
   - `compat/villagenames/ChunkManagerBiomeNames` (add a World constructor; built at `VillageNamesOracles.java:128`)
   - `compat/rwg2/ManagerLandforms` (built at `Rwg2Landmarks.java:102-111`)
   - Roguelike and Witchery: optional, since their fetches are deferred.
2. A routed `TerrainHeightOracle`: on the worker, delegate to a worker-owned `Rwg2HeightOracle`. This needs a few-line
   factory `Rwg2HeightOracle.create(ChunkManagerRealistic, long seed, boolean continental)` (lane CD file; report it),
   plus a factory in `compat/HeightOracles`.
3. Wiring:
   - `OverlayWorld`: create the lock and worker when enabled; `close()` stops the worker.
   - `OverlayChunkProvider`: the provideChunk window.
   - `PrefetchTicker`: split into START/END handlers.
   - `PlanPrefetcher` worker mode: drain deferred fetches (heavy only at tick start, under `HEAVY_START_NANOS`), then
     submit targets that are not cached or loaded, with a queue cap. The inline mode stays unchanged when the worker is off.
   - Budget report: add `PlanWorker.summary()`.
4. `PlanWorkerTest`:
   - Two identical stacks from `NetworkWriterTest.networkPlan()` and `CityFixtures`.
   - Plan a 32×32 region inline on one stack, and through `PlanWorker` on the other, with the test thread holding `PlanLock` windows.
   - Compare per-chunk descriptions (ChunkPlan has no `equals`).
   - Assert that a city chunk and a highway are present.
5. Add decision-log rows, then run the targeted tests:
   `lc-remote.sh build perfA HEAD -- test --tests '*PlanWorker*' --tests '*PlanPrefetcher*' --tests '*LayoutCityPlan*' --tests '*Golden*'`.
   Then build the jar and do one real-pack run with the brief's command (`pw1`, seed 4242, rarecities), and grep the
   console for exceptions, `ConcurrentModification` and `LCNH plan worker`.

## Thread-safety findings in other lanes' files (report, do not fix)

- `layout/ChunkCache.java`: an access-ordered `LinkedHashMap`, so `get()` mutates it. It is used by lane B's
  `CityMap`, `LandmarkZones` and `link/CityLinkGraph`.
- `layout/LayoutPlanner.java`: `warned` HashSet (line 154) and lazy `Derived` fields (~2359–2380).
- `worldgen/plan/CachedCityPlan`: unsynchronized set-associative cache.
- The brief's "caches are synchronized LRUs" does not hold, which is why the design uses one coarse lock.
- Worldgen `ChunkPlan` content writers call the planner at write time, so they rely on the server holding the lock
  outside windows.
