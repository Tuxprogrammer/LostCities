# Post-processing: torches, deferred jobs, vines

Everything that runs after the main staging pass: torch orientation before the commit, then (after the commit) the chunk fixer's vines and deferred per-chunk jobs, and finally global jobs that run on later server ticks.

## 1 · Identity

| Piece | Role |
|---|---|
| `LostCityTerrainFeature.fixTorches` | Turns queued torch positions into standing or wall torches (staged). |
| `BuildingInfo` torch list and post-todo map (04-city-layout's class, filled here) | `torchTodo` (list of positions), `postTodo` (`HashMap<BlockPos, Runnable>`). |
| `LostCityTerrainFeature.updateNeeded` | Queues a "set air, then set the state again" job to refresh lighting. |
| `worldgen/ChunkFixer` | `fix(info, coord)`: vines, post-todo jobs, boundary vine cleanup. |
| `worldgen/GlobalTodo` | Per-dimension queue of jobs run on server ticks (forced sapling growth). |
| `varia/TodoQueue` (01-core) | The concurrent queue behind `GlobalTodo`. |

Profile options: `VINE_CHANCE`, `GENERATE_LIGHTING`. World-style settings (05-assets): `vinewest`, `vineeast`, `vinenorth`, `vinesouth` block states (default vanilla vines facing that way). Config: `todoQueueSize` (`Config.TODO_QUEUE_SIZE`), `forceSaplingGrowth`.

## 2 · Inputs

The chunk's and its +X and +Z neighbors' `BuildingInfo` (building, max height, city ground); the region after the commit; neighbor chunk statuses; the queues.

## 3 · Numbers

| Item | Value |
|---|---|
| `VINE_CHANCE` | 0.009 per candidate block; vines skipped entirely when < 0.000001 |
| Vine strip continuation | 0.8 per block downward |
| Vine band bottom | `max(other side's cityGround + 3, other side's building top if it has a building)` |
| Vine band top | the wall owner's building top (`getMaxHeight`), exclusive |
| Vine columns per wall | 15 (local 0–14; 15 is never used) |
| Boundary vine scan | columns x = −1 … 16 on rows z = −1, 0, 15, 16, and z = 1 … 14 on columns x = −1, 0, 15, 16 (128 columns), each from `min(maxBuild − 1, WORLD_SURFACE)` down to the min build height |
| `todoQueueSize` | default 20, range 1–100 000: global jobs run per dimension per tick |
| Forced sapling growth area check | `isAreaLoaded(pos, 1)` |

## 4 · Behavior

### `fixTorches` (server, before the commit)

For each queued torch position (the context's current chunk), in queue order: if the block below is not air, a standing torch; else a wall torch attached to the first non-air neighbor in the order west (x > 0), east (x < 15), north (z > 0), south (z < 15), facing away from it; otherwise the block stays as staged. Each position also gets a light-refresh job. The queue is cleared. Torches are only queued when `GENERATE_LIGHTING` is on (default off; otherwise torch characters become air).

### `ChunkFixer.fix(info, coord)` (server, after the commit)

1. Reseed the context random to `worldSeed ^ chunkX × 341873128712 ^ chunkZ × 132897987541`.
2. **Vines** (`generateVines`), writing straight into the region with flag 0 (no updates):
   1. If this chunk has a building and the +X chunk is at least at `FEATURES` status: on the +X neighbor's column x = 16 (local 0 of the neighbor), for z 0–14 and Y from the band bottom (+X side) to this building's top, start a vine strip with probability `VINE_CHANCE`, using `vinewest` (attached to this building's east face). Then remove any configured vine in that strip that cannot survive.
   2. If the +X neighbor has a building and it is at least at `FEATURES`: on this chunk's x = 15, the same with `vineeast`, up to the neighbor's building top.
   3. The same two steps on the +Z side with `vinenorth` (at z = 16) and `vinesouth` (at z = 15).

   A strip places a vine at the start if the block is empty and the vine can survive, then continues down with probability 0.8 per block while empty, survivable and at or above the band bottom. Faces toward −X and −Z are never handled by this chunk, and a face is skipped forever if its +X/+Z neighbor was not yet at `FEATURES` (`TERR-11`).
3. **Post-todo jobs:** run every job in this chunk's `BuildingInfo.postTodo` map (hash-map iteration order), then clear it. Jobs are: loot placement and loot-table assignment, POI block swap, light refresh, non-forced sapling stage, command-block tick ([driver sheet](chunk-driver-and-parts.md)).
4. **Boundary vine cleanup:** in the 128 border columns (both sides of all four chunk edges), walk each column from the top of `WORLD_SURFACE` down to the minimum build height and remove every vine block that cannot survive (`removeBlock`, no drops). This catches vines whose support a city or blended chunk removed, including vanilla vines in already-finished neighbors.

### `GlobalTodo` (server, on ticks)

1. `addTodo(pos, job)` puts the job in the queue for `pos`'s chunk, in the per-dimension instance (a static map keyed by dimension, never cleared; `TERR-21`).
2. Each server-level tick (end phase), `ForgeEventHandlers.onWorldTick` first loads assets, then calls `executeAndClearTodo`: copy the chunk → queue map, and for each chunk entry (arbitrary order) poll and run jobs until `todoQueueSize` jobs ran in total this tick; drop chunk entries whose queues are empty.
3. The only job type is forced sapling growth: if the area within 1 block of the sapling is loaded and the block is still a sapling, set the sapling state again and call the sapling's tree growth with a random forked from the chunk's generation random. If the area is not loaded, the job is consumed and nothing happens, so the sapling stays a sapling (`TERR-20`).

## 5 · Determinism

- Vines: one random reseeded per chunk, but the number of draws depends on which neighbors are at `FEATURES` (`TERR-11`).
- Post-todo jobs run in `HashMap<BlockPos, …>` order; loot jobs draw from the shared context random in that order (deterministic for a given JVM, since `BlockPos` hashes are stable, but tied to the hash layout).
- Tree growth happens on later ticks, after neighbors may have generated, using a forked random; trees grow into whatever is there by then (*inferred*).

## 6 · Data formats

World-style vine states (`vinewest`, `vineeast`, `vinenorth`, `vinesouth`, 05-assets). No NBT.

## 7 · Output

Oriented torches; vines on the sides of buildings that face +X and +Z neighbors; loot tables on chests; POI blocks in place; refreshed light sources; grown trees later; no unsupported vines along the chunk border.

## 8 · Relationships

`fix` is called at the end of the [pipeline](generation-pipeline.md) and again by `Spheres.generateSpheres` in sphere worlds (`TERR-26`). Loot and spawner logic: 08-loot-mobs-blocks. `ForgeEventHandlers.onWorldTick`: 01-core.

## 9 · 1.20 dependencies

- `ChunkStatus.FEATURES` / `isOrAfter` gating.
- `BlockState.canSurvive`, `LevelAccessor.removeBlock`, `Heightmap.Types.WORLD_SURFACE`, vine block states with face properties.
- `SaplingBlock.advanceTree` and `RandomSource.fork`; `ServerLevel.isAreaLoaded`.
- `Map.copyOf` (Java 10+) in `GlobalTodo`.

## 10 · Bugs

`TERR-11`, `TERR-20`, `TERR-21`, `TERR-26`, `TERR-32`.

## On 1.12

1.12 did vines, sapling growth and loot in `LostCityChunkGenerator.populate`, the vanilla population pass that runs once the 2 × 2 chunks at +X/+Z exist. Vines used the same bands and chances but tested `world.isAirBlock` and needed the holder block to be non-air; trees grew immediately during population. That timing (population with the +8 offset) is what 1.7.10 has.

## Deviations in the new mod

None recorded yet.
