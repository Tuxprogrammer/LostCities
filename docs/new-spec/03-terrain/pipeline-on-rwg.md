# The terrain pipeline on RWG

Where each stage of upstream's per-chunk pipeline ([generation-pipeline.md](../../original-spec/03-terrain/generation-pipeline.md)) runs once the city is an overlay on RWG, what each phase may touch, and the three pieces of terrain work upstream does not have: the RWG decoration footprint, soft-edge strips toward landmark cores, and village lanes.

Status: *Accepted* from [0005](../../design/0005-rwg-overlay.md) and [0006](../../design/0006-build-around-other-worldgen.md), except where a line says *Proposed*; those are collected in the area's [Proposed, not decided](index.md#proposed-not-decided).

## The two phases

The delegating chunk provider ([0005](../../design/0005-rwg-overlay.md#1-hook-a-delegating-chunk-provider-q1) §1) gives the overlay two moments per chunk C:

| Phase | Runs | May read | May write | Never |
|---|---|---|---|---|
| **Terrain phase** | After RWG's `provideChunk(C)` returns, before C joins the loaded map | The layout plan for C and its neighbors (pure: seed, coordinates, the pinned profile snapshot, assets, oracle heights, structure oracles); C's own blocks | C's `ExtendedBlockStorage` sections, through the chunk buffer | Neighbor chunks, loaded or not; world lookups for C itself, which joins the loaded map only after `provideChunk` returns ([rwg.md](../00-pack-integration/rwg.md#forge-1710-facts-the-overlay-relies-on)), so block tests use block classes and the ore dictionary, never `IBlockAccess` methods; the live RWG generator; `world.rand`; tile entities; biomes; replacing the `Chunk` object |
| **Population phase** | After RWG's `populate(C)` returns (snow and ice included), before every `IWorldGenerator` | The plan; the world inside C | Blocks and tile entities inside C, through the world | Any block outside C; RWG's or an event's `Random` (it seeds its own from the world seed and C's coordinates) |

During RWG's own `populate`, two of our listeners act without being a phase: the `Populate` vetoes (`LAKE`, `LAVA`, `DUNGEON`, `CUSTOM`) when the population area touches a city chunk, and the footprint write guard ([below](#the-rwg-decoration-footprint)).

## Stage map

Upstream's stages are numbered as in [generation-pipeline.md](../../original-spec/03-terrain/generation-pipeline.md#lostcityterrainfeaturegenerate-server).

| # | Upstream stage | Here | Notes |
|---|---|---|---|
| 1 | Open the generation context (seeded random, palette sequence, buffers) | Terrain phase | Seeds mix in the world seed everywhere (TERR-27) |
| 2 | Attach the `ChunkDriver` | Terrain phase | The chunk buffer over C's sections; no staging |
| 3 | Fetch the heightmap and `BuildingInfo` | Terrain phase | The plan from `layout`, built from oracle heights and structure oracles; validated completely before the first write (TERR-25) |
| 4 | City or normal; floating void probe | Terrain phase | The void probe is deferred with the floating landscape (D12). A **landmark core** stops here: nothing is written but a village lane's surface path |
| 5 | City chunk (`doCityChunk`) or normal chunk (`doNormalChunk`: blend, bridges, highways, scattered) | Terrain phase | Toward a core, soft-edge strips replace border walls; blend corners next to a core are fixed at its natural height (E1) |
| 6 | Sphere centerpiece | Deferred | D12 |
| 7 | Railways and rail dungeons | Terrain phase | `06-highways-railways` |
| 8 | `fixTorches` | Population phase | [0005](../../design/0005-rwg-overlay.md#3-population-phase) §3 lists torch and attachment fix-ups there |
| 9 | Reseed to a world-independent seed | Terrain phase | The seed now includes the world seed (TERR-27) |
| 10 | `PreExplosionEvent`, explosion damage, debris | Terrain phase | Pure and confined to C; followed by the floating-fragment pass ported from 1.12 (TERR-28) |
| 11 | Early commit for second parts | None | Writes are direct, so there is nothing to commit |
| 12 | Commit and heightmap raise | Terrain phase, last step | Recompute the height map and sky light (`generateSkylightMap`) for C (TERR-13) |
| 13a | `ChunkFixer`: vines | Terrain phase | Vines in C's own columns, against neighbors' walls as the plan describes them (TERR-11) |
| 13b | `ChunkFixer`: post-todo jobs (loot, light refresh, saplings, command blocks; POI swap) | Population phase | POI swap dropped (no POI in 1.7.10) |
| 13c | `ChunkFixer`: border vine cleanup | Dropped | TERR-26 |
| 14 | Close the context; `Statistics` | Both phases | The wrapper's timers ([0005](../../design/0005-rwg-overlay.md#8-time-budget-and-how-it-is-measured-q10) §8) |
| — | `GlobalTodo` tick jobs | Dropped | Sapling growth moves into the population phase (TERR-20, TERR-21) |

The population phase, in order: data-bearing tile entities (chests filled from our `ChestGenHooks` categories, spawners, furnaces and other palette `tag` blocks) with their 1.7.10 NBT layout; torches; sapling stages and growth (*Proposed*: only trees that stay inside C); the command-block tick; light updates for the light sources the terrain phase wrote (*Proposed*, checked in M04).

## The RWG decoration footprint

[0005](../../design/0005-rwg-overlay.md#3a-rwg-sets-the-landscape-the-city-is-set-into-it) §3a: RWG builds its landscape first, terrain and decoration; the city is set into it. Inside the city's footprint RWG's decoration is cleared; outside it stays.

| Inside the footprint | Outside the footprint |
|---|---|
| Building volumes and their foundations (cellars, border skirts) | Park and yard columns, fountains' surroundings |
| Streets, plazas, stairs, slopes and their base | Edge-ring parks and soft-edge strips |
| Bridges and their supports | Blended non-city chunks |
| Corridors | Landmark cores, entirely (village pieces land on RWG's untouched ground) |
| Highway and railway cross-sections | Chunks the city does not touch |

**Mechanism.** The prototype picks it (0005 §3a): preferably one mixin on the chunk's block setter that drops RWG decoration writes inside footprint volumes, answered from pure layout data; otherwise a footprint restore after decoration. Decoration uses the vanilla +8 offset, so one chunk's decoration lands in up to four chunks; the guard looks up the chunk each write lands in.

**Volume** (*Proposed*). The outcome the owner asked for is no trees on streets and roofs (D22), so a footprint column must reach above the surface it protects. Recommended: for each column the city writes in a building, street, bridge, highway or railway chunk, everything from its lowest written block up to Y 255, plus, for highways, the 15-block clearance above open and bridge parts by the same rule. RWG's snow and ice, added after `PopulateChunkEvent.Post`, pass the guard. Cut canopies where a park tree overhangs a street are judged in the S2 screenshots ([0005](../../design/0005-rwg-overlay.md#still-open) "Still open" item 4).

## Soft-edge strips

Rules E2 to E4 of [coexistence.md § Soft edges](../00-pack-integration/coexistence.md#soft-edges), adopted by [0006](../../design/0006-build-around-other-worldgen.md#4-edge-rules) §4. They replace `generateBorders`' border wall ([city-chunk-terrain.md](../../original-spec/03-terrain/city-chunk-terrain.md#street-edge-borders-generateborders-part-2-server)) on every side of a city chunk that faces a landmark core.

| Item | Value | Source |
|---|---|---|
| Width | `softEdgeWidth` 6 columns, varied ±2 per column along the side by upstream's terrain transition noise | E2, E4 |
| Inner edge | The city ground of the chunk | E2 |
| Outer edge | The column's own RWG height at the core's edge, read from C's own columns | E2; [0005](../../design/0005-rwg-overlay.md#4-height-oracle) §4 |
| Surface | Each column keeps its own top block, moved with upstream's `moveDown` (6 kept layers) or `moveUp` | E2 |
| Steep step | When the height difference exceeds twice the strip's width, the strip ramps as far as it can and a retaining border of the city style's `borderBlock`, no taller than the remaining step, stands at the strip's inner edge | E3 |
| Decoration | Outside the footprint: RWG's trees and boulders stay | E5 |
| Ramp shape | *Proposed*: linear across the strip. The E3 threshold means a strip absorbs at most 2 blocks of height per column | none |

A strip is written only in the city chunk's own columns, so it never writes into the core.

## Village lanes

[0006](../../design/0006-build-around-other-worldgen.md#3-streets-buildings-and-the-planner) §3, written in the terrain phase before RWG's `populate` places the village pieces:

1. In the gate's edge-ring chunk the street continues as a 3-block-wide surface path, down or up the soft-edge strip to the core's edge (E8).
2. Across the core, a surface-only path to the end of the village path: the path block replaces the natural top block, with nothing written above or below it, and never within `landmarkMargin` (2 blocks) of a village piece box. This is the only thing the city ever writes in a core.
3. For villages outside a city but within `villageLinkDistance` (4 chunks) of its edge, the same surface lane runs across the land between, as part of the city's footprint.

The path block is *Proposed* (gravel, or the village's own path block when its oracle reports one).

## Measurement points

| Point | What | Source |
|---|---|---|
| Delegate `provideChunk`, terrain phase, delegate `populate`, population phase, oracle calls | Fixed-bucket `System.nanoTime` histograms, split by city, blend and untouched chunks; budgets B1 to B5 | [0005](../../design/0005-rwg-overlay.md#8-time-budget-and-how-it-is-measured-q10) §8 |
| After the terrain phase | Golden-chunk hash | [0005](../../design/0005-rwg-overlay.md#consequences) |
| After our population phase | A second golden hash, never after the whole of population | [0005](../../design/0005-rwg-overlay.md#consequences) |
